# ALL ROUNDER AI – PHASE 1: FORMAL STRATEGY SPECIFICATION & PPT MAPPING

**Document Version:** 1.0.0  
**Reference Document:** `All Rounder 2 (October Edition).pptx` (69 Slides Total)  
**Target Platform:** ALL ROUNDER AI – Stock Market Strategy Scanner & Automated Paper Trading Platform  
**Market Coverage:** NSE (Equities & F&O), BSE (Equities), MCX (Commodities), Indices (NIFTY 50, BANK NIFTY, FINNIFTY, SENSEX, etc.)  

---

## 1. Executive Summary & Baseline Decisions

This document formalizes every trading strategy, technical pattern, indicator setup, and options methodology contained within the 69 slides of the **All Rounder 2 (October Edition)** presentation. 

### Confirmed User Decisions (Phase 1 Baseline)
1. **7–10 Point Rule:** Configurable per instrument (both absolute point difference and percentage difference for varying price scales).
2. **Signal Confirmation Modes:** Dual configurable modes supported per strategy:
   - **Confirmed Mode:** Wait for current candle close (plus optional next-candle confirmation).
   - **Intrabar / Early Mode:** Trigger before candle closing (e.g., 30 seconds before candle close as per Slide 62).
3. **MACD Parameters:** Fully configurable (Default: Fast EMA = 12, Slow EMA = 26, Signal SMA = 9).
4. **Bollinger Bands Parameters:** Fully configurable (Default: Period = 20, Standard Deviation = 2.0).
5. **Scanning Scope:** Dual independent scanners for **Cash (Equity)** and **F&O (Futures & Options)** with dedicated risk, liquidity, and eligibility filters.

---

## 2. PPT Slide-by-Slide Inventory & Mapping

| Slide Range | Topic / Strategy | Type | PPT Direction | Automated Action |
|:---|:---|:---|:---|:---|
| **Slides 1–7** | Technical Analysis Basics, Trends, Timeframes | Education & Foundations | Neutral | Framework Setup |
| **Slides 8–10** | **Hammer Pattern** | Candlestick | Bullish (Buy) | Long Entry Engine |
| **Slides 11–12** | **Inverse Hammer Pattern** | Candlestick | Bullish (Buy) | Long Entry Engine |
| **Slides 13–14** | **Low Break, Close Upside** (7–10 pt rule) | Proprietary Price Action | Bullish (Buy) | Reversal Buy Engine |
| **Slides 15–16** | **Bullish Harami** | Candlestick (2-Candle) | Bullish (Buy) | Long Entry Engine |
| **Slides 17–18** | **Hanging Man Pattern** | Candlestick | Bearish (Sell) | Short Entry Engine |
| **Slides 19–20** | **Shooting Star Pattern** | Candlestick | Bearish (Sell) | Short Entry Engine |
| **Slides 21–22** | **High Break, Close Downside** (7–10 pt rule)| Proprietary Price Action | Bearish (Sell) | Reversal Sell Engine |
| **Slides 23–24** | **Bearish Harami** | Candlestick (2-Candle) | Bearish (Sell) | Short Entry Engine |
| **Slides 25–26** | **Bullish & Bearish Marubozu** | Candlestick (Momentum) | Buy / Sell | Momentum Entry Engine |
| **Slides 27–29** | **Doji Formations** (Standard, Dragonfly, Gravestone, Long-legged) | Candlestick | Indecision / Neutral | Confirmation Filter |
| **Slides 30–39** | **Price Action, Trendlines & Consolidation** | Price Action Formations | Trend Follow | Support/Resistance & Breakout Filter |
| **Slides 40–41** | **Morning Star** | Candlestick (3-Candle) | Bullish (Buy) | Multi-Candle Buy Engine |
| **Slides 42–43** | **Evening Star** | Candlestick (3-Candle) | Bearish (Sell) | Multi-Candle Sell Engine |
| **Slides 44–49** | **Bollinger Bands Strategy** | Indicator | Trend / Reversal | Call / Put Buying Engine |
| **Slides 50–58** | **Golden Cross & Re-entry Strategy** (EMA 50 / 200) | Indicator & Trend | Buy / Sell / Re-entry | Moving Average Engine |
| **Slides 59–62** | **Daily Intraday Technique** (Supertrend + VWAP + MACD) | Indicator Composite | Buy / Sell | 5-Minute Intraday Strategy |
| **Slides 63–66** | **Options Foundations, Open Interest (OI), Terminology** | Options Data | Directional / Volatility | OI Analytics & Payoffs |
| **Slides 67–68** | **Uptrend Strategy: Protective Put** (Future Long + ATM Put) | Options Strategy | Hedged Bullish | Auto-Hedged Order Engine |
| **Slides 69** | **Downtrend Strategy: Protective Call** (Future Short + ATM Call) | Options Strategy | Hedged Bearish | Auto-Hedged Order Engine |

