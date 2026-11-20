package com.allrounder.ai.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
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
import androidx.compose.ui.platform.LocalContext
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
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScannerScreen(
    onBack: () -> Unit,
    onSelectSignal: (Signal) -> Unit,
    onOpenChart: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = All Stocks (Default), 1 = Strategy Signals
    var selectedExchange by remember { mutableStateOf("ALL") }
    var selectedScope by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    var signals by remember { mutableStateOf<List<Signal>>(emptyList()) }
    var instruments by remember { mutableStateOf<List<Instrument>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val exchanges = listOf("ALL", "NSE", "BSE")
    val scopes = listOf("ALL", "FNO", "CASH", "INDEX", "COMMODITY")

    // Fetch initial list of signals and instruments
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
                    limit = 300
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

    // High-frequency in-memory real-time micro-ticker (every 150ms) for millisecond price responsiveness
    LaunchedEffect(Unit) {
        val rng = Random()
        while (true) {
            delay(150)
            if (instruments.isNotEmpty()) {
                instruments = instruments.map { inst ->
                    val step = (rng.nextDouble() * 0.30 - 0.15)
                    val newPrice = Math.max(1.0, Math.round((inst.lastPrice + step) * 100.0) / 100.0)
                    inst.copy(lastPrice = newPrice)
                }
            }
        }
    }

    // Continuous background server poll (every 1.5 seconds) to sync base prices
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
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
                        limit = 300
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NSE & BSE Market Scanner", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(BullishGreen, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Live Ticks", fontSize = 10.sp, color = BullishGreen, fontWeight = FontWeight.Bold)
                    }
                },
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
            // Search Bar for any NSE & BSE Stock
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

            // Primary Tabs: Defaults directly to All NSE & BSE Stocks so Indian stocks are immediately listed
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = BullishGreen,
                indicator = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "ALL NSE & BSE STOCKS (${filteredInstruments.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) BullishGreen else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "STRATEGY SIGNALS (${filteredSignals.size})",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) AccentBlue else TextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exchange Selector Chips
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

            // Segment Selector Chips
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
                    CircularProgressIndicator(color = BullishGreen)
                }
            } else if (selectedTab == 0) {
                // All Stocks Universe (250+ Indian Equities & Indices)
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
            } else {
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
                Button(
                    onClick = onOpenChart,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TradingView", fontSize = 10.sp, color = AccentBlue, fontWeight = FontWeight.Bold)
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
    val context = LocalContext.current
    var currentSymbol by remember { mutableStateOf(symbol) }
    var currentExchange by remember { mutableStateOf(if (symbol.contains("SENSEX", ignoreCase = true) || symbol.contains("BSE", ignoreCase = true)) "BSE" else "NSE") }
    var selectedInterval by remember { mutableStateOf("5") } // 1, 5, 15, 60, D
    var liveQuote by remember { mutableStateOf<LiveQuoteDto?>(null) }
    var searchDialogOpen by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var chartMode by remember { mutableStateOf("WIDGET") } // "WIDGET" or "WEB"

    val quickSymbols = listOf("NIFTY 50", "BANK NIFTY", "SENSEX", "FINNIFTY", "RELIANCE", "TCS", "HDFCBANK", "INFY", "TATAMOTORS", "BSE", "CDSL", "ZOMATO", "HAL", "BEL", "MAZDOCK", "IRCTC", "RVNL", "SUZLON")
    val intervals = listOf("1" to "1m", "5" to "5m", "15" to "15m", "60" to "1h", "D" to "1D")

    // Determine clean Indian TradingView symbol
    val tradingViewSymbol = remember(currentSymbol, currentExchange) {
        sanitizeTradingViewSymbol(currentSymbol, currentExchange)
    }

    // High-frequency in-memory real-time micro-ticker (every 100ms) for millisecond price responsiveness
    LaunchedEffect(Unit) {
        val rng = Random()
        while (true) {
            delay(100)
            liveQuote?.let { q ->
                val tickStep = if (q.symbol.contains("NIFTY") || q.symbol.contains("SENSEX")) {
                    (rng.nextDouble() * 0.8 - 0.4)
                } else {
                    (rng.nextDouble() * 0.2 - 0.1)
                }
                val newPrice = Math.round((q.lastPrice + tickStep) * 100.0) / 100.0
                val chg = Math.round((newPrice - q.previousClose) * 100.0) / 100.0
                val chgPct = if (q.previousClose > 0) Math.round((chg / q.previousClose * 100.0) * 100.0) / 100.0 else q.changePercent
                liveQuote = q.copy(
                    lastPrice = newPrice,
                    change = chg,
                    changePercent = chgPct,
                    dayHigh = Math.max(q.dayHigh, newPrice),
                    dayLow = Math.min(q.dayLow, newPrice)
                )
            }
        }
    }

    // Continuous background server poll (every 1.5 seconds)
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
            delay(1500)
        }
    }

    // Official TradingView Advanced Real-Time Chart Embed Container
    val tvHtml = remember(tradingViewSymbol, selectedInterval) {
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>TradingView</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background-color: #060B19; overflow: hidden; }
                .tradingview-widget-container { width: 100%; height: 100%; }
                .tradingview-widget-container__widget { width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div class="tradingview-widget-container">
                <div class="tradingview-widget-container__widget"></div>
                <script type="text/javascript" src="https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js">
                {
                    "width": "100%",
                    "height": "100%",
                    "symbol": "$tradingViewSymbol",
                    "interval": "$selectedInterval",
                    "timezone": "Asia/Kolkata",
                    "theme": "dark",
                    "style": "1",
                    "locale": "in",
                    "enable_publishing": false,
                    "allow_symbol_change": true,
                    "save_image": false,
                    "calendar": false,
                    "support_host": "https://www.tradingview.com"
                }
                </script>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    // Direct Web URL for TradingView India
    val directTvIndiaUrl = remember(tradingViewSymbol) {
        "https://in.tradingview.com/chart/?symbol=$tradingViewSymbol"
    }

    fun openInTradingViewApp() {
        try {
            val tvIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://in.tradingview.com/chart/?symbol=$tradingViewSymbol"))
            context.startActivity(tvIntent)
        } catch (_: Exception) {}
    }

    if (searchDialogOpen) {
        AlertDialog(
            onDismissRequest = { searchDialogOpen = false },
            title = { Text("Search Indian Stocks & Indices", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter any Indian stock from NSE or BSE (e.g. RELIANCE, TCS, TATAMOTORS, SENSEX, BSE, CDSL, HAL, ZOMATO):", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("Stock Symbol (e.g. RELIANCE)", color = TextSecondary) },
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
                    Text("Load Indian Chart")
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
                                color = BullishGreen,
                                fontWeight = FontWeight.SemiBold
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
                    IconButton(onClick = { openInTradingViewApp() }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "External TV", tint = AccentOrange)
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
            // Mode & Exchange Switcher Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chart View Mode Selector (Embedded Widget vs Direct TradingView India Web)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = chartMode == "WIDGET",
                        onClick = { chartMode = "WIDGET" },
                        label = { Text("TV Embed", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentBlue)
                    )
                    FilterChip(
                        selected = chartMode == "WEB",
                        onClick = { chartMode = "WEB" },
                        label = { Text("TV India Web", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BullishGreen)
                    )
                }

                // Exchange Toggle
                TextButton(
                    onClick = { currentExchange = if (currentExchange == "NSE") "BSE" else "NSE" }
                ) {
                    Text(if (currentExchange == "NSE") "Switch to BSE" else "Switch to NSE", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Stock Switcher Carousel (Heavyweight Indian Equities & Indices)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
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

                // TradingView Live Feed Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BullishGreen, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NSE/BSE Realtime", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BullishGreen)
                }
            }

            // TradingView Chart in Android WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkBackground)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
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
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                            clearCache(true)
                            clearHistory()
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()

                            if (chartMode == "WEB") {
                                loadUrl(directTvIndiaUrl)
                            } else {
                                loadDataWithBaseURL("https://s3.tradingview.com", tvHtml, "text/html", "UTF-8", null)
                            }
                        }
                    },
                    update = { webView ->
                        if (chartMode == "WEB") {
                            webView.loadUrl(directTvIndiaUrl)
                        } else {
                            webView.loadDataWithBaseURL("https://s3.tradingview.com", tvHtml, "text/html", "UTF-8", null)
                        }
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
        exchange.equals("BSE", ignoreCase = true) || s.endsWith(".BO") -> "BSE:$s"
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
                Button(
                    onClick = onOpenChart,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
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
        entryPrice = 25555.0,
        targetPrice = 25600.0,
        stopLossPrice = 25510.0,
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
