package io.github.piyushdaiya.vaachak.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EInkColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black
)

@Composable
fun VaachakTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EInkColorScheme, content = content)
}

