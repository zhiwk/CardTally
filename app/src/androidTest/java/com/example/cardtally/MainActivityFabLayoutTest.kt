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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            assertEquals(expectedGap, measureCurrentFabGap(scenario, "ledger"))

            scenario.onActivity { activity ->
                val bottomNavigation = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNavigation.selectedItemId = R.id.nav_ledger
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
    fun ledgerSelectorReturn_keepsScrollFabAbovePrimaryFab() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val ledgerView = requireNotNull(
                    activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                )
                ledgerView.findViewById<View>(R.id.button_ledger_switch).performClick()
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is LedgerManagementFragment)
                activity.supportFragmentManager.popBackStack()
            }
            instrumentation.waitForIdleSync()

            scenario.onActivity { activity ->
                val ledgerView = requireNotNull(
                    activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                )
                val scrollFab = requireNotNull(ledgerView.findViewById<FloatingActionButton>(R.id.fab_scroll_top))
                val addFab = requireNotNull(ledgerView.findViewById<FloatingActionButton>(R.id.fab_add))
                scrollFab.visibility = View.VISIBLE

                assertFalse(
                    "The return-to-top FAB must not overlap the primary add FAB after returning from ledger selection.",
                    boundsOverlap(scrollFab, addFab)
                )
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

    private fun boundsOverlap(first: View, second: View): Boolean {
        val firstLocation = IntArray(2)
        val secondLocation = IntArray(2)
        first.getLocationOnScreen(firstLocation)
        second.getLocationOnScreen(secondLocation)
        return firstLocation[0] < secondLocation[0] + second.width &&
            firstLocation[0] + first.width > secondLocation[0] &&
            firstLocation[1] < secondLocation[1] + second.height &&
            firstLocation[1] + first.height > secondLocation[1]
    }
}
