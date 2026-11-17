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

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToSignals = { navController.navigate("signals") },
                                onNavigateToScanner = { navController.navigate("scanner") },
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

                        composable("signals") {
                            MarketScannerScreen(
                                onBack = { navController.popBackStack() },
                                onSelectSignal = { signal ->
                                    currentSignal = signal
                                    navController.navigate("signal_detail")
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
                                onExecutePaperTrade = {
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
