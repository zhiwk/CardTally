package com.example.cardtally.cloud

import org.json.JSONObject
import org.json.JSONTokener

/** Not a data class: generated toString/copy output must not expose credentials. */
internal class WebDavQrConnection(
    val endpoint: String,
    val user: String,
    val secret: String,
    val prefix: String,
    val backupPassword: String? = null
)

/** A dedicated, versioned transfer format, deliberately separate from CloudConfig.json(). */
internal object WebDavQrCodec {
    const val HEADER = "cardtally-webdav:"
    const val MAX_BYTES = 2000
    private const val MAX_FIELD_LENGTH = 4096
    private val required = setOf("version", "endpoint", "user", "secret", "prefix")
    private val allowed = required + "backupPassword"

    fun encode(connection: WebDavQrConnection): String {
        validate(connection)
        val json = JSONObject().put("version", 1).put("endpoint", connection.endpoint)
            .put("user", connection.user).put("secret", connection.secret).put("prefix", connection.prefix)
        connection.backupPassword?.let { json.put("backupPassword", it) }
        return (HEADER + json.toString()).also(::checkSize)
    }

    fun decode(payload: String): WebDavQrConnection {
        checkSize(payload)
        require(payload.startsWith(HEADER)) { "Unsupported QR format" }
        // Read flat fields explicitly, never recursively parse arbitrary scanner input.
        // This also rejects duplicate keys consistently on Android and the JVM.
        val parser = JSONTokener(payload.substring(HEADER.length))
        require(parser.nextClean() == '{') { "Invalid QR structure" }
        val keys = mutableSetOf<String>()
        val values = mutableMapOf<String, String>()
        while (true) {
            require(parser.nextClean() == '"') { "Invalid QR field" }
            val key = parser.nextString('"')
            require(key in allowed && keys.add(key)) { "Invalid QR fields" }
            require(parser.nextClean() == ':') { "Invalid QR structure" }
            if (key == "version") {
                require(parser.nextClean() == '1') { "Unsupported QR version" }
            } else {
                require(parser.nextClean() == '"') { "Invalid QR field type" }
                values[key] = parser.nextString('"')
            }
            when (parser.nextClean()) {
                '}' -> break
                ',' -> Unit
                else -> throw IllegalArgumentException("Invalid QR structure")
            }
        }
        require(parser.nextClean() == '\u0000') { "Invalid QR structure" }
        require(keys.containsAll(required)) { "Missing QR fields" }
        fun text(key: String): String = values.getValue(key)
        return WebDavQrConnection(text("endpoint").trim(), text("user").trim(), text("secret"),
            text("prefix").trim(), values["backupPassword"])
            .also(::validate)
    }

    private fun checkSize(payload: String) {
        require(payload.length <= MAX_BYTES && payload.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) {
            "QR content too long"
        }
    }

    private fun validate(connection: WebDavQrConnection) {
        val values = listOf(connection.endpoint, connection.user, connection.secret, connection.prefix)
        require(values.all { it.length <= MAX_FIELD_LENGTH }) { "QR field too long" }
        require(CloudConnectionRules.validEndpoint(connection.endpoint)) { "Invalid HTTPS endpoint" }
        require(connection.user.isNotBlank() && connection.secret.isNotBlank()) { "Missing QR credentials" }
        require(CloudConnectionRules.validPrefix(connection.prefix)) { "Invalid backup directory" }
        connection.backupPassword?.let {
            require(it.length in 8..MAX_FIELD_LENGTH) { "Invalid backup password" }
        }
    }
}
