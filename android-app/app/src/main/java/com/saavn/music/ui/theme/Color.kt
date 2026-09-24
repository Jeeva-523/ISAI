package com.saavn.music.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Dynamic Theme Color Accessors via LocalIsaiColors
val DarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.background

val DarkSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.surface

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.surface

val DarkSurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.surfaceVariant

val DarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.surfaceVariant

val DarkSurfaceGlass: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.surfaceGlass

val DarkBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.border

// Brand Accents
val IsaiLime: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.primaryAccent

val NeonLime: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.primaryAccent

val IsaiViolet: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.primaryAccent

val NeonCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.secondaryAccent

val NeonPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFF6D28D9)

val NeonPink: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFFDB2777)

val NeonBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFF2563EB)

val NeonAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFFD97706)

// Dedicated Heart / Favorite Colors
val HeartColor = Color(0xFFFF2D55)
val HeartColorGlow = Color(0x33FF2D55)

val GoldRank = Color(0xFFD97706)
val SilverRank = Color(0xFF4B5563)
val BronzeRank = Color(0xFFB45309)

// Typography
val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.textMuted

val TextDisabled: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.textMuted.copy(alpha = 0.5f)

val DividerColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.borderSubtle

// Borders & Glass
val GlassBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.border

val GlassBorderSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.borderSubtle

// Mood Accents
val MoodCalm = Color(0xFF7C3AED)
val MoodSad = Color(0xFF4C1D95)
val MoodLove = Color(0xFFDB2777)
val MoodEnergy = Color(0xFF7C3AED)
val MoodChill = Color(0xFF059669)
val MoodNight = Color(0xFF4C1D95)
val MoodParty = Color(0xFFDB2777)

// Status Indicators
val SuccessGreen = Color(0xFF059669)
val WarningAmber = Color(0xFFD97706)
val ErrorRed = Color(0xFFDC2626)
val InfoBlue = Color(0xFF2563EB)

val AccentPlay: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.primaryAccent

val SliderTrack: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalIsaiColors.current.borderSubtle
