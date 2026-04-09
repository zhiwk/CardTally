package com.example.cardtally.util

import android.content.Context
import com.example.cardtally.R

object ThemeHelper {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_mode"

    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_SYSTEM = 2

    fun saveTheme(context: Context, themeMode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, themeMode).apply()
    }

    fun getTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedTheme = prefs.getInt(KEY_THEME, THEME_LIGHT)
        return when (storedTheme) {
            THEME_LIGHT, THEME_DARK, THEME_SYSTEM -> storedTheme
            else -> {
                prefs.edit().putInt(KEY_THEME, THEME_LIGHT).apply()
                THEME_LIGHT
            }
        }
    }

    fun getThemeResId(themeMode: Int): Int {
        return when (themeMode) {
            THEME_DARK -> R.style.Theme_CardTally_Dark
            THEME_SYSTEM -> R.style.Theme_CardTally_System
            else -> R.style.Theme_CardTally_Light
        }
    }

    fun getThemeName(context: Context, themeMode: Int): String {
        val themeNames = context.resources.getStringArray(R.array.theme_names)
        val safeThemeMode = when (themeMode) {
            THEME_LIGHT, THEME_DARK, THEME_SYSTEM -> themeMode
            else -> THEME_LIGHT
        }
        return themeNames[safeThemeMode]
    }
}
