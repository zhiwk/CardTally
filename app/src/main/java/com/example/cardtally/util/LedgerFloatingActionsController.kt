package com.example.cardtally.util

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/** View-lifetime controller for the ledger's two existing floating actions. */
class LedgerFloatingActionsController(
    private val root: View,
    private val add: FloatingActionButton,
    private val top: FloatingActionButton,
    private val activeList: () -> RecyclerView?,
    private val monthPaging: () -> Boolean,
    private val topEnabled: () -> Boolean
) {
    private var active = false
    private var disposed = false
    private val scrolling = ScrollingFabController(root) { refresh() }
    private var returningToTop = false
    private var topEligible = false
    private val finishReturnIfIdle = object : Runnable {
        override fun run() {
            if (!active || disposed || !returningToTop) return
            val list = activeList() ?: return
            if (list.scrollState == RecyclerView.SCROLL_STATE_IDLE &&
                list.layoutManager?.isSmoothScrolling != true) finishReturn()
            else root.postOnAnimation(this)
        }
    }

    fun resume() {
        if (disposed) return
        active = true
        returningToTop = false
        topEligible = false
        scrolling.resume()
        refresh()
    }

    fun onMonthSelected() {
        cancelPending()
        returningToTop = false
        topEligible = false
        scrolling.reset()
        refresh()
    }

    fun onMonthPagingChanged() {
        cancelPending()
        if (monthPaging()) {
            returningToTop = false
            scrolling.started()
        } else {
            scrolling.reset()
        }
        refresh()
    }

    fun onScrolled(list: RecyclerView, dy: Int) {
        if (!active || disposed || list !== activeList()) return
        // Layout/data restoration may emit onScrolled while idle; it is not a user gesture.
        if (dy != 0 && list.scrollState != RecyclerView.SCROLL_STATE_IDLE && !monthPaging() && !returningToTop) {
            scrolling.scrolled(dy)
        }
        refresh()
    }

    fun onScrollStateChanged(list: RecyclerView, state: Int) {
        if (!active || disposed || list !== activeList()) return
        scrolling.cancelPending()
        if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            root.removeCallbacks(finishReturnIfIdle)
            returningToTop = false
            scrolling.started()
        }
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            if (returningToTop) {
                root.removeCallbacks(finishReturnIfIdle)
                root.postOnAnimation(finishReturnIfIdle)
            } else if (!monthPaging()) scrolling.idle()
        }
        refresh()
    }

    fun returnToTop() {
        val list = activeList() ?: return
        if (!active || disposed || monthPaging()) return
        list.stopScroll()
        cancelPending()
        returningToTop = true
        scrolling.started()
        refresh()
        list.smoothScrollToPosition(0)
        // SmoothScroller can be running before RecyclerView enters SETTLING.
        // Also covers a no-op request that never emits a scroll state callback.
        root.postOnAnimation(finishReturnIfIdle)
    }

    private fun finishReturn() {
        root.removeCallbacks(finishReturnIfIdle)
        returningToTop = false
        topEligible = false
        scrolling.reset()
        refresh()
    }

    fun refresh() {
        if (disposed) return
        val list = activeList()
        if (!topEnabled() || list == null || list.height <= 0 || !list.canScrollVertically(-1)) {
            topEligible = false
        } else {
            val viewport = (list.height - list.paddingTop - list.paddingBottom).coerceAtLeast(1)
            val distance = list.computeVerticalScrollOffset()
            if (!topEligible && distance > viewport * 2f) topEligible = true
            else if (topEligible && distance < viewport * 1.5f) topEligible = false
        }
        val show = active && scrolling.browsingVisible && !monthPaging() && !returningToTop
        scrolling.setVisible(add, show)
        scrolling.setVisible(top, show && topEligible)
    }

    private fun cancelPending() {
        scrolling.cancelPending()
        root.removeCallbacks(finishReturnIfIdle)
    }

    fun pause() {
        active = false
        cancelPending()
        scrolling.pause()
    }

    fun dispose() {
        pause()
        disposed = true
        scrolling.dispose()
    }
}
