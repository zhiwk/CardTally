package com.example.cardtally.network

import android.util.Log
import com.example.cardtally.model.AiChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import okio.BufferedSource

/**
 * A per-request cancellation handle. Owning a handle lets a caller cancel exactly
 * the request it created without disturbing any newer request, so a user stop of
 * request A cannot be re-enabled by starting request B.
 */
class MiniMaxRequestHandle internal constructor(
    private val cancelAction: () -> Unit
) {
    internal val cancelled = AtomicBoolean(false)
    private val call = AtomicReference<okhttp3.Call?>(null)

    internal fun attachCall(newCall: okhttp3.Call) {
        call.set(newCall)
    }

    fun cancel() {
        if (cancelled.compareAndSet(false, true)) {
            cancelAction()
            call.getAndSet(null)?.cancel()
        }
    }

    fun isCancelled(): Boolean = cancelled.get()
}

/**
 * The minimal contract the Agent page uses to start an AI request. A fake can
 * implement this in a test to deterministically drive stop/restart/late-callback
 * sequences without a real network call.
 */
fun interface AiChatSender {
    fun sendChat(
        config: MiniMaxConfig,
        messages: List<AiChatMessage>,
        callback: (MiniMaxChatResult) -> Unit
    ): MiniMaxRequestHandle
}

