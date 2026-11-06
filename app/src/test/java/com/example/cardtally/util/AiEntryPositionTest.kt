package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AiEntryPositionTest {
    @Test
    fun missingOrUnknownPreferencesUseTheLeftSide() {
        listOf(null, "", "unknown", true, 1).forEach { value ->
            assertEquals(AiEntryPosition.LEFT, AiEntryPosition.fromPreference(value))
        }
    }

    @Test
    fun explicitlySavedPositionsKeepTheirMeaning() {
        AiEntryPosition.entries.forEach { position ->
            assertEquals(position, AiEntryPosition.fromPreference(position.preferenceValue))
        }
    }

    @Test
    fun styleAndMenuPositionValuesDoNotChangeTheDefaultSide() {
        listOf("menu", "circle", "floating", "docked").forEach { value ->
            assertEquals(AiEntryPosition.LEFT, AiEntryPosition.fromPreference(value))
        }
    }
}
