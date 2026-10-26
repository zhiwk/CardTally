package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRowLayoutTest {
    @Test fun resultWidthReservesTheMajorityForTheTitle() {
        assertEquals(126, SettingsRowLayout.valueWidthLimit(300))
        assertTrue(SettingsRowLayout.valueWidthLimit(240) < 240 / 2)
    }

    @Test fun widthTracksWindowSizeAndHandlesUnavailableSpace() {
        assertEquals(84, SettingsRowLayout.valueWidthLimit(200))
        assertEquals(168, SettingsRowLayout.valueWidthLimit(400))
        assertEquals(0, SettingsRowLayout.valueWidthLimit(0))
        assertEquals(0, SettingsRowLayout.valueWidthLimit(-20))
    }
}
