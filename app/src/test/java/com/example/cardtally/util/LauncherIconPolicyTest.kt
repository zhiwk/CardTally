package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherIconPolicyTest {
    @Test fun invalidAndDefaultValuesUseWhite() {
        assertEquals(LauncherIconMode.WHITE, LauncherIconMode.fromValue(-1))
        assertEquals(LauncherIconMode.WHITE, LauncherIconMode.fromValue(0))
        assertEquals(LauncherIconMode.WHITE, LauncherIconMode.fromValue(99))
        for (enabled in listOf(false, true)) {
            for (stored in listOf(null, -1, 99)) {
                assertEquals(LauncherIconMode.WHITE, LauncherIconPolicy.resolveMode(stored, enabled))
            }
        }
        LauncherIconMode.entries.forEach { assertEquals(it, LauncherIconMode.fromValue(it.value)) }
    }

    @Test fun fixedChoicesIgnoreEnabledAlias() {
        assertEquals(listOf(LauncherIconMode.WHITE, LauncherIconMode.DARK), LauncherIconMode.entries.toList())
        for (enabled in listOf(false, true)) {
            assertEquals(LauncherIconMode.WHITE, LauncherIconPolicy.resolveMode(0, enabled))
            assertEquals(LauncherIconMode.DARK, LauncherIconPolicy.resolveMode(1, enabled))
        }
    }

    @Test fun legacyFollowChoicesFreezeToEnabledAlias() {
        for (stored in listOf(2, 3)) {
            assertEquals(LauncherIconMode.WHITE, LauncherIconPolicy.resolveMode(stored, false))
            assertEquals(LauncherIconMode.DARK, LauncherIconPolicy.resolveMode(stored, true))
        }
    }

    @Test fun migratedChoicesStayFixedWhenEnabledAliasChanges() {
        for (stored in listOf(2, 3)) for (enabled in listOf(false, true)) {
            val migrated = LauncherIconPolicy.resolveMode(stored, enabled)
            assertEquals(migrated, LauncherIconPolicy.resolveMode(migrated.value, !enabled))
        }
    }
}
