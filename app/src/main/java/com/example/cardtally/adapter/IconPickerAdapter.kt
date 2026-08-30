package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.TablerIconCatalog

class IconPickerAdapter(
    private val allIcons: List<String>,
    private val selectedIcon: String?,
    private val onIconSelected: (String?) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    private var icons: List<String> = allIcons
    private var selectedPosition = -1
    private var selectionListener: ((String?) -> Unit)? = onIconSelected

    init {
        if (selectedIcon != null) {
            selectedPosition = icons.indexOf(selectedIcon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val iconName = icons[position]
        
        val resourceId = TablerIconCatalog.resourceId(holder.itemView.context, iconName)
        if (resourceId != 0) {
            holder.imageIcon.setImageResource(resourceId)
        } else {
            holder.imageIcon.setImageDrawable(null)
        }

        if (position == selectedPosition) {
            holder.cardView.setCardBackgroundColor(0xFF4CAF50.toInt())
        } else {
            holder.cardView.setCardBackgroundColor(0xFFFFFFFF.toInt())
        }

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            if (previousPosition >= 0) notifyItemChanged(previousPosition)
            if (selectedPosition >= 0) notifyItemChanged(selectedPosition)
            selectionListener?.invoke(iconName)
        }
    }

    override fun getItemCount(): Int = icons.size

    fun filter(query: String) {
        val normalizedQuery = query.trim().lowercase()
        icons = if (normalizedQuery.isEmpty()) {
            allIcons
        } else {
            allIcons.filter { it.lowercase().contains(normalizedQuery) }
        }
        selectedPosition = icons.indexOf(selectedIcon)
        notifyDataSetChanged()
    }

    fun setOnIconSelected(listener: (String?) -> Unit) {
        selectionListener = listener
    }

    class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_view)
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
    }
}
