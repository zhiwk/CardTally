package com.example.cardtally.analytics

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cardtally.BuildConfig
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class LaunchAnalyticsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    @Volatile private var call: Call? = null
    override fun onStopped() { call?.cancel(); super.onStopped() }
    override fun doWork(): Result {
        if (isStopped || !LaunchAnalytics.enabled(applicationContext)) return Result.success()
        val installation = inputData.getString("installation") ?: return Result.failure()
        val event = inputData.getString("event") ?: return Result.failure()
        val version = inputData.getString("version") ?: return Result.failure()
        val time = inputData.getLong("time", 0)
        if (time <= 0 || System.currentTimeMillis() - time > TimeUnit.DAYS.toMillis(7)) return Result.success()
        val body = LaunchEvent.json(BuildConfig.POSTHOG_PROJECT_TOKEN, installation, event, time, version)
        val request = Request.Builder().url("https://us.i.posthog.com/i/v0/e/")
            .post(body.toRequestBody("application/json".toMediaType())).build()
        val current = client.newCall(request)
        call = current
        if (!LaunchAnalytics.register(applicationContext, installation, current)) { call = null; return Result.success() }
        return try {
            if (isStopped) { current.cancel(); return Result.success() }
            current.execute().use { response ->
                when {
                    response.isSuccessful -> Result.success()
                    response.code == 429 || response.code in 500..599 -> retryLimited()
                    else -> Result.failure()
                }
            }
        } catch (_: IOException) {
            if (isStopped || !LaunchAnalytics.enabled(applicationContext)) Result.success() else retryLimited()
        } finally { LaunchAnalytics.unregister(current); call = null }
    }
    private fun retryLimited() = if (runAttemptCount < 3) Result.retry() else Result.failure()
    companion object {
        private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).callTimeout(15, TimeUnit.SECONDS).build()
    }
}
