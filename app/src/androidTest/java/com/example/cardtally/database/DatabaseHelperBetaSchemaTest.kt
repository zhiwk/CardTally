package com.example.cardtally.database

import android.content.Context
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
class DatabaseHelperBetaSchemaTest {
    private lateinit var context: Context
    private lateinit var helper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("CardTally.db")
        helper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase("CardTally.db")
    }

    @Test
    fun betaSchema_usesIntegerMoneyAndCoreIndexes() {
        val db = helper.readableDatabase
        assertEquals("INTEGER", columnType(db, "records", "amount"))
        assertEquals("INTEGER", columnType(db, "records", "fee"))
        assertEquals("INTEGER", columnType(db, "assets", "amount"))
        assertEquals("INTEGER", columnType(db, "record_deletion_undo", "balance_delta"))
        assertTrue(indexNames(db, "records").contains("idx_records_ledger_date_sort"))
        assertTrue(indexNames(db, "records").contains("idx_records_ledger_type_date"))
        assertTrue(indexNames(db, "assets").contains("idx_assets_ledger_archived_name"))
    }

    private fun columnType(db: android.database.sqlite.SQLiteDatabase, table: String, column: String): String {
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == column) {
                    return cursor.getString(cursor.getColumnIndexOrThrow("type"))
                }
            }
        }
        error("Missing $table.$column")
    }

    private fun indexNames(db: android.database.sqlite.SQLiteDatabase, table: String): Set<String> {
        db.rawQuery("PRAGMA index_list($table)", null).use { cursor ->
            return buildSet {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
        }
    }
}
