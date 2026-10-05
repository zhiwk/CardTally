package com.example.cardtally.network

import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun buildRequestBody_includesStreamTrue() {
        val requestBody = MiniMaxPayloadParser.buildRequestBody(
            model = "M2-her",
            messages = listOf(AiChatMessage(role = AiChatRole.USER, content = "Hello"))
        )

        val json = JSONObject(requestBody)
        assertTrue("Request body should include stream=true", json.getBoolean("stream"))
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
    fun parseStreamingChunk_extractsContentFromDelta() {
        val chunk = "data: {\"choices\": [{\"delta\": {\"content\": \"Hello\"}}]}"
        
        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)
        
        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Content)
        assertEquals("Hello", (result as MiniMaxPayloadParser.StreamingParseResult.Content).content)
    }

    @Test
    fun parseStreamingChunk_handlesDoneMarker() {
        val chunk = "data: [DONE]"
        
        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)
        
        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Done)
    }

    @Test
    fun parseStreamingChunk_handlesEmptyData() {
        val chunk = "data: "
        
        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)
        
        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Empty)
    }

    @Test
    fun parseStreamingChunk_ignoresNonDataLines() {
        val eventLine = "event: message"
        val idLine = "id: 123"
        val commentLine = ": this is a comment"
        
        assertTrue(MiniMaxPayloadParser.parseStreamingChunk(eventLine) is MiniMaxPayloadParser.StreamingParseResult.Empty)
        assertTrue(MiniMaxPayloadParser.parseStreamingChunk(idLine) is MiniMaxPayloadParser.StreamingParseResult.Empty)
        assertTrue(MiniMaxPayloadParser.parseStreamingChunk(commentLine) is MiniMaxPayloadParser.StreamingParseResult.Empty)
    }

    @Test
    fun parseStreamingChunk_handlesPlainJsonWithoutDataPrefix() {
        val chunk = "{\"choices\": [{\"delta\": {\"content\": \"World\"}}]}"
        
        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)
        
        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Content)
        assertEquals("World", (result as MiniMaxPayloadParser.StreamingParseResult.Content).content)
    }

    @Test
    fun parseStreamingChunk_returnsEmptyForInvalidJson() {
        val chunk = "data: {invalid json"
        
        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)
        
        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Empty)
    }

    @Test
    fun parseStreamingChunk_returnsEmptyWhenNoContent() {
        val chunk = "data: {\"choices\": [{\"delta\": {}}]}"
        
        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)
        
        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Empty)
    }

    @Test
    fun isStreamingResponse_detectsSseFormat() {
        val sseResponse = "data: {\"choices\": [{\"delta\": {\"content\": \"Hello\"}}]}\n\ndata: [DONE]"
        
        assertTrue(MiniMaxPayloadParser.isStreamingResponse(sseResponse))
    }

    @Test
    fun isStreamingResponse_detectsLineDelimitedJson() {
        val lineDelimited = "{\"choices\": [{\"delta\": {\"content\": \"A\"}}]}\n{\"choices\": [{\"delta\": {\"content\": \"B\"}}]}"
        
        assertTrue(MiniMaxPayloadParser.isStreamingResponse(lineDelimited))
    }

    @Test
    fun isStreamingResponse_returnsFalseForCompleteJson() {
        val completeJson = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Complete response"
                  }
                }
              ]
            }
        """.trimIndent()
        
        assertFalse(MiniMaxPayloadParser.isStreamingResponse(completeJson))
    }

    @Test
    fun isStreamingResponse_returnsFalseForEmptyString() {
        assertFalse(MiniMaxPayloadParser.isStreamingResponse(""))
    }

    @Test
    fun isStreamingResponse_detectsDeltaFormat() {
        val deltaFormat = "{\"choices\": [{\"delta\": {\"content\": \"Partial\"}}]}"
        
        assertTrue(MiniMaxPayloadParser.isStreamingResponse(deltaFormat))
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

    @Test
    fun buildRequestBody_filtersErrorMessages() {
        val requestBody = MiniMaxPayloadParser.buildRequestBody(
            model = "M2-her",
            messages = listOf(
                AiChatMessage(role = AiChatRole.USER, content = "User question"),
                AiChatMessage(role = AiChatRole.ASSISTANT, content = "Normal reply", isError = false),
                AiChatMessage(role = AiChatRole.ASSISTANT, content = "Error message", isError = true),
                AiChatMessage(role = AiChatRole.USER, content = "  ", isError = false), // blank content
                AiChatMessage(role = AiChatRole.USER, content = "Follow-up question")
            )
        )

        val json = JSONObject(requestBody)
        val messages = json.getJSONArray("messages")

        // Should only include non-error, non-blank messages
        assertEquals(3, messages.length())
        assertEquals("User question", messages.getJSONObject(0).getString("content"))
        assertEquals("Normal reply", messages.getJSONObject(1).getString("content"))
        assertEquals("Follow-up question", messages.getJSONObject(2).getString("content"))
    }

    @Test
    fun buildRequestBody_doesNotInventAModelWhenBlank() {
        val requestBody = MiniMaxPayloadParser.buildRequestBody(
            model = "   ",
            messages = listOf(AiChatMessage(role = AiChatRole.USER, content = "Hello"))
        )

        val json = JSONObject(requestBody)
        assertEquals("", json.getString("model"))
    }

    @Test
    fun buildRequestBody_excludesLocalFinancialToolHistory() {
        val body = MiniMaxPayloadParser.buildRequestBody("chosen-model", listOf(
            AiChatMessage(role = AiChatRole.USER, content = "ordinary question"),
            AiChatMessage(role = AiChatRole.ASSISTANT, content = "private queried records", isLocalOnly = true)
        ))
        val messages = JSONObject(body).getJSONArray("messages")
        assertEquals(1, messages.length())
        assertEquals("ordinary question", messages.getJSONObject(0).getString("content"))
    }

    // --- JSON null handling and reasoning_content (regression: "nullnull..." replies) ---

    @Test
    fun parseStreamingChunk_treatsJsonNullContentAsEmpty() {
        val chunk = "data: {\"choices\": [{\"delta\": {\"content\": null}}]}"

        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)

        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Empty)
    }

    @Test
    fun parseStreamingChunk_extractsReasoningWhenContentIsNull() {
        val chunk = "data: {\"choices\": [{\"delta\": {\"content\": null, \"reasoning_content\": \"thinking\"}}]}"

        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)

        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Content)
        val content = result as MiniMaxPayloadParser.StreamingParseResult.Content
        assertNull(content.content)
        assertEquals("thinking", content.reasoning)
    }

    @Test
    fun parseStreamingChunk_extractsContentAndReasoningTogether() {
        val chunk = "data: {\"choices\": [{\"delta\": {\"content\": \"Hi\", \"reasoning_content\": \"thinking\"}}]}"

        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)

        val content = result as MiniMaxPayloadParser.StreamingParseResult.Content
        assertEquals("Hi", content.content)
        assertEquals("thinking", content.reasoning)
    }

    @Test
    fun parseStreamingChunk_alternativeNullContentIsEmpty() {
        val chunk = "data: {\"content\": null}"

        val result = MiniMaxPayloadParser.parseStreamingChunk(chunk)

        assertTrue(result is MiniMaxPayloadParser.StreamingParseResult.Empty)
    }

    @Test
    fun parseAssistantReply_nullContentIsEmptyReplyNotLiteralNull() {
        val response = "{\"choices\": [{\"message\": {\"content\": null}}]}"

        val result = MiniMaxPayloadParser.parseAssistantReply(response)

        assertTrue(result is MiniMaxChatResult.Failure)
        assertEquals(MiniMaxErrorType.EMPTY_REPLY, (result as MiniMaxChatResult.Failure).type)
    }

    @Test
    fun parseAssistantReply_extractsReasoning() {
        val response = "{\"choices\": [{\"message\": {\"content\": \"Hello\", \"reasoning_content\": \"thinking\"}}]}"

        val result = MiniMaxPayloadParser.parseAssistantReply(response)

        assertTrue(result is MiniMaxChatResult.Success)
        val success = result as MiniMaxChatResult.Success
        assertEquals("Hello", success.reply)
        assertEquals("thinking", success.reasoning)
    }
}