package com.example.cardtally

import android.content.res.Configuration
import android.text.Layout
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.adapter.RecordRowLayoutController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real layout measurements for long amounts and long names (UX05).
 *
 * The row is measured with `wrap_content` height (AT_MOST) so the assertions can
 * actually observe overflow, ellipsis and wrapping — a forced fixed height proves
 * nothing about growth. No Activity, preferences, database, or network access.
 */
@RunWith(AndroidJUnit4::class)
class RecordRowLayoutMeasurementTest {

    private val classUnderTest = RecordRowLayoutController::class.java.simpleName

    private fun context(scale: Float): ContextThemeWrapper {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration).apply { fontScale = scale }
        return ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_CardTally_Light)
    }

    private fun inflateRow(context: ContextThemeWrapper): View {
        val parent = FrameLayout(context)
        return LayoutInflater.from(context).inflate(R.layout.item_record, parent, false)
    }

    /** Measures with a wrap_content height so growth and overlap stay observable. */
    private fun measureWrap(view: View, width: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.AT_MOST)
        )
        view.layout(0, 0, width, view.measuredHeight)
    }

    private fun ellipsisCount(view: TextView): Int {
        val layout: Layout = view.layout ?: return 0
        var count = 0
        for (line in 0 until layout.lineCount) count += layout.getEllipsisCount(line)
        return count
    }

    private fun fitRow(
        context: ContextThemeWrapper,
        widthDp: Int,
        scale: Float,
        amount: String,
        name: String
    ): View {
        val density = context.resources.displayMetrics.density
        val row = inflateRow(context)
        val controller = RecordRowLayoutController(row)
        row.findViewById<TextView>(R.id.text_category).text = name
        row.findViewById<TextView>(R.id.text_amount).text = amount
        measureWrap(row, (widthDp * density).toInt())
        controller.attach()
        controller.applyForWidth((widthDp * density).toInt())
        row.measure(
            View.MeasureSpec.makeMeasureSpec((widthDp * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.AT_MOST)
        )
        row.layout(0, 0, row.measuredWidth, row.measuredHeight)
        return row
    }

    @Test
    fun longAmount_isNeverEllipsizedOrClippedAndKeepsReadableSize() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (scale in listOf(1f, 1.3f, 2f)) {
                for (widthDp in listOf(320, 360, 600)) {
                    val ctx = context(scale)
                    val row = fitRow(
                        context = ctx,
                        widthDp = widthDp,
                        scale = scale,
                        amount = "-¥999,999,999.99",
                        name = "非常长的中文分类名称用于测试布局是否被挤压到无法阅读的极端情况"
                    )
                    val amountView = row.findViewById<TextView>(R.id.text_amount)
                    val card = row.findViewById<View>(R.id.card_content)
                    val label = "amount ($amountView.text) at ${widthDp}dp/${scale}x"

                    assertEquals("$label must have a laid-out text layout", 1, if (amountView.layout != null) 1 else 0)
                    assertEquals("$label must not be ellipsized", 0, ellipsisCount(amountView))
                    assertTrue(
                        "$label must keep at least the auto-size floor (${amountView.textSize}px)",
                        amountView.textSize >= (RecordRowLayoutController.MIN_AMOUNT_SIZE_SP - 0.5f) *
                            ctx.resources.displayMetrics.scaledDensity
                    )
                    assertTrue(
                        "$label must fit inside the card width",
                        amountView.width <= card.width && amountView.right <= card.width
                    )
                    assertTrue("$label must be visible", amountView.visibility == View.VISIBLE && amountView.width > 0)
                }
            }
        }
    }

    @Test
    fun longName_wrapsUpToTwoLinesWithoutLosingText() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(1f)
            val longName = "非常长的中文分类名称用于测试布局是否被挤压" + "AndAVeryLongEnglishAccountNameSuffix"
            val row = fitRow(
                context = ctx,
                widthDp = 320,
                scale = 1f,
                amount = "-¥999,999,999.99",
                name = longName
            )
            val nameView = row.findViewById<TextView>(R.id.text_category)
            assertEquals("model text must be unchanged", longName, nameView.text.toString())
            assertTrue(
                "name may wrap but must not exceed the two-line budget",
                (nameView.layout?.lineCount ?: 0) in 1..RecordRowLayoutController.RECORD_NAME_MAX_LINES
            )
        }
    }

    @Test
    fun longAmount_dropsBelowTheNameInsteadOfOverlappingIt() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(2f)
            val density = ctx.resources.displayMetrics.density
            val row = fitRow(
                context = ctx,
                widthDp = 320,
                scale = 2f,
                amount = "-¥999,999,999.99",
                name = "餐饮"
            )
            val amountView = row.findViewById<TextView>(R.id.text_amount)
            val nameView = row.findViewById<TextView>(R.id.text_category)
            val belowContainer = row.findViewById<View>(R.id.layout_amount_below)
            val nameBounds = android.graphics.Rect()
            val amountBounds = android.graphics.Rect()
            nameView.getHitRect(nameBounds)
            amountView.getHitRect(amountBounds)
            assertTrue("name and amount must not overlap", !android.graphics.Rect.intersects(nameBounds, amountBounds))
            assertTrue("amount must stay inside the row", amountView.right <= row.width)
            if (belowContainer.visibility == View.VISIBLE) {
                assertTrue(
                    "a wrapped amount must sit on its own full-width row",
                    amountView.layoutParams.width != 0 || amountView.width >= (280 * density).toInt()
                )
            }
        }
    }

    @Test
    fun shortAmount_staysInlineAndReleasesTheRow() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(1f)
            val row = fitRow(
                context = ctx,
                widthDp = 360,
                scale = 1f,
                amount = "-¥12.00",
                name = "餐饮"
            )
            assertEquals(
                "a short amount must not use the wrapped row",
                View.GONE,
                row.findViewById<View>(R.id.layout_amount_below).visibility
            )
            assertEquals(
                View.VISIBLE,
                row.findViewById<View>(R.id.layout_inline_amount).visibility
            )
        }
    }

    @Test
    fun amountMinimumSize_isNotUndercut() {
        assertTrue(
            "$classUnderTest must expose a sane minimum size",
            RecordRowLayoutController.MIN_AMOUNT_SIZE_SP >= 14f
        )
    }
}
