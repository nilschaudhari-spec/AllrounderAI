using System;
using System.Collections.Generic;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Indicators;

namespace AllRounderAi.Core.Strategies
{
    // ========================================================================
    // 1. BOLLINGER BANDS STRATEGY (Slides 44-49 - Call & Put Buying)
    // ========================================================================
    public class BollingerBandsStrategy : IStrategyEngine
    {
        public string StrategyId => "BOLLINGER_BANDS";
        public string StrategyName => "Bollinger Bands Trend & Reversal";
        public string PptSlideReference => "Slides 44-49";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < config.BollingerPeriod) return new StrategyMatchResult { IsMatched = false };

            var bb = TechnicalIndicators.CalculateBollingerBands(candles, config.BollingerPeriod, config.BollingerStdDev);
            var c = candles[idx];

            decimal upper = bb.Upper[idx];
            decimal lower = bb.Lower[idx];
            decimal median = bb.Middle[idx];

            // Slide 48: Upper Line touch/break = strong uptrend (Call Buying)
            bool isUpperBreak = c.Close >= upper && c.IsGreen;
            // Slide 48: Lower Line touch/break = strong downtrend (Put Buying)
            bool isLowerBreak = c.Close <= lower && c.IsRed;

            if (isUpperBreak)
            {
                decimal entry = c.Close;
                decimal stopLoss = median;
                decimal risk = entry - stopLoss;
                decimal target = entry + (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "BOLLINGER_UPPER_BREAK",
                    StrategyName = "Bollinger Bands - Call Buying (Upper Line Break)",
                    PptSlideReference = "Slides 48-49",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 86.0m,
                    MarketTrend = "Strong Uptrend Momentum",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Bollinger Bands Upper Line broken with strong green candle; indicates strong uptrend momentum for Call Buying.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Price Breaking Upper Band", IsSatisfied = true, ExpectedValue = $">= {upper:F2}", ActualValue = $"{c.Close:F2}" },
                        new() { ConditionName = "Green Candle Confirmation", IsSatisfied = true, ExpectedValue = "Green", ActualValue = "Green" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }
            else if (isLowerBreak)
            {
                decimal entry = c.Close;
                decimal stopLoss = median;
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "BOLLINGER_LOWER_BREAK",
                    StrategyName = "Bollinger Bands - Put Buying (Lower Line Break)",
                    PptSlideReference = "Slides 48-49",
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 86.0m,
                    MarketTrend = "Strong Downtrend Momentum",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Bollinger Bands Lower Line broken with strong red candle; indicates strong downtrend momentum for Put Buying.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Price Breaking Lower Band", IsSatisfied = true, ExpectedValue = $"<= {lower:F2}", ActualValue = $"{c.Close:F2}" },
                        new() { ConditionName = "Red Candle Confirmation", IsSatisfied = true, ExpectedValue = "Red", ActualValue = "Red" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 2. GOLDEN CROSS & RE-ENTRY STRATEGY (Slides 50-58 - EMA 50 & EMA 200)
    // ========================================================================
    public class GoldenCrossStrategy : IStrategyEngine
    {
        public string StrategyId => "GOLDEN_CROSS";
        public string StrategyName => "Golden Cross & Re-entry Strategy";
        public string PptSlideReference => "Slides 50-58";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 201) return new StrategyMatchResult { IsMatched = false };

            var ema50 = TechnicalIndicators.CalculateEma(candles, config.EmaFast);
            var ema200 = TechnicalIndicators.CalculateEma(candles, config.EmaSlow);

            decimal fastCurr = ema50[idx];
            decimal fastPrev = ema50[idx - 1];
            decimal slowCurr = ema200[idx];
            decimal slowPrev = ema200[idx - 1];

            var c = candles[idx];

            // 1. Fresh Golden Cross (Buy)
            bool isGoldenCross = fastPrev <= slowPrev && fastCurr > slowCurr;
            // 2. Fresh Death Cross (Sell)
            bool isDeathCross = fastPrev >= slowPrev && fastCurr < slowCurr;

            // 3. Re-entry Opportunity (Slides 55-58)
            // Bullish re-entry: EMA 50 is above EMA 200, candle pulls back near or touches EMA 200 (support) and closes green
            bool isBullishReentry = fastCurr > slowCurr && 
                                   c.Low <= slowCurr * 1.003m && 
                                   c.Close >= slowCurr && 
                                   c.IsGreen;

            // Bearish re-entry: EMA 50 is below EMA 200, candle rallies near or touches EMA 200 (resistance) and closes red
            bool isBearishReentry = fastCurr < slowCurr && 
                                    c.High >= slowCurr * 0.997m && 
                                    c.Close <= slowCurr && 
                                    c.IsRed;

            if (isGoldenCross)
            {
                decimal entry = c.Close;
                decimal stopLoss = slowCurr - (instrument.TickSize * 10);
                decimal risk = entry - stopLoss;
                decimal target = entry + (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "GOLDEN_CROSS_BUY",
                    StrategyName = "Golden Cross (EMA 50 / 200 Crossover)",
                    PptSlideReference = "Slides 50-54",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 95.0m,
                    MarketTrend = "Macro Uptrend Initiation",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Golden Cross: Fast Blue line (EMA 50) crossed above Red line (EMA 200), confirming new major uptrend.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "EMA 50 Crossed Above EMA 200", IsSatisfied = true, ExpectedValue = $"> {slowCurr:F2}", ActualValue = $"{fastCurr:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }
            else if (isDeathCross)
            {
                decimal entry = c.Close;
                decimal stopLoss = slowCurr + (instrument.TickSize * 10);
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "DEATH_CROSS_SELL",
                    StrategyName = "Death Cross (EMA 50 / 200 Bearish Crossover)",
                    PptSlideReference = "Slides 50-54",
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 95.0m,
                    MarketTrend = "Macro Downtrend Initiation",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Death Cross: Blue line (EMA 50) crossed below Red line (EMA 200), confirming macro downtrend.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "EMA 50 Crossed Below EMA 200", IsSatisfied = true, ExpectedValue = $"< {slowCurr:F2}", ActualValue = $"{fastCurr:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }
            else if (isBullishReentry)
            {
                decimal entry = c.Close;
                decimal stopLoss = slowCurr - (instrument.TickSize * 5);
                decimal risk = entry - stopLoss;
                decimal target = entry + (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "GOLDEN_CROSS_REENTRY_BUY",
                    StrategyName = "Golden Cross Re-entry Opportunity (EMA 200 Support)",
                    PptSlideReference = "Slides 55-58",
                    SignalType = SignalDirection.Buy,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 92.0m,
                    MarketTrend = "Uptrend Pullback Bounce",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Golden Cross Re-entry: Price touched dynamic support at EMA 200 Red Line during active uptrend and formed bullish bounce.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Uptrend Active (EMA 50 > EMA 200)", IsSatisfied = true, ExpectedValue = "EMA50 > EMA200", ActualValue = $"{fastCurr:F2} > {slowCurr:F2}" },
                        new() { ConditionName = "Candle Touched/Near EMA 200 Support", IsSatisfied = true, ExpectedValue = $"<= {slowCurr * 1.003m:F2}", ActualValue = $"{c.Low:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }
            else if (isBearishReentry)
            {
                decimal entry = c.Close;
                decimal stopLoss = slowCurr + (instrument.TickSize * 5);
                decimal risk = stopLoss - entry;
                decimal target = entry - (risk * config.RiskRewardRatio);

                var signal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "GOLDEN_CROSS_REENTRY_SELL",
                    StrategyName = "Golden Cross Re-entry Short (EMA 200 Resistance)",
                    PptSlideReference = "Slides 55-58",
                    SignalType = SignalDirection.Sell,
                    Status = SignalStatus.Confirmed,
                    Timeframe = config.Timeframe,
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = 92.0m,
                    MarketTrend = "Downtrend Pullback Rejection",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Golden Cross Re-entry Short: Price rallied to test EMA 200 Red Line resistance during active downtrend and got rejected.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Downtrend Active (EMA 50 < EMA 200)", IsSatisfied = true, ExpectedValue = "EMA50 < EMA200", ActualValue = $"{fastCurr:F2} < {slowCurr:F2}" },
                        new() { ConditionName = "Candle Touched EMA 200 Resistance", IsSatisfied = true, ExpectedValue = $">= {slowCurr * 0.997m:F2}", ActualValue = $"{c.High:F2}" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }

    // ========================================================================
    // 3. DAILY INTRADAY TECHNIQUE (Slides 59-62 - 5M Supertrend + VWAP + MACD)
    // ========================================================================
    public class DailyIntradayStrategy : IStrategyEngine
    {
        public string StrategyId => "DAILY_INTRADAY_5M";
        public string StrategyName => "Daily Intraday Technique (5-Minute)";
        public string PptSlideReference => "Slides 59-62";
        public SignalDirection DefaultDirection => SignalDirection.Buy;

        public StrategyMatchResult Evaluate(IReadOnlyList<Candle> candles, Instrument instrument, StrategyConfig config, int currentIndex = -1)
        {
            int idx = currentIndex >= 0 ? currentIndex : candles.Count - 1;
            if (idx < 30) return new StrategyMatchResult { IsMatched = false };

            var c = candles[idx];
            var supertrend = TechnicalIndicators.CalculateSupertrend(candles, config.SupertrendLength, config.SupertrendMultiplier);
            var vwap = TechnicalIndicators.CalculateVwap(candles);
            var macd = TechnicalIndicators.CalculateMacd(candles, config.MacdFast, config.MacdSlow, config.MacdSignal);
            var atr = TechnicalIndicators.CalculateAtr(candles, 14);

            bool stBullish = supertrend.IsBullish[idx];
            bool stFlippedBullish = stBullish && !supertrend.IsBullish[idx - 1];
            bool stFlippedBearish = !stBullish && supertrend.IsBullish[idx - 1];

            decimal currVwap = vwap[idx];
            bool aboveVwap = c.Close > currVwap;
            bool belowVwap = c.Close < currVwap;

            decimal macdLine = macd.MacdLine[idx];
            decimal macdSig = macd.SignalLine[idx];
            bool macdBullish = macdLine > macdSig;
            bool macdBearish = macdLine < macdSig;

            // Slide 62 Rule 1: Candlestick Body should not be weak / small
            decimal minBody = 0.4m * atr[idx];
            bool bodyStrong = c.Body >= minBody;

            // Concurrency Buy Check
            bool isBuyConfluence = stBullish && aboveVwap && macdBullish && bodyStrong && (stFlippedBullish || c.Close > candles[idx - 1].High);
            // Concurrency Sell Check
            bool isSellConfluence = !stBullish && belowVwap && macdBearish && bodyStrong && (stFlippedBearish || c.Close < candles[idx - 1].Low);

            // Slide 62 Rule 4: Target & Stop Loss should be 1:1
            decimal pointTarget = Math.Max(20.0m, atr[idx]); // Default 20 pts for index as per Slide 62 example

            if (isBuyConfluence)
            {
                decimal entry = c.Close;
                decimal stopLoss = entry - pointTarget;
                decimal target = entry + pointTarget; // Strict 1:1 ratio per Slide 62 Rule 4

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
                    Timeframe = "5m",
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = 1.0m, // Strict 1:1
                    QualityScore = 96.0m,
                    MarketTrend = "Intraday Bullish Reversal Confluence",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Triple Indicator Confirmation: Supertrend(20,2) is Bullish, Price above VWAP, MACD bullish crossover, Candlestick body strong. Strict 1:1 Risk-to-Reward applied.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Supertrend (20,2) Bullish", IsSatisfied = stBullish, ExpectedValue = "Green / Bullish", ActualValue = "Bullish" },
                        new() { ConditionName = "Price > VWAP", IsSatisfied = aboveVwap, ExpectedValue = $"> {currVwap:F2}", ActualValue = $"{c.Close:F2}" },
                        new() { ConditionName = "MACD Bullish Confirmation", IsSatisfied = macdBullish, ExpectedValue = "MACD > Signal", ActualValue = $"{macdLine:F2} > {macdSig:F2}" },
                        new() { ConditionName = "Strong Candle Body (Slide 62 Rule 1)", IsSatisfied = bodyStrong, ExpectedValue = $">= {minBody:F2}", ActualValue = $"{c.Body:F2}" },
                        new() { ConditionName = "1:1 Target & Stop Loss (Slide 62 Rule 4)", IsSatisfied = true, ExpectedValue = "1:1 Ratio", ActualValue = $"{pointTarget:F2} pts Target & SL" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }
            else if (isSellConfluence)
            {
                decimal entry = c.Close;
                decimal stopLoss = entry + pointTarget;
                decimal target = entry - pointTarget; // Strict 1:1 ratio per Slide 62 Rule 4

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
                    Timeframe = "5m",
                    SignalTimestamp = c.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = 1.0m,
                    QualityScore = 96.0m,
                    MarketTrend = "Intraday Bearish Reversal Confluence",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = "Triple Indicator Confirmation: Supertrend(20,2) is Bearish, Price below VWAP, MACD bearish crossover, Candlestick body strong. Strict 1:1 Risk-to-Reward applied.",
                    Conditions = new List<ConditionResult>
                    {
                        new() { ConditionName = "Supertrend (20,2) Bearish", IsSatisfied = !stBullish, ExpectedValue = "Red / Bearish", ActualValue = "Bearish" },
                        new() { ConditionName = "Price < VWAP", IsSatisfied = belowVwap, ExpectedValue = $"< {currVwap:F2}", ActualValue = $"{c.Close:F2}" },
                        new() { ConditionName = "MACD Bearish Confirmation", IsSatisfied = macdBearish, ExpectedValue = "MACD < Signal", ActualValue = $"{macdLine:F2} < {macdSig:F2}" },
                        new() { ConditionName = "Strong Candle Body (Slide 62 Rule 1)", IsSatisfied = bodyStrong, ExpectedValue = $">= {minBody:F2}", ActualValue = $"{c.Body:F2}" },
                        new() { ConditionName = "1:1 Target & Stop Loss (Slide 62 Rule 4)", IsSatisfied = true, ExpectedValue = "1:1 Ratio", ActualValue = $"{pointTarget:F2} pts Target & SL" }
                    }
                };
                return new StrategyMatchResult { IsMatched = true, Signal = signal };
            }

            return new StrategyMatchResult { IsMatched = false };
        }
    }
}
