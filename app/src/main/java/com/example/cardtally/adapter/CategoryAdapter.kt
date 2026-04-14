package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.updatePaddingRelative
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Category
import com.example.cardtally.util.SwipeToEditDeleteHelper

class CategoryAdapter(
    private var categories: List<Category>,
    private var categoryDepths: Map<Long, Int>,
    private val listener: OnCategoryActionListener
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    interface OnCategoryActionListener {
        fun onEdit(category: Category)
        fun onDelete(category: Category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val depth = categoryDepths[category.id] ?: 0
        holder.textCategoryName.text = category.name
        holder.layoutContent.updatePaddingRelative(
            start = holder.basePaddingStart + depth * holder.depthIndentPx,
            top = holder.layoutContent.paddingTop,
            end = holder.layoutContent.paddingEnd,
            bottom = holder.layoutContent.paddingBottom
        )

        if (!category.icon.isNullOrEmpty()) {
            val resourceId = holder.itemView.context.resources.getIdentifier(
                category.icon,
                "drawable",
                holder.itemView.context.packageName
            )
            if (resourceId != 0) {
                holder.imageCategoryIcon.setImageResource(resourceId)
                holder.imageCategoryIcon.visibility = View.VISIBLE
            } else {
                holder.imageCategoryIcon.visibility = View.GONE
            }
        } else {
            holder.imageCategoryIcon.visibility = View.GONE
        }

        holder.swipeHelper = SwipeToEditDeleteHelper(
            holder.cardContent,
            holder.layoutActions,
            onEdit = { listener.onEdit(category) },
            onDelete = { listener.onDelete(category) }
        )
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<Category>, newCategoryDepths: Map<Long, Int>) {
        categories = newCategories
        categoryDepths = newCategoryDepths
        notifyDataSetChanged()
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContent: CardView = itemView.findViewById(R.id.card_content)
        val layoutContent: View = itemView.findViewById(R.id.layout_content)
        val layoutActions: View = itemView.findViewById(R.id.layout_actions)
        val imageCategoryIcon: ImageView = itemView.findViewById(R.id.image_category_icon)
        val textCategoryName: TextView = itemView.findViewById(R.id.text_category_name)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        val basePaddingStart: Int = layoutContent.paddingStart
        val depthIndentPx: Int = itemView.resources.getDimensionPixelSize(R.dimen.category_depth_indent)
        var swipeHelper: SwipeToEditDeleteHelper? = null
    }
}
