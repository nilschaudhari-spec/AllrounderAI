using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using Microsoft.AspNetCore.Mvc;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Services;
using AllRounderAi.Core.Strategies;
using AllRounderAi.Core.Strategies.InverseHammer;

namespace AllRounderAi.WebApi.Controllers
{
    [ApiController]
    [Route("api/strategies/inverse-hammer")]
    [Route("api/v1/strategies/inverse-hammer")]
    public class InverseHammerStrategyController : ControllerBase
    {
        private static InverseHammerPatternConfig CurrentConfig = InverseHammerPatternConfig.CreateDefault();
        private static readonly ConcurrentDictionary<string, InverseHammerDetectionDetail> RecentDetections = new();
        private static readonly ConcurrentDictionary<string, BacktestResult> BacktestJobs = new();

        static InverseHammerStrategyController()
        {
            SeedInitialDetections();
        }

        // ====================================================================
        // 1. CONFIGURATION MANAGEMENT
        // ====================================================================

        [HttpGet("config")]
        public ActionResult<InverseHammerPatternConfig> GetConfig([FromQuery] string? preset = null)
        {
            if (!string.IsNullOrWhiteSpace(preset))
            {
                return Ok(InverseHammerPatternConfig.CreatePreset(preset));
            }
            return Ok(CurrentConfig);
        }

        [HttpPut("config")]
        public ActionResult<InverseHammerPatternConfig> UpdateConfig([FromBody] InverseHammerPatternConfig newConfig)
        {
            if (newConfig == null)
                return BadRequest(new { message = "Configuration payload is required." });

            // Validation rules
            if (newConfig.MinUpperWickToBodyRatio < 1.0m || newConfig.MinUpperWickToBodyRatio > 10.0m)
                return BadRequest(new { message = "MinUpperWickToBodyRatio must be between 1.0 and 10.0." });

            if (newConfig.MaxLowerWickToRangeRatio < 0.01m || newConfig.MaxLowerWickToRangeRatio > 0.50m)
                return BadRequest(new { message = "MaxLowerWickToRangeRatio must be between 0.01 and 0.50." });

            if (newConfig.MaxBodyToRangeRatio < 0.05m || newConfig.MaxBodyToRangeRatio > 0.70m)
                return BadRequest(new { message = "MaxBodyToRangeRatio must be between 0.05 and 0.70." });

            if (newConfig.MaxBodyPosition < 0.10m || newConfig.MaxBodyPosition > 0.80m)
                return BadRequest(new { message = "MaxBodyPosition must be between 0.10 and 0.80." });

            newConfig.Version = CurrentConfig.Version + 1;
            newConfig.UpdatedAt = DateTime.UtcNow;
            CurrentConfig = newConfig;

            return Ok(CurrentConfig);
        }

        [HttpPost("config/reset")]
        public ActionResult<object> ResetConfig()
        {
            CurrentConfig = InverseHammerPatternConfig.CreateDefault();
            return Ok(new { message = "Configuration restored to Balanced defaults.", config = CurrentConfig });
        }

        // ====================================================================
        // 2. SCANNER & RUN
        // ====================================================================

        [HttpGet("scanner")]
        public ActionResult<object> GetScannerResults(
            [FromQuery] string? scope = "ALL", // ALL, CASH, FNO, INDICES
            [FromQuery] string? exchange = "ALL", // ALL, NSE, BSE
            [FromQuery] string? timeframe = "5m",
            [FromQuery] string? status = "ALL", // CONFIRMED, CANDIDATE, ALL
            [FromQuery] decimal minScore = 55.0m)
        {
            var query = RecentDetections.Values.AsEnumerable();

            if (!string.IsNullOrEmpty(scope) && !scope.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                if (scope.Equals("CASH", StringComparison.OrdinalIgnoreCase))
                    query = query.Where(d => d.Segment == "Equity");
                else if (scope.Equals("FNO", StringComparison.OrdinalIgnoreCase))
                    query = query.Where(d => d.Segment == "Futures" || d.Segment == "Options" || StockUniverseService.GetInstrumentBySymbol(d.TradingSymbol)?.IsFnoEligible == true);
                else if (scope.Equals("INDICES", StringComparison.OrdinalIgnoreCase))
                    query = query.Where(d => d.Segment == "Index");
            }

            if (!string.IsNullOrEmpty(exchange) && !exchange.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                query = query.Where(d => d.Exchange.Equals(exchange, StringComparison.OrdinalIgnoreCase));
            }

            if (!string.IsNullOrEmpty(timeframe) && !timeframe.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                query = query.Where(d => d.Timeframe.Equals(timeframe, StringComparison.OrdinalIgnoreCase));
            }

            if (!string.IsNullOrEmpty(status) && !status.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                if (status.Equals("CONFIRMED", StringComparison.OrdinalIgnoreCase))
                    query = query.Where(d => d.PatternStatus == "BUY");
                else if (status.Equals("CANDIDATE", StringComparison.OrdinalIgnoreCase))
                    query = query.Where(d => d.PatternStatus.Contains("CANDIDATE") || d.PatternStatus == "WATCH");
            }

            query = query.Where(d => d.Score.FinalScore >= minScore);

            var list = query.OrderByDescending(d => d.Score.FinalScore).ToList();

            return Ok(new
            {
                totalMatches = list.Count,
                scope = scope ?? "ALL",
                exchange = exchange ?? "ALL",
                timeframe = timeframe ?? "5m",
                results = list
            });
        }

