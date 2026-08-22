package com.example.cardtally.util

import android.content.Context
import android.content.SharedPreferences

enum class LedgerStartupBehavior(val token: String) {
    DETAILS("details"),
    STATISTICS("statistics"),
    REMEMBER_LAST("remember_last");

    companion object {
        fun fromToken(token: String?): LedgerStartupBehavior? = entries.firstOrNull { it.token == token }
    }
}

enum class LedgerView(val token: String) {
    DETAILS("details"),
    STATISTICS_EXPENSE("statistics_expense"),
    STATISTICS_INCOME("statistics_income");

    companion object {
        fun fromToken(token: String?): LedgerView? = entries.firstOrNull { it.token == token }
    }
}

object LedgerUxPreferences {
    private const val PREFS_NAME = "ledger_ux_prefs"
    private const val SCHEMA_VERSION = 1
    private const val KEY_SCHEMA_VERSION = "ledger_ux_schema_version"
    private const val KEY_STARTUP_BEHAVIOR = "ledger_startup_behavior"
    private const val KEY_LAST_VIEW = "ledger_last_view"

    fun getStartupBehavior(context: Context): LedgerStartupBehavior {
        return readEnum(
            preferences(context),
            KEY_STARTUP_BEHAVIOR,
            LedgerStartupBehavior.DETAILS,
            { LedgerStartupBehavior.fromToken(it) }
        )
    }

    fun saveStartupBehavior(context: Context, value: LedgerStartupBehavior) {
        preferences(context).edit().putString(KEY_STARTUP_BEHAVIOR, value.token).apply()
    }

    fun getLastView(context: Context): LedgerView {
        return readEnum(preferences(context), KEY_LAST_VIEW, LedgerView.DETAILS) { LedgerView.fromToken(it) }
    }

    fun saveLastView(context: Context, value: LedgerView) {
        preferences(context).edit().putString(KEY_LAST_VIEW, value.token).apply()
    }

    fun resolveStartupView(
        context: Context,
        behavior: LedgerStartupBehavior = getStartupBehavior(context)
    ): LedgerView {
        return when (behavior) {
            LedgerStartupBehavior.DETAILS -> LedgerView.DETAILS
            LedgerStartupBehavior.STATISTICS -> LedgerView.STATISTICS_EXPENSE
            LedgerStartupBehavior.REMEMBER_LAST -> getLastView(context)
        }
    }

    private fun preferences(context: Context): SharedPreferences {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        migrate(preferences)
        return preferences
    }

    private fun migrate(preferences: SharedPreferences) {
        val storedVersion = preferences.all[KEY_SCHEMA_VERSION] as? Int ?: 0
        if (storedVersion >= SCHEMA_VERSION) return

        val editor = preferences.edit()
        if (!preferences.contains(KEY_STARTUP_BEHAVIOR)) editor.putString(KEY_STARTUP_BEHAVIOR, LedgerStartupBehavior.DETAILS.token)
        if (!preferences.contains(KEY_LAST_VIEW)) editor.putString(KEY_LAST_VIEW, LedgerView.DETAILS.token)
        editor.putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION).apply()
    }

    private fun <T> readEnum(
        preferences: SharedPreferences,
        key: String,
        fallback: T,
        parse: (String?) -> T?
    ): T {
        val value = parse(preferences.all[key] as? String) ?: fallback
        val token = when (value) {
            is LedgerStartupBehavior -> value.token
            is LedgerView -> value.token
            else -> error("Unsupported Ledger preference enum")
        }
        if (!isFutureSchema(preferences) && preferences.all[key] != token) {
            preferences.edit().putString(key, token).apply()
        }
        return value
    }

    private fun isFutureSchema(preferences: SharedPreferences): Boolean {
        return (preferences.all[KEY_SCHEMA_VERSION] as? Int ?: 0) > SCHEMA_VERSION
    }
}
