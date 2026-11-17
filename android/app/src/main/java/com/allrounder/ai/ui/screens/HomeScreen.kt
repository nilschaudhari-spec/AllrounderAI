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
import com.allrounder.ai.domain.model.Signal
import com.allrounder.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSignals: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToChart: (String) -> Unit,
    onNavigateToOptions: () -> Unit,
    onNavigateToBacktest: () -> Unit,
    onNavigateToPaperTrading: () -> Unit,
    onSelectSignal: (Signal) -> Unit
) {
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
            // Market Indices Strip
            item {
                Text(
                    text = "LIVE MARKET OVERVIEW",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        IndexCard("NIFTY 50", "24,350.25", "+0.45%", isUp = true) { onNavigateToChart("NIFTY 50") }
                    }
                    item {
                        IndexCard("BANK NIFTY", "51,240.00", "+0.82%", isUp = true) { onNavigateToChart("BANK NIFTY") }
                    }
                    item {
                        IndexCard("SENSEX", "79,880.50", "+0.38%", isUp = true) { onNavigateToChart("SENSEX") }
                    }
                    item {
                        IndexCard("MCX GOLD", "76,500.00", "-0.15%", isUp = false) { onNavigateToChart("MCX GOLD") }
                    }
                }
            }

            // Quick Navigation Grid
            item {
                Text(
                    text = "STRATEGY TERMINAL MODULES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile("Multi-Market Scanner", Icons.Default.FilterAlt, AccentBlue, Modifier.weight(1f), onNavigateToScanner)
                    ActionTile("Live Buy/Sell Signals", Icons.Default.TrendingUp, BullishGreen, Modifier.weight(1f), onNavigateToSignals)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile("Options & Payoff (Slide 68)", Icons.Default.Shield, AccentOrange, Modifier.weight(1f), onNavigateToOptions)
                    ActionTile("Historical Backtest", Icons.Default.History, Color(0xFF8B5CF6), Modifier.weight(1f), onNavigateToBacktest)
                }
            }

            // Recent Top PPT Strategy Signals
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOP MATCHED STRATEGIES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    TextButton(onClick = onNavigateToSignals) {
                        Text("View All", color = AccentBlue, fontSize = 12.sp)
                    }
                }
            }

            val mockSignals = listOf(
                Signal(
                    signalId = "s1",
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
                    marketTrend = "Bullish Reversal Confluence",
                    confirmationMode = "CANDLE_CLOSE",
                    analysisSummary = "Supertrend(20,2) Bullish, Price > VWAP, MACD Bullish crossover on 5m chart. Strict 1:1 Target & Stop Loss configured."
                ),
                Signal(
                    signalId = "s2",
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
                    qualityScore = 92.0,
                    marketTrend = "Reversal Trend",
                    confirmationMode = "CANDLE_CLOSE",
                    analysisSummary = "Red candle low broken, current green candle closed upside with PPT 7-10 pt rule satisfied."
                )
            )

            items(mockSignals) { signal ->
                SignalCard(signal = signal, onClick = { onSelectSignal(signal) })
            }
        }
    }
}

@Composable
fun IndexCard(name: String, price: String, change: String, isUp: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = name, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = price, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = change,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isUp) BullishGreen else BearishRed
            )
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
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = signal.signalType.uppercase(),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = signal.tradingSymbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${signal.timeframe}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Score: ${signal.qualityScore.toInt()}%",
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${signal.strategyName} (${signal.pptSlideReference})",
                color = AccentOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry", color = TextSecondary, fontSize = 11.sp)
                    Text("₹${signal.entryPrice}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                }
                Column {
                    Text("Target (1:1)", color = TextSecondary, fontSize = 11.sp)
                    Text("₹${signal.targetPrice}", fontWeight = FontWeight.Bold, color = BullishGreen, fontSize = 13.sp)
                }
                Column {
                    Text("Stop Loss", color = TextSecondary, fontSize = 11.sp)
                    Text("₹${signal.stopLossPrice}", fontWeight = FontWeight.Bold, color = BearishRed, fontSize = 13.sp)
                }
                Column {
                    Text("R:R Ratio", color = TextSecondary, fontSize = 11.sp)
                    Text("1:1", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}
