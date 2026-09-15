package com.example.cardtally.state

import android.os.Bundle
import com.example.cardtally.network.MiniMaxRequestHandle

object StableIdResolver {
    fun resolve(savedId: Long?, existingIds: Set<Long>): Long? = savedId?.takeIf(existingIds::contains)
}

class InFlightAiLifecycle {
    var isLoading: Boolean = false
        private set

    private var activeHandle: MiniMaxRequestHandle? = null
    private var completed = false

    /**
     * Binds the request handle. A sender may report a failure synchronously —
     * before it returns its handle — in which case [markFinished] has already run
     * and flagging the request as loading again would strand the page in a
     * permanent busy state with nothing left to cancel. Marking a finished
     * request as started is therefore ignored until the next request begins.
     */
    fun markStarted(handle: MiniMaxRequestHandle) {
        if (completed) return
        activeHandle = handle
        isLoading = true
    }

    fun markFinished() {
        activeHandle = null
        isLoading = false
        completed = true
    }

    fun cancelAndReset() {
        if (isLoading) {
            activeHandle?.cancel()
        }
        activeHandle = null
        isLoading = false
        completed = false
    }

    /**
     * Registers the in-flight request before the sender is invoked, so a failure
     * reported synchronously can still be cancelled/cleared correctly.
     */
    fun registerPending() {
        activeHandle = null
        isLoading = true
        completed = false
    }
}

internal fun Bundle.putNullableLong(key: String, value: Long?) {
    if (value == null) remove(key) else putLong(key, value)
}

internal fun Bundle.getNullableLong(key: String): Long? = if (containsKey(key)) getLong(key) else null

internal fun Bundle.putNullableString(key: String, value: String?) {
    if (value == null) remove(key) else putString(key, value)
}
