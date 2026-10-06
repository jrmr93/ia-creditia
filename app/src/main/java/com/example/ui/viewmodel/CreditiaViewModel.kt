package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import com.example.receiver.PaymentReminderReceiver
import com.example.util.NotificationScheduler
import com.example.util.PaymentAlertSchedule
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import com.example.data.repository.CreditiaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import org.json.JSONObject
import org.json.JSONArray

enum class SortOrder(val displayName: String) {
    DATE_DESC("Fecha (más reciente)"),
    DATE_ASC("Fecha (más antigua)"),
    AMOUNT_DESC("Monto (mayor a menor)"),
    AMOUNT_ASC("Monto (menor a mayor)")
}

data class CreditiaUiState(
    val cards: List<CreditCardEntity> = emptyList(),
    val selectedCard: CreditCardEntity? = null,
    val selectedPeriod: String = "",
    val selectedExactDate: String? = null,
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val currentTab: String = "consumos",
    val isBiometricsActive: Boolean = true,
    val isVaultUnlocked: Boolean = false,
    val isFirstLaunch: Boolean = true,
    val masterPin: String = "",
    val failedAttempts: Int = 0,
    val isPinSetupMode: Boolean = false,
    val toastMessage: String? = null,
    val isPaymentNotificationEnabled: Boolean = false,
    val notificationLeadDays: Int = 3,
    // Modals
    val expenseToDelete: ExpenseEntity? = null,
    val cardToDelete: CreditCardEntity? = null,
    val editingExpense: ExpenseEntity? = null,
    val cycleConfigCard: CreditCardEntity? = null,
    val editingCard: CreditCardEntity? = null,
    val isAddCardOpen: Boolean = false,
    val isChangePinOpen: Boolean = false,
    val isClearDbOpen: Boolean = false,
    val availablePeriods: List<String> = emptyList()
)

class CreditiaViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("creditia_secure_prefs", android.content.Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    val repository = CreditiaRepository(database.creditCardDao(), database.expenseDao())

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "creditia_payment_reminders"
        const val PAYMENT_NOTIFICATION_ID = 2001
    }

    private val _uiState = MutableStateFlow(
        run {
            val savedPin = prefs.getString("master_pin", "") ?: ""
            val isFirst = prefs.getBoolean("is_first_launch", true) || savedPin.isEmpty()
            val biometricsEnabled = prefs.getBoolean("biometrics_enabled", true)
            val notifEnabled = prefs.getBoolean("payment_notifications_enabled", false)
            val leadDays = prefs.getInt("notification_lead_days", 3)
            CreditiaUiState(
                isBiometricsActive = biometricsEnabled,
                masterPin = savedPin,
                isFirstLaunch = isFirst,
                isVaultUnlocked = false, // Always require authentication on launch
                isPaymentNotificationEnabled = notifEnabled,
                notificationLeadDays = leadDays
            )
        }
    )
    val uiState: StateFlow<CreditiaUiState> = _uiState.asStateFlow()

    val allCards: StateFlow<List<CreditCardEntity>> = repository.allCards
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Collect cards and maintain active card
        viewModelScope.launch {
            allCards.collect { cardsList ->
                // Remove legacy default "BLACK EDITION" text from existing saved cards
                cardsList.filter { it.tier.equals("BLACK EDITION", ignoreCase = true) }.forEach { card ->
                    repository.updateCard(card.copy(tier = ""))
                }

                _uiState.update { current ->
                    val currentSelected = current.selectedCard
                    val newSelected = if (cardsList.isEmpty()) {
                        null
                    } else if (currentSelected != null) {
                        cardsList.find { it.id == currentSelected.id } ?: cardsList.first()
                    } else {
                        cardsList.find { it.isDefault } ?: cardsList.first()
                    }
                    val currentPeriod = getCurrentActivePeriod(newSelected)
                    current.copy(
                        cards = cardsList,
                        selectedCard = newSelected,
                        selectedPeriod = if (current.selectedPeriod.isBlank()) currentPeriod else current.selectedPeriod
                    )
                }
            }
        }
    }

    fun selectTab(tab: String) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectCard(card: CreditCardEntity) {
        val currentPeriod = getCurrentActivePeriod(card)
        _uiState.update { it.copy(selectedCard = card, selectedPeriod = currentPeriod, selectedExactDate = null) }
        showToast("Tarjeta activa: ${card.cardName}")
    }

    fun switchCard() {
        val currentCards = _uiState.value.cards
        val current = _uiState.value.selectedCard
        if (currentCards.size > 1 && current != null) {
            val next = currentCards.firstOrNull { it.id != current.id } ?: currentCards.first()
            val currentPeriod = getCurrentActivePeriod(next)
            _uiState.update { it.copy(selectedCard = next, selectedPeriod = currentPeriod, selectedExactDate = null) }
            showToast("Cambiando a ${next.cardName}")
        }
    }

    fun selectPeriod(period: String) {
        _uiState.update { it.copy(selectedPeriod = period, selectedExactDate = null) }
        showToast("Periodo: $period")
    }

    fun setExactDateFilter(date: String?) {
        _uiState.update { it.copy(selectedExactDate = date) }
        if (date != null) {
            showToast("Filtrando por fecha: $date")
        } else {
            showToast("Filtro de fecha exacta removido")
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        showToast("Orden: ${order.displayName}")
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active, searchQuery = if (!active) "" else it.searchQuery) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    // Modal triggers
    fun promptDeleteExpense(expense: ExpenseEntity) {
        _uiState.update { it.copy(expenseToDelete = expense) }
    }

    fun dismissDeleteExpense() {
        _uiState.update { it.copy(expenseToDelete = null) }
    }

    fun confirmDeleteExpense() {
        val target = _uiState.value.expenseToDelete ?: return
        viewModelScope.launch {
            repository.deleteExpenseById(target.id)
            _uiState.update { it.copy(expenseToDelete = null) }
            showToast("Consumo eliminado correctamente")
        }
    }

    fun deleteExpenseDirectly(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpenseById(expenseId)
            _uiState.update { it.copy(expenseToDelete = null, editingExpense = null) }
            showToast("Consumo eliminado correctamente")
        }
    }

    fun promptDeleteCard(card: CreditCardEntity) {
        _uiState.update { it.copy(cardToDelete = card) }
    }

    fun dismissDeleteCard() {
        _uiState.update { it.copy(cardToDelete = null) }
    }

    fun confirmDeleteCard() {
        val target = _uiState.value.cardToDelete ?: return
        val app = getApplication<Application>()
        NotificationScheduler.cancelCardPaymentAlarm(app, target.id)
        viewModelScope.launch {
            repository.deleteCardById(target.id)
            _uiState.update { current ->
                val remaining = current.cards.filter { it.id != target.id }
                val newSelected = if (current.selectedCard?.id == target.id) {
                    remaining.firstOrNull()
                } else {
                    current.selectedCard
                }
                current.copy(
                    cardToDelete = null,
                    cards = remaining,
                    selectedCard = newSelected
                )
            }
            showToast("Tarjeta eliminada correctamente")
        }
    }

    fun openCycleConfig(card: CreditCardEntity) {
        _uiState.update { it.copy(cycleConfigCard = card) }
    }

    fun closeCycleConfig() {
        _uiState.update { it.copy(cycleConfigCard = null) }
    }

    fun saveCycleConfig(cardId: Long, newCutDay: Int, newPayDay: Int, newPayBusinessDays: Int = 12) {
        viewModelScope.launch {
            val card = _uiState.value.cards.find { it.id == cardId } ?: return@launch
            val updated = card.copy(
                cutDay = newCutDay,
                payDay = newPayDay,
                payBusinessDays = newPayBusinessDays
            )
            repository.updateCard(updated)
            _uiState.update { current ->
                val updatedCards = current.cards.map { if (it.id == cardId) updated else it }
                val updatedSelected = if (current.selectedCard?.id == cardId) updated else current.selectedCard
                current.copy(
                    cycleConfigCard = null,
                    cards = updatedCards,
                    selectedCard = updatedSelected
                )
            }
            showToast("Ciclo de \"${card.cardName}\" actualizado localmente.")
        }
    }

    fun openAddCard() {
        _uiState.update { it.copy(isAddCardOpen = true) }
    }

    fun closeAddCard() {
        _uiState.update { it.copy(isAddCardOpen = false) }
    }

    fun openEditCard(card: CreditCardEntity) {
        _uiState.update { it.copy(editingCard = card) }
    }

    fun closeEditCard() {
        _uiState.update { it.copy(editingCard = null) }
    }

    fun updateCardDetails(
        cardId: Long,
        bankName: String,
        cardName: String,
        network: String,
        tier: String,
        last4: String,
        cutDay: Int,
        payBusinessDays: Int,
        payDay: Int,
        creditLimit: Double,
        style: String
    ) {
        viewModelScope.launch {
            val existing = _uiState.value.cards.find { it.id == cardId } ?: return@launch
            val updated = existing.copy(
                bankName = bankName.ifBlank { existing.bankName },
                cardName = cardName.ifBlank { existing.cardName },
                network = network,
                tier = tier,
                last4 = last4.ifBlank { existing.last4 },
                cutDay = cutDay,
                payBusinessDays = payBusinessDays,
                payDay = payDay,
                creditLimit = creditLimit,
                cardColorTheme = style.ifBlank { existing.cardColorTheme }
            )
            repository.updateCard(updated)
            _uiState.update { current ->
                val updatedCards = current.cards.map { if (it.id == cardId) updated else it }
                val updatedSelected = if (current.selectedCard?.id == cardId) updated else current.selectedCard
                current.copy(
                    editingCard = null,
                    cards = updatedCards,
                    selectedCard = updatedSelected
                )
            }
            showToast("Tarjeta \"${updated.cardName}\" actualizada correctamente")
        }
    }

    fun addNewCard(
        bankName: String,
        cardName: String,
        network: String,
        tier: String,
        last4: String,
        cutDay: Int,
        payDay: Int,
        style: String,
        payBusinessDays: Int = 15,
        creditLimit: Double = 5000.0
    ) {
        viewModelScope.launch {
            val newCard = CreditCardEntity(
                bankName = bankName.ifBlank { "BANCO INTERNACIONAL" },
                cardName = cardName.ifBlank { if (network.contains("VISA", ignoreCase = true)) "Visa $tier" else "Mastercard $tier" },
                network = network,
                tier = tier,
                last4 = last4.ifBlank { "0000" },
                cutDay = cutDay,
                payDay = payDay,
                payBusinessDays = payBusinessDays,
                cardColorTheme = style.ifBlank { if (network.contains("VISA", ignoreCase = true)) "blue" else "black" },
                creditLimit = creditLimit,
                isDefault = false
            )
            val newId = repository.insertCard(newCard)
            val insertedCard = newCard.copy(id = newId)
            _uiState.update { current ->
                val newCards = current.cards + insertedCard
                current.copy(
                    isAddCardOpen = false,
                    cards = newCards,
                    selectedCard = current.selectedCard ?: insertedCard
                )
            }
            showToast("Tarjeta agregada a la bóveda SQLite")
        }
    }

    fun openChangePin() {
        _uiState.update { it.copy(isChangePinOpen = true) }
    }

    fun closeChangePin() {
        _uiState.update { it.copy(isChangePinOpen = false) }
    }

    fun changeMasterPin(currentInput: String, nextInput: String, confirmInput: String): Boolean {
        if (currentInput != _uiState.value.masterPin) {
            showToast("PIN actual incorrecto")
            return false
        }
        if (nextInput.length !in 4..6 || !nextInput.all { it.isDigit() }) {
            showToast("El nuevo PIN debe tener entre 4 y 6 dígitos")
            return false
        }
        if (nextInput != confirmInput) {
            showToast("Los PINs no coinciden")
            return false
        }
        prefs.edit().putString("master_pin", nextInput).apply()
        _uiState.update { it.copy(masterPin = nextInput, isChangePinOpen = false) }
        showToast("PIN actualizado con éxito en la bóveda local")
        return true
    }

    fun toggleBiometrics(active: Boolean) {
        prefs.edit().putBoolean("biometrics_enabled", active).apply()
        _uiState.update { it.copy(isBiometricsActive = active) }
        showToast(if (active) "Biometría activada. Se solicitará la huella al abrir la app." else "Biometría desactivada.")
    }

    fun lockApp() {
        _uiState.update { it.copy(isVaultUnlocked = false) }
        showToast("Bóveda bloqueada")
    }

    fun openClearDb() {
        _uiState.update { it.copy(isClearDbOpen = true) }
    }

    fun closeClearDb() {
        _uiState.update { it.copy(isClearDbOpen = false) }
    }

    fun confirmClearDb() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiState.update { it.copy(isClearDbOpen = false, selectedCard = null, cards = emptyList()) }
            showToast("Base SQLite purgada completamente (tarjetas y consumos)")
        }
    }

    fun finishInitialSetup(newPin: String, enableBiometrics: Boolean = false): Boolean {
        if (newPin.length !in 4..6 || !newPin.all { it.isDigit() }) {
            showToast("El PIN debe tener entre 4 y 6 dígitos numéricos")
            return false
        }
        prefs.edit()
            .putBoolean("is_first_launch", false)
            .putString("master_pin", newPin)
            .putBoolean("biometrics_enabled", enableBiometrics)
            .apply()
        _uiState.update {
            it.copy(
                isFirstLaunch = false,
                masterPin = newPin,
                isBiometricsActive = enableBiometrics,
                isVaultUnlocked = true,
                failedAttempts = 0
            )
        }
        showToast("PIN de seguridad configurado exitosamente.")
        return true
    }

    fun exportDatabaseJson(): String {
        val cards = _uiState.value.cards
        val expenses = allExpenses.value
        val json = JSONObject()
        json.put("app", "Creditia")
        json.put("version", 1)
        json.put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        json.put("cards_count", cards.size)
        json.put("expenses_count", expenses.size)

        val cardsArray = JSONArray()
        for (c in cards) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("bankName", c.bankName)
            obj.put("cardName", c.cardName)
            obj.put("network", c.network)
            obj.put("tier", c.tier)
            obj.put("last4", c.last4)
            obj.put("cutDay", c.cutDay)
            obj.put("payDay", c.payDay)
            obj.put("cardColorTheme", c.cardColorTheme)
            obj.put("creditLimit", c.creditLimit)
            obj.put("isDefault", c.isDefault)
            cardsArray.put(obj)
        }
        json.put("cards", cardsArray)

        val expensesArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("cardId", e.cardId)
            obj.put("sequenceNumber", e.sequenceNumber)
            obj.put("amount", e.amount)
            obj.put("establishment", e.establishment)
            obj.put("description", e.description)
            obj.put("dateString", e.dateString)
            obj.put("billingPeriod", e.billingPeriod)
            obj.put("timestamp", e.timestamp)
            expensesArray.put(obj)
        }
        json.put("expenses", expensesArray)
        return json.toString(2)
    }

    suspend fun restoreDatabaseFromJson(jsonContent: String): Pair<Int, Int> {
        val root = JSONObject(jsonContent)
        val cardsArray = root.optJSONArray("cards") ?: JSONArray()
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()

        repository.clearAllData()

        val idMapping = mutableMapOf<Long, Long>()
        var restoredCards = 0
        var restoredExpenses = 0

        for (i in 0 until cardsArray.length()) {
            val obj = cardsArray.getJSONObject(i)
            val oldId = obj.optLong("id", 0L)
            val card = CreditCardEntity(
                bankName = obj.optString("bankName", "BANCO"),
                cardName = obj.optString("cardName", "TARJETA"),
                network = obj.optString("network", "VISA"),
                tier = obj.optString("tier", "CLASSIC"),
                last4 = obj.optString("last4", "0000"),
                cutDay = obj.optInt("cutDay", 24),
                payDay = obj.optInt("payDay", 14),
                cardColorTheme = obj.optString("cardColorTheme", "blue"),
                creditLimit = obj.optDouble("creditLimit", 4000.0),
                isDefault = obj.optBoolean("isDefault", false)
            )
            val newId = repository.insertCard(card)
            idMapping[oldId] = newId
            restoredCards++
        }

        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.getJSONObject(i)
            val oldCardId = obj.optLong("cardId", 0L)
            val targetCardId = idMapping[oldCardId] ?: idMapping.values.firstOrNull() ?: 1L
            val expense = ExpenseEntity(
                cardId = targetCardId,
                sequenceNumber = obj.optInt("sequenceNumber", i + 1),
                amount = obj.optDouble("amount", 0.0),
                establishment = obj.optString("establishment", "Comercio"),
                description = obj.optString("description", "Consumo"),
                dateString = obj.optString("dateString", "2026/10/01"),
                billingPeriod = obj.optString("billingPeriod", "Octubre 2026"),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
            repository.insertExpense(expense)
            restoredExpenses++
        }

        val allCardsList = repository.getAllCardsList()
        val firstCard = allCardsList.firstOrNull()
        _uiState.update { current ->
            current.copy(
                cards = allCardsList,
                selectedCard = firstCard,
                currentTab = "consumos",
                isSearchActive = false,
                searchQuery = ""
            )
        }
        showToast("✓ Restauración completada: $restoredCards tarjetas y $restoredExpenses consumos")
        return Pair(restoredCards, restoredExpenses)
    }

    fun setPinSetupMode(setup: Boolean) {
        _uiState.update { it.copy(isPinSetupMode = setup) }
    }

    fun unlockVault() {
        _uiState.update { it.copy(isVaultUnlocked = true, failedAttempts = 0) }
        showToast("Bóveda local desbloqueada")
    }

    fun verifyPin(inputPin: String): Boolean {
        val master = _uiState.value.masterPin
        return if (inputPin == master) {
            _uiState.update { it.copy(isVaultUnlocked = true, failedAttempts = 0) }
            showToast("✓ Bóveda local desbloqueada")
            true
        } else {
            recordFailedAttempt()
            showToast("PIN incorrecto. Intenta de nuevo.")
            false
        }
    }

    fun checkPinOnly(inputPin: String): Boolean {
        return inputPin == _uiState.value.masterPin
    }

    fun recordFailedAttempt() {
        _uiState.update {
            val next = it.failedAttempts + 1
            it.copy(failedAttempts = next)
        }
    }

    fun startEditExpense(expense: ExpenseEntity) {
        _uiState.update { it.copy(editingExpense = expense) }
    }

    fun clearEditingExpense() {
        _uiState.update { it.copy(editingExpense = null) }
    }

    fun saveExpense(
        id: Long?,
        cardId: Long,
        amount: Double,
        establishment: String,
        description: String,
        dateString: String
    ) {
        viewModelScope.launch {
            val card = _uiState.value.cards.find { it.id == cardId } ?: _uiState.value.selectedCard
            val cutDay = card?.cutDay ?: 24
            val billingPeriod = repository.calculateBillingPeriod(dateString, cutDay)

            if (id != null && id > 0) {
                val existing = _uiState.value.editingExpense
                val seq = existing?.sequenceNumber ?: repository.getNextSequenceNumber()
                val updated = ExpenseEntity(
                    id = id,
                    sequenceNumber = seq,
                    cardId = cardId,
                    establishment = establishment.ifBlank { "Consumo General" },
                    description = description.ifBlank { "Sin descripción" },
                    amount = amount,
                    dateString = dateString,
                    billingPeriod = billingPeriod,
                    timestamp = existing?.timestamp ?: System.currentTimeMillis()
                )
                repository.updateExpense(updated)
                showToast("Consumo #$seq actualizado")
            } else {
                val nextSeq = repository.getNextSequenceNumber()
                val newExpense = ExpenseEntity(
                    sequenceNumber = nextSeq,
                    cardId = cardId,
                    establishment = establishment.ifBlank { "Consumo General" },
                    description = description.ifBlank { "Sin descripción" },
                    amount = amount,
                    dateString = dateString,
                    billingPeriod = billingPeriod,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertExpense(newExpense)
                showToast("¡Consumo N° $nextSeq Registrado!")
            }
            _uiState.update { it.copy(editingExpense = null) }
        }
    }

    // Helper functions for UI
    fun getFilteredExpenses(
        expenses: List<ExpenseEntity>,
        selectedCard: CreditCardEntity?,
        selectedPeriod: String,
        selectedExactDate: String?,
        searchQuery: String,
        sortOrder: SortOrder
    ): List<ExpenseEntity> {
        val cardId = selectedCard?.id ?: return emptyList()
        var list = expenses.filter { it.cardId == cardId }

        if (!selectedExactDate.isNullOrBlank()) {
            list = list.filter { it.dateString == selectedExactDate }
        } else if (selectedPeriod != "Todos los periodos" && selectedPeriod != "Todos los históricos") {
            list = list.filter { it.billingPeriod.equals(selectedPeriod, ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            list = list.filter {
                it.establishment.lowercase(Locale.ROOT).contains(q) ||
                        it.description.lowercase(Locale.ROOT).contains(q) ||
                        it.amount.toString().contains(q)
            }
        }

        return when (sortOrder) {
            SortOrder.DATE_DESC -> list.sortedWith(compareByDescending<ExpenseEntity> { it.dateString }.thenByDescending { it.sequenceNumber })
            SortOrder.DATE_ASC -> list.sortedWith(compareBy<ExpenseEntity> { it.dateString }.thenBy { it.sequenceNumber })
            SortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            SortOrder.AMOUNT_ASC -> list.sortedBy { it.amount }
        }
    }

    fun calculatePeriodSum(expenses: List<ExpenseEntity>, cardId: Long, period: String, exactDate: String? = null): Double {
        if (!exactDate.isNullOrBlank()) {
            return expenses.filter { it.cardId == cardId && it.dateString == exactDate }.sumOf { it.amount }
        }
        val target = if (period == "Todos los periodos" || period == "Todos los históricos") {
            expenses.filter { it.cardId == cardId }
        } else {
            expenses.filter { it.cardId == cardId && it.billingPeriod.equals(period, ignoreCase = true) }
        }
        return target.sumOf { it.amount }
    }

    fun calculateHistoricalTotal(expenses: List<ExpenseEntity>, cardId: Long): Double {
        return expenses.filter { it.cardId == cardId }.sumOf { it.amount }
    }

    fun getDaysUntilCut(cutDay: Int): Int {
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        return if (currentDay <= cutDay) {
            cutDay - currentDay
        } else {
            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            (maxDaysInMonth - currentDay) + cutDay
        }
    }

    /**
     * Calculates the exact maximum payment date for a given billing period (e.g. "Octubre 2026", "Noviembre 2026").
     * The payment date is established by taking the upper cut date of the current period ("fecha superior del corte del periodo actual")
     * and adding the configured business days (skipping Saturdays and Sundays).
     * For example, upper cut date of Oct 2026 is 24 Oct 2026 + 12 business days = 10 Nov 2026 (10/11/2026).
     * Upper cut date of Nov 2026 is 24 Nov 2026 + 12 business days = 10 Dec 2026 (10/12/2026).
     */
    fun getPaymentLimitDateForPeriod(card: CreditCardEntity?, periodName: String): String {
        if (card == null) return "10/11/2026"
        val monthNames = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val cal = Calendar.getInstance()
        var targetYear = cal.get(Calendar.YEAR)
        var targetMonth = cal.get(Calendar.MONTH) // 0-based

        val parts = periodName.trim().split(" ")
        if (parts.size == 2) {
            val mIdx = monthNames.indexOf(parts[0])
            val yr = parts[1].toIntOrNull()
            if (mIdx != -1 && yr != null) {
                targetYear = yr
                targetMonth = mIdx
            }
        }

        val cutCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, card.cutDay.coerceIn(1, maxDay))
        }

        var remainingBusinessDays = card.payBusinessDays.coerceIn(1, 30)
        while (remainingBusinessDays > 0) {
            cutCal.add(Calendar.DAY_OF_MONTH, 1)
            val dow = cutCal.get(Calendar.DAY_OF_WEEK)
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                remainingBusinessDays--
            }
        }

        val day = String.format(Locale.US, "%02d", cutCal.get(Calendar.DAY_OF_MONTH))
        val month = String.format(Locale.US, "%02d", cutCal.get(Calendar.MONTH) + 1)
        val year = cutCal.get(Calendar.YEAR)
        return "$day/$month/$year"
    }

    fun getPaymentLimitDateDescription(card: CreditCardEntity?, periodName: String): String {
        if (card == null) return "10 de noviembre de 2026"
        val monthNames = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val cal = Calendar.getInstance()
        var targetYear = cal.get(Calendar.YEAR)
        var targetMonth = cal.get(Calendar.MONTH)

        val parts = periodName.trim().split(" ")
        if (parts.size == 2) {
            val mIdx = monthNames.indexOf(parts[0])
            val yr = parts[1].toIntOrNull()
            if (mIdx != -1 && yr != null) {
                targetYear = yr
                targetMonth = mIdx
            }
        }

        val cutCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, card.cutDay.coerceIn(1, maxDay))
        }

        var remainingBusinessDays = card.payBusinessDays.coerceIn(1, 30)
        while (remainingBusinessDays > 0) {
            cutCal.add(Calendar.DAY_OF_MONTH, 1)
            val dow = cutCal.get(Calendar.DAY_OF_WEEK)
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                remainingBusinessDays--
            }
        }

        val dayNames = listOf("", "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val monthLower = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        val dName = dayNames.getOrElse(cutCal.get(Calendar.DAY_OF_WEEK)) { "" }
        val mName = monthLower.getOrElse(cutCal.get(Calendar.MONTH)) { "" }
        val dNum = cutCal.get(Calendar.DAY_OF_MONTH)
        val yr = cutCal.get(Calendar.YEAR)
        return "$dName, $dNum de $mName de $yr"
    }

    fun getPaymentLimitDate(payDay: Int = 0): String {
        val targetCard = _uiState.value.selectedCard ?: _uiState.value.cards.firstOrNull()
        return if (targetCard != null) {
            val currentPeriod = getCurrentActivePeriod(targetCard)
            getPaymentLimitDateForPeriod(targetCard, currentPeriod)
        } else {
            "10/11/2026"
        }
    }

    fun getCurrentActivePeriod(card: CreditCardEntity? = null): String {
        val cutDay = card?.cutDay ?: 24
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val todayStr = String.format(Locale.US, "%04d/%02d/%02d", currentYear, currentMonth, currentDay)
        return repository.calculateBillingPeriod(todayStr, cutDay)
    }

    /**
     * Computes available periods showing ONLY periods that have stored records in the database.
     * The CURRENT active period is placed at the very beginning if it has records,
     * followed by remaining expense periods in descending chronological order, and "Todos los históricos".
     */
    fun getAvailablePeriods(card: CreditCardEntity?, expenses: List<ExpenseEntity>): List<String> {
        val currentActivePeriod = getCurrentActivePeriod(card)

        val cardExpenses = if (card != null) expenses.filter { it.cardId == card.id } else expenses
        val monthNames = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        fun parseYearMonth(p: String): Pair<Int, Int>? {
            val parts = p.trim().split(" ")
            if (parts.size == 2) {
                val mIdx = monthNames.indexOf(parts[0])
                val yr = parts[1].toIntOrNull()
                if (mIdx != -1 && yr != null) {
                    return Pair(yr, mIdx)
                }
            }
            return null
        }

        // Obtener ÚNICAMENTE los periodos que tienen registros almacenados
        val expensePeriods = cardExpenses
            .map { it.billingPeriod }
            .filter { it.isNotBlank() && it != "Todos los históricos" }
            .distinct()

        if (expensePeriods.isEmpty()) {
            return listOf(currentActivePeriod)
        }

        // Ordenar periodos con registros almacenados cronológicamente descendente (más reciente primero)
        val sortedPeriods = expensePeriods
            .mapNotNull { p ->
                parseYearMonth(p)?.let { Triple(it.first, it.second, p) }
            }.sortedWith(compareByDescending<Triple<Int, Int, String>> { it.first }.thenByDescending { it.second })
            .map { it.third }

        // Si el periodo actual tiene registros almacenados, colocarlo al principio de la lista
        val orderedList = if (currentActivePeriod in sortedPeriods) {
            listOf(currentActivePeriod) + sortedPeriods.filter { it != currentActivePeriod }
        } else {
            sortedPeriods
        }

        return if (orderedList.size > 1) {
            orderedList + listOf("Todos los históricos")
        } else {
            orderedList
        }
    }

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Recordatorios de Pago",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para no olvidar pagar la tarjeta antes de la fecha máxima de pago"
                enableLights(true)
                enableVibration(true)
            }
            val manager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun togglePaymentNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("payment_notifications_enabled", enabled).apply()
        _uiState.update { it.copy(isPaymentNotificationEnabled = enabled) }
        if (enabled) {
            ensureNotificationChannel()
            schedulePaymentAlarm()
            showToast("✓ Notificaciones de pago activadas y programadas")
        } else {
            cancelPaymentAlarm()
            showToast("Notificaciones de pago desactivadas")
        }
    }

    fun setNotificationLeadDays(days: Int) {
        prefs.edit().putInt("notification_lead_days", days).apply()
        _uiState.update { it.copy(notificationLeadDays = days) }
        if (_uiState.value.isPaymentNotificationEnabled) {
            schedulePaymentAlarm()
        }
        showToast("Aviso configurado: $days días antes del pago")
    }

    fun schedulePaymentAlarm() {
        val app = getApplication<Application>()
        ensureNotificationChannel()
        val cards = _uiState.value.cards
        if (cards.isEmpty()) return
        val leadDays = _uiState.value.notificationLeadDays
        NotificationScheduler.scheduleAllCards(app, cards, leadDays)
    }

    fun cancelPaymentAlarm() {
        val app = getApplication<Application>()
        val cards = _uiState.value.cards
        NotificationScheduler.cancelAllCards(app, cards)
    }

    fun getUpcomingAlertSchedule(card: CreditCardEntity? = null): PaymentAlertSchedule? {
        val targetCard = card ?: _uiState.value.selectedCard ?: _uiState.value.cards.firstOrNull() ?: return null
        return NotificationScheduler.calculateUpcomingAlert(
            card = targetCard,
            leadDays = _uiState.value.notificationLeadDays
        )
    }

    fun calculateNotificationDate(paymentLimitDateStr: String, leadDays: Int): String {
        return try {
            val parts = paymentLimitDateStr.split("/")
            if (parts.size == 3) {
                val d = parts[0].toInt()
                val m = parts[1].toInt() - 1
                val y = parts[2].toInt()
                val cal = Calendar.getInstance()
                cal.set(y, m, d)
                cal.add(Calendar.DAY_OF_MONTH, -leadDays)
                String.format(Locale.US, "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun sendTestPaymentNotification(card: CreditCardEntity? = null) {
        val targetCard = card ?: _uiState.value.selectedCard ?: _uiState.value.cards.firstOrNull()
        val app = getApplication<Application>()
        ensureNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            showToast("Permiso de notificaciones pendiente por autorizar")
            return
        }

        if (targetCard == null) {
            showToast("No hay tarjetas para programar aviso")
            return
        }

        val schedule = NotificationScheduler.calculateUpcomingAlert(
            card = targetCard,
            leadDays = _uiState.value.notificationLeadDays
        )

        try {
            val notifId = (PAYMENT_NOTIFICATION_ID + targetCard.id).toInt()
            val builder = NotificationCompat.Builder(app, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(com.example.R.mipmap.ic_launcher)
                .setContentTitle("🔔 Recordatorio de Pago: ${schedule.cardName}")
                .setContentText("Fecha máxima de pago: ${schedule.paymentLimitDateStr}. ¡Aviso ${schedule.leadDays} días antes!")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "¡No olvides realizar el pago de tu tarjeta ${schedule.cardName}!\n\n" +
                        "• Periodo contable: ${schedule.periodName}\n" +
                        "• Fecha máxima de pago: ${schedule.paymentLimitDesc}\n" +
                        "• Aviso establecido: ${schedule.alertDateDesc} (a las 09:00 AM)\n" +
                        "• Anticipación configurada: ${schedule.leadDays} días antes\n\n" +
                        "La fecha de aviso llegará exactamente en la fecha establecida."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            NotificationManagerCompat.from(app).notify(notifId, builder.build())
            showToast("✓ Notificación de prueba enviada (Fecha establecida: ${schedule.alertDateStr})")
        } catch (e: Exception) {
            showToast("Error al enviar notificación: ${e.localizedMessage}")
        }
    }
}
