package com.example.cardtally.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.AssetBalanceEvent
import com.example.cardtally.model.AssetHistoryEntry
import com.example.cardtally.model.Record
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Keyset-paged union of bills and account-only events. Neither branch changes a balance on read. */
internal class AssetHistoryRepository(
    private val readableDatabase: () -> SQLiteDatabase,
    private val currentLedgerId: () -> Long,
    private val recordFromCursor: (Cursor) -> Record
) {
    fun page(
        assetId: Long?, assetName: String?, after: DatabaseHelper.AssetHistoryCursor?,
        limit: Int = DatabaseHelper.MAX_RECORD_QUERY_LIMIT
    ): DatabaseHelper.AssetHistoryPage {
        require(limit in 1..DatabaseHelper.MAX_RECORD_QUERY_LIMIT)
        val db = readableDatabase()
        val args = mutableListOf<String>()
        val recordScope = if (assetId != null) {
            args.addAll(listOf(assetId.toString(), assetId.toString()))
            "(asset_id = ? OR destination_asset_id = ?)"
        } else {
            args.addAll(listOf(currentLedgerId().toString(), assetName.orEmpty()))
            "ledger_id = ? AND asset_source = ?"
        }
        args += (assetId ?: 0L).toString()
        // Events have their own date and newest-first timestamp/ID. Bills retain their existing order.
        val union = "SELECT date, 0 AS kind, sort_order AS order_key, id AS tie_key, id AS entry_id " +
            "FROM records WHERE $recordScope UNION ALL " +
            "SELECT date, 1 AS kind, -created_at AS order_key, -id AS tie_key, id AS entry_id " +
            "FROM $TABLE WHERE asset_id = ?"
        val continuation = if (after == null) "" else {
            args.addAll(listOf(after.date, after.date, after.kind.toString(), after.kind.toString(),
                after.orderKey.toString(), after.orderKey.toString(), after.tieKey.toString()))
            " WHERE date < ? OR (date = ? AND (kind < ? OR (kind = ? AND " +
                "(order_key > ? OR (order_key = ? AND tie_key > ?)))))"
        }
        db.beginTransactionNonExclusive()
        return try {
            val keys = db.rawQuery(
                "SELECT * FROM ($union)$continuation " +
                    "ORDER BY date DESC, kind DESC, order_key ASC, tie_key ASC LIMIT ${limit + 1}",
                args.toTypedArray()
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(Key(
                        DatabaseHelper.AssetHistoryCursor(cursor.getString(0), cursor.getInt(1),
                            cursor.getLong(2), cursor.getLong(3)), cursor.getLong(4)))
                }
            }
            val pageKeys = keys.take(limit)
            val bills = readRows(db, "records", pageKeys.filter { it.cursor.kind == 0 }.map { it.id }, recordFromCursor)
                .associateBy { it.id }
            val events = readRows(db, TABLE, pageKeys.filter { it.cursor.kind == 1 }.map { it.id }, ::eventFromCursor)
                .associateBy { it.id }
            val entries = pageKeys.map { key ->
                if (key.cursor.kind == 0) AssetHistoryEntry.Bill(bills.getValue(key.id))
                else AssetHistoryEntry.Balance(events.getValue(key.id))
            }
            db.setTransactionSuccessful()
            DatabaseHelper.AssetHistoryPage(entries, pageKeys.lastOrNull()?.cursor?.takeIf { keys.size > limit })
        } finally {
            db.endTransaction()
        }
    }

    private data class Key(val cursor: DatabaseHelper.AssetHistoryCursor, val id: Long)

    private fun <T> readRows(db: SQLiteDatabase, table: String, ids: List<Long>, mapper: (Cursor) -> T): List<T> {
        if (ids.isEmpty()) return emptyList()
        return db.rawQuery("SELECT * FROM $table WHERE id IN (${ids.joinToString { "?" }})",
            ids.map(Long::toString).toTypedArray()).use { cursor ->
            buildList { while (cursor.moveToNext()) add(mapper(cursor)) }
        }
    }

    private fun eventFromCursor(cursor: Cursor) = AssetBalanceEvent(
        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
        assetId = cursor.getLong(cursor.getColumnIndexOrThrow("asset_id")),
        kind = cursor.getInt(cursor.getColumnIndexOrThrow("kind")),
        date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
        balanceBeforeMinor = cursor.getLong(cursor.getColumnIndexOrThrow("balance_before")),
        balanceAfterMinor = cursor.getLong(cursor.getColumnIndexOrThrow("balance_after"))
    )

    companion object {
        const val TABLE = "asset_balance_events"
        const val CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS $TABLE (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, asset_id INTEGER NOT NULL, " +
            "kind INTEGER NOT NULL CHECK(kind IN (0, 1)), date TEXT NOT NULL, created_at INTEGER NOT NULL, " +
            "balance_before INTEGER NOT NULL, balance_after INTEGER NOT NULL, " +
            "CHECK((kind = 0 AND balance_before = 0) OR (kind = 1 AND balance_before != balance_after)), " +
            "FOREIGN KEY(asset_id) REFERENCES assets(id) ON DELETE CASCADE)"
        const val CREATE_INDEX_SQL = "CREATE INDEX IF NOT EXISTS idx_asset_balance_events_date " +
            "ON $TABLE(asset_id, date DESC, created_at DESC, id DESC)"

        /** The caller owns the transaction that also inserts/updates the asset itself. */
        fun insertBalanceEvent(db: SQLiteDatabase, assetId: Long, kind: Int, before: Long, after: Long) {
            check(db.inTransaction())
            val now = System.currentTimeMillis()
            val event = AssetBalanceEvent(assetId = assetId, kind = kind,
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now)), createdAt = now,
                balanceBeforeMinor = before, balanceAfterMinor = after)
            db.insertOrThrow(TABLE, null, ContentValues().apply {
                put("asset_id", event.assetId)
                put("kind", event.kind)
                put("date", event.date)
                put("created_at", event.createdAt)
                put("balance_before", event.balanceBeforeMinor)
                put("balance_after", event.balanceAfterMinor)
            })
        }
    }
}
