package com.example.cardtally.util

import com.example.cardtally.model.Category
import com.example.cardtally.model.Record

/** Current category labels for presentation only; never overwrite historical record snapshots. */
object RecordCategoryDisplay {
    fun paths(categories: List<Category>): Map<Long, String> {
        val byId = categories.associateBy { it.id }
        return categories.associate { category ->
            val segments = mutableListOf<String>()
            val visited = mutableSetOf<Long>()
            var current: Category? = category
            while (current != null && visited.add(current.id)) {
                segments += current.name
                current = current.parentId?.let(byId::get)
            }
            category.id to segments.asReversed().joinToString(" / ")
        }
    }

    fun label(record: Record, paths: Map<Long, String>): String =
        record.categoryId?.let(paths::get)?.takeIf { it.isNotBlank() }
            ?: record.categoryPathSnapshot?.takeIf { it.isNotBlank() }
            ?: record.categoryNameSnapshot?.takeIf { it.isNotBlank() }
            ?: record.category
}
