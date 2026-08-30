package com.example.cardtally.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class LedgerPeriodPreset {
    WEEK,
    MONTH,
    YEAR,
    ALL,
    CUSTOM
}

data class LedgerDateRange(
    val startDate: String?,
    val endDate: String?
)

data class LedgerCalendarDay(
    val isoDate: String?,
    val dayOfMonth: Int?,
    val isRangeBoundary: Boolean,
    val isRangeStart: Boolean,
    val isRangeEnd: Boolean,
    val isInSelectedRange: Boolean,
    val isToday: Boolean,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val isCurrentMonth: Boolean = true
)

object LedgerPeriodHelper {
    private const val ISO_PATTERN = "yyyy-MM-dd"

    fun resolveRange(
        preset: LedgerPeriodPreset,
        nowMillis: Long = System.currentTimeMillis(),
        customRange: LedgerDateRange? = null,
        allRange: LedgerDateRange? = null
    ): LedgerDateRange {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            firstDayOfWeek = Calendar.MONDAY
        }

        return when (preset) {
            LedgerPeriodPreset.WEEK -> {
                val start = calendar.clone() as Calendar
                val end = calendar.clone() as Calendar
                val delta = (start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                start.add(Calendar.DAY_OF_YEAR, -delta)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_YEAR, 6)
                LedgerDateRange(formatIsoDate(start.time), formatIsoDate(end.time))
            }

            LedgerPeriodPreset.MONTH -> {
                val start = calendar.clone() as Calendar
                val end = calendar.clone() as Calendar
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                LedgerDateRange(formatIsoDate(start.time), formatIsoDate(end.time))
            }

            LedgerPeriodPreset.YEAR -> {
                val start = calendar.clone() as Calendar
                val end = calendar.clone() as Calendar
                start.set(Calendar.MONTH, Calendar.JANUARY)
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.MONTH, Calendar.DECEMBER)
                end.set(Calendar.DAY_OF_MONTH, 31)
                LedgerDateRange(formatIsoDate(start.time), formatIsoDate(end.time))
            }

            LedgerPeriodPreset.ALL -> allRange ?: LedgerDateRange(null, null)
            LedgerPeriodPreset.CUSTOM -> customRange ?: LedgerDateRange(null, null)
        }
    }

    fun formatRangeLabel(
        range: LedgerDateRange,
        locale: Locale = Locale.getDefault()
    ): String {
        val start = range.startDate ?: return ""
        val end = range.endDate ?: return ""
        val formatter = SimpleDateFormat("MMM d, yyyy", locale)
        return "${formatter.format(parseIsoDate(start))} - ${formatter.format(parseIsoDate(end))}"
    }

    fun formatSingleDateLabel(date: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat("MMM d, yyyy", locale)
        return formatter.format(parseIsoDate(date))
    }

    fun formatMonthTitle(year: Int, month: Int, locale: Locale = Locale.getDefault()): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", locale).format(calendar.time)
    }

    fun buildMonthCells(
        year: Int,
        month: Int,
        selectedRange: LedgerDateRange,
        todayMillis: Long = System.currentTimeMillis()
    ): List<LedgerCalendarDay> {
        val firstDay = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            clearTime()
        }
        val leadingCount = (firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = 42
        val todayIso = formatIsoDate(Date(todayMillis))
        val cells = mutableListOf<LedgerCalendarDay>()

        repeat(totalCells) { index ->
            val cellCalendar = firstDay.clone() as Calendar
            cellCalendar.add(Calendar.DAY_OF_MONTH, index - leadingCount)
            val isoDate = formatIsoDate(cellCalendar.time)
            val inRange = isDateInRange(isoDate, selectedRange)
            val isStart = isoDate == selectedRange.startDate
            val isEnd = isoDate == selectedRange.endDate
            cells += LedgerCalendarDay(
                isoDate = isoDate,
                dayOfMonth = cellCalendar.get(Calendar.DAY_OF_MONTH),
                isRangeBoundary = isStart || isEnd,
                isRangeStart = isStart,
                isRangeEnd = isEnd,
                isInSelectedRange = inRange,
                isToday = isoDate == todayIso,
                isCurrentMonth = cellCalendar.get(Calendar.MONTH) == month
            )
        }
        return cells
    }

    fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        return calendar.get(Calendar.YEAR) to calendar.get(Calendar.MONTH)
    }

    fun normalizeRange(startDate: String?, endDate: String?): LedgerDateRange {
        if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) {
            return LedgerDateRange(startDate, endDate)
        }
        return if (startDate <= endDate) {
            LedgerDateRange(startDate, endDate)
        } else {
            LedgerDateRange(endDate, startDate)
        }
    }

    private fun isDateInRange(date: String, range: LedgerDateRange): Boolean {
        val start = range.startDate ?: return false
        val end = range.endDate ?: return date == start
        return date >= start && date <= end
    }

    private fun formatIsoDate(date: Date): String {
        return SimpleDateFormat(ISO_PATTERN, Locale.US).format(date)
    }

    fun parseIsoDate(value: String): Date {
        return SimpleDateFormat(ISO_PATTERN, Locale.US).parse(value)
    }

    fun formatIsoDateForExternal(date: Date): String {
        return SimpleDateFormat(ISO_PATTERN, Locale.US).format(date)
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
