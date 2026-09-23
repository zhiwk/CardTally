package com.example.cardtally.util

import com.example.cardtally.model.RecurringRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringScheduleCalculatorTest {
    @Test
    fun monthlyDay31_skipsMonthsWithout31st() {
        val schedule = RecurringRecord(frequency = RecurringRecord.MONTHLY, monthlyDay = 31)
        assertEquals("2026-03-31", RecurringScheduleCalculator.normalizeDueDate("2026-02-01", schedule))
        assertEquals("2026-03-31", RecurringScheduleCalculator.nextDate("2026-01-31", schedule))
    }

    @Test
    fun yearlyLeapDay_skipsNonLeapYears() {
        val schedule = RecurringRecord(frequency = RecurringRecord.YEARLY, yearlyMonth = 2, yearlyDay = 29)
        assertEquals("2028-02-29", RecurringScheduleCalculator.normalizeDueDate("2027-02-28", schedule))
        assertEquals("2028-02-29", RecurringScheduleCalculator.nextDate("2024-02-29", schedule))
    }

    @Test
    fun initialDueDate_advancesToNextOccurrenceAfterStartBoundary() {
        val schedule = RecurringRecord(frequency = RecurringRecord.MONTHLY, monthlyDay = 10)
        assertEquals("2026-03-10", RecurringScheduleCalculator.initialDueDate("2026-02-11", schedule))
    }

    @Test
    fun intervalUsesAtLeastOneDay() {
        val schedule = RecurringRecord(frequency = RecurringRecord.INTERVAL, intervalDays = 0)
        assertEquals("2026-01-02", RecurringScheduleCalculator.nextDate("2026-01-01", schedule))
    }
}
