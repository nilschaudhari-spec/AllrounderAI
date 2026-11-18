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
import com.allrounder.ai.domain.model.InverseHammerConfigDto
import com.allrounder.ai.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InverseHammerSettingsScreen(
    onBack: () -> Unit
) {
    var config by remember { mutableStateOf(InverseHammerConfigDto()) }
    var selectedPreset by remember { mutableStateOf("Balanced") }
    var isLoading by remember { mutableStateOf(true) }
    var saveStatus by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val presets = listOf("Strict", "Balanced", "Early Entry", "Custom")

    LaunchedEffect(Unit) {
        try {
            val resp = withContext(Dispatchers.IO) {
                ApiClient.apiService.getInverseHammerConfig()
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
                title = { Text("Inverse Hammer Strategy Parameters", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                        onClick = {
                            scope.launch {
                                val resetResp = withContext(Dispatchers.IO) {
                                    ApiClient.apiService.resetInverseHammerConfig()
                                }
                                if (resetResp.isSuccessful) {
                                    config = InverseHammerConfigDto()
                                    selectedPreset = "Balanced"
                                    saveStatus = "Reset to factory defaults"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Reset")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                saveStatus = "Saving..."
                                val putResp = withContext(Dispatchers.IO) {
                                    ApiClient.apiService.updateInverseHammerConfig(config)
                                }
                                if (putResp.isSuccessful) {
                                    saveStatus = "Saved successfully!"
                                } else {
                                    saveStatus = "Error updating settings"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
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
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status feedback
                if (saveStatus.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(saveStatus, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Presets Chips
                item {
                    Text("Configuration Preset", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presets) { preset ->
                            val isSelected = selectedPreset.equals(preset, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .background(if (isSelected) AccentBlue else DarkSurface, RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedPreset = preset
                                        when (preset) {
                                            "Strict" -> {
                                                config = config.copy(
                                                    minUpperWickToBodyRatio = 2.5,
                                                    maxLowerWickToRangeRatio = 0.05,
                                                    maxBodyToRangeRatio = 0.30,
                                                    maxBodyPosition = 0.30,
                                                    requireDowntrend = true,
                                                    trendMode = "Strict",
                                                    requireSupportProximity = true,
                                                    requireVolumeConfirmation = true,
                                                    volumeRequirementMode = "Mandatory",
                                                    minVolumeRatio = 1.6,
                                                    scoreThreshold = 85.0
                                                )
                                            }
                                            "Early Entry" -> {
                                                config = config.copy(
                                                    minUpperWickToBodyRatio = 1.8,
                                                    maxLowerWickToRangeRatio = 0.15,
                                                    maxBodyToRangeRatio = 0.40,
                                                    maxBodyPosition = 0.45,
                                                    confirmationMode = "Intrabar",
                                                    requireSupportProximity = false,
                                                    requireVolumeConfirmation = false,
                                                    volumeRequirementMode = "Advisory",
                                                    scoreThreshold = 55.0
                                                )
                                            }
                                            "Balanced" -> {
                                                config = InverseHammerConfigDto()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    preset,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Section 1: Geometry Constraints
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("1. Candle Geometry Thresholds", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Min Upper Wick / Body Ratio
                            Text(
                                "Min Upper Wick / Body Ratio: ${String.format("%.1f", config.minUpperWickToBodyRatio)}x",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = config.minUpperWickToBodyRatio.toFloat(),
                                onValueChange = { config = config.copy(minUpperWickToBodyRatio = it.toDouble()); selectedPreset = "Custom" },
                                valueRange = 1.5f..4.0f,
                                steps = 25,
                                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                            )

                            // Max Lower Wick / Range Ratio
                            Text(
                                "Max Lower Wick / Range Ratio: ${(config.maxLowerWickToRangeRatio * 100).toInt()}%",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = config.maxLowerWickToRangeRatio.toFloat(),
                                onValueChange = { config = config.copy(maxLowerWickToRangeRatio = it.toDouble()); selectedPreset = "Custom" },
                                valueRange = 0.05f..0.25f,
                                steps = 20,
                                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                            )

                            // Max Body / Range Ratio
                            Text(
                                "Max Body / Range Ratio: ${(config.maxBodyToRangeRatio * 100).toInt()}%",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = config.maxBodyToRangeRatio.toFloat(),
                                onValueChange = { config = config.copy(maxBodyToRangeRatio = it.toDouble()); selectedPreset = "Custom" },
                                valueRange = 0.15f..0.50f,
                                steps = 35,
                                colors = SliderDefaults.colors(thumbColor = AccentBlue, activeTrackColor = AccentBlue)
                            )

                            // Max Body Position (Close to low)
                            Text(
                                "Max Body Position from Low: ${(config.maxBodyPosition * 100).toInt()}%",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = config.maxBodyPosition.toFloat(),
                                onValueChange = { config = config.copy(maxBodyPosition = it.toDouble()); selectedPreset = "Custom" },
                                valueRange = 0.20f..0.50f,
                                steps = 30,
                                colors = SliderDefaults.colors(thumbColor = AccentOrange, activeTrackColor = AccentOrange)
                            )
                        }
                    }
                }

                // Section 2: Trend & Context Filters
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("2. Trend Context & Modes", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Require Bearish Downtrend", color = TextPrimary, fontSize = 12.sp)
                                Switch(
                                    checked = config.requireDowntrend,
                                    onCheckedChange = { config = config.copy(requireDowntrend = it); selectedPreset = "Custom" }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Require Support Proximity", color = TextPrimary, fontSize = 12.sp)
                                Switch(
                                    checked = config.requireSupportProximity,
                                    onCheckedChange = { config = config.copy(requireSupportProximity = it); selectedPreset = "Custom" }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Volume Confirmation", color = TextPrimary, fontSize = 12.sp)
                                Switch(
                                    checked = config.requireVolumeConfirmation,
                                    onCheckedChange = { config = config.copy(requireVolumeConfirmation = it); selectedPreset = "Custom" }
                                )
                            }
                        }
                    }
                }

                // Section 3: Confirmation & Scoring Thresholds
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("3. Confirmation & Minimum Quality", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Confirmation Mode: ${config.confirmationMode}", color = TextSecondary, fontSize = 11.sp)
                            Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("CandleClose", "Intrabar").forEach { mode ->
                                    val isMode = config.confirmationMode.equals(mode, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .background(if (isMode) AccentBlue else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                            .clickable {
                                                config = config.copy(confirmationMode = mode)
                                                selectedPreset = "Custom"
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            mode,
                                            color = if (isMode) Color.White else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Minimum Quality Score Threshold: ${config.scoreThreshold.toInt()}/100",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Slider(
                                value = config.scoreThreshold.toFloat(),
                                onValueChange = { config = config.copy(scoreThreshold = it.toDouble()); selectedPreset = "Custom" },
                                valueRange = 50f..90f,
                                steps = 40,
                                colors = SliderDefaults.colors(thumbColor = BullishGreen, activeTrackColor = BullishGreen)
                            )
                        }
                    }
                }
            }
        }
    }
}
