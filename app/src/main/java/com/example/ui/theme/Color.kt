package com.example.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Terminal Color Palette (Clean Modern Financial Terminal)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFF1F5F9)
val LightCardElevated = Color(0xFFE2E8F0)
val LightBorder = Color(0xFFE2E8F0)
val LightDivider = Color(0xFFE2E8F0)

val LightBullishGreen = Color(0xFF059669)
val LightBullishGreenDark = Color(0xFFECFDF5)
val LightBullishGreenGlow = Color(0xFF10B981)

val LightBearishRed = Color(0xFFDC2626)
val LightBearishRedDark = Color(0xFFFEF2F2)
val LightBearishRedGlow = Color(0xFFEF4444)

val LightCyanRS = Color(0xFF0284C7)
val LightCyanRSDark = Color(0xFFE0F2FE)
val LightAmberWarning = Color(0xFFD97706)
val LightAmberGold = Color(0xFFB45309)
val LightAmberDark = Color(0xFFFFFBEB)
val LightPurpleMA = Color(0xFF7C3AED)

val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextTertiary = Color(0xFF94A3B8)

// Dark Terminal Color Palette (Obsidian / Bloomberg Terminal Dark)
val DarkBackground = Color(0xFF080C14)
val DarkSurface = Color(0xFF0F172A)
val DarkCard = Color(0xFF131D31)
val DarkCardElevated = Color(0xFF1E293B)
val DarkBorder = Color(0xFF243247)
val DarkDivider = Color(0xFF1E293B)

val DarkBullishGreen = Color(0xFF10B981)
val DarkBullishGreenDark = Color(0xFF065F46)
val DarkBullishGreenGlow = Color(0xFF34D399)

val DarkBearishRed = Color(0xFFEF4444)
val DarkBearishRedDark = Color(0xFF7F1D1D)
val DarkBearishRedGlow = Color(0xFFF87171)

val DarkCyanRS = Color(0xFF06B6D4)
val DarkCyanRSDark = Color(0xFF164E63)
val DarkAmberWarning = Color(0xFFF59E0B)
val DarkAmberGold = Color(0xFFFBBF24)
val DarkAmberDark = Color(0xFF78350F)
val DarkPurpleMA = Color(0xFFA855F7)

val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextTertiary = Color(0xFF64748B)

// Data class for dynamic terminal colors
data class TerminalThemeColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val divider: Color,
    val bullishGreen: Color,
    val bullishGreenLight: Color,
    val bullishGreenGlow: Color,
    val bearishRed: Color,
    val bearishRedLight: Color,
    val bearishRedGlow: Color,
    val cyanRS: Color,
    val cyanRSLight: Color,
    val amberGold: Color,
    val amberWarning: Color,
    val amberLight: Color,
    val purpleMA: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val isDark: Boolean
)

val LocalTerminalColors = compositionLocalOf {
    LightTerminalColors
}

val LightTerminalColors = TerminalThemeColors(
    background = LightBackground,
    surface = LightSurface,
    card = LightCard,
    cardElevated = LightCardElevated,
    border = LightBorder,
    divider = LightDivider,
    bullishGreen = LightBullishGreen,
    bullishGreenLight = LightBullishGreenDark,
    bullishGreenGlow = LightBullishGreenGlow,
    bearishRed = LightBearishRed,
    bearishRedLight = LightBearishRedDark,
    bearishRedGlow = LightBearishRedGlow,
    cyanRS = LightCyanRS,
    cyanRSLight = LightCyanRSDark,
    amberGold = LightAmberGold,
    amberWarning = LightAmberWarning,
    amberLight = LightAmberDark,
    purpleMA = LightPurpleMA,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    isDark = false
)

val DarkTerminalColors = TerminalThemeColors(
    background = DarkBackground,
    surface = DarkSurface,
    card = DarkCard,
    cardElevated = DarkCardElevated,
    border = DarkBorder,
    divider = DarkDivider,
    bullishGreen = DarkBullishGreen,
    bullishGreenLight = DarkBullishGreenDark,
    bullishGreenGlow = DarkBullishGreenGlow,
    bearishRed = DarkBearishRed,
    bearishRedLight = DarkBearishRedDark,
    bearishRedGlow = DarkBearishRedGlow,
    cyanRS = DarkCyanRS,
    cyanRSLight = DarkCyanRSDark,
    amberGold = DarkAmberGold,
    amberWarning = DarkAmberWarning,
    amberLight = DarkAmberDark,
    purpleMA = DarkPurpleMA,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    isDark = true
)

// Global aliases to ensure backwards compatibility
var TerminalBackground = LightBackground
var TerminalSurface = LightSurface
var TerminalCard = LightCard
var TerminalCardElevated = LightCardElevated
var TerminalBorder = LightBorder

var BullishGreen = LightBullishGreen
var BullishGreenDark = LightBullishGreenDark
var BullishGreenGlow = LightBullishGreenGlow

var BearishRed = LightBearishRed
var BearishRedDark = LightBearishRedDark
var BearishRedGlow = LightBearishRedGlow

var CyanRS = LightCyanRS
var CyanRSDark = LightCyanRSDark
var AmberWarning = LightAmberWarning
var AmberGold = LightAmberGold
var PurpleMA = LightPurpleMA

var TextPrimary = LightTextPrimary
var TextSecondary = LightTextSecondary
var TextTertiary = LightTextTertiary
var DividerColor = LightDivider

fun updateGlobalColors(colors: TerminalThemeColors) {
    TerminalBackground = colors.background
    TerminalSurface = colors.surface
    TerminalCard = colors.card
    TerminalCardElevated = colors.cardElevated
    TerminalBorder = colors.border

    BullishGreen = colors.bullishGreen
    BullishGreenDark = colors.bullishGreenLight
    BullishGreenGlow = colors.bullishGreenGlow

    BearishRed = colors.bearishRed
    BearishRedDark = colors.bearishRedLight
    BearishRedGlow = colors.bearishRedGlow

    CyanRS = colors.cyanRS
    CyanRSDark = colors.cyanRSLight
    AmberWarning = colors.amberWarning
    AmberGold = colors.amberGold
    PurpleMA = colors.purpleMA

    TextPrimary = colors.textPrimary
    TextSecondary = colors.textSecondary
    TextTertiary = colors.textTertiary
    DividerColor = colors.divider
}
