package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Check
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import com.example.ui.theme.CreditiaCardBlueStart
import com.example.ui.theme.CreditiaCardBlueMid
import com.example.ui.theme.CreditiaCardBlueEnd
import com.example.ui.theme.CreditiaVisaBlueStart
import com.example.ui.theme.CreditiaVisaBlueMid
import com.example.ui.theme.CreditiaVisaBlueEnd
import com.example.ui.theme.CreditiaCardBlackStart
import com.example.ui.theme.CreditiaCardBlackMid
import com.example.ui.theme.CreditiaCardBlackEnd
import com.example.ui.theme.CreditiaMasterBlackStart
import com.example.ui.theme.CreditiaMasterBlackMid
import com.example.ui.theme.CreditiaMasterBlackEnd
import com.example.ui.theme.CreditiaCardEmeraldStart
import com.example.ui.theme.CreditiaCardEmeraldMid
import com.example.ui.theme.CreditiaCardEmeraldEnd
import com.example.ui.theme.CreditiaCardPurpleStart
import com.example.ui.theme.CreditiaCardPurpleMid
import com.example.ui.theme.CreditiaCardPurpleEnd
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreditCardEntity
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.CreditiaCardBlueEnd
import com.example.ui.theme.CreditiaCardBlueMid
import com.example.ui.theme.CreditiaCardBlueStart
import com.example.ui.theme.CreditiaPrimary
import com.example.ui.theme.CreditiaPrimaryContainer
import com.example.ui.theme.CreditiaSurfaceContainer
import com.example.ui.theme.CreditiaSurfaceContainerHigh
import com.example.ui.theme.CreditiaSurfaceContainerLow
import com.example.ui.theme.CreditiaSurfaceContainerLowest
import com.example.ui.theme.CreditiaTertiary
import kotlinx.coroutines.delay

@Composable
fun MastercardLogo(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Box(
        modifier = modifier.size(width = size * 1.6f, height = size),
        contentAlignment = Alignment.CenterStart
    ) {
        // Red circle on left
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFEB001B))
        )
        // Orange / Golden Yellow circle overlapping on right
        Box(
            modifier = Modifier
                .offset(x = size * 0.58f)
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFF79E1B).copy(alpha = 0.95f))
        )
    }
}

@Composable
fun VisaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Text(
        text = "VISA",
        fontSize = (size.value * 0.85f).sp,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        color = Color.White,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

fun isMastercardNetwork(network: String, cardName: String = ""): Boolean {
    val net = network.trim().lowercase(java.util.Locale.ROOT)
    val name = cardName.trim().lowercase(java.util.Locale.ROOT)
    return net.contains("master") || net.contains("mc") ||
           name.contains("master") || name.contains("mc")
}

@Composable
fun CardBrandLogo(
    card: CreditCardEntity,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    CardNetworkLogo(
        network = card.network,
        modifier = modifier,
        size = size,
        cardName = card.cardName
    )
}

@Composable
fun CardNetworkLogo(
    network: String,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    cardName: String = ""
) {
    val isMc = isMastercardNetwork(network, cardName)
    if (isMc) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MastercardLogo(size = size)
            Text(
                text = "Mastercard",
                fontSize = (size.value * 0.55f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VisaLogo(modifier = modifier, size = size)
        }
    }
}

@Composable
fun CardMiniGraphic(
    card: CreditCardEntity,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isMc = isMastercardNetwork(card.network, card.cardName)
    val gradient = getCardGradient(card.cardColorTheme, card.network, card.tier, card.cardName)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, if (isMc) Color(0xFFFCD34D) else CreditiaPrimary) else null,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .let { if (onClick != null) it.clickable { onClick() } else it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = card.bankName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 0.8.sp
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (isMc) Color(0xFFFCD34D) else CreditiaPrimary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMc) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    CardNetworkLogo(
                        network = card.network,
                        size = 18.dp,
                        cardName = card.cardName
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
                                )
                            )
                    )

                    Text(
                        text = "•••• ${card.last4}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isMc) Color(0xFFFCD34D) else Color.White,
                        letterSpacing = 2.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.cardName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f)
                    )

                    Text(
                        text = "Corte: Día ${String.format(java.util.Locale.US, "%02d", card.cutDay)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMc) Color(0xFFFCD34D) else Color(0xFF6FFBBE)
                    )
                }
            }
        }
    }
}

