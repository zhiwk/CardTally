package com.example.cardtally.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_RECORDS = "records"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_DESCRIPTION = "description"

        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_CATEGORY_ID = "id"
        private const val COLUMN_CATEGORY_NAME = "name"
        private const val COLUMN_CATEGORY_TYPE = "type"

        private const val CREATE_TABLE_RECORDS =
            "CREATE TABLE $TABLE_RECORDS (" +
            "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_DATE TEXT NOT NULL, " +
            "$COLUMN_AMOUNT REAL NOT NULL, " +
            "$COLUMN_CATEGORY TEXT NOT NULL, " +
            "$COLUMN_TYPE INTEGER NOT NULL, " +
            "$COLUMN_DESCRIPTION TEXT)"

        private const val CREATE_TABLE_CATEGORIES =
            "CREATE TABLE $TABLE_CATEGORIES (" +
            "$COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COLUMN_CATEGORY_NAME TEXT NOT NULL, " +
            "$COLUMN_CATEGORY_TYPE INTEGER NOT NULL)"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_RECORDS)
        db.execSQL(CREATE_TABLE_CATEGORIES)
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
    }

    private fun insertDefaultCategories(db: SQLiteDatabase) {
        val expenseCategories = arrayOf("餐饮", "交通", "购物", "娱乐", "医疗", "教育", "住房", "其他")
        val incomeCategories = arrayOf("工资", "奖金", "投资", "兼职", "其他")

        for (category in expenseCategories) {
            val values = ContentValues().apply {
                put(COLUMN_CATEGORY_NAME, category)
                put(COLUMN_CATEGORY_TYPE, 0)
            }
            db.insert(TABLE_CATEGORIES, null, values)
        }

        for (category in incomeCategories) {
            val values = ContentValues().apply {
                put(COLUMN_CATEGORY_NAME, category)
                put(COLUMN_CATEGORY_TYPE, 1)
            }
            db.insert(TABLE_CATEGORIES, null, values)
        }
    }

    fun addRecord(record: Record): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, record.date)
            put(COLUMN_AMOUNT, record.amount)
            put(COLUMN_CATEGORY, record.category)
            put(COLUMN_TYPE, record.type)
            put(COLUMN_DESCRIPTION, record.description)
        }

        val id = db.insert(TABLE_RECORDS, null, values)
        db.close()
        return id
    }

    fun getAllRecords(): List<Record> {
        val records = mutableListOf<Record>()
        val selectQuery = "SELECT * FROM $TABLE_RECORDS ORDER BY $COLUMN_DATE DESC"

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
                    description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION))
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
        val values = ContentValues().apply {
            put(COLUMN_DATE, record.date)
            put(COLUMN_AMOUNT, record.amount)
            put(COLUMN_CATEGORY, record.category)
            put(COLUMN_TYPE, record.type)
            put(COLUMN_DESCRIPTION, record.description)
        }

        val rowsAffected = db.update(TABLE_RECORDS, values, "$COLUMN_ID = ?",
            arrayOf(record.id.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteRecord(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_RECORDS, "$COLUMN_ID = ?", arrayOf(id.toString()))
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
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE))
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
                    type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE))
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
        val selectQuery = "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_DATE BETWEEN ? AND ? ORDER BY $COLUMN_DATE DESC"

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
                    description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION))
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
}
