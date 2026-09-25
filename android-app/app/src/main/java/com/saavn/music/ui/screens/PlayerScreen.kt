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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.saavn.music.data.model.AudioQuality
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
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
import com.saavn.music.ui.components.NowPlayingNativeAdCard
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurfaceElevated
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

enum class QueueTab(val label: String) {
    UP_NEXT("Up Next"),
    PLAYED("Played"),
    ALL("All Queue")
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
    val userProfile by viewModel.userProfile.collectAsState()

    val currentRoom by viewModel.listenTogetherManager.currentRoom.collectAsState()
    val inRoom = currentRoom != null
    val isListenTogetherListener = inRoom && !viewModel.listenTogetherManager.isHost()
    val roomHostName = currentRoom?.hostDeviceName ?: "Host"

    val syncState by viewModel.isaiConnectManager.playbackState.collectAsState()
    val isSeparateMode by viewModel.isMultiDevicePlaybackSeparate.collectAsState()
    val isMyDeviceActive = if (isSeparateMode) true else viewModel.isaiConnectManager.isMyDeviceActive()
    val isRemoteActive = !inRoom && !isSeparateMode && !isMyDeviceActive && syncState != null &&
        syncState!!.currentDeviceId.isNotBlank() &&
        !syncState!!.currentTitle.isNullOrBlank() &&
        (syncState!!.isPlaying || Math.abs(System.currentTimeMillis() - syncState!!.updatedAt) < 15 * 60_000L)

    val remoteVolume = ((syncState?.volume ?: 1.0f) * 100).toInt()
    val volume = if (isRemoteActive) remoteVolume else localVolume

    val queue by viewModel.playbackQueue.collectAsState()
    val currentQueueIdx by viewModel.currentQueueIndex.collectAsState()
    val uiConfig by viewModel.dynamicUiConfig.collectAsState()

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
    var showListenTogetherSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionSec by remember { mutableFloatStateOf(0f) }
    var visualMode by remember { mutableStateOf(PlayerVisualMode.ALBUM_CARD) }

    val roomSong = currentRoom?.playbackState?.song?.takeIf { it.title.isNotBlank() }?.let { rs ->
        com.saavn.music.data.model.YouTubeSong(
            videoId = rs.videoId,
            title = rs.title,
            channelTitle = rs.artist,
            thumbnailUrl = rs.artwork,
            audioUrl = rs.audioUrl.ifBlank { null }
        )
    }

    val currentSong = when {
        inRoom -> song ?: roomSong
        isRemoteActive && !syncState?.currentTitle.isNullOrBlank() -> {
            com.saavn.music.data.model.YouTubeSong(
                videoId = syncState?.currentSongId ?: "",
                title = syncState?.currentTitle ?: "Remote Track",
                channelTitle = syncState?.currentArtist ?: "ISAI Connect",
                thumbnailUrl = syncState?.currentArtwork ?: "",
                audioUrl = syncState?.currentAudioUrl?.ifBlank { null }
            )
        }
        else -> song
    }

