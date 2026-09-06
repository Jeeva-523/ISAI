package com.saavn.music.data.trending

import android.content.Context
import com.saavn.music.data.analytics.AnalyticsService
import com.saavn.music.data.model.YouTubeSong

enum class TrendingTimeWindow(val label: String, val ms: Long) {
    TODAY("Today", 24 * 3600 * 1000L),
    THIS_WEEK("This Week", 7 * 24 * 3600 * 1000L),
    THIS_MONTH("This Month", 30 * 24 * 3600 * 1000L)
}

class TrendingService private constructor(private val context: Context) {
    private val analytics = AnalyticsService.getInstance(context)

    companion object {
        @Volatile
        private var instance: TrendingService? = null

        fun getInstance(context: Context): TrendingService {
            return instance ?: synchronized(this) {
                instance ?: TrendingService(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Ranks songs dynamically using time decay score:
     * Score = (Plays * 1.0) + (Unique Listeners * 2.0) + (Likes * 3.0) + (Playlist Adds * 4.0) + (Search * 2.5)
     */
    fun calculateTrendingScore(song: YouTubeSong, timeWindow: TrendingTimeWindow): Double {
        val windowMs = timeWindow.ms
        val plays = analytics.getEventCount(song.videoId, "play", windowMs)
        val listeners = analytics.getUniqueListenerCount(song.videoId, windowMs)
        val likes = analytics.getEventCount(song.videoId, "like", windowMs)
        val adds = analytics.getEventCount(song.videoId, "playlist_add", windowMs)
        val searches = analytics.getEventCount(song.videoId, "search", windowMs)

        val baseScore = (plays * 1.0) + (listeners * 2.0) + (likes * 3.0) + (adds * 4.0) + (searches * 2.5)
        
        // Base seed tiebreaker from video ID hash to ensure stable dynamic ordering for new songs
        val seed = (song.videoId.hashCode() % 100).toDouble() / 100.0
        return baseScore + seed
    }

    fun rankTrendingSongs(songs: List<YouTubeSong>, timeWindow: TrendingTimeWindow = TrendingTimeWindow.TODAY): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        return songs.sortedByDescending { calculateTrendingScore(it, timeWindow) }
    }
}
