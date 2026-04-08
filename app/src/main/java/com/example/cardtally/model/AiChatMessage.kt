package com.example.cardtally.model

enum class AiChatRole(val apiValue: String) {
    USER("user"),
    ASSISTANT("assistant")
}

data class AiChatMessage(
    val role: AiChatRole,
    val content: String,
    val isError: Boolean = false
)
