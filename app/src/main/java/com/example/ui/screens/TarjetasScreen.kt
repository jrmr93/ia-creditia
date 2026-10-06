package com.example.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AddCardDialog
import com.example.ui.components.CardFormDialog
import com.example.ui.components.CycleConfigBottomSheet
import com.example.ui.components.MastercardLogo
import com.example.ui.components.VisaLogo
import com.example.ui.components.CardNetworkLogo
import com.example.ui.components.getCardGradient
import com.example.ui.components.isMastercardNetwork
import com.example.util.BiometricHelper
import com.example.ui.theme.CreditiaCardBlackEnd
import com.example.ui.theme.CreditiaCardBlackMid
import com.example.ui.theme.CreditiaCardBlackStart
import com.example.ui.theme.CreditiaCardBlueEnd
import com.example.ui.theme.CreditiaCardBlueMid
import com.example.ui.theme.CreditiaCardBlueStart
import com.example.ui.theme.CreditiaError
import com.example.ui.theme.CreditiaErrorContainer
import com.example.ui.theme.CreditiaOnError
import com.example.ui.theme.CreditiaOnErrorContainer
import com.example.ui.theme.CreditiaPrimary
import com.example.ui.theme.CreditiaPrimaryContainer
import com.example.ui.theme.CreditiaSecondaryContainer
import com.example.ui.theme.CreditiaSurfaceContainer
import com.example.ui.theme.CreditiaSurfaceContainerHigh
import com.example.ui.theme.CreditiaSurfaceContainerLow
import com.example.ui.theme.CreditiaSurfaceContainerLowest
import com.example.ui.theme.CreditiaTertiary
import com.example.ui.theme.CreditiaTertiaryContainer
import com.example.ui.viewmodel.CreditiaUiState
import com.example.ui.viewmodel.CreditiaViewModel
import java.util.Locale

