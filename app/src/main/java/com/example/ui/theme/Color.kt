package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ThemePreferenceState {
    var isDarkMode by mutableStateOf(false)
}

val FireOrange = Color(0xFFFF5722)
val FireOrangeDark = Color(0xFFEA580C)
val FireOrangeLight = Color(0xFFFF8A65)

val HighContrastDark = Color(0xFF0F172A)
val HighContrastSurface = Color(0xFF1E293B)
val HighContrastBorder = Color(0xFF334155)

val TrophyGold = Color(0xFFF59E0B)
val TrophyGoldLight = Color(0xFFFCD34D)

val SuccessGreen = Color(0xFF10B981)
val SuccessGreenLight = Color(0xFFD1FAE5)

val LiveRed = Color(0xFFEF4444)
val LiveRedLight = Color(0xFFFEE2E2)

val SlateLightBg: Color
    get() = if (ThemePreferenceState.isDarkMode) Color(0xFF0B1120) else Color(0xFFF8FAFC)

val SlateCardBg: Color
    get() = if (ThemePreferenceState.isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)

val SlateMutedBorder: Color
    get() = if (ThemePreferenceState.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

val SlateSubtle: Color
    get() = if (ThemePreferenceState.isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)

val TextPrimaryDark: Color
    get() = if (ThemePreferenceState.isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val TextSecondaryMuted: Color
    get() = if (ThemePreferenceState.isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

val TextTertiaryLight: Color
    get() = if (ThemePreferenceState.isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
