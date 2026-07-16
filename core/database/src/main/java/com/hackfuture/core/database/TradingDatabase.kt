package com.hackfuture.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hackfuture.core.database.dao.MarketDataDao
import com.hackfuture.core.database.dao.OrderDao
import com.hackfuture.core.database.dao.PositionDao
import com.hackfuture.core.database.entity.MarketDataEntity
import com.hackfuture.core.database.entity.OrderEntity
import com.hackfuture.core.database.entity.PositionEntity
import com.hackfuture.core.database.util.Converters

/**
 * Room 本地数据库
 * 用于离线缓存行情数据、订单记录和持仓信息
 */
@Database(
    entities = [
        MarketDataEntity::class,
        OrderEntity::class,
        PositionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TradingDatabase : RoomDatabase() {

    abstract fun marketDataDao(): MarketDataDao
    abstract fun orderDao(): OrderDao
    abstract fun positionDao(): PositionDao

    companion object {
        const val DATABASE_NAME = "blackfuture_trading.db"
    }
}
