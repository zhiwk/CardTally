package com.example.cardtally.util

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.DateGroupAdapter

class RecordDragCallback(
    private val adapter: DateGroupAdapter,
    private val onRecordMoved: (fromPosition: Int, toPosition: Int) -> Unit,
    private val onEnterMultiSelectMode: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    0
) {

    private var hasMoved = false
    private var isLongPressTriggered = false
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var currentPosition = RecyclerView.NO_POSITION

    override fun getDragDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (adapter.isMultiSelectMode()) {
            0
        } else {
            super.getDragDirs(recyclerView, viewHolder)
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        
        if (adapter.getItemViewType(fromPosition) != DateGroupAdapter.TYPE_RECORD ||
            adapter.getItemViewType(toPosition) != DateGroupAdapter.TYPE_RECORD) {
            return false
        }
        
        val fromDate = adapter.getDateForPosition(fromPosition)
        val toDate = adapter.getDateForPosition(toPosition)
        
        if (fromDate != toDate) {
            return false
        }
        
        hasMoved = true
        cancelLongPressTimer()
        adapter.moveItem(fromPosition, toPosition)
        onRecordMoved(fromPosition, toPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            hasMoved = false
            isLongPressTriggered = false
            currentPosition = viewHolder?.adapterPosition ?: RecyclerView.NO_POSITION
            
            if (currentPosition != RecyclerView.NO_POSITION) {
                startLongPressTimer(currentPosition)
            }
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            cancelLongPressTimer()
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        cancelLongPressTimer()
        hasMoved = false
    }

    private fun startLongPressTimer(position: Int) {
        cancelLongPressTimer()
        
        longPressRunnable = Runnable {
            if (!hasMoved && !isLongPressTriggered) {
                isLongPressTriggered = true
                onEnterMultiSelectMode(position)
            }
        }
        
        handler.postDelayed(longPressRunnable!!, 800)
    }

    private fun cancelLongPressTimer() {
        longPressRunnable?.let {
            handler.removeCallbacks(it)
        }
        longPressRunnable = null
    }
}
