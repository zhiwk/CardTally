package com.example.cardtally.util

import android.text.Editable
import android.text.TextPaint
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Keeps the record-entry amount fully visible and hugs its currency prefix.
 *
 * The amount field has no reserved `minEms`; while the text is short the field
 * stays `wrap_content`, so the prefix (`¥`) sits right next to the number. When
 * the text would overflow the width left after the note column, the field is
 * pinned to that width and the text size is reduced — computed here instead of
 * relying on framework auto-size, which does not shrink editable fields reliably.
 * A field already width-bounded by a `layout_weight` (the standard layout) is
 * never re-sized; it only receives the text-size reduction.
 */
class EntryAmountLayoutController(
    private val row: View,
    private val amount: EditText,
    private val prefix: TextView?
) {
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var textWatcher: TextWatcher? = null
    private var amountBaseSizePx = 0f
    private var lastAppliedSizePx = 0f
    private var isApplying = false

    fun attach() {
        if (layoutListener == null) {
            val added = ViewTreeObserver.OnGlobalLayoutListener { applyForWidth(row.width) }
            layoutListener = added
            row.viewTreeObserver.addOnGlobalLayoutListener(added)
            row.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit
                override fun onViewDetachedFromWindow(v: View) {
                    if (v.viewTreeObserver.isAlive) v.viewTreeObserver.removeOnGlobalLayoutListener(added)
                    layoutListener = null
                }
            })
        }
        if (textWatcher == null) {
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    applyForWidth(row.width)
                }
                override fun afterTextChanged(s: Editable?) = Unit
            }
            textWatcher = watcher
            amount.addTextChangedListener(watcher)
        }
    }

    fun detach() {
        layoutListener?.let {
            if (row.viewTreeObserver.isAlive) row.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        layoutListener = null
        textWatcher?.let { amount.removeTextChangedListener(it) }
        textWatcher = null
    }

    fun applyForWidth(rowWidth: Int) {
        if (isApplying) return
        isApplying = true
        try {
            if (amountBaseSizePx <= 0f) amountBaseSizePx = amount.textSize
            if (amountBaseSizePx <= 0f) return

            val params = amount.layoutParams
            val weighted = ((params as? LinearLayout.LayoutParams)?.weight ?: 0f) > 0f
            var changed = false

            val boundedWidth: Int
            if (weighted) {
                // Standard layout: the field is already bounded by its weight.
                boundedWidth = amount.width.takeIf { it > 0 } ?: 0
            } else {
                if (rowWidth <= 0) return
                val density = row.resources.displayMetrics.density
                val prefixWidth = prefix?.takeIf { it.visibility == View.VISIBLE }?.width ?: 0
                val prefixMargin = (prefix?.layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart ?: 0
                val amountMargin = (params as? ViewGroup.MarginLayoutParams)?.marginStart ?: 0
                val minNoteWidth = (MIN_NOTE_WIDTH_DP * density).toInt()
                val available = (rowWidth - row.paddingLeft - row.paddingRight -
                    prefixWidth - prefixMargin - amountMargin - minNoteWidth)
                    .takeIf { it > 0 } ?: rowWidth
                val natural = measureTextWidth(amount.text?.toString().orEmpty(), amountBaseSizePx)
                val target = if (natural > available) available else ViewGroup.LayoutParams.WRAP_CONTENT
                if (params != null && params.width != target) {
                    params.width = target
                    amount.layoutParams = params
                    changed = true
                }
                boundedWidth = if (natural > available) available else 0
            }

            val text = amount.text?.toString().orEmpty()
            val natural = measureTextWidth(text, amountBaseSizePx)
            val minPx = minSizePx()
            var size = amountBaseSizePx
            if (boundedWidth > 0 && natural > boundedWidth) {
                size = amountBaseSizePx * boundedWidth.toFloat() / natural
                if (size < minPx) size = minPx
                var guard = 0
                while (measureTextWidth(text, size) > boundedWidth && size > minPx && guard < 40) {
                    size -= 1f
                    guard++
                }
                if (size < minPx) size = minPx
            }
            if (setTextSizePx(size)) changed = true
            // Only request a new pass when something actually changed; otherwise the
            // global-layout callback would re-request forever (layout loop).
            if (changed) row.requestLayout()
        } finally {
            isApplying = false
        }
    }

    private fun measureTextWidth(text: String, sizePx: Float): Float {
        if (text.isEmpty()) return 0f
        val paint = TextPaint(amount.paint)
        paint.textSize = sizePx
        return paint.measureText(text)
    }

    private fun setTextSizePx(px: Float): Boolean {
        if (abs(px - lastAppliedSizePx) < 0.5f) return false
        lastAppliedSizePx = px
        amount.setTextSize(TypedValue.COMPLEX_UNIT_PX, px)
        return true
    }

    private fun minSizePx(): Float {
        val scaledDensity = amount.resources.displayMetrics.scaledDensity
        val baseSp = amountBaseSizePx / scaledDensity
        val minSp = (baseSp * 0.6f).coerceIn(MIN_AMOUNT_SIZE_SP, MAX_AMOUNT_SIZE_SP)
        return minSp * scaledDensity
    }

    companion object {
        /** Lower bound for a shrunk amount (smaller entry fields may reach it). */
        const val MIN_AMOUNT_SIZE_SP = 12f
        /** Upper bound for the shrunk amount floor so prominent amounts stay readable. */
        const val MAX_AMOUNT_SIZE_SP = 14f
        private const val MIN_NOTE_WIDTH_DP = 96f
    }
}
