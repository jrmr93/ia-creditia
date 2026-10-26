package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Info
import java.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.ZoomIn
import coil.compose.AsyncImage
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.DiferirConsumoDialog
import com.example.ui.components.LogoSearchDialog
import com.example.ui.components.EditBillingCycleRangeDialog
import com.example.ui.viewmodel.ExpenseFilterType
import androidx.compose.material.icons.filled.AutoAwesome
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MastercardLogo
import com.example.ui.components.VisaLogo
import com.example.ui.components.CardNetworkLogo
import com.example.ui.components.CardMiniGraphic
import com.example.ui.components.isMastercardNetwork
import com.example.ui.components.getCardGradient
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import com.example.ui.components.OfflineStatusFootnote
import com.example.ui.theme.CreditiaError
import com.example.ui.theme.CreditiaErrorContainer
import com.example.ui.theme.CreditiaOnError
import com.example.ui.theme.CreditiaOnErrorContainer
import com.example.ui.theme.CreditiaPrimary
import com.example.ui.theme.CreditiaPrimaryContainer
import com.example.ui.theme.CreditiaSecondary
import com.example.ui.theme.CreditiaSecondaryContainer
import com.example.ui.theme.CreditiaSurfaceContainer
import com.example.ui.theme.CreditiaSurfaceContainerHigh
import com.example.ui.theme.CreditiaSurfaceContainerLow
import com.example.ui.theme.CreditiaSurfaceContainerLowest
import com.example.ui.theme.CreditiaTertiary
import com.example.ui.viewmodel.CreditiaUiState
import com.example.ui.viewmodel.CreditiaViewModel
import com.example.ui.viewmodel.SortOrder
import java.util.Locale

