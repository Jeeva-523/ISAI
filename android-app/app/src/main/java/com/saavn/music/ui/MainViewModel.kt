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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.saavn.music.data.model.SearchPlaylistItem
import com.saavn.music.data.repository.EditorialPlaylistsCatalog
import com.saavn.music.data.repository.KadhalVibesCatalog
import com.saavn.music.data.repository.IdhayamPesutheyCatalog
import com.saavn.music.data.repository.SemmaKuthuCatalog
import com.saavn.music.data.repository.MassModeCatalog
import com.saavn.music.data.repository.GaanaPettaiCatalog
import com.saavn.music.data.repository.JannalOraPayanamCatalog
import com.saavn.music.data.repository.IravuMelodiesCatalog
import com.saavn.music.data.repository.PudhuUdhayamCatalog
import com.saavn.music.data.repository.IraiIsaiCatalog
import com.saavn.music.data.repository.SchoolDaysMemoriesCatalog

enum class AppScreen {
    HOME,
    SEARCH,
    LIBRARY,
    PROFILE,
    PLAYLIST_DETAIL
}

data class PlaylistDetailState(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val language: String = "",
    val coverUrl: String = "",
    val searchQuery: String = "",
    val songs: List<YouTubeSong> = emptyList(),
    val isLoading: Boolean = false,
    val creatorName: String = "",
    val isPublic: Boolean = false,
    val isCustomPlaylist: Boolean = false
)

