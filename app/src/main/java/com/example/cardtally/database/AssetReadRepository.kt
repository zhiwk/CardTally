package com.example.cardtally.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Asset
import com.example.cardtally.util.Money

/** Read/mapping boundary for active and archived asset rows. */
internal class AssetReadRepository(
    private val readableDatabase: () -> SQLiteDatabase,
    private val activeAssetScope: () -> String,
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

    fun getActive(ledgerId: Long?): List<Asset> {
        val selection = if (ledgerId == null) {
            "${activeAssetScope()} AND ${columns.archived} = 0"
        } else {
            "${columns.ledgerId} = ? AND ${columns.archived} = 0"
        }
        val order = "${columns.sortOrder}, ${columns.id}"
        return query(
            "SELECT * FROM ${columns.table} WHERE $selection ORDER BY $order",
            ledgerId?.let { arrayOf(it.toString()) }
        )
    }

    fun getArchived(): List<Asset> = query(
        "SELECT * FROM ${columns.table} WHERE ${activeAssetScope()} AND ${columns.archived} = 1 " +
            "ORDER BY ${columns.name}",
        null
    )

    private fun query(sql: String, args: Array<String>?): List<Asset> =
        readableDatabase().rawQuery(sql, args).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toAsset()) }
        }

    private fun Cursor.toAsset() = Asset(
        id = getLong(column(columns.id)),
        ledgerId = getLong(column(columns.ledgerId)),
        name = getString(column(columns.name)),
        amount = Money.toMajorDouble(getLong(column(columns.amount))),
        type = getInt(column(columns.type)),
        categoryLabel = getString(column(columns.categoryLabel)),
        categoryIconName = getString(column(columns.categoryIcon)),
        isArchived = getInt(column(columns.archived)) == 1,
        isPinned = getInt(column(columns.pinned)) == 1,
        includeInTotal = getInt(column(columns.includeInTotal)) == 1,
        sortOrder = getInt(column(columns.sortOrder))
    )

    private fun Cursor.column(name: String) = getColumnIndexOrThrow(name)
}