fun getCardGradient(cardColorTheme: String = "", network: String = "", tier: String = "", cardName: String = ""): Brush {
    val theme = cardColorTheme.lowercase().trim()
    return when {
        theme == "black" -> {
            Brush.linearGradient(
                listOf(
                    Color(0xFF141416), // Deep midnight black
                    Color(0xFF23252B), // Obsidian slate
                    Color(0xFF33353D)  // Sleek graphite
                )
            )
        }
        theme == "platinum" || theme == "platinium" -> {
            Brush.linearGradient(
                listOf(
                    Color(0xFF4A4E5A), // Steel platinum dark
                    Color(0xFF7E8494), // Lustrous silver mid
                    Color(0xFFAAB2C4)  // Radiant metallic platinum
                )
            )
        }
        theme == "gold" -> {
            Brush.linearGradient(
                listOf(
                    Color(0xFF7A5400), // Deep rich bronze-gold
                    Color(0xFFB8860B), // Classic metallic gold
                    Color(0xFFE5A91A)  // Radiant amber gold
                )
            )
        }
        theme == "yellow" || theme == "yelow" -> {
            Brush.linearGradient(
                listOf(
                    Color(0xFF8A6500), // Warm golden base
                    Color(0xFFD49A00), // Vibrant amber yellow
                    Color(0xFFFFC72C)  // Luminous sunflower yellow
                )
            )
        }
        theme == "pink" -> {
            Brush.linearGradient(
                listOf(
                    Color(0xFF88124E), // Deep velvet rose
                    Color(0xFFB8286F), // Rich magenta pink
                    Color(0xFFE04A91)  // Radiant luxury rose
                )
            )
        }
        theme == "blue" -> {
            Brush.linearGradient(
                listOf(
                    Color(0xFF0D256C), // Deep classic Visa royal blue
                    Color(0xFF1434CB), // Iconic Visa blue mid
                    Color(0xFF0055D4)  // Radiant Visa blue end
                )
            )
        }
        tier.contains("BLACK", ignoreCase = true) || isMastercardNetwork(network, cardName) -> {
            Brush.linearGradient(
                listOf(
                    Color(0xFF141416),
                    Color(0xFF23252B),
                    Color(0xFF33353D)
                )
            )
        }
        else -> {
            Brush.linearGradient(
                listOf(
                    Color(0xFF0D256C),
                    Color(0xFF1434CB),
                    Color(0xFF0055D4)
                )
            )
        }
    }
}

data class CardTemplateInfo(
    val key: String,
    val name: String,
    val description: String,
    val primaryColor: Color
)

val AVAILABLE_CARD_TEMPLATES = listOf(
    CardTemplateInfo("black", "Black", "Obsidiana & Grafito", Color(0xFF141416)),
    CardTemplateInfo("platinium", "Platinium", "Plata & Platino", Color(0xFF6B7280)),
    CardTemplateInfo("gold", "Gold", "Oro Imperial", Color(0xFFB8860B)),
    CardTemplateInfo("blue", "Blue", "Azul Zafiro", Color(0xFF1434CB)),
    CardTemplateInfo("yelow", "Yellow", "Amarillo Solar", Color(0xFFEAB308)),
    CardTemplateInfo("pink", "Pink", "Rosa Velvet", Color(0xFFBE185D))
)

