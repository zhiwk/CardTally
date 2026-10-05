package com.example.cardtally.util

import android.content.Context
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors

object ThemeColorHelper {
    fun resolveCardSurface(context: Context): Int =
        resolveColor(context, com.example.cardtally.R.attr.cardSurfaceColor)

    fun resolveColor(context: Context, @AttrRes attr: Int): Int {
        return MaterialColors.getColor(context, attr, ThemeColorHelper::class.java.simpleName)
    }

    fun resolveThemeAwareResource(context: Context, resId: Int): Int {
        return ContextCompat.getColor(context, resId)
    }
}
