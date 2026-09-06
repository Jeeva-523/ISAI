import React from 'react'
import type { Song } from '@shared/models/song'
import type { Artist } from '../components/ArtistCard'
import { SongListItem } from '../components/SongListItem'
import { Play, CheckCircle2, UserPlus, ArrowLeft } from 'lucide-react'

interface ArtistDetailPageProps {
  artist: Artist
  artistSongs: Song[]
  currentSong: Song | null
  isPlaying: boolean
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong: (song: Song) => void
  onAddToPlaylist: (song: Song) => void
  onBack: () => void
}

export const ArtistDetailPage: React.FC<ArtistDetailPageProps> = ({
  artist,
  artistSongs,
  currentSong,
  isPlaying,
  isFavorite,
  onToggleFavorite,
  onPlaySong,
  onAddToPlaylist,
  onBack
}) => {
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

      {/* Hero Banner */}
      <div
        style={{
          position: 'relative',
          borderRadius: 'var(--radius-xl)',
          overflow: 'hidden',
          minHeight: '280px',
          background: `linear-gradient(to top, rgba(9, 7, 15, 0.95), rgba(9, 7, 15, 0.3)), url(${artist.image}) center/cover no-repeat`,
          display: 'flex',
          alignItems: 'flex-end',
          padding: '40px',
          marginBottom: '36px',
          border: '1px solid var(--border-subtle)',
          boxShadow: '0 12px 40px rgba(0, 0, 0, 0.6)'
        }}
      >
        <div style={{ zIndex: 2, display: 'flex', gap: '28px', alignItems: 'center', flexWrap: 'wrap' }}>
          <img
            src={artist.image}
            alt={artist.name}
            referrerPolicy="no-referrer"
            style={{
              width: '140px',
              height: '140px',
              borderRadius: '50%',
              objectFit: 'cover',
              border: '4px solid rgba(139, 92, 246, 0.6)',
              boxShadow: '0 8px 30px rgba(0, 0, 0, 0.8)'
            }}
          />

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--isai-purple-light)', fontSize: '12px', fontWeight: 800 }}>
              <CheckCircle2 size={16} /> VERIFIED ARTIST
            </div>
            <h1 style={{ fontSize: '42px', fontWeight: 900, color: '#fff', margin: '4px 0 8px' }}>
              {artist.name}
            </h1>
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '16px' }}>
              {artist.role || 'Composer & Singer'} • {artist.followers || '18.4M Monthly Listeners'}
            </p>

            <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
              <button
                className="pill-button active"
                onClick={() => artistSongs.length > 0 && onPlaySong(artistSongs[0])}
                style={{ padding: '12px 28px', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}
              >
                <Play size={18} fill="#fff" /> Play Popular
              </button>

              <button
                className="pill-button"
                style={{ padding: '12px 20px', display: 'flex', alignItems: 'center', gap: '8px' }}
              >
                <UserPlus size={18} /> Follow
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Popular Songs Section */}
      <div style={{ marginBottom: '40px' }}>
        <h2 style={{ fontSize: '22px', fontWeight: 900, marginBottom: '16px' }}>
          Popular Tracks
        </h2>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          {artistSongs.slice(0, 8).map((song, idx) => (
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
