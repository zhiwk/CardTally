package com.example.cardtally.database

import com.example.cardtally.model.Category

/** Additive catalog upgrade: never edits an existing row or turns a leaf into a parent. */
internal object CategoryDefaultsAppender {
    fun append(
        existing: List<Category>,
        insert: (DefaultCategoryCatalog.Entry, Long?, Int) -> Long
    ) {
        val rows = existing.toMutableList()
        fun hasChildren(id: Long) = rows.any { it.parentId == id }
        fun add(entry: DefaultCategoryCatalog.Entry, parentId: Long?): Category {
            val maxOrder = rows.filter { it.type == entry.type && it.parentId == parentId }
                .maxOfOrNull { it.sortOrder }
            val order = maxOrder?.let { Math.addExact(it, 1) } ?: 0
            val id = insert(entry, parentId, order)
            check(id > 0) { "Default category insertion failed" }
            return Category(id = id, name = entry.name, type = entry.type, icon = entry.icon,
                parentId = parentId, sortOrder = order).also { rows += it }
        }

        for (root in DefaultCategoryCatalog.entries.filter { it.parent == null }) {
            val children = DefaultCategoryCatalog.entries.filter { it.type == root.type && it.parent == root.name }
            // A same-named leaf may be referenced by records, recurring rules or preferences.
            // Preserve it as a leaf; put the default group after existing roots instead.
            val parent = rows.filter {
                it.type == root.type && it.parentId == null && it.name == root.name && hasChildren(it.id)
            }.sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.id }).firstOrNull()
                ?: add(root, null)
            for (child in children) {
                val exists = rows.any {
                    it.type == child.type && it.parentId == parent.id && it.name == child.name && !hasChildren(it.id)
                }
                if (!exists) add(child, parent.id)
            }
        }
    }
}
