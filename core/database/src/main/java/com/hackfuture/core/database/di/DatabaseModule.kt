package com.hackfuture.core.database.di

import android.content.Context
import androidx.room.Room
import com.hackfuture.core.database.TradingDatabase
import com.hackfuture.core.database.dao.MarketDataDao
import com.hackfuture.core.database.dao.OrderDao
import com.hackfuture.core.database.dao.PositionDao
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
    fun provideDatabase(@ApplicationContext context: Context): TradingDatabase {
        return Room.databaseBuilder(
            context,
            TradingDatabase::class.java,
            TradingDatabase.DATABASE_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMarketDataDao(database: TradingDatabase): MarketDataDao {
        return database.marketDataDao()
    }

    @Provides
    fun provideOrderDao(database: TradingDatabase): OrderDao {
        return database.orderDao()
    }

    @Provides
    fun providePositionDao(database: TradingDatabase): PositionDao {
        return database.positionDao()
    }
}
