package com.saavn.music.data.trending

import android.content.Context
import android.util.Log
import com.saavn.music.data.analytics.AnalyticsService
import com.saavn.music.data.model.YouTubeSong
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class SongScoreMeta(
    val score: Double,
    val daysOld: Double,
    val boost: Double,
    val weightedPlays: Double
)

class TrendingService private constructor(private val context: Context) {
    private val analytics = AnalyticsService.getInstance(context)
    private val releaseDateCache = ConcurrentHashMap<String, Long>()

    companion object {
        private const val DAY_MS = 24 * 3600 * 1000L
        private const val TAG = "ISAI_TRENDING_DEBUG"

        @Volatile
        private var instance: TrendingService? = null

        fun getInstance(context: Context): TrendingService {
            return instance ?: synchronized(this) {
                instance ?: TrendingService(context.applicationContext).also { instance = it }
            }
        }

        fun parseReleaseDateToMs(input: Any?): Long {
            val now = System.currentTimeMillis()
            if (input == null) return now - (3 * DAY_MS) // Default fallback: 3 days ago

            if (input is Long) {
                if (input < 10000000000L) return input * 1000L
                return input
            }
            if (input is Int) {
                val longInput = input.toLong()
                if (longInput < 10000000000L) return longInput * 1000L
                return longInput
            }

            if (input is String) {
                if (input.isBlank()) return now - (3 * DAY_MS)
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val parsed = sdf.parse(input)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {}

                try {
                    val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val parsedIso = sdfIso.parse(input)
                    if (parsedIso != null) return parsedIso.time
                } catch (_: Exception) {}

                // Match 4 digit year "2026"
                val regex = Regex("\\b(20\\d{2})\\b")
                val match = regex.find(input)
                if (match != null) {
                    val year = match.groupValues[1].toIntOrNull()
                    if (year != null) {
                        try {
                            val sdfYear = SimpleDateFormat("yyyy", Locale.US)
                            val parsedYear = sdfYear.parse(year.toString())
                            if (parsedYear != null) return parsedYear.time
                        } catch (_: Exception) {}
                    }
                }
            }

            if (input is Date) return input.time

            return now - (3 * DAY_MS)
        }
    }

    fun registerSongReleaseDate(videoId: String, rawReleaseDate: Any?) {
        if (videoId.isNotBlank()) {
            val parsedMs = parseReleaseDateToMs(rawReleaseDate)
            releaseDateCache[videoId] = parsedMs
        }
    }

    /**
     * Calculates Recency-Weighted Trending Score with New Release Booster
     */
    fun calculateTrendingScoreMeta(song: YouTubeSong): SongScoreMeta {
        val now = System.currentTimeMillis()

        val plays48h = analytics.getEventCount(song.videoId, "play", 2 * DAY_MS)
        val plays7d = analytics.getEventCount(song.videoId, "play", 7 * DAY_MS)
        val plays30d = analytics.getEventCount(song.videoId, "play", 30 * DAY_MS)

        val likes48h = analytics.getEventCount(song.videoId, "like", 2 * DAY_MS)
        val adds48h = analytics.getEventCount(song.videoId, "playlist_add", 2 * DAY_MS)

        val weightedPlays = (plays48h * 3.0) + (likes48h * 2.5) + (adds48h * 3.0) + (plays7d * 1.5) + (plays30d * 1.0)

        val releaseMs = releaseDateCache[song.videoId] ?: (now - (3 * DAY_MS))
        val daysSinceRelease = Math.max(0.0, (now - releaseMs).toDouble() / DAY_MS)

        // New Release Booster Factor for fresh tracks (0-7 days old)
        var newReleaseBoost = 0.0
        if (daysSinceRelease <= 7.0) {
            newReleaseBoost = (7.0 - daysSinceRelease) * 3.0 // Up to +21 points for 0-day old songs
        }

        val recencyScore = weightedPlays / (daysSinceRelease + 1.0)
        val tieBreaker = Math.abs(song.videoId.hashCode() % 100).toDouble() / 1000.0

        val finalScore = recencyScore + newReleaseBoost + tieBreaker

        return SongScoreMeta(
            score = finalScore,
            daysOld = daysSinceRelease,
            boost = newReleaseBoost,
            weightedPlays = weightedPlays
        )
    }

    fun calculateTrendingScore(song: YouTubeSong): Double {
        return calculateTrendingScoreMeta(song).score
    }

    /**
     * 1. 🔥 "Trending Now": Strictly songs released within last 30 days
     */
    fun getTrendingRecentReleases(songs: List<YouTubeSong>, maxReleaseDays: Int = 30): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()

        Log.d(TAG, "Stage 1: Evaluating ${songs.size} raw candidate songs")

        val recentEligibleSongs = songs.filter { song ->
            val releaseMs = releaseDateCache[song.videoId] ?: (now - (3 * DAY_MS))
            val daysOld = (now - releaseMs).toDouble() / DAY_MS
            val isEligible = daysOld <= maxReleaseDays
            if (!isEligible) {
                Log.d(TAG, "🚫 Filtered out \"${song.title}\" - Released ${String.format("%.1f", daysOld)} days ago (> ${maxReleaseDays}d)")
            }
            isEligible
        }

        Log.d(TAG, "✅ Stage 2: ${recentEligibleSongs.size} / ${songs.size} songs passed 30-day release filter")

        val scoredList = recentEligibleSongs.map { song ->
            Pair(song, calculateTrendingScoreMeta(song))
        }.sortedByDescending { it.second.score }

        Log.d(TAG, "🏆 Stage 3: Top Trending Rankings:")
        scoredList.take(5).forEachIndexed { idx, pair ->
            val s = pair.first
            val m = pair.second
            Log.d(TAG, "   #${idx + 1} \"${s.title}\" | Score: ${String.format("%.2f", m.score)} | DaysOld: ${String.format("%.1f", m.daysOld)}d | Boost: +${String.format("%.1f", m.boost)} | Plays: ${m.weightedPlays}")
        }

        return scoredList.map { it.first }
    }

    fun rankTrendingSongs(songs: List<YouTubeSong>, timeWindow: Any? = null): List<YouTubeSong> {
        return getTrendingRecentReleases(songs, 30)
    }

    fun getTopChartbusters(songs: List<YouTubeSong>): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        return songs.sortedByDescending { song ->
            analytics.getEventCount(song.videoId, "play", 30 * DAY_MS)
        }
    }
}
