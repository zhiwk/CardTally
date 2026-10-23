package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Asset
import com.example.cardtally.util.AssetTypeIconCatalog
import com.example.cardtally.util.BankIconCatalog
import com.example.cardtally.util.Money
import com.example.cardtally.util.DefaultAssetPickerOptions.Group
import com.example.cardtally.util.DefaultAssetPickerOptions.Row
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.ThemeColorHelper
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

internal class DefaultRecordAssetPickerAdapter(
    private val selectedId: Long?,
    private val onSelected: (Asset?) -> Unit
) : ListAdapter<Row, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<Row>() {
    override fun areItemsTheSame(oldItem: Row, newItem: Row) = when {
        oldItem is Row.Header && newItem is Row.Header -> oldItem.group == newItem.group
        oldItem is Row.Account && newItem is Row.Account -> oldItem.asset.id == newItem.asset.id
        oldItem is Row.NoAsset && newItem is Row.NoAsset -> true
        else -> false
    }
    override fun areContentsTheSame(oldItem: Row, newItem: Row) = oldItem == newItem
}) {
    init {
        stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun getItemViewType(position: Int) = if (getItem(position) is Row.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) HeaderHolder(inflater.inflate(R.layout.item_default_asset_group, parent, false))
            else Holder(inflater.inflate(R.layout.item_default_record_asset, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is Row.Account -> (holder as Holder).bind(row.asset)
            is Row.Header -> (holder as HeaderHolder).bind(row.group)
            is Row.NoAsset -> (holder as Holder).bindNoAsset()
        }
    }

    private class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(group: Group) {
            (itemView as TextView).setText(when (group) {
                Group.FUNDS -> R.string.asset_account_cash
                Group.CREDIT -> R.string.asset_account_credit
                Group.TOPUP -> R.string.asset_account_topup
                Group.INVESTMENT -> R.string.asset_account_investment
                Group.RECEIVABLE -> R.string.asset_account_receivable
                Group.PAYABLE -> R.string.asset_account_payable
            })
            ViewCompat.setAccessibilityHeading(itemView, true)
        }
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name = view.findViewById<TextView>(R.id.text_default_asset_name)
        private val balance = view.findViewById<TextView>(R.id.text_default_asset_balance)
        private val excluded = view.findViewById<TextView>(R.id.text_default_asset_excluded)
        private val icon = view.findViewById<ImageView>(R.id.image_default_asset_icon)
        private val iconContainer = view.findViewById<View>(R.id.container_default_asset_icon)
        private val checked = view.findViewById<View>(R.id.image_default_asset_checked)

        fun bind(asset: Asset) {
            itemView.id = View.NO_ID
            val context = itemView.context
            val selected = asset.id == selectedId
            name.text = asset.name
            excluded.visibility = if (asset.includeInTotal) View.GONE else View.VISIBLE
            val amount = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
                isGroupingUsed = true
            }.format(BigDecimal(Money.formatYuan(asset.amount)))
            balance.text = context.getString(R.string.default_asset_balance, amount)
            balance.visibility = View.VISIBLE
            itemView.isSelected = selected
            checked.visibility = if (selected) View.VISIBLE else View.INVISIBLE

            val bank = BankIconCatalog.forCard(asset.categoryLabel, asset.categoryIconName, asset.type)
            val custom = asset.categoryIconName.takeIf {
                it.isNotBlank() && it !in setOf("tabler_category", "ic_category_other", "ic_asset")
            }?.let(TablerIconCatalog::resourceId)?.takeIf { it != 0 }
            val resource = bank?.iconRes ?: AssetTypeIconCatalog.resourceForLabel(asset.categoryLabel)
                ?: custom ?: when (asset.type) {
                    0 -> R.drawable.tabler_cash
                    1 -> R.drawable.tabler_building_bank
                    2 -> R.drawable.ic_asset_brand_alipay
                    3 -> R.drawable.ic_asset_brand_wechat
                    else -> R.drawable.ic_asset
                }
            AssetTypeIconCatalog.bindIcon(icon, resource, 20,
                ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            iconContainer.backgroundTintList = ColorStateList.valueOf(
                ThemeColorHelper.resolveThemeAwareResource(context,
                    if (AssetTypeIconCatalog.isBrandIcon(resource)) R.color.surface_container_lowest
                    else R.color.surface_container_low)
            )
            itemView.contentDescription = if (asset.includeInTotal) "${name.text}, ${balance.text}"
                else "${name.text}, ${excluded.text}, ${balance.text}"
            bindAccessibility(selected)
            itemView.setOnClickListener { onSelected(asset) }
        }

        fun bindNoAsset() {
            itemView.id = R.id.button_record_asset_none
            val context = itemView.context
            val selected = selectedId == null
            name.setText(R.string.record_asset_no_selection)
            balance.visibility = View.GONE
            excluded.visibility = View.GONE
            itemView.isSelected = selected
            checked.visibility = if (selected) View.VISIBLE else View.INVISIBLE
            AssetTypeIconCatalog.bindIcon(icon, R.drawable.tabler_wallet_off, 20,
                ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            iconContainer.backgroundTintList = ColorStateList.valueOf(
                ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_container_low))
            itemView.contentDescription = name.text
            bindAccessibility(selected)
            itemView.setOnClickListener { onSelected(null) }
        }

        private fun bindAccessibility(selected: Boolean) {
            ViewCompat.setAccessibilityDelegate(itemView, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = RadioButton::class.java.name
                    info.isCheckable = true
                    info.isChecked = selected
                }
            })
        }
    }
}
