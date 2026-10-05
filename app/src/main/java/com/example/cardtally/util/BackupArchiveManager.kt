package com.example.cardtally.util

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.cardtally.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable, additive backup. A source installation and its original row ID identify an imported
 * item; matching amounts, dates or names alone never make two rows the same item.
 */
class BackupArchiveManager(private val context: Context) {
    class AssetBalanceConflictException : IllegalStateException("Asset balance conflict")
    class BackupIntegrityException : IllegalArgumentException("Incorrect password or damaged backup")
    data class MergeResult(
        val ledgers: Int, val categories: Int, val assets: Int, val records: Int,
        val recurring: Int, val sessions: Int, val messages: Int,
        val legacy: Boolean = false, val skipped: Int = 0, val different: Int = 0,
        val balanceConflicts: Int = 0
    )

    fun exportTo(output: OutputStream, password: CharArray) {
        require(password.size >= 8) { "Backup password is too short" }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(16).also(SecureRandom()::nextBytes)
        val header = ENCRYPTED_MAGIC + salt + nonce
        output.write(header)
        val keys = backupKeys(password, salt)
        val cipher = backupCipher(Cipher.ENCRYPT_MODE, keys, nonce)
        val mac = backupMac(keys, header)
        keys.fill(0)
        exportZipTo(CipherOutputStream(MacOutputStream(output, mac), cipher))
    }

