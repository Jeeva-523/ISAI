package com.saavn.music.connect

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.saavn.music.data.model.YouTubeSong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val platform: String = "android",
    val lastActiveAt: Long = 0L,
    val isActive: Boolean = true
)

data class SyncSong(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artwork: String = "",
    val duration: String = ""
)

data class PlaybackStateSync(
    val currentDeviceId: String = "",
    val currentSongId: String = "",
    val currentTitle: String = "",
    val currentArtist: String = "",
    val currentArtwork: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val volume: Float = 1.0f,
    val shuffle: Boolean = false,
    val repeatMode: String = "OFF",
    val queueIndex: Int = 0,
    val updatedAt: Long = 0L,
    val updatedByDeviceId: String = ""
)

class IsaiConnectManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("isai_connect_prefs", Context.MODE_PRIVATE)
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://isai-49b51-default-rtdb.firebaseio.com")

    val deviceId: String = getOrCreateDeviceId()
    val deviceName: String = computeDeviceName()

    private var userId: String = ""
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackStateSync?>(null)
    val playbackState: StateFlow<PlaybackStateSync?> = _playbackState.asStateFlow()

    private var devicesEventListener: ValueEventListener? = null
    private var playbackEventListener: ValueEventListener? = null

    private fun getOrCreateDeviceId(): String {
        val existing = prefs.getString("device_id", null)
        if (!existing.isNullOrEmpty()) {
            return existing
        }
        val newId = "android_${Build.MODEL.replace(" ", "_").lowercase()}_${UUID.randomUUID().toString().take(6)}"
        prefs.edit().putString("device_id", newId).apply()
        return newId
    }

    private fun computeDeviceName(): String {
        val model = Build.MODEL ?: "Android Device"
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
        return "Jeeva's $manufacturer $model"
    }

    fun sanitizeUserId(rawId: String): String {
        if (rawId.isBlank()) return "user_jeeva_default"
        return rawId.replace(Regex("[.#$\\[\\]]"), "_").lowercase().trim()
    }

    fun initialize(rawUserId: String) {
        val sanitized = sanitizeUserId(rawUserId)
        if (userId == sanitized) return
        disconnect()

        userId = sanitized
        Log.d("IsaiConnect", "Initializing ISAI Connect for user: $userId, device: $deviceId ($deviceName)")

        registerDevice()
        startHeartbeat()
        listenToDevices()
        listenToPlaybackState()
    }

    private fun registerDevice() {
        if (userId.isEmpty()) return
        val deviceRef = database.getReference("connect/$userId/devices/$deviceId")
        val info = DeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            platform = "android",
            lastActiveAt = System.currentTimeMillis(),
            isActive = true
        )
        deviceRef.setValue(info)
        deviceRef.onDisconnect().updateChildren(mapOf("isActive" to false, "lastActiveAt" to System.currentTimeMillis()))
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                if (userId.isNotEmpty()) {
                    val deviceRef = database.getReference("connect/$userId/devices/$deviceId")
                    deviceRef.updateChildren(mapOf(
                        "lastActiveAt" to System.currentTimeMillis(),
                        "isActive" to true
                    ))
                }
                delay(15000)
            }
        }
    }

    private fun listenToDevices() {
        if (userId.isEmpty()) return
        val devicesRef = database.getReference("connect/$userId/devices")
        devicesEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<DeviceInfo>()
                for (child in snapshot.children) {
                    child.getValue(DeviceInfo::class.java)?.let { list.add(it) }
                }
                _devices.value = list.sortedByDescending { it.lastActiveAt }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "Devices listener cancelled: ${error.message}")
            }
        }
        devicesRef.addValueEventListener(devicesEventListener!!)
    }

    private fun listenToPlaybackState() {
        if (userId.isEmpty()) return
        val stateRef = database.getReference("connect/$userId/playbackState")
        playbackEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(PlaybackStateSync::class.java)?.let {
                    _playbackState.value = it
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "Playback listener cancelled: ${error.message}")
            }
        }
        stateRef.addValueEventListener(playbackEventListener!!)
    }

    fun updatePlaybackState(
        song: YouTubeSong? = null,
        isPlaying: Boolean? = null,
        positionMs: Long? = null,
        durationMs: Long? = null,
        currentDeviceId: String? = null
    ) {
        if (userId.isEmpty()) return
        val stateRef = database.getReference("connect/$userId/playbackState")
        val current = _playbackState.value ?: PlaybackStateSync()

        val updates = mutableMapOf<String, Any>(
            "updatedAt" to System.currentTimeMillis(),
            "updatedByDeviceId" to deviceId
        )

        if (currentDeviceId != null) {
            updates["currentDeviceId"] = currentDeviceId
        } else if (current.currentDeviceId.isEmpty()) {
            updates["currentDeviceId"] = deviceId
        }

        song?.let {
            updates["currentSongId"] = it.videoId
            updates["currentTitle"] = it.title
            updates["currentArtist"] = it.channelTitle
            updates["currentArtwork"] = it.thumbnailUrl
        }
        isPlaying?.let { updates["isPlaying"] = it }
        positionMs?.let { updates["positionMs"] = it }
        durationMs?.let { updates["durationMs"] = it }

        stateRef.updateChildren(updates)
    }

    fun transferPlaybackToDevice(targetDeviceId: String) {
        if (userId.isEmpty()) return
        Log.d("IsaiConnect", "Transferring playback to device: $targetDeviceId")
        updatePlaybackState(currentDeviceId = targetDeviceId)
    }

    fun isMyDeviceActive(): Boolean {
        val current = _playbackState.value
        return current == null || current.currentDeviceId.isEmpty() || current.currentDeviceId == deviceId
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        if (userId.isNotEmpty()) {
            database.getReference("connect/$userId/devices/$deviceId")
                .updateChildren(mapOf("isActive" to false, "lastActiveAt" to System.currentTimeMillis()))
            devicesEventListener?.let { database.getReference("connect/$userId/devices").removeEventListener(it) }
            playbackEventListener?.let { database.getReference("connect/$userId/playbackState").removeEventListener(it) }
        }
        userId = ""
    }
}
