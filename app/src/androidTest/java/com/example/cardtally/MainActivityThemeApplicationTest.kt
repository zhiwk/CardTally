package com.example.cardtally

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.util.ThemeHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityThemeApplicationTest {
    @Before
    fun setUp() {
        ThemeHelper.saveTheme(InstrumentationRegistry.getInstrumentation().targetContext, ThemeHelper.THEME_LIGHT)
        ThemeHelper.saveWallpaperPalette(InstrumentationRegistry.getInstrumentation().targetContext, ThemeHelper.THEME_DARK)
        ThemeHelper.saveCardOpacity(InstrumentationRegistry.getInstrumentation().targetContext, 80)
    }

    @After
    fun restoreLightPreference() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ThemeHelper.saveTheme(instrumentation.targetContext, ThemeHelper.THEME_LIGHT)
        ThemeHelper.saveWallpaperPalette(instrumentation.targetContext, ThemeHelper.THEME_DARK)
        ThemeHelper.saveCardOpacity(instrumentation.targetContext, 80)
        instrumentation.runOnMainSync { ThemeHelper.applyThemeMode(ThemeHelper.THEME_LIGHT) }
    }

    @Test
    fun savedDarkPreference_recolorsPagesAndPreservesCurrentDestination() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var lightBackground = 0
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                lightBackground = activity.getColor(R.color.background_light)
                activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_settings
            }
            instrumentation.waitForIdleSync()
            ThemeHelper.saveTheme(instrumentation.targetContext, ThemeHelper.THEME_DARK)
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(Configuration.UI_MODE_NIGHT_YES,
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                val darkBackground = activity.getColor(R.color.background_light)
                assertNotEquals(lightBackground, darkBackground)
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertEquals(true, fragment is SettingsFragment)
                assertEquals(darkBackground, (fragment?.view?.background as ColorDrawable).color)
                assertEquals(R.id.nav_settings, activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
            }
            scenario.onActivity { activity ->
                activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                    .requireView().findViewById<View>(R.id.card_theme).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val appearance = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertEquals(true, appearance is AppearanceSettingsFragment)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_shell).visibility)
                assertEquals(true, appearance!!.requireView().findViewById<RadioButton>(R.id.radio_theme_dark).isChecked)
                appearance.requireView().findViewById<RadioButton>(R.id.radio_wallpaper_dark).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val appearance = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertEquals(true, appearance is AppearanceSettingsFragment)
                assertEquals(ThemeHelper.THEME_WALLPAPER, ThemeHelper.getTheme(activity))
                assertEquals(Configuration.UI_MODE_NIGHT_YES,
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.wallpaper_background).visibility)
                assertEquals(android.graphics.Color.TRANSPARENT, (appearance!!.requireView().background as ColorDrawable).color)
                assertEquals(true, appearance.requireView().findViewById<RadioButton>(R.id.radio_wallpaper_dark).isChecked)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_shell).visibility)
                appearance.requireView().findViewById<com.google.android.material.slider.Slider>(R.id.slider_card_opacity).value = 30f
                appearance.requireView().findViewById<RadioButton>(R.id.radio_wallpaper_light).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val appearance = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                assertEquals(ThemeHelper.THEME_WALLPAPER, ThemeHelper.getTheme(activity))
                assertEquals(ThemeHelper.THEME_LIGHT, ThemeHelper.getWallpaperPalette(activity))
                assertEquals(Configuration.UI_MODE_NIGHT_NO,
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.wallpaper_background).visibility)
                assertEquals(android.graphics.Color.WHITE,
                    com.example.cardtally.util.ThemeColorHelper.resolveCardSurface(activity) or 0xFF000000.toInt())
                assertEquals(-1, appearance.requireView().findViewById<android.widget.RadioGroup>(R.id.group_theme).checkedRadioButtonId)
                assertEquals(true, appearance.requireView().findViewById<RadioButton>(R.id.radio_wallpaper_light).isChecked)
                assertEquals(30, ThemeHelper.getCardOpacity(activity))
                assertEquals(77, android.graphics.Color.alpha(com.example.cardtally.util.ThemeColorHelper.resolveCardSurface(activity)))
                appearance.requireView().findViewById<com.google.android.material.slider.Slider>(R.id.slider_card_opacity).value = 55f
                appearance.requireView().findViewById<RadioButton>(R.id.radio_wallpaper_dark).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(Configuration.UI_MODE_NIGHT_YES,
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertEquals(activity.getColor(R.color.surface_light),
                    com.example.cardtally.util.ThemeColorHelper.resolveCardSurface(activity) or 0xFF000000.toInt())
                assertEquals(55, ThemeHelper.getCardOpacity(activity))
                assertEquals(140, android.graphics.Color.alpha(com.example.cardtally.util.ThemeColorHelper.resolveCardSurface(activity)))
                activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                    .requireView().findViewById<RadioButton>(R.id.radio_wallpaper_system).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val appearance = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                assertEquals(ThemeHelper.THEME_WALLPAPER, ThemeHelper.getTheme(activity))
                assertEquals(ThemeHelper.THEME_SYSTEM, ThemeHelper.getWallpaperPalette(activity))
                assertEquals(android.content.res.Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertEquals(55, ThemeHelper.getCardOpacity(activity))
                assertEquals(140, android.graphics.Color.alpha(com.example.cardtally.util.ThemeColorHelper.resolveCardSurface(activity)))
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.wallpaper_background).visibility)
                assertEquals(true, appearance.requireView().findViewById<RadioButton>(R.id.radio_wallpaper_system).isChecked)
                appearance.requireView().findViewById<RadioButton>(R.id.radio_theme_dark).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val appearance = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertEquals(true, appearance is AppearanceSettingsFragment)
                assertEquals(ThemeHelper.THEME_DARK, ThemeHelper.getTheme(activity))
                assertEquals(View.GONE, activity.findViewById<View>(R.id.wallpaper_background).visibility)
                assertEquals(activity.getColor(R.color.background_light), (appearance!!.requireView().background as ColorDrawable).color)
                appearance.requireView().findViewById<RadioButton>(R.id.radio_theme_light).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val appearance = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertEquals(true, appearance is AppearanceSettingsFragment)
                assertEquals(ThemeHelper.THEME_LIGHT, ThemeHelper.getTheme(activity))
                assertEquals(Configuration.UI_MODE_NIGHT_NO,
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                assertEquals(true, appearance!!.requireView().findViewById<RadioButton>(R.id.radio_theme_light).isChecked)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_shell).visibility)
                appearance.requireView().findViewById<RadioButton>(R.id.radio_theme_system).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val appearance = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertEquals(true, appearance is AppearanceSettingsFragment)
                assertEquals(ThemeHelper.THEME_SYSTEM, ThemeHelper.getTheme(activity))
                assertEquals(true, appearance!!.requireView().findViewById<RadioButton>(R.id.radio_theme_system).isChecked)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_shell).visibility)
                appearance.requireView().findViewById<View>(R.id.btn_back).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val settings = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertEquals(true, settings is SettingsFragment)
                assertEquals(activity.getString(R.string.theme_system), settings!!.requireView()
                    .findViewById<TextView>(R.id.text_current_theme).text.toString())
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.nav_shell).visibility)
                assertEquals(R.id.nav_settings, activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
                activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_ledger
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                assertEquals(activity.getColor(R.color.background_light), (fragment?.view?.background as ColorDrawable).color)
            }
        }
    }
}
