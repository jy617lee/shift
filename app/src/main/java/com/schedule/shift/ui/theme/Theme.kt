package com.schedule.shift.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShiftColorScheme = lightColorScheme(
    primary = ShiftGreen,
    onPrimary = Color.White,
    primaryContainer = ShiftGreenLight,
    onPrimaryContainer = ShiftGreenDark,
    secondary = ShiftMutedFg,
    onSecondary = Color.White,
    secondaryContainer = ShiftMuted,
    onSecondaryContainer = ShiftGreenDark,
    tertiary = ShiftGold,
    onTertiary = Color.White,
    tertiaryContainer = ShiftVacationBg,
    onTertiaryContainer = ShiftVacationFg,
    background = ShiftBackground,
    onBackground = ShiftGreenDark,
    surface = ShiftSurface,
    onSurface = ShiftGreenDark,
    surfaceVariant = ShiftMuted,
    onSurfaceVariant = ShiftMutedFg,
    outline = ShiftBorder,
    outlineVariant = ShiftBorder,
)

@Composable
fun ShiftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShiftColorScheme,
        typography = Typography,
        content = content,
    )
}
