package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = FireOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDE6),
    onPrimaryContainer = FireOrangeDark,
    secondary = HighContrastDark,
    onSecondary = Color.White,
    secondaryContainer = SlateSubtle,
    onSecondaryContainer = HighContrastDark,
    tertiary = TrophyGold,
    onTertiary = Color.White,
    background = SlateLightBg,
    onBackground = TextPrimaryDark,
    surface = SlateCardBg,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSubtle,
    onSurfaceVariant = TextSecondaryMuted,
    outline = SlateMutedBorder,
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
    background = HighContrastDark,
    onBackground = Color.White,
    surface = HighContrastSurface,
    onSurface = Color.White,
    surfaceVariant = HighContrastBorder,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = HighContrastBorder,
    error = LiveRed,
    onError = Color.White
)

@Composable
fun FFTournamentsTheme(
    darkTheme: Boolean = false, // Default to the requested minimalist modern light theme
    dynamicColor: Boolean = false, // Keep consistent esports branding
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
