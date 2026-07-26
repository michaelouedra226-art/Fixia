package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FixiaBlueTertiary,
    onPrimary = Color.White,
    secondary = FixiaBlueSecondary,
    background = FixiaDarkBackground,
    surface = FixiaDarkSurface,
    surfaceVariant = FixiaDarkCard,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = FixiaBluePrimary,
    onPrimary = Color.White,
    secondary = FixiaBlueSecondary,
    tertiary = FixiaBlueTertiary,
    background = FixiaLightBackground,
    surface = FixiaLightSurface,
    surfaceVariant = FixiaLightCard,
    outline = FixiaLightBorder,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B)
)

@Composable
fun FixiaTheme(
    themeMode: String = "system", // "system", "dark", "light"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
