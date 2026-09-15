package com.example.cardtally.adapter

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import com.example.cardtally.R

/**
 * Keeps a bill row readable when the amount is long or the font is enlarged.
 *
 * The amount is a key value and must always be fully visible. While it still fits
 * beside the name it stays inline; when it would be squeezed — or would have to
 * shrink below [MIN_AMOUNT_SIZE_SP] — the amount moves onto its own full-width row
 * below the name instead of being ellipsized, overlapping, or clipped.
 *
 * The name also gets the width it is actually given, with up to
 * [RECORD_NAME_MAX_LINES] lines. Expected layout contract inside the card:
 * `row_content` (horizontal, holds the icon, the name column and
 * `layout_inline_amount`) plus `layout_amount_below` (vertical, initially gone)
 * with `text_amount` moving between them.
 */
class RecordRowLayoutController(private val itemView: View) {

    private val cardContent: View? = itemView.findViewById(R.id.card_content)
    private val rowContent: LinearLayout? = itemView.findViewById(R.id.row_content)
    private val inlineAmount: View? = itemView.findViewById(R.id.layout_inline_amount)
    private val belowAmount: View? = itemView.findViewById(R.id.layout_amount_below)
    private val iconView: View? = itemView.findViewById(R.id.view_icon)
    private val categoryText: TextView? = itemView.findViewById(R.id.text_category)
    private val amountText: TextView? = itemView.findViewById(R.id.text_amount)

    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var amountBaseSizePx = 0f
    private var nameBaseSizePx = 0f

