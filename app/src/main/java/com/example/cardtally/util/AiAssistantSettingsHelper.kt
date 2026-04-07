package com.example.cardtally.util

import android.content.Context

object AiAssistantSettingsHelper {
    private const val PREFS_NAME = "ai_assistant_settings_prefs"
    private const val KEY_AI_ASSISTANT_ENABLED = "ai_assistant_enabled"
    private const val KEY_AI_API_KEY = "ai_api_key"

    fun saveAiAssistantEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AI_ASSISTANT_ENABLED, enabled).apply()
    }

    fun getAiAssistantEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AI_ASSISTANT_ENABLED, true)
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AI_API_KEY, apiKey.trim()).apply()
    }

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AI_API_KEY, "").orEmpty()
    }

    fun hasApiKey(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }
}
