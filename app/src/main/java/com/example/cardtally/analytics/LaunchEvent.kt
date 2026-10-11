package com.example.cardtally.analytics

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object LaunchEvent {
    fun json(token: String, installation: String, eventId: String, time: Long, version: String): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(time))
        return JSONObject().put("api_key", token).put("distinct_id", installation)
            .put("uuid", eventId).put("timestamp", timestamp).put("event", "app_launch")
            .put("properties", JSONObject().put("version", version).put("channel", "release")
                .put("\$process_person_profile", false).put("\$geoip_disable", true)).toString()
    }
}
