package com.saavn.music.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.saavn.music.BuildConfig
import com.saavn.music.R
import com.saavn.music.data.model.AppUpdateModel
import kotlinx.coroutines.tasks.await

class AppUpdateService private constructor(private val context: Context) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val rtdb: FirebaseDatabase = FirebaseDatabase.getInstance()

    companion object {
        private const val TAG = "AppUpdateService"
        private const val UPDATE_CHANNEL_ID = "isai_app_updates"
        private const val NOTIFICATION_ID = 2001

        @Volatile
        private var instance: AppUpdateService? = null

        fun getInstance(context: Context): AppUpdateService {
            return instance ?: synchronized(this) {
                instance ?: AppUpdateService(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Checks if a new version is available on Firebase.
     * Returns AppUpdateModel if an update is required or available, null otherwise.
     */
    suspend fun checkForUpdate(): AppUpdateModel? {
        return try {
            val updateInfo = fetchUpdateConfig() ?: return null
            val currentVersionCode = BuildConfig.VERSION_CODE

            Log.d(TAG, "Current VersionCode: $currentVersionCode, Latest: ${updateInfo.latestVersionCode}")

            if (updateInfo.latestVersionCode > currentVersionCode) {
                // If current version is less than minRequiredVersionCode, force update
                val isStrictlyForced = updateInfo.isForceUpdate || (currentVersionCode < updateInfo.minRequiredVersionCode)
                val finalInfo = updateInfo.copy(isForceUpdate = isStrictlyForced)

                // Fire Android system notification for the update
                showUpdateNotification(finalInfo)

                finalInfo
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking for updates: ${e.message}")
            null
        }
    }

    /**
     * Tries fetching update config from Firestore first, then falls back to Realtime Database.
     */
    private suspend fun fetchUpdateConfig(): AppUpdateModel? {
        // 1. Try Firestore: app_config / version
        try {
            val snapshot = firestore.collection("app_config").document("version").get().await()
            if (snapshot.exists()) {
                val latestCode = (snapshot.get("latestVersionCode") as? Number)?.toInt() ?: 1
                val latestName = snapshot.getString("latestVersionName") ?: "1.0.0"
                val minCode = (snapshot.get("minRequiredVersionCode") as? Number)?.toInt() ?: 1
                val title = snapshot.getString("updateTitle") ?: "New Update Available! 🚀"
                val message = snapshot.getString("updateMessage") ?: "A new version of ISAI is available with improvements."
                val notes = (snapshot.get("releaseNotes") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val downloadUrl = snapshot.getString("downloadUrl") ?: ""
                val forceUpdate = snapshot.getBoolean("isForceUpdate") ?: false

                return AppUpdateModel(
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    minRequiredVersionCode = minCode,
                    updateTitle = title,
                    updateMessage = message,
                    releaseNotes = notes,
                    downloadUrl = downloadUrl,
                    isForceUpdate = forceUpdate
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore update config fetch failed: ${e.message}")
        }

        // 2. Fallback to Realtime Database: app_config / version
        try {
            val rtdbSnapshot = rtdb.getReference("app_config/version").get().await()
            if (rtdbSnapshot.exists()) {
                val latestCode = (rtdbSnapshot.child("latestVersionCode").value as? Number)?.toInt() ?: 1
                val latestName = rtdbSnapshot.child("latestVersionName").value?.toString() ?: "1.0.0"
                val minCode = (rtdbSnapshot.child("minRequiredVersionCode").value as? Number)?.toInt() ?: 1
                val title = rtdbSnapshot.child("updateTitle").value?.toString() ?: "New Update Available! 🚀"
                val message = rtdbSnapshot.child("updateMessage").value?.toString() ?: "A new version of ISAI is available with improvements."
                val notes = (rtdbSnapshot.child("releaseNotes").value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val downloadUrl = rtdbSnapshot.child("downloadUrl").value?.toString() ?: ""
                val forceUpdate = rtdbSnapshot.child("isForceUpdate").value as? Boolean ?: false

                return AppUpdateModel(
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    minRequiredVersionCode = minCode,
                    updateTitle = title,
                    updateMessage = message,
                    releaseNotes = notes,
                    downloadUrl = downloadUrl,
                    isForceUpdate = forceUpdate
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime DB update config fetch failed: ${e.message}")
        }

        return null
    }

    /**
     * Opens the download link or Play Store / browser to download the updated APK.
     */
    fun openUpdateUrl(context: Context, downloadUrl: String) {
        val targetUrl = if (downloadUrl.isNotBlank()) {
            downloadUrl
        } else {
            // Default to ISAI Web App or Play Store if not specified
            "https://isaihub.web.app"
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch update URL: $targetUrl", e)
        }
    }

    /**
     * Fires a high-priority system notification in the Android notification shade.
     * Guaranteed to only fire once per new version code release.
     */
    fun showUpdateNotification(updateInfo: AppUpdateModel) {
        val prefs = context.getSharedPreferences("isai_update_prefs", Context.MODE_PRIVATE)
        val lastNotified = prefs.getInt("last_notified_version_code", 0)

        if (lastNotified >= updateInfo.latestVersionCode) {
            return
        }

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    "ISAI App Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies users when a new version of ISAI is released"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val targetUrl = updateInfo.downloadUrl.ifBlank { "https://isaihub.web.app" }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_music)
                .setContentTitle(updateInfo.updateTitle)
                .setContentText("v${updateInfo.latestVersionName} is available! Tap to download.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("${updateInfo.updateMessage}\n\nTap here to update now."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager.notify(NOTIFICATION_ID, builder.build())
            prefs.edit().putInt("last_notified_version_code", updateInfo.latestVersionCode).apply()
            Log.d(TAG, "Posted update notification for v${updateInfo.latestVersionName}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not post system update notification: ${e.message}")
        }
    }

    /**
     * Attaches a Realtime Database listener so updates posted to Firebase
     * immediately trigger the in-app notification & dialog even while the app is active.
     */
    fun listenForRealtimeUpdates(onUpdateDetected: (AppUpdateModel) -> Unit) {
        val ref = rtdb.getReference("app_config/version")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val latestCode = (snapshot.child("latestVersionCode").value as? Number)?.toInt() ?: 1
                val currentVersionCode = BuildConfig.VERSION_CODE

                if (latestCode > currentVersionCode) {
                    val latestName = snapshot.child("latestVersionName").value?.toString() ?: "1.1.0"
                    val minCode = (snapshot.child("minRequiredVersionCode").value as? Number)?.toInt() ?: 1
                    val title = snapshot.child("updateTitle").value?.toString() ?: "New Update Available! 🚀"
                    val message = snapshot.child("updateMessage").value?.toString() ?: "A new version of ISAI is ready."
                    val notes = (snapshot.child("releaseNotes").value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val downloadUrl = snapshot.child("downloadUrl").value?.toString() ?: ""
                    val forceUpdate = snapshot.child("isForceUpdate").value as? Boolean ?: false

                    val model = AppUpdateModel(
                        latestVersionCode = latestCode,
                        latestVersionName = latestName,
                        minRequiredVersionCode = minCode,
                        updateTitle = title,
                        updateMessage = message,
                        releaseNotes = notes,
                        downloadUrl = downloadUrl,
                        isForceUpdate = forceUpdate || (currentVersionCode < minCode)
                    )

                    showUpdateNotification(model)
                    onUpdateDetected(model)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Realtime update listener cancelled: ${error.message}")
            }
        })
    }
}
