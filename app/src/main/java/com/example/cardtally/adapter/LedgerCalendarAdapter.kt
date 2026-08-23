package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.LedgerCalendarDay
import com.example.cardtally.util.ThemeColorHelper

class LedgerCalendarAdapter(
    private val singleSelection: Boolean = false,
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
            holder.bind(items[position], onDayClicked, singleSelection)
    }

    override fun getItemCount(): Int = items.size

    class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.day_container)
        private val fillLeft: View = itemView.findViewById(R.id.day_fill_left)
        private val fillRight: View = itemView.findViewById(R.id.day_fill_right)
        private val value: TextView = itemView.findViewById(R.id.text_day_value)
        private val income: TextView = itemView.findViewById(R.id.text_day_income)
        private val expense: TextView = itemView.findViewById(R.id.text_day_expense)
        private val todayDot: View = itemView.findViewById(R.id.view_today_dot)

        fun bind(
            day: LedgerCalendarDay,
            onDayClicked: (LedgerCalendarDay) -> Unit,
            singleSelection: Boolean
        ) {
            val context = itemView.context
            if (day.isoDate == null || day.dayOfMonth == null) {
                value.text = ""
                container.isClickable = false
                value.isSelected = false
                fillLeft.isVisible = false
                fillRight.isVisible = false
                todayDot.isVisible = false
                income.isVisible = false
                expense.isVisible = false
                value.setTextColor(ThemeColorHelper.resolveThemeAwareResource(context, android.R.color.transparent))
                return
            }

            value.text = day.dayOfMonth.toString()
            positionRangeFill(fillLeft, Gravity.END)
            positionRangeFill(fillRight, Gravity.START)
            income.isVisible = day.income > 0.0
            expense.isVisible = day.expense > 0.0
            income.text = String.format(java.util.Locale.US, "+%.0f", day.income)
            expense.text = String.format(java.util.Locale.US, "-%.0f", day.expense)
            value.isSelected = day.isRangeBoundary
            // The selected range uses a flat band with the same 28dp height as
            // the circular date marker; the boundary dates remain circular.
            // The start date connects to the next cell on its right, while
            // the end date connects back to the previous cell on its left.
            // Middle dates show both halves and therefore form one band.
            fillLeft.isVisible = !singleSelection && day.isInSelectedRange && !day.isRangeEnd
            fillRight.isVisible = !singleSelection && day.isInSelectedRange && !day.isRangeStart
            todayDot.isVisible = false
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

        private fun positionRangeFill(fill: View, gravity: Int) {
            val applyPosition = {
                val params = fill.layoutParams as FrameLayout.LayoutParams
                params.width = itemView.width / 2
                params.gravity = gravity or Gravity.TOP
                fill.layoutParams = params
            }
            if (itemView.width > 0) applyPosition() else itemView.post { applyPosition() }
        }
    }
}
