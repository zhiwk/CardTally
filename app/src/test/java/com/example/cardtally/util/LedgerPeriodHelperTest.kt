package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LedgerPeriodHelperTest {

    @Test
    fun resolveRange_monthUsesFullMonthBoundaries() {
        val range = LedgerPeriodHelper.resolveRange(
            preset = LedgerPeriodPreset.MONTH,
            nowMillis = 1775001600000L
        )

        assertEquals("2026-04-01", range.startDate)
        assertEquals("2026-04-30", range.endDate)
    }

    @Test
    fun resolveRange_weekStartsOnMondayAndEndsOnSunday() {
        val range = LedgerPeriodHelper.resolveRange(
            preset = LedgerPeriodPreset.WEEK,
            nowMillis = 1775174400000L
        )

        assertEquals("2026-03-30", range.startDate)
        assertEquals("2026-04-05", range.endDate)
    }

    @Test
    fun resolveRange_customPreservesExplicitRange() {
        val range = LedgerPeriodHelper.resolveRange(
            preset = LedgerPeriodPreset.CUSTOM,
            customRange = LedgerDateRange("2026-04-12", "2026-04-27")
        )

        assertEquals("2026-04-12", range.startDate)
        assertEquals("2026-04-27", range.endDate)
    }

    @Test
    fun formatRangeLabel_usesEditorialShortDateFormat() {
        val label = LedgerPeriodHelper.formatRangeLabel(
            LedgerDateRange("2026-04-01", "2026-04-30"),
            locale = Locale.US
        )

        assertEquals("Apr 1, 2026 - Apr 30, 2026", label)
    }

    @Test
    fun buildMonthCells_marksBoundariesAndInRangeDates() {
        val cells = LedgerPeriodHelper.buildMonthCells(
            year = 2026,
            month = 3,
            selectedRange = LedgerDateRange("2026-04-12", "2026-04-15"),
            todayMillis = 1775001600000L
        )

        assertEquals(42, cells.size)

        val startCell = cells.first { it.isoDate == "2026-04-12" }
        val middleCell = cells.first { it.isoDate == "2026-04-13" }
        val endCell = cells.first { it.isoDate == "2026-04-15" }
        val todayCell = cells.first { it.isoDate == "2026-04-01" }

        assertTrue(startCell.isRangeBoundary)
        assertTrue(startCell.isRangeStart)
        assertTrue(startCell.isInSelectedRange)
        assertTrue(middleCell.isInSelectedRange)
        assertFalse(middleCell.isRangeBoundary)
        assertTrue(endCell.isRangeBoundary)
        assertTrue(endCell.isRangeEnd)
        assertTrue(todayCell.isToday)
        assertFalse(cells.first { it.isoDate == "2026-03-30" }.isCurrentMonth)
        assertFalse(cells.first { it.isoDate == "2026-05-10" }.isCurrentMonth)
    }
}
