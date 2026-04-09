package com.example.cardtally.util

import android.content.Context
import com.example.cardtally.network.MiniMaxConfig

object AiAssistantSettingsHelper {
    private const val PREFS_NAME = "ai_assistant_settings_prefs"
    private const val KEY_AI_ASSISTANT_ENABLED = "ai_assistant_enabled"
    private const val KEY_AI_API_KEY = "ai_api_key"
    private const val KEY_MINIMAX_MODEL = "minimax_model"
    private const val KEY_MINIMAX_REQUEST_URL = "minimax_base_url"
    private const val KEY_ACTIVE_SESSION_ID = "active_session_id"

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

    fun saveModel(context: Context, model: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MINIMAX_MODEL, model.trim()).apply()
    }

    fun getModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MINIMAX_MODEL, MiniMaxConfig.DEFAULT_MODEL)
            .orEmpty()
            .trim()
            .ifBlank { MiniMaxConfig.DEFAULT_MODEL }
    }

    fun saveRequestUrl(context: Context, requestUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MINIMAX_REQUEST_URL, requestUrl.trim()).apply()
    }

    fun getRequestUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return MiniMaxConfig(
            apiKey = getApiKey(context),
            model = getModel(context),
            requestUrl = prefs.getString(KEY_MINIMAX_REQUEST_URL, MiniMaxConfig.DEFAULT_REQUEST_URL).orEmpty()
        ).normalized().requestUrl
    }

    fun getMiniMaxConfig(context: Context): MiniMaxConfig {
        return MiniMaxConfig(
            apiKey = getApiKey(context),
            model = getModel(context),
            requestUrl = getRequestUrl(context)
        ).normalized()
    }

    fun isMiniMaxConfigComplete(context: Context): Boolean {
        return getMiniMaxConfig(context).isComplete()
    }

    fun saveActiveSessionId(context: Context, sessionId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_ACTIVE_SESSION_ID, sessionId).apply()
    }

    fun getActiveSessionId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_ACTIVE_SESSION_ID, 0L)
    }
}
