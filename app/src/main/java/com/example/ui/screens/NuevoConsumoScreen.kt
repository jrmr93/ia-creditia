package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.ui.components.CardNetworkLogo
import com.example.ui.components.MastercardLogo
import com.example.ui.components.VisaLogo
import com.example.ui.theme.CreditiaErrorContainer
import com.example.ui.theme.CreditiaOnErrorContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.CreditiaError
import com.example.ui.theme.CreditiaPrimary
import com.example.ui.theme.CreditiaPrimaryContainer
import com.example.ui.theme.CreditiaSecondaryContainer
import com.example.ui.theme.CreditiaSurfaceContainer
import com.example.ui.theme.CreditiaSurfaceContainerHigh
import com.example.ui.theme.CreditiaSurfaceContainerLow
import com.example.ui.theme.CreditiaSurfaceContainerLowest
import com.example.ui.theme.CreditiaTertiary
import com.example.ui.viewmodel.CreditiaUiState
import com.example.ui.viewmodel.CreditiaViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Composable
fun NuevoConsumoScreen(
    viewModel: CreditiaViewModel,
    uiState: CreditiaUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val editing = uiState.editingExpense
    val cards = uiState.cards

    var selectedCard by remember {
        mutableStateOf(
            if (editing != null) {
                cards.find { it.id == editing.cardId } ?: uiState.selectedCard ?: cards.firstOrNull()
            } else {
                uiState.selectedCard ?: cards.firstOrNull()
            }
        )
    }

    var amountString by remember {
        mutableStateOf(
            if (editing != null) String.format(Locale.US, "%.2f", editing.amount) else "0.00"
        )
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showConfirmEditDialog by remember { mutableStateOf(false) }

    val calInit = remember(editing) {
        val c = Calendar.getInstance()
        if (editing != null) {
            val parts = editing.dateString.split("/")
            if (parts.size == 3) {
                val y = parts[0].toIntOrNull() ?: c.get(Calendar.YEAR)
                val m = (parts[1].toIntOrNull() ?: (c.get(Calendar.MONTH) + 1)) - 1
                val d = parts[2].toIntOrNull() ?: c.get(Calendar.DAY_OF_MONTH)
                c.set(y, m, d)
            }
        }
        c
    }

    var selectedYear by remember { mutableIntStateOf(calInit.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calInit.get(Calendar.MONTH) + 1) } // 1..12
    var selectedDay by remember { mutableIntStateOf(calInit.get(Calendar.DAY_OF_MONTH)) }

    var establishment by remember {
        mutableStateOf(editing?.establishment ?: "")
    }

    var description by remember {
        mutableStateOf(editing?.description ?: "")
    }

    var showCardDropdown by remember { mutableStateOf(false) }

    val formattedDate = String.format(Locale.US, "%04d/%02d/%02d", selectedYear, selectedMonth, selectedDay)
    val cutDay = selectedCard?.cutDay ?: 24
    val billingPeriod = viewModel.repository.calculateBillingPeriod(formattedDate, cutDay)
    val cycleRange = viewModel.repository.getBillingCycleRange(billingPeriod, cutDay)

    var nextSeqNumber by remember { mutableIntStateOf(6) }
    LaunchedEffect(Unit) {
        if (editing != null) {
            nextSeqNumber = editing.sequenceNumber
        } else {
            nextSeqNumber = viewModel.repository.getNextSequenceNumber()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Sequential Badge & Native Offline Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(CreditiaSecondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (editing != null) "Editando Registro N° $nextSeqNumber" else "Registro Secuencial N° $nextSeqNumber",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = CreditiaTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Offline Nativo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CreditiaTertiary
                        )
                    }
                }
            }

            // Amount Hero Input Card (Android numeric keyboard)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_amount_hero")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VALOR DEL CONSUMO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(CreditiaSurfaceContainerHigh)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Solo números",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = amountString,
                            onValueChange = { input ->
                                val sanitized = input.replace(',', '.')
                                if (sanitized.isEmpty() || sanitized.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    amountString = sanitized
                                }
                            },
                            label = { Text("Monto del consumo ($)") },
                            placeholder = { Text("0.00") },
                            leadingIcon = {
                                Text(
                                    text = "$",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaPrimary,
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                )
                            },
                            trailingIcon = {
                                if (amountString.isNotEmpty()) {
                                    IconButton(
                                        onClick = { amountString = "" },
                                        modifier = Modifier.testTag("btn_clear_amount_field")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar monto",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_amount")
                        )

                        // Quick increment chips for speed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(10.0, 20.0, 50.0, 100.0).forEach { plusVal ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CreditiaSurfaceContainerLow)
                                        .clickable {
                                            val current = amountString.toDoubleOrNull() ?: 0.0
                                            val newVal = current + plusVal
                                            amountString = String.format(Locale.US, "%.2f", newVal)
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$${plusVal.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CreditiaPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card Selector Dropdown
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Tarjeta de Crédito Seleccionada",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CreditiaPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Corte: Día ${selectedCard?.cutDay ?: 24}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaPrimary
                                )
                            }
                        }

                        // Dropdown trigger box
                        Box {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showCardDropdown = true }
                                    .testTag("btn_select_card_dropdown")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CardNetworkLogo(
                                            network = selectedCard?.network ?: "VISA",
                                            cardName = selectedCard?.cardName ?: "",
                                            size = 20.dp
                                        )
                                        Column {
                                            Text(
                                                text = selectedCard?.cardName ?: (selectedCard?.network ?: "Tarjeta de Crédito"),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "terminación ${selectedCard?.last4 ?: "----"} • Día de corte: ${selectedCard?.cutDay ?: 24}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.UnfoldMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCardDropdown,
                                onDismissRequest = { showCardDropdown = false }
                            ) {
                                cards.forEach { card ->
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            CardNetworkLogo(
                                                network = card.network,
                                                cardName = card.cardName,
                                                size = 18.dp
                                            )
                                        },
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${card.cardName} (${card.last4})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "Corte: Día ${card.cutDay} de cada mes",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCard = card
                                            showCardDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Date Selection & Auto-Period Calculation
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Fecha de Transacción",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CreditiaTertiary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "YYYY/MM/DD",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaTertiary
                                )
                            }
                        }

                        // Selected Date Bar with interactive calendar picker and today shortcut
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CreditiaSurfaceContainerLow)
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            selectedYear = y
                                            selectedMonth = m + 1
                                            selectedDay = d
                                        },
                                        selectedYear,
                                        selectedMonth - 1,
                                        selectedDay
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = formattedDate,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Toca para abrir selector de fecha completo",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(CreditiaPrimary)
                                    .clickable {
                                        val cal = Calendar.getInstance()
                                        selectedYear = cal.get(Calendar.YEAR)
                                        selectedMonth = cal.get(Calendar.MONTH) + 1
                                        selectedDay = cal.get(Calendar.DAY_OF_MONTH)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("btn_set_today")
                            ) {
                                Text(
                                    text = "Hoy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Auto-period calculated box
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CreditiaSecondaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timelapse,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Periodo asignado: $billingPeriod",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Desde ${cycleRange.split(" - ").firstOrNull()} hasta ${cycleRange.split(" - ").lastOrNull()} (calculado a partir del día de corte $cutDay)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Establecimiento Comercial
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Establecimiento comercial",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = establishment,
                            onValueChange = { establishment = it },
                            placeholder = { Text("Nombre del comercio o proveedor") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_establishment"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Descripción
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Descripción",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Referencia o detalle del consumo") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_description"),
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Primary Action Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val parsedAmount = amountString.toDoubleOrNull() ?: 0.0
                            if (editing != null) {
                                showConfirmEditDialog = true
                            } else {
                                val cardId = selectedCard?.id ?: 1L
                                viewModel.saveExpense(
                                    id = null,
                                    cardId = cardId,
                                    amount = parsedAmount,
                                    establishment = establishment,
                                    description = description,
                                    dateString = formattedDate
                                )
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_confirm_save_expense"),
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (editing != null) "Actualizar Consumo" else "Confirmar y Guardar Consumo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (editing != null) {
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_delete_editing_expense"),
                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaErrorContainer),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = CreditiaError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Eliminar este consumo",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaError
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.clearEditingExpense()
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_discard_expense"),
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaSurfaceContainerHigh),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Descartar",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // SQLite Footnote
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Registro guardado únicamente en la base de datos interna SQLite de este dispositivo.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Modal Confirmar Modificación de Registro / Consumo
        if (showConfirmEditDialog && editing != null) {
            val parsedAmount = amountString.toDoubleOrNull() ?: 0.0
            val cardId = selectedCard?.id ?: 1L
            AlertDialog(
                onDismissRequest = { showConfirmEditDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = CreditiaPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "¿Guardar cambios en el registro?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "¿Estás seguro de que deseas guardar las modificaciones realizadas en el registro #${editing.sequenceNumber}?",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "• Establecimiento: ${establishment.ifBlank { "Consumo General" }}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "• Monto: S/ ${String.format(Locale.US, "%.2f", parsedAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaPrimary
                                )
                                Text(
                                    text = "• Fecha: $formattedDate",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• Tarjeta: ${selectedCard?.cardName ?: "Tarjeta"}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmEditDialog = false
                            viewModel.saveExpense(
                                id = editing.id,
                                cardId = cardId,
                                amount = parsedAmount,
                                establishment = establishment,
                                description = description,
                                dateString = formattedDate
                            )
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_confirm_edit_expense_dialog")
                    ) {
                        Text("Confirmar y Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmEditDialog = false },
                        modifier = Modifier.testTag("btn_cancel_edit_expense_dialog")
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Modal Confirmar Eliminación al Editar
        if (showDeleteConfirmation && editing != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = CreditiaError,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "¿Eliminar este consumo?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "¿Deseas eliminar permanentemente el registro #${editing.sequenceNumber} (${editing.establishment} por $ ${String.format(Locale.US, "%.2f", editing.amount)}) de la base de datos local?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation = false
                            viewModel.deleteExpenseDirectly(editing.id)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaError),
                        modifier = Modifier.testTag("btn_confirm_delete_from_edit")
                    ) {
                        Text(
                            text = "Eliminar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirmation = false },
                        modifier = Modifier.testTag("btn_cancel_delete_from_edit")
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
