using System;
using System.Collections.Generic;
using System.Linq;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies.InverseHammer
{
    public class VolumeEvaluationResult
    {
        public bool IsConfirmed { get; set; }
        public long CurrentVolume { get; set; }
        public long AverageVolume { get; set; }
        public decimal VolumeRatio { get; set; }
        public string VolumeClassification { get; set; } = "Normal"; // Low, Below Average, Normal, Strong, Very Strong
        public bool HasSufficientData { get; set; }
        public string Explanation { get; set; } = string.Empty;
    }

    public static class InverseHammerVolumeAnalyzer
    {
        public static VolumeEvaluationResult AnalyzeVolume(
            IReadOnlyList<Candle> candles,
            int hammerIndex,
            InverseHammerPatternConfig config)
        {
            var result = new VolumeEvaluationResult();

            if (hammerIndex < 1 || candles.Count == 0)
            {
                result.IsConfirmed = false;
                result.HasSufficientData = false;
                result.Explanation = "Volume data unavailable or insufficient candle history.";
                return result;
            }

            var candle = candles[hammerIndex];
            result.CurrentVolume = candle.Volume;

            int period = config.VolumeAveragePeriod;
            int start = Math.Max(0, hammerIndex - period);
            int count = hammerIndex - start;

            if (count < 3)
            {
                result.IsConfirmed = !config.RequireVolumeConfirmation;
                result.HasSufficientData = false;
                result.Explanation = "Insufficient history to calculate 20-period volume SMA.";
                return result;
            }

            long totalVol = 0;
            for (int i = start; i < hammerIndex; i++)
            {
                totalVol += candles[i].Volume;
            }

            long avgVol = totalVol / count;
            result.AverageVolume = avgVol > 0 ? avgVol : 1;
            result.HasSufficientData = true;

            decimal ratio = result.AverageVolume > 0 
                ? (decimal)result.CurrentVolume / result.AverageVolume 
                : 1.0m;
            result.VolumeRatio = Math.Round(ratio, 2);

            // Classification
            if (ratio < 0.80m) result.VolumeClassification = "Low";
            else if (ratio < 1.00m) result.VolumeClassification = "Below Average";
            else if (ratio < 1.50m) result.VolumeClassification = "Normal";
            else if (ratio < 2.00m) result.VolumeClassification = "Strong";
            else result.VolumeClassification = "Very Strong";

            // Mode handling
            string mode = config.VolumeRequirementMode.ToUpperInvariant();
            if (mode == "DISABLED")
            {
                result.IsConfirmed = true;
                result.Explanation = $"Volume filter is disabled ({result.VolumeRatio:F2}x average).";
            }
            else if (mode == "MANDATORY" || config.RequireVolumeConfirmation)
            {
                result.IsConfirmed = result.VolumeRatio >= config.MinVolumeRatio;
                result.Explanation = result.IsConfirmed
                    ? $"Volume confirmed: {result.VolumeRatio:F2}x >= required {config.MinVolumeRatio:F2}x ({result.VolumeClassification})."
                    : $"Volume failed mandatory threshold: {result.VolumeRatio:F2}x < {config.MinVolumeRatio:F2}x ({result.VolumeClassification}).";
            }
            else // "ADVISORY"
            {
                result.IsConfirmed = true;
                result.Explanation = $"Volume is {result.VolumeClassification} ({result.VolumeRatio:F2}x of {period}-period average, advisory mode).";
            }

            return result;
        }
    }
}
