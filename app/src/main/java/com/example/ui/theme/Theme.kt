package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = Emerald,
    background = BgDark,
    surface = BgDark,
    surfaceVariant = CardBgDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextMainDark,
    onSurface = TextMainDark,
    onSurfaceVariant = TextMutedDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    secondary = Emerald,
    background = BgLight,
    surface = CardBgLight,
    surfaceVariant = CardBgLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextMainLight,
    onSurface = TextMainLight,
    onSurfaceVariant = TextMutedLight,
    outline = BorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // default to dark
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
