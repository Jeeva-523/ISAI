import { rtdb } from '../firebase'
import {
  ref,
  set,
  get,
  update,
  remove,
  onValue,
  onDisconnect,
  off,
  DataSnapshot
} from 'firebase/database'
import type { Song } from '@shared/models/song'

export interface RoomDevice {
  deviceId: string
  deviceName: string
  deviceType: 'WEB' | 'ANDROID' | 'IOS'
  isHost: boolean
  connected: boolean
  joinedAt: number
  lastSeenAt: number
}

export interface RoomSong {
  videoId: string
  title: string
  artist: string
  artwork: string
  audioUrl?: string
  durationMs?: number
}

export interface RoomPlaybackState {
  state: 'PLAYING' | 'PAUSED' | 'IDLE'
  positionSec: number
  serverTimestamp: number
  version: number
  song: RoomSong | null
  action?: string
}

export interface RoomData {
  roomId: string
  roomCode: string
  hostDeviceId: string
  hostDeviceName: string
  createdAt: number
  updatedAt: number
  version: number
  playbackState: RoomPlaybackState
  devices: Record<string, RoomDevice>
}

export type SyncStatus = 'SYNCED' | 'SYNCING' | 'RECONNECTING' | 'DISCONNECTED'

export interface DriftAdjustment {
  action: 'NONE' | 'SPEED_ADJUST' | 'SEEK'
  speed?: number
  targetPosition?: number
  driftSec?: number
}

class ListenTogetherServiceManager {
  private deviceId: string = ''
  private deviceName: string = ''
  private serverTimeOffset: number = 0
  private currentRoomId: string | null = null
  private currentRoom: RoomData | null = null
  private lastProcessedVersion: number = 0

  private roomRef: any = null
  private roomListener: ((snap: DataSnapshot) => void) | null = null
  private connectedRef: any = null
  private connectedListener: ((snap: DataSnapshot) => void) | null = null

  private heartbeatInterval: any = null
  private lastSeekTimestamp: number = 0

  private roomUpdateListeners: ((room: RoomData | null) => void)[] = []
  private playbackStateListeners: ((state: RoomPlaybackState, isHost: boolean) => void)[] = []
  private syncStatusListeners: ((status: SyncStatus) => void)[] = []
  private currentSyncStatus: SyncStatus = 'DISCONNECTED'

  constructor() {
    this.initDevice()
    this.initClockOffset()
  }

  private initDevice() {
    if (typeof window === 'undefined') return
    let storedId = localStorage.getItem('isai_room_device_id')
    if (!storedId) {
      storedId = 'web_' + Math.random().toString(36).substring(2, 10) + '_' + Date.now().toString(36)
      localStorage.setItem('isai_room_device_id', storedId)
    }
    this.deviceId = storedId

    // Human-friendly device name
    const ua = navigator.userAgent
    let browserName = 'Web Browser'
    if (ua.includes('Chrome') && !ua.includes('Edg')) browserName = 'Chrome'
    else if (ua.includes('Edg')) browserName = 'Edge'
    else if (ua.includes('Firefox')) browserName = 'Firefox'
    else if (ua.includes('Safari') && !ua.includes('Chrome')) browserName = 'Safari'

    let osName = 'Web'
    if (ua.includes('Win')) osName = 'Windows'
    else if (ua.includes('Mac')) osName = 'Mac'
    else if (ua.includes('Linux')) osName = 'Linux'
    else if (ua.includes('Android')) osName = 'Android'

    this.deviceName = `${browserName} (${osName})`
  }

  private initClockOffset() {
    try {
      const offsetRef = ref(rtdb, '.info/serverTimeOffset')
      onValue(offsetRef, (snap) => {
        const offset = snap.val()
        this.serverTimeOffset = typeof offset === 'number' ? offset : 0
      })
    } catch (e) {
      console.warn('[ListenTogether] Error listening to serverTimeOffset:', e)
    }
  }

  public getDeviceId(): string {
    return this.deviceId
  }

  public getDeviceName(): string {
    return this.deviceName
  }

  public getEstimatedServerTime(): number {
    return Date.now() + this.serverTimeOffset
  }

  public getCurrentRoom(): RoomData | null {
    return this.currentRoom
  }

