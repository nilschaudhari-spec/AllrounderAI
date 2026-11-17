# ALL ROUNDER AI – REST API & REAL-TIME WEBSOCKET/SIGNALR SPECIFICATION

**API Version:** v1.0  
**Base URL:** `https://api.allrounder-ai.com/api/v1` (Local: `http://localhost:5000/api/v1`)  
**Real-Time Hub:** `ws://localhost:5000/hubs/market-signals`  
**Authentication:** Bearer JWT Token (`Authorization: Bearer <token>`)  
**Default Content-Type:** `application/json`  

---

## 1. Authentication & User Management

### 1.1 User Login
- **Endpoint:** `POST /auth/login`
- **Request Body:**
```json
{
  "email": "trader@allrounder.ai",
  "password": "SecurePassword123!"
}
```
- **Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2026-11-18T17:30:00Z",
  "user": {
    "userId": "c3a9f5d2-8b1e-4c7a-9a2f-1e4b8c9d0e1f",
    "email": "trader@allrounder.ai",
    "fullName": "Nilesh Trader",
    "role": "User"
  }
}
```

---

## 2. Market Data Endpoints

### 2.1 Get Instruments Master
- **Endpoint:** `GET /market/instruments`
- **Query Params:**
  - `exchange` (optional): `NSE`, `BSE`, `MCX`
  - `segment` (optional): `EQUITY`, `FUTURES`, `OPTIONS`, `INDEX`
  - `isFno` (optional): `true`, `false`
- **Response (200 OK):**
```json
[
  {
    "instrumentId": "1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d",
    "exchange": "NSE",
    "symbol": "NIFTY",
    "tradingSymbol": "NIFTY 50",
    "name": "Nifty 50 Index",
    "segment": "INDEX",
    "instrumentType": "INDEX",
    "lotSize": 50,
    "tickSize": 0.05,
    "isFnoEligible": true,
    "lastPrice": 24350.25,
    "changePercent": 0.45
  }
]
```

### 2.2 Get Historical OHLCV Candles
- **Endpoint:** `GET /market/candles`
- **Query Params:**
  - `symbol`: e.g. `NIFTY 50`, `RELIANCE`
  - `timeframe`: `1m`, `3m`, `5m`, `15m`, `30m`, `1h`, `D`
  - `limit`: Default `200`, max `1000`
- **Response (200 OK):**
```json
{
  "symbol": "NIFTY 50",
  "timeframe": "5m",
  "candles": [
    {
      "timestamp": "2026-11-17T09:15:00+05:30",
      "open": 24310.0,
      "high": 24345.5,
      "low": 24305.2,
      "close": 24340.0,
      "volume": 285400,
      "openInterest": 1254000
    }
  ]
}
```

### 2.3 Get Option Chain with Greeks and OI Analysis
- **Endpoint:** `GET /options/chain`
- **Query Params:**
  - `underlying`: `BANKNIFTY`, `NIFTY`
  - `expiry`: `2026-11-26`
- **Response (200 OK):**
```json
{
  "underlying": "BANKNIFTY",
  "underlyingPrice": 51240.0,
  "expiryDate": "2026-11-26",
  "strikes": [
    {
      "strikePrice": 51200.0,
      "call": {
        "tradingSymbol": "BANKNIFTY26NOV51200CE",
        "ltp": 245.5,
        "oi": 142000,
        "changeOi": 15400,
        "impliedVolatility": 14.2,
        "delta": 0.52
      },
      "put": {
        "tradingSymbol": "BANKNIFTY26NOV51200PE",
        "ltp": 205.0,
        "oi": 168000,
        "changeOi": -4200,
        "impliedVolatility": 14.8,
        "delta": -0.48
      }
    }
  ]
}
```

---

## 3. Market Scanner & Strategy Engine

### 3.1 Execute Multi-Timeframe Strategy Scan
- **Endpoint:** `POST /scanner/run`
- **Request Body:**
```json
{
  "marketScope": "FNO", // "CASH", "FNO", "COMMODITY", "ALL"
  "timeframe": "5m",
  "strategies": [
    "DAILY_INTRADAY_5M",
    "LOW_BREAK_CLOSE_UPSIDE",
    "GOLDEN_CROSS",
    "HAMMER"
  ],
  "confirmationMode": "CANDLE_CLOSE", // or "INTRABAR_EARLY"
  "minQualityScore": 75.0
}
```
- **Response (200 OK):**
```json
{
  "scanId": "scan-9f8e-4a2b",
  "totalScanned": 184,
  "matchesCount": 3,
  "matches": [
    {
      "signalId": "sig-01-banknifty",
      "symbol": "BANKNIFTY",
      "strategyId": "DAILY_INTRADAY_5M",
      "signalType": "BUY",
      "timeframe": "5m",
      "entryPrice": 51240.0,
      "targetPrice": 51280.0,
      "stopLossPrice": 51200.0,
      "riskRewardRatio": 1.0,
      "qualityScore": 92.5,
      "analysisSummary": "Supertrend(20,2) turned Bullish, VWAP support confirmed (Price 51240 > VWAP 51210), MACD bullish crossover on 5m chart. Strict 1:1 RR applied."
    }
  ]
}
```

---

## 4. Live Signals & Detailed Technical Analysis

### 4.1 Get Signal Detailed Analysis
- **Endpoint:** `GET /signals/{signalId}`
- **Response (200 OK):**
```json
{
  "signalId": "sig-01-banknifty",
  "tradingSymbol": "BANKNIFTY",
  "exchange": "NSE",
  "strategyName": "Daily Intraday Technique (5-Minute)",
  "pptSlideReference": "Slides 59-62",
  "signalType": "BUY",
  "status": "CONFIRMED",
  "currentPrice": 51240.0,
  "entryPrice": 51240.0,
  "targetPrice": 51280.0,
  "stopLossPrice": 51200.0,
  "riskRewardRatio": 1.0,
  "qualityScore": 92.5,
  "conditions": [
    {
      "conditionName": "Supertrend (20, 2) Direction",
      "isSatisfied": true,
      "expectedValue": "Bullish / Green",
      "actualValue": "Bullish (Value: 51180.5)",
      "notes": "Confirmed prior trend reversal"
    },
    {
      "conditionName": "VWAP Trend Position",
      "isSatisfied": true,
      "expectedValue": "Price > VWAP",
      "actualValue": "Price 51240.0 > VWAP 51210.0",
      "notes": "Positive institutional volume accumulation"
    },
    {
      "conditionName": "MACD Crossover",
      "isSatisfied": true,
      "expectedValue": "MACD > Signal Line",
      "actualValue": "MACD: 14.2 > Signal: 9.8",
      "notes": "Concurrent momentum confirmation"
    },
    {
      "conditionName": "Candle Body Strength (Slide 62)",
      "isSatisfied": true,
      "expectedValue": "Body >= 0.5 * ATR",
      "actualValue": "Body: 32 pts >= 18 pts threshold",
      "notes": "Candle is strong, not weak or doji"
    }
  ],
  "recommendation": "Execute 1 Lot Future or ATM Call Option (Strike 51200 CE) with 1:1 Target (40 pts) and Stop Loss (40 pts)."
}
```

---

## 5. Historical Backtesting API

### 5.1 Run Historical Backtest
- **Endpoint:** `POST /backtest/execute`
- **Request Body:**
```json
{
  "strategyId": "DAILY_INTRADAY_5M",
  "symbol": "NIFTY 50",
  "timeframe": "5m",
  "startDate": "2026-01-01",
  "endDate": "2026-11-15",
  "initialCapital": 100000.0,
  "riskRewardRatio": 1.0,
  "slippagePercent": 0.05,
  "brokeragePerTrade": 20.0,
  "parameters": {
    "supertrendLength": 20,
    "supertrendMultiplier": 2.0,
    "macdFast": 12,
    "macdSlow": 26,
    "macdSignal": 9
  }
}
```
- **Response (200 OK):**
```json
{
  "backtestId": "bt-7a2e-8f9c",
  "summary": {
    "totalTrades": 312,
    "winningTrades": 194,
    "losingTrades": 118,
    "winRate": 62.18,
    "totalNetPnl": 148520.0,
    "profitFactor": 1.64,
    "maxDrawdown": 18400.0,
    "maxDrawdownPercent": 12.4,
    "sharpeRatio": 1.82
  },
  "trades": [
    {
      "tradeId": 1,
      "entryTime": "2026-01-02T09:45:00+05:30",
      "exitTime": "2026-01-02T10:25:00+05:30",
      "side": "BUY",
      "entryPrice": 21850.0,
      "exitPrice": 21890.0,
      "pnl": 2000.0,
      "exitReason": "TARGET_HIT"
    }
  ]
}
```

---

## 6. Paper Trading & FrontPage Integration

### 6.1 Get Paper Portfolio
- **Endpoint:** `GET /papertrading/portfolio`
- **Response (200 OK):**
```json
{
  "accountId": "acc-fp-01",
  "provider": "FRONTPAGE",
  "availableMargin": 894250.0,
  "realizedPnl": 42150.0,
  "unrealizedPnl": 3400.0,
  "activePositions": [
    {
      "tradingSymbol": "BANKNIFTY26NOV51200CE",
      "quantity": 30,
      "buyPrice": 245.5,
      "currentPrice": 258.0,
      "unrealizedPnl": 375.0
    }
  ]
}
```

### 6.2 Toggle Auto-Trade Mode
- **Endpoint:** `POST /papertrading/auto-trade/toggle`
- **Request Body:**
```json
{
  "enabled": true,
  "maxDailyLossLimit": 10000.0,
  "maxConcurrentPositions": 3,
  "allowedStrategies": ["DAILY_INTRADAY_5M", "LOW_BREAK_CLOSE_UPSIDE", "GOLDEN_CROSS"]
}
```

---

## 7. Real-Time SignalR Hub Specification

**Hub Route:** `/hubs/market-signals`

### Client-to-Server Methods:
- `SubscribeToInstruments(string[] symbols)`: Subscribes mobile app to live ticks for specific instruments.
- `SubscribeToStrategySignals(string strategyId, string timeframe)`: Subscribes to live signals generated by scanner.

### Server-to-Client Invocations:
- `ReceiveLiveSignal(SignalDto signal)`: Dispatched immediately when a new pattern or setup matches in real time.
- `SignalStatusChanged(string signalId, string newStatus, double currentPrice)`: Dispatched when target or stop loss is hit.
- `MarketTickUpdate(TickDto tick)`: Broadcasts real-time LTP, Volume, and Change.
- `PaperOrderFilled(PaperOrderDto order)`: Dispatched when an automated paper order fills in FrontPage.
