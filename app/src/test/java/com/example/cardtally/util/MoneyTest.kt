package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun evaluateYuanExpression_keepsDecimalAccountingExact() {
        assertEquals(30L, Money.evaluateYuanExpression("0.10+0.20"))
        assertEquals(1L, Money.evaluateYuanExpression("1.00-0.99"))
    }

    @Test
    fun parseYuan_requiresAtMostTwoDecimalPlaces() {
        assertEquals(122L, Money.parseYuan("1.22"))
        assertNull(Money.parseYuan("1.234"))
        assertNull(Money.parseYuan("-1.00"))
        assertEquals(-100L, Money.parseYuan("-1.00", allowNegative = true))
    }

    @Test
    fun evaluateYuanExpression_supportsNegativeAssetBalancesOnlyWhenEnabled() {
        assertNull(Money.evaluateYuanExpression("-100.00"))
        assertEquals(-10000L, Money.evaluateYuanExpression("-100.00", allowNegative = true))
        assertEquals(-50L, Money.evaluateYuanExpression("100.00-100.50", allowNegative = true))
    }

    @Test
    fun formatYuan_keepsTheUiInYuan() {
        assertEquals("1.22", Money.formatYuan(122L))
        assertEquals("-0.01", Money.formatYuan(-1L))
    }

    @Test
    fun formatYuan_doubleDoesNotTurnFloatingPointNoiseIntoZero() {
        assertEquals("16340.53", Money.formatYuan(20873.58 - 4533.05))
    }
}
