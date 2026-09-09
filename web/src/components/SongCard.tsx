import React from 'react'
import type { Song } from '@shared/models/song'
import { Play, Heart, Plus, ListPlus, ListStart } from 'lucide-react'

interface SongCardProps {
  song: Song
  isPlaying?: boolean
  isFavorite?: boolean
  onToggleFavorite?: (song: Song) => void
  onPlay?: (song: Song) => void
  onAddToPlaylist?: (song: Song) => void
  onAddToQueue?: (song: Song) => void
  onPlayNext?: (song: Song) => void
  variant?: 'square' | 'landscape'
}

export const SongCard: React.FC<SongCardProps> = ({
  song,
  isPlaying = false,
  isFavorite = false,
  onToggleFavorite,
  onPlay,
  onAddToPlaylist,
  onAddToQueue,
  onPlayNext,
  variant = 'square'
}) => {
  const isLandscape = variant === 'landscape'

  return (
    <div
      className={`song-card ${isLandscape ? 'ytm-video-card' : ''} ${isPlaying ? 'active-playing-card' : ''}`}
      title={`Play ${song.title}`}
      onClick={() => onPlay?.(song)}
    >
      <div className="song-thumbnail-wrap">
        <img
          src={song.thumbnailUrl || 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg'}
          alt={song.title}
          className="song-thumbnail"
          loading="lazy"
          onError={(e) => {
            const target = e.currentTarget
            if (!target.src.includes('Jailer-Tamil-2023')) {
              target.src = 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg'
            }
          }}
        />
        <div className="discovery-badge">ISAI</div>
        {song.durationFormatted && (
          <div className="duration-badge">{song.durationFormatted}</div>
        )}

        {/* Hover / Active Play Button Overlay */}
        <div className="play-hover-overlay" style={{ opacity: isPlaying ? 1 : undefined }}>
          {isPlaying ? (
            <div className="equalizer-wave">
              <div className="equalizer-bar" />
              <div className="equalizer-bar" />
              <div className="equalizer-bar" />
              <div className="equalizer-bar" />
            </div>
          ) : (
            <div className="play-icon-circle">
              <Play size={22} fill="#ffffff" style={{ marginLeft: '3px' }} />
            </div>
          )}
        </div>
      </div>

      <div className="song-info">
        <h3 className="song-title">{song.title}</h3>
        <p className="song-artist">
          {song.channelTitle}
          {song.viewCountFormatted ? ` • ${song.viewCountFormatted}` : ''}
        </p>

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '8px' }}>
          <span style={{ fontSize: '11px', color: 'var(--text-muted)', fontWeight: 600 }}>
            {song.viewCountFormatted || 'Tamil Hit'}
          </span>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            {onToggleFavorite && (
              <button
                className={`control-btn like-btn ${isFavorite ? 'liked active' : ''}`}
                onClick={(e) => {
                  e.stopPropagation()
                  onToggleFavorite(song)
                }}
                style={{
                  color: isFavorite ? '#EC4899' : 'var(--text-muted)',
                  padding: '4px'
                }}
                title={isFavorite ? 'Remove Favorite' : 'Save to Favorites'}
              >
                <Heart
                  size={16}
                  color={isFavorite ? '#EC4899' : 'currentColor'}
                  fill={isFavorite ? '#EC4899' : 'none'}
                  style={{
                    filter: isFavorite ? 'drop-shadow(0 0 5px rgba(236, 72, 153, 0.75))' : 'none',
                    transition: 'all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275)'
                  }}
                />
              </button>
            )}

            {onPlayNext && (
              <button
                className="control-btn"
                onClick={(e) => {
                  e.stopPropagation()
                  onPlayNext(song)
                }}
                style={{ color: 'var(--text-muted)', padding: '4px' }}
                title="Play Next"
              >
                <ListStart size={16} />
              </button>
            )}

            {onAddToQueue && (
              <button
                className="control-btn"
                onClick={(e) => {
                  e.stopPropagation()
                  onAddToQueue(song)
                }}
                style={{ color: 'var(--text-muted)', padding: '4px' }}
                title="Add to Queue"
              >
                <ListPlus size={16} />
              </button>
            )}

            {onAddToPlaylist && (
              <button
                className="control-btn"
                onClick={(e) => {
                  e.stopPropagation()
                  onAddToPlaylist(song)
                }}
                style={{ color: 'var(--text-muted)', padding: '4px' }}
                title="Add to Playlist"
              >
                <Plus size={16} />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
