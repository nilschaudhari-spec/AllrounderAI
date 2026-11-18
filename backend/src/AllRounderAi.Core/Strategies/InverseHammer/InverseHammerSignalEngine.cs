using System;
using System.Collections.Generic;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies.InverseHammer
{
    public class InverseHammerDetectionDetail
    {
        public string DetectionId { get; set; } = Guid.NewGuid().ToString();
        public string InstrumentId { get; set; } = string.Empty;
        public string TradingSymbol { get; set; } = string.Empty;
        public string Exchange { get; set; } = "NSE";
        public string Segment { get; set; } = "EQUITY";
        public string Timeframe { get; set; } = "5m";
        public DateTime CandleTimestamp { get; set; }
        public string PatternStatus { get; set; } = "NO_TRADE"; // INVERSE_HAMMER_DETECTED, INVERSE_HAMMER_GEOMETRY_ONLY, INVERSE_HAMMER_CANDIDATE, INVERSE_HAMMER_CONFIRMED, BUY, NO_TRADE, INVALIDATED

        public InverseHammerCandleGeometry Geometry { get; set; } = new();
        public TrendEvaluationResult Trend { get; set; } = new();
        public SupportEvaluationResult Support { get; set; } = new();
        public VolumeEvaluationResult Volume { get; set; } = new();
        public ConfirmationEvaluationResult Confirmation { get; set; } = new();
        public InverseHammerScoreResult Score { get; set; } = new();

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

    public static class InverseHammerSignalEngine
    {
        public static InverseHammerDetectionDetail AnalyzeInverseHammer(
            IReadOnlyList<Candle> candles,
            Instrument instrument,
            InverseHammerPatternConfig config,
            int targetIndex = -1,
            bool isDevelopingCandle = false,
            decimal accountCapital = 100000m)
        {
            var detail = new InverseHammerDetectionDetail
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
                detail.ExplainabilityBulletPoints.Add("Insufficient candle history to evaluate Inverse Hammer setup.");
                return detail;
            }

            var candle = candles[idx];
            detail.CandleTimestamp = candle.Timestamp;

            // 1. Calculate Average True Range (ATR 14)
            decimal atr = CalculateAtr(candles, idx, config.AtrPeriod);
            detail.Atr = atr;

            // 2. Geometry Calculation
            var geo = InverseHammerCandleGeometry.Calculate(candle, atr);
            detail.Geometry = geo;

            if (!geo.IsValid || geo.IsZeroBodyDoji)
            {
                detail.PatternStatus = "INVALIDATED";
                detail.ExplainabilityBulletPoints.Add(geo.IsZeroBodyDoji
                    ? "Zero-body Doji rejected: Institutional policy requires an identifiable real body in lower range."
                    : $"Geometry invalid: {geo.ValidationError}");
                return detail;
            }

            // Verify Geometry Thresholds
            bool upperWickPassed = geo.UpperWickToBodyRatio >= config.MinUpperWickToBodyRatio;
            bool lowerWickPassed = geo.LowerWickToRangeRatio <= config.MaxLowerWickToRangeRatio;
            bool bodyRatioPassed = geo.BodyToRangeRatio <= config.MaxBodyToRangeRatio;
            bool bodyPosPassed = geo.BodyPosition <= config.MaxBodyPosition; // Body must be near candle low
            bool rangeAtrPassed = geo.RangeToAtrRatio >= config.MinCandleRangeToAtrRatio;

            bool geometryQualified = upperWickPassed && lowerWickPassed && bodyRatioPassed && bodyPosPassed && rangeAtrPassed;

            detail.ExplainabilityBulletPoints.Add($"Upper wick is {geo.UpperWickToBodyRatio:F2}× candle body (minimum required: {config.MinUpperWickToBodyRatio:F2}×).");
            detail.ExplainabilityBulletPoints.Add($"Lower wick occupies {geo.LowerWickToRangeRatio:P1} of range (maximum allowed: {config.MaxLowerWickToRangeRatio:P1}).");
            detail.ExplainabilityBulletPoints.Add($"Body occupies {geo.BodyToRangeRatio:P1} of candle range (maximum allowed: {config.MaxBodyToRangeRatio:P1}).");
            detail.ExplainabilityBulletPoints.Add($"Body position is at {geo.BodyPosition:P1} of candle range from low (maximum allowed: {config.MaxBodyPosition:P1}).");
            detail.ExplainabilityBulletPoints.Add($"Candle range is {geo.RangeToAtrRatio:F2}× ATR.");

            if (!geometryQualified)
            {
                detail.PatternStatus = "INVALIDATED";
                if (!upperWickPassed) detail.ExplainabilityBulletPoints.Add($"Failed upper wick ratio ({geo.UpperWickToBodyRatio:F2}x < {config.MinUpperWickToBodyRatio:F2}x).");
                if (!lowerWickPassed) detail.ExplainabilityBulletPoints.Add($"Failed lower wick threshold ({geo.LowerWickToRangeRatio:P1} > {config.MaxLowerWickToRangeRatio:P1}).");
                if (!bodyRatioPassed) detail.ExplainabilityBulletPoints.Add($"Body size too large ({geo.BodyToRangeRatio:P1} > {config.MaxBodyToRangeRatio:P1}).");
                if (!bodyPosPassed) detail.ExplainabilityBulletPoints.Add($"Body not situated near low ({geo.BodyPosition:P1} > {config.MaxBodyPosition:P1}).");
                if (!rangeAtrPassed) detail.ExplainabilityBulletPoints.Add($"Insufficient range volatility ({geo.RangeToAtrRatio:F2}x ATR < {config.MinCandleRangeToAtrRatio:F2}x).");
                return detail;
            }

            // 3. Trend Context Evaluation
            var trend = InverseHammerTrendDetector.EvaluateDowntrend(candles, idx, config);
            detail.Trend = trend;
            detail.ExplainabilityBulletPoints.Add(trend.SummaryExplanation);

            if (config.RequireDowntrend && !trend.IsDowntrendConfirmed)
            {
                // GEOMETRY_ONLY separation
                detail.PatternStatus = "INVERSE_HAMMER_GEOMETRY_ONLY";
                detail.ExplainabilityBulletPoints.Add("Classified as INVERSE_HAMMER_GEOMETRY_ONLY: Pattern geometry satisfied, but bearish trend context failed. No BUY signal generated.");
                return detail;
            }

            // 4. Support Proximity Evaluation
            var support = InverseHammerSupportDetector.EvaluateSupportProximity(candles, idx, atr, config.MaxSupportDistanceAtr);
            detail.Support = support;
            detail.ExplainabilityBulletPoints.Add(support.Explanation);

            if (config.RequireSupportProximity && !support.IsNearSupport)
            {
                detail.PatternStatus = "WATCH";
                detail.ExplainabilityBulletPoints.Add("Mandatory support requirement not satisfied. Trade setup suspended.");
                return detail;
            }

            // 5. Volume Confirmation Evaluation
            var volume = InverseHammerVolumeAnalyzer.AnalyzeVolume(candles, idx, config);
            detail.Volume = volume;
            detail.ExplainabilityBulletPoints.Add(volume.Explanation);

            if (config.RequireVolumeConfirmation && !volume.IsConfirmed)
            {
                detail.PatternStatus = "WATCH";
                detail.ExplainabilityBulletPoints.Add("Mandatory volume confirmation rule failed. Setup marked as WATCH.");
                return detail;
            }

            // 6. Confirmation Mode Engine
            var confirmation = InverseHammerConfirmationEngine.EvaluateConfirmation(candles, idx, geo, config, isDevelopingCandle);
            detail.Confirmation = confirmation;
            detail.ExplainabilityBulletPoints.Add(confirmation.Explanation);

            if (!confirmation.IsConfirmed)
            {
                detail.PatternStatus = confirmation.ConfirmationStatus switch
                {
                    "INTRABAR_CANDIDATE" => "INVERSE_HAMMER_CANDIDATE",
                    "AWAITING_NEXT_CANDLE" => "INVERSE_HAMMER_CANDIDATE",
                    "FORMING" => "WATCH",
                    _ => "INVALIDATED"
                };
                return detail;
            }

            // 7. Calculate Explainable Quality Score (0 - 100)
            var score = InverseHammerScoringEngine.CalculateScore(geo, trend, support, volume, confirmation, config);
            detail.Score = score;
            detail.ExplainabilityBulletPoints.Add(score.Summary);

            if (score.FinalScore < config.ScoreThreshold)
            {
                detail.PatternStatus = "WATCH";
                detail.ExplainabilityBulletPoints.Add($"Score {score.FinalScore:F1} below minimum threshold of {config.ScoreThreshold:F1}.");
                return detail;
            }

            // 8. 1:1 Risk-to-Reward Trade Planning
            decimal tick = instrument.TickSize > 0 ? instrument.TickSize : 0.05m;
            decimal entryPrice = Math.Round(candle.High + tick, 2);
            decimal stopLossPrice = Math.Round(candle.Low - (atr * config.StopLossBufferAtr), 2);
            decimal riskPerShare = entryPrice - stopLossPrice;

            if (riskPerShare <= 0)
            {
                riskPerShare = Math.Round(atr * 0.50m, 2);
                stopLossPrice = entryPrice - riskPerShare;
            }

            decimal targetPrice = Math.Round(entryPrice + (riskPerShare * config.RiskRewardRatio), 2);

            detail.EntryPrice = entryPrice;
            detail.StopLossPrice = stopLossPrice;
            detail.TargetPrice = targetPrice;
            detail.RiskRewardRatio = config.RiskRewardRatio;

            // Position Sizing
            decimal maxRiskCapital = accountCapital * (config.MaxRiskPerTradePercent / 100m);
            int posSize = riskPerShare > 0 ? (int)(maxRiskCapital / riskPerShare) : 1;
            detail.RecommendedPositionSize = Math.Max(1, posSize);
            detail.RiskAmount = Math.Round(detail.RecommendedPositionSize * riskPerShare, 2);

            detail.PatternStatus = "BUY";
            detail.ExplainabilityBulletPoints.Add("BUY SETUP CONFIRMED: All mandatory rules, geometry, trend, volume, and score criteria satisfied.");

            // 9. Generate Trading Signal
            detail.GeneratedSignal = new TradingSignal
            {
                SignalId = Guid.NewGuid().ToString(),
                InstrumentId = instrument.InstrumentId,
                TradingSymbol = instrument.TradingSymbol,
                Exchange = instrument.Exchange,
                StrategyId = "INVERSE_HAMMER",
                StrategyName = "Inverse Hammer Pattern Reversal",
                PptSlideReference = "Slides 11-12",
                SignalType = SignalDirection.Buy,
                Status = SignalStatus.Confirmed,
                Timeframe = detail.Timeframe,
                SignalTimestamp = candle.Timestamp,
                EntryPrice = detail.EntryPrice,
                TargetPrice = detail.TargetPrice,
                StopLossPrice = detail.StopLossPrice,
                RiskRewardRatio = detail.RiskRewardRatio,
                QualityScore = detail.Score.FinalScore,
                MarketTrend = "Reversal from Downtrend",
                ConfirmationMode = config.ConfirmationMode,
                AnalysisSummary = string.Join(" • ", detail.ExplainabilityBulletPoints.GetRange(0, Math.Min(4, detail.ExplainabilityBulletPoints.Count))),
                Conditions = new List<ConditionResult>
                {
                    new() { ConditionName = "Inverse Hammer Geometry", IsSatisfied = true, ExpectedValue = $">= {config.MinUpperWickToBodyRatio:F1}x", ActualValue = $"{geo.UpperWickToBodyRatio:F2}x wick" },
                    new() { ConditionName = "Downtrend Context", IsSatisfied = trend.IsDowntrendConfirmed, ExpectedValue = "Bearish", ActualValue = $"{trend.ConditionsPassed}/{trend.TotalConditionsChecked} passed" },
                    new() { ConditionName = "Support Proximity", IsSatisfied = support.IsNearSupport, ExpectedValue = $"<= {config.MaxSupportDistanceAtr:F1} ATR", ActualValue = $"{support.DistanceInAtr:F2} ATR" },
                    new() { ConditionName = "Volume Ratio", IsSatisfied = volume.IsConfirmed, ExpectedValue = $">= {config.MinVolumeRatio:F2}x", ActualValue = $"{volume.VolumeRatio:F2}x" },
                    new() { ConditionName = "Confirmation", IsSatisfied = confirmation.IsConfirmed, ExpectedValue = config.ConfirmationMode, ActualValue = confirmation.ConfirmationStatus }
                }
            };

            return detail;
        }

        private static decimal CalculateAtr(IReadOnlyList<Candle> candles, int targetIndex, int period)
        {
            if (candles.Count == 0 || targetIndex <= 0) return 1.0m;
            int start = Math.Max(1, targetIndex - period + 1);
            decimal trSum = 0m;
            int count = 0;
            for (int i = start; i <= targetIndex; i++)
            {
                decimal tr = Math.Max(candles[i].High - candles[i].Low,
                             Math.Max(Math.Abs(candles[i].High - candles[i - 1].Close),
                                      Math.Abs(candles[i].Low - candles[i - 1].Close)));
                trSum += tr;
                count++;
            }
            return count > 0 ? Math.Round(trSum / count, 4) : 1.0m;
        }
    }
}
