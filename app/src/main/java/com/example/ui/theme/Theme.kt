package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColorScheme = lightColorScheme(
    primary = LightBullishGreen,
    onPrimary = LightSurface,
    primaryContainer = LightBullishGreenDark,
    onPrimaryContainer = LightBullishGreen,
    secondary = LightCyanRS,
    onSecondary = LightSurface,
    secondaryContainer = LightCyanRSDark,
    onSecondaryContainer = LightCyanRS,
    tertiary = LightAmberGold,
    onTertiary = LightSurface,
    tertiaryContainer = LightAmberDark,
    onTertiaryContainer = LightAmberWarning,
    error = LightBearishRed,
    onError = LightSurface,
    errorContainer = LightBearishRedDark,
    onErrorContainer = LightBearishRed,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBullishGreen,
    onPrimary = DarkBackground,
    primaryContainer = DarkBullishGreenDark,
    onPrimaryContainer = DarkBullishGreenGlow,
    secondary = DarkCyanRS,
    onSecondary = DarkBackground,
    secondaryContainer = DarkCyanRSDark,
    onSecondaryContainer = DarkCyanRS,
    tertiary = DarkAmberGold,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkAmberWarning,
    onTertiaryContainer = DarkAmberGold,
    error = DarkBearishRed,
    onError = DarkTextPrimary,
    errorContainer = DarkBearishRedDark,
    onErrorContainer = DarkBearishRedGlow,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

@Composable
fun MarketEdgeTheme(
    darkTheme: Boolean = false, // Defaults to Light theme as requested
    content: @Composable () -> Unit
) {
    val terminalColors = if (darkTheme) DarkTerminalColors else LightTerminalColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    updateGlobalColors(terminalColors)

    CompositionLocalProvider(LocalTerminalColors provides terminalColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
