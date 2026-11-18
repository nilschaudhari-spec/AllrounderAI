using System;
using System.Collections.Generic;
using System.Linq;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies.Hammer
{
    public class TrendEvaluationResult
    {
        public bool IsDowntrendConfirmed { get; set; }
        public int ConditionsPassed { get; set; }
        public int TotalConditionsChecked { get; set; }
        public decimal PriceDeclinePercent { get; set; }
        public List<string> PassedConditions { get; set; } = new();
        public List<string> FailedConditions { get; set; } = new();
        public string SummaryExplanation { get; set; } = string.Empty;
    }

    public static class HammerTrendDetector
    {
        public static TrendEvaluationResult EvaluateDowntrend(
            IReadOnlyList<Candle> candles, 
            int hammerIndex, 
            HammerPatternConfig config)
        {
            var result = new TrendEvaluationResult();
            if (hammerIndex < 2)
            {
                result.IsDowntrendConfirmed = false;
                result.SummaryExplanation = "Insufficient historical candles to determine trend.";
                return result;
            }

            int lookback = Math.Min(config.TrendLookback, hammerIndex);
            var lookbackCandles = new List<Candle>();
            for (int i = hammerIndex - lookback; i < hammerIndex; i++)
            {
                lookbackCandles.Add(candles[i]);
            }

            // Condition 1: Consecutive Lower Highs or Lower Lows
            int lowerHighsCount = 0;
            int lowerLowsCount = 0;
            for (int i = 1; i < lookbackCandles.Count; i++)
            {
                if (lookbackCandles[i].High <= lookbackCandles[i - 1].High) lowerHighsCount++;
                if (lookbackCandles[i].Low <= lookbackCandles[i - 1].Low) lowerLowsCount++;
            }
            bool structureBearish = (lowerHighsCount + lowerLowsCount) >= (lookbackCandles.Count - 1);
            if (structureBearish)
                result.PassedConditions.Add($"Bearish Structure ({lowerHighsCount} lower highs, {lowerLowsCount} lower lows over {lookback} bars)");
            else
                result.FailedConditions.Add($"Bearish Structure: Mixed highs/lows ({lowerHighsCount} LH, {lowerLowsCount} LL)");

            // Condition 2: Net Price Percentage Decline
            decimal startPrice = lookbackCandles[0].Close;
            decimal endPrice = lookbackCandles.Last().Close;
            decimal declinePct = startPrice > 0 ? ((endPrice - startPrice) / startPrice) * 100m : 0m;
            result.PriceDeclinePercent = Math.Round(declinePct, 2);

            bool priceDeclined = declinePct <= -0.20m; // At least -0.20% net drop
            if (priceDeclined)
                result.PassedConditions.Add($"Net Price Decline of {declinePct:F2}% over {lookback} candles");
            else
                result.FailedConditions.Add($"Net Price Change was positive/flat ({declinePct:F2}%)");

            // Condition 3: Moving Average Context (Price < EMA 20 or Prior Close < EMA 20)
            decimal ema20 = CalculateSimpleEma(candles, hammerIndex - 1, 20);
            bool belowEma = candles[hammerIndex - 1].Close <= ema20;
            if (belowEma)
                result.PassedConditions.Add($"Prior Candle Close (₹{candles[hammerIndex - 1].Close:F2}) < EMA 20 (₹{ema20:F2})");
            else
                result.FailedConditions.Add($"Prior Candle Close above EMA 20 (₹{ema20:F2})");

            result.ConditionsPassed = result.PassedConditions.Count;
            result.TotalConditionsChecked = 3;

            // Apply Mode Hierarchy
            result.IsDowntrendConfirmed = config.TrendMode.ToUpperInvariant() switch
            {
                "STRICT" => result.ConditionsPassed == result.TotalConditionsChecked,
                "RELAXED" => result.ConditionsPassed >= 1,
                _ => result.ConditionsPassed >= 2 // "MODERATE"
            };

            result.SummaryExplanation = result.IsDowntrendConfirmed
                ? $"Downtrend confirmed ({result.ConditionsPassed}/{result.TotalConditionsChecked} conditions passed: {string.Join("; ", result.PassedConditions)})"
                : $"Downtrend not confirmed ({result.ConditionsPassed}/{result.TotalConditionsChecked} passed; failed: {string.Join("; ", result.FailedConditions)})";

            return result;
        }

        private static decimal CalculateSimpleEma(IReadOnlyList<Candle> candles, int toIndex, int period)
        {
            if (toIndex < 0 || candles.Count == 0) return 0m;
            int start = Math.Max(0, toIndex - period + 1);
            decimal sum = 0m;
            int count = 0;
            for (int i = start; i <= toIndex; i++)
            {
                sum += candles[i].Close;
                count++;
            }
            return count > 0 ? Math.Round(sum / count, 2) : candles[toIndex].Close;
        }
    }
}
