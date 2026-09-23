package com.example.cardtally.database

import com.example.cardtally.model.Category

/** Deterministic parent-before-child ordering for the category rows loaded from SQLite. */
internal object CategoryTreeOrdering {
    fun order(categories: List<Category>): List<Category> {
        val byParent = categories.groupBy { category ->
            category.parentId?.takeIf { parentId -> categories.any { it.id == parentId } }
        }
        val ordered = mutableListOf<Category>()
        val visited = mutableSetOf<Long>()
        fun appendChildren(parentId: Long?) {
            byParent[parentId].orEmpty().forEach { category ->
                if (visited.add(category.id)) {
                    ordered += category
                    appendChildren(category.id)
                }
            }
        }
        appendChildren(null)
        // Corrupt legacy cycles cannot become roots; preserve them in output rather than hiding rows.
        categories.filterNot { it.id in visited }.forEach { category ->
            if (visited.add(category.id)) ordered += category
        }
        return ordered
    }
}
