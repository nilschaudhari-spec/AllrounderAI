using System;
using System.Collections.Generic;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies.Hammer
{
    public class ConfirmationEvaluationResult
    {
        public bool IsConfirmed { get; set; }
        public string ConfirmationStatus { get; set; } = "CONFIRMED"; // FORMING, INTRABAR_CANDIDATE, CONFIRMED, INVALIDATED
        public string MethodUsed { get; set; } = "CandleClose";
        public decimal ConfirmationPrice { get; set; }
        public string Explanation { get; set; } = string.Empty;
    }

    public static class HammerConfirmationEngine
    {
        public static ConfirmationEvaluationResult EvaluateConfirmation(
            IReadOnlyList<Candle> candles,
            int hammerIndex,
            HammerCandleGeometry geometry,
            HammerPatternConfig config,
            bool isDevelopingCandle = false)
        {
            var result = new ConfirmationEvaluationResult();
            var hammer = candles[hammerIndex];

            // 1. Mode A: Intrabar Developing Candle
            if (config.ConfirmationMode.Equals("Intrabar", StringComparison.OrdinalIgnoreCase) && isDevelopingCandle)
            {
                result.MethodUsed = "IntrabarRealtime";
                if (!geometry.IsValid)
                {
                    result.ConfirmationStatus = "INVALIDATED";
                    result.IsConfirmed = false;
                    result.Explanation = "Intrabar pattern invalidated: Candle geometry failed integrity rules.";
                }
                else if (geometry.LowerWickToBodyRatio >= config.MinLowerWickToBodyRatio &&
                         geometry.UpperWickToRangeRatio <= config.MaxUpperWickToRangeRatio &&
                         geometry.BodyPosition >= config.MinBodyPosition)
                {
                    result.ConfirmationStatus = "INTRABAR_CANDIDATE";
                    result.IsConfirmed = false; // Intrabar candidate is NOT final confirmed
                    result.Explanation = "Intrabar candidate: Live developing candle satisfies Hammer geometry. Awaiting candle close.";
                }
                else
                {
                    result.ConfirmationStatus = "FORMING";
                    result.IsConfirmed = false;
                    result.Explanation = "Developing candle currently forming. Requirements not yet met.";
                }
                return result;
            }

            // 2. Mode B: Candle Close Mode (Finalized OHLC)
            result.MethodUsed = "CandleClose";
            result.ConfirmationPrice = hammer.Close;

            // 3. Next-Candle Confirmation (if configured)
            if (config.RequireNextCandleConfirmation)
            {
                if (hammerIndex >= candles.Count - 1)
                {
                    // Next candle has not formed yet!
                    result.ConfirmationStatus = "AWAITING_NEXT_CANDLE";
                    result.IsConfirmed = false;
                    result.Explanation = $"Hammer closed. Awaiting next candle confirmation ({config.NextCandleConfirmationMethod}).";
                    return result;
                }

                var nextCandle = candles[hammerIndex + 1];
                decimal hammerBodyHigh = Math.Max(hammer.Open, hammer.Close);

                bool confirmed = config.NextCandleConfirmationMethod switch
                {
                    "CloseAboveBodyHigh" => nextCandle.Close > hammerBodyHigh,
                    "BreakHighIntrabar" => nextCandle.High > hammer.High,
                    "BullishBody" => nextCandle.Close > nextCandle.Open,
                    "BullishWithVolume" => nextCandle.Close > nextCandle.Open && nextCandle.Volume >= hammer.Volume,
                    _ => nextCandle.Close > hammer.High // Default: "CloseAboveHammerHigh"
                };

                result.MethodUsed = config.NextCandleConfirmationMethod;
                result.ConfirmationPrice = nextCandle.Close;

                if (confirmed)
                {
                    result.ConfirmationStatus = "CONFIRMED";
                    result.IsConfirmed = true;
                    result.Explanation = $"Next-candle confirmation verified: Bar closed at ₹{nextCandle.Close:F2} satisfying rule '{config.NextCandleConfirmationMethod}'.";
                }
                else
                {
                    result.ConfirmationStatus = "INVALIDATED";
                    result.IsConfirmed = false;
                    result.Explanation = $"Next candle failed confirmation: Bar closed at ₹{nextCandle.Close:F2} without satisfying '{config.NextCandleConfirmationMethod}'.";
                }
                return result;
            }

            // Standard Candle Close confirmation
            result.ConfirmationStatus = "CONFIRMED";
            result.IsConfirmed = true;
            result.Explanation = $"Candle-close confirmation satisfied on finalized bar timestamp {hammer.Timestamp:HH:mm:ss}.";
            return result;
        }
    }
}
