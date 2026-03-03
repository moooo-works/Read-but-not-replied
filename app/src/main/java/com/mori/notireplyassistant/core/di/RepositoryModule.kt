package com.mori.notireplyassistant.core.di

import com.mori.notireplyassistant.core.database.NotiReplyDatabase
import com.mori.notireplyassistant.core.domain.scheduler.ReminderScheduler
import com.mori.notireplyassistant.core.repository.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNotificationRepository(
        db: NotiReplyDatabase,
        scheduler: ReminderScheduler
    ): NotificationRepository {
        return NotificationRepository(db, scheduler)
    }
}
