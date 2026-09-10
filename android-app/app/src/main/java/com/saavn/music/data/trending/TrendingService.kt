package com.saavn.music.data.trending

import android.content.Context
import com.saavn.music.data.analytics.AnalyticsService
import com.saavn.music.data.model.YouTubeSong

data class TopArtistData(
    val name: String,
    val playCount: Int,
    val role: String = "Popular Artist",
    val imageUrl: String = ""
)

class TrendingService private constructor(private val context: Context) {
    private val analytics = AnalyticsService.getInstance(context)

    companion object {
        private const val DAY_MS = 24 * 3600 * 1000L

        @Volatile
        private var instance: TrendingService? = null

        fun getInstance(context: Context): TrendingService {
            return instance ?: synchronized(this) {
                instance ?: TrendingService(context.applicationContext).also { instance = it }
            }
        }

        val CURATED_TAMIL_ARTISTS = listOf(
            TopArtistData(
                name = "Anirudh Ravichander",
                role = "Rockstar",
                imageUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg",
                playCount = 98500
            ),
            TopArtistData(
                name = "A. R. Rahman",
                role = "Isai Puyal",
                imageUrl = "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg",
                playCount = 94200
            ),
            TopArtistData(
                name = "Yuvan Shankar Raja",
                role = "Youth Icon",
                imageUrl = "https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_002_20180802174245_500x500.jpg",
                playCount = 89100
            ),
            TopArtistData(
                name = "Harris Jayaraj",
                role = "Melody King",
                imageUrl = "https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg",
                playCount = 81400
            ),
            TopArtistData(
                name = "Sid Sriram",
                role = "Soulful Singer",
                imageUrl = "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg",
                playCount = 76300
            ),
            TopArtistData(
                name = "G. V. Prakash Kumar",
                role = "Composer & Singer",
                imageUrl = "https://c.saavncdn.com/artists/G_V__Prakash_Kumar_003_20251113063655_500x500.jpg",
                playCount = 72000
            ),
            TopArtistData(
                name = "Santhosh Narayanan",
                role = "SaNa",
                imageUrl = "https://c.saavncdn.com/artists/Santhosh_Narayanan_002_20250527101718_500x500.jpg",
                playCount = 68900
            ),
            TopArtistData(
                name = "Pradeep Kumar",
                role = "Vocal Maestro",
                imageUrl = "https://c.saavncdn.com/artists/Pradeep_Kumar_002_20250807084559_500x500.jpg",
                playCount = 64500
            ),
            TopArtistData(
                name = "Shreya Ghoshal",
                role = "Melody Queen",
                imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg",
                playCount = 61200
            ),
            TopArtistData(
                name = "Vijay Antony",
                role = "Composer & Singer",
                imageUrl = "https://c.saavncdn.com/artists/Vijay_Antony_500x500.jpg",
                playCount = 58400
            ),
            TopArtistData(
                name = "Ilaiyaraaja",
                role = "Isaignani",
                imageUrl = "https://c.saavncdn.com/artists/Ilaiyaraaja_001_20251020081419_500x500.jpg",
                playCount = 55100
            ),
            TopArtistData(
                name = "S. P. Balasubrahmanyam",
                role = "Legend SPB",
                imageUrl = "https://c.saavncdn.com/artists/S_P_Balasubrahmanyam_500x500.jpg",
                playCount = 53000
            ),
            TopArtistData(
                name = "Dhee",
                role = "Indie Icon",
                imageUrl = "https://c.saavncdn.com/artists/Dhee_20180510121326_500x500.jpg",
                playCount = 51000
            ),
            TopArtistData(
                name = "Jonita Gandhi",
                role = "Playback Singer",
                imageUrl = "https://c.saavncdn.com/artists/Jonita_Gandhi_003_20180507091741_500x500.jpg",
                playCount = 49000
            ),
            TopArtistData(
                name = "Sean Roldan",
                role = "Composer & Singer",
                imageUrl = "https://c.saavncdn.com/artists/Sean_Roldan_002_20240319071510_500x500.jpg",
                playCount = 47500
            ),
            TopArtistData(
                name = "K. S. Chithra",
                role = "Chinnakuyil",
                imageUrl = "https://c.saavncdn.com/artists/K_S_Chithra_002_20190906071921_500x500.jpg",
                playCount = 46000
            ),
            TopArtistData(
                name = "Arijit Singh",
                role = "Soul of Melody",
                imageUrl = "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg",
                playCount = 45000
            )
        )
        val CURATED_ARTISTS_BY_LANG: Map<String, List<TopArtistData>> = mapOf(
            "tamil" to listOf(
                TopArtistData(name = "Anirudh Ravichander", role = "Rockstar", imageUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg", playCount = 98500),
                TopArtistData(name = "A. R. Rahman", role = "Isai Puyal", imageUrl = "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg", playCount = 94200),
                TopArtistData(name = "Yuvan Shankar Raja", role = "Youth Icon", imageUrl = "https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_002_20180802174245_500x500.jpg", playCount = 89100),
                TopArtistData(name = "Harris Jayaraj", role = "Melody King", imageUrl = "https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg", playCount = 81400),
                TopArtistData(name = "Sid Sriram", role = "Soulful Singer", imageUrl = "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg", playCount = 76300),
                TopArtistData(name = "G. V. Prakash Kumar", role = "Composer & Singer", imageUrl = "https://c.saavncdn.com/artists/G_V__Prakash_Kumar_003_20251113063655_500x500.jpg", playCount = 72000),
                TopArtistData(name = "Santhosh Narayanan", role = "SaNa", imageUrl = "https://c.saavncdn.com/artists/Santhosh_Narayanan_002_20250527101718_500x500.jpg", playCount = 68900),
                TopArtistData(name = "Shreya Ghoshal", role = "Melody Queen", imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg", playCount = 61200),
                TopArtistData(name = "Ilaiyaraaja", role = "Isaignani", imageUrl = "https://c.saavncdn.com/artists/Ilaiyaraaja_001_20251020081419_500x500.jpg", playCount = 55100),
                TopArtistData(name = "S. P. Balasubrahmanyam", role = "Legend SPB", imageUrl = "https://c.saavncdn.com/artists/S_P_Balasubrahmanyam_500x500.jpg", playCount = 53000),
                TopArtistData(name = "Dhee", role = "Indie Icon", imageUrl = "https://c.saavncdn.com/artists/Dhee_20180510121326_500x500.jpg", playCount = 51000),
                TopArtistData(name = "Jonita Gandhi", role = "Playback Singer", imageUrl = "https://c.saavncdn.com/artists/Jonita_Gandhi_003_20180507091741_500x500.jpg", playCount = 49000)
            ),
            "telugu" to listOf(
                TopArtistData(name = "Devi Sri Prasad", role = "Rockstar DSP", imageUrl = "https://c.saavncdn.com/artists/Devi_Sri_Prasad_500x500.jpg", playCount = 95000),
                TopArtistData(name = "Thaman S", role = "Musical Storm", imageUrl = "https://c.saavncdn.com/artists/Thaman_S_500x500.jpg", playCount = 92000),
                TopArtistData(name = "Sid Sriram", role = "Melody Sensation", imageUrl = "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg", playCount = 88000),
                TopArtistData(name = "Anurag Kulkarni", role = "Playback Vocalist", imageUrl = "https://c.saavncdn.com/artists/Anurag_Kulkarni_500x500.jpg", playCount = 81000),
                TopArtistData(name = "Ram Miriyala", role = "Folk & Indie", imageUrl = "https://c.saavncdn.com/artists/Ram_Miriyala_500x500.jpg", playCount = 77000),
                TopArtistData(name = "M. M. Keeravani", role = "Oscar Maestro", imageUrl = "https://c.saavncdn.com/artists/M_M_Keeravani_500x500.jpg", playCount = 75000),
                TopArtistData(name = "Armaan Malik", role = "Prince of Romance", imageUrl = "https://c.saavncdn.com/artists/Armaan_Malik_500x500.jpg", playCount = 72000),
                TopArtistData(name = "Shreya Ghoshal", role = "Melody Queen", imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg", playCount = 69000)
            ),
            "hindi" to listOf(
                TopArtistData(name = "Arijit Singh", role = "Soul of Melody", imageUrl = "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg", playCount = 99000),
                TopArtistData(name = "Pritam", role = "Hit Machine", imageUrl = "https://c.saavncdn.com/artists/Pritam_Chakraborty_500x500.jpg", playCount = 93000),
                TopArtistData(name = "Shreya Ghoshal", role = "Melody Queen", imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg", playCount = 88000),
                TopArtistData(name = "Sachin-Jigar", role = "Music Duo", imageUrl = "https://c.saavncdn.com/artists/Sachin-Jigar_500x500.jpg", playCount = 82000),
                TopArtistData(name = "Vishal-Shekhar", role = "Party Beats", imageUrl = "https://c.saavncdn.com/artists/Vishal-Shekhar_500x500.jpg", playCount = 79000),
                TopArtistData(name = "Badshah", role = "Rap Star", imageUrl = "https://c.saavncdn.com/artists/Badshah_500x500.jpg", playCount = 76000),
                TopArtistData(name = "Atif Aslam", role = "Romantic Icon", imageUrl = "https://c.saavncdn.com/artists/Atif_Aslam_500x500.jpg", playCount = 74000),
                TopArtistData(name = "Neha Kakkar", role = "Groove Queen", imageUrl = "https://c.saavncdn.com/artists/Neha_Kakkar_500x500.jpg", playCount = 71000)
            ),
            "malayalam" to listOf(
                TopArtistData(name = "Sushin Shyam", role = "Trendsetter", imageUrl = "https://c.saavncdn.com/artists/Sushin_Shyam_500x500.jpg", playCount = 92000),
                TopArtistData(name = "Hesham Abdul Wahab", role = "Melody Maker", imageUrl = "https://c.saavncdn.com/artists/Hesham_Abdul_Wahab_500x500.jpg", playCount = 87000),
                TopArtistData(name = "K. S. Harisankar", role = "Golden Voice", imageUrl = "https://c.saavncdn.com/artists/K_S_Harisankar_500x500.jpg", playCount = 81000),
                TopArtistData(name = "Shaan Rahman", role = "Hit Composer", imageUrl = "https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg", playCount = 76000),
                TopArtistData(name = "Jakes Bejoy", role = "BGM King", imageUrl = "https://c.saavncdn.com/artists/Jakes_Bejoy_500x500.jpg", playCount = 72000),
                TopArtistData(name = "Vineeth Sreenivasan", role = "All-Rounder", imageUrl = "https://c.saavncdn.com/artists/Vineeth_Sreenivasan_500x500.jpg", playCount = 69000)
            ),
            "english" to listOf(
                TopArtistData(name = "Ed Sheeran", role = "Acoustic Pop", imageUrl = "https://c.saavncdn.com/artists/Ed_Sheeran_500x500.jpg", playCount = 91000),
                TopArtistData(name = "Taylor Swift", role = "Global Icon", imageUrl = "https://c.saavncdn.com/artists/Taylor_Swift_500x500.jpg", playCount = 90000),
                TopArtistData(name = "The Weeknd", role = "R&B Pop", imageUrl = "https://c.saavncdn.com/artists/The_Weeknd_500x500.jpg", playCount = 88000),
                TopArtistData(name = "Dua Lipa", role = "Disco Pop", imageUrl = "https://c.saavncdn.com/artists/Dua_Lipa_500x500.jpg", playCount = 82000),
                TopArtistData(name = "Bruno Mars", role = "Funk & Soul", imageUrl = "https://c.saavncdn.com/artists/Bruno_Mars_500x500.jpg", playCount = 79000)
            )
        )
    }

    /**
     * 1. 🏆 "Most Played Songs": Pure play count sum across all users (No release date penalty)
     */
    fun getMostPlayedSongs(songs: List<YouTubeSong>): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        return songs.sortedWith(
            compareByDescending<YouTubeSong> { song ->
                analytics.getEventCount(song.videoId, "play", 30 * DAY_MS)
            }.thenByDescending { it.playCount }
        )
    }

    /**
     * 2. 🎤 "Popular Singers / Artists": Returns rich curated artists for user's preferred languages merged with play counts
     */
    fun getPopularArtists(
        songs: List<YouTubeSong>,
        preferredLanguages: List<String> = emptyList(),
        limit: Int = 12
    ): List<TopArtistData> {
        val baseList = mutableListOf<TopArtistData>()
        val activeLangs = preferredLanguages.map { it.lowercase().trim() }.ifEmpty { listOf("tamil") }

        for (lang in activeLangs) {
            val artists = CURATED_ARTISTS_BY_LANG[lang]
            if (artists != null) {
                baseList.addAll(artists)
            }
        }
        if (baseList.isEmpty()) {
            baseList.addAll(CURATED_TAMIL_ARTISTS)
        }

        val result = baseList.distinctBy { it.name.lowercase() }.map { curated ->
            val matchingSongs = songs.filter { s ->
                val title = s.title.lowercase()
                val artist = s.channelTitle.lowercase()
                val searchName = curated.name.lowercase()
                title.contains(searchName) || artist.contains(searchName)
            }
            val playSum = matchingSongs.sumOf { s -> analytics.getEventCount(s.videoId, "play", 30 * DAY_MS).toInt() }
            if (playSum > 0) {
                curated.copy(playCount = curated.playCount + playSum)
            } else {
                curated
            }
        }.sortedByDescending { it.playCount }

        return result.take(limit)
    }

    fun rankTrendingSongs(songs: List<YouTubeSong>, timeWindow: Any? = null): List<YouTubeSong> {
        return getMostPlayedSongs(songs)
    }

    fun getTrendingRecentReleases(songs: List<YouTubeSong>, maxReleaseDays: Int = 30): List<YouTubeSong> {
        return getMostPlayedSongs(songs)
    }

    fun getTopChartbusters(songs: List<YouTubeSong>): List<YouTubeSong> {
        return getMostPlayedSongs(songs)
    }
}
