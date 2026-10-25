package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.CreditiaRepository
import com.example.ui.theme.CreditiaPrimary
import com.example.ui.theme.CreditiaPrimaryContainer
import com.example.ui.theme.CreditiaSecondaryContainer
import com.example.ui.theme.CreditiaSurfaceContainerHigh
import com.example.ui.theme.CreditiaSurfaceContainerLow
import com.example.ui.theme.CreditiaSurfaceContainerLowest
import com.example.ui.theme.CreditiaTertiary
import java.util.Locale

@Composable
fun DiferirConsumoDialog(
    amount: Double,
    establishment: String,
    dateString: String,
    cutDay: Int,
    cardName: String,
    repository: CreditiaRepository,
    onDismiss: () -> Unit,
    periodResolver: ((String) -> String)? = null,
    onConfirm: (months: Int, monthlyInterestRate: Double, startDateString: String) -> Unit
) {
    val context = LocalContext.current
    var deferDateString by remember(dateString) { mutableStateOf(dateString) }

    val quickMonthOptions = listOf(3, 6, 9, 12, 18, 24, 36)
    var selectedMonths by remember { mutableIntStateOf(3) }
    var isCustomMonths by remember { mutableStateOf(false) }
    var customMonthsInput by remember { mutableStateOf("") }
    var showAllInstallments by remember { mutableStateOf(false) }

    var isZeroInterest by remember { mutableStateOf(true) }
    var interestRateInput by remember { mutableStateOf("2.5") }

    val effectiveMonths = if (isCustomMonths) {
        customMonthsInput.toIntOrNull()?.coerceIn(2, 60) ?: selectedMonths
    } else {
        selectedMonths
    }

    val effectiveRate = if (isZeroInterest) 0.0 else (interestRateInput.toDoubleOrNull() ?: 0.0)

    val installmentPlan = remember(amount, effectiveMonths, effectiveRate, deferDateString, cutDay) {
        repository.calculateInstallmentPlan(
            amount = amount,
            months = effectiveMonths,
            monthlyInterestRate = effectiveRate,
            startDateString = deferDateString,
            cutDay = cutDay,
            periodResolver = periodResolver
        )
    }

    val installmentAmount = installmentPlan.firstOrNull()?.amount ?: (amount / effectiveMonths)
    val totalPayment = installmentPlan.sumOf { it.amount }
    val totalInterest = (totalPayment - amount).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Evitar dismiss al hacer clic dentro del modal
                    ),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Header fijo en la parte superior
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CreditiaTertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = CreditiaTertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Diferir Consumo",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$cardName • Corte: día $cutDay",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cuerpo Scrolleable: Selector, Intereses, Resumen y Cronograma
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Selector para modificar la fecha de creación del diferido
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CreditiaSurfaceContainerLow,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    try {
                                        val parts = deferDateString.split("/")
                                        if (parts.size == 3) {
                                            cal.set(Calendar.YEAR, parts[0].toInt())
                                            cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                                            cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                        }
                                    } catch (_: Exception) {}

                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            deferDateString = String.format(Locale.US, "%04d/%02d/%02d", y, m + 1, d)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CreditiaPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Fecha del diferido",
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Fecha de creación del diferido",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = deferDateString,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        try {
                                            val parts = deferDateString.split("/")
                                            if (parts.size == 3) {
                                                cal.set(Calendar.YEAR, parts[0].toInt())
                                                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                                                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                            }
                                        } catch (_: Exception) {}

                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                deferDateString = String.format(Locale.US, "%04d/%02d/%02d", y, m + 1, d)
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditCalendar,
                                        contentDescription = "Cambiar fecha",
                                        tint = CreditiaPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Info Box sobre el corte y la primera cuota
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CreditiaPrimary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                val firstPeriod = installmentPlan.firstOrNull()?.billingPeriod ?: ""
                                Text(
                                    text = "Diferido creado el $deferDateString. Al cerrar el corte (día $cutDay), la 1ª cuota ingresará en el periodo $firstPeriod.",
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Selector de Plazo (Meses)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Seleccionar Plazo (Meses a diferir):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickMonthOptions.take(4).forEach { months ->
                                    val isSelected = !isCustomMonths && selectedMonths == months
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CreditiaPrimary else CreditiaSurfaceContainerLow)
                                            .border(
                                                1.dp,
                                                if (isSelected) CreditiaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                isCustomMonths = false
                                                selectedMonths = months
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$months m",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                quickMonthOptions.drop(4).forEach { months ->
                                    val isSelected = !isCustomMonths && selectedMonths == months
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CreditiaPrimary else CreditiaSurfaceContainerLow)
                                            .border(
                                                1.dp,
                                                if (isSelected) CreditiaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                isCustomMonths = false
                                                selectedMonths = months
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$months m",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Opción personalizada de meses
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCustomMonths) CreditiaTertiary else CreditiaSurfaceContainerLow)
                                        .border(
                                            1.dp,
                                            if (isCustomMonths) CreditiaTertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            isCustomMonths = true
                                            if (customMonthsInput.isBlank()) customMonthsInput = "4"
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isCustomMonths && customMonthsInput.isNotBlank()) "$customMonthsInput m" else "Otro",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomMonths) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCustomMonths) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (isCustomMonths) {
                                OutlinedTextField(
                                    value = customMonthsInput,
                                    onValueChange = { customMonthsInput = it.filter { char -> char.isDigit() }.take(2) },
                                    label = { Text("Cantidad de meses (2 a 60)", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Sección de Interés Bancario
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CreditiaSurfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Sin Intereses (0%)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Promoción de cuotas sin recargo del banco",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isZeroInterest,
                                        onCheckedChange = { isZeroInterest = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = CreditiaPrimary
                                        )
                                    )
                                }

                                if (!isZeroInterest) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = interestRateInput,
                                            onValueChange = { interestRateInput = it.replace(",", ".") },
                                            label = { Text("Tasa de interés mensual (% TEM)", fontSize = 11.sp) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Percent,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = CreditiaPrimary
                                                )
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Interés Total:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "+ S/ ${String.format(Locale.US, "%.2f", totalInterest)}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CreditiaTertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Resumen Financiero y Cuota
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CreditiaPrimaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Cuota mensual ($effectiveMonths meses):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "S/ ${String.format(Locale.US, "%.2f", installmentAmount)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CreditiaPrimary
                                    )
                                }

                                HorizontalDivider(color = CreditiaPrimary.copy(alpha = 0.15f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Monto original: S/ ${String.format(Locale.US, "%.2f", amount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Total final: S/ ${String.format(Locale.US, "%.2f", totalPayment)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Cronograma de Cuotas (Vista previa)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cronograma de pagos estimado:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (installmentPlan.size > 4) {
                                Text(
                                    text = if (showAllInstallments) "Ver menos" else "Ver todas (${installmentPlan.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CreditiaPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { showAllInstallments = !showAllInstallments }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CreditiaSurfaceContainerLowest)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val displayedList = if (showAllInstallments || installmentPlan.size <= 4) {
                                installmentPlan
                            } else {
                                installmentPlan.take(3)
                            }

                            displayedList.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Cuota ${item.installmentNumber}/${item.totalInstallments} • ${item.billingPeriod}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Fecha aproximada: ${item.dateString}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "S/ ${String.format(Locale.US, "%.2f", item.amount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CreditiaPrimary
                                    )
                                }
                                if (index < displayedList.size - 1 || (!showAllInstallments && installmentPlan.size > 4)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                }
                            }

                            if (!showAllInstallments && installmentPlan.size > 4) {
                                val remaining = installmentPlan.size - 3
                                val lastItem = installmentPlan.last()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { showAllInstallments = true }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+ $remaining cuotas más hasta ${lastItem.billingPeriod}...",
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "S/ ${String.format(Locale.US, "%.2f", lastItem.amount)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CreditiaPrimary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botones de Acción Fijos al final del diálogo (siempre visibles y sobre la barra de navegación)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                onConfirm(effectiveMonths, effectiveRate, deferDateString)
                            },
                            modifier = Modifier.weight(1.4f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Generar $effectiveMonths Cuotas",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiferirConsumoDialog(
    amount: Double,
    establishment: String,
    dateString: String,
    cutDay: Int,
    cardName: String,
    repository: CreditiaRepository,
    onDismiss: () -> Unit,
    periodResolver: ((String) -> String)? = null,
    onConfirm: (months: Int, monthlyInterestRate: Double) -> Unit
) = DiferirConsumoDialog(
    amount = amount,
    establishment = establishment,
    dateString = dateString,
    cutDay = cutDay,
    cardName = cardName,
    repository = repository,
    onDismiss = onDismiss,
    periodResolver = periodResolver,
    onConfirm = { months, rate, _ -> onConfirm(months, rate) }
)

