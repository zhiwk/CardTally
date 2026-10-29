package com.example.cardtally.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import com.example.cardtally.R
import com.example.cardtally.util.ThemeColorHelper
import java.util.Locale
import kotlin.math.*

/** Centered, proportion-preserving donut with selectively labelled, single-elbow callouts. */
class LedgerDonutChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    data class Slice(val value: Float, val color: Int, val label: String = "")
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity
    private fun dp(value: Float) = value * density
    private val ink = ThemeColorHelper.resolveThemeAwareResource(context, R.color.onSurface_light)
    private val muted = ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_text_muted)
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = dp(1f) }
    private val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink; textSize = 14f * scaledDensity; isFakeBoldText = true
    }
    private val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted; textSize = 14f * scaledDensity
    }
    private val amountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
    private var slices = emptyList<Slice>()
    private var totalLabel = ""
    private var totalValue = ""
    private var dirty = true
    private var startAngle = -90f
    private var labels = emptyList<Callout>()
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private var stroke = 0f
    private var below: StaticLayout? = null
    private var centerTitle: StaticLayout? = null
    private var centerAmount: StaticLayout? = null
    private val bounds = RectF()
    private data class Callout(
        val index: Int, val right: Boolean, val anchor: PointF,
        val idealY: Float, val y: Float, val elbow: PointF, val end: PointF,
        val name: StaticLayout, val percent: StaticLayout
    ) {
        val height: Float get() = (name.height + percent.height).toFloat()
    }

    fun submitData(slices: List<Slice>, totalLabel: String, totalValue: String) {
        this.slices = slices.filter { it.value.isFinite() && it.value > 0f }
        this.totalLabel = totalLabel
        this.totalValue = totalValue
        // All slices, including unlabelled ones, remain available to screen readers.
        val total = this.slices.sumOf { it.value.toDouble() }
        contentDescription = buildString {
            append("$totalLabel $totalValue")
            this@LedgerDonutChartView.slices.forEach {
                append("; ${it.label} ${percentage(it.value.toDouble() / total)}")
            }
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        dirty = true
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        dirty = true
    }

    private fun textLayout(
        text: String, paint: TextPaint, width: Float,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
    ): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(alignment).setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()

    private fun percentage(fraction: Double) = String.format(Locale.getDefault(), "%.1f%%", fraction * 100)

    private fun prepare() {
        dirty = false
        cx = width / 2f
        radius = min(dp(63f), width * .21f)
        stroke = dp(19f).coerceAtMost(radius * .3f)
        val inner = (radius - stroke / 2f).coerceAtLeast(1f)
        val centerWidth = inner * 1.65f
        amountPaint.textSize = 16f * scaledDensity
        if (amountPaint.measureText(totalValue) > centerWidth) amountPaint.textSize = 14f * scaledDensity
        val title = textLayout(totalLabel, metaPaint, centerWidth)
        val amount = textLayout(totalValue, amountPaint, centerWidth)
        // Never ellipsize money or shrink it below 14sp: move long/enlarged text below the ring.
        val outside = amount.lineCount > 1 || title.height + amount.height + dp(6f) > inner * 1.4f
        below = if (outside) textLayout("$totalLabel\n$totalValue", amountPaint, width - dp(16f)) else null
        centerTitle = if (outside) null else title
        centerAmount = if (outside) null else amount
        val plotHeight = (height - (below?.height?.plus(dp(12f)) ?: 0f)).coerceAtLeast(1f)
        cy = plotHeight / 2f
        radius = min(radius, (plotHeight / 2f - dp(24f)).coerceAtLeast(dp(12f)))
        stroke = min(stroke, radius * .3f)
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
        startAngle = -90f
        labels = emptyList()
        val total = slices.sumOf { it.value.toDouble() }
        if (total <= 0) return
        val ranked = slices.indices.sortedByDescending { slices[it].value }
        // Prefer the two meaningful shares when the remainder is visually insignificant,
        // including a split such as 57.5% + 37.9%, not just a single dominant sector.
        val concentrated = slices.size > 2 && (slices[ranked.first()].value / total >= .85 ||
            ranked.take(2).sumOf { slices[it].value.toDouble() } / total >= .90)
        // Only cap callouts. All sectors remain in slices and are drawn proportionally.
        var selected = if (concentrated) ranked.take(2) else ranked.take(4)
        while (selected.isNotEmpty()) {
            var bestScore = Float.POSITIVE_INFINITY
            var best: List<Callout>? = null
            for (rotation in -180..178 step 2) {
                val angle = -90f + rotation
                val placed = place(angle, selected, total, plotHeight) ?: continue
                val sameSidePenalty = if (placed.size == 2 && placed[0].right == placed[1].right) dp(120f) else 0f
                val score = placed.sumOf {
                    (hypot(it.anchor.x - it.elbow.x, it.anchor.y - it.elbow.y) +
                        abs(it.y - it.idealY) * 1.3f + abs(it.y - cy) * .25f).toDouble()
                }.toFloat() + abs(rotation) * dp(.15f) + sameSidePenalty
                if (score < bestScore) { bestScore = score; best = placed; startAngle = angle }
            }
            if (best != null) { labels = best; break }
            selected = selected.dropLast(1)
        }
    }

    private fun place(angle: Float, selected: List<Int>, total: Double, plotHeight: Float): List<Callout>? {
        val outer = radius + stroke / 2f
        val textWidth = cx - outer - dp(28f)
        if (textWidth <= dp(20f)) return null
        val leafNames = slices.map { it.label.substringAfterLast("/").trim() }
        var start = angle.toDouble()
        val candidates = slices.mapIndexedNotNull { index, slice ->
            val sweep = slice.value / total * 360
            val middle = Math.toRadians(start + sweep / 2)
            start += sweep
            if (index !in selected) return@mapIndexedNotNull null
            val right = cos(middle) >= 0
            val anchor = PointF(cx + cos(middle).toFloat() * outer, cy + sin(middle).toFloat() * outer)
            val idealY = cy + sin(middle).toFloat() * (outer + dp(18f))
            val name = if (leafNames.count { it == leafNames[index] } > 1) slice.label else leafNames[index]
            val percent = percentage(slice.value / total)
            if (metaPaint.measureText(percent) > textWidth) return null
            val labelWidth = min(textWidth, ceil(max(namePaint.measureText(name), metaPaint.measureText(percent))))
            val alignment = if (right) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
            Callout(index, right, anchor, idealY, idealY, PointF(), PointF(),
                textLayout(name, namePaint, labelWidth, alignment),
                textLayout(percent, metaPaint, labelWidth, alignment))
        }
        val placed = mutableListOf<Callout>()
        for (right in listOf(false, true)) {
            val group = candidates.filter { it.right == right }.sortedBy { it.idealY }
            if (group.isEmpty()) continue
            val ys = group.map { it.idealY }.toMutableList()
            for (i in 1 until ys.size) ys[i] = max(ys[i], ys[i - 1] +
                (group[i - 1].height + group[i].height) / 2f + dp(12f))
            val low = ys.last() + group.last().height / 2f - (plotHeight - dp(4f))
            val high = ys.first() - group.first().height / 2f - dp(4f)
            if (low > high) return null
            val shift = group.indices.map { ys[it] - group[it].idealY }.average().toFloat().coerceIn(low, high)
            for ((i, label) in group.withIndex()) {
                val y = ys[i] - shift
                val sign = if (right) 1 else -1
                val elbow = PointF(cx + sign * (outer + dp(12f)), y)
                val end = PointF(elbow.x + sign * dp(8f), y)
                if (hypot(elbow.x - label.anchor.x, y - label.anchor.y) > outer * .95f ||
                    distanceToSegment(label.anchor, elbow) < outer - dp(.4f)) return null
                placed += label.copy(y = y, elbow = elbow, end = end)
            }
        }
        for (i in placed.indices) for (j in 0 until i) {
            val a = placed[i]; val b = placed[j]
            if (crosses(a.anchor, a.elbow, b.anchor, b.elbow) ||
                crosses(a.anchor, a.elbow, b.elbow, b.end) ||
                crosses(a.elbow, a.end, b.anchor, b.elbow)) return null
        }
        return placed
    }

    private fun distanceToSegment(a: PointF, b: PointF): Float {
        val dx = b.x - a.x; val dy = b.y - a.y
        val denominator = dx * dx + dy * dy
        val t = if (denominator == 0f) 0f else (((cx - a.x) * dx + (cy - a.y) * dy) / denominator).coerceIn(0f, 1f)
        return hypot(a.x + t * dx - cx, a.y + t * dy - cy)
    }

    private fun crosses(a: PointF, b: PointF, c: PointF, d: PointF): Boolean {
        fun cross(p: PointF, q: PointF, r: PointF) = (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x)
        return cross(a, b, c) * cross(a, b, d) < 0 && cross(c, d, a) * cross(c, d, b) < 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        if (dirty) prepare()
        arcPaint.strokeWidth = stroke
        arcPaint.color = ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_container_high)
        canvas.drawArc(bounds, 0f, 360f, false, arcPaint)
        val total = slices.sumOf { it.value.toDouble() }
        var angle = startAngle
        slices.forEach {
            val sweep = (it.value / total * 360).toFloat()
            arcPaint.color = it.color
            canvas.drawArc(bounds, angle, sweep, false, arcPaint)
            angle += sweep
        }
        fun drawText(layout: StaticLayout, x: Float, y: Float) {
            canvas.save(); canvas.translate(x, y); layout.draw(canvas); canvas.restore()
        }
        below?.let { drawText(it, (width - it.width) / 2f, height - it.height - dp(4f)) }
        centerTitle?.let { title -> centerAmount?.let { amount ->
            val top = cy - (title.height + amount.height + dp(6f)) / 2f
            drawText(title, cx - title.width / 2f, top)
            drawText(amount, cx - amount.width / 2f, top + title.height + dp(6f))
        } }
        labels.forEach {
            linePaint.color = ColorUtils.blendARGB(slices[it.index].color, ink, .35f)
            canvas.drawLine(it.anchor.x, it.anchor.y, it.elbow.x, it.elbow.y, linePaint)
            canvas.drawLine(it.elbow.x, it.elbow.y, it.end.x, it.end.y, linePaint)
            // labelLine alignment: text touches the guide-line end, not the card edge.
            val x = if (it.right) it.end.x + dp(4f) else it.end.x - dp(4f) - it.name.width
            val top = it.y - it.height / 2f
            drawText(it.name, x, top)
            drawText(it.percent, x, top + it.name.height)
        }
    }
}
