import React from 'react'
import type { Song } from '@shared/models/song'
import { SongListItem } from '../components/SongListItem'
import { Play, Shuffle, ArrowLeft, Music, Clock } from 'lucide-react'

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
  onPlaySong: (song: Song) => void
  onAddToPlaylist: (song: Song) => void
  onBack: () => void
}

export const PlaylistDetailPage: React.FC<PlaylistDetailPageProps> = ({
  title,
  subtitle,
  coverUrl,
  gradient = 'linear-gradient(135deg, #8B5CF6 0%, #D946EF 100%)',
  songs,
  currentSong,
  isPlaying,
  isFavorite,
  onToggleFavorite,
  onPlaySong,
  onAddToPlaylist,
  onBack
}) => {
  const totalDurationMin = Math.round(songs.reduce((acc, s) => acc + (s.durationMs || 200000), 0) / 60000)

  return (
    <div style={{ maxWidth: '1300px', margin: '0 auto', paddingBottom: '8px' }}>
      {/* Back button */}
      <button
        onClick={onBack}
        className="pill-button"
        style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', marginBottom: '24px' }}
      >
        <ArrowLeft size={16} /> Back
      </button>

      {/* Hero Playlist Header */}
      <div
        style={{
          display: 'flex',
          gap: '32px',
          alignItems: 'flex-end',
          padding: '36px',
          borderRadius: 'var(--radius-xl)',
          background: `linear-gradient(to bottom, rgba(139, 92, 246, 0.25), var(--surface-card))`,
          border: '1px solid var(--border-subtle)',
          marginBottom: '32px',
          flexWrap: 'wrap'
        }}
      >
        <div
          style={{
            width: '200px',
            height: '200px',
            borderRadius: 'var(--radius-lg)',
            overflow: 'hidden',
            boxShadow: '0 12px 36px rgba(0, 0, 0, 0.6)',
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
            <Music size={72} color="#fff" />
          )}
        </div>

        <div>
          <span style={{ fontSize: '11px', fontWeight: 900, color: 'var(--isai-purple-light)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            PLAYLIST
          </span>
          <h1 style={{ fontSize: '38px', fontWeight: 900, color: '#fff', margin: '4px 0 8px' }}>
            {title}
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
            {subtitle} • {songs.length} tracks, ~{totalDurationMin} mins
          </p>

          <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
            <button
              className="pill-button active"
              onClick={() => songs.length > 0 && onPlaySong(songs[0])}
              style={{ padding: '12px 28px', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Play size={18} fill="#fff" /> Play All
            </button>

            <button
              className="pill-button"
              onClick={() => {
                if (songs.length > 0) {
                  const randomSong = songs[Math.floor(Math.random() * songs.length)]
                  onPlaySong(randomSong)
                }
              }}
              style={{ padding: '12px 20px', display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <Shuffle size={18} /> Shuffle
            </button>
          </div>
        </div>
      </div>

      {/* Track List */}
      <div>
        <div style={{ display: 'grid', gridTemplateColumns: '40px 52px 1fr 140px 80px 100px', gap: '14px', padding: '0 16px 12px', fontSize: '12px', fontWeight: 800, color: 'var(--text-muted)', borderBottom: '1px solid var(--border-subtle)', marginBottom: '8px' }}>
          <div style={{ textAlign: 'center' }}>#</div>
          <div>ART</div>
          <div>TITLE</div>
          <div>TAG</div>
          <div><Clock size={14} /></div>
          <div style={{ textAlign: 'right' }}>ACTIONS</div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          {songs.map((song, idx) => (
            <SongListItem
              key={song.videoId}
              song={song}
              index={idx}
              isPlaying={currentSong?.videoId === song.videoId && isPlaying}
              isFavorite={isFavorite(song.videoId)}
              onPlay={onPlaySong}
              onToggleFavorite={onToggleFavorite}
              onAddToPlaylist={onAddToPlaylist}
            />
          ))}
        </div>
      </div>
    </div>
  )
}
