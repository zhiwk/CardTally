package com.example.cardtally.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Resolves an OpenAI-compatible base URL or a full chat URL without choosing a provider. */
data class ApiEndpoints(val chat: HttpUrl, val models: HttpUrl) {
    companion object {
        fun from(rawUrl: String): ApiEndpoints? {
            val url = rawUrl.trim().toHttpUrlOrNull() ?: return null
            if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null) return null
            val path = url.encodedPath.trimEnd('/')
            val chatPath: String
            val prefix: String
            when {
                path.endsWith("/chat/completions") -> {
                    prefix = path.removeSuffix("/chat/completions")
                    chatPath = path
                }
                // Existing MiniMax text-chat configurations keep their original chat endpoint.
                path.endsWith("/text/chatcompletion_v2") -> {
                    prefix = path.removeSuffix("/text/chatcompletion_v2")
                    chatPath = path
                }
                else -> {
                    prefix = path.ifEmpty { "/v1" }
                    chatPath = "$prefix/chat/completions"
                }
            }
            return ApiEndpoints(
                chat = url.newBuilder().encodedPath(chatPath).build(),
                models = url.newBuilder().encodedPath("$prefix/models").build()
            )
        }

        fun isValidApiKey(value: String): Boolean =
            value.trim().isNotEmpty() && value.trim().all { it in '!'..'~' }
    }
}
