package com.example.cardtally.util

import android.content.Context
import com.example.cardtally.R

object ThemeHelper {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_mode"

    const val THEME_LIGHT = 0

    fun saveTheme(context: Context, themeMode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, normalizeTheme(themeMode)).apply()
    }

    fun getTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedTheme = prefs.all[KEY_THEME]
        if (storedTheme != THEME_LIGHT) {
            prefs.edit().putInt(KEY_THEME, normalizeStoredTheme(storedTheme)).apply()
        }
        return THEME_LIGHT
    }

    fun getThemeResId(themeMode: Int): Int {
        return R.style.Theme_CardTally_Light
    }

    fun normalizeTheme(themeMode: Int): Int = THEME_LIGHT

    internal fun normalizeStoredTheme(storedTheme: Any?): Int = THEME_LIGHT
}
