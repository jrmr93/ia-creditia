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
    val receiptUri: String? = null,
    val deferredGroupId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isDeferredExpense(): Boolean {
        if (!deferredGroupId.isNullOrBlank()) return true
        return Regex("""\s*\(Cuota\s*\d+/\d+\)\s*$""").containsMatchIn(description)
    }

    fun getDeferredInfo(): Pair<Int, Int>? {
        val match = Regex("""\s*\(Cuota\s*(\d+)/(\d+)\)\s*$""").find(description) ?: return null
        val current = match.groupValues[1].toIntOrNull() ?: return null
        val total = match.groupValues[2].toIntOrNull() ?: return null
        return Pair(current, total)
    }

    fun getReceiptUriList(): List<String> {
        if (receiptUri.isNullOrBlank()) return emptyList()
        return receiptUri.split("|").filter { it.isNotBlank() }
    }
}

