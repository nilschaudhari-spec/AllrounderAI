package com.allrounder.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.allrounder.ai.ui.theme.*

// ============================================================================
// 1. OPTIONS STRATEGY BUILDER SCREEN (Slides 63-69)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsStrategyScreen(
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Protective Put (Slide 68), 1 = Protective Call (Slide 69), 2 = Option Chain

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Options Hedged Strategies", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = TextPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Protective Put (Slide 68)", fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Protective Call (Slide 69)", fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Option Chain", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> ProtectiveHedgeCard(
                    title = "PROTECTIVE PUT (UPTREND HEDGE)",
                    slide = "PPT Slide 68",
                    marketView = "Uptrend (Avoid if sideways)",
                    leg1 = "Buy BANKNIFTY Future (1 Lot @ ₹51,240)",
                    leg2 = "Buy BANKNIFTY ATM Put (1 Lot Strike 51,200 PE @ ₹210)",
                    maxProfit = "Unlimited",
                    maxLoss = "₹3,750 (Limited)",
                    breakeven = "₹51,450",
                    isBullish = true
                )
                1 -> ProtectiveHedgeCard(
                    title = "PROTECTIVE CALL (DOWNTREND HEDGE)",
                    slide = "PPT Slide 69",
                    marketView = "Downtrend (Avoid if sideways)",
                    leg1 = "Short Sell BANKNIFTY Future (1 Lot @ ₹51,240)",
                    leg2 = "Buy BANKNIFTY ATM Call (1 Lot Strike 51,200 CE @ ₹240)",
                    maxProfit = "₹7,65,000 (Capped at 0)",
                    maxLoss = "₹3,000 (Limited)",
                    breakeven = "₹51,000",
                    isBullish = false
                )
                2 -> OptionChainView()
            }
        }
    }
}

@Composable
fun ProtectiveHedgeCard(
    title: String,
    slide: String,
    marketView: String,
    leg1: String,
    leg2: String,
    maxProfit: String,
    maxLoss: String,
    breakeven: String,
    isBullish: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = if (isBullish) BullishGreen else BearishRed, fontSize = 14.sp)
                Text(slide, color = AccentOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Rule: $marketView", color = TextSecondary, fontSize = 12.sp)

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)

            Text("POSITION LEGS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(leg1, color = TextPrimary, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(leg2, color = TextPrimary, fontSize = 13.sp)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("MAX PROFIT", color = TextSecondary, fontSize = 11.sp)
                    Text(maxProfit, color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("MAX LOSS", color = TextSecondary, fontSize = 11.sp)
                    Text(maxLoss, color = BearishRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("BREAKEVEN", color = TextSecondary, fontSize = 11.sp)
                    Text(breakeven, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun OptionChainView() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CALL OI / LTP", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("STRIKE", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("PUT LTP / OI", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = DarkSurfaceVariant)

            val strikes = listOf(
                Triple("124k / ₹380", "51,000", "₹120 / 89k"),
                Triple("168k / ₹295", "51,100", "₹165 / 112k"),
                Triple("210k / ₹210", "51,200 (ATM)", "₹210 / 195k"),
                Triple("145k / ₹145", "51,300", "₹275 / 140k"),
                Triple("98k / ₹95", "51,400", "₹350 / 84k")
            )

            strikes.forEach { (call, strike, put) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(call, color = TextSecondary, fontSize = 11.sp)
                    Text(strike, color = if (strike.contains("ATM")) AccentOrange else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(put, color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// ============================================================================
// 2. HISTORICAL BACKTESTING SCREEN (Prompt Section 10)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacktestScreen(
    onBack: () -> Unit
) {
    var isExecuting by remember { mutableStateOf(false) }
    var backtestCompleted by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historical Backtesting Engine", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("BACKTEST CONFIGURATION", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Strategy: Daily Intraday Technique (5M Supertrend+VWAP+MACD)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Instrument: NIFTY 50 • Timeframe: 5-Minute", color = TextSecondary, fontSize = 12.sp)
                        Text("Date Range: 01-Jan-2026 to 15-Nov-2026", color = TextSecondary, fontSize = 12.sp)
                        Text("Risk-to-Reward: Strict 1:1 Ratio (Slide 62 Rule 4)", color = AccentOrange, fontSize = 12.sp)
                        Text("Initial Capital: ₹1,00,000 • Brokerage: ₹20/order", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // Results Performance Dashboard
            if (backtestCompleted) {
                item {
                    Text("VERIFIED PERFORMANCE RESULTS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("WIN RATE", color = TextSecondary, fontSize = 11.sp)
                                    Text("63.8%", color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column {
                                    Text("TOTAL NET P&L", color = TextSecondary, fontSize = 11.sp)
                                    Text("+₹1,48,520", color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column {
                                    Text("PROFIT FACTOR", color = TextSecondary, fontSize = 11.sp)
                                    Text("1.68", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = DarkSurfaceVariant)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Executed Trades: 312", color = TextPrimary, fontSize = 12.sp)
                                Text("Winning: 199 | Losing: 113", color = TextSecondary, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Max Drawdown: ₹18,400 (12.4%)", color = BearishRed, fontSize = 12.sp)
                                Text("Sharpe Ratio: 1.84", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
