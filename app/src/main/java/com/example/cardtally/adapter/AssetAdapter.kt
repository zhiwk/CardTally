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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Asset
import com.example.cardtally.util.SwipeToEditDeleteHelper

class AssetAdapter(
    private var assets: List<Asset>,
    private val listener: OnAssetActionListener
) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

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
        holder.textAssetAmount.text = String.format("¥%.2f", asset.amount)

        val presentation = when (asset.type) {
            0 -> AssetPresentation("现金", R.drawable.ic_asset, R.color.warning_primary, R.color.warning_container)
            1 -> AssetPresentation("银行卡", R.drawable.ic_asset, R.color.primary_light, R.color.surface_container_lowest)
            2 -> AssetPresentation("支付宝", R.drawable.ic_asset, R.color.secondary_light, R.color.surface_container_lowest)
            3 -> AssetPresentation("微信", R.drawable.ic_asset, R.color.secondary_light, R.color.surface_container_lowest)
            else -> AssetPresentation("其他", R.drawable.ic_asset, R.color.primary_light, R.color.surface_container_lowest)
        }
        holder.textAssetType.text = presentation.label
        holder.imageAssetIcon.setImageResource(presentation.iconRes)
        holder.imageAssetIcon.setColorFilter(ContextCompat.getColor(context, presentation.iconTint))
        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, presentation.iconBackground)
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
    }

    override fun getItemCount(): Int = assets.size

    fun updateAssets(newAssets: List<Asset>) {
        assets = newAssets
        notifyDataSetChanged()
    }

    private data class AssetPresentation(
        val label: String,
        val iconRes: Int,
        val iconTint: Int,
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
        val btnArchive: ImageButton = itemView.findViewById(R.id.btn_archive)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        var swipeHelper: SwipeToEditDeleteHelper? = null
    }
}
