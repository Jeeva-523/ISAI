package com.saavn.music.data.repository

import com.saavn.music.data.api.JioSaavnApiService
import com.saavn.music.data.crypto.DesCrypto
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
            val queryWithTamil = if (!cleanQuery.contains("tamil", ignoreCase = true)) {
                "$cleanQuery Tamil"
            } else {
                cleanQuery
            }
            val response = api.searchSongs(query = queryWithTamil)
            val songs = response.results?.mapNotNull { it.toSongItem() } ?: emptyList()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrending(category: String = "Tamil", limit: Int = 60): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        try {
            val queries = if (category.equals("tamil", ignoreCase = true)) {
                listOf(
                    "Latest Tamil Movie Songs 2024",
                    "Anirudh Ravichander Tamil Hits",
                    "A R Rahman Tamil Super Hits",
                    "Yuvan Shankar Raja Tamil Hits",
                    "Harris Jayaraj Tamil Melodies",
                    "Santhosh Narayanan Tamil Hits"
                )
            } else {
                listOf("Latest $category Hit Songs 2024")
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
                title.contains("trending") || title.contains("jukebox") || title.contains("full album") || title.contains("non stop") || title.contains("compilation")
            }
            Result.success(if (filtered.isNotEmpty()) filtered else allSongs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
            decryptedMediaUrl = decUrl
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
