package com.example.cardtally

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * UX16: the shared amount keypad must really render readable key labels.
 *
 * Asserting `android:text` alone is exactly the false PASS the round-6 review
 * called out, so this test inflates the production keypad inside the production
 * theme, measures/lays it out and draws it, then checks (a) the resolved
 * foreground/background pair for every key, (b) the real painted pixels on each
 * key face, and (c) that a regression to the Material3 button surface is caught.
 * No network, preferences or database access.
 */
@RunWith(AndroidJUnit4::class)
class AmountKeypadRenderTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val normalKeyIds = listOf(
        R.id.keypad_0, R.id.keypad_1, R.id.keypad_2, R.id.keypad_3, R.id.keypad_4,
        R.id.keypad_5, R.id.keypad_6, R.id.keypad_7, R.id.keypad_8, R.id.keypad_9,
        R.id.keypad_dot, R.id.keypad_minus, R.id.keypad_plus, R.id.keypad_delete
    )

    private fun themedContext(): ContextThemeWrapper {
        val base = instrumentation.targetContext
        return ContextThemeWrapper(base, R.style.Theme_CardTally_Light)
    }

    /** Colour actually painted behind the text, honouring the view's state. */
    private fun surfaceColorOf(view: View): Int? =
        colorOfDrawable(view.background, view.drawableState)

    private fun colorOfDrawable(drawable: Drawable?, state: IntArray): Int? = when (drawable) {
        is ColorDrawable -> drawable.color
        is StateListDrawable -> {
            drawable.setState(state)
            colorOfDrawable(drawable.current, state)
        }
        is android.graphics.drawable.GradientDrawable -> {
            if (drawable.color != null) drawable.color?.defaultColor else null
        }
        is android.graphics.drawable.LayerDrawable -> {
            (0 until drawable.numberOfLayers)
                .mapNotNull { colorOfDrawable(drawable.getDrawable(it), state) }
                .firstOrNull()
        }
        is android.graphics.drawable.InsetDrawable -> colorOfDrawable(drawable.drawable, state)
        null -> null
        else -> null
    }

    private fun relativeLuminance(color: Int): Double {
        fun channel(value: Int): Double {
            val normalized = value / 255.0
            return if (normalized <= 0.03928) normalized / 12.92
            else ((normalized + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(Color.red(color)) +
            0.7152 * channel(Color.green(color)) +
            0.0722 * channel(Color.blue(color))
    }

    private fun contrastRatio(foreground: Int, background: Int): Double {
        val first = relativeLuminance(foreground)
        val second = relativeLuminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private fun layoutKeypad(): LinearLayout {
        val ctx = themedContext()
        val root = LinearLayout(ctx)
        val keypad = android.view.LayoutInflater.from(ctx)
            .inflate(R.layout.layout_amount_keypad, root, false) as LinearLayout
        keypad.visibility = View.VISIBLE
        root.addView(keypad)
        val density = ctx.resources.displayMetrics.density
        val width = (360 * density).toInt()
        val height = (420 * density).toInt()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
        )
        root.layout(0, 0, width, root.measuredHeight)
        return keypad
    }

    /**
     * Distinct colours in the middle band of the key: proves a glyph was painted.
     * Coordinates come from window positions because the keys live in nested
     * containers, so their `left`/`top` are parent-relative and would be wrong.
     */
    private fun distinctColorsInCenter(key: View, keypadOrigin: IntArray, bitmap: Bitmap): Int {
        val keyLocation = IntArray(2)
        key.getLocationInWindow(keyLocation)
        val left = keyLocation[0] - keypadOrigin[0] + key.width * 32 / 100
        val right = keyLocation[0] - keypadOrigin[0] + key.width - key.width * 32 / 100
        val top = keyLocation[1] - keypadOrigin[1] + key.height * 28 / 100
        val bottom = keyLocation[1] - keypadOrigin[1] + key.height - key.height * 28 / 100
        val colors = HashSet<Int>()
        for (x in left.coerceAtLeast(0) until right.coerceAtMost(bitmap.width)) {
            for (y in top.coerceAtLeast(0) until bottom.coerceAtMost(bitmap.height)) {
                colors.add(bitmap.getPixel(x, y))
            }
        }
        return colors.size
    }

    @Test
    fun everyKey_rendersReadableForegroundOnItsRealSurface() {
        instrumentation.runOnMainSync {
            val keypad = layoutKeypad()
            val bitmap = Bitmap.createBitmap(keypad.width, keypad.height, Bitmap.Config.ARGB_8888)
            keypad.draw(Canvas(bitmap))
            val location = IntArray(2)
            keypad.getLocationInWindow(location)

            val failures = mutableListOf<String>()
            (normalKeyIds + R.id.keypad_confirm).forEach { id ->
                val key = keypad.findViewById<TextView>(id)
                assertNotNull("keypad key $id must exist", key)
                val label = key!!.text?.toString().orEmpty()
                val accessibility = key.contentDescription?.toString().orEmpty()
                assertTrue(
                    "key $id must carry an accessible label",
                    label.isNotBlank() || accessibility.isNotBlank()
                )

                val surface = surfaceColorOf(key)
                if (surface == null) {
                    failures += "key $id ('$label') has no explicit opaque surface " +
                        "(bg=${key.background?.javaClass?.name})"
                    return@forEach
                }
                val ratio = contrastRatio(key.currentTextColor, surface)
                if (ratio < MIN_CONTRAST) {
                    failures += "key $id ('$label') contrast %.2f (fg=%08X bg=%08X)"
                        .format(ratio, key.currentTextColor, surface)
                }
                if (key.width < dp(48) || key.height < dp(48)) {
                    failures += "key $id ('$label') touch target ${key.width}x${key.height} < 48dp"
                }
                val glyphColors = distinctColorsInCenter(key, location, bitmap)
                if (glyphColors < 2) {
                    failures += "key $id ('$label') painted no visible glyph ($glyphColors colour)"
                }
            }
            assertTrue(failures.joinToString("\n"), failures.isEmpty())
        }
    }

    @Test
    fun primaryKey_usesASeparateHighContrastSurface() {
        instrumentation.runOnMainSync {
            val keypad = layoutKeypad()
            val confirm = keypad.findViewById<TextView>(R.id.keypad_confirm)
            val digit = keypad.findViewById<TextView>(R.id.keypad_7)
            assertTrue(
                "the confirm/= key must be visually distinct from the digit keys",
                surfaceColorOf(confirm) != surfaceColorOf(digit)
            )
            assertTrue(
                "confirm/= text must stay readable",
                contrastRatio(confirm.currentTextColor, surfaceColorOf(confirm)!!) >= MIN_CONTRAST
            )
            assertTrue(
                "digit keys must not inherit the primary surface",
                surfaceColorOf(digit) != surfaceColorOf(confirm)
            )
        }
    }

    @Test
    fun keypad_inflatedInsideTheRecordAndAssetScreens_isReadable() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            listOf(R.layout.fragment_add_record, R.layout.fragment_add_asset).forEach { layoutId ->
                val container = FrameLayout(ctx)
                val screen = android.view.LayoutInflater.from(ctx).inflate(layoutId, container, false)
                val keypad = findKeypad(screen)
                assertNotNull("$layoutId must host the shared amount keypad", keypad)
                keypad!!.visibility = View.VISIBLE
                val density = ctx.resources.displayMetrics.density
                val width = (360 * density).toInt()
                // AT_MOST height: the record screen root is match_parent, and
                // forcing an exact height would squeeze the keypad out of bounds.
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec((900 * density).toInt(), View.MeasureSpec.AT_MOST)
                )
                screen.layout(0, 0, width, screen.measuredHeight)
                listOf(R.id.keypad_7, R.id.keypad_delete, R.id.keypad_confirm).forEach { id ->
                    val key = keypad.findViewById<TextView>(id)
                    assertTrue(
                        "$layoutId key $id must meet 48dp",
                        key.width >= dp(48) && key.height >= dp(48)
                    )
                    assertTrue(
                        "$layoutId key $id must keep readable contrast",
                        contrastRatio(key.currentTextColor, surfaceColorOf(key)!!) >= MIN_CONTRAST
                    )
                }
            }
        }
    }

    @Test
    fun keypadOpensInARealActivity_andKeepsItsLabels() {
        // Exercises the production controller path (bind + show), not only the
        // inflated layout, without touching records or the database.
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddRecordFragment())
                    .commitNow()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager
                    .findFragmentById(R.id.fragment_container) as AddRecordFragment
                val screen = requireNotNull(fragment.view)
                val keypad = findKeypad(screen)
                assertNotNull("the record screen must host the shared keypad", keypad)
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(dp(360), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp(900), View.MeasureSpec.AT_MOST)
                )
                screen.layout(0, 0, dp(360), screen.measuredHeight)
                listOf(R.id.keypad_7, R.id.keypad_dot, R.id.keypad_minus, R.id.btn_save)
                    .forEach { id ->
                        val key = keypad!!.findViewById<TextView>(id)
                        assertTrue(
                            "key $id must stay readable in the live screen",
                            contrastRatio(key.currentTextColor, surfaceColorOf(key)!!) >= MIN_CONTRAST
                        )
                    }
            }
        }
    }

    private fun findKeypad(view: View): View? {
        if (view.id == R.id.layout_amount_keypad) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findKeypad(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    private fun dp(value: Int): Int =
        (value * instrumentation.targetContext.resources.displayMetrics.density).toInt()

    companion object {
        private const val MIN_CONTRAST = 4.5
    }
}
