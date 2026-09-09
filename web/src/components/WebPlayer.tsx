import React, { useRef, useState, useEffect } from 'react'
import type { Song } from '@shared/models/song'
import { musicApi } from '@shared/api/music-api'
import { cleanHtmlTitle } from '@shared/utils/formatters'
import { DeviceInfo, IsaiConnectService, PlaybackStateSync } from '../services/IsaiConnectService'
import { recommendationService } from '../services/RecommendationService'
import { IsaiConnectModal } from './IsaiConnectModal'
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Volume2,
  VolumeX,
  Heart,
  ListMusic,
  Laptop,
  Maximize2,
  Minimize2,
  GripVertical,
  Trash2
} from 'lucide-react'

interface WebPlayerProps {
  song: Song | null
  remoteState?: PlaybackStateSync | null
  connectedDevices?: DeviceInfo[]
  onClose: () => void
  isFavorite?: boolean
  onToggleFavorite?: (song: Song) => void
  onNextSong?: () => void
  onPrevSong?: () => void
  queue?: Song[]
  onSelectQueueItem?: (song: Song) => void
  userId?: string
  onTransferPlayback?: (song: Song) => void
  onRemoveQueueItem?: (index: number) => void
  onReorderQueue?: (newQueue: Song[]) => void
  onClearQueue?: () => void
}

function formatTime(sec: number): string {
  if (isNaN(sec) || sec < 0) return '0:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${s < 10 ? '0' : ''}${s}`
}

