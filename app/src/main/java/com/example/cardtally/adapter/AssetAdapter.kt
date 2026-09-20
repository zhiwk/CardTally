package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ReplacementSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Asset
import com.example.cardtally.util.Money
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.AssetTypeIconCatalog
import com.example.cardtally.util.IncomeExpenseColorScheme
import com.example.cardtally.util.SwipeToEditDeleteHelper

class AssetAdapter(
    private var assets: List<Asset>,
    private val listener: OnAssetActionListener,
    private val archivedMode: Boolean = false,
    private val showAmount: Boolean = true,
    private val pickerMode: Boolean = false,
    private var selectedAssetId: Long? = null
) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    private var amountsVisible = true

    init {
        setHasStableIds(true)
    }

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
        holder.cardContent.translationX = 0f
        holder.archivedActions.visibility = if (archivedMode) View.VISIBLE else View.GONE
        holder.textAssetName.text = assetNameLabel(context, asset)
        holder.textAssetAmount.text = if (amountsVisible) {
            "¥${Money.formatYuan(asset.amount)}"
        } else {
            "***"
        }
        holder.textAssetAmount.setTextColor(
            if (asset.amount >= 0.0) {
                IncomeExpenseColorScheme.incomePrimary(context)
            } else {
                IncomeExpenseColorScheme.expensePrimary(context)
            }
        )
        holder.textAssetAmount.visibility = if (showAmount) View.VISIBLE else View.GONE
        holder.cardContent.setCardBackgroundColor(
            ThemeColorHelper.resolveThemeAwareResource(
                context,
                if (pickerMode && asset.id == selectedAssetId) {
                    R.color.primaryContainer_light
                } else {
                    R.color.surface_light
                }
            )
        )
        (holder.itemView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.leftMargin = 0
            params.rightMargin = 0
            params.bottomMargin = 0
            holder.itemView.layoutParams = params
        }
        holder.cardContent.radius = 0f
        holder.iconAssetPinned.visibility = if (asset.isPinned) View.VISIBLE else View.GONE

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
        holder.textAssetType.text = presentation.label
        holder.imageAssetIcon.setImageResource(presentation.iconRes)
        holder.imageAssetIcon.setColorFilter(
            presentation.iconTint?.let { ThemeColorHelper.resolveThemeAwareResource(context, it) }
                ?: ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
        )
        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(
            ThemeColorHelper.resolveThemeAwareResource(context, presentation.iconBackground)
        )

        holder.cardContent.setOnClickListener {
            if (!archivedMode) listener.onClick(asset)
        }
        holder.swipeHelper = if (archivedMode) {
            SwipeToEditDeleteHelper(
                cardContent = holder.cardContent,
                layoutActions = holder.archivedActions,
                onEdit = {},
                onDelete = { listener.onDelete(asset) },
                onArchive = { listener.onArchive(asset) },
                onClick = {}
            )
        } else {
            null
        }
    }

    override fun getItemCount(): Int = assets.size

    override fun getItemId(position: Int): Long = assets[position].id

    fun updateAssets(newAssets: List<Asset>) {
        assets = newAssets
        notifyDataSetChanged()
    }

    fun setAmountsVisible(visible: Boolean) {
        amountsVisible = visible
        notifyDataSetChanged()
    }

    fun setSelectedAssetId(assetId: Long?) {
        selectedAssetId = assetId
        notifyDataSetChanged()
    }

    fun moveAsset(from: Int, to: Int): Boolean {
        if (from !in assets.indices || to !in assets.indices || from == to) return false
        val reordered = assets.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        assets = reordered
        notifyItemMoved(from, to)
        return true
    }

    fun assetIdsInOrder(): List<Long> = assets.map { it.id }

    private data class AssetPresentation(
        val label: String,
        val iconRes: Int,
        val iconTint: Int?,
        val iconBackground: Int
    )

    private fun assetNameLabel(context: Context, asset: Asset): CharSequence {
        if (asset.includeInTotal) return asset.name
        val label = context.getString(R.string.asset_excluded_badge)
        return SpannableStringBuilder(asset.name).apply {
            append(' ')
            val labelStart = length
            append(label)
            setSpan(
                AssetExclusionBadgeSpan(context, label),
                labelStart,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private class AssetExclusionBadgeSpan(context: Context, private val label: String) : ReplacementSpan() {
        private val density = context.resources.displayMetrics.density
        private val scaledDensity = context.resources.displayMetrics.scaledDensity
        private val horizontalPadding = 6f * density
        private val verticalPadding = 2f * density
        private val cornerRadius = 6f * density
        private val textSize = 11f * scaledDensity
        private val backgroundColor = ThemeColorHelper.resolveThemeAwareResource(
            context,
            R.color.surface_container_low
        )
        private val textColor = ThemeColorHelper.resolveColor(
            context,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )

        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int = (badgePaint(paint).measureText(label) + horizontalPadding * 2).toInt()

        override fun draw(
            canvas: Canvas,
            text: CharSequence?,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            val badgePaint = badgePaint(paint)
            val baseline = (top + bottom - badgePaint.fontMetrics.bottom - badgePaint.fontMetrics.top) / 2f
            val bounds = RectF(
                x,
                baseline + badgePaint.fontMetrics.top - verticalPadding,
                x + badgePaint.measureText(label) + horizontalPadding * 2,
                baseline + badgePaint.fontMetrics.bottom + verticalPadding
            )
            canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, Paint().apply { color = backgroundColor })
            canvas.drawText(label, x + horizontalPadding, baseline, badgePaint)
        }

        private fun badgePaint(source: Paint) = Paint(source).apply {
            color = textColor
            textSize = this@AssetExclusionBadgeSpan.textSize
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
    }

    class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContent: CardView = itemView.findViewById(R.id.card_content)
        val archivedActions: View = itemView.findViewById(R.id.archived_actions)
        var swipeHelper: SwipeToEditDeleteHelper? = null
        val layoutAssetInfo: LinearLayout = itemView.findViewById(R.id.layout_asset_info)
        val iconContainer: View = itemView.findViewById(R.id.icon_container)
        val imageAssetIcon: ImageView = itemView.findViewById(R.id.image_asset_icon)
        val textAssetName: TextView = itemView.findViewById(R.id.text_asset_name)
        val textAssetAmount: TextView = itemView.findViewById(R.id.text_asset_amount)
        val textAssetType: TextView = itemView.findViewById(R.id.text_asset_type)
        val iconAssetPinned: ImageView = itemView.findViewById(R.id.icon_asset_pinned)
    }
}
