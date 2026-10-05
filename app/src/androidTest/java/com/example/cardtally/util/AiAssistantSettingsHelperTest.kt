package com.example.cardtally.util

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.network.MiniMaxConfig
import org.junit.Assert.assertFalse
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
    fun missingSettings_haveNoPresetConfigurationOrActiveSession() {
        assertEquals(0L, AiAssistantSettingsHelper.getActiveSessionId(context))
        assertEquals("", AiAssistantSettingsHelper.getModel(context))
        assertEquals("", AiAssistantSettingsHelper.getRequestUrl(context))
        assertFalse(AiAssistantSettingsHelper.isMiniMaxConfigComplete(context))
    }

    @Test
    fun savedConfiguration_preservesLegacyDefaultsAndExplicitValues() {
        AiAssistantSettingsHelper.saveActiveSessionId(context, 42L)

        assertEquals(42L, AiAssistantSettingsHelper.getActiveSessionId(context))
        // Older versions allowed a stored key to rely on implicit MiniMax defaults.
        AiAssistantSettingsHelper.saveApiKey(context, "legacy-key")
        assertEquals(MiniMaxConfig.DEFAULT_MODEL, AiAssistantSettingsHelper.getModel(context))
        assertEquals(MiniMaxConfig.DEFAULT_REQUEST_URL, AiAssistantSettingsHelper.getRequestUrl(context))
        val custom = MiniMaxConfig("new-key", "chosen-model", "https://service.example/v1/chat/completions")
        AiAssistantSettingsHelper.saveConfiguration(context, custom)
        assertEquals(custom, AiAssistantSettingsHelper.getMiniMaxConfig(context))
        // An explicitly blank configuration must never resurrect legacy defaults.
        AiAssistantSettingsHelper.saveConfiguration(context, MiniMaxConfig(""))
        assertEquals("", AiAssistantSettingsHelper.getModel(context))
        assertEquals("", AiAssistantSettingsHelper.getRequestUrl(context))
    }
}
