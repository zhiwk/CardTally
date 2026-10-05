package com.example.cardtally

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerMonthPagerTest {
    @Test fun swipesChangeOneMonthAndToolbarStaysOutsidePager() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_ledger
            }
            instrumentation.waitForIdleSync()
            var initial = 0
            scenario.onActivity { activity ->
                val pager = activity.findViewById<ViewPager2>(R.id.pager_months)
                initial = pager.currentItem
                assertFalse(pager.findViewById<android.view.View>(R.id.ledger_toolbar) != null)
            }
            onView(withId(R.id.pager_months)).perform(swipeRight())
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(initial - 1, activity.findViewById<ViewPager2>(R.id.pager_months).currentItem)
            }
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(initial - 1, activity.findViewById<ViewPager2>(R.id.pager_months).currentItem)
            }
            onView(withId(R.id.pager_months)).perform(swipeLeft())
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(initial, activity.findViewById<ViewPager2>(R.id.pager_months).currentItem)
            }
        }
    }
    @Test fun statisticsIndicatorFollowsPagerProgress() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_statistics
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val pager = activity.findViewById<ViewPager2>(R.id.layout_statistics_content)
                pager.setCurrentItem(0, false)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val pager = activity.findViewById<ViewPager2>(R.id.layout_statistics_content)
                val indicator = activity.findViewById<android.view.View>(R.id.indicator_type_expense)
                val tab = activity.findViewById<android.view.View>(R.id.text_type_expense)
                val pages = pager.getChildAt(0) as androidx.recyclerview.widget.RecyclerView
                assertTrue(pages.childCount > 0)
                for (index in 0 until pages.childCount) {
                    val params = pages.getChildAt(index).layoutParams
                    assertEquals(android.view.ViewGroup.LayoutParams.MATCH_PARENT, params.width)
                    assertEquals(android.view.ViewGroup.LayoutParams.MATCH_PARENT, params.height)
                }
                val start = indicator.translationX
                assertTrue(pager.beginFakeDrag())
                pager.fakeDragBy(-pager.width * 0.4f)
                assertTrue(indicator.translationX > start)
                assertTrue(indicator.translationX < start + tab.width)
                pager.endFakeDrag()
            }
        }
    }

}
