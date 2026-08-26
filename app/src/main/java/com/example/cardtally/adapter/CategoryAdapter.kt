package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.ClipData
import android.view.DragEvent
import android.view.Gravity
import android.view.View.DragShadowBuilder
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
        fun onChildOrderChanged(children: List<Category>)
    }

    private val expandedIds = mutableSetOf<Long>()
    private var activeDraggedChildId: Long? = null

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

        holder.cardContent.translationX = 0f
        holder.textCategoryName.text = category.name
        bindIcon(holder.itemView, holder.imageCategoryIcon, category.icon)
        holder.imageExpand.setImageResource(
            if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        )
        holder.imageExpand.isClickable = true
        holder.imageExpand.setOnClickListener { toggle(category.id) }
        holder.childScroll.visibility = if (expanded) View.VISIBLE else View.GONE
        holder.layoutChildren.removeAllViews()

        if (expanded) {
            bindChildGrid(holder, category, children)
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

    fun moveParent(fromPosition: Int, toPosition: Int) {
        val orderedParents = parents.toMutableList()
        if (fromPosition !in orderedParents.indices || toPosition !in orderedParents.indices) return
        val moved = orderedParents.removeAt(fromPosition)
        orderedParents.add(toPosition, moved)
        val childrenByParent = categories.filter { it.parentId != null }.groupBy { it.parentId }
        categories = orderedParents.flatMap { parent ->
            listOf(parent) + childrenByParent[parent.id].orEmpty()
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun parentIdsInOrder(): List<Long> = parents.map { it.id }

    private fun bindChildGrid(
        holder: CategoryViewHolder,
        parent: Category,
        children: List<Category>
    ) {
        val context = holder.itemView.context
        var row: LinearLayout? = null
        children.forEachIndexed { index, child ->
            if (index % CHILDREN_PER_ROW == 0) {
                row = createChildRow(context)
                holder.layoutChildren.addView(row)
            }
            val childView = LayoutInflater.from(context)
                .inflate(R.layout.item_category_child, row, false)
            bindChild(childView, child)
            childView.tag = child
            childView.setOnClickListener { listener.onEdit(child) }
            childView.setOnLongClickListener {
                // Keep the original tile in the grid layout, but hide its content while
                // Android renders the drag shadow. This prevents the old icon and the
                // moving icon from being visible at the same time.
                childView.alpha = 0f
                activeDraggedChildId = child.id
                val data = ClipData.newPlainText("category", child.id.toString())
                childView.startDragAndDrop(data, DragShadowBuilder(childView), childView, 0)
                true
            }
            row?.addView(childView)
        }

        if (children.isEmpty() || children.size % CHILDREN_PER_ROW == 0) {
            row = createChildRow(context)
            holder.layoutChildren.addView(row)
        }
        val addView = LayoutInflater.from(context)
            .inflate(R.layout.item_category_child, row, false)
        bindAddChild(addView, parent)
        row?.addView(addView)
        while ((row?.childCount ?: 0) < CHILDREN_PER_ROW) {
            row?.addView(LayoutInflater.from(context).inflate(R.layout.item_category_child, row, false).apply {
                visibility = View.INVISIBLE
                isClickable = false
            })
        }
        holder.layoutChildren.setOnDragListener { view, event ->
            val draggedView = event.localState as? View ?: return@setOnDragListener true
            val draggedCategory = draggedView.tag as? Category ?: return@setOnDragListener true
            val root = view as? ViewGroup ?: return@setOnDragListener true

            when (event.action) {
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val target = findClosestChild(root, event.x, event.y, draggedView)
                    val targetCategory = target?.tag as? Category
                    if (targetCategory != null && draggedCategory.id != targetCategory.id) {
                        // Rebind the compact child grid as the pointer crosses tiles so
                        // the order visibly follows the drag before the drop is released.
                        moveChild(parent, draggedCategory.id, targetCategory.id, persist = false)
                        // Do not notify the RecyclerView while Android is holding the
                        // drag source. Rebinding the parent card here leaves the old
                        // source view alive and produces the duplicated/merged icon
                        // seen during a child drag. Rebuild only the nested grid.
                        holder.layoutChildren.removeAllViews()
                        bindChildGrid(
                            holder,
                            parent,
                            categories.filter { it.parentId == parent.id }
                        )
                    }
                }
                DragEvent.ACTION_DROP -> {
                    // The list has already been reordered during the drag. Persist only
                    // once, after the user releases the item.
                    listener.onChildOrderChanged(
                        categories.filter { it.parentId == parent.id }
                    )
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    activeDraggedChildId = null
                    draggedView.alpha = 1f
                    holder.layoutChildren.removeAllViews()
                    bindChildGrid(
                        holder,
                        parent,
                        categories.filter { it.parentId == parent.id }
                    )
                }
            }
            true
        }
    }

    private fun createChildRow(context: android.content.Context): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_HORIZONTAL
            orientation = LinearLayout.HORIZONTAL
        }
    }

    private fun findClosestChild(root: ViewGroup, x: Float, y: Float, ignored: View): View? {
        val rootLocation = IntArray(2)
        root.getLocationOnScreen(rootLocation)
        return childViews(root)
            .filter { it !== ignored && it.tag is Category }
            .minByOrNull { child ->
                val location = IntArray(2)
                child.getLocationOnScreen(location)
                val centerX = location[0] - rootLocation[0] + child.width / 2f
                val centerY = location[1] - rootLocation[1] + child.height / 2f
                (centerX - x) * (centerX - x) + (centerY - y) * (centerY - y)
            }
    }

    private fun childViews(root: ViewGroup): List<View> {
        val result = mutableListOf<View>()
        for (index in 0 until root.childCount) {
            val child = root.getChildAt(index)
            if (child is ViewGroup) {
                if (child.tag is Category) result.add(child)
                result += childViews(child)
            }
        }
        return result
    }

    private fun moveChild(parent: Category, fromId: Long, targetId: Long, persist: Boolean = true) {
        val children = categories.filter { it.parentId == parent.id }.toMutableList()
        val fromIndex = children.indexOfFirst { it.id == fromId }
        val targetIndex = children.indexOfFirst { it.id == targetId }
        if (fromIndex < 0 || targetIndex < 0) return
        val moved = children.removeAt(fromIndex)
        children.add(targetIndex, moved)
        val childrenByParent = categories.filter { it.parentId != null }.groupBy { it.parentId }
        val reorderedChildren = childrenByParent.toMutableMap()
        reorderedChildren[parent.id] = children
        categories = parents.flatMap { category ->
            listOf(category) + reorderedChildren[category.id].orEmpty()
        }
        if (persist) listener.onChildOrderChanged(children)
    }

    private fun toggle(categoryId: Long) {
        if (!expandedIds.add(categoryId)) expandedIds.remove(categoryId)
        val index = parents.indexOfFirst { it.id == categoryId }
        if (index >= 0) notifyItemChanged(index)
    }

    private fun bindChild(view: View, child: Category) {
        view.alpha = if (activeDraggedChildId == child.id) 0f else 1f
        view.findViewById<TextView>(R.id.text_child_name).text = child.name
        bindIcon(view, view.findViewById(R.id.image_child_icon), child.icon)
        // Reordering is gesture-based; an external handle would overlap the compact icon tile.
        view.findViewById<ImageView>(R.id.image_drag_handle).visibility = View.GONE
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

    companion object {
        private const val CHILDREN_PER_ROW = 4
    }
}
