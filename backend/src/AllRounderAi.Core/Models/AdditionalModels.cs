using System;
using System.Collections.Generic;

namespace AllRounderAi.Core.Models
{
    public class OptionStrike
    {
        public decimal StrikePrice { get; set; }
        public OptionLeg? Call { get; set; }
        public OptionLeg? Put { get; set; }
    }

    public class OptionLeg
    {
        public string TradingSymbol { get; set; } = string.Empty;
        public string OptionType { get; set; } = "CE"; // CE or PE
        public decimal StrikePrice { get; set; }
        public decimal LastPrice { get; set; }
        public long OpenInterest { get; set; }
        public long ChangeInOpenInterest { get; set; }
        public long Volume { get; set; }
        public decimal ImpliedVolatility { get; set; }
        public decimal Delta { get; set; }
    }

    public class ProtectiveHedgeStrategy
    {
        public string StrategyName { get; set; } = string.Empty; // Protective Put or Protective Call
        public string PptSlideReference { get; set; } = string.Empty; // Slide 68 or Slide 69
        public string UnderlyingSymbol { get; set; } = string.Empty;
        public decimal FuturePrice { get; set; }
        public decimal OptionStrikePrice { get; set; }
        public decimal OptionPremium { get; set; }
        public string OptionType { get; set; } = "PE"; // PE for Protective Put, CE for Protective Call
        public decimal MaxProfit { get; set; } // Limited or Unlimited
        public decimal MaxLoss { get; set; }
        public decimal BreakevenPrice { get; set; }
        public List<PayoffPoint> PayoffCurve { get; set; } = new();
    }

    public class PayoffPoint
    {
        public decimal UnderlyingPrice { get; set; }
        public decimal FuturePnl { get; set; }
        public decimal OptionPnl { get; set; }
        public decimal NetPnl { get; set; }
    }

    public class BacktestRequest
    {
        public string StrategyId { get; set; } = string.Empty;
        public string Symbol { get; set; } = string.Empty;
        public string Timeframe { get; set; } = "5m";
        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }
        public decimal InitialCapital { get; set; } = 100000m;
        public decimal RiskRewardRatio { get; set; } = 1.0m;
        public decimal SlippagePercent { get; set; } = 0.05m;
        public decimal BrokeragePerTrade { get; set; } = 20.0m;
        public StrategyConfig Config { get; set; } = new();
    }

    public class BacktestTrade
    {
        public int TradeNumber { get; set; }
        public DateTime EntryTime { get; set; }
        public DateTime ExitTime { get; set; }
        public SignalDirection Side { get; set; }
        public decimal EntryPrice { get; set; }
        public decimal ExitPrice { get; set; }
        public decimal TargetPrice { get; set; }
        public decimal StopLossPrice { get; set; }
        public int Quantity { get; set; }
        public decimal GrossPnl { get; set; }
        public decimal NetPnl { get; set; }
        public string ExitReason { get; set; } = string.Empty; // TargetHit, StopLossHit
    }

    public class BacktestResult
    {
        public string BacktestId { get; set; } = Guid.NewGuid().ToString();
        public string StrategyId { get; set; } = string.Empty;
        public string Symbol { get; set; } = string.Empty;
        public int TotalTrades { get; set; }
        public int WinningTrades { get; set; }
        public int LosingTrades { get; set; }
        public decimal WinRate { get; set; }
        public decimal TotalNetPnl { get; set; }
        public decimal ProfitFactor { get; set; }
        public decimal MaxDrawdown { get; set; }
        public decimal MaxDrawdownPercent { get; set; }
        public decimal SharpeRatio { get; set; }
        public List<BacktestTrade> Trades { get; set; } = new();
    }

    public class PaperAccount
    {
        public string AccountId { get; set; } = "FP_USER_88291";
        public string ProviderName { get; set; } = "FrontPage";
        public decimal Balance { get; set; } = 1000000m;
        public decimal AvailableMargin { get; set; } = 1000000m;
        public decimal RealizedPnl { get; set; } = 0m;
        public decimal UnrealizedPnl { get; set; } = 0m;
    }

    public class PaperOrder
    {
        public string OrderId { get; set; } = Guid.NewGuid().ToString();
        public string SignalId { get; set; } = string.Empty;
        public string TradingSymbol { get; set; } = string.Empty;
        public string Exchange { get; set; } = "NSE";
        public SignalDirection Side { get; set; }
        public int Quantity { get; set; }
        public decimal Price { get; set; }
        public decimal TargetPrice { get; set; }
        public decimal StopLossPrice { get; set; }
        public string Status { get; set; } = "FILLED"; // PENDING, FILLED, REJECTED, CANCELLED
        public DateTime PlacedAt { get; set; } = DateTime.UtcNow;
    }

    public class PaperPosition
    {
        public string PositionId { get; set; } = Guid.NewGuid().ToString();
        public string TradingSymbol { get; set; } = string.Empty;
        public string Exchange { get; set; } = "NSE";
        public SignalDirection Side { get; set; } = SignalDirection.Buy;
        public int Quantity { get; set; }
        public decimal BuyPrice { get; set; }
        public decimal CurrentPrice { get; set; }
        public decimal TargetPrice { get; set; }
        public decimal StopLossPrice { get; set; }
        public decimal UnrealizedPnl => Side == SignalDirection.Buy ? Quantity * (CurrentPrice - BuyPrice) : Quantity * (BuyPrice - CurrentPrice);
        public decimal PnlPercent => BuyPrice > 0 ? Math.Round(((Side == SignalDirection.Buy ? CurrentPrice - BuyPrice : BuyPrice - CurrentPrice) / BuyPrice) * 100m, 2) : 0m;
    }
}
