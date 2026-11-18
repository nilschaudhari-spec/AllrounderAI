using System.Collections.Generic;
using System.Linq;

namespace AllRounderAi.Core.Services
{
    public class StrategyDefinition
    {
        public string StrategyId { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Category { get; set; } = string.Empty; // INTRADAY_5M, CANDLESTICK, BREAKOUT, INDICATOR, OPTIONS_HEDGE
        public string Direction { get; set; } = "BULLISH"; // BULLISH, BEARISH, HEDGED
        public string PptSlideReference { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string DefaultTimeframe { get; set; } = "5m";
        public decimal RiskRewardRatio { get; set; } = 1.0m;
        public decimal WinRateBenchmark { get; set; } = 90.0m;
        public List<string> Rules { get; set; } = new();
        public int ActiveSignalsCount { get; set; }
    }

    public static class StrategyRegistry
    {
        private static readonly List<StrategyDefinition> Strategies = new()
        {
            new StrategyDefinition
            {
                StrategyId = "DAILY_INTRADAY_5M",
                Name = "Daily Intraday 5-Minute Technique",
                Category = "INTRADAY_5M",
                Direction = "BULLISH",
                PptSlideReference = "Slides 59-62",
                Description = "High-probability intraday trend setup combining Supertrend(20,2), VWAP support, and MACD bullish confluence on 5-minute charts.",
                DefaultTimeframe = "5m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 94.5m,
                Rules = new List<string>
                {
                    "5-Minute timeframe chart execution",
                    "Supertrend(20, 2) must be GREEN / Bullish",
                    "Price must be trading ABOVE Volume Weighted Average Price (VWAP)",
                    "MACD line (12, 26, 9) crossing or trading ABOVE the Signal line",
                    "Strict 1:1 Target and Stop Loss based on signal candle range",
                    "Mandatory Confirmation on candle close (no premature entry)"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "LOW_BREAK_CLOSE_UPSIDE",
                Name = "Low Break, Close Upside",
                Category = "BREAKOUT",
                Direction = "BULLISH",
                PptSlideReference = "Slides 13-14",
                Description = "Powerful reversal setup: Previous red candle low is broken to trap weak sellers, followed by an aggressive green candle closing upside.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 92.0m,
                Rules = new List<string>
                {
                    "Prior candle must be RED (downtrend / bearish exhaustion)",
                    "Current candle breaks the prior candle's low (liquidity sweep)",
                    "Current green candle closes firmly ABOVE the prior candle's body/open",
                    "7 to 10 points candle body rule satisfied for Bank Nifty / equivalent equities %",
                    "Target = 1:1 distance from entry to stop loss (signal candle low)"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "HIGH_BREAK_CLOSE_DOWNSIDE",
                Name = "High Break, Close Downside",
                Category = "BREAKOUT",
                Direction = "BEARISH",
                PptSlideReference = "Slides 27-28",
                Description = "Institutional trap reversal: Previous green candle high is broken to trap buyers, followed by a strong red candle closing downside.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 91.5m,
                Rules = new List<string>
                {
                    "Prior candle must be GREEN (uptrend / bullish exhaustion)",
                    "Current candle pierces ABOVE prior candle's high (false breakout)",
                    "Current red candle closes firmly BELOW prior candle's body/open",
                    "7 to 10 points candle body rule satisfied",
                    "Stop loss placed at high of signal candle, Target = 1:1 RR"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "HAMMER",
                Name = "Hammer Bottom Reversal",
                Category = "CANDLESTICK",
                Direction = "BULLISH",
                PptSlideReference = "Slides 5-8",
                Description = "Classic bottom reversal pattern appearing after a distinct downtrend. Features a long lower shadow at least 2x the body size.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 89.0m,
                Rules = new List<string>
                {
                    "Must form after a clear 4+ candle downtrend or at key support",
                    "Lower shadow length must be >= 2x real candle body",
                    "Upper shadow is very small or non-existent (<= 15% of range)",
                    "Next candle must confirm with close above hammer high",
                    "Target: 1:1 risk-to-reward ratio based on hammer range"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "INVERSE_HAMMER",
                Name = "Inverted Hammer Reversal",
                Category = "CANDLESTICK",
                Direction = "BULLISH",
                PptSlideReference = "Slides 9-12",
                Description = "Bullish reversal pattern appearing at bottom of downtrend with long upper wick showing buyers testing higher price levels.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 88.5m,
                Rules = new List<string>
                {
                    "Forms after prior bearish swing / downtrend",
                    "Upper wick >= 2x real body size",
                    "Small real body near the bottom of candle range",
                    "Bullish confirmation candle closing above body",
                    "Target 1:1 RR with stop loss placed at candle low"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "BULLISH_HARAMI",
                Name = "Bullish Harami (Inside Day)",
                Category = "CANDLESTICK",
                Direction = "BULLISH",
                PptSlideReference = "Slides 15-18",
                Description = "Two-candle reversal where a small green candle is completely contained inside the body of the previous large red candle.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 87.5m,
                Rules = new List<string>
                {
                    "Candle 1 is a long red bearish candle in a downtrend",
                    "Candle 2 is a small green candle completely inside Candle 1 body",
                    "Indicates cessation of selling pressure and imminent bounce",
                    "Entry on break above Candle 2 high with 1:1 target"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "BEARISH_HARAMI",
                Name = "Bearish Harami (Top Reversal)",
                Category = "CANDLESTICK",
                Direction = "BEARISH",
                PptSlideReference = "Slides 29-32",
                Description = "Bearish inside day reversal at the crest of an uptrend showing exhaustion of buyer momentum.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 87.0m,
                Rules = new List<string>
                {
                    "Candle 1 is a large green bullish candle in an uptrend",
                    "Candle 2 is a small red candle completely contained within Candle 1",
                    "Confirms loss of upside momentum",
                    "Entry on break below Candle 2 low with 1:1 target"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "HANGING_MAN",
                Name = "Hanging Man Top Reversal",
                Category = "CANDLESTICK",
                Direction = "BEARISH",
                PptSlideReference = "Slides 19-22",
                Description = "Bearish reversal warning candle appearing at the top of an uptrend with long lower shadow revealing heavy intra-candle selling.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 86.5m,
                Rules = new List<string>
                {
                    "Occurs after sustained uptrend / at key resistance",
                    "Lower shadow >= 2x real body",
                    "Next candle must confirm by closing below hanging man body",
                    "Stop loss at high, Target 1:1 RR"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "SHOOTING_STAR",
                Name = "Shooting Star Reversal",
                Category = "CANDLESTICK",
                Direction = "BEARISH",
                PptSlideReference = "Slides 23-26",
                Description = "Premier bearish pin-bar reversal with long upper shadow showing severe rejection of higher prices by institutional sellers.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 90.5m,
                Rules = new List<string>
                {
                    "Forms after strong uptrend near resistance zone",
                    "Upper shadow >= 2x body length with minimal lower shadow",
                    "Bearish candle close confirming selling takeover",
                    "Stop loss at candle high, Target = 1:1 RR"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "MARUBOZU",
                Name = "Marubozu Momentum Breakout",
                Category = "CANDLESTICK",
                Direction = "BULLISH",
                PptSlideReference = "Slides 33-36",
                Description = "High-conviction trend candle with virtually zero wicks (< 5% of body) signaling complete institutional order flow dominance.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 93.0m,
                Rules = new List<string>
                {
                    "Real body covers >= 90% of entire candle range",
                    "Above-average volume (>= 1.5x 20-period volume SMA)",
                    "Bullish Marubozu (Open = Low, Close = High) triggers strong long continuation",
                    "Target: 1:1 risk-to-reward ratio based on candle body size"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "MORNING_STAR",
                Name = "Morning Star 3-Candle Reversal",
                Category = "CANDLESTICK",
                Direction = "BULLISH",
                PptSlideReference = "Slides 37-40",
                Description = "Triple candlestick bottom reversal pattern: Long red candle, indecision star candle, followed by strong green candle closing > 50% into first candle.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 92.5m,
                Rules = new List<string>
                {
                    "Candle 1: Long bearish red candle in downtrend",
                    "Candle 2: Indecision star / spinning top / doji",
                    "Candle 3: Strong green candle closing well above midpoint of Candle 1",
                    "Target: 1:1 RR with stop loss placed at low of Star candle"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "EVENING_STAR",
                Name = "Evening Star 3-Candle Reversal",
                Category = "CANDLESTICK",
                Direction = "BEARISH",
                PptSlideReference = "Slides 41-44",
                Description = "Triple candlestick top reversal pattern: Long green candle, high star candle, followed by decisive red candle penetrating > 50% into first candle.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 91.0m,
                Rules = new List<string>
                {
                    "Candle 1: Strong bullish green candle in rally",
                    "Candle 2: Small body star candle at the peak",
                    "Candle 3: Bearish red candle closing below 50% of Candle 1 body",
                    "Stop loss at high of Star, Target 1:1 RR"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "BOLLINGER_BANDS",
                Name = "Bollinger Bands Mean Reversion",
                Category = "INDICATOR",
                Direction = "BULLISH",
                PptSlideReference = "Slides 45-50",
                Description = "Mean reversion technique using 20-period SMA and 2.0 Standard Deviation bands. Catches extreme price stretches returning to the middle band.",
                DefaultTimeframe = "15m",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 89.5m,
                Rules = new List<string>
                {
                    "20-Period Bollinger Bands with 2.0 StdDev",
                    "Price pierces or closes outside lower band (oversold condition)",
                    "Reversal candle closes back inside the band",
                    "Target: Middle band (20 SMA) or 1:1 risk-reward ratio"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "GOLDEN_CROSS",
                Name = "Golden Cross & 200 EMA Re-entry",
                Category = "INDICATOR",
                Direction = "BULLISH",
                PptSlideReference = "Slides 51-58",
                Description = "Trend-following powerhouse: 50 EMA crosses above 200 EMA, and the subsequent pullback/retest of the 200 EMA creates a high-confidence long entry.",
                DefaultTimeframe = "Daily",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 94.0m,
                Rules = new List<string>
                {
                    "50 EMA crosses ABOVE 200 EMA (Golden Cross confirmation)",
                    "Wait for natural price pullback towards the rising 200 EMA",
                    "Bullish price action bounce on the 200 EMA level",
                    "Target: 1:1 RR on swing basis, Stop Loss below 200 EMA support"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "OPTIONS_PROTECTIVE_PUT",
                Name = "Protective Put Hedging (Slide 68)",
                Category = "OPTIONS_HEDGE",
                Direction = "HEDGED",
                PptSlideReference = "Slide 68",
                Description = "Hedging strategy for long positions: Buy an ATM/OTM Put option against long futures or stock holdings to eliminate downside crash risk while maintaining upside potential.",
                DefaultTimeframe = "Daily",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 96.0m,
                Rules = new List<string>
                {
                    "Long Futures or Equity position",
                    "Buy At-The-Money (ATM) Put Option as hedge",
                    "Max Loss is capped strictly to Option Premium paid",
                    "Upside profit potential remains unlimited (Synthetic Long Call)",
                    "Breakeven = Entry Price + Put Premium"
                }
            },
            new StrategyDefinition
            {
                StrategyId = "OPTIONS_PROTECTIVE_CALL",
                Name = "Protective Call Hedging (Slide 69)",
                Category = "OPTIONS_HEDGE",
                Direction = "HEDGED",
                PptSlideReference = "Slide 69",
                Description = "Hedging strategy for short positions: Buy an ATM Call option against short futures to eliminate unlimited upside short-squeeze risk.",
                DefaultTimeframe = "Daily",
                RiskRewardRatio = 1.0m,
                WinRateBenchmark = 95.5m,
                Rules = new List<string>
                {
                    "Short Futures position",
                    "Buy At-The-Money (ATM) Call Option as insurance",
                    "Max Loss is strictly capped to Call Premium paid",
                    "Downside profit remains unlimited (Synthetic Long Put)",
                    "Breakeven = Short Entry Price - Call Premium"
                }
            }
        };

        public static IReadOnlyList<StrategyDefinition> GetAllStrategies() => Strategies.AsReadOnly();

        public static StrategyDefinition? GetStrategyById(string strategyId) =>
            Strategies.FirstOrDefault(s => s.StrategyId.Equals(strategyId, System.StringComparison.OrdinalIgnoreCase));
    }
}
