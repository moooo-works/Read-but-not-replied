package com.mori.notireplyassistant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mori.notireplyassistant.service.util.ThreadType

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["package_name"]),
        Index(value = ["last_timestamp"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    @ColumnInfo(name = "conversation_id")
    val conversationId: String, // format: packageName|normalizedKey

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "title")
    val title: String, // Sender or Group Name

    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String,

    @ColumnInfo(name = "last_timestamp")
    val lastTimestamp: Long,

    @ColumnInfo(name = "pending_count")
    val pendingCount: Int = 0,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "thread_type", defaultValue = "UNKNOWN")
    val threadType: String = ThreadType.UNKNOWN.name,

    @ColumnInfo(name = "thread_key_source", defaultValue = "UNKNOWN")
    val threadKeySource: String = "UNKNOWN"
)
