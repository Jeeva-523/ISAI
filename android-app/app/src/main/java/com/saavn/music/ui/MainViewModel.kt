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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

import com.saavn.music.auth.GoogleAuthHelper
import com.saavn.music.data.analytics.AnalyticsService
import com.saavn.music.data.auth.AuthService
import com.saavn.music.data.model.UserProfile
import com.saavn.music.data.repository.FirestoreMusicService
import com.saavn.music.data.repository.PlaylistService
import com.saavn.music.data.search.SearchService
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

    // User Authentication Profile
    val userProfile: StateFlow<UserProfile?> = localStorage.userProfile

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    init {
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
            playCount = playCount
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
    val spotifyDailyMixes: StateFlow<List<SpotifyDailyMix>> = combine(_trendingSongs, _preferredLanguages) { songs, langs ->
        val primaryLang = (langs.firstOrNull() ?: "Tamil").uppercase()
        val anirudhSongs = songs.filter { 
            it.title.contains("anirudh", ignoreCase = true) || it.channelTitle.contains("anirudh", ignoreCase = true) 
        }
        val arrSongs = songs.filter { 
            it.title.contains("rahman", ignoreCase = true) || it.channelTitle.contains("rahman", ignoreCase = true) || it.channelTitle.contains("arr", ignoreCase = true) 
        }
        val yuvanSongs = songs.filter { 
            it.title.contains("yuvan", ignoreCase = true) || it.channelTitle.contains("yuvan", ignoreCase = true) || it.channelTitle.contains("u1", ignoreCase = true) 
        }

        listOf(
            SpotifyDailyMix(
                id = "daily_mix_1",
                title = "Daily Mix 1 • Anirudh Hits",
                subtitle = "Anirudh, Dhanush, Vijay & club anthems",
                coverUrl = anirudhSongs.firstOrNull()?.thumbnailUrl ?: "https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg",
                songs = if (anirudhSongs.isNotEmpty()) anirudhSongs else songs.take(8)
            ),
            SpotifyDailyMix(
                id = "daily_mix_2",
                title = "Daily Mix 2 • A.R. Rahman Soul",
                subtitle = "A.R. Rahman, Bombay Jayashri & timeless melodies",
                coverUrl = arrSongs.firstOrNull()?.thumbnailUrl ?: "https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg",
                songs = if (arrSongs.isNotEmpty()) arrSongs else songs.drop(1).take(8)
            ),
            SpotifyDailyMix(
                id = "daily_mix_3",
                title = "Daily Mix 3 • Yuvan Drug Melodies",
                subtitle = "Yuvan Shankar Raja, Harris & night drives",
                coverUrl = yuvanSongs.firstOrNull()?.thumbnailUrl ?: "https://c.saavncdn.com/276/Maari-2-Tamil-2018-20260203193952-500x500.jpg",
                songs = if (yuvanSongs.isNotEmpty()) yuvanSongs else songs.drop(2).take(8)
            ),
            SpotifyDailyMix(
                id = "daily_mix_4",
                title = "Top 50 • $primaryLang",
                subtitle = "The most played and trending hits in $primaryLang",
                coverUrl = songs.firstOrNull()?.thumbnailUrl ?: "https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg",
                songs = songs
            )
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

        // Initialize ISAI Connect with user profile
        viewModelScope.launch {
            userProfile.collect { profile ->
                val email = profile?.email?.ifBlank { null } 
                    ?: authService.getCurrentUser()?.email?.ifBlank { null } 
                    ?: "kongujeeva523@gmail.com"
                isaiConnectManager.initialize(email)
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

        // Sync local playback state to ISAI Connect when this device is active player
        viewModelScope.launch {
            ytPlayerController.currentSong.collect { song ->
                if (song != null && isaiConnectManager.isMyDeviceActive()) {
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
                if (isaiConnectManager.isMyDeviceActive()) {
                    isaiConnectManager.updatePlaybackState(queueIndex = idx)
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

        // Periodic position sync to ISAI Connect (every 1.5s while playing on this phone)
        viewModelScope.launch {
            while (isActive) {
                delay(1500)
                if (isaiConnectManager.isMyDeviceActive() && ytPlayerController.isPlaying.value) {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
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

                // Compute / Populate Popular Singers & Artists
                val trendingSvc = com.saavn.music.data.trending.TrendingService.getInstance(getApplication())
                val artists = trendingSvc.getPopularArtists(_trendingSongs.value)
                if (artists.isNotEmpty()) {
                    _popularArtists.value = artists
                }

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
        viewModelScope.launch {
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
        viewModelScope.launch {
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

    fun selectArtist(artistName: String) {
        val clean = artistName
            .replace("Topic", "", ignoreCase = true)
            .replace("VEVO", "", ignoreCase = true)
            .replace("Official", "", ignoreCase = true)
            .trim()
        val query = if (clean.isNotBlank()) "$clean Hits" else "Popular Hits"
        onSearchQueryChanged(query)
        setScreen(AppScreen.SEARCH)
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

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            delay(350)
            try {
                val clean = newQuery.trim().lowercase()

                // Smart Category / Keyword Router
                if (clean == "trending" || clean == "trends" || clean == "charts") {
                    _searchResults.value = _trendingSongs.value
                    _searchError.value = null
                    return@launch
                }

                // 1. Check for Tamil Gaana / Folk intent
                if (clean.contains("gana") || clean.contains("gaana")) {
                    android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Gaana intent detected for '$newQuery'")
                    val ganaArtists = listOf(
                        "Gana Bala", "Marana Gana Viji", "Gana Balachandar", "Vaathi Coming",
                        "Anthony Daasan", "Aathangara Orathil", "Danga Maari Oodhari", "Aaluma Doluma",
                        "Open the Tasmac", "Ora Kannala", "Marana Gaana"
                    )
                    val saavnSongsList = mutableListOf<YouTubeSong>()
                    for (q in ganaArtists.take(8)) {
                        val r = musicRepo.search(q).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                        saavnSongsList.addAll(r)
                    }
                    
                    val deduped = ytRepo.deduplicateSongs(saavnSongsList.distinctBy { it.videoId })
                    val sorted = deduped.sortedByDescending { calculateSongRelevance(it, newQuery) }
                    _searchResults.value = sorted
                    _searchError.value = null
                    return@launch
                }

                val activeLang = _searchLanguageFilter.value
                val langFilter = if (activeLang != "All") activeLang else ""

                val queryOverride = when (clean) {
                    "melody", "melodies" -> if (langFilter.isNotBlank()) "$langFilter melody hit songs" else "feel good melody hit songs"
                    "romantic", "romance", "love" -> if (langFilter.isNotBlank()) "$langFilter romantic love songs" else "love romantic songs"
                    "sad", "emotional" -> if (langFilter.isNotBlank()) "$langFilter sad emotional songs" else "sad emotional songs"
                    "party", "dance" -> if (langFilter.isNotBlank()) "$langFilter party dance songs" else "party dance songs"
                    "kuthu" -> "Tamil party kuthu mass dance songs"
                    "workout", "gym" -> "gym workout bgm beats"
                    "relax", "chill", "acoustic" -> if (langFilter.isNotBlank()) "$langFilter relaxing acoustic songs" else "relaxing acoustic melody songs"
                    "folk" -> if (langFilter.isNotBlank()) "$langFilter folk songs" else "folk songs"
                    "devotional", "god", "bhakti" -> if (langFilter.isNotBlank()) "$langFilter devotional songs" else "devotional songs"
                    "kadhal" -> "Tamil love romantic songs"
                    else -> {
                        if (langFilter.isNotBlank() && !clean.contains(langFilter.lowercase())) {
                            "${newQuery.trim()} $langFilter"
                        } else {
                            newQuery.trim()
                        }
                    }
                }

                // 2. Direct Audio Search (JioSaavn / NepoTune) - multi-lingual
                val saavnResult = musicRepo.search(queryOverride)
                val saavnSongs = saavnResult.getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()

                if (saavnSongs.isNotEmpty() && isSearchRelevant(newQuery, saavnSongs)) {
                    android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Direct Audio search success & relevant: ${saavnSongs.size} songs found for '$queryOverride'")
                    val sorted = saavnSongs.sortedByDescending { calculateSongRelevance(it, newQuery) }
                    _searchResults.value = ytRepo.deduplicateSongs(sorted)
                    _searchError.value = null
                } else {
                    // Fallback to YouTube Music (Multi-lingual & high precision)
                    android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Saavn results missing or irrelevant, querying YouTube Music for '$newQuery'")
                    val result = ytRepo.searchSongs(newQuery.trim())
                    val songs = result.getOrDefault(emptyList())
                    if (songs.isNotEmpty()) {
                        val sorted = songs.sortedByDescending { calculateSongRelevance(it, newQuery) }
                        _searchResults.value = ytRepo.deduplicateSongs(sorted)
                        _searchError.value = null
                    } else if (saavnSongs.isNotEmpty()) {
                        _searchResults.value = ytRepo.deduplicateSongs(saavnSongs)
                        _searchError.value = null
                    } else {
                        _searchResults.value = emptyList()
                        _searchError.value = "No songs found"
                    }
                }
            } catch (e: Exception) {
                try {
                    val result = ytRepo.searchSongs(newQuery)
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

        // Generate context-aware relevant playback queue (Intro songs get intro songs, Melodies get melodies)
        val isAdvancingInExistingQueue = !queue.isNullOrEmpty() && queue.any { it.videoId == song.videoId } && queue != _trendingSongs.value
        val effectiveQueue = if (isAdvancingInExistingQueue) {
            queue!!
        } else {
            val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
            val candidatePool = if (queue.isNullOrEmpty() || queue.size <= 1 || queue == _trendingSongs.value) {
                val suggestionsList = _suggestions.value.filter { it.videoId != song.videoId }
                val trendingList = _trendingSongs.value.filter { it.videoId != song.videoId }
                (listOf(song) + suggestionsList + trendingList).distinctBy { it.videoId }
            } else {
                queue
            }
            com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, candidatePool, activeLangs, 25)
        }

        // Asynchronously load matching suggestions for this specific mood/genre
        loadSuggestionsForSong(song)

        // Claim ISAI Connect active device status and sync song details and queue to Firebase
        isaiConnectManager.updatePlaybackState(
            song = song,
            isPlaying = true,
            positionMs = (startPositionSec * 1000).toLong(),
            currentDeviceId = isaiConnectManager.deviceId,
            queue = effectiveQueue,
            queueIndex = 0
        )

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
        ytPlayerController.removeFromQueue(index)
        if (isaiConnectManager.isMyDeviceActive()) {
            isaiConnectManager.updatePlaybackState(
                queue = ytPlayerController.playbackQueue.value,
                queueIndex = ytPlayerController.currentQueueIndex.value
            )
        }
    }

    fun clearQueue() {
        ytPlayerController.clearQueue()
        if (isaiConnectManager.isMyDeviceActive()) {
            isaiConnectManager.updatePlaybackState(
                queue = ytPlayerController.playbackQueue.value,
                queueIndex = 0
            )
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
                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Loading mood-targeted suggestions for query: '$query'")
                val directResult = musicRepo.search(query).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                val filteredDirect = directResult.filter { it.videoId != song.videoId }

                val relevantMatches = if (filteredDirect.isNotEmpty()) {
                    com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, filteredDirect, activeLangs, 20).filter { it.videoId != song.videoId }
                } else {
                    val ytResult = ytRepo.searchTamilSongs(query).getOrDefault(emptyList())
                    val filteredYt = ytResult.filter { it.videoId != song.videoId }
                    if (filteredYt.isNotEmpty()) {
                        com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, filteredYt, activeLangs, 20).filter { it.videoId != song.videoId }
                    } else {
                        com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, _trendingSongs.value, activeLangs, 20).filter { it.videoId != song.videoId }
                    }
                }

                _suggestions.value = relevantMatches

                // Immediately replace the generic upcoming queue with these mood and language matched songs!
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

                if (remainingInQueue <= 4 && song != null) {
                    val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                    val primaryLang = activeLangs.first()

                    val query = com.saavn.music.util.RelevanceEngine.getRelevantSearchQuery(song, primaryLang)
                    val primaryArtist = com.saavn.music.util.RelevanceEngine.extractPrimaryArtist(song.channelTitle)
                    val artistQuery = if (primaryArtist.isNotBlank()) "$primaryArtist $primaryLang hit songs" else "$primaryLang top trending songs"

                    val searchHits = async {
                        musicRepo.search(query).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    }
                    val artistHits = async {
                        musicRepo.search(artistQuery).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    }
                    val combinedHits = (searchHits.await() + artistHits.await())

                    val existingVideoIds = currentQueue.map { it.videoId }.toSet()
                    val existingNormTitles = currentQueue.map { 
                        it.title.lowercase().filter { ch: Char -> ch.isLetterOrDigit() }.take(15) 
                    }.toSet()

                    val newSongs = mutableListOf<YouTubeSong>()
                    val seenInBatch = mutableSetOf<String>()

                    for (candidate in combinedHits) {
                        if (candidate.videoId == song.videoId) continue
                        if (existingVideoIds.contains(candidate.videoId)) continue
                        val norm = candidate.title.lowercase().filter { ch: Char -> ch.isLetterOrDigit() }.take(15)
                        if (existingNormTitles.contains(norm) || seenInBatch.contains(norm)) continue
                        seenInBatch.add(norm)
                        newSongs.add(candidate)
                        if (newSongs.size >= 8) break
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
            val currentSuggestions = _suggestions.value
            val currentId = ytPlayerController.currentSong.value?.videoId
            val candidate = currentSuggestions.firstOrNull { it.videoId != currentId }
                ?: _trendingSongs.value.firstOrNull { it.videoId != currentId }

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
