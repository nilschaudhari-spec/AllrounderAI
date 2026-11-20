package com.allrounder.ai.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScannerScreen(
    onBack: () -> Unit,
    onSelectSignal: (Signal) -> Unit,
    onOpenChart: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Signals, 1 = All Stocks
    var selectedExchange by remember { mutableStateOf("ALL") }
    var selectedScope by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    var signals by remember { mutableStateOf<List<Signal>>(emptyList()) }
    var instruments by remember { mutableStateOf<List<Instrument>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val exchanges = listOf("ALL", "NSE", "BSE")
    val scopes = listOf("ALL", "FNO", "CASH", "INDEX", "COMMODITY")

    // Fetch signals and instruments from live backend
    LaunchedEffect(selectedExchange, selectedScope, searchQuery) {
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
                    search = if (searchQuery.isBlank()) null else searchQuery,
                    limit = 200
                )
            }
            if (instResp.isSuccessful && !instResp.body().isNullOrEmpty()) {
                instruments = instResp.body()!!
            }
        } catch (_: Exception) {
            signals = FallbackSignals
        } finally {
            isLoading = false
        }
    }

    // Continuous real-time polling loop (every 3 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            try {
                withContext(Dispatchers.IO) {
                    val sigResp = ApiClient.apiService.getLiveSignals(exchange = if (selectedExchange == "ALL") null else selectedExchange)
                    if (sigResp.isSuccessful && !sigResp.body().isNullOrEmpty()) {
                        signals = sigResp.body()!!
                    }
                    val instResp = ApiClient.apiService.getInstruments(
                        exchange = if (selectedExchange == "ALL") null else selectedExchange,
                        segment = if (selectedScope == "ALL") null else selectedScope,
                        search = if (searchQuery.isBlank()) null else searchQuery,
                        limit = 200
                    )
                    if (instResp.isSuccessful && !instResp.body().isNullOrEmpty()) {
                        instruments = instResp.body()!!
                    }
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
        val matchesExchange = selectedExchange == "ALL" || inst.exchange.equals(selectedExchange, ignoreCase = true)
        val matchesScope = when (selectedScope) {
            "FNO" -> inst.isFnoEligible
            "INDEX" -> inst.segment.contains("Index", ignoreCase = true)
            "COMMODITY" -> inst.segment.contains("Commodity", ignoreCase = true)
            "CASH" -> inst.segment.contains("Equity", ignoreCase = true)
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                inst.symbol.contains(searchQuery, ignoreCase = true) ||
                inst.tradingSymbol.contains(searchQuery, ignoreCase = true) ||
                inst.name.contains(searchQuery, ignoreCase = true)
        matchesExchange && matchesScope && matchesSearch
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
            // Search Bar with Real-time Universal Resolver
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search any NSE & BSE stock (Reliance, TCS, Tata Motors, BSE...)", fontSize = 12.sp, color = TextSecondary) },
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

            // Primary Tabs: Strategy Signals vs All NSE/BSE Stocks
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = AccentBlue,
                indicator = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "STRATEGY SIGNALS (${filteredSignals.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) AccentBlue else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "ALL NSE & BSE STOCKS (${filteredInstruments.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) BullishGreen else TextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exchange & Segment Filter Chips
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EXCHANGE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                exchanges.forEach { ex ->
                    FilterChip(
                        selected = selectedExchange == ex,
                        onClick = { selectedExchange = ex },
                        label = { Text(ex, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (selectedTab == 0) {
                // Signals Tab
                if (filteredSignals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No strategy signals match current filter", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredSignals) { signal ->
                            SignalCard(
                                signal = signal,
                                onClick = { onSelectSignal(signal) },
                                onOpenChart = { onOpenChart(signal.tradingSymbol) }
                            )
                        }
                    }
                }
            } else {
                // All Stocks Universe Tab
                if (filteredInstruments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No instruments found matching search criteria", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredInstruments) { inst ->
                            InstrumentRowCard(
                                instrument = inst,
                                onOpenChart = { onOpenChart(inst.symbol) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstrumentRowCard(
    instrument: Instrument,
    onOpenChart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenChart() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = instrument.symbol,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (instrument.exchange.equals("BSE", ignoreCase = true)) AccentOrange.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = instrument.exchange,
                            color = if (instrument.exchange.equals("BSE", ignoreCase = true)) AccentOrange else AccentBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (instrument.isFnoEligible) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .background(BullishGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("F&O", color = BullishGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${instrument.name} • Lot: ${instrument.lotSize}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${instrument.lastPrice}",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "${if (instrument.changePercent >= 0) "▲ +" else "▼ "}${instrument.changePercent}%",
                    color = if (instrument.changePercent >= 0) BullishGreen else BearishRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    FilledTonalButton(
                        onClick = onOpenChart,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Chart", fontSize = 10.sp, color = AccentBlue)
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
    var currentExchange by remember { mutableStateOf(if (symbol.contains("SENSEX", ignoreCase = true) || symbol.contains("BSE", ignoreCase = true)) "BSE" else "NSE") }
    var selectedInterval by remember { mutableStateOf("5") } // 1, 5, 15, 60, D
    var liveQuote by remember { mutableStateOf<LiveQuoteDto?>(null) }
    var searchDialogOpen by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }

    val quickSymbols = listOf("NIFTY 50", "BANK NIFTY", "SENSEX", "RELIANCE", "TCS", "HDFCBANK", "INFY", "TATAMOTORS", "BSE", "CDSL", "ZOMATO", "HAL")
    val intervals = listOf("1" to "1m", "5" to "5m", "15" to "15m", "60" to "1h", "D" to "1D")

    // Continuous real-time price fetcher for the active symbol
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
            delay(2500)
        }
    }

    // Determine strict Indian TradingView formatted ticker
    val tradingViewSymbol = remember(currentSymbol, currentExchange) {
        sanitizeTradingViewSymbol(currentSymbol, currentExchange)
    }

    val encodedTvSymbol = remember(tradingViewSymbol) {
        URLEncoder.encode(tradingViewSymbol, "UTF-8")
    }

    // Modern, robust TradingView Advanced Chart Embed HTML
    val tvHtml = remember(tradingViewSymbol, selectedInterval, encodedTvSymbol) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background: #060B19; overflow: hidden; }
                #tv_chart_container { width: 100%; height: 100%; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <div id="tv_chart_container">
                <iframe 
                    src="https://s.tradingview.com/widgetembed/?frameElementId=tradingview_widget&symbol=$encodedTvSymbol&interval=$selectedInterval&theme=dark&style=1&timezone=Asia%2FKolkata&locale=in&symboledit=1&saveimage=0&toolbarbg=0B1528&studies=%5B%22MASimple%40tv-basicstudies%22%2C%22RSI%40tv-basicstudies%22%2C%22BollingerBands%40tv-basicstudies%22%5D&utm_source=www.tradingview.com"
                    allowfullscreen>
                </iframe>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    if (searchDialogOpen) {
        AlertDialog(
            onDismissRequest = { searchDialogOpen = false },
            title = { Text("Search Indian Stocks & Indices", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Type any NSE or BSE stock symbol (e.g. RELIANCE, TCS, TATAMOTORS, SENSEX, BSE, CDSL):", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("Stock Symbol", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchInput.isNotBlank()) {
                            currentSymbol = searchInput.trim().uppercase()
                            if (currentSymbol.contains("SENSEX") || currentSymbol == "BSE") {
                                currentExchange = "BSE"
                            }
                            searchDialogOpen = false
                            searchInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Load Chart")
                }
            },
            dismissButton = {
                TextButton(onClick = { searchDialogOpen = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "($tradingViewSymbol)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
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
                    IconButton(onClick = { searchDialogOpen = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Stock", tint = AccentBlue)
                    }
                    TextButton(
                        onClick = { currentExchange = if (currentExchange == "NSE") "BSE" else "NSE" }
                    ) {
                        Text(if (currentExchange == "NSE") "BSE" else "NSE", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            if (sym == "SENSEX" || sym == "BSE") currentExchange = "BSE"
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

                // TradingView Realtime Feed Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BullishGreen, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TradingView Live", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BullishGreen)
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
                            settings.databaseEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
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
        }
    }
}

fun sanitizeTradingViewSymbol(symbol: String, exchange: String): String {
    val s = symbol.trim().uppercase()
        .replace("NSE:", "")
        .replace("BSE:", "")
        .replace("NSE-EQ-", "")
        .replace("BSE-EQ-", "")
        .replace("IDX-NSE-", "")
        .replace("IDX-BSE-", "")
        .replace(".NS", "")
        .replace(".BO", "")
        .trim()

    return when {
        s == "NIFTY 50" || s == "NIFTY" -> "NSE:NIFTY"
        s.contains("BANK") && s.contains("NIFTY") -> "NSE:BANKNIFTY"
        s == "SENSEX" -> "BSE:SENSEX"
        s == "FINNIFTY" -> "NSE:FINNIFTY"
        s == "NIFTY IT" || s == "NIFTYIT" -> "NSE:CNXIT"
        s == "BSE 100" || s == "BSE100" -> "BSE:BSE100"
        s == "BSE 500" || s == "BSE500" -> "BSE:BSE500"
        s == "GOLD" || s.contains("GOLD") -> "MCX:GOLD"
        s == "SILVER" || s.contains("SILVER") -> "MCX:SILVER"
        s == "CRUDEOIL" || s.contains("CRUDE") -> "MCX:CRUDEOIL"
        exchange.equals("BSE", ignoreCase = true) || s == "BSE" || s.endsWith(".BO") -> "BSE:$s"
        else -> "NSE:$s"
    }
}

@Composable
fun SignalCard(
    signal: Signal,
    onClick: () -> Unit,
    onOpenChart: () -> Unit = {}
) {
    val isBuy = signal.signalType.equals("BUY", ignoreCase = true)
    val accentColor = if (isBuy) BullishGreen else BearishRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = signal.tradingSymbol,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(signal.exchange, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${signal.signalType.uppercase()} • ${signal.timeframe}",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${signal.strategyName} • ${signal.pptSlideReference}",
                color = AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Price Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry", color = TextSecondary, fontSize = 11.sp)
                    Text("₹${signal.entryPrice}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column {
                    Text("Target (1:1)", color = TextSecondary, fontSize = 11.sp)
                    Text("₹${signal.targetPrice}", color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Stop Loss", color = TextSecondary, fontSize = 11.sp)
                    Text("₹${signal.stopLossPrice}", color = BearishRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Quality", color = TextSecondary, fontSize = 11.sp)
                    Text("${signal.qualityScore}%", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onOpenChart,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TradingView Chart", fontSize = 10.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
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
        strategyName = "Daily Intraday 5M Supertrend + MACD",
        pptSlideReference = "Slides 59-62",
        signalType = "BUY",
        status = "CONFIRMED",
        timeframe = "5m",
        entryPrice = 24350.0,
        targetPrice = 24390.0,
        stopLossPrice = 24310.0,
        riskRewardRatio = 1.0,
        qualityScore = 96.0,
        marketTrend = "Bullish Reversal Confluence",
        confirmationMode = "CANDLE_CLOSE",
        analysisSummary = "Supertrend(20,2) Bullish, Price > VWAP, MACD Bullish crossover. Strict 1:1 Target."
    ),
    Signal(
        signalId = "sig-banknifty-lb-02",
        tradingSymbol = "BANK NIFTY",
        exchange = "NSE",
        strategyId = "LOW_BREAK_CLOSE_UPSIDE",
        strategyName = "Low Break, Close Upside",
        pptSlideReference = "Slides 13-14",
        signalType = "BUY",
        status = "CONFIRMED",
        timeframe = "15m",
        entryPrice = 51240.0,
        targetPrice = 51320.0,
        stopLossPrice = 51160.0,
        riskRewardRatio = 1.0,
        qualityScore = 90.0,
        marketTrend = "Reversal from Downtrend",
        confirmationMode = "CANDLE_CLOSE",
        analysisSummary = "Breaks prior swing low then aggressively reverses and closes on upside with huge demand absorption."
    ),
    Signal(
        signalId = "sig-reliance-hammer-03",
        tradingSymbol = "RELIANCE",
        exchange = "NSE",
        strategyId = "HAMMER",
        strategyName = "Institutional Hammer Reversal",
        pptSlideReference = "Slides 1-12",
        signalType = "BUY",
        status = "CONFIRMED",
        timeframe = "15m",
        entryPrice = 1363.0,
        targetPrice = 1380.0,
        stopLossPrice = 1346.0,
        riskRewardRatio = 1.0,
        qualityScore = 93.5,
        marketTrend = "Support Zone Bounce",
        confirmationMode = "CANDLE_CLOSE",
        analysisSummary = "Classic Hammer with long lower shadow >= 2x body formed right on multi-day horizontal demand zone."
    )
)
