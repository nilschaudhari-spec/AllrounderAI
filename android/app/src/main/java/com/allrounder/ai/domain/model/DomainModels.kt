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

data class PaperAccountDto(
    val accountId: String = "FP_USER_88291",
    val providerName: String = "FrontPage",
    val balance: Double = 1000000.0,
    val availableMargin: Double = 1000000.0,
    val realizedPnl: Double = 0.0,
    val unrealizedPnl: Double = 0.0
)

data class PaperPositionDto(
    val positionId: String = "",
    val tradingSymbol: String = "",
    val exchange: String = "NSE",
    val side: String = "Buy",
    val quantity: Int = 0,
    val buyPrice: Double = 0.0,
    val currentPrice: Double = 0.0,
    val targetPrice: Double = 0.0,
    val stopLossPrice: Double = 0.0,
    val unrealizedPnl: Double = 0.0,
    val pnlPercent: Double = 0.0
)

data class PaperOrderDto(
    val orderId: String = "",
    val signalId: String = "",
    val tradingSymbol: String = "",
    val exchange: String = "NSE",
    val side: String = "Buy",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val targetPrice: Double = 0.0,
    val stopLossPrice: Double = 0.0,
    val status: String = "FILLED",
    val placedAt: String = ""
)

data class PaperPortfolioResponse(
    val account: PaperAccountDto = PaperAccountDto(),
    val positions: List<PaperPositionDto> = emptyList(),
    val orders: List<PaperOrderDto> = emptyList()
)

data class PaperOrderRequest(
    val tradingSymbol: String,
    val exchange: String = "NSE",
    val signalType: String = "BUY",
    val quantity: Int = 15,
    val entryPrice: Double = 0.0,
    val targetPrice: Double = 0.0,
    val stopLossPrice: Double = 0.0,
    val strategyName: String = "FrontPage Paper Trade"
)

data class PaperPortfolio(
    val accountId: String,
    val provider: String,
    val balance: Double,
    val availableMargin: Double,
    val realizedPnl: Double,
    val unrealizedPnl: Double
)

data class StrategyInfo(
    val strategyId: String,
    val name: String,
    val category: String,
    val direction: String,
    val pptSlideReference: String,
    val description: String,
    val defaultTimeframe: String,
    val riskRewardRatio: Double = 1.0,
    val winRateBenchmark: Double = 90.0,
    val rules: List<String> = emptyList(),
    val activeSignalsCount: Int = 0
)

data class LiveQuoteDto(
    val symbol: String,
    val exchange: String = "NSE",
    val lastPrice: Double,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val dayHigh: Double = 0.0,
    val dayLow: Double = 0.0,
    val openPrice: Double = 0.0,
    val previousClose: Double = 0.0,
    val volume: Long = 0,
    val lastUpdated: String = ""
)

data class HammerConfigDto(
    var configId: String = "GLOBAL_DEFAULT",
    var patternEnabled: Boolean = true,
    var minLowerWickToBodyRatio: Double = 2.0,
    var maxUpperWickToRangeRatio: Double = 0.10,
    var maxBodyToRangeRatio: Double = 0.35,
    var minBodyPosition: Double = 0.60,
    var minCandleRangeToAtrRatio: Double = 0.50,
    var atrPeriod: Int = 14,
    var trendLookback: Int = 5,
    var requireDowntrend: Boolean = true,
    var trendMode: String = "Moderate",
    var requireSupportProximity: Boolean = false,
    var maxSupportDistanceAtr: Double = 1.0,
    var requireVolumeConfirmation: Boolean = false,
    var volumeRequirementMode: String = "Advisory",
    var minVolumeRatio: Double = 1.50,
    var confirmationMode: String = "CandleClose",
    var requireNextCandleConfirmation: Boolean = false,
    var nextCandleConfirmationMethod: String = "CloseAboveHammerHigh",
    var scoreThreshold: Double = 70.0,
    var riskRewardRatio: Double = 1.0,
    var stopLossBufferAtr: Double = 0.10
)

