package com.example.cardtally.util

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordEntryModePreferencesTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = context.getSharedPreferences("record_entry_mode_prefs", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @Test
    fun missingPreference_defaultsToStandardAndRepairsTheKey() {
        assertEquals(RecordEntryMode.STANDARD, RecordEntryModePreferences.getMode(context))
        assertEquals("standard", preferences.getString("record_entry_mode", null))
    }

    @Test
    fun everyMode_roundTripsThroughStorage() {
        RecordEntryMode.entries.forEach { mode ->
            RecordEntryModePreferences.saveMode(context, mode)
            assertEquals(mode, RecordEntryModePreferences.getMode(context))
        }
    }

    @Test
    fun malformedValues_repairToStandardWithoutClearingSentinel() {
        val malformedWrites: List<(SharedPreferences.Editor) -> Unit> = listOf(
            { it.putString("record_entry_mode", "") },
            { it.putString("record_entry_mode", "future_mode") },
            { it.putLong("record_entry_mode", 3L) },
            { it.putBoolean("record_entry_mode", true) }
        )

        malformedWrites.forEach { writeMalformed ->
            preferences.edit().clear().putString("unrelated_sentinel", "preserve-me").commit()
            val editor = preferences.edit()
            writeMalformed(editor)
            editor.commit()

            assertEquals(RecordEntryMode.STANDARD, RecordEntryModePreferences.getMode(context))
            assertEquals("standard", preferences.getString("record_entry_mode", null))
            assertEquals("preserve-me", preferences.getString("unrelated_sentinel", null))
        }
    }

    @Test
    fun quickMode_persistsAcrossHelperInstances() {
        RecordEntryModePreferences.saveMode(context, RecordEntryMode.QUICK)
        assertEquals(RecordEntryMode.QUICK, RecordEntryModePreferences.getMode(context))
        assertEquals(RecordEntryMode.QUICK, RecordEntryModePreferences.getMode(context))
    }
}
