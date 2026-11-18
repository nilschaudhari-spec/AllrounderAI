package com.allrounder.ai.data.network

import com.allrounder.ai.domain.model.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface AllRounderApiService {

    @GET("api/v1/market/instruments")
    suspend fun getInstruments(
        @Query("exchange") exchange: String? = null,
        @Query("segment") segment: String? = null,
        @Query("search") search: String? = null,
        @Query("fnoOnly") fnoOnly: Boolean? = null,
        @Query("limit") limit: Int = 100
    ): Response<List<Instrument>>

    @GET("api/v1/market/quote")
    suspend fun getLiveQuote(
        @Query("symbol") symbol: String,
        @Query("exchange") exchange: String = "NSE"
    ): Response<LiveQuoteDto>

    @GET("api/v1/market/quotes")
    suspend fun getLiveQuotes(
        @Query("exchange") exchange: String? = null,
        @Query("segment") segment: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<List<LiveQuoteDto>>

    @GET("api/v1/strategies")
    suspend fun getStrategies(
        @Query("category") category: String? = null
    ): Response<List<StrategyInfo>>

    @GET("api/v1/strategies/{id}/signals")
    suspend fun getStrategySignals(
        @Path("id") id: String,
        @Query("exchange") exchange: String? = null
    ): Response<List<Signal>>

    @GET("api/v1/market/candles")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("timeframe") timeframe: String = "5m",
        @Query("limit") limit: Int = 100
    ): Response<CandlesResponse>

    @GET("api/v1/signals/live")
    suspend fun getLiveSignals(
        @Query("status") status: String? = null,
        @Query("exchange") exchange: String? = null
    ): Response<List<Signal>>

    @GET("api/v1/options/protective-put")
    suspend fun getProtectivePut(
        @Query("underlying") underlying: String = "BANKNIFTY",
        @Query("futurePrice") futurePrice: Double = 51240.0,
        @Query("atmStrike") atmStrike: Double = 51200.0,
        @Query("putPremium") putPremium: Double = 210.0
    ): Response<ProtectivePayoff>

    @GET("api/v1/options/protective-call")
    suspend fun getProtectiveCall(
        @Query("underlying") underlying: String = "BANKNIFTY",
        @Query("futurePrice") futurePrice: Double = 51240.0,
        @Query("atmStrike") atmStrike: Double = 51200.0,
        @Query("callPremium") callPremium: Double = 240.0
    ): Response<ProtectivePayoff>

    @POST("api/v1/backtest/execute")
    suspend fun executeBacktest(@Body request: BacktestApiRequest): Response<BacktestSummary>

    @GET("api/v1/papertrading/portfolio")
    suspend fun getPaperPortfolio(): Response<PaperPortfolioResponse>

    // ========================================================================
    // HAMMER STRATEGY INSTITUTIONAL API
    // ========================================================================

    @GET("api/strategies/hammer/config")
    suspend fun getHammerConfig(@Query("preset") preset: String? = null): Response<HammerConfigDto>

    @PUT("api/strategies/hammer/config")
    suspend fun updateHammerConfig(@Body config: HammerConfigDto): Response<HammerConfigDto>

    @POST("api/strategies/hammer/config/reset")
    suspend fun resetHammerConfig(): Response<Any>

    @GET("api/strategies/hammer/scanner")
    suspend fun getHammerScanner(
        @Query("scope") scope: String? = "ALL",
        @Query("exchange") exchange: String? = "ALL",
        @Query("timeframe") timeframe: String? = "5m",
        @Query("status") status: String? = "ALL",
        @Query("minScore") minScore: Double = 55.0
    ): Response<HammerScannerResponse>

    @POST("api/strategies/hammer/scanner/run")
    suspend fun runHammerScanner(@Body request: HammerScanApiRequest): Response<Any>

    @GET("api/strategies/hammer/{instrumentId}/analysis")
    suspend fun getHammerAnalysis(@Path("instrumentId") instrumentId: String): Response<HammerDetectionDetailDto>
}

data class CandlesResponse(
    val symbol: String,
    val timeframe: String,
    val candles: List<Candle>
)

data class BacktestApiRequest(
    val strategyId: String,
    val symbol: String,
    val timeframe: String = "5m",
    val initialCapital: Double = 100000.0,
    val riskRewardRatio: Double = 1.0
)

data class PaperPortfolioResponse(
    val account: PaperAccountDto,
    val positions: List<Any>,
    val orders: List<Any>
)

data class PaperAccountDto(
    val accountId: String,
    val providerName: String,
    val balance: Double,
    val availableMargin: Double,
    val realizedPnl: Double,
    val unrealizedPnl: Double
)

object ApiClient {
    private const val BASE_URL = "http://allrounderai.runasp.net/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: AllRounderApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AllRounderApiService::class.java)
    }
}
