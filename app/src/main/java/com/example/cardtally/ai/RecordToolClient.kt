package com.example.cardtally.ai

import com.example.cardtally.network.MiniMaxConfig
import com.example.cardtally.network.MiniMaxErrorType
import com.example.cardtally.network.MiniMaxPayloadParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Tools use non-streaming Chat Completions; ordinary chat keeps its existing streaming path. */
class RecordToolClient {
    sealed class Result {
        data class Success(val reply: RecordToolProtocol.Reply) : Result()
        data class Failure(val type: MiniMaxErrorType, val statusCode: Int? = null,
                           val protocolReason: RecordToolProtocol.FailureReason? = null) : Result()
    }
    private val client = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()

    fun send(config: MiniMaxConfig, messages: JSONArray, allowWrites: Boolean = true, callback: (Result) -> Unit): Call {
        val body = JSONObject().put("model", config.model).put("messages", JSONArray(messages.toString()))
            .put("tools", RecordToolProtocol.tools(allowWrites)).put("tool_choice", "auto").put("stream", false)
        val request = Request.Builder().url(config.requestUrl)
            .header("Authorization", "Bearer ${config.apiKey}").header("Accept", "application/json")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        return client.newCall(request).also { call -> call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) callback(Result.Failure(MiniMaxErrorType.NETWORK))
            }
            override fun onResponse(call: Call, response: Response) {
                val result = response.use {
                    try {
                        val source = response.body?.source() ?: return@use Result.Failure(MiniMaxErrorType.EMPTY_REPLY)
                        val limit = 1024L * 1024L
                        source.request(limit + 1)
                        if (source.buffer.size > limit) return@use Result.Failure(MiniMaxErrorType.PARSE)
                        val text = source.readUtf8()
                        decodeResponse(response.code, text)
                    } catch (_: IOException) {
                        Result.Failure(MiniMaxErrorType.NETWORK)
                    } catch (_: Exception) {
                        Result.Failure(MiniMaxErrorType.PARSE)
                    }
                }
                if (!call.isCanceled()) callback(result)
            }
        }) }
    }

    /** Keep safe status/reason codes, never expose the provider body or its error message. */
    internal fun decodeResponse(statusCode: Int, body: String): Result {
        if (statusCode !in 200..299)
            return Result.Failure(MiniMaxPayloadParser.parseHttpFailure(statusCode, body).type, statusCode)
        return try {
            Result.Success(RecordToolProtocol.parse(body))
        } catch (error: RecordToolProtocol.ParseException) {
            Result.Failure(MiniMaxErrorType.PARSE, protocolReason = error.reason)
        }
    }
}
