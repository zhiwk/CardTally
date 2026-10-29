package com.example.cardtally.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import com.example.cardtally.util.ScrollingFabController
import com.google.android.material.floatingactionbutton.FloatingActionButton

/** Reports actual touch/fling scrolling, not layout or saved-position restoration. */
class FabScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {
    private var controller: ScrollingFabController? = null
    private var touching = false
    private var flinging = false
    private var active = false

    fun bindFab(fab: FloatingActionButton) {
        controller?.dispose()
        controller = ScrollingFabController(this) {
            controller?.let { it.setVisible(fab, active && it.browsingVisible) }
        }
    }
    fun resumeFab() { active = true; controller?.resume() }
    fun pauseFab() { active = false; touching = false; flinging = false; controller?.pause() }
    fun disposeFab() { pauseFab(); controller?.dispose(); controller = null }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            touching = true
            controller?.started()
        }
        val handled = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            touching = false
            if (!flinging) controller?.idle()
        }
        return handled
    }

    override fun fling(velocityY: Int) {
        flinging = true
        controller?.cancelPending()
        super.fling(velocityY)
    }

    override fun stopNestedScroll(type: Int) {
        super.stopNestedScroll(type)
        // NestedScrollView ends its non-touch nested scroll when its scroller finishes,
        // even when there is no nested-scrolling parent.
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            flinging = false
            if (!touching) controller?.idle()
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (touching || flinging) controller?.scrolled(t - oldt)
    }
}
