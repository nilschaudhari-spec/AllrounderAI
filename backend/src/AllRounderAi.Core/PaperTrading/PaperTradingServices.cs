using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using AllRounderAi.Core.Models;

namespace AllRounderAi.Core.PaperTrading
{
    public interface IPaperTradingProvider
    {
        string ProviderName { get; }
        Task<PaperAccount> GetAccountAsync();
        Task<IReadOnlyList<PaperPosition>> GetPositionsAsync();
        Task<IReadOnlyList<PaperOrder>> GetOrdersAsync();
        Task<PaperOrder> PlaceOrderAsync(TradingSignal signal, int quantity);
        Task<bool> CancelOrderAsync(string orderId);
    }

    public class FrontPagePaperTradingBroker : IPaperTradingProvider
    {
        public string ProviderName => "FrontPage Paper Trading";

        private readonly PaperAccount _account = new()
        {
            AccountId = "FP_USER_88291",
            ProviderName = "FrontPage",
            Balance = 1000000m,
            AvailableMargin = 1000000m
        };

        private readonly List<PaperOrder> _orders = new();
        private readonly List<PaperPosition> _positions = new();

        public Task<PaperAccount> GetAccountAsync()
        {
            return Task.FromResult(_account);
        }

        public Task<IReadOnlyList<PaperPosition>> GetPositionsAsync()
        {
            return Task.FromResult<IReadOnlyList<PaperPosition>>(_positions.AsReadOnly());
        }

        public Task<IReadOnlyList<PaperOrder>> GetOrdersAsync()
        {
            return Task.FromResult<IReadOnlyList<PaperOrder>>(_orders.AsReadOnly());
        }

        public Task<PaperOrder> PlaceOrderAsync(TradingSignal signal, int quantity)
        {
            var order = new PaperOrder
            {
                SignalId = signal.SignalId,
                TradingSymbol = signal.TradingSymbol,
                Side = signal.SignalType,
                Quantity = quantity,
                Price = signal.EntryPrice,
                TargetPrice = signal.TargetPrice,
                StopLossPrice = signal.StopLossPrice,
                Status = "FILLED",
                PlacedAt = DateTime.UtcNow
            };

            _orders.Insert(0, order);

            // Update or create simulated position
            var existingPos = _positions.FirstOrDefault(p => p.TradingSymbol == signal.TradingSymbol);
            if (existingPos == null)
            {
                _positions.Add(new PaperPosition
                {
                    TradingSymbol = signal.TradingSymbol,
                    Quantity = quantity,
                    BuyPrice = signal.EntryPrice,
                    CurrentPrice = signal.EntryPrice
                });
            }
            else
            {
                existingPos.Quantity += quantity;
            }

            return Task.FromResult(order);
        }

        public Task<bool> CancelOrderAsync(string orderId)
        {
            var ord = _orders.FirstOrDefault(o => o.OrderId == orderId);
            if (ord != null && ord.Status == "PENDING")
            {
                ord.Status = "CANCELLED";
                return Task.FromResult(true);
            }
            return Task.FromResult(false);
        }
    }
}
