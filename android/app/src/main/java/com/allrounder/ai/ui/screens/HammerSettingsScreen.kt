package com.allrounder.ai.ui.screens

import androidx.compose.foundation.background
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
import com.allrounder.ai.domain.model.HammerConfigDto
import com.allrounder.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HammerSettingsScreen(
    onBack: () -> Unit
) {
    var config by remember { mutableStateOf(HammerConfigDto()) }
    var selectedPreset by remember { mutableStateOf("Balanced") }
    var isLoading by remember { mutableStateOf(true) }
    var saveStatus by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val presets = listOf("Strict", "Balanced", "Early Entry", "Custom")

    LaunchedEffect(Unit) {
        try {
            val resp = withContext(Dispatchers.IO) {
                ApiClient.apiService.getHammerConfig()
            }
            if (resp.isSuccessful && resp.body() != null) {
                config = resp.body()!!
            }
        } catch (_: Exception) {}
        finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hammer Strategy Parameters", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        ApiClient.apiService.resetHammerConfig()
                                    }
                                    config = HammerConfigDto()
                                    selectedPreset = "Balanced"
                                    saveStatus = "Reset to Defaults"
                                } catch (_: Exception) {}
                            }
                        }
                    ) {
                        Text("Reset", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            Surface(color = DarkSurface, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (saveStatus.isNotEmpty()) {
                        Text(saveStatus, color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val resp = withContext(Dispatchers.IO) {
                                        ApiClient.apiService.updateHammerConfig(config)
                                    }
                                    if (resp.isSuccessful) {
                                        saveStatus = "Configuration Saved Successfully!"
                                    }
                                } catch (e: Exception) {
                                    saveStatus = "Saved locally"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Preset Selection Row
                item {
                    Text("STRATEGY PRESETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            val isSel = selectedPreset == preset
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedPreset = preset
                                    when (preset) {
                                        "Strict" -> {
                                            config.minLowerWickToBodyRatio = 2.5
                                            config.maxUpperWickToRangeRatio = 0.05
                                            config.maxBodyToRangeRatio = 0.30
                                            config.minBodyPosition = 0.65
                                            config.requireDowntrend = true
                                            config.trendMode = "Strict"
                                            config.requireSupportProximity = true
                                            config.requireVolumeConfirmation = true
                                            config.volumeRequirementMode = "Mandatory"
                                            config.requireNextCandleConfirmation = true
                                            config.scoreThreshold = 85.0
                                        }
                                        "Balanced" -> {
                                            config = HammerConfigDto()
                                        }
                                        "Early Entry" -> {
                                            config.minLowerWickToBodyRatio = 1.8
                                            config.maxUpperWickToRangeRatio = 0.15
                                            config.confirmationMode = "Intrabar"
                                            config.requireDowntrend = true
                                            config.trendMode = "Relaxed"
                                            config.requireSupportProximity = false
                                            config.requireVolumeConfirmation = false
                                            config.requireNextCandleConfirmation = false
                                            config.scoreThreshold = 55.0
                                        }
                                        else -> {}
                                    }
                                },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // 1. Candle Geometry Thresholds
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("1. CANDLE GEOMETRY RULES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                            Spacer(modifier = Modifier.height(10.dp))

                            SliderRow("Min Lower Wick / Body Ratio", "${config.minLowerWickToBodyRatio}×", config.minLowerWickToBodyRatio.toFloat(), 1.0f, 5.0f) {
                                config.minLowerWickToBodyRatio = Math.round(it * 10.0) / 10.0
                                selectedPreset = "Custom"
                            }

                            SliderRow("Max Upper Wick / Range", "${(config.maxUpperWickToRangeRatio * 100).toInt()}%", config.maxUpperWickToRangeRatio.toFloat(), 0.01f, 0.30f) {
                                config.maxUpperWickToRangeRatio = Math.round(it * 100.0) / 100.0
                                selectedPreset = "Custom"
                            }

                            SliderRow("Max Body / Range", "${(config.maxBodyToRangeRatio * 100).toInt()}%", config.maxBodyToRangeRatio.toFloat(), 0.10f, 0.50f) {
                                config.maxBodyToRangeRatio = Math.round(it * 100.0) / 100.0
                                selectedPreset = "Custom"
                            }

                            SliderRow("Min Body Position", "${(config.minBodyPosition * 100).toInt()}%", config.minBodyPosition.toFloat(), 0.40f, 0.90f) {
                                config.minBodyPosition = Math.round(it * 100.0) / 100.0
                                selectedPreset = "Custom"
                            }
                        }
                    }
                }

                // 2. Trend & Context Settings
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("2. TREND & CONTEXT RULES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Require Downtrend Context", fontSize = 12.sp, color = TextPrimary)
                                Switch(
                                    checked = config.requireDowntrend,
                                    onCheckedChange = { config.requireDowntrend = it; selectedPreset = "Custom" },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AccentOrange)
                                )
                            }

                            if (config.requireDowntrend) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Trend Filter Mode: ${config.trendMode}", fontSize = 11.sp, color = TextSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Strict", "Moderate", "Relaxed").forEach { mode ->
                                        FilterChip(
                                            selected = config.trendMode == mode,
                                            onClick = { config.trendMode = mode; selectedPreset = "Custom" },
                                            label = { Text(mode, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Support & Volume Filters
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("3. SUPPORT & VOLUME FILTERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Require Support Proximity", fontSize = 12.sp, color = TextPrimary)
                                Switch(
                                    checked = config.requireSupportProximity,
                                    onCheckedChange = { config.requireSupportProximity = it; selectedPreset = "Custom" },
                                    colors = SwitchDefaults.colors(checkedThumbColor = BullishGreen)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Require Volume Confirmation", fontSize = 12.sp, color = TextPrimary)
                                Switch(
                                    checked = config.requireVolumeConfirmation,
                                    onCheckedChange = { config.requireVolumeConfirmation = it; selectedPreset = "Custom" },
                                    colors = SwitchDefaults.colors(checkedThumbColor = BullishGreen)
                                )
                            }
                        }
                    }
                }

                // 4. Confirmation Modes & Quality Threshold
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("4. CONFIRMATION & SCORING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Confirmation Mode", fontSize = 11.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = config.confirmationMode == "CandleClose",
                                    onClick = { config.confirmationMode = "CandleClose"; selectedPreset = "Custom" },
                                    label = { Text("Candle Close (Finalized)") }
                                )
                                FilterChip(
                                    selected = config.confirmationMode == "Intrabar",
                                    onClick = { config.confirmationMode = "Intrabar"; selectedPreset = "Custom" },
                                    label = { Text("Intrabar (Live Developing)") }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            SliderRow("Quality Score Threshold", "${config.scoreThreshold.toInt()}/100", config.scoreThreshold.toFloat(), 50.0f, 95.0f) {
                                config.scoreThreshold = Math.round(it).toDouble()
                                selectedPreset = "Custom"
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SliderRow(label: String, valueStr: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(valueStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
        )
    }
}
