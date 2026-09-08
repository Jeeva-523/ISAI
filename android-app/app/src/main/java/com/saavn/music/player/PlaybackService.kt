package com.saavn.music.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes, true)
        }
        
        mediaSession = MediaSession.Builder(this, player).build()

        val notificationProvider = androidx.media3.session.DefaultMediaNotificationProvider.Builder(this).build()
        notificationProvider.setSmallIcon(com.saavn.music.R.drawable.ic_notification_music)
        
        setMediaNotificationProvider(object : androidx.media3.session.MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
                actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
                onNotificationChangedCallback: androidx.media3.session.MediaNotification.Provider.Callback
            ): androidx.media3.session.MediaNotification {
                val defaultNotification = notificationProvider.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
                
                val isPlaying = mediaSession.player.playWhenReady
                
                // As requested in your prompt (Step 3):
                // We use MediaStyle (handled natively by Media3) and ensure setOngoing(true) is called while playing.
                // NOTE: On Android 11+ (Vivo/Xiaomi), the OS will still place this in the Media Carousel and allow swiping, 
                // overriding setOngoing(true). This is standard modern Android behavior.
                val builder = androidx.core.app.NotificationCompat.Builder(this@PlaybackService, defaultNotification.notification)
                    .setOngoing(isPlaying)
                    .setColor(android.graphics.Color.parseColor("#E0287D"))
                
                // Setting the MediaStyle exactly as requested, linking to the MediaSession token
                val mediaStyle = androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(0, 1, 2)
                builder.setStyle(mediaStyle)
                
                return androidx.media3.session.MediaNotification(defaultNotification.notificationId, builder.build())
            }
            
            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: android.os.Bundle
            ): Boolean {
                return false
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        // If playing, we intentionally do NOT call super.onTaskRemoved() 
        // to prevent the service from pausing and stopping.
    }

    override fun onDestroy() {
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
