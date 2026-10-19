package com.example.data.repository

import com.example.data.local.CreditCardDao
import com.example.data.local.ExpenseDao
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import com.example.util.ReceiptStorageUtil
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

    suspend fun getExpenseById(id: Long): ExpenseEntity? = expenseDao.getExpenseById(id)
    suspend fun getExpensesListByCard(cardId: Long): List<ExpenseEntity> = expenseDao.getExpensesListByCard(cardId)
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)

    suspend fun getDeferredInstallments(expense: ExpenseEntity): List<ExpenseEntity> {
        if (!expense.deferredGroupId.isNullOrBlank()) {
            return expenseDao.getExpensesByDeferredGroupId(expense.deferredGroupId)
        }
        val match = Regex("""^(.*?)\s*\(Cuota\s*(\d+)/(\d+)\)\s*$""").find(expense.description)
        if (match != null) {
            val baseDesc = match.groupValues[1].trim()
            val totalInstallments = match.groupValues[3]
            val pattern = if (baseDesc.isNotBlank()) "$baseDesc (Cuota %/$totalInstallments)" else "%(Cuota %/$totalInstallments)"
            return expenseDao.findSimilarDeferredExpenses(expense.cardId, expense.establishment, pattern)
        }
        return emptyList()
    }

    /**
     * Deletes an expense. If the expense is part of a deferred consumption,
     * deletes ALL installment records belonging to that deferred group across
     * all billing periods, as well as the receipt image file.
     * Returns the number of expenses deleted.
     */
    suspend fun deleteExpenseWithCascade(target: ExpenseEntity): Int {
        val groupExpenses = getDeferredInstallments(target)
        if (groupExpenses.size > 1) {
            groupExpenses.mapNotNull { it.receiptUri }.distinct().forEach { uri ->
                ReceiptStorageUtil.deleteReceiptFile(uri)
            }
            if (!target.deferredGroupId.isNullOrBlank()) {
                expenseDao.deleteExpensesByDeferredGroupId(target.deferredGroupId)
            } else {
                groupExpenses.forEach { expenseDao.deleteExpense(it) }
            }
            return groupExpenses.size
        } else {
            ReceiptStorageUtil.deleteReceiptFile(target.receiptUri)
            expenseDao.deleteExpense(target)
            return 1
        }
    }

    suspend fun deleteExpenseByIdCascade(id: Long): Int {
        val target = expenseDao.getExpenseById(id) ?: return 0
        return deleteExpenseWithCascade(target)
    }

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

    data class InstallmentPreview(
        val installmentNumber: Int,
        val totalInstallments: Int,
        val amount: Double,
        val dateString: String,
        val billingPeriod: String
    )

    /**
     * Calculates the installment schedule and amounts for deferring an expense.
     * Takes into account the monthly interest rate (TEM) using standard French amortization,
     * the starting transaction date, and the card's cutoff day.
     */
    fun calculateInstallmentPlan(
        amount: Double,
        months: Int,
        monthlyInterestRate: Double,
        startDateString: String,
        cutDay: Int
    ): List<InstallmentPreview> {
        val i = monthlyInterestRate / 100.0
        val installmentAmount = if (i > 0) {
            val factor = Math.pow(1.0 + i, months.toDouble())
            amount * (i * factor) / (factor - 1.0)
        } else {
            amount / months
        }

        val previews = mutableListOf<InstallmentPreview>()
        val cal = Calendar.getInstance()
        val parts = startDateString.split("/")
        if (parts.size == 3) {
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }

        for (k in 1..months) {
            if (k > 1) {
                cal.add(Calendar.MONTH, 1)
            }
            val dateStr = String.format(Locale.US, "%04d/%02d/%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            val period = calculateBillingPeriod(dateStr, cutDay)
            previews.add(
                InstallmentPreview(
                    installmentNumber = k,
                    totalInstallments = months,
                    amount = installmentAmount,
                    dateString = dateStr,
                    billingPeriod = period
                )
            )
        }
        return previews
    }

    /**
     * Defers an existing expense into multiple monthly installments.
     * Updates the original expense to become Installment 1, and generates
     * subsequent installments (2..N) with consecutive sequence numbers and
     * corresponding billing periods.
     */
    suspend fun deferExpense(
        originalExpense: ExpenseEntity,
        months: Int,
        monthlyInterestRate: Double,
        cutDay: Int
    ): List<ExpenseEntity> {
        val plan = calculateInstallmentPlan(
            amount = originalExpense.amount,
            months = months,
            monthlyInterestRate = monthlyInterestRate,
            startDateString = originalExpense.dateString,
            cutDay = cutDay
        )

        // Clean base description (strip any previous "(Cuota X/Y)")
        val baseDescription = originalExpense.description.replace(Regex("\\s*\\(Cuota\\s*\\d+/\\d+\\)"), "").trim()
        val baseEstablishment = originalExpense.establishment
        val groupId = "def_${System.currentTimeMillis()}_${originalExpense.id}"

        // 1. Update original expense as Cuota 1
        val firstPlan = plan[0]
        val updatedExpense1 = originalExpense.copy(
            amount = firstPlan.amount,
            description = if (baseDescription.isNotBlank()) "$baseDescription (Cuota 1/$months)" else "Consumo (Cuota 1/$months)",
            billingPeriod = firstPlan.billingPeriod,
            dateString = firstPlan.dateString,
            deferredGroupId = groupId
        )
        expenseDao.updateExpense(updatedExpense1)

        val resultList = mutableListOf(updatedExpense1)

        // 2. Insert installments 2..N
        var currentSeq = getNextSequenceNumber()
        val newExpenses = mutableListOf<ExpenseEntity>()

        for (k in 2..months) {
            val itemPlan = plan[k - 1]
            val newExpense = ExpenseEntity(
                sequenceNumber = currentSeq++,
                cardId = originalExpense.cardId,
                establishment = baseEstablishment,
                description = if (baseDescription.isNotBlank()) "$baseDescription (Cuota $k/$months)" else "Consumo (Cuota $k/$months)",
                amount = itemPlan.amount,
                dateString = itemPlan.dateString,
                billingPeriod = itemPlan.billingPeriod,
                receiptUri = originalExpense.receiptUri,
                deferredGroupId = groupId,
                timestamp = System.currentTimeMillis() + k
            )
            newExpenses.add(newExpense)
            resultList.add(newExpense)
        }

        if (newExpenses.isNotEmpty()) {
            expenseDao.insertExpenses(newExpenses)
        }

        return resultList
    }

    /**
     * Consolidates multiple selected expenses into a single deferred installment plan.
     * Calculates the sum of all selected expenses, deletes the individual original expenses,
     * and generates a unified schedule of N installments with a shared deferredGroupId.
     */
    suspend fun deferMultipleExpenses(
        selectedExpenses: List<ExpenseEntity>,
        months: Int,
        monthlyInterestRate: Double,
        cutDay: Int
    ): List<ExpenseEntity> {
        if (selectedExpenses.isEmpty()) return emptyList()

        val totalAmount = selectedExpenses.sumOf { it.amount }
        val latestDate = selectedExpenses.maxByOrNull { it.dateString }?.dateString ?: "2026/10/01"
        val combinedEstablishment = selectedExpenses.map { it.establishment }
            .distinct()
            .joinToString(" + ")
            .take(45)

        val plan = calculateInstallmentPlan(
            amount = totalAmount,
            months = months,
            monthlyInterestRate = monthlyInterestRate,
            startDateString = latestDate,
            cutDay = cutDay
        )

        val groupId = "def_multi_${System.currentTimeMillis()}"
        var currentSeq = getNextSequenceNumber()
        val cardId = selectedExpenses.first().cardId
        val allReceiptUris = selectedExpenses
            .mapNotNull { it.receiptUri }
            .flatMap { it.split("|") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        val sharedReceipt = if (allReceiptUris.isNotEmpty()) allReceiptUris.joinToString("|") else null

        val newInstallments = plan.mapIndexed { index, itemPlan ->
            val k = index + 1
            ExpenseEntity(
                sequenceNumber = currentSeq++,
                cardId = cardId,
                establishment = combinedEstablishment,
                description = "Diferido Unificado (Cuota $k/$months)",
                amount = itemPlan.amount,
                dateString = itemPlan.dateString,
                billingPeriod = itemPlan.billingPeriod,
                deferredGroupId = groupId,
                receiptUri = sharedReceipt,
                timestamp = System.currentTimeMillis() + k
            )
        }

        // Delete the individual original records
        selectedExpenses.forEach { expenseDao.deleteExpense(it) }

        // Insert the unified installment schedule
        expenseDao.insertExpenses(newInstallments)

        return newInstallments
    }
}
