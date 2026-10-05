package com.example.cardtally.ai

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RecordToolProtocolTest {
    private fun response(calls: JSONArray?, text: String = "") = JSONObject().put("choices", JSONArray()
        .put(JSONObject().put("message", JSONObject().put("content", text)
            .apply { if (calls != null) put("tool_calls", calls) }))).toString()
    private fun call(name: String, id: String = "call_1") = JSONObject().put("id", id).put("type", "function")
        .put("function", JSONObject().put("name", name).put("arguments", "{\"record_id\":1}"))

    @Test fun proseOrQuotedJson_doesNotBecomeAnOperation() {
        assertNull(RecordToolProtocol.parse(response(null, "确认，删除账单 1")).call)
        assertNull(RecordToolProtocol.parse(response(null, """{"tool":"delete_record","record_id":1}""")).call)
    }
    @Test fun registeredFunction_requiresNativeExecutionAfterParsing() {
        val result = RecordToolProtocol.parse(response(JSONArray().put(call("delete_record"))))
        assertEquals("delete_record", result.call?.name)
        assertEquals(1L, JSONObject(result.call!!.arguments).getLong("record_id"))
    }
    @Test fun unregisteredCallsAndDuplicateIds_areRejectedAsAWhole() {
        val unregistered = assertThrows(RecordToolProtocol.ParseException::class.java) {
            RecordToolProtocol.parse(response(JSONArray().put(call("get_record")).put(call("run_sql", "call_2"))))
        }
        assertEquals(RecordToolProtocol.FailureReason.UNREGISTERED_TOOL, unregistered.reason)
        val duplicate = assertThrows(RecordToolProtocol.ParseException::class.java) {
            RecordToolProtocol.parse(response(JSONArray().put(call("get_record")).put(call("record_options"))))
        }
        assertEquals(RecordToolProtocol.FailureReason.MALFORMED_RESPONSE, duplicate.reason)
    }
    @Test fun multipleReads_keepOrderAndMatchEveryToolResultById() {
        val result = RecordToolProtocol.parse(response(JSONArray()
            .put(call("record_options", "assets"))
            .put(call("record_options", "categories"))
            .put(call("query_records", "records"))))
        assertEquals(listOf("assets", "categories", "records"), result.calls.map { it.id })
        assertTrue(result.calls.all { it.name in RecordToolProtocol.readNames })
        assertNull(result.call) // The controller must handle the batch, not treat it as a single call.
        val assistant = RecordToolProtocol.assistantCalls(result.calls)
        val calls = assistant.getJSONArray("tool_calls")
        assertEquals(3, calls.length())
        result.calls.forEachIndexed { index, tool ->
            assertEquals(tool.id, calls.getJSONObject(index).getString("id"))
            assertEquals(tool.arguments, calls.getJSONObject(index).getJSONObject("function").getString("arguments"))
            assertEquals(tool.id, RecordToolProtocol.toolResult(tool, JSONObject().put("success", true)).getString("tool_call_id"))
        }
    }
    @Test fun mixedAndMultipleWrites_canOnlyReturnAnUnexecutedResult() {
        val result = RecordToolProtocol.parse(response(JSONArray().put(call("record_options", "read"))
            .put(call("create_record", "write_1")).put(call("delete_record", "write_2"))))
        assertEquals(3, result.calls.size)
        assertNull(result.call)
        result.calls.filter { it.name in RecordToolProtocol.writeNames }.forEach {
            val deferred = RecordToolProtocol.deferredWriteResult(allowWrites = true)
            assertFalse(deferred.getBoolean("success"))
            assertEquals("single_write_required", deferred.getString("error"))
            assertEquals(it.id, RecordToolProtocol.toolResult(it, deferred).getString("tool_call_id"))
        }
        val afterSuccess = RecordToolProtocol.deferredWriteResult(allowWrites = false)
        assertFalse(afterSuccess.getBoolean("success"))
        assertEquals("write_already_completed", afterSuccess.getString("error"))
    }
    @Test fun oversizedBatchOrMalformedLaterCall_doesNotProduceAPartialReply() {
        val maximum = JSONArray().apply { repeat(RecordToolProtocol.MAX_CALLS_PER_RESPONSE) { put(call("query_records", "call_$it")) } }
        assertEquals(RecordToolProtocol.MAX_CALLS_PER_RESPONSE, RecordToolProtocol.parse(response(maximum)).calls.size)
        maximum.put(call("query_records", "extra"))
        val oversized = assertThrows(RecordToolProtocol.ParseException::class.java) { RecordToolProtocol.parse(response(maximum)) }
        assertEquals(RecordToolProtocol.FailureReason.TOO_MANY_CALLS, oversized.reason)
        val malformed = call("record_options", "bad").apply { getJSONObject("function").put("arguments", "invalid") }
        val badArgs = assertThrows(RecordToolProtocol.ParseException::class.java) {
            RecordToolProtocol.parse(response(JSONArray().put(call("get_record", "good")).put(malformed)))
        }
        assertEquals(RecordToolProtocol.FailureReason.INVALID_ARGUMENTS, badArgs.reason)
    }
    @Test fun malformedAndEmptyResponses_haveDistinctSafeReasons() {
        val empty = assertThrows(RecordToolProtocol.ParseException::class.java) { RecordToolProtocol.parse(response(null)) }
        assertEquals(RecordToolProtocol.FailureReason.EMPTY_MESSAGE, empty.reason)
        val malformed = assertThrows(RecordToolProtocol.ParseException::class.java) { RecordToolProtocol.parse("<html>gateway error</html>") }
        assertEquals(RecordToolProtocol.FailureReason.MALFORMED_RESPONSE, malformed.reason)
        val wrongCalls = assertThrows(RecordToolProtocol.ParseException::class.java) {
            RecordToolProtocol.parse("""{"choices":[{"message":{"content":"done","tool_calls":{}}}]}""")
        }
        assertEquals(RecordToolProtocol.FailureReason.MALFORMED_RESPONSE, wrongCalls.reason)
        val badArgs = assertThrows(RecordToolProtocol.ParseException::class.java) {
            val tool = call("create_record")
            tool.getJSONObject("function").put("arguments", "private invalid content")
            RecordToolProtocol.parse(response(JSONArray().put(tool)))
        }
        assertEquals(RecordToolProtocol.FailureReason.INVALID_ARGUMENTS, badArgs.reason)
        assertEquals("INVALID_ARGUMENTS", badArgs.message)
        assertNull(badArgs.cause)
    }
    @Test fun datesMoneyAndIds_rejectInvalidOrLossyValues() {
        assertTrue(AiRecordEngine.validDate("2024-02-29"))
        assertTrue(AiRecordEngine.validDate("0001-01-01"))
        listOf("2025-02-29", "0000-01-01", "2026-13-01", "2026-01-01extra").forEach { assertFalse(AiRecordEngine.validDate(it)) }
        assertEquals(1250L, AiRecordEngine.money(JSONObject("{\"amount\":\"12.50\"}"), "amount", false))
        listOf("{\"amount\":12.50}", "{\"amount\":\"1.001\"}", "{\"amount\":\"-1\"}", "{\"amount\":\"0\"}").forEach {
            assertThrows(IllegalArgumentException::class.java) { AiRecordEngine.money(JSONObject(it), "amount", false) }
        }
        assertThrows(IllegalArgumentException::class.java) { AiRecordEngine.id(JSONObject("{\"id\":1.5}"), "id") }
    }
    @Test fun toolDefinitions_haveNoSqlOrBulkWriteEntryPoint() {
        val tools = RecordToolProtocol.tools()
        val names = (0 until tools.length()).map { tools.getJSONObject(it).getJSONObject("function").getString("name") }.toSet()
        assertEquals(RecordToolProtocol.names, names)
    }
    @Test fun onlyFinalWrites_requireConfirmationAndCanBeDisabledAfterSuccess() {
        RecordToolProtocol.readNames.forEach { assertFalse(RecordToolProtocol.requiresConfirmation(it)) }
        RecordToolProtocol.writeNames.forEach { assertTrue(RecordToolProtocol.requiresConfirmation(it)) }
        val tools = RecordToolProtocol.tools(allowWrites = false)
        val names = (0 until tools.length()).map { tools.getJSONObject(it).getJSONObject("function").getString("name") }.toSet()
        assertEquals(RecordToolProtocol.readNames, names)
        assertThrows(IllegalArgumentException::class.java) { RecordToolProtocol.requiresConfirmation("run_sql") }
    }
}
