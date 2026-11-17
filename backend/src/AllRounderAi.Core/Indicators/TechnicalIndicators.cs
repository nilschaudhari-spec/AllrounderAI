using System;
using System.Collections.Generic;
using System.Linq;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Indicators
{
    public static class TechnicalIndicators
    {
        public static decimal[] CalculateSma(IReadOnlyList<Candle> candles, int period)
        {
            var result = new decimal[candles.Count];
            if (candles.Count < period) return result;

            decimal sum = 0;
            for (int i = 0; i < period; i++)
            {
                sum += candles[i].Close;
            }
            result[period - 1] = sum / period;

            for (int i = period; i < candles.Count; i++)
            {
                sum += candles[i].Close - candles[i - period].Close;
                result[i] = sum / period;
            }

            return result;
        }

        public static decimal[] CalculateEma(IReadOnlyList<Candle> candles, int period)
        {
            var result = new decimal[candles.Count];
            if (candles.Count < period) return result;

            decimal multiplier = 2m / (period + 1m);
            decimal sum = 0;
            for (int i = 0; i < period; i++)
            {
                sum += candles[i].Close;
            }
            decimal ema = sum / period;
            result[period - 1] = ema;

            for (int i = period; i < candles.Count; i++)
            {
                ema = (candles[i].Close - ema) * multiplier + ema;
                result[i] = ema;
            }

            return result;
        }

        public static decimal[] CalculateAtr(IReadOnlyList<Candle> candles, int period = 14)
        {
            var result = new decimal[candles.Count];
            if (candles.Count < 2) return result;

            var tr = new decimal[candles.Count];
            tr[0] = candles[0].High - candles[0].Low;

            for (int i = 1; i < candles.Count; i++)
            {
                decimal hl = candles[i].High - candles[i].Low;
                decimal hc = Math.Abs(candles[i].High - candles[i - 1].Close);
                decimal lc = Math.Abs(candles[i].Low - candles[i - 1].Close);
                tr[i] = Math.Max(hl, Math.Max(hc, lc));
            }

            if (candles.Count < period) return result;

            decimal sum = 0;
            for (int i = 0; i < period; i++)
            {
                sum += tr[i];
            }
            decimal atr = sum / period;
            result[period - 1] = atr;

            for (int i = period; i < candles.Count; i++)
            {
                atr = (atr * (period - 1) + tr[i]) / period;
                result[i] = atr;
            }

            return result;
        }

        public class BollingerBandValues
        {
            public decimal[] Upper { get; set; } = Array.Empty<decimal>();
            public decimal[] Middle { get; set; } = Array.Empty<decimal>();
            public decimal[] Lower { get; set; } = Array.Empty<decimal>();
        }

        public static BollingerBandValues CalculateBollingerBands(IReadOnlyList<Candle> candles, int period = 20, decimal stdDevMultiplier = 2.0m)
        {
            int n = candles.Count;
            var middle = CalculateSma(candles, period);
            var upper = new decimal[n];
            var lower = new decimal[n];

            for (int i = period - 1; i < n; i++)
            {
                decimal m = middle[i];
                decimal sumSquares = 0;
                for (int j = i - period + 1; j <= i; j++)
                {
                    decimal diff = candles[j].Close - m;
                    sumSquares += diff * diff;
                }
                decimal stdDev = (decimal)Math.Sqrt((double)(sumSquares / period));
                upper[i] = m + stdDevMultiplier * stdDev;
                lower[i] = m - stdDevMultiplier * stdDev;
            }

            return new BollingerBandValues
            {
                Upper = upper,
                Middle = middle,
                Lower = lower
            };
        }

        public class MacdValues
        {
            public decimal[] MacdLine { get; set; } = Array.Empty<decimal>();
            public decimal[] SignalLine { get; set; } = Array.Empty<decimal>();
            public decimal[] Histogram { get; set; } = Array.Empty<decimal>();
        }

        public static MacdValues CalculateMacd(IReadOnlyList<Candle> candles, int fastPeriod = 12, int slowPeriod = 26, int signalPeriod = 9)
        {
            int n = candles.Count;
            var emaFast = CalculateEma(candles, fastPeriod);
            var emaSlow = CalculateEma(candles, slowPeriod);

            var macdLine = new decimal[n];
            for (int i = slowPeriod - 1; i < n; i++)
            {
                macdLine[i] = emaFast[i] - emaSlow[i];
            }

            var signalLine = new decimal[n];
            if (n >= slowPeriod - 1 + signalPeriod)
            {
                decimal multiplier = 2m / (signalPeriod + 1m);
                int startIdx = slowPeriod - 1;
                decimal sum = 0;
                for (int i = startIdx; i < startIdx + signalPeriod; i++)
                {
                    sum += macdLine[i];
                }
                decimal sig = sum / signalPeriod;
                signalLine[startIdx + signalPeriod - 1] = sig;

                for (int i = startIdx + signalPeriod; i < n; i++)
                {
                    sig = (macdLine[i] - sig) * multiplier + sig;
                    signalLine[i] = sig;
                }
            }

            var histogram = new decimal[n];
            for (int i = 0; i < n; i++)
            {
                histogram[i] = macdLine[i] - signalLine[i];
            }

            return new MacdValues
            {
                MacdLine = macdLine,
                SignalLine = signalLine,
                Histogram = histogram
            };
        }

        public class SupertrendValues
        {
            public decimal[] Line { get; set; } = Array.Empty<decimal>();
            public bool[] IsBullish { get; set; } = Array.Empty<bool>(); // true = Green/Uptrend, false = Red/Downtrend
        }

        public static SupertrendValues CalculateSupertrend(IReadOnlyList<Candle> candles, int period = 20, decimal multiplier = 2.0m)
        {
            int n = candles.Count;
            var line = new decimal[n];
            var isBullish = new bool[n];
            if (n < period) return new SupertrendValues { Line = line, IsBullish = isBullish };

            var atr = CalculateAtr(candles, period);
            var basicUpper = new decimal[n];
            var basicLower = new decimal[n];
            var finalUpper = new decimal[n];
            var finalLower = new decimal[n];

            for (int i = 0; i < n; i++)
            {
                decimal hl2 = (candles[i].High + candles[i].Low) / 2m;
                basicUpper[i] = hl2 + multiplier * atr[i];
                basicLower[i] = hl2 - multiplier * atr[i];
            }

            finalUpper[period - 1] = basicUpper[period - 1];
            finalLower[period - 1] = basicLower[period - 1];
            bool trend = true; // default bullish

            for (int i = period; i < n; i++)
            {
                // Final upper band
                if (basicUpper[i] < finalUpper[i - 1] || candles[i - 1].Close > finalUpper[i - 1])
                    finalUpper[i] = basicUpper[i];
                else
                    finalUpper[i] = finalUpper[i - 1];

                // Final lower band
                if (basicLower[i] > finalLower[i - 1] || candles[i - 1].Close < finalLower[i - 1])
                    finalLower[i] = basicLower[i];
                else
                    finalLower[i] = finalLower[i - 1];

                // Trend switch
                if (trend && candles[i].Close < finalLower[i - 1])
                    trend = false;
                else if (!trend && candles[i].Close > finalUpper[i - 1])
                    trend = true;

                isBullish[i] = trend;
                line[i] = trend ? finalLower[i] : finalUpper[i];
            }

            return new SupertrendValues
            {
                Line = line,
                IsBullish = isBullish
            };
        }

        public static decimal[] CalculateVwap(IReadOnlyList<Candle> candles)
        {
            int n = candles.Count;
            var vwap = new decimal[n];
            decimal cumVolPrice = 0;
            long cumVol = 0;

            for (int i = 0; i < n; i++)
            {
                // Reset VWAP at start of each trading day
                if (i > 0 && candles[i].Timestamp.Date != candles[i - 1].Timestamp.Date)
                {
                    cumVolPrice = 0;
                    cumVol = 0;
                }

                decimal typicalPrice = (candles[i].High + candles[i].Low + candles[i].Close) / 3m;
                cumVolPrice += typicalPrice * candles[i].Volume;
                cumVol += candles[i].Volume;

                vwap[i] = cumVol > 0 ? cumVolPrice / cumVol : candles[i].Close;
            }

            return vwap;
        }
    }
}
