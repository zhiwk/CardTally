package com.example.cardtally.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.provider.Settings
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs

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
    private var browsingVisible = true
    private var returningToTop = false
    private var topEligible = false
    private var directionDistance = 0
    private val directionThreshold = (24 * root.resources.displayMetrics.density).toInt()
    private val desiredVisibility = mutableMapOf<FloatingActionButton, Boolean>()
    private val animations = mutableMapOf<FloatingActionButton, Animator>()
    private val animationTrackers = listOf(add, top).associateWith { fab ->
        object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) { animations[fab] = animation }
            override fun onAnimationEnd(animation: Animator) {
                if (animations[fab] === animation) animations.remove(fab)
            }
        }
    }
    private val restore = Runnable {
        val list = activeList()
        if (active && !disposed && !monthPaging() && !returningToTop &&
            (list == null || list.scrollState == RecyclerView.SCROLL_STATE_IDLE)) {
            browsingVisible = true
            directionDistance = 0
            refresh()
        }
    }
    private val finishReturnIfIdle = object : Runnable {
        override fun run() {
            if (!active || disposed || !returningToTop) return
            val list = activeList() ?: return
            if (list.scrollState == RecyclerView.SCROLL_STATE_IDLE &&
                list.layoutManager?.isSmoothScrolling != true) finishReturn()
            else root.postOnAnimation(this)
        }
    }

    init {
        for (fab in listOf(add, top)) {
            val tracker = animationTrackers.getValue(fab)
            fab.addOnShowAnimationListener(tracker)
            fab.addOnHideAnimationListener(tracker)
        }
    }

    fun resume() {
        if (disposed) return
        active = true
        browsingVisible = true
        returningToTop = false
        directionDistance = 0
        topEligible = false
        refresh()
    }

    fun onMonthSelected() {
        cancelPending()
        returningToTop = false
        directionDistance = 0
        browsingVisible = true
        topEligible = false
        refresh()
    }

    fun onMonthPagingChanged() {
        cancelPending()
        if (monthPaging()) {
            returningToTop = false
            directionDistance = 0
        } else {
            browsingVisible = true
        }
        refresh()
    }

    fun onScrolled(list: RecyclerView, dy: Int) {
        if (!active || disposed || list !== activeList()) return
        // Layout/data restoration may emit onScrolled while idle; it is not a user gesture.
        if (dy != 0 && list.scrollState != RecyclerView.SCROLL_STATE_IDLE && !monthPaging() && !returningToTop) {
            root.removeCallbacks(restore)
            if ((dy > 0) != (directionDistance > 0)) directionDistance = 0
            directionDistance = (directionDistance + dy).coerceIn(-directionThreshold, directionThreshold)
            if (abs(directionDistance) >= directionThreshold) browsingVisible = directionDistance < 0
        }
        refresh()
    }

    fun onScrollStateChanged(list: RecyclerView, state: Int) {
        if (!active || disposed || list !== activeList()) return
        root.removeCallbacks(restore)
        if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            root.removeCallbacks(finishReturnIfIdle)
            returningToTop = false
            directionDistance = 0
        }
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            if (returningToTop) {
                root.removeCallbacks(finishReturnIfIdle)
                root.postOnAnimation(finishReturnIfIdle)
            } else if (!monthPaging()) root.postDelayed(restore, 1500L)
        }
        refresh()
    }

    fun returnToTop() {
        val list = activeList() ?: return
        if (!active || disposed || monthPaging()) return
        list.stopScroll()
        cancelPending()
        returningToTop = true
        directionDistance = 0
        refresh()
        list.smoothScrollToPosition(0)
        // SmoothScroller can be running before RecyclerView enters SETTLING.
        // Also covers a no-op request that never emits a scroll state callback.
        root.postOnAnimation(finishReturnIfIdle)
    }

    private fun finishReturn() {
        root.removeCallbacks(finishReturnIfIdle)
        returningToTop = false
        browsingVisible = true
        directionDistance = 0
        topEligible = false
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
        val show = active && browsingVisible && !monthPaging() && !returningToTop
        setVisible(add, show)
        setVisible(top, show && topEligible)
    }

    private fun setVisible(fab: FloatingActionButton, visible: Boolean, immediate: Boolean = false) {
        fab.isEnabled = visible
        fab.isClickable = visible
        fab.isFocusable = visible
        if (!visible) fab.clearFocus()
        fab.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        if (!immediate && desiredVisibility[fab] == visible) return
        desiredVisibility[fab] = visible
        val animate = !immediate && fab.isLaidOut && Settings.Global.getFloat(
            root.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) > 0f
        if (animate) {
            if (visible) fab.show() else fab.hide()
        } else {
            // Cancel any Material animator before changing visibility without animation.
            animations[fab]?.cancel()
            fab.visibility = if (visible) View.VISIBLE else View.GONE
            fab.alpha = 1f
            fab.scaleX = 1f
            fab.scaleY = 1f
        }
    }

    private fun cancelPending() {
        root.removeCallbacks(restore)
        root.removeCallbacks(finishReturnIfIdle)
    }

    fun pause() {
        active = false
        cancelPending()
        animations.values.toList().forEach { it.cancel() }
        animations.clear()
        setVisible(add, false, immediate = true)
        setVisible(top, false, immediate = true)
    }

    fun dispose() {
        pause()
        disposed = true
        for (fab in listOf(add, top)) {
            val tracker = animationTrackers.getValue(fab)
            fab.removeOnShowAnimationListener(tracker)
            fab.removeOnHideAnimationListener(tracker)
        }
    }
}
