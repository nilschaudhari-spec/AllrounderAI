using System.Collections.Generic;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.Strategies
{
    public class StrategyMatchResult
    {
        public bool IsMatched { get; set; }
        public TradingSignal? Signal { get; set; }
    }

    public interface IStrategyEngine
    {
        string StrategyId { get; }
        string StrategyName { get; }
        string PptSlideReference { get; }
        SignalDirection DefaultDirection { get; }

        StrategyMatchResult Evaluate(
            IReadOnlyList<Candle> candles, 
            Instrument instrument, 
            StrategyConfig config, 
            int currentIndex = -1);
    }
}
