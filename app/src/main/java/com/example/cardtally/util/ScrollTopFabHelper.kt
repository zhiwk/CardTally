package com.example.cardtally.util

import android.content.Context

object ScrollTopFabHelper {
    private const val PREFS_NAME = "scroll_top_fab_prefs"
    private const val KEY_ENABLED = "scroll_top_fab_enabled"

    fun saveEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }
}
