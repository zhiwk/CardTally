package com.example.cardtally.database

import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Ledger
import com.example.cardtally.util.Money

/** Read-side queries for ledgers, asset pools and ledger provenance. */
internal class LedgerReadRepository(
    private val readableDatabase: () -> SQLiteDatabase,
    private val columns: Columns
) {
    data class Columns(
        val ledgersTable: String,
        val ledgerId: String,
        val ledgerName: String,
        val ledgerSubtitle: String,
        val ledgerSortOrder: String,
        val ledgerIcon: String,
        val sharedLedgersTable: String,
        val sharedLedgerId: String,
        val sharedSourceLedgerId: String,
        val sharedAssetsTable: String,
        val sharedAssetId: String,
        val assetsTable: String,
        val assetId: String,
        val assetLedgerId: String,
        val assetAmount: String,
        val assetArchived: String,
        val assetIncludeInTotal: String,
        val recordsTable: String,
        val recordId: String,
        val recordLedgerId: String
    )

    fun getLedgers(): List<Ledger> = readableDatabase().rawQuery(
        "SELECT ${columns.ledgerId}, ${columns.ledgerName}, ${columns.ledgerSubtitle}, " +
            "${columns.ledgerSortOrder}, ${columns.ledgerIcon} FROM ${columns.ledgersTable} " +
            "ORDER BY ${columns.ledgerSortOrder}, ${columns.ledgerId}",
        null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(Ledger(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getString(4)))
            }
        }
    }

    fun containsLedger(id: Long): Boolean = readableDatabase().rawQuery(
        "SELECT 1 FROM ${columns.ledgersTable} WHERE ${columns.ledgerId} = ? LIMIT 1",
        arrayOf(id.toString())
    ).use { it.moveToFirst() }

    fun getMasterLedgerId(): Long = readableDatabase().rawQuery(
        "SELECT ${columns.ledgerId} FROM ${columns.ledgersTable} " +
            "ORDER BY ${columns.ledgerSortOrder}, ${columns.ledgerId} LIMIT 1",
        null
    ).use { if (it.moveToFirst()) it.getLong(0) else 1L }

    fun getSharedSourceLedgerId(ledgerId: Long): Long? = readableDatabase().rawQuery(
        "SELECT ${columns.sharedSourceLedgerId} FROM ${columns.sharedLedgersTable} " +
            "WHERE ${columns.sharedLedgerId} = ?",
        arrayOf(ledgerId.toString())
    ).use { if (it.moveToFirst()) it.getLong(0) else null }

    fun getAssetPoolSummary(rootLedgerId: Long): Pair<Int, Double> = readableDatabase().rawQuery(
        "SELECT COUNT(*), COALESCE(SUM(CASE WHEN ${columns.assetIncludeInTotal} = 1 " +
            "THEN ${columns.assetAmount} ELSE 0 END), 0) FROM ${columns.assetsTable} " +
            "WHERE ${columns.assetLedgerId} = ? AND ${columns.assetArchived} = 0",
        arrayOf(rootLedgerId.toString())
    ).use { if (it.moveToFirst()) it.getInt(0) to Money.toMajorDouble(it.getLong(1)) else 0 to 0.0 }

    fun getSharedAssetIds(ledgerId: Long): Set<Long> = readableDatabase().rawQuery(
        "SELECT ${columns.sharedAssetId} FROM ${columns.sharedAssetsTable} " +
            "WHERE ${columns.sharedLedgerId} = ?",
        arrayOf(ledgerId.toString())
    ).use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getLong(0)) } }

    fun canManageAsset(assetId: Long, currentLedgerId: Long): Boolean = readableDatabase().rawQuery(
        "SELECT 1 FROM ${columns.assetsTable} WHERE ${columns.assetId} = ? " +
            "AND ${columns.assetLedgerId} = ? LIMIT 1",
        arrayOf(assetId.toString(), currentLedgerId.toString())
    ).use { it.moveToFirst() }

    fun getLedgerRecordCount(ledgerId: Long): Int = readableDatabase().rawQuery(
        "SELECT COUNT(*) FROM ${columns.recordsTable} WHERE ${columns.recordLedgerId} = ?",
        arrayOf(ledgerId.toString())
    ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun getLedgerNamesByIds(ledgerIds: Set<Long>): Map<Long, String> {
        if (ledgerIds.isEmpty()) return emptyMap()
        return readableDatabase().rawQuery(
            "SELECT ${columns.ledgerId}, ${columns.ledgerName} FROM ${columns.ledgersTable}",
            null
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    if (id in ledgerIds) put(id, cursor.getString(1))
                }
            }
        }
    }

    fun getLedgerIdForRecord(recordId: Long): Long? = readableDatabase().rawQuery(
        "SELECT ${columns.recordLedgerId} FROM ${columns.recordsTable} WHERE ${columns.recordId} = ?",
        arrayOf(recordId.toString())
    ).use { if (it.moveToFirst()) it.getLong(0) else null }
}
