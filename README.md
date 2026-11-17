# ALL ROUNDER AI – STOCK MARKET STRATEGY SCANNER & AUTO PAPER TRADING

**All Rounder AI** is a production-grade algorithmic stock market scanner, technical analysis decision-support platform, and automated paper trading system. It formalizes and automates all trading strategies, candlestick patterns, price action setups, indicator combinations, and options structures from the reference presentation **All Rounder 2 (October Edition).pptx** (69 Slides).

---

## 1. Supported Markets & Instruments
- **NSE (National Stock Exchange):** Equities (NIFTY 50, NIFTY 500), Index Futures & Options, Stock Futures & Options.
- **BSE (Bombay Stock Exchange):** SENSEX, Equities.
- **MCX (Multi Commodity Exchange):** Gold, Silver, Crude Oil, Natural Gas.
- **Indices:** NIFTY 50, BANK NIFTY, FINNIFTY, MIDCPNIFTY, SENSEX.

---

## 2. Strategies Implemented from "All Rounder 2 (October Edition).pptx"

### A. Candlestick Patterns
1. **Hammer** (*Slides 8–10*): Buy signal at bottom of downtrend ($LowerWick \ge 2 \times Body$).
2. **Inverse Hammer** (*Slides 11–12*): Buy signal with long upper shadow.
3. **Low Break, Close Upside** (*Slides 13–14*): Previous red candle low broken, green candle closes upside; strictly satisfies the PPT **7–10 point rule** (configurable per instrument: points for indices, percentage for equities).
4. **Bullish Harami** (*Slides 15–16*): Small green candle inside prior long red body.
5. **Hanging Man** (*Slides 17–18*): Bearish sell signal at top of uptrend.
6. **Shooting Star** (*Slides 19–20*): Bearish sell signal with long upper rejection wick.
7. **High Break, Close Downside** (*Slides 21–22*): Previous green candle high broken, red candle closes downside with **7–10 point rule**.
8. **Bearish Harami** (*Slides 23–24*): Small red candle inside prior long green body.
9. **Bullish & Bearish Marubozu** (*Slides 25–26*): Full body momentum candles with minimal/no wicks ($\le 5\%$).
10. **Doji Formations** (*Slides 27–29*): Indecision and reversal alerts.
11. **Morning Star** (*Slides 40–41*): 3-candle bullish reversal closing $>50\%$ into first red body.
12. **Evening Star** (*Slides 42–43*): 3-candle bearish reversal closing $>50\%$ into first green body.

### B. Indicator Strategies
1. **Bollinger Bands** (*Slides 44–49*):
   - **Median Line Reversal:** Trend switch trigger.
   - **Call Buying:** Upper line breakout/touch with strong green candle.
   - **Put Buying:** Lower line breakdown/touch with strong red candle.
2. **Golden Cross & Re-entry Strategy** (*Slides 50–58*):
   - **Golden Cross:** EMA 50 (Blue line) crosses above EMA 200 (Red line) $\implies$ Buy.
   - **Death Cross:** EMA 50 crosses below EMA 200 $\implies$ Sell.
   - **Re-entry Opportunity (Slides 55–58):** In established uptrend, price pulls back to touch dynamic support at EMA 200 Red Line and prints a bullish reaction candle $\implies$ Re-entry Buy!
3. **Daily Intraday Technique** (*Slides 59–62*):
   - **Timeframe:** Strict **5-Minute Chart**.
   - **Triple Concurrence:** Supertrend (Length 20, Multiplier 2.0) + VWAP Support + MACD (12, 26, 9) crossover.
   - **Execution Rules (Slide 62):** Strong candle body (not weak/doji), enter on or 30s before candle close, **Strict 1:1 Target to Stop-Loss Ratio**.

### C. Options Hedged Strategies
1. **Protective Put** (*Slide 68 - Uptrend Strategy*):
   - Buy 1 Lot BANKNIFTY Future + Buy 1 Lot BANKNIFTY ATM Put.
   - Limited Max Loss: $(Future Entry - Strike Price) + Put Premium$. Unlimited upside.
2. **Protective Call** (*Slide 69 - Downtrend Strategy*):
   - Short Sell 1 Lot BANKNIFTY Future + Buy 1 Lot BANKNIFTY ATM Call.
   - Limited Max Loss: $(Strike Price - Future Short Price) + Call Premium$.

---

## 3. Five Confirmed User Decisions (Phase 1 Baseline)
1. **7–10 Point Rule:** Configurable per instrument (point thresholds for indices, percentage thresholds for equities).
2. **Dual Signal Confirmation:** Supports both Candle-Close mode and Intrabar (early entry) mode.
3. **MACD Parameters:** Fully configurable (Fast, Slow, Signal).
4. **Bollinger Bands Parameters:** Fully configurable (Period, Standard Deviation).
5. **Separate Market Scanners:** Dedicated independent scanners for Cash (Equities) and F&O (Futures & Options).

---

## 4. Project Structure
```
AI Stocks Scanner/
├── All Rounder 2 (October Edition).pptx  # Original Reference Presentation (69 Slides)
├── docs/
│   ├── PHASE_1_STRATEGY_SPECIFICATION.md # Full Slide Mapping & Mathematical Specs
│   ├── PHASE_2_SYSTEM_ARCHITECTURE.md    # Architecture & Component Specs
│   ├── API_SPECIFICATION.md              # REST & SignalR WebSocket API Specs
│   └── P0_P1_P2_TASK_REGISTER.md         # Priority Task & Ambiguity Register
├── database/
│   └── DATABASE_SCHEMA.sql               # Production PostgreSQL Schema DDL
├── backend/                              # ASP.NET Core (.NET 10) Solution
│   ├── AllRounderAi.sln
│   ├── src/
│   │   ├── AllRounderAi.Core/            # Indicators, Strategy Engines, Scanner, Backtesting
│   │   └── AllRounderAi.WebApi/          # Controllers, SignalR Hubs, Endpoints
│   └── tests/
│       └── AllRounderAi.Tests/           # Unit Tests verifying all PPT strategies
└── android/                              # Kotlin / Jetpack Compose App
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── app/
        ├── build.gradle.kts
        └── src/main/java/com/allrounder/ai/
            ├── core/database/            # Room Database Entities & DAOs
            ├── data/network/             # Retrofit & SignalR Client
            ├── domain/model/             # Domain Entities
            └── ui/screens/               # Jetpack Compose Screens (Home, Signals, Chart, Options, etc.)
```

---

## 5. How to Run & Verify

### Running Backend & Tests (.NET 10)
```bash
cd backend
# Run all verified unit tests (100% passing)
dotnet test

# Run WebApi server
dotnet run --project src/AllRounderAi.WebApi/AllRounderAi.WebApi.csproj
```
The WebApi starts at `http://localhost:5000` with Swagger and SignalR Hub available.

### Running Android Application
Open the `android/` directory in Android Studio (Hedgehog / Iguana / Jellyfish / Ladybug), sync Gradle, and run on any Android device or emulator running Android 8.0+ (API 26+).
