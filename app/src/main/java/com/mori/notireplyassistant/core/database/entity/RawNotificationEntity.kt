package com.mori.notireplyassistant.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "raw_notifications",
    indices = [
        Index(value = ["sbn_key"]),
        Index(value = ["package_name", "post_time"])
    ]
)
data class RawNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "event_id")
    val eventId: Long = 0,

    @ColumnInfo(name = "sbn_key")
    val sbnKey: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "notification_id")
    val notificationId: Int,

    @ColumnInfo(name = "tag")
    val tag: String?,

    @ColumnInfo(name = "post_time")
    val postTime: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String, // Redacted/Truncated if necessary

    @ColumnInfo(name = "style_metadata")
    val styleMetadata: String, // JSON string

    @ColumnInfo(name = "event_type")
    val eventType: String // POSTED, REMOVED
)
