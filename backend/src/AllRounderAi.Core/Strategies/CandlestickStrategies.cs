using System;
using System.Collections.Generic;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Indicators;

namespace AllRounderAi.Core.Strategies
{
    // ========================================================================
    // 1. HAMMER STRATEGY (Slides 8-10)
    // ========================================================================
    public class HammerStrategy : IStrategyEngine
    {
        public string StrategyId => "HAMMER";
        public string StrategyName => "Hammer Pattern";
        public string PptSlideReference => "Slides 5-8";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            var hammerConfig = new Hammer.HammerPatternConfig
            {
                RiskRewardRatio = config.RiskRewardRatio,
                ConfirmationMode = config.ConfirmationMode
            };

            var detail = Hammer.HammerSignalEngine.AnalyzeHammer(candles, instrument, hammerConfig, currentIndex);
            if (detail.PatternStatus == "BUY" && detail.GeneratedSignal != null)
            {
                return new StrategyMatchResult { IsMatched = true, Signal = detail.GeneratedSignal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 2. INVERSE HAMMER STRATEGY (Slides 11-12)
    // ========================================================================
    public class InverseHammerStrategy : IStrategyEngine
    {
        public string StrategyId => "INVERSE_HAMMER";
        public string StrategyName => "Inverse Hammer Pattern";
        public string PptSlideReference => "Slides 11-12";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 5) return new StrategyMatchResult { IsMatched = false };

            var c = candles[idx];
            bool priorDowntrend = candles[idx - 1].Close < candles[idx - 5].Close;
            bool isInvHammer = c.Body > 0 && c.UpperWick >= (2.0m * c.Body) && c.LowerWick <= (0.15m * c.Range);

            var conditions = new List<ConditionResult>
            {
                new() { ConditionName = "Prior Downtrend", IsSatisfied = priorDowntrend, ExpectedValue = "Bearish Trend", ActualValue = priorDowntrend ? "Downtrend" : "Uptrend" },
                new() { ConditionName = "Upper Wick >= 2x Body", IsSatisfied = c.UpperWick >= (2.0m * c.Body), ExpectedValue = $">= {2.0m * c.Body:F2}", ActualValue = $"{c.UpperWick:F2}" },
                new() { ConditionName = "Lower Wick Minimal", IsSatisfied = c.LowerWick <= (0.15m * c.Range), ExpectedValue = $"<= {0.15m * c.Range:F2}", ActualValue = $"{c.LowerWick:F2}" }
            };

            if (priorDowntrend && isInvHammer)
            {
                decimal entry = c.High + instrument.TickSize;
                decimal stopLoss = c.Low - instrument.TickSize;
                decimal risk = entry - stopLoss;
                decimal target = entry + (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 80.0m,
                    MarketTrend = "Bullish Reversal",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Inverse Hammer formed in downtrend with long upper shadow signaling buying pressure test.",
                    Conditions = conditions
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 3. LOW BREAK, CLOSE UPSIDE STRATEGY (Slides 13-14 - 7-10 Point Rule)
    // ========================================================================
    public class LowBreakCloseUpsideStrategy : IStrategyEngine
    {
        public string StrategyId => "LOW_BREAK_CLOSE_UPSIDE";
        public string StrategyName => "Low Break, Close Upside";
        public string PptSlideReference => "Slides 13-14";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 1) return new StrategyMatchResult { IsMatched = false };

            var c1 = candles[idx - 1]; // Red candle
            var c2 = candles[idx];     // Green candle

            bool isC1Red = c1.IsRed;
            bool isC2Green = c2.IsGreen;
            bool lowBroken = c2.Low < c1.Low;
            bool closeUpside = c2.Close > Math.Min(c1.Open, c1.Close);

            // 7-10 Point Rule (Configurable per instrument: points for indices, percentage for equities)
            bool bodyRuleSatisfied;
            decimal diffPoints = c2.Body;
            if (instrument.Segment == ExchangeSegment.Index || instrument.Segment == ExchangeSegment.Futures)
            {
                bodyRuleSatisfied = diffPoints >= config.MinCandleBodyPoints && diffPoints <= config.MaxCandleBodyPoints;
            }
            else
            {
                decimal bodyPct = (diffPoints / c2.Open) * 100m;
                bodyRuleSatisfied = bodyPct >= config.MinBodyPercentage && bodyPct <= config.MaxBodyPercentage;
            }

            var conditions = new List<ConditionResult>
            {
                new() { ConditionName = "Candle 1 is RED", IsSatisfied = isC1Red, ExpectedValue = "Red Candle", ActualValue = isC1Red ? "Red" : "Green" },
                new() { ConditionName = "Candle 2 is GREEN", IsSatisfied = isC2Green, ExpectedValue = "Green Candle", ActualValue = isC2Green ? "Green" : "Red" },
                new() { ConditionName = "Low Break", IsSatisfied = lowBroken, ExpectedValue = $"C2 Low < {c1.Low:F2}", ActualValue = $"{c2.Low:F2}" },
                new() { ConditionName = "Close Upside", IsSatisfied = closeUpside, ExpectedValue = $"C2 Close > {Math.Min(c1.Open, c1.Close):F2}", ActualValue = $"{c2.Close:F2}" },
                new() { ConditionName = "7-10 Points / % Body Rule (Slide 13)", IsSatisfied = bodyRuleSatisfied, ExpectedValue = $"{config.MinCandleBodyPoints} to {config.MaxCandleBodyPoints} pts", ActualValue = $"{diffPoints:F2} pts" }
            };

            if (isC1Red && isC2Green && lowBroken && closeUpside && bodyRuleSatisfied)
            {
                decimal entry = c2.Close;
                decimal stopLoss = c2.Low - instrument.TickSize;
                decimal risk = entry - stopLoss;
                decimal target = entry + (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c2.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 90.0m,
                    MarketTrend = "Reversal Trend",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Low Break Close Upside matched: Previous red candle's low broken, current green candle closed upside with PPT 7-10 pt body rule satisfied.",
                    Conditions = conditions
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 4. BULLISH HARAMI STRATEGY (Slides 15-16)
    // ========================================================================
    public class BullishHaramiStrategy : IStrategyEngine
    {
        public string StrategyId => "BULLISH_HARAMI";
        public string StrategyName => "Bullish Harami";
        public string PptSlideReference => "Slides 15-16";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 1) return new StrategyMatchResult { IsMatched = false };

            var c1 = candles[idx - 1]; // Large red
            var c2 = candles[idx];     // Small green inside c1

            bool isC1Red = c1.IsRed && c1.Body > 0;
            bool isC2Green = c2.IsGreen && c2.Body > 0;
            bool insideBody = c2.Open > c1.Close && c2.Close < c1.Open && c2.High < c1.High && c2.Low > c1.Low;

            if (isC1Red && isC2Green && insideBody)
            {
                decimal entry = c2.High + instrument.TickSize;
                decimal stopLoss = c1.Low - instrument.TickSize;
                decimal risk = entry - stopLoss;
                decimal target = entry + (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c2.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 85.0m,
                    MarketTrend = "Bullish Reversal",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Bullish Harami: 2-candle pattern with small green candle completely encapsulated inside prior long red candle.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Candle 1 Long Red", IsSatisfied = isC1Red, ExpectedValue = "Red", ActualValue = "Red" },
                        new() { ConditionName = "Candle 2 Small Green Inside", IsSatisfied = insideBody, ExpectedValue = "Inside C1 Body", ActualValue = "Contained" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 5. HANGING MAN STRATEGY (Slides 17-18)
    // ========================================================================
    public class HangingManStrategy : IStrategyEngine
    {
        public string StrategyId => "HANGING_MAN";
        public string StrategyName => "Hanging Man Pattern";
        public string PptSlideReference => "Slides 17-18";
        public SignalDirection DefaultDirection => SignalDirection.Sell;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 5) return new StrategyMatchResult { IsMatched = false };

            var c = candles[idx];
            bool priorUptrend = candles[idx - 1].Close > candles[idx - 5].Close;
            bool isHangingMan = c.Body > 0 && c.LowerWick >= (2.0m * c.Body) && c.UpperWick <= (0.15m * c.Range);

            if (priorUptrend && isHangingMan)
            {
                decimal entry = c.Low - instrument.TickSize;
                decimal stopLoss = c.High + instrument.TickSize;
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 82.0m,
                    MarketTrend = "Bearish Reversal from Uptrend",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Hanging Man pattern printed at top of uptrend signaling impending buyer exhaustion.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Prior Uptrend", IsSatisfied = priorUptrend, ExpectedValue = "Uptrend", ActualValue = "Uptrend" },
                        new() { ConditionName = "Lower Wick >= 2x Body", IsSatisfied = true, ExpectedValue = "Long lower wick", ActualValue = $"{c.LowerWick:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 6. SHOOTING STAR STRATEGY (Slides 19-20)
    // ========================================================================
    public class ShootingStarStrategy : IStrategyEngine
    {
        public string StrategyId => "SHOOTING_STAR";
        public string StrategyName => "Shooting Star Pattern";
        public string PptSlideReference => "Slides 19-20";
        public SignalDirection DefaultDirection => SignalDirection.Sell;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 5) return new StrategyMatchResult { IsMatched = false };

            var c = candles[idx];
            bool priorUptrend = candles[idx - 1].Close > candles[idx - 5].Close;
            bool isShootingStar = c.Body > 0 && c.UpperWick >= (2.0m * c.Body) && c.LowerWick <= (0.15m * c.Range);

            if (priorUptrend && isShootingStar)
            {
                decimal entry = c.Low - instrument.TickSize;
                decimal stopLoss = c.High + instrument.TickSize;
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 88.0m,
                    MarketTrend = "Bearish Reversal at Resistance",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Shooting Star printed at swing high with long upper rejection wick.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Prior Uptrend", IsSatisfied = priorUptrend, ExpectedValue = "Uptrend", ActualValue = "Uptrend" },
                        new() { ConditionName = "Upper Wick >= 2x Body", IsSatisfied = true, ExpectedValue = ">= 2x Body", ActualValue = $"{c.UpperWick:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 7. HIGH BREAK, CLOSE DOWNSIDE STRATEGY (Slides 21-22 - 7-10 Point Rule)
    // ========================================================================
    public class HighBreakCloseDownsideStrategy : IStrategyEngine
    {
        public string StrategyId => "HIGH_BREAK_CLOSE_DOWNSIDE";
        public string StrategyName => "High Break, Close Downside";
        public string PptSlideReference => "Slides 21-22";
        public SignalDirection DefaultDirection => SignalDirection.Sell;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 1) return new StrategyMatchResult { IsMatched = false };

            var c1 = candles[idx - 1]; // Green candle
            var c2 = candles[idx];     // Red candle

            bool isC1Green = c1.IsGreen;
            bool isC2Red = c2.IsRed;
            bool highBroken = c2.High > c1.High;
            bool closeDownside = c2.Close < Math.Min(c1.Open, c1.Close);

            bool bodyRuleSatisfied;
            decimal diffPoints = c2.Body;
            if (instrument.Segment == ExchangeSegment.Index || instrument.Segment == ExchangeSegment.Futures)
            {
                bodyRuleSatisfied = diffPoints >= config.MinCandleBodyPoints && diffPoints <= config.MaxCandleBodyPoints;
            }
            else
            {
                decimal bodyPct = (diffPoints / c2.Open) * 100m;
                bodyRuleSatisfied = bodyPct >= config.MinBodyPercentage && bodyPct <= config.MaxBodyPercentage;
            }

            if (isC1Green && isC2Red && highBroken && closeDownside && bodyRuleSatisfied)
            {
                decimal entry = c2.Close;
                decimal stopLoss = c2.High + instrument.TickSize;
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c2.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 90.0m,
                    MarketTrend = "Reversal Trend",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "High Break Close Downside matched: Green candle high broken, current red candle closed downside with PPT 7-10 pt body rule satisfied.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "High Break", IsSatisfied = highBroken, ExpectedValue = $"High > {c1.High:F2}", ActualValue = $"{c2.High:F2}" },
                        new() { ConditionName = "Close Downside", IsSatisfied = closeDownside, ExpectedValue = $"Close < {Math.Min(c1.Open, c1.Close):F2}", ActualValue = $"{c2.Close:F2}" },
                        new() { ConditionName = "7-10 Point Body Rule", IsSatisfied = bodyRuleSatisfied, ExpectedValue = $"{config.MinCandleBodyPoints}-{config.MaxCandleBodyPoints} pts", ActualValue = $"{diffPoints:F2} pts" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 8. BEARISH HARAMI STRATEGY (Slides 23-24)
    // ========================================================================
    public class BearishHaramiStrategy : IStrategyEngine
    {
        public string StrategyId => "BEARISH_HARAMI";
        public string StrategyName => "Bearish Harami";
        public string PptSlideReference => "Slides 23-24";
        public SignalDirection DefaultDirection => SignalDirection.Sell;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 1) return new StrategyMatchResult { IsMatched = false };

            var c1 = candles[idx - 1]; // Long green
            var c2 = candles[idx];     // Small red inside c1

            bool isC1Green = c1.IsGreen && c1.Body > 0;
            bool isC2Red = c2.IsRed && c2.Body > 0;
            bool insideBody = c2.Open < c1.Close && c2.Close > c1.Open && c2.High < c1.High && c2.Low > c1.Low;

            if (isC1Green && isC2Red && insideBody)
            {
                decimal entry = c2.Low - instrument.TickSize;
                decimal stopLoss = c1.High + instrument.TickSize;
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c2.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 84.0m,
                    MarketTrend = "Bearish Reversal",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Bearish Harami: Small red candle fully encapsulated within preceding large green candle.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Candle 1 Long Green", IsSatisfied = isC1Green, ExpectedValue = "Green", ActualValue = "Green" },
                        new() { ConditionName = "Candle 2 Small Red Inside", IsSatisfied = insideBody, ExpectedValue = "Contained inside C1", ActualValue = "Contained" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 9. MARUBOZU STRATEGY (Slides 25-26 - Bullish & Bearish)
    // ========================================================================
    public class MarubozuStrategy : IStrategyEngine
    {
        public string StrategyId => "MARUBOZU";
        public string StrategyName => "Marubozu Pattern";
        public string PptSlideReference => "Slides 25-26";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 0) return new StrategyMatchResult { IsMatched = false };

            var c = candles[idx];
            bool hasMinimalWicks = c.UpperWick <= (0.05m * c.Range) && c.LowerWick <= (0.05m * c.Range) && c.Range > 0;
            if (!hasMinimalWicks) return new StrategyMatchResult { IsMatched = false };

            if (c.IsGreen)
            {
                // Bullish Marubozu
                decimal entry = c.Close;
                decimal stopLoss = c.Low - instrument.TickSize;
                decimal risk = entry - stopLoss;
                decimal target = entry + (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "BULLISH_MARUBOZU",
                    StrategyName = "Bullish Marubozu",
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 87.0m,
                    MarketTrend = "Strong Bullish Momentum",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Bullish Marubozu: Full green body candle with virtually zero wicks indicating complete buyer dominance.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Full Body Green", IsSatisfied = true, ExpectedValue = "Green", ActualValue = "Green" },
                        new() { ConditionName = "Minimal Wicks (<= 5%)", IsSatisfied = true, ExpectedValue = "<= 5% Range", ActualValue = $"{c.UpperWick + c.LowerWick:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }
            else
            {
                // Bearish Marubozu
                decimal entry = c.Close;
                decimal stopLoss = c.High + instrument.TickSize;
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "BEARISH_MARUBOZU",
                    StrategyName = "Bearish Marubozu",
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 87.0m,
                    MarketTrend = "Strong Bearish Momentum",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Bearish Marubozu: Full red body candle with virtually zero wicks indicating intense selling pressure.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Full Body Red", IsSatisfied = true, ExpectedValue = "Red", ActualValue = "Red" },
                        new() { ConditionName = "Minimal Wicks (<= 5%)", IsSatisfied = true, ExpectedValue = "<= 5% Range", ActualValue = $"{c.UpperWick + c.LowerWick:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }
        }
    }

    // ========================================================================
    // 10. MORNING STAR STRATEGY (Slides 40-41)
    // ========================================================================
    public class MorningStarStrategy : IStrategyEngine
    {
        public string StrategyId => "MORNING_STAR";
        public string StrategyName => "Morning Star Pattern";
        public string PptSlideReference => "Slides 40-41";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 3) return new StrategyMatchResult { IsMatched = false };

            var c1 = candles[idx - 2]; // Long red
            var c2 = candles[idx - 1]; // Small star
            var c3 = candles[idx];     // Strong green

            bool isC1Red = c1.IsRed && c1.Body > 0;
            bool isC2Star = c2.Body < (0.4m * c1.Body);
            bool isC3Green = c3.IsGreen && c3.Close >= (c1.Open + c1.Close) / 2m; // Penetrates > 50% into C1

            if (isC1Red && isC2Star && isC3Green)
            {
                decimal entry = c3.Close;
                decimal stopLoss = Math.Min(c1.Low, Math.Min(c2.Low, c3.Low)) - instrument.TickSize;
                decimal risk = entry - stopLoss;
                decimal target = entry + (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c3.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 91.0m,
                    MarketTrend = "Bullish Reversal",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Morning Star 3-candle reversal: Bearish candle followed by indecision star and strong bullish close over 50% into first body.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "C1 Long Bearish", IsSatisfied = isC1Red, ExpectedValue = "Red", ActualValue = "Red" },
                        new() { ConditionName = "C2 Indecision Star", IsSatisfied = isC2Star, ExpectedValue = "< 40% C1 Body", ActualValue = $"{c2.Body:F2}" },
                        new() { ConditionName = "C3 Bullish Close > 50%", IsSatisfied = isC3Green, ExpectedValue = $"> {(c1.Open + c1.Close) / 2m:F2}", ActualValue = $"{c3.Close:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 11. EVENING STAR STRATEGY (Slides 42-43)
    // ========================================================================
    public class EveningStarStrategy : IStrategyEngine
    {
        public string StrategyId => "EVENING_STAR";
        public string StrategyName => "Evening Star Pattern";
        public string PptSlideReference => "Slides 42-43";
        public SignalDirection DefaultDirection => SignalDirection.Sell;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 3) return new StrategyMatchResult { IsMatched = false };

            var c1 = candles[idx - 2]; // Long green
            var c2 = candles[idx - 1]; // Small star
            var c3 = candles[idx];     // Strong red

            bool isC1Green = c1.IsGreen && c1.Body > 0;
            bool isC2Star = c2.Body < (0.4m * c1.Body);
            bool isC3Red = c3.IsRed && c3.Close <= (c1.Open + c1.Close) / 2m; // Penetrates > 50% down

            if (isC1Green && isC2Star && isC3Red)
            {
                decimal entry = c3.Close;
                decimal stopLoss = Math.Max(c1.High, Math.Max(c2.High, c3.High)) + instrument.TickSize;
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = StrategyId,
                    StrategyName = StrategyName,
                    PptSlideReference = PptSlideReference,
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c3.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 91.0m,
                    MarketTrend = "Bearish Reversal",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Evening Star 3-candle bearish reversal completed.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "C1 Long Green", IsSatisfied = isC1Green, ExpectedValue = "Green", ActualValue = "Green" },
                        new() { ConditionName = "C2 Star at High", IsSatisfied = isC2Star, ExpectedValue = "< 40% C1 Body", ActualValue = $"{c2.Body:F2}" },
                        new() { ConditionName = "C3 Bearish Close < 50%", IsSatisfied = isC3Red, ExpectedValue = $"< {(c1.Open + c1.Close) / 2m:F2}", ActualValue = $"{c3.Close:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }
}
