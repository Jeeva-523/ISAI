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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
    val isPlaying by viewModel.ytPlayerController.isPlaying.collectAsState()
    val isBuffering by viewModel.ytPlayerController.isBuffering.collectAsState()
    val positionSec by viewModel.ytPlayerController.currentPositionSec.collectAsState()
    val durationSec by viewModel.ytPlayerController.durationSec.collectAsState()
    val volume by viewModel.ytPlayerController.volume.collectAsState()
    val isShuffle by viewModel.ytPlayerController.isShuffle.collectAsState()
    val isRepeat by viewModel.ytPlayerController.isRepeat.collectAsState()

    val queue by viewModel.playbackQueue.collectAsState()
    val currentQueueIdx by viewModel.currentQueueIndex.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoadingSuggestions by viewModel.isLoadingSuggestions.collectAsState()

    var showQueueSheet by remember { mutableStateOf(false) }
    var selectedSheetTab by remember { mutableIntStateOf(0) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionSec by remember { mutableFloatStateOf(0f) }
    var visualMode by remember { mutableStateOf(PlayerVisualMode.VINYL) }

    val currentSong = song ?: return
    val isFav = viewModel.isFavorite(currentSong.videoId)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDirectAudio = viewModel.ytPlayerController.isDirectAudio()

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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimize Button
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
                        contentDescription = "Minimize",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Center Title & Live Stream Pill
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ISAI STUDIO PLAYER",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
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
                                .background(if (isPlaying) NeonCyan else TextMuted)
                        )
                        Text(
                            text = if (isDirectAudio) "320 KBPS HD DIRECT" else "LIVE STREAM",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Add to Playlist Button
                IconButton(
                    onClick = { viewModel.openAddToPlaylistDialog(currentSong) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceGlass)
                        .border(1.dp, GlassBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Visual Mode Switcher (Vinyl vs Card vs Video)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF101322))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = if (isDirectAudio) {
                    listOf(PlayerVisualMode.VINYL to "Vinyl Disc", PlayerVisualMode.ALBUM_CARD to "Album Art")
                } else {
                    listOf(PlayerVisualMode.VINYL to "Vinyl", PlayerVisualMode.ALBUM_CARD to "Art", PlayerVisualMode.VIDEO to "Video")
                }

                modes.forEach { (mode, label) ->
                    val isSelected = visualMode == mode
                    val itemModifier = if (isSelected) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.8f), NeonPurple.copy(alpha = 0.8f))))
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Transparent)
                    }
                    Box(
                        modifier = itemModifier
                            .clickable { visualMode = mode }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }

            // Visual Showcase Center Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                contentAlignment = Alignment.Center
            ) {
                when (visualMode) {
                    PlayerVisualMode.VINYL -> {
                        // Glowing Pulsing Aura
                        Box(
                            modifier = Modifier
                                .size((270 * if (isPlaying) auraScale else 1f).dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            NeonCyan.copy(alpha = 0.35f),
                                            NeonPurple.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Realistic Concentric Grooved Vinyl Record
                        Box(
                            modifier = Modifier
                                .size(268.dp)
                                .shadow(
                                    elevation = 32.dp,
                                    shape = CircleShape,
                                    spotColor = NeonCyan.copy(alpha = 0.6f),
                                    ambientColor = NeonPurple.copy(alpha = 0.45f)
                                )
                                .clip(CircleShape)
                                .background(Color(0xFF090A10))
                                .border(
                                    3.5.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            NeonCyan,
                                            NeonPurple,
                                            NeonPink,
                                            NeonCyan
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Metallic vinyl micro grooves
                            Box(
                                modifier = Modifier
                                    .size(244.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFF22283A), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFF1B1F30), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(196.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color(0xFF252C42), CircleShape)
                            )

                            // Center Rotating Album Artwork
                            AsyncImage(
                                model = currentSong.thumbnailUrl,
                                contentDescription = currentSong.title,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .rotate(if (isPlaying) rotationAngle else 0f)
                                    .border(2.dp, NeonCyan.copy(alpha = 0.7f), CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            // Center Spindle Hole with Live Equalizer
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(DarkBackground)
                                    .border(2.dp, NeonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                EqualizerBars(
                                    isPlaying = isPlaying,
                                    barCount = 3,
                                    barWidth = 2.5.dp,
                                    maxHeight = 14.dp,
                                    activeColor = NeonCyan
                                )
                            }
                        }
                    }

                    PlayerVisualMode.ALBUM_CARD -> {
                        // Floating 3D Artwork Card
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .shadow(
                                    elevation = 28.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    spotColor = NeonCyan.copy(alpha = 0.5f),
                                    ambientColor = NeonPurple.copy(alpha = 0.4f)
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .border(
                                    2.dp,
                                    Brush.linearGradient(listOf(NeonCyan, NeonPurple, NeonPink)),
                                    RoundedCornerShape(26.dp)
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

                    PlayerVisualMode.VIDEO -> {
                        // Embedded YouTube Player View (Fallback)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black)
                                .border(1.5.dp, GlassBorder, RoundedCornerShape(18.dp))
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    YouTubePlayerView(ctx).apply {
                                        lifecycleOwner.lifecycle.addObserver(this)
                                        addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                                viewModel.ytPlayerController.registerPlayer(youTubePlayer)
                                                youTubePlayer.loadVideo(currentSong.videoId, 0f)
                                            }
                                            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                                                when (state) {
                                                    PlayerConstants.PlayerState.PLAYING -> {
                                                        viewModel.ytPlayerController.setBuffering(false)
                                                        viewModel.ytPlayerController.setPlaying(true)
                                                    }
                                                    PlayerConstants.PlayerState.PAUSED -> {
                                                        viewModel.ytPlayerController.setBuffering(false)
                                                        viewModel.ytPlayerController.setPlaying(false)
                                                    }
                                                    PlayerConstants.PlayerState.BUFFERING -> {
                                                        viewModel.ytPlayerController.setBuffering(true)
                                                    }
                                                    PlayerConstants.PlayerState.ENDED -> {
                                                        viewModel.playNext()
                                                    }
                                                    else -> viewModel.ytPlayerController.setBuffering(false)
                                                }
                                            }
                                        })
                                        tag = currentSong.videoId
                                    }
                                },
                                update = { playerView ->
                                    if (playerView.tag != currentSong.videoId) {
                                        playerView.tag = currentSong.videoId
                                        val activePlayer = viewModel.ytPlayerController.getActivePlayer()
                                        if (activePlayer != null) {
                                            activePlayer.loadVideo(currentSong.videoId, 0f)
                                        } else {
                                            playerView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                                                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                                                    viewModel.ytPlayerController.registerPlayer(youTubePlayer)
                                                    youTubePlayer.loadVideo(currentSong.videoId, 0f)
                                                }
                                            })
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Live Dancing Audio Waveform Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val barHeights = listOf(
                    14, 22, 10, 26, 18, 12, 24, 16, 22, 10,
                    25, 17, 12, 24, 15, 20, 26, 11, 23, 16,
                    19, 25, 12, 21, 14, 24, 18, 10
                )
                barHeights.forEachIndexed { index, _ ->
                    val dynamicH = if (isPlaying) {
                        val factor = (Math.sin(rotationAngle.toDouble() * 0.1 + index) + 1) / 2.0
                        (8 + factor * 18).coerceIn(4.0, 26.0).dp
                    } else {
                        4.dp
                    }
                    Box(
                        modifier = Modifier
                            .width(3.2.dp)
                            .height(dynamicH)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        NeonCyan,
                                        NeonPurple
                                    )
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
                        .background(DarkSurfaceGlass)
                        .border(1.dp, if (isFav) NeonPink else GlassBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) NeonPink else TextMuted,
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

            // Flagship Media Controls (Shuffle, Prev, Big FAB, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) NeonCyan else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = { viewModel.playPrevious() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Giant Glowing Neon FAB Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            spotColor = NeonCyan.copy(alpha = 0.7f),
                            ambientColor = NeonPurple.copy(alpha = 0.5f)
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
                            modifier = Modifier.size(38.dp),
                            strokeWidth = 3.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DarkBackground,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = { viewModel.playNext() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Repeat Button
                IconButton(
                    onClick = { viewModel.toggleRepeat() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) NeonPink else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Volume Row & Fallback YouTube Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceGlass)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = "Volume Down",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { viewModel.setVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan.copy(alpha = 0.7f),
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume Up",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Share Track Button
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, currentSong.title)
                            putExtra(Intent.EXTRA_TEXT, "Listening to ${currentSong.title} - ${currentSong.channelTitle} on ISAI Music!")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Track",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // YouTube Music Style Up Next & Queue Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.15f),
                                DarkSurfaceGlass,
                                NeonPink.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                    .clickable { showQueueSheet = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
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
                            text = "UP NEXT & QUEUE",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        val nextSong = queue.getOrNull(currentQueueIdx + 1) ?: suggestions.firstOrNull()
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
                        contentDescription = "Open Queue & Suggestions",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
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
                    // Two Tabs: Up Next / Suggestions
                    TabRow(
                        selectedTabIndex = selectedSheetTab,
                        containerColor = Color.Transparent,
                        contentColor = TextPrimary,
                        indicator = { tabPositions ->
                            if (selectedSheetTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSheetTab]),
                                    color = if (selectedSheetTab == 0) NeonCyan else NeonPink,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(GlassBorderSubtle)
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedSheetTab == 0,
                            onClick = { selectedSheetTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = if (selectedSheetTab == 0) NeonCyan else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Up Next (${queue.size})",
                                        color = if (selectedSheetTab == 0) NeonCyan else TextMuted,
                                        fontWeight = if (selectedSheetTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedSheetTab == 1,
                            onClick = { selectedSheetTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (selectedSheetTab == 1) NeonPink else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Suggestions (${suggestions.size})",
                                        color = if (selectedSheetTab == 1) NeonPink else TextMuted,
                                        fontWeight = if (selectedSheetTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedSheetTab == 0) {
                        // QUEUE VIEW
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PLAYING QUEUE",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            if (queue.size > 1) {
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

                        if (queue.isEmpty()) {
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
                                    .height(400.dp)
                            ) {
                                itemsIndexed(queue) { idx, qSong ->
                                    val isCurrent = (idx == currentQueueIdx || qSong.videoId == currentSong.videoId)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isCurrent) NeonCyan.copy(alpha = 0.12f) else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.playSong(qSong, queue)
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

                                        if (!isCurrent) {
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
                    } else {
                        // SUGGESTIONS VIEW (YouTube Music Style)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SIMILAR SONGS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            if (suggestions.isNotEmpty()) {
                                Text(
                                    text = "+ Add All to Queue",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .clickable {
                                            suggestions.forEach { viewModel.addToQueue(it) }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (isLoadingSuggestions) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = NeonPink, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Finding similar Tamil tracks...", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        } else if (suggestions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No suggestions found", color = TextMuted, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                            ) {
                                itemsIndexed(suggestions) { sIdx, sSong ->
                                    val inQueue = queue.any { it.videoId == sSong.videoId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.playSong(sSong)
                                            }
                                            .padding(horizontal = 20.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = sSong.thumbnailUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Title & Artist
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sSong.title,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = sSong.channelTitle,
                                                color = TextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // "+ Queue" action pill
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (inQueue) DarkSurfaceVariant else NeonCyan.copy(alpha = 0.2f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (inQueue) Color.Transparent else NeonCyan.copy(alpha = 0.5f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    if (!inQueue) {
                                                        viewModel.addToQueue(sSong)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (inQueue) "Added" else "+ Queue",
                                                color = if (inQueue) TextMuted else NeonCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Direct Play button
                                        IconButton(
                                            onClick = { viewModel.playSong(sSong) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(20.dp)
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
}

private fun formatSeconds(sec: Float): String {
    val totalSeconds = sec.toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
