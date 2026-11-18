using System.Threading.Tasks;
using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using AllRounderAi.WebApi.Hubs;
using AllRounderAi.WebApi.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter());
    });

builder.Services.AddScoped<IDatabaseService, DatabaseService>();
builder.Services.AddSignalR();

builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.AllowAnyHeader()
              .AllowAnyMethod()
              .SetIsOriginAllowed(_ => true)
              .AllowCredentials();
    });
});

var app = builder.Build();

app.UseCors("AllowAll");
app.UseRouting();
app.UseAuthorization();

app.MapControllers();
app.MapHub<MarketSignalHub>("/hubs/market-signals");

app.MapGet("/", async (IDatabaseService db) =>
{
    var dbStatus = await db.TestConnectionAsync();
    return Results.Ok(new
    {
        Application = "ALL ROUNDER AI – Stock Market Strategy Scanner & Automated Paper Trading Platform",
        Version = "1.0.0",
        Status = "Online",
        Database = new
        {
            Host = "db80695.databaseasp.net",
            Database = "db80695",
            Connected = dbStatus.IsConnected,
            ServerVersion = dbStatus.ServerVersion,
            Message = dbStatus.Message
        },
        Endpoints = new[]
        {
            "/api/v1/strategies",
            "/api/v1/strategies/DAILY_INTRADAY_5M/signals",
            "/api/v1/market/instruments",
            "/api/v1/market/quote?symbol=RELIANCE&exchange=NSE",
            "/api/v1/market/quote?symbol=TCS&exchange=BSE",
            "/api/v1/market/quotes",
            "/api/v1/signals/live",
            "/api/v1/scanner/run",
            "/api/v1/options/chain?underlying=BANKNIFTY",
            "/api/v1/options/protective-put",
            "/api/v1/options/protective-call",
            "/api/v1/database/status",
            "/api/v1/database/tables",
            "/hubs/market-signals"
        }
    });
});

// Trigger automatic background schema initialization
_ = Task.Run(async () =>
{
    try
    {
        using var scope = app.Services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<IDatabaseService>();
        await db.InitializeSchemaAsync();
    }
    catch
    {
        // Logged inside DatabaseService
    }
});

app.Run();
