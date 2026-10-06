package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = CreditiaPrimaryContainer,
    onPrimaryContainer = CreditiaOnPrimaryContainer,
    secondary = Color(0xFFBEC6E0),
    onSecondary = Color(0xFF283044),
    secondaryContainer = Color(0xFF3F465C),
    onSecondaryContainer = Color(0xFFDAE2FD),
    tertiary = CreditiaTertiaryFixed,
    onTertiary = Color(0xFF003824),
    tertiaryContainer = CreditiaTertiaryContainer,
    onTertiaryContainer = CreditiaOnTertiaryContainer,
    background = Color(0xFF10141D),
    onBackground = Color(0xFFE0E2EC),
    surface = Color(0xFF10141D),
    onSurface = Color(0xFFE0E2EC),
    surfaceVariant = Color(0xFF434655),
    onSurfaceVariant = Color(0xFFC3C6D7),
    outline = CreditiaOutline,
    outlineVariant = Color(0xFF434655)
)

private val LightColorScheme = lightColorScheme(
    primary = CreditiaPrimary,
    onPrimary = CreditiaOnPrimary,
    primaryContainer = CreditiaPrimaryContainer,
    onPrimaryContainer = CreditiaOnPrimaryContainer,
    secondary = CreditiaSecondary,
    onSecondary = CreditiaOnSecondary,
    secondaryContainer = CreditiaSecondaryContainer,
    onSecondaryContainer = CreditiaOnSecondaryContainer,
    tertiary = CreditiaTertiary,
    onTertiary = CreditiaOnTertiary,
    tertiaryContainer = CreditiaTertiaryContainer,
    onTertiaryContainer = CreditiaOnTertiaryContainer,
    error = CreditiaError,
    onError = CreditiaOnError,
    errorContainer = CreditiaErrorContainer,
    onErrorContainer = CreditiaOnErrorContainer,
    background = CreditiaBackground,
    onBackground = CreditiaOnBackground,
    surface = CreditiaSurface,
    onSurface = CreditiaOnSurface,
    surfaceVariant = CreditiaSurfaceVariant,
    onSurfaceVariant = CreditiaOnSurfaceVariant,
    outline = CreditiaOutline,
    outlineVariant = CreditiaOutlineVariant
)

@Composable
fun CreditiaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always force light mode as required by user
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
