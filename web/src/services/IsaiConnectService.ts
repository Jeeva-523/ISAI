import { db } from '../firebase'
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
}

export interface PlaybackStateSync {
  currentDeviceId: string
  currentSongId: string
  currentTitle: string
  currentArtist: string
  currentArtwork: string
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

class IsaiConnectServiceManager {
  private userId: string = ''
  private deviceId: string = ''
  private deviceName: string = ''
  private platform: 'web' | 'windows' | 'mac' = 'web'
  private heartbeatTimer: any = null
  private devicesListener: (() => void) | null = null
  private playbackStateListener: (() => void) | null = null

  public currentDevices: DeviceInfo[] = []
  public currentPlaybackState: PlaybackStateSync | null = null

  private stateChangeCallbacks: ((state: PlaybackStateSync | null) => void)[] = []
  private devicesChangeCallbacks: ((devices: DeviceInfo[]) => void)[] = []

  constructor() {
    this.deviceId = this.getOrCreateDeviceId()
    this.deviceName = this.detectDeviceName()
    this.platform = this.detectPlatform()
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

  public getMyDeviceId(): string {
    return this.deviceId
  }

  public getMyDeviceName(): string {
    return this.deviceName
  }

  public sanitizeUserId(rawId: string): string {
    if (!rawId) return 'user_jeeva_default'
    return rawId.replace(/[.#$\[\]]/g, '_').toLowerCase().trim()
  }

  public initialize(rawUserId: string) {
    const sanitized = this.sanitizeUserId(rawUserId)
    if (this.userId === sanitized) return
    this.disconnect()

    this.userId = sanitized
    console.log(`[ISAI Connect] Initializing device ${this.deviceId} (${this.deviceName}) for user: ${this.userId}`)

    this.registerDevice()
    this.startHeartbeat()
    this.listenToDevices()
    this.listenToPlaybackState()
  }

  private registerDevice() {
    if (!this.userId) return
    const deviceRef = ref(db, `connect/${this.userId}/devices/${this.deviceId}`)
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
    this.heartbeatTimer = setInterval(() => {
      if (!this.userId) return
      const deviceRef = ref(db, `connect/${this.userId}/devices/${this.deviceId}`)
      update(deviceRef, {
        lastActiveAt: Date.now(),
        isActive: true
      }).catch(() => {})
    }, 15000)
  }

  private listenToDevices() {
    if (!this.userId) return
    const devicesRef = ref(db, `connect/${this.userId}/devices`)
    this.devicesListener = onValue(devicesRef, (snapshot) => {
      const val = snapshot.val()
      if (!val) {
        this.currentDevices = []
      } else {
        const list: DeviceInfo[] = Object.values(val)
        // Sort active first, then most recently active
        this.currentDevices = list.sort((a, b) => (b.lastActiveAt || 0) - (a.lastActiveAt || 0))
      }
      this.devicesChangeCallbacks.forEach(cb => cb(this.currentDevices))
    })
  }

  private listenToPlaybackState() {
    if (!this.userId) return
    const stateRef = ref(db, `connect/${this.userId}/playbackState`)
    this.playbackStateListener = onValue(stateRef, (snapshot) => {
      const val = snapshot.val()
      if (val) {
        this.currentPlaybackState = val
        this.stateChangeCallbacks.forEach(cb => cb(val))
      }
    })
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
    const stateRef = ref(db, `connect/${this.userId}/playbackState`)
    const updated: Partial<PlaybackStateSync> = {
      ...partial,
      updatedAt: Date.now(),
      updatedByDeviceId: this.deviceId
    }
    // If no active device was set yet, claim ownership
    if (!this.currentPlaybackState?.currentDeviceId) {
      updated.currentDeviceId = this.deviceId
    }
    update(stateRef, updated).catch(err => {
      console.warn('[ISAI Connect] State update error:', err)
    })
  }

  public transferPlaybackToDevice(targetDeviceId: string) {
    if (!this.userId) return
    console.log(`[ISAI Connect] Handoff playback to device: ${targetDeviceId}`)
    this.updatePlaybackState({
      currentDeviceId: targetDeviceId
    })
  }

  public disconnect() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
    if (this.devicesListener) {
      this.devicesListener()
      this.devicesListener = null
    }
    if (this.playbackStateListener) {
      this.playbackStateListener()
      this.playbackStateListener = null
    }
    if (this.userId) {
      const deviceRef = ref(db, `connect/${this.userId}/devices/${this.deviceId}`)
      update(deviceRef, { isActive: false, lastActiveAt: Date.now() }).catch(() => {})
      const devicesRef = ref(db, `connect/${this.userId}/devices`)
      const stateRef = ref(db, `connect/${this.userId}/playbackState`)
      off(devicesRef)
      off(stateRef)
    }
    this.userId = ''
  }
}

export const IsaiConnectService = new IsaiConnectServiceManager()
