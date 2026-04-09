package com.example.cardtally

import android.content.Context
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference
import com.example.cardtally.util.QuickAddHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityFabLayoutTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        QuickAddHelper.saveQuickAdd(context, false)
    }

    @Test
    fun pagesWithBottomNav_shareFabSpacingAndAssetUsesFloatingAction() {
        val expectedGap = context.resources
            .getDimensionPixelSize(R.dimen.floating_primary_fab_gap_above_nav)
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            assertEquals(expectedGap, measureCurrentFabGap(scenario, "home"))

            scenario.onActivity { activity ->
                val bottomNavigation = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNavigation.selectedItemId = R.id.nav_statistics
            }
            instrumentation.waitForIdleSync()
            assertEquals(expectedGap, measureCurrentFabGap(scenario, "statistics"))

            scenario.onActivity { activity ->
                val bottomNavigation = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNavigation.selectedItemId = R.id.nav_asset
            }
            instrumentation.waitForIdleSync()
            assertEquals(expectedGap, measureCurrentFabGap(scenario, "asset"))

            scenario.onActivity { activity ->
                val assetFragmentView = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                val assetFab = requireNotNull(assetFragmentView?.findViewById<FloatingActionButton>(R.id.fab_add))
                assertEquals(View.VISIBLE, assetFab.visibility)
            }
        }
    }

    @Test
    fun secondaryPages_hideFloatingNavigationShell() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val navShell = activity.findViewById<View>(R.id.nav_shell)
                assertEquals(View.VISIBLE, navShell.visibility)

                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddRecordFragment())
                    .addToBackStack(null)
                    .commit()
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val navShell = activity.findViewById<View>(R.id.nav_shell)
                assertEquals(View.GONE, navShell.visibility)

                activity.supportFragmentManager.popBackStack()
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val navShell = activity.findViewById<View>(R.id.nav_shell)
                assertEquals(View.VISIBLE, navShell.visibility)
            }
        }
    }

    @Test
    fun agentSessionDrawer_hidesFloatingNavigationShellWhileExpanded() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val bottomNavigation = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNavigation.selectedItemId = R.id.nav_agent
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val navShell = activity.findViewById<View>(R.id.nav_shell)
                val agentFragmentView = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                val buttonMenu = requireNotNull(agentFragmentView?.findViewById<View>(R.id.button_agent_menu))

                assertEquals(View.VISIBLE, navShell.visibility)
                buttonMenu.performClick()
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val navShell = activity.findViewById<View>(R.id.nav_shell)
                val agentFragmentView = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                val overlay = requireNotNull(agentFragmentView?.findViewById<View>(R.id.view_agent_session_overlay))

                assertEquals(View.GONE, navShell.visibility)
                overlay.performClick()
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val navShell = activity.findViewById<View>(R.id.nav_shell)
                assertEquals(View.VISIBLE, navShell.visibility)
            }
        }
    }

    private fun measureCurrentFabGap(scenario: ActivityScenario<MainActivity>, label: String): Int {
        val measuredGap = AtomicReference<Int>()
        scenario.onActivity { activity ->
            activity.supportFragmentManager.executePendingTransactions()
            val navShell = activity.findViewById<View>(R.id.nav_shell)
            val fragmentView = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
            val fab = requireNotNull(fragmentView?.findViewById<FloatingActionButton>(R.id.fab_add)) {
                "Missing fab_add for $label"
            }
            measuredGap.set(navShell.topOnScreen() - fab.bottomOnScreen())
        }
        return measuredGap.get()
    }

    private fun View.topOnScreen(): Int {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return location[1]
    }

    private fun View.bottomOnScreen(): Int {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return location[1] + height
    }
}
