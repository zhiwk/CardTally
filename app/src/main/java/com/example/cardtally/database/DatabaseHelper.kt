package com.example.cardtally.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import com.example.cardtally.model.AiChatSession
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.util.CategoryHierarchySettingsHelper
import com.example.cardtally.util.LedgerSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DatabaseHelper(
    private val appContext: Context
) : SQLiteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    enum class CategoryOperationError {
        PARENT_NOT_FOUND,
        PARENT_TYPE_MISMATCH,
        SELF_PARENT,
        DESCENDANT_CYCLE,
        MAX_DEPTH_EXCEEDED,
        HAS_CHILDREN,
        IN_USE_BY_RECORDS
    }

    class CategoryOperationException(
        val error: CategoryOperationError
    ) : IllegalArgumentException(error.name)

    enum class AssetOperationError {
        NOT_ARCHIVED,
        IN_USE_BY_RECORDS
    }

    class AssetOperationException(
        val error: AssetOperationError
    ) : IllegalArgumentException(error.name)

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val DATABASE_VERSION = 28
        const val MAX_RECORD_QUERY_LIMIT = 200
        const val RECORD_UNDO_WINDOW_MS = 10_000L

        private const val TABLE_RECORDS = "records"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_RECORD_CATEGORY_ID = "category_id"
        private const val COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT = "category_name_snapshot"
        private const val COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT = "category_path_snapshot"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_RECORD_ASSET_ID = "asset_id"
        private const val COLUMN_RECORD_DESTINATION_ASSET_ID = "destination_asset_id"
        private const val COLUMN_ASSET_SOURCE = "asset_source"
        private const val COLUMN_DESTINATION_ASSET_SOURCE = "destination_asset_source"
        private const val COLUMN_PHOTO_URI = "photo_uri"
        private const val COLUMN_PHOTO_URIS = "photo_uris"
        private const val PHOTO_URI_SEPARATOR = "|"
        private const val COLUMN_SORT_ORDER = "sort_order"
        private const val COLUMN_LEDGER_ID = "ledger_id"

        private const val TABLE_LEDGERS = "ledgers"
        private const val COLUMN_LEDGER_NAME = "name"
        private const val COLUMN_LEDGER_SUBTITLE = "subtitle"
        private const val COLUMN_LEDGER_SORT_ORDER = "sort_order"
        private const val COLUMN_LEDGER_ICON_NAME = "icon_name"
        private const val COLUMN_LEDGER_SHARED_ASSET_IDS = "shared_asset_ids"
        private const val TABLE_LEDGER_SHARED_ASSETS = "ledger_shared_assets"
        private const val COLUMN_SHARED_LEDGER_ID = "ledger_id"
        private const val COLUMN_SHARED_ASSET_ID = "asset_id"
        private const val TABLE_LEDGER_SHARED_LEDGERS = "ledger_shared_ledgers"
        private const val COLUMN_SHARED_SOURCE_LEDGER_ID = "source_ledger_id"

        private const val TABLE_RECORD_DELETION_UNDO = "record_deletion_undo"
        private const val COLUMN_UNDO_TOKEN = "undo_token"
        private const val COLUMN_UNDO_EXPIRES_AT = "expires_at"
        private const val COLUMN_UNDO_BALANCE_DELTA = "balance_delta"

        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_CATEGORY_ID = "id"
        private const val COLUMN_CATEGORY_NAME = "name"
        private const val COLUMN_CATEGORY_TYPE = "type"
        private const val COLUMN_CATEGORY_ICON = "icon"
        private const val COLUMN_CATEGORY_PARENT_ID = "parent_id"
        private const val COLUMN_CATEGORY_SORT_ORDER = "sort_order"

        private const val TABLE_ASSETS = "assets"
        private const val COLUMN_ASSET_ID = "id"
        private const val COLUMN_ASSET_NAME = "name"
        private const val COLUMN_ASSET_AMOUNT = "amount"
        private const val COLUMN_ASSET_TYPE = "type"
        private const val COLUMN_ASSET_CATEGORY_LABEL = "category_label"
        private const val COLUMN_ASSET_CATEGORY_ICON_NAME = "category_icon_name"
        private const val COLUMN_ASSET_IS_ARCHIVED = "is_archived"
        private const val COLUMN_ASSET_IS_PINNED = "is_pinned"
        private const val COLUMN_ASSET_INCLUDE_IN_TOTAL = "include_in_total"

        private const val TABLE_AI_CHAT_SESSIONS = "ai_chat_sessions"
        private const val COLUMN_AI_CHAT_SESSION_ID = "id"
        private const val COLUMN_AI_CHAT_SESSION_TITLE = "title"
        private const val COLUMN_AI_CHAT_SESSION_CREATED_AT = "created_at"
        private const val COLUMN_AI_CHAT_SESSION_UPDATED_AT = "updated_at"

        private const val TABLE_AI_CHAT_MESSAGES = "ai_chat_messages"
        private const val COLUMN_AI_CHAT_MESSAGE_ID = "id"
        private const val COLUMN_AI_CHAT_MESSAGE_SESSION_ID = "session_id"
        private const val COLUMN_AI_CHAT_MESSAGE_ROLE = "role"
        private const val COLUMN_AI_CHAT_MESSAGE_CONTENT = "content"
        private const val COLUMN_AI_CHAT_MESSAGE_IS_ERROR = "is_error"
        private const val COLUMN_AI_CHAT_MESSAGE_CREATED_AT = "created_at"

        private const val CREATE_TABLE_RECORDS =
            "CREATE TABLE $TABLE_RECORDS (" +
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_DATE TEXT NOT NULL, " +
            "$COLUMN_AMOUNT REAL NOT NULL, " +
            "$COLUMN_CATEGORY TEXT NOT NULL, " +
            "$COLUMN_RECORD_CATEGORY_ID INTEGER, " +
            "$COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT TEXT, " +
            "$COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT TEXT, " +
            "$COLUMN_TYPE INTEGER NOT NULL, " +
            "$COLUMN_DESCRIPTION TEXT, " +
            "$COLUMN_RECORD_ASSET_ID INTEGER, " +
            "$COLUMN_RECORD_DESTINATION_ASSET_ID INTEGER, " +
            "$COLUMN_ASSET_SOURCE TEXT, " +
            "$COLUMN_DESTINATION_ASSET_SOURCE TEXT, " +
            "$COLUMN_PHOTO_URI TEXT, " +
            "$COLUMN_PHOTO_URIS TEXT, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1, " +
            "$COLUMN_SORT_ORDER INTEGER DEFAULT 0)"

        private const val CREATE_TABLE_CATEGORIES =
            "CREATE TABLE $TABLE_CATEGORIES (" +
            "$COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_CATEGORY_NAME TEXT NOT NULL, " +
            "$COLUMN_CATEGORY_TYPE INTEGER NOT NULL, " +
            "$COLUMN_CATEGORY_ICON TEXT, " +
            "$COLUMN_CATEGORY_PARENT_ID INTEGER, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1, " +
            "$COLUMN_CATEGORY_SORT_ORDER INTEGER NOT NULL DEFAULT 0)"

        private const val CREATE_TABLE_ASSETS =
            "CREATE TABLE $TABLE_ASSETS (" +
            "$COLUMN_ASSET_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_ASSET_NAME TEXT NOT NULL, " +
            "$COLUMN_ASSET_AMOUNT REAL NOT NULL, " +
            "$COLUMN_ASSET_TYPE INTEGER NOT NULL, " +
            "$COLUMN_ASSET_CATEGORY_LABEL TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_ASSET_CATEGORY_ICON_NAME TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_ASSET_IS_ARCHIVED INTEGER DEFAULT 0, " +
            "$COLUMN_ASSET_IS_PINNED INTEGER DEFAULT 0, " +
            "$COLUMN_ASSET_INCLUDE_IN_TOTAL INTEGER DEFAULT 1, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1)"

        private const val CREATE_TABLE_RECORD_DELETION_UNDO =
            "CREATE TABLE $TABLE_RECORD_DELETION_UNDO (" +
            "$COLUMN_UNDO_TOKEN TEXT PRIMARY KEY, " +
            "$COLUMN_UNDO_EXPIRES_AT INTEGER NOT NULL, " +
            "$COLUMN_UNDO_BALANCE_DELTA REAL NOT NULL, " +
            "$COLUMN_ID INTEGER NOT NULL, " +
            "$COLUMN_DATE TEXT NOT NULL, " +
            "$COLUMN_AMOUNT REAL NOT NULL, " +
            "$COLUMN_CATEGORY TEXT NOT NULL, " +
            "$COLUMN_RECORD_CATEGORY_ID INTEGER, " +
            "$COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT TEXT, " +
            "$COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT TEXT, " +
            "$COLUMN_TYPE INTEGER NOT NULL, " +
            "$COLUMN_DESCRIPTION TEXT, " +
            "$COLUMN_RECORD_ASSET_ID INTEGER, " +
            "$COLUMN_RECORD_DESTINATION_ASSET_ID INTEGER, " +
            "$COLUMN_ASSET_SOURCE TEXT, " +
            "$COLUMN_DESTINATION_ASSET_SOURCE TEXT, " +
            "$COLUMN_PHOTO_URI TEXT, " +
            "$COLUMN_PHOTO_URIS TEXT, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1, " +
            "$COLUMN_SORT_ORDER INTEGER NOT NULL)"

        private const val CREATE_TABLE_LEDGERS =
            "CREATE TABLE $TABLE_LEDGERS (" +
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_LEDGER_NAME TEXT NOT NULL, " +
            "$COLUMN_LEDGER_SUBTITLE TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_LEDGER_SHARED_ASSET_IDS TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_LEDGER_SORT_ORDER INTEGER NOT NULL DEFAULT 0, " +
            "$COLUMN_LEDGER_ICON_NAME TEXT NOT NULL DEFAULT 'ms_rounded_book')"

        private const val CREATE_TABLE_LEDGER_SHARED_ASSETS =
            "CREATE TABLE $TABLE_LEDGER_SHARED_ASSETS (" +
            "$COLUMN_SHARED_LEDGER_ID INTEGER NOT NULL, " +
            "$COLUMN_SHARED_ASSET_ID INTEGER NOT NULL, " +
            "PRIMARY KEY ($COLUMN_SHARED_LEDGER_ID, $COLUMN_SHARED_ASSET_ID))"

        private const val CREATE_TABLE_LEDGER_SHARED_LEDGERS =
            "CREATE TABLE $TABLE_LEDGER_SHARED_LEDGERS (" +
            "$COLUMN_SHARED_LEDGER_ID INTEGER PRIMARY KEY, " +
            "$COLUMN_SHARED_SOURCE_LEDGER_ID INTEGER NOT NULL)"

        private const val CREATE_TABLE_AI_CHAT_SESSIONS =
            "CREATE TABLE $TABLE_AI_CHAT_SESSIONS (" +
            "$COLUMN_AI_CHAT_SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_AI_CHAT_SESSION_TITLE TEXT NOT NULL, " +
            "$COLUMN_AI_CHAT_SESSION_CREATED_AT INTEGER NOT NULL, " +
            "$COLUMN_AI_CHAT_SESSION_UPDATED_AT INTEGER NOT NULL)"

        private const val CREATE_TABLE_AI_CHAT_MESSAGES =
            "CREATE TABLE $TABLE_AI_CHAT_MESSAGES (" +
            "$COLUMN_AI_CHAT_MESSAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_AI_CHAT_MESSAGE_SESSION_ID INTEGER NOT NULL, " +
            "$COLUMN_AI_CHAT_MESSAGE_ROLE TEXT NOT NULL, " +
            "$COLUMN_AI_CHAT_MESSAGE_CONTENT TEXT NOT NULL, " +
            "$COLUMN_AI_CHAT_MESSAGE_IS_ERROR INTEGER DEFAULT 0, " +
            "$COLUMN_AI_CHAT_MESSAGE_CREATED_AT INTEGER NOT NULL)"

        private const val CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT =
            "CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_updated_at ON $TABLE_AI_CHAT_SESSIONS($COLUMN_AI_CHAT_SESSION_UPDATED_AT DESC)"

        private const val CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT =
            "CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session_created_at ON $TABLE_AI_CHAT_MESSAGES($COLUMN_AI_CHAT_MESSAGE_SESSION_ID, $COLUMN_AI_CHAT_MESSAGE_CREATED_AT ASC)"
    }

    data class RecordPageCursor(
        val sortOrder: Int,
        val recordId: Long
    )

    data class RecordPage(
        val records: List<Record>,
        val nextCursor: RecordPageCursor?
    )

    data class RecordDeletionToken(
        val value: String,
        val expiresAtEpochMs: Long
    )

    private fun currentLedgerId(): Long {
        val saved = LedgerSession.getCurrentId(appContext)
        if (saved != null) return saved
        val id = readableDatabase.rawQuery(
            "SELECT $COLUMN_ID FROM $TABLE_LEDGERS ORDER BY $COLUMN_LEDGER_SORT_ORDER, $COLUMN_ID LIMIT 1", null
        ).use { if (it.moveToFirst()) it.getLong(0) else 1L }
        LedgerSession.setCurrentId(appContext, id)
        return id
    }

    /** Assets owned by the active ledger plus assets explicitly shared into it. */
    private fun assetScope(): String {
        return "($COLUMN_LEDGER_ID = ${currentLedgerId()} OR " +
            "$COLUMN_LEDGER_ID IN (SELECT $COLUMN_SHARED_SOURCE_LEDGER_ID FROM $TABLE_LEDGER_SHARED_LEDGERS " +
            "WHERE $COLUMN_SHARED_LEDGER_ID = ${currentLedgerId()}))"
    }

    fun getLedgers(): List<com.example.cardtally.model.Ledger> {
        val result = mutableListOf<com.example.cardtally.model.Ledger>()
        readableDatabase.rawQuery(
            "SELECT $COLUMN_ID, $COLUMN_LEDGER_NAME, $COLUMN_LEDGER_SUBTITLE, $COLUMN_LEDGER_SORT_ORDER, $COLUMN_LEDGER_ICON_NAME " +
                "FROM $TABLE_LEDGERS ORDER BY $COLUMN_LEDGER_SORT_ORDER, $COLUMN_ID", null
        ).use { c ->
            while (c.moveToNext()) result += com.example.cardtally.model.Ledger(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3), c.getString(4))
        }
        return result
    }

    fun addLedger(name: String, subtitle: String = "", iconName: String = "ms_rounded_book"): Long {
        val db = writableDatabase
        val id = db.insertOrThrow(TABLE_LEDGERS, null, ContentValues().apply {
            put(COLUMN_LEDGER_NAME, name)
            put(COLUMN_LEDGER_SUBTITLE, subtitle.ifBlank { "${name}账本" })
            put(COLUMN_LEDGER_SORT_ORDER, getLedgers().size)
            put(COLUMN_LEDGER_ICON_NAME, iconName)
        })
        return id
    }

    fun addLedger(name: String, subtitle: String = "", sharedAssetIds: List<Long>): Long {
        return addLedger(name, subtitle, sharedAssetIds, emptyList())
    }

    fun addLedgerWithAssetPool(name: String, sharedSourceLedgerId: Long? = null, copyFromLedgerId: Long? = null, iconName: String = "ms_rounded_book"): Long {
        val id = addLedger(name, iconName = iconName)
        val db = writableDatabase
        if (sharedSourceLedgerId != null) {
            db.insertOrThrow(TABLE_LEDGER_SHARED_LEDGERS, null, ContentValues().apply {
                put(COLUMN_SHARED_LEDGER_ID, id)
                put(COLUMN_SHARED_SOURCE_LEDGER_ID, sharedSourceLedgerId)
            })
        }
        if (copyFromLedgerId != null) {
            db.query(TABLE_ASSETS, arrayOf(COLUMN_ASSET_ID), "$COLUMN_LEDGER_ID = ?", arrayOf(copyFromLedgerId.toString()), null, null, null).use { cursor ->
                while (cursor.moveToNext()) copyAssetToLedgerInternal(db, cursor.getLong(0), id)
            }
        }
        return id
    }

    fun getSharedSourceLedgerId(ledgerId: Long): Long? = readableDatabase.rawQuery(
        "SELECT $COLUMN_SHARED_SOURCE_LEDGER_ID FROM $TABLE_LEDGER_SHARED_LEDGERS WHERE $COLUMN_SHARED_LEDGER_ID = ?",
        arrayOf(ledgerId.toString())
    ).use { if (it.moveToFirst()) it.getLong(0) else null }

    /** Copies a shared pool to one ledger and normalizes a one-ledger remainder to independent pools. */
    fun forkLedgerAssetPool(ledgerId: Long): Boolean {
        fun poolRoot(id: Long): Long {
            val visited = mutableSetOf<Long>()
            var root = id
            while (visited.add(root)) root = getSharedSourceLedgerId(root) ?: break
            return root
        }
        val root = poolRoot(ledgerId)
        if (root == ledgerId) return false
        val members = getLedgers().map { it.id }.filter { poolRoot(it) == root }
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val poolAssetIds = mutableSetOf<Long>()
            members.forEach { memberId ->
                db.query(TABLE_ASSETS, arrayOf(COLUMN_ASSET_ID), "$COLUMN_LEDGER_ID = ?", arrayOf(memberId.toString()), null, null, null).use { cursor ->
                    while (cursor.moveToNext()) poolAssetIds += cursor.getLong(0)
                }
            }
            poolAssetIds.forEach { assetId ->
                val alreadyCopied = db.query(
                    TABLE_ASSETS,
                    arrayOf(COLUMN_ASSET_ID),
                    "$COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_ID = ?",
                    arrayOf(ledgerId.toString(), assetId.toString()), null, null, null
                ).use { it.moveToFirst() }
                if (!alreadyCopied) copyAssetToLedgerInternal(db, assetId, ledgerId)
            }
            db.delete(TABLE_LEDGER_SHARED_LEDGERS, "$COLUMN_SHARED_LEDGER_ID = ?", arrayOf(ledgerId.toString()))
            val remaining = members.filter { it != ledgerId }
            if (remaining.size == 1) {
                val onlyLedger = remaining.single()
                poolAssetIds.forEach { assetId ->
                    val alreadyCopied = db.query(
                        TABLE_ASSETS,
                        arrayOf(COLUMN_ASSET_ID),
                        "$COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_ID = ?",
                        arrayOf(onlyLedger.toString(), assetId.toString()), null, null, null
                    ).use { it.moveToFirst() }
                    if (!alreadyCopied) copyAssetToLedgerInternal(db, assetId, onlyLedger)
                }
                db.delete(TABLE_LEDGER_SHARED_LEDGERS, "$COLUMN_SHARED_LEDGER_ID = ?", arrayOf(onlyLedger.toString()))
            }
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    fun getLedgerAssetPoolSummary(ledgerId: Long): Pair<Int, Double> {
        val sourceId = getSharedSourceLedgerId(ledgerId) ?: ledgerId
        return readableDatabase.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(CASE WHEN $COLUMN_ASSET_INCLUDE_IN_TOTAL = 1 THEN $COLUMN_ASSET_AMOUNT ELSE 0 END), 0) " +
                "FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0",
            arrayOf(sourceId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) to cursor.getDouble(1) else 0 to 0.0
        }
    }

    fun updateLedgerAssetPool(ledgerId: Long, sharedSourceLedgerId: Long?, iconName: String? = null, name: String? = null): Boolean {
        if (ledgerId != currentLedgerId()) return false
        val db = writableDatabase
        if (iconName != null || name != null) {
            db.update(TABLE_LEDGERS, ContentValues().apply {
                iconName?.let { put(COLUMN_LEDGER_ICON_NAME, it) }
                name?.let {
                    put(COLUMN_LEDGER_NAME, it)
                    put(COLUMN_LEDGER_SUBTITLE, "${it}账本")
                }
            }, "$COLUMN_ID = ?", arrayOf(ledgerId.toString()))
        }
        db.delete(TABLE_LEDGER_SHARED_LEDGERS, "$COLUMN_SHARED_LEDGER_ID = ?", arrayOf(ledgerId.toString()))
        if (sharedSourceLedgerId != null && sharedSourceLedgerId != ledgerId) {
            db.insertOrThrow(TABLE_LEDGER_SHARED_LEDGERS, null, ContentValues().apply {
                put(COLUMN_SHARED_LEDGER_ID, ledgerId)
                put(COLUMN_SHARED_SOURCE_LEDGER_ID, sharedSourceLedgerId)
            })
        }
        return true
    }

    fun addLedger(name: String, subtitle: String = "", sharedAssetIds: List<Long>, copiedAssetIds: List<Long>): Long {
        val id = addLedger(name, subtitle)
        val db = writableDatabase
        sharedAssetIds.distinct().forEach { assetId ->
            db.insertWithOnConflict(TABLE_LEDGER_SHARED_ASSETS, null, ContentValues().apply {
                put(COLUMN_SHARED_LEDGER_ID, id)
                put(COLUMN_SHARED_ASSET_ID, assetId)
            }, SQLiteDatabase.CONFLICT_IGNORE)
        }
        copiedAssetIds.distinct().forEach { assetId -> copyAssetToLedgerInternal(db, assetId, id) }
        return id
    }

    private fun copyAssetToLedgerInternal(db: SQLiteDatabase, assetId: Long, targetLedgerId: Long): Long {
        val values = db.query(TABLE_ASSETS, null, "$COLUMN_ASSET_ID = ?", arrayOf(assetId.toString()), null, null, null).use { cursor ->
            if (!cursor.moveToFirst()) return -1L
            ContentValues().apply {
                put(COLUMN_LEDGER_ID, targetLedgerId)
                put(COLUMN_ASSET_NAME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)))
                put(COLUMN_ASSET_AMOUNT, cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ASSET_AMOUNT)))
                put(COLUMN_ASSET_TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TYPE)))
                put(COLUMN_ASSET_CATEGORY_LABEL, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_CATEGORY_LABEL)))
                put(COLUMN_ASSET_CATEGORY_ICON_NAME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_CATEGORY_ICON_NAME)))
                put(COLUMN_ASSET_IS_ARCHIVED, 0)
                put(COLUMN_ASSET_IS_PINNED, 0)
                put(COLUMN_ASSET_INCLUDE_IN_TOTAL, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_INCLUDE_IN_TOTAL)))
            }
        }
        return db.insertOrThrow(TABLE_ASSETS, null, values)
    }

    fun copyAssetToLedger(assetId: Long, targetLedgerId: Long): Long =
        copyAssetToLedgerInternal(writableDatabase, assetId, targetLedgerId)

    fun updateLedger(ledgerId: Long, name: String, sharedAssetIds: List<Long>): Boolean {
        if (ledgerId != currentLedgerId()) return false
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val updated = db.update(
                TABLE_LEDGERS,
                ContentValues().apply {
                    put(COLUMN_LEDGER_NAME, name)
                    put(COLUMN_LEDGER_SUBTITLE, "${name}账本")
                },
                "$COLUMN_ID = ?",
                arrayOf(ledgerId.toString())
            )
            db.delete(
                TABLE_LEDGER_SHARED_ASSETS,
                "$COLUMN_SHARED_LEDGER_ID = ?",
                arrayOf(ledgerId.toString())
            )
            sharedAssetIds.distinct().forEach { assetId ->
                db.insertWithOnConflict(TABLE_LEDGER_SHARED_ASSETS, null, ContentValues().apply {
                    put(COLUMN_SHARED_LEDGER_ID, ledgerId)
                    put(COLUMN_SHARED_ASSET_ID, assetId)
                }, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
            updated > 0
        } finally {
            db.endTransaction()
        }
    }

    fun getSharedAssetIds(ledgerId: Long): Set<Long> = readableDatabase.rawQuery(
        "SELECT $COLUMN_SHARED_ASSET_ID FROM $TABLE_LEDGER_SHARED_ASSETS WHERE $COLUMN_SHARED_LEDGER_ID = ?",
        arrayOf(ledgerId.toString())
    ).use { cursor ->
        buildSet { while (cursor.moveToNext()) add(cursor.getLong(0)) }
    }

    fun canManageAsset(assetId: Long): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM $TABLE_ASSETS WHERE $COLUMN_ASSET_ID = ? AND $COLUMN_LEDGER_ID = ? LIMIT 1",
        arrayOf(assetId.toString(), currentLedgerId().toString())
    ).use { it.moveToFirst() }

    fun getLedgerRecordCount(ledgerId: Long): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ?", arrayOf(ledgerId.toString())
    ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    /** Permanently folds sourceLedgerId into the active ledger, retaining IDs and history. */
    fun mergeLedgerIntoCurrent(sourceLedgerId: Long): Boolean {
        val targetLedgerId = currentLedgerId()
        if (sourceLedgerId == targetLedgerId || getLedgers().none { it.id == sourceLedgerId }) return false
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val sourceAssetIds = mutableSetOf<Long>()
            db.rawQuery(
                "SELECT $COLUMN_ASSET_ID FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ?",
                arrayOf(sourceLedgerId.toString())
            ).use { cursor -> while (cursor.moveToNext()) sourceAssetIds += cursor.getLong(0) }
            db.rawQuery(
                "SELECT $COLUMN_SHARED_ASSET_ID FROM $TABLE_LEDGER_SHARED_ASSETS WHERE $COLUMN_SHARED_LEDGER_ID = ?",
                arrayOf(sourceLedgerId.toString())
            ).use { cursor -> while (cursor.moveToNext()) sourceAssetIds += cursor.getLong(0) }

            db.update(TABLE_RECORDS, ContentValues().apply { put(COLUMN_LEDGER_ID, targetLedgerId) }, "$COLUMN_LEDGER_ID = ?", arrayOf(sourceLedgerId.toString()))
            db.update(TABLE_ASSETS, ContentValues().apply { put(COLUMN_LEDGER_ID, targetLedgerId) }, "$COLUMN_LEDGER_ID = ?", arrayOf(sourceLedgerId.toString()))
            sourceAssetIds.forEach { assetId ->
                db.insertWithOnConflict(TABLE_LEDGER_SHARED_ASSETS, null, ContentValues().apply {
                    put(COLUMN_SHARED_LEDGER_ID, targetLedgerId)
                    put(COLUMN_SHARED_ASSET_ID, assetId)
                }, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.delete(TABLE_LEDGER_SHARED_ASSETS, "$COLUMN_SHARED_LEDGER_ID = ?", arrayOf(sourceLedgerId.toString()))
            db.delete(TABLE_LEDGERS, "$COLUMN_ID = ?", arrayOf(sourceLedgerId.toString()))
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    /** Deletes ledgers and their records while retaining a shared pool for surviving ledgers. */
    fun deleteLedgers(ledgerIds: Set<Long>): Boolean {
        val allLedgers = getLedgers()
        if (ledgerIds.isEmpty() || ledgerIds.size >= allLedgers.size || !ledgerIds.all { id -> allLedgers.any { it.id == id } }) return false
        fun poolRoot(ledgerId: Long): Long {
            val visited = mutableSetOf<Long>()
            var root = ledgerId
            while (visited.add(root)) root = getSharedSourceLedgerId(root) ?: break
            return root
        }
        val roots = allLedgers.associate { it.id to poolRoot(it.id) }
        val db = writableDatabase
        db.beginTransaction()
        return try {
            ledgerIds.forEach { id ->
                db.delete(TABLE_RECORDS, "$COLUMN_LEDGER_ID = ?", arrayOf(id.toString()))
            }
            roots.values.distinct().filter { root -> roots.any { it.value == root && it.key in ledgerIds } }.forEach { root ->
                val members = roots.filterValues { it == root }.keys
                val survivors = members - ledgerIds
                val keeper = survivors.minOrNull()
                if (keeper == null) {
                    db.delete(TABLE_ASSETS, "$COLUMN_LEDGER_ID = ?", arrayOf(root.toString()))
                } else {
                    members.filter { it in ledgerIds }.forEach { deletedId ->
                        db.update(TABLE_ASSETS, ContentValues().apply { put(COLUMN_LEDGER_ID, keeper) }, "$COLUMN_LEDGER_ID = ?", arrayOf(deletedId.toString()))
                    }
                    db.delete(TABLE_LEDGER_SHARED_LEDGERS, "$COLUMN_SHARED_LEDGER_ID IN (${survivors.joinToString { "?" }})", survivors.map { it.toString() }.toTypedArray())
                    survivors.filter { it != keeper }.forEach { survivor ->
                        db.insertWithOnConflict(TABLE_LEDGER_SHARED_LEDGERS, null, ContentValues().apply {
                            put(COLUMN_SHARED_LEDGER_ID, survivor)
                            put(COLUMN_SHARED_SOURCE_LEDGER_ID, keeper)
                        }, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                }
            }
            db.delete(TABLE_LEDGER_SHARED_LEDGERS, "$COLUMN_SHARED_LEDGER_ID IN (${ledgerIds.joinToString { "?" }})", ledgerIds.map { it.toString() }.toTypedArray())
            db.delete(TABLE_LEDGER_SHARED_LEDGERS, "$COLUMN_SHARED_SOURCE_LEDGER_ID IN (${ledgerIds.joinToString { "?" }})", ledgerIds.map { it.toString() }.toTypedArray())
            ledgerIds.forEach { id -> db.delete(TABLE_ASSETS, "$COLUMN_LEDGER_ID = ?", arrayOf(id.toString())) }
            db.delete(TABLE_LEDGERS, "$COLUMN_ID IN (${ledgerIds.joinToString { "?" }})", ledgerIds.map { it.toString() }.toTypedArray())
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    fun getCurrentLedger(): com.example.cardtally.model.Ledger? = getLedgers().firstOrNull { it.id == currentLedgerId() }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_LEDGERS)
        db.execSQL(CREATE_TABLE_LEDGER_SHARED_ASSETS)
        db.execSQL(CREATE_TABLE_LEDGER_SHARED_LEDGERS)
        db.insertOrThrow(TABLE_LEDGERS, null, ContentValues().apply {
            put(COLUMN_LEDGER_NAME, "日常")
            put(COLUMN_LEDGER_SUBTITLE, "日常账本")
            put(COLUMN_LEDGER_SORT_ORDER, 0)
        })
        db.execSQL(CREATE_TABLE_RECORDS)
        db.execSQL(CREATE_TABLE_CATEGORIES)
        db.execSQL(CREATE_TABLE_ASSETS)
        db.execSQL(CREATE_TABLE_RECORD_DELETION_UNDO)
        db.execSQL(CREATE_TABLE_AI_CHAT_SESSIONS)
        db.execSQL(CREATE_TABLE_AI_CHAT_MESSAGES)
        db.execSQL(CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT)
        db.execSQL(CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT)
        insertDefaultCategories(db, 1L)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v22 starts the clean multi-ledger model. Financial data is intentionally reset;
        // AI conversations remain shared and are kept in their own tables.
        if (oldVersion < 22) {
            resetFinancialData(db)
            return
        }
        if (oldVersion < 23) {
            ensureColumn(db, TABLE_LEDGERS, COLUMN_LEDGER_SHARED_ASSET_IDS,
                "ALTER TABLE $TABLE_LEDGERS ADD COLUMN $COLUMN_LEDGER_SHARED_ASSET_IDS TEXT NOT NULL DEFAULT ''")
            db.execSQL(CREATE_TABLE_LEDGER_SHARED_ASSETS)
        }
        if (oldVersion < 24) {
            resetFinancialData(db)
            return
        }
        if (oldVersion < 25) {
            db.execSQL(CREATE_TABLE_LEDGER_SHARED_LEDGERS)
            // Preserve the best ledger-level interpretation of the old per-asset links.
            db.execSQL(
                "INSERT OR IGNORE INTO $TABLE_LEDGER_SHARED_LEDGERS " +
                    "($COLUMN_SHARED_LEDGER_ID, $COLUMN_SHARED_SOURCE_LEDGER_ID) " +
                    "SELECT old.$COLUMN_SHARED_LEDGER_ID, assets.$COLUMN_LEDGER_ID " +
                    "FROM $TABLE_LEDGER_SHARED_ASSETS old " +
                    "JOIN $TABLE_ASSETS assets ON assets.$COLUMN_ASSET_ID = old.$COLUMN_SHARED_ASSET_ID " +
                    "WHERE old.$COLUMN_SHARED_LEDGER_ID != assets.$COLUMN_LEDGER_ID " +
                    "GROUP BY old.$COLUMN_SHARED_LEDGER_ID, assets.$COLUMN_LEDGER_ID"
            )
        }
        if (oldVersion < 26) {
            ensureColumn(
                db,
                TABLE_LEDGERS,
                COLUMN_LEDGER_ICON_NAME,
                "ALTER TABLE $TABLE_LEDGERS ADD COLUMN $COLUMN_LEDGER_ICON_NAME TEXT NOT NULL DEFAULT 'ms_rounded_book'"
            )
        }
        if (oldVersion < 28) {
            normalizeCategoriesAsGlobal(db)
        }
        if (oldVersion >= 2 && oldVersion < 21) migrateLedgerSchema(db)
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_CATEGORIES)
            insertDefaultCategories(db)
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_DESCRIPTION TEXT")
        }
        if (oldVersion < 4) {
            db.execSQL(CREATE_TABLE_ASSETS)
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_ASSET_SOURCE TEXT")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE $TABLE_CATEGORIES ADD COLUMN $COLUMN_CATEGORY_ICON TEXT")
            updateCategoriesWithIcons(db)
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_SORT_ORDER INTEGER DEFAULT 0")
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_ASSET_IS_ARCHIVED INTEGER DEFAULT 0")
        }
        if (oldVersion < 9) {
            db.execSQL(CREATE_TABLE_AI_CHAT_SESSIONS)
            db.execSQL(CREATE_TABLE_AI_CHAT_MESSAGES)
            db.execSQL(CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT)
            db.execSQL(CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT)
        }
        if (oldVersion < 10) {
            ensureColumn(
                db = db,
                tableName = TABLE_CATEGORIES,
                columnName = COLUMN_CATEGORY_PARENT_ID,
                alterStatement = "ALTER TABLE $TABLE_CATEGORIES ADD COLUMN $COLUMN_CATEGORY_PARENT_ID INTEGER"
            )
            ensureColumn(
                db = db,
                tableName = TABLE_RECORDS,
                columnName = COLUMN_RECORD_CATEGORY_ID,
                alterStatement = "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_RECORD_CATEGORY_ID INTEGER"
            )
            ensureColumn(
                db = db,
                tableName = TABLE_RECORDS,
                columnName = COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT,
                alterStatement = "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT TEXT"
            )
            ensureColumn(
                db = db,
                tableName = TABLE_RECORDS,
                columnName = COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT,
                alterStatement = "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT TEXT"
            )
            backfillLegacyRecordCategorySnapshots(db)
        }
        if (oldVersion < 11) {
            db.execSQL(CREATE_TABLE_RECORD_DELETION_UNDO)
        }
        if (oldVersion < 12) {
            replaceUnusedDefaultExpenseCategories(db)
        }
        if (oldVersion < 13) {
            ensureColumn(
                db = db,
                tableName = TABLE_CATEGORIES,
                columnName = COLUMN_CATEGORY_SORT_ORDER,
                alterStatement = "ALTER TABLE $TABLE_CATEGORIES ADD COLUMN $COLUMN_CATEGORY_SORT_ORDER INTEGER NOT NULL DEFAULT 0"
            )
            initializeCategorySortOrders(db)
        }
        if (oldVersion < 14) {
            replaceDefaultIncomeCategories(db)
        }
        if (oldVersion < 15) {
            ensureColumn(
                db = db,
                tableName = TABLE_ASSETS,
                columnName = COLUMN_ASSET_IS_PINNED,
                alterStatement = "ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_ASSET_IS_PINNED INTEGER DEFAULT 0"
            )
        }
        if (oldVersion < 20) {
            ensureColumn(
                db = db,
                tableName = TABLE_RECORDS,
                columnName = COLUMN_DESTINATION_ASSET_SOURCE,
                alterStatement = "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_DESTINATION_ASSET_SOURCE TEXT"
            )
            ensureColumn(
                db = db,
                tableName = TABLE_RECORD_DELETION_UNDO,
                columnName = COLUMN_DESTINATION_ASSET_SOURCE,
                alterStatement = "ALTER TABLE $TABLE_RECORD_DELETION_UNDO ADD COLUMN $COLUMN_DESTINATION_ASSET_SOURCE TEXT"
            )
        }
        if (oldVersion < 16) {
            ensureColumn(
                db = db,
                tableName = TABLE_RECORDS,
                columnName = COLUMN_PHOTO_URI,
                alterStatement = "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_PHOTO_URI TEXT"
            )
            ensureColumn(
                db = db,
                tableName = TABLE_RECORD_DELETION_UNDO,
                columnName = COLUMN_PHOTO_URI,
                alterStatement = "ALTER TABLE $TABLE_RECORD_DELETION_UNDO ADD COLUMN $COLUMN_PHOTO_URI TEXT"
            )
        }
        if (oldVersion < 17) {
            ensureColumn(
                db = db,
                tableName = TABLE_RECORDS,
                columnName = COLUMN_PHOTO_URIS,
                alterStatement = "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_PHOTO_URIS TEXT"
            )
            ensureColumn(
                db = db,
                tableName = TABLE_RECORD_DELETION_UNDO,
                columnName = COLUMN_PHOTO_URIS,
                alterStatement = "ALTER TABLE $TABLE_RECORD_DELETION_UNDO ADD COLUMN $COLUMN_PHOTO_URIS TEXT"
            )
            db.execSQL(
                "UPDATE $TABLE_RECORDS SET $COLUMN_PHOTO_URIS = $COLUMN_PHOTO_URI " +
                    "WHERE $COLUMN_PHOTO_URI IS NOT NULL AND ($COLUMN_PHOTO_URIS IS NULL OR $COLUMN_PHOTO_URIS = '')"
            )
            db.execSQL(
                "UPDATE $TABLE_RECORD_DELETION_UNDO SET $COLUMN_PHOTO_URIS = $COLUMN_PHOTO_URI " +
                    "WHERE $COLUMN_PHOTO_URI IS NOT NULL AND ($COLUMN_PHOTO_URIS IS NULL OR $COLUMN_PHOTO_URIS = '')"
            )
        }
        if (oldVersion < 18) {
            ensureColumn(
                db = db,
                tableName = TABLE_ASSETS,
                columnName = COLUMN_ASSET_INCLUDE_IN_TOTAL,
                alterStatement = "ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_ASSET_INCLUDE_IN_TOTAL INTEGER DEFAULT 1"
            )
        }
        if (oldVersion < 19) {
            ensureColumn(
                db = db,
                tableName = TABLE_ASSETS,
                columnName = COLUMN_ASSET_CATEGORY_LABEL,
                alterStatement = "ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_ASSET_CATEGORY_LABEL TEXT NOT NULL DEFAULT ''"
            )
            ensureColumn(
                db = db,
                tableName = TABLE_ASSETS,
                columnName = COLUMN_ASSET_CATEGORY_ICON_NAME,
                alterStatement = "ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_ASSET_CATEGORY_ICON_NAME TEXT NOT NULL DEFAULT ''"
            )
        }
        /* ledger schema is migrated before the legacy category migrations above */
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_LEDGERS)
            val defaultLedgerId = db.insertOrThrow(TABLE_LEDGERS, null, ContentValues().apply {
                put(COLUMN_LEDGER_NAME, "日常")
                put(COLUMN_LEDGER_SUBTITLE, "日常账本")
                put(COLUMN_LEDGER_SORT_ORDER, 0)
            })
            ensureColumn(db, TABLE_RECORDS, COLUMN_LEDGER_ID,
                "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1")
            ensureColumn(db, TABLE_CATEGORIES, COLUMN_LEDGER_ID,
                "ALTER TABLE $TABLE_CATEGORIES ADD COLUMN $COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1")
            ensureColumn(db, TABLE_ASSETS, COLUMN_LEDGER_ID,
                "ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1")
            ensureColumn(db, TABLE_RECORD_DELETION_UNDO, COLUMN_LEDGER_ID,
                "ALTER TABLE $TABLE_RECORD_DELETION_UNDO ADD COLUMN $COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1")
            db.execSQL("UPDATE $TABLE_RECORDS SET $COLUMN_LEDGER_ID = ?", arrayOf(defaultLedgerId))
            db.execSQL("UPDATE $TABLE_CATEGORIES SET $COLUMN_LEDGER_ID = ?", arrayOf(defaultLedgerId))
            db.execSQL("UPDATE $TABLE_ASSETS SET $COLUMN_LEDGER_ID = ?", arrayOf(defaultLedgerId))
            db.execSQL("UPDATE $TABLE_RECORD_DELETION_UNDO SET $COLUMN_LEDGER_ID = ?", arrayOf(defaultLedgerId))
        }
    }

    private fun resetFinancialData(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORD_DELETION_UNDO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ASSETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGER_SHARED_ASSETS")
        db.execSQL(CREATE_TABLE_LEDGERS)
        db.execSQL(CREATE_TABLE_LEDGER_SHARED_ASSETS)
        db.insertOrThrow(TABLE_LEDGERS, null, ContentValues().apply {
            put(COLUMN_LEDGER_NAME, "日常")
            put(COLUMN_LEDGER_SUBTITLE, "日常账本")
            put(COLUMN_LEDGER_SORT_ORDER, 0)
        })
        db.execSQL(CREATE_TABLE_RECORDS)
        db.execSQL(CREATE_TABLE_CATEGORIES)
        db.execSQL(CREATE_TABLE_ASSETS)
        db.execSQL(CREATE_TABLE_RECORD_DELETION_UNDO)
        insertDefaultCategories(db, 1L)
        LedgerSession.setCurrentId(appContext, 1L)
    }

    /** Collapses legacy per-ledger category copies into one global category tree. */
    private fun normalizeCategoriesAsGlobal(db: SQLiteDatabase) {
        data class LegacyCategory(val id: Long, val name: String, val type: Int, val parentId: Long?)
        val rows = mutableListOf<LegacyCategory>()
        db.rawQuery(
            "SELECT $COLUMN_CATEGORY_ID, $COLUMN_CATEGORY_NAME, $COLUMN_CATEGORY_TYPE, $COLUMN_CATEGORY_PARENT_ID " +
                "FROM $TABLE_CATEGORIES ORDER BY $COLUMN_CATEGORY_ID",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows += LegacyCategory(
                    cursor.getLong(0), cursor.getString(1), cursor.getInt(2), getNullableLong(cursor, COLUMN_CATEGORY_PARENT_ID)
                )
            }
        }
        val byId = rows.associateBy { it.id }
        val canonicalById = mutableMapOf<Long, Long>()
        val canonicalByKey = mutableMapOf<String, Long>()
        val resolving = mutableSetOf<Long>()

        fun resolve(id: Long): Long {
            canonicalById[id]?.let { return it }
            val row = byId[id] ?: return id
            if (!resolving.add(id)) return id
            val parent = row.parentId?.let { resolve(it) }
            resolving.remove(id)
            if (row.parentId != parent) {
                db.update(
                    TABLE_CATEGORIES,
                    ContentValues().apply {
                        if (parent == null) putNull(COLUMN_CATEGORY_PARENT_ID)
                        else put(COLUMN_CATEGORY_PARENT_ID, parent)
                    },
                    "$COLUMN_CATEGORY_ID = ?",
                    arrayOf(id.toString())
                )
            }
            val key = "${row.type}|${parent ?: 0L}|${row.name}"
            val canonicalId = canonicalByKey[key]
            if (canonicalId == null) {
                canonicalByKey[key] = id
                canonicalById[id] = id
            } else {
                canonicalById[id] = canonicalId
                db.update(
                    TABLE_RECORDS,
                    ContentValues().apply { put(COLUMN_RECORD_CATEGORY_ID, canonicalId) },
                    "$COLUMN_RECORD_CATEGORY_ID = ?",
                    arrayOf(id.toString())
                )
                db.update(
                    TABLE_RECORD_DELETION_UNDO,
                    ContentValues().apply { put(COLUMN_RECORD_CATEGORY_ID, canonicalId) },
                    "$COLUMN_RECORD_CATEGORY_ID = ?",
                    arrayOf(id.toString())
                )
                db.update(
                    TABLE_CATEGORIES,
                    ContentValues().apply { put(COLUMN_CATEGORY_PARENT_ID, canonicalId) },
                    "$COLUMN_CATEGORY_PARENT_ID = ?",
                    arrayOf(id.toString())
                )
                db.delete(TABLE_CATEGORIES, "$COLUMN_CATEGORY_ID = ?", arrayOf(id.toString()))
            }
            return canonicalId ?: id
        }

        rows.forEach { resolve(it.id) }
        db.update(
            TABLE_CATEGORIES,
            ContentValues().apply { put(COLUMN_LEDGER_ID, 1L) },
            null,
            null
        )
    }

    private fun migrateLedgerSchema(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_LEDGERS)
        val defaultLedgerId = db.insertOrThrow(TABLE_LEDGERS, null, ContentValues().apply {
            put(COLUMN_LEDGER_NAME, "日常")
            put(COLUMN_LEDGER_SUBTITLE, "日常账本")
            put(COLUMN_LEDGER_SORT_ORDER, 0)
        })
        ensureColumn(db, TABLE_RECORDS, COLUMN_LEDGER_ID,
            "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1")
        ensureColumn(db, TABLE_CATEGORIES, COLUMN_LEDGER_ID,
            "ALTER TABLE $TABLE_CATEGORIES ADD COLUMN $COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1")
        ensureColumn(db, TABLE_ASSETS, COLUMN_LEDGER_ID,
            "ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1")
        ensureColumn(db, TABLE_RECORD_DELETION_UNDO, COLUMN_LEDGER_ID,
            "ALTER TABLE $TABLE_RECORD_DELETION_UNDO ADD COLUMN $COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE $TABLE_RECORDS SET $COLUMN_LEDGER_ID = ?", arrayOf(defaultLedgerId))
        db.execSQL("UPDATE $TABLE_CATEGORIES SET $COLUMN_LEDGER_ID = ?", arrayOf(defaultLedgerId))
        db.execSQL("UPDATE $TABLE_ASSETS SET $COLUMN_LEDGER_ID = ?", arrayOf(defaultLedgerId))
        db.execSQL("UPDATE $TABLE_RECORD_DELETION_UNDO SET $COLUMN_LEDGER_ID = ?", arrayOf(defaultLedgerId))
    }

    private fun insertDefaultCategories(db: SQLiteDatabase, ledgerId: Long = 1L) {
        val expenseParents = listOf(
            "购物" to "ic_category_shopping",
            "餐饮" to "ic_category_food",
            "居住" to "ic_category_housing",
            "交通" to "ic_category_transport"
        )
        val parentIds = mutableMapOf<String, Long>()
        for ((index, categoryAndIcon) in expenseParents.withIndex()) {
            val (category, icon) = categoryAndIcon
            val values = ContentValues().apply {
                put(COLUMN_CATEGORY_NAME, category)
                put(COLUMN_CATEGORY_TYPE, 0)
                put(COLUMN_CATEGORY_ICON, icon)
                put(COLUMN_CATEGORY_SORT_ORDER, index)
                put(COLUMN_LEDGER_ID, 1L)
            }
            parentIds[category] = db.insertOrThrow(TABLE_CATEGORIES, null, values)
        }

        val expenseChildren = mapOf(
            "购物" to listOf("服饰" to "ms_rounded_checkroom", "家电" to "ms_rounded_devices", "数码" to "ms_rounded_devices"),
            "餐饮" to listOf("早午晚餐" to "ms_rounded_lunch_dining"),
            "居住" to listOf("房租" to "ms_rounded_home", "酒店" to "ms_rounded_hotel"),
            "交通" to listOf("短途" to "ms_rounded_directions_car", "飞机高铁" to "ms_rounded_flight")
        )
        for ((parentName, children) in expenseChildren) {
            val parentId = parentIds[parentName] ?: continue
            for ((category, icon) in children) {
                val values = ContentValues().apply {
                    put(COLUMN_CATEGORY_NAME, category)
                    put(COLUMN_CATEGORY_TYPE, 0)
                    put(COLUMN_CATEGORY_ICON, icon)
                    put(COLUMN_CATEGORY_PARENT_ID, parentId)
                }
                values.put(COLUMN_LEDGER_ID, 1L)
                db.insertOrThrow(TABLE_CATEGORIES, null, values)
            }
        }

        insertDefaultIncomeCategories(db, ledgerId)
    }

    private fun updateCategoriesWithIcons(db: SQLiteDatabase) {
        val categoryIcons = mapOf(
            "餐饮" to "ic_category_food",
            "交通" to "ic_category_transport",
            "购物" to "ic_category_shopping",
            "娱乐" to "ic_category_entertainment",
            "医疗" to "ic_category_medical",
            "教育" to "ic_category_education",
            "住房" to "ic_category_housing",
            "工资" to "ic_category_salary",
            "奖金" to "ic_category_bonus",
            "其他" to "ic_category_other"
        )
        
        for ((category, icon) in categoryIcons) {
            val values = ContentValues().apply {
                put(COLUMN_CATEGORY_ICON, icon)
            }
            db.update(TABLE_CATEGORIES, values, "$COLUMN_CATEGORY_NAME = ?", arrayOf(category))
        }
    }

    private fun ensureColumn(
        db: SQLiteDatabase,
        tableName: String,
        columnName: String,
        alterStatement: String
    ) {
        if (!tableHasColumn(db, tableName, columnName)) {
            db.execSQL(alterStatement)
        }
    }

    private fun tableHasColumn(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
        cursor.use {
            while (it.moveToNext()) {
                if (it.getString(it.getColumnIndexOrThrow("name")) == columnName) {
                    return true
                }
            }
        }
        return false
    }

    private fun backfillLegacyRecordCategorySnapshots(db: SQLiteDatabase) {
        db.execSQL(
            "UPDATE $TABLE_RECORDS " +
                "SET $COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT = $COLUMN_CATEGORY " +
                "WHERE $COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT IS NULL OR TRIM($COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT) = ''"
        )
        db.execSQL(
            "UPDATE $TABLE_RECORDS " +
                "SET $COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT = $COLUMN_CATEGORY " +
                "WHERE $COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT IS NULL OR TRIM($COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT) = ''"
        )
        db.execSQL(
            "UPDATE $TABLE_RECORDS " +
                "SET $COLUMN_RECORD_CATEGORY_ID = (" +
                "SELECT c.$COLUMN_CATEGORY_ID FROM $TABLE_CATEGORIES c " +
                "WHERE c.$COLUMN_CATEGORY_TYPE = $TABLE_RECORDS.$COLUMN_TYPE " +
                "AND c.$COLUMN_CATEGORY_NAME = $TABLE_RECORDS.$COLUMN_CATEGORY" +
                ") " +
                "WHERE (" +
                "SELECT COUNT(*) FROM $TABLE_CATEGORIES c " +
                "WHERE c.$COLUMN_CATEGORY_TYPE = $TABLE_RECORDS.$COLUMN_TYPE " +
                "AND c.$COLUMN_CATEGORY_NAME = $TABLE_RECORDS.$COLUMN_CATEGORY" +
                ") = 1"
        )
    }

    private fun createRecordValues(record: Record): ContentValues {
        val categoryNameSnapshot = record.categoryNameSnapshot ?: record.category
        val categoryPathSnapshot = record.categoryPathSnapshot ?: record.category

        return ContentValues().apply {
            put(COLUMN_LEDGER_ID, currentLedgerId())
            put(COLUMN_DATE, record.date)
            put(COLUMN_AMOUNT, record.amount)
            put(COLUMN_CATEGORY, record.category)
            if (record.categoryId != null) {
                put(COLUMN_RECORD_CATEGORY_ID, record.categoryId)
            } else {
                putNull(COLUMN_RECORD_CATEGORY_ID)
            }
            put(COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT, categoryNameSnapshot)
            put(COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT, categoryPathSnapshot)
            put(COLUMN_TYPE, record.type)
            put(COLUMN_DESCRIPTION, record.description)
            putNullable(COLUMN_RECORD_ASSET_ID, record.assetId)
            putNullable(COLUMN_RECORD_DESTINATION_ASSET_ID, record.destinationAssetId)
            put(COLUMN_ASSET_SOURCE, record.assetSource)
            put(COLUMN_DESTINATION_ASSET_SOURCE, record.destinationAssetSource)
            put(COLUMN_PHOTO_URI, record.photoUri)
            put(COLUMN_PHOTO_URIS, record.photoUris.joinToString(PHOTO_URI_SEPARATOR))
        }
    }

    private fun createRecordFromCursor(cursor: Cursor): Record {
        return Record(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
            amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
            categoryId = getNullableLong(cursor, COLUMN_RECORD_CATEGORY_ID),
            categoryNameSnapshot = getNullableString(cursor, COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT),
            categoryPathSnapshot = getNullableString(cursor, COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT),
            type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
            description = getNullableString(cursor, COLUMN_DESCRIPTION),
            assetId = getNullableLong(cursor, COLUMN_RECORD_ASSET_ID),
            destinationAssetId = getNullableLong(cursor, COLUMN_RECORD_DESTINATION_ASSET_ID),
            assetSource = getNullableString(cursor, COLUMN_ASSET_SOURCE),
            destinationAssetSource = getNullableString(cursor, COLUMN_DESTINATION_ASSET_SOURCE),
            photoUri = getNullableString(cursor, COLUMN_PHOTO_URI),
            photoUris = getNullableString(cursor, COLUMN_PHOTO_URIS)
                ?.split(PHOTO_URI_SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?.ifEmpty { getNullableString(cursor, COLUMN_PHOTO_URI)?.let(::listOf) ?: emptyList() }
                ?: getNullableString(cursor, COLUMN_PHOTO_URI)?.let(::listOf)
                ?: emptyList(),
            sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SORT_ORDER))
        )
    }

    private fun createCategoryFromCursor(cursor: Cursor): Category {
        return Category(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME)),
            type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE)),
            icon = getNullableString(cursor, COLUMN_CATEGORY_ICON),
            parentId = getNullableLong(cursor, COLUMN_CATEGORY_PARENT_ID),
            sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_SORT_ORDER))
        )
    }

    private fun getNullableString(cursor: Cursor, columnName: String): String? {
        val columnIndex = cursor.getColumnIndex(columnName)
        if (columnIndex == -1 || cursor.isNull(columnIndex)) {
            return null
        }
        return cursor.getString(columnIndex)
    }

    private fun getNullableLong(cursor: Cursor, columnName: String): Long? {
        val columnIndex = cursor.getColumnIndex(columnName)
        if (columnIndex == -1 || cursor.isNull(columnIndex)) {
            return null
        }
        return cursor.getLong(columnIndex)
    }

    private fun getCategoryByIdInternal(db: SQLiteDatabase, id: Long): Category? {
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CATEGORIES WHERE $COLUMN_CATEGORY_ID = ?",
            arrayOf(id.toString())
        )

        cursor.use {
            if (!it.moveToFirst()) {
                return null
            }
            return createCategoryFromCursor(it)
        }
    }

    private fun getCategoriesByTypeInternal(db: SQLiteDatabase, type: Int): List<Category> {
        val categories = mutableListOf<Category>()
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CATEGORIES WHERE $COLUMN_CATEGORY_TYPE = ? " +
                "ORDER BY CASE WHEN $COLUMN_CATEGORY_PARENT_ID IS NULL THEN 0 ELSE 1 END, " +
                "$COLUMN_CATEGORY_PARENT_ID, $COLUMN_CATEGORY_SORT_ORDER ASC, " +
                "$COLUMN_CATEGORY_NAME COLLATE NOCASE ASC, $COLUMN_CATEGORY_ID ASC",
            arrayOf(type.toString())
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    categories.add(createCategoryFromCursor(it))
                } while (it.moveToNext())
            }
        }

        return categories
    }

    private fun validateParentAssignment(db: SQLiteDatabase, category: Category) {
        val parentId = category.parentId ?: return
        val categoryId = category.id.takeIf { it > 0L }
        val parent = getCategoryByIdInternal(db, parentId)
            ?: throw CategoryOperationException(CategoryOperationError.PARENT_NOT_FOUND)

        if (parent.type != category.type) {
            throw CategoryOperationException(CategoryOperationError.PARENT_TYPE_MISMATCH)
        }
        if (categoryId != null && parentId == categoryId) {
            throw CategoryOperationException(CategoryOperationError.SELF_PARENT)
        }
        if (categoryId != null && isDescendantCategory(db, categoryId, parentId)) {
            throw CategoryOperationException(CategoryOperationError.DESCENDANT_CYCLE)
        }

        val assignedDepth = resolveCategoryDepth(db, parentId, mutableSetOf()) + 1
        val maxDepth = CategoryHierarchySettingsHelper.getCategoryMaxDepth(appContext)
        if (assignedDepth > maxDepth) {
            throw CategoryOperationException(CategoryOperationError.MAX_DEPTH_EXCEEDED)
        }
    }

    private fun isDescendantCategory(db: SQLiteDatabase, categoryId: Long, candidateParentId: Long): Boolean {
        var currentParentId: Long? = candidateParentId
        val visited = mutableSetOf<Long>()

        while (currentParentId != null) {
            if (!visited.add(currentParentId)) {
                break
            }
            if (currentParentId == categoryId) {
                return true
            }
            currentParentId = getCategoryByIdInternal(db, currentParentId)?.parentId
        }

        return false
    }

    private fun resolveCategoryDepth(
        db: SQLiteDatabase,
        categoryId: Long,
        visiting: MutableSet<Long>
    ): Int {
        if (!visiting.add(categoryId)) {
            return 1
        }

        val category = getCategoryByIdInternal(db, categoryId) ?: return 1
        val depth = category.parentId?.let { resolveCategoryDepth(db, it, visiting) + 1 } ?: 1
        visiting.remove(categoryId)
        return depth
    }

    private fun categoryHasChildren(db: SQLiteDatabase, id: Long): Boolean {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_CATEGORIES WHERE $COLUMN_CATEGORY_PARENT_ID = ?",
            arrayOf(id.toString())
        )

        cursor.use {
            return it.moveToFirst() && it.getInt(0) > 0
        }
    }

    private fun categoryIsReferencedByRecordId(db: SQLiteDatabase, id: Long): Boolean {
        val cursor = db.rawQuery(
            "SELECT 1 FROM (" +
                "SELECT $COLUMN_RECORD_CATEGORY_ID FROM $TABLE_RECORDS " +
                "WHERE $COLUMN_RECORD_CATEGORY_ID = ? " +
                "UNION ALL " +
                "SELECT $COLUMN_RECORD_CATEGORY_ID FROM $TABLE_RECORD_DELETION_UNDO " +
                "WHERE $COLUMN_RECORD_CATEGORY_ID = ? AND $COLUMN_UNDO_EXPIRES_AT > ?" +
                ") LIMIT 1",
            arrayOf(id.toString(), id.toString(), System.currentTimeMillis().toString())
        )

        cursor.use {
            return it.moveToFirst()
        }
    }

    fun addRecord(record: Record): Long {
        val db = writableDatabase
        
        val maxSortOrderQuery = "SELECT MAX($COLUMN_SORT_ORDER) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_DATE = ?"
        val cursor = db.rawQuery(maxSortOrderQuery, arrayOf(currentLedgerId().toString(), record.date))
        var maxSortOrder = 0
        if (cursor.moveToFirst()) {
            maxSortOrder = cursor.getInt(0)
        }
        cursor.close()
        
        val values = createRecordValues(record).apply {
            put(COLUMN_SORT_ORDER, maxSortOrder + 1)
        }

        val id = db.insert(TABLE_RECORDS, null, values)
        
        if (id != -1L) {
            applyRecordAssetEffect(db, record, reverse = false)
        }

        return id
    }

    fun getAllRecords(): List<Record> {
        val records = mutableListOf<Record>()
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ${currentLedgerId()} ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                records.add(createRecordFromCursor(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return records
    }

    fun updateRecord(record: Record): Int {
        val db = writableDatabase
        
        val oldRecord = getRecordByIdInternal(db, record.id)
        
        val values = createRecordValues(record)

        val rowsAffected = db.update(TABLE_RECORDS, values, "$COLUMN_ID = ? AND $COLUMN_LEDGER_ID = ?",
            arrayOf(record.id.toString(), currentLedgerId().toString()))
        
        if (rowsAffected > 0 && oldRecord != null) {
            applyRecordAssetEffect(db, oldRecord, reverse = true)
            applyRecordAssetEffect(db, record, reverse = false)
        }

        return rowsAffected
    }

    fun getRecordById(id: Long): Record? {
        val db = readableDatabase
        val record = getRecordByIdInternal(db, id)
        return record
    }

    private fun getRecordByIdInternal(db: SQLiteDatabase, id: Long): Record? {
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_ID = ? AND $COLUMN_LEDGER_ID = ${currentLedgerId()}"
        
        val cursor = db.rawQuery(selectQuery, arrayOf(id.toString()))
        
        var record: Record? = null
        if (cursor.moveToFirst()) {
            record = createRecordFromCursor(cursor)
        }
        
        cursor.close()
        return record
    }

    fun deleteRecord(
        id: Long,
        deletedAtEpochMs: Long = System.currentTimeMillis()
    ): RecordDeletionToken? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                TABLE_RECORD_DELETION_UNDO,
                "$COLUMN_UNDO_EXPIRES_AT <= ?",
                arrayOf(deletedAtEpochMs.toString())
            )
            val record = getRecordByIdInternal(db, id)
            if (record == null) {
                db.setTransactionSuccessful()
                return null
            }

            if (record.type == 2) {
                applyRecordAssetEffect(db, record, reverse = true)
                check(db.delete(TABLE_RECORDS, "$COLUMN_ID = ?", arrayOf(id.toString())) == 1)
                db.setTransactionSuccessful()
                return null
            }

            val requestedBalanceDelta = if (record.type == 1) -record.amount else record.amount
            val appliedBalanceDelta = if (
                record.assetId == null ||
                !db.adjustAssetBalance(record.assetId!!, record.assetSource, requestedBalanceDelta, activeOnly = true)
            ) {
                0.0
            } else {
                requestedBalanceDelta
            }
            val token = RecordDeletionToken(
                value = UUID.randomUUID().toString(),
                expiresAtEpochMs = deletedAtEpochMs + RECORD_UNDO_WINDOW_MS
            )
            val undoValues = createRecordDeletionUndoValues(record, token, appliedBalanceDelta)
            check(db.insertOrThrow(TABLE_RECORD_DELETION_UNDO, null, undoValues) != -1L)
            check(db.delete(TABLE_RECORDS, "$COLUMN_ID = ?", arrayOf(id.toString())) == 1)
            (record.photoUris.ifEmpty { record.photoUri?.let(::listOf) ?: emptyList() }).forEach { photoUri ->
                runCatching {
                    appContext.contentResolver.delete(android.net.Uri.parse(photoUri), null, null)
                }
            }

            db.setTransactionSuccessful()
            return token
        } finally {
            db.endTransaction()
        }
    }

    private fun replaceUnusedDefaultExpenseCategories(db: SQLiteDatabase) {
        val hasRecords = db.rawQuery("SELECT 1 FROM $TABLE_RECORDS LIMIT 1", null).use { it.moveToFirst() }
        if (hasRecords) return

        db.delete(TABLE_CATEGORIES, "$COLUMN_CATEGORY_TYPE = ?", arrayOf("0"))
        insertDefaultExpenseCategories(db)
    }

    private fun insertDefaultExpenseCategories(db: SQLiteDatabase, ledgerId: Long = 1L) {
        val values = ContentValues()
        val parents = listOf(
            "购物" to "ic_category_shopping",
            "餐饮" to "ic_category_food",
            "居住" to "ic_category_housing",
            "交通" to "ic_category_transport"
        )
        val parentIds = mutableMapOf<String, Long>()
        parents.forEachIndexed { index, (name, icon) ->
            values.clear()
            values.put(COLUMN_CATEGORY_NAME, name)
            values.put(COLUMN_CATEGORY_TYPE, 0)
            values.put(COLUMN_CATEGORY_ICON, icon)
            values.put(COLUMN_CATEGORY_SORT_ORDER, index)
            values.put(COLUMN_LEDGER_ID, 1L)
            parentIds[name] = db.insertOrThrow(TABLE_CATEGORIES, null, values)
        }
        val children = mapOf(
            "购物" to listOf("服饰" to "ms_rounded_checkroom", "家电" to "ms_rounded_devices", "数码" to "ms_rounded_devices"),
            "餐饮" to listOf("早午晚餐" to "ms_rounded_lunch_dining"),
            "居住" to listOf("房租" to "ms_rounded_home", "酒店" to "ms_rounded_hotel"),
            "交通" to listOf("短途" to "ms_rounded_directions_car", "飞机高铁" to "ms_rounded_flight")
        )
        children.forEach { (parent, items) ->
            items.forEach { (name, icon) ->
                values.clear()
                values.put(COLUMN_CATEGORY_NAME, name)
                values.put(COLUMN_CATEGORY_TYPE, 0)
                values.put(COLUMN_CATEGORY_ICON, icon)
                values.put(COLUMN_CATEGORY_PARENT_ID, parentIds[parent])
                values.put(COLUMN_LEDGER_ID, 1L)
                db.insertOrThrow(TABLE_CATEGORIES, null, values)
            }
        }
    }

    private fun insertDefaultIncomeCategories(db: SQLiteDatabase, ledgerId: Long = 1L) {
        val parents = listOf(
            "工作" to "ms_rounded_work",
            "理财" to "ms_rounded_account_balance"
        )
        val parentIds = mutableMapOf<String, Long>()
        parents.forEachIndexed { index, (name, icon) ->
            val parentId = findCategoryId(db, name, 1, null, ledgerId) ?: db.insertOrThrow(
                TABLE_CATEGORIES,
                null,
                ContentValues().apply {
                    put(COLUMN_CATEGORY_NAME, name)
                    put(COLUMN_CATEGORY_TYPE, 1)
                    put(COLUMN_CATEGORY_ICON, icon)
                    put(COLUMN_CATEGORY_SORT_ORDER, index)
                    put(COLUMN_LEDGER_ID, 1L)
                }
            )
            parentIds[name] = parentId
        }

        val children = mapOf(
            "工作" to listOf("工资" to "ic_category_salary", "报销" to "ms_rounded_receipt_long"),
            "理财" to listOf(
                "股票" to "ms_rounded_show_chart",
                "基金" to "ms_rounded_pie_chart",
                "黄金" to "ms_rounded_savings"
            )
        )
        children.forEach { (parentName, items) ->
            items.forEachIndexed { index, (name, icon) ->
                val parentId = parentIds[parentName] ?: return@forEachIndexed
                if (findCategoryId(db, name, 1, parentId, ledgerId) == null) {
                    db.insertOrThrow(
                        TABLE_CATEGORIES,
                        null,
                        ContentValues().apply {
                            put(COLUMN_CATEGORY_NAME, name)
                            put(COLUMN_CATEGORY_TYPE, 1)
                            put(COLUMN_CATEGORY_ICON, icon)
                            put(COLUMN_CATEGORY_PARENT_ID, parentId)
                            put(COLUMN_CATEGORY_SORT_ORDER, index)
                            put(COLUMN_LEDGER_ID, 1L)
                        }
                    )
                }
            }
        }
    }

    private fun replaceDefaultIncomeCategories(db: SQLiteDatabase) {
        val hasIncomeRecords = db.rawQuery(
            "SELECT 1 FROM $TABLE_RECORDS WHERE $COLUMN_TYPE = 1 LIMIT 1",
            null
        ).use { it.moveToFirst() }

        if (!hasIncomeRecords) {
            db.delete(TABLE_CATEGORIES, "$COLUMN_CATEGORY_TYPE = ?", arrayOf("1"))
            insertDefaultIncomeCategories(db)
            return
        }

        val oldDefaultNames = listOf("工资", "奖金", "投资", "兼职", "其他")
        oldDefaultNames.forEach { name ->
            val cursor = db.rawQuery(
                "SELECT $COLUMN_CATEGORY_ID FROM $TABLE_CATEGORIES " +
                    "WHERE $COLUMN_CATEGORY_TYPE = 1 AND $COLUMN_CATEGORY_NAME = ?",
                arrayOf(name)
            )
            cursor.use {
                if (it.moveToFirst()) {
                    do {
                        val id = it.getLong(0)
                        if (!categoryHasChildren(db, id) && !categoryIsReferencedByRecordId(db, id)) {
                            db.delete(TABLE_CATEGORIES, "$COLUMN_CATEGORY_ID = ?", arrayOf(id.toString()))
                        }
                    } while (it.moveToNext())
                }
            }
        }
        insertDefaultIncomeCategories(db)
    }

    private fun findCategoryId(
        db: SQLiteDatabase,
        name: String,
        type: Int,
        parentId: Long?,
        ledgerId: Long = 1L
    ): Long? {
        val selection = if (parentId == null) {
            "$COLUMN_CATEGORY_NAME = ? AND $COLUMN_CATEGORY_TYPE = ? AND $COLUMN_CATEGORY_PARENT_ID IS NULL"
        } else {
            "$COLUMN_CATEGORY_NAME = ? AND $COLUMN_CATEGORY_TYPE = ? AND $COLUMN_CATEGORY_PARENT_ID = ?"
        }
        val args = if (parentId == null) {
            arrayOf(name, type.toString())
        } else {
            arrayOf(name, type.toString(), parentId.toString())
        }
        return db.query(
            TABLE_CATEGORIES,
            arrayOf(COLUMN_CATEGORY_ID),
            selection,
            args,
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    fun undoRecordDeletion(
        token: RecordDeletionToken,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_RECORD_DELETION_UNDO WHERE $COLUMN_UNDO_TOKEN = ?",
                arrayOf(token.value)
            )
            val undoEntry = cursor.use {
                if (!it.moveToFirst()) {
                    null
                } else {
                    RecordDeletionUndoEntry(
                        record = createRecordFromCursor(it),
                        expiresAtEpochMs = it.getLong(it.getColumnIndexOrThrow(COLUMN_UNDO_EXPIRES_AT)),
                        balanceDelta = it.getDouble(it.getColumnIndexOrThrow(COLUMN_UNDO_BALANCE_DELTA))
                    )
                }
            }
            if (undoEntry == null) {
                db.setTransactionSuccessful()
                return false
            }
            if (nowEpochMs >= undoEntry.expiresAtEpochMs) {
                db.delete(TABLE_RECORD_DELETION_UNDO, "$COLUMN_UNDO_TOKEN = ?", arrayOf(token.value))
                db.setTransactionSuccessful()
                return false
            }

            val recordValues = createRecordValues(undoEntry.record).apply {
                put(COLUMN_ID, undoEntry.record.id)
                put(COLUMN_SORT_ORDER, undoEntry.record.sortOrder)
                putNullable(COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT, undoEntry.record.categoryNameSnapshot)
                putNullable(COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT, undoEntry.record.categoryPathSnapshot)
            }
            db.insertOrThrow(TABLE_RECORDS, null, recordValues)
            if (undoEntry.record.assetId != null && undoEntry.balanceDelta != 0.0) {
                check(db.adjustAssetBalance(undoEntry.record.assetId!!, undoEntry.record.assetSource, -undoEntry.balanceDelta, activeOnly = false))
            }
            check(
                db.delete(
                    TABLE_RECORD_DELETION_UNDO,
                    "$COLUMN_UNDO_TOKEN = ?",
                    arrayOf(token.value)
                ) == 1
            )

            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    private data class RecordDeletionUndoEntry(
        val record: Record,
        val expiresAtEpochMs: Long,
        val balanceDelta: Double
    )

    private fun createRecordDeletionUndoValues(
        record: Record,
        token: RecordDeletionToken,
        balanceDelta: Double
    ): ContentValues {
        return ContentValues().apply {
            put(COLUMN_LEDGER_ID, currentLedgerId())
            put(COLUMN_UNDO_TOKEN, token.value)
            put(COLUMN_UNDO_EXPIRES_AT, token.expiresAtEpochMs)
            put(COLUMN_UNDO_BALANCE_DELTA, balanceDelta)
            put(COLUMN_ID, record.id)
            put(COLUMN_DATE, record.date)
            put(COLUMN_AMOUNT, record.amount)
            put(COLUMN_CATEGORY, record.category)
            putNullable(COLUMN_RECORD_CATEGORY_ID, record.categoryId)
            putNullable(COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT, record.categoryNameSnapshot)
            putNullable(COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT, record.categoryPathSnapshot)
            put(COLUMN_TYPE, record.type)
            putNullable(COLUMN_DESCRIPTION, record.description)
            putNullable(COLUMN_RECORD_ASSET_ID, record.assetId)
            putNullable(COLUMN_RECORD_DESTINATION_ASSET_ID, record.destinationAssetId)
            putNullable(COLUMN_ASSET_SOURCE, record.assetSource)
            putNullable(COLUMN_PHOTO_URI, record.photoUri)
            putNullable(COLUMN_PHOTO_URIS, record.photoUris.joinToString(PHOTO_URI_SEPARATOR))
            put(COLUMN_SORT_ORDER, record.sortOrder)
        }
    }

    private fun ContentValues.putNullable(columnName: String, value: String?) {
        if (value == null) putNull(columnName) else put(columnName, value)
    }

    private fun ContentValues.putNullable(columnName: String, value: Long?) {
        if (value == null) putNull(columnName) else put(columnName, value)
    }

    private fun SQLiteDatabase.adjustAssetBalance(
        assetId: Long,
        assetName: String?,
        delta: Double,
        activeOnly: Boolean
    ): Boolean {
        val archiveClause = if (activeOnly) " AND $COLUMN_ASSET_IS_ARCHIVED = 0" else ""
        val identityClause = if (assetId > 0) "$COLUMN_ASSET_ID = ?" else "$COLUMN_ASSET_NAME = ?"
        val identityValue = if (assetId > 0) assetId.toString() else assetName.orEmpty()
        val cursor = rawQuery(
            "SELECT 1 FROM $TABLE_ASSETS WHERE ${assetScope()} AND $identityClause$archiveClause LIMIT 1",
            arrayOf(identityValue)
        )
        val assetExists = cursor.use { it.moveToFirst() }
        if (!assetExists) {
            return false
        }
        execSQL(
            "UPDATE $TABLE_ASSETS SET $COLUMN_ASSET_AMOUNT = $COLUMN_ASSET_AMOUNT + ? " +
                "WHERE ${assetScope()} AND $identityClause$archiveClause",
            arrayOf(delta.toString(), identityValue)
        )
        return true
    }

    private fun applyRecordAssetEffect(db: SQLiteDatabase, record: Record, reverse: Boolean) {
        val direction = if (reverse) -1 else 1
        when (record.type) {
            0 -> record.assetId?.let { updateAssetAmount(db, it, record.amount * direction, false) }
            1 -> record.assetId?.let { updateAssetAmount(db, it, record.amount * direction, true) }
            2 -> {
                record.assetId?.let { updateAssetAmount(db, it, record.amount * direction, false) }
                record.destinationAssetId?.let { updateAssetAmount(db, it, record.amount * direction, true) }
            }
        }
    }

    private fun updateAssetAmount(db: SQLiteDatabase, assetId: Long, amount: Double, isAdd: Boolean) {
        val selectQuery = "SELECT $COLUMN_ASSET_AMOUNT FROM $TABLE_ASSETS WHERE ${assetScope()} AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0 LIMIT 1"
        val cursor = db.rawQuery(selectQuery, arrayOf(assetId.toString()))
        
        if (cursor.moveToFirst()) {
            val currentAmount = cursor.getDouble(0)
            val newAmount = if (isAdd) currentAmount + amount else currentAmount - amount
            
            val values = ContentValues().apply {
                put(COLUMN_ASSET_AMOUNT, newAmount)
            }
            db.update(TABLE_ASSETS, values, "${assetScope()} AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0", arrayOf(assetId.toString()))
        }
        
        cursor.close()
    }

    fun updateRecordSortOrder(recordId: Long, newSortOrder: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SORT_ORDER, newSortOrder)
        }
        db.update(TABLE_RECORDS, values, "$COLUMN_ID = ?", arrayOf(recordId.toString()))
    }

    fun updateRecordsSortOrder(records: List<Record>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (record in records) {
                val values = ContentValues().apply {
                    put(COLUMN_SORT_ORDER, record.sortOrder)
                }
                db.update(TABLE_RECORDS, values, "$COLUMN_ID = ?", arrayOf(record.id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCurrentDate(nowMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(nowMillis))
    }

    fun addCategory(category: Category): Long {
        val db = writableDatabase
        validateParentAssignment(db, category)
        findCategoryId(db, category.name, category.type, category.parentId)?.let { return it }
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, category.name)
            put(COLUMN_CATEGORY_TYPE, category.type)
            put(COLUMN_CATEGORY_ICON, category.icon)
            if (category.parentId != null) {
                put(COLUMN_CATEGORY_PARENT_ID, category.parentId)
            } else {
                putNull(COLUMN_CATEGORY_PARENT_ID)
            }
            put(COLUMN_CATEGORY_SORT_ORDER, nextCategorySortOrder(db, category.type, category.parentId))
            put(COLUMN_LEDGER_ID, 1L)
        }

        return db.insert(TABLE_CATEGORIES, null, values)
    }

    fun updateCategorySortOrders(categoryIds: List<Long>) {
        if (categoryIds.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            categoryIds.forEachIndexed { index, id ->
                val values = ContentValues().apply {
                    put(COLUMN_CATEGORY_SORT_ORDER, index)
                }
                db.update(
                    TABLE_CATEGORIES,
                    values,
                    "$COLUMN_CATEGORY_ID = ?",
                    arrayOf(id.toString())
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    private fun nextCategorySortOrder(db: SQLiteDatabase, type: Int, parentId: Long?): Int {
        val selection = if (parentId == null) {
            "$COLUMN_CATEGORY_TYPE = ? AND $COLUMN_CATEGORY_PARENT_ID IS NULL"
        } else {
            "$COLUMN_CATEGORY_TYPE = ? AND $COLUMN_CATEGORY_PARENT_ID = ?"
        }
        val args = if (parentId == null) {
            arrayOf(type.toString())
        } else {
            arrayOf(type.toString(), parentId.toString())
        }
        return db.query(
            TABLE_CATEGORIES,
            arrayOf("MAX($COLUMN_CATEGORY_SORT_ORDER)"),
            selection,
            args,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) + 1 else 0
        }
    }

    private fun initializeCategorySortOrders(db: SQLiteDatabase) {
        val cursor = db.rawQuery(
            "SELECT $COLUMN_CATEGORY_ID, $COLUMN_CATEGORY_TYPE, $COLUMN_CATEGORY_PARENT_ID " +
                "FROM $TABLE_CATEGORIES ORDER BY $COLUMN_CATEGORY_TYPE, " +
                "$COLUMN_CATEGORY_PARENT_ID, $COLUMN_CATEGORY_NAME COLLATE NOCASE, $COLUMN_CATEGORY_ID",
            null
        )
        val nextByGroup = mutableMapOf<String, Int>()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(0)
                    val type = it.getInt(1)
                    val parentId = getNullableLong(it, COLUMN_CATEGORY_PARENT_ID)
                    val groupKey = "$type:${parentId ?: "root"}"
                    val values = ContentValues().apply {
                        put(COLUMN_CATEGORY_SORT_ORDER, nextByGroup.getOrDefault(groupKey, 0))
                    }
                    db.update(TABLE_CATEGORIES, values, "$COLUMN_CATEGORY_ID = ?", arrayOf(id.toString()))
                    nextByGroup[groupKey] = values.getAsInteger(COLUMN_CATEGORY_SORT_ORDER) + 1
                } while (it.moveToNext())
            }
        }
    }

    fun getCategoriesByType(type: Int): List<Category> {
        val categories = mutableListOf<Category>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_CATEGORIES WHERE $COLUMN_CATEGORY_TYPE = ?",
            arrayOf(type.toString())
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    categories.add(createCategoryFromCursor(it))
                } while (it.moveToNext())
            }
        }

        db.close()
        return categories
    }

    fun getCategoryTreeByType(type: Int): List<Category> {
        val db = readableDatabase
        ensureDefaultCategoriesForCurrentLedger(db)
        val categories = getCategoriesByTypeInternal(db, type)
        db.close()

        if (categories.isEmpty()) {
            return emptyList()
        }

        val categoriesByParentId = categories.groupBy { parentId ->
            val parentExists = parentId.parentId != null && categories.any { it.id == parentId.parentId }
            if (parentExists) {
                parentId.parentId
            } else {
                null
            }
        }
        val orderedCategories = mutableListOf<Category>()

        fun appendChildren(parentId: Long?) {
            categoriesByParentId[parentId]?.forEach { category ->
                orderedCategories.add(category)
                appendChildren(category.id)
            }
        }

        appendChildren(null)
        return orderedCategories
    }

    private fun ensureDefaultCategoriesForCurrentLedger(db: SQLiteDatabase) {
        val hasCategories = db.rawQuery("SELECT 1 FROM $TABLE_CATEGORIES LIMIT 1", null).use { it.moveToFirst() }
        if (!hasCategories) {
            insertDefaultCategories(db)
        }
    }

    fun getLeafCategoriesByType(type: Int): List<Category> {
        val categories = mutableListOf<Category>()
        val selectQuery =
            "SELECT c.* FROM $TABLE_CATEGORIES c " +
                "WHERE c.$COLUMN_CATEGORY_TYPE = ? " +
                "AND NOT EXISTS (" +
                "SELECT 1 FROM $TABLE_CATEGORIES child " +
                "WHERE child.$COLUMN_CATEGORY_PARENT_ID = c.$COLUMN_CATEGORY_ID " +
                "AND child.$COLUMN_CATEGORY_TYPE = c.$COLUMN_CATEGORY_TYPE) " +
                "ORDER BY c.$COLUMN_CATEGORY_NAME"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(type.toString()))

        if (cursor.moveToFirst()) {
            do {
                categories.add(createCategoryFromCursor(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return categories
    }

    fun buildCategoryPathLabel(categoryId: Long): String? {
        val db = readableDatabase
        val segments = mutableListOf<String>()
        val visited = mutableSetOf<Long>()
        var currentCategory = getCategoryByIdInternal(db, categoryId)

        while (currentCategory != null && visited.add(currentCategory.id)) {
            segments.add(currentCategory.name)
            currentCategory = currentCategory.parentId?.let { parentId ->
                getCategoryByIdInternal(db, parentId)
            }
        }

        db.close()
        return segments.takeIf { it.isNotEmpty() }?.asReversed()?.joinToString(" / ")
    }

    fun getCategoryById(id: Long): Category? {
        val db = readableDatabase
        val category = getCategoryByIdInternal(db, id)
        db.close()
        return category
    }

    fun getAllCategories(): List<Category> {
        val db = readableDatabase
        val categories = mutableListOf<Category>()
        categories += getCategoriesByTypeInternal(db, 0)
        categories += getCategoriesByTypeInternal(db, 1)
        db.close()
        return categories
    }

    fun updateCategory(category: Category): Int {
        val db = writableDatabase
        validateParentAssignment(db, category)
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, category.name)
            put(COLUMN_CATEGORY_TYPE, category.type)
            put(COLUMN_CATEGORY_ICON, category.icon)
            if (category.parentId != null) {
                put(COLUMN_CATEGORY_PARENT_ID, category.parentId)
            } else {
                putNull(COLUMN_CATEGORY_PARENT_ID)
            }
        }

        val rowsAffected = db.update(TABLE_CATEGORIES, values, "$COLUMN_CATEGORY_ID = ?",
            arrayOf(category.id.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteCategory(id: Long) {
        val db = writableDatabase
        if (categoryHasChildren(db, id)) {
            db.close()
            throw CategoryOperationException(CategoryOperationError.HAS_CHILDREN)
        }
        if (categoryIsReferencedByRecordId(db, id)) {
            db.close()
            throw CategoryOperationException(CategoryOperationError.IN_USE_BY_RECORDS)
        }
        db.delete(TABLE_CATEGORIES, "$COLUMN_CATEGORY_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun getTotalByType(type: Int): Double {
        var total = 0.0
        val selectQuery = "SELECT SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ?"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(currentLedgerId().toString(), type.toString()))

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }

        cursor.close()
        db.close()
        return total
    }

    fun getTotalByTypeAndDateRange(type: Int, startDate: String, endDate: String): Double {
        var total = 0.0
        val selectQuery = "SELECT SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ? AND $COLUMN_DATE BETWEEN ? AND ?"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(currentLedgerId().toString(), type.toString(), startDate, endDate))

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }

        cursor.close()
        db.close()
        return total
    }

    fun getRecordsByDateRange(startDate: String, endDate: String): List<Record> {
        val records = mutableListOf<Record>()
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_DATE BETWEEN ? AND ? ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(currentLedgerId().toString(), startDate, endDate))

        if (cursor.moveToFirst()) {
            do {
                records.add(createRecordFromCursor(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return records
    }

    fun getTodayRecordsPage(
        todayDate: String = getCurrentDate(),
        after: RecordPageCursor? = null
    ): RecordPage {
        val records = mutableListOf<Record>()
        val query: String
        val arguments: Array<String>
        if (after == null) {
            query =
                "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_DATE = ? " +
                    "ORDER BY $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC LIMIT $MAX_RECORD_QUERY_LIMIT"
            arguments = arrayOf(todayDate)
        } else {
            query =
                "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_DATE = ? " +
                    "AND ($COLUMN_SORT_ORDER > ? OR ($COLUMN_SORT_ORDER = ? AND $COLUMN_ID > ?)) " +
                    "ORDER BY $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC LIMIT $MAX_RECORD_QUERY_LIMIT"
            arguments = arrayOf(
                todayDate,
                after.sortOrder.toString(),
                after.sortOrder.toString(),
                after.recordId.toString()
            )
        }

        val cursor = readableDatabase.rawQuery(query, arguments)
        if (cursor.moveToFirst()) {
            do {
                records.add(createRecordFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()

        val nextCursor = if (records.size == MAX_RECORD_QUERY_LIMIT) {
            records.last().let { RecordPageCursor(it.sortOrder, it.id) }
        } else {
            null
        }
        return RecordPage(records, nextCursor)
    }

    fun getRecordsByAssetSource(assetSource: String): List<Record> {
        val records = mutableListOf<Record>()
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_SOURCE = ? ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(assetSource))

        if (cursor.moveToFirst()) {
            do {
                records.add(createRecordFromCursor(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return records
    }

    fun getRecordsByAssetId(assetId: Long): List<Record> {
        val records = mutableListOf<Record>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? " +
                "AND ($COLUMN_RECORD_ASSET_ID = ? OR $COLUMN_RECORD_DESTINATION_ASSET_ID = ?) " +
                "ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC",
            arrayOf(currentLedgerId().toString(), assetId.toString(), assetId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) records += createRecordFromCursor(cursor)
        }
        return records
    }

    fun getCategoryStatistics(type: Int): Map<String, Double> {
        val categoryStats = mutableMapOf<String, Double>()
        val selectQuery = "SELECT $COLUMN_CATEGORY, SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ? GROUP BY $COLUMN_CATEGORY"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(currentLedgerId().toString(), type.toString()))

        if (cursor.moveToFirst()) {
            do {
                val category = cursor.getString(0)
                val total = cursor.getDouble(1)
                categoryStats[category] = total
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return categoryStats
    }

    fun getCategoryStatisticsByDateRange(type: Int, startDate: String, endDate: String): Map<String, Double> {
        val categoryStats = mutableMapOf<String, Double>()
        val selectQuery = "SELECT $COLUMN_CATEGORY, SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ? AND $COLUMN_DATE BETWEEN ? AND ? GROUP BY $COLUMN_CATEGORY"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(currentLedgerId().toString(), type.toString(), startDate, endDate))

        if (cursor.moveToFirst()) {
            do {
                val category = cursor.getString(0)
                val total = cursor.getDouble(1)
                categoryStats[category] = total
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return categoryStats
    }

    fun getMonthlyStatistics(type: Int, year: Int): Map<String, Double> {
        val monthlyStats = mutableMapOf<String, Double>()
        val selectQuery = "SELECT SUBSTR($COLUMN_DATE, 1, 7) as month, SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ? AND SUBSTR($COLUMN_DATE, 1, 4) = ? GROUP BY month ORDER BY month"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(currentLedgerId().toString(), type.toString(), year.toString()))

        if (cursor.moveToFirst()) {
            do {
                val month = cursor.getString(0)
                val total = cursor.getDouble(1)
                monthlyStats[month] = total
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return monthlyStats
    }

    fun addAsset(asset: Asset): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LEDGER_ID, currentLedgerId())
            put(COLUMN_ASSET_NAME, asset.name)
            put(COLUMN_ASSET_AMOUNT, asset.amount)
            put(COLUMN_ASSET_TYPE, asset.type)
            put(COLUMN_ASSET_CATEGORY_LABEL, asset.categoryLabel)
            put(COLUMN_ASSET_CATEGORY_ICON_NAME, asset.categoryIconName)
            put(COLUMN_ASSET_IS_ARCHIVED, if (asset.isArchived) 1 else 0)
            put(COLUMN_ASSET_IS_PINNED, if (asset.isPinned) 1 else 0)
            put(COLUMN_ASSET_INCLUDE_IN_TOTAL, if (asset.includeInTotal) 1 else 0)
        }

        val id = db.insert(TABLE_ASSETS, null, values)
        db.close()
        return id
    }

    fun getAllAssets(): List<Asset> {
        val assets = mutableListOf<Asset>()
        val selectQuery = "SELECT * FROM $TABLE_ASSETS WHERE ${assetScope()} AND $COLUMN_ASSET_IS_ARCHIVED = 0 ORDER BY $COLUMN_ASSET_NAME"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val asset = Asset(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ASSET_ID)),
                    ledgerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LEDGER_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ASSET_AMOUNT)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TYPE)),
                    categoryLabel = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_CATEGORY_LABEL)),
                    categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_CATEGORY_ICON_NAME)),
                    isArchived = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_IS_ARCHIVED)) == 1,
                    isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_IS_PINNED)) == 1
                    ,includeInTotal = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_INCLUDE_IN_TOTAL)) == 1
                )
                assets.add(asset)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return assets
    }

    fun getArchivedAssets(): List<Asset> {
        val assets = mutableListOf<Asset>()
        val selectQuery = "SELECT * FROM $TABLE_ASSETS WHERE ${assetScope()} AND $COLUMN_ASSET_IS_ARCHIVED = 1 ORDER BY $COLUMN_ASSET_NAME"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val asset = Asset(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ASSET_ID)),
                    ledgerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LEDGER_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ASSET_AMOUNT)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TYPE)),
                    categoryLabel = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_CATEGORY_LABEL)),
                    categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_CATEGORY_ICON_NAME)),
                    isArchived = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_IS_ARCHIVED)) == 1,
                    isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_IS_PINNED)) == 1
                    ,includeInTotal = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_INCLUDE_IN_TOTAL)) == 1
                )
                assets.add(asset)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return assets
    }

    fun archiveAsset(id: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ASSET_IS_ARCHIVED, 1)
            put(COLUMN_ASSET_IS_PINNED, 0)
        }
        db.update(TABLE_ASSETS, values, "$COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun unarchiveAsset(id: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ASSET_IS_ARCHIVED, 0)
        }
        db.update(TABLE_ASSETS, values, "$COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun updateAsset(asset: Asset): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ASSET_NAME, asset.name)
            put(COLUMN_ASSET_AMOUNT, asset.amount)
            put(COLUMN_ASSET_TYPE, asset.type)
            put(COLUMN_ASSET_CATEGORY_LABEL, asset.categoryLabel)
            put(COLUMN_ASSET_CATEGORY_ICON_NAME, asset.categoryIconName)
            put(COLUMN_ASSET_IS_ARCHIVED, if (asset.isArchived) 1 else 0)
            put(COLUMN_ASSET_IS_PINNED, if (asset.isPinned) 1 else 0)
            put(COLUMN_ASSET_INCLUDE_IN_TOTAL, if (asset.includeInTotal) 1 else 0)
        }

        val rowsAffected = db.update(TABLE_ASSETS, values, "$COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_ID = ?",
            arrayOf(asset.id.toString()))
        db.close()
        return rowsAffected
    }

    fun setAssetPinned(id: Long, pinned: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ASSET_IS_PINNED, if (pinned) 1 else 0)
        }
        val rows = db.update(
            TABLE_ASSETS,
            values,
            "$COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0",
            arrayOf(id.toString())
        )
        db.close()
        return rows
    }

    fun deleteArchivedAsset(id: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery(
                "SELECT $COLUMN_ASSET_NAME, $COLUMN_ASSET_IS_ARCHIVED FROM $TABLE_ASSETS " +
                    "WHERE $COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_ID = ?",
                arrayOf(id.toString())
            )
            val asset = cursor.use {
                if (!it.moveToFirst()) null else Pair(it.getString(0), it.getInt(1) == 1)
            }
            if (asset == null) {
                db.setTransactionSuccessful()
                return false
            }
            if (!asset.second) {
                throw AssetOperationException(AssetOperationError.NOT_ARCHIVED)
            }
            if (assetNameIsReferenced(db, asset.first)) {
                throw AssetOperationException(AssetOperationError.IN_USE_BY_RECORDS)
            }

            val deleted = db.delete(TABLE_ASSETS, "$COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_ID = ?", arrayOf(id.toString())) == 1
            db.setTransactionSuccessful()
            return deleted
        } finally {
            db.endTransaction()
        }
    }

    fun deleteAsset(id: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery(
                "SELECT $COLUMN_ASSET_NAME FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_ID = ?",
                arrayOf(id.toString())
            )
            val assetName = cursor.use { if (it.moveToFirst()) it.getString(0) else null }
            if (assetName == null) {
                db.setTransactionSuccessful()
                return false
            }
            if (assetNameIsReferenced(db, assetName)) {
                throw AssetOperationException(AssetOperationError.IN_USE_BY_RECORDS)
            }
            val deleted = db.delete(TABLE_ASSETS, "$COLUMN_LEDGER_ID = ${currentLedgerId()} AND $COLUMN_ASSET_ID = ?", arrayOf(id.toString())) == 1
            db.setTransactionSuccessful()
            return deleted
        } finally {
            db.endTransaction()
        }
    }

    private fun assetNameIsReferenced(db: SQLiteDatabase, assetName: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT 1 FROM (" +
                "SELECT $COLUMN_ASSET_SOURCE FROM $TABLE_RECORDS WHERE $COLUMN_ASSET_SOURCE = ? " +
                "UNION ALL " +
                "SELECT $COLUMN_ASSET_SOURCE FROM $TABLE_RECORD_DELETION_UNDO " +
                "WHERE $COLUMN_ASSET_SOURCE = ? AND $COLUMN_UNDO_EXPIRES_AT > ?" +
                ") LIMIT 1",
            arrayOf(assetName, assetName, System.currentTimeMillis().toString())
        )
        return cursor.use { it.moveToFirst() }
    }

    fun getTotalAssets(): Double {
        var total = 0.0
        val selectQuery = "SELECT SUM($COLUMN_ASSET_AMOUNT) FROM $TABLE_ASSETS WHERE ${assetScope()} AND $COLUMN_ASSET_IS_ARCHIVED = 0 AND $COLUMN_ASSET_INCLUDE_IN_TOTAL = 1"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }

        cursor.close()
        db.close()
        return total
    }

    fun addAiChatSession(
        title: String,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = createdAt
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_AI_CHAT_SESSION_TITLE, title)
            put(COLUMN_AI_CHAT_SESSION_CREATED_AT, createdAt)
            put(COLUMN_AI_CHAT_SESSION_UPDATED_AT, updatedAt)
        }

        val id = db.insert(TABLE_AI_CHAT_SESSIONS, null, values)
        db.close()
        return id
    }

    fun getAiChatSessions(): List<AiChatSession> {
        val sessions = mutableListOf<AiChatSession>()
        val selectQuery = "SELECT * FROM $TABLE_AI_CHAT_SESSIONS ORDER BY $COLUMN_AI_CHAT_SESSION_UPDATED_AT DESC, $COLUMN_AI_CHAT_SESSION_ID DESC"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                sessions.add(
                    AiChatSession(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_SESSION_ID)),
                        title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_SESSION_TITLE)),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_SESSION_CREATED_AT)),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_SESSION_UPDATED_AT))
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return sessions
    }

    fun getAiChatSessionById(id: Long): AiChatSession? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_AI_CHAT_SESSIONS WHERE $COLUMN_AI_CHAT_SESSION_ID = ?",
            arrayOf(id.toString())
        )

        val session = if (cursor.moveToFirst()) {
            AiChatSession(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_SESSION_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_SESSION_TITLE)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_SESSION_CREATED_AT)),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_SESSION_UPDATED_AT))
            )
        } else {
            null
        }

        cursor.close()
        db.close()
        return session
    }

    fun updateAiChatSessionTitle(
        id: Long,
        title: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_AI_CHAT_SESSION_TITLE, title)
            put(COLUMN_AI_CHAT_SESSION_UPDATED_AT, updatedAt)
        }

        val rowsAffected = db.update(
            TABLE_AI_CHAT_SESSIONS,
            values,
            "$COLUMN_AI_CHAT_SESSION_ID = ?",
            arrayOf(id.toString())
        )
        db.close()
        return rowsAffected
    }

    fun touchAiChatSession(id: Long, updatedAt: Long = System.currentTimeMillis()): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_AI_CHAT_SESSION_UPDATED_AT, updatedAt)
        }

        val rowsAffected = db.update(
            TABLE_AI_CHAT_SESSIONS,
            values,
            "$COLUMN_AI_CHAT_SESSION_ID = ?",
            arrayOf(id.toString())
        )
        db.close()
        return rowsAffected
    }

    fun addAiChatMessage(message: AiChatMessage): Long {
        val db = writableDatabase
        val createdAt = message.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val values = ContentValues().apply {
            put(COLUMN_AI_CHAT_MESSAGE_SESSION_ID, message.sessionId)
            put(COLUMN_AI_CHAT_MESSAGE_ROLE, message.role.apiValue)
            put(COLUMN_AI_CHAT_MESSAGE_CONTENT, message.content)
            put(COLUMN_AI_CHAT_MESSAGE_IS_ERROR, if (message.isError) 1 else 0)
            put(COLUMN_AI_CHAT_MESSAGE_CREATED_AT, createdAt)
        }

        val id = db.insert(TABLE_AI_CHAT_MESSAGES, null, values)
        if (message.sessionId > 0L) {
            val sessionValues = ContentValues().apply {
                put(COLUMN_AI_CHAT_SESSION_UPDATED_AT, createdAt)
            }
            db.update(
                TABLE_AI_CHAT_SESSIONS,
                sessionValues,
                "$COLUMN_AI_CHAT_SESSION_ID = ?",
                arrayOf(message.sessionId.toString())
            )
        }
        db.close()
        return id
    }

    fun getAiChatMessages(sessionId: Long): List<AiChatMessage> {
        val messages = mutableListOf<AiChatMessage>()
        val selectQuery = "SELECT * FROM $TABLE_AI_CHAT_MESSAGES WHERE $COLUMN_AI_CHAT_MESSAGE_SESSION_ID = ? ORDER BY $COLUMN_AI_CHAT_MESSAGE_CREATED_AT ASC, $COLUMN_AI_CHAT_MESSAGE_ID ASC"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(sessionId.toString()))

        if (cursor.moveToFirst()) {
            do {
                messages.add(
                    AiChatMessage(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_MESSAGE_ID)),
                        sessionId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_MESSAGE_SESSION_ID)),
                        role = AiChatRole.values().firstOrNull {
                            it.apiValue == cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_MESSAGE_ROLE))
                        } ?: AiChatRole.ASSISTANT,
                        content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_MESSAGE_CONTENT)),
                        isError = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_MESSAGE_IS_ERROR)) == 1,
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_AI_CHAT_MESSAGE_CREATED_AT))
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return messages
    }
}
