package com.example.cardtally.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.example.cardtally.R
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.network.MiniMaxChatResult
import com.example.cardtally.network.MiniMaxConfig
import com.example.cardtally.network.MiniMaxErrorType
import com.example.cardtally.network.MiniMaxRequestHandle
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.LedgerSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Enabling tools allows scoped reads/context reuse; each final write still needs native confirmation. */
class AiRecordAssistant(
    private val context: Context,
    private val config: MiniMaxConfig,
    private val ledgerId: Long,
    private val sessionId: Long,
    private val onPending: (AiRecordEngine.Plan?) -> Unit,
    private val onLocalResult: (String) -> Unit,
    private val onComplete: (MiniMaxChatResult) -> Unit
) {
    data class ContinuationContext(val serialize: () -> String)
    private val db = DatabaseHelper(context, ledgerId)
    private val engine = AiRecordEngine(context, db)
    private val audit get() = AiRecordAuditStore(db.writableDatabase)
    private val client = RecordToolClient()
    private val main = Handler(Looper.getMainLooper())
    private val messages = JSONArray()
    private var active = true
    private var call: Call? = null
    private var pending: AiRecordEngine.Plan? = null
    private var operationId: String? = null
    private var dialog: AlertDialog? = null
    private var steps = 0
    private var toolCalls = 0
    private var writeCount = 0
    private val receipts = mutableListOf<String>()
    private val origin = config.requestUrl.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}:${it.port}" }.orEmpty()

    fun contextForNextTurn(nextConfig: MiniMaxConfig, nextLedger: Long, nextSession: Long): ContinuationContext? =
        if (!active && config == nextConfig && ledgerId == nextLedger && sessionId == nextSession)
            ContinuationContext { messages.toString() } else null

    fun start(content: String, previousContext: ContinuationContext? = null): MiniMaxRequestHandle {
        audit.expireUnfinished()
        val handle = MiniMaxRequestHandle { cancel() }
        if (!validScope()) { cancel(); return handle }
        messages.put(RecordToolProtocol.systemMessage(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())))
        if (previousContext != null) {
            val previous = JSONArray(previousContext.serialize())
            for (index in 1 until previous.length()) messages.put(previous.getJSONObject(index))
        }
        messages.put(JSONObject().put("role", "user").put("content", content))
        request()
        return handle
    }

    private fun validScope(): Boolean = active &&
        AiAssistantSettingsHelper.getRecordToolsEnabled(context) &&
        AiAssistantSettingsHelper.getMiniMaxConfig(context) == config &&
        LedgerSession.getCurrentId(context) == ledgerId &&
        AiAssistantSettingsHelper.getActiveSessionId(context) == sessionId

    private fun request() {
        if (!validScope()) { cancel(); return }
        if (++steps > 12) { finish(MiniMaxChatResult.Success(context.getString(R.string.ai_record_step_limit))); return }
        try {
            call = client.send(config, messages, allowWrites = writeCount == 0) { result -> main.post {
                if (!active) return@post
                call = null
                if (!validScope()) { cancel(); return@post }
                when (result) {
                    is RecordToolClient.Result.Failure -> finish(toolFailure(result))
                    is RecordToolClient.Result.Success -> {
                        val tools = result.reply.calls
                        if (tools.isEmpty()) {
                            messages.put(JSONObject().put("role", "assistant").put("content", result.reply.text))
                            finish(MiniMaxChatResult.Success(result.reply.text))
                        } else if (toolCalls + tools.size > 12) {
                            finish(MiniMaxChatResult.Success(context.getString(R.string.ai_record_step_limit)))
                        } else {
                            toolCalls += tools.size
                            if (tools.size == 1) propose(tools.single()) else processBatch(tools)
                        }
                    }
                }
            } }
        } catch (_: IllegalArgumentException) {
            finish(MiniMaxChatResult.Failure(MiniMaxErrorType.INVALID_CONFIG))
        }
    }

    private fun toolFailure(result: RecordToolClient.Result.Failure): MiniMaxChatResult {
        val reasonText = result.protocolReason?.let { reason -> context.getString(when (reason) {
            RecordToolProtocol.FailureReason.EMPTY_MESSAGE -> R.string.ai_record_tool_empty
            RecordToolProtocol.FailureReason.TOO_MANY_CALLS -> R.string.ai_record_tool_multiple
            RecordToolProtocol.FailureReason.UNREGISTERED_TOOL -> R.string.ai_record_tool_unregistered
            RecordToolProtocol.FailureReason.INVALID_ARGUMENTS -> R.string.ai_record_tool_arguments
            RecordToolProtocol.FailureReason.MALFORMED_RESPONSE -> R.string.ai_record_tool_failed
        }) }
        if (reasonText != null) return MiniMaxChatResult.Success(reasonText)
        if (result.type == MiniMaxErrorType.INVALID_REQUEST && result.statusCode != null)
            return MiniMaxChatResult.Success(context.getString(R.string.ai_record_tool_request_rejected, result.statusCode))
        if (result.type == MiniMaxErrorType.PARSE)
            return MiniMaxChatResult.Success(context.getString(R.string.ai_record_tool_failed))
        return MiniMaxChatResult.Failure(result.type, statusCode = result.statusCode)
    }

    /** Serial reads; batched writes get a negative result and must be proposed separately. */
    private fun processBatch(tools: List<RecordToolProtocol.Call>) {
        try {
            // Validate every read before doing any of them. No batch can create a write preview.
            val plans = tools.filter { it.name in RecordToolProtocol.readNames }.map(engine::prepare)
            val results = linkedMapOf<String, JSONObject>()
            for (plan in plans) {
                if (!validScope()) { cancel(); return }
                val op = UUID.randomUUID().toString()
                operationId = op
                audit.propose(op, sessionId, ledgerId, origin, config.model,
                    plan.call.name, plan.targetId, plan.fingerprint)
                val result = engine.execute(plan, op)
                results[plan.call.id] = result
                receipts.add(engine.localResult(plan, result))
                operationId = null
                onLocalResult(receipts.joinToString("\n\n"))
                if (!active) return
            }
            for (tool in tools.filter { it.name in RecordToolProtocol.writeNames }) {
                if (!validScope()) { cancel(); return }
                val op = UUID.randomUUID().toString()
                operationId = op
                audit.propose(op, sessionId, ledgerId, origin, config.model, tool.name, null, "")
                audit.finish(op, "failed") // Not confirmed or executed; do not log raw arguments.
                results[tool.id] = RecordToolProtocol.deferredWriteResult(allowWrites = writeCount == 0)
                operationId = null
            }
            if (!validScope()) { cancel(); return }
            // Keep the original call batch together and match each result by ID. Only continue
            // once every call has a result, so later turns cannot inherit an unfinished exchange.
            messages.put(RecordToolProtocol.assistantCalls(tools))
            tools.forEach { messages.put(RecordToolProtocol.toolResult(it, results.getValue(it.id))) }
            request()
        } catch (_: Exception) {
            try { operationId?.let { audit.finish(it, "failed") } } catch (_: Exception) { }
            finish(MiniMaxChatResult.Success(context.getString(R.string.ai_record_invalid)))
        }
    }

    private fun propose(tool: RecordToolProtocol.Call) {
        if (RecordToolProtocol.requiresConfirmation(tool.name) && writeCount > 0) {
            finish(MiniMaxChatResult.Success(context.getString(R.string.ai_record_write_finished)))
            return
        }
        val op = UUID.randomUUID().toString()
        operationId = op
        try {
            val plan = engine.prepare(tool)
            audit.propose(op, sessionId, ledgerId, origin, config.model, tool.name, plan.targetId, plan.fingerprint)
            if (RecordToolProtocol.requiresConfirmation(tool.name)) {
                pending = plan
                onPending(plan)
                review() // Open the final preview directly: only one confirmation button is needed.
            } else {
                val result = engine.execute(plan, op)
                continueAfterExecution(plan, result)
            }
        } catch (_: Exception) {
            // A malformed suggestion never reaches a write or a provider continuation.
            try {
                if (audit.state(op) == null) audit.propose(op, sessionId, ledgerId, origin, config.model, tool.name, null, "")
                audit.finish(op, "failed")
            } catch (_: Exception) { /* Failure text contains no database/provider details. */ }
            finish(MiniMaxChatResult.Success(context.getString(R.string.ai_record_invalid)))
        }
    }

    fun review() {
        val plan = pending ?: return
        if (!validScope() || engine.isExpired(plan)) { expire(); return }
        if (dialog?.isShowing == true) return
        val disclosure = context.getString(R.string.ai_record_disclosure, origin, config.model)
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(plan.title)
            .setMessage(plan.details + "\n\n" + disclosure)
            .setNegativeButton(R.string.ai_record_reject) { _, _ -> reject() }
            .setPositiveButton(R.string.ai_record_confirm) { _, _ -> confirm(plan) }
            .setOnCancelListener { reject() }
            .show()
    }

    private fun confirm(plan: AiRecordEngine.Plan) {
        if (pending !== plan || !active) return
        if (!validScope() || engine.isExpired(plan)) { expire(); return }
        val op = operationId ?: return
        pending = null // Consume before any SQLite operation: double clicks cannot execute twice.
        onPending(null)
        try {
            check(audit.recordConfirmation(op))
            val result = engine.execute(plan, op)
            writeCount++
            continueAfterExecution(plan, result)
        } catch (_: Exception) {
            audit.finish(op, "failed")
            finish(MiniMaxChatResult.Success(context.getString(R.string.ai_record_changed_or_failed)))
        }
    }

    private fun continueAfterExecution(plan: AiRecordEngine.Plan, result: JSONObject) {
        receipts.add(engine.localResult(plan, result))
        onLocalResult(receipts.joinToString("\n\n"))
        messages.put(RecordToolProtocol.assistantCall(plan.call))
        messages.put(RecordToolProtocol.toolResult(plan.call, result))
        operationId = null
        request()
    }

    fun reject() {
        if (!active) return
        operationId?.let { audit.finish(it, "rejected") }
        finish(MiniMaxChatResult.Success(context.getString(R.string.ai_record_rejected)))
    }

    private fun expire() {
        operationId?.let { audit.finish(it, "expired") }
        finish(MiniMaxChatResult.Success(context.getString(R.string.ai_record_expired)))
    }

    fun cancel() {
        if (!active) return
        operationId?.let { audit.finish(it, "cancelled") }
        finish(MiniMaxChatResult.Failure(MiniMaxErrorType.CANCELLED))
    }

    private fun finish(result: MiniMaxChatResult) {
        if (!active) return
        active = false
        call?.cancel()
        call = null
        pending = null
        dialog?.dismiss()
        dialog = null
        onPending(null)
        db.close()
        val safeResult = if (result is MiniMaxChatResult.Success && writeCount == 0)
            result.copy(reply = context.getString(R.string.ai_record_no_writes) + "\n\n" + result.reply) else result
        val finalResult = if (receipts.isEmpty()) safeResult else {
            val tail = when (safeResult) {
                is MiniMaxChatResult.Success -> safeResult.reply
                else -> context.getString(R.string.ai_record_followup_failed)
            }
            MiniMaxChatResult.Success(receipts.joinToString("\n\n") + "\n\n" + tail)
        }
        onComplete(finalResult)
    }
}
