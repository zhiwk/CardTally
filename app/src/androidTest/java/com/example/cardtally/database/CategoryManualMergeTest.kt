package com.example.cardtally.database

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Category
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryManualMergeTest {
    private lateinit var context: Context
    private lateinit var helper: DatabaseHelper
    private var previousDepth = 2
    @Before fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        previousDepth = com.example.cardtally.util.CategoryHierarchySettingsHelper.getCategoryMaxDepth(context)
        com.example.cardtally.util.CategoryHierarchySettingsHelper.saveCategoryMaxDepth(context, 2)
        context.deleteDatabase("CardTally.db")
        helper = DatabaseHelper(context)
    }
    @After fun tearDown() {
        if (::helper.isInitialized) helper.close()
        if (::context.isInitialized) {
            context.deleteDatabase("CardTally.db")
            com.example.cardtally.util.CategoryHierarchySettingsHelper.saveCategoryMaxDepth(context, previousDepth)
        }
    }

    @Test fun groupMergeUpdatesAllLedgersAndDisplaySnapshotsWithoutChangingFinancialFields() {
        val a = helper.addCategory(Category(name = "合并来源"))
        val b = helper.addCategory(Category(name = "合并目标"))
        val old = helper.addCategory(Category(name = "同名", parentId = a))
        val target = helper.addCategory(Category(name = "同名", parentId = b))
        val moved = helper.addCategory(Category(name = "独有", parentId = a))
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO records (date, amount, category, category_id, type, ledger_id, description) " +
            "VALUES ('2026-10-23', 1234, '旧显示', ?, 0, 1, '保留备注'), ('2026-10-22', 4567, '旧显示', ?, 0, 2, '另一账本')",
            arrayOf(old, moved))
        db.execSQL("INSERT INTO recurring_records (ledger_id, type, name, amount, category_id, category_name, frequency, start_date, next_due_date) " +
            "VALUES (1, 0, '重复任务', 500, ?, '旧显示', 'daily', '2026-10-23', '2026-10-24')", arrayOf(old))
        db.execSQL("INSERT INTO record_deletion_undo (undo_token, expires_at, balance_delta, id, date, amount, category, category_id, type) " +
            "VALUES ('merge-test', 9999999999999, 0, 999, '2026-10-23', 300, '旧显示', ?, 0)", arrayOf(old))
        db.execSQL("INSERT INTO backup_import_map VALUES ('merge-test', 'categories', 123, ?)", arrayOf(old))
        val preview = helper.previewCategoryMerge(a, b)
        assertEquals(2L, preview.records)
        assertEquals(1L, preview.recurring); assertEquals(1L, preview.undo)
        helper.mergeCategories(preview)
        db.rawQuery("SELECT amount, category_id, category, category_path_snapshot, ledger_id, description FROM records ORDER BY id", null).use { c ->
            assertTrue(c.moveToFirst()); assertEquals(1234L, c.getLong(0)); assertEquals(target, c.getLong(1))
            assertEquals("同名", c.getString(2)); assertEquals("合并目标 / 同名", c.getString(3)); assertEquals(1L, c.getLong(4)); assertEquals("保留备注", c.getString(5))
            assertTrue(c.moveToNext()); assertEquals(4567L, c.getLong(0)); assertEquals(moved, c.getLong(1))
            assertEquals("独有", c.getString(2)); assertEquals("合并目标 / 独有", c.getString(3)); assertEquals(2L, c.getLong(4))
        }
        db.rawQuery("SELECT category_id, category_name, category_path, amount, next_due_date FROM recurring_records", null).use { c ->
            assertTrue(c.moveToFirst()); assertEquals(target, c.getLong(0)); assertEquals("同名", c.getString(1))
            assertEquals("合并目标 / 同名", c.getString(2)); assertEquals(500L, c.getLong(3)); assertEquals("2026-10-24", c.getString(4))
        }
        db.rawQuery("SELECT category_id, category, amount FROM record_deletion_undo", null).use { c ->
            assertTrue(c.moveToFirst()); assertEquals(target, c.getLong(0)); assertEquals("同名", c.getString(1)); assertEquals(300L, c.getLong(2))
        }
        db.rawQuery("SELECT local_row_id FROM backup_import_map WHERE source_id = 'merge-test'", null).use { c ->
            assertTrue(c.moveToFirst()); assertEquals(target, c.getLong(0))
        }
        assertNull(helper.getCategoryById(old)); assertNull(helper.getCategoryById(a))
    }

    @Test fun staleConfirmationAndWriteFailureLeaveSourcesIntact() {
        val old = helper.addCategory(Category(name = "来源叶子"))
        val target = helper.addCategory(Category(name = "目标叶子"))
        val db = helper.writableDatabase
        val preview = helper.previewCategoryMerge(old, target)
        db.execSQL("INSERT INTO records (date, amount, category, category_id, type) VALUES ('2026-10-23', 100, '来源叶子', ?, 0)", arrayOf(old))
        try { helper.mergeCategories(preview); fail("Expected stale preview") }
        catch (e: CategoryMergeException) { assertEquals(CategoryMergeException.Reason.STALE, e.reason) }
        val updated = helper.previewCategoryMerge(old, target)
        db.execSQL("CREATE TRIGGER fail_merge BEFORE DELETE ON categories WHEN OLD.id = $old BEGIN SELECT RAISE(ABORT, 'injected'); END")
        try { helper.mergeCategories(updated); fail("Expected rollback") }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertNotNull(helper.getCategoryById(old))
        db.rawQuery("SELECT category_id, category FROM records", null).use { c ->
            assertTrue(c.moveToFirst()); assertEquals(old, c.getLong(0)); assertEquals("来源叶子", c.getString(1))
        }
    }
}
