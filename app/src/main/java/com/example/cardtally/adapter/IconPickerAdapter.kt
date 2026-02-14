package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R

class IconPickerAdapter(
    private val icons: List<String>,
    private val selectedIcon: String?,
    private val onIconSelected: (String?) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    private var selectedPosition = -1

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
        
        val resourceId = holder.itemView.context.resources.getIdentifier(
            iconName,
            "drawable",
            holder.itemView.context.packageName
        )
        
        if (resourceId != 0) {
            holder.imageIcon.setImageResource(resourceId)
        }

        if (position == selectedPosition) {
            holder.cardView.setCardBackgroundColor(0xFF4CAF50.toInt())
        } else {
            holder.cardView.setCardBackgroundColor(0xFFFFFFFF.toInt())
        }

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onIconSelected(iconName)
        }
    }

    override fun getItemCount(): Int = icons.size

    class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_view)
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
    }
}