---

## 3. Mathematical Strategy Specifications

### 3.1 Candlestick Pattern Engine

For candle $C_t$ at time $t$ with $O_t, H_t, L_t, C_t$:
- $\text{Body}_t = |C_t - O_t|$
- $\text{UpperWick}_t = H_t - \max(O_t, C_t)$
- $\text{LowerWick}_t = \min(O_t, C_t) - L_t$
- $\text{Range}_t = H_t - L_t$

#### 1. Hammer (Slides 8–10)
- **Preceding Trend:** Downward trend ($SMA_{20} < SMA_{50}$ or consecutive lower highs over $N$ candles, default $N=5$).
- **Candle Geometry:**
  1. $\text{LowerWick}_t \ge 2.0 \times \text{Body}_t$
  2. $\text{UpperWick}_t \le 0.1 \times \text{Range}_t$ (very small or zero upper wick)
  3. $\text{Body}_t > 0$
- **Signal:** BUY.
- **Entry:** Price breaks above Hammer $H_t$.
- **Stop Loss:** $L_t - \text{buffer}$ (default buffer = 0.05% of price).
- **Target:** 1:1 risk-to-reward ratio minimum (configurable).

#### 2. Inverse Hammer (Slides 11–12)
- **Preceding Trend:** Downward trend at support.
- **Candle Geometry:**
  1. $\text{UpperWick}_t \ge 2.0 \times \text{Body}_t$
  2. $\text{LowerWick}_t \le 0.1 \times \text{Range}_t$
  3. Small real body at the bottom of the candle range.
- **Signal:** BUY.
- **Confirmation:** Next candle closes above Inverse Hammer high $H_t$.

#### 3. Low Break, Close Upside (Slides 13–14)
- **Preceding Trend:** Reversal trend context required (prior downtrend).
- **Setup Candles:**
  - Candle $C_{t-1}$ is RED ($C_{t-1} < O_{t-1}$).
  - Candle $C_t$ is GREEN ($C_t > O_t$).
- **Trigger Conditions:**
  1. **Low Break:** $L_t < L_{t-1}$ (Candle $t$ breaks the low of previous red candle).
  2. **Close Upside:** $C_t > \min(O_{t-1}, C_{t-1})$ and closes in the upper half of the range.
  3. **Configurable 7–10 Point Rule:**
     $$\Delta_{\text{pts}} = |O_t - C_t| \text{ and } |O_{t-1} - C_{t-1}|$$
     For NIFTY/BANKNIFTY: $7 \le \text{Body} \le 10$ points (or instrument-configured threshold $K_{\text{min}} \le \text{Body} \le K_{\text{max}}$).
     For Equities: Normalized to percentage ($0.2\% \le \text{Body\%} \le 0.5\%$).
- **Signal:** BUY.

