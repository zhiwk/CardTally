package com.example.cardtally.ai

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** Local operation lifecycle only. No credentials, hidden reasoning, or provider bodies. */
class AiRecordAuditStore(private val db: SQLiteDatabase) {
    fun propose(id: String, session: Long, ledger: Long, provider: String, model: String,
                tool: String, target: Long?, fingerprint: String) {
        db.insertOrThrow(TABLE, null, ContentValues().apply {
            put("operation_id", id); put("session_id", session); put("ledger_id", ledger)
            put("provider", provider); put("model", model); put("tool", tool); put("target_id", target)
            put("fingerprint", fingerprint); put("state", "proposed"); put("created_at", System.currentTimeMillis())
        })
    }
    fun transition(id: String, from: String, to: String): Boolean = db.update(TABLE,
        ContentValues().apply { put("state", to); if (to == "confirmed") put("confirmed_at", System.currentTimeMillis()) }, "operation_id = ? AND state = ?", arrayOf(id, from)) == 1
    fun recordConfirmation(id: String): Boolean = db.update(TABLE,
        ContentValues().apply { put("confirmed_at", System.currentTimeMillis()) },
        "operation_id = ? AND state = 'proposed'", arrayOf(id)) == 1
    fun finish(id: String, state: String, result: String? = null) {
        db.update(TABLE, ContentValues().apply {
            put("state", state); put("result", result); put("finished_at", System.currentTimeMillis())
            if (state == "success" && result != null) put("target_id", org.json.JSONObject(result).getLong("record_id"))
        }, "operation_id = ? AND state NOT IN ('success','rejected','cancelled','expired','failed')", arrayOf(id))
    }
    fun state(id: String): String? = db.rawQuery("SELECT state FROM $TABLE WHERE operation_id = ?", arrayOf(id))
        .use { if (it.moveToFirst()) it.getString(0) else null }
    data class Entry(val createdAt: Long, val tool: String, val target: Long?, val state: String)
    fun recent(): List<Entry> = db.rawQuery(
        "SELECT created_at, tool, target_id, state FROM $TABLE ORDER BY created_at DESC LIMIT 50", null
    ).use { cursor -> buildList {
        while (cursor.moveToNext()) add(Entry(cursor.getLong(0), cursor.getString(1),
            if (cursor.isNull(2)) null else cursor.getLong(2), cursor.getString(3)))
    } }
    fun expireUnfinished() {
        db.execSQL("UPDATE $TABLE SET state = 'expired', finished_at = ? WHERE state IN ('proposed','confirmed')",
            arrayOf(System.currentTimeMillis()))
    }
    fun clear() { db.delete(TABLE, null, null) }
    companion object {
        const val TABLE = "ai_record_audit"
        const val CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS ai_record_audit (" +
            "operation_id TEXT PRIMARY KEY, session_id INTEGER NOT NULL, ledger_id INTEGER NOT NULL, " +
            "provider TEXT NOT NULL, model TEXT NOT NULL, tool TEXT NOT NULL, target_id INTEGER, " +
            "fingerprint TEXT NOT NULL, state TEXT NOT NULL, result TEXT, created_at INTEGER NOT NULL, confirmed_at INTEGER, finished_at INTEGER)"
    }
}
