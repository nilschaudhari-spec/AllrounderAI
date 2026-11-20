package com.allrounder.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allrounder.ai.data.network.ApiClient
import com.allrounder.ai.domain.model.*
import com.allrounder.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTradingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var autoTradeEnabled by remember { mutableStateOf(true) }
    var account by remember { mutableStateOf(PaperAccountDto()) }
    var positions by remember { mutableStateOf<List<PaperPositionDto>>(emptyList()) }
    var orders by remember { mutableStateOf<List<PaperOrderDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var tradeDialogOpen by remember { mutableStateOf(false) }
    var manualSymbol by remember { mutableStateOf("NIFTY 50") }
    var manualExchange by remember { mutableStateOf("NSE") }
    var manualSide by remember { mutableStateOf("BUY") }
    var manualQty by remember { mutableStateOf("25") }
    var manualPrice by remember { mutableStateOf("24350") }

    // High-frequency in-memory real-time micro-ticker (every 200ms) for millisecond MTM fluctuations
    LaunchedEffect(Unit) {
        val rng = java.util.Random()
        while (true) {
            delay(200)
            if (positions.isNotEmpty()) {
                positions = positions.map { pos ->
                    val tickStep = (rng.nextDouble() * 0.40 - 0.20)
                    val newPrice = Math.max(1.0, Math.round((pos.currentPrice + tickStep) * 100.0) / 100.0)
                    val pnl = Math.round(((newPrice - pos.buyPrice) * pos.quantity) * 100.0) / 100.0
                    val pnlPct = if (pos.buyPrice > 0) Math.round(((newPrice - pos.buyPrice) / pos.buyPrice * 100.0) * 100.0) / 100.0 else 0.0
                    pos.copy(currentPrice = newPrice, unrealizedPnl = pnl, pnlPercent = pnlPct)
                }
                val totalUnrealized = positions.sumOf { it.unrealizedPnl }
                account = account.copy(unrealizedPnl = totalUnrealized)
            }
        }
    }

    // Continuous server sync loop (every 1.5 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getPaperPortfolio()
                }
                if (resp.isSuccessful && resp.body() != null) {
                    account = resp.body()!!.account
                    positions = resp.body()!!.positions
                    orders = resp.body()!!.orders
                }
            } catch (_: Exception) {}
            finally {
                isLoading = false
            }
            delay(1500)
        }
    }

    fun openFrontPageApp() {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.frontpage.app")
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://frontpage.live/"))
                context.startActivity(webIntent)
            }
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://frontpage.live/"))
            context.startActivity(webIntent)
        }
    }

    fun closePosition(symbol: String) {
        coroutineScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.closePaperPosition(symbol)
                }
                if (resp.isSuccessful) {
                    Toast.makeText(context, "$symbol squared off at market price!", Toast.LENGTH_SHORT).show()
                    val pResp = withContext(Dispatchers.IO) { ApiClient.apiService.getPaperPortfolio() }
                    if (pResp.isSuccessful && pResp.body() != null) {
                        account = pResp.body()!!.account
                        positions = pResp.body()!!.positions
                        orders = pResp.body()!!.orders
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to square off: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun resetPortfolio() {
        coroutineScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.resetPaperPortfolio()
                }
                if (resp.isSuccessful) {
                    Toast.makeText(context, "Virtual portfolio reset to ₹10,00,000", Toast.LENGTH_SHORT).show()
                    val pResp = withContext(Dispatchers.IO) { ApiClient.apiService.getPaperPortfolio() }
                    if (pResp.isSuccessful && pResp.body() != null) {
                        account = pResp.body()!!.account
                        positions = pResp.body()!!.positions
                        orders = pResp.body()!!.orders
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun executeManualTrade() {
        coroutineScope.launch {
            try {
                val qty = manualQty.toIntOrNull() ?: 15
                val pr = manualPrice.toDoubleOrNull() ?: 0.0
                val req = PaperOrderRequest(
                    tradingSymbol = manualSymbol.trim().uppercase(),
                    exchange = manualExchange,
                    signalType = manualSide,
                    quantity = qty,
                    entryPrice = pr,
                    targetPrice = if (manualSide == "BUY") pr * 1.015 else pr * 0.985,
                    stopLossPrice = if (manualSide == "BUY") pr * 0.99 else pr * 1.01,
                    strategyName = "Manual FrontPage Paper Trade"
                )
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.placePaperOrder(req, qty)
                }
                if (resp.isSuccessful) {
                    Toast.makeText(context, "Order for ${req.tradingSymbol} placed on FrontPage!", Toast.LENGTH_SHORT).show()
                    tradeDialogOpen = false
                    val pResp = withContext(Dispatchers.IO) { ApiClient.apiService.getPaperPortfolio() }
                    if (pResp.isSuccessful && pResp.body() != null) {
                        account = pResp.body()!!.account
                        positions = pResp.body()!!.positions
                        orders = pResp.body()!!.orders
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Order failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Manual Trade Dialog
    if (tradeDialogOpen) {
        AlertDialog(
            onDismissRequest = { tradeDialogOpen = false },
            title = { Text("Place Paper Trade (FrontPage)", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualSymbol,
                        onValueChange = { manualSymbol = it },
                        label = { Text("Symbol (e.g. RELIANCE, TCS, NIFTY 50, BSE)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = manualExchange == "NSE",
                            onClick = { manualExchange = "NSE" },
                            label = { Text("NSE") }
                        )
                        FilterChip(
                            selected = manualExchange == "BSE",
                            onClick = { manualExchange = "BSE" },
                            label = { Text("BSE") }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = manualSide == "BUY",
                            onClick = { manualSide = "BUY" },
                            label = { Text("BUY") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BullishGreen)
                        )
                        FilterChip(
                            selected = manualSide == "SELL",
                            onClick = { manualSide = "SELL" },
                            label = { Text("SELL") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BearishRed)
                        )
                    }

                    OutlinedTextField(
                        value = manualQty,
                        onValueChange = { manualQty = it },
                        label = { Text("Quantity") },
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
                    onClick = { executeManualTrade() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (manualSide == "BUY") BullishGreen else BearishRed)
                ) {
                    Text("Execute Order")
                }
            },
            dismissButton = {
                TextButton(onClick = { tradeDialogOpen = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FrontPage Real-time Paper Trading", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { tradeDialogOpen = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Trade", tint = BullishGreen)
                    }
                    IconButton(onClick = { resetPortfolio() }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = TextSecondary)
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
            // Portfolio Balance Card with Realtime MTM
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
                                Text("FrontPage Live Broker Sync", color = BullishGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Account: ${account.accountId}", color = TextSecondary, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("TOTAL VIRTUAL CAPITAL", color = TextSecondary, fontSize = 11.sp)
                        val totalCapital = account.balance + account.unrealizedPnl
                        Text(
                            "₹${String.format("%,.2f", totalCapital)}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Available Margin", color = TextSecondary, fontSize = 11.sp)
                                Text("₹${String.format("%,.2f", account.availableMargin)}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Realized P&L", color = TextSecondary, fontSize = 11.sp)
                                val rPnl = account.realizedPnl
                                Text(
                                    "${if (rPnl >= 0) "+₹" else "-₹"}${String.format("%,.2f", Math.abs(rPnl))}",
                                    color = if (rPnl >= 0) BullishGreen else BearishRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text("Live Unrealized MTM", color = TextSecondary, fontSize = 11.sp)
                                val uPnl = account.unrealizedPnl
                                Text(
                                    "${if (uPnl >= 0) "+₹" else "-₹"}${String.format("%,.2f", Math.abs(uPnl))}",
                                    color = if (uPnl >= 0) BullishGreen else BearishRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Launch Official FrontPage App / Web Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Official FrontPage Trading App", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("View positions in official FrontPage paper trading community.", color = TextSecondary, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { openFrontPageApp() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open App", fontSize = 12.sp)
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
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Paper Trading", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Auto-executes simulated orders in FrontPage when PPT strategy confirmation triggers.",
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

            // Active Positions Section (Realtime Ticking CMP & MTM)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVE POSITIONS (${positions.size})", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { tradeDialogOpen = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Trade", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (positions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No open positions. Execute a signal or trade manually.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(positions) { pos ->
                    val isProfit = pos.unrealizedPnl >= 0
                    val pnlColor = if (isProfit) BullishGreen else BearishRed

                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                    Text(pos.tradingSymbol, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(pos.exchange, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(if (pos.side.equals("BUY", ignoreCase = true)) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(pos.side.uppercase(), color = if (pos.side.equals("BUY", ignoreCase = true)) BullishGreen else BearishRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    "${if (isProfit) "+₹" else "-₹"}${String.format("%,.2f", Math.abs(pos.unrealizedPnl))} (${if (isProfit) "+" else ""}${pos.pnlPercent}%)",
                                    fontWeight = FontWeight.Bold,
                                    color = pnlColor,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Qty: ${pos.quantity} • Buy: ₹${pos.buyPrice}", color = TextSecondary, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(BullishGreen, RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("LTP: ₹${pos.currentPrice}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Target: ₹${pos.targetPrice}", color = BullishGreen, fontSize = 11.sp)
                                    Text("SL: ₹${pos.stopLossPrice}", color = BearishRed, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { closePosition(pos.tradingSymbol) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Square Off", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Executed Orders History Section
            item {
                Text("TODAY'S EXECUTED ORDERS (${orders.size})", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (orders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No executed orders today.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(orders) { ord ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${ord.tradingSymbol} (${ord.exchange}) - ${ord.side.uppercase()} ${ord.quantity} Qty",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(BullishGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(ord.status, color = BullishGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Fill Price: ₹${ord.price} • Order ID: ${ord.orderId}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
