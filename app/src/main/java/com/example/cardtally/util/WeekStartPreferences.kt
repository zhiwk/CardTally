package com.example.cardtally.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.cardtally.R
import java.util.Calendar

object WeekStartPreferences {
    private const val KEY = "week_start_day"
    fun normalize(day: Int): Int = if (day == Calendar.SUNDAY) Calendar.SUNDAY else Calendar.MONDAY
    fun get(context: Context): Int = normalize(context.getSharedPreferences("cardtally_preferences", Context.MODE_PRIVATE)
        .getInt(KEY, Calendar.MONDAY))
    fun save(context: Context, day: Int) {
        context.getSharedPreferences("cardtally_preferences", Context.MODE_PRIVATE).edit()
            .putInt(KEY, normalize(day)).apply()
    }
    fun dayOrder(firstDay: Int): List<Int> = (0..6).map { (normalize(firstDay) - 1 + it) % 7 + 1 }

    fun bindHeader(root: View) {
        val header = root.findViewById<ViewGroup>(R.id.weekday_header)
        val labels = listOf(R.string.weekday_sunday_short, R.string.weekday_monday_short,
            R.string.weekday_tuesday_short, R.string.weekday_wednesday_short, R.string.weekday_thursday_short,
            R.string.weekday_friday_short, R.string.weekday_saturday_short)
        dayOrder(get(root.context)).forEachIndexed { index, day ->
            (header.getChildAt(index) as TextView).setText(labels[day - 1])
        }
    }
}
