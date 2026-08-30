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
import com.example.cardtally.util.IncomeExpenseColorScheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LedgerCalendarAdapter(
    private val singleSelection: Boolean = false,
    private val compact: Boolean = false,
    private val showAmounts: Boolean = true,
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
        if (compact) {
            val cellHeight = 36.dp(parent.context)
            view.layoutParams = view.layoutParams.apply { height = cellHeight }

            view.findViewById<View>(R.id.day_selected_background).layoutParams =
                (view.findViewById<View>(R.id.day_selected_background).layoutParams as FrameLayout.LayoutParams).apply {
                    height = cellHeight
                    gravity = Gravity.CENTER
                }
            view.findViewById<View>(R.id.day_fill_left).layoutParams.height = 28.dp(parent.context)
            view.findViewById<View>(R.id.day_fill_right).layoutParams.height = 28.dp(parent.context)
            view.findViewById<TextView>(R.id.text_day_value).layoutParams =
                (view.findViewById<TextView>(R.id.text_day_value).layoutParams as FrameLayout.LayoutParams).apply {
                    gravity = Gravity.CENTER
                }
        }
        return CalendarDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
            holder.bind(items[position], onDayClicked, singleSelection, showAmounts)
    }

    override fun getItemCount(): Int = items.size

    private fun Int.dp(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    class CalendarDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.day_container)
        private val fillLeft: View = itemView.findViewById(R.id.day_fill_left)
        private val fillRight: View = itemView.findViewById(R.id.day_fill_right)
        private val selectedBackground: View = itemView.findViewById(R.id.day_selected_background)
        private val value: TextView = itemView.findViewById(R.id.text_day_value)
        private val income: TextView = itemView.findViewById(R.id.text_day_income)
        private val expense: TextView = itemView.findViewById(R.id.text_day_expense)
        private val todayDot: View = itemView.findViewById(R.id.view_today_dot)

        fun bind(
            day: LedgerCalendarDay,
            onDayClicked: (LedgerCalendarDay) -> Unit,
            singleSelection: Boolean,
            showAmounts: Boolean
        ) {
            val context = itemView.context
            if (day.isoDate == null || day.dayOfMonth == null) {
                value.text = ""
                container.isClickable = false
                value.isSelected = false
                fillLeft.isVisible = false
                fillRight.isVisible = false
                selectedBackground.isVisible = false
                todayDot.isVisible = false
                income.isVisible = false
                expense.isVisible = false
                value.setTextColor(ThemeColorHelper.resolveThemeAwareResource(context, android.R.color.transparent))
                return
            }

            value.text = day.dayOfMonth.toString()
            positionRangeFill(fillLeft, Gravity.END)
            positionRangeFill(fillRight, Gravity.START)
            // Show a complete zero-based daily accounting row for today and
            // earlier dates. Future dates remain visually empty because they
            // do not have a settled daily result yet.
            val todayIsoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val hasSettledDailyTotal = day.isoDate <= todayIsoDate
            income.isVisible = showAmounts && hasSettledDailyTotal
            expense.isVisible = showAmounts && hasSettledDailyTotal
            if (showAmounts) {
                income.text = String.format(java.util.Locale.US, "+%.2f", day.income)
                expense.text = String.format(java.util.Locale.US, "-%.2f", day.expense)
            }
            value.isSelected = day.isRangeBoundary
            // The selected range uses a flat band with the same 28dp height as
            // the circular date marker; the boundary dates remain circular.
            // The start date connects to the next cell on its right, while
            // the end date connects back to the previous cell on its left.
            // Middle dates show both halves and therefore form one band.
            fillLeft.isVisible = !singleSelection && day.isInSelectedRange && !day.isRangeEnd
            fillRight.isVisible = !singleSelection && day.isInSelectedRange && !day.isRangeStart
            selectedBackground.isVisible = singleSelection && day.isRangeBoundary
            value.background = if (singleSelection) {
                null
            } else {
                androidx.appcompat.content.res.AppCompatResources.getDrawable(
                    context,
                    R.drawable.bg_ledger_calendar_day_bubble
                )
            }
            todayDot.isVisible = false
            val selectedColor = if (day.isRangeBoundary && singleSelection) {
                ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimary)
            } else if (!day.isCurrentMonth) {
                ThemeColorHelper.resolveThemeAwareResource(context, R.color.calendar_adjacent_day)
            } else {
                ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurface)
            }
            income.setTextColor(if (day.isRangeBoundary && singleSelection) selectedColor else IncomeExpenseColorScheme.incomePrimary(context))
            expense.setTextColor(if (day.isRangeBoundary && singleSelection) selectedColor else IncomeExpenseColorScheme.expensePrimary(context))
            value.setTextColor(selectedColor)
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
