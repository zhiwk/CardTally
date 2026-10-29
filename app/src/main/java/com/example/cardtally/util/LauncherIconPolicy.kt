package com.example.cardtally.util

enum class LauncherIconMode(val value: Int) {
    WHITE(0), DARK(1), SYSTEM(2), APP(3);

    companion object {
        fun fromValue(value: Int): LauncherIconMode = entries.firstOrNull { it.value == value } ?: WHITE
    }
}

object LauncherIconPolicy {
    fun useDark(mode: LauncherIconMode, systemDark: Boolean, appDark: Boolean): Boolean = when (mode) {
        LauncherIconMode.WHITE -> false
        LauncherIconMode.DARK -> true
        LauncherIconMode.SYSTEM -> systemDark
        LauncherIconMode.APP -> appDark
    }
}
