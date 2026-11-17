package com.allrounder.ai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.allrounder.ai.core.database.dao.*
import com.allrounder.ai.core.database.entity.*

@Database(
    entities = [
        InstrumentEntity::class,
        SignalEntity::class,
        CandleEntity::class,
        PaperOrderEntity::class,
        PaperPositionEntity::class,
        BacktestResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AllRounderDatabase : RoomDatabase() {
    abstract fun instrumentDao(): InstrumentDao
    abstract fun signalDao(): SignalDao
    abstract fun candleDao(): CandleDao
    abstract fun paperTradingDao(): PaperTradingDao
    abstract fun backtestDao(): BacktestDao
}