  public isHost(): boolean {
    if (!this.currentRoom) return false
    return this.currentRoom.hostDeviceId === this.deviceId
  }

  public getSyncStatus(): SyncStatus {
    return this.currentSyncStatus
  }

  private setSyncStatus(status: SyncStatus) {
    if (this.currentSyncStatus !== status) {
      this.currentSyncStatus = status
      this.syncStatusListeners.forEach((l) => l(status))
    }
  }

  // ==========================================
  // ROOM LIFECYCLE
  // ==========================================

  public generateRoomCode(): string {
    const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZ'
    let code = ''
    for (let i = 0; i < 4; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return `ISAI-${code}`
  }

  public async createRoom(initialSong?: Song | null): Promise<RoomData> {
    if (this.currentRoomId) {
      await this.leaveRoom()
    }

    const roomCode = this.generateRoomCode()
    const roomId = roomCode
    const now = this.getEstimatedServerTime()

    const songPayload: RoomSong | null = initialSong
      ? {
          videoId: initialSong.videoId,
          title: initialSong.title,
          artist: initialSong.channelTitle || 'Artist',
          artwork: initialSong.thumbnailUrl,
          audioUrl: initialSong.audioUrl,
          durationMs: initialSong.durationMs || 210000
        }
      : null

    const initialPlayback: RoomPlaybackState = {
      state: initialSong ? 'PLAYING' : 'IDLE',
      positionSec: 0,
      serverTimestamp: now,
      version: 1,
      song: songPayload,
      action: initialSong ? 'PLAY' : 'INIT'
    }

    const initialDevice: RoomDevice = {
      deviceId: this.deviceId,
      deviceName: this.deviceName,
      deviceType: 'WEB',
      isHost: true,
      connected: true,
      joinedAt: now,
      lastSeenAt: now
    }

    const roomData: RoomData = {
      roomId,
      roomCode,
      hostDeviceId: this.deviceId,
      hostDeviceName: this.deviceName,
      createdAt: now,
      updatedAt: now,
      version: 1,
      playbackState: initialPlayback,
      devices: {
        [this.deviceId]: initialDevice
      }
    }

    const roomDatabaseRef = ref(rtdb, `rooms/${roomId}`)
    await set(roomDatabaseRef, roomData)

    await this.attachToRoom(roomId)
    return roomData
  }

  public async joinRoom(roomCodeOrId: string): Promise<RoomData> {
    const normalized = roomCodeOrId.trim().toUpperCase()
    const roomId = normalized.startsWith('ISAI-') ? normalized : `ISAI-${normalized}`

    if (this.currentRoomId === roomId && this.currentRoom) {
      return this.currentRoom
    }

    if (this.currentRoomId) {
      await this.leaveRoom()
    }

    const roomDatabaseRef = ref(rtdb, `rooms/${roomId}`)
    const snap = await get(roomDatabaseRef)

    if (!snap.exists()) {
      throw new Error('Room not found. Please check the room code.')
    }

    const data = snap.val() as RoomData
    const devices = data.devices || {}

    // Check device count limit: Maximum 3 devices allowed
    const isAlreadyMember = Boolean(devices[this.deviceId])
    const connectedDevices = Object.values(devices).filter((d) => d && d.connected)

    if (!isAlreadyMember && connectedDevices.length >= 3) {
      throw new Error('Maximum 3 devices are allowed in this room.')
    }

    const now = this.getEstimatedServerTime()
    const myDeviceData: RoomDevice = {
      deviceId: this.deviceId,
      deviceName: this.deviceName,
      deviceType: 'WEB',
      isHost: data.hostDeviceId === this.deviceId,
      connected: true,
      joinedAt: isAlreadyMember && devices[this.deviceId].joinedAt ? devices[this.deviceId].joinedAt : now,
      lastSeenAt: now
    }

    await set(ref(rtdb, `rooms/${roomId}/devices/${this.deviceId}`), myDeviceData)
    await this.attachToRoom(roomId)
    return data
  }

  private async attachToRoom(roomId: string) {
    this.currentRoomId = roomId
    this.setSyncStatus('SYNCING')

    const roomDatabaseRef = ref(rtdb, `rooms/${roomId}`)
    this.roomRef = roomDatabaseRef

    // Setup onDisconnect hook for my device in RTDB
    const myDeviceRef = ref(rtdb, `rooms/${roomId}/devices/${this.deviceId}`)
    onDisconnect(myDeviceRef).update({
      connected: false,
      lastSeenAt: this.getEstimatedServerTime()
    })

    // Listen to Firebase connection state
    this.connectedRef = ref(rtdb, '.info/connected')
    this.connectedListener = onValue(this.connectedRef, (snap) => {
      const isConnected = snap.val() === true
      if (isConnected) {
        if (this.currentRoomId) {
          update(ref(rtdb, `rooms/${this.currentRoomId}/devices/${this.deviceId}`), {
            connected: true,
            lastSeenAt: this.getEstimatedServerTime()
          }).catch(() => {})
          this.setSyncStatus('SYNCED')
        }
      } else {
        this.setSyncStatus('RECONNECTING')
      }
    })

    // Room real-time data listener
    this.roomListener = onValue(roomDatabaseRef, (snap) => {
      if (!snap.exists()) {
        this.handleRoomDeleted()
        return
      }

      const roomData = snap.val() as RoomData
      this.currentRoom = roomData
      this.setSyncStatus('SYNCED')

      // Check for host failover if host disconnected
      this.evaluateHostFailover(roomData)

      // Notify room updates
      this.roomUpdateListeners.forEach((l) => l(roomData))

      // Check playback state version
      const pb = roomData.playbackState
      if (pb) {
        if (pb.version >= this.lastProcessedVersion) {
          this.lastProcessedVersion = pb.version
          const isHost = roomData.hostDeviceId === this.deviceId
          this.playbackStateListeners.forEach((l) => l(pb, isHost))
        }
      }
    })

    // Heartbeat every 10 seconds to update lastSeenAt
    this.heartbeatInterval = setInterval(() => {
      if (this.currentRoomId) {
        update(ref(rtdb, `rooms/${this.currentRoomId}/devices/${this.deviceId}`), {
          lastSeenAt: this.getEstimatedServerTime(),
          connected: true
        }).catch(() => {})
      }
    }, 10000)
  }

  private evaluateHostFailover(room: RoomData) {
    if (!room || !room.devices) return

    const devices = Object.values(room.devices).filter((d) => d && d.connected)
    const hostDevice = room.devices[room.hostDeviceId]

    // If host is disconnected or missing
    if (!hostDevice || !hostDevice.connected) {
      if (devices.length > 0) {
        // Sort by joinedAt ascending (oldest connected device)
        const sorted = [...devices].sort((a, b) => (a.joinedAt || 0) - (b.joinedAt || 0))
        const nextHost = sorted[0]

        if (nextHost.deviceId === this.deviceId && room.hostDeviceId !== this.deviceId) {
          // Current device assumes host responsibility
          console.log('[ListenTogether] Assuming Host responsibility for room:', room.roomId)
          update(ref(rtdb, `rooms/${room.roomId}`), {
            hostDeviceId: this.deviceId,
            hostDeviceName: this.deviceName,
            updatedAt: this.getEstimatedServerTime()
          }).catch(() => {})

          update(ref(rtdb, `rooms/${room.roomId}/devices/${this.deviceId}`), {
            isHost: true
          }).catch(() => {})
        }
      } else {
        // No devices remaining, cleanup room
        remove(ref(rtdb, `rooms/${room.roomId}`)).catch(() => {})
      }
    }
  }

  private handleRoomDeleted() {
    this.cleanupRoomState()
    this.roomUpdateListeners.forEach((l) => l(null))
  }

  public async leaveRoom(): Promise<void> {
    if (!this.currentRoomId) return
    const roomId = this.currentRoomId

    try {
      const myDeviceRef = ref(rtdb, `rooms/${roomId}/devices/${this.deviceId}`)
      await remove(myDeviceRef)

      // Fetch remaining devices to either transfer host or delete room
      const snap = await get(ref(rtdb, `rooms/${roomId}/devices`))
      if (!snap.exists() || Object.keys(snap.val() || {}).length === 0) {
        await remove(ref(rtdb, `rooms/${roomId}`))
      } else {
        const remaining = Object.values(snap.val() as Record<string, RoomDevice>).filter(
          (d) => d && d.connected
        )
        if (this.isHost() && remaining.length > 0) {
          const sorted = remaining.sort((a, b) => (a.joinedAt || 0) - (b.joinedAt || 0))
          const newHost = sorted[0]
          await update(ref(rtdb, `rooms/${roomId}`), {
            hostDeviceId: newHost.deviceId,
            hostDeviceName: newHost.deviceName,
            updatedAt: this.getEstimatedServerTime()
          })
          await update(ref(rtdb, `rooms/${roomId}/devices/${newHost.deviceId}`), {
            isHost: true
          })
        }
      }
    } catch (e) {
      console.warn('[ListenTogether] Leave room error:', e)
    } finally {
      this.cleanupRoomState()
    }
  }

  private cleanupRoomState() {
    if (this.roomRef && this.roomListener) {
      off(this.roomRef, 'value', this.roomListener)
    }
    if (this.connectedRef && this.connectedListener) {
      off(this.connectedRef, 'value', this.connectedListener)
    }
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }

    this.currentRoomId = null
    this.currentRoom = null
    this.roomRef = null
    this.roomListener = null
    this.lastProcessedVersion = 0
    this.setSyncStatus('DISCONNECTED')
  }

