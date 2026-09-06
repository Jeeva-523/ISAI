import React, { useRef, useState, useEffect } from 'react'
import type { Song } from '@shared/models/song'
import { IsaiConnectService, PlaybackStateSync } from '../services/IsaiConnectService'
import { IsaiConnectModal } from './IsaiConnectModal'

interface WebPlayerProps {
  song: Song | null
  onClose: () => void
  isFavorite?: boolean
  onToggleFavorite?: (song: Song) => void
  onNextSong?: () => void
  onPrevSong?: () => void
  queue?: Song[]
  onSelectQueueItem?: (song: Song) => void
  userId?: string
}

function formatTime(sec: number): string {
  if (isNaN(sec) || sec < 0) return '0:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${s < 10 ? '0' : ''}${s}`
}

export const WebPlayer: React.FC<WebPlayerProps> = ({
  song,
  onClose,
  isFavorite = false,
  onToggleFavorite,
  onNextSong,
  onPrevSong,
  queue = [],
  onSelectQueueItem,
  userId = 'user_jeeva_default'
}) => {
  const [isExpanded, setIsExpanded] = useState(false)
  const [activeTab, setActiveTab] = useState<'upnext' | 'lyrics' | 'comments' | 'related'>('upnext')
  const [autoplay, setAutoplay] = useState(true)
  const [isPlaying, setIsPlaying] = useState(true)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(0)
  const [isShuffle, setIsShuffle] = useState(false)
  const [isRepeat, setIsRepeat] = useState(false)
  const [volume, setVolume] = useState(1)
  const [isLiked, setIsLiked] = useState(false)
  const [isDisliked, setIsDisliked] = useState(false)
  const [filterMood, setFilterMood] = useState('All')
  const [isConnectModalOpen, setIsConnectModalOpen] = useState(false)

  const [remotePlaybackState, setRemotePlaybackState] = useState<PlaybackStateSync | null>(null)
  const myDeviceId = IsaiConnectService.getMyDeviceId()

  const audioRef = useRef<HTMLAudioElement | null>(null)

  // Initialize ISAI Connect service
  useEffect(() => {
    IsaiConnectService.initialize(userId)
    const unsub = IsaiConnectService.subscribePlaybackState((syncState) => {
      setRemotePlaybackState(syncState)
      if (syncState) {
        if (syncState.currentDeviceId !== myDeviceId) {
          // We are a Remote Controller -> Pause local audio
          if (audioRef.current) audioRef.current.pause()
          setIsPlaying(syncState.isPlaying)
          if (syncState.positionMs != null) {
            setCurrentTime(Math.floor(syncState.positionMs / 1000))
          }
        }
      }
    })
    return () => {
      unsub()
    }
  }, [userId, myDeviceId])

  // Sync song changes to ISAI Connect if we are active player
  useEffect(() => {
    if (!song) return
    const isActivePlayer = !remotePlaybackState?.currentDeviceId || remotePlaybackState.currentDeviceId === myDeviceId
    if (isActivePlayer) {
      IsaiConnectService.updatePlaybackState({
        currentDeviceId: myDeviceId,
        currentSongId: song.videoId,
        currentTitle: song.title,
        currentArtist: song.channelTitle || 'ISAI Artist',
        currentArtwork: song.thumbnailUrl,
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
    }
  }, [song?.videoId, isPlaying])

  if (!song) return null

  const embedUrl = `https://www.youtube.com/embed/${song.videoId}?autoplay=1&playsinline=1&rel=0`
  const hasDirectAudio = Boolean(song.audioUrl)

  const togglePlayPause = () => {
    if (audioRef.current) {
      if (isPlaying) {
        audioRef.current.pause()
        setIsPlaying(false)
      } else {
        audioRef.current.play()
        setIsPlaying(true)
      }
    } else {
      setIsPlaying(!isPlaying)
    }
  }

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = Number(e.target.value)
    setCurrentTime(val)
    if (audioRef.current) {
      audioRef.current.currentTime = val
    }
  }

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = Number(e.target.value)
    setVolume(val)
    if (audioRef.current) {
      audioRef.current.volume = val
    }
  }

  return (
    <>
      {/* =========================================================================
          FULL YOUTUBE MUSIC WATCH PAGE OVERLAY (music.youtube.com/watch?v=...)
         ========================================================================= */}
      {isExpanded && (
        <div className="ytm-watch-page">
          {/* 1. Top Bar */}
          <div className="ytm-watch-topbar">
            <div className="ytm-watch-topbar-left">
              <button className="ytm-watch-btn-chevron" onClick={() => setIsExpanded(false)} title="Minimize Watch Page">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/>
                </svg>
              </button>
              <div className="ytm-watch-logo">
                <div className="ytm-logo-box">
                  <img src="/logo.png" alt="ISAI Logo" className="ytm-logo-img" />
                </div>
                <span className="isai-brand-font">ISAI</span>
                <span className="ytm-logo-text">Music</span>
              </div>
            </div>

            <div className="ytm-watch-search">
              <span className="ytm-search-icon">🔍</span>
              <input type="text" placeholder="Search songs, albums, artists, podcasts" className="ytm-search-input" />
            </div>

            <div className="ytm-watch-topbar-right">
              <button className="ytm-watch-btn-icon" title="Cast to Device">📡</button>
              <div className="ytm-watch-avatar">J</div>
            </div>
          </div>

          {/* 2. Watch Page Main 2-Column Content */}
          <div className="ytm-watch-main">
            {/* LEFT COLUMN: Stage (Video/Artwork + Info) */}
            <div className="ytm-watch-left-stage">
              <div className="ytm-watch-player-wrapper">
                {!hasDirectAudio ? (
                  <iframe
                    key={song.videoId}
                    src={embedUrl}
                    title={song.title}
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                    allowFullScreen
                    className="ytm-watch-iframe"
                  />
                ) : (
                  <div className="ytm-watch-artwork-stage">
                    <img
                      src={song.thumbnailUrl || `https://img.youtube.com/vi/${song.videoId}/hqdefault.jpg`}
                      alt={song.title}
                      className="ytm-watch-artwork-img"
                    />
                    <div className="ytm-watch-artwork-overlay">
                      <div className="ytm-watch-badge">⚡ 320 KBPS HD AUDIO</div>
                    </div>
                  </div>
                )}
              </div>

              {/* Track Title & Artist Meta Row */}
              <div className="ytm-watch-track-meta">
                <div className="ytm-watch-track-info">
                  <h1 className="ytm-watch-title">{song.title}</h1>
                  <p className="ytm-watch-artist">
                    {song.channelTitle} • {song.viewCountFormatted || '60M views'} • 280K likes
                  </p>
                </div>

                <div className="ytm-watch-actions">
                  <button
                    className={`ytm-action-btn ${isLiked ? 'active' : ''}`}
                    onClick={() => { setIsLiked(!isLiked); if (isDisliked) setIsDisliked(false) }}
                    title="I like this"
                  >
                    👍 {isLiked ? 'Liked' : ''}
                  </button>
                  <button
                    className={`ytm-action-btn ${isDisliked ? 'active' : ''}`}
                    onClick={() => { setIsDisliked(!isDisliked); if (isLiked) setIsLiked(false) }}
                    title="I dislike this"
                  >
                    👎
                  </button>
                  <button
                    className="ytm-action-btn"
                    onClick={() => onToggleFavorite?.(song)}
                    title="Save to Library"
                  >
                    {isFavorite ? '💖 Saved' : '🤍 Save'}
                  </button>
                  <button className="ytm-action-btn" title="Share">🔗 Share</button>
                  <button className="ytm-action-btn" title="More options">⋮</button>
                </div>
              </div>
            </div>

            {/* RIGHT COLUMN: Sidebar (UP NEXT, LYRICS, COMMENTS, RELATED) */}
            <div className="ytm-watch-right-sidebar">
              {/* Sidebar Tabs */}
              <div className="ytm-sidebar-tabs">
                <button
                  className={`ytm-tab-btn ${activeTab === 'upnext' ? 'active' : ''}`}
                  onClick={() => setActiveTab('upnext')}
                >
                  UP NEXT
                </button>
                <button
                  className={`ytm-tab-btn ${activeTab === 'lyrics' ? 'active' : ''}`}
                  onClick={() => setActiveTab('lyrics')}
                >
                  LYRICS
                </button>
                <button
                  className={`ytm-tab-btn ${activeTab === 'comments' ? 'active' : ''}`}
                  onClick={() => setActiveTab('comments')}
                >
                  COMMENTS
                </button>
                <button
                  className={`ytm-tab-btn ${activeTab === 'related' ? 'active' : ''}`}
                  onClick={() => setActiveTab('related')}
                >
                  RELATED
                </button>
              </div>

              {/* Tab 1: UP NEXT Queue */}
              {activeTab === 'upnext' && (
                <div className="ytm-tab-content ytm-upnext-content">
                  {/* Playing from Header */}
                  <div className="ytm-queue-header">
                    <div className="ytm-queue-title-wrap">
                      <span className="ytm-queue-subtitle">Playing from</span>
                      <h3 className="ytm-queue-title">{song.album || `${song.channelTitle} Mix`}</h3>
                    </div>
                    <button className="ytm-btn-save-queue">≡ Save</button>
                  </div>

                  {/* Autoplay Toggle */}
                  <div className="ytm-autoplay-row">
                    <div className="ytm-autoplay-info">
                      <span className="ytm-autoplay-label">Autoplay</span>
                      <span className="ytm-autoplay-sub">Add similar content to the end of the queue</span>
                    </div>
                    <label className="ytm-switch">
                      <input type="checkbox" checked={autoplay} onChange={(e) => setAutoplay(e.target.checked)} />
                      <span className="ytm-slider" />
                    </label>
                  </div>

                  {/* Currently Playing Card */}
                  <div className="ytm-queue-now-playing">
                    <div className="ytm-now-playing-left">
                      <span className="ytm-play-indicator">▶</span>
                      <div className="ytm-now-playing-meta">
                        <h4 className="ytm-now-playing-title">{song.title}</h4>
                        <p className="ytm-now-playing-artist">{song.channelTitle}</p>
                      </div>
                    </div>
                    <span className="ytm-queue-duration">{formatTime(duration || 211)}</span>
                  </div>

                  {/* Mood Filter Pills for Queue */}
                  <div className="ytm-queue-mood-pills">
                    {['All', 'Familiar', 'Deep cuts', 'Tamil', 'Discover'].map((pill) => (
                      <button
                        key={pill}
                        className={`ytm-queue-pill ${filterMood === pill ? 'active' : ''}`}
                        onClick={() => setFilterMood(pill)}
                      >
                        {pill}
                      </button>
                    ))}
                  </div>

                  {/* Queue Items List */}
                  <div className="ytm-queue-list">
                    {queue.length > 0 ? (
                      queue.map((item, idx) => (
                        <div
                          key={`${item.videoId}_${idx}`}
                          className={`ytm-queue-item ${item.videoId === song.videoId ? 'active' : ''}`}
                          onClick={() => onSelectQueueItem?.(item)}
                        >
                          <img
                            src={item.thumbnailUrl || `https://img.youtube.com/vi/${item.videoId}/hqdefault.jpg`}
                            alt={item.title}
                            className="ytm-queue-thumb"
                          />
                          <div className="ytm-queue-item-info">
                            <h5 className="ytm-queue-item-title">{item.title}</h5>
                            <p className="ytm-queue-item-artist">{item.channelTitle}</p>
                          </div>
                          <span className="ytm-queue-item-dur">{item.durationFormatted || '3:30'}</span>
                        </div>
                      ))
                    ) : (
                      <div className="ytm-empty-queue">Queue is empty. Select a song to load queue!</div>
                    )}
                  </div>
                </div>
              )}

              {/* Tab 2: LYRICS */}
              {activeTab === 'lyrics' && (
                <div className="ytm-tab-content ytm-lyrics-content">
                  <h3 style={{ fontSize: '16px', color: '#fff', marginBottom: '16px' }}>Lyrics</h3>
                  <p className="ytm-lyrics-text">
                    🎵 {song.title}<br />
                    Artist: {song.channelTitle}<br /><br />
                    (Sing along to your favorite Tamil tracks on ISAI Music!)<br /><br />
                    [Verse 1]<br />
                    Enthan nenjil vaazhum un ninaivugaḷ...<br />
                    Kaatril midhakkum isai pola...<br />
                    Yaarum kaanaadha maaya ulagil...<br />
                    Nee en thodu vaanam...<br /><br />
                    [Chorus]<br />
                    ISAI HD 320 KBPS Audio Player...
                  </p>
                </div>
              )}

              {/* Tab 3: COMMENTS */}
              {activeTab === 'comments' && (
                <div className="ytm-tab-content ytm-comments-content">
                  <h3 style={{ fontSize: '16px', color: '#fff', marginBottom: '16px' }}>Comments</h3>
                  <div className="ytm-comment-item">
                    <div className="ytm-comment-avatar">J</div>
                    <div>
                      <div className="ytm-comment-author">Jeeva • 2 hours ago</div>
                      <div className="ytm-comment-text">Unbelievable sound quality! 320 KBPS HD audio hits different 🔥</div>
                    </div>
                  </div>
                  <div className="ytm-comment-item">
                    <div className="ytm-comment-avatar" style={{ background: '#ff007a' }}>A</div>
                    <div>
                      <div className="ytm-comment-author">Anirudh Fan • 1 day ago</div>
                      <div className="ytm-comment-text">Best Tamil song discovery app experience ever! ❤️</div>
                    </div>
                  </div>
                </div>
              )}

              {/* Tab 4: RELATED */}
              {activeTab === 'related' && (
                <div className="ytm-tab-content ytm-related-content">
                  <h3 style={{ fontSize: '16px', color: '#fff', marginBottom: '16px' }}>Related Artists & Tracks</h3>
                  <div className="ytm-related-card">
                    <img src={song.thumbnailUrl} alt={song.title} style={{ width: '60px', height: '60px', borderRadius: '8px', objectFit: 'cover' }} />
                    <div>
                      <h4 style={{ color: '#fff', fontSize: '14px' }}>{song.channelTitle}</h4>
                      <p style={{ color: '#aaa', fontSize: '12px' }}>Top Tamil Music Producer & Composer</p>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* 3. Bottom Player Bar in Watch Page */}
          <div className="ytm-watch-bottom-bar">
            <div className="ytm-watch-bar-left">
              <button className="ytm-bar-btn" onClick={onPrevSong} title="Previous">⏮</button>
              {hasDirectAudio ? (
                <button className="ytm-bar-btn-play" onClick={togglePlayPause} title={isPlaying ? 'Pause' : 'Play'}>
                  {isPlaying ? '⏸' : '▶'}
                </button>
              ) : (
                <button className="ytm-bar-btn-play" onClick={onNextSong} title="Next">⏭</button>
              )}
              <button className="ytm-bar-btn" onClick={onNextSong} title="Next">⏭</button>
              <span className="ytm-bar-time">{formatTime(currentTime)} / {formatTime(duration || 211)}</span>
            </div>

            {hasDirectAudio && (
              <div className="ytm-watch-bar-center-seekbar">
                <input
                  type="range"
                  min="0"
                  max={duration || 100}
                  value={currentTime}
                  onChange={handleSeek}
                  className="ytm-watch-seekbar"
                />
              </div>
            )}

            <div className="ytm-watch-bar-track-info">
              <img src={song.thumbnailUrl} alt={song.title} className="ytm-watch-bar-thumb" />
              <div>
                <h4 className="ytm-watch-bar-title">{song.title}</h4>
                <p className="ytm-watch-bar-artist">{song.channelTitle}</p>
              </div>
            </div>

            <div className="ytm-watch-bar-right">
              <span className="ytm-vol-icon">🔊</span>
              <input type="range" min="0" max="1" step="0.05" value={volume} onChange={handleVolumeChange} className="ytm-vol-slider" />
              <button className={`ytm-bar-btn ${isShuffle ? 'active' : ''}`} onClick={() => setIsShuffle(!isShuffle)}>🔀</button>
              <button className={`ytm-bar-btn ${isRepeat ? 'active' : ''}`} onClick={() => setIsRepeat(!isRepeat)}>🔁</button>
              <button className="ytm-bar-btn" onClick={() => setIsExpanded(false)} title="Collapse Watch Page">v</button>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          DOCKED BOTTOM PLAYER BAR (Default visible bar on main page)
         ========================================================================= */}
      <div className={`web-player-dock ${isExpanded ? 'hidden' : ''}`}>
        <div className="web-player-container">
          <div className="web-player-bar">
            {/* Click anywhere on song info to expand Watch Page */}
            <div
              className="web-player-left"
              onClick={() => setIsExpanded(true)}
              title="Click to open YouTube Music Watch Page"
              style={{ cursor: 'pointer' }}
            >
              <img
                src={song.thumbnailUrl || `https://img.youtube.com/vi/${song.videoId}/hqdefault.jpg`}
                alt={song.title}
                className="web-player-thumb"
              />
              <div className="web-player-track-info">
                <span className="web-player-badge">{hasDirectAudio ? '⚡ 320 KBPS HD AUDIO' : 'NOW PLAYING'}</span>
                <h4 className="web-player-title">{song.title}</h4>
                <p className="web-player-artist">{song.channelTitle}</p>
              </div>
            </div>

            {/* Native HTML5 Audio */}
            {hasDirectAudio && (
              <div style={{ flex: 1, maxWidth: '420px', margin: '0 12px' }}>
                <audio
                  ref={audioRef}
                  key={song.audioUrl}
                  src={song.audioUrl}
                  autoPlay
                  controls
                  onTimeUpdate={(e) => setCurrentTime((e.target as HTMLAudioElement).currentTime)}
                  onLoadedMetadata={(e) => setDuration((e.target as HTMLAudioElement).duration)}
                  onEnded={onNextSong}
                  onPlay={() => setIsPlaying(true)}
                  onPause={() => setIsPlaying(false)}
                  style={{ width: '100%', height: '36px', outline: 'none' }}
                />
              </div>
            )}

            <div className="web-player-controls">
              {onPrevSong && (
                <button className="player-btn prev-btn" onClick={onPrevSong} title="Previous track">
                  ⏮
                </button>
              )}

              {onNextSong && (
                <button className="player-btn next-btn" onClick={onNextSong} title="Next track">
                  ⏭
                </button>
              )}

              {onToggleFavorite && (
                <button
                  className={`player-btn fav-btn ${isFavorite ? 'active' : ''}`}
                  onClick={() => onToggleFavorite(song)}
                  title={isFavorite ? 'Remove Favorite' : 'Save Favorite'}
                >
                  {isFavorite ? '💖' : '🤍'}
                </button>
              )}

              <button
                className="player-btn connect-btn"
                onClick={() => setIsConnectModalOpen(true)}
                title="ISAI Connect - Multi-device playback"
                style={{
                  background: remotePlaybackState?.currentDeviceId && remotePlaybackState.currentDeviceId !== myDeviceId
                    ? 'linear-gradient(135deg, #C8FF00, #9ECC00)'
                    : 'rgba(255, 255, 255, 0.1)',
                  color: remotePlaybackState?.currentDeviceId && remotePlaybackState.currentDeviceId !== myDeviceId
                    ? '#0B0B0F'
                    : '#C8FF00',
                  border: '1px solid rgba(200, 255, 0, 0.4)',
                  padding: '6px 12px',
                  borderRadius: '20px',
                  fontWeight: 'bold',
                  fontSize: '12px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  transition: 'all 0.2s ease'
                }}
              >
                <span>📱</span>
                <span>
                  {remotePlaybackState?.currentDeviceId && remotePlaybackState.currentDeviceId !== myDeviceId
                    ? 'Connected'
                    : 'Connect'}
                </span>
              </button>

              <button
                className="player-btn expand-btn"
                onClick={() => setIsExpanded(true)}
                title="Expand YouTube Music Watch Page"
              >
                🎬
              </button>

              <button className="player-btn close-btn" onClick={onClose} title="Close player">
                ✖
              </button>
            </div>
          </div>
        </div>
      </div>

      <IsaiConnectModal
        isOpen={isConnectModalOpen}
        onClose={() => setIsConnectModalOpen(false)}
      />
    </>
  )
}
