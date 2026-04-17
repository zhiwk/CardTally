package com.example.cardtally.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.cardtally.R
import java.util.Locale

object LanguageHelper {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "app_language"
    private const val KEY_PENDING_LOCALE_TRANSITION = "pending_locale_transition"

    const val LANGUAGE_CHINESE = "zh-CN"
    const val LANGUAGE_ENGLISH = "en"

    fun normalizeLanguageTag(languageTag: String?): String {
        val normalized = languageTag.orEmpty().lowercase(Locale.ROOT)
        return if (normalized.startsWith("en")) LANGUAGE_ENGLISH else LANGUAGE_CHINESE
    }

    fun applySavedLanguage(context: Context) {
        val targetLanguage = resolveLanguageTag(context)
        if (!isCurrentAppLanguage(targetLanguage)) {
            applyLanguage(targetLanguage)
        }
    }

    fun updateLanguage(context: Context, languageTag: String) {
        val normalized = normalizeLanguageTag(languageTag)
        if (isCurrentAppLanguage(normalized)) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, normalized)
                .putBoolean(KEY_PENDING_LOCALE_TRANSITION, false)
                .apply()
            return
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, normalized)
            .putBoolean(KEY_PENDING_LOCALE_TRANSITION, true)
            .apply()
        applyLanguage(normalized)
    }

    fun consumePendingLocaleTransition(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(KEY_PENDING_LOCALE_TRANSITION, false)
        if (pending) {
            prefs.edit().putBoolean(KEY_PENDING_LOCALE_TRANSITION, false).apply()
        }
        return pending
    }

    fun getCurrentLanguageTag(context: Context): String = resolveLanguageTag(context)

    fun getCurrentLanguageDisplayName(context: Context): String {
        return context.getString(getLanguageDisplayNameResId(resolveLanguageTag(context)))
    }

    fun getCurrentLocale(context: Context): Locale {
        return Locale.forLanguageTag(resolveLanguageTag(context))
    }

    fun getLanguageDisplayNameResId(languageTag: String): Int {
        return when (normalizeLanguageTag(languageTag)) {
            LANGUAGE_ENGLISH -> R.string.language_option_english
            else -> R.string.language_option_chinese
        }
    }

    private fun applyLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    private fun isCurrentAppLanguage(languageTag: String): Boolean {
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (currentTags.isBlank()) {
            return false
        }
        return normalizeLanguageTag(currentTags) == normalizeLanguageTag(languageTag)
    }

    private fun resolveLanguageTag(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (!saved.isNullOrBlank()) {
            return normalizeLanguageTag(saved)
        }

        val appLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (appLocales.isNotBlank()) {
            return normalizeLanguageTag(appLocales)
        }

        return normalizeLanguageTag(Locale.getDefault().toLanguageTag())
    }
}
