package com.example.cardtally.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.provider.Settings
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.cardtally.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs
import kotlin.math.min

/** Native navigation semantics with a spring-driven selection capsule. */
class ElasticBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle
) : BottomNavigationView(context, attrs, defStyleAttr) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.navigation_dock_selected)
    }
    private val bounds = Rect()
    private val capsule = RectF()
    private val position = Spring()
    private val pressure = Spring()
    private var centerY = 0f
    private var capsuleWidth = 64f * density
    private var capsuleHeight = 56f * density
    private var initialized = false
    private var framePending = false
    private var lastFrameNanos = 0L
    private var pressedItem: View? = null
    private var animatedIcon: View? = null
    private var geometryChanged = true
    private var visibleItems = emptyList<Int>()

    private val frame = Choreographer.FrameCallback { time -> tick(time) }

    init {
        // Draw one travelling capsule instead of Material's per-item fading indicators.
        isItemActiveIndicatorEnabled = false
    }

    private fun animationScale(): Float = Settings.Global.getFloat(
        context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
    ).coerceAtLeast(0f)

    private fun iconOf(item: View): View? = item.findViewById(
        com.google.android.material.R.id.navigation_bar_item_icon_view
    )

    private fun itemBounds(item: View): Rect {
        item.getDrawingRect(bounds)
        offsetDescendantRectToMyCoords(item, bounds)
        return bounds
    }

    private fun itemAt(x: Float, y: Float): View? {
        for (index in 0 until menu.size()) {
            val entry = menu.getItem(index)
            if (!entry.isVisible || !entry.isEnabled) continue
            val item = findViewById<View>(entry.itemId) ?: continue
            if (itemBounds(item).contains(x.toInt(), y.toInt())) return item
        }
        return null
    }

    private fun syncTarget() {
        val visible = (0 until menu.size()).map { menu.getItem(it) }
            .filter { it.isVisible }.map { it.itemId }
        if (visible != visibleItems) {
            visibleItems = visible
            geometryChanged = true
        }
        val entry = menu.findItem(selectedItemId) ?: return
        if (!entry.isVisible) return
        val item = pressedItem ?: findViewById<View>(entry.itemId) ?: return
        val icon = iconOf(item) ?: return
        if (icon.width == 0 || item.width == 0) return
        itemBounds(item)
        val target = bounds.exactCenterX()
        centerY = bounds.exactCenterY()
        capsuleWidth = min(72f * density, (item.width - 4f * density).coerceAtLeast(32f * density))
        capsuleHeight = min(item.height.toFloat(), (height - paddingTop - paddingBottom).toFloat())
        if (!initialized || geometryChanged || animationScale() == 0f) {
            position.snap(target)
            initialized = true
            geometryChanged = false
        } else if (position.target != target) {
            // Retarget without discarding velocity when the user taps several items quickly.
            position.target = target
            scheduleFrame()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        syncTarget()
        if (initialized) {
            val stretch = (abs(position.velocity) / (900f * density)).coerceIn(0f, 1f) * .25f
            val lift = pressure.value.coerceIn(-.15f, 1.15f)
            val width = capsuleWidth * (1f + stretch + .08f * lift)
            val height = capsuleHeight * (1f - stretch / 2f + .08f * lift)
            capsule.set(position.value - width / 2, centerY - height / 2,
                position.value + width / 2, centerY + height / 2)
            canvas.drawRoundRect(capsule, height / 2, height / 2, paint)
        }
        super.dispatchDraw(canvas)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedItem = itemAt(event.x, event.y)
                animatedIcon?.let { it.scaleX = 1f; it.scaleY = 1f }
                animatedIcon = pressedItem?.let(::iconOf)
                if (animatedIcon != null && animationScale() > 0f) {
                    pressure.target = 1f
                    scheduleFrame()
                }
            }
            MotionEvent.ACTION_MOVE -> pressedItem?.let {
                if (!itemBounds(it).contains(event.x.toInt(), event.y.toInt())) releasePressure()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> releasePressure()
        }
        // Native clicks, cancellation, keyboard and accessibility activation remain authoritative.
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) invalidate()
        return handled
    }

    private fun releasePressure() {
        pressedItem = null
        pressure.target = 0f
        scheduleFrame()
    }

    private fun scheduleFrame() {
        if (framePending || !isAttachedToWindow || !isShown) return
        framePending = true
        Choreographer.getInstance().postFrameCallback(frame)
    }

    private fun tick(time: Long) {
        framePending = false
        if (!isShown || !isAttachedToWindow) {
            resetMotion()
            return
        }
        val scale = animationScale()
        if (scale == 0f) {
            position.snap(position.target)
            pressure.snap(0f)
        } else {
            val elapsed = if (lastFrameNanos == 0L) 1f / 60 else
                ((time - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, .032f)
            val dt = (elapsed / scale).coerceAtMost(.064f) / 8f
            repeat(8) {
                position.step(dt)
                pressure.step(dt)
            }
        }
        lastFrameNanos = time
        animatedIcon?.let {
            val lift = pressure.value.coerceIn(-.15f, 1.15f)
            it.scaleX = 1f + .10f * lift
            it.scaleY = 1f + .10f * lift
        }
        invalidate()
        if (!position.atRest(density * .1f) || !pressure.atRest(.001f)) {
            scheduleFrame()
        } else {
            position.snap(position.target)
            pressure.snap(pressure.target)
            if (pressure.target == 0f) {
                animatedIcon?.let { it.scaleX = 1f; it.scaleY = 1f }
                animatedIcon = null
            }
            lastFrameNanos = 0L
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        val dockWidth = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED)
            (368f * density).toInt() else available
        // Keep the five-destination width even when only three/four destinations are visible.
        minimumHeight = (60f * density + 16f * density *
            (resources.configuration.fontScale - 1f).coerceAtLeast(0f)).toInt()
        super.onMeasure(MeasureSpec.makeMeasureSpec(dockWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        geometryChanged = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Material anchors icon and labels to opposite edges. Center them as one compact stack.
        for (index in 0 until menu.size()) {
            val entry = menu.getItem(index)
            if (!entry.isVisible) continue
            val item = findViewById<View>(entry.itemId) ?: continue
            val iconContainer = item.findViewById<View>(
                com.google.android.material.R.id.navigation_bar_item_icon_container
            ) ?: continue
            val labels = item.findViewById<View>(
                com.google.android.material.R.id.navigation_bar_item_labels_group
            ) ?: continue
            val gap = 2f * density
            val stackTop = (item.height - iconContainer.height - gap - labels.height) / 2f
            iconContainer.translationY = stackTop - iconContainer.top
            labels.translationY = stackTop + iconContainer.height + gap - labels.top
        }
        // Resizing or changing the number of visible destinations should not animate across the bar.
        if (changed) geometryChanged = true
    }

    private fun resetMotion() {
        Choreographer.getInstance().removeFrameCallback(frame)
        framePending = false
        lastFrameNanos = 0L
        position.snap(position.target)
        pressure.snap(0f)
        pressedItem = null
        animatedIcon?.let { it.scaleX = 1f; it.scaleY = 1f }
        animatedIcon = null
        geometryChanged = true
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (!isVisible) resetMotion()
    }

    override fun onDetachedFromWindow() {
        resetMotion()
        super.onDetachedFromWindow()
    }

    private class Spring {
        var value = 0f
        var target = 0f
        var velocity = 0f

        fun step(dt: Float) {
            velocity += (360f * (target - value) - 23f * velocity) * dt
            value += velocity * dt
        }

        fun atRest(tolerance: Float) = abs(target - value) < tolerance && abs(velocity) < tolerance * 10f

        fun snap(to: Float) {
            value = to
            target = to
            velocity = 0f
        }
    }
}
