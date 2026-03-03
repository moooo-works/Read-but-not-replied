package com.mori.notireplyassistant.core.di

import com.mori.notireplyassistant.core.common.AndroidLogger
import com.mori.notireplyassistant.core.common.Logger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun bindLogger(androidLogger: AndroidLogger): Logger
}
