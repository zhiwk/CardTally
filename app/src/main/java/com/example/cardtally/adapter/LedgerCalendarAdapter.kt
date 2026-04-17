package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.LedgerCalendarDay
import com.example.cardtally.util.ThemeColorHelper

class LedgerCalendarAdapter(
    private val onDayClicked: (LedgerCalendarDay) -> Unit
) : RecyclerView.Adapter<LedgerCalendarAdapter.CalendarDayViewHolder>() {

    private val items = mutableListOf<LedgerCalendarDay>()

    fun submitList(days: List<LedgerCalendarDay>) {
        items.clear()
        items.addAll(days)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ledger_calendar_day, parent, false)
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        holder.bind(items[position], onDayClicked)
    }

    override fun getItemCount(): Int = items.size

    class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.day_container)
        private val fillLeft: View = itemView.findViewById(R.id.day_fill_left)
        private val fillRight: View = itemView.findViewById(R.id.day_fill_right)
        private val value: TextView = itemView.findViewById(R.id.text_day_value)
        private val todayDot: View = itemView.findViewById(R.id.view_today_dot)

        fun bind(day: LedgerCalendarDay, onDayClicked: (LedgerCalendarDay) -> Unit) {
            val context = itemView.context
            if (day.isoDate == null || day.dayOfMonth == null) {
                value.text = ""
                container.isClickable = false
                value.isSelected = false
                fillLeft.isVisible = false
                fillRight.isVisible = false
                todayDot.isVisible = false
                value.setTextColor(ThemeColorHelper.resolveThemeAwareResource(context, android.R.color.transparent))
                return
            }

            value.text = day.dayOfMonth.toString()
            value.isSelected = day.isRangeBoundary
            fillLeft.isVisible = day.isInSelectedRange && (!day.isRangeStart || day.isRangeEnd)
            fillRight.isVisible = day.isInSelectedRange && (!day.isRangeEnd || day.isRangeStart)
            todayDot.isVisible = day.isToday && !day.isRangeBoundary
            value.setTextColor(
                if (day.isRangeBoundary) {
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimary)
                } else {
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurface)
                }
            )
            container.isClickable = true
            container.setOnClickListener { onDayClicked(day) }
        }
    }
}
