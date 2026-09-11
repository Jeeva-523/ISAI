package com.saavn.music.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saavn.music.ui.theme.*
import coil.compose.AsyncImage
import com.saavn.music.ui.MainViewModel
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPink
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

@Composable
fun MiniPlayer(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentSongLocal by viewModel.ytPlayerController.currentSong.collectAsState()
    val isPlayingLocal by viewModel.ytPlayerController.isPlaying.collectAsState()
    val isBuffering by viewModel.ytPlayerController.isBuffering.collectAsState()
    val positionSecLocal by viewModel.ytPlayerController.currentPositionSec.collectAsState()
    val durationSecLocal by viewModel.ytPlayerController.durationSec.collectAsState()

    val syncState by viewModel.isaiConnectManager.playbackState.collectAsState()
    val isSeparateMode by viewModel.isMultiDevicePlaybackSeparate.collectAsState()
    val isMyDeviceActive = if (isSeparateMode) true else viewModel.isaiConnectManager.isMyDeviceActive()
    val isRemoteActive = !isSeparateMode && !isMyDeviceActive && syncState != null && 
        syncState!!.currentDeviceId.isNotBlank() && 
        !syncState!!.currentTitle.isNullOrBlank() && 
        (syncState!!.isPlaying || Math.abs(System.currentTimeMillis() - syncState!!.updatedAt) < 15 * 60_000L)

    val activeSong = if (isRemoteActive && !syncState?.currentTitle.isNullOrBlank()) {
        YouTubeSong(
            videoId = syncState?.currentSongId ?: "",
            title = syncState?.currentTitle ?: "Remote Track",
            channelTitle = syncState?.currentArtist ?: "ISAI Connect",
            thumbnailUrl = syncState?.currentArtwork ?: "",
            audioUrl = syncState?.currentAudioUrl?.ifBlank { null }
        )
    } else {
        currentSongLocal
    }

    val isPlaying = if (isRemoteActive) (syncState?.isPlaying == true) else isPlayingLocal
    val positionSec = if (isRemoteActive) ((syncState?.positionMs ?: 0L) / 1000f) else positionSecLocal
    val durationSec = if (isRemoteActive) ((syncState?.durationMs ?: 210000L) / 1000f) else durationSecLocal

    // Smooth vinyl rotation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "MiniVinylSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miniRotation"
    )

    AnimatedVisibility(
        visible = activeSong != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        val song = activeSong ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = NeonCyan.copy(alpha = 0.35f),
                    ambientColor = NeonPurple.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkSurfaceGlass,
                            DarkSecondary
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.6f),
                            GlassBorderSubtle,
                            NeonPurple.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable {
                    Log.i("ISAI_PLAYER", "[MiniPlayer] Opened full player for: '${song.title}'")
                    viewModel.openFullPlayer()
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sleek Album Artwork Thumbnail
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = NeonCyan.copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title + Channel / 320 KBPS Tag
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = song.title,
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (isPlaying) {
                                EqualizerBars(
                                    isPlaying = true,
                                    barCount = 3,
                                    barWidth = 2.dp,
                                    maxHeight = 11.dp,
                                    activeColor = NeonCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = song.channelTitle,
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            val isMyDeviceActive = viewModel.isaiConnectManager.isMyDeviceActive()
                            if (!isMyDeviceActive) {
                                Text(
                                    text = "• 📱 Connected",
                                    color = com.saavn.music.ui.theme.IsaiLime,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            } else {
                                Text(
                                    text = "• 320K HD",
                                    color = NeonCyan.copy(alpha = 0.85f),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Direct Favorite Heart Button
                    val isFav = viewModel.isFavorite(song.videoId)
                    IconButton(
                        onClick = { viewModel.toggleFavorite(song) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFav) "Liked" else "Like",
                            tint = if (isFav) HeartColor else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Glowing Play / Pause FAB
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                spotColor = NeonCyan.copy(alpha = 0.5f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NeonCyan, NeonPurple, NeonPink)
                                )
                            )
                            .clickable {
                                viewModel.togglePlayPause()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Next button
                    IconButton(
                        onClick = { viewModel.playNext() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = TextPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Seamless Micro Progress Bar hugging the bottom
                val progress = if (durationSec > 0f) (positionSec / durationSec).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(DarkSurfaceElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(NeonCyan, NeonPurple, NeonPink)
                                )
                            )
                    )
                }
            }
        }
    }
}
