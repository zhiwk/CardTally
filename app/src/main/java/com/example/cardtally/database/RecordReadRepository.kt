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
        val amount: String,
        val categoryId: String,
        val category: String,
        val categoryPathSnapshot: String,
        val description: String,
        val type: String,
        val fee: String,
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

    fun dateBounds(): Pair<String, String>? = readableDatabase().rawQuery(
        "SELECT MIN(${columns.date}), MAX(${columns.date}) FROM ${columns.table} WHERE ${columns.ledgerId} = ?",
        arrayOf(currentLedgerId().toString())
    ).use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) to cursor.getString(1) else null
    }

    fun listPage(
        startDate: String? = null,
        endDate: String? = null,
        keyword: String? = null,
        categoryId: Long? = null,
        after: DatabaseHelper.RecordListCursor? = null,
        descendingWithinDate: Boolean = false,
        limit: Int = pageLimit
    ): DatabaseHelper.RecordListPage {
        val (selection, args) = selection(startDate, endDate, keyword, categoryId)
        return pagedQuery(selection, args, after, descendingWithinDate, limit)
    }

    fun assetPage(
        assetId: Long?, assetSource: String?, after: DatabaseHelper.RecordListCursor?
    ): DatabaseHelper.RecordListPage {
        val selection: String
        val args: MutableList<String>
        if (assetId != null) {
            selection = "(${columns.sourceAssetId} = ? OR ${columns.destinationAssetId} = ?)"
            args = mutableListOf(assetId.toString(), assetId.toString())
        } else {
            selection = "${columns.ledgerId} = ? AND ${columns.assetSource} = ?"
            args = mutableListOf(currentLedgerId().toString(), assetSource.orEmpty())
        }
        return pagedQuery(selection, args, after)
    }

    private fun pagedQuery(
        selection: String, args: MutableList<String>, after: DatabaseHelper.RecordListCursor?,
        descendingWithinDate: Boolean = false,
        limit: Int = pageLimit
    ): DatabaseHelper.RecordListPage {
        require(limit in 1..pageLimit)
        val comparison = if (descendingWithinDate) "<" else ">"
        val direction = if (descendingWithinDate) "DESC" else "ASC"
        val cursorClause = if (after == null) "" else
            " AND (${columns.date} < ? OR (${columns.date} = ? AND " +
                "(${columns.sortOrder} $comparison ? OR " +
                "(${columns.sortOrder} = ? AND ${columns.id} > ?))))"
        if (after != null) args.addAll(listOf(
            after.date, after.date, after.sortOrder.toString(), after.sortOrder.toString(), after.recordId.toString()
        ))
        val records = query(
            "SELECT * FROM ${columns.table} WHERE $selection$cursorClause " +
                "ORDER BY ${columns.date} DESC, ${columns.sortOrder} $direction, ${columns.id} ASC " +
                "LIMIT ${limit + 1}",
            args.toTypedArray()
        )
        val hasMore = records.size > limit
        val page = if (hasMore) records.dropLast(1) else records
        val next = if (hasMore) page.last().let { DatabaseHelper.RecordListCursor(it.date, it.sortOrder, it.id) } else null
        return DatabaseHelper.RecordListPage(page, next)
    }

    fun searchTotals(
        startDate: String?, endDate: String?, keyword: String?, categoryId: Long?
    ): DatabaseHelper.RecordSearchTotals {
        val (selection, args) = selection(startDate, endDate, keyword, categoryId)
        return readableDatabase().rawQuery(
            "SELECT COALESCE(SUM(CASE WHEN ${columns.type} = 0 THEN ${columns.amount} " +
                "WHEN ${columns.type} = 2 THEN ${columns.fee} ELSE 0 END), 0), " +
                "COALESCE(SUM(CASE WHEN ${columns.type} = 1 THEN ${columns.amount} ELSE 0 END), 0) " +
                "FROM ${columns.table} WHERE $selection",
            args.toTypedArray()
        ).use { cursor ->
            if (cursor.moveToFirst()) DatabaseHelper.RecordSearchTotals(cursor.getLong(0), cursor.getLong(1))
            else DatabaseHelper.RecordSearchTotals(0L, 0L)
        }
    }

    private fun selection(
        startDate: String?, endDate: String?, keyword: String?, categoryId: Long?
    ): Pair<String, MutableList<String>> {
        val clauses = mutableListOf("${columns.ledgerId} = ?")
        val args = mutableListOf(currentLedgerId().toString())
        if (startDate != null) { clauses += "${columns.date} >= ?"; args += startDate }
        if (endDate != null) { clauses += "${columns.date} <= ?"; args += endDate }
        if (categoryId != null) {
            clauses += "${columns.categoryId} = ?"
            args += categoryId.toString()
        } else if (!keyword.isNullOrBlank()) {
            clauses += "(instr(lower(COALESCE(${columns.categoryPathSnapshot}, ${columns.category}, '')), lower(?)) > 0 " +
                "OR instr(lower(COALESCE(${columns.description}, '')), lower(?)) > 0 " +
                "OR instr(printf('%d.%02d', ${columns.amount} / 100, ABS(${columns.amount} % 100)), ?) > 0 " +
                "OR instr(lower(COALESCE(${columns.assetSource}, '')), lower(?)) > 0 " +
                "OR instr(${columns.date}, ?) > 0)"
            repeat(5) { args += keyword }
        }
        return clauses.joinToString(" AND ") to args
    }

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
