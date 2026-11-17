package com.allrounder.ai.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "historical_candles",
    indices = [
        Index(value = ["instrumentId", "timeframe", "timestamp"], unique = true)
    ]
)
data class CandleEntity(
    @PrimaryKey(autoGenerate = true)
    val candleId: Long = 0,
    val instrumentId: String,
    val timeframe: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val openInterest: Long = 0
)

@Entity(tableName = "paper_orders")
data class PaperOrderEntity(
    @PrimaryKey
    val orderId: String,
    val signalId: String?,
    val instrumentId: String,
    val tradingSymbol: String,
    val orderSide: String, // "BUY", "SELL"
    val orderType: String, // "MARKET", "LIMIT"
    val quantity: Int,
    val price: Double,
    val targetPrice: Double,
    val stopLossPrice: Double,
    val status: String, // "PENDING", "FILLED", "REJECTED", "CANCELLED"
    val filledPrice: Double = 0.0,
    val timestamp: Long
)

@Entity(tableName = "paper_positions")
data class PaperPositionEntity(
    @PrimaryKey
    val positionId: String,
    val instrumentId: String,
    val tradingSymbol: String,
    val quantity: Int,
    val averageBuyPrice: Double,
    val averageSellPrice: Double,
    val currentMarketPrice: Double,
    val unrealizedPnl: Double,
    val realizedPnl: Double
)

@Entity(tableName = "backtest_results")
data class BacktestResultEntity(
    @PrimaryKey
    val backtestId: String,
    val strategyId: String,
    val instrumentSymbol: String,
    val timeframe: String,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val netPnl: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val sharpeRatio: Double,
    val executedAt: Long
)
