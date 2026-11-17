using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Strategies;
using AllRounderAi.Core.Backtesting;
using AllRounderAi.Core.PaperTrading;

namespace AllRounderAi.WebApi.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    public class SignalsController : ControllerBase
    {
        [HttpGet("live")]
        public ActionResult<IEnumerable<TradingSignal>> GetLiveSignals([FromQuery] string? status = null)
        {
            var signals = new List<TradingSignal>
            {
                new()
                {
                    SignalId = "sig-nifty-5m-01",
                    InstrumentId = "inst-01",
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
                    QualityScore = 95.0m,
                    MarketTrend = "Intraday Reversal Confluence",
                    ConfirmationMode = "CANDLE_CLOSE",
                    AnalysisSummary = "Triple Confluence: Supertrend(20,2) Bullish, Price > VWAP, MACD Bullish crossover on 5m chart. Strict 1:1 Target & Stop Loss configured.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Supertrend (20,2)", IsSatisfied = true, ExpectedValue = "Bullish", ActualValue = "Bullish" },
                        new() { ConditionName = "VWAP Support", IsSatisfied = true, ExpectedValue = "Price > VWAP", ActualValue = "24350 > 24320" },
                        new() { ConditionName = "MACD Concurrence", IsSatisfied = true, ExpectedValue = "MACD > Signal", ActualValue = "12.4 > 8.1" }
                    }
                },
                new()
                {
                    SignalId = "sig-banknifty-lb-02",
                    InstrumentId = "inst-02",
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
