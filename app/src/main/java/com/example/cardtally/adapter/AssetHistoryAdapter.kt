package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.AssetBalanceEvent
import com.example.cardtally.model.AssetHistoryEntry
import com.example.cardtally.model.DateGroup
import com.example.cardtally.util.ThemeColorHelper
import java.math.BigDecimal

/** Only the asset-detail screen combines bills with non-editable balance history. */
class AssetHistoryAdapter(private val listener: DateGroupAdapter.OnRecordActionListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class Header(val group: DateGroup) : Row()
        data class Item(val entry: AssetHistoryEntry, val first: Boolean, val last: Boolean) : Row()
    }

    private var rows: List<Row> = emptyList()
    private var categoryIcons: Map<Long, String> = emptyMap()
    private var categoryPaths: Map<Long, String> = emptyMap()
    private var assetNames: Map<Long, String> = emptyMap()

    fun submit(
        entries: List<AssetHistoryEntry>, categoryIcons: Map<Long, String>,
        categoryPaths: Map<Long, String>, assetNames: Map<Long, String>
    ) {
        this.categoryIcons = categoryIcons
        this.categoryPaths = categoryPaths
        this.assetNames = assetNames
        rows = buildList {
            entries.groupBy { it.date }.toSortedMap(compareByDescending { it }).forEach { (date, items) ->
                // Initializations/adjustments never contribute to the date's income/expense totals.
                add(Row.Header(DateGroup(date, items.filterIsInstance<AssetHistoryEntry.Bill>().map { it.record })))
                items.forEachIndexed { index, entry -> add(Row.Item(entry, index == 0, index == items.lastIndex)) }
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = when (val row = rows[position]) {
        is Row.Header -> 0
        is Row.Item -> if (row.entry is AssetHistoryEntry.Bill) 1 else 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            if (viewType == 0) R.layout.item_date_header else R.layout.item_record, parent, false)
        return when (viewType) {
            0 -> DateGroupAdapter.DateHeaderViewHolder(view)
            1 -> DateGroupAdapter.RecordViewHolder(view)
            else -> BalanceViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as DateGroupAdapter.DateHeaderViewHolder).bind(row.group)
            is Row.Item -> when (val entry = row.entry) {
                is AssetHistoryEntry.Bill -> (holder as DateGroupAdapter.RecordViewHolder).bind(
                    entry.record, listener, false, false, categoryIconsById = categoryIcons,
                    showAssetRoute = false, roundTopCorners = row.first, roundBottomCorners = row.last,
                    categoryPathsById = categoryPaths, assetNamesById = assetNames)
                is AssetHistoryEntry.Balance -> (holder as BalanceViewHolder).bind(entry.event, row.first, row.last)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is BalanceViewHolder) holder.recycle()
        super.onViewRecycled(holder)
    }

    private class BalanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val layoutController = RecordRowLayoutController(view)

        fun bind(event: AssetBalanceEvent, first: Boolean, last: Boolean) {
            val context = itemView.context
            val title = context.getString(if (event.kind == AssetBalanceEvent.INITIAL)
                R.string.asset_balance_initial else R.string.asset_balance_adjustment)
            val delta = (if (event.deltaMinor < 0) "-" else if (event.deltaMinor > 0) "+" else "") +
                "¥" + BigDecimal.valueOf(event.deltaMinor, 2).abs().toPlainString()
            val change = context.getString(R.string.asset_balance_change,
                BigDecimal.valueOf(event.balanceBeforeMinor, 2).toPlainString(),
                BigDecimal.valueOf(event.balanceAfterMinor, 2).toPlainString())
            itemView.findViewById<TextView>(R.id.text_category).text = title
            itemView.findViewById<TextView>(R.id.text_time).apply { text = event.date; visibility = View.VISIBLE }
            itemView.findViewById<TextView>(R.id.text_description).apply { text = change; visibility = View.VISIBLE }
            itemView.findViewById<TextView>(R.id.text_amount).apply {
                text = delta
                setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurface))
            }
            listOf(R.id.text_asset, R.id.text_fee, R.id.text_ledger, R.id.layout_actions,
                R.id.view_record_divider).forEach { itemView.findViewById<View>(it).visibility = View.GONE }
            itemView.findViewById<ImageView>(R.id.image_icon).apply {
                setImageResource(R.drawable.ic_edit)
                imageTintList = ColorStateList.valueOf(ThemeColorHelper.resolveThemeAwareResource(context, R.color.warning_primary))
            }
            itemView.findViewById<View>(R.id.view_icon).backgroundTintList = ColorStateList.valueOf(
                ThemeColorHelper.resolveThemeAwareResource(context, R.color.warning_container))
            itemView.findViewById<View>(R.id.card_content).apply {
                isClickable = false
                isFocusable = false
                contentDescription = "$title，${event.date}，$delta，$change"
                val radius = 12f * resources.displayMetrics.density
                val top = if (first) radius else 0f
                val bottom = if (last) radius else 0f
                background = GradientDrawable().apply {
                    setColor(ThemeColorHelper.resolveCardSurface(context))
                    cornerRadii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
                }
            }
            layoutController.attach()
            layoutController.applyLayout()
        }

        fun recycle() = layoutController.detach()
    }
}
