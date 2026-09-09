package com.saavn.music.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.saavn.music.ui.MainViewModel
import com.saavn.music.ui.components.EqualizerBars
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.GlassBorder
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPink
import com.saavn.music.ui.theme.HeartColor
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.SliderTrack
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary
import kotlin.random.Random

enum class PlayerVisualMode {
    VINYL,
    ALBUM_CARD,
    VIDEO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val song by viewModel.ytPlayerController.currentSong.collectAsState()
    val isPlayingLocal by viewModel.ytPlayerController.isPlaying.collectAsState()
    val isBuffering by viewModel.ytPlayerController.isBuffering.collectAsState()
    val positionSecLocal by viewModel.ytPlayerController.currentPositionSec.collectAsState()
    val durationSecLocal by viewModel.ytPlayerController.durationSec.collectAsState()
    val localVolume by viewModel.ytPlayerController.volume.collectAsState()
    val isShuffle by viewModel.ytPlayerController.isShuffle.collectAsState()
    val isRepeat by viewModel.ytPlayerController.isRepeat.collectAsState()

    val syncState by viewModel.isaiConnectManager.playbackState.collectAsState()
    val isMyDeviceActive = viewModel.isaiConnectManager.isMyDeviceActive()
    val isRemoteActive = !isMyDeviceActive && syncState != null &&
        syncState!!.currentDeviceId.isNotBlank() &&
        !syncState!!.currentTitle.isNullOrBlank() &&
        (syncState!!.isPlaying || Math.abs(System.currentTimeMillis() - syncState!!.updatedAt) < 15 * 60_000L)

    val remoteVolume = ((syncState?.volume ?: 1.0f) * 100).toInt()
    val volume = if (isRemoteActive) remoteVolume else localVolume

    val queue by viewModel.playbackQueue.collectAsState()
    val currentQueueIdx by viewModel.currentQueueIndex.collectAsState()

    val remoteQueueSongs = remember(syncState?.queue) {
        syncState?.queue?.map { syncSong ->
            com.saavn.music.data.model.YouTubeSong(
                videoId = syncSong.id,
                title = syncSong.title,
                channelTitle = syncSong.artist,
                thumbnailUrl = syncSong.artwork,
                audioUrl = syncSong.audioUrl.ifBlank { null }
            )
        } ?: emptyList()
    }
    val effectiveDisplayQueue = if (isRemoteActive && remoteQueueSongs.isNotEmpty()) {
        remoteQueueSongs
    } else {
        queue
    }
    val effectiveCurrentQueueIdx = if (isRemoteActive) {
        syncState?.queueIndex ?: 0
    } else {
        currentQueueIdx
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showConnectSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionSec by remember { mutableFloatStateOf(0f) }
    var visualMode by remember { mutableStateOf(PlayerVisualMode.ALBUM_CARD) }

    val currentSong = song ?: if (isRemoteActive) {
        com.saavn.music.data.model.YouTubeSong(
            videoId = syncState?.currentSongId ?: "",
            title = syncState?.currentTitle ?: "Remote Track",
            channelTitle = syncState?.currentArtist ?: "ISAI Connect",
            thumbnailUrl = syncState?.currentArtwork ?: "",
            audioUrl = syncState?.currentAudioUrl?.ifBlank { null }
        )
    } else null

    if (currentSong == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewModel.closeFullPlayer()
        }
        return
    }

    val isPlaying = if (isRemoteActive) (syncState?.isPlaying == true) else isPlayingLocal
    val positionSec = if (isRemoteActive) ((syncState?.positionMs ?: 0L) / 1000f) else positionSecLocal
    val durationSec = if (isRemoteActive) ((syncState?.durationMs ?: 210000L) / 1000f) else durationSecLocal
    val isFav = viewModel.isFavorite(currentSong.videoId)
    val context = LocalContext.current

