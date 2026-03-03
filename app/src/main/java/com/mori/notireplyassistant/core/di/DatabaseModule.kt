package com.mori.notireplyassistant.core.di

import android.content.Context
import androidx.room.Room
import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.database.dao.ConversationDao
import com.mori.notireplyassistant.core.database.dao.MessageDao
import com.mori.notireplyassistant.core.database.dao.QuickReplyTemplateDao
import com.mori.notireplyassistant.core.database.dao.RawNotificationDao
import com.mori.notireplyassistant.core.database.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotiReplyDatabase {
        return Room.databaseBuilder(
            context,
            NotiReplyDatabase::class.java,
            "notireply.db"
        ).build()
    }

    @Provides
    fun provideRawNotificationDao(db: NotiReplyDatabase): RawNotificationDao = db.rawNotificationDao()

    @Provides
    fun provideConversationDao(db: NotiReplyDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideMessageDao(db: NotiReplyDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideReminderDao(db: NotiReplyDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideQuickReplyTemplateDao(db: NotiReplyDatabase): QuickReplyTemplateDao = db.quickReplyTemplateDao()
}
