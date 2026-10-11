package com.example.cardtally.analytics

import android.content.Context
import androidx.work.*
import com.example.cardtally.BuildConfig
import okhttp3.Call
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Process-local launch guard; preferences and work input never enter financial backups. */
object LaunchAnalytics {
    const val TAG = "launch-analytics"
    private var recorded = false
    private val calls = mutableSetOf<Call>()
    private fun prefs(context: Context) = context.getSharedPreferences("launch_analytics", Context.MODE_PRIVATE)
    fun available() = !BuildConfig.DEBUG && BuildConfig.APPLICATION_ID == "com.example.cardtally.release" &&
        BuildConfig.POSTHOG_PROJECT_TOKEN.isNotEmpty()
    fun enabled(context: Context) = available() && prefs(context).getBoolean("enabled", true)

    @Synchronized fun setEnabled(context: Context, enabled: Boolean) {
        val edit = prefs(context).edit().putBoolean("enabled", enabled && available())
        if (!enabled) edit.remove("installation_id")
        edit.apply()
        if (!enabled) {
            calls.forEach { it.cancel() }
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        }
    }

    @Synchronized fun recordLaunch(context: Context) {
        if (recorded || !enabled(context)) return
        val preferences = prefs(context)
        val installation = preferences.getString("installation_id", null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString("installation_id", it).apply()
        }
        val eventId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val work = OneTimeWorkRequestBuilder<LaunchAnalyticsWorker>()
            .setInputData(workDataOf("installation" to installation, "event" to eventId,
                "time" to now, "version" to BuildConfig.VERSION_NAME))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG).build()
        WorkManager.getInstance(context).enqueueUniqueWork("$TAG-$eventId", ExistingWorkPolicy.KEEP, work)
        recorded = true
    }

    @Synchronized fun register(context: Context, installation: String, call: Call): Boolean {
        if (!enabled(context) || prefs(context).getString("installation_id", null) != installation) return false
        calls.add(call)
        return true
    }
    @Synchronized fun unregister(call: Call) { calls.remove(call) }
}
