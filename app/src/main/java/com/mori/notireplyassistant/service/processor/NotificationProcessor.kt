package com.mori.notireplyassistant.service.processor

import android.util.LruCache
import androidx.room.withTransaction
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.database.entity.ConversationEntity
import com.mori.notireplyassistant.core.database.entity.MessageEntity
import com.mori.notireplyassistant.core.database.entity.RawNotificationEntity
import com.mori.notireplyassistant.core.domain.model.NotificationEvent
import com.mori.notireplyassistant.service.util.ConversationIdGenerator
import com.mori.notireplyassistant.service.util.MessageIdGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationProcessor @Inject constructor(
    private val db: NotiReplyDatabase
) {
    data class ProcessedRecord(val messageIdsHash: Int, val processedAt: Long)
    // Use LruCache to prevent infinite memory leak (keeps last 200 records)
    private val recentProcessedMap = LruCache<String, ProcessedRecord>(200)
    private val SUPPRESSION_WINDOW_MS = 2000L

    suspend fun processNotification(event: NotificationEvent) {
        // Step 1: Calculate Conversation ID
        val conversationId = ConversationIdGenerator.generate(
            event.packageName,
            event.groupKey,
            event.title,
            if (event.messages.isNotEmpty()) event.messages.last().sender else event.title,
            event.sbnKey
        )

        // Pre-compute message IDs and check short-window duplication
        val pendingMessages = if (event.messages.isNotEmpty()) {
            event.messages.mapIndexed { index, msg ->
                val id = MessageIdGenerator.generate(
                    event.packageName,
                    conversationId,
                    msg.sender,
                    msg.text,
                    msg.timestamp,
                    event.sbnKey,
                    index
                )
                Triple(id, msg, index)
            }
        } else {
            val id = MessageIdGenerator.generate(
                event.packageName,
                conversationId,
                event.title, // Sender fallback
                event.content,
                event.postTime,
                event.sbnKey,
                0
            )
            listOf(Triple(id, null, 0))
        }

        val messageIdsHash = pendingMessages.map { it.first }.hashCode()
        val now = System.currentTimeMillis()

        var shouldSkip = false
        synchronized(recentProcessedMap) {
            val lastProcessed = recentProcessedMap.get(event.sbnKey)
            if (lastProcessed != null &&
                lastProcessed.messageIdsHash == messageIdsHash &&
                (now - lastProcessed.processedAt) < SUPPRESSION_WINDOW_MS) {
                shouldSkip = true
            } else {
                recentProcessedMap.put(event.sbnKey, ProcessedRecord(messageIdsHash, now))
            }
        }

        if (shouldSkip) {
            // Duplicate update within 2 seconds, skip processing
            return
        }

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

            // Ignore group summary notifications from creating duplicate message entries
            if (event.isGroupSummary) {
                return@withTransaction
            }

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
            val messagesToInsert = pendingMessages.map { triple ->
                val (id, msgData, _) = triple
                if (msgData != null) {
                    MessageEntity(
                        messageId = id,
                        conversationId = conversationId,
                        rawEventId = rawId,
                        sender = msgData.sender,
                        text = msgData.text ?: "",
                        timestamp = msgData.timestamp
                    )
                } else {
                    MessageEntity(
                        messageId = id,
                        conversationId = conversationId,
                        rawEventId = rawId,
                        sender = event.title,
                        text = event.content,
                        timestamp = event.postTime
                    )
                }
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
