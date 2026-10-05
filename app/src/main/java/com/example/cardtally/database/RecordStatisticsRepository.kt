package com.example.cardtally.database

import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.util.Money

/** Aggregate SQL for the statistics screens, including the agreed transfer-fee accounting rule. */
internal class RecordStatisticsRepository(
    private val readableDatabase: () -> SQLiteDatabase,
    private val currentLedgerId: () -> Long,
    private val columns: Columns
) {
    data class Columns(
        val table: String,
        val ledgerId: String,
        val date: String,
        val amount: String,
        val category: String,
        val categoryId: String,
        val categoryPathSnapshot: String,
        val type: String,
        val fee: String
    )

    fun totalByTypeMinor(type: Int): Long {
        val db = readableDatabase()
        val ledgerId = currentLedgerId()
        val amount = db.rawQuery(
            "SELECT COALESCE(SUM(${columns.amount}), 0) FROM ${columns.table} " +
                "WHERE ${columns.ledgerId} = ? AND ${columns.type} = ?",
            arrayOf(ledgerId.toString(), type.toString())
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        val fee = if (type == 0) transferFeeSumMinor(db, ledgerId, null, null) else 0L
        return Math.addExact(amount, fee)
    }

    fun totalByTypeAndDateRangeMinor(type: Int, startDate: String, endDate: String): Long {
        val db = readableDatabase()
        val ledgerId = currentLedgerId()
        val amount = db.rawQuery(
            "SELECT COALESCE(SUM(${columns.amount}), 0) FROM ${columns.table} " +
                "WHERE ${columns.ledgerId} = ? AND ${columns.type} = ? AND ${columns.date} BETWEEN ? AND ?",
            arrayOf(ledgerId.toString(), type.toString(), startDate, endDate)
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        val fee = if (type == 0) transferFeeSumMinor(db, ledgerId, startDate, endDate) else 0L
        return Math.addExact(amount, fee)
    }

    fun categoryStatistics(type: Int): Map<String, Double> = readCategoryStatistics(
        "${columns.ledgerId} = ? AND ${columns.type} = ?",
        arrayOf(currentLedgerId().toString(), type.toString())
    )

    fun categoryStatisticsByDateRange(type: Int, startDate: String, endDate: String): Map<String, Double> =
        readCategoryStatistics(
            "${columns.ledgerId} = ? AND ${columns.type} = ? AND ${columns.date} BETWEEN ? AND ?",
            arrayOf(currentLedgerId().toString(), type.toString(), startDate, endDate)
        )

    fun categoryTotalsById(type: Int, startDate: String?, endDate: String?): List<DatabaseHelper.CategoryTotal> {
        val range = if (startDate != null && endDate != null) " AND ${columns.date} BETWEEN ? AND ?" else ""
        val args = mutableListOf(currentLedgerId().toString(), type.toString())
        if (range.isNotEmpty()) args.addAll(listOf(startDate!!, endDate!!))
        return readableDatabase().rawQuery(
            "SELECT ${columns.categoryId}, SUM(${columns.amount}), COUNT(*), " +
                "MAX(COALESCE(${columns.categoryPathSnapshot}, ${columns.category})) FROM ${columns.table} " +
                "WHERE ${columns.ledgerId} = ? AND ${columns.type} = ?$range " +
                "GROUP BY ${columns.categoryId}",
            args.toTypedArray()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    if (!cursor.isNull(0)) add(DatabaseHelper.CategoryTotal(
                        cursor.getLong(0), cursor.getLong(1), cursor.getInt(2), cursor.getString(3).orEmpty()
                    ))
                }
            }
        }
    }

    fun trendByDate(type: Int, startDate: String, endDate: String, byMonth: Boolean): Map<String, Long> {
        val key = if (byMonth) "SUBSTR(${columns.date}, 1, 7)" else columns.date
        val db = readableDatabase()
        val result = db.rawQuery(
            "SELECT $key, SUM(${columns.amount}) FROM ${columns.table} " +
                "WHERE ${columns.ledgerId} = ? AND ${columns.type} = ? AND ${columns.date} BETWEEN ? AND ? " +
                "GROUP BY $key ORDER BY $key",
            arrayOf(currentLedgerId().toString(), type.toString(), startDate, endDate)
        ).use { cursor ->
            buildMap { while (cursor.moveToNext()) put(cursor.getString(0), cursor.getLong(1)) }
        }
        if (type != 0) return result
        val withFees = result.toMutableMap()
        db.rawQuery(
            "SELECT $key, SUM(${columns.fee}) FROM ${columns.table} " +
                "WHERE ${columns.ledgerId} = ? AND ${columns.type} = 2 AND ${columns.date} BETWEEN ? AND ? " +
                "GROUP BY $key",
            arrayOf(currentLedgerId().toString(), startDate, endDate)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val date = cursor.getString(0)
                withFees[date] = Math.addExact(withFees[date] ?: 0L, cursor.getLong(1))
            }
        }
        return withFees
    }

    fun dailyTotals(startDate: String, endDate: String): Map<String, DatabaseHelper.DailyTotals> =
        readableDatabase().rawQuery(
            "SELECT ${columns.date}, " +
                "SUM(CASE WHEN ${columns.type} = 1 THEN ${columns.amount} ELSE 0 END), " +
                "SUM(CASE WHEN ${columns.type} = 0 THEN ${columns.amount} " +
                "WHEN ${columns.type} = 2 THEN ${columns.fee} ELSE 0 END) " +
                "FROM ${columns.table} WHERE ${columns.ledgerId} = ? AND ${columns.date} BETWEEN ? AND ? " +
                "GROUP BY ${columns.date}",
            arrayOf(currentLedgerId().toString(), startDate, endDate)
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    put(cursor.getString(0), DatabaseHelper.DailyTotals(cursor.getLong(1), cursor.getLong(2)))
                }
            }
        }

    fun monthlyStatistics(type: Int, year: Int): Map<String, Double> {
        val db = readableDatabase()
        val ledgerId = currentLedgerId()
        val result = linkedMapOf<String, Double>()
        db.rawQuery(
            "SELECT SUBSTR(${columns.date}, 1, 7), SUM(${columns.amount}) FROM ${columns.table} " +
                "WHERE ${columns.ledgerId} = ? AND ${columns.type} = ? " +
                "AND SUBSTR(${columns.date}, 1, 4) = ? GROUP BY SUBSTR(${columns.date}, 1, 7) " +
                "ORDER BY SUBSTR(${columns.date}, 1, 7)",
            arrayOf(ledgerId.toString(), type.toString(), year.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) result[cursor.getString(0)] = Money.toMajorDouble(cursor.getLong(1))
        }
        if (type == 0) {
            db.rawQuery(
                "SELECT SUBSTR(${columns.date}, 1, 7), SUM(${columns.fee}) FROM ${columns.table} " +
                    "WHERE ${columns.ledgerId} = ? AND ${columns.type} = 2 " +
                    "AND SUBSTR(${columns.date}, 1, 4) = ? GROUP BY SUBSTR(${columns.date}, 1, 7)",
                arrayOf(ledgerId.toString(), year.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val month = cursor.getString(0)
                    result[month] = (result[month] ?: 0.0) + Money.toMajorDouble(cursor.getLong(1))
                }
            }
        }
        return result
    }

    private fun readCategoryStatistics(selection: String, args: Array<String>): Map<String, Double> {
        val result = linkedMapOf<String, Double>()
        readableDatabase().rawQuery(
            "SELECT ${columns.category}, SUM(${columns.amount}) FROM ${columns.table} " +
                "WHERE $selection GROUP BY ${columns.category}",
            args
        ).use { cursor ->
            while (cursor.moveToNext()) result[cursor.getString(0)] = Money.toMajorDouble(cursor.getLong(1))
        }
        return result
    }

    private fun transferFeeSumMinor(
        db: SQLiteDatabase,
        ledgerId: Long,
        startDate: String?,
        endDate: String?
    ): Long {
        val range = if (startDate != null && endDate != null) " AND ${columns.date} BETWEEN ? AND ?" else ""
        val args = if (range.isEmpty()) arrayOf(ledgerId.toString(), "2")
        else arrayOf(ledgerId.toString(), "2", startDate!!, endDate!!)
        return db.rawQuery(
            "SELECT COALESCE(SUM(${columns.fee}), 0) FROM ${columns.table} " +
                "WHERE ${columns.ledgerId} = ? AND ${columns.type} = ?$range",
            args
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
    }
}
