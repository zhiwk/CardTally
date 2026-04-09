package com.example.cardtally.util

import android.view.View
import android.view.ViewGroup
import com.example.cardtally.R

object FloatingNavLayoutHelper {

    fun applyFabGapAboveBottomNav(fab: View, navShell: View) {
        fab.post {
            navShell.post {
                val fabLayoutParams = fab.layoutParams as? ViewGroup.MarginLayoutParams
                if (fabLayoutParams != null) {
                    val navTop = navShell.topOnScreen()
                    val currentGap = navTop - fab.bottomOnScreen()
                    val expectedGap = fab.resources.getDimensionPixelSize(R.dimen.floating_primary_fab_gap_above_nav)
                    val targetBottomMargin = fabLayoutParams.bottomMargin + (expectedGap - currentGap)

                    if (fabLayoutParams.bottomMargin != targetBottomMargin) {
                        fabLayoutParams.bottomMargin = targetBottomMargin
                        fab.layoutParams = fabLayoutParams
                    }
                }
            }
        }
    }

    private fun View.topOnScreen(): Int {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return location[1]
    }

    private fun View.bottomOnScreen(): Int {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return location[1] + height
    }
}
