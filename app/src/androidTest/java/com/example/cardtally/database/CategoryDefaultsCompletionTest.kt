package com.example.cardtally.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDefaultsCompletionTest {
    private lateinit var context: Context

    @Before fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("CardTally.db")
    }

    @After fun tearDown() {
        if (::context.isInitialized) context.deleteDatabase("CardTally.db")
    }

    @Test fun existingV43WithCategoriesMergesKnownLeavesAndKeepsRecordSnapshots() {
        legacyFixture()
        rawDatabase().use { db ->
            db.execSQL("CREATE TABLE category_defaults_state (id INTEGER PRIMARY KEY, catalog_version INTEGER NOT NULL)")
            db.execSQL("INSERT INTO category_defaults_state VALUES (1, 1)")
        }
        val original = rawDatabase().use { rows(it, "categories") to rows(it, "records") }
        DatabaseHelper(context).use { helper ->
            val db = helper.writableDatabase
            assertEquals(43, db.version)
            assertCatalog(db)
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM categories WHERE id = 502 AND name = '服饰'"))
            assertEquals(original.second.map { row -> row.filterIndexed { index, _ -> index != 4 } },
                rows(db, "records").map { row -> row.filterIndexed { index, _ -> index != 4 } })
            assertEquals(505, scalar(db, "SELECT category_id FROM records WHERE category = '午餐'"))
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM categories WHERE id = 505 AND name = '餐饮'"))
            assertEquals(1, scalar(db, "SELECT COUNT(*) FROM categories WHERE id = 507 AND parent_id = 504 AND name = '自定义夜宵'"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM categories WHERE parent_id = 503"))
            assertEquals(2, scalar(db, "SELECT catalog_version FROM category_defaults_state WHERE id = 1"))
        }
    }

    @Test fun freshInstallAndReopeningKeepExactlyTheCanonicalTree() {
        repeat(2) {
            DatabaseHelper(context).use { helper ->
                val db = helper.writableDatabase
                assertCatalog(db)
                assertEquals(144, scalar(db, "SELECT COUNT(*) FROM categories"))
            }
        }
    }

    @Test fun completedCatalogDoesNotRestoreLaterDeletionsOrRenames() {
        DatabaseHelper(context).use { helper ->
            helper.writableDatabase.execSQL("DELETE FROM categories WHERE name = '水果'")
            helper.writableDatabase.execSQL("UPDATE categories SET name = '我的购物' WHERE name = '日用购物'")
        }
        repeat(2) {
            DatabaseHelper(context).use { helper ->
                val db = helper.writableDatabase
                assertEquals(143, scalar(db, "SELECT COUNT(*) FROM categories"))
                assertEquals(0, scalar(db, "SELECT COUNT(*) FROM categories WHERE name IN ('水果', '日用购物')"))
                assertEquals(1, scalar(db, "SELECT COUNT(*) FROM categories WHERE name = '我的购物'"))
            }
        }
    }

    @Test fun failureRollsBackAppendsAndMarkerThenNextOpenRetries() {
        legacyFixture()
        val original = rawDatabase().use { db ->
            db.execSQL("CREATE TRIGGER fail_catalog BEFORE INSERT ON categories " +
                "WHEN NEW.name = '水果' BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
            rows(db, "categories")
        }
        try {
            DatabaseHelper(context).use { it.writableDatabase }
            fail("Expected the injected catalog insertion failure")
        } catch (_: SQLiteException) {
            // Opening must not leave a success marker or a partially appended catalog.
        }
        rawDatabase().use { db ->
            assertEquals(original, rows(db, "categories"))
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM sqlite_master WHERE name = 'category_defaults_state'"))
            db.execSQL("DROP TRIGGER fail_catalog")
        }
        DatabaseHelper(context).use { assertCatalog(it.writableDatabase) }
    }

    private fun legacyFixture() {
        DatabaseHelper(context).use { it.writableDatabase }
        rawDatabase().use { db ->
            db.execSQL("DROP TABLE category_defaults_state")
            db.execSQL("DELETE FROM categories")
            db.execSQL("INSERT INTO categories (id, name, type, icon, color, sort_order) " +
                "VALUES (501, '购物', 0, 'ic_category_shopping', '#123456', 7)")
            db.execSQL("INSERT INTO categories (id, name, type, parent_id, sort_order) " +
                "VALUES (502, '服饰', 0, 501, 9)")
            // A same-named leaf referenced by records must not become a parent.
            db.execSQL("INSERT INTO categories (id, name, type, sort_order) VALUES (503, '餐饮', 0, 11)")
            db.execSQL("INSERT INTO records (date, amount, category, category_id, type) " +
                "VALUES ('2026-10-23', 2500, '餐饮', 503, 0)")
            db.execSQL("INSERT INTO categories (id, name, type) VALUES (504, '餐饮', 0)")
            db.execSQL("INSERT INTO categories (id, name, type, parent_id) VALUES (505, '早餐', 0, 504), (506, '午餐', 0, 504), (507, '自定义夜宵', 0, 504)")
            db.execSQL("INSERT INTO records (date, amount, category, category_id, type) VALUES ('2026-10-23', 1000, '午餐', 506, 0)")
            db.version = 43
        }
    }

    private fun assertCatalog(db: SQLiteDatabase) {
        for (root in DefaultCategoryCatalog.entries.filter { it.parent == null }) {
            val children = DefaultCategoryCatalog.entries.filter { it.type == root.type && it.parent == root.name }
            val ids = db.rawQuery("SELECT id FROM categories WHERE type = ? AND name = ? AND parent_id IS NULL",
                arrayOf(root.type.toString(), root.name)).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
            }
            assertTrue("Missing complete group ${root.name}", ids.any { parent ->
                children.all { child ->
                    db.rawQuery("SELECT c.id FROM categories c WHERE c.type = ? AND c.name = ? " +
                        "AND c.parent_id = ? AND NOT EXISTS (SELECT 1 FROM categories d WHERE d.parent_id = c.id)",
                        arrayOf(child.type.toString(), child.name, parent.toString())).use { it.moveToFirst() }
                }
            })
        }
    }

    private fun rawDatabase() = SQLiteDatabase.openDatabase(
        context.getDatabasePath("CardTally.db").path, null, SQLiteDatabase.OPEN_READWRITE)

    private fun scalar(db: SQLiteDatabase, sql: String): Int = db.rawQuery(sql, null).use {
        check(it.moveToFirst())
        it.getInt(0)
    }

    private fun rows(db: SQLiteDatabase, table: String): List<List<String?>> =
        db.rawQuery("SELECT * FROM $table ORDER BY id", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add((0 until cursor.columnCount).map {
                    if (cursor.isNull(it)) null else cursor.getString(it)
                })
            }
        }
}
