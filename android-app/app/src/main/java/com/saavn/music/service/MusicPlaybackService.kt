package com.saavn.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.saavn.music.MainActivity
import com.saavn.music.R
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.player.YouTubePlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MusicPlaybackService : Service() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentTitle: String = "ISAI Music"
    private var currentArtist: String = ""
    private var currentThumbnailUrl: String = ""
    private var isPlaying: Boolean = false
    private var currentDurationSec: Float = 0f
    private var currentPositionSec: Float = 0f
    private var cachedArtwork: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MusicPlaybackService onCreate")
        try {
            createNotificationChannel()
            setupMediaSession()
        } catch (e: Exception) {
            Log.e(TAG, "Error in MusicPlaybackService onCreate: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "onStartCommand: action=$action")

        try {
            when (action) {
                ACTION_PLAY_PAUSE -> {
                    YouTubePlayerController.getInstance()?.togglePlayPause()
                }
                ACTION_NEXT -> {
                    YouTubePlayerController.getInstance()?.playNext()
                }
                ACTION_PREV -> {
                    YouTubePlayerController.getInstance()?.playPrevious()
                }
                ACTION_STOP -> {
                    YouTubePlayerController.getInstance()?.pause()
                    stopForegroundState()
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_START_OR_UPDATE -> {
                    val newTitle = intent.getStringExtra(EXTRA_TITLE) ?: currentTitle
                    val newArtist = intent.getStringExtra(EXTRA_ARTIST) ?: currentArtist
                    val newThumbnail = intent.getStringExtra(EXTRA_THUMBNAIL) ?: currentThumbnailUrl
                    val newPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isPlaying)
                    val newDur = intent.getFloatExtra(EXTRA_DURATION_SEC, currentDurationSec)
                    val newPos = intent.getFloatExtra(EXTRA_POSITION_SEC, currentPositionSec)

                    val thumbnailChanged = newThumbnail != currentThumbnailUrl
                    currentTitle = newTitle
                    currentArtist = newArtist
                    currentThumbnailUrl = newThumbnail
                    isPlaying = newPlaying
                    currentDurationSec = newDur
                    currentPositionSec = newPos

                    if (thumbnailChanged) {
                        cachedArtwork = null
                        loadArtwork(currentThumbnailUrl)
                    }

                    updateForegroundNotification()
                    updateMediaSessionState()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing action $action: ${e.message}", e)
        }

        return START_STICKY
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "IsaiMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    YouTubePlayerController.getInstance()?.play()
                }

                override fun onPause() {
                    YouTubePlayerController.getInstance()?.pause()
                }

                override fun onSkipToNext() {
                    YouTubePlayerController.getInstance()?.playNext()
                }

                override fun onSkipToPrevious() {
                    YouTubePlayerController.getInstance()?.playPrevious()
                }

                override fun onStop() {
                    YouTubePlayerController.getInstance()?.pause()
                    stopForegroundState()
                    stopSelf()
                }

                override fun onSeekTo(pos: Long) {
                    YouTubePlayerController.getInstance()?.seekTo(pos / 1000f)
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSessionState() {
        val session = mediaSession ?: return
        try {
            val playbackActions = PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_STOP or
                    PlaybackState.ACTION_SEEK_TO

            val stateBuilder = PlaybackState.Builder()
                .setActions(playbackActions)
                .setState(
                    if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    (currentPositionSec * 1000).toLong(),
                    if (isPlaying) 1.0f else 0.0f
                )
            session.setPlaybackState(stateBuilder.build())

            val metaBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, currentArtist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, (currentDurationSec * 1000).toLong())

            cachedArtwork?.let {
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, it)
            }
            session.setMetadata(metaBuilder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update MediaSession: ${e.message}")
        }
    }

    private fun updateForegroundNotification() {
        startForegroundSafely(buildNotification())
    }

    private fun startForegroundSafely(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed startForeground with mediaPlayback type: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (inner: Exception) {
                Log.e(TAG, "Complete startForeground fallback failed: ${inner.message}")
            }
        }
    }

    private fun buildNotification(): Notification {
        val playPauseIcon = if (isPlaying) {
            R.drawable.ic_noti_pause
        } else {
            R.drawable.ic_noti_play
        }
        val playPauseText = if (isPlaying) "Pause" else "Play"

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist.ifBlank { "ISAI Tamil Music" })
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentIntent(contentIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_noti_prev),
                    "Previous",
                    prevIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, playPauseIcon),
                    playPauseText,
                    playPauseIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_noti_next),
                    "Next",
                    nextIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_noti_close),
                    "Close",
                    stopIntent
                ).build()
            )

        cachedArtwork?.let {
            builder.setLargeIcon(it)
        }

        return builder.build()
    }

    private fun loadArtwork(url: String) {
        if (url.isBlank()) return
        serviceScope.launch(Dispatchers.IO) {
            try {
                val loader = coil.Coil.imageLoader(applicationContext)
                val request = coil.request.ImageRequest.Builder(applicationContext)
                    .data(url)
                    .allowHardware(false) // Software bitmap required for Notification largeIcon
                    .build()
                val result = loader.execute(request)
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    cachedArtwork = drawable.bitmap
                    launch(Dispatchers.Main) {
                        updateForegroundNotification()
                        updateMediaSessionState()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching artwork bitmap: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous playback controls and media display for ISAI"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "onTaskRemoved: Activity cleared from recents")
        val controller = YouTubePlayerController.getInstance()
        val isStillPlaying = controller?.isPlaying?.value == true || isPlaying
        if (!isStillPlaying) {
            stopForegroundState()
            stopSelf()
        }
        // If actively playing, the foreground service stays alive and keeps playing seamlessly!
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MusicPlaybackService onDestroy")
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
    }

    companion object {
        const val TAG = "MusicPlaybackService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "isai_music_playback"
        const val CHANNEL_NAME = "ISAI Music Playback"

        const val ACTION_START_OR_UPDATE = "com.saavn.music.action.START_OR_UPDATE"
        const val ACTION_PLAY_PAUSE = "com.saavn.music.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.saavn.music.action.NEXT"
        const val ACTION_PREV = "com.saavn.music.action.PREV"
        const val ACTION_STOP = "com.saavn.music.action.STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_THUMBNAIL = "extra_thumbnail"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"
        const val EXTRA_POSITION_SEC = "extra_position_sec"

        fun startOrUpdate(
            context: Context,
            song: YouTubeSong?,
            isPlaying: Boolean,
            durationSec: Float = 0f,
            positionSec: Float = 0f
        ) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_TITLE, song?.title ?: "ISAI Music")
                putExtra(EXTRA_ARTIST, song?.channelTitle ?: "")
                putExtra(EXTRA_THUMBNAIL, song?.thumbnailUrl ?: "")
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_DURATION_SEC, durationSec)
                putExtra(EXTRA_POSITION_SEC, positionSec)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start/update MusicPlaybackService: ${e.message}", e)
                try {
                    context.startService(intent)
                } catch (inner: Exception) {
                    Log.w(TAG, "Secondary startService fallback failed: ${inner.message}")
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop MusicPlaybackService: ${e.message}", e)
            }
        }
    }
}
