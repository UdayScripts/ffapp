package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = FireOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDE6),
    onPrimaryContainer = FireOrangeDark,
    secondary = HighContrastDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = HighContrastDark,
    tertiary = TrophyGold,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    error = LiveRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = FireOrangeLight,
    onPrimary = HighContrastDark,
    primaryContainer = FireOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF94A3B8),
    onSecondary = HighContrastDark,
    secondaryContainer = HighContrastSurface,
    onSecondaryContainer = Color.White,
    tertiary = TrophyGoldLight,
    onTertiary = HighContrastDark,
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF8FAFC),
    surface = HighContrastSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = HighContrastBorder,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = HighContrastBorder,
    error = LiveRed,
    onError = Color.White
)

@Composable
fun FFTournamentsTheme(
    darkTheme: Boolean = ThemePreferenceState.isDarkMode,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    ThemePreferenceState.isDarkMode = darkTheme

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
