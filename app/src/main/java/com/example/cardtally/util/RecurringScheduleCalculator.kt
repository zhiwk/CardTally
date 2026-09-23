package com.example.cardtally.util

import com.example.cardtally.model.RecurringRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Pure calendar rules for recurring record due dates. */
object RecurringScheduleCalculator {
    fun initialDueDate(startDate: String, recurring: RecurringRecord): String {
        val calendar = parse(startDate)
        when (recurring.frequency) {
            RecurringRecord.WEEKLY -> {
                val current = isoDay(calendar)
                val target = recurring.weeklyDay ?: current
                calendar.add(Calendar.DAY_OF_YEAR, (target - current + 7) % 7)
            }
            RecurringRecord.MONTHLY -> {
                val target = recurring.monthlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                if (target == 0) {
                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                } else {
                    do {
                        val max = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                        if (target <= max) {
                            calendar.set(Calendar.DAY_OF_MONTH, target)
                            if (!calendar.time.before(parse(startDate).time)) break
                        }
                        calendar.add(Calendar.MONTH, 1)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                    } while (true)
                }
            }
            RecurringRecord.YEARLY -> {
                val month = (recurring.yearlyMonth ?: calendar.get(Calendar.MONTH) + 1) - 1
                val day = recurring.yearlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.MONTH, month)
                while (day > calendar.getActualMaximum(Calendar.DAY_OF_MONTH) || calendar.time.before(parse(startDate).time)) {
                    calendar.add(Calendar.YEAR, 1)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                }
                calendar.set(Calendar.DAY_OF_MONTH, day)
            }
            RecurringRecord.INTERVAL -> Unit
        }
        return format(calendar)
    }

    fun normalizeDueDate(date: String, recurring: RecurringRecord): String {
        val calendar = parse(date)
        var attempts = 0
        while (!matches(calendar, recurring) && attempts < 4000) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            attempts++
        }
        return format(calendar)
    }

    fun nextDate(date: String, recurring: RecurringRecord): String {
        val calendar = parse(date)
        when (recurring.frequency) {
            RecurringRecord.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            RecurringRecord.MONTHLY -> {
                val target = recurring.monthlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                do {
                    calendar.add(Calendar.MONTH, 1)
                    val max = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    if (target <= max) {
                        calendar.set(Calendar.DAY_OF_MONTH, target)
                        break
                    }
                } while (true)
            }
            RecurringRecord.YEARLY -> {
                val month = (recurring.yearlyMonth ?: calendar.get(Calendar.MONTH) + 1) - 1
                val day = recurring.yearlyDay ?: calendar.get(Calendar.DAY_OF_MONTH)
                do {
                    calendar.add(Calendar.YEAR, 1)
                    calendar.set(Calendar.MONTH, month)
                    val max = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    if (day <= max) {
                        calendar.set(Calendar.DAY_OF_MONTH, day)
                        break
                    }
                } while (true)
            }
            RecurringRecord.INTERVAL -> calendar.add(Calendar.DAY_OF_YEAR, (recurring.intervalDays ?: 1).coerceAtLeast(1))
            else -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return format(calendar)
    }

    private fun matches(calendar: Calendar, recurring: RecurringRecord): Boolean = when (recurring.frequency) {
        RecurringRecord.WEEKLY -> recurring.weeklyDay == null || isoDay(calendar) == recurring.weeklyDay
        RecurringRecord.MONTHLY -> {
            val day = recurring.monthlyDay
            day == null || if (day == 0) calendar.get(Calendar.DAY_OF_MONTH) == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            else calendar.get(Calendar.DAY_OF_MONTH) == day
        }
        RecurringRecord.YEARLY -> {
            (recurring.yearlyMonth == null || calendar.get(Calendar.MONTH) + 1 == recurring.yearlyMonth) &&
                (recurring.yearlyDay == null || calendar.get(Calendar.DAY_OF_MONTH) == recurring.yearlyDay)
        }
        else -> true
    }

    private fun isoDay(calendar: Calendar): Int =
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1

    private fun parse(date: String): Calendar {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val parsed = requireNotNull(parser.parse(date)) { "Invalid date: $date" }
        return Calendar.getInstance().apply { time = parsed }
    }

    private fun format(calendar: Calendar): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
}
