package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CreditiaError
import com.example.ui.theme.CreditiaPrimary
import com.example.ui.theme.CreditiaPrimaryContainer
import com.example.ui.theme.CreditiaSecondary
import com.example.ui.theme.CreditiaSurfaceContainer
import com.example.ui.theme.CreditiaSurfaceContainerHigh
import com.example.ui.theme.CreditiaSurfaceContainerHighest
import com.example.ui.theme.CreditiaSurfaceContainerLow
import com.example.ui.theme.CreditiaSurfaceContainerLowest
import com.example.ui.theme.CreditiaTertiary
import com.example.ui.theme.CreditiaTertiaryContainer
import com.example.ui.theme.CreditiaTertiaryFixed
import com.example.ui.viewmodel.CreditiaUiState
import com.example.ui.viewmodel.CreditiaViewModel
import com.example.util.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SeguridadScreen(
    viewModel: CreditiaViewModel,
    uiState: CreditiaUiState,
    isLockScreen: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // 1. First Launch Setup State
    val isFirstTime = uiState.isFirstLaunch || uiState.masterPin.isEmpty()

    var initialPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var enableBiometricsChoice by remember { mutableStateOf(BiometricHelper.isBiometricHardwareAvailable(context)) }
    var setupErrorMessage by remember { mutableStateOf<String?>(null) }

    // 2. Regular Lock Screen State
    var showPinPad by remember { mutableStateOf(!uiState.isBiometricsActive) }
    var enteredPin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var sensorStatusText by remember {
        mutableStateOf(
            if (uiState.isBiometricsActive) "Toca para abrir la autenticación por huella del sistema" else "Ingresa tu PIN"
        )
    }

    // Pulse animation for biometric ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Trigger native OS biometric prompt on entry if biometrics is enabled and not first launch
    LaunchedEffect(uiState.isBiometricsActive, isFirstTime) {
        if (!isFirstTime && uiState.isBiometricsActive && activity != null) {
            delay(350)
            BiometricHelper.showNativeBiometricPrompt(
                activity = activity,
                title = "Creditia - Acceso Seguro",
                subtitle = "Usa tu huella digital para desbloquear",
                onSuccess = {
                    sensorStatusText = "✓ Identidad confirmada"
                    viewModel.unlockVault()
                },
                onError = { err ->
                    sensorStatusText = err
                    showPinPad = true
                },
                onUsePin = {
                    showPinPad = true
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }

            // 1. Branding Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CreditiaPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(CreditiaTertiaryFixed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = Color(0xFF002113),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Text(
                        text = "CREDITIA",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = CreditiaPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isFirstTime) "CONFIGURACIÓN INICIAL" else "BÓVEDA CIFRADA LOCAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CreditiaSecondary,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            if (isFirstTime) {
                // ==========================================
                // REQUISITO 1: CONFIGURACIÓN INICIAL OBLIGATORIA (SÓLO 1RA VEZ)
                // ==========================================
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("initial_setup_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(CreditiaPrimary.copy(alpha = 0.1f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "PRIMER INICIO • OBLIGATORIO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CreditiaPrimary,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            Text(
                                text = "Configura tu PIN de Acceso",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Para proteger tu información financiera local, es obligatorio registrar un PIN de 4 a 6 dígitos numéricos. En tus próximos inicios podrás ingresar con este PIN o tu huella.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = initialPin,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                        initialPin = it
                                        setupErrorMessage = null
                                    }
                                },
                                label = { Text("Nuevo PIN de Seguridad (4 - 6 dígitos)") },
                                placeholder = { Text("••••") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_initial_pin"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                        confirmPin = it
                                        setupErrorMessage = null
                                    }
                                },
                                label = { Text("Confirmar PIN de Seguridad") },
                                placeholder = { Text("••••") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_confirm_pin"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Biometrics Option for First Launch
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow),
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Habilitar Huella Digital",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Desbloquea con huella en próximos inicios",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    androidx.compose.material3.Switch(
                                        checked = enableBiometricsChoice,
                                        onCheckedChange = { enableBiometricsChoice = it },
                                        modifier = Modifier.testTag("switch_initial_biometrics")
                                    )
                                }
                            }

                            if (setupErrorMessage != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = CreditiaError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = setupErrorMessage ?: "",
                                        fontSize = 12.sp,
                                        color = CreditiaError,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (initialPin.length < 4) {
                                        setupErrorMessage = "El PIN debe tener al menos 4 dígitos numéricos."
                                    } else if (initialPin != confirmPin) {
                                        setupErrorMessage = "Los PINs ingresados no coinciden."
                                    } else {
                                        val ok = viewModel.finishInitialSetup(initialPin, enableBiometricsChoice)
                                        if (!ok) {
                                            setupErrorMessage = "El PIN debe tener entre 4 y 6 dígitos numéricos."
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_save_initial_pin"),
                                colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Guardar PIN y Comenzar",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // REQUISITO 1 & 2: INICIO NORMAL (PIN O HUELLA NATIVA)
                // NO SE MUESTRA LA OPCIÓN DE CONFIGURACIÓN INICIAL
                // ==========================================
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_container_card")
                    ) {
                        if (uiState.isBiometricsActive && !showPinPad) {
                            // Pantalla Huella Digital Nativa
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(CreditiaSurfaceContainer)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "AUTENTICACIÓN BIOMÉTRICA NATIVA",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "Desbloquear Creditia",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Usa el lector nativo de huella de tu sistema operativo para identificarte.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                // Sensor touch trigger that calls native Android OS BiometricPrompt
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(130.dp)
                                        .padding(vertical = 10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .scale(pulseScale)
                                            .clip(CircleShape)
                                            .background(CreditiaPrimary.copy(alpha = 0.15f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .background(CreditiaSurfaceContainerHigh)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(CreditiaPrimary)
                                            .clickable {
                                                if (activity != null) {
                                                    BiometricHelper.showNativeBiometricPrompt(
                                                        activity = activity,
                                                        title = "Creditia",
                                                        subtitle = "Confirma tu huella digital",
                                                        onSuccess = {
                                                            sensorStatusText = "✓ Identidad confirmada"
                                                            viewModel.unlockVault()
                                                        },
                                                        onError = { err ->
                                                            sensorStatusText = err
                                                            showPinPad = true
                                                        },
                                                        onUsePin = {
                                                            showPinPad = true
                                                        }
                                                    )
                                                }
                                            }
                                            .testTag("btn_fingerprint_sensor"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Autenticar con huella nativa",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = sensorStatusText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CreditiaPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        showPinPad = true
                                        enteredPin = ""
                                        pinMessage = null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_switch_to_pin"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaSurfaceContainer),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Dialpad,
                                            contentDescription = null,
                                            tint = CreditiaPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Ingresar con PIN de seguridad",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CreditiaPrimary
                                        )
                                    }
                                }
                            }
                        } else {
                            // Modo Teclado PIN
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState.isBiometricsActive) {
                                        Button(
                                            onClick = {
                                                showPinPad = false
                                                enteredPin = ""
                                                pinMessage = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CreditiaSurfaceContainer),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = null,
                                                    tint = CreditiaPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Usar Huella",
                                                    fontSize = 11.sp,
                                                    color = CreditiaPrimary
                                                )
                                            }
                                        }
                                    } else {
                                        Box(modifier = Modifier.size(1.dp))
                                    }

                                    Text(
                                        text = "PIN de Acceso",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Box(modifier = Modifier.size(36.dp))
                                }

                                Text(
                                    text = "Introduce tu PIN de seguridad",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Dots indicator (accommodates 4 to 6 digits)
                                val pinLengthTarget = maxOf(4, minOf(6, uiState.masterPin.length.coerceAtLeast(4)))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    for (i in 0 until pinLengthTarget) {
                                        val isFilled = i < enteredPin.length
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isFilled) CreditiaPrimary else CreditiaSurfaceContainerHighest
                                                )
                                        )
                                    }
                                }

                                // Message Feedback
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (pinMessage != null) {
                                        val (msg, isSuccess) = pinMessage!!
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                                contentDescription = null,
                                                tint = if (isSuccess) CreditiaTertiary else CreditiaError,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = msg,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSuccess) CreditiaTertiary else CreditiaError
                                            )
                                        }
                                    }
                                }

                                // Keypad
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                ) {
                                    val keys = listOf(
                                        listOf("1", "2", "3"),
                                        listOf("4", "5", "6"),
                                        listOf("7", "8", "9"),
                                        listOf("C", "0", "DEL")
                                    )

                                    keys.forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            row.forEach { digit ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(52.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                            if (digit == "C" || digit == "DEL") CreditiaSurfaceContainer else CreditiaSurfaceContainerLow
                                                        )
                                                        .clickable {
                                                            when (digit) {
                                                                "C" -> {
                                                                    enteredPin = ""
                                                                    pinMessage = null
                                                                }
                                                                "DEL" -> {
                                                                    if (enteredPin.isNotEmpty()) {
                                                                        enteredPin = enteredPin.dropLast(1)
                                                                        pinMessage = null
                                                                    }
                                                                }
                                                                else -> {
                                                                    if (enteredPin.length < 6) {
                                                                        val newPin = enteredPin + digit
                                                                        enteredPin = newPin
                                                                        pinMessage = null

                                                                        // Check if pin matches masterPin
                                                                        if (newPin == uiState.masterPin) {
                                                                            pinMessage = Pair("PIN correcto. Desbloqueando...", true)
                                                                            coroutineScope.launch {
                                                                                delay(300)
                                                                                viewModel.verifyPin(newPin)
                                                                            }
                                                                        } else if (newPin.length >= 6) {
                                                                            pinMessage = Pair("PIN incorrecto", false)
                                                                            coroutineScope.launch {
                                                                                delay(1000)
                                                                                enteredPin = ""
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .testTag("pin_key_$digit"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (digit == "DEL") {
                                                        Icon(
                                                            imageVector = Icons.Default.Backspace,
                                                            contentDescription = "Borrar",
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = digit,
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Manual Confirm Button (in case PIN is 4 or 5 digits)
                                if (enteredPin.length in 4..6) {
                                    Button(
                                        onClick = {
                                            val ok = viewModel.verifyPin(enteredPin)
                                            if (!ok) {
                                                pinMessage = Pair("PIN incorrecto", false)
                                                coroutineScope.launch {
                                                    delay(1000)
                                                    enteredPin = ""
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth(0.92f)
                                            .height(44.dp)
                                            .testTag("btn_verify_pin")
                                    ) {
                                        Text("Entrar con PIN", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Keystore Guarantee Seal
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CreditiaTertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Android Keystore & Biometrics",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = CreditiaTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Autenticación 100% nativa y local. Tus credenciales nunca salen de tu dispositivo.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
