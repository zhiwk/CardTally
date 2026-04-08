package com.example.cardtally.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniMaxConfigTest {

    @Test
    fun normalized_trimsValuesAndPreservesFullRequestUrl() {
        val config = MiniMaxConfig(
            apiKey = "  secret-key  ",
            model = "  MiniMax-Text-01  ",
            requestUrl = " https://api.minimax.io/v1/text/chatcompletion_v2 "
        )

        val normalized = config.normalized()

        assertEquals("secret-key", normalized.apiKey)
        assertEquals("MiniMax-Text-01", normalized.model)
        assertEquals("https://api.minimax.io/v1/text/chatcompletion_v2", normalized.requestUrl)
    }

    @Test
    fun isComplete_requiresNonBlankApiKey() {
        assertFalse(
            MiniMaxConfig(
                apiKey = "   ",
                model = "M2-her",
                requestUrl = "https://api.minimax.io/v1/text/chatcompletion_v2"
            ).isComplete()
        )

        assertTrue(
            MiniMaxConfig(
                apiKey = "secret-key",
                model = "M2-her",
                requestUrl = "https://api.minimax.io/v1/text/chatcompletion_v2"
            ).isComplete()
        )
    }

    @Test
    fun normalized_usesFullRequestUrlDefaultWhenBlank() {
        val normalized = MiniMaxConfig(
            apiKey = "secret-key",
            model = "",
            requestUrl = "   "
        ).normalized()

        assertEquals(MiniMaxConfig.DEFAULT_MODEL, normalized.model)
        assertEquals(MiniMaxConfig.DEFAULT_REQUEST_URL, normalized.requestUrl)
    }

    @Test
    fun normalized_migratesLegacyDefaultBaseUrlToFullRequestUrl() {
        val normalized = MiniMaxConfig(
            apiKey = "secret-key",
            model = "M2-her",
            requestUrl = "https://api.minimax.io"
        ).normalized()

        assertEquals(MiniMaxConfig.DEFAULT_REQUEST_URL, normalized.requestUrl)
    }
}
