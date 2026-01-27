package io.github.piyushdaiya.vaachak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    secondary = Color.Gray,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// Define DarkColorScheme if needed, or stick to Light for now
private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color.Gray,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun VaachakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isEinkMode: Boolean = false, // We'll pull this from settings
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isEinkMode -> lightColorScheme(
            primary = PureBlack,
            background = PureWhite,
            surface = PureWhite,
            onPrimary = PureWhite,
            onBackground = PureBlack,
            onSurface = PureBlack
        )
        darkTheme -> darkColorScheme(
            primary = PureWhite,
            background = PureBlack,
            surface = Color(0xFF121212)
        )
        else -> lightColorScheme(
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

