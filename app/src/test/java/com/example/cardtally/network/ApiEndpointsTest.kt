package com.example.cardtally.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiEndpointsTest {
    @Test
    fun baseUrls_resolveWithoutChoosingAProvider() {
        val root = requireNotNull(ApiEndpoints.from(" https://service.example/ "))
        assertEquals("https://service.example/v1/models", root.models.toString())
        assertEquals("https://service.example/v1/chat/completions", root.chat.toString())
        val gateway = requireNotNull(ApiEndpoints.from("https://service.example/gateway/v1/"))
        assertEquals("/gateway/v1/models", gateway.models.encodedPath)
        assertEquals("/gateway/v1/chat/completions", gateway.chat.encodedPath)
    }

    @Test
    fun fullChatUrl_preservesPrefixAndQuery() {
        val endpoints = requireNotNull(ApiEndpoints.from("https://service.example/proxy/v1/chat/completions?version=2"))
        assertEquals("/proxy/v1/models", endpoints.models.encodedPath)
        assertEquals("version=2", endpoints.models.query)
        assertEquals("/proxy/v1/chat/completions", endpoints.chat.encodedPath)
    }

    @Test
    fun existingMiniMaxEndpoint_keepsChatPath() {
        val url = "https://api.minimax.io/v1/text/chatcompletion_v2"
        val endpoints = requireNotNull(ApiEndpoints.from(url))
        assertEquals(url, endpoints.chat.toString())
        assertEquals("https://api.minimax.io/v1/models", endpoints.models.toString())
    }

    @Test
    fun invalidUrlsAndUnsafeHeaders_areRejected() {
        listOf("", "not-a-url", "ftp://service.example", "https://user:password@service.example", "https://service.example/#fragment")
            .forEach { assertNull(ApiEndpoints.from(it)) }
        assertTrue(ApiEndpoints.isValidApiKey(" key-value "))
        listOf("", "  ", "key\nvalue", "key value", "密钥").forEach { assertFalse(ApiEndpoints.isValidApiKey(it)) }
    }
}
