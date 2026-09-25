package com.saavn.music.player

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.saavn.music.data.local.LocalMusicStorage
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.service.MusicPlaybackService
import com.saavn.music.util.RelevanceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class YouTubePlayerController(
    private val context: Context,
    private val localStorage: LocalMusicStorage
) {

    companion object {
        @Volatile
        private var instance: YouTubePlayerController? = null

        fun getInstance(): YouTubePlayerController? = instance

        fun initialize(context: Context, localStorage: LocalMusicStorage): YouTubePlayerController {
            return instance ?: synchronized(this) {
                instance ?: YouTubePlayerController(context.applicationContext, localStorage).also {
                    instance = it
                }
            }
        }

        // High-fidelity digital headroom (-2.5 dBFS) to prevent inter-sample peak clipping / harsh distortion
        // on 320kbps lossy AAC/MP3 streams decoded to 16-bit PCM at >75% volume across all output devices
        const val DIGITAL_HEADROOM_GAIN = 0.75f

        @Volatile
        private var simpleCache: SimpleCache? = null

        fun getMediaCache(context: Context): SimpleCache {
            return simpleCache ?: synchronized(this) {
                simpleCache ?: run {
                    val cacheDir = java.io.File(context.cacheDir, "media_stream_cache")
                    val evictor = LeastRecentlyUsedCacheEvictor(250L * 1024 * 1024) // 250 MB LRU disk cache
                    val databaseProvider = StandaloneDatabaseProvider(context)
                    SimpleCache(cacheDir, evictor, databaseProvider).also {
                        simpleCache = it
                    }
                }
            }
        }
    }

    // Audio Attributes for continuous background playback and audio focus
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    // High-stability LoadControl: Buffers 60s ahead (up to 3 min), 300ms ultra-fast initial start, 1.5s rebuffer headroom
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 60_000,
            /* maxBufferMs = */ 180_000,
            /* bufferForPlaybackMs = */ 300,
            /* bufferForPlaybackAfterRebufferMs = */ 1_500
        )
        .setBackBuffer(
            /* backBufferDurationMs = */ 30_000,
            /* retainBackBufferFromKeyframe = */ false
        )
        .setPrioritizeTimeOverSizeThresholds(false)
        .build()

    // HTTP Data Source Factory with Mobile User-Agent, resilient timeouts, and Cross-Protocol Redirects
    private val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(25_000)
        .setKeepPostFor302Redirects(true)

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(getMediaCache(context))
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    private val loadErrorHandlingPolicy = androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(3)

    private val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheDataSourceFactory)
        .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

    // Native ExoPlayer for Direct 320kbps Audio Streams (NepoTune / JioSaavn)
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build().apply {
            volume = DIGITAL_HEADROOM_GAIN
        }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var progressTrackerJob: Job? = null
    private var isUsingExoPlayer = false

    // YouTube Player fallback for video ID if direct stream is not available
    private var activeYouTubePlayer: YouTubePlayer? = null

    private val _currentSong = MutableStateFlow<YouTubeSong?>(null)
    val currentSong: StateFlow<YouTubeSong?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0f)
    val currentPositionSec: StateFlow<Float> = _currentPositionSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0f)
    val durationSec: StateFlow<Float> = _durationSec.asStateFlow()

    private val _volume = MutableStateFlow(100)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    // Playback Queue
    private val _playbackQueue = MutableStateFlow<List<YouTubeSong>>(emptyList())
    val playbackQueue: StateFlow<List<YouTubeSong>> = _playbackQueue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _audioQuality = MutableStateFlow(com.saavn.music.data.model.AudioQuality.VERY_HIGH)
    val audioQuality: StateFlow<com.saavn.music.data.model.AudioQuality> = _audioQuality.asStateFlow()

    fun setQuality(quality: com.saavn.music.data.model.AudioQuality) {
        if (_audioQuality.value == quality) return
        _audioQuality.value = quality
        val song = _currentSong.value ?: return
        val currentPosMs = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying
        
        val url = song.audioUrl ?: ""
        val newUrl = when (quality.bitrate) {
            "96" -> url.replace("_320.", "_96.").replace("_160.", "_96.")
            "160" -> url.replace("_320.", "_160.").replace("_96.", "_160.")
            "320" -> url.replace("_96.", "_320.").replace("_160.", "_320.")
            else -> url
        }

        if (newUrl.isNotBlank() && isUsingExoPlayer) {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(newUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.seekTo(currentPosMs)
            if (wasPlaying) exoPlayer.play()
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    var onQueueExhausted: (() -> Unit)? = null
    var onTrackChangeRequested: ((YouTubeSong, List<YouTubeSong>) -> Unit)? = null
    var onPlaybackErrorFallback: ((YouTubeSong) -> Unit)? = null
    var isPlaybackRestricted: (() -> Boolean)? = null
    var onPlaybackReady: (() -> Unit)? = null
    var onAudioFocusLostOrCall: (() -> Unit)? = null

    init {
        instance = this
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (isUsingExoPlayer) {
                    _isPlaying.value = playing
                    notifyService(playing)
                    if (playing) {
                        _isBuffering.value = false
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }
            }

            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                if (!isUsingExoPlayer) return
                if (playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) {
                    Log.i("ISAI_PLAYER", "[YouTubePlayerController] Transient audio focus loss detected (call/interruption)")
                    onAudioFocusLostOrCall?.invoke()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!isUsingExoPlayer) return
                when (playbackState) {
                    Player.STATE_BUFFERING -> _isBuffering.value = true
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        val dur = exoPlayer.duration
                        if (dur > 0L) {
                            _durationSec.value = dur / 1000f
                        }
                        notifyService(_isPlaying.value)
                        onPlaybackReady?.invoke()
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        notifyService(false)
                        if (isPlaybackRestricted?.invoke() == true) {
                            Log.i("ISAI_PLAYER", "[YouTubePlayerController] Track ended on listener device. Waiting for Host to change song.")
                        } else {
                            coroutineScope.launch {
                                delay(100)
                                playNext()
                            }
                        }
                    }
                    else -> _isBuffering.value = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ISAI_PLAYER", "[YouTubePlayerController] ExoPlayer error: ${error.errorCodeName} (${error.errorCode}) - ${error.message}", error)
                val current = _currentSong.value
                val currentUrl = current?.audioUrl

                // 1. Bitrate fallback: 320 -> 160 -> 96
                if (!currentUrl.isNullOrBlank() && currentUrl.contains("_320.mp4")) {
                    val fallback160 = currentUrl.replace("_320.mp4", "_160.mp4")
                    Log.w("ISAI_PLAYER", "[YouTubePlayerController] Bitrate fallback 320 -> 160: $fallback160")
                    playDirectAudioUrl(fallback160)
                    return
                } else if (!currentUrl.isNullOrBlank() && currentUrl.contains("_160.mp4")) {
                    val fallback96 = currentUrl.replace("_160.mp4", "_96.mp4")
                    Log.w("ISAI_PLAYER", "[YouTubePlayerController] Bitrate fallback 160 -> 96: $fallback96")
                    playDirectAudioUrl(fallback96)
                    return
                }

                // Direct stream failed after bitrate fallbacks
                if (isUsingExoPlayer) {
                    isUsingExoPlayer = false
                    _isBuffering.value = false
                    _isPlaying.value = false
                    notifyService(false)
                    if (current != null && activeYouTubePlayer != null) {
                        Log.i("ISAI_PLAYER", "[YouTubePlayerController] Falling back to YouTube video ID: ${current.videoId}")
                        activeYouTubePlayer?.loadVideo(current.videoId, 0f)
                    } else if (current != null) {
                        Log.w("ISAI_PLAYER", "[YouTubePlayerController] Triggering onPlaybackErrorFallback for: '${current.title}'")
                        onPlaybackErrorFallback?.invoke(current)
                    }
                }
            }
        })
    }

    fun notifyService(isPlaying: Boolean = _isPlaying.value) {
        val song = _currentSong.value ?: return
        try {
            MusicPlaybackService.startOrUpdate(
                context = context,
                song = song,
                isPlaying = isPlaying,
                durationSec = _durationSec.value,
                positionSec = _currentPositionSec.value
            )
        } catch (e: Exception) {
            Log.w("ISAI_PLAYER", "Failed to update notification service: ${e.message}")
        }
    }

    fun getActivePlayer(): YouTubePlayer? = activeYouTubePlayer
    fun isDirectAudio(): Boolean = isUsingExoPlayer

    fun setBuffering(buffering: Boolean) {
        if (!isUsingExoPlayer) {
            _isBuffering.value = buffering
        }
    }

    fun setPlaying(playing: Boolean) {
        if (!isUsingExoPlayer) {
            _isPlaying.value = playing
        }
    }

    // Connects to the active Android YouTube Player View instance (Fallback)
    fun registerPlayer(player: YouTubePlayer) {
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] registerPlayer called! Player instance attached.")
        activeYouTubePlayer = player

        player.addListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                Log.i("ISAI_PLAYER", "[YouTubePlayerController] onReady received! currentSong='${_currentSong.value?.title}'")
                if (!isUsingExoPlayer) {
                    _isBuffering.value = false
                    _currentSong.value?.let { song ->
                        youTubePlayer.loadVideo(song.videoId, _currentPositionSec.value)
                    }
                }
            }

            override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                if (isUsingExoPlayer) return
                Log.i("ISAI_PLAYER", "[YouTubePlayerController] YouTube state changed: $state")
                when (state) {
                    PlayerConstants.PlayerState.PLAYING -> {
                        _isPlaying.value = true
                        _isBuffering.value = false
                        notifyService(true)
                    }
                    PlayerConstants.PlayerState.PAUSED -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        notifyService(false)
                    }
                    PlayerConstants.PlayerState.BUFFERING -> {
                        _isBuffering.value = true
                    }
                    PlayerConstants.PlayerState.ENDED -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        notifyService(false)
                        if (isPlaybackRestricted?.invoke() == true) {
                            Log.i("ISAI_PLAYER", "[YouTubePlayerController] YT track ended on listener device. Waiting for Host.")
                        } else {
                            playNext()
                        }
                    }
                    else -> {
                        _isBuffering.value = false
                    }
                }
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                if (!isUsingExoPlayer) {
                    _currentPositionSec.value = second
                }
            }

            override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                if (!isUsingExoPlayer && duration > 0f) {
                    _durationSec.value = duration
                    notifyService(_isPlaying.value)
                }
            }

            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                if (!isUsingExoPlayer) {
                    Log.e("ISAI_PLAYER", "[YouTubePlayerController] YouTube onError: $error")
                    _isBuffering.value = false
                    _isPlaying.value = false
                    notifyService(false)
                }
            }
        })
    }

    fun prepareForPlayback(song: YouTubeSong, queue: List<YouTubeSong>? = null) {
        _currentSong.value = song
        val targetQueue = when {
            queue != null -> queue
            _playbackQueue.value.any { it.videoId == song.videoId } -> _playbackQueue.value
            _playbackQueue.value.isNotEmpty() -> _playbackQueue.value + song
            else -> listOf(song)
        }
        _playbackQueue.value = targetQueue
        val idx = targetQueue.indexOfFirst { it.videoId == song.videoId }
        _currentQueueIndex.value = if (idx >= 0) idx else 0
        _isBuffering.value = true
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] prepareForPlayback: '${song.title}' at index ${_currentQueueIndex.value}")
    }

    fun playDirectAudioUrl(url: String, startPositionSec: Float = 0f, autoPlay: Boolean = true) {
        try {
            isUsingExoPlayer = true
            _isBuffering.value = true
            activeYouTubePlayer?.pause()
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] >>> Playing 320kbps HD Audio via ExoPlayer at ${startPositionSec}s (autoPlay=$autoPlay): $url <<<")
            val mediaItem = MediaItem.fromUri(url)
            if (startPositionSec > 0f) {
                exoPlayer.setMediaItem(mediaItem, (startPositionSec * 1000).toLong())
            } else {
                exoPlayer.setMediaItem(mediaItem, true)
            }
            exoPlayer.prepare()
            exoPlayer.playWhenReady = autoPlay
            if (autoPlay) {
                startProgressTracker()
                notifyService(true)
            } else {
                notifyService(false)
            }
            saveCurrentSession(wasPlaying = autoPlay)
        } catch (e: Exception) {
            Log.e("ISAI_PLAYER", "[YouTubePlayerController] playDirectAudioUrl failed: ${e.message}", e)
        }
    }

    fun playSong(song: YouTubeSong, queue: List<YouTubeSong>? = null, startPositionSec: Float = 0f, autoPlay: Boolean = true) {
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] playSong: '${song.title}' (${song.videoId}) | startPos=${startPositionSec}s | audioUrl=${song.audioUrl}")
        val targetQueue = when {
            queue != null -> queue
            _playbackQueue.value.any { it.videoId == song.videoId } -> _playbackQueue.value
            _playbackQueue.value.isNotEmpty() -> _playbackQueue.value + song
            else -> listOf(song)
        }
        _playbackQueue.value = targetQueue
        val idx = targetQueue.indexOfFirst { it.videoId == song.videoId }
        _currentQueueIndex.value = if (idx >= 0) idx else 0
        _currentPositionSec.value = startPositionSec
        _isBuffering.value = true

        localStorage.addRecentlyPlayed(song)

        var audioUrlToPlay = song.audioUrl
        if (audioUrlToPlay.isNullOrBlank()) {
            val matchedCurated = com.saavn.music.data.repository.YouTubeMusicRepository.CURATED_TAMIL_SONGS.firstOrNull { curated ->
                curated.videoId == song.videoId ||
                curated.title.equals(song.title, ignoreCase = true)
            }
            audioUrlToPlay = matchedCurated?.audioUrl
        }

        if (!audioUrlToPlay.isNullOrBlank()) {
            val songWithAudio = if (song.audioUrl != audioUrlToPlay) song.copy(audioUrl = audioUrlToPlay) else song
            _currentSong.value = songWithAudio
            playDirectAudioUrl(audioUrlToPlay, startPositionSec, autoPlay)
        } else {
            _currentSong.value = song
            notifyService(false)
            isUsingExoPlayer = false
            exoPlayer.stop()
            stopProgressTracker()
            if (activeYouTubePlayer != null) {
                Log.i("ISAI_PLAYER", "[YouTubePlayerController] Loading into activeYouTubePlayer at ${startPositionSec}s: ${song.videoId}")
                if (autoPlay) {
                    activeYouTubePlayer?.loadVideo(song.videoId, startPositionSec)
                } else {
                    activeYouTubePlayer?.cueVideo(song.videoId, startPositionSec)
                }
            } else {
                Log.w("ISAI_PLAYER", "[YouTubePlayerController] activeYouTubePlayer is NULL and direct audio stream unavailable for: ${song.title}")
                _isBuffering.value = false
            }
        }
    }

    fun pause() {
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] pause() requested (isUsingExoPlayer=$isUsingExoPlayer)")
        saveCurrentSession(wasPlaying = false)
        try {
            exoPlayer.pause()
        } catch (e: Exception) {
            Log.w("ISAI_PLAYER", "ExoPlayer pause warning: ${e.message}")
        }
        try {
            activeYouTubePlayer?.pause()
        } catch (e: Exception) {
            Log.w("ISAI_PLAYER", "activeYouTubePlayer pause warning: ${e.message}")
        }
        _isPlaying.value = false
        notifyService(false)
    }

    fun play() {
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] play() requested")
        if (isUsingExoPlayer) {
            if (exoPlayer.mediaItemCount == 0 && _currentSong.value != null) {
                playSong(_currentSong.value!!, _playbackQueue.value, _currentPositionSec.value)
                return
            }
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        } else {
            if (activeYouTubePlayer != null) {
                activeYouTubePlayer?.play()
            } else if (_currentSong.value != null) {
                playSong(_currentSong.value!!, _playbackQueue.value, _currentPositionSec.value)
                return
            }
        }
        _isPlaying.value = true
        notifyService(true)
    }

    fun togglePlayPause() {
        if (isPlaybackRestricted?.invoke() == true) {
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] togglePlayPause blocked: device is a listener in room")
            return
        }
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] togglePlayPause: isUsingExoPlayer=$isUsingExoPlayer, isPlaying=${_isPlaying.value}")
        if (isUsingExoPlayer) {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                if (exoPlayer.mediaItemCount == 0 && _currentSong.value != null) {
                    playSong(_currentSong.value!!, _playbackQueue.value, _currentPositionSec.value)
                } else {
                    if (exoPlayer.playbackState == Player.STATE_IDLE) {
                        exoPlayer.prepare()
                    }
                    exoPlayer.play()
                }
            }
            return
        }

        val player = activeYouTubePlayer
        if (player == null) {
            val current = _currentSong.value
            if (current != null) {
                Log.i("ISAI_PLAYER", "[YouTubePlayerController] activeYouTubePlayer is null, replaying song: ${current.title}")
                playSong(current, _playbackQueue.value, _currentPositionSec.value)
            } else {
                Log.e("ISAI_PLAYER", "[YouTubePlayerController] ERROR: activeYouTubePlayer is NULL and currentSong is null")
            }
            return
        }
        if (_isPlaying.value) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(seconds: Float) {
        if (isPlaybackRestricted?.invoke() == true) {
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] seekTo blocked: device is a listener in room")
            return
        }
        _currentPositionSec.value = seconds
        if (isUsingExoPlayer) {
            exoPlayer.seekTo((seconds * 1000).toLong())
        } else {
            activeYouTubePlayer?.seekTo(seconds)
        }
        saveCurrentSession(wasPlaying = isPlaying.value)
    }

    fun syncSeekTo(seconds: Float) {
        _currentPositionSec.value = seconds
        if (isUsingExoPlayer) {
            exoPlayer.seekTo((seconds * 1000).toLong())
        } else {
            activeYouTubePlayer?.seekTo(seconds)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        if (isUsingExoPlayer) {
            exoPlayer.setPlaybackSpeed(clamped)
        }
    }

    fun setVolume(volumePercent: Int) {
        if (isPlaybackRestricted?.invoke() == true) {
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] setVolume blocked: device is a listener in room")
            return
        }
        applyVolume(volumePercent)
    }

    fun updateSystemVolume(volumePercent: Int) {
        applyVolume(volumePercent)
    }

    private fun applyVolume(volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        _volume.value = clamped

        // Scale digital player audio volume smoothly (0.0f to DIGITAL_HEADROOM_GAIN)
        val normalized = (clamped / 100f) * DIGITAL_HEADROOM_GAIN
        if (isUsingExoPlayer) {
            exoPlayer.volume = if (clamped == 0) 0f else normalized
        } else {
            activeYouTubePlayer?.setVolume((clamped * DIGITAL_HEADROOM_GAIN).toInt())
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    private val userQueuedSongIds = mutableSetOf<String>()

    fun isUserQueued(videoId: String): Boolean = userQueuedSongIds.contains(videoId)

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun addToQueue(song: YouTubeSong) {
        if (RelevanceEngine.isPlaylistOrCompilation(song)) {
            Log.w("ISAI_PLAYER", "[YouTubePlayerController] Rejected playlist/compilation from queue: '${song.title}'")
            return
        }
        val manualSong = song.copy(isManual = true)
        userQueuedSongIds.add(manualSong.videoId)
        val list = _playbackQueue.value.toMutableList()
        list.removeAll { it.videoId == manualSong.videoId || RelevanceEngine.isSameSongOrDuplicate(it, manualSong) }
        val currentSongId = _currentSong.value?.videoId
        val curIdx = if (!currentSongId.isNullOrBlank()) {
            val idx = list.indexOfFirst { it.videoId == currentSongId }
            if (idx >= 0) idx else _currentQueueIndex.value
        } else {
            _currentQueueIndex.value
        }

        // Find the last index of existing manually queued items after curIdx
        var lastManualIdx = -1
        for (i in (curIdx + 1) until list.size) {
            val item = list[i]
            if (item.isManual || userQueuedSongIds.contains(item.videoId)) {
                lastManualIdx = i
            }
        }
        val insertPos = if (lastManualIdx >= 0) (lastManualIdx + 1).coerceIn(0, list.size) else (curIdx + 1).coerceIn(0, list.size)
        list.add(insertPos, manualSong)
        _playbackQueue.value = RelevanceEngine.deduplicateSongs(list)
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] Added to queue after manual items at $insertPos: '${manualSong.title}', total=${_playbackQueue.value.size}")
    }

    fun appendQueue(songs: List<YouTubeSong>) {
        if (songs.isEmpty()) return
        val list = _playbackQueue.value.toMutableList()
        val deduppedCandidates = RelevanceEngine.deduplicateSongs(songs)
        var addedCount = 0
        deduppedCandidates.forEach { song ->
            if (list.none { RelevanceEngine.isSameSongOrDuplicate(it, song) }) {
                list.add(song)
                addedCount++
            }
        }
        if (addedCount > 0) {
            _playbackQueue.value = list
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] Appended $addedCount new distinct songs to queue, total=${list.size}")
        }
    }

    fun replaceUpcomingQueue(newUpcoming: List<YouTubeSong>) {
        val current = _playbackQueue.value
        val curIdx = _currentQueueIndex.value
        val playedSoFar = current.take(curIdx + 1)
        
        // Preserve any upcoming items that were explicitly added by the user in their chosen order
        val upcomingUserAdded = current.drop(curIdx + 1).filter { s -> s.isManual || userQueuedSongIds.contains(s.videoId) }

        val deduppedNew = RelevanceEngine.deduplicateSongs(newUpcoming)

        // Filter out any song that was already played in playedSoFar or in user added
        val filteredNew = deduppedNew.filter { candidate ->
            playedSoFar.none { RelevanceEngine.isSameSongOrDuplicate(it, candidate) } &&
            upcomingUserAdded.none { RelevanceEngine.isSameSongOrDuplicate(it, candidate) }
        }

        if (filteredNew.isNotEmpty() || upcomingUserAdded.isNotEmpty()) {
            val combined = RelevanceEngine.deduplicateSongs(playedSoFar + upcomingUserAdded + filteredNew)
            _playbackQueue.value = combined
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] Replaced upcoming queue with ${filteredNew.size} mood-matched songs (preserved ${upcomingUserAdded.size} user items), total=${combined.size}")
        }
    }

    fun setPlaybackQueue(songs: List<YouTubeSong>, newIndex: Int = _currentQueueIndex.value) {
        val deduped = songs.distinctBy { it.videoId }
        _playbackQueue.value = deduped
        _currentQueueIndex.value = newIndex.coerceIn(0, (deduped.size - 1).coerceAtLeast(0))
        val validIds = deduped.map { it.videoId }.toSet()
        userQueuedSongIds.retainAll(validIds)
    }

    fun playNextInQueue(song: YouTubeSong) {
        if (RelevanceEngine.isPlaylistOrCompilation(song)) {
            Log.w("ISAI_PLAYER", "[YouTubePlayerController] Rejected playlist/compilation from Play Next: '${song.title}'")
            return
        }
        val manualSong = song.copy(isManual = true)
        userQueuedSongIds.add(manualSong.videoId)
        val list = _playbackQueue.value.toMutableList()
        list.removeAll { it.videoId == manualSong.videoId || RelevanceEngine.isSameSongOrDuplicate(it, manualSong) }
        val currentSongId = _currentSong.value?.videoId
        val curIdx = if (!currentSongId.isNullOrBlank()) {
            val idx = list.indexOfFirst { it.videoId == currentSongId }
            if (idx >= 0) idx else _currentQueueIndex.value
        } else {
            _currentQueueIndex.value
        }
        val insertPos = (curIdx + 1).coerceIn(0, list.size)
        list.add(insertPos, manualSong)
        _playbackQueue.value = RelevanceEngine.deduplicateSongs(list)
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] Play Next inserted immediately at $insertPos: '${manualSong.title}', total=${_playbackQueue.value.size}")
    }

    fun removeFromQueue(index: Int) {
        val list = _playbackQueue.value.toMutableList()
        if (index in list.indices) {
            val removed = list.removeAt(index)
            userQueuedSongIds.remove(removed.videoId)
            val currentIdx = _currentQueueIndex.value
            if (index < currentIdx) {
                _currentQueueIndex.value = (currentIdx - 1).coerceAtLeast(0)
            } else if (index == currentIdx) {
                if (list.isNotEmpty()) {
                    val nextIdx = currentIdx.coerceAtMost(list.size - 1)
                    _currentQueueIndex.value = nextIdx
                    playSong(list[nextIdx])
                } else {
                    _currentQueueIndex.value = 0
                    _currentSong.value = null
                    _isPlaying.value = false
                    pause()
                }
            }
            _playbackQueue.value = list
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] Removed from queue: '${removed.title}'")
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val list = _playbackQueue.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return

        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)

        val currentIdx = _currentQueueIndex.value
        val newCurrentIdx = when {
            currentIdx == fromIndex -> toIndex
            fromIndex < currentIdx && toIndex >= currentIdx -> currentIdx - 1
            fromIndex > currentIdx && toIndex <= currentIdx -> currentIdx + 1
            else -> currentIdx
        }
        _currentQueueIndex.value = newCurrentIdx
        _playbackQueue.value = list
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] Moved queue item from $fromIndex to $toIndex. CurrentIdx now $newCurrentIdx")
    }

    fun clearQueue() {
        val current = _currentSong.value
        _playbackQueue.value = if (current != null) listOf(current) else emptyList()
        _currentQueueIndex.value = 0
    }

    fun playNext() {
        if (isPlaybackRestricted?.invoke() == true) {
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] playNext blocked: device is a listener in room")
            return
        }
        val queue = _playbackQueue.value
        if (queue.isNotEmpty()) {
            if (_isRepeat.value) {
                seekTo(0f)
                play()
                return
            }
            val nextIdx = if (_isShuffle.value && queue.size > 1) {
                (queue.indices - _currentQueueIndex.value).random()
            } else {
                _currentQueueIndex.value + 1
            }

            if (nextIdx in queue.indices) {
                _currentQueueIndex.value = nextIdx
                val nextSong = queue[nextIdx]
                if (onTrackChangeRequested != null) {
                    onTrackChangeRequested?.invoke(nextSong, queue)
                } else {
                    playSong(nextSong, queue)
                }
            } else {
                // End of queue reached! Trigger auto-play for next suggestion!
                Log.i("ISAI_PLAYER", "[YouTubePlayerController] Queue ended! Calling onQueueExhausted for automatic next song.")
                onQueueExhausted?.invoke()
            }
        } else {
            onQueueExhausted?.invoke()
        }
    }

    fun playPrevious() {
        if (isPlaybackRestricted?.invoke() == true) {
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] playPrevious blocked: device is a listener in room")
            return
        }
        val queue = _playbackQueue.value
        if (queue.isNotEmpty()) {
            if (_isRepeat.value) {
                seekTo(0f)
                play()
                return
            }
            val prevIdx = if (_isShuffle.value && queue.size > 1) {
                (queue.indices - _currentQueueIndex.value).random()
            } else {
                if (_currentQueueIndex.value - 1 < 0) queue.size - 1 else _currentQueueIndex.value - 1
            }
            if (prevIdx in queue.indices) {
                _currentQueueIndex.value = prevIdx
                val prevSong = queue[prevIdx]
                if (onTrackChangeRequested != null) {
                    onTrackChangeRequested?.invoke(prevSong, queue)
                } else {
                    playSong(prevSong, queue)
                }
            }
        }
    }

    fun getLivePositionSec(): Float {
        return if (isUsingExoPlayer) {
            (exoPlayer.currentPosition / 1000f).coerceAtLeast(0f)
        } else {
            _currentPositionSec.value
        }
    }

    fun saveCurrentSession(wasPlaying: Boolean = isPlaying.value) {
        val song = _currentSong.value ?: return
        val pos = getLivePositionSec()
        localStorage.savePlaybackSession(
            song = song,
            queue = _playbackQueue.value,
            queueIndex = _currentQueueIndex.value,
            positionSec = pos,
            wasPlaying = wasPlaying
        )
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = coroutineScope.launch {
            var lastSavedSec = 0f
            while (isActive) {
                if (isUsingExoPlayer && exoPlayer.isPlaying) {
                    val currentSec = (exoPlayer.currentPosition / 1000f).coerceAtLeast(0f)
                    _currentPositionSec.value = currentSec
                    if (Math.abs(currentSec - lastSavedSec) >= 2f) {
                        lastSavedSec = currentSec
                        saveCurrentSession(wasPlaying = true)
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun detachPlayer() {
        saveCurrentSession(wasPlaying = false)
        activeYouTubePlayer = null
        stopProgressTracker()
        try {
            exoPlayer.pause()
        } catch (_: Exception) {}
        MusicPlaybackService.stop(context)
    }

    fun release() {
        detachPlayer()
        try {
            exoPlayer.release()
        } catch (_: Exception) {}
    }
}
