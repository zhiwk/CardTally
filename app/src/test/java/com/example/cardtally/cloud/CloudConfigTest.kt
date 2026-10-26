package com.example.cardtally.cloud

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CloudConfigTest {
    private fun dav() = CloudConfig("https://example.invalid/dav/", "example-user", "example-password",
        "cardtally", "example-backup-password", true, 15)

    @Test fun webDavConfigurationRoundTripsWithoutRemovedProviderFields() {
        val json = dav().json()
        assertEquals(dav(), CloudConfig.from(json))
        for (name in listOf("bucket", "region", "sessionToken", "pathStyle")) assertFalse(json.has(name))
        assertEquals("webdav", json.getString("type"))
    }

    @Test fun oldWebDavExtraFieldsAreIgnoredWithoutChangingLoginOrBackupOptions() {
        val json = dav().json().put("bucket", "").put("region", "us-east-1").put("pathStyle", true).put("sessionToken", "")
        assertEquals(dav(), CloudConfig.from(json))
    }

    @Test fun unsupportedTypesCannotBeDecodedAsWebDav() {
        for (type in listOf("s3", "unknown", "")) {
            try { CloudConfig.from(dav().json().put("type", type)); fail("Expected provider rejection") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun unsupportedAutomaticTargetDoesNotFallBackToDormantWebDav() {
        val root = JSONObject().put("active", "s3").put("s3", JSONObject().put("automatic", true)).put("webdav", dav().json())
        assertTrue(CloudSettings.unsupportedActive(root))
        assertNull(CloudSettings.activeConfig(root))
    }

    @Test fun existingActiveWebDavAndEmptySettingsKeepTheirBehavior() {
        val root = JSONObject().put("active", "webdav").put("webdav", dav().json())
        assertFalse(CloudSettings.unsupportedActive(root))
        assertEquals(dav(), CloudSettings.activeConfig(root))
        assertNull(CloudSettings.activeConfig(JSONObject()))
    }
}
