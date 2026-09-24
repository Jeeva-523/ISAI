package com.saavn.music.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(
    val title: String,
    val subtitle: String,
    val iconEmoji: String
) {
    DARK(
        title = "Midnight Dark",
        subtitle = "Classic deep dark with neon glow",
        iconEmoji = "🌙"
    ),
    LIGHT(
        title = "Clean Snow",
        subtitle = "Crisp white background with dark text",
        iconEmoji = "☀️"
    ),
    AMOLED(
        title = "AMOLED Black",
        subtitle = "100% pitch black for battery saving",
        iconEmoji = "🖤"
    ),
    CYBERPUNK(
        title = "Cyberpunk Neon",
        subtitle = "Deep cosmic violet with neon cyan glow",
        iconEmoji = "🔮"
    )
}

data class IsaiColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceGlass: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val isDark: Boolean
)

val DarkThemePalette = IsaiColors(
    background = Color(0xFF0F141C),
    surface = Color(0xFF161C26),
    surfaceVariant = Color(0xFF1E2634),
    surfaceGlass = Color(0xEB161C26),
    border = Color(0x338B5CF6),
    borderSubtle = Color(0x26FFFFFF),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    primaryAccent = Color(0xFF8B5CF6),
    secondaryAccent = Color(0xFF06B6D4),
    isDark = true
)

val LightThemePalette = IsaiColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF8F9FA),
    surfaceVariant = Color(0xFFF1F3F5),
    surfaceGlass = Color(0xF5FFFFFF),
    border = Color(0xFFE2E8F0),
    borderSubtle = Color(0x1A000000),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textMuted = Color(0xFF64748B),
    primaryAccent = Color(0xFF7C3AED),
    secondaryAccent = Color(0xFFEC4899),
    isDark = false
)

val AmoledThemePalette = IsaiColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF141414),
    surfaceGlass = Color(0xFA050505),
    border = Color(0x3322C55E),
    borderSubtle = Color(0x26FFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFD4D4D4),
    textMuted = Color(0xFFA3A3A3),
    primaryAccent = Color(0xFF10B981),
    secondaryAccent = Color(0xFF06B6D4),
    isDark = true
)

val CyberpunkThemePalette = IsaiColors(
    background = Color(0xFF0B0418),
    surface = Color(0xFF150A2E),
    surfaceVariant = Color(0xFF221147),
    surfaceGlass = Color(0xF0150A2E),
    border = Color(0x4DDB2777),
    borderSubtle = Color(0x33A78BFA),
    textPrimary = Color(0xFFFDF4FF),
    textSecondary = Color(0xFFF0ABFC),
    textMuted = Color(0xFFC084FC),
    primaryAccent = Color(0xFFE879F9),
    secondaryAccent = Color(0xFF06B6D4),
    isDark = true
)

fun getPaletteForMode(mode: AppThemeMode): IsaiColors {
    return when (mode) {
        AppThemeMode.DARK -> DarkThemePalette
        AppThemeMode.LIGHT -> LightThemePalette
        AppThemeMode.AMOLED -> AmoledThemePalette
        AppThemeMode.CYBERPUNK -> CyberpunkThemePalette
    }
}

val LocalIsaiColors = staticCompositionLocalOf { DarkThemePalette }
