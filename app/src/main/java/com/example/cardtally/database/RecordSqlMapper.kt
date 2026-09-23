package com.example.cardtally.database

import android.content.ContentValues
import android.database.Cursor
import com.example.cardtally.model.Record
import com.example.cardtally.util.Money

/** SQLite row mapping kept separate from DatabaseHelper's transactional operations. */
internal object RecordSqlMapper {
    data class Columns(
        val id: String,
        val ledgerId: String,
        val date: String,
        val amount: String,
        val category: String,
        val categoryId: String,
        val categoryNameSnapshot: String,
        val categoryPathSnapshot: String,
        val type: String,
        val description: String,
        val fee: String,
        val assetId: String,
        val destinationAssetId: String,
        val assetSource: String,
        val destinationAssetSource: String,
        val photoUri: String,
        val photoUris: String,
        val sortOrder: String,
        val photoUriSeparator: String
    )

    fun toContentValues(record: Record, ledgerId: Long, columns: Columns): ContentValues {
        val nameSnapshot = record.categoryNameSnapshot ?: record.category
        val pathSnapshot = record.categoryPathSnapshot ?: record.category
        return ContentValues().apply {
            put(columns.ledgerId, ledgerId)
            put(columns.date, record.date)
            put(columns.amount, requireNotNull(Money.toMinor(record.amount)))
            put(columns.category, record.category)
            putNullable(columns.categoryId, record.categoryId)
            put(columns.categoryNameSnapshot, nameSnapshot)
            put(columns.categoryPathSnapshot, pathSnapshot)
            put(columns.type, record.type)
            put(columns.description, record.description)
            put(columns.fee, if (record.type == 2) requireNotNull(Money.toMinor(record.fee)) else 0L)
            putNullable(columns.assetId, record.assetId)
            putNullable(columns.destinationAssetId, record.destinationAssetId)
            put(columns.assetSource, record.assetSource)
            put(columns.destinationAssetSource, record.destinationAssetSource)
            put(columns.photoUri, record.photoUri)
            put(columns.photoUris, record.photoUris.joinToString(columns.photoUriSeparator))
        }
    }

    fun fromCursor(cursor: Cursor, columns: Columns): Record = Record(
        id = cursor.long(columns.id),
        date = cursor.string(columns.date),
        amount = Money.toMajorDouble(cursor.long(columns.amount)),
        category = cursor.string(columns.category),
        categoryId = cursor.nullableLong(columns.categoryId),
        categoryNameSnapshot = cursor.nullableString(columns.categoryNameSnapshot),
        categoryPathSnapshot = cursor.nullableString(columns.categoryPathSnapshot),
        type = cursor.int(columns.type),
        description = cursor.nullableString(columns.description),
        fee = Money.toMajorDouble(cursor.long(columns.fee)),
        assetId = cursor.nullableLong(columns.assetId),
        destinationAssetId = cursor.nullableLong(columns.destinationAssetId),
        assetSource = cursor.nullableString(columns.assetSource),
        destinationAssetSource = cursor.nullableString(columns.destinationAssetSource),
        photoUri = cursor.nullableString(columns.photoUri),
        photoUris = cursor.nullableString(columns.photoUris)
            ?.split(columns.photoUriSeparator)
            ?.filter(String::isNotBlank)
            ?.ifEmpty { cursor.nullableString(columns.photoUri)?.let(::listOf) ?: emptyList() }
            ?: cursor.nullableString(columns.photoUri)?.let(::listOf)
            ?: emptyList(),
        ledgerId = cursor.nullableLong(columns.ledgerId),
        sortOrder = cursor.int(columns.sortOrder)
    )

    private fun ContentValues.putNullable(column: String, value: Long?) {
        if (value == null) putNull(column) else put(column, value)
    }

    private fun Cursor.index(column: String) = getColumnIndexOrThrow(column)
    private fun Cursor.long(column: String) = getLong(index(column))
    private fun Cursor.int(column: String) = getInt(index(column))
    private fun Cursor.string(column: String) = getString(index(column))
    private fun Cursor.nullableString(column: String): String? = getString(index(column))
    private fun Cursor.nullableLong(column: String): Long? {
        val index = index(column)
        return if (isNull(index)) null else getLong(index)
    }
}
