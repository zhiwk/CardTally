package com.example.cardtally.model

data class Asset(
    var id: Long = 0,
    var ledgerId: Long = 0,
    var name: String = "",
    /** UI/domain balance in yuan; SQLite persists an exact integer-fen representation. */
    var amount: Double = 0.0,
    var type: Int = 0,
    var categoryLabel: String = "",
    var categoryIconName: String = "",
    var isArchived: Boolean = false,
    var isPinned: Boolean = false,
    var includeInTotal: Boolean = true,
    var sortOrder: Int = 0
)
