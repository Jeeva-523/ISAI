package com.saavn.music.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.ui.components.EqualizerBars
import com.saavn.music.ui.theme.BronzeRank
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.GoldRank
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPink
import com.saavn.music.ui.theme.SilverRank
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext

@Composable
fun YouTubeSongRowItem(
    index: Int,
    song: YouTubeSong,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPlayPauseClick: (() -> Unit)? = null,
    showPlayButton: Boolean = false,
    isFav: Boolean = false,
    onToggleFav: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null
) {
    val rankColor = when (index) {
        1 -> GoldRank
        2 -> SilverRank
        3 -> BronzeRank
        else -> TextMuted
    }

    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable {
                    Log.i("ISAI_PLAYER", "[YouTubeSongRowItem] Row clicked: '${song.title}' (${song.videoId})")
                    onClick()
                } else Modifier
            )
            .background(
                Brush.horizontalGradient(
                    if (isCurrent) {
                        listOf(
                            NeonCyan.copy(alpha = 0.15f),
                            DarkSurfaceVariant.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    } else {
                        listOf(Color.Transparent, Color.Transparent)
                    }
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Rank or Equalizer
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent && isPlaying) {
                EqualizerBars(
                    isPlaying = true,
                    barCount = 3,
                    barWidth = 2.5.dp,
                    maxHeight = 16.dp,
                    activeColor = NeonCyan
                )
            } else {
                Text(
                    text = String.format("%02d", index),
                    color = if (isCurrent) NeonCyan else rankColor,
                    fontSize = 13.sp,
                    fontWeight = if (index <= 3 || isCurrent) FontWeight.ExtraBold else FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isCurrent) 1.5.dp else 0.5.dp,
                    color = if (isCurrent) NeonCyan else GlassBorderSubtle,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) NeonCyan else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.channelTitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (song.viewCountFormatted.isNotBlank()) {
                    Text(
                        text = " • ${song.viewCountFormatted}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration
        Text(
            text = song.durationFormatted,
            color = TextMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Play Button
        IconButton(
            onClick = {
                Log.i("ISAI_PLAYER", "[YouTubeSongRowItem] Play/Pause icon clicked: '${song.title}' (${song.videoId}), isCurrent=$isCurrent, isPlaying=$isPlaying")
                (onPlayPauseClick ?: onClick)?.invoke()
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isCurrent && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isCurrent && isPlaying) "Pause" else "Play",
                tint = if (isCurrent) NeonCyan else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // More Options Menu
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                if (onToggleFav != null) {
                    DropdownMenuItem(
                        text = { Text(if (isFav) "Remove Favorite" else "Add to Favorites", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFav) NeonPink else TextMuted
                            )
                        },
                        onClick = {
                            onToggleFav()
                            showMenu = false
                        }
                    )
                }

                if (onPlayNext != null) {
                    DropdownMenuItem(
                        text = { Text("Play Next", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        },
                        onClick = {
                            onPlayNext()
                            showMenu = false
                        }
                    )
                }

                if (onAddToQueue != null) {
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        },
                        onClick = {
                            onAddToQueue()
                            showMenu = false
                        }
                    )
                }

                if (onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = TextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        },
                        onClick = {
                            onAddToPlaylist()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
