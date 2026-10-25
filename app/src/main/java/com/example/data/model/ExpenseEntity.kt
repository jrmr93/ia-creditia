package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sequenceNumber: Int = 1, // #1, #2, #3, #4, #5, #6... strict continuous order
    val cardId: Long,
    val establishment: String,
    val description: String,
    val amount: Double,
    val dateString: String, // "YYYY/MM/DD"
    val billingPeriod: String, // "Octubre 2026", "Noviembre 2026", etc.
    val receiptUri: String? = null,
    val deferredGroupId: String? = null,
    val transactionType: String = TYPE_EXPENSE, // "EXPENSE", "PAYMENT", "CREDIT_NOTE"
    val transactionGroupId: String? = null, // for grouping multiple consumptions into one transaction
    val transactionGroupName: String? = null, // optional label for grouped transaction
    val establishmentLogoUrl: String? = null, // URL or local file path of the commercial establishment logo
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_EXPENSE = "EXPENSE"
        const val TYPE_PAYMENT = "PAYMENT"
        const val TYPE_CREDIT_NOTE = "CREDIT_NOTE"
    }

    fun isExpense(): Boolean = transactionType == TYPE_EXPENSE || transactionType.isBlank()
    fun isPayment(): Boolean = transactionType == TYPE_PAYMENT
    fun isCreditNote(): Boolean = transactionType == TYPE_CREDIT_NOTE

    fun isDeferredExpense(): Boolean {
        if (isCreditNote() || isPayment()) return false
        if (isOriginalDeferredExpense()) return false
        return isDeferredInstallment()
    }

    fun isDeferredInstallment(): Boolean {
        if (isCreditNote() || isPayment()) return false
        return getDeferredInfo() != null || Regex("""\s*\(Cuota\s*\d+/\d+\)\s*$""").containsMatchIn(description)
    }

    /**
     * Retorna verdadero si este registro corresponde al consumo original que se utilizó para diferir
     * (conservado tras la creación de cuotas y nota de crédito).
     */
    fun isOriginalDeferredExpense(): Boolean {
        if (isCreditNote() || isPayment()) return false
        if (isDeferredInstallment()) return false
        return !deferredGroupId.isNullOrBlank()
    }

    /**
     * Verifica si existen cuotas diferidas activas generadas a partir de este diferido.
     */
    fun hasActiveDeferredInstallments(allExpenses: List<ExpenseEntity>): Boolean {
        if (deferredGroupId.isNullOrBlank()) return false
        return allExpenses.any { it.deferredGroupId == this.deferredGroupId && it.isDeferredInstallment() }
    }

    /**
     * Retorna verdadero si este registro está bloqueado contra eliminación y edición
     * debido a que tiene cuotas diferidas activas asociadas.
     * Aplica tanto al consumo original como a la nota de crédito generada por el diferido.
     */
    fun isLockedByActiveDeferred(allExpenses: List<ExpenseEntity>): Boolean {
        if (deferredGroupId.isNullOrBlank()) return false
        if (isCreditNote()) return true
        return isOriginalDeferredExpense() && hasActiveDeferredInstallments(allExpenses)
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

    /**
     * Retorna la lista de URLs o rutas locales de logotipos asociados a este registro.
     * En diferidos unificados, múltiples logotipos se almacenan concatenados con '|'.
     */
    fun getLogoUrls(): List<String> {
        if (establishmentLogoUrl.isNullOrBlank()) return emptyList()
        return establishmentLogoUrl.split("|").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
}

