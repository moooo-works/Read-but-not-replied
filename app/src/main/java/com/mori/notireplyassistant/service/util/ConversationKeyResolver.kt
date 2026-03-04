package com.mori.notireplyassistant.service.util

import com.mori.notireplyassistant.core.domain.model.NotificationEvent

enum class ThreadType {
    DIRECT, GROUP, UNKNOWN
}

data class ResolvedConversationKey(
    val threadKey: String,
    val threadType: ThreadType,
    val keySource: String
)

object ConversationKeyResolver {

    fun normalize(input: String): String {
        return input
            .replace("\\s+".toRegex(), " ")
            .trim()
            .lowercase()
            .replace(Regex("[(（]\\d+[)）]$"), "")
            .trim()
    }

    fun resolve(event: NotificationEvent): ResolvedConversationKey {
        if (!event.conversationTitle.isNullOrBlank()) {
            return ResolvedConversationKey(
                threadKey = "mt:" + normalize(event.conversationTitle!!),
                threadType = if (event.isGroupConversation == true) ThreadType.GROUP else ThreadType.UNKNOWN,
                keySource = "MESSAGING_CONVERSATION_TITLE"
            )
        }

        if (event.title.isNotBlank()) {
            return ResolvedConversationKey(
                threadKey = "t:" + normalize(event.title),
                threadType = ThreadType.UNKNOWN,
                keySource = "TITLE"
            )
        }

        return ResolvedConversationKey(
            threadKey = "k:" + event.sbnKey,
            threadType = ThreadType.UNKNOWN,
            keySource = "SBN_KEY"
        )
    }
}
