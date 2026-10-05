package com.example.cardtally.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordEntryValidatorTest {
    private fun input(
        type: Int = 0,
        date: String = "2026-09-23",
        amount: String = "12.50",
        fee: String = "",
        source: Long? = 1L,
        destination: Long? = 2L,
        category: Boolean = true,
        leaf: Boolean = true
    ) = RecordEntryValidator.Input(type, date, amount, fee, source, destination, category, leaf)

    private val decimalParser: (String) -> Double? = { it.toDoubleOrNull() }

    @Test
    fun transfer_requiresDistinctSourceAndDestinationAssets() {
        assertEquals(
            RecordEntryValidator.Error.TRANSFER_ASSETS_REQUIRED,
            (RecordEntryValidator.validate(input(type = 2, source = null), decimalParser) as RecordEntryValidator.Result.Invalid).error
        )
        assertEquals(
            RecordEntryValidator.Error.TRANSFER_ASSETS_MUST_DIFFER,
            (RecordEntryValidator.validate(input(type = 2, source = 1, destination = 1), decimalParser) as RecordEntryValidator.Result.Invalid).error
        )
    }

    @Test
    fun feeIsOptionalAndOnlyParsedForTransfers() {
        val validTransfer = RecordEntryValidator.validate(input(type = 2, fee = ""), decimalParser)
        assertEquals(0.0, (validTransfer as RecordEntryValidator.Result.Valid).value.fee, 0.0)
        assertEquals(
            RecordEntryValidator.Error.FEE_INVALID,
            (RecordEntryValidator.validate(input(type = 2, fee = "-1"), decimalParser) as RecordEntryValidator.Result.Invalid).error
        )
        val nonTransfer = RecordEntryValidator.validate(input(type = 0, fee = "not a number"), decimalParser)
        assertEquals(0.0, (nonTransfer as RecordEntryValidator.Result.Valid).value.fee, 0.0)
    }

    @Test
    fun nonTransferRequiresLeafCategory() {
        listOf(0, 1).forEach { type ->
            assertTrue(RecordEntryValidator.validate(input(type = type, source = null, destination = null), decimalParser)
                is RecordEntryValidator.Result.Valid)
        }
        assertEquals(
            RecordEntryValidator.Error.CATEGORY_MUST_BE_LEAF,
            (RecordEntryValidator.validate(input(leaf = false), decimalParser) as RecordEntryValidator.Result.Invalid).error
        )
        assertTrue(RecordEntryValidator.validate(input(type = 2, category = true), decimalParser) is RecordEntryValidator.Result.Valid)
    }

    @Test
    fun dateAmountAndCategoryErrorsFollowExistingValidationOrder() {
        assertEquals(RecordEntryValidator.Error.DATE_REQUIRED,
            (RecordEntryValidator.validate(input(date = ""), decimalParser) as RecordEntryValidator.Result.Invalid).error)
        assertEquals(RecordEntryValidator.Error.AMOUNT_REQUIRED,
            (RecordEntryValidator.validate(input(amount = ""), decimalParser) as RecordEntryValidator.Result.Invalid).error)
        assertEquals(RecordEntryValidator.Error.AMOUNT_ZERO,
            (RecordEntryValidator.validate(input(amount = "0"), decimalParser) as RecordEntryValidator.Result.Invalid).error)
        assertEquals(RecordEntryValidator.Error.CATEGORY_REQUIRED,
            (RecordEntryValidator.validate(input(category = false), decimalParser) as RecordEntryValidator.Result.Invalid).error)
    }
}
