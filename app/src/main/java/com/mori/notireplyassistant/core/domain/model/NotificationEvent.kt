package com.mori.notireplyassistant.core.domain.model

data class NotificationEvent(
    val sbnKey: String,
    val packageName: String,
    val notificationId: Int,
    val tag: String?,
    val postTime: Long,
    val title: String,
    val content: String,
    val groupKey: String?,
    val category: String?,
    val isGroup: Boolean,
    val isGroupSummary: Boolean = isGroup,
    val styleType: String?, // Messaging, BigText, etc.
    val styleMetadata: String, // JSON
    val messages: List<MessageData> = emptyList(),
    val hasRemoteInput: Boolean,
    val conversationTitle: String? = null,
    val isGroupConversation: Boolean? = null
)

data class MessageData(
    val sender: String,
    val text: String?,
    val timestamp: Long,
    val mimeType: String? = null
)

data class ActiveReplyInfo(
    val key: String,
    val canReply: Boolean
)
