package com.intimocoffee.loyalty.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Intimo Coffee Loyalty — Dark elegant theme
private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFB0B0B0),
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFCCCCCC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF444444),
    error = Color(0xFFCF6679),
    onError = Color.White,
)

// Shared colors used across screens
object IntimoColors {
    val CardBackground = Color(0xFF1E1E1E)
    val CardBackgroundElevated = Color(0xFF252525)
    val Accent = Color(0xFFE0E0E0)
    val SubtleText = Color(0xFF888888)
    val Divider = Color(0xFF333333)
    val Green = Color(0xFF66BB6A)
    val Red = Color(0xFFEF5350)
    val Gold = Color(0xFFFFD700)
    val Silver = Color(0xFFC0C0C0)
    val Bronze = Color(0xFFCD7F32)
}

@Composable
fun IntimoCoffeeLoyaltyTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color(0xFF121212).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
