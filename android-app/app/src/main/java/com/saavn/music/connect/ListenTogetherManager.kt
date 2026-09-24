package com.saavn.music.connect

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.saavn.music.data.model.YouTubeSong
import com.saavn.music.player.YouTubePlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@IgnoreExtraProperties
data class RoomDevice(
    val deviceId: String = "",
    val deviceName: String = "",
    val deviceType: String = "ANDROID",
    @get:PropertyName("isHost") @set:PropertyName("isHost") var isHost: Boolean = false,
    @get:PropertyName("connected") @set:PropertyName("connected") var connected: Boolean = true,
    val joinedAt: Long = 0L,
    val lastSeenAt: Long = 0L
)

@IgnoreExtraProperties
data class RoomSong(
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val artwork: String = "",
    val audioUrl: String = "",
    val durationMs: Long = 0L
)

@IgnoreExtraProperties
data class RoomPlaybackState(
    val state: String = "IDLE", // PLAYING, PAUSED, IDLE
    val positionSec: Float = 0f,
    val serverTimestamp: Long = 0L,
    val version: Long = 0L,
    val song: RoomSong? = null,
    val action: String = "",
    val volume: Int = 100
)

@IgnoreExtraProperties
data class RoomData(
    val roomId: String = "",
    val roomCode: String = "",
    val hostDeviceId: String = "",
    val hostDeviceName: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val version: Long = 0L,
    val playbackState: RoomPlaybackState = RoomPlaybackState(),
    val devices: Map<String, RoomDevice> = emptyMap()
)

enum class RoomSyncStatus {
    SYNCED,
    SYNCING,
    RECONNECTING,
    DISCONNECTED
}

