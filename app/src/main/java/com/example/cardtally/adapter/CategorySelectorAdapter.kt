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
import com.example.cardtally.util.TablerIconCatalog

class CategorySelectorAdapter(
    categories: List<Category>,
    private var selectedCategoryId: Long?,
    private val pathLabelProvider: ((Category) -> String)? = null,
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<CategorySelectorAdapter.CategoryViewHolder>() {

    // Records may only reference leaf categories. Keep this adapter safe as well as
    // the tree selector, because older callers can still pass a flat category list.
    private var leafCategories: List<Category> = deriveLeaves(categories)
    var onAddCategory: (() -> Unit)? = null
    var showAddCategory = true
        set(value) { field = value; notifyDataSetChanged() }
    fun isCategoryPosition(position: Int) = position in leafCategories.indices

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_selector, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        if (!isCategoryPosition(position)) {
            holder.textName.setText(R.string.record_category_quick_add)
            holder.textName.isSelected = false
            holder.imageIcon.setImageResource(R.drawable.ic_add)
            holder.imageSelected.visibility = View.GONE
            holder.cardCategory.setCardBackgroundColor(ThemeColorHelper.resolveColor(holder.itemView.context, com.google.android.material.R.attr.colorSurface))
            val color = ThemeColorHelper.resolveColor(holder.itemView.context, com.google.android.material.R.attr.colorOnSurfaceVariant)
            holder.imageIcon.setColorFilter(color)
            holder.textName.setTextColor(color)
            holder.itemView.contentDescription = holder.itemView.context.getString(R.string.record_category_quick_add)
            holder.itemView.setOnClickListener { onAddCategory?.invoke() }
            return
        }
        val category = leafCategories[position]
        holder.textName.text = category.name
        // Keep every category label on one line; long labels scroll instead of
        // changing the grid row height or hiding part of the name.
        holder.textName.isSelected = true
        holder.itemView.contentDescription = pathLabelProvider?.invoke(category) ?: category.name

        val iconName = category.icon
        if (!iconName.isNullOrEmpty()) {
            val drawableId = TablerIconCatalog.resourceId(holder.itemView.context, iconName)
            holder.imageIcon.setImageResource(drawableId.takeIf { it != 0 } ?: R.drawable.ic_category_other)
        } else {
            holder.imageIcon.setImageResource(R.drawable.ic_category_other)
        }

        val isSelected = selectedCategoryId == category.id
        holder.imageSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
        if (isSelected) {
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
            selectedCategoryId = category.id
            notifyDataSetChanged()
            onCategorySelected(category)
        }
    }

    override fun getItemCount(): Int = leafCategories.size + if (showAddCategory && onAddCategory != null) 1 else 0

    fun updateCategories(newCategories: List<Category>) {
        leafCategories = deriveLeaves(newCategories)
        notifyDataSetChanged()
    }

    fun setSelectedCategoryId(categoryId: Long?) {
        if (selectedCategoryId == categoryId) return
        selectedCategoryId = categoryId
        notifyDataSetChanged()
    }

    fun getSelectedCategoryId(): Long? = selectedCategoryId

    fun moveCategory(fromPosition: Int, toPosition: Int) {
        if (fromPosition !in leafCategories.indices || toPosition !in leafCategories.indices) return
        val reordered = leafCategories.toMutableList()
        reordered.add(toPosition, reordered.removeAt(fromPosition))
        leafCategories = reordered
        notifyItemMoved(fromPosition, toPosition)
    }

    fun categoryIdsInOrder(): List<Long> = leafCategories.map { it.id }

    private fun deriveLeaves(categories: List<Category>): List<Category> {
        val parentIds = categories.mapNotNull { it.parentId }.toSet()
        return categories.filter { it.id !in parentIds }
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardCategory: CardView = itemView.findViewById(R.id.card_category)
        val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
        val imageSelected: ImageView = itemView.findViewById(R.id.image_selected)
        val textName: TextView = itemView.findViewById(R.id.text_name)
    }
}
