package com.example.cardtally.util

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.example.cardtally.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

/** Shared opaque, content-sized category surfaces. No category writes or host callbacks here. */
internal object CategorySheetUi {
    fun dp(context: Context, value: Int) = (context.resources.displayMetrics.density * value).toInt()
    fun color(context: Context, attr: Int) = ThemeColorHelper.resolveColor(context, attr)
    fun text(context: Context, value: CharSequence, size: Float = 16f) = TextView(context).apply {
        text = value; textSize = size
        setTextColor(color(context, com.google.android.material.R.attr.colorOnSurface))
        setPadding(0, dp(context, 8), 0, dp(context, 8))
    }
    data class Content(val root: LinearLayout, val body: LinearLayout, val footer: LinearLayout)
    fun content(context: Context, title: String, close: () -> Unit): Content {
        fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val root = column().apply {
            setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 16))
            background = GradientDrawable().apply {
                setColor(color(context, com.google.android.material.R.attr.colorSurface))
                val radius = dp(context, 24).toFloat()
                cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
            }
        }
        root.addView(View(context).apply { setBackgroundResource(R.drawable.bg_bottom_sheet_handle) },
            LinearLayout.LayoutParams(dp(context, 36), dp(context, 4)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        root.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text(context, title, 22f).apply {
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                ViewCompat.setAccessibilityHeading(this, true)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(androidx.appcompat.widget.AppCompatImageButton(context).apply {
                setImageResource(R.drawable.ic_close)
                imageTintList = android.content.res.ColorStateList.valueOf(color(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
                val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackgroundBorderless))
                background = attrs.getDrawable(0); attrs.recycle()
                contentDescription = context.getString(R.string.dialog_cancel)
                setPadding(dp(context, 14), dp(context, 14), dp(context, 14), dp(context, 14))
                setOnClickListener { close() }
            }, LinearLayout.LayoutParams(dp(context, 48), dp(context, 48)))
        })
        val body = column()
        root.addView(NestedScrollView(context).apply { addView(body); isFillViewport = false },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val footer = column()
        root.addView(footer)
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight,
                bottom + insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
        return Content(root, body, footer)
    }
    fun button(context: Context, label: String, primary: Boolean = false, danger: Boolean = false,
        navigation: Boolean = false, click: () -> Unit) =
        MaterialButton(context).apply {
            text = label; isAllCaps = false; textSize = 16f; minHeight = dp(context, 48)
            cornerRadius = dp(context, 12)
            if (!primary) {
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                setTextColor(if (danger) androidx.core.content.ContextCompat.getColor(context, R.color.error_primary)
                    else color(context, com.google.android.material.R.attr.colorOnSurface))
            }
            if (primary && danger) {
                backgroundTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.error_light))
                setTextColor(color(context, com.google.android.material.R.attr.colorOnError))
            }
            if (navigation) {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setIconResource(R.drawable.ic_arrow_right)
                iconGravity = MaterialButton.ICON_GRAVITY_END
                iconTint = textColors
            }
            setOnClickListener { click() }
        }
    fun configure(dialog: BottomSheetDialog, root: View) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
            background = ColorDrawable(Color.TRANSPARENT)
            layoutParams = layoutParams.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
        }
        fun limitHeight() {
            val frame = Rect()
            root.getWindowVisibleDisplayFrame(frame)
            val available = frame.height().takeIf { it > 0 } ?: root.resources.displayMetrics.heightPixels
            val cap = (available * .75f).toInt()
            if (dialog.behavior.maxHeight != cap) { dialog.behavior.maxHeight = cap; root.requestLayout() }
        }
        limitHeight()
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener { limitHeight() }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (v.viewTreeObserver.isAlive) v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        })
        ViewCompat.requestApplyInsets(root)
    }
}
