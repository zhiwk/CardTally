package com.example.cardtally.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiModelsResult {
    data class Success(val models: List<String>) : ApiModelsResult()
    data class Failure(val reason: Reason, val statusCode: Int? = null) : ApiModelsResult()
    enum class Reason { AUTH, UNSUPPORTED, HTTP, NETWORK, INVALID_RESPONSE, EMPTY }
}

class ApiModelClient {
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    /** Only called by the explicit model-list button; no conversation or financial data is sent. */
    fun fetch(endpoints: ApiEndpoints, apiKey: String, callback: (ApiModelsResult) -> Unit): Call {
        val request = Request.Builder().url(endpoints.models)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Accept", "application/json")
            .get().build()
        return client.newCall(request).also { call ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!call.isCanceled()) callback(ApiModelsResult.Failure(ApiModelsResult.Reason.NETWORK))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use {
                        when {
                            response.code == 401 || response.code == 403 ->
                                ApiModelsResult.Failure(ApiModelsResult.Reason.AUTH)
                            response.code == 404 || response.code == 405 ->
                                ApiModelsResult.Failure(ApiModelsResult.Reason.UNSUPPORTED)
                            !response.isSuccessful ->
                                ApiModelsResult.Failure(ApiModelsResult.Reason.HTTP, response.code)
                            else -> readModels(response)
                        }
                    }
                    if (!call.isCanceled()) callback(result)
                }
            })
        }
    }

    private fun readModels(response: Response): ApiModelsResult {
        return try {
            val source = response.body?.source()
                ?: return ApiModelsResult.Failure(ApiModelsResult.Reason.INVALID_RESPONSE)
            // Bound response size and do not display or log provider bodies or credentials.
            val limit = 1024L * 1024L
            source.request(limit + 1)
            if (source.buffer.size > limit) return ApiModelsResult.Failure(ApiModelsResult.Reason.INVALID_RESPONSE)
            val models = ApiModelCatalog.parse(source.readUtf8())
            if (models.isEmpty()) ApiModelsResult.Failure(ApiModelsResult.Reason.EMPTY)
            else ApiModelsResult.Success(models)
        } catch (_: JSONException) {
            ApiModelsResult.Failure(ApiModelsResult.Reason.INVALID_RESPONSE)
        } catch (_: IOException) {
            ApiModelsResult.Failure(ApiModelsResult.Reason.NETWORK)
        }
    }
}
