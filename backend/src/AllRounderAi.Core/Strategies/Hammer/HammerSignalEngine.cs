using System;
using System.Collections.Generic;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies.Hammer
{
    public class HammerDetectionDetail
    {
        public string DetectionId { get; set; } = Guid.NewGuid().ToString();
        public string InstrumentId { get; set; } = string.Empty;
        public string TradingSymbol { get; set; } = string.Empty;
        public string Exchange { get; set; } = "NSE";
        public string Segment { get; set; } = "EQUITY";
        public string Timeframe { get; set; } = "5m";
        public DateTime CandleTimestamp { get; set; }
        public string PatternStatus { get; set; } = "NO_TRADE"; // HAMMER_DETECTED, HAMMER_GEOMETRY_ONLY, HAMMER_CANDIDATE, HAMMER_CONFIRMED, BUY, NO_TRADE, INVALIDATED

        public HammerCandleGeometry Geometry { get; set; } = new();
        public TrendEvaluationResult Trend { get; set; } = new();
        public SupportEvaluationResult Support { get; set; } = new();
        public VolumeEvaluationResult Volume { get; set; } = new();
        public ConfirmationEvaluationResult Confirmation { get; set; } = new();
        public HammerScoreResult Score { get; set; } = new();

        public decimal Atr { get; set; }
        public decimal EntryPrice { get; set; }
        public decimal StopLossPrice { get; set; }
        public decimal TargetPrice { get; set; }
        public decimal RiskRewardRatio { get; set; } = 1.0m;
        public int RecommendedPositionSize { get; set; }
        public decimal RiskAmount { get; set; }

        public List<string> ExplainabilityBulletPoints { get; set; } = new();
        public TradingSignal? GeneratedSignal { get; set; }
    }

    public static class HammerSignalEngine
    {
        public static HammerDetectionDetail AnalyzeHammer(
            IReadOnlyList<Candle> candles,
            Instrument instrument,
            HammerPatternConfig config,
            int targetIndex = -1,
            bool isDevelopingCandle = false,
            decimal accountCapital = 100000m)
        {
            var detail = new HammerDetectionDetail
            {
                InstrumentId = instrument.InstrumentId,
                TradingSymbol = instrument.TradingSymbol,
                Exchange = instrument.Exchange,
                Segment = instrument.Segment.ToString(),
                Timeframe = config.Timeframes.Contains("5m") ? "5m" : (config.Timeframes.Count > 0 ? config.Timeframes[0] : "5m")
            };

            int idx = targetIndex >= 0 ? targetIndex : candles.Count - 1;
            if (idx < 5 || candles.Count == 0)
            {
                detail.PatternStatus = "NO_TRADE";
                detail.ExplainabilityBulletPoints.Add("Insufficient candle history to evaluate Hammer setup.");
                return detail;
            }

            var candle = candles[idx];
            detail.CandleTimestamp = candle.Timestamp;

            // 1. Calculate Average True Range (ATR 14)
            decimal atr = CalculateAtr(candles, idx, config.AtrPeriod);
            detail.Atr = atr;

            // 2. Geometry Calculation
            var geo = HammerCandleGeometry.Calculate(candle, atr);
            detail.Geometry = geo;

            if (!geo.IsValid || geo.IsZeroBodyDoji)
            {
                detail.PatternStatus = "INVALIDATED";
                detail.ExplainabilityBulletPoints.Add(geo.IsZeroBodyDoji
                    ? "Zero-body Doji rejected: Institutional policy requires an identifiable real body in upper range."
                    : $"Geometry invalid: {geo.ValidationError}");
                return detail;
            }

            // Verify Geometry Thresholds
            bool wickRatioPassed = geo.LowerWickToBodyRatio >= config.MinLowerWickToBodyRatio;
            bool upperWickPassed = geo.UpperWickToRangeRatio <= config.MaxUpperWickToRangeRatio;
            bool bodyRatioPassed = geo.BodyToRangeRatio <= config.MaxBodyToRangeRatio;
            bool bodyPosPassed = geo.BodyPosition >= config.MinBodyPosition;
            bool rangeAtrPassed = geo.RangeToAtrRatio >= config.MinCandleRangeToAtrRatio;

            bool geometryQualified = wickRatioPassed && upperWickPassed && bodyRatioPassed && bodyPosPassed && rangeAtrPassed;

            detail.ExplainabilityBulletPoints.Add($"Lower wick is {geo.LowerWickToBodyRatio:F2}× candle body (minimum required: {config.MinLowerWickToBodyRatio:F2}×).");
            detail.ExplainabilityBulletPoints.Add($"Upper wick occupies {geo.UpperWickToRangeRatio:P1} of range (maximum allowed: {config.MaxUpperWickToRangeRatio:P1}).");
            detail.ExplainabilityBulletPoints.Add($"Body occupies {geo.BodyToRangeRatio:P1} of candle range (maximum allowed: {config.MaxBodyToRangeRatio:P1}).");
            detail.ExplainabilityBulletPoints.Add($"Body position is at {geo.BodyPosition:P1} of total candle range.");
            detail.ExplainabilityBulletPoints.Add($"Candle range is {geo.RangeToAtrRatio:F2}× ATR.");

            if (!geometryQualified)
            {
                detail.PatternStatus = "NO_TRADE";
                detail.ExplainabilityBulletPoints.Add("Candle geometry does not qualify as a Hammer pattern.");
                return detail;
            }

            // 3. Trend Detection
            var trend = HammerTrendDetector.EvaluateDowntrend(candles, idx, config);
            detail.Trend = trend;
            detail.ExplainabilityBulletPoints.Add(trend.SummaryExplanation);

            // If geometry passes but downtrend context fails:
            if (config.RequireDowntrend && !trend.IsDowntrendConfirmed)
            {
                detail.PatternStatus = "HAMMER_GEOMETRY_ONLY";
                detail.ExplainabilityBulletPoints.Add("Classified as HAMMER_GEOMETRY_ONLY: Candle possesses Hammer proportions but lacks preceding bearish trend context.");
                return detail;
            }

            // 4. Support Detection
            var support = HammerSupportDetector.EvaluateSupportProximity(candles, idx, atr, config.MaxSupportDistanceAtr);
            detail.Support = support;
            detail.ExplainabilityBulletPoints.Add(support.Explanation);

            if (config.RequireSupportProximity && !support.IsNearSupport)
            {
                detail.PatternStatus = "NO_TRADE";
                detail.ExplainabilityBulletPoints.Add("Mandatory support proximity rule failed.");
                return detail;
            }

            // 5. Volume Confirmation
            var volume = HammerVolumeAnalyzer.EvaluateVolume(candles, idx, config);
            detail.Volume = volume;
            detail.ExplainabilityBulletPoints.Add(volume.Explanation);

            if (config.VolumeRequirementMode.Equals("Mandatory", StringComparison.OrdinalIgnoreCase) && !volume.IsConfirmed)
            {
                detail.PatternStatus = "NO_TRADE";
                detail.ExplainabilityBulletPoints.Add("Mandatory volume confirmation rule failed.");
                return detail;
            }

            // 6. Confirmation Mode Engine
            var confirmation = HammerConfirmationEngine.EvaluateConfirmation(candles, idx, geo, config, isDevelopingCandle);
            detail.Confirmation = confirmation;
            detail.ExplainabilityBulletPoints.Add(confirmation.Explanation);

            // 7. Calculate Comprehensive Explainable Score (0 - 100)
            var score = HammerScoringEngine.CalculateScore(geo, trend, support, volume, confirmation, config);
            detail.Score = score;
            detail.ExplainabilityBulletPoints.Add(score.Summary);

            // 8. Determine Final Trade Status
            if (confirmation.ConfirmationStatus == "INTRABAR_CANDIDATE")
            {
                detail.PatternStatus = "HAMMER_CANDIDATE";
                detail.ExplainabilityBulletPoints.Add("Pattern status: HAMMER_CANDIDATE (Intrabar developing candidate; awaiting bar close).");
            }
            else if (!confirmation.IsConfirmed)
            {
                detail.PatternStatus = "HAMMER_DETECTED";
                detail.ExplainabilityBulletPoints.Add("Pattern status: HAMMER_DETECTED (Awaiting confirmation trigger).");
            }
            else if (score.FinalScore < config.ScoreThreshold)
            {
                detail.PatternStatus = "HAMMER_CONFIRMED";
                detail.ExplainabilityBulletPoints.Add($"Pattern confirmed but quality score ({score.FinalScore}) is below execution threshold ({config.ScoreThreshold}).");
            }
            else
            {
                detail.PatternStatus = "BUY";
                detail.ExplainabilityBulletPoints.Add("BUY SETUP CONFIRMED: All mandatory rules, geometry, trend, volume, and score criteria satisfied.");
            }

            // 9. Risk Management Calculations
            decimal entry = Math.Round(candle.High + instrument.TickSize, 2);
            decimal stopLoss = Math.Round(candle.Low - (atr * config.StopLossBufferAtr) - instrument.TickSize, 2);
            decimal riskPerUnit = Math.Max(instrument.TickSize, entry - stopLoss);
            decimal target = Math.Round(entry + (riskPerUnit * config.RiskRewardRatio), 2);

            detail.EntryPrice = entry;
            detail.StopLossPrice = stopLoss;
            detail.TargetPrice = target;
            detail.RiskRewardRatio = config.RiskRewardRatio;

            // Position Sizing
            decimal maxRiskBudget = accountCapital * (config.MaxRiskPerTradePercent / 100m);
            int qty = riskPerUnit > 0 ? (int)(maxRiskBudget / riskPerUnit) : instrument.LotSize;
            if (instrument.LotSize > 1)
            {
                qty = Math.Max(instrument.LotSize, (qty / instrument.LotSize) * instrument.LotSize);
            }
            else
            {
                qty = Math.Max(1, qty);
            }

            detail.RecommendedPositionSize = qty;
            detail.RiskAmount = Math.Round(qty * riskPerUnit, 2);

            // 10. Generate TradingSignal if BUY or CANDIDATE
            if (detail.PatternStatus == "BUY" || detail.PatternStatus == "HAMMER_CANDIDATE")
            {
                var condResults = new List<ConditionResult>
                {
                    new() { ConditionName = "Hammer Geometry", IsSatisfied = true, ExpectedValue = $">= {config.MinLowerWickToBodyRatio}x", ActualValue = $"{geo.LowerWickToBodyRatio:F2}x wick" },
                    new() { ConditionName = "Downtrend Context", IsSatisfied = trend.IsDowntrendConfirmed, ExpectedValue = "Bearish", ActualValue = $"{trend.ConditionsPassed}/{trend.TotalConditionsChecked} passed" },
                    new() { ConditionName = "Support Proximity", IsSatisfied = support.IsNearSupport, ExpectedValue = $"<= {config.MaxSupportDistanceAtr} ATR", ActualValue = $"{support.DistanceInAtr:F2} ATR" },
                    new() { ConditionName = "Volume Ratio", IsSatisfied = volume.IsConfirmed, ExpectedValue = $">= {config.MinVolumeRatio}x", ActualValue = $"{volume.VolumeRatio:F2}x" },
                    new() { ConditionName = "Confirmation", IsSatisfied = confirmation.IsConfirmed, ExpectedValue = config.ConfirmationMode, ActualValue = confirmation.ConfirmationStatus }
                };

                detail.GeneratedSignal = new TradingSignal
                {
                    InstrumentId = instrument.InstrumentId,
                    TradingSymbol = instrument.TradingSymbol,
                    Exchange = instrument.Exchange,
                    StrategyId = "HAMMER",
                    StrategyName = "Hammer Pattern Reversal",
                    PptSlideReference = "Slides 5-8",
                    SignalType = SignalDirection.Buy,
                    Status = detail.PatternStatus == "BUY" ? SignalStatus.Confirmed : SignalStatus.EarlyIntrabar,
                    Timeframe = detail.Timeframe,
                    SignalTimestamp = candle.Timestamp,
                    EntryPrice = entry,
                    TargetPrice = target,
                    StopLossPrice = stopLoss,
                    RiskRewardRatio = config.RiskRewardRatio,
                    QualityScore = score.FinalScore,
                    MarketTrend = "Reversal from Downtrend",
                    ConfirmationMode = config.ConfirmationMode,
                    AnalysisSummary = string.Join(" • ", detail.ExplainabilityBulletPoints.Take(4)),
                    Conditions = condResults
                };
            }

            return detail;
        }

        private static decimal CalculateAtr(IReadOnlyList<Candle> candles, int toIndex, int period)
        {
            if (toIndex < 1 || candles.Count < 2) return candles[toIndex].High - candles[toIndex].Low;

            int start = Math.Max(1, toIndex - period + 1);
            decimal sumTr = 0m;
            int count = 0;

            for (int i = start; i <= toIndex; i++)
            {
                decimal h = candles[i].High;
                decimal l = candles[i].Low;
                decimal prevC = candles[i - 1].Close;

                decimal tr = Math.Max(h - l, Math.Max(Math.Abs(h - prevC), Math.Abs(l - prevC)));
                sumTr += tr;
                count++;
            }

            return count > 0 ? Math.Round(sumTr / count, 4) : candles[toIndex].High - candles[toIndex].Low;
        }
    }
}