sealed class ExpenseListItem {
    data class Single(val expense: ExpenseEntity) : ExpenseListItem()
    data class Group(
        val groupId: String,
        val groupName: String,
        val expenses: List<ExpenseEntity>,
        val totalAmount: Double,
        val dateString: String
    ) : ExpenseListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumosScreen(
    viewModel: CreditiaViewModel,
    uiState: CreditiaUiState,
    allExpenses: List<ExpenseEntity>,
    onNavigateToNewExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedCard = uiState.selectedCard
    val currentActivePeriod = remember(selectedCard) {
        viewModel.getCurrentActivePeriod(selectedCard)
    }
    val availablePeriods = remember(selectedCard, allExpenses, uiState.selectedPeriod, uiState.customCycleRanges, uiState.customCutDates, uiState.customPaymentLimits) {
        viewModel.getAvailablePeriods(selectedCard, allExpenses)
    }

    val selectedPeriod = if (uiState.selectedPeriod.isNotBlank()) {
        uiState.selectedPeriod
    } else if (currentActivePeriod in availablePeriods) {
        currentActivePeriod
    } else {
        availablePeriods.firstOrNull() ?: currentActivePeriod
    }

    val isCurrentPeriod = selectedPeriod.equals(currentActivePeriod, ignoreCase = true)

    val filteredExpenses = viewModel.getFilteredExpenses(
        expenses = allExpenses,
        selectedCard = selectedCard,
        selectedPeriod = selectedPeriod,
        selectedExactDate = uiState.selectedExactDate,
        searchQuery = uiState.searchQuery,
        sortOrder = uiState.sortOrder,
        filterOnlyDeferred = uiState.filterOnlyDeferred,
        expenseFilterType = uiState.expenseFilterType
    )

    val periodSum = if (selectedCard != null) {
        viewModel.calculatePeriodSum(
            expenses = allExpenses,
            cardId = selectedCard.id,
            period = selectedPeriod,
            exactDate = uiState.selectedExactDate,
            filterOnlyDeferred = uiState.filterOnlyDeferred,
            expenseFilterType = uiState.expenseFilterType
        )
    } else 0.0

    val historicalTotal = if (selectedCard != null) {
        viewModel.calculateHistoricalTotal(allExpenses, selectedCard.id)
    } else 0.0

    val daysUntilCut = selectedCard?.let { viewModel.getDaysUntilCut(it.cutDay) } ?: 4
    val periodCutDate = selectedCard?.let {
        viewModel.getEffectiveCutDateForPeriod(it, selectedPeriod)
    } ?: "Sáb 24/10/2026"
    val paymentLimitDate = selectedCard?.let {
        viewModel.getEffectivePaymentLimitDateForPeriod(it, selectedPeriod)
    } ?: "Mar 10/11/2026"
    val cycleRange = if (!uiState.selectedExactDate.isNullOrBlank()) {
        "Transacciones del ${uiState.selectedExactDate}"
    } else if (selectedPeriod == "Todos los históricos") {
        "Historial completo de consumos"
    } else {
        selectedCard?.let {
            viewModel.getBillingCycleRange(it.id, selectedPeriod, it.cutDay)
        } ?: "Periodo actual"
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterTypeDropdown by remember { mutableStateOf(false) }
    var showCardDropdown by remember { mutableStateOf(false) }
    var showPeriodPickerSheet by remember { mutableStateOf(false) }
    var viewingReceiptUri by remember { mutableStateOf<String?>(null) }
    var viewingReceiptTitle by remember { mutableStateOf("") }
    var viewingReceiptSubtitle by remember { mutableStateOf("") }

    val selectedExpenseIds = uiState.selectedExpenseIds
    val isSelectionMode = uiState.isSelectionMode
    var showMultiDeferDialog by remember { mutableStateOf(false) }
    var showEditCycleRangeDialog by remember { mutableStateOf(false) }
    var targetPeriodToEdit by remember { mutableStateOf<String?>(null) }

    val selectableExpenses = remember(filteredExpenses, allExpenses) {
        filteredExpenses.filter { !it.isLockedByActiveDeferred(allExpenses) }
    }
    val selectableIds = remember(selectableExpenses) {
        selectableExpenses.map { it.id }.toSet()
    }
    val areAllSelectableSelected = selectableIds.isNotEmpty() && selectedExpenseIds.containsAll(selectableIds)

    // Interceptar botón atrás del sistema para cancelar el modo de selección sin salir de la app
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearExpenseSelection()
    }

    val expenseListItems = remember(filteredExpenses) {
        val result = mutableListOf<ExpenseListItem>()
        val seenGroupIds = mutableSetOf<String>()

        for (expense in filteredExpenses) {
            val gId = expense.transactionGroupId
            if (gId.isNullOrBlank()) {
                result.add(ExpenseListItem.Single(expense))
            } else {
                if (gId !in seenGroupIds) {
                    seenGroupIds.add(gId)
                    val groupExpenses = filteredExpenses.filter { it.transactionGroupId == gId }
                    val totalGroup = groupExpenses.filter { it.isExpense() }.sumOf { it.amount } -
                            groupExpenses.filter { it.isCreditNote() }.sumOf { it.amount } -
                            groupExpenses.filter { it.isPayment() }.sumOf { it.amount }
                    result.add(
                        ExpenseListItem.Group(
                            groupId = gId,
                            groupName = expense.transactionGroupName ?: "Transacción Agrupada",
                            expenses = groupExpenses,
                            totalAmount = if (totalGroup != 0.0) totalGroup else groupExpenses.sumOf { it.amount },
                            dateString = groupExpenses.firstOrNull()?.dateString ?: expense.dateString
                        )
                    )
                }
            }
        }
        result
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Search Bar if Active
            if (uiState.isSearchActive) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Buscar comercio o descripción...") },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_text_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (selectedCard == null) {
                // Empty State when no card exists: Prominent "Crear Tarjeta" button
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_empty_no_cards")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CreditiaSurfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCard,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "Sin tarjetas configuradas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "La base de datos está limpia. Crea una tarjeta para comenzar a registrar consumos y controlar tus ciclos de facturación.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.openAddCard() },
                                colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(48.dp)
                                    .testTag("btn_create_card")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AddCard, contentDescription = null)
                                    Text("Crear Tarjeta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // 1. Header de Tarjeta Seleccionada
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TARJETA SELECCIONADA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Panel desplegable gráfico con tarjetas visuales
                if (showCardDropdown) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("panel_graphical_dropdown")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Toca una tarjeta para activarla:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { showCardDropdown = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cerrar",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                uiState.cards.forEach { card ->
                                    val isCurrent = card.id == selectedCard.id
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .testTag("dropdown_item_card_${card.id}")
                                    ) {
                                        CardMiniGraphic(
                                            card = card,
                                            isSelected = isCurrent,
                                            periodName = selectedPeriod,
                                            onClick = {
                                                viewModel.selectCard(card)
                                                showCardDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Tarjeta Activa Visual
                item {
                    val isMaster = isMastercardNetwork(selectedCard.network, selectedCard.cardName)
                    val cardGradient = getCardGradient(selectedCard.cardColorTheme, selectedCard.network, selectedCard.tier, selectedCard.cardName)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showCardDropdown = !showCardDropdown }
                            .testTag("card_active_selector")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(cardGradient)
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = selectedCard.bankName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.85f),
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = selectedCard.cardName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // Graphical Logo: Visa or Mastercard
                                    CardNetworkLogo(
                                        network = selectedCard.network,
                                        size = 22.dp,
                                        cardName = selectedCard.cardName
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 30.dp, height = 20.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
                                                    )
                                                )
                                        )

                                        Text(
                                            text = "•••• ${selectedCard.last4}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isMaster) Color(0xFFFCD34D) else Color.White,
                                            letterSpacing = 2.sp
                                        )
                                    }

                                    // Graphical Dropdown Action Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(Color.Black.copy(alpha = 0.35f))
                                            .clickable { showCardDropdown = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("btn_open_card_dropdown")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Cambiar tarjeta",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Desplegar lista gráfica de tarjetas",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EventRepeat,
                                            contentDescription = null,
                                            tint = if (isMaster) Color(0xFFFCD34D) else Color(0xFF6FFBBE),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Corte: $periodCutDate",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isMaster) Color(0xFFFCD34D) else Color(0xFF6FFBBE)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payment,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Pago: $paymentLimitDate",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                if (selectedPeriod != "Todos los históricos") {
                                    val isCustomRange = selectedCard?.let { viewModel.getCustomBillingCycleRange(it.id, selectedPeriod) != null } ?: false
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCustomRange) Color(0xFFFCD34D).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, if (isCustomRange) Color(0xFFFCD34D) else Color.White.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                targetPeriodToEdit = selectedPeriod
                                                showEditCycleRangeDialog = true
                                            }
                                            .testTag("clickable_cycle_range_card")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Facturación: $cycleRange",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isCustomRange) Color(0xFFFCD34D) else Color.White
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar rango de facturación",
                                                tint = if (isCustomRange) Color(0xFFFCD34D) else Color.White.copy(alpha = 0.85f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Banner permanente de periodo seleccionado
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CreditiaPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CreditiaPrimary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("banner_current_period")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isCurrentPeriod) "Periodo activo:" else "Periodo:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedPeriod,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaPrimary
                                )
                            }

                            if (isCurrentPeriod) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CreditiaPrimary)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "EN CURSO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Filtro de Periodos y Navegación Temporal
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PERIODO DE FACTURACIÓN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CreditiaPrimary)
                                )
                                Text(
                                    text = cycleRange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CreditiaPrimary
                                )
                            }
                        }

                        // Selector interactivo por lista con indicador de periodo actual
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                            border = BorderStroke(1.dp, CreditiaPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showPeriodPickerSheet = true }
                                .testTag("period_selector_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CreditiaPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (uiState.selectedExactDate.isNullOrBlank()) selectedPeriod else "Fecha: ${uiState.selectedExactDate}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (selectedPeriod == currentActivePeriod && uiState.selectedExactDate.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = CreditiaPrimary
                                                ) {
                                                    Text(
                                                        text = "ACTUAL",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (!uiState.selectedExactDate.isNullOrBlank()) {
                                            Text(
                                                text = "Filtro por día específico",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else if (selectedPeriod == "Todos los históricos") {
                                            Text(
                                                text = "Historial completo de consumos",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CreditiaPrimary.copy(alpha = 0.1f),
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Lista",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CreditiaPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Desplegar lista de periodos",
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                                // Filtros secundarios: Fecha exacta y Solo diferidos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón de selección de fecha exacta
                            val isDateActive = !uiState.selectedExactDate.isNullOrBlank()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDateActive) CreditiaPrimary.copy(alpha = 0.12f) else CreditiaSurfaceContainer)
                                    .border(
                                        1.dp,
                                        if (isDateActive) CreditiaPrimary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val chosenDate = String.format(Locale.US, "%04d/%02d/%02d", y, m + 1, d)
                                                viewModel.setExactDateFilter(chosenDate)
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("btn_filter_exact_date"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = "Filtrar por fecha exacta",
                                        tint = if (isDateActive) CreditiaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isDateActive) uiState.selectedExactDate!! else "Por fecha",
                                        fontSize = 12.sp,
                                        fontWeight = if (isDateActive) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isDateActive) CreditiaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    if (isDateActive) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Quitar filtro de fecha",
                                            tint = CreditiaPrimary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.setExactDateFilter(null) }
                                                .testTag("btn_clear_exact_date")
                                        )
                                    }
                                }
                            }

                            // Botón/Selector de Tipo de Movimiento (Todos, Consumos, Diferidos, Pagos)
                            val isFilterActive = uiState.expenseFilterType != ExpenseFilterType.ALL
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFilterActive) CreditiaSecondaryContainer else CreditiaSurfaceContainer)
                                    .border(
                                        1.dp,
                                        if (isFilterActive) CreditiaSecondary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        showFilterTypeDropdown = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("btn_filter_expense_type"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val filterIcon = when (uiState.expenseFilterType) {
                                        ExpenseFilterType.ALL -> Icons.Default.Layers
                                        ExpenseFilterType.EXPENSES -> Icons.Default.CreditCard
                                        ExpenseFilterType.DEFERRED -> Icons.Default.EventRepeat
                                        ExpenseFilterType.PAYMENTS -> Icons.Default.Payment
                                    }
                                    Icon(
                                        imageVector = if (isFilterActive) Icons.Default.CheckCircle else filterIcon,
                                        contentDescription = "Filtrar por tipo",
                                        tint = if (isFilterActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = uiState.expenseFilterType.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isFilterActive) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isFilterActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isFilterActive) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Quitar filtro de tipo",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.setExpenseFilterType(ExpenseFilterType.ALL) }
                                                .testTag("btn_clear_type_filter")
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showFilterTypeDropdown,
                                    onDismissRequest = { showFilterTypeDropdown = false }
                                ) {
                                    ExpenseFilterType.values().forEach { fType ->
                                        val isCurrent = uiState.expenseFilterType == fType
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = fType.displayName,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) CreditiaPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            leadingIcon = {
                                                val icon = when (fType) {
                                                    ExpenseFilterType.ALL -> Icons.Default.Layers
                                                    ExpenseFilterType.EXPENSES -> Icons.Default.CreditCard
                                                    ExpenseFilterType.DEFERRED -> Icons.Default.EventRepeat
                                                    ExpenseFilterType.PAYMENTS -> Icons.Default.Payment
                                                }
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isCurrent) CreditiaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = if (isCurrent) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = CreditiaPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                viewModel.setExpenseFilterType(fType)
                                                showFilterTypeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }                    }
                    }
                }

                // 3. Resumen Financiero M3 con Gradiente Sutil de Confianza
                item {
                    val summaryTitle = if (!uiState.selectedExactDate.isNullOrBlank()) {
                        when (uiState.expenseFilterType) {
                            ExpenseFilterType.DEFERRED -> "TOTAL DIFERIDOS DEL DÍA (${uiState.selectedExactDate})"
                            ExpenseFilterType.EXPENSES -> "TOTAL CONSUMOS DEL DÍA (${uiState.selectedExactDate})"
                            ExpenseFilterType.PAYMENTS -> "TOTAL PAGOS DEL DÍA (${uiState.selectedExactDate})"
                            ExpenseFilterType.ALL -> "TOTAL DE LA FECHA SELECCIONADA (${uiState.selectedExactDate})"
                        }
                    } else if (selectedPeriod == "Todos los históricos") {
                        when (uiState.expenseFilterType) {
                            ExpenseFilterType.DEFERRED -> "TOTAL DIFERIDOS HISTÓRICO COMPLETO"
                            ExpenseFilterType.EXPENSES -> "TOTAL CONSUMOS HISTÓRICO COMPLETO"
                            ExpenseFilterType.PAYMENTS -> "TOTAL PAGOS HISTÓRICO COMPLETO"
                            ExpenseFilterType.ALL -> "TOTAL HISTÓRICO COMPLETO"
                        }
                    } else {
                        when (uiState.expenseFilterType) {
                            ExpenseFilterType.DEFERRED -> "SUMATORIA DIFERIDOS: ${selectedPeriod.uppercase(Locale.ROOT)}"
                            ExpenseFilterType.EXPENSES -> "SUMATORIA CONSUMOS: ${selectedPeriod.uppercase(Locale.ROOT)}"
                            ExpenseFilterType.PAYMENTS -> "SUMATORIA PAGOS: ${selectedPeriod.uppercase(Locale.ROOT)}"
                            ExpenseFilterType.ALL -> "SUMATORIA TOTAL: ${selectedPeriod.uppercase(Locale.ROOT)}"
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CreditiaSurfaceContainerLowest,
                                        CreditiaSurfaceContainerLow,
                                        CreditiaSurfaceContainer
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("card_financial_summary")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = summaryTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "$ %,.2f", periodSum),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = "USD",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }

                                // Corte en X días badge - SOLO si el periodo actual está seleccionado
                                if (isCurrentPeriod && uiState.selectedExactDate.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CreditiaErrorContainer)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = CreditiaOnErrorContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Corte en $daysUntilCut días",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CreditiaOnErrorContainer
                                            )
                                        }
                                    }
                                } else if (!uiState.selectedExactDate.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CreditiaSecondaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "Fecha Filtrada",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                } else if (selectedPeriod == "Todos los históricos") {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CreditiaSecondaryContainer)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "Todos los Registros",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            // Metadatos de Liquidación y Comparativo General
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest.copy(alpha = 0.85f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(CreditiaTertiary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = CreditiaTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Pago límite",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = paymentLimitDate,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(24.dp)
                                            .width(1.dp)
                                            .background(CreditiaSurfaceContainerHigh)
                                    )

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Total Histórico",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$ %,.2f", historicalTotal),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Detalle de Registros & Ordenar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Detalle de Registros",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(CreditiaSurfaceContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${filteredExpenses.size} registros",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaPrimary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (filteredExpenses.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelectionMode && areAllSelectableSelected) CreditiaTertiary.copy(alpha = 0.15f) else CreditiaPrimary.copy(alpha = 0.10f))
                                        .clickable {
                                            if (isSelectionMode && areAllSelectableSelected) {
                                                viewModel.clearExpenseSelection()
                                            } else {
                                                viewModel.selectAllExpenses(filteredExpenses)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("btn_select_all_expenses")
                                ) {
                                    Icon(
                                        imageVector = if (isSelectionMode && areAllSelectableSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                        contentDescription = if (isSelectionMode && areAllSelectableSelected) "Deseleccionar todos" else "Seleccionar todos",
                                        tint = if (isSelectionMode && areAllSelectableSelected) CreditiaTertiary else CreditiaPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = when {
                                            isSelectionMode && areAllSelectableSelected -> "Deseleccionar"
                                            isSelectionMode -> "Todos (${selectableExpenses.size})"
                                            else -> "Seleccionar todos"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelectionMode && areAllSelectableSelected) CreditiaTertiary else CreditiaPrimary
                                    )
                                }

                                if (isSelectionMode) {
                                    IconButton(
                                        onClick = { viewModel.clearExpenseSelection() },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("btn_cancel_selection")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancelar selección",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (showSortMenu) CreditiaPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { showSortMenu = !showSortMenu }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("btn_sort"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Ordenar",
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Barra Horizontal de Criterios de Ordenamiento
                if (showSortMenu) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chips de Tipo de Movimiento: Todos, Consumos, Diferidos, Pagos
                            ExpenseFilterType.values().forEach { fType ->
                                val isSelected = uiState.expenseFilterType == fType
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(if (isSelected) CreditiaSecondary else CreditiaSurfaceContainer)
                                        .border(
                                            1.dp,
                                            if (isSelected) CreditiaSecondary else CreditiaSurfaceContainerHigh,
                                            RoundedCornerShape(100.dp)
                                        )
                                        .clickable {
                                            viewModel.setExpenseFilterType(fType)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("chip_filter_${fType.name.lowercase()}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val icon = when (fType) {
                                            ExpenseFilterType.ALL -> Icons.Default.Layers
                                            ExpenseFilterType.EXPENSES -> Icons.Default.CreditCard
                                            ExpenseFilterType.DEFERRED -> Icons.Default.EventRepeat
                                            ExpenseFilterType.PAYMENTS -> Icons.Default.Payment
                                        }
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = fType.shortLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Separador visual sutil entre filtros de tipo y ordenamientos
                            Box(
                                modifier = Modifier
                                    .height(18.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            )
                            SortOrder.values().forEach { order ->
                                val isSelected = uiState.sortOrder == order
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(if (isSelected) CreditiaPrimary else CreditiaSurfaceContainer)
                                        .border(
                                            1.dp,
                                            if (isSelected) CreditiaPrimary else CreditiaSurfaceContainerHigh,
                                            RoundedCornerShape(100.dp)
                                        )
                                        .clickable {
                                            viewModel.setSortOrder(order)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = order.shortLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Lista de Consumos
                if (filteredExpenses.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                val emptyMessage = when (uiState.expenseFilterType) {
                                    ExpenseFilterType.DEFERRED -> {
                                        if (!uiState.selectedExactDate.isNullOrBlank()) "No hay consumos diferidos en la fecha ${uiState.selectedExactDate}"
                                        else if (selectedPeriod == "Todos los históricos") "No hay consumos diferidos registrados en el historial de esta tarjeta"
                                        else "No hay consumos diferidos en el periodo $selectedPeriod"
                                    }
                                    ExpenseFilterType.EXPENSES -> {
                                        if (!uiState.selectedExactDate.isNullOrBlank()) "No hay consumos corrientes en la fecha ${uiState.selectedExactDate}"
                                        else if (selectedPeriod == "Todos los históricos") "No hay consumos corrientes registrados en el historial de esta tarjeta"
                                        else "No hay consumos corrientes en el periodo $selectedPeriod"
                                    }
                                    ExpenseFilterType.PAYMENTS -> {
                                        if (!uiState.selectedExactDate.isNullOrBlank()) "No hay pagos con tarjeta en la fecha ${uiState.selectedExactDate}"
                                        else if (selectedPeriod == "Todos los históricos") "No hay pagos con tarjeta registrados en el historial de esta tarjeta"
                                        else "No hay pagos con tarjeta registrados en el periodo $selectedPeriod"
                                    }
                                    ExpenseFilterType.ALL -> {
                                        if (!uiState.selectedExactDate.isNullOrBlank()) "No hay movimientos en la fecha ${uiState.selectedExactDate}"
                                        else if (selectedPeriod == "Todos los históricos") "No hay movimientos registrados en el historial de esta tarjeta"
                                        else "No hay consumos ni movimientos en el periodo $selectedPeriod"
                                    }
                                }
                                Text(
                                    text = emptyMessage,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(expenseListItems, key = { item ->
                        when (item) {
                            is ExpenseListItem.Single -> "single_${item.expense.id}"
                            is ExpenseListItem.Group -> "group_${item.groupId}"
                        }
                    }) { item ->
                        when (item) {
                            is ExpenseListItem.Single -> {
                                val expense = item.expense
                                val isSelected = expense.id in selectedExpenseIds
                                val isLocked = expense.isLockedByActiveDeferred(allExpenses)
                                ExpenseCardItem(
                                    expense = expense,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    isLocked = isLocked,
                                    onToggleSelect = {
                                        viewModel.toggleExpenseSelection(expense.id)
                                    },
                                    onLongPress = {
                                        viewModel.toggleExpenseSelection(expense.id)
                                    },
                                    onEdit = {
                                        if (isLocked) {
                                            viewModel.showToast(
                                                if (expense.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden modificar"
                                                else "Registro bloqueado: no se puede editar porque pertenece a un diferido activo"
                                            )
                                        } else {
                                            viewModel.startEditExpense(expense)
                                            onNavigateToNewExpense()
                                        }
                                    },
                                    onDelete = {
                                        if (isLocked) {
                                            viewModel.showToast(
                                                if (expense.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden eliminar"
                                                else "Registro bloqueado: no se puede eliminar porque tiene cuotas diferidas activas"
                                            )
                                        } else {
                                            viewModel.promptDeleteExpense(expense)
                                        }
                                    },
                                    onReceiptClick = { uri, title, subtitle ->
                                        viewingReceiptUri = uri
                                        viewingReceiptTitle = title
                                        viewingReceiptSubtitle = subtitle
                                    }
                                )
                            }
                            is ExpenseListItem.Group -> {
                                GroupedTransactionCardItem(
                                    groupId = item.groupId,
                                    groupName = item.groupName,
                                    expenses = item.expenses,
                                    totalAmount = item.totalAmount,
                                    dateString = item.dateString,
                                    selectedExpenseIds = selectedExpenseIds,
                                    isSelectionMode = isSelectionMode,
                                    allExpenses = allExpenses,
                                    onToggleSelect = { expenseId ->
                                        viewModel.toggleExpenseSelection(expenseId)
                                    },
                                    onEdit = { expense ->
                                        val isChildLocked = expense.isLockedByActiveDeferred(allExpenses)
                                        if (isChildLocked) {
                                            viewModel.showToast(
                                                if (expense.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden modificar"
                                                else "Registro bloqueado: no se puede editar porque pertenece a un diferido activo"
                                            )
                                        } else {
                                            viewModel.startEditExpense(expense)
                                            onNavigateToNewExpense()
                                        }
                                    },
                                    onDelete = { expense ->
                                        val isChildLocked = expense.isLockedByActiveDeferred(allExpenses)
                                        if (isChildLocked) {
                                            viewModel.showToast(
                                                if (expense.isCreditNote()) "Las notas de crédito creadas por un diferido no se pueden eliminar"
                                                else "Registro bloqueado: no se puede eliminar porque tiene cuotas diferidas activas"
                                            )
                                        } else {
                                            viewModel.promptDeleteExpense(expense)
                                        }
                                    },
                                    onReceiptClick = { uri, title, subtitle ->
                                        viewingReceiptUri = uri
                                        viewingReceiptTitle = title
                                        viewingReceiptSubtitle = subtitle
                                    },
                                    onUngroup = {
                                        viewModel.ungroupTransaction(item.groupId)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Offline Footnote
            item {
                OfflineStatusFootnote()
                Spacer(modifier = Modifier.height(if (isSelectionMode) 120.dp else 80.dp))
            }
        }
    }

    // Barra Flotante de Selección Múltiple
    AnimatedVisibility(
        visible = isSelectionMode,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
    ) {
        val selectedExpenses = allExpenses.filter { it.id in selectedExpenseIds }
        val totalSelectedAmount = selectedExpenses.sumOf { it.amount }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CreditiaPrimary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lockedCount = remember(filteredExpenses, allExpenses) {
                        filteredExpenses.count { it.isLockedByActiveDeferred(allExpenses) }
                    }
                    Column {
                        Text(
                            text = if (lockedCount > 0) {
                                "${selectedExpenseIds.size} seleccionados ($lockedCount bloqueados omitidos)"
                            } else {
                                "${selectedExpenseIds.size} consumos seleccionados"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Suma total: S/ ${String.format(Locale.US, "%.2f", totalSelectedAmount)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CreditiaPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                if (areAllSelectableSelected) {
                                    viewModel.clearExpenseSelection()
                                } else {
                                    viewModel.selectAllExpenses(filteredExpenses)
                                }
                            },
                            modifier = Modifier.testTag("btn_toggle_select_all_bar")
                        ) {
                            Icon(
                                imageVector = if (areAllSelectableSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (areAllSelectableSelected) "Deseleccionar" else "Seleccionar todos",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { viewModel.clearExpenseSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar selección",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedExpenseIds.size >= 2) {
                        Button(
                            onClick = {
                                viewModel.groupSelectedExpenses()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Agrupar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val selectedExpenses = allExpenses.filter { it.id in selectedExpenseIds }
                            if (selectedExpenses.any { it.isDeferredInstallment() || it.isDeferredExpense() }) {
                                viewModel.showToast("No se pueden diferir registros de tipo cuota diferida")
                            } else if (selectedExpenses.any { it.isPayment() }) {
                                viewModel.showToast("No se pueden diferir registros de tipo pago a tarjeta")
                            } else {
                                showMultiDeferDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaTertiary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = selectedExpenseIds.isNotEmpty()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (selectedExpenseIds.size > 1) {
                                    "Diferir (${selectedExpenseIds.size})"
                                } else {
                                    "Diferir Consumo"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal para diferir los consumos consolidados
    if (showMultiDeferDialog && selectedExpenseIds.isNotEmpty()) {
        val selectedExpenses = allExpenses.filter { it.id in selectedExpenseIds }
        val totalSelectedAmount = selectedExpenses.sumOf { it.amount }
        val latestDate = selectedExpenses.maxByOrNull { it.dateString }?.dateString ?: "2026/10/01"
        val combinedEstablishment = selectedExpenses.map { it.establishment }
            .distinct()
            .joinToString(" + ")
            .take(45)
        val card = selectedCard ?: uiState.selectedCard
        val cutDay = card?.cutDay ?: 24

        DiferirConsumoDialog(
            amount = totalSelectedAmount,
            establishment = combinedEstablishment,
            dateString = latestDate,
            cutDay = cutDay,
            cardName = card?.cardName ?: "Tarjeta",
            repository = viewModel.repository,
            periodResolver = { dStr -> viewModel.getBillingPeriodForDate(card?.id, dStr, cutDay) },
            onDismiss = { showMultiDeferDialog = false },
            onConfirm = { months, monthlyInterestRate, customStartDate ->
                showMultiDeferDialog = false
                viewModel.deferMultipleExpenses(selectedExpenses, months, monthlyInterestRate, customStartDate) {
                    viewModel.clearExpenseSelection()
                }
            }
        )
    }



    // Modal de confirmación de eliminación
    if (uiState.expenseToDelete != null) {
        val target = uiState.expenseToDelete
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteExpense() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CreditiaErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = CreditiaError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (target.isDeferredExpense()) "¿Eliminar consumo diferido?" else "¿Eliminar consumo?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Deseas eliminar el consumo de ${target.establishment} por S/ ${String.format(Locale.US, "%.2f", target.amount)} de la base de datos local?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (target.isDeferredExpense()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CreditiaError.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CreditiaError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Este consumo forma parte de una compra diferida. Al eliminarlo, se eliminarán automáticamente todas sus cuotas en todos los periodos.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CreditiaError
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteExpense() },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaError),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) {
                    Text(text = "Eliminar", color = CreditiaOnError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeleteExpense() },
                    modifier = Modifier.testTag("btn_cancel_delete")
                ) {
                    Text(
                        text = "Cancelar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    // Modal Lista Desplegable Gráfica de Tarjetas
    if (showCardDropdown) {
        AlertDialog(
            onDismissRequest = { showCardDropdown = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seleccionar Tarjeta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    TextButton(
                        onClick = {
                            showCardDropdown = false
                            viewModel.openAddCard()
                        },
                        modifier = Modifier.testTag("btn_dropdown_add_new_card")
                    ) {
                        Text("+ Nueva", fontWeight = FontWeight.Bold, color = CreditiaPrimary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Toca una tarjeta para ver sus consumos y ciclo contable:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    uiState.cards.forEach { card ->
                        val isCurrent = card.id == selectedCard?.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("dropdown_card_item_${card.id}")
                        ) {
                            CardMiniGraphic(
                                card = card,
                                isSelected = isCurrent,
                                periodName = selectedPeriod,
                                onClick = {
                                    viewModel.selectCard(card)
                                    showCardDropdown = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showCardDropdown = false },
                    modifier = Modifier.testTag("btn_close_card_dropdown")
                ) {
                    Text(
                        text = "Cerrar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    // ModalBottomSheet Lista Seleccionable de Periodos
    if (showPeriodPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPeriodPickerSheet = false },
            containerColor = CreditiaSurfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Seleccionar Periodo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Elige un periodo para consultar los consumos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showPeriodPickerSheet = false },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_close_period_picker")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar selector de periodos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = CreditiaSurfaceContainerHigh)

                // Lista de periodos seleccionables ordenados cronológicamente
                val periodListState = rememberLazyListState()
                val activeOrSelectedIndex = remember(availablePeriods, selectedPeriod, currentActivePeriod) {
                    val target = if (selectedPeriod.isNotBlank() && selectedPeriod != "Todos los históricos") {
                        selectedPeriod
                    } else {
                        currentActivePeriod
                    }
                    val idx = availablePeriods.indexOf(target)
                    if (idx >= 0) idx else 0
                }

                LaunchedEffect(showPeriodPickerSheet) {
                    if (showPeriodPickerSheet && activeOrSelectedIndex >= 0 && availablePeriods.isNotEmpty()) {
                        periodListState.scrollToItem(maxOf(0, activeOrSelectedIndex))
                    }
                }

                LazyColumn(
                    state = periodListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availablePeriods) { period ->
                        val isSelected = (selectedPeriod == period) && uiState.selectedExactDate.isNullOrBlank()
                        val isCurrent = period == currentActivePeriod

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) CreditiaPrimary.copy(alpha = 0.08f) else CreditiaSurfaceContainerLow
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CreditiaPrimary else CreditiaSurfaceContainerHigh
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectPeriod(period)
                                    showPeriodPickerSheet = false
                                }
                                .testTag("period_item_$period")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) CreditiaPrimary else CreditiaSurfaceContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (period == "Todos los históricos") Icons.Default.History else Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = period,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) CreditiaPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isCurrent) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = CreditiaPrimary
                                                ) {
                                                    Text(
                                                        text = "ACTUAL",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (period == "Todos los históricos") {
                                            Text(
                                                text = "Historial consolidado de todas las compras",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.selectPeriod(period)
                                        showPeriodPickerSheet = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = CreditiaPrimary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
    // Visor de Comprobante a Pantalla Completa con Zoom
    if (viewingReceiptUri != null) {
        ReceiptViewerDialog(
            imageUri = viewingReceiptUri!!,
            title = viewingReceiptTitle.ifBlank { "Comprobante de compra" },
            subtitle = viewingReceiptSubtitle,
            onDismiss = { viewingReceiptUri = null }
        )
    }

    // Modal de Búsqueda y Selección de Logotipos en Internet
    if (uiState.isLogoSearchModalOpen && uiState.logoTargetExpense != null) {
        LogoSearchDialog(
            expense = uiState.logoTargetExpense!!,
            searchQuery = uiState.logoSearchQuery,
            isSearching = uiState.isSearchingLogos,
            candidates = uiState.logoSearchResults,
            errorMessage = uiState.logoSearchError,
            isApplying = uiState.isApplyingLogo,
            onQueryChange = { viewModel.onLogoSearchQueryChange(it) },
            onSearch = { viewModel.searchLogos(it) },
            onSelectCandidate = { candidate -> viewModel.selectAndApplyLogo(candidate) },
            onApplyDirectUrlOrUri = { uri -> viewModel.applyLogoFromUrlOrUri(uri) },
            onRemoveLogo = { exp -> viewModel.removeEstablishmentLogo(exp) },
            onDismiss = { viewModel.closeLogoSearchModal() }
        )
    }

    // Diálogo para editar y sobrescribir el rango de fecha de facturación
    if (showEditCycleRangeDialog && targetPeriodToEdit != null && selectedCard != null) {
        val pToEdit = targetPeriodToEdit!!
        val curRange = viewModel.getBillingCycleRange(selectedCard.id, pToEdit, selectedCard.cutDay)
        val calcRange = viewModel.repository.getBillingCycleRange(pToEdit, selectedCard.cutDay)
        val isOver = viewModel.getCustomBillingCycleRange(selectedCard.id, pToEdit) != null

        val curCut = viewModel.getEffectiveCutDateForPeriod(selectedCard, pToEdit)
        val calcCut = viewModel.getCutDateForPeriod(selectedCard, pToEdit)
        val isCutOver = viewModel.getCustomCutDate(selectedCard.id, pToEdit) != null

        val curPayment = viewModel.getEffectivePaymentLimitDateForPeriod(selectedCard, pToEdit)
        val calcPayment = viewModel.getPaymentLimitDateForPeriod(selectedCard, pToEdit)
        val isPayOver = viewModel.getCustomPaymentLimitDate(selectedCard.id, pToEdit) != null

        EditBillingCycleRangeDialog(
            periodName = pToEdit,
            cardName = selectedCard.cardName,
            currentRange = curRange,
            calculatedRange = calcRange,
            isOverridden = isOver,
            currentCutDate = curCut,
            currentPaymentDate = curPayment,
            isCutOverridden = isCutOver,
            isPaymentOverridden = isPayOver,
            onDismiss = {
                showEditCycleRangeDialog = false
                targetPeriodToEdit = null
            },
            onSaveAll = { newRange, newCut, newPay ->
                if (!newRange.isNullOrBlank()) {
                    viewModel.setCustomBillingCycleRange(selectedCard.id, pToEdit, newRange)
                }
                viewModel.setCustomCutDate(selectedCard.id, pToEdit, newCut)
                viewModel.setCustomPaymentLimitDate(selectedCard.id, pToEdit, newPay)
                showEditCycleRangeDialog = false
                targetPeriodToEdit = null
            },
            onResetToCalculated = {
                viewModel.setCustomBillingCycleRange(selectedCard.id, pToEdit, null)
                viewModel.setCustomCutDate(selectedCard.id, pToEdit, null)
                viewModel.setCustomPaymentLimitDate(selectedCard.id, pToEdit, null)
                showEditCycleRangeDialog = false
                targetPeriodToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseCardItem(
    expense: ExpenseEntity,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isLocked: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReceiptClick: ((receiptUri: String, title: String, subtitle: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isPayment = expense.isPayment()
    val isCreditNote = expense.isCreditNote()
    val isDeferred = expense.isDeferredExpense()

    val accentColor = when {
        isPayment -> Color(0xFF10B981) // Verde esmeralda
        isCreditNote -> Color(0xFF0D9488) // Teal / Nota de crédito
        isDeferred -> Color(0xFFF59E0B) // Ámbar / Cuota Diferida
        else -> Color(0xFFEF4444) // Rojo / Consumo regular
    }

    val cardBorderColor = when {
        isSelected -> CreditiaPrimary
        isPayment -> Color(0xFF10B981).copy(alpha = 0.45f)
        isCreditNote -> Color(0xFF0D9488).copy(alpha = 0.45f)
        isDeferred -> Color(0xFFF59E0B).copy(alpha = 0.45f)
        else -> Color(0xFFEF4444).copy(alpha = 0.35f)
    }

    val cardContainerColor = when {
        isSelected -> CreditiaPrimary.copy(alpha = 0.08f)
        isPayment -> Color(0xFF10B981).copy(alpha = 0.04f)
        isCreditNote -> Color(0xFF0D9488).copy(alpha = 0.04f)
        isDeferred -> Color(0xFFF59E0B).copy(alpha = 0.04f)
        else -> CreditiaSurfaceContainerLowest
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onEdit()
                    }
                },
                onLongClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onLongPress?.invoke()
                    }
                }
            )
            .testTag("expense_card_${expense.id}")
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra indicadora lateral izquierda con color del tipo de registro
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 90.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fila superior con Badge de tipo y Periodo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.14f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Text(
                                    text = when {
                                        isPayment -> "PAGO A TARJETA"
                                        isCreditNote -> "NOTA DE CRÉDITO"
                                        isDeferred -> "CUOTA DIFERIDA"
                                        else -> "CONSUMO"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (isLocked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "BLOQUEADO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = expense.billingPeriod,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = CreditiaPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Checkbox / selection indicator when in selection mode
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isLocked) MaterialTheme.colorScheme.surfaceVariant else if (isSelected) CreditiaPrimary else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (isLocked) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else if (isSelected) CreditiaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    // Logotipo(s) del Comercio (soporta diferidos unificados)
                    val logoUrls = remember(expense.establishmentLogoUrl) { expense.getLogoUrls() }
                    if (logoUrls.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-8).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            logoUrls.take(3).forEach { logoUrl ->
                                val logoModel: Any = if (logoUrl.startsWith("/") || logoUrl.startsWith("file://")) {
                                    File(logoUrl.removePrefix("file://"))
                                } else {
                                    logoUrl
                                }
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 2.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    AsyncImage(
                                        model = logoModel,
                                        contentDescription = "Logo de ${expense.establishment}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Miniatura del Comprobante si existe
                    val receiptList = remember(expense.receiptUri) { expense.getReceiptUriList() }
                    if (receiptList.isNotEmpty()) {
                        val firstUri = receiptList.first()
                        val imgModel: Any = if (firstUri.startsWith("/") || firstUri.startsWith("file://")) {
                            File(firstUri.removePrefix("file://"))
                        } else {
                            firstUri
                        }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CreditiaSurfaceContainerHigh)
                                .border(1.dp, CreditiaPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .clickable {
                                    onReceiptClick?.invoke(
                                        expense.receiptUri ?: firstUri,
                                        expense.establishment,
                                        "${expense.dateString} • $ ${String.format(Locale.US, "%.2f", expense.amount)}"
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = imgModel,
                                contentDescription = "Miniatura del comprobante de ${expense.establishment}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Indicador de foto única o múltiple en esquina inferior
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .clip(RoundedCornerShape(topStart = 4.dp))
                                    .background(if (receiptList.size > 1) Color(0xFF0284C7).copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                if (receiptList.size > 1) {
                                    Text(
                                        text = "+${receiptList.size}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Ampliar foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = expense.establishment,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = expense.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = when {
                            isPayment -> String.format(Locale.US, "-$ %,.2f", expense.amount)
                            isCreditNote -> String.format(Locale.US, "-$ %,.2f", expense.amount)
                            else -> String.format(Locale.US, "$ %,.2f", expense.amount)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isPayment -> Color(0xFF059669) // Verde
                            isCreditNote -> Color(0xFF0F766E) // Teal/Verde
                            isDeferred -> Color(0xFFD97706) // Ámbar
                            else -> Color(0xFFDC2626) // Rojo
                        }
                    )
                    if (isPayment) {
                        Text(
                            text = "Abono / Pago",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    } else if (isCreditNote) {
                        Text(
                            text = "Anulación Diferido",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F766E)
                        )
                    } else if (isDeferred) {
                        Text(
                            text = "Diferido",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            // Sub-row with Date and Action Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CreditiaSurfaceContainerLow.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = expense.dateString,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (expense.receiptUri != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CreditiaTertiary.copy(alpha = 0.15f))
                                .clickable {
                                    onReceiptClick?.invoke(
                                        expense.receiptUri,
                                        expense.establishment,
                                        "${expense.dateString} • $ ${String.format(Locale.US, "%.2f", expense.amount)}"
                                    )
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = "Comprobante",
                                    tint = CreditiaTertiary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Ver Ticket",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaTertiary
                                )
                            }
                        }
                    }
                }

                if (!isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("btn_edit_expense_${expense.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("btn_delete_expense_${expense.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = CreditiaError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Requisito 5: Nuevo panel para apreciar que varios consumos pertenecen a una sola transacción.
 */
@Composable
fun GroupedTransactionCardItem(
    groupId: String,
    groupName: String,
    expenses: List<ExpenseEntity>,
    totalAmount: Double,
    dateString: String,
    selectedExpenseIds: Set<Long>,
    isSelectionMode: Boolean,
    allExpenses: List<ExpenseEntity> = emptyList(),
    onToggleSelect: (Long) -> Unit,
    onEdit: (ExpenseEntity) -> Unit,
    onDelete: (ExpenseEntity) -> Unit,
    onReceiptClick: ((receiptUri: String, title: String, subtitle: String) -> Unit)?,
    onUngroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showUngroupConfirmDialog by remember { mutableStateOf(false) }

    if (showUngroupConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUngroupConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.LinkOff,
                    contentDescription = null,
                    tint = CreditiaTertiary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "¿Desagrupar consumos?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas desagrupar los ${expenses.size} consumos de esta transacción?",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUngroupConfirmDialog = false
                        onUngroup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaTertiary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Desagrupar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUngroupConfirmDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
        border = BorderStroke(1.5.dp, Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("grouped_transaction_card_$groupId")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Cabecera del Panel de Transacción Agrupada
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 2.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Transacción Agrupada",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = groupName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "$ %,.2f", totalAmount),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total grupo",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showUngroupConfirmDialog = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = "Desagrupar consumos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Contenido Desplegable con los Consumos Vinculados
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        color = Color.Black.copy(alpha = 0.18f),
                        thickness = 1.dp
                    )

                    expenses.forEach { childExpense ->
                        val isChildSelected = childExpense.id in selectedExpenseIds
                        val isChildLocked = childExpense.isLockedByActiveDeferred(allExpenses)
                        ExpenseCardItem(
                            expense = childExpense,
                            isSelected = isChildSelected,
                            isSelectionMode = isSelectionMode,
                            isLocked = isChildLocked,
                            onToggleSelect = { onToggleSelect(childExpense.id) },
                            onLongPress = { onToggleSelect(childExpense.id) },
                            onEdit = { onEdit(childExpense) },
                            onDelete = { onDelete(childExpense) },
                            onReceiptClick = onReceiptClick
                        )
                    }
                }
            }
        }
    }
}

