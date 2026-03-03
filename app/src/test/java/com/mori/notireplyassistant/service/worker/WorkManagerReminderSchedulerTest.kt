package com.mori.notireplyassistant.service.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class WorkManagerReminderSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerReminderScheduler

    @Before
    fun setup() {
        workManager = mock(WorkManager::class.java)
        scheduler = WorkManagerReminderScheduler(workManager)
    }

    @Test
    fun schedule_enqueuesUniqueWorkWithCorrectParams() {
        scheduler.schedule(123L, System.currentTimeMillis() + 10000)

        val requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest::class.java)

        verify(workManager).enqueueUniqueWork(
            org.mockito.ArgumentMatchers.eq("reminder_123"),
            org.mockito.ArgumentMatchers.eq(ExistingWorkPolicy.REPLACE),
            requestCaptor.capture()
        )

        val request = requestCaptor.value
        assertEquals(123L, request.workSpec.input.getLong(ReminderWorker.KEY_REMINDER_ID, -1L))
        assertTrue(request.tags.contains(WorkManagerReminderScheduler.TAG_REMINDERS))
    }

    @Test
    fun cancel_cancelsUniqueWork() {
        scheduler.cancel(123L)
        verify(workManager).cancelUniqueWork("reminder_123")
    }

    @Test
    fun cancelAll_cancelsByTag() {
        scheduler.cancelAll()
        verify(workManager).cancelAllWorkByTag(WorkManagerReminderScheduler.TAG_REMINDERS)
    }
}
