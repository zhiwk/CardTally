package com.example.cardtally.util

import android.content.Context

object CategoryHierarchySettingsHelper {
    private const val PREFS_NAME = "category_hierarchy_settings_prefs"
    private const val KEY_CATEGORY_MAX_DEPTH = "category_hierarchy_max_depth"
    private const val DEFAULT_CATEGORY_MAX_DEPTH = 2
    private const val MIN_CATEGORY_MAX_DEPTH = 1
    private const val MAX_CATEGORY_MAX_DEPTH = 50

    fun saveCategoryMaxDepth(context: Context, depth: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_CATEGORY_MAX_DEPTH, sanitizeCategoryMaxDepth(depth)).apply()
    }

    fun getCategoryMaxDepth(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sanitizeCategoryMaxDepth(
            prefs.getInt(KEY_CATEGORY_MAX_DEPTH, DEFAULT_CATEGORY_MAX_DEPTH)
        )
    }

    fun sanitizeCategoryMaxDepth(depth: Int): Int {
        return depth.coerceIn(MIN_CATEGORY_MAX_DEPTH, MAX_CATEGORY_MAX_DEPTH)
    }
}