  // ==========================================
  // HOST CONTROLS (Authoritative)
  // ==========================================

  public async hostPlay(positionSec: number) {
    if (!this.currentRoomId || !this.isHost()) return
    const now = this.getEstimatedServerTime()
    const nextVersion = (this.currentRoom?.version || 1) + 1

    const updatePayload: Partial<RoomPlaybackState> = {
      state: 'PLAYING',
      positionSec,
      serverTimestamp: now,
      version: nextVersion,
      action: 'PLAY'
    }

    await update(ref(rtdb, `rooms/${this.currentRoomId}/playbackState`), updatePayload)
    await update(ref(rtdb, `rooms/${this.currentRoomId}`), {
      version: nextVersion,
      updatedAt: now
    })
  }

  public async hostPause(positionSec: number) {
    if (!this.currentRoomId || !this.isHost()) return
    const now = this.getEstimatedServerTime()
    const nextVersion = (this.currentRoom?.version || 1) + 1

    const updatePayload: Partial<RoomPlaybackState> = {
      state: 'PAUSED',
      positionSec,
      serverTimestamp: now,
      version: nextVersion,
      action: 'PAUSE'
    }

    await update(ref(rtdb, `rooms/${this.currentRoomId}/playbackState`), updatePayload)
    await update(ref(rtdb, `rooms/${this.currentRoomId}`), {
      version: nextVersion,
      updatedAt: now
    })
  }

