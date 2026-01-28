package io.github.piyushdaiya.vaachak.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun VaachakTheme(
    themeMode: ThemeMode = ThemeMode.E_INK,
    contrast: Float = 0.5f, // 0.0f (soft) to 1.0f (sharp/pure black)
    isEinkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.E_INK -> {
            // Sharpen colors based on contrast slider
            val contrastColor = lerp(Color.Gray, PureBlack, contrast)
            lightColorScheme(
                primary = PureBlack,
                onPrimary = PureWhite,
                background = PureWhite,
                surface = PureWhite,
                onBackground = PureBlack,
                onSurface = PureBlack,
                outline = contrastColor, // Sharper borders/dividers
                secondary = contrastColor // Sharper supporting text
            )
        }
        ThemeMode.DARK -> darkColorScheme(
            primary = PureWhite,
            onPrimary = PureBlack,
            background = PureBlack,
            surface = DarkSurface,
            onBackground = PureWhite,
            onSurface = PureWhite
        )
        ThemeMode.LIGHT -> lightColorScheme(
            primary = PureBlack,
            background = SoftWhite,
            surface = PureWhite
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

enum class ThemeMode {
    LIGHT,
    DARK,
    E_INK
}