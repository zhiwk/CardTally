package com.example.cardtally.network

enum class MiniMaxErrorType {
    INVALID_CONFIG,
    INVALID_URL,
    NETWORK,
    AUTH,
    RATE_LIMIT,
    INSUFFICIENT_BALANCE,
    INVALID_REQUEST,
    HTTP,
    PARSE,
    EMPTY_REPLY
}

sealed class MiniMaxChatResult {
    data class Success(val reply: String) : MiniMaxChatResult()

    data class Failure(
        val type: MiniMaxErrorType,
        val detail: String? = null,
        val statusCode: Int? = null
    ) : MiniMaxChatResult()
}
