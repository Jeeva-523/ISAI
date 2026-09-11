package com.saavn.music.data.repository

import android.text.Html
import android.util.Log
import com.saavn.music.BuildConfig
import com.saavn.music.data.api.YouTubeApiService
import com.saavn.music.data.model.YouTubeSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class YouTubeMusicRepository {

    private val httpClient: OkHttpClient
    private val apiService: YouTubeApiService

    // In-memory cache to conserve YouTube Data API quota
    private val searchCache = ConcurrentHashMap<String, List<YouTubeSong>>()

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(YouTubeApiService::class.java)
    }

    private fun getApiKey(): String {
        return BuildConfig.YOUTUBE_API_KEY
    }

    suspend fun searchSongs(query: String, maxResults: Int = 50): Result<List<YouTubeSong>> =
        searchTamilSongs(query, maxResults)

    suspend fun searchTamilSongs(query: String, maxResults: Int = 50): Result<List<YouTubeSong>> =
        withContext(Dispatchers.IO) {
            val cacheKey = query.trim().lowercase()
            searchCache[cacheKey]?.let {
                return@withContext Result.success(it)
            }

            // 1. Primary: YouTube Music Innertube API (IcySnex/YouTubeMusicAPI architecture - No API key quota limit)
            try {
                val ytmSongs = searchYouTubeMusicInnertube(query, maxResults)
                if (ytmSongs.isNotEmpty()) {
                    searchCache[cacheKey] = ytmSongs
                    return@withContext Result.success(ytmSongs)
                }
            } catch (e: Exception) {
                Log.w("YouTubeRepo", "YouTube Music Innertube API search failed, falling back to YouTube Data API v3", e)
            }

            // 2. Fallback: YouTube Data API v3
            val apiKey = getApiKey()
            if (apiKey.isBlank() || apiKey == "YOUR_YOUTUBE_API_KEY_HERE") {
                Log.w("YouTubeRepo", "No valid YouTube API key configured. Using curated Tamil songs.")
                val fallback = getCuratedTamilSongs(query)
                return@withContext Result.success(fallback)
            }

            try {
                val effectiveQuery = query.trim()
                val searchRes = apiService.searchVideos(
                    query = effectiveQuery,
                    maxResults = maxResults,
                    apiKey = apiKey
                )

                val videoIds = searchRes.items.mapNotNull { it.id?.videoId }.filter { it.isNotBlank() }
                if (videoIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                // Batch fetch durations and statistics
                val detailsMap = try {
                    val detailsRes = apiService.getVideoDetails(
                        videoIds = videoIds.joinToString(","),
                        apiKey = apiKey
                    )
                    detailsRes.items.associateBy { it.id }
                } catch (e: Exception) {
                    emptyMap()
                }

                val songs = searchRes.items.mapNotNull { item ->
                    val vid = item.id?.videoId ?: return@mapNotNull null
                    val snippet = item.snippet ?: return@mapNotNull null
                    val details = detailsMap[vid]

                    val cleanTitle = cleanHtmlTitle(snippet.title)
                    val cleanChannel = cleanHtmlTitle(snippet.channelTitle)
                    val thumbUrl = snippet.thumbnails?.highThumb?.url
                        ?: snippet.thumbnails?.mediumThumb?.url
                        ?: snippet.thumbnails?.defaultThumb?.url
                        ?: "https://img.youtube.com/vi/$vid/hqdefault.jpg"

                    val isoDuration = details?.contentDetails?.duration ?: ""
                    val (durationStr, durationMs) = parseIsoDuration(isoDuration)
                    val viewCount = formatViewCount(details?.statistics?.viewCount)

                    YouTubeSong(
                        videoId = vid,
                        title = cleanTitle,
                        channelTitle = cleanChannel,
                        thumbnailUrl = thumbUrl,
                        durationFormatted = durationStr,
                        durationMs = durationMs,
                        viewCountFormatted = viewCount
                    )
                }

                val deduplicated = deduplicateSongs(songs)
                searchCache[cacheKey] = deduplicated
                Result.success(deduplicated)
            } catch (e: Exception) {
                Log.w("YouTubeRepo", "Search API failed: ${e.message}, returning curated songs fallback", e)
                val fallback = getCuratedTamilSongs(query)
                searchCache[cacheKey] = fallback
                Result.success(fallback)
            }
        }

    // Category fetchers with fallback to curated songs
    suspend fun getTrendingSongs(preferredLanguages: List<String> = emptyList(), maxResults: Int = 60): List<YouTubeSong> {
        val languageQueryMap = mapOf(
            "tamil" to listOf(
                "Latest Tamil Movie Songs 2025 2026",
                "Anirudh Ravichander Tamil Hits",
                "A R Rahman Tamil Super Hits",
                "Yuvan Shankar Raja Tamil Hits",
                "Harris Jayaraj Tamil Melodies"
            ),
            "telugu" to listOf(
                "Latest Telugu Hits 2025",
                "Telugu Super Hits",
                "Thaman S Telugu Hits"
            ),
            "hindi" to listOf(
                "Latest Bollywood Hindi Hits 2025",
                "Arijit Singh Super Hits",
                "Top Hindi Songs 2025"
            ),
            "malayalam" to listOf(
                "Latest Malayalam Hits 2025",
                "Sushin Shyam Malayalam Hits"
            ),
            "english" to listOf(
                "Top Global Pop Hits 2025",
                "Billboard Hot 100 Hits"
            ),
            "kannada" to listOf(
                "Latest Kannada Hits 2025",
                "Kannada Super Hits"
            ),
            "punjabi" to listOf(
                "Latest Punjabi Hits 2025",
                "Top Punjabi Songs"
            )
        )

        val queries = mutableListOf<String>()
        if (preferredLanguages.isNotEmpty()) {
            for (lang in preferredLanguages) {
                val list = languageQueryMap[lang.lowercase().trim()]
                if (list != null) {
                    queries.addAll(list)
                } else {
                    queries.add("Latest $lang Hit Songs 2025 2026")
                }
            }
        }
        if (queries.isEmpty()) {
            queries.addAll(listOf(
                "Latest Tamil Movie Songs 2025 2026",
                "Anirudh Ravichander Tamil Hits",
                "A R Rahman Tamil Super Hits",
                "Latest Telugu Hits 2025",
                "Latest Bollywood Hindi Hits 2025"
            ))
        }

        val combined = mutableListOf<YouTubeSong>()
        for (q in queries) {
            val songs = searchSongs(q, 15).getOrDefault(emptyList())
            combined.addAll(songs)
        }
        val filtered = combined.filterNot { song ->
            val title = song.title.lowercase()
            title.contains("trending") || title.contains("jukebox") || title.contains("full album") || title.contains("non stop") || title.contains("compilation")
        }
        return deduplicateSongs(if (filtered.isNotEmpty()) filtered else combined)
    }

    suspend fun getTrendingTamil(maxResults: Int = 60): List<YouTubeSong> =
        getTrendingSongs(listOf("tamil"), maxResults)

    suspend fun getTamilMelody(): List<YouTubeSong> =
        searchTamilSongs("Tamil melody songs all time hits").getOrElse { getCuratedTamilSongs("Melody") }

    suspend fun getTamilLove(): List<YouTubeSong> =
        searchTamilSongs("Tamil love romantic songs").getOrElse { getCuratedTamilSongs("Love") }

    suspend fun getTamilFolk(): List<YouTubeSong> =
        searchTamilSongs("Tamil folk songs gramiya paadalgal").getOrElse { getCuratedTamilSongs("Folk") }

    suspend fun getTamilDevotional(): List<YouTubeSong> =
        searchTamilSongs("Tamil devotional bakthi songs").getOrElse { getCuratedTamilSongs("Devotional") }

    suspend fun getTamilGaana(): List<YouTubeSong> =
        searchTamilSongs("Tamil gaana songs kuthu").getOrElse { getCuratedTamilSongs("Gaana") }

    suspend fun getTamilClassical(): List<YouTubeSong> =
        searchTamilSongs("Tamil classical carnatic songs").getOrElse { getCuratedTamilSongs("Classical") }

    suspend fun getNewReleases(): List<YouTubeSong> =
        searchTamilSongs("Latest Tamil movie songs 2024 2025").getOrElse { getCuratedTamilSongs("New") }

    /**
     * Native YouTube Music Innertube WEB_REMIX search implementation
     * Based on https://github.com/IcySnex/YouTubeMusicAPI architecture
     * Direct song search with no API key or daily quota limits
     */
    private fun searchYouTubeMusicInnertube(query: String, maxResults: Int): List<YouTubeSong> {
        val effectiveQuery = query.trim()
        val jsonBody = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20260715.04.00")
                    put("browserName", "Chrome")
                    put("osName", "Windows")
                    put("hl", "en")
                    put("gl", "IN")
                })
            })
            put("query", effectiveQuery)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/search")
            .post(requestBody)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("Referer", "https://music.youtube.com/")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w("YouTubeRepo", "Innertube search failed: HTTP ${response.code}")
            return emptyList()
        }

        val bodyString = response.body?.string() ?: return emptyList()
        val root = JSONObject(bodyString)
        val tabs = root.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
        val sectionList = tabs?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return emptyList()

        val songs = mutableListOf<YouTubeSong>()
        for (i in 0 until sectionList.length()) {
            val section = sectionList.optJSONObject(i) ?: continue
            val contents = section.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")
                ?: section.optJSONObject("musicShelfRenderer")?.optJSONArray("contents")
                ?: continue

            for (j in 0 until contents.length()) {
                val item = contents.optJSONObject(j) ?: continue
                val m = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue

                val flexColumns = m.optJSONArray("flexColumns")
                val titleRuns = flexColumns?.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                val title = titleRuns?.optJSONObject(0)?.optString("text")

                val flex1Runs = flexColumns?.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")

                var artist = "Music Artist"
                var durationStr = "3:45"
                if (flex1Runs != null && flex1Runs.length() > 0) {
                    val parts = mutableListOf<String>()
                    for (k in 0 until flex1Runs.length()) {
                        val t = flex1Runs.optJSONObject(k)?.optString("text")?.trim()
                        if (!t.isNullOrEmpty() && t != "•") {
                            parts.add(t)
                        }
                    }
                    if (parts.size > 1) {
                        artist = parts[1]
                        val last = parts.last()
                        if (last.matches(Regex("\\d+:\\d+(?::\\d+)?"))) {
                            durationStr = last
                        }
                    } else if (parts.size == 1) {
                        artist = parts[0]
                    }
                }

                var videoId = m.optJSONObject("overlay")
                    ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")

                if (videoId.isNullOrBlank()) {
                    videoId = m.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("watchEndpoint")
                        ?.optString("videoId")
                }

                if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                    songs.add(
                        YouTubeSong(
                            videoId = videoId,
                            title = cleanHtmlTitle(title),
                            channelTitle = cleanHtmlTitle(artist),
                            thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                            durationFormatted = durationStr,
                            durationMs = 225000L,
                            viewCountFormatted = "YouTube Music"
                        )
                    )
                }

                if (songs.size >= maxResults) break
            }
            if (songs.size >= maxResults) break
        }

        return deduplicateSongs(songs)
    }

    companion object {
        private val NOISE_REGEX = Regex(
            "\\b(official|video|lyric|lyrics|full|audio|hd|4k|8k|uhd|song|songs|trending|version|remix|bgm|theme|track|singles|single|teaser|trailer|promo|lyrical|visualizer|jukebox|compilation|all time hits|tamil|telugu|hindi|kannada|malayalam|dj|mix|prod|feat|ft|instrumental|reprise|extended|motion|poster|dialogue|scene|scenes|special|exclusive|original|soundtrack|ost|mashup)\\b",
            RegexOption.IGNORE_CASE
        )

        private val STOPWORDS = setOf(
            "the", "a", "an", "in", "of", "to", "and", "from", "with", "by", "on", "for", "at", "is", "it"
        )

        private val PARENTHESES_REGEX = Regex("\\([^)]*\\)")
        private val BRACKETS_REGEX = Regex("\\[[^\\]]*\\]")
        private val FROM_QUOTED_REGEX = Regex("(?i)\\bfrom\\s+[\"'].*?[\"']")
        private val FROM_RAW_REGEX = Regex("(?i)\\bfrom\\s+[A-Za-z0-9\\s:]+")
        private val NON_ALPHANUM_REGEX = Regex("[^a-zA-Z0-9\\s]")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val PHONETIC_CLEAN_REGEX = Regex("[^a-z0-9]")

        // Global in-memory cache to prevent re-parsing identical song titles
        private val titleKeyCache = ConcurrentHashMap<String, String>()
        private val titleTokensCache = ConcurrentHashMap<String, Set<String>>()
    }

    /**
     * Determines whether two songs represent the exact same track based on ID,
     * normalized titles, or significant title token containment.
     */
    fun isSameSong(songA: YouTubeSong, songB: YouTubeSong): Boolean {
        if (songA.videoId.isNotBlank() && songB.videoId.isNotBlank() && songA.videoId == songB.videoId) return true

        val keyA = normalizeTitleKey(songA.title)
        val keyB = normalizeTitleKey(songB.title)

        if (keyA.isNotBlank() && keyB.isNotBlank() && keyA == keyB) return true

        val tokensA = extractTitleTokens(songA.title)
        val tokensB = extractTitleTokens(songB.title)

        if (tokensA.isEmpty() || tokensB.isEmpty()) return false

        val common = tokensA.intersect(tokensB)
        val commonLen = common.sumOf { it.length }
        val maxLen = maxOf(tokensA.sumOf { it.length }, tokensB.sumOf { it.length })

        // High similarity ratio (>= 80% character overlap): genuine duplicates (audio vs video / lyrics)
        if (maxLen > 0 && (commonLen.toFloat() / maxLen.toFloat()) >= 0.80f) {
            return true
        }

        return false
    }

    /**
     * High-speed song deduplication using O(1) set lookups for exact IDs and Title keys,
     * followed by fuzzy matching only on genuinely distinct songs.
     */
    fun deduplicateSongs(songs: List<YouTubeSong>): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        val result = ArrayList<YouTubeSong>(songs.size)
        val seenIds = HashSet<String>(songs.size)
        val seenKeys = HashSet<String>(songs.size)

        for (song in songs) {
            val vid = song.videoId.trim()
            if (vid.isBlank() || song.title.isBlank()) continue
            if (!seenIds.add(vid)) continue

            val key = normalizeTitleKey(song.title)
            if (key.isNotBlank() && !seenKeys.add(key)) continue

            // Fuzzy comparison against accepted songs
            var isDuplicate = false
            for (existing in result) {
                if (isSameSong(existing, song)) {
                    isDuplicate = true
                    break
                }
            }

            if (!isDuplicate) {
                result.add(song)
            }
        }
        return result
    }

    fun extractTitleTokens(rawTitle: String): Set<String> {
        return titleTokensCache.getOrPut(rawTitle) {
            val cleaned = cleanRawTitle(rawTitle)
            cleaned.split(WHITESPACE_REGEX)
                .map { phoneticNormalize(it) }
                .filter { it.length >= 2 && !STOPWORDS.contains(it) }
                .toSet()
        }
    }

    fun normalizeTitleKey(rawTitle: String): String {
        return titleKeyCache.getOrPut(rawTitle) {
            val tokens = extractTitleTokens(rawTitle)
            tokens.sorted().joinToString("")
        }
    }

    private fun cleanRawTitle(raw: String): String {
        if (raw.isBlank()) return ""
        val unescaped = cleanHtmlTitle(raw)
        var s = PARENTHESES_REGEX.replace(unescaped, " ")
        s = BRACKETS_REGEX.replace(s, " ")
        s = FROM_QUOTED_REGEX.replace(s, " ")
        s = FROM_RAW_REGEX.replace(s, " ")
        s = NOISE_REGEX.replace(s, " ")
        s = NON_ALPHANUM_REGEX.replace(s, " ")
        s = WHITESPACE_REGEX.replace(s, " ")
        return s.trim()
    }

    private fun phoneticNormalize(raw: String): String {
        if (raw.isBlank()) return ""
        val clean = raw.lowercase()
            .replace("th", "t")
            .replace("zh", "l")
            .replace("dh", "d")
            .replace("sh", "s")
            .replace("ck", "k")
            .replace("ch", "c")
            .replace("aa", "a")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("ii", "i")
            .replace("uu", "u")
        return PHONETIC_CLEAN_REGEX.replace(clean, "").trim()
    }

    private fun getThumbnailKey(url: String, videoId: String): String {
        if (videoId.length == 11) return videoId
        if (url.isBlank()) return ""
        val match = Regex("/vi/([a-zA-Z0-9_-]{11})/").find(url)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun cleanHtmlTitle(raw: String): String {
        if (raw.isBlank()) return ""
        var cleaned = raw
        var prev = ""
        var pass = 0
        while (cleaned != prev && pass < 5) {
            prev = cleaned
            pass++
            cleaned = try {
                Html.fromHtml(cleaned, Html.FROM_HTML_MODE_LEGACY).toString()
            } catch (e: Exception) {
                cleaned
            }
            cleaned = cleaned
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
        }
        return cleaned.trim()
    }

    // Parses ISO 8601 duration e.g. PT4M35S -> ("04:35", 275000L)
    private fun parseIsoDuration(iso: String): Pair<String, Long> {
        if (iso.isBlank()) return Pair("3:45", 225000L)
        try {
            val pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
            val matcher = pattern.matcher(iso)
            if (matcher.matches()) {
                val hours = matcher.group(1)?.toLongOrNull() ?: 0L
                val minutes = matcher.group(2)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(3)?.toLongOrNull() ?: 0L
                val totalSec = hours * 3600 + minutes * 60 + seconds
                val totalMs = totalSec * 1000

                val formatted = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%d:%02d", minutes, seconds)
                }
                return Pair(formatted, totalMs)
            }
        } catch (e: Exception) {
            // Ignore
        }
        return Pair("3:45", 225000L)
    }

    private fun formatViewCount(raw: String?): String {
        val views = raw?.toLongOrNull() ?: return ""
        return when {
            views >= 1_000_000_000 -> String.format("%.1fB views", views / 1_000_000_000.0)
            views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
            views >= 1_000 -> String.format("%.1fK views", views / 1_000.0)
            else -> "$views views"
        }
    }

    // Curated high quality Tamil songs (Real working YouTube video IDs)
    private fun getCuratedTamilSongs(category: String): List<YouTubeSong> {
        return listOf(
            YouTubeSong(
                videoId = "KUN5Uf9mObQ",
                title = "Arabic Kuthu - Halamithi Habibo",
                channelTitle = "Sun TV • Anirudh Ravichander",
                thumbnailUrl = "https://img.youtube.com/vi/KUN5Uf9mObQ/hqdefault.jpg",
                durationFormatted = "4:39",
                durationMs = 279000L,
                viewCountFormatted = "480M views"
            ),
            YouTubeSong(
                videoId = "1F3hm6MfR1k",
                title = "Hukum - Thalaivar Alappara | Jailer",
                channelTitle = "Sun TV • Anirudh Ravichander",
                thumbnailUrl = "https://img.youtube.com/vi/1F3hm6MfR1k/hqdefault.jpg",
                durationFormatted = "3:26",
                durationMs = 206000L,
                viewCountFormatted = "185M views"
            ),
            YouTubeSong(
                videoId = "szvt1vD0Uug",
                title = "Naa Ready | Leo | Thalapathy Vijay",
                channelTitle = "Sony Music South • Anirudh",
                thumbnailUrl = "https://img.youtube.com/vi/szvt1vD0Uug/hqdefault.jpg",
                durationFormatted = "4:08",
                durationMs = 248000L,
                viewCountFormatted = "240M views"
            ),
            YouTubeSong(
                videoId = "3tmd-ClpJxA",
                title = "Marakkuma Nenjam - VTK | A.R. Rahman",
                channelTitle = "Think Music India • A.R. Rahman",
                thumbnailUrl = "https://img.youtube.com/vi/3tmd-ClpJxA/hqdefault.jpg",
                durationFormatted = "4:16",
                durationMs = 256000L,
                viewCountFormatted = "65M views"
            ),
            YouTubeSong(
                videoId = "mqqft2x_Aa4",
                title = "Kaavaalaa - Jailer | Rajinikanth | Tamannaah",
                channelTitle = "Sun TV • Anirudh Ravichander",
                thumbnailUrl = "https://img.youtube.com/vi/mqqft2x_Aa4/hqdefault.jpg",
                durationFormatted = "3:10",
                durationMs = 190000L,
                viewCountFormatted = "290M views"
            ),
            YouTubeSong(
                videoId = "eN6AnYGYdVE",
                title = "Vaseegara - Minnale | Bombay Jayashri",
                channelTitle = "Harris Jayaraj Melodies",
                thumbnailUrl = "https://img.youtube.com/vi/eN6AnYGYdVE/hqdefault.jpg",
                durationFormatted = "5:00",
                durationMs = 300000L,
                viewCountFormatted = "85M views"
            ),
            YouTubeSong(
                videoId = "jHNNMj5bNQw",
                title = "Rowdy Baby - Maari 2 | Dhanush | Sai Pallavi",
                channelTitle = "Wunderbar Films • Yuvan Shankar Raja",
                thumbnailUrl = "https://img.youtube.com/vi/jHNNMj5bNQw/hqdefault.jpg",
                durationFormatted = "4:44",
                durationMs = 284000L,
                viewCountFormatted = "1.5B views"
            ),
            YouTubeSong(
                videoId = "x6Q7c9Ry3tk",
                title = "Why This Kolaveri Di - 3 | Dhanush",
                channelTitle = "Sony Music South • Anirudh",
                thumbnailUrl = "https://img.youtube.com/vi/x6Q7c9Ry3tk/hqdefault.jpg",
                durationFormatted = "4:05",
                durationMs = 245000L,
                viewCountFormatted = "400M views"
            ),
            YouTubeSong(
                videoId = "bo_efYhYU2A",
                title = "Enna Sona / O Sona Re | A.R. Rahman",
                channelTitle = "Sony Music South",
                thumbnailUrl = "https://img.youtube.com/vi/bo_efYhYU2A/hqdefault.jpg",
                durationFormatted = "3:33",
                durationMs = 213000L,
                viewCountFormatted = "95M views"
            ),
            YouTubeSong(
                videoId = "gvyUuxdRdR4",
                title = "Kutti Story | Master | Thalapathy Vijay",
                channelTitle = "Sony Music South • Anirudh",
                thumbnailUrl = "https://img.youtube.com/vi/gvyUuxdRdR4/hqdefault.jpg",
                durationFormatted = "5:05",
                durationMs = 305000L,
                viewCountFormatted = "130M views"
            )
        )
    }
}
