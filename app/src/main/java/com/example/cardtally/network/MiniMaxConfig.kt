package com.example.cardtally.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class MiniMaxConfig(
    val apiKey: String,
    val model: String = "",
    val requestUrl: String = ""
) {

    fun normalized(): MiniMaxConfig {
        val normalizedRequestUrl = normalizeRequestUrl(requestUrl)
        return MiniMaxConfig(
            apiKey = apiKey.trim(),
            model = model.trim(),
            requestUrl = normalizedRequestUrl
        )
    }

    fun isComplete(): Boolean {
        val normalized = normalized()
        return normalized.apiKey.isNotBlank() &&
            normalized.model.isNotBlank() &&
            normalized.requestUrl.isNotBlank()
    }

    /**
     * Whether the configured request URL can actually be used as an HTTP(S)
     * request target. [isComplete] only checks that a value exists, so an unusable
     * value (for example `not-a-url`) must be rejected before a request starts
     * rather than surfacing as a synchronous client failure.
     */
    fun hasValidRequestUrl(): Boolean {
        val candidate = normalized().requestUrl
        if (candidate.isBlank()) return false
        return candidate.toHttpUrlOrNull() != null
    }

    companion object {
        const val DEFAULT_MODEL = "M2-her"
        const val LEGACY_DEFAULT_BASE_URL = "https://api.minimax.io"
        const val DEFAULT_REQUEST_URL = "https://api.minimax.io/v1/text/chatcompletion_v2"

        private fun normalizeRequestUrl(rawValue: String): String {
            val trimmedValue = rawValue.trim()
            return when (trimmedValue.removeSuffix("/")) {
                LEGACY_DEFAULT_BASE_URL -> DEFAULT_REQUEST_URL
                else -> trimmedValue
            }
        }
    }
}
