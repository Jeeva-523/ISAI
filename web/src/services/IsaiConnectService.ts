import { rtdb } from '../firebase'
import { ref, set, update, onValue, onDisconnect, off } from 'firebase/database'

export interface DeviceInfo {
  deviceId: string
  deviceName: string
  platform: 'web' | 'android' | 'windows' | 'mac' | 'ios' | 'tablet'
  lastActiveAt: number
  isActive: boolean
}

export interface SyncSong {
  id: string
  title: string
  artist: string
  artwork: string
  duration?: string
  audioUrl?: string
}

export interface PlaybackStateSync {
  currentDeviceId: string
  currentSongId: string
  currentTitle: string
  currentArtist: string
  currentArtwork: string
  currentAudioUrl?: string
  durationMs: number
  positionMs: number
  isPlaying: boolean
  volume: number
  shuffle: boolean
  repeatMode: string
  queueIndex: number
  queue: SyncSong[]
  updatedAt: number
  updatedByDeviceId: string
}

export interface RemoteCommand {
  action: 'PLAY' | 'PAUSE' | 'NEXT' | 'PREV' | 'SEEK' | 'PLAY_SONG' | 'SET_VOLUME'
  targetDeviceId?: string
  positionMs?: number
  songId?: string
  songTitle?: string
  songArtist?: string
  songArtwork?: string
  songAudioUrl?: string
  volume?: number
  timestamp: number
  issuedByDeviceId: string
}

class IsaiConnectServiceManager {
  private userId: string = ''
  private deviceId: string = ''
  private deviceName: string = ''
  private platform: 'web' | 'windows' | 'mac' = 'web'
  private heartbeatTimer: any = null
  private livenessTimer: any = null
  private devicesListener: (() => void) | null = null
  private playbackStateListener: (() => void) | null = null
  private commandListener: (() => void) | null = null

  public rawDevices: DeviceInfo[] = []
  public currentDevices: DeviceInfo[] = []
  public currentPlaybackState: PlaybackStateSync | null = null
  private commandCallbacks: ((cmd: RemoteCommand) => void)[] = []

  private stateChangeCallbacks: ((state: PlaybackStateSync | null) => void)[] = []
  private devicesChangeCallbacks: ((devices: DeviceInfo[]) => void)[] = []

  constructor() {
    this.deviceId = this.getOrCreateDeviceId()
    this.deviceName = this.detectDeviceName()
    this.platform = this.detectPlatform()
    this.setupWindowListeners()
  }

