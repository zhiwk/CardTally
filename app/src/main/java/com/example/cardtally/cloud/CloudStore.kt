package com.example.cardtally.cloud

import android.util.Xml
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal data class RemoteFile(val name: String, val size: Long)
internal class CloudHttpException(val status: Int, val method: String = "") : IOException("Cloud request failed ($status)")
internal class CloudStore(private val config: CloudConfig, private val gate: () -> Unit = {}) {
    private val base = config.endpoint.trim().toHttpUrl()
    private val prefix = config.prefix.trim('/').split('/').filter(String::isNotEmpty)
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain -> gate(); chain.proceed(chain.request()) }.build()
    init {
        require(base.scheme == "https" && base.username.isEmpty() && base.password.isEmpty() && base.query == null && base.fragment == null)
        require(config.type in setOf("webdav", "s3") && prefix.none { it == "." || it == ".." })
        if (config.type == "s3") require(Regex("[a-zA-Z0-9.-]{3,63}").matches(config.bucket) && Regex("[a-z0-9-]+").matches(config.region))
    }
    fun cancel() = client.dispatcher.cancelAll()
    private fun url(name: String? = null, query: Map<String, String> = emptyMap()): HttpUrl {
        name?.let { require(Regex("[a-zA-Z0-9._-]+").matches(it)) }
        val builder = base.newBuilder()
        if (config.type == "s3") {
            if (config.pathStyle) builder.addPathSegment(config.bucket)
            else builder.host("${config.bucket}.${base.host}")
        }
        prefix.forEach { builder.addPathSegment(it) }
        if (name != null) builder.addPathSegment(name)
        else if (config.type == "webdav") builder.addPathSegment("")
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build()
    }
    private fun request(method: String, url: HttpUrl, body: RequestBody? = null, payloadHash: String = EMPTY_HASH,
        headers: Map<String, String> = emptyMap()): Request {
        val builder = Request.Builder().url(url).method(method, body)
        headers.forEach { (name, value) -> builder.header(name, value) }
        if (config.type == "webdav") builder.header("Authorization", Credentials.basic(config.user, config.secret))
        else {
            val stamp = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
            S3Signer.headers(method, url, payloadHash, config.user, config.secret, config.region, stamp,
                config.sessionToken, headers).forEach { (name, value) -> builder.header(name, value) }
        }
        return builder.build()
    }
    private fun call(request: Request): Response { gate(); return client.newCall(request).execute() }
    private fun requireSuccess(response: Response) { if (!response.isSuccessful) throw CloudHttpException(response.code, response.request.method) }
    fun prepare() {
        if (config.type != "webdav") return
        val builder = base.newBuilder()
        for (part in prefix) {
            builder.addPathSegment(part)
            call(request("MKCOL", builder.build().newBuilder().addPathSegment("").build(), ByteArray(0).toRequestBody())).use {
                if (!it.isSuccessful && it.code != 405) throw CloudHttpException(it.code, it.request.method)
            }
        }
    }
    fun get(name: String, target: File, limit: Long, progress: (Long) -> Unit = {}) {
        call(request("GET", url(name))).use { response ->
            requireSuccess(response); val body = requireNotNull(response.body)
            require(body.contentLength() <= limit)
            body.byteStream().use { input -> target.outputStream().use { output ->
                var total = 0L; val buffer = ByteArray(32768)
                while (true) { gate(); val count = input.read(buffer); if (count < 0) break
                    total += count; require(total <= limit); output.write(buffer, 0, count); progress(total) }
            } }
        }
    }
    fun put(name: String, file: File, createOnly: Boolean = false, progress: (Long) -> Unit = {}): Boolean {
        if (createOnly && config.type == "webdav") {
            // DAV's Overwrite header applies to the destination, unlike a PUT precondition.
            val staged = "upload-${UUID.randomUUID()}.tmp"
            var moved = false
            try {
                uploadFile(staged, file, false, progress)
                return call(request("MOVE", url(staged), headers = mapOf(
                    "Destination" to url(name).toString(), "Overwrite" to "F"
                ))).use { response ->
                    if (response.code == 412 || (response.code == 409 &&
                        base.host == "dav.jianguoyun.com" && destinationExists(name))) false
                    else {
                        requireSuccess(response)
                        moved = true
                        true
                    }
                }
            } finally {
                // Only our random staging object is eligible for cleanup. Preserve the
                // original failure if interrupted/network cleanup also fails.
                if (!moved) runCatching { delete(staged) }
            }
        }
        return uploadFile(name, file, createOnly, progress)
    }
    private fun destinationExists(name: String): Boolean =
        call(request("HEAD", url(name))).use { response ->
            when {
                response.isSuccessful -> true
                response.code == 404 -> false
                else -> { requireSuccess(response); false }
            }
        }

    private fun uploadFile(name: String, file: File, createOnly: Boolean, progress: (Long) -> Unit): Boolean {
        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = file.length()
            override fun writeTo(sink: BufferedSink) { file.inputStream().use { input ->
                var total = 0L; val buffer = ByteArray(32768)
                while (true) { gate(); val count = input.read(buffer); if (count < 0) break
                    sink.write(buffer, 0, count); total += count; progress(total) }
            } }
        }
        call(request("PUT", url(name), body, CloudCrypto.hash(file), if (createOnly) mapOf("If-None-Match" to "*") else emptyMap())).use {
            if (createOnly && it.code == 412) return false
            requireSuccess(it); return true
        }
    }
    fun delete(name: String) { call(request("DELETE", url(name))).use { if (it.code != 404) requireSuccess(it) } }
    fun list(): List<RemoteFile> {
        if (config.type == "webdav") {
            val body = "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:getcontentlength/><d:resourcetype/></d:prop></d:propfind>".toRequestBody("application/xml".toMediaType())
            call(request("PROPFIND", url(), body, headers = mapOf("Depth" to "1"))).use {
                requireSuccess(it); return parseList(readXml(it), false).first
            }
        }
        val result = mutableListOf<RemoteFile>(); var token: String? = null
        val seen = mutableSetOf<String>()
        do {
            val query = linkedMapOf("list-type" to "2", "prefix" to (prefix.joinToString("/") + if (prefix.isEmpty()) "" else "/"), "max-keys" to "1000")
            token?.let { query["continuation-token"] = it }
            val bucketUrl = base.newBuilder().apply {
                if (config.pathStyle) addPathSegment(config.bucket) else host("${config.bucket}.${base.host}")
                query.forEach { (key, value) -> addQueryParameter(key, value) }
            }.build()
            call(request("GET", bucketUrl)).use { response -> requireSuccess(response)
                val parsed = parseList(readXml(response), true); result += parsed.first; token = parsed.second }
            require(result.size <= 100000)
            token?.let { require(seen.add(it)) }
        } while (token != null)
        return result
    }
    private fun readXml(response: Response): String {
        val body = requireNotNull(response.body); require(body.contentLength() <= 8 * 1024 * 1024)
        val out = java.io.ByteArrayOutputStream()
        body.byteStream().use { input -> val buffer = ByteArray(8192); while (true) {
            gate(); val count = input.read(buffer); if (count < 0) break; require(out.size() + count <= 8 * 1024 * 1024); out.write(buffer, 0, count)
        } }
        return out.toString("UTF-8")
    }
    private fun parseList(xml: String, s3: Boolean): Pair<List<RemoteFile>, String?> {
        require(!xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true))
        val parser = Xml.newPullParser(); parser.setInput(xml.reader())
        val files = mutableListOf<RemoteFile>(); var name = ""; var size = 0L; var collection = false; var token: String? = null
        val rootPath = url().encodedPath
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) when (parser.name) {
                "Contents", "response" -> { name = ""; size = 0; collection = false }
                "Key" -> { val key = parser.nextText(); val start = prefix.joinToString("/") + if (prefix.isEmpty()) "" else "/"
                    if (key.startsWith(start)) name = key.removePrefix(start) }
                "href" -> { val path = URI(parser.nextText()).rawPath.orEmpty()
                    if (path.startsWith(rootPath)) name = path.removePrefix(rootPath) }
                "Size", "getcontentlength" -> size = parser.nextText().toLongOrNull() ?: 0
                "collection" -> collection = true
                "NextContinuationToken" -> token = parser.nextText().takeIf(String::isNotEmpty)
            }
            if (parser.eventType == XmlPullParser.END_TAG && parser.name == (if (s3) "Contents" else "response")) {
                if (!collection && Regex("[a-zA-Z0-9._-]+").matches(name)) files += RemoteFile(name, size)
            }
            parser.next()
        }
        return files to token
    }
    companion object { private const val EMPTY_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" }
}
