package com.example.cardtally.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.cardtally.R
import com.example.cardtally.util.ThemeColorHelper

/** An independent AI destination, sharing the dock palette rather than page-card opacity. */
class AiNavigationButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    init {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density + .5f).toInt()
        orientation = VERTICAL
        gravity = Gravity.CENTER
        minimumWidth = dp(64)
        minimumHeight = dp(64)
        setPadding(dp(8), dp(8), dp(8), dp(8))
        isClickable = true
        isFocusable = true
        isSaveEnabled = false
        contentDescription = context.getString(R.string.nav_agent)
        elevation = dp(6).toFloat()

        fun circle(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(1), ContextCompat.getColor(context, R.color.navigation_dock_outline))
        }
        val surfaces = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_selected),
                circle(ContextCompat.getColor(context, R.color.navigation_dock_selected)))
            addState(intArrayOf(), circle(ThemeColorHelper.resolveCardSurface(context) or 0xFF000000.toInt()))
        }
        background = RippleDrawable(
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high)),
            surfaces, GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(android.graphics.Color.WHITE)
            }
        )
        val foregrounds = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(ContextCompat.getColor(context, R.color.onSurface_light),
                ContextCompat.getColor(context, R.color.onSurfaceVariant_light))
        )
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_agent)
            imageTintList = foregrounds
            isDuplicateParentStateEnabled = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(dp(22), dp(22)))
        addView(TextView(context).apply {
            setText(R.string.nav_agent)
            textSize = 12f
            setTextColor(foregrounds)
            gravity = Gravity.CENTER
            includeFontPadding = false
            isDuplicateParentStateEnabled = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Grow with the label/font scale, keeping an actual circle instead of an oval.
        val side = maxOf(measuredWidth, measuredHeight)
        super.onMeasure(MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(side, MeasureSpec.EXACTLY))
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = RadioButton::class.java.name
        info.isCheckable = true
        info.isChecked = isSelected
    }
}
