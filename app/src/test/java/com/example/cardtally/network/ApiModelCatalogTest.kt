package com.example.cardtally.network

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiModelCatalogTest {
    @Test
    fun response_keepsModelIdsInServerOrderAndRemovesDuplicates() {
        val body = """{"data":[{"id":"model-b"},{"id":"model-a"},{"id":"model-b"},{"id":null},{"id":42},{"id":" "}]}"""
        assertEquals(listOf("model-b", "model-a"), ApiModelCatalog.parse(body))
    }

    @Test
    fun emptyCatalog_hasNoFallbackModel() {
        assertEquals(emptyList<String>(), ApiModelCatalog.parse("""{"data":[]}"""))
    }

    @Test(expected = JSONException::class)
    fun errorResponse_isNotAValidCatalog() {
        ApiModelCatalog.parse("""{"error":{"message":"denied"}}""")
    }

    @Test(expected = JSONException::class)
    fun nonJsonResponse_isRejected() {
        ApiModelCatalog.parse("<html>Service unavailable</html>")
    }
}
