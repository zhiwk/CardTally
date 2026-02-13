package com.example.cardtally.util

import android.content.Context

object QuickAddHelper {
    private const val PREFS_NAME = "quick_add_prefs"
    private const val KEY_QUICK_ADD = "quick_add_enabled"

    fun saveQuickAdd(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_QUICK_ADD, enabled).apply()
    }

    fun getQuickAdd(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_QUICK_ADD, false)
    }
}
