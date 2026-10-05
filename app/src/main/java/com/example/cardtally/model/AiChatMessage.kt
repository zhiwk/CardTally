package com.example.cardtally.model

enum class AiChatRole(val apiValue: String) {
    USER("user"),
    ASSISTANT("assistant")
}

data class AiChatMessage(
    val id: Long = 0L,
    val sessionId: Long = 0L,
    val role: AiChatRole,
    val content: String,
    /** Model reasoning/thinking text for assistant replies; null when none was returned. */
    val reasoning: String? = null,
    val isError: Boolean = false,
    val createdAt: Long = 0L,
    /** Financial-tool turns stay local and are never replayed into ordinary chat. */
    val isLocalOnly: Boolean = false
)
