package com.calmcoloring.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    background = CalmPalette.BgLight,
    surface = CalmPalette.SurfaceLight,
    onBackground = CalmPalette.InkLight,
    onSurface = CalmPalette.InkLight,
    outline = CalmPalette.LineLight,
    primary = CalmPalette.ClayLight,
)

private val DarkColors = darkColorScheme(
    background = CalmPalette.BgDark,
    surface = CalmPalette.SurfaceDark,
    onBackground = CalmPalette.InkDark,
    onSurface = CalmPalette.InkDark,
    outline = CalmPalette.LineDark,
    primary = CalmPalette.ClayDark,
)

@Composable
fun CalmColoringTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CalmTypography,
        content = content,
    )
}
