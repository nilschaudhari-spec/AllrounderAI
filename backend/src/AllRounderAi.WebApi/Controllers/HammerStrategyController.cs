using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Services;
using AllRounderAi.Core.Strategies;
using AllRounderAi.Core.Strategies.Hammer;

namespace AllRounderAi.WebApi.Controllers
{
    [ApiController]
    [Route("api/strategies/hammer")]
    [Route("api/v1/strategies/hammer")]
    public class HammerStrategyController : ControllerBase
    {
        private static HammerPatternConfig CurrentConfig = HammerPatternConfig.CreateDefault();
        private static readonly ConcurrentDictionary<string, HammerDetectionDetail> RecentDetections = new();
        private static readonly ConcurrentDictionary<string, BacktestResult> BacktestJobs = new();

        static HammerStrategyController()
        {
            SeedInitialDetections();
        }

        // ====================================================================
        // 1. CONFIGURATION MANAGEMENT
        // ====================================================================

        [HttpGet("config")]
        public ActionResult<HammerPatternConfig> GetConfig([FromQuery] string? preset = null)
        {
            if (!string.IsNullOrWhiteSpace(preset))
            {
                return Ok(HammerPatternConfig.CreatePreset(preset));
            }
            return Ok(CurrentConfig);
        }

        [HttpPut("config")]
        public ActionResult<HammerPatternConfig> UpdateConfig([FromBody] HammerPatternConfig newConfig)
        {
            if (newConfig == null)
                return BadRequest(new { message = "Configuration payload is required." });

            // Validation rules
            if (newConfig.MinLowerWickToBodyRatio < 1.0m || newConfig.MinLowerWickToBodyRatio > 10.0m)
                return BadRequest(new { message = "MinLowerWickToBodyRatio must be between 1.0 and 10.0." });

            if (newConfig.MaxUpperWickToRangeRatio < 0.01m || newConfig.MaxUpperWickToRangeRatio > 0.50m)
                return BadRequest(new { message = "MaxUpperWickToRangeRatio must be between 0.01 and 0.50." });

            if (newConfig.MaxBodyToRangeRatio < 0.05m || newConfig.MaxBodyToRangeRatio > 0.70m)
                return BadRequest(new { message = "MaxBodyToRangeRatio must be between 0.05 and 0.70." });

            newConfig.Version = CurrentConfig.Version + 1;
            newConfig.UpdatedAt = DateTime.UtcNow;
            CurrentConfig = newConfig;

            return Ok(CurrentConfig);
        }

        [HttpPost("config/reset")]
        public ActionResult<HammerPatternConfig> ResetConfig()
        {
            CurrentConfig = HammerPatternConfig.CreateDefault();
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

            if (!string.IsNullOrEmpty(status) && !status.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                query = query.Where(d => d.PatternStatus.Equals(status, StringComparison.OrdinalIgnoreCase));
            }

            query = query.Where(d => d.Score.FinalScore >= minScore);

            var list = query.OrderByDescending(d => d.Score.FinalScore).ToList();

            return Ok(new
            {
                totalMatches = list.Count,
                scope,
                exchange,
                timeframe,
                results = list
            });
        }

        [HttpPost("scanner/run")]
        public ActionResult<object> RunScanner([FromBody] HammerScanRequest request)
        {
            var universe = StockUniverseService.GetAllInstruments();
            var targetList = universe.AsEnumerable();

            if (!string.IsNullOrEmpty(request.Exchange) && !request.Exchange.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                targetList = targetList.Where(i => i.Exchange.Equals(request.Exchange, StringComparison.OrdinalIgnoreCase));
            }

            if (request.Scope.Equals("FNO", StringComparison.OrdinalIgnoreCase))
            {
                targetList = targetList.Where(i => i.IsFnoEligible);
            }
            else if (request.Scope.Equals("CASH", StringComparison.OrdinalIgnoreCase))
            {
                targetList = targetList.Where(i => i.Segment == ExchangeSegment.Equity);
            }
            else if (request.Scope.Equals("INDICES", StringComparison.OrdinalIgnoreCase))
            {
                targetList = targetList.Where(i => i.Segment == ExchangeSegment.Index);
            }

            var scanned = targetList.Take(request.Limit > 0 ? request.Limit : 40).ToList();
            var detections = new List<HammerDetectionDetail>();

            foreach (var inst in scanned)
            {
                var candles = GenerateHistoricalCandles(inst.Symbol, inst.LastPrice > 0 ? inst.LastPrice : 1500m, 50, true);
                var detail = HammerSignalEngine.AnalyzeHammer(candles, inst, CurrentConfig);
                if (detail.PatternStatus != "NO_TRADE")
                {
                    detections.Add(detail);
                    RecentDetections[detail.DetectionId] = detail;
                }
            }

            return Ok(new
            {
                scannedCount = scanned.Count,
                matchedCount = detections.Count,
                detections = detections.OrderByDescending(d => d.Score.FinalScore).ToList()
            });
        }

        [HttpPost("watchlist/scan")]
        public ActionResult<object> ScanWatchlist([FromBody] List<string> symbols)
        {
            if (symbols == null || symbols.Count == 0)
                return BadRequest(new { message = "Symbol list cannot be empty." });

            var detections = new List<HammerDetectionDetail>();
            foreach (var sym in symbols)
            {
                var inst = StockUniverseService.GetInstrumentBySymbol(sym) ?? new Instrument { Symbol = sym, TradingSymbol = sym, Exchange = "NSE" };
                var candles = GenerateHistoricalCandles(inst.Symbol, inst.LastPrice > 0 ? inst.LastPrice : 1500m, 50, true);
                var detail = HammerSignalEngine.AnalyzeHammer(candles, inst, CurrentConfig);
                detections.Add(detail);
            }

            return Ok(new
            {
                totalWatchlist = symbols.Count,
                detections
            });
        }

