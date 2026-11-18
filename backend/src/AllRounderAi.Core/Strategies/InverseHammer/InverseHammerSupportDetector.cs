using System;
using System.Collections.Generic;
using System.Linq;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies.InverseHammer
{
    public class SupportEvaluationResult
    {
        public bool IsNearSupport { get; set; }
        public decimal NearestSupportPrice { get; set; }
        public string SupportType { get; set; } = "None"; // SwingLow, PivotS1, VWAP, DailyLow, MovingAverage
        public decimal AbsoluteDistance { get; set; }
        public decimal DistanceInAtr { get; set; }
        public int TouchesCount { get; set; } = 1;
        public string StrengthLevel { get; set; } = "Moderate"; // Weak, Moderate, Strong, KeyInstitutional
        public string Explanation { get; set; } = string.Empty;
    }

    public static class InverseHammerSupportDetector
    {
        public static SupportEvaluationResult EvaluateSupportProximity(
            IReadOnlyList<Candle> candles,
            int hammerIndex,
            decimal atr,
            decimal maxSupportDistanceAtr)
        {
            var result = new SupportEvaluationResult();
            if (hammerIndex < 5 || candles.Count == 0)
            {
                result.IsNearSupport = false;
                result.Explanation = "Insufficient data to evaluate support levels.";
                return result;
            }

            decimal inverseHammerLow = candles[hammerIndex].Low;
            var supportCandidates = new List<(decimal Price, string Type, int Touches, string Strength)>();

            // 1. Detect Swing Lows & Repeated Lows in prior 50 candles
            int lookbackStart = Math.Max(0, hammerIndex - 50);
            var pastLows = new List<decimal>();
            for (int i = lookbackStart; i < hammerIndex; i++)
            {
                pastLows.Add(candles[i].Low);
            }

            decimal lowestPrior = pastLows.Min();
            int touches = pastLows.Count(l => Math.Abs(l - lowestPrior) <= (atr * 0.25m));
            supportCandidates.Add((lowestPrior, "Prior Swing Low", touches, touches > 2 ? "Strong" : "Moderate"));

            // 2. Intraday VWAP Support Level
            decimal cumVol = 0m;
            decimal cumTypVol = 0m;
            int vwapStart = Math.Max(0, hammerIndex - 20);
            for (int i = vwapStart; i < hammerIndex; i++)
            {
                decimal typPrice = (candles[i].High + candles[i].Low + candles[i].Close) / 3m;
                long vol = candles[i].Volume > 0 ? candles[i].Volume : 1000;
                cumTypVol += typPrice * vol;
                cumVol += vol;
            }
            if (cumVol > 0)
            {
                decimal vwap = Math.Round(cumTypVol / cumVol, 2);
                supportCandidates.Add((vwap, "Dynamic VWAP Support", 1, "Moderate"));
            }

            // 3. Find candidate closest to Inverse Hammer Low
            var bestSupport = supportCandidates
                .OrderBy(s => Math.Abs(inverseHammerLow - s.Price))
                .FirstOrDefault();

            if (bestSupport.Price <= 0)
            {
                result.IsNearSupport = false;
                result.Explanation = "No valid support candidate found.";
                return result;
            }

            decimal dist = Math.Abs(inverseHammerLow - bestSupport.Price);
            decimal distAtr = atr > 0 ? Math.Round(dist / atr, 3) : 1.0m;

            result.NearestSupportPrice = bestSupport.Price;
            result.SupportType = bestSupport.Type;
            result.AbsoluteDistance = Math.Round(dist, 2);
            result.DistanceInAtr = distAtr;
            result.TouchesCount = bestSupport.Touches;
            result.StrengthLevel = bestSupport.Strength;

            result.IsNearSupport = distAtr <= maxSupportDistanceAtr;
            result.Explanation = result.IsNearSupport
                ? $"Inverse Hammer low (₹{inverseHammerLow:F2}) confirmed near {bestSupport.Type} (₹{bestSupport.Price:F2}) at {distAtr:F2} ATR distance ({bestSupport.Touches} touches)."
                : $"Inverse Hammer low (₹{inverseHammerLow:F2}) is outside support proximity ({distAtr:F2} ATR > threshold of {maxSupportDistanceAtr:F2} ATR).";

            return result;
        }
    }
}
