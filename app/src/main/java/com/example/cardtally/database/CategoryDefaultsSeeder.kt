package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** Creates the canonical initial income/expense category tree for a new local database. */
internal class CategoryDefaultsSeeder(private val columns: Columns) {
    data class Columns(
        val table: String,
        val id: String,
        val name: String,
        val type: String,
        val icon: String,
        val parentId: String,
        val sortOrder: String,
        val ledgerId: String
    )

    fun seed(db: SQLiteDatabase, ledgerId: Long) {
        val parentIds = mutableMapOf<Pair<Int, String>, Long>()
        for (entry in DefaultCategoryCatalog.entries) {
            val parentId = entry.parent?.let { parentIds.getValue(entry.type to it) }
            val id = db.insertOrThrow(columns.table, null, ContentValues().apply {
                put(columns.name, entry.name)
                put(columns.type, entry.type)
                put(columns.icon, entry.icon)
                put(columns.sortOrder, entry.sortOrder)
                put(columns.ledgerId, ledgerId)
                if (parentId != null) put(columns.parentId, parentId)
            })
            if (entry.parent == null) parentIds[entry.type to entry.name] = id
        }
    }
}
