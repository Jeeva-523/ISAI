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
        val targetLangs = preferredLanguages.ifEmpty { listOf("tamil") }
        if (queries.isEmpty()) {
            queries.addAll(listOf(
                "Latest Tamil Movie Songs 2025 2026",
                "Anirudh Ravichander Tamil Hits",
                "A R Rahman Tamil Super Hits"
            ))
        }

        val combined = mutableListOf<YouTubeSong>()
        for (q in queries) {
            val songs = searchSongs(q, 15).getOrDefault(emptyList())
            combined.addAll(songs)
        }
        val languageFiltered = combined.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, targetLangs) }
        val filtered = languageFiltered.filterNot { song ->
            val title = song.title.lowercase()
            title.contains("trending") || title.contains("jukebox") || title.contains("full album") || title.contains("non stop") || title.contains("compilation")
        }
        return deduplicateSongs(if (filtered.isNotEmpty()) filtered else languageFiltered)
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
                ?: section.optJSONObject("musicCardShelfRenderer")?.optJSONArray("contents")
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
                var album = ""
                var durationStr = "3:45"
                if (flex1Runs != null && flex1Runs.length() > 0) {
                    val parts = mutableListOf<String>()
                    for (k in 0 until flex1Runs.length()) {
                        val t = flex1Runs.optJSONObject(k)?.optString("text")?.trim()
                        if (!t.isNullOrEmpty() && t != "•") {
                            parts.add(t)
                        }
                    }
                    if (parts.isNotEmpty()) {
                        val last = parts.last()
                        val hasDuration = last.matches(Regex("\\d+:\\d+(?::\\d+)?"))
                        if (hasDuration) {
                            durationStr = last
                        }
                        val metadataParts = if (hasDuration) parts.dropLast(1) else parts
                        val contentParts = if (metadataParts.firstOrNull()?.equals("Song", ignoreCase = true) == true ||
                            metadataParts.firstOrNull()?.equals("Video", ignoreCase = true) == true) {
                            metadataParts.drop(1)
                        } else {
                            metadataParts
                        }

                        if (contentParts.isNotEmpty()) {
                            artist = contentParts[0]
                            if (contentParts.size > 1) {
                                album = contentParts[1]
                            }
                        }
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

                val displayChannel = if (album.isNotBlank() && !artist.contains(album, ignoreCase = true)) {
                    "$artist • $album"
                } else {
                    artist
                }

                if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                    songs.add(
                        YouTubeSong(
                            videoId = videoId,
                            title = cleanHtmlTitle(title),
                            channelTitle = cleanHtmlTitle(displayChannel),
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

        val CURATED_TAMIL_SONGS: List<YouTubeSong> = listOf(
            YouTubeSong(
                videoId = "KUN5Uf9mObQ",
                title = "Arabic Kuthu - Halamithi Habibo",
                channelTitle = "Anirudh Ravichander • Beast",
                thumbnailUrl = "https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg",
                durationFormatted = "4:39",
                durationMs = 279000L,
                viewCountFormatted = "53M views",
                audioUrl = "https://aac.saavncdn.com/510/9d96fc7ddd4ffadb745f25aed86f7a4e_320.mp4",
                playCount = 53472094L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "1F3hm6MfR1k",
                title = "Hukum",
                channelTitle = "Dinker kalvala • Jailer",
                thumbnailUrl = "https://c.saavncdn.com/435/Jailer-Telugu-2023-20230810132954-500x500.jpg",
                durationFormatted = "3:27",
                durationMs = 207000L,
                viewCountFormatted = "25M views",
                audioUrl = "https://aac.saavncdn.com/435/4161a58e6cff0010c02431e6c21728d9_320.mp4",
                playCount = 25395492L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "szvt1vD0Uug",
                title = "Naa Ready",
                channelTitle = "Vijay • Leo",
                thumbnailUrl = "https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg",
                durationFormatted = "4:08",
                durationMs = 248000L,
                viewCountFormatted = "34M views",
                audioUrl = "https://aac.saavncdn.com/415/3789bee89b94522160f1e50b2266d2c4_320.mp4",
                playCount = 34284684L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "3tmd-ClpJxA",
                title = "Marakkuma Nenjam",
                channelTitle = "A.R. Rahman • Vendhu Thanindhathu Kaadu (Original Motion Picture Soundtrack)",
                thumbnailUrl = "https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg",
                durationFormatted = "4:18",
                durationMs = 258000L,
                viewCountFormatted = "10M views",
                audioUrl = "https://aac.saavncdn.com/420/14cb0983229d366c09447dbfb731f351_320.mp4",
                playCount = 10520L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "mqqft2x_Aa4",
                title = "Kaavaalaa",
                channelTitle = "Shilpa Rao • Jailer",
                thumbnailUrl = "https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg",
                durationFormatted = "3:10",
                durationMs = 190000L,
                viewCountFormatted = "21M views",
                audioUrl = "https://aac.saavncdn.com/187/49797372d021638077d8a6b749068bc8_320.mp4",
                playCount = 20930587L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "eN6AnYGYdVE",
                title = "Vaseegara (From \"Minnale\")",
                channelTitle = "Bombay Jayashri • 2 - In - 1 Hits Of Maddy",
                thumbnailUrl = "https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg",
                durationFormatted = "4:59",
                durationMs = 299000L,
                viewCountFormatted = "10M views",
                audioUrl = "https://aac.saavncdn.com/450/4f7b9da8e887586e60b11afb602befac_320.mp4",
                playCount = 9810296L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "jHNNMj5bNQw",
                title = "Rowdy Baby",
                channelTitle = "Dhanush • Maari 2",
                thumbnailUrl = "https://c.saavncdn.com/276/Maari-2-Tamil-2018-20260203193952-500x500.jpg",
                durationFormatted = "4:41",
                durationMs = 281000L,
                viewCountFormatted = "48M views",
                audioUrl = "https://aac.saavncdn.com/276/64b835b4e1829992f8d35f54d6dad5f3_320.mp4",
                playCount = 47932333L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "x6Q7c9Ry3tk",
                title = "Why This Kolaveri Di (The Soup Of Love)",
                channelTitle = "Anirudh Ravichander • 3 (Hindi)",
                thumbnailUrl = "https://c.saavncdn.com/932/3-Hindi-2012-500x500.jpg",
                durationFormatted = "4:23",
                durationMs = 263000L,
                viewCountFormatted = "26M views",
                audioUrl = "https://aac.saavncdn.com/932/7cf7f8a9d9c3faa2633d1605e97ba4a5_320.mp4",
                playCount = 25684207L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "Y6wB5b89E1E",
                title = "Chilla Chilla",
                channelTitle = "Ghibran • Thunivu",
                thumbnailUrl = "https://c.saavncdn.com/blob/842/Thunivu-Tamil-2022-20230217151809-500x500.jpg",
                durationFormatted = "3:42",
                durationMs = 222000L,
                viewCountFormatted = "10M views",
                audioUrl = "https://aac.saavncdn.com/071/342ecd79c19839b794216476801ab364_320.mp4",
                playCount = 9885042L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "vx2u5uUu3DE",
                title = "Vaathi Coming",
                channelTitle = "Anirudh Ravichander • Master",
                thumbnailUrl = "https://c.saavncdn.com/347/Master-Tamil-2020-20200316084627-500x500.jpg",
                durationFormatted = "3:48",
                durationMs = 228000L,
                viewCountFormatted = "50M views",
                audioUrl = "https://aac.saavncdn.com/347/c536b256fca6b96aa432322c11c21fbb_320.mp4",
                playCount = 50273938L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "vB8csw_kkyU",
                title = "Chellamma",
                channelTitle = "Sivakarthikeyan • Doctor",
                thumbnailUrl = "https://c.saavncdn.com/312/Doctor-Tamil-2021-20211005133149-500x500.jpg",
                durationFormatted = "3:56",
                durationMs = 236000L,
                viewCountFormatted = "25M views",
                audioUrl = "https://aac.saavncdn.com/312/9faaf9ef4a0f3596b15adfbf3b9cc273_320.mp4",
                playCount = 24836165L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "mSgN49e_Jyo",
                title = "Dippam Dappam",
                channelTitle = "Anirudh Ravichander • Kaathuvaakula Rendu Kaadhal",
                thumbnailUrl = "https://c.saavncdn.com/403/Kaathuvaakula-Rendu-Kaadhal-Original-Motion-Picture-Soundtrack-Tamil-2022-20220428131043-500x500.jpg",
                durationFormatted = "3:29",
                durationMs = 209000L,
                viewCountFormatted = "20M views",
                audioUrl = "https://aac.saavncdn.com/403/1a833ab353132dfe3d5f48ecd1c80334_320.mp4",
                playCount = 19873227L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "I4_mN6-rC0E",
                title = "Badass",
                channelTitle = "Anirudh Ravichander • Leo",
                thumbnailUrl = "https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg",
                durationFormatted = "3:49",
                durationMs = 229000L,
                viewCountFormatted = "27M views",
                audioUrl = "https://aac.saavncdn.com/415/46a7b21d2a3f4b9e019a7cdff7442c55_320.mp4",
                playCount = 27150544L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "sUzG3h5V6bE",
                title = "Ranjithame",
                channelTitle = "Vijay • Varisu",
                thumbnailUrl = "https://c.saavncdn.com/145/Varisu-Tamil-2022-20221226190213-500x500.jpg",
                durationFormatted = "4:47",
                durationMs = 287000L,
                viewCountFormatted = "23M views",
                audioUrl = "https://aac.saavncdn.com/145/a2e4e1d3758ea68c1a216032e2b23491_320.mp4",
                playCount = 23036680L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "uM5d992fL9g",
                title = "Thee Thalapathy",
                channelTitle = "Silambarasan Tr • Varisu",
                thumbnailUrl = "https://c.saavncdn.com/145/Varisu-Tamil-2022-20221226190213-500x500.jpg",
                durationFormatted = "4:18",
                durationMs = 258000L,
                viewCountFormatted = "17M views",
                audioUrl = "https://aac.saavncdn.com/145/143277c609348c0f2d5a396fef2cd8f3_320.mp4",
                playCount = 17351812L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "8uN1kR4-31w",
                title = "Jimikki Ponnu",
                channelTitle = "Anirudh Ravichander • Varisu",
                thumbnailUrl = "https://c.saavncdn.com/145/Varisu-Tamil-2022-20221226190213-500x500.jpg",
                durationFormatted = "3:44",
                durationMs = 224000L,
                viewCountFormatted = "14M views",
                audioUrl = "https://aac.saavncdn.com/145/b58fb16bcca4b777a5acb7a0a960a5d9_320.mp4",
                playCount = 13853633L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "3XmrZaWVUpE",
                title = "Aalaporaan Thamizhan",
                channelTitle = "A.R. Rahman • Mersal",
                thumbnailUrl = "https://c.saavncdn.com/492/Mersal-Tamil-2017-20170820120559-500x500.jpg",
                durationFormatted = "5:48",
                durationMs = 348000L,
                viewCountFormatted = "15M views",
                audioUrl = "https://aac.saavncdn.com/492/eb45d961a1639b3c3a6df4c8777fa71c_320.mp4",
                playCount = 15206502L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "dJvj2c8d20A",
                title = "Singappenney",
                channelTitle = "A.R. Rahman • Bigil",
                thumbnailUrl = "https://c.saavncdn.com/162/Bigil-Tamil-2019-20191017202521-500x500.jpg",
                durationFormatted = "6:04",
                durationMs = 364000L,
                viewCountFormatted = "11M views",
                audioUrl = "https://aac.saavncdn.com/162/a888165f1ebeda4b0ac6ac7f4be1a0f6_320.mp4",
                playCount = 10705095L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "8kL3yJ_aH4s",
                title = "Verithanam",
                channelTitle = "Vijay • Bigil",
                thumbnailUrl = "https://c.saavncdn.com/162/Bigil-Tamil-2019-20191017202521-500x500.jpg",
                durationFormatted = "4:06",
                durationMs = 246000L,
                viewCountFormatted = "16M views",
                audioUrl = "https://aac.saavncdn.com/162/c005e1ef77af574df7ba1d24bd8ee9ea_320.mp4",
                playCount = 15615056L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "cR77gU2g16s",
                title = "Kadhaippoma",
                channelTitle = "Leon James • Oh My Kadavule",
                thumbnailUrl = "https://c.saavncdn.com/435/Oh-My-Kadavule-Tamil-2020-20200207054852-500x500.jpg",
                durationFormatted = "4:42",
                durationMs = 282000L,
                viewCountFormatted = "14M views",
                audioUrl = "https://aac.saavncdn.com/435/c8c5e3d0895c11377439673577049ddd_320.mp4",
                playCount = 13885652L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "Gg4x336nN2E",
                title = "En Rojaa Neeye",
                channelTitle = "Hesham Abdul Wahab • Kushi (Tamil)",
                thumbnailUrl = "https://c.saavncdn.com/683/Kushi-Tamil-Tamil-2023-20250130073118-500x500.jpg",
                durationFormatted = "4:03",
                durationMs = 243000L,
                viewCountFormatted = "10M views",
                audioUrl = "https://aac.saavncdn.com/683/b9c5368d21c6cab0efe4cf6a05d02f38_320.mp4",
                playCount = 426862L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "w3v1Q3-2qgY",
                title = "Pookkal Pookkum",
                channelTitle = "G.V. Prakash Kumar • Madharasapattinam",
                thumbnailUrl = "https://c.saavncdn.com/960/Madharasapattinam-Tamil-2010-20200627073521-500x500.jpg",
                durationFormatted = "6:36",
                durationMs = 396000L,
                viewCountFormatted = "17M views",
                audioUrl = "https://aac.saavncdn.com/960/c8427f9eff13f8e0e9cce7ad2f2aa841_320.mp4",
                playCount = 16799076L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "xVj_kL3o9yE",
                title = "Adiye",
                channelTitle = "Dhibu Ninan Thomas • Bachelor (Original Motion Picture Soundtrack)",
                thumbnailUrl = "https://c.saavncdn.com/357/Bachelor-Original-Motion-Picture-Soundtrack-Tamil-2021-20251024161148-500x500.jpg",
                durationFormatted = "4:32",
                durationMs = 272000L,
                viewCountFormatted = "47M views",
                audioUrl = "https://aac.saavncdn.com/357/89d4fdbeb263f4b44c7eb6294a269831_320.mp4",
                playCount = 47184103L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "oRdxUFDoQe0",
                title = "Kannaana Kanney",
                channelTitle = "Sid Sriram • Viswasam",
                thumbnailUrl = "https://c.saavncdn.com/006/Viswasam-Tamil-2020-20240321064638-500x500.jpg",
                durationFormatted = "4:30",
                durationMs = 270000L,
                viewCountFormatted = "8M views",
                audioUrl = "https://aac.saavncdn.com/006/40aacc11863cba58d37eefcbd23bee99_320.mp4",
                playCount = 8426328L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "P36eX-189fU",
                title = "Munbe Vaa",
                channelTitle = "Naresh Iyer • Sillunu Oru Kadhal",
                thumbnailUrl = "https://c.saavncdn.com/106/Jillunu-Oru-Kadhal-2006-500x500.jpg",
                durationFormatted = "5:58",
                durationMs = 358000L,
                viewCountFormatted = "38M views",
                audioUrl = "https://aac.saavncdn.com/595/86c6a67ff7120d287cb4484ea020f488_320.mp4",
                playCount = 38122730L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "3r88BwZ_g60",
                title = "Hosanna",
                channelTitle = "A.R. Rahman • Vinnaithaandi Varuvaayaa",
                thumbnailUrl = "https://c.saavncdn.com/880/Vinnaithaandi-Varuvaayaa-Tamil-2010-20260120201226-500x500.jpg",
                durationFormatted = "5:30",
                durationMs = 330000L,
                viewCountFormatted = "7M views",
                audioUrl = "https://aac.saavncdn.com/880/42c2865fcf3f2744c3131d8548e41e29_320.mp4",
                playCount = 7325873L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "kLp65mG9w1Y",
                title = "Anbil Avan",
                channelTitle = "A.R. Rahman • Vinnaithaandi Varuvaayaa",
                thumbnailUrl = "https://c.saavncdn.com/880/Vinnaithaandi-Varuvaayaa-Tamil-2010-20260120201226-500x500.jpg",
                durationFormatted = "4:11",
                durationMs = 251000L,
                viewCountFormatted = "8M views",
                audioUrl = "https://aac.saavncdn.com/880/4927b38215013ed6e2329a6ccd8111e2_320.mp4",
                playCount = 7928872L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "4Gq8o0y123E",
                title = "Kanave Kanave",
                channelTitle = "Anirudh Ravichander • David",
                thumbnailUrl = "https://c.saavncdn.com/470/David-2012-500x500.jpg",
                durationFormatted = "4:46",
                durationMs = 286000L,
                viewCountFormatted = "37M views",
                audioUrl = "https://aac.saavncdn.com/470/763cdd1fafbf1c17a5996ec8ff1e5c0a_320.mp4",
                playCount = 37163415L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "9L0sW81a33w",
                title = "Sirikkadhey",
                channelTitle = "Vignesh Shivan • Remo",
                thumbnailUrl = "https://c.saavncdn.com/110/Remo-Tamil-2016-500x500.jpg",
                durationFormatted = "4:05",
                durationMs = 245000L,
                viewCountFormatted = "8M views",
                audioUrl = "https://aac.saavncdn.com/110/2ec3baf8b62e4d69335de93d3685325e_320.mp4",
                playCount = 8476905L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "e3X9827fghY",
                title = "Neeyum Naanum Anbe",
                channelTitle = "Hiphop Tamizha • Imaikkaa Nodigal (Original Motion Picture Soundtrack)",
                thumbnailUrl = "https://c.saavncdn.com/015/Imaikkaa-Nodigal-Original-Motion-Picture-Soundtrack-Tamil-2018-20251026054442-500x500.jpg",
                durationFormatted = "4:45",
                durationMs = 285000L,
                viewCountFormatted = "15M views",
                audioUrl = "https://aac.saavncdn.com/015/1071c160f1c11471287b4015f5dedf62_320.mp4",
                playCount = 14798996L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "P6q9oK3Lw77",
                title = "Annul Maelae",
                channelTitle = "Harris Jayaraj • Vaaranam Aayiram",
                thumbnailUrl = "https://c.saavncdn.com/635/Vaaranam-Aayiram-Tamil-2008-20190629141128-500x500.jpg",
                durationFormatted = "5:22",
                durationMs = 322000L,
                viewCountFormatted = "9M views",
                audioUrl = "https://aac.saavncdn.com/635/8a800cc2cb33a59266193c0d1c028601_320.mp4",
                playCount = 9266322L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "5w21q9Y68wY",
                title = "Mental Manadhil",
                channelTitle = "A.R. Rahman • O Kadhal Kanmani",
                thumbnailUrl = "https://c.saavncdn.com/336/O-Kadhal-Kanmani-Tamil-2015-20200805153450-500x500.jpg",
                durationFormatted = "3:30",
                durationMs = 210000L,
                viewCountFormatted = "14M views",
                audioUrl = "https://aac.saavncdn.com/336/811c2d826d85ef779a7151da9b9b9273_320.mp4",
                playCount = 14041382L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "VT7412hT91g",
                title = "Katchi Sera (From \"Think Indie\")",
                channelTitle = "Sai Abhyankkar • Katchi Sera (From &quot;Think Indie&quot;)",
                thumbnailUrl = "https://c.saavncdn.com/118/Katchi-Sera-From-Think-Indie-Tamil-2024-20251026074526-500x500.jpg",
                durationFormatted = "3:01",
                durationMs = 181000L,
                viewCountFormatted = "29M views",
                audioUrl = "https://aac.saavncdn.com/118/3456f4e5990e8fb33d7af6678aca034a_320.mp4",
                playCount = 28582825L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "gvyUuxdRdR4",
                title = "Kutti Story",
                channelTitle = "Vijay • Master",
                thumbnailUrl = "https://c.saavncdn.com/347/Master-Tamil-2020-20200316084627-500x500.jpg",
                durationFormatted = "5:02",
                durationMs = 302000L,
                viewCountFormatted = "24M views",
                audioUrl = "https://aac.saavncdn.com/347/a9a507593fa9b9d0f1b448d0fb8c4f07_320.mp4",
                playCount = 23700334L,
                language = "tamil"
            ),
            YouTubeSong(
                videoId = "bo_efYhYU2A",
                title = "Enna Sona",
                channelTitle = "A.R. Rahman • OK Jaanu",
                thumbnailUrl = "https://c.saavncdn.com/835/OK-Jaanu-Original-Motion-Picture-Soundtrack-Hindi-2017-20230203133341-500x500.jpg",
                durationFormatted = "3:33",
                durationMs = 213000L,
                viewCountFormatted = "140M views",
                audioUrl = "https://aac.saavncdn.com/835/3bb5690b365de4a583ed5a98632cc8c4_320.mp4",
                playCount = 142538827L,
                language = "tamil"
            ),
        )

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
