package com.allrounder.ai.domain.model

data class Instrument(
    val instrumentId: String,
    val exchange: String,
    val symbol: String,
    val tradingSymbol: String,
    val name: String,
    val segment: String,
    val lotSize: Int = 1,
    val tickSize: Double = 0.05,
    val lastPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val isFnoEligible: Boolean = false
)

data class Candle(
    val timestamp: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val openInterest: Long = 0
)

data class ConditionCheck(
    val conditionName: String,
    val isSatisfied: Boolean,
    val expectedValue: String,
    val actualValue: String,
    val notes: String = ""
)

data class Signal(
    val signalId: String,
    val tradingSymbol: String,
    val exchange: String,
    val strategyId: String,
    val strategyName: String,
    val pptSlideReference: String,
    val signalType: String, // BUY or SELL
    val status: String,
    val timeframe: String,
    val entryPrice: Double,
    val targetPrice: Double,
    val stopLossPrice: Double,
    val riskRewardRatio: Double = 1.0,
    val qualityScore: Double,
    val marketTrend: String,
    val confirmationMode: String,
    val analysisSummary: String,
    val conditions: List<ConditionCheck> = emptyList()
)

data class OptionStrikeInfo(
    val strikePrice: Double,
    val callLtp: Double,
    val callOi: Long,
    val putLtp: Double,
    val putOi: Long
)

data class ProtectivePayoff(
    val strategyName: String,
    val slideReference: String,
    val underlying: String,
    val futurePrice: Double,
    val strikePrice: Double,
    val premium: Double,
    val maxProfit: String,
    val maxLoss: Double,
    val breakeven: Double
)

data class BacktestSummary(
    val backtestId: String,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val netPnl: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val sharpeRatio: Double
)

data class PaperPortfolio(
    val accountId: String,
    val provider: String,
    val balance: Double,
    val availableMargin: Double,
    val realizedPnl: Double,
    val unrealizedPnl: Double
)
