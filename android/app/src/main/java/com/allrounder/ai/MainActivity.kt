package com.allrounder.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.allrounder.ai.data.network.ApiClient
import com.allrounder.ai.domain.model.PaperOrderRequest
import com.allrounder.ai.domain.model.Signal
import com.allrounder.ai.ui.screens.*
import com.allrounder.ai.ui.theme.AllRounderAiTheme
import com.allrounder.ai.ui.theme.DarkBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AllRounderAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()
                    var currentSignal by remember {
                        mutableStateOf(
                            Signal(
                                signalId = "s1",
                                tradingSymbol = "NIFTY 50",
                                exchange = "NSE",
                                strategyId = "DAILY_INTRADAY_5M",
                                strategyName = "Daily Intraday Technique (5-Minute)",
                                pptSlideReference = "Slides 59-62",
                                signalType = "BUY",
                                status = "CONFIRMED",
                                timeframe = "5m",
                                entryPrice = 24350.0,
                                targetPrice = 24390.0,
                                stopLossPrice = 24310.0,
                                riskRewardRatio = 1.0,
                                qualityScore = 96.0,
                                marketTrend = "Bullish Reversal Confluence",
                                confirmationMode = "CANDLE_CLOSE",
                                analysisSummary = "Supertrend(20,2) Bullish, Price > VWAP, MACD Bullish crossover on 5m chart. Strict 1:1 Target & Stop Loss configured."
                            )
                        )
                    }
                    var currentSymbol by remember { mutableStateOf("NIFTY 50") }
                    var currentHammerDetection by remember { mutableStateOf(com.allrounder.ai.domain.model.HammerDetectionDetailDto()) }
                    var currentInverseHammerDetection by remember { mutableStateOf(com.allrounder.ai.domain.model.InverseHammerDetectionDetailDto()) }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToSignals = { navController.navigate("signals") },
                                onNavigateToScanner = { navController.navigate("scanner") },
                                onNavigateToStrategies = { navController.navigate("strategies") },
                                onNavigateToChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                },
                                onNavigateToOptions = { navController.navigate("options") },
                                onNavigateToBacktest = { navController.navigate("backtest") },
                                onNavigateToPaperTrading = { navController.navigate("papertrading") },
                                onSelectSignal = { signal ->
                                    currentSignal = signal
                                    navController.navigate("signal_detail")
                                }
                            )
                        }

                        composable("strategies") {
                            StrategiesScreen(
                                onBack = { navController.popBackStack() },
                                onSelectStrategy = { strat ->
                                    if (strat.strategyId.equals("HAMMER", ignoreCase = true)) {
                                        navController.navigate("hammer_scanner")
                                    } else if (strat.strategyId.equals("INVERSE_HAMMER", ignoreCase = true)) {
                                        navController.navigate("inverse_hammer_scanner")
                                    } else {
                                        navController.navigate("scanner")
                                    }
                                },
                                onOpenChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                },
                                onSelectSignal = { signal ->
                                    currentSignal = signal
                                    navController.navigate("signal_detail")
                                }
                            )
                        }

                        composable("hammer_scanner") {
                            HammerScannerScreen(
                                onBack = { navController.popBackStack() },
                                onSelectDetection = { detection ->
                                    currentHammerDetection = detection
                                    navController.navigate("hammer_analysis")
                                },
                                onOpenChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                },
                                onOpenSettings = {
                                    navController.navigate("hammer_settings")
                                }
                            )
                        }

                        composable("hammer_analysis") {
                            HammerAnalysisScreen(
                                detection = currentHammerDetection,
                                onBack = { navController.popBackStack() },
                                onOpenChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                }
                            )
                        }

                        composable("hammer_settings") {
                            HammerSettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("inverse_hammer_scanner") {
                            InverseHammerScannerScreen(
                                onBack = { navController.popBackStack() },
                                onSelectDetection = { detection ->
                                    currentInverseHammerDetection = detection
                                    navController.navigate("inverse_hammer_analysis")
                                },
                                onOpenChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                },
                                onOpenSettings = {
                                    navController.navigate("inverse_hammer_settings")
                                }
                            )
                        }

                        composable("inverse_hammer_analysis") {
                            InverseHammerAnalysisScreen(
                                detection = currentInverseHammerDetection,
                                onBack = { navController.popBackStack() },
                                onOpenChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                }
                            )
                        }

                        composable("inverse_hammer_settings") {
                            InverseHammerSettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("signals") {
                            MarketScannerScreen(
                                onBack = { navController.popBackStack() },
                                onSelectSignal = { signal ->
                                    currentSignal = signal
                                    navController.navigate("signal_detail")
                                },
                                onOpenChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                }
                            )
                        }

                        composable("signal_detail") {
                            SignalDetailScreen(
                                signal = currentSignal,
                                onBack = { navController.popBackStack() },
                                onOpenChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                },
                                onExecutePaperTrade = { sig ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            val req = PaperOrderRequest(
                                                tradingSymbol = sig.tradingSymbol,
                                                exchange = sig.exchange,
                                                signalType = sig.signalType,
                                                quantity = 15,
                                                entryPrice = sig.entryPrice,
                                                targetPrice = sig.targetPrice,
                                                stopLossPrice = sig.stopLossPrice,
                                                strategyName = sig.strategyName
                                            )
                                            ApiClient.apiService.placePaperOrder(req, 15)
                                        } catch (_: Exception) {}
                                    }
                                    navController.navigate("papertrading")
                                }
                            )
                        }

                        composable("scanner") {
                            MarketScannerScreen(
                                onBack = { navController.popBackStack() },
                                onSelectSignal = { signal ->
                                    currentSignal = signal
                                    navController.navigate("signal_detail")
                                },
                                onOpenChart = { symbol ->
                                    currentSymbol = symbol
                                    navController.navigate("chart")
                                }
                            )
                        }

                        composable("chart") {
                            InteractiveChartScreen(
                                symbol = currentSymbol,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("options") {
                            OptionsStrategyScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("backtest") {
                            BacktestScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("papertrading") {
                            PaperTradingScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
