package com.example.cardtally.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.cardtally.R
import java.util.Locale

object LanguageHelper {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "app_language"

    const val LANGUAGE_CHINESE = "zh-CN"
    const val LANGUAGE_ENGLISH = "en"

    fun normalizeLanguageTag(languageTag: String?): String {
        val normalized = languageTag.orEmpty().lowercase(Locale.ROOT)
        return if (normalized.startsWith("en")) LANGUAGE_ENGLISH else LANGUAGE_CHINESE
    }

    fun applySavedLanguage(context: Context) {
        applyLanguage(resolveLanguageTag(context))
    }

    fun updateLanguage(context: Context, languageTag: String) {
        val normalized = normalizeLanguageTag(languageTag)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, normalized)
            .apply()
        applyLanguage(normalized)
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
