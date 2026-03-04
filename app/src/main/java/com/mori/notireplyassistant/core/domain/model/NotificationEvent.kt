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
    val hasRemoteInput: Boolean
)

data class MessageData(
    val sender: String,
    val text: String?,
    val timestamp: Long,
    val mimeType: String? = null
)

data class ActiveReplyInfo(
    val key: String,
    // Note: PendingIntent and RemoteInput are framework objects,
    // so we keep them in a Service-level cache, not in this pure domain model if possible.
    // However, for the Processor to handle replies later, we need a way to reference them.
    // We will use the sbnKey as the handle.
    val canReply: Boolean
)