data class HammerCandleGeometryDto(
    val isValid: Boolean = true,
    val range: Double = 0.0,
    val body: Double = 0.0,
    val lowerWick: Double = 0.0,
    val upperWick: Double = 0.0,
    val lowerWickToBodyRatio: Double = 0.0,
    val upperWickToRangeRatio: Double = 0.0,
    val bodyToRangeRatio: Double = 0.0,
    val bodyPosition: Double = 0.0,
    val rangeToAtrRatio: Double = 0.0,
    val isBullishColor: Boolean = true,
    val isZeroBodyDoji: Boolean = false
)

data class TrendEvaluationDto(
    val isDowntrendConfirmed: Boolean = true,
    val conditionsPassed: Int = 0,
    val totalConditionsChecked: Int = 3,
    val priceDeclinePercent: Double = 0.0,
    val passedConditions: List<String> = emptyList(),
    val summaryExplanation: String = ""
)

data class SupportEvaluationDto(
    val isNearSupport: Boolean = false,
    val nearestSupportPrice: Double = 0.0,
    val supportType: String = "",
    val distanceInAtr: Double = 0.0,
    val touchesCount: Int = 0,
    val strengthLevel: String = "",
    val explanation: String = ""
)

data class VolumeEvaluationDto(
    val isConfirmed: Boolean = true,
    val currentVolume: Long = 0,
    val averageVolume: Long = 0,
    val volumeRatio: Double = 1.0,
    val volumeClassification: String = "Normal",
    val explanation: String = ""
)

data class ConfirmationEvaluationDto(
    val isConfirmed: Boolean = true,
    val confirmationStatus: String = "CONFIRMED",
    val methodUsed: String = "CandleClose",
    val confirmationPrice: Double = 0.0,
    val explanation: String = ""
)

data class HammerScoreBreakdownDto(
    val component: String,
    val maxPoints: Double,
    val awardedPoints: Double,
    val notes: String = ""
)

data class HammerScoreDto(
    val finalScore: Double = 0.0,
    val classification: String = "Strong",
    val breakdown: List<HammerScoreBreakdownDto> = emptyList(),
    val summary: String = ""
)

data class HammerDetectionDetailDto(
    val detectionId: String = "",
    val instrumentId: String = "",
    val tradingSymbol: String = "",
    val exchange: String = "NSE",
    val segment: String = "Equity",
    val timeframe: String = "5m",
    val candleTimestamp: String = "",
    val patternStatus: String = "NO_TRADE",
    val geometry: HammerCandleGeometryDto = HammerCandleGeometryDto(),
    val trend: TrendEvaluationDto = TrendEvaluationDto(),
    val support: SupportEvaluationDto = SupportEvaluationDto(),
    val volume: VolumeEvaluationDto = VolumeEvaluationDto(),
    val confirmation: ConfirmationEvaluationDto = ConfirmationEvaluationDto(),
    val score: HammerScoreDto = HammerScoreDto(),
    val atr: Double = 0.0,
    val entryPrice: Double = 0.0,
    val stopLossPrice: Double = 0.0,
    val targetPrice: Double = 0.0,
    val riskRewardRatio: Double = 1.0,
    val recommendedPositionSize: Int = 1,
    val riskAmount: Double = 0.0,
    val explainabilityBulletPoints: List<String> = emptyList(),
    val generatedSignal: Signal? = null
)

data class HammerScannerResponse(
    val totalMatches: Int = 0,
    val scope: String = "ALL",
    val exchange: String = "ALL",
    val timeframe: String = "5m",
    val results: List<HammerDetectionDetailDto> = emptyList()
)

data class HammerScanApiRequest(
    val scope: String = "ALL",
    val exchange: String = "ALL",
    val timeframe: String = "5m",
    val minScore: Double = 60.0,
    val limit: Int = 40
)

