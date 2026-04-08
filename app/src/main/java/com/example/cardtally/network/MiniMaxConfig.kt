package com.example.cardtally.network

data class MiniMaxConfig(
    val apiKey: String,
    val model: String = DEFAULT_MODEL,
    val requestUrl: String = DEFAULT_REQUEST_URL
) {

    fun normalized(): MiniMaxConfig {
        val normalizedRequestUrl = normalizeRequestUrl(requestUrl)
        return MiniMaxConfig(
            apiKey = apiKey.trim(),
            model = model.trim().ifBlank { DEFAULT_MODEL },
            requestUrl = normalizedRequestUrl
        )
    }

    fun isComplete(): Boolean {
        val normalized = normalized()
        return normalized.apiKey.isNotBlank() &&
            normalized.model.isNotBlank() &&
            normalized.requestUrl.isNotBlank()
    }

    companion object {
        const val DEFAULT_MODEL = "M2-her"
        const val LEGACY_DEFAULT_BASE_URL = "https://api.minimax.io"
        const val DEFAULT_REQUEST_URL = "https://api.minimax.io/v1/text/chatcompletion_v2"

        private fun normalizeRequestUrl(rawValue: String): String {
            val trimmedValue = rawValue.trim()
            if (trimmedValue.isBlank()) {
                return DEFAULT_REQUEST_URL
            }

            return when (trimmedValue.removeSuffix("/")) {
                LEGACY_DEFAULT_BASE_URL -> DEFAULT_REQUEST_URL
                else -> trimmedValue
            }
        }
    }
}