#### 4. Bullish Harami (Slides 15–16)
- **Preceding Trend:** Downtrend.
- **Setup Candles:**
  - Candle $C_{t-1}$: Long red candle ($\text{Body}_{t-1} \ge 1.5 \times \text{ATR}_{14}$).
  - Candle $C_t$: Small green candle contained entirely inside the body of $C_{t-1}$:
    $$O_t > C_{t-1} \quad \text{and} \quad C_t < O_{t-1}$$
    $$H_t < H_{t-1} \quad \text{and} \quad L_t > L_{t-1}$$
- **Signal:** BUY.

#### 5. Hanging Man (Slides 17–18)
- **Preceding Trend:** Strong uptrend ($SMA_{20} > SMA_{50}$).
- **Candle Geometry:**
  1. $\text{LowerWick}_t \ge 2.0 \times \text{Body}_t$
  2. $\text{UpperWick}_t \le 0.1 \times \text{Range}_t$
  3. Small body at the upper end of the candle.
- **Signal:** SELL / SHORT.
- **Confirmation:** Next candle closes below Hanging Man body.

#### 6. Shooting Star (Slides 19–20)
- **Preceding Trend:** Uptrend at resistance.
- **Candle Geometry:**
  1. $\text{UpperWick}_t \ge 2.0 \times \text{Body}_t$
  2. $\text{LowerWick}_t \le 0.1 \times \text{Range}_t$
  3. Small body at the bottom of the range.
- **Signal:** SELL / SHORT.

#### 7. High Break, Close Downside (Slides 21–22)
- **Preceding Trend:** Reversal trend context required (prior uptrend).
- **Setup Candles:**
  - Candle $C_{t-1}$ is GREEN ($C_{t-1} > O_{t-1}$).
  - Candle $C_t$ is RED ($C_t < O_t$).
- **Trigger Conditions:**
  1. **High Break:** $H_t > H_{t-1}$ (Candle $t$ breaks the high of previous green candle).
  2. **Close Downside:** $C_t < \min(O_{t-1}, C_{t-1})$ and closes in the lower half of the range.
  3. **Configurable 7–10 Point Rule:** Instrument-calibrated body threshold.
- **Signal:** SELL / SHORT.

#### 8. Bearish Harami (Slides 23–24)
- **Preceding Trend:** Uptrend.
- **Setup Candles:**
  - Candle $C_{t-1}$: Long green candle.
  - Candle $C_t$: Small red candle completely inside body of $C_{t-1}$:
    $$O_t < C_{t-1} \quad \text{and} \quad C_t > O_{t-1}$$
- **Signal:** SELL / SHORT.

#### 9. Bullish Marubozu & Bearish Marubozu (Slides 25–26)
- **Bullish Marubozu:**
  - Green candle with no or negligible wicks:
    $$\text{UpperWick}_t \le 0.02 \times \text{Range}_t \quad \text{and} \quad \text{LowerWick}_t \le 0.02 \times \text{Range}_t$$
  - $\text{Body}_t \ge 1.8 \times \text{ATR}_{14}$.
  - Signal: BUY.
- **Bearish Marubozu:**
  - Red candle with no or negligible wicks:
    $$\text{UpperWick}_t \le 0.02 \times \text{Range}_t \quad \text{and} \quad \text{LowerWick}_t \le 0.02 \times \text{Range}_t$$
  - $\text{Body}_t \ge 1.8 \times \text{ATR}_{14}$.
  - Signal: SELL.

#### 10. Doji (Slides 27–29)
- **Geometry:** $\text{Body}_t \le 0.1 \times \text{Range}_t$ (Open and Close almost equal).
- Subtypes: Standard Doji, Long-Legged Doji, Dragonfly Doji (no upper wick), Gravestone Doji (no lower wick).
- **Role:** Indecision marker. Used as an immediate reversal alert requiring next-candle confirmation.

#### 11. Morning Star (Slides 40–41)
- **Preceding Trend:** Downtrend.
- **Three-Candle Formation:**
  1. Candle $C_{t-2}$: Long bearish red candle.
  2. Candle $C_{t-1}$: Small body candle (star/doji) gapping down or opening below $C_{t-2}$ close.
  3. Candle $C_t$: Strong bullish green candle closing above $50\%$ of $C_{t-2}$ body:
     $$C_t > O_{t-2} - 0.5 \times (O_{t-2} - C_{t-2})$$
