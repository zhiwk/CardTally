package com.example.cardtally.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cardtally.BuildConfig
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatSession
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.model.RecurringRecord
import com.example.cardtally.util.CategoryHierarchySettingsHelper
import com.example.cardtally.util.LedgerSession
import com.example.cardtally.util.Money
import com.example.cardtally.util.RecurringScheduleCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DatabaseHelper(
    private val appContext: Context
) : SQLiteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val recurringRepository by lazy {
        RecurringRecordRepository(
            readableDatabase = { readableDatabase },
            writableDatabase = { writableDatabase },
            currentLedgerId = ::currentLedgerId,
            validateCategory = { db, recurring ->
                val categoryId = requireNotNull(recurring.categoryId)
                val category = getCategoryByIdInternal(db, categoryId) ?: error("Category missing")
                require(recurring.type != 2 && category.type == recurring.type && !categoryHasChildren(db, categoryId))
            },
            isAssetAvailable = { db, ledgerId, assetId ->
                db.rawQuery(
                    "SELECT 1 FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0",
                    arrayOf(ledgerId.toString(), assetId.toString())
                ).use { it.moveToFirst() }
            },
            columns = RECURRING_RECORD_COLUMNS
        )
    }
    private val categoryHierarchyValidator by lazy {
        CategoryHierarchyValidator(
            findCategory = ::getCategoryByIdInternal,
            configuredMaxDepth = { CategoryHierarchySettingsHelper.getCategoryMaxDepth(appContext) },
            throwError = { throw CategoryOperationException(it) }
        )
    }
    private val categoryReadRepository by lazy {
        CategoryReadRepository(
            readableDatabase = { readableDatabase },
            columns = CATEGORY_READ_COLUMNS
        )
    }
    private val categoryWriteRepository by lazy {
        CategoryWriteRepository(
            writableDatabase = { writableDatabase },
            validateParent = ::validateParentAssignment,
            findExistingId = { db, category ->
                findCategoryId(db, category.name, category.type, category.parentId)
            },
            hasChildren = ::categoryHasChildren,
            isReferencedByRecord = ::categoryIsReferencedByRecordId,
            throwError = { throw CategoryOperationException(it) },
            columns = CATEGORY_WRITE_COLUMNS
        )
    }
    private val aiChatRepository by lazy {
        AiChatRepository(
            readableDatabase = { readableDatabase },
            writableDatabase = { writableDatabase },
            columns = AI_CHAT_COLUMNS
        )
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

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
        private val RECURRING_PROCESS_LOCK = Any()
        /** v35 splits the combined credit asset label. */
        private const val DATABASE_VERSION = 37
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
        private const val COLUMN_RECORD_FEE = "fee"
        private const val COLUMN_RECORD_ASSET_ID = "asset_id"
        private const val COLUMN_RECORD_DESTINATION_ASSET_ID = "destination_asset_id"
        private const val COLUMN_ASSET_SOURCE = "asset_source"
        private const val COLUMN_DESTINATION_ASSET_SOURCE = "destination_asset_source"
        private const val COLUMN_PHOTO_URI = "photo_uri"
        private const val COLUMN_PHOTO_URIS = "photo_uris"
        private const val PHOTO_URI_SEPARATOR = "|"
        private const val COLUMN_SORT_ORDER = "sort_order"
        private const val COLUMN_LEDGER_ID = "ledger_id"
        private val RECORD_SQL_COLUMNS = RecordSqlMapper.Columns(
            id = COLUMN_ID,
            ledgerId = COLUMN_LEDGER_ID,
            date = COLUMN_DATE,
            amount = COLUMN_AMOUNT,
            category = COLUMN_CATEGORY,
            categoryId = COLUMN_RECORD_CATEGORY_ID,
            categoryNameSnapshot = COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT,
            categoryPathSnapshot = COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT,
            type = COLUMN_TYPE,
            description = COLUMN_DESCRIPTION,
            fee = COLUMN_RECORD_FEE,
            assetId = COLUMN_RECORD_ASSET_ID,
            destinationAssetId = COLUMN_RECORD_DESTINATION_ASSET_ID,
            assetSource = COLUMN_ASSET_SOURCE,
            destinationAssetSource = COLUMN_DESTINATION_ASSET_SOURCE,
            photoUri = COLUMN_PHOTO_URI,
            photoUris = COLUMN_PHOTO_URIS,
            sortOrder = COLUMN_SORT_ORDER,
            photoUriSeparator = PHOTO_URI_SEPARATOR
        )

        private const val TABLE_RECURRING_RECORDS = "recurring_records"
        private const val COLUMN_RECURRING_ID = "id"
        private const val COLUMN_RECURRING_TYPE = "type"
        private const val COLUMN_RECURRING_NAME = "name"
        private const val COLUMN_RECURRING_AMOUNT = "amount"
        private const val COLUMN_RECURRING_CATEGORY_ID = "category_id"
        private const val COLUMN_RECURRING_CATEGORY_NAME = "category_name"
        private const val COLUMN_RECURRING_CATEGORY_PATH = "category_path"
        private const val COLUMN_RECURRING_ASSET_ID = "asset_id"
        private const val COLUMN_RECURRING_ASSET_SOURCE = "asset_source"
        private const val COLUMN_RECURRING_DESTINATION_ASSET_ID = "destination_asset_id"
        private const val COLUMN_RECURRING_DESTINATION_ASSET_SOURCE = "destination_asset_source"
        private const val COLUMN_RECURRING_NOTE = "note"
        private const val COLUMN_RECURRING_FREQUENCY = "frequency"
        private const val COLUMN_RECURRING_WEEKLY_DAY = "weekly_day"
        private const val COLUMN_RECURRING_MONTHLY_DAY = "monthly_day"
        private const val COLUMN_RECURRING_YEARLY_MONTH = "yearly_month"
        private const val COLUMN_RECURRING_YEARLY_DAY = "yearly_day"
        private const val COLUMN_RECURRING_INTERVAL_DAYS = "interval_days"
        private const val COLUMN_RECURRING_START_DATE = "start_date"
        private const val COLUMN_RECURRING_END_DATE = "end_date"
        private const val COLUMN_RECURRING_ENABLED = "enabled"
        private const val COLUMN_RECURRING_NEXT_DUE_DATE = "next_due_date"
        private val RECURRING_RECORD_COLUMNS = RecurringRecordRepository.Columns(
            table = TABLE_RECURRING_RECORDS,
            id = COLUMN_RECURRING_ID,
            ledgerId = COLUMN_LEDGER_ID,
            type = COLUMN_RECURRING_TYPE,
            name = COLUMN_RECURRING_NAME,
            amount = COLUMN_RECURRING_AMOUNT,
            categoryId = COLUMN_RECURRING_CATEGORY_ID,
            categoryName = COLUMN_RECURRING_CATEGORY_NAME,
            categoryPath = COLUMN_RECURRING_CATEGORY_PATH,
            assetId = COLUMN_RECURRING_ASSET_ID,
            assetSource = COLUMN_RECURRING_ASSET_SOURCE,
            destinationAssetId = COLUMN_RECURRING_DESTINATION_ASSET_ID,
            destinationAssetSource = COLUMN_RECURRING_DESTINATION_ASSET_SOURCE,
            note = COLUMN_RECURRING_NOTE,
            frequency = COLUMN_RECURRING_FREQUENCY,
            weeklyDay = COLUMN_RECURRING_WEEKLY_DAY,
            monthlyDay = COLUMN_RECURRING_MONTHLY_DAY,
            yearlyMonth = COLUMN_RECURRING_YEARLY_MONTH,
            yearlyDay = COLUMN_RECURRING_YEARLY_DAY,
            intervalDays = COLUMN_RECURRING_INTERVAL_DAYS,
            startDate = COLUMN_RECURRING_START_DATE,
            endDate = COLUMN_RECURRING_END_DATE,
            enabled = COLUMN_RECURRING_ENABLED,
            nextDueDate = COLUMN_RECURRING_NEXT_DUE_DATE
        )

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
        private const val COLUMN_CATEGORY_COLOR = "color"
        private val CATEGORY_READ_COLUMNS = CategoryReadRepository.Columns(
            table = TABLE_CATEGORIES,
            id = COLUMN_CATEGORY_ID,
            name = COLUMN_CATEGORY_NAME,
            type = COLUMN_CATEGORY_TYPE,
            icon = COLUMN_CATEGORY_ICON,
            color = COLUMN_CATEGORY_COLOR,
            parentId = COLUMN_CATEGORY_PARENT_ID,
            sortOrder = COLUMN_CATEGORY_SORT_ORDER
        )
        private val CATEGORY_WRITE_COLUMNS = CategoryWriteRepository.Columns(
            table = TABLE_CATEGORIES,
            id = COLUMN_CATEGORY_ID,
            name = COLUMN_CATEGORY_NAME,
            type = COLUMN_CATEGORY_TYPE,
            icon = COLUMN_CATEGORY_ICON,
            color = COLUMN_CATEGORY_COLOR,
            parentId = COLUMN_CATEGORY_PARENT_ID,
            sortOrder = COLUMN_CATEGORY_SORT_ORDER,
            ledgerId = COLUMN_LEDGER_ID
        )

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
        private const val COLUMN_ASSET_SORT_ORDER = "sort_order"

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
        private const val COLUMN_AI_CHAT_MESSAGE_REASONING = "reasoning_content"
        private const val COLUMN_AI_CHAT_MESSAGE_IS_ERROR = "is_error"
        private const val COLUMN_AI_CHAT_MESSAGE_CREATED_AT = "created_at"
        private val AI_CHAT_COLUMNS = AiChatRepository.Columns(
            sessionsTable = TABLE_AI_CHAT_SESSIONS,
            sessionId = COLUMN_AI_CHAT_SESSION_ID,
            sessionTitle = COLUMN_AI_CHAT_SESSION_TITLE,
            sessionCreatedAt = COLUMN_AI_CHAT_SESSION_CREATED_AT,
            sessionUpdatedAt = COLUMN_AI_CHAT_SESSION_UPDATED_AT,
            messagesTable = TABLE_AI_CHAT_MESSAGES,
            messageId = COLUMN_AI_CHAT_MESSAGE_ID,
            messageSessionId = COLUMN_AI_CHAT_MESSAGE_SESSION_ID,
            messageRole = COLUMN_AI_CHAT_MESSAGE_ROLE,
            messageContent = COLUMN_AI_CHAT_MESSAGE_CONTENT,
            messageReasoning = COLUMN_AI_CHAT_MESSAGE_REASONING,
            messageIsError = COLUMN_AI_CHAT_MESSAGE_IS_ERROR,
            messageCreatedAt = COLUMN_AI_CHAT_MESSAGE_CREATED_AT
        )

        private const val CREATE_TABLE_RECORDS =
            "CREATE TABLE $TABLE_RECORDS (" +
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_DATE TEXT NOT NULL, " +
            "$COLUMN_AMOUNT INTEGER NOT NULL CHECK ($COLUMN_AMOUNT > 0), " +
            "$COLUMN_CATEGORY TEXT NOT NULL, " +
            "$COLUMN_RECORD_CATEGORY_ID INTEGER, " +
            "$COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT TEXT, " +
            "$COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT TEXT, " +
            "$COLUMN_TYPE INTEGER NOT NULL CHECK ($COLUMN_TYPE IN (0, 1, 2)), " +
            "$COLUMN_DESCRIPTION TEXT, " +
            "$COLUMN_RECORD_FEE INTEGER NOT NULL DEFAULT 0 CHECK ($COLUMN_RECORD_FEE >= 0), " +
            "$COLUMN_RECORD_ASSET_ID INTEGER, " +
            "$COLUMN_RECORD_DESTINATION_ASSET_ID INTEGER, " +
            "$COLUMN_ASSET_SOURCE TEXT, " +
            "$COLUMN_DESTINATION_ASSET_SOURCE TEXT, " +
            "$COLUMN_PHOTO_URI TEXT, " +
            "$COLUMN_PHOTO_URIS TEXT, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1, " +
            "$COLUMN_SORT_ORDER INTEGER NOT NULL DEFAULT 0, " +
            "CHECK ($COLUMN_TYPE != 2 OR ($COLUMN_RECORD_ASSET_ID IS NOT NULL AND $COLUMN_RECORD_DESTINATION_ASSET_ID IS NOT NULL AND $COLUMN_RECORD_ASSET_ID != $COLUMN_RECORD_DESTINATION_ASSET_ID)))"

        private const val CREATE_TABLE_CATEGORIES =
            "CREATE TABLE $TABLE_CATEGORIES (" +
            "$COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_CATEGORY_NAME TEXT NOT NULL, " +
            "$COLUMN_CATEGORY_TYPE INTEGER NOT NULL, " +
            "$COLUMN_CATEGORY_ICON TEXT, " +
            "$COLUMN_CATEGORY_COLOR TEXT NOT NULL DEFAULT '#F5F5F5', " +
            "$COLUMN_CATEGORY_PARENT_ID INTEGER, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL DEFAULT 1, " +
            "$COLUMN_CATEGORY_SORT_ORDER INTEGER NOT NULL DEFAULT 0)"

        private const val CREATE_TABLE_ASSETS =
            "CREATE TABLE $TABLE_ASSETS (" +
            "$COLUMN_ASSET_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_ASSET_NAME TEXT NOT NULL, " +
            "$COLUMN_ASSET_AMOUNT INTEGER NOT NULL, " +
            "$COLUMN_ASSET_TYPE INTEGER NOT NULL, " +
            "$COLUMN_ASSET_CATEGORY_LABEL TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_ASSET_CATEGORY_ICON_NAME TEXT NOT NULL DEFAULT '', " +
            "$COLUMN_ASSET_IS_ARCHIVED INTEGER DEFAULT 0, " +
            "$COLUMN_ASSET_IS_PINNED INTEGER DEFAULT 0, " +
            "$COLUMN_ASSET_INCLUDE_IN_TOTAL INTEGER DEFAULT 1, " +
            "$COLUMN_ASSET_SORT_ORDER INTEGER NOT NULL DEFAULT 0, " +
            "$COLUMN_LEDGER_ID INTEGER NOT NULL)"

        private const val CREATE_TABLE_RECORD_DELETION_UNDO =
            "CREATE TABLE $TABLE_RECORD_DELETION_UNDO (" +
            "$COLUMN_UNDO_TOKEN TEXT PRIMARY KEY, " +
            "$COLUMN_UNDO_EXPIRES_AT INTEGER NOT NULL, " +
            "$COLUMN_UNDO_BALANCE_DELTA INTEGER NOT NULL, " +
            "$COLUMN_ID INTEGER NOT NULL, " +
            "$COLUMN_DATE TEXT NOT NULL, " +
            "$COLUMN_AMOUNT INTEGER NOT NULL, " +
            "$COLUMN_CATEGORY TEXT NOT NULL, " +
            "$COLUMN_RECORD_CATEGORY_ID INTEGER, " +
            "$COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT TEXT, " +
            "$COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT TEXT, " +
            "$COLUMN_TYPE INTEGER NOT NULL, " +
            "$COLUMN_DESCRIPTION TEXT, " +
            "$COLUMN_RECORD_FEE INTEGER NOT NULL DEFAULT 0, " +
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
            "$COLUMN_LEDGER_ICON_NAME TEXT NOT NULL DEFAULT 'tabler_book')"

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
            "$COLUMN_AI_CHAT_MESSAGE_REASONING TEXT, " +
            "$COLUMN_AI_CHAT_MESSAGE_IS_ERROR INTEGER DEFAULT 0, " +
            "$COLUMN_AI_CHAT_MESSAGE_CREATED_AT INTEGER NOT NULL)"

        private const val CREATE_TABLE_RECURRING_RECORDS =
            "CREATE TABLE $TABLE_RECURRING_RECORDS (" +
                "$COLUMN_RECURRING_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_LEDGER_ID INTEGER NOT NULL, " +
                 "$COLUMN_RECURRING_TYPE INTEGER NOT NULL CHECK ($COLUMN_RECURRING_TYPE IN (0, 1, 2)), " +
                "$COLUMN_RECURRING_NAME TEXT NOT NULL, " +
                "$COLUMN_RECURRING_AMOUNT INTEGER NOT NULL CHECK ($COLUMN_RECURRING_AMOUNT > 0), " +
                "$COLUMN_RECURRING_CATEGORY_ID INTEGER, " +
                "$COLUMN_RECURRING_CATEGORY_NAME TEXT NOT NULL, " +
                "$COLUMN_RECURRING_CATEGORY_PATH TEXT, " +
                "$COLUMN_RECURRING_ASSET_ID INTEGER, " +
                 "$COLUMN_RECURRING_ASSET_SOURCE TEXT, " +
                 "$COLUMN_RECURRING_DESTINATION_ASSET_ID INTEGER, " +
                 "$COLUMN_RECURRING_DESTINATION_ASSET_SOURCE TEXT, " +
                "$COLUMN_RECURRING_NOTE TEXT, " +
                 "$COLUMN_RECURRING_FREQUENCY TEXT NOT NULL, " +
                 "$COLUMN_RECURRING_WEEKLY_DAY INTEGER, " +
                 "$COLUMN_RECURRING_MONTHLY_DAY INTEGER, " +
                 "$COLUMN_RECURRING_YEARLY_MONTH INTEGER, " +
                 "$COLUMN_RECURRING_YEARLY_DAY INTEGER, " +
                 "$COLUMN_RECURRING_INTERVAL_DAYS INTEGER, " +
                "$COLUMN_RECURRING_START_DATE TEXT NOT NULL, " +
                "$COLUMN_RECURRING_END_DATE TEXT, " +
                "$COLUMN_RECURRING_ENABLED INTEGER NOT NULL DEFAULT 1, " +
                "$COLUMN_RECURRING_NEXT_DUE_DATE TEXT NOT NULL)"

        private const val CREATE_INDEX_RECURRING_DUE =
            "CREATE INDEX IF NOT EXISTS idx_recurring_ledger_due ON $TABLE_RECURRING_RECORDS($COLUMN_LEDGER_ID, $COLUMN_RECURRING_ENABLED, $COLUMN_RECURRING_NEXT_DUE_DATE)"

        private const val CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT =
            "CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_updated_at ON $TABLE_AI_CHAT_SESSIONS($COLUMN_AI_CHAT_SESSION_UPDATED_AT DESC)"

        private const val CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT =
            "CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session_created_at ON $TABLE_AI_CHAT_MESSAGES($COLUMN_AI_CHAT_MESSAGE_SESSION_ID, $COLUMN_AI_CHAT_MESSAGE_CREATED_AT ASC)"

        private const val CREATE_INDEX_RECORDS_LEDGER_DATE =
            "CREATE INDEX IF NOT EXISTS idx_records_ledger_date_sort ON $TABLE_RECORDS($COLUMN_LEDGER_ID, $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC)"
        private const val CREATE_INDEX_RECORDS_LEDGER_TYPE_DATE =
            "CREATE INDEX IF NOT EXISTS idx_records_ledger_type_date ON $TABLE_RECORDS($COLUMN_LEDGER_ID, $COLUMN_TYPE, $COLUMN_DATE DESC)"
        private const val CREATE_INDEX_RECORDS_SOURCE_ASSET =
            "CREATE INDEX IF NOT EXISTS idx_records_source_asset_date ON $TABLE_RECORDS($COLUMN_RECORD_ASSET_ID, $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC)"
        private const val CREATE_INDEX_RECORDS_DESTINATION_ASSET =
            "CREATE INDEX IF NOT EXISTS idx_records_destination_asset_date ON $TABLE_RECORDS($COLUMN_RECORD_DESTINATION_ASSET_ID, $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC)"
        private const val CREATE_INDEX_CATEGORIES_TYPE_PARENT =
            "CREATE INDEX IF NOT EXISTS idx_categories_type_parent_sort ON $TABLE_CATEGORIES($COLUMN_CATEGORY_TYPE, $COLUMN_CATEGORY_PARENT_ID, $COLUMN_CATEGORY_SORT_ORDER, $COLUMN_CATEGORY_ID)"
        private const val CREATE_INDEX_ASSETS_LEDGER_ARCHIVED =
            "CREATE INDEX IF NOT EXISTS idx_assets_ledger_archived_name ON $TABLE_ASSETS($COLUMN_LEDGER_ID, $COLUMN_ASSET_IS_ARCHIVED, $COLUMN_ASSET_NAME)"
        private const val CREATE_INDEX_ASSETS_LEDGER_ARCHIVED_ORDER =
            "CREATE INDEX IF NOT EXISTS idx_assets_ledger_archived_order ON $TABLE_ASSETS($COLUMN_LEDGER_ID, $COLUMN_ASSET_IS_ARCHIVED, $COLUMN_ASSET_SORT_ORDER, $COLUMN_ASSET_ID)"
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
        if (saved != null) {
            val exists = readableDatabase.rawQuery(
                "SELECT 1 FROM $TABLE_LEDGERS WHERE $COLUMN_ID = ? LIMIT 1",
                arrayOf(saved.toString())
            ).use { it.moveToFirst() }
            if (exists) return saved
        }
        val id = readableDatabase.rawQuery(
            "SELECT $COLUMN_ID FROM $TABLE_LEDGERS ORDER BY $COLUMN_LEDGER_SORT_ORDER, $COLUMN_ID LIMIT 1", null
        ).use { if (it.moveToFirst()) it.getLong(0) else 1L }
        LedgerSession.setCurrentId(appContext, id)
        return id
    }

    private fun assetPoolRootId(ledgerId: Long): Long {
        val visited = mutableSetOf<Long>()
        var root = ledgerId
        while (visited.add(root)) root = getSharedSourceLedgerId(root) ?: break
        return root
    }

    /** Assets owned by the active ledger's effective asset pool. */
    private fun assetScope(): String {
        return "$COLUMN_LEDGER_ID = ${assetPoolRootId(currentLedgerId())}"
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

    /** The first ledger owns the permanent master asset pool. */
    fun getMasterLedgerId(): Long = readableDatabase.rawQuery(
        "SELECT $COLUMN_ID FROM $TABLE_LEDGERS ORDER BY $COLUMN_LEDGER_SORT_ORDER, $COLUMN_ID LIMIT 1", null
    ).use { if (it.moveToFirst()) it.getLong(0) else 1L }

    fun addLedger(name: String, subtitle: String = "", iconName: String = "tabler_book"): Long {
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

    fun addLedgerWithAssetPool(name: String, sharedSourceLedgerId: Long? = null, copyFromLedgerId: Long? = null, iconName: String = "tabler_book"): Long {
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
        val sourceId = assetPoolRootId(ledgerId)
        return readableDatabase.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(CASE WHEN $COLUMN_ASSET_INCLUDE_IN_TOTAL = 1 THEN $COLUMN_ASSET_AMOUNT ELSE 0 END), 0) " +
                "FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0",
            arrayOf(sourceId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) to Money.toMajorDouble(cursor.getLong(1)) else 0 to 0.0
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

    fun updateLedgerDetails(ledgerId: Long, iconName: String? = null, name: String? = null): Boolean {
        if (ledgerId != currentLedgerId()) return false
        if (iconName == null && name == null) return true
        return writableDatabase.update(TABLE_LEDGERS, ContentValues().apply {
            iconName?.let { put(COLUMN_LEDGER_ICON_NAME, it) }
            name?.let {
                put(COLUMN_LEDGER_NAME, it)
                put(COLUMN_LEDGER_SUBTITLE, "${it}账本")
            }
        }, "$COLUMN_ID = ?", arrayOf(ledgerId.toString())) > 0
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
                put(COLUMN_ASSET_AMOUNT, cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ASSET_AMOUNT)))
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

    /** Deletes ledgers and their records while retaining a shared pool for surviving ledgers. */
    fun deleteLedgers(ledgerIds: Set<Long>): Boolean {
        val allLedgers = getLedgers()
        if (ledgerIds.isEmpty() || getMasterLedgerId() in ledgerIds || ledgerIds.size >= allLedgers.size || !ledgerIds.all { id -> allLedgers.any { it.id == id } }) return false
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
        db.execSQL(CREATE_TABLE_RECURRING_RECORDS)
        db.execSQL(CREATE_INDEX_RECORDS_LEDGER_DATE)
        db.execSQL(CREATE_INDEX_RECORDS_LEDGER_TYPE_DATE)
        db.execSQL(CREATE_INDEX_RECORDS_SOURCE_ASSET)
        db.execSQL(CREATE_INDEX_RECORDS_DESTINATION_ASSET)
        db.execSQL(CREATE_INDEX_CATEGORIES_TYPE_PARENT)
        db.execSQL(CREATE_INDEX_ASSETS_LEDGER_ARCHIVED)
        db.execSQL(CREATE_INDEX_ASSETS_LEDGER_ARCHIVED_ORDER)
        db.execSQL(CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT)
        db.execSQL(CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT)
        db.execSQL(CREATE_INDEX_RECURRING_DUE)
        insertDefaultCategories(db, 1L)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Verification deliberately starts from one coherent schema. Earlier builds
        // do not carry forward local data, so no partial legacy migration can survive.
        if (BuildConfig.APPLICATION_ID.endsWith(".verification")) {
            resetAllDataForVerification(db)
        } else {
            // Daily sandboxes retain their data. Their existing schema already has
            // the columns used by the app; add safe performance-only indices.
            if (oldVersion < 33) {
                ensureColumn(
                    db,
                    TABLE_ASSETS,
                    COLUMN_ASSET_SORT_ORDER,
                    "ALTER TABLE $TABLE_ASSETS ADD COLUMN $COLUMN_ASSET_SORT_ORDER INTEGER NOT NULL DEFAULT 0"
                )
                initializeAssetSortOrders(db)
            }
            if (oldVersion < 34) {
                db.execSQL(CREATE_TABLE_RECURRING_RECORDS)
                db.execSQL(CREATE_INDEX_RECURRING_DUE)
            }
            if (oldVersion < 35) {
                db.execSQL(
                    "UPDATE $TABLE_ASSETS SET $COLUMN_ASSET_CATEGORY_LABEL = ?, " +
                        "$COLUMN_ASSET_CATEGORY_ICON_NAME = ? WHERE $COLUMN_ASSET_CATEGORY_LABEL = ?",
                    arrayOf("其他信用", "tabler_credit_card", "借呗 / 其他信用")
                )
            }
            if (oldVersion < 36) {
                ensureColumn(db, TABLE_RECURRING_RECORDS, COLUMN_RECURRING_WEEKLY_DAY,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_WEEKLY_DAY INTEGER")
                ensureColumn(db, TABLE_RECURRING_RECORDS, COLUMN_RECURRING_MONTHLY_DAY,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_MONTHLY_DAY INTEGER")
                ensureColumn(db, TABLE_RECURRING_RECORDS, COLUMN_RECURRING_YEARLY_MONTH,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_YEARLY_MONTH INTEGER")
                ensureColumn(db, TABLE_RECURRING_RECORDS, COLUMN_RECURRING_YEARLY_DAY,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_YEARLY_DAY INTEGER")
                ensureColumn(db, TABLE_RECURRING_RECORDS, COLUMN_RECURRING_INTERVAL_DAYS,
                    "ALTER TABLE $TABLE_RECURRING_RECORDS ADD COLUMN $COLUMN_RECURRING_INTERVAL_DAYS INTEGER")
            }
            if (oldVersion < 37) {
                val legacyTable = "${TABLE_RECURRING_RECORDS}_legacy"
                db.execSQL("ALTER TABLE $TABLE_RECURRING_RECORDS RENAME TO $legacyTable")
                db.execSQL(CREATE_TABLE_RECURRING_RECORDS)
                db.execSQL(
                    "INSERT INTO $TABLE_RECURRING_RECORDS (" +
                        "$COLUMN_RECURRING_ID, $COLUMN_LEDGER_ID, $COLUMN_RECURRING_TYPE, $COLUMN_RECURRING_NAME, " +
                        "$COLUMN_RECURRING_AMOUNT, $COLUMN_RECURRING_CATEGORY_ID, $COLUMN_RECURRING_CATEGORY_NAME, " +
                        "$COLUMN_RECURRING_CATEGORY_PATH, $COLUMN_RECURRING_ASSET_ID, $COLUMN_RECURRING_ASSET_SOURCE, " +
                        "$COLUMN_RECURRING_NOTE, $COLUMN_RECURRING_FREQUENCY, $COLUMN_RECURRING_START_DATE, " +
                        "$COLUMN_RECURRING_END_DATE, $COLUMN_RECURRING_ENABLED, $COLUMN_RECURRING_NEXT_DUE_DATE) " +
                        "SELECT $COLUMN_RECURRING_ID, $COLUMN_LEDGER_ID, $COLUMN_RECURRING_TYPE, $COLUMN_RECURRING_NAME, " +
                        "$COLUMN_RECURRING_AMOUNT, $COLUMN_RECURRING_CATEGORY_ID, $COLUMN_RECURRING_CATEGORY_NAME, " +
                        "$COLUMN_RECURRING_CATEGORY_PATH, $COLUMN_RECURRING_ASSET_ID, $COLUMN_RECURRING_ASSET_SOURCE, " +
                        "$COLUMN_RECURRING_NOTE, $COLUMN_RECURRING_FREQUENCY, $COLUMN_RECURRING_START_DATE, " +
                        "$COLUMN_RECURRING_END_DATE, $COLUMN_RECURRING_ENABLED, $COLUMN_RECURRING_NEXT_DUE_DATE " +
                        "FROM $legacyTable"
                )
                db.execSQL("DROP TABLE $legacyTable")
                db.execSQL(CREATE_INDEX_RECURRING_DUE)
            }
            createPerformanceIndices(db)
        }
        return

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
                "ALTER TABLE $TABLE_LEDGERS ADD COLUMN $COLUMN_LEDGER_ICON_NAME TEXT NOT NULL DEFAULT 'tabler_book'"
            )
        }
        if (oldVersion < 28) {
            normalizeCategoriesAsGlobal(db)
        }
        if (oldVersion < 29) {
            ensureColumn(db, TABLE_CATEGORIES, COLUMN_CATEGORY_COLOR,
                "ALTER TABLE $TABLE_CATEGORIES ADD COLUMN $COLUMN_CATEGORY_COLOR TEXT NOT NULL DEFAULT '#F5F5F5'")
        }
        if (oldVersion < 30) {
            ensureColumn(db, TABLE_RECORDS, COLUMN_RECORD_FEE,
                "ALTER TABLE $TABLE_RECORDS ADD COLUMN $COLUMN_RECORD_FEE REAL NOT NULL DEFAULT 0")
            ensureColumn(db, TABLE_RECORD_DELETION_UNDO, COLUMN_RECORD_FEE,
                "ALTER TABLE $TABLE_RECORD_DELETION_UNDO ADD COLUMN $COLUMN_RECORD_FEE REAL NOT NULL DEFAULT 0")
        }
        if (oldVersion < 31) {
            ensureColumn(db, TABLE_AI_CHAT_MESSAGES, COLUMN_AI_CHAT_MESSAGE_REASONING,
                "ALTER TABLE $TABLE_AI_CHAT_MESSAGES ADD COLUMN $COLUMN_AI_CHAT_MESSAGE_REASONING TEXT")
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

    private fun resetAllDataForVerification(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORD_DELETION_UNDO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGER_SHARED_ASSETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGER_SHARED_LEDGERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ASSETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AI_CHAT_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AI_CHAT_SESSIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECURRING_RECORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGERS")
        onCreate(db)
        LedgerSession.setCurrentId(appContext, 1L)
    }

    private fun createPerformanceIndices(db: SQLiteDatabase) {
        db.execSQL(CREATE_INDEX_RECORDS_LEDGER_DATE)
        db.execSQL(CREATE_INDEX_RECORDS_LEDGER_TYPE_DATE)
        db.execSQL(CREATE_INDEX_RECORDS_SOURCE_ASSET)
        db.execSQL(CREATE_INDEX_RECORDS_DESTINATION_ASSET)
        db.execSQL(CREATE_INDEX_CATEGORIES_TYPE_PARENT)
        db.execSQL(CREATE_INDEX_ASSETS_LEDGER_ARCHIVED)
        db.execSQL(CREATE_INDEX_ASSETS_LEDGER_ARCHIVED_ORDER)
        db.execSQL(CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT)
        db.execSQL(CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT)
        db.execSQL(CREATE_INDEX_RECURRING_DUE)
    }

    private fun resetFinancialData(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORD_DELETION_UNDO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ASSETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LEDGER_SHARED_ASSETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RECURRING_RECORDS")
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
        db.execSQL(CREATE_TABLE_RECURRING_RECORDS)
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
            "购物" to listOf("服饰" to "tabler_shirt", "家电" to "tabler_devices", "数码" to "tabler_device_laptop"),
            "餐饮" to listOf("早午晚餐" to "tabler_tools_kitchen"),
            "居住" to listOf("房租" to "tabler_home", "酒店" to "tabler_hotel_service"),
            "交通" to listOf("短途" to "tabler_car", "飞机高铁" to "tabler_plane")
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

    private fun createRecordValues(record: Record): ContentValues =
        RecordSqlMapper.toContentValues(record, currentLedgerId(), RECORD_SQL_COLUMNS)

    private fun createRecordFromCursor(cursor: Cursor): Record =
        RecordSqlMapper.fromCursor(cursor, RECORD_SQL_COLUMNS)

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

    private fun getNullableInt(cursor: Cursor, columnName: String): Int? {
        val columnIndex = cursor.getColumnIndex(columnName)
        if (columnIndex == -1 || cursor.isNull(columnIndex)) return null
        return cursor.getInt(columnIndex)
    }

    private fun getCategoryByIdInternal(db: SQLiteDatabase, id: Long): Category? =
        categoryReadRepository.getById(db, id)

    private fun getCategoriesByTypeInternal(db: SQLiteDatabase, type: Int): List<Category> =
        categoryReadRepository.getByType(db, type)

    private fun validateParentAssignment(db: SQLiteDatabase, category: Category) {
        categoryHierarchyValidator.validateParentAssignment(db, category)
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
        db.beginTransaction()
        return try {
            validateRecordForWrite(db, record)
            val maxSortOrder = db.rawQuery(
                "SELECT COALESCE(MAX($COLUMN_SORT_ORDER), 0) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_DATE = ?",
                arrayOf(currentLedgerId().toString(), record.date)
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
            val id = db.insertOrThrow(TABLE_RECORDS, null, createRecordValues(record).apply {
                put(COLUMN_SORT_ORDER, maxSortOrder + 1)
            })
            applyRecordAssetEffect(db, record, reverse = false)
            db.setTransactionSuccessful()
            id
        } catch (_: IllegalArgumentException) {
            -1L
        } finally {
            db.endTransaction()
        }
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
        db.beginTransaction()
        return try {
            validateRecordForWrite(db, record)
            val oldRecord = getRecordByIdInternal(db, record.id) ?: return 0
            applyRecordAssetEffect(db, oldRecord, reverse = true)
            val rowsAffected = db.update(
                TABLE_RECORDS,
                createRecordValues(record),
                "$COLUMN_ID = ? AND $COLUMN_LEDGER_ID = ?",
                arrayOf(record.id.toString(), currentLedgerId().toString())
            )
            check(rowsAffected == 1)
            applyRecordAssetEffect(db, record, reverse = false)
            db.setTransactionSuccessful()
            rowsAffected
        } catch (_: IllegalArgumentException) {
            0
        } finally {
            db.endTransaction()
        }
    }

    fun getRecurringRecords(): List<RecurringRecord> = recurringRepository.getForCurrentLedger()

    fun getAllRecurringRecords(): List<RecurringRecord> = recurringRepository.getAll()

    fun getRecurringRecord(id: Long): RecurringRecord? = recurringRepository.getForCurrentLedger(id)

    fun getRecurringRecordById(id: Long): RecurringRecord? = recurringRepository.getById(id)

    fun saveRecurringRecord(recurring: RecurringRecord): Long = recurringRepository.save(recurring)

    fun setRecurringEnabled(id: Long, enabled: Boolean): Boolean = recurringRepository.setEnabled(id, enabled)

    fun deleteRecurringRecord(id: Long): Boolean = recurringRepository.delete(id)

    fun getEarliestRecurringDueDate(): String? = recurringRepository.getEarliestDueDate()

    /** Repairs stale due dates left behind when a recurring rule was changed. */
    fun repairInvalidRecurringNextDueDates(today: String = getCurrentDate()): Int =
        synchronized(RECURRING_PROCESS_LOCK) {
            val selectedLedgerId = LedgerSession.getCurrentId(appContext)
            var repaired = 0
            getLedgers().forEach { ledger ->
                LedgerSession.setCurrentId(appContext, ledger.id)
                getRecurringRecords().forEach { template ->
                    val normalized = RecurringScheduleCalculator.normalizeDueDate(template.nextDueDate, template)
                    if (normalized != template.nextDueDate) {
                        recurringRepository.updateNextDueDate(template.id, ledger.id, normalized)
                        repaired++
                    }
                }
            }
            selectedLedgerId?.let { LedgerSession.setCurrentId(appContext, it) }
            repaired
        }

    /** Processes every ledger without changing which ledger the user has selected. */
    fun processDueRecurringRecordsForAllLedgers(today: String = getCurrentDate()): Int =
        synchronized(RECURRING_PROCESS_LOCK) {
            val selectedLedgerId = LedgerSession.getCurrentId(appContext)
            var generated = 0
            getLedgers().forEach { ledger ->
                LedgerSession.setCurrentId(appContext, ledger.id)
                generated += processDueRecurringRecordsForCurrentLedger(today)
            }
            selectedLedgerId?.let { LedgerSession.setCurrentId(appContext, it) }
            generated
        }

    /** Generates all missed occurrences for the active ledger when the app returns to the foreground. */
    fun processDueRecurringRecords(today: String = getCurrentDate()): Int =
        synchronized(RECURRING_PROCESS_LOCK) {
            processDueRecurringRecordsForCurrentLedger(today)
        }

    private fun processDueRecurringRecordsForCurrentLedger(today: String): Int {
        var generated = 0
        getRecurringRecords().filter { it.enabled }.forEach { template ->
            var due = RecurringScheduleCalculator.normalizeDueDate(template.nextDueDate, template)
            while (due <= today && (template.endDate == null || due <= template.endDate!!)) {
                val recordId = addRecord(Record(
                    date = due,
                    amount = Money.toMajorDouble(template.amountMinor),
                    category = template.categoryName,
                    categoryId = template.categoryId,
                    categoryNameSnapshot = template.categoryName,
                    categoryPathSnapshot = template.categoryPath,
                    type = template.type,
                    description = template.note,
                    assetId = template.assetId,
                    assetSource = template.assetSource,
                    destinationAssetId = template.destinationAssetId,
                    destinationAssetSource = template.destinationAssetSource,
                    ledgerId = template.ledgerId
                ))
                if (recordId <= 0L) break
                generated++
                due = RecurringScheduleCalculator.nextDate(due, template)
            }
            val stillActive = template.endDate == null || due <= template.endDate!!
            recurringRepository.updateNextDueDate(
                template.id,
                currentLedgerId(),
                due,
                enabled = if (stillActive) null else false
            )
        }
        return generated
    }

    private fun validateRecordForWrite(db: SQLiteDatabase, record: Record) {
        require(record.type in 0..2) { "Unsupported record type" }
        require(record.amount > 0.0 && Money.toMinor(record.amount) != null) { "Amount must be positive and finite" }
        require(record.fee >= 0.0 && Money.toMinor(record.fee) != null) { "Fee cannot be negative" }
        if (record.type == 2) {
            require(record.assetId != null && record.destinationAssetId != null) { "Transfer assets are required" }
            require(record.assetId != record.destinationAssetId) { "Transfer assets must differ" }
        } else {
            require(record.category.isNotBlank()) { "Category is required" }
            record.categoryId?.let { categoryId ->
                val category = getCategoryByIdInternal(db, categoryId)
                    ?: throw IllegalArgumentException("Category missing")
                require(category.type == record.type && !categoryHasChildren(db, categoryId)) { "Category must be a matching leaf" }
            }
        }
        listOfNotNull(record.assetId, record.destinationAssetId).forEach { assetId ->
            val exists = db.rawQuery(
                "SELECT 1 FROM $TABLE_ASSETS WHERE ${assetScope()} AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0 LIMIT 1",
                arrayOf(assetId.toString())
            ).use { it.moveToFirst() }
            require(exists) { "Asset unavailable" }
        }
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
            "购物" to listOf("服饰" to "tabler_shirt", "家电" to "tabler_devices", "数码" to "tabler_device_laptop"),
            "餐饮" to listOf("早午晚餐" to "tabler_tools_kitchen"),
            "居住" to listOf("房租" to "tabler_home", "酒店" to "tabler_hotel_service"),
            "交通" to listOf("短途" to "tabler_car", "飞机高铁" to "tabler_plane")
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
            "工作" to "tabler_briefcase",
            "理财" to "tabler_building_bank"
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
            "工作" to listOf("工资" to "ic_category_salary", "报销" to "tabler_receipt"),
            "理财" to listOf(
                "股票" to "tabler_chart_line",
                "基金" to "tabler_chart_donut",
                "黄金" to "tabler_pig_money"
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
                        balanceDelta = Money.toMajorDouble(it.getLong(it.getColumnIndexOrThrow(COLUMN_UNDO_BALANCE_DELTA)))
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
            put(COLUMN_UNDO_BALANCE_DELTA, requireNotNull(Money.toMinor(balanceDelta)))
            put(COLUMN_ID, record.id)
            put(COLUMN_DATE, record.date)
            put(COLUMN_AMOUNT, requireNotNull(Money.toMinor(record.amount)))
            put(COLUMN_CATEGORY, record.category)
            putNullable(COLUMN_RECORD_CATEGORY_ID, record.categoryId)
            putNullable(COLUMN_RECORD_CATEGORY_NAME_SNAPSHOT, record.categoryNameSnapshot)
            putNullable(COLUMN_RECORD_CATEGORY_PATH_SNAPSHOT, record.categoryPathSnapshot)
            put(COLUMN_TYPE, record.type)
            putNullable(COLUMN_DESCRIPTION, record.description)
            put(COLUMN_RECORD_FEE, requireNotNull(Money.toMinor(record.fee)))
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
            arrayOf(requireNotNull(Money.toMinor(delta)).toString(), identityValue)
        )
        return true
    }

    private fun applyRecordAssetEffect(db: SQLiteDatabase, record: Record, reverse: Boolean) {
        val direction = if (reverse) -1 else 1
        when (record.type) {
            0 -> record.assetId?.let { check(updateAssetAmount(db, it, record.amount * direction, false)) }
            1 -> record.assetId?.let { check(updateAssetAmount(db, it, record.amount * direction, true)) }
            2 -> {
                val transferOut = record.amount + record.fee
                record.assetId?.let { check(updateAssetAmount(db, it, transferOut * direction, false)) }
                record.destinationAssetId?.let { check(updateAssetAmount(db, it, record.amount * direction, true)) }
            }
        }
    }

    private fun updateAssetAmount(db: SQLiteDatabase, assetId: Long, amount: Double, isAdd: Boolean): Boolean {
        val selectQuery = "SELECT $COLUMN_ASSET_AMOUNT FROM $TABLE_ASSETS WHERE ${assetScope()} AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0 LIMIT 1"
        val cursor = db.rawQuery(selectQuery, arrayOf(assetId.toString()))
        
        val updated = if (cursor.moveToFirst()) {
            val currentAmount = cursor.getLong(0)
            val amountMinor = requireNotNull(Money.toMinor(amount))
            val newAmount = if (isAdd) currentAmount + amountMinor else currentAmount - amountMinor
            
            val values = ContentValues().apply {
                put(COLUMN_ASSET_AMOUNT, newAmount)
            }
            db.update(TABLE_ASSETS, values, "${assetScope()} AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0", arrayOf(assetId.toString())) == 1
        } else false
        
        cursor.close()
        return updated
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

    fun addCategory(category: Category): Long = categoryWriteRepository.add(category)

    fun updateCategorySortOrders(categoryIds: List<Long>) =
        categoryWriteRepository.updateSortOrders(categoryIds)

    private fun initializeCategorySortOrders(db: SQLiteDatabase) =
        categoryWriteRepository.initializeSortOrders(db)

    private fun initializeAssetSortOrders(db: SQLiteDatabase) {
        db.rawQuery(
            "SELECT $COLUMN_ASSET_ID FROM $TABLE_ASSETS ORDER BY $COLUMN_ASSET_NAME COLLATE NOCASE, $COLUMN_ASSET_ID",
            null
        ).use { cursor ->
            var order = 0
            while (cursor.moveToNext()) {
                db.update(
                    TABLE_ASSETS,
                    ContentValues().apply { put(COLUMN_ASSET_SORT_ORDER, order++) },
                    "$COLUMN_ASSET_ID = ?",
                    arrayOf(cursor.getLong(0).toString())
                )
            }
        }
    }

    fun getCategoriesByType(type: Int): List<Category> = categoryReadRepository.getByType(type)

    fun getCategoryTreeByType(type: Int): List<Category> {
        val db = readableDatabase
        ensureDefaultCategoriesForCurrentLedger(db)
        val categories = getCategoriesByTypeInternal(db, type)
        db.close()

        return CategoryTreeOrdering.order(categories)
    }

    private fun ensureDefaultCategoriesForCurrentLedger(db: SQLiteDatabase) {
        val hasCategories = db.rawQuery("SELECT 1 FROM $TABLE_CATEGORIES LIMIT 1", null).use { it.moveToFirst() }
        if (!hasCategories) {
            insertDefaultCategories(db)
        }
    }

    fun getLeafCategoriesByType(type: Int): List<Category> =
        categoryReadRepository.getLeavesByType(type)

    fun buildCategoryPathLabel(categoryId: Long): String? =
        categoryReadRepository.buildPathLabel(categoryId)

    fun getCategoryById(id: Long): Category? =
        categoryReadRepository.getById(readableDatabase, id)

    fun getAllCategories(): List<Category> {
        val db = readableDatabase
        val categories = mutableListOf<Category>()
        categories += getCategoriesByTypeInternal(db, 0)
        categories += getCategoriesByTypeInternal(db, 1)
        db.close()
        return categories
    }

    fun updateCategory(category: Category): Int = categoryWriteRepository.update(category)

    fun deleteCategory(id: Long) = categoryWriteRepository.delete(id)

    fun getTotalByType(type: Int): Double = Money.toMajorDouble(getTotalByTypeMinor(type))

    fun getTotalByTypeMinor(type: Int): Long {
        val db = readableDatabase
        val amount = db.rawQuery(
            "SELECT COALESCE(SUM($COLUMN_AMOUNT), 0) FROM $TABLE_RECORDS " +
                "WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ?",
            arrayOf(currentLedgerId().toString(), type.toString())
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        val fee = if (type == 0) getTransferFeeSumMinor(db, null, null) else 0L
        db.close()
        return Math.addExact(amount, fee)
    }

    /** Transfer fees count toward the expense total even though transfers are not income/expense. */
    private fun getTransferFeeSumMinor(db: SQLiteDatabase, startDate: String?, endDate: String?): Long {
        val rangeClause = if (startDate != null && endDate != null) " AND $COLUMN_DATE BETWEEN ? AND ?" else ""
        val args = if (rangeClause.isEmpty()) {
            arrayOf(currentLedgerId().toString(), "2")
        } else {
            arrayOf(currentLedgerId().toString(), "2", startDate!!, endDate!!)
        }
        val cursor = db.rawQuery(
            "SELECT COALESCE(SUM($COLUMN_RECORD_FEE), 0) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ?$rangeClause",
            args
        )
        val sum = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        cursor.close()
        return sum
    }

    fun getTotalByTypeAndDateRange(type: Int, startDate: String, endDate: String): Double =
        Money.toMajorDouble(getTotalByTypeAndDateRangeMinor(type, startDate, endDate))

    fun getTotalByTypeAndDateRangeMinor(type: Int, startDate: String, endDate: String): Long {
        val db = readableDatabase
        val amount = db.rawQuery(
            "SELECT COALESCE(SUM($COLUMN_AMOUNT), 0) FROM $TABLE_RECORDS " +
                "WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ? AND $COLUMN_DATE BETWEEN ? AND ?",
            arrayOf(currentLedgerId().toString(), type.toString(), startDate, endDate)
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        val fee = if (type == 0) getTransferFeeSumMinor(db, startDate, endDate) else 0L
        db.close()
        return Math.addExact(amount, fee)
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
                "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_DATE = ? " +
                    "ORDER BY $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC LIMIT $MAX_RECORD_QUERY_LIMIT"
            arguments = arrayOf(currentLedgerId().toString(), todayDate)
        } else {
            query =
                "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_DATE = ? " +
                    "AND ($COLUMN_SORT_ORDER > ? OR ($COLUMN_SORT_ORDER = ? AND $COLUMN_ID > ?)) " +
                    "ORDER BY $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC LIMIT $MAX_RECORD_QUERY_LIMIT"
            arguments = arrayOf(
                currentLedgerId().toString(),
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

    /**
     * Every ledger that references [assetId], in every ledger — the asset-detail
     * history is a property of the asset id, not of the currently selected ledger.
     * Each row appears once even when the same asset is both source and
     * destination (a transfer to itself is still a single record).
     */
    fun getAllRecordsByAssetId(assetId: Long): List<Record> {
        val records = mutableListOf<Record>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLE_RECORDS " +
                "WHERE ($COLUMN_RECORD_ASSET_ID = ? OR $COLUMN_RECORD_DESTINATION_ASSET_ID = ?) " +
                "ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC, $COLUMN_ID ASC",
            arrayOf(assetId.toString(), assetId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) records += createRecordFromCursor(cursor)
        }
        return records
    }

    /** Ledger names keyed by id, for bulk provenance labels (one query, no N+1). */
    fun getLedgerNamesByIds(ledgerIds: Set<Long>): Map<Long, String> {
        if (ledgerIds.isEmpty()) return emptyMap()
        val names = HashMap<Long, String>()
        readableDatabase.rawQuery(
            "SELECT $COLUMN_ID, $COLUMN_LEDGER_NAME FROM $TABLE_LEDGERS",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                if (id in ledgerIds) names[id] = cursor.getString(1)
            }
        }
        return names
    }

    /** A ledger's ledger-row id, needed to flag "this row belongs to this ledger". */
    fun getLedgerIdForRecord(recordId: Long): Long? = readableDatabase.rawQuery(
        "SELECT $COLUMN_LEDGER_ID FROM $TABLE_RECORDS WHERE $COLUMN_ID = ?",
        arrayOf(recordId.toString())
    ).use { if (it.moveToFirst()) it.getLong(0) else null }

    /**
     * One row per distinct asset group (pool root). [rootLedgerId] and
     * [rootLedgerName] describe the owner; [memberLedgerIds] are the ledgers that
     * share the same pool, and [sameNameGroupCount] distinguishes identically
     * named groups without forcing the user to maintain group names.
     */
    data class AssetGroupDescriptor(
        val rootLedgerId: Long,
        val rootLedgerName: String,
        val memberLedgerIds: List<Long>,
        val memberLedgerNames: List<String>,
        val isMaster: Boolean,
        val assetCount: Int,
        val sameNameGroupCount: Int
    )

    /** Stable pool root of a ledger (follows shared-source chains, cycle-safe). */
    fun getAssetGroupRootId(ledgerId: Long): Long = assetPoolRootId(ledgerId)

    /** Ledgers that resolve to the same asset pool as [ledgerId], including itself. */
    fun getAssetGroupMemberIds(ledgerId: Long): List<Long> {
        val root = assetPoolRootId(ledgerId)
        return getLedgers().map { it.id }.filter { assetPoolRootId(it) == root }
    }

    /** Whether [ledgerId] owns the permanent master asset pool. */
    fun isMasterAssetGroup(ledgerId: Long): Boolean =
        assetPoolRootId(ledgerId) == getMasterLedgerId()

    fun getAssetGroups(): List<AssetGroupDescriptor> {
        val ledgers = getLedgers()
        val masterId = getMasterLedgerId()
        val grouped = ledgers.groupBy { assetPoolRootId(it.id) }
        // How many distinct groups share the same owner name; used only to tell
        // otherwise identical group labels apart in the picker.
        val groupNameCounts = HashMap<String, Int>()
        grouped.forEach { (rootId, members) ->
            val name = members.firstOrNull { it.id == rootId }?.name.orEmpty()
            groupNameCounts[name] = (groupNameCounts[name] ?: 0) + 1
        }
        return grouped
            .map { (rootId, members) ->
                val ordered = members.sortedBy { it.id }
                val rootName = ordered.firstOrNull { it.id == rootId }?.name.orEmpty()
                AssetGroupDescriptor(
                    rootLedgerId = rootId,
                    rootLedgerName = rootName,
                    memberLedgerIds = ordered.map { it.id },
                    memberLedgerNames = ordered.map { it.name },
                    isMaster = rootId == masterId,
                    assetCount = getLedgerAssetPoolAssetCount(rootId),
                    sameNameGroupCount = groupNameCounts[rootName] ?: 1
                )
            }
            .sortedWith(compareBy({ !it.isMaster }, { it.rootLedgerId }))
    }

    private fun getLedgerAssetPoolAssetCount(rootId: Long): Int {
        return readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0",
            arrayOf(rootId.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * Creates a ledger that either starts an empty independent asset group
     * ([sharedSourceLedgerId] null) or joins an existing group. Joining stores no
     * asset copies: the ledger references the group root, so the same asset ids
     * and balances are shared. The whole operation is one transaction, so a
     * failure leaves neither a ledger nor a half-written relationship behind.
     */
    fun createLedgerInAssetGroup(
        name: String,
        sharedSourceLedgerId: Long?,
        iconName: String = "tabler_book"
    ): Long? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return null
        val resolvedRoot = sharedSourceLedgerId?.let { sourceId ->
            if (getLedgers().none { it.id == sourceId }) return null
            assetPoolRootId(sourceId)
        }
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val id = db.insertOrThrow(TABLE_LEDGERS, null, ContentValues().apply {
                put(COLUMN_LEDGER_NAME, trimmedName)
                put(COLUMN_LEDGER_SUBTITLE, "${trimmedName}账本")
                put(COLUMN_LEDGER_SORT_ORDER, getLedgers().size)
                put(COLUMN_LEDGER_ICON_NAME, iconName)
            })
            if (resolvedRoot != null && resolvedRoot != id) {
                db.insertOrThrow(TABLE_LEDGER_SHARED_LEDGERS, null, ContentValues().apply {
                    put(COLUMN_SHARED_LEDGER_ID, id)
                    put(COLUMN_SHARED_SOURCE_LEDGER_ID, resolvedRoot)
                })
            }
            db.setTransactionSuccessful()
            id
        } catch (exception: Exception) {
            null
        } finally {
            db.endTransaction()
        }
    }

    enum class LedgerMergeFailure {
        NONE,
        SAME_LEDGER,
        MISSING_LEDGER,
        CROSS_GROUP,
        MASTER_AS_SOURCE
    }

    /** Validates an explicit source → target merge without writing anything. */
    fun validateLedgerMerge(sourceLedgerId: Long, targetLedgerId: Long): LedgerMergeFailure {
        if (sourceLedgerId == targetLedgerId) return LedgerMergeFailure.SAME_LEDGER
        val ledgerIds = getLedgers().map { it.id }.toSet()
        if (sourceLedgerId !in ledgerIds || targetLedgerId !in ledgerIds) {
            return LedgerMergeFailure.MISSING_LEDGER
        }
        if (sourceLedgerId == getMasterLedgerId()) return LedgerMergeFailure.MASTER_AS_SOURCE
        if (assetPoolRootId(sourceLedgerId) != assetPoolRootId(targetLedgerId)) {
            return LedgerMergeFailure.CROSS_GROUP
        }
        return LedgerMergeFailure.NONE
    }

    /**
     * Merges [sourceLedgerId] into [targetLedgerId] in a single transaction.
     *
     * Record ids, dates, amounts, category ids/snapshots, notes, photos and both
     * transfer asset ids are preserved: only the owning ledger changes. Asset ids
     * and balances are untouched because both ledgers already share one asset
     * pool, and global categories are never copied. Survivors of the same pool are
     * re-pointed to the kept root so no shared reference dangles or loops, and a
     * failure rolls the whole merge back.
     */
    fun mergeLedgerInto(sourceLedgerId: Long, targetLedgerId: Long): Boolean {
        if (validateLedgerMerge(sourceLedgerId, targetLedgerId) != LedgerMergeFailure.NONE) {
            return false
        }
        val originalRoot = assetPoolRootId(sourceLedgerId)
        val groupMemberIds = getAssetGroupMemberIds(sourceLedgerId)
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Re-check inside the transaction: the pre-check is not the guarantee.
            if (validateLedgerMerge(sourceLedgerId, targetLedgerId) != LedgerMergeFailure.NONE) {
                db.endTransaction()
                return false
            }
            db.update(
                TABLE_RECORDS,
                ContentValues().apply { put(COLUMN_LEDGER_ID, targetLedgerId) },
                "$COLUMN_LEDGER_ID = ?",
                arrayOf(sourceLedgerId.toString())
            )
            // Asset rows belong to whichever ledger is the pool root. If the source
            // is the root, they move to the target; otherwise they stay where they
            // are and the target simply keeps referencing the same root.
            if (originalRoot == sourceLedgerId) {
                db.update(
                    TABLE_ASSETS,
                    ContentValues().apply { put(COLUMN_LEDGER_ID, targetLedgerId) },
                    "$COLUMN_LEDGER_ID = ?",
                    arrayOf(sourceLedgerId.toString())
                )
            }
            db.delete(
                TABLE_LEDGER_SHARED_LEDGERS,
                "$COLUMN_SHARED_LEDGER_ID = ?",
                arrayOf(sourceLedgerId.toString())
            )
            db.delete(TABLE_LEDGERS, "$COLUMN_ID = ?", arrayOf(sourceLedgerId.toString()))
            repointSharedReferences(db, groupMemberIds, sourceLedgerId, targetLedgerId)
            db.setTransactionSuccessful()
            true
        } catch (exception: Exception) {
            false
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Guarantees every surviving *member of the merged group* references a ledger
     * that still exists, and that the group resolves to exactly one root.
     *
     * Only [groupMemberIds] — captured before the source was removed — are
     * touched. Unrelated groups are never re-pointed, so merging one group cannot
     * change which assets another ledger sees.
     */
    private fun repointSharedReferences(
        db: SQLiteDatabase,
        groupMemberIds: List<Long>,
        removedSourceId: Long,
        keptLedgerId: Long
    ) {
        val survivingIds = getLedgers().map { it.id }.toSet()
        val survivors = groupMemberIds.filter { it != removedSourceId && it in survivingIds }
        if (survivors.isEmpty()) return

        val keptOldRoot = db.rawQuery(
            "SELECT $COLUMN_SHARED_SOURCE_LEDGER_ID FROM $TABLE_LEDGER_SHARED_LEDGERS WHERE $COLUMN_SHARED_LEDGER_ID = ?",
            arrayOf(keptLedgerId.toString())
        ).use { if (it.moveToFirst()) it.getLong(0) else null }

        // The group root is the ledger that owns the asset rows. It only has to
        // change when the removed source *was* the root that the target pointed to;
        // in that case the target now owns the moved rows and is promoted.
        val newRoot = if (keptOldRoot == removedSourceId || keptOldRoot == null) {
            keptLedgerId
        } else if (keptOldRoot in survivingIds) {
            keptOldRoot
        } else {
            keptLedgerId
        }

        // Rewrite the whole group with direct links so no chain can dangle, loop,
        // or resolve to a ledger that no longer exists.
        groupMemberIds.forEach { ledgerId ->
            db.delete(
                TABLE_LEDGER_SHARED_LEDGERS,
                "$COLUMN_SHARED_LEDGER_ID = ?",
                arrayOf(ledgerId.toString())
            )
        }
        survivors.filter { it != newRoot }.forEach { ledgerId ->
            db.insertOrThrow(TABLE_LEDGER_SHARED_LEDGERS, null, ContentValues().apply {
                put(COLUMN_SHARED_LEDGER_ID, ledgerId)
                put(COLUMN_SHARED_SOURCE_LEDGER_ID, newRoot)
            })
        }
    }

    fun getCategoryStatistics(type: Int): Map<String, Double> {
        val categoryStats = mutableMapOf<String, Double>()
        val selectQuery = "SELECT $COLUMN_CATEGORY, SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = ? GROUP BY $COLUMN_CATEGORY"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(currentLedgerId().toString(), type.toString()))

        if (cursor.moveToFirst()) {
            do {
                val category = cursor.getString(0)
                val total = Money.toMajorDouble(cursor.getLong(1))
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
                val total = Money.toMajorDouble(cursor.getLong(1))
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
                val total = Money.toMajorDouble(cursor.getLong(1))
                monthlyStats[month] = total
            } while (cursor.moveToNext())
        }

        cursor.close()
        if (type == 0) {
            val feeCursor = db.rawQuery(
                "SELECT SUBSTR($COLUMN_DATE, 1, 7) as month, SUM($COLUMN_RECORD_FEE) FROM $TABLE_RECORDS " +
                    "WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_TYPE = 2 AND SUBSTR($COLUMN_DATE, 1, 4) = ? GROUP BY month",
                arrayOf(currentLedgerId().toString(), year.toString())
            )
            if (feeCursor.moveToFirst()) {
                do {
                    val month = feeCursor.getString(0)
                    monthlyStats[month] = (monthlyStats[month] ?: 0.0) + Money.toMajorDouble(feeCursor.getLong(1))
                } while (feeCursor.moveToNext())
            }
            feeCursor.close()
        }
        db.close()
        return monthlyStats
    }

    fun addAsset(asset: Asset): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LEDGER_ID, currentLedgerId())
            put(COLUMN_ASSET_NAME, asset.name)
            put(COLUMN_ASSET_AMOUNT, requireNotNull(Money.toMinor(asset.amount)))
            put(COLUMN_ASSET_TYPE, asset.type)
            put(COLUMN_ASSET_CATEGORY_LABEL, asset.categoryLabel)
            put(COLUMN_ASSET_CATEGORY_ICON_NAME, asset.categoryIconName)
            put(COLUMN_ASSET_IS_ARCHIVED, if (asset.isArchived) 1 else 0)
            put(COLUMN_ASSET_IS_PINNED, if (asset.isPinned) 1 else 0)
            put(COLUMN_ASSET_INCLUDE_IN_TOTAL, if (asset.includeInTotal) 1 else 0)
            put(COLUMN_ASSET_SORT_ORDER, nextAssetSortOrder(db))
        }

        val id = db.insert(TABLE_ASSETS, null, values)
        db.close()
        return id
    }

    fun getAllAssets(ledgerId: Long? = null): List<Asset> {
        val assets = mutableListOf<Asset>()
        val selectQuery = if (ledgerId == null) {
            "SELECT * FROM $TABLE_ASSETS WHERE ${assetScope()} AND $COLUMN_ASSET_IS_ARCHIVED = 0 ORDER BY $COLUMN_ASSET_SORT_ORDER, $COLUMN_ASSET_ID"
        } else {
            "SELECT * FROM $TABLE_ASSETS WHERE $COLUMN_LEDGER_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0 ORDER BY $COLUMN_ASSET_SORT_ORDER, $COLUMN_ASSET_ID"
        }

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, ledgerId?.let { arrayOf(it.toString()) })

        if (cursor.moveToFirst()) {
            do {
                val asset = Asset(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ASSET_ID)),
                    ledgerId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LEDGER_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)),
                    amount = Money.toMajorDouble(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ASSET_AMOUNT))),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TYPE)),
                    categoryLabel = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_CATEGORY_LABEL)),
                    categoryIconName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_CATEGORY_ICON_NAME)),
                    isArchived = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_IS_ARCHIVED)) == 1,
                    isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_IS_PINNED)) == 1
                    ,includeInTotal = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_INCLUDE_IN_TOTAL)) == 1,
                    sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_SORT_ORDER))
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
                    amount = Money.toMajorDouble(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ASSET_AMOUNT))),
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
            put(COLUMN_ASSET_AMOUNT, requireNotNull(Money.toMinor(asset.amount)))
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
            put(COLUMN_ASSET_SORT_ORDER, nextAssetSortOrder(db))
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

    fun updateAssetSortOrder(assetIds: List<Long>): Boolean {
        if (assetIds.isEmpty() || assetIds.distinct().size != assetIds.size) return false
        val db = writableDatabase
        db.beginTransaction()
        return try {
            assetIds.forEachIndexed { order, id ->
                val changed = db.update(
                    TABLE_ASSETS,
                    ContentValues().apply { put(COLUMN_ASSET_SORT_ORDER, order) },
                    "${assetScope()} AND $COLUMN_ASSET_ID = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0",
                    arrayOf(id.toString())
                )
                if (changed != 1) return false
            }
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    private fun nextAssetSortOrder(db: SQLiteDatabase): Int = db.rawQuery(
        "SELECT COALESCE(MAX($COLUMN_ASSET_SORT_ORDER), -1) + 1 FROM $TABLE_ASSETS WHERE ${assetScope()}",
        null
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
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

    fun getTotalAssets(): Double = Money.toMajorDouble(getTotalAssetsMinor())

    fun getTotalAssetsMinor(): Long {
        val db = readableDatabase
        val total = db.rawQuery(
            "SELECT COALESCE(SUM($COLUMN_ASSET_AMOUNT), 0) FROM $TABLE_ASSETS " +
                "WHERE ${assetScope()} AND $COLUMN_ASSET_IS_ARCHIVED = 0 AND $COLUMN_ASSET_INCLUDE_IN_TOTAL = 1",
            null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
        db.close()
        return total
    }

    fun addAiChatSession(
        title: String,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = createdAt
    ): Long = aiChatRepository.addSession(title, createdAt, updatedAt)

    fun getAiChatSessions(): List<AiChatSession> = aiChatRepository.getSessions()

    fun getAiChatSessionById(id: Long): AiChatSession? = aiChatRepository.getSessionById(id)

    fun updateAiChatSessionTitle(
        id: Long,
        title: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int = aiChatRepository.updateSessionTitle(id, title, updatedAt)

    fun touchAiChatSession(id: Long, updatedAt: Long = System.currentTimeMillis()): Int =
        aiChatRepository.touchSession(id, updatedAt)

    fun addAiChatMessage(message: AiChatMessage): Long = aiChatRepository.addMessage(message)

    fun getAiChatMessages(sessionId: Long): List<AiChatMessage> = aiChatRepository.getMessages(sessionId)
}
