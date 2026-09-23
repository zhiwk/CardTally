package com.example.cardtally

import android.graphics.Rect
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecurringEntryLayoutTest {
    @Test
    fun statusRowCanScrollAboveFixedKeypadOnShortScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_CardTally_Light)
            val screen = LayoutInflater.from(context).inflate(
                R.layout.fragment_recurring_record_edit, FrameLayout(context), false
            )
            val density = context.resources.displayMetrics.density
            screen.measure(
                View.MeasureSpec.makeMeasureSpec((360 * density).toInt(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec((600 * density).toInt(), View.MeasureSpec.EXACTLY)
            )
            screen.layout(0, 0, screen.measuredWidth, screen.measuredHeight)

            val scroll = (screen as android.widget.LinearLayout).getChildAt(0) as ScrollView
            val keypad = screen.findViewById<View>(R.id.layout_amount_keypad)
            val status = screen.findViewById<View>(R.id.switch_recurring_enabled)
            assertTrue("form must be scrollable on a short screen", scroll.getChildAt(0).height > scroll.height)
            scroll.scrollTo(0, scroll.getChildAt(0).height)
            val bounds = Rect()
            status.getDrawingRect(bounds)
            screen.offsetDescendantRectToMyCoords(status, bounds)
            assertTrue("status control must remain above keypad", bounds.bottom <= keypad.top)
            assertTrue("status control must remain inside visible scroll region", bounds.top >= scroll.top)
        }
    }
}