    // Smooth continuous vinyl rotation
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsing aura scale
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                // Consume clicks on full player background so items on the page behind it are never selected
            }
    ) {
        // 1. Ambient Blurred Backdrop
        AsyncImage(
            model = currentSong.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.38f)
                .blur(radius = 70.dp),
            contentScale = ContentScale.Crop
        )

        // 2. Dark Vignette Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground.copy(alpha = 0.5f),
                            DarkBackground.copy(alpha = 0.88f),
                            DarkBackground
                        )
                    )
                )
        )

        // 3. Main Player Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ↓ Back Button
                IconButton(
                    onClick = { viewModel.closeFullPlayer() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceGlass)
                        .border(1.dp, GlassBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Center Title: NOW PLAYING & ISAI Audio Quality Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) com.saavn.music.ui.theme.IsaiLime else TextMuted)
                        )
                        Text(
                            text = "UN ISAI",
                            color = com.saavn.music.ui.theme.IsaiLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // ⋮ More Options Menu
                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(DarkSurfaceGlass)
                    ) {
                        DropdownMenuItem(
                            text = { Text("➕ Add to Playlist", color = TextPrimary) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.openAddToPlaylistDialog(currentSong)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🔗 Share Track", color = TextPrimary) },
                            onClick = {
                                showMoreMenu = false
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, currentSong.title)
                                    putExtra(Intent.EXTRA_TEXT, "Listening to ${currentSong.title} - ${currentSong.channelTitle} on ISAI Music!")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📥 Offline Download", color = TextPrimary) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.addToQueue(currentSong)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("👤 Go to Artist", color = TextPrimary) },
                            onClick = {
                                showMoreMenu = false
                                viewModel.onSearchQueryChanged(currentSong.channelTitle)
                                viewModel.closeFullPlayer()
                            }
                        )
                    }
                }
            }

            // Visual Showcase Center Area - Hero Album Cover Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing Aura behind Artwork Card
                Box(
                    modifier = Modifier
                        .size((205 * if (isPlaying) auraScale else 1f).dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.35f),
                                    NeonPurple.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Floating 3D Artwork Card
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.5f),
                            ambientColor = NeonPurple.copy(alpha = 0.4f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(com.saavn.music.ui.theme.IsaiLime, NeonPurple, NeonPink)),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    AsyncImage(
                        model = currentSong.thumbnailUrl,
                        contentDescription = currentSong.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Live Dancing Vibrant Full Rainbow Audio Waveform Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val barHeights = listOf(
                    14, 22, 10, 26, 18, 12, 24, 16, 22, 10,
                    25, 17, 12, 24, 15, 20, 26, 11, 23, 16,
                    19, 25, 12, 21, 14, 24, 18, 10
                )
                val rainbowColors = listOf(
                    Color(0xFFFF007A), // Neon Pink
                    Color(0xFFFF3366), // Coral Pink
                    Color(0xFFFF6600), // Electric Orange
                    Color(0xFFFFD700), // Bright Gold
                    Color(0xFFAEEA00), // Neon Lime
                    Color(0xFF00FF88), // Spring Green
                    Color(0xFF00E5FF), // Bright Turquoise
                    Color(0xFF00F0FF), // Neon Cyan
                    Color(0xFF0088FF), // Electric Blue
                    Color(0xFF7000FF), // Deep Violet
                    Color(0xFFB000FF), // Neon Purple
                    Color(0xFFFF00E5), // Vivid Magenta
                    Color(0xFFFF007A), // Neon Pink
                    Color(0xFFFF5722), // Deep Orange
                    Color(0xFFFFC107), // Gold
                    Color(0xFF76FF03), // Lime Green
                    Color(0xFF00E676), // Emerald
                    Color(0xFF00B0FF), // Sky Blue
                    Color(0xFF3D5AFE), // Indigo
                    Color(0xFFD500F9), // Purple Pink
                    Color(0xFFFF1744), // Crimson
                    Color(0xFFFFAB00), // Amber
                    Color(0xFF00E5FF), // Cyan
                    Color(0xFF651FFF), // Purple
                    Color(0xFFFF007A), // Pink
                    Color(0xFF00FF88), // Green
                    Color(0xFFFFD700), // Yellow Gold
                    Color(0xFF00F0FF)  // Cyan
                )

                barHeights.forEachIndexed { index, _ ->
                    val dynamicH = if (isPlaying) {
                        val factor = (Math.sin(rotationAngle.toDouble() * 0.12 + index * 0.4) + 1) / 2.0
                        (5 + factor * 15).coerceIn(3.0, 20.0).dp
                    } else {
                        4.dp
                    }

                    val topColor = rainbowColors[index % rainbowColors.size]
                    val bottomColor = rainbowColors[(index + 4) % rainbowColors.size]

                    Box(
                        modifier = Modifier
                            .width(3.2.dp)
                            .height(dynamicH)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(topColor, bottomColor)
                                )
                            )
                    )
                }
            }

            // Song Title, Channel, and Heart Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currentSong.channelTitle,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "✓",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite(currentSong) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isFav) HeartColor.copy(alpha = 0.18f) else DarkSurfaceGlass)
                        .border(1.dp, if (isFav) HeartColor else GlassBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) HeartColor else TextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Seek Bar & Digital Time Chips
            Column(modifier = Modifier.fillMaxWidth()) {
                val totalDuration = if (durationSec > 0f) durationSec else 210f
                val currentSec = if (isDraggingSlider) dragPositionSec else positionSec
                val sliderValue = if (totalDuration > 0f) {
                    (currentSec / totalDuration).coerceIn(0f, 1f)
                } else 0f

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isDraggingSlider = true
                        dragPositionSec = it * totalDuration
                    },
                    onValueChangeFinished = {
                        viewModel.seekTo(dragPositionSec)
                        isDraggingSlider = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = SliderTrack
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Elapsed Time Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceGlass)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = formatSeconds(currentSec),
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Total Duration Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceGlass)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = formatSeconds(totalDuration),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Flagship Media Controls (Shuffle, Prev, Compact FAB, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) NeonCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Glowing Neon FAB Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = NeonCyan.copy(alpha = 0.6f),
                            ambientColor = NeonPurple.copy(alpha = 0.4f)
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
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DarkBackground,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = { viewModel.toggleRepeat() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) NeonPink else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Compact Bottom Controls & Up Next Group
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Modern Compact Sound Volume Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceGlass)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var previousVol by remember { mutableIntStateOf(80) }
                    IconButton(
                        onClick = {
                            if (volume > 0) {
                                previousVol = volume
                                viewModel.setVolume(0)
                            } else {
                                viewModel.setVolume(previousVol.coerceAtLeast(40))
                            }
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                volume == 0 -> Icons.AutoMirrored.Filled.VolumeOff
                                volume < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = "Volume Toggle",
                            tint = if (volume > 0) NeonCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Slider(
                        value = volume.toFloat(),
                        onValueChange = { viewModel.setVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Volume Percentage Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$volume%",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ISAI Connect Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMyDeviceActive = viewModel.isaiConnectManager.isMyDeviceActive()

                    // ISAI Connect Multi-Device Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showConnectSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (!isMyDeviceActive) com.saavn.music.ui.theme.IsaiLime else DarkSurfaceGlass,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isMyDeviceActive) com.saavn.music.ui.theme.IsaiLime else NeonCyan.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = "ISAI Connect",
                                tint = if (!isMyDeviceActive) DarkBackground else com.saavn.music.ui.theme.IsaiLime,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (!isMyDeviceActive) "⚡ Connected" else "⚡ Connect Devices",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (!isMyDeviceActive) DarkBackground else com.saavn.music.ui.theme.IsaiLime
                                )
                            )
                        }
                    }
                }

                // Up Next & Queue Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.15f),
                                    DarkSurfaceGlass,
                                    NeonPink.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "UP NEXT",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            val nextSong = queue.getOrNull(currentQueueIdx + 1)
                            if (nextSong != null) {
                                Text(
                                    text = "Next: ${nextSong.title}",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonCyan.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${queue.size} Songs",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Open Queue",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // YouTube Music Style Queue & Suggestions Bottom Sheet
        if (showQueueSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = { showQueueSheet = false },
                sheetState = sheetState,
                containerColor = DarkBackground.copy(alpha = 0.98f),
                scrimColor = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(NeonCyan.copy(alpha = 0.6f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    // Single Header: Playing Queue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLAYING QUEUE (${effectiveDisplayQueue.size})",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        if (effectiveDisplayQueue.size > 1 && !isRemoteActive) {
                            Text(
                                text = "Clear Queue",
                                color = NeonPink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPink.copy(alpha = 0.15f))
                                    .clickable { viewModel.clearQueue() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GlassBorderSubtle)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (effectiveDisplayQueue.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Queue is empty", color = TextMuted, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                        ) {
                            itemsIndexed(effectiveDisplayQueue) { idx, qSong ->
                                val isCurrent = (idx == effectiveCurrentQueueIdx || qSong.videoId == currentSong.videoId)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isCurrent) NeonCyan.copy(alpha = 0.12f) else Color.Transparent
                                        )
                                        .clickable {
                                            if (isRemoteActive && !syncState?.currentDeviceId.isNullOrBlank()) {
                                                viewModel.isaiConnectManager.sendCommand(
                                                    action = "PLAY_SONG",
                                                    song = qSong,
                                                    targetDeviceId = syncState!!.currentDeviceId
                                                )
                                            } else {
                                                viewModel.playSong(qSong, effectiveDisplayQueue)
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank or Equalizer
                                    Box(
                                        modifier = Modifier.width(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrent && isPlaying) {
                                            EqualizerBars(
                                                isPlaying = true,
                                                barCount = 3,
                                                barWidth = 2.5.dp,
                                                maxHeight = 14.dp,
                                                activeColor = NeonCyan
                                            )
                                        } else {
                                            Text(
                                                text = "${idx + 1}",
                                                color = if (isCurrent) NeonCyan else TextMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = qSong.thumbnailUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = qSong.title,
                                            color = if (isCurrent) NeonCyan else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = qSong.channelTitle,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!isCurrent && !isRemoteActive) {
                                        IconButton(
                                            onClick = { viewModel.removeFromQueue(idx) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove",
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(NeonCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "PLAYING",
                                                color = NeonCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConnectSheet) {
        com.saavn.music.ui.components.IsaiConnectBottomSheet(
            connectManager = viewModel.isaiConnectManager,
            viewModel = viewModel,
            onDismissRequest = { showConnectSheet = false }
        )
    }
}

private fun formatSeconds(sec: Float): String {
    val totalSeconds = sec.toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
