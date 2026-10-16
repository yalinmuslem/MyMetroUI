package com.mymetroui.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mymetroui.launcher.data.ThemeMode
import com.mymetroui.launcher.data.ThemeSettings

@Composable
fun MyMetroUITheme(settings: ThemeSettings = ThemeSettings(), content: @Composable () -> Unit) {
    val accent = accentFor(settings.accentId).color
    val scheme = when (settings.mode) {
        ThemeMode.DARK -> darkColorScheme(
            primary = accent, onPrimary = Color.White,
            background = Color.Black, surface = Color.Black,
            onBackground = Color.White, onSurface = Color.White,
            surfaceVariant = Color(0xFF1F1F1F), onSurfaceVariant = Color.White,
        )
        ThemeMode.LIGHT -> lightColorScheme(
            primary = accent, onPrimary = Color.White,
            background = Color.White, surface = Color.White,
            onBackground = Color.Black, onSurface = Color.Black,
            surfaceVariant = Color(0xFFE6E6E6), onSurfaceVariant = Color.Black,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = MetroTypography) {
        // Surface supplies contentColor; without it Text falls back to black, unreadable on the dark background.
        Surface(color = scheme.background, contentColor = scheme.onBackground, content = content)
    }
}
