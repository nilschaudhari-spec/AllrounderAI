# ALL ROUNDER AI – PHASE 2: SYSTEM ARCHITECTURE SPECIFICATION

**Document Version:** 1.0.0  
**Project Name:** ALL ROUNDER AI – Stock Market Strategy Scanner & Automated Paper Trading Platform  
**Target Runtime:** Android 8.0+ (API 26+) Client / ASP.NET Core (.NET 10) Server  
**Architecture Style:** Modular Clean Architecture (Client) + Micro-Service / Modular Monolith (Backend)  

---

## 1. System Architecture Overview

The system is split into two robust, independent, yet tightly synchronized components:
1. **ALL ROUNDER AI Mobile Terminal (Android):** Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Room local database, Retrofit/OkHttp, and SignalR Client.
2. **ALL ROUNDER AI Backend & Scanner Engine:** ASP.NET Core Web API (.NET 10), PostgreSQL database, Redis distributed cache, SignalR real-time hubs, and Background Hosted Scanner Services.

```mermaid
flowchart TD
    subgraph MarketData [Market Data Feeds]
        MD1[NSE Feed Adapter]
        MD2[BSE Feed Adapter]
        MD3[MCX Feed Adapter]
    end

    subgraph Backend [.NET 10 Backend & Engine Server]
        Ingest[Market Data Ingestion Service]
        Redis[(Redis Cache - Live Ticks & Order Books)]
        Scanner[Multi-Timeframe Strategy Scanner Worker]
        StrategyEng[Strategy Engine: Candlestick, Indicator, Options]
        BacktestEng[Historical Backtesting Engine]
        SignalHub[SignalR Real-Time Broadcasting Hub]
        PaperEng[Paper Trading Broker & Auto-Trade Engine]
        PgSql[(PostgreSQL Database)]
        RestApi[ASP.NET Core REST API Gateway]
    end

    subgraph Client [Android Client - Jetpack Compose]
        Room[(Room Offline DB)]
        Repo[Data Repository Layer]
        VM[ViewModel Layer]
        UI[Material 3 Compose UI Screens]
        SigClient[SignalR Live Hub Client]
    end

    subgraph Integrations [External Paper Trading]
        FP[FrontPage Paper Trading Adapter]
    end

    MarketData --> Ingest
    Ingest --> Redis
    Redis --> Scanner
    Scanner --> StrategyEng
    StrategyEng --> PgSql
    StrategyEng --> SignalHub
    SignalHub --> SigClient
    SigClient --> Repo
    RestApi --> Repo
    Repo --> Room
    Repo --> VM
    VM --> UI

    Scanner -.->|Auto-Trade Fill| PaperEng
    PaperEng --> FP
    PaperEng --> PgSql
    BacktestEng --> PgSql
    RestApi --> BacktestEng
```

---

## 2. Component Design & Clean Architecture

### 2.1 Android Client Clean Architecture
```
com.allrounder.ai/
├── app/                  # Application class, Hilt dependency injection modules
├── core/
│   ├── common/           # Result wrapper, AppConstants, Extensions, TimeUtils
│   ├── network/          # Retrofit clients, SignalR manager, Interceptors, Auth
│   ├── database/         # Room Database, DAOs, Entity definitions, Converters
│   └── designsystem/     # Theme, Color, Typography, Custom Compose Components
├── domain/
│   ├── model/            # Domain models (Instrument, Candle, Signal, Strategy, Order)
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Pure business logic UseCases (ScanMarket, RunBacktest, etc.)
└── presentation/
    ├── home/             # Home Dashboard screen & ViewModel
    ├── scanner/          # NSE/BSE/MCX/F&O Scanner screen & ViewModel
    ├── signals/          # Live Buy/Sell signals & Detailed Analysis screen
    ├── chart/            # Interactive TradingView-style candlestick chart screen
    ├── backtest/         # Backtesting dashboard & results screen
    ├── options/          # Option Chain & Strategy Builder screen
    └── papertrading/     # FrontPage paper trading & portfolio screen
```

### 2.2 Backend (.NET 10) Clean Architecture
```
AllRounderAi/
├── src/
│   ├── AllRounderAi.Domain/         # Core Domain Entities, Enums, Value Objects
│   ├── AllRounderAi.Application/    # Interfaces, DTOs, Business Logic, MediatR
│   ├── AllRounderAi.Infrastructure/ # EF Core, PostgreSQL, Redis, Feed Adapters, FrontPage
│   ├── AllRounderAi.Engine/         # Strategy Algorithms, Indicators, Scanner, Backtester
│   └── AllRounderAi.WebApi/         # Controllers, SignalR Hubs, Background Workers
└── tests/
    ├── AllRounderAi.Tests.Unit/     # Strategy engine mathematical unit tests
    └── AllRounderAi.Tests.Backtest/ # Historical backtesting verification tests
```

