package com.mori.notireplyassistant.service.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: NotiReplyDatabase,
    private val notificationPublisher: NotificationPublisher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
        if (reminderId == -1L) return Result.success()

        val reminder = db.reminderDao().getReminderById(reminderId) ?: return Result.success()

        if (reminder.status != "PENDING" && reminder.status != "SNOOZED") {
            return Result.success() // Already fired or dismissed
        }

        val conversation = db.conversationDao().getConversationById(reminder.conversationId)
        val title = conversation?.title ?: "Reminder"
        val noteText = if (!reminder.note.isNullOrBlank()) "- ${reminder.note}" else ""
        val text = "You have a pending reply $noteText"

        return try {
            notificationPublisher.publishReminder(
                notificationId = reminderId.toInt(),
                title = title,
                text = text
            )

            db.reminderDao().updateStatus(reminderId, "FIRED")
            Result.success()
        } catch (e: Exception) {
            // If publishing fails (e.g., system error), retry later
            Result.retry()
        }
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
    }
}
