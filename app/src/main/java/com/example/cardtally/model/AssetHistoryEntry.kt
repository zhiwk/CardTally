package com.example.cardtally.model

/** Keeps account balance events separate from ordinary bill models and their write/delete actions. */
sealed class AssetHistoryEntry {
    abstract val date: String

    data class Bill(val record: Record) : AssetHistoryEntry() {
        override val date: String get() = record.date
    }

    data class Balance(val event: AssetBalanceEvent) : AssetHistoryEntry() {
        override val date: String get() = event.date
    }
}
