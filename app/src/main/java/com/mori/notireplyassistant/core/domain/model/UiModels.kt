package com.mori.notireplyassistant.core.domain.model

data class ConversationUiModel(
    val conversationId: String,
    val packageName: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
    val pendingCount: Int,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val hasPendingReminder: Boolean = false, // Populated later if needed
    val threadType: String = "UNKNOWN"
)

data class MessageUiModel(
    val messageId: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val isMe: Boolean // Inferred from sender == "Me" or specific logic
)

data class ReminderUiModel(
    val reminderId: Long,
    val conversationId: String,
    val conversationTitle: String,
    val scheduledTime: Long,
    val status: String,
    val note: String?
)
