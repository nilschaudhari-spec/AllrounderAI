package com.allrounder.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import com.allrounder.ai.domain.model.Signal
import com.allrounder.ai.domain.model.StrategyInfo
import com.allrounder.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategiesScreen(
    onBack: () -> Unit,
    onSelectStrategy: (StrategyInfo) -> Unit,
    onOpenChart: (String) -> Unit,
    onSelectSignal: (Signal) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var strategies by remember { mutableStateOf<List<StrategyInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val categories = listOf("ALL", "INTRADAY_5M", "CANDLESTICK", "BREAKOUT", "INDICATOR", "OPTIONS_HEDGE")

    // Fetch strategies from backend or fallback to PPT catalog
    LaunchedEffect(Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.getStrategies()
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                strategies = response.body()!!
            } else {
                strategies = FallbackStrategies
            }
        } catch (e: Exception) {
            strategies = FallbackStrategies
        } finally {
            isLoading = false
        }
    }

    val filteredStrategies = strategies.filter { strat ->
        val matchesCategory = selectedCategory == "ALL" || strat.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                strat.name.contains(searchQuery, ignoreCase = true) ||
                strat.pptSlideReference.contains(searchQuery, ignoreCase = true) ||
                strat.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trading Strategies Hub", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                },
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search strategies (e.g. 5M, Hammer, Golden Cross...)", fontSize = 13.sp, color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val displayLabel = when (cat) {
                        "ALL" -> "All Strategies"
                        "INTRADAY_5M" -> "5M Intraday"
                        "CANDLESTICK" -> "Candlestick"
                        "BREAKOUT" -> "Breakouts"
                        "INDICATOR" -> "Indicators"
                        "OPTIONS_HEDGE" -> "Options Hedging"
                        else -> cat
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(displayLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REGISTERED STRATEGIES (${filteredStrategies.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Text(
                    text = "Risk-Reward 1:1 Strict",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentOrange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredStrategies) { strategy ->
                        StrategyCard(
                            strategy = strategy,
                            onOpenChart = { onOpenChart("NIFTY 50") },
                            onSelect = { onSelectStrategy(strategy) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StrategyCard(
    strategy: StrategyInfo,
    onOpenChart: () -> Unit,
    onSelect: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val directionColor = when (strategy.direction.uppercase()) {
        "BULLISH" -> BullishGreen
        "BEARISH" -> BearishRed
        else -> AccentOrange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge + PPT Slide Badge + Direction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = strategy.category,
                            color = AccentBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(AccentOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = strategy.pptSlideReference,
                            color = AccentOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(directionColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = strategy.direction,
                        color = directionColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Strategy Name
            Text(
                text = strategy.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = strategy.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats Strip: Timeframe + Win Rate + 1:1 RR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strategy.defaultTimeframe, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = BullishGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${strategy.winRateBenchmark}% Win Rate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Balance, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("1:1 Target", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                }
            }

            // Expandable Rules Checklist
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = DarkSurfaceVariant, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "STRATEGY EXECUTION RULES & FORMULA:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    strategy.rules.forEach { rule ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
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
                            Text(
                                text = rule,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenChart,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.CandlestickChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Chart", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onSelect,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Stocks", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Click hint
            if (!expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tap to view rules & scan", fontSize = 10.sp, color = AccentBlue)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

val FallbackStrategies = listOf(
    StrategyInfo(
        strategyId = "DAILY_INTRADAY_5M",
        name = "Daily Intraday 5-Minute Technique",
        category = "INTRADAY_5M",
        direction = "BULLISH",
        pptSlideReference = "Slides 59-62",
        description = "High-probability intraday trend setup combining Supertrend(20,2), VWAP support, and MACD bullish confluence on 5-minute charts.",
        defaultTimeframe = "5m",
        riskRewardRatio = 1.0,
        winRateBenchmark = 94.5,
        rules = listOf(
            "5-Minute timeframe chart execution",
            "Supertrend(20, 2) must be GREEN / Bullish",
            "Price must be trading ABOVE Volume Weighted Average Price (VWAP)",
            "MACD line (12, 26, 9) crossing or trading ABOVE the Signal line",
            "Strict 1:1 Target and Stop Loss based on signal candle range",
            "Mandatory Confirmation on candle close (no premature entry)"
        )
    ),
    StrategyInfo(
        strategyId = "LOW_BREAK_CLOSE_UPSIDE",
        name = "Low Break, Close Upside",
        category = "BREAKOUT",
        direction = "BULLISH",
        pptSlideReference = "Slides 13-14",
        description = "Previous red candle low is broken to trap weak sellers, followed by an aggressive green candle closing upside.",
        defaultTimeframe = "15m",
        riskRewardRatio = 1.0,
        winRateBenchmark = 92.0,
        rules = listOf(
            "Prior candle must be RED (bearish exhaustion)",
            "Current candle breaks prior candle low (liquidity sweep)",
            "Current green candle closes firmly ABOVE prior candle body/open",
            "7 to 10 points candle body rule satisfied for Bank Nifty / equivalent equities %",
            "Target = 1:1 distance from entry to stop loss (signal candle low)"
        )
    ),
    StrategyInfo(
        strategyId = "HAMMER",
        name = "Hammer Bottom Reversal",
        category = "CANDLESTICK",
        direction = "BULLISH",
        pptSlideReference = "Slides 5-8",
        description = "Classic bottom reversal pattern appearing after a distinct downtrend. Lower shadow is at least 2x the body size.",
        defaultTimeframe = "15m",
        riskRewardRatio = 1.0,
        winRateBenchmark = 89.0,
        rules = listOf(
            "Must form after a clear 4+ candle downtrend or at key support",
            "Lower shadow length must be >= 2x real candle body",
            "Upper shadow is very small or non-existent (<= 15% of range)",
            "Next candle must confirm with close above hammer high",
            "Target: 1:1 risk-to-reward ratio based on hammer range"
        )
    ),
    StrategyInfo(
        strategyId = "INVERSE_HAMMER",
        name = "Inverse Hammer Reversal",
        category = "CANDLESTICK",
        direction = "BULLISH",
        pptSlideReference = "Slides 11-12",
        description = "Powerful bottom reversal pattern appearing at downtrend termination. Upper shadow is at least 2x the body size with small lower shadow.",
        defaultTimeframe = "15m",
        riskRewardRatio = 1.0,
        winRateBenchmark = 88.5,
        rules = listOf(
            "Must form after a clear preceding downtrend or at support level",
            "Upper shadow length must be >= 2x real candle body",
            "Lower shadow must be small (<= 10% of total candle range)",
            "Body positioned in lower 40% of candle range",
            "Bullish confirmation on candle close or breakout of high",
            "Strict 1:1 risk-to-reward target planning"
        )
    ),
    StrategyInfo(
        strategyId = "HIGH_BREAK_CLOSE_DOWNSIDE",
        name = "High Break, Close Downside",
        category = "BREAKOUT",
        direction = "BEARISH",
        pptSlideReference = "Slides 27-28",
        description = "Previous green candle high is broken to trap buyers, followed by a strong red candle closing downside.",
        defaultTimeframe = "15m",
        riskRewardRatio = 1.0,
        winRateBenchmark = 91.5,
        rules = listOf(
            "Prior candle must be GREEN (uptrend / bullish exhaustion)",
            "Current candle pierces ABOVE prior candle high (false breakout)",
            "Current red candle closes firmly BELOW prior candle body/open",
            "7 to 10 points candle body rule satisfied",
            "Stop loss at signal candle high, Target = 1:1 RR"
        )
    ),
    StrategyInfo(
        strategyId = "MARUBOZU",
        name = "Marubozu Momentum Breakout",
        category = "CANDLESTICK",
        direction = "BULLISH",
        pptSlideReference = "Slides 33-36",
        description = "High-conviction trend candle with virtually zero wicks (< 5% of body) signaling complete institutional order flow dominance.",
        defaultTimeframe = "15m",
        riskRewardRatio = 1.0,
        winRateBenchmark = 93.0,
        rules = listOf(
            "Real body covers >= 90% of entire candle range",
            "Above-average volume (>= 1.5x 20-period volume SMA)",
            "Bullish Marubozu (Open = Low, Close = High) triggers strong long continuation",
            "Target: 1:1 risk-to-reward ratio based on candle body size"
        )
    ),
    StrategyInfo(
        strategyId = "GOLDEN_CROSS",
        name = "Golden Cross & 200 EMA Re-entry",
        category = "INDICATOR",
        direction = "BULLISH",
        pptSlideReference = "Slides 51-58",
        description = "50 EMA crosses above 200 EMA, and the subsequent pullback/retest of the 200 EMA creates a high-confidence long entry.",
        defaultTimeframe = "Daily",
        riskRewardRatio = 1.0,
        winRateBenchmark = 94.0,
        rules = listOf(
            "50 EMA crosses ABOVE 200 EMA (Golden Cross confirmation)",
            "Wait for natural price pullback towards the rising 200 EMA",
            "Bullish price action bounce on the 200 EMA level",
            "Target: 1:1 RR on swing basis, Stop Loss below 200 EMA support"
        )
    ),
    StrategyInfo(
        strategyId = "BOLLINGER_BANDS",
        name = "Bollinger Bands Mean Reversion",
        category = "INDICATOR",
        direction = "BULLISH",
        pptSlideReference = "Slides 45-50",
        description = "20-period SMA and 2.0 Standard Deviation bands. Catches extreme price stretches returning to the middle band.",
        defaultTimeframe = "15m",
        riskRewardRatio = 1.0,
        winRateBenchmark = 89.5,
        rules = listOf(
            "20-Period Bollinger Bands with 2.0 StdDev",
            "Price pierces or closes outside lower band (oversold condition)",
            "Reversal candle closes back inside the band",
            "Target: Middle band (20 SMA) or 1:1 risk-reward ratio"
        )
    ),
    StrategyInfo(
        strategyId = "OPTIONS_PROTECTIVE_PUT",
        name = "Protective Put Hedging (Slide 68)",
        category = "OPTIONS_HEDGE",
        direction = "HEDGED",
        pptSlideReference = "Slide 68",
        description = "Buy an ATM Put option against long futures or stock holdings to eliminate downside crash risk while maintaining upside.",
        defaultTimeframe = "Daily",
        riskRewardRatio = 1.0,
        winRateBenchmark = 96.0,
        rules = listOf(
            "Long Futures or Equity position",
            "Buy At-The-Money (ATM) Put Option as hedge",
            "Max Loss is capped strictly to Option Premium paid",
            "Upside profit potential remains unlimited (Synthetic Long Call)",
            "Breakeven = Entry Price + Put Premium"
        )
    )
)
