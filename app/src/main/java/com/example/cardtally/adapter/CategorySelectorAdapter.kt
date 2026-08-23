package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Category
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.MaterialSymbolCatalog

class CategorySelectorAdapter(
    private var categories: List<Category>,
    private var selectedCategory: Category?,
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<CategorySelectorAdapter.CategoryViewHolder>() {

    // Records may only reference leaf categories. Keep this adapter safe as well as
    // the tree selector, because older callers can still pass a flat category list.
    private val leafCategories: List<Category>
        get() {
            val parentIds = categories.mapNotNull { it.parentId }.toSet()
            return categories.filter { it.id !in parentIds }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_selector, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = leafCategories[position]
        holder.textName.text = category.name

        val iconName = category.icon
        if (!iconName.isNullOrEmpty()) {
            val resourceId = MaterialSymbolCatalog.resourceId(iconName)
            if (resourceId != 0) {
                holder.imageIcon.setImageResource(resourceId)
            } else {
                holder.imageIcon.setImageResource(R.drawable.ic_category_other)
            }
        } else {
            holder.imageIcon.setImageResource(R.drawable.ic_category_other)
        }

        if (selectedCategory?.id == category.id) {
            holder.cardCategory.setCardBackgroundColor(
                ThemeColorHelper.resolveColor(holder.itemView.context, com.google.android.material.R.attr.colorSecondaryContainer)
            )
            holder.imageIcon.setColorFilter(
                ThemeColorHelper.resolveColor(holder.itemView.context, com.google.android.material.R.attr.colorSecondary)
            )
            holder.textName.setTextColor(
                ThemeColorHelper.resolveColor(holder.itemView.context, com.google.android.material.R.attr.colorSecondary)
            )
        } else {
            holder.cardCategory.setCardBackgroundColor(
                ThemeColorHelper.resolveColor(holder.itemView.context, com.google.android.material.R.attr.colorSurfaceContainerLowest)
            )
            holder.imageIcon.setColorFilter(
                ThemeColorHelper.resolveColor(holder.itemView.context, com.google.android.material.R.attr.colorPrimary)
            )
            holder.textName.setTextColor(
                ThemeColorHelper.resolveColor(holder.itemView.context, com.google.android.material.R.attr.colorOnBackground)
            )
        }

        holder.itemView.setOnClickListener {
            selectedCategory = category
            notifyDataSetChanged()
            onCategorySelected(category)
        }
    }

    override fun getItemCount(): Int = leafCategories.size

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    fun setSelectedCategory(category: Category?) {
        selectedCategory = category
        notifyDataSetChanged()
    }

    fun getSelectedCategory(): Category? = selectedCategory

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardCategory: CardView = itemView.findViewById(R.id.card_category)
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
        val textName: TextView = itemView.findViewById(R.id.text_name)
    }
}
