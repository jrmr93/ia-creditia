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
import com.example.util.GeminiService
import com.example.util.GeminiTestResult
import com.example.util.ReceiptStorageUtil
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

enum class SortOrder(val displayName: String, val shortLabel: String) {
    DATE_DESC("Fecha (más reciente)", "🕒 Más reciente"),
    DATE_ASC("Fecha (más antigua)", "📅 Más antiguo"),
    AMOUNT_DESC("Monto (mayor a menor)", "▲ Mayor monto"),
    AMOUNT_ASC("Monto (menor a mayor)", "▼ Menor monto")
}

data class PeriodFinancialSummary(
    val totalExpenses: Double = 0.0,
    val totalPayments: Double = 0.0,
    val totalCreditNotes: Double = 0.0,
    val netExpenses: Double = 0.0,
    val finalPaymentAmount: Double = 0.0
)

data class CreditiaUiState(
    val cards: List<CreditCardEntity> = emptyList(),
    val selectedCard: CreditCardEntity? = null,
    val selectedPeriod: String = "",
    val selectedExactDate: String? = null,
    val filterOnlyDeferred: Boolean = false,
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
    // Selection state for multi-defer
    val isSelectionMode: Boolean = false,
    val selectedExpenseIds: Set<Long> = emptySet(),
    // Modals
    val expenseToDelete: ExpenseEntity? = null,
    val cardToDelete: CreditCardEntity? = null,
    val editingExpense: ExpenseEntity? = null,
    val cycleConfigCard: CreditCardEntity? = null,
    val editingCard: CreditCardEntity? = null,
    val isAddCardOpen: Boolean = false,
    val isChangePinOpen: Boolean = false,
    val isClearDbOpen: Boolean = false,
    val availablePeriods: List<String> = emptyList(),
    // Gemini AI Config & State
    val isGeminiAiEnabled: Boolean = true,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.5-flash",
    val geminiTestStatus: GeminiTestResult? = null,
    val isTestingGemini: Boolean = false,
    val isAnalyzingReceipt: Boolean = false,
    val availableModelOptions: List<String> = emptyList(),
    val isDetectingModels: Boolean = false,
    // Logo Search Modal State
    val isLogoSearchModalOpen: Boolean = false,
    val logoTargetExpense: ExpenseEntity? = null,
    val logoSearchQuery: String = "",
    val logoSearchResults: List<com.example.util.LogoCandidate> = emptyList(),
    val isSearchingLogos: Boolean = false,
    val logoSearchError: String? = null,
    val isApplyingLogo: Boolean = false,
    val lastAppliedLogoPath: String? = null,
    val customCycleRanges: Map<String, String> = emptyMap(),
    val customCutDates: Map<String, String> = emptyMap(),
    val customPaymentLimits: Map<String, String> = emptyMap()
)

class CreditiaViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("creditia_secure_prefs", android.content.Context.MODE_PRIVATE)
    private val logoPrefs = application.getSharedPreferences("creditia_establishment_logos", android.content.Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    val repository = CreditiaRepository(database.creditCardDao(), database.expenseDao())

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "creditia_payment_reminders"
        const val PAYMENT_NOTIFICATION_ID = 2001
    }

    fun normalizeEstablishmentKey(name: String): String {
        return name.trim().lowercase(Locale.ROOT)
    }

    fun getRememberedLogo(establishment: String): String? {
        val key = normalizeEstablishmentKey(establishment)
        if (key.isBlank()) return null
        return logoPrefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    fun rememberEstablishmentLogo(establishment: String, logoPath: String) {
        val key = normalizeEstablishmentKey(establishment)
        if (key.isBlank() || logoPath.isBlank()) return
        logoPrefs.edit().putString(key, logoPath).apply()
    }

    fun forgetEstablishmentLogo(establishment: String) {
        val key = normalizeEstablishmentKey(establishment)
        if (key.isBlank()) return
        logoPrefs.edit().remove(key).apply()
    }

    private val _uiState = MutableStateFlow(
        run {
            val savedPin = prefs.getString("master_pin", "") ?: ""
            val isFirst = prefs.getBoolean("is_first_launch", true) || savedPin.isEmpty()
            val biometricsEnabled = prefs.getBoolean("biometrics_enabled", true)
            val notifEnabled = prefs.getBoolean("payment_notifications_enabled", false)
            val leadDays = prefs.getInt("notification_lead_days", 3)
            val aiEnabled = prefs.getBoolean("gemini_ai_enabled", true)
            val savedGeminiKey = prefs.getString("gemini_api_key", "") ?: ""
            val rawSavedModel = prefs.getString("gemini_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
            val legacyModels = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro", "gemini-1.5-flash-latest", "gemini-2.0-flash-exp")
            val savedGeminiModel = if (rawSavedModel in legacyModels) {
                prefs.edit().putString("gemini_model", "gemini-3.5-flash").apply()
                "gemini-3.5-flash"
            } else {
                rawSavedModel
            }
            val initialCustomRanges = mutableMapOf<String, String>()
            val initialCustomCutDates = mutableMapOf<String, String>()
            val initialCustomPaymentLimits = mutableMapOf<String, String>()
            for ((k, v) in prefs.all) {
                if (k.startsWith("custom_cycle_range_") && v is String) {
                    initialCustomRanges[k.removePrefix("custom_cycle_range_")] = v
                } else if (k.startsWith("custom_cut_date_") && v is String) {
                    initialCustomCutDates[k.removePrefix("custom_cut_date_")] = v
                } else if (k.startsWith("custom_payment_limit_") && v is String) {
                    initialCustomPaymentLimits[k.removePrefix("custom_payment_limit_")] = v
                }
            }
            CreditiaUiState(
                isBiometricsActive = biometricsEnabled,
                masterPin = savedPin,
                isFirstLaunch = isFirst,
                isVaultUnlocked = false, // Always require authentication on launch
                isPaymentNotificationEnabled = notifEnabled,
                notificationLeadDays = leadDays,
                isGeminiAiEnabled = aiEnabled,
                geminiApiKey = savedGeminiKey,
                geminiModel = savedGeminiModel,
                customCycleRanges = initialCustomRanges,
                customCutDates = initialCustomCutDates,
                customPaymentLimits = initialCustomPaymentLimits
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

        // Cache remembered logos from existing expenses in database
        viewModelScope.launch {
            allExpenses.collect { expenses ->
                val editor = logoPrefs.edit()
                var changed = false
                expenses.forEach { exp ->
                    val logo = exp.establishmentLogoUrl
                    if (!logo.isNullOrBlank() && exp.establishment.isNotBlank()) {
                        val key = normalizeEstablishmentKey(exp.establishment)
                        if (!logoPrefs.contains(key)) {
                            editor.putString(key, logo)
                            changed = true
                        }
                    }
                }
                if (changed) editor.apply()
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

    fun toggleFilterOnlyDeferred(onlyDeferred: Boolean? = null) {
        val next = onlyDeferred ?: !_uiState.value.filterOnlyDeferred
        _uiState.update { it.copy(filterOnlyDeferred = next) }
        showToast(if (next) "Filtrando: Solo compras diferidas" else "Mostrando todos los consumos")
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
        val all = allExpenses.value
        if (expense.isLockedByActiveDeferred(all)) {
            showToast(
                if (expense.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden eliminar"
                else "Registro bloqueado: no se puede eliminar porque tiene cuotas diferidas activas"
            )
            return
        }
        _uiState.update { it.copy(expenseToDelete = expense) }
    }

    fun dismissDeleteExpense() {
        _uiState.update { it.copy(expenseToDelete = null) }
    }

    fun confirmDeleteExpense() {
        val target = _uiState.value.expenseToDelete ?: return
        val all = allExpenses.value
        if (target.isLockedByActiveDeferred(all)) {
            showToast(
                if (target.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden eliminar"
                else "Registro bloqueado: no se puede eliminar porque tiene cuotas diferidas activas"
            )
            _uiState.update { it.copy(expenseToDelete = null) }
            return
        }
        viewModelScope.launch {
            ReceiptStorageUtil.deleteReceiptFile(target.receiptUri)
            val count = repository.deleteExpenseWithCascade(target)
            _uiState.update { it.copy(expenseToDelete = null) }
            if (count > 1) {
                showToast("✓ Se eliminaron las $count cuotas del consumo diferido de todos los periodos")
            } else {
                showToast("Consumo eliminado correctamente")
            }
        }
    }

    fun deleteExpenseDirectly(expenseId: Long) {
        viewModelScope.launch {
            val target = repository.getExpenseById(expenseId)
            val all = allExpenses.value
            if (target != null && target.isLockedByActiveDeferred(all)) {
                showToast(
                    if (target.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden eliminar"
                    else "Registro bloqueado: no se puede eliminar porque tiene cuotas diferidas activas"
                )
                _uiState.update { it.copy(expenseToDelete = null, editingExpense = null) }
                return@launch
            }
            if (target != null) {
                ReceiptStorageUtil.deleteReceiptFile(target.receiptUri)
            }
            val count = repository.deleteExpenseByIdCascade(expenseId)
            _uiState.update { it.copy(expenseToDelete = null, editingExpense = null) }
            if (count > 1) {
                showToast("✓ Se eliminaron las $count cuotas del consumo diferido de todos los periodos")
            } else {
                showToast("Consumo eliminado correctamente")
            }
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
            val cardExpenses = repository.getExpensesListByCard(target.id)
            ReceiptStorageUtil.deleteReceiptsForExpenses(cardExpenses)
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

    fun toggleSelectionMode(enabled: Boolean) {
        _uiState.update { it.copy(isSelectionMode = enabled, selectedExpenseIds = if (!enabled) emptySet() else it.selectedExpenseIds) }
    }

    fun toggleExpenseSelection(expenseId: Long) {
        val all = allExpenses.value
        val target = all.find { it.id == expenseId }
        if (target != null && target.isLockedByActiveDeferred(all)) {
            showToast("Registro bloqueado: no se puede seleccionar ni agrupar")
            return
        }
        _uiState.update { current ->
            val newSet = if (expenseId in current.selectedExpenseIds) {
                current.selectedExpenseIds - expenseId
            } else {
                current.selectedExpenseIds + expenseId
            }
            current.copy(
                selectedExpenseIds = newSet,
                isSelectionMode = newSet.isNotEmpty()
            )
        }
    }

    fun selectAllExpenses(expenseIds: Set<Long>) {
        _uiState.update { it.copy(selectedExpenseIds = expenseIds, isSelectionMode = expenseIds.isNotEmpty()) }
    }

    fun clearExpenseSelection() {
        _uiState.update { it.copy(selectedExpenseIds = emptySet(), isSelectionMode = false) }
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun updateExpenseReceipt(expense: ExpenseEntity, newReceiptUri: String) {
        viewModelScope.launch {
            val updated = expense.copy(receiptUri = newReceiptUri)
            repository.updateExpense(updated)
            showToast("✓ Comprobante guardado")
        }
    }

    fun deleteExpenseReceipt(expense: ExpenseEntity, targetUri: String? = null) {
        viewModelScope.launch {
            val currentUri = expense.receiptUri ?: return@launch
            val uris = expense.getReceiptUriList()
            val toDelete = targetUri ?: currentUri
            ReceiptStorageUtil.deleteReceiptFile(toDelete)

            val remainingUris = if (targetUri != null) {
                uris.filter { it != targetUri }
            } else {
                emptyList()
            }
            val newReceiptUri = if (remainingUris.isEmpty()) null else remainingUris.joinToString("|")
            val updated = expense.copy(receiptUri = newReceiptUri)
            repository.updateExpense(updated)
            showToast("✓ Foto del comprobante eliminada del almacenamiento interno")
        }
    }

    fun openLogoSearchModal(expense: ExpenseEntity, initialQuery: String? = null) {
        val query = initialQuery ?: expense.establishment
        _uiState.update {
            it.copy(
                isLogoSearchModalOpen = true,
                logoTargetExpense = expense,
                logoSearchQuery = query,
                logoSearchResults = emptyList(),
                logoSearchError = null,
                isSearchingLogos = false
            )
        }
        searchLogos(query)
    }

    fun closeLogoSearchModal() {
        _uiState.update {
            it.copy(
                isLogoSearchModalOpen = false,
                logoTargetExpense = null,
                logoSearchResults = emptyList(),
                logoSearchError = null,
                isSearchingLogos = false,
                isApplyingLogo = false
            )
        }
    }

    fun onLogoSearchQueryChange(query: String) {
        _uiState.update { it.copy(logoSearchQuery = query) }
    }

    fun searchLogos(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    isSearchingLogos = false,
                    logoSearchResults = emptyList(),
                    logoSearchError = "Ingresa un nombre de comercio o término de búsqueda para ver imágenes."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearchingLogos = true,
                    logoSearchError = null,
                    logoSearchQuery = cleanQuery,
                    logoSearchResults = emptyList()
                )
            }
            try {
                val candidates = GeminiService.searchLogoCandidates(
                    apiKey = _uiState.value.geminiApiKey,
                    modelName = _uiState.value.geminiModel,
                    query = cleanQuery
                )
                _uiState.update {
                    it.copy(
                        isSearchingLogos = false,
                        logoSearchResults = candidates,
                        logoSearchError = if (candidates.isEmpty()) "No se encontraron imágenes en internet para \"$cleanQuery\". Intenta con otro término o selecciona una imagen desde tu galería." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearchingLogos = false,
                        logoSearchError = "Error buscando imágenes: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    fun selectAndApplyLogo(candidate: com.example.util.LogoCandidate, applyToAllMatching: Boolean = true) {
        val target = _uiState.value.logoTargetExpense ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingLogo = true) }
            val res = GeminiService.downloadAndSaveLogo(
                context = getApplication(),
                logoUrlOrUri = candidate.url,
                establishmentName = target.establishment,
                fallbackUrl = candidate.thumbnailUrl
            )
            res.onSuccess { savedPath ->
                rememberEstablishmentLogo(target.establishment, savedPath)

                if (target.id > 0L) {
                    if (applyToAllMatching) {
                        repository.updateLogoForEstablishment(target.establishment, savedPath)
                        val matching = allExpenses.value.filter {
                            it.establishment.equals(target.establishment, ignoreCase = true)
                        }
                        matching.mapNotNull { it.deferredGroupId }.distinct().forEach { gid ->
                            repository.syncLogosForDeferredGroup(gid)
                        }
                        if (!target.deferredGroupId.isNullOrBlank()) {
                            repository.syncLogosForDeferredGroup(target.deferredGroupId)
                        }
                        val count = (matching.size).coerceAtLeast(1)
                        showToast("✓ Logo de ${target.establishment} asignado y recordado ($count consumos)")
                    } else {
                        repository.updateExpense(target.copy(establishmentLogoUrl = savedPath))
                        if (!target.deferredGroupId.isNullOrBlank()) {
                            repository.syncLogosForDeferredGroup(target.deferredGroupId)
                        }
                        showToast("✓ Logo de ${target.establishment} asignado y recordado")
                    }
                } else {
                    showToast("✓ Logo de ${target.establishment} asignado")
                }
                _uiState.update {
                    it.copy(
                        isApplyingLogo = false,
                        isLogoSearchModalOpen = false,
                        logoTargetExpense = null,
                        lastAppliedLogoPath = savedPath
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isApplyingLogo = false) }
                showToast("Error al procesar el logo: ${err.localizedMessage ?: err.message}")
            }
        }
    }

    fun applyLogoFromUrlOrUri(urlOrUri: String, applyToAllMatching: Boolean = true) {
        val target = _uiState.value.logoTargetExpense ?: return
        val clean = urlOrUri.trim()
        if (clean.isBlank()) {
            showToast("Por favor ingresa una URL o selecciona una imagen válida")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingLogo = true) }
            val res = GeminiService.downloadAndSaveLogo(
                context = getApplication(),
                logoUrlOrUri = clean,
                establishmentName = target.establishment
            )
            res.onSuccess { savedPath ->
                rememberEstablishmentLogo(target.establishment, savedPath)

                if (target.id > 0L) {
                    if (applyToAllMatching) {
                        repository.updateLogoForEstablishment(target.establishment, savedPath)
                        val matching = allExpenses.value.filter {
                            it.establishment.equals(target.establishment, ignoreCase = true)
                        }
                        matching.mapNotNull { it.deferredGroupId }.distinct().forEach { gid ->
                            repository.syncLogosForDeferredGroup(gid)
                        }
                        if (!target.deferredGroupId.isNullOrBlank()) {
                            repository.syncLogosForDeferredGroup(target.deferredGroupId)
                        }
                        val count = (matching.size).coerceAtLeast(1)
                        showToast("✓ Logo de ${target.establishment} asignado y recordado ($count consumos)")
                    } else {
                        repository.updateExpense(target.copy(establishmentLogoUrl = savedPath))
                        if (!target.deferredGroupId.isNullOrBlank()) {
                            repository.syncLogosForDeferredGroup(target.deferredGroupId)
                        }
                        showToast("✓ Logo de ${target.establishment} asignado y recordado")
                    }
                } else {
                    showToast("✓ Logo de ${target.establishment} asignado")
                }
                _uiState.update {
                    it.copy(
                        isApplyingLogo = false,
                        isLogoSearchModalOpen = false,
                        logoTargetExpense = null,
                        lastAppliedLogoPath = savedPath
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isApplyingLogo = false) }
                showToast("Error al guardar logo: ${err.localizedMessage ?: err.message}")
            }
        }
    }

    fun removeEstablishmentLogo(expense: ExpenseEntity, removeFromAllMatching: Boolean = true) {
        viewModelScope.launch {
            forgetEstablishmentLogo(expense.establishment)
            if (expense.id > 0L) {
                if (removeFromAllMatching) {
                    repository.updateLogoForEstablishment(expense.establishment, null)
                    val matching = allExpenses.value.filter {
                        it.establishment.equals(expense.establishment, ignoreCase = true)
                    }
                    matching.mapNotNull { it.deferredGroupId }.distinct().forEach { gid ->
                        repository.syncLogosForDeferredGroup(gid)
                    }
                    if (!expense.deferredGroupId.isNullOrBlank()) {
                        repository.syncLogosForDeferredGroup(expense.deferredGroupId)
                    }
                    showToast("Logo removido de ${expense.establishment}")
                } else {
                    repository.updateExpense(expense.copy(establishmentLogoUrl = null))
                    if (!expense.deferredGroupId.isNullOrBlank()) {
                        repository.syncLogosForDeferredGroup(expense.deferredGroupId)
                    }
                    showToast("Logo removido de ${expense.establishment}")
                }
            } else {
                showToast("Logo removido de ${expense.establishment}")
            }
            _uiState.update {
                it.copy(
                    isLogoSearchModalOpen = false,
                    logoTargetExpense = null,
                    lastAppliedLogoPath = ""
                )
            }
        }
    }

    fun clearLastAppliedLogoPath() {
        _uiState.update { it.copy(lastAppliedLogoPath = null) }
    }

    fun getBillingCycleRange(cardId: Long?, periodName: String, cutDay: Int): String {
        if (cardId != null && periodName.isNotBlank() && periodName != "Todos los históricos") {
            val key = "${cardId}_${periodName}"
            val custom = _uiState.value.customCycleRanges[key]
            if (!custom.isNullOrBlank()) {
                return custom
            }
        }
        return repository.getBillingCycleRange(periodName, cutDay)
    }

    fun getCustomBillingCycleRange(cardId: Long?, periodName: String): String? {
        if (cardId == null || periodName.isBlank() || periodName == "Todos los históricos") return null
        return _uiState.value.customCycleRanges["${cardId}_${periodName}"]
    }

    fun setCustomBillingCycleRange(cardId: Long, periodName: String, customRange: String?) {
        val key = "${cardId}_${periodName}"
        val prefKey = "custom_cycle_range_$key"
        if (customRange.isNullOrBlank()) {
            prefs.edit().remove(prefKey).apply()
            _uiState.update { current ->
                current.copy(customCycleRanges = current.customCycleRanges - key)
            }
            showToast("Rango de facturación restablecido a cálculo automático")
        } else {
            prefs.edit().putString(prefKey, customRange.trim()).apply()
            _uiState.update { current ->
                current.copy(customCycleRanges = current.customCycleRanges + (key to customRange.trim()))
            }
            showToast("✓ Rango de facturación sobrescrito para $periodName")
        }
    }

    fun getCustomCutDate(cardId: Long?, periodName: String): String? {
        if (cardId == null || periodName.isBlank() || periodName == "Todos los históricos") return null
        return _uiState.value.customCutDates["${cardId}_${periodName}"]
    }

    fun setCustomCutDate(cardId: Long, periodName: String, customCutDate: String?) {
        val key = "${cardId}_${periodName}"
        val prefKey = "custom_cut_date_$key"
        if (customCutDate.isNullOrBlank()) {
            prefs.edit().remove(prefKey).apply()
            _uiState.update { current ->
                current.copy(customCutDates = current.customCutDates - key)
            }
            showToast("Fecha de corte restablecida a cálculo automático")
        } else {
            prefs.edit().putString(prefKey, customCutDate.trim()).apply()
            _uiState.update { current ->
                current.copy(customCutDates = current.customCutDates + (key to customCutDate.trim()))
            }
            showToast("✓ Fecha de corte sobrescrita para $periodName")
        }
    }

    fun getCustomPaymentLimitDate(cardId: Long?, periodName: String): String? {
        if (cardId == null || periodName.isBlank() || periodName == "Todos los históricos") return null
        return _uiState.value.customPaymentLimits["${cardId}_${periodName}"]
    }

    fun setCustomPaymentLimitDate(cardId: Long, periodName: String, customPaymentDate: String?) {
        val key = "${cardId}_${periodName}"
        val prefKey = "custom_payment_limit_$key"
        if (customPaymentDate.isNullOrBlank()) {
            prefs.edit().remove(prefKey).apply()
            _uiState.update { current ->
                current.copy(customPaymentLimits = current.customPaymentLimits - key)
            }
            showToast("Fecha máxima de pago restablecida a cálculo automático")
        } else {
            prefs.edit().putString(prefKey, customPaymentDate.trim()).apply()
            _uiState.update { current ->
                current.copy(customPaymentLimits = current.customPaymentLimits + (key to customPaymentDate.trim()))
            }
            showToast("✓ Fecha máxima de pago sobrescrita para $periodName")
        }
    }

    fun searchAndApplyEstablishmentLogo(expense: ExpenseEntity) {
        openLogoSearchModal(expense)
    }



    fun confirmClearDb() {
        val app = getApplication<Application>()
        cancelPaymentAlarm()
        viewModelScope.launch {
            ReceiptStorageUtil.deleteAllReceiptFiles(app)
            repository.clearAllData()
            _uiState.update {
                it.copy(
                    isClearDbOpen = false,
                    selectedCard = null,
                    cards = emptyList(),
                    editingExpense = null,
                    expenseToDelete = null,
                    cardToDelete = null,
                    searchQuery = "",
                    isSearchActive = false,
                    selectedExactDate = null,
                    selectedPeriod = "",
                    isSelectionMode = false,
                    selectedExpenseIds = emptySet()
                )
            }
            showToast("✓ Base de datos purgada completamente (tarjetas, consumos, diferidos y fotos)")
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

    /**
     * Exporta el respaldo completo (tarjetas, consumos con todos sus datos y fotos de comprobantes)
     * en un archivo ZIP al OutputStream proporcionado.
     */
    fun exportFullBackup(outputStream: java.io.OutputStream) {
        val cards = _uiState.value.cards
        val expenses = allExpenses.value
        val app = getApplication<Application>()
        com.example.util.BackupUtil.exportFullBackup(app, cards, expenses, outputStream)
    }

    /**
     * Restaura un respaldo completo desde el InputStream (acepta archivo ZIP con fotos o JSON legacy).
     */
    suspend fun restoreFullBackup(inputStream: java.io.InputStream): Triple<Int, Int, Int> {
        val app = getApplication<Application>()

        cancelPaymentAlarm()
        ReceiptStorageUtil.deleteAllReceiptFiles(app)
        repository.clearAllData()

        val parsed = com.example.util.BackupUtil.parseBackup(app, inputStream)

        // Restaurar configuración y preferencias de la aplicación si están presentes
        if (parsed.preferences.isNotEmpty()) {
            val editor = prefs.edit()
            for ((key, value) in parsed.preferences) {
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> {
                        if (key == "notification_lead_days") {
                            editor.putInt(key, value.toInt())
                        } else {
                            editor.putLong(key, value.toLong())
                        }
                    }
                    is String -> editor.putString(key, value)
                    else -> if (value != null) editor.putString(key, value.toString())
                }
            }
            editor.apply()
        }

        val idMapping = mutableMapOf<Long, Long>()
        var restoredCards = 0
        var restoredExpenses = 0

        for (card in parsed.cards) {
            val oldId = card.id
            val newCard = card.copy(id = 0L)
            val newId = repository.insertCard(newCard)
            idMapping[oldId] = newId
            restoredCards++
        }

        for (expense in parsed.expenses) {
            val oldCardId = expense.cardId
            val targetCardId = idMapping[oldCardId] ?: idMapping.values.firstOrNull() ?: 1L
            val newExpense = expense.copy(
                id = 0L,
                cardId = targetCardId
            )
            repository.insertExpense(newExpense)
            restoredExpenses++
        }

        val allCardsList = repository.getAllCardsList()
        val targetCard = allCardsList.firstOrNull { it.isDefault } ?: allCardsList.firstOrNull()
        val defaultPeriod = targetCard?.let { getCurrentActivePeriod(it) } ?: ""

        val savedPin = prefs.getString("master_pin", "") ?: ""
        val isFirst = prefs.getBoolean("is_first_launch", true) || savedPin.isEmpty()
        val biometricsEnabled = prefs.getBoolean("biometrics_enabled", true)
        val notifEnabled = prefs.getBoolean("payment_notifications_enabled", false)
        val leadDays = prefs.getInt("notification_lead_days", 3)
        val aiEnabled = prefs.getBoolean("gemini_ai_enabled", true)
        val savedGeminiKey = prefs.getString("gemini_api_key", "") ?: ""
        val savedGeminiModel = prefs.getString("gemini_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"

        if (notifEnabled) {
            ensureNotificationChannel()
            schedulePaymentAlarm()
        } else {
            cancelPaymentAlarm()
        }

        _uiState.update { current ->
            current.copy(
                cards = allCardsList,
                selectedCard = targetCard,
                selectedPeriod = defaultPeriod,
                currentTab = "consumos",
                isSearchActive = false,
                searchQuery = "",
                selectedExpenseIds = emptySet(),
                isSelectionMode = false,
                isVaultUnlocked = true,
                masterPin = savedPin,
                isFirstLaunch = isFirst,
                isBiometricsActive = biometricsEnabled,
                isPaymentNotificationEnabled = notifEnabled,
                notificationLeadDays = leadDays,
                isGeminiAiEnabled = aiEnabled,
                geminiApiKey = savedGeminiKey,
                geminiModel = savedGeminiModel
            )
        }
        val photoMsg = if (parsed.photosCount > 0) " y ${parsed.photosCount} fotos" else ""
        val configMsg = if (parsed.preferences.isNotEmpty()) ", configuración" else ""
        showToast("✓ Respaldo restaurado: $restoredCards tarjetas, $restoredExpenses consumos$configMsg$photoMsg")
        return Triple(restoredCards, restoredExpenses, parsed.photosCount)
    }

    fun exportDatabaseJson(): String {
        val cards = _uiState.value.cards
        val expenses = allExpenses.value
        val json = JSONObject()
        json.put("app", "Creditia")
        json.put("version", 2)
        json.put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        json.put("cards_count", cards.size)
        json.put("expenses_count", expenses.size)

        // Exportar configuración completa de la aplicación (SharedPreferences)
        val prefsObj = JSONObject()
        for ((key, value) in prefs.all) {
            when (value) {
                is Boolean -> prefsObj.put(key, value)
                is Int -> prefsObj.put(key, value)
                is Long -> prefsObj.put(key, value)
                is Float -> prefsObj.put(key, value.toDouble())
                is Double -> prefsObj.put(key, value)
                is String -> prefsObj.put(key, value)
                else -> if (value != null) prefsObj.put(key, value.toString())
            }
        }
        json.put("preferences", prefsObj)

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
            obj.put("payBusinessDays", c.payBusinessDays)
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
            obj.put("deferredGroupId", e.deferredGroupId ?: "")
            obj.put("transactionType", e.transactionType)
            obj.put("transactionGroupId", e.transactionGroupId ?: "")
            obj.put("transactionGroupName", e.transactionGroupName ?: "")
            obj.put("establishmentLogoUrl", e.establishmentLogoUrl ?: "")
            obj.put("receiptUri", e.receiptUri ?: "")
            obj.put("timestamp", e.timestamp)
            expensesArray.put(obj)
        }
        json.put("expenses", expensesArray)
        return json.toString(2)
    }

    suspend fun restoreDatabaseFromJson(jsonContent: String): Pair<Int, Int> {
        val triple = restoreFullBackup(jsonContent.byteInputStream(Charsets.UTF_8))
        return Pair(triple.first, triple.second)
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
        val all = allExpenses.value
        if (expense.isLockedByActiveDeferred(all)) {
            showToast(
                if (expense.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden modificar"
                else "Registro bloqueado: no se puede modificar porque tiene cuotas diferidas activas"
            )
            return
        }
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
        dateString: String,
        receiptUri: String? = null,
        transactionType: String = ExpenseEntity.TYPE_EXPENSE,
        establishmentLogoUrl: String? = null
    ) {
        viewModelScope.launch {
            val card = _uiState.value.cards.find { it.id == cardId } ?: _uiState.value.selectedCard
            val cutDay = card?.cutDay ?: 24
            val billingPeriod = repository.calculateBillingPeriod(dateString, cutDay)

            val defaultEst = if (transactionType == ExpenseEntity.TYPE_PAYMENT) "Pago a Tarjeta" else "Consumo General"
            val defaultDesc = if (transactionType == ExpenseEntity.TYPE_PAYMENT) "Pago a la tarjeta" else "Sin descripción"
            val effectiveEstablishment = establishment.ifBlank { defaultEst }

            // Auto-asignar logo recordado si no se especificó uno explícito
            val effectiveLogo = establishmentLogoUrl ?: getRememberedLogo(effectiveEstablishment)
            if (!effectiveLogo.isNullOrBlank()) {
                rememberEstablishmentLogo(effectiveEstablishment, effectiveLogo)
            }

            if (id != null && id > 0) {
                val existing = _uiState.value.editingExpense ?: repository.getExpenseById(id)
                val all = allExpenses.value
                if (existing != null && existing.isLockedByActiveDeferred(all)) {
                    showToast(
                        if (existing.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden modificar"
                        else "Registro bloqueado: no se puede modificar porque tiene cuotas diferidas activas"
                    )
                    _uiState.update { it.copy(editingExpense = null) }
                    return@launch
                }
                if (existing?.receiptUri != null && existing.receiptUri != receiptUri) {
                    ReceiptStorageUtil.deleteReceiptFile(existing.receiptUri)
                }
                val seq = existing?.sequenceNumber ?: repository.getNextSequenceNumber()
                val finalType = when {
                    existing?.isCreditNote() == true -> ExpenseEntity.TYPE_CREDIT_NOTE
                    existing?.isPayment() == true -> ExpenseEntity.TYPE_PAYMENT
                    else -> transactionType
                }
                val finalLogo = establishmentLogoUrl ?: existing?.establishmentLogoUrl ?: effectiveLogo
                val updated = ExpenseEntity(
                    id = id,
                    sequenceNumber = seq,
                    cardId = cardId,
                    establishment = effectiveEstablishment,
                    description = description.ifBlank { defaultDesc },
                    amount = amount,
                    dateString = dateString,
                    billingPeriod = billingPeriod,
                    receiptUri = receiptUri ?: existing?.receiptUri,
                    deferredGroupId = existing?.deferredGroupId,
                    establishmentLogoUrl = finalLogo,
                    transactionType = finalType,
                    transactionGroupId = existing?.transactionGroupId,
                    transactionGroupName = existing?.transactionGroupName,
                    timestamp = existing?.timestamp ?: System.currentTimeMillis()
                )
                val countUpdated = repository.updateExpenseWithDeferredSync(updated, cutDay)
                if (countUpdated > 1) {
                    showToast("✓ Se reorganizaron las $countUpdated cuotas del diferido (Nota de crédito conservada)")
                } else {
                    val label = when {
                        finalType == ExpenseEntity.TYPE_PAYMENT -> "Pago"
                        finalType == ExpenseEntity.TYPE_CREDIT_NOTE -> "Nota de Crédito"
                        else -> "Consumo #$seq"
                    }
                    showToast("$label actualizado")
                }
            } else {
                val nextSeq = repository.getNextSequenceNumber()
                val newExpense = ExpenseEntity(
                    sequenceNumber = nextSeq,
                    cardId = cardId,
                    establishment = effectiveEstablishment,
                    description = description.ifBlank { defaultDesc },
                    amount = amount,
                    dateString = dateString,
                    billingPeriod = billingPeriod,
                    receiptUri = receiptUri,
                    establishmentLogoUrl = effectiveLogo,
                    transactionType = transactionType,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertExpense(newExpense)
                if (transactionType == ExpenseEntity.TYPE_PAYMENT) {
                    showToast("¡Pago a la tarjeta registrado!")
                } else {
                    showToast("¡Consumo N° $nextSeq Registrado!")
                }
            }
            _uiState.update { it.copy(editingExpense = null) }
        }
    }

    fun groupSelectedExpenses(groupName: String? = null) {
        val selectedIds = _uiState.value.selectedExpenseIds.toList()
        if (selectedIds.size < 2) {
            showToast("Selecciona al menos 2 consumos para agrupar")
            return
        }
        val all = allExpenses.value
        if (selectedIds.any { id -> all.find { it.id == id }?.isLockedByActiveDeferred(all) == true }) {
            showToast("No se pueden agrupar registros bloqueados")
            return
        }
        viewModelScope.launch {
            val name = if (!groupName.isNullOrBlank()) groupName.trim() else "Transacción Agrupada"
            repository.groupExpenses(selectedIds, name)
            clearExpenseSelection()
            showToast("✓ Se agruparon ${selectedIds.size} consumos en una sola transacción")
        }
    }

    fun ungroupTransaction(groupId: String) {
        viewModelScope.launch {
            repository.ungroupExpenses(groupId)
            showToast("✓ Consumos desagrupados de la transacción")
        }
    }

    fun deferExpense(
        originalExpense: ExpenseEntity,
        months: Int,
        monthlyInterestRate: Double,
        startDateString: String? = null,
        onSuccess: () -> Unit
    ) {
        if (originalExpense.isDeferredInstallment() || originalExpense.isDeferredExpense()) {
            showToast("No se pueden diferir registros de tipo cuota diferida")
            return
        }
        if (originalExpense.isPayment()) {
            showToast("No se pueden diferir registros de tipo pago a tarjeta")
            return
        }
        viewModelScope.launch {
            try {
                val card = _uiState.value.cards.find { it.id == originalExpense.cardId } ?: _uiState.value.selectedCard
                val cutDay = card?.cutDay ?: 24
                repository.deferExpense(
                    originalExpense = originalExpense,
                    months = months,
                    monthlyInterestRate = monthlyInterestRate,
                    cutDay = cutDay,
                    startDateString = startDateString
                )
                showToast("✓ Consumo diferido a $months meses generado con éxito")
                _uiState.update { it.copy(editingExpense = null) }
                onSuccess()
            } catch (e: Exception) {
                showToast("Error al diferir consumo: ${e.localizedMessage}")
            }
        }
    }

    fun deferExpense(
        originalExpense: ExpenseEntity,
        months: Int,
        monthlyInterestRate: Double,
        onSuccess: () -> Unit
    ) = deferExpense(originalExpense, months, monthlyInterestRate, null, onSuccess)

    fun deferMultipleExpenses(
        selectedExpenses: List<ExpenseEntity>,
        months: Int,
        monthlyInterestRate: Double,
        startDateString: String? = null,
        onSuccess: () -> Unit
    ) {
        if (selectedExpenses.any { it.isDeferredInstallment() || it.isDeferredExpense() }) {
            showToast("No se pueden diferir registros de tipo cuota diferida")
            return
        }
        if (selectedExpenses.any { it.isPayment() }) {
            showToast("No se pueden diferir registros de tipo pago a tarjeta")
            return
        }
        viewModelScope.launch {
            try {
                if (selectedExpenses.isEmpty()) return@launch
                val card = _uiState.value.cards.find { it.id == selectedExpenses.first().cardId } ?: _uiState.value.selectedCard
                val cutDay = card?.cutDay ?: 24
                repository.deferMultipleExpenses(
                    selectedExpenses = selectedExpenses,
                    months = months,
                    monthlyInterestRate = monthlyInterestRate,
                    cutDay = cutDay,
                    startDateString = startDateString
                )
                showToast("✓ ${selectedExpenses.size} consumos consolidados en $months cuotas con éxito")
                onSuccess()
            } catch (e: Exception) {
                showToast("Error al diferir consumos: ${e.localizedMessage}")
            }
        }
    }

    fun deferMultipleExpenses(
        selectedExpenses: List<ExpenseEntity>,
        months: Int,
        monthlyInterestRate: Double,
        onSuccess: () -> Unit
    ) = deferMultipleExpenses(selectedExpenses, months, monthlyInterestRate, null, onSuccess)

    /**
     * Registra directamente una compra diferida en N cuotas desde "+ Registrar"
     * SIN generar nota de crédito, creando únicamente las cuotas mensuales en color amarillo.
     */
    fun registerDirectDeferred(
        cardId: Long,
        totalAmount: Double,
        establishment: String,
        description: String,
        startDateString: String,
        months: Int,
        monthlyInterestRate: Double,
        receiptUri: String? = null,
        establishmentLogoUrl: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val card = _uiState.value.cards.find { it.id == cardId } ?: _uiState.value.selectedCard
                val cutDay = card?.cutDay ?: 24
                val effectiveLogo = establishmentLogoUrl ?: getRememberedLogo(establishment)
                if (!effectiveLogo.isNullOrBlank()) {
                    rememberEstablishmentLogo(establishment, effectiveLogo)
                }
                repository.createDirectDeferred(
                    cardId = cardId,
                    totalAmount = totalAmount,
                    establishment = establishment,
                    description = description,
                    startDateString = startDateString,
                    months = months,
                    monthlyInterestRate = monthlyInterestRate,
                    receiptUri = receiptUri,
                    cutDay = cutDay,
                    establishmentLogoUrl = effectiveLogo
                )
                showToast("✓ Se registraron directamente las $months cuotas diferidas")
                _uiState.update { it.copy(editingExpense = null) }
                onSuccess?.invoke()
            } catch (e: Exception) {
                showToast("Error al registrar diferido directo: ${e.localizedMessage}")
            }
        }
    }

    // Helper functions for UI
    fun getFilteredExpenses(
        expenses: List<ExpenseEntity>,
        selectedCard: CreditCardEntity?,
        selectedPeriod: String,
        selectedExactDate: String?,
        searchQuery: String,
        sortOrder: SortOrder,
        filterOnlyDeferred: Boolean = false
    ): List<ExpenseEntity> {
        val cardId = selectedCard?.id ?: return emptyList()
        var list = expenses.filter { it.cardId == cardId }

        if (!selectedExactDate.isNullOrBlank()) {
            list = list.filter { it.dateString == selectedExactDate }
        } else if (selectedPeriod != "Todos los periodos" && selectedPeriod != "Todos los históricos") {
            list = list.filter { it.billingPeriod.equals(selectedPeriod, ignoreCase = true) }
        }

        if (filterOnlyDeferred) {
            list = list.filter { it.isDeferredInstallment() && !it.isOriginalDeferredExpense() }
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

    fun getPeriodFinancialSummary(
        expenses: List<ExpenseEntity>,
        cardId: Long,
        period: String,
        exactDate: String? = null,
        filterOnlyDeferred: Boolean = false
    ): PeriodFinancialSummary {
        val target = if (!exactDate.isNullOrBlank()) {
            expenses.filter { it.cardId == cardId && it.dateString == exactDate && (!filterOnlyDeferred || (it.isDeferredInstallment() && !it.isOriginalDeferredExpense())) }
        } else if (period == "Todos los periodos" || period == "Todos los históricos") {
            expenses.filter { it.cardId == cardId && (!filterOnlyDeferred || (it.isDeferredInstallment() && !it.isOriginalDeferredExpense())) }
        } else {
            expenses.filter { it.cardId == cardId && it.billingPeriod.equals(period, ignoreCase = true) && (!filterOnlyDeferred || (it.isDeferredInstallment() && !it.isOriginalDeferredExpense())) }
        }

        val totalExpenses = target.filter { it.isExpense() }.sumOf { it.amount }
        val totalPayments = target.filter { it.isPayment() }.sumOf { it.amount }
        val totalCreditNotes = target.filter { it.isCreditNote() }.sumOf { it.amount }
        val netExpenses = (totalExpenses - totalCreditNotes).coerceAtLeast(0.0)
        // Requisito 2: El pago final de cada periodo debe calcularse consumos-pagos a la tarjeta
        val finalPayment = (netExpenses - totalPayments).coerceAtLeast(0.0)

        return PeriodFinancialSummary(
            totalExpenses = totalExpenses,
            totalPayments = totalPayments,
            totalCreditNotes = totalCreditNotes,
            netExpenses = netExpenses,
            finalPaymentAmount = finalPayment
        )
    }

    fun calculatePeriodSum(
        expenses: List<ExpenseEntity>,
        cardId: Long,
        period: String,
        exactDate: String? = null,
        filterOnlyDeferred: Boolean = false
    ): Double {
        return getPeriodFinancialSummary(expenses, cardId, period, exactDate, filterOnlyDeferred).finalPaymentAmount
    }

    fun calculateHistoricalTotal(expenses: List<ExpenseEntity>, cardId: Long): Double {
        val list = expenses.filter { it.cardId == cardId }
        val totalExp = list.filter { it.isExpense() }.sumOf { it.amount }
        val totalCredit = list.filter { it.isCreditNote() }.sumOf { it.amount }
        val totalPay = list.filter { it.isPayment() }.sumOf { it.amount }
        return ((totalExp - totalCredit) - totalPay).coerceAtLeast(0.0)
    }

    fun getDaysUntilCut(cutDay: Int): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentYear = today.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH)

        // Fecha de corte de este mes determinada por la configuración de la tarjeta
        val thisMonthCut = CreditiaRepository.getCutCalendar(currentYear, currentMonth, cutDay).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetCut = if (!today.after(thisMonthCut)) {
            thisMonthCut
        } else {
            val nextMonthCal = Calendar.getInstance().apply {
                timeInMillis = today.timeInMillis
                add(Calendar.MONTH, 1)
            }
            CreditiaRepository.getCutCalendar(nextMonthCal.get(Calendar.YEAR), nextMonthCal.get(Calendar.MONTH), cutDay).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val diffMs = targetCut.timeInMillis - today.timeInMillis
        return (diffMs / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }

    /**
     * Retorna el Calendar de corte para el periodo especificado (o periodo activo).
     * Regla 1:
     * El día de corte de la tarjeta está determinado en la configuración (cutDay).
     * Este cálculo se aplica para todos los periodos.
     */
    fun getCutCalendarForPeriod(card: CreditCardEntity?, periodName: String): Calendar {
        val cutDay = card?.cutDay ?: 24
        val effectivePeriod = if (periodName.isNotBlank() && periodName != "Todos los históricos") {
            periodName
        } else {
            getCurrentActivePeriod(card)
        }
        val (year, month) = CreditiaRepository.parsePeriodYearMonth(effectivePeriod)
        return CreditiaRepository.getCutCalendar(year, month, cutDay)
    }

    fun getEffectiveCutCalendarForPeriod(card: CreditCardEntity?, periodName: String): Calendar =
        getCutCalendarForPeriod(card, periodName)

    /**
     * Retorna la información de corte formateada para mostrar en el gráfico de la tarjeta:
     * Ejemplo: "Sáb 24/10/2026"
     */
    fun getCutDateForPeriod(card: CreditCardEntity?, periodName: String): String {
        val cutCal = getCutCalendarForPeriod(card, periodName)
        val dayNames = listOf("", "Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        val dow = dayNames.getOrElse(cutCal.get(Calendar.DAY_OF_WEEK)) { "" }
        val day = String.format(Locale.US, "%02d", cutCal.get(Calendar.DAY_OF_MONTH))
        val month = String.format(Locale.US, "%02d", cutCal.get(Calendar.MONTH) + 1)
        val year = cutCal.get(Calendar.YEAR)
        return "$dow $day/$month/$year"
    }

    fun getEffectiveCutDateForPeriod(card: CreditCardEntity?, periodName: String): String {
        if (card != null && periodName.isNotBlank() && periodName != "Todos los históricos") {
            val key = "${card.id}_${periodName}"
            val custom = _uiState.value.customCutDates[key]
            if (!custom.isNullOrBlank()) {
                return custom
            }
        }
        return getCutDateForPeriod(card, periodName)
    }

    fun getCutDateDescription(card: CreditCardEntity?, periodName: String): String {
        val cutCal = getCutCalendarForPeriod(card, periodName)
        val dayNames = listOf("", "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val monthLower = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        val dName = dayNames.getOrElse(cutCal.get(Calendar.DAY_OF_WEEK)) { "" }
        val mName = monthLower.getOrElse(cutCal.get(Calendar.MONTH)) { "" }
        val dNum = cutCal.get(Calendar.DAY_OF_MONTH)
        val yr = cutCal.get(Calendar.YEAR)
        return "$dName, $dNum de $mName de $yr"
    }

    fun getEffectiveCutDateDescription(card: CreditCardEntity?, periodName: String): String =
        getCutDateDescription(card, periodName)

    /**
     * Calcula la fecha máxima de pago para un periodo específico.
     * Regla 2:
     * Para el cálculo de fecha máxima de pago se debe contabilizar el número de días
     * establecido en la configuración de la tarjeta tomando en cuenta desde el día de corte,
     * solo contabilizando días de lunes a viernes (días hábiles).
     *
     * Ejemplo:
     * Periodo Octubre 2026 con fecha de corte 24 de octubre y 12 días configurados:
     * 12 días después del 24 de octubre contando de lunes a viernes:
     * Martes 10 de noviembre de 2026 ("Mar 10/11/2026").
     */
    fun getPaymentLimitDateForPeriod(card: CreditCardEntity?, periodName: String): String {
        if (card == null) return "Mar 10/11/2026"
        val cutCal = getCutCalendarForPeriod(card, periodName)
        val payCal = CreditiaRepository.calculateMaxPaymentCalendar(cutCal, card.payBusinessDays)

        val dayNames = listOf("", "Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
        val dow = dayNames.getOrElse(payCal.get(Calendar.DAY_OF_WEEK)) { "" }
        val day = String.format(Locale.US, "%02d", payCal.get(Calendar.DAY_OF_MONTH))
        val month = String.format(Locale.US, "%02d", payCal.get(Calendar.MONTH) + 1)
        val year = payCal.get(Calendar.YEAR)
        return "$dow $day/$month/$year"
    }

    fun getEffectivePaymentLimitDateForPeriod(card: CreditCardEntity?, periodName: String): String {
        if (card != null && periodName.isNotBlank() && periodName != "Todos los históricos") {
            val key = "${card.id}_${periodName}"
            val custom = _uiState.value.customPaymentLimits[key]
            if (!custom.isNullOrBlank()) {
                return custom
            }
        }
        return getPaymentLimitDateForPeriod(card, periodName)
    }

    fun getPaymentLimitDateDescription(card: CreditCardEntity?, periodName: String): String {
        if (card == null) return "Martes, 10 de noviembre de 2026"
        val cutCal = getCutCalendarForPeriod(card, periodName)
        val payCal = CreditiaRepository.calculateMaxPaymentCalendar(cutCal, card.payBusinessDays)

        val dayNames = listOf("", "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val monthLower = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        val dName = dayNames.getOrElse(payCal.get(Calendar.DAY_OF_WEEK)) { "" }
        val mName = monthLower.getOrElse(payCal.get(Calendar.MONTH)) { "" }
        val dNum = payCal.get(Calendar.DAY_OF_MONTH)
        val yr = payCal.get(Calendar.YEAR)
        return "$dName, $dNum de $mName de $yr"
    }

    fun getPaymentLimitDate(payDay: Int = 0): String {
        val targetCard = _uiState.value.selectedCard ?: _uiState.value.cards.firstOrNull()
        return if (targetCard != null) {
            val currentPeriod = getCurrentActivePeriod(targetCard)
            getEffectivePaymentLimitDateForPeriod(targetCard, currentPeriod)
        } else {
            "Mar 10/11/2026"
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
     * Computa los periodos disponibles ordenados estrictamente por año y mes cronológicamente descendente:
     * periodos futuros (que aún no llegan) hacia arriba, periodo actual en medio, y periodos pasados hacia abajo.
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
                val mIdx = monthNames.indexOfFirst { it.equals(parts[0], ignoreCase = true) }
                val yr = parts[1].toIntOrNull()
                if (mIdx != -1 && yr != null) {
                    return Pair(yr, mIdx)
                }
            }
            return null
        }

        // Obtener los periodos únicos de los registros almacenados
        val expensePeriods = cardExpenses
            .map { it.billingPeriod }
            .filter { it.isNotBlank() && it != "Todos los históricos" && it != "Todos los periodos" }
            .distinct()

        // Incluir siempre el periodo activo actual de la tarjeta
        val allPeriods = if (currentActivePeriod.isNotBlank()) {
            (expensePeriods + currentActivePeriod).distinct()
        } else {
            expensePeriods
        }

        if (allPeriods.isEmpty()) {
            return listOf(currentActivePeriod)
        }

        // Ordenar periodos cronológicamente descendente:
        // Años y meses mayores arriba (futuros que aún no llegan), actual en medio, pasados hacia abajo
        val sortedPeriods = allPeriods
            .mapNotNull { p ->
                parseYearMonth(p)?.let { Triple(it.first, it.second, p) }
            }.sortedWith(compareByDescending<Triple<Int, Int, String>> { it.first }.thenByDescending { it.second })
            .map { it.third }

        return if (sortedPeriods.size > 1) {
            sortedPeriods + listOf("Todos los históricos")
        } else {
            sortedPeriods
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

    fun toggleGeminiAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("gemini_ai_enabled", enabled).apply()
        _uiState.update { it.copy(isGeminiAiEnabled = enabled) }
        showToast(if (enabled) "Inteligencia Artificial Gemini activada" else "Inteligencia Artificial desactivada (Modo Offline activo)")
    }

    fun saveGeminiApiKey(apiKey: String) {
        prefs.edit().putString("gemini_api_key", apiKey.trim()).apply()
        _uiState.update { it.copy(geminiApiKey = apiKey.trim(), geminiTestStatus = null) }
        showToast("Clave de Gemini guardada")
    }

    fun saveGeminiModel(model: String) {
        prefs.edit().putString("gemini_model", model).apply()
        _uiState.update { it.copy(geminiModel = model, geminiTestStatus = null) }
    }

    fun testGeminiConnection() {
        val key = _uiState.value.geminiApiKey
        val model = _uiState.value.geminiModel
        if (key.isBlank()) {
            _uiState.update { it.copy(geminiTestStatus = GeminiTestResult.Error("Ingresa una API Key primero.")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingGemini = true, geminiTestStatus = null) }
            val result = GeminiService.testConnection(key, model)
            _uiState.update { it.copy(isTestingGemini = false, geminiTestStatus = result) }
        }
    }

    fun detectAvailableModels() {
        val key = _uiState.value.geminiApiKey.trim()
        if (key.isBlank()) {
            showToast("Ingresa y guarda tu API Key de Gemini primero")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingModels = true) }
            val result = GeminiService.fetchAvailableModels(key)
            result.onSuccess { models ->
                _uiState.update { it.copy(isDetectingModels = false, availableModelOptions = models) }
                if (models.isNotEmpty()) {
                    showToast("✓ Se detectaron ${models.size} modelos disponibles para tu cuenta")
                    val currentModel = _uiState.value.geminiModel
                    if (!models.contains(currentModel)) {
                        val suggested = models.firstOrNull { it.contains("flash") } ?: models.first()
                        saveGeminiModel(suggested)
                        showToast("Modelo ajustado automáticamente a: $suggested")
                    }
                } else {
                    showToast("No se encontraron modelos con soporte generateContent")
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isDetectingModels = false) }
                showToast("Error al detectar modelos: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun clearGeminiTestStatus() {
        _uiState.update { it.copy(geminiTestStatus = null) }
    }

    fun setAnalyzingReceipt(isAnalyzing: Boolean) {
        _uiState.update { it.copy(isAnalyzingReceipt = isAnalyzing) }
    }
}
