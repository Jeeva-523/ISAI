package com.saavn.music.data.repository

import com.saavn.music.data.api.JioSaavnApiService
import com.saavn.music.data.crypto.DesCrypto
import com.saavn.music.data.model.AudioQuality
import com.saavn.music.data.model.RawSongItem
import com.saavn.music.data.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(
    private val api: JioSaavnApiService = JioSaavnApiService.create()
) {

    suspend fun search(query: String): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.trim()
            val response = api.searchSongs(query = cleanQuery)
            val songs = response.results?.mapNotNull { it.toSongItem() } ?: emptyList()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrending(preferredLanguages: List<String> = emptyList(), limit: Int = 60): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        try {
            val languageQueryMap = mapOf(
                "tamil" to listOf(
                    "Latest Tamil Hits",
                    "Tamil Top Hits",
                    "Sai Abhyankkar Hits",
                    "Amaran Tamil Songs",
                    "The Greatest Of All Time Tamil Songs",
                    "Anirudh Ravichander Tamil Hits"
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
                        queries.add("Latest $lang Hits")
                    }
                }
            }

            if (queries.isEmpty()) {
                queries.addAll(listOf(
                    "Latest Tamil Hits",
                    "Tamil Top Hits",
                    "Sai Abhyankkar Hits",
                    "Amaran Tamil Songs",
                    "Anirudh Ravichander Tamil Hits"
                ))
            }

            val allSongs = mutableListOf<SongItem>()
            for (q in queries) {
                try {
                    val response = api.searchSongs(query = q, limit = 20)
                    val songs = response.results?.mapNotNull { it.toSongItem() } ?: emptyList()
                    allSongs.addAll(songs)
                } catch (_: Exception) {}
            }

            val filtered = allSongs.filterNot { song ->
                val title = song.title.lowercase()
                title.contains("trending") || title.contains("jukebox") || title.contains("full album") || 
                title.contains("non stop") || title.contains("compilation") || title.contains("remaster") ||
                song.playCount < 10000L
            }.sortedByDescending { it.playCount }

            Result.success(if (filtered.isNotEmpty()) filtered else allSongs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrending(category: String, limit: Int = 60): Result<List<SongItem>> =
        getTrending(listOf(category), limit)

    suspend fun ensureStreamUrl(song: SongItem): SongItem = withContext(Dispatchers.IO) {
        if (!song.decryptedMediaUrl.isNullOrBlank()) {
            return@withContext song
        }

        val decryptedFromEnc = DesCrypto.decryptMediaUrl(song.encryptedMediaUrl)
        if (!decryptedFromEnc.isNullOrBlank()) {
            return@withContext song.copy(decryptedMediaUrl = decryptedFromEnc)
        }

        try {
            val details = api.getSongDetails(songId = song.id)
            val first = details.songs?.firstOrNull()
            val enc = first?.moreInfo?.encryptedMediaUrl
            val decrypted = DesCrypto.decryptMediaUrl(enc)
            song.copy(
                decryptedMediaUrl = decrypted,
                lyricsId = first?.moreInfo?.lyricsId ?: song.lyricsId,
                hasLyrics = (first?.moreInfo?.hasLyrics == "true") || song.hasLyrics
            )
        } catch (e: Exception) {
            song
        }
    }

    suspend fun resolveStreamUrl(title: String, artist: String = ""): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Search JioSaavn official API
            val searchRes = search(title).getOrNull()
            val first = searchRes?.firstOrNull()
            if (first != null) {
                val ensured = ensureStreamUrl(first)
                val streamUrl = ensured.getStreamUrl(AudioQuality.VERY_HIGH)
                if (!streamUrl.isNullOrBlank()) return@withContext streamUrl
            }
        } catch (_: Exception) {}

        try {
            // 2. Secondary fallback: saavn-api-seven endpoint for direct downloadUrl
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val cleanQuery = java.net.URLEncoder.encode("$title $artist".trim(), "UTF-8")
            val req = okhttp3.Request.Builder()
                .url("https://saavn-api-seven.vercel.app/api/search/songs?query=$cleanQuery&limit=3")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val json = org.json.JSONObject(body)
                val results = json.optJSONObject("data")?.optJSONArray("results")
                    ?: json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val songObj = results.getJSONObject(0)
                    val downloadUrls = songObj.optJSONArray("downloadUrl")
                    if (downloadUrls != null && downloadUrls.length() > 0) {
                        for (i in (downloadUrls.length() - 1) downTo 0) {
                            val item = downloadUrls.getJSONObject(i)
                            val url = item.optString("url")
                            if (url.isNotBlank()) return@withContext url
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }


    suspend fun getLyrics(song: SongItem): Result<String> = withContext(Dispatchers.IO) {
        try {
            val targetId = if (!song.lyricsId.isNullOrBlank()) song.lyricsId else song.id
            val response = api.getLyrics(lyricsId = targetId)
            val rawLyrics = response.lyrics ?: response.snippet
            if (rawLyrics.isNullOrBlank()) {
                Result.failure(Exception("Lyrics not available for this song"))
            } else {
                val cleaned = rawLyrics
                    .replace("<br>", "\n")
                    .replace("<br/>", "\n")
                    .replace("<br />", "\n")
                    .replace("&quot;", "\"")
                    .replace("&#039;", "'")
                    .replace("&amp;", "&")
                    .trim()
                Result.success(cleaned)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun RawSongItem.toSongItem(): SongItem? {
        val songId = id ?: return null
        val songTitle = cleanHtml(title ?: "Unknown Title")
        val artists = cleanHtml(moreInfo?.artistMap?.primaryArtists?.joinToString(", ") { it.name ?: "" }
            ?: subtitle
            ?: "")
        val album = cleanHtml(moreInfo?.album ?: "")
        val rawImage = image ?: ""
        val highResImage = rawImage.replace("150x150", "500x500").replace("50x50", "500x500")
        val dur = moreInfo?.duration?.toLongOrNull() ?: 0L
        val encUrl = moreInfo?.encryptedMediaUrl
        val decUrl = DesCrypto.decryptMediaUrl(encUrl)

        return SongItem(
            id = songId,
            title = songTitle,
            subtitle = cleanHtml(subtitle ?: ""),
            artistNames = artists,
            albumName = album,
            albumId = moreInfo?.albumId,
            imageUrl = highResImage,
            durationSeconds = dur,
            year = year ?: "",
            language = language ?: "",
            hasLyrics = moreInfo?.hasLyrics == "true",
            lyricsId = moreInfo?.lyricsId,
            encryptedMediaUrl = encUrl,
            decryptedMediaUrl = decUrl,
            playCount = playCount?.toLongOrNull() ?: 0L
        )
    }

    private fun cleanHtml(text: String): String {
        if (text.isBlank()) return ""
        var cleaned = text
        var prev = ""
        var pass = 0
        while (cleaned != prev && pass < 5) {
            prev = cleaned
            pass++
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
}
