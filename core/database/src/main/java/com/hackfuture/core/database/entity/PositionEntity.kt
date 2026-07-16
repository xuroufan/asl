package com.hackfuture.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "positions",
    indices = [
        Index(value = ["positionId"], unique = true),
        Index(value = ["symbol"]),
    ],
)
data class PositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val positionId: String,
    val symbol: String,
    val side: String,
    val quantity: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val unrealizedPnl: Double = 0.0,
    val unrealizedPnlPercent: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val marginUsed: Double = 0.0,
    val leverage: Int = 1,
    val liquidationPrice: Double? = null,
    val openedAt: Long,
    val updatedAt: Long,
)
