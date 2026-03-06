package com.mori.notireplyassistant.core.database.dao

import androidx.room.ColumnInfo

data class ReminderWithConversationPOJO(
    @ColumnInfo(name = "reminder_id") val reminderId: Long,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "message_id") val messageId: String?,
    @ColumnInfo(name = "scheduled_time") val scheduledTime: Long,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "conversationTitle") val conversationTitle: String
)
