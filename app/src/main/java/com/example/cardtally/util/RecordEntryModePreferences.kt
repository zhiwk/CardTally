package com.example.cardtally.util

import android.content.Context
import android.content.SharedPreferences

/** Layout presentation used by the shared add/edit record form. */
enum class RecordEntryMode(val token: String) {
    STANDARD("standard"),
    QUICK("quick");

    companion object {
        fun fromToken(token: String?): RecordEntryMode? = entries.firstOrNull { it.token == token }
    }
}

object RecordEntryModePreferences {
    private const val PREFS_NAME = "record_entry_mode_prefs"
    private const val KEY_MODE = "record_entry_mode"

    fun getMode(context: Context): RecordEntryMode {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readMode(preferences)
    }

    fun saveMode(context: Context, mode: RecordEntryMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.token)
            .apply()
    }

    private fun readMode(preferences: SharedPreferences): RecordEntryMode {
        val stored = preferences.all[KEY_MODE]
        val mode = RecordEntryMode.fromToken(stored as? String) ?: RecordEntryMode.STANDARD
        if (stored != mode.token) {
            preferences.edit().putString(KEY_MODE, mode.token).apply()
        }
        return mode
    }
}
