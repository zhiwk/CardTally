package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.TablerIconCatalog

/**
 * Icon-only grid used by the shared category icon browser. The tile shows just
 * the icon; [labels] supplies a localized description so TalkBack announces a
 * name instead of a raw catalog key.
 */
class CategoryIconGridAdapter(
    private val labels: Map<String, String>,
    private val onSelected: (String) -> Unit
) : RecyclerView.Adapter<CategoryIconGridAdapter.IconViewHolder>() {

    private var icons: List<String> = emptyList()
    private var selectedIcon: String? = null

    fun submit(newIcons: List<String>, selection: String?) {
        icons = newIcons
        selectedIcon = selection
        notifyDataSetChanged()
    }

    fun setSelected(selection: String?) {
        if (selection == selectedIcon) return
        val previous = selectedIcon
        selectedIcon = selection
        icons.indexOf(previous).takeIf { it >= 0 }?.let { notifyItemChanged(it) }
        icons.indexOf(selection).takeIf { it >= 0 }?.let { notifyItemChanged(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon_grid, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val icon = icons[position]
        val isSelected = icon == selectedIcon
        val context = holder.itemView.context

        val resourceId = TablerIconCatalog.resourceId(context, icon)
        holder.imageIcon.setImageResource(resourceId.takeIf { it != 0 } ?: R.drawable.tabler_category)
        holder.imageCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        holder.itemView.isSelected = isSelected
        holder.itemView.contentDescription = FeaturedIconLabels.describe(icon, labels)
        holder.itemView.setOnClickListener { onSelected(icon) }
    }

    override fun getItemCount(): Int = icons.size

    class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
        val imageCheck: ImageView = itemView.findViewById(R.id.image_icon_check)
    }
}
