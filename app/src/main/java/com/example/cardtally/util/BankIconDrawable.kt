package com.example.cardtally.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Fits the visible bank mark, rather than its padded resource canvas, to the icon slot. */
internal class BankIconDrawable private constructor(
    private val source: Drawable,
    private val markBounds: RectF,
    private val sourceWidth: Int,
    private val sourceHeight: Int
) : Drawable() {
    companion object {
        private const val SAMPLE_SIZE = 256
        private data class Geometry(val bounds: RectF, val width: Int, val height: Int)
        // Light and night assets share their colored mark geometry.
        // Cache geometry and immutable transparent PNGs, never view-owned drawables.
        private val geometryByResource = mutableMapOf<Int, Geometry>()
        private val transparentPngByResource = mutableMapOf<Int, Bitmap>()

        fun create(context: Context, resource: Int): Drawable? {
            val original = AppCompatResources.getDrawable(context, resource)?.mutate() ?: return null
            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
            val source = if (night && original is BitmapDrawable) {
                val bitmap = synchronized(transparentPngByResource) {
                    transparentPngByResource.getOrPut(resource) { removeOuterWhiteCanvas(original.bitmap) }
                }
                BitmapDrawable(context.resources, bitmap)
            } else original
            val geometry = synchronized(geometryByResource) {
                geometryByResource.getOrPut(resource) { measure(source) }
            }
            return BankIconDrawable(source, geometry.bounds, geometry.width, geometry.height)
        }

        /** Remove only white padding connected to the PNG perimeter; retain enclosed white details. */
        private fun removeOuterWhiteCanvas(source: Bitmap): Bitmap {
            val width = source.width
            val height = source.height
            val pixels = IntArray(width * height)
            source.getPixels(pixels, 0, width, 0, 0, width, height)
            val visited = BooleanArray(pixels.size)
            val queue = IntArray(pixels.size)
            var head = 0
            var tail = 0
            fun enqueue(index: Int) {
                if (visited[index]) return
                visited[index] = true
                val color = pixels[index]
                if (Color.alpha(color) <= 8 ||
                    (Color.red(color) >= 245 && Color.green(color) >= 245 && Color.blue(color) >= 245)) {
                    queue[tail++] = index
                }
            }
            for (x in 0 until width) { enqueue(x); enqueue((height - 1) * width + x) }
            for (y in 0 until height) { enqueue(y * width); enqueue(y * width + width - 1) }
            while (head < tail) {
                val index = queue[head++]
                pixels[index] = Color.TRANSPARENT
                val x = index % width
                val y = index / width
                if (x > 0) enqueue(index - 1)
                if (x + 1 < width) enqueue(index + 1)
                if (y > 0) enqueue(index - width)
                if (y + 1 < height) enqueue(index + width)
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).apply {
                density = source.density
            }
        }

        private fun measure(source: Drawable): Geometry {
            val intrinsicWidth = source.intrinsicWidth.coerceAtLeast(1)
            val intrinsicHeight = source.intrinsicHeight.coerceAtLeast(1)
            val ratio = SAMPLE_SIZE.toFloat() / max(intrinsicWidth, intrinsicHeight)
            val width = (intrinsicWidth * ratio).roundToInt().coerceAtLeast(1)
            val height = (intrinsicHeight * ratio).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                source.setBounds(0, 0, width, height)
                source.draw(Canvas(bitmap))
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                var left = width
                var top = height
                var right = -1
                var bottom = -1
                pixels.forEachIndexed { index, color ->
                    // White canvases and transparent PNG margins are padding. White
                    // details inside the mark remain in the original drawable.
                    if (Color.alpha(color) > 8 &&
                        (Color.red(color) < 245 || Color.green(color) < 245 || Color.blue(color) < 245)
                    ) {
                        val x = index % width
                        val y = index / width
                        left = min(left, x)
                        top = min(top, y)
                        right = max(right, x)
                        bottom = max(bottom, y)
                    }
                }
                val mark = if (right >= left && bottom >= top) {
                    // Keep a sampling pixel around anti-aliased edges.
                    RectF(
                        (left - 1).coerceAtLeast(0).toFloat(),
                        (top - 1).coerceAtLeast(0).toFloat(),
                        (right + 2).coerceAtMost(width).toFloat(),
                        (bottom + 2).coerceAtMost(height).toFloat()
                    )
                } else {
                    RectF(0f, 0f, width.toFloat(), height.toFloat())
                }
                return Geometry(mark, width, height)
            } finally {
                bitmap.recycle()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val scale = min(bounds.width() / markBounds.width(), bounds.height() / markBounds.height())
        val left = bounds.exactCenterX() - markBounds.width() * scale / 2f
        val top = bounds.exactCenterY() - markBounds.height() * scale / 2f
        val save = canvas.save()
        try {
            // Clip the source's unused white canvas before centering its visible mark.
            canvas.clipRect(left, top, left + markBounds.width() * scale, top + markBounds.height() * scale)
            canvas.translate(left - markBounds.left * scale, top - markBounds.top * scale)
            canvas.scale(scale, scale)
            source.setBounds(0, 0, sourceWidth, sourceHeight)
            source.draw(canvas)
        } finally {
            canvas.restoreToCount(save)
        }
    }

    override fun getIntrinsicWidth(): Int = max(source.intrinsicWidth, source.intrinsicHeight).coerceAtLeast(1)
    override fun getIntrinsicHeight(): Int = getIntrinsicWidth()
    override fun setAlpha(alpha: Int) { source.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { source.colorFilter = colorFilter; invalidateSelf() }
    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
