package com.example.cardtally.model

data class Record(
    var id: Long = 0,
    var date: String = "",
    /** UI/domain amount in yuan; SQLite persists an exact integer-fen representation. */
    var amount: Double = 0.0,
    var category: String = "",
    var categoryId: Long? = null,
    var categoryNameSnapshot: String? = null,
    var categoryPathSnapshot: String? = null,
    var type: Int = 0,
    var description: String? = null,
    /** Transfer fee. Only meaningful for transfer records; always 0 for income/expense. */
    var fee: Double = 0.0,
    var assetId: Long? = null,
    var destinationAssetId: Long? = null,
    var assetSource: String? = null,
    var destinationAssetSource: String? = null,
    var photoUri: String? = null,
    var photoUris: List<String> = emptyList(),
    /** Ledger this record belongs to. Only rendered by the asset-detail history. */
    var ledgerId: Long? = null,
    /** Ledger name for [ledgerId]; filled by callers that show provenance. */
    var ledgerName: String? = null,
    var sortOrder: Int = 0
)
