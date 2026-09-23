package com.example.cardtally.database

import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Category

/** Enforces parent, cycle and depth rules while leaving category SQL owned by the database layer. */
internal class CategoryHierarchyValidator(
    private val findCategory: (SQLiteDatabase, Long) -> Category?,
    private val configuredMaxDepth: () -> Int,
    private val throwError: (DatabaseHelper.CategoryOperationError) -> Nothing
) {
    fun validateParentAssignment(db: SQLiteDatabase, category: Category) {
        val parentId = category.parentId ?: return
        val categoryId = category.id.takeIf { it > 0L }
        val parent = findCategory(db, parentId)
            ?: throwError(DatabaseHelper.CategoryOperationError.PARENT_NOT_FOUND)

        if (parent.type != category.type) {
            throwError(DatabaseHelper.CategoryOperationError.PARENT_TYPE_MISMATCH)
        }
        if (categoryId != null && parentId == categoryId) {
            throwError(DatabaseHelper.CategoryOperationError.SELF_PARENT)
        }
        if (categoryId != null && isDescendant(db, categoryId, parentId)) {
            throwError(DatabaseHelper.CategoryOperationError.DESCENDANT_CYCLE)
        }

        val assignedDepth = resolveDepth(db, parentId, mutableSetOf()) + 1
        if (assignedDepth > configuredMaxDepth()) {
            throwError(DatabaseHelper.CategoryOperationError.MAX_DEPTH_EXCEEDED)
        }
    }

    private fun isDescendant(db: SQLiteDatabase, categoryId: Long, candidateParentId: Long): Boolean {
        var parentId: Long? = candidateParentId
        val visited = mutableSetOf<Long>()
        while (parentId != null) {
            if (!visited.add(parentId)) break
            if (parentId == categoryId) return true
            parentId = findCategory(db, parentId)?.parentId
        }
        return false
    }

    private fun resolveDepth(db: SQLiteDatabase, categoryId: Long, visiting: MutableSet<Long>): Int {
        if (!visiting.add(categoryId)) return 1
        val category = findCategory(db, categoryId) ?: return 1
        val depth = category.parentId?.let { resolveDepth(db, it, visiting) + 1 } ?: 1
        visiting.remove(categoryId)
        return depth
    }
}
