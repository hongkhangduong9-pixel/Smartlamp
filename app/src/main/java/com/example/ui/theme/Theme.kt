package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlynkDarkColorScheme = darkColorScheme(
    primary = AmberGlow,
    onPrimary = Color(0xFF1F1400),
    primaryContainer = AmberSurfaceTint,
    onPrimaryContainer = AmberWarmLight,
    secondary = SkyAccent,
    onSecondary = Color(0xFF061B33),
    secondaryContainer = ObsidianSurfaceElevated,
    onSecondaryContainer = OffWhiteText,
    tertiary = EmeraldOnline,
    background = ObsidianBg,
    onBackground = OffWhiteText,
    surface = ObsidianSurface,
    onSurface = OffWhiteText,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = SlateMuted,
    outline = ObsidianBorder,
    error = CoralOffline,
    onError = Color.White,
)

@Composable
fun BlynkLightTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BlynkDarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
