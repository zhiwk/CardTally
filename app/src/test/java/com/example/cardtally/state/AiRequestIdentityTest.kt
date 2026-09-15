package com.example.cardtally.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRequestIdentityTest {

    @Test
    fun `request is current only while active`() {
        val identity = AiRequestIdentity()
        val requestA = identity.begin(sessionId = 1L)
        assertTrue(identity.isCurrent(requestA, 1L))
    }

    @Test
    fun `stopping invalidates request so late callbacks are dropped`() {
        val identity = AiRequestIdentity()
        val requestA = identity.begin(1L)

        identity.invalidate()

        assertFalse("A is stopped, late A callbacks must be rejected", identity.isCurrent(requestA, 1L))
    }

    @Test
    fun `restart B keeps A stale while B is active`() {
        val identity = AiRequestIdentity()
        val requestA = identity.begin(1L)

        // User stops A, then immediately sends B in the same session.
        identity.invalidate()
        val requestB = identity.begin(1L)

        // A's late chunk/terminal must not be accepted.
        assertFalse(identity.isCurrent(requestA, 1L))
        // B's callbacks are accepted.
        assertTrue(identity.isCurrent(requestB, 1L))

        // A's late terminal must not complete B.
        identity.complete(requestA)
        assertTrue("A completion must not touch B", identity.isCurrent(requestB, 1L))
    }

    @Test
    fun `completing current request clears active state`() {
        val identity = AiRequestIdentity()
        val requestB = identity.begin(2L)
        assertTrue(identity.isActive)

        identity.complete(requestB)

        assertFalse(identity.isActive)
        assertFalse(identity.isCurrent(requestB, 2L))
    }

    @Test
    fun `session switch invalidates prior request`() {
        val identity = AiRequestIdentity()
        val requestA = identity.begin(1L)

        identity.invalidate()
        val requestB = identity.begin(2L)

        assertFalse(identity.isCurrent(requestA, 1L))
        assertTrue(identity.isCurrent(requestB, 2L))
        assertEquals(2L, requestB)
    }

    @Test
    fun `orchestrated stop restart keeps B text and state unaffected by A`() {
        val identity = AiRequestIdentity()
        // Mimic the fragment's guard: a result only mutates the list when the
        // request is still current. Kept as a local model so the acceptance
        // scenario runs deterministically on the JVM.
        val messageLog = mutableListOf<String>()
        fun accept(requestId: Long, text: String) {
            if (identity.isCurrent(requestId, 1L)) messageLog.add(text)
        }

        // A starts streaming, first chunk lands.
        val requestA = identity.begin(sessionId = 1L)
        accept(requestA, "A part 1")

        // User stops A (invalidate), then immediately sends B.
        identity.invalidate()
        val requestB = identity.begin(sessionId = 1L)

        // A's late chunk arrives after B started: must be dropped.
        accept(requestA, "A part 2 (late chunk)")

        // A's late success/failure arrives: must not finish B.
        identity.complete(requestA)
        assertTrue("B still in flight, A completion must not end it", identity.isCurrent(requestB, 1L))

        // B streams and completes.
        accept(requestB, "B reply")
        identity.complete(requestB)

        assertFalse(identity.isActive)
        // The persisted final message is exactly B's reply; A's late text is absent.
        assertEquals(listOf("A part 1", "B reply"), messageLog)
        assertEquals("B reply", messageLog.last())
    }

    @Test
    fun `late callbacks for an invalidated request never resolve a newer one`() {
        val identity = AiRequestIdentity()
        val requestA = identity.begin(1L)
        identity.invalidate()
        val requestB = identity.begin(1L)

        // A's terminal (success and failure) attempts must not resolve B.
        assertFalse(identity.completeAndIsCurrent(requestA))
        assertTrue(identity.isCurrent(requestB, 1L))
    }

    @Test
    fun `request identity is bound to its session`() {
        val identity = AiRequestIdentity()
        val requestId = identity.begin(sessionId = 7L)

        assertEquals(7L, identity.sessionId)
        assertTrue(identity.isCurrent(requestId, 7L))
        assertFalse("a different session must not accept the same request id", identity.isCurrent(requestId, 8L))
    }

    @Test
    fun `switching session drops the previous session callbacks`() {
        val identity = AiRequestIdentity()
        val requestInOldSession = identity.begin(sessionId = 1L)

        // User switches session; the pending request is invalidated and a new one
        // starts in the new session.
        identity.invalidate()
        val requestInNewSession = identity.begin(sessionId = 2L)

        assertFalse(identity.isCurrent(requestInOldSession, 1L))
        assertFalse(identity.isCurrent(requestInOldSession, 2L))
        assertTrue(identity.isCurrent(requestInNewSession, 2L))
        assertFalse(identity.isCurrent(requestInNewSession, 1L))
    }

    @Test
    fun `invalidate clears the tracked session`() {
        val identity = AiRequestIdentity()
        identity.begin(sessionId = 3L)

        identity.invalidate()

        assertFalse(identity.isActive)
        assertEquals(null, identity.sessionId)
    }
}
