package com.example.cardtally.database

import android.content.ContentValues
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Asset
import com.example.cardtally.model.AssetBalanceEvent
import com.example.cardtally.util.Money

/** Metadata and lifecycle writes for assets; record-balance transactions remain in DatabaseHelper. */
internal class AssetWriteRepository(
    private val writableDatabase: () -> SQLiteDatabase,
    private val currentLedgerId: () -> Long,
    private val activeAssetScope: () -> String,
    private val isAssetNameReferenced: (SQLiteDatabase, String) -> Boolean,
    private val throwError: (DatabaseHelper.AssetOperationError) -> Nothing,
    private val columns: Columns
) {
    data class Columns(
        val table: String,
        val id: String,
        val ledgerId: String,
        val name: String,
        val amount: String,
        val type: String,
        val categoryLabel: String,
        val categoryIcon: String,
        val archived: String,
        val pinned: String,
        val includeInTotal: String,
        val sortOrder: String
    )

    fun add(asset: Asset): Long {
        val db = writableDatabase()
        val amount = requireNotNull(Money.toMinor(asset.amount))
        db.beginTransaction()
        return try {
            val id = db.insertOrThrow(columns.table, null, assetValues(asset, amount).apply {
                put(columns.ledgerId, currentLedgerId())
                put(columns.sortOrder, nextSortOrder(db))
            })
            AssetHistoryRepository.insertBalanceEvent(db, id, AssetBalanceEvent.INITIAL, 0, amount)
            db.setTransactionSuccessful()
            id
        } catch (_: SQLException) {
            -1L
        } finally {
            db.endTransaction()
        }
    }

    fun archive(id: Long) {
        writableDatabase().update(
            columns.table,
            ContentValues().apply {
                put(columns.archived, 1)
                put(columns.pinned, 0)
            },
            "${columns.ledgerId} = ${currentLedgerId()} AND ${columns.id} = ?",
            arrayOf(id.toString())
        )
    }

    fun unarchive(id: Long) {
        writableDatabase().update(
            columns.table,
            ContentValues().apply { put(columns.archived, 0) },
            "${columns.ledgerId} = ${currentLedgerId()} AND ${columns.id} = ?",
            arrayOf(id.toString())
        )
    }

    fun update(asset: Asset): Int {
        val db = writableDatabase()
        val amount = requireNotNull(Money.toMinor(asset.amount))
        val selection = "${columns.ledgerId} = ${currentLedgerId()} AND ${columns.id} = ?"
        val args = arrayOf(asset.id.toString())
        db.beginTransaction()
        return try {
            // Read the persisted balance here: editors may already have mutated their Asset instance.
            val before = db.query(columns.table, arrayOf(columns.amount), selection, args, null, null, null).use {
                if (!it.moveToFirst()) return 0
                it.getLong(0)
            }
            Math.subtractExact(amount, before)
            val updated = db.update(columns.table, assetValues(asset, amount), selection, args)
            if (updated == 1 && before != amount) {
                AssetHistoryRepository.insertBalanceEvent(db, asset.id, AssetBalanceEvent.ADJUSTMENT, before, amount)
            }
            db.setTransactionSuccessful()
            updated
        } catch (_: SQLException) {
            0
        } catch (_: ArithmeticException) {
            0
        } finally {
            db.endTransaction()
        }
    }

    private fun assetValues(asset: Asset, amount: Long) = ContentValues().apply {
        put(columns.name, asset.name)
        put(columns.amount, amount)
        put(columns.type, asset.type)
        put(columns.categoryLabel, asset.categoryLabel)
        put(columns.categoryIcon, asset.categoryIconName)
        put(columns.archived, if (asset.isArchived) 1 else 0)
        put(columns.pinned, if (asset.isPinned) 1 else 0)
        put(columns.includeInTotal, if (asset.includeInTotal) 1 else 0)
    }

    fun setPinned(id: Long, pinned: Boolean): Int {
        val db = writableDatabase()
        return db.update(
            columns.table,
            ContentValues().apply {
                put(columns.pinned, if (pinned) 1 else 0)
                put(columns.sortOrder, nextSortOrder(db))
            },
            "${columns.ledgerId} = ${currentLedgerId()} AND ${columns.id} = ? AND ${columns.archived} = 0",
            arrayOf(id.toString())
        ).also { db.close() }
    }

    fun updateSortOrder(assetIds: List<Long>): Boolean {
        if (assetIds.isEmpty() || assetIds.distinct().size != assetIds.size) return false
        val db = writableDatabase()
        db.beginTransaction()
        return try {
            assetIds.forEachIndexed { order, id ->
                val changed = db.update(
                    columns.table,
                    ContentValues().apply { put(columns.sortOrder, order) },
                    "${activeAssetScope()} AND ${columns.id} = ? AND ${columns.archived} = 0",
                    arrayOf(id.toString())
                )
                if (changed != 1) return false
            }
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun deleteArchived(id: Long): Boolean {
        val db = writableDatabase()
        db.beginTransaction()
        try {
            val asset = findAsset(db, id) ?: run {
                db.setTransactionSuccessful()
                return false
            }
            if (!asset.second) throwError(DatabaseHelper.AssetOperationError.NOT_ARCHIVED)
            if (isAssetNameReferenced(db, asset.first)) {
                throwError(DatabaseHelper.AssetOperationError.IN_USE_BY_RECORDS)
            }
            val deleted = db.delete(
                columns.table,
                "${columns.ledgerId} = ${currentLedgerId()} AND ${columns.id} = ?",
                arrayOf(id.toString())
            ) == 1
            db.setTransactionSuccessful()
            return deleted
        } finally {
            db.endTransaction()
        }
    }

    fun delete(id: Long): Boolean {
        val db = writableDatabase()
        db.beginTransaction()
        try {
            val assetName = findAsset(db, id)?.first ?: run {
                db.setTransactionSuccessful()
                return false
            }
            if (isAssetNameReferenced(db, assetName)) {
                throwError(DatabaseHelper.AssetOperationError.IN_USE_BY_RECORDS)
            }
            val deleted = db.delete(
                columns.table,
                "${columns.ledgerId} = ${currentLedgerId()} AND ${columns.id} = ?",
                arrayOf(id.toString())
            ) == 1
            db.setTransactionSuccessful()
            return deleted
        } finally {
            db.endTransaction()
        }
    }

    private fun findAsset(db: SQLiteDatabase, id: Long): Pair<String, Boolean>? = db.rawQuery(
        "SELECT ${columns.name}, ${columns.archived} FROM ${columns.table} " +
            "WHERE ${columns.ledgerId} = ${currentLedgerId()} AND ${columns.id} = ?",
        arrayOf(id.toString())
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) to (cursor.getInt(1) == 1) else null
    }

    private fun nextSortOrder(db: SQLiteDatabase): Int = db.rawQuery(
        "SELECT COALESCE(MAX(${columns.sortOrder}), -1) + 1 FROM ${columns.table} " +
            "WHERE ${activeAssetScope()}",
        null
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
}
