package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Asset
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.LedgerSession
import com.example.cardtally.util.SwipeToEditDeleteHelper
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.AssetTypeIconCatalog

class AssetAdapter(
    private var assets: List<Asset>,
    private val listener: OnAssetActionListener,
    private val archivedMode: Boolean = false,
    private val showAmount: Boolean = true
) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    private var amountsVisible = true

    interface OnAssetActionListener {
        fun onClick(asset: Asset)
        fun onEdit(asset: Asset)
        fun onDelete(asset: Asset)
        fun onArchive(asset: Asset)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asset, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val asset = assets[position]
        val context = holder.itemView.context
        holder.textAssetName.text = asset.name
        holder.textAssetAmount.text = if (amountsVisible) {
            String.format("¥%.2f", asset.amount)
        } else {
            "***"
        }
        holder.textAssetAmount.visibility = if (showAmount) View.VISIBLE else View.GONE
        (holder.itemView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            val margin = if (archivedMode) (16 * context.resources.displayMetrics.density).toInt() else 0
            params.leftMargin = margin
            params.rightMargin = margin
            params.bottomMargin = if (archivedMode) (8 * context.resources.displayMetrics.density).toInt() else 0
            holder.itemView.layoutParams = params
        }
        holder.cardContent.radius = if (archivedMode) {
            12 * context.resources.displayMetrics.density
        } else {
            0f
        }
        holder.iconAssetPinned.visibility = if (asset.isPinned) View.VISIBLE else View.GONE
        val canManage = asset.ledgerId == LedgerSession.getCurrentId(context)
        holder.actionEditContainer.visibility = if (archivedMode || !canManage) View.GONE else View.VISIBLE
        holder.actionPinContainer.visibility = if (archivedMode || !canManage) View.GONE else View.VISIBLE
        holder.btnArchive.setImageResource(
            if (archivedMode) R.drawable.tabler_archive else R.drawable.ic_archive
        )
        holder.btnArchive.contentDescription = if (archivedMode) "取消归档" else "归档资产"
        holder.actionArchiveContainer.visibility = if (!canManage && !archivedMode) View.GONE else View.VISIBLE

        val fallbackPresentation = when (asset.type) {
            0 -> AssetPresentation("现金", R.drawable.ic_asset, R.color.warning_primary, R.color.warning_container)
            1 -> AssetPresentation("银行卡", R.drawable.ic_asset, null, R.color.surface_container_lowest)
            2 -> AssetPresentation("支付宝", R.drawable.ic_asset, null, R.color.surface_container_lowest)
            3 -> AssetPresentation("微信", R.drawable.ic_asset, null, R.color.surface_container_lowest)
            else -> AssetPresentation("其他", R.drawable.ic_asset, null, R.color.surface_container_lowest)
        }
        val selectedIcon = asset.categoryIconName
            .takeIf { it.isNotBlank() }
            ?.takeUnless {
                it == "tabler_category" || it == "ic_category_other" || it == "ic_asset"
            }
            ?.let(TablerIconCatalog::resourceId)
            ?.takeIf { it != 0 }
        val standardIcon = AssetTypeIconCatalog.resourceForLabel(asset.categoryLabel)
        val presentation = fallbackPresentation.copy(
            label = asset.categoryLabel.takeIf { it.isNotBlank() } ?: fallbackPresentation.label,
            iconRes = standardIcon ?: selectedIcon ?: fallbackPresentation.iconRes
        )
        holder.textAssetType.text = if (asset.includeInTotal) {
            presentation.label
        } else {
            "${presentation.label} · 不计入"
        }
        holder.imageAssetIcon.setImageResource(presentation.iconRes)
        holder.imageAssetIcon.setColorFilter(
            presentation.iconTint?.let { ThemeColorHelper.resolveThemeAwareResource(context, it) }
                ?: ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        )
        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(
            ThemeColorHelper.resolveThemeAwareResource(context, presentation.iconBackground)
        )

        holder.swipeHelper = SwipeToEditDeleteHelper(
            holder.cardContent,
            holder.layoutActions,
            onEdit = { listener.onEdit(asset) },
            onDelete = { listener.onDelete(asset) },
            onArchive = { listener.onArchive(asset) },
            onClick = { listener.onClick(asset) }
        )
        
        holder.btnArchive.setOnClickListener {
            listener.onArchive(asset)
        }
        holder.btnDelete.contentDescription = if (asset.isPinned) "取消置顶资产" else "置顶资产"
    }

    override fun getItemCount(): Int = assets.size

    fun updateAssets(newAssets: List<Asset>) {
        assets = newAssets
        notifyDataSetChanged()
    }

    fun setAmountsVisible(visible: Boolean) {
        amountsVisible = visible
        notifyDataSetChanged()
    }

    private data class AssetPresentation(
        val label: String,
        val iconRes: Int,
        val iconTint: Int?,
        val iconBackground: Int
    )

    class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContent: CardView = itemView.findViewById(R.id.card_content)
        val layoutActions: View = itemView.findViewById(R.id.layout_actions)
        val layoutAssetInfo: LinearLayout = itemView.findViewById(R.id.layout_asset_info)
        val iconContainer: View = itemView.findViewById(R.id.icon_container)
        val imageAssetIcon: ImageView = itemView.findViewById(R.id.image_asset_icon)
        val textAssetName: TextView = itemView.findViewById(R.id.text_asset_name)
        val textAssetAmount: TextView = itemView.findViewById(R.id.text_asset_amount)
        val textAssetType: TextView = itemView.findViewById(R.id.text_asset_type)
        val iconAssetPinned: ImageView = itemView.findViewById(R.id.icon_asset_pinned)
        val btnArchive: ImageButton = itemView.findViewById(R.id.btn_archive)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        val actionArchiveContainer: View = btnArchive.parent as View
        val actionEditContainer: View = btnEdit.parent as View
        val actionPinContainer: View = btnDelete.parent as View
        var swipeHelper: SwipeToEditDeleteHelper? = null
    }
}