- **Signal:** BUY.

#### 12. Evening Star (Slides 42–43)
- **Preceding Trend:** Uptrend.
- **Three-Candle Formation:**
  1. Candle $C_{t-2}$: Long bullish green candle.
  2. Candle $C_{t-1}$: Small body candle gapping up.
  3. Candle $C_t$: Strong bearish red candle closing below $50\%$ of $C_{t-2}$ body.
- **Signal:** SELL.

---

### 3.2 Indicator Strategies

#### 1. Bollinger Bands Strategy (Slides 44–49)
- **Formula:**
  $$\text{Middle Band (MB)} = \text{SMA}_{20}(C)$$
  $$\text{Upper Band (UB)} = \text{MB} + 2.0 \times \sigma_{20}(C)$$
  $$\text{Lower Band (LB)} = \text{MB} - 2.0 \times \sigma_{20}(C)$$
- **Rules per PPT:**
  - **Median Line Reversal:** If candle passes through Median Line from below, uptrend continuation. If candle breaks below Median Line, downtrend continuation.
  - **Call Buying (Slide 49):** When candle touches or breaks Upper Line with strong body $\implies$ Strong Uptrend $\implies$ Buy Call Option / Buy Underlying.
  - **Put Buying (Slide 49):** When candle touches or breaks Lower Line with strong body $\implies$ Strong Downtrend $\implies$ Buy Put Option / Short Underlying.

#### 2. Golden Cross & Re-entry Strategy (Slides 50–58)
- **Indicators:** EMA 50 (Blue line) and EMA 200 (Red line).
- **Main Golden Cross (Buy):**
  - Trigger: EMA 50 crosses above EMA 200:
    $$\text{EMA}_{50, t-1} \le \text{EMA}_{200, t-1} \quad \text{and} \quad \text{EMA}_{50, t} > \text{EMA}_{200, t}$$
  - Signal: BUY.
- **Main Death Cross (Sell):**
  - Trigger: EMA 50 crosses below EMA 200:
    $$\text{EMA}_{50, t-1} \ge \text{EMA}_{200, t-1} \quad \text{and} \quad \text{EMA}_{50, t} < \text{EMA}_{200, t}$$
  - Signal: SELL.
- **Exit Rule (Slide 54):** Exit position when price reverses and closes against the moving average trend or re-crosses.
- **Re-entry Opportunity (Slides 55–58):**
  - Must occur *after* initial Golden Cross trade has completed.
  - **Bullish Re-entry:** In an established uptrend ($\text{EMA}_{50} > \text{EMA}_{200}$), price pulls back so that the candle low or shadow comes near or touches the EMA 200 red line (which acts as dynamic support) and forms a bullish reaction candle:
    $$L_t \le \text{EMA}_{200, t} \times 1.002 \quad \text{and} \quad C_t > \text{EMA}_{200, t}$$
    $$\implies \text{BUY (Re-entry)}$$
  - **Bearish Re-entry:** In an established downtrend ($\text{EMA}_{50} < \text{EMA}_{200}$), price bounces near or touches the EMA 200 red line (acting as dynamic resistance) and prints a rejection candle:
    $$H_t \ge \text{EMA}_{200, t} \times 0.998 \quad \text{and} \quad C_t < \text{EMA}_{200, t}$$
    $$\implies \text{SELL (Re-entry)}$$

