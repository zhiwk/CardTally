package com.example.cardtally.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.util.Money
import com.example.cardtally.model.Category
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperRecursiveCategoryQueryTest {

    private lateinit var context: Context
    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        databaseHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        databaseHelper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun getLeafCategoriesByType_returnsOnlyLeafCategoriesForRecordSelection() {
        val database = databaseHelper.writableDatabase
        ensureRecursiveSchemaForTestData(database)
        database.delete("records", null, null)
        database.delete("categories", null, null)

        insertCategory(database, id = 1L, name = "餐饮", type = EXPENSE_TYPE, parentId = null)
        insertCategory(database, id = 2L, name = "早餐", type = EXPENSE_TYPE, parentId = 1L)
        insertCategory(database, id = 3L, name = "午餐", type = EXPENSE_TYPE, parentId = 1L)
        insertCategory(database, id = 4L, name = "交通", type = EXPENSE_TYPE, parentId = null)
        insertCategory(database, id = 5L, name = "地铁", type = EXPENSE_TYPE, parentId = 4L)

        val leafQuery = DatabaseHelper::class.java.getMethod(
            "getLeafCategoriesByType",
            Int::class.javaPrimitiveType
        )
        @Suppress("UNCHECKED_CAST")
        val leafCategories = leafQuery.invoke(databaseHelper, EXPENSE_TYPE) as List<Category>

        assertEquals(3, leafCategories.size)
        assertEquals(setOf("早餐", "午餐", "地铁"), leafCategories.map { it.name }.toSet())
        assertTrue(leafCategories.none { it.name == "餐饮" || it.name == "交通" })
    }

    @Test
    fun categoryStatistics_remainFlatWithoutParentRollup() {
        val database = databaseHelper.writableDatabase
        ensureRecursiveSchemaForTestData(database)
        database.delete("records", null, null)
        database.delete("categories", null, null)

        insertCategory(database, id = 1L, name = "餐饮", type = EXPENSE_TYPE, parentId = null)
        insertCategory(database, id = 2L, name = "早餐", type = EXPENSE_TYPE, parentId = 1L)
        insertCategory(database, id = 3L, name = "午餐", type = EXPENSE_TYPE, parentId = 1L)

        insertRecord(
            database = database,
            id = 1L,
            date = "2026-04-13",
            amount = 90.0,
            category = "餐饮",
            categoryId = 1L,
            categoryNameSnapshot = "餐饮",
            categoryPathSnapshot = "餐饮"
        )
        insertRecord(
            database = database,
            id = 2L,
            date = "2026-04-13",
            amount = 20.0,
            category = "餐饮/早餐",
            categoryId = 2L,
            categoryNameSnapshot = "早餐",
            categoryPathSnapshot = "餐饮/早餐"
        )
        insertRecord(
            database = database,
            id = 3L,
            date = "2026-04-13",
            amount = 30.0,
            category = "餐饮/午餐",
            categoryId = 3L,
            categoryNameSnapshot = "午餐",
            categoryPathSnapshot = "餐饮/午餐"
        )

        val statistics = databaseHelper.getCategoryStatistics(EXPENSE_TYPE)

        assertEquals(3, statistics.size)
        assertEquals(listOf(20.0, 30.0, 90.0), statistics.values.sorted())
        assertTrue(statistics.values.none { it == 140.0 })
    }

    private fun ensureRecursiveSchemaForTestData(database: SQLiteDatabase) {
        ensureColumn(
            database = database,
            tableName = "categories",
            columnName = "parent_id",
            alterStatement = "ALTER TABLE categories ADD COLUMN parent_id INTEGER"
        )
        ensureColumn(
            database = database,
            tableName = "records",
            columnName = "category_id",
            alterStatement = "ALTER TABLE records ADD COLUMN category_id INTEGER"
        )
        ensureColumn(
            database = database,
            tableName = "records",
            columnName = "category_name_snapshot",
            alterStatement = "ALTER TABLE records ADD COLUMN category_name_snapshot TEXT"
        )
        ensureColumn(
            database = database,
            tableName = "records",
            columnName = "category_path_snapshot",
            alterStatement = "ALTER TABLE records ADD COLUMN category_path_snapshot TEXT"
        )
    }

    private fun ensureColumn(
        database: SQLiteDatabase,
        tableName: String,
        columnName: String,
        alterStatement: String
    ) {
        if (!tableHasColumn(database, tableName, columnName)) {
            database.execSQL(alterStatement)
        }
    }

    private fun tableHasColumn(database: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        database.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == columnName) {
                    return true
                }
            }
        }
        return false
    }

    private fun insertCategory(
        database: SQLiteDatabase,
        id: Long,
        name: String,
        type: Int,
        parentId: Long?
    ) {
        database.insertOrThrow("categories", null, ContentValues().apply {
            put("id", id)
            put("name", name)
            put("type", type)
            putNull("icon")
            if (parentId != null) {
                put("parent_id", parentId)
            } else {
                putNull("parent_id")
            }
        })
    }

    private fun insertRecord(
        database: SQLiteDatabase,
        id: Long,
        date: String,
        amount: Double,
        category: String,
        categoryId: Long?,
        categoryNameSnapshot: String,
        categoryPathSnapshot: String
    ) {
        database.insertOrThrow("records", null, ContentValues().apply {
            put("id", id)
            put("date", date)
            put("amount", requireNotNull(Money.toMinor(amount)))
            put("category", category)
            put("type", EXPENSE_TYPE)
            putNull("description")
            putNull("asset_source")
            put("sort_order", id.toInt())
            if (categoryId != null) {
                put("category_id", categoryId)
            } else {
                putNull("category_id")
            }
            put("category_name_snapshot", categoryNameSnapshot)
            put("category_path_snapshot", categoryPathSnapshot)
        })
    }

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val EXPENSE_TYPE = 0
    }
}
