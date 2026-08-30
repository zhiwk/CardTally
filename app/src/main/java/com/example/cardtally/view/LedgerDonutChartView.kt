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
import kotlin.math.max

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
        val centerX = width / 2f
        val centerY = height / 2f
        // Keep the donut visually subordinate to its labels. The previous radius
        // used almost the whole chart height, leaving no safe area for callouts.
        val radius = min(width, height) * 0.23f
        val stroke = radius * 0.28f
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
            drawCalloutLabels(canvas, total, radius, centerX, centerY)
        }
    }

    private data class Callout(
        val slice: Slice,
        val startX: Float,
        val startY: Float,
        val desiredY: Float,
        val rightSide: Boolean
    )

    private fun drawCalloutLabels(
        canvas: Canvas,
        total: Float,
        radius: Float,
        centerX: Float,
        centerY: Float
    ) {
        val callouts = mutableListOf<Callout>()
        var angle = -90f
        slices.filter { it.value > 0f }.forEach { slice ->
            val sweep = (slice.value / total) * 360f
            val middleAngle = Math.toRadians((angle + sweep / 2f).toDouble())
            val cos = kotlin.math.cos(middleAngle).toFloat()
            val sin = kotlin.math.sin(middleAngle).toFloat()
            val startX = centerX + cos * radius
            val startY = centerY + sin * radius
            callouts += Callout(
                slice = slice,
                startX = startX,
                startY = startY,
                desiredY = centerY + sin * (radius + 14f),
                rightSide = cos >= 0f
            )
            angle += sweep
        }

        // Resolve labels independently on both sides. This keeps the leader lines
        // readable when two small categories have nearly identical angles.
        val fontMetrics = labelPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val minimumGap = textHeight + 6f
        callouts.groupBy { it.rightSide }.values.forEach { sideCallouts ->
            val placed = sideCallouts.sortedBy { it.desiredY }.map { callout ->
                val label = formatCalloutLabel(callout.slice, total)
                val textWidth = labelPaint.measureText(label)
                val top = textHeight / 2f
                val bottom = height - textHeight / 2f
                val y = callout.desiredY.coerceIn(top, bottom)
                CalloutPlacement(callout, label, textWidth, y)
            }.toMutableList()

            for (index in 1 until placed.size) {
                placed[index] = placed[index].copy(
                    centerY = max(placed[index].centerY, placed[index - 1].centerY + minimumGap)
                )
            }
            for (index in placed.lastIndex - 1 downTo 0) {
                placed[index] = placed[index].copy(
                    centerY = min(placed[index].centerY, placed[index + 1].centerY - minimumGap)
                )
            }
            val shift = when {
                placed.isEmpty() -> 0f
                placed.first().centerY < textHeight / 2f -> textHeight / 2f - placed.first().centerY
                placed.last().centerY > height - textHeight / 2f -> height - textHeight / 2f - placed.last().centerY
                else -> 0f
            }
            placed.forEach { placement ->
                val callout = placement.callout
                val y = placement.centerY + shift

                val elbowX = centerX + if (callout.rightSide) radius + 12f else -(radius + 12f)
                val textX = if (callout.rightSide) width - 6f else 6f
                val lineEndX = if (callout.rightSide) {
                    textX - placement.textWidth - 5f
                } else {
                    textX + placement.textWidth + 5f
                }
                leaderPaint.color = callout.slice.color
                canvas.drawLine(callout.startX, callout.startY, elbowX, y, leaderPaint)
                canvas.drawLine(elbowX, y, lineEndX, y, leaderPaint)

                labelPaint.textAlign = if (callout.rightSide) Paint.Align.RIGHT else Paint.Align.LEFT
                canvas.drawText(placement.label, textX, y - (fontMetrics.ascent + fontMetrics.descent) / 2f, labelPaint)
            }
        }
        labelPaint.textAlign = Paint.Align.LEFT
    }

    private data class CalloutPlacement(
        val callout: Callout,
        val label: String,
        val textWidth: Float,
        val centerY: Float
    )

    private fun formatCalloutLabel(slice: Slice, total: Float): String {
        val percentage = String.format(java.util.Locale.US, "%.1f%%", slice.value / total * 100f)
        return if (slice.label.isBlank()) percentage else "${slice.label} · $percentage"
    }
}
