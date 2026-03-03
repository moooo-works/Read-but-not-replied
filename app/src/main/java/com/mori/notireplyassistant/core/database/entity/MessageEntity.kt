package com.mori.notireplyassistant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["conversation_id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RawNotificationEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["raw_event_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["conversation_id"]),
        Index(value = ["raw_event_id"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String, // Deterministic Hash

    @ColumnInfo(name = "conversation_id")
    val conversationId: String,

    @ColumnInfo(name = "raw_event_id")
    val rawEventId: Long?,

    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "attachment_placeholder")
    val attachmentPlaceholder: String? = null // e.g., "[Image]"
)
