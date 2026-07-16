package com.hackfuture.trading.di

import android.content.Context
import com.hackfuture.core.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        NetworkMonitor(context)

    @Provides @Singleton @Named("api_base_url")
    fun provideApiBaseUrl(): String = "https://dev.api.hackfuture.com/"

    @Provides @Singleton @Named("ws_base_url")
    fun provideWsBaseUrl(): String = "wss://ws-stage.hackfuture.com/"
}
