package com.saavn.music.data.api

import com.saavn.music.data.model.YouTubeSearchResponse
import com.saavn.music.data.model.YouTubeVideoListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("videoCategoryId") videoCategoryId: String? = "10", // Category 10 is Music
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String,
        @Query("pageToken") pageToken: String? = null
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("id") videoIds: String,
        @Query("key") apiKey: String
    ): YouTubeVideoListResponse
}