class MiniMaxClient(
    private val okHttpClient: OkHttpClient = DEFAULT_CLIENT
) : AiChatSender {

    // The single most-recent in-flight call, kept only so a legacy broad cancel
    // can be routed to the current request if needed. Each handle owns its own
    // cancellation flag and call reference.
    private val activeCall = AtomicReference<okhttp3.Call?>(null)

    override fun sendChat(
        config: MiniMaxConfig,
        messages: List<AiChatMessage>,
        callback: (MiniMaxChatResult) -> Unit
    ): MiniMaxRequestHandle {
        val handle = MiniMaxRequestHandle(cancelAction = { activeCall.compareAndSet(activeCall.get(), null) })

        val normalizedConfig = config.normalized()
        if (!normalizedConfig.isComplete()) {
            callback(MiniMaxChatResult.Failure(MiniMaxErrorType.INVALID_CONFIG))
            return handle
        }

        val requestBody = MiniMaxPayloadParser.buildRequestBody(
            model = normalizedConfig.model,
            messages = messages
        ).toRequestBody(JSON_MEDIA_TYPE)

        val request = try {
            Request.Builder()
                .url(normalizedConfig.requestUrl)
                .addHeader("Authorization", "Bearer ${normalizedConfig.apiKey}")
                .addHeader("Accept", "text/event-stream, application/json")
                .post(requestBody)
                .build()
        } catch (_: IllegalArgumentException) {
            callback(MiniMaxChatResult.Failure(MiniMaxErrorType.INVALID_URL))
            return handle
        }

        val call = okHttpClient.newCall(request)
        handle.attachCall(call)
        activeCall.set(call)

        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                activeCall.compareAndSet(call, null)

                if (handle.isCancelled()) {
                    callback(MiniMaxChatResult.Failure(MiniMaxErrorType.CANCELLED))
                    return
                }

                val errorType = when (e) {
                    is SocketTimeoutException -> MiniMaxErrorType.TIMEOUT
                    is InterruptedIOException -> {
                        if (handle.isCancelled()) MiniMaxErrorType.CANCELLED else MiniMaxErrorType.INTERRUPTED
                    }
                    else -> MiniMaxErrorType.NETWORK
                }

                Log.e(TAG, "MiniMax network failure", e)
                callback(
                    MiniMaxChatResult.Failure(
                        type = errorType,
                        detail = e.localizedMessage
                    )
                )
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                activeCall.compareAndSet(call, null)

                if (handle.isCancelled()) {
                    callback(MiniMaxChatResult.Failure(MiniMaxErrorType.CANCELLED))
                    response.close()
                    return
                }

                response.use { resp ->
                    if (!resp.isSuccessful) {
                        handleHttpError(resp, callback)
                        return
                    }

                    val body = resp.body
                    if (body == null) {
                        callback(MiniMaxChatResult.Failure(MiniMaxErrorType.EMPTY_REPLY))
                        return
                    }

                    handleSuccessBody(body.source(), callback, handle)
                }
            }
        })

        return handle
    }

    fun cancel() {
        // Legacy broad cancel: cancels whatever single call is currently active.
        val call = activeCall.getAndSet(null)
        call?.cancel()
    }

    private fun handleHttpError(response: okhttp3.Response, callback: (MiniMaxChatResult) -> Unit) {
        val body = response.body?.string().orEmpty()
        Log.w(
            TAG,
            "MiniMax HTTP ${response.code} ${response.message}. Response body: ${body.truncateForLog()}"
        )
        callback(
            MiniMaxPayloadParser.parseHttpFailure(
                statusCode = response.code,
                responseBody = body
            )
        )
    }

    private fun handleSuccessBody(
        source: BufferedSource,
        callback: (MiniMaxChatResult) -> Unit,
        handle: MiniMaxRequestHandle
    ) {
        try {
            source.use { bufferedSource ->
                val bufferedLines = mutableListOf<String>()

                while (true) {
                    if (handle.isCancelled()) {
                        callback(MiniMaxChatResult.Failure(MiniMaxErrorType.CANCELLED))
                        return
                    }

                    val line = bufferedSource.readUtf8Line()
                    if (line == null) {
                        val fullBody = bufferedLines.joinToString("\n")
                        if (fullBody.isBlank()) {
                            callback(MiniMaxChatResult.Failure(MiniMaxErrorType.EMPTY_REPLY))
                        } else {
                            callback(MiniMaxPayloadParser.parseAssistantReply(fullBody))
                        }
                        return
                    }

                    bufferedLines.add(line)

                    if (line.isBlank()) {
                        continue
                    }

                    val parsedChunk = MiniMaxPayloadParser.parseStreamingChunk(line)
                    val trimmed = line.trim()
                    val looksLikeStreamingFrame = parsedChunk !is MiniMaxPayloadParser.StreamingParseResult.Empty ||
                        trimmed.startsWith("data:") ||
                        trimmed.startsWith("event:") ||
                        trimmed.startsWith("id:") ||
                        trimmed.startsWith(":")

                    if (looksLikeStreamingFrame) {
                        processStreamingSource(
                            source = bufferedSource,
                            firstChunk = parsedChunk,
                            callback = callback,
                            handle = handle
                        )
                        return
                    }

                    val remainder = bufferedSource.readUtf8()
                    val fullBody = buildString {
                        append(bufferedLines.joinToString("\n"))
                        if (remainder.isNotEmpty()) {
                            append("\n")
                            append(remainder)
                        }
                    }

                    if (handle.isCancelled()) {
                        callback(MiniMaxChatResult.Failure(MiniMaxErrorType.CANCELLED))
                    } else {
                        callback(MiniMaxPayloadParser.parseAssistantReply(fullBody))
                    }
                    return
                }
            }
        } catch (e: SocketTimeoutException) {
            callback(MiniMaxChatResult.Failure(MiniMaxErrorType.TIMEOUT, e.localizedMessage))
        } catch (e: InterruptedIOException) {
            callback(
                MiniMaxChatResult.Failure(
                    if (handle.isCancelled()) MiniMaxErrorType.CANCELLED else MiniMaxErrorType.INTERRUPTED,
                    e.localizedMessage
                )
            )
        } catch (e: IOException) {
            callback(
                MiniMaxChatResult.Failure(
                    if (handle.isCancelled()) MiniMaxErrorType.CANCELLED else MiniMaxErrorType.NETWORK,
                    e.localizedMessage
                )
            )
        }
    }

    private fun processStreamingSource(
        source: BufferedSource,
        firstChunk: MiniMaxPayloadParser.StreamingParseResult,
        callback: (MiniMaxChatResult) -> Unit,
        handle: MiniMaxRequestHandle
    ) {
        val accumulatedContent = StringBuilder()
        val accumulatedReasoning = StringBuilder()

        fun emitChunkResult(result: MiniMaxPayloadParser.StreamingParseResult): Boolean {
            return when (result) {
                is MiniMaxPayloadParser.StreamingParseResult.Content -> {
                    result.content?.let { accumulatedContent.append(it) }
                    result.reasoning?.let { accumulatedReasoning.append(it) }
                    callback(
                        MiniMaxChatResult.StreamingChunk(
                            accumulatedContent.toString(),
                            accumulatedReasoning.toString()
                        )
                    )
                    false
                }
                is MiniMaxPayloadParser.StreamingParseResult.Done -> {
                    val finalContent = accumulatedContent.toString()
                    if (finalContent.isNotBlank()) {
                        callback(MiniMaxChatResult.StreamingDone(finalContent, accumulatedReasoning.toString()))
                    } else {
                        callback(MiniMaxChatResult.Failure(MiniMaxErrorType.EMPTY_REPLY))
                    }
                    true
                }
                is MiniMaxPayloadParser.StreamingParseResult.Empty -> false
            }
        }

        try {
            if (emitChunkResult(firstChunk)) {
                return
            }

            while (true) {
                if (handle.isCancelled()) {
                    val partial = accumulatedContent.toString()
                    callback(
                        MiniMaxChatResult.Failure(
                            type = MiniMaxErrorType.CANCELLED,
                            detail = partial.ifBlank { null }
                        )
                    )
                    return
                }

                val line = source.readUtf8Line()
                if (line == null) {
                    val finalContent = accumulatedContent.toString()
                    if (finalContent.isNotBlank()) {
                        callback(MiniMaxChatResult.StreamingDone(finalContent, accumulatedReasoning.toString()))
                    } else {
                        callback(MiniMaxChatResult.Failure(MiniMaxErrorType.EMPTY_REPLY))
                    }
                    return
                }

                if (emitChunkResult(MiniMaxPayloadParser.parseStreamingChunk(line))) {
                    return
                }
            }
        } catch (e: SocketTimeoutException) {
            val partial = accumulatedContent.toString()
            callback(
                MiniMaxChatResult.Failure(
                    type = MiniMaxErrorType.TIMEOUT,
                    detail = partial.ifBlank { e.localizedMessage }
                )
            )
        } catch (e: InterruptedIOException) {
            val partial = accumulatedContent.toString()
            callback(
                MiniMaxChatResult.Failure(
                    type = if (handle.isCancelled()) MiniMaxErrorType.CANCELLED else MiniMaxErrorType.INTERRUPTED,
                    detail = partial.ifBlank { e.localizedMessage }
                )
            )
        } catch (e: IOException) {
            val partial = accumulatedContent.toString()
            callback(
                MiniMaxChatResult.Failure(
                    type = if (partial.isNotBlank()) MiniMaxErrorType.INTERRUPTED else if (handle.isCancelled()) MiniMaxErrorType.CANCELLED else MiniMaxErrorType.NETWORK,
                    detail = partial.ifBlank { e.localizedMessage }
                )
            )
        }
    }

    private companion object {
        private const val TAG = "MiniMaxClient"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun String.truncateForLog(maxLength: Int = 2000): String {
        return if (length <= maxLength) this else take(maxLength) + "…"
    }
}
