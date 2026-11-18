using System;
using System.Collections.Generic;

namespace AllRounderAi.Core.Strategies.Hammer
{
    public class HammerPatternConfig
    {
        public string ConfigId { get; set; } = "GLOBAL_DEFAULT";
        public string Level { get; set; } = "GLOBAL"; // GLOBAL, STRATEGY, TIMEFRAME, INSTRUMENT
        public string? TargetInstrumentId { get; set; }
        public string? TargetTimeframe { get; set; }
        public int Version { get; set; } = 1;
        public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

        // Core Parameters
        public bool PatternEnabled { get; set; } = true;
        public decimal MinLowerWickToBodyRatio { get; set; } = 2.0m;
        public decimal MaxUpperWickToRangeRatio { get; set; } = 0.10m;
        public decimal MaxBodyToRangeRatio { get; set; } = 0.35m;
        public decimal MinBodyPosition { get; set; } = 0.60m;
        public decimal MinCandleRangeToAtrRatio { get; set; } = 0.50m;
        public int AtrPeriod { get; set; } = 14;

        // Trend Settings
        public int TrendLookback { get; set; } = 5;
        public bool RequireDowntrend { get; set; } = true;
        public string TrendMode { get; set; } = "Moderate"; // Strict, Moderate, Relaxed, Custom
        public bool UseEmaTrend { get; set; } = true;
        public bool UseSupertrend { get; set; } = false;

        // Support Settings
        public bool RequireSupportProximity { get; set; } = false;
        public decimal MaxSupportDistanceAtr { get; set; } = 1.0m;

        // Volume Settings
        public bool RequireVolumeConfirmation { get; set; } = false;
        public string VolumeRequirementMode { get; set; } = "Advisory"; // Disabled, Advisory, Mandatory
        public decimal MinVolumeRatio { get; set; } = 1.50m;
        public int VolumeAveragePeriod { get; set; } = 20;

        // Confirmation Modes
        public string ConfirmationMode { get; set; } = "CandleClose"; // CandleClose, Intrabar
        public bool RequireNextCandleConfirmation { get; set; } = false;
        public string NextCandleConfirmationMethod { get; set; } = "CloseAboveHammerHigh"; // CloseAboveHammerHigh, CloseAboveBodyHigh, BreakHighIntrabar, BullishBody

        // Scoring & Risk
        public decimal ScoreThreshold { get; set; } = 70.0m;
        public decimal RiskRewardRatio { get; set; } = 1.0m;
        public decimal StopLossBufferAtr { get; set; } = 0.10m;
        public string TargetMethod { get; set; } = "FixedRiskReward"; // FixedRiskReward, NextResistance, TrailingAtr
        public decimal MaxRiskPerTradePercent { get; set; } = 2.0m;

        // Scoring Weights
        public decimal WeightGeometry { get; set; } = 30.0m;
        public decimal WeightTrend { get; set; } = 20.0m;
        public decimal WeightSupport { get; set; } = 15.0m;
        public decimal WeightVolume { get; set; } = 10.0m;
        public decimal WeightConfirmation { get; set; } = 15.0m;
        public decimal WeightHigherTimeframe { get; set; } = 10.0m;

        public List<string> Timeframes { get; set; } = new() { "1m", "3m", "5m", "15m", "30m", "1h", "D" };

        public static HammerPatternConfig CreateDefault() => new();

        public static HammerPatternConfig CreatePreset(string presetName)
        {
            return presetName.ToUpperInvariant() switch
            {
                "STRICT" => new HammerPatternConfig
                {
                    ConfigId = "PRESET_STRICT",
                    MinLowerWickToBodyRatio = 2.5m,
                    MaxUpperWickToRangeRatio = 0.05m,
                    MaxBodyToRangeRatio = 0.30m,
                    MinBodyPosition = 0.65m,
                    MinCandleRangeToAtrRatio = 0.60m,
                    RequireDowntrend = true,
                    TrendMode = "Strict",
                    RequireSupportProximity = true,
                    MaxSupportDistanceAtr = 0.75m,
                    RequireVolumeConfirmation = true,
                    VolumeRequirementMode = "Mandatory",
                    MinVolumeRatio = 1.60m,
                    RequireNextCandleConfirmation = true,
                    NextCandleConfirmationMethod = "CloseAboveHammerHigh",
                    ScoreThreshold = 85.0m
                },
                "EARLY_ENTRY" => new HammerPatternConfig
                {
                    ConfigId = "PRESET_EARLY_ENTRY",
                    MinLowerWickToBodyRatio = 1.8m,
                    MaxUpperWickToRangeRatio = 0.15m,
                    MaxBodyToRangeRatio = 0.40m,
                    MinBodyPosition = 0.55m,
                    MinCandleRangeToAtrRatio = 0.40m,
                    ConfirmationMode = "Intrabar",
                    RequireDowntrend = true,
                    TrendMode = "Relaxed",
                    RequireSupportProximity = false,
                    RequireVolumeConfirmation = false,
                    VolumeRequirementMode = "Advisory",
                    RequireNextCandleConfirmation = false,
                    ScoreThreshold = 55.0m
                },
                _ => CreateDefault() // "BALANCED"
            };
        }

        public HammerPatternConfig Clone() => (HammerPatternConfig)MemberwiseClone();
    }
}
