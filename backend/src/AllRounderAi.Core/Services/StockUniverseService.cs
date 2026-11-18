using System;
using System.Collections.Generic;
using System.Linq;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Services
{
    public static class StockUniverseService
    {
        private static readonly List<Instrument> Universe = new();

        static StockUniverseService()
        {
            InitializeUniverse();
        }

        public static IReadOnlyList<Instrument> GetAllInstruments() => Universe.AsReadOnly();

        public static IEnumerable<Instrument> GetInstruments(
            string? exchange = null,
            string? segment = null,
            string? search = null,
            bool? fnoOnly = null,
            int limit = 200)
        {
            var query = Universe.AsEnumerable();

            if (!string.IsNullOrWhiteSpace(exchange) && !exchange.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                query = query.Where(i => i.Exchange.Equals(exchange, StringComparison.OrdinalIgnoreCase));
            }

            if (!string.IsNullOrWhiteSpace(segment) && !segment.Equals("ALL", StringComparison.OrdinalIgnoreCase))
            {
                query = query.Where(i => i.Segment.ToString().Equals(segment, StringComparison.OrdinalIgnoreCase));
            }

            if (fnoOnly == true)
            {
                query = query.Where(i => i.IsFnoEligible);
            }

            if (!string.IsNullOrWhiteSpace(search))
            {
                var term = search.Trim();
                query = query.Where(i =>
                    i.Symbol.Contains(term, StringComparison.OrdinalIgnoreCase) ||
                    i.TradingSymbol.Contains(term, StringComparison.OrdinalIgnoreCase) ||
                    i.Name.Contains(term, StringComparison.OrdinalIgnoreCase));
            }

            return query.Take(limit);
        }

        public static Instrument? GetInstrumentBySymbol(string symbol, string? exchange = null)
        {
            var cleaned = symbol.Replace(" ", "").ToUpperInvariant();
            if (!string.IsNullOrEmpty(exchange))
            {
                var match = Universe.FirstOrDefault(i => 
                    i.Symbol.Replace(" ", "").Equals(cleaned, StringComparison.OrdinalIgnoreCase) && 
                    i.Exchange.Equals(exchange, StringComparison.OrdinalIgnoreCase));
                if (match != null) return match;
            }

            return Universe.FirstOrDefault(i => 
                i.Symbol.Replace(" ", "").Equals(cleaned, StringComparison.OrdinalIgnoreCase) ||
                i.TradingSymbol.Replace(" ", "").Equals(cleaned, StringComparison.OrdinalIgnoreCase));
        }

        private static void InitializeUniverse()
        {
            // Major Indices
            AddIndex("NIFTY 50", "NIFTY 50", "Nifty 50 Benchmark Index", "NSE", 50, 24350.25m, 0.45m, true);
            AddIndex("BANKNIFTY", "BANK NIFTY", "Nifty Bank Sectoral Index", "NSE", 15, 51240.00m, 0.82m, true);
            AddIndex("FINNIFTY", "FIN NIFTY", "Nifty Financial Services Index", "NSE", 40, 23890.50m, 0.55m, true);
            AddIndex("NIFTYIT", "NIFTY IT", "Nifty IT Sectoral Index", "NSE", 25, 42150.00m, 1.25m, true);
            AddIndex("SENSEX", "SENSEX", "BSE S&P Sensex Benchmark Index", "BSE", 10, 79880.50m, 0.38m, false);
            AddIndex("BSE100", "BSE 100", "S&P BSE 100 Index", "BSE", 25, 25600.00m, 0.42m, false);
            AddIndex("BSE500", "BSE 500", "S&P BSE 500 Broad Market Index", "BSE", 50, 36800.00m, 0.48m, false);

            // MCX Commodities
            AddCommodity("GOLD", "MCX GOLD", "Gold Mini 100g", 10, 76500.00m, -0.15m);
            AddCommodity("SILVER", "MCX SILVER", "Silver Mini 5kg", 5, 91200.00m, 0.65m);
            AddCommodity("CRUDEOIL", "MCX CRUDE", "Crude Oil 100 BBL", 100, 6150.00m, -1.10m);

            // NIFTY 50 & Top Equities - Listed on both NSE and BSE
            AddDualEquity("RELIANCE", "Reliance Industries Ltd", 250, 1363.00m, -0.29m, true);
            AddDualEquity("TCS", "Tata Consultancy Services Ltd", 175, 4150.00m, 1.15m, true);
            AddDualEquity("HDFCBANK", "HDFC Bank Ltd", 550, 1740.50m, 0.65m, true);
            AddDualEquity("INFY", "Infosys Ltd", 400, 1860.00m, 1.45m, true);
            AddDualEquity("ICICIBANK", "ICICI Bank Ltd", 700, 1265.00m, 0.72m, true);
            AddDualEquity("SBIN", "State Bank of India", 750, 815.00m, -0.18m, true);
            AddDualEquity("BHARTIARTL", "Bharti Airtel Ltd", 475, 1590.00m, 0.88m, true);
            AddDualEquity("ITC", "ITC Ltd", 1600, 475.50m, 0.35m, true);
            AddDualEquity("LT", "Larsen & Toubro Ltd", 150, 3580.00m, 0.95m, true);
            AddDualEquity("KOTAKBANK", "Kotak Mahindra Bank Ltd", 400, 1780.00m, 0.40m, true);
            AddDualEquity("AXISBANK", "Axis Bank Ltd", 625, 1175.00m, -0.45m, true);
            AddDualEquity("TATAMOTORS", "Tata Motors Ltd", 1425, 790.00m, 1.85m, true);
            AddDualEquity("BAJFINANCE", "Bajaj Finance Ltd", 125, 6850.00m, 0.90m, true);
            AddDualEquity("MARUTI", "Maruti Suzuki India Ltd", 50, 11250.00m, 0.30m, true);
            AddDualEquity("SUNPHARMA", "Sun Pharmaceutical Industries Ltd", 350, 1780.00m, 0.55m, true);
            AddDualEquity("ASIANPAINT", "Asian Paints Ltd", 200, 2480.00m, -1.25m, true);
            AddDualEquity("TITAN", "Titan Company Ltd", 175, 3240.00m, 0.45m, true);
            AddDualEquity("HCLTECH", "HCL Technologies Ltd", 350, 1845.00m, 1.60m, true);
            AddDualEquity("WIPRO", "Wipro Ltd", 1500, 565.00m, 1.10m, true);
            AddDualEquity("NTPC", "NTPC Ltd", 1500, 385.00m, 0.75m, true);
            AddDualEquity("ONGC", "Oil & Natural Gas Corp Ltd", 2250, 260.00m, -0.30m, true);
            AddDualEquity("POWERGRID", "Power Grid Corp of India Ltd", 1800, 320.00m, 0.45m, true);
            AddDualEquity("TATASTEEL", "Tata Steel Ltd", 5500, 142.50m, 0.85m, true);
            AddDualEquity("JSWSTEEL", "JSW Steel Ltd", 675, 965.00m, 1.05m, true);
            AddDualEquity("ADANIENT", "Adani Enterprises Ltd", 300, 2820.00m, 2.10m, true);
            AddDualEquity("ADANIPORTS", "Adani Ports & SEZ Ltd", 400, 1290.00m, 1.35m, true);
            AddDualEquity("COALINDIA", "Coal India Ltd", 2100, 415.00m, 0.60m, true);
            AddDualEquity("BAJAJFINSV", "Bajaj Finserv Ltd", 500, 1610.00m, 0.80m, true);
            AddDualEquity("BPCL", "Bharat Petroleum Corp Ltd", 1800, 310.00m, -0.65m, true);
            AddDualEquity("EICHERMOT", "Eicher Motors Ltd", 150, 4850.00m, 1.95m, true);
            AddDualEquity("HEROMOTOCO", "Hero MotoCorp Ltd", 150, 4650.00m, 0.45m, true);
            AddDualEquity("CIPLA", "Cipla Ltd", 650, 1510.00m, 0.70m, true);
            AddDualEquity("DRREDDY", "Dr. Reddy's Laboratories Ltd", 125, 1260.00m, 0.25m, true);
            AddDualEquity("DIVISLAB", "Divi's Laboratories Ltd", 150, 5920.00m, 1.40m, true);
            AddDualEquity("APOLLOHOSP", "Apollo Hospitals Enterprise Ltd", 125, 6950.00m, 1.15m, true);
            AddDualEquity("ULTRACEMCO", "UltraTech Cement Ltd", 100, 11400.00m, 0.85m, true);
            AddDualEquity("GRASIM", "Grasim Industries Ltd", 250, 2540.00m, 0.60m, true);
            AddDualEquity("TECHM", "Tech Mahindra Ltd", 600, 1690.00m, 1.75m, true);
            AddDualEquity("HINDALCO", "Hindalco Industries Ltd", 1400, 650.00m, 1.20m, true);
            AddDualEquity("NESTLEIND", "Nestle India Ltd", 25, 2280.00m, -0.40m, true);
            AddDualEquity("BRITANNIA", "Britannia Industries Ltd", 200, 4920.00m, 0.15m, true);
            AddDualEquity("TATACONSUM", "Tata Consumer Products Ltd", 900, 960.00m, 0.50m, true);
            AddDualEquity("INDUSINDBK", "IndusInd Bank Ltd", 500, 1020.00m, -1.80m, true);
            AddDualEquity("SBILIFE", "SBI Life Insurance Co Ltd", 375, 1510.00m, 0.35m, true);
            AddDualEquity("HDFCLIFE", "HDFC Life Insurance Co Ltd", 1100, 685.00m, 0.20m, true);
            AddDualEquity("BAJAJ-AUTO", "Bajaj Auto Ltd", 125, 8750.00m, 0.95m, true);
            AddDualEquity("TRENT", "Trent Ltd", 200, 6420.00m, 2.45m, true);
            AddDualEquity("BEL", "Bharat Electronics Ltd", 2850, 295.00m, 1.65m, true);
            AddDualEquity("SHRIRAMFIN", "Shriram Finance Ltd", 300, 3120.00m, 0.85m, true);

            // NIFTY NEXT 50 & High-Growth Midcaps (Dual Listed NSE & BSE)
            AddDualEquity("HAL", "Hindustan Aeronautics Ltd", 300, 4180.00m, 2.30m, true);
            AddDualEquity("ZOMATO", "Zomato Ltd", 3300, 275.50m, 3.20m, true);
            AddDualEquity("JIOFIN", "Jio Financial Services Ltd", 2150, 315.00m, 1.10m, true);
            AddDualEquity("IRFC", "Indian Railway Finance Corp", 3500, 148.00m, 0.95m, false);
            AddDualEquity("RVNL", "Rail Vikas Nigam Ltd", 1750, 420.00m, 2.15m, false);
            AddDualEquity("VEDL", "Vedanta Ltd", 1550, 445.00m, 1.40m, true);
            AddDualEquity("SUZLON", "Suzlon Energy Ltd", 5000, 62.50m, 2.80m, false);
            AddDualEquity("YESBANK", "Yes Bank Ltd", 5000, 19.80m, -0.50m, false);
            AddDualEquity("IDEA", "Vodafone Idea Ltd", 10000, 7.45m, -1.20m, false);
            AddDualEquity("DLF", "DLF Ltd", 825, 840.00m, 1.15m, true);
            AddDualEquity("SIEMENS", "Siemens Ltd", 125, 7150.00m, 1.30m, true);
            AddDualEquity("INDHOTEL", "The Indian Hotels Co Ltd", 1000, 760.00m, 1.80m, true);
            AddDualEquity("TATAPOWER", "Tata Power Co Ltd", 1650, 395.00m, 0.70m, true);
            AddDualEquity("AMBUJACEM", "Ambuja Cements Ltd", 900, 545.00m, 0.85m, true);
            AddDualEquity("RECLTD", "REC Ltd", 1400, 510.00m, 1.25m, true);
            AddDualEquity("PFC", "Power Finance Corporation Ltd", 1300, 460.00m, 1.10m, true);
            AddDualEquity("GAIL", "GAIL India Ltd", 2650, 192.00m, 0.45m, true);
            AddDualEquity("BHEL", "Bharat Heavy Electricals Ltd", 2625, 230.00m, 1.55m, true);
            AddDualEquity("POLYCAB", "Polycab India Ltd", 125, 6650.00m, 0.90m, true);
            AddDualEquity("PERSISTENT", "Persistent Systems Ltd", 100, 5650.00m, 2.10m, true);
            AddDualEquity("COFORGE", "Coforge Ltd", 150, 8150.00m, 1.95m, true);
            AddDualEquity("DIXON", "Dixon Technologies Ltd", 50, 15200.00m, 2.75m, true);
            AddDualEquity("HAVELLS", "Havells India Ltd", 500, 1620.00m, 0.50m, true);
            AddDualEquity("ABB", "ABB India Ltd", 125, 7350.00m, 1.40m, true);
            AddDualEquity("ASHOKLEY", "Ashok Leyland Ltd", 5000, 220.00m, 0.80m, true);
            AddDualEquity("CGPOWER", "CG Power & Industrial Solutions", 850, 680.00m, 1.65m, false);
            AddDualEquity("FEDERALBNK", "Federal Bank Ltd", 5000, 202.00m, 0.90m, true);
            AddDualEquity("IDFCFIRSTB", "IDFC First Bank Ltd", 7500, 64.50m, 0.30m, true);
            AddDualEquity("AUBANK", "AU Small Finance Bank Ltd", 1000, 595.00m, -0.80m, true);
            AddDualEquity("BANKBARODA", "Bank of Baroda", 2925, 245.00m, 0.55m, true);
            AddDualEquity("CANBK", "Canara Bank", 4500, 98.50m, 0.70m, true);
            AddDualEquity("PNB", "Punjab National Bank", 8000, 98.00m, 0.40m, true);
            AddDualEquity("IOC", "Indian Oil Corporation Ltd", 4875, 135.00m, -0.20m, true);
            AddDualEquity("HINDPETRO", "Hindustan Petroleum Corp Ltd", 2025, 360.00m, -0.45m, true);
            AddDualEquity("NMDC", "NMDC Ltd", 3375, 218.00m, 1.10m, true);
            AddDualEquity("SAIL", "Steel Authority of India Ltd", 8000, 114.00m, 0.65m, true);
            AddDualEquity("NATIONALUM", "National Aluminium Co Ltd", 3750, 235.00m, 2.05m, true);
            AddDualEquity("EXIDEIND", "Exide Industries Ltd", 1200, 440.00m, 0.85m, true);
            AddDualEquity("LTIM", "LTIMindtree Ltd", 150, 5950.00m, 1.35m, true);
            AddDualEquity("MPHASIS", "Mphasis Ltd", 275, 2980.00m, 1.50m, true);
            AddDualEquity("KPITTECH", "KPIT Technologies Ltd", 400, 1420.00m, 1.85m, false);
            AddDualEquity("TATAELXSI", "Tata Elxsi Ltd", 100, 6650.00m, 0.75m, true);

            // BSE Marquee & Capital Markets (Listed on BSE and NSE)
            AddDualEquity("BSE", "BSE Limited", 375, 4650.00m, 3.40m, true);
            AddDualEquity("CDSL", "Central Depository Services Ltd", 425, 1540.00m, 2.10m, true);
            AddDualEquity("ANGELONE", "Angel One Ltd", 250, 2850.00m, 1.75m, true);
            AddBseExclusiveEquity("500033", "FORCEMOT", "Force Motors Ltd", 7250.00m, 1.85m);
            AddBseExclusiveEquity("500020", "BOMBDYE", "Bombay Dyeing & Mfg Co", 195.00m, -0.75m);
            AddBseExclusiveEquity("500067", "BLUESTARCO", "Blue Star Ltd", 1780.00m, 1.20m);
        }

        private static void AddIndex(string symbol, string tradingSymbol, string name, string exchange, int lotSize, decimal price, decimal chg, bool isFno)
        {
            Universe.Add(new Instrument
            {
                InstrumentId = $"idx-{exchange.ToLower()}-{symbol.ToLower()}",
                Exchange = exchange,
                Symbol = symbol,
                TradingSymbol = tradingSymbol,
                Name = name,
                Segment = ExchangeSegment.Index,
                LotSize = lotSize,
                TickSize = 0.05m,
                LastPrice = price,
                ChangePercent = chg,
                IsFnoEligible = isFno
            });
        }

        private static void AddCommodity(string symbol, string tradingSymbol, string name, int lotSize, decimal price, decimal chg)
        {
            Universe.Add(new Instrument
            {
                InstrumentId = $"mcx-{symbol.ToLower()}",
                Exchange = "MCX",
                Symbol = symbol,
                TradingSymbol = tradingSymbol,
                Name = name,
                Segment = ExchangeSegment.Commodity,
                LotSize = lotSize,
                TickSize = 1.0m,
                LastPrice = price,
                ChangePercent = chg,
                IsFnoEligible = true
            });
        }

        private static void AddDualEquity(string symbol, string name, int lotSize, decimal price, decimal chg, bool isFno)
        {
            // NSE Entry
            Universe.Add(new Instrument
            {
                InstrumentId = $"nse-eq-{symbol.ToLower()}",
                Exchange = "NSE",
                Symbol = symbol,
                TradingSymbol = symbol,
                Name = $"{name} (NSE)",
                Segment = ExchangeSegment.Equity,
                LotSize = lotSize,
                TickSize = 0.05m,
                LastPrice = price,
                ChangePercent = chg,
                IsFnoEligible = isFno
            });

            // BSE Entry
            Universe.Add(new Instrument
            {
                InstrumentId = $"bse-eq-{symbol.ToLower()}",
                Exchange = "BSE",
                Symbol = symbol,
                TradingSymbol = $"{symbol}.BO",
                Name = $"{name} (BSE)",
                Segment = ExchangeSegment.Equity,
                LotSize = lotSize,
                TickSize = 0.05m,
                LastPrice = price,
                ChangePercent = chg,
                IsFnoEligible = isFno
            });
        }

        private static void AddBseExclusiveEquity(string scripCode, string symbol, string name, decimal price, decimal chg)
        {
            Universe.Add(new Instrument
            {
                InstrumentId = $"bse-scrip-{scripCode}",
                Exchange = "BSE",
                Symbol = symbol,
                TradingSymbol = $"{symbol} ({scripCode})",
                Name = $"{name} (BSE)",
                Segment = ExchangeSegment.Equity,
                LotSize = 1,
                TickSize = 0.05m,
                LastPrice = price,
                ChangePercent = chg,
                IsFnoEligible = false
            });
        }
    }
}
