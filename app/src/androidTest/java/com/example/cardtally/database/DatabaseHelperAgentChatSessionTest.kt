package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperAgentChatSessionTest {

    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("CardTally.db")
        databaseHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseHelper.close()
        context.deleteDatabase("CardTally.db")
    }

    @Test
    fun aiChatSessions_areReturnedByLatestUpdateDescending() {
        val olderId = databaseHelper.addAiChatSession(
            title = "旧会话",
            createdAt = 1_000L,
            updatedAt = 2_000L
        )
        val newerId = databaseHelper.addAiChatSession(
            title = "新会话",
            createdAt = 3_000L,
            updatedAt = 4_000L
        )

        val sessions = databaseHelper.getAiChatSessions()

        assertEquals(listOf(newerId, olderId), sessions.map { it.id })
        assertEquals(listOf("新会话", "旧会话"), sessions.map { it.title })
    }

    @Test
    fun updateAiChatSessionTitle_renamesSessionAndUpdatesTimestamp() {
        val sessionId = databaseHelper.addAiChatSession(
            title = "未命名会话",
            createdAt = 1_000L,
            updatedAt = 1_000L
        )

        val affectedRows = databaseHelper.updateAiChatSessionTitle(
            id = sessionId,
            title = "预算复盘",
            updatedAt = 5_000L
        )
        val session = databaseHelper.getAiChatSessionById(sessionId)

        assertEquals(1, affectedRows)
        assertEquals("预算复盘", session?.title)
        assertEquals(5_000L, session?.updatedAt)
    }

    @Test
    fun aiChatMessages_areScopedToSessionAndOrderedByCreatedTime() {
        val firstSessionId = databaseHelper.addAiChatSession("会话一", createdAt = 1_000L, updatedAt = 1_000L)
        val secondSessionId = databaseHelper.addAiChatSession("会话二", createdAt = 2_000L, updatedAt = 2_000L)

        databaseHelper.addAiChatMessage(
            AiChatMessage(
                sessionId = firstSessionId,
                role = AiChatRole.ASSISTANT,
                content = "第一条",
                createdAt = 3_000L
            )
        )
        databaseHelper.addAiChatMessage(
            AiChatMessage(
                sessionId = secondSessionId,
                role = AiChatRole.USER,
                content = "不应出现在会话一",
                createdAt = 3_500L
            )
        )
        databaseHelper.addAiChatMessage(
            AiChatMessage(
                sessionId = firstSessionId,
                role = AiChatRole.USER,
                content = "第二条",
                createdAt = 4_000L,
                isError = true
            )
        )

        val firstSessionMessages = databaseHelper.getAiChatMessages(firstSessionId)

        assertEquals(listOf("第一条", "第二条"), firstSessionMessages.map { it.content })
        assertEquals(listOf(AiChatRole.ASSISTANT, AiChatRole.USER), firstSessionMessages.map { it.role })
        assertTrue(firstSessionMessages.last().isError)
        assertEquals(listOf(3_000L, 4_000L), firstSessionMessages.map { it.createdAt })
    }

    @Test
    fun aiChatMessage_reasoningRoundTripsAndDefaultsToNull() {
        val sessionId = databaseHelper.addAiChatSession("会话", createdAt = 1_000L, updatedAt = 1_000L)
        databaseHelper.addAiChatMessage(
            AiChatMessage(
                sessionId = sessionId,
                role = AiChatRole.ASSISTANT,
                content = "回答",
                reasoning = "思考内容",
                isLocalOnly = true,
                createdAt = 3_000L
            )
        )
        databaseHelper.addAiChatMessage(
            AiChatMessage(
                sessionId = sessionId,
                role = AiChatRole.USER,
                content = "问题",
                createdAt = 4_000L
            )
        )

        val messages = databaseHelper.getAiChatMessages(sessionId)

        assertEquals("思考内容", messages.first { it.role == AiChatRole.ASSISTANT }.reasoning)
        assertNull(messages.first { it.role == AiChatRole.USER }.reasoning)
        assertTrue(messages.first { it.role == AiChatRole.ASSISTANT }.isLocalOnly)
        org.junit.Assert.assertFalse(messages.first { it.role == AiChatRole.USER }.isLocalOnly)
    }
}
