package com.mori.notireplyassistant.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.database.entity.ConversationEntity
import com.mori.notireplyassistant.core.database.entity.MessageEntity
import com.mori.notireplyassistant.core.database.entity.RawNotificationEntity
import com.mori.notireplyassistant.core.database.entity.ReminderEntity
import kotlinx.coroutines.flow.first
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
class DaoTest {

    private lateinit var db: NotiReplyDatabase
    private lateinit var rawDao: RawNotificationDao
    private lateinit var conversationDao: ConversationDao
    private lateinit var messageDao: MessageDao
    private lateinit var reminderDao: ReminderDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NotiReplyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        rawDao = db.rawNotificationDao()
        conversationDao = db.conversationDao()
        messageDao = db.messageDao()
        reminderDao = db.reminderDao()

        // Enable FK constraints for SQLite
        db.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys=ON;")
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertRawNotification() = runBlocking {
        val event = RawNotificationEntity(
            sbnKey = "key1",
            packageName = "com.test",
            notificationId = 1,
            tag = null,
            postTime = 1000,
            title = "Title",
            content = "Content",
            styleMetadata = "{}",
            eventType = "POSTED"
        )
        val id = rawDao.insertRawEvent(event)
        val events = rawDao.getEventsByPackage("com.test")
        assertEquals(1, events.size)
        assertEquals(id, events[0].eventId)
    }

    @Test
    fun conversationInsertAndUpdate() = runBlocking {
        val conv = ConversationEntity(
            conversationId = "com.test|key1",
            packageName = "com.test",
            title = "User A",
            lastMessagePreview = "Hello",
            lastTimestamp = 1000
        )
        conversationDao.upsertConversation(conv)

        var retrieved = conversationDao.getConversationById("com.test|key1")
        assertNotNull(retrieved)
        assertEquals(0, retrieved?.pendingCount)

        conversationDao.updateConversationAfterInsert("com.test|key1", 1, "World", 2000)
        retrieved = conversationDao.getConversationById("com.test|key1")
        assertEquals(1, retrieved?.pendingCount)
        assertEquals("World", retrieved?.lastMessagePreview)
    }

    @Test
    fun messageInsertIgnore() = runBlocking {
        val conv = ConversationEntity("c1", "pkg", "Title", "Preview", 1000)
        conversationDao.upsertConversation(conv)

        val msg1 = MessageEntity("m1", "c1", null, "Me", "Hi", 1000)
        val msg2 = MessageEntity("m1", "c1", null, "Me", "Hi", 1000) // Duplicate ID

        messageDao.insertMessagesIgnore(listOf(msg1))
        val initialList = messageDao.getMessagesForConversation("c1").first()
        assertEquals(1, initialList.size)

        // Should ignore duplicate
        messageDao.insertMessagesIgnore(listOf(msg2))
        val finalList = messageDao.getMessagesForConversation("c1").first()
        assertEquals(1, finalList.size)
    }

    @Test
    fun cascadeDeleteConversation() = runBlocking {
        val conv = ConversationEntity("c1", "pkg", "Title", "Preview", 1000)
        conversationDao.upsertConversation(conv)
        val msg = MessageEntity("m1", "c1", null, "Me", "Hi", 1000)
        messageDao.insertMessagesIgnore(listOf(msg))

        // Manually delete conversation via SQL to test CASCADE
        db.openHelper.writableDatabase.execSQL("DELETE FROM conversations WHERE conversation_id = 'c1'")

        val messages = messageDao.getMessagesForConversation("c1").first()
        assertEquals(0, messages.size)
    }

    @Test
    fun setNullOnRawEventDelete() = runBlocking {
         val raw = RawNotificationEntity(
            sbnKey = "key1",
            packageName = "com.test",
            notificationId = 1,
            tag = null,
            postTime = 1000,
            title = "Title",
            content = "Content",
            styleMetadata = "{}",
            eventType = "POSTED"
        )
        val rawId = rawDao.insertRawEvent(raw)

        val conv = ConversationEntity("c1", "pkg", "Title", "Preview", 1000)
        conversationDao.upsertConversation(conv)

        val msg = MessageEntity("m1", "c1", rawId, "Me", "Hi", 1000)
        messageDao.insertMessagesIgnore(listOf(msg))

        // Delete raw event
        db.openHelper.writableDatabase.execSQL("DELETE FROM raw_notifications WHERE event_id = " + rawId)

        val retrievedMsg = messageDao.getMessageById("m1")
        assertNotNull(retrievedMsg)
        assertNull(retrievedMsg?.rawEventId)
    }
}
