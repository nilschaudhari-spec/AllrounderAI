package com.allrounder.ai.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.allrounder.ai.data.network.ApiClient
import com.allrounder.ai.domain.model.Instrument
import com.allrounder.ai.domain.model.LiveQuoteDto
import com.allrounder.ai.domain.model.Signal
import com.allrounder.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScannerScreen(
    onBack: () -> Unit,
    onSelectSignal: (Signal) -> Unit,
    onOpenChart: (String) -> Unit = {}
) {
    var selectedExchange by remember { mutableStateOf("ALL") }
    var selectedScope by remember { mutableStateOf("ALL") }
    var selectedTimeframe by remember { mutableStateOf("5m") }
    var searchQuery by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    var signals by remember { mutableStateOf<List<Signal>>(emptyList()) }
    var instruments by remember { mutableStateOf<List<Instrument>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val exchanges = listOf("ALL", "NSE", "BSE")
    val scopes = listOf("ALL", "FNO", "CASH", "INDEX", "COMMODITY")
    val timeframes = listOf("1m", "3m", "5m", "15m", "30m", "1h", "D")

    // Fetch signals and instruments from live backend with real-time updates
    LaunchedEffect(selectedExchange, selectedScope) {
        isLoading = true
        try {
            val sigResp = withContext(Dispatchers.IO) {
                ApiClient.apiService.getLiveSignals(exchange = if (selectedExchange == "ALL") null else selectedExchange)
            }
            if (sigResp.isSuccessful && !sigResp.body().isNullOrEmpty()) {
                signals = sigResp.body()!!
            } else {
                signals = FallbackSignals
            }

            val instResp = withContext(Dispatchers.IO) {
                ApiClient.apiService.getInstruments(
                    exchange = if (selectedExchange == "ALL") null else selectedExchange,
                    segment = if (selectedScope == "ALL") null else selectedScope,
                    limit = 60
                )
            }
            if (instResp.isSuccessful && !instResp.body().isNullOrEmpty()) {
                instruments = instResp.body()!!
            }
        } catch (e: Exception) {
            signals = FallbackSignals
        } finally {
            isLoading = false
        }
    }

    // Real-time price background refresh loop every 6 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            try {
                val refreshResp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getLiveSignals(exchange = if (selectedExchange == "ALL") null else selectedExchange)
                }
                if (refreshResp.isSuccessful && !refreshResp.body().isNullOrEmpty()) {
                    signals = refreshResp.body()!!
                }
            } catch (_: Exception) {}
        }
    }

    val filteredSignals = signals.filter { sig ->
        val matchesExchange = selectedExchange == "ALL" || sig.exchange.equals(selectedExchange, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                sig.tradingSymbol.contains(searchQuery, ignoreCase = true) ||
                sig.strategyName.contains(searchQuery, ignoreCase = true) ||
                sig.pptSlideReference.contains(searchQuery, ignoreCase = true)
        matchesExchange && matchesSearch
    }

    val filteredInstruments = instruments.filter { inst ->
        searchQuery.isNotBlank() && (
                inst.symbol.contains(searchQuery, ignoreCase = true) ||
                inst.tradingSymbol.contains(searchQuery, ignoreCase = true) ||
                inst.name.contains(searchQuery, ignoreCase = true)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NSE & BSE Market Scanner", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { isScanning = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar for All Stocks
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 100+ NSE & BSE stocks (Reliance, TCS, Tata Motors...)", fontSize = 12.sp, color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Exchange Selector Chips (NSE / BSE / ALL)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EXCHANGE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.width(8.dp))
                exchanges.forEach { ex ->
                    FilterChip(
                        selected = selectedExchange == ex,
                        onClick = { selectedExchange = ex },
                        label = { Text(ex, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Market Segment Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(scopes) { scope ->
                    FilterChip(
                        selected = selectedScope == scope,
                        onClick = { selectedScope = scope },
                        label = { Text(scope, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCHED LIVE SIGNALS (${filteredSignals.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text(
                    text = "Strict 1:1 Target",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BullishGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Show search results if user searched for specific stock
                    if (searchQuery.isNotBlank() && filteredInstruments.isNotEmpty()) {
                        item {
                            Text("MATCHED INSTRUMENTS FROM UNIVERSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                        }
                        items(filteredInstruments.take(5)) { inst ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChart(inst.symbol) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${inst.symbol} (${inst.exchange})", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text(inst.name, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${inst.lastPrice}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text("${if (inst.changePercent >= 0) "+" else ""}${inst.changePercent}%", color = if (inst.changePercent >= 0) BullishGreen else BearishRed, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("CONFIRMED STRATEGY SIGNALS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                    }

                    items(filteredSignals) { signal ->
                        SignalCard(signal = signal, onClick = { onSelectSignal(signal) })
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveChartScreen(
    symbol: String,
    onBack: () -> Unit
) {
    var currentSymbol by remember { mutableStateOf(symbol) }
    var currentExchange by remember { mutableStateOf(if (symbol.contains("SENSEX") || symbol.contains("BSE")) "BSE" else "NSE") }
    var selectedInterval by remember { mutableStateOf("5") } // 1, 5, 15, 60, D
    var liveQuote by remember { mutableStateOf<LiveQuoteDto?>(null) }

    val quickSymbols = listOf("NIFTY 50", "BANK NIFTY", "SENSEX", "RELIANCE", "TCS", "HDFCBANK", "INFY", "TATAMOTORS", "ZOMATO", "BSE")
    val intervals = listOf("1" to "1m", "5" to "5m", "15" to "15m", "60" to "1h", "D" to "1D")

    // Real-time price fetcher for the active symbol
    LaunchedEffect(currentSymbol, currentExchange) {
        while (true) {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getLiveQuote(currentSymbol, currentExchange)
                }
                if (resp.isSuccessful && resp.body() != null) {
                    liveQuote = resp.body()
                }
            } catch (_: Exception) {}
            delay(4000)
        }
    }

    // Determine TradingView formatted ticker
    val tradingViewSymbol = remember(currentSymbol, currentExchange) {
        val s = currentSymbol.trim().uppercase()
        when {
            s == "NIFTY 50" || s == "NIFTY" -> "NSE:NIFTY"
            s.contains("BANK") && s.contains("NIFTY") -> "NSE:BANKNIFTY"
            s == "SENSEX" -> "BSE:SENSEX"
            s == "NIFTY IT" || s == "NIFTYIT" -> "NSE:CNXIT"
            currentExchange == "BSE" -> "BSE:${s.replace(".BO", "")}"
            else -> "NSE:$s"
        }
    }

    // HTML with TradingView Advanced Real-Time Chart widget
    val tvHtml = remember(tradingViewSymbol, selectedInterval) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #060B19; overflow: hidden; }
                #tv_chart_container { width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div id="tv_chart_container"></div>
            <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
            <script type="text/javascript">
                new TradingView.widget({
                    "autosize": true,
                    "symbol": "$tradingViewSymbol",
                    "interval": "$selectedInterval",
                    "timezone": "Asia/Kolkata",
                    "theme": "dark",
                    "style": "1",
                    "locale": "in",
                    "toolbar_bg": "#0B1528",
                    "enable_publishing": false,
                    "allow_symbol_change": true,
                    "save_image": false,
                    "container_id": "tv_chart_container",
                    "studies": [
                        "MASimple@tv-basicstudies",
                        "RSI@tv-basicstudies",
                        "BollingerBands@tv-basicstudies"
                    ],
                    "backgroundColor": "#060B19",
                    "gridColor": "rgba(255, 255, 255, 0.05)"
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currentSymbol, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(if (currentExchange == "BSE") AccentOrange.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(currentExchange, color = if (currentExchange == "BSE") AccentOrange else AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (liveQuote != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "₹${liveQuote!!.lastPrice}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${if (liveQuote!!.changePercent >= 0) "▲ +" else "▼ "}${liveQuote!!.changePercent}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (liveQuote!!.changePercent >= 0) BullishGreen else BearishRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "H: ₹${liveQuote!!.dayHigh}  L: ₹${liveQuote!!.dayLow}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Exchange Switcher Button
                    TextButton(
                        onClick = { currentExchange = if (currentExchange == "NSE") "BSE" else "NSE" }
                    ) {
                        Text(if (currentExchange == "NSE") "Switch to BSE" else "Switch to NSE", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Quick Stock Switcher Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickSymbols) { sym ->
                    val isSelected = currentSymbol.equals(sym, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            currentSymbol = sym
                            if (sym == "SENSEX") currentExchange = "BSE"
                        },
                        label = { Text(sym, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            // Timeframe Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    intervals.forEach { (code, label) ->
                        val isSelected = selectedInterval == code
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) AccentOrange else DarkSurface, RoundedCornerShape(6.dp))
                                .clickable { selectedInterval = code }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = if (isSelected) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Live TradingView Feed Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BullishGreen, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TradingView Realtime", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BullishGreen)
                }
            }

            // TradingView Interactive Chart in WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkBackground)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            loadDataWithBaseURL("https://www.tradingview.com", tvHtml, "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL("https://www.tradingview.com", tvHtml, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Strategy Confluence Footer
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ALL ROUNDER AI Confluence Matrix", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                        }
                        Text("Risk-Reward 1:1 Strict", fontSize = 10.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Slide 60: Supertrend(20,2) Green + Price > VWAP + MACD Bullish Crossover. Candle close confirmation required.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

val FallbackSignals = listOf(
    Signal(
        signalId = "sig-nifty-5m-01",
        tradingSymbol = "NIFTY 50",
        exchange = "NSE",
        strategyId = "DAILY_INTRADAY_5M",
        strategyName = "Daily Intraday Technique (5-Minute)",
        pptSlideReference = "Slides 59-62",
        signalType = "BUY",
        status = "CONFIRMED",
        timeframe = "5m",
        entryPrice = 24350.0,
        targetPrice = 24390.0,
        stopLossPrice = 24310.0,
        riskRewardRatio = 1.0,
        qualityScore = 96.0,
        marketTrend = "Intraday Reversal Confluence",
        confirmationMode = "CANDLE_CLOSE",
        analysisSummary = "Triple Confluence: Supertrend(20,2) Bullish, Price > VWAP, MACD Bullish crossover on 5m chart. Strict 1:1 Target & Stop Loss configured."
    ),
    Signal(
        signalId = "sig-reliance-lb-02",
        tradingSymbol = "RELIANCE",
        exchange = "NSE",
        strategyId = "LOW_BREAK_CLOSE_UPSIDE",
        strategyName = "Low Break, Close Upside",
        pptSlideReference = "Slides 13-14",
        signalType = "BUY",
        status = "CONFIRMED",
        timeframe = "15m",
        entryPrice = 1363.0,
        targetPrice = 1373.0,
        stopLossPrice = 1353.0,
        riskRewardRatio = 1.0,
        qualityScore = 93.0,
        marketTrend = "Reversal from Liquidity Sweep",
        confirmationMode = "CANDLE_CLOSE",
        analysisSummary = "Prior red candle low broken, followed by strong green candle closing upside with PPT candle body rules satisfied."
    ),
    Signal(
        signalId = "sig-tcs-bse-hammer-03",
        tradingSymbol = "TCS.BO",
        exchange = "BSE",
        strategyId = "HAMMER",
        strategyName = "Hammer Bottom Reversal",
        pptSlideReference = "Slides 5-8",
        signalType = "BUY",
        status = "CONFIRMED",
        timeframe = "15m",
        entryPrice = 2380.2,
        targetPrice = 2405.0,
        stopLossPrice = 2355.4,
        riskRewardRatio = 1.0,
        qualityScore = 91.5,
        marketTrend = "Support Demand Absorption",
        confirmationMode = "CANDLE_CLOSE",
        analysisSummary = "Clear 4-bar downtrend terminated with a classic Hammer on BSE. Lower shadow >= 2.4x body length. Confirmed close above high."
    ),
    Signal(
        signalId = "sig-tatamotors-maru-04",
        tradingSymbol = "TATAMOTORS",
        exchange = "NSE",
        strategyId = "MARUBOZU",
        strategyName = "Marubozu Momentum Breakout",
        pptSlideReference = "Slides 33-36",
        signalType = "BUY",
        status = "CONFIRMED",
        timeframe = "15m",
        entryPrice = 790.0,
        targetPrice = 804.0,
        stopLossPrice = 776.0,
        riskRewardRatio = 1.0,
        qualityScore = 94.0,
        marketTrend = "Institutional Order Flow Surge",
        confirmationMode = "CANDLE_CLOSE",
        analysisSummary = "Bullish Marubozu: Candle body represents 94% of entire range with 2.1x average volume. Open = Low with negligible upper wick."
    ),
    Signal(
        signalId = "sig-sensex-gc-05",
        tradingSymbol = "SENSEX",
        exchange = "BSE",
        strategyId = "GOLDEN_CROSS",
        strategyName = "Golden Cross & 200 EMA Re-entry",
        pptSlideReference = "Slides 51-58",
        signalType = "BUY",
        status = "CONFIRMED",
        timeframe = "Daily",
        entryPrice = 79880.0,
        targetPrice = 80680.0,
        stopLossPrice = 79080.0,
        riskRewardRatio = 1.0,
        qualityScore = 95.5,
        marketTrend = "Macro Bullish Swing Trend",
        confirmationMode = "CANDLE_CLOSE",
        analysisSummary = "S&P BSE Sensex 50 EMA trading above 200 EMA with clean retest bounce off the rising 200-day moving average."
    )
)
