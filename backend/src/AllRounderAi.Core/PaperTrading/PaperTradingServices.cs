using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using AllRounderAi.Core.Models;
using AllRounderAi.Core.Services;

namespace AllRounderAi.Core.PaperTrading
{
    public interface IPaperTradingProvider
    {
        string ProviderName { get; }
        Task<PaperAccount> GetAccountAsync();
        Task<IReadOnlyList<PaperPosition>> GetPositionsAsync();
        Task<IReadOnlyList<PaperOrder>> GetOrdersAsync();
        Task<PaperOrder> PlaceOrderAsync(TradingSignal signal, int quantity);
        Task<PaperOrder> PlaceCustomOrderAsync(string symbol, string exchange, SignalDirection side, int quantity, decimal price, decimal target, decimal stopLoss, string? signalId = null);
        Task<bool> ClosePositionAsync(string symbol);
        Task<bool> CancelOrderAsync(string orderId);
        Task ResetAccountAsync();
    }

    public class FrontPagePaperTradingBroker : IPaperTradingProvider
    {
        public string ProviderName => "FrontPage Paper Trading";

        private readonly LiveQuoteService _quoteService = new();
        private readonly object _lock = new();

        private PaperAccount _account = new()
        {
            AccountId = "FP_USER_88291",
            ProviderName = "FrontPage",
            Balance = 1000000m,
            AvailableMargin = 894250m,
            RealizedPnl = 42150m,
            UnrealizedPnl = 0m
        };

        private readonly List<PaperOrder> _orders = new();
        private readonly List<PaperPosition> _positions = new();

        public FrontPagePaperTradingBroker()
        {
            SeedInitialState();
        }

        private void SeedInitialState()
        {
            _positions.Clear();
            _orders.Clear();

            _positions.Add(new PaperPosition
            {
                TradingSymbol = "BANKNIFTY",
                Exchange = "NSE",
                Side = SignalDirection.Buy,
                Quantity = 30,
                BuyPrice = 51200.00m,
                CurrentPrice = 51240.00m,
                TargetPrice = 51350.00m,
                StopLossPrice = 51050.00m
            });

            _positions.Add(new PaperPosition
            {
                TradingSymbol = "RELIANCE",
                Exchange = "NSE",
                Side = SignalDirection.Buy,
                Quantity = 50,
                BuyPrice = 1358.00m,
                CurrentPrice = 1363.00m,
                TargetPrice = 1380.00m,
                StopLossPrice = 1345.00m
            });

            _orders.Add(new PaperOrder
            {
                OrderId = "ORD-FP-9941",
                SignalId = "sig-reliance-hammer-01",
                TradingSymbol = "RELIANCE",
                Exchange = "NSE",
                Side = SignalDirection.Buy,
                Quantity = 50,
                Price = 1358.00m,
                TargetPrice = 1380.00m,
                StopLossPrice = 1345.00m,
                Status = "FILLED",
                PlacedAt = DateTime.UtcNow.AddMinutes(-45)
            });

            _orders.Add(new PaperOrder
            {
                OrderId = "ORD-FP-9940",
                SignalId = "sig-banknifty-lb-02",
                TradingSymbol = "BANKNIFTY",
                Exchange = "NSE",
                Side = SignalDirection.Buy,
                Quantity = 30,
                Price = 51200.00m,
                TargetPrice = 51350.00m,
                StopLossPrice = 51050.00m,
                Status = "FILLED",
                PlacedAt = DateTime.UtcNow.AddMinutes(-90)
            });
        }

        public async Task<PaperAccount> GetAccountAsync()
        {
            await RefreshPositionPricesAsync();

            lock (_lock)
            {
                decimal unrealized = _positions.Sum(p => p.UnrealizedPnl);
                _account.UnrealizedPnl = Math.Round(unrealized, 2);
                return _account;
            }
        }

        public async Task<IReadOnlyList<PaperPosition>> GetPositionsAsync()
        {
            await RefreshPositionPricesAsync();

            lock (_lock)
            {
                return _positions.ToList().AsReadOnly();
            }
        }

        public Task<IReadOnlyList<PaperOrder>> GetOrdersAsync()
        {
            lock (_lock)
            {
                return Task.FromResult<IReadOnlyList<PaperOrder>>(_orders.ToList().AsReadOnly());
            }
        }

        private async Task RefreshPositionPricesAsync()
        {
            List<PaperPosition> snapshot;
            lock (_lock)
            {
                snapshot = _positions.ToList();
            }

            foreach (var pos in snapshot)
            {
                try
                {
                    var quote = await _quoteService.GetQuoteAsync(pos.TradingSymbol, pos.Exchange);
                    if (quote.LastPrice > 0)
                    {
                        pos.CurrentPrice = quote.LastPrice;
                    }
                }
                catch
                {
                    // Keep existing price if quote fails
                }
            }
        }

        public async Task<PaperOrder> PlaceOrderAsync(TradingSignal signal, int quantity)
        {
            var quote = await _quoteService.GetQuoteAsync(signal.TradingSymbol, signal.Exchange);
            decimal fillPrice = quote.LastPrice > 0 ? quote.LastPrice : signal.EntryPrice;

            return await PlaceCustomOrderAsync(
                signal.TradingSymbol,
                signal.Exchange,
                signal.SignalType,
                quantity,
                fillPrice,
                signal.TargetPrice,
                signal.StopLossPrice,
                signal.SignalId
            );
        }

