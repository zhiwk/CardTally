package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Asset
import com.example.cardtally.util.ThemeColorHelper

class RecordAssetSheetAdapter(
    private var items: List<AssetSheetItem>,
    private var selectedItemId: Long?,
    private val onSelected: (AssetSheetItem) -> Unit
) : RecyclerView.Adapter<RecordAssetSheetAdapter.AssetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record_asset_sheet, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == selectedItemId)
        holder.itemView.setOnClickListener {
            selectedItemId = items[position].id
            notifyDataSetChanged()
            onSelected(items[position])
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateSelection(itemId: Long?) {
        selectedItemId = itemId
        notifyDataSetChanged()
    }

    class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconContainer: View = itemView.findViewById(R.id.icon_container)
        private val iconView: ImageView = itemView.findViewById(R.id.image_icon)
        private val titleView: TextView = itemView.findViewById(R.id.text_title)
        private val subtitleView: TextView = itemView.findViewById(R.id.text_subtitle)
        private val amountView: TextView = itemView.findViewById(R.id.text_amount)
        private val checkView: ImageView = itemView.findViewById(R.id.image_check)

        fun bind(item: AssetSheetItem, selected: Boolean) {
            val context = itemView.context
            titleView.text = item.title
            subtitleView.text = item.subtitle
            amountView.text = item.amountLabel
            iconView.setImageResource(R.drawable.ic_asset)

            if (selected) {
                itemView.backgroundTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
                )
                titleView.setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimary))
                subtitleView.setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimary))
                amountView.setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimary))
                iconContainer.backgroundTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveThemeAwareResource(context, R.color.primaryContainer_light)
                )
                iconView.setColorFilter(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer))
                checkView.visibility = View.VISIBLE
                checkView.setColorFilter(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimary))
            } else {
                itemView.backgroundTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSurfaceContainerLow)
                )
                titleView.setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurface))
                subtitleView.setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
                amountView.setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurface))
                iconContainer.backgroundTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_surface_low)
                )
                iconView.setColorFilter(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary))
                checkView.visibility = View.GONE
            }
        }
    }
}

data class AssetSheetItem(
    val id: Long?,
    val asset: Asset?,
    val title: String,
    val subtitle: String,
    val amountLabel: String
)
