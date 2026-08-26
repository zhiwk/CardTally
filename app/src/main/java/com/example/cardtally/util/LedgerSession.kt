package com.example.cardtally.util

import android.content.Context

object LedgerSession {
    private const val PREFS = "ledger_session"
    private const val KEY_CURRENT_ID = "current_ledger_id"

    fun getCurrentId(context: Context): Long? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getLong(KEY_CURRENT_ID, -1L).takeIf { it > 0L }

    fun setCurrentId(context: Context, id: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_CURRENT_ID, id).apply()
    }
}
