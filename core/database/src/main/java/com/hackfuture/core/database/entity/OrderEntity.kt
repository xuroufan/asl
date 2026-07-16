package com.hackfuture.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderId"], unique = true),
        Index(value = ["symbol"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
    ],
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: String,
    val symbol: String,
    val type: String,
    val side: String,
    val status: String,
    val price: Double? = null,
    val stopPrice: Double? = null,
    val quantity: Double,
    val filledQuantity: Double = 0.0,
    val averageFilledPrice: Double? = null,
    val totalAmount: Double,
    val fee: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long,
)
