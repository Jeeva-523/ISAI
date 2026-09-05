package com.saavn.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saavn.music.R
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.ui.AppScreen
import com.saavn.music.ui.MainViewModel
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurface
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.GlassBorder
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.NeonBlue
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPink
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val categorySongs by viewModel.categorySongs.collectAsState()
    val isLoading by viewModel.isLoadingHome.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentPlayingSong by viewModel.ytPlayerController.currentSong.collectAsState()
    val isPlaying by viewModel.ytPlayerController.isPlaying.collectAsState()

    val categories = listOf(
        "Trending", "Tamil Songs", "Melody", "Love Songs",
        "Folk", "Devotional", "Gaana", "Classical", "New Releases"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. Futuristic App Header with ISAI Logo
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = NeonCyan.copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "ISAI Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "ISAI",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "LISTEN • FEEL • LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // Search Action Button
                IconButton(
                    onClick = { viewModel.setScreen(AppScreen.SEARCH) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceGlass)
                        .border(1.dp, GlassBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Tamil Music Categories Pill Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = if (isSelected) 8.dp else 0.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = NeonCyan.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))
                                } else {
                                    Brush.horizontalGradient(listOf(DarkSurfaceVariant, DarkSurface))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonCyan.copy(alpha = 0.6f) else GlassBorderSubtle,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.selectCategory(cat) }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) DarkBackground else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

        // 3. Featured Hero Spotlight Cards (Top 4 Trending Tamil)
        if (trendingSongs.isNotEmpty()) {
            val featuredList = trendingSongs.take(4)
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "🔥 Trending Tamil Spotlight",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(featuredList.size) { index ->
                        val song = featuredList[index]
                        YouTubeHeroCard(
                            song = song,
                            rank = index + 1,
                            onClick = { viewModel.playSong(song, trendingSongs) }
                        )
                    }
                }
            }
        }

        // 4. Quick Hits Horizontal Cards
        if (trendingSongs.size > 4) {
            val quickPicks = trendingSongs.drop(4).take(8)
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ Popular Tamil Songs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "OFFICIAL YOUTUBE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quickPicks.size) { index ->
                        val song = quickPicks[index]
                        YouTubeQuickHitCard(
                            song = song,
                            onClick = { viewModel.playSong(song, trendingSongs) }
                        )
                    }
                }
            }
        }

        // 5. Category Song List Section Header
        item {
            Spacer(modifier = Modifier.height(26.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎵 $selectedCategory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${categorySongs.size} tracks",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 6. Loading Indicator or Song List
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                }
            }
        } else {
            itemsIndexed(categorySongs) { index, song ->
                val isThisPlaying = (currentPlayingSong?.videoId == song.videoId)
                val isFav = viewModel.isFavorite(song.videoId)
                YouTubeSongRowItem(
                    index = index + 1,
                    song = song,
                    isCurrent = isThisPlaying,
                    isPlaying = isThisPlaying && isPlaying,
                    onClick = { viewModel.playSong(song, categorySongs) },
                    onPlayPauseClick = {
                        if (isThisPlaying) {
                            viewModel.togglePlayPause()
                        } else {
                            viewModel.playSong(song, categorySongs)
                        }
                    },
                    isFav = isFav,
                    onToggleFav = { viewModel.toggleFavorite(song) },
                    onAddToPlaylist = { viewModel.openAddToPlaylistDialog(song) },
                    onAddToQueue = { viewModel.addToQueue(song) },
                    onPlayNext = { viewModel.playNextInQueue(song) }
                )
            }
        }
    }
}

@Composable
fun YouTubeHeroCard(
    song: YouTubeSong,
    rank: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(175.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = NeonPurple.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkBackground.copy(alpha = 0.6f),
                            DarkBackground.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(NeonPink, NeonPurple)))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "#$rank TRENDING",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (song.durationFormatted.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song.durationFormatted,
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Song Info + Play FAB
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.channelTitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(8.dp, CircleShape, spotColor = NeonCyan.copy(alpha = 0.5f))
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonCyan, NeonPurple))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = DarkBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeQuickHitCard(
    song: YouTubeSong,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(125.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(125.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = NeonBlue.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Mini Play Floating Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = DarkBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = song.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.channelTitle,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
