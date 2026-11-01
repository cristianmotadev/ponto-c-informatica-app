package com.ddcrcodelab.pontocinformatica.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Três modos: seguir o sistema, forçar claro (diurno) ou forçar escuro (noturno)
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Paleta diurna: fundo claro, acento laranja (identidade do app)
private val LightColors = lightColorScheme(
    primary = Color(0xFFB25E3A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFE8A184),
    background = Color(0xFFF5F2EF),
    onBackground = Color(0xFF1C1A18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1A18)
)

// Paleta noturna: fundo escuro, acento laranja suave (conforto visual)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8A184),
    onPrimary = Color(0xFF3A2014),
    secondary = Color(0xFFB25E3A),
    background = Color(0xFF141210),
    onBackground = Color(0xFFEDE8E4),
    surface = Color(0xFF1E1B18),
    onSurface = Color(0xFFEDE8E4)
)

@Composable
fun PontoCTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}