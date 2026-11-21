package com.example.cardtally.cloud

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WebDavQrCodecTest {
    // Synthetic credentials only; never use a real account or backup password in fixtures.
    private fun connection(password: String? = null) = WebDavQrConnection(
        "https://example.invalid/dav/", "example-user", "example-login-password", "cardtally/设备", password)
    private fun json() = JSONObject(WebDavQrCodec.encode(connection()).removePrefix(WebDavQrCodec.HEADER))
    private fun payload(json: JSONObject) = WebDavQrCodec.HEADER + json.toString()
    private fun rejected(action: () -> Unit) {
        try { action(); fail("Expected invalid QR rejection") }
        catch (_: IllegalArgumentException) { }
        catch (_: org.json.JSONException) { }
    }

    @Test fun defaultTransferIncludesLoginButNotBackupPasswordOrTaskSettings() {
        val encoded = WebDavQrCodec.encode(connection())
        val json = json()
        assertEquals(setOf("version", "endpoint", "user", "secret", "prefix"), json.keys().asSequence().toSet())
        val decoded = WebDavQrCodec.decode(encoded)
        assertEquals(connection().endpoint, decoded.endpoint)
        assertEquals(connection().user, decoded.user)
        assertEquals(connection().secret, decoded.secret)
        assertEquals(connection().prefix, decoded.prefix)
        assertNull(decoded.backupPassword)
        assertFalse(json.has("automatic"))
        assertFalse(json.has("keep"))
        assertFalse(json.has("device"))
    }

    @Test fun explicitlyIncludedBackupPasswordAndLoginWhitespaceRoundTripExactly() {
        val source = WebDavQrConnection(connection().endpoint, "测试用户", " 登录🔑&\"[]{}密码 ",
            "备份/子目录", " 备份🔒&\"密码 ")
        val decoded = WebDavQrCodec.decode(WebDavQrCodec.encode(source))
        assertEquals(source.user, decoded.user)
        assertEquals(source.secret, decoded.secret)
        assertEquals(source.prefix, decoded.prefix)
        assertEquals(source.backupPassword, decoded.backupPassword)
    }

    @Test fun importNormalizesOnlyAddressUsernameAndDirectory() {
        val json = json().put("endpoint", " ${connection().endpoint} ")
            .put("user", " example-user ").put("prefix", " cardtally ")
            .put("secret", " password with spaces ")
        val decoded = WebDavQrCodec.decode(payload(json))
        assertEquals(connection().endpoint, decoded.endpoint)
        assertEquals("example-user", decoded.user)
        assertEquals("cardtally", decoded.prefix)
        assertEquals(" password with spaces ", decoded.secret)
    }

    @Test fun rejectsOtherFormatsVersionsAndTrailingData() {
        for (raw in listOf("", "https://example.invalid/dav/", "other:" + json(),
            WebDavQrCodec.HEADER + "[]", payload(json()) + " extra")) {
            rejected { WebDavQrCodec.decode(raw) }
        }
        for (version in listOf<Any>(0, 2, "1", true, 1.5)) {
            rejected { WebDavQrCodec.decode(payload(json().put("version", version))) }
        }
    }

    @Test fun rejectsMissingUnknownNullAndWronglyTypedFields() {
        for (name in listOf("version", "endpoint", "user", "secret", "prefix")) {
            rejected { WebDavQrCodec.decode(payload(json().apply { remove(name) })) }
            rejected { WebDavQrCodec.decode(payload(json().put(name, JSONObject.NULL))) }
            rejected { WebDavQrCodec.decode(payload(json().put(name, JSONObject()))) }
        }
        for (name in listOf("automatic", "keep", "device", "active", "type")) {
            rejected { WebDavQrCodec.decode(payload(json().put(name, true))) }
        }
        rejected { WebDavQrCodec.decode(payload(json().put("backupPassword", JSONObject.NULL))) }
        rejected { WebDavQrCodec.decode(payload(json().put("backupPassword", 12345678))) }
        val nested = "{\"a\":".repeat(150) + "0" + "}".repeat(150)
        rejected { WebDavQrCodec.decode(WebDavQrCodec.HEADER + "{\"secret\":$nested}") }
        rejected { WebDavQrCodec.decode(WebDavQrCodec.HEADER + json().toString().dropLast(1) + ",\"secret\":\"duplicate\"}") }
    }

    @Test fun sharedEndpointRulesRejectInsecureOrAmbiguousAddresses() {
        for (endpoint in listOf("http://example.invalid/dav/", "file:///dav/", "not a URL",
            "https://user:pass@example.invalid/dav/", "https://example.invalid/?a=1",
            "https://example.invalid/#fragment")) {
            assertFalse(CloudConnectionRules.validEndpoint(endpoint))
            rejected { WebDavQrCodec.decode(payload(json().put("endpoint", endpoint))) }
            rejected { WebDavQrCodec.encode(WebDavQrConnection(endpoint, "user", "secret", "")) }
        }
        assertTrue(CloudConnectionRules.validEndpoint("https://example.invalid:8443/dav/"))
    }

    @Test fun sharedDirectoryRulesRejectTraversalButAllowEmptyAndNestedPaths() {
        for (prefix in listOf(".", "..", "/a/../b/", "./a")) {
            assertFalse(CloudConnectionRules.validPrefix(prefix))
            rejected { WebDavQrCodec.decode(payload(json().put("prefix", prefix))) }
        }
        for (prefix in listOf("", "/", "a/b", "备份/子目录")) {
            assertTrue(CloudConnectionRules.validPrefix(prefix))
            assertEquals(prefix, WebDavQrCodec.decode(payload(json().put("prefix", prefix))).prefix)
        }
    }

    @Test fun rejectsBlankCredentialsAndTooShortOptionalBackupPasswords() {
        for (field in listOf("user", "secret")) for (value in listOf("", "   ")) {
            rejected { WebDavQrCodec.decode(payload(json().put(field, value))) }
        }
        for (password in listOf("", "1234567")) {
            rejected { WebDavQrCodec.encode(connection(password)) }
            rejected { WebDavQrCodec.decode(payload(json().put("backupPassword", password))) }
        }
        assertEquals("12345678", WebDavQrCodec.decode(WebDavQrCodec.encode(connection("12345678"))).backupPassword)
    }

    @Test fun enforcesUtf8ByteLimitAtTheExactBoundary() {
        val source = connection()
        val overhead = WebDavQrCodec.encode(WebDavQrConnection(source.endpoint, source.user, "s", source.prefix))
            .toByteArray(Charsets.UTF_8).size - 1
        val secret = "s".repeat(WebDavQrCodec.MAX_BYTES - overhead)
        val encoded = WebDavQrCodec.encode(WebDavQrConnection(source.endpoint, source.user, secret, source.prefix))
        assertEquals(WebDavQrCodec.MAX_BYTES, encoded.toByteArray(Charsets.UTF_8).size)
        assertEquals(secret, WebDavQrCodec.decode(encoded).secret)
        rejected { WebDavQrCodec.encode(WebDavQrConnection(source.endpoint, source.user, secret + "s", source.prefix)) }
        rejected { WebDavQrCodec.decode(encoded + " ") }
        rejected { WebDavQrCodec.encode(WebDavQrConnection(source.endpoint, source.user, "猫".repeat(700), source.prefix)) }
    }

    @Test fun connectionToStringDoesNotExposeEitherPassword() {
        val source = connection("example-backup-password")
        assertFalse(source.toString().contains(source.secret))
        assertFalse(source.toString().contains(requireNotNull(source.backupPassword)))
    }

    @Test fun unicodeConfigurationSurvivesActualQrEncodingAndDecoding() {
        val encoded = WebDavQrCodec.encode(connection("example-backup-password"))
        val matrix = QRCodeWriter().encode(encoded, BarcodeFormat.QR_CODE, 640, 640,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 4))
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels)))
        val decoded = WebDavQrCodec.decode(QRCodeReader().decode(bitmap).text)
        assertEquals(connection().prefix, decoded.prefix)
        assertEquals("example-backup-password", decoded.backupPassword)
    }
}
