package com.example.cardtally.database

import android.database.sqlite.SQLiteDatabase

/** Version-gated upgrade orchestration; destructive reset remains verification-flavor-only. */
internal class DatabaseSchemaUpgradeManager(
    private val isVerificationBuild: Boolean,
    private val resetVerificationDatabase: (SQLiteDatabase) -> Unit,
    private val ensureColumn: (SQLiteDatabase, String, String, String) -> Unit,
    private val initializeAssetSortOrders: (SQLiteDatabase) -> Unit,
    private val createRecurringTableAndIndex: (SQLiteDatabase) -> Unit,
    private val normalizeLegacyAssetCategory: (SQLiteDatabase) -> Unit,
    private val normalizeLegacyCardBank: (SQLiteDatabase) -> Unit,
    private val recurringScheduleColumns: List<ColumnMigration>,
    private val recurringTableRebuild: TableRebuildMigration,
    private val createPerformanceIndices: (SQLiteDatabase) -> Unit,
    private val config: Config
) {
    data class ColumnMigration(val table: String, val column: String, val alterSql: String)

    data class TableRebuildMigration(
        val table: String,
        val createTableSql: String,
        val copiedColumns: List<String>,
        val legacySuffix: String = "_legacy"
    ) {
        fun apply(db: SQLiteDatabase) {
            val legacyTable = "$table$legacySuffix"
            db.execSQL("ALTER TABLE $table RENAME TO $legacyTable")
            db.execSQL(createTableSql)
            val columnList = copiedColumns.joinToString(", ")
            db.execSQL("INSERT INTO $table ($columnList) SELECT $columnList FROM $legacyTable")
            db.execSQL("DROP TABLE $legacyTable")
        }
    }

    data class Config(
        val assetTable: String,
        val assetSortOrderColumn: String,
        val assetSortOrderSql: String,
        val recurringDueIndexSql: String
    )

    fun upgrade(db: SQLiteDatabase, oldVersion: Int) {
        if (isVerificationBuild) {
            resetVerificationDatabase(db)
            return
        }

        // Daily data is retained. Its supported upgrade path adds the later safe
        // columns/tables and performance indices without resetting financial rows.
        if (oldVersion < 33) {
            ensureColumn(db, config.assetTable, config.assetSortOrderColumn, config.assetSortOrderSql)
            initializeAssetSortOrders(db)
        }
        if (oldVersion < 34) createRecurringTableAndIndex(db)
        if (oldVersion < 35) normalizeLegacyAssetCategory(db)
        if (oldVersion < 36) {
            recurringScheduleColumns.forEach { migration ->
                ensureColumn(db, migration.table, migration.column, migration.alterSql)
            }
        }
        if (oldVersion < 37) {
            recurringTableRebuild.apply(db)
            db.execSQL(config.recurringDueIndexSql)
        }
        if (oldVersion < 38) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS backup_import_map (" +
                    "source_id TEXT NOT NULL, table_name TEXT NOT NULL, source_row_id TEXT NOT NULL, " +
                    "local_row_id INTEGER NOT NULL, " +
                    "PRIMARY KEY (source_id, table_name, source_row_id))"
            )
        }
        if (oldVersion < 39) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS backup_asset_snapshot (" +
                    "source_id TEXT NOT NULL, source_row_id TEXT NOT NULL, " +
                    "local_row_id INTEGER NOT NULL, source_amount INTEGER NOT NULL, " +
                    "PRIMARY KEY (source_id, source_row_id))"
            )
        }
        if (oldVersion < 40) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS backup_pending_settings (" +
                    "id INTEGER PRIMARY KEY CHECK(id = 1), payload TEXT NOT NULL)"
            )
        }
        if (oldVersion < 41) normalizeLegacyCardBank(db)
        createPerformanceIndices(db)
    }
}
