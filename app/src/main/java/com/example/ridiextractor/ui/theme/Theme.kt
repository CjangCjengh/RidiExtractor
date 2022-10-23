package com.example.ridiextractor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = LightBlue,
    primaryVariant = DarkBlue,
    secondary = Teal
)

private val LightColorPalette = lightColors(
    primary = MediumBlue,
    primaryVariant = DarkBlue,
    secondary = Teal
)

@Composable
fun RidiExtractorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}