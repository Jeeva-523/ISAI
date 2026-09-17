package com.saavn.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.saavn.music.ui.AppScreen
import com.saavn.music.ui.MainViewModel
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.HeartColor
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary

data class ArtistItem(
    val name: String,
    val role: String,
    val image: String,
    val query: String
)

private val POPULAR_ARTISTS_BY_LANG = mapOf(
    "tamil" to listOf(
        ArtistItem("Anirudh Ravichander", "Composer & Singer", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Anirudh_Ravichander_at_Audi_R8_LMX_launch.jpg/480px-Anirudh_Ravichander_at_Audi_R8_LMX_launch.jpg", "Anirudh Ravichander Tamil hits"),
        ArtistItem("A.R. Rahman", "Composer & Maestro", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9c/A._R._Rahman_WM2016.jpg/480px-A._R._Rahman_WM2016.jpg", "A R Rahman Tamil hits"),
        ArtistItem("Yuvan Shankar Raja", "Composer & Singer", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/Yuvan_Shankar_Raja.jpg/480px-Yuvan_Shankar_Raja.jpg", "Yuvan Shankar Raja Tamil hits"),
        ArtistItem("Harris Jayaraj", "Composer", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Harris_Jayaraj_at_Irandaam_Ulagam_Audio_Launch.jpg/480px-Harris_Jayaraj_at_Irandaam_Ulagam_Audio_Launch.jpg", "Harris Jayaraj Tamil hits"),
        ArtistItem("Sid Sriram", "Singer", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Sid_Sriram_at_Enai_Noki_Paayum_Thota_Audio_Launch.jpg/480px-Sid_Sriram_at_Enai_Noki_Paayum_Thota_Audio_Launch.jpg", "Sid Sriram Tamil hits")
    ),
    "telugu" to listOf(
        ArtistItem("Devi Sri Prasad", "Composer & Singer", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Devi_Sri_Prasad.jpg/480px-Devi_Sri_Prasad.jpg", "Devi Sri Prasad Telugu hits"),
        ArtistItem("Thaman S", "Music Director", "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/S_Thaman.jpg/480px-S_Thaman.jpg", "Thaman S Telugu hits"),
        ArtistItem("M.M. Keeravani", "Maestro", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/MM_Keeravani_2023.jpg/480px-MM_Keeravani_2023.jpg", "MM Keeravani Telugu hits"),
        ArtistItem("Sid Sriram", "Singer", "https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Sid_Sriram_at_Enai_Noki_Paayum_Thota_Audio_Launch.jpg/480px-Sid_Sriram_at_Enai_Noki_Paayum_Thota_Audio_Launch.jpg", "Sid Sriram Telugu hits")
    ),
    "hindi" to listOf(
        ArtistItem("Arijit Singh", "Playback Singer", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/Arijit_Singh_5th_GiMA_Awards.jpg/480px-Arijit_Singh_5th_GiMA_Awards.jpg", "Arijit Singh Hindi hits"),
        ArtistItem("Pritam", "Composer", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Pritam_Chakraborty.jpg/480px-Pritam_Chakraborty.jpg", "Pritam Hindi hits"),
        ArtistItem("Shreya Ghoshal", "Singer", "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Shreya_Ghoshal_at_FCAT.jpg/480px-Shreya_Ghoshal_at_FCAT.jpg", "Shreya Ghoshal Hindi hits")
    )
)

private val SUPPORTED_LANGUAGES = listOf("Tamil", "Telugu", "Hindi", "Kannada", "Malayalam", "English")

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentPlayingSong by viewModel.ytPlayerController.currentSong.collectAsState()
    val isPlaying by viewModel.ytPlayerController.isPlaying.collectAsState()
    val preferredLanguages by viewModel.preferredLanguages.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isLoadingHome by viewModel.isLoadingHome.collectAsState()

    var showLangMenu by remember { mutableStateOf(false) }

    val activeLang = preferredLanguages.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Tamil"
    val userName = userProfile?.displayName?.ifBlank { "JEEVA ⚡" } ?: "JEEVA ⚡"

    // Section 4: Picks For You (10 recommended songs)
    val picksSongs = remember(trendingSongs) { trendingSongs.take(10) }

    // Section 5: New Releases (rolling recent releases)
    val verifiedNewReleases = remember(trendingSongs) { trendingSongs.drop(6).take(12) }

    val displayArtists = POPULAR_ARTISTS_BY_LANG[activeLang.lowercase()]
        ?: POPULAR_ARTISTS_BY_LANG["tamil"]!!

    val moodCards = listOf(
        Triple("Love 💖", "${activeLang} love romantic hit songs", Brush.linearGradient(listOf(Color(0xFFEC4899).copy(alpha = 0.3f), Color(0xFF8B5CF6).copy(alpha = 0.3f)))),
        Triple("Chill ☕", "${activeLang} lo-fi chill rain songs", Brush.linearGradient(listOf(Color(0xFF6366F1).copy(alpha = 0.3f), Color(0xFFA855F7).copy(alpha = 0.3f)))),
        Triple("Gym ⚡", "${activeLang} energetic gym workout bgm beats", Brush.linearGradient(listOf(Color(0xFF10B981).copy(alpha = 0.3f), Color(0xFF06B6D4).copy(alpha = 0.3f)))),
        Triple("Travel 🚗", "${activeLang} road trip travel songs", Brush.linearGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.3f), Color(0xFFEF4444).copy(alpha = 0.3f))))
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // 1. Header Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome, $userName 👋",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Enjoying $activeLang Music on ISAI",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Compact Language Selector Dropdown Button
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.15f))
                                .border(1.dp, com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable { showLangMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = com.saavn.music.ui.theme.IsaiLime,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeLang,
                                    color = com.saavn.music.ui.theme.IsaiLime,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = com.saavn.music.ui.theme.IsaiLime,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false },
                            modifier = Modifier.background(DarkSurfaceGlass)
                        ) {
                            SUPPORTED_LANGUAGES.forEach { lang ->
                                val isSelected = lang.equals(activeLang, ignoreCase = true)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isSelected) "$lang ✓" else lang,
                                            color = if (isSelected) com.saavn.music.ui.theme.IsaiLime else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setPreferredLanguages(listOf(lang.lowercase()))
                                        showLangMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Profile Icon Button
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.PROFILE) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Search Section (Full-width Search Bar)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                    .clickable { viewModel.setScreen(AppScreen.SEARCH) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = com.saavn.music.ui.theme.IsaiLime,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search songs, artists, albums…",
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Continue Listening Section (Hidden when history is empty)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Continue Listening 🎧",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recentlyPlayed) { song ->
                            val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { viewModel.playSong(song, recentlyPlayed) },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            if (isThisPlaying) 2.dp else 1.dp,
                                            if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle,
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = song.title,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Picks For You ✨ Section (YouTube Music Style Cards)
        item {
            Column {
                Text(
                    text = "Picks For You ✨",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (recentlyPlayed.isNotEmpty()) "Personalized for your $activeLang taste" else "Popular in $activeLang",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(picksSongs) { song ->
                        val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                        val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { viewModel.playSong(song, picksSongs) },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        if (isThisPlaying) 2.dp else 1.dp,
                                        if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle,
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable { viewModel.toggleFavorite(song) }
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFav) HeartColor else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                if (isThisPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Playing",
                                            tint = com.saavn.music.ui.theme.IsaiLime,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = song.title,
                                color = if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
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
                }
            }
        }

        // 5. New Releases Section (New {Language} Songs)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "New $activeLang Songs 🎵",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Latest releases (Last 30 Days)",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    TextButton(onClick = { viewModel.selectCategory("$activeLang new releases 2026") }) {
                        Text(
                            text = "See all",
                            color = com.saavn.music.ui.theme.IsaiLime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(verifiedNewReleases) { song ->
                        val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { viewModel.playSong(song, verifiedNewReleases) },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        if (isThisPlaying) 2.dp else 1.dp,
                                        if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle,
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        color = com.saavn.music.ui.theme.IsaiLime,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = song.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
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
                }
            }
        }

        // 6. What's Your Mood Today? Section
        item {
            Column {
                Text(
                    text = "What's Your Mood Today? 💫",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    moodCards.forEach { (title, query, brush) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(brush)
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { viewModel.selectCategory(query) }
                                .padding(12.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // 7. Your Favorite Artists Section
        item {
            Column {
                Text(
                    text = if (recentlyPlayed.isNotEmpty()) "Your Favorite Artists 🎤" else "Explore $activeLang Artists 🎤",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(displayArtists) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(90.dp)
                                .clickable { viewModel.selectCategory(artist.query) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(com.saavn.music.ui.theme.NeonPurple, com.saavn.music.ui.theme.NeonPink)
                                        )
                                    )
                                    .border(2.dp, GlassBorderSubtle, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artist.name.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                                AsyncImage(
                                    model = artist.image,
                                    contentDescription = artist.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artist.name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 8. Most Played Songs Section (Adhigam Ketta Paadalgal 🔥)
        item {
            Column {
                val mostPlayedList = remember(trendingSongs) { trendingSongs.take(25) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Most Played Songs 🔥",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Top played hits by ISAI listeners (${mostPlayedList.size} songs)",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshMostPlayed() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .border(1.dp, GlassBorderSubtle, CircleShape)
                    ) {
                        if (isLoadingHome) {
                            CircularProgressIndicator(
                                color = com.saavn.music.ui.theme.IsaiLime,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Most Played",
                                tint = com.saavn.music.ui.theme.IsaiLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    mostPlayedList.forEachIndexed { index, song ->
                        val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                        val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.12f) else DarkSurfaceVariant)
                                .border(1.dp, if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { viewModel.playSong(song, mostPlayedList) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.saavn.music.ui.theme.IsaiLime)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    color = DarkBackground,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
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
                            IconButton(
                                onClick = { viewModel.toggleFavorite(song) }
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) HeartColor else TextMuted,
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
