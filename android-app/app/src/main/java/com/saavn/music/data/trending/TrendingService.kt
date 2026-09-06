package com.saavn.music.data.trending

import android.content.Context
import com.saavn.music.data.analytics.AnalyticsService
import com.saavn.music.data.model.YouTubeSong
import java.util.concurrent.ConcurrentHashMap

class TrendingService private constructor(private val context: Context) {
    private val analytics = AnalyticsService.getInstance(context)
    private val releaseDateCache = ConcurrentHashMap<String, Long>()

    companion object {
        private const val DAY_MS = 24 * 3600 * 1000L

        @Volatile
        private var instance: TrendingService? = null

        fun getInstance(context: Context): TrendingService {
            return instance ?: synchronized(this) {
                instance ?: TrendingService(context.applicationContext).also { instance = it }
            }
        }
    }

    fun registerSongReleaseDate(videoId: String, releaseDateMs: Long) {
        if (videoId.isNotBlank() && releaseDateMs > 0) {
            releaseDateCache[videoId] = releaseDateMs
        }
    }

    /**
     * Calculates Recency-Weighted Trending Score:
     * score = (plays_last_48h * 3.0 + plays_last_7d * 1.5 + plays_last_30d * 1.0) / (daysSinceRelease + 1)
     */
    fun calculateTrendingScore(song: YouTubeSong): Double {
        val now = System.currentTimeMillis()

        val plays48h = analytics.getEventCount(song.videoId, "play", 2 * DAY_MS)
        val plays7d = analytics.getEventCount(song.videoId, "play", 7 * DAY_MS)
        val plays30d = analytics.getEventCount(song.videoId, "play", 30 * DAY_MS)

        val likes48h = analytics.getEventCount(song.videoId, "like", 2 * DAY_MS)
        val adds48h = analytics.getEventCount(song.videoId, "playlist_add", 2 * DAY_MS)

        val weightedPlays = (plays48h * 3.0) + (likes48h * 2.5) + (adds48h * 3.0) + (plays7d * 1.5) + (plays30d * 1.0)

        val releaseMs = releaseDateCache[song.videoId] ?: (now - 7 * DAY_MS)
        val daysSinceRelease = Math.max(0.0, (now - releaseMs).toDouble() / DAY_MS)

        val recencyScore = weightedPlays / (daysSinceRelease + 1.0)
        val tieBreaker = Math.abs(song.videoId.hashCode() % 100).toDouble() / 1000.0

        return recencyScore + tieBreaker
    }

    /**
     * 1. 🔥 "Trending Now": Strictly songs released within last 30 days
     */
    fun getTrendingRecentReleases(songs: List<YouTubeSong>, maxReleaseDays: Int = 30): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()

        val recentEligibleSongs = songs.filter { song ->
            val releaseMs = releaseDateCache[song.videoId] ?: return@filter true
            val daysOld = (now - releaseMs) / DAY_MS
            daysOld <= maxReleaseDays
        }

        return recentEligibleSongs.sortedByDescending { calculateTrendingScore(it) }
    }

    /**
     * Alias for backward compatibility
     */
    fun rankTrendingSongs(songs: List<YouTubeSong>, timeWindow: Any? = null): List<YouTubeSong> {
        return getTrendingRecentReleases(songs, 30)
    }

    /**
     * 2. 🏆 "Top Charts / Popular": All-time popular songs without release date restriction
     */
    fun getTopChartbusters(songs: List<YouTubeSong>): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        return songs.sortedByDescending { song ->
            analytics.getEventCount(song.videoId, "play", 30 * DAY_MS)
        }
    }
}
