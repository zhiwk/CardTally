package com.example.cardtally.adapter

import android.content.Context
import com.example.cardtally.R

/**
 * Pure helpers for the category icon browser, kept Android-free where possible
 * so label parsing can be unit tested on the JVM.
 *
 * The browser shows icons without visible text, so the description is the only
 * name a screen reader gets. It prefers a localized featured label and falls
 * back to a humanized English name, never a raw `tabler_...` catalog key.
 */
object FeaturedIconLabels {

    /**
     * Parses a list of `key=label` entries into a name -> label map.
     * Malformed entries and blank labels are ignored.
     */
    fun parse(entries: List<String>): Map<String, String> {
        return entries.mapNotNull { entry ->
            val separator = entry.indexOf('=')
            if (separator > 0) {
                val key = entry.substring(0, separator).trim()
                val label = entry.substring(separator + 1).trim()
                if (key.isNotEmpty() && label.isNotEmpty()) key to label else null
            } else {
                null
            }
        }.toMap()
    }

    /** Turns `tabler_cash_banknote_plus` into `Cash banknote plus`. */
    fun humanize(iconName: String): String {
        return iconName.removePrefix("tabler_")
            .replace('_', ' ')
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Accessible description for an icon: localized featured label when present,
     * otherwise the humanized English name.
     */
    fun describe(iconName: String, labels: Map<String, String>): String {
        return labels[iconName] ?: humanize(iconName)
    }

    /** Loads the bundled localized featured labels for the current locale. */
    fun load(context: Context): Map<String, String> {
        return try {
            parse(context.resources.getStringArray(R.array.icon_featured_labels).toList())
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
