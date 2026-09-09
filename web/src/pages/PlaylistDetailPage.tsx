import React, { useState, useEffect } from 'react'
import type { Song } from '@shared/models/song'
import { musicApi } from '@shared/api/music-api'
import { SongListItem } from '../components/SongListItem'
import { Play, Pause, Shuffle, ArrowLeft, Music, Clock, Sparkles, Plus, RefreshCw, Heart } from 'lucide-react'

interface PlaylistDetailPageProps {
  title: string
  subtitle: string
  coverUrl?: string
  gradient?: string
  songs: Song[]
  currentSong: Song | null
  isPlaying: boolean
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong: (song: Song, queue?: Song[]) => void
  onAddToPlaylist: (song: Song) => void
  onAddToQueue?: (song: Song) => void
  onPlayNext?: (song: Song) => void
  onBack: () => void
  userPreferredLanguages?: string[]
}

export const PlaylistDetailPage: React.FC<PlaylistDetailPageProps> = ({
  title,
  subtitle,
  coverUrl,
  gradient = 'linear-gradient(135deg, #1DB954 0%, #191414 100%)',
  songs: initialSongs,
  currentSong,
  isPlaying,
  isFavorite,
  onToggleFavorite,
  onPlaySong,
  onAddToPlaylist,
  onAddToQueue,
  onPlayNext,
  onBack,
  userPreferredLanguages = ['tamil']
}) => {
  const [playlistSongs, setPlaylistSongs] = useState<Song[]>(initialSongs)
  const [recommendedSongs, setRecommendedSongs] = useState<Song[]>([])
  const [isLoadingRecs, setIsLoadingRecs] = useState(false)
  const [addedIds, setAddedIds] = useState<Set<string>>(new Set())

  // Keep internal songs synced if parent prop changes
  useEffect(() => {
    setPlaylistSongs(initialSongs)
  }, [initialSongs])

  const isPlaylistActive = currentSong && playlistSongs.some(s => s.videoId === currentSong.videoId)
  const totalDurationMin = Math.round(playlistSongs.reduce((acc, s) => acc + (s.durationMs || 210000), 0) / 60000)

  // Fetch Spotify-style smart recommendations for this playlist
  const fetchRecommendations = async () => {
    setIsLoadingRecs(true)
    try {
      const primaryLang = (userPreferredLanguages[0] || 'tamil').toLowerCase()
      // Use playlist title or sample artist to query
      const sampleArtist = playlistSongs[0]?.channelTitle?.split(/[•,&|-]/)[0]?.trim() || ''
      const query = sampleArtist
        ? `${sampleArtist} ${primaryLang} hit songs`
        : `${title.replace(/mix|playlist|daily/gi, '').trim() || primaryLang} songs`

      const hits = await musicApi.searchSongs(query)
      const currentIds = new Set(playlistSongs.map(s => s.videoId))
      const cleanNorm = (t: string) => t.toLowerCase().replace(/[^a-z0-9]/g, '')
      const currentTitles = new Set(playlistSongs.map(s => cleanNorm(s.title).slice(0, 15)))

      const uniqueRecs = (hits || []).filter(h => {
        if (!h || !h.videoId || currentIds.has(h.videoId)) return false
        const t = cleanNorm(h.title).slice(0, 15)
        if (currentTitles.has(t)) return false
        return true
      }).slice(0, 6)

      setRecommendedSongs(uniqueRecs)
    } catch (e) {
      console.warn('[PlaylistDetail] Recommendations fetch error:', e)
    } finally {
      setIsLoadingRecs(false)
    }
  }

  useEffect(() => {
    fetchRecommendations()
  }, [title, userPreferredLanguages])

  const handleAddRecommendedSong = (song: Song) => {
    setPlaylistSongs(prev => [...prev, song])
    setAddedIds(prev => new Set(prev).add(song.videoId))
    onAddToPlaylist(song)
  }

  const handlePlayAll = () => {
    if (playlistSongs.length === 0) return
    if (isPlaylistActive) {
      onPlaySong(currentSong || playlistSongs[0], playlistSongs)
    } else {
      onPlaySong(playlistSongs[0], playlistSongs)
    }
  }

  const handleShufflePlay = () => {
    if (playlistSongs.length === 0) return
    const shuffled = [...playlistSongs].sort(() => Math.random() - 0.5)
    onPlaySong(shuffled[0], shuffled)
  }

  return (
    <div style={{ maxWidth: '1300px', margin: '0 auto', paddingBottom: '32px' }}>
      {/* Navigation Bar */}
      <button
        onClick={onBack}
        className="pill-button"
        style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}
      >
        <ArrowLeft size={16} /> Back
      </button>

      {/* Spotify Hero Playlist Header with Dynamic Ambient Glow */}
      <div
        style={{
          display: 'flex',
          gap: '32px',
          alignItems: 'flex-end',
          padding: '36px',
          borderRadius: 'var(--radius-xl)',
          background: `linear-gradient(180deg, rgba(29, 185, 84, 0.22) 0%, rgba(139, 92, 246, 0.12) 50%, var(--surface-card) 100%)`,
          border: '1px solid var(--border-glass)',
          marginBottom: '28px',
          flexWrap: 'wrap',
          boxShadow: '0 20px 50px rgba(0, 0, 0, 0.5)'
        }}
      >
        <div
          style={{
            width: '220px',
            height: '220px',
            borderRadius: 'var(--radius-lg)',
            overflow: 'hidden',
            boxShadow: '0 16px 48px rgba(0, 0, 0, 0.8), 0 0 30px rgba(29, 185, 84, 0.25)',
            background: gradient,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0
          }}
        >
          {coverUrl ? (
            <img src={coverUrl} alt={title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          ) : (
            <div style={{ textAlign: 'center', color: '#fff' }}>
              <Music size={64} color="#fff" style={{ opacity: 0.9 }} />
              <div style={{ fontSize: '13px', fontWeight: 800, marginTop: '8px', letterSpacing: '0.1em' }}>ISAI MIX</div>
            </div>
          )}
        </div>

        <div style={{ flex: 1, minWidth: '280px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
            <span style={{ fontSize: '11px', fontWeight: 900, background: 'rgba(29, 185, 84, 0.2)', color: '#1db954', border: '1px solid rgba(29, 185, 84, 0.35)', padding: '3px 10px', borderRadius: '12px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
              SPOTIFY MIX
            </span>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>• Public Playlist</span>
          </div>

          <h1 style={{ fontSize: '42px', fontWeight: 900, color: '#fff', margin: '4px 0 10px', letterSpacing: '-0.02em', lineHeight: 1.15 }}>
            {title}
          </h1>

          <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '16px', lineHeight: 1.5 }}>
            {subtitle}
          </p>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--text-primary)', marginBottom: '22px' }}>
            <span style={{ fontWeight: 800, color: 'var(--isai-purple-light)' }}>ISAI ⚡</span>
            <span style={{ color: 'var(--text-muted)' }}>•</span>
            <span style={{ fontWeight: 700 }}>{playlistSongs.length} songs</span>
            <span style={{ color: 'var(--text-muted)' }}>•</span>
            <span style={{ color: 'var(--text-muted)' }}>about {totalDurationMin} min</span>
          </div>

          {/* Spotify Action Bar */}
          <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
            {/* Big Spotify Green Circular Play Button */}
            <button
              onClick={handlePlayAll}
              style={{
                width: '56px',
                height: '56px',
                borderRadius: '50%',
                background: '#1db954',
                border: 'none',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
                boxShadow: '0 8px 24px rgba(29, 185, 84, 0.4)',
                transition: 'transform 0.2s ease, box-shadow 0.2s ease'
              }}
              title="Play Playlist"
            >
              {isPlaylistActive && isPlaying ? (
                <Pause size={26} fill="#000" color="#000" />
              ) : (
                <Play size={26} fill="#000" color="#000" style={{ marginLeft: '3px' }} />
              )}
            </button>

            {/* Shuffle Button */}
            <button
              className="pill-button"
              onClick={handleShufflePlay}
              style={{ padding: '10px 20px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}
              title="Shuffle Play"
            >
              <Shuffle size={16} /> Shuffle
            </button>

            {/* Like Playlist Button */}
            <button
              className="pill-button"
              style={{ padding: '10px 16px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px' }}
              title="Save to Your Library"
            >
              <Heart size={16} color="var(--isai-pink)" fill="var(--isai-pink)" /> Saved
            </button>
          </div>
        </div>
      </div>

      {/* Playlist Track List Table */}
      <div style={{ marginBottom: '48px' }}>
        <div style={{
          display: 'grid',
          gridTemplateColumns: '40px 52px 1fr 140px 80px 100px',
          gap: '14px',
          padding: '0 16px 12px',
          fontSize: '12px',
          fontWeight: 800,
          color: 'var(--text-muted)',
          borderBottom: '1px solid var(--border-subtle)',
          marginBottom: '10px'
        }}>
          <div style={{ textAlign: 'center' }}>#</div>
          <div>ART</div>
          <div>TITLE</div>
          <div>ALBUM</div>
          <div><Clock size={14} /></div>
          <div style={{ textAlign: 'right' }}>ACTIONS</div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          {playlistSongs.map((song, idx) => (
            <SongListItem
              key={`${song.videoId}_${idx}`}
              song={song}
              index={idx}
              isPlaying={currentSong?.videoId === song.videoId && isPlaying}
              isFavorite={isFavorite(song.videoId)}
              onPlay={(s) => onPlaySong(s, playlistSongs)}
              onToggleFavorite={onToggleFavorite}
              onAddToPlaylist={onAddToPlaylist}
              onAddToQueue={onAddToQueue}
              onPlayNext={onPlayNext}
            />
          ))}
        </div>
      </div>

      {/* Spotify Signature: Recommended Songs Section */}
      <div style={{
        marginTop: '32px',
        padding: '28px',
        background: 'var(--surface-card)',
        borderRadius: 'var(--radius-xl)',
        border: '1px solid var(--border-subtle)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Sparkles size={18} color="#1db954" />
              <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#fff', margin: 0 }}>
                Recommended
              </h2>
            </div>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: '4px 0 0 0' }}>
              Based on what's in this playlist and your language taste
            </p>
          </div>

          <button
            onClick={fetchRecommendations}
            disabled={isLoadingRecs}
            className="pill-button"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '12px', padding: '8px 16px' }}
          >
            <RefreshCw size={14} className={isLoadingRecs ? 'animate-spin' : ''} /> Refresh
          </button>
        </div>

        {recommendedSongs.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--text-muted)', fontSize: '13px' }}>
            {isLoadingRecs ? 'Finding fresh recommendations...' : 'No additional recommendations found.'}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {recommendedSongs.map((recSong) => {
              const isAdded = addedIds.has(recSong.videoId)
              return (
                <div
                  key={recSong.videoId}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '8px 12px',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--surface-dark)',
                    border: '1px solid var(--border-subtle)',
                    transition: 'background 0.2s ease'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', minWidth: 0, flex: 1 }}>
                    <img
                      src={recSong.thumbnailUrl}
                      alt={recSong.title}
                      style={{ width: '44px', height: '44px', borderRadius: '6px', objectFit: 'cover' }}
                    />
                    <div style={{ minWidth: 0 }}>
                      <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {recSong.title}
                      </div>
                      <div style={{ fontSize: '12px', color: 'var(--text-secondary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {recSong.channelTitle}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginLeft: '12px' }}>
                    <button
                      className="control-btn"
                      onClick={() => onPlaySong(recSong, [recSong, ...playlistSongs])}
                      title="Preview Track"
                      style={{ width: '32px', height: '32px' }}
                    >
                      <Play size={16} fill="var(--text-primary)" />
                    </button>

                    <button
                      onClick={() => !isAdded && handleAddRecommendedSong(recSong)}
                      disabled={isAdded}
                      className={`pill-button ${isAdded ? '' : 'active'}`}
                      style={{
                        padding: '6px 14px',
                        fontSize: '12px',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '6px',
                        borderColor: isAdded ? 'var(--border-subtle)' : '#1db954',
                        background: isAdded ? 'transparent' : '#1db954',
                        color: isAdded ? 'var(--text-muted)' : '#000',
                        fontWeight: 800
                      }}
                    >
                      {isAdded ? 'Added ✓' : <><Plus size={14} /> Add</>}
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