    if (currentSong == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewModel.closeFullPlayer()
        }
        return
    }

    val activePlayingIndex = remember(effectiveDisplayQueue, effectiveCurrentQueueIdx, currentSong.videoId) {
        if (effectiveCurrentQueueIdx in effectiveDisplayQueue.indices &&
            effectiveDisplayQueue[effectiveCurrentQueueIdx].videoId == currentSong.videoId
        ) {
            effectiveCurrentQueueIdx
        } else {
            val found = effectiveDisplayQueue.indexOfFirst { it.videoId == currentSong.videoId }
            if (found != -1) found else effectiveCurrentQueueIdx
        }
    }

    val isPlaying = when {
        inRoom -> isPlayingLocal || (currentRoom?.playbackState?.state == "PLAYING")
        isRemoteActive -> (syncState?.isPlaying == true)
        else -> isPlayingLocal
    }
    val positionSec = when {
        inRoom && positionSecLocal > 0f -> positionSecLocal
        inRoom -> (viewModel.listenTogetherManager.getExpectedPosition() / 1000f)
        isRemoteActive -> ((syncState?.positionMs ?: 0L) / 1000f)
        else -> positionSecLocal
    }
    val durationSec = when {
        inRoom && durationSecLocal > 0f -> durationSecLocal
        inRoom -> ((currentRoom?.playbackState?.song?.durationMs ?: 0L) / 1000f).takeIf { it > 0f } ?: durationSecLocal
        isRemoteActive -> ((syncState?.durationMs ?: 210000L) / 1000f)
        else -> durationSecLocal
    }
    val favorites by viewModel.favorites.collectAsState()
    val isFav = favorites.any { it.videoId.trim() == currentSong.videoId.trim() }
    val context = LocalContext.current
    val currentAudioQuality by viewModel.ytPlayerController.audioQuality.collectAsState()
    var showQualityDialog by remember { mutableStateOf(false) }

    // Initial sync of volume when PlayerScreen opens
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.syncWithSystemVolume()
    }

    // Smooth continuous animations for visualizer & aura
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
        // 1. Ambient Backdrop (Hardware GPU accelerated, 0 CPU software blur)
        AsyncImage(
            model = currentSong.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.18f),
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
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back Button & NOW PLAYING Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.closeFullPlayer() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "NOW PLAYING",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }

                // Right: Action Buttons (Connected Devices + Listen Together + More Options Menu)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val isConnectedToOther = isRemoteActive

                    // Connected Device Button
                    IconButton(
                        onClick = { showConnectSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnectedToOther) com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.2f)
                                else DarkSurfaceGlass
                            )
                            .border(
                                1.dp,
                                if (isConnectedToOther) com.saavn.music.ui.theme.IsaiLime
                                else GlassBorderSubtle,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Connect Devices",
                            tint = if (isConnectedToOther) com.saavn.music.ui.theme.IsaiLime else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Listen Together Button
                    val isRoomActive = currentRoom != null
                    IconButton(
                        onClick = { showListenTogetherSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRoomActive) com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.2f)
                                else DarkSurfaceGlass
                            )
                            .border(
                                1.dp,
                                if (isRoomActive) com.saavn.music.ui.theme.IsaiLime
                                else GlassBorderSubtle,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Listen Together",
                            tint = if (isRoomActive) com.saavn.music.ui.theme.IsaiLime else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // ⋮ More Options Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceGlass)
                                .border(1.dp, GlassBorderSubtle, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
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
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (showQualityDialog) {
                AlertDialog(
                    onDismissRequest = { showQualityDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = com.saavn.music.ui.theme.IsaiLime,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "AUDIO QUALITY",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.2f))
                                    .border(1.dp, com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${currentAudioQuality.bitrate} KBPS",
                                    color = com.saavn.music.ui.theme.IsaiLime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Select streaming audio bit-rate clarity for playback",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            AudioQuality.values().forEach { quality ->
                                val isSelected = quality == currentAudioQuality
                                val (iconEmoji, accentColor, description) = when(quality) {
                                    AudioQuality.VERY_HIGH -> Triple("💎", com.saavn.music.ui.theme.IsaiLime, "Ultra HD Lossless • Studio Clarity & Deep Bass")
                                    AudioQuality.HIGH -> Triple("🎧", NeonCyan, "High Definition • Balanced Audio & Fast Buffer")
                                    AudioQuality.DATA_SAVER -> Triple("⚡", NeonPink, "Data Saver • Minimal Mobile Data Usage")
                                }
                                Surface(
                                    onClick = {
                                        viewModel.ytPlayerController.setQuality(quality)
                                        showQualityDialog = false
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) accentColor.copy(alpha = 0.18f) else DarkSurfaceGlass,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) accentColor else GlassBorderSubtle
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) accentColor.copy(alpha = 0.25f) else DarkSurfaceVariant)
                                                    .border(1.dp, if (isSelected) accentColor else Color.Transparent, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(iconEmoji, fontSize = 18.sp)
                                            }
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(
                                                        text = quality.title,
                                                        color = if (isSelected) accentColor else TextPrimary,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(accentColor.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "${quality.bitrate}k",
                                                            color = accentColor,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = description,
                                                    color = if (isSelected) TextPrimary.copy(alpha = 0.9f) else TextMuted,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("✓", color = DarkBackground, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showQualityDialog = false },
                            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                        ) {
                            Text("Done", color = com.saavn.music.ui.theme.IsaiLime, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = DarkBackground.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // Center Visual Showcase Area (takes weight(1f) to absorb extra screen height smoothly)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Clearly identifiable AdMob Native Ad placement above Artwork (FREE users only, collapses on fail)
                NowPlayingNativeAdCard(
                    userProfile = userProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )

                // Hero Album Cover Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val volumeFactor = (volume.toFloat() / 100f).coerceIn(0f, 1f)

                    // Pulsing Aura behind Artwork Card (hardware GPU scaled dynamically with volume)
                    Box(
                        modifier = Modifier
                            .size(195.dp)
                            .graphicsLayer {
                                val dynamicScale = if (isPlaying && volumeFactor > 0f) {
                                    1f + (auraScale - 1f) * volumeFactor
                                } else {
                                    1f
                                }
                                scaleX = dynamicScale
                                scaleY = dynamicScale
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.35f * volumeFactor.coerceAtLeast(0.08f)),
                                        NeonPurple.copy(alpha = 0.2f * volumeFactor.coerceAtLeast(0.05f)),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Floating 3D Artwork Card
                    Box(
                        modifier = Modifier
                            .size(200.dp)
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

                Spacer(modifier = Modifier.height(10.dp))

                // Live Dancing Vibrant Full Rainbow Audio Waveform Visualizer (Hardware-accelerated GPU Canvas, Zero Composable Overhead)
                val barHeights = remember {
                    listOf(
                        14, 22, 10, 26, 18, 12, 24, 16, 22, 10,
                        25, 17, 12, 24, 15, 20, 26, 11, 23, 16,
                        19, 25, 12, 21, 14, 24, 18, 10
                    )
                }
                val rainbowColors = remember {
                    listOf(
                        Color(0xFFFF007A), Color(0xFFFF3366), Color(0xFFFF6600), Color(0xFFFFD700),
                        Color(0xFFAEEA00), Color(0xFF00FF88), Color(0xFF00E5FF), Color(0xFF00F0FF),
                        Color(0xFF0088FF), Color(0xFF7000FF), Color(0xFFB000FF), Color(0xFFFF00E5),
                        Color(0xFFFF007A), Color(0xFFFF5722), Color(0xFFFFC107), Color(0xFF76FF03),
                        Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFF3D5AFE), Color(0xFFD500F9),
                        Color(0xFFFF1744), Color(0xFFFFAB00), Color(0xFF00E5FF), Color(0xFF651FFF),
                        Color(0xFFFF007A), Color(0xFF00FF88), Color(0xFFFFD700), Color(0xFF00F0FF)
                    )
                }

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    val totalBars = barHeights.size
                    val availableWidth = size.width
                    val barWidthPx = 3.2.dp.toPx()
                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                    val spaceBetween = if (totalBars > 1) (availableWidth - (totalBars * barWidthPx)) / (totalBars - 1) else 0f
                    val volumeFactor = (volume.toFloat() / 100f).coerceIn(0.05f, 1f)

                    barHeights.forEachIndexed { index, _ ->
                        val dynamicHPx = if (isPlaying && volume > 0) {
                            val factor = (Math.sin(rotationAngle.toDouble() * (0.07 + 0.05 * (volume.toFloat() / 100f)) + index * 0.4) + 1) / 2.0
                            val minH = (2.5f + 2.5f * volumeFactor).dp.toPx()
                            val maxH = (4f + 16f * volumeFactor).dp.toPx()
                            (minH + factor * (maxH - minH)).toFloat().coerceIn(3.dp.toPx(), 20.dp.toPx())
                        } else {
                            3.dp.toPx()
                        }

                        val x = index * (barWidthPx + spaceBetween)
                        val y = size.height - dynamicHPx

                        val topColor = rainbowColors[index % rainbowColors.size]
                        val bottomColor = rainbowColors[(index + 4) % rainbowColors.size]

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(topColor, bottomColor),
                                startY = y,
                                endY = size.height
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidthPx, dynamicHPx),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }

            // Bottom Player Control Panel (Tightly packed with fixed 8dp spacing, NO random empty gaps!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Song Title, Channel, Audio Quality, and Heart Favorite
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Line 1: Song Title + Favorite Heart Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentSong.title,
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.toggleFavorite(currentSong) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isFav) HeartColor.copy(alpha = 0.18f) else DarkSurfaceGlass)
                                .border(1.dp, if (isFav) HeartColor else GlassBorderSubtle, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) HeartColor else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Line 2: Artist Subtitle & Verified Badge (Left) + Audio Quality Chip (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = currentSong.channelTitle,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "✓",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            onClick = { showQualityDialog = true },
                            shape = CircleShape,
                            color = com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) com.saavn.music.ui.theme.IsaiLime else TextMuted)
                                )
                                Text(
                                    text = "${currentAudioQuality.bitrate} KBPS ⚙️",
                                    color = com.saavn.music.ui.theme.IsaiLime,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // Seek Bar & Digital Time Chips
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isListenTogetherListener) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF22C55E).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "👑 Host Controlled ($roomHostName) • Listener Mode",
                                color = Color(0xFF4ADE80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val totalDuration = if (durationSec > 0f) durationSec else 210f
                    val currentSec = if (isDraggingSlider) dragPositionSec else positionSec
                    val sliderValue = if (totalDuration > 0f) {
                        (currentSec / totalDuration).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            if (isListenTogetherListener) {
                                android.widget.Toast.makeText(context, "👑 Playback position is controlled by Host ($roomHostName)", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                isDraggingSlider = true
                                dragPositionSec = it * totalDuration
                            }
                        },
                        onValueChangeFinished = {
                            if (!isListenTogetherListener) {
                                viewModel.seekTo(dragPositionSec)
                                isDraggingSlider = false
                            }
                        },
                        enabled = !isListenTogetherListener,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isListenTogetherListener) TextMuted.copy(alpha = 0.5f) else NeonCyan,
                            activeTrackColor = if (isListenTogetherListener) TextMuted.copy(alpha = 0.5f) else NeonCyan,
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
                                .padding(horizontal = 8.dp, vertical = 2.dp)
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
                                .padding(horizontal = 8.dp, vertical = 2.dp)
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
                        onClick = {
                            if (isListenTogetherListener) {
                                android.widget.Toast.makeText(context, "👑 Shuffle is controlled by Host ($roomHostName)", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.toggleShuffle()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isShuffle && !isListenTogetherListener) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isListenTogetherListener) TextMuted.copy(alpha = 0.35f) else if (isShuffle) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = {
                            if (isListenTogetherListener) {
                                android.widget.Toast.makeText(context, "👑 Only Host ($roomHostName) can skip songs", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.playPrevious()
                            }
                        },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (isListenTogetherListener) TextMuted.copy(alpha = 0.4f) else TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Glowing Neon FAB Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = CircleShape,
                                spotColor = if (isListenTogetherListener) Color(0xFF22C55E).copy(alpha = 0.4f) else NeonCyan.copy(alpha = 0.6f),
                                ambientColor = NeonPurple.copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .background(
                                if (isListenTogetherListener) {
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF16A34A), Color(0xFF0D9488))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(NeonCyan, NeonPurple, NeonPink)
                                    )
                                }
                            )
                            .clickable {
                                if (isListenTogetherListener) {
                                    android.widget.Toast.makeText(context, "👑 Play/Pause is controlled by Host ($roomHostName)", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.togglePlayPause()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                modifier = Modifier.size(28.dp),
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
                        onClick = {
                            if (isListenTogetherListener) {
                                android.widget.Toast.makeText(context, "👑 Only Host ($roomHostName) can skip songs", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.playNext()
                            }
                        },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (isListenTogetherListener) TextMuted.copy(alpha = 0.4f) else TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat Button
                    IconButton(
                        onClick = {
                            if (isListenTogetherListener) {
                                android.widget.Toast.makeText(context, "👑 Repeat is controlled by Host ($roomHostName)", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.toggleRepeat()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isRepeat && !isListenTogetherListener) NeonPink.copy(alpha = 0.15f) else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isListenTogetherListener) TextMuted.copy(alpha = 0.35f) else if (isRepeat) NeonPink else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Modern Compact Sound Volume Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceGlass)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var previousVol by remember { mutableIntStateOf(80) }
                    var isDraggingVolume by remember { mutableStateOf(false) }
                    var dragVolume by remember { mutableFloatStateOf(volume.toFloat()) }
                    val currentDisplayVolume = if (isDraggingVolume) dragVolume.toInt() else volume

                    IconButton(
                        onClick = {
                            if (isListenTogetherListener) {
                                android.widget.Toast.makeText(context, "👑 Sound/Volume is controlled by Host ($roomHostName)", android.widget.Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            if (currentDisplayVolume > 0) {
                                previousVol = currentDisplayVolume
                                dragVolume = 0f
                                viewModel.setVolume(0)
                            } else {
                                val restored = previousVol.coerceAtLeast(40)
                                dragVolume = restored.toFloat()
                                viewModel.setVolume(restored)
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                currentDisplayVolume == 0 -> Icons.AutoMirrored.Filled.VolumeOff
                                currentDisplayVolume < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = "Volume Toggle",
                            tint = if (isListenTogetherListener) TextMuted else if (currentDisplayVolume > 0) NeonCyan else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Slider(
                        value = if (isDraggingVolume) dragVolume else volume.toFloat(),
                        onValueChange = { newVol ->
                            if (isListenTogetherListener) {
                                android.widget.Toast.makeText(context, "👑 Sound/Volume is controlled by Host ($roomHostName)", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                isDraggingVolume = true
                                dragVolume = newVol
                                viewModel.setVolume(newVol.toInt())
                            }
                        },
                        onValueChangeFinished = {
                            isDraggingVolume = false
                        },
                        enabled = !isListenTogetherListener,
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isListenTogetherListener) TextMuted else NeonCyan,
                            activeTrackColor = if (isListenTogetherListener) TextMuted.copy(alpha = 0.5f) else NeonCyan,
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
                            text = if (isListenTogetherListener) "👑 $currentDisplayVolume%" else "$currentDisplayVolume%",
                            color = if (isListenTogetherListener) Color(0xFF4ADE80) else NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "UP NEXT",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            val nextSong = effectiveDisplayQueue.getOrNull(activePlayingIndex + 1)
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

                    Spacer(modifier = Modifier.width(8.dp))

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
                                text = "${effectiveDisplayQueue.size} Songs",
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
            var selectedQueueTab by remember { mutableStateOf(QueueTab.UP_NEXT) }

            val safePlayingIndex = if (activePlayingIndex in effectiveDisplayQueue.indices) activePlayingIndex else 0
            val playedSongsWithIndex = remember(effectiveDisplayQueue, safePlayingIndex) {
                if (safePlayingIndex > 0 && effectiveDisplayQueue.isNotEmpty()) {
                    effectiveDisplayQueue.take(safePlayingIndex).mapIndexed { idx, s -> idx to s }
                } else emptyList()
            }
            val currentSongWithIndex = remember(effectiveDisplayQueue, safePlayingIndex) {
                if (safePlayingIndex in effectiveDisplayQueue.indices) {
                    safePlayingIndex to effectiveDisplayQueue[safePlayingIndex]
                } else null
            }
            val upNextSongsWithIndex = remember(effectiveDisplayQueue, safePlayingIndex) {
                if (safePlayingIndex >= 0 && safePlayingIndex < effectiveDisplayQueue.size - 1) {
                    effectiveDisplayQueue.drop(safePlayingIndex + 1).mapIndexed { offset, s -> (safePlayingIndex + 1 + offset) to s }
                } else emptyList()
            }

            val playQueueSong: (Int, com.saavn.music.data.model.YouTubeSong) -> Unit = { origIdx, qSong ->
                if (isListenTogetherListener) {
                    android.widget.Toast.makeText(context, "👑 Only Host ($roomHostName) can change songs", android.widget.Toast.LENGTH_SHORT).show()
                } else if (isRemoteActive && !syncState?.currentDeviceId.isNullOrBlank()) {
                    viewModel.isaiConnectManager.sendCommand(
                        action = "PLAY_SONG",
                        song = qSong,
                        targetDeviceId = syncState!!.currentDeviceId
                    )
                    viewModel.isaiConnectManager.updatePlaybackState(
                        song = qSong,
                        isPlaying = true,
                        positionMs = 0L,
                        currentDeviceId = syncState!!.currentDeviceId,
                        queueIndex = origIdx
                    )
                } else {
                    viewModel.playSong(qSong, effectiveDisplayQueue)
                }
            }

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
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Header with Queue Count and Clear Queue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Queue",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${effectiveDisplayQueue.size} songs",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (uiConfig.queueHeaderSubtitle.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "•",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = uiConfig.queueHeaderSubtitle,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (effectiveDisplayQueue.size > 1 && !isListenTogetherListener) {
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

                    // Modern Tab Selector Pills: Up Next | Played | All Queue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(QueueTab.UP_NEXT, "Up Next", upNextSongsWithIndex.size),
                            Triple(QueueTab.PLAYED, "Played", playedSongsWithIndex.size),
                            Triple(QueueTab.ALL, "All", effectiveDisplayQueue.size)
                        ).forEach { (tab, title, count) ->
                            val isSelected = selectedQueueTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) NeonCyan.copy(alpha = 0.18f)
                                        else DarkSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) NeonCyan.copy(alpha = 0.6f) else GlassBorderSubtle,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedQueueTab = tab }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = when (tab) {
                                            QueueTab.UP_NEXT -> Icons.Default.SkipNext
                                            QueueTab.PLAYED -> Icons.Default.History
                                            QueueTab.ALL -> Icons.Default.QueueMusic
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) NeonCyan else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = title,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) NeonCyan.copy(alpha = 0.25f)
                                                else DarkSurfaceGlass
                                            )
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            color = if (isSelected) NeonCyan else TextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GlassBorderSubtle)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

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
                                .height(440.dp)
                        ) {
                            when (selectedQueueTab) {
                                QueueTab.UP_NEXT -> {
                                    // 1. Now Playing Card
                                    if (currentSongWithIndex != null) {
                                        item(key = "now_playing_${currentSongWithIndex.second.videoId}") {
                                            NowPlayingQueueCard(
                                                song = currentSongWithIndex.second,
                                                isPlaying = isPlaying
                                            )
                                        }
                                    }

                                    // 2. Up Next Section Header
                                    item(key = "header_upnext") {
                                        QueueSectionHeader(
                                            title = "UP NEXT",
                                            count = upNextSongsWithIndex.size,
                                            icon = Icons.Default.SkipNext,
                                            badgeColor = NeonCyan
                                        )
                                    }

                                    // 3. Up Next Song Items
                                    if (upNextSongsWithIndex.isEmpty()) {
                                        item(key = "empty_upnext") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        imageVector = Icons.Default.SkipNext,
                                                        contentDescription = null,
                                                        tint = TextMuted.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("End of upcoming queue", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Check Played tab or search songs to add to queue", color = TextMuted, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        items(
                                            items = upNextSongsWithIndex,
                                            key = { (origIdx, s) -> "upnext_${origIdx}_${s.videoId}" }
                                        ) { (origIdx, qSong) ->
                                            QueueSongRow(
                                                originalIndex = origIdx,
                                                qSong = qSong,
                                                isCurrent = false,
                                                isPlayed = false,
                                                isPlaying = false,
                                                uiConfig = uiConfig,
                                                totalQueueSize = effectiveDisplayQueue.size,
                                                onPlay = { playQueueSong(origIdx, qSong) },
                                                onMoveUp = if (isListenTogetherListener) null else ({ viewModel.moveQueueItem(origIdx, origIdx - 1) }),
                                                onMoveDown = if (isListenTogetherListener) null else ({ viewModel.moveQueueItem(origIdx, origIdx + 1) }),
                                                onRemove = if (isListenTogetherListener) null else ({ viewModel.removeFromQueue(origIdx) })
                                            )
                                        }
                                    }
                                }

                                QueueTab.PLAYED -> {
                                    // 1. Now Playing Card
                                    if (currentSongWithIndex != null) {
                                        item(key = "now_playing_${currentSongWithIndex.second.videoId}") {
                                            NowPlayingQueueCard(
                                                song = currentSongWithIndex.second,
                                                isPlaying = isPlaying
                                            )
                                        }
                                    }

                                    // 2. Played Songs Section Header
                                    item(key = "header_played") {
                                        QueueSectionHeader(
                                            title = "PLAYED SONGS",
                                            count = playedSongsWithIndex.size,
                                            icon = Icons.Default.History,
                                            badgeColor = Color(0xFFA78BFA)
                                        )
                                    }

                                    // 3. Played Song Items
                                    if (playedSongsWithIndex.isEmpty()) {
                                        item(key = "empty_played") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        imageVector = Icons.Default.History,
                                                        contentDescription = null,
                                                        tint = TextMuted.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text("No played songs yet", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Songs played during this session will appear here", color = TextMuted, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        items(
                                            items = playedSongsWithIndex,
                                            key = { (origIdx, s) -> "played_${origIdx}_${s.videoId}" }
                                        ) { (origIdx, qSong) ->
                                            QueueSongRow(
                                                originalIndex = origIdx,
                                                qSong = qSong,
                                                isCurrent = false,
                                                isPlayed = true,
                                                isPlaying = false,
                                                uiConfig = uiConfig,
                                                totalQueueSize = effectiveDisplayQueue.size,
                                                onPlay = { playQueueSong(origIdx, qSong) },
                                                onMoveUp = if (isListenTogetherListener) null else ({ viewModel.moveQueueItem(origIdx, origIdx - 1) }),
                                                onMoveDown = if (isListenTogetherListener) null else ({ viewModel.moveQueueItem(origIdx, origIdx + 1) }),
                                                onRemove = if (isListenTogetherListener) null else ({ viewModel.removeFromQueue(origIdx) })
                                            )
                                        }
                                    }
                                }

                                QueueTab.ALL -> {
                                    // 1. Played Section (if any)
                                    if (playedSongsWithIndex.isNotEmpty()) {
                                        item(key = "header_all_played") {
                                            QueueSectionHeader(
                                                title = "PLAYED SONGS",
                                                count = playedSongsWithIndex.size,
                                                icon = Icons.Default.History,
                                                badgeColor = Color(0xFFA78BFA)
                                            )
                                        }
                                        items(
                                            items = playedSongsWithIndex,
                                            key = { (origIdx, s) -> "all_played_${origIdx}_${s.videoId}" }
                                        ) { (origIdx, qSong) ->
                                            QueueSongRow(
                                                originalIndex = origIdx,
                                                qSong = qSong,
                                                isCurrent = false,
                                                isPlayed = true,
                                                isPlaying = false,
                                                uiConfig = uiConfig,
                                                totalQueueSize = effectiveDisplayQueue.size,
                                                onPlay = { playQueueSong(origIdx, qSong) },
                                                onMoveUp = if (isListenTogetherListener) null else ({ viewModel.moveQueueItem(origIdx, origIdx - 1) }),
                                                onMoveDown = if (isListenTogetherListener) null else ({ viewModel.moveQueueItem(origIdx, origIdx + 1) }),
                                                onRemove = if (isListenTogetherListener) null else ({ viewModel.removeFromQueue(origIdx) })
                                            )
                                        }
                                    }

                                    // 2. Now Playing Card
                                    if (currentSongWithIndex != null) {
                                        item(key = "all_now_playing_${currentSongWithIndex.second.videoId}") {
                                            NowPlayingQueueCard(
                                                song = currentSongWithIndex.second,
                                                isPlaying = isPlaying
                                            )
                                        }
                                    }

                                    // 3. Up Next Section (if any)
                                    if (upNextSongsWithIndex.isNotEmpty()) {
                                        item(key = "header_all_upnext") {
                                            QueueSectionHeader(
                                                title = "UP NEXT",
                                                count = upNextSongsWithIndex.size,
                                                icon = Icons.Default.SkipNext,
                                                badgeColor = NeonCyan
                                            )
                                        }
                                        items(
                                            items = upNextSongsWithIndex,
                                            key = { (origIdx, s) -> "all_upnext_${origIdx}_${s.videoId}" }
                                        ) { (origIdx, qSong) ->
                                            QueueSongRow(
                                                originalIndex = origIdx,
                                                qSong = qSong,
                                                isCurrent = false,
                                                isPlayed = false,
                                                isPlaying = false,
                                                uiConfig = uiConfig,
                                                totalQueueSize = effectiveDisplayQueue.size,
                                                onPlay = { playQueueSong(origIdx, qSong) },
                                                onMoveUp = if (isListenTogetherListener) null else ({ viewModel.moveQueueItem(origIdx, origIdx - 1) }),
                                                onMoveDown = if (isListenTogetherListener) null else ({ viewModel.moveQueueItem(origIdx, origIdx + 1) }),
                                                onRemove = if (isListenTogetherListener) null else ({ viewModel.removeFromQueue(origIdx) })
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

    if (showListenTogetherSheet) {
        com.saavn.music.ui.components.ListenTogetherBottomSheet(
            listenManager = viewModel.listenTogetherManager,
            currentSong = currentSong,
            onDismissRequest = { showListenTogetherSheet = false }
        )
    }
}

private fun formatSeconds(sec: Float): String {
    val totalSeconds = sec.toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
private fun QueueSectionHeader(
    title: String,
    count: Int? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badgeColor: Color = NeonCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        if (count != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$count",
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NowPlayingQueueCard(
    song: com.saavn.music.data.model.YouTubeSong,
    isPlaying: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "NOW PLAYING",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            NeonCyan.copy(alpha = 0.16f),
                            DarkSurfaceElevated.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(26.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    EqualizerBars(
                        isPlaying = true,
                        barCount = 3,
                        barWidth = 2.5.dp,
                        maxHeight = 14.dp,
                        activeColor = NeonCyan
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.channelTitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NeonCyan.copy(alpha = 0.22f))
                    .border(0.5.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isPlaying) "PLAYING" else "PAUSED",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun QueueSongRow(
    originalIndex: Int,
    qSong: com.saavn.music.data.model.YouTubeSong,
    isCurrent: Boolean,
    isPlayed: Boolean,
    isPlaying: Boolean,
    uiConfig: com.saavn.music.data.model.DynamicUiConfig,
    totalQueueSize: Int,
    onPlay: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val accentColor = uiConfig.parseAccentColor(NeonCyan)
    val deleteColor = uiConfig.parseDeleteColor(NeonPink)
    val btnSize = uiConfig.capsulePillSize.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) NeonCyan.copy(alpha = 0.10f) else Color.Transparent
            )
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading indicator
        Box(
            modifier = Modifier.width(26.dp),
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
            } else if (isPlayed) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Played",
                    tint = TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Text(
                    text = "${originalIndex + 1}",
                    color = if (isCurrent) NeonCyan else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

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

        Spacer(modifier = Modifier.width(10.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = qSong.title,
                    color = if (isCurrent) NeonCyan else if (isPlayed) TextPrimary.copy(alpha = 0.75f) else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "PLAYING",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Text(
                text = qSong.channelTitle,
                color = if (isPlayed) TextMuted.copy(alpha = 0.8f) else TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        if (isPlayed) {
            // Replay Button for Played Songs
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeonCyan.copy(alpha = 0.12f))
                    .border(0.5.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable { onPlay() }
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        tint = NeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Replay",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (onMoveUp != null || onMoveDown != null || onRemove != null) {
            // Server-Driven Dynamic Queue Actions (Live Configurable via Firebase)
            when (uiConfig.queueActionStyle) {
                "three_dots" -> {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(btnSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            if (uiConfig.enableQueueReorder && onMoveUp != null && originalIndex > 0) {
                                DropdownMenuItem(
                                    text = { Text("Move Up", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null, tint = accentColor) },
                                    onClick = {
                                        showMenu = false
                                        onMoveUp()
                                    }
                                )
                            }
                            if (uiConfig.enableQueueReorder && onMoveDown != null && originalIndex < totalQueueSize - 1) {
                                DropdownMenuItem(
                                    text = { Text("Move Down", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = accentColor) },
                                    onClick = {
                                        showMenu = false
                                        onMoveDown()
                                    }
                                )
                            }
                            if (uiConfig.enableQueueDelete && onRemove != null) {
                                DropdownMenuItem(
                                    text = { Text("Remove from Queue", color = deleteColor) },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = deleteColor) },
                                    onClick = {
                                        showMenu = false
                                        onRemove()
                                    }
                                )
                            }
                        }
                    }
                }
                "minimal_delete" -> {
                    if (uiConfig.enableQueueDelete && onRemove != null) {
                        Box(
                            modifier = Modifier
                                .size(btnSize)
                                .clip(CircleShape)
                                .background(deleteColor.copy(alpha = 0.16f))
                                .border(0.5.dp, deleteColor.copy(alpha = 0.4f), CircleShape)
                                .clickable { onRemove() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove from queue",
                                tint = deleteColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
                else -> {
                    // Modern Capsule Pill (default "capsule_pill")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF1E172E),
                                        Color(0xFF140F22)
                                    )
                                )
                            )
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    ) {
                        if (uiConfig.enableQueueReorder && (onMoveUp != null || onMoveDown != null)) {
                            val canUp = onMoveUp != null && originalIndex > 0
                            val canDown = onMoveDown != null && originalIndex < totalQueueSize - 1

                            // Move Up button
                            Box(
                                modifier = Modifier
                                    .size(btnSize)
                                    .clip(CircleShape)
                                    .background(if (canUp) DarkSurfaceGlass else Color.Transparent)
                                    .clickable(enabled = canUp) { onMoveUp?.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    tint = if (canUp) accentColor else TextMuted.copy(alpha = 0.25f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            // Move Down button
                            Box(
                                modifier = Modifier
                                    .size(btnSize)
                                    .clip(CircleShape)
                                    .background(if (canDown) DarkSurfaceGlass else Color.Transparent)
                                    .clickable(enabled = canDown) { onMoveDown?.invoke() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    tint = if (canDown) accentColor else TextMuted.copy(alpha = 0.25f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (uiConfig.enableQueueReorder && uiConfig.enableQueueDelete && onRemove != null && (onMoveUp != null || onMoveDown != null)) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color(0x33FFFFFF))
                            )
                        }

                        if (uiConfig.enableQueueDelete && onRemove != null) {
                            // Remove (Delete) button
                            Box(
                                modifier = Modifier
                                    .size(btnSize)
                                    .clip(CircleShape)
                                    .background(deleteColor.copy(alpha = 0.16f))
                                    .border(0.5.dp, deleteColor.copy(alpha = 0.4f), CircleShape)
                                    .clickable { onRemove() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove from queue",
                                    tint = deleteColor,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
