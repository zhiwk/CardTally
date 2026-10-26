package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Category

/** Creates the canonical initial income/expense category tree for a new local database. */
internal class CategoryDefaultsSeeder(private val columns: Columns) {
    companion object {
        // Local migration state, deliberately excluded from financial backup tables.
        const val COMPLETION_TABLE = "category_defaults_state"
        private const val CATALOG_VERSION = 2
    }

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

    /** Covers existing v43 databases too, without restoring later user deletions on each open. */
    fun completeOnce(db: SQLiteDatabase, ledgerId: Long) {
        if (completedVersion(db) >= CATALOG_VERSION) return
        db.beginTransaction()
        try {
            // Another helper may have completed the catalog while this connection waited.
            if (completedVersion(db) < CATALOG_VERSION) {
                db.execSQL("CREATE TABLE IF NOT EXISTS $COMPLETION_TABLE (" +
                    "id INTEGER PRIMARY KEY CHECK (id = 1), catalog_version INTEGER NOT NULL)")
                MergedCategoryMigration.apply(db) { appendMissing(db, ledgerId) }
                db.execSQL("INSERT OR REPLACE INTO $COMPLETION_TABLE (id, catalog_version) VALUES (1, ?)",
                    arrayOf(CATALOG_VERSION))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun completedVersion(db: SQLiteDatabase): Int {
        val exists = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(COMPLETION_TABLE)).use { it.moveToFirst() }
        if (!exists) return 0
        return db.rawQuery("SELECT catalog_version FROM $COMPLETION_TABLE WHERE id = 1", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /** Caller owns the transaction, whether this is an upgrade or catalog completion. */
    fun appendMissing(db: SQLiteDatabase, ledgerId: Long) {
        val existing = mutableListOf<Category>()
        db.query(columns.table, arrayOf(columns.id, columns.name, columns.type, columns.parentId, columns.sortOrder),
            null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                existing += Category(id = cursor.getLong(0), name = cursor.getString(1), type = cursor.getInt(2),
                    parentId = if (cursor.isNull(3)) null else cursor.getLong(3), sortOrder = cursor.getInt(4))
            }
        }
        CategoryDefaultsAppender.append(existing) { entry, parentId, order ->
            db.insertOrThrow(columns.table, null, ContentValues().apply {
                put(columns.name, entry.name)
                put(columns.type, entry.type)
                put(columns.icon, entry.icon)
                put(columns.sortOrder, order)
                put(columns.ledgerId, ledgerId)
                if (parentId != null) put(columns.parentId, parentId)
            })
        }
    }
}
