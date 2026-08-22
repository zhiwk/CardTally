package com.example.cardtally.util

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeHelperTest {

    private lateinit var context: Context
    private val themePreferences by lazy {
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        themePreferences.edit().clear().commit()
    }

    @Test
    fun getTheme_normalizesStoredModesAndPreservesUnrelatedPreference() {
        // Given: each known, stale, and malformed theme value plus unrelated state
        val storedModes = listOf(0, 1, 2, 999)

        storedModes.forEach { storedMode ->
            themePreferences.edit()
                .putInt("theme_mode", storedMode)
                .putString("unrelated_sentinel", "preserve-me")
                .commit()

            // When: the stored preference crosses the ThemeHelper boundary
            val effectiveTheme = ThemeHelper.getTheme(context)

            // Then: Light is returned and persisted without clearing unrelated state
            assertEquals(ThemeHelper.THEME_LIGHT, effectiveTheme)
            assertEquals(ThemeHelper.THEME_LIGHT, themePreferences.getInt("theme_mode", -1))
            assertEquals("preserve-me", themePreferences.getString("unrelated_sentinel", null))
        }
    }

    @Test
    fun saveTheme_normalizesMalformedInputAndPreservesUnrelatedPreference() {
        // Given: unrelated state in the same preferences file
        themePreferences.edit().putString("unrelated_sentinel", "preserve-me").commit()

        // When: a malformed theme mode is saved
        ThemeHelper.saveTheme(context, 999)

        // Then: only the theme key is normalized
        assertEquals(ThemeHelper.THEME_LIGHT, themePreferences.getInt("theme_mode", -1))
        assertEquals("preserve-me", themePreferences.getString("unrelated_sentinel", null))
    }

    @Test
    fun getTheme_normalizesWrongTypedValuesAndPreservesUnrelatedPreference() {
        // Given: stale String and Boolean theme values plus unrelated state
        val writeWrongTypedThemeValues = listOf<(android.content.SharedPreferences.Editor) -> Unit>(
            { editor -> editor.putString("theme_mode", "dark") },
            { editor -> editor.putBoolean("theme_mode", true) }
        )

        writeWrongTypedThemeValues.forEach { writeThemeValue ->
            val editor = themePreferences.edit()
                .putString("unrelated_sentinel", "preserve-me")
            writeThemeValue(editor)
            editor.commit()

            // When: ThemeHelper reads the wrong-typed preference
            val effectiveTheme = ThemeHelper.getTheme(context)

            // Then: it repairs only theme_mode without throwing or clearing the sentinel
            assertEquals(ThemeHelper.THEME_LIGHT, effectiveTheme)
            assertEquals(ThemeHelper.THEME_LIGHT, themePreferences.getInt("theme_mode", -1))
            assertEquals("preserve-me", themePreferences.getString("unrelated_sentinel", null))
        }
    }
}
