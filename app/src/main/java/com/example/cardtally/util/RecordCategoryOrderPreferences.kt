package com.example.cardtally.util

import android.content.Context
import com.example.cardtally.model.Category

/** Keeps record-entry ordering separate from category management ordering. */
object RecordCategoryOrderPreferences {
    private const val PREFS_NAME = "record_category_order_prefs"

    internal fun remapMergedIds(context: Context, aliases: Map<Long, Long>) {
        if (aliases.isEmpty()) return
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        var changed = false
        for (type in 0..1) for (quick in listOf(false, true)) {
            val key = keyFor(type, quick)
            val old = preferences.getString(key, null) ?: continue
            val updated = old.split(',').mapNotNull { it.toLongOrNull() }
                .map { aliases[it] ?: it }.distinct().joinToString(",")
            if (old != updated) { editor.putString(key, updated); changed = true }
        }
        if (changed) editor.apply()
    }

    fun orderForMode(context: Context, categories: List<Category>, quickMode: Boolean): List<Category> {
        val candidates = if (quickMode) leafCategories(categories) else parentCategories(categories)
        val order = readOrder(context, keyFor(categories.firstOrNull()?.type ?: 0, quickMode))
        return orderCategories(candidates, order)
    }

    fun saveOrder(context: Context, type: Int, quickMode: Boolean, categoryIds: List<Long>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(keyFor(type, quickMode), categoryIds.joinToString(","))
            .apply()
    }

    internal fun orderCategories(categories: List<Category>, order: List<Long>): List<Category> {
        val byId = categories.associateBy { it.id }
        val ordered = order.mapNotNull(byId::get)
        val orderedIds = ordered.mapTo(mutableSetOf()) { it.id }
        return ordered + categories.filterNot { it.id in orderedIds }
    }

    /** Resolves the first recordable category after the caller has applied its mode order. */
    internal fun firstLeafCategory(categories: List<Category>): Category? =
        leafCategories(categories).firstOrNull()

    private fun readOrder(context: Context, key: String): List<Long> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.toLongOrNull() }
            .distinct()

    private fun parentCategories(categories: List<Category>): List<Category> =
        categories.filter { it.parentId == null }

    private fun leafCategories(categories: List<Category>): List<Category> {
        val parentIds = categories.mapNotNull { it.parentId }.toSet()
        return categories.filter { it.id !in parentIds }
    }

    private fun keyFor(type: Int, quickMode: Boolean): String =
        "${if (quickMode) "quick" else "standard"}_$type"
}
