package com.example.cardtally.state

import com.example.cardtally.network.MiniMaxRequestHandle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InFlightAiLifecycleTest {

    private fun handle(onCancel: () -> Unit = {}): MiniMaxRequestHandle =
        MiniMaxRequestHandle(onCancel)

    @Test
    fun `normal request loads then finishes`() {
        val lifecycle = InFlightAiLifecycle()
        lifecycle.registerPending()
        assertTrue(lifecycle.isLoading)

        lifecycle.markStarted(handle())
        assertTrue(lifecycle.isLoading)

        lifecycle.markFinished()
        assertFalse(lifecycle.isLoading)
    }

    @Test
    fun `synchronous failure cannot be re-marked as loading`() {
        val lifecycle = InFlightAiLifecycle()
        lifecycle.registerPending()

        // The sender reports a failure before returning its handle.
        lifecycle.markFinished()
        assertFalse(lifecycle.isLoading)

        // The handle then arrives late; it must not strand the page as busy.
        lifecycle.markStarted(handle())
        assertFalse(
            "a finished request must not become loading again",
            lifecycle.isLoading
        )
    }

    @Test
    fun `next request is allowed to become loading after a synchronous failure`() {
        val lifecycle = InFlightAiLifecycle()
        lifecycle.registerPending()
        lifecycle.markFinished()
        lifecycle.markStarted(handle())

        // User retries: a brand new request starts normally.
        lifecycle.registerPending()
        assertTrue(lifecycle.isLoading)
        lifecycle.markStarted(handle())
        assertTrue(lifecycle.isLoading)
        lifecycle.markFinished()
        assertFalse(lifecycle.isLoading)
    }

    @Test
    fun `cancelAndReset cancels the active handle and clears state`() {
        var cancelled = false
        val lifecycle = InFlightAiLifecycle()
        lifecycle.registerPending()
        lifecycle.markStarted(handle { cancelled = true })

        lifecycle.cancelAndReset()

        assertTrue("the in-flight request must be cancelled", cancelled)
        assertFalse(lifecycle.isLoading)
        // After an explicit reset the next request starts clean.
        lifecycle.registerPending()
        assertTrue(lifecycle.isLoading)
    }

    @Test
    fun `markFinished without an active handle is safe`() {
        val lifecycle = InFlightAiLifecycle()
        lifecycle.markFinished()
        assertFalse(lifecycle.isLoading)
        lifecycle.markStarted(handle())
        assertFalse(lifecycle.isLoading)
    }
}
