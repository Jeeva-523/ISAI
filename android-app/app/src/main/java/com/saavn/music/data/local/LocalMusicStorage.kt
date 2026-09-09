package com.saavn.music.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saavn.music.data.model.YouTubeSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

import com.saavn.music.data.model.UserProfile

data class UserPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val songs: List<YouTubeSong> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

class LocalMusicStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("isai_local_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _favorites = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val favorites: StateFlow<List<YouTubeSong>> = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val recentlyPlayed: StateFlow<List<YouTubeSong>> = _recentlyPlayed.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _localDeviceSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val localDeviceSongs: StateFlow<List<YouTubeSong>> = _localDeviceSongs.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        // 1. Favorites
        val favJson = prefs.getString(KEY_FAVORITES, null)
        if (!favJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<YouTubeSong>>() {}.type
                _favorites.value = gson.fromJson(favJson, type) ?: emptyList()
            } catch (e: Exception) {
                _favorites.value = emptyList()
            }
        }

        // 2. Playlists
        val playlistJson = prefs.getString(KEY_PLAYLISTS, null)
        if (!playlistJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<UserPlaylist>>() {}.type
                _playlists.value = gson.fromJson(playlistJson, type) ?: emptyList()
            } catch (e: Exception) {
                _playlists.value = emptyList()
            }
        }

        // 3. Recently Played
        val recentJson = prefs.getString(KEY_RECENTLY_PLAYED, null)
        if (!recentJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<YouTubeSong>>() {}.type
                _recentlyPlayed.value = gson.fromJson(recentJson, type) ?: emptyList()
            } catch (e: Exception) {
                _recentlyPlayed.value = emptyList()
            }
        }

        // 4. User Profile
        val userJson = prefs.getString(KEY_USER_PROFILE, null)
        if (!userJson.isNullOrBlank()) {
            try {
                _userProfile.value = gson.fromJson(userJson, UserProfile::class.java)
            } catch (e: Exception) {
                _userProfile.value = null
            }
        }
    }

    fun setLocalDeviceSongs(songs: List<YouTubeSong>) {
        _localDeviceSongs.value = songs
    }

    // --- User Profile ---
    fun saveUserProfile(profile: UserProfile) {
        _userProfile.value = profile
        prefs.edit().putString(KEY_USER_PROFILE, gson.toJson(profile)).apply()
    }

    fun clearUserProfile() {
        _userProfile.value = null
        prefs.edit().remove(KEY_USER_PROFILE).apply()
    }

    // --- Favorites ---
    fun isFavorite(videoId: String): Boolean {
        return _favorites.value.any { it.videoId == videoId }
    }

    fun toggleFavorite(song: YouTubeSong) {
        val current = _favorites.value.toMutableList()
        val exists = current.indexOfFirst { it.videoId == song.videoId }
        if (exists >= 0) {
            current.removeAt(exists)
        } else {
            current.add(0, song)
        }
        _favorites.value = current
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(current)).apply()
    }

    fun setFavorites(songs: List<YouTubeSong>) {
        _favorites.value = songs
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(songs)).apply()
    }

    // --- Recently Played (Strictly latest 20) ---
    fun addRecentlyPlayed(song: YouTubeSong) {
        val current = _recentlyPlayed.value.toMutableList()
        current.removeAll { it.videoId == song.videoId }
        current.add(0, song)
        val trimmed = if (current.size > 20) current.take(20) else current
        _recentlyPlayed.value = trimmed
        prefs.edit().putString(KEY_RECENTLY_PLAYED, gson.toJson(trimmed)).apply()
    }

    fun setRecentlyPlayed(songs: List<YouTubeSong>) {
        val trimmed = if (songs.size > 20) songs.take(20) else songs
        _recentlyPlayed.value = trimmed
        prefs.edit().putString(KEY_RECENTLY_PLAYED, gson.toJson(trimmed)).apply()
    }

    // --- Playlists ---
    fun createPlaylist(name: String): UserPlaylist {
        val newPlaylist = UserPlaylist(name = name)
        val current = _playlists.value.toMutableList()
        current.add(0, newPlaylist)
        _playlists.value = current
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(current)).apply()
        return newPlaylist
    }

    fun addSongToPlaylist(playlistId: String, song: YouTubeSong) {
        val current = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.songs.none { it.videoId == song.videoId }) {
                    pl.copy(songs = pl.songs + song)
                } else pl
            } else pl
        }
        _playlists.value = current
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(current)).apply()
    }

    fun removeSongFromPlaylist(playlistId: String, videoId: String) {
        val current = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(songs = pl.songs.filterNot { it.videoId == videoId })
            } else pl
        }
        _playlists.value = current
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(current)).apply()
    }

    fun deletePlaylist(playlistId: String) {
        val current = _playlists.value.filterNot { it.id == playlistId }
        _playlists.value = current
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(current)).apply()
    }

    companion object {
        private const val KEY_FAVORITES = "isai_fav_songs"
        private const val KEY_PLAYLISTS = "isai_user_playlists"
        private const val KEY_RECENTLY_PLAYED = "isai_recent_20_songs"
        private const val KEY_USER_PROFILE = "isai_user_profile"
    }
}
