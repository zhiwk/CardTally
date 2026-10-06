package com.example.cardtally.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AgentSessionTitleHelper {
    fun createDefaultTitle(
        timestamp: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val format = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).apply {
            this.timeZone = timeZone
        }
        return "新会话-${format.format(Date(timestamp))}"
    }
}
