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
import com.allrounder.ai.domain.model.HammerDetectionDetailDto
import com.allrounder.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HammerScannerScreen(
    onBack: () -> Unit,
    onSelectDetection: (HammerDetectionDetailDto) -> Unit,
    onOpenChart: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("All") }
    var selectedExchange by remember { mutableStateOf("ALL") }
    var minScoreFilter by remember { mutableStateOf(55.0) }
    var searchQuery by remember { mutableStateOf("") }
    var detections by remember { mutableStateOf<List<HammerDetectionDetailDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val tabs = listOf("All", "Cash", "F&O", "Indices", "Confirmed", "Candidates")
    val exchanges = listOf("ALL", "NSE", "BSE")

    // Fetch scanner results from authoritative backend API
    LaunchedEffect(selectedTab, selectedExchange, minScoreFilter) {
        isLoading = true
        try {
            val scopeParam = when (selectedTab) {
                "Cash" -> "CASH"
                "F&O" -> "FNO"
                "Indices" -> "INDICES"
                else -> "ALL"
            }
            val statusParam = when (selectedTab) {
                "Confirmed" -> "BUY"
                "Candidates" -> "HAMMER_CANDIDATE"
                else -> "ALL"
            }

            val resp = withContext(Dispatchers.IO) {
                ApiClient.apiService.getHammerScanner(
                    scope = scopeParam,
                    exchange = if (selectedExchange == "ALL") null else selectedExchange,
                    status = statusParam,
                    minScore = minScoreFilter
                )
            }

            if (resp.isSuccessful && resp.body() != null) {
                detections = resp.body()!!.results
            }
        } catch (_: Exception) {
            // Handled gracefully
        } finally {
            isLoading = false
        }
    }

    // Auto-refresh polling loop every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getHammerScanner(minScore = minScoreFilter)
                }
                if (resp.isSuccessful && resp.body() != null && resp.body()!!.results.isNotEmpty()) {
                    detections = resp.body()!!.results
                }
            } catch (_: Exception) {}
        }
    }

    val filteredList = detections.filter { item ->
        searchQuery.isBlank() ||
                item.tradingSymbol.contains(searchQuery, ignoreCase = true) ||
                item.patternStatus.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hardware, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hammer Reversal Scanner", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        }
                        Text("Authoritative Institutional Pattern Engine", fontSize = 10.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Tune, contentDescription = "Hammer Settings", tint = AccentOrange)
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search stocks in scanner...", fontSize = 12.sp, color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Category Tabs Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tabs) { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = { Text(tab, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Exchange & Min Score Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("EXCHANGE:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    exchanges.forEach { ex ->
                        val isExSelected = selectedExchange == ex
                        Box(
                            modifier = Modifier
                                .background(if (isExSelected) AccentOrange else DarkSurface, RoundedCornerShape(4.dp))
                                .clickable { selectedExchange = ex }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(ex, color = if (isExSelected) Color.White else TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SCORE ≥ ${minScoreFilter.toInt()}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Results Counter Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETECTED SETUPS (${filteredList.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text(
                    text = "Live Auto-Refresh (5s)",
                    fontSize = 10.sp,
                    color = BullishGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Radar, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching Hammer patterns found for current filters", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList) { item ->
                        HammerResultCard(
                            item = item,
                            onClick = { onSelectDetection(item) },
                            onOpenChart = { onOpenChart(item.tradingSymbol) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HammerResultCard(
    item: HammerDetectionDetailDto,
    onClick: () -> Unit,
    onOpenChart: () -> Unit
) {
    val statusColor = when (item.patternStatus) {
        "BUY" -> BullishGreen
        "HAMMER_CONFIRMED" -> BullishGreen
        "HAMMER_CANDIDATE" -> AccentBlue
        "HAMMER_GEOMETRY_ONLY" -> AccentOrange
        else -> NeutralGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Symbol + Exchange + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.tradingSymbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.exchange, color = AccentBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.timeframe, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.patternStatus,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics Matrix Row: Score + Wick Ratio + Volume + Support
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("QUALITY SCORE", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${item.score.finalScore}/100", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LOWER WICK", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${item.geometry.lowerWickToBodyRatio}× Body", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VOLUME", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${item.volume.volumeRatio}× (${item.volume.volumeClassification})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SUPPORT", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(if (item.support.isNearSupport) "${item.support.distanceInAtr} ATR" else "None", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (item.support.isNearSupport) BullishGreen else TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trading Plan Row (if BUY or CONFIRMED)
            if (item.entryPrice > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Entry: ₹${item.entryPrice}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Target: ₹${item.targetPrice} (1:1)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                        Text("SL: ₹${item.stopLossPrice}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BearishRed)
                    }

                    TextButton(onClick = onOpenChart, contentPadding = PaddingValues(0.dp)) {
                        Text("Chart", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