        public async Task<PaperOrder> PlaceCustomOrderAsync(
            string symbol,
            string exchange,
            SignalDirection side,
            int quantity,
            decimal price,
            decimal target,
            decimal stopLoss,
            string? signalId = null)
        {
            var cleanSymbol = symbol.Trim().ToUpperInvariant()
                .Replace("NSE:", "").Replace("BSE:", "").Replace(".NS", "").Replace(".BO", "");

            if (price <= 0)
            {
                var quote = await _quoteService.GetQuoteAsync(cleanSymbol, exchange);
                price = quote.LastPrice > 0 ? quote.LastPrice : 1000m;
            }

            if (target <= 0) target = side == SignalDirection.Buy ? price * 1.015m : price * 0.985m;
            if (stopLoss <= 0) stopLoss = side == SignalDirection.Buy ? price * 0.99m : price * 1.01m;

            var order = new PaperOrder
            {
                OrderId = $"ORD-FP-{Guid.NewGuid().ToString()[..8].ToUpperInvariant()}",
                SignalId = signalId ?? $"MANUAL-{cleanSymbol}",
                TradingSymbol = cleanSymbol,
                Exchange = exchange,
                Side = side,
                Quantity = quantity,
                Price = Math.Round(price, 2),
                TargetPrice = Math.Round(target, 2),
                StopLossPrice = Math.Round(stopLoss, 2),
                Status = "FILLED",
                PlacedAt = DateTime.UtcNow
            };

            lock (_lock)
            {
                _orders.Insert(0, order);

                // Margin check and adjustment
                decimal requiredMargin = price * quantity;
                if (_account.AvailableMargin >= requiredMargin)
                {
                    _account.AvailableMargin -= requiredMargin;
                }

                // Update or create active position
                var existingPos = _positions.FirstOrDefault(p =>
                    p.TradingSymbol.Equals(cleanSymbol, StringComparison.OrdinalIgnoreCase) &&
                    p.Side == side);

                if (existingPos == null)
                {
                    _positions.Insert(0, new PaperPosition
                    {
                        TradingSymbol = cleanSymbol,
                        Exchange = exchange,
                        Side = side,
                        Quantity = quantity,
                        BuyPrice = price,
                        CurrentPrice = price,
                        TargetPrice = target,
                        StopLossPrice = stopLoss
                    });
                }
                else
                {
                    // Weighted average price
                    decimal totalVal = (existingPos.BuyPrice * existingPos.Quantity) + (price * quantity);
                    existingPos.Quantity += quantity;
                    existingPos.BuyPrice = Math.Round(totalVal / existingPos.Quantity, 2);
                    existingPos.CurrentPrice = price;
                }
            }

            return order;
        }

        public async Task<bool> ClosePositionAsync(string symbol)
        {
            var cleanSymbol = symbol.Trim().ToUpperInvariant()
                .Replace("NSE:", "").Replace("BSE:", "").Replace(".NS", "").Replace(".BO", "");

            PaperPosition? posToClose;
            lock (_lock)
            {
                posToClose = _positions.FirstOrDefault(p =>
                    p.TradingSymbol.Equals(cleanSymbol, StringComparison.OrdinalIgnoreCase));
            }

            if (posToClose == null) return false;

            var quote = await _quoteService.GetQuoteAsync(posToClose.TradingSymbol, posToClose.Exchange);
            decimal exitPrice = quote.LastPrice > 0 ? quote.LastPrice : posToClose.CurrentPrice;

            lock (_lock)
            {
                decimal realized = posToClose.Side == SignalDirection.Buy
                    ? (exitPrice - posToClose.BuyPrice) * posToClose.Quantity
                    : (posToClose.BuyPrice - exitPrice) * posToClose.Quantity;

                _account.RealizedPnl += Math.Round(realized, 2);
                _account.Balance += Math.Round(realized, 2);
                _account.AvailableMargin += (posToClose.BuyPrice * posToClose.Quantity) + Math.Round(realized, 2);

                // Record exit order
                _orders.Insert(0, new PaperOrder
                {
                    OrderId = $"ORD-FP-SQ-{Guid.NewGuid().ToString()[..8].ToUpperInvariant()}",
                    SignalId = $"SQUAREOFF-{posToClose.TradingSymbol}",
                    TradingSymbol = posToClose.TradingSymbol,
                    Exchange = posToClose.Exchange,
                    Side = posToClose.Side == SignalDirection.Buy ? SignalDirection.Sell : SignalDirection.Buy,
                    Quantity = posToClose.Quantity,
                    Price = exitPrice,
                    TargetPrice = posToClose.TargetPrice,
                    StopLossPrice = posToClose.StopLossPrice,
                    Status = "FILLED",
                    PlacedAt = DateTime.UtcNow
                });

                _positions.Remove(posToClose);
                _account.UnrealizedPnl = _positions.Sum(p => p.UnrealizedPnl);
                return true;
            }
        }

        public Task<bool> CancelOrderAsync(string orderId)
        {
            lock (_lock)
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

        public Task ResetAccountAsync()
        {
            lock (_lock)
            {
                _account = new PaperAccount
                {
                    AccountId = "FP_USER_88291",
                    ProviderName = "FrontPage",
                    Balance = 1000000m,
                    AvailableMargin = 894250m,
                    RealizedPnl = 42150m,
                    UnrealizedPnl = 0m
                };
                SeedInitialState();
            }
            return Task.CompletedTask;
        }
    }
}
