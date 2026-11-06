package com.example.cardtally.util

enum class AiEntryStyle(val preferenceValue: String) {
    IN_MENU("menu"),
    SEPARATE_CIRCLE("circle");

    companion object {
        fun fromPreference(value: Any?): AiEntryStyle =
            entries.firstOrNull { it.preferenceValue == value } ?: SEPARATE_CIRCLE
    }
}
