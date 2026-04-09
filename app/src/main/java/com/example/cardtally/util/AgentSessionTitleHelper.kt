package com.example.cardtally.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AgentSessionTitleHelper {
    private val defaultTitleFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)

    fun createDefaultTitle(timestamp: Long = System.currentTimeMillis()): String {
        return "新会话-${defaultTitleFormat.format(Date(timestamp))}"
    }
}
