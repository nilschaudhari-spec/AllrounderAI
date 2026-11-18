package com.allrounder.ai.ui.screens

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
import com.allrounder.ai.data.network.ApiClient
import com.allrounder.ai.domain.model.LiveQuoteDto
import com.allrounder.ai.domain.model.Signal
import com.allrounder.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSignals: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToStrategies: () -> Unit,
    onNavigateToChart: (String) -> Unit,
    onNavigateToOptions: () -> Unit,
    onNavigateToBacktest: () -> Unit,
    onNavigateToPaperTrading: () -> Unit,
    onSelectSignal: (Signal) -> Unit
) {
    var signals by remember { mutableStateOf<List<Signal>>(emptyList()) }
    var niftyQuote by remember { mutableStateOf(LiveQuoteDto("NIFTY 50", "NSE", 24350.25, 108.5, 0.45)) }
    var bankNiftyQuote by remember { mutableStateOf(LiveQuoteDto("BANK NIFTY", "NSE", 51240.00, 415.0, 0.82)) }
    var sensexQuote by remember { mutableStateOf(LiveQuoteDto("SENSEX", "BSE", 79880.50, 305.2, 0.38)) }
    var relianceQuote by remember { mutableStateOf(LiveQuoteDto("RELIANCE", "NSE", 1363.00, -4.0, -0.29)) }

    // Fetch initial signals and quotes
    LaunchedEffect(Unit) {
        try {
            val sigResp = withContext(Dispatchers.IO) {
                ApiClient.apiService.getLiveSignals()
            }
            if (sigResp.isSuccessful && !sigResp.body().isNullOrEmpty()) {
                signals = sigResp.body()!!
            } else {
                signals = FallbackSignals
            }
        } catch (_: Exception) {
            signals = FallbackSignals
        }
    }

    // Real-time market overview polling loop (every 5 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            try {
                withContext(Dispatchers.IO) {
                    val nq = ApiClient.apiService.getLiveQuote("NIFTY 50", "NSE")
                    if (nq.isSuccessful && nq.body() != null) niftyQuote = nq.body()!!

                    val bq = ApiClient.apiService.getLiveQuote("BANK NIFTY", "NSE")
                    if (bq.isSuccessful && bq.body() != null) bankNiftyQuote = bq.body()!!

                    val sq = ApiClient.apiService.getLiveQuote("SENSEX", "BSE")
                    if (sq.isSuccessful && sq.body() != null) sensexQuote = sq.body()!!

                    val rq = ApiClient.apiService.getLiveQuote("RELIANCE", "NSE")
                    if (rq.isSuccessful && rq.body() != null) relianceQuote = rq.body()!!

                    val sigResp = ApiClient.apiService.getLiveSignals()
                    if (sigResp.isSuccessful && !sigResp.body().isNullOrEmpty()) {
                        signals = sigResp.body()!!
                    }
                }
            } catch (_: Exception) {}
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ALL ROUNDER AI",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.Search, contentDescription = "Scan Market", tint = TextPrimary)
                    }
                    IconButton(onClick = onNavigateToPaperTrading) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Paper Trading", tint = BullishGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Real-Time Market Overview Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE MARKET OVERVIEW (NSE & BSE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(BullishGreen, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Realtime Stream", fontSize = 10.sp, color = BullishGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        IndexCard(
                            name = "NIFTY 50 (NSE)",
                            price = "₹${niftyQuote.lastPrice}",
                            change = "${if (niftyQuote.changePercent >= 0) "+" else ""}${niftyQuote.changePercent}%",
                            isUp = niftyQuote.changePercent >= 0
                        ) { onNavigateToChart("NIFTY 50") }
                    }
                    item {
                        IndexCard(
                            name = "BANK NIFTY (NSE)",
                            price = "₹${bankNiftyQuote.lastPrice}",
                            change = "${if (bankNiftyQuote.changePercent >= 0) "+" else ""}${bankNiftyQuote.changePercent}%",
                            isUp = bankNiftyQuote.changePercent >= 0
                        ) { onNavigateToChart("BANK NIFTY") }
                    }
                    item {
                        IndexCard(
                            name = "SENSEX (BSE)",
                            price = "₹${sensexQuote.lastPrice}",
                            change = "${if (sensexQuote.changePercent >= 0) "+" else ""}${sensexQuote.changePercent}%",
                            isUp = sensexQuote.changePercent >= 0
                        ) { onNavigateToChart("SENSEX") }
                    }
                    item {
                        IndexCard(
                            name = "RELIANCE (NSE)",
                            price = "₹${relianceQuote.lastPrice}",
                            change = "${if (relianceQuote.changePercent >= 0) "+" else ""}${relianceQuote.changePercent}%",
                            isUp = relianceQuote.changePercent >= 0
                        ) { onNavigateToChart("RELIANCE") }
                    }
                }
            }

            // Quick Navigation Grid
            item {
                Text(
                    text = "STRATEGY TERMINAL MODULES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile("Multi-Market Scanner", Icons.Default.FilterAlt, AccentBlue, Modifier.weight(1f), onNavigateToScanner)
                    ActionTile("TradingView Chart", Icons.Default.CandlestickChart, BullishGreen, Modifier.weight(1f)) { onNavigateToChart("NIFTY 50") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile("Strategies Hub (16)", Icons.Default.AutoAwesome, Color(0xFF38BDF8), Modifier.weight(1f), onNavigateToStrategies)
                    ActionTile("Live Buy/Sell Signals", Icons.Default.TrendingUp, AccentOrange, Modifier.weight(1f), onNavigateToSignals)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile("Options & Payoff (Slide 68)", Icons.Default.Shield, Color(0xFFF59E0B), Modifier.weight(1f), onNavigateToOptions)
                    ActionTile("Historical Backtest", Icons.Default.History, Color(0xFF8B5CF6), Modifier.weight(1f), onNavigateToBacktest)
                }
            }

            // Top Matched Strategy Signals
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOP MATCHED STRATEGIES (LIVE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    TextButton(onClick = onNavigateToSignals) {
                        Text("View All Signals", color = AccentBlue, fontSize = 12.sp)
                    }
                }
            }

            items(signals) { signal ->
                SignalCard(signal = signal, onClick = { onSelectSignal(signal) })
            }
        }
    }
}

@Composable
fun IndexCard(name: String, price: String, change: String, isUp: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(155.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = name, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = price, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isUp) "▲ $change" else "▼ $change",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUp) BullishGreen else BearishRed
                )
            }
        }
    }
}

@Composable
fun ActionTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 14.sp)
        }
    }
}

@Composable
fun SignalCard(signal: Signal, onClick: () -> Unit) {
    val isBuy = signal.signalType.equals("BUY", ignoreCase = true)
    val accentColor = if (isBuy) BullishGreen else BearishRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = signal.exchange,
                            color = AccentBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = signal.pptSlideReference,
                            color = AccentOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = signal.signalType,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = signal.strategyName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ENTRY", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("₹${signal.entryPrice}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TARGET (1:1)", fontSize = 9.sp, color = BullishGreen, fontWeight = FontWeight.Bold)
                    Text("₹${signal.targetPrice}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("STOP LOSS", fontSize = 9.sp, color = BearishRed, fontWeight = FontWeight.Bold)
                    Text("₹${signal.stopLossPrice}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BearishRed)
                }
            }
        }
    }
}
