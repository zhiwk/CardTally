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

    data class Slice(val value: Float, val color: Int, val label: String = "")

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        color = ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_text_muted)
        textSize = 10f * resources.displayMetrics.density
        isFakeBoldText = true
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        textSize = 14f * resources.displayMetrics.density
    }
    private val leaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
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

        labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(totalLabel, width / 2f, height / 2f - 18f, labelPaint)
        canvas.drawText(totalValue, width / 2f, height / 2f + 22f, valuePaint)
        labelPaint.textAlign = Paint.Align.LEFT

        if (total > 0f) {
            drawTopLabels(canvas, total, radius, centerX, centerY, startAngle)
        }
    }

    private fun drawTopLabels(
        canvas: Canvas,
        total: Float,
        radius: Float,
        centerX: Float,
        centerY: Float,
        ignoredEndAngle: Float
    ) {
        var angle = -90f
        slices.take(3).forEach { slice ->
            val sweep = (slice.value / total) * 360f
            val middleAngle = Math.toRadians((angle + sweep / 2f).toDouble())
            val cos = kotlin.math.cos(middleAngle).toFloat()
            val sin = kotlin.math.sin(middleAngle).toFloat()
            val startX = centerX + cos * radius
            val startY = centerY + sin * radius
            val elbowX = centerX + cos * (radius + 8f)
            val elbowY = centerY + sin * (radius + 8f)
            val rightSide = when {
                kotlin.math.abs(cos) > 0.25f -> cos >= 0f
                sin >= 0f -> true
                else -> false
            }
            val textX = if (rightSide) width - 4f else 4f
            val lineEndX = if (rightSide) textX - 4f else textX + 4f
            leaderPaint.color = slice.color
            canvas.drawLine(startX, startY, elbowX, elbowY, leaderPaint)
            canvas.drawLine(elbowX, elbowY, lineEndX, elbowY, leaderPaint)
            labelPaint.textAlign = if (rightSide) Paint.Align.RIGHT else Paint.Align.LEFT
            val percentage = (slice.value / total * 100f).let { String.format(java.util.Locale.US, "%.1f%%", it) }
            val label = if (slice.label.isBlank()) percentage else "${slice.label} $percentage"
            canvas.drawText(label, textX, elbowY + labelPaint.textSize / 3f, labelPaint)
            angle += sweep
        }
    }
}
