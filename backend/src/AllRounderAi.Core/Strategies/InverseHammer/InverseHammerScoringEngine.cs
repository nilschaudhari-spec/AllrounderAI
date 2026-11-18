using System;
using System.Collections.Generic;

namespace AllRounderAi.Core.Strategies.InverseHammer
{
    public class ScoreBreakdownItem
    {
        public string Component { get; set; } = string.Empty;
        public decimal MaxPoints { get; set; }
        public decimal AwardedPoints { get; set; }
        public string Notes { get; set; } = string.Empty;
    }

    public class InverseHammerScoreResult
    {
        public decimal FinalScore { get; set; }
        public string Classification { get; set; } = "Strong"; // Excellent, Strong, Moderate, Weak
        public List<ScoreBreakdownItem> Breakdown { get; set; } = new();
        public string Summary { get; set; } = string.Empty;
    }

    public static class InverseHammerScoringEngine
    {
        public static InverseHammerScoreResult CalculateScore(
            InverseHammerCandleGeometry geometry,
            TrendEvaluationResult trend,
            SupportEvaluationResult support,
            VolumeEvaluationResult volume,
            ConfirmationEvaluationResult confirmation,
            InverseHammerPatternConfig config)
        {
            var result = new InverseHammerScoreResult();
            decimal totalWeightAvailable = 0m;
            decimal totalPointsAwarded = 0m;

            // 1. Candle Geometry (Weight: 30)
            decimal geoMax = config.WeightGeometry;
            totalWeightAvailable += geoMax;
            decimal geoPoints = 0m;
            if (geometry.IsValid && !geometry.IsZeroBodyDoji)
            {
                // Upper wick ratio contribution (up to 15 pts)
                decimal wickBonus = Math.Min(15m, (geometry.UpperWickToBodyRatio / 3.0m) * 15m);
                // Body position near bottom (<= 40% from low) contribution (up to 10 pts)
                decimal posCloseness = Math.Max(0m, 1.0m - (geometry.BodyPosition / 0.50m));
                decimal posBonus = Math.Min(10m, posCloseness * 10m);
                // Color bonus (green inverse hammer gets 5 pts, red gets 2 pts)
                decimal colorBonus = geometry.IsBullishColor ? 5m : 2m;
                geoPoints = Math.Min(geoMax, wickBonus + posBonus + colorBonus);
            }
            result.Breakdown.Add(new ScoreBreakdownItem
            {
                Component = "Candle Geometry",
                MaxPoints = geoMax,
                AwardedPoints = Math.Round(geoPoints, 1),
                Notes = $"Upper wick {geometry.UpperWickToBodyRatio:F1}x body, position {geometry.BodyPosition:P0}, {(geometry.IsBullishColor ? "Bullish Green" : "Bearish Red")}"
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
                    "Very Strong" => 10.0m,
                    "Strong" => 8.0m,
                    "Normal" => 6.0m,
                    "Below Average" => 3.0m,
                    _ => 1.0m
                };
            }
            else
            {
                volPoints = 5.0m; // Neutral default if volume data unavailable
            }
            result.Breakdown.Add(new ScoreBreakdownItem
            {
                Component = "Volume Confirmation",
                MaxPoints = volMax,
                AwardedPoints = volPoints,
                Notes = $"{volume.VolumeClassification} ({volume.VolumeRatio:F2}x average volume)"
            });
            totalPointsAwarded += volPoints;

            // 5. Confirmation Status (Weight: 15)
            decimal confMax = config.WeightConfirmation;
            totalWeightAvailable += confMax;
            decimal confPoints = confirmation.ConfirmationStatus switch
            {
                "CONFIRMED" => 15.0m,
                "INTRABAR_CANDIDATE" => 8.0m,
                "AWAITING_NEXT_CANDLE" => 7.0m,
                "FORMING" => 4.0m,
                _ => 0m
            };
            result.Breakdown.Add(new ScoreBreakdownItem
            {
                Component = "Confirmation Status",
                MaxPoints = confMax,
                AwardedPoints = confPoints,
                Notes = confirmation.ConfirmationStatus
            });
            totalPointsAwarded += confPoints;

            // Normalize Score to 0 - 100 Scale
            decimal normalizedScore = totalWeightAvailable > 0
                ? Math.Round((totalPointsAwarded / totalWeightAvailable) * 100m, 1)
                : 0m;

            result.FinalScore = Math.Clamp(normalizedScore, 0m, 100m);

            // Classification
            if (result.FinalScore >= 85.0m) result.Classification = "Excellent";
            else if (result.FinalScore >= 70.0m) result.Classification = "Strong";
            else if (result.FinalScore >= 55.0m) result.Classification = "Moderate";
            else result.Classification = "Weak";

            result.Summary = $"Inverse Hammer Quality Score: {result.FinalScore}/100 ({result.Classification}). Total points: {totalPointsAwarded:F1}/{totalWeightAvailable:F1}.";

            return result;
        }
    }
}
