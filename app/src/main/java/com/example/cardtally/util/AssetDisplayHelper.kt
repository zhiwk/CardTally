package com.example.cardtally.util

import android.content.Context

object AssetDisplayHelper {
    private const val PREFS_NAME = "asset_display_prefs"
    private const val KEY_SHOW_ASSET = "show_asset_enabled"

    fun saveShowAsset(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_ASSET, enabled).apply()
    }

    fun getShowAsset(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_ASSET, false)
    }
}
