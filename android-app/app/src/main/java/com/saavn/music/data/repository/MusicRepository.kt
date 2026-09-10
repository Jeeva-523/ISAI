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

    suspend fun search(query: String, limit: Int = 40): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.trim()
            val response = api.searchSongs(query = cleanQuery, limit = limit)
            val songs = response.results?.mapNotNull { it.toSongItem() } ?: emptyList()
            val deduped = deduplicateSongItems(songs)
            Result.success(deduped)
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
                    "Anirudh Ravichander Hits"
                ),
                "telugu" to listOf(
                    "Latest Telugu Hits 2025",
                    "Telugu Super Hits",
                    "Thaman S Telugu Hits",
                    "Devi Sri Prasad Telugu Hits"
                ),
                "hindi" to listOf(
                    "Latest Bollywood Hindi Hits 2025",
                    "Arijit Singh Super Hits",
                    "Pritam Bollywood Hits",
                    "Top Hindi Songs 2025"
                ),
                "malayalam" to listOf(
                    "Latest Malayalam Hits 2025",
                    "Sushin Shyam Malayalam Hits",
                    "Malayalam Melody Hits"
                ),
                "english" to listOf(
                    "Top Global Pop Hits 2025",
                    "Billboard Hot 100 Hits",
                    "Ed Sheeran Popular Songs"
                ),
                "kannada" to listOf(
                    "Latest Kannada Hits 2025",
                    "Kannada Super Hits",
                    "Ravi Basrur Hits"
                ),
                "punjabi" to listOf(
                    "Latest Punjabi Hits 2025",
                    "Top Punjabi Songs",
                    "Diljit Dosanjh Hits"
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
                    "The Greatest Of All Time Tamil Songs",
                    "Anirudh Ravichander Hits"
                ))
            }

            val allSongs = mutableListOf<SongItem>()
            for (q in queries) {
                var querySongs: List<SongItem> = emptyList()
                try {
                    val response = api.searchSongs(query = q, limit = 15)
                    querySongs = response.results?.mapNotNull { it.toSongItem() } ?: emptyList()
                } catch (_: Exception) {}

                if (querySongs.isEmpty()) {
                    querySongs = searchSaavnMirror(query = q, limit = 15)
                }
                allSongs.addAll(querySongs)
            }

            val dedupedAll = deduplicateSongItems(allSongs)

            val filtered = dedupedAll.filterNot { song ->
                val title = song.title.lowercase()
                title.contains("trending") || title.contains("jukebox") || title.contains("full album") || 
                title.contains("non stop") || title.contains("compilation") || title.contains("remaster") ||
                song.playCount < 10000L
            }.sortedByDescending { it.playCount }

            Result.success(if (filtered.isNotEmpty()) filtered.take(limit) else dedupedAll.take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val NOISE_REGEX = Regex(
            "\\b(official|video|lyric|lyrics|full|audio|hd|4k|8k|uhd|song|songs|trending|version|remix|bgm|theme|track|singles|single|teaser|trailer|promo|lyrical|visualizer|jukebox|compilation|all time hits|tamil|telugu|hindi|kannada|malayalam|dj|mix|prod|feat|ft|instrumental|reprise|extended|motion|poster|dialogue|scene|scenes|special|exclusive|original|soundtrack|ost|mashup)\\b",
            RegexOption.IGNORE_CASE
        )

        private val STOPWORDS = setOf(
            "the", "a", "an", "in", "of", "to", "and", "from", "with", "by", "on", "for", "at", "is", "it"
        )
    }

    fun isSameSongItem(songA: SongItem, songB: SongItem): Boolean {
        if (songA.id.isNotBlank() && songB.id.isNotBlank() && songA.id == songB.id) return true

        val keyA = normalizeTitleKey(songA.title)
        val keyB = normalizeTitleKey(songB.title)

        if (keyA.isNotBlank() && keyB.isNotBlank() && keyA == keyB) return true

        if (keyA.length >= 4 && keyB.length >= 4) {
            if (keyA.contains(keyB) || keyB.contains(keyA)) {
                return true
            }
        }

        val tokensA = extractTitleTokens(songA.title)
        val tokensB = extractTitleTokens(songB.title)

        if (tokensA.isEmpty() || tokensB.isEmpty()) return false

        val aInB = tokensA.all { tokensB.contains(it) }
        val bInA = tokensB.all { tokensA.contains(it) }
        val common = tokensA.intersect(tokensB)
        val commonLen = common.sumOf { it.length }

        if ((aInB || bInA) && commonLen >= 4) {
            return true
        }

        val significantCommon = common.filter { it.length >= 3 }
        if (significantCommon.size >= 2) {
            return true
        }

        return false
    }

    fun deduplicateSongItems(songs: List<SongItem>): List<SongItem> {
        if (songs.isEmpty()) return emptyList()
        val result = mutableListOf<SongItem>()

        for (song in songs) {
            val vid = song.id.trim()
            if (vid.isBlank() || song.title.isBlank()) continue

            val isDuplicate = result.any { existing ->
                isSameSongItem(existing, song)
            }

            if (!isDuplicate) {
                result.add(song)
            }
        }
        return result
    }

    fun extractTitleTokens(rawTitle: String): Set<String> {
        val cleaned = cleanRawTitle(rawTitle)
        return cleaned.split(Regex("\\s+"))
            .map { phoneticNormalize(it) }
            .filter { it.length >= 2 && !STOPWORDS.contains(it) }
            .toSet()
    }

    fun normalizeTitleKey(rawTitle: String): String {
        val tokens = extractTitleTokens(rawTitle)
        return tokens.sorted().joinToString("")
    }

    private fun cleanRawTitle(raw: String): String {
        if (raw.isBlank()) return ""
        val unescaped = cleanHtml(raw)
        return unescaped
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("(?i)\\bfrom\\s+[\"'].*?[\"']"), " ")
            .replace(Regex("(?i)\\bfrom\\s+[A-Za-z0-9\\s:]+"), " ")
            .replace(NOISE_REGEX, " ")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun phoneticNormalize(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .lowercase()
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
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    fun searchSaavnMirror(query: String, limit: Int = 15): List<SongItem> {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val cleanQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val req = okhttp3.Request.Builder()
                .url("https://saavn-api-seven.vercel.app/api/search/songs?query=$cleanQuery&limit=$limit")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val json = org.json.JSONObject(body)
                val results = json.optJSONObject("data")?.optJSONArray("results")
                    ?: json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val list = mutableListOf<SongItem>()
                    for (i in 0 until results.length()) {
                        val obj = results.getJSONObject(i)
                        val id = obj.optString("id")
                        val name = cleanHtml(obj.optString("name").ifBlank { obj.optString("title") })
                        val year = obj.optString("year")
                        val lang = obj.optString("language")
                        val playCount = obj.optLong("playCount", 0L)
                        val dur = obj.optLong("duration", 0L)
                        val albumObj = obj.optJSONObject("album")
                        val albumName = cleanHtml(albumObj?.optString("name") ?: "")
                        val albumId = albumObj?.optString("id")

                        val artistsObj = obj.optJSONObject("artists")
                        val primaryArr = artistsObj?.optJSONArray("primary")
                        val artistList = mutableListOf<String>()
                        if (primaryArr != null) {
                            for (a in 0 until primaryArr.length()) {
                                val aName = primaryArr.getJSONObject(a).optString("name")
                                if (aName.isNotBlank()) artistList.add(aName)
                            }
                        }
                        val artistNames = cleanHtml(artistList.joinToString(", "))

                        val imgArr = obj.optJSONArray("image")
                        var imgUrl = ""
                        if (imgArr != null && imgArr.length() > 0) {
                            for (j in (imgArr.length() - 1) downTo 0) {
                                val imgItem = imgArr.getJSONObject(j)
                                val u = imgItem.optString("url")
                                if (u.isNotBlank()) {
                                    imgUrl = u
                                    break
                                }
                            }
                        }

                        val dlArr = obj.optJSONArray("downloadUrl")
                        var audioUrl = ""
                        if (dlArr != null && dlArr.length() > 0) {
                            for (j in (dlArr.length() - 1) downTo 0) {
                                val dlItem = dlArr.getJSONObject(j)
                                val u = dlItem.optString("url")
                                if (u.isNotBlank()) {
                                    audioUrl = u
                                    break
                                }
                            }
                        }

                        if (id.isNotBlank() && name.isNotBlank()) {
                            list.add(
                                SongItem(
                                    id = id,
                                    title = name,
                                    subtitle = artistNames,
                                    artistNames = artistNames,
                                    albumName = albumName,
                                    albumId = albumId,
                                    imageUrl = imgUrl,
                                    durationSeconds = dur,
                                    year = year,
                                    language = lang,
                                    hasLyrics = obj.optBoolean("hasLyrics", false),
                                    lyricsId = null,
                                    encryptedMediaUrl = null,
                                    decryptedMediaUrl = audioUrl.ifBlank { null },
                                    playCount = playCount
                                )
                            )
                        }
                    }
                    return list
                }
            }
        } catch (_: Exception) {}
        return emptyList()
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
