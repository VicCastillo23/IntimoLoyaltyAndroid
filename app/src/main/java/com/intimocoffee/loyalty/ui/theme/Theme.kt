package com.intimocoffee.loyalty.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.intimocoffee.loyalty.BuildConfig

// Íntimo Coffee — paleta cálida editorial (crema, caramelo, espresso)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC9A66B),
    onPrimary = Color(0xFF2A1810),
    primaryContainer = Color(0xFF3D2A1E),
    onPrimaryContainer = Color(0xFFE8D5C4),
    secondary = Color(0xFFE8D5C4),
    onSecondary = Color(0xFF1A120E),
    secondaryContainer = Color(0xFF2A2018),
    onSecondaryContainer = Color(0xFFD4C4B0),
    tertiary = Color(0xFF8B7355),
    background = Color(0xFF0D0B0A),
    onBackground = Color(0xFFF0E6DC),
    surface = Color(0xFF1A1614),
    onSurface = Color(0xFFF0E6DC),
    surfaceVariant = Color(0xFF252019),
    onSurfaceVariant = Color(0xFF9A9088),
    outline = Color(0xFF4A4038),
    error = Color(0xFFE57373),
    onError = Color(0xFF1A120E),
)

object IntimoColors {
    val Background = Color(0xFF0D0B0A)
    val Cream = Color(0xFFE8D5C4)
    val Caramel = Color(0xFFC9A66B)
    val CaramelDark = Color(0xFFA8844F)
    val Espresso = Color(0xFF2A1810)
    val CardBackground = Color(0xFF1A1614)
    val CardBackgroundElevated = Color(0xFF221C18)
    val Accent = Color(0xFFE8D5C4)
    val SubtleText = Color(0xFF8A8078)
    val Divider = Color(0xFF3A322C)
    val BorderMuted = Color(0xFF4A4038)
    val TabBar = Color(0xFF141110)
    val ChipBg = Color(0xFF2A221C)
    val GradientStart = Color(0xFF3D2A1E)
    val GradientEnd = Color(0xFF1A120E)
    val ProgressTrack = Color(0xFF3A322C)
    val Green = Color(0xFF81C784)
    val Red = Color(0xFFE57373)
    val Gold = Color(0xFFD4AF37)
    val Silver = Color(0xFFB8B8B8)
    val Bronze = Color(0xFFCD7F32)
}

object IntimoAppInfo {
    const val brandName = "Íntimo Coffee"
    val settingsFooter: String
        get() = "Íntimo Loyalty v${BuildConfig.VERSION_NAME}"
}

@Composable
fun IntimoCoffeeLoyaltyTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = IntimoColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = IntimoTypography,
        content = content,
    )
}