@Composable
fun CreditiaHeader(
    title: String,
    subtitle: String? = null,
    onSearchClick: () -> Unit,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Main branding header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Creditia Vector Mini Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CreditiaCardBlueStart, CreditiaPrimary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "Creditia Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "CREDITIA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CreditiaPrimary,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!subtitle.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CreditiaPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreditiaPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Búsqueda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreditiaBottomNav(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CreditiaSurfaceContainerLowest.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("consumos", "Consumos", Icons.Default.ReceiptLong),
                Triple("tarjetas", "Tarjetas", Icons.Default.CreditCard),
                Triple("ajustes", "Ajustes", Icons.Default.Settings)
            )

            tabs.forEach { (route, label, icon) ->
                val isSelected = currentTab == route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(route) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("nav_tab_$route")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) CreditiaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) CreditiaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CreditiaFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .testTag("fab_registrar"),
        shape = RoundedCornerShape(16.dp),
        containerColor = CreditiaPrimaryContainer,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Registrar",
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Registrar",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun OfflineStatusFootnote(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(CreditiaSurfaceContainerLow)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = CreditiaTertiary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Base de datos local cifrada SQLite • 100% Offline y Descentralizada",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ToastNotification(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (message != null) {
            LaunchedEffect(message) {
                delay(2600)
                onDismiss()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp, start = 20.dp, end = 20.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF213145),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF6FFBBE),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = message,
                            color = Color(0xFFEAF1FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleConfigBottomSheet(
    card: CreditCardEntity,
    onDismiss: () -> Unit,
    onSave: (cutDay: Int, payDay: Int, payBusinessDays: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var cutDay by remember { mutableFloatStateOf(card.cutDay.toFloat()) }
    var payBusinessDays by remember { mutableFloatStateOf(card.payBusinessDays.toFloat()) }
    var showConfirmCycleDialog by remember { mutableStateOf(false) }

    val (sampleDate, computedPayDay) = remember(cutDay, payBusinessDays) {
        calculateMaxPaymentDate(cutDay.toInt(), payBusinessDays.toInt(), 2026, Calendar.NOVEMBER)
    }

    val dayNames = listOf("", "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    val monthNames = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    val sampleDayName = dayNames.getOrElse(sampleDate.get(Calendar.DAY_OF_WEEK)) { "" }
    val sampleMonthName = monthNames.getOrElse(sampleDate.get(Calendar.MONTH)) { "" }
    val sampleDayOfMonth = sampleDate.get(Calendar.DAY_OF_MONTH)
    val sampleYear = sampleDate.get(Calendar.YEAR)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CreditiaSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Configurar Fechas y Plazos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${card.cardName} (•••• ${card.last4})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Cut day slider
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow)
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
                        Text(
                            text = "Día de Corte Mensual",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Día ${cutDay.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CreditiaPrimary
                        )
                    }
                    Slider(
                        value = cutDay,
                        onValueChange = { cutDay = it },
                        valueRange = 1f..31f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = CreditiaPrimary,
                            activeTrackColor = CreditiaPrimary
                        ),
                        modifier = Modifier.testTag("slider_cut_day")
                    )
                    val nextDay = if (cutDay.toInt() == 31) 1 else cutDay.toInt() + 1
                    Text(
                        text = "El ciclo de consumos corta el ${cutDay.toInt()} de cada mes e inicia el $nextDay.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Business days for payment slider
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow)
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
                        Text(
                            text = "Plazo de Pago (Días Hábiles)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${payBusinessDays.toInt()} días hábiles",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CreditiaTertiary
                        )
                    }
                    Slider(
                        value = payBusinessDays,
                        onValueChange = { payBusinessDays = it },
                        valueRange = 1f..30f,
                        steps = 28,
                        colors = SliderDefaults.colors(
                            thumbColor = CreditiaTertiary,
                            activeTrackColor = CreditiaTertiary
                        ),
                        modifier = Modifier.testTag("slider_pay_business_days")
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Fecha máxima calculada: Día $computedPayDay ($sampleDayName, $sampleDayOfMonth de $sampleMonthName de $sampleYear)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreditiaTertiary
                            )
                            Text(
                                text = "Calculado sumando ${payBusinessDays.toInt()} días hábiles (sin contar sábados ni domingos) tras la fecha superior de corte del periodo.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_cancel_cycle"),
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaSurfaceContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancelar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = { showConfirmCycleDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_save_cycle"),
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Guardar Cambios",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showConfirmCycleDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCycleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.EventRepeat,
                    contentDescription = null,
                    tint = CreditiaPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "¿Guardar cambios en el ciclo?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "¿Deseas actualizar el día de corte a ${cutDay.toInt()} y el plazo a ${payBusinessDays.toInt()} días hábiles de pago para la tarjeta \"${card.cardName}\"?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCycleDialog = false
                        onSave(cutDay.toInt(), computedPayDay, payBusinessDays.toInt())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_cycle_dialog")
                ) {
                    Text("Confirmar y Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmCycleDialog = false },
                    modifier = Modifier.testTag("btn_cancel_cycle_dialog")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Counts business days (Mon..Fri) excluding Sat and Sun starting after the cutDay
 * of the period's upper cut date ("fecha superior del corte del periodo actual").
 * Example: If cutDay of Nov 2026 is 24 Nov 2026, counting 12 business days (without Sat/Sun)
 * reaches 10 Dec 2026.
 */
fun calculateMaxPaymentDate(
    cutDay: Int,
    businessDays: Int,
    referenceYear: Int = 2026,
    referenceMonth: Int = Calendar.NOVEMBER // Nov = 10
): Pair<Calendar, Int> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, referenceYear)
    cal.set(Calendar.MONTH, referenceMonth)
    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, cutDay.coerceIn(1, maxDay))

    var remaining = businessDays.coerceIn(1, 30)
    while (remaining > 0) {
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
            remaining--
        }
    }
    return Pair(cal, cal.get(Calendar.DAY_OF_MONTH))
}

