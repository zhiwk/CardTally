package com.example.cardtally.state

/** Validation order and rules shared by the record-entry screen and unit tests. */
object RecordEntryValidator {
    enum class Error {
        DATE_REQUIRED,
        AMOUNT_REQUIRED,
        AMOUNT_INVALID,
        AMOUNT_ZERO,
        FEE_INVALID,
        TRANSFER_ASSETS_REQUIRED,
        TRANSFER_ASSETS_MUST_DIFFER,
        CATEGORY_REQUIRED,
        CATEGORY_MUST_BE_LEAF
    }

    data class Input(
        val type: Int,
        val date: String,
        val amount: String,
        val fee: String,
        val sourceAssetId: Long?,
        val destinationAssetId: Long?,
        val hasCategory: Boolean,
        val categoryIsLeaf: Boolean
    )

    data class Validated(val amount: Double, val fee: Double)

    sealed class Result {
        data class Valid(val value: Validated) : Result()
        data class Invalid(val error: Error) : Result()
    }

    fun validate(input: Input, parseAmount: (String) -> Double?): Result {
        if (input.date.isEmpty()) return Result.Invalid(Error.DATE_REQUIRED)
        if (input.amount.isEmpty()) return Result.Invalid(Error.AMOUNT_REQUIRED)
        val amount = parseAmount(input.amount) ?: return Result.Invalid(Error.AMOUNT_INVALID)
        if (amount == 0.0) return Result.Invalid(Error.AMOUNT_ZERO)
        val fee = if (input.type == 2 && input.fee.isNotEmpty()) {
            parseAmount(input.fee)?.takeIf { it >= 0.0 } ?: return Result.Invalid(Error.FEE_INVALID)
        } else 0.0
        if (input.type == 2) {
            if (input.sourceAssetId == null || input.destinationAssetId == null) {
                return Result.Invalid(Error.TRANSFER_ASSETS_REQUIRED)
            }
            if (input.sourceAssetId == input.destinationAssetId) {
                return Result.Invalid(Error.TRANSFER_ASSETS_MUST_DIFFER)
            }
        }
        if (!input.hasCategory) return Result.Invalid(Error.CATEGORY_REQUIRED)
        if (input.type != 2 && !input.categoryIsLeaf) return Result.Invalid(Error.CATEGORY_MUST_BE_LEAF)
        return Result.Valid(Validated(amount, fee))
    }
}
