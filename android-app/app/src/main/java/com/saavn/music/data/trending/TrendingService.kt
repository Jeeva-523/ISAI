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
                imageUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_002_20230222074343_500x500.jpg",
                playCount = 98500
            ),
            TopArtistData(
                name = "A. R. Rahman",
                role = "Isai Puyal",
                imageUrl = "https://c.saavncdn.com/artists/A_R_Rahman_002_20210201083627_500x500.jpg",
                playCount = 94200
            ),
            TopArtistData(
                name = "Yuvan Shankar Raja",
                role = "Youth Icon",
                imageUrl = "https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_002_20220613093202_500x500.jpg",
                playCount = 89100
            ),
            TopArtistData(
                name = "Harris Jayaraj",
                role = "Melody King",
                imageUrl = "https://c.saavncdn.com/artists/Harris_Jayaraj_500x500.jpg",
                playCount = 81400
            ),
            TopArtistData(
                name = "Sid Sriram",
                role = "Soulful Singer",
                imageUrl = "https://c.saavncdn.com/artists/Sid_Sriram_003_20230222074403_500x500.jpg",
                playCount = 76300
            ),
            TopArtistData(
                name = "G. V. Prakash Kumar",
                role = "Composer & Singer",
                imageUrl = "https://c.saavncdn.com/artists/G_V_Prakash_Kumar_500x500.jpg",
                playCount = 72000
            ),
            TopArtistData(
                name = "Santhosh Narayanan",
                role = "SaNa",
                imageUrl = "https://c.saavncdn.com/artists/Santhosh_Narayanan_500x500.jpg",
                playCount = 68900
            ),
            TopArtistData(
                name = "Pradeep Kumar",
                role = "Vocal Maestro",
                imageUrl = "https://c.saavncdn.com/artists/Pradeep_Kumar_500x500.jpg",
                playCount = 64500
            ),
            TopArtistData(
                name = "Shreya Ghoshal",
                role = "Melody Queen",
                imageUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_004_20230222074347_500x500.jpg",
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
                imageUrl = "https://c.saavncdn.com/artists/Ilaiyaraaja_002_20220613093155_500x500.jpg",
                playCount = 55100
            ),
            TopArtistData(
                name = "S. P. Balasubrahmanyam",
                role = "Legend SPB",
                imageUrl = "https://c.saavncdn.com/artists/S_P_Balasubrahmanyam_500x500.jpg",
                playCount = 53000
            )
        )
    }

    /**
     * 1. 🏆 "Most Played Songs": Pure play count sum across all users (No release date penalty)
     */
    fun getMostPlayedSongs(songs: List<YouTubeSong>): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        return songs.sortedByDescending { song ->
            analytics.getEventCount(song.videoId, "play", 30 * DAY_MS)
        }
    }

    /**
     * 2. 🎤 "Popular Singers / Artists": Returns rich curated Tamil artists merged with play counts
     */
    fun getPopularArtists(songs: List<YouTubeSong>, limit: Int = 12): List<TopArtistData> {
        val result = CURATED_TAMIL_ARTISTS.map { curated ->
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
