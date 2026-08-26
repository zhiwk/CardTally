package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.MaterialSymbolCatalog

class StatisticsAdapter(
    private val onCategoryClick: (String) -> Unit = {}
) : RecyclerView.Adapter<StatisticsAdapter.StatisticsViewHolder>() {

    private val items = mutableListOf<StatisticsItem>()
    private var totalAmount = 0.0

    fun updateData(
        data: Map<String, Double>,
        entryCounts: Map<String, Int> = emptyMap(),
        categoryIcons: Map<String, String> = emptyMap()
    ) {
        items.clear()
        totalAmount = data.values.sumOf { kotlin.math.abs(it) }
        data.entries
            .sortedByDescending { (_, amount) -> kotlin.math.abs(amount) }
            .forEach { (label, amount) ->
                val normalizedLabel = label.substringAfter(": ", label).substringAfter("· ", label)
                items.add(
                    StatisticsItem(
                        label,
                        amount,
                        entryCounts[normalizedLabel] ?: 0,
                        categoryIcons[normalizedLabel]
                    )
                )
            }
        notifyDataSetChanged()
    }

    private fun resolveIconResource(categoryName: String, iconName: String?): Int {
        val resourceId = iconName?.let(MaterialSymbolCatalog::resourceId) ?: 0
        if (resourceId != 0) return resourceId
        return when (categoryName) {
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistics, parent, false)
        return StatisticsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val normalizedLabel = item.label.substringAfter("· ", item.label).substringAfter(": ", item.label)
        holder.textLabel.text = normalizedLabel
        holder.textAmount.text = String.format("¥%.2f", kotlin.math.abs(item.amount))

        holder.itemView.setOnClickListener { onCategoryClick(normalizedLabel) }

        val iconRes = resolveIconResource(normalizedLabel, item.iconName)
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
        val percentage = if (totalAmount == 0.0) 0.0 else kotlin.math.abs(item.amount) / totalAmount * 100.0
        holder.textMeta.text = String.format("%.2f%%", percentage)
        holder.textAmountMeta.visibility = View.GONE
        holder.progressBar.progress = percentage.coerceIn(0.0, 100.0).toInt()
        holder.progressBar.progressTintList = ColorStateList.valueOf(amountColor)
        holder.textAmount.setTextColor(amountColor)
    }

    override fun getItemCount(): Int = items.size

    class StatisticsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconContainer: View = itemView.findViewById(R.id.icon_container)
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
        val textLabel: TextView = itemView.findViewById(R.id.text_label)
        val textMeta: TextView = itemView.findViewById(R.id.text_meta)
        val textAmount: TextView = itemView.findViewById(R.id.text_amount)
        val textAmountMeta: TextView = itemView.findViewById(R.id.text_amount_meta)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
    }

    private data class StatisticsItem(
        val label: String,
        val amount: Double,
        val entryCount: Int,
        val iconName: String?
    )
}
