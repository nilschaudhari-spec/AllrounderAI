using System;
using System.Collections.Generic;
using Xunit;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Strategies.Hammer;

namespace AllRounderAi.Tests
{
    public class HammerStrategyTests
    {
        private readonly Instrument _testInstrument = new()
        {
            InstrumentId = "test-eq-01",
            Symbol = "RELIANCE",
            TradingSymbol = "RELIANCE",
            Exchange = "NSE",
            Segment = ExchangeSegment.Equity,
            LotSize = 1,
            TickSize = 0.05m,
            LastPrice = 1360.0m
        };

        [Fact]
        public void IdealGreenHammer_IdentifiedAccurately()
        {
            var candles = CreateDowntrendCandles(5);
            // Ideal green hammer: Open=1350, Close=1354, High=1355 (0.25 upper wick), Low=1338 (12pt lower wick, 3x body)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1354.5m,
                Low = 1338m,
                Close = 1354m,
                Volume = 500000
            });

            var config = HammerPatternConfig.CreateDefault();
            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config);

            Assert.Equal("BUY", result.PatternStatus);
            Assert.True(result.Geometry.IsValid);
            Assert.True(result.Geometry.IsBullishColor);
            Assert.True(result.Geometry.LowerWickToBodyRatio >= 2.0m);
            Assert.True(result.Score.FinalScore >= 70.0m);
            Assert.NotNull(result.GeneratedSignal);
            Assert.Equal(1.0m, result.GeneratedSignal.RiskRewardRatio);
        }

        [Fact]
        public void IdealRedHammer_IdentifiedAccurately()
        {
            var candles = CreateDowntrendCandles(5);
            // Ideal red hammer: Open=1354, Close=1350, High=1354.5, Low=1338 (lower wick = 12pts, 3x body)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1354m,
                High = 1354.5m,
                Low = 1338m,
                Close = 1350m,
                Volume = 500000
            });

            var config = HammerPatternConfig.CreateDefault();
            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config);

            Assert.Equal("BUY", result.PatternStatus);
            Assert.True(result.Geometry.IsValid);
            Assert.False(result.Geometry.IsBullishColor);
            Assert.True(result.Geometry.LowerWickToBodyRatio >= 2.0m);
            Assert.NotNull(result.GeneratedSignal);
        }

        [Fact]
        public void ShortLowerWick_IsRejected()
        {
            var candles = CreateDowntrendCandles(5);
            // Short lower wick: Open=1350, Close=1354 (body 4), Low=1346 (lower wick only 4, ratio 1.0x < 2.0x required)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1354.5m,
                Low = 1346m,
                Close = 1354m,
                Volume = 100000
            });

            var config = HammerPatternConfig.CreateDefault();
            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config);

            Assert.Equal("NO_TRADE", result.PatternStatus);
            Assert.Null(result.GeneratedSignal);
        }

        [Fact]
        public void LargeUpperWick_IsRejected()
        {
            var candles = CreateDowntrendCandles(5);
            // Large upper wick: High=1365 (upper wick = 11 pts, far above 10% of range)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1365m,
                Low = 1338m,
                Close = 1354m,
                Volume = 100000
            });

            var config = HammerPatternConfig.CreateDefault();
            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config);

            Assert.Equal("NO_TRADE", result.PatternStatus);
        }

        [Fact]
        public void ZeroBodyCandle_HandledSafelyByPolicy_AndRejectedAsHammer()
        {
            var candles = CreateDowntrendCandles(5);
            // Pure Dragonfly Doji (Open == Close)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1350m,
                Low = 1335m,
                Close = 1350m,
                Volume = 100000
            });

            var geo = HammerCandleGeometry.Calculate(candles[^1], 10m);

            Assert.True(geo.IsValid);
            Assert.True(geo.IsZeroBodyDoji);
            Assert.Equal(0m, geo.LowerWickToBodyRatio); // Never infinity!

            var config = HammerPatternConfig.CreateDefault();
            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config);

            Assert.Equal("INVALIDATED", result.PatternStatus);
            Assert.Contains("Zero-body Doji rejected", result.ExplainabilityBulletPoints[0]);
        }

        [Fact]
        public void ZeroRangeCandle_HandledSafely_InvalidGeometry()
        {
            var candle = new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1350m,
                Low = 1350m,
                Close = 1350m
            };

            var geo = HammerCandleGeometry.Calculate(candle, 10m);
            Assert.False(geo.IsValid);
            Assert.Contains("Zero candle range", geo.ValidationError);
        }

        [Fact]
        public void HammerGeometryInUptrend_ClassifiedAs_HammerGeometryOnly()
        {
            // Uptrend candles
            var candles = new List<Candle>();
            decimal p = 1300m;
            for (int i = 0; i < 6; i++)
            {
                candles.Add(new Candle
                {
                    Timestamp = DateTime.UtcNow.AddMinutes(-5 * (6 - i)),
                    Open = p,
                    High = p + 8m,
                    Low = p - 2m,
                    Close = p + 6m,
                    Volume = 100000
                });
                p += 6m;
            }

            // Add valid hammer geometry at the top of the rally
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = p,
                High = p + 4.5m, // High must be >= Close
                Low = p - 12m,
                Close = p + 4m,
                Volume = 300000
            });

            var config = HammerPatternConfig.CreateDefault();
            config.RequireDowntrend = true;

            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config);

            Assert.Equal("HAMMER_GEOMETRY_ONLY", result.PatternStatus);
            Assert.False(result.Trend.IsDowntrendConfirmed);
        }

        [Fact]
        public void MandatoryVolumeRequirement_BlocksSignal_WhenVolumeIsLow()
        {
            var candles = CreateDowntrendCandles(5);
            // Low volume hammer
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1354.5m,
                Low = 1338m,
                Close = 1354m,
                Volume = 20000 // Average is ~200,000, so 0.1x ratio
            });

            var config = HammerPatternConfig.CreateDefault();
            config.RequireVolumeConfirmation = true;
            config.VolumeRequirementMode = "Mandatory";
            config.MinVolumeRatio = 1.50m;

            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config);

            Assert.Equal("NO_TRADE", result.PatternStatus);
            Assert.False(result.Volume.IsConfirmed);
        }

        [Fact]
        public void IntrabarMode_SetsStatusTo_HammerCandidate()
        {
            var candles = CreateDowntrendCandles(5);
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1354.5m,
                Low = 1338m,
                Close = 1354m,
                Volume = 400000
            });

            var config = HammerPatternConfig.CreateDefault();
            config.ConfirmationMode = "Intrabar";

            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config, isDevelopingCandle: true);

            Assert.Equal("HAMMER_CANDIDATE", result.PatternStatus);
            Assert.Equal("INTRABAR_CANDIDATE", result.Confirmation.ConfirmationStatus);
        }

        [Fact]
        public void StrictPreset_RequiresNextCandleConfirmation()
        {
            var candles = CreateDowntrendCandles(5);
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1354.5m,
                Low = 1338m,
                Close = 1354m,
                Volume = 500000
            });

            var config = HammerPatternConfig.CreatePreset("STRICT");
            config.RequireSupportProximity = false; // Isolate next candle confirmation rule test
            var result = HammerSignalEngine.AnalyzeHammer(candles, _testInstrument, config);

            // Awaiting next candle confirmation
            Assert.Equal("HAMMER_DETECTED", result.PatternStatus);
            Assert.Equal("AWAITING_NEXT_CANDLE", result.Confirmation.ConfirmationStatus);
        }

        private static List<Candle> CreateDowntrendCandles(int count)
        {
            var list = new List<Candle>();
            decimal p = 1400m;
            for (int i = 0; i < count; i++)
            {
                decimal open = p;
                decimal close = p - 6m;
                decimal high = open + 2m;
                decimal low = close - 2m;

                list.Add(new Candle
                {
                    Timestamp = DateTime.UtcNow.AddMinutes(-5 * (count - i)),
                    Open = open,
                    High = high,
                    Low = low,
                    Close = close,
                    Volume = 200000
                });
                p = close;
            }
            return list;
        }
    }
}