@Composable
fun TarjetasScreen(
    viewModel: CreditiaViewModel,
    uiState: CreditiaUiState,
    allExpenses: List<ExpenseEntity>,
    onNavigateToConsumosWithCard: (CreditCardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val cards = uiState.cards
    val totalExpensesSum = allExpenses.sumOf { it.amount }
    val context = LocalContext.current
    val activity = context as? Activity
    var showPinFallbackDialog by remember { mutableStateOf(false) }
    var enteredDeletePin by remember { mutableStateOf("") }
    var deletePinError by remember { mutableStateOf<String?>(null) }

    fun triggerBiometricDelete(target: CreditCardEntity) {
        if (activity != null && BiometricHelper.isBiometricHardwareAvailable(context)) {
            BiometricHelper.showNativeBiometricPrompt(
                activity = activity,
                title = "Creditia - Autenticación Requerida",
                subtitle = "Confirma tu huella digital para eliminar la tarjeta ${target.cardName}",
                onSuccess = {
                    viewModel.confirmDeleteCard()
                },
                onError = { _ ->
                    showPinFallbackDialog = true
                },
                onUsePin = {
                    showPinFallbackDialog = true
                }
            )
        } else {
            showPinFallbackDialog = true
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
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Top Hero Action: Agregar Nueva Tarjeta
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.openAddCard() }
                        .testTag("btn_open_add_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CreditiaPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCard,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Agregar Tarjeta de Crédito",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Configura cortes, cuotas y límites locales",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CreditiaSurfaceContainerLowest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Resumen Métrico Rápido Bento
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tile 1: Tarjetas Activas
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tarjetas Activas",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${cards.size}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CreditiaTertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "100% Offline",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CreditiaTertiary
                                    )
                                }
                            }
                        }
                    }

                    // Tile 2: Consumos en Curso
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Consumos en Curso",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = CreditiaTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "$ %,.2f", totalExpensesSum),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Título de Sección
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tarjetas Configuradas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gestión de ciclos de facturación y amortización",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(CreditiaSecondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${cards.size} Registradas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Lista de Tarjetas
            if (cards.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
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
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "No hay tarjetas configuradas",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "La base de datos está limpia. Presiona \"Agregar Tarjeta de Crédito\" arriba para registrar una nueva.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(cards, key = { it.id }) { card ->
                    val cardExpenses = allExpenses.filter { it.cardId == card.id }
                    val cardSum = cardExpenses.sumOf { it.amount }
                    val daysUntilCut = viewModel.getDaysUntilCut(card.cutDay)

                    CardItemView(
                        card = card,
                        expenseCount = cardExpenses.size,
                        totalSum = cardSum,
                        daysUntilCut = daysUntilCut,
                        onEditCard = { viewModel.openEditCard(card) },
                        onViewConsumos = {
                            viewModel.selectCard(card)
                            onNavigateToConsumosWithCard(card)
                        },
                        onDelete = { viewModel.promptDeleteCard(card) }
                    )
                }
            }

            // Nota de Arquitectura Técnica y Privacidad Local
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CreditiaTertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Arquitectura 100% Descentralizada",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Datos almacenados localmente de forma descentralizada. Ningún dato sale de este teléfono ni se sincroniza con servidores remotos.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal Editar Tarjeta Completa
    if (uiState.editingCard != null) {
        val targetCard = uiState.editingCard
        CardFormDialog(
            cardToEdit = targetCard,
            onDismiss = { viewModel.closeEditCard() },
            onConfirm = { bank, name, network, tier, last4, cut, businessDays, payDay, limit, style ->
                viewModel.updateCardDetails(
                    cardId = targetCard.id,
                    bankName = bank,
                    cardName = name,
                    network = network,
                    tier = tier,
                    last4 = last4,
                    cutDay = cut,
                    payBusinessDays = businessDays,
                    payDay = payDay,
                    creditLimit = limit,
                    style = style
                )
            }
        )
    }

    // Modal Editar Ciclos (rápido)
    if (uiState.cycleConfigCard != null) {
        val targetCard = uiState.cycleConfigCard
        CycleConfigBottomSheet(
            card = targetCard,
            onDismiss = { viewModel.closeCycleConfig() },
            onSave = { cutDay, payDay, payBusinessDays ->
                viewModel.saveCycleConfig(targetCard.id, cutDay, payDay, payBusinessDays)
            }
        )
    }

    // Dialog Confirmar Eliminar Tarjeta con Biometría
    if (uiState.cardToDelete != null) {
        val target = uiState.cardToDelete
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissDeleteCard()
                showPinFallbackDialog = false
                enteredDeletePin = ""
                deletePinError = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CreditiaErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = CreditiaError,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(text = "¿Eliminar tarjeta?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "¿Estás seguro de que deseas eliminar la tarjeta ${target.cardName} (•••• ${target.last4})? Se eliminarán también sus consumos asociados de este dispositivo.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CreditiaErrorContainer.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CreditiaError,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Se requiere verificación biométrica para eliminar.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CreditiaError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { triggerBiometricDelete(target) },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaError),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = "Autenticar y Eliminar", color = CreditiaOnError, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissDeleteCard()
                        showPinFallbackDialog = false
                        enteredDeletePin = ""
                        deletePinError = null
                    },
                    modifier = Modifier.testTag("btn_cancel_delete_card")
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

    // Modal Fallback PIN para Eliminar Tarjeta
    if (showPinFallbackDialog && uiState.cardToDelete != null) {
        val target = uiState.cardToDelete
        AlertDialog(
            onDismissRequest = {
                showPinFallbackDialog = false
                enteredDeletePin = ""
                deletePinError = null
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CreditiaErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CreditiaError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(text = "Autorizar con PIN Maestro", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Para eliminar la tarjeta ${target?.cardName} (•••• ${target?.last4}), ingresa tu PIN Maestro de Creditia:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = enteredDeletePin,
                        onValueChange = {
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                enteredDeletePin = it
                                deletePinError = null
                            }
                        },
                        label = { Text("PIN Maestro") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = deletePinError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_delete_card_pin")
                    )
                    if (deletePinError != null) {
                        Text(
                            text = deletePinError ?: "",
                            color = CreditiaError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredDeletePin == uiState.masterPin) {
                            showPinFallbackDialog = false
                            enteredDeletePin = ""
                            deletePinError = null
                            viewModel.confirmDeleteCard()
                        } else {
                            deletePinError = "PIN incorrecto. Inténtalo nuevamente."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaError),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_pin_delete")
                ) {
                    Text(text = "Confirmar Eliminación", color = CreditiaOnError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinFallbackDialog = false
                        enteredDeletePin = ""
                        deletePinError = null
                    }
                ) {
                    Text(text = "Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun CardItemView(
    card: CreditCardEntity,
    expenseCount: Int,
    totalSum: Double,
    daysUntilCut: Int,
    onEditCard: () -> Unit,
    onViewConsumos: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaster = isMastercardNetwork(card.network, card.cardName)
    val isBlackCard = isMaster || card.cardColorTheme == "black" || card.tier.contains("BLACK", ignoreCase = true)
    val gradientBrush = getCardGradient(card.cardColorTheme, card.network, card.tier, card.cardName)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_config_item_${card.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Visual Credit Card Surface Top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush)
                    .clickable { onEditCard() }
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header: Bank name & Network
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.bankName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBlackCard) Color(0xFFFDE68A) else Color(0xFFDBE1FF),
                            letterSpacing = 1.sp
                        )
                        val isMastercard = isMastercardNetwork(card.network, card.cardName)
                        if (isMastercard) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Mastercard",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (card.tier.isNotBlank() && !card.tier.equals("BLACK EDITION", ignoreCase = true)) {
                                        Text(
                                            text = card.tier,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBlackCard) Color(0xFFFDE68A) else Color(0xFFB4C5FF),
                                            letterSpacing = 1.2.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                MastercardLogo(size = 20.dp)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "VISA",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White
                                )
                                if (card.tier.isNotBlank() && !card.tier.equals("BLACK EDITION", ignoreCase = true)) {
                                    Text(
                                        text = card.tier,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBlackCard) Color(0xFFFDE68A) else Color(0xFFB4C5FF),
                                        letterSpacing = 1.2.sp
                                    )
                                }
                            }
                        }
                    }

                    // EMV Chip & Contactless
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 38.dp, height = 26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
                                    )
                                )
                        )
                        Text(
                            text = ")))",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Card Number & Active Period
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "NÚMERO DE TARJETA",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "•••• •••• •••• ${card.last4}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isBlackCard) Color(0xFFFCD34D) else Color.White,
                                letterSpacing = 2.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBlackCard) "CORTE DEL MES" else "PERIODO ACTIVO",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (isBlackCard) "Día ${String.format(Locale.US, "%02d", card.cutDay)}" else "Corta en $daysUntilCut días",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlackCard) Color.White else Color(0xFF6FFBBE)
                            )
                        }
                    }
                }
            }

            // Configuration Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = "IDENTIFICADOR LOCAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = card.cardName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Ciclos de Facturación
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CreditiaSurfaceContainerLow)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Día de Corte",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Día ${String.format(Locale.US, "%02d", card.cutDay)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val nextCut = if (card.cutDay == 31) 1 else card.cutDay + 1
                        Text(
                            text = "Corta el ${card.cutDay} e inicia el $nextCut",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = CreditiaTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Día de Pago",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Día ${String.format(Locale.US, "%02d", card.payDay)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${card.payBusinessDays} días hábiles tras corte",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Consumos Actuales Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CreditiaSurfaceContainer)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CreditiaPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "$expenseCount consumos activos",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Facturación del ciclo",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = String.format(Locale.US, "$ %,.2f", totalSum),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Acciones de Tarjeta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onEditCard,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_edit_card_${card.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Editar Tarjeta",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = onViewConsumos,
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("btn_view_consumos_${card.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaSurfaceContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ListAlt,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Consumos",
                                fontSize = 13.sp,
                                color = CreditiaPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CreditiaErrorContainer)
                            .testTag("btn_delete_card_${card.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar tarjeta",
                            tint = CreditiaOnErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
