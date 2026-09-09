package com.saavn.music.data.model

import com.google.gson.annotations.SerializedName

// --- YouTube API Search Response ---
data class YouTubeSearchResponse(
    @SerializedName("items") val items: List<YouTubeSearchItem> = emptyList(),
    @SerializedName("nextPageToken") val nextPageToken: String? = null
)

data class YouTubeSearchItem(
    @SerializedName("id") val id: YouTubeIdPayload? = null,
    @SerializedName("snippet") val snippet: YouTubeSnippet? = null
)

data class YouTubeIdPayload(
    @SerializedName("kind") val kind: String = "",
    @SerializedName("videoId") val videoId: String? = null
)

data class YouTubeSnippet(
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("channelTitle") val channelTitle: String = "",
    @SerializedName("publishedAt") val publishedAt: String = "",
    @SerializedName("thumbnails") val thumbnails: YouTubeThumbnails? = null
)

data class YouTubeThumbnails(
    @SerializedName("default") val defaultThumb: YouTubeThumbnail? = null,
    @SerializedName("medium") val mediumThumb: YouTubeThumbnail? = null,
    @SerializedName("high") val highThumb: YouTubeThumbnail? = null
)

data class YouTubeThumbnail(
    @SerializedName("url") val url: String = "",
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0
)

// --- YouTube API Video Details Response ---
data class YouTubeVideoListResponse(
    @SerializedName("items") val items: List<YouTubeVideoDetailsItem> = emptyList()
)

data class YouTubeVideoDetailsItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("snippet") val snippet: YouTubeSnippet? = null,
    @SerializedName("contentDetails") val contentDetails: YouTubeContentDetails? = null,
    @SerializedName("statistics") val statistics: YouTubeStatistics? = null
)

data class YouTubeContentDetails(
    @SerializedName("duration") val duration: String = "" // ISO 8601 like PT4M35S
)

data class YouTubeStatistics(
    @SerializedName("viewCount") val viewCount: String? = null,
    @SerializedName("likeCount") val likeCount: String? = null
)

// --- Unified App Domain Model for YouTube Songs ---
data class YouTubeSong(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationFormatted: String = "3:30",
    val durationMs: Long = 210000L,
    val viewCountFormatted: String = "",
    val audioUrl: String? = null,
    val addedAtTimestamp: Long = System.currentTimeMillis(),
    val playCount: Long = 0L
)
