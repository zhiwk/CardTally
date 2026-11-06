package com.example.cardtally.util

import android.content.Context

object BottomNavigationPreferences {
    private const val NAME = "bottom_navigation_prefs"
    fun getMenuOrder(context: Context): List<BottomNavigationDestination> = BottomNavigationOrder.decode(
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).all["item_order"]
    )
    fun getVisibleMenuOrder(context: Context): List<BottomNavigationDestination> = getMenuOrder(context).filter {
        when (it) {
            BottomNavigationDestination.ASSETS -> AssetDisplayHelper.getShowAsset(context)
            BottomNavigationDestination.AGENT -> AiAssistantSettingsHelper.getAiAssistantEnabled(context) &&
                getAiEntryStyle(context) == AiEntryStyle.IN_MENU
            else -> true
        }
    }
    fun saveMenuOrder(context: Context, order: List<BottomNavigationDestination>) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("item_order", BottomNavigationOrder.encode(order)).apply()
    }
    fun saveVisibleMenuOrder(context: Context, order: List<BottomNavigationDestination>) {
        saveMenuOrder(context, BottomNavigationOrder.mergeVisible(getMenuOrder(context), order))
    }
    fun resetMenuOrder(context: Context) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().remove("item_order").apply()
    }
    fun getAiEntryStyle(context: Context): AiEntryStyle = AiEntryStyle.fromPreference(
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).all["ai_entry_style"]
    )
    fun saveAiEntryStyle(context: Context, style: AiEntryStyle) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("ai_entry_style", style.preferenceValue).apply()
    }
    fun getAiEntryPosition(context: Context): AiEntryPosition = AiEntryPosition.fromPreference(
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).all["ai_entry_position"]
    )
    fun saveAiEntryPosition(context: Context, position: AiEntryPosition) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("ai_entry_position", position.preferenceValue).apply()
    }
    fun isDocked(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).all["style"] == "docked"
    fun saveDocked(context: Context, docked: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString("style", if (docked) "docked" else "floating").apply()
    }
}
