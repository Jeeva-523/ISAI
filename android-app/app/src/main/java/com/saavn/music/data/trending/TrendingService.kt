package com.saavn.music.data.trending

import android.content.Context
import com.saavn.music.data.analytics.AnalyticsService
import com.saavn.music.data.model.YouTubeSong

data class TopArtistData(
    val name: String,
    val playCount: Int
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
     * 2. 🎤 "Popular Singers / Artists": Aggregates play counts grouped by artist name
     */
    fun getPopularArtists(songs: List<YouTubeSong>, limit: Int = 10): List<TopArtistData> {
        if (songs.isEmpty()) return emptyList()
        val artistPlayMap = mutableMapOf<String, Int>()

        songs.forEach { song ->
            val artist = song.channelTitle
                .replace(" - Topic", "")
                .replace(" Official", "")
                .trim()
            if (artist.isNotBlank() && artist != "Tamil Artist") {
                val plays = analytics.getEventCount(song.videoId, "play", 30 * DAY_MS).toInt()
                val score = if (plays > 0) plays else 1
                artistPlayMap[artist] = (artistPlayMap[artist] ?: 0) + score
            }
        }

        return artistPlayMap.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { TopArtistData(name = it.key, playCount = it.value) }
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
