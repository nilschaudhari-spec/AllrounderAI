using System;
using System.Collections.Generic;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies
{
    public class OptionsStrategyEngine
    {
        // ========================================================================
        // 1. PROTECTIVE PUT (Slide 68 - Uptrend Hedged Strategy)
        // ========================================================================
        // When market is in uptrend:
        // Buy Banknifty Future - 1 lot
        // Buy Banknifty ATM Put - 1 lot
        public static ProtectiveHedgeStrategy BuildProtectivePut(
            string underlyingSymbol, 
            decimal futureEntryPrice, 
            decimal atmStrikePrice, 
            decimal putPremium,
            int lotSize = 15)
        {
            var strategy = new ProtectiveHedgeStrategy
            {
                StrategyName = "Protective Put (Hedged Uptrend Strategy)",
                PptSlideReference = "Slide 68",
                UnderlyingSymbol = underlyingSymbol,
                FuturePrice = futureEntryPrice,
                OptionStrikePrice = atmStrikePrice,
                OptionPremium = putPremium,
                OptionType = "PE",
                // Max Loss = (Future Entry - Strike Price) + Put Premium
                MaxLoss = ((futureEntryPrice - atmStrikePrice) + putPremium) * lotSize,
                // Max Profit = Unlimited (Future gains indefinitely above Strike + Premium)
                MaxProfit = decimal.MaxValue,
                // Breakeven = Future Entry Price + Put Premium
                BreakevenPrice = futureEntryPrice + putPremium
            };

            // Generate Payoff Curve across a -10% to +10% price range
            decimal step = futureEntryPrice * 0.01m;
            for (decimal price = futureEntryPrice * 0.90m; price <= futureEntryPrice * 1.10m; price += step)
            {
                decimal futurePnl = (price - futureEntryPrice) * lotSize;
                decimal optionIntrinsic = Math.Max(0m, atmStrikePrice - price);
                decimal optionPnl = (optionIntrinsic - putPremium) * lotSize;
                decimal netPnl = futurePnl + optionPnl;

                strategy.PayoffCurve.Add(new PayoffPoint
                {
                    UnderlyingPrice = price,
                    FuturePnl = futurePnl,
                    OptionPnl = optionPnl,
                    NetPnl = netPnl
                });
            }

            return strategy;
        }

        // ========================================================================
        // 2. PROTECTIVE CALL (Slide 69 - Downtrend Hedged Strategy)
        // ========================================================================
        // When market is in downtrend:
        // Short sell Banknifty Future - 1 lot
        // Buy Banknifty ATM Call - 1 lot
        public static ProtectiveHedgeStrategy BuildProtectiveCall(
            string underlyingSymbol, 
            decimal futureShortPrice, 
            decimal atmStrikePrice, 
            decimal callPremium,
            int lotSize = 15)
        {
            var strategy = new ProtectiveHedgeStrategy
            {
                StrategyName = "Protective Call (Hedged Downtrend Strategy)",
                PptSlideReference = "Slide 69",
                UnderlyingSymbol = underlyingSymbol,
                FuturePrice = futureShortPrice,
                OptionStrikePrice = atmStrikePrice,
                OptionPremium = callPremium,
                OptionType = "CE",
                // Max Loss = (Strike Price - Future Short Price) + Call Premium
                MaxLoss = ((atmStrikePrice - futureShortPrice) + callPremium) * lotSize,
                // Max Profit = Future Short Price - Call Premium (if price falls to 0)
                MaxProfit = (futureShortPrice - callPremium) * lotSize,
                // Breakeven = Future Short Price - Call Premium
                BreakevenPrice = futureShortPrice - callPremium
            };

            decimal step = futureShortPrice * 0.01m;
            for (decimal price = futureShortPrice * 0.90m; price <= futureShortPrice * 1.10m; price += step)
            {
                decimal futurePnl = (futureShortPrice - price) * lotSize;
                decimal optionIntrinsic = Math.Max(0m, price - atmStrikePrice);
                decimal optionPnl = (optionIntrinsic - callPremium) * lotSize;
                decimal netPnl = futurePnl + optionPnl;

                strategy.PayoffCurve.Add(new PayoffPoint
                {
                    UnderlyingPrice = price,
                    FuturePnl = futurePnl,
                    OptionPnl = optionPnl,
                    NetPnl = netPnl
                });
            }

            return strategy;
        }
    }
}
