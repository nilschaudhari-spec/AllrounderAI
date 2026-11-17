# ALL ROUNDER AI – MONSTERASP.NET DEPLOYMENT GUIDE

**GitHub Repository:** [https://github.com/nilschaudhari-spec/AllrounderAI.git](https://github.com/nilschaudhari-spec/AllrounderAI.git)  
**Target Host:** [MonsterASP.NET](https://monsterasp.net/)  
**Database:** PostgreSQL (`db80695.databaseasp.net:5432`, Database: `db80695`)  
**Application:** ALL ROUNDER AI Web API & SignalR Real-Time Hub  

---

## Method 1: Deploy Directly from Git (MonsterASP.NET Control Panel)

1. Log in to your [MonsterASP.NET Control Panel](https://monsterasp.net/).
2. Navigate to **Web Sites** $\rightarrow$ Click on your site (e.g., `your-site.runasp.net`).
3. Under the site settings, click **Git Deployment** (or **Source Control**).
4. Enter the GitHub repository details:
   - **Repository URL:** `https://github.com/nilschaudhari-spec/AllrounderAI.git`
   - **Branch:** `main`
   - **Solution / Project Path:** `AllRounderAi.sln` (or `backend/src/AllRounderAi.WebApi/AllRounderAi.WebApi.csproj`)
5. Click **Connect & Deploy**.
6. MonsterASP.NET pulls the code, restores packages (`Npgsql`), builds the Web API, and hosts it under IIS.

---

## Method 2: Automated Continuous Deployment via GitHub Actions

The repository includes a ready-to-run GitHub Actions workflow (`.github/workflows/deploy.yml`):

1. In MonsterASP.NET Control Panel, download your `.publishSettings` file (**Web Sites -> Web Deploy -> Download Publish Profile**).
2. Open your GitHub Repository:  
   👉 [https://github.com/nilschaudhari-spec/AllrounderAI/settings/secrets/actions](https://github.com/nilschaudhari-spec/AllrounderAI/settings/secrets/actions)
3. Click **New repository secret**:
   - **Name:** `MONSTERASP_PUBLISH_SETTINGS`
   - **Value:** Paste the full XML content of your `.publishSettings` file.
4. Click **Add secret**.
5. Any new push to `main` will automatically build, execute unit tests, publish the self-contained package, and deploy directly to your MonsterASP.NET website via MSDeploy!

---

## Method 3: Instant Zip Upload via MonsterASP.NET File Manager

If you ever want an instant offline or manual update without Git:
1. In MonsterASP.NET Control Panel, click **File Manager**.
2. Open the root folder (`/` or `site/wwwroot/`).
3. Upload `AllRounderAi_Api_MonsterASP_SelfContained.zip`.
4. Click the file and select **Extract**.

---

## Live Endpoints & Database Verification

Once deployed via Git, verify your endpoints:

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
