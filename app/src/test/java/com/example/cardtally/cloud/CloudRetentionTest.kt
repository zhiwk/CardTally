package com.example.cardtally.cloud

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CloudRetentionTest {
    private fun snapshot(date: Long, device: String = "own", automatic: Boolean = true) =
        CloudSnapshot("$device-$date", date, device, automatic, JSONObject())
    @Test fun defaultFifteenKeepsManualAndOtherDevices() {
        val snapshots = (1L..20L).map { snapshot(it) } + snapshot(0, automatic = false) + snapshot(0, "other")
        assertEquals((1L..5L).toSet(), CloudRetention.expired(snapshots.reversed(), "own", 15).map { it.date }.toSet())
    }
    @Test fun configurableRetentionKeepsNewest() {
        assertEquals(listOf(1L), CloudRetention.expired(listOf(snapshot(1), snapshot(3), snapshot(2)), "own", 2).map { it.date })
        assertTrue(CloudRetention.expired(listOf(snapshot(1)), "own", 365).isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun invalidRetentionCannotDeleteAll() {
        CloudRetention.expired(listOf(snapshot(1)), "own", 0)
    }
}
