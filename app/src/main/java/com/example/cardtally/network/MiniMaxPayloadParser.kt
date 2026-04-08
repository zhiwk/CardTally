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
            .filter { !it.isError && it.content.isNotBlank() }
            .forEach { message ->
                jsonMessages.put(
                    JSONObject()
                        .put("role", message.role.apiValue)
                        .put("content", message.content)
                )
            }

        return JSONObject()
            .put("model", model.trim().ifBlank { MiniMaxConfig.DEFAULT_MODEL })
            .put("messages", jsonMessages)
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
            val message = choice?.optJSONObject("message")
                ?: return MiniMaxChatResult.Failure(MiniMaxErrorType.PARSE)
            val content = message?.optString("content").orEmpty().trim()

            if (content.isBlank()) {
                MiniMaxChatResult.Failure(MiniMaxErrorType.EMPTY_REPLY)
            } else {
                MiniMaxChatResult.Success(content)
            }
        } catch (_: JSONException) {
            MiniMaxChatResult.Failure(MiniMaxErrorType.PARSE)
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
                    baseResp?.optString("status_msg"),
                    root.optString("message"),
                    root.optString("msg"),
                    root.optString("detail"),
                    root.optJSONObject("error")?.optString("message")
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
}
