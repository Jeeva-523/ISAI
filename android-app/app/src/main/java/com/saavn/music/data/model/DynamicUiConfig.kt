package com.saavn.music.data.model

import androidx.compose.ui.graphics.Color

data class DynamicUiConfig(
    val queueActionStyle: String = "capsule_pill", // "capsule_pill", "three_dots", "minimal_delete", "standard_row"
    val enableQueueReorder: Boolean = true,
    val enableQueueDelete: Boolean = true,
    val capsulePillSize: Int = 32,
    val accentColorHex: String = "#00F5D4", // NeonCyan default
    val deleteColorHex: String = "#FF007F", // NeonPink default
    val queueHeaderSubtitle: String = "Reorder with ▲ ▼ • Remove with 🗑",
    val showPlayingBadge: Boolean = true,
    val bannerMessage: String = "",
    val version: Long = 1L
) {
    fun parseAccentColor(fallback: Color): Color {
        return try {
            Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (_: Exception) {
            fallback
        }
    }

    fun parseDeleteColor(fallback: Color): Color {
        return try {
            Color(android.graphics.Color.parseColor(deleteColorHex))
        } catch (_: Exception) {
            fallback
        }
    }
}