class ListenTogetherManager(
    private val context: Context,
    private val playerController: YouTubePlayerController
) {
    companion object {
        private const val TAG = "ListenTogetherManager"
        @Volatile
        private var instance: ListenTogetherManager? = null

        fun getInstance(context: Context, playerController: YouTubePlayerController): ListenTogetherManager {
            return instance ?: synchronized(this) {
                instance ?: ListenTogetherManager(context.applicationContext, playerController).also {
                    instance = it
                }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("isai_room_prefs", Context.MODE_PRIVATE)
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://isai-49b51-default-rtdb.firebaseio.com")
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val deviceId: String = getOrCreateDeviceId()
    val deviceName: String = getActualDeviceName()

    private var serverTimeOffset: Long = 0L
    private var currentRoomListener: ValueEventListener? = null
    private var connectedListener: ValueEventListener? = null
    private var driftJob: Job? = null
    private var heartbeatJob: Job? = null
    private var lastSeekTimestamp: Long = 0L
    private var lastHostSyncTimestamp: Long = 0L
    private var lastProcessedVersion: Long = 0L

    var onGuestPlaySongRequested: ((YouTubeSong, Float, Boolean) -> Unit)? = null

    @Volatile
    private var latestPlaybackState: RoomPlaybackState? = null

    private val _currentRoom = MutableStateFlow<RoomData?>(null)
    val currentRoom: StateFlow<RoomData?> = _currentRoom.asStateFlow()

    private val _syncStatus = MutableStateFlow(RoomSyncStatus.DISCONNECTED)
    val syncStatus: StateFlow<RoomSyncStatus> = _syncStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        initClockOffset()
    }

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString("room_device_id", null)
        if (id.isNullOrBlank()) {
            id = "android_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("room_device_id", id).apply()
        }
        return id
    }

    private fun getActualDeviceName(): String {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val name = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                if (!name.isNullOrBlank() && !name.equals("Android", ignoreCase = true)) {
                    return name.trim()
                }
            }
        } catch (_: Exception) {}

        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: ""
        val model = Build.MODEL ?: "Android Phone"
        return if (model.contains(manufacturer, ignoreCase = true)) model.trim() else "$manufacturer $model".trim()
    }

    private fun initClockOffset() {
        database.getReference(".info/serverTimeOffset").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offset = snapshot.getValue(Long::class.java)
                serverTimeOffset = offset ?: 0L
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read serverTimeOffset: ${error.message}")
            }
        })
    }

    fun getEstimatedServerTime(): Long {
        return System.currentTimeMillis() + serverTimeOffset
    }

    fun isHost(): Boolean {
        val room = _currentRoom.value ?: return false
        return room.hostDeviceId == deviceId
    }

    // ==========================================
    // ROOM OPERATIONS
    // ==========================================

    private fun generateRoomCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val sb = StringBuilder("ISAI-")
        for (i in 0 until 4) {
            sb.append(chars.random())
        }
        return sb.toString()
    }

    fun createRoom(initialSong: YouTubeSong? = null, onComplete: (Result<RoomData>) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                if (_currentRoom.value != null) {
                    leaveRoomInternal()
                }

                val roomCode = generateRoomCode()
                val roomId = roomCode
                val now = getEstimatedServerTime()

                val songPayload = initialSong?.let {
                    RoomSong(
                        videoId = it.videoId,
                        title = it.title,
                        artist = it.channelTitle,
                        artwork = it.thumbnailUrl,
                        audioUrl = it.audioUrl ?: "",
                        durationMs = it.durationMs
                    )
                }

                val initialPlayback = RoomPlaybackState(
                    state = if (initialSong != null) "PLAYING" else "IDLE",
                    positionSec = playerController.currentPositionSec.value,
                    serverTimestamp = now,
                    version = 1L,
                    song = songPayload,
                    action = if (initialSong != null) "PLAY" else "INIT",
                    volume = playerController.volume.value
                )

                val myDevice = RoomDevice(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    deviceType = "ANDROID",
                    isHost = true,
                    connected = true,
                    joinedAt = now,
                    lastSeenAt = now
                )

                val roomData = RoomData(
                    roomId = roomId,
                    roomCode = roomCode,
                    hostDeviceId = deviceId,
                    hostDeviceName = deviceName,
                    createdAt = now,
                    updatedAt = now,
                    version = 1L,
                    playbackState = initialPlayback,
                    devices = mapOf(deviceId to myDevice)
                )

                database.getReference("rooms/$roomId").setValue(roomData).await()

                launch(Dispatchers.Main) {
                    attachToRoom(roomId)
                    _errorMessage.value = null
                    onComplete(Result.success(roomData))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating room", e)
                launch(Dispatchers.Main) {
                    _errorMessage.value = e.message ?: "Unable to create room."
                    onComplete(Result.failure(e))
                }
            }
        }
    }

    fun joinRoom(codeOrId: String, onComplete: (Result<RoomData>) -> Unit) {
        val trimmed = codeOrId.trim().uppercase()
        val roomId = if (trimmed.startsWith("ISAI-")) trimmed else "ISAI-$trimmed"

        scope.launch(Dispatchers.IO) {
            try {
                if (_currentRoom.value?.roomId == roomId) {
                    launch(Dispatchers.Main) {
                        onComplete(Result.success(_currentRoom.value!!))
                    }
                    return@launch
                }

                if (_currentRoom.value != null) {
                    leaveRoomInternal()
                }

                val roomRef = database.getReference("rooms/$roomId")
                val snap = roomRef.get().await()

                if (!snap.exists()) {
                    throw IllegalStateException("Room not found. Please check room code.")
                }

                val data = snap.getValue(RoomData::class.java)
                    ?: throw IllegalStateException("Invalid room format.")

                val devices = data.devices
                val isAlreadyMember = devices.containsKey(deviceId)
                val activeConnectedCount = devices.values.count { it.connected }

                // Maximum 3 devices constraint
                if (!isAlreadyMember && activeConnectedCount >= 3) {
                    throw IllegalStateException("Maximum 3 devices are allowed in this room.")
                }

                val now = getEstimatedServerTime()
                val myDevice = RoomDevice(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    deviceType = "ANDROID",
                    isHost = data.hostDeviceId == deviceId,
                    connected = true,
                    joinedAt = if (isAlreadyMember) (devices[deviceId]?.joinedAt ?: now) else now,
                    lastSeenAt = now
                )

                roomRef.child("devices").child(deviceId).setValue(myDevice).await()

                launch(Dispatchers.Main) {
                    attachToRoom(roomId)
                    _errorMessage.value = null
                    onComplete(Result.success(data))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error joining room $roomId", e)
                launch(Dispatchers.Main) {
                    _errorMessage.value = e.message ?: "Unable to join room."
                    onComplete(Result.failure(e))
                }
            }
        }
    }

    private fun attachToRoom(roomId: String) {
        lastProcessedVersion = 0L
        _syncStatus.value = RoomSyncStatus.SYNCING
        val roomRef = database.getReference("rooms/$roomId")

        // Disconnect hook
        roomRef.child("devices").child(deviceId).onDisconnect().updateChildren(
            mapOf("connected" to false, "lastSeenAt" to ServerValue.TIMESTAMP)
        )

        // Connection state
        connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (connected) {
                    _syncStatus.value = RoomSyncStatus.SYNCED
                    roomRef.child("devices").child(deviceId).updateChildren(
                        mapOf("connected" to true, "lastSeenAt" to getEstimatedServerTime())
                    )
                } else {
                    _syncStatus.value = RoomSyncStatus.RECONNECTING
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.getReference(".info/connected").addValueEventListener(connectedListener!!)

        // Room real-time listener
        currentRoomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    handleRoomDeleted()
                    return
                }

                val room = snapshot.getValue(RoomData::class.java) ?: return
                val prev = _currentRoom.value
                latestPlaybackState = room.playbackState

                val isJustProgressTick = prev != null &&
                    prev.roomId == room.roomId &&
                    prev.hostDeviceId == room.hostDeviceId &&
                    prev.playbackState.version == room.playbackState.version &&
                    prev.playbackState.state == room.playbackState.state &&
                    prev.playbackState.song?.videoId == room.playbackState.song?.videoId &&
                    prev.playbackState.volume == room.playbackState.volume &&
                    prev.devices == room.devices

                if (!isJustProgressTick) {
                    _currentRoom.value = room
                }
                _syncStatus.value = RoomSyncStatus.SYNCED

                evaluateHostFailover(room)

                val pb = room.playbackState
                if (!isHost()) {
                    val hostVol = pb.volume
                    if (hostVol in 0..100 && playerController.volume.value != hostVol) {
                        playerController.updateSystemVolume(hostVol)
                        scope.launch(Dispatchers.IO) {
                            try {
                                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                                if (am != null) {
                                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    if (max > 0) {
                                        val target = Math.round((hostVol.toFloat() / 100f) * max).coerceIn(0, max)
                                        if (am.getStreamVolume(AudioManager.STREAM_MUSIC) != target) {
                                            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }

                if (pb.version > lastProcessedVersion) {
                    lastProcessedVersion = pb.version
                    handleIncomingPlaybackState(room, pb)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Room listener cancelled: ${error.message}")
            }
        }
        roomRef.addValueEventListener(currentRoomListener!!)

        // Hardware sync callback when Guest finishes buffering
        playerController.onPlaybackReady = {
            if (!isHost()) {
                // Ensure natural speed when playback commences; drift monitor seamlessly aligns via speed trimming
                playerController.setPlaybackSpeed(1.0f)
            }
        }

        // Start Heartbeat & Drift monitor
        startHeartbeat(roomId)
        startDriftMonitor()
    }

    private fun handleIncomingPlaybackState(room: RoomData, pb: RoomPlaybackState) {
        val isHostDevice = room.hostDeviceId == deviceId
        if (isHostDevice) return // Host device drives audio, does not react to itself

        val roomSong = pb.song ?: return
        val currentSong = playerController.currentSong.value

        val isDifferentSong = currentSong == null || currentSong.videoId != roomSong.videoId
        val needsAudioUrlUpgrade = currentSong != null &&
            currentSong.videoId == roomSong.videoId &&
            currentSong.audioUrl.isNullOrBlank() &&
            roomSong.audioUrl.isNotBlank()

        if (isDifferentSong || needsAudioUrlUpgrade) {
            val converted = YouTubeSong(
                videoId = roomSong.videoId,
                title = roomSong.title,
                channelTitle = roomSong.artist,
                thumbnailUrl = roomSong.artwork,
                audioUrl = roomSong.audioUrl.ifBlank { null },
                durationMs = if (roomSong.durationMs > 0) roomSong.durationMs else 210000L
            )

            // Immediately prime controller with incoming room song so UI receives song without waiting for network stream resolution
            playerController.prepareForPlayback(converted)

            val expectedPos = getExpectedPosition(pb)
            val isPlaying = pb.state == "PLAYING"

            val callback = onGuestPlaySongRequested
            if (callback != null) {
                callback(converted, expectedPos, isPlaying)
            } else {
                playerController.playSong(converted, startPositionSec = expectedPos, autoPlay = isPlaying)
            }
        } else {
            // Same song, playback state changed
            if (pb.state == "PLAYING") {
                val expectedPos = getExpectedPosition(pb)
                val curPos = playerController.currentPositionSec.value
                if (abs(curPos - expectedPos) > 2.0f) {
                    playerController.syncSeekTo(expectedPos)
                }
                playerController.play()
            } else if (pb.state == "PAUSED") {
                playerController.pause()
                playerController.syncSeekTo(pb.positionSec)
            }
        }

        // Synchronize room volume from Host
        if (pb.volume in 0..100 && playerController.volume.value != pb.volume) {
            playerController.updateSystemVolume(pb.volume)
        }
    }

    private fun evaluateHostFailover(room: RoomData) {
        val connectedDevices = room.devices.values.filter { it.connected }
        val hostDevice = room.devices[room.hostDeviceId]

        if (hostDevice == null || !hostDevice.connected) {
            if (connectedDevices.isNotEmpty()) {
                val oldest = connectedDevices.minByOrNull { it.joinedAt }
                if (oldest != null && oldest.deviceId == deviceId && room.hostDeviceId != deviceId) {
                    Log.i(TAG, "Assuming Host responsibility for room ${room.roomId}")
                    val updates = mapOf<String, Any>(
                        "hostDeviceId" to deviceId,
                        "hostDeviceName" to deviceName,
                        "updatedAt" to getEstimatedServerTime()
                    )
                    database.getReference("rooms/${room.roomId}").updateChildren(updates)
                    database.getReference("rooms/${room.roomId}/devices/$deviceId").updateChildren(mapOf("isHost" to true))
                }
            } else {
                database.getReference("rooms/${room.roomId}").removeValue()
            }
        }
    }

    private fun handleRoomDeleted() {
        leaveRoom()
    }

    fun leaveRoom() {
        scope.launch(Dispatchers.IO) {
            leaveRoomInternal()
        }
    }

    private suspend fun leaveRoomInternal() {
        val room = _currentRoom.value ?: return
        val roomId = room.roomId

        try {
            database.getReference("rooms/$roomId/devices/$deviceId").removeValue().await()
            val snap = database.getReference("rooms/$roomId/devices").get().await()

            if (!snap.exists() || snap.childrenCount == 0L) {
                database.getReference("rooms/$roomId").removeValue().await()
            } else {
                val remaining = snap.children.mapNotNull { it.getValue(RoomDevice::class.java) }.filter { it.connected }
                if (isHost() && remaining.isNotEmpty()) {
                    val oldest = remaining.minByOrNull { it.joinedAt }
                    if (oldest != null) {
                        database.getReference("rooms/$roomId").updateChildren(
                            mapOf("hostDeviceId" to oldest.deviceId, "hostDeviceName" to oldest.deviceName)
                        ).await()
                        database.getReference("rooms/$roomId/devices/${oldest.deviceId}").updateChildren(
                            mapOf("isHost" to true)
                        ).await()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error leaving room: ${e.message}")
        } finally {
            cleanupListeners(roomId)
        }
    }

    private fun cleanupListeners(roomId: String) {
        currentRoomListener?.let {
            database.getReference("rooms/$roomId").removeEventListener(it)
        }
        connectedListener?.let {
            database.getReference(".info/connected").removeEventListener(it)
        }
        driftJob?.cancel()
        heartbeatJob?.cancel()
        driftJob = null
        heartbeatJob = null
        currentRoomListener = null
        connectedListener = null
        playerController.onPlaybackReady = null

        scope.launch(Dispatchers.Main) {
            lastProcessedVersion = 0L
            latestPlaybackState = null
            _currentRoom.value = null
            _syncStatus.value = RoomSyncStatus.DISCONNECTED
            playerController.setPlaybackSpeed(1.0f)
        }
    }

    private fun startHeartbeat(roomId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(10000)
                try {
                    database.getReference("rooms/$roomId/devices/$deviceId").updateChildren(
                        mapOf("lastSeenAt" to getEstimatedServerTime(), "connected" to true)
                    )
                } catch (_: Exception) {}
            }
        }
    }

    // ==========================================
    // DRIFT CORRECTION ENGINE
    // ==========================================

    fun getExpectedPosition(state: RoomPlaybackState? = _currentRoom.value?.playbackState): Float {
        val pb = state ?: return 0f
        if (pb.state != "PLAYING") return pb.positionSec

        val currentServer = getEstimatedServerTime()
        val elapsedSec = max(0f, (currentServer - pb.serverTimestamp) / 1000f)
        val maxDurSec = if ((pb.song?.durationMs ?: 0L) > 0) (pb.song!!.durationMs / 1000f) else 300f

        return min(pb.positionSec + elapsedSec, maxDurSec)
    }

    private fun startDriftMonitor() {
        driftJob?.cancel()
        driftJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(400)
                val room = _currentRoom.value ?: continue
                if (room.hostDeviceId == deviceId) {
                    // Host keeps 1.0f speed
                    playerController.setPlaybackSpeed(1.0f)

                    // Periodically calibrate Host position to Firebase every 3.0 seconds
                    if (playerController.isPlaying.value && !playerController.isBuffering.value) {
                        val now = getEstimatedServerTime()
                        val curPos = playerController.getLivePositionSec()
                        if (curPos > 0.1f && now - lastHostSyncTimestamp >= 3000L) {
                            lastHostSyncTimestamp = now
                            val syncUpdates = mapOf<String, Any>(
                                "playbackState/positionSec" to curPos,
                                "playbackState/serverTimestamp" to now,
                                "updatedAt" to now
                            )
                            scope.launch(Dispatchers.IO) {
                                try {
                                    database.getReference("rooms/${room.roomId}").updateChildren(syncUpdates)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    continue
                }

                val pb = latestPlaybackState ?: room.playbackState
                if (pb.state != "PLAYING" || !playerController.isPlaying.value) {
                    playerController.setPlaybackSpeed(1.0f)
                    continue
                }

                // If player is actively buffering from network, do NOT interrupt or seek
                if (playerController.isBuffering.value) {
                    continue
                }

                val expected = getExpectedPosition(pb)
                val actual = playerController.getLivePositionSec()
                val drift = actual - expected
                val absDrift = abs(drift)

                when {
                    // Tier 1: In tight sync (< 40ms) -> Normal 1.0x playback
                    absDrift < 0.040f -> {
                        playerController.setPlaybackSpeed(1.0f)
                    }

                    // Tier 2: Micro drift (40ms - 250ms) -> Smooth pitch-preserved rate trim without buffering
                    absDrift <= 0.25f -> {
                        val rate = if (drift > 0) 0.95f else 1.05f
                        playerController.setPlaybackSpeed(rate)
                    }

                    // Tier 3: Moderate drift (250ms - 2.5s) -> Dynamic Sonic speed catch-up (zero buffer dump, zero stutter)
                    absDrift <= 2.50f -> {
                        val rate = if (drift > 0) 0.90f else 1.10f
                        playerController.setPlaybackSpeed(rate)
                    }

                    // Tier 4: Major drift (> 2.5s) -> Hard seek with safe 5-second debounce
                    else -> {
                        val now = System.currentTimeMillis()
                        if (now - lastSeekTimestamp > 5000L) {
                            lastSeekTimestamp = now
                            Log.i(TAG, "[ListenTogetherManager] Major drift (${drift}s) detected. Performing recovery seek to $expected")
                            playerController.syncSeekTo(expected)
                            playerController.setPlaybackSpeed(1.0f)
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // HOST ACTIONS (Authoritative)
    // ==========================================

    fun hostPlay(positionSec: Float) {
        val room = _currentRoom.value ?: return
        if (!isHost()) return

        val now = getEstimatedServerTime()
        val nextVersion = room.version + 1L

        val updates = mapOf<String, Any>(
            "playbackState/state" to "PLAYING",
            "playbackState/positionSec" to positionSec,
            "playbackState/serverTimestamp" to now,
            "playbackState/version" to nextVersion,
            "playbackState/action" to "PLAY",
            "playbackState/volume" to playerController.volume.value,
            "version" to nextVersion,
            "updatedAt" to now
        )

        database.getReference("rooms/${room.roomId}").updateChildren(updates)
    }

    fun hostPause(positionSec: Float) {
        val room = _currentRoom.value ?: return
        if (!isHost()) return

        val now = getEstimatedServerTime()
        val nextVersion = room.version + 1L

        val updates = mapOf<String, Any>(
            "playbackState/state" to "PAUSED",
            "playbackState/positionSec" to positionSec,
            "playbackState/serverTimestamp" to now,
            "playbackState/version" to nextVersion,
            "playbackState/action" to "PAUSE",
            "version" to nextVersion,
            "updatedAt" to now
        )

        database.getReference("rooms/${room.roomId}").updateChildren(updates)
    }

    fun hostSeek(positionSec: Float) {
        val room = _currentRoom.value ?: return
        if (!isHost()) return

        val now = getEstimatedServerTime()
        val nextVersion = room.version + 1L
        val currentState = room.playbackState.state

        val updates = mapOf<String, Any>(
            "playbackState/state" to currentState,
            "playbackState/positionSec" to positionSec,
            "playbackState/serverTimestamp" to now,
            "playbackState/version" to nextVersion,
            "playbackState/action" to "SEEK",
            "version" to nextVersion,
            "updatedAt" to now
        )

        database.getReference("rooms/${room.roomId}").updateChildren(updates)
    }

    fun hostChangeSong(song: YouTubeSong, positionSec: Float = 0f) {
        val room = _currentRoom.value ?: return
        if (!isHost()) return

        val now = getEstimatedServerTime()
        val nextVersion = room.version + 1L

        val songPayload = RoomSong(
            videoId = song.videoId,
            title = song.title,
            artist = song.channelTitle,
            artwork = song.thumbnailUrl,
            audioUrl = song.audioUrl ?: "",
            durationMs = song.durationMs
        )

        val updates = mapOf<String, Any>(
            "playbackState/state" to "PLAYING",
            "playbackState/positionSec" to positionSec,
            "playbackState/serverTimestamp" to now,
            "playbackState/version" to nextVersion,
            "playbackState/song" to songPayload,
            "playbackState/action" to "SONG_CHANGED",
            "playbackState/volume" to playerController.volume.value,
            "version" to nextVersion,
            "updatedAt" to now
        )

        database.getReference("rooms/${room.roomId}").updateChildren(updates)
    }

    fun hostSetVolume(volumePercent: Int) {
        val room = _currentRoom.value ?: return
        if (!isHost()) return

        val now = getEstimatedServerTime()
        val nextVersion = room.version + 1L
        val clamped = volumePercent.coerceIn(0, 100)

        val updates = mapOf<String, Any>(
            "playbackState/volume" to clamped,
            "playbackState/version" to nextVersion,
            "version" to nextVersion,
            "updatedAt" to now
        )

        scope.launch(Dispatchers.IO) {
            try {
                database.getReference("rooms/${room.roomId}").updateChildren(updates)
            } catch (_: Exception) {}
        }
    }
}
