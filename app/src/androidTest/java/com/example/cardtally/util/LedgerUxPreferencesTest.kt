package com.example.cardtally.util

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerUxPreferencesTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = context.getSharedPreferences("ledger_ux_prefs", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @Test
    fun legalValues_roundTripAndResolveEveryStartupBehavior() {
        // Given: every approved startup and last-view token
        LedgerStartupBehavior.entries.forEach { startup ->
            LedgerUxPreferences.saveStartupBehavior(context, startup)
            assertEquals(startup, LedgerUxPreferences.getStartupBehavior(context))
        }
        LedgerView.entries.forEach { view ->
            LedgerUxPreferences.saveLastView(context, view)
            assertEquals(view, LedgerUxPreferences.getLastView(context))
        }

        // When: each startup behavior is resolved
        LedgerUxPreferences.saveLastView(context, LedgerView.STATISTICS_INCOME)

        // Then: details/statistics/remember-last map exactly
        assertEquals(LedgerView.DETAILS, LedgerUxPreferences.resolveStartupView(context, LedgerStartupBehavior.DETAILS))
        assertEquals(LedgerView.STATISTICS_EXPENSE, LedgerUxPreferences.resolveStartupView(context, LedgerStartupBehavior.STATISTICS))
        assertEquals(LedgerView.STATISTICS_INCOME, LedgerUxPreferences.resolveStartupView(context, LedgerStartupBehavior.REMEMBER_LAST))
    }

    @Test
    fun firstRead_migratesDefaultsAndPreservesUnrelatedPreference() {
        // Given: missing managed keys and unrelated state
        preferences.edit().putString("unrelated_sentinel", "preserve-me").commit()

        // When: the helper performs its first read
        val startup = LedgerUxPreferences.getStartupBehavior(context)

        // Then: details defaults and schema 1 are installed key-locally
        assertEquals(LedgerStartupBehavior.DETAILS, startup)
        assertEquals(1, preferences.getInt("ledger_ux_schema_version", 0))

        // Given: a wrong-typed schema marker
        preferences.edit().clear().putString("ledger_ux_schema_version", "one")
            .putString("unrelated_sentinel", "preserve-me").commit()

        // When: schema parsing crosses the helper boundary
        LedgerUxPreferences.getStartupBehavior(context)

        // Then: it is treated as version zero and migrated
        assertEquals(1, preferences.getInt("ledger_ux_schema_version", 0))
        assertEquals("details", preferences.getString("ledger_startup_behavior", null))
        assertEquals("details", preferences.getString("ledger_last_view", null))
        assertEquals("preserve-me", preferences.getString("unrelated_sentinel", null))
    }

    @Test
    fun malformedEnums_repairBlankUnknownAndWrongTypeWithoutClearingSentinel() {
        // Given: every invalid enum storage class
        val malformedWrites: List<Pair<String, (SharedPreferences.Editor) -> Unit>> = listOf(
            "ledger_startup_behavior" to { it.putString("ledger_startup_behavior", "") },
            "ledger_startup_behavior" to { it.putString("ledger_startup_behavior", "future") },
            "ledger_startup_behavior" to { it.putLong("ledger_startup_behavior", 1L) },
            "ledger_last_view" to { it.putString("ledger_last_view", "") },
            "ledger_last_view" to { it.putString("ledger_last_view", "future") },
            "ledger_last_view" to { it.putBoolean("ledger_last_view", true) }
        )

        malformedWrites.forEach { (key, writeMalformed) ->
            preferences.edit().clear().putInt("ledger_ux_schema_version", 1)
                .putString("unrelated_sentinel", "preserve-me").commit()
            val editor = preferences.edit()
            writeMalformed(editor)
            editor.commit()

            // When: malformed storage is parsed
            if (key == "ledger_startup_behavior") {
                assertEquals(LedgerStartupBehavior.DETAILS, LedgerUxPreferences.getStartupBehavior(context))
            } else {
                assertEquals(LedgerView.DETAILS, LedgerUxPreferences.getLastView(context))
            }

            // Then: only the affected key is normalized
            assertEquals("details", preferences.getString(key, null))
            assertEquals("preserve-me", preferences.getString("unrelated_sentinel", null))
        }
    }

    @Test
    fun schemaVersions_migrateLowerButLeaveFutureStorageIntact() {
        // Given: a lower schema
        preferences.edit().putInt("ledger_ux_schema_version", 0).commit()

        // When: migration runs
        LedgerUxPreferences.getLastView(context)

        // Then: schema 1 is installed
        assertEquals(1, preferences.getInt("ledger_ux_schema_version", 0))

        // Given: a future schema and future token
        preferences.edit().clear().putInt("ledger_ux_schema_version", 2)
            .putString("ledger_last_view", "future_view").commit()

        // When: the future token is parsed
        val view = LedgerUxPreferences.getLastView(context)

        // Then: runtime is safe and future storage is untouched
        assertEquals(LedgerView.DETAILS, view)
        assertEquals(2, preferences.getInt("ledger_ux_schema_version", 0))
        assertEquals("future_view", preferences.getString("ledger_last_view", null))
    }
}
