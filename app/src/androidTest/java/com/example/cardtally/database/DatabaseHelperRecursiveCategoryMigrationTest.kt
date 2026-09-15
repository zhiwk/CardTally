package com.example.cardtally.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperRecursiveCategoryMigrationTest {

    private lateinit var context: Context
    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun upgradingLegacyVersion9_resetsFinancialDataToCurrentSchema() {
        createLegacyVersion9Database(
            categories = listOf(
                LegacyCategoryRow(id = 1L, name = "餐饮", type = EXPENSE_TYPE, icon = "ic_category_food")
            ),
            records = listOf(
                LegacyRecordRow(
                    id = 1L,
                    date = "2026-04-13",
                    amount = 32.5,
                    category = "餐饮",
                    type = EXPENSE_TYPE,
                    description = "午饭",
                    assetSource = "现金",
                    sortOrder = 1
                )
            )
        )

        databaseHelper = DatabaseHelper(context)
        val upgradedDatabase = databaseHelper.readableDatabase

        assertTableHasColumns(upgradedDatabase, "categories", "parent_id")
        assertTableHasColumns(
            upgradedDatabase,
            "records",
            "category_id",
            "category_name_snapshot",
            "category_path_snapshot"
        )

        assertTrue(databaseHelper.getAllRecords().isEmpty())
        assertTrue(databaseHelper.getCategoriesByType(EXPENSE_TYPE).isNotEmpty())
    }

    @Test
    fun upgradingLegacyVersion9_resetsAllDataForTheBetaSchema() {
        createLegacyVersion9Database(
            categories = listOf(
                LegacyCategoryRow(id = 1L, name = "早餐", type = EXPENSE_TYPE, icon = null),
                LegacyCategoryRow(id = 2L, name = "早餐", type = EXPENSE_TYPE, icon = null)
            ),
            records = listOf(
                LegacyRecordRow(
                    id = 1L,
                    date = "2026-04-13",
                    amount = 18.0,
                    category = "早餐",
                    type = EXPENSE_TYPE,
                    description = "豆浆油条",
                    assetSource = "现金",
                    sortOrder = 1
                )
            )
        )
        insertLegacyChat()

        databaseHelper = DatabaseHelper(context)
        val upgradedDatabase = databaseHelper.readableDatabase

        assertTableHasColumns(
            upgradedDatabase,
            "records",
            "category_id",
            "category_name_snapshot",
            "category_path_snapshot"
        )

        assertEquals(0, tableRowCount(upgradedDatabase, "records"))
        assertEquals(0, tableRowCount(upgradedDatabase, "ai_chat_sessions"))
        assertEquals(0, tableRowCount(upgradedDatabase, "ai_chat_messages"))
    }

    private fun insertLegacyChat() {
        val database = SQLiteDatabase.openDatabase(
            context.getDatabasePath(DATABASE_NAME).path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        database.insertOrThrow("ai_chat_sessions", null, ContentValues().apply {
            put("id", 7L)
            put("title", "Legacy session")
            put("created_at", 100L)
            put("updated_at", 200L)
        })
        database.insertOrThrow("ai_chat_messages", null, ContentValues().apply {
            put("id", 8L)
            put("session_id", 7L)
            put("role", "user")
            put("content", "Kept locally")
            put("is_error", 0)
            put("created_at", 150L)
        })
        database.close()
    }

    private fun tableRowCount(database: SQLiteDatabase, tableName: String): Int =
        database.rawQuery("SELECT COUNT(*) FROM $tableName", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun createLegacyVersion9Database(
        categories: List<LegacyCategoryRow>,
        records: List<LegacyRecordRow>
    ) {
        val databaseFile = context.getDatabasePath(DATABASE_NAME)
        databaseFile.parentFile?.mkdirs()

        val database = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        database.execSQL(
            "CREATE TABLE records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "category TEXT NOT NULL, " +
                "type INTEGER NOT NULL, " +
                "description TEXT, " +
                "asset_source TEXT, " +
                "sort_order INTEGER DEFAULT 0)"
        )
        database.execSQL(
            "CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "type INTEGER NOT NULL, " +
                "icon TEXT)"
        )
        database.execSQL(
            "CREATE TABLE assets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "type INTEGER NOT NULL, " +
                "is_archived INTEGER DEFAULT 0)"
        )
        database.execSQL(
            "CREATE TABLE ai_chat_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE ai_chat_messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "session_id INTEGER NOT NULL, " +
                "role TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "is_error INTEGER DEFAULT 0, " +
                "created_at INTEGER NOT NULL)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_updated_at " +
                "ON ai_chat_sessions(updated_at DESC)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session_created_at " +
                "ON ai_chat_messages(session_id, created_at ASC)"
        )

        categories.forEach { category ->
            database.insertOrThrow("categories", null, ContentValues().apply {
                put("id", category.id)
                put("name", category.name)
                put("type", category.type)
                if (category.icon != null) {
                    put("icon", category.icon)
                } else {
                    putNull("icon")
                }
            })
        }

        records.forEach { record ->
            database.insertOrThrow("records", null, ContentValues().apply {
                put("id", record.id)
                put("date", record.date)
                put("amount", record.amount)
                put("category", record.category)
                put("type", record.type)
                put("description", record.description)
                if (record.assetSource != null) {
                    put("asset_source", record.assetSource)
                } else {
                    putNull("asset_source")
                }
                put("sort_order", record.sortOrder)
            })
        }

        database.version = LEGACY_DATABASE_VERSION
        database.close()
    }

    private fun assertTableHasColumns(
        database: SQLiteDatabase,
        tableName: String,
        vararg expectedColumns: String
    ) {
        val actualColumns = getTableColumns(database, tableName)
        expectedColumns.forEach { columnName ->
            assertTrue(
                "$tableName should contain column '$columnName' after recursive category migration. Actual columns: $actualColumns",
                actualColumns.contains(columnName)
            )
        }
    }

    private fun getTableColumns(database: SQLiteDatabase, tableName: String): Set<String> {
        val columns = linkedSetOf<String>()
        database.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
        }
        return columns
    }

    private data class LegacyCategoryRow(
        val id: Long,
        val name: String,
        val type: Int,
        val icon: String?
    )

    private data class LegacyRecordRow(
        val id: Long,
        val date: String,
        val amount: Double,
        val category: String,
        val type: Int,
        val description: String?,
        val assetSource: String?,
        val sortOrder: Int
    )

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val LEGACY_DATABASE_VERSION = 9
        private const val EXPENSE_TYPE = 0
    }
}
