package com.example.cardtally.analytics

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LaunchEventTest {
    @Test fun retriesPreserveEventIdentityAndTimestamp() {
        val first = LaunchEvent.json("test", "installation", "event", 1000, "0.0.4")
        assertEquals(first, LaunchEvent.json("test", "installation", "event", 1000, "0.0.4"))
        val event = JSONObject(first)
        assertEquals("1970-01-01T00:00:01.000Z", event.getString("timestamp"))
        assertEquals("event", event.getString("uuid"))
    }
    @Test fun payloadContainsOnlyApprovedFieldsWithoutProfilesOrGeoEnrichment() {
        val event = JSONObject(LaunchEvent.json("test", "installation", "event", 1000, "0.0.4"))
        assertEquals(setOf("api_key", "distinct_id", "uuid", "timestamp", "event", "properties"), event.keySet())
        assertEquals("app_launch", event.getString("event"))
        val properties = event.getJSONObject("properties")
        assertEquals(setOf("version", "channel", "\$process_person_profile", "\$geoip_disable"), properties.keySet())
        assertFalse(properties.getBoolean("\$process_person_profile"))
        assertTrue(properties.getBoolean("\$geoip_disable"))
        assertEquals("release", properties.getString("channel"))
    }
}
