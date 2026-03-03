package com.mori.notireplyassistant.service.processor

import androidx.room.withTransaction
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.database.entity.ConversationEntity
import com.mori.notireplyassistant.core.database.entity.MessageEntity
import com.mori.notireplyassistant.core.database.entity.RawNotificationEntity
import com.mori.notireplyassistant.core.domain.model.NotificationEvent
import com.mori.notireplyassistant.core.domain.model.MessageData
import com.mori.notireplyassistant.service.util.ConversationIdGenerator
import com.mori.notireplyassistant.service.util.MessageIdGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationProcessor @Inject constructor(
    private val db: NotiReplyDatabase
) {

    suspend fun processNotification(event: NotificationEvent) {
        // Step 1: Calculate Conversation ID
        val conversationId = ConversationIdGenerator.generate(
            event.packageName,
            event.groupKey,
            event.title,
            if (event.messages.isNotEmpty()) event.messages.last().sender else event.title,
            event.sbnKey
        )

        db.withTransaction {
            // Step 2: Insert Raw Event
            val rawEntity = RawNotificationEntity(
                sbnKey = event.sbnKey,
                packageName = event.packageName,
                notificationId = event.notificationId,
                tag = event.tag,
                postTime = event.postTime,
                title = event.title,
                content = event.content, // Redacted if PII concerns
                styleMetadata = event.styleMetadata,
                eventType = "POSTED"
            )
            val rawId = db.rawNotificationDao().insertRawEvent(rawEntity)

            // Step 3: Ensure Conversation Exists
            val existingConversation = db.conversationDao().getConversationById(conversationId)
            if (existingConversation == null) {
                db.conversationDao().upsertConversation(
                    ConversationEntity(
                        conversationId = conversationId,
                        packageName = event.packageName,
                        title = event.title,
                        lastMessagePreview = event.content,
                        lastTimestamp = event.postTime,
                        pendingCount = 0
                    )
                )
            }

            // Step 4: Generate Message Entities
            val messagesToInsert = if (event.messages.isNotEmpty()) {
                event.messages.map { msg ->
                    MessageEntity(
                        messageId = MessageIdGenerator.generate(
                            event.packageName,
                            conversationId,
                            msg.sender,
                            msg.text,
                            msg.timestamp
                        ),
                        conversationId = conversationId,
                        rawEventId = rawId,
                        sender = msg.sender,
                        text = msg.text ?: "",
                        timestamp = msg.timestamp
                    )
                }
            } else {
                // Fallback for non-messaging style
                listOf(
                    MessageEntity(
                        messageId = MessageIdGenerator.generate(
                            event.packageName,
                            conversationId,
                            event.title, // Sender fallback
                            event.content,
                            event.postTime
                        ),
                        conversationId = conversationId,
                        rawEventId = rawId,
                        sender = event.title,
                        text = event.content,
                        timestamp = event.postTime
                    )
                )
            }

            // Step 5: Insert Messages (Ignore duplicates)
            val insertedIds = db.messageDao().insertMessagesIgnore(messagesToInsert)
            val insertedCount = insertedIds.count { it != -1L }

            // Step 6: Update Conversation Count Only if New Messages Inserted
            if (insertedCount > 0) {
                db.conversationDao().updateConversationAfterInsert(
                    conversationId = conversationId,
                    deltaCount = insertedCount,
                    lastPreview = event.content,
                    lastTimestamp = event.postTime
                )
            }
        }
    }

    suspend fun processRemoval(sbnKey: String, packageName: String, id: Int, tag: String?, postTime: Long) {
        val rawEntity = RawNotificationEntity(
            sbnKey = sbnKey,
            packageName = packageName,
            notificationId = id,
            tag = tag,
            postTime = postTime,
            title = "",
            content = "",
            styleMetadata = "{}",
            eventType = "REMOVED"
        )
        db.rawNotificationDao().insertRawEvent(rawEntity)
        // No update to pending_count on removal
    }
}
