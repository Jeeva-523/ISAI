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
import com.saavn.music.data.model.UserProfile
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import android.os.Environment
import android.provider.Settings

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progress: Float, val currentMb: String, val totalMb: String) : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

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
     * Evaluates if the current user profile qualifies to receive this update based on targetMode:
     * - "ALL" -> All users receive update
     * - "TESTERS_ONLY" or "BETA" -> Only users with isTester = true, updateChannel = "BETA", or listed in targetEmails/targetUserIds
     * - "SPECIFIC_USERS" -> Only users whose email or userId is explicitly listed in targetEmails/targetUserIds
     */
    fun isUserEligibleForUpdate(updateInfo: AppUpdateModel, user: UserProfile?): Boolean {
        val mode = updateInfo.targetMode.uppercase().trim()
        if (mode == "ALL" || mode.isBlank()) {
            return true
        }

        val userEmail = user?.email?.trim()?.lowercase() ?: ""
        val userId = user?.id?.trim() ?: ""
        val emails = updateInfo.targetEmails.map { it.trim().lowercase() }
        val ids = updateInfo.targetUserIds.map { it.trim() }

        val isDirectlyTargeted = (userEmail.isNotBlank() && emails.contains(userEmail)) ||
                (userId.isNotBlank() && ids.contains(userId))

        if (mode == "SPECIFIC_USERS") {
            return isDirectlyTargeted
        }

        if (mode == "TESTERS_ONLY" || mode == "BETA") {
            val isTesterUser = user?.isTester == true || user?.updateChannel.equals("BETA", ignoreCase = true)
            return isTesterUser || isDirectlyTargeted
        }

        return true
    }

    /**
     * Checks if a new version is available on Firebase and eligible for this user.
     * Returns AppUpdateModel if an update is required or available, null otherwise.
     */
    suspend fun checkForUpdate(user: UserProfile? = null): AppUpdateModel? {
        return try {
            val updateInfo = fetchUpdateConfig() ?: return null
            val currentVersionCode = BuildConfig.VERSION_CODE

            Log.d(TAG, "Current VersionCode: $currentVersionCode, Latest: ${updateInfo.latestVersionCode}, Mode: ${updateInfo.targetMode}")

            if (updateInfo.latestVersionCode > currentVersionCode) {
                // Check if user is eligible for targeted update
                if (!isUserEligibleForUpdate(updateInfo, user)) {
                    Log.d(TAG, "Update v${updateInfo.latestVersionName} available, but user is not in targeted group (${updateInfo.targetMode}). Skipping.")
                    return null
                }

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
                val targetMode = snapshot.getString("targetMode") ?: "ALL"
                val targetEmails = (snapshot.get("targetEmails") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val targetUserIds = (snapshot.get("targetUserIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                return AppUpdateModel(
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    minRequiredVersionCode = minCode,
                    updateTitle = title,
                    updateMessage = message,
                    releaseNotes = notes,
                    downloadUrl = downloadUrl,
                    isForceUpdate = forceUpdate,
                    targetMode = targetMode,
                    targetEmails = targetEmails,
                    targetUserIds = targetUserIds
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
                val targetMode = rtdbSnapshot.child("targetMode").value?.toString() ?: "ALL"
                val targetEmails = (rtdbSnapshot.child("targetEmails").value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val targetUserIds = (rtdbSnapshot.child("targetUserIds").value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                return AppUpdateModel(
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    minRequiredVersionCode = minCode,
                    updateTitle = title,
                    updateMessage = message,
                    releaseNotes = notes,
                    downloadUrl = downloadUrl,
                    isForceUpdate = forceUpdate,
                    targetMode = targetMode,
                    targetEmails = targetEmails,
                    targetUserIds = targetUserIds
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime DB update config fetch failed: ${e.message}")
        }

        return null
    }

    /**
     * Downloads the APK directly inside the app with live byte streaming progress,
     * then immediately launches the Android package installer.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        rawUrl: String,
        onStateChange: (UpdateDownloadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val targetUrl = when {
            rawUrl.endsWith("/update") || rawUrl.isBlank() || rawUrl == "https://isaihub.web.app" -> "https://isaihub.web.app/isai.dat"
            rawUrl.endsWith(".apk") || rawUrl.endsWith(".dat") -> rawUrl
            else -> "https://isaihub.web.app/isai.dat"
        }

        try {
            onStateChange(UpdateDownloadState.Downloading(0.02f, "0 MB", "..."))

            val request = okhttp3.Request.Builder()
                .url(targetUrl)
                .addHeader("User-Agent", "ISAI-InApp-Updater")
                .build()

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Download failed (HTTP ${response.code})"
                Log.e(TAG, err)
                onStateChange(UpdateDownloadState.Error(err))
                return@withContext
            }

            val body = response.body
            if (body == null) {
                val err = "Empty server response"
                Log.e(TAG, err)
                onStateChange(UpdateDownloadState.Error(err))
                return@withContext
            }

            val contentLength = body.contentLength()
            val totalMbStr = if (contentLength > 0) String.format("%.1f MB", contentLength / (1024.0 * 1024.0)) else ""

            val updateDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()

            val apkFile = File(updateDir, "isai_latest.apk")
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Long = 0
                    var read: Int
                    var lastReportedTime = System.currentTimeMillis()

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportedTime > 150 || bytesRead == contentLength) {
                            lastReportedTime = now
                            val progress = if (contentLength > 0) {
                                (bytesRead.toFloat() / contentLength.toFloat()).coerceIn(0.05f, 0.99f)
                            } else 0.5f

                            val currentMbStr = String.format("%.1f MB", bytesRead / (1024.0 * 1024.0))
                            onStateChange(UpdateDownloadState.Downloading(progress, currentMbStr, totalMbStr))
                        }
                    }
                }
            }

            Log.i(TAG, "APK download finished successfully: ${apkFile.length()} bytes")
            onStateChange(UpdateDownloadState.ReadyToInstall(apkFile))

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "In-app update download error", e)
            onStateChange(UpdateDownloadState.Error(e.localizedMessage ?: "Download failed. Please check internet connection."))
        }
    }

    /**
     * Prompts the Android Package Installer to install or update the app.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists() || apkFile.length() <= 0) {
                Log.e(TAG, "Cannot install: APK file does not exist or is empty")
                return
            }

            // Android 8.0+ Unknown Sources Permission Check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            openUpdateUrl(context, "https://isaihub.web.app")
        }
    }

    /**
     * Fallback: Opens the download link or Play Store / browser.
     */
    fun openUpdateUrl(context: Context, downloadUrl: String) {
        val targetUrl = if (downloadUrl.isNotBlank()) {
            downloadUrl
        } else {
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
    fun listenForRealtimeUpdates(getUser: (() -> UserProfile?)? = null, onUpdateDetected: (AppUpdateModel) -> Unit) {
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
                    val targetMode = snapshot.child("targetMode").value?.toString() ?: "ALL"
                    val targetEmails = (snapshot.child("targetEmails").value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val targetUserIds = (snapshot.child("targetUserIds").value as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                    val model = AppUpdateModel(
                        latestVersionCode = latestCode,
                        latestVersionName = latestName,
                        minRequiredVersionCode = minCode,
                        updateTitle = title,
                        updateMessage = message,
                        releaseNotes = notes,
                        downloadUrl = downloadUrl,
                        isForceUpdate = forceUpdate || (currentVersionCode < minCode),
                        targetMode = targetMode,
                        targetEmails = targetEmails,
                        targetUserIds = targetUserIds
                    )

                    val user = getUser?.invoke()
                    if (isUserEligibleForUpdate(model, user)) {
                        showUpdateNotification(model)
                        onUpdateDetected(model)
                    } else {
                        Log.d(TAG, "Realtime update detected but user not in targeted group ($targetMode). Ignoring.")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Realtime update listener cancelled: ${error.message}")
            }
        })
    }
}
