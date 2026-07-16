package com.hackfuture.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackfuture.core.database.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE symbol = :symbol ORDER BY createdAt DESC")
    fun observeBySymbol(symbol: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    suspend fun getByOrderId(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OrderEntity)

    @Query("UPDATE orders SET status = :status, filledQuantity = :filledQuantity, averageFilledPrice = :avgPrice, updatedAt = :updatedAt WHERE orderId = :orderId")
    suspend fun updateStatus(
        orderId: String,
        status: String,
        filledQuantity: Double,
        avgPrice: Double?,
        updatedAt: Long,
    )

    @Query("DELETE FROM orders")
    suspend fun deleteAll()
}
