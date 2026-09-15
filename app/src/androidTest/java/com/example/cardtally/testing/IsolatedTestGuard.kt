package com.example.cardtally.testing

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume

/**
 * Safety gate for device tests.
 *
 * Several instrumentation tests reset the app database by deleting `CardTally.db`.
 * That is only acceptable on the `verification` flavor, which installs under its
 * own applicationId (`com.example.cardtally.verification`) and therefore has its
 * own sandbox. On the everyday `dev` build the tests are skipped instead of
 * wiping the user's real ledger, records and assets.
 *
 * Run them with `:app:connectedVerificationDebugAndroidTest`.
 */
object IsolatedTestGuard {

    const val ISOLATED_APPLICATION_ID_SUFFIX = ".verification"

    val isIsolatedBuild: Boolean
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.packageName
            .endsWith(ISOLATED_APPLICATION_ID_SUFFIX)

    /** Skips the calling test unless it is running against the isolated build. */
    fun requireIsolatedBuild() {
        Assume.assumeTrue(
            "Device test skipped: it resets the app database and only runs on the " +
                "isolated verification flavor (run :app:connectedVerificationDebugAndroidTest).",
            isIsolatedBuild
        )
    }
}
