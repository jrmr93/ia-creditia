package com.example.ui.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.util.GeminiService
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.ZoomIn
import com.example.util.ReceiptStorageUtil
import com.example.util.OfflineReceiptAnalyzer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedButton
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.components.DiferirConsumoDialog
import com.example.ui.components.LogoSearchDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventRepeat
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
import androidx.compose.material.icons.filled.Percent
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
    var showDeferDialog by remember { mutableStateOf(false) }

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

    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    var currentLogoUrl by remember {
        mutableStateOf(editing?.establishmentLogoUrl ?: viewModel.getRememberedLogo(editing?.establishment ?: ""))
    }

    LaunchedEffect(establishment) {
        val rem = viewModel.getRememberedLogo(establishment)
        if (rem != null && currentLogoUrl != rem) {
            currentLogoUrl = rem
        }
    }

    LaunchedEffect(uiState.lastAppliedLogoPath) {
        val path = uiState.lastAppliedLogoPath
        if (path != null) {
            if (path.isEmpty()) {
                currentLogoUrl = null
            } else {
                currentLogoUrl = path
            }
            viewModel.clearLastAppliedLogoPath()
        }
    }

    val knownEstablishments = remember(allExpenses) {
        allExpenses
            .map { it.establishment.trim() }
            .filter { it.isNotBlank() && !it.startsWith("Nota de Crédito", ignoreCase = true) }
            .distinctBy { it.lowercase(Locale.ROOT) }
    }

    val trimmedEst = establishment.trim()
    val suggestions = remember(trimmedEst, knownEstablishments) {
        if (trimmedEst.isNotEmpty()) {
            knownEstablishments.filter {
                it.contains(trimmedEst, ignoreCase = true) && !it.equals(trimmedEst, ignoreCase = true)
            }.take(4)
        } else {
            emptyList()
        }
    }

    var description by remember {
        mutableStateOf(editing?.description ?: "")
    }

    var transactionType by remember {
        mutableStateOf(editing?.transactionType ?: ExpenseEntity.TYPE_EXPENSE)
    }

    var isDirectDeferred by remember { mutableStateOf(false) }
    var deferredMonths by remember { mutableIntStateOf(3) }
    var isCustomDeferredMonths by remember { mutableStateOf(false) }
    var customDeferredMonthsInput by remember { mutableStateOf("") }
    var isDeferredZeroInterest by remember { mutableStateOf(true) }
    var deferredInterestRateInput by remember { mutableStateOf("0.0") }

    var showCardDropdown by remember { mutableStateOf(false) }

    var scannedReceiptUri by remember { mutableStateOf(editing?.receiptUri) }
    var viewingReceiptUri by remember { mutableStateOf<String?>(null) }
    var isAnalyzingReceipt by remember { mutableStateOf(false) }
    var showNoApiKeyDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (viewingReceiptUri != null) {
            viewingReceiptUri = null
        } else {
            if (scannedReceiptUri != null && scannedReceiptUri != editing?.receiptUri) {
                ReceiptStorageUtil.deleteReceiptFile(scannedReceiptUri)
            }
            viewModel.clearEditingExpense()
            onBack()
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val analyzeReceiptAction: (targetUri: String, forceOffline: Boolean) -> Unit = { targetUri, forceOffline ->
        isAnalyzingReceipt = true
        coroutineScope.launch {
            try {
                val parsedUri = if (targetUri.startsWith("/")) {
                    android.net.Uri.fromFile(File(targetUri))
                } else if (targetUri.startsWith("file://")) {
                    android.net.Uri.fromFile(File(targetUri.removePrefix("file://")))
                } else {
                    android.net.Uri.parse(targetUri)
                }

                val bitmap = GeminiService.loadSampledBitmapFromUri(context, parsedUri)
                if (bitmap == null) {
                    viewModel.showToast("No se pudo cargar la imagen del comprobante")
                    isAnalyzingReceipt = false
                    return@launch
                }

                val useCloudAi = !forceOffline && uiState.isGeminiAiEnabled && uiState.geminiApiKey.isNotBlank()

                if (useCloudAi) {
                    val result = GeminiService.analyzeReceipt(
                        apiKey = uiState.geminiApiKey,
                        modelName = uiState.geminiModel,
                        bitmap = bitmap
                    )
                    result.onSuccess { data ->
                        if (data.amount > 0.0) {
                            amountString = String.format(Locale.US, "%.2f", data.amount)
                        }
                        if (data.establishment.isNotBlank()) {
                            establishment = data.establishment
                        }
                        if (data.description.isNotBlank()) {
                            description = data.description
                        }
                        if (data.dateString.isNotBlank()) {
                            val parts = data.dateString.split("/")
                            if (parts.size == 3) {
                                val y = parts[0].toIntOrNull()
                                val m = parts[1].toIntOrNull()
                                val d = parts[2].toIntOrNull()
                                if (y != null && m != null && d != null) {
                                    selectedYear = y
                                    selectedMonth = m
                                    selectedDay = d
                                }
                            }
                        }
                        viewModel.showToast("✓ Comprobante analizado con éxito por Gemini IA")
                    }.onFailure { err ->
                        // Fallback automático al analizador Offline si hay problemas de red o cuota en Gemini
                        viewModel.showToast("Gemini IA no respondió. Analizando con OCR Offline...")
                        val offResult = OfflineReceiptAnalyzer.analyzeReceiptOffline(bitmap)
                        offResult.onSuccess { offData ->
                            if (offData.amount > 0.0) amountString = String.format(Locale.US, "%.2f", offData.amount)
                            if (offData.establishment.isNotBlank()) establishment = offData.establishment
                            if (offData.description.isNotBlank()) description = offData.description
                            if (offData.dateString.isNotBlank()) {
                                val parts = offData.dateString.split("/")
                                if (parts.size == 3) {
                                    val y = parts[0].toIntOrNull()
                                    val m = parts[1].toIntOrNull()
                                    val d = parts[2].toIntOrNull()
                                    if (y != null && m != null && d != null) {
                                        selectedYear = y
                                        selectedMonth = m
                                        selectedDay = d
                                    }
                                }
                            }
                            viewModel.showToast("✓ Datos extraídos de forma local (Offline)")
                        }.onFailure { offErr ->
                            viewModel.showToast("Error al analizar comprobante: ${offErr.localizedMessage ?: offErr.message}")
                        }
                    }
                } else {
                    // Modo Offline
                    val offResult = OfflineReceiptAnalyzer.analyzeReceiptOffline(bitmap)
                    offResult.onSuccess { offData ->
                        if (offData.amount > 0.0) amountString = String.format(Locale.US, "%.2f", offData.amount)
                        if (offData.establishment.isNotBlank()) establishment = offData.establishment
                        if (offData.description.isNotBlank()) description = offData.description
                        if (offData.dateString.isNotBlank()) {
                            val parts = offData.dateString.split("/")
                            if (parts.size == 3) {
                                val y = parts[0].toIntOrNull()
                                val m = parts[1].toIntOrNull()
                                val d = parts[2].toIntOrNull()
                                if (y != null && m != null && d != null) {
                                    selectedYear = y
                                    selectedMonth = m
                                    selectedDay = d
                                }
                            }
                        }
                        viewModel.showToast("✓ Comprobante analizado con éxito de forma 100% Offline")
                    }.onFailure { offErr ->
                        viewModel.showToast("No se pudo analizar offline: ${offErr.localizedMessage ?: offErr.message}")
                    }
                }
            } catch (e: Exception) {
                viewModel.showToast("Error inesperado: ${e.localizedMessage}")
            } finally {
                isAnalyzingReceipt = false
            }
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val gmsResult = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            val imageUri = gmsResult?.pages?.firstOrNull()?.imageUri
            if (imageUri != null) {
                // Guarda la imagen de inmediato en la carpeta interna privada de la app
                val savedInternalPath = ReceiptStorageUtil.saveReceiptImage(context, imageUri)
                val targetPath = savedInternalPath ?: imageUri.toString()
                if (savedInternalPath != null) {
                    if (scannedReceiptUri != null && scannedReceiptUri != editing?.receiptUri) {
                        ReceiptStorageUtil.deleteReceiptFile(scannedReceiptUri)
                    }
                    scannedReceiptUri = savedInternalPath
                } else {
                    scannedReceiptUri = imageUri.toString()
                }

                analyzeReceiptAction(targetPath, false)
            }
        }
    }

    val galleryReceiptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { imageUri: Uri? ->
        if (imageUri != null) {
            val savedInternalPath = ReceiptStorageUtil.saveReceiptImage(context, imageUri)
            val targetPath = savedInternalPath ?: imageUri.toString()
            if (savedInternalPath != null) {
                if (scannedReceiptUri != null && scannedReceiptUri != editing?.receiptUri) {
                    ReceiptStorageUtil.deleteReceiptFile(scannedReceiptUri)
                }
                scannedReceiptUri = savedInternalPath
            } else {
                scannedReceiptUri = imageUri.toString()
            }

            analyzeReceiptAction(targetPath, false)
        }
    }

    val launchReceiptScanner = {
        var currentContext = context
        var act: Activity? = null
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                act = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        if (act == null) {
            viewModel.showToast("No se pudo iniciar el escáner de cámara")
        } else {
            scanner.getStartScanIntent(act)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
                .addOnFailureListener { e ->
                    viewModel.showToast("Error al abrir escáner: ${e.localizedMessage}")
                }
        }
    }

    val formattedDate = String.format(Locale.US, "%04d/%02d/%02d", selectedYear, selectedMonth, selectedDay)
    val cutDay = selectedCard?.cutDay ?: 24
    val billingPeriod = viewModel.repository.calculateBillingPeriod(formattedDate, cutDay)
    val cycleRange = viewModel.getBillingCycleRange(selectedCard?.id, billingPeriod, cutDay)

    val parsedAmount = amountString.toDoubleOrNull() ?: 0.0
    val effectiveDirectMonths = if (isCustomDeferredMonths) {
        customDeferredMonthsInput.toIntOrNull()?.coerceIn(2, 60) ?: deferredMonths
    } else {
        deferredMonths
    }.coerceAtLeast(2)

    val effectiveDirectRate = if (isDeferredZeroInterest) {
        0.0
    } else {
        deferredInterestRateInput.toDoubleOrNull() ?: 0.0
    }

    val directInstallmentPlan = remember(parsedAmount, effectiveDirectMonths, effectiveDirectRate, formattedDate, cutDay) {
        if (parsedAmount > 0.0) {
            viewModel.repository.calculateInstallmentPlan(
                amount = parsedAmount,
                months = effectiveDirectMonths,
                monthlyInterestRate = effectiveDirectRate,
                startDateString = formattedDate,
                cutDay = cutDay
            )
        } else {
            emptyList()
        }
    }
    val directInstallmentAmount = directInstallmentPlan.firstOrNull()?.amount ?: if (effectiveDirectMonths > 0) (parsedAmount / effectiveDirectMonths) else 0.0
    val directTotalPayment = directInstallmentPlan.sumOf { it.amount }
    val directTotalInterest = (directTotalPayment - parsedAmount).coerceAtLeast(0.0)

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

            // 1. Selector de Tipo: Consumo vs Diferido vs Pago a la Tarjeta
            if (editing == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("selector_transaction_type")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isConsumo = !isDirectDeferred && transactionType == ExpenseEntity.TYPE_EXPENSE
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isConsumo) Color(0xFFEF4444) else Color.Transparent)
                                    .clickable {
                                        transactionType = ExpenseEntity.TYPE_EXPENSE
                                        isDirectDeferred = false
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("btn_select_type_expense"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = if (isConsumo) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Consumo",
                                        fontSize = 12.sp,
                                        fontWeight = if (isConsumo) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isConsumo) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val isDiferido = isDirectDeferred && transactionType == ExpenseEntity.TYPE_EXPENSE
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDiferido) Color(0xFFF59E0B) else Color.Transparent)
                                    .clickable {
                                        transactionType = ExpenseEntity.TYPE_EXPENSE
                                        isDirectDeferred = true
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("btn_select_type_deferred"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isDiferido) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Diferido",
                                        fontSize = 12.sp,
                                        fontWeight = if (isDiferido) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isDiferido) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val isPago = transactionType == ExpenseEntity.TYPE_PAYMENT
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPago) Color(0xFF10B981) else Color.Transparent)
                                    .clickable {
                                        transactionType = ExpenseEntity.TYPE_PAYMENT
                                        isDirectDeferred = false
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("btn_select_type_payment"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isPago) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Pago Tarjeta",
                                        fontSize = 12.sp,
                                        fontWeight = if (isPago) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPago) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sequential Badge & Native Offline Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPago = transactionType == ExpenseEntity.TYPE_PAYMENT
                    val isEditingDeferred = editing != null && editing.isDeferredExpense()
                    val isDirectDef = isDirectDeferred && editing == null
                    val badgeBg = when {
                        isPago -> Color(0xFF10B981).copy(alpha = 0.15f)
                        isEditingDeferred || isDirectDef -> Color(0xFFFEF3C7)
                        else -> CreditiaSecondaryContainer
                    }
                    val badgeTint = when {
                        isPago -> Color(0xFF047857)
                        isEditingDeferred || isDirectDef -> Color(0xFFB45309)
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(badgeBg)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (editing != null) Icons.Default.Edit else if (isPago) Icons.Default.CheckCircle else if (isDirectDef) Icons.Default.CalendarMonth else Icons.Default.Receipt,
                                contentDescription = null,
                                tint = badgeTint,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (editing != null) {
                                    if (isPago) "Editando Pago a Tarjeta"
                                    else if (isEditingDeferred) "Editando Cuota de Diferido"
                                    else "Editando Consumo"
                                } else {
                                    if (isPago) "Nuevo Pago a Tarjeta"
                                    else if (isDirectDef) "Nuevo Consumo Diferido Directo"
                                    else "Nuevo Consumo"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTint
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

            // Receipt Scanner with ML Kit and Gemini AI
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
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Comprobante de Pago (Opcional)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (uiState.isGeminiAiEnabled) CreditiaTertiary.copy(alpha = 0.12f) else CreditiaPrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isGeminiAiEnabled) Icons.Default.AutoAwesome else Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = if (uiState.isGeminiAiEnabled) CreditiaTertiary else CreditiaPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (uiState.isGeminiAiEnabled) "Lectura IA Gemini" else "OCR 100% Offline",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isGeminiAiEnabled) CreditiaTertiary else CreditiaPrimary
                                    )
                                }
                            }
                        }

                        if (isAnalyzingReceipt) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CreditiaPrimary.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = CreditiaPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                    Column {
                                        Text(
                                            text = if (uiState.isGeminiAiEnabled) "Leyendo comprobante con Gemini..." else "Leyendo comprobante con OCR Offline...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CreditiaPrimary
                                        )
                                        Text(
                                            text = "Autoajustando recorte y extrayendo monto, fecha y comercio",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (scannedReceiptUri != null) {
                            val currentReceipt = scannedReceiptUri!!
                            val imgModel: Any = if (currentReceipt.startsWith("/") || currentReceipt.startsWith("file://")) {
                                File(currentReceipt.removePrefix("file://"))
                            } else {
                                currentReceipt
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CreditiaSurfaceContainerLow)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewingReceiptUri = currentReceipt
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CreditiaSurfaceContainerHigh)
                                                .border(1.dp, CreditiaPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = imgModel,
                                                contentDescription = "Comprobante recortado",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .clip(RoundedCornerShape(topStart = 4.dp))
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .padding(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ZoomIn,
                                                    contentDescription = "Ampliar foto",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "Comprobante Guardado",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Toca para pantalla completa y zoom",
                                                fontSize = 11.sp,
                                                color = CreditiaPrimary
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { galleryReceiptLauncher.launch("image/*") }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = "Cambiar desde galería",
                                                tint = CreditiaPrimary
                                            )
                                        }
                                        IconButton(
                                            onClick = { launchReceiptScanner() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Volver a escanear con cámara",
                                                tint = CreditiaPrimary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val uriToDelete = scannedReceiptUri ?: editing?.receiptUri
                                                if (!uriToDelete.isNullOrBlank()) {
                                                    ReceiptStorageUtil.deleteReceiptFile(uriToDelete)
                                                }
                                                scannedReceiptUri = null
                                                viewModel.showToast("✓ Foto del comprobante eliminada del dispositivo")
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar foto",
                                                tint = CreditiaError
                                            )
                                        }
                                    }
                                }

                                // Botones para reanalizar comprobante existente
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (uiState.isGeminiAiEnabled) {
                                        Button(
                                            onClick = { analyzeReceiptAction(currentReceipt, false) },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaTertiary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Reanalizar con IA",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { analyzeReceiptAction(currentReceipt, true) },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (uiState.isGeminiAiEnabled) CreditiaSurfaceContainerHigh else CreditiaPrimary,
                                            contentColor = if (uiState.isGeminiAiEnabled) MaterialTheme.colorScheme.onSurface else Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (uiState.isGeminiAiEnabled) "Reanalizar Offline" else "Reanalizar Comprobante",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { launchReceiptScanner() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("btn_scan_receipt"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Tomar foto",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Button(
                                    onClick = { galleryReceiptLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("btn_upload_receipt"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CreditiaSurfaceContainerHigh,
                                        contentColor = CreditiaPrimary
                                    ),
                                    border = BorderStroke(1.dp, CreditiaPrimary.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Subir imagen",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Toma una foto o sube un comprobante de la galería. Se autocompleta monto, fecha, establecimiento y descripción con IA.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
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

            // Establecimiento Comercial / Canal de Pago
            item {
                val isPago = transactionType == ExpenseEntity.TYPE_PAYMENT
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
                                    imageVector = if (isPago) Icons.Default.AccountBalanceWallet else Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = if (isPago) Color(0xFF10B981) else CreditiaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isPago) "Entidad o canal de pago" else "Establecimiento comercial",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!isPago && !currentLogoUrl.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Logo recordado",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = establishment,
                                onValueChange = { establishment = it },
                                placeholder = { Text(if (isPago) "Entidad (ej. Transferencia, Débito automático, Banco...)" else "Nombre del comercio o proveedor") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_establishment"),
                                leadingIcon = {
                                    if (!isPago && !currentLogoUrl.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .padding(2.dp)
                                        ) {
                                            AsyncImage(
                                                model = currentLogoUrl,
                                                contentDescription = "Logo",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize().padding(2.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (isPago) Icons.Default.AccountBalanceWallet else Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = if (isPago) Color(0xFF10B981) else CreditiaPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (establishment.isNotEmpty()) {
                                        IconButton(onClick = {
                                            establishment = ""
                                            currentLogoUrl = null
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Limpiar comercio",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (!isPago) {
                                OutlinedButton(
                                    onClick = {
                                        val targetExp = (editing?.copy(establishment = establishment.ifBlank { editing.establishment }))
                                            ?: ExpenseEntity(
                                                id = 0L,
                                                cardId = selectedCard?.id ?: 0L,
                                                amount = parsedAmount,
                                                establishment = establishment.ifBlank { "Comercio" },
                                                description = description,
                                                dateString = formattedDate,
                                                billingPeriod = billingPeriod,
                                                establishmentLogoUrl = currentLogoUrl
                                            )
                                        viewModel.openLogoSearchModal(targetExp, initialQuery = establishment.ifBlank { targetExp.establishment })
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("btn_search_logo_establishment"),
                                    border = BorderStroke(1.dp, CreditiaPrimary.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = CreditiaPrimary.copy(alpha = 0.08f),
                                        contentColor = CreditiaPrimary
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Buscar logo",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Buscar logo",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Sugerencias de autocompletado con logos recordados
                        if (!isPago && suggestions.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Sugerencias con comercios existentes:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                suggestions.forEach { sugName ->
                                    val sugLogo = viewModel.getRememberedLogo(sugName)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CreditiaSurfaceContainerLow,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                establishment = sugName
                                                if (sugLogo != null) {
                                                    currentLogoUrl = sugLogo
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (!sugLogo.isNullOrBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color.White,
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        AsyncImage(
                                                            model = sugLogo,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Fit,
                                                            modifier = Modifier.fillMaxSize().padding(2.dp)
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Storefront,
                                                        contentDescription = null,
                                                        tint = CreditiaPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Text(
                                                    text = sugName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (!sugLogo.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(100.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "✓ Con logo",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF059669),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Descripción / Detalle
            item {
                val isPago = transactionType == ExpenseEntity.TYPE_PAYMENT
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
                                tint = if (isPago) Color(0xFF10B981) else CreditiaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isPago) "Detalle o concepto del pago" else "Descripción",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text(if (isPago) "Referencia (ej. Pago total del mes, abono a capital...)" else "Referencia o detalle del consumo") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_description"),
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Configuración de Diferido Directo (Cuotas e Interés)
            if (isDirectDeferred && editing == null) {
                item {
                    val quickMonthOptions = listOf(3, 6, 9, 12, 18, 24, 36)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_direct_deferred_config")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Plan de Cuotas (Diferido Directo)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Genera cuotas mensuales en amarillo sin nota de crédito",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Info del periodo inicial
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.08f),
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
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    val firstPeriod = directInstallmentPlan.firstOrNull()?.billingPeriod ?: billingPeriod
                                    Text(
                                        text = "Compra el $formattedDate. Al cerrar corte (día $cutDay), la 1ª cuota ingresará en el periodo $firstPeriod.",
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Selector de Plazo (Meses)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Meses a diferir:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quickMonthOptions.take(4).forEach { months ->
                                        val isSelected = !isCustomDeferredMonths && deferredMonths == months
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFF59E0B) else CreditiaSurfaceContainerLow)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    isCustomDeferredMonths = false
                                                    deferredMonths = months
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
                                        val isSelected = !isCustomDeferredMonths && deferredMonths == months
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFF59E0B) else CreditiaSurfaceContainerLow)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    isCustomDeferredMonths = false
                                                    deferredMonths = months
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

                                    // Opción personalizada
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCustomDeferredMonths) Color(0xFFD97706) else CreditiaSurfaceContainerLow)
                                            .border(
                                                1.dp,
                                                if (isCustomDeferredMonths) Color(0xFFD97706) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                isCustomDeferredMonths = true
                                                if (customDeferredMonthsInput.isBlank()) customDeferredMonthsInput = "4"
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isCustomDeferredMonths && customDeferredMonthsInput.isNotBlank()) "$customDeferredMonthsInput m" else "Otro",
                                            fontSize = 12.sp,
                                            fontWeight = if (isCustomDeferredMonths) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCustomDeferredMonths) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (isCustomDeferredMonths) {
                                    OutlinedTextField(
                                        value = customDeferredMonthsInput,
                                        onValueChange = { customDeferredMonthsInput = it.filter { char -> char.isDigit() }.take(2) },
                                        label = { Text("Cantidad de meses (2 a 60)", fontSize = 11.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Sección Intereses
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
                                                text = "Promoción de cuotas sin recargo",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isDeferredZeroInterest,
                                            onCheckedChange = { isDeferredZeroInterest = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFFF59E0B)
                                            )
                                        )
                                    }

                                    if (!isDeferredZeroInterest) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = deferredInterestRateInput,
                                                onValueChange = { deferredInterestRateInput = it.replace(",", ".") },
                                                label = { Text("Tasa de interés mensual (% TEM)", fontSize = 11.sp) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Percent,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = Color(0xFFD97706)
                                                    )
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            // Resumen financiero del diferido directo
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Valor estimado por cuota",
                                            fontSize = 11.sp,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$%.2f / mes", directInstallmentAmount),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF78350F)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$effectiveDirectMonths cuotas mensuales",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                        if (directTotalInterest > 0.0) {
                                            Text(
                                                text = String.format(Locale.US, "Total: $%.2f", directTotalPayment),
                                                fontSize = 11.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Primary Action Buttons
            item {
                val isPago = transactionType == ExpenseEntity.TYPE_PAYMENT
                val isDirectDef = isDirectDeferred && editing == null
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val parsedAmt = amountString.toDoubleOrNull() ?: 0.0
                            if (editing != null) {
                                showConfirmEditDialog = true
                            } else if (isDirectDef) {
                                if (parsedAmt <= 0.0) {
                                    viewModel.showToast("El monto debe ser mayor a 0")
                                    return@Button
                                }
                                val cardId = selectedCard?.id ?: 1L
                                viewModel.registerDirectDeferred(
                                    cardId = cardId,
                                    totalAmount = parsedAmt,
                                    establishment = establishment,
                                    description = description,
                                    startDateString = formattedDate,
                                    months = effectiveDirectMonths,
                                    monthlyInterestRate = effectiveDirectRate,
                                    receiptUri = scannedReceiptUri,
                                    establishmentLogoUrl = currentLogoUrl,
                                    onSuccess = { onBack() }
                                )
                            } else {
                                val cardId = selectedCard?.id ?: 1L
                                viewModel.saveExpense(
                                    id = null,
                                    cardId = cardId,
                                    amount = parsedAmt,
                                    establishment = establishment,
                                    description = description,
                                    dateString = formattedDate,
                                    receiptUri = scannedReceiptUri,
                                    transactionType = transactionType,
                                    establishmentLogoUrl = currentLogoUrl
                                )
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_confirm_save_expense"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isPago -> Color(0xFF10B981)
                                isDirectDef -> Color(0xFFF59E0B)
                                else -> CreditiaPrimary
                            }
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isDirectDef) Icons.Default.CalendarMonth else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (editing != null) {
                                    if (isPago) "Actualizar Pago a Tarjeta"
                                    else if (editing.isDeferredExpense()) "Actualizar Cuota y Diferido"
                                    else "Actualizar Consumo"
                                } else {
                                    when {
                                        isPago -> "Confirmar y Guardar Pago"
                                        isDirectDef -> "Registrar Diferido ($effectiveDirectMonths Cuotas)"
                                        else -> "Confirmar y Guardar Consumo"
                                    }
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (editing != null && !editing.isDeferredExpense() && !isPago) {
                        Button(
                            onClick = { showDeferDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_defer_editing_expense"),
                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaTertiary.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = CreditiaTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Diferir Consumo en Cuotas",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaTertiary
                                )
                            }
                        }

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
                            if (scannedReceiptUri != null && scannedReceiptUri != editing?.receiptUri) {
                                ReceiptStorageUtil.deleteReceiptFile(scannedReceiptUri)
                            }
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
                        text = if (editing.isDeferredExpense()) "¿Actualizar y reorganizar diferido?" else "¿Guardar cambios en el registro?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (editing.isDeferredExpense())
                                "Este consumo es parte de un diferido. Al guardar con la fecha $formattedDate, las cuotas del diferido se reorganizarán manteniendo el intervalo mensual a partir de la fecha seleccionada."
                            else
                                "¿Estás seguro de que deseas guardar las modificaciones realizadas en este consumo?",
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
                                if (editing.isDeferredExpense()) {
                                    val defInfo = editing.getDeferredInfo()
                                    if (defInfo != null) {
                                        Text(
                                            text = "• Cuota actual: Cuota ${defInfo.first} de ${defInfo.second} (se sincronizarán las demás)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CreditiaPrimary
                                        )
                                    }
                                }
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
                                dateString = formattedDate,
                                receiptUri = scannedReceiptUri,
                                transactionType = transactionType,
                                establishmentLogoUrl = currentLogoUrl
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
                        text = if (editing.isDeferredExpense()) "¿Eliminar consumo diferido?" else "¿Eliminar este consumo?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "¿Deseas eliminar permanentemente el consumo de ${editing.establishment} por S/ ${String.format(Locale.US, "%.2f", editing.amount)} de la base de datos local?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (editing.isDeferredExpense()) {
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
                                        fontSize = 11.5.sp,
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

        if (showNoApiKeyDialog) {
            AlertDialog(
                onDismissRequest = { showNoApiKeyDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CreditiaPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Configuración de Gemini requerida",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "Para que la IA de Gemini pueda leer automáticamente el valor, fecha, establecimiento y descripción del comprobante, primero debes ingresar tu API Key en la pantalla de Ajustes.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNoApiKeyDialog = false
                            onBack()
                            viewModel.selectTab("ajustes")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary)
                    ) {
                        Text("Ir a Ajustes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoApiKeyDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Modal de Diferir Consumo en Cuotas
        if (showDeferDialog && editing != null) {
            val parsedAmount = amountString.toDoubleOrNull() ?: editing.amount
            val currentEstablishment = establishment.ifBlank { editing.establishment }
            val currentDescription = description.ifBlank { editing.description }
            val currentReceipt = scannedReceiptUri ?: editing.receiptUri
            val card = selectedCard ?: uiState.selectedCard
            val cutDay = card?.cutDay ?: 24

            DiferirConsumoDialog(
                amount = parsedAmount,
                establishment = currentEstablishment,
                dateString = formattedDate,
                cutDay = cutDay,
                cardName = card?.cardName ?: "Tarjeta",
                repository = viewModel.repository,
                onDismiss = { showDeferDialog = false },
                onConfirm = { months, monthlyInterestRate ->
                    showDeferDialog = false
                    val targetExpense = editing.copy(
                        cardId = card?.id ?: editing.cardId,
                        amount = parsedAmount,
                        dateString = formattedDate,
                        establishment = currentEstablishment,
                        description = currentDescription,
                        receiptUri = currentReceipt
                    )
                    viewModel.deferExpense(targetExpense, months, monthlyInterestRate) {
                        onBack()
                    }
                }
            )
        }

        // Visor a Pantalla Completa con Zoom para comprobante en edición/nuevo
        if (viewingReceiptUri != null) {
            ReceiptViewerDialog(
                imageUri = viewingReceiptUri!!,
                title = establishment.ifBlank { "Comprobante de compra" },
                subtitle = "Toca dos veces o pellizca para hacer zoom",
                onDeleteReceipt = { deletedUri ->
                    ReceiptStorageUtil.deleteReceiptFile(deletedUri)
                    scannedReceiptUri = null
                    viewingReceiptUri = null
                    viewModel.showToast("✓ Foto del comprobante eliminada del dispositivo")
                },
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
    }
}
