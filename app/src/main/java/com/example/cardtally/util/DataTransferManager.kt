package com.example.cardtally.util

import android.content.Context
import android.content.SharedPreferences
import com.example.cardtally.database.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** JSON backup/merge support. Database amounts remain exact integer fen values. */
class DataTransferManager(private val context: Context) : AutoCloseable {
    data class ImportResult(
        val ledgers: Int,
        val categories: Int,
        val assets: Int,
        val records: Int,
        val recurring: Int,
        val sessions: Int,
        val skipped: Int
    )

    private val databaseHelper = DatabaseHelper(context)

    fun exportJson(): String {
        val db = databaseHelper.readableDatabase
        val root = JSONObject()
            .put("format", "cardtally-json")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
        val tables = JSONObject()
        TABLES.forEach { table -> tables.put(table, readRows(db, table)) }
        root.put("tables", tables)
        root.put("preferences", readPreferences())
        return root.toString(2)
    }

    fun importJson(json: String): ImportResult {
        val root = JSONObject(json)
        require(root.optString("format") == "cardtally-json") { "Unsupported JSON format" }
        val tables = root.optJSONObject("tables") ?: throw IllegalArgumentException("Missing tables")
        val db = databaseHelper.writableDatabase
        val ledgerMap = mutableMapOf<Long, Long>()
        val categoryMap = mutableMapOf<Long, Long>()
        val assetMap = mutableMapOf<Long, Long>()
        val sessionMap = mutableMapOf<Long, Long>()
        var skipped = 0
        var ledgerCount = 0
        var categoryCount = 0
        var assetCount = 0
        var recordCount = 0
        var recurringCount = 0
        var sessionCount = 0

        db.beginTransaction()
        try {
            rows(tables, "ledgers").forEach { row ->
                val oldId = row.optLong("id", 0L)
                if (oldId <= 0L) return@forEach
                val name = row.optString("name").ifBlank { "导入账本" }
                val existing = db.rawQuery(
                    "SELECT id FROM ledgers WHERE name = ? AND subtitle = ? LIMIT 1",
                    arrayOf(name, row.optString("subtitle"))
                ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
                val id = existing.takeIf { it > 0 } ?: db.insertOrThrow(
                    "ledgers", null, values(row, setOf("id"), columns(db, "ledgers"))
                )
                ledgerMap[oldId] = id
                if (existing == 0L) ledgerCount++
            }

            rows(tables, "categories").forEach { row ->
                val oldId = row.optLong("id", 0L)
                if (oldId <= 0L) return@forEach
                val parent = row.optLong("parent_id", 0L).takeIf { it > 0 }?.let { categoryMap[it] }
                val name = row.optString("name").ifBlank { "其他" }
                val type = row.optInt("type", 0).coerceIn(0, 1)
                val existing = db.rawQuery(
                    "SELECT id FROM categories WHERE name = ? AND type = ? AND " +
                        "COALESCE(parent_id, 0) = ? LIMIT 1",
                    arrayOf(name, type.toString(), (parent ?: 0L).toString())
                ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
                val content = values(row, setOf("id", "parent_id", "ledger_id"), columns(db, "categories"))
                content.put("name", name)
                content.put("type", type)
                if (parent == null) content.putNull("parent_id") else content.put("parent_id", parent)
                val id = existing.takeIf { it > 0 } ?: db.insertOrThrow("categories", null, content)
                categoryMap[oldId] = id
                if (existing == 0L) categoryCount++
            }

            rows(tables, "assets").forEach { row ->
                val oldId = row.optLong("id", 0L)
                if (oldId <= 0L) return@forEach
                val ledgerId = ledgerMap[row.optLong("ledger_id", 0L)] ?: databaseHelper.getMasterLedgerId()
                val name = row.optString("name").ifBlank { "导入资产" }
                val type = row.optInt("type", 0).coerceIn(0, 3)
                val existing = db.rawQuery(
                    "SELECT id FROM assets WHERE ledger_id = ? AND name = ? AND type = ? LIMIT 1",
                    arrayOf(ledgerId.toString(), name, type.toString())
                ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
                val content = values(row, setOf("id", "ledger_id"), columns(db, "assets"))
                content.put("ledger_id", ledgerId)
                content.put("name", name)
                content.put("type", type)
                val id = existing.takeIf { it > 0 } ?: db.insertOrThrow("assets", null, content)
                assetMap[oldId] = id
                if (existing == 0L) assetCount++
            }

            val existingRecordKeys = mutableSetOf<String>()
            val existingRecords = readRows(db, "records")
            for (index in 0 until existingRecords.length()) {
                existingRecordKeys += recordKey(existingRecords.getJSONObject(index), emptyMap(), emptyMap(), emptyMap())
            }
            rows(tables, "records").forEach { row ->
                val type = row.optInt("type", 0).coerceIn(0, 2)
                val amount = row.optLong("amount", Long.MIN_VALUE)
                if (amount <= 0L) { skipped++; return@forEach }
                val mapped = JSONObject(row.toString()).apply {
                    put("ledger_id", ledgerMap[optLong("ledger_id", 0L)] ?: databaseHelper.getMasterLedgerId())
                    optLong("category_id", 0L).takeIf { it > 0 }?.let { put("category_id", categoryMap[it] ?: JSONObject.NULL) }
                    optLong("asset_id", 0L).takeIf { it > 0 }?.let { put("asset_id", assetMap[it] ?: JSONObject.NULL) }
                    optLong("destination_asset_id", 0L).takeIf { it > 0 }?.let { put("destination_asset_id", assetMap[it] ?: JSONObject.NULL) }
                    put("type", type)
                }
                if (type == 2 && (mapped.optLong("asset_id", 0L) <= 0L || mapped.optLong("destination_asset_id", 0L) <= 0L)) {
                    mapped.put("type", 0)
                    mapped.put("destination_asset_id", JSONObject.NULL)
                }
                val key = recordKey(mapped, emptyMap(), emptyMap(), emptyMap())
                if (!existingRecordKeys.add(key)) return@forEach
                val content = values(mapped, setOf("id"), columns(db, "records"))
                content.remove("category_id")
                mapped.optLong("category_id", 0L).takeIf { it > 0 }?.let { content.put("category_id", it) }
                content.remove("asset_id")
                mapped.optLong("asset_id", 0L).takeIf { it > 0 }?.let { content.put("asset_id", it) }
                content.remove("destination_asset_id")
                mapped.optLong("destination_asset_id", 0L).takeIf { it > 0 }?.let { content.put("destination_asset_id", it) }
                db.insertOrThrow("records", null, content)
                recordCount++
            }

            rows(tables, "recurring_records").forEach { row ->
                val oldId = row.optLong("id", 0L)
                val ledger = ledgerMap[row.optLong("ledger_id", 0L)] ?: databaseHelper.getMasterLedgerId()
                val category = row.optLong("category_id", 0L).takeIf { it > 0 }?.let { categoryMap[it] }
                val asset = row.optLong("asset_id", 0L).takeIf { it > 0 }?.let { assetMap[it] }
                val duplicate = db.rawQuery(
                    "SELECT 1 FROM recurring_records WHERE ledger_id = ? AND name = ? AND amount = ? AND type = ? AND start_date = ? LIMIT 1",
                    arrayOf(ledger.toString(), row.optString("name"), row.optLong("amount").toString(), row.optInt("type").toString(), row.optString("start_date"))
                ).use { it.moveToFirst() }
                if (!duplicate && oldId > 0L) {
                    val content = values(row, setOf("id", "ledger_id", "category_id", "asset_id"), columns(db, "recurring_records"))
                    content.put("ledger_id", ledger)
                    if (category == null) content.putNull("category_id") else content.put("category_id", category)
                    if (asset == null) content.putNull("asset_id") else content.put("asset_id", asset)
                    db.insertOrThrow("recurring_records", null, content)
                    recurringCount++
                }
            }

            rows(tables, "ledger_shared_ledgers").forEach { row ->
                val ledger = ledgerMap[row.optLong("ledger_id", 0L)] ?: return@forEach
                val source = ledgerMap[row.optLong("source_ledger_id", 0L)] ?: return@forEach
                db.execSQL("INSERT OR IGNORE INTO ledger_shared_ledgers(shared_ledger_id, shared_source_ledger_id) VALUES (?, ?)", arrayOf(ledger, source))
            }
            rows(tables, "ledger_shared_assets").forEach { row ->
                val ledger = ledgerMap[row.optLong("ledger_id", 0L)] ?: return@forEach
                val asset = assetMap[row.optLong("asset_id", 0L)] ?: return@forEach
                db.execSQL("INSERT OR IGNORE INTO ledger_shared_assets(shared_ledger_id, shared_asset_id) VALUES (?, ?)", arrayOf(ledger, asset))
            }

            rows(tables, "ai_chat_sessions").forEach { row ->
                val oldId = row.optLong("id", 0L)
                if (oldId <= 0L) return@forEach
                val existing = db.rawQuery("SELECT id FROM ai_chat_sessions WHERE title = ? AND created_at = ? LIMIT 1", arrayOf(row.optString("title"), row.optLong("created_at").toString())).use { if (it.moveToFirst()) it.getLong(0) else 0L }
                val id = existing.takeIf { it > 0 } ?: db.insertOrThrow("ai_chat_sessions", null, values(row, setOf("id"), columns(db, "ai_chat_sessions")))
                sessionMap[oldId] = id
                if (existing == 0L) sessionCount++
            }
            rows(tables, "ai_chat_messages").forEach { row ->
                val session = sessionMap[row.optLong("session_id", 0L)] ?: return@forEach
                val duplicate = db.rawQuery("SELECT 1 FROM ai_chat_messages WHERE session_id = ? AND role = ? AND content = ? AND created_at = ? LIMIT 1", arrayOf(session.toString(), row.optString("role"), row.optString("content"), row.optLong("created_at").toString())).use { it.moveToFirst() }
                if (!duplicate) {
                    val content = values(row, setOf("id"), columns(db, "ai_chat_messages"))
                    content.put("session_id", session)
                    db.insertOrThrow("ai_chat_messages", null, content)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        writePreferences(root.optJSONObject("preferences"))
        return ImportResult(ledgerCount, categoryCount, assetCount, recordCount, recurringCount, sessionCount, skipped)
    }

    override fun close() = databaseHelper.close()

    private fun readRows(db: android.database.sqlite.SQLiteDatabase, table: String): JSONArray {
        val result = JSONArray()
        db.rawQuery("SELECT * FROM $table", null).use { cursor ->
            while (cursor.moveToNext()) {
                val row = JSONObject()
                for (index in 0 until cursor.columnCount) {
                    if (cursor.isNull(index)) row.put(cursor.getColumnName(index), JSONObject.NULL)
                    else when (cursor.getType(index)) {
                        1 -> row.put(cursor.getColumnName(index), cursor.getLong(index))
                        2 -> row.put(cursor.getColumnName(index), cursor.getDouble(index))
                        else -> row.put(cursor.getColumnName(index), cursor.getString(index))
                    }
                }
                result.put(row)
            }
        }
        return result
    }

    private fun rows(tables: JSONObject, name: String): List<JSONObject> {
        val array = tables.optJSONArray(name) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    private fun values(row: JSONObject, excluded: Set<String>, allowed: Set<String>? = null): android.content.ContentValues =
        android.content.ContentValues().also { result ->
            row.keys().forEach { key ->
                if (key !in excluded && (allowed == null || key in allowed) && !row.isNull(key)) {
                    when (val value = row.get(key)) {
                        is Number -> result.put(key, value.toString())
                        is Boolean -> result.put(key, if (value) 1 else 0)
                        else -> result.put(key, value.toString())
                    }
                }
            }
        }

    private fun columns(db: android.database.sqlite.SQLiteDatabase, table: String): Set<String> =
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
        }

    private fun recordKey(row: JSONObject, ignoredA: Map<Long, Long>, ignoredB: Map<Long, Long>, ignoredC: Map<Long, Long>): String = listOf(
        row.optLong("ledger_id"), row.optString("date"), row.optLong("amount"), row.optInt("type"),
        row.optString("description"), row.optLong("category_id"), row.optLong("asset_id"),
        row.optLong("destination_asset_id"), row.optLong("fee"), row.optString("photo_uris")
    ).joinToString("|")

    private fun readPreferences(): JSONObject {
        val result = JSONObject()
        val directory = File(context.applicationInfo.dataDir, "shared_prefs")
        directory.listFiles()?.filter { it.extension == "xml" }?.forEach { file ->
            val name = file.nameWithoutExtension
            if (name != "beta_reset_control") {
                val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                result.put(name, JSONObject().apply { prefs.all.forEach { (key, value) -> put(key, value) } })
            }
        }
        return result
    }

    private fun writePreferences(all: JSONObject?) {
        all ?: return
        all.keys().forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val values = all.optJSONObject(name) ?: return@forEach
            values.keys().forEach { key ->
                val value = values.get(key)
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Number -> editor.putLong(key, value.toLong())
                    JSONObject.NULL -> editor.remove(key)
                    else -> editor.putString(key, value.toString())
                }
            }
            editor.apply()
        }
    }

    companion object {
        private val TABLES = listOf(
            "ledgers", "ledger_shared_assets", "ledger_shared_ledgers", "categories", "assets",
            "records", "record_deletion_undo", "recurring_records", "ai_chat_sessions", "ai_chat_messages"
        )
    }
}
