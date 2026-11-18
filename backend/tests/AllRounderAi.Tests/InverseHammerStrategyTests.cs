using System;
using System.Collections.Generic;
using Xunit;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Strategies.InverseHammer;

namespace AllRounderAi.Tests
{
    public class InverseHammerStrategyTests
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
        public void IdealGreenInverseHammer_IdentifiedAccurately()
        {
            var candles = CreateDowntrendCandles(5);
            // Ideal green inverse hammer:
            // Low = 1338, Open = 1339, Close = 1342 (Body = 3, Bullish Green)
            // Lower wick = 1339 - 1338 = 1.0 (1.0 / 14.0 = 7.1% <= 10%)
            // High = 1352 (Upper wick = 1352 - 1342 = 10, ratio = 10/3 = 3.33x >= 2.0x)
            // Body position = (1342 - 1338) / 14 = 4 / 14 = 0.286 <= 0.40
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1339m,
                High = 1352m,
                Low = 1338m,
                Close = 1342m,
                Volume = 500000
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config);

            Assert.Equal("BUY", result.PatternStatus);
            Assert.True(result.Geometry.IsValid);
            Assert.True(result.Geometry.IsBullishColor);
            Assert.True(result.Geometry.UpperWickToBodyRatio >= 2.0m);
            Assert.True(result.Geometry.BodyPosition <= 0.40m);
            Assert.True(result.Score.FinalScore >= 70.0m);
            Assert.NotNull(result.GeneratedSignal);
            Assert.Equal(1.0m, result.GeneratedSignal.RiskRewardRatio);
        }

        [Fact]
        public void IdealRedInverseHammer_IdentifiedAccurately()
        {
            var candles = CreateDowntrendCandles(5);
            // Ideal red inverse hammer:
            // Low = 1338, Close = 1339, Open = 1342 (Body = 3, Bearish Red)
            // Lower wick = 1339 - 1338 = 1.0
            // High = 1352 (Upper wick = 1352 - 1342 = 10, ratio = 3.33x >= 2.0x)
            // Body position = (1342 - 1338) / 14 = 0.286 <= 0.40
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1342m,
                High = 1352m,
                Low = 1338m,
                Close = 1339m,
                Volume = 500000
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config);

            Assert.Equal("BUY", result.PatternStatus);
            Assert.True(result.Geometry.IsValid);
            Assert.False(result.Geometry.IsBullishColor);
            Assert.True(result.Geometry.UpperWickToBodyRatio >= 2.0m);
            Assert.NotNull(result.GeneratedSignal);
        }

        [Fact]
        public void ShortUpperWick_IsRejected()
        {
            var candles = CreateDowntrendCandles(5);
            // Short upper wick: Open = 1339, Close = 1342 (body 3), High = 1345 (upper wick only 3, ratio 1.0x < 2.0x)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1339m,
                High = 1345m,
                Low = 1338m,
                Close = 1342m,
                Volume = 500000
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config);

            Assert.NotEqual("BUY", result.PatternStatus);
            Assert.False(result.Geometry.UpperWickToBodyRatio >= config.MinUpperWickToBodyRatio);
        }

        [Fact]
        public void LongLowerWick_IsRejected()
        {
            var candles = CreateDowntrendCandles(5);
            // Long lower wick: Open = 1344, Close = 1347, High = 1357, Low = 1330 (lower wick = 14, range = 27, 14/27 = 51% > 10% max)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1344m,
                High = 1357m,
                Low = 1330m,
                Close = 1347m,
                Volume = 500000
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config);

            Assert.NotEqual("BUY", result.PatternStatus);
            Assert.True(result.Geometry.LowerWickToRangeRatio > config.MaxLowerWickToRangeRatio);
        }

        [Fact]
        public void BodyNearCandleHigh_IsRejected()
        {
            var candles = CreateDowntrendCandles(5);
            // Candle with body placed near high (e.g. Hammer instead of Inverse Hammer):
            // Low = 1330, High = 1350, Open = 1345, Close = 1348 (Body position = 18/20 = 90% > 40%)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1345m,
                High = 1350m,
                Low = 1330m,
                Close = 1348m,
                Volume = 500000
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config);

            Assert.NotEqual("BUY", result.PatternStatus);
            Assert.True(result.Geometry.BodyPosition > config.MaxBodyPosition);
        }

        [Fact]
        public void ZeroBodyCandle_RejectedAsDoji_NoDivideByZero()
        {
            var candles = CreateDowntrendCandles(5);
            // Flat candle: Open == Close (Gravestone Doji)
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1339m,
                High = 1352m,
                Low = 1338m,
                Close = 1339m,
                Volume = 500000
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config);

            Assert.True(result.Geometry.IsZeroBodyDoji);
            Assert.Equal("INVALIDATED", result.PatternStatus);
            Assert.Null(result.GeneratedSignal);
        }

        [Fact]
        public void ZeroRangeCandle_HandledGracefully()
        {
            var candle = new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1350m,
                Low = 1350m,
                Close = 1350m,
                Volume = 0
            };

            var geo = InverseHammerCandleGeometry.Calculate(candle, 10m);
            Assert.False(geo.IsValid);
            Assert.Contains("Zero candle range", geo.ValidationError);
        }

        [Fact]
        public void CorruptOHLC_IsRejected()
        {
            var candle = new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1350m,
                High = 1340m, // High is less than Open!
                Low = 1330m,
                Close = 1345m,
                Volume = 1000
            };

            var geo = InverseHammerCandleGeometry.Calculate(candle, 10m);
            Assert.False(geo.IsValid);
            Assert.Contains("Corrupt OHLC", geo.ValidationError);
        }

        [Fact]
        public void InverseHammerGeometryInUptrend_ClassifiedAs_GeometryOnly()
        {
            // Uptrend where candles rise
            var candles = new List<Candle>();
            var now = DateTime.UtcNow;
            decimal p = 1300m;
            for (int i = 0; i < 5; i++)
            {
                p += 10m;
                candles.Add(new Candle
                {
                    Timestamp = now.AddMinutes(-(5 - i) * 5),
                    Open = p - 2m,
                    High = p + 4.5m,
                    Low = p - 3m,
                    Close = p + 4m,
                    Volume = 200000
                });
            }

            // Add valid inverse hammer geometry at peak of uptrend
            candles.Add(new Candle
            {
                Timestamp = now,
                Open = p + 1m,
                High = p + 15m,
                Low = p,
                Close = p + 3m,
                Volume = 500000
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config);

            Assert.Equal("INVERSE_HAMMER_GEOMETRY_ONLY", result.PatternStatus);
            Assert.Null(result.GeneratedSignal);
        }

        [Fact]
        public void MandatoryVolumeConfirmation_FiltersLowVolume()
        {
            var candles = CreateDowntrendCandles(5);
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1339m,
                High = 1352m,
                Low = 1338m,
                Close = 1342m,
                Volume = 50000 // Very low volume relative to prior 200k
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            config.RequireVolumeConfirmation = true;
            config.VolumeRequirementMode = "Mandatory";
            config.MinVolumeRatio = 1.50m;

            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config);

            Assert.Equal("WATCH", result.PatternStatus);
            Assert.False(result.Volume.IsConfirmed);
        }

        [Fact]
        public void IntrabarCandidateMode_FlagsRealtimeCandle()
        {
            var candles = CreateDowntrendCandles(5);
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1339m,
                High = 1352m,
                Low = 1338m,
                Close = 1342m,
                Volume = 500000
            });

            var config = InverseHammerPatternConfig.CreateDefault();
            config.ConfirmationMode = "Intrabar";

            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config, isDevelopingCandle: true);

            Assert.Equal("INVERSE_HAMMER_CANDIDATE", result.PatternStatus);
            Assert.Equal("INTRABAR_CANDIDATE", result.Confirmation.ConfirmationStatus);
        }

        [Fact]
        public void StrictPreset_RequiresNextCandleConfirmation()
        {
            var candles = CreateDowntrendCandles(5);
            // Inverse Hammer candle
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow.AddMinutes(-5),
                Open = 1339m,
                High = 1352m,
                Low = 1338m,
                Close = 1342m,
                Volume = 500000
            });

            // Next candle closing below High (1352) -> Fails confirmation
            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow,
                Open = 1343m,
                High = 1348m,
                Low = 1341m,
                Close = 1346m,
                Volume = 400000
            });

            var config = InverseHammerPatternConfig.CreatePreset("STRICT");
            config.RequireSupportProximity = false;
            var result = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, _testInstrument, config, targetIndex: candles.Count - 2);

            Assert.Equal("INVALIDATED", result.PatternStatus);
            Assert.False(result.Confirmation.IsConfirmed);
        }

        private static List<Candle> CreateDowntrendCandles(int count)
        {
            var list = new List<Candle>();
            var now = DateTime.UtcNow;
            decimal p = 1400m;

            for (int i = 0; i < count; i++)
            {
                p -= 10m;
                list.Add(new Candle
                {
                    Timestamp = now.AddMinutes(-(count - i) * 5),
                    Open = p + 4m,
                    High = p + 5m,
                    Low = p - 4m,
                    Close = p - 3m,
                    Volume = 200000
                });
            }

            return list;
        }
    }
}
