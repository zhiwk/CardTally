package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Category
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.ThemeColorHelper

/** Expandable top-level category cards used by standard record entry. */
class RecordCategoryGroupAdapter(
    categories: List<Category>,
    selectedCategoryId: Long?,
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<RecordCategoryGroupAdapter.CategoryGroupViewHolder>() {

    private var categories = categories
    private var selectedCategoryId = selectedCategoryId
    private val expandedIds = mutableSetOf<Long>()

    init {
        selectedCategoryId?.let(::expandAncestors)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryGroupViewHolder =
        CategoryGroupViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_record_category_group, parent, false)
        )

    override fun onBindViewHolder(holder: CategoryGroupViewHolder, position: Int) {
        holder.bind(
            categories.filter { it.parentId == null }[position],
            categories,
            expandedIds.contains(categories.filter { it.parentId == null }[position].id),
            selectedCategoryId,
            onToggle = { toggle(it) },
            onSelect = {
                selectedCategoryId = it.id
                notifyDataSetChanged()
                onCategorySelected(it)
            }
        )
    }

    override fun getItemCount(): Int = categories.count { it.parentId == null }

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        val validIds = categories.filter { it.parentId == null }.map { it.id }.toSet()
        expandedIds.retainAll(validIds)
        notifyDataSetChanged()
    }

    fun setSelectedCategoryId(categoryId: Long?, expandParent: Boolean = false) {
        selectedCategoryId = categoryId
        if (expandParent && categoryId != null) expandAncestors(categoryId)
        notifyDataSetChanged()
    }

    fun getSelectedCategoryId(): Long? = selectedCategoryId

    fun moveParent(fromPosition: Int, toPosition: Int) {
        val parents = categories.filter { it.parentId == null }.toMutableList()
        if (fromPosition !in parents.indices || toPosition !in parents.indices) return
        val moved = parents.removeAt(fromPosition)
        parents.add(toPosition, moved)
        val childrenByParent = categories.filter { it.parentId != null }.groupBy { it.parentId }
        categories = parents.flatMap { parent ->
            listOf(parent) + childrenByParent[parent.id].orEmpty()
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun parentIdsInOrder(): List<Long> = categories.filter { it.parentId == null }.map { it.id }

    private fun toggle(categoryId: Long) {
        if (!expandedIds.add(categoryId)) expandedIds.remove(categoryId)
        notifyDataSetChanged()
    }

    private fun expandAncestors(categoryId: Long) {
        var current = categories.firstOrNull { it.id == categoryId }
        while (current?.parentId != null) {
            expandedIds.add(current.parentId!!)
            current = categories.firstOrNull { it.id == current.parentId }
        }
    }

    class CategoryGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val parentIcon: ImageView = itemView.findViewById(R.id.image_parent_icon)
        private val parentName: TextView = itemView.findViewById(R.id.text_parent_name)
        private val expandIcon: ImageView = itemView.findViewById(R.id.image_expand)
        private val childrenContainer: LinearLayout = itemView.findViewById(R.id.children_container)

        fun bind(
            parent: Category,
            allCategories: List<Category>,
            expanded: Boolean,
            selectedCategoryId: Long?,
            onToggle: (Long) -> Unit,
            onSelect: (Category) -> Unit
        ) {
            val context = itemView.context
            val children = allCategories.filter { it.parentId == parent.id }
            parentName.text = parent.name
            parentIcon.setImageResource(resolveIcon(context, parent.icon))
            expandIcon.setImageResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
            expandIcon.visibility = if (children.isEmpty()) View.GONE else View.VISIBLE
            childrenContainer.visibility = if (expanded) View.VISIBLE else View.GONE
            itemView.setOnClickListener {
                if (children.isEmpty()) onSelect(parent) else onToggle(parent.id)
            }
            childrenContainer.removeAllViews()
            if (!expanded) return

            var row = createChildRow(context)
            childrenContainer.addView(row)
            children.forEachIndexed { index, child ->
                if (index > 0 && index % CHILDREN_PER_ROW == 0) {
                    row = createChildRow(context)
                    childrenContainer.addView(row)
                }
                val childView = LayoutInflater.from(context)
                    .inflate(R.layout.item_category_child, row, false)
                childView.findViewById<ImageView>(R.id.image_drag_handle).visibility = View.GONE
                childView.findViewById<TextView>(R.id.text_child_name).text = child.name
                childView.findViewById<ImageView>(R.id.image_child_icon)
                    .setImageResource(resolveIcon(context, child.icon))
                val selected = child.id == selectedCategoryId
                childView.findViewById<View>(R.id.child_icon_container).backgroundTintList =
                    ColorStateList.valueOf(
                        ThemeColorHelper.resolveColor(
                            context,
                            if (selected) com.google.android.material.R.attr.colorSecondaryContainer
                            else com.google.android.material.R.attr.colorSurfaceContainerHigh
                        )
                    )
                childView.findViewById<TextView>(R.id.text_child_name).setTextColor(
                    ThemeColorHelper.resolveColor(
                        context,
                        if (selected) com.google.android.material.R.attr.colorSecondary
                        else com.google.android.material.R.attr.colorOnSurfaceVariant
                    )
                )
                childView.setOnClickListener { onSelect(child) }
                row.addView(childView)
            }
            while (row.childCount < CHILDREN_PER_ROW) {
                row.addView(LayoutInflater.from(context).inflate(R.layout.item_category_child, row, false).apply {
                    visibility = View.INVISIBLE
                    isClickable = false
                })
            }
        }

        private fun createChildRow(context: android.content.Context) = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.START
            orientation = LinearLayout.HORIZONTAL
        }

        private fun resolveIcon(context: android.content.Context, iconName: String?): Int =
            iconName?.let { TablerIconCatalog.resourceId(context, it) }
                ?.takeIf { it != 0 }
                ?: R.drawable.ic_category_other
    }

    private companion object {
        const val CHILDREN_PER_ROW = 4
    }
}
