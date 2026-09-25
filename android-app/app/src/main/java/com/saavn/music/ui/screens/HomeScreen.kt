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

data class FeaturedPlaylistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val language: String,
    val coverUrl: String,
    val searchQuery: String,
    val isCategory: Boolean = true
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
    val recommendedReason by viewModel.recommendedReason.collectAsState()
    val personalizedRecs by viewModel.personalizedRecommendations.collectAsState()
    val similarSongs by viewModel.similarToRecentSongs.collectAsState()
    val similarTitle by viewModel.similarToRecentTitle.collectAsState()

    var showListenTogetherSheet by remember { mutableStateOf(false) }
    val currentRoom by viewModel.listenTogetherManager.currentRoom.collectAsState()

    val activeLang = RelevanceEngine.formatLanguageDisplayName(preferredLanguages.firstOrNull())
    val userName = userProfile?.displayName?.trim()?.takeIf { it.isNotBlank() && !it.equals("JEEVA ⚡", ignoreCase = true) }
        ?: "Listener"

    val activeLangs = remember(preferredLanguages) {
        preferredLanguages.map { RelevanceEngine.normalizeLanguage(it) }.ifEmpty { listOf("tamil") }
    }

    // Section 4: Picks For You (Dynamic YouTube Music Style - Live adaptation to user's listened songs)
    val picksSongs = remember(rawPicksSongs, personalizedRecs, trendingSongs, activeLangs) {
        val userRecs = (personalizedRecs + rawPicksSongs)
            .filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val validTrending = trendingSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        if (userRecs.isNotEmpty()) {
            (userRecs + validTrending).distinctBy { it.videoId }.take(35)
        } else {
            validTrending.take(35)
        }
    }

    // Section 5: New Releases (Authentic recent singles pool - Strictly in active language, min 30-35 songs)
    val verifiedNewReleases = remember(rawLatestReleases, trendingSongs, activeLangs) {
        val validNew = rawLatestReleases.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val validTrending = trendingSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val pool = (validNew + validTrending.drop(5)).distinctBy { it.videoId }
        pool.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }.take(35)
    }

    // Section 8: Most Played Songs pool cached
    val mostPlayedList = remember(rawMostPlayed, trendingSongs, activeLangs) {
        val validMost = rawMostPlayed.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val validTrending = trendingSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
        val pool = (validMost + validTrending).distinctBy { it.videoId }
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

    val allFeaturedPlaylists = remember {
        listOf(
            // ── TAMIL (12) ──
            FeaturedPlaylistItem("kadhal-vibes", "Kadhal Vibes ❤️", "50 Pure Romantic Classics", "Tamil", "https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg", "Kadhal Vibes", isCategory = true),
            FeaturedPlaylistItem("idhayam-pesuthey", "Idhayam Pesuthey 💔", "50 Soul-Stirring Melodies", "Tamil", "https://c.saavncdn.com/134/Mudhal-Kanave-Tamil-2021-20211207181143-500x500.jpg", "Idhayam Pesuthey", isCategory = true),
            FeaturedPlaylistItem("semma-kuthu", "Semma Kuthu ⚡", "50 High-Voltage Kuthu Hits", "Tamil", "https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg", "Semma Kuthu", isCategory = true),
            FeaturedPlaylistItem("mass-mode", "Mass Mode 💥", "50 Pure Mass & Adrenaline Hits", "Tamil", "https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg", "Mass Mode", isCategory = true),
            FeaturedPlaylistItem("gaana-pettai", "Gaana Pettai 🥁", "50 Chennai Gaana & Folk Beats", "Tamil", "https://c.saavncdn.com/274/Anegan-Tamil-2014-20190822152158-500x500.jpg", "Gaana Pettai", isCategory = true),
            FeaturedPlaylistItem("jannal-ora-payanam", "Jannal Ora Payanam 🌿", "50 Breezy Journey Songs", "Tamil", "https://c.saavncdn.com/137/96-Original-Motion-Picture-Soundtrack-Tamil-2018-20250905072505-500x500.jpg", "Jannal Ora Payanam", isCategory = true),
            FeaturedPlaylistItem("iravu-melodies", "Iravu Melodies 🌙", "50 Quiet Nights Melodies", "Tamil", "https://c.saavncdn.com/203/Indira-Tamil-1993-20251014143719-500x500.jpg", "Iravu Melodies", isCategory = true),
            FeaturedPlaylistItem("pudhu-udhayam", "Pudhu Udhayam 💪", "50 Motivation & Hope Songs", "Tamil", "https://c.saavncdn.com/002/Autograph-Tamil-2004-20180531-500x500.jpg", "Pudhu Udhayam", isCategory = true),
            FeaturedPlaylistItem("irai-isai", "Irai Isai 🙏", "50 Divine Tamil Prayers", "Tamil", "https://c.saavncdn.com/274/Bhakthi-Sangamam-Tamil-2003-20201009144033-500x500.jpg", "Irai Isai", isCategory = true),
            FeaturedPlaylistItem("school-days-memories", "School Days Memories 📻", "50 Nostalgia Hits", "Tamil", "https://c.saavncdn.com/752/Kadhal-Desam-Tamil-1996-20260923110001-500x500.jpg", "School Days Memories", isCategory = false),
            FeaturedPlaylistItem("trending-tamil", "Trending Hits 2026 🔥", "By ISAI Editorial • Jan - Sep 2026+", "Tamil", "https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg", "Tamil latest songs 2026 trending", isCategory = true),
            FeaturedPlaylistItem("kuthu-tamil", "Kuthu & Party Blast ⚡", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg", "Tamil kuthu dance party songs", isCategory = true),
            FeaturedPlaylistItem("romance-tamil", "Tamil Romance ❤️", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg", "Tamil romantic love melodies", isCategory = true),
            FeaturedPlaylistItem("chill-tamil", "Midnight Chill & Lo-Fi 🌙", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg", "Tamil acoustic lofi chill songs", isCategory = true),
            FeaturedPlaylistItem("mass-tamil", "Mass & Gym Motivation 💥", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg", "Tamil mass gym motivation songs", isCategory = true),
            FeaturedPlaylistItem("90s-tamil", "90s Golden Era 📻 (1990 - 1999)", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg", "Tamil 90s golden super hits", isCategory = false),
            FeaturedPlaylistItem("2k-tamil", "2K Evergreen Hits ✨ (2000 - 2009)", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg", "Tamil 2000s evergreen super hit songs", isCategory = false),
            FeaturedPlaylistItem("2010s-tamil", "2010s Blockbusters 🏆 (2010 - 2019)", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg", "Tamil 2010 to 2019 blockbuster hits", isCategory = false),
            FeaturedPlaylistItem("2020s-tamil", "2020 - 2025 Modern Hits 🌟", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/403/Kaathuvaakula-Rendu-Kaadhal-Original-Motion-Picture-Soundtrack-Tamil-2022-20220428131043-500x500.jpg", "Tamil 2020 to 2025 super hit songs", isCategory = false),
            FeaturedPlaylistItem("retro-tamil", "Retro Vintage Classics 🎙️ (80s & Earlier)", "By ISAI Editorial • 50+ Songs", "Tamil", "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg", "Tamil retro 80s 70s vintage classic songs", isCategory = false),

            // ── TELUGU (10) ──
            FeaturedPlaylistItem("trending-telugu", "Trending Hits 2026 🔥", "By ISAI Editorial • Jan - Sep 2026+", "Telugu", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg", "Telugu latest songs 2026 trending", isCategory = true),
            FeaturedPlaylistItem("kuthu-telugu", "Teenmaar & Mass Beats ⚡", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg", "Telugu teenmaar mass dance party songs", isCategory = true),
            FeaturedPlaylistItem("romance-telugu", "Telugu Romance ❤️", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg", "Telugu romantic love melodies", isCategory = true),
            FeaturedPlaylistItem("chill-telugu", "Telugu Chill & Acoustic 🌙", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg", "Telugu acoustic chill lofi songs", isCategory = true),
            FeaturedPlaylistItem("mass-telugu", "Tollywood Gym Motivation 💥", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg", "Telugu gym workout mass bgm beats", isCategory = true),
            FeaturedPlaylistItem("90s-telugu", "90s Telugu Golden Era 📻", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg", "Telugu 90s golden melody super hits", isCategory = false),
            FeaturedPlaylistItem("2k-telugu", "2K Tollywood Evergreen ✨", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg", "Telugu 2000s evergreen blockbuster songs", isCategory = false),
            FeaturedPlaylistItem("2010s-telugu", "2010s Tollywood Blockbusters 🏆", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg", "Telugu 2010 to 2019 blockbuster hits", isCategory = false),
            FeaturedPlaylistItem("2020s-telugu", "2020 - 2025 Modern Tollywood 🌟", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg", "Telugu 2020 to 2025 super hit songs", isCategory = false),
            FeaturedPlaylistItem("retro-telugu", "Vintage Tollywood Classics 🎙️", "By ISAI Editorial • 50+ Songs", "Telugu", "https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg", "Telugu vintage golden classic hits", isCategory = false),

            // ── HINDI (10) ──
            FeaturedPlaylistItem("trending-hindi", "Trending Hits 2026 🔥", "By ISAI Editorial • Jan - Sep 2026+", "Hindi", "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg", "Hindi latest songs 2026 trending", isCategory = true),
            FeaturedPlaylistItem("kuthu-hindi", "Bollywood Party Blast ⚡", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg", "Bollywood party dance club songs", isCategory = true),
            FeaturedPlaylistItem("romance-hindi", "Bollywood Romance ❤️", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg", "Hindi romantic love melodies Arijit", isCategory = true),
            FeaturedPlaylistItem("chill-hindi", "Midnight Chill & Acoustic 🌙", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg", "Hindi acoustic lofi chill songs", isCategory = true),
            FeaturedPlaylistItem("mass-hindi", "Desi Workout Motivation 💥", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg", "Hindi gym workout energetic bgm songs", isCategory = true),
            FeaturedPlaylistItem("90s-hindi", "90s Bollywood Classics 📻", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg", "90s bollywood golden melody super hits", isCategory = false),
            FeaturedPlaylistItem("2k-hindi", "2K Bollywood Nostalgia ✨", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg", "Bollywood 2000s evergreen hit songs", isCategory = false),
            FeaturedPlaylistItem("2010s-hindi", "2010s Bollywood Blockbusters 🏆", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg", "Bollywood 2010 to 2019 blockbuster hits", isCategory = false),
            FeaturedPlaylistItem("2020s-hindi", "2020 - 2025 Modern Bollywood 🌟", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg", "Hindi 2020 to 2025 super hit songs", isCategory = false),
            FeaturedPlaylistItem("retro-hindi", "Retro Golden Classics 🎙️", "By ISAI Editorial • 50+ Songs", "Hindi", "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg", "Hindi retro 70s 80s golden classic songs", isCategory = false),

            // ── MALAYALAM (10) ──
            FeaturedPlaylistItem("trending-malayalam", "Trending Hits 2026 🔥", "By ISAI Editorial • Jan - Sep 2026+", "Malayalam", "https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg", "Malayalam latest songs 2026 trending", isCategory = true),
            FeaturedPlaylistItem("kuthu-malayalam", "Party & Fast Beats ⚡", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg", "Malayalam party fast dance songs", isCategory = true),
            FeaturedPlaylistItem("romance-malayalam", "Malayalam Romance ❤️", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg", "Malayalam romantic melody songs", isCategory = true),
            FeaturedPlaylistItem("chill-malayalam", "Serene Chill & Acoustic 🌿", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg", "Malayalam acoustic chill lofi melodies", isCategory = true),
            FeaturedPlaylistItem("mass-malayalam", "Mollywood Mass & Gym 💥", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg", "Malayalam mass workout bgm beats", isCategory = true),
            FeaturedPlaylistItem("90s-malayalam", "90s Mollywood Golden Era 📻", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg", "Malayalam 90s golden melody hits", isCategory = false),
            FeaturedPlaylistItem("2k-malayalam", "2K Evergreen Mollywood ✨", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg", "Malayalam 2000s evergreen hits", isCategory = false),
            FeaturedPlaylistItem("2010s-malayalam", "2010s Mollywood Wave 🏆", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg", "Malayalam 2010 to 2019 blockbuster songs", isCategory = false),
            FeaturedPlaylistItem("2020s-malayalam", "2020 - 2025 Modern Mollywood 🌟", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg", "Malayalam 2020 to 2025 super hit songs", isCategory = false),
            FeaturedPlaylistItem("retro-malayalam", "Vintage Mollywood Classics 🎙️", "By ISAI Editorial • 50+ Songs", "Malayalam", "https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg", "Malayalam vintage old classic melodies", isCategory = false),

            // ── KANNADA (10) ──
            FeaturedPlaylistItem("trending-kannada", "Trending Hits 2026 🔥", "By ISAI Editorial • Jan - Sep 2026+", "Kannada", "https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg", "Kannada latest songs 2026 trending", isCategory = true),
            FeaturedPlaylistItem("kuthu-kannada", "Sandalwood Dance Blast ⚡", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg", "Kannada dance party fast beat songs", isCategory = true),
            FeaturedPlaylistItem("romance-kannada", "Kannada Romance ❤️", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg", "Kannada romantic love melodies", isCategory = true),
            FeaturedPlaylistItem("chill-kannada", "Kannada Chill Lo-Fi 🌙", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg", "Kannada acoustic chill lofi songs", isCategory = true),
            FeaturedPlaylistItem("mass-kannada", "Mass & Gym Motivation 💥", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg", "Kannada gym workout mass bgm beats", isCategory = true),
            FeaturedPlaylistItem("90s-kannada", "90s Sandalwood Golden Era 📻", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg", "Kannada 90s golden melody hits", isCategory = false),
            FeaturedPlaylistItem("2k-kannada", "2K Sandalwood Evergreen ✨", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg", "Kannada 2000s evergreen super hits", isCategory = false),
            FeaturedPlaylistItem("2010s-kannada", "2010s Sandalwood Wave 🏆", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg", "Kannada 2010 to 2019 blockbuster hits", isCategory = false),
            FeaturedPlaylistItem("2020s-kannada", "2020 - 2025 Modern Sandalwood 🌟", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg", "Kannada 2020 to 2025 super hit songs", isCategory = false),
            FeaturedPlaylistItem("retro-kannada", "Vintage Sandalwood Classics 🎙️", "By ISAI Editorial • 50+ Songs", "Kannada", "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg", "Kannada vintage golden classic songs", isCategory = false),

            // ── ENGLISH (10) ──
            FeaturedPlaylistItem("trending-english", "Today's Top Hits (July 2026+) 🔥", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg", "Global english latest hits 2026 July August September", isCategory = true),
            FeaturedPlaylistItem("kuthu-english", "Dance & Club Anthems ⚡", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Taylor_Swift_003_20200226074119_500x500.jpg", "EDM pop party dance hits", isCategory = true),
            FeaturedPlaylistItem("romance-english", "Soulful Love & Pop ❤️", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg", "English romantic pop love songs", isCategory = true),
            FeaturedPlaylistItem("chill-english", "Midnight Chill & Acoustic 🌙", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg", "English acoustic chill lo-fi relax songs", isCategory = true),
            FeaturedPlaylistItem("mass-english", "Beast Mode Workout 💥", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg", "Gym workout motivation high energy songs", isCategory = true),
            FeaturedPlaylistItem("90s-english", "90s Pop & Rock Anthems 📻", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Taylor_Swift_003_20200226074119_500x500.jpg", "90s english pop rock classic hits", isCategory = false),
            FeaturedPlaylistItem("2k-english", "2K Pop Nostalgia ✨", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Taylor_Swift_003_20200226074119_500x500.jpg", "2000s english pop billboard hits", isCategory = false),
            FeaturedPlaylistItem("2010s-english", "2010s Global Blockbusters 🏆", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg", "2010 to 2019 billboard top hits", isCategory = false),
            FeaturedPlaylistItem("2020s-english", "2020 - 2025 Modern Pop 🌟", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Taylor_Swift_003_20200226074119_500x500.jpg", "2020 to 2025 global billboard hits", isCategory = false),
            FeaturedPlaylistItem("retro-english", "70s & 80s Vintage Classics 🎙️", "By ISAI Editorial • 50+ Songs", "English", "https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg", "70s 80s classic rock pop hits", isCategory = false)
        )
    }

    val filteredPlaylists = remember(activeLang, allFeaturedPlaylists) {
        val matching = allFeaturedPlaylists.filter { playlist ->
            playlist.language.equals(activeLang, ignoreCase = true)
        }
        if (matching.isNotEmpty()) matching else allFeaturedPlaylists
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
                                    .clickable { viewModel.playSong(song, openFullPlayer = true, forceLocal = true) },
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

        // 3.1. Similar to [Recently Played] 🎶 (YouTube Music Dynamic Radio)
        if (similarSongs.isNotEmpty() && similarTitle.isNotBlank()) {
            item {
                Column {
                    Text(
                        text = "$similarTitle 🎶",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Dynamic suggestions based on what you played",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(similarSongs, key = { it.videoId }) { song ->
                            val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                            val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { viewModel.playSong(song, openFullPlayer = true, forceLocal = true) },
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
                                                .background(Color.Black.copy(alpha = 0.45f)),
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
                                    color = TextPrimary,
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
                        }
                    }
                }
            }
        }

        // 3.5. Featured Playlists 🎧
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Featured Playlists 🎧",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Curated playlists in top languages",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Playlists Carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredPlaylists, key = { it.id }) { playlist ->
                        Column(
                            modifier = Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(DarkSurfaceGlass)
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
                                .clickable {
                                    viewModel.openFeaturedPlaylist(
                                        id = playlist.id,
                                        title = playlist.title,
                                        subtitle = playlist.subtitle,
                                        language = playlist.language,
                                        coverUrl = playlist.coverUrl,
                                        searchQuery = playlist.searchQuery
                                    )
                                }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurfaceVariant)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(playlist.coverUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = playlist.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Language Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = playlist.language.uppercase(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Round green Play button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DB954))
                                        .clickable {
                                            viewModel.openFeaturedPlaylist(
                                                id = playlist.id,
                                                title = playlist.title,
                                                subtitle = playlist.subtitle,
                                                language = playlist.language,
                                                coverUrl = playlist.coverUrl,
                                                searchQuery = playlist.searchQuery
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                text = playlist.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = playlist.subtitle,
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )
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
                    text = if (recentlyPlayed.isNotEmpty()) recommendedReason else "Popular in $activeLang",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (picksSongs.isEmpty() && isLoadingHome) {
                        items(5) {
                            Column(
                                modifier = Modifier.width(140.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurfaceVariant)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkSurfaceVariant)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkSurfaceVariant)
                                )
                            }
                        }
                    } else {
                        items(picksSongs, key = { it.videoId }) { song ->
                        val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                        val isFav = favorites.any { it.videoId.trim() == song.videoId.trim() }
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { viewModel.playSong(song, openFullPlayer = true, forceLocal = true) },
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

                    TextButton(onClick = {
                        viewModel.openFeaturedPlaylist(
                            id = "new-releases-$activeLang",
                            title = "$activeLang New Releases 🌟",
                            subtitle = "Fresh Tracks & Latest Releases",
                            language = activeLang,
                            coverUrl = verifiedNewReleases.firstOrNull()?.thumbnailUrl ?: "",
                            searchQuery = "$activeLang new songs 2026"
                        )
                    }) {
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
                    if (verifiedNewReleases.isEmpty() && isLoadingHome) {
                        items(5) {
                            Column(
                                modifier = Modifier.width(140.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurfaceVariant)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkSurfaceVariant)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkSurfaceVariant)
                                )
                            }
                        }
                    } else {
                        items(verifiedNewReleases, key = { it.videoId }) { song ->
                        val isThisPlaying = currentPlayingSong?.videoId == song.videoId && isPlaying
                        Column(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { viewModel.playSong(song, openFullPlayer = true, forceLocal = true) },
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
                                .clickable {
                                    viewModel.openFeaturedPlaylist(
                                        id = "mood-${card.title.lowercase().replace(" ", "-")}",
                                        title = "${card.title} Hits",
                                        subtitle = "ISAI Curated Vibes • 50+ Songs",
                                        language = activeLang,
                                        coverUrl = card.imageUrl,
                                        searchQuery = card.query
                                    )
                                }
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
                                .clickable {
                                    viewModel.openFeaturedPlaylist(
                                        id = "artist-${artist.name.lowercase().replace(" ", "-")}",
                                        title = artist.name,
                                        subtitle = "${artist.role} • Top Hits",
                                        language = activeLang,
                                        coverUrl = artist.image,
                                        searchQuery = artist.query
                                    )
                                }
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

        if (mostPlayedList.isEmpty() && isLoadingHome) {
            items(5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkBackground.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkBackground.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        } else {
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
                    .clickable { viewModel.playSong(song, openFullPlayer = true, forceLocal = true) }
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
    }

    if (showListenTogetherSheet) {
        com.saavn.music.ui.components.ListenTogetherBottomSheet(
            listenManager = viewModel.listenTogetherManager,
            onDismissRequest = { showListenTogetherSheet = false }
        )
    }
}
