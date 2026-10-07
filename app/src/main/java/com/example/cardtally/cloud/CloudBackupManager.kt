package com.example.cardtally.cloud

import android.content.Context
import com.example.cardtally.util.BackupArchiveManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class CloudVaultPasswordException : IllegalArgumentException()
internal class CloudCompatibilityException : IllegalArgumentException()

internal data class CloudSnapshot(val name: String, val date: Long, val device: String, val automatic: Boolean, val manifest: JSONObject)
internal data class CloudListing(val snapshots: List<CloudSnapshot>, val bytes: Long)
internal class CloudBackupManager(private val context: Context, config: CloudConfig,
    private val gate: () -> Unit = {}, private val progress: (String, Long, Long) -> Unit = { _, _, _ -> }) : AutoCloseable {
    private val config = config
    private val channel = if (context.packageName.endsWith(".release")) "release" else "dev"
    private val store = CloudStore(config.copy(prefix = config.prefix.trim('/') + "/$channel"), ::checkActive)
    private val directory = File.createTempFile("cloud-", ".work", context.cacheDir).also { check(it.delete() && it.mkdirs()) }
    init {
        context.cacheDir.listFiles()?.filter {
            ((it.name.startsWith("cloud-") && it.name.endsWith(".work")) || it.name.startsWith("cloud-prepared-")) &&
                System.currentTimeMillis() - it.lastModified() > 24L * 60 * 60 * 1000
        }?.forEach { it.deleteRecursively() }
    }
    private var key: ByteArray? = null
    private var sequence = 0
    private var transferred = 0L
    private var expectedUpload = 0L
    var cleanupWarning: Boolean = false
        private set
    fun cancel() = store.cancel()
    private fun checkActive() { if (Thread.currentThread().isInterrupted) throw InterruptedException(); gate() }
    private fun temp() = File(directory, "stage-${sequence++}")
    private fun locked(block: () -> Unit) { lock.lockInterruptibly(); try { block() } finally { lock.unlock() } }
    private fun download(name: String, limit: Long): File = temp().also { target ->
        var previous = 0L
        store.get(name, target, limit) { bytes -> transferred += bytes - previous; previous = bytes; progress("transfer", transferred, expectedUpload) }
    }
    private fun upload(name: String, file: File, create: Boolean = false): Boolean {
        var previous = 0L
        return store.put(name, file, create) { bytes -> transferred += bytes - previous; previous = bytes; progress("transfer", transferred, expectedUpload) }
    }
    private fun vault(create: Boolean): ByteArray {
        key?.let { return it }
        require(config.password.length >= 8)
        store.prepare()
        var metadata: File
        try { metadata = download("vault.json", 4096) }
        catch (error: CloudHttpException) {
            if (error.status != 404 || !create) throw error
            test() // Require conditional-create support before creating a shared password vault.
            val salt = ByteArray(16).also(SecureRandom()::nextBytes)
            val password = config.password.toCharArray()
            val newKey = try { CloudCrypto.key(password, salt) } finally { password.fill('\u0000') }
            metadata = temp().apply { writeText(JSONObject().put("version", 1)
                .put("salt", android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
                .put("verify", CloudCrypto.hmac(newKey, "CardTally vault 1".toByteArray())).toString()) }
            newKey.fill(0)
            // A concurrent first connection must never replace another vault's password.
            if (!upload("vault.json", metadata, true)) metadata = download("vault.json", 4096)
        }
        val json = JSONObject(metadata.readText()); require(json.getInt("version") == 1)
        val salt = android.util.Base64.decode(json.getString("salt"), android.util.Base64.NO_WRAP)
        val password = config.password.toCharArray()
        val derived = try { CloudCrypto.key(password, salt) } finally { password.fill('\u0000') }
        if (CloudCrypto.hmac(derived, "CardTally vault 1".toByteArray()) != json.getString("verify")) {
            derived.fill(0); throw CloudVaultPasswordException()
        }
        key = derived; return derived
    }
    fun test() = locked {
        store.prepare()
        val name = "test-${UUID.randomUUID()}.bin"
        val source = temp().apply { writeBytes(ByteArray(64).also(SecureRandom()::nextBytes)) }
        try { check(upload(name, source, true)); require(store.list().any { it.name == name })
            val remote = download(name, 1024); require(CloudCrypto.hash(remote) == CloudCrypto.hash(source))
            val other = temp().apply { writeBytes(ByteArray(64).also(SecureRandom()::nextBytes)) }
            if (upload(name, other, true)) throw CloudCompatibilityException()
            require(CloudCrypto.hash(download(name, 1024)) == CloudCrypto.hash(source))
        } finally { store.delete(name) }
        // Existing vaults must be readable with this password; test does not create one.
        if (store.list().any { it.name == "vault.json" }) vault(false)
    }
    fun list(): CloudListing { var result: CloudListing? = null; locked { result = listing() }; return requireNotNull(result) }
    private fun listing(): CloudListing {
        val files = store.list()
        if (files.none { it.name == "vault.json" }) return CloudListing(emptyList(), files.sumOf { it.size })
        val key = vault(false)
        val snapshots = files.filter { snapshotPattern.matches(it.name) }.map { remote ->
            require(remote.size in 28..MAX_MANIFEST)
            val root = snapshotJson(key, remote.name)
            validate(root, remote.name)
            CloudSnapshot(remote.name, root.getLong("createdAt"), root.getString("device"), root.getBoolean("automatic"), root)
        }.sortedByDescending { it.date }
        return CloudListing(snapshots, files.sumOf { it.size })
    }
    fun backup(automatic: Boolean): Boolean { var uploaded = false; locked {
        val key = vault(true); val device = CloudSettings.device(context)
        progress("prepare", 0, 0)
        val archive = temp()
        archive.outputStream().use { BackupArchiveManager(context).exportZipTo(it) }
        val entries = JSONArray()
        ZipInputStream(archive.inputStream()).use { zip ->
            while (true) {
                checkActive(); val entry = zip.nextEntry ?: break
                require(entryPattern.matches(entry.name))
                val plain = temp(); var total = 0L
                plain.outputStream().use { output -> val buffer = ByteArray(32768); while (true) {
                    val count = zip.read(buffer); if (count < 0) break; total += count; require(total <= MAX_BLOB)
                    output.write(buffer, 0, count)
                } }
                if (entry.name == "manifest.json") {
                    val root = JSONObject(plain.readText()); root.remove("exportedAt"); plain.writeText(root.toString())
                }
                val compressed = temp(); GZIPOutputStream(compressed.outputStream()).use { out -> plain.inputStream().use { it.copyTo(out) } }
                val id = CloudCrypto.id(key, plain)
                entries.put(JSONObject().put("entry", entry.name).put("key", "blob-$device-$id.bin")
                    .put("id", id).put("plainSize", plain.length()).put("compressedSize", compressed.length()).put("stage", compressed.name))
                plain.delete(); zip.closeEntry()
            }
        }
        archive.delete()
        val fingerprint = CloudCrypto.hmac(key, (0 until entries.length()).joinToString("|") {
            entries.getJSONObject(it).getString("entry") + ":" + entries.getJSONObject(it).getString("id")
        }.toByteArray())
        val knownSnapshots = listing().snapshots
        val current = knownSnapshots.firstOrNull { it.device == device }
        val knownBlobs = mutableMapOf<String, JSONObject>()
        knownSnapshots.filter { it.device == device }.forEach { snapshot ->
            val knownEntries = snapshot.manifest.getJSONArray("entries")
            for (i in 0 until knownEntries.length()) knownBlobs[knownEntries.getJSONObject(i).getString("key")] = knownEntries.getJSONObject(i)
        }
        if (automatic && current?.manifest?.optString("fingerprint") == fingerprint) {
            cleanRetention(device, knownSnapshots)
            return@locked
        }
        val existing = store.list().associateBy { it.name }.toMutableMap()
        var expected = 0L
        val newKeys = mutableSetOf<String>()
        for (i in 0 until entries.length()) { val item = entries.getJSONObject(i)
            val name = item.getString("key")
            if (existing[name] == null && newKeys.add(name)) expected += item.getLong("compressedSize") + 28 }
        expectedUpload = expected
        progress("estimate", transferred, expected)
        for (i in 0 until entries.length()) {
            checkActive(); val item = entries.getJSONObject(i); val name = item.getString("key")
            val compressed = File(directory, item.getString("stage")); item.remove("stage")
            val prior = existing[name]; val known = knownBlobs[name]
            if (prior != null && known != null && prior.size == known.getLong("size")) {
                // A committed, authenticated manifest already verified this blob. No media download for routine backups.
                item.put("size", known.getLong("size")).put("sha256", known.getString("sha256"))
                    .put("compressedSize", known.getLong("compressedSize"))
            } else {
                val encrypted = temp(); CloudCrypto.encrypt(key, compressed, encrypted)
                if (prior == null) upload(name, encrypted, true)
                // New or interrupted objects are read back once before publishing a snapshot.
                var remote = download(name, MAX_BLOB + 28); val checked = temp()
                try {
                    CloudCrypto.decrypt(key, remote, checked, MAX_BLOB)
                    require(contentId(key, checked, item.getLong("plainSize")) == item.getString("id"))
                } catch (error: Exception) {
                    // An interrupted orphan has no committed references and can be repaired safely.
                    if (known != null || error is CloudHttpException || error is InterruptedException || error is WifiRequiredException) throw error
                    checkActive(); upload(name, encrypted); remote.delete()
                    remote = download(name, MAX_BLOB + 28)
                    CloudCrypto.decrypt(key, remote, checked, MAX_BLOB)
                    require(contentId(key, checked, item.getLong("plainSize")) == item.getString("id"))
                }
                item.put("size", remote.length()).put("sha256", CloudCrypto.hash(remote))
                encrypted.delete(); remote.delete(); checked.delete()
            }
            existing[name] = RemoteFile(name, item.getLong("size"))
            knownBlobs[name] = JSONObject(item.toString())
            compressed.delete()
        }
        val created = System.currentTimeMillis()
        val snapshot = JSONObject().put("format", "cardtally-cloud").put("version", 1).put("device", device)
            .put("createdAt", created).put("automatic", automatic).put("fingerprint", fingerprint).put("entries", entries)
        val name = "snapshot-$device-$created-${if (automatic) "auto" else "manual"}-${UUID.randomUUID()}.bin"
        val plain = temp().apply { writeText(snapshot.toString()) }; require(plain.length() <= MAX_MANIFEST - 28)
        val packed = temp(); GZIPOutputStream(packed.outputStream()).use { out -> plain.inputStream().use { it.copyTo(out) } }
        val encrypted = temp(); CloudCrypto.encrypt(key, packed, encrypted)
        check(upload(name, encrypted, true))
        val verify = download(name, MAX_MANIFEST); require(CloudCrypto.hash(verify) == CloudCrypto.hash(encrypted))
        uploaded = true
        // Retention never affects manual backups or another installation's objects.
        cleanRetention(device, knownSnapshots + CloudSnapshot(name, created, device, automatic, snapshot))
    }; return uploaded }
    fun prepareRestore(snapshot: CloudSnapshot): File {
        var result: File? = null
        locked {
            val key = vault(false)
            val root = snapshotJson(key, snapshot.name); validate(root, snapshot.name)
            val entries = root.getJSONArray("entries"); val archive = temp(); var total = 0L
            ZipOutputStream(archive.outputStream()).use { zip ->
                for (index in 0 until entries.length()) {
                    checkActive(); val item = entries.getJSONObject(index)
                    val encrypted = download(item.getString("key"), MAX_BLOB + 28)
                    require(encrypted.length() == item.getLong("size") && CloudCrypto.hash(encrypted) == item.getString("sha256"))
                    val compressed = temp(); CloudCrypto.decrypt(key, encrypted, compressed, MAX_BLOB)
                    require(compressed.length() == item.getLong("compressedSize") && contentId(key, compressed, item.getLong("plainSize")) == item.getString("id"))
                    zip.putNextEntry(ZipEntry(item.getString("entry")))
                    var length = 0L
                    GZIPInputStream(compressed.inputStream()).use { input -> val buffer = ByteArray(32768); while (true) {
                        checkActive(); val count = input.read(buffer); if (count < 0) break
                        length += count; total += count; require(length <= item.getLong("plainSize") && total <= MAX_TOTAL)
                        zip.write(buffer, 0, count)
                    } }
                    require(length == item.getLong("plainSize")); zip.closeEntry(); encrypted.delete(); compressed.delete()
                }
            }
            val prepared = File.createTempFile("cloud-prepared-", ".zip", context.cacheDir)
            try { archive.inputStream().use { input -> prepared.outputStream().use { input.copyTo(it); it.fd.sync() } }
                result = prepared } catch (error: Exception) { prepared.delete(); throw error }
        }
        return requireNotNull(result)
    }
    fun delete(snapshot: CloudSnapshot) = locked {
        vault(false); require(snapshotPattern.matches(snapshot.name)); store.delete(snapshot.name)
        if (snapshot.device == CloudSettings.device(context)) collectOwnOrphans(snapshot.device)
    }
    private fun cleanRetention(device: String, snapshots: List<CloudSnapshot>) {
        try {
            val expired = CloudRetention.expired(snapshots, device, config.keep)
            expired.forEach { store.delete(it.name) }
            collectOwnOrphans(device, snapshots.filter { candidate -> expired.none { it.name == candidate.name } })
        } catch (error: Exception) {
            if (error is InterruptedException || error is WifiRequiredException) throw error
            cleanupWarning = true // The completed snapshot remains usable if retention cleanup fails.
        }
    }
    private fun snapshotJson(key: ByteArray, name: String): JSONObject {
        val encrypted = download(name, MAX_MANIFEST); val compressed = temp()
        try {
            CloudCrypto.decrypt(key, encrypted, compressed, MAX_MANIFEST)
            val output = java.io.ByteArrayOutputStream()
            GZIPInputStream(compressed.inputStream()).use { input ->
                val buffer = ByteArray(8192)
                while (true) { checkActive(); val count = input.read(buffer); if (count < 0) break
                    require(output.size() + count <= MAX_MANIFEST); output.write(buffer, 0, count) }
            }
            return JSONObject(output.toString("UTF-8"))
        } finally { encrypted.delete(); compressed.delete() }
    }
    private fun contentId(key: ByteArray, compressed: File, expected: Long): String {
        val plain = temp()
        try {
            GZIPInputStream(compressed.inputStream()).use { input -> plain.outputStream().use { output ->
                var total = 0L; val buffer = ByteArray(32768)
                while (true) { checkActive(); val count = input.read(buffer); if (count < 0) break
                    total += count; require(total <= expected && total <= MAX_BLOB); output.write(buffer, 0, count) }
                require(total == expected)
            } }
            return CloudCrypto.id(key, plain)
        } finally { plain.delete() }
    }
    private fun collectOwnOrphans(device: String, snapshots: List<CloudSnapshot> = listing().snapshots) {
        // Local operations are serialized; namespaces are unique and are never restored to another device.
        val retained = snapshots.filter { it.device == device }
        val referenced = mutableSetOf<String>()
        retained.forEach { snapshot -> val entries = snapshot.manifest.getJSONArray("entries")
            for (i in 0 until entries.length()) referenced += entries.getJSONObject(i).getString("key") }
        store.list().filter { it.name.startsWith("blob-$device-") && blobPattern.matches(it.name) && it.name !in referenced }
            .forEach { store.delete(it.name) }
    }
    private fun validate(root: JSONObject, name: String) {
        require(root.getString("format") == "cardtally-cloud" && root.getInt("version") == 1)
        val device = root.getString("device"); require(uuidPattern.matches(device))
        val date = root.getLong("createdAt"); require(date > 0)
        val kind = if (root.getBoolean("automatic")) "auto" else "manual"
        require(name.startsWith("snapshot-$device-$date-$kind-") && snapshotPattern.matches(name))
        val entries = root.getJSONArray("entries"); require(entries.length() in 1..50000)
        val seen = mutableSetOf<String>(); var total = 0L
        for (i in 0 until entries.length()) {
            val item = entries.getJSONObject(i); val entry = item.getString("entry")
            require(entryPattern.matches(entry) && seen.add(entry) && (i != 0 || entry == "manifest.json"))
            val id = item.getString("id"); require(Regex("[a-f0-9]{64}").matches(id))
            require(item.getString("key") == "blob-$device-$id.bin")
            require(Regex("[a-f0-9]{64}").matches(item.getString("sha256")))
            require(item.getLong("size") in 28..(MAX_BLOB + 28) && item.getLong("compressedSize") in 1..MAX_BLOB)
            val size = item.getLong("plainSize"); require(size in 0..MAX_BLOB); total += size; require(total <= MAX_TOTAL)
        }
    }
    override fun close() { store.cancel(); key?.fill(0); directory.deleteRecursively() }
    companion object {
        private val lock = ReentrantLock()
        private const val MAX_BLOB = 128L * 1024 * 1024
        private const val MAX_TOTAL = 1200L * 1024 * 1024
        private const val MAX_MANIFEST = 16L * 1024 * 1024
        private val uuidPattern = Regex("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")
        private val snapshotPattern = Regex("snapshot-[a-f0-9-]{36}-[0-9]+-(auto|manual)-[a-f0-9-]{36}\\.bin")
        private val blobPattern = Regex("blob-[a-f0-9-]{36}-[a-f0-9]{64}\\.bin")
        private val entryPattern = Regex("manifest\\.json|photos/[0-9]+\\.jpg|wallpapers/[0-9]+\\.webp")
    }
}
