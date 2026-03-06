package com.remnd.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.math.abs

fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = lightness - c / 2f
    val (r, g, b) = when {
        h < 60f  -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else     -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

fun buildColorScheme(hue: Float, darkTheme: Boolean): ColorScheme {
    val secondary = (hue + 40f) % 360f
    val tertiary = (hue + 80f) % 360f
    return if (darkTheme) {
        darkColorScheme(
            primary = hslToColor(hue, 0.65f, 0.78f),
            onPrimary = hslToColor(hue, 0.65f, 0.18f),
            primaryContainer = hslToColor(hue, 0.50f, 0.28f),
            onPrimaryContainer = hslToColor(hue, 0.70f, 0.90f),
            secondary = hslToColor(secondary, 0.40f, 0.72f),
            onSecondary = hslToColor(secondary, 0.40f, 0.18f),
            secondaryContainer = hslToColor(secondary, 0.30f, 0.28f),
            onSecondaryContainer = hslToColor(secondary, 0.60f, 0.90f),
            tertiary = hslToColor(tertiary, 0.40f, 0.72f),
            onTertiary = hslToColor(tertiary, 0.40f, 0.18f),
            tertiaryContainer = hslToColor(tertiary, 0.30f, 0.28f),
            onTertiaryContainer = hslToColor(tertiary, 0.60f, 0.90f),
        )
    } else {
        lightColorScheme(
            primary = hslToColor(hue, 0.40f, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hslToColor(hue, 0.70f, 0.90f),
            onPrimaryContainer = hslToColor(hue, 0.40f, 0.15f),
            secondary = hslToColor(secondary, 0.25f, 0.38f),
            onSecondary = Color.White,
            secondaryContainer = hslToColor(secondary, 0.50f, 0.90f),
            onSecondaryContainer = hslToColor(secondary, 0.25f, 0.15f),
            tertiary = hslToColor(tertiary, 0.25f, 0.38f),
            onTertiary = Color.White,
            tertiaryContainer = hslToColor(tertiary, 0.50f, 0.90f),
            onTertiaryContainer = hslToColor(tertiary, 0.25f, 0.15f),
        )
    }
}

@Composable
fun RemndTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeHue: Float = 264f,
    content: @Composable () -> Unit
) {
    val colorScheme = buildColorScheme(themeHue, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
