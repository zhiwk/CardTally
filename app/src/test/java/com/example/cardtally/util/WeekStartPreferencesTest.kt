package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeekStartPreferencesTest {
    private fun time(date: String) = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!.time

    @Test fun onlySundayAndMondayAreAccepted() {
        assertEquals(Calendar.SUNDAY, WeekStartPreferences.normalize(Calendar.SUNDAY))
        assertEquals(Calendar.MONDAY, WeekStartPreferences.normalize(Calendar.MONDAY))
        assertEquals(Calendar.MONDAY, WeekStartPreferences.normalize(99))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), WeekStartPreferences.dayOrder(Calendar.SUNDAY))
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 1), WeekStartPreferences.dayOrder(Calendar.MONDAY))
    }

    @Test fun sundayBelongsToDifferentWeekDependingOnPreference() {
        assertEquals(LedgerDateRange("2026-10-18", "2026-10-24"),
            LedgerPeriodHelper.resolveRange(LedgerPeriodPreset.WEEK, time("2026-10-18"), weekStart = Calendar.SUNDAY))
        assertEquals(LedgerDateRange("2026-10-12", "2026-10-18"),
            LedgerPeriodHelper.resolveRange(LedgerPeriodPreset.WEEK, time("2026-10-18"), weekStart = Calendar.MONDAY))
    }

    @Test fun weekRangesCanCrossYearBoundaries() {
        assertEquals(LedgerDateRange("2026-12-27", "2027-01-02"),
            LedgerPeriodHelper.resolveRange(LedgerPeriodPreset.WEEK, time("2027-01-01"), weekStart = Calendar.SUNDAY))
        assertEquals(LedgerDateRange("2026-12-28", "2027-01-03"),
            LedgerPeriodHelper.resolveRange(LedgerPeriodPreset.WEEK, time("2027-01-01"), weekStart = Calendar.MONDAY))
    }

    @Test fun calendarLeadingCellsMatchWeekdayHeadersAndStillHaveSixRows() {
        val range = LedgerDateRange("2026-11-01", "2026-11-01")
        val sunday = LedgerPeriodHelper.buildMonthCells(2026, Calendar.NOVEMBER, range, weekStart = Calendar.SUNDAY)
        val monday = LedgerPeriodHelper.buildMonthCells(2026, Calendar.NOVEMBER, range, weekStart = Calendar.MONDAY)
        assertEquals("2026-11-01", sunday.first().isoDate)
        assertEquals("2026-10-26", monday.first().isoDate)
        assertEquals(42, sunday.size)
        assertEquals(42, monday.size)
        assertEquals(1, sunday.count { it.isRangeStart })
        assertEquals(1, monday.count { it.isRangeStart })
    }
}
