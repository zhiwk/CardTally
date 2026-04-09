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
    val isError: Boolean = false,
    val createdAt: Long = 0L
)
