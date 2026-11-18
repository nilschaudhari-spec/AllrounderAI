using System;
using System.Collections.Generic;

namespace AllRounderAi.Core.Strategies.Hammer
{
    public class ScoreBreakdownItem
    {
        public string Component { get; set; } = string.Empty;
        public decimal MaxPoints { get; set; }
        public decimal AwardedPoints { get; set; }
        public string Notes { get; set; } = string.Empty;
    }

    public class HammerScoreResult
    {
        public decimal FinalScore { get; set; }
        public string Classification { get; set; } = "Strong"; // Excellent, Strong, Moderate, Weak
        public List<ScoreBreakdownItem> Breakdown { get; set; } = new();
        public string Summary { get; set; } = string.Empty;
    }

    public static class HammerScoringEngine
    {
        public static HammerScoreResult CalculateScore(
            HammerCandleGeometry geometry,
            TrendEvaluationResult trend,
            SupportEvaluationResult support,
            VolumeEvaluationResult volume,
            ConfirmationEvaluationResult confirmation,
            HammerPatternConfig config)
        {
            var result = new HammerScoreResult();
            decimal totalWeightAvailable = 0m;
            decimal totalPointsAwarded = 0m;

            // 1. Candle Geometry (Weight: 30)
            decimal geoMax = config.WeightGeometry;
            totalWeightAvailable += geoMax;
            decimal geoPoints = 0m;
            if (geometry.IsValid && !geometry.IsZeroBodyDoji)
            {
                // Wick ratio contribution (up to 15 pts)
                decimal wickBonus = Math.Min(15m, (geometry.LowerWickToBodyRatio / 3.0m) * 15m);
                // Body position contribution (up to 10 pts)
                decimal posBonus = Math.Min(10m, (geometry.BodyPosition / 0.85m) * 10m);
                // Color bonus (green hammer gets 5 pts, red gets 2 pts)
                decimal colorBonus = geometry.IsBullishColor ? 5m : 2m;
                geoPoints = Math.Min(geoMax, wickBonus + posBonus + colorBonus);
            }
            result.Breakdown.Add(new ScoreBreakdownItem
            {
                Component = "Candle Geometry",
                MaxPoints = geoMax,
                AwardedPoints = Math.Round(geoPoints, 1),
                Notes = $"Lower wick {geometry.LowerWickToBodyRatio:F1}x body, position {geometry.BodyPosition:P0}, {(geometry.IsBullishColor ? "Bullish Green" : "Bearish Red")}"
            });
            totalPointsAwarded += geoPoints;

            // 2. Downtrend Context (Weight: 20)
            decimal trendMax = config.WeightTrend;
            totalWeightAvailable += trendMax;
            decimal trendPoints = 0m;
            if (trend.IsDowntrendConfirmed)
            {
                decimal passRatio = trend.TotalConditionsChecked > 0 ? (decimal)trend.ConditionsPassed / trend.TotalConditionsChecked : 1.0m;
                trendPoints = Math.Round(trendMax * passRatio, 1);
            }
            result.Breakdown.Add(new ScoreBreakdownItem
            {
                Component = "Downtrend Context",
                MaxPoints = trendMax,
                AwardedPoints = trendPoints,
                Notes = $"{trend.ConditionsPassed}/{trend.TotalConditionsChecked} conditions passed ({trend.PriceDeclinePercent:F1}% prior drop)"
            });
            totalPointsAwarded += trendPoints;

            // 3. Support Proximity (Weight: 15)
            decimal suppMax = config.WeightSupport;
            totalWeightAvailable += suppMax;
            decimal suppPoints = 0m;
            if (support.IsNearSupport)
            {
                decimal closeness = Math.Max(0m, 1.0m - (support.DistanceInAtr / 1.5m));
                suppPoints = Math.Round(suppMax * closeness, 1);
            }
            result.Breakdown.Add(new ScoreBreakdownItem
            {
                Component = "Support Proximity",
                MaxPoints = suppMax,
                AwardedPoints = suppPoints,
                Notes = support.IsNearSupport
                    ? $"{support.SupportType} at {support.DistanceInAtr:F2} ATR ({support.TouchesCount} touches)"
                    : "No nearby major support zone confirmed"
            });
            totalPointsAwarded += suppPoints;

            // 4. Volume Confirmation (Weight: 10)
            decimal volMax = config.WeightVolume;
            totalWeightAvailable += volMax;
            decimal volPoints = 0m;
            if (volume.HasSufficientData)
            {
                volPoints = volume.VolumeClassification switch
                {
                    "Very Strong" => volMax,
                    "Strong" => volMax * 0.85m,
                    "Normal" => volMax * 0.60m,
                    "Below Average" => volMax * 0.30m,
                    _ => 0m
                };
            }
            else
            {
                volPoints = volMax * 0.50m; // Neutral default if missing
            }
            result.Breakdown.Add(new ScoreBreakdownItem
            {
                Component = "Volume Confirmation",
                MaxPoints = volMax,
                AwardedPoints = Math.Round(volPoints, 1),
                Notes = $"{volume.VolumeClassification} ({volume.VolumeRatio:F2}x average volume)"
            });
            totalPointsAwarded += volPoints;

            // 5. Confirmation Candle (Weight: 15)
            decimal confMax = config.WeightConfirmation;
            totalWeightAvailable += confMax;
            decimal confPoints = 0m;
            if (confirmation.IsConfirmed)
            {
                confPoints = confMax;
            }
            else if (confirmation.ConfirmationStatus == "INTRABAR_CANDIDATE")
            {
                confPoints = confMax * 0.50m;
            }
            result.Breakdown.Add(new ScoreBreakdownItem
            {
                Component = "Confirmation Status",
                MaxPoints = confMax,
                AwardedPoints = Math.Round(confPoints, 1),
                Notes = confirmation.ConfirmationStatus
            });
            totalPointsAwarded += confPoints;

            // Normalize to 100
            decimal normalizedScore = totalWeightAvailable > 0
                ? Math.Round((totalPointsAwarded / totalWeightAvailable) * 100m, 1)
                : 0m;

            result.FinalScore = Math.Min(100m, Math.Max(0m, normalizedScore));
            result.Classification = result.FinalScore switch
            {
                >= 85.0m => "Excellent",
                >= 70.0m => "Strong",
                >= 55.0m => "Moderate",
                _ => "Weak"
            };

            result.Summary = $"Hammer Quality Score: {result.FinalScore}/100 ({result.Classification}). Total points: {totalPointsAwarded:F1}/{totalWeightAvailable:F1}.";
            return result;
        }
    }
}
