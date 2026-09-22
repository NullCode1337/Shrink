package dev.nullcode.shrink.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF2D4BFF),
    onPrimary = Color.White,
    background = Color(0xFFEEF1F5),
    onBackground = Color(0xFF0F1B2D),
    surface = Color.White,
    onSurface = Color(0xFF0F1B2D),
    onSurfaceVariant = Color(0xFF5F6B7E),
    outline = Color(0xFFCBD3DF),
    error = Color(0xFFC62E4A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8AA0FF),
    onPrimary = Color(0xFF0B1230),
    background = Color(0xFF0E1420),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF172033),
    onSurface = Color(0xFFE8EDF5),
    onSurfaceVariant = Color(0xFF93A3BB),
    outline = Color(0xFF2D3A55),
    error = Color(0xFFFF7A90),
)

@Composable
fun ShrinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
