package com.hackfuture.trading.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton @Named("api_base_url")
    fun provideApiBaseUrl(): String = "http://10.0.2.2:8088/"

    @Provides @Singleton @Named("ws_base_url")
    fun provideWsBaseUrl(): String = "ws://10.0.2.2:8088/ws"
}
