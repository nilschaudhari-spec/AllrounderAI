package com.allrounder.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.allrounder.ai.data.network.ApiClient
import com.allrounder.ai.domain.model.InverseHammerDetectionDetailDto
import com.allrounder.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InverseHammerScannerScreen(
    onBack: () -> Unit,
    onSelectDetection: (InverseHammerDetectionDetailDto) -> Unit,
    onOpenChart: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("All") }
    var selectedExchange by remember { mutableStateOf("ALL") }
    var minScoreFilter by remember { mutableStateOf(55.0) }
    var searchQuery by remember { mutableStateOf("") }
    var detections by remember { mutableStateOf<List<InverseHammerDetectionDetailDto>>(emptyList()) }
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
                "Candidates" -> "INVERSE_HAMMER_CANDIDATE"
                else -> "ALL"
            }

            val resp = withContext(Dispatchers.IO) {
                ApiClient.apiService.getInverseHammerScanner(
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
                    ApiClient.apiService.getInverseHammerScanner(minScore = minScoreFilter)
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
                            Text("Inverse Hammer Scanner", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        }
                        Text("Authoritative Institutional Pattern Engine", fontSize = 10.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings", tint = AccentOrange)
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(${filteredList.size} matches)", fontSize = 9.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detections List
            if (isLoading && detections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching Inverse Hammer setups found", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList) { item ->
                        InverseHammerScanCard(
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
fun InverseHammerScanCard(
    item: InverseHammerDetectionDetailDto,
    onClick: () -> Unit,
    onOpenChart: () -> Unit
) {
    val statusColor = when (item.patternStatus) {
        "BUY" -> BullishGreen
        "INVERSE_HAMMER_CONFIRMED" -> BullishGreen
        "INVERSE_HAMMER_CANDIDATE" -> AccentBlue
        "INVERSE_HAMMER_GEOMETRY_ONLY" -> AccentOrange
        else -> NeutralGray
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Symbol, Exchange, Quality Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.tradingSymbol, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(item.exchange, fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(item.timeframe, fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                }

                // Quality Score Badge
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${item.score.finalScore.toInt()}/100",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metrics Row: Upper Wick Ratio, Lower Wick Ratio, Body Position, Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("UPPER WICK", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.1f", item.geometry.upperWickToBodyRatio)}x body", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Column {
                    Text("LOWER WICK", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${(item.geometry.lowerWickToRangeRatio * 100).toInt()}% range", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Column {
                    Text("BODY POS", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${(item.geometry.bodyPosition * 100).toInt()}% from low", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Column {
                    Text("VOLUME", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(item.volume.volumeClassification, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (item.volume.volumeRatio >= 1.5) BullishGreen else TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1:1 Risk Reward Trade Plan Strip
            if (item.entryPrice > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Entry: ₹${String.format("%.2f", item.entryPrice)}", fontSize = 10.sp, color = TextSecondary)
                    Text("SL: ₹${String.format("%.2f", item.stopLossPrice)}", fontSize = 10.sp, color = BearishRed)
                    Text("Target (1:1): ₹${String.format("%.2f", item.targetPrice)}", fontSize = 10.sp, color = BullishGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer Row: Status Pill & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        item.patternStatus.replace("_", " "),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenChart, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chart", fontSize = 11.sp, color = AccentBlue)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
