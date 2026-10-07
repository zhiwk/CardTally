package com.example.cardtally.cloud

import okhttp3.HttpUrl
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal object S3Signer {
    fun encode(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val n = byte.toInt() and 255
            if (n in 65..90 || n in 97..122 || n in 48..57 || n in listOf(45, 46, 95, 126)) append(n.toChar())
            else append("%%%02X".format(Locale.US, n))
        }
    }
    fun headers(method: String, url: HttpUrl, payloadHash: String, access: String, secret: String,
        region: String, stamp: String, token: String = "", extra: Map<String, String> = emptyMap()): Map<String, String> {
        val day = stamp.take(8)
        val signed = sortedMapOf("host" to (url.host + if (url.port != 443) ":${url.port}" else ""),
            "x-amz-content-sha256" to payloadHash, "x-amz-date" to stamp)
        if (token.isNotBlank()) signed["x-amz-security-token"] = token
        extra.forEach { (key, value) -> signed[key.lowercase(Locale.US)] = value.trim() }
        val names = signed.keys.joinToString(";")
        val query = (0 until url.querySize).map { encode(url.queryParameterName(it)) + "=" + encode(url.queryParameterValue(it).orEmpty()) }.sorted().joinToString("&")
        val path = "/" + url.pathSegments.joinToString("/", transform = ::encode)
        val canonical = "$method\n$path\n$query\n" + signed.entries.joinToString("") { "${it.key}:${it.value}\n" } + "\n$names\n$payloadHash"
        val scope = "$day/$region/s3/aws4_request"
        val signing = hmac(hmac(hmac(hmac(("AWS4$secret").toByteArray(), day), region), "s3"), "aws4_request")
        val signature = try { CloudCrypto.hex(hmac(signing, "AWS4-HMAC-SHA256\n$stamp\n$scope\n${CloudCrypto.hex(MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()))}")) }
        finally { signing.fill(0) }
        return signed.filterKeys { it != "host" } + ("Authorization" to "AWS4-HMAC-SHA256 Credential=$access/$scope, SignedHeaders=$names, Signature=$signature")
    }
    private fun hmac(key: ByteArray, text: String) = Mac.getInstance("HmacSHA256").apply {
        init(SecretKeySpec(key, "HmacSHA256"))
    }.doFinal(text.toByteArray(Charsets.UTF_8))
}
