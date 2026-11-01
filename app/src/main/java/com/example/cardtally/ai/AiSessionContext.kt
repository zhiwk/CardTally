package com.example.cardtally.ai

import com.example.cardtally.model.AiChatMessage

/** Text history only: never restore reasoning, tool calls or native execution authority. */
object AiSessionContext {
    const val MAX_MESSAGES = 80
    const val MAX_CHARACTERS = 48_000

    fun history(messages: List<AiChatMessage>, sessionId: Long,
                characterBudget: Int = MAX_CHARACTERS,
                messageBudget: Int = MAX_MESSAGES): List<AiChatMessage> {
        val selected = mutableListOf<AiChatMessage>()
        var size = 0
        for (message in messages.asReversed()) {
            if (message.sessionId != sessionId || message.isError || message.content.isBlank()) continue
            if (selected.size >= messageBudget || size + message.content.length > characterBudget) break
            selected.add(message.copy(reasoning = null, isLocalOnly = false))
            size += message.content.length
        }
        return selected.asReversed().dropWhile { it.role != com.example.cardtally.model.AiChatRole.USER }
    }
}
