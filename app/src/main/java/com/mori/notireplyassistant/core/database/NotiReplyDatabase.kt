package com.mori.notireplyassistant.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.mori.notireplyassistant.core.database.dao.ConversationDao
import com.mori.notireplyassistant.core.database.dao.MessageDao
import com.mori.notireplyassistant.core.database.dao.QuickReplyTemplateDao
import com.mori.notireplyassistant.core.database.dao.RawNotificationDao
import com.mori.notireplyassistant.core.database.dao.ReminderDao
import com.mori.notireplyassistant.core.database.entity.ConversationEntity
import com.mori.notireplyassistant.core.database.entity.MessageEntity
import com.mori.notireplyassistant.core.database.entity.QuickReplyTemplateEntity
import com.mori.notireplyassistant.core.database.entity.RawNotificationEntity
import com.mori.notireplyassistant.core.database.entity.ReminderEntity

@Database(
    entities = [
        RawNotificationEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ReminderEntity::class,
        QuickReplyTemplateEntity::class
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
abstract class NotiReplyDatabase : RoomDatabase() {
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun reminderDao(): ReminderDao
    abstract fun quickReplyTemplateDao(): QuickReplyTemplateDao
}
