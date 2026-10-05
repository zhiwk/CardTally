package com.example.cardtally.util

import android.content.Context
import com.example.cardtally.network.MiniMaxConfig

object AiAssistantSettingsHelper {
    private const val PREFS_NAME = "ai_assistant_settings_prefs"
    private const val KEY_AI_ASSISTANT_ENABLED = "ai_assistant_enabled"
    private const val KEY_AI_API_KEY = "ai_api_key"
    private const val KEY_MINIMAX_MODEL = "minimax_model"
    private const val KEY_MINIMAX_REQUEST_URL = "minimax_base_url"
    private const val KEY_EXPLICIT_CONFIG = "explicit_api_config"
    private const val KEY_ACTIVE_SESSION_ID = "active_session_id"

    fun getRecordToolsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("record_tools_enabled", false)

    fun saveRecordToolsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("record_tools_enabled", enabled).apply()
    }

    fun saveAiAssistantEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AI_ASSISTANT_ENABLED, enabled).apply()
    }

    fun getAiAssistantEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AI_ASSISTANT_ENABLED, false)
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
        val value = prefs.getString(KEY_MINIMAX_MODEL, "").orEmpty().trim()
        // Before explicit configuration, a stored key could rely on implicit MiniMax defaults.
        return value.ifBlank {
            if (isLegacyConfigured(context)) MiniMaxConfig.DEFAULT_MODEL else ""
        }
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
            requestUrl = prefs.getString(KEY_MINIMAX_REQUEST_URL, "").orEmpty().ifBlank {
                if (isLegacyConfigured(context)) MiniMaxConfig.DEFAULT_REQUEST_URL else ""
            }
        ).normalized().requestUrl
    }

    private fun isLegacyConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_EXPLICIT_CONFIG, false) && hasApiKey(context)
    }

    fun saveConfiguration(context: Context, config: MiniMaxConfig) {
        val normalized = config.normalized()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_AI_API_KEY, normalized.apiKey)
            .putString(KEY_MINIMAX_MODEL, normalized.model)
            .putString(KEY_MINIMAX_REQUEST_URL, normalized.requestUrl)
            .putBoolean(KEY_EXPLICIT_CONFIG, true)
            .apply()
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
