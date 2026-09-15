package com.example.cardtally.util

import android.content.Context

/** Stores the optional asset defaults used when starting expense/income entries. */
object DefaultRecordAssetPreferences {
    private const val PREFS_NAME = "default_record_asset_prefs"
    private const val EXPENSE_ID = "default_expense_asset_id"
    private const val INCOME_ID = "default_income_asset_id"

    fun getExpenseAssetId(context: Context): Long? = get(context, EXPENSE_ID)

    fun getIncomeAssetId(context: Context): Long? = get(context, INCOME_ID)

    fun saveExpenseAssetId(context: Context, assetId: Long?) = save(context, EXPENSE_ID, assetId)

    fun saveIncomeAssetId(context: Context, assetId: Long?) = save(context, INCOME_ID, assetId)

    private fun get(context: Context, key: String): Long? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(key, 0L)
            .takeIf { it > 0L }

    private fun save(context: Context, key: String, assetId: Long?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(key, assetId ?: 0L)
            .apply()
    }
}
