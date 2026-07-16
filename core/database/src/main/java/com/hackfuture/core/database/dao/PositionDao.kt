package com.hackfuture.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackfuture.core.database.entity.PositionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {

    @Query("SELECT * FROM positions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE symbol = :symbol ORDER BY updatedAt DESC")
    fun observeBySymbol(symbol: String): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE positionId = :positionId")
    suspend fun getByPositionId(positionId: String): PositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PositionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PositionEntity)

    @Query("DELETE FROM positions WHERE positionId = :positionId")
    suspend fun deleteByPositionId(positionId: String)

    @Query("DELETE FROM positions")
    suspend fun deleteAll()
}
