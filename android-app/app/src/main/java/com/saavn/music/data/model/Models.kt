package com.saavn.music.data.model

import com.google.gson.annotations.SerializedName

data class SongItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val artistNames: String = "",
    val albumName: String = "",
    val albumId: String? = null,
    val imageUrl: String = "",
    val durationSeconds: Long = 0L,
    val year: String = "",
    val language: String = "",
    val hasLyrics: Boolean = false,
    val lyricsId: String? = null,
    val encryptedMediaUrl: String? = null,
    var decryptedMediaUrl: String? = null,
    val playCount: Long = 0L
) {
    fun getStreamUrl(quality: AudioQuality): String? {
        val base = decryptedMediaUrl ?: return null
        return com.saavn.music.data.crypto.DesCrypto.getStreamUrl(base, quality.bitrate)
    }

    fun getFormattedDuration(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

enum class AudioQuality(val title: String, val bitrate: String, val tag: String) {
    VERY_HIGH("320 kbps (Very High Quality)", "320", "HD"),
    HIGH("160 kbps (Standard Quality)", "160", "HQ"),
    DATA_SAVER("96 kbps (Data Saver)", "96", "SD")
}

data class SearchResultResponse(
    @SerializedName("total") val total: Int? = null,
    @SerializedName("start") val start: Int? = null,
    @SerializedName("results") val results: List<RawSongItem>? = null
)

data class SongDetailsResponse(
    @SerializedName("songs") val songs: List<RawSongItem>? = null
)

data class RawSongItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("header_desc") val headerDesc: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("perma_url") val permaUrl: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("play_count") val playCount: String? = null,
    @SerializedName("more_info") val moreInfo: RawMoreInfo? = null
)

data class RawMoreInfo(
    @SerializedName("music") val music: String? = null,
    @SerializedName("album_id") val albumId: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("has_lyrics") val hasLyrics: String? = null,
    @SerializedName("lyrics_snippet") val lyricsSnippet: String? = null,
    @SerializedName("lyrics_id") val lyricsId: String? = null,
    @SerializedName("encrypted_media_url") val encryptedMediaUrl: String? = null,
    @SerializedName("artistMap") val artistMap: RawArtistMap? = null
)

data class RawArtistMap(
    @SerializedName("primary_artists") val primaryArtists: List<RawArtist>? = null,
    @SerializedName("artists") val artists: List<RawArtist>? = null
)

data class RawArtist(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("image") val image: String? = null
)

data class LyricsResponse(
    @SerializedName("lyrics") val lyrics: String? = null,
    @SerializedName("snippet") val snippet: String? = null,
    @SerializedName("lyrics_copyright") val lyricsCopyright: String? = null
)
