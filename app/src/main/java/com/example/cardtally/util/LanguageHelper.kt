package com.example.cardtally.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
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
        // Empty app locales means follow system; never persist a first-launch default.
        val saved = savedLanguage(context)
        applyLanguage(context, saved?.let(::normalizeLanguageTag).orEmpty())
    }

    fun attachLanguageContext(context: Context): Context {
        applySavedLanguage(context)
        val language = resolvePreferredLanguageTag(savedLanguage(context), systemLanguage(context))
        // The Activity's incoming configuration may predate LocaleManager's update.
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
        }
        return context.createConfigurationContext(configuration)
    }

    internal fun resolvePreferredLanguageTag(saved: String?, system: String): String =
        normalizeLanguageTag(saved?.takeIf { it.isNotBlank() } ?: system)

    private fun savedLanguage(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() }

    private fun systemLanguage(context: Context): String =
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).systemLocales[0].toLanguageTag()
        } else {
            Resources.getSystem().configuration.locales[0].toLanguageTag()
        }

    fun updateLanguage(context: Context, languageTag: String) {
        val normalized = normalizeLanguageTag(languageTag)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, normalized)
            .putBoolean(KEY_PENDING_LOCALE_TRANSITION, getCurrentLanguageTag(context) != normalized)
            .apply()
        applyLanguage(context, normalized)
    }

    fun consumePendingLocaleTransition(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(KEY_PENDING_LOCALE_TRANSITION, false)
        if (pending) {
            prefs.edit().putBoolean(KEY_PENDING_LOCALE_TRANSITION, false).apply()
        }
        return pending
    }

    fun getCurrentLanguageTag(context: Context): String =
        normalizeLanguageTag(context.resources.configuration.locales[0].toLanguageTag())

    fun getCurrentLanguageDisplayName(context: Context): String {
        return context.getString(getLanguageDisplayNameResId(getCurrentLanguageTag(context)))
    }

    fun getCurrentLocale(context: Context): Locale {
        return Locale.forLanguageTag(getCurrentLanguageTag(context))
    }

    fun getLanguageDisplayNameResId(languageTag: String): Int {
        return when (normalizeLanguageTag(languageTag)) {
            LANGUAGE_ENGLISH -> R.string.language_option_english
            else -> R.string.language_option_chinese
        }
    }

    private fun applyLanguage(context: Context, languageTag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            // Do not depend on an active AppCompat delegate during cold startup.
            val manager = context.getSystemService(LocaleManager::class.java)
            val locales = LocaleList.forLanguageTags(languageTag)
            if (manager.applicationLocales != locales) manager.applicationLocales = locales
        } else {
            val locales = LocaleListCompat.forLanguageTags(languageTag)
            if (AppCompatDelegate.getApplicationLocales() != locales) {
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }
}
