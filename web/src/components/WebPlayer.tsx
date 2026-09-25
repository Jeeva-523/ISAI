import { detectSongLanguage, musicApi } from '@shared/api/music-api'
import { storageService } from '@shared/services/storageService'
import { cleanHtmlTitle, isSameSongOrDuplicate } from '@shared/utils/formatters'
import {
  FileText,
  GripVertical,
  Heart,
  History,
  Laptop,
  ListMusic,
  Maximize2,
  Minimize2,
  Pause,
  Play,
  Plus,
  Radio,
  Repeat,
  Share2,
  Shuffle,
  SkipBack,
  SkipForward,
  Sparkles,
  Trash2,
  Volume2,
  VolumeX
} from 'lucide-react'
import React, { useEffect, useRef, useState } from 'react'
import { IsaiConnectService, type DeviceInfo, type PlaybackStateSync } from '../services/IsaiConnectService'
import { ListenTogetherService, type RoomData } from '../services/ListenTogetherService'
import { recommendationService } from '../services/RecommendationService'
import { IsaiConnectModal } from './IsaiConnectModal'
import { ListenTogetherModal } from './ListenTogetherModal'
import type { Song } from '@shared/models/song'

interface WebPlayerProps {
  song: Song | null
  remoteState?: PlaybackStateSync | null
  connectedDevices?: DeviceInfo[]
  onClose?: () => void
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
  if (Number.isNaN(sec) || sec < 0) return '0:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${s < 10 ? '0' : ''}${s}`
}

