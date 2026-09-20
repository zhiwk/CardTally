package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.RecurringRecord
import com.example.cardtally.util.IncomeExpenseColorScheme
import com.example.cardtally.util.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecurringRecordAdapter(
    private var items: List<RecurringRecord>,
    private val onClick: (RecurringRecord) -> Unit,
    private val onEnabledChanged: (RecurringRecord, Boolean) -> Unit,
    private val ledgerNameFor: (Long) -> String = { "" }
) : RecyclerView.Adapter<RecurringRecordAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_recurring_record, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    fun updateItems(newItems: List<RecurringRecord>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val status: View = view.findViewById(R.id.view_recurring_status)
        private val name: TextView = view.findViewById(R.id.text_recurring_name)
        private val meta: TextView = view.findViewById(R.id.text_recurring_meta)
        private val amount: TextView = view.findViewById(R.id.text_recurring_amount)

        fun bind(item: RecurringRecord) {
            name.text = item.name
            val type = if (item.type == 0) "支出" else "收入"
            val cycle = when (item.frequency) {
                RecurringRecord.WEEKLY -> "每周 · 周${weeklyDayLabel(item.weeklyDay ?: isoDay(item.startDate))}"
                RecurringRecord.MONTHLY -> if (item.monthlyDay == 0) "每月月末" else "每月${item.monthlyDay ?: ""}日"
                RecurringRecord.YEARLY -> "每年${item.yearlyMonth ?: ""}月${item.yearlyDay ?: ""}日"
                RecurringRecord.INTERVAL -> "每${item.intervalDays ?: 1}天"
                else -> "每天"
            }
            val ledger = ledgerNameFor(item.ledgerId)
            meta.text = listOfNotNull(ledger.takeIf { it.isNotBlank() }, type, cycle, "下次 ${item.nextDueDate}").joinToString(" · ")
            amount.text = "¥${Money.formatYuan(item.amountMinor)}"
            amount.setTextColor(if (item.type == 0) IncomeExpenseColorScheme.expensePrimary(itemView.context) else IncomeExpenseColorScheme.incomePrimary(itemView.context))
            status.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    itemView.context,
                    if (item.enabled) R.color.success_primary else R.color.error_primary
                )
            )
            status.contentDescription = itemView.context.getString(
                if (item.enabled) R.string.recurring_status_running else R.string.recurring_status_stopped
            )
            itemView.alpha = 1f
            itemView.setOnClickListener { onClick(item) }
            itemView.setOnLongClickListener {
                onEnabledChanged(item, !item.enabled)
                true
            }
        }

        private fun weeklyDayLabel(day: Int): String =
            listOf("一", "二", "三", "四", "五", "六", "日")[day.coerceIn(1, 7) - 1]

        private fun isoDay(date: String): Int = runCatching {
            val calendar = Calendar.getInstance()
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)!!
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1
        }.getOrDefault(1)
    }
}
