package com.allrounder.ai.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trading_signals",
    indices = [
        Index(value = ["signalTimestamp"]),
        Index(value = ["status"]),
        Index(value = ["instrumentId"])
    ]
)
data class SignalEntity(
    @PrimaryKey
    val signalId: String,
    val instrumentId: String,
    val tradingSymbol: String,
    val exchange: String,
    val strategyId: String,
    val strategyName: String,
    val signalType: String, // "BUY", "SELL"
    val status: String, // "NEW", "CONFIRMED", "EARLY_INTRABAR", "EXECUTED_PAPER", "TARGET_HIT", "STOP_LOSS_HIT", "EXPIRED", "CANCELLED"
    val timeframe: String,
    val signalTimestamp: Long,
    val currentPrice: Double,
    val entryPrice: Double,
    val targetPrice: Double,
    val stopLossPrice: Double,
    val riskRewardRatio: Double = 1.0,
    val qualityScore: Double,
    val marketTrend: String,
    val confirmationMode: String, // "CANDLE_CLOSE", "INTRABAR_EARLY"
    val analysisSummary: String,
    val matchedConditionsJson: String,
    val failedConditionsJson: String
)
