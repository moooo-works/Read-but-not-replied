package com.mori.notireplyassistant.core.di

import android.content.Context
import androidx.work.WorkManager
import com.mori.notireplyassistant.core.domain.scheduler.ReminderScheduler
import com.mori.notireplyassistant.service.worker.AndroidNotificationPublisher
import com.mori.notireplyassistant.service.worker.NotificationPublisher
import com.mori.notireplyassistant.service.worker.WorkManagerReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkerModuleBinds {

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(
        workManagerReminderScheduler: WorkManagerReminderScheduler
    ): ReminderScheduler

    @Binds
    @Singleton
    abstract fun bindNotificationPublisher(
        androidNotificationPublisher: AndroidNotificationPublisher
    ): NotificationPublisher
}

@Module
@InstallIn(SingletonComponent::class)
object WorkerModuleProvides {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
