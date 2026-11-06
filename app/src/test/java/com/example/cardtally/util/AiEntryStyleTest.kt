package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AiEntryStyleTest {
    @Test
    fun missingOrUnknownPreferencesUseTheSeparateCircle() {
        listOf(null, "", "unknown", true, 1).forEach { value ->
            assertEquals(AiEntryStyle.SEPARATE_CIRCLE, AiEntryStyle.fromPreference(value))
        }
    }

    @Test
    fun savedStylesRoundTripWithoutChangingTheirMeaning() {
        AiEntryStyle.entries.forEach { style ->
            assertEquals(style, AiEntryStyle.fromPreference(style.preferenceValue))
        }
        assertEquals(AiEntryStyle.IN_MENU, AiEntryStyle.fromPreference("menu"))
    }

    @Test
    fun menuPositionValuesAreNotAiEntryStyles() {
        listOf("floating", "docked").forEach { value ->
            assertEquals(AiEntryStyle.SEPARATE_CIRCLE, AiEntryStyle.fromPreference(value))
        }
    }
}
