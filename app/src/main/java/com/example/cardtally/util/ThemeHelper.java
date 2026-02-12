package com.example.cardtally.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.cardtally.R;

public class ThemeHelper {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "theme_mode";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    public static void saveTheme(Context context, int themeMode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_THEME, themeMode);
        editor.apply();
    }

    public static int getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_LIGHT);
    }

    public static int getThemeResId(int themeMode) {
        switch (themeMode) {
            case THEME_DARK:
                return R.style.Theme_CardTally_Dark;
            case THEME_SYSTEM:
                return R.style.Theme_CardTally_System;
            case THEME_LIGHT:
            default:
                return R.style.Theme_CardTally_Light;
        }
    }

    public static String getThemeName(Context context, int themeMode) {
        String[] themeNames = context.getResources().getStringArray(R.array.theme_names);
        return themeNames[themeMode];
    }
}