#### 3. Daily Intraday Technique (Slides 59–62)
- **Instrument:** NIFTY 50 / Major Indices / Liquid Equities.
- **Timeframe:** Strict **5-Minute Chart**.
- **Indicator Set:**
  1. **Supertrend:** Length = 20, Multiplier / Factor = 2.0.
  2. **VWAP (Volume Weighted Average Price):**
     $$\text{VWAP}_t = \frac{\sum (P_i \times V_i)}{\sum V_i}$$
     - Uptrend: $C_t > \text{VWAP}_t$.
     - Downtrend: $C_t < \text{VWAP}_t$.
  3. **MACD:** Fast = 12, Slow = 26, Signal = 9.
     - MACD Line $= \text{EMA}_{12} - \text{EMA}_{26}$
     - Signal Line $= \text{EMA}_{9}(\text{MACD Line})$
     - Bullish: $\text{MACD Line} > \text{Signal Line}$ and Histogram $> 0$.
     - Bearish: $\text{MACD Line} < \text{Signal Line}$ and Histogram $< 0$.
- **Triple Confluence Buy Condition:**
  1. Supertrend turns Green / Bullish.
  2. Price closes above VWAP.
  3. MACD produces bullish crossover / confirmation at the same time.
  4. Candlestick body must NOT be weak/small ($\text{Body}_t \ge 0.5 \times \text{ATR}_{14}$).
  5. Trend must be reversal context, avoid sideways consolidation.
- **Execution Rules (Slide 62):**
  - **Entry Timing:** Enter on or before 30 seconds of candle closing (e.g., at 4m 30s into the 5-minute bar).
  - **Target & Stop Loss:** Strict **1:1 Ratio** (e.g., 20 points target $\implies$ 20 points stop-loss).
  - **Order Placement:** Target and Stop-Loss orders placed immediately upon trade fill.

---

### 3.3 Options Strategies Engine (Slides 63–69)

#### 1. Open Interest (OI) Analysis
- **Price Rising + OI Rising:** Long Buildup (Strong Bullish).
- **Price Falling + OI Rising:** Short Buildup (Strong Bearish).
- **Price Rising + OI Falling:** Short Covering (Mild Bullish Reversal).
- **Price Falling + OI Falling:** Long Unwinding (Mild Bearish Reversal).

#### 2. Protective Put (Slide 68) – Uptrend Strategy
- **Market View:** Bullish, but protected against sudden crashes.
- **Leg 1:** Buy 1 Lot Underlying Future (e.g., BANKNIFTY Future at $P_{\text{fut}}$).
- **Leg 2:** Buy 1 Lot ATM Put Option (Strike $K_{\text{ATM}}$, Premium $V_p$).
- **Payoff Formulation:**
  $$\Pi(S_T) = (S_T - P_{\text{fut}}) + \max(0, K_{\text{ATM}} - S_T) - V_p$$
- **Max Loss:** $(P_{\text{fut}} - K_{\text{ATM}}) + V_p$ (Limited).
- **Max Profit:** Unlimited ($S_T \to \infty$).
- **Breakeven:** $P_{\text{fut}} + V_p$.

#### 3. Protective Call (Slide 69) – Downtrend Strategy
- **Market View:** Bearish, but protected against upside squeeze.
- **Leg 1:** Short Sell 1 Lot Underlying Future (Sell at $P_{\text{fut}}$).
- **Leg 2:** Buy 1 Lot ATM Call Option (Strike $K_{\text{ATM}}$, Premium $V_c$).
- **Payoff Formulation:**
  $$\Pi(S_T) = (P_{\text{fut}} - S_T) + \max(0, S_T - K_{\text{ATM}}) - V_c$$
- **Max Loss:** $(K_{\text{ATM}} - P_{\text{fut}}) + V_c$ (Limited).
- **Max Profit:** $P_{\text{fut}} - V_c$ (as $S_T \to 0$).
- **Breakeven:** $P_{\text{fut}} - V_c$.

#### 4. Additional Options Strategies Defined in Slide 66
- **Long Straddle:** Buy ATM Call + Buy ATM Put.
- **Short Straddle:** Sell ATM Call + Sell ATM Put.
- **Long Strangle:** Buy OTM Call + Buy OTM Put.
- **Short Strangle:** Sell OTM Call + Sell OTM Put.
- **Bull Call Spread:** Buy ITM/ATM Call + Sell OTM Call.
- **Bear Put Spread:** Buy ATM Put + Sell OTM Put.
