package com.example.cardtally.state

import android.os.Bundle

object StableIdResolver {
    fun resolve(savedId: Long?, existingIds: Set<Long>): Long? = savedId?.takeIf(existingIds::contains)
}

class InFlightAiLifecycle(private val cancelRequest: () -> Unit) {
    var isLoading: Boolean = false
        private set

    fun markStarted() {
        isLoading = true
    }

    fun markFinished() {
        isLoading = false
    }

    fun cancelAndReset() {
        if (isLoading) cancelRequest()
        isLoading = false
    }
}

internal fun Bundle.putNullableLong(key: String, value: Long?) {
    if (value == null) remove(key) else putLong(key, value)
}

internal fun Bundle.getNullableLong(key: String): Long? = if (containsKey(key)) getLong(key) else null

internal fun Bundle.putNullableString(key: String, value: String?) {
    if (value == null) remove(key) else putString(key, value)
}