---

## 3. Signal Lifecycle & State Machine

Every signal identified by the Scanner passes through a deterministic state machine:

```mermaid
stateDiagram-v2
    [*] --> Detected : Strategy Pattern Matched
    Detected --> EarlyIntrabar : Intrabar Mode Active (30s before close)
    Detected --> Confirmed : Candle Close Formed & Confirmed
    EarlyIntrabar --> Confirmed : Candle Closes Validating Setup
    EarlyIntrabar --> Cancelled : Candle Closes Invalidating Setup
    Confirmed --> ExecutedPaper : Auto-Paper Trade Triggered
    Confirmed --> Expired : Price breaches invalidation level before fill
    ExecutedPaper --> TargetHit : Target 1:1 reached
    ExecutedPaper --> StopLossHit : Stop Loss reached
    ExecutedPaper --> ManualExit : User closed trade
    TargetHit --> [*]
    StopLossHit --> [*]
    ManualExit --> [*]
    Expired --> [*]
    Cancelled --> [*]
```

---

## 4. Market Scanner & Ingestion Architecture

### Separate Scopes: Cash vs F&O
1. **Cash Market Scanner:**
   - Evaluates all NIFTY 500, BSE 500, Small/Mid-cap liquid instruments.
   - Filters: Minimum turnover (INR 5 Cr/day), Volume > 100k, Spread < 0.15%.
   - Candlestick body rule configured in percentage ($0.2\% \text{ to } 0.8\%$).
2. **F&O Market Scanner:**
   - Evaluates NIFTY, BANK NIFTY, FINNIFTY, MIDCPNIFTY, and ~180 F&O stock futures.
   - Evaluates Option strikes (ATM $\pm$ 5 strikes) with high Open Interest.
   - Strict 7–10 point rule applied for NIFTY / BANK NIFTY indices.
   - Applies Protective Put / Protective Call strategies based on macro trend and OI buildup.

---

## 5. FrontPage Paper Trading Integration Architecture

FrontPage is India's leading paper trading and community platform.
To ensure robustness without violating terms or brittle automations:
- **Adapter Interface (`IPaperTradingProvider`):** Standard broker contract.
- **Provider Implementations:**
  1. `FrontPageApiProvider`: Connects via user-authorized session tokens or Webhook API if provisioned.
  2. `InternalSimulatedBroker`: Ultra-high performance zero-latency simulated broker providing identical order book matching, slippage, and brokerage calculation.
- **Auto-Trade Safeguards:**
  - Idempotency keys generated per signal ($ID = \text{SignalId} + \text{Timestamp}$).
  - Maximum daily risk limit (default 2% of paper capital).
  - Maximum concurrent open positions (default 5 positions).
  - No duplicate orders per instrument in the same direction.

---

## 6. Historical Backtesting Architecture

```mermaid
sequenceDiagram
    autonumber
    actor User as Android User
    participant API as Backtest API
    participant Engine as Backtest Engine
    participant DB as Historical Database
    participant Metric as Analytics Engine

    User->>API: Configure Backtest (Strategy, Instrument, 1m/5m/D, Date Range, 1:1 RR)
    API->>Engine: Initialize Simulation Context
    Engine->>DB: Stream Historical OHLCV Candles
    loop For Every Historical Candle
        Engine->>Engine: Update Moving Averages, Supertrend, MACD, Bands
        Engine->>Engine: Evaluate Strategy Matching Rules (Strictly Point-in-Time)
        alt Signal Generated & No Open Position
            Engine->>Engine: Enter Simulated Trade (Deduct Brokerage/Slippage)
        else In Position
            Engine->>Engine: Check Target 1:1 or Stop Loss
            alt Target Hit or Stop Loss Hit
                Engine->>Engine: Close Position & Record P&L
            end
        end
    end
    Engine->>Metric: Compute Metrics (Win Rate, Profit Factor, Drawdown, Sharpe)
    Metric-->>API: Return Backtest Summary & Trades Log
    API-->>User: Display Interactive Visual Results & Equity Curve
```

---

## 7. Security, Reliability & Compliance
- **No Real Capital Risk:** Built strictly as a simulated paper trading and technical analysis decision-support tool.
- **Timezone Synchronization:** All timestamps normalized to `Asia/Kolkata` (IST, UTC+5:30).
- **Exchange Holidays:** Dedicated Indian market calendar database table preventing false scanning signals on holidays or outside trading hours (09:15 to 15:30 IST).
