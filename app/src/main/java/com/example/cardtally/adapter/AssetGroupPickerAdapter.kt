package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.database.DatabaseHelper

/**
 * Lists one row per stable asset group (already de-duplicated by the database) so
 * the user picks a whole group rather than individual assets. The selected group
 * is marked by both the radio and a check glyph, not by colour alone.
 */
class AssetGroupPickerAdapter(
    private val groups: List<DatabaseHelper.AssetGroupDescriptor>,
    private val selectedRootId: Long?,
    private val primaryLabel: (DatabaseHelper.AssetGroupDescriptor) -> String,
    private val secondaryLabel: (DatabaseHelper.AssetGroupDescriptor) -> String,
    private val onSelected: (DatabaseHelper.AssetGroupDescriptor) -> Unit
) : RecyclerView.Adapter<AssetGroupPickerAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radio: RadioButton = itemView.findViewById(R.id.radio_asset_group)
        val check: ImageView = itemView.findViewById(R.id.image_asset_group_check)
        val name: TextView = itemView.findViewById(R.id.text_asset_group_name)
        val members: TextView = itemView.findViewById(R.id.text_asset_group_members)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asset_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        val isSelected = group.rootLedgerId == selectedRootId
        val primary = primaryLabel(group)
        holder.name.text = primary
        holder.members.text = secondaryLabel(group)
        holder.radio.isChecked = isSelected
        holder.check.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        // Identically named groups stay distinguishable through their members.
        holder.itemView.contentDescription = holder.itemView.context.getString(
            if (isSelected) R.string.icon_picker_item_selected else R.string.icon_picker_item_unselected,
            "$primary, ${secondaryLabel(group)}"
        )
        holder.itemView.isSelected = isSelected
        holder.itemView.setOnClickListener {
            val current = holder.adapterPosition
            if (current == RecyclerView.NO_POSITION) return@setOnClickListener
            onSelected(groups[current])
        }
    }

    override fun getItemCount(): Int = groups.size
}
