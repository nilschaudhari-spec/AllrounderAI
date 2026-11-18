using System;
using System.Collections.Generic;
using System.Linq;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies.Hammer
{
    public class VolumeEvaluationResult
    {
        public bool IsConfirmed { get; set; }
        public long CurrentVolume { get; set; }
        public long AverageVolume { get; set; }
        public decimal VolumeRatio { get; set; }
        public string VolumeClassification { get; set; } = "Normal"; // Low, BelowAverage, Normal, Strong, VeryStrong
        public bool HasSufficientData { get; set; }
        public string Explanation { get; set; } = string.Empty;
    }

    public static class HammerVolumeAnalyzer
    {
        public static VolumeEvaluationResult EvaluateVolume(
            IReadOnlyList<Candle> candles,
            int hammerIndex,
            HammerPatternConfig config)
        {
            var result = new VolumeEvaluationResult();
            if (hammerIndex < 1 || candles.Count == 0)
            {
                result.IsConfirmed = config.VolumeRequirementMode != "Mandatory";
                result.Explanation = "Insufficient data for volume analysis.";
                return result;
            }

            long currentVol = candles[hammerIndex].Volume;
            result.CurrentVolume = currentVol;

            if (currentVol <= 0)
            {
                result.HasSufficientData = false;
                result.VolumeClassification = "Unavailable";
                result.VolumeRatio = 0m;
                result.IsConfirmed = config.VolumeRequirementMode != "Mandatory";
                result.Explanation = "Volume data unavailable or zero. Missing volume not treated as confirmation.";
                return result;
            }

            int period = Math.Min(config.VolumeAveragePeriod, hammerIndex);
            long sumVol = 0;
            int count = 0;
            for (int i = hammerIndex - period; i < hammerIndex; i++)
            {
                if (candles[i].Volume > 0)
                {
                    sumVol += candles[i].Volume;
                    count++;
                }
            }

            long avgVol = count > 0 ? sumVol / count : currentVol;
            result.AverageVolume = avgVol;
            result.HasSufficientData = true;

            decimal ratio = avgVol > 0 ? (decimal)currentVol / (decimal)avgVol : 1.0m;
            result.VolumeRatio = Math.Round(ratio, 2);

            result.VolumeClassification = ratio switch
            {
                < 0.80m => "Low",
                >= 0.80m and < 1.00m => "Below Average",
                >= 1.00m and < 1.50m => "Normal",
                >= 1.50m and <= 2.00m => "Strong",
                _ => "Very Strong"
            };

            bool passesRatio = ratio >= config.MinVolumeRatio;
            if (config.VolumeRequirementMode.Equals("Mandatory", StringComparison.OrdinalIgnoreCase))
            {
                result.IsConfirmed = passesRatio;
                result.Explanation = passesRatio
                    ? $"Volume confirmed: Current volume ({currentVol:N0}) is {ratio:F2}x of {period}-period average ({result.VolumeClassification})."
                    : $"Volume failed mandatory threshold: Current volume is {ratio:F2}x (minimum required {config.MinVolumeRatio:F2}x).";
            }
            else // Advisory or Disabled
            {
                result.IsConfirmed = true;
                result.Explanation = $"Volume is {result.VolumeClassification} ({ratio:F2}x of {period}-period average, advisory mode).";
            }

            return result;
        }
    }
}
