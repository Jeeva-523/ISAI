package com.saavn.music.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.saavn.music.data.model.YouTubeSong
import kotlinx.coroutines.tasks.await

class FirestoreMusicService private constructor(context: Context) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val ytRepo = YouTubeMusicRepository.getInstance(context)

    companion object {
        @Volatile
        private var instance: FirestoreMusicService? = null

        fun getInstance(context: Context): FirestoreMusicService {
            return instance ?: synchronized(this) {
                instance ?: FirestoreMusicService(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun getTrendingSongs(limitCount: Int = 30): List<YouTubeSong> {
        return try {
            val snap = db.collection("songs")
                .orderBy("releaseDate", Query.Direction.DESCENDING)
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
