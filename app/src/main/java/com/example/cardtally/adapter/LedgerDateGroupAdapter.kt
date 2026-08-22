package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.LedgerDisplayHelper

class LedgerDateGroupAdapter(
    private var groups: List<DateGroup>,
    private val listener: DateGroupAdapter.OnRecordActionListener,
    private val onHeaderBound: (View) -> Unit
) : RecyclerView.Adapter<LedgerDateGroupAdapter.GroupViewHolder>() {

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_GROUP = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val layout = if (viewType == TYPE_HEADER) R.layout.item_ledger_header else R.layout.item_date_group
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return if (viewType == TYPE_HEADER) HeaderViewHolder(view) else GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        if (position == 0) {
            onHeaderBound(holder.itemView)
        } else {
            holder.bind(groups[position - 1])
        }
    }

    override fun getItemViewType(position: Int): Int = if (position == 0) TYPE_HEADER else TYPE_GROUP

    override fun getItemCount(): Int = groups.size + 1

    fun updateDateGroups(newGroups: List<DateGroup>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    private inner class HeaderViewHolder(itemView: View) : GroupViewHolder(itemView)

    open inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val date = itemView.findViewById<TextView>(R.id.text_date)
        private val total = itemView.findViewById<TextView>(R.id.text_date_total)
        private val records = itemView.findViewById<RecyclerView>(R.id.recycler_day_records)

        fun bind(group: DateGroup) {
            date.text = LedgerDisplayHelper.formatDateHeader(group.date)
            val expense = group.records.filter { it.type == 0 }.sumOf { it.amount }
            val income = group.records.filter { it.type == 1 }.sumOf { it.amount }
            total.text = itemView.context.getString(
                R.string.ledger_day_income_expense,
                expense,
                income
            )
            records.layoutManager = LinearLayoutManager(itemView.context)
            records.adapter = DayRecordAdapter(group.records)
        }
    }

    private inner class DayRecordAdapter(private val records: List<Record>) :
        RecyclerView.Adapter<DateGroupAdapter.RecordViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateGroupAdapter.RecordViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
            return DateGroupAdapter.RecordViewHolder(view)
        }

        override fun onBindViewHolder(holder: DateGroupAdapter.RecordViewHolder, position: Int) {
            holder.bind(records[position], listener, false, false)
        }

        override fun getItemCount(): Int = records.size
    }
}
