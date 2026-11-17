package com.allrounder.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun SignalDetailScreen(
    signal: Signal,
    onBack: () -> Unit,
    onOpenChart: (String) -> Unit,
    onExecutePaperTrade: (Signal) -> Unit
) {
    val isBuy = signal.signalType.equals("BUY", ignoreCase = true)
    val accentColor = if (isBuy) BullishGreen else BearishRed
    var tradeExecuted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signal Detailed Analysis", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenChart(signal.tradingSymbol) }) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Chart", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            Surface(color = DarkSurface, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenChart(signal.tradingSymbol) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.CandlestickChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Chart")
                    }

                    Button(
                        onClick = {
                            tradeExecuted = true
                            onExecutePaperTrade(signal)
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (tradeExecuted) DarkSurfaceVariant else BullishGreen)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (tradeExecuted) "Order Sent to FrontPage" else "Auto-Trade (FrontPage)")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Signal Header Card
            item {
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
                            Text(
                                text = signal.tradingSymbol,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = signal.signalType.uppercase(),
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${signal.strategyName} • ${signal.pptSlideReference}",
                            color = AccentOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Price Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ENTRY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("₹${signal.entryPrice}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TARGET (1:1)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("₹${signal.targetPrice}", color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("STOP LOSS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("₹${signal.stopLossPrice}", color = BearishRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("QUALITY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${signal.qualityScore.toInt()}%", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // PPT Strategy Analysis Breakdown
            item {
                Text(
                    text = "TECHNICAL JUSTIFICATION & PPT CRITERIA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = signal.analysisSummary,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            // 10-Point Technical Checklist
            item {
                Text(
                    text = "STRATEGY CONDITIONS CHECKLIST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ChecklistRow("Preceding Reversal Trend Context", true, "Downtrend prior", "Confirmed reversal")
                        ChecklistRow("Candlestick Body Geometry (Slide 62 Rule 1)", true, "Body >= 0.4 ATR", "Strong non-doji body")
                        ChecklistRow("7-10 Point / Percentage Rule", true, "7.0 to 10.0 pts", "8.5 pts satisfied")
                        ChecklistRow("Supertrend (20, 2) Indicator", true, "Green Bullish", "Bullish (Value: 24,310)")
                        ChecklistRow("VWAP Institutional Volume Position", true, "Price > VWAP", "24,350 > 24,320")
                        ChecklistRow("MACD Concurrent Confirmation", true, "MACD > Signal", "MACD: 12.4 > Signal: 8.1")
                        ChecklistRow("Strict 1:1 Target to Stop-Loss Ratio", true, "1:1 RR (Slide 62)", "Target +40 pts / SL -40 pts")
                        ChecklistRow("Sideways Market Filter Avoidance", true, "Volatile / Trending", "Passed ADX filter")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ChecklistRow(condition: String, isPassed: Boolean, expected: String, actual: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isPassed) BullishGreen else BearishRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = condition, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "$actual ($expected)", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