        [HttpPost("scanner/run")]
        public ActionResult<object> RunBatchScan([FromBody] InverseHammerScanRequest request)
        {
            var results = new List<InverseHammerDetectionDetail>();
            var instruments = StockUniverseService.GetAllInstruments();

            if (!string.IsNullOrEmpty(request.Exchange) && !request.Exchange.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                instruments = instruments.Where(i => i.Exchange.Equals(request.Exchange, StringComparison.OrdinalIgnoreCase)).ToList();
            }

            if (request.Scope.Equals("FNO", StringComparison.OrdinalIgnoreCase))
            {
                instruments = instruments.Where(i => i.IsFnoEligible).ToList();
            }
            else if (request.Scope.Equals("CASH", StringComparison.OrdinalIgnoreCase))
            {
                instruments = instruments.Where(i => i.Segment == ExchangeSegment.Equity).ToList();
            }
            else if (request.Scope.Equals("INDICES", StringComparison.OrdinalIgnoreCase))
            {
                instruments = instruments.Where(i => i.Segment == ExchangeSegment.Index).ToList();
            }

            var scanned = instruments.Take(request.Limit > 0 ? request.Limit : 40).ToList();

            foreach (var inst in scanned)
            {
                var candles = GenerateHistoricalCandles(inst.Symbol, inst.LastPrice, 50, true);
                var detail = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, inst, CurrentConfig);
                if (detail.PatternStatus == "BUY" || detail.PatternStatus.Contains("CANDIDATE"))
                {
                    RecentDetections[detail.DetectionId] = detail;
                    results.Add(detail);
                }
            }

            return Ok(new
            {
                message = $"Batch scan completed across {scanned.Count} instruments.",
                scannedCount = scanned.Count,
                detectedCount = results.Count,
                detections = results
            });
        }

        // ====================================================================
        // 3. DETECTIONS & SIGNALS
        // ====================================================================

        [HttpGet("detections")]
        public ActionResult<IEnumerable<InverseHammerDetectionDetail>> GetDetections([FromQuery] int limit = 20)
        {
            return Ok(RecentDetections.Values.OrderByDescending(d => d.CandleTimestamp).Take(limit));
        }

        [HttpGet("signals")]
        public ActionResult<IEnumerable<TradingSignal>> GetSignals([FromQuery] string? status = null)
        {
            var signals = RecentDetections.Values
                .Where(d => d.GeneratedSignal != null && d.PatternStatus == "BUY")
                .Select(d => d.GeneratedSignal!)
                .ToList();

            if (!string.IsNullOrEmpty(status))
            {
                signals = signals.Where(s => s.Status.ToString().Equals(status, StringComparison.OrdinalIgnoreCase)).ToList();
            }

            return Ok(signals);
        }

        // ====================================================================
        // 4. SINGLE INSTRUMENT ANALYSIS & HISTORY
        // ====================================================================

        [HttpGet("{instrumentId}/analysis")]
        public ActionResult<InverseHammerDetectionDetail> GetInstrumentAnalysis(string instrumentId)
        {
            var inst = StockUniverseService.GetInstrumentBySymbol(instrumentId) ??
                       StockUniverseService.GetAllInstruments().FirstOrDefault(i => i.InstrumentId.Equals(instrumentId, StringComparison.OrdinalIgnoreCase));

            if (inst == null)
            {
                inst = new Instrument { Symbol = instrumentId, TradingSymbol = instrumentId, Exchange = "NSE", LastPrice = 1500m };
            }

            var candles = GenerateHistoricalCandles(inst.Symbol, inst.LastPrice, 50, true);
            var detail = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, inst, CurrentConfig);

