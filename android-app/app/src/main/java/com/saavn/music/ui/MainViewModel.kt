package com.saavn.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saavn.music.data.local.LocalMusicStorage
import com.saavn.music.data.local.UserPlaylist
import com.saavn.music.data.model.AudioQuality
import com.saavn.music.data.model.SongItem
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.data.repository.MusicRepository
import com.saavn.music.data.repository.YouTubeMusicRepository
import com.saavn.music.player.YouTubePlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.saavn.music.auth.GoogleAuthHelper
import com.saavn.music.data.model.UserProfile

enum class AppScreen {
    HOME,
    SEARCH,
    LIBRARY
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val localStorage = LocalMusicStorage(application.applicationContext)
    val googleAuthHelper = GoogleAuthHelper(application.applicationContext)
    val musicRepo = MusicRepository()
    val ytRepo = YouTubeMusicRepository()
    val ytPlayerController = YouTubePlayerController(application.applicationContext, localStorage)
    val isaiConnectManager = com.saavn.music.connect.IsaiConnectManager(application.applicationContext)

    // User Authentication Profile
    val userProfile: StateFlow<UserProfile?> = localStorage.userProfile

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private fun SongItem.toYouTubeSong(): YouTubeSong {
        return YouTubeSong(
            videoId = id,
            title = title,
            channelTitle = artistNames.ifBlank { subtitle },
            thumbnailUrl = imageUrl,
            durationFormatted = getFormattedDuration(),
            durationMs = durationSeconds * 1000L,
            viewCountFormatted = if (year.isNotBlank()) "Year $year" else "",
            audioUrl = getStreamUrl(AudioQuality.VERY_HIGH)
        )
    }

    // Navigation Screen
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active Tamil Music Category
    private val _selectedCategory = MutableStateFlow("Trending")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Home Categorized Songs
    private val _trendingSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val trendingSongs: StateFlow<List<YouTubeSong>> = _trendingSongs.asStateFlow()

    private val _categorySongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val categorySongs: StateFlow<List<YouTubeSong>> = _categorySongs.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val searchResults: StateFlow<List<YouTubeSong>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Modals
    private val _showFullPlayer = MutableStateFlow(false)
    val showFullPlayer: StateFlow<Boolean> = _showFullPlayer.asStateFlow()

    private val _showLyrics = MutableStateFlow(false)
    val showLyrics: StateFlow<Boolean> = _showLyrics.asStateFlow()

    private val _showAddToPlaylistDialog = MutableStateFlow<YouTubeSong?>(null)
    val showAddToPlaylistDialog: StateFlow<YouTubeSong?> = _showAddToPlaylistDialog.asStateFlow()

    // Playback Queue & Suggestions
    val playbackQueue: StateFlow<List<YouTubeSong>> = ytPlayerController.playbackQueue
    val currentQueueIndex: StateFlow<Int> = ytPlayerController.currentQueueIndex

    private val _suggestions = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val suggestions: StateFlow<List<YouTubeSong>> = _suggestions.asStateFlow()

    private val _personalizedRecommendations = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val personalizedRecommendations: StateFlow<List<YouTubeSong>> = _personalizedRecommendations.asStateFlow()

    private val _recommendedReason = MutableStateFlow("✨ Based on your listening")
    val recommendedReason: StateFlow<String> = _recommendedReason.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    // Storage references
    val favorites: StateFlow<List<YouTubeSong>> = localStorage.favorites
    val playlists: StateFlow<List<UserPlaylist>> = localStorage.playlists
    val recentlyPlayed: StateFlow<List<YouTubeSong>> = localStorage.recentlyPlayed

    private var searchJob: Job? = null

    init {
        loadHomeData()
        loadPersonalizedRecommendations()
        ytPlayerController.onQueueExhausted = {
            playNextAutoSuggestion()
        }
        viewModelScope.launch {
            localStorage.recentlyPlayed.collect {
                loadPersonalizedRecommendations()
            }
        }

        // Initialize ISAI Connect with user profile
        viewModelScope.launch {
            userProfile.collect { profile ->
                val email = profile?.email?.ifBlank { null } ?: "user_jeeva_default"
                isaiConnectManager.initialize(email)
            }
        }

        // Sync local playback state to ISAI Connect when this device is active player
        viewModelScope.launch {
            ytPlayerController.currentSong.collect { song ->
                if (song != null && isaiConnectManager.isMyDeviceActive()) {
                    isaiConnectManager.updatePlaybackState(
                        song = song,
                        isPlaying = ytPlayerController.isPlaying.value,
                        positionMs = (ytPlayerController.currentPositionSec.value * 1000).toLong(),
                        durationMs = (ytPlayerController.durationSec.value * 1000).toLong()
                    )
                }
            }
        }

        viewModelScope.launch {
            ytPlayerController.isPlaying.collect { playing ->
                if (isaiConnectManager.isMyDeviceActive()) {
                    isaiConnectManager.updatePlaybackState(
                        isPlaying = playing,
                        positionMs = (ytPlayerController.currentPositionSec.value * 1000).toLong()
                    )
                }
            }
        }

        // Remote playback listener: if another device becomes active, pause local playback
        viewModelScope.launch {
            isaiConnectManager.playbackState.collect { syncState ->
                if (syncState != null && !isaiConnectManager.isMyDeviceActive()) {
                    if (ytPlayerController.isPlaying.value) {
                        ytPlayerController.pause()
                    }
                }
            }
        }
    }

