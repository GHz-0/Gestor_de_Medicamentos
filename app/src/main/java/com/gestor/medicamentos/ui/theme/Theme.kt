package com.gestor.medicamentos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gestor.medicamentos.data.DoseStatus

// Modern medical palette: Teal / Cyan / Indigo / Soft card surfaces
val Cyan = Color(0xFF00897B)       // Rich Medical Teal / Cyan
val CyanDark = Color(0xFF004D40)   // Deep Teal for high-contrast headlines
val Turquoise = Color(0xFF26A69A)  // Vibrant Accent
val Sky = Color(0xFFE0F2F1)        // Very soft mint container
val SkyBlue = Color(0xFF80CBC4)
val Red = Color(0xFFD32F2F)
val RedSoft = Color(0xFFFFEBEE)
val White = Color(0xFFFFFFFF)
val Ink = Color(0xFF0A252C)        // Deep dark ink for crisp text legibility

// Semáforo de tarjetas de medicamentos (Modo Claro)
val OkBg = Color(0xFFE8F5E9);   val OkBorder = Color(0xFF2E7D32)
val SoonBg = Color(0xFFFFF8E1); val SoonBorder = Color(0xFFF57F17)

fun statusColor(s: DoseStatus) = when (s) {
    DoseStatus.TOMADA -> Color(0xFF2E7D32)
    DoseStatus.OMITIDA -> Red
    DoseStatus.POSPUESTA -> CyanDark
}

private val LightScheme = lightColorScheme(
    primary = Cyan, onPrimary = White,
    primaryContainer = Sky, onPrimaryContainer = Ink,
    secondary = Turquoise, onSecondary = White,
    secondaryContainer = Color(0xFFE0F2F1), onSecondaryContainer = Ink,
    surfaceVariant = Color(0xFFEDF7F6), onSurfaceVariant = Ink,
    tertiary = SkyBlue,
    background = Color(0xFFF5FBFB), onBackground = Ink,
    surface = White, onSurface = Ink,
    error = Red
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF26A69A), onPrimary = Color(0xFF002520),
    primaryContainer = Color(0xFF004D40), onPrimaryContainer = Color(0xFFE0F2F1),
    secondary = Color(0xFF80CBC4), onSecondary = Color(0xFF002520),
    secondaryContainer = Color(0xFF1B2E32), onSecondaryContainer = Color(0xFFB2DFDB),
    surfaceVariant = Color(0xFF1E2D30), onSurfaceVariant = Color(0xFFE0F2F1),
    tertiary = SkyBlue,
    background = Color(0xFF121B1D), onBackground = Color(0xFFE0F2F1),
    surface = Color(0xFF172326), onSurface = Color(0xFFE0F2F1),
    error = Color(0xFFEF5350)
)

@Composable
fun GestorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(colorScheme = scheme, content = content)
}
