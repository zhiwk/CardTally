package com.example.cardtally.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.cardtally.R
import kotlin.math.hypot

/**
 * Draws the curved flow between the transfer source and destination cards.
 *
 * The single curve leaves the source card, passes behind the centered swap
 * button, and reaches the destination card. Its opacity grows from the
 * source (translucent) toward the destination (more solid), and the hue
 * switches from income green to expense red under the swap button.
 */
class TransferFlowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeWidth = 4f * density
    }

    private val flowPath = Path()
    private val strokePath = Path()
    private val pathMeasure = PathMeasure()
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var sourceAnchorView: View? = null
    private var destinationAnchorView: View? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resolveAnchors()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            resolveAnchors()
            invalidate()
        }
    }

    private fun resolveAnchors() {
        val host = parent as? ViewGroup ?: return
        if (sourceAnchorView == null) {
            sourceAnchorView = host.findViewById(R.id.row_asset)
        }
        if (destinationAnchorView == null) {
            destinationAnchorView = host.findViewById(R.id.row_destination_asset)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val centerX = width / 2f
        val centerY = height / 2f
        val sourceY = sourceAnchorY()
        val destinationY = destinationAnchorY()

        flowPath.reset()
        flowPath.moveTo(0f, sourceY)
        flowPath.cubicTo(
            width * 0.42f, sourceY,
            centerX - width * 0.28f, centerY,
            centerX, centerY
        )
        flowPath.cubicTo(
            centerX + width * 0.30f, centerY,
            width * 0.62f, destinationY - destinationApproach(centerY, destinationY),
            width.toFloat(), destinationY
        )

        val income = ContextCompat.getColor(context, R.color.income_primary)
        val expense = ContextCompat.getColor(context, R.color.expense_primary)
        val blendAlpha = (255 * 0.63f).toInt()
        val destinationAlpha = (255 * 0.71f).toInt()

        linePaint.shader = LinearGradient(
            0f, sourceY,
            width.toFloat(), destinationY,
            intArrayOf(
                ColorUtils.setAlphaComponent(income, 0),
                ColorUtils.setAlphaComponent(income, blendAlpha),
                ColorUtils.setAlphaComponent(expense, blendAlpha),
                ColorUtils.setAlphaComponent(expense, destinationAlpha)
            ),
            floatArrayOf(0f, 0.5f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        pathMeasure.setPath(flowPath, false)
        val length = pathMeasure.length
        val arrowLength = 11f * density

        // Stop the line at the arrow base so the stroke never runs through the head.
        strokePath.reset()
        if (length > arrowLength) {
            pathMeasure.getSegment(0f, length - arrowLength, strokePath, true)
            canvas.drawPath(strokePath, linePaint)
        } else {
            canvas.drawPath(flowPath, linePaint)
        }
        drawDestinationArrow(canvas, expense, destinationAlpha, length, arrowLength)
    }

    /** Draws a filled arrow head aligned with the curve's destination direction. */
    private fun drawDestinationArrow(
        canvas: Canvas,
        color: Int,
        alpha: Int,
        length: Float,
        arrowLength: Float
    ) {
        if (length <= 0f) return

        // Anchor the head on the exact point where the stroke stops and align its
        // base with the curve's tangent there, so line and head join seamlessly.
        val base = FloatArray(2)
        val tangent = FloatArray(2)
        pathMeasure.getPosTan((length - arrowLength).coerceAtLeast(0f), base, tangent)

        val magnitude = hypot(tangent[0], tangent[1])
        if (magnitude <= 0f) return
        val dirX = tangent[0] / magnitude
        val dirY = tangent[1] / magnitude

        val tipX = base[0] + dirX * arrowLength
        val tipY = base[1] + dirY * arrowLength
        val halfWidth = 6f * density
        val perpX = -dirY
        val perpY = dirX

        val head = Path().apply {
            moveTo(tipX, tipY)
            lineTo(base[0] + perpX * halfWidth, base[1] + perpY * halfWidth)
            lineTo(base[0] - perpX * halfWidth, base[1] - perpY * halfWidth)
            close()
        }
        arrowPaint.color = ColorUtils.setAlphaComponent(color, alpha)
        canvas.drawPath(head, arrowPaint)
    }

    /** Lifts the final control point so the curve ends pointing toward the card. */
    private fun destinationApproach(centerY: Float, destinationY: Float): Float {
        val distance = kotlin.math.abs(destinationY - centerY)
        if (distance <= 1f) return 0f
        val lift = minOf(18f * density, distance * 0.3f)
        return if (destinationY > centerY) lift else -lift
    }

    /** Source anchor: vertical center of the source card. */
    private fun sourceAnchorY(): Float =
        anchorY(sourceAnchorView, fraction = 0.5f, fallbackFraction = 0.3f)

    /** Destination anchor: vertical center of the destination card. */
    private fun destinationAnchorY(): Float =
        anchorY(destinationAnchorView, fraction = 0.5f, fallbackFraction = 0.7f)

    private fun anchorY(anchor: View?, fraction: Float, fallbackFraction: Float): Float {
        val view = anchor ?: return height * fallbackFraction
        if (view.height <= 0) return height * fallbackFraction
        val anchorPoint = view.top + view.height * fraction
        return (anchorPoint - top).coerceIn(0f, height.toFloat())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val measuredWidth = if (widthMode == MeasureSpec.EXACTLY) widthSize else 96.dp()

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val measuredHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(120.dp(), heightSize)
            else -> 120.dp()
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private fun Int.dp(): Int = (this * density).toInt()
}
