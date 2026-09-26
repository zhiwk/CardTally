package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Record
import com.example.cardtally.util.Money

/** Owns the financial side effects that accompany ordinary record transactions. */
internal class RecordAssetBalanceRepository(
    private val assetScope: () -> String,
    private val columns: Columns
) {
    data class Columns(
        val assetsTable: String,
        val assetId: String,
        val assetName: String,
        val assetAmount: String,
        val assetArchived: String
    )

    fun adjustBalance(
        db: SQLiteDatabase,
        assetId: Long,
        assetName: String?,
        delta: Double,
        activeOnly: Boolean
    ): Boolean {
        val archiveClause = if (activeOnly) " AND ${columns.assetArchived} = 0" else ""
        val identityClause = if (assetId > 0) "${columns.assetId} = ?" else "${columns.assetName} = ?"
        val identityValue = if (assetId > 0) assetId.toString() else assetName.orEmpty()
        val exists = db.rawQuery(
            "SELECT 1 FROM ${columns.assetsTable} WHERE ${assetScope()} AND $identityClause$archiveClause LIMIT 1",
            arrayOf(identityValue)
        ).use { it.moveToFirst() }
        if (!exists) return false
        db.execSQL(
            "UPDATE ${columns.assetsTable} SET ${columns.assetAmount} = ${columns.assetAmount} + ? " +
                "WHERE ${assetScope()} AND $identityClause$archiveClause",
            arrayOf(requireNotNull(Money.toMinor(delta)).toString(), identityValue)
        )
        return true
    }

    fun applyRecordEffect(db: SQLiteDatabase, record: Record, reverse: Boolean) {
        val direction = if (reverse) -1 else 1
        when (record.type) {
            0 -> record.assetId?.let { check(updateAssetAmount(db, it, record.amount * direction, false)) }
            1 -> record.assetId?.let { check(updateAssetAmount(db, it, record.amount * direction, true)) }
            2 -> {
                val transferOut = record.amount + record.fee
                record.assetId?.let { check(updateAssetAmount(db, it, transferOut * direction, false)) }
                record.destinationAssetId?.let { check(updateAssetAmount(db, it, record.amount * direction, true)) }
            }
        }
    }

    private fun updateAssetAmount(db: SQLiteDatabase, assetId: Long, amount: Double, isAdd: Boolean): Boolean {
        val currentAmount = db.rawQuery(
            "SELECT ${columns.assetAmount} FROM ${columns.assetsTable} WHERE ${assetScope()} " +
                "AND ${columns.assetId} = ? AND ${columns.assetArchived} = 0 LIMIT 1",
            arrayOf(assetId.toString())
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else return false }
        val amountMinor = requireNotNull(Money.toMinor(amount))
        val newAmount = if (isAdd) currentAmount + amountMinor else currentAmount - amountMinor
        return db.update(
            columns.assetsTable,
            ContentValues().apply { put(columns.assetAmount, newAmount) },
            "${assetScope()} AND ${columns.assetId} = ? AND ${columns.assetArchived} = 0",
            arrayOf(assetId.toString())
        ) == 1
    }
}
