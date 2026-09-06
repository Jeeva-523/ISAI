import React, { useRef, useState, useEffect } from 'react'
import type { Song } from '@shared/models/song'
import { DeviceInfo, IsaiConnectService, PlaybackStateSync } from '../services/IsaiConnectService'
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
  FileText
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
  userId = 'user_jeeva_default',
  onTransferPlayback
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
  const [showLyricsPanel, setShowLyricsPanel] = useState(false)
  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false)

  const myDeviceId = IsaiConnectService.getMyDeviceId()
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const pendingSeekTimeRef = useRef<number | null>(null)

  // Remote active determination (Spotify Connect mode)
  const isRemoteActive = Boolean(
    remoteState &&
    remoteState.currentDeviceId &&
    remoteState.currentDeviceId !== myDeviceId &&
    remoteState.currentTitle
  )

  const activeTitle = isRemoteActive ? remoteState?.currentTitle : song?.title
  const activeArtist = isRemoteActive ? (remoteState?.currentArtist || 'Artist') : song?.channelTitle
  const activeArtwork = isRemoteActive ? remoteState?.currentArtwork : song?.thumbnailUrl
  const activeDuration = isRemoteActive ? ((remoteState?.durationMs || 0) / 1000 || 210) : (duration || 210)
  const activePosition = isRemoteActive ? ((remoteState?.positionMs || 0) / 1000 || 0) : currentTime
  const activeIsPlaying = isRemoteActive ? Boolean(remoteState?.isPlaying) : isPlaying

  const playingDevice = connectedDevices.find(d => d.deviceId === remoteState?.currentDeviceId)
  const remoteDeviceName = playingDevice?.deviceName || (remoteState?.currentDeviceId?.includes('android') ? "Jeeva's Phone" : "Mobile Device")

  // Initialize ISAI Connect service
  useEffect(() => {
    IsaiConnectService.initialize(userId)
    const unsub = IsaiConnectService.subscribePlaybackState((syncState) => {
      if (syncState && syncState.currentDeviceId) {
        if (syncState.currentDeviceId !== myDeviceId && syncState.updatedByDeviceId !== myDeviceId) {
          if (audioRef.current) audioRef.current.pause()
          setIsPlaying(false)
        }
      }
    })
    return () => unsub()
  }, [userId, myDeviceId])

  // Listen to remote commands sent from Android or another device (when Web is active)
  useEffect(() => {
    const unsubCmd = IsaiConnectService.subscribeCommands((cmd) => {
      if (cmd.issuedByDeviceId === myDeviceId) return
      if (cmd.targetDeviceId && cmd.targetDeviceId !== myDeviceId) return
      console.log('[WebPlayer] Incoming remote command from other device:', cmd.action, cmd)

      if (cmd.action === 'PAUSE') {
        if (audioRef.current) audioRef.current.pause()
        setIsPlaying(false)
      } else if (cmd.action === 'PLAY') {
        if (audioRef.current) audioRef.current.play().catch(() => {})
        setIsPlaying(true)
      } else if (cmd.action === 'NEXT') {
        onNextSong?.()
      } else if (cmd.action === 'PREV') {
        onPrevSong?.()
      } else if (cmd.action === 'SEEK' && cmd.positionMs != null) {
        const sec = cmd.positionMs / 1000
        setCurrentTime(sec)
        if (audioRef.current) audioRef.current.currentTime = sec
      } else if (cmd.action === 'PLAY_SONG' && cmd.songId) {
        const songToPlay: Song = {
          videoId: cmd.songId,
          title: cmd.songTitle || 'Selected Song',
          channelTitle: cmd.songArtist || '',
          thumbnailUrl: cmd.songArtwork || '',
          audioUrl: cmd.songAudioUrl,
          durationFormatted: '3:30',
          durationMs: 210000,
          viewCountFormatted: ''
        }
        IsaiConnectService.transferPlaybackToDevice(myDeviceId)
        const seekSec = (cmd.positionMs || 0) / 1000
        if (seekSec > 0) {
          pendingSeekTimeRef.current = seekSec
          setCurrentTime(seekSec)
        }
        if (onTransferPlayback) {
          onTransferPlayback(songToPlay)
        } else {
          onSelectQueueItem?.(songToPlay)
        }
        setIsPlaying(true)
      }
    })
    return () => unsubCmd()
  }, [myDeviceId, onNextSong, onPrevSong, onSelectQueueItem, onTransferPlayback])

  // Sync song changes to ISAI Connect (when Web is playing locally)
  useEffect(() => {
    if (!song || isRemoteActive) return
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
      queue: queue.map(q => ({
        id: q.videoId,
        title: q.title,
        artist: q.channelTitle || '',
        artwork: q.thumbnailUrl || ''
      }))
    })
  }, [song?.videoId, song?.audioUrl, isPlaying, isRemoteActive, myDeviceId])

  // Control audio element play/pause
  useEffect(() => {
    if (isRemoteActive) {
      if (audioRef.current) audioRef.current.pause()
      return
    }

    if (audioRef.current && song?.audioUrl) {
      audioRef.current.volume = isMuted ? 0 : volume
      if (isPlaying) {
        audioRef.current.play().catch(err => {
          console.warn('[WebPlayer] Playback play warning:', err)
        })
      } else {
        audioRef.current.pause()
      }
    }
  }, [song?.videoId, song?.audioUrl, isPlaying, volume, isMuted, isRemoteActive])

  if (!song && !isRemoteActive) return null

  const togglePlay = () => {
    if (isRemoteActive) {
      IsaiConnectService.sendCommand(remoteState?.isPlaying ? 'PAUSE' : 'PLAY')
      IsaiConnectService.updatePlaybackState({ isPlaying: !remoteState?.isPlaying })
    } else {
      setIsPlaying(prev => !prev)
    }
  }

  const handleNext = () => {
    if (isRemoteActive) {
      IsaiConnectService.sendCommand('NEXT')
    } else if (onNextSong) {
      onNextSong()
    }
  }

  const handlePrev = () => {
    if (isRemoteActive) {
      IsaiConnectService.sendCommand('PREV')
    } else if (onPrevSong) {
      onPrevSong()
    }
  }

  const handleTransferToWeb = () => {
    IsaiConnectService.transferPlaybackToDevice(myDeviceId)
    if (remoteState && remoteState.currentSongId && onTransferPlayback) {
      const seekSec = (remoteState.positionMs || 0) / 1000
      if (seekSec > 0) {
        pendingSeekTimeRef.current = seekSec
        setCurrentTime(seekSec)
      }
      onTransferPlayback({
        videoId: remoteState.currentSongId,
        title: remoteState.currentTitle || 'Current Track',
        channelTitle: remoteState.currentArtist || 'Artist',
        thumbnailUrl: remoteState.currentArtwork || '',
        audioUrl: remoteState.currentAudioUrl,
        durationFormatted: '3:30',
        durationMs: remoteState.durationMs || 210000,
        viewCountFormatted: ''
      })
      setIsPlaying(true)
    }
  }

  const handleTransferToRemote = (_targetDeviceId: string) => {
    if (audioRef.current) {
      audioRef.current.pause()
    }
    setIsPlaying(false)
  }

  const lastSyncTimeRef = useRef<number>(0)

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

      // Sync position to Firebase every 1.5 seconds so remote device has accurate timestamp
      const now = Date.now()
      if (!isRemoteActive && song && now - lastSyncTimeRef.current > 1500) {
        lastSyncTimeRef.current = now
        IsaiConnectService.updatePlaybackState({
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
  }

  const handleAudioEnded = () => {
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

  return (
    <>
      {!isRemoteActive && song && (
        <audio
          ref={audioRef}
          src={song.audioUrl}
          onTimeUpdate={handleTimeUpdate}
          onLoadedMetadata={handleLoadedMetadata}
          onCanPlay={handleLoadedMetadata}
          onEnded={handleAudioEnded}
        />
      )}

      {/* Spotify Connect Remote Active Indicator */}
      {isRemoteActive && (
        <div
          className="spotify-connect-badge"
          onClick={() => setIsConnectModalOpen(true)}
          style={{
            position: 'fixed',
            bottom: '86px',
            right: '24px',
            background: 'linear-gradient(135deg, #1DB954 0%, #108538 100%)',
            color: '#fff',
            padding: '8px 16px',
            borderRadius: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            fontSize: '12px',
            fontWeight: 800,
            boxShadow: '0 8px 24px rgba(29, 185, 84, 0.4), 0 2px 6px rgba(0,0,0,0.4)',
            cursor: 'pointer',
            zIndex: 9999,
            backdropFilter: 'blur(8px)',
            border: '1px solid rgba(255, 255, 255, 0.3)'
          }}
        >
          <span style={{ fontSize: '16px' }}>📱</span>
          <span>Listening on <strong style={{ textDecoration: 'underline' }}>{remoteDeviceName}</strong></span>
          <button
            onClick={(e) => {
              e.stopPropagation()
              handleTransferToWeb()
            }}
            style={{
              background: '#000',
              color: '#1DB954',
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: '14px',
              padding: '4px 10px',
              fontSize: '11px',
              fontWeight: 900,
              cursor: 'pointer',
              marginLeft: '4px',
              boxShadow: '0 2px 6px rgba(0,0,0,0.3)'
            }}
          >
            Play on Web 💻
          </button>
        </div>
      )}

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
              className="control-btn"
              onClick={(e) => {
                e.stopPropagation()
                onToggleFavorite(song)
              }}
              style={{ color: isFavorite ? 'var(--isai-pink)' : 'var(--text-muted)', marginLeft: '8px' }}
            >
              <Heart size={18} fill={isFavorite ? 'var(--isai-pink)' : 'none'} />
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
            className={`control-btn ${showLyricsPanel ? 'active' : ''}`}
            onClick={() => {
              setIsExpanded(true)
              setShowLyricsPanel(true)
            }}
            title="Lyrics"
          >
            <FileText size={18} />
          </button>

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
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '11px', fontWeight: 900, color: 'var(--isai-purple-light)', letterSpacing: '0.12em' }}>
                {isRemoteActive ? `PLAYING ON ${remoteDeviceName.toUpperCase()}` : 'PLAYING FROM ISAI'}
              </span>
            </div>

            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                className={`pill-button ${!showLyricsPanel && !showQueuePanel ? 'active' : ''}`}
                onClick={() => {
                  setShowLyricsPanel(false)
                  setShowQueuePanel(false)
                }}
              >
                Artwork
              </button>
              <button
                className={`pill-button ${showLyricsPanel ? 'active' : ''}`}
                onClick={() => {
                  setShowLyricsPanel(true)
                  setShowQueuePanel(false)
                }}
              >
                Lyrics
              </button>
              <button
                className={`pill-button ${showQueuePanel ? 'active' : ''}`}
                onClick={() => {
                  setShowQueuePanel(true)
                  setShowLyricsPanel(false)
                }}
              >
                Queue ({queue.length})
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

            {/* Right Panel (Lyrics or Queue) */}
            {showLyricsPanel ? (
              <div className="expanded-lyrics-panel">
                <h3 style={{ fontSize: '18px', fontWeight: 800, marginBottom: '16px', color: 'var(--isai-purple-light)' }}>
                  Lyrics • {activeTitle}
                </h3>
                <p style={{ fontSize: '15px', lineHeight: 2, color: 'var(--text-primary)', whiteSpace: 'pre-line' }}>
                  {`[Intro Instrumental]
                  
                  Anirudh Ravichander Beats...
                  
                  Nallaru Po dude, feel the rhythm flow,
                  Every beat in your soul starting to glow.
                  
                  [Chorus]
                  ISAI Tamil music in 320kbps high definition,
                  Listening to your favorite song is the highest emotion!
                  
                  [Outro]`
                  }
                </p>
              </div>
            ) : showQueuePanel ? (
              <div className="expanded-lyrics-panel">
                <h3 style={{ fontSize: '18px', fontWeight: 800, marginBottom: '16px', color: 'var(--isai-purple-light)' }}>
                  Up Next Queue
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {queue.map((qSong) => (
                    <div
                      key={qSong.videoId}
                      onClick={() => onSelectQueueItem?.(qSong)}
                      className="ytm-playlist-item"
                      style={{ cursor: 'pointer', background: qSong.videoId === (song?.videoId || remoteState?.currentSongId) ? 'rgba(139, 92, 246, 0.2)' : undefined }}
                    >
                      <img src={qSong.thumbnailUrl} alt={qSong.title} style={{ width: '40px', height: '40px', borderRadius: '6px' }} />
                      <div>
                        <div className="ytm-pl-title">{qSong.title}</div>
                        <div className="ytm-pl-sub">{qSong.channelTitle}</div>
                      </div>
                    </div>
                  ))}
                </div>
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
