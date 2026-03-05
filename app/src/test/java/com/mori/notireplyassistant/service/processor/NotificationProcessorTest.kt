package com.mori.notireplyassistant.service.processor

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.domain.model.MessageData
import com.mori.notireplyassistant.core.domain.model.NotificationEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationProcessorTest {

    private lateinit var db: NotiReplyDatabase
    private lateinit var processor: NotificationProcessor

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotiReplyDatabase::class.java
        ).allowMainThreadQueries().build()

        processor = NotificationProcessor(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun testProcessNotification_conversationSeparation_byTitle() = runBlocking {
        // Event 1: Chat A
        val eventA = NotificationEvent(
            sbnKey = "keyA", packageName = "com.test", notificationId = 1, tag = null, postTime = 1000L,
            title = "Chat A", content = "Hello", groupKey = null, category = null, isGroup = false,
            styleType = null, styleMetadata = "{}", hasRemoteInput = false
        )

        // Event 2: Chat B
        val eventB = NotificationEvent(
            sbnKey = "keyB", packageName = "com.test", notificationId = 2, tag = null, postTime = 2000L,
            title = "Chat B", content = "Hi there", groupKey = null, category = null, isGroup = false,
            styleType = null, styleMetadata = "{}", hasRemoteInput = false
        )

        processor.processNotification(eventA)
        processor.processNotification(eventB)

        val activeConvos = db.conversationDao().getActiveConversations().first()
        assertEquals(2, activeConvos.size)

        val convA = activeConvos.find { it.title == "Chat A" }
        assertNotNull(convA)
        assertEquals("com.test|t:chat a", convA?.conversationId)
        assertEquals(1, convA?.pendingCount)

        val convB = activeConvos.find { it.title == "Chat B" }
        assertNotNull(convB)
        assertEquals("com.test|t:chat b", convB?.conversationId)
        assertEquals(1, convB?.pendingCount)
    }

    @Test
    fun testProcessNotification_conversationSeparation_byConversationTitle() = runBlocking {
        // Event 1: LINE Group 1
        val eventA = NotificationEvent(
            sbnKey = "key1", packageName = "jp.naver.line.android", notificationId = 1, tag = null, postTime = 1000L,
            title = "Alice", content = "Hello", groupKey = null, category = null, isGroup = false,
            styleType = "android.app.Notification\$MessagingStyle", styleMetadata = "{}", hasRemoteInput = false,
            conversationTitle = "Group 1", isGroupConversation = true,
            messages = listOf(MessageData("Alice", "Hello", 1000L))
        )

        // Event 2: LINE Group 2
        val eventB = NotificationEvent(
            sbnKey = "key2", packageName = "jp.naver.line.android", notificationId = 2, tag = null, postTime = 2000L,
            title = "Bob", content = "Hi", groupKey = null, category = null, isGroup = false,
            styleType = "android.app.Notification\$MessagingStyle", styleMetadata = "{}", hasRemoteInput = false,
            conversationTitle = "Group 2", isGroupConversation = true,
            messages = listOf(MessageData("Bob", "Hi", 2000L))
        )

        processor.processNotification(eventA)
        processor.processNotification(eventB)

        val activeConvos = db.conversationDao().getActiveConversations().first()
        assertEquals(2, activeConvos.size)

        val convA = activeConvos.find { it.conversationId == "jp.naver.line.android|mt:group 1" }
        assertNotNull(convA)
        assertEquals("GROUP", convA?.threadType)

        val convB = activeConvos.find { it.conversationId == "jp.naver.line.android|mt:group 2" }
        assertNotNull(convB)
        assertEquals("GROUP", convB?.threadType)
    }
}
