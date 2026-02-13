package com.example.cardtally.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.cardtally.model.Category;
import com.example.cardtally.model.Record;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "CardTally.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_RECORDS = "records";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_DESCRIPTION = "description";

    private static final String TABLE_CATEGORIES = "categories";
    private static final String COLUMN_CATEGORY_ID = "id";
    private static final String COLUMN_CATEGORY_NAME = "name";
    private static final String COLUMN_CATEGORY_TYPE = "type";

    private static final String CREATE_TABLE_RECORDS =
            "CREATE TABLE " + TABLE_RECORDS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_DATE + " TEXT NOT NULL, " +
            COLUMN_AMOUNT + " REAL NOT NULL, " +
            COLUMN_CATEGORY + " TEXT NOT NULL, " +
            COLUMN_TYPE + " INTEGER NOT NULL, " +
            COLUMN_DESCRIPTION + " TEXT)";

    private static final String CREATE_TABLE_CATEGORIES =
            "CREATE TABLE " + TABLE_CATEGORIES + " (" +
            COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_CATEGORY_NAME + " TEXT NOT NULL, " +
            COLUMN_CATEGORY_TYPE + " INTEGER NOT NULL)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_RECORDS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        insertDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_CATEGORIES);
            insertDefaultCategories(db);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_DESCRIPTION + " TEXT");
        }
    }

    private void insertDefaultCategories(SQLiteDatabase db) {
        String[] expenseCategories = {"餐饮", "交通", "购物", "娱乐", "医疗", "教育", "住房", "其他"};
        String[] incomeCategories = {"工资", "奖金", "投资", "兼职", "其他"};

        for (String category : expenseCategories) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CATEGORY_NAME, category);
            values.put(COLUMN_CATEGORY_TYPE, 0);
            db.insert(TABLE_CATEGORIES, null, values);
        }

        for (String category : incomeCategories) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CATEGORY_NAME, category);
            values.put(COLUMN_CATEGORY_TYPE, 1);
            db.insert(TABLE_CATEGORIES, null, values);
        }
    }

    public long addRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, record.getDate());
        values.put(COLUMN_AMOUNT, record.getAmount());
        values.put(COLUMN_CATEGORY, record.getCategory());
        values.put(COLUMN_TYPE, record.getType());
        values.put(COLUMN_DESCRIPTION, record.getDescription());

        long id = db.insert(TABLE_RECORDS, null, values);
        db.close();
        return id;
    }

    public List<Record> getAllRecords() {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_RECORDS + " ORDER BY " + COLUMN_DATE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Record record = new Record();
                record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                record.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                record.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                record.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                record.setType(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
                
                int descIndex = cursor.getColumnIndex(COLUMN_DESCRIPTION);
                if (descIndex != -1) {
                    record.setDescription(cursor.getString(descIndex));
                }

                records.add(record);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return records;
    }

    public int updateRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, record.getDate());
        values.put(COLUMN_AMOUNT, record.getAmount());
        values.put(COLUMN_CATEGORY, record.getCategory());
        values.put(COLUMN_TYPE, record.getType());

        int rowsAffected = db.update(TABLE_RECORDS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(record.getId())});
        db.close();
        return rowsAffected;
    }

    public void deleteRecord(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECORDS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    public long addCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, category.getName());
        values.put(COLUMN_CATEGORY_TYPE, category.getType());

        long id = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return id;
    }

    public List<Category> getCategoriesByType(int type) {
        List<Category> categories = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CATEGORIES + " WHERE " + COLUMN_CATEGORY_TYPE + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(type)});

        if (cursor.moveToFirst()) {
            do {
                Category category = new Category();
                category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME)));
                category.setType(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE)));

                categories.add(category);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return categories;
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + COLUMN_CATEGORY_TYPE + ", " + COLUMN_CATEGORY_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Category category = new Category();
                category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID)));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME)));
                category.setType(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE)));

                categories.add(category);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return categories;
    }

    public int updateCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, category.getName());
        values.put(COLUMN_CATEGORY_TYPE, category.getType());

        int rowsAffected = db.update(TABLE_CATEGORIES, values, COLUMN_CATEGORY_ID + " = ?",
                new String[]{String.valueOf(category.getId())});
        db.close();
        return rowsAffected;
    }

    public void deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CATEGORIES, COLUMN_CATEGORY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public double getTotalByType(int type) {
        double total = 0;
        String selectQuery = "SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_RECORDS + " WHERE " + COLUMN_TYPE + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(type)});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        db.close();
        return total;
    }

    public double getTotalByTypeAndDateRange(int type, String startDate, String endDate) {
        double total = 0;
        String selectQuery = "SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + TABLE_RECORDS + 
                " WHERE " + COLUMN_TYPE + " = ? AND " + COLUMN_DATE + " BETWEEN ? AND ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(type), startDate, endDate});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        db.close();
        return total;
    }

    public List<Record> getRecordsByDateRange(String startDate, String endDate) {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_RECORDS + 
                " WHERE " + COLUMN_DATE + " BETWEEN ? AND ? ORDER BY " + COLUMN_DATE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{startDate, endDate});

        if (cursor.moveToFirst()) {
            do {
                Record record = new Record();
                record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                record.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                record.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                record.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)));
                record.setType(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));

                records.add(record);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return records;
    }

    public java.util.Map<String, Double> getCategoryStatistics(int type) {
        java.util.Map<String, Double> categoryStats = new java.util.HashMap<>();
        String selectQuery = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") FROM " + 
                TABLE_RECORDS + " WHERE " + COLUMN_TYPE + " = ? GROUP BY " + COLUMN_CATEGORY;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(type)});

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                double total = cursor.getDouble(1);
                categoryStats.put(category, total);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return categoryStats;
    }

    public java.util.Map<String, Double> getCategoryStatisticsByDateRange(int type, String startDate, String endDate) {
        java.util.Map<String, Double> categoryStats = new java.util.HashMap<>();
        String selectQuery = "SELECT " + COLUMN_CATEGORY + ", SUM(" + COLUMN_AMOUNT + ") FROM " + 
                TABLE_RECORDS + " WHERE " + COLUMN_TYPE + " = ? AND " + COLUMN_DATE + 
                " BETWEEN ? AND ? GROUP BY " + COLUMN_CATEGORY;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(type), startDate, endDate});

        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                double total = cursor.getDouble(1);
                categoryStats.put(category, total);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return categoryStats;
    }

    public java.util.Map<String, Double> getMonthlyStatistics(int type, int year) {
        java.util.Map<String, Double> monthlyStats = new java.util.HashMap<>();
        String selectQuery = "SELECT SUBSTR(" + COLUMN_DATE + ", 1, 7) as month, SUM(" + COLUMN_AMOUNT + ") FROM " + 
                TABLE_RECORDS + " WHERE " + COLUMN_TYPE + " = ? AND SUBSTR(" + COLUMN_DATE + ", 1, 4) = ?" +
                " GROUP BY month ORDER BY month";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(type), String.valueOf(year)});

        if (cursor.moveToFirst()) {
            do {
                String month = cursor.getString(0);
                double total = cursor.getDouble(1);
                monthlyStats.put(month, total);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return monthlyStats;
    }
}
