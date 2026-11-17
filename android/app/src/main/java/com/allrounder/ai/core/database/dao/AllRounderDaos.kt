package com.allrounder.ai.core.database.dao

import androidx.room.*
import com.allrounder.ai.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrumentDao {
    @Query("SELECT * FROM instruments WHERE isActive = 1 ORDER BY symbol ASC")
    fun getAllInstruments(): Flow<List<InstrumentEntity>>

    @Query("SELECT * FROM instruments WHERE segment = :segment AND isActive = 1")
    fun getInstrumentsBySegment(segment: String): Flow<List<InstrumentEntity>>

    @Query("SELECT * FROM instruments WHERE instrumentId = :instrumentId")
    suspend fun getInstrumentById(instrumentId: String): InstrumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruments(instruments: List<InstrumentEntity>)
}

@Dao
interface SignalDao {
    @Query("SELECT * FROM trading_signals ORDER BY signalTimestamp DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM trading_signals WHERE status = :status ORDER BY signalTimestamp DESC")
    fun getSignalsByStatus(status: String): Flow<List<SignalEntity>>

    @Query("SELECT * FROM trading_signals WHERE signalId = :signalId")
    suspend fun getSignalById(signalId: String): SignalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<SignalEntity>)

    @Update
    suspend fun updateSignal(signal: SignalEntity)
}

@Dao
interface CandleDao {
    @Query("SELECT * FROM historical_candles WHERE instrumentId = :instrumentId AND timeframe = :timeframe ORDER BY timestamp ASC")
    fun getCandles(instrumentId: String, timeframe: String): Flow<List<CandleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<CandleEntity>)
}

@Dao
interface PaperTradingDao {
    @Query("SELECT * FROM paper_orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<PaperOrderEntity>>

    @Query("SELECT * FROM paper_positions")
    fun getAllPositions(): Flow<List<PaperPositionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: PaperOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePositions(positions: List<PaperPositionEntity>)
}

@Dao
interface BacktestDao {
    @Query("SELECT * FROM backtest_results ORDER BY executedAt DESC")
    fun getAllBacktestResults(): Flow<List<BacktestResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBacktestResult(result: BacktestResultEntity)
}
