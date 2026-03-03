package com.mori.notireplyassistant.service.worker

import androidx.work.*
import com.mori.notireplyassistant.core.domain.scheduler.ReminderScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    private val workManager: WorkManager
) : ReminderScheduler {

    override fun schedule(reminderId: Long, scheduledTimeMillis: Long) {
        val delay = (scheduledTimeMillis - System.currentTimeMillis()).coerceAtLeast(0)

        val inputData = Data.Builder()
            .putLong(ReminderWorker.KEY_REMINDER_ID, reminderId)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(TAG_REMINDERS)
            .build()

        workManager.enqueueUniqueWork(
            "reminder_$reminderId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancel(reminderId: Long) {
        workManager.cancelUniqueWork("reminder_$reminderId")
    }

    override fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG_REMINDERS)
    }

    companion object {
        const val TAG_REMINDERS = "reminders"
    }
}
