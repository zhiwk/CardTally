package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerScrollTailTest {
    @Test fun floatingModeKeepsTheExistingXmlTail() {
        assertEquals(112, LedgerScrollTail.resolve(112, 76, 15, false, 32))
    }

    @Test fun largeFloatingNavigationStillGetsItsOriginalExtraClearance() {
        assertEquals(175, LedgerScrollTail.resolve(112, 160, 15, false, 32))
    }

    @Test fun dockedModeUsesCompactTailWithoutReservingTheBarAgain() {
        assertEquals(32, LedgerScrollTail.resolve(112, 0, 15, true, 32))
        assertEquals(32, LedgerScrollTail.resolve(112, 76, 15, true, 32))
    }

    @Test fun repeatedModeChangesRestoreTheBaselineAndNeverAccumulatePadding() {
        val actual = listOf(false, true, true, false, true, false).map { docked ->
            LedgerScrollTail.resolve(112, if (docked) 0 else 76, 15, docked, 32)
        }
        assertEquals(listOf(112, 32, 32, 112, 32, 112), actual)
        assertEquals(112, LedgerScrollTail.resolve(112, 0, 15, false, 32))
    }
}
