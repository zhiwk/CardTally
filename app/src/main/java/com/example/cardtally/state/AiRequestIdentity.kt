package com.example.cardtally.state

/**
 * Pure per-request identity tracking for AI sends. Each send binds an immutable
 * request id and session id. A callback is accepted only when *both* match the
 * currently active request, so a stopped request (whose id is invalidated)
 * cannot resolve a later request's state, and a request that belongs to a
 * session the user has already left cannot write into the newly active session.
 *
 * Kept free of Android types so the stop→restart→late-callback race can be
 * exercised deterministically on the JVM.
 */
class AiRequestIdentity {

    private var sequence = 0L
    private var activeId: Long? = null
    private var activeSessionId: Long? = null

    val isActive: Boolean
        get() = activeId != null

    /** The session the active request belongs to, or null when idle. */
    val sessionId: Long?
        get() = activeSessionId

    fun begin(sessionId: Long): Long {
        sequence += 1
        activeId = sequence
        activeSessionId = sessionId
        return sequence
    }

    fun isCurrent(requestId: Long, sessionId: Long): Boolean =
        activeId == requestId && activeSessionId == sessionId

    fun invalidate() {
        activeId = null
        activeSessionId = null
    }

    fun complete(requestId: Long) {
        if (activeId == requestId) {
            activeId = null
            activeSessionId = null
        }
    }

    /** Completes [requestId] only if it is still current; returns whether it was. */
    fun completeAndIsCurrent(requestId: Long): Boolean {
        if (activeId == requestId) {
            activeId = null
            activeSessionId = null
            return true
        }
        return false
    }
}
