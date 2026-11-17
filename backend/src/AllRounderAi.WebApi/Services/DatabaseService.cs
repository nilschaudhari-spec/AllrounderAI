using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Npgsql;

namespace AllRounderAi.WebApi.Services
{
    public interface IDatabaseService
    {
        Task<DatabaseStatusResult> TestConnectionAsync();
        Task<SchemaInitResult> InitializeSchemaAsync();
        Task<List<string>> GetExistingTablesAsync();
    }

    public class DatabaseStatusResult
    {
        public bool IsConnected { get; set; }
        public string DatabaseName { get; set; } = string.Empty;
        public string ServerVersion { get; set; } = string.Empty;
        public string Message { get; set; } = string.Empty;
        public DateTime Timestamp { get; set; } = DateTime.UtcNow;
    }

    public class SchemaInitResult
    {
        public bool Success { get; set; }
        public int TablesCreated { get; set; }
        public string Message { get; set; } = string.Empty;
        public List<string> Details { get; set; } = new();
    }

    public class DatabaseService : IDatabaseService
    {
        private readonly string _connectionString;
        private readonly ILogger<DatabaseService> _logger;

        public DatabaseService(IConfiguration configuration, ILogger<DatabaseService> logger)
        {
            _connectionString = configuration.GetConnectionString("DefaultConnection") 
                ?? "Server=db80695.databaseasp.net;Port=5432;Database=db80695;User Id=db80695;Password=6x?F!5Rd4e-D;";
            _logger = logger;
        }

        public async Task<DatabaseStatusResult> TestConnectionAsync()
        {
            var result = new DatabaseStatusResult();
            try
            {
                await using var conn = new NpgsqlConnection(_connectionString);
                await conn.OpenAsync();
                result.IsConnected = true;
                result.DatabaseName = conn.Database;
                result.ServerVersion = conn.ServerVersion;
                result.Message = "Connected successfully to PostgreSQL on MonsterASP.NET!";
            }
            catch (Exception ex)
            {
                result.IsConnected = false;
                result.Message = $"Connection failed: {ex.Message}";
                _logger.LogError(ex, "Database connection test failed.");
            }
            return result;
        }

        public async Task<List<string>> GetExistingTablesAsync()
        {
            var tables = new List<string>();
            try
            {
                await using var conn = new NpgsqlConnection(_connectionString);
                await conn.OpenAsync();

                const string sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;";
                await using var cmd = new NpgsqlCommand(sql, conn);
                await using var reader = await cmd.ExecuteReaderAsync();
                while (await reader.ReadAsync())
                {
                    tables.Add(reader.GetString(0));
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to retrieve existing tables.");
            }
            return tables;
        }

        public async Task<SchemaInitResult> InitializeSchemaAsync()
        {
            var result = new SchemaInitResult();
            try
            {
                await using var conn = new NpgsqlConnection(_connectionString);
                await conn.OpenAsync();

                var ddlStatements = new List<string>
                {
                    @"CREATE EXTENSION IF NOT EXISTS ""uuid-ossp"";",

                    @"CREATE TABLE IF NOT EXISTS users (
                        user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        email VARCHAR(255) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        full_name VARCHAR(150) NOT NULL,
                        role VARCHAR(50) NOT NULL DEFAULT 'User',
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );",

                    @"CREATE TABLE IF NOT EXISTS exchanges (
                        exchange_id VARCHAR(20) PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        currency VARCHAR(10) NOT NULL DEFAULT 'INR',
                        is_active BOOLEAN NOT NULL DEFAULT TRUE
                    );",

                    @"CREATE TABLE IF NOT EXISTS instruments (
                        instrument_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        exchange_id VARCHAR(20) NOT NULL REFERENCES exchanges(exchange_id) ON DELETE CASCADE,
                        symbol VARCHAR(50) NOT NULL,
                        trading_symbol VARCHAR(100) NOT NULL UNIQUE,
                        name VARCHAR(200) NOT NULL,
                        segment VARCHAR(30) NOT NULL,
                        instrument_type VARCHAR(30) NOT NULL,
                        lot_size INT NOT NULL DEFAULT 1,
                        tick_size NUMERIC(10, 4) NOT NULL DEFAULT 0.05,
                        is_fno_eligible BOOLEAN NOT NULL DEFAULT FALSE,
                        is_active BOOLEAN NOT NULL DEFAULT TRUE
                    );",

                    @"CREATE TABLE IF NOT EXISTS historical_candles (
                        candle_id BIGSERIAL PRIMARY KEY,
                        instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE CASCADE,
                        timeframe VARCHAR(10) NOT NULL,
                        timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                        open NUMERIC(15, 4) NOT NULL,
                        high NUMERIC(15, 4) NOT NULL,
                        low NUMERIC(15, 4) NOT NULL,
                        close NUMERIC(15, 4) NOT NULL,
                        volume BIGINT NOT NULL DEFAULT 0,
                        open_interest BIGINT DEFAULT 0,
                        UNIQUE (instrument_id, timeframe, timestamp)
                    );",

