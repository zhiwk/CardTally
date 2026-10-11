package com.example.cardtally.util

import android.content.Context

object BottomNavigationPreferences {
    private const val NAME = "bottom_navigation_prefs"
    fun isDocked(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).all["style"] == "docked"
    fun saveDocked(context: Context, docked: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("style", if (docked) "docked" else "floating").apply()
    }
}
