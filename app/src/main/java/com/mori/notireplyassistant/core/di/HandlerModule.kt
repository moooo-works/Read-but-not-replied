package com.mori.notireplyassistant.core.di

import com.mori.notireplyassistant.core.domain.handler.ReplyHandler
import com.mori.notireplyassistant.service.handler.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HandlerModule {

    @Binds
    @Singleton
    abstract fun bindReplyHandler(
        androidReplyHandler: AndroidReplyHandler
    ): ReplyHandler

    @Binds
    @Singleton
    abstract fun bindRemoteInputComposer(
        defaultRemoteInputComposer: DefaultRemoteInputComposer
    ): RemoteInputComposer

    @Binds
    @Singleton
    abstract fun bindPendingIntentSender(
        frameworkPendingIntentSender: FrameworkPendingIntentSender
    ): PendingIntentSender

    @Binds
    @Singleton
    abstract fun bindIntentOpener(
        frameworkIntentOpener: FrameworkIntentOpener
    ): IntentOpener

    @Binds
    @Singleton
    abstract fun bindIntentFactory(
        frameworkIntentFactory: FrameworkIntentFactory
    ): IntentFactory
}
