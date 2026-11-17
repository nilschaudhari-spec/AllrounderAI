-- ============================================================================
-- ALL ROUNDER AI - PRODUCTION POSTGRESQL DATABASE SCHEMA
-- Version: 1.0.0
-- Market Timezone: Asia/Kolkata (IST, UTC+05:30)
-- Target: PostgreSQL 15+ / 16+
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. USERS, ROLES & AUTHENTICATION
-- ============================================================================

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    role VARCHAR(50) NOT NULL DEFAULT 'User', -- 'User', 'Admin', 'Analyst'
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata'),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    theme VARCHAR(20) NOT NULL DEFAULT 'Dark', -- 'Dark', 'Light', 'System'
    default_exchange VARCHAR(20) NOT NULL DEFAULT 'NSE',
    enable_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    enable_sound BOOLEAN NOT NULL DEFAULT TRUE,
    auto_paper_trade_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_daily_loss_limit NUMERIC(15, 2) NOT NULL DEFAULT 5000.00,
    max_concurrent_trades INT NOT NULL DEFAULT 5,
    default_risk_reward_ratio NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

-- ============================================================================
-- 2. EXCHANGES, INSTRUMENTS & MARKET DATA
-- ============================================================================

CREATE TABLE exchanges (
    exchange_id VARCHAR(20) PRIMARY KEY, -- 'NSE', 'BSE', 'MCX'
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50) NOT NULL DEFAULT 'India',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    market_open_time TIME NOT NULL DEFAULT '09:15:00',
    market_close_time TIME NOT NULL DEFAULT '15:30:00',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE market_holidays (
    holiday_id SERIAL PRIMARY KEY,
    exchange_id VARCHAR(20) NOT NULL REFERENCES exchanges(exchange_id) ON DELETE CASCADE,
    holiday_date DATE NOT NULL,
    description VARCHAR(200) NOT NULL,
    UNIQUE (exchange_id, holiday_date)
);

