/*
 *  Copyright (c) 2026 Piyush Daiya
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 */

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