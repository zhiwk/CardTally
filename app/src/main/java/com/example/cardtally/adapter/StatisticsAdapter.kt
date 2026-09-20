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
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.IncomeExpenseColorScheme
import com.example.cardtally.util.normalizeStatisticsCategoryLabel
import com.example.cardtally.util.Money

class StatisticsAdapter(
    private val onCategoryClick: (String) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SECONDARY = 0
        private const val VIEW_TYPE_PRIMARY_PARENT = 1
        private const val VIEW_TYPE_PRIMARY_CHILD = 2
    }

    enum class ItemType {
        SECONDARY,
        PRIMARY_PARENT,
        PRIMARY_CHILD
    }

    data class StatisticsAdapterItem(
        val type: ItemType,
        val label: String,
        val amount: Double,
        val entryCount: Int = 0,
        val iconName: String? = null,
        val parentLabel: String? = null,
        val children: List<StatisticsAdapterItem> = emptyList(),
        val isExpanded: Boolean = false
    )

    private val displayItems = mutableListOf<StatisticsAdapterItem>()
    private val rawParentItems = mutableListOf<StatisticsAdapterItem>()
    private val rawFlatItems = mutableListOf<StatisticsAdapterItem>()
    private val expandedParentLabels = mutableSetOf<String>()
    private var isTreeMode = false
    private var overallTotalAmount = 0.0

    fun updateFlatData(
        data: Map<String, Double>,
        entryCounts: Map<String, Int> = emptyMap(),
        categoryIcons: Map<String, String> = emptyMap()
    ) {
        isTreeMode = false
        rawFlatItems.clear()
        overallTotalAmount = data.values.sumOf { kotlin.math.abs(it) }
        data.entries
            .sortedByDescending { (_, amount) -> kotlin.math.abs(amount) }
            .forEach { (label, amount) ->
                val normalizedLabel = normalizeStatisticsCategoryLabel(label)
                rawFlatItems.add(
                    StatisticsAdapterItem(
                        type = ItemType.SECONDARY,
                        label = label,
                        amount = amount,
                        entryCount = entryCounts[normalizedLabel] ?: 0,
                        iconName = categoryIcons[normalizedLabel]
                    )
                )
            }
        rebuildDisplayList()
    }

    fun updateTreeData(
        parentItems: List<StatisticsAdapterItem>,
        overallTotal: Double
    ) {
        isTreeMode = true
        rawParentItems.clear()
        rawParentItems.addAll(parentItems)
        overallTotalAmount = overallTotal
        rebuildDisplayList()
    }

    private fun rebuildDisplayList() {
        displayItems.clear()
        if (isTreeMode) {
            rawParentItems.forEach { parent ->
                val isExpanded = expandedParentLabels.contains(parent.label)
                displayItems.add(parent.copy(isExpanded = isExpanded))
                if (isExpanded) {
                    displayItems.addAll(parent.children)
                }
            }
        } else {
            displayItems.addAll(rawFlatItems)
        }
        notifyDataSetChanged()
    }

    private fun resolveIconResource(categoryName: String, iconName: String?, context: android.content.Context): Int {
        val resourceId = iconName?.let { TablerIconCatalog.resourceId(context, it) } ?: 0
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

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position].type) {
            ItemType.SECONDARY -> VIEW_TYPE_SECONDARY
            ItemType.PRIMARY_PARENT -> VIEW_TYPE_PRIMARY_PARENT
            ItemType.PRIMARY_CHILD -> VIEW_TYPE_PRIMARY_CHILD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PRIMARY_CHILD -> {
                val view = inflater.inflate(R.layout.item_statistics_child, parent, false)
                ChildViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_statistics, parent, false)
                ParentViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = displayItems[position]
        val context = holder.itemView.context
        val normalizedLabel = normalizeStatisticsCategoryLabel(item.label)

        val iconRes = resolveIconResource(normalizedLabel, item.iconName, context)
        val iconBackground = if (item.amount >= 0) IncomeExpenseColorScheme.incomeContainer(context)
        else IncomeExpenseColorScheme.expenseContainer(context)
        val iconTint = ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurface)
        val amountColor = if (item.amount >= 0) {
            IncomeExpenseColorScheme.incomePrimary(context)
        } else {
            IncomeExpenseColorScheme.expensePrimary(context)
        }

        val percentage = if (overallTotalAmount == 0.0) 0.0 else kotlin.math.abs(item.amount) / overallTotalAmount * 100.0

        if (holder is ParentViewHolder) {
            holder.textLabel.text = normalizedLabel
            holder.textAmount.text = "¥${Money.formatYuan(kotlin.math.abs(item.amount))}"

            holder.imageIcon.setImageResource(iconRes)
            holder.imageIcon.setColorFilter(iconTint)
            holder.iconContainer.backgroundTintList = ColorStateList.valueOf(iconBackground)
            holder.textMeta.text = String.format("%.2f%%", percentage)
            holder.textAmountMeta.visibility = View.GONE
            holder.progressBar.progress = percentage.coerceIn(0.0, 100.0).toInt()
            holder.progressBar.progressTintList = ColorStateList.valueOf(amountColor)
            holder.textAmount.setTextColor(amountColor)

            if (item.type == ItemType.PRIMARY_PARENT) {
                holder.imageExpandArrow.visibility = View.VISIBLE
                holder.imageExpandArrow.setImageResource(
                    if (item.isExpanded) R.drawable.tabler_chevron_down else R.drawable.tabler_chevron_right
                )
                holder.itemView.setOnClickListener {
                    if (item.children.isNotEmpty()) {
                        if (expandedParentLabels.contains(item.label)) {
                            expandedParentLabels.remove(item.label)
                        } else {
                            expandedParentLabels.add(item.label)
                        }
                        rebuildDisplayList()
                    } else {
                        onCategoryClick(normalizedLabel)
                    }
                }
            } else {
                holder.imageExpandArrow.visibility = View.GONE
                holder.itemView.setOnClickListener { onCategoryClick(normalizedLabel) }
            }
        } else if (holder is ChildViewHolder) {
            holder.textLabel.text = normalizedLabel
            holder.textAmount.text = "¥${Money.formatYuan(kotlin.math.abs(item.amount))}"

            holder.imageIcon.setImageResource(iconRes)
            holder.imageIcon.setColorFilter(iconTint)
            holder.iconContainer.backgroundTintList = ColorStateList.valueOf(iconBackground)
            holder.textMeta.text = String.format("%.2f%%", percentage)
            holder.progressBar.progress = percentage.coerceIn(0.0, 100.0).toInt()
            holder.progressBar.progressTintList = ColorStateList.valueOf(amountColor)
            holder.textAmount.setTextColor(amountColor)

            holder.itemView.setOnClickListener { onCategoryClick(normalizedLabel) }
        }
    }

    override fun getItemCount(): Int = displayItems.size

    class ParentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconContainer: View = itemView.findViewById(R.id.icon_container)
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
        val textLabel: TextView = itemView.findViewById(R.id.text_label)
        val textMeta: TextView = itemView.findViewById(R.id.text_meta)
        val textAmount: TextView = itemView.findViewById(R.id.text_amount)
        val textAmountMeta: TextView = itemView.findViewById(R.id.text_amount_meta)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        val imageExpandArrow: ImageView = itemView.findViewById(R.id.image_expand_arrow)
    }

    class ChildViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconContainer: View = itemView.findViewById(R.id.icon_container)
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
        val textLabel: TextView = itemView.findViewById(R.id.text_label)
        val textMeta: TextView = itemView.findViewById(R.id.text_meta)
        val textAmount: TextView = itemView.findViewById(R.id.text_amount)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
    }
}
