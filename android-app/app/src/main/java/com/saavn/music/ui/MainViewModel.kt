package com.saavn.music.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.media.AudioManager
import android.os.Build
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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
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
import kotlinx.coroutines.withTimeoutOrNull

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
import com.saavn.music.util.RelevanceEngine
import com.saavn.music.util.SongMood
import com.saavn.music.data.repository.UpdateDownloadState

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
    val listenTogetherManager = com.saavn.music.connect.ListenTogetherManager.getInstance(application.applicationContext, ytPlayerController)
    val audioUrlCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    // Firebase & Discovery Services
    val analyticsService = AnalyticsService.getInstance(application.applicationContext)
    val trendingService = TrendingService.getInstance(application.applicationContext)
    val searchService = SearchService.getInstance(application.applicationContext)
    val authService = AuthService.getInstance(application.applicationContext)
    val playlistService = PlaylistService.getInstance(application.applicationContext)
    val firestoreMusicService = FirestoreMusicService.getInstance(application.applicationContext)
    val appUpdateService = AppUpdateService.getInstance(application.applicationContext)
    val dynamicUiService = com.saavn.music.data.repository.DynamicUiService.getInstance(application.applicationContext)

    // User Authentication Profile
    val userProfile: StateFlow<UserProfile?> = localStorage.userProfile

    // Subscription & Payment Management
    val subscriptionRepo = com.saavn.music.payment.SubscriptionRepository(application.applicationContext, localStorage)
    private val _isPaymentLoading = MutableStateFlow(false)
    val isPaymentLoading: StateFlow<Boolean> = _isPaymentLoading.asStateFlow()
    private val _paymentMessage = MutableStateFlow<String?>(null)
    val paymentMessage: StateFlow<String?> = _paymentMessage.asStateFlow()
    private var pendingPlanType: String = "MONTHLY"

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _showPlanSelectionDialog = MutableStateFlow(false)
    val showPlanSelectionDialog: StateFlow<Boolean> = _showPlanSelectionDialog.asStateFlow()

    fun openPlanSelectionDialog() {
        // Paid/subscription options hidden for now as per user preference
        _showPlanSelectionDialog.value = false
    }

    fun closePlanSelectionDialog() {
        _showPlanSelectionDialog.value = false
    }

    fun selectUserPlan(plan: String) {
        val isPrem = plan.equals("PREMIUM", ignoreCase = true)
        val current = localStorage.userProfile.value ?: UserProfile(
            id = authService.getCurrentUserId(),
            displayName = authService.getCurrentUser()?.displayName ?: "ISAI Listener",
            email = authService.getCurrentUser()?.email ?: "user@isaimusic.com",
            isLoggedIn = true
        )
        val updated = current.copy(
            isLoggedIn = true,
            isPremium = isPrem,
            selectedPlan = plan
        )
        localStorage.saveUserProfile(updated)
        closePlanSelectionDialog()

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val msg = if (isPrem) "👑 Welcome to ISAI Premium! VIP features unlocked." else "🎵 Welcome to ISAI Free! Unlimited ad-free listening."
            android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_SHORT).show()
        }

        if (updated.preferredLanguages.isEmpty()) {
            openLanguageDialog()
        } else {
            val normLangs = updated.preferredLanguages.map { com.saavn.music.util.RelevanceEngine.normalizeLanguage(it) }.distinct()
            _preferredLanguages.value = normLangs
            isaiConnectManager.syncPreferences(normLangs)
            loadHomeData()
        }
    }

    fun initiateSubscription(activity: android.app.Activity, planType: String) {
        val user = userProfile.value
        val userId = user?.id?.ifBlank { null } ?: authService.getCurrentUserId().ifBlank { "user_${System.currentTimeMillis()}" }
        val email = user?.email?.ifBlank { null } ?: authService.getCurrentUser()?.email ?: "user@isaimusic.com"
        val name = user?.displayName?.ifBlank { null } ?: authService.getCurrentUser()?.displayName ?: "ISAI Listener"

        pendingPlanType = planType
        _isPaymentLoading.value = true
        _paymentMessage.value = "Preparing checkout..."

        viewModelScope.launch {
            val orderResult = subscriptionRepo.createOrder(planType, userId, email, name)
            _isPaymentLoading.value = false
            orderResult.onSuccess { order ->
                val keyId = order.keyId ?: com.saavn.music.payment.PaymentConfig.RAZORPAY_KEY_ID
                val orderId = order.orderId ?: ""
                com.saavn.music.payment.RazorpayManager.openCheckout(
                    activity = activity,
                    keyId = keyId,
                    orderId = orderId,
                    amount = order.amount,
                    planType = planType,
                    userEmail = email,
                    userName = name
                )
            }
            orderResult.onFailure { err ->
                _paymentMessage.value = "Failed to create order: ${err.message}"
                android.widget.Toast.makeText(getApplication(), "Failed to create order: ${err.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handlePaymentSuccess(paymentData: com.razorpay.PaymentData) {
        val user = userProfile.value
        val userId = user?.id?.ifBlank { null } ?: authService.getCurrentUserId().ifBlank { "user_${System.currentTimeMillis()}" }
        val paymentId = paymentData.paymentId ?: ""
        val orderId = paymentData.orderId ?: ""
        val signature = paymentData.signature ?: ""

        _isPaymentLoading.value = true
        _paymentMessage.value = "Verifying payment with Razorpay..."

        viewModelScope.launch {
            val verifyReq = com.saavn.music.payment.VerifyPaymentApiRequest(
                razorpayPaymentId = paymentId,
                razorpayOrderId = orderId,
                razorpaySignature = signature,
                userId = userId,
                planType = pendingPlanType,
                userEmail = user?.email,
                userName = user?.displayName
            )

            val verifyRes = subscriptionRepo.verifyPayment(verifyReq)
            _isPaymentLoading.value = false

            verifyRes.onSuccess { _ ->
                closePlanSelectionDialog()
                _paymentMessage.value = "💎 Premium activated successfully!"
                android.widget.Toast.makeText(
                    getApplication(),
                    "💎 Premium activated successfully! Enjoy Ad-free music & all VIP features!",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                // Immediately stop AdMob ads
                com.saavn.music.ads.AdManager.clearCachedAds()
            }

            verifyRes.onFailure { err ->
                _paymentMessage.value = "Payment verification failed: ${err.message}"
                android.widget.Toast.makeText(
                    getApplication(),
                    "Payment verification failed: ${err.message}. You remain on ISAI Free.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun handlePaymentError(code: Int, response: String?, paymentData: com.razorpay.PaymentData?) {
        _isPaymentLoading.value = false
        _paymentMessage.value = "Payment cancelled or failed."
        android.util.Log.w("ISAI_PAYMENT", "Payment failed - code: $code, response: $response, data: $paymentData")
        android.widget.Toast.makeText(
            getApplication(),
            "Payment cancelled or failed. You remain on ISAI Free.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun cancelSubscription() {
        val user = userProfile.value ?: return
        viewModelScope.launch {
            val res = subscriptionRepo.cancelSubscription(user.id)
            res.onSuccess {
                android.widget.Toast.makeText(getApplication(), "Subscription cancelled.", android.widget.Toast.LENGTH_SHORT).show()
            }
            res.onFailure {
                android.widget.Toast.makeText(getApplication(), "Failed to cancel subscription.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // In-App Auto Update State
    private val _appUpdateInfo = MutableStateFlow<AppUpdateModel?>(null)
    val appUpdateInfo: StateFlow<AppUpdateModel?> = _appUpdateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateDownloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val updateDownloadState: StateFlow<UpdateDownloadState> = _updateDownloadState.asStateFlow()

    // Multi-Device Playback Mode: Separate (independent on 2+ devices) vs Sync (Spotify Connect)
    val isMultiDevicePlaybackSeparate: StateFlow<Boolean> = localStorage.isMultiDevicePlaybackSeparate

    fun setMultiDevicePlaybackSeparate(enabled: Boolean) {
        localStorage.setMultiDevicePlaybackSeparate(enabled)
        isaiConnectManager.syncMultiDeviceSeparate(enabled)
    }

    val dynamicUiConfig: StateFlow<com.saavn.music.data.model.DynamicUiConfig> = dynamicUiService.uiConfig

    // Spotify-like Recommendation Context & Session Seed
    var sessionSeedSong: YouTubeSong? = null
    var recommendationGeneration: Long = 0L

    init {
        // Synchronize initial device audio volume
        syncWithSystemVolume()

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
                            existing!!.preferredLanguages.map { com.saavn.music.util.RelevanceEngine.normalizeLanguage(it) }.distinct()
                        } else {
                            listOf("tamil")
                        }
                        val profile = UserProfile(
                            id = user.uid,
                            displayName = user.displayName ?: existing?.displayName ?: (user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "ISAI Listener"),
                            email = user.email ?: existing?.email ?: "",
                            photoUrl = user.photoUrl?.toString() ?: existing?.photoUrl,
                            isLoggedIn = true,
                            preferredLanguages = langs,
                            isPremium = existing?.isPremium ?: false,
                            selectedPlan = existing?.selectedPlan ?: "FREE",
                            subscriptionStatus = existing?.subscriptionStatus ?: "FREE",
                            planType = existing?.planType ?: "FREE",
                            subscriptionStart = existing?.subscriptionStart ?: 0L,
                            subscriptionExpiry = existing?.subscriptionExpiry ?: 0L,
                            isTester = existing?.isTester ?: false,
                            updateChannel = existing?.updateChannel ?: "STABLE"
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
                    _preferredLanguages.value = existing.preferredLanguages.map { com.saavn.music.util.RelevanceEngine.normalizeLanguage(it) }.distinct()
                }
            }
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            val update = appUpdateService.checkForUpdate(userProfile.value)
            if (update != null) {
                _appUpdateInfo.value = update
            }
        }
        appUpdateService.listenForRealtimeUpdates(getUser = { userProfile.value }) { update ->
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
                val update = appUpdateService.checkForUpdate(userProfile.value)
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

    fun setTesterMode(enabled: Boolean) {
        val current = userProfile.value ?: return
        val updated = current.copy(
            isTester = enabled,
            updateChannel = if (enabled) "BETA" else "STABLE"
        )
        localStorage.saveUserProfile(updated)

        // Sync tester status to Firebase RTDB
        try {
            if (current.id.isNotBlank()) {
                val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance()
                rtdb.getReference("users").child(current.id).child("isTester").setValue(enabled)
                rtdb.getReference("users").child(current.id).child("updateChannel").setValue(if (enabled) "BETA" else "STABLE")
            }
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "Could not sync tester status to RTDB: ${e.message}")
        }

        android.widget.Toast.makeText(
            getApplication(),
            if (enabled) "🧪 Beta Tester Mode Enabled! Early tester updates unlocked." else "Switched to Stable Channel.",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        // Immediately re-evaluate update availability with new tester status
        checkForAppUpdates()
    }

    fun launchAppUpdate(context: android.content.Context) {
        val update = _appUpdateInfo.value ?: return
        viewModelScope.launch {
            appUpdateService.downloadAndInstallApk(
                context = context,
                rawUrl = update.downloadUrl,
                onStateChange = { state ->
                    _updateDownloadState.value = state
                }
            )
        }
    }

    private fun SongItem.toYouTubeSong(): YouTubeSong {
        val displayChannel = if (albumName.isNotBlank() && !artistNames.contains(albumName, ignoreCase = true)) {
            if (artistNames.isNotBlank()) "$artistNames • $albumName" else albumName
        } else {
            artistNames.ifBlank { subtitle }
        }
        return YouTubeSong(
            videoId = id,
            title = title,
            channelTitle = displayChannel,
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

    // Home Categorized Songs (Pre-seeded with CURATED_TAMIL_SONGS so Home is never empty)
    private val _trendingSongs = MutableStateFlow<List<YouTubeSong>>(YouTubeMusicRepository.CURATED_TAMIL_SONGS)
    val trendingSongs: StateFlow<List<YouTubeSong>> = _trendingSongs.asStateFlow()

    private val _popularArtists = MutableStateFlow<List<com.saavn.music.data.trending.TopArtistData>>(emptyList())
    val popularArtists: StateFlow<List<com.saavn.music.data.trending.TopArtistData>> = _popularArtists.asStateFlow()

    private val _categorySongs = MutableStateFlow<List<YouTubeSong>>(YouTubeMusicRepository.CURATED_TAMIL_SONGS)
    val categorySongs: StateFlow<List<YouTubeSong>> = _categorySongs.asStateFlow()

    private val _latestReleases = MutableStateFlow<List<YouTubeSong>>(YouTubeMusicRepository.CURATED_TAMIL_SONGS.take(35))
    val latestReleases: StateFlow<List<YouTubeSong>> = _latestReleases.asStateFlow()

    private val _picksSongs = MutableStateFlow<List<YouTubeSong>>(YouTubeMusicRepository.CURATED_TAMIL_SONGS.take(35))
    val picksSongs: StateFlow<List<YouTubeSong>> = _picksSongs.asStateFlow()

    private val _mostPlayedSongs = MutableStateFlow<List<YouTubeSong>>(YouTubeMusicRepository.CURATED_TAMIL_SONGS.take(35))
    val mostPlayedSongs: StateFlow<List<YouTubeSong>> = _mostPlayedSongs.asStateFlow()

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

    // Preferred Music Languages (Always default to "tamil" so user immediately gets Tamil feed)
    private val _preferredLanguages = MutableStateFlow<List<String>>(
        localStorage.userProfile.value?.preferredLanguages?.map { com.saavn.music.util.RelevanceEngine.normalizeLanguage(it) }?.distinct()?.ifEmpty { listOf("tamil") } ?: listOf("tamil")
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

    // App Theme Controls
    val appThemeMode: StateFlow<com.saavn.music.ui.theme.AppThemeMode> = localStorage.appThemeMode

    fun setAppTheme(mode: com.saavn.music.ui.theme.AppThemeMode) {
        localStorage.setAppThemeMode(mode)
    }

    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog: StateFlow<Boolean> = _showThemeDialog.asStateFlow()

    fun openThemeDialog() {
        _showThemeDialog.value = true
    }

    fun closeThemeDialog() {
        _showThemeDialog.value = false
    }

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
        // Pre-fill audio cache with curated Tamil songs for instant 0ms playback
        YouTubeMusicRepository.CURATED_TAMIL_SONGS.forEach { curated ->
            if (!curated.audioUrl.isNullOrBlank()) {
                audioUrlCache[curated.videoId] = curated.audioUrl
            }
        }

        loadHomeData()
        loadPersonalizedRecommendations()
        ytPlayerController.onQueueExhausted = {
            playNextAutoSuggestion()
        }
        ytPlayerController.onTrackChangeRequested = { nextSong, queue ->
            playSong(nextSong, queue, openFullPlayer = false)
        }
        ytPlayerController.onPlaybackErrorFallback = { failedSong ->
            audioUrlCache.remove(failedSong.videoId)
            android.util.Log.w("ISAI_PLAYER", "[MainViewModel] Playback failed for: '${failedSong.title}', attempting live stream re-resolution...")
            viewModelScope.launch(Dispatchers.IO) {
                val cleanTitle = failedSong.title
                    .replace(Regex("\\s*[|•].*$"), "")
                    .replace(Regex("\\s*\\(.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*\\[.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\]", RegexOption.IGNORE_CASE), "")
                    .trim()
                val targetLang = failedSong.language.ifBlank { "tamil" }
                val freshUrl = musicRepo.resolveStreamUrl(cleanTitle.ifBlank { failedSong.title }, failedSong.channelTitle, targetLanguage = targetLang)
                withContext(Dispatchers.Main) {
                    if (!freshUrl.isNullOrBlank() && freshUrl != failedSong.audioUrl) {
                        audioUrlCache[failedSong.videoId] = freshUrl
                        val recovered = failedSong.copy(audioUrl = freshUrl)
                        playSong(recovered, openFullPlayer = false)
                    } else {
                        ytPlayerController.playNext()
                    }
                }
            }
        }
        ytPlayerController.isPlaybackRestricted = {
            listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()
        }
        ytPlayerController.onAudioFocusLostOrCall = {
            if (listenTogetherManager.isHost() && ytPlayerController.isPlaying.value) {
                val curPos = ytPlayerController.getLivePositionSec()
                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Host received call or audio interruption! Pausing room across all joined devices.")
                listenTogetherManager.hostPause(curPos)
                ytPlayerController.pause()
            }
        }
        listenTogetherManager.onGuestPlaySongRequested = { guestSong, startPos, shouldPlay ->
            playSong(
                song = guestSong,
                startPositionSec = startPos,
                openFullPlayer = false,
                forceLocal = true,
                autoPlay = shouldPlay
            )
        }
        viewModelScope.launch {
            localStorage.recentlyPlayed.collect { list ->
                if (list.isNotEmpty()) {
                    isaiConnectManager.syncRecentlyPlayed(list)
                }
                // Only trigger initial recommendation load if list is currently empty to prevent reshuffling on every track change
                if (_personalizedRecommendations.value.isEmpty()) {
                    loadPersonalizedRecommendations()
                }
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
                    if (_personalizedRecommendations.value.isEmpty()) {
                        loadPersonalizedRecommendations()
                    }
                }
            }
        }

        viewModelScope.launch {
            isaiConnectManager.syncedPreferences.collect { remoteLangs ->
                val normRemote = remoteLangs.map { com.saavn.music.util.RelevanceEngine.normalizeLanguage(it) }.distinct()
                if (normRemote.isNotEmpty() && normRemote != _preferredLanguages.value) {
                    _preferredLanguages.value = normRemote
                    val currentProfile = localStorage.userProfile.value
                    if (currentProfile != null) {
                        localStorage.saveUserProfile(currentProfile.copy(preferredLanguages = normRemote))
                    }
                    loadHomeData()
                }
            }
        }

        viewModelScope.launch {
            isaiConnectManager.syncedMultiDeviceSeparate.collect { isSeparate ->
                if (isSeparate != null && isSeparate != localStorage.isMultiDevicePlaybackSeparate.value) {
                    localStorage.setMultiDevicePlaybackSeparate(isSeparate)
                }
            }
        }

        viewModelScope.launch {
            isaiConnectManager.syncedHomeFeed.collect { remoteFeed ->
                if (remoteFeed != null && remoteFeed.revision > 0L) {
                    val activeLang = _preferredLanguages.value.firstOrNull()?.lowercase() ?: "tamil"
                    val activeLangs = listOf(activeLang)
                    if (remoteFeed.language.equals(activeLang, ignoreCase = true)) {
                        val validPicks = remoteFeed.picksForYou.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
                        if (validPicks.isNotEmpty()) _picksSongs.value = validPicks
                        val validNew = remoteFeed.newReleases.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
                        if (validNew.isNotEmpty()) _latestReleases.value = validNew
                        val validTrending = remoteFeed.trending.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
                        if (validTrending.isNotEmpty()) {
                            _trendingSongs.value = validTrending
                            _categorySongs.value = validTrending
                        }
                        val validMostPlayed = remoteFeed.mostPlayed.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
                        if (validMostPlayed.isNotEmpty()) _mostPlayedSongs.value = validMostPlayed
                    }
                }
            }
        }

        viewModelScope.launch {
            isaiConnectManager.syncedHomeSongs.collect { remoteHome ->
                if (remoteHome.isNotEmpty() && _trendingSongs.value.isEmpty()) {
                    _trendingSongs.value = remoteHome
                    _categorySongs.value = remoteHome
                    _mostPlayedSongs.value = remoteHome
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
                isaiConnectManager.syncMultiDeviceSeparate(localStorage.isMultiDevicePlaybackSeparate.value)
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
                            videoId = syncState.currentSongId,
                            title = if (syncState.currentTitle.isNotBlank()) syncState.currentTitle else "Remote Track",
                            channelTitle = "${if (syncState.currentArtist.isNotBlank()) syncState.currentArtist else "ISAI"} • Web 💻",
                            thumbnailUrl = syncState.currentArtwork
                        )
                        com.saavn.music.service.MusicPlaybackService.startOrUpdate(
                            context = application.applicationContext,
                            song = remoteSong,
                            isPlaying = syncState.isPlaying,
                            durationSec = (syncState.durationMs / 1000f).coerceAtLeast(0f),
                            positionSec = (syncState.positionMs / 1000f).coerceAtLeast(0f)
                        )
                    }

                    if (!isaiConnectManager.isMyDeviceActive() && syncState.currentDeviceId == isaiConnectManager.deviceId && syncState.updatedByDeviceId != isaiConnectManager.deviceId) {
                        if (syncState.isPlaying && !ytPlayerController.isPlaying.value) {
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
                        "ADD_TO_QUEUE", "PLAY_NEXT_IN_QUEUE" -> {
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
                                    queueIndex = ytPlayerController.currentQueueIndex.value,
                                    currentDeviceId = isaiConnectManager.deviceId
                                )
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    android.widget.Toast.makeText(getApplication(), "Added next in queue from Web 🌐", android.widget.Toast.LENGTH_SHORT).show()
                                }
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

        // Seamlessly restore and resume the last played song from previous app session
        viewModelScope.launch(Dispatchers.Main) {
            delay(400)
            if (ytPlayerController.currentSong.value == null) {
                restoreLastPlaybackSession()
            }
        }
    }

    private fun restoreLastPlaybackSession() {
        val session = localStorage.lastPlaybackSession.value ?: return
        val song = session.song
        if (song.videoId.isBlank()) return

        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Restoring last playback session: '${song.title}' at ${session.positionSec}s (wasPlaying=${session.wasPlaying})")
        val queue = if (session.queue.isNotEmpty()) session.queue else listOf(song)

        ytPlayerController.prepareForPlayback(song, queue)
        ytPlayerController.seekTo(session.positionSec)

        playSong(
            song = song,
            queue = queue,
            startPositionSec = session.positionSec,
            openFullPlayer = false,
            forceLocal = true,
            autoPlay = session.wasPlaying
        )
    }

    // Anchor tracking to keep recommended stable and prevent jitter
    private var lastRecommendedAnchor: String = ""
    private var lastRecommendedFetchTime: Long = 0L

    fun loadPersonalizedRecommendations(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && _personalizedRecommendations.value.isNotEmpty() && (now - lastRecommendedFetchTime < 15 * 60 * 1000L)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songPlayCounts = localStorage.songPlayCounts.value
                val artistPlayCounts = localStorage.artistPlayCounts.value
                val recentList = (localStorage.recentlyPlayed.value + isaiConnectManager.syncedRecentlyPlayed.value).distinctBy { it.videoId }
                val favList = localStorage.favorites.value
                val allUserSongs = (recentList + favList).distinctBy { it.videoId }

                if (allUserSongs.isNotEmpty()) {
                    // Score each song based on actual listening count + favorites (+6) + recency bonus
                    val scoredSongs = allUserSongs.map { song ->
                        val playCount = songPlayCounts[song.videoId] ?: 0
                        val isFav = favList.any { it.videoId == song.videoId }
                        val recencyIndex = recentList.indexOfFirst { it.videoId == song.videoId }
                        val recencyBonus = if (recencyIndex >= 0) maxOf(0, 10 - recencyIndex) else 0
                        val score = (playCount * 3) + (if (isFav) 6 else 0) + recencyBonus
                        song to score
                    }.sortedByDescending { it.second }

                    val topSong = scoredSongs.firstOrNull()?.first

                    // Aggregate artist affinity from artist play counts + song scores
                    val aggregatedArtistScores = mutableMapOf<String, Int>()
                    for ((art, cnt) in artistPlayCounts) {
                        aggregatedArtistScores[art] = cnt * 3
                    }
                    scoredSongs.forEach { (song, score) ->
                        val art = song.channelTitle
                            .replace(" - Topic", "")
                            .replace(" Official", "")
                            .split("•").first()
                            .split(",").first()
                            .split("&").first()
                            .trim()
                        if (art.isNotBlank() && art != "Tamil Artist" && art != "Tamil Music") {
                            aggregatedArtistScores[art] = (aggregatedArtistScores[art] ?: 0) + score
                        }
                    }

                    val topArtist = aggregatedArtistScores.maxByOrNull { it.value }?.key
                    val currentAnchor = "$topArtist|${topSong?.videoId}"

                    if (!force && currentAnchor == lastRecommendedAnchor && _personalizedRecommendations.value.isNotEmpty()) {
                        return@launch
                    }

                    if (topSong != null || !topArtist.isNullOrBlank()) {
                        val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                        val dominantMood = topSong?.let { RelevanceEngine.detectSongMood(it) } ?: SongMood.MELODY_ROMANCE

                        val searchTarget = if (!topArtist.isNullOrBlank()) {
                            "$topArtist ${activeLangs.first()} songs"
                        } else {
                            RelevanceEngine.getRelevantSearchQuery(topSong!!, activeLangs.first())
                        }

                        if (!topArtist.isNullOrBlank()) {
                            _recommendedReason.value = "Because you frequently listen to $topArtist"
                        } else {
                            _recommendedReason.value = "Based on your most played songs"
                        }

                        val saavnResult = musicRepo.search(searchTarget, limit = 40)
                        val rawRecSongs = saavnResult.getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()

                        // Also fetch related songs for topSong if available
                        val extraRelated = if (topSong != null) {
                            val relQ = RelevanceEngine.getRelevantSearchQuery(topSong, activeLangs.first())
                            if (relQ != searchTarget) {
                                musicRepo.search(relQ, limit = 20).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                            } else emptyList()
                        } else emptyList()

                        val combinedCandidates = ytRepo.deduplicateSongs(rawRecSongs + extraRelated)
                        if (combinedCandidates.isNotEmpty()) {
                            val validCombined = combinedCandidates.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }
                            // Filter candidate songs by relevance to user's most listened songs and mood
                            val filtered = validCombined.filter { candidate ->
                                val alreadyListened = allUserSongs.any { ytRepo.isSameSong(it, candidate) }
                                if (alreadyListened) return@filter false

                                if (topSong != null) {
                                    RelevanceEngine.scoreSongRelevance(topSong, candidate, activeLangs) > 0
                                } else {
                                    RelevanceEngine.detectSongMood(candidate) == dominantMood
                                }
                            }

                            val finalRecs = if (filtered.size >= 6) filtered else validCombined.filterNot { c ->
                                allUserSongs.any { ytRepo.isSameSong(it, c) }
                            }

                            if (finalRecs.isNotEmpty()) {
                                _personalizedRecommendations.value = finalRecs.take(35)
                                lastRecommendedAnchor = currentAnchor
                                lastRecommendedFetchTime = now
                                return@launch
                            }
                        }
                    }
                }

                // Default recommendation mix for first-time users
                _recommendedReason.value = "✨ Top Picks For You"
                val langs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                val defaultRec = musicRepo.getTrending(langs).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                val validDefault = defaultRec.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                val dedupedRec = ytRepo.deduplicateSongs(validDefault)
                val heroExclude = _trendingSongs.value.take(4).map { it.videoId }.toSet()
                val distinctRec = dedupedRec.filterNot { heroExclude.contains(it.videoId) }
                val recs = if (distinctRec.isNotEmpty()) distinctRec.take(35) else dedupedRec.take(35)
                val curatedFallback = if (langs.contains("tamil")) YouTubeMusicRepository.CURATED_TAMIL_SONGS else emptyList()
                _personalizedRecommendations.value = (recs + curatedFallback).distinctBy { it.videoId }.take(35)
                lastRecommendedFetchTime = now
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
                val langFilteredSaavn = saavnSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                val cleanSaavn = langFilteredSaavn.filterNot { s ->
                    val t = s.title.lowercase()
                    t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                }
                val tamilFallback = if (langs.contains("tamil")) YouTubeMusicRepository.CURATED_TAMIL_SONGS else emptyList()
                val mostPlayed: List<YouTubeSong>
                if (cleanSaavn.isNotEmpty()) {
                    val deduped = ytRepo.deduplicateSongs(cleanSaavn)
                    val pool = (deduped + tamilFallback).distinctBy { it.videoId }
                    mostPlayed = trendingService.getMostPlayedSongs(pool).filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }.take(40)
                    _trendingSongs.value = mostPlayed
                    _categorySongs.value = mostPlayed
                    _mostPlayedSongs.value = mostPlayed
                    isaiConnectManager.syncHomeSongs(mostPlayed)
                } else {
                    val trending = ytRepo.getTrendingSongs(langs).filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }.filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    val deduped = ytRepo.deduplicateSongs(trending)
                    val pool = (deduped + tamilFallback).distinctBy { it.videoId }
                    mostPlayed = trendingService.getMostPlayedSongs(pool).filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }.take(40)
                    _trendingSongs.value = mostPlayed
                    _categorySongs.value = mostPlayed
                    _mostPlayedSongs.value = mostPlayed
                    isaiConnectManager.syncHomeSongs(mostPlayed)
                }

                // Pre-warm audio URLs in the background for top home songs for instant playback (0ms on click)
                viewModelScope.launch(Dispatchers.IO) {
                    val songsToPreWarm = mostPlayed.take(20)
                    for (s in songsToPreWarm) {
                        if (!audioUrlCache.containsKey(s.videoId)) {
                            val matchedCurated = YouTubeMusicRepository.CURATED_TAMIL_SONGS.firstOrNull {
                                it.videoId == s.videoId || it.title.equals(s.title, ignoreCase = true) ||
                                (s.title.length >= 4 && it.title.contains(s.title, ignoreCase = true)) ||
                                (it.title.length >= 4 && s.title.contains(it.title, ignoreCase = true))
                            }?.audioUrl
                            if (!matchedCurated.isNullOrBlank()) {
                                audioUrlCache[s.videoId] = matchedCurated
                            } else if (s.audioUrl.isNullOrBlank()) {
                                val clean = s.title.replace(Regex("\\s*[|•].*$"), "").trim()
                                val url = musicRepo.resolveStreamUrl(clean, s.channelTitle, targetLanguage = langs.firstOrNull() ?: "tamil")
                                if (!url.isNullOrBlank()) {
                                    audioUrlCache[s.videoId] = url
                                }
                            } else {
                                audioUrlCache[s.videoId] = s.audioUrl
                            }
                        }
                    }
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
                val primaryLang = langs.first().lowercase().trim()
                try {
                    val releaseQueries = if (primaryLang == "tamil") {
                        listOf(
                            "Latest Tamil Movie Songs 2026",
                            "Latest Tamil Singles 2026",
                            "Latest Tamil Hits 2026",
                            "Tamil Top Hits 2025 2026"
                        )
                    } else {
                        listOf(
                            "Latest $primaryLang Hits",
                            "Latest $primaryLang Movie Songs"
                        )
                    }
                    val deferredReleases = releaseQueries.map { q ->
                        async {
                            musicRepo.search(q, limit = 20).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                        }
                    }
                    val newSongsPool: MutableList<YouTubeSong> = deferredReleases.awaitAll().flatten().toMutableList()
                    if (newSongsPool.isEmpty()) {
                        val fallbackNew = ytRepo.getNewReleases()
                        newSongsPool.addAll(fallbackNew)
                    }
                    val cleanNew = newSongsPool.filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("jukebox") || t.contains("full album") || t.contains("non stop") || t.contains("all time hits") || t.contains("video jukebox")
                    }.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }

                    val dedupedNew = ytRepo.deduplicateSongs(cleanNew)
                    val poolTrending = _trendingSongs.value.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                    _latestReleases.value = (dedupedNew + poolTrending.drop(5) + tamilFallback).distinctBy { it.videoId }.take(35)
                } catch (_: Exception) {
                    if (_latestReleases.value.isEmpty()) {
                        val poolTrending = _trendingSongs.value.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                        _latestReleases.value = (poolTrending.drop(5) + tamilFallback).distinctBy { it.videoId }.take(35)
                    }
                }

                // Refresh recommendations on home reload
                loadPersonalizedRecommendations(force = true)
                val validRecs = _personalizedRecommendations.value.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                val poolTrending = _trendingSongs.value.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                val picks = (validRecs + poolTrending + tamilFallback).distinctBy { it.videoId }.take(35)
                _picksSongs.value = picks

                // Sync unified authoritative feed revision to Firebase RTDB for web & mobile consistency
                val authoritativeFeed = com.saavn.music.connect.HomeFeedData(
                    revision = System.currentTimeMillis(),
                    language = primaryLang,
                    updatedAt = System.currentTimeMillis(),
                    generatedBy = "android",
                    picksForYou = picks,
                    newReleases = _latestReleases.value,
                    trending = _trendingSongs.value,
                    mostPlayed = _mostPlayedSongs.value
                )
                isaiConnectManager.syncHomeFeed(authoritativeFeed)
            } catch (e: Exception) {
                try {
                    val langs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                    val tamilFallback = if (langs.contains("tamil")) YouTubeMusicRepository.CURATED_TAMIL_SONGS else emptyList()
                    val trending = ytRepo.getTrendingSongs(langs).filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    val deduped = ytRepo.deduplicateSongs(trending)
                    val pool = (deduped + tamilFallback).distinctBy { it.videoId }
                    val mostPlayed = trendingService.getMostPlayedSongs(pool).take(40)
                    _trendingSongs.value = mostPlayed
                    _categorySongs.value = mostPlayed
                    _mostPlayedSongs.value = mostPlayed
                    _picksSongs.value = pool.take(35)
                    _latestReleases.value = (pool.drop(5) + tamilFallback).distinctBy { it.videoId }.take(35)
                } catch (_: Exception) {
                    val fallback = if (_preferredLanguages.value.any { it != "tamil" }) emptyList() else YouTubeMusicRepository.CURATED_TAMIL_SONGS
                    _trendingSongs.value = fallback
                    _categorySongs.value = fallback
                    _mostPlayedSongs.value = fallback
                    _picksSongs.value = fallback.take(10)
                    _latestReleases.value = fallback.drop(4).take(12)
                }
            } finally {
                _isLoadingHome.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = category
        setScreen(AppScreen.SEARCH)
        onSearchQueryChanged(category)
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

    fun refreshMostPlayed() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingHome.value = true
            try {
                val langs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                val primaryLang = langs.first().lowercase().trim()

                val queries = listOf(
                    "Top $primaryLang hits 2025 2026",
                    "Latest $primaryLang super hit songs",
                    "Most played $primaryLang songs 2026",
                    "$primaryLang chartbusters top 50"
                )
                val randomQuery = queries.random()
                val saavnHits = musicRepo.search(randomQuery, limit = 50).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                val ytHits = ytRepo.searchSongs(randomQuery, maxResults = 50).getOrDefault(emptyList())

                val combined = (saavnHits + ytHits + _trendingSongs.value)
                    .filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("jukebox") || t.contains("full album") || t.contains("non stop") || t.contains("all time hits")
                    }

                val filteredByLang = combined.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                val deduped = ytRepo.deduplicateSongs(if (filteredByLang.isNotEmpty()) filteredByLang else combined)
                val fullPool = (deduped + YouTubeMusicRepository.CURATED_TAMIL_SONGS).distinctBy { it.videoId }
                val shuffledFresh = fullPool.shuffled()
                val reRanked = trendingService.getMostPlayedSongs(shuffledFresh).take(40)

                if (reRanked.isNotEmpty()) {
                    _trendingSongs.value = reRanked
                    _categorySongs.value = reRanked
                    _mostPlayedSongs.value = reRanked
                    isaiConnectManager.syncHomeSongs(reRanked)
                    val authoritativeFeed = com.saavn.music.connect.HomeFeedData(
                        revision = System.currentTimeMillis(),
                        language = primaryLang,
                        updatedAt = System.currentTimeMillis(),
                        generatedBy = "android",
                        picksForYou = _picksSongs.value,
                        newReleases = _latestReleases.value,
                        trending = reRanked,
                        mostPlayed = reRanked
                    )
                    isaiConnectManager.syncHomeFeed(authoritativeFeed)
                }
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYER", "[MainViewModel] refreshMostPlayed error: ${e.message}")
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

                val deferredMixes = mixConfigs.map { (info, query) ->
                    async {
                        val (id, title, subtitle) = info
                        val rawSongs = musicRepo.search(query, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                        val dedupedSongs = ytRepo.deduplicateSongs(rawSongs).take(30)
                        val cover = dedupedSongs.firstOrNull()?.thumbnailUrl ?: "https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg"
                        if (dedupedSongs.isNotEmpty()) {
                            SpotifyDailyMix(
                                id = id,
                                title = title,
                                subtitle = "$subtitle • ${dedupedSongs.size} Songs",
                                coverUrl = cover,
                                songs = dedupedSongs
                            )
                        } else null
                    }
                }
                val loadedMixes = deferredMixes.awaitAll().filterNotNull()

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
                        val mainResults = ytRepo.searchSongs(parsedIntent.youtubeMusicQuery, maxResults = 50).getOrDefault(emptyList())
                        val hasSongsWord = parsedIntent.youtubeMusicQuery.lowercase(java.util.Locale.ROOT).let {
                            it.contains("song") || it.contains("paatu") || it.contains("paadal")
                        }
                        if (!hasSongsWord && parsedIntent.unmatchedTerms.isNotEmpty()) {
                            val extraQuery = "${parsedIntent.youtubeMusicQuery} songs"
                            val movieSongs = ytRepo.searchSongs(extraQuery, maxResults = 30).getOrDefault(emptyList())
                            (mainResults + movieSongs).distinctBy { it.videoId }
                        } else {
                            mainResults
                        }
                    }
                    val saavnDeferred = async {
                        if (parsedIntent.directAudioQuery.isNotBlank()) {
                            val mainSaavn = musicRepo.search(parsedIntent.directAudioQuery, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                            val hasSongsWord = parsedIntent.directAudioQuery.lowercase(java.util.Locale.ROOT).let {
                                it.contains("song") || it.contains("paatu") || it.contains("paadal")
                            }
                            if (!hasSongsWord && parsedIntent.unmatchedTerms.isNotEmpty()) {
                                val extraQuery = "${parsedIntent.directAudioQuery} songs"
                                val movieSongs = musicRepo.search(extraQuery, limit = 25).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                                (mainSaavn + movieSongs).distinctBy { it.videoId }
                            } else {
                                mainSaavn
                            }
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

                    val nonPlaylistRanked = ranked.filter { !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) }

                    val filteredByLang = nonPlaylistRanked.filter { song ->
                        com.saavn.music.util.RelevanceEngine.isSongInLanguage(song, userLangs)
                    }

                    val searchLang = if (activeLangFilter != "All") {
                        activeLangFilter.lowercase()
                    } else if (!parsedIntent.detectedLanguage.isNullOrBlank() && parsedIntent.detectedLanguage != "unknown") {
                        parsedIntent.detectedLanguage.lowercase()
                    } else ""

                    val rawResults = if (filteredByLang.isNotEmpty()) filteredByLang else nonPlaylistRanked
                    val finalResults = if (searchLang.isNotBlank()) {
                        rawResults.map { s -> if (s.language.isBlank()) s.copy(language = searchLang) else s }
                    } else {
                        rawResults
                    }
                    _searchResults.value = finalResults
                    _searchError.value = null
                } else {
                    _searchResults.value = emptyList()
                    _searchError.value = "No songs found for '$newQuery'"
                }
            } catch (e: Exception) {
                try {
                    val result = ytRepo.searchSongs(newQuery)
                    _searchResults.value = result.getOrDefault(emptyList()).filter { !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) }
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
        forceLocal: Boolean = false,
        autoPlay: Boolean = true
    ) {
        // Listen Together Host/Listener Access Check
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost() && !forceLocal) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Only Room Host ($hostName) can change songs", android.widget.Toast.LENGTH_SHORT).show()
            }
            if (openFullPlayer) {
                _showFullPlayer.value = true
            }
            return
        }

        val remoteState = isaiConnectManager.playbackState.value
        val isRemoteActive = !forceLocal && !localStorage.isMultiDevicePlaybackSeparate.value &&
                remoteState != null &&
                remoteState.currentDeviceId.isNotBlank() &&
                remoteState.currentDeviceId != isaiConnectManager.deviceId &&
                remoteState.isPlaying &&
                (System.currentTimeMillis() - remoteState.updatedAt) < 30_000L

        if (isRemoteActive && remoteState != null) {
            android.util.Log.i("ISAI_CONNECT", "[MainViewModel] Remote is active on ${remoteState.currentDeviceId}. Routing song to Web: ${song.title}")
            isaiConnectManager.sendCommand(
                action = "PLAY_SONG",
                song = song,
                positionMs = (startPositionSec * 1000).toLong(),
                targetDeviceId = remoteState.currentDeviceId
            )
            isaiConnectManager.updatePlaybackState(
                song = song,
                isPlaying = true,
                positionMs = (startPositionSec * 1000).toLong(),
                currentDeviceId = remoteState.currentDeviceId
            )
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val devName = if (remoteState.currentDeviceId.contains("web", ignoreCase = true)) "Web 💻" else "Remote Device"
                android.widget.Toast.makeText(getApplication(), "Playing on $devName: ${song.title.take(25)}...", android.widget.Toast.LENGTH_SHORT).show()
            }
            if (openFullPlayer) {
                _showFullPlayer.value = true
            }
            return
        }

        // Claim ISAI Connect active device status for this phone
        isaiConnectManager.transferPlaybackToDevice(
            targetDeviceId = isaiConnectManager.deviceId,
            song = song,
            positionMs = (startPositionSec * 1000).toLong(),
            isPlaying = true
        )

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

        // Instant in-memory audio match check (0ms overhead)
        val inMemoryAudioUrl = song.audioUrl?.ifBlank { null }
            ?: audioUrlCache[song.videoId]
            ?: run {
                YouTubeMusicRepository.CURATED_TAMIL_SONGS.firstOrNull { curated ->
                    curated.videoId == song.videoId ||
                    curated.title.equals(song.title, ignoreCase = true) ||
                    (song.title.length >= 4 && curated.title.contains(song.title, ignoreCase = true)) ||
                    (curated.title.length >= 4 && song.title.contains(curated.title, ignoreCase = true)) ||
                    com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(curated, song)
                }?.audioUrl
            }

        if (!inMemoryAudioUrl.isNullOrBlank()) {
            audioUrlCache[song.videoId] = inMemoryAudioUrl
        }
        val immediateSong = if (!inMemoryAudioUrl.isNullOrBlank()) song.copy(audioUrl = inMemoryAudioUrl) else song

        // Fast initial queue so player starts with 0ms UI delay
        val isAdvancingInCurrentPlaybackQueue = !queue.isNullOrEmpty() &&
            queue == ytPlayerController.playbackQueue.value &&
            queue.any { it.videoId == song.videoId }

        val targetSongLang = song.language.ifBlank { com.saavn.music.util.RelevanceEngine.detectSongLanguage(song) }.ifBlank { _preferredLanguages.value.firstOrNull() ?: "tamil" }
        val targetQueueLangs = listOf(targetSongLang)

        val hasExplicitQueue = !queue.isNullOrEmpty()
        val initialQueue = if (isAdvancingInCurrentPlaybackQueue) {
            queue!!
        } else if (hasExplicitQueue && queue!!.size > 1) {
            recommendationGeneration++
            sessionSeedSong = song
            val songIdx = queue!!.indexOfFirst { it.videoId == song.videoId }
            if (songIdx >= 0) queue!! else (listOf(immediateSong) + queue!!).distinctBy { it.videoId }
        } else {
            // New Recommendation Context: User tapped a single track or search result
            recommendationGeneration++
            sessionSeedSong = song
            val curQ = ytPlayerController.playbackQueue.value
            val curIdx = ytPlayerController.currentQueueIndex.value
            val upcomingUserAdded = curQ.drop((curIdx + 1).coerceAtMost(curQ.size)).filter { s -> s.isManual || ytPlayerController.isUserQueued(s.videoId) }
            listOf(immediateSong) + upcomingUserAdded
        }

        // Asynchronously compute deep relevant queue when user selected a seed
        val currentGen = recommendationGeneration
        if (!isAdvancingInCurrentPlaybackQueue && (!hasExplicitQueue || queue!!.size <= 1)) {
            loadSuggestionsForSong(song, currentGen)
        }

        // Claim ISAI Connect active device status and sync song details and queue to Firebase
        if (!localStorage.isMultiDevicePlaybackSeparate.value) {
            isaiConnectManager.updatePlaybackState(
                song = immediateSong,
                isPlaying = true,
                positionMs = (startPositionSec * 1000).toLong(),
                currentDeviceId = isaiConnectManager.deviceId,
                queue = initialQueue,
                queueIndex = 0
            )
        }

        ytPlayerController.prepareForPlayback(immediateSong, initialQueue)

        if (!immediateSong.audioUrl.isNullOrBlank()) {
            ytPlayerController.playSong(immediateSong, initialQueue, startPositionSec, autoPlay)
            if (listenTogetherManager.isHost()) {
                listenTogetherManager.hostChangeSong(immediateSong, startPositionSec)
            }
            prefetchNextSongUrl(initialQueue, immediateSong)
        } else {
            // Start controller immediately so UI shows the playing track and buffer state right away without delay
            ytPlayerController.playSong(immediateSong, initialQueue, startPositionSec, autoPlay)
            if (listenTogetherManager.isHost()) {
                listenTogetherManager.hostChangeSong(immediateSong, startPositionSec)
            }

            // Direct 320kbps audio resolution with robust fast fallback
            viewModelScope.launch(Dispatchers.IO) {
                var resolvedUrl: String? = null
                try {
                    resolvedUrl = withTimeoutOrNull(2800L) {
                        var url: String? = null
                        // Only try song ID if it is a numeric or Saavn token (NOT a standard 11-char YouTube ID)
                        val isYouTubeId = song.videoId.length == 11 && Regex("^[a-zA-Z0-9_-]{11}$").matches(song.videoId)
                        if (!isYouTubeId && song.videoId.length in 5..14 && !song.videoId.contains("-") && !song.videoId.contains("_")) {
                            url = musicRepo.resolveStreamUrlBySongId(song.videoId)
                        }
                        if (url.isNullOrBlank()) {
                            val cleanTitle = song.title
                                .replace(Regex("\\s*[|•].*$"), "")
                                .replace(Regex("\\s*\\(.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\)", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("\\s*\\[.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\]", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("(?i)\\b(official\\s*(video|audio|lyric(al)?\\s*video)?|lyric(al)?\\s*video|video\\s*song|full\\s*song|audio\\s*song|hd\\s*video)\\b"), "")
                                .replace(Regex("\\.{2,}$"), "")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            val targetLang = song.language.ifBlank {
                                com.saavn.music.util.RelevanceEngine.detectSongLanguage(song).ifBlank {
                                    _preferredLanguages.value.firstOrNull() ?: "tamil"
                                }
                            }
                            url = musicRepo.resolveStreamUrl(cleanTitle.ifBlank { song.title }, song.channelTitle, targetLanguage = targetLang)
                        }
                        if (url.isNullOrBlank()) {
                            val cleanTitleOnly = song.title.split("-")[0].split("(")[0].trim()
                            if (cleanTitleOnly.isNotBlank()) {
                                val quickHits = musicRepo.search(cleanTitleOnly, limit = 5).getOrNull()
                                val matched = quickHits?.firstOrNull { it.getStreamUrl(AudioQuality.VERY_HIGH) != null }
                                url = matched?.getStreamUrl(AudioQuality.VERY_HIGH)
                            }
                        }
                        url
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ISAI_PLAYER", "[MainViewModel] Quick resolve failed: ${e.message}")
                }

                withContext(Dispatchers.Main) {
                    val finalUrl = resolvedUrl ?: run {
                        val matched = YouTubeMusicRepository.CURATED_TAMIL_SONGS.firstOrNull {
                            it.videoId == song.videoId ||
                            it.title.equals(song.title, ignoreCase = true) ||
                            (song.title.length >= 4 && it.title.contains(song.title, ignoreCase = true)) ||
                            (it.title.length >= 4 && song.title.contains(it.title, ignoreCase = true))
                        }
                        matched?.audioUrl
                    }

                    if (!finalUrl.isNullOrBlank()) {
                        audioUrlCache[song.videoId] = finalUrl
                        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Fast-resolved direct 320kbps audio: $finalUrl")
                        val updatedSong = song.copy(audioUrl = finalUrl)
                        val updatedQueue = ytPlayerController.playbackQueue.value.map { if (it.videoId == song.videoId) updatedSong else it }
                        ytPlayerController.playSong(updatedSong, updatedQueue, startPositionSec, autoPlay)
                        prefetchNextSongUrl(updatedQueue, updatedSong)
                    }
                }
            }
        }
    }

    private fun prefetchNextSongUrl(queue: List<YouTubeSong>, currentSong: YouTubeSong) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val curIdx = queue.indexOfFirst { it.videoId == currentSong.videoId }
                if (curIdx >= 0) {
                    val nextSongs = queue.drop(curIdx + 1).take(2)
                    for (next in nextSongs) {
                        val cached = audioUrlCache[next.videoId]
                        if (!cached.isNullOrBlank()) continue
                        if (next.audioUrl.isNullOrBlank()) {
                            val cleanTitle = next.title
                                .replace(Regex("\\s*[|•].*$"), "")
                                .replace(Regex("\\s*\\(.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\)", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("\\s*\\[.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\]", RegexOption.IGNORE_CASE), "")
                                .replace(Regex("(?i)\\b(official\\s*(video|audio|lyric(al)?\\s*video)?|lyric(al)?\\s*video|video\\s*song|full\\s*song|audio\\s*song|hd\\s*video)\\b"), "")
                                .replace(Regex("\\.{2,}$"), "")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            val targetLang = next.language.ifBlank {
                                com.saavn.music.util.RelevanceEngine.detectSongLanguage(next).ifBlank {
                                    _preferredLanguages.value.firstOrNull() ?: "tamil"
                                }
                            }
                            val url = musicRepo.resolveStreamUrl(cleanTitle.ifBlank { next.title }, next.channelTitle, targetLanguage = targetLang)
                            if (!url.isNullOrBlank()) {
                                audioUrlCache[next.videoId] = url
                                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Pre-fetched next song audioUrl: '${next.title}' -> $url")
                                val enriched = next.copy(audioUrl = url)
                                val updatedQueue = ytPlayerController.playbackQueue.value.map { if (it.videoId == next.videoId) enriched else it }
                                withContext(Dispatchers.Main) {
                                    ytPlayerController.setPlaybackQueue(updatedQueue, ytPlayerController.currentQueueIndex.value)
                                }
                            }
                        } else {
                            audioUrlCache[next.videoId] = next.audioUrl
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun addToQueue(song: YouTubeSong) {
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Queue is managed by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

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
            currentRemoteSongs.removeAll { it.videoId == song.videoId }
            val currentPlayingId = remoteState.currentSongId
            val curIdx = if (!currentPlayingId.isNullOrBlank()) {
                val found = currentRemoteSongs.indexOfFirst { it.videoId == currentPlayingId }
                if (found >= 0) found else remoteState.queueIndex
            } else {
                remoteState.queueIndex
            }
            val insertPos = (curIdx + 1).coerceIn(0, currentRemoteSongs.size)
            currentRemoteSongs.add(insertPos, song)
            isaiConnectManager.updatePlaybackState(
                queue = currentRemoteSongs,
                currentDeviceId = remoteState.currentDeviceId
            )
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(getApplication(), "Added next in queue 🎵", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            if (ytPlayerController.currentSong.value == null) {
                playSong(song, listOf(song))
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(getApplication(), "Playing ${song.title.take(20)}... 🎵", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                ytPlayerController.addToQueue(song)
                isaiConnectManager.updatePlaybackState(
                    currentDeviceId = isaiConnectManager.deviceId,
                    queue = ytPlayerController.playbackQueue.value,
                    queueIndex = ytPlayerController.currentQueueIndex.value
                )
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(getApplication(), "Added next in queue 🎵", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun playNextInQueue(song: YouTubeSong) {
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Queue is managed by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

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
            val currentPlayingId = remoteState.currentSongId
            val curIdx = if (!currentPlayingId.isNullOrBlank()) {
                val found = currentRemoteSongs.indexOfFirst { it.videoId == currentPlayingId }
                if (found >= 0) found else remoteState.queueIndex
            } else {
                remoteState.queueIndex
            }
            val insertPos = (curIdx + 1).coerceIn(0, currentRemoteSongs.size)
            currentRemoteSongs.add(insertPos, song)
            isaiConnectManager.updatePlaybackState(
                queue = currentRemoteSongs,
                currentDeviceId = remoteState.currentDeviceId
            )
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
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Queue is managed by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

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
                        newQueueIndex = newQueueIndex.coerceIn(currentRemoteSongs.indices)
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
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            return
        }

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
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Queue is managed by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

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

    private var suggestionJob: Job? = null

    fun loadSuggestionsForSong(song: YouTubeSong, generation: Long = recommendationGeneration) {
        suggestionJob?.cancel()
        val currentGen = generation
        suggestionJob = viewModelScope.launch(Dispatchers.Default) {
            _isLoadingSuggestions.value = true
            try {
                if (recommendationGeneration != currentGen) return@launch
                val songLang = song.language.ifBlank { com.saavn.music.util.RelevanceEngine.detectSongLanguage(song) }.ifBlank { _preferredLanguages.value.firstOrNull() ?: "tamil" }
                val activeLangs = listOf(songLang)
                val primaryLang = songLang
                val query = com.saavn.music.util.RelevanceEngine.getRelevantSearchQuery(song, primaryLang)
                val primaryArtist = com.saavn.music.util.RelevanceEngine.extractPrimaryArtist(song.channelTitle)
                val songMood = com.saavn.music.util.RelevanceEngine.detectSongMood(song)
                val songEra = com.saavn.music.util.RelevanceEngine.detectSongEra(song)?.let { "$it " } ?: ""
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
                val artistQuery = if (primaryArtist.isNotBlank()) "$primaryArtist $primaryLang $songEra$moodKeyword songs" else "$primaryLang $songEra$moodKeyword songs"
                val extraMoodQuery = if (songMood == com.saavn.music.util.SongMood.MELODY_ROMANCE) {
                    "$primaryLang ${songEra}romantic love melody super hits"
                } else {
                    "$primaryLang ${songEra}evergreen $moodKeyword songs"
                }

                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Loading era ($songEra) & mood ($songMood) suggestions: '$query' / '$artistQuery'")

                val searchResultDeferred = async { musicRepo.search(query, limit = 40).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList() }
                val artistResultDeferred = async { musicRepo.search(artistQuery, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList() }
                val extraResultDeferred = async { musicRepo.search(extraMoodQuery, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList() }

                val searchResult = searchResultDeferred.await()
                val artistResult = artistResultDeferred.await()
                val extraResult = extraResultDeferred.await()

                if (!isActive || recommendationGeneration != currentGen) return@launch

                val pool = (searchResult + artistResult + extraResult)
                    .distinctBy { it.videoId }
                    .filter { !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) }
                val currentQueue = ytPlayerController.playbackQueue.value
                val curIdx = ytPlayerController.currentQueueIndex.value
                val playedSoFar = currentQueue.take(curIdx + 1)

                val filteredPool = pool.filter { candidate ->
                    candidate.videoId != song.videoId &&
                    !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(candidate) &&
                    !com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(song, candidate) &&
                    playedSoFar.none { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
                    com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs) &&
                    com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, candidate, activeLangs, sessionSeedSong) > 0
                }

                val candidateMatches = if (filteredPool.isNotEmpty()) {
                    com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, filteredPool, activeLangs, maxItems = 50, minItems = 25, sessionSeed = sessionSeedSong).filter { it.videoId != song.videoId && !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) }
                } else {
                    val ytResult = ytRepo.searchSongs(query, maxResults = 35).getOrDefault(emptyList())
                    val filteredYt = ytResult.filter { candidate ->
                        candidate.videoId != song.videoId &&
                        !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(candidate) &&
                        !com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(song, candidate) &&
                        playedSoFar.none { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
                        com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs) &&
                        com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, candidate, activeLangs, sessionSeedSong) > 0
                    }
                    com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, filteredYt, activeLangs, maxItems = 50, minItems = 25, sessionSeed = sessionSeedSong).filter { it.videoId != song.videoId && !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) }
                }

                // Strictly ensure minimum 25 upcoming songs in the song's exact language and mood
                val relevantMatches = if (candidateMatches.size < 25) {
                    val backupPool = mutableListOf<YouTubeSong>()
                    if (activeLangs.contains("tamil")) {
                        backupPool.addAll(_trendingSongs.value)
                        backupPool.addAll(_mostPlayedSongs.value)
                        backupPool.addAll(_latestReleases.value)
                        backupPool.addAll(YouTubeMusicRepository.CURATED_TAMIL_SONGS)
                    }
                    val backupCandidates = backupPool
                        .distinctBy { it.videoId }
                        .filter { candidate ->
                            candidate.videoId != song.videoId &&
                            !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(candidate) &&
                            !com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(song, candidate) &&
                            playedSoFar.none { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
                            !candidateMatches.any { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
                            com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs) &&
                            com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, candidate, activeLangs, sessionSeedSong) > 0
                        }
                    (candidateMatches + backupCandidates).take(40)
                } else {
                    candidateMatches
                }.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) && !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) && com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, it, activeLangs, sessionSeedSong) > 0 }

                if (!isActive || recommendationGeneration != currentGen) {
                    android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Dropping stale recommendation results (gen mismatch: $recommendationGeneration vs $currentGen)")
                    return@launch
                }

                _suggestions.value = relevantMatches

                withContext(Dispatchers.Main) {
                    if (recommendationGeneration == currentGen && ytPlayerController.currentSong.value?.videoId == song.videoId) {
                        ytPlayerController.replaceUpcomingQueue(relevantMatches)
                        if (isaiConnectManager.isMyDeviceActive()) {
                            isaiConnectManager.updatePlaybackState(
                                queue = ytPlayerController.playbackQueue.value,
                                queueIndex = ytPlayerController.currentQueueIndex.value
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("ISAI_PLAYER", "[MainViewModel] Failed to load suggestions: ${e.message}")
                }
            } finally {
                _isLoadingSuggestions.value = false
            }
        }
    }

    fun ensureEndlessQueue(song: YouTubeSong? = ytPlayerController.currentSong.value) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val currentQueue = ytPlayerController.playbackQueue.value
                val currentIndex = ytPlayerController.currentQueueIndex.value
                val remainingInQueue = currentQueue.size - (currentIndex + 1)

                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] ensureEndlessQueue check: queueSize=${currentQueue.size}, currentIdx=$currentIndex, remaining=$remainingInQueue")

                if (remainingInQueue < 25 && song != null) {
                    val targetSongLang = song.language.ifBlank { com.saavn.music.util.RelevanceEngine.detectSongLanguage(song) }.ifBlank { _preferredLanguages.value.firstOrNull() ?: "tamil" }
                    val activeLangs = listOf(targetSongLang)
                    val primaryLang = targetSongLang

                    val query = com.saavn.music.util.RelevanceEngine.getRelevantSearchQuery(song, primaryLang)
                    val primaryArtist = com.saavn.music.util.RelevanceEngine.extractPrimaryArtist(song.channelTitle)
                    val songMood = com.saavn.music.util.RelevanceEngine.detectSongMood(song)
                    val songEra = com.saavn.music.util.RelevanceEngine.detectSongEra(song)?.let { "$it " } ?: ""
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
                    val artistQuery = if (primaryArtist.isNotBlank()) "$primaryArtist $primaryLang $songEra$moodKeyword songs" else "$primaryLang $songEra$moodKeyword hit songs"

                    val searchHits = async {
                        musicRepo.search(query, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    }
                    val artistHits = async {
                        musicRepo.search(artistQuery, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    }
                    val combinedHits = (searchHits.await() + artistHits.await())

                    val newSongs = mutableListOf<YouTubeSong>()
                    for (candidate in combinedHits) {
                        if (candidate.videoId == song.videoId) continue
                        if (com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(song, candidate)) continue
                        if (sessionSeedSong != null && com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(sessionSeedSong!!, candidate)) continue
                        if (currentQueue.any { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) }) continue
                        if (newSongs.any { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) }) continue
                        if (!com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs)) continue
                        if (com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, candidate, activeLangs, sessionSeedSong) <= 0) continue
                        newSongs.add(candidate)
                        if (newSongs.size >= 35) break
                    }

                    val strictlyPreferred = newSongs.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) && com.saavn.music.util.RelevanceEngine.scoreSongRelevance(song, it, activeLangs, sessionSeedSong) > 0 }
                    if (strictlyPreferred.isNotEmpty()) {
                        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Appending ${strictlyPreferred.size} candidate songs to endless queue in language $activeLangs")
                        ytPlayerController.appendQueue(strictlyPreferred)
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
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            return
        }
        viewModelScope.launch {
            android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Auto-play triggered by onQueueExhausted!")
            val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
            val currentQueue = ytPlayerController.playbackQueue.value
            val currentSong = ytPlayerController.currentSong.value
            val candidate = _suggestions.value.firstOrNull { candidate ->
                (currentSong == null || !com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(currentSong, candidate)) &&
                currentQueue.none { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
                com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs)
            } ?: _trendingSongs.value.firstOrNull { candidate ->
                (currentSong == null || !com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(currentSong, candidate)) &&
                currentQueue.none { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
                com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs)
            } ?: if (activeLangs.contains("tamil")) {
                YouTubeMusicRepository.CURATED_TAMIL_SONGS.firstOrNull { candidate ->
                    (currentSong == null || !com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(currentSong, candidate)) &&
                    currentQueue.none { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
                    com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs)
                }
            } else null

            if (candidate != null) {
                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Auto-advancing to: '${candidate.title}'")
                // Add to playback queue so it's reflected in queue UI
                ytPlayerController.addToQueue(candidate)
                playSong(candidate, ytPlayerController.playbackQueue.value, openFullPlayer = false)
            } else {
                android.util.Log.w("ISAI_PLAYER", "[MainViewModel] No candidate song found for auto-advance in $activeLangs.")
            }
        }
    }

    fun togglePlayPause() {
        android.util.Log.i("ISAI_PLAYER", "[MainViewModel] togglePlayPause called!")

        // Listen Together Host/Listener Access Check
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Play/Pause is controlled by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

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
        if (listenTogetherManager.isHost()) {
            if (ytPlayerController.isPlaying.value) {
                listenTogetherManager.hostPause(ytPlayerController.currentPositionSec.value)
            } else {
                listenTogetherManager.hostPlay(ytPlayerController.currentPositionSec.value)
            }
        }
        ytPlayerController.togglePlayPause()
    }

    fun playNext() {
        // Listen Together Host/Listener Access Check
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Only Host ($hostName) can skip songs", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

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
        // Listen Together Host/Listener Access Check
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Only Host ($hostName) can skip songs", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

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
        // Listen Together Host/Listener Access Check
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Playback position is controlled by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }

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
        if (listenTogetherManager.isHost()) {
            listenTogetherManager.hostSeek(seconds)
        }
        ytPlayerController.seekTo(seconds)
    }

    private var volumeDebounceJob: Job? = null
    private var lastVolumeToastTime: Long = 0L

    fun syncWithSystemVolume() {
        // In Listen Together room, listener volume stays synced to Host
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val am = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@launch
                val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                if (max > 0) {
                    val percent = Math.round((current.toFloat() / max.toFloat()) * 100).toInt().coerceIn(0, 100)
                    withContext(Dispatchers.Main) {
                        ytPlayerController.updateSystemVolume(percent)
                    }
                    if (listenTogetherManager.isHost()) {
                        listenTogetherManager.hostSetVolume(percent)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Failed to sync system volume: ${e.message}")
            }
        }
    }

    fun setVolume(volumePercent: Int) {
        // Listen Together Host/Listener Access Check
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            val now = System.currentTimeMillis()
            if (now - lastVolumeToastTime > 2000L) {
                lastVolumeToastTime = now
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                    android.widget.Toast.makeText(getApplication(), "👑 Sound/Volume is controlled by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        val clamped = volumePercent.coerceIn(0, 100)
        // 1. Instantly update in-memory digital volume on Main thread (0ms latency, butter smooth slider)
        ytPlayerController.setVolume(clamped)

        // 2. Debounce heavy network and AudioService IPC calls to IO dispatcher (never blocks UI)
        volumeDebounceJob?.cancel()
        volumeDebounceJob = viewModelScope.launch(Dispatchers.IO) {
            delay(120)

            if (listenTogetherManager.isHost()) {
                listenTogetherManager.hostSetVolume(clamped)
            }

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
            } else {
                try {
                    val am = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    if (am != null) {
                        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        if (max > 0) {
                            val target = Math.round((clamped.toFloat() / 100f) * max).coerceIn(0, max)
                            val currentStream = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                            if (currentStream != target) {
                                am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
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
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Shuffle is controlled by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }
        ytPlayerController.toggleShuffle()
    }

    fun toggleRepeat() {
        if (listenTogetherManager.currentRoom.value != null && !listenTogetherManager.isHost()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val hostName = listenTogetherManager.currentRoom.value?.hostDeviceName ?: "Host"
                android.widget.Toast.makeText(getApplication(), "👑 Repeat is controlled by Host ($hostName)", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }
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
        val normLangs = languages.map { com.saavn.music.util.RelevanceEngine.normalizeLanguage(it) }.distinct().ifEmpty { listOf("tamil") }
        _preferredLanguages.value = normLangs
        _showLanguageDialog.value = false
        val currentProfile = localStorage.userProfile.value
        if (currentProfile != null) {
            localStorage.saveUserProfile(currentProfile.copy(preferredLanguages = normLangs))
        } else {
            localStorage.saveUserProfile(
                UserProfile(
                    id = "user_default",
                    displayName = "ISAI Listener",
                    isLoggedIn = true,
                    preferredLanguages = normLangs
                )
            )
        }
        isaiConnectManager.syncPreferences(normLangs)
        loadHomeData()
        val currentQueue = ytPlayerController.playbackQueue.value
        val curIdx = ytPlayerController.currentQueueIndex.value
        if (currentQueue.isNotEmpty()) {
            val playedSoFar = currentQueue.take(curIdx + 1)
            val upcoming = currentQueue.drop(curIdx + 1)
            val validUpcoming = upcoming.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, normLangs) }
            ytPlayerController.setPlaybackQueue(playedSoFar + validUpcoming, curIdx)
        }
        ytPlayerController.currentSong.value?.let { current ->
            loadSuggestionsForSong(current)
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        val withLogin = profile.copy(isLoggedIn = true)
        localStorage.saveUserProfile(withLogin)
        subscriptionRepo.attachSubscriptionListener(withLogin.id)
        closeLoginDialog()
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
        subscriptionRepo.attachSubscriptionListener(profile.id)
        closeLoginDialog()
    }

    fun logoutUser() {
        subscriptionRepo.detachSubscriptionListener()
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
        subscriptionRepo.detachSubscriptionListener()
        ytPlayerController.detachPlayer()
    }
}
