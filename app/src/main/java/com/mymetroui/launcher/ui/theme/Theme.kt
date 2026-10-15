package com.mymetroui.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MetroAccent = Color(0xFF1BA1E2)

@Composable
fun MyMetroUITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = MetroAccent,
            background = Color.Black,
            surface = Color.Black,
        ),
        typography = MetroTypography,
        content = content,
    )
}