data class IsaiDailyMix(
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

    // Multi-Device Playback Mode: Separate (independent on 2+ devices) vs Sync (ISAI Connect)
    val isMultiDevicePlaybackSeparate: StateFlow<Boolean> = localStorage.isMultiDevicePlaybackSeparate

    fun setMultiDevicePlaybackSeparate(enabled: Boolean) {
        localStorage.setMultiDevicePlaybackSeparate(enabled)
        isaiConnectManager.syncMultiDeviceSeparate(enabled)
    }

    val dynamicUiConfig: StateFlow<com.saavn.music.data.model.DynamicUiConfig> = dynamicUiService.uiConfig

    // Recommendation Context & Session Seed
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
    private var previousScreen: AppScreen = AppScreen.HOME

    // Playlist Detail Screen State & In-Memory Fast Cache
    private val playlistSongsCache = java.util.concurrent.ConcurrentHashMap<String, List<YouTubeSong>>()
    private val _playlistDetail = MutableStateFlow<PlaylistDetailState?>(null)
    val playlistDetail: StateFlow<PlaylistDetailState?> = _playlistDetail.asStateFlow()

    // Active Tamil Music Category
    private val _selectedCategory = MutableStateFlow("Most Played")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Home Categorized Songs (Starts empty and dynamically populates from live APIs)
    private val _trendingSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val trendingSongs: StateFlow<List<YouTubeSong>> = _trendingSongs.asStateFlow()

    private val _popularArtists = MutableStateFlow<List<com.saavn.music.data.trending.TopArtistData>>(emptyList())
    val popularArtists: StateFlow<List<com.saavn.music.data.trending.TopArtistData>> = _popularArtists.asStateFlow()

    private val _categorySongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val categorySongs: StateFlow<List<YouTubeSong>> = _categorySongs.asStateFlow()

    private val _latestReleases = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val latestReleases: StateFlow<List<YouTubeSong>> = _latestReleases.asStateFlow()

    private val _picksSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val picksSongs: StateFlow<List<YouTubeSong>> = _picksSongs.asStateFlow()

    private val _mostPlayedSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val mostPlayedSongs: StateFlow<List<YouTubeSong>> = _mostPlayedSongs.asStateFlow()

    private val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val searchResults: StateFlow<List<YouTubeSong>> = _searchResults.asStateFlow()

    private val _searchPlaylists = MutableStateFlow<List<SearchPlaylistItem>>(emptyList())
    val searchPlaylists: StateFlow<List<SearchPlaylistItem>> = _searchPlaylists.asStateFlow()

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

    private val _similarToRecentSongs = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val similarToRecentSongs: StateFlow<List<YouTubeSong>> = _similarToRecentSongs.asStateFlow()

    private val _similarToRecentTitle = MutableStateFlow<String>("")
    val similarToRecentTitle: StateFlow<String> = _similarToRecentTitle.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    // ISAI Daily Mixes
    private val _isaiDailyMixes = MutableStateFlow<List<IsaiDailyMix>>(emptyList())
    val isaiDailyMixes: StateFlow<List<IsaiDailyMix>> = _isaiDailyMixes.asStateFlow()

    // Storage references
    val favorites: StateFlow<List<YouTubeSong>> = localStorage.favorites
    val playlists: StateFlow<List<UserPlaylist>> = localStorage.playlists
    val recentlyPlayed: StateFlow<List<YouTubeSong>> = localStorage.recentlyPlayed

    // Community / Public Playlists
    private val gson = Gson()
    private val _publicPlaylists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val publicPlaylists: StateFlow<List<UserPlaylist>> = _publicPlaylists.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Listen to Community Public Playlists
        listenToPublicPlaylists()

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
                    // Live YouTube Music dynamic adaptation: Update recommendations based on what the user listens to
                    loadPersonalizedRecommendations(force = true)
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

        // Remote command listener: ISAI Connect commands received from Web or other devices
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
        if (!force && _personalizedRecommendations.value.isNotEmpty() && (now - lastRecommendedFetchTime < 3000L)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songPlayCounts = localStorage.songPlayCounts.value
                val artistPlayCounts = localStorage.artistPlayCounts.value
                val recentList = (localStorage.recentlyPlayed.value + isaiConnectManager.syncedRecentlyPlayed.value).distinctBy { it.videoId }
                val favList = localStorage.favorites.value
                val allUserSongs = (recentList + favList).distinctBy { it.videoId }

                if (recentList.isNotEmpty() || allUserSongs.isNotEmpty()) {
                    // Massive recency weight so newly played songs immediately reshape recommendations
                    val scoredSongs = allUserSongs.map { song ->
                        val playCount = songPlayCounts[song.videoId] ?: 0
                        val isFav = favList.any { it.videoId == song.videoId }
                        val recencyIndex = recentList.indexOfFirst { it.videoId == song.videoId }
                        val recencyBonus = if (recencyIndex >= 0) maxOf(0, 50 - (recencyIndex * 5)) else 0
                        val score = (playCount * 3) + (if (isFav) 10 else 0) + recencyBonus
                        song to score
                    }.sortedByDescending { it.second }

                    val topSong = scoredSongs.firstOrNull()?.first
                    val latestSong = recentList.firstOrNull() ?: topSong

                    // Aggregate artist affinity
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
                    val activeLangs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                    val primaryLang = activeLangs.first().lowercase()

                    val latestArtist = latestSong?.channelTitle
                        ?.replace(" - Topic", "")
                        ?.replace(" Official", "")
                        ?.split("•")?.first()
                        ?.split(",")?.first()
                        ?.split("&")?.first()
                        ?.trim() ?: ""

                    val cleanLatestTitle = latestSong?.title
                        ?.replace(Regex("\\s*[|•].*$"), "")
                        ?.replace(Regex("\\s*\\(.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\)", RegexOption.IGNORE_CASE), "")
                        ?.replace(Regex("\\s*\\[.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\]", RegexOption.IGNORE_CASE), "")
                        ?.trim() ?: ""

                    // Build dynamic queries based on what the user listened to
                    val queries = mutableListOf<String>()
                    if (cleanLatestTitle.isNotBlank()) {
                        queries.add("$cleanLatestTitle $primaryLang")
                        latestSong?.let { queries.add(RelevanceEngine.getRelevantSearchQuery(it, primaryLang)) }
                    }
                    if (latestArtist.isNotBlank() && latestArtist != "Tamil Artist") {
                        queries.add("$latestArtist $primaryLang hits")
                    }
                    if (!topArtist.isNullOrBlank() && topArtist != latestArtist) {
                        queries.add("$topArtist $primaryLang songs")
                    }

                    if (cleanLatestTitle.isNotBlank()) {
                        _recommendedReason.value = "Because you listened to ${cleanLatestTitle.take(26)}"
                        _similarToRecentTitle.value = "Similar to ${cleanLatestTitle.take(24)}"
                    } else if (!topArtist.isNullOrBlank()) {
                        _recommendedReason.value = "Because you frequently listen to $topArtist"
                        _similarToRecentTitle.value = "More from $topArtist"
                    }

                    val searchDeferred = queries.take(4).map { q ->
                        async {
                            musicRepo.search(q, limit = 25).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                        }
                    }
                    val fetched = searchDeferred.awaitAll().flatten()
                    val deduped = ytRepo.deduplicateSongs(fetched)
                        .filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, activeLangs) }

                    // Exclude the currently played song to give genuine discovery
                    val freshRecs = deduped.filterNot { rec ->
                        recentList.take(3).any { ytRepo.isSameSong(it, rec) }
                    }
                    val finalRecs = if (freshRecs.size >= 10) freshRecs else deduped

                    if (finalRecs.isNotEmpty()) {
                        _personalizedRecommendations.value = finalRecs.take(40)
                        _picksSongs.value = finalRecs.take(40)
                        _similarToRecentSongs.value = finalRecs.take(20)
                        lastRecommendedFetchTime = now
                        return@launch
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
                val finalDefault = recs.distinctBy { it.videoId }.take(35)
                _personalizedRecommendations.value = finalDefault
                _picksSongs.value = finalDefault
                _similarToRecentSongs.value = emptyList()
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
                val mostPlayed: List<YouTubeSong>
                if (cleanSaavn.isNotEmpty()) {
                    val deduped = ytRepo.deduplicateSongs(cleanSaavn)
                    val pool = deduped.distinctBy { it.videoId }
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
                    val pool = deduped.distinctBy { it.videoId }
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

                // Load Curated Daily Mixes (curated by preferred language with 30 songs each)
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
                    _latestReleases.value = (dedupedNew + poolTrending.drop(5)).distinctBy { it.videoId }.take(35)
                } catch (_: Exception) {
                    if (_latestReleases.value.isEmpty()) {
                        val poolTrending = _trendingSongs.value.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                        _latestReleases.value = poolTrending.drop(5).distinctBy { it.videoId }.take(35)
                    }
                }

                // Refresh recommendations on home reload
                loadPersonalizedRecommendations(force = true)
                val validRecs = _personalizedRecommendations.value.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                val poolTrending = _trendingSongs.value.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, langs) }
                val picks = (validRecs + poolTrending).distinctBy { it.videoId }.take(35)
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

                // Pre-warm top featured playlists in background so clicking them opens instantly with 0ms delay
                viewModelScope.launch(Dispatchers.IO) {
                    preWarmFeaturedPlaylists(primaryLang)
                }
            } catch (e: Exception) {
                try {
                    val langs = _preferredLanguages.value.ifEmpty { listOf("tamil") }
                    val trending = ytRepo.getTrendingSongs(langs).filterNot { s ->
                        val t = s.title.lowercase()
                        t.contains("trending") || t.contains("jukebox") || t.contains("full album") || t.contains("non stop")
                    }
                    val deduped = ytRepo.deduplicateSongs(trending)
                    val pool = deduped.distinctBy { it.videoId }
                    val mostPlayed = trendingService.getMostPlayedSongs(pool).take(40)
                    if (mostPlayed.isNotEmpty()) {
                        _trendingSongs.value = mostPlayed
                        _categorySongs.value = mostPlayed
                        _mostPlayedSongs.value = mostPlayed
                        _picksSongs.value = pool.take(35)
                        _latestReleases.value = pool.drop(5).distinctBy { it.videoId }.take(35)
                    }
                } catch (_: Exception) {
                    // Retain dynamic in-memory results during network interruptions
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

    fun openFeaturedPlaylist(
        id: String,
        title: String,
        subtitle: String,
        language: String,
        coverUrl: String,
        searchQuery: String
    ) {
        if (_currentScreen.value != AppScreen.PLAYLIST_DETAIL) {
            previousScreen = _currentScreen.value
        }

        val idNorm = id.lowercase().trim()
        val langNorm = language.ifBlank { "tamil" }.lowercase().trim()
        val cacheKey = "${idNorm}_$langNorm"

        // 1. FAST IN-MEMORY CACHE (0ms Instant Load)
        val cached = playlistSongsCache[cacheKey]
            ?: playlistSongsCache[idNorm]
            ?: playlistSongsCache[title.lowercase().trim()]

        if (cached != null && cached.isNotEmpty()) {
            _playlistDetail.value = PlaylistDetailState(
                id = id,
                title = title,
                subtitle = subtitle,
                language = language,
                coverUrl = coverUrl,
                searchQuery = searchQuery,
                songs = cached,
                isLoading = false
            )
            _currentScreen.value = AppScreen.PLAYLIST_DETAIL
            return
        }

        // 2. INSTANT PRE-SEEDING from in-memory pools (User NEVER sees a blank screen)
        val preSeeded = getPreSeededPlaylistSongs(id, title, language)

        _playlistDetail.value = PlaylistDetailState(
            id = id,
            title = title,
            subtitle = subtitle,
            language = language,
            coverUrl = coverUrl,
            searchQuery = searchQuery,
            songs = preSeeded,
            isLoading = preSeeded.isEmpty()
        )
        _currentScreen.value = AppScreen.PLAYLIST_DETAIL
        loadPlaylistSongs(id, title, searchQuery, language)
    }

    private fun cleanPlaylistQuery(text: String): String {
        return text
            .replace(Regex("[\\p{So}\\p{Cn}]"), " ")
            .replace(Regex("(?i)\\b(50\\+\\s*songs|isai\\s*editorial|curated|playlist|tracks)\\b"), " ")
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun getPreSeededPlaylistSongs(playlistId: String, title: String, language: String): List<YouTubeSong> {
        val idNorm = playlistId.lowercase().trim()
        val titleNorm = title.lowercase().trim()

        // Kadhal Vibes: Exact 50 fixed Tamil romantic classics
        if (idNorm == "kadhal-vibes" || idNorm.contains("kadhal-vibes") || titleNorm.contains("kadhal vibes") || titleNorm.contains("kadhal vibe")) {
            return KadhalVibesCatalog.SONGS
        }

        // Idhayam Pesuthey: Exact 50 fixed soulful emotional melodies
        if (idNorm == "idhayam-pesuthey" || idNorm.contains("idhayam-pesuthey") || titleNorm.contains("idhayam pesuthey") || titleNorm.contains("idhayam pesuthe")) {
            return IdhayamPesutheyCatalog.SONGS
        }

        // Semma Kuthu: Exact 50 fixed high-energy kuthu party tracks
        if (idNorm == "semma-kuthu" || idNorm.contains("semma-kuthu") || titleNorm.contains("semma kuthu")) {
            return SemmaKuthuCatalog.SONGS
        }

        // Mass Mode: Exact 50 fixed high-octane mass/hero anthems
        if (idNorm == "mass-mode" || idNorm.contains("mass-mode") || titleNorm.contains("mass mode")) {
            return MassModeCatalog.SONGS
        }

        // Gaana Pettai: Exact 50 fixed Chennai gaana & folk tracks
        if (idNorm == "gaana-pettai" || idNorm.contains("gaana-pettai") || titleNorm.contains("gaana pettai")) {
            return GaanaPettaiCatalog.SONGS
        }

        // Jannal Ora Payanam: Exact 50 fixed breezy travel songs
        if (idNorm == "jannal-ora-payanam" || idNorm.contains("jannal-ora-payanam") || titleNorm.contains("jannal ora payanam")) {
            return JannalOraPayanamCatalog.SONGS
        }

        // Iravu Melodies: Exact 50 fixed peaceful night melodies
        if (idNorm == "iravu-melodies" || idNorm.contains("iravu-melodies") || titleNorm.contains("iravu melodies") || titleNorm.contains("iravu melody")) {
            return IravuMelodiesCatalog.SONGS
        }

        // Pudhu Udhayam: Exact 50 fixed motivation, resilience & hope anthems
        if (idNorm == "pudhu-udhayam" || idNorm.contains("pudhu-udhayam") || titleNorm.contains("pudhu udhayam") || titleNorm.contains("pudhu udhayam")) {
            return PudhuUdhayamCatalog.SONGS
        }

        // Irai Isai: Exact 50 fixed divine Tamil prayers & hymns
        if (idNorm == "irai-isai" || idNorm.contains("irai-isai") || titleNorm.contains("irai isai") || titleNorm.contains("irai isai")) {
            return IraiIsaiCatalog.SONGS
        }

        // School Days Memories: Exact 50 fixed late 90s & 2000s nostalgia songs
        if (idNorm == "school-days-memories" || idNorm.contains("school-days-memories") || titleNorm.contains("school days memories") || titleNorm.contains("school days")) {
            return SchoolDaysMemoriesCatalog.SONGS
        }

        val lang = language.ifBlank { "tamil" }.lowercase().trim()
        val targetLangs = listOf(lang)

        val candidatePool = (
            YouTubeMusicRepository.CURATED_TAMIL_SONGS +
            _trendingSongs.value +
            _mostPlayedSongs.value +
            _picksSongs.value +
            _latestReleases.value
        ).distinctBy { it.videoId }

        val langMatches = candidatePool.filter { com.saavn.music.util.RelevanceEngine.isSongInLanguage(it, targetLangs) }
        val pool = if (langMatches.size >= 8) langMatches else candidatePool

        val matched = when {
            idNorm.contains("romance") || titleNorm.contains("romance") || titleNorm.contains("love") -> {
                pool.filter { s ->
                    val t = s.title.lowercase()
                    val c = s.channelTitle.lowercase()
                    t.contains("love") || t.contains("kadhal") || t.contains("kaadhal") || t.contains("melody") ||
                    t.contains("duet") || t.contains("romantic") || t.contains("anbe") || t.contains("kanmani") ||
                    c.contains("sid sriram") || c.contains("harris jayaraj") || c.contains("a.r. rahman") || c.contains("ar rahman")
                }
            }
            idNorm.contains("kuthu") || titleNorm.contains("kuthu") || titleNorm.contains("dance") || titleNorm.contains("party") -> {
                pool.filter { s ->
                    val t = s.title.lowercase()
                    val c = s.channelTitle.lowercase()
                    t.contains("kuthu") || t.contains("party") || t.contains("dance") || t.contains("beat") ||
                    t.contains("arabic") || t.contains("hukum") || t.contains("naa ready") || t.contains("kaavaalaa") ||
                    t.contains("gana") || t.contains("dappan") || t.contains("vaathi") || c.contains("anirudh")
                }
            }
            idNorm.contains("chill") || titleNorm.contains("chill") || titleNorm.contains("lofi") || titleNorm.contains("lo-fi") -> {
                pool.filter { s ->
                    val t = s.title.lowercase()
                    t.contains("chill") || t.contains("lofi") || t.contains("acoustic") || t.contains("rain") ||
                    t.contains("midnight") || t.contains("peace") || t.contains("guitar")
                }
            }
            idNorm.contains("mass") || titleNorm.contains("mass") || titleNorm.contains("gym") || titleNorm.contains("workout") -> {
                pool.filter { s ->
                    val t = s.title.lowercase()
                    val c = s.channelTitle.lowercase()
                    t.contains("mass") || t.contains("gym") || t.contains("leo") || t.contains("jailer") ||
                    t.contains("beast") || t.contains("master") || t.contains("vikram") || t.contains("badass") ||
                    t.contains("hukum") || t.contains("energy") || c.contains("anirudh")
                }
            }
            idNorm.contains("90s") || titleNorm.contains("90s") || idNorm.contains("retro") || titleNorm.contains("retro") -> {
                pool.filter { s ->
                    val c = s.channelTitle.lowercase()
                    c.contains("spb") || c.contains("ilaiyaraaja") || c.contains("ilayaraja") || c.contains("rahman") || s.viewCountFormatted.contains("Year 199") || s.viewCountFormatted.contains("Year 198")
                }
            }
            titleNorm.contains("anirudh") -> pool.filter { it.channelTitle.contains("anirudh", ignoreCase = true) || it.title.contains("anirudh", ignoreCase = true) }
            titleNorm.contains("rahman") -> pool.filter { it.channelTitle.contains("rahman", ignoreCase = true) || it.title.contains("rahman", ignoreCase = true) }
            titleNorm.contains("harris") -> pool.filter { it.channelTitle.contains("harris", ignoreCase = true) || it.title.contains("harris", ignoreCase = true) }
            titleNorm.contains("yuvan") -> pool.filter { it.channelTitle.contains("yuvan", ignoreCase = true) || it.title.contains("yuvan", ignoreCase = true) }
            titleNorm.contains("sid sriram") -> pool.filter { it.channelTitle.contains("sid sriram", ignoreCase = true) || it.title.contains("sid sriram", ignoreCase = true) }
            titleNorm.contains("dsp") || titleNorm.contains("devi sri prasad") -> pool.filter { it.channelTitle.contains("dsp", ignoreCase = true) || it.channelTitle.contains("devi", ignoreCase = true) }
            titleNorm.contains("ilayaraja") || titleNorm.contains("ilaiyaraaja") -> pool.filter { it.channelTitle.contains("ilayaraja", ignoreCase = true) || it.channelTitle.contains("ilaiyaraaja", ignoreCase = true) }
            titleNorm.contains("spb") || titleNorm.contains("balasubrahmanyam") -> pool.filter { it.channelTitle.contains("spb", ignoreCase = true) || it.channelTitle.contains("balasubrahmanyam", ignoreCase = true) }
            idNorm.contains("trending") || titleNorm.contains("trending") -> {
                val recent2026 = pool.filter { s ->
                    s.viewCountFormatted.contains("2026") || s.title.contains("2026") || s.channelTitle.contains("2026")
                }
                if (recent2026.size >= 8) (recent2026 + pool).distinctBy { it.videoId } else pool
            }
            else -> {
                val cleanTokens = titleNorm.split(" ", "_", "-").filter { it.length > 2 }
                val tokenMatches = if (cleanTokens.isNotEmpty()) {
                    pool.filter { s -> cleanTokens.any { t -> s.title.lowercase().contains(t) || s.channelTitle.lowercase().contains(t) } }
                } else emptyList()
                if (tokenMatches.isNotEmpty()) tokenMatches else pool
            }
        }

        return matched.take(30)
    }

    suspend fun fetchRelatedSongsForPlaylistName(name: String, language: String = "tamil"): List<YouTubeSong> {
        val lang = language.ifBlank { "tamil" }
        val cleanName = cleanPlaylistQuery(name).ifBlank { "$lang hits" }
        val lower = cleanName.lowercase().trim()

        // Kadhal Vibes: Return exact 50 fixed Tamil romantic classics
        if (lower.contains("kadhal vibes") || lower.contains("kadhal vibe")) {
            return KadhalVibesCatalog.SONGS
        }

        // Idhayam Pesuthey: Return exact 50 fixed soulful emotional melodies
        if (lower.contains("idhayam pesuthey") || lower.contains("idhayam pesuthe")) {
            return IdhayamPesutheyCatalog.SONGS
        }

        // Semma Kuthu: Return exact 50 fixed high-energy kuthu party tracks
        if (lower.contains("semma kuthu")) {
            return SemmaKuthuCatalog.SONGS
        }

        // Mass Mode: Return exact 50 fixed high-octane mass/hero anthems
        if (lower.contains("mass mode")) {
            return MassModeCatalog.SONGS
        }

        // Gaana Pettai: Return exact 50 fixed Chennai gaana & folk tracks
        if (lower.contains("gaana pettai")) {
            return GaanaPettaiCatalog.SONGS
        }

        // Jannal Ora Payanam: Return exact 50 fixed breezy travel songs
        if (lower.contains("jannal ora payanam")) {
            return JannalOraPayanamCatalog.SONGS
        }

        // Iravu Melodies: Return exact 50 fixed peaceful night melodies
        if (lower.contains("iravu melodies") || lower.contains("iravu melody")) {
            return IravuMelodiesCatalog.SONGS
        }

        // Pudhu Udhayam: Return exact 50 fixed motivation, resilience & hope anthems
        if (lower.contains("pudhu udhayam")) {
            return PudhuUdhayamCatalog.SONGS
        }

        // Irai Isai: Return exact 50 fixed divine Tamil prayers & hymns
        if (lower.contains("irai isai")) {
            return IraiIsaiCatalog.SONGS
        }

        // School Days Memories: Return exact 50 fixed late 90s & 2000s nostalgia songs
        if (lower.contains("school days memories") || lower.contains("school days")) {
            return SchoolDaysMemoriesCatalog.SONGS
        }

        val queryCandidates = mutableListOf<String>()

        queryCandidates.add(cleanName)

        when {
            lower.contains("trending") || lower.contains("latest") || lower.contains("recent") -> {
                queryCandidates.add("$lang latest songs 2026")
                queryCandidates.add("$lang trending hits 2026")
                queryCandidates.add("$lang new songs 2026")
                queryCandidates.add("$lang latest releases 2026")
                queryCandidates.add("$lang chartbusters 2026")
                queryCandidates.add("Latest $lang songs")
            }
            lower.contains("love") || lower.contains("romance") || lower.contains("kadhal") || lower.contains("kaadhal") -> {
                queryCandidates.add("$lang romantic love melody songs")
                queryCandidates.add("$lang love duet hit songs")
                queryCandidates.add("Sid Sriram $lang romantic melodies")
            }
            lower.contains("kuthu") || lower.contains("dance") || lower.contains("party") || lower.contains("dappan") || lower.contains("fast") -> {
                queryCandidates.add("$lang kuthu dance party songs")
                queryCandidates.add("Anirudh $lang kuthu dance hits")
                queryCandidates.add("$lang dappankuthu fast beat hits")
            }
            lower.contains("chill") || lower.contains("lofi") || lower.contains("relax") || lower.contains("sleep") || lower.contains("midnight") -> {
                queryCandidates.add("$lang acoustic chill lofi songs")
                queryCandidates.add("$lang midnight chill relaxing songs")
            }
            lower.contains("gym") || lower.contains("workout") || lower.contains("mass") || lower.contains("motivation") -> {
                queryCandidates.add("$lang mass gym workout motivation songs")
                queryCandidates.add("Anirudh $lang mass energetic hits")
            }
            lower.contains("90s") || lower.contains("90") -> {
                queryCandidates.add("$lang 90s golden melody super hits")
                queryCandidates.add("AR Rahman 90s $lang hits")
            }
            lower.contains("2k") || lower.contains("2000s") -> {
                queryCandidates.add("$lang 2000s evergreen super hit songs")
                queryCandidates.add("Harris Jayaraj $lang 2000s evergreen hits")
            }
            lower.contains("2010s") -> {
                queryCandidates.add("$lang 2010 to 2019 blockbuster hits")
                queryCandidates.add("Anirudh $lang 2010 to 2019 hits")
            }
            lower.contains("2020s") -> {
                queryCandidates.add("$lang 2020 to 2025 blockbuster hit songs")
            }
            lower.contains("retro") || lower.contains("80s") || lower.contains("70s") || lower.contains("vintage") -> {
                queryCandidates.add("$lang 80s 70s vintage classic hit songs")
                queryCandidates.add("Ilaiyaraaja vintage evergreen classics $lang")
            }
            lower.contains("sad") || lower.contains("breakup") || lower.contains("vali") -> {
                queryCandidates.add("$lang sad heartbreak melody songs")
                queryCandidates.add("$lang emotional breakup songs")
            }
            lower.contains("devotional") || lower.contains("bakthi") || lower.contains("god") -> {
                queryCandidates.add("$lang devotional bakthi padalgal")
            }
            lower.contains("melody") || lower.contains("melodies") -> {
                queryCandidates.add("$lang evergreen melody super hits")
                queryCandidates.add("$cleanName $lang")
            }
            else -> {
                if (!lower.contains(lang.lowercase())) {
                    queryCandidates.add("$cleanName $lang hit songs")
                    queryCandidates.add("$cleanName $lang songs")
                } else {
                    queryCandidates.add("$cleanName hit songs")
                }
            }
        }
        queryCandidates.add("$lang top trending hits")

        val list = mutableListOf<YouTubeSong>()

        // 1. Multi-query search via JioSaavn
        for (q in queryCandidates.distinct().take(3)) {
            if (list.size >= 40) break
            try {
                val results = musicRepo.search(q, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                list.addAll(results)
            } catch (_: Exception) {}
        }

        // 2. Multi-query fallback / supplement via YouTube Music scraper
        if (list.size < 25) {
            for (q in queryCandidates.distinct().take(2)) {
                if (list.size >= 40) break
                try {
                    val ytResults = ytRepo.searchSongs(q, maxResults = 35).getOrDefault(emptyList())
                    list.addAll(ytResults)
                } catch (_: Exception) {}
            }
        }

        // 3. Fallback to candidate pre-seeded pool if still empty
        if (list.isEmpty()) {
            val fallbackPool = getPreSeededPlaylistSongs("fallback_$lower", cleanName, lang)
            list.addAll(fallbackPool)
        }

        return com.saavn.music.util.RelevanceEngine.deduplicateSongs(list).take(45)
    }

    fun autoAddRelatedSongsToPlaylist(playlistId: String, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _playlistDetail.value = _playlistDetail.value?.copy(isLoading = true)
            }
            val lang = _preferredLanguages.value.firstOrNull() ?: "tamil"
            val songs = fetchRelatedSongsForPlaylistName(title, lang)
            if (songs.isNotEmpty()) {
                val isCustom = localStorage.playlists.value.any { it.id == playlistId }
                if (isCustom) {
                    localStorage.addSongsToPlaylist(playlistId, songs)
                }
                val currentSongs = _playlistDetail.value?.songs ?: emptyList()
                val merged = com.saavn.music.util.RelevanceEngine.deduplicateSongs(currentSongs + songs)
                val cacheKey = "${playlistId.lowercase().trim()}_${lang.lowercase().trim()}"
                playlistSongsCache[cacheKey] = merged

                val pl = localStorage.playlists.value.find { it.id == playlistId }
                if (pl != null && pl.isPublic) {
                    syncPublicPlaylistToFirebase(pl)
                }
                withContext(Dispatchers.Main) {
                    if (_playlistDetail.value?.id == playlistId) {
                        _playlistDetail.value = _playlistDetail.value?.copy(
                            songs = if (isCustom) (pl?.songs ?: merged) else merged,
                            isLoading = false
                        )
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (_playlistDetail.value?.id == playlistId) {
                        _playlistDetail.value = _playlistDetail.value?.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private suspend fun preWarmFeaturedPlaylists(language: String) {
        val lang = language.lowercase().trim()
        val popular = listOf(
            "romance-$lang" to "$lang romantic love melody songs",
            "kuthu-$lang" to "$lang kuthu dance party songs",
            "chill-$lang" to "$lang acoustic chill lofi songs",
            "mass-$lang" to "$lang mass gym workout motivation songs",
            "90s-$lang" to "$lang 90s golden melody super hits"
        )
        for ((id, query) in popular) {
            val cacheKey = "${id}_$lang"
            if (playlistSongsCache.containsKey(cacheKey)) continue
            try {
                val songs = musicRepo.search(query, limit = 35).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                if (songs.isNotEmpty()) {
                    playlistSongsCache[cacheKey] = com.saavn.music.util.RelevanceEngine.deduplicateSongs(songs)
                }
            } catch (_: Exception) {}
        }
    }

    fun openCustomPlaylist(playlist: UserPlaylist) {
        if (_currentScreen.value != AppScreen.PLAYLIST_DETAIL) {
            previousScreen = _currentScreen.value
        }
        val isCreator = (playlist.creatorId.isNotBlank() && playlist.creatorId == getCurrentUserId()) ||
                (playlist.creatorName.isNotBlank() && playlist.creatorName.equals(getCurrentUserName(), ignoreCase = true))
        val displayCreator = if (isCreator) {
            "You"
        } else if (playlist.creatorName.isNotBlank()) {
            playlist.creatorName
        } else {
            "ISAI Listener"
        }

        val lang = _preferredLanguages.value.firstOrNull() ?: "tamil"
        val cacheKey = "${playlist.id.lowercase().trim()}_${lang.lowercase().trim()}"
        val cached = playlistSongsCache[cacheKey] ?: emptyList()
        val preSeeded = if (playlist.songs.isEmpty() && cached.isEmpty()) getPreSeededPlaylistSongs(playlist.id, playlist.name, lang) else emptyList()
        val effectiveSongs = if (playlist.songs.isNotEmpty()) playlist.songs else (if (cached.isNotEmpty()) cached else preSeeded)
        val hasExistingSongs = effectiveSongs.isNotEmpty()

        _playlistDetail.value = PlaylistDetailState(
            id = playlist.id,
            title = playlist.name,
            subtitle = if (hasExistingSongs) "${effectiveSongs.size} tracks" else "Finding songs for \"${playlist.name}\"...",
            language = lang,
            coverUrl = effectiveSongs.firstOrNull()?.thumbnailUrl ?: "",
            searchQuery = playlist.name,
            songs = effectiveSongs,
            isLoading = !hasExistingSongs,
            creatorName = displayCreator,
            isPublic = playlist.isPublic,
            isCustomPlaylist = true
        )
        _currentScreen.value = AppScreen.PLAYLIST_DETAIL

        if (playlist.songs.isEmpty()) {
            // Automatically find and add songs related to the playlist name!
            autoAddRelatedSongsToPlaylist(playlist.id, playlist.name)
        }
    }

    fun loadPlaylistSongs(
        playlistId: String,
        title: String,
        searchQuery: String,
        language: String,
        autoSaveToCustomPlaylist: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val idNorm = playlistId.lowercase().trim()
                val lang = language.ifBlank { "tamil" }.lowercase().trim()
                val cacheKey = "${idNorm}_$lang"

                val queryText = if (searchQuery.isNotBlank()) searchQuery else title

                // Kadhal Vibes: Instant 50 fixed songs without external search
                if (idNorm == "kadhal-vibes" || queryText.lowercase().contains("kadhal vibes")) {
                    playlistSongsCache[cacheKey] = KadhalVibesCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = KadhalVibesCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, KadhalVibesCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // Idhayam Pesuthey: Instant 50 fixed songs without external search
                if (idNorm == "idhayam-pesuthey" || queryText.lowercase().contains("idhayam pesuthey")) {
                    playlistSongsCache[cacheKey] = IdhayamPesutheyCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = IdhayamPesutheyCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, IdhayamPesutheyCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // Semma Kuthu: Instant 50 fixed songs without external search
                if (idNorm == "semma-kuthu" || queryText.lowercase().contains("semma kuthu")) {
                    playlistSongsCache[cacheKey] = SemmaKuthuCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = SemmaKuthuCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, SemmaKuthuCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // Mass Mode: Instant 50 fixed songs without external search
                if (idNorm == "mass-mode" || queryText.lowercase().contains("mass mode")) {
                    playlistSongsCache[cacheKey] = MassModeCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = MassModeCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, MassModeCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // Gaana Pettai: Instant 50 fixed songs without external search
                if (idNorm == "gaana-pettai" || queryText.lowercase().contains("gaana pettai")) {
                    playlistSongsCache[cacheKey] = GaanaPettaiCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = GaanaPettaiCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, GaanaPettaiCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // Jannal Ora Payanam: Instant 50 fixed songs without external search
                if (idNorm == "jannal-ora-payanam" || queryText.lowercase().contains("jannal ora payanam")) {
                    playlistSongsCache[cacheKey] = JannalOraPayanamCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = JannalOraPayanamCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, JannalOraPayanamCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // Iravu Melodies: Instant 50 fixed songs without external search
                if (idNorm == "iravu-melodies" || queryText.lowercase().contains("iravu melodies") || queryText.lowercase().contains("iravu melody")) {
                    playlistSongsCache[cacheKey] = IravuMelodiesCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = IravuMelodiesCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, IravuMelodiesCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // Pudhu Udhayam: Instant 50 fixed songs without external search
                if (idNorm == "pudhu-udhayam" || queryText.lowercase().contains("pudhu udhayam")) {
                    playlistSongsCache[cacheKey] = PudhuUdhayamCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = PudhuUdhayamCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, PudhuUdhayamCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // Irai Isai: Instant 50 fixed songs without external search
                if (idNorm == "irai-isai" || queryText.lowercase().contains("irai isai")) {
                    playlistSongsCache[cacheKey] = IraiIsaiCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = IraiIsaiCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, IraiIsaiCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                // School Days Memories: Instant 50 fixed songs without external search
                if (idNorm == "school-days-memories" || queryText.lowercase().contains("school days memories") || queryText.lowercase().contains("school days")) {
                    playlistSongsCache[cacheKey] = SchoolDaysMemoriesCatalog.SONGS
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = SchoolDaysMemoriesCatalog.SONGS,
                                isLoading = false
                            )
                        }
                    }
                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, SchoolDaysMemoriesCatalog.SONGS)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                    return@launch
                }

                val fetched = fetchRelatedSongsForPlaylistName(queryText, lang)
                val cleanResults = com.saavn.music.util.RelevanceEngine.deduplicateSongs(fetched)

                if (cleanResults.isNotEmpty()) {
                    playlistSongsCache[cacheKey] = cleanResults
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == playlistId) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = cleanResults,
                                isLoading = false
                            )
                        }
                    }

                    if (autoSaveToCustomPlaylist) {
                        localStorage.addSongsToPlaylist(playlistId, cleanResults)
                        val pl = localStorage.playlists.value.find { it.id == playlistId }
                        if (pl != null && pl.isPublic) {
                            syncPublicPlaylistToFirebase(pl)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYLIST", "Error loading playlist songs", e)
            } finally {
                withContext(Dispatchers.Main) {
                    if (_playlistDetail.value?.id == playlistId && _playlistDetail.value?.isLoading == true) {
                        _playlistDetail.value = _playlistDetail.value?.copy(isLoading = false)
                    }
                }
            }
        }
    }

    fun playPlaylistAll(shuffle: Boolean = false) {
        val songs = _playlistDetail.value?.songs ?: return
        if (songs.isEmpty()) return
        val list = if (shuffle) songs.shuffled() else songs
        playSong(list.first(), list, openFullPlayer = false)
    }

    fun closePlaylistDetail() {
        _playlistDetail.value = null
        _currentScreen.value = if (previousScreen == AppScreen.PLAYLIST_DETAIL) AppScreen.HOME else previousScreen
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
                val fullPool = deduped.distinctBy { it.videoId }
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
                        val songs = if (rawSongs.size < 10) {
                            val cleanQ = query.replace("2025", "").replace("2026", "").trim()
                            val ytSongs = ytRepo.searchSongs(cleanQ, maxResults = 30).getOrDefault(emptyList())
                            (rawSongs + ytSongs).distinctBy { it.videoId }
                        } else {
                            rawSongs
                        }
                        val dedupedSongs = ytRepo.deduplicateSongs(songs).take(30)
                        val cover = dedupedSongs.firstOrNull()?.thumbnailUrl ?: "https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg"
                        if (dedupedSongs.isNotEmpty()) {
                            IsaiDailyMix(
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
                    _isaiDailyMixes.value = loadedMixes
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

    fun searchMatchingPlaylists(query: String, langFilter: String, sampleSongs: List<YouTubeSong>): List<SearchPlaylistItem> {
        if (query.isBlank()) return emptyList()
        val clean = query.trim().lowercase()
        val cleanTokens = clean.split(" ", "_", "-", ",", "&").filter { it.length > 1 }
        val results = mutableListOf<SearchPlaylistItem>()

        // 1. User's Own Playlists + Community Public Playlists
        val allUserPlaylists = (localStorage.playlists.value + _publicPlaylists.value).distinctBy { it.id }
        for (pl in allUserPlaylists) {
            val nameNorm = pl.name.lowercase()
            val creatorNorm = pl.creatorName.lowercase()
            val isMatch = cleanTokens.isEmpty() || cleanTokens.any { token -> nameNorm.contains(token) || creatorNorm.contains(token) }
            if (isMatch) {
                results.add(
                    SearchPlaylistItem(
                        id = pl.id,
                        title = pl.name,
                        subtitle = if (pl.isPublic) "🌐 Public by ${pl.creatorName.ifBlank { "User" }} • ${pl.songs.size} tracks" else "🔒 Private • ${pl.songs.size} tracks",
                        coverUrl = pl.songs.firstOrNull()?.thumbnailUrl ?: "",
                        songCount = pl.songs.size,
                        isUserPlaylist = true,
                        creatorName = pl.creatorName.ifBlank { "User" },
                        isPublic = pl.isPublic,
                        userPlaylist = pl
                    )
                )
            }
        }

        // 2. Curated Editorial Catalog
        for (item in EditorialPlaylistsCatalog.ALL_PLAYLISTS) {
            val tNorm = item.title.lowercase()
            val sNorm = item.searchQuery.lowercase()
            val lNorm = item.language.lowercase()

            val matchesTokens = cleanTokens.any { token ->
                tNorm.contains(token) || sNorm.contains(token)
            }
            val matchesLang = langFilter == "All" || lNorm.contains(langFilter.lowercase())

            if (matchesTokens && matchesLang) {
                results.add(item)
            }
        }

        // 3. Dynamic Smart Mixes tailored specifically for this query
        val displayTitle = query.trim().split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        val fallbackCover1 = sampleSongs.getOrNull(0)?.thumbnailUrl ?: "https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg"
        val fallbackCover2 = sampleSongs.getOrNull(1)?.thumbnailUrl ?: fallbackCover1
        val fallbackCover3 = sampleSongs.getOrNull(2)?.thumbnailUrl ?: fallbackCover1

        val queryMixId1 = "search_mix_hits_${clean.replace(Regex("[^a-z0-9]"), "_")}"
        if (results.none { it.id == queryMixId1 }) {
            results.add(
                SearchPlaylistItem(
                    id = queryMixId1,
                    title = "$displayTitle • Best of Hits 🎶",
                    subtitle = "Curated Mix • ISAI Editorial • 35+ Songs",
                    coverUrl = fallbackCover1,
                    searchQuery = "$clean super hit songs",
                    language = langFilter.takeIf { it != "All" } ?: ""
                )
            )
        }

        val queryMixId2 = "search_mix_melodies_${clean.replace(Regex("[^a-z0-9]"), "_")}"
        if (results.none { it.id == queryMixId2 }) {
            results.add(
                SearchPlaylistItem(
                    id = queryMixId2,
                    title = "$displayTitle • Radio & Melodies 📻",
                    subtitle = "Soulful melodies & classic tracks",
                    coverUrl = fallbackCover2,
                    searchQuery = "$clean romantic melody songs",
                    language = langFilter.takeIf { it != "All" } ?: ""
                )
            )
        }

        val queryMixId3 = "search_mix_party_${clean.replace(Regex("[^a-z0-9]"), "_")}"
        if (results.none { it.id == queryMixId3 }) {
            results.add(
                SearchPlaylistItem(
                    id = queryMixId3,
                    title = "$displayTitle • Mass & Party Mix 🔥",
                    subtitle = "High energy beats & fast dance hits",
                    coverUrl = fallbackCover3,
                    searchQuery = "$clean mass dance party songs",
                    language = langFilter.takeIf { it != "All" } ?: ""
                )
            )
        }

        return results.distinctBy { it.id }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            _searchPlaylists.value = emptyList()
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
                val activeLangFilter = _searchLanguageFilter.value

                // Special direct commands
                if (clean == "trending" || clean == "trends" || clean == "charts") {
                    _searchResults.value = _trendingSongs.value
                    _searchPlaylists.value = searchMatchingPlaylists(newQuery, activeLangFilter, _trendingSongs.value)
                    _searchError.value = null
                    return@launch
                }

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
                    _searchPlaylists.value = searchMatchingPlaylists(newQuery, activeLangFilter, finalResults)
                    _searchError.value = null
                } else {
                    _searchResults.value = emptyList()
                    _searchPlaylists.value = searchMatchingPlaylists(newQuery, activeLangFilter, emptyList())
                    _searchError.value = "No songs found for '$newQuery'"
                }
            } catch (e: Exception) {
                try {
                    val result = ytRepo.searchSongs(newQuery)
                    val ytSongs = result.getOrDefault(emptyList()).filter { !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) }
                    _searchResults.value = ytSongs
                    _searchPlaylists.value = searchMatchingPlaylists(newQuery, _searchLanguageFilter.value, ytSongs)
                    _searchError.value = null
                } catch (_: Exception) {
                    _searchResults.value = emptyList()
                    _searchPlaylists.value = searchMatchingPlaylists(newQuery, _searchLanguageFilter.value, emptyList())
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
                    curated.title.equals(song.title, ignoreCase = true)
                }?.audioUrl
            }

        if (!inMemoryAudioUrl.isNullOrBlank()) {
            audioUrlCache[song.videoId] = inMemoryAudioUrl
        }
        val immediateSong = if (!inMemoryAudioUrl.isNullOrBlank()) song.copy(audioUrl = inMemoryAudioUrl) else song

        // Fast initial queue so player starts with 0ms UI delay
        val isAdvancingInCurrentPlaybackQueue = !queue.isNullOrEmpty() &&
            queue.any { it.videoId == song.videoId } &&
            (queue == ytPlayerController.playbackQueue.value || ytPlayerController.playbackQueue.value.any { it.videoId == song.videoId })

        val targetSongLang = song.language.ifBlank { com.saavn.music.util.RelevanceEngine.detectSongLanguage(song) }.ifBlank { _preferredLanguages.value.firstOrNull() ?: "tamil" }
        val hasExplicitQueue = !queue.isNullOrEmpty()
        val initialQueue = if (isAdvancingInCurrentPlaybackQueue && queue == ytPlayerController.playbackQueue.value) {
            queue!!
        } else if (hasExplicitQueue && queue!!.size > 1) {
            recommendationGeneration++
            sessionSeedSong = song
            val explicitQ = queue!!
            val songIdx = explicitQ.indexOfFirst { it.videoId == song.videoId }
            if (songIdx >= 0) explicitQ else (listOf(immediateSong) + explicitQ).distinctBy { it.videoId }
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

        val currentQueueIdx = initialQueue.indexOfFirst { it.videoId == immediateSong.videoId }.coerceAtLeast(0)

        // Claim ISAI Connect active device status and sync song details and queue to Firebase
        if (!localStorage.isMultiDevicePlaybackSeparate.value) {
            isaiConnectManager.updatePlaybackState(
                song = immediateSong,
                isPlaying = true,
                positionMs = (startPositionSec * 1000).toLong(),
                currentDeviceId = isaiConnectManager.deviceId,
                queue = initialQueue,
                queueIndex = currentQueueIdx
            )
        }

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
                    } else {
                        android.util.Log.w("ISAI_PLAYER", "[MainViewModel] Audio stream could not be resolved for '${song.title}'. Auto-advancing to next track...")
                        ytPlayerController.playNext()
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
                val cleanTitle = song.title
                    .replace(Regex("\\s*[|•].*$"), "")
                    .replace(Regex("\\s*\\(.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\)", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*\\[.*?(official|video|audio|lyrics|hd|4k|song|teaser|trailer|promo).*?\\]", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("(?i)\\b(official\\s*(video|audio|lyric(al)?\\s*video)?|lyric(al)?\\s*video|video\\s*song|full\\s*song|audio\\s*song|hd\\s*video)\\b"), "")
                    .trim()

                val primaryArtist = com.saavn.music.util.RelevanceEngine.extractPrimaryArtist(song.channelTitle)

                // Exact Home screen "Similar to [Track]" query builder
                val dynamicQueries = mutableListOf<String>()
                if (cleanTitle.isNotBlank()) {
                    dynamicQueries.add("$cleanTitle $primaryLang")
                    dynamicQueries.add(com.saavn.music.util.RelevanceEngine.getRelevantSearchQuery(song, primaryLang))
                }
                if (primaryArtist.isNotBlank() && primaryArtist != "Tamil Artist") {
                    dynamicQueries.add("$primaryArtist $primaryLang hits")
                    dynamicQueries.add("$primaryArtist $primaryLang songs")
                }

                android.util.Log.i("ISAI_PLAYER", "[MainViewModel] Loading similar queue for: '${song.title}' with queries: $dynamicQueries")

                val searchDeferred = dynamicQueries.take(4).map { q ->
                    async {
                        musicRepo.search(q, limit = 25).getOrNull()?.map { it.toYouTubeSong() } ?: emptyList()
                    }
                }
                val rawFetched = searchDeferred.awaitAll().flatten()

                val pool = if (rawFetched.size < 15 && cleanTitle.isNotBlank()) {
                    val ytFallback = ytRepo.searchSongs("$cleanTitle $primaryLang", maxResults = 25).getOrDefault(emptyList())
                    (rawFetched + ytFallback).distinctBy { it.videoId }
                } else {
                    rawFetched.distinctBy { it.videoId }
                }.filter { !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) }

                if (!isActive || recommendationGeneration != currentGen) return@launch

                val currentQueue = ytPlayerController.playbackQueue.value
                val curIdx = ytPlayerController.currentQueueIndex.value
                val playedSoFar = currentQueue.take(curIdx + 1)

                val filteredPool = pool.filter { candidate ->
                    candidate.videoId != song.videoId &&
                    !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(candidate) &&
                    !com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(song, candidate) &&
                    playedSoFar.none { com.saavn.music.util.RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
                    com.saavn.music.util.RelevanceEngine.isSongInLanguage(candidate, activeLangs)
                }

                val candidateMatches = if (filteredPool.isNotEmpty()) {
                    com.saavn.music.util.RelevanceEngine.buildRelevantQueue(song, filteredPool, activeLangs, maxItems = 45, minItems = 20, sessionSeed = sessionSeedSong).filter { it.videoId != song.videoId && !com.saavn.music.util.RelevanceEngine.isPlaylistOrCompilation(it) }
                } else {
                    emptyList()
                }

                // Strictly ensure minimum 25 upcoming songs in the song's exact language and mood
                val relevantMatches = if (candidateMatches.size < 25) {
                    val backupPool = mutableListOf<YouTubeSong>()
                    backupPool.addAll(_trendingSongs.value)
                    backupPool.addAll(_mostPlayedSongs.value)
                    backupPool.addAll(_latestReleases.value)
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
            }

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

        val syncState = isaiConnectManager.playbackState.value
        val isRemoteActive = syncState != null && 
            syncState.currentDeviceId.isNotBlank() && 
            !isaiConnectManager.isMyDeviceActive() && 
            (syncState.isPlaying || Math.abs(System.currentTimeMillis() - syncState.updatedAt) < 15 * 60_000L)

        if (isRemoteActive && syncState != null) {
            // Debounce heavy remote network commands so network/Firebase is not flooded during continuous dragging
            volumeDebounceJob?.cancel()
            volumeDebounceJob = viewModelScope.launch(Dispatchers.IO) {
                delay(80)
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
        } else {
            // 2. Local device: Instantly apply hardware stream volume on IO dispatcher without delay (0ms latency!)
            viewModelScope.launch(Dispatchers.IO) {
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

            // 3. Debounce Listen Together room sync so we don't spam Firebase on every slider micro-tick
            if (listenTogetherManager.isHost()) {
                volumeDebounceJob?.cancel()
                volumeDebounceJob = viewModelScope.launch(Dispatchers.IO) {
                    delay(80)
                    listenTogetherManager.hostSetVolume(clamped)
                }
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

    fun getCurrentUserName(): String {
        val profile = userProfile.value
        val name = profile?.displayName?.trim()?.takeIf { it.isNotBlank() && !it.equals("JEEVA ⚡", ignoreCase = true) }
            ?: authService.getCurrentUser()?.displayName?.trim()?.takeIf { it.isNotBlank() && !it.equals("JEEVA ⚡", ignoreCase = true) }
            ?: authService.getCurrentUser()?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "ISAI Listener"
        return name
    }

    fun getCurrentUserId(): String {
        return userProfile.value?.id?.ifBlank { null }
            ?: authService.getCurrentUserId().ifBlank {
                try {
                    android.provider.Settings.Secure.getString(
                        getApplication<Application>().contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    ) ?: "user_${System.currentTimeMillis()}"
                } catch (_: Exception) {
                    "user_${System.currentTimeMillis()}"
                }
            }
    }

    private fun syncPublicPlaylistToFirebase(playlist: UserPlaylist) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rtdb = FirebaseDatabase.getInstance("https://isai-49b51-default-rtdb.firebaseio.com")
                val ref = rtdb.getReference("publicPlaylists").child(playlist.id)
                val data = hashMapOf<String, Any>(
                    "id" to playlist.id,
                    "name" to playlist.name,
                    "creatorId" to playlist.creatorId,
                    "creatorName" to playlist.creatorName.ifBlank { "ISAI Listener" },
                    "createdAt" to playlist.createdAt,
                    "isPublic" to true,
                    "songCount" to playlist.songs.size,
                    "coverUrl" to (playlist.songs.firstOrNull()?.thumbnailUrl ?: ""),
                    "songsJson" to gson.toJson(playlist.songs)
                )
                ref.setValue(data)
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYLISTS", "Failed to sync public playlist", e)
            }
        }
    }

    private fun removePublicPlaylistFromFirebase(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rtdb = FirebaseDatabase.getInstance("https://isai-49b51-default-rtdb.firebaseio.com")
                rtdb.getReference("publicPlaylists").child(playlistId).removeValue()
            } catch (e: Exception) {
                android.util.Log.e("ISAI_PLAYLISTS", "Failed to remove public playlist from Firebase", e)
            }
        }
    }

    fun listenToPublicPlaylists() {
        try {
            val rtdb = FirebaseDatabase.getInstance("https://isai-49b51-default-rtdb.firebaseio.com")
            val ref = rtdb.getReference("publicPlaylists").limitToLast(50)
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val list = mutableListOf<UserPlaylist>()
                        for (child in snapshot.children) {
                            try {
                                val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                                val name = child.child("name").getValue(String::class.java) ?: "Public Playlist"
                                val creatorId = child.child("creatorId").getValue(String::class.java) ?: ""
                                val creatorName = child.child("creatorName").getValue(String::class.java) ?: "ISAI User"
                                val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                                val songsJson = child.child("songsJson").getValue(String::class.java)
                                val songs: List<YouTubeSong> = if (!songsJson.isNullOrBlank()) {
                                    try {
                                        val type = object : TypeToken<List<YouTubeSong>>() {}.type
                                        gson.fromJson(songsJson, type) ?: emptyList()
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                } else emptyList()

                                list.add(
                                    UserPlaylist(
                                        id = id,
                                        name = name,
                                        songs = songs,
                                        createdAt = createdAt,
                                        isPublic = true,
                                        creatorId = creatorId,
                                        creatorName = creatorName
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                        _publicPlaylists.value = list.reversed()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.w("ISAI_PLAYLISTS", "Public playlists cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("ISAI_PLAYLISTS", "Failed to listen to public playlists", e)
        }
    }

    fun createPlaylist(name: String, isPublic: Boolean = false): UserPlaylist {
        val creatorName = getCurrentUserName()
        val creatorId = getCurrentUserId()
        val pl = localStorage.createPlaylist(
            name = name,
            isPublic = isPublic,
            creatorId = creatorId,
            creatorName = creatorName
        )
        if (isPublic) {
            syncPublicPlaylistToFirebase(pl)
        }

        // Auto-populate related songs matching the playlist name in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songs = fetchRelatedSongsForPlaylistName(name, _preferredLanguages.value.firstOrNull() ?: "tamil")
                if (songs.isNotEmpty()) {
                    localStorage.addSongsToPlaylist(pl.id, songs)
                    val updated = localStorage.playlists.value.find { it.id == pl.id }
                    if (isPublic && updated != null) {
                        syncPublicPlaylistToFirebase(updated)
                    }
                    withContext(Dispatchers.Main) {
                        if (_playlistDetail.value?.id == pl.id) {
                            _playlistDetail.value = _playlistDetail.value?.copy(
                                songs = updated?.songs ?: songs,
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return pl
    }

    fun addSongToPlaylist(playlistId: String, song: YouTubeSong) {
        localStorage.addSongToPlaylist(playlistId, song)
        val pl = localStorage.playlists.value.find { it.id == playlistId }
        if (pl != null && pl.isPublic) {
            syncPublicPlaylistToFirebase(pl)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, videoId: String) {
        localStorage.removeSongFromPlaylist(playlistId, videoId)
        val pl = localStorage.playlists.value.find { it.id == playlistId }
        if (pl != null && pl.isPublic) {
            syncPublicPlaylistToFirebase(pl)
        }
    }

    fun deletePlaylist(playlistId: String) {
        val pl = localStorage.playlists.value.find { it.id == playlistId }
            ?: _publicPlaylists.value.find { it.id == playlistId }
        if (pl != null && pl.isPublic) {
            removePublicPlaylistFromFirebase(playlistId)
        }
        localStorage.deletePlaylist(playlistId)
    }

    fun openAddToPlaylistDialog(song: YouTubeSong) {
        _showAddToPlaylistDialog.value = song
    }

    fun closeAddToPlaylistDialog() {
        _showAddToPlaylistDialog.value = null
    }

    fun setScreen(screen: AppScreen) {
        if (_currentScreen.value != AppScreen.PLAYLIST_DETAIL && screen != AppScreen.PLAYLIST_DETAIL) {
            previousScreen = _currentScreen.value
        }
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
