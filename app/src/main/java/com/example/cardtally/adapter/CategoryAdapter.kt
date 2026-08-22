package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Category
import com.example.cardtally.util.MaterialSymbolCatalog
import com.example.cardtally.util.SwipeToEditDeleteHelper

class CategoryAdapter(
    private var categories: List<Category>,
    private val listener: OnCategoryActionListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    interface OnCategoryActionListener {
        fun onEdit(category: Category)
        fun onDelete(category: Category)
        fun onAddChild(parent: Category)
    }

    private val expandedIds = mutableSetOf<Long>()

    private val parents: List<Category>
        get() = categories.filter { it.parentId == null }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = parents[position]
        val children = categories.filter { it.parentId == category.id }
        val expanded = expandedIds.contains(category.id)

        holder.textCategoryName.text = category.name
        bindIcon(holder.itemView, holder.imageCategoryIcon, category.icon)
        holder.imageExpand.setImageResource(
            if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        )
        holder.childScroll.visibility = if (expanded) View.VISIBLE else View.GONE
        holder.layoutChildren.removeAllViews()

        if (expanded) {
            children.forEach { child ->
                val childView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_category_child, holder.layoutChildren, false)
                bindChild(childView, child)
                childView.setOnClickListener { listener.onEdit(child) }
                holder.layoutChildren.addView(childView)
            }

            val addView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_category_child, holder.layoutChildren, false)
            bindAddChild(addView, category)
            holder.layoutChildren.addView(addView)
        }

        holder.swipeHelper = SwipeToEditDeleteHelper(
            holder.cardContent,
            holder.layoutActions,
            onEdit = { listener.onEdit(category) },
            onDelete = { listener.onDelete(category) },
            onClick = { toggle(category.id) }
        )
    }

    override fun getItemCount(): Int = parents.size

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        val validIds = parents.map { it.id }.toSet()
        expandedIds.retainAll(validIds)
        notifyDataSetChanged()
    }

    private fun toggle(categoryId: Long) {
        if (!expandedIds.add(categoryId)) expandedIds.remove(categoryId)
        val index = parents.indexOfFirst { it.id == categoryId }
        if (index >= 0) notifyItemChanged(index)
    }

    private fun bindChild(view: View, child: Category) {
        view.findViewById<TextView>(R.id.text_child_name).text = child.name
        bindIcon(view, view.findViewById(R.id.image_child_icon), child.icon)
        view.findViewById<ImageView>(R.id.image_drag_handle).visibility = View.VISIBLE
    }

    private fun bindAddChild(view: View, parent: Category) {
        view.findViewById<TextView>(R.id.text_child_name).setText(R.string.category_add_child_cta)
        view.findViewById<ImageView>(R.id.image_child_icon)
            .setImageResource(R.drawable.ms_rounded_add)
        view.findViewById<ImageView>(R.id.image_drag_handle).visibility = View.GONE
        view.setOnClickListener { listener.onAddChild(parent) }
    }

    private fun bindIcon(root: View, imageView: ImageView, icon: String?) {
        val resourceId = icon?.let { name ->
            MaterialSymbolCatalog.resourceId(name).takeIf { it != 0 }
                ?: root.context.resources.getIdentifier(
                    name,
                    "drawable",
                    root.context.packageName
                )
        } ?: 0
        imageView.setImageResource(resourceId.takeIf { it != 0 } ?: R.drawable.ms_rounded_category)
        imageView.visibility = View.VISIBLE
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContent: CardView = itemView.findViewById(R.id.card_content)
        val layoutActions: View = itemView.findViewById(R.id.layout_actions)
        val imageCategoryIcon: ImageView = itemView.findViewById(R.id.image_category_icon)
        val imageExpand: ImageView = itemView.findViewById(R.id.image_expand)
        val textCategoryName: TextView = itemView.findViewById(R.id.text_category_name)
        val childScroll: View = itemView.findViewById(R.id.child_scroll)
        val layoutChildren: LinearLayout = itemView.findViewById(R.id.layout_children)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        var swipeHelper: SwipeToEditDeleteHelper? = null
    }
}
