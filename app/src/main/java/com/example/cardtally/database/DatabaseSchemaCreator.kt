package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** Creates the current schema in dependency order and seeds the first local ledger/categories. */
internal class DatabaseSchemaCreator(
    private val createTablesBeforeDefaultLedger: List<String>,
    private val defaultLedgerTable: String,
    private val defaultLedgerNameColumn: String,
    private val defaultLedgerSubtitleColumn: String,
    private val defaultLedgerOrderColumn: String,
    private val createTablesAfterDefaultLedger: List<String>,
    private val createIndices: List<String>,
    private val insertDefaultCategories: (SQLiteDatabase, Long) -> Unit
) {
    fun create(db: SQLiteDatabase) {
        createTablesBeforeDefaultLedger.forEach(db::execSQL)
        val ledgerId = db.insertOrThrow(
            defaultLedgerTable,
            null,
            ContentValues().apply {
                put(defaultLedgerNameColumn, "日常")
                put(defaultLedgerSubtitleColumn, "日常账本")
                put(defaultLedgerOrderColumn, 0)
            }
        )
        createTablesAfterDefaultLedger.forEach(db::execSQL)
        createIndices.forEach(db::execSQL)
        insertDefaultCategories(db, ledgerId)
    }
}