export const WebPlayer: React.FC<WebPlayerProps> = ({
  song,
  remoteState = null,
  connectedDevices = [],
  isFavorite = false,
  onToggleFavorite,
  onNextSong,
  onPrevSong,
  queue = [],
  onSelectQueueItem,
  userId = '',
  onTransferPlayback,
  onRemoveQueueItem,
  onReorderQueue,
  onClearQueue
}) => {
  const lastSessionInitial = useRef(storageService.getLastPlaybackSession()).current
  const initialSeek =
    lastSessionInitial &&
    lastSessionInitial.song?.videoId === song?.videoId &&
    typeof lastSessionInitial.positionSec === 'number'
      ? lastSessionInitial.positionSec
      : null

  const [isExpanded, setIsExpanded] = useState(false)
  const [isPlaying, setIsPlaying] = useState(() => {
    if (
      lastSessionInitial &&
      lastSessionInitial.song?.videoId === song?.videoId &&
      typeof lastSessionInitial.wasPlaying === 'boolean'
    ) {
      return lastSessionInitial.wasPlaying
    }
    return true
  })
  const [currentTime, setCurrentTime] = useState(() => initialSeek || 0)
  const [duration, setDuration] = useState(0)
  const [isShuffle, setIsShuffle] = useState(false)
  const [isRepeat, setIsRepeat] = useState(false)
  const [volume, setVolume] = useState(0.8)
  const [isMuted, setIsMuted] = useState(false)
  const [selectedQueueTab, setSelectedQueueTab] = useState<'up_next' | 'played' | 'all'>('up_next')
  const [sessionPlayedSongs, setSessionPlayedSongs] = useState<Song[]>([])
  const [queueRecommendations, setQueueRecommendations] = useState<Song[]>([])
  const [activeExpandedTab, setActiveExpandedTab] = useState<'queue' | 'lyrics'>('queue')
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const toastTimeoutRef = useRef<any>(null)
  const volumeSyncTimeoutRef = useRef<any>(null)

  const showPlayerToast = (msg: string) => {
    if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current)
    setToastMessage(msg)
    toastTimeoutRef.current = setTimeout(() => setToastMessage(null), 3000)
  }
  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false)
  const [isListenTogetherModalOpen, setIsListenTogetherModalOpen] = useState(false)
  const [listenRoom, setListenRoom] = useState<RoomData | null>(() => ListenTogetherService.getCurrentRoom())
  const [needsAutoplayGesture, setNeedsAutoplayGesture] = useState(false)

  // Drag and drop state for queue reordering
  const [draggedIdx, setDraggedIdx] = useState<number | null>(null)
  const [dragOverIdx, setDragOverIdx] = useState<number | null>(null)
  const isDraggingRef = useRef<boolean>(false)

  const myDeviceId = IsaiConnectService.getMyDeviceId()
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const pendingSeekTimeRef = useRef<number | null>(initialSeek)
  const lastSyncTimeRef = useRef<number>(0)
  const lastSaveSessionTimeRef = useRef<number>(0)

  // Meaningful listening tracking refs
  const listenSecondsRef = useRef<number>(0)
  const recordedMeaningfulRef = useRef<boolean>(false)
  const activeSongRef = useRef<Song | null>(song)
  const lastTimeRef = useRef<number>(0)
  const streamRetryCountRef = useRef<number>(0)

  useEffect(() => {
    streamRetryCountRef.current = 0
  }, [song?.videoId, remoteState?.currentSongId])

  // Track session played history
  useEffect(() => {
    if (song && song.videoId) {
      setSessionPlayedSongs((prev) => {
        if (prev.some((s) => s.videoId === song.videoId)) return prev
        return [song, ...prev.slice(0, 49)]
      })
    }
  }, [song?.videoId])

  // Fetch similar song recommendations for queue autoplay
  useEffect(() => {
    if (!song || !song.videoId) return
    let isCancelled = false
    const query = `${(song.channelTitle || '').split(',')[0].replace(/•.*/, '').trim() || 'Tamil'} hits`
    musicApi
      .searchSongs(query, 6)
      .then((recs: Song[]) => {
        if (!isCancelled && recs && recs.length > 0) {
          setQueueRecommendations(recs.filter((r) => r.videoId !== song.videoId).slice(0, 5))
        }
      })
      .catch(() => {})
    return () => {
      isCancelled = true
    }
  }, [song?.videoId])

  // Native Web Share or clipboard copy fallback
  const handleShareSong = async () => {
    const title = activeTitle
    const artist = activeArtist
    const shareText = `🎵 Listening to "${title}" by ${artist} on ISAI Music!`
    const url = window.location.href
    if (navigator.share) {
      try {
        await navigator.share({ title, text: shareText, url })
        return
      } catch {}
    }
    if (navigator.clipboard) {
      try {
        await navigator.clipboard.writeText(`${shareText}\n${url}`)
        showPlayerToast('Share link copied to clipboard! 📋')
        return
      } catch {}
    }
    showPlayerToast('Share link ready!')
  }

  // Remote active determination (ISAI Connect mode)
  // Active when another device is set as the active player in Firebase playbackState
  const inListenTogetherRoom = Boolean(listenRoom)
  const isSeparateMode = storageService.isMultiDevicePlaybackSeparate()
  const isRemoteOnline = connectedDevices.some(
    (d) => d.deviceId === remoteState?.currentDeviceId && IsaiConnectService.isDeviceOnline(d)
  )
  const isRemoteActive =
    !inListenTogetherRoom &&
    !isSeparateMode &&
    Boolean(
      remoteState &&
      remoteState.isPlaying &&
      remoteState.currentDeviceId &&
      remoteState.currentDeviceId !== myDeviceId &&
      remoteState.currentTitle &&
      Date.now() - (remoteState.updatedAt || 0) < 30000 &&
      isRemoteOnline
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
          .replace(/\s*[|–—-].*$/, '')
          .replaceAll(/\s*\(.*?(official|video|audio|lyrics|hd|4k|song).*?\)/gi, '')
          .replaceAll(/\s*\[.*?(official|video|audio|lyrics|hd|4k|song).*?\]/gi, '')
          .replace(/\.{2,}$/, '')
          .trim()

        const results = await musicApi.searchSongs(cleanQuery || remoteState.currentTitle)
        if (isMounted && results && results.length > 0) {
          const expectedLang = detectSongLanguage({ title: remoteState.currentTitle })
          const matched = results.find((r) => {
            if (!r.audioUrl) return false
            const rLang = (r.language || detectSongLanguage(r)).toLowerCase().trim()
            if (expectedLang && rLang && expectedLang !== rLang) return false
            return isSameSongOrDuplicate(r, { title: remoteState.currentTitle })
          })
          if (matched?.audioUrl) {
            setCachedRemoteAudioUrl(matched.audioUrl)
            IsaiConnectService.updatePlaybackState({ currentAudioUrl: matched.audioUrl })
          }
        }
      } catch (error) {
        console.warn('[WebPlayer] Pre-fetch remote audio error:', error)
      }
    }
    resolveRemoteAudio()
    return () => {
      isMounted = false
    }
  }, [isRemoteActive, remoteState?.currentSongId, remoteState?.currentTitle, remoteState?.currentAudioUrl])

  const roomSongData = listenRoom?.playbackState?.song
  const roomAsSong: Song | null =
    roomSongData && roomSongData.title
      ? {
          videoId: roomSongData.videoId || (roomSongData as any).id || `room_${Date.now()}`,
          title: roomSongData.title,
          channelTitle: roomSongData.artist || 'Artist',
          thumbnailUrl: roomSongData.artwork || '',
          audioUrl: roomSongData.audioUrl || '',
          durationFormatted: formatTime((roomSongData.durationMs || (roomSongData as any).duration || 210000) / 1000),
          durationMs: roomSongData.durationMs || (roomSongData as any).duration || 210000,
          viewCountFormatted: ''
        }
      : null

  const remoteAsSong: Song | null =
    remoteState && remoteState.currentTitle
      ? {
          videoId: remoteState.currentSongId || `transferred_${Date.now()}`,
          title: remoteState.currentTitle,
          channelTitle: remoteState.currentArtist || 'Artist',
          thumbnailUrl: remoteState.currentArtwork || '',
          audioUrl: remoteState.currentAudioUrl || cachedRemoteAudioUrl || '',
          durationFormatted: formatTime((remoteState.durationMs || 210000) / 1000),
          durationMs: remoteState.durationMs || 210000,
          viewCountFormatted: ''
        }
      : null

  const displaySong: Song | null = inListenTogetherRoom
    ? song || roomAsSong
    : isRemoteActive
      ? remoteAsSong || song
      : song || remoteAsSong

  const activeTitle = inListenTogetherRoom
    ? displaySong?.title || roomAsSong?.title
    : isRemoteActive
      ? remoteState?.currentTitle || displaySong?.title
      : displaySong?.title
  const activeArtist = inListenTogetherRoom
    ? displaySong?.channelTitle || roomAsSong?.channelTitle || 'Artist'
    : isRemoteActive
      ? remoteState?.currentArtist || displaySong?.channelTitle || 'Artist'
      : displaySong?.channelTitle
  const activeArtwork = inListenTogetherRoom
    ? displaySong?.thumbnailUrl || roomAsSong?.thumbnailUrl
    : isRemoteActive
      ? remoteState?.currentArtwork || displaySong?.thumbnailUrl
      : displaySong?.thumbnailUrl
  const activeDuration = inListenTogetherRoom
    ? duration || (displaySong?.durationMs ? displaySong.durationMs / 1000 : 210)
    : isRemoteActive
      ? (remoteState?.durationMs || 0) / 1000 || 210
      : duration || (displaySong?.durationMs ? displaySong.durationMs / 1000 : 210)

  const effectiveQueue: Song[] = React.useMemo(() => {
    if (isRemoteActive && remoteState?.queue) {
      const raw = Array.isArray(remoteState.queue)
        ? remoteState.queue
        : typeof remoteState.queue === 'object' && remoteState.queue !== null
          ? Object.values(remoteState.queue)
          : []
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
      : queue && typeof queue === 'object' && queue !== null
        ? Object.values(queue)
        : []
    return (safeQueue || []).filter((s: any) => Boolean(s && typeof s === 'object' && (s.videoId || s.id))) as Song[]
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

  const playingDevice = connectedDevices.find((d) => d.deviceId === remoteState?.currentDeviceId)
  const remoteDeviceName =
    playingDevice?.deviceName || (remoteState?.currentDeviceId?.includes('android') ? 'Mobile Phone' : 'Remote Device')

  // Activate local playback on Web when user transfers or remote device hands off
  const activateLocalPlayback = async (targetSong?: any, positionMs?: number) => {
    const seekSec = (positionMs != null ? positionMs : remoteState?.positionMs || 0) / 1000
    const rawTitle = targetSong?.title || remoteState?.currentTitle || song?.title || ''
    const rawArtist =
      targetSong?.channelTitle || targetSong?.artist || remoteState?.currentArtist || song?.channelTitle || 'Artist'
    const rawArtwork =
      targetSong?.thumbnailUrl || targetSong?.artwork || remoteState?.currentArtwork || song?.thumbnailUrl || ''
    const rawId =
      targetSong?.videoId ||
      targetSong?.id ||
      remoteState?.currentSongId ||
      song?.videoId ||
      `transferred_${Date.now()}`
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
      audioRef.current.play().catch((error) => {
        console.warn('[WebPlayer] Transfer play catch (may need user gesture):', error)
        const onFirstInteraction = () => {
          audioRef.current?.play().catch(() => {})
          window.removeEventListener('click', onFirstInteraction)
          window.removeEventListener('keydown', onFirstInteraction)
        }
        window.addEventListener('click', onFirstInteraction, { once: true })
        window.addEventListener('keydown', onFirstInteraction, { once: true })
      })
    }

    if (!storageService.isMultiDevicePlaybackSeparate()) {
      IsaiConnectService.transferPlaybackToDevice(
        myDeviceId,
        transferred,
        positionMs != null ? positionMs : remoteState?.positionMs || 0
      )
    }
    onTransferPlayback?.(transferred)

    // If audio stream is not yet cached, resolve 320kbps stream via search
    if (!targetAudio && rawTitle) {
      try {
        const cleanTitle = cleanHtmlTitle(rawTitle)
          .replace(/\s*[|–—-].*$/, '')
          .replaceAll(/\s*\(.*?(official|video|audio|lyrics|hd|4k|song).*?\)/gi, '')
          .replaceAll(/\s*\[.*?(official|video|audio|lyrics|hd|4k|song).*?\]/gi, '')
          .replace(/\.{2,}$/, '')
          .trim()

        const expectedLang = (transferred.language || detectSongLanguage(transferred)).toLowerCase().trim()
        const results = await musicApi.searchSongs(cleanTitle ? `${cleanTitle} ${expectedLang}` : rawTitle)
        const matched = results?.find((r) => {
          if (!r.audioUrl) return false
          const rLang = (r.language || detectSongLanguage(r)).toLowerCase().trim()
          if (expectedLang && rLang && expectedLang !== rLang) return false
          return isSameSongOrDuplicate(r, transferred)
        })
        if (matched?.audioUrl) {
          const resolvedUrl = matched.audioUrl
          setCachedRemoteAudioUrl(resolvedUrl)
          targetAudio = resolvedUrl
          if (audioRef.current) {
            audioRef.current.src = resolvedUrl
            if (seekSec > 0) audioRef.current.currentTime = seekSec
            audioRef.current.play().catch((error) => {
              console.warn('[WebPlayer] Resolved async play catch:', error)
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
          if (!storageService.isMultiDevicePlaybackSeparate()) {
            IsaiConnectService.updatePlaybackState({ currentAudioUrl: resolvedUrl })
          }
        }
      } catch (error) {
        console.warn('[WebPlayer] Failed to resolve audio for transferred song:', error)
      }
    }
  }

  const activateLocalPlaybackRef = useRef(activateLocalPlayback)
  activateLocalPlaybackRef.current = activateLocalPlayback

  // Initialize ISAI Connect service
  useEffect(() => {
    IsaiConnectService.initialize(userId)
    const unsub = IsaiConnectService.subscribePlaybackState((syncState) => {
      const isSeparate = storageService.isMultiDevicePlaybackSeparate()
      if (isSeparate) {
        // In separate multi-device mode, do not auto-pause when another device plays!
        return
      }
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
          if (
            syncState.currentTitle &&
            (!activeSongRef.current || activeSongRef.current.title !== syncState.currentTitle || !isPlaying)
          ) {
            console.info('[WebPlayer] Playback handed off to Web in syncState:', syncState)
            activateLocalPlaybackRef.current(
              {
                id: syncState.currentSongId,
                title: syncState.currentTitle,
                artist: syncState.currentArtist,
                artwork: syncState.currentArtwork,
                audioUrl: syncState.currentAudioUrl
              },
              syncState.positionMs
            )
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
      const isSeparate = storageService.isMultiDevicePlaybackSeparate()
      if (isSeparate && (!cmd.targetDeviceId || cmd.targetDeviceId !== myDeviceId)) {
        return
      }
      if (cmd.targetDeviceId && cmd.targetDeviceId !== myDeviceId) return
      console.info('[WebPlayer] Incoming remote command from other device:', cmd.action, cmd)

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
          audioRef.current.play().catch((error) => console.warn('[WebPlayer] Remote PLAY catch:', error))
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
        const rawVol = cmd.volume != null ? cmd.volume : cmd.positionMs != null ? cmd.positionMs / 100 : 1
        const clamped = Math.max(0, Math.min(1, rawVol))
        console.info('[WebPlayer] Setting remote volume to:', clamped)
        setVolume(clamped)
        setIsMuted(clamped === 0)
      } else if (cmd.action === 'PLAY_SONG') {
        console.info('[WebPlayer] Remote command PLAY_SONG received:', cmd)
        const incomingSong =
          cmd.song ||
          (cmd.songId
            ? {
                videoId: cmd.songId,
                title: cmd.songTitle,
                channelTitle: cmd.songArtist,
                thumbnailUrl: cmd.songArtwork,
                audioUrl: cmd.songAudioUrl
              }
            : null)
        activateLocalPlaybackRef.current(incomingSong, cmd.positionMs)
      }
    })
    return () => unsubCmd()
  }, [myDeviceId, onNextSong, onPrevSong, song?.audioUrl, displaySong?.audioUrl])

  // Listen Together Room & Playback State Subscription
  useEffect(() => {
    const unsubRoom = ListenTogetherService.subscribeRoomUpdate((r) => {
      setListenRoom(r)
    })

    const unsubPb = ListenTogetherService.subscribePlaybackState((pbState, isHost) => {
      if (isHost) return // Host controls local playback and broadcasts to guests

      if (pbState.song) {
        const isDifferentSong = !activeSongRef.current || activeSongRef.current.videoId !== pbState.song.videoId

        const roomSong: Song = {
          videoId: pbState.song.videoId,
          title: pbState.song.title,
          channelTitle: pbState.song.artist,
          thumbnailUrl: pbState.song.artwork,
          audioUrl: pbState.song.audioUrl || '',
          durationFormatted: formatTime((pbState.song.durationMs || 210000) / 1000),
          durationMs: pbState.song.durationMs || 210000,
          viewCountFormatted: ''
        }

        if (isDifferentSong) {
          activeSongRef.current = roomSong
          onTransferPlayback?.(roomSong)
        }

        const incomingUrl = pbState.song.audioUrl
        if (incomingUrl && audioRef.current && (isDifferentSong || audioRef.current.src !== incomingUrl)) {
          audioRef.current.src = incomingUrl
          const expected = ListenTogetherService.getExpectedPosition()
          audioRef.current.currentTime = expected
          if (pbState.state === 'PLAYING') {
            audioRef.current
              .play()
              .then(() => {
                setIsPlaying(true)
                setNeedsAutoplayGesture(false)
              })
              .catch((error) => {
                console.warn('[WebPlayer] Autoplay blocked for synchronized playback:', error)
                setNeedsAutoplayGesture(true)
              })
          } else {
            audioRef.current.pause()
            setIsPlaying(false)
          }
        }

        if (!isDifferentSong && audioRef.current) {
          // Same song, state or position update
          if (pbState.state === 'PLAYING') {
            const expected = ListenTogetherService.getExpectedPosition()
            if (Math.abs(audioRef.current.currentTime - expected) > 1.2) {
              audioRef.current.currentTime = expected
            }
            audioRef.current
              .play()
              .then(() => {
                setIsPlaying(true)
                setNeedsAutoplayGesture(false)
              })
              .catch((error) => {
                console.warn('[WebPlayer] Autoplay blocked for synchronized playback:', error)
                setNeedsAutoplayGesture(true)
              })
          } else if (pbState.state === 'PAUSED') {
            audioRef.current.pause()
            audioRef.current.currentTime = pbState.positionSec
            setIsPlaying(false)
          }
        }
      }

      // Synchronize room volume from Host
      if (typeof pbState.volume === 'number' && !isHost) {
        const normVol = Math.max(0, Math.min(1, pbState.volume / 100))
        setVolume(normVol)
        setIsMuted(normVol === 0)
        if (audioRef.current) {
          audioRef.current.volume = normVol
        }
      }
    })

    return () => {
      unsubRoom()
      unsubPb()
    }
  }, [onTransferPlayback])

  // Periodic Drift Correction and Host Live Position Calibration for Listen Together
  useEffect(() => {
    if (!listenRoom) return
    const isHostDevice = listenRoom.hostDeviceId === ListenTogetherService.getDeviceId()

    const syncInterval = setInterval(() => {
      if (!audioRef.current) return

      if (isHostDevice) {
        if (audioRef.current.playbackRate !== 1) {
          audioRef.current.playbackRate = 1
        }
        if (!audioRef.current.paused && audioRef.current.currentTime > 0.1) {
          ListenTogetherService.hostCalibratePosition(audioRef.current.currentTime)
        }
        return
      }

      // Guest: Never interrupt active audio buffering
      if (audioRef.current.paused || audioRef.current.readyState < 3) return

      const adj = ListenTogetherService.calculateDriftAdjustment(audioRef.current.currentTime)
      if (adj.action === 'SPEED_ADJUST' && adj.speed) {
        audioRef.current.playbackRate = adj.speed
      } else if (adj.action === 'SEEK' && typeof adj.targetPosition === 'number') {
        audioRef.current.currentTime = adj.targetPosition
        audioRef.current.playbackRate = 1
      } else if (audioRef.current.playbackRate !== 1) {
        audioRef.current.playbackRate = 1
      }
    }, 500)

    return () => {
      clearInterval(syncInterval)
      if (audioRef.current) audioRef.current.playbackRate = 1
    }
  }, [listenRoom])

  // Sync song changes to ISAI Connect (when Web is playing locally in sync mode)
  useEffect(() => {
    if (!song || isRemoteActive || storageService.isMultiDevicePlaybackSeparate()) return
    if (remoteState?.currentDeviceId && remoteState.currentDeviceId !== myDeviceId) return

    const qList = (Array.isArray(queue) ? queue : []).filter(Boolean)
    const curQIdx = song ? qList.findIndex((q) => (q.videoId || (q as any).id) === song.videoId) : 0

    IsaiConnectService.updatePlaybackState({
      currentDeviceId: myDeviceId,
      currentSongId: song.videoId,
      currentTitle: song.title,
      currentArtist: song.channelTitle || 'ISAI Artist',
      currentArtwork: song.thumbnailUrl,
      currentAudioUrl: song.audioUrl,
      isPlaying,
      durationMs: (duration || 211) * 1000,
      positionMs: currentTime * 1000,
      queueIndex: curQIdx >= 0 ? curQIdx : 0,
      queue: qList.map((q) => ({
        id: q.videoId || (q as any).id || '',
        title: q.title || '',
        artist: q.channelTitle || (q as any).artist || '',
        artwork: q.thumbnailUrl || (q as any).artwork || '',
        audioUrl: (q as any).audioUrl || ''
      }))
    })
  }, [
    song?.videoId,
    song?.title,
    song?.thumbnailUrl,
    song?.audioUrl,
    isPlaying,
    isRemoteActive,
    remoteState?.currentDeviceId,
    myDeviceId,
    queue,
    duration,
    currentTime
  ])

  // Immediate auto-play and recommendation threshold session management when a new song is selected
  useEffect(() => {
    if (isRemoteActive) {
      // Remote device (Phone) is playing! Do not start local audio on Web
      return
    }

    // If previous song was skipped (<10s) and not marked meaningful:
    const prevSong = activeSongRef.current
    if (
      prevSong &&
      prevSong.videoId !== song?.videoId &&
      !recordedMeaningfulRef.current &&
      listenSecondsRef.current > 0 &&
      listenSecondsRef.current < 10
    ) {
      recommendationService.recordListen(userId, prevSong, listenSecondsRef.current, duration)
    }

    activeSongRef.current = song
    listenSecondsRef.current = 0
    recordedMeaningfulRef.current = false
    lastTimeRef.current = 0

    const activeAudioUrl = displaySong?.audioUrl || (!isRemoteActive ? remoteState?.currentAudioUrl : undefined)
    if (activeAudioUrl && !isRemoteActive) {
      setIsPlaying(true)
      const seekTarget =
        pendingSeekTimeRef.current !== null && pendingSeekTimeRef.current > 0 ? pendingSeekTimeRef.current : 0
      pendingSeekTimeRef.current = null
      setCurrentTime(seekTarget)
      if (audioRef.current) {
        if (audioRef.current.src !== activeAudioUrl) {
          audioRef.current.src = activeAudioUrl
          if (seekTarget > 0) {
            try {
              audioRef.current.currentTime = seekTarget
            } catch {}
          }
        }
        audioRef.current.play().catch((error) => {
          console.warn('[WebPlayer] Autoplay catch:', error)
          if (error.name === 'NotAllowedError') {
            setNeedsAutoplayGesture(true)
          }
        })
      }
    }

    if (listenRoom && listenRoom.hostDeviceId === ListenTogetherService.getDeviceId() && song) {
      ListenTogetherService.hostChangeSong(song, true, Math.round(volume * 100))
    }
  }, [
    displaySong?.videoId,
    displaySong?.audioUrl,
    isRemoteActive,
    listenRoom?.hostDeviceId,
    song?.videoId,
    duration,
    remoteState?.currentAudioUrl,
    song,
    userId
  ])

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
        audioRef.current.play().catch((error) => {
          console.warn('[WebPlayer] Playback play warning:', error)
          if (error.name === 'NotAllowedError') {
            setNeedsAutoplayGesture(true)
          }
        })
      } else {
        audioRef.current.pause()
      }
    }
  }, [
    displaySong?.videoId,
    displaySong?.audioUrl,
    isPlaying,
    volume,
    isMuted,
    isRemoteActive,
    remoteState?.currentAudioUrl
  ])

  const togglePlay = () => {
    if (listenRoom) {
      const isHost = listenRoom.hostDeviceId === ListenTogetherService.getDeviceId()
      if (!isHost) {
        showPlayerToast(`👑 Play/Pause is controlled by Host (${listenRoom.hostDeviceName || 'Host'})`)
        return
      }
      if (isPlaying) {
        ListenTogetherService.hostPause(currentTime)
      } else {
        ListenTogetherService.hostPlay(currentTime)
      }
    }
    if (isRemoteActive) {
      const nextAction = remoteState?.isPlaying ? 'PAUSE' : 'PLAY'
      const targetDev = remoteState?.currentDeviceId || ''
      IsaiConnectService.sendCommand(nextAction, { targetDeviceId: targetDev })
      if (!storageService.isMultiDevicePlaybackSeparate()) {
        IsaiConnectService.updatePlaybackState({ isPlaying: !remoteState?.isPlaying })
      }
    } else if (audioRef.current) {
      if (isPlaying) {
        audioRef.current.pause()
        setIsPlaying(false)
        if (song && !isRemoteActive) {
          const curIdx = effectiveQueue.findIndex((q) => (q.videoId || (q as any).id) === song.videoId)
          storageService.setLastPlaybackSession({
            song,
            queue: effectiveQueue,
            queueIndex: curIdx >= 0 ? curIdx : 0,
            positionSec: audioRef.current.currentTime || currentTime,
            wasPlaying: false,
            timestamp: Date.now()
          })
        }
      } else {
        const activeAudioUrl = displaySong?.audioUrl || remoteState?.currentAudioUrl
        if (activeAudioUrl && !audioRef.current.src) {
          audioRef.current.src = activeAudioUrl
        }
        audioRef.current
          .play()
          .then(() => setIsPlaying(true))
          .catch((error) => console.warn(error))
        setIsPlaying(true)
        if (song && !isRemoteActive) {
          const curIdx = effectiveQueue.findIndex((q) => (q.videoId || (q as any).id) === song.videoId)
          storageService.setLastPlaybackSession({
            song,
            queue: effectiveQueue,
            queueIndex: curIdx >= 0 ? curIdx : 0,
            positionSec: audioRef.current.currentTime || currentTime,
            wasPlaying: true,
            timestamp: Date.now()
          })
        }
      }
    } else {
      setIsPlaying((prev) => !prev)
    }
  }

  const handleNext = () => {
    if (listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId()) {
      showPlayerToast(`👑 Only Host (${listenRoom.hostDeviceName || 'Host'}) can skip songs`)
      return
    }
    if (isRemoteActive) {
      IsaiConnectService.sendCommand('NEXT', { targetDeviceId: remoteState?.currentDeviceId })
    } else if (onNextSong) {
      onNextSong()
    }
  }

  const handlePrev = () => {
    if (listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId()) {
      showPlayerToast(`👑 Only Host (${listenRoom.hostDeviceName || 'Host'}) can skip songs`)
      return
    }
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

  const handleTransferToRemote = (targetDeviceId: string) => {
    if (audioRef.current) {
      audioRef.current.pause()
    }
    setIsPlaying(false)
    if (targetDeviceId && song) {
      IsaiConnectService.transferPlaybackToDevice(targetDeviceId, song, Math.round((currentTime || 0) * 1000))
    }
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
        const delta = lastTimeRef.current > 0 ? cur - lastTimeRef.current : 0
        if (delta > 0 && delta < 2) {
          listenSecondsRef.current += delta
        }
        lastTimeRef.current = cur

        const played = listenSecondsRef.current
        if (!recordedMeaningfulRef.current && (played >= 30 || (dur > 0 && played / dur >= 0.5))) {
          recordedMeaningfulRef.current = true
          recommendationService.recordListen(userId, song, played, dur)
        }
      }

      // Persist last playback session periodically (every 2.5s)
      const now = Date.now()
      if (song && !isRemoteActive && now - lastSaveSessionTimeRef.current > 2500) {
        lastSaveSessionTimeRef.current = now
        const curIdx = effectiveQueue.findIndex((q) => (q.videoId || (q as any).id) === song.videoId)
        storageService.setLastPlaybackSession({
          song,
          queue: effectiveQueue,
          queueIndex: curIdx >= 0 ? curIdx : 0,
          positionSec: cur,
          wasPlaying: isPlaying,
          timestamp: now
        })
      }

      // Sync position to Firebase every 1.5 seconds so remote device has accurate timestamp (in sync mode)
      if (
        !isRemoteActive &&
        !storageService.isMultiDevicePlaybackSeparate() &&
        song &&
        now - lastSyncTimeRef.current > 1500
      ) {
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
    const val = Number.parseFloat(e.target.value)
    if (listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId()) {
      showPlayerToast(`👑 Playback position is controlled by Host (${listenRoom.hostDeviceName || 'Host'})`)
      return
    }
    if (isRemoteActive) {
      setRemoteCurrentTime(val)
      IsaiConnectService.sendCommand('SEEK', { positionMs: Math.round(val * 1000) })
      if (!storageService.isMultiDevicePlaybackSeparate()) {
        IsaiConnectService.updatePlaybackState({ positionMs: Math.round(val * 1000) })
      }
    } else {
      setCurrentTime(val)
      if (audioRef.current) {
        audioRef.current.currentTime = val
      }
      if (song && !isRemoteActive) {
        const curIdx = effectiveQueue.findIndex((q) => (q.videoId || (q as any).id) === song.videoId)
        storageService.setLastPlaybackSession({
          song,
          queue: effectiveQueue,
          queueIndex: curIdx >= 0 ? curIdx : 0,
          positionSec: val,
          wasPlaying: isPlaying,
          timestamp: Date.now()
        })
      }
      if (listenRoom && listenRoom.hostDeviceId === ListenTogetherService.getDeviceId()) {
        ListenTogetherService.hostSeek(val)
      }
    }
  }

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = Number.parseFloat(e.target.value)
    if (listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId()) {
      showPlayerToast(`👑 Sound/Volume is controlled by Host (${listenRoom.hostDeviceName || 'Host'})`)
      return
    }
    // 1. Instant 0ms local audio and visual state change
    if (audioRef.current) {
      audioRef.current.volume = val
    }
    setVolume(val)
    if (val === 0) setIsMuted(true)
    else setIsMuted(false)

    // 2. Debounce heavy Firebase network sync so UI slider & audio never lag
    if (volumeSyncTimeoutRef.current) {
      clearTimeout(volumeSyncTimeoutRef.current)
    }
    volumeSyncTimeoutRef.current = setTimeout(() => {
      if (listenRoom && listenRoom.hostDeviceId === ListenTogetherService.getDeviceId()) {
        ListenTogetherService.hostSetVolume(Math.round(val * 100))
      }
      if (!isRemoteActive) {
        if (!storageService.isMultiDevicePlaybackSeparate()) {
          IsaiConnectService.updatePlaybackState({ volume: val })
        }
      } else {
        IsaiConnectService.sendCommand('SET_VOLUME', { volume: val })
      }
    }, 80)
  }

  const handleAudioEnded = () => {
    if (listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId()) {
      return // Guests wait for Host to transition track
    }
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

  const handleAudioError = (e: React.SyntheticEvent<HTMLAudioElement, Event>) => {
    const audio = audioRef.current
    const currentSrc = audio?.src || displaySong?.audioUrl || song?.audioUrl || ''
    console.warn('[WebPlayer] Audio stream error encountered for:', currentSrc, e)

    // 1. Bitrate fallback: 320kbps -> 160kbps -> 96kbps
    if (currentSrc && (currentSrc.includes('_320.mp4') || currentSrc.includes('_320.'))) {
      const fallback160 = currentSrc.replace(/_320\.(mp4|m4a|mp3)/, '_160.$1')
      console.warn('[WebPlayer] Bitrate fallback 320 -> 160:', fallback160)
      if (audio) {
        audio.src = fallback160
        audio.load()
        audio.play().catch(() => {})
      }
      return
    }
    if (currentSrc && (currentSrc.includes('_160.mp4') || currentSrc.includes('_160.'))) {
      const fallback96 = currentSrc.replace(/_160\.(mp4|m4a|mp3)/, '_96.$1')
      console.warn('[WebPlayer] Bitrate fallback 160 -> 96:', fallback96)
      if (audio) {
        audio.src = fallback96
        audio.load()
        audio.play().catch(() => {})
      }
      return
    }

    // 2. Transient network retry (up to 2 retries)
    if (streamRetryCountRef.current < 2) {
      streamRetryCountRef.current += 1
      console.warn(`[WebPlayer] Retrying stream playback (attempt ${streamRetryCountRef.current})...`)
      setTimeout(() => {
        if (audioRef.current) {
          audioRef.current.load()
          audioRef.current.play().catch(() => {})
        }
      }, 1200)
      return
    }

    // 3. All stream fallbacks exhausted -> skip to next song
    streamRetryCountRef.current = 0
    console.warn('[WebPlayer] All stream fallbacks exhausted, skipping to next track')
    onNextSong?.()
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
        preload="auto"
        onTimeUpdate={handleTimeUpdate}
        onLoadedMetadata={handleLoadedMetadata}
        onCanPlay={handleLoadedMetadata}
        onEnded={handleAudioEnded}
        onError={handleAudioError}
      />

      {/* 1. Persistent Bottom Player Bar */}
      <div className="web-player-bar">
        {/* Left Track Info */}
        <div className="player-left-info" onClick={() => setIsExpanded(true)} style={{ cursor: 'pointer' }}>
          {activeArtwork ? (
            <img src={activeArtwork} alt={activeTitle} className="player-artwork" />
          ) : (
            <div
              className="player-artwork"
              style={{ background: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
            >
              🎵
            </div>
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
              {activeIsPlaying ? (
                <Pause size={22} fill="#fff" />
              ) : (
                <Play size={22} fill="#fff" style={{ marginLeft: '3px' }} />
              )}
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
              style={{
                background: `linear-gradient(to right, var(--isai-purple) 0%, var(--isai-purple) ${
                  activeDuration > 0 ? (activePosition / activeDuration) * 100 : 0
                }%, var(--surface-elevated, #CBD5E1) ${
                  activeDuration > 0 ? (activePosition / activeDuration) * 100 : 0
                }%, var(--surface-elevated, #CBD5E1) 100%)`
              }}
            />
            <span className="time-stamp">{formatTime(activeDuration)}</span>
          </div>
        </div>

        {/* Right Action Icons */}
        <div className="player-right-actions">
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
            <Laptop size={20} color={isRemoteActive ? '#10B981' : 'var(--text-primary)'} />
            {isRemoteActive && (
              <span
                style={{
                  position: 'absolute',
                  top: '4px',
                  right: '4px',
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: '#10B981',
                  boxShadow: '0 0 8px #10B981'
                }}
              />
            )}
          </button>

          <button
            className="control-btn"
            onClick={() => setIsListenTogetherModalOpen(true)}
            title="Listen Together (Multi-device Sync Room)"
            style={{ position: 'relative' }}
          >
            <Radio size={20} color={listenRoom ? '#16A34A' : 'var(--text-primary)'} />
            {listenRoom && (
              <span
                style={{
                  position: 'absolute',
                  top: '4px',
                  right: '4px',
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: '#16A34A',
                  boxShadow: '0 0 8px #16A34A'
                }}
              />
            )}
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              className="control-btn"
              onClick={() => {
                if (listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId()) {
                  showPlayerToast(`👑 Sound/Volume is controlled by Host (${listenRoom.hostDeviceName || 'Host'})`)
                  return
                }
                const nextMuted = !isMuted
                setIsMuted(nextMuted)
                const effectiveVol = nextMuted ? 0 : (volume || 0.8)
                if (audioRef.current) {
                  audioRef.current.volume = effectiveVol
                }
                if (volumeSyncTimeoutRef.current) {
                  clearTimeout(volumeSyncTimeoutRef.current)
                }
                volumeSyncTimeoutRef.current = setTimeout(() => {
                  if (listenRoom && listenRoom.hostDeviceId === ListenTogetherService.getDeviceId()) {
                    ListenTogetherService.hostSetVolume(Math.round(effectiveVol * 100))
                  }
                  if (!isRemoteActive) {
                    if (!storageService.isMultiDevicePlaybackSeparate()) {
                      IsaiConnectService.updatePlaybackState({ volume: effectiveVol })
                    }
                  } else {
                    IsaiConnectService.sendCommand('SET_VOLUME', { volume: effectiveVol })
                  }
                }, 80)
              }}
              title={isMuted ? 'Unmute' : 'Mute'}
            >
              {isMuted || volume === 0 ? <VolumeX size={20} /> : <Volume2 size={20} />}
            </button>

            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={isMuted ? 0 : volume}
              onInput={handleVolumeChange}
              onChange={handleVolumeChange}
              disabled={Boolean(listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId())}
              className="custom-range-slider"
              title={
                listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId()
                  ? `Volume is controlled by Host (${listenRoom.hostDeviceName || 'Host'})`
                  : 'Volume'
              }
              style={{
                width: '85px',
                background: `linear-gradient(to right, var(--isai-purple) 0%, var(--isai-purple) ${
                  (isMuted ? 0 : volume) * 100
                }%, var(--surface-elevated, #CBD5E1) ${
                  (isMuted ? 0 : volume) * 100
                }%, var(--surface-elevated, #CBD5E1) 100%)`,
                opacity: listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId() ? 0.6 : 1,
                cursor:
                  listenRoom && listenRoom.hostDeviceId !== ListenTogetherService.getDeviceId()
                    ? 'not-allowed'
                    : 'pointer'
              }}
            />
          </div>

          <button className="control-btn" onClick={() => setIsExpanded(true)} title="Expand Fullscreen">
            <Maximize2 size={20} />
          </button>
        </div>
      </div>

      {/* Floating Notice Toast */}
      {toastMessage && (
        <div
          style={{
            position: 'fixed',
            bottom: '100px',
            left: '50%',
            transform: 'translateX(-50%)',
            background: 'rgba(20, 20, 25, 0.95)',
            border: '1px solid var(--border-glass-bright)',
            backdropFilter: 'blur(12px)',
            color: '#fff',
            padding: '10px 20px',
            borderRadius: '24px',
            fontSize: '13px',
            fontWeight: 700,
            boxShadow: '0 8px 30px rgba(0,0,0,0.5)',
            zIndex: 9999,
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}
        >
          <span>{toastMessage}</span>
        </div>
      )}

      {/* 2. Expanded Full-screen View */}
      {isExpanded && (
        <div className="expanded-player-overlay">
          <div className="expanded-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span
                style={{
                  fontSize: '11px',
                  fontWeight: 900,
                  color: 'var(--isai-purple)',
                  letterSpacing: '0.12em'
                }}
              >
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

            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
              <button
                className={`pill-button ${listenRoom ? 'active' : ''}`}
                onClick={() => setIsListenTogetherModalOpen(true)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  backgroundColor: listenRoom ? 'rgba(200, 255, 0, 0.15)' : undefined,
                  borderColor: listenRoom ? '#C8FF00' : undefined,
                  color: listenRoom ? '#C8FF00' : undefined
                }}
              >
                <Radio size={14} />
                <span>{listenRoom ? listenRoom.roomCode : 'Sync Room'}</span>
              </button>
              <button
                className="control-btn"
                onClick={handleShareSong}
                title="Share Song"
                style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '6px 12px' }}
              >
                <Share2 size={16} />
                <span style={{ fontSize: '12px', fontWeight: 600 }}>Share</span>
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
                <div
                  className="expanded-artwork-box"
                  style={{ background: 'var(--surface-elevated, #E2E8F0)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                >
                  🎵
                </div>
              )}
              <h2 style={{ fontSize: '28px', fontWeight: 900, marginTop: '24px', color: 'var(--text-primary)' }}>{activeTitle}</h2>
              <p style={{ fontSize: '16px', color: 'var(--text-secondary)', marginTop: '4px' }}>{activeArtist}</p>
            </div>

            {/* Right Panel: Tabs for Queue & Lyrics */}
            <div
              className="expanded-lyrics-panel"
              style={{ display: 'flex', flexDirection: 'column', height: '100%', maxHeight: '550px' }}
            >
              {/* Tab Selector: Queue vs Lyrics */}
              <div
                style={{
                  display: 'flex',
                  gap: '8px',
                  paddingBottom: '14px',
                  borderBottom: '1px solid var(--border-subtle)',
                  marginBottom: '16px'
                }}
              >
                <button
                  onClick={() => setActiveExpandedTab('queue')}
                  style={{
                    flex: 1,
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: 'none',
                    fontWeight: 700,
                    fontSize: '13px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    background:
                      activeExpandedTab === 'queue' ? 'var(--isai-gradient)' : 'var(--surface-elevated, #E2E8F0)',
                    color: activeExpandedTab === 'queue' ? '#fff' : 'var(--text-secondary, #334155)',
                    transition: 'all 0.2s',
                    boxShadow: activeExpandedTab === 'queue' ? '0 4px 12px rgba(124, 58, 237, 0.3)' : 'none'
                  }}
                >
                  <ListMusic size={15} /> Up Next Queue ({effectiveQueue.length})
                </button>
                <button
                  onClick={() => setActiveExpandedTab('lyrics')}
                  style={{
                    flex: 1,
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: 'none',
                    fontWeight: 700,
                    fontSize: '13px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '6px',
                    background:
                      activeExpandedTab === 'lyrics' ? 'var(--isai-gradient)' : 'var(--surface-elevated, #E2E8F0)',
                    color: activeExpandedTab === 'lyrics' ? '#fff' : 'var(--text-secondary, #334155)',
                    transition: 'all 0.2s',
                    boxShadow: activeExpandedTab === 'lyrics' ? '0 4px 12px rgba(124, 58, 237, 0.3)' : 'none'
                  }}
                >
                  <FileText size={15} /> Lyrics & Sing-Along
                </button>
              </div>

              {activeExpandedTab === 'queue' ? (
                <div style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
                  {/* Queue Sub-Tabs: Up Next, Played, All */}
                  <div style={{ display: 'flex', gap: '6px', marginBottom: '14px' }}>
                    {(['up_next', 'played', 'all'] as const).map((tabKey) => {
                      const labels = {
                        up_next: `Up Next (${Math.max(0, effectiveQueue.length - 1)})`,
                        played: `Played (${sessionPlayedSongs.length})`,
                        all: `All Queue (${effectiveQueue.length})`
                      }
                      const isSelected = selectedQueueTab === tabKey
                      return (
                        <button
                          key={tabKey}
                          onClick={() => setSelectedQueueTab(tabKey)}
                          style={{
                            padding: '4px 10px',
                            borderRadius: '16px',
                            border: isSelected ? '1px solid var(--isai-pink)' : '1px solid var(--border-subtle)',
                            background: isSelected ? 'rgba(236, 72, 153, 0.15)' : 'transparent',
                            color: isSelected ? 'var(--isai-pink)' : 'var(--text-secondary)',
                            fontSize: '11px',
                            fontWeight: 700,
                            cursor: 'pointer'
                          }}
                        >
                          {labels[tabKey]}
                        </button>
                      )
                    })}
                    {effectiveQueue.length > 1 && selectedQueueTab !== 'played' && (
                      <button
                        onClick={() => onClearQueue?.()}
                        style={{
                          marginLeft: 'auto',
                          background: 'rgba(239, 68, 68, 0.12)',
                          border: '1px solid rgba(239, 68, 68, 0.3)',
                          color: '#ef4444',
                          borderRadius: '6px',
                          padding: '3px 8px',
                          fontSize: '11px',
                          fontWeight: 700,
                          cursor: 'pointer'
                        }}
                      >
                        Clear
                      </button>
                    )}
                  </div>

                  {/* Queue Sub-tab Content with scroll */}
                  <div style={{ overflowY: 'auto', flex: 1, paddingRight: '4px' }}>
                    {selectedQueueTab === 'played' ? (
                      sessionPlayedSongs.length === 0 ? (
                        <div
                          style={{
                            textAlign: 'center',
                            padding: '40px 0',
                            color: 'var(--text-muted)',
                            fontSize: '13px'
                          }}
                        >
                          No previously played songs in this session yet
                        </div>
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          {sessionPlayedSongs.map((pSong, pIdx) => (
                            <div
                              key={`played_${pSong.videoId}_${pIdx}`}
                              onClick={() => onSelectQueueItem?.(pSong)}
                              className="queue-row-item"
                              style={{ cursor: 'pointer' }}
                            >
                              <History size={15} color="var(--text-muted)" />
                              <img
                                src={pSong.thumbnailUrl || ''}
                                alt={pSong.title}
                                style={{ width: '38px', height: '38px', borderRadius: '6px', objectFit: 'cover' }}
                              />
                              <div style={{ flex: 1, minWidth: 0 }}>
                                <div className="ytm-pl-title">{pSong.title}</div>
                                <div className="ytm-pl-sub">{pSong.channelTitle}</div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )
                    ) : (
                      <>
                        {/* Queue items */}
                        {effectiveQueue.length === 0 ? (
                          <div
                            style={{
                              textAlign: 'center',
                              padding: '30px 0',
                              color: 'var(--text-muted)',
                              fontSize: '13px'
                            }}
                          >
                            Queue is empty
                          </div>
                        ) : (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            {effectiveQueue.filter(Boolean).map((qSong, idx) => {
                              if (selectedQueueTab === 'up_next' && idx === 0) return null // Hide current in Up Next view
                              const songId = qSong?.videoId || (qSong as any)?.id || `q_${idx}`
                              const isCurrent =
                                songId ===
                                (isRemoteActive
                                  ? remoteState?.currentSongId || song?.videoId
                                  : song?.videoId || remoteState?.currentSongId)
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
                                    const sourceIdx =
                                      draggedIdx !== null
                                        ? draggedIdx
                                        : Number.parseInt(e.dataTransfer.getData('text/plain'), 10)
                                    if (Number.isNaN(sourceIdx) || sourceIdx === idx) {
                                      setDraggedIdx(null)
                                      setDragOverIdx(null)
                                      setTimeout(() => {
                                        isDraggingRef.current = false
                                      }, 150)
                                      return
                                    }
                                    const nextQueue = [...effectiveQueue]
                                    const [movedSong] = nextQueue.splice(sourceIdx, 1)
                                    nextQueue.splice(idx, 0, movedSong)
                                    if (isRemoteActive) {
                                      IsaiConnectService.updatePlaybackState({
                                        queue: nextQueue.map((item) => ({
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
                                  <div
                                    className="queue-drag-handle"
                                    title="Drag to reorder"
                                    onClick={(e) => e.stopPropagation()}
                                  >
                                    <GripVertical size={16} />
                                  </div>

                                  <div
                                    style={{
                                      width: '22px',
                                      textAlign: 'center',
                                      fontSize: '12px',
                                      fontWeight: 700,
                                      color: isCurrent ? 'var(--isai-pink)' : 'var(--text-muted)'
                                    }}
                                  >
                                    {isCurrent ? '▶' : `${idx + 1}`}
                                  </div>

                                  <img
                                    src={qSong.thumbnailUrl || ''}
                                    alt={qSong.title || 'Track'}
                                    style={{
                                      width: '40px',
                                      height: '40px',
                                      borderRadius: '6px',
                                      objectFit: 'cover',
                                      flexShrink: 0
                                    }}
                                  />

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

                                  {isCurrent ? (
                                    <span
                                      style={{
                                        fontSize: '10px',
                                        fontWeight: 800,
                                        color: 'var(--isai-purple-light)',
                                        background: 'rgba(139, 92, 246, 0.2)',
                                        padding: '3px 8px',
                                        borderRadius: '6px',
                                        letterSpacing: '0.05em'
                                      }}
                                    >
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

                        {/* Autoplay Recommendations section */}
                        {queueRecommendations.length > 0 && (
                          <div
                            style={{
                              marginTop: '24px',
                              paddingTop: '16px',
                              borderTop: '1px solid var(--border-subtle)'
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '12px' }}>
                              <Sparkles size={16} color="var(--isai-pink)" />
                              <span style={{ fontSize: '13px', fontWeight: 800, color: 'var(--text-primary)' }}>
                                Autoplay Similar Songs
                              </span>
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                              {queueRecommendations.map((rSong, rIdx) => (
                                <div
                                  key={`rec_${rSong.videoId}_${rIdx}`}
                                  style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '12px',
                                    padding: '8px 10px',
                                    borderRadius: '8px',
                                    background: 'rgba(255,255,255,0.03)',
                                    border: '1px solid var(--border-subtle)',
                                    cursor: 'pointer'
                                  }}
                                  onClick={() => onSelectQueueItem?.(rSong)}
                                >
                                  <img
                                    src={rSong.thumbnailUrl || ''}
                                    alt={rSong.title}
                                    style={{ width: '36px', height: '36px', borderRadius: '6px', objectFit: 'cover' }}
                                  />
                                  <div style={{ flex: 1, minWidth: 0 }}>
                                    <div className="ytm-pl-title">{rSong.title}</div>
                                    <div className="ytm-pl-sub">{rSong.channelTitle}</div>
                                  </div>
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation()
                                      const nextQueue = [...effectiveQueue, rSong]
                                      onReorderQueue?.(nextQueue)
                                      setQueueRecommendations((prev) => prev.filter((_, i) => i !== rIdx))
                                    }}
                                    style={{
                                      background: 'rgba(139, 92, 246, 0.15)',
                                      border: 'none',
                                      color: 'var(--isai-purple-light)',
                                      borderRadius: '50%',
                                      width: '28px',
                                      height: '28px',
                                      display: 'flex',
                                      alignItems: 'center',
                                      justifyContent: 'center',
                                      cursor: 'pointer'
                                    }}
                                    title="Add to queue"
                                  >
                                    <Plus size={16} />
                                  </button>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>
              ) : (
                /* Lyrics & Sing-Along Panel */
                <div style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
                  <div
                    style={{
                      flex: 1,
                      overflowY: 'auto',
                      padding: '20px',
                      background: 'var(--surface-card)',
                      borderRadius: '12px',
                      border: '1px solid var(--border-subtle)',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      textAlign: 'center'
                    }}
                  >
                    <FileText size={40} color="var(--isai-pink)" style={{ marginBottom: '14px', opacity: 0.8 }} />
                    <h4 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '8px' }}>
                      {activeTitle}
                    </h4>
                    <p style={{ fontSize: '13px', color: 'var(--isai-purple)', marginBottom: '24px' }}>
                      Sing-Along Mode • {activeArtist}
                    </p>
                    <div
                      style={{
                        lineHeight: '2.2',
                        fontSize: '16px',
                        color: 'var(--text-secondary)',
                        fontWeight: 500
                      }}
                    >
                      <p style={{ margin: '8px 0', color: 'var(--isai-pink)', fontWeight: 700, fontSize: '18px' }}>
                        ♪ {activeTitle} ♪
                      </p>
                      <p style={{ margin: '8px 0' }}>Listen and sing along with high-fidelity acoustics...</p>
                      <p style={{ margin: '8px 0' }}>Artist: {activeArtist}</p>
                      <p style={{ margin: '8px 0', opacity: 0.6, fontSize: '13px' }}>
                        Lyrics sync automatically when synchronized track timestamps are available.
                      </p>
                    </div>
                  </div>
                </div>
              )}
            </div>
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

      {/* Listen Together Modal */}
      <ListenTogetherModal
        isOpen={isListenTogetherModalOpen}
        onClose={() => setIsListenTogetherModalOpen(false)}
        currentSong={song || displaySong}
        currentTime={currentTime}
        needsAutoplayGesture={needsAutoplayGesture}
        onAutoplayGestureUnlock={() => {
          if (audioRef.current) {
            audioRef.current.currentTime = ListenTogetherService.getExpectedPosition()
            audioRef.current
              .play()
              .then(() => {
                setIsPlaying(true)
                setNeedsAutoplayGesture(false)
              })
              .catch(() => {})
          }
        }}
      />

      {/* Floating Autoplay Unblock prompt if browser blocks guest auto-play */}
      {needsAutoplayGesture && (
        <div
          style={{
            position: 'fixed',
            bottom: '95px',
            left: '50%',
            transform: 'translateX(-50%)',
            backgroundColor: '#C8FF00',
            color: '#000000',
            fontWeight: 800,
            padding: '12px 24px',
            borderRadius: '30px',
            boxShadow: '0 10px 30px rgba(0,0,0,0.6)',
            zIndex: 9999,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}
          onClick={() => {
            if (audioRef.current) {
              audioRef.current.currentTime = ListenTogetherService.getExpectedPosition()
              audioRef.current
                .play()
                .then(() => {
                  setIsPlaying(true)
                  setNeedsAutoplayGesture(false)
                })
                .catch(() => {})
            }
          }}
        >
          <Volume2 size={18} />
          <span>Tap to start synchronized playback</span>
        </div>
      )}
    </>
  )
}
