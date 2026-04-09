package com.example.cardtally.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import com.example.cardtally.model.AiChatSession
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val DATABASE_VERSION = 9

        private const val TABLE_RECORDS = "records"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_ASSET_SOURCE = "asset_source"
        private const val COLUMN_SORT_ORDER = "sort_order"

        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_CATEGORY_ID = "id"
        private const val COLUMN_CATEGORY_NAME = "name"
        private const val COLUMN_CATEGORY_TYPE = "type"
        private const val COLUMN_CATEGORY_ICON = "icon"

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
            "$COLUMN_TYPE INTEGER NOT NULL, " +
            "$COLUMN_DESCRIPTION TEXT, " +
            "$COLUMN_ASSET_SOURCE TEXT, " +
            "$COLUMN_SORT_ORDER INTEGER DEFAULT 0)"

        private const val CREATE_TABLE_CATEGORIES =
            "CREATE TABLE $TABLE_CATEGORIES (" +
            "$COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_CATEGORY_NAME TEXT NOT NULL, " +
            "$COLUMN_CATEGORY_TYPE INTEGER NOT NULL, " +
            "$COLUMN_CATEGORY_ICON TEXT)"

        private const val CREATE_TABLE_ASSETS =
            "CREATE TABLE $TABLE_ASSETS (" +
            "$COLUMN_ASSET_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_ASSET_NAME TEXT NOT NULL, " +
            "$COLUMN_ASSET_AMOUNT REAL NOT NULL, " +
            "$COLUMN_ASSET_TYPE INTEGER NOT NULL, " +
            "$COLUMN_ASSET_IS_ARCHIVED INTEGER DEFAULT 0)"

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

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_RECORDS)
        db.execSQL(CREATE_TABLE_CATEGORIES)
        db.execSQL(CREATE_TABLE_ASSETS)
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

    fun addRecord(record: Record): Long {
        val db = writableDatabase
        
        val maxSortOrderQuery = "SELECT MAX($COLUMN_SORT_ORDER) FROM $TABLE_RECORDS WHERE $COLUMN_DATE = ?"
        val cursor = db.rawQuery(maxSortOrderQuery, arrayOf(record.date))
        var maxSortOrder = 0
        if (cursor.moveToFirst()) {
            maxSortOrder = cursor.getInt(0)
        }
        cursor.close()
        
        val values = ContentValues().apply {
            put(COLUMN_DATE, record.date)
            put(COLUMN_AMOUNT, record.amount)
            put(COLUMN_CATEGORY, record.category)
            put(COLUMN_TYPE, record.type)
            put(COLUMN_DESCRIPTION, record.description)
            put(COLUMN_ASSET_SOURCE, record.assetSource)
            put(COLUMN_SORT_ORDER, maxSortOrder + 1)
        }

        val id = db.insert(TABLE_RECORDS, null, values)
        
        val assetSource = record.assetSource
        if (id != -1L && !assetSource.isNullOrEmpty()) {
            updateAssetAmount(db, assetSource, record.amount, record.type == 1)
        }
        
        db.close()
        return id
    }

    fun getAllRecords(): List<Record> {
        val records = mutableListOf<Record>()
        val selectQuery = "SELECT * FROM $TABLE_RECORDS ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val record = Record(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                    description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                    assetSource = cursor.getString(cursor.getColumnIndex(COLUMN_ASSET_SOURCE)),
                    sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SORT_ORDER))
                )
                records.add(record)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return records
    }

    fun updateRecord(record: Record): Int {
        val db = writableDatabase
        
        val oldRecord = getRecordByIdInternal(db, record.id)
        
        val values = ContentValues().apply {
            put(COLUMN_DATE, record.date)
            put(COLUMN_AMOUNT, record.amount)
            put(COLUMN_CATEGORY, record.category)
            put(COLUMN_TYPE, record.type)
            put(COLUMN_DESCRIPTION, record.description)
            put(COLUMN_ASSET_SOURCE, record.assetSource)
        }

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
        
        db.close()
        return rowsAffected
    }

    fun getRecordById(id: Long): Record? {
        val db = readableDatabase
        val record = getRecordByIdInternal(db, id)
        db.close()
        return record
    }

    private fun getRecordByIdInternal(db: SQLiteDatabase, id: Long): Record? {
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_ID = ?"
        
        val cursor = db.rawQuery(selectQuery, arrayOf(id.toString()))
        
        var record: Record? = null
        if (cursor.moveToFirst()) {
            record = Record(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                assetSource = cursor.getString(cursor.getColumnIndex(COLUMN_ASSET_SOURCE)),
                sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SORT_ORDER))
            )
        }
        
        cursor.close()
        return record
    }

    fun deleteRecord(id: Long) {
        val db = writableDatabase
        
        val record = getRecordByIdInternal(db, id)
        
        db.delete(TABLE_RECORDS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        
        val assetSource = record?.assetSource
        if (record != null && !assetSource.isNullOrEmpty()) {
            updateAssetAmount(db, assetSource, record.amount, record.type != 1)
        }
        
        db.close()
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
        db.close()
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
        db.close()
    }

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun addCategory(category: Category): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, category.name)
            put(COLUMN_CATEGORY_TYPE, category.type)
            put(COLUMN_CATEGORY_ICON, category.icon)
        }

        val id = db.insert(TABLE_CATEGORIES, null, values)
        db.close()
        return id
    }

    fun getCategoriesByType(type: Int): List<Category> {
        val categories = mutableListOf<Category>()
        val selectQuery = "SELECT * FROM $TABLE_CATEGORIES WHERE $COLUMN_CATEGORY_TYPE = ?"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(type.toString()))

        if (cursor.moveToFirst()) {
            do {
                val category = Category(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE)),
                    icon = cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_ICON))
                )
                categories.add(category)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return categories
    }

    fun getAllCategories(): List<Category> {
        val categories = mutableListOf<Category>()
        val selectQuery = "SELECT * FROM $TABLE_CATEGORIES ORDER BY $COLUMN_CATEGORY_TYPE, $COLUMN_CATEGORY_NAME"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor.moveToFirst()) {
            do {
                val category = Category(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE)),
                    icon = cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_ICON))
                )
                categories.add(category)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return categories
    }

    fun updateCategory(category: Category): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, category.name)
            put(COLUMN_CATEGORY_TYPE, category.type)
            put(COLUMN_CATEGORY_ICON, category.icon)
        }

        val rowsAffected = db.update(TABLE_CATEGORIES, values, "$COLUMN_CATEGORY_ID = ?",
            arrayOf(category.id.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteCategory(id: Long) {
        val db = writableDatabase
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
                val record = Record(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                    description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                    assetSource = cursor.getString(cursor.getColumnIndex(COLUMN_ASSET_SOURCE)),
                    sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SORT_ORDER))
                )
                records.add(record)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return records
    }

    fun getRecordsByAssetSource(assetSource: String): List<Record> {
        val records = mutableListOf<Record>()
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_ASSET_SOURCE = ? ORDER BY $COLUMN_DATE DESC, $COLUMN_SORT_ORDER ASC"

        val db = readableDatabase
        val cursor = db.rawQuery(selectQuery, arrayOf(assetSource))

        if (cursor.moveToFirst()) {
            do {
                val record = Record(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                    description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION)),
                    assetSource = cursor.getString(cursor.getColumnIndex(COLUMN_ASSET_SOURCE)),
                    sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SORT_ORDER))
                )
                records.add(record)
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

    fun deleteAsset(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_ASSETS, "$COLUMN_ASSET_ID = ?", arrayOf(id.toString()))
        db.close()
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
