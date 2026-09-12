package com.saavn.music.player

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.saavn.music.data.local.LocalMusicStorage
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.service.MusicPlaybackService
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
    }

    // Audio Attributes for continuous background playback and audio focus
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    // Native ExoPlayer for Direct 320kbps Audio Streams (NepoTune / JioSaavn)
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()

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

    var onQueueExhausted: (() -> Unit)? = null
    var onTrackChangeRequested: ((YouTubeSong, List<YouTubeSong>) -> Unit)? = null

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
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        notifyService(false)
                        coroutineScope.launch {
                            delay(100)
                            playNext()
                        }
                    }
                    else -> _isBuffering.value = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ISAI_PLAYER", "[YouTubePlayerController] ExoPlayer error: ${error.message}", error)
                if (isUsingExoPlayer) {
                    isUsingExoPlayer = false
                    _isBuffering.value = false
                    _isPlaying.value = false
                    notifyService(false)
                    val current = _currentSong.value
                    if (current != null) {
                        Log.i("ISAI_PLAYER", "[YouTubePlayerController] Falling back to YouTube video ID: ${current.videoId}")
                        activeYouTubePlayer?.loadVideo(current.videoId, 0f)
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
                        playNext()
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

    fun playSong(song: YouTubeSong, queue: List<YouTubeSong>? = null, startPositionSec: Float = 0f) {
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] playSong: '${song.title}' (${song.videoId}) | startPos=${startPositionSec}s | audioUrl=${song.audioUrl}")
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
        _currentPositionSec.value = startPositionSec
        _isBuffering.value = true

        localStorage.addRecentlyPlayed(song)
        notifyService(true)

        if (!song.audioUrl.isNullOrBlank()) {
            isUsingExoPlayer = true
            try {
                // Pause YouTube if it was playing
                activeYouTubePlayer?.pause()

                Log.i("ISAI_PLAYER", "[YouTubePlayerController] >>> Playing 320kbps HD Audio via ExoPlayer at ${startPositionSec}s: ${song.audioUrl} <<<")
                val mediaItem = MediaItem.fromUri(song.audioUrl)
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(mediaItem, true)
                if (startPositionSec > 0f) {
                    exoPlayer.seekTo((startPositionSec * 1000).toLong())
                } else {
                    exoPlayer.seekTo(0L)
                }
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                exoPlayer.play()
                startProgressTracker()
            } catch (e: Exception) {
                Log.e("ISAI_PLAYER", "[YouTubePlayerController] ExoPlayer failed to load: ${e.message}", e)
                isUsingExoPlayer = false
                activeYouTubePlayer?.loadVideo(song.videoId, startPositionSec)
            }
        } else {
            isUsingExoPlayer = false
            exoPlayer.stop()
            stopProgressTracker()
            if (activeYouTubePlayer != null) {
                Log.i("ISAI_PLAYER", "[YouTubePlayerController] Loading into activeYouTubePlayer at ${startPositionSec}s: ${song.videoId}")
                activeYouTubePlayer?.loadVideo(song.videoId, startPositionSec)
            } else {
                Log.w("ISAI_PLAYER", "[YouTubePlayerController] activeYouTubePlayer is NULL in playSong! Direct stream required.")
                _isBuffering.value = false
            }
        }
    }

    fun pause() {
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] pause() requested (isUsingExoPlayer=$isUsingExoPlayer)")
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
            exoPlayer.play()
        } else {
            activeYouTubePlayer?.play()
        }
        _isPlaying.value = true
        notifyService(true)
    }

    fun togglePlayPause() {
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] togglePlayPause: isUsingExoPlayer=$isUsingExoPlayer, isPlaying=${_isPlaying.value}")
        if (isUsingExoPlayer) {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
            return
        }

        val player = activeYouTubePlayer
        if (player == null) {
            Log.e("ISAI_PLAYER", "[YouTubePlayerController] ERROR: activeYouTubePlayer is NULL! Cannot play/pause.")
            return
        }
        if (_isPlaying.value) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(seconds: Float) {
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
        val clamped = volumePercent.coerceIn(0, 100)
        _volume.value = clamped
        if (isUsingExoPlayer) {
            exoPlayer.volume = clamped / 100f
        } else {
            activeYouTubePlayer?.setVolume(clamped)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    fun addToQueue(song: YouTubeSong) {
        val list = _playbackQueue.value.toMutableList()
        list.removeAll { it.videoId == song.videoId }
        val currentSongId = _currentSong.value?.videoId
        val curIdx = if (!currentSongId.isNullOrBlank()) {
            val idx = list.indexOfFirst { it.videoId == currentSongId }
            if (idx >= 0) idx else _currentQueueIndex.value
        } else {
            _currentQueueIndex.value
        }
        val insertPos = (curIdx + 1).coerceIn(0, list.size)
        list.add(insertPos, song)
        _playbackQueue.value = list
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] Added to queue after current song ($currentSongId at $curIdx -> $insertPos): '${song.title}', total=${list.size}")
    }

    fun appendQueue(songs: List<YouTubeSong>) {
        if (songs.isEmpty()) return
        val list = _playbackQueue.value.toMutableList()
        var addedCount = 0
        songs.forEach { song ->
            if (list.none { it.videoId == song.videoId }) {
                list.add(song)
                addedCount++
            }
        }
        if (addedCount > 0) {
            _playbackQueue.value = list
            Log.i("ISAI_PLAYER", "[YouTubePlayerController] Appended $addedCount new songs to queue, total=${list.size}")
        }
    }

    fun replaceUpcomingQueue(newUpcoming: List<YouTubeSong>) {
        if (newUpcoming.isEmpty()) return
        val current = _playbackQueue.value
        val curIdx = _currentQueueIndex.value
        val playedSoFar = current.take(curIdx + 1)
        val existingUpcoming = current.drop(curIdx + 1)
        val existingUpcomingIds = existingUpcoming.map { it.videoId }.toSet()
        val filteredNew = newUpcoming.filterNot { it.videoId in existingUpcomingIds }
        val combined = (playedSoFar + existingUpcoming + filteredNew).distinctBy { it.videoId }
        _playbackQueue.value = combined
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] Updated upcoming queue: preserved=${existingUpcoming.size}, added=${filteredNew.size}, total=${combined.size}")
    }

    fun setPlaybackQueue(songs: List<YouTubeSong>, newIndex: Int = _currentQueueIndex.value) {
        _playbackQueue.value = songs
        _currentQueueIndex.value = newIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
    }

    fun playNextInQueue(song: YouTubeSong) {
        val list = _playbackQueue.value.toMutableList()
        list.removeAll { it.videoId == song.videoId }
        val currentSongId = _currentSong.value?.videoId
        val curIdx = if (!currentSongId.isNullOrBlank()) {
            val idx = list.indexOfFirst { it.videoId == currentSongId }
            if (idx >= 0) idx else _currentQueueIndex.value
        } else {
            _currentQueueIndex.value
        }
        val insertPos = (curIdx + 1).coerceIn(0, list.size)
        list.add(insertPos, song)
        _playbackQueue.value = list
        Log.i("ISAI_PLAYER", "[YouTubePlayerController] Play Next inserted at $insertPos: '${song.title}', total=${list.size}")
    }

    fun removeFromQueue(index: Int) {
        val list = _playbackQueue.value.toMutableList()
        if (index in list.indices) {
            val removed = list.removeAt(index)
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

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = coroutineScope.launch {
            while (isActive) {
                if (isUsingExoPlayer && exoPlayer.isPlaying) {
                    _currentPositionSec.value = (exoPlayer.currentPosition / 1000f).coerceAtLeast(0f)
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    fun detachPlayer() {
        activeYouTubePlayer = null
        stopProgressTracker()
        try {
            exoPlayer.stop()
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
