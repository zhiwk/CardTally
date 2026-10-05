package com.example.cardtally.ai

import android.content.Context
import android.os.SystemClock
import com.example.cardtally.R
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record
import com.example.cardtally.util.Money
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

/** One immutable proposal, scoped to one ledger. Preview and execution share the same validation. */
class AiRecordEngine(
    private val context: Context,
    private val db: DatabaseHelper,
    private val clock: () -> Long = SystemClock::elapsedRealtime
) {
    data class Plan(val call: RecordToolProtocol.Call, val title: String, val details: String,
                    val fingerprint: String, val createdAt: Long, val targetId: Long?)
    private val writeFields = setOf("date", "amount", "type", "category_id", "asset_id", "destination_asset_id", "fee", "description")
    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    fun prepare(call: RecordToolProtocol.Call): Plan {
        require(call.name in RecordToolProtocol.names)
        val args = JSONObject(call.arguments)
        val ledger = requireNotNull(db.getCurrentLedger())
        val target = if (call.name in setOf("get_record", "update_record", "delete_record")) id(args, "record_id") else null
        val allowed = when (call.name) {
            "record_options" -> setOf("keyword")
            "query_records" -> setOf("start_date", "end_date", "keyword", "category_id", "after")
            "get_record", "delete_record" -> setOf("record_id")
            "update_record" -> writeFields + "record_id"
            else -> writeFields
        }
        require(args.keys().asSequence().all { it in allowed })
        val old = if (call.name in setOf("update_record", "delete_record")) {
            requireNotNull(db.getRecordById(requireNotNull(target)))
        } else null
        val title = text(when (call.name) {
            "create_record" -> R.string.ai_record_create
            "update_record" -> R.string.ai_record_update
            "delete_record" -> R.string.ai_record_delete
            "record_options" -> R.string.ai_record_options
            else -> R.string.ai_record_query
        })
        val scope = text(R.string.ai_record_scope, ledger.name, ledger.id)
        if (call.name in setOf("record_options", "query_records", "get_record")) {
            validateQuery(call.name, args)
            val filters = buildList {
                target?.let { add(text(R.string.ai_record_id, it)) }
                listOf("start_date", "end_date", "keyword", "category_id").forEach { key ->
                    if (args.has(key)) add(text(when (key) {
                        "start_date" -> R.string.ai_record_start_date
                        "end_date" -> R.string.ai_record_end_date
                        "keyword" -> R.string.ai_record_keyword
                        else -> R.string.ai_record_category_id
                    }, args.get(key).toString()))
                }
                if (args.has("after")) add(text(R.string.ai_record_next_page))
            }.joinToString("\n")
            return Plan(call, title, "$scope\n$filters\n${text(R.string.ai_record_read_fields)}", "", clock(), target)
        }
        val assets = (db.getAllAssets() + db.getArchivedAssets()).associateBy { it.id }
        val next = if (call.name == "delete_record") null else proposedRecord(args, old, assets)
        val effects = balanceEffects(old, next)
        val impact = effects.entries.joinToString("\n") { (assetId, delta) ->
            val asset = requireNotNull(assets[assetId])
            val balance = requireNotNull(Money.toMinor(asset.amount))
            val after = Math.addExact(balance, delta)
            text(R.string.ai_record_balance_change, asset.name, asset.id, Money.formatYuan(balance), Money.formatYuan(after))
        }
        val members = db.getAssetGroupMemberIds(ledger.id).toSet()
        val affected = db.getLedgers().filter { it.id in members }.joinToString { "${it.name} (#${it.id})" }
        val detail = buildList {
            add(scope)
            target?.let { add(text(R.string.ai_record_id, it)) }
            old?.let { add(text(R.string.ai_record_before) + "\n" + describe(it)) }
            next?.let { add(text(R.string.ai_record_after) + "\n" + describe(it)) }
            if (impact.isNotBlank()) {
                add(text(R.string.ai_record_balances) + "\n" + impact)
                add(text(R.string.ai_record_shared_effect, affected))
            } else {
                add(text(R.string.ai_record_no_asset_effect))
            }
            if (old != null && call.name == "delete_record") {
                add(text(R.string.ai_record_delete_effect, (old.photoUris + listOfNotNull(old.photoUri)).distinct().size))
            }
            add(text(R.string.ai_record_expiry))
        }.joinToString("\n\n")
        val relevant = effects.keys.sorted().joinToString { assets[it].toString() }
        val fingerprint = sha256(listOf(call.arguments, ledger.toString(), old.toString(), next.toString(), relevant, affected).joinToString("|"))
        return Plan(call, title, detail, fingerprint, clock(), target)
    }

    fun isExpired(plan: Plan): Boolean = clock() - plan.createdAt !in 0..300_000L

    /** Guard + balance changes + audit terminal state share a single SQLite transaction. */
    fun execute(plan: Plan, operationId: String): JSONObject {
        val sql = db.writableDatabase
        val audit = AiRecordAuditStore(sql)
        sql.beginTransaction()
        return try {
            require(!isExpired(plan))
            check(audit.transition(operationId, "proposed",
                if (RecordToolProtocol.requiresConfirmation(plan.call.name)) "confirmed" else "reading"))
            val fresh = prepare(plan.call)
            require(fresh.fingerprint == plan.fingerprint)
            val args = JSONObject(plan.call.arguments)
            val result = when (plan.call.name) {
                "record_options" -> options(args)
                "query_records" -> query(args)
                "get_record" -> JSONObject().put("record", recordJson(requireNotNull(db.getRecordById(id(args, "record_id")))))
                "create_record" -> {
                    val newId = db.addRecord(proposedRecord(args, null, db.getAllAssets().associateBy { it.id }))
                    check(newId > 0)
                    JSONObject().put("record_id", newId)
                }
                "update_record" -> {
                    val old = requireNotNull(db.getRecordById(id(args, "record_id")))
                    check(db.updateRecord(proposedRecord(args, old, db.getAllAssets().associateBy { it.id })) == 1)
                    JSONObject().put("record_id", old.id)
                }
                "delete_record" -> {
                    val recordId = id(args, "record_id")
                    requireNotNull(db.getRecordById(recordId))
                    db.deleteRecord(recordId)
                    check(db.getRecordById(recordId) == null)
                    JSONObject().put("record_id", recordId)
                }
                else -> error("Unregistered tool")
            }.put("success", true)
            // Read result bodies are deliberately not written to the audit log.
            audit.finish(operationId, "success", if (plan.call.name in setOf("create_record", "update_record", "delete_record")) result.toString() else null)
            sql.setTransactionSuccessful()
            result
        } finally {
            sql.endTransaction()
        }
    }

    private fun validateQuery(name: String, args: JSONObject) {
        if (args.has("keyword")) require(args.get("keyword") is String && args.getString("keyword").length <= 256)
        val start = args.opt("start_date") as? String
        val end = args.opt("end_date") as? String
        if (args.has("start_date")) require(start != null && validDate(start))
        if (args.has("end_date")) require(end != null && validDate(end))
        if (start != null && end != null) require(start <= end)
        if (args.has("category_id")) id(args, "category_id")
        if (args.has("after")) {
            val after = args.getJSONObject("after")
            require(after.keys().asSequence().toSet() == setOf("date", "sort_order", "record_id"))
            require(validDate(after.getString("date")))
            require(integer(after, "sort_order") in 0..Int.MAX_VALUE.toLong())
            id(after, "record_id")
        }
        if (name == "get_record") id(args, "record_id")
    }

    private fun options(args: JSONObject): JSONObject {
        val keyword = args.optString("keyword").trim()
        val assets = db.getAllAssets().filter { it.name.contains(keyword, true) }
        val categories = (db.getLeafCategoriesByType(0) + db.getLeafCategoriesByType(1)).map {
            JSONObject().put("id", it.id).put("type", it.type).put("name", it.name)
                .put("path", db.buildCategoryPathLabel(it.id).orEmpty())
        }.filter { it.getString("path").contains(keyword, true) }
        return JSONObject().put("assets", JSONArray(assets.take(100).map { JSONObject().put("id", it.id).put("name", it.name) }))
            .put("categories", JSONArray(categories.take(100)))
            .put("has_more", assets.size > 100 || categories.size > 100)
    }

    private fun query(args: JSONObject): JSONObject {
        val after = args.optJSONObject("after")?.let {
            DatabaseHelper.RecordListCursor(it.getString("date"), integer(it, "sort_order").toInt(), id(it, "record_id"))
        }
        val page = db.getRecordsPage(args.opt("start_date") as? String, args.opt("end_date") as? String,
            args.opt("keyword") as? String, if (args.has("category_id")) id(args, "category_id") else null, after, limit = 20)
        val next = page.nextCursor?.let {
            JSONObject().put("date", it.date).put("sort_order", it.sortOrder).put("record_id", it.recordId)
        }
        return JSONObject().put("records", JSONArray(page.records.map(::recordJson)))
            .put("next_cursor", next ?: JSONObject.NULL)
    }

    private fun proposedRecord(args: JSONObject, old: Record?, assets: Map<Long, Asset>): Record {
        if (old != null) require(args.keys().asSequence().any { it in writeFields })
        val type = if (args.has("type")) integer(args, "type").toInt().also { require(it in 0..2 && integer(args, "type") == it.toLong()) }
            else requireNotNull(old).type
        val date = if (args.has("date")) args.getString("date") else requireNotNull(old).date
        require(validDate(date))
        val amount = if (args.has("amount")) money(args, "amount", false) else requireNotNull(Money.toMinor(requireNotNull(old).amount))
        require(amount > 0)
        val sourceId = if (args.has("asset_id")) {
            if (args.isNull("asset_id")) null else id(args, "asset_id")
        } else old?.assetId
        require(type != 2 || sourceId != null)
        val source = sourceId?.let { requireNotNull(assets[it]).also { asset -> require(!asset.isArchived) } }
        val destinationId = if (type == 2) {
            if (args.has("destination_asset_id")) id(args, "destination_asset_id") else requireNotNull(old?.destinationAssetId)
        } else null
        val destination = destinationId?.let { requireNotNull(assets[it]).also { asset -> require(!asset.isArchived && asset.id != sourceId) } }
        val categoryId = if (type == 2) null else {
            if (args.has("category_id")) id(args, "category_id") else requireNotNull(old?.categoryId)
        }
        val category = categoryId?.let { categoryIdValue ->
            requireNotNull(db.getLeafCategoriesByType(type).firstOrNull { it.id == categoryIdValue })
        }
        val fee = if (type != 2) {
            if (args.has("fee")) require(money(args, "fee", true) == 0L)
            0L
        } else if (args.has("fee")) money(args, "fee", true) else requireNotNull(Money.toMinor(old?.fee ?: 0.0))
        Math.addExact(amount, fee)
        if (type != 2) require(!args.has("destination_asset_id"))
        if (type == 2) require(!args.has("category_id"))
        val description = if (args.has("description")) args.getString("description").also {
            require(args.get("description") is String && it.length <= 2000)
        } else old?.description
        require(Money.toMinor(Money.toMajorDouble(amount)) == amount && Money.toMinor(Money.toMajorDouble(fee)) == fee)
        return (old?.copy() ?: Record()).copy(
            date = date, amount = Money.toMajorDouble(amount), type = type, fee = Money.toMajorDouble(fee),
            assetId = source?.id, assetSource = source?.name, destinationAssetId = destination?.id,
            destinationAssetSource = destination?.name, categoryId = category?.id,
            category = category?.name.orEmpty(), categoryNameSnapshot = category?.name,
            categoryPathSnapshot = category?.let { db.buildCategoryPathLabel(it.id) }, description = description,
            ledgerId = db.getCurrentLedger()?.id
        )
    }

    private fun balanceEffects(old: Record?, next: Record?): Map<Long, Long> {
        val result = linkedMapOf<Long, Long>()
        fun apply(record: Record?, reverse: Boolean) {
            if (record == null) return
            val amount = requireNotNull(Money.toMinor(record.amount))
            val fee = requireNotNull(Money.toMinor(record.fee))
            val direction = if (reverse) -1L else 1L
            fun add(id: Long?, delta: Long) { if (id != null) result[id] = Math.addExact(result[id] ?: 0L, delta * direction) }
            when (record.type) {
                0 -> add(record.assetId, -amount)
                1 -> add(record.assetId, amount)
                2 -> { add(record.assetId, -Math.addExact(amount, fee)); add(record.destinationAssetId, amount) }
            }
        }
        apply(old, true); apply(next, false)
        return result
    }

    fun localResult(plan: Plan, result: JSONObject): String {
        val summary = text(R.string.ai_record_done, plan.title)
        val records = result.optJSONArray("records")
        return when {
            records != null -> summary + "\n" + (0 until records.length()).joinToString("\n\n") {
                val row = records.getJSONObject(it)
                text(R.string.ai_record_result_row, row.getLong("id"), row.getString("date"),
                    row.getString("amount"), row.getString("category"), row.getString("asset"), row.getString("description"),
                    text(when (row.getInt("type")) { 0 -> R.string.record_type_expense; 1 -> R.string.record_type_income; else -> R.string.record_type_transfer }),
                    row.getString("destination_asset"), row.getString("fee"))
            }.ifEmpty { text(R.string.ai_record_no_results) } +
                if (!result.isNull("next_cursor")) "\n" + text(R.string.ai_record_more_results) else ""
            result.has("record") -> {
                val row = result.getJSONObject("record")
                summary + "\n" + text(R.string.ai_record_result_row, row.getLong("id"), row.getString("date"),
                    row.getString("amount"), row.getString("category"), row.getString("asset"), row.getString("description"),
                    text(when (row.getInt("type")) { 0 -> R.string.record_type_expense; 1 -> R.string.record_type_income; else -> R.string.record_type_transfer }),
                    row.getString("destination_asset"), row.getString("fee"))
            }
            result.has("record_id") -> summary + "\n" + text(R.string.ai_record_id, result.getLong("record_id"))
            else -> summary + "\n" + text(R.string.ai_record_options_result,
                result.getJSONArray("assets").length(), result.getJSONArray("categories").length())
        }
    }

    private fun describe(record: Record): String = text(R.string.ai_record_description,
        record.date, text(when (record.type) { 0 -> R.string.record_type_expense; 1 -> R.string.record_type_income; else -> R.string.record_type_transfer }),
        Money.formatYuan(record.amount), record.categoryPathSnapshot ?: record.category,
        record.assetSource?.takeIf { it.isNotBlank() } ?: text(R.string.record_asset_no_selection),
        record.destinationAssetSource.orEmpty(), Money.formatYuan(record.fee), record.description.orEmpty())

    companion object {
        fun validDate(value: String): Boolean {
            if (!value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) || value.startsWith("0000")) return false
            return try {
                SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
                    calendar = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("UTC"), Locale.ROOT).apply {
                        gregorianChange = java.util.Date(Long.MIN_VALUE)
                    }
                    isLenient = false
                }.parse(value) != null
            }
            catch (_: java.text.ParseException) { false }
        }
        fun integer(args: JSONObject, name: String): Long {
            val value = args.get(name)
            require(value is Number)
            return requireNotNull(value.toString().toLongOrNull())
        }
        fun id(args: JSONObject, name: String): Long = integer(args, name).also { require(it > 0) }
        fun money(args: JSONObject, name: String, zeroAllowed: Boolean): Long {
            require(args.get(name) is String)
            return requireNotNull(Money.parseYuan(args.getString(name))).also { require(if (zeroAllowed) it >= 0 else it > 0) }
        }
        fun recordJson(record: Record): JSONObject = JSONObject().put("id", record.id).put("date", record.date)
            .put("amount", Money.formatYuan(record.amount)).put("type", record.type)
            .put("category_id", record.categoryId ?: JSONObject.NULL).put("category", record.categoryPathSnapshot ?: record.category)
            .put("asset_id", record.assetId ?: JSONObject.NULL).put("asset", record.assetSource.orEmpty())
            .put("destination_asset_id", record.destinationAssetId ?: JSONObject.NULL).put("destination_asset", record.destinationAssetSource.orEmpty())
            .put("fee", Money.formatYuan(record.fee)).put("description", record.description.orEmpty())
        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
