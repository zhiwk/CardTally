package com.example.cardtally.network

import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniMaxPayloadParserTest {

    @Test
    fun buildRequestBody_includesModelAndMessages() {
        val requestBody = MiniMaxPayloadParser.buildRequestBody(
            model = "M2-her",
            messages = listOf(
                AiChatMessage(role = AiChatRole.USER, content = "你好"),
                AiChatMessage(role = AiChatRole.ASSISTANT, content = "你好，请说。")
            )
        )

        val json = JSONObject(requestBody)
        val messages = json.getJSONArray("messages")

        assertEquals("M2-her", json.getString("model"))
        assertEquals(2, messages.length())
        assertEquals("user", messages.getJSONObject(0).getString("role"))
        assertEquals("你好", messages.getJSONObject(0).getString("content"))
        assertEquals("assistant", messages.getJSONObject(1).getString("role"))
    }

    @Test
    fun parseAssistantReply_returnsAssistantMessage() {
        val responseBody = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "这是 MiniMax 的回复。"
                  }
                }
              ]
            }
        """.trimIndent()

        val result = MiniMaxPayloadParser.parseAssistantReply(responseBody)

        assertTrue(result is MiniMaxChatResult.Success)
        assertEquals("这是 MiniMax 的回复。", (result as MiniMaxChatResult.Success).reply)
    }

    @Test
    fun parseAssistantReply_rejectsMissingContent() {
        val responseBody = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "   "
                  }
                }
              ]
            }
        """.trimIndent()

        val result = MiniMaxPayloadParser.parseAssistantReply(responseBody)

        assertTrue(result is MiniMaxChatResult.Failure)
        assertEquals(MiniMaxErrorType.EMPTY_REPLY, (result as MiniMaxChatResult.Failure).type)
    }

    @Test
    fun parseHttpFailure_mapsAuthFailures() {
        val result = MiniMaxPayloadParser.parseHttpFailure(
            statusCode = 401,
            responseBody = """
                {
                  "base_resp": {
                    "status_code": 1004,
                    "status_msg": "Authentication failed"
                  }
                }
            """.trimIndent()
        )

        assertEquals(MiniMaxErrorType.AUTH, result.type)
        assertEquals(401, result.statusCode)
        assertEquals("Authentication failed", result.detail)
    }

    @Test
    fun parseHttpFailure_mapsRateLimitFailures() {
        val result = MiniMaxPayloadParser.parseHttpFailure(
            statusCode = 429,
            responseBody = """
                {
                  "base_resp": {
                    "status_code": 1002,
                    "status_msg": "Rate limit triggered"
                  }
                }
            """.trimIndent()
        )

        assertEquals(MiniMaxErrorType.RATE_LIMIT, result.type)
        assertEquals(429, result.statusCode)
    }

    @Test
    fun parseHttpFailure_mapsInsufficientBalanceFailures() {
        val result = MiniMaxPayloadParser.parseHttpFailure(
            statusCode = 402,
            responseBody = """
                {
                  "base_resp": {
                    "status_code": 1008,
                    "status_msg": "Insufficient balance"
                  }
                }
            """.trimIndent()
        )

        assertEquals(MiniMaxErrorType.INSUFFICIENT_BALANCE, result.type)
        assertEquals(402, result.statusCode)
    }

    @Test
    fun parseHttpFailure_mapsInvalidRequestFailures() {
        val result = MiniMaxPayloadParser.parseHttpFailure(
            statusCode = 400,
            responseBody = """
                {
                  "base_resp": {
                    "status_code": 2013,
                    "status_msg": "Parameter error"
                  }
                }
            """.trimIndent()
        )

        assertEquals(MiniMaxErrorType.INVALID_REQUEST, result.type)
        assertEquals(400, result.statusCode)
    }

    @Test
    fun parseHttpFailure_fallsBackToGenericHttpWithStatusCode() {
        val result = MiniMaxPayloadParser.parseHttpFailure(
            statusCode = 503,
            responseBody = """
                {
                  "message": "Service unavailable"
                }
            """.trimIndent()
        )

        assertEquals(MiniMaxErrorType.HTTP, result.type)
        assertEquals(503, result.statusCode)
        assertEquals("Service unavailable", result.detail)
    }

    @Test
    fun parseHttpFailure_handlesUnparseableBody() {
        val result = MiniMaxPayloadParser.parseHttpFailure(
            statusCode = 500,
            responseBody = "<html>bad gateway</html>"
        )

        assertEquals(MiniMaxErrorType.HTTP, result.type)
        assertEquals(500, result.statusCode)
        assertNull(result.detail)
    }
}
