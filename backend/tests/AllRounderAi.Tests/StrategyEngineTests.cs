using System;
using System.Collections.Generic;
using Xunit;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Strategies;

namespace AllRounderAi.Tests
{
    public class StrategyEngineTests
    {
        [Fact]
        public void HammerStrategy_Should_Match_On_Valid_Hammer_In_Downtrend()
        {
            var strategy = new HammerStrategy();
            var inst = new Instrument { InstrumentId = "1", Symbol = "NIFTY", TradingSymbol = "NIFTY 50", TickSize = 0.05m };
            var config = new StrategyConfig();

            var candles = new List<Candle>();
            for (int i = 0; i < 5; i++)
            {
                candles.Add(new Candle
                {
                    Timestamp = DateTime.UtcNow.AddMinutes(5 * i),
                    Open = 24000m - (i * 20m),
                    High = 24005m - (i * 20m),
                    Low = 23970m - (i * 20m),
                    Close = 23980m - (i * 20m)
                });
            }

            candles.Add(new Candle
            {
                Timestamp = DateTime.UtcNow.AddMinutes(25),
                Open = 23900m,
                High = 23912m,
                Low = 23860m, // Lower wick = 40
                Close = 23910m // Body = 10, Upper wick = 2
            });

            var result = strategy.Evaluate(candles, inst, config);

            Assert.True(result.IsMatched);
            Assert.NotNull(result.Signal);
            Assert.Equal(SignalDirection.Buy, result.Signal.SignalType);
            Assert.Equal("HAMMER", result.Signal.StrategyId);
        }

        [Fact]
        public void LowBreakCloseUpside_Should_Enforce_7_To_10_Point_Rule_From_PPT_Slide_13()
        {
            var strategy = new LowBreakCloseUpsideStrategy();
            var inst = new Instrument { InstrumentId = "1", Symbol = "NIFTY", Segment = ExchangeSegment.Index };
            var config = new StrategyConfig
            {
                MinCandleBodyPoints = 7.0m,
                MaxCandleBodyPoints = 10.0m
            };

            var candles = new List<Candle>
            {
                new()
                {
                    Timestamp = DateTime.UtcNow.AddMinutes(-10),
                    Open = 24050m,
                    High = 24055m,
                    Low = 24000m,
                    Close = 24010m // Red
                },
                new()
                {
                    Timestamp = DateTime.UtcNow.AddMinutes(-5),
                    Open = 24016.5m,
                    High = 24030m,
                    Low = 23990m, // Low break!
                    Close = 24025m // Close upside! Body = 8.5 pts
                }
            };

            var result = strategy.Evaluate(candles, inst, config);

            Assert.True(result.IsMatched);
            Assert.NotNull(result.Signal);
            Assert.Equal(SignalDirection.Buy, result.Signal.SignalType);
            Assert.Equal("LOW_BREAK_CLOSE_UPSIDE", result.Signal.StrategyId);
        }

        [Fact]
        public void HighBreakCloseDownside_Should_Match_When_High_Broken_And_Closed_Downside_Slide_21()
        {
            var strategy = new HighBreakCloseDownsideStrategy();
            var inst = new Instrument { InstrumentId = "1", Symbol = "NIFTY", Segment = ExchangeSegment.Index };
            var config = new StrategyConfig
            {
                MinCandleBodyPoints = 7.0m,
                MaxCandleBodyPoints = 10.0m
            };

            var candles = new List<Candle>
            {
                new()
                {
                    Timestamp = DateTime.UtcNow.AddMinutes(-10),
                    Open = 24000m,
                    High = 24040m,
                    Low = 23995m,
                    Close = 24035m // Green
                },
                new()
                {
                    Timestamp = DateTime.UtcNow.AddMinutes(-5),
                    Open = 24006m,
                    High = 24045m, // High break!
                    Low = 23985m,
                    Close = 23998m // Close downside! Body = 8.0 pts
                }
            };

            var result = strategy.Evaluate(candles, inst, config);

            Assert.True(result.IsMatched);
            Assert.NotNull(result.Signal);
            Assert.Equal(SignalDirection.Sell, result.Signal.SignalType);
            Assert.Equal("HIGH_BREAK_CLOSE_DOWNSIDE", result.Signal.StrategyId);
        }

