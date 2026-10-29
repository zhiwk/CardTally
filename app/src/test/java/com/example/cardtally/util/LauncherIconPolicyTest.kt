package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherIconPolicyTest {
    @Test fun invalidAndDefaultValuesUseWhite() {
        assertEquals(LauncherIconMode.WHITE, LauncherIconMode.fromValue(-1))
        assertEquals(LauncherIconMode.WHITE, LauncherIconMode.fromValue(0))
        assertEquals(LauncherIconMode.WHITE, LauncherIconMode.fromValue(99))
        LauncherIconMode.entries.forEach { assertEquals(it, LauncherIconMode.fromValue(it.value)) }
    }

    @Test fun fixedChoicesIgnoreBothThemes() {
        for (system in listOf(false, true)) for (app in listOf(false, true)) {
            assertEquals(false, LauncherIconPolicy.useDark(LauncherIconMode.WHITE, system, app))
            assertEquals(true, LauncherIconPolicy.useDark(LauncherIconMode.DARK, system, app))
        }
    }

    @Test fun systemChoiceIgnoresAppOverride() {
        for (system in listOf(false, true)) for (app in listOf(false, true)) {
            assertEquals(system, LauncherIconPolicy.useDark(LauncherIconMode.SYSTEM, system, app))
        }
    }

    @Test fun appChoiceIgnoresSystemOverride() {
        for (system in listOf(false, true)) for (app in listOf(false, true)) {
            assertEquals(app, LauncherIconPolicy.useDark(LauncherIconMode.APP, system, app))
        }
    }
}
