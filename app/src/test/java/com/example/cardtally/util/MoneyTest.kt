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

    @Test
    fun parseYuan_checksExactLongBoundariesWithoutWrapping() {
        assertEquals(Long.MAX_VALUE, Money.parseYuan("92233720368547758.07"))
        assertNull(Money.parseYuan("92233720368547758.08"))
        assertEquals(Long.MIN_VALUE, Money.parseYuan("-92233720368547758.08", allowNegative = true))
        assertNull(Money.parseYuan("-92233720368547758.09", allowNegative = true))
    }

    @Test
    fun evaluateYuanExpression_rejectsAdditionAndSubtractionOverflow() {
        assertNull(Money.evaluateYuanExpression("92233720368547758.07+0.01"))
        assertNull(Money.evaluateYuanExpression("-92233720368547758.07-0.02", allowNegative = true))
        assertEquals(0L, Money.evaluateYuanExpression("92233720368547758.07-92233720368547758.07"))
    }

    @Test
    fun evaluateYuanExpression_rejectsIncompleteOrUnsupportedSyntax() {
        listOf("", "1+", "1--2", "1*2", "1/2", "1.001", "NaN", "Infinity").forEach {
            assertNull(it, Money.evaluateYuanExpression(it))
        }
        assertEquals(125L, Money.evaluateYuanExpression(" 1.00 + 0.50 - 0.25 "))
    }

    @Test
    fun toMinor_rejectsNonFiniteAndSubFenValues() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.001, Double.MAX_VALUE).forEach {
            assertNull(Money.toMinor(it))
        }
        assertEquals(1L, Money.toMinor(0.01))
        assertEquals(-1L, Money.toMinor(-0.01))
    }

    @Test
    fun formatYuan_roundsLegacyDoubleHalfUpAndHandlesNonFiniteValues() {
        assertEquals("1.01", Money.formatYuan(1.005))
        assertEquals("-1.01", Money.formatYuan(-1.005))
        assertEquals("0.00", Money.formatYuan(Double.NaN))
        assertEquals("0.00", Money.formatYuan(Double.POSITIVE_INFINITY))
    }
}
