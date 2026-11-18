using System;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies.Hammer
{
    public class HammerCandleGeometry
    {
        public bool IsValid { get; set; }
        public string ValidationError { get; set; } = string.Empty;

        public decimal Range { get; set; }
        public decimal Body { get; set; }
        public decimal LowerWick { get; set; }
        public decimal UpperWick { get; set; }

        public decimal BodyToRangeRatio { get; set; }
        public decimal LowerWickToBodyRatio { get; set; }
        public decimal UpperWickToRangeRatio { get; set; }
        public decimal BodyPosition { get; set; }
        public decimal RangeToAtrRatio { get; set; }

        public bool IsBullishColor { get; set; } // Close >= Open
        public bool IsZeroBodyDoji { get; set; }

        public static HammerCandleGeometry Calculate(Candle candle, decimal atr)
        {
            var geo = new HammerCandleGeometry();

            // 1. Data Integrity and Sanity Checks
            if (candle == null)
            {
                geo.IsValid = false;
                geo.ValidationError = "Candle is null.";
                return geo;
            }

            if (candle.Open <= 0 || candle.High <= 0 || candle.Low <= 0 || candle.Close <= 0)
            {
                geo.IsValid = false;
                geo.ValidationError = "Invalid non-positive price detected.";
                return geo;
            }

            if (candle.High < candle.Low || candle.High < candle.Open || candle.High < candle.Close ||
                candle.Low > candle.Open || candle.Low > candle.Close)
            {
                geo.IsValid = false;
                geo.ValidationError = "Corrupt OHLC data: High is not highest or Low is not lowest.";
                return geo;
            }

            decimal range = candle.High - candle.Low;
            if (range <= 0.00001m)
            {
                geo.IsValid = false;
                geo.ValidationError = "Zero candle range: High equals Low.";
                return geo;
            }

            decimal body = Math.Abs(candle.Close - candle.Open);
            decimal openCloseMax = Math.Max(candle.Open, candle.Close);
            decimal openCloseMin = Math.Min(candle.Open, candle.Close);

            decimal lowerWick = openCloseMin - candle.Low;
            decimal upperWick = candle.High - openCloseMax;

            // Clamp small negative floating rounding artifacts
            if (lowerWick < 0) lowerWick = 0;
            if (upperWick < 0) upperWick = 0;

            geo.Range = Math.Round(range, 4);
            geo.Body = Math.Round(body, 4);
            geo.LowerWick = Math.Round(lowerWick, 4);
            geo.UpperWick = Math.Round(upperWick, 4);
            geo.IsBullishColor = candle.Close >= candle.Open;

            // 2. Documented Zero-Body Policy:
            // A candle with zero body (Open == Close) is a Dragonfly Doji or pure Doji.
            // Under institutional Hammer rules, an authentic Hammer requires an identifiable real body
            // situated in the upper portion of the range.
            // Therefore, if Body == 0, LowerWickToBodyRatio cannot be computed as infinity;
            // it is safely set to 0 and flagged as IsZeroBodyDoji so it does not falsely qualify as a Hammer.
            if (body <= 0.0001m)
            {
                geo.IsZeroBodyDoji = true;
                geo.BodyToRangeRatio = 0m;
                geo.LowerWickToBodyRatio = 0m;
                geo.BodyPosition = Math.Round((openCloseMax - candle.Low) / range, 4);
                geo.UpperWickToRangeRatio = Math.Round(upperWick / range, 4);
                geo.RangeToAtrRatio = atr > 0 ? Math.Round(range / atr, 4) : 1.0m;
                geo.IsValid = true;
                return geo;
            }

            // Normal Geometry Ratios
            geo.BodyToRangeRatio = Math.Round(body / range, 4);
            geo.LowerWickToBodyRatio = Math.Round(lowerWick / body, 4);
            geo.UpperWickToRangeRatio = Math.Round(upperWick / range, 4);
            geo.BodyPosition = Math.Round((openCloseMax - candle.Low) / range, 4);

            if (atr > 0.0001m)
            {
                geo.RangeToAtrRatio = Math.Round(range / atr, 4);
            }
            else
            {
                geo.RangeToAtrRatio = 1.0m; // Default safe neutral ratio if ATR is 0
            }

            geo.IsValid = true;
            return geo;
        }
    }
}
