package com.saavn.music.data.api

import com.saavn.music.data.model.LyricsResponse
import com.saavn.music.data.model.RawSongItem
import com.saavn.music.data.model.SearchResultResponse
import com.saavn.music.data.model.SongDetailsResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface JioSaavnApiService {

    @GET("api.php")
    suspend fun searchSongs(
        @Query("__call") call: String = "search.getResults",
        @Query("_format") format: String = "json",
        @Query("_marker") marker: String = "0",
        @Query("api_version") apiVersion: String = "4",
        @Query("ctx") ctx: String = "web6dot0",
        @Query("q") query: String,
        @Query("p") page: Int = 1,
        @Query("n") limit: Int = 20
    ): SearchResultResponse

    @GET("api.php")
    suspend fun getSongDetails(
        @Query("__call") call: String = "song.getDetails",
        @Query("_format") format: String = "json",
        @Query("_marker") marker: String = "0",
        @Query("api_version") apiVersion: String = "4",
        @Query("ctx") ctx: String = "web6dot0",
        @Query("pids") songId: String
    ): SongDetailsResponse

    @GET("api.php")
    suspend fun getLyrics(
        @Query("__call") call: String = "lyrics.getLyrics",
        @Query("_format") format: String = "json",
        @Query("_marker") marker: String = "0",
        @Query("api_version") apiVersion: String = "4",
        @Query("ctx") ctx: String = "web6dot0",
        @Query("lyrics_id") lyricsId: String
    ): LyricsResponse

    @GET("api.php")
    suspend fun getTrending(
        @Query("__call") call: String = "content.getTrending",
        @Query("_format") format: String = "json",
        @Query("_marker") marker: String = "0",
        @Query("api_version") apiVersion: String = "4",
        @Query("ctx") ctx: String = "web6dot0"
    ): List<RawSongItem>

    companion object {
        private const val BASE_URL = "https://www.jiosaavn.com/"

        fun create(): JioSaavnApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        .header("Accept", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(JioSaavnApiService::class.java)
        }
    }
}
