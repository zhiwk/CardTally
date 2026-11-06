package com.example.cardtally.adapter

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.util.BottomNavigationDestination

/** A short, non-scrolling list inside the existing settings page. Save only settled moves. */
internal class BottomNavigationOrderAdapter(
    private val onOrderSettled: (List<BottomNavigationDestination>) -> Unit
) : RecyclerView.Adapter<BottomNavigationOrderAdapter.Holder>() {
    private val items = mutableListOf<BottomNavigationDestination>()
    private var touchHelper: ItemTouchHelper? = null
    private var dirty = false

    fun attach(list: RecyclerView) {
        list.layoutManager = LinearLayoutManager(list.context)
        list.isNestedScrollingEnabled = false
        list.adapter = this
        touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled() = false
            override fun onMove(list: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder) =
                move(from.bindingAdapterPosition, to.bindingAdapterPosition)
            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun clearView(list: RecyclerView, holder: RecyclerView.ViewHolder) {
                super.clearView(list, holder)
                finishMove()
            }
        }).also { it.attachToRecyclerView(list) }
    }

    fun submitOrder(order: List<BottomNavigationDestination>) {
        finishMove()
        if (items == order) return
        items.clear()
        items.addAll(order)
        notifyDataSetChanged()
    }

    private fun move(from: Int, to: Int): Boolean {
        if (from !in items.indices || to !in items.indices || from == to) return false
        items.add(to, items.removeAt(from))
        dirty = true
        notifyItemMoved(from, to)
        return true
    }

    fun finishMove() {
        if (!dirty) return
        dirty = false
        onOrderSettled(items.toList())
    }

    fun detach() {
        finishMove()
        touchHelper?.attachToRecyclerView(null)
        touchHelper = null
    }

    override fun getItemCount() = items.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val holder = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_bottom_navigation_order, parent, false))
        holder.itemView.findViewById<View>(R.id.handle_navigation_order).setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                // ItemTouchHelper blocks the outer scroll parent itself. Requesting this from
                // the handle also blocks RecyclerView from intercepting the subsequent MOVE.
                touchHelper?.startDrag(holder)
            }
            false
        }
        ViewCompat.setAccessibilityDelegate(holder.itemView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                val position = holder.bindingAdapterPosition
                if (position !in items.indices) return
                if (position > 0) info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_navigation_order_up, host.context.getString(R.string.navigation_order_move_up)))
                if (position < items.lastIndex) info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_navigation_order_down, host.context.getString(R.string.navigation_order_move_down)))
            }

            override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                val from = holder.bindingAdapterPosition
                val to = when (action) {
                    R.id.action_navigation_order_up -> from - 1
                    R.id.action_navigation_order_down -> from + 1
                    else -> return super.performAccessibilityAction(host, action, args)
                }
                if (!move(from, to)) return false
                finishMove()
                return true
            }
        })
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val destination = items[position]
        holder.itemView.findViewById<ImageView>(R.id.icon_navigation_order).setImageResource(destination.iconRes)
        holder.itemView.findViewById<TextView>(R.id.text_navigation_order).setText(destination.titleRes)
        holder.itemView.contentDescription = holder.itemView.context.getString(destination.titleRes)
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view)
}
