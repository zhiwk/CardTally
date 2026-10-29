package com.example.cardtally.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.provider.Settings
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs

/** Shared direction, idle restoration and Material animation contract for primary-page FABs. */
class ScrollingFabController(private val root: View, private val changed: () -> Unit) {
    var browsingVisible = true
        private set
    private var active = false
    private var distance = 0
    private val threshold = (24 * root.resources.displayMetrics.density).toInt()
    private val desired = mutableMapOf<FloatingActionButton, Boolean>()
    private val animations = mutableMapOf<FloatingActionButton, Animator>()
    private val trackers = mutableMapOf<FloatingActionButton, AnimatorListenerAdapter>()
    private val restore = Runnable { if (active) reset() }

    fun resume() { active = true; reset() }
    fun reset() { cancelPending(); distance = 0; browsingVisible = true; changed() }
    fun cancelPending() { root.removeCallbacks(restore) }
    fun started() { cancelPending(); distance = 0 }
    fun scrolled(dy: Int) {
        if (!active || dy == 0) return
        cancelPending()
        if ((dy > 0) != (distance > 0)) distance = 0
        distance = (distance + dy).coerceIn(-threshold, threshold)
        if (abs(distance) >= threshold) browsingVisible = distance < 0
        changed()
    }
    fun idle() { cancelPending(); if (active) root.postDelayed(restore, 1500L) }

    fun setVisible(fab: FloatingActionButton, visible: Boolean, immediate: Boolean = false) {
        if (fab !in trackers) {
            val tracker = object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) { animations[fab] = animation }
                override fun onAnimationEnd(animation: Animator) {
                    if (animations[fab] === animation) animations.remove(fab)
                }
            }
            trackers[fab] = tracker
            fab.addOnShowAnimationListener(tracker)
            fab.addOnHideAnimationListener(tracker)
        }
        fab.isEnabled = visible
        fab.isClickable = visible
        fab.isFocusable = visible
        if (!visible) fab.clearFocus()
        fab.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        if (!immediate && desired[fab] == visible) return
        desired[fab] = visible
        val animate = !immediate && fab.isLaidOut && Settings.Global.getFloat(
            root.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) > 0f
        if (animate) {
            if (visible) fab.show() else fab.hide()
        } else {
            animations[fab]?.cancel()
            fab.visibility = if (visible) View.VISIBLE else View.GONE
            fab.alpha = 1f
            fab.scaleX = 1f
            fab.scaleY = 1f
        }
    }

    fun pause() {
        active = false
        cancelPending()
        animations.values.toList().forEach { it.cancel() }
        animations.clear()
        desired.keys.toList().forEach { setVisible(it, false, immediate = true) }
    }
    fun dispose() {
        pause()
        trackers.forEach { (fab, tracker) ->
            fab.removeOnShowAnimationListener(tracker)
            fab.removeOnHideAnimationListener(tracker)
        }
        trackers.clear()
        desired.clear()
    }
}
