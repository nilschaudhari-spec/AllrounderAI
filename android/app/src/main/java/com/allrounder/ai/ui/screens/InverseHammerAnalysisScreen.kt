package com.allrounder.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allrounder.ai.domain.model.InverseHammerDetectionDetailDto
import com.allrounder.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InverseHammerAnalysisScreen(
    detection: InverseHammerDetectionDetailDto,
    onBack: () -> Unit,
    onOpenChart: (String) -> Unit
) {
    val statusColor = when (detection.patternStatus) {
        "BUY" -> BullishGreen
        "INVERSE_HAMMER_CONFIRMED" -> BullishGreen
        "INVERSE_HAMMER_CANDIDATE" -> AccentBlue
        "INVERSE_HAMMER_GEOMETRY_ONLY" -> AccentOrange
        else -> NeutralGray
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inverse Hammer Algorithmic Audit", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenChart(detection.tradingSymbol) }) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Open Chart", tint = AccentBlue)
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
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View on Chart", fontWeight = FontWeight.Bold)
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
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(detection.tradingSymbol, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${detection.exchange} • ${detection.segment} • ${detection.timeframe}", fontSize = 11.sp, color = TextSecondary)
                            }

                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(detection.patternStatus.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                        }
                    }
                }
            }

            // 2. Score Breakdown Card (0 - 100)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Inverse Hammer Quality Score", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "${detection.score.finalScore.toInt()}/100 (${detection.score.classification})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (detection.score.finalScore >= 70) BullishGreen else AccentOrange
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (detection.score.finalScore / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (detection.score.finalScore >= 70) BullishGreen else AccentBlue,
                            trackColor = DarkSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        detection.score.breakdown.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.component, fontSize = 11.sp, color = TextSecondary)
                                Row {
                                    Text(
                                        "${String.format("%.1f", item.awardedPoints)}/${String.format("%.0f", item.maxPoints)} pts",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    if (item.notes.isNotBlank()) {
                                        Text(" (${item.notes})", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Trade Setup Plan (Strict 1:1 Risk-to-Reward)
            if (detection.entryPrice > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Algorithmic Trading Plan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Box(
                                    modifier = Modifier
                                        .background(BullishGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("1:1 Fixed Risk-Reward", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Entry Reference", fontSize = 10.sp, color = TextSecondary)
                                    Text("₹${String.format("%.2f", detection.entryPrice)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Column {
                                    Text("Stop Loss", fontSize = 10.sp, color = TextSecondary)
                                    Text("₹${String.format("%.2f", detection.stopLossPrice)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BearishRed)
                                }
                                Column {
                                    Text("Target (1:1)", fontSize = 10.sp, color = TextSecondary)
                                    Text("₹${String.format("%.2f", detection.targetPrice)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = DarkSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Suggested Position Size", fontSize = 11.sp, color = TextSecondary)
                                Text("${detection.recommendedPositionSize} Shares", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Max Risk Exposure", fontSize = 11.sp, color = TextSecondary)
                                Text("₹${String.format("%.2f", detection.riskAmount)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
                            }
                        }
                    }
                }
            }

            // 4. Candle Geometry Audit
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Candle Geometry Measurements", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        AuditRow("Upper Wick / Body Ratio", "${String.format("%.2f", detection.geometry.upperWickToBodyRatio)}x", "Min >= 2.0x", detection.geometry.upperWickToBodyRatio >= 2.0)
                        AuditRow("Lower Wick / Range Ratio", "${(detection.geometry.lowerWickToRangeRatio * 100).toInt()}%", "Max <= 10%", detection.geometry.lowerWickToRangeRatio <= 0.10)
                        AuditRow("Body / Range Ratio", "${(detection.geometry.bodyToRangeRatio * 100).toInt()}%", "Max <= 35%", detection.geometry.bodyToRangeRatio <= 0.35)
                        AuditRow("Body Position from Low", "${(detection.geometry.bodyPosition * 100).toInt()}%", "Max <= 40%", detection.geometry.bodyPosition <= 0.40)
                        AuditRow("Range / ATR Ratio", "${String.format("%.2f", detection.geometry.rangeToAtrRatio)}x", "Min >= 0.50x", detection.geometry.rangeToAtrRatio >= 0.50)
                    }
                }
            }

            // 5. Context Verification
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Context & Confirmation Verification", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        ContextAuditItem("Downtrend Context", detection.trend.summaryExplanation, detection.trend.isDowntrendConfirmed)
                        ContextAuditItem("Support Proximity", detection.support.explanation, detection.support.isNearSupport)
                        ContextAuditItem("Volume Analysis", detection.volume.explanation, detection.volume.isConfirmed)
                        ContextAuditItem("Confirmation Status", detection.confirmation.explanation, detection.confirmation.isConfirmed)
                    }
                }
            }

            // 6. Institutional Explainability Checklist
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Institutional Explainability Audit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        detection.explainabilityBulletPoints.forEach { point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BullishGreen,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(top = 2.dp)
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
fun ContextAuditItem(title: String, description: String, isPassed: Boolean) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .background(if (isPassed) BullishGreen.copy(alpha = 0.15f) else DarkSurfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (isPassed) "PASSED" else "NEUTRAL / ADVISORY",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPassed) BullishGreen else TextSecondary
                )
            }
        }
        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 10.sp, color = TextSecondary)
        }
        Divider(color = DarkSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}
