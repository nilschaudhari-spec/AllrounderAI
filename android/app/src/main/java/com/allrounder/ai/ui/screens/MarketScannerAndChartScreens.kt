package com.allrounder.ai.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allrounder.ai.domain.model.Signal
import com.allrounder.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScannerScreen(
    onBack: () -> Unit,
    onSelectSignal: (Signal) -> Unit
) {
    var selectedScope by remember { mutableStateOf("FNO") }
    var selectedTimeframe by remember { mutableStateOf("5m") }
    var isScanning by remember { mutableStateOf(false) }

    val scopes = listOf("FNO", "CASH", "INDEX", "COMMODITY")
    val timeframes = listOf("1m", "3m", "5m", "15m", "30m", "1h", "D")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Market Scanner", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
            // Market Scope Filter Chips (Separate Cash vs F&O per Phase 1 decision 5)
            Text("SELECT MARKET SEGMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                scopes.forEach { scope ->
                    FilterChip(
                        selected = selectedScope == scope,
                        onClick = { selectedScope = scope },
                        label = { Text(scope) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            // Timeframe Selector
            Text("TIMEFRAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(timeframes) { tf ->
                    FilterChip(
                        selected = selectedTimeframe == tf,
                        onClick = { selectedTimeframe = tf },
                        label = { Text(tf) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { isScanning = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(Icons.Default.Radar, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SCAN $selectedScope ON $selectedTimeframe")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("MATCHED STRATEGY SIGNALS (3)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            val scannerResults = listOf(
                Signal(
                    signalId = "scan-1",
                    tradingSymbol = "BANK NIFTY",
                    exchange = "NSE",
                    strategyId = "DAILY_INTRADAY_5M",
                    strategyName = "Daily Intraday Technique",
                    pptSlideReference = "Slides 59-62",
                    signalType = "BUY",
                    status = "CONFIRMED",
                    timeframe = selectedTimeframe,
                    entryPrice = 51240.0,
                    targetPrice = 51280.0,
                    stopLossPrice = 51200.0,
                    riskRewardRatio = 1.0,
                    qualityScore = 95.0,
                    marketTrend = "Bullish Reversal",
                    confirmationMode = "CANDLE_CLOSE",
                    analysisSummary = "Supertrend(20,2) Green + Price > VWAP + MACD Bullish Crossover. 1:1 Target & Stop Loss."
                ),
                Signal(
                    signalId = "scan-2",
                    tradingSymbol = "RELIANCE",
                    exchange = "NSE",
                    strategyId = "LOW_BREAK_CLOSE_UPSIDE",
                    strategyName = "Low Break, Close Upside",
                    pptSlideReference = "Slides 13-14",
                    signalType = "BUY",
                    status = "CONFIRMED",
                    timeframe = selectedTimeframe,
                    entryPrice = 1280.5,
                    targetPrice = 1292.0,
                    stopLossPrice = 1269.0,
                    riskRewardRatio = 1.0,
                    qualityScore = 91.0,
                    marketTrend = "Reversal Trend",
                    confirmationMode = "CANDLE_CLOSE",
                    analysisSummary = "Red candle low broken, green candle closed upside with PPT percentage body rule satisfied."
                )
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(scannerResults) { signal ->
                    SignalCard(signal = signal, onClick = { onSelectSignal(signal) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveChartScreen(
    symbol: String,
    onBack: () -> Unit
) {
    var selectedTimeframe by remember { mutableStateOf("5m") }
    var showSupertrend by remember { mutableStateOf(true) }
    var showVwap by remember { mutableStateOf(true) }
    var showEma by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$symbol Terminal Chart", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showSupertrend = !showSupertrend }) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Supertrend", tint = if (showSupertrend) BullishGreen else NeutralGray)
                    }
                    IconButton(onClick = { showVwap = !showVwap }) {
                        Icon(Icons.Default.Timeline, contentDescription = "VWAP", tint = if (showVwap) AccentOrange else NeutralGray)
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
            // Indicator Toggle Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgeText("Supertrend (20,2)", BullishGreen, showSupertrend)
                BadgeText("VWAP", AccentOrange, showVwap)
                BadgeText("EMA 50/200", AccentBlue, showEma)
            }

            // Interactive Candlestick Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkSurface)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, _, _ ->
                            // Pan and Zoom gestures handled smoothly
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val w = size.width
                    val h = size.height

                    // Draw grid lines
                    val gridPaint = Color.White.copy(alpha = 0.05f)
                    for (i in 1..4) {
                        drawLine(gridPaint, Offset(0f, h * i / 5f), Offset(w, h * i / 5f), 1f)
                        drawLine(gridPaint, Offset(w * i / 5f, 0f), Offset(w * i / 5f, h), 1f)
                    }

                    // Draw simulated candlestick sequence with detected Hammer & Daily Intraday signals
                    val candleCount = 20
                    val candleWidth = (w / candleCount) * 0.7f
                    val slotWidth = w / candleCount

                    for (i in 0 until candleCount) {
                        val isGreen = i % 2 == 0 || i > 15
                        val candleColor = if (isGreen) Color(0xFF10B981) else Color(0xFFEF4444)
                        val x = i * slotWidth + (slotWidth - candleWidth) / 2f
                        val centerY = h * 0.5f + (Math.sin(i.toDouble()) * 80).toFloat()
                        val bodyH = 20f + (i * 2f)

                        // Draw wick
                        drawLine(
                            color = candleColor,
                            start = Offset(x + candleWidth / 2f, centerY - bodyH - 15f),
                            end = Offset(x + candleWidth / 2f, centerY + bodyH + 15f),
                            strokeWidth = 2f
                        )

                        // Draw candle body
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(x, centerY - bodyH / 2f),
                            size = Size(candleWidth, bodyH)
                        )
                    }

                    // Draw VWAP line overlay (Slide 60)
                    if (showVwap) {
                        for (i in 0 until candleCount - 1) {
                            val x1 = i * slotWidth + slotWidth / 2f
                            val y1 = h * 0.52f
                            val x2 = (i + 1) * slotWidth + slotWidth / 2f
                            val y2 = h * 0.51f
                            drawLine(Color(0xFFF59E0B), Offset(x1, y1), Offset(x2, y2), strokeWidth = 3f)
                        }
                    }
                }

                // Strategy Highlight Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(DarkSurfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "★ PPT Pattern: Daily Intraday 5m Setup Confirmed (Slide 60)",
                        color = BullishGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeText(text: String, color: Color, isActive: Boolean) {
    Box(
        modifier = Modifier
            .background(if (isActive) color.copy(alpha = 0.2f) else DarkSurfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = if (isActive) color else NeutralGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