data class InverseHammerConfigDto(
    var configId: String = "GLOBAL_DEFAULT",
    var patternEnabled: Boolean = true,
    var minUpperWickToBodyRatio: Double = 2.0,
    var maxLowerWickToRangeRatio: Double = 0.10,
    var maxBodyToRangeRatio: Double = 0.35,
    var maxBodyPosition: Double = 0.40,
    var minCandleRangeToAtrRatio: Double = 0.50,
    var atrPeriod: Int = 14,
    var trendLookback: Int = 5,
    var requireDowntrend: Boolean = true,
    var trendMode: String = "Moderate",
    var requireSupportProximity: Boolean = false,
    var maxSupportDistanceAtr: Double = 1.0,
    var requireVolumeConfirmation: Boolean = false,
    var volumeRequirementMode: String = "Advisory",
    var minVolumeRatio: Double = 1.50,
    var confirmationMode: String = "CandleClose",
    var requireNextCandleConfirmation: Boolean = false,
    var nextCandleConfirmationMethod: String = "CloseAboveInverseHammerHigh",
    var scoreThreshold: Double = 70.0,
    var riskRewardRatio: Double = 1.0,
    var stopLossBufferAtr: Double = 0.10
)

data class InverseHammerCandleGeometryDto(
    val isValid: Boolean = true,
    val range: Double = 0.0,
    val body: Double = 0.0,
    val upperWick: Double = 0.0,
    val lowerWick: Double = 0.0,
    val upperWickToBodyRatio: Double = 0.0,
    val lowerWickToRangeRatio: Double = 0.0,
    val bodyToRangeRatio: Double = 0.0,
    val bodyPosition: Double = 0.0,
    val rangeToAtrRatio: Double = 0.0,
    val isBullishColor: Boolean = true,
    val isZeroBodyDoji: Boolean = false
)

data class InverseHammerScoreBreakdownDto(
    val component: String,
    val maxPoints: Double,
    val awardedPoints: Double,
    val notes: String = ""
)

data class InverseHammerScoreDto(
    val finalScore: Double = 0.0,
    val classification: String = "Strong",
    val breakdown: List<InverseHammerScoreBreakdownDto> = emptyList(),
    val summary: String = ""
)

data class InverseHammerDetectionDetailDto(
    val detectionId: String = "",
    val instrumentId: String = "",
    val tradingSymbol: String = "",
    val exchange: String = "NSE",
    val segment: String = "Equity",
    val timeframe: String = "5m",
    val candleTimestamp: String = "",
    val patternStatus: String = "NO_TRADE",
    val geometry: InverseHammerCandleGeometryDto = InverseHammerCandleGeometryDto(),
    val trend: TrendEvaluationDto = TrendEvaluationDto(),
    val support: SupportEvaluationDto = SupportEvaluationDto(),
    val volume: VolumeEvaluationDto = VolumeEvaluationDto(),
    val confirmation: ConfirmationEvaluationDto = ConfirmationEvaluationDto(),
    val score: InverseHammerScoreDto = InverseHammerScoreDto(),
    val atr: Double = 0.0,
    val entryPrice: Double = 0.0,
    val stopLossPrice: Double = 0.0,
    val targetPrice: Double = 0.0,
    val riskRewardRatio: Double = 1.0,
    val recommendedPositionSize: Int = 1,
    val riskAmount: Double = 0.0,
    val explainabilityBulletPoints: List<String> = emptyList(),
    val generatedSignal: Signal? = null
)

data class InverseHammerScannerResponse(
    val totalMatches: Int = 0,
    val scope: String = "ALL",
    val exchange: String = "ALL",
    val timeframe: String = "5m",
    val results: List<InverseHammerDetectionDetailDto> = emptyList()
)

data class InverseHammerScanApiRequest(
    val scope: String = "ALL",
    val exchange: String = "ALL",
    val timeframe: String = "5m",
    val minScore: Double = 60.0,
    val limit: Int = 40
)
