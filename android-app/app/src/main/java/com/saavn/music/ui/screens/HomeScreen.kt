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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
        ArtistItem("Anirudh Ravichander", "Composer & Singer", "https://c.saavncdn.com/artists/Anirudh_Ravichander_004_20230222091040_500x500.jpg", "Anirudh Ravichander Tamil hits"),
        ArtistItem("A.R. Rahman", "Composer & Maestro", "https://c.saavncdn.com/artists/A_R_Rahman_004_20230718070940_500x500.jpg", "A R Rahman Tamil hits"),
        ArtistItem("Yuvan Shankar Raja", "Composer & Singer", "https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_004_20220908070940_500x500.jpg", "Yuvan Shankar Raja Tamil hits"),
        ArtistItem("Harris Jayaraj", "Composer", "https://c.saavncdn.com/artists/Harris_Jayaraj_002_20200812070940_500x500.jpg", "Harris Jayaraj Tamil hits"),
        ArtistItem("Sid Sriram", "Singer", "https://c.saavncdn.com/artists/Sid_Sriram_003_20230516070940_500x500.jpg", "Sid Sriram Tamil hits")
    ),
    "telugu" to listOf(
        ArtistItem("Devi Sri Prasad", "Composer & Singer", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_002_20210608070940_500x500.jpg", "Devi Sri Prasad Telugu hits"),
        ArtistItem("Thaman S", "Music Director", "https://c.saavncdn.com/artists/Thaman_S_003_20230116070940_500x500.jpg", "Thaman S Telugu hits"),
        ArtistItem("M.M. Keeravani", "Maestro", "https://c.saavncdn.com/artists/M_M_Keeravani_002_20230314070940_500x500.jpg", "MM Keeravani Telugu hits"),
        ArtistItem("Sid Sriram", "Singer", "https://c.saavncdn.com/artists/Sid_Sriram_003_20230516070940_500x500.jpg", "Sid Sriram Telugu hits")
    ),
    "hindi" to listOf(
        ArtistItem("Arijit Singh", "Playback Singer", "https://c.saavncdn.com/artists/Arijit_Singh_002_20230323070940_500x500.jpg", "Arijit Singh Hindi hits"),
        ArtistItem("Pritam", "Composer", "https://c.saavncdn.com/artists/Pritam_003_20220608070940_500x500.jpg", "Pritam Hindi hits"),
        ArtistItem("Shreya Ghoshal", "Singer", "https://c.saavncdn.com/artists/Shreya_Ghoshal_003_20230412070940_500x500.jpg", "Shreya Ghoshal Hindi hits")
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

    var showLangMenu by remember { mutableStateOf(false) }

    val activeLang = preferredLanguages.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Tamil"
    val userName = userProfile?.displayName?.ifBlank { "JEEVA ⚡" } ?: "JEEVA ⚡"

    // Section 4: Unakkaaga Picks (6 songs)
    val picksSongs = remember(trendingSongs) { trendingSongs.take(6) }

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
                        text = "Vanakkam, $userName 👋",
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
                        text = "Paadal, artist, album thedu…",
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

        // 4. Unakkaaga Picks ✨ Section (6 recommended songs)
        item {
            Column {
                Text(
                    text = "Unakkaaga Picks ✨",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (recentlyPlayed.isNotEmpty()) "Personalized for your $activeLang taste" else "$activeLang-la Popular",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    picksSongs.forEach { song ->
                        val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                        val isFav = viewModel.isFavorite(song.videoId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.12f) else DarkSurfaceVariant)
                                .border(1.dp, if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { viewModel.playSong(song, trendingSongs) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

        // 5. New Releases Section (Pudhu {Language} Paadalgal)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pudhu $activeLang Paadalgal 🎵",
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

        // 6. Innaiku Enna Mood? Section
        item {
            Column {
                Text(
                    text = "Innaiku Enna Mood? 💫",
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

        // 7. Un Favourite Artists Section
        item {
            Column {
                Text(
                    text = if (recentlyPlayed.isNotEmpty()) "Un Favourite Artists 🎤" else "Explore $activeLang Artists 🎤",
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
                            AsyncImage(
                                model = artist.image,
                                contentDescription = artist.name,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GlassBorderSubtle, CircleShape),
                                contentScale = ContentScale.Crop
                            )
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
    }
}
