package com.saavn.music.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.saavn.music.data.model.AudioQuality
import com.saavn.music.data.model.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerController(context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _currentSong = MutableStateFlow<SongItem?>(null)
    val currentSong: StateFlow<SongItem?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _audioQuality = MutableStateFlow(AudioQuality.VERY_HIGH)
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()

    private val _playlist = MutableStateFlow<List<SongItem>>(emptyList())
    val playlist: StateFlow<List<SongItem>> = _playlist.asStateFlow()

    private var currentIndex: Int = -1

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    playNext()
                }
            }
        })
    }

    fun playSong(song: SongItem, queue: List<SongItem> = listOf(song)) {
        _playlist.value = queue
        currentIndex = queue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        loadAndPlay(song)
    }

    private fun loadAndPlay(song: SongItem) {
        _currentSong.value = song
        val streamUrl = song.getStreamUrl(_audioQuality.value)

        if (!streamUrl.isNullOrBlank()) {
            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMediaId(song.id)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun playNext() {
        val list = _playlist.value
        if (list.isEmpty()) return
        if (currentIndex < list.size - 1) {
            currentIndex++
            loadAndPlay(list[currentIndex])
        } else {
            // Loop back to start
            currentIndex = 0
            loadAndPlay(list[0])
        }
    }

    fun playPrevious() {
        val list = _playlist.value
        if (list.isEmpty()) return
        if (player.currentPosition > 3000L) {
            player.seekTo(0)
            return
        }
        if (currentIndex > 0) {
            currentIndex--
            loadAndPlay(list[currentIndex])
        } else {
            player.seekTo(0)
        }
    }

    fun setQuality(quality: AudioQuality) {
        if (_audioQuality.value == quality) return
        _audioQuality.value = quality
        val song = _currentSong.value ?: return
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying

        val newUrl = song.getStreamUrl(quality)
        if (!newUrl.isNullOrBlank()) {
            val mediaItem = MediaItem.fromUri(newUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.seekTo(currentPos)
            if (wasPlaying) player.play()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _currentPosition.value = player.currentPosition
                if (player.duration > 0) {
                    _duration.value = player.duration
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        player.release()
    }
}
