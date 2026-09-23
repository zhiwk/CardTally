package com.example.cardtally.database

import com.example.cardtally.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTreeOrderingTest {
    @Test
    fun ordersNestedCategoriesParentFirstAndKeepsOrphansVisible() {
        val root = Category(id = 1, name = "Root")
        val child = Category(id = 2, name = "Child", parentId = 1)
        val grandchild = Category(id = 3, name = "Grandchild", parentId = 2)
        val orphan = Category(id = 4, name = "Orphan", parentId = 99)
        assertEquals(
            listOf(root, child, grandchild, orphan),
            CategoryTreeOrdering.order(listOf(root, child, grandchild, orphan))
        )
    }

    @Test
    fun corruptedCyclesDoNotRecurseForeverOrDropRows() {
        val first = Category(id = 1, name = "First", parentId = 2)
        val second = Category(id = 2, name = "Second", parentId = 1)
        assertEquals(listOf(first, second), CategoryTreeOrdering.order(listOf(first, second)))
    }
}
