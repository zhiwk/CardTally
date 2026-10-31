package com.example.cardtally.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AssetBalanceEventTest {
    private fun event(kind: Int, before: Long, after: Long) = AssetBalanceEvent(
        assetId = 1, kind = kind, date = "2026-11-01", createdAt = 1,
        balanceBeforeMinor = before, balanceAfterMinor = after)

    @Test fun initializationSupportsPositiveZeroAndNegativeBalances() {
        for (amount in listOf(50_000L, 0L, -12_550L)) {
            assertEquals(amount, event(AssetBalanceEvent.INITIAL, 0, amount).deltaMinor)
        }
    }

    @Test fun adjustmentsUseSignedExactDifferenceRatherThanNewBalance() {
        assertEquals(10_000L, event(AssetBalanceEvent.ADJUSTMENT, 50_000, 60_000).deltaMinor)
        assertEquals(-20_000L, event(AssetBalanceEvent.ADJUSTMENT, 60_000, 40_000).deltaMinor)
        assertEquals(-5_001L, event(AssetBalanceEvent.ADJUSTMENT, -5_000, -10_001).deltaMinor)
    }

    @Test fun invalidKindsInitialBaselinesAndNoOpAdjustmentsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { event(2, 0, 100) }
        assertThrows(IllegalArgumentException::class.java) { event(AssetBalanceEvent.INITIAL, 100, 200) }
        assertThrows(IllegalArgumentException::class.java) { event(AssetBalanceEvent.ADJUSTMENT, 100, 100) }
    }

    @Test fun deltaOverflowCannotSilentlyWrap() {
        assertThrows(ArithmeticException::class.java) { event(AssetBalanceEvent.ADJUSTMENT, Long.MIN_VALUE, Long.MAX_VALUE) }
    }
}
