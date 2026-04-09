package com.example.cardtally.util

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiAssistantSettingsHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("ai_assistant_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun activeSessionId_defaultsToZeroWhenMissing() {
        assertEquals(0L, AiAssistantSettingsHelper.getActiveSessionId(context))
    }

    @Test
    fun saveActiveSessionId_persistsSelection() {
        AiAssistantSettingsHelper.saveActiveSessionId(context, 42L)

        assertEquals(42L, AiAssistantSettingsHelper.getActiveSessionId(context))
    }
}