        // ====================================================================
        // 3. DETECTIONS & SIGNALS
        // ====================================================================

        [HttpGet("detections")]
        public ActionResult<IEnumerable<HammerDetectionDetail>> GetDetections([FromQuery] int limit = 20)
        {
            return Ok(RecentDetections.Values.OrderByDescending(d => d.CandleTimestamp).Take(limit));
        }

        [HttpGet("signals")]
        public ActionResult<IEnumerable<TradingSignal>> GetSignals([FromQuery] string? status = null)
        {
            var signals = RecentDetections.Values
                .Where(d => d.GeneratedSignal != null)
                .Select(d => d.GeneratedSignal!)
                .ToList();

            if (!string.IsNullOrEmpty(status))
            {
                signals = signals.Where(s => s.Status.ToString().Equals(status, StringComparison.OrdinalIgnoreCase)).ToList();
            }

            return Ok(signals);
        }

        [HttpGet("{instrumentId}/analysis")]
        public ActionResult<HammerDetectionDetail> GetInstrumentAnalysis(string instrumentId)
        {
            var inst = StockUniverseService.GetInstrumentBySymbol(instrumentId) ??
                       StockUniverseService.GetAllInstruments().FirstOrDefault(i => i.InstrumentId.Equals(instrumentId, StringComparison.OrdinalIgnoreCase));

            if (inst == null)
            {
                inst = new Instrument { Symbol = instrumentId, TradingSymbol = instrumentId, Exchange = "NSE", LastPrice = 1500m };
            }

            var candles = GenerateHistoricalCandles(inst.Symbol, inst.LastPrice, 50, true);
            var detail = HammerSignalEngine.AnalyzeHammer(candles, inst, CurrentConfig);
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
        // 4. BACKTESTING
        // ====================================================================

        [HttpPost("backtest")]
        public ActionResult<object> ExecuteBacktest([FromBody] BacktestRequest request)
        {
            var inst = StockUniverseService.GetInstrumentBySymbol(request.Symbol) ??
                       new Instrument { Symbol = request.Symbol, TradingSymbol = request.Symbol, Exchange = "NSE", LastPrice = 24350m };

            var candles = GenerateHistoricalCandles(request.Symbol, inst.LastPrice, 300, false);
            var strategy = new HammerStrategy();
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
        // HELPERS & SEED DATA
        // ====================================================================

        private static List<Candle> GenerateHistoricalCandles(string symbol, decimal basePrice, int count, bool injectHammerAtEnd)
        {
            var list = new List<Candle>();
            var dt = DateTime.UtcNow.AddMinutes(-5 * count);
            var rand = new Random(symbol.GetHashCode() ^ 42);

            decimal p = basePrice > 0 ? basePrice : 1500m;
            for (int i = 0; i < count - (injectHammerAtEnd ? 1 : 0); i++)
            {
                decimal chg = (decimal)(rand.NextDouble() * 15.0 - 9.0); // Mild downward drift
                decimal open = p;
                decimal close = p + chg;
                decimal high = Math.Max(open, close) + (decimal)(rand.NextDouble() * 5.0);
                decimal low = Math.Min(open, close) - (decimal)(rand.NextDouble() * 5.0);

                list.Add(new Candle
                {
                    Timestamp = dt.AddMinutes(5 * i),
                    Open = open,
                    High = high,
                    Low = low,
                    Close = close,
                    Volume = rand.Next(20000, 300000)
                });
                p = close;
            }

            if (injectHammerAtEnd)
            {
                // Inject an authentic textbook Hammer: small body, long lower wick (>= 2.5x body), tiny upper wick
                decimal hammerOpen = p;
                decimal hammerClose = p + 4.0m; // Small green body of 4 pts
                decimal hammerHigh = hammerClose + 0.5m; // Tiny upper wick of 0.5 pt
                decimal hammerLow = hammerOpen - 12.0m; // Long lower wick of 12 pts (3.0x body!)

                list.Add(new Candle
                {
                    Timestamp = dt.AddMinutes(5 * count),
                    Open = hammerOpen,
                    High = hammerHigh,
                    Low = hammerLow,
                    Close = hammerClose,
                    Volume = 580000 // Above-average volume
                });
            }

            return list;
        }

        private static void SeedInitialDetections()
        {
            var sampleStocks = new[] { "RELIANCE", "TCS", "NIFTY 50", "BANK NIFTY", "TATAMOTORS" };
            foreach (var sym in sampleStocks)
            {
                var inst = StockUniverseService.GetInstrumentBySymbol(sym);
                if (inst != null)
                {
                    var candles = GenerateHistoricalCandles(inst.Symbol, inst.LastPrice, 50, true);
                    var detail = HammerSignalEngine.AnalyzeHammer(candles, inst, CurrentConfig);
                    RecentDetections[detail.DetectionId] = detail;
                }
            }
        }
    }

    public class HammerScanRequest
    {
        public string Scope { get; set; } = "ALL"; // ALL, CASH, FNO, INDICES
        public string Exchange { get; set; } = "ALL"; // ALL, NSE, BSE
        public string Timeframe { get; set; } = "5m";
        public decimal MinScore { get; set; } = 60.0m;
        public int Limit { get; set; } = 40;
    }
}
