package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RaixActionPrimary,
    onPrimary = RaixBackground,
    primaryContainer = RaixSurfaceElevated,
    onPrimaryContainer = RaixTextPrimary,
    secondary = RaixActionPrimary,
    onSecondary = RaixBackground,
    secondaryContainer = RaixSurfaceElevated,
    onSecondaryContainer = RaixActionPrimary,
    tertiary = RaixAccentPremium,
    onTertiary = RaixBackground,
    tertiaryContainer = Color(0xFF2D2510),
    onTertiaryContainer = RaixAccentPremium,
    error = RaixError,
    onError = Color.White,
    errorContainer = RaixErrorContainer,
    onErrorContainer = RaixError,
    background = RaixBackground,
    onBackground = RaixTextPrimary,
    surface = RaixSurface,
    onSurface = RaixTextPrimary,
    surfaceVariant = RaixSurface,
    onSurfaceVariant = RaixTextSecondary,
    outline = RaixBorder,
    outlineVariant = Color(0xFF1E2432)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = getRaixTypography(), content = content)
}
