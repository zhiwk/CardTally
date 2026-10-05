package com.example.cardtally.ai

import com.example.cardtally.network.MiniMaxErrorType
import org.junit.Assert.*
import org.junit.Test

class RecordToolClientTest {
    private val client = RecordToolClient()

    @Test fun rejectedRequest_keepsStatusWithoutExposingProviderDetails() {
        val result = client.decodeResponse(400, """{"error":{"message":"private provider request details"}}""")
            as RecordToolClient.Result.Failure
        assertEquals(MiniMaxErrorType.INVALID_REQUEST, result.type)
        assertEquals(400, result.statusCode)
        assertNull(result.protocolReason)
        assertFalse(result.toString().contains("private provider"))
    }

    @Test fun successfulHttpWithInvalidPayload_isAProtocolFailure() {
        val result = client.decodeResponse(200, "<html>private upstream failure</html>")
            as RecordToolClient.Result.Failure
        assertEquals(MiniMaxErrorType.PARSE, result.type)
        assertEquals(RecordToolProtocol.FailureReason.MALFORMED_RESPONSE, result.protocolReason)
        assertNull(result.statusCode)
        assertFalse(result.toString().contains("private upstream"))
    }

    @Test fun authAndRateLimits_remainSeparateFromToolCompatibility() {
        listOf(401 to MiniMaxErrorType.AUTH, 429 to MiniMaxErrorType.RATE_LIMIT).forEach { (status, type) ->
            val result = client.decodeResponse(status, "{}") as RecordToolClient.Result.Failure
            assertEquals(type, result.type)
            assertEquals(status, result.statusCode)
            assertNull(result.protocolReason)
        }
    }

    @Test fun normalMultiCallResponse_isAcceptedByTheClient() {
        val body = """{"choices":[{"message":{"content":null,"tool_calls":[
            {"id":"accounts","type":"function","function":{"name":"record_options","arguments":"{\"keyword\":\"Wallet\"}"}},
            {"id":"categories","type":"function","function":{"name":"record_options","arguments":"{\"keyword\":\"Food\"}"}}
        ]}}]}"""
        val result = client.decodeResponse(200, body) as RecordToolClient.Result.Success
        assertEquals(listOf("accounts", "categories"), result.reply.calls.map { it.id })
    }
}
