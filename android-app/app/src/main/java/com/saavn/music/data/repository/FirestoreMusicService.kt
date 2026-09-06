package com.saavn.music.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.data.trending.TopArtistData
import kotlinx.coroutines.tasks.await

data class PopularCacheResult(
    val songs: List<YouTubeSong>,
    val topArtists: List<TopArtistData>
)

class FirestoreMusicService private constructor(context: Context) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val ytRepo = YouTubeMusicRepository()

    companion object {
        @Volatile
        private var instance: FirestoreMusicService? = null

        fun getInstance(context: Context): FirestoreMusicService {
            return instance ?: synchronized(this) {
                instance ?: FirestoreMusicService(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Reads popular_cache/global from Firestore for Most Played Songs & Popular Artists
     */
    suspend fun getPopularCache(): PopularCacheResult {
        return try {
            val docSnap = db.collection("popular_cache").document("global").get().await()
            if (docSnap.exists()) {
                val songIds = (docSnap.get("topSongIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val rawArtists = docSnap.get("topArtists") as? List<*> ?: emptyList<Any>()

                val artists = rawArtists.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        val name = item["name"]?.toString()
                        val count = (item["playCount"] as? Number)?.toInt() ?: 1
                        if (!name.isNullOrBlank()) TopArtistData(name, count) else null
                    } else null
                }

                val songs = if (songIds.isNotEmpty()) {
                    val list = mutableListOf<YouTubeSong>()
                    for (id in songIds.take(20)) {
                        val ytSong = YouTubeSong(
                            videoId = id,
                            title = "Hit Track $id",
                            channelTitle = "Tamil Music",
                            thumbnailUrl = "https://img.youtube.com/vi/$id/hqdefault.jpg",
                            durationFormatted = "3:45"
                        )
                        list.add(ytSong)
                    }
                    list
                } else emptyList()

                PopularCacheResult(songs = songs, topArtists = artists)
            } else {
                PopularCacheResult(songs = emptyList(), topArtists = emptyList())
            }
        } catch (_: Exception) {
            PopularCacheResult(songs = emptyList(), topArtists = emptyList())
        }
    }

    suspend fun getTrendingSongs(limitCount: Int = 30): List<YouTubeSong> {
        val cache = getPopularCache()
        if (cache.songs.isNotEmpty()) {
            return cache.songs
        }
        return try {
            val snap = db.collection("songs")
                .limit(limitCount.toLong())
                .get()
                .await()

            if (!snap.isEmpty) {
                snap.documents.mapNotNull { doc ->
                    val vid = doc.getString("videoId") ?: doc.id
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val artist = doc.getString("artist") ?: "Tamil Music"
                    val thumb = doc.getString("artworkUrl") ?: "https://img.youtube.com/vi/$vid/hqdefault.jpg"
                    YouTubeSong(
                        videoId = vid,
                        title = title,
                        channelTitle = artist,
                        thumbnailUrl = thumb,
                        durationFormatted = doc.getString("duration") ?: "3:45",
                        durationMs = doc.getLong("durationMs") ?: 225000L
                    )
                }
            } else {
                ytRepo.getTrendingTamil()
            }
        } catch (e: Exception) {
            ytRepo.getTrendingTamil()
        }
    }

    suspend fun searchSongs(queryStr: String, limitCount: Int = 20): List<YouTubeSong> {
        val clean = queryStr.trim().lowercase()
        return try {
            val snap = db.collection("songs")
                .whereGreaterThanOrEqualTo("titleLower", clean)
                .whereLessThanOrEqualTo("titleLower", clean + "\uf8ff")
                .limit(limitCount.toLong())
                .get()
                .await()

            if (!snap.isEmpty) {
                snap.documents.mapNotNull { doc ->
                    val vid = doc.getString("videoId") ?: doc.id
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val artist = doc.getString("artist") ?: "Tamil Music"
                    val thumb = doc.getString("artworkUrl") ?: "https://img.youtube.com/vi/$vid/hqdefault.jpg"
                    YouTubeSong(
                        videoId = vid,
                        title = title,
                        channelTitle = artist,
                        thumbnailUrl = thumb,
                        durationFormatted = doc.getString("duration") ?: "3:45",
                        durationMs = doc.getLong("durationMs") ?: 225000L
                    )
                }
            } else {
                ytRepo.searchTamilSongs(queryStr, limitCount).getOrDefault(emptyList())
            }
        } catch (e: Exception) {
            ytRepo.searchTamilSongs(queryStr, limitCount).getOrDefault(emptyList())
        }
    }
}
