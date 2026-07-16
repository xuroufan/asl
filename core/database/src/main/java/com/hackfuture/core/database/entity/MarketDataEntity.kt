package com.hackfuture.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "market_data",
    indices = [
        Index(value = ["symbol", "timestamp"], unique = true),
        Index(value = ["symbol"]),
    ],
)
data class MarketDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val name: String = "",
    val price: Double,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val open: Double = 0.0,
    val close: Double = 0.0,
    val volume: Long = 0L,
    val timestamp: Long,
)

@Entity(
    tableName = "candle_data",
    indices = [
        Index(value = ["symbol", "interval", "timestamp"], unique = true),
    ],
)
data class CandleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val interval: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val timestamp: Long,
)

@Entity(
    tableName = "order_book",
    indices = [Index(value = ["symbol"])],
)
data class OrderBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val bids: String, // JSON string
    val asks: String, // JSON string
    val timestamp: Long,
)
