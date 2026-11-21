package com.example.cardtally.cloud

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Shared local validation for manual entry and QR transfer. No requests are made. */
internal object CloudConnectionRules {
    fun validEndpoint(endpoint: String): Boolean {
        val url = endpoint.toHttpUrlOrNull() ?: return false
        return url.scheme == "https" && url.username.isEmpty() && url.password.isEmpty() &&
            url.query == null && url.fragment == null
    }

    fun validPrefix(prefix: String): Boolean =
        prefix.trim('/').split('/').none { it == "." || it == ".." }
}
