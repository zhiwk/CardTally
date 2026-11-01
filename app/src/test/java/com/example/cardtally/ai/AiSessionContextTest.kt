package com.example.cardtally.ai

import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import org.junit.Assert.*
import org.junit.Test

class AiSessionContextTest {
    private fun user(text: String, session: Long = 1) =
        AiChatMessage(sessionId = session, role = AiChatRole.USER, content = text)

    @Test fun restoredFinancialHistoryIsTextOnlyAndSessionScoped() {
        val history = listOf(user("记午餐"),
            AiChatMessage(sessionId = 1, role = AiChatRole.ASSISTANT, content = "已记录",
                reasoning = "不应外发", isLocalOnly = true),
            user("其他会话", 2), user("错误").copy(isError = true),
            AiChatMessage(role = AiChatRole.ASSISTANT, content = "未完成"))
        val restored = AiSessionContext.history(history, 1)
        assertEquals(listOf("记午餐", "已记录"), restored.map { it.content })
        assertTrue(restored.all { it.reasoning == null && !it.isLocalOnly && it.sessionId == 1L })
        assertEquals(restored, AiSessionContext.history(history, 1)) // Re-entry/API changes have no separate cache.
    }

    @Test fun characterBudgetKeepsRecentMessagesWithoutCuttingText() {
        val restored = AiSessionContext.history(listOf(user("早期消息"), user("最近")), 1, 2)
        assertEquals(listOf("最近"), restored.map { it.content })
        assertTrue(AiSessionContext.history(listOf(user("太长")), 1, 1).isEmpty())
    }

    @Test fun countLimitNeverLeaksOtherSessions() {
        val restored = AiSessionContext.history((1..100).map { user("$it") } + user("其他", 2), 1)
        assertEquals(AiSessionContext.MAX_MESSAGES, restored.size)
        assertEquals("100", restored.last().content)
    }
}
