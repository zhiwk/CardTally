package com.example.cardtally.network

import android.util.Log
import com.example.cardtally.model.AiChatMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MiniMaxClient(
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {

    fun sendChat(
        config: MiniMaxConfig,
        messages: List<AiChatMessage>,
        callback: (MiniMaxChatResult) -> Unit
    ) {
        val normalizedConfig = config.normalized()
        if (!normalizedConfig.isComplete()) {
            callback(MiniMaxChatResult.Failure(MiniMaxErrorType.INVALID_CONFIG))
            return
        }

        val requestBody = MiniMaxPayloadParser.buildRequestBody(
            model = normalizedConfig.model,
            messages = messages
        ).toRequestBody(JSON_MEDIA_TYPE)

        val request = try {
            Request.Builder()
                .url(normalizedConfig.requestUrl)
                .addHeader("Authorization", "Bearer ${normalizedConfig.apiKey}")
                .addHeader("Accept", "application/json")
                .post(requestBody)
                .build()
        } catch (_: IllegalArgumentException) {
            callback(MiniMaxChatResult.Failure(MiniMaxErrorType.INVALID_URL))
            return
        }

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e(TAG, "MiniMax network failure", e)
                callback(
                    MiniMaxChatResult.Failure(
                        type = MiniMaxErrorType.NETWORK,
                        detail = e.localizedMessage
                    )
                )
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val body = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        Log.w(
                            TAG,
                            "MiniMax HTTP ${it.code} ${it.message}. Response body: ${body.truncateForLog()}"
                        )
                        callback(
                            MiniMaxPayloadParser.parseHttpFailure(
                                statusCode = it.code,
                                responseBody = body
                            )
                        )
                        return
                    }

                    callback(MiniMaxPayloadParser.parseAssistantReply(body))
                }
            }
        })
    }

    private companion object {
        private const val TAG = "MiniMaxClient"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private fun String.truncateForLog(maxLength: Int = 2000): String {
        return if (length <= maxLength) this else take(maxLength) + "…"
    }
}
