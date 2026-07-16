package com.hackfuture.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackfuture.core.database.entity.MarketDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketDataDao {

    @Query("SELECT * FROM market_data WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(symbol: String): Flow<MarketDataEntity?>

    @Query("SELECT * FROM market_data ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MarketDataEntity>>

    @Query("SELECT * FROM market_data WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(symbol: String): MarketDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MarketDataEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MarketDataEntity)

    @Query("DELETE FROM market_data WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM market_data")
    suspend fun deleteAll()
}
