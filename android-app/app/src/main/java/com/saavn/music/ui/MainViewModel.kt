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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers

import com.saavn.music.auth.GoogleAuthHelper
import com.saavn.music.data.analytics.AnalyticsService
import com.saavn.music.data.auth.AuthService
import com.saavn.music.data.model.UserProfile
import com.saavn.music.data.model.AppUpdateModel
import com.saavn.music.data.repository.AppUpdateService
import com.saavn.music.data.repository.FirestoreMusicService
import com.saavn.music.data.repository.PlaylistService
import com.saavn.music.data.search.SearchService
import com.saavn.music.data.search.SmartSearchEngine
import com.saavn.music.data.search.IsaiKeywordDictionary
import com.saavn.music.data.trending.TrendingService

enum class AppScreen {
    HOME,
    SEARCH,
    LIBRARY,
    PROFILE
}

data class SpotifyDailyMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val songs: List<YouTubeSong>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val localStorage = LocalMusicStorage(application.applicationContext)
    val googleAuthHelper = GoogleAuthHelper(application.applicationContext)
    val musicRepo = MusicRepository()
    val ytRepo = YouTubeMusicRepository()
    val ytPlayerController = YouTubePlayerController.initialize(application.applicationContext, localStorage)
    val isaiConnectManager = com.saavn.music.connect.IsaiConnectManager(application.applicationContext)

    // Firebase & Discovery Services
    val analyticsService = AnalyticsService.getInstance(application.applicationContext)
    val trendingService = TrendingService.getInstance(application.applicationContext)
    val searchService = SearchService.getInstance(application.applicationContext)
    val authService = AuthService.getInstance(application.applicationContext)
    val playlistService = PlaylistService.getInstance(application.applicationContext)
    val firestoreMusicService = FirestoreMusicService.getInstance(application.applicationContext)
    val appUpdateService = AppUpdateService.getInstance(application.applicationContext)

    // User Authentication Profile
    val userProfile: StateFlow<UserProfile?> = localStorage.userProfile

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    // In-App Auto Update State
    private val _appUpdateInfo = MutableStateFlow<AppUpdateModel?>(null)
    val appUpdateInfo: StateFlow<AppUpdateModel?> = _appUpdateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    // Multi-Device Playback Mode: Separate (independent on 2+ devices) vs Sync (Spotify Connect)
    val isMultiDevicePlaybackSeparate: StateFlow<Boolean> = localStorage.isMultiDevicePlaybackSeparate

    fun setMultiDevicePlaybackSeparate(enabled: Boolean) {
        localStorage.setMultiDevicePlaybackSeparate(enabled)
    }

    // Server-Driven Dynamic UI Config (Cloud-driven real-time design updates)
    val dynamicUiService = com.saavn.music.data.repository.DynamicUiService.getInstance(application)
    val dynamicUiConfig: StateFlow<com.saavn.music.data.model.DynamicUiConfig> = dynamicUiService.uiConfig

    init {
        // Automatically check for newer app updates on startup
        checkForAppUpdates()

        viewModelScope.launch {
            val existing = localStorage.userProfile.value
            if (authService.isUserLoggedIn()) {
                val isVerified = authService.checkEmailVerified()
                if (isVerified) {
                    val user = authService.getCurrentUser()
                    if (user != null) {
                        val langs = if (!existing?.preferredLanguages.isNullOrEmpty()) {
                            existing!!.preferredLanguages
                        } else {
                            listOf("tamil")
                        }
                        val profile = UserProfile(
                            id = user.uid,
                            displayName = user.displayName ?: existing?.displayName ?: (user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "ISAI Listener"),
                            email = user.email ?: existing?.email ?: "",
                            photoUrl = user.photoUrl?.toString() ?: existing?.photoUrl,
                            isLoggedIn = true,
                            preferredLanguages = langs
                        )
                        localStorage.saveUserProfile(profile)
                        _preferredLanguages.value = langs
                    }
                } else {
                    // Unverified user on startup -> Block home flow and show Email Verification dialog
                    _showLoginDialog.value = true
                }
            } else if (existing?.isLoggedIn == true) {
                // Preserved persistent session
                if (!existing.preferredLanguages.isNullOrEmpty()) {
                    _preferredLanguages.value = existing.preferredLanguages
                }
            }
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            val update = appUpdateService.checkForUpdate()
            if (update != null) {
                _appUpdateInfo.value = update
            }
        }
        appUpdateService.listenForRealtimeUpdates { update ->
            _appUpdateInfo.value = update
        }
    }

    fun dismissUpdateDialog() {
        _appUpdateInfo.value = null
    }

    fun checkForAppUpdatesManual(onResult: (hasUpdate: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            try {
                val update = appUpdateService.checkForUpdate()
                if (update != null) {
                    _appUpdateInfo.value = update
                    onResult(true, "New update available: v${update.latestVersionName}")
                } else {
                    onResult(false, "You are using the latest version of ISAI (v${com.saavn.music.BuildConfig.VERSION_NAME})")
                }
            } catch (e: Exception) {
                onResult(false, "Could not check for updates: ${e.message ?: "Network error"}")
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    fun launchAppUpdate(context: android.content.Context) {
        val update = _appUpdateInfo.value
        if (update != null) {
            appUpdateService.openUpdateUrl(context, update.downloadUrl)
        }
    }

    private fun SongItem.toYouTubeSong(): YouTubeSong {
        return YouTubeSong(
            videoId = id,
            title = title,
            channelTitle = artistNames.ifBlank { subtitle },
            thumbnailUrl = imageUrl,
            durationFormatted = getFormattedDuration(),
            durationMs = durationSeconds * 1000L,
            viewCountFormatted = if (playCount > 0) "${java.text.NumberFormat.getInstance().format(playCount)} plays" else if (year.isNotBlank()) "Year $year" else "",
            audioUrl = getStreamUrl(AudioQuality.VERY_HIGH),
            playCount = playCount,
            language = this.language.lowercase().trim()
        )
    }

    // Navigation Screen
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active Tamil Music Category
    private val _selectedCategory = MutableStateFlow("Most Played")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Home Categorized Songs
    private val _trendingSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val trendingSongs: StateFlow<List<YouTubeSong>> = _trendingSongs.asStateFlow()

    private val _popularArtists = MutableStateFlow<List<com.saavn.music.data.trending.TopArtistData>>(emptyList())
    val popularArtists: StateFlow<List<com.saavn.music.data.trending.TopArtistData>> = _popularArtists.asStateFlow()

    private val _categorySongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val categorySongs: StateFlow<List<YouTubeSong>> = _categorySongs.asStateFlow()

    private val _latestReleases = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val latestReleases: StateFlow<List<YouTubeSong>> = _latestReleases.asStateFlow()

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

    private val _searchLanguageFilter = MutableStateFlow("All")
    val searchLanguageFilter: StateFlow<String> = _searchLanguageFilter.asStateFlow()

    fun setSearchLanguageFilter(lang: String) {
        _searchLanguageFilter.value = lang
        val currentQ = _searchQuery.value
        if (currentQ.isNotBlank()) {
            onSearchQueryChanged(currentQ)
        }
    }

    // Preferred Music Languages
    private val _preferredLanguages = MutableStateFlow<List<String>>(
        localStorage.userProfile.value?.preferredLanguages ?: emptyList()
    )
    val preferredLanguages: StateFlow<List<String>> = _preferredLanguages.asStateFlow()

    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

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

    // Spotify-Style Daily Mixes
    private val _spotifyDailyMixes = MutableStateFlow<List<SpotifyDailyMix>>(emptyList())
    val spotifyDailyMixes: StateFlow<List<SpotifyDailyMix>> = _spotifyDailyMixes.asStateFlow()

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
        ytPlayerController.onTrackChangeRequested = { nextSong, queue ->
            playSong(nextSong, queue, openFullPlayer = false)
        }
        viewModelScope.launch {
            localStorage.recentlyPlayed.collect { list ->
                if (list.isNotEmpty()) {
                    isaiConnectManager.syncRecentlyPlayed(list)
                }
                loadPersonalizedRecommendations()
            }
        }

        viewModelScope.launch {
            isaiConnectManager.syncedRecentlyPlayed.collect { remoteList ->
                if (remoteList.isNotEmpty()) {
                    val local = localStorage.recentlyPlayed.value
                    val remoteIds = remoteList.map { it.videoId }.toSet()
                    val localIds = local.map { it.videoId }.toSet()
                    if (remoteIds != localIds) {
                        val merged = (remoteList + local).distinctBy { it.videoId }.take(20)
                        localStorage.setRecentlyPlayed(merged)
                    }
                    loadPersonalizedRecommendations()
                }
            }
        }

        viewModelScope.launch {
            isaiConnectManager.syncedPreferences.collect { remoteLangs ->
                if (remoteLangs.isNotEmpty() && remoteLangs != _preferredLanguages.value) {
                    _preferredLanguages.value = remoteLangs
                    val currentProfile = localStorage.userProfile.value
                    if (currentProfile != null) {
                        localStorage.saveUserProfile(currentProfile.copy(preferredLanguages = remoteLangs))
                    }
                    loadHomeData()
                }
            }
        }

        viewModelScope.launch {
            isaiConnectManager.syncedHomeSongs.collect { remoteHome ->
                if (remoteHome.isNotEmpty() && _trendingSongs.value.isEmpty()) {
                    _trendingSongs.value = remoteHome
                    _categorySongs.value = remoteHome
                }
            }
        }

        viewModelScope.launch {
            isaiConnectManager.syncedFavorites.collect { remoteFavs ->
                if (remoteFavs.isNotEmpty()) {
                    val local = localStorage.favorites.value
                    val remoteIds = remoteFavs.map { it.videoId }.toSet()
                    val localIds = local.map { it.videoId }.toSet()
                    if (remoteIds != localIds) {
                        val merged = (remoteFavs + local).distinctBy { it.videoId }
                        localStorage.setFavorites(merged)
                    }
                }
            }
        }

        // Initialize ISAI Connect with user profile (with user_guest fallback matching web client)
        viewModelScope.launch {
            userProfile.collect { profile ->
                val email = profile?.email?.takeIf { it.isNotBlank() } 
                    ?: authService.getCurrentUser()?.email?.takeIf { it.isNotBlank() } 
                    ?: "user_guest"
                val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                    ?: authService.getCurrentUser()?.displayName?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                isaiConnectManager.initialize(email, displayName)
                if (localStorage.recentlyPlayed.value.isNotEmpty()) {
                    isaiConnectManager.syncRecentlyPlayed(localStorage.recentlyPlayed.value)
                }
                if (localStorage.favorites.value.isNotEmpty()) {
                    isaiConnectManager.syncFavorites(localStorage.favorites.value)
                }
                if (_preferredLanguages.value.isNotEmpty()) {
                    isaiConnectManager.syncPreferences(_preferredLanguages.value)
                }
                if (_trendingSongs.value.isNotEmpty()) {
                    isaiConnectManager.syncHomeSongs(_trendingSongs.value)
                }
            }
        }

        // Sync local playback state to ISAI Connect when this device is active player and not in separate mode
        viewModelScope.launch {
            ytPlayerController.currentSong.collect { song ->
                if (song != null && !localStorage.isMultiDevicePlaybackSeparate.value && isaiConnectManager.isMyDeviceActive()) {
                    isaiConnectManager.updatePlaybackState(
                        song = song,
                        isPlaying = ytPlayerController.isPlaying.value,
                        positionMs = (ytPlayerController.currentPositionSec.value * 1000).toLong(),
                        durationMs = (ytPlayerController.durationSec.value * 1000).toLong(),
                        queue = ytPlayerController.playbackQueue.value,
                        queueIndex = ytPlayerController.currentQueueIndex.value
                    )
                }
            }
        }

        viewModelScope.launch {
            ytPlayerController.currentQueueIndex.collect { idx ->
                if (!localStorage.isMultiDevicePlaybackSeparate.value && isaiConnectManager.isMyDeviceActive()) {
                    isaiConnectManager.updatePlaybackState(queueIndex = idx)
                }
            }
        }

        viewModelScope.launch {
            ytPlayerController.isPlaying.collect { playing ->
                if (!localStorage.isMultiDevicePlaybackSeparate.value && isaiConnectManager.isMyDeviceActive()) {
                    isaiConnectManager.updatePlaybackState(
                        isPlaying = playing,
                        positionMs = (ytPlayerController.currentPositionSec.value * 1000).toLong()
                    )
                }
            }
        }

        // Periodic position sync to ISAI Connect (every 1.5s while playing on this phone)
        viewModelScope.launch {
            while (isActive) {
                delay(1500)
                if (!localStorage.isMultiDevicePlaybackSeparate.value && isaiConnectManager.isMyDeviceActive() && ytPlayerController.isPlaying.value) {
                    val posMs = (ytPlayerController.currentPositionSec.value * 1000).toLong()
                    val durMs = (ytPlayerController.durationSec.value * 1000).toLong()
                    isaiConnectManager.updatePlaybackState(
                        positionMs = posMs,
                        durationMs = durMs,
                        isPlaying = true
                    )
                }
            }
        }

        // Remote playback listener: handles device handoff and auto-pause
        viewModelScope.launch {
            isaiConnectManager.playbackState.collect { syncState ->
                if (syncState != null) {
                    if (localStorage.isMultiDevicePlaybackSeparate.value) {
                        // In Separate Multi-Device Mode:
                        // Both devices play independently without pausing or hijacking each other!
                        return@collect
                    }
                    val isAnotherDeviceActivelyPlaying = !isaiConnectManager.isMyDeviceActive() &&
                        syncState.updatedByDeviceId != isaiConnectManager.deviceId &&
                        syncState.currentDeviceId != isaiConnectManager.deviceId &&
                        syncState.isPlaying
                    if (isAnotherDeviceActivelyPlaying) {
                        if (ytPlayerController.isPlaying.value) {
                            ytPlayerController.pause()
                        }
                    }

                    val isRemoteActive = !isaiConnectManager.isMyDeviceActive() &&
                        syncState.currentDeviceId.isNotBlank() &&
                        !syncState.currentTitle.isNullOrBlank() &&
                        (syncState.isPlaying || Math.abs(System.currentTimeMillis() - syncState.updatedAt) < 15 * 60_000L)

                    if (isRemoteActive) {
                        val remoteSong = YouTubeSong(
                            videoId = syncState.currentSongId ?: "",
                            title = syncState.currentTitle ?: "Remote Track",
                            channelTitle = "${syncState.currentArtist ?: "ISAI"} • Web 💻",
                            thumbnailUrl = syncState.currentArtwork ?: ""
                        )
                        com.saavn.music.service.MusicPlaybackService.startOrUpdate(
                            context = application.applicationContext,
                            song = remoteSong,
                            isPlaying = syncState.isPlaying,
                            durationSec = (syncState.durationMs / 1000f).coerceAtLeast(0f),
                            positionSec = (syncState.positionMs / 1000f).coerceAtLeast(0f)
                        )
                    }

                    if ((syncState.currentDeviceId == isaiConnectManager.deviceId || isaiConnectManager.isMyDeviceActive() || ytPlayerController.isPlaying.value) && syncState.updatedByDeviceId != isaiConnectManager.deviceId) {
                        if (!syncState.isPlaying && ytPlayerController.isPlaying.value) {
                            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Pausing playback from remote syncState updated by ${syncState.updatedByDeviceId}")
                            ytPlayerController.pause()
                        } else if (syncState.isPlaying && !ytPlayerController.isPlaying.value && syncState.currentDeviceId == isaiConnectManager.deviceId) {
                            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Resuming playback from remote syncState updated by ${syncState.updatedByDeviceId}")
                            ytPlayerController.play()
                        }
                    } else if (isaiConnectManager.isMyDeviceActive() && syncState.updatedByDeviceId != isaiConnectManager.deviceId) {
                        // Playback was transferred to this Phone from another device!
                        val isRecentTransfer = Math.abs(System.currentTimeMillis() - syncState.updatedAt) < 60_000L
                        val curSong = ytPlayerController.currentSong.value
                        val isPlaying = ytPlayerController.isPlaying.value
                        if (isRecentTransfer && (!isPlaying || curSong?.videoId != syncState.currentSongId) && syncState.currentSongId.isNotBlank()) {
                            val song = YouTubeSong(
                                videoId = syncState.currentSongId,
                                title = syncState.currentTitle,
                                channelTitle = syncState.currentArtist,
                                thumbnailUrl = syncState.currentArtwork,
                                audioUrl = syncState.currentAudioUrl.ifBlank { null }
                            )
                            val startSec = (syncState.positionMs / 1000f).coerceAtLeast(0f)
                            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Auto-resuming transferred song: ${song.title} at ${startSec}s")
                            playSong(song, startPositionSec = startSec, openFullPlayer = false, forceLocal = true)
                        }
                    }
                }
            }
        }

        // Remote command listener: Spotify Connect commands received from Web or other devices
        viewModelScope.launch {
            isaiConnectManager.remoteCommand.collect { cmd ->
                if (cmd.issuedByDeviceId != isaiConnectManager.deviceId) {
                    val isSeparate = localStorage.isMultiDevicePlaybackSeparate.value
                    if (isSeparate && (cmd.targetDeviceId.isBlank() || cmd.targetDeviceId != isaiConnectManager.deviceId)) {
                        return@collect
                    }
                    val isTargetedToMe = cmd.targetDeviceId.isEmpty() || cmd.targetDeviceId == isaiConnectManager.deviceId
                    val canExecute = when (cmd.action) {
                        "PLAY_SONG", "ADD_TO_QUEUE", "PLAY_NEXT_IN_QUEUE" -> isTargetedToMe
                        "PAUSE" -> isTargetedToMe || ytPlayerController.isPlaying.value || isaiConnectManager.isMyDeviceActive()
                        "PLAY" -> isTargetedToMe && (isaiConnectManager.isMyDeviceActive() || !ytPlayerController.isPlaying.value)
                        "NEXT", "PREV", "SEEK", "SET_VOLUME" -> isTargetedToMe && isaiConnectManager.isMyDeviceActive()
                        else -> isTargetedToMe
                    }
                    if (!canExecute) return@collect

                    android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Remote control action: ${cmd.action}, target=${cmd.targetDeviceId}, posMs=${cmd.positionMs}")
                    when (cmd.action) {
                        "PAUSE" -> {
                            ytPlayerController.pause()
                            isaiConnectManager.updatePlaybackState(isPlaying = false)
                        }
                        "PLAY" -> {
                            ytPlayerController.play()
                            isaiConnectManager.updatePlaybackState(isPlaying = true)
                        }
                        "NEXT" -> playNext()
                        "PREV" -> playPrevious()
                        "SEEK" -> seekTo(cmd.positionMs / 1000f)
                        "ADD_TO_QUEUE" -> {
                            if (cmd.songId.isNotBlank()) {
                                val song = YouTubeSong(
                                    videoId = cmd.songId,
                                    title = cmd.songTitle,
                                    channelTitle = cmd.songArtist,
                                    thumbnailUrl = cmd.songArtwork,
                                    audioUrl = cmd.songAudioUrl.ifBlank { null }
                                )
                                ytPlayerController.addToQueue(song)
                                isaiConnectManager.updatePlaybackState(
                                    queue = ytPlayerController.playbackQueue.value,
                                    queueIndex = ytPlayerController.currentQueueIndex.value
                                )
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    android.widget.Toast.makeText(getApplication(), "Added to queue from Web 🌐", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        "PLAY_NEXT_IN_QUEUE" -> {
                            if (cmd.songId.isNotBlank()) {
                                val song = YouTubeSong(
                                    videoId = cmd.songId,
                                    title = cmd.songTitle,
                                    channelTitle = cmd.songArtist,
                                    thumbnailUrl = cmd.songArtwork,
                                    audioUrl = cmd.songAudioUrl.ifBlank { null }
                                )
                                ytPlayerController.playNextInQueue(song)
                                isaiConnectManager.updatePlaybackState(
                                    queue = ytPlayerController.playbackQueue.value,
                                    queueIndex = ytPlayerController.currentQueueIndex.value
                                )
                            }
                        }
                        "PLAY_SONG" -> {
                            if (cmd.songId.isNotBlank()) {
                                val song = YouTubeSong(
                                    videoId = cmd.songId,
                                    title = cmd.songTitle,
                                    channelTitle = cmd.songArtist,
                                    thumbnailUrl = cmd.songArtwork,
                                    audioUrl = cmd.songAudioUrl.ifBlank { null }
                                )
                                val startSec = (cmd.positionMs / 1000f).coerceAtLeast(0f)
                                android.util.Log.i("ISAI_CONNECT", "[MainViewModel] PLAY_SONG remote command: ${song.title} at ${startSec}s")
                                playSong(song, startPositionSec = startSec, openFullPlayer = false, forceLocal = true)
                            }
                        }
                    }
                }
            }
        }

        // Automatic Endless Queue: Ensure songs keep getting added to Up Next as current song changes or advances
        viewModelScope.launch {
            ytPlayerController.currentSong.collect { song ->
                if (song != null) {
                    ensureEndlessQueue(song)
                }
            }
        }

        viewModelScope.launch {
            ytPlayerController.currentQueueIndex.collect { _ ->
                val song = ytPlayerController.currentSong.value
                if (song != null) {
                    ensureEndlessQueue(song)
                }
            }
        }
    }

    fun loadPersonalizedRecommendations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recentList = (localStorage.recentlyPlayed.value + isaiConnectManager.syncedRecentlyPlayed.value).distinctBy { it.videoId }
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
                        val cleanTopArtist = topArtist.split(",").first().split("&").first().trim()
                        _recommendedReason.value = "Because you listen to $cleanTopArtist"
                        val saavnResult = musicRepo.search("$cleanTopArtist Tamil songs")
                        val rawRecSongs = saavnResult.getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                        val recSongs = ytRepo.deduplicateSongs(rawRecSongs)
                        if (recSongs.isNotEmpty()) {
                            val heroExclude = _trendingSongs.value.take(4)
                            val filtered = recSongs.filterNot { s -> 
                                combined.any { ytRepo.isSameSong(it, s) } ||
                                heroExclude.any { ytRepo.isSameSong(it, s) }
                            }
                            val finalRecs = ytRepo.deduplicateSongs(if (filtered.isNotEmpty()) filtered else recSongs)
                            _personalizedRecommendations.value = finalRecs.take(12)
                            return@launch
                        }
                    }
                }

                // Default recommendation mix for first-time users
                _recommendedReason.value = "✨ Top Picks For You"
                val langs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                val defaultRec = musicRepo.getTrending(langs).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                val dedupedRec = ytRepo.deduplicateSongs(defaultRec)
                val heroExclude = _trendingSongs.value.take(4).map { it.videoId }.toSet()
                val distinctRec = dedupedRec.filterNot { heroExclude.contains(it.videoId) }
                _personalizedRecommendations.value = if (distinctRec.isNotEmpty()) distinctRec.take(12) else dedupedRec.take(12)
            } catch (_: Exception) {}
        }
    }

    fun loadHomeData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingHome.value = true
            try {
                val langs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                // 1. Try fetching direct audio songs via MusicRepository (JioSaavn / NepoTune)
                val saavnResult = musicRepo.getTrending(langs)
                val saavnSongs = saavnResult.getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                val cleanSaavn = saavnSongs.filterNot { s ->
                    val t = s.title.lowercase()
                    t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                }
                if (cleanSaavn.isNotEmpty()) {
                    val deduped = ytRepo.deduplicateSongs(cleanSaavn)
                    val mostPlayed = trendingService.getMostPlayedSongs(deduped)
                    _trendingSongs.value = mostPlayed
                    _categorySongs.value = mostPlayed
                    isaiConnectManager.syncHomeSongs(mostPlayed)
                } else {
                    val trending = ytRepo.getTrendingSongs(langs).filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    val deduped = ytRepo.deduplicateSongs(trending)
                    val mostPlayed = trendingService.getMostPlayedSongs(deduped)
                    _trendingSongs.value = mostPlayed
                    _categorySongs.value = mostPlayed
                    isaiConnectManager.syncHomeSongs(mostPlayed)
                }

                // Compute / Populate Popular Singers & Artists (curated by preferred language)
                val trendingSvc = com.saavn.music.data.trending.TrendingService.getInstance(getApplication())
                val artists = trendingSvc.getPopularArtists(_trendingSongs.value, _preferredLanguages.value)
                if (artists.isNotEmpty()) {
                    _popularArtists.value = artists
                }

                // Load Spotify-Style Daily Mixes (curated by preferred language with 30 songs each)
                loadDailyMixes(_preferredLanguages.value)

                // Load Popular New Releases (Released in last 30 days)
                try {
                    val newSaavn = musicRepo.search("Latest Tamil Movie Songs 2025 2026 official single").getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    val cleanNew = newSaavn.filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("jukebox") || t.contains("full album") || t.contains("non stop") || t.contains("all time hits")
                    }
                    if (cleanNew.isNotEmpty()) {
                        _latestReleases.value = ytRepo.deduplicateSongs(cleanNew).take(12)
                    } else {
                        val fallbackNew = ytRepo.getNewReleases()
                        _latestReleases.value = ytRepo.deduplicateSongs(fallbackNew).take(12)
                    }
                } catch (_: Exception) {}
            } catch (e: Exception) {
                try {
                    val langs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                    val trending = ytRepo.getTrendingSongs(langs).filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    val deduped = ytRepo.deduplicateSongs(trending)
                    _trendingSongs.value = deduped
                    _categorySongs.value = deduped
                } catch (_: Exception) {}
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingHome.value = true
            try {
                if (category == "Most Played" || category == "Trending") {
                    _categorySongs.value = trendingService.getMostPlayedSongs(_trendingSongs.value)
                    _isLoadingHome.value = false
                    return@launch
                }
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
                    _categorySongs.value = ytRepo.deduplicateSongs(cleanSaavn)
                } else {
                    val fallback = ytRepo.searchTamilSongs(query, maxResults = 50).getOrDefault(emptyList()).filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    _categorySongs.value = ytRepo.deduplicateSongs(fallback)
                }
            } catch (e: Exception) {
                // Handled
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    fun refreshCategorySongs() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingHome.value = true
            try {
                if (_selectedCategory.value == "Most Played" || _selectedCategory.value == "Trending") {
                    loadHomeData()
                } else {
                    selectCategory(_selectedCategory.value)
                }
            } catch (_: Exception) {
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    fun loadDailyMixes(preferredLanguages: List<String> = _preferredLanguages.value) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeLangs = preferredLanguages.ifEmpty { listOf("tamil") }
                val primaryLang = activeLangs.first().lowercase().trim()

                val mixConfigs = when (primaryLang) {
                    "telugu" -> listOf(
                        Triple("daily_mix_1", "Daily Mix 1 • DSP Mass Hits", "Devi Sri Prasad, Thaman & energetic Telugu anthems") to "Devi Sri Prasad Telugu hit songs",
                        Triple("daily_mix_2", "Daily Mix 2 • Sid Sriram Soul", "Sid Sriram, Anurag Kulkarni & heartfelt melodies") to "Sid Sriram Telugu melody songs",
                        Triple("daily_mix_3", "Daily Mix 3 • Keeravani Classics", "M.M. Keeravani, SPB & timeless Telugu melodies") to "M M Keeravani Telugu songs",
                        Triple("daily_mix_4", "Top 50 • TELUGU", "Trending chartbusters & fresh Telugu releases") to "Latest Telugu hits 2025 2026"
                    )
                    "hindi" -> listOf(
                        Triple("daily_mix_1", "Daily Mix 1 • Arijit Singh Soul", "Arijit Singh, Jasleen Royal & soulful romantic hits") to "Arijit Singh Hindi romantic hit songs",
                        Triple("daily_mix_2", "Daily Mix 2 • Pritam Hit Machine", "Pritam, KK & iconic Bollywood melodies") to "Pritam Bollywood super hit songs",
                        Triple("daily_mix_3", "Daily Mix 3 • Bollywood Party Beats", "Badshah, Sachin-Jigar & club bangers") to "Hindi party dance songs 2025",
                        Triple("daily_mix_4", "Top 50 • HINDI", "Top trending Hindi hits and new releases") to "Top Hindi songs 2025 2026"
                    )
                    "malayalam" -> listOf(
                        Triple("daily_mix_1", "Daily Mix 1 • Sushin Shyam Hits", "Sushin Shyam, Jakes Bejoy & trendy new wave Malayalam") to "Sushin Shyam Malayalam hit songs",
                        Triple("daily_mix_2", "Daily Mix 2 • Hesham Melodies", "Hesham Abdul Wahab, K.S. Harisankar & heart-touching tunes") to "Hesham Abdul Wahab Malayalam songs",
                        Triple("daily_mix_3", "Daily Mix 3 • Evergreen Malayalam", "Vidyasagar, Deepak Dev & nostalgic melodies") to "Malayalam melody hit songs",
                        Triple("daily_mix_4", "Top 50 • MALAYALAM", "The most played trending hits in Malayalam") to "Latest Malayalam hits 2025 2026"
                    )
                    else -> listOf(
                        Triple("daily_mix_1", "Daily Mix 1 • Anirudh Hits", "Anirudh, Dhanush, Vijay & club anthems") to "Anirudh Ravichander Tamil hit songs",
                        Triple("daily_mix_2", "Daily Mix 2 • A.R. Rahman Soul", "A.R. Rahman, Bombay Jayashri & timeless melodies") to "A R Rahman Tamil melody hit songs",
                        Triple("daily_mix_3", "Daily Mix 3 • Yuvan Drug Melodies", "Yuvan Shankar Raja, Harris Jayaraj & night drives") to "Yuvan Shankar Raja Tamil hits",
                        Triple("daily_mix_4", "Top 50 • TAMIL", "The most played and trending hits in Tamil") to "Latest Tamil hits 2025 2026"
                    )
                }

                val loadedMixes = mutableListOf<SpotifyDailyMix>()
                for ((info, query) in mixConfigs) {
                    val (id, title, subtitle) = info
                    val rawSongs = musicRepo.search(query, limit = 40).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    val dedupedSongs = ytRepo.deduplicateSongs(rawSongs).take(30)
                    val cover = dedupedSongs.firstOrNull()?.thumbnailUrl ?: "https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg"

                    if (dedupedSongs.isNotEmpty()) {
                        loadedMixes.add(
                            SpotifyDailyMix(
                                id = id,
                                title = title,
                                subtitle = "$subtitle • ${dedupedSongs.size} Songs",
                                coverUrl = cover,
                                songs = dedupedSongs
                            )
                        )
                    }
                }

                if (loadedMixes.isNotEmpty()) {
                    _spotifyDailyMixes.value = loadedMixes
                }
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYER", "Failed loading daily mixes: ${e.message}")
            }
        }
    }

    fun selectArtist(artistName: String) {
        val clean = artistName
            .replace("Topic", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .trim()
        val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
        val primaryLang = activeLangs.first().replaceFirstChar { it.uppercase() }

        _searchQuery.value = "$clean Hits"
        _searchLanguageFilter.value = primaryLang
        setScreen(AppScreen.SEARCH)

        // Asynchronously fetch rich collection of min 30-50 songs for this artist in preferred languages
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                val q1 = "$clean $primaryLang hit songs"
                val q2 = "$clean super hits"
                val q3 = "$clean best melody songs"

                val r1 = async { musicRepo.search(q1, limit = 40).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList() }
                val r2 = async { musicRepo.search(q2, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList() }
                val r3 = async { musicRepo.search(q3, limit = 30).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList() }

                val combined = (r1.await() + r2.await() + r3.await())
                val deduped = ytRepo.deduplicateSongs(combined.distinctBy { it.videoId })

                val filtered = if (deduped.isNotEmpty()) {
                    deduped.filter { s ->
                        val t = s.title.lowercase()
                        val a = s.channelTitle.lowercase()
                        val c = clean.lowercase()
                        t.contains(c) || a.contains(c) || calculateSongRelevance(s, clean) > 0
                    }
                } else emptyList()

                val finalSongs = if (filtered.size >= 30) filtered else deduped
                _searchResults.value = finalSongs.take(50)
            } catch (e: Exception) {
                _searchError.value = "Could not load artist songs: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun calculateSongRelevance(song: YouTubeSong, query: String): Int {
        val stopWords = setOf("song", "songs", "all", "the", "a", "an", "hits", "track", "music", "mp3", "new", "latest", "best", "padal", "paadalgal")
        val tokens = query.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 1 && it !in stopWords }
        if (tokens.isEmpty()) return 1

        val title = song.title.lowercase()
        val artist = song.channelTitle.lowercase()
        val fullText = "$title $artist"

        var score = 0
        for (token in tokens) {
            if (title.contains(token)) score += 3
            else if (artist.contains(token)) score += 2

            if (token == "gana" && fullText.contains("gaana")) score += 3
            if (token == "gaana" && fullText.contains("gana")) score += 3
        }
        return score
    }

    private fun isSearchRelevant(query: String, songs: List<YouTubeSong>): Boolean {
        if (songs.isEmpty()) return false
        val stopWords = setOf("song", "songs", "tamil", "telugu", "hindi", "hit", "hits", "all", "the", "a", "an", "best", "new", "latest")
        val tokens = query.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 1 && it !in stopWords }
        if (tokens.isEmpty()) return true

        val matchCount = songs.count { song -> calculateSongRelevance(song, query) > 0 }
        return matchCount > 0 && (matchCount.toDouble() / songs.size) >= 0.15
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

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            _searchError.value = null
            delay(350)
            try {
                val clean = newQuery.trim().lowercase()

                // Special direct commands
                if (clean == "trending" || clean == "trends" || clean == "charts") {
                    _searchResults.value = _trendingSongs.value
                    _searchError.value = null
                    return@launch
                }

                val activeLangFilter = _searchLanguageFilter.value
                val userLangs = if (activeLangFilter != "All") {
                    listOf(activeLangFilter.lowercase())
                } else {
                    _preferredLanguages.value.ifEmpty { listOf("tamil") }
                }

                // Extract user favorite artists for non-invasive personalization
                val favArtists = favorites.value.mapNotNull { it.channelTitle.takeIf { t -> t.isNotBlank() } }.distinct().take(10)

                // Parse query using ISAI Smart Search Engine
                val parsedIntent = SmartSearchEngine.parseQuery(
                    rawQuery = newQuery,
                    userPreferredLanguages = userLangs,
                    userFavoriteArtists = favArtists
                )

                android.util.Log.i("ISAI_SEARCH", "[SmartSearch] Query: '$newQuery' -> AudioQuery: '${parsedIntent.directAudioQuery}', YTQuery: '${parsedIntent.youtubeMusicQuery}', Intent: [lang=${parsedIntent.detectedLanguage}, art=${parsedIntent.detectedArtist}, mood=${parsedIntent.detectedMood}, genre=${parsedIntent.detectedGenre}, era=${parsedIntent.detectedYearOrEra}]")

                val candidateList = mutableListOf<Pair<YouTubeSong, Int>>()

                // Execute targeted search with primary YouTube Music rank preservation
                coroutineScope {
                    val ytDeferred = async {
                        ytRepo.searchSongs(parsedIntent.youtubeMusicQuery, maxResults = 50).getOrDefault(emptyList())
                    }
                    val saavnDeferred = async {
                        if (parsedIntent.directAudioQuery.isNotBlank()) {
                            musicRepo.search(parsedIntent.directAudioQuery, limit = 30).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                        } else emptyList()
                    }

                    val ytResults = ytDeferred.await()
                    val saavnResults = saavnDeferred.await()

                    // Add primary YouTube results retaining their original Innertube rank
                    ytResults.forEachIndexed { index, song ->
                        candidateList.add(Pair(song, index))
                    }

                    // If primary query had very few results, fallback to raw user query
                    if (ytResults.size < 5) {
                        val cleanRaw = newQuery.trim()
                        if (!cleanRaw.equals(parsedIntent.youtubeMusicQuery, ignoreCase = true)) {
                            val rawYt = ytRepo.searchSongs(cleanRaw, maxResults = 30).getOrDefault(emptyList())
                            rawYt.forEachIndexed { index, song ->
                                candidateList.add(Pair(song, ytResults.size + index))
                            }
                        }
                    }

                    // Add JioSaavn supplementary results with lower initial priority
                    saavnResults.forEachIndexed { index, song ->
                        candidateList.add(Pair(song, ytResults.size + 15 + index))
                    }

                    // Fallback to related queries ONLY if total candidate count is very low (< 5)
                    if (candidateList.size < 5 && parsedIntent.relatedQueries.isNotEmpty()) {
                        for (relQ in parsedIntent.relatedQueries.take(2)) {
                            val fallbackList = ytRepo.searchSongs(relQ, maxResults = 20).getOrDefault(emptyList())
                            fallbackList.forEachIndexed { index, song ->
                                candidateList.add(Pair(song, 40 + index))
                            }
                            if (candidateList.size >= 15) break
                        }
                    }
                }

                if (candidateList.isNotEmpty()) {
                    // Deduplicate identical videoIds and identical (title, channel) pairs
                    val seenIds = mutableSetOf<String>()
                    val seenSignatures = mutableSetOf<String>()
                    val uniqueCandidates = mutableListOf<Pair<YouTubeSong, Int>>()

                    for (pair in candidateList) {
                        val song = pair.first
                        val id = song.videoId.trim()
                        val normTitle = song.title.lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]"), "").take(25)
                        val normChannel = song.channelTitle.lowercase(java.util.Locale.ROOT).replace(Regex("[^a-z0-9]"), "").take(15)
                        val sig = "$normTitle|$normChannel"

                        if (id.isNotBlank() && id in seenIds) continue
                        if (sig.length > 5 && sig in seenSignatures) continue

                        if (id.isNotBlank()) seenIds.add(id)
                        seenSignatures.add(sig)
                        uniqueCandidates.add(pair)
                    }

                    // Score and rank using SmartSearchEngine precision ranker
                    val ranked = uniqueCandidates.sortedByDescending { (song, origRank) ->
                        SmartSearchEngine.scoreAndRankSong(
                            song = song,
                            intent = parsedIntent,
                            userPreferredLanguages = userLangs,
                            userFavoriteArtists = favArtists,
                            originalRank = origRank
                        )
                    }.map { it.first }

                    _searchResults.value = ranked
                    _searchError.value = null
                } else {
                    _searchResults.value = emptyList()
                    _searchError.value = "No songs found for '$newQuery'"
                }
            } catch (e: Exception) {
                try {
                    val result = ytRepo.searchSongs(newQuery)
                    _searchResults.value = result.getOrDefault(emptyList())
                    _searchError.value = null
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

    fun playSong(
        song: YouTubeSong,
        queue: List<YouTubeSong>? = null,
        startPositionSec: Float = 0f,
        openFullPlayer: Boolean = true,
        forceLocal: Boolean = false
    ) {
        // Claim ISAI Connect active device status for this phone
        isaiConnectManager.transferPlaybackToDevice(isaiConnectManager.deviceId)

        android.util.Log.i("ISAI_PLAYER", "========================================")
        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] playSong triggered! startPositionSec=$startPositionSec, openFullPlayer=$openFullPlayer")
        android.util.Log.i("ISAI_PLAYER", "Song Title: ${song.title}")
        android.util.Log.i("ISAI_PLAYER", "Has Direct Audio: ${!song.audioUrl.isNullOrBlank()}")
        android.util.Log.i("ISAI_PLAYER", "========================================")

        if (openFullPlayer) {
            _showFullPlayer.value = true
        }

        // Log Analytics and Listening History
        analyticsService.trackEvent(song.videoId, song.title, song.channelTitle, "play")
        viewModelScope.launch {
            playlistService.recordHistory(song)
        }

        // Generate context-aware relevant playback queue (Love songs get love songs, Motivation get motivation)
        val isAdvancingInCurrentPlaybackQueue = !queue.isNullOrEmpty() &&
            queue == ytPlayerController.playbackQueue.value &&
            queue.any { it.videoId == song.videoId }

        val effectiveQueue = if (isAdvancingInCurrentPlaybackQueue) {
            queue!!
        } else {
            val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
            val candidatePool = if (queue.isNullOrEmpty() || queue.size <= 1 || queue == _trendingSongs.value) {
                val suggestionsList = _suggestions.value.filter { it.videoId != song.videoId }
                val trendingList = _trendingSongs.value.filter { it.videoId != song.videoId }
                (listOf(song) + suggestionsList + trendingList).distinctBy { it.videoId }
            } else {
                (listOf(song) + queue + _trendingSongs.value).distinctBy { it.videoId }
            }
            com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, candidatePool, activeLangs, 50)
        }

        // Asynchronously load matching suggestions for this specific mood/genre
        loadSuggestionsForSong(song)

        // Claim ISAI Connect active device status and sync song details and queue to Firebase (only when not in separate multi-device mode)
        if (!localStorage.isMultiDevicePlaybackSeparate.value) {
            isaiConnectManager.updatePlaybackState(
                song = song,
                isPlaying = true,
                positionMs = (startPositionSec * 1000).toLong(),
                currentDeviceId = isaiConnectManager.deviceId,
                queue = effectiveQueue,
                queueIndex = 0
            )
        }

        if (!song.audioUrl.isNullOrBlank()) {
            ytPlayerController.playSong(song, effectiveQueue, startPositionSec)
        } else {
            ytPlayerController.setBuffering(true)
            // Asynchronously resolve direct 320kbps audio stream
            viewModelScope.launch {
                try {
                    val cleanTitle = song.title
                        .replace(Regex("\\s*[|\\-–—].*$"), "")
                        .replace(Regex("\\s*\\(.*?(official|video|audio|lyrics|hd|4k|song).*?\\)", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("\\s*\\[.*?(official|video|audio|lyrics|hd|4k|song).*?\\]", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("\\.{2,}$"), "")
                        .trim()
                    val directUrl = musicRepo.resolveStreamUrl(cleanTitle.ifBlank { song.title }, song.channelTitle)
                    if (!directUrl.isNullOrBlank()) {
                        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Resolved direct 320kbps audio: $directUrl")
                        val updatedSong = song.copy(audioUrl = directUrl)
                        val updatedQueue = effectiveQueue.map { if (it.videoId == song.videoId) updatedSong else it }
                        isaiConnectManager.updatePlaybackState(song = updatedSong, isPlaying = true, queue = updatedQueue)
                        ytPlayerController.playSong(updatedSong, updatedQueue, startPositionSec)
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ISAI_PLAYER", "[MainViewModel] Could not resolve direct stream: ${e.message}")
                }
                ytPlayerController.playSong(song, effectiveQueue, startPositionSec)
            }
        }
    }

    fun addToQueue(song: YouTubeSong) {
        val remoteState = isaiConnectManager.playbackState.value
        val isRemoteActive = remoteState != null &&
                remoteState.currentDeviceId.isNotBlank() &&
                remoteState.currentDeviceId != isaiConnectManager.deviceId &&
                remoteState.currentTitle.isNotBlank() &&
                (remoteState.isPlaying || Math.abs(System.currentTimeMillis() - remoteState.updatedAt) < 15 * 60_000L)

        if (remoteState != null && isRemoteActive) {
            isaiConnectManager.sendCommand(
                action = "ADD_TO_QUEUE",
                song = song,
                targetDeviceId = remoteState.currentDeviceId
            )
            val currentRemoteSongs = remoteState.queue.map { syncSong ->
                YouTubeSong(
                    videoId = syncSong.id,
                    title = syncSong.title,
                    channelTitle = syncSong.artist,
                    thumbnailUrl = syncSong.artwork,
                    audioUrl = syncSong.audioUrl.ifBlank { null }
                )
            }.toMutableList()
            if (currentRemoteSongs.none { it.videoId == song.videoId }) {
                currentRemoteSongs.add(song)
                isaiConnectManager.updatePlaybackState(queue = currentRemoteSongs)
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(getApplication(), "Added to Remote Queue 📱", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            if (ytPlayerController.currentSong.value == null) {
                playSong(song, listOf(song))
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(getApplication(), "Playing ${song.title.take(20)}... 🎵", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                val currentQueue = ytPlayerController.playbackQueue.value
                val isAlreadyInQueue = currentQueue.any { it.videoId == song.videoId }
                ytPlayerController.addToQueue(song)
                isaiConnectManager.updatePlaybackState(
                    currentDeviceId = isaiConnectManager.deviceId,
                    queue = ytPlayerController.playbackQueue.value,
                    queueIndex = ytPlayerController.currentQueueIndex.value
                )
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (isAlreadyInQueue) {
                        android.widget.Toast.makeText(getApplication(), "Song already in queue ℹ️", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(getApplication(), "Added to queue 🎵", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun playNextInQueue(song: YouTubeSong) {
        val remoteState = isaiConnectManager.playbackState.value
        val isRemoteActive = remoteState != null &&
                remoteState.currentDeviceId.isNotBlank() &&
                remoteState.currentDeviceId != isaiConnectManager.deviceId &&
                remoteState.currentTitle.isNotBlank() &&
                (remoteState.isPlaying || Math.abs(System.currentTimeMillis() - remoteState.updatedAt) < 15 * 60_000L)

        if (remoteState != null && isRemoteActive) {
            isaiConnectManager.sendCommand(
                action = "PLAY_NEXT_IN_QUEUE",
                song = song,
                targetDeviceId = remoteState.currentDeviceId
            )
            val currentRemoteSongs = remoteState.queue.map { syncSong ->
                YouTubeSong(
                    videoId = syncSong.id,
                    title = syncSong.title,
                    channelTitle = syncSong.artist,
                    thumbnailUrl = syncSong.artwork,
                    audioUrl = syncSong.audioUrl.ifBlank { null }
                )
            }.toMutableList()
            currentRemoteSongs.removeAll { it.videoId == song.videoId }
            val insertPos = (remoteState.queueIndex + 1).coerceAtMost(currentRemoteSongs.size)
            currentRemoteSongs.add(insertPos, song)
            isaiConnectManager.updatePlaybackState(queue = currentRemoteSongs)
        } else {
            if (ytPlayerController.currentSong.value == null) {
                playSong(song, listOf(song))
            } else {
                ytPlayerController.playNextInQueue(song)
                isaiConnectManager.updatePlaybackState(
                    currentDeviceId = isaiConnectManager.deviceId,
                    queue = ytPlayerController.playbackQueue.value,
                    queueIndex = ytPlayerController.currentQueueIndex.value
                )
            }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(getApplication(), "Playing next in queue ⏭️", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun removeFromQueue(index: Int) {
        val remoteState = isaiConnectManager.playbackState.value
        val isRemoteActive = remoteState != null &&
                remoteState.currentDeviceId.isNotBlank() &&
                remoteState.currentDeviceId != isaiConnectManager.deviceId &&
                remoteState.currentTitle.isNotBlank() &&
                (remoteState.isPlaying || Math.abs(System.currentTimeMillis() - remoteState.updatedAt) < 15 * 60_000L)

        if (remoteState != null && isRemoteActive) {
            val currentRemoteSongs = remoteState.queue.map { syncSong ->
                YouTubeSong(
                    videoId = syncSong.id,
                    title = syncSong.title,
                    channelTitle = syncSong.artist,
                    thumbnailUrl = syncSong.artwork,
                    audioUrl = syncSong.audioUrl.ifBlank { null }
                )
            }.toMutableList()
            if (index in currentRemoteSongs.indices) {
                currentRemoteSongs.removeAt(index)
                var newQueueIndex = remoteState.queueIndex
                if (index < newQueueIndex) {
                    newQueueIndex = (newQueueIndex - 1).coerceAtLeast(0)
                } else if (index == newQueueIndex) {
                    if (currentRemoteSongs.isNotEmpty()) {
                        newQueueIndex = newQueueIndex.coerceAtMost(currentRemoteSongs.size - 1)
                        val nextSong = currentRemoteSongs[newQueueIndex]
                        isaiConnectManager.sendCommand(
                            action = "PLAY_SONG",
                            song = nextSong,
                            targetDeviceId = remoteState.currentDeviceId
                        )
                    } else {
                        newQueueIndex = 0
                    }
                }
                isaiConnectManager.updatePlaybackState(queue = currentRemoteSongs, queueIndex = newQueueIndex)
            }
        } else {
            ytPlayerController.removeFromQueue(index)
            if (isaiConnectManager.isMyDeviceActive()) {
                isaiConnectManager.updatePlaybackState(
                    queue = ytPlayerController.playbackQueue.value,
                    queueIndex = ytPlayerController.currentQueueIndex.value
                )
            }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(getApplication(), "Removed from queue ✕", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val remoteState = isaiConnectManager.playbackState.value
        val isRemoteActive = remoteState != null &&
                remoteState.currentDeviceId.isNotBlank() &&
                remoteState.currentDeviceId != isaiConnectManager.deviceId &&
                remoteState.currentTitle.isNotBlank() &&
                (remoteState.isPlaying || Math.abs(System.currentTimeMillis() - remoteState.updatedAt) < 15 * 60_000L)

        if (remoteState != null && isRemoteActive) {
            val currentRemoteSongs = remoteState.queue.map { syncSong ->
                YouTubeSong(
                    videoId = syncSong.id,
                    title = syncSong.title,
                    channelTitle = syncSong.artist,
                    thumbnailUrl = syncSong.artwork,
                    audioUrl = syncSong.audioUrl.ifBlank { null }
                )
            }.toMutableList()
            if (fromIndex in currentRemoteSongs.indices && toIndex in currentRemoteSongs.indices && fromIndex != toIndex) {
                val item = currentRemoteSongs.removeAt(fromIndex)
                currentRemoteSongs.add(toIndex, item)
                val currentIdx = remoteState.queueIndex
                val newCurrentIdx = when {
                    currentIdx == fromIndex -> toIndex
                    fromIndex < currentIdx && toIndex >= currentIdx -> currentIdx - 1
                    fromIndex > currentIdx && toIndex <= currentIdx -> currentIdx + 1
                    else -> currentIdx
                }
                isaiConnectManager.updatePlaybackState(queue = currentRemoteSongs, queueIndex = newCurrentIdx)
            }
        } else {
            ytPlayerController.moveQueueItem(fromIndex, toIndex)
            if (isaiConnectManager.isMyDeviceActive()) {
                isaiConnectManager.updatePlaybackState(
                    queue = ytPlayerController.playbackQueue.value,
                    queueIndex = ytPlayerController.currentQueueIndex.value
                )
            }
        }
    }

    fun clearQueue() {
        val remoteState = isaiConnectManager.playbackState.value
        val isRemoteActive = remoteState != null &&
                remoteState.currentDeviceId.isNotBlank() &&
                remoteState.currentDeviceId != isaiConnectManager.deviceId &&
                remoteState.currentTitle.isNotBlank() &&
                (remoteState.isPlaying || Math.abs(System.currentTimeMillis() - remoteState.updatedAt) < 15 * 60_000L)

        if (remoteState != null && isRemoteActive) {
            val currentSong = remoteState.queue.getOrNull(remoteState.queueIndex)
            val newQueue = if (currentSong != null) listOf(YouTubeSong(
                videoId = currentSong.id,
                title = currentSong.title,
                channelTitle = currentSong.artist,
                thumbnailUrl = currentSong.artwork,
                audioUrl = currentSong.audioUrl.ifBlank { null }
            )) else emptyList()
            isaiConnectManager.updatePlaybackState(queue = newQueue, queueIndex = 0)
        } else {
            ytPlayerController.clearQueue()
            if (isaiConnectManager.isMyDeviceActive()) {
                isaiConnectManager.updatePlaybackState(
                    queue = ytPlayerController.playbackQueue.value,
                    queueIndex = 0
                )
            }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(getApplication(), "Queue cleared", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun loadSuggestionsForSong(song: YouTubeSong) {
        viewModelScope.launch {
            _isLoadingSuggestions.value = true
            try {
                val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                val primaryLang = activeLangs.first()
                // Generate relevant targeted query based on song mood/genre (Gana, Kuthu, Melody, etc.) and preferred language
                val query = com.saavn.music.util.RelevanceEngine.getRelevantSearchQuery(song, primaryLang)
                val primaryArtist = com.saavn.music.util.RelevanceEngine.extractPrimaryArtist(song.channelTitle)
                val songMood = com.saavn.music.util.RelevanceEngine.detectSongMood(song)
                val moodKeyword = when (songMood) {
                    com.saavn.music.util.SongMood.MOTIVATION_INSPIRING -> "motivational inspiring"
                    com.saavn.music.util.SongMood.MELODY_ROMANCE -> "love romantic melody"
                    com.saavn.music.util.SongMood.DEVOTIONAL -> "devotional bakthi"
                    com.saavn.music.util.SongMood.SAD_HEARTBREAK -> "sad breakup"
                    com.saavn.music.util.SongMood.PARTY_KUTHU -> "kuthu dance party"
                    com.saavn.music.util.SongMood.GANA_FOLK -> "gana folk"
                    com.saavn.music.util.SongMood.INTRO_MASS -> "mass hero entry"
                    else -> "top songs"
                }
                val artistQuery = if (primaryArtist.isNotBlank()) "$primaryArtist $primaryLang $moodKeyword songs" else "$primaryLang $moodKeyword songs"

                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Loading mood ($songMood) & artist suggestions: '$query' / '$artistQuery'")

                val searchResultDeferred = async { musicRepo.search(query, limit = 40).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList() }
                val artistResultDeferred = async { musicRepo.search(artistQuery, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList() }

                val searchResult = searchResultDeferred.await()
                val artistResult = artistResultDeferred.await()

                val pool = (searchResult + artistResult + _trendingSongs.value).distinctBy { it.videoId }
                val filteredPool = pool.filter { 
                    it.videoId != song.videoId &&
                    com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) &&
                    com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, it, activeLangs) > 0
                }

                val relevantMatches = if (filteredPool.isNotEmpty()) {
                    com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, filteredPool, activeLangs, maxItems = 50).filter { it.videoId != song.videoId }
                } else {
                    val ytResult = ytRepo.searchTamilSongs(query, maxResults = 35).getOrDefault(emptyList())
                    val filteredYt = ytResult.filter { 
                        it.videoId != song.videoId &&
                        com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) &&
                        com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, it, activeLangs) > 0
                    }
                    com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, filteredYt, activeLangs, maxItems = 50).filter { it.videoId != song.videoId }
                }

                _suggestions.value = relevantMatches

                // Immediately replace the generic upcoming queue with these mood and artist matched songs!
                if (relevantMatches.isNotEmpty()) {
                    ytPlayerController.replaceUpcomingQueue(relevantMatches)
                    if (isaiConnectManager.isMyDeviceActive()) {
                        isaiConnectManager.updatePlaybackState(
                            queue = ytPlayerController.playbackQueue.value,
                            queueIndex = ytPlayerController.currentQueueIndex.value
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYER", "[MainViewModel] Failed to load suggestions: ${e.message}")
            } finally {
                _isLoadingSuggestions.value = false
                ensureEndlessQueue(song)
            }
        }
    }

    fun ensureEndlessQueue(song: YouTubeSong? = ytPlayerController.currentSong.value) {
        viewModelScope.launch {
            try {
                val currentQueue = ytPlayerController.playbackQueue.value
                val currentIndex = ytPlayerController.currentQueueIndex.value
                val remainingInQueue = currentQueue.size - (currentIndex + 1)

                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] ensureEndlessQueue check: queueSize=${currentQueue.size}, currentIdx=$currentIndex, remaining=$remainingInQueue")

                if (remainingInQueue <= 10 && song != null) {
                    val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                    val primaryLang = activeLangs.first()

                    val query = com.saavn.music.util.RelevanceEngine.getRelevantSearchQuery(song, primaryLang)
                    val primaryArtist = com.saavn.music.util.RelevanceEngine.extractPrimaryArtist(song.channelTitle)
                    val songMood = com.saavn.music.util.RelevanceEngine.detectSongMood(song)
                    val moodKeyword = when (songMood) {
                        com.saavn.music.util.SongMood.MOTIVATION_INSPIRING -> "motivational inspiring"
                        com.saavn.music.util.SongMood.MELODY_ROMANCE -> "love romantic melody"
                        com.saavn.music.util.SongMood.DEVOTIONAL -> "devotional bakthi"
                        com.saavn.music.util.SongMood.SAD_HEARTBREAK -> "sad breakup"
                        com.saavn.music.util.SongMood.PARTY_KUTHU -> "kuthu dance party"
                        com.saavn.music.util.SongMood.GANA_FOLK -> "gana folk"
                        com.saavn.music.util.SongMood.INTRO_MASS -> "mass hero"
                        else -> "top songs"
                    }
                    val artistQuery = if (primaryArtist.isNotBlank()) "$primaryArtist $primaryLang $moodKeyword songs" else "$primaryLang $moodKeyword hit songs"

                    val searchHits = async {
                        musicRepo.search(query, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    }
                    val artistHits = async {
                        musicRepo.search(artistQuery, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    }
                    val combinedHits = (searchHits.await() + artistHits.await() + _trendingSongs.value)

                    val existingVideoIds = currentQueue.map { it.videoId }.toSet()
                    val existingNormTitles = currentQueue.map { 
                        it.title.lowercase().filter { ch: Char -> ch.isLetterOrDigit() }.take(15) 
                    }.toSet()

                    val newSongs = mutableListOf<YouTubeSong>()
                    val seenInBatch = mutableSetOf<String>()

                    for (candidate in combinedHits) {
                        if (candidate.videoId == song.videoId) continue
                        if (existingVideoIds.contains(candidate.videoId)) continue
                        if (!com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs)) continue
                        if (com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, candidate, activeLangs) <= 0) continue
                        val norm = candidate.title.lowercase().filter { ch: Char -> ch.isLetterOrDigit() }.take(15)
                        if (existingNormTitles.contains(norm) || seenInBatch.contains(norm)) continue
                        seenInBatch.add(norm)
                        newSongs.add(candidate)
                        if (newSongs.size >= 20) break
                    }

                    if (newSongs.isNotEmpty()) {
                        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Appending ${newSongs.size} candidate songs to endless queue")
                        ytPlayerController.appendQueue(newSongs)
                        if (isaiConnectManager.isMyDeviceActive()) {
                            isaiConnectManager.updatePlaybackState(
                                queue = ytPlayerController.playbackQueue.value,
                                queueIndex = ytPlayerController.currentQueueIndex.value
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYER", "[MainViewModel] Endless queue error: ${e.message}")
            }
        }
    }

    fun playNextAutoSuggestion() {
        viewModelScope.launch {
            android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Auto-play triggered by onQueueExhausted!")
            val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
            val currentSuggestions = _suggestions.value.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
            val currentId = ytPlayerController.currentSong.value?.videoId
            val candidate = currentSuggestions.firstOrNull { it.videoId != currentId }
                ?: _trendingSongs.value.firstOrNull { it.videoId != currentId && com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }

            if (candidate != null) {
                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Auto-advancing to: '${candidate.title}'")
                // Add to playback queue so it's reflected in queue UI
                ytPlayerController.addToQueue(candidate)
                playSong(candidate, ytPlayerController.playbackQueue.value, openFullPlayer = false)
            } else {
                android.util.Log.w("ISAI_PLAYER", "[MainViewModel] No candidate song found for auto-advance.")
            }
        }
    }

    fun togglePlayPause() {
        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] togglePlayPause called!")
        val syncState = isaiConnectManager.playbackState.value
        val isRemoteActive = syncState != null && 
            syncState.currentDeviceId.isNotBlank() && 
            !isaiConnectManager.isMyDeviceActive() && 
            !syncState.currentTitle.isNullOrBlank() &&
            (syncState.isPlaying || Math.abs(System.currentTimeMillis() - syncState.updatedAt) < 15 * 60_000L)

        if (isRemoteActive) {
            val isPlaying = syncState?.isPlaying == true
            val nextAction = if (isPlaying) "PAUSE" else "PLAY"
            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Sending remote $nextAction to active device: ${syncState?.currentDeviceId}")
            isaiConnectManager.sendCommand(nextAction, targetDeviceId = syncState?.currentDeviceId ?: "")
            isaiConnectManager.updatePlaybackState(isPlaying = !isPlaying)
            return
        }
        ytPlayerController.togglePlayPause()
    }

    fun playNext() {
        val syncState = isaiConnectManager.playbackState.value
        val isRemoteActive = syncState != null && 
            syncState.currentDeviceId.isNotBlank() && 
            !isaiConnectManager.isMyDeviceActive() && 
            !syncState.currentTitle.isNullOrBlank() &&
            (syncState.isPlaying || Math.abs(System.currentTimeMillis() - syncState.updatedAt) < 15 * 60_000L)

        if (isRemoteActive) {
            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Sending remote NEXT to active device")
            isaiConnectManager.sendCommand("NEXT", targetDeviceId = syncState?.currentDeviceId ?: "")
            return
        }
        ytPlayerController.playNext()
    }

    fun playPrevious() {
        val syncState = isaiConnectManager.playbackState.value
        val isRemoteActive = syncState != null && 
            syncState.currentDeviceId.isNotBlank() && 
            !isaiConnectManager.isMyDeviceActive() && 
            !syncState.currentTitle.isNullOrBlank() &&
            (syncState.isPlaying || Math.abs(System.currentTimeMillis() - syncState.updatedAt) < 15 * 60_000L)

        if (isRemoteActive) {
            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Sending remote PREV to active device")
            isaiConnectManager.sendCommand("PREV", targetDeviceId = syncState?.currentDeviceId ?: "")
            return
        }
        ytPlayerController.playPrevious()
    }

    fun seekTo(seconds: Float) {
        val syncState = isaiConnectManager.playbackState.value
        val isRemoteActive = syncState != null && 
            syncState.currentDeviceId.isNotBlank() && 
            !isaiConnectManager.isMyDeviceActive() && 
            !syncState.currentTitle.isNullOrBlank() &&
            (syncState.isPlaying || Math.abs(System.currentTimeMillis() - syncState.updatedAt) < 15 * 60_000L)

        if (isRemoteActive) {
            val ms = (seconds * 1000).toLong()
            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Sending remote SEEK $ms to active device")
            isaiConnectManager.sendCommand("SEEK", positionMs = ms, targetDeviceId = syncState?.currentDeviceId ?: "")
            isaiConnectManager.updatePlaybackState(positionMs = ms)
            return
        }
        ytPlayerController.seekTo(seconds)
    }

    fun setVolume(volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        ytPlayerController.setVolume(clamped)

        val syncState = isaiConnectManager.playbackState.value
        val isRemoteActive = syncState != null && 
            syncState.currentDeviceId.isNotBlank() && 
            !isaiConnectManager.isMyDeviceActive() && 
            (syncState.isPlaying || Math.abs(System.currentTimeMillis() - syncState.updatedAt) < 15 * 60_000L)

        if (isRemoteActive && syncState != null) {
            val volFloat = clamped / 100f
            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Sending remote volume $volFloat ($clamped%) to active device")
            isaiConnectManager.sendCommand(
                action = "SET_VOLUME",
                positionMs = clamped.toLong(),
                volume = volFloat,
                targetDeviceId = syncState.currentDeviceId
            )
            isaiConnectManager.updatePlaybackState(volume = volFloat)
        }
    }

    fun isFavorite(videoId: String): Boolean {
        return localStorage.isFavorite(videoId)
    }

    fun toggleFavorite(song: YouTubeSong) {
        localStorage.toggleFavorite(song)
        isaiConnectManager.syncFavorites(localStorage.favorites.value)
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
        // Only allow closing dialog if user is unauthenticated or fully verified
        if (!authService.isUserLoggedIn() || authService.isEmailVerified()) {
            _showLoginDialog.value = false
        }
    }

    fun openLanguageDialog() {
        _showLanguageDialog.value = true
    }

    fun closeLanguageDialog() {
        _showLanguageDialog.value = false
    }

    fun setPreferredLanguages(languages: List<String>) {
        _preferredLanguages.value = languages
        _showLanguageDialog.value = false
        val currentProfile = localStorage.userProfile.value
        if (currentProfile != null) {
            localStorage.saveUserProfile(currentProfile.copy(preferredLanguages = languages))
        } else {
            localStorage.saveUserProfile(
                UserProfile(
                    id = "user_default",
                    displayName = "ISAI Listener",
                    isLoggedIn = true,
                    preferredLanguages = languages
                )
            )
        }
        isaiConnectManager.syncPreferences(languages)
        loadHomeData()
        val currentQueue = ytPlayerController.playbackQueue.value
        val curIdx = ytPlayerController.currentQueueIndex.value
        if (currentQueue.isNotEmpty()) {
            val playedSoFar = currentQueue.take(curIdx + 1)
            val upcoming = currentQueue.drop(curIdx + 1)
            val validUpcoming = upcoming.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, languages) }
            ytPlayerController.setPlaybackQueue(playedSoFar + validUpcoming, curIdx)
        }
        ytPlayerController.currentSong.value?.let { current ->
            loadSuggestionsForSong(current)
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        val withLogin = profile.copy(isLoggedIn = true)
        localStorage.saveUserProfile(withLogin)
        closeLoginDialog()
        if (withLogin.preferredLanguages.isEmpty()) {
            openLanguageDialog()
        } else {
            _preferredLanguages.value = withLogin.preferredLanguages
            isaiConnectManager.syncPreferences(withLogin.preferredLanguages)
            loadHomeData()
        }
    }

    fun updateUsername(newUsername: String) {
        val current = userProfile.value ?: UserProfile(
            id = authService.getCurrentUserId(),
            displayName = newUsername,
            email = authService.getCurrentUser()?.email ?: "user@isaimusic.com"
        )
        val updated = current.copy(displayName = newUsername)
        localStorage.saveUserProfile(updated)
        authService.updateDisplayName(newUsername)
        isaiConnectManager.updateDeviceOwner(newUsername)
    }

    fun quickSignInGoogleAccount(displayName: String = "ISAI Listener", email: String = "user@isaimusic.com") {
        val existing = localStorage.userProfile.value
        val langs = existing?.preferredLanguages ?: emptyList()
        val profile = UserProfile(
            id = authService.getCurrentUserId().ifBlank { "user_${System.currentTimeMillis()}" },
            displayName = displayName,
            email = email,
            photoUrl = authService.getCurrentUser()?.photoUrl?.toString(),
            isLoggedIn = true,
            preferredLanguages = langs
        )
        localStorage.saveUserProfile(profile)
        closeLoginDialog()
        if (langs.isEmpty()) {
            openLanguageDialog()
        } else {
            _preferredLanguages.value = langs
            loadHomeData()
        }
    }

    fun logoutUser() {
        authService.logout()
        localStorage.clearUserProfile()
        _preferredLanguages.value = emptyList()
        try {
            googleAuthHelper.signOut {
                localStorage.clearUserProfile()
            }
        } catch (_: Exception) {}
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
