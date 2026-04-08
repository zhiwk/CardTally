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
    EMPTY_REPLY,
    TIMEOUT,
    CANCELLED,
    INTERRUPTED
}

sealed class MiniMaxChatResult {
    data class Success(val reply: String) : MiniMaxChatResult()

    data class Failure(
        val type: MiniMaxErrorType,
        val detail: String? = null,
        val statusCode: Int? = null
    ) : MiniMaxChatResult()

    /**
     * Represents a streaming chunk with partial assistant content.
     * Used for incremental UI updates during streaming responses.
     */
    data class StreamingChunk(val partialContent: String) : MiniMaxChatResult()

    /**
     * Signifies the streaming response is complete.
     * The final accumulated content is provided.
     */
    data class StreamingDone(val finalContent: String) : MiniMaxChatResult()
}
