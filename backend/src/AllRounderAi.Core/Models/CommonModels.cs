using System;
using System.Collections.Generic;

namespace AllRounderAi.Core.Models
{
    public class Candle
    {
        public DateTime Timestamp { get; set; }
        public decimal Open { get; set; }
        public decimal High { get; set; }
        public decimal Low { get; set; }
        public decimal Close { get; set; }
        public long Volume { get; set; }
        public long OpenInterest { get; set; }

        public decimal Body => Math.Abs(Close - Open);
        public decimal Range => High - Low;
        public decimal UpperWick => High - Math.Max(Open, Close);
        public decimal LowerWick => Math.Min(Open, Close) - Low;
        public bool IsGreen => Close >= Open;
        public bool IsRed => Close < Open;
    }

    public enum ExchangeSegment
    {
        Equity,
        Futures,
        Options,
        Index,
        Commodity
    }

    public class Instrument
    {
        public string InstrumentId { get; set; } = Guid.NewGuid().ToString();
        public string Exchange { get; set; } = "NSE"; // NSE, BSE, MCX
        public string Symbol { get; set; } = string.Empty;
        public string TradingSymbol { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public ExchangeSegment Segment { get; set; } = ExchangeSegment.Equity;
        public int LotSize { get; set; } = 1;
        public decimal TickSize { get; set; } = 0.05m;
        public bool IsFnoEligible { get; set; }
        public decimal LastPrice { get; set; }
        public decimal ChangePercent { get; set; }
        public long Volume { get; set; }
    }

    public enum SignalDirection
    {
        Buy,
        Sell,
        Neutral
    }

    public enum SignalStatus
    {
        New,
        Confirmed,
        EarlyIntrabar,
        ExecutedPaper,
        TargetHit,
        StopLossHit,
        Expired,
        Cancelled
    }

    public class ConditionResult
    {
        public string ConditionName { get; set; } = string.Empty;
        public bool IsSatisfied { get; set; }
        public string ExpectedValue { get; set; } = string.Empty;
        public string ActualValue { get; set; } = string.Empty;
        public string Notes { get; set; } = string.Empty;
    }

    public class TradingSignal
    {
        public string SignalId { get; set; } = Guid.NewGuid().ToString();
        public string InstrumentId { get; set; } = string.Empty;
        public string TradingSymbol { get; set; } = string.Empty;
        public string Exchange { get; set; } = "NSE";
        public string StrategyId { get; set; } = string.Empty;
        public string StrategyName { get; set; } = string.Empty;
        public string PptSlideReference { get; set; } = string.Empty;
        public SignalDirection SignalType { get; set; }
        public SignalStatus Status { get; set; } = SignalStatus.New;
        public string Timeframe { get; set; } = "5m";
        public DateTime SignalTimestamp { get; set; } = DateTime.UtcNow;
        public decimal EntryPrice { get; set; }
        public decimal TargetPrice { get; set; }
        public decimal StopLossPrice { get; set; }
        public decimal RiskRewardRatio { get; set; } = 1.0m;
        public decimal QualityScore { get; set; } // 0 - 100
        public string MarketTrend { get; set; } = "Reversal";
        public string ConfirmationMode { get; set; } = "CANDLE_CLOSE";
        public string AnalysisSummary { get; set; } = string.Empty;
        public List<ConditionResult> Conditions { get; set; } = new();
    }

    public class StrategyConfig
    {
        public string StrategyId { get; set; } = string.Empty;
        public string Timeframe { get; set; } = "5m";
        public string ConfirmationMode { get; set; } = "CANDLE_CLOSE"; // CANDLE_CLOSE or INTRABAR_EARLY
        
        // 7-10 Point rule parameters (configurable per instrument)
        public decimal MinCandleBodyPoints { get; set; } = 7.0m;
        public decimal MaxCandleBodyPoints { get; set; } = 10.0m;
        public decimal MinBodyPercentage { get; set; } = 0.2m; // For equities
        public decimal MaxBodyPercentage { get; set; } = 0.8m;

        // Indicator parameters
        public int BollingerPeriod { get; set; } = 20;
        public decimal BollingerStdDev { get; set; } = 2.0m;
        
        public int MacdFast { get; set; } = 12;
        public int MacdSlow { get; set; } = 26;
        public int MacdSignal { get; set; } = 9;

        public int SupertrendLength { get; set; } = 20;
        public decimal SupertrendMultiplier { get; set; } = 2.0m;

        public int EmaFast { get; set; } = 50;
        public int EmaSlow { get; set; } = 200;
        public decimal RiskRewardRatio { get; set; } = 1.0m;
    }
}
