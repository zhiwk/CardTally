package com.example.cardtally.database

import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.Record
import com.example.cardtally.util.Money

/** Storage-bound record invariants, checked before the write transaction applies balance effects. */
internal class RecordWriteValidator(
    private val findCategory: (SQLiteDatabase, Long) -> com.example.cardtally.model.Category?,
    private val categoryHasChildren: (SQLiteDatabase, Long) -> Boolean,
    private val isAvailableAsset: (SQLiteDatabase, Long) -> Boolean
) {
    fun validate(db: SQLiteDatabase, record: Record) {
        require(record.type in 0..2) { "Unsupported record type" }
        require(record.amount > 0.0 && Money.toMinor(record.amount) != null) { "Amount must be positive and finite" }
        require(record.fee >= 0.0 && Money.toMinor(record.fee) != null) { "Fee cannot be negative" }
        if (record.type == 2) {
            require(record.assetId != null && record.destinationAssetId != null) { "Transfer assets are required" }
            require(record.assetId != record.destinationAssetId) { "Transfer assets must differ" }
        } else {
            require(record.category.isNotBlank()) { "Category is required" }
            record.categoryId?.let { categoryId ->
                val category = findCategory(db, categoryId) ?: throw IllegalArgumentException("Category missing")
                require(category.type == record.type && !categoryHasChildren(db, categoryId)) {
                    "Category must be a matching leaf"
                }
            }
        }
        listOfNotNull(record.assetId, record.destinationAssetId).forEach { assetId ->
            require(isAvailableAsset(db, assetId)) { "Asset unavailable" }
        }
    }
}