  public async hostSeek(positionSec: number) {
    if (!this.currentRoomId || !this.isHost()) return
    const now = this.getEstimatedServerTime()
    const nextVersion = (this.currentRoom?.version || 1) + 1

    const currentState = this.currentRoom?.playbackState?.state || 'PLAYING'
    const updatePayload: Partial<RoomPlaybackState> = {
      state: currentState,
      positionSec,
      serverTimestamp: now,
      version: nextVersion,
      action: 'SEEK'
    }

    await update(ref(rtdb, `rooms/${this.currentRoomId}/playbackState`), updatePayload)
    await update(ref(rtdb, `rooms/${this.currentRoomId}`), {
      version: nextVersion,
      updatedAt: now
    })
  }

  public async hostChangeSong(song: Song, autoPlay: boolean = true) {
    if (!this.currentRoomId || !this.isHost()) return
    const now = this.getEstimatedServerTime()
    const nextVersion = (this.currentRoom?.version || 1) + 1

    const songPayload: RoomSong = {
      videoId: song.videoId,
      title: song.title,
      artist: song.channelTitle || 'Artist',
      artwork: song.thumbnailUrl,
      audioUrl: song.audioUrl,
      durationMs: song.durationMs || 210000
    }

    const updatePayload: RoomPlaybackState = {
      state: autoPlay ? 'PLAYING' : 'PAUSED',
      positionSec: 0,
      serverTimestamp: now,
      version: nextVersion,
      song: songPayload,
      action: 'SONG_CHANGED'
    }

    await update(ref(rtdb, `rooms/${this.currentRoomId}/playbackState`), updatePayload)
    await update(ref(rtdb, `rooms/${this.currentRoomId}`), {
      version: nextVersion,
      updatedAt: now
    })
  }