    fun loadPersonalizedRecommendations() {
        viewModelScope.launch {
            try {
                val recentList = localStorage.recentlyPlayed.value
                val favList = localStorage.favorites.value
                val combined = (recentList + favList).distinctBy { it.videoId }

                if (combined.isNotEmpty()) {
                    val artistMap = mutableMapOf<String, Int>()
                    combined.forEach { song ->
                        val artist = song.channelTitle
                            .replace(" - Topic", "")
                            .replace(" Official", "")
                            .trim()
                        if (artist.isNotBlank() && artist != "Tamil Artist" && artist != "Tamil Music") {
                            artistMap[artist] = (artistMap[artist] ?: 0) + 1
                        }
                    }

                    val topArtist = artistMap.maxByOrNull { it.value }?.key
                    if (!topArtist.isNullOrBlank()) {
                        _recommendedReason.value = "Because you listen to $topArtist"
                        val saavnResult = musicRepo.search("$topArtist Tamil songs")
                        val recSongs = saavnResult.getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                        if (recSongs.isNotEmpty()) {
                            val filtered = recSongs.filterNot { s -> combined.any { it.videoId == s.videoId } }
                            _personalizedRecommendations.value = if (filtered.isNotEmpty()) filtered else recSongs
                            return@launch
                        }
                    }
                }

                // Default recommendation mix for first-time users
                _recommendedReason.value = "✨ Top Picks For You"
                val defaultRec = musicRepo.getTrending("Tamil").getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                _personalizedRecommendations.value = defaultRec.take(12)
            } catch (_: Exception) {}
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoadingHome.value = true
            try {
                // 1. Try fetching direct audio songs via MusicRepository (JioSaavn / NepoTune)
                val saavnResult = musicRepo.getTrending("Tamil")
                val saavnSongs = saavnResult.getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                val cleanSaavn = saavnSongs.filterNot { s ->
                    val t = s.title.lowercase()
                    t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                }
                if (cleanSaavn.isNotEmpty()) {
                    _trendingSongs.value = cleanSaavn
                    _categorySongs.value = cleanSaavn
                } else {
                    val trending = ytRepo.getTrendingTamil().filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    _trendingSongs.value = trending
                    _categorySongs.value = trending
                }
            } catch (e: Exception) {
                try {
                    val trending = ytRepo.getTrendingTamil().filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    _trendingSongs.value = trending
                    _categorySongs.value = trending
                } catch (_: Exception) {}
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            _isLoadingHome.value = true
            try {
                val query = when (category) {
                    "Tamil Songs" -> "Tamil all time hit songs"
                    "Melody" -> "Tamil melody hits"
                    "Love Songs" -> "Tamil love romantic songs"
                    "Folk" -> "Tamil folk village hits"
                    "Devotional" -> "Tamil devotional songs"
                    "Gaana" -> "Tamil gaana hits"
                    "Classical" -> "Tamil classical hits"
                    "New Releases" -> "Latest Tamil Movie Songs 2025 2026"
                    else -> "Latest Tamil Movie Songs 2025 2026"
                }
                val saavnResult = musicRepo.search(query)
                val saavnSongs = saavnResult.getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                val cleanSaavn = saavnSongs.filterNot { s ->
                    val t = s.title.lowercase()
                    t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                }
                if (cleanSaavn.isNotEmpty()) {
                    _categorySongs.value = cleanSaavn
                } else {
                    val fallback = ytRepo.searchTamilSongs(query).getOrDefault(emptyList()).filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    _categorySongs.value = fallback
                }
            } catch (e: Exception) {
                // Handled
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            _searchError.value = null
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            delay(350)
            try {
                // 1. Direct Audio Search (JioSaavn / NepoTune)
                val saavnResult = musicRepo.search(newQuery)
                val saavnSongs = saavnResult.getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()

                if (saavnSongs.isNotEmpty()) {
                    android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Direct Audio search success: ${saavnSongs.size} songs found")
                    _searchResults.value = saavnSongs
                    _searchError.value = null
                } else {
                    // Fallback to YouTube
                    val result = ytRepo.searchTamilSongs(newQuery)
                    if (result.isSuccess) {
                        _searchResults.value = result.getOrDefault(emptyList())
                        _searchError.value = null
                    } else {
                        _searchResults.value = emptyList()
                        _searchError.value = "No songs found"
                    }
                }
            } catch (e: Exception) {
                try {
                    val result = ytRepo.searchTamilSongs(newQuery)
                    _searchResults.value = result.getOrDefault(emptyList())
                } catch (_: Exception) {
                    _searchResults.value = emptyList()
                    _searchError.value = e.localizedMessage ?: "An error occurred while searching"
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun retrySearch() {
        val q = _searchQuery.value
        if (q.isNotBlank()) {
            onSearchQueryChanged(q)
        }
    }

    fun scanDeviceMusic(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val scanner = com.saavn.music.data.local.LocalAudioScanner(context)
                val localSongs = scanner.scanDeviceAudioFiles()
                localStorage.setLocalDeviceSongs(localSongs)
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYER", "Failed to scan device music: ${e.message}")
            }
        }
    }

    fun playSong(song: YouTubeSong, queue: List<YouTubeSong>? = null) {
        android.util.Log.i("ISAI_PLAYER", "========================================")
        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] playSong triggered!")
        android.util.Log.i("ISAI_PLAYER", "Song Title: ${song.title}")
        android.util.Log.i("ISAI_PLAYER", "Has Direct Audio: ${!song.audioUrl.isNullOrBlank()}")
        android.util.Log.i("ISAI_PLAYER", "========================================")

        _showFullPlayer.value = true

        // Generate smart YouTube Radio auto-recommendation queue if single song passed
        val effectiveQueue = if (queue.isNullOrEmpty() || queue.size <= 1) {
            val suggestionsList = _suggestions.value.filter { it.videoId != song.videoId }
            val trendingList = _trendingSongs.value.filter { it.videoId != song.videoId }
            val combined = (listOf(song) + suggestionsList + trendingList).distinctBy { it.videoId }
            combined.take(20)
        } else {
            queue
        }

        loadSuggestionsForSong(song)

        if (!song.audioUrl.isNullOrBlank()) {
            ytPlayerController.playSong(song, effectiveQueue)
        } else {
            // Asynchronously resolve direct 320kbps audio stream from MusicRepository
            viewModelScope.launch {
                try {
                    val searchResult = musicRepo.search(song.title).getOrNull()
                    val match = searchResult?.firstOrNull()
                    val directUrl = match?.getStreamUrl(AudioQuality.VERY_HIGH)
                    if (!directUrl.isNullOrBlank()) {
                        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Resolved direct 320kbps audio: $directUrl")
                        val updatedSong = song.copy(audioUrl = directUrl)
                        val updatedQueue = effectiveQueue.map { if (it.videoId == song.videoId) updatedSong else it }
                        ytPlayerController.playSong(updatedSong, updatedQueue)
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ISAI_PLAYER", "[MainViewModel] Could not resolve direct stream: ${e.message}")
                }
                ytPlayerController.playSong(song, effectiveQueue)
            }
        }
    }

    fun addToQueue(song: YouTubeSong) {
        ytPlayerController.addToQueue(song)
    }

    fun playNextInQueue(song: YouTubeSong) {
        ytPlayerController.playNextInQueue(song)
    }

    fun removeFromQueue(index: Int) {
        ytPlayerController.removeFromQueue(index)
    }

    fun clearQueue() {
        ytPlayerController.clearQueue()
    }

    fun loadSuggestionsForSong(song: YouTubeSong) {
        viewModelScope.launch {
            _isLoadingSuggestions.value = true
            try {
                // Generate a smart query based on artist or title
                val cleanArtist = song.channelTitle
                    .replace("Topic", "", ignoreCase = true)
                    .replace("VEVO", "", ignoreCase = true)
                    .replace("Official", "", ignoreCase = true)
                    .replace("Channel", "", ignoreCase = true)
                    .trim()

                val query = if (cleanArtist.isNotBlank() && cleanArtist.length > 2) {
                    "$cleanArtist Tamil songs"
                } else {
                    "${song.title} Tamil"
                }

                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Loading suggestions for query: '$query'")
                val directResult = musicRepo.search(query).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                val filteredDirect = directResult.filter { it.videoId != song.videoId }

                if (filteredDirect.isNotEmpty()) {
                    _suggestions.value = filteredDirect
                } else {
                    val ytResult = ytRepo.searchTamilSongs(query).getOrDefault(emptyList())
                    val filteredYt = ytResult.filter { it.videoId != song.videoId }
                    _suggestions.value = if (filteredYt.isNotEmpty()) filteredYt else _trendingSongs.value.filter { it.videoId != song.videoId }
                }
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYER", "[MainViewModel] Failed to load suggestions: ${e.message}")
                _suggestions.value = _trendingSongs.value.filter { it.videoId != song.videoId }
            } finally {
                _isLoadingSuggestions.value = false
            }
        }
    }

    fun playNextAutoSuggestion() {
        viewModelScope.launch {
            android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Auto-play triggered by onQueueExhausted!")
            val currentSuggestions = _suggestions.value
            val currentId = ytPlayerController.currentSong.value?.videoId
            val candidate = currentSuggestions.firstOrNull { it.videoId != currentId }
                ?: _trendingSongs.value.firstOrNull { it.videoId != currentId }

            if (candidate != null) {
                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Auto-advancing to: '${candidate.title}'")
                // Add to playback queue so it's reflected in queue UI
                ytPlayerController.addToQueue(candidate)
                playSong(candidate, ytPlayerController.playbackQueue.value)
            } else {
                android.util.Log.w("ISAI_PLAYER", "[MainViewModel] No candidate song found for auto-advance.")
            }
        }
    }

    fun togglePlayPause() {
        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] togglePlayPause called!")
        ytPlayerController.togglePlayPause()
    }

    fun playNext() {
        ytPlayerController.playNext()
    }

    fun playPrevious() {
        ytPlayerController.playPrevious()
    }

    fun seekTo(seconds: Float) {
        ytPlayerController.seekTo(seconds)
    }

    fun setVolume(volumePercent: Int) {
        ytPlayerController.setVolume(volumePercent)
    }

    fun isFavorite(videoId: String): Boolean {
        return localStorage.isFavorite(videoId)
    }

    fun toggleFavorite(song: YouTubeSong) {
        localStorage.toggleFavorite(song)
    }

    fun createPlaylist(name: String) {
        localStorage.createPlaylist(name)
    }

    fun addSongToPlaylist(playlistId: String, song: YouTubeSong) {
        localStorage.addSongToPlaylist(playlistId, song)
    }

    fun removeSongFromPlaylist(playlistId: String, videoId: String) {
        localStorage.removeSongFromPlaylist(playlistId, videoId)
    }

    fun deletePlaylist(playlistId: String) {
        localStorage.deletePlaylist(playlistId)
    }

    fun openAddToPlaylistDialog(song: YouTubeSong) {
        _showAddToPlaylistDialog.value = song
    }

    fun closeAddToPlaylistDialog() {
        _showAddToPlaylistDialog.value = null
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openFullPlayer() {
        _showFullPlayer.value = true
    }

    fun closeFullPlayer() {
        _showFullPlayer.value = false
    }

    fun toggleShuffle() {
        ytPlayerController.toggleShuffle()
    }

    fun toggleRepeat() {
        ytPlayerController.toggleRepeat()
    }

    fun openLoginDialog() {
        _showLoginDialog.value = true
    }

    fun closeLoginDialog() {
        _showLoginDialog.value = false
    }

    fun saveUserProfile(profile: UserProfile) {
        localStorage.saveUserProfile(profile)
        closeLoginDialog()
    }

    fun updateUsername(newUsername: String) {
        val current = userProfile.value ?: UserProfile(
            id = "user_" + System.currentTimeMillis(),
            displayName = newUsername,
            email = "user@isaimusic.com"
        )
        val updated = current.copy(displayName = newUsername)
        localStorage.saveUserProfile(updated)
    }

    fun quickSignInGoogleAccount(displayName: String = "JEEVA ⚡", email: String = "jeeva.google@gmail.com") {
        val profile = UserProfile(
            id = "google_" + System.currentTimeMillis(),
            displayName = displayName,
            email = email,
            photoUrl = null
        )
        localStorage.saveUserProfile(profile)
        closeLoginDialog()
    }

    fun logoutUser() {
        googleAuthHelper.signOut {
            localStorage.clearUserProfile()
            closeLoginDialog()
        }
    }

    fun openLyrics() {
        _showLyrics.value = true
    }

    fun closeLyrics() {
        _showLyrics.value = false
    }

    override fun onCleared() {
        super.onCleared()
        ytPlayerController.detachPlayer()
    }
}
