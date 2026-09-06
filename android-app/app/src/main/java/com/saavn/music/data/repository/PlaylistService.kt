package com.saavn.music.data.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.saavn.music.data.auth.AuthService
import com.saavn.music.data.model.YouTubeSong
import kotlinx.coroutines.tasks.await

data class AndroidPlaylist(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val songCount: Int = 0
)

class PlaylistService private constructor(context: Context) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val authService = AuthService.getInstance(context)

    companion object {
        @Volatile
        private var instance: PlaylistService? = null

        fun getInstance(context: Context): PlaylistService {
            return instance ?: synchronized(this) {
                instance ?: PlaylistService(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun getUserId(): String {
        return authService.getCurrentUserId()
    }

    suspend fun likeSong(song: YouTubeSong) {
        val uid = getUserId()
        try {
            val data = hashMapOf(
                "songId" to song.videoId,
                "title" to song.title,
                "artist" to song.channelTitle,
                "thumbnailUrl" to song.thumbnailUrl,
                "durationFormatted" to song.durationFormatted,
                "durationMs" to song.durationMs,
                "likedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("likedSongs").document(song.videoId)
                .set(data)
                .await()
        } catch (_: Exception) {}
    }

    suspend fun unlikeSong(songId: String) {
        val uid = getUserId()
        try {
            db.collection("users").document(uid).collection("likedSongs").document(songId)
                .delete()
                .await()
        } catch (_: Exception) {}
    }

    suspend fun getLikedSongs(): List<YouTubeSong> {
        val uid = getUserId()
        return try {
            val snap = db.collection("users").document(uid).collection("likedSongs")
                .orderBy("likedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                val vid = doc.getString("songId") ?: doc.id
                val title = doc.getString("title") ?: return@mapNotNull null
                val artist = doc.getString("artist") ?: "Tamil Music"
                val thumb = doc.getString("thumbnailUrl") ?: ""
                val dur = doc.getString("durationFormatted") ?: "3:45"
                val ms = doc.getLong("durationMs") ?: 225000L
                YouTubeSong(
                    videoId = vid,
                    title = title,
                    channelTitle = artist,
                    thumbnailUrl = thumb,
                    durationFormatted = dur,
                    durationMs = ms
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createPlaylist(name: String, desc: String = ""): String {
        val uid = getUserId()
        val plId = "pl_" + System.currentTimeMillis()
        try {
            val data = hashMapOf(
                "id" to plId,
                "name" to name,
                "description" to desc,
                "songCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("playlists").document(plId)
                .set(data)
                .await()
        } catch (_: Exception) {}
        return plId
    }

    suspend fun getUserPlaylists(): List<AndroidPlaylist> {
        val uid = getUserId()
        return try {
            val snap = db.collection("users").document(uid).collection("playlists").get().await()
            snap.documents.mapNotNull { doc ->
                AndroidPlaylist(
                    id = doc.id,
                    name = doc.getString("name") ?: "Playlist",
                    description = doc.getString("description") ?: "",
                    songCount = doc.getLong("songCount")?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun recordHistory(song: YouTubeSong) {
        val uid = getUserId()
        try {
            val data = hashMapOf(
                "songId" to song.videoId,
                "title" to song.title,
                "artist" to song.channelTitle,
                "thumbnailUrl" to song.thumbnailUrl,
                "durationFormatted" to song.durationFormatted,
                "durationMs" to song.durationMs,
                "playedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("listeningHistory").document()
                .set(data)
                .await()
        } catch (_: Exception) {}
    }
}
