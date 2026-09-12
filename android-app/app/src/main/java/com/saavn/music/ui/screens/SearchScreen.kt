package com.saavn.music.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saavn.music.ui.AppScreen
import com.saavn.music.ui.MainViewModel
import com.saavn.music.ui.components.SongListNativeAdItem
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.GlassBorder
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.NeonCyan
import com.saavn.music.ui.theme.NeonPurple
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary

data class SearchCategory(
    val title: String,
    val subtitle: String,
    val query: String,
    val gradient: List<Color>
)

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val currentSong by viewModel.ytPlayerController.currentSong.collectAsState()
    val isPlaying by viewModel.ytPlayerController.isPlaying.collectAsState()
    val selectedLang by viewModel.searchLanguageFilter.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val syncState by viewModel.isaiConnectManager.playbackState.collectAsState()
    val isSeparateMode by viewModel.isMultiDevicePlaybackSeparate.collectAsState()
    val isMyDeviceActive = if (isSeparateMode) true else viewModel.isaiConnectManager.isMyDeviceActive()
    val isRemoteActive = !isSeparateMode && !isMyDeviceActive && syncState != null &&
        syncState!!.currentDeviceId.isNotBlank() &&
        !syncState!!.currentTitle.isNullOrBlank() &&
        (syncState!!.isPlaying || Math.abs(System.currentTimeMillis() - syncState!!.updatedAt) < 15 * 60_000L)

    val effectivePlayingId = if (isRemoteActive) syncState?.currentSongId else currentSong?.videoId
    val effectiveIsPlaying = if (isRemoteActive) (syncState?.isPlaying == true) else isPlaying

    val languages = listOf("All", "Tamil", "Hindi", "English", "Telugu", "Malayalam", "Punjabi", "Kannada")

    val quickQueries = listOf(
        "🔥 Trending",
        "💖 Kadhal Melody",
        "⚡ Anirudh Hits",
        "🎼 A. R. Rahman",
        "💔 Yuvan Sad",
        "🥁 Tamil Kuthu",
        "📻 90s Golden Hits",
        "💪 Gym Workout",
        "🌧️ Mazhai Songs",
        "🎙️ Gaana Hits",
        "🪔 Murugan Bhakti",
        "🎧 Lo-Fi Chill"
    )

    val exploreCategories = listOf(
        SearchCategory("🔥 Trending & Viral", "Top Chartbusters", "Trending hit songs", listOf(Color(0xFFE91E63), Color(0xFF9C27B0))),
        SearchCategory("💖 Kadhal & Romance", "Love & Heartfelt Melodies", "kadhal romantic melody songs", listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))),
        SearchCategory("🌧️ Sad & Heartbreak", "Emotional & Sogam Hits", "sad heartbreak emotional songs", listOf(Color(0xFF3B82F6), Color(0xFF1E40AF))),
        SearchCategory("🥁 Gaana & Kuthu", "High Energy Folk Beats", "tamil gaana kuthu songs", listOf(Color(0xFFFF5722), Color(0xFFFF9800))),
        SearchCategory("📻 90s Golden Era", "Evergreen Raaja & ARR Classics", "90s tamil evergreen hit songs", listOf(Color(0xFFA855F7), Color(0xFFEC4899))),
        SearchCategory("💪 Gym & Workout", "Pump-up Beats & Mass BGM", "gym workout motivational bgm beats", listOf(Color(0xFF10B981), Color(0xFF06B6D4))),
        SearchCategory("🌧️ Rain & Mazhai", "Soulful Monsoon Melodies", "mazhai rain melody songs", listOf(Color(0xFF00BCD4), Color(0xFF3F51B5))),
        SearchCategory("🪔 Devotional & Bhakti", "Murugan, Shiva & Temple Chants", "tamil devotional bhakti songs", listOf(Color(0xFFF97316), Color(0xFFEAB308))),
        SearchCategory("🌙 Late Night Lo-Fi", "Acoustic & Midnight Beats", "tamil lofi acoustic chill songs", listOf(Color(0xFF673AB7), Color(0xFF2A124A)))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Search Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setScreen(AppScreen.HOME) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceGlass)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Glassmorphism Input Field
            TextField(
                value = query,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "Search songs, albums, artists, podcasts…",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceGlass,
                    unfocusedContainerColor = DarkSurfaceGlass,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            )
        }

        // Language Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.width(14.dp))
            languages.forEach { lang ->
                val isSelected = (lang == selectedLang)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) NeonCyan else DarkSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else GlassBorderSubtle,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.setSearchLanguageFilter(lang) }
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = lang,
                        color = if (isSelected) DarkBackground else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Quick Search Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.width(14.dp))
            quickQueries.forEach { q ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceGlass)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                        .clickable { viewModel.onSearchQueryChanged(q) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = q,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        if (isSearching) {
            // 1. Loading State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                    Text(
                        text = "Discovering songs...",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Searching \"$query\"",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (searchError != null) {
            // 2. Error Handling
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 50.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceGlass)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = NeonCyan,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "Search Error",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = searchError ?: "Unable to fetch songs. Please verify network.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(NeonCyan, NeonPurple)
                                )
                            )
                            .clickable { viewModel.retrySearch() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Retry Search",
                            color = DarkBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (query.isNotBlank() && results.isEmpty()) {
            // 3. Empty Search Results
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 44.sp
                    )
                    Text(
                        text = "No Songs Found",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "We couldn't find any songs matching \"$query\". Check spelling or try searching another song title, artist, or album.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else if (results.isNotEmpty()) {
            // 4. Search Results Song List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        Text(
                            text = "Songs Discovered (${results.size})",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ISAI HD Music • Tap to play directly",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                itemsIndexed(results) { index, song ->
                        val isThisPlaying = (effectivePlayingId == song.videoId)
                        val isFav = viewModel.isFavorite(song.videoId)
                        YouTubeSongRowItem(
                            index = index + 1,
                            song = song,
                            isCurrent = isThisPlaying,
                            isPlaying = isThisPlaying && effectiveIsPlaying,
                            onClick = { viewModel.playSong(song) },
                            onPlayPauseClick = {
                                if (isThisPlaying) {
                                    viewModel.togglePlayPause()
                                } else {
                                    viewModel.playSong(song)
                                }
                            },
                            isFav = isFav,
                            onToggleFav = { viewModel.toggleFavorite(song) },
                            onAddToPlaylist = { viewModel.openAddToPlaylistDialog(song) },
                            onAddToQueue = { viewModel.addToQueue(song) },
                            onPlayNext = { viewModel.playNextInQueue(song) }
                        )

                        // AdMob Native Ad after every 5 songs (FREE users only, collapse on fail)
                        if ((index + 1) % 5 == 0) {
                            SongListNativeAdItem(
                                userProfile = userProfile,
                                slotIndex = (index + 1) / 5,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
        } else {
            // Browse Categories Grid
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item {
                    Text(
                        text = "✨ Explore Music",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                items(exploreCategories.chunked(2).size) { rowIndex ->
                    val pair = exploreCategories.chunked(2)[rowIndex]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = cat.gradient.first().copy(alpha = 0.4f))
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(cat.gradient))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
                                    .clickable { viewModel.onSearchQueryChanged(cat.query) }
                                    .padding(14.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Column {
                                    Text(
                                        text = cat.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = cat.subtitle,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
