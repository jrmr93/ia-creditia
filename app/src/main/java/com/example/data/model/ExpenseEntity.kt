package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sequenceNumber: Int, // #1, #2, #3, #4, #5, #6... strict continuous order
    val cardId: Long,
    val establishment: String,
    val description: String,
    val amount: Double,
    val dateString: String, // "YYYY/MM/DD"
    val billingPeriod: String, // "Octubre 2026", "Noviembre 2026", etc.
    val timestamp: Long = System.currentTimeMillis()
)
