package com.example.cardtally.util

import com.example.cardtally.R

enum class BottomNavigationDestination(
    val key: String,
    val menuId: Int,
    val titleRes: Int,
    val iconRes: Int
) {
    LEDGER("ledger", R.id.nav_ledger, R.string.nav_ledger, R.drawable.ic_book),
    STATISTICS("statistics", R.id.nav_statistics, R.string.nav_statistics, R.drawable.ic_statistics),
    ASSETS("assets", R.id.nav_asset, R.string.nav_assets, R.drawable.ic_asset),
    AGENT("agent", R.id.nav_agent, R.string.nav_agent, R.drawable.ic_agent),
    SETTINGS("settings", R.id.nav_settings, R.string.nav_me, R.drawable.ic_settings)
}

/** Stable keys, never generated resource IDs or translated labels, are persisted. */
object BottomNavigationOrder {
    val defaultOrder: List<BottomNavigationDestination> = BottomNavigationDestination.entries.toList()

    fun normalize(order: List<BottomNavigationDestination>): List<BottomNavigationDestination> =
        (order + defaultOrder).distinct()

    fun decode(value: Any?): List<BottomNavigationDestination> = normalize(
        (value as? String).orEmpty().split(',').mapNotNull { key ->
            defaultOrder.firstOrNull { it.key == key }
        }
    )

    fun encode(order: List<BottomNavigationDestination>): String = normalize(order).joinToString(",") { it.key }

    /** Only the visible subsequence changes; hidden/independent entries keep their saved slots. */
    fun mergeVisible(
        saved: List<BottomNavigationDestination>,
        visible: List<BottomNavigationDestination>
    ): List<BottomNavigationDestination> {
        val replacements = visible.distinct()
        val included = replacements.toSet()
        val iterator = replacements.iterator()
        return normalize(saved).map { if (it in included) iterator.next() else it }
    }
}
