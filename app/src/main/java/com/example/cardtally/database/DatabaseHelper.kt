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
        private const val DATABASE_VERSION = 11
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
        private const val COLUMN_ASSET_SOURCE = "asset_source"
        private const val COLUMN_SORT_ORDER = "sort_order"

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

        private const val TABLE_ASSETS = "assets"
        private const val COLUMN_ASSET_ID = "id"
        private const val COLUMN_ASSET_NAME = "name"
        private const val COLUMN_ASSET_AMOUNT = "amount"
        private const val COLUMN_ASSET_TYPE = "type"
        private const val COLUMN_ASSET_IS_ARCHIVED = "is_archived"

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
            "$COLUMN_ASSET_SOURCE TEXT, " +
            "$COLUMN_SORT_ORDER INTEGER DEFAULT 0)"

        private const val CREATE_TABLE_CATEGORIES =
            "CREATE TABLE $TABLE_CATEGORIES (" +
            "$COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_CATEGORY_NAME TEXT NOT NULL, " +
            "$COLUMN_CATEGORY_TYPE INTEGER NOT NULL, " +
            "$COLUMN_CATEGORY_ICON TEXT, " +
            "$COLUMN_CATEGORY_PARENT_ID INTEGER)"

        private const val CREATE_TABLE_ASSETS =
            "CREATE TABLE $TABLE_ASSETS (" +
            "$COLUMN_ASSET_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_ASSET_NAME TEXT NOT NULL, " +
            "$COLUMN_ASSET_AMOUNT REAL NOT NULL, " +
            "$COLUMN_ASSET_TYPE INTEGER NOT NULL, " +
            "$COLUMN_ASSET_IS_ARCHIVED INTEGER DEFAULT 0)"

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
            "$COLUMN_ASSET_SOURCE TEXT, " +
            "$COLUMN_SORT_ORDER INTEGER NOT NULL)"

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

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_RECORDS)
        db.execSQL(CREATE_TABLE_CATEGORIES)
        db.execSQL(CREATE_TABLE_ASSETS)
        db.execSQL(CREATE_TABLE_RECORD_DELETION_UNDO)
        db.execSQL(CREATE_TABLE_AI_CHAT_SESSIONS)
        db.execSQL(CREATE_TABLE_AI_CHAT_MESSAGES)
        db.execSQL(CREATE_INDEX_AI_CHAT_SESSIONS_UPDATED_AT)
        db.execSQL(CREATE_INDEX_AI_CHAT_MESSAGES_SESSION_CREATED_AT)
        insertDefaultCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
    }

    private fun insertDefaultCategories(db: SQLiteDatabase) {
        val expenseCategories = listOf(
            Pair("餐饮", "ic_category_food"),
            Pair("交通", "ic_category_transport"),
            Pair("购物", "ic_category_shopping"),
            Pair("娱乐", "ic_category_entertainment"),
            Pair("医疗", "ic_category_medical"),
            Pair("教育", "ic_category_education"),
            Pair("住房", "ic_category_housing"),
            Pair("其他", "ic_category_other")
        )
        
        val incomeCategories = listOf(
            Pair("工资", "ic_category_salary"),
            Pair("奖金", "ic_category_bonus"),
            Pair("投资", null),
            Pair("兼职", null),
            Pair("其他", "ic_category_other")
        )

        for ((category, icon) in expenseCategories) {
            val values = ContentValues().apply {
                put(COLUMN_CATEGORY_NAME, category)
                put(COLUMN_CATEGORY_TYPE, 0)
                put(COLUMN_CATEGORY_ICON, icon)
            }
            db.insert(TABLE_CATEGORIES, null, values)
        }

        for ((category, icon) in incomeCategories) {
            val values = ContentValues().apply {
                put(COLUMN_CATEGORY_NAME, category)
                put(COLUMN_CATEGORY_TYPE, 1)
                put(COLUMN_CATEGORY_ICON, icon)
            }
            db.insert(TABLE_CATEGORIES, null, values)
        }
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
            put(COLUMN_ASSET_SOURCE, record.assetSource)
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
            assetSource = getNullableString(cursor, COLUMN_ASSET_SOURCE),
            sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SORT_ORDER))
        )
    }

    private fun createCategoryFromCursor(cursor: Cursor): Category {
        return Category(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME)),
            type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE)),
            icon = getNullableString(cursor, COLUMN_CATEGORY_ICON),
            parentId = getNullableLong(cursor, COLUMN_CATEGORY_PARENT_ID)
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
            "SELECT * FROM $TABLE_CATEGORIES WHERE $COLUMN_CATEGORY_TYPE = ? ORDER BY $COLUMN_CATEGORY_NAME COLLATE NOCASE ASC, $COLUMN_CATEGORY_ID ASC",
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
        
        val maxSortOrderQuery = "SELECT MAX($COLUMN_SORT_ORDER) FROM $TABLE_RECORDS WHERE $COLUMN_DATE = ?"
        val cursor = db.rawQuery(maxSortOrderQuery, arrayOf(record.date))
        var maxSortOrder = 0
        if (cursor.moveToFirst()) {
            maxSortOrder = cursor.getInt(0)
        }
        cursor.close()
        
        val values = createRecordValues(record).apply {
            put(COLUMN_SORT_ORDER, maxSortOrder + 1)
        }

        val id = db.insert(TABLE_RECORDS, null, values)
        
        val assetSource = record.assetSource
        if (id != -1L && !assetSource.isNullOrEmpty()) {
            updateAssetAmount(db, assetSource, record.amount, record.type == 1)
        }

        return id
    }

    fun getAllRecords(): List<Record> {
        val records = mutableListOf<Record>()
        val selectQuery = "SELECT * FROM $TABLE_RECORDS ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC"

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

        val rowsAffected = db.update(TABLE_RECORDS, values, "$COLUMN_ID = ?",
            arrayOf(record.id.toString()))
        
        if (rowsAffected > 0 && oldRecord != null) {
            val oldAssetSource = oldRecord.assetSource
            val newAssetSource = record.assetSource
            if (!oldAssetSource.isNullOrEmpty()) {
                updateAssetAmount(db, oldAssetSource, oldRecord.amount, oldRecord.type != 1)
            }
            if (!newAssetSource.isNullOrEmpty()) {
                updateAssetAmount(db, newAssetSource, record.amount, record.type == 1)
            }
        }

        return rowsAffected
    }

    fun getRecordById(id: Long): Record? {
        val db = readableDatabase
        val record = getRecordByIdInternal(db, id)
        return record
    }

    private fun getRecordByIdInternal(db: SQLiteDatabase, id: Long): Record? {
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_ID = ?"
        
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

            val assetSource = record.assetSource
            val requestedBalanceDelta = if (record.type == 1) -record.amount else record.amount
            val appliedBalanceDelta = if (
                assetSource.isNullOrEmpty() ||
                !db.adjustAssetBalance(assetSource, requestedBalanceDelta, activeOnly = true)
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

            db.setTransactionSuccessful()
            return token
        } finally {
            db.endTransaction()
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
            val assetSource = undoEntry.record.assetSource
            if (!assetSource.isNullOrEmpty() && undoEntry.balanceDelta != 0.0) {
                check(db.adjustAssetBalance(assetSource, -undoEntry.balanceDelta, activeOnly = false))
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
            putNullable(COLUMN_ASSET_SOURCE, record.assetSource)
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
        assetName: String,
        delta: Double,
        activeOnly: Boolean
    ): Boolean {
        val archiveClause = if (activeOnly) " AND $COLUMN_ASSET_IS_ARCHIVED = 0" else ""
        val cursor = rawQuery(
            "SELECT 1 FROM $TABLE_ASSETS WHERE $COLUMN_ASSET_NAME = ?$archiveClause LIMIT 1",
            arrayOf(assetName)
        )
        val assetExists = cursor.use { it.moveToFirst() }
        if (!assetExists) {
            return false
        }
        execSQL(
            "UPDATE $TABLE_ASSETS SET $COLUMN_ASSET_AMOUNT = $COLUMN_ASSET_AMOUNT + ? " +
                "WHERE $COLUMN_ASSET_NAME = ?$archiveClause",
            arrayOf(delta, assetName)
        )
        return true
    }

    private fun updateAssetAmount(db: SQLiteDatabase, assetName: String, amount: Double, isAdd: Boolean) {
        val selectQuery = "SELECT $COLUMN_ASSET_AMOUNT FROM $TABLE_ASSETS WHERE $COLUMN_ASSET_NAME = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0"
        val cursor = db.rawQuery(selectQuery, arrayOf(assetName))
        
        if (cursor.moveToFirst()) {
            val currentAmount = cursor.getDouble(0)
            val newAmount = if (isAdd) currentAmount + amount else currentAmount - amount
            
            val values = ContentValues().apply {
                put(COLUMN_ASSET_AMOUNT, newAmount)
            }
            db.update(TABLE_ASSETS, values, "$COLUMN_ASSET_NAME = ? AND $COLUMN_ASSET_IS_ARCHIVED = 0", arrayOf(assetName))
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

        val id = db.insert(TABLE_CATEGORIES, null, values)
        db.close()
        return id
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
        val selectQuery = "SELECT SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_TYPE = ?"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(type.toString()))

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }

        cursor.close()
        db.close()
        return total
    }

    fun getTotalByTypeAndDateRange(type: Int, startDate: String, endDate: String): Double {
        var total = 0.0
        val selectQuery = "SELECT SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_TYPE = ? AND $COLUMN_DATE BETWEEN ? AND ?"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(type.toString(), startDate, endDate))

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }

        cursor.close()
        db.close()
        return total
    }

    fun getRecordsByDateRange(startDate: String, endDate: String): List<Record> {
        val records = mutableListOf<Record>()
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_DATE BETWEEN ? AND ? ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(startDate, endDate))

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
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_ASSET_SOURCE = ? ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC"

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

    fun getCategoryStatistics(type: Int): Map<String, Double> {
        val categoryStats = mutableMapOf<String, Double>()
        val selectQuery = "SELECT $COLUMN_CATEGORY, SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_TYPE = ? GROUP BY $COLUMN_CATEGORY"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(type.toString()))

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
        val selectQuery = "SELECT $COLUMN_CATEGORY, SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_TYPE = ? AND $COLUMN_DATE BETWEEN ? AND ? GROUP BY $COLUMN_CATEGORY"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(type.toString(), startDate, endDate))

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
        val selectQuery = "SELECT SUBSTR($COLUMN_DATE, 1, 7) as month, SUM($COLUMN_AMOUNT) FROM $TABLE_RECORDS WHERE $COLUMN_TYPE = ? AND SUBSTR($COLUMN_DATE, 1, 4) = ? GROUP BY month ORDER BY month"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(type.toString(), year.toString()))

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
            put(COLUMN_ASSET_NAME, asset.name)
            put(COLUMN_ASSET_AMOUNT, asset.amount)
            put(COLUMN_ASSET_TYPE, asset.type)
            put(COLUMN_ASSET_IS_ARCHIVED, if (asset.isArchived) 1 else 0)
        }

        val id = db.insert(TABLE_ASSETS, null, values)
        db.close()
        return id
    }

    fun getAllAssets(): List<Asset> {
        val assets = mutableListOf<Asset>()
        val selectQuery = "SELECT * FROM $TABLE_ASSETS WHERE $COLUMN_ASSET_IS_ARCHIVED = 0 ORDER BY $COLUMN_ASSET_NAME"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val asset = Asset(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ASSET_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ASSET_AMOUNT)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TYPE)),
                    isArchived = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_IS_ARCHIVED)) == 1
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
        val selectQuery = "SELECT * FROM $TABLE_ASSETS WHERE $COLUMN_ASSET_IS_ARCHIVED = 1 ORDER BY $COLUMN_ASSET_NAME"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val asset = Asset(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ASSET_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSET_NAME)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_ASSET_AMOUNT)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_TYPE)),
                    isArchived = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ASSET_IS_ARCHIVED)) == 1
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
        }
        db.update(TABLE_ASSETS, values, "$COLUMN_ASSET_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun unarchiveAsset(id: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ASSET_IS_ARCHIVED, 0)
        }
        db.update(TABLE_ASSETS, values, "$COLUMN_ASSET_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun updateAsset(asset: Asset): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ASSET_NAME, asset.name)
            put(COLUMN_ASSET_AMOUNT, asset.amount)
            put(COLUMN_ASSET_TYPE, asset.type)
            put(COLUMN_ASSET_IS_ARCHIVED, if (asset.isArchived) 1 else 0)
        }

        val rowsAffected = db.update(TABLE_ASSETS, values, "$COLUMN_ASSET_ID = ?",
            arrayOf(asset.id.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteArchivedAsset(id: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.rawQuery(
                "SELECT $COLUMN_ASSET_NAME, $COLUMN_ASSET_IS_ARCHIVED FROM $TABLE_ASSETS " +
                    "WHERE $COLUMN_ASSET_ID = ?",
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

            val deleted = db.delete(TABLE_ASSETS, "$COLUMN_ASSET_ID = ?", arrayOf(id.toString())) == 1
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
        val selectQuery = "SELECT SUM($COLUMN_ASSET_AMOUNT) FROM $TABLE_ASSETS WHERE $COLUMN_ASSET_IS_ARCHIVED = 0"

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
