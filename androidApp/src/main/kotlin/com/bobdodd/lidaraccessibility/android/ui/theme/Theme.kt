package com.bobdodd.lidaraccessibility.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * AAA-contrast palette using the a11ybob "maps" zonal tint
 * (155 deg / 0.045 forest green, OKLCH L=95% light / L=20% dark).
 * All foreground / background pairings pass WCAG 2.2 AAA (7:1) for
 * body text — verified against WCAG contrast tables.
 *
 * See docs/architecture.md § "Theming".
 */
private val MapsForestGreenLight = Color(0xFFEFF4EE)   // OKLCH L=95% at 155°/0.045
private val MapsForestGreenDark = Color(0xFF1F2A22)    // OKLCH L=20% at 155°/0.045

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0B3D2E),
    onPrimary = Color.White,
    background = MapsForestGreenLight,
    onBackground = Color(0xFF0A0F0B),
    surface = Color.White,
    onSurface = Color(0xFF0A0F0B),
    error = Color(0xFF8A0F1A),
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB7E0C7),
    onPrimary = Color(0xFF06231A),
    background = MapsForestGreenDark,
    onBackground = Color(0xFFF2F5F1),
    surface = Color(0xFF14201A),
    onSurface = Color(0xFFF2F5F1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun LidarAccessibilityTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = LidarAccessibilityTypography,
        content = content,
    )
}
