package com.saavn.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPurple

@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 16.dp,
    activeColor: Color = NeonCyan
) {
    val transition = rememberInfiniteTransition(label = "equalizer")

    val h1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )

    val h2 by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )

    val h3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    val h4 by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h4"
    )

    val count = barCount.coerceIn(1, 4)
    val spacing = 2.dp
    val totalWidth = (barWidth * count) + (spacing * (count - 1))
    val gradientColor = NeonPurple

    Canvas(
        modifier = modifier
            .width(totalWidth)
            .height(maxHeight)
    ) {
        val heights = listOf(h1, h2, h3, h4)
        val wPx = barWidth.toPx()
        val spacePx = spacing.toPx()
        val maxHPx = maxHeight.toPx()
        val cornerRadius = CornerRadius(2.dp.toPx())

        for (i in 0 until count) {
            val barFraction = if (isPlaying) heights[i % heights.size].coerceIn(0.15f, 1f) else 0.25f
            val barHPx = (maxHPx * barFraction).coerceAtLeast(4.dp.toPx())
            val x = i * (wPx + spacePx)
            val y = size.height - barHPx

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(activeColor, gradientColor),
                    startY = y,
                    endY = size.height
                ),
                topLeft = Offset(x, y),
                size = Size(wPx, barHPx),
                cornerRadius = cornerRadius
            )
        }
    }
}
