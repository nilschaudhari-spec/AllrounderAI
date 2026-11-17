# ALL ROUNDER AI – P0 / P1 / P2 TASK & AMBIGUITY REGISTER

**Document Version:** 1.0.0  
**Project:** ALL ROUNDER AI – Stock Market Strategy Scanner & Automated Paper Trading Platform  

---

## 1. P0 Tasks: Critical Path, Ambiguities & Safeguards

| Item ID | Classification | Description & PPT Reference | Formal Resolution & Safeguard Implementation | Status |
|:---|:---|:---|:---|:---|
| **P0-01** | **Safety Safeguard** | Live Money Execution Risk | The application must NEVER execute real-money trades. The order execution layer is architecturally locked to simulated broker engines and authorized paper-trading platforms (FrontPage). Live broker APIs (e.g. Zerodha Kite, Upstox, AngelOne live order placement) are strictly excluded from order routing pipelines. | **RESOLVED** |
| **P0-02** | **Rule Ambiguity** | 7–10 Point Candle Body Rule (Slides 13, 21) | In the PPT, "7 to 10 points difference between open and close" was designed around NIFTY/BANKNIFTY index scale. Applying 7-10 points to a ₹150 stock or a ₹4000 stock would break. **Resolution:** Configurable per instrument with point mode for indices (7–10 pts default) and percentage mode for equities (0.2%–0.5% default). | **RESOLVED** |
| **P0-03** | **Integration** | FrontPage Paper Trading API Availability | FrontPage does not publish a public OAuth2 trading API. **Resolution:** Implemented an adapter pattern (`IPaperTradingProvider`) with `InternalSimulatedBroker` providing full realistic matching, slippage, and brokerage, with configurable Webhook / Session integration for FrontPage accounts. | **RESOLVED** |
| **P0-04** | **Data Integrity** | Backtesting Look-Ahead Bias | Indicator calculations must never peek at current candle close during intrabar evaluation or future candles. **Resolution:** Indicators computed sequentially using strict rolling window up to $t-1$ for signals or strictly point-in-time candle close data. | **RESOLVED** |
| **P0-05** | **Execution Order** | Daily Intraday 1:1 Target & Stop Loss (Slide 62) | If high and low of a candle touch both target and stop-loss within the same bar, ambiguity arises. **Resolution:** Conservative backtest assumption: if both hit in the same bar, assume Stop Loss hit first (pessimistic bias) unless tick-level data proves target hit first. | **RESOLVED** |

---

## 2. P1 Tasks: Core Functional Engines

| Item ID | Component | PPT Slide Reference | Specification & Implementation Deliverable | Status |
|:---|:---|:---|:---|:---|
| **P1-01** | Candlestick Engine | Slides 8–29, 40–43 | Implement exact geometric ratio algorithms for all 13 patterns (Hammer, Inv Hammer, Low Break Close Upside, Bullish Harami, Hanging Man, Shooting Star, High Break Close Downside, Bearish Harami, Marubozu, Doji, Morning Star, Evening Star). | **COMPLETE** |
| **P1-02** | Golden Cross & Re-entry Engine | Slides 50–58 | Implement EMA 50 / 200 crossover logic and dynamic red line (EMA 200) support/resistance re-entry bounce detection. | **COMPLETE** |
| **P1-03** | Daily Intraday 5M Strategy | Slides 59–62 | Implement 5-minute multi-indicator concurrence: Supertrend(20,2), VWAP position, and MACD crossover, with 1:1 RR target/SL generator. | **COMPLETE** |
| **P1-04** | Options Hedged Engine | Slides 63–69 | Implement Protective Put (Long Future + ATM Put) and Protective Call (Short Future + ATM Call) payoff curves, max profit, max loss, and breakeven formulas. | **COMPLETE** |
| **P1-05** | Multi-Market Scanner | Prompt Section 6 | Implement multi-timeframe scanner for NSE, BSE, MCX, Indices, Cash and F&O instruments with separate liquidity filters. | **COMPLETE** |
| **P1-06** | Signal Detailed Analysis | Prompt Section 7 | Provide 10-point checklist breakdown for every signal showing matched vs failed conditions. | **COMPLETE** |
| **P1-07** | Historical Backtester | Prompt Section 10 | Complete multi-year backtesting engine outputting Win Rate, Net P&L, Profit Factor, Max Drawdown, and Sharpe Ratio. | **COMPLETE** |

---

## 3. P2 Tasks: User Experience, Telemetry & Refinements

| Item ID | Component | Description | Implementation Deliverable | Status |
|:---|:---|:---|:---|:---|
| **P2-01** | Interactive Chart | TradingView-style candlestick chart with zoom, pan, crosshair, and indicator overlays in Jetpack Compose. | Compose Canvas chart component with touch gestures. | **COMPLETE** |
| **P2-02** | Real-Time Sync | SignalR real-time websocket broadcasting for zero-latency mobile notifications. | ASP.NET Core SignalR Hub + Android SignalR client. | **COMPLETE** |
| **P2-03** | Offline Persistence | Offline caching of scanned instruments, signals, and active paper positions. | Android Room Database with Flow reactive queries. | **COMPLETE** |
| **P2-04** | Theme & Audio Alerts | Material 3 Dark & Light theme with sound alerts on high-quality signal generation. | Jetpack Compose theme + Android MediaPlayer service. | **COMPLETE** |
