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

    suspend fun getAllExpensesList(): List<ExpenseEntity> = expenseDao.getAllExpensesList()

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

    suspend fun updateExpense(expense: ExpenseEntity) {
        if (expense.isCreditNote() && !expense.deferredGroupId.isNullOrBlank()) {
            // Las notas de crédito creadas por un diferido no se pueden modificar directamente
            return
        }
        expenseDao.updateExpense(expense)
        if (!expense.deferredGroupId.isNullOrBlank()) {
            syncLogosForDeferredGroup(expense.deferredGroupId)
        }
    }

    /**
     * Updates an expense. If the expense is part of a deferred consumption group,
     * reorganizes all installments in the group:
     * - Adjusts dates for all installments relative to the edited installment, maintaining monthly intervals.
     * - Recalculates each installment's billingPeriod based on its new date and the card's cutoff day.
     * - Synchronizes establishment, base description (preserving Cuota X/Y), cardId, and deferredGroupId.
     * Returns the number of updated installments.
     */
    suspend fun updateExpenseWithDeferredSync(
        updatedExpense: ExpenseEntity,
        cutDay: Int
    ): Int {
        val originalInDb = expenseDao.getExpenseById(updatedExpense.id)
        val group = if (!updatedExpense.deferredGroupId.isNullOrBlank()) {
            expenseDao.getExpensesByDeferredGroupId(updatedExpense.deferredGroupId)
        } else if (originalInDb != null && !originalInDb.deferredGroupId.isNullOrBlank()) {
            expenseDao.getExpensesByDeferredGroupId(originalInDb.deferredGroupId)
        } else if (originalInDb != null) {
            getDeferredInstallments(originalInDb)
        } else {
            getDeferredInstallments(updatedExpense)
        }

        val effectiveGroupId = updatedExpense.deferredGroupId
            ?: originalInDb?.deferredGroupId
            ?: group.firstNotNullOfOrNull { it.deferredGroupId }

        // Las notas de crédito creadas por un diferido no se pueden modificar directamente
        if (updatedExpense.isCreditNote() && !effectiveGroupId.isNullOrBlank()) {
            return 0
        }

        if (group.size <= 1) {
            expenseDao.updateExpense(updatedExpense)
            return 1
        }

        // Separemos el grupo en notas de crédito, cuotas del diferido y consumos originales preservados
        val creditNotes = group.filter { it.isCreditNote() || it.transactionType == ExpenseEntity.TYPE_CREDIT_NOTE }
        val installments = group.filter { !it.isCreditNote() && !it.isPayment() && (it.getDeferredInfo() != null || it.description.contains(Regex("""\(Cuota\s*\d+/\d+\)"""))) }
        val originalPreserved = group.filter { !it.isCreditNote() && !it.isPayment() && it.getDeferredInfo() == null && !it.description.contains(Regex("""\(Cuota\s*\d+/\d+\)""")) }

        // Si es una Nota de Crédito manual sin grupo diferido, se actualiza directamente
        if (updatedExpense.isCreditNote() || updatedExpense.transactionType == ExpenseEntity.TYPE_CREDIT_NOTE) {
            expenseDao.updateExpense(updatedExpense)
            return 1
        }

        // Caso A: Se está editando una cuota del diferido (o el grupo contiene cuotas)
        val isEditingInstallment = updatedExpense.isDeferredInstallment() || installments.any { it.id == updatedExpense.id }
        if (isEditingInstallment && installments.isNotEmpty()) {
            // Sort ONLY the actual installments by installment number
            val sortedInstallments = installments.sortedWith(
                compareBy(
                    { it.getDeferredInfo()?.first ?: it.sequenceNumber },
                    { it.dateString }
                )
            )

            val sortedWithIndex = sortedInstallments.mapIndexed { index, expense ->
                val info = expense.getDeferredInfo()
                val cuotaNum = info?.first ?: (index + 1)
                val total = info?.second ?: sortedInstallments.size
                Triple(expense, cuotaNum, total)
            }

            val targetTriple = sortedWithIndex.find { it.first.id == updatedExpense.id }
            val editedCuota = targetTriple?.second ?: (updatedExpense.getDeferredInfo()?.first ?: 1)
            val totalInstallments = sortedWithIndex.maxOfOrNull { it.third } ?: sortedInstallments.size

            val dateParts = updatedExpense.dateString.split("/")
            val targetYear = dateParts.getOrNull(0)?.toIntOrNull() ?: 2026
            val targetMonth = (dateParts.getOrNull(1)?.toIntOrNull() ?: 10) - 1 // 0-based
            val targetDay = dateParts.getOrNull(2)?.toIntOrNull() ?: 1

            val cleanBaseDesc = updatedExpense.description
                .replace(Regex("""\s*\(Cuota\s*\d+/\d+\)\s*$"""), "")
                .trim()
            val updatedEstablishment = updatedExpense.establishment.ifBlank { "Consumo General" }

            val toUpdateInstallments = sortedWithIndex.map { (installmentExpense, cuotaNum, _) ->
                val monthOffset = cuotaNum - editedCuota
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, targetYear)
                cal.set(Calendar.MONTH, targetMonth + monthOffset)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDay))

                val installmentDateStr = String.format(
                    Locale.US,
                    "%04d/%02d/%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                val installmentBillingPeriod = calculateBillingPeriod(installmentDateStr, cutDay)
                val newDesc = if (cleanBaseDesc.isNotBlank()) {
                    "$cleanBaseDesc (Cuota $cuotaNum/$totalInstallments)"
                } else {
                    "Consumo (Cuota $cuotaNum/$totalInstallments)"
                }

                if (installmentExpense.id == updatedExpense.id) {
                    updatedExpense.copy(
                        dateString = installmentDateStr,
                        billingPeriod = installmentBillingPeriod,
                        establishment = updatedEstablishment,
                        description = newDesc,
                        cardId = updatedExpense.cardId,
                        deferredGroupId = effectiveGroupId,
                        transactionType = ExpenseEntity.TYPE_EXPENSE,
                        establishmentLogoUrl = updatedExpense.establishmentLogoUrl ?: installmentExpense.establishmentLogoUrl
                    )
                } else {
                    installmentExpense.copy(
                        dateString = installmentDateStr,
                        billingPeriod = installmentBillingPeriod,
                        establishment = updatedEstablishment,
                        description = newDesc,
                        cardId = updatedExpense.cardId,
                        deferredGroupId = effectiveGroupId,
                        transactionType = ExpenseEntity.TYPE_EXPENSE,
                        receiptUri = updatedExpense.receiptUri ?: installmentExpense.receiptUri,
                        establishmentLogoUrl = updatedExpense.establishmentLogoUrl ?: installmentExpense.establishmentLogoUrl
                    )
                }
            }

            toUpdateInstallments.forEach { expenseDao.updateExpense(it) }

            val firstInstallment = toUpdateInstallments.first()
            val newCreditNoteDate = firstInstallment.dateString
            val newCreditNotePeriod = firstInstallment.billingPeriod

            // SINCRONIZAR Y PRESERVAR NOTAS DE CRÉDITO:
            // Al cambiar la fecha de una cuota diferida, también cambia la fecha de la nota de crédito
            // generada por esa cuota, manteniéndose intacta como TYPE_CREDIT_NOTE.
            creditNotes.forEach { cn ->
                val preservedCn = cn.copy(
                    cardId = updatedExpense.cardId,
                    deferredGroupId = effectiveGroupId,
                    dateString = newCreditNoteDate,
                    billingPeriod = newCreditNotePeriod,
                    transactionType = ExpenseEntity.TYPE_CREDIT_NOTE,
                    establishmentLogoUrl = updatedExpense.establishmentLogoUrl ?: cn.establishmentLogoUrl
                )
                expenseDao.updateExpense(preservedCn)
            }

            // Los consumos originales que fueron seleccionados para diferir NO deben cambiar sus fechas ni sus logos.
            // Conservan su dateString, billingPeriod y establishmentLogoUrl originales intactos.
            originalPreserved.forEach { orig ->
                if (orig.id != updatedExpense.id) {
                    val preservedOrig = orig.copy(
                        cardId = updatedExpense.cardId,
                        deferredGroupId = effectiveGroupId,
                        transactionType = ExpenseEntity.TYPE_EXPENSE,
                        establishmentLogoUrl = orig.establishmentLogoUrl
                    )
                    expenseDao.updateExpense(preservedOrig)
                }
            }

            return toUpdateInstallments.size
        } else {
            // Caso B: Se está editando el consumo original preservado
            val updatedOrig = updatedExpense.copy(
                cardId = updatedExpense.cardId,
                deferredGroupId = effectiveGroupId,
                billingPeriod = calculateBillingPeriod(updatedExpense.dateString, cutDay),
                transactionType = ExpenseEntity.TYPE_EXPENSE
            )
            expenseDao.updateExpense(updatedOrig)

            // Recolectar todos los logotipos de los consumos originales diferidos
            val otherOriginals = originalPreserved.filter { it.id != updatedExpense.id }
            val allOrigs = otherOriginals + updatedOrig
            val distinctLogos = allOrigs.flatMap { it.getLogoUrls() }.distinct()
            val combinedLogos = if (distinctLogos.isNotEmpty()) distinctLogos.joinToString("|") else null

            // Sincronizar fecha, periodo y logotipos combinados de la Nota de Crédito
            creditNotes.forEach { cn ->
                val preservedCn = cn.copy(
                    cardId = updatedExpense.cardId,
                    deferredGroupId = effectiveGroupId,
                    dateString = updatedExpense.dateString,
                    billingPeriod = updatedOrig.billingPeriod,
                    establishment = "Nota de Crédito - ${updatedExpense.establishment}",
                    transactionType = ExpenseEntity.TYPE_CREDIT_NOTE,
                    establishmentLogoUrl = combinedLogos ?: cn.establishmentLogoUrl
                )
                expenseDao.updateExpense(preservedCn)
            }

            // Sincronizar logotipos combinados en todas las cuotas del diferido consecuentes
            installments.forEach { inst ->
                val updatedInst = inst.copy(
                    establishmentLogoUrl = combinedLogos ?: inst.establishmentLogoUrl
                )
                expenseDao.updateExpense(updatedInst)
            }

            return 1
        }
    }

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
        if (target.isCreditNote() && !target.deferredGroupId.isNullOrBlank()) {
            // Las notas de crédito creadas por un diferido no se pueden eliminar de forma individual
            return 0
        }
        val groupExpenses = if (!target.deferredGroupId.isNullOrBlank()) {
            expenseDao.getExpensesByDeferredGroupId(target.deferredGroupId)
        } else {
            getDeferredInstallments(target)
        }

        if (groupExpenses.size > 1) {
            val isTargetOriginal = target.isExpense() && !target.description.contains(Regex("""\(Cuota\s*\d+/\d+\)""")) && !target.isCreditNote()

            groupExpenses.filter { it.id == target.id || it.isDeferredExpense() || it.isCreditNote() }
                .mapNotNull { it.receiptUri }.distinct().forEach { uri ->
                    ReceiptStorageUtil.deleteReceiptFile(uri)
                }

            if (isTargetOriginal) {
                if (!target.deferredGroupId.isNullOrBlank()) {
                    expenseDao.deleteExpensesByDeferredGroupId(target.deferredGroupId)
                } else {
                    groupExpenses.forEach { expenseDao.deleteExpense(it) }
                }
                return groupExpenses.size
            } else {
                val toDelete = groupExpenses.filter { it.isCreditNote() || it.description.contains(Regex("""\(Cuota\s*\d+/\d+\)""")) }
                val toRestore = groupExpenses.filter { !it.isCreditNote() && !it.description.contains(Regex("""\(Cuota\s*\d+/\d+\)""")) }

                toDelete.forEach { expenseDao.deleteExpense(it) }
                toRestore.forEach { expenseDao.updateExpense(it.copy(deferredGroupId = null)) }
                return toDelete.size
            }
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

    suspend fun groupExpenses(expenseIds: List<Long>, groupName: String? = null): String {
        val groupId = "group_${System.currentTimeMillis()}"
        val finalName = groupName?.trim()?.ifBlank { null }
        expenseDao.updateTransactionGroup(expenseIds, groupId, finalName)
        return groupId
    }

    suspend fun ungroupExpenses(groupId: String) {
        expenseDao.clearTransactionGroup(groupId)
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

    companion object {
        /**
         * Retorna la fecha de corte para el mes y año dados.
         * Regla 1:
         * El día de corte de la tarjeta está determinado en la configuración (cutDay).
         * Por ejemplo, si la tarjeta tiene configurado el día 24, el corte para octubre de 2026 es el 24 de octubre.
         * Se acota únicamente al último día del mes en caso de meses con menos días (ej. 28 o 29 en febrero).
         */
        fun getCutCalendar(year: Int, monthIndex: Int, cutDay: Int): Calendar {
            return Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthIndex)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, cutDay.coerceIn(1, maxDay))
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        fun getCutDay(year: Int, monthIndex: Int, cutDay: Int): Int {
            return getCutCalendar(year, monthIndex, cutDay).get(Calendar.DAY_OF_MONTH)
        }

        fun getEffectiveCutCalendar(year: Int, monthIndex: Int, nominalCutDay: Int): Calendar {
            return getCutCalendar(year, monthIndex, nominalCutDay)
        }

        fun getEffectiveCutDay(year: Int, monthIndex: Int, nominalCutDay: Int): Int {
            return getCutDay(year, monthIndex, nominalCutDay)
        }

        /**
         * Calcula la fecha máxima de pago de la tarjeta para cada periodo.
         * Regla 2:
         * Se contabiliza el número de días establecido en la configuración de la tarjeta
         * a partir del día de corte, contabilizando ÚNICAMENTE días de lunes a viernes (días hábiles).
         *
         * Ejemplo:
         * Para el periodo de octubre 2026 con fecha de corte 24 de octubre y 12 días configurados:
         * Se cuentan 12 días después del 24 de octubre de lunes a viernes:
         * 1: Lun 26 oct, 2: Mar 27 oct, 3: Mié 28 oct, 4: Jue 29 oct, 5: Vie 30 oct,
         * 6: Lun 2 nov, 7: Mar 3 nov, 8: Mié 4 nov, 9: Jue 5 nov, 10: Vie 6 nov,
         * 11: Lun 9 nov, 12: Mar 10 nov.
         * Fecha máxima de pago: Martes 10 de noviembre de 2026.
         */
        fun calculateMaxPaymentCalendar(
            cutCal: Calendar,
            payBusinessDays: Int
        ): Calendar {
            val cal = cutCal.clone() as Calendar
            var daysCounted = 0
            val targetDays = payBusinessDays.coerceAtLeast(1)
            while (daysCounted < targetDays) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                    daysCounted++
                }
            }
            return cal
        }

        fun parsePeriodYearMonth(periodName: String): Pair<Int, Int> {
            val monthNames = listOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )
            val cal = Calendar.getInstance()
            var targetYear = cal.get(Calendar.YEAR)
            var targetMonth = cal.get(Calendar.MONTH)
            val parts = periodName.trim().split(" ")
            if (parts.size == 2) {
                val mIdx = monthNames.indexOfFirst { it.equals(parts[0], ignoreCase = true) }
                val yr = parts[1].toIntOrNull()
                if (mIdx != -1 && yr != null) {
                    targetYear = yr
                    targetMonth = mIdx
                }
            }
            return Pair(targetYear, targetMonth)
        }
    }

    /**
     * Calcula el Periodo de Facturación (ej. "Octubre 2026") según la fecha de la transacción (YYYY/MM/DD)
     * y el día de corte configurado de la tarjeta (cutDay).
     * Regla 1:
     * Si el día de la transacción es <= cutDay, pertenece al periodo del mes actual.
     * Si el día de la transacción es > cutDay, pertenece al periodo del mes siguiente.
     * Ejemplo: con corte día 24, el periodo de Octubre 2026 abarca desde el 25 de septiembre hasta el 24 de octubre.
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

                // El día de corte de la tarjeta está determinado en la configuración
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val periodCutDay = cutDay.coerceIn(1, maxDay)

                if (day > periodCutDay) {
                    // Cae después del corte -> pertenece al siguiente periodo
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
     * Calcula el rango de fechas de facturación para un periodo determinado.
     * Regla 1:
     * El rango va desde el día siguiente al corte del mes anterior hasta el día de corte de este periodo.
     * Ejemplo: Para Octubre 2026 con corte 24, el rango es desde 25 de septiembre hasta 24 de octubre (2026/09/25 - 2026/10/24).
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
                    val endCal = getCutCalendar(year, monthIndex, cutDay)

                    val prevMonthCal = Calendar.getInstance().apply {
                        timeInMillis = endCal.timeInMillis
                        add(Calendar.MONTH, -1)
                    }
                    val prevCut = getCutCalendar(prevMonthCal.get(Calendar.YEAR), prevMonthCal.get(Calendar.MONTH), cutDay)
                    val startCal = Calendar.getInstance().apply {
                        timeInMillis = prevCut.timeInMillis
                        add(Calendar.DAY_OF_MONTH, 1)
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
     * Conserva el registro del consumo original y crea un registro de Nota de Crédito
     * para que al restarse se anulen, dejando vigentes las N cuotas calculadas.
     */
    suspend fun deferExpense(
        originalExpense: ExpenseEntity,
        months: Int,
        monthlyInterestRate: Double,
        cutDay: Int,
        startDateString: String? = null
    ): List<ExpenseEntity> {
        if (originalExpense.isDeferredInstallment() || originalExpense.isDeferredExpense()) {
            throw IllegalArgumentException("No se pueden diferir registros de tipo cuota diferida")
        }
        if (originalExpense.isPayment()) {
            throw IllegalArgumentException("No se pueden diferir registros de tipo pago a tarjeta")
        }
        val effectiveDate = startDateString?.ifBlank { null } ?: originalExpense.dateString
        val plan = calculateInstallmentPlan(
            amount = originalExpense.amount,
            months = months,
            monthlyInterestRate = monthlyInterestRate,
            startDateString = effectiveDate,
            cutDay = cutDay
        )

        val baseDescription = originalExpense.description.replace(Regex("\\s*\\(Cuota\\s*\\d+/\\d+\\)"), "").trim()
        val baseEstablishment = originalExpense.establishment
        val groupId = "def_${System.currentTimeMillis()}_${originalExpense.id}"
        val allLogos = originalExpense.getLogoUrls()
        val combinedLogos = if (allLogos.isNotEmpty()) allLogos.joinToString("|") else null

        // 1. Conservar el consumo original vinculándolo al grupo diferido
        val preservedOriginal = originalExpense.copy(deferredGroupId = groupId)
        expenseDao.updateExpense(preservedOriginal)

        // 2. Crear registro total de Nota de Crédito para anular el consumo diferido
        var currentSeq = getNextSequenceNumber()
        val latestBillingPeriod = calculateBillingPeriod(effectiveDate, cutDay)
        val notaCredito = ExpenseEntity(
            sequenceNumber = currentSeq++,
            cardId = originalExpense.cardId,
            establishment = "Nota de Crédito - $baseEstablishment",
            description = "Anulación por diferido a $months cuotas",
            amount = originalExpense.amount,
            dateString = effectiveDate,
            billingPeriod = latestBillingPeriod,
            receiptUri = originalExpense.receiptUri,
            deferredGroupId = groupId,
            establishmentLogoUrl = combinedLogos,
            transactionType = ExpenseEntity.TYPE_CREDIT_NOTE,
            timestamp = System.currentTimeMillis()
        )
        expenseDao.insertExpense(notaCredito)

        // 3. Generar cuotas 1..N
        val newInstallments = mutableListOf<ExpenseEntity>()
        for (k in 1..months) {
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
                establishmentLogoUrl = combinedLogos,
                transactionType = ExpenseEntity.TYPE_EXPENSE,
                timestamp = System.currentTimeMillis() + k
            )
            newInstallments.add(newExpense)
        }

        if (newInstallments.isNotEmpty()) {
            expenseDao.insertExpenses(newInstallments)
        }

        return listOf(preservedOriginal, notaCredito) + newInstallments
    }

    /**
     * Consolidates multiple selected expenses into a single deferred installment plan.
     * Conserva los registros de los consumos individuales seleccionados y crea un registro total
     * de Nota de Crédito por la sumatoria para que al restarse se anulen, generando las N cuotas unificadas.
     * Almacena todos los logotipos de los consumos seleccionados en las cuotas y en la nota de crédito.
     */
    suspend fun deferMultipleExpenses(
        selectedExpenses: List<ExpenseEntity>,
        months: Int,
        monthlyInterestRate: Double,
        cutDay: Int,
        startDateString: String? = null
    ): List<ExpenseEntity> {
        if (selectedExpenses.isEmpty()) return emptyList()
        if (selectedExpenses.any { it.isDeferredInstallment() || it.isDeferredExpense() }) {
            throw IllegalArgumentException("No se pueden diferir registros de tipo cuota diferida")
        }
        if (selectedExpenses.any { it.isPayment() }) {
            throw IllegalArgumentException("No se pueden diferir registros de tipo pago a tarjeta")
        }

        val totalAmount = selectedExpenses.sumOf { it.amount }
        val effectiveDate = startDateString?.ifBlank { null }
            ?: selectedExpenses.maxByOrNull { it.dateString }?.dateString
            ?: "2026/10/01"
        val combinedEstablishment = selectedExpenses.map { it.establishment }
            .distinct()
            .joinToString(" + ")
            .take(45)

        val plan = calculateInstallmentPlan(
            amount = totalAmount,
            months = months,
            monthlyInterestRate = monthlyInterestRate,
            startDateString = effectiveDate,
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

        // Todos los logotipos de los consumos seleccionados para diferir
        val allLogos = selectedExpenses
            .flatMap { it.getLogoUrls() }
            .distinct()
        val combinedLogos = if (allLogos.isNotEmpty()) allLogos.joinToString("|") else null

        // 1. Conservar los registros individuales originales vinculándolos al deferredGroupId
        // Cada consumo original conserva su propio logo intacto
        selectedExpenses.forEach { exp ->
            expenseDao.updateExpense(
                exp.copy(
                    deferredGroupId = groupId
                )
            )
        }

        // 2. Crear un registro total de Nota de Crédito por el monto total diferido
        // Debe contener todos los logotipos de los consumos seleccionados para diferir
        val latestBillingPeriod = calculateBillingPeriod(effectiveDate, cutDay)
        val notaCredito = ExpenseEntity(
            sequenceNumber = currentSeq++,
            cardId = cardId,
            establishment = "Nota de Crédito - $combinedEstablishment",
            description = "Anulación por diferido unificado (${selectedExpenses.size} consumos a $months cuotas)",
            amount = totalAmount,
            dateString = effectiveDate,
            billingPeriod = latestBillingPeriod,
            deferredGroupId = groupId,
            receiptUri = sharedReceipt,
            establishmentLogoUrl = combinedLogos,
            transactionType = ExpenseEntity.TYPE_CREDIT_NOTE,
            timestamp = System.currentTimeMillis()
        )
        expenseDao.insertExpense(notaCredito)

        // 3. Generar cuotas 1..N
        // Cada cuota nueva debe contener todos los logotipos de los consumos seleccionados para diferir
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
                establishmentLogoUrl = combinedLogos,
                transactionType = ExpenseEntity.TYPE_EXPENSE,
                timestamp = System.currentTimeMillis() + k
            )
        }

        expenseDao.insertExpenses(newInstallments)

        return listOf(notaCredito) + newInstallments
    }

    /**
     * Registra directamente una compra diferida en N cuotas mensuales desde "+ Registrar"
     * SIN generar nota de crédito ni registro previo, creando únicamente las cuotas activas en amarillo.
     */
    suspend fun createDirectDeferred(
        cardId: Long,
        totalAmount: Double,
        establishment: String,
        description: String,
        startDateString: String,
        months: Int,
        monthlyInterestRate: Double,
        receiptUri: String?,
        cutDay: Int,
        establishmentLogoUrl: String? = null
    ): List<ExpenseEntity> {
        val plan = calculateInstallmentPlan(
            amount = totalAmount,
            months = months,
            monthlyInterestRate = monthlyInterestRate,
            startDateString = startDateString,
            cutDay = cutDay
        )

        val cleanDesc = description.ifBlank { "Consumo diferido" }
        val baseEst = establishment.ifBlank { "Consumo General" }
        val groupId = "def_direct_${System.currentTimeMillis()}"
        var currentSeq = getNextSequenceNumber()

        val newInstallments = plan.mapIndexed { index, itemPlan ->
            val k = index + 1
            ExpenseEntity(
                sequenceNumber = currentSeq++,
                cardId = cardId,
                establishment = baseEst,
                description = "$cleanDesc (Cuota $k/$months)",
                amount = itemPlan.amount,
                dateString = itemPlan.dateString,
                billingPeriod = itemPlan.billingPeriod,
                deferredGroupId = groupId,
                receiptUri = receiptUri,
                establishmentLogoUrl = establishmentLogoUrl,
                transactionType = ExpenseEntity.TYPE_EXPENSE,
                timestamp = System.currentTimeMillis() + k
            )
        }

        expenseDao.insertExpenses(newInstallments)
        return newInstallments
    }

    /**
     * Sincroniza y propaga los logotipos agregados a los consumos originales
     * hacia todas las cuotas diferidas y hacia la nota de crédito del grupo.
     * Recolecta todos los logotipos únicos de los consumos originales diferidos
     * y los concatena con '|' para que se reflejen fielmente en las cuotas y en la nota de crédito.
     */
    suspend fun syncLogosForDeferredGroup(deferredGroupId: String): Int {
        if (deferredGroupId.isBlank()) return 0
        val group = expenseDao.getExpensesByDeferredGroupId(deferredGroupId)
        if (group.isEmpty()) return 0

        // Consumos originales que conformaron este diferido (no son cuotas ni notas de crédito)
        val originals = group.filter { !it.isCreditNote() && !it.isPayment() && !it.isDeferredInstallment() }
        val creditNotes = group.filter { it.isCreditNote() }
        val installments = group.filter { it.isDeferredInstallment() }

        // Si existen consumos originales, tomamos todos los logos distintos de todos los consumos originales
        val combinedLogos = if (originals.isNotEmpty()) {
            val distinctLogos = originals.flatMap { it.getLogoUrls() }.distinct()
            if (distinctLogos.isNotEmpty()) distinctLogos.joinToString("|") else null
        } else {
            // Si es un diferido directo sin consumo original previo, mantenemos los logos existentes de las cuotas
            val distinctLogos = installments.flatMap { it.getLogoUrls() }.distinct()
            if (distinctLogos.isNotEmpty()) distinctLogos.joinToString("|") else null
        }

        var updatedCount = 0
        // Actualizar cuotas
        installments.forEach { inst ->
            if (inst.establishmentLogoUrl != combinedLogos) {
                expenseDao.updateExpense(inst.copy(establishmentLogoUrl = combinedLogos))
                updatedCount++
            }
        }

        // Actualizar notas de crédito
        creditNotes.forEach { cn ->
            if (cn.establishmentLogoUrl != combinedLogos) {
                expenseDao.updateExpense(cn.copy(establishmentLogoUrl = combinedLogos))
                updatedCount++
            }
        }

        return updatedCount
    }

    suspend fun updateLogoForDeferredGroup(deferredGroupId: String, logoUrl: String?): Int {
        val group = expenseDao.getExpensesByDeferredGroupId(deferredGroupId)
        var count = 0
        group.forEach { exp ->
            if (exp.isCreditNote() || exp.isDeferredInstallment()) {
                expenseDao.updateExpense(exp.copy(establishmentLogoUrl = logoUrl))
                count++
            }
        }
        return count
    }

    suspend fun updateLogoForEstablishment(establishment: String, logoUrl: String?): Int {
        return expenseDao.updateLogoByEstablishment(establishment, logoUrl)
    }

    suspend fun updateTransactionGroup(expenseIds: List<Long>, groupId: String?, groupName: String?) {
        expenseDao.updateTransactionGroup(expenseIds, groupId, groupName)
    }

    suspend fun clearTransactionGroup(groupId: String) {
        expenseDao.clearTransactionGroup(groupId)
    }
}
