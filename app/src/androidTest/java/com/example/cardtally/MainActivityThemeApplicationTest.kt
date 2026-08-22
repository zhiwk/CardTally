package com.example.cardtally

import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.util.ThemeHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityThemeApplicationTest {

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ThemeHelper.saveTheme(context, 1)
    }

    @Test
    fun staleDarkPreference_appliesLightToLedgerAndSettingsScreens() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val expectedBackground = context.getColor(R.color.background_light)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val currentFragmentView = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                assertEquals(true, activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is LedgerFragment)
                assertEquals(R.id.nav_ledger, activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
                assertEquals(expectedBackground, (currentFragmentView?.background as ColorDrawable).color)
            }

            scenario.onActivity { activity ->
                val bottomNavigation = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNavigation.selectedItemId = R.id.nav_settings
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val currentFragmentView = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                assertEquals(expectedBackground, (currentFragmentView?.background as ColorDrawable).color)
            }
        }
    }
}
