using System;
using System.Collections.Generic;
using System.Linq;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Strategies;

namespace AllRounderAi.Core.Backtesting
{
    public class BacktestEngine
    {
        public BacktestResult RunBacktest(
            IReadOnlyList<Candle> historicalCandles,
            Instrument instrument,
            IStrategyEngine strategy,
            BacktestRequest request)
        {
            var result = new BacktestResult
            {
                StrategyId = strategy.StrategyId,
                Symbol = instrument.Symbol
            };

            if (historicalCandles.Count < 50) return result;

            var trades = new List<BacktestTrade>();
            BacktestTrade? currentTrade = null;
            decimal capital = request.InitialCapital;
            decimal peakCapital = capital;
            decimal maxDrawdown = 0;
            int tradeCount = 0;

            for (int i = 30; i < historicalCandles.Count; i++)
            {
                var candle = historicalCandles[i];

                // If currently in a position, evaluate exit against target or stop-loss
                if (currentTrade != null)
                {
                    bool isLong = currentTrade.Side == SignalDirection.Buy;
                    bool targetHit = isLong ? candle.High >= currentTrade.TargetPrice : candle.Low <= currentTrade.TargetPrice;
                    bool stopLossHit = isLong ? candle.Low <= currentTrade.StopLossPrice : candle.High >= currentTrade.StopLossPrice;

                    if (targetHit || stopLossHit)
                    {
                        currentTrade.ExitTime = candle.Timestamp;
                        currentTrade.ExitPrice = targetHit ? currentTrade.TargetPrice : currentTrade.StopLossPrice;
                        currentTrade.ExitReason = targetHit ? "TargetHit" : "StopLossHit";

                        decimal priceDiff = isLong 
                            ? currentTrade.ExitPrice - currentTrade.EntryPrice 
                            : currentTrade.EntryPrice - currentTrade.ExitPrice;

                        currentTrade.GrossPnl = priceDiff * currentTrade.Quantity;
                        currentTrade.NetPnl = currentTrade.GrossPnl - (request.BrokeragePerTrade * 2m); // Round trip brokerage

                        capital += currentTrade.NetPnl;
                        if (capital > peakCapital) peakCapital = capital;
                        decimal drawdown = peakCapital - capital;
                        if (drawdown > maxDrawdown) maxDrawdown = drawdown;

                        trades.Add(currentTrade);
                        currentTrade = null;
                    }
                }

                // If no active position, check for new strategy entry signal
                if (currentTrade == null)
                {
                    var match = strategy.Evaluate(historicalCandles, instrument, request.Config, i);
                    if (match.IsMatched && match.Signal != null)
                    {
                        tradeCount++;
                        int qty = Math.Max(1, (int)(request.InitialCapital * 0.1m / match.Signal.EntryPrice));

                        currentTrade = new BacktestTrade
                        {
                            TradeNumber = tradeCount,
                            EntryTime = candle.Timestamp,
                            Side = match.Signal.SignalType,
                            EntryPrice = match.Signal.EntryPrice,
                            TargetPrice = match.Signal.TargetPrice,
                            StopLossPrice = match.Signal.StopLossPrice,
                            Quantity = qty
                        };
                    }
                }
            }

            // Calculate aggregate performance metrics
            result.Trades = trades;
            result.TotalTrades = trades.Count;
            result.WinningTrades = trades.Count(t => t.NetPnl > 0);
            result.LosingTrades = trades.Count(t => t.NetPnl <= 0);
            result.WinRate = result.TotalTrades > 0 ? (decimal)result.WinningTrades / result.TotalTrades * 100m : 0m;
            result.TotalNetPnl = trades.Sum(t => t.NetPnl);

            decimal grossProfit = trades.Where(t => t.NetPnl > 0).Sum(t => t.NetPnl);
            decimal grossLoss = Math.Abs(trades.Where(t => t.NetPnl < 0).Sum(t => t.NetPnl));
            result.ProfitFactor = grossLoss > 0 ? grossProfit / grossLoss : grossProfit > 0 ? 99.9m : 0m;

            result.MaxDrawdown = maxDrawdown;
            result.MaxDrawdownPercent = request.InitialCapital > 0 ? (maxDrawdown / request.InitialCapital) * 100m : 0m;

            // Simple Sharpe Ratio estimate
            if (trades.Count > 1)
            {
                var returns = trades.Select(t => (double)t.NetPnl).ToList();
                double avgReturn = returns.Average();
                double sumOfSquares = returns.Select(r => (r - avgReturn) * (r - avgReturn)).Sum();
                double stdDev = Math.Sqrt(sumOfSquares / (returns.Count - 1));
                result.SharpeRatio = stdDev > 0 ? (decimal)(avgReturn / stdDev * Math.Sqrt(252)) : 0m;
            }

            return result;
        }
    }
}
