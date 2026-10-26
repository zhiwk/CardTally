package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Category

data class CategoryMergePreview(val plan: CategoryMergePlanner.Plan, val records: Long, val recurring: Long, val undo: Long)

/** No record save/delete business calls: a merge must never replay balance effects. */
internal class CategoryMergeRepository(private val database: () -> SQLiteDatabase, private val maxDepth: () -> Int) {
    private fun rows(db: SQLiteDatabase): List<Category> = db.rawQuery("SELECT * FROM categories ORDER BY id", null).use { c ->
        fun index(name: String) = c.getColumnIndexOrThrow(name)
        buildList {
            while (c.moveToNext()) add(Category(c.getLong(index("id")), c.getString(index("name")), c.getInt(index("type")),
                c.getString(index("icon")), c.getString(index("color")),
                if (c.isNull(index("parent_id"))) null else c.getLong(index("parent_id")), c.getInt(index("sort_order"))))
        }
    }

    private fun preview(db: SQLiteDatabase, source: Long, target: Long): CategoryMergePreview {
        val plan = CategoryMergePlanner.prepare(rows(db), source, target, maxDepth())
        fun count(table: String, ids: Set<Long>): Long = ids.sumOf { id ->
            db.rawQuery("SELECT COUNT(*) FROM $table WHERE category_id = ?", arrayOf(id.toString())).use { it.moveToFirst(); it.getLong(0) }
        }
        val groups = plan.sourceNodes - plan.affectedLeaves
        if (listOf("records", "recurring_records", "record_deletion_undo").any { count(it, groups) > 0 })
            throw CategoryMergeException(CategoryMergeException.Reason.INVALID_REFERENCES)
        return CategoryMergePreview(plan, count("records", plan.affectedLeaves), count("recurring_records", plan.affectedLeaves),
            count("record_deletion_undo", plan.affectedLeaves))
    }

    fun preview(source: Long, target: Long): CategoryMergePreview {
        val db = database()
        db.beginTransaction()
        try { return preview(db, source, target).also { db.setTransactionSuccessful() } }
        finally { db.endTransaction() }
    }

    fun merge(expected: CategoryMergePreview) {
        val db = database()
        db.beginTransaction()
        try {
            val actual = preview(db, expected.plan.sourceId, expected.plan.targetId)
            if (actual != expected) throw CategoryMergeException(CategoryMergeException.Reason.STALE)
            val plan = actual.plan
            val remaining = plan.result.associateBy { it.id }
            for (id in plan.affectedLeaves) {
                val targetId = plan.aliases[id] ?: id
                val target = remaining.getValue(targetId)
                val path = CategoryMergePlanner.path(plan.result, targetId)
                for (table in listOf("records", "record_deletion_undo")) {
                    db.update(table, ContentValues().apply {
                        put("category_id", targetId); put("category", target.name)
                        put("category_name_snapshot", target.name); put("category_path_snapshot", path)
                    }, "category_id = ?", arrayOf(id.toString()))
                }
                db.update("recurring_records", ContentValues().apply {
                    put("category_id", targetId); put("category_name", target.name); put("category_path", path)
                }, "category_id = ?", arrayOf(id.toString()))
            }
            for (row in plan.result) if (row != plan.original.first { it.id == row.id }) {
                db.update("categories", ContentValues().apply {
                    put("parent_id", row.parentId); put("sort_order", row.sortOrder)
                }, "id = ?", arrayOf(row.id.toString()))
            }
            for ((old, target) in plan.aliases) {
                db.execSQL("UPDATE backup_import_map SET local_row_id = ? WHERE table_name = 'categories' AND local_row_id = ?", arrayOf(target, old))
                db.execSQL("UPDATE ${MergedCategoryMigration.ALIASES} SET new_id = ? WHERE new_id = ?", arrayOf(target, old))
                db.execSQL("INSERT OR REPLACE INTO ${MergedCategoryMigration.ALIASES} VALUES (?, ?)", arrayOf(old, target))
                db.delete("categories", "id = ?", arrayOf(old.toString()))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
