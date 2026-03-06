package com.mori.notireplyassistant.core.repository

import androidx.room.withTransaction
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.database.entity.ReminderEntity
import com.mori.notireplyassistant.core.domain.model.ConversationUiModel
import com.mori.notireplyassistant.core.domain.model.ReminderUiModel
import com.mori.notireplyassistant.core.domain.model.MessageUiModel
import com.mori.notireplyassistant.core.domain.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ConversationFilter {
    ALL,
    ARCHIVED,
}

@Singleton
class NotificationRepository @Inject constructor(
    private val db: NotiReplyDatabase,
    private val scheduler: ReminderScheduler
) {

    fun observeConversations(filter: ConversationFilter): Flow<List<ConversationUiModel>> {
        val flow = when (filter) {
            ConversationFilter.ARCHIVED -> db.conversationDao().getArchivedConversations()
            else -> db.conversationDao().getActiveConversations() // Default to ALL (Active)
        }

        return flow.map { entities ->
            entities.map { entity ->
                ConversationUiModel(
                    conversationId = entity.conversationId,
                    packageName = entity.packageName,
                    title = entity.title,
                    preview = entity.lastMessagePreview,
                    timestamp = entity.lastTimestamp,
                    pendingCount = entity.pendingCount,
                    isArchived = entity.isArchived,
                    isPinned = entity.isPinned,
                    threadType = entity.threadType
                )
            }
        }
    }

    fun observeActiveReminders(): Flow<List<ReminderUiModel>> {
        return db.reminderDao().getActiveRemindersWithConversation().map { list ->
            list.map { pojo ->
                ReminderUiModel(
                    reminderId = pojo.reminderId,
                    conversationId = pojo.conversationId,
                    conversationTitle = pojo.conversationTitle,
                    scheduledTime = pojo.scheduledTime,
                    status = pojo.status,
                    note = pojo.note
                )
            }
        }
    }

    fun observeMessages(conversationId: String): Flow<List<MessageUiModel>> {
        return db.messageDao().getMessagesForConversation(conversationId)
            .map { entities ->
                entities.map { entity ->
                    MessageUiModel(
                        messageId = entity.messageId,
                        sender = entity.sender,
                        text = entity.text,
                        timestamp = entity.timestamp,
                        isMe = false
                    )
                }
            }
    }

    suspend fun markConversationHandled(conversationId: String) {
        db.conversationDao().updatePendingCount(conversationId, 0)
    }

    suspend fun setArchived(conversationId: String, isArchived: Boolean) {
        db.conversationDao().updateArchived(conversationId, isArchived)
    }

    suspend fun setPinned(conversationId: String, isPinned: Boolean) {
        db.conversationDao().updatePinned(conversationId, isPinned)
    }

    suspend fun dismissReminder(reminderId: Long) {
        db.reminderDao().updateStatus(reminderId, "DISMISSED")
        scheduler.cancel(reminderId)
    }

    suspend fun snoozeReminder(reminderId: Long, snoozeDurationMs: Long = 10 * 60 * 1000L) {
        val reminder = db.reminderDao().getReminderById(reminderId) ?: return
        val newTime = System.currentTimeMillis() + snoozeDurationMs
        val updated = reminder.copy(
            scheduledTime = newTime,
            status = "SNOOZED"
        )
        db.reminderDao().insertReminder(updated)
        scheduler.schedule(reminderId, newTime)
    }

    suspend fun createReminder(conversationId: String, scheduledTime: Long, note: String?) {
        val id = db.reminderDao().insertReminder(
            ReminderEntity(
                conversationId = conversationId,
                messageId = null,
                scheduledTime = scheduledTime,
                status = "PENDING",
                note = note
            )
        )
        scheduler.schedule(id, scheduledTime)
    }

    suspend fun clearLocalData(preserveTemplates: Boolean = true) {
        db.withTransaction {
            db.rawNotificationDao().deleteAllRawNotifications()
            db.messageDao().deleteAllMessages()
            db.reminderDao().deleteAllReminders()
            db.conversationDao().deleteAllConversations()

            if (!preserveTemplates) {
                db.quickReplyTemplateDao().deleteAllTemplates()
            }
        }
        scheduler.cancelAll()
    }
}
