package com.mangatv.reader.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val TvDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = TextDark,
    primaryContainer = CinemaSurfaceVariant,
    onPrimaryContainer = TextWhite,
    secondary = AccentTeal,
    onSecondary = TextDark,
    background = CinemaDarkBg,
    onBackground = TextWhite,
    surface = CinemaSurface,
    onSurface = TextWhite,
    surfaceVariant = CinemaSurfaceVariant,
    onSurfaceVariant = TextMuted,
    error = AccentRed,
    onError = TextWhite
)

@Composable
fun MangaTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = TvTypography,
        content = content
    )
}
