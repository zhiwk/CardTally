package com.example.cardtally.util

import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.PopupWindow
import androidx.core.view.ViewCompat
import com.example.cardtally.R
import com.example.cardtally.util.SettingsChoiceMenuGeometry.Bounds

/** Measure the final wrapping rows AND list padding before showing the popup. */
internal object SettingsChoiceMenu {
    fun show(anchor: View, labels: List<String>, selected: Int, preferredWidthDp: Int,
        onDismiss: () -> Unit, onSelected: (Int) -> Unit): PopupWindow? {
        if (!anchor.isAttachedToWindow || labels.isEmpty()) return null
        val context = anchor.context
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        fun shape() = GradientDrawable().apply {
            setColor(ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_light))
            cornerRadius = dp(20).toFloat()
        }
        val root = FrameLayout(context).apply { background = shape(); clipToOutline = true }
        val list = ListView(context).apply {
            divider = null; dividerHeight = 0
            setPadding(0, dp(8), 0, dp(8))
            clipToPadding = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        root.addView(list, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val inflater = LayoutInflater.from(context)
        val adapter = object : BaseAdapter() {
            override fun getCount() = labels.size
            override fun getItem(position: Int) = labels[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                ((convertView as? CheckedTextView) ?: inflater.inflate(R.layout.item_record_photo_count, parent, false) as CheckedTextView).apply {
                    text = labels[position]
                    isChecked = position == selected
                }
        }
        list.adapter = adapter
        val popup = PopupWindow(root, 1, 1, true).apply {
            setBackgroundDrawable(shape())
            elevation = dp(6).toFloat()
            isOutsideTouchable = true
            isClippingEnabled = true
            inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
        }
        val screen = IntArray(2)
        val window = IntArray(2)
        val decor = anchor.rootView
        fun geometry(): SettingsChoiceMenuGeometry.Placement? {
            if (!anchor.isShown) return null
            val frame = Rect()
            decor.getWindowVisibleDisplayFrame(frame)
            val decorLocation = IntArray(2)
            decor.getLocationOnScreen(decorLocation)
            if (!frame.intersect(decorLocation[0], decorLocation[1], decorLocation[0] + decor.width, decorLocation[1] + decor.height)) return null
            frame.inset(dp(8), dp(8))
            if (frame.width() <= 0 || frame.height() <= 0) return null
            val visible = Rect()
            if (!anchor.getGlobalVisibleRect(visible) || !Rect.intersects(frame, visible)) return null
            anchor.getLocationOnScreen(screen)
            anchor.getLocationInWindow(window)
            val safe = Bounds(frame.left, frame.top, frame.right, frame.bottom)
            // AbsListView includes selector insets in its list padding; measure those too.
            list.measure(View.MeasureSpec.makeMeasureSpec(safe.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            val horizontalPadding = list.listPaddingLeft + list.listPaddingRight
            var widest = dp(preferredWidthDp)
            for (index in labels.indices) {
                val row = adapter.getView(index, null, list)
                row.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                widest = maxOf(widest, row.measuredWidth + horizontalPadding)
            }
            val width = minOf(widest, safe.width)
            val heights = labels.indices.map { index ->
                adapter.getView(index, null, list).apply {
                    measure(View.MeasureSpec.makeMeasureSpec((width - horizontalPadding).coerceAtLeast(1), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                }.measuredHeight
            }
            val height = SettingsChoiceMenuGeometry.contentHeight(heights, list.listPaddingTop, list.listPaddingBottom)
            return SettingsChoiceMenuGeometry.place(safe,
                Bounds(screen[0], screen[1], screen[0] + anchor.width, screen[1] + anchor.height),
                width, height, (heights.minOrNull() ?: 0) + list.listPaddingTop + list.listPaddingBottom,
                ViewCompat.getLayoutDirection(anchor) == ViewCompat.LAYOUT_DIRECTION_RTL)
        }
        var placement = geometry() ?: return null
        fun x() = placement.left - (screen[0] - window[0])
        fun y() = placement.top - (screen[1] - window[1])
        popup.width = placement.width; popup.height = placement.height
        list.setOnItemClickListener { _, _, position, _ -> onSelected(position); popup.dismiss() }
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            val next = geometry()
            if (next == null) popup.dismiss()
            else if (next != placement) {
                val previous = placement
                placement = next
                popup.update(x(), y(), placement.width, placement.height)
                if (!next.scrollable) list.setSelectionFromTop(0, 0)
                else if ((!previous.scrollable || next.height < previous.height) && selected in labels.indices) list.setSelection(selected)
            }
        }
        val detached = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) { popup.dismiss() }
        }
        popup.setOnDismissListener {
            if (decor.viewTreeObserver.isAlive) decor.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            anchor.removeOnAttachStateChangeListener(detached)
            onDismiss()
        }
        // Explicit window coordinates avoid scrolling the settings page merely to fit the popup.
        popup.showAtLocation(anchor, Gravity.TOP or Gravity.LEFT, x(), y())
        decor.viewTreeObserver.addOnGlobalLayoutListener(listener)
        anchor.addOnAttachStateChangeListener(detached)
        list.post {
            if (!popup.isShowing) return@post
            if (placement.scrollable && selected in labels.indices) list.setSelection(selected)
            else list.setSelectionFromTop(0, 0)
        }
        return popup
    }
}
