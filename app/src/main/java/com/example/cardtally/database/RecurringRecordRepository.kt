package com.example.cardtally.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.RecurringRecord

/** SQLite persistence boundary for recurring templates; ledger-wide processing stays in DatabaseHelper. */
internal class RecurringRecordRepository(
    private val readableDatabase: () -> SQLiteDatabase,
    private val writableDatabase: () -> SQLiteDatabase,
    private val currentLedgerId: () -> Long,
    private val validateCategory: (SQLiteDatabase, RecurringRecord) -> Unit,
    private val isAssetAvailable: (SQLiteDatabase, Long, Long) -> Boolean,
    private val columns: Columns
) {
    data class Columns(
        val table: String,
        val id: String,
        val ledgerId: String,
        val type: String,
        val name: String,
        val amount: String,
        val categoryId: String,
        val categoryName: String,
        val categoryPath: String,
        val assetId: String,
        val assetSource: String,
        val destinationAssetId: String,
        val destinationAssetSource: String,
        val note: String,
        val frequency: String,
        val weeklyDay: String,
        val monthlyDay: String,
        val yearlyMonth: String,
        val yearlyDay: String,
        val intervalDays: String,
        val startDate: String,
        val endDate: String,
        val enabled: String,
        val nextDueDate: String
    )

    fun getForCurrentLedger(): List<RecurringRecord> = getAll(
        "${columns.ledgerId} = ?",
        arrayOf(currentLedgerId().toString())
    )

    fun getAll(): List<RecurringRecord> = getAll(null, null)

    private fun getAll(selection: String?, args: Array<String>?): List<RecurringRecord> {
        val result = mutableListOf<RecurringRecord>()
        val where = selection?.let { " WHERE $it" }.orEmpty()
        readableDatabase().rawQuery(
            "SELECT * FROM ${columns.table}$where ORDER BY ${columns.enabled} DESC, " +
                "${columns.nextDueDate}, ${columns.id}",
            args
        ).use { cursor -> while (cursor.moveToNext()) result += fromCursor(cursor) }
        return result
    }

    fun getForCurrentLedger(id: Long): RecurringRecord? = getById(
        "${columns.id} = ? AND ${columns.ledgerId} = ?",
        arrayOf(id.toString(), currentLedgerId().toString())
    )

    fun getById(id: Long): RecurringRecord? = getById(
        "${columns.id} = ?",
        arrayOf(id.toString())
    )

    private fun getById(selection: String, args: Array<String>): RecurringRecord? =
        readableDatabase().rawQuery(
            "SELECT * FROM ${columns.table} WHERE $selection",
            args
        ).use { cursor -> if (cursor.moveToFirst()) fromCursor(cursor) else null }

    fun save(recurring: RecurringRecord): Long {
        require(recurring.type in 0..2)
        require(recurring.name.isNotBlank())
        require(recurring.amountMinor > 0L)
        require(recurring.frequency in setOf(
            RecurringRecord.DAILY,
            RecurringRecord.WEEKLY,
            RecurringRecord.MONTHLY,
            RecurringRecord.YEARLY,
            RecurringRecord.INTERVAL
        ))
        require(recurring.startDate.isNotBlank() && recurring.nextDueDate.isNotBlank())
        val db = writableDatabase()
        val ledgerId = recurring.ledgerId.takeIf { it > 0L } ?: currentLedgerId()
        recurring.categoryId?.let { validateCategory(db, recurring) }
        recurring.assetId?.let { require(isAssetAvailable(db, ledgerId, it)) { "Asset unavailable" } }
        recurring.destinationAssetId?.let {
            require(recurring.type == 2 && it != recurring.assetId)
            require(isAssetAvailable(db, ledgerId, it)) { "Asset unavailable" }
        }
        if (recurring.type == 2) require(recurring.assetId != null && recurring.destinationAssetId != null)
        val values = toContentValues(recurring, ledgerId)
        return if (recurring.id == 0L) {
            db.insertOrThrow(columns.table, null, values)
        } else {
            check(db.update(columns.table, values, "${columns.id} = ?", arrayOf(recurring.id.toString())) == 1)
            recurring.id
        }
    }

    fun setEnabled(id: Long, enabled: Boolean): Boolean = writableDatabase().update(
        columns.table,
        ContentValues().apply { put(columns.enabled, if (enabled) 1 else 0) },
        "${columns.id} = ?",
        arrayOf(id.toString())
    ) == 1

    fun delete(id: Long): Boolean =
        writableDatabase().delete(columns.table, "${columns.id} = ?", arrayOf(id.toString())) == 1

    fun getEarliestDueDate(): String? = readableDatabase().rawQuery(
        "SELECT MIN(${columns.nextDueDate}) FROM ${columns.table} WHERE ${columns.enabled} = 1",
        null
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    fun updateNextDueDate(id: Long, ledgerId: Long, dueDate: String, enabled: Boolean? = null): Int =
        writableDatabase().update(
            columns.table,
            ContentValues().apply {
                put(columns.nextDueDate, dueDate)
                enabled?.let { put(columns.enabled, if (it) 1 else 0) }
            },
            "${columns.id} = ? AND ${columns.ledgerId} = ?",
            arrayOf(id.toString(), ledgerId.toString())
        )

    private fun toContentValues(item: RecurringRecord, ledgerId: Long) = ContentValues().apply {
        put(columns.ledgerId, ledgerId)
        put(columns.type, item.type)
        put(columns.name, item.name.trim())
        put(columns.amount, item.amountMinor)
        putNullable(columns.categoryId, item.categoryId)
        put(columns.categoryName, item.categoryName)
        put(columns.categoryPath, item.categoryPath)
        putNullable(columns.assetId, item.assetId)
        put(columns.assetSource, item.assetSource)
        putNullable(columns.destinationAssetId, item.destinationAssetId)
        put(columns.destinationAssetSource, item.destinationAssetSource)
        put(columns.note, item.note)
        put(columns.frequency, item.frequency)
        putNullable(columns.weeklyDay, item.weeklyDay?.toLong())
        putNullable(columns.monthlyDay, item.monthlyDay?.toLong())
        putNullable(columns.yearlyMonth, item.yearlyMonth?.toLong())
        putNullable(columns.yearlyDay, item.yearlyDay?.toLong())
        putNullable(columns.intervalDays, item.intervalDays?.toLong())
        put(columns.startDate, item.startDate)
        put(columns.endDate, item.endDate)
        put(columns.enabled, if (item.enabled) 1 else 0)
        put(columns.nextDueDate, item.nextDueDate)
    }

    private fun fromCursor(cursor: Cursor) = RecurringRecord(
        id = cursor.getLong(cursor.column(columns.id)),
        ledgerId = cursor.getLong(cursor.column(columns.ledgerId)),
        type = cursor.getInt(cursor.column(columns.type)),
        name = cursor.getString(cursor.column(columns.name)),
        amountMinor = cursor.getLong(cursor.column(columns.amount)),
        categoryId = cursor.nullableLong(columns.categoryId),
        categoryName = cursor.getString(cursor.column(columns.categoryName)),
        categoryPath = cursor.nullableString(columns.categoryPath),
        assetId = cursor.nullableLong(columns.assetId),
        assetSource = cursor.nullableString(columns.assetSource),
        destinationAssetId = cursor.nullableLong(columns.destinationAssetId),
        destinationAssetSource = cursor.nullableString(columns.destinationAssetSource),
        note = cursor.nullableString(columns.note),
        frequency = cursor.getString(cursor.column(columns.frequency)),
        weeklyDay = cursor.nullableInt(columns.weeklyDay),
        monthlyDay = cursor.nullableInt(columns.monthlyDay),
        yearlyMonth = cursor.nullableInt(columns.yearlyMonth),
        yearlyDay = cursor.nullableInt(columns.yearlyDay),
        intervalDays = cursor.nullableInt(columns.intervalDays),
        startDate = cursor.getString(cursor.column(columns.startDate)),
        endDate = cursor.nullableString(columns.endDate),
        enabled = cursor.getInt(cursor.column(columns.enabled)) == 1,
        nextDueDate = cursor.getString(cursor.column(columns.nextDueDate))
    )

    private fun ContentValues.putNullable(column: String, value: Long?) {
        if (value == null) putNull(column) else put(column, value)
    }

    private fun Cursor.column(name: String) = getColumnIndexOrThrow(name)
    private fun Cursor.nullableString(name: String): String? = getString(column(name))
    private fun Cursor.nullableLong(name: String): Long? {
        val index = column(name)
        return if (isNull(index)) null else getLong(index)
    }
    private fun Cursor.nullableInt(name: String): Int? {
        val index = column(name)
        return if (isNull(index)) null else getInt(index)
    }
}
