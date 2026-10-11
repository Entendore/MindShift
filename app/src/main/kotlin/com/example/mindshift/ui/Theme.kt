package com.example.mindshift.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColors = lightColorScheme(
    primary = Color(0xFF5F8D75),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEDE3),
    onPrimaryContainer = Color(0xFF3E5C4B),
    secondary = Color(0xFF8FBC9F),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF26332C),
    surface = Color.White,
    onSurface = Color(0xFF26332C),
    surfaceVariant = Color(0xFFEFF3EE),
    onSurfaceVariant = Color(0xFF5A6B60),
    outline = Color(0xFFB9C8BE)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BC7AC),
    onPrimary = Color(0xFF10291C),
    primaryContainer = Color(0xFF2E4A3B),
    onPrimaryContainer = Color(0xFFD7EADD),
    secondary = Color(0xFF7EA98F),
    background = Color(0xFF151A17),
    onBackground = Color(0xFFE0E6E1),
    surface = Color(0xFF1B211D),
    onSurface = Color(0xFFE0E6E1),
    surfaceVariant = Color(0xFF242C27),
    onSurfaceVariant = Color(0xFFA7B4AA),
    outline = Color(0xFF46524A)
)

@Composable
fun MindShiftTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val colors = when (mode) {
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColors else LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
