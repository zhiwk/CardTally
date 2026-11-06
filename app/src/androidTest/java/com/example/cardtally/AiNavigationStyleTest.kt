package com.example.cardtally

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.NestedScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.AiEntryStyle
import com.example.cardtally.util.AiEntryPosition
import com.example.cardtally.util.AssetDisplayHelper
import com.example.cardtally.util.BottomNavigationPreferences
import com.example.cardtally.util.BottomNavigationDestination
import com.example.cardtally.util.BottomNavigationOrder
import com.example.cardtally.util.QuickAddHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matcher

@RunWith(AndroidJUnit4::class)
class AiNavigationStyleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var savedPreferences: Map<Pair<String, String>, Any?>

    @Before
    fun setUp() {
        check(context.packageName.endsWith(".verification")) { "Use the isolated verification variant" }
        savedPreferences = listOf(
            "bottom_navigation_prefs" to "style",
            "bottom_navigation_prefs" to "ai_entry_style",
            "bottom_navigation_prefs" to "ai_entry_position",
            "bottom_navigation_prefs" to "item_order",
            "ai_assistant_settings_prefs" to "ai_assistant_enabled",
            "asset_display_prefs" to "show_asset_enabled",
            "quick_add_prefs" to "quick_add_enabled"
        ).associateWith { (name, key) -> context.getSharedPreferences(name, Context.MODE_PRIVATE).all[key] }
        BottomNavigationPreferences.saveDocked(context, false)
        BottomNavigationPreferences.resetMenuOrder(context)
        BottomNavigationPreferences.saveAiEntryStyle(context, AiEntryStyle.IN_MENU)
        context.getSharedPreferences("bottom_navigation_prefs", Context.MODE_PRIVATE).edit()
            .remove("ai_entry_position").commit()
        AiAssistantSettingsHelper.saveAiAssistantEnabled(context, true)
        AssetDisplayHelper.saveShowAsset(context, true)
        QuickAddHelper.saveQuickAdd(context, false)
    }

    @After
    fun restorePreferences() {
        if (!::savedPreferences.isInitialized) return
        savedPreferences.forEach { (entry, value) ->
            val (name, key) = entry
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().apply {
                when (value) {
                    null -> remove(key)
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> error("Unexpected navigation preference type")
                }
            }.commit()
        }
    }

    @Test
    fun settingsPreserveBothIndependentChoicesAcrossRecreation() {
        context.getSharedPreferences("bottom_navigation_prefs", Context.MODE_PRIVATE).edit()
            .remove("ai_entry_style").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse(activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .menu.findItem(R.id.nav_agent).isVisible)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.ai_navigation_button).visibility)
                assertEquals(AiEntryStyle.SEPARATE_CIRCLE, BottomNavigationPreferences.getAiEntryStyle(activity))
                assertEquals(AiEntryPosition.LEFT, BottomNavigationPreferences.getAiEntryPosition(activity))
                assertTrue(activity.findViewById<View>(R.id.ai_navigation_button).right <
                    activity.findViewById<View>(R.id.bottom_navigation).left)
                activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_settings
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                    .requireView().findViewById<View>(R.id.card_bottom_navigation).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val root = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!.requireView()
                assertTrue(root.findViewById<RadioButton>(R.id.radio_ai_entry_circle).isChecked)
                assertTrue(root.findViewById<RadioButton>(R.id.radio_ai_entry_left).isChecked)
                assertEquals(View.VISIBLE, root.findViewById<View>(R.id.section_ai_entry_position).visibility)
                root.findViewById<View>(R.id.row_ai_entry_right).performClick()
                root.findViewById<View>(R.id.row_ai_entry_circle).performClick()
                root.findViewById<View>(R.id.row_navigation_docked).performClick()
                assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_shell).visibility)
            }
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val root = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!.requireView()
                assertTrue(root.findViewById<RadioButton>(R.id.radio_ai_entry_circle).isChecked)
                assertTrue(root.findViewById<RadioButton>(R.id.radio_navigation_docked).isChecked)
                assertTrue(root.findViewById<RadioButton>(R.id.radio_ai_entry_right).isChecked)
                assertFalse(root.findViewById<RadioButton>(R.id.radio_ai_entry_menu).isChecked)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_shell).visibility)
                root.findViewById<View>(R.id.row_ai_entry_menu).performClick()
                assertEquals(View.GONE, root.findViewById<View>(R.id.section_ai_entry_position).visibility)
                assertEquals(AiEntryPosition.RIGHT, BottomNavigationPreferences.getAiEntryPosition(activity))
                root.findViewById<View>(R.id.row_ai_entry_circle).performClick()
                assertEquals(View.VISIBLE, root.findViewById<View>(R.id.section_ai_entry_position).visibility)
                assertTrue(root.findViewById<RadioButton>(R.id.radio_ai_entry_right).isChecked)
                root.findViewById<View>(R.id.btn_back).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.ai_navigation_button).visibility)
                assertFalse(activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .menu.findItem(R.id.nav_agent).isVisible)
                BottomNavigationPreferences.saveAiEntryStyle(activity, AiEntryStyle.IN_MENU)
                activity.refreshBottomNavigationStyle()
                assertTrue(BottomNavigationPreferences.isDocked(activity))
                assertEquals(AiEntryPosition.RIGHT, BottomNavigationPreferences.getAiEntryPosition(activity))
                assertEquals(View.GONE, activity.findViewById<View>(R.id.ai_navigation_button).visibility)
                assertTrue(activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .menu.findItem(R.id.nav_agent).isVisible)
            }
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(AiEntryStyle.IN_MENU, BottomNavigationPreferences.getAiEntryStyle(activity))
                assertEquals(AiEntryPosition.RIGHT, BottomNavigationPreferences.getAiEntryPosition(activity))
                assertEquals(View.GONE, activity.findViewById<View>(R.id.ai_navigation_button).visibility)
                assertTrue(activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .menu.findItem(R.id.nav_agent).isVisible)
            }
            verifyMenuOrderControls(scenario)
        }
    }

    private fun verifyMenuOrderControls(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity { activity ->
            activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                .requireView().findViewById<View>(R.id.card_bottom_navigation).performClick()
        }
        instrumentation.waitForIdleSync()
        verifyMenuDragGestures(scenario)
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
            val list = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_navigation_order)
            assertEquals("in-menu AI participates in sorting", 5, list.adapter!!.itemCount)
            val first = requireNotNull(list.findViewHolderForAdapterPosition(0)).itemView
            assertTrue(first.performAccessibilityAction(R.id.action_navigation_order_down, null))
            assertSame(fragment, activity.supportFragmentManager.findFragmentById(R.id.fragment_container))
            assertEquals(R.id.nav_settings, activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
        }
        scenario.recreate()
        instrumentation.waitForIdleSync()
        scenario.onActivity { activity ->
            val root = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!.requireView()
            val menu = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).menu
            assertEquals(BottomNavigationDestination.STATISTICS, BottomNavigationPreferences.getMenuOrder(activity).first())
            assertEquals(R.id.nav_statistics, menu.getItem(0).itemId)
            assertTrue(menu.findItem(R.id.nav_settings).isChecked)
            val list = root.findViewById<RecyclerView>(R.id.recycler_navigation_order)
            assertEquals(activity.getString(R.string.nav_statistics),
                requireNotNull(list.findViewHolderForAdapterPosition(0)).itemView
                    .findViewById<TextView>(R.id.text_navigation_order).text.toString())
            root.findViewById<View>(R.id.row_ai_entry_circle).performClick()
            assertEquals("independent AI is not an in-menu sort row", 4, list.adapter!!.itemCount)
            AssetDisplayHelper.saveShowAsset(activity, false)
            root.findViewById<View>(R.id.row_ai_entry_circle).performClick()
            assertEquals("hidden assets do not become enabled by sorting", 3, list.adapter!!.itemCount)
            root.findViewById<View>(R.id.button_reset_navigation_order).performClick()
            assertEquals(BottomNavigationOrder.defaultOrder, BottomNavigationPreferences.getMenuOrder(activity))
            assertFalse(AssetDisplayHelper.getShowAsset(activity))
            assertEquals(AiEntryStyle.SEPARATE_CIRCLE, BottomNavigationPreferences.getAiEntryStyle(activity))
            assertEquals(AiEntryPosition.RIGHT, BottomNavigationPreferences.getAiEntryPosition(activity))
            assertTrue(BottomNavigationPreferences.isDocked(activity))
            assertEquals(View.GONE, activity.findViewById<View>(R.id.nav_shell).visibility)
        }
    }

    private fun verifyMenuDragGestures(scenario: ActivityScenario<MainActivity>) {
        var expected = BottomNavigationPreferences.getMenuOrder(context)
        // Exercise fresh immediate/held presses repeatedly, not the adapter or accessibility shortcut.
        for (holdMillis in listOf(0L, 450L, 0L, 450L)) {
            onView(withId(R.id.recycler_navigation_order)).perform(MenuHandleGesture(holdMillis, cancel = false))
            expected = expected.toMutableList().apply { add(1, removeAt(0)) }
            scenario.onActivity { activity ->
                assertEquals("every handle gesture must reorder and persist", expected,
                    BottomNavigationPreferences.getMenuOrder(activity))
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is BottomNavigationSettingsFragment)
                assertEquals(R.id.nav_settings, activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
            }
        }
        onView(withId(R.id.recycler_navigation_order)).perform(MenuHandleGesture(0L, cancel = true))
        assertEquals("cancelling before movement must not change the saved order", expected,
            BottomNavigationPreferences.getMenuOrder(context))
        onView(withId(R.id.recycler_navigation_order)).perform(MenuHandleGesture(0L, cancel = false))
        expected = expected.toMutableList().apply { add(1, removeAt(0)) }
        assertEquals("a fresh drag must still work after cancellation", expected,
            BottomNavigationPreferences.getMenuOrder(context))
        scenario.onActivity { activity ->
            activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!.requireView()
                .findViewById<View>(R.id.button_reset_navigation_order).performClick()
        }
        instrumentation.waitForIdleSync()
    }

    /** Inject through the window, so NestedScrollView and RecyclerView must arbitrate real touch. */
    private class MenuHandleGesture(private val holdMillis: Long, private val cancel: Boolean) : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)
        override fun getDescription() = "press the first menu handle, then drag or cancel through the real touch hierarchy"

        override fun perform(uiController: UiController, view: View) {
            val list = view as RecyclerView
            val first = requireNotNull(list.findViewHolderForAdapterPosition(0)).itemView
            val second = requireNotNull(list.findViewHolderForAdapterPosition(1)).itemView
            list.requestRectangleOnScreen(Rect(0, first.top, list.width, second.bottom), true)
            uiController.loopMainThreadUntilIdle()
            val handle = first.findViewById<View>(R.id.handle_navigation_order)
            val visible = Rect()
            assertTrue("the handle must be visible before injecting touch", handle.getGlobalVisibleRect(visible))
            val handleLocation = IntArray(2).also(handle::getLocationOnScreen)
            val targetLocation = IntArray(2).also(second::getLocationOnScreen)
            val x = handleLocation[0] + handle.width / 2f
            val startY = handleLocation[1] + handle.height / 2f
            // Cross the adjacent row's center far enough for ItemTouchHelper's drop threshold.
            val endY = targetLocation[1] + second.height * .75f
            val scroll = requireNotNull(generateSequence(list.parent) { it.parent }
                .filterIsInstance<NestedScrollView>().firstOrNull())
            val initialScrollY = scroll.scrollY
            val downTime = SystemClock.uptimeMillis()
            fun inject(action: Int, y: Float) {
                val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try {
                    assertTrue("touch injection must succeed", uiController.injectMotionEvent(event))
                } finally {
                    event.recycle()
                }
            }
            inject(MotionEvent.ACTION_DOWN, startY)
            if (holdMillis > 0) uiController.loopMainThreadForAtLeast(holdMillis)
            if (cancel) {
                inject(MotionEvent.ACTION_CANCEL, startY)
            } else {
                for (step in 1..8) {
                    inject(MotionEvent.ACTION_MOVE, startY + (endY - startY) * step / 8f)
                    uiController.loopMainThreadForAtLeast(16L)
                }
                inject(MotionEvent.ACTION_UP, endY)
            }
            // Let ItemTouchHelper's release/recovery animation reach clearView and persist.
            uiController.loopMainThreadForAtLeast(600L)
            uiController.loopMainThreadUntilIdle()
            assertEquals("dragging the handle must not scroll the outer page", initialScrollY, scroll.scrollY)
        }
    }

    @Test
    fun separateAiSharesTheExistingPageAndHasExclusiveSelection() {
        BottomNavigationPreferences.saveAiEntryStyle(context, AiEntryStyle.SEPARATE_CIRCLE)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.ai_navigation_button).performClick()
                activity.findViewById<View>(R.id.ai_navigation_button).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is AgentFragment)
                assertTrue(activity.findViewById<View>(R.id.ai_navigation_button).isSelected)
                val menu = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).menu
                assertEquals(R.id.nav_agent, activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
                for (index in 0 until menu.size()) {
                    assertEquals(menu.getItem(index).itemId == R.id.nav_agent, menu.getItem(index).isChecked)
                }
                val current = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                BottomNavigationPreferences.saveMenuOrder(activity, BottomNavigationOrder.defaultOrder.reversed())
                BottomNavigationPreferences.saveAiEntryPosition(activity, AiEntryPosition.RIGHT)
                activity.refreshBottomNavigationStyle()
                assertEquals(R.id.nav_settings, activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).menu.getItem(0).itemId)
                assertTrue(activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).menu.findItem(R.id.nav_agent).isChecked)
                assertTrue(activity.findViewById<View>(R.id.ai_navigation_button).isSelected)
                assertSame(current, activity.supportFragmentManager.findFragmentById(R.id.fragment_container))
                activity.findViewById<View>(R.id.ai_navigation_button).performClick()
                activity.supportFragmentManager.executePendingTransactions()
                assertSame(current, activity.supportFragmentManager.findFragmentById(R.id.fragment_container))
            }
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<View>(R.id.ai_navigation_button).isSelected)
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is AgentFragment)
                activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_settings
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse(activity.findViewById<View>(R.id.ai_navigation_button).isSelected)
                assertTrue(activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .menu.findItem(R.id.nav_settings).isChecked)
            }
        }
    }

    @Test
    fun bothMenuPositionsRespectToggleAssetVisibilityAndContentClearance() {
        BottomNavigationPreferences.saveAiEntryStyle(context, AiEntryStyle.SEPARATE_CIRCLE)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            for ((docked, position) in listOf(false, true).flatMap { docked ->
                AiEntryPosition.entries.map { position -> docked to position }
            }) {
                scenario.onActivity { activity ->
                    BottomNavigationPreferences.saveDocked(activity, docked)
                    BottomNavigationPreferences.saveAiEntryPosition(activity, position)
                    AssetDisplayHelper.saveShowAsset(activity, true)
                    activity.refreshBottomNavigationStyle()
                }
                instrumentation.waitForIdleSync()
                var separateMenuWidth = 0
                scenario.onActivity { activity ->
                    val menu = activity.findViewById<View>(R.id.bottom_navigation)
                    val circle = activity.findViewById<View>(R.id.ai_navigation_button)
                    val shell = activity.findViewById<View>(R.id.nav_shell)
                    val page = activity.findViewById<View>(R.id.fragment_container)
                    assertEquals(circle.width, circle.height)
                    if (position == AiEntryPosition.LEFT) {
                        assertTrue(circle.right < menu.left)
                    } else {
                        assertTrue(menu.right < circle.left)
                    }
                    assertTrue(circle.left >= shell.paddingLeft)
                    assertTrue(circle.right <= shell.width - shell.paddingRight)
                    val params = circle.layoutParams as android.widget.LinearLayout.LayoutParams
                    val density = activity.resources.displayMetrics.density
                    val gap = (12 * density + .5f).toInt()
                    val edge = if (docked) (8 * density + .5f).toInt() else 0
                    assertEquals(if (position == AiEntryPosition.LEFT) edge else gap, params.marginStart)
                    assertEquals(if (position == AiEntryPosition.LEFT) gap else edge, params.marginEnd)
                    if (docked) assertTrue(page.bottom <= shell.top) else assertEquals(shell.bottom, page.bottom)
                    val fab = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
                        .requireView().findViewById<View>(R.id.fab_add)
                    val location = IntArray(2)
                    fab.getLocationInWindow(location)
                    val shellLocation = IntArray(2)
                    shell.getLocationInWindow(shellLocation)
                    assertTrue(location[1] + fab.height <= shellLocation[1] + shell.paddingTop)
                    separateMenuWidth = menu.width
                    AiAssistantSettingsHelper.saveAiAssistantEnabled(activity, false)
                    activity.refreshBottomNavigationStyle()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val menu = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    assertEquals(View.GONE, activity.findViewById<View>(R.id.ai_navigation_button).visibility)
                    assertFalse(menu.menu.findItem(R.id.nav_agent).isVisible)
                    assertTrue(menu.width > separateMenuWidth)
                    assertEquals(AiEntryStyle.SEPARATE_CIRCLE, BottomNavigationPreferences.getAiEntryStyle(activity))
                    assertEquals(position, BottomNavigationPreferences.getAiEntryPosition(activity))
                    AiAssistantSettingsHelper.saveAiAssistantEnabled(activity, true)
                    AssetDisplayHelper.saveShowAsset(activity, false)
                    activity.refreshBottomNavigationStyle()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val menu = activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).menu
                    assertFalse(menu.findItem(R.id.nav_asset).isVisible)
                    assertFalse(menu.findItem(R.id.nav_agent).isVisible)
                    assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.ai_navigation_button).visibility)
                }
            }
        }
    }
}
