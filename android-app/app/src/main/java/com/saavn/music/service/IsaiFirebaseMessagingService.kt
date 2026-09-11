package com.saavn.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.saavn.music.MainActivity

class IsaiFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "IsaiFCM"
        const val FCM_CHANNEL_ID = "isai_fcm_channel"
        const val FCM_CHANNEL_NAME = "ISAI Announcements & Updates"

        /**
         * Automatically subscribe the device to general broadcast topics
         * so Firebase Console Campaigns targeting 'all' or 'updates' deliver to every user.
         */
        fun setupFCMSubscriptions(context: Context) {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("all")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to FCM topic: all")
                        }
                    }
                FirebaseMessaging.getInstance().subscribeToTopic("updates")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Subscribed to FCM topic: updates")
                        }
                    }

                // Create channel early so it's registered with Android OS
                createNotificationChannel(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to setup FCM subscriptions: ${e.message}")
            }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    FCM_CHANNEL_ID,
                    FCM_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new songs, app updates, and announcements"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 1. Extract Title & Body (from Notification payload or Data payload)
        var title = remoteMessage.notification?.title
        var body = remoteMessage.notification?.body

        if (title.isNullOrBlank()) {
            title = remoteMessage.data["title"] ?: "ISAI Music"
        }
        if (body.isNullOrBlank()) {
            body = remoteMessage.data["body"] ?: remoteMessage.data["message"] ?: "New notification from ISAI"
        }

        val downloadUrl = remoteMessage.data["downloadUrl"] 
            ?: remoteMessage.data["url"] 
            ?: remoteMessage.data["link"]

        showNotification(title, body, downloadUrl)
    }

    private fun showNotification(title: String, body: String, directUrl: String?) {
        try {
            createNotificationChannel(this)

            val intent = if (!directUrl.isNullOrBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse(directUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(this, FCM_CHANNEL_ID)
                .setSmallIcon(com.saavn.music.R.drawable.ic_notification_music)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show FCM notification: ${e.message}", e)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        try {
            FirebaseFirestore.getInstance().collection("users")
                .document(currentUser.uid)
                .update(
                    mapOf(
                        "fcmToken" to token,
                        "fcmUpdatedAt" to System.currentTimeMillis()
                    )
                )
                .addOnFailureListener {
                    // Document might not exist yet, set with merge
                    FirebaseFirestore.getInstance().collection("users")
                        .document(currentUser.uid)
                        .set(mapOf("fcmToken" to token, "fcmUpdatedAt" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving FCM token: ${e.message}")
        }
    }
}
