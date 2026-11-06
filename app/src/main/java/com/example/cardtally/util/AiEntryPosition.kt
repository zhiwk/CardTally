package com.example.cardtally.util

enum class AiEntryPosition(val preferenceValue: String) {
    LEFT("left"),
    RIGHT("right");

    companion object {
        fun fromPreference(value: Any?): AiEntryPosition =
            entries.firstOrNull { it.preferenceValue == value } ?: LEFT
    }
}