    /** Attaches a layout listener so every re-layout re-applies the fitting rules. */
    fun attach() {
        val anchor = cardContent ?: return
        if (layoutListener != null) return
        val added = ViewTreeObserver.OnGlobalLayoutListener { applyLayout() }
        layoutListener = added
        anchor.viewTreeObserver.addOnGlobalLayoutListener(added)
        anchor.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (v.viewTreeObserver.isAlive) {
                    v.viewTreeObserver.removeOnGlobalLayoutListener(added)
                }
                layoutListener = null
            }
        })
    }

    fun detach() {
        val anchor = cardContent ?: return
        layoutListener?.let {
            if (anchor.viewTreeObserver.isAlive) anchor.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        layoutListener = null
    }

    /** Re-applies the fitting rules using the card's current laid-out width. */
    fun applyLayout() {
        val width = cardContent?.width ?: 0
        if (width <= 0) return
        applyForWidth(width)
    }

    /**
     * Deterministic core of the rule, separated from view geometry so it can be
     * exercised directly by layout tests.
     */
    fun applyForWidth(cardWidth: Int) {
        val card = cardContent ?: return
        val row = rowContent ?: return
        val inline = inlineAmount ?: return
        val below = belowAmount ?: return
        val amount = amountText ?: return
        val category = categoryText ?: return
        if (cardWidth <= 0) return

        if (amountBaseSizePx == 0f) amountBaseSizePx = amount.textSize
        if (nameBaseSizePx == 0f) nameBaseSizePx = category.textSize
        configureAmountAutoSize(amount)
        category.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameBaseSizePx)

        val density = card.resources.displayMetrics.density
        val contentWidth = cardWidth - card.paddingLeft - card.paddingRight
        val iconWidth = iconView?.takeIf { it.visibility == View.VISIBLE }?.width?.takeIf { it > 0 } ?: 0
        val iconGap = if (iconWidth > 0) (12 * density).toInt() else 0
        val amountGap = (12 * density).toInt()
        val minNameWidth = (96 * density).toInt()

        // The amount always gets the space left after the icon and a readable name
        // column, in a full-width container, and auto-sizing keeps it complete.
        val availableForAmount = contentWidth - iconWidth - iconGap - minNameWidth - amountGap
        val amountNaturalWidth = measureNaturalWidth(amount)
        val minAmountPx = MIN_AMOUNT_SIZE_SP * card.resources.displayMetrics.scaledDensity
        val shrunkSizePx = if (amountNaturalWidth > 0 && availableForAmount > 0) {
            amountBaseSizePx * (availableForAmount.toFloat() / amountNaturalWidth)
        } else {
            0f
        }
        val fitsInline = availableForAmount > 0 &&
            amountNaturalWidth > 0 &&
            (amountNaturalWidth <= availableForAmount || shrunkSizePx >= minAmountPx)

        if (fitsInline) {
            moveAmountTo(amount, inline, ViewGroup.LayoutParams.WRAP_CONTENT)
            below.visibility = View.GONE
            val amountWidth = amountNaturalWidth.coerceAtMost(availableForAmount)
            val nameWidth = (contentWidth - iconWidth - iconGap - amountGap - amountWidth)
                .coerceAtLeast(minNameWidth)
            fitName(category, nameWidth)
        } else {
            moveAmountTo(amount, below, ViewGroup.LayoutParams.MATCH_PARENT)
            inline.visibility = View.GONE
            below.visibility = View.VISIBLE
            fitName(category, (contentWidth - iconWidth - iconGap).coerceAtLeast(minNameWidth))
        }
        row.requestLayout()
        card.requestLayout()
    }

    /**
     * Measures the text the way the TextView would lay it out, rather than through
     * a paint heuristic, so the inline/wrapped decision matches what is rendered.
     */
    private fun measureNaturalWidth(amount: TextView): Int {
        val text = amount.text?.toString().orEmpty()
        if (text.isEmpty()) return 0
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val previousMax = amount.maxLines
        amount.maxLines = 1
        amount.measure(widthSpec, heightSpec)
        val measured = amount.measuredWidth
        amount.maxLines = previousMax
        return measured
    }

    /**
     * Lets the framework shrink the amount to fit the width it is given (never
     * enlarging it), so a long amount stays complete instead of being ellipsized.
     * The floor is [MIN_AMOUNT_SIZE_SP], clamped below the row's own base size so
     * the configuration stays valid for every font scale.
     */
    private fun configureAmountAutoSize(amount: TextView) {
        if (amountBaseSizePx <= 0f) return
        val density = amount.resources.displayMetrics.density
        val scaledDensity = amount.resources.displayMetrics.scaledDensity
        val desiredMaxSp = amountBaseSizePx / scaledDensity
        val minSizePx = (MIN_AMOUNT_SIZE_SP * scaledDensity).coerceAtMost(amountBaseSizePx * 0.5f)
        val minSizeSp = (minSizePx / density).coerceAtLeast(8f)
        val maxSizeSp = desiredMaxSp.coerceAtLeast(minSizeSp + 1f)
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            amount,
            minSizeSp.toInt().coerceAtLeast(8),
            maxSizeSp.toInt().coerceAtLeast(minSizeSp.toInt() + 1),
            1,
            TypedValue.COMPLEX_UNIT_SP
        )
    }

    /**
     * The name keeps its size: it is allowed [RECORD_NAME_MAX_LINES] lines and
     * ellipsizes only as the last resort, so it can never be shrunk to an
     * unreadable size just because the amount is long. The measured width is what
     * decides whether the amount still fits beside it.
     */
    private fun fitName(category: TextView, availableWidthPx: Int) {
        category.maxLines = RECORD_NAME_MAX_LINES
        category.ellipsize = android.text.TextUtils.TruncateAt.END
        if (nameBaseSizePx > 0f) {
            category.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameBaseSizePx)
        }
        // Nothing else to do: the layout gives the name this exact width.
    }

    private fun moveAmountTo(amount: TextView, container: View, widthMode: Int) {
        val currentParent = amount.parent as? ViewGroup
        if (currentParent !== container) {
            currentParent?.removeView(amount)
            (container as? ViewGroup)?.addView(amount)
        }
        val params = amount.layoutParams
        if (params != null && params.width != widthMode) {
            params.width = widthMode
            amount.layoutParams = params
        }
        container.visibility = View.VISIBLE
    }

    companion object {
        /** Auto-sizing floor: the amount never renders smaller than this. */
        const val MIN_AMOUNT_SIZE_SP = 14f
        const val RECORD_NAME_MAX_LINES = 2
    }
}
