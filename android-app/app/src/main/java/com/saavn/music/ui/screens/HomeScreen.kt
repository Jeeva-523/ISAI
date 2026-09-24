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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.saavn.music.ui.AppScreen
import com.saavn.music.ui.MainViewModel
import com.saavn.music.util.RelevanceEngine
import com.saavn.music.ui.theme.DarkBackground
import com.saavn.music.ui.theme.DarkSurfaceGlass
import com.saavn.music.ui.theme.DarkSurfaceVariant
import com.saavn.music.ui.theme.GlassBorderSubtle
import com.saavn.music.ui.theme.HeartColor
import com.saavn.music.ui.theme.IsaiLime
import com.saavn.music.ui.theme.TextMuted
import com.saavn.music.ui.theme.TextPrimary
import com.saavn.music.ui.theme.TextSecondary

data class ArtistItem(
    val name: String,
    val role: String,
    val image: String,
    val query: String
)

data class MoodCardItem(
    val title: String,
    val query: String,
    val brush: Brush,
    val imageUrl: String
)

private val POPULAR_ARTISTS_BY_LANG = mapOf(
    "tamil" to listOf(
        ArtistItem("Anirudh Ravichander", "Composer & Singer", "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg", "Anirudh Ravichander Tamil hits"),
        ArtistItem("A.R. Rahman", "Composer & Maestro", "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg", "A R Rahman Tamil hits"),
        ArtistItem("Yuvan Shankar Raja", "Composer & Singer", "https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_002_20180802174245_500x500.jpg", "Yuvan Shankar Raja Tamil hits"),
        ArtistItem("Harris Jayaraj", "Composer", "https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg", "Harris Jayaraj Tamil hits"),
        ArtistItem("Sid Sriram", "Singer", "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg", "Sid Sriram Tamil hits")
    ),
    "telugu" to listOf(
        ArtistItem("Devi Sri Prasad", "Composer & Singer", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg", "Devi Sri Prasad Telugu hits"),
        ArtistItem("Thaman S", "Music Director", "https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg", "Thaman S Telugu hits"),
        ArtistItem("M.M. Keeravani", "Maestro", "https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg", "MM Keeravani Telugu hits"),
        ArtistItem("Sid Sriram", "Singer", "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg", "Sid Sriram Telugu hits")
    ),
    "hindi" to listOf(
        ArtistItem("Arijit Singh", "Playback Singer", "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg", "Arijit Singh Hindi hits"),
        ArtistItem("Pritam", "Composer", "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg", "Pritam Hindi hits"),
        ArtistItem("Shreya Ghoshal", "Singer", "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg", "Shreya Ghoshal Hindi hits")
    ),
    "malayalam" to listOf(
        ArtistItem("Sushin Shyam", "Composer & Singer", "https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg", "Sushin Shyam Malayalam hits"),
        ArtistItem("Shaan Rahman", "Composer", "https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg", "Shaan Rahman Malayalam hits")
    ),
    "kannada" to listOf(
        ArtistItem("Ravi Basrur", "Composer", "https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg", "Ravi Basrur Kannada hits"),
        ArtistItem("Vijay Prakash", "Singer", "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg", "Vijay Prakash Kannada hits")
    ),
    "english" to listOf(
        ArtistItem("Ed Sheeran", "Singer-Songwriter", "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg", "Ed Sheeran top popular songs"),
        ArtistItem("Taylor Swift", "Pop Icon", "https://c.saavncdn.com/artists/Taylor_Swift_003_20200226074119_500x500.jpg", "Taylor Swift top hit songs")
    )
)

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val rawPicksSongs by viewModel.picksSongs.collectAsState()
    val rawLatestReleases by viewModel.latestReleases.collectAsState()
    val rawMostPlayed by viewModel.mostPlayedSongs.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentPlayingSong by viewModel.ytPlayerController.currentSong.collectAsState()
    val isPlaying by viewModel.ytPlayerController.isPlaying.collectAsState()
    val preferredLanguages by viewModel.preferredLanguages.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isLoadingHome by viewModel.isLoadingHome.collectAsState()

    var showListenTogetherSheet by remember { mutableStateOf(false) }
    val currentRoom by viewModel.listenTogetherManager.currentRoom.collectAsState()

    val activeLang = RelevanceEngine.formatLanguageDisplayName(preferredLanguages.firstOrNull())
    val userName = userProfile?.displayName?.trim()?.takeIf { it.isNotBlank() && !it.equals("JEEVA ⚡", ignoreCase = true) }
        ?: "Listener"

    val activeLangs = remember(preferredLanguages) {
        preferredLanguages.map { RelevanceEngine.normalizeLanguage(it) }.ifEmpty { listOf("tamil") }
    }

    val tamilFallback = remember(activeLangs) {
        if (activeLangs.contains("tamil")) com.saavn.music.data.repository.YouTubeMusicRepository.CURATED_TAMIL_SONGS else emptyList()
    }

    // Section 4: Picks For You (Authoritative / Personalized distinct pool - Strictly in active language, min 30-35 songs)
    val picksSongs = remember(rawPicksSongs, trendingSongs, activeLangs, tamilFallback) {
        val validPicks = rawPicksSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val validTrending = trendingSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val pool = (validPicks + validTrending + tamilFallback).distinctBy { it.videoId }
        pool.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }.take(35)
    }

    // Section 5: New Releases (Authentic recent singles pool - Strictly in active language, min 30-35 songs)
    val verifiedNewReleases = remember(rawLatestReleases, trendingSongs, activeLangs, tamilFallback) {
        val validNew = rawLatestReleases.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val validTrending = trendingSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val pool = (validNew + validTrending.drop(5) + tamilFallback).distinctBy { it.videoId }
        pool.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }.take(35)
    }

    // Section 8: Most Played Songs pool cached
    val mostPlayedList = remember(rawMostPlayed, trendingSongs, activeLangs, tamilFallback) {
        val validMost = rawMostPlayed.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val validTrending = trendingSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val pool = (validMost + validTrending + tamilFallback).distinctBy { it.videoId }
        pool.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }.take(35)
    }

    val displayArtists = POPULAR_ARTISTS_BY_LANG[activeLang.lowercase()]
        ?: POPULAR_ARTISTS_BY_LANG["tamil"]!!

    val moodCards = remember(activeLang) {
        listOf(
            MoodCardItem(
                "Love 💖",
                "${activeLang} love romantic hit songs",
                Brush.linearGradient(listOf(Color(0xFFEC4899).copy(alpha = 0.45f), Color(0xFF8B5CF6).copy(alpha = 0.45f))),
                "https://images.unsplash.com/photo-1518199266791-5375a83190b7?auto=format&fit=crop&w=600&q=80"
            ),
            MoodCardItem(
                "Chill ☕",
                "${activeLang} lo-fi chill rain songs",
                Brush.linearGradient(listOf(Color(0xFF6366F1).copy(alpha = 0.45f), Color(0xFFA855F7).copy(alpha = 0.45f))),
                "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80"
            ),
            MoodCardItem(
                "Gym ⚡",
                "${activeLang} energetic gym workout bgm beats",
                Brush.linearGradient(listOf(Color(0xFF10B981).copy(alpha = 0.45f), Color(0xFF06B6D4).copy(alpha = 0.45f))),
                "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=600&q=80"
            ),
            MoodCardItem(
                "Party 🎉",
                "${activeLang} party dance kuthu fast beat songs",
                Brush.linearGradient(listOf(Color(0xFFF43F5E).copy(alpha = 0.45f), Color(0xFFF97316).copy(alpha = 0.45f))),
                "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=600&q=80"
            ),
            MoodCardItem(
                "Travel 🚗",
                "${activeLang} road trip travel songs",
                Brush.linearGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.45f), Color(0xFFEF4444).copy(alpha = 0.45f))),
                "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=600&q=80"
            ),
            MoodCardItem(
                "Sad 🌧️",
                "${activeLang} sad emotional heartbreak breakup songs",
                Brush.linearGradient(listOf(Color(0xFF3B82F6).copy(alpha = 0.45f), Color(0xFF6366F1).copy(alpha = 0.45f))),
                "https://images.unsplash.com/photo-1518495973542-4542c06a5843?auto=format&fit=crop&w=600&q=80"
            )
        )
    }

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
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Welcome, $userName 👋",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Let's play some music 🎵",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Listen Together Header Action Button
                    IconButton(
                        onClick = { showListenTogetherSheet = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentRoom != null) IsaiLime.copy(alpha = 0.2f)
                                else DarkSurfaceGlass
                            )
                            .border(
                                1.dp,
                                if (currentRoom != null) IsaiLime.copy(alpha = 0.6f)
                                else GlassBorderSubtle,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Listen Together Room",
                            tint = if (currentRoom != null) IsaiLime else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Profile Icon Button
                    IconButton(
                        onClick = { viewModel.setScreen(AppScreen.PROFILE) },
                        modifier = Modifier
                            .size(40.dp)
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
                        tint = IsaiLime,
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
                        items(recentlyPlayed, key = { it.videoId }) { song ->
                            val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { viewModel.playSong(song, recentlyPlayed, forceLocal = true) },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DarkSurfaceVariant)
                                        .border(
                                            if (isThisPlaying) 2.dp else 1.dp,
                                            if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle,
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(RelevanceEngine.getSafeThumbnailUrl(song.thumbnailUrl, song.videoId))
                                            .crossfade(true)
                                            .error(com.saavn.music.R.drawable.app_logo)
                                            .build(),
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
                    items(picksSongs, key = { it.videoId }) { song ->
                        val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                        val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { viewModel.playSong(song, picksSongs, openFullPlayer = true, forceLocal = true) },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(
                                        if (isThisPlaying) 2.dp else 1.dp,
                                        if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle,
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(RelevanceEngine.getSafeThumbnailUrl(song.thumbnailUrl, song.videoId))
                                        .crossfade(true)
                                        .error(com.saavn.music.R.drawable.app_logo)
                                        .build(),
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
                    items(verifiedNewReleases, key = { it.videoId }) { song ->
                        val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { viewModel.playSong(song, verifiedNewReleases, openFullPlayer = true, forceLocal = true) },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(
                                        if (isThisPlaying) 2.dp else 1.dp,
                                        if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle,
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(RelevanceEngine.getSafeThumbnailUrl(song.thumbnailUrl, song.videoId))
                                        .crossfade(true)
                                        .error(com.saavn.music.R.drawable.app_logo)
                                        .build(),
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

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(moodCards, key = { it.title }) { card ->
                        Box(
                            modifier = Modifier
                                .width(136.dp)
                                .height(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { viewModel.selectCategory(card.query) }
                        ) {
                            AsyncImage(
                                model = card.imageUrl,
                                contentDescription = card.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.2f),
                                                Color.Black.copy(alpha = 0.82f)
                                            )
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(card.brush)
                            )
                            Text(
                                text = card.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
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
                    items(displayArtists, key = { it.name }) { artist ->
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
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(artist.image)
                                        .crossfade(true)
                                        .error(com.saavn.music.R.drawable.app_logo)
                                        .build(),
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

        // 8. Most Played Songs Section Header
        item {
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
        }

        // Virtualized Most Played Items (renders on-demand, no lag!)
        itemsIndexed(mostPlayedList, key = { _, song -> song.videoId }) { index, song ->
            val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
            val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime.copy(alpha = 0.12f) else DarkSurfaceVariant)
                    .border(1.dp, if (isThisPlaying) com.saavn.music.ui.theme.IsaiLime else GlassBorderSubtle, RoundedCornerShape(14.dp))
                    .clickable { viewModel.playSong(song, mostPlayedList, openFullPlayer = true, forceLocal = true) }
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
                    if (isThisPlaying) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Playing",
                            tint = DarkBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "#${index + 1}",
                            color = DarkBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(RelevanceEngine.getSafeThumbnailUrl(song.thumbnailUrl, song.videoId))
                        .crossfade(true)
                        .error(com.saavn.music.R.drawable.app_logo)
                        .build(),
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant),
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

    if (showListenTogetherSheet) {
        com.saavn.music.ui.components.ListenTogetherBottomSheet(
            listenManager = viewModel.listenTogetherManager,
            onDismissRequest = { showListenTogetherSheet = false }
        )
    }
}
