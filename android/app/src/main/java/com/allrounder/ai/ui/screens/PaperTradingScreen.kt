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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTradingScreen(
    onBack: () -> Unit
) {
    var autoTradeEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FrontPage Paper Trading", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            // Portfolio Balance Card
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(BullishGreen, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("FrontPage Connected", color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Account: FP-88291", color = TextSecondary, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("TOTAL VIRTUAL CAPITAL", color = TextSecondary, fontSize = 11.sp)
                        Text("₹10,42,150.00", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Available Margin", color = TextSecondary, fontSize = 11.sp)
                                Text("₹8,94,250.00", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Realized P&L", color = TextSecondary, fontSize = 11.sp)
                                Text("+₹42,150.00", color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Unrealized P&L", color = TextSecondary, fontSize = 11.sp)
                                Text("+₹3,400.00", color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Auto-Trade Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Paper Trading", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Automatically executes simulated orders in FrontPage when PPT strategy confirmation matches.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoTradeEnabled,
                            onCheckedChange = { autoTradeEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = BullishGreen)
                        )
                    }
                }
            }

            // Active Positions
            item {
                Text("ACTIVE POSITIONS (1)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("BANKNIFTY 51200 CE", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("+₹3,400.00 (+5.1%)", fontWeight = FontWeight.Bold, color = BullishGreen, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Qty: 30 (2 Lots) • Avg: ₹245.50", color = TextSecondary, fontSize = 12.sp)
                            Text("LTP: ₹258.00", color = TextPrimary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Target (1:1): ₹285.50", color = BullishGreen, fontSize = 11.sp)
                            Text("Stop Loss: ₹205.50", color = BearishRed, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Executed Orders History
            item {
                Text("TODAY'S EXECUTED ORDERS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NIFTY 50 Future - BUY 50 Qty", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("FILLED", color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Triggered by: Daily Intraday 5M Strategy (Slide 60)", color = AccentOrange, fontSize = 11.sp)
                        Text("Fill Price: ₹24,350.00 • 09:45:00 IST", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
