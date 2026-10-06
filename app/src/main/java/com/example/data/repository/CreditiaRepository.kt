package com.example.data.repository

import com.example.data.local.CreditCardDao
import com.example.data.local.ExpenseDao
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreditiaRepository(
    private val cardDao: CreditCardDao,
    private val expenseDao: ExpenseDao
) {
    val allCards: Flow<List<CreditCardEntity>> = cardDao.getAllCards()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    fun getExpensesByCard(cardId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesByCard(cardId)

    suspend fun getAllCardsList(): List<CreditCardEntity> = cardDao.getAllCardsList()

    suspend fun insertCard(card: CreditCardEntity): Long = cardDao.insertCard(card)

    suspend fun updateCard(card: CreditCardEntity) = cardDao.updateCard(card)

    suspend fun deleteCard(card: CreditCardEntity) {
        cardDao.deleteCard(card)
        expenseDao.deleteExpensesByCard(card.id)
    }

    suspend fun deleteCardById(cardId: Long) {
        cardDao.deleteCardById(cardId)
        expenseDao.deleteExpensesByCard(cardId)
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)

    suspend fun clearAllData() {
        expenseDao.clearAllExpenses()
        cardDao.clearAllCards()
    }

    suspend fun getNextSequenceNumber(): Int {
        val max = expenseDao.getMaxSequenceNumber() ?: 0
        return max + 1
    }

    suspend fun getExpenseCount(): Int = expenseDao.getExpenseCount()

    /**
     * Calculates the Billing Period name (e.g. "Octubre 2026") based on the transaction date (YYYY/MM/DD)
     * and the credit card's cutoff day (cutDay).
     * If the date's day is <= cutDay, the cycle ends in this month -> Period is current month.
     * If the date's day is > cutDay, the cycle ends in next month -> Period is next month.
     */
    fun calculateBillingPeriod(dateString: String, cutDay: Int): String {
        return try {
            val parts = dateString.split("/")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt() // 1-12
                val day = parts[2].toInt()

                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month - 1)
                cal.set(Calendar.DAY_OF_MONTH, day)

                if (day > cutDay) {
                    // Falls into the following period
                    cal.add(Calendar.MONTH, 1)
                }

                val monthNames = arrayOf(
                    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
                )
                val periodMonth = monthNames[cal.get(Calendar.MONTH)]
                val periodYear = cal.get(Calendar.YEAR)
                "$periodMonth $periodYear"
            } else {
                "Octubre 2026"
            }
        } catch (e: Exception) {
            "Octubre 2026"
        }
    }

    /**
     * Computes the billing cycle date range string, e.g. "2026/09/25 - 2026/10/24" for October 2026 period with cutDay 24.
     */
    fun getBillingCycleRange(periodName: String, cutDay: Int): String {
        return try {
            val parts = periodName.split(" ")
            if (parts.size == 2) {
                val monthName = parts[0]
                val year = parts[1].toInt()
                val monthNames = listOf(
                    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
                )
                val monthIndex = monthNames.indexOf(monthName)
                if (monthIndex != -1) {
                    val endCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, monthIndex)
                        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                        set(Calendar.DAY_OF_MONTH, cutDay.coerceAtMost(maxDay))
                    }

                    val startCal = Calendar.getInstance().apply {
                        timeInMillis = endCal.timeInMillis
                        add(Calendar.MONTH, -1)
                        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                        val startDay = (cutDay + 1).coerceAtMost(maxDay)
                        set(Calendar.DAY_OF_MONTH, startDay)
                    }

                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    "${sdf.format(startCal.time)} - ${sdf.format(endCal.time)}"
                } else {
                    "2026/09/25 - 2026/10/24"
                }
            } else {
                "2026/09/25 - 2026/10/24"
            }
        } catch (e: Exception) {
            "2026/09/25 - 2026/10/24"
        }
    }
}
