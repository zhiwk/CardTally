package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Asset
import com.example.cardtally.util.SwipeToEditDeleteHelper

class AssetAdapter(
    private var assets: List<Asset>,
    private val listener: OnAssetActionListener
) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    interface OnAssetActionListener {
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
        holder.textAssetName.text = asset.name
        holder.textAssetAmount.text = String.format("¥%.2f", asset.amount)

        val typeText = when (asset.type) {
            0 -> "现金"
            1 -> "银行卡"
            2 -> "支付宝"
            3 -> "微信"
            else -> "其他"
        }
        holder.textAssetType.text = typeText

        holder.swipeHelper = SwipeToEditDeleteHelper(
            holder.cardContent,
            holder.layoutActions,
            onEdit = { listener.onEdit(asset) },
            onDelete = { listener.onDelete(asset) },
            onArchive = { listener.onArchive(asset) }
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

    class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContent: CardView = itemView.findViewById(R.id.card_content)
        val layoutActions: View = itemView.findViewById(R.id.layout_actions)
        val layoutAssetInfo: LinearLayout = itemView.findViewById(R.id.layout_asset_info)
        val textAssetName: TextView = itemView.findViewById(R.id.text_asset_name)
        val textAssetAmount: TextView = itemView.findViewById(R.id.text_asset_amount)
        val textAssetType: TextView = itemView.findViewById(R.id.text_asset_type)
        val btnArchive: ImageButton = itemView.findViewById(R.id.btn_archive)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        var swipeHelper: SwipeToEditDeleteHelper? = null
    }
}
