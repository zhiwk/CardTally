package com.example.cardtally.util

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun getTheme_returnsLightWhenStoredValueIsLegacyColorTheme() {
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("theme_mode", 4)
            .commit()

        assertEquals(ThemeHelper.THEME_LIGHT, ThemeHelper.getTheme(context))
    }

    @Test
    fun getThemeName_usesSafeThemeNameAfterLegacyValueMigration() {
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("theme_mode", 5)
            .commit()

        assertEquals(
            context.getString(R.string.theme_light),
            ThemeHelper.getThemeName(context, ThemeHelper.getTheme(context))
        )
    }
}
