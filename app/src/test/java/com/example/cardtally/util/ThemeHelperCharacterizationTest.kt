package com.example.cardtally.util

import com.example.cardtally.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeHelperCharacterizationTest {

    @Test
    fun getThemeResId_mapsCurrentStoredThemeModesToLight() {
        // Given: current, stale, and malformed stored theme values
        val storedModes = listOf(0, 1, 2, 999)

        // When / Then: each stored mode selects the sole runtime style
        storedModes.forEach { storedMode ->
            assertEquals(R.style.Theme_CardTally_Light, ThemeHelper.getThemeResId(storedMode))
        }
    }

    @Test
    fun normalizeTheme_mapsEveryStoredValueToLight() {
        // Given: current, stale, and malformed stored theme values
        val storedModes = listOf(0, 1, 2, 999)

        // When / Then: every value normalizes to the sole supported mode
        storedModes.forEach { storedMode ->
            assertEquals(ThemeHelper.THEME_LIGHT, ThemeHelper.normalizeTheme(storedMode))
        }
    }

    @Test
    fun normalizeStoredTheme_mapsWrongTypedValuesToLight() {
        // Given: stale preference values written with the wrong SharedPreferences type
        val storedValues = listOf("dark", true)

        // When / Then: boundary normalization accepts either type without throwing
        storedValues.forEach { storedValue ->
            assertEquals(ThemeHelper.THEME_LIGHT, ThemeHelper.normalizeStoredTheme(storedValue))
        }
    }
}