CREATE TABLE instruments (
    instrument_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exchange_id VARCHAR(20) NOT NULL REFERENCES exchanges(exchange_id) ON DELETE CASCADE,
    symbol VARCHAR(50) NOT NULL,
    trading_symbol VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    segment VARCHAR(30) NOT NULL, -- 'EQUITY', 'FUTURES', 'OPTIONS', 'INDEX', 'COMMODITY'
    instrument_type VARCHAR(30) NOT NULL, -- 'EQ', 'FUT', 'CE', 'PE', 'INDEX'
    underlying_symbol VARCHAR(50),
    lot_size INT NOT NULL DEFAULT 1,
    tick_size NUMERIC(10, 4) NOT NULL DEFAULT 0.05,
    strike_price NUMERIC(15, 2),
    expiry_date DATE,
    option_type VARCHAR(10), -- 'CE', 'PE' or NULL
    is_fno_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE INDEX idx_instruments_symbol ON instruments(symbol);
CREATE INDEX idx_instruments_segment ON instruments(segment);
CREATE INDEX idx_instruments_expiry ON instruments(expiry_date);
CREATE INDEX idx_instruments_fno ON instruments(is_fno_eligible);

CREATE TABLE historical_candles (
    candle_id BIGSERIAL PRIMARY KEY,
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    timeframe VARCHAR(10) NOT NULL, -- '1m', '3m', '5m', '15m', '30m', '1h', 'D', 'W', 'M'
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    open NUMERIC(15, 4) NOT NULL,
    high NUMERIC(15, 4) NOT NULL,
    low NUMERIC(15, 4) NOT NULL,
    close NUMERIC(15, 4) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    open_interest BIGINT DEFAULT 0,
    UNIQUE (instrument_id, timeframe, timestamp)
);

CREATE INDEX idx_candles_lookup ON historical_candles(instrument_id, timeframe, timestamp DESC);

CREATE TABLE live_quotes (
    instrument_id UUID PRIMARY KEY REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    last_price NUMERIC(15, 4) NOT NULL,
    day_open NUMERIC(15, 4) NOT NULL,
    day_high NUMERIC(15, 4) NOT NULL,
    day_low NUMERIC(15, 4) NOT NULL,
    day_close NUMERIC(15, 4) NOT NULL,
    change NUMERIC(15, 4) NOT NULL,
    percentage_change NUMERIC(8, 4) NOT NULL,
    total_volume BIGINT NOT NULL,
    open_interest BIGINT DEFAULT 0,
    vwap NUMERIC(15, 4),
    bid_price NUMERIC(15, 4),
    ask_price NUMERIC(15, 4),
    last_trade_time TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

-- ============================================================================
-- 3. STRATEGIES, PARAMETERS & USER OVERRIDES
-- ============================================================================

CREATE TABLE strategies (
    strategy_id VARCHAR(50) PRIMARY KEY, -- 'HAMMER', 'LOW_BREAK_CLOSE_UPSIDE', 'DAILY_INTRADAY_5M', 'GOLDEN_CROSS', 'PROTECTIVE_PUT', etc.
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'CANDLESTICK', 'PRICE_ACTION', 'INDICATOR', 'OPTIONS'
    direction VARCHAR(20) NOT NULL, -- 'BUY', 'SELL', 'NEUTRAL', 'HEDGED'
    ppt_slide_reference VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    default_timeframe VARCHAR(10) NOT NULL DEFAULT '5m',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE strategy_parameters (
    parameter_id SERIAL PRIMARY KEY,
    strategy_id VARCHAR(50) NOT NULL REFERENCES strategies(strategy_id) ON DELETE CASCADE,
    param_key VARCHAR(100) NOT NULL,
    param_name VARCHAR(150) NOT NULL,
    param_type VARCHAR(30) NOT NULL, -- 'INTEGER', 'FLOAT', 'BOOLEAN', 'STRING'
    default_value VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    UNIQUE (strategy_id, param_key)
);

CREATE TABLE instrument_strategy_overrides (
    override_id SERIAL PRIMARY KEY,
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    strategy_id VARCHAR(50) NOT NULL REFERENCES strategies(strategy_id) ON DELETE CASCADE,
    param_key VARCHAR(100) NOT NULL,
    param_value VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata'),
    UNIQUE (instrument_id, strategy_id, param_key)
);

-- ============================================================================
-- 4. SCANNER MATCHES, SIGNALS & DETAILED ANALYSIS
-- ============================================================================

CREATE TABLE trading_signals (
    signal_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    strategy_id VARCHAR(50) NOT NULL REFERENCES strategies(strategy_id) ON DELETE RESTRICT,
    signal_type VARCHAR(20) NOT NULL, -- 'BUY', 'SELL'
    status VARCHAR(30) NOT NULL DEFAULT 'NEW', -- 'NEW', 'CONFIRMED', 'EARLY_INTRABAR', 'EXECUTED_PAPER', 'TARGET_HIT', 'STOP_LOSS_HIT', 'EXPIRED', 'CANCELLED'
    timeframe VARCHAR(10) NOT NULL,
    signal_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    entry_price NUMERIC(15, 4) NOT NULL,
    target_price NUMERIC(15, 4) NOT NULL,
    stop_loss_price NUMERIC(15, 4) NOT NULL,
    risk_reward_ratio NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    quality_score NUMERIC(5, 2) NOT NULL, -- 0.00 to 100.00
    market_trend VARCHAR(30) NOT NULL, -- 'UPTREND', 'DOWNTREND', 'REVERSAL'
    confirmation_mode VARCHAR(30) NOT NULL, -- 'CANDLE_CLOSE', 'INTRABAR_EARLY'
    analysis_summary TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata'),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE INDEX idx_signals_timestamp ON trading_signals(signal_timestamp DESC);
CREATE INDEX idx_signals_status ON trading_signals(status);
CREATE INDEX idx_signals_instrument ON trading_signals(instrument_id);

CREATE TABLE signal_conditions (
    condition_id BIGSERIAL PRIMARY KEY,
    signal_id UUID NOT NULL REFERENCES trading_signals(signal_id) ON DELETE CASCADE,
    condition_name VARCHAR(150) NOT NULL,
    is_satisfied BOOLEAN NOT NULL,
    expected_value VARCHAR(100) NOT NULL,
    actual_value VARCHAR(100) NOT NULL,
    notes VARCHAR(255)
);

-- ============================================================================
-- 5. PAPER TRADING & FRONTPAGE INTEGRATION
-- ============================================================================

CREATE TABLE paper_accounts (
    account_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL DEFAULT 'FRONTPAGE', -- 'FRONTPAGE', 'INTERNAL_SIM'
    account_name VARCHAR(100) NOT NULL,
    initial_balance NUMERIC(15, 2) NOT NULL DEFAULT 1000000.00, -- Default 10 Lakhs INR
    available_margin NUMERIC(15, 2) NOT NULL DEFAULT 1000000.00,
    realized_pnl NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    unrealized_pnl NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE paper_orders (
    order_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES paper_accounts(account_id) ON DELETE CASCADE,
    signal_id UUID REFERENCES trading_signals(signal_id) ON DELETE SET NULL,
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE RESTRICT,
    order_side VARCHAR(10) NOT NULL, -- 'BUY', 'SELL'
    order_type VARCHAR(20) NOT NULL, -- 'MARKET', 'LIMIT', 'SL', 'SL_M'
    product_type VARCHAR(20) NOT NULL DEFAULT 'INTRADAY', -- 'INTRADAY', 'CARRYFORWARD'
    quantity INT NOT NULL,
    price NUMERIC(15, 4),
    trigger_price NUMERIC(15, 4),
    target_price NUMERIC(15, 4),
    stop_loss_price NUMERIC(15, 4),
    filled_quantity INT NOT NULL DEFAULT 0,
    average_fill_price NUMERIC(15, 4),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'OPEN', 'FILLED', 'REJECTED', 'CANCELLED'
    rejection_reason VARCHAR(255),
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata'),
    filled_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE paper_positions (
    position_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES paper_accounts(account_id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE RESTRICT,
    product_type VARCHAR(20) NOT NULL,
    net_quantity INT NOT NULL,
    average_buy_price NUMERIC(15, 4) NOT NULL DEFAULT 0.00,
    average_sell_price NUMERIC(15, 4) NOT NULL DEFAULT 0.00,
    current_market_price NUMERIC(15, 4) NOT NULL,
    realized_pnl NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    unrealized_pnl NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE paper_trades (
    trade_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES paper_orders(order_id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES paper_accounts(account_id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE RESTRICT,
    side VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    execution_price NUMERIC(15, 4) NOT NULL,
    brokerage NUMERIC(10, 2) NOT NULL DEFAULT 20.00,
    taxes_and_charges NUMERIC(10, 2) NOT NULL DEFAULT 5.00,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

-- ============================================================================
-- 6. HISTORICAL BACKTESTING ENGINE
-- ============================================================================

CREATE TABLE backtest_configurations (
    backtest_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    strategy_id VARCHAR(50) NOT NULL REFERENCES strategies(strategy_id) ON DELETE RESTRICT,
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE RESTRICT,
    timeframe VARCHAR(10) NOT NULL DEFAULT '5m',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    initial_capital NUMERIC(15, 2) NOT NULL DEFAULT 100000.00,
    risk_reward_ratio NUMERIC(5, 2) NOT NULL DEFAULT 1.00,
    slippage_percent NUMERIC(5, 3) NOT NULL DEFAULT 0.050,
    brokerage_per_order NUMERIC(10, 2) NOT NULL DEFAULT 20.00,
    parameters_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE backtest_results (
    result_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backtest_id UUID NOT NULL UNIQUE REFERENCES backtest_configurations(backtest_id) ON DELETE CASCADE,
    total_trades INT NOT NULL,
    winning_trades INT NOT NULL,
    losing_trades INT NOT NULL,
    win_rate NUMERIC(6, 2) NOT NULL,
    total_net_pnl NUMERIC(15, 2) NOT NULL,
    total_gross_profit NUMERIC(15, 2) NOT NULL,
    total_gross_loss NUMERIC(15, 2) NOT NULL,
    profit_factor NUMERIC(8, 2) NOT NULL,
    max_drawdown NUMERIC(15, 2) NOT NULL,
    max_drawdown_percent NUMERIC(6, 2) NOT NULL,
    sharpe_ratio NUMERIC(6, 2),
    sortino_ratio NUMERIC(6, 2),
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE backtest_trades (
    trade_id BIGSERIAL PRIMARY KEY,
    backtest_id UUID NOT NULL REFERENCES backtest_configurations(backtest_id) ON DELETE CASCADE,
    entry_time TIMESTAMP WITH TIME ZONE NOT NULL,
    exit_time TIMESTAMP WITH TIME ZONE NOT NULL,
    side VARCHAR(10) NOT NULL,
    entry_price NUMERIC(15, 4) NOT NULL,
    exit_price NUMERIC(15, 4) NOT NULL,
    quantity INT NOT NULL,
    pnl NUMERIC(15, 2) NOT NULL,
    pnl_percentage NUMERIC(8, 4) NOT NULL,
    exit_reason VARCHAR(50) NOT NULL -- 'TARGET_HIT', 'STOP_LOSS_HIT', 'TIME_EXIT'
);

-- ============================================================================
-- 7. WATCHLISTS, NOTIFICATIONS & AUDIT LOGS
-- ============================================================================

CREATE TABLE watchlists (
    watchlist_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE watchlist_items (
    item_id SERIAL PRIMARY KEY,
    watchlist_id UUID NOT NULL REFERENCES watchlists(watchlist_id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE CASCADE,
    sort_order INT NOT NULL DEFAULT 0,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata'),
    UNIQUE (watchlist_id, instrument_id)
);

CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL, -- 'SIGNAL', 'ORDER_FILLED', 'RISK_ALERT'
    payload_json JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE trading_journal (
    journal_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    trade_id UUID,
    instrument_id UUID NOT NULL REFERENCES instruments(instrument_id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    notes TEXT NOT NULL,
    tags VARCHAR(255),
    rating INT CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);

CREATE TABLE audit_logs (
    log_id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    details_json JSONB,
    ip_address VARCHAR(50),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')
);
