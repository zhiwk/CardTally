package com.example.cardtally.util;

import android.content.Context;
import android.content.SharedPreferences;

public class QuickAddHelper {
    private static final String PREFS_NAME = "quick_add_prefs";
    private static final String KEY_QUICK_ADD = "quick_add_enabled";

    public static void saveQuickAdd(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_QUICK_ADD, enabled);
        editor.apply();
    }

    public static boolean getQuickAdd(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_QUICK_ADD, false);
    }
}
