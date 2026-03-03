package com.mori.notireplyassistant.core.domain.handler

enum class ReplyResult {
    SUCCESS,
    FALLBACK_OPENED,
    NOT_AVAILABLE,
    FAILED
}

interface ReplyHandler {
    suspend fun canReply(conversationId: String): Boolean
    suspend fun sendReply(conversationId: String, packageName: String, message: String): ReplyResult
}
