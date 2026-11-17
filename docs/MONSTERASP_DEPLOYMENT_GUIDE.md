# ALL ROUNDER AI – MONSTERASP.NET DEPLOYMENT GUIDE

**Target Host:** [MonsterASP.NET](https://monsterasp.net/)  
**Database:** PostgreSQL (`db80695.databaseasp.net:5432`, Database: `db80695`)  
**Application:** ALL ROUNDER AI Web API & SignalR Real-Time Hub  
**Published Packages:**
- `AllRounderAi_Api_MonsterASP_SelfContained.zip` *(Recommended – includes .NET 10 win-x64 runtime & PostgreSQL driver)*
- `AllRounderAi_Api_MonsterASP_FrameworkDependent.zip` *(Lightweight)*

---

## 1. Database Configuration
The application is pre-configured with your MonsterASP.NET PostgreSQL connection string in both `appsettings.json` and `appsettings.Production.json`:
```json
{
  "ConnectionStrings": {
    "DefaultConnection": "Server=db80695.databaseasp.net;Port=5432;Database=db80695;User Id=db80695;Password=6x?F!5Rd4e-D;"
  }
}
```

### Automatic Table Initialization
When the API boots up on MonsterASP.NET, a background task automatically connects to `db80695.databaseasp.net` and runs `CREATE TABLE IF NOT EXISTS` for all system tables:
- `users`, `exchanges`, `instruments`
- `historical_candles`, `strategies`, `trading_signals`
- `paper_accounts`, `paper_orders`, `backtest_results`
- Pre-seeds default exchanges: `NSE`, `BSE`, `MCX`.

---

## 2. Instant Deployment via MonsterASP.NET Control Panel

1. Log in to your [MonsterASP.NET Control Panel](https://monsterasp.net/).
2. Open your website (e.g., `allrounderai.runasp.net`).
3. Click **File Manager** on the left menu.
4. Navigate into the root web folder (or `site/wwwroot/`).
5. Delete any existing default files (e.g. `default.aspx`, `index.html`).
6. Click **Upload** and upload:
   ```text
   d:\Nilesh\Nilesh\AI Stocks Scanner\AllRounderAi_Api_MonsterASP_SelfContained.zip
   ```
7. Click the uploaded file and select **Extract**.
8. Verify that `AllRounderAi.WebApi.exe`, `web.config`, `Npgsql.dll`, and `appsettings.json` are present in the folder.
9. Visit your site URL in the browser (e.g., `http://your-site.runasp.net/`).

---

## 3. Verified Endpoints & Database Verification

| Endpoint | Method | Expected Output |
|:---|:---:|:---|
| `/` | `GET` | Health status + live PostgreSQL connection report |
| `/api/v1/database/status` | `GET` | `{"isConnected": true, "databaseName": "db80695", ...}` |
| `/api/v1/database/tables` | `GET` | Lists all created PostgreSQL tables |
| `/api/v1/database/init` | `POST` | Manually re-trigger schema creation and verification |
| `/api/v1/market/instruments` | `GET` | Active stock & index instruments |
| `/api/v1/signals/live` | `GET` | Live strategy signals (Daily Intraday 5M, Hammer, Low Break Close Upside) |
| `/api/v1/options/chain?underlying=BANKNIFTY` | `GET` | Full Option Chain with OI & Greeks |
| `/api/v1/options/protective-put` | `GET` | Protective Put Payoff Curve & Breakeven (*Slide 68*) |
| `/api/v1/options/protective-call` | `GET` | Protective Call Payoff Curve & Breakeven (*Slide 69*) |
| `/hubs/market-signals` | `WS` | SignalR WebSocket Hub for real-time mobile push |
