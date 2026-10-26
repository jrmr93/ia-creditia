package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import com.example.util.GeminiService
import com.example.util.GeminiTestResult
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.CreditiaSurfaceContainerHighest
import com.example.ui.theme.CreditiaSurfaceContainerLow
import com.example.ui.theme.CreditiaSurfaceContainerLowest
import com.example.ui.theme.CreditiaTertiary
import com.example.ui.theme.CreditiaTertiaryContainer
import com.example.ui.viewmodel.CreditiaUiState
import com.example.ui.viewmodel.CreditiaViewModel

@Composable
fun AjustesScreen(
    viewModel: CreditiaViewModel,
    uiState: CreditiaUiState,
    modifier: Modifier = Modifier
) {
    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinFeedbackError by remember { mutableStateOf<String?>(null) }
    var geminiApiKeyInput by remember(uiState.geminiApiKey) { mutableStateOf(uiState.geminiApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isModelSelectorExpanded by remember { mutableStateOf(false) }
    var customModelInput by remember(uiState.geminiModel) { mutableStateOf(uiState.geminiModel) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        viewModel.exportFullBackup(outputStream)
                    }
                    withContext(Dispatchers.Main) {
                        viewModel.showToast("✓ Respaldo completo guardado exitosamente (tarjetas, consumos, configuración, comprobantes y logos)")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        viewModel.showToast("Error al exportar: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        viewModel.restoreFullBackup(inputStream)
                        withContext(Dispatchers.Main) {
                            viewModel.selectTab("consumos")
                        }
                    } ?: run {
                        withContext(Dispatchers.Main) {
                            viewModel.showToast("Error: No se pudo abrir el archivo seleccionado")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        viewModel.showToast("Error al restaurar: ${e.localizedMessage ?: "Formato inválido"}")
                    }
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.togglePaymentNotifications(true)
            viewModel.sendTestPaymentNotification()
        } else {
            viewModel.togglePaymentNotifications(false)
            viewModel.showToast("Permiso de notificaciones denegado")
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

            // Status Badge Hero Notification
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(CreditiaTertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "BÓVEDA CIFRADA ACTIVA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaTertiary,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Protección local AES-256 en reposo",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(CreditiaSurfaceContainerLowest)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CreditiaTertiary)
                                )
                                Text(
                                    text = "100% Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaTertiary
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 1: Seguridad y Acceso
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Seguridad y Acceso",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Autenticación local",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Switch Biometrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CreditiaSurfaceContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Autenticación Biométrica",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Desbloqueo rápido por Huella o Rostro",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.isBiometricsActive,
                                    onCheckedChange = { viewModel.toggleBiometrics(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CreditiaPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("switch_biometrics")
                                )
                            }

                            HorizontalDivider(color = CreditiaSurfaceContainerHigh)

                            // Change PIN Action Card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                imageVector = Icons.Default.Pin,
                                                contentDescription = null,
                                                tint = CreditiaPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "PIN de Acceso Maestro",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CreditiaSurfaceContainer)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Configurado",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Protege transacciones y consultas críticas cuando la biometría no esté disponible.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Button(
                                        onClick = {
                                            currentPinInput = ""
                                            newPinInput = ""
                                            confirmPinInput = ""
                                            pinFeedbackError = null
                                            showChangePinDialog = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("btn_change_pin"),
                                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Password,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Cambiar PIN de Acceso",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    if (uiState.isBiometricsActive) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { viewModel.lockApp() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp)
                                                .testTag("btn_lock_from_settings"),
                                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaSurfaceContainer),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = CreditiaPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Bloquear aplicación ahora",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = CreditiaPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = CreditiaSurfaceContainerHigh)

                            // Attempt Log & Auto Lock Policy
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            imageVector = Icons.Default.Policy,
                                            contentDescription = null,
                                            tint = CreditiaSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Política de Bloqueo Temporal",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = CreditiaTertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Activa",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CreditiaTertiary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainer)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "Intentos Fallidos",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${uiState.failedAttempts} / 5 permitidos",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Estado normal",
                                                fontSize = 11.sp,
                                                color = CreditiaTertiary
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainer)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "Pena por Bloqueo",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "5 minutos",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Exponencial tras 3 fallos",
                                                fontSize = 11.sp,
                                                color = CreditiaSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: Notificaciones y Recordatorios de Pago
            item {
                val activeCard = uiState.selectedCard ?: uiState.cards.firstOrNull()
                val upcomingSchedule = activeCard?.let { viewModel.getUpcomingAlertSchedule(it) }
                val paymentLimit = upcomingSchedule?.paymentLimitDateStr ?: viewModel.getEffectivePaymentLimitDateForPeriod(activeCard, viewModel.getCurrentActivePeriod(activeCard))
                val paymentLimitDesc = upcomingSchedule?.paymentLimitDesc ?: viewModel.getPaymentLimitDateDescription(activeCard, viewModel.getCurrentActivePeriod(activeCard))
                val alertDate = upcomingSchedule?.alertDateStr ?: viewModel.calculateNotificationDate(paymentLimit, uiState.notificationLeadDays)
                val alertDateDesc = upcomingSchedule?.alertDateDesc ?: alertDate
                val daysUntil = upcomingSchedule?.daysUntilAlert ?: 0
                val leadOptions = listOf(1, 2, 3, 5, 7)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Recordatorios de Pago",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (uiState.isPaymentNotificationEnabled) "Activas" else "Desactivadas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isPaymentNotificationEnabled) CreditiaTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Switch Notificaciones de Pago
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (uiState.isPaymentNotificationEnabled) CreditiaPrimary.copy(alpha = 0.12f) else CreditiaSurfaceContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = if (uiState.isPaymentNotificationEnabled) CreditiaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Aviso de Fecha Máxima de Pago",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Notificación previa para no olvidar de pagar",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.isPaymentNotificationEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                                    viewModel.togglePaymentNotifications(true)
                                                } else {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            } else {
                                                viewModel.togglePaymentNotifications(true)
                                            }
                                        } else {
                                            viewModel.togglePaymentNotifications(false)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CreditiaPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("switch_payment_notifications")
                                )
                            }

                            if (uiState.isPaymentNotificationEnabled) {
                                HorizontalDivider(color = CreditiaSurfaceContainerHigh)

                                // Selector de días de anticipación
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "¿Cuántos días antes deseas recibir la alerta?",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "Elige con qué anticipación deseas recibir la alerta en tu dispositivo:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Fila de chips de días
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        leadOptions.forEach { days ->
                                            val isSelected = uiState.notificationLeadDays == days
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) CreditiaPrimary else CreditiaSurfaceContainer
                                                    )
                                                    .clickable { viewModel.setNotificationLeadDays(days) }
                                                    .padding(vertical = 10.dp)
                                                    .testTag("chip_lead_days_$days"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "$days",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (days == 1) "día" else "días",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Tarjeta informativa con cálculo dinámico
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                                    border = BorderStroke(1.dp, CreditiaPrimary.copy(alpha = 0.25f)),
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
                                                    imageVector = Icons.Default.EventNote,
                                                    contentDescription = null,
                                                    tint = CreditiaPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Tarjeta: ${activeCard?.cardName ?: "Principal"}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = CreditiaTertiary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "PROGRAMADO",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CreditiaTertiary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Fecha máxima de pago:",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = paymentLimit,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (upcomingSchedule?.isOverwritten == true) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "Personalizada",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFD97706),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Fecha de alerta (${uiState.notificationLeadDays}d antes):",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = alertDate,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CreditiaPrimary
                                                )
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "🔔 Tu alerta llegará exactamente el $alertDateDesc a las 09:00 AM ($daysUntil días restantes, $paymentLimitDesc como fecha límite).",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 16.sp
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = CreditiaTertiary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Garantizado por AlarmManager con persistencia tras reinicio",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CreditiaTertiary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Botón para probar la notificación de inmediato
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.sendTestPaymentNotification(activeCard)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("btn_test_notification"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Probar Notificación Ahora",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: Inteligencia Artificial (Gemini AI)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Inteligencia Artificial (Gemini)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Lectura de comprobantes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Switch Activar / Desactivar Inteligencia Artificial
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (uiState.isGeminiAiEnabled) CreditiaPrimary.copy(alpha = 0.12f) else CreditiaSurfaceContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SmartToy,
                                            contentDescription = null,
                                            tint = if (uiState.isGeminiAiEnabled) CreditiaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Usar Inteligencia Artificial",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (uiState.isGeminiAiEnabled) "Gemini Cloud activo • Lectura avanzada de tickets" else "Desactivado • Se usará OCR 100% Offline",
                                            fontSize = 12.sp,
                                            color = if (uiState.isGeminiAiEnabled) CreditiaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.isGeminiAiEnabled,
                                    onCheckedChange = { viewModel.toggleGeminiAiEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CreditiaPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("switch_gemini_ai")
                                )
                            }

                            if (!uiState.isGeminiAiEnabled) {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                                    border = BorderStroke(1.dp, CreditiaTertiary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = CreditiaTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Modo Offline Activo: La app leerá tus comprobantes con el motor OCR local de Google en tu dispositivo, sin consumir internet ni cuotas de API.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            } else {
                                // Campo de API Key
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Gemini API Key (Google AI Studio):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                OutlinedTextField(
                                    value = geminiApiKeyInput,
                                    onValueChange = {
                                        geminiApiKeyInput = it
                                        viewModel.clearGeminiTestStatus()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_gemini_api_key"),
                                    placeholder = {
                                        Text(
                                            "Pega tu API Key (ej. AIzaSy...)",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    singleLine = true,
                                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                            Icon(
                                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isApiKeyVisible) "Ocultar clave" else "Mostrar clave",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Botones Guardar / Limpiar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.saveGeminiApiKey(geminiApiKeyInput)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .testTag("btn_save_gemini_key"),
                                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Save,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Guardar Clave",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    if (geminiApiKeyInput.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                geminiApiKeyInput = ""
                                                viewModel.saveGeminiApiKey("")
                                            },
                                            modifier = Modifier.height(40.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaSurfaceContainer),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "Borrar",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CreditiaError
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = CreditiaSurfaceContainerHigh)

                            // Selector de Modelo
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Modelo a utilizar:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Botón para detectar modelos de la cuenta
                                    TextButton(
                                        onClick = { viewModel.detectAvailableModels() },
                                        enabled = !uiState.isDetectingModels && uiState.geminiApiKey.isNotBlank()
                                    ) {
                                        if (uiState.isDetectingModels) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Detectando...", fontSize = 11.sp)
                                        } else {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = CreditiaPrimary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Detectar de mi cuenta", fontSize = 11.sp, color = CreditiaPrimary)
                                        }
                                    }
                                }

                                // Chips de modelos estándar y más compatibles
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val standardModels = listOf(
                                        "gemini-3.5-flash" to "Recomendado / Parki",
                                        "gemini-3.8-flash" to "Última versión 3.8",
                                        "gemini-3.5-flash-lite" to "Ultra rápido y ligero",
                                        "gemini-flash-latest" to "Alias auto-actualizado",
                                        "gemini-3.7-flash" to "Avanzado 3.7"
                                    )

                                    standardModels.chunked(2).forEach { rowModels ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowModels.forEach { (modelName, subtitle) ->
                                                val isSelected = uiState.geminiModel == modelName
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                            if (isSelected) CreditiaPrimaryContainer else CreditiaSurfaceContainer
                                                        )
                                                        .clickable {
                                                            viewModel.saveGeminiModel(modelName)
                                                            customModelInput = modelName
                                                        }
                                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = modelName,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = subtitle,
                                                            fontSize = 9.sp,
                                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            if (rowModels.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                // Si se detectaron modelos específicos de la API Key del usuario
                                if (uiState.availableModelOptions.isNotEmpty()) {
                                    Text(
                                        text = "Modelos detectados en tu cuenta:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CreditiaPrimary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        uiState.availableModelOptions.take(4).forEach { detectedModel ->
                                            val isSelected = uiState.geminiModel == detectedModel
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) CreditiaTertiaryContainer else CreditiaSurfaceContainerHigh)
                                                    .clickable {
                                                        viewModel.saveGeminiModel(detectedModel)
                                                        customModelInput = detectedModel
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = detectedModel,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Campo de texto para modelo manual o personalizado (como en parki)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "O escribe el nombre exacto de tu modelo (ej. como en tu app parki):",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = customModelInput,
                                            onValueChange = { customModelInput = it },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            placeholder = { Text("ej. gemini-1.5-flash", fontSize = 12.sp) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        Button(
                                            onClick = {
                                                if (customModelInput.isNotBlank()) {
                                                    viewModel.saveGeminiModel(customModelInput.trim())
                                                    viewModel.showToast("Modelo configurado a: ${customModelInput.trim()}")
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary)
                                        ) {
                                            Text("Aplicar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = CreditiaSurfaceContainerHigh)

                            // Test de Conexión y Modelo
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.testGeminiConnection()
                                    },
                                    enabled = !uiState.isTestingGemini && uiState.geminiApiKey.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("btn_test_gemini_connection"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CreditiaTertiary,
                                        disabledContainerColor = CreditiaSurfaceContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (uiState.isTestingGemini) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = "Probando conexión con ${uiState.geminiModel}...",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (uiState.geminiApiKey.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Probar Conexión con ${uiState.geminiModel}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (uiState.geminiApiKey.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (uiState.geminiApiKey.isBlank()) {
                                    Text(
                                        text = "ℹ️ Guarda tu API Key de Gemini para habilitar el test de conexión y el escaneo de tickets.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Resultado del test
                                when (val status = uiState.geminiTestStatus) {
                                    is GeminiTestResult.Success -> {
                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = CreditiaTertiaryContainer),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "¡Prueba Exitosa!",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = status.message,
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.9f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    is GeminiTestResult.Error -> {
                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = CreditiaErrorContainer),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Error,
                                                        contentDescription = null,
                                                        tint = CreditiaOnErrorContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = "Fallo en la conexión",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CreditiaOnErrorContainer
                                                    )
                                                }
                                                Text(
                                                    text = status.message,
                                                    fontSize = 11.sp,
                                                    color = CreditiaOnErrorContainer
                                                )

                                                if (uiState.geminiModel != "gemini-3.5-flash") {
                                                    Button(
                                                        onClick = {
                                                            viewModel.saveGeminiModel("gemini-3.5-flash")
                                                            customModelInput = "gemini-3.5-flash"
                                                            viewModel.testGeminiConnection()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaTertiary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Cambiar a 'gemini-3.5-flash' y probar ahora", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    null -> {}
                                }
                            }
                        }
                    }
                    }
                }
            }

            // SECTION 2: Almacenamiento y Bóveda
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = CreditiaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Almacenamiento y Bóveda",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "SQLite / Room",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CreditiaTertiary
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Room SQLite Local Summary Card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(CreditiaSecondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Storage,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Room SQLite Local",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(CreditiaTertiary)
                                                )
                                            }
                                            Text(
                                                text = "Cifrado estricto SQLCipher AES-256",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "2.4 MB",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CreditiaPrimary
                                        )
                                        Text(
                                            text = "En disco",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Action Item: Exportar Respaldo Local
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                        exportLauncher.launch("creditia_respaldo_$timestamp.zip")
                                    }
                                    .testTag("btn_export_backup")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Exportar Respaldo Local Completo (ZIP)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Guarda absolutamente toda la información: tarjetas, consumos, comprobantes, fotos de logos, fechas y configuración",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Action Item: Restaurar Respaldo Local
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        restoreLauncher.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "*/*"))
                                    }
                                    .testTag("btn_restore_backup")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SettingsBackupRestore,
                                            contentDescription = null,
                                            tint = CreditiaSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Restaurar Respaldo Local (ZIP o JSON)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Restaura absolutamente toda la información: tarjetas, consumos, comprobantes, fotos de logos, fechas y configuración",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Action Item: Limpiar Base de Datos Local
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = CreditiaErrorContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.openClearDb() }
                                    .testTag("btn_clear_db")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = CreditiaError,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Limpiar Base de Datos Local",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CreditiaError
                                            )
                                            Text(
                                                text = "Purga absoluta de tarjetas, consumos, comprobantes, fotos de logos, fechas y toda la información",
                                                fontSize = 11.sp,
                                                color = CreditiaOnErrorContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = CreditiaError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: System Identity & Offline-First Badge
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = CreditiaTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Creditia para Android v1.0.0",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Offline-First Architecture • Sin telemetría de nube",
                        fontSize = 11.sp,
                        color = CreditiaSecondary
                    )
                    Text(
                        text = "Build 2024.11.08-rc2 (ARM64-v8a)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Dialog: Cambiar PIN de Acceso
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = {
                Text(
                    text = "Cambiar PIN de Acceso",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Configuración de seguridad de 4 a 6 dígitos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 6) currentPinInput = it },
                        label = { Text("PIN Actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_current_pin"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6) newPinInput = it },
                        label = { Text("Nuevo PIN (4 a 6 dígitos)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_pin"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 6) confirmPinInput = it },
                        label = { Text("Confirmar Nuevo PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_confirm_pin"),
                        singleLine = true
                    )

                    if (pinFeedbackError != null) {
                        Text(
                            text = pinFeedbackError!!,
                            color = CreditiaError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.changeMasterPin(
                            currentPinInput,
                            newPinInput,
                            confirmPinInput
                        )
                        if (success) {
                            showChangePinDialog = false
                        } else {
                            pinFeedbackError = "Verifica los datos ingresados."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_save_new_pin")
                ) {
                    Text("Guardar PIN")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePinDialog = false },
                    modifier = Modifier.testTag("btn_cancel_change_pin")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog: Limpiar Base de Datos Local
    if (uiState.isClearDbOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeClearDb() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CreditiaErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CreditiaError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(text = "¿Limpiar base local?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Se eliminará absolutamente toda la información: todas las tarjetas de crédito, todos los consumos, cuotas diferidas, fotos de comprobantes, fotos de logos de establecimientos y fechas personalizadas de corte y pago. Esta acción no se puede deshacer.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = CreditiaTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Se recomienda exportar un respaldo primero.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmClearDb() },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaError),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_clear_db")
                ) {
                    Text(text = "Eliminar", color = CreditiaOnError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.closeClearDb() },
                    modifier = Modifier.testTag("btn_cancel_clear_db")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
