package com.example.cardtally.model

/** An account-only balance change, never an income/expense record. Amounts are exact fen. */
data class AssetBalanceEvent(
    val id: Long = 0,
    val assetId: Long,
    val kind: Int,
    val date: String,
    val createdAt: Long,
    val balanceBeforeMinor: Long,
    val balanceAfterMinor: Long
) {
    val deltaMinor: Long = Math.subtractExact(balanceAfterMinor, balanceBeforeMinor)

    init {
        require(assetId > 0 && createdAt >= 0)
        require(kind == INITIAL || kind == ADJUSTMENT)
        require(if (kind == INITIAL) balanceBeforeMinor == 0L else deltaMinor != 0L)
    }

    companion object {
        const val INITIAL = 0
        const val ADJUSTMENT = 1
    }
}
