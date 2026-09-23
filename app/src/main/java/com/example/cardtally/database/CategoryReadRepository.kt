package com.example.cardtally.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Category

/** Read/query boundary for category rows shared by record, statistics and management flows. */
internal class CategoryReadRepository(
    private val readableDatabase: () -> SQLiteDatabase,
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
        val sortOrder: String
    )

    fun getById(db: SQLiteDatabase, id: Long): Category? = db.rawQuery(
        "SELECT * FROM ${columns.table} WHERE ${columns.id} = ?",
        arrayOf(id.toString())
    ).use { cursor -> if (cursor.moveToFirst()) fromCursor(cursor) else null }

    fun getByType(db: SQLiteDatabase, type: Int): List<Category> {
        val result = mutableListOf<Category>()
        db.rawQuery(
            "SELECT * FROM ${columns.table} WHERE ${columns.type} = ? " +
                "ORDER BY CASE WHEN ${columns.parentId} IS NULL THEN 0 ELSE 1 END, " +
                "${columns.parentId}, ${columns.sortOrder} ASC, ${columns.name} COLLATE NOCASE ASC, ${columns.id} ASC",
            arrayOf(type.toString())
        ).use { cursor -> while (cursor.moveToNext()) result += fromCursor(cursor) }
        return result
    }

    fun getByType(type: Int): List<Category> {
        val result = mutableListOf<Category>()
        readableDatabase().rawQuery(
            "SELECT * FROM ${columns.table} WHERE ${columns.type} = ?",
            arrayOf(type.toString())
        ).use { cursor -> while (cursor.moveToNext()) result += fromCursor(cursor) }
        return result
    }

    fun getLeavesByType(type: Int): List<Category> {
        val result = mutableListOf<Category>()
        readableDatabase().rawQuery(
            "SELECT c.* FROM ${columns.table} c WHERE c.${columns.type} = ? " +
                "AND NOT EXISTS (SELECT 1 FROM ${columns.table} child " +
                "WHERE child.${columns.parentId} = c.${columns.id} AND child.${columns.type} = c.${columns.type}) " +
                "ORDER BY c.${columns.name}",
            arrayOf(type.toString())
        ).use { cursor -> while (cursor.moveToNext()) result += fromCursor(cursor) }
        return result
    }

    fun buildPathLabel(categoryId: Long): String? {
        val db = readableDatabase()
        val segments = mutableListOf<String>()
        val visited = mutableSetOf<Long>()
        var category = getById(db, categoryId)
        while (category != null && visited.add(category.id)) {
            segments += category.name
            category = category.parentId?.let { getById(db, it) }
        }
        return segments.takeIf { it.isNotEmpty() }?.asReversed()?.joinToString(" / ")
    }

    private fun fromCursor(cursor: Cursor): Category {
        fun nullableString(column: String): String? {
            val index = cursor.getColumnIndex(column)
            return if (index < 0 || cursor.isNull(index)) null else cursor.getString(index)
        }
        fun nullableLong(column: String): Long? {
            val index = cursor.getColumnIndex(column)
            return if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index)
        }
        return Category(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(columns.id)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(columns.name)),
            type = cursor.getInt(cursor.getColumnIndexOrThrow(columns.type)),
            icon = nullableString(columns.icon),
            color = nullableString(columns.color),
            parentId = nullableLong(columns.parentId),
            sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(columns.sortOrder))
        )
    }
}
