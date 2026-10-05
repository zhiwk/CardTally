package com.example.cardtally.network

import com.example.cardtally.model.AiChatMessage
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object MiniMaxPayloadParser {

    private data class ErrorInfo(
        val detail: String?,
        val miniMaxStatusCode: Int?
    )

    fun buildRequestBody(model: String, messages: List<AiChatMessage>): String {
        val jsonMessages = JSONArray()
        messages
            .filter { !it.isError && !it.isLocalOnly && it.content.isNotBlank() }
            .forEach { message ->
                jsonMessages.put(
                    JSONObject()
                        .put("role", message.role.apiValue)
                        .put("content", message.content)
                )
            }

        return JSONObject()
            .put("model", model.trim())
            .put("messages", jsonMessages)
            .put("stream", true)
            .toString()
    }

    fun parseAssistantReply(responseBody: String): MiniMaxChatResult {
        if (responseBody.isBlank()) {
            return MiniMaxChatResult.Failure(MiniMaxErrorType.PARSE)
        }

        return try {
            val root = JSONObject(responseBody)
            val choice = root.optJSONArray("choices")?.optJSONObject(0)
                ?: return MiniMaxChatResult.Failure(MiniMaxErrorType.PARSE)
            val message = choice.optJSONObject("message")
                ?: return MiniMaxChatResult.Failure(MiniMaxErrorType.PARSE)
            val content = message.stringOrNull("content")?.trim().orEmpty()
            val reasoning = message.reasoningOrNull()

            if (content.isBlank()) {
                MiniMaxChatResult.Failure(MiniMaxErrorType.EMPTY_REPLY)
            } else {
                MiniMaxChatResult.Success(content, reasoning)
            }
        } catch (_: JSONException) {
            MiniMaxChatResult.Failure(MiniMaxErrorType.PARSE)
        }
    }

    /**
     * Parses a streaming SSE chunk and extracts delta content if present.
     * Returns null if the chunk is a non-data line (e.g., "event:" or empty).
     * Returns empty string for [DONE] marker.
     * Returns delta content if present in the data JSON.
     */
    fun parseStreamingChunk(chunkLine: String): StreamingParseResult {
        val trimmed = chunkLine.trim()
        
        // Handle empty lines
        if (trimmed.isEmpty()) {
            return StreamingParseResult.Empty
        }
        
        // SSE data lines start with "data:"
        val dataPrefix = "data:"
        val dataContent = when {
            trimmed.startsWith(dataPrefix) -> trimmed.substring(dataPrefix.length).trim()
            trimmed.startsWith("event:") || trimmed.startsWith("id:") || trimmed.startsWith(":") -> {
                // SSE metadata lines - ignore
                return StreamingParseResult.Empty
            }
            else -> trimmed
        }
        
        // Check for [DONE] marker
        if (dataContent == "[DONE]") {
            return StreamingParseResult.Done
        }
        
        // Try to parse as JSON
        return try {
            val json = JSONObject(dataContent)

            // MiniMax streaming format: choices[0].delta.content
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choice = choices.optJSONObject(0)
                val delta = choice?.optJSONObject("delta")
                val content = delta?.stringOrNull("content")
                val reasoning = delta?.reasoningOrNull()

                if (content != null || reasoning != null) {
                    StreamingParseResult.Content(content, reasoning)
                } else {
                    StreamingParseResult.Empty
                }
            } else {
                // Alternative format: top-level content / reasoning_content
                val content = json.stringOrNull("content")
                val reasoning = json.reasoningOrNull()
                if (content != null || reasoning != null) {
                    StreamingParseResult.Content(content, reasoning)
                } else {
                    StreamingParseResult.Empty
                }
            }
        } catch (_: JSONException) {
            // Not valid JSON, return empty
            StreamingParseResult.Empty
        }
    }

    /**
     * Reads a string field, treating a JSON `null` as absent.
     *
     * `JSONObject.optString` stringifies `JSONObject.NULL` to the literal "null",
     * which is how streamed `content: null` chunks used to leak "null" into replies.
     */
    private fun JSONObject.stringOrNull(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).takeIf { it.isNotEmpty() }
    }

    private fun JSONObject.reasoningOrNull(): String? =
        stringOrNull("reasoning_content") ?: stringOrNull("reasoning")
    
    /**
     * Checks if a response body appears to be a streaming response based on its shape.
     * A streaming response typically contains lines starting with "data:" or is not a complete JSON object.
     */
    fun isStreamingResponse(responseBody: String): Boolean {
        if (responseBody.isBlank()) return false

        val trimmed = responseBody.trim()

        // Check for SSE format markers first (strong indicator of streaming)
        if (trimmed.startsWith("data:")) return true
        if (trimmed.contains("\ndata:")) return true

        // If it's valid complete JSON with choices array, check the structure
        return try {
            val root = JSONObject(trimmed)
            // If it has choices with a complete message (not delta), it's non-streaming
            val choices = root.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choice = choices.optJSONObject(0)
                val hasMessage = choice?.has("message") == true
                val hasDelta = choice?.has("delta") == true
                // Streaming if it has delta; non-streaming if it has message without delta
                if (hasDelta) return true
                if (hasMessage) return false
            }
            // Check if it's a complete response structure (has id, created, etc.)
            if (root.has("id") && root.has("created")) {
                return false
            }
            // Default to streaming for ambiguous cases
            true
        } catch (_: JSONException) {
            // Not valid JSON - check for line-delimited JSON format
            val lines = trimmed.lines()
            if (lines.size > 1) {
                // Multiple non-empty lines of JSON objects suggests streaming
                val nonEmptyLines = lines.filter { it.isNotBlank() }
                if (nonEmptyLines.size > 1) {
                    // Check if multiple lines look like JSON objects
                    val jsonObjectLines = nonEmptyLines.count { it.trim().startsWith("{") && it.trim().endsWith("}") }
                    return jsonObjectLines > 1
                }
            }
            // Single line or not valid JSON - could be either, but likely streaming if not valid JSON
            true
        }
    }

    fun parseHttpFailure(statusCode: Int, responseBody: String?): MiniMaxChatResult.Failure {
        val errorInfo = parseErrorInfo(responseBody)
        val detail = errorInfo.detail

        val type = when {
            statusCode == 401 || statusCode == 403 || errorInfo.miniMaxStatusCode == 1004 ||
                containsAny(detail, "auth", "authentication", "api key", "unauthorized", "forbidden") -> {
                MiniMaxErrorType.AUTH
            }

            statusCode == 429 || errorInfo.miniMaxStatusCode == 1002 ||
                containsAny(detail, "rate limit", "too many requests", "rate-limited") -> {
                MiniMaxErrorType.RATE_LIMIT
            }

            statusCode == 402 || errorInfo.miniMaxStatusCode == 1008 ||
                containsAny(detail, "insufficient balance", "balance", "recharge", "quota") -> {
                MiniMaxErrorType.INSUFFICIENT_BALANCE
            }

            statusCode == 400 || statusCode == 404 || statusCode == 422 || errorInfo.miniMaxStatusCode == 2013 ||
                containsAny(detail, "parameter", "param", "bad request", "model", "not found", "endpoint") -> {
                MiniMaxErrorType.INVALID_REQUEST
            }

            else -> MiniMaxErrorType.HTTP
        }

        return MiniMaxChatResult.Failure(
            type = type,
            detail = detail,
            statusCode = statusCode
        )
    }

    fun parseErrorDetail(responseBody: String?): String? {
        return parseErrorInfo(responseBody).detail
    }

    private fun parseErrorInfo(responseBody: String?): ErrorInfo {
        if (responseBody.isNullOrBlank()) {
            return ErrorInfo(detail = null, miniMaxStatusCode = null)
        }

        return try {
            val root = JSONObject(responseBody)
            val baseResp = root.optJSONObject("base_resp")
            ErrorInfo(
                detail = firstNonBlank(
                    baseResp?.stringOrNull("status_msg"),
                    root.stringOrNull("message"),
                    root.stringOrNull("msg"),
                    root.stringOrNull("detail"),
                    root.optJSONObject("error")?.stringOrNull("message")
                ),
                miniMaxStatusCode = baseResp?.takeIf { it.has("status_code") }?.optInt("status_code")
            )
        } catch (_: JSONException) {
            ErrorInfo(detail = null, miniMaxStatusCode = null)
        }
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun containsAny(source: String?, vararg needles: String): Boolean {
        if (source.isNullOrBlank()) {
            return false
        }

        val normalizedSource = source.lowercase()
        return needles.any { normalizedSource.contains(it.lowercase()) }
    }
    
    /**
     * Result of parsing a streaming chunk.
     */
    sealed class StreamingParseResult {
        object Empty : StreamingParseResult()
        object Done : StreamingParseResult()
        data class Content(val content: String?, val reasoning: String?) : StreamingParseResult()
    }
}