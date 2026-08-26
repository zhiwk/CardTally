package com.example.cardtally.util

import android.content.Context

object RecordPhotoSettingsHelper {
    private const val PREFS = "record_photo_settings"
    private const val KEY_MAX_PHOTOS = "max_photos_per_record"
    private const val DEFAULT_MAX_PHOTOS = 3

    fun getMaxPhotos(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MAX_PHOTOS, DEFAULT_MAX_PHOTOS)
            .coerceIn(1, 9)
    }

    fun saveMaxPhotos(context: Context, value: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MAX_PHOTOS, value.coerceIn(1, 9))
            .apply()
    }
}
