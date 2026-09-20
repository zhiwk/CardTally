package com.example.cardtally.util

import android.content.Context

enum class StatisticsRankingMode(val token: String) {
    SECONDARY("secondary"),
    PRIMARY("primary");

    companion object {
        fun fromToken(token: String?): StatisticsRankingMode =
            entries.firstOrNull { it.token == token } ?: SECONDARY
    }
}

object StatisticsRankingModePreferences {
    private const val PREFS_NAME = "statistics_ranking_mode_prefs"
    private const val KEY_MODE = "statistics_ranking_mode"

    fun getMode(context: Context): StatisticsRankingMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return StatisticsRankingMode.fromToken(prefs.getString(KEY_MODE, null))
    }

    fun saveMode(context: Context, mode: StatisticsRankingMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.token)
            .apply()
    }
}
