package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationScrollTailTest {
    @Test
    fun floatingTailClearsTheDockByTheRequestedGap() {
        assertEquals(80, NavigationScrollTail.resolve(76, 8, 12))
    }

    @Test
    fun dockedTailOnlyKeepsTheGap() {
        assertEquals(12, NavigationScrollTail.resolve(0, 0, 12))
    }

    @Test
    fun hiddenDockDoesNotReserveItsShadowRoom() {
        assertEquals(12, NavigationScrollTail.resolve(0, 8, 12))
    }

    @Test
    fun largerDockUsesItsMeasuredHeight() {
        assertEquals(112, NavigationScrollTail.resolve(108, 8, 12))
    }

    @Test
    fun repeatedModeChangesNeverAccumulatePadding() {
        listOf(76, 0, 76, 0, 76).forEach { inset ->
            assertEquals(if (inset == 0) 12 else 80, NavigationScrollTail.resolve(inset, 8, 12))
        }
    }
}
