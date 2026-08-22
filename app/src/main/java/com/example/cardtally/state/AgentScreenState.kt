package com.example.cardtally.state

import android.os.Bundle

data class AgentScreenState(
    val activeSessionId: Long,
    val composerText: String,
    val isSessionDrawerOpen: Boolean,
    val messageScrollPosition: Int,
    val messageScrollOffset: Int,
    val sessionScrollPosition: Int,
    val sessionScrollOffset: Int
) {
    fun writeTo(bundle: Bundle) {
        bundle.putLong(KEY_ACTIVE_SESSION_ID, activeSessionId)
        bundle.putString(KEY_COMPOSER_TEXT, composerText)
        bundle.putBoolean(KEY_DRAWER_OPEN, isSessionDrawerOpen)
        bundle.putInt(KEY_MESSAGE_SCROLL_POSITION, messageScrollPosition)
        bundle.putInt(KEY_MESSAGE_SCROLL_OFFSET, messageScrollOffset)
        bundle.putInt(KEY_SESSION_SCROLL_POSITION, sessionScrollPosition)
        bundle.putInt(KEY_SESSION_SCROLL_OFFSET, sessionScrollOffset)
    }

    companion object {
        private const val KEY_ACTIVE_SESSION_ID = "state_active_session_id"
        private const val KEY_COMPOSER_TEXT = "state_composer_text"
        private const val KEY_DRAWER_OPEN = "state_is_session_drawer_open"
        private const val KEY_MESSAGE_SCROLL_POSITION = "state_message_scroll_position"
        private const val KEY_MESSAGE_SCROLL_OFFSET = "state_message_scroll_offset"
        private const val KEY_SESSION_SCROLL_POSITION = "state_session_scroll_position"
        private const val KEY_SESSION_SCROLL_OFFSET = "state_session_scroll_offset"

        fun readFrom(bundle: Bundle?): AgentScreenState {
            if (bundle == null) return AgentScreenState(0L, "", false, 0, 0, 0, 0)
            return AgentScreenState(
                bundle.getLong(KEY_ACTIVE_SESSION_ID),
                bundle.getString(KEY_COMPOSER_TEXT, ""),
                bundle.getBoolean(KEY_DRAWER_OPEN),
                bundle.getInt(KEY_MESSAGE_SCROLL_POSITION).coerceAtLeast(0),
                bundle.getInt(KEY_MESSAGE_SCROLL_OFFSET),
                bundle.getInt(KEY_SESSION_SCROLL_POSITION).coerceAtLeast(0),
                bundle.getInt(KEY_SESSION_SCROLL_OFFSET)
            )
        }
    }
}
