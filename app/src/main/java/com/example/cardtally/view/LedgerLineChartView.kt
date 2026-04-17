package com.example.cardtally.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.example.cardtally.R
import com.example.cardtally.util.ThemeColorHelper
import kotlin.math.max

class LedgerLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Point(val value: Float)

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

    private var points: List<Point> = emptyList()

    fun submitData(points: List<Point>) {
        this.points = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

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
    }
}
