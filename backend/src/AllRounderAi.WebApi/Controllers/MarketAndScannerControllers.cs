using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Scanner;
using AllRounderAi.Core.Services;

namespace AllRounderAi.WebApi.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    public class MarketController : ControllerBase
    {
        private readonly LiveQuoteService _quoteService = new();

        [HttpGet("instruments")]
        public ActionResult<IEnumerable<Instrument>> GetInstruments(
            [FromQuery] string? exchange = null,
            [FromQuery] string? segment = null,
            [FromQuery] string? search = null,
            [FromQuery] bool? fnoOnly = null,
            [FromQuery] int limit = 200)
        {
            var results = StockUniverseService.GetInstruments(exchange, segment, search, fnoOnly, limit);
            return Ok(results);
        }

        [HttpGet("quote")]
        public async Task<ActionResult<LiveQuote>> GetLiveQuote([FromQuery] string symbol, [FromQuery] string exchange = "NSE")
        {
            if (string.IsNullOrWhiteSpace(symbol))
                return BadRequest(new { message = "Symbol parameter is required." });

            var quote = await _quoteService.GetQuoteAsync(symbol, exchange);
            return Ok(quote);
        }

        [HttpGet("quotes")]
        public async Task<ActionResult<IEnumerable<LiveQuote>>> GetQuotes(
            [FromQuery] string? exchange = null, 
            [FromQuery] string? segment = null,
            [FromQuery] int limit = 30)
        {
            var instruments = StockUniverseService.GetInstruments(exchange, segment, limit: limit);
            var quotes = await _quoteService.GetQuotesBatchAsync(instruments);
            return Ok(quotes);
        }

        [HttpGet("candles")]
        public ActionResult<object> GetCandles([FromQuery] string symbol, [FromQuery] string timeframe = "5m", [FromQuery] int limit = 100)
        {
            var candles = GenerateSyntheticCandles(symbol, limit);
            return Ok(new
            {
                symbol,
                timeframe,
                candles
            });
        }

        private static List<Candle> GenerateSyntheticCandles(string symbol, int count)
        {
            var list = new List<Candle>();
            var inst = StockUniverseService.GetInstrumentBySymbol(symbol);
            decimal basePrice = inst?.LastPrice > 0 ? inst.LastPrice : (symbol.Contains("BANK") ? 51000m : symbol.Contains("NIFTY") ? 24000m : 2500m);
            var dt = DateTime.UtcNow.AddMinutes(-5 * count);

            var rand = new Random(symbol.GetHashCode());
            for (int i = 0; i < count; i++)
            {
                decimal change = (decimal)(rand.NextDouble() * 40.0 - 19.5);
                decimal open = basePrice;
                decimal close = basePrice + change;
                decimal high = Math.Max(open, close) + (decimal)(rand.NextDouble() * 15.0);
                decimal low = Math.Min(open, close) - (decimal)(rand.NextDouble() * 15.0);

                list.Add(new Candle
                {
                    Timestamp = dt.AddMinutes(5 * i),
                    Open = open,
                    High = high,
                    Low = low,
                    Close = close,
                    Volume = rand.Next(10000, 500000),
                    OpenInterest = rand.Next(500000, 2000000)
                });

                basePrice = close;
            }
            return list;
        }
    }

    [ApiController]
    [Route("api/v1/[controller]")]
    public class ScannerController : ControllerBase
    {
        private readonly MarketScannerService _scannerService = new();

        [HttpPost("run")]
        public ActionResult<ScanResult> RunScan([FromBody] ScanRequest request)
        {
            // Pick instruments from universe according to requested scope
            var universe = StockUniverseService.GetAllInstruments();
            var targetList = universe.AsEnumerable();

            if (request.MarketScope.Equals("FNO", StringComparison.OrdinalIgnoreCase))
            {
                targetList = targetList.Where(i => i.IsFnoEligible);
            }
            else if (request.MarketScope.Equals("INDEX", StringComparison.OrdinalIgnoreCase))
            {
                targetList = targetList.Where(i => i.Segment == ExchangeSegment.Index);
            }
            else if (request.MarketScope.Equals("COMMODITY", StringComparison.OrdinalIgnoreCase))
            {
                targetList = targetList.Where(i => i.Segment == ExchangeSegment.Commodity);
            }

            var selectedInstruments = targetList.Take(30).ToList();

            var candleCache = new Dictionary<string, IReadOnlyList<Candle>>();
            foreach (var inst in selectedInstruments)
            {
                candleCache[inst.Symbol] = GenerateCandlesWithSetup(inst.LastPrice);
            }

            var config = new StrategyConfig
            {
                Timeframe = request.Timeframe,
                ConfirmationMode = request.ConfirmationMode
            };

            var result = _scannerService.ExecuteScan(selectedInstruments, candleCache, request, config);
            return Ok(result);
        }

        private static List<Candle> GenerateCandlesWithSetup(decimal basePrice)
        {
            var list = new List<Candle>();
            var dt = DateTime.UtcNow.AddMinutes(-5 * 40);
            for (int i = 0; i < 35; i++)
            {
                list.Add(new Candle
                {
                    Timestamp = dt.AddMinutes(5 * i),
                    Open = basePrice + i,
                    High = basePrice + i + 10,
                    Low = basePrice + i - 5,
                    Close = basePrice + i + 8,
                    Volume = 50000
                });
            }
            // Add a clean 5-minute setup
            list.Add(new Candle
            {
                Timestamp = dt.AddMinutes(5 * 35),
                Open = basePrice + 35,
                High = basePrice + 65,
                Low = basePrice + 34,
                Close = basePrice + 60,
                Volume = 120000
            });
            return list;
        }
    }
}
