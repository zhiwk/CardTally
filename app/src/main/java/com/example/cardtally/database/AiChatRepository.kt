package com.example.cardtally.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import com.example.cardtally.model.AiChatSession

/** Local persistence boundary for AI chat sessions and messages. */
internal class AiChatRepository(
    private val readableDatabase: () -> SQLiteDatabase,
    private val writableDatabase: () -> SQLiteDatabase,
    private val columns: Columns
) {
    data class Columns(
        val sessionsTable: String,
        val sessionId: String,
        val sessionTitle: String,
        val sessionCreatedAt: String,
        val sessionUpdatedAt: String,
        val messagesTable: String,
        val messageId: String,
        val messageSessionId: String,
        val messageRole: String,
        val messageContent: String,
        val messageReasoning: String,
        val messageIsError: String,
        val messageCreatedAt: String
    )

    fun addSession(title: String, createdAt: Long, updatedAt: Long): Long {
        val values = ContentValues().apply {
            put(columns.sessionTitle, title)
            put(columns.sessionCreatedAt, createdAt)
            put(columns.sessionUpdatedAt, updatedAt)
        }
        return writableDatabase().insert(columns.sessionsTable, null, values)
    }

    fun getSessions(): List<AiChatSession> = readableDatabase().rawQuery(
        "SELECT * FROM ${columns.sessionsTable} ORDER BY ${columns.sessionUpdatedAt} DESC, ${columns.sessionId} DESC",
        null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(cursor.toSession())
        }
    }

    fun getSessionById(id: Long): AiChatSession? = readableDatabase().rawQuery(
        "SELECT * FROM ${columns.sessionsTable} WHERE ${columns.sessionId} = ?",
        arrayOf(id.toString())
    ).use { cursor -> if (cursor.moveToFirst()) cursor.toSession() else null }

    fun updateSessionTitle(id: Long, title: String, updatedAt: Long): Int = writableDatabase().update(
        columns.sessionsTable,
        ContentValues().apply {
            put(columns.sessionTitle, title)
            put(columns.sessionUpdatedAt, updatedAt)
        },
        "${columns.sessionId} = ?",
        arrayOf(id.toString())
    )

    fun touchSession(id: Long, updatedAt: Long): Int = writableDatabase().update(
        columns.sessionsTable,
        ContentValues().apply { put(columns.sessionUpdatedAt, updatedAt) },
        "${columns.sessionId} = ?",
        arrayOf(id.toString())
    )

    fun addMessage(message: AiChatMessage): Long {
        val createdAt = message.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val db = writableDatabase()
        val id = db.insert(
            columns.messagesTable,
            null,
            ContentValues().apply {
                put(columns.messageSessionId, message.sessionId)
                put(columns.messageRole, message.role.apiValue)
                put(columns.messageContent, message.content)
                put(columns.messageReasoning, message.reasoning)
                put(columns.messageIsError, if (message.isError) 1 else 0)
                put(columns.messageCreatedAt, createdAt)
            }
        )
        if (message.sessionId > 0L) {
            db.update(
                columns.sessionsTable,
                ContentValues().apply { put(columns.sessionUpdatedAt, createdAt) },
                "${columns.sessionId} = ?",
                arrayOf(message.sessionId.toString())
            )
        }
        return id
    }

    fun getMessages(sessionId: Long): List<AiChatMessage> = readableDatabase().rawQuery(
        "SELECT * FROM ${columns.messagesTable} WHERE ${columns.messageSessionId} = ? " +
            "ORDER BY ${columns.messageCreatedAt} ASC, ${columns.messageId} ASC",
        arrayOf(sessionId.toString())
    ).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.toMessage()) }
    }

    private fun Cursor.toSession() = AiChatSession(
        id = getLong(column(columns.sessionId)),
        title = getString(column(columns.sessionTitle)),
        createdAt = getLong(column(columns.sessionCreatedAt)),
        updatedAt = getLong(column(columns.sessionUpdatedAt))
    )

    private fun Cursor.toMessage() = AiChatMessage(
        id = getLong(column(columns.messageId)),
        sessionId = getLong(column(columns.messageSessionId)),
        role = AiChatRole.values().firstOrNull { it.apiValue == getString(column(columns.messageRole)) }
            ?: AiChatRole.ASSISTANT,
        content = getString(column(columns.messageContent)),
        reasoning = getString(column(columns.messageReasoning)),
        isError = getInt(column(columns.messageIsError)) == 1,
        createdAt = getLong(column(columns.messageCreatedAt))
    )

    private fun Cursor.column(name: String) = getColumnIndexOrThrow(name)
}