            RecentDetections[detail.DetectionId] = detail;
            return Ok(detail);
        }

        [HttpGet("{instrumentId}/history")]
        public ActionResult<object> GetInstrumentHistory(string instrumentId)
        {
            var inst = StockUniverseService.GetInstrumentBySymbol(instrumentId) ??
                       new Instrument { Symbol = instrumentId, TradingSymbol = instrumentId, Exchange = "NSE", LastPrice = 1500m };

            var candles = GenerateHistoricalCandles(inst.Symbol, inst.LastPrice, 60, false);
            return Ok(new
            {
                symbol = inst.Symbol,
                exchange = inst.Exchange,
                totalCandles = candles.Count,
                candles
            });
        }

        // ====================================================================
        // 5. BACKTESTING
        // ====================================================================

        [HttpPost("backtest")]
        public ActionResult<object> ExecuteBacktest([FromBody] BacktestRequest request)
        {
            var inst = StockUniverseService.GetInstrumentBySymbol(request.Symbol) ??
                       new Instrument { Symbol = request.Symbol, TradingSymbol = request.Symbol, Exchange = "NSE", LastPrice = 24350m };

            var candles = GenerateHistoricalCandles(request.Symbol, inst.LastPrice, 300, false);
            var strategy = new InverseHammerStrategy();
            var engine = new AllRounderAi.Core.Backtesting.BacktestEngine();
            var result = engine.RunBacktest(candles, inst, strategy, request);

            BacktestJobs[result.BacktestId] = result;
            return Ok(result);
        }

        [HttpGet("backtest/{jobId}")]
        public ActionResult<BacktestResult> GetBacktestResult(string jobId)
        {
            if (BacktestJobs.TryGetValue(jobId, out var res))
            {
                return Ok(res);
            }
            return NotFound(new { message = $"Backtest job '{jobId}' not found." });
        }

        // ====================================================================
        // 6. SEED & HELPER METHODS
        // ====================================================================

        private static void SeedInitialDetections()
        {
            var sampleStocks = new[] { "RELIANCE", "TCS", "NIFTY 50", "BANK NIFTY", "TATAMOTORS" };
            foreach (var sym in sampleStocks)
            {
                var inst = StockUniverseService.GetInstrumentBySymbol(sym);
                if (inst != null)
                {
                    var candles = GenerateHistoricalCandles(inst.Symbol, inst.LastPrice, 50, true);
                    var detail = InverseHammerSignalEngine.AnalyzeInverseHammer(candles, inst, CurrentConfig);
                    RecentDetections[detail.DetectionId] = detail;
                }
            }
        }

        private static List<Candle> GenerateHistoricalCandles(string symbol, decimal basePrice, int count, bool injectInverseHammerAtEnd)
        {
            var list = new List<Candle>();
            var random = new Random(Math.Abs(symbol.GetHashCode()));
            decimal current = basePrice;
            var now = DateTime.UtcNow;

            for (int i = 0; i < count; i++)
            {
                // Trend generation: simulate gradual price decline in recent 10 bars
                decimal step = (decimal)(random.NextDouble() * 0.008 - 0.005) * current;
                if (i >= count - 8)
                {
                    step = -(decimal)(random.NextDouble() * 0.004 + 0.002) * current; // downward trend
                }

                current = Math.Max(10m, current + step);
                decimal spread = current * 0.006m;

                decimal o = current;
                decimal c = current + ((decimal)(random.NextDouble() * 0.004 - 0.002) * current);
                decimal h = Math.Max(o, c) + ((decimal)random.NextDouble() * spread);
                decimal l = Math.Min(o, c) - ((decimal)random.NextDouble() * spread);
                long v = random.Next(50000, 300000);

                list.Add(new Candle
                {
                    Timestamp = now.AddMinutes(-(count - i) * 5),
                    Open = Math.Round(o, 2),
                    High = Math.Round(h, 2),
                    Low = Math.Round(l, 2),
                    Close = Math.Round(c, 2),
                    Volume = v
                });
            }

            if (injectInverseHammerAtEnd && list.Count >= 6)
            {
                // Inject textbook Inverse Hammer on the last candle:
                // Small body located near the bottom of the candle range,
                // long upper wick (>= 2x body), short lower wick (<= 10% range).
                var last = list[^1];
                decimal priorClose = list[^2].Close;
                decimal low = priorClose - (priorClose * 0.008m);
                decimal open = low + 0.50m;
                decimal close = open + 2.00m; // Body = 2.00 (Bullish Green body near low)
                decimal high = close + 8.00m; // Upper wick = 8.00 (4.0x body!)
                // Range = high - low = close + 8.00 - low = low + 2.50 + 8.00 - low = 10.50m
                // Lower wick = open - low = 0.50m (0.50 / 10.50 = 4.76% <= 10%)
                // Body position = (close - low) / Range = 2.50 / 10.50 = 0.238 <= 0.40

                last.Open = Math.Round(open, 2);
                last.Close = Math.Round(close, 2);
                last.Low = Math.Round(low, 2);
                last.High = Math.Round(high, 2);
                last.Volume = 580000; // Strong volume
            }

            return list;
        }
    }

    public class InverseHammerScanRequest
    {
        public string Scope { get; set; } = "ALL"; // ALL, CASH, FNO, INDICES
        public string Exchange { get; set; } = "ALL"; // ALL, NSE, BSE
        public string Timeframe { get; set; } = "5m";
        public decimal MinScore { get; set; } = 60.0m;
        public int Limit { get; set; } = 40;
    }
}
