using System.Threading.Tasks;
using Microsoft.AspNetCore.SignalR;
using AllRounderAi.Core.Models;

namespace AllRounderAi.WebApi.Hubs
{
    public interface IMarketSignalClient
    {
        Task ReceiveLiveSignal(TradingSignal signal);
        Task SignalStatusChanged(string signalId, string newStatus, decimal currentPrice);
        Task MarketTickUpdate(string symbol, decimal lastPrice, decimal changePercent);
        Task PaperOrderFilled(PaperOrder order);
    }

    public class MarketSignalHub : Hub<IMarketSignalClient>
    {
        public async Task SubscribeToSymbol(string symbol)
        {
            await Groups.AddToGroupAsync(Context.ConnectionId, symbol);
        }

        public async Task UnsubscribeFromSymbol(string symbol)
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, symbol);
        }
    }
}
