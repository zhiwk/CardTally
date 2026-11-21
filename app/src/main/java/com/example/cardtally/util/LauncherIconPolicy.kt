package com.example.cardtally.util

enum class LauncherIconMode(val value: Int) {
    WHITE(0), DARK(1);

    companion object {
        fun fromValue(value: Int): LauncherIconMode = entries.firstOrNull { it.value == value } ?: WHITE
    }
}

object LauncherIconPolicy {
    /** Legacy follow modes freeze to the enabled launcher alias, never the current theme. */
    fun resolveMode(storedValue: Int?, darkIconEnabled: Boolean): LauncherIconMode = when (storedValue) {
        2, 3 -> if (darkIconEnabled) LauncherIconMode.DARK else LauncherIconMode.WHITE
        else -> LauncherIconMode.fromValue(storedValue ?: 0)
    }
}