@Composable
fun CardFormDialog(
    cardToEdit: CreditCardEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        bank: String,
        cardName: String,
        network: String,
        tier: String,
        last4: String,
        cutDay: Int,
        payBusinessDays: Int,
        payDay: Int,
        creditLimit: Double,
        style: String
    ) -> Unit
) {
    val isEdit = cardToEdit != null

    var bankName by remember(cardToEdit) { mutableStateOf(cardToEdit?.bankName ?: "") }
    var cardName by remember(cardToEdit) { mutableStateOf(cardToEdit?.cardName ?: "") }
    var network by remember(cardToEdit) { mutableStateOf(cardToEdit?.network ?: "VISA") }
    var tier by remember(cardToEdit) { mutableStateOf(cardToEdit?.tier ?: "SIGNATURE") }
    var last4 by remember(cardToEdit) { mutableStateOf(cardToEdit?.last4 ?: "") }
    var cutDay by remember(cardToEdit) { mutableFloatStateOf(cardToEdit?.cutDay?.toFloat() ?: 24f) }
    var payBusinessDays by remember(cardToEdit) { mutableFloatStateOf(cardToEdit?.payBusinessDays?.toFloat() ?: 12f) }
    var creditLimitText by remember(cardToEdit) { mutableStateOf((cardToEdit?.creditLimit ?: 5000.0).toString()) }
    var selectedStyle by remember(cardToEdit) {
        val initialStyle = cardToEdit?.cardColorTheme?.lowercase()?.trim()
        mutableStateOf(
            if (!initialStyle.isNullOrBlank()) initialStyle
            else if (network == "VISA") "blue" else "black"
        )
    }
    var showConfirmEditCardDialog by remember { mutableStateOf(false) }

    val (sampleDate, computedPayDay) = remember(cutDay, payBusinessDays) {
        calculateMaxPaymentDate(cutDay.toInt(), payBusinessDays.toInt(), 2026, Calendar.NOVEMBER)
    }

    val dayNames = listOf("", "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    val monthNames = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    val sampleDayName = dayNames.getOrElse(sampleDate.get(Calendar.DAY_OF_WEEK)) { "" }
    val sampleMonthName = monthNames.getOrElse(sampleDate.get(Calendar.MONTH)) { "" }
    val sampleDayOfMonth = sampleDate.get(Calendar.DAY_OF_MONTH)
    val sampleYear = sampleDate.get(Calendar.YEAR)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Editar Tarjeta de Crédito" else "Nueva Tarjeta de Crédito",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vista previa visual interactiva de la tarjeta
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(getCardGradient(selectedStyle, network, tier, cardName))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bankName.ifBlank { "BANCO EMISOR" }.uppercase(Locale.ROOT),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                letterSpacing = 0.8.sp
                            )
                            CardNetworkLogo(network = network, size = 18.dp, cardName = cardName)
                        }
                        Text(
                            text = cardName.ifBlank { if (network == "VISA") "Visa $tier" else "Mastercard $tier" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "•••• ${last4.ifBlank { "0000" }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Plantilla: ${selectedStyle.replaceFirstChar { it.uppercase() }}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Selector de Plantilla Gráfica de Tarjeta (black, platinium, gold, blue, yelow, pink)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Plantilla Gráfica (Color y Estilo)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AVAILABLE_CARD_TEMPLATES.forEach { template ->
                            val isSelected = selectedStyle.equals(template.key, ignoreCase = true) ||
                                (template.key == "platinium" && selectedStyle.equals("platinum", ignoreCase = true)) ||
                                (template.key == "yelow" && selectedStyle.equals("yellow", ignoreCase = true))

                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(getCardGradient(template.key))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedStyle = template.key }
                                    .padding(7.dp)
                                    .testTag("template_option_${template.key}")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = template.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = template.description,
                                        fontSize = 8.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                // Selector de Red / Franquicia con botones simétricos y proporcionados
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Franquicia / Red",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Botón VISA
                        val isVisa = (network == "VISA")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isVisa) CreditiaVisaBlueMid else CreditiaSurfaceContainerLow)
                                .border(
                                    width = if (isVisa) 2.dp else 1.dp,
                                    color = if (isVisa) CreditiaVisaBlueStart else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    network = "VISA"
                                    if (tier.contains("BLACK", ignoreCase = true)) tier = ""
                                    if (cardName.isBlank() || cardName.contains("Mastercard", ignoreCase = true)) {
                                        cardName = if (tier.isNotBlank()) "Visa $tier" else "Visa"
                                    }
                                }
                                .testTag("btn_select_network_visa"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "VISA",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                letterSpacing = 1.sp,
                                color = if (isVisa) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Botón MASTERCARD
                        val isMaster = (network == "MASTERCARD")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMaster) CreditiaMasterBlackMid else CreditiaSurfaceContainerLow)
                                .border(
                                    width = if (isMaster) 2.dp else 1.dp,
                                    color = if (isMaster) Color(0xFFFCD34D) else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    network = "MASTERCARD"
                                    if (tier.equals("BLACK EDITION", ignoreCase = true)) tier = ""
                                    if (cardName.isBlank() || cardName.contains("Visa", ignoreCase = true)) {
                                        cardName = "Mastercard"
                                    }
                                }
                                .testTag("btn_select_network_mastercard"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MastercardLogo(size = 18.dp)
                                Text(
                                    text = "Mastercard",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMaster) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Banco Emisor
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Banco emisor") },
                    placeholder = { Text("Ej. Banco Pichincha") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bank_name"),
                    singleLine = true
                )

                // Nombre / Alias de la Tarjeta
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Nombre o alias de la tarjeta") },
                    placeholder = { Text("Ej. Visa Signature Corporativa") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_card_name"),
                    singleLine = true
                )

                // Últimos 4 dígitos y Categoría
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) last4 = it },
                        label = { Text("Últimos 4 dígitos") },
                        placeholder = { Text("4892") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_last4"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tier,
                        onValueChange = { tier = it.uppercase(Locale.US) },
                        label = { Text("Categoría / Nivel") },
                        placeholder = { Text("SIGNATURE") },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("input_tier"),
                        singleLine = true
                    )
                }

                // Límite de Crédito
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    label = { Text("Límite de crédito ($)") },
                    placeholder = { Text("5000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_credit_limit"),
                    singleLine = true
                )

                // 1. Selector Día de Corte Mensual (1 a 31)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow)
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
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = null,
                                    tint = CreditiaPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Día de Corte Mensual",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CreditiaPrimary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Día ${cutDay.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (cutDay > 1f) cutDay -= 1f },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                            }
                            Slider(
                                value = cutDay,
                                onValueChange = { cutDay = it },
                                valueRange = 1f..31f,
                                steps = 29,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("slider_cut_day"),
                                colors = SliderDefaults.colors(
                                    thumbColor = CreditiaPrimary,
                                    activeTrackColor = CreditiaPrimary
                                )
                            )
                            IconButton(
                                onClick = { if (cutDay < 31f) cutDay += 1f },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = "Día del mes en que concluye el ciclo de compras.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2. Selector Días Hábiles para Fecha Máxima de Pago (sin sábados ni domingos)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditiaSurfaceContainerLow)
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
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = CreditiaTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Días Hábiles para Pago",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CreditiaTertiary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${payBusinessDays.toInt()} días hábiles",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (payBusinessDays > 1f) payBusinessDays -= 1f },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                            }
                            Slider(
                                value = payBusinessDays,
                                onValueChange = { payBusinessDays = it },
                                valueRange = 1f..30f,
                                steps = 28,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("slider_pay_business_days"),
                                colors = SliderDefaults.colors(
                                    thumbColor = CreditiaTertiary,
                                    activeTrackColor = CreditiaTertiary
                                )
                            )
                            IconButton(
                                onClick = { if (payBusinessDays < 30f) payBusinessDays += 1f },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Cálculo dinámico transparente
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CreditiaTertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Fecha máxima de pago calculada: Día $computedPayDay",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CreditiaTertiary
                                    )
                                }
                                Text(
                                    text = "Se toma la fecha superior de corte del periodo (día ${cutDay.toInt()}) y se suman ${payBusinessDays.toInt()} días hábiles (sin contar sábados ni domingos).",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = "Ejemplo: Si la fecha superior de corte es el ${cutDay.toInt()} de noviembre de 2026, la fecha máxima de pago es: $sampleDayName, $sampleDayOfMonth de $sampleMonthName de $sampleYear (Día $computedPayDay).",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
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
                    if (isEdit) {
                        showConfirmEditCardDialog = true
                    } else {
                        onConfirm(
                            bankName,
                            cardName,
                            network,
                            tier,
                            last4,
                            cutDay.toInt(),
                            payBusinessDays.toInt(),
                            computedPayDay,
                            creditLimitText.toDoubleOrNull() ?: 5000.0,
                            selectedStyle
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_card_form")
            ) {
                Text(if (isEdit) "Guardar Cambios" else "Crear Tarjeta")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_card_form")
            ) {
                Text("Cancelar")
            }
        }
    )

    // Modal Confirmar Modificación de Tarjeta
    if (showConfirmEditCardDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEditCardDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = CreditiaPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "¿Guardar cambios en la tarjeta?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas guardar las modificaciones realizadas en la tarjeta \"${cardName.ifBlank { "Tarjeta" }}\" (${network} •••• ${last4.ifBlank { "0000" }}) con plantilla \"${selectedStyle.replaceFirstChar { it.uppercase() }}\"?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmEditCardDialog = false
                        onConfirm(
                            bankName,
                            cardName,
                            network,
                            tier,
                            last4,
                            cutDay.toInt(),
                            payBusinessDays.toInt(),
                            computedPayDay,
                            creditLimitText.toDoubleOrNull() ?: 5000.0,
                            selectedStyle
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CreditiaPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_save_card_dialog")
                ) {
                    Text("Confirmar y Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmEditCardDialog = false },
                    modifier = Modifier.testTag("btn_cancel_save_card_dialog")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (bank: String, cardName: String, network: String, tier: String, last4: String, cutDay: Int, payDay: Int, style: String) -> Unit
) {
    CardFormDialog(
        cardToEdit = null,
        onDismiss = onDismiss,
        onConfirm = { bank, cardName, network, tier, last4, cutDay, _, payDay, _, style ->
            onConfirm(bank, cardName, network, tier, last4, cutDay, payDay, style)
        }
    )
}
