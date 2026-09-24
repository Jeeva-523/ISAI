package com.saavn.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun SaavnMusicTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val palette = getPaletteForMode(themeMode)

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primaryAccent,
            secondary = palette.secondaryAccent,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceVariant,
            onPrimary = palette.background,
            onSecondary = palette.textPrimary,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary
        )
    } else {
        lightColorScheme(
            primary = palette.primaryAccent,
            secondary = palette.secondaryAccent,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.surfaceVariant,
            onPrimary = Color.White,
            onSecondary = palette.textPrimary,
            onBackground = palette.textPrimary,
            onSurface = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary
        )
    }

    CompositionLocalProvider(
        LocalIsaiColors provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
