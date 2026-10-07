package com.example.cardtally.view

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import android.util.AttributeSet
import android.view.WindowInsets
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

/** Center-crop against the window without the IME, while content can still resize. */
class StableWallpaperImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    private val cropMatrix = Matrix()
    private var referenceWidth = 0
    private var referenceHeight = 0
    private var lastGeometry: List<Int>? = null
    private val activity: Activity? by lazy {
        var current = context
        while (current is ContextWrapper && current !is Activity) {
            val base = current.baseContext
            if (base === current) break
            current = base
        }
        current as? Activity
    }

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onDraw(canvas: Canvas) {
        val image = drawable
        if (width > 0 && height > 0 && image != null && image.intrinsicWidth > 0 && image.intrinsicHeight > 0) {
            val viewportHeight = stableViewportHeight()
            val geometry = listOf(width, viewportHeight, image.intrinsicWidth, image.intrinsicHeight)
            if (geometry != lastGeometry) {
                val scale = max(width.toFloat() / image.intrinsicWidth, viewportHeight.toFloat() / image.intrinsicHeight)
                cropMatrix.setScale(scale, scale)
                cropMatrix.postTranslate(
                    (width - image.intrinsicWidth * scale) / 2f,
                    (viewportHeight - image.intrinsicHeight * scale) / 2f
                )
                imageMatrix = cropMatrix
                lastGeometry = geometry
            }
        }
        super.onDraw(canvas)
    }

    private fun stableViewportHeight(): Int {
        // Window metrics exclude neither the keyboard nor system bars. Only remove the
        // stable system decorations: this Activity uses a non-edge-to-edge content area.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.windowManager?.currentWindowMetrics?.let { metrics ->
                val bars = metrics.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                return (metrics.bounds.height() - bars.top - bars.bottom).coerceAtLeast(height)
            }
        }
        // Android 7–10: reconstruct the unresized area from root IME insets.
        // Keep that reference while the IME animates to avoid transient recropping.
        val insets = ViewCompat.getRootWindowInsets(this)
        val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        val navigation = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        val keyboardVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
        val candidate = height + (ime - navigation).coerceAtLeast(0)
        if (referenceWidth != width || !keyboardVisible) {
            referenceWidth = width
            referenceHeight = candidate
        } else {
            referenceHeight = max(referenceHeight, candidate)
        }
        return referenceHeight.coerceAtLeast(height)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        referenceWidth = 0
        referenceHeight = 0
        lastGeometry = null
    }
}
