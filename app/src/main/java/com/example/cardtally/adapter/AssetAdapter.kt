package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Asset

class AssetAdapter(
    private var assets: List<Asset>,
    private val listener: OnAssetActionListener
) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    interface OnAssetActionListener {
        fun onEdit(asset: Asset)
        fun onDelete(asset: Asset)
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

        holder.btnEdit.setOnClickListener {
            listener.onEdit(asset)
        }

        holder.btnDelete.setOnClickListener {
            listener.onDelete(asset)
        }
    }

    override fun getItemCount(): Int = assets.size

    fun updateAssets(newAssets: List<Asset>) {
        assets = newAssets
        notifyDataSetChanged()
    }

    class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textAssetName: TextView = itemView.findViewById(R.id.text_asset_name)
        val textAssetAmount: TextView = itemView.findViewById(R.id.text_asset_amount)
        val textAssetType: TextView = itemView.findViewById(R.id.text_asset_type)
        val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        val btnDelete: Button = itemView.findViewById(R.id.btn_delete)
    }
}
