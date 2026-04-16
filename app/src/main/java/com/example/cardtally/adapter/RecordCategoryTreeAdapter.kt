package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Category
import com.example.cardtally.util.ThemeColorHelper

class RecordCategoryTreeAdapter(
    private val allCategories: List<Category>,
    selectedCategoryId: Long?,
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<RecordCategoryTreeAdapter.CategoryViewHolder>() {

    private val categoriesByParent = allCategories.groupBy { it.parentId }
    private val expandedIds = mutableSetOf<Long>()
    private var selectedCategoryId: Long? = selectedCategoryId
    private var visibleItems = buildVisibleItems()

    init {
        selectedCategoryId?.let(::expandAncestors)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record_category_tree, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = visibleItems[position]
        holder.bind(item, item.category.id == selectedCategoryId)
        holder.itemView.setOnClickListener {
            if (item.hasChildren) {
                toggleExpanded(item.category.id)
            } else {
                selectedCategoryId = item.category.id
                notifyDataSetChanged()
                onCategorySelected(item.category)
            }
        }
    }

    override fun getItemCount(): Int = visibleItems.size

    fun updateSelectedCategory(categoryId: Long?) {
        selectedCategoryId = categoryId
        if (categoryId != null) {
            expandAncestors(categoryId)
        }
        visibleItems = buildVisibleItems()
        notifyDataSetChanged()
    }

    private fun toggleExpanded(categoryId: Long) {
        if (!expandedIds.add(categoryId)) {
            expandedIds.remove(categoryId)
        }
        visibleItems = buildVisibleItems()
        notifyDataSetChanged()
    }

    private fun expandAncestors(categoryId: Long) {
        var current = allCategories.firstOrNull { it.id == categoryId }
        while (current?.parentId != null) {
            expandedIds.add(current.parentId!!)
            current = allCategories.firstOrNull { it.id == current?.parentId }
        }
        visibleItems = buildVisibleItems()
    }

    private fun buildVisibleItems(): List<CategoryTreeItem> {
        val items = mutableListOf<CategoryTreeItem>()

        fun appendChildren(parentId: Long?, level: Int) {
            categoriesByParent[parentId]
                ?.sortedBy { it.name }
                ?.forEach { category ->
                    val children = categoriesByParent[category.id].orEmpty()
                    val hasChildren = children.isNotEmpty()
                    items.add(
                        CategoryTreeItem(
                            category = category,
                            level = level,
                            hasChildren = hasChildren,
                            expanded = hasChildren && expandedIds.contains(category.id)
                        )
                    )
                    if (hasChildren && expandedIds.contains(category.id)) {
                        appendChildren(category.id, level + 1)
                    }
                }
        }

        appendChildren(null, 0)
        return items
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconContainer: View = itemView.findViewById(R.id.icon_container)
        private val iconView: ImageView = itemView.findViewById(R.id.image_icon)
        private val nameView: TextView = itemView.findViewById(R.id.text_name)
        private val trailingView: ImageView = itemView.findViewById(R.id.image_trailing)
        private val indentView: View = itemView.findViewById(R.id.view_indent)

        fun bind(item: CategoryTreeItem, selected: Boolean) {
            val context = itemView.context
            val params = indentView.layoutParams
            params.width = (item.level * 18 * context.resources.displayMetrics.density).toInt()
            indentView.layoutParams = params

            nameView.text = item.category.name
            val iconRes = item.category.icon?.let {
                context.resources.getIdentifier(it, "drawable", context.packageName)
            }?.takeIf { it != 0 } ?: R.drawable.ic_category_other
            iconView.setImageResource(iconRes)

            if (selected) {
                itemView.backgroundTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSurfaceContainerHigh)
                )
                iconContainer.backgroundTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
                )
                iconView.setColorFilter(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimary))
                nameView.setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary))
            } else {
                itemView.backgroundTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSurfaceContainerLowest)
                )
                iconContainer.backgroundTintList = ColorStateList.valueOf(
                    ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_surface_low)
                )
                iconView.setColorFilter(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary))
                nameView.setTextColor(ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurface))
            }

            trailingView.visibility = View.VISIBLE
            when {
                item.hasChildren -> {
                    trailingView.setImageResource(if (item.expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
                    trailingView.imageTintList = ColorStateList.valueOf(
                        ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant)
                    )
                    trailingView.alpha = 1f
                }
                selected -> {
                    trailingView.setImageResource(R.drawable.ic_check)
                    trailingView.imageTintList = ColorStateList.valueOf(
                        ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
                    )
                    trailingView.alpha = 1f
                }
                else -> {
                    trailingView.setImageResource(R.drawable.ic_arrow_right)
                    trailingView.imageTintList = ColorStateList.valueOf(
                        ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant)
                    )
                    trailingView.alpha = 0.4f
                }
            }
        }
    }
}

data class CategoryTreeItem(
    val category: Category,
    val level: Int,
    val hasChildren: Boolean,
    val expanded: Boolean
)