        [Fact]
        public void MarubozuStrategy_Should_Identify_Bullish_And_Bearish_Marubozu_Slide_25_26()
        {
            var strategy = new MarubozuStrategy();
            var inst = new Instrument { InstrumentId = "1", Symbol = "RELIANCE" };
            var config = new StrategyConfig();

            // Bullish Marubozu (Range = 20, Body = 19.5, Upper wick = 0.3, Lower wick = 0.2)
            var candlesBullish = new List<Candle>
            {
                new()
                {
                    Timestamp = DateTime.UtcNow,
                    Open = 1280.2m,
                    High = 1300.0m,
                    Low = 1280.0m,
                    Close = 1299.7m
                }
            };

            var resultBullish = strategy.Evaluate(candlesBullish, inst, config);
            Assert.True(resultBullish.IsMatched);
            Assert.Equal(SignalDirection.Buy, resultBullish.Signal!.SignalType);
            Assert.Equal("BULLISH_MARUBOZU", resultBullish.Signal.StrategyId);

            // Bearish Marubozu
            var candlesBearish = new List<Candle>
            {
                new()
                {
                    Timestamp = DateTime.UtcNow,
                    Open = 1299.7m,
                    High = 1300.0m,
                    Low = 1280.0m,
                    Close = 1280.3m
                }
            };

            var resultBearish = strategy.Evaluate(candlesBearish, inst, config);
            Assert.True(resultBearish.IsMatched);
            Assert.Equal(SignalDirection.Sell, resultBearish.Signal!.SignalType);
            Assert.Equal("BEARISH_MARUBOZU", resultBearish.Signal.StrategyId);
        }

        [Fact]
        public void ProtectivePut_Should_Calculate_Limited_MaxLoss_And_Breakeven_Slide_68()
        {
            decimal futurePrice = 51240m;
            decimal atmStrike = 51200m;
            decimal putPremium = 200m;
            int lotSize = 15;

            var strategy = OptionsStrategyEngine.BuildProtectivePut("BANKNIFTY", futurePrice, atmStrike, putPremium, lotSize);

            Assert.Equal(3600m, strategy.MaxLoss);
            Assert.Equal(51440m, strategy.BreakevenPrice);
            Assert.Equal("Slide 68", strategy.PptSlideReference);
            Assert.NotEmpty(strategy.PayoffCurve);
        }

        [Fact]
        public void ProtectiveCall_Should_Calculate_Limited_MaxLoss_And_Breakeven_Slide_69()
        {
            decimal futurePrice = 51240m;
            decimal atmStrike = 51200m;
            decimal callPremium = 220m;
            int lotSize = 15;

            var strategy = OptionsStrategyEngine.BuildProtectiveCall("BANKNIFTY", futurePrice, atmStrike, callPremium, lotSize);

            // Max Loss = ((51200 - 51240) + 220) * 15 = (-40 + 220) * 15 = 180 * 15 = 2700
            Assert.Equal(2700m, strategy.MaxLoss);
            // Breakeven = 51240 - 220 = 51020
            Assert.Equal(51020m, strategy.BreakevenPrice);
            Assert.Equal("Slide 69", strategy.PptSlideReference);
            Assert.NotEmpty(strategy.PayoffCurve);
        }

        [Fact]
        public void DailyIntradayStrategy_Should_Enforce_1_To_1_Target_And_StopLoss_Slide_62()
        {
            var strategy = new DailyIntradayStrategy();
            var inst = new Instrument { InstrumentId = "1", Symbol = "NIFTY 50", Segment = ExchangeSegment.Index };
            var config = new StrategyConfig { Timeframe = "5m", RiskRewardRatio = 1.0m };

            var candles = new List<Candle>();
            for (int i = 0; i < 35; i++)
            {
                candles.Add(new Candle
                {
                    Timestamp = DateTime.UtcNow.AddMinutes(5 * i),
                    Open = 24000m + (i * 5m),
                    High = 24010m + (i * 5m),
                    Low = 23995m + (i * 5m),
                    Close = 24008m + (i * 5m),
                    Volume = 50000
                });
            }

            var result = strategy.Evaluate(candles, inst, config);

            if (result.IsMatched && result.Signal != null)
            {
                decimal targetDiff = Math.Abs(result.Signal.TargetPrice - result.Signal.EntryPrice);
                decimal slDiff = Math.Abs(result.Signal.EntryPrice - result.Signal.StopLossPrice);
                Assert.Equal(targetDiff, slDiff);
                Assert.Equal(1.0m, result.Signal.RiskRewardRatio);
            }
        }
    }
}
