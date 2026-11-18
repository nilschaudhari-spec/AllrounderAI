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
import com.allrounder.ai.domain.model.HammerDetectionDetailDto
import com.allrounder.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HammerAnalysisScreen(
    detection: HammerDetectionDetailDto,
    onBack: () -> Unit,
    onOpenChart: (String) -> Unit
) {
    val statusColor = when (detection.patternStatus) {
        "BUY" -> BullishGreen
        "HAMMER_CONFIRMED" -> BullishGreen
        "HAMMER_CANDIDATE" -> AccentBlue
        "HAMMER_GEOMETRY_ONLY" -> AccentOrange
        else -> NeutralGray
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hammer Algorithmic Audit", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenChart(detection.tradingSymbol) }) {
                        Icon(Icons.Default.CandlestickChart, contentDescription = "Open Chart", tint = AccentBlue)
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
                    Button(
                        onClick = { onOpenChart(detection.tradingSymbol) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open TradingView Terminal Chart", fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(detection.tradingSymbol, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(detection.exchange, color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("${detection.segment} • ${detection.timeframe} Chart Execution", color = TextSecondary, fontSize = 11.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(detection.patternStatus, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quality Score Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("HAMMER QUALITY SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("${detection.score.finalScore} / 100", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (detection.score.finalScore / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = BullishGreen,
                            trackColor = DarkSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(detection.score.summary, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                    }
                }
            }

            // Candle Geometry Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. CANDLE GEOMETRY ANALYSIS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                        Spacer(modifier = Modifier.height(10.dp))

                        AuditRow("Lower Wick / Body Ratio", "${detection.geometry.lowerWickToBodyRatio}×", "≥ 2.0× Required", isPass = detection.geometry.lowerWickToBodyRatio >= 2.0)
                        AuditRow("Upper Wick / Range", "${(detection.geometry.upperWickToRangeRatio * 100).toInt()}%", "≤ 10% Allowed", isPass = detection.geometry.upperWickToRangeRatio <= 0.10)
                        AuditRow("Body / Range Ratio", "${(detection.geometry.bodyToRangeRatio * 100).toInt()}%", "≤ 35% Allowed", isPass = detection.geometry.bodyToRangeRatio <= 0.35)
                        AuditRow("Body Position in Range", "${(detection.geometry.bodyPosition * 100).toInt()}%", "≥ 60% Upper Range", isPass = detection.geometry.bodyPosition >= 0.60)
                        AuditRow("Range / ATR Ratio", "${detection.geometry.rangeToAtrRatio}×", "≥ 0.50× ATR", isPass = detection.geometry.rangeToAtrRatio >= 0.50)
                        AuditRow("Zero-Body Doji Policy", if (detection.geometry.isZeroBodyDoji) "Pure Doji" else "Authentic Body", "Real body required", isPass = !detection.geometry.isZeroBodyDoji)
                    }
                }
            }

            // Trend & Context Evaluation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. TREND & CONTEXT EVALUATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                        Spacer(modifier = Modifier.height(10.dp))

                        AuditRow("Preceding Downtrend", if (detection.trend.isDowntrendConfirmed) "Confirmed" else "Failed", "Downtrend Required", isPass = detection.trend.isDowntrendConfirmed)
                        AuditRow("Conditions Passed", "${detection.trend.conditionsPassed} / ${detection.trend.totalConditionsChecked}", "≥ 2/3 (Moderate)", isPass = detection.trend.conditionsPassed >= 2)
                        AuditRow("Prior Net Price Change", "${detection.trend.priceDeclinePercent}%", "Negative Decline", isPass = detection.trend.priceDeclinePercent < 0)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(detection.trend.summaryExplanation, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                    }
                }
            }

            // Support & Volume Confirmation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("3. SUPPORT & VOLUME CONFLUENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                        Spacer(modifier = Modifier.height(10.dp))

                        AuditRow("Support Proximity", if (detection.support.isNearSupport) "Near Support" else "Outside Zone", "≤ 1.0 ATR Distance", isPass = detection.support.isNearSupport)
                        if (detection.support.isNearSupport) {
                            AuditRow("Nearest Support Level", "₹${detection.support.nearestSupportPrice} (${detection.support.supportType})", "${detection.support.distanceInAtr} ATR", isPass = true)
                            AuditRow("Support Touch Count", "${detection.support.touchesCount} Touches", detection.support.strengthLevel, isPass = true)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        AuditRow("Volume Ratio vs 20SMA", "${detection.volume.volumeRatio}×", "≥ 1.50× Strong", isPass = detection.volume.volumeRatio >= 1.50)
                        AuditRow("Volume Classification", detection.volume.volumeClassification, "Advisory Mode", isPass = true)
                    }
                }
            }

            // Institutional Trading Plan Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("4. INSTITUTIONAL TRADING PLAN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                            Text("1:1 Strict Target", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ENTRY", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("₹${detection.entryPrice}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TARGET (1:1)", fontSize = 10.sp, color = BullishGreen, fontWeight = FontWeight.Bold)
                                Text("₹${detection.targetPrice}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("STOP LOSS", fontSize = 10.sp, color = BearishRed, fontWeight = FontWeight.Bold)
                                Text("₹${detection.stopLossPrice}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BearishRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Position Size: ${detection.recommendedPositionSize} shares", fontSize = 11.sp, color = TextSecondary)
                            Text("Risk Budget: ₹${detection.riskAmount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }

            // Full Explainability Checklist
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("5. EXPLAINABILITY BULLET AUDIT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        detection.explainabilityBulletPoints.forEach { point ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BullishGreen,
                                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(point, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditRow(label: String, value: String, expected: String, isPass: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(label, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(expected, fontSize = 9.sp, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isPass) BullishGreen else BearishRed)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                if (isPass) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isPass) BullishGreen else BearishRed,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