  private setupWindowListeners() {
    if (typeof window === 'undefined') return
    const setInactive = () => {
      if (!this.userId) return
      const deviceRef = ref(rtdb, `connect/${this.userId}/devices/${this.deviceId}`)
      update(deviceRef, {
        isActive: false,
        lastActiveAt: Date.now()
      }).catch(() => {})
    }
    window.addEventListener('beforeunload', setInactive)
    window.addEventListener('pagehide', setInactive)
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        this.sendHeartbeat()
      }
    })
  }

  public isDeviceOnline(device: DeviceInfo): boolean {
    return this.isDeviceAvailable(device)
  }

  public isDeviceAvailable(device: DeviceInfo): boolean {
    if (!device) return false
    const diffMs = Date.now() - (device.lastActiveAt || 0)
    // Device must be actively running (isActive is true AND heartbeat received within 40 seconds)
    return Boolean(device.isActive) && diffMs <= 40000
  }

  public sendHeartbeat() {
    if (!this.userId) return
    const deviceRef = ref(rtdb, `connect/${this.userId}/devices/${this.deviceId}`)
    update(deviceRef, {
      lastActiveAt: Date.now(),
      isActive: true
    }).catch(() => {})
  }

  private getOrCreateDeviceId(): string {
    if (typeof window === 'undefined') return 'web_default'
    let id = localStorage.getItem('isai_device_id')
    if (!id) {
      id = `web_${Date.now().toString(36)}_${Math.random().toString(36).substring(2, 6)}`
      localStorage.setItem('isai_device_id', id)
    }
    return id
  }

  private detectDeviceName(): string {
    if (typeof navigator === 'undefined') return "Jeeva's Web Browser"
    const ua = navigator.userAgent
    let browser = 'Browser'
    if (ua.includes('Edg/')) browser = 'Edge'
    else if (ua.includes('Chrome/')) browser = 'Chrome'
    else if (ua.includes('Safari/')) browser = 'Safari'
    else if (ua.includes('Firefox/')) browser = 'Firefox'

    let os = 'Web'
    if (ua.includes('Win')) os = 'Windows Laptop'
    else if (ua.includes('Mac')) os = 'Mac'
    else if (ua.includes('Android')) os = 'Android Device'
    else if (ua.includes('iPhone') || ua.includes('iPad')) os = 'iOS Device'

    return `Jeeva's ${os} (${browser})`
  }

  private detectPlatform(): 'web' | 'windows' | 'mac' {
    if (typeof navigator === 'undefined') return 'web'
    const ua = navigator.userAgent
    if (ua.includes('Win')) return 'windows'
    if (ua.includes('Mac')) return 'mac'
    return 'web'
  }

  private userEmail: string = ''

  public getMyDeviceId(): string {
    return this.deviceId
  }

  public getMyDeviceName(): string {
    return this.deviceName
  }

  public getUserEmail(): string {
    return this.userEmail
  }

  public sanitizeUserId(rawId: string): string {
    if (!rawId) return ''
    return rawId.toLowerCase().trim().replace(/[.#$\[\]]/g, '_')
  }

  public initialize(rawUserId: string) {
    const sanitized = this.sanitizeUserId(rawUserId)
    if (this.userId === sanitized) return
    this.disconnect()

    this.userEmail = rawUserId
    this.userId = sanitized
    console.log(`[ISAI Connect] Initializing device ${this.deviceId} (${this.deviceName}) for user: ${this.userId}`)

    this.registerDevice()
    this.startHeartbeat()
    this.listenToDevices()
    this.listenToPlaybackState()
    this.listenToCommands()
  }

  private registerDevice() {
    if (!this.userId) return
    const deviceRef = ref(rtdb, `connect/${this.userId}/devices/${this.deviceId}`)
    const deviceInfo: DeviceInfo = {
      deviceId: this.deviceId,
      deviceName: this.deviceName,
      platform: this.platform,
      lastActiveAt: Date.now(),
      isActive: true
    }

    set(deviceRef, deviceInfo).catch(err => {
      console.warn('[ISAI Connect] Device registration warning:', err)
    })

    // Setup disconnect handler
    onDisconnect(deviceRef).update({
      isActive: false,
      lastActiveAt: Date.now()
    }).catch(() => {})
  }

  private startHeartbeat() {
    if (this.heartbeatTimer) clearInterval(this.heartbeatTimer)
    this.sendHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.sendHeartbeat()
    }, 10000)
  }

  private deduplicateDevices(list: DeviceInfo[]): DeviceInfo[] {
    const activePlayerId = this.currentPlaybackState?.currentDeviceId || ''
    const map = new Map<string, DeviceInfo>()
    for (const d of list) {
      const key = d.deviceName?.trim() || d.deviceId
      const existing = map.get(key)
      if (!existing) {
        map.set(key, d)
      } else {
        if (d.deviceId === activePlayerId) {
          map.set(key, d)
        } else if (existing.deviceId !== activePlayerId && (d.lastActiveAt || 0) > (existing.lastActiveAt || 0)) {
          map.set(key, d)
        }
      }
    }
    return Array.from(map.values())
  }

  private listenToDevices() {
    if (!this.userId) return
    const devicesRef = ref(rtdb, `connect/${this.userId}/devices`)
    this.devicesListener = onValue(devicesRef, (snapshot) => {
      const val = snapshot.val()
      if (!val) {
        this.rawDevices = []
        this.currentDevices = []
      } else {
        const list: DeviceInfo[] = Object.values(val)
        this.rawDevices = list
        this.currentDevices = this.deduplicateDevices(
          list
            .filter(d => this.isDeviceAvailable(d) || d.deviceId === this.deviceId)
            .sort((a, b) => (b.lastActiveAt || 0) - (a.lastActiveAt || 0))
        )
      }
      this.devicesChangeCallbacks.forEach(cb => cb(this.currentDevices))
    })

    // Periodic liveness check: drops offline devices automatically
    if (this.livenessTimer) clearInterval(this.livenessTimer)
    this.livenessTimer = setInterval(() => {
      if (this.rawDevices.length > 0) {
        const activeList = this.deduplicateDevices(
          this.rawDevices
            .filter(d => this.isDeviceAvailable(d) || d.deviceId === this.deviceId)
            .sort((a, b) => (b.lastActiveAt || 0) - (a.lastActiveAt || 0))
        )

        const changed = activeList.length !== this.currentDevices.length ||
          activeList.some((d, idx) => d.deviceId !== this.currentDevices[idx]?.deviceId)

        if (changed) {
          this.currentDevices = activeList
          this.devicesChangeCallbacks.forEach(cb => cb(this.currentDevices))
        }
      }
    }, 5000)
  }

  private listenToPlaybackState() {
    if (!this.userId) return
    const stateRef = ref(rtdb, `connect/${this.userId}/playbackState`)
    this.playbackStateListener = onValue(stateRef, (snapshot) => {
      const val = snapshot.val()
      if (val) {
        this.currentPlaybackState = val
        this.stateChangeCallbacks.forEach(cb => cb(val))
      }
    })
  }

  private listenToCommands() {
    if (!this.userId) return
    const cmdRef = ref(rtdb, `connect/${this.userId}/command`)
    this.commandListener = onValue(cmdRef, (snapshot) => {
      const val = snapshot.val()
      if (val && val.timestamp && val.issuedByDeviceId !== this.deviceId) {
        // Only process fresh commands (less than 60 seconds old, clock skew tolerant)
        if (Math.abs(Date.now() - val.timestamp) < 60000) {
          this.commandCallbacks.forEach(cb => cb(val))
        }
      }
    })
  }

  public subscribeCommands(callback: (cmd: RemoteCommand) => void) {
    this.commandCallbacks.push(callback)
    return () => {
      this.commandCallbacks = this.commandCallbacks.filter(c => c !== callback)
    }
  }

  public sendCommand(
    action: 'PLAY' | 'PAUSE' | 'NEXT' | 'PREV' | 'SEEK' | 'PLAY_SONG' | 'SET_VOLUME',
    data?: { positionMs?: number; song?: any; targetDeviceId?: string; volume?: number }
  ) {
    if (!this.userId) return
    const cmdRef = ref(rtdb, `connect/${this.userId}/command`)
    const cmd: RemoteCommand = {
      action,
      targetDeviceId: data?.targetDeviceId || '',
      positionMs: data?.positionMs || 0,
      volume: data?.volume,
      songId: data?.song?.videoId || data?.song?.id || '',
      songTitle: data?.song?.title || '',
      songArtist: data?.song?.channelTitle || data?.song?.artist || '',
      songArtwork: data?.song?.thumbnailUrl || data?.song?.artwork || '',
      songAudioUrl: data?.song?.audioUrl || '',
      timestamp: Date.now(),
      issuedByDeviceId: this.deviceId
    }
    set(cmdRef, cmd).catch(err => {
      console.warn('[ISAI Connect] Send command warning:', err)
    })
  }

  public isMyDeviceActive(): boolean {
    return Boolean(this.currentPlaybackState?.currentDeviceId === this.deviceId)
  }

  public subscribePlaybackState(callback: (state: PlaybackStateSync | null) => void) {
    this.stateChangeCallbacks.push(callback)
    if (this.currentPlaybackState) callback(this.currentPlaybackState)
    return () => {
      this.stateChangeCallbacks = this.stateChangeCallbacks.filter(c => c !== callback)
    }
  }

  public subscribeDevices(callback: (devices: DeviceInfo[]) => void) {
    this.devicesChangeCallbacks.push(callback)
    if (this.currentDevices.length > 0) callback(this.currentDevices)
    return () => {
      this.devicesChangeCallbacks = this.devicesChangeCallbacks.filter(c => c !== callback)
    }
  }

  public updatePlaybackState(partial: Partial<PlaybackStateSync>) {
    if (!this.userId) return
    const stateRef = ref(rtdb, `connect/${this.userId}/playbackState`)
    const updated: Partial<PlaybackStateSync> = {
      ...partial,
      updatedAt: Date.now(),
      updatedByDeviceId: this.deviceId
    }
    // If no active device was set yet and none was specified, claim ownership
    if (!this.currentPlaybackState?.currentDeviceId && !partial.currentDeviceId) {
      updated.currentDeviceId = this.deviceId
    }

    // Optimistically update cached playback state and notify subscribers immediately
    this.currentPlaybackState = {
      ...(this.currentPlaybackState || {}),
      ...updated
    } as PlaybackStateSync
    this.stateChangeCallbacks.forEach(cb => cb(this.currentPlaybackState))

    update(stateRef, updated).catch(err => {
      console.warn('[ISAI Connect] State update error:', err)
    })
  }

  public transferPlaybackToDevice(targetDeviceId: string, song?: any, positionMs?: number) {
    if (!this.userId) return
    console.log(`[ISAI Connect] Handoff playback to device: ${targetDeviceId}`)
    const updatePayload: Partial<PlaybackStateSync> = {
      currentDeviceId: targetDeviceId,
      isPlaying: true
    }
    if (song) {
      updatePayload.currentSongId = song.videoId || song.id || ''
      updatePayload.currentTitle = song.title || ''
      updatePayload.currentArtist = song.channelTitle || song.artist || ''
      updatePayload.currentArtwork = song.thumbnailUrl || song.artwork || ''
      updatePayload.currentAudioUrl = song.audioUrl || ''
    }
    if (positionMs !== undefined) {
      updatePayload.positionMs = positionMs
    }
    this.updatePlaybackState(updatePayload)
  }

  public syncRecentlyPlayed(songs: any[]) {
    if (!this.userId || !songs || songs.length === 0) return
    const clean = songs.slice(0, 20).map(s => ({
      id: s.videoId || s.id || '',
      title: s.title || '',
      artist: s.channelTitle || s.artist || '',
      artwork: s.thumbnailUrl || s.artwork || '',
      audioUrl: s.audioUrl || ''
    }))
    const historyRef = ref(rtdb, `connect/${this.userId}/recentlyPlayed`)
    set(historyRef, clean).catch(e => console.warn('[ISAI Connect] syncRecentlyPlayed warning:', e))
  }

  public subscribeRecentlyPlayed(callback: (songs: any[]) => void): () => void {
    if (!this.userId) return () => {}
    const historyRef = ref(rtdb, `connect/${this.userId}/recentlyPlayed`)
    onValue(historyRef, (snapshot) => {
      const data = snapshot.val()
      if (data) {
        const raw = Array.isArray(data) ? data : (typeof data === 'object' ? Object.values(data) : [])
        const clean = (raw || []).filter(Boolean).map((s: any) => ({
          videoId: s.id || s.videoId || '',
          title: s.title || '',
          channelTitle: s.artist || s.channelTitle || '',
          thumbnailUrl: s.artwork || s.thumbnailUrl || '',
          audioUrl: s.audioUrl || '',
          durationFormatted: '3:30',
          durationMs: 210000,
          viewCountFormatted: ''
        }))
        if (clean.length > 0) {
          callback(clean)
        }
      }
    })
    return () => off(historyRef)
  }

  public disconnect() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
    if (this.livenessTimer) {
      clearInterval(this.livenessTimer)
      this.livenessTimer = null
    }
    if (this.devicesListener) {
      this.devicesListener()
      this.devicesListener = null
    }
    if (this.playbackStateListener) {
      this.playbackStateListener()
      this.playbackStateListener = null
    }
    if (this.commandListener) {
      this.commandListener()
      this.commandListener = null
    }
    if (this.userId) {
      const deviceRef = ref(rtdb, `connect/${this.userId}/devices/${this.deviceId}`)
      update(deviceRef, { isActive: false, lastActiveAt: Date.now() }).catch(() => {})
      const devicesRef = ref(rtdb, `connect/${this.userId}/devices`)
      const stateRef = ref(rtdb, `connect/${this.userId}/playbackState`)
      const cmdRef = ref(rtdb, `connect/${this.userId}/command`)
      const historyRef = ref(rtdb, `connect/${this.userId}/recentlyPlayed`)
      off(devicesRef)
      off(stateRef)
      off(cmdRef)
      off(historyRef)
    }
    this.userId = ''
  }
}

export const IsaiConnectService = new IsaiConnectServiceManager()
