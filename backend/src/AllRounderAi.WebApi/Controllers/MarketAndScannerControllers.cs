using System;
using System.Collections.Generic;
using Microsoft.AspNetCore.Mvc;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Scanner;

namespace AllRounderAi.WebApi.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")]
    public class MarketController : ControllerBase
    {
        private static readonly List<Instrument> SampleInstruments = new()
        {
            new() { Symbol = "NIFTY 50", TradingSymbol = "NIFTY 50", Name = "Nifty 50 Index", Segment = ExchangeSegment.Index, Exchange = "NSE", LotSize = 50, LastPrice = 24350m, ChangePercent = 0.45m, IsFnoEligible = true },
            new() { Symbol = "BANKNIFTY", TradingSymbol = "BANK NIFTY", Name = "Nifty Bank Index", Segment = ExchangeSegment.Index, Exchange = "NSE", LotSize = 15, LastPrice = 51240m, ChangePercent = 0.82m, IsFnoEligible = true },
            new() { Symbol = "RELIANCE", TradingSymbol = "RELIANCE", Name = "Reliance Industries Ltd", Segment = ExchangeSegment.Equity, Exchange = "NSE", LotSize = 250, LastPrice = 1280.5m, ChangePercent = -0.35m, IsFnoEligible = true },
            new() { Symbol = "TCS", TradingSymbol = "TCS", Name = "Tata Consultancy Services", Segment = ExchangeSegment.Equity, Exchange = "NSE", LotSize = 175, LastPrice = 4150.0m, ChangePercent = 1.15m, IsFnoEligible = true },
            new() { Symbol = "GOLD", TradingSymbol = "GOLDM", Name = "Gold Mini", Segment = ExchangeSegment.Commodity, Exchange = "MCX", LotSize = 10, LastPrice = 76500m, ChangePercent = 0.25m, IsFnoEligible = false }
        };

        [HttpGet("instruments")]
        public ActionResult<IEnumerable<Instrument>> GetInstruments([FromQuery] string? segment = null)
        {
            if (string.IsNullOrEmpty(segment))
                return Ok(SampleInstruments);

            return Ok(SampleInstruments.FindAll(i => i.Segment.ToString().Equals(segment, StringComparison.OrdinalIgnoreCase)));
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
            decimal basePrice = symbol.Contains("BANK") ? 51000m : symbol.Contains("NIFTY") ? 24000m : 2500m;
            var dt = DateTime.UtcNow.AddMinutes(-5 * count);

            var rand = new Random(42);
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
            var instruments = new List<Instrument>
            {
                new() { Symbol = "NIFTY 50", TradingSymbol = "NIFTY 50", Segment = ExchangeSegment.Index, Exchange = "NSE", LotSize = 50, LastPrice = 24350m, IsFnoEligible = true },
                new() { Symbol = "BANKNIFTY", TradingSymbol = "BANK NIFTY", Segment = ExchangeSegment.Index, Exchange = "NSE", LotSize = 15, LastPrice = 51240m, IsFnoEligible = true },
                new() { Symbol = "RELIANCE", TradingSymbol = "RELIANCE", Segment = ExchangeSegment.Equity, Exchange = "NSE", LotSize = 250, LastPrice = 1280.5m, IsFnoEligible = true }
            };

            var candleCache = new Dictionary<string, IReadOnlyList<Candle>>();
            foreach (var inst in instruments)
            {
                // Generate test candle data with an active pattern
                candleCache[inst.Symbol] = GenerateCandlesWithSetup(inst.LastPrice);
            }

            var config = new StrategyConfig
            {
                Timeframe = request.Timeframe,
                ConfirmationMode = request.ConfirmationMode
            };

            var result = _scannerService.ExecuteScan(instruments, candleCache, request, config);
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
