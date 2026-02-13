package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R

class StatisticsAdapter : RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder>() {
    
    private val items = mutableListOf<StatisticsItem>()

    fun updateData(data: Map<String, Double>) {
        items.clear()
        data.forEach { (label, amount) ->
            items.add(StatisticsItem(label, amount))
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistics, parent, false)
        return StatisticsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        val item = items[position]
        holder.textLabel.text = item.label
        holder.textAmount.text = String.format("¥%.2f", item.amount)
    }

    override fun getItemCount(): Int = items.size

    class StatisticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textLabel: TextView = itemView.findViewById(R.id.text_label)
        val textAmount: TextView = itemView.findViewById(R.id.text_amount)
    }

    private data class StatisticsItem(val label: String, val amount: Double)
}
