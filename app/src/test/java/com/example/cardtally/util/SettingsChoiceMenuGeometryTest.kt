package com.example.cardtally.util

import org.junit.Assert.*
import org.junit.Test
import com.example.cardtally.util.SettingsChoiceMenuGeometry.Bounds

class SettingsChoiceMenuGeometryTest {
    @Test fun contentIncludesBothPaddingsAndEveryWrappedRow() {
        assertEquals(160, SettingsChoiceMenuGeometry.contentHeight(listOf(48, 48, 48), 8, 8))
        assertEquals(320, SettingsChoiceMenuGeometry.contentHeight(listOf(80, 104, 120), 8, 8))
    }

    @Test fun shortMenuFitsBelowWithoutScrollingOrDroppingItsFirstRow() {
        val result = SettingsChoiceMenuGeometry.place(Bounds(0, 0, 360, 800), Bounds(20, 100, 340, 156), 280, 160, 64, false)!!
        assertEquals(60, result.left); assertEquals(156, result.top)
        assertEquals(160, result.height); assertFalse(result.scrollable)
    }

    @Test fun belowCannotShowEvenOneOptionFallsBackAbove() {
        val result = SettingsChoiceMenuGeometry.place(Bounds(0, 0, 360, 800), Bounds(20, 700, 340, 756), 280, 160, 64, false)!!
        assertEquals(540, result.top); assertEquals(160, result.height); assertFalse(result.scrollable)
    }

    @Test fun oversizedContentUsesTheLargerSideAndScrolls() {
        val result = SettingsChoiceMenuGeometry.place(Bounds(0, 0, 360, 600), Bounds(20, 300, 340, 356), 280, 448, 64, false)!!
        assertEquals(0, result.top); assertEquals(300, result.height); assertTrue(result.scrollable)
    }

    @Test fun insufficientRoomBelowUsesMoreSpaciousAboveAndShowsAllContent() {
        val result = SettingsChoiceMenuGeometry.place(Bounds(0, 0, 360, 800), Bounds(20, 500, 340, 556), 280, 320, 64, false)!!
        assertEquals(180, result.top); assertEquals(320, result.height); assertFalse(result.scrollable)
    }

    @Test fun fullFitBelowDoesNotFlipJustBecauseAboveHasMoreSpace() {
        val result = SettingsChoiceMenuGeometry.place(Bounds(0, 0, 360, 800), Bounds(20, 500, 340, 556), 280, 160, 64, false)!!
        assertEquals(556, result.top); assertEquals(160, result.height); assertFalse(result.scrollable)
    }

    @Test fun equallyLimitedSidesPreferBelowAndScroll() {
        val result = SettingsChoiceMenuGeometry.place(Bounds(0, 0, 360, 600), Bounds(20, 272, 340, 328), 280, 448, 64, false)!!
        assertEquals(328, result.top); assertEquals(272, result.height); assertTrue(result.scrollable)
    }

    @Test fun widthAndEndAlignmentStayWithinSafeBoundsInBothDirections() {
        val safe = Bounds(8, 20, 352, 800)
        val anchor = Bounds(0, 100, 340, 156)
        assertEquals(344, SettingsChoiceMenuGeometry.place(safe, anchor, 500, 160, 64, false)!!.width)
        assertEquals(60, SettingsChoiceMenuGeometry.place(safe, anchor, 280, 160, 64, false)!!.left)
        assertEquals(8, SettingsChoiceMenuGeometry.place(safe, anchor, 280, 160, 64, true)!!.left)
    }

    @Test fun tinySideSpacesUseTheWindowRatherThanClippingEveryOption() {
        val result = SettingsChoiceMenuGeometry.place(Bounds(0, 0, 360, 200), Bounds(20, 20, 340, 180), 280, 160, 64, false)!!
        assertEquals(0, result.top); assertEquals(160, result.height); assertFalse(result.scrollable)
    }

    @Test fun noVisibleSpaceDoesNotCreateAnUnusablePopup() {
        assertNull(SettingsChoiceMenuGeometry.place(Bounds(0, 0, 360, 0), Bounds(0, 0, 360, 56), 280, 160, 64, false))
    }
}
