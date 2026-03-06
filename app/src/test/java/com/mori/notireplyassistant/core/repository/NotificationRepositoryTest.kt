package com.mori.notireplyassistant.core.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.database.entity.ConversationEntity
import com.mori.notireplyassistant.core.database.entity.QuickReplyTemplateEntity
import com.mori.notireplyassistant.core.domain.scheduler.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify
import com.mori.notireplyassistant.core.database.entity.ReminderEntity
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationRepositoryTest {

    private lateinit var db: NotiReplyDatabase
    private lateinit var repository: NotificationRepository
    @Mock private lateinit var scheduler: ReminderScheduler

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NotiReplyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NotificationRepository(db, scheduler)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun clearLocalData_preservesTemplatesByDefault() = runBlocking {
        // Insert sample data
        db.conversationDao().upsertConversation(ConversationEntity("c1", "pkg", "Title", "Msg", 1000))
        db.quickReplyTemplateDao().insertTemplate(QuickReplyTemplateEntity(text = "Hi"))

        // Verify inserted
        assertEquals(1, db.conversationDao().getActiveConversations().first().size)
        assertEquals(1, db.quickReplyTemplateDao().getAllTemplates().first().size)

        // Clear
        repository.clearLocalData(preserveTemplates = true)

        // Verify cleared
        assertEquals(0, db.conversationDao().getActiveConversations().first().size)
        // Templates preserved
        assertEquals(1, db.quickReplyTemplateDao().getAllTemplates().first().size)
    }

    @Test
    fun clearLocalData_removesTemplatesIfRequested() = runBlocking {
        db.quickReplyTemplateDao().insertTemplate(QuickReplyTemplateEntity(text = "Hi"))

        repository.clearLocalData(preserveTemplates = false)

        assertEquals(0, db.quickReplyTemplateDao().getAllTemplates().first().size)
    }

    @Test
    fun markConversationHandled_setsPendingCountToZero() = runBlocking {
        db.conversationDao().upsertConversation(ConversationEntity("c1", "pkg", "Title", "Msg", 1000, pendingCount = 5))

        repository.markConversationHandled("c1")

        val conv = db.conversationDao().getConversationById("c1")
        assertEquals(0, conv?.pendingCount)
    }

    @Test
    fun snoozeReminder_updatesScheduledTimeAndStatus_andSchedulesWork() = runBlocking {
        // Insert conversation to satisfy foreign key
        db.conversationDao().upsertConversation(ConversationEntity("c1", "pkg", "Title", "Msg", 1000))

        // Insert a pending reminder
        val reminderId = db.reminderDao().insertReminder(
            ReminderEntity(
                conversationId = "c1",
                messageId = null,
                scheduledTime = 1000L,
                status = "PENDING",
                note = "Test note"
            )
        )

        repository.snoozeReminder(reminderId, 600000L) // Snooze 10 mins

        val updatedReminder = db.reminderDao().getReminderById(reminderId)
        assertTrue(updatedReminder?.status == "SNOOZED")
        assertTrue((updatedReminder?.scheduledTime ?: 0L) > 1000L)

        // Verify scheduling was called with the updated time
        verify(scheduler).schedule(org.mockito.ArgumentMatchers.eq(reminderId), org.mockito.ArgumentMatchers.eq(updatedReminder!!.scheduledTime))
    }

    @Test
    fun dismissReminder_updatesStatusAndCancelsWork() = runBlocking {
        // Insert conversation to satisfy foreign key
        db.conversationDao().upsertConversation(ConversationEntity("c1", "pkg", "Title", "Msg", 1000))

        // Insert a pending reminder
        val reminderId = db.reminderDao().insertReminder(
            ReminderEntity(
                conversationId = "c1",
                messageId = null,
                scheduledTime = 1000L,
                status = "PENDING",
                note = "Test note"
            )
        )

        repository.dismissReminder(reminderId)

        val updatedReminder = db.reminderDao().getReminderById(reminderId)
        assertEquals("DISMISSED", updatedReminder?.status)

        verify(scheduler).cancel(reminderId)
}
}
