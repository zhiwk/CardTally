package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerMonthIndexTest {
    @Test fun firstAndLastMonthMatchPagerBounds() {
        assertEquals("0001-01", LedgerMonthIndex.month(0))
        assertEquals("9999-12", LedgerMonthIndex.month(LedgerMonthIndex.COUNT - 1))
        assertEquals(0, LedgerMonthIndex.position("0001-01"))
        assertEquals(LedgerMonthIndex.COUNT - 1, LedgerMonthIndex.position("9999-12"))
    }
    @Test fun adjacentPositionsCrossYearInChronologicalOrder() {
        val december = LedgerMonthIndex.position("2026-12")
        assertEquals("2027-01", LedgerMonthIndex.month(december + 1))
        assertEquals("2026-11", LedgerMonthIndex.month(december - 1))
    }
    @Test fun everySupportedMonthHasAnUnambiguousStablePosition() {
        for (position in 0 until LedgerMonthIndex.COUNT) {
            assertEquals(position, LedgerMonthIndex.position(LedgerMonthIndex.month(position)))
        }
    }
}
