package com.example.cardtally.database

import com.example.cardtally.model.Category
import org.junit.Assert.*
import org.junit.Test

class CategoryDefaultsAppenderTest {
    private fun append(rows: MutableList<Category>) {
        var nextId = (rows.maxOfOrNull { it.id } ?: 0L) + 1
        CategoryDefaultsAppender.append(rows.toList()) { entry, parentId, order ->
            val row = Category(id = nextId++, name = entry.name, type = entry.type, icon = entry.icon,
                parentId = parentId, sortOrder = order)
            rows += row
            row.id
        }
    }

    private fun assertCatalogVisible(rows: List<Category>) {
        for (root in DefaultCategoryCatalog.entries.filter { it.parent == null }) {
            val children = DefaultCategoryCatalog.entries.filter { it.type == root.type && it.parent == root.name }
            assertTrue("Missing default group ${root.name}", rows.any { parent ->
                parent.type == root.type && parent.parentId == null && parent.name == root.name &&
                    children.all { child -> rows.any { row ->
                        row.type == child.type && row.parentId == parent.id && row.name == child.name &&
                            rows.none { it.parentId == row.id }
                    } }
            })
        }
    }

    @Test fun emptyDatabaseGetsTheEntireCatalogAndCanonicalOrder() {
        val rows = mutableListOf<Category>()
        append(rows)
        assertEquals(144, rows.size)
        assertCatalogVisible(rows)
        DefaultCategoryCatalog.entries.zip(rows).forEach { (entry, row) ->
            assertEquals(entry.name, row.name)
            assertEquals(entry.sortOrder, row.sortOrder)
        }
        val first = rows.map { it.copy() }
        append(rows)
        assertEquals(first, rows)
    }

    @Test fun customRootsStayUnchangedAndDefaultsAppendSeparatelyForEachType() {
        val expense = Category(id = 10, name = "自定义支出", type = 0, icon = "custom", color = "#123456", sortOrder = 9)
        val income = Category(id = 20, name = "自定义收入", type = 1, sortOrder = 17)
        val original = listOf(expense.copy(), income.copy())
        val rows = mutableListOf(expense, income)
        append(rows)
        assertEquals(original, rows.take(2))
        assertTrue(rows.drop(2).filter { it.parentId == null && it.type == 0 }.all { it.sortOrder > 9 })
        assertTrue(rows.drop(2).filter { it.parentId == null && it.type == 1 }.all { it.sortOrder > 17 })
        assertEquals(146, rows.size)
        assertCatalogVisible(rows)
    }

    @Test fun existingParentAndLeafAreReusedAndMissingChildrenFollowCustomChildren() {
        val parent = Category(id = 1, name = "餐饮食品", sortOrder = 4)
        val breakfast = Category(id = 2, name = "餐饮", parentId = 1, sortOrder = 8, icon = "custom")
        val custom = Category(id = 3, name = "夜宵", parentId = 1, sortOrder = 30)
        val rows = mutableListOf(parent, breakfast, custom)
        append(rows)
        assertEquals(1, rows.count { it.name == "餐饮食品" && it.parentId == null && it.type == 0 })
        assertEquals(listOf(breakfast, custom), rows.filter { it.parentId == 1L }.take(2))
        assertTrue(rows.filter { it.parentId == 1L && it.id > 3 }.all { it.sortOrder > 30 })
        assertEquals("custom", rows.single { it.id == 2L }.icon)
        assertCatalogVisible(rows)
    }

    @Test fun sameNamedExistingLeafIsNeverConvertedIntoAParent() {
        val leaf = Category(id = 1, name = "餐饮食品", sortOrder = 5)
        val rows = mutableListOf(leaf)
        append(rows)
        assertTrue(rows.none { it.parentId == leaf.id })
        val defaultRoot = rows.single { it.id != leaf.id && it.name == "餐饮食品" && it.parentId == null && it.type == 0 }
        assertTrue(defaultRoot.sortOrder > leaf.sortOrder)
        assertCatalogVisible(rows)
    }

    @Test fun matchingNamesUnderAnotherParentOrTypeDoNotSuppressDefaults() {
        val rows = mutableListOf(Category(id = 1, name = "自定义"),
            Category(id = 2, name = "水果", parentId = 1), Category(id = 3, name = "餐饮食品", type = 1))
        append(rows)
        assertEquals(2, rows.count { it.name == "水果" && it.type == 0 })
        assertCatalogVisible(rows)
    }

    @Test fun sameNamedNonLeafChildIsPreservedAndDefaultLeafRemainsAvailable() {
        val rows = mutableListOf(Category(id = 1, name = "餐饮食品"),
            Category(id = 2, name = "餐饮", parentId = 1), Category(id = 3, name = "自定义", parentId = 2))
        append(rows)
        assertEquals(2, rows.count { it.name == "餐饮" && it.parentId == 1L })
        assertEquals(2L, rows.single { it.id == 3L }.parentId)
        assertCatalogVisible(rows)
    }

    @Test fun repeatingAppendDoesNotDuplicateACanonicalOrExtendedTree() {
        val rows = mutableListOf(Category(id = 1, name = "自定义"))
        append(rows)
        val first = rows.map { it.copy() }
        append(rows)
        assertEquals(first, rows)
    }
}
