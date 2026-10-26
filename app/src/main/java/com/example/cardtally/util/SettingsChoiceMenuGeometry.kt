package com.example.cardtally.util

/** Pixel geometry only, independent of ListView selection and Android popup measurement. */
internal object SettingsChoiceMenuGeometry {
    data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = (right - left).coerceAtLeast(0)
        val height get() = (bottom - top).coerceAtLeast(0)
    }
    data class Placement(val left: Int, val top: Int, val width: Int, val height: Int, val scrollable: Boolean)

    fun contentHeight(rows: List<Int>, topPadding: Int, bottomPadding: Int): Int =
        (rows.sumOf { it.coerceAtLeast(0).toLong() } + topPadding.coerceAtLeast(0) + bottomPadding.coerceAtLeast(0))
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun place(safe: Bounds, anchor: Bounds, desiredWidth: Int, desiredHeight: Int,
        minimumVisibleHeight: Int, rtl: Boolean): Placement? {
        if (safe.width == 0 || safe.height == 0 || desiredHeight <= 0) return null
        val width = desiredWidth.coerceIn(1, safe.width)
        val below = (safe.bottom - anchor.bottom).coerceIn(0, safe.height)
        val above = (anchor.top - safe.top).coerceIn(0, safe.height)
        val minimum = minOf(minimumVisibleHeight, desiredHeight)
        val fullWindow = maxOf(above, below) < minimum
        // Prefer a full menu below; otherwise use the larger side, keeping ties below.
        val openAbove = !fullWindow && desiredHeight > below && above > below
        val available = if (fullWindow) safe.height else if (openAbove) above else below
        val height = minOf(desiredHeight, available)
        val left = (if (rtl) anchor.left else anchor.right - width).coerceIn(safe.left, safe.right - width)
        val top = when {
            fullWindow -> safe.top
            openAbove -> anchor.top - height
            else -> anchor.bottom
        }.coerceIn(safe.top, safe.bottom - height)
        return Placement(left, top, width, height, desiredHeight > height)
    }
}
