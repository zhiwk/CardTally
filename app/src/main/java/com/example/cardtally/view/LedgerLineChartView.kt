package com.example.cardtally.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.cardtally.R
import com.example.cardtally.util.ThemeColorHelper
import kotlin.math.max
import kotlin.math.roundToInt

class LedgerLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Point(val value: Float, val label: String = "")

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_outline)
        alpha = 90
        strokeWidth = 2f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        alpha = 30
        style = Paint.Style.FILL
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        style = Paint.Style.FILL
    }
    private val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_container_high)
        style = Paint.Style.FILL
    }
    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_text_muted)
        textSize = 11f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }
    private val tooltipLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_text_muted)
        strokeWidth = 1f * resources.displayMetrics.density
    }

    private var points: List<Point> = emptyList()
    private var pressedPointIndex = -1

    fun submitData(points: List<Point>) {
        this.points = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingStart = 24f
        val paddingEnd = width - 24f
        val paddingTop = 24f
        val paddingBottom = height - 24f
        val chartHeight = paddingBottom - paddingTop
        val chartWidth = paddingEnd - paddingStart

        repeat(4) { index ->
            val y = paddingTop + (chartHeight / 3f) * index
            canvas.drawLine(paddingStart, y, paddingEnd, y, gridPaint)
        }

        if (points.isEmpty()) {
            // 空周期仍然保留“零值趋势”的语义：显示一条贴近底部的水平线，
            // 而不是让图表区域看起来像加载失败或没有渲染。
            canvas.drawLine(paddingStart, paddingBottom, paddingEnd, paddingBottom, linePaint)
            return
        }

        val maxValue = max(points.maxOf { it.value }, 1f)
        val stepX = if (points.size == 1) 0f else chartWidth / (points.size - 1)
        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, point ->
            val x = paddingStart + stepX * index
            val normalized = point.value / maxValue
            val y = paddingBottom - (chartHeight * normalized)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, paddingBottom)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(paddingStart + stepX * (points.size - 1), paddingBottom)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        points.forEachIndexed { index, point ->
            val x = paddingStart + stepX * index
            val normalized = point.value / maxValue
            val y = paddingBottom - (chartHeight * normalized)
            canvas.drawCircle(x, y, 6f, pointPaint)
        }

        drawTooltip(canvas, paddingStart, chartWidth, paddingTop, paddingBottom)
    }

    private fun drawTooltip(
        canvas: Canvas,
        paddingStart: Float,
        chartWidth: Float,
        paddingTop: Float,
        paddingBottom: Float
    ) {
        val index = pressedPointIndex
        if (index !in points.indices || points.isEmpty()) return
        val maxValue = max(points.maxOf { it.value }, 1f)
        val stepX = if (points.size == 1) 0f else chartWidth / (points.size - 1)
        val point = points[index]
        val pointX = paddingStart + stepX * index
        val pointY = paddingBottom - ((paddingBottom - paddingTop) * point.value / maxValue)
        val amount = String.format(java.util.Locale.US, "¥%.2f", point.value)
        val text = if (point.label.isBlank()) amount else "${point.label}  $amount"
        val horizontalPadding = 10f * resources.displayMetrics.density
        val bubbleHeight = 28f * resources.displayMetrics.density
        val bubbleWidth = tooltipTextPaint.measureText(text) + horizontalPadding * 2f
        val bubbleLeft = (pointX - bubbleWidth / 2f).coerceIn(4f, width - bubbleWidth - 4f)
        val bubbleTop = (pointY - bubbleHeight - 10f * resources.displayMetrics.density)
            .coerceAtLeast(4f)
        val bubble = RectF(bubbleLeft, bubbleTop, bubbleLeft + bubbleWidth, bubbleTop + bubbleHeight)
        canvas.drawLine(pointX, pointY, pointX, bubble.bottom, tooltipLinePaint)
        canvas.drawRoundRect(bubble, 6f * resources.displayMetrics.density, 6f * resources.displayMetrics.density, tooltipPaint)
        val baseline = bubble.centerY() - (tooltipTextPaint.ascent() + tooltipTextPaint.descent()) / 2f
        canvas.drawText(text, bubble.centerX(), baseline, tooltipTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (points.isEmpty()) return false
        val paddingStart = 24f
        val chartWidth = width - 48f
        val stepX = if (points.size == 1) 0f else chartWidth / (points.size - 1)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                pressedPointIndex = if (stepX == 0f) {
                    0
                } else {
                    ((event.x - paddingStart) / stepX).roundToInt().coerceIn(0, points.lastIndex)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pressedPointIndex = -1
                invalidate()
                return true
            }
        }
        return true
    }
}
