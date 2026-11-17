package com.allrounder.ai.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "instruments",
    indices = [
        Index(value = ["symbol"]),
        Index(value = ["tradingSymbol"], unique = true),
        Index(value = ["segment"])
    ]
)
data class InstrumentEntity(
    @PrimaryKey
    val instrumentId: String,
    val exchangeId: String, // "NSE", "BSE", "MCX"
    val symbol: String,
    val tradingSymbol: String,
    val name: String,
    val segment: String, // "EQUITY", "FUTURES", "OPTIONS", "INDEX"
    val instrumentType: String, // "EQ", "FUT", "CE", "PE", "INDEX"
    val underlyingSymbol: String?,
    val lotSize: Int = 1,
    val tickSize: Double = 0.05,
    val strikePrice: Double? = null,
    val expiryDate: String? = null,
    val optionType: String? = null,
    val isFnoEligible: Boolean = false,
    val isActive: Boolean = true,
    val lastPrice: Double = 0.0,
    val changePercent: Double = 0.0,
    val volume: Long = 0L
)
