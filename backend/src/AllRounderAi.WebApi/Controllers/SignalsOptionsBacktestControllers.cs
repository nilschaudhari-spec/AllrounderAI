using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Strategies;
using AllRounderAi.Core.Backtesting;
using AllRounderAi.Core.PaperTrading;
using AllRounderAi.Core.Services;

namespace AllRounderAi.WebApi.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    public class SignalsController : ControllerBase
    {
        [HttpGet("live")]
        public ActionResult<IEnumerable<TradingSignal>> GetLiveSignals([FromQuery] string? status = null, [FromQuery] string? exchange = null)
        {
            var signals = new List<TradingSignal>
            {
                new()
                {
                    SignalId = "sig-nifty-5m-01",
                    InstrumentId = "nse-idx-nifty",
                    TradingSymbol = "NIFTY 50",
                    Exchange = "NSE",
                    StrategyId = "DAILY_INTRADAY_5M",
                    StrategyName = "Daily Intraday Technique (5-Minute)",
                    PptSlideReference = "Slides 59-62",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = "5m",
                    SignalTimestamp = DateTime.UtcNow,
                    EntryPrice = 24350.0m,
                    TargetPrice = 24390.0m,
                    StopLossPrice = 24310.0m,
                    RiskRewardRatio = 1.0m,
                    QualityScore = 96.0m,
                    MarketTrend = "Intraday Reversal Confluence",
                    ConfirmationMode = "CANDLE_CLOSE",
                    AnalysisSummary = "Triple Confluence: Supertrend(20,2) Bullish, Price > VWAP, MACD Bullish crossover on 5m chart. Strict 1:1 Target & Stop Loss configured.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Supertrend (20,2)", IsSatisfied = true, ExpectedValue = "Bullish", ActualValue = "Bullish (Green)" },
                        new() { ConditionName = "VWAP Support", IsSatisfied = true, ExpectedValue = "Price > VWAP", ActualValue = "24350 > 24320" },
                        new() { ConditionName = "MACD Concurrence", IsSatisfied = true, ExpectedValue = "MACD > Signal", ActualValue = "12.4 > 8.1" }
                    }
                },
                new()
                {
                    SignalId = "sig-reliance-lb-02",
                    InstrumentId = "nse-eq-reliance",
                    TradingSymbol = "RELIANCE",
                    Exchange = "NSE",
                    StrategyId = "LOW_BREAK_CLOSE_UPSIDE",
                    StrategyName = "Low Break, Close Upside",
                    PptSlideReference = "Slides 13-14",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = "15m",
                    SignalTimestamp = DateTime.UtcNow.AddMinutes(-12),
                    EntryPrice = 1363.0m,
                    TargetPrice = 1373.0m,
                    StopLossPrice = 1353.0m,
                    RiskRewardRatio = 1.0m,
                    QualityScore = 93.0m,
                    MarketTrend = "Reversal from Liquidity Sweep",
                    ConfirmationMode = "CANDLE_CLOSE",
                    AnalysisSummary = "Prior red candle low broken, followed by strong green candle closing upside with PPT candle body rules satisfied.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Prior Low Broken", IsSatisfied = true, ExpectedValue = "Low < C1 Low", ActualValue = "1353.0 < 1354.2" },
                        new() { ConditionName = "Close Upside", IsSatisfied = true, ExpectedValue = "Close > C1 Open", ActualValue = "1363.0 > 1358.5" },
                        new() { ConditionName = "Candle Body Rule", IsSatisfied = true, ExpectedValue = "7 - 10 pt equiv", ActualValue = "10.0 pts" }
                    }
                },
                new()
                {
                    SignalId = "sig-tcs-bse-hammer-03",
                    InstrumentId = "bse-eq-tcs",
                    TradingSymbol = "TCS.BO",
                    Exchange = "BSE",
                    StrategyId = "HAMMER",
                    StrategyName = "Hammer Bottom Reversal",
                    PptSlideReference = "Slides 5-8",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = "15m",
                    SignalTimestamp = DateTime.UtcNow.AddMinutes(-20),
                    EntryPrice = 2380.2m,
                    TargetPrice = 2405.0m,
                    StopLossPrice = 2355.4m,
                    RiskRewardRatio = 1.0m,
                    QualityScore = 91.5m,
                    MarketTrend = "Support Demand Absorption",
                    ConfirmationMode = "CANDLE_CLOSE",
                    AnalysisSummary = "Clear 4-bar downtrend terminated with a classic Hammer on BSE. Lower shadow >= 2.4x body length. Confirmed close above high.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Downtrend Preceding", IsSatisfied = true, ExpectedValue = ">= 3 Red Bars", ActualValue = "4 Consecutive Red Bars" },
                        new() { ConditionName = "Lower Shadow Ratio", IsSatisfied = true, ExpectedValue = ">= 2.0x Body", ActualValue = "2.42x Body Length" },
                        new() { ConditionName = "Upper Shadow Limit", IsSatisfied = true, ExpectedValue = "<= 15% Range", ActualValue = "4.2%" }
                    }
                },
                new()
                {
                    SignalId = "sig-tatamotors-maru-04",
                    InstrumentId = "nse-eq-tatamotors",
                    TradingSymbol = "TATAMOTORS",
                    Exchange = "NSE",
                    StrategyId = "MARUBOZU",
                    StrategyName = "Marubozu Momentum Breakout",
                    PptSlideReference = "Slides 33-36",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = "15m",
                    SignalTimestamp = DateTime.UtcNow.AddMinutes(-5),
                    EntryPrice = 790.0m,
                    TargetPrice = 804.0m,
                    StopLossPrice = 776.0m,
                    RiskRewardRatio = 1.0m,
                    QualityScore = 94.0m,
                    MarketTrend = "Institutional Order Flow Surge",
                    ConfirmationMode = "CANDLE_CLOSE",
                    AnalysisSummary = "Bullish Marubozu: Candle body represents 94% of entire range with 2.1x average volume. Open = Low with negligible upper wick.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Body to Range Ratio", IsSatisfied = true, ExpectedValue = ">= 90%", ActualValue = "94.2%" },
                        new() { ConditionName = "Volume Surge", IsSatisfied = true, ExpectedValue = ">= 1.5x 20SMA", ActualValue = "2.14x 20SMA" }
                    }
                },
                new()
                {
                    SignalId = "sig-sensex-gc-05",
                    InstrumentId = "bse-idx-sensex",
                    TradingSymbol = "SENSEX",
                    Exchange = "BSE",
                    StrategyId = "GOLDEN_CROSS",
                    StrategyName = "Golden Cross & 200 EMA Re-entry",
                    PptSlideReference = "Slides 51-58",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = "Daily",
                    SignalTimestamp = DateTime.UtcNow.AddHours(-2),
                    EntryPrice = 79880.0m,
                    TargetPrice = 80680.0m,
                    StopLossPrice = 79080.0m,
                    RiskRewardRatio = 1.0m,
                    QualityScore = 95.5m,
                    MarketTrend = "Macro Bullish Swing Trend",
                    ConfirmationMode = "CANDLE_CLOSE",
                    AnalysisSummary = "S&P BSE Sensex 50 EMA trading above 200 EMA with clean retest bounce off the rising 200-day moving average.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "EMA Cross", IsSatisfied = true, ExpectedValue = "50 EMA > 200 EMA", ActualValue = "79420 > 77850" },
                        new() { ConditionName = "200 EMA Retest", IsSatisfied = true, ExpectedValue = "Bounce Confirmed", ActualValue = "Bullish Engulfing off 200 EMA" }
                    }
                },
                new()
                {
                    SignalId = "sig-banknifty-lb-02",
                    InstrumentId = "nse-idx-banknifty",
                    TradingSymbol = "BANK NIFTY",
                    Exchange = "NSE",
                    StrategyId = "LOW_BREAK_CLOSE_UPSIDE",
                    StrategyName = "Low Break, Close Upside",
                    PptSlideReference = "Slides 13-14",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = "15m",
                    SignalTimestamp = DateTime.UtcNow.AddMinutes(-15),
                    EntryPrice = 51240.0m,
                    TargetPrice = 51320.0m,
                    StopLossPrice = 51160.0m,
                    RiskRewardRatio = 1.0m,
                    QualityScore = 90.0m,
                    MarketTrend = "Reversal from Downtrend",
                    ConfirmationMode = "CANDLE_CLOSE",
                    AnalysisSummary = "Red candle low broken, followed by strong green close upside satisfying PPT 7-10 pt candle body rule.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Low Broken", IsSatisfied = true, ExpectedValue = "Low < C1 Low", ActualValue = "51160 < 51175" },
                        new() { ConditionName = "Close Upside", IsSatisfied = true, ExpectedValue = "Close > C1 Open", ActualValue = "51240 > 51210" },
                        new() { ConditionName = "7-10 Point Rule", IsSatisfied = true, ExpectedValue = "7 - 10 pts", ActualValue = "8.5 pts" }
                    }
                }
            };

            if (!string.IsNullOrWhiteSpace(exchange) && !exchange.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                return Ok(signals.Where(s => s.Exchange.Equals(exchange, StringComparison.OrdinalIgnoreCase)));
            }

            return Ok(signals);
        }
    }

    [ApiController]
    [Route("api/v1/[controller]")]
    public class OptionsController : ControllerBase
    {
        [HttpGet("chain")]
        public ActionResult<object> GetOptionChain([FromQuery] string underlying = "BANKNIFTY")
        {
            decimal ltp = underlying.Contains("BANK") ? 51240m : 24350m;
            decimal strikeStep = underlying.Contains("BANK") ? 100m : 50m;
            decimal atmStrike = Math.Round(ltp / strikeStep) * strikeStep;

            var strikes = new List<OptionStrike>();
            for (int i = -5; i <= 5; i++)
            {
                decimal strike = atmStrike + (i * strikeStep);
                strikes.Add(new OptionStrike
                {
                    StrikePrice = strike,
                    Call = new OptionLeg
                    {
                        TradingSymbol = $"{underlying}26NOV{strike}CE",
                        OptionType = "CE",
                        StrikePrice = strike,
                        LastPrice = Math.Max(20m, (ltp - strike) + 120m),
                        OpenInterest = 120000 + (Math.Abs(i) * 15000),
                        ChangeInOpenInterest = 4500,
                        Volume = 85000,
                        Delta = 0.50m - (i * 0.08m)
                    },
                    Put = new OptionLeg
                    {
                        TradingSymbol = $"{underlying}26NOV{strike}PE",
                        OptionType = "PE",
                        StrikePrice = strike,
                        LastPrice = Math.Max(20m, (strike - ltp) + 115m),
                        OpenInterest = 145000 + (Math.Abs(i) * 12000),
                        ChangeInOpenInterest = -2100,
                        Volume = 92000,
                        Delta = -0.50m - (i * 0.08m)
                    }
                });
            }

            return Ok(new
            {
                underlying,
                underlyingPrice = ltp,
                atmStrike,
                expiryDate = "2026-11-26",
                strikes
            });
        }

        [HttpGet("protective-put")]
        public ActionResult<ProtectiveHedgeStrategy> GetProtectivePut(
            [FromQuery] string underlying = "BANKNIFTY", 
            [FromQuery] decimal futurePrice = 51240m, 
            [FromQuery] decimal atmStrike = 51200m, 
            [FromQuery] decimal putPremium = 210m)
        {
            var strategy = OptionsStrategyEngine.BuildProtectivePut(underlying, futurePrice, atmStrike, putPremium);
            return Ok(strategy);
        }

        [HttpGet("protective-call")]
        public ActionResult<ProtectiveHedgeStrategy> GetProtectiveCall(
            [FromQuery] string underlying = "BANKNIFTY", 
            [FromQuery] decimal futurePrice = 51240m, 
            [FromQuery] decimal atmStrike = 51200m, 
            [FromQuery] decimal callPremium = 240m)
        {
            var strategy = OptionsStrategyEngine.BuildProtectiveCall(underlying, futurePrice, atmStrike, callPremium);
            return Ok(strategy);
        }
    }

    [ApiController]
    [Route("api/v1/[controller]")]
    public class BacktestController : ControllerBase
    {
        private readonly BacktestEngine _engine = new();

        [HttpPost("execute")]
        public ActionResult<BacktestResult> ExecuteBacktest([FromBody] BacktestRequest request)
        {
            var inst = new Instrument
            {
                Symbol = request.Symbol,
                TradingSymbol = request.Symbol,
                Segment = ExchangeSegment.Index,
                LotSize = 50,
                LastPrice = 24350m
            };

            // Generate 300 synthetic candles to run backtest
            var candles = new List<Candle>();
            decimal p = 24000m;
            var dt = DateTime.UtcNow.AddDays(-60);
            var rand = new Random(101);

            for (int i = 0; i < 300; i++)
            {
                decimal chg = (decimal)(rand.NextDouble() * 50.0 - 24.5);
                decimal open = p;
                decimal close = p + chg;
                decimal high = Math.Max(open, close) + 15m;
                decimal low = Math.Min(open, close) - 15m;

                candles.Add(new Candle
                {
                    Timestamp = dt.AddMinutes(5 * i),
                    Open = open,
                    High = high,
                    Low = low,
                    Close = close,
                    Volume = 50000
                });
                p = close;
            }

            IStrategyEngine strategy = request.StrategyId switch
            {
                "HAMMER" => new HammerStrategy(),
                "LOW_BREAK_CLOSE_UPSIDE" => new LowBreakCloseUpsideStrategy(),
                "GOLDEN_CROSS" => new GoldenCrossStrategy(),
                _ => new DailyIntradayStrategy()
            };

            var result = _engine.RunBacktest(candles, inst, strategy, request);
            return Ok(result);
        }
    }

    [ApiController]
    [Route("api/v1/[controller]")]
    public class PaperTradingController : ControllerBase
    {
        private static readonly FrontPagePaperTradingBroker _broker = new();

        [HttpGet("portfolio")]
        public async Task<ActionResult<object>> GetPortfolio()
        {
            var account = await _broker.GetAccountAsync();
            var positions = await _broker.GetPositionsAsync();
            var orders = await _broker.GetOrdersAsync();

            return Ok(new
            {
                account,
                positions,
                orders
            });
        }

        [HttpPost("order")]
        public async Task<ActionResult<PaperOrder>> PlaceOrder([FromBody] TradingSignal signal, [FromQuery] int quantity = 15)
        {
            var order = await _broker.PlaceOrderAsync(signal, quantity);
            return Ok(order);
        }
    }
}
