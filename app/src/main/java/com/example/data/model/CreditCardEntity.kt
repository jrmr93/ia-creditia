package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val cardName: String,
    val network: String, // "VISA", "MASTERCARD", "AMEX"
    val tier: String = "", // "SIGNATURE", "PLATINUM", "GOLD", etc.
    val last4: String,
    val cutDay: Int, // 1..31
    val payDay: Int, // 1..31
    val cardColorTheme: String = "blue", // "blue", "black"
    val creditLimit: Double = 5000.0,
    val isDefault: Boolean = false,
    val payBusinessDays: Int = 15 // Business days (excluding Sat/Sun) after cutDay to compute max payment day
)
