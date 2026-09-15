package com.example.cardtally

import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.util.EntryAmountLayoutController
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntryAmountLayoutTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun themedContext() = ContextThemeWrapper(
        instrumentation.targetContext,
        R.style.Theme_CardTally_Light
    )

    private fun dp(value: Int): Int =
        (value * instrumentation.targetContext.resources.displayMetrics.density).toInt()

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        instrumentation.targetContext.resources.displayMetrics
    )

    private fun measureAndLayout(screen: View, width: Int, height: Int) {
        screen.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        screen.layout(0, 0, width, height)
    }

    @Test
    fun quickShortAmount_keepsPrefixHuggingTheAmount() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val screen = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.fragment_add_record_quick, FrameLayout(ctx), false)
            val amount = screen.findViewById<EditText>(R.id.edit_amount)
            amount.setText("12.34")
            measureAndLayout(screen, dp(360), dp(800))

            val prefix = screen.findViewById<TextView>(R.id.text_amount_prefix)
            val controller = EntryAmountLayoutController(amount.parent as View, amount, prefix)
            controller.applyForWidth((amount.parent as View).width)
            measureAndLayout(screen, dp(360), dp(800))

            assertNotNull(prefix)
            val gap = amount.left - prefix.right
            assertTrue("prefix must sit next to the amount (gap=${gap}px)", gap in 0..dp(10))
        }
    }

    @Test
    fun quickLongAmount_autosizesAndStaysComplete() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val screen = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.fragment_add_record_quick, FrameLayout(ctx), false)
            val amount = screen.findViewById<EditText>(R.id.edit_amount)
            val longText = "1234567890.12"
            amount.setText(longText)
            measureAndLayout(screen, dp(360), dp(800))

            val prefix = screen.findViewById<TextView>(R.id.text_amount_prefix)
            val controller = EntryAmountLayoutController(amount.parent as View, amount, prefix)
            controller.applyForWidth((amount.parent as View).width)
            measureAndLayout(screen, dp(360), dp(800))

            assertTrue(
                "long amount must shrink below the 30sp base " +
                    "(size=${amount.textSize} base=${sp(30f)} width=${amount.width} " +
                    "row=${(amount.parent as View).width} natural=${amount.paint.measureText(longText)})",
                amount.textSize < sp(30f)
            )
            assertTrue(
                "amount must not shrink below the readable floor",
                amount.textSize >= sp(12f) - 1f
            )
            val painted = amount.paint.measureText(longText)
            assertTrue(
                "full amount must be visible (painted=$painted width=${amount.width})",
                painted <= amount.width + 2f
            )
        }
    }

    @Test
    fun standardLongAmount_autosizesWithinItsBoundedField() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val screen = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.fragment_add_record, FrameLayout(ctx), false)
            val amount = screen.findViewById<EditText>(R.id.edit_amount)
            val longText = "123456789012345678.99"
            amount.setText(longText)
            measureAndLayout(screen, dp(320), dp(900))

            val controller = EntryAmountLayoutController(amount.parent as View, amount, null)
            controller.applyForWidth((amount.parent as View).width)
            measureAndLayout(screen, dp(320), dp(900))

            assertTrue(
                "standard amount must shrink for a long value " +
                    "(size=${amount.textSize} base=${sp(16f)} width=${amount.width} " +
                    "row=${(amount.parent as View).width} natural=${amount.paint.measureText(longText)})",
                amount.textSize < sp(16f)
            )
            val painted = amount.paint.measureText(longText)
            assertTrue(
                "full standard amount must fit (painted=$painted width=${amount.width})",
                painted <= amount.width + 2f
            )
        }
    }

    @Test
    fun entryAmountController_doesNotLoopLayoutsInARealActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddRecordFragment())
                    .commitNow()
            }

            val layoutCount = AtomicInteger(0)
            scenario.onActivity { activity ->
                val view = activity.supportFragmentManager
                    .findFragmentById(R.id.fragment_container)?.view
                view?.viewTreeObserver?.addOnGlobalLayoutListener { layoutCount.incrementAndGet() }
            }
            Thread.sleep(1500)
            val count = layoutCount.get()
            assertTrue(
                "amount row must settle instead of re-requesting layout forever (fired $count times in ~1.5s)",
                count < 60
            )
        }
    }
}