  // ==========================================
  // SYNCHRONIZATION & DRIFT ALGORITHM
  // ==========================================

  /**
   * Calculates the exact mathematical playback position that this song SHOULD be at right now
   * based on server timestamp and elapsed time.
   */
  public getExpectedPosition(): number {
    if (!this.currentRoom || !this.currentRoom.playbackState) return 0
    const pb = this.currentRoom.playbackState

    if (pb.state === 'PAUSED' || pb.state === 'IDLE') {
      return pb.positionSec || 0
    }

    const currentServerTime = this.getEstimatedServerTime()
    const elapsedSec = Math.max(0, (currentServerTime - (pb.serverTimestamp || currentServerTime)) / 1000)
    const durationSec = (pb.song?.durationMs || 300000) / 1000
    const target = (pb.positionSec || 0) + elapsedSec

    return Math.min(target, durationSec)
  }

  /**
   * Evaluates drift between local audio player position and expected room playback position.
   * Returns a 3-tier correction strategy:
   * - Tier 1: |drift| < 0.20s -> Imperceptible, keep normal speed (1.0x).
   * - Tier 2: 0.20s <= |drift| <= 1.5s -> Smooth speed adjustment (0.96x or 1.04x) without audible seek pops.
   * - Tier 3: |drift| > 1.5s -> Controlled debounced seek to target position.
   */
  public calculateDriftAdjustment(actualAudioPositionSec: number): DriftAdjustment {
    if (!this.currentRoom || !this.currentRoom.playbackState) {
      return { action: 'NONE', speed: 1.0 }
    }

    const pb = this.currentRoom.playbackState
    if (pb.state !== 'PLAYING') {
      return { action: 'NONE', speed: 1.0 }
    }

    const expected = this.getExpectedPosition()
    const drift = actualAudioPositionSec - expected // positive = guest is ahead, negative = guest is behind
    const absDrift = Math.abs(drift)

    // Tier 1: In tight sync (within 200ms)
    if (absDrift < 0.20) {
      return { action: 'NONE', speed: 1.0, driftSec: drift }
    }

    // Tier 2: Moderate drift (200ms to 1.5s) -> Smooth rate nudge
    if (absDrift <= 1.5) {
      // If guest is ahead, slow down slightly (0.96); if behind, speed up slightly (1.04)
      const adjustedSpeed = drift > 0 ? 0.96 : 1.04
      return {
        action: 'SPEED_ADJUST',
        speed: adjustedSpeed,
        driftSec: drift
      }
    }

    // Tier 3: Large drift (> 1.5s) -> Perform debounced seek
    const now = Date.now()
    if (now - this.lastSeekTimestamp > 2500) {
      this.lastSeekTimestamp = now
      return {
        action: 'SEEK',
        targetPosition: expected,
        speed: 1.0,
        driftSec: drift
      }
    }

    return { action: 'NONE', speed: 1.0, driftSec: drift }
  }

  // ==========================================
  // EVENT SUBSCRIPTIONS
  // ==========================================

  public subscribeRoomUpdate(callback: (room: RoomData | null) => void): () => void {
    this.roomUpdateListeners.push(callback)
    callback(this.currentRoom)
    return () => {
      this.roomUpdateListeners = this.roomUpdateListeners.filter((cb) => cb !== callback)
    }
  }

  public subscribePlaybackState(
    callback: (state: RoomPlaybackState, isHost: boolean) => void
  ): () => void {
    this.playbackStateListeners.push(callback)
    if (this.currentRoom?.playbackState) {
      callback(this.currentRoom.playbackState, this.isHost())
    }
    return () => {
      this.playbackStateListeners = this.playbackStateListeners.filter((cb) => cb !== callback)
    }
  }

  public subscribeSyncStatus(callback: (status: SyncStatus) => void): () => void {
    this.syncStatusListeners.push(callback)
    callback(this.currentSyncStatus)
    return () => {
      this.syncStatusListeners = this.syncStatusListeners.filter((cb) => cb !== callback)
    }
  }
}

export const ListenTogetherService = new ListenTogetherServiceManager()
