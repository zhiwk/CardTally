package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.ThemeColorHelper

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
        val context = holder.itemView.context
        holder.textLabel.text = item.label
        holder.textAmount.text = String.format("¥%.2f", item.amount)

        val iconRes = when (item.label) {
            "工资", "奖金", "投资", "兼职" -> R.drawable.ic_asset
            "餐饮" -> R.drawable.ic_category_food
            "交通" -> R.drawable.ic_category_transport
            "购物" -> R.drawable.ic_category_shopping
            "娱乐" -> R.drawable.ic_category_entertainment
            "医疗" -> R.drawable.ic_category_medical
            "教育" -> R.drawable.ic_category_education
            "住房" -> R.drawable.ic_category_housing
            else -> R.drawable.ic_book
        }
        val iconBackground = if (item.amount >= 0) {
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSecondaryContainer)
        } else {
            ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_container_low)
        }
        val iconTint = if (item.amount >= 0) {
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSecondary)
        } else {
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        }
        val amountColor = if (item.amount >= 0) {
            ThemeColorHelper.resolveThemeAwareResource(context, R.color.income_primary)
        } else {
            ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        }

        holder.imageIcon.setImageResource(iconRes)
        holder.imageIcon.setColorFilter(iconTint)
        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(
            iconBackground
        )
        holder.textMeta.text = if (item.amount >= 0) "POSITIVE FLOW" else "OUTGOING FLOW"
        holder.textAmount.setTextColor(amountColor)
    }

    override fun getItemCount(): Int = items.size

    class StatisticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconContainer: View = itemView.findViewById(R.id.icon_container)
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
        val textLabel: TextView = itemView.findViewById(R.id.text_label)
        val textMeta: TextView = itemView.findViewById(R.id.text_meta)
        val textAmount: TextView = itemView.findViewById(R.id.text_amount)
    }

    private data class StatisticsItem(val label: String, val amount: Double)
}
