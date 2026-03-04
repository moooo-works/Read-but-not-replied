package com.mori.notireplyassistant.service.processor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.domain.model.MessageData
import com.mori.notireplyassistant.core.domain.model.NotificationEvent
import com.mori.notireplyassistant.service.util.ConversationIdGenerator
import com.mori.notireplyassistant.service.util.MessageIdGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationProcessorTest {

    private lateinit var db: NotiReplyDatabase
    private lateinit var processor: NotificationProcessor

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NotiReplyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        processor = NotificationProcessor(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun processNotification_insertsDataCorrectly() = runBlocking {
        val msg = MessageData("Sender", "Hello", 1000)
        val event = NotificationEvent(
            sbnKey = "key1",
            packageName = "com.pkg",
            notificationId = 1,
            tag = null,
            postTime = 1000,
            title = "Title",
            content = "Content",
            groupKey = null,
            category = "msg",
            isGroup = false,
            isGroupSummary = false,
            styleType = "MessagingStyle",
            styleMetadata = "{}",
            hasRemoteInput = true,
            messages = listOf(msg)
        )

        processor.processNotification(event)

        // Expected conversation ID: com.pkg|sender:Sender
        val convId = ConversationIdGenerator.generate("com.pkg", null, "Title", "Sender", "key1")
        val conv = db.conversationDao().getConversationById(convId)

        assertNotNull(conv)
        assertEquals(1, conv?.pendingCount)
        assertEquals("Content", conv?.lastMessagePreview)

        // Expected Message ID
        val msgId = MessageIdGenerator.generate("com.pkg", convId, "Sender", "Hello", 1000, "key1", 0)
        val storedMsg = db.messageDao().getMessageById(msgId)
        assertNotNull(storedMsg)
    }

    @Test
    fun processNotification_repeatedUpdate_doesNotinflateCount() = runBlocking {
        val msg = MessageData("Sender", "Hello", 1000)
        val event = NotificationEvent(
            sbnKey = "key1",
            packageName = "com.pkg",
            notificationId = 1,
            tag = null,
            postTime = 1000,
            title = "Title",
            content = "Hello",
            groupKey = null,
            category = "msg",
            isGroup = false,
            isGroupSummary = false,
            styleType = "MessagingStyle",
            styleMetadata = "{}",
            hasRemoteInput = true,
            messages = listOf(msg)
        )

        // First process
        processor.processNotification(event)
        val convId = ConversationIdGenerator.generate("com.pkg", null, "Title", "Sender", "key1")
        var conv = db.conversationDao().getConversationById(convId)
        assertEquals(1, conv?.pendingCount)

        // Second process (same exact event/message) immediately
        processor.processNotification(event)
        conv = db.conversationDao().getConversationById(convId)
        assertEquals(1, conv?.pendingCount) // Should still be 1 due to short window suppression
    }

    @Test
    fun processNotification_messageWithZeroTimestamp_generatesStableId() = runBlocking {
        val msg = MessageData("Sender", "Hello", 0)
        val event = NotificationEvent(
            sbnKey = "key1",
            packageName = "com.pkg",
            notificationId = 1,
            tag = null,
            postTime = 1000,
            title = "Title",
            content = "Hello",
            groupKey = null,
            category = "msg",
            isGroup = false,
            isGroupSummary = false,
            styleType = "MessagingStyle",
            styleMetadata = "{}",
            hasRemoteInput = true,
            messages = listOf(msg)
        )

        processor.processNotification(event)

        val convId = ConversationIdGenerator.generate("com.pkg", null, "Title", "Sender", "key1")
        val expectedMsgId = MessageIdGenerator.generate("com.pkg", convId, "Sender", "Hello", 0, "key1", 0)
        val storedMsg = db.messageDao().getMessageById(expectedMsgId)

        assertNotNull(storedMsg)
    }

    @Test
    fun processNotification_newMessages_incrementsCount() = runBlocking {
        val msg1 = MessageData("Sender", "Hello", 1000)
        val event1 = NotificationEvent(
            sbnKey = "key1",
            packageName = "com.pkg",
            notificationId = 1,
            tag = null,
            postTime = 1000,
            title = "Title",
            content = "Hello",
            groupKey = null,
            category = "msg",
            isGroup = false,
            isGroupSummary = false,
            styleType = "MessagingStyle",
            styleMetadata = "{}",
            hasRemoteInput = true,
            messages = listOf(msg1)
        )
        processor.processNotification(event1)

        val msg2 = MessageData("Sender", "World", 2000)
        val event2 = event1.copy(
            postTime = 2000,
            content = "World",
            messages = listOf(msg1, msg2) // Old + New
        )
        processor.processNotification(event2)

        val convId = ConversationIdGenerator.generate("com.pkg", null, "Title", "Sender", "key1")
        val conv = db.conversationDao().getConversationById(convId)
        assertEquals(2, conv?.pendingCount)
        assertEquals("World", conv?.lastMessagePreview)
    }

    @Test
    fun processNotification_groupSummary_doesNotInsertMessagesOrIncrementCount() = runBlocking {
        val msg = MessageData("Sender", "Hello from summary", 1000)

        // Emulate summary notification
        val summaryEvent = NotificationEvent(
            sbnKey = "summary_key",
            packageName = "com.pkg",
            notificationId = 100,
            tag = null,
            postTime = 1000,
            title = "Group Summary",
            content = "Hello from summary",
            groupKey = "group1",
            category = "msg",
            isGroup = true,
            isGroupSummary = true, // Key flag
            styleType = "MessagingStyle",
            styleMetadata = "{}",
            hasRemoteInput = true,
            messages = listOf(msg)
        )
        processor.processNotification(summaryEvent)

        val convId = ConversationIdGenerator.generate("com.pkg", "group1", "Group Summary", "Sender", "summary_key")
        val convAfterSummary = db.conversationDao().getConversationById(convId)

        // The conversation shouldn't even be created if we bail out early, or it shouldn't have any messages if created
        // Based on the code, we bail out right after raw notification insertion, so conversation is null.
        assertNull(convAfterSummary)

        // Now process the child
        val childEvent = NotificationEvent(
            sbnKey = "child_key",
            packageName = "com.pkg",
            notificationId = 101,
            tag = null,
            postTime = 1005,
            title = "Group Summary", // Usually same title in child
            content = "Hello from summary",
            groupKey = "group1",
            category = "msg",
            isGroup = true,
            isGroupSummary = false, // Not a summary
            styleType = "MessagingStyle",
            styleMetadata = "{}",
            hasRemoteInput = true,
            messages = listOf(msg)
        )
        processor.processNotification(childEvent)

        val convAfterChild = db.conversationDao().getConversationById(convId)
        assertNotNull(convAfterChild)
        assertEquals(1, convAfterChild?.pendingCount)
    }
}
