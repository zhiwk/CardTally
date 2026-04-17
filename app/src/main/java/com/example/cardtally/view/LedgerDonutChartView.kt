package com.example.cardtally.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.cardtally.R
import com.example.cardtally.util.ThemeColorHelper
import kotlin.math.min

class LedgerDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Slice(val value: Float, val color: Int)

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_text_muted)
        textSize = 28f
        isFakeBoldText = true
        letterSpacing = 0.08f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        textSize = 52f
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_container_high)
    }

    private val arcBounds = RectF()
    private var slices: List<Slice> = emptyList()
    private var totalLabel: String = "TOTAL"
    private var totalValue: String = "¥0"

    fun submitData(slices: List<Slice>, totalLabel: String, totalValue: String) {
        this.slices = slices
        this.totalLabel = totalLabel
        this.totalValue = totalValue
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val stroke = size * 0.11f
        val padding = stroke * 1.3f
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) / 2f) - padding
        arcBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        slicePaint.strokeWidth = stroke
        trackPaint.strokeWidth = stroke

        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint)

        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = if (total == 0f) 0f else (slice.value / total) * 360f
            slicePaint.color = slice.color
            canvas.drawArc(arcBounds, startAngle, sweep, false, slicePaint)
            startAngle += sweep
        }

        canvas.drawText(totalLabel, width / 2f, height / 2f - 12f, labelPaint)
        canvas.drawText(totalValue, width / 2f, height / 2f + 34f, valuePaint)
    }
}
