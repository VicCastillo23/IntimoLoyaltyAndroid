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

// Íntimo — tema claro luminoso (referencia: apps de lealtad premium)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3D2817),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0E6DC),
    onPrimaryContainer = Color(0xFF3D2817),
    secondary = Color(0xFFC9A66B),
    onSecondary = Color(0xFF2A1810),
    secondaryContainer = Color(0xFFFFF8F0),
    onSecondaryContainer = Color(0xFF5C4A38),
    tertiary = Color(0xFFD4AF37),
    background = Color(0xFFF7F3EE),
    onBackground = Color(0xFF1A120E),
    surface = Color.White,
    onSurface = Color(0xFF1A120E),
    surfaceVariant = Color(0xFFF0EBE4),
    onSurfaceVariant = Color(0xFF6B5E54),
    outline = Color(0xFFD8D0C8),
    error = Color(0xFFC62828),
    onError = Color.White,
)

object IntimoColors {
    val Background = Color(0xFFF7F3EE)
    val Cream = Color(0xFF1A120E)
    val Caramel = Color(0xFFC9A66B)
    val CaramelDark = Color(0xFFA8844F)
    val Espresso = Color(0xFF3D2817)
    val EspressoSoft = Color(0xFF5C4030)
    val CardBackground = Color.White
    val CardBackgroundElevated = Color(0xFFFAFAF8)
    val Accent = Color(0xFFC9A66B)
    val SubtleText = Color(0xFF7A6E64)
    val Divider = Color(0xFFE8E0D8)
    val BorderMuted = Color(0xFFD8D0C8)
    val TabBar = Color.White
    val ChipBg = Color(0xFFF0EBE4)
    val GradientStart = Color(0xFF4A3728)
    val GradientEnd = Color(0xFF2A1810)
    val ProgressTrack = Color(0xFFE8E0D8)
    val ProgressGold = Color(0xFFD4AF37)
    val Green = Color(0xFF2E7D32)
    val Red = Color(0xFFC62828)
    val Gold = Color(0xFFD4AF37)
    val Silver = Color(0xFF9E9E9E)
    val Bronze = Color(0xFFCD7F32)
    val ScanFab = Color(0xFF3D2817)
}

object IntimoAppInfo {
    const val brandName = "Íntimo Coffee"
    val settingsFooter: String
        get() = "Íntimo Loyalty v${BuildConfig.VERSION_NAME}"
}

@Composable
fun IntimoCoffeeLoyaltyTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = IntimoColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = IntimoTypography,
        content = content,
    )
}
