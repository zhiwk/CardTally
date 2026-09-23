package com.example.cardtally.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Record

/** Ledger-scoped and asset-scoped record queries with cursor pagination. */
internal class RecordReadRepository(
    private val readableDatabase: () -> SQLiteDatabase,
    private val currentLedgerId: () -> Long,
    private val fromCursor: (Cursor) -> Record,
    private val columns: Columns,
    private val pageLimit: Int
) {
    data class Columns(
        val table: String,
        val id: String,
        val ledgerId: String,
        val date: String,
        val sortOrder: String,
        val sourceAssetId: String,
        val destinationAssetId: String,
        val assetSource: String
    )

    fun getById(db: SQLiteDatabase, id: Long): Record? = db.rawQuery(
        "SELECT * FROM ${columns.table} WHERE ${columns.id} = ? AND ${columns.ledgerId} = ?",
        arrayOf(id.toString(), currentLedgerId().toString())
    ).use { cursor -> if (cursor.moveToFirst()) fromCursor(cursor) else null }

    fun getAll(): List<Record> = query(
        "SELECT * FROM ${columns.table} WHERE ${columns.ledgerId} = ? " +
            "ORDER BY ${columns.date} DESC, ${columns.sortOrder} ASC",
        arrayOf(currentLedgerId().toString())
    )

    fun byDateRange(startDate: String, endDate: String): List<Record> = query(
        "SELECT * FROM ${columns.table} WHERE ${columns.ledgerId} = ? AND ${columns.date} BETWEEN ? AND ? " +
            "ORDER BY ${columns.date} DESC, ${columns.sortOrder} ASC",
        arrayOf(currentLedgerId().toString(), startDate, endDate)
    )

    fun todayPage(todayDate: String, after: DatabaseHelper.RecordPageCursor?): DatabaseHelper.RecordPage {
        val query: String
        val args: Array<String>
        if (after == null) {
            query = "SELECT * FROM ${columns.table} WHERE ${columns.ledgerId} = ? AND ${columns.date} = ? " +
                "ORDER BY ${columns.sortOrder} ASC, ${columns.id} ASC LIMIT $pageLimit"
            args = arrayOf(currentLedgerId().toString(), todayDate)
        } else {
            query = "SELECT * FROM ${columns.table} WHERE ${columns.ledgerId} = ? AND ${columns.date} = ? " +
                "AND (${columns.sortOrder} > ? OR (${columns.sortOrder} = ? AND ${columns.id} > ?)) " +
                "ORDER BY ${columns.sortOrder} ASC, ${columns.id} ASC LIMIT $pageLimit"
            args = arrayOf(
                currentLedgerId().toString(), todayDate,
                after.sortOrder.toString(), after.sortOrder.toString(), after.recordId.toString()
            )
        }
        val records = query(query, args)
        val next = records.lastOrNull()?.takeIf { records.size == pageLimit }
            ?.let { DatabaseHelper.RecordPageCursor(it.sortOrder, it.id) }
        return DatabaseHelper.RecordPage(records, next)
    }

    fun byAssetSource(assetSource: String): List<Record> = query(
        "SELECT * FROM ${columns.table} WHERE ${columns.ledgerId} = ? AND ${columns.assetSource} = ? " +
            "ORDER BY ${columns.date} DESC, ${columns.sortOrder} ASC",
        arrayOf(currentLedgerId().toString(), assetSource)
    )

    fun byAssetId(assetId: Long): List<Record> = query(
        "SELECT * FROM ${columns.table} WHERE ${columns.ledgerId} = ? " +
            "AND (${columns.sourceAssetId} = ? OR ${columns.destinationAssetId} = ?) " +
            "ORDER BY ${columns.date} DESC, ${columns.sortOrder} ASC",
        arrayOf(currentLedgerId().toString(), assetId.toString(), assetId.toString())
    )

    fun allByAssetId(assetId: Long): List<Record> = query(
        "SELECT * FROM ${columns.table} WHERE ${columns.sourceAssetId} = ? OR ${columns.destinationAssetId} = ? " +
            "ORDER BY ${columns.date} DESC, ${columns.sortOrder} ASC, ${columns.id} ASC",
        arrayOf(assetId.toString(), assetId.toString())
    )

    private fun query(sql: String, args: Array<String>): List<Record> =
        readableDatabase().rawQuery(sql, args).use { cursor ->
            buildList { while (cursor.moveToNext()) add(fromCursor(cursor)) }
        }
}
