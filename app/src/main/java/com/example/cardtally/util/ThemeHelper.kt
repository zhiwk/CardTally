package com.example.cardtally.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.cardtally.R

object ThemeHelper {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_CARD_OPACITY = "card_opacity"
    private const val KEY_WALLPAPER_PALETTE = "wallpaper_palette"
    const val DEFAULT_CARD_OPACITY = 80

    private val cardOpacityOverlays = intArrayOf(
        R.style.ThemeOverlay_CardTally_CardOpacity_P0,
        R.style.ThemeOverlay_CardTally_CardOpacity_P5,
        R.style.ThemeOverlay_CardTally_CardOpacity_P10,
        R.style.ThemeOverlay_CardTally_CardOpacity_P15,
        R.style.ThemeOverlay_CardTally_CardOpacity_P20,
        R.style.ThemeOverlay_CardTally_CardOpacity_P25,
        R.style.ThemeOverlay_CardTally_CardOpacity_P30,
        R.style.ThemeOverlay_CardTally_CardOpacity_P35,
        R.style.ThemeOverlay_CardTally_CardOpacity_P40,
        R.style.ThemeOverlay_CardTally_CardOpacity_P45,
        R.style.ThemeOverlay_CardTally_CardOpacity_P50,
        R.style.ThemeOverlay_CardTally_CardOpacity_P55,
        R.style.ThemeOverlay_CardTally_CardOpacity_P60,
        R.style.ThemeOverlay_CardTally_CardOpacity_P65,
        R.style.ThemeOverlay_CardTally_CardOpacity_P70,
        R.style.ThemeOverlay_CardTally_CardOpacity_P75,
        R.style.ThemeOverlay_CardTally_CardOpacity_P80,
        R.style.ThemeOverlay_CardTally_CardOpacity_P85,
        R.style.ThemeOverlay_CardTally_CardOpacity_P90,
        R.style.ThemeOverlay_CardTally_CardOpacity_P95,
        R.style.ThemeOverlay_CardTally_CardOpacity_P100
    )

    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_SYSTEM = 2
    const val THEME_WALLPAPER = 3

    fun normalizeCardOpacity(opacity: Int): Int = ((opacity.coerceIn(0, 100) + 2) / 5) * 5

    fun getCardOpacity(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.all[KEY_CARD_OPACITY]
        val normalized = (stored as? Int)?.let(::normalizeCardOpacity) ?: DEFAULT_CARD_OPACITY
        if (stored != null && stored != normalized) {
            prefs.edit().putInt(KEY_CARD_OPACITY, normalized).apply()
        }
        return normalized
    }

    fun saveCardOpacity(context: Context, opacity: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_CARD_OPACITY, normalizeCardOpacity(opacity)).apply()
    }

    fun getCardOpacityOverlayResId(opacity: Int): Int = cardOpacityOverlays[normalizeCardOpacity(opacity) / 5]

    fun saveTheme(context: Context, themeMode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_THEME, normalizeTheme(themeMode)).apply()
    }

    fun getTheme(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedTheme = prefs.all[KEY_THEME]
        val normalized = normalizeStoredTheme(storedTheme)
        if (storedTheme != null && storedTheme != normalized) {
            prefs.edit().putInt(KEY_THEME, normalized).apply()
        }
        return normalized
    }

    fun normalizeWallpaperPalette(palette: Int): Int = when (palette) {
        THEME_LIGHT, THEME_DARK, THEME_SYSTEM -> palette
        else -> THEME_DARK
    }

    fun getWallpaperPalette(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.all[KEY_WALLPAPER_PALETTE]
        val normalized = (stored as? Int)?.let(::normalizeWallpaperPalette) ?: THEME_DARK
        if (stored != null && stored != normalized) {
            prefs.edit().putInt(KEY_WALLPAPER_PALETTE, normalized).apply()
        }
        return normalized
    }

    fun saveWallpaperPalette(context: Context, palette: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_WALLPAPER_PALETTE, normalizeWallpaperPalette(palette)).apply()
    }

    fun applyThemeMode(themeMode: Int, wallpaperPalette: Int = THEME_DARK) {
        AppCompatDelegate.setDefaultNightMode(nightModeFor(themeMode, wallpaperPalette))
    }

    fun nightModeFor(themeMode: Int, wallpaperPalette: Int = THEME_DARK): Int = when (normalizeTheme(themeMode)) {
        THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        THEME_WALLPAPER -> when (normalizeWallpaperPalette(wallpaperPalette)) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }
        THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else -> AppCompatDelegate.MODE_NIGHT_NO
    }

    fun getThemeResId(themeMode: Int): Int = if (normalizeTheme(themeMode) == THEME_WALLPAPER) {
        R.style.Theme_CardTally_Wallpaper
    } else {
        R.style.Theme_CardTally
    }

    fun normalizeTheme(themeMode: Int): Int = when (themeMode) {
        THEME_LIGHT, THEME_DARK, THEME_SYSTEM, THEME_WALLPAPER -> themeMode
        else -> THEME_LIGHT
    }

    internal fun normalizeStoredTheme(storedTheme: Any?): Int =
        (storedTheme as? Int)?.let(::normalizeTheme) ?: THEME_LIGHT
}
