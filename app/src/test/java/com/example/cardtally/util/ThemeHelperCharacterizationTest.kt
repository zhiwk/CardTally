package com.example.cardtally.util

import androidx.appcompat.app.AppCompatDelegate
import com.example.cardtally.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeHelperCharacterizationTest {
    @Test
    fun getThemeResId_preservesBaseStyleAndUsesWallpaperStyleForPictureMode() {
        listOf(0, 1, 2, 999).forEach { mode ->
            assertEquals(R.style.Theme_CardTally, ThemeHelper.getThemeResId(mode))
        }
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeHelper.nightModeFor(0))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeHelper.nightModeFor(1))
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeHelper.nightModeFor(2))
        assertEquals(R.style.Theme_CardTally_Wallpaper, ThemeHelper.getThemeResId(3))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeHelper.nightModeFor(3))
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeHelper.nightModeFor(3, ThemeHelper.THEME_LIGHT))
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeHelper.nightModeFor(3, ThemeHelper.THEME_SYSTEM))
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeHelper.nightModeFor(3, 999))
    }

    @Test
    fun normalizeTheme_preservesSupportedModesAndRejectsUnknownValues() {
        listOf(0, 1, 2, 3).forEach { mode -> assertEquals(mode, ThemeHelper.normalizeTheme(mode)) }
        listOf(-1, 999).forEach { mode -> assertEquals(ThemeHelper.THEME_LIGHT, ThemeHelper.normalizeTheme(mode)) }
    }

    @Test
    fun normalizeStoredTheme_handlesMalformedPreferencesWithoutThrowing() {
        listOf(null, "dark", true, 1L, 999).forEach { value ->
            assertEquals(ThemeHelper.THEME_LIGHT, ThemeHelper.normalizeStoredTheme(value))
        }
        assertEquals(ThemeHelper.THEME_DARK, ThemeHelper.normalizeStoredTheme(1))
        assertEquals(ThemeHelper.THEME_SYSTEM, ThemeHelper.normalizeStoredTheme(2))
        assertEquals(ThemeHelper.THEME_WALLPAPER, ThemeHelper.normalizeStoredTheme(3))
    }
}
