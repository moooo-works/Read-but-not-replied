package com.mori.notireplyassistant.service.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.database.dao.ConversationDao
import com.mori.notireplyassistant.core.database.dao.ReminderDao
import com.mori.notireplyassistant.core.database.entity.ConversationEntity
import com.mori.notireplyassistant.core.database.entity.ReminderEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderWorkerTest {

    @Mock private lateinit var db: NotiReplyDatabase
    @Mock private lateinit var reminderDao: ReminderDao
    @Mock private lateinit var conversationDao: ConversationDao
    @Mock private lateinit var publisher: NotificationPublisher

    private lateinit var context: Context
    private lateinit var workerFactory: WorkerFactory

    @Before
    fun setup() {
        org.mockito.MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        `when`(db.reminderDao()).thenReturn(reminderDao)
        `when`(db.conversationDao()).thenReturn(conversationDao)

        workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker {
                return ReminderWorker(appContext, workerParameters, db, publisher)
            }
        }
    }

    @Test
    fun doWork_noReminderId_returnsSuccess() = runBlocking {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verifyNoInteractions(db)
        verifyNoInteractions(publisher)
    }

    @Test
    fun doWork_reminderNotFound_returnsSuccess() = runBlocking {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
            .setWorkerFactory(workerFactory)
            .setInputData(workDataOf(ReminderWorker.KEY_REMINDER_ID to 1L))
            .build()

        `when`(reminderDao.getReminderById(1L)).thenReturn(null)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verifyNoInteractions(publisher)
    }

    @Test
    fun doWork_statusFired_returnsSuccessNoPublish() = runBlocking {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
            .setWorkerFactory(workerFactory)
            .setInputData(workDataOf(ReminderWorker.KEY_REMINDER_ID to 1L))
            .build()

        val reminder = ReminderEntity(1L, "c1", null, 1000, "FIRED", null)
        `when`(reminderDao.getReminderById(1L)).thenReturn(reminder)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verifyNoInteractions(publisher)
    }

    @Test
    fun doWork_statusPending_publishesAndUpdatesToFired() = runBlocking {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
            .setWorkerFactory(workerFactory)
            .setInputData(workDataOf(ReminderWorker.KEY_REMINDER_ID to 1L))
            .build()

        val reminder = ReminderEntity(1L, "c1", null, 1000, "PENDING", "Call back")
        val conversation = ConversationEntity("c1", "pkg", "Mom", "Preview", 1000)

        `when`(reminderDao.getReminderById(1L)).thenReturn(reminder)
        `when`(conversationDao.getConversationById("c1")).thenReturn(conversation)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        verify(publisher).publishReminder(1, "Mom", "You have a pending reply - Call back")
        verify(reminderDao).updateStatus(1L, "FIRED")
    }

    @Test
    fun doWork_publisherThrows_returnsRetry() = runBlocking {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context)
            .setWorkerFactory(workerFactory)
            .setInputData(workDataOf(ReminderWorker.KEY_REMINDER_ID to 1L))
            .build()

        val reminder = ReminderEntity(1L, "c1", null, 1000, "PENDING", null)
        val conversation = ConversationEntity("c1", "pkg", "Mom", "Preview", 1000)

        `when`(reminderDao.getReminderById(1L)).thenReturn(reminder)
        `when`(conversationDao.getConversationById("c1")).thenReturn(conversation)

        doThrow(RuntimeException("Publish failed")).`when`(publisher)
            .publishReminder(1, "Mom", "You have a pending reply ")

        val result = worker.doWork()

        assertEquals(Result.retry(), result)
        verify(reminderDao, never()).updateStatus(anyLong(), anyString())
    }
}
