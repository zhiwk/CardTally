package com.example.cardtally.util

import java.text.SimpleDateFormat
import java.util.Locale

object LedgerDisplayHelper {
    fun formatDateHeader(date: String, locale: Locale = Locale.getDefault()): String {
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
        if (locale.language == Locale.CHINESE.language) {
            return SimpleDateFormat("M月d日 EEEE", locale).format(parsed)
        }
        val weekday = SimpleDateFormat("EEEE", locale).format(parsed)
        val day = SimpleDateFormat("d", locale).format(parsed).toInt()
        return "$weekday, ${day}${ordinalSuffix(day)}"
    }

    fun formatEntriesMeta(count: Int, locale: Locale = Locale.getDefault()): String {
        return if (locale.language == Locale.CHINESE.language) {
            "$count 笔记录"
        } else {
            "$count ENTRIES"
        }
    }

    fun formatShortDateLabel(date: String, locale: Locale = Locale.getDefault()): String {
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
        return if (locale.language == Locale.CHINESE.language) {
            SimpleDateFormat("M/d", locale).format(parsed)
        } else {
            SimpleDateFormat("MMM d", locale).format(parsed)
        }
    }

    fun formatDayOnlyLabel(date: String, locale: Locale = Locale.getDefault()): String {
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
        return SimpleDateFormat("d", locale).format(parsed)
    }

    fun formatMonthLabel(monthStr: String, locale: Locale = Locale.getDefault()): String {
        val parsed = java.text.SimpleDateFormat("yyyy-MM", Locale.US).parse(monthStr) ?: return monthStr
        return if (locale.language == Locale.CHINESE.language) {
            SimpleDateFormat("M月", locale).format(parsed)
        } else {
            SimpleDateFormat("MMM", locale).format(parsed)
        }
    }

    private fun ordinalSuffix(day: Int): String {
        if (day in 11..13) return "th"
        return when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}
