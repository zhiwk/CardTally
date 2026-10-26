package com.example.cardtally.util

import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.text.TextUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import android.widget.TextView
import com.example.cardtally.R

/** Profile titles/descriptions stay left; current option values are a separate trailing column. */
internal object SettingsRowLayout {
    private val valueIds = mapOf(
        R.id.card_theme to R.id.text_current_theme,
        R.id.card_bottom_navigation to R.id.text_bottom_navigation_style,
        R.id.card_language to R.id.text_current_language,
        R.id.card_income_expense_color to R.id.text_income_expense_color,
        R.id.card_week_start to R.id.text_week_start,
        R.id.card_record_entry_mode to R.id.text_record_entry_mode,
        R.id.card_default_expense_asset to R.id.text_default_expense_asset,
        R.id.card_default_income_asset to R.id.text_default_income_asset,
        R.id.card_ai_api_key to R.id.text_ai_api_key_status,
        R.id.card_record_photo_limit to R.id.text_record_photo_limit
    )

    internal fun valueWidthLimit(availableWidth: Int): Int = (availableWidth.coerceAtLeast(0).toLong() * 42 / 100).toInt()

    fun bindChoices(root: View, vararg rowIds: Int) {
        rowIds.forEach { id ->
            val row = root.findViewById<LinearLayout>(id)
            val density = row.resources.displayMetrics.density
            fun dp(value: Int) = (value * density).toInt()
            (row.parent as View).setPadding(0, dp(8), 0, dp(8))
            row.layoutParams = row.layoutParams.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
            row.minimumHeight = dp(56)
            row.setPaddingRelative(dp(16), dp(8), dp(16), dp(8))
            val attrs = row.context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            try { row.background = attrs.getDrawable(0) } finally { attrs.recycle() }
            for (index in 0 until row.childCount) {
                when (val child = row.getChildAt(index)) {
                    is RadioButton -> {
                        child.isSaveEnabled = false
                        child.buttonTintList = ContextCompat.getColorStateList(row.context, R.color.appearance_radio_tint)
                        child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                    is LinearLayout -> {
                        for (textIndex in 0 until child.childCount) {
                            (child.getChildAt(textIndex) as? TextView)?.apply {
                                textSize = if (textIndex == 0) 16f else 14f
                                if (textIndex > 0) (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dp(4)
                            }
                        }
                    }
                }
            }
            // Expose one actionable, checkable row rather than a separately focusable circle.
            androidx.core.view.ViewCompat.setAccessibilityDelegate(row,
                object : androidx.core.view.AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(host: View,
                        info: androidx.core.view.accessibility.AccessibilityNodeInfoCompat) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = RadioButton::class.java.name
                        info.isCheckable = true
                        info.isChecked = (0 until row.childCount).map { row.getChildAt(it) }
                            .filterIsInstance<RadioButton>().any { it.isChecked }
                    }
                })
        }
    }

    fun bind(root: View) {
        val rows = listOf(R.id.card_theme, R.id.card_bottom_navigation, R.id.card_language,
            R.id.card_income_expense_color, R.id.card_week_start, R.id.card_record_entry_mode, R.id.card_default_expense_asset,
            R.id.card_default_income_asset, R.id.card_ai_api_key, R.id.card_category,
            R.id.card_ledger_management, R.id.card_recurring_records, R.id.card_export_data,
            R.id.card_import_data, R.id.card_record_photo_limit, R.id.card_quick_add, R.id.card_show_asset)
        rows.forEach { id -> styleRow(root.findViewById(id)) }
        listOf(R.id.switch_ai_assistant, R.id.switch_scroll_top_fab, R.id.switch_launch_analytics).forEach { id ->
            styleRow(root.findViewById<View>(id).parent as LinearLayout)
        }
    }

    private fun styleRow(row: LinearLayout) {
        val density = row.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        row.layoutParams = row.layoutParams.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
        row.minimumHeight = dp(56)
        row.setPaddingRelative(dp(16), dp(12), dp(16), dp(12))
        val children = (0 until row.childCount).map { row.getChildAt(it) }
        val value = valueIds[row.id]?.let { row.findViewById<TextView>(it) }
        val textChildren = children.filterIsInstance<TextView>().filterNot { it is Switch || it === value }
        val column = children.filterIsInstance<LinearLayout>().firstOrNull()
            ?: LinearLayout(row.context).apply {
                orientation = LinearLayout.VERTICAL
                textChildren.forEach { row.removeView(it) }
                row.addView(this, 0, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                textChildren.forEach { addView(it, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)) }
            }
        // Actual descriptions remain under the title; never mistake an option result for a hint.
        textChildren.filter { it.parent === row }.forEach {
            row.removeView(it)
            column.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        for (index in 0 until column.childCount) {
            (column.getChildAt(index) as? TextView)?.apply {
                textSize = if (index == 0) 16f else 14f
                setSingleLine(false)
                maxLines = Int.MAX_VALUE
                ellipsize = null
                (layoutParams as? LinearLayout.LayoutParams)?.topMargin = if (index == 0) 0 else dp(4)
            }
        }
        children.filterIsInstance<ImageView>().forEach {
            it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            (it.layoutParams as? LinearLayout.LayoutParams)?.marginStart = dp(12)
        }
        if (value != null) {
            (value.parent as? ViewGroup)?.removeView(value)
            val arrowIndex = (0 until row.childCount).firstOrNull { row.getChildAt(it) is ImageView } ?: row.childCount
            row.addView(value, arrowIndex, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(12) })
            value.textSize = 14f
            value.setTextColor(ThemeColorHelper.resolveColor(row.context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            value.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            value.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            value.setSingleLine(true)
            value.ellipsize = TextUtils.TruncateAt.END
            // TextView retains the full text for accessibility and for the existing selector.
            value.maxWidth = valueWidthLimit(row.resources.displayMetrics.widthPixels - row.paddingStart - row.paddingEnd)
            fun updateWidth() {
                val index = row.indexOfChild(value)
                val trailingWidth = (index + 1 until row.childCount).sumOf { childIndex ->
                    val child = row.getChildAt(childIndex)
                    val params = child.layoutParams as? ViewGroup.MarginLayoutParams
                    if (child.visibility == View.GONE) 0 else child.measuredWidth + (params?.marginStart ?: 0) + (params?.marginEnd ?: 0)
                }
                val width = valueWidthLimit(row.width - row.paddingStart - row.paddingEnd - trailingWidth - dp(12))
                if (row.width > 0 && value.maxWidth != width) value.maxWidth = width
            }
            row.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateWidth() }
            updateWidth()
        }
        val toggle = children.filterIsInstance<Switch>().firstOrNull()
        if (toggle == null) {
            val attrs = row.context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            try { row.background = attrs.getDrawable(0) } finally { attrs.recycle() }
            row.isFocusable = true
        } else {
            // Keep existing toggle handlers (including consent) as the only action path.
            row.setOnClickListener { if (toggle.isEnabled) toggle.performClick() }
            val attrs = row.context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            try { row.background = attrs.getDrawable(0) } finally { attrs.recycle() }
            toggle.minWidth = dp(48)
            toggle.minHeight = dp(48)
            (toggle.layoutParams as? LinearLayout.LayoutParams)?.marginStart = dp(12)
        }
    }
}