export const WebPlayer: React.FC<WebPlayerProps> = ({
  song,
  remoteState = null,
  connectedDevices = [],
  onClose: _onClose,
  isFavorite = false,
  onToggleFavorite,
  onNextSong,
  onPrevSong,
  queue = [],
  onSelectQueueItem,
  userId = 'kongujeeva523@gmail.com',
  onTransferPlayback,
  onRemoveQueueItem,
  onReorderQueue,
  onClearQueue
}) => {
  const [isExpanded, setIsExpanded] = useState(false)
  const [isPlaying, setIsPlaying] = useState(true)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [isShuffle, setIsShuffle] = useState(false)
  const [isRepeat, setIsRepeat] = useState(false)
  const [volume, setVolume] = useState(0.8)
  const [isMuted, setIsMuted] = useState(false)
  const [showQueuePanel, setShowQueuePanel] = useState(false)
  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false)

  // Drag and drop state for queue reordering
  const [draggedIdx, setDraggedIdx] = useState<number | null>(null)
  const [dragOverIdx, setDragOverIdx] = useState<number | null>(null)
  const isDraggingRef = useRef<boolean>(false)

  const myDeviceId = IsaiConnectService.getMyDeviceId()
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const pendingSeekTimeRef = useRef<number | null>(null)
  const lastSyncTimeRef = useRef<number>(0)

  // Meaningful listening tracking refs
  const listenSecondsRef = useRef<number>(0)
  const recordedMeaningfulRef = useRef<boolean>(false)
  const activeSongRef = useRef<Song | null>(song)
  const lastTimeRef = useRef<number>(0)

  // Remote active determination (Spotify Connect mode)
  // Active when another device is set as the active player in Firebase playbackState
  const isRemoteActive = Boolean(
    remoteState &&
    remoteState.currentDeviceId &&
    remoteState.currentDeviceId !== myDeviceId &&
    remoteState.currentTitle &&
    (Date.now() - (remoteState.updatedAt || 0) < 15 * 60 * 1000)
  )

  // Pre-resolve audioUrl for remote track so it is instantly playable on transfer
  const [cachedRemoteAudioUrl, setCachedRemoteAudioUrl] = useState<string>('')
  useEffect(() => {
    if (!isRemoteActive || !remoteState?.currentTitle) return
    if (remoteState.currentAudioUrl) {
      setCachedRemoteAudioUrl(remoteState.currentAudioUrl)
      return
    }

    let isMounted = true
    const resolveRemoteAudio = async () => {
      try {
        const cleanQuery = cleanHtmlTitle(remoteState.currentTitle || '')
          .replace(/\s*[\|\-\–\—].*$/, '')
          .replace(/\s*\(.*?(official|video|audio|lyrics|hd|4k|song).*?\)/gi, '')
          .replace(/\s*\[.*?(official|video|audio|lyrics|hd|4k|song).*?\]/gi, '')
          .replace(/\.{2,}$/, '')
          .trim()

        const results = await musicApi.searchSongs(cleanQuery || remoteState.currentTitle)
        if (isMounted && results && results.length > 0 && results[0].audioUrl) {
          setCachedRemoteAudioUrl(results[0].audioUrl)
          IsaiConnectService.updatePlaybackState({ currentAudioUrl: results[0].audioUrl })
        }
      } catch (err) {
        console.warn('[WebPlayer] Pre-fetch remote audio error:', err)
      }
    }
    resolveRemoteAudio()
    return () => { isMounted = false }
  }, [isRemoteActive, remoteState?.currentSongId, remoteState?.currentTitle, remoteState?.currentAudioUrl])

  const remoteAsSong: Song | null = (remoteState && remoteState.currentTitle) ? {
    videoId: remoteState.currentSongId || `transferred_${Date.now()}`,
    title: remoteState.currentTitle,
    channelTitle: remoteState.currentArtist || 'Artist',
    thumbnailUrl: remoteState.currentArtwork || '',
    audioUrl: remoteState.currentAudioUrl || cachedRemoteAudioUrl || '',
    durationFormatted: formatTime((remoteState.durationMs || 210000) / 1000),
    durationMs: remoteState.durationMs || 210000,
    viewCountFormatted: ''
  } : null

  const displaySong: Song | null = isRemoteActive ? (remoteAsSong || song) : (song || remoteAsSong)

  const activeTitle = isRemoteActive ? (remoteState?.currentTitle || displaySong?.title) : displaySong?.title
  const activeArtist = isRemoteActive ? (remoteState?.currentArtist || displaySong?.channelTitle || 'Artist') : displaySong?.channelTitle
  const activeArtwork = isRemoteActive ? (remoteState?.currentArtwork || displaySong?.thumbnailUrl) : displaySong?.thumbnailUrl
  const activeDuration = isRemoteActive ? ((remoteState?.durationMs || 0) / 1000 || 210) : (duration || (displaySong?.durationMs ? displaySong.durationMs / 1000 : 210))

  const effectiveQueue: Song[] = React.useMemo(() => {
    if (isRemoteActive && remoteState?.queue) {
      const raw = Array.isArray(remoteState.queue)
        ? remoteState.queue
        : (typeof remoteState.queue === 'object' && remoteState.queue !== null ? Object.values(remoteState.queue) : [])
      return (raw || [])
        .filter((q: any) => Boolean(q && typeof q === 'object' && (q.id || q.videoId)))
        .map((q: any) => ({
          videoId: q.id || q.videoId || `sync_${Date.now()}`,
          title: q.title || 'Unknown Title',
          channelTitle: q.artist || q.channelTitle || 'ISAI Artist',
          thumbnailUrl: q.artwork || q.thumbnailUrl || '',
          audioUrl: q.audioUrl || '',
          durationFormatted: q.duration || '3:30',
          durationMs: 210000,
          viewCountFormatted: ''
        }))
    }
    const safeQueue: any[] = Array.isArray(queue)
      ? queue
      : (queue && typeof queue === 'object' && queue !== null ? Object.values(queue) : [])
    return (safeQueue || [])
      .filter((s: any) => Boolean(s && typeof s === 'object' && (s.videoId || s.id))) as Song[]
  }, [isRemoteActive, remoteState?.queue, queue])

  // Real-time seconds ticking when playing remotely on mobile
  const [remoteCurrentTime, setRemoteCurrentTime] = useState(0)
  useEffect(() => {
    if (!isRemoteActive || !remoteState) return

    const basePosition = (remoteState.positionMs || 0) / 1000
    const updatedAt = remoteState.updatedAt || Date.now()
    const maxDur = (remoteState.durationMs || 0) / 1000 || 210

    if (!remoteState.isPlaying) {
      setRemoteCurrentTime(Math.min(basePosition, maxDur))
      return
    }

    const updateTimer = () => {
      const elapsed = Math.max(0, (Date.now() - updatedAt) / 1000)
      const current = Math.min(basePosition + elapsed, maxDur)
      setRemoteCurrentTime(current)
    }

    updateTimer()
    const interval = setInterval(updateTimer, 500)
    return () => clearInterval(interval)
  }, [isRemoteActive, remoteState?.positionMs, remoteState?.updatedAt, remoteState?.isPlaying, remoteState?.durationMs])

  const activePosition = isRemoteActive ? remoteCurrentTime : currentTime
  const activeIsPlaying = isRemoteActive ? Boolean(remoteState?.isPlaying) : isPlaying

  const playingDevice = connectedDevices.find(d => d.deviceId === remoteState?.currentDeviceId)
  const remoteDeviceName = playingDevice?.deviceName || (remoteState?.currentDeviceId?.includes('android') ? "Jeeva's Phone" : "Mobile Device")

  // Activate local playback on Web when user transfers or remote device hands off
  const activateLocalPlayback = async (targetSong?: any, positionMs?: number) => {
    const seekSec = (positionMs != null ? positionMs : (remoteState?.positionMs || 0)) / 1000
    const rawTitle = targetSong?.title || remoteState?.currentTitle || song?.title || ''
    const rawArtist = targetSong?.channelTitle || targetSong?.artist || remoteState?.currentArtist || song?.channelTitle || 'Artist'
    const rawArtwork = targetSong?.thumbnailUrl || targetSong?.artwork || remoteState?.currentArtwork || song?.thumbnailUrl || ''
    const rawId = targetSong?.videoId || targetSong?.id || remoteState?.currentSongId || song?.videoId || `transferred_${Date.now()}`
    let targetAudio = targetSong?.audioUrl || cachedRemoteAudioUrl || remoteState?.currentAudioUrl || ''

    if (seekSec > 0) {
      pendingSeekTimeRef.current = seekSec
      setCurrentTime(seekSec)
    }

    const transferred: Song = {
      videoId: rawId,
      title: rawTitle,
      channelTitle: rawArtist,
      thumbnailUrl: rawArtwork,
      audioUrl: targetAudio,
      durationFormatted: formatTime((remoteState?.durationMs || 210000) / 1000),
      durationMs: remoteState?.durationMs || 210000,
      viewCountFormatted: ''
    }

    setIsPlaying(true)

    // Directly assign audio src and trigger play immediately
    if (audioRef.current && targetAudio) {
      if (audioRef.current.src !== targetAudio) {
        audioRef.current.src = targetAudio
      }
      if (seekSec > 0) audioRef.current.currentTime = seekSec
      audioRef.current.play().catch(e => {
        console.warn('[WebPlayer] Transfer play catch (may need user gesture):', e)
        const onFirstInteraction = () => {
          audioRef.current?.play().catch(() => {})
          window.removeEventListener('click', onFirstInteraction)
          window.removeEventListener('keydown', onFirstInteraction)
        }
        window.addEventListener('click', onFirstInteraction, { once: true })
        window.addEventListener('keydown', onFirstInteraction, { once: true })
      })
    }

    IsaiConnectService.transferPlaybackToDevice(myDeviceId, transferred, (positionMs != null ? positionMs : (remoteState?.positionMs || 0)))
    onTransferPlayback?.(transferred)

    // If audio stream is not yet cached, resolve 320kbps stream via JioSaavn search
    if (!targetAudio && rawTitle) {
      try {
        const cleanTitle = cleanHtmlTitle(rawTitle)
          .replace(/\s*[\|\-\–\—].*$/, '')
          .replace(/\s*\(.*?(official|video|audio|lyrics|hd|4k|song).*?\)/gi, '')
          .replace(/\s*\[.*?(official|video|audio|lyrics|hd|4k|song).*?\]/gi, '')
          .replace(/\.{2,}$/, '')
          .trim()

        const results = await musicApi.searchSongs(cleanTitle || rawTitle)
        if (results && results.length > 0 && results[0].audioUrl) {
          const resolvedUrl = results[0].audioUrl
          setCachedRemoteAudioUrl(resolvedUrl)
          targetAudio = resolvedUrl
          if (audioRef.current) {
            audioRef.current.src = resolvedUrl
            if (seekSec > 0) audioRef.current.currentTime = seekSec
            audioRef.current.play().catch(e => {
              console.warn('[WebPlayer] Resolved async play catch:', e)
              const onFirstInteraction = () => {
                audioRef.current?.play().catch(() => {})
                window.removeEventListener('click', onFirstInteraction)
                window.removeEventListener('keydown', onFirstInteraction)
              }
              window.addEventListener('click', onFirstInteraction, { once: true })
              window.addEventListener('keydown', onFirstInteraction, { once: true })
            })
          }
          const updated = { ...transferred, audioUrl: resolvedUrl }
          onTransferPlayback?.(updated)
          IsaiConnectService.updatePlaybackState({ currentAudioUrl: resolvedUrl })
        }
      } catch (e) {
        console.warn('[WebPlayer] Failed to resolve audio for transferred song:', e)
      }
    }
  }

  const activateLocalPlaybackRef = useRef(activateLocalPlayback)
  activateLocalPlaybackRef.current = activateLocalPlayback

  // Initialize ISAI Connect service
  useEffect(() => {
    IsaiConnectService.initialize(userId)
    const unsub = IsaiConnectService.subscribePlaybackState((syncState) => {
      if (syncState && syncState.currentDeviceId) {
        if (syncState.currentDeviceId !== myDeviceId && syncState.updatedByDeviceId !== myDeviceId) {
          if (audioRef.current) audioRef.current.pause()
          setIsPlaying(false)
        } else if (syncState.currentDeviceId === myDeviceId && syncState.updatedByDeviceId !== myDeviceId) {
          if (syncState.volume != null) {
            const clamped = Math.max(0, Math.min(1, syncState.volume))
            setVolume(clamped)
            setIsMuted(clamped === 0)
            if (audioRef.current) audioRef.current.volume = clamped
          }

          // If playback was handed off to this Web device by another device (e.g. Android App):
          if (syncState.currentTitle && (!activeSongRef.current || activeSongRef.current.title !== syncState.currentTitle || !isPlaying)) {
            console.log('[WebPlayer] Playback handed off to Web in syncState:', syncState)
            activateLocalPlaybackRef.current({
              id: syncState.currentSongId,
              title: syncState.currentTitle,
              artist: syncState.currentArtist,
              artwork: syncState.currentArtwork,
              audioUrl: syncState.currentAudioUrl
            }, syncState.positionMs)
          }
        }
      }
    })
    return () => unsub()
  }, [userId, myDeviceId, isPlaying])

  // Listen to remote commands sent from Android or another device
  useEffect(() => {
    const unsubCmd = IsaiConnectService.subscribeCommands((cmd) => {
      if (cmd.issuedByDeviceId === myDeviceId) return
      if (cmd.targetDeviceId && cmd.targetDeviceId !== myDeviceId) return
      console.log('[WebPlayer] Incoming remote command from other device:', cmd.action, cmd)

      if (cmd.action === 'PAUSE') {
        if (audioRef.current) audioRef.current.pause()
        setIsPlaying(false)
        IsaiConnectService.updatePlaybackState({ isPlaying: false })
      } else if (cmd.action === 'PLAY') {
        if (audioRef.current) {
          const activeAudioUrl = song?.audioUrl || displaySong?.audioUrl
          if (!audioRef.current.src && activeAudioUrl) {
            audioRef.current.src = activeAudioUrl
          }
          audioRef.current.play().catch(err => console.warn('[WebPlayer] Remote PLAY catch:', err))
        }
        setIsPlaying(true)
        IsaiConnectService.updatePlaybackState({ isPlaying: true })
      } else if (cmd.action === 'NEXT') {
        onNextSong?.()
      } else if (cmd.action === 'PREV') {
        onPrevSong?.()
      } else if (cmd.action === 'SEEK' && cmd.positionMs != null) {
        const sec = cmd.positionMs / 1000
        setCurrentTime(sec)
      } else if (cmd.action === 'SET_VOLUME') {
        const rawVol = cmd.volume != null ? cmd.volume : (cmd.positionMs != null ? cmd.positionMs / 100 : 1)
        const clamped = Math.max(0, Math.min(1, rawVol))
        console.log('[WebPlayer] Setting remote volume to:', clamped)
        setVolume(clamped)
        setIsMuted(clamped === 0)
      } else if (cmd.action === 'PLAY_SONG') {
        console.log('[WebPlayer] Remote command PLAY_SONG received:', cmd)
        const incomingSong = cmd.song || (cmd.songId ? {
          videoId: cmd.songId,
          title: cmd.songTitle,
          channelTitle: cmd.songArtist,
          thumbnailUrl: cmd.songArtwork,
          audioUrl: cmd.songAudioUrl
        } : null)
        activateLocalPlaybackRef.current(incomingSong, cmd.positionMs)
      }
    })
    return () => unsubCmd()
  }, [myDeviceId, onNextSong, onPrevSong])

  // Sync song changes to ISAI Connect (when Web is playing locally)
  useEffect(() => {
    if (!song || isRemoteActive) return
    if (remoteState?.currentDeviceId && remoteState.currentDeviceId !== myDeviceId) return

    IsaiConnectService.updatePlaybackState({
      currentDeviceId: myDeviceId,
      currentSongId: song.videoId,
      currentTitle: song.title,
      currentArtist: song.channelTitle || 'ISAI Artist',
      currentArtwork: song.thumbnailUrl,
      currentAudioUrl: song.audioUrl,
      isPlaying: isPlaying,
      durationMs: (duration || 211) * 1000,
      positionMs: currentTime * 1000,
      queue: (Array.isArray(queue) ? queue : []).filter(Boolean).map(q => ({
        id: q.videoId || (q as any).id || '',
        title: q.title || '',
        artist: q.channelTitle || (q as any).artist || '',
        artwork: q.thumbnailUrl || (q as any).artwork || '',
        audioUrl: (q as any).audioUrl || ''
      }))
    })
  }, [song?.videoId, song?.title, song?.thumbnailUrl, song?.audioUrl, isPlaying, isRemoteActive, remoteState?.currentDeviceId, myDeviceId])

  // Immediate auto-play and recommendation threshold session management when a new song is selected
  useEffect(() => {
    if (isRemoteActive) {
      // Remote device (Phone) is playing! Do not start local audio on Web
      return
    }

    // If previous song was skipped (<10s) and not marked meaningful:
    const prevSong = activeSongRef.current
    if (prevSong && prevSong.videoId !== song?.videoId) {
      if (!recordedMeaningfulRef.current && listenSecondsRef.current > 0 && listenSecondsRef.current < 10) {
        recommendationService.recordListen(userId, prevSong, listenSecondsRef.current, duration)
      }
    }

    activeSongRef.current = song
    listenSecondsRef.current = 0
    recordedMeaningfulRef.current = false
    lastTimeRef.current = 0

    const activeAudioUrl = displaySong?.audioUrl || (!isRemoteActive ? remoteState?.currentAudioUrl : undefined)
    if (activeAudioUrl && !isRemoteActive) {
      setIsPlaying(true)
      const seekTarget = (pendingSeekTimeRef.current !== null && pendingSeekTimeRef.current > 0) ? pendingSeekTimeRef.current : 0
      pendingSeekTimeRef.current = null
      setCurrentTime(seekTarget)
      if (audioRef.current) {
        if (audioRef.current.src !== activeAudioUrl) {
          audioRef.current.src = activeAudioUrl
          audioRef.current.currentTime = seekTarget
        }
        audioRef.current.play().catch(err => {
          console.warn('[WebPlayer] Autoplay catch:', err)
        })
      }
    }
  }, [displaySong?.videoId, displaySong?.audioUrl, isRemoteActive])

  // Control audio element play/pause
  useEffect(() => {
    if (isRemoteActive) {
      if (audioRef.current) audioRef.current.pause()
      return
    }

    const activeAudioUrl = displaySong?.audioUrl || remoteState?.currentAudioUrl
    if (audioRef.current && activeAudioUrl) {
      audioRef.current.volume = isMuted ? 0 : volume
      if (isPlaying) {
        audioRef.current.play().catch(err => {
          console.warn('[WebPlayer] Playback play warning:', err)
        })
      } else {
        audioRef.current.pause()
      }
    }
  }, [displaySong?.videoId, displaySong?.audioUrl, isPlaying, volume, isMuted, isRemoteActive, remoteState?.currentAudioUrl])

  const togglePlay = () => {
    if (isRemoteActive) {
      const nextAction = remoteState?.isPlaying ? 'PAUSE' : 'PLAY'
      const targetDev = remoteState?.currentDeviceId || ''
      IsaiConnectService.sendCommand(nextAction, { targetDeviceId: targetDev })
      IsaiConnectService.updatePlaybackState({ isPlaying: !remoteState?.isPlaying })
    } else {
      if (audioRef.current) {
        if (isPlaying) {
          audioRef.current.pause()
          setIsPlaying(false)
        } else {
          const activeAudioUrl = displaySong?.audioUrl || remoteState?.currentAudioUrl
          if (activeAudioUrl && !audioRef.current.src) {
            audioRef.current.src = activeAudioUrl
          }
          audioRef.current.play().then(() => setIsPlaying(true)).catch(e => console.warn(e))
          setIsPlaying(true)
        }
      } else {
        setIsPlaying(prev => !prev)
      }
    }
  }

  const handleNext = () => {
    if (isRemoteActive) {
      IsaiConnectService.sendCommand('NEXT', { targetDeviceId: remoteState?.currentDeviceId })
    } else if (onNextSong) {
      onNextSong()
    }
  }

  const handlePrev = () => {
    if (isRemoteActive) {
      IsaiConnectService.sendCommand('PREV', { targetDeviceId: remoteState?.currentDeviceId })
    } else if (onPrevSong) {
      onPrevSong()
    }
  }

  const handleTransferToWeb = async () => {
    const previousDeviceId = remoteState?.currentDeviceId || ''
    if (previousDeviceId && previousDeviceId !== myDeviceId) {
      IsaiConnectService.sendCommand('PAUSE', { targetDeviceId: previousDeviceId })
    }
    await activateLocalPlayback()
  }

  const handleTransferToRemote = (_targetDeviceId: string) => {
    if (audioRef.current) {
      audioRef.current.pause()
    }
    setIsPlaying(false)
  }

  const handleLoadedMetadata = () => {
    if (audioRef.current) {
      const dur = audioRef.current.duration || 0
      setDuration(dur)
      if (pendingSeekTimeRef.current !== null && pendingSeekTimeRef.current > 0) {
        const seek = pendingSeekTimeRef.current
        pendingSeekTimeRef.current = null
        audioRef.current.currentTime = seek
        setCurrentTime(seek)
        audioRef.current.play().catch(() => {})
        setIsPlaying(true)
      }
    }
  }

  const handleTimeUpdate = () => {
    if (audioRef.current) {
      const cur = audioRef.current.currentTime
      const dur = audioRef.current.duration || 0
      setCurrentTime(cur)
      setDuration(dur)

      // Increment continuous listening seconds
      if (isPlaying && !isRemoteActive && song) {
        const delta = lastTimeRef.current > 0 ? (cur - lastTimeRef.current) : 0
        if (delta > 0 && delta < 2.0) {
          listenSecondsRef.current += delta
        }
        lastTimeRef.current = cur

        const played = listenSecondsRef.current
        if (!recordedMeaningfulRef.current && (played >= 30 || (dur > 0 && (played / dur) >= 0.5))) {
          recordedMeaningfulRef.current = true
          recommendationService.recordListen(userId, song, played, dur)
        }
      }

      // Sync position to Firebase every 1.5 seconds so remote device has accurate timestamp
      const now = Date.now()
      if (!isRemoteActive && song && now - lastSyncTimeRef.current > 1500) {
        lastSyncTimeRef.current = now
        IsaiConnectService.updatePlaybackState({
          currentDeviceId: myDeviceId,
          positionMs: Math.round(cur * 1000),
          durationMs: Math.round(dur * 1000),
          isPlaying: true
        })
      }
    }
  }

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value)
    if (isRemoteActive) {
      setRemoteCurrentTime(val)
      IsaiConnectService.sendCommand('SEEK', { positionMs: Math.round(val * 1000) })
      IsaiConnectService.updatePlaybackState({ positionMs: Math.round(val * 1000) })
    } else {
      setCurrentTime(val)
      if (audioRef.current) {
        audioRef.current.currentTime = val
      }
    }
  }

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value)
    setVolume(val)
    if (val === 0) setIsMuted(true)
    else setIsMuted(false)
    if (audioRef.current) audioRef.current.volume = val
    if (!isRemoteActive) {
      IsaiConnectService.updatePlaybackState({ volume: val })
    } else {
      IsaiConnectService.sendCommand('SET_VOLUME', { volume: val })
    }
  }

  const handleAudioEnded = () => {
    if (song && !recordedMeaningfulRef.current) {
      recordedMeaningfulRef.current = true
      recommendationService.recordListen(userId, song, duration || 210, duration || 210)
    }
    if (isRepeat) {
      if (audioRef.current) {
        audioRef.current.currentTime = 0
        audioRef.current.play()
      }
    } else if (onNextSong) {
      onNextSong()
    } else {
      setIsPlaying(false)
    }
  }

  if (!displaySong && !activeTitle) {
    return null
  }

  return (
    <>
      <audio
        ref={audioRef}
        src={displaySong?.audioUrl || song?.audioUrl || cachedRemoteAudioUrl || undefined}
        autoPlay
        playsInline
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onCanPlay={handleLoadedMetadata}
        onEnded={handleAudioEnded}
        onError={(e) => {
          console.warn('[WebPlayer] Audio error on stream, skipping to next track:', e)
          onNextSong?.()
        }}
      />



      {/* 1. Persistent Bottom Player Bar */}
      <div className="web-player-bar">
        {/* Left Track Info */}
        <div className="player-left-info" onClick={() => setIsExpanded(true)} style={{ cursor: 'pointer' }}>
          {activeArtwork ? (
            <img src={activeArtwork} alt={activeTitle} className="player-artwork" />
          ) : (
            <div className="player-artwork" style={{ background: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>🎵</div>
          )}
          <div className="player-track-details">
            <span className="player-title">{activeTitle || 'ISAI Track'}</span>
            <span className="player-artist">{activeArtist || 'ISAI Artist'}</span>
          </div>

          {!isRemoteActive && song && onToggleFavorite && (
            <button
              className={`control-btn like-btn ${isFavorite ? 'liked active' : ''}`}
              onClick={(e) => {
                e.stopPropagation()
                onToggleFavorite(song)
              }}
              style={{ color: isFavorite ? '#EC4899' : 'var(--text-muted)', marginLeft: '8px' }}
              title={isFavorite ? 'Liked' : 'Like'}
            >
              <Heart
                size={18}
                color={isFavorite ? '#EC4899' : 'currentColor'}
                fill={isFavorite ? '#EC4899' : 'none'}
                style={{
                  filter: isFavorite ? 'drop-shadow(0 0 6px rgba(236, 72, 153, 0.75))' : 'none',
                  transition: 'all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275)'
                }}
              />
            </button>
          )}
        </div>

        {/* Center Controls & Progress Bar */}
        <div className="player-center-controls">
          <div className="control-buttons-row">
            <button
              className={`control-btn ${isShuffle ? 'active' : ''}`}
              onClick={() => setIsShuffle(!isShuffle)}
              title="Shuffle"
            >
              <Shuffle size={18} />
            </button>

            <button className="control-btn" onClick={handlePrev} title="Previous track">
              <SkipBack size={20} />
            </button>

            <button className="play-pause-circle" onClick={togglePlay} title={activeIsPlaying ? 'Pause' : 'Play'}>
              {activeIsPlaying ? <Pause size={22} fill="#fff" /> : <Play size={22} fill="#fff" style={{ marginLeft: '3px' }} />}
            </button>

            <button className="control-btn" onClick={handleNext} title="Next track">
              <SkipForward size={20} />
            </button>

            <button
              className={`control-btn ${isRepeat ? 'active' : ''}`}
              onClick={() => setIsRepeat(!isRepeat)}
              title="Repeat"
            >
              <Repeat size={18} />
            </button>
          </div>

          <div className="progress-bar-wrap">
            <span className="time-stamp">{formatTime(activePosition)}</span>
            <input
              type="range"
              min={0}
              max={activeDuration || 100}
              value={activePosition}
              onChange={handleSeek}
              className="custom-range-slider"
            />
            <span className="time-stamp">{formatTime(activeDuration)}</span>
          </div>
        </div>

        {/* Right Action Icons */}
        <div className="player-right-actions">
          <button
            className={`control-btn ${showQueuePanel ? 'active' : ''}`}
            onClick={() => {
              setIsExpanded(true)
              setShowQueuePanel(true)
            }}
            title="Queue"
          >
            <ListMusic size={18} />
          </button>

          {isRemoteActive ? (
            <button
              onClick={(e) => {
                e.stopPropagation()
                handleTransferToWeb()
              }}
              style={{
                padding: '6px 14px',
                borderRadius: '20px',
                background: 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 100%)',
                color: '#fff',
                border: 'none',
                fontSize: '12px',
                fontWeight: 800,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                boxShadow: '0 4px 14px rgba(139, 92, 246, 0.4)',
                whiteSpace: 'nowrap',
                marginRight: '6px'
              }}
              title="Switch audio playback to this web browser"
            >
              <Laptop size={14} />
              <span>Switch to Web</span>
            </button>
          ) : null}

          <button
            className="control-btn"
            onClick={() => setIsConnectModalOpen(true)}
            title="ISAI Connect (Device Picker)"
            style={{ position: 'relative' }}
          >
            <Laptop size={18} color={isRemoteActive ? '#1DB954' : 'var(--isai-purple-light)'} />
            {isRemoteActive && (
              <span style={{
                position: 'absolute',
                top: '4px',
                right: '4px',
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                backgroundColor: '#1DB954',
                boxShadow: '0 0 8px #1DB954'
              }} />
            )}
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <button
              className="control-btn"
              onClick={() => setIsMuted(!isMuted)}
              title={isMuted ? 'Unmute' : 'Mute'}
            >
              {isMuted || volume === 0 ? <VolumeX size={18} /> : <Volume2 size={18} />}
            </button>

            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={isMuted ? 0 : volume}
              onChange={handleVolumeChange}
              className="custom-range-slider"
              style={{ width: '70px' }}
            />
          </div>

          <button className="control-btn" onClick={() => setIsExpanded(true)} title="Expand Fullscreen">
            <Maximize2 size={18} />
          </button>
        </div>
      </div>

      {/* 2. Expanded Full-screen View */}
      {isExpanded && (
        <div className="expanded-player-overlay">
          <div className="expanded-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span style={{ fontSize: '11px', fontWeight: 900, color: 'var(--isai-purple-light)', letterSpacing: '0.12em' }}>
                {isRemoteActive ? `PLAYING ON ${remoteDeviceName.toUpperCase()}` : 'PLAYING FROM ISAI'}
              </span>
              {isRemoteActive ? (
                <button
                  onClick={handleTransferToWeb}
                  style={{
                    padding: '4px 12px',
                    borderRadius: '16px',
                    background: 'linear-gradient(135deg, #8B5CF6 0%, #06B6D4 100%)',
                    color: '#fff',
                    border: 'none',
                    fontSize: '11px',
                    fontWeight: 800,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '5px',
                    boxShadow: '0 2px 10px rgba(139, 92, 246, 0.35)'
                  }}
                  title="Switch audio playback to this web browser"
                >
                  <Laptop size={12} />
                  <span>Switch to Web</span>
                </button>
              ) : null}
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                className={`pill-button ${showQueuePanel ? 'active' : ''}`}
                onClick={() => setShowQueuePanel(!showQueuePanel)}
              >
                Queue ({effectiveQueue.length})
              </button>
              <button className="control-btn" onClick={() => setIsExpanded(false)}>
                <Minimize2 size={24} />
              </button>
            </div>
          </div>

          <div className="expanded-content-grid">
            {/* Left/Main Artwork View */}
            <div style={{ textAlign: 'center' }}>
              {activeArtwork ? (
                <img src={activeArtwork} alt={activeTitle} className="expanded-artwork-box" />
              ) : (
                <div className="expanded-artwork-box" style={{ background: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>🎵</div>
              )}
              <h2 style={{ fontSize: '28px', fontWeight: 900, marginTop: '24px', color: '#fff' }}>
                {activeTitle}
              </h2>
              <p style={{ fontSize: '16px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                {activeArtist}
              </p>
            </div>

            {/* Right Panel (Queue or Song Details) */}
            {showQueuePanel ? (
              <div className="expanded-lyrics-panel">
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                  <div>
                    <h3 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--isai-purple-light)', margin: 0 }}>
                      Up Next Queue ({effectiveQueue.length})
                    </h3>
                    <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '4px 0 0 0' }}>
                      Drag songs to reorder • Tap to play
                    </p>
                  </div>
                  {effectiveQueue.length > 1 && (
                    <button
                      onClick={() => onClearQueue?.()}
                      style={{
                        background: 'rgba(239, 68, 68, 0.12)',
                        border: '1px solid rgba(239, 68, 68, 0.3)',
                        color: '#ef4444',
                        borderRadius: '6px',
                        padding: '4px 10px',
                        fontSize: '12px',
                        fontWeight: 700,
                        cursor: 'pointer',
                        transition: 'all 0.2s ease'
                      }}
                      title="Clear upcoming songs"
                    >
                      Clear Queue
                    </button>
                  )}
                </div>

                {effectiveQueue.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)', fontSize: '14px' }}>
                    Queue is empty
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {effectiveQueue.filter(Boolean).map((qSong, idx) => {
                      const songId = qSong?.videoId || (qSong as any)?.id || `q_${idx}`
                      const isCurrent = songId === (isRemoteActive ? (remoteState?.currentSongId || song?.videoId) : (song?.videoId || remoteState?.currentSongId))
                      const isDragging = draggedIdx === idx
                      const isDragOver = dragOverIdx === idx

                      return (
                        <div
                          key={`${songId}_${idx}`}
                          draggable={true}
                          onDragStart={(e) => {
                            isDraggingRef.current = true
                            setDraggedIdx(idx)
                            e.dataTransfer.effectAllowed = 'move'
                            e.dataTransfer.setData('text/plain', `${idx}`)
                          }}
                          onDragOver={(e) => {
                            e.preventDefault()
                            e.dataTransfer.dropEffect = 'move'
                            if (dragOverIdx !== idx) setDragOverIdx(idx)
                          }}
                          onDragEnter={(e) => {
                            e.preventDefault()
                            if (dragOverIdx !== idx) setDragOverIdx(idx)
                          }}
                          onDragEnd={() => {
                            setDraggedIdx(null)
                            setDragOverIdx(null)
                            setTimeout(() => {
                              isDraggingRef.current = false
                            }, 150)
                          }}
                          onDrop={(e) => {
                            e.preventDefault()
                            e.stopPropagation()
                            const sourceIdx = draggedIdx !== null ? draggedIdx : parseInt(e.dataTransfer.getData('text/plain'), 10)
                            if (isNaN(sourceIdx) || sourceIdx === idx) {
                              setDraggedIdx(null)
                              setDragOverIdx(null)
                              setTimeout(() => { isDraggingRef.current = false }, 150)
                              return
                            }
                            const nextQueue = [...effectiveQueue]
                            const [movedSong] = nextQueue.splice(sourceIdx, 1)
                            nextQueue.splice(idx, 0, movedSong)
                            if (isRemoteActive) {
                              IsaiConnectService.updatePlaybackState({
                                queue: nextQueue.map(item => ({
                                  id: item.videoId || (item as any).id || '',
                                  title: item.title || '',
                                  artist: item.channelTitle || (item as any).artist || '',
                                  artwork: item.thumbnailUrl || (item as any).artwork || ''
                                }))
                              })
                            }
                            onReorderQueue?.(nextQueue)
                            setDraggedIdx(null)
                            setDragOverIdx(null)
                            setTimeout(() => {
                              isDraggingRef.current = false
                            }, 150)
                          }}
                          onClick={() => {
                            if (isDraggingRef.current) return
                            onSelectQueueItem?.(qSong)
                          }}
                          className={`queue-row-item ${isCurrent ? 'active' : ''} ${isDragging ? 'dragging' : ''} ${isDragOver ? 'drag-over' : ''}`}
                        >
                          {/* Drag Handle */}
                          <div
                            className="queue-drag-handle"
                            title="Drag to reorder"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <GripVertical size={16} />
                          </div>

                          {/* Index / Playing Equalizer Indicator */}
                          <div style={{ width: '22px', textAlign: 'center', fontSize: '12px', fontWeight: 700, color: isCurrent ? 'var(--isai-pink)' : 'var(--text-muted)' }}>
                            {isCurrent ? '▶' : `${idx + 1}`}
                          </div>

                          {/* Song Thumbnail */}
                          <img
                            src={qSong.thumbnailUrl || ''}
                            alt={qSong.title || 'Track'}
                            style={{ width: '42px', height: '42px', borderRadius: '6px', objectFit: 'cover', flexShrink: 0 }}
                          />

                          {/* Song Details */}
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <div
                              className="ytm-pl-title"
                              style={{
                                color: isCurrent ? 'var(--isai-pink)' : 'var(--text-primary)',
                                maxWidth: '100%',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap'
                              }}
                            >
                              {qSong.title || 'Unknown Track'}
                            </div>
                            <div
                              className="ytm-pl-sub"
                              style={{
                                maxWidth: '100%',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap'
                              }}
                            >
                              {qSong.channelTitle || 'ISAI Artist'}
                            </div>
                          </div>

                          {/* Playing Badge or Delete Button */}
                          {isCurrent ? (
                            <span style={{
                              fontSize: '10px',
                              fontWeight: 800,
                              color: 'var(--isai-purple-light)',
                              background: 'rgba(139, 92, 246, 0.2)',
                              padding: '3px 8px',
                              borderRadius: '6px',
                              letterSpacing: '0.05em'
                            }}>
                              PLAYING
                            </span>
                          ) : (
                            <button
                              className="queue-delete-btn"
                              onClick={(e) => {
                                e.stopPropagation()
                                onRemoveQueueItem?.(idx)
                              }}
                              title="Remove from queue"
                            >
                              <Trash2 size={16} />
                            </button>
                          )}
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            ) : (
              <div style={{ padding: '32px', background: 'var(--surface-card)', borderRadius: 'var(--radius-xl)', border: '1px solid var(--border-subtle)' }}>
                <h3 style={{ fontSize: '18px', fontWeight: 800, marginBottom: '12px', color: 'var(--isai-purple-light)' }}>
                  Song Details
                </h3>
                <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '8px' }}>
                  <strong>Audio Format:</strong> 320kbps AAC High Fidelity
                </p>
                <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '8px' }}>
                  <strong>Artist:</strong> {activeArtist}
                </p>
                <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
                  <strong>Duration:</strong> {formatTime(activeDuration)}
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Isai Connect Modal */}
      <IsaiConnectModal
        isOpen={isConnectModalOpen}
        onClose={() => setIsConnectModalOpen(false)}
        currentSong={song}
        currentTime={audioRef.current ? audioRef.current.currentTime : currentTime}
        onTransferToLocal={handleTransferToWeb}
        onTransferToRemote={handleTransferToRemote}
      />
    </>
  )
}
