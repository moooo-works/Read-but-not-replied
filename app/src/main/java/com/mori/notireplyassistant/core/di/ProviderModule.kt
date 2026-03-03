package com.mori.notireplyassistant.core.di

import com.mori.notireplyassistant.core.data.provider.AndroidAppInfoProvider
import com.mori.notireplyassistant.core.domain.provider.AppInfoProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {

    @Binds
    @Singleton
    abstract fun bindAppInfoProvider(
        androidAppInfoProvider: AndroidAppInfoProvider
    ): AppInfoProvider
}
