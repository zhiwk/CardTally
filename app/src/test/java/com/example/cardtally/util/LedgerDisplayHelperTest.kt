package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LedgerDisplayHelperTest {

    @Test
    fun formatDateHeader_includesYearAndEditorialWeekday() {
        val result = LedgerDisplayHelper.formatDateHeader("2026-04-14", Locale.US)

        assertEquals("Tuesday, 14th, 2026", result)
    }

    @Test
    fun formatEntriesMeta_usesUppercaseCountCopy() {
        val result = LedgerDisplayHelper.formatEntriesMeta(12, Locale.US)

        assertEquals("12 ENTRIES", result)
    }

    @Test
    fun formatDayOnlyLabel_stripsMonthForChineseAxis() {
        val result = LedgerDisplayHelper.formatDayOnlyLabel("2026-04-14", Locale.CHINA)

        assertEquals("14", result)
    }

    @Test
    fun formatDayOnlyLabel_stripsMonthForEnglishAxis() {
        val result = LedgerDisplayHelper.formatDayOnlyLabel("2026-04-14", Locale.US)

        assertEquals("14", result)
    }
}
