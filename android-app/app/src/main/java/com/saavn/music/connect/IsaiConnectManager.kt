package com.saavn.music.connect

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.saavn.music.data.model.YouTubeSong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import com.google.firebase.database.PropertyName
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DeviceInfo(
    val deviceId: String = "",
    val deviceName: String = "",
    val platform: String = "android",
    val lastActiveAt: Long = 0L,
    @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true
)

@IgnoreExtraProperties
data class SyncSong(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artwork: String = "",
    val duration: String = "",
    val audioUrl: String = ""
)

@IgnoreExtraProperties
data class PlaybackStateSync(
    val currentDeviceId: String = "",
    val currentSongId: String = "",
    val currentTitle: String = "",
    val currentArtist: String = "",
    val currentArtwork: String = "",
    val currentAudioUrl: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    @get:PropertyName("isPlaying") @set:PropertyName("isPlaying") var isPlaying: Boolean = false,
    val volume: Float = 1.0f,
    val shuffle: Boolean = false,
    val repeatMode: String = "OFF",
    val queueIndex: Int = 0,
    val queue: List<SyncSong> = emptyList(),
    val updatedAt: Long = 0L,
    val updatedByDeviceId: String = ""
)

data class RemoteCommand(
    val action: String = "",
    val targetDeviceId: String = "",
    val positionMs: Long = 0L,
    val songId: String = "",
    val songTitle: String = "",
    val songArtist: String = "",
    val songArtwork: String = "",
    val songAudioUrl: String = "",
    val timestamp: Long = 0L,
    val issuedByDeviceId: String = "",
    val volume: Float = 1.0f
)

class IsaiConnectManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("isai_connect_prefs", Context.MODE_PRIVATE)
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://isai-49b51-default-rtdb.firebaseio.com")

    val deviceId: String = getOrCreateDeviceId()

    fun getActualSystemDeviceName(): String {
        // 1. Android Settings Global DEVICE_NAME (user-configured device name e.g. "OnePlus 11R", "Jeeva S23", "Galaxy A54")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val name = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                if (!name.isNullOrBlank() && !name.equals("Android", ignoreCase = true)) {
                    return name.trim()
                }
            }
        } catch (_: Exception) {}

        // 2. Settings.System "device_name" (used on Xiaomi, Oppo, Vivo, Realme, OnePlus, Samsung)
        try {
            val name = Settings.System.getString(context.contentResolver, "device_name")
            if (!name.isNullOrBlank() && !name.equals("Android", ignoreCase = true)) {
                return name.trim()
            }
        } catch (_: Exception) {}

        // 3. Bluetooth name in Settings.Secure / System (often customized by user in phone settings)
        try {
            val btName = Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                ?: Settings.System.getString(context.contentResolver, "bluetooth_name")
            if (!btName.isNullOrBlank() && !btName.equals("Android", ignoreCase = true)) {
                return btName.trim()
            }
        } catch (_: Exception) {}

        // 4. Default to Manufacturer + Clean Model (e.g., "OnePlus 11R", "Samsung Galaxy S23")
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: ""
        val model = Build.MODEL ?: "Android Phone"
        return if (model.contains(manufacturer, ignoreCase = true)) {
            model.trim()
        } else {
            "$manufacturer $model".trim()
        }
    }

    private val _currentDeviceName = MutableStateFlow(getActualSystemDeviceName())
    val currentDeviceName: StateFlow<String> = _currentDeviceName.asStateFlow()

    var deviceName: String
        get() = _currentDeviceName.value
        private set(value) {
            _currentDeviceName.value = value
        }

    private var userId: String = ""
    var userEmail: String = ""
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    private val _devices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val devices: StateFlow<List<DeviceInfo>> = _devices.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackStateSync?>(null)
    val playbackState: StateFlow<PlaybackStateSync?> = _playbackState.asStateFlow()

    private val _remoteCommand = MutableSharedFlow<RemoteCommand>(extraBufferCapacity = 64)
    val remoteCommand: SharedFlow<RemoteCommand> = _remoteCommand.asSharedFlow()
    private var lastHandledCommandTimestamp = 0L

    private val _syncedRecentlyPlayed = MutableStateFlow<List<com.saavn.music.data.model.YouTubeSong>>(emptyList())
    val syncedRecentlyPlayed: StateFlow<List<com.saavn.music.data.model.YouTubeSong>> = _syncedRecentlyPlayed.asStateFlow()

    private var homeSongsListener: ValueEventListener? = null
    private val _syncedHomeSongs = MutableStateFlow<List<com.saavn.music.data.model.YouTubeSong>>(emptyList())
    val syncedHomeSongs: StateFlow<List<com.saavn.music.data.model.YouTubeSong>> = _syncedHomeSongs.asStateFlow()

    private var favoritesListener: ValueEventListener? = null
    private val _syncedFavorites = MutableStateFlow<List<com.saavn.music.data.model.YouTubeSong>>(emptyList())
    val syncedFavorites: StateFlow<List<com.saavn.music.data.model.YouTubeSong>> = _syncedFavorites.asStateFlow()

    private var devicesEventListener: ValueEventListener? = null
    private var playbackEventListener: ValueEventListener? = null
    private var commandEventListener: ValueEventListener? = null
    private var recentlyPlayedListener: ValueEventListener? = null

    private fun getOrCreateDeviceId(): String {
        val existing = prefs.getString("device_id", null)
        if (!existing.isNullOrEmpty()) {
            return existing
        }
        val newId = "android_${Build.MODEL.replace(" ", "_").lowercase()}_${UUID.randomUUID().toString().take(6)}"
        prefs.edit().putString("device_id", newId).apply()
        return newId
    }

    fun computeDeviceName(ownerName: String? = null): String {
        return getActualSystemDeviceName()
    }

    fun sanitizeUserId(rawId: String): String {
        if (rawId.isBlank()) return ""
        return rawId.lowercase().trim().replace(Regex("[.#$\\[\\]]"), "_")
    }

    fun updateDeviceOwner(userName: String?) {
        val newName = computeDeviceName(userName)
        if (deviceName != newName) {
            deviceName = newName
            if (userId.isNotEmpty()) {
                registerDevice()
            }
        }
    }

    fun initialize(rawUserId: String, rawUserName: String? = null) {
        val sanitized = sanitizeUserId(rawUserId)
        val resolvedName = rawUserName?.trim()?.takeIf { it.isNotBlank() }
            ?: rawUserId.substringBefore("@").replaceFirstChar { it.uppercase() }.takeIf { it.isNotBlank() && !it.equals("guest", ignoreCase = true) }

        val newDeviceName = computeDeviceName(resolvedName)
        val nameChanged = (deviceName != newDeviceName)
        deviceName = newDeviceName

        if (userId == sanitized && !nameChanged) return
        disconnect()

        userEmail = rawUserId
        userId = sanitized
        Log.d("IsaiConnect", "Initializing ISAI Connect for user: $userId, device: $deviceId ($deviceName)")

        registerDevice()
        startHeartbeat()
        listenToDevices()
        listenToPlaybackState()
        listenToCommands()
        listenToRecentlyPlayed()
        listenToPreferences()
        listenToHomeSongs()
        listenToFavorites()
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

    fun isDeviceOnline(device: DeviceInfo): Boolean {
        return isDeviceAvailable(device)
    }

    fun isDeviceAvailable(device: DeviceInfo): Boolean {
        if (!device.isActive) return false
        val diffMs = System.currentTimeMillis() - device.lastActiveAt
        return diffMs <= 40_000L
    }

    private var rawDevicesList = listOf<DeviceInfo>()
    private var livenessJob: Job? = null

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
                delay(10000)
            }
        }
    }

    fun deduplicateDevices(list: List<DeviceInfo>, activeDeviceId: String = ""): List<DeviceInfo> {
        val map = linkedMapOf<String, DeviceInfo>()
        for (device in list) {
            val key = device.deviceName.trim()
            val existing = map[key]
            if (existing == null) {
                map[key] = device
            } else {
                if (device.deviceId == activeDeviceId) {
                    map[key] = device
                } else if (existing.deviceId != activeDeviceId && device.lastActiveAt > existing.lastActiveAt) {
                    map[key] = device
                }
            }
        }
        return map.values.toList()
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
                rawDevicesList = list
                val activePlayerId = _playbackState.value?.currentDeviceId ?: ""
                _devices.value = deduplicateDevices(
                    list.filter { isDeviceAvailable(it) || it.deviceId == deviceId }
                        .sortedByDescending { it.lastActiveAt },
                    activePlayerId
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "Devices listener cancelled: ${error.message}")
            }
        }
        devicesRef.addValueEventListener(devicesEventListener!!)

        // Periodic liveness check: drops offline devices automatically
        livenessJob?.cancel()
        livenessJob = scope.launch {
            while (isActive) {
                delay(5000)
                if (rawDevicesList.isNotEmpty()) {
                    val activePlayerId = _playbackState.value?.currentDeviceId ?: ""
                    val active = deduplicateDevices(
                        rawDevicesList.filter { isDeviceAvailable(it) || it.deviceId == deviceId }
                            .sortedByDescending { it.lastActiveAt },
                        activePlayerId
                    )
                    if (active.size != _devices.value.size) {
                        _devices.value = active
                    }
                }
            }
        }
    }

    private fun listenToPlaybackState() {
        if (userId.isEmpty()) return
        val stateRef = database.getReference("connect/$userId/playbackState")
        playbackEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val rawPlaying = snapshot.child("isPlaying").getValue(Boolean::class.java)
                    snapshot.getValue(PlaybackStateSync::class.java)?.let { state ->
                        if (rawPlaying != null) {
                            state.isPlaying = rawPlaying
                        }
                        _playbackState.value = state
                    }
                } catch (e: Exception) {
                    Log.w("IsaiConnect", "Playback listener parse error: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "Playback listener cancelled: ${error.message}")
            }
        }
        stateRef.addValueEventListener(playbackEventListener!!)
    }

    private fun listenToCommands() {
        if (userId.isEmpty()) return
        val cmdRef = database.getReference("connect/$userId/command")
        commandEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val action = snapshot.child("action").getValue(String::class.java) ?: ""
                        val targetDeviceId = snapshot.child("targetDeviceId").getValue(String::class.java) ?: ""
                        val positionMs = snapshot.child("positionMs").getValue(Long::class.java) ?: 0L
                        val songId = snapshot.child("songId").getValue(String::class.java) ?: ""
                        val songTitle = snapshot.child("songTitle").getValue(String::class.java) ?: ""
                        val songArtist = snapshot.child("songArtist").getValue(String::class.java) ?: ""
                        val songArtwork = snapshot.child("songArtwork").getValue(String::class.java) ?: ""
                        val songAudioUrl = snapshot.child("songAudioUrl").getValue(String::class.java) ?: ""
                        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val issuedByDeviceId = snapshot.child("issuedByDeviceId").getValue(String::class.java) ?: ""

                        if (issuedByDeviceId.isNotEmpty() && issuedByDeviceId != deviceId) {
                            val timeDiff = Math.abs(System.currentTimeMillis() - timestamp)
                            if (timeDiff < 600_000L && timestamp != lastHandledCommandTimestamp) {
                                lastHandledCommandTimestamp = timestamp
                                android.util.Log.i("IsaiConnect", "Received command: $action for target: $targetDeviceId from: $issuedByDeviceId")
                                _remoteCommand.tryEmit(
                                    RemoteCommand(
                                        action = action,
                                        targetDeviceId = targetDeviceId,
                                        positionMs = positionMs,
                                        songId = songId,
                                        songTitle = songTitle,
                                        songArtist = songArtist,
                                        songArtwork = songArtwork,
                                        songAudioUrl = songAudioUrl,
                                        timestamp = timestamp,
                                        issuedByDeviceId = issuedByDeviceId
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("IsaiConnect", "Failed to parse command: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "Command listener cancelled: ${error.message}")
            }
        }
        cmdRef.addValueEventListener(commandEventListener!!)
    }

    fun sendCommand(
        action: String,
        positionMs: Long = 0L,
        song: YouTubeSong? = null,
        targetDeviceId: String = "",
        volume: Float? = null
    ) {
        if (userId.isEmpty()) return
        val cmdRef = database.getReference("connect/$userId/command")
        val map = mutableMapOf<String, Any>(
            "action" to action,
            "positionMs" to positionMs,
            "timestamp" to System.currentTimeMillis(),
            "issuedByDeviceId" to deviceId,
            "targetDeviceId" to targetDeviceId
        )
        volume?.let { map["volume"] = it }
        song?.let {
            map["songId"] = it.videoId
            map["songTitle"] = it.title
            map["songArtist"] = it.channelTitle
            map["songArtwork"] = it.thumbnailUrl
            it.audioUrl?.let { url -> if (url.isNotBlank()) map["songAudioUrl"] = url }
            map["song"] = mapOf(
                "videoId" to it.videoId,
                "title" to it.title,
                "channelTitle" to it.channelTitle,
                "thumbnailUrl" to it.thumbnailUrl,
                "audioUrl" to (it.audioUrl ?: "")
            )
        }
        cmdRef.setValue(map)
    }

    fun updatePlaybackState(
        song: YouTubeSong? = null,
        isPlaying: Boolean? = null,
        positionMs: Long? = null,
        durationMs: Long? = null,
        currentDeviceId: String? = null,
        volume: Float? = null,
        queue: List<YouTubeSong>? = null,
        queueIndex: Int? = null
    ) {
        if (userId.isEmpty()) return
        val stateRef = database.getReference("connect/$userId/playbackState")
        val current = _playbackState.value ?: PlaybackStateSync()

        val updates = mutableMapOf<String, Any>(
            "updatedAt" to System.currentTimeMillis(),
            "updatedByDeviceId" to deviceId
        )

        val targetDevice = currentDeviceId ?: current.currentDeviceId.ifBlank { deviceId }
        updates["currentDeviceId"] = targetDevice

        song?.let {
            updates["currentSongId"] = it.videoId
            updates["currentTitle"] = it.title
            updates["currentArtist"] = it.channelTitle
            updates["currentArtwork"] = it.thumbnailUrl
            it.audioUrl?.let { url -> if (url.isNotBlank()) updates["currentAudioUrl"] = url }
        }
        isPlaying?.let { updates["isPlaying"] = it }
        positionMs?.let { updates["positionMs"] = it }
        durationMs?.let { updates["durationMs"] = it }
        volume?.let { updates["volume"] = it }
        queueIndex?.let { updates["queueIndex"] = it }
        queue?.let { qList ->
            updates["queue"] = qList.map {
                mapOf(
                    "id" to it.videoId,
                    "title" to it.title,
                    "artist" to it.channelTitle,
                    "artwork" to it.thumbnailUrl,
                    "audioUrl" to (it.audioUrl ?: "")
                )
            }
        }

        // Optimistically update local playbackState so isMyDeviceActive() is immediately accurate
        val updatedLocal = current.copy(
            updatedAt = updates["updatedAt"] as Long,
            updatedByDeviceId = deviceId,
            currentDeviceId = targetDevice,
            currentSongId = song?.videoId ?: current.currentSongId,
            currentTitle = song?.title ?: current.currentTitle,
            currentArtist = song?.channelTitle ?: current.currentArtist,
            currentArtwork = song?.thumbnailUrl ?: current.currentArtwork,
            currentAudioUrl = song?.audioUrl ?: current.currentAudioUrl,
            isPlaying = isPlaying ?: current.isPlaying,
            positionMs = positionMs ?: current.positionMs,
            durationMs = durationMs ?: current.durationMs,
            volume = volume ?: current.volume,
            queueIndex = queueIndex ?: current.queueIndex,
            queue = queue?.map {
                SyncSong(
                    id = it.videoId,
                    title = it.title,
                    artist = it.channelTitle,
                    artwork = it.thumbnailUrl,
                    audioUrl = it.audioUrl ?: ""
                )
            } ?: current.queue
        )
        _playbackState.value = updatedLocal

        stateRef.updateChildren(updates)
    }

    fun transferPlaybackToDevice(
        targetDeviceId: String,
        song: YouTubeSong? = null,
        positionMs: Long? = null,
        isPlaying: Boolean = true
    ) {
        if (userId.isEmpty()) return
        Log.d("IsaiConnect", "Transferring playback to device: $targetDeviceId")
        updatePlaybackState(
            song = song,
            isPlaying = isPlaying,
            positionMs = positionMs,
            currentDeviceId = targetDeviceId
        )
    }

    fun syncRecentlyPlayed(songs: List<com.saavn.music.data.model.YouTubeSong>) {
        if (userId.isEmpty() || songs.isEmpty()) return
        val historyRef = database.getReference("connect/$userId/recentlyPlayed")
        val clean = songs.take(20).map {
            mapOf(
                "id" to it.videoId,
                "title" to it.title,
                "artist" to it.channelTitle,
                "artwork" to it.thumbnailUrl,
                "audioUrl" to (it.audioUrl ?: "")
            )
        }
        historyRef.setValue(clean)
    }

    private fun listenToRecentlyPlayed() {
        if (userId.isEmpty()) return
        val historyRef = database.getReference("connect/$userId/recentlyPlayed")
        recentlyPlayedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<com.saavn.music.data.model.YouTubeSong>()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: ""
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val artist = child.child("artist").getValue(String::class.java) ?: ""
                    val artwork = child.child("artwork").getValue(String::class.java) ?: ""
                    val audioUrl = child.child("audioUrl").getValue(String::class.java)
                    if (id.isNotBlank() && title.isNotBlank()) {
                        list.add(
                            com.saavn.music.data.model.YouTubeSong(
                                videoId = id,
                                title = title,
                                channelTitle = artist,
                                thumbnailUrl = artwork,
                                audioUrl = audioUrl
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    _syncedRecentlyPlayed.value = list
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "recentlyPlayed listener cancelled: ${error.message}")
            }
        }
        historyRef.addValueEventListener(recentlyPlayedListener!!)
    }

    private var preferencesListener: ValueEventListener? = null
    private val _syncedPreferences = MutableStateFlow<List<String>>(emptyList())
    val syncedPreferences: StateFlow<List<String>> = _syncedPreferences.asStateFlow()

    private val _syncedMultiDeviceSeparate = MutableStateFlow<Boolean?>(null)
    val syncedMultiDeviceSeparate: StateFlow<Boolean?> = _syncedMultiDeviceSeparate.asStateFlow()

    fun syncPreferences(languages: List<String>) {
        if (userId.isEmpty() || languages.isEmpty()) return
        database.getReference("connect/$userId/preferences")
            .updateChildren(mapOf("preferredLanguages" to languages))
    }

    fun syncMultiDeviceSeparate(enabled: Boolean) {
        if (userId.isEmpty()) return
        database.getReference("connect/$userId/preferences")
            .updateChildren(mapOf("isMultiDevicePlaybackSeparate" to enabled))
    }

    private fun listenToPreferences() {
        if (userId.isEmpty()) return
        val prefRef = database.getReference("connect/$userId/preferences")
        preferencesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                // 1. Preferred Languages
                val langsSnap = snapshot.child("preferredLanguages")
                val list = mutableListOf<String>()
                if (langsSnap.childrenCount > 0) {
                    for (child in langsSnap.children) {
                        val lang = child.getValue(String::class.java)
                        if (!lang.isNullOrBlank()) list.add(lang)
                    }
                } else {
                    val single = langsSnap.getValue(String::class.java)
                    if (!single.isNullOrBlank()) list.add(single)
                }
                if (list.isNotEmpty()) {
                    _syncedPreferences.value = list
                }

                // 2. Multi-Device Playback Mode: Separate vs Sync
                val isSeparate = snapshot.child("isMultiDevicePlaybackSeparate").getValue(Boolean::class.java)
                if (isSeparate != null) {
                    _syncedMultiDeviceSeparate.value = isSeparate
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "preferences listener cancelled: ${error.message}")
            }
        }
        prefRef.addValueEventListener(preferencesListener!!)
    }

    fun syncHomeSongs(songs: List<com.saavn.music.data.model.YouTubeSong>) {
        if (userId.isEmpty() || songs.isEmpty()) return
        val clean = songs.take(60).map { s ->
            mapOf(
                "id" to s.videoId,
                "title" to s.title,
                "artist" to s.channelTitle,
                "artwork" to s.thumbnailUrl,
                "audioUrl" to (s.audioUrl ?: ""),
                "durationFormatted" to s.durationFormatted,
                "durationMs" to s.durationMs,
                "playCount" to s.playCount
            )
        }
        database.getReference("connect/$userId/homeSongs").setValue(clean)
            .addOnFailureListener { e -> Log.w("IsaiConnect", "syncHomeSongs error: ${e.message}") }
    }

    private fun listenToHomeSongs() {
        if (userId.isEmpty()) return
        val homeRef = database.getReference("connect/$userId/homeSongs")
        homeSongsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<com.saavn.music.data.model.YouTubeSong>()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: ""
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val artist = child.child("artist").getValue(String::class.java) ?: ""
                    val artwork = child.child("artwork").getValue(String::class.java) ?: ""
                    val audioUrl = child.child("audioUrl").getValue(String::class.java)
                    val durFormatted = child.child("durationFormatted").getValue(String::class.java) ?: "3:30"
                    val durMs = child.child("durationMs").getValue(Long::class.java) ?: 210000L
                    val playCount = child.child("playCount").getValue(Long::class.java) ?: 0L

                    if (id.isNotBlank() && title.isNotBlank()) {
                        list.add(
                            com.saavn.music.data.model.YouTubeSong(
                                videoId = id,
                                title = title,
                                channelTitle = artist,
                                thumbnailUrl = artwork,
                                audioUrl = audioUrl,
                                durationFormatted = durFormatted,
                                durationMs = durMs,
                                playCount = playCount
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    _syncedHomeSongs.value = list
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "homeSongs listener cancelled: ${error.message}")
            }
        }
        homeRef.addValueEventListener(homeSongsListener!!)
    }

    fun syncFavorites(songs: List<com.saavn.music.data.model.YouTubeSong>) {
        if (userId.isEmpty()) return
        val clean = songs.take(100).map { s ->
            mapOf(
                "id" to s.videoId,
                "title" to s.title,
                "artist" to s.channelTitle,
                "artwork" to s.thumbnailUrl,
                "audioUrl" to (s.audioUrl ?: ""),
                "durationFormatted" to s.durationFormatted,
                "durationMs" to s.durationMs
            )
        }
        database.getReference("connect/$userId/favorites").setValue(clean)
            .addOnFailureListener { e -> Log.w("IsaiConnect", "syncFavorites error: ${e.message}") }
    }

    private fun listenToFavorites() {
        if (userId.isEmpty()) return
        val favRef = database.getReference("connect/$userId/favorites")
        favoritesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<com.saavn.music.data.model.YouTubeSong>()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: ""
                    val title = child.child("title").getValue(String::class.java) ?: ""
                    val artist = child.child("artist").getValue(String::class.java) ?: ""
                    val artwork = child.child("artwork").getValue(String::class.java) ?: ""
                    val audioUrl = child.child("audioUrl").getValue(String::class.java)
                    val durFormatted = child.child("durationFormatted").getValue(String::class.java) ?: "3:30"
                    val durMs = child.child("durationMs").getValue(Long::class.java) ?: 210000L

                    if (id.isNotBlank() && title.isNotBlank()) {
                        list.add(
                            com.saavn.music.data.model.YouTubeSong(
                                videoId = id,
                                title = title,
                                channelTitle = artist,
                                thumbnailUrl = artwork,
                                audioUrl = audioUrl,
                                durationFormatted = durFormatted,
                                durationMs = durMs
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    _syncedFavorites.value = list
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("IsaiConnect", "favorites listener cancelled: ${error.message}")
            }
        }
        favRef.addValueEventListener(favoritesListener!!)
    }

    fun isMyDeviceActive(): Boolean {
        val current = _playbackState.value
        if (current == null || current.currentDeviceId.isBlank()) {
            return true
        }
        return current.currentDeviceId == deviceId
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        livenessJob?.cancel()
        livenessJob = null
        if (userId.isNotEmpty()) {
            database.getReference("connect/$userId/devices/$deviceId")
                .updateChildren(mapOf("isActive" to false, "lastActiveAt" to System.currentTimeMillis()))
            devicesEventListener?.let { database.getReference("connect/$userId/devices").removeEventListener(it) }
            playbackEventListener?.let { database.getReference("connect/$userId/playbackState").removeEventListener(it) }
            commandEventListener?.let { database.getReference("connect/$userId/command").removeEventListener(it) }
            recentlyPlayedListener?.let { database.getReference("connect/$userId/recentlyPlayed").removeEventListener(it) }
            preferencesListener?.let { database.getReference("connect/$userId/preferences/preferredLanguages").removeEventListener(it) }
            homeSongsListener?.let { database.getReference("connect/$userId/homeSongs").removeEventListener(it) }
            favoritesListener?.let { database.getReference("connect/$userId/favorites").removeEventListener(it) }
        }
        userId = ""
    }
}
