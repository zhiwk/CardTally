package com.example.cardtally.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Record
import java.util.UUID

/** Transaction coordinator for record insert/update/delete/undo operations. */
internal class RecordWriteRepository(
    private val writableDatabase: () -> SQLiteDatabase,
    private val currentLedgerId: () -> Long,
    private val validateRecord: (SQLiteDatabase, Record) -> Unit,
    private val findRecord: (SQLiteDatabase, Long) -> Record?,
    private val recordFromCursor: (android.database.Cursor) -> Record,
    private val recordValues: (Record) -> ContentValues,
    private val undoValues: (Record, DatabaseHelper.RecordDeletionToken, Double) -> ContentValues,
    private val applyAssetEffect: (SQLiteDatabase, Record, Boolean) -> Unit,
    private val adjustAssetBalance: (SQLiteDatabase, Long, String?, Double, Boolean) -> Boolean,
    private val deleteRecordPhotos: (Record) -> Unit,
    private val columns: Columns
) {
    data class Columns(
        val recordsTable: String,
        val recordId: String,
        val recordLedgerId: String,
        val recordDate: String,
        val recordSortOrder: String,
        val categoryNameSnapshot: String,
        val categoryPathSnapshot: String,
        val undoTable: String,
        val undoToken: String,
        val undoExpiresAt: String,
        val undoBalanceDelta: String
    )

    fun add(record: Record): Long {
        val db = writableDatabase()
        db.beginTransaction()
        return try {
            validateRecord(db, record)
            val maxSortOrder = db.rawQuery(
                "SELECT COALESCE(MAX(${columns.recordSortOrder}), 0) FROM ${columns.recordsTable} " +
                    "WHERE ${columns.recordLedgerId} = ? AND ${columns.recordDate} = ?",
                arrayOf(currentLedgerId().toString(), record.date)
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
            val id = db.insertOrThrow(
                columns.recordsTable,
                null,
                recordValues(record).apply { put(columns.recordSortOrder, maxSortOrder + 1) }
            )
            applyAssetEffect(db, record, false)
            db.setTransactionSuccessful()
            id
        } catch (_: IllegalArgumentException) {
            -1L
        } finally {
            db.endTransaction()
        }
    }

    fun update(record: Record): Int {
        val db = writableDatabase()
        db.beginTransaction()
        return try {
            validateRecord(db, record)
            val oldRecord = findRecord(db, record.id) ?: return 0
            applyAssetEffect(db, oldRecord, true)
            val rowsAffected = db.update(
                columns.recordsTable,
                recordValues(record),
                "${columns.recordId} = ? AND ${columns.recordLedgerId} = ?",
                arrayOf(record.id.toString(), currentLedgerId().toString())
            )
            check(rowsAffected == 1)
            applyAssetEffect(db, record, false)
            db.setTransactionSuccessful()
            rowsAffected
        } catch (_: IllegalArgumentException) {
            0
        } finally {
            db.endTransaction()
        }
    }

    fun delete(id: Long, deletedAtEpochMs: Long, undoWindowMs: Long): DatabaseHelper.RecordDeletionToken? {
        val db = writableDatabase()
        db.beginTransaction()
        try {
            db.delete(
                columns.undoTable,
                "${columns.undoExpiresAt} <= ?",
                arrayOf(deletedAtEpochMs.toString())
            )
            val record = findRecord(db, id)
            if (record == null) {
                db.setTransactionSuccessful()
                return null
            }
            if (record.type == 2) {
                applyAssetEffect(db, record, true)
                check(db.delete(columns.recordsTable, "${columns.recordId} = ?", arrayOf(id.toString())) == 1)
                db.setTransactionSuccessful()
                return null
            }

            val requestedBalanceDelta = if (record.type == 1) -record.amount else record.amount
            val appliedBalanceDelta = if (
                record.assetId == null ||
                !adjustAssetBalance(db, record.assetId!!, record.assetSource, requestedBalanceDelta, true)
            ) 0.0 else requestedBalanceDelta
            val token = DatabaseHelper.RecordDeletionToken(
                value = UUID.randomUUID().toString(),
                expiresAtEpochMs = deletedAtEpochMs + undoWindowMs
            )
            check(db.insertOrThrow(columns.undoTable, null, undoValues(record, token, appliedBalanceDelta)) != -1L)
            check(db.delete(columns.recordsTable, "${columns.recordId} = ?", arrayOf(id.toString())) == 1)
            deleteRecordPhotos(record)
            db.setTransactionSuccessful()
            return token
        } finally {
            db.endTransaction()
        }
    }

    fun undoDeletion(token: DatabaseHelper.RecordDeletionToken, nowEpochMs: Long): Boolean {
        val db = writableDatabase()
        db.beginTransaction()
        try {
            val undoEntry = db.rawQuery(
                "SELECT * FROM ${columns.undoTable} WHERE ${columns.undoToken} = ?",
                arrayOf(token.value)
            ).use { cursor ->
                if (!cursor.moveToFirst()) null else UndoEntry(
                    record = recordFromCursor(cursor),
                    expiresAtEpochMs = cursor.getLong(cursor.getColumnIndexOrThrow(columns.undoExpiresAt)),
                    balanceDelta = com.example.cardtally.util.Money.toMajorDouble(
                        cursor.getLong(cursor.getColumnIndexOrThrow(columns.undoBalanceDelta))
                    )
                )
            }
            if (undoEntry == null) {
                db.setTransactionSuccessful()
                return false
            }
            if (nowEpochMs >= undoEntry.expiresAtEpochMs) {
                db.delete(columns.undoTable, "${columns.undoToken} = ?", arrayOf(token.value))
                db.setTransactionSuccessful()
                return false
            }

            val record = undoEntry.record
            val values = recordValues(record).apply {
                put(columns.recordId, record.id)
                put(columns.recordSortOrder, record.sortOrder)
                putNullable(columns.categoryNameSnapshot, record.categoryNameSnapshot)
                putNullable(columns.categoryPathSnapshot, record.categoryPathSnapshot)
            }
            db.insertOrThrow(columns.recordsTable, null, values)
            if (record.assetId != null && undoEntry.balanceDelta != 0.0) {
                check(adjustAssetBalance(db, record.assetId!!, record.assetSource, -undoEntry.balanceDelta, false))
            }
            check(db.delete(columns.undoTable, "${columns.undoToken} = ?", arrayOf(token.value)) == 1)
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    private data class UndoEntry(
        val record: Record,
        val expiresAtEpochMs: Long,
        val balanceDelta: Double
    )

    private fun ContentValues.putNullable(column: String, value: String?) {
        if (value == null) putNull(column) else put(column, value)
    }
}
