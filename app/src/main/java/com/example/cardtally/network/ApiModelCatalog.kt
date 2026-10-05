package com.example.cardtally.network

import org.json.JSONObject

object ApiModelCatalog {
    /** An empty list is distinct from a malformed response; never invent fallback models. */
    fun parse(body: String): List<String> {
        val data = JSONObject(body).getJSONArray("data")
        return (0 until data.length()).mapNotNull { index ->
            (data.optJSONObject(index)?.opt("id") as? String)?.trim()?.takeIf { it.isNotEmpty() }
        }.distinct()
    }
}