    private fun exportZipTo(output: OutputStream) {
        val tables = DataTransferManager(context).use { it.exportTables() }
        tables.remove("record_deletion_undo")
        val source = sourceId()
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("sourceId", source)
            .put("exportedAt", System.currentTimeMillis())
            .put("tables", tables)
            .put("aliases", exportAliases(tables))
            .put("preferences", typedPreferences())

        val photoUris = referencedPhotoUris(tables)
        val photoEntries = JSONArray()
        photoUris.forEachIndexed { index, uri ->
            photoEntries.put(JSONObject().put("uri", uri).put("entry", "photos/" + index + ".jpg"))
        }
        root.put("photos", photoEntries)

        val manifestBytes = root.toString().toByteArray(Charsets.UTF_8)
        require(manifestBytes.size <= MAX_MANIFEST_BYTES) { "Backup manifest is too large" }
        var totalPhotoBytes = 0L
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifestBytes)
            zip.closeEntry()
            for (index in 0 until photoEntries.length()) {
                val photo = photoEntries.getJSONObject(index)
                zip.putNextEntry(ZipEntry(photo.getString("entry")))
                context.contentResolver.openInputStream(Uri.parse(photo.getString("uri")))
                    ?.use { photoInput ->
                        val buffer = ByteArray(8192)
                        var photoBytes = 0L
                        while (true) {
                            val bytes = photoInput.read(buffer)
                            if (bytes < 0) break
                            photoBytes += bytes
                            totalPhotoBytes += bytes
                            require(photoBytes <= MAX_PHOTO_BYTES &&
                                totalPhotoBytes <= MAX_TOTAL_PHOTO_BYTES) {
                                "Backup photos exceed limit"
                            }
                            zip.write(buffer, 0, bytes)
                        }
                    }
                    ?: throw IllegalStateException("A record photo cannot be read")
                zip.closeEntry()
            }
        }
        seedSelfMappings(source, tables)
    }

    private fun referencedPhotoUris(tables: JSONObject): Set<String> {
        val photoUris = linkedSetOf<String>()
        val records = tables.getJSONArray("records")
        for (index in 0 until records.length()) {
            val row = records.getJSONObject(index)
            if (!row.isNull("photo_uri")) {
                row.getString("photo_uri").takeIf(String::isNotBlank)?.let(photoUris::add)
            }
            if (!row.isNull("photo_uris")) {
                row.getString("photo_uris").split('|').filter(String::isNotBlank).forEach(photoUris::add)
            }
        }
        return photoUris
    }

    private fun seedSelfMappings(source: String, tables: JSONObject) {
        DatabaseHelper(context).use { helper ->
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                ID_TABLES.forEach { table ->
                    rows(tables, table).forEach { row ->
                        val id = row.getLong("id")
                        db.execSQL(
                            "INSERT OR IGNORE INTO backup_import_map(source_id, table_name, source_row_id, local_row_id) VALUES (?, ?, ?, ?)",
                            arrayOf(source, table, id.toString(), id)
                        )
                    }
                }
                rows(tables, "assets").forEach { row ->
                    db.execSQL(
                        "INSERT OR REPLACE INTO backup_asset_snapshot(source_id, source_row_id, local_row_id, source_amount) VALUES (?, ?, ?, ?)",
                        arrayOf(source, row.getLong("id").toString(), row.getLong("id"), row.getLong("amount"))
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    private data class Identity(val source: String, val table: String, val rowId: Long)

    private fun exportAliases(tables: JSONObject): JSONArray {
        val included = ID_TABLES.associateWith { table ->
            rows(tables, table).mapTo(mutableSetOf()) { it.getLong("id") }
        }
        val aliases = JSONArray()
        DatabaseHelper(context).use { helper ->
            helper.readableDatabase.rawQuery(
                "SELECT source_id, table_name, source_row_id, local_row_id FROM backup_import_map",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val table = cursor.getString(1)
                    val localId = cursor.getLong(3)
                    if (localId !in included[table].orEmpty()) continue
                    aliases.put(
                        JSONObject()
                            .put("sourceId", cursor.getString(0))
                            .put("table", table)
                            .put("sourceRowId", cursor.getString(2))
                            .put("localRowId", localId)
                    )
                }
            }
        }
        return aliases
    }

    private fun readAliases(root: JSONObject): Map<Pair<String, Long>, List<Identity>> {
        val byRow = mutableMapOf<Pair<String, Long>, MutableList<Identity>>()
        val tables = root.getJSONObject("tables")
        val rowIds = ID_TABLES.associateWith { table ->
            val sourceRows = rows(tables, table)
            val ids = sourceRows.mapTo(mutableSetOf()) { it.getLong("id") }
            require(ids.size == sourceRows.size && ids.all { it > 0L }) {
                "Duplicate or invalid backup row ID"
            }
            ids
        }
        val owners = mutableMapOf<Identity, Pair<String, Long>>()
        ID_TABLES.forEach { table ->
            rowIds.getValue(table).forEach { id ->
                owners[Identity(root.getString("sourceId"), table, id)] = table to id
            }
        }
        if (!root.has("aliases")) return emptyMap()
        val aliases = root.getJSONArray("aliases")
        for (index in 0 until aliases.length()) {
            val row = aliases.getJSONObject(index)
            val table = row.getString("table")
            val localId = row.getLong("localRowId")
            val source = row.getString("sourceId")
            val sourceRowId = row.getString("sourceRowId").toLong()
            require(table in ID_TABLES && localId in rowIds[table].orEmpty() &&
                source.isNotBlank() && sourceRowId > 0) {
                "Invalid backup identity"
            }
            val identity = Identity(source, table, sourceRowId)
            val owner = table to localId
            require(owners[identity] == null || owners[identity] == owner) {
                "Conflicting backup identity"
            }
            owners[identity] = owner
            byRow.getOrPut(table to localId) { mutableListOf() }
                .add(identity)
        }
        return byRow
    }

    fun importFrom(
        input: InputStream, password: CharArray? = null, dryRun: Boolean = false,
        useBackupBalances: Boolean = false
    ): MergeResult {
        val stream = PushbackInputStream(input, 4)
        val signature = ByteArray(4)
        var count = 0
        while (count < signature.size) {
            val next = stream.read(signature, count, signature.size - count)
            if (next < 0) break
            count += next
        }
        if (count > 0) stream.unread(signature, 0, count)
        if (count == 4 && signature.contentEquals(ENCRYPTED_MAGIC)) {
            require(password != null) { "Backup password required" }
            val header = ByteArray(ENCRYPTED_MAGIC.size + 16 + 16)
            var headerCount = 0
            while (headerCount < header.size) {
                val next = stream.read(header, headerCount, header.size - headerCount)
                require(next > 0) { "Invalid encrypted backup" }
                headerCount += next
            }
            require(header.copyOfRange(0, 4).contentEquals(ENCRYPTED_MAGIC)) { "Invalid encrypted backup" }
            val salt = header.copyOfRange(4, 20)
            val nonce = header.copyOfRange(20, 36)
            val staged = File.createTempFile("cardtally_import_", ".ctb", context.cacheDir)
            try {
                staged.outputStream().use { output ->
                    output.write(header)
                    val buffer = ByteArray(8192)
                    var total = header.size.toLong()
                    while (true) {
                        val next = stream.read(buffer)
                        if (next < 0) break
                        total += next
                        require(total <= MAX_ENCRYPTED_BYTES) { "Backup is too large" }
                        output.write(buffer, 0, next)
                    }
                }
                val keys = backupKeys(password, salt)
                try {
                    verifyEncryptedFile(staged, keys, header)
                    val cipher = backupCipher(Cipher.DECRYPT_MODE, keys, nonce)
                    val ciphertextLength = staged.length() - header.size - 32
                    val encrypted = FileInputStream(staged)
                    encrypted.skip(header.size.toLong())
                    return importZip(CipherInputStream(LimitedInputStream(encrypted, ciphertextLength), cipher),
                        dryRun, useBackupBalances)
                } finally {
                    keys.fill(0)
                }
            } finally {
                staged.delete()
            }
        }
        if (count < 4 || signature[0] != 'P'.code.toByte() || signature[1] != 'K'.code.toByte()) {
            require(!dryRun) { "Legacy JSON preview is unavailable" }
            val legacy = DataTransferManager(context).use {
                it.importJson(stream.reader(Charsets.UTF_8).readText())
            }
            return MergeResult(
                legacy.ledgers, legacy.categories, legacy.assets, legacy.records,
                legacy.recurring, legacy.sessions, 0, true
            )
        }
        return importZip(stream, dryRun, useBackupBalances)
    }

    private fun importZip(input: InputStream, dryRun: Boolean, useBackupBalances: Boolean): MergeResult {
        val newFiles = mutableListOf<File>()
        try {
            ZipInputStream(input).use { zip ->
                require(zip.nextEntry?.name == "manifest.json") { "Missing backup manifest" }
                val manifest = readLimited(zip, MAX_MANIFEST_BYTES).toString(Charsets.UTF_8)
                zip.closeEntry()
                val root = JSONObject(manifest)
                require(root.optString("format") == FORMAT && root.optInt("version") == VERSION) {
                    "Unsupported backup version"
                }
                require(root.optString("sourceId").isNotBlank()) { "Missing backup source" }
                val tables = root.getJSONObject("tables")
                ROW_TABLES.forEach { require(tables.optJSONArray(it) != null) { "Missing table: " + it } }
                require(tables.getJSONArray("ledgers").length() > 0) { "Backup has no ledger" }
                val photos = root.getJSONArray("photos")
                val photoUris = mutableMapOf<String, String>()
                val photoDirectory = File(context.filesDir, "record_photos").apply { mkdirs() }
                var totalPhotoBytes = 0L
                for (index in 0 until photos.length()) {
                    val photo = photos.getJSONObject(index)
                    require(photo.getString("entry") == "photos/" + index + ".jpg") { "Invalid photo entry" }
                    require(zip.nextEntry?.name == photo.getString("entry")) { "Missing record photo" }
                    val file = File(photoDirectory, "backup_" + UUID.randomUUID() + ".jpg")
                    newFiles += file
                    file.outputStream().use { destination ->
                        val buffer = ByteArray(8192)
                        var photoBytes = 0L
                        while (true) {
                            val bytes = zip.read(buffer)
                            if (bytes < 0) break
                            photoBytes += bytes
                            totalPhotoBytes += bytes
                            require(photoBytes <= MAX_PHOTO_BYTES && totalPhotoBytes <= MAX_TOTAL_PHOTO_BYTES) {
                                "Backup photos exceed limit"
                            }
                            destination.write(buffer, 0, bytes)
                        }
                    }
                    zip.closeEntry()
                    val oldUri = photo.getString("uri")
                    require(oldUri.isNotBlank() && oldUri !in photoUris) { "Duplicate record photo" }
                    photoUris[oldUri] = FileProvider.getUriForFile(
                        context, context.packageName + ".fileprovider", file
                    ).toString()
                }
                require(zip.nextEntry == null) { "Unexpected backup entry" }
                require(photoUris.keys == referencedPhotoUris(tables)) { "Incomplete record photos" }
                val result = merge(root, photoUris, dryRun, useBackupBalances)
                cleanupUnreferenced(newFiles)
                return result
            }
        } catch (error: Exception) {
            // The database may already have committed even if a preference write failed.
            runCatching { cleanupUnreferenced(newFiles) }
            throw error
        }
    }

    private fun backupKeys(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, 210_000, 512)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun backupCipher(mode: Int, keys: ByteArray, nonce: ByteArray): Cipher =
        Cipher.getInstance("AES/CTR/NoPadding").apply {
            init(mode, SecretKeySpec(keys.copyOfRange(0, 32), "AES"), IvParameterSpec(nonce))
        }

    private fun backupMac(keys: ByteArray, header: ByteArray): Mac =
        Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(keys.copyOfRange(32, 64), "HmacSHA256"))
            update(header)
        }

    private fun verifyEncryptedFile(file: File, keys: ByteArray, header: ByteArray) {
        val ciphertextLength = file.length() - header.size - 32
        require(ciphertextLength >= 0) { "Incomplete encrypted backup" }
        val mac = backupMac(keys, header)
        FileInputStream(file).use { input ->
            require(input.skip(header.size.toLong()) == header.size.toLong())
            val buffer = ByteArray(8192)
            var remaining = ciphertextLength
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                require(count > 0) { "Incomplete encrypted backup" }
                mac.update(buffer, 0, count)
                remaining -= count
            }
            val expected = ByteArray(32)
            var offset = 0
            while (offset < expected.size) {
                val count = input.read(expected, offset, expected.size - offset)
                require(count > 0) { "Incomplete encrypted backup" }
                offset += count
            }
            if (!MessageDigest.isEqual(mac.doFinal(), expected)) throw BackupIntegrityException()
        }
    }

    private class MacOutputStream(private val output: OutputStream, private val mac: Mac) : OutputStream() {
        override fun write(value: Int) {
            output.write(value)
            mac.update(value.toByte())
        }
        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            output.write(bytes, offset, length)
            mac.update(bytes, offset, length)
        }
        override fun close() {
            output.write(mac.doFinal())
            output.close()
        }
    }

    private class LimitedInputStream(private val input: InputStream, private var remaining: Long) : InputStream() {
        override fun read(): Int {
            if (remaining <= 0) return -1
            val value = input.read()
            if (value >= 0) remaining--
            return value
        }
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0) return -1
            val count = input.read(bytes, offset, minOf(length.toLong(), remaining).toInt())
            if (count > 0) remaining -= count
            return count
        }
        override fun close() = input.close()
    }

    private fun cleanupUnreferenced(files: List<File>) {
        DatabaseHelper(context).use { helper ->
            val db = helper.readableDatabase
            files.forEach { file ->
                val uri = FileProvider.getUriForFile(
                    context, context.packageName + ".fileprovider", file
                ).toString()
                val referenced = db.rawQuery(
                    "SELECT 1 FROM records WHERE photo_uri = ? OR photo_uris LIKE ? LIMIT 1",
                    arrayOf(uri, "%" + uri + "%")
                ).use { it.moveToFirst() }
                if (!referenced) file.delete()
            }
        }
    }

    private fun merge(
        root: JSONObject, photos: Map<String, String>, dryRun: Boolean, useBackupBalances: Boolean
    ): MergeResult {
        if (!dryRun) replayPendingPreferences()
        val source = root.getString("sourceId")
        val tables = root.getJSONObject("tables")
        val aliases = readAliases(root)
        val preferences = root.getJSONObject("preferences")
        validatePreferences(preferences)
        val ids = mutableMapOf<String, MutableMap<Long, Long>>()
        var ledgers = 0
        var categories = 0
        var assets = 0
        var records = 0
        var recurring = 0
        var sessions = 0
        var messages = 0
        var skipped = 0
        var different = 0
        var balanceConflicts = 0
        var emptyTarget = false
        DatabaseHelper(context).use { helper ->
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                emptyTarget = removePristineSeed(db)
                fun mapped(table: String, oldId: Long): Long? = ids[table]?.get(oldId)
                fun insert(table: String, row: JSONObject, adjust: (ContentValues) -> Unit = {}): Boolean {
                    val oldId = row.getLong("id")
                    require(oldId > 0L) { "Invalid backup row ID" }
                    val map = ids.getOrPut(table) { mutableMapOf() }
                    val identities = (listOf(Identity(source, table, oldId)) +
                        aliases[table to oldId].orEmpty()).distinct()
                    val existing = identities.mapNotNull { identity ->
                        db.rawQuery(
                            "SELECT local_row_id FROM backup_import_map WHERE source_id = ? AND table_name = ? AND source_row_id = ?",
                            arrayOf(identity.source, identity.table, identity.rowId.toString())
                        ).use { if (it.moveToFirst()) it.getLong(0) else null }
                    }.filter { candidate ->
                        val exists = db.rawQuery(
                            "SELECT 1 FROM " + table + " WHERE id = ? LIMIT 1",
                            arrayOf(candidate.toString())
                        ).use { it.moveToFirst() }
                        exists
                    }.distinct()
                    require(existing.size <= 1) { "Conflicting backup identities" }
                    val localId = if (existing.isNotEmpty()) existing.single() else {
                        val content = rowValues(row)
                        content.remove("id")
                        adjust(content)
                        db.insertOrThrow(table, null, content)
                    }
                    if (existing.isNotEmpty()) {
                        skipped++
                        val expected = rowValues(row).apply { remove("id"); adjust(this) }
                        val ignored = when (table) {
                            "assets" -> setOf("amount")
                            "records" -> setOf("photo_uri", "photo_uris")
                            "categories" -> setOf("parent_id")
                            else -> emptySet()
                        }
                        if (rowDiffers(db, table, localId, expected, ignored)) different++
                    }
                    require(localId !in map.values) { "Distinct backup rows resolve to one local row" }
                    identities.forEach { identity ->
                        db.execSQL(
                            "INSERT OR REPLACE INTO backup_import_map(source_id, table_name, source_row_id, local_row_id) VALUES (?, ?, ?, ?)",
                            arrayOf(identity.source, table, identity.rowId.toString(), localId)
                        )
                    }
                    map[oldId] = localId
                    return existing.isEmpty()
                }
                rows(tables, "ledgers").forEach { row ->
                    if (insert("ledgers", row) { it.put("shared_asset_ids", "") }) ledgers++
                }
                val addedCategories = mutableSetOf<Long>()
                rows(tables, "categories").forEach { row ->
                    if (insert("categories", row) {
                            it.putNull("parent_id")
                            it.put("ledger_id", requiredId(mapped("ledgers", row.getLong("ledger_id"))))
                        }) {
                        categories++
                        addedCategories += row.getLong("id")
                    }
                }
                rows(tables, "categories").forEach { row ->
                    if (row.getLong("id") !in addedCategories) return@forEach
                    val parent = row.optLong("parent_id").takeIf { it > 0 }
                        ?.let { requiredId(mapped("categories", it)) }
                    db.execSQL(
                        "UPDATE categories SET parent_id = ? WHERE id = ?",
                        arrayOf(parent, mapped("categories", row.getLong("id")))
                    )
                }
                rows(tables, "assets").forEach { row ->
                    val added = insert("assets", row) {
                            it.put("ledger_id", requiredId(mapped("ledgers", row.getLong("ledger_id"))))
                        }
                    val localId = requiredId(mapped("assets", row.getLong("id")))
                    if (reconcileAssetAmount(db, source, row,
                            aliases["assets" to row.getLong("id")].orEmpty(), localId, added,
                            useBackupBalances)) balanceConflicts++
                    if (added) assets++
                }
                rows(tables, "records").forEach { row ->
                    val added = insert("records", row) { content ->
                        content.put("ledger_id", requiredId(mapped("ledgers", row.getLong("ledger_id"))))
                        remapOptional(content, "category_id", row, mapped("categories", row.optLong("category_id")))
                        remapOptional(content, "asset_id", row, mapped("assets", row.optLong("asset_id")))
                        remapOptional(content, "destination_asset_id", row, mapped("assets", row.optLong("destination_asset_id")))
                        if (!row.isNull("photo_uri")) {
                            row.getString("photo_uri").takeIf(String::isNotBlank)?.let {
                                content.put("photo_uri", photos[it] ?: error("Missing record photo"))
                            }
                        }
                        if (!row.isNull("photo_uris")) {
                            row.getString("photo_uris").takeIf(String::isNotBlank)?.let { originals ->
                                content.put(
                                    "photo_uris",
                                    originals.split('|').filter(String::isNotBlank).joinToString("|") {
                                        photos[it] ?: error("Missing record photo")
                                    }
                                )
                            }
                        }
                    }
                    if (added) records++
                    else repairMissingRecordPhotos(
                        db, requiredId(mapped("records", row.getLong("id"))), row, photos
                    )
                }
                rows(tables, "recurring_records").forEach { row ->
                    if (insert("recurring_records", row) { content ->
                            content.put("ledger_id", requiredId(mapped("ledgers", row.getLong("ledger_id"))))
                            remapOptional(content, "category_id", row, mapped("categories", row.optLong("category_id")))
                            remapOptional(content, "asset_id", row, mapped("assets", row.optLong("asset_id")))
                            remapOptional(content, "destination_asset_id", row, mapped("assets", row.optLong("destination_asset_id")))
                        }) recurring++
                }
                rows(tables, "ai_chat_sessions").forEach { row ->
                    if (insert("ai_chat_sessions", row)) sessions++
                }
                rows(tables, "ai_chat_messages").forEach { row ->
                    if (insert("ai_chat_messages", row) {
                            it.put("session_id", requiredId(mapped("ai_chat_sessions", row.getLong("session_id"))))
                        }) messages++
                }
                rows(tables, "ledger_shared_ledgers").forEach { row ->
                    val ledger = requiredId(mapped("ledgers", row.getLong("shared_ledger_id")))
                    val sourceLedger = requiredId(mapped("ledgers", row.getLong("shared_source_ledger_id")))
                    db.execSQL(
                        "INSERT OR IGNORE INTO ledger_shared_ledgers(shared_ledger_id, shared_source_ledger_id) VALUES (?, ?)",
                        arrayOf(ledger, sourceLedger)
                    )
                }
                rows(tables, "ledger_shared_assets").forEach { row ->
                    val ledger = requiredId(mapped("ledgers", row.getLong("shared_ledger_id")))
                    val asset = requiredId(mapped("assets", row.getLong("shared_asset_id")))
                    db.execSQL(
                        "INSERT OR IGNORE INTO ledger_shared_assets(shared_ledger_id, shared_asset_id) VALUES (?, ?)",
                        arrayOf(ledger, asset)
                    )
                }
                if (!dryRun) {
                    db.execSQL(
                        "INSERT OR REPLACE INTO backup_pending_settings(id, payload) VALUES (1, ?)",
                        arrayOf(encodePendingPreferences(preferences, ids, overwriteExisting = emptyTarget).toString())
                    )
                }
                if (!dryRun) db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        if (!dryRun) replayPendingPreferences()
        return MergeResult(ledgers, categories, assets, records, recurring, sessions, messages,
            skipped = skipped, different = different, balanceConflicts = balanceConflicts)
    }

    private fun rowDiffers(
        db: SQLiteDatabase, table: String, id: Long, expected: ContentValues, ignored: Set<String>
    ): Boolean = db.rawQuery("SELECT * FROM $table WHERE id = ?", arrayOf(id.toString())).use { cursor ->
        if (!cursor.moveToFirst()) return@use true
        expected.valueSet().any { item ->
            if (item.key in ignored) return@any false
            val index = cursor.getColumnIndex(item.key)
            if (index < 0) return@any true
            val current = if (cursor.isNull(index)) null else cursor.getString(index)
            val value = item.value?.toString()
            current != value
        }
    }

    private fun remapOptional(
        content: ContentValues, column: String, row: JSONObject, target: Long?
    ) {
        if (row.isNull(column) || row.optLong(column) <= 0L) content.putNull(column)
        else content.put(column, requiredId(target))
    }

    private fun reconcileAssetAmount(
        db: SQLiteDatabase, source: String, row: JSONObject, aliases: List<Identity>,
        localId: Long, added: Boolean, useBackupBalance: Boolean
    ): Boolean {
        val sourceAmount = row.getLong("amount")
        val identities = (listOf(Identity(source, "assets", row.getLong("id"))) + aliases).distinct()
        val previous = identities.firstNotNullOfOrNull { identity ->
            db.rawQuery(
                "SELECT source_amount FROM backup_asset_snapshot WHERE source_id = ? AND source_row_id = ? AND local_row_id = ?",
                arrayOf(identity.source, identity.rowId.toString(), localId.toString())
            ).use { if (it.moveToFirst()) it.getLong(0) else null }
        }
        val localAmount = db.rawQuery(
            "SELECT amount FROM assets WHERE id = ?", arrayOf(localId.toString())
        ).use { check(it.moveToFirst()); it.getLong(0) }
        val conflict = !added && localAmount != sourceAmount && previous != sourceAmount &&
            (previous == null || localAmount != previous)
        if (conflict && !useBackupBalance) throw AssetBalanceConflictException()
        if (!added && localAmount != sourceAmount && previous != sourceAmount) {
            db.execSQL("UPDATE assets SET amount = ? WHERE id = ?", arrayOf(sourceAmount, localId))
        }
        identities.forEach { identity ->
            db.execSQL(
                "INSERT OR REPLACE INTO backup_asset_snapshot(source_id, source_row_id, local_row_id, source_amount) VALUES (?, ?, ?, ?)",
                arrayOf(identity.source, identity.rowId.toString(), localId, sourceAmount)
            )
        }
        return conflict
    }

    private fun removePristineSeed(db: SQLiteDatabase): Boolean {
        val seedLedger = db.rawQuery(
            "SELECT id, name, subtitle, shared_asset_ids, sort_order, icon_name FROM ledgers", null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return false
            val row = listOf(cursor.getString(1), cursor.getString(2), cursor.getString(3),
                cursor.getString(4), cursor.getString(5))
            if (cursor.moveToNext() || row != listOf("日常", "日常账本", "", "0", "tabler_book")) return false
            cursor.moveToFirst()
            cursor.getLong(0)
        }
        val dependentTables = listOf(
            "assets", "records", "recurring_records", "ai_chat_sessions", "ai_chat_messages",
            "ledger_shared_assets", "ledger_shared_ledgers", "backup_import_map", "backup_asset_snapshot"
        )
        if (dependentTables.any { table ->
                db.rawQuery("SELECT 1 FROM $table LIMIT 1", null).use { it.moveToFirst() }
            }) return false
        val expected = mapOf(
            "0||购物" to "ic_category_shopping", "0||餐饮" to "ic_category_food",
            "0||居住" to "ic_category_housing", "0||交通" to "ic_category_transport",
            "0|购物|服饰" to "tabler_shirt", "0|购物|家电" to "tabler_devices",
            "0|购物|数码" to "tabler_device_laptop", "0|餐饮|早午晚餐" to "tabler_tools_kitchen",
            "0|居住|房租" to "tabler_home", "0|居住|酒店" to "tabler_hotel_service",
            "0|交通|短途" to "tabler_car", "0|交通|飞机高铁" to "tabler_plane",
            "1||工作" to "tabler_briefcase", "1||理财" to "tabler_building_bank",
            "1|工作|工资" to "ic_category_salary", "1|工作|报销" to "tabler_receipt",
            "1|理财|股票" to "tabler_chart_line", "1|理财|基金" to "tabler_chart_donut",
            "1|理财|黄金" to "tabler_pig_money"
        )
        val actual = mutableMapOf<String, String>()
        val nonzeroSortOrders = mapOf(
            "0||餐饮" to 1, "0||居住" to 2, "0||交通" to 3,
            "1||理财" to 1, "1|工作|报销" to 1,
            "1|理财|基金" to 1, "1|理财|黄金" to 2
        )
        val matches = db.rawQuery(
            "SELECT c.type, COALESCE(p.name, ''), c.name, c.icon, c.ledger_id, c.color, c.sort_order " +
                "FROM categories c LEFT JOIN categories p ON p.id = c.parent_id", null
        ).use { cursor ->
            var count = 0
            while (cursor.moveToNext()) {
                count++
                if (cursor.getLong(4) != seedLedger) return@use false
                val key = "${cursor.getInt(0)}|${cursor.getString(1)}|${cursor.getString(2)}"
                if (cursor.getString(5) != "#F5F5F5" ||
                    cursor.getInt(6) != (nonzeroSortOrders[key] ?: 0)) return@use false
                if (actual.put(key, cursor.getString(3)) != null) return@use false
            }
            count == expected.size && actual == expected
        }
        if (!matches) return false
        db.delete("categories", null, null)
        db.delete("ledgers", "id = ?", arrayOf(seedLedger.toString()))
        return true
    }

    private fun repairMissingRecordPhotos(
        db: SQLiteDatabase, localId: Long, source: JSONObject, photos: Map<String, String>
    ) {
        val currentUris = db.rawQuery(
            "SELECT photo_uri, photo_uris FROM records WHERE id = ?",
            arrayOf(localId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return
            cursor.getString(1).orEmpty().split('|').filter(String::isNotBlank)
                .ifEmpty { cursor.getString(0)?.let { listOf(it) }.orEmpty() }
        }
        if (currentUris.isEmpty()) return
        val hasMissingFile = currentUris.any { uri ->
            !runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { true } == true
            }.getOrDefault(false)
        }
        if (!hasMissingFile) return
        val originals = if (!source.isNull("photo_uris")) {
            source.getString("photo_uris").split('|').filter(String::isNotBlank)
        } else emptyList()
        val sourceUris = originals.ifEmpty {
            if (source.isNull("photo_uri")) emptyList()
            else listOf(source.getString("photo_uri"))
        }
        if (sourceUris.isEmpty()) return
        val restored = sourceUris.map { photos[it] ?: error("Missing record photo") }
        db.update(
            "records",
            ContentValues().apply {
                put("photo_uri", restored.first())
                put("photo_uris", restored.joinToString("|"))
            },
            "id = ?",
            arrayOf(localId.toString())
        )
    }

    private fun requiredId(value: Long?): Long = value ?: error("Missing referenced backup row")

    private fun rows(tables: JSONObject, table: String): List<JSONObject> {
        val array = tables.getJSONArray(table)
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private fun rowValues(row: JSONObject): ContentValues = ContentValues().apply {
        row.keys().forEach { key ->
            when (val value = row.get(key)) {
                JSONObject.NULL -> putNull(key)
                is Boolean -> put(key, if (value) 1 else 0)
                is Int -> put(key, value)
                is Long -> put(key, value)
                is Double -> put(key, value)
                else -> put(key, value.toString())
            }
        }
    }

    private fun typedPreferences(): JSONObject {
        val all = JSONObject()
        val names = KNOWN_PREFS + File(context.applicationInfo.dataDir, "shared_prefs")
            .listFiles().orEmpty().filter { it.extension == "xml" }.map { it.nameWithoutExtension }
        names.filterNot { it in EXCLUDED_PREFS }.forEach { name ->
                val values = JSONObject()
                context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .all.forEach { (key, value) ->
                        val type = when (value) {
                            is Boolean -> "boolean"
                            is Int -> "int"
                            is Long -> "long"
                            is Float -> "float"
                            is Set<*> -> "string_set"
                            else -> "string"
                        }
                        val encoded = if (value is Set<*>) JSONArray(value.map { it.toString() })
                        else value ?: JSONObject.NULL
                        values.put(key, JSONObject().put("type", type).put("value", encoded))
                    }
                all.put(name, values)
            }
        return all
    }

    private fun validatePreferences(all: JSONObject) {
        all.keys().forEach { name ->
            require(name !in EXCLUDED_PREFS && name.isNotBlank() && name != "." &&
                name != ".." && !name.contains('/') && !name.contains('\\')) {
                "Invalid preference file"
            }
            val values = all.getJSONObject(name)
            values.keys().forEach { key ->
                val item = values.getJSONObject(key)
                val type = item.getString("type")
                require(type in PREF_TYPES) { "Invalid preference type" }
                val expectedType = expectedPreferenceType(name, key)
                require(expectedType == null || expectedType == type) {
                    "Incompatible preference type"
                }
                require(item.has("value")) { "Missing preference value" }
                val value = item.get("value")
                require(
                    when (type) {
                        "boolean" -> value is Boolean
                        "int", "long", "float" -> value is Number
                        "string" -> value is String
                        "string_set" -> value is JSONArray &&
                            (0 until value.length()).all { value.opt(it) is String }
                        else -> false
                    }
                ) { "Invalid preference value" }
            }
        }
    }

    private fun expectedPreferenceType(name: String, key: String): String? = when (name) {
        "ai_assistant_settings_prefs" -> when (key) {
            "ai_assistant_enabled", "explicit_api_config", "record_tools_enabled" -> "boolean"
            "active_session_id" -> "long"
            "ai_api_key", "minimax_model", "minimax_base_url" -> "string"
            else -> null
        }
        "quick_add_prefs" -> if (key == "quick_add_enabled") "boolean" else null
        "asset_display_prefs" -> if (key == "show_asset_enabled") "boolean" else null
        "scroll_top_fab_prefs" -> if (key == "scroll_top_fab_enabled") "boolean" else null
        "category_hierarchy_settings_prefs" ->
            if (key == "category_hierarchy_max_depth") "int" else null
        "record_photo_settings" -> if (key == "max_photos_per_record") "int" else null
        "cardtally_preferences" -> if (key == "income_expense_color_mode") "int" else null
        "theme_prefs" -> if (key in setOf("theme_mode", "card_opacity", "wallpaper_palette")) "int" else null
        "ledger_ux_prefs" -> when (key) {
            "ledger_ux_schema_version" -> "int"
            "ledger_startup_behavior", "ledger_last_view" -> "string"
            else -> null
        }
        "ledger_session" -> if (key == "current_ledger_id") "long" else null
        "default_record_asset_prefs" -> if (key.endsWith("_asset_id")) "long" else null
        "record_category_order_prefs" -> "string"
        "record_entry_mode_prefs" -> if (key == "record_entry_mode") "string" else null
        "statistics_ranking_mode_prefs" ->
            if (key == "statistics_ranking_mode") "string" else null
        "language_prefs" -> when (key) {
            "app_language" -> "string"
            "pending_locale_transition" -> "boolean"
            else -> null
        }
        else -> null
    }

    private fun writePreferences(
        all: JSONObject, ids: Map<String, Map<Long, Long>>, overwriteExisting: Boolean
    ) {
        all.keys().forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val values = all.getJSONObject(name)
            values.keys().forEach preference@ { key ->
                if (!overwriteExisting && prefs.contains(key)) return@preference
                val item = values.getJSONObject(key)
                val value = item.get("value")
                val type = item.getString("type")
                val targetTable = when {
                    name == "ledger_session" && key == "current_ledger_id" -> "ledgers"
                    name == "default_record_asset_prefs" && key.endsWith("_asset_id") -> "assets"
                    name == "ai_assistant_settings_prefs" && key == "active_session_id" -> "ai_chat_sessions"
                    else -> null
                }
                val remapped = if (targetTable != null && value is Number && value.toLong() > 0L) {
                    ids[targetTable]?.get(value.toLong()) ?: 0L
                } else null
                when (type) {
                    "boolean" -> editor.putBoolean(key, value as Boolean)
                    "int" -> editor.putInt(key, (remapped ?: (value as Number).toLong()).toInt())
                    "long" -> editor.putLong(key, remapped ?: (value as Number).toLong())
                    "float" -> editor.putFloat(key, (value as Number).toFloat())
                    "string" -> {
                        val restored = if (name == "record_category_order_prefs") {
                            value.toString().split(',').mapNotNull { it.toLongOrNull() }
                                .mapNotNull { ids["categories"]?.get(it) }.joinToString(",")
                        } else value.toString()
                        editor.putString(key, restored)
                    }
                    "string_set" -> {
                        val array = value as JSONArray
                        editor.putStringSet(key, (0 until array.length()).map { array.getString(it) }.toSet())
                    }
                }
            }
            check(editor.commit()) { "Could not restore preferences" }
        }
    }

    private fun encodePendingPreferences(
        preferences: JSONObject,
        ids: Map<String, Map<Long, Long>>,
        overwriteExisting: Boolean
    ): JSONObject = JSONObject().apply {
        put("preferences", preferences)
        put("overwriteExisting", overwriteExisting)
        put("ids", JSONObject().apply {
            ids.forEach { (table, mapping) ->
                put(table, JSONObject().apply {
                    mapping.forEach { (sourceId, localId) -> put(sourceId.toString(), localId) }
                })
            }
        })
    }

    /** Replays settings left pending when a previous import stopped after its DB commit. */
    private fun replayPendingPreferences() {
        val payload = DatabaseHelper(context).use { helper ->
            helper.readableDatabase.rawQuery(
                "SELECT payload FROM backup_pending_settings WHERE id = 1", null
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } ?: return
        val pending = JSONObject(payload)
        val ids = mutableMapOf<String, Map<Long, Long>>()
        val encodedIds = pending.getJSONObject("ids")
        encodedIds.keys().forEach { table ->
            val tableIds = encodedIds.getJSONObject(table)
            ids[table] = buildMap {
                tableIds.keys().forEach { sourceId ->
                    put(sourceId.toLong(), tableIds.getLong(sourceId))
                }
            }
        }
        writePreferences(
            pending.getJSONObject("preferences"),
            ids,
            overwriteExisting = pending.getBoolean("overwriteExisting")
        )
        DatabaseHelper(context).use { helper ->
            helper.writableDatabase.delete("backup_pending_settings", "id = 1", null)
        }
    }

    private fun sourceId(): String {
        val preferences = context.getSharedPreferences("backup_internal", Context.MODE_PRIVATE)
        val existing = preferences.getString("source_id", null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        check(preferences.edit().putString("source_id", created).commit())
        return created
    }

    private fun readLimited(input: InputStream, maxBytes: Int): ByteArray {
        val bytes = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(bytes.size() + count <= maxBytes) { "Backup manifest is too large" }
            bytes.write(buffer, 0, count)
        }
        return bytes.toByteArray()
    }

    companion object {
        private val ENCRYPTED_MAGIC = byteArrayOf('C'.code.toByte(), 'T'.code.toByte(), 'B'.code.toByte(), '3'.code.toByte())
        private const val FORMAT = "cardtally-backup"
        private const val VERSION = 2
        private const val MAX_MANIFEST_BYTES = 128 * 1024 * 1024
        private const val MAX_PHOTO_BYTES = 50L * 1024 * 1024
        private const val MAX_TOTAL_PHOTO_BYTES = 1024L * 1024 * 1024
        private const val MAX_ENCRYPTED_BYTES = 1200L * 1024 * 1024
        private val EXCLUDED_PREFS = setOf("beta_reset_control", "backup_internal")
        private val KNOWN_PREFS = setOf(
            "ai_assistant_settings_prefs", "asset_display_prefs", "quick_add_prefs",
            "record_photo_settings", "record_entry_mode_prefs", "ledger_ux_prefs",
            "category_hierarchy_settings_prefs", "ledger_session", "theme_prefs",
            "default_record_asset_prefs", "cardtally_preferences", "language_prefs",
            "statistics_ranking_mode_prefs", "scroll_top_fab_prefs",
            "record_category_order_prefs"
        )
        private val PREF_TYPES = setOf("boolean", "int", "long", "float", "string", "string_set")
        private val ROW_TABLES = listOf(
            "ledgers", "ledger_shared_assets", "ledger_shared_ledgers", "categories", "assets",
            "records", "recurring_records", "ai_chat_sessions", "ai_chat_messages"
        )
        private val ID_TABLES = listOf(
            "ledgers", "categories", "assets", "records", "recurring_records",
            "ai_chat_sessions", "ai_chat_messages"
        )
    }
}