                    @"CREATE TABLE IF NOT EXISTS strategies (
                        strategy_id VARCHAR(50) PRIMARY KEY,
                        name VARCHAR(150) NOT NULL,
                        category VARCHAR(50) NOT NULL,
                        direction VARCHAR(20) NOT NULL,
                        ppt_slide_reference VARCHAR(50) NOT NULL,
                        description TEXT NOT NULL,
                        default_timeframe VARCHAR(10) NOT NULL DEFAULT '5m',
                        is_active BOOLEAN NOT NULL DEFAULT TRUE
                    );",

                    @"CREATE TABLE IF NOT EXISTS trading_signals (
                        signal_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE CASCADE,
                        strategy_id VARCHAR(50) NOT NULL REFERENCES strategies(strategy_id) ON DELETE RESTRICT,
                        signal_type VARCHAR(20) NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
                        timeframe VARCHAR(10) NOT NULL,
                        signal_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                        entry_price NUMERIC(15, 4) NOT NULL,
                        target_price NUMERIC(15, 4) NOT NULL,
                        stop_loss_price NUMERIC(15, 4) NOT NULL,
                        risk_reward_ratio NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
                        quality_score NUMERIC(5, 2) NOT NULL,
                        market_trend VARCHAR(30) NOT NULL,
                        confirmation_mode VARCHAR(30) NOT NULL,
                        analysis_summary TEXT NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );",

                    @"CREATE TABLE IF NOT EXISTS paper_accounts (
                        account_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        provider VARCHAR(50) NOT NULL DEFAULT 'FRONTPAGE',
                        account_name VARCHAR(100) NOT NULL,
                        balance NUMERIC(15, 2) NOT NULL DEFAULT 1000000.00,
                        available_margin NUMERIC(15, 2) NOT NULL DEFAULT 1000000.00,
                        realized_pnl NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
                        unrealized_pnl NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
                        is_active BOOLEAN NOT NULL DEFAULT TRUE
                    );",

                    @"CREATE TABLE IF NOT EXISTS paper_orders (
                        order_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        account_id UUID NOT NULL REFERENCES paper_accounts(account_id) ON DELETE CASCADE,
                        signal_id UUID REFERENCES trading_signals(signal_id) ON DELETE SET NULL,
                        instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE RESTRICT,
                        order_side VARCHAR(10) NOT NULL,
                        quantity INT NOT NULL,
                        price NUMERIC(15, 4) NOT NULL,
                        target_price NUMERIC(15, 4) NOT NULL,
                        stop_loss_price NUMERIC(15, 4) NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'FILLED',
                        placed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );",

                    @"CREATE TABLE IF NOT EXISTS backtest_results (
                        result_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        strategy_id VARCHAR(50) NOT NULL REFERENCES strategies(strategy_id) ON DELETE RESTRICT,
                        symbol VARCHAR(50) NOT NULL,
                        timeframe VARCHAR(10) NOT NULL,
                        total_trades INT NOT NULL,
                        winning_trades INT NOT NULL,
                        losing_trades INT NOT NULL,
                        win_rate NUMERIC(6, 2) NOT NULL,
                        total_net_pnl NUMERIC(15, 2) NOT NULL,
                        profit_factor NUMERIC(8, 2) NOT NULL,
                        max_drawdown NUMERIC(15, 2) NOT NULL,
                        sharpe_ratio NUMERIC(6, 2),
                        executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
                    );"
                };

                int created = 0;
                foreach (var ddl in ddlStatements)
                {
                    try
                    {
                        await using var cmd = new NpgsqlCommand(ddl, conn);
                        await cmd.ExecuteNonQueryAsync();
                        created++;
                    }
                    catch (Exception ex)
                    {
                        result.Details.Add($"DDL notice: {ex.Message}");
                    }
                }

                // Seed Default Exchanges if empty
                const string seedExchanges = @"
                    INSERT INTO exchanges (exchange_id, name, currency, is_active)
                    VALUES 
                        ('NSE', 'National Stock Exchange', 'INR', TRUE),
                        ('BSE', 'Bombay Stock Exchange', 'INR', TRUE),
                        ('MCX', 'Multi Commodity Exchange', 'INR', TRUE)
                    ON CONFLICT (exchange_id) DO NOTHING;";
                await using (var seedCmd = new NpgsqlCommand(seedExchanges, conn))
                {
                    await seedCmd.ExecuteNonQueryAsync();
                }

                result.Success = true;
                result.TablesCreated = created;
                result.Message = "PostgreSQL database schema initialized successfully on MonsterASP.NET!";
            }
            catch (Exception ex)
            {
                result.Success = false;
                result.Message = $"Schema initialization error: {ex.Message}";
                _logger.LogError(ex, "Schema initialization failed.");
            }
            return result;
        }
    }
}
