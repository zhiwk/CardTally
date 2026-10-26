package com.example.cardtally.database

import com.example.cardtally.model.Category
import org.junit.Assert.*
import org.junit.Test

class CategoryMergePlannerTest {
    private val rows = listOf(Category(1, "来源"), Category(2, "目标"),
        Category(3, "同名", parentId = 1), Category(4, "独有", parentId = 1), Category(5, "同名", parentId = 2))

    @Test fun leafCanMergeAcrossGroupsWithoutMutatingInput() {
        val copy = rows.map { it.copy() }
        val plan = CategoryMergePlanner.prepare(rows, 3, 5, 2)
        assertEquals(mapOf(3L to 5L), plan.aliases)
        assertEquals(setOf(3L), plan.affectedLeaves)
        assertEquals(copy, rows)
    }

    @Test fun groupsMergeMatchingChildrenAndMoveDistinctChildren() {
        val plan = CategoryMergePlanner.prepare(rows, 1, 2, 2)
        assertEquals(mapOf(3L to 5L, 1L to 2L), plan.aliases)
        assertEquals(2L, plan.result.first { it.id == 4L }.parentId)
        assertEquals("目标 / 独有", CategoryMergePlanner.path(plan.result, 4))
        assertEquals(setOf(3L, 4L), plan.affectedLeaves)
    }

    @Test fun selfCrossTypeAndRelatedBranchesAreRejected() {
        listOf(1L to 1L, 1L to 3L, 3L to 1L).forEach { (a, b) -> reject { CategoryMergePlanner.prepare(rows, a, b, 3) } }
        reject { CategoryMergePlanner.prepare(listOf(Category(1, "支出"), Category(2, "收入", type = 1)), 1, 2, 2) }
    }

    @Test fun sameNameLeafGroupConflictIsNotSilentlyFlattened() {
        reject { CategoryMergePlanner.prepare(rows + Category(6, "深层", parentId = 3), 1, 2, 3) }
    }

    @Test fun movingDeepSubtreesRespectsMaximumDepth() {
        val deep = rows + Category(6, "深层", parentId = 4) + Category(7, "新目标", parentId = 5) +
            Category(8, "目标子项", parentId = 7)
        reject { CategoryMergePlanner.prepare(deep, 1, 7, 3) }
    }

    @Test fun nestedGroupsCanMergeRecursively() {
        val deep = rows + Category(6, "叶子", parentId = 3) + Category(7, "叶子", parentId = 5)
        val plan = CategoryMergePlanner.prepare(deep, 1, 2, 3)
        assertEquals(7L, plan.aliases[6L])
        assertEquals(setOf(6L, 4L), plan.affectedLeaves)
    }

    private fun reject(block: () -> Unit) {
        try { block(); fail("Expected merge rejection") } catch (_: CategoryMergeException) { }
    }
}
