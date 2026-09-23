package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Category

/** Transactional category writes and sibling-order persistence. */
internal class CategoryWriteRepository(
    private val writableDatabase: () -> SQLiteDatabase,
    private val validateParent: (SQLiteDatabase, Category) -> Unit,
    private val findExistingId: (SQLiteDatabase, Category) -> Long?,
    private val hasChildren: (SQLiteDatabase, Long) -> Boolean,
    private val isReferencedByRecord: (SQLiteDatabase, Long) -> Boolean,
    private val throwError: (DatabaseHelper.CategoryOperationError) -> Nothing,
    private val columns: Columns
) {
    data class Columns(
        val table: String,
        val id: String,
        val name: String,
        val type: String,
        val icon: String,
        val color: String,
        val parentId: String,
        val sortOrder: String,
        val ledgerId: String
    )

    fun add(category: Category): Long {
        val db = writableDatabase()
        validateParent(db, category)
        findExistingId(db, category)?.let { return it }
        val values = categoryValues(category).apply {
            put(columns.sortOrder, nextSortOrder(db, category.type, category.parentId))
            put(columns.ledgerId, 1L)
        }
        return db.insert(columns.table, null, values)
    }

    fun update(category: Category): Int {
        val db = writableDatabase()
        validateParent(db, category)
        return db.update(
            columns.table,
            categoryValues(category),
            "${columns.id} = ?",
            arrayOf(category.id.toString())
        )
    }

    fun delete(id: Long) {
        val db = writableDatabase()
        if (hasChildren(db, id)) throwError(DatabaseHelper.CategoryOperationError.HAS_CHILDREN)
        if (isReferencedByRecord(db, id)) throwError(DatabaseHelper.CategoryOperationError.IN_USE_BY_RECORDS)
        db.delete(columns.table, "${columns.id} = ?", arrayOf(id.toString()))
    }

    fun updateSortOrders(categoryIds: List<Long>) {
        if (categoryIds.isEmpty()) return
        val db = writableDatabase()
        db.beginTransaction()
        try {
            categoryIds.forEachIndexed { index, id ->
                db.update(
                    columns.table,
                    ContentValues().apply { put(columns.sortOrder, index) },
                    "${columns.id} = ?",
                    arrayOf(id.toString())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun initializeSortOrders(db: SQLiteDatabase) {
        db.rawQuery(
            "SELECT ${columns.id}, ${columns.type}, ${columns.parentId} FROM ${columns.table} " +
                "ORDER BY ${columns.type}, ${columns.parentId}, ${columns.name} COLLATE NOCASE, ${columns.id}",
            null
        ).use { cursor ->
            val nextByGroup = mutableMapOf<String, Int>()
            while (cursor.moveToNext()) {
                val parentIndex = cursor.getColumnIndex(columns.parentId)
                val parentId = if (parentIndex < 0 || cursor.isNull(parentIndex)) null else cursor.getLong(parentIndex)
                val groupKey = "${cursor.getInt(1)}:${parentId ?: "root"}"
                val order = nextByGroup.getOrDefault(groupKey, 0)
                db.update(
                    columns.table,
                    ContentValues().apply { put(columns.sortOrder, order) },
                    "${columns.id} = ?",
                    arrayOf(cursor.getLong(0).toString())
                )
                nextByGroup[groupKey] = order + 1
            }
        }
    }

    private fun categoryValues(category: Category) = ContentValues().apply {
        put(columns.name, category.name)
        put(columns.type, category.type)
        put(columns.icon, category.icon)
        put(columns.color, category.color ?: "#F5F5F5")
        category.parentId?.let { put(columns.parentId, it) } ?: putNull(columns.parentId)
    }

    private fun nextSortOrder(db: SQLiteDatabase, type: Int, parentId: Long?): Int {
        val selection = if (parentId == null) {
            "${columns.type} = ? AND ${columns.parentId} IS NULL"
        } else {
            "${columns.type} = ? AND ${columns.parentId} = ?"
        }
        val args = if (parentId == null) arrayOf(type.toString())
        else arrayOf(type.toString(), parentId.toString())
        return db.query(
            columns.table,
            arrayOf("MAX(${columns.sortOrder})"),
            selection,
            args,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) + 1 else 0
        }
    }
}
