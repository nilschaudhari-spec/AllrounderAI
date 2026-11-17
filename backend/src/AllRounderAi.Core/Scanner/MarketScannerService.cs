using System;
using System.Collections.Generic;
using System.Linq;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Strategies;

namespace AllRounderAi.Core.Scanner
{
    public class ScanRequest
    {
        public string MarketScope { get; set; } = "ALL"; // "CASH", "FNO", "INDEX", "ALL"
        public string Timeframe { get; set; } = "5m";
        public List<string> StrategyIds { get; set; } = new();
        public decimal MinQualityScore { get; set; } = 75.0m;
        public string ConfirmationMode { get; set; } = "CANDLE_CLOSE";
    }

    public class ScanResult
    {
        public string ScanId { get; set; } = Guid.NewGuid().ToString();
        public DateTime ExecutedAt { get; set; } = DateTime.UtcNow;
        public int TotalInstrumentsScanned { get; set; }
        public int TotalMatchesFound { get; set; }
        public List<TradingSignal> Signals { get; set; } = new();
    }

    public class MarketScannerService
    {
        private readonly List<IStrategyEngine> _strategies;

        public MarketScannerService()
        {
            _strategies = new List<IStrategyEngine>
            {
                new DailyIntradayStrategy(),
                new LowBreakCloseUpsideStrategy(),
                new HighBreakCloseDownsideStrategy(),
                new GoldenCrossStrategy(),
                new BollingerBandsStrategy(),
                new HammerStrategy(),
                new InverseHammerStrategy(),
                new BullishHaramiStrategy(),
                new HangingManStrategy(),
                new ShootingStarStrategy(),
                new BearishHaramiStrategy(),
                new MarubozuStrategy(),
                new MorningStarStrategy(),
                new EveningStarStrategy()
            };
        }

        public ScanResult ExecuteScan(
            IReadOnlyList<Instrument> instruments,
            Dictionary<string, IReadOnlyList<Candle>> candleCache,
            ScanRequest request,
            StrategyConfig defaultConfig)
        {
            var result = new ScanResult();
            var eligibleInstruments = FilterInstrumentsByScope(instruments, request.MarketScope);
            result.TotalInstrumentsScanned = eligibleInstruments.Count;

            var activeStrategies = request.StrategyIds.Count > 0
                ? _strategies.Where(s => request.StrategyIds.Contains(s.StrategyId)).ToList()
                : _strategies;

            foreach (var inst in eligibleInstruments)
            {
                if (!candleCache.TryGetValue(inst.Symbol, out var candles) || candles.Count < 20)
                    continue;

                foreach (var strategy in activeStrategies)
                {
                    var match = strategy.Evaluate(candles, inst, defaultConfig);
                    if (match.IsMatched && match.Signal != null)
                    {
                        if (match.Signal.QualityScore >= request.MinQualityScore)
                        {
                            result.Signals.Add(match.Signal);
                        }
                    }
                }
            }

            // Rank signals by quality score descending
            result.Signals = result.Signals.OrderByDescending(s => s.QualityScore).ToList();
            result.TotalMatchesFound = result.Signals.Count;

            return result;
        }

        private List<Instrument> FilterInstrumentsByScope(IReadOnlyList<Instrument> instruments, string scope)
        {
            return scope.ToUpperInvariant() switch
            {
                "CASH" => instruments.Where(i => i.Segment == ExchangeSegment.Equity).ToList(),
                "FNO" => instruments.Where(i => i.IsFnoEligible || i.Segment == ExchangeSegment.Futures || i.Segment == ExchangeSegment.Options).ToList(),
                "INDEX" => instruments.Where(i => i.Segment == ExchangeSegment.Index).ToList(),
                "COMMODITY" => instruments.Where(i => i.Exchange == "MCX").ToList(),
                _ => instruments.ToList()
            };
        }
    }
}
