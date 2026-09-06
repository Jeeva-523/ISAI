import React from 'react'
import type { Song } from '@shared/models/song'

interface SongCardProps {
  song: Song
  isFavorite?: boolean
  onToggleFavorite?: (song: Song) => void
  onPlay?: (song: Song) => void
  variant?: 'square' | 'landscape'
}

export const SongCard: React.FC<SongCardProps> = ({
  song,
  isFavorite = false,
  onToggleFavorite,
  onPlay,
  variant = 'square'
}) => {
  const isLandscape = variant === 'landscape'

  return (
    <div
      className={`song-card ${isLandscape ? 'ytm-video-card' : ''}`}
      title={`Play ${song.title}`}
      onClick={() => onPlay?.(song)}
      style={{ cursor: onPlay ? 'pointer' : 'default' }}
    >
      <div className={isLandscape ? 'ytm-video-thumb-wrap' : 'song-thumbnail-wrap'}>
        <img
          src={song.thumbnailUrl}
          alt={song.title}
          className="song-thumbnail"
          loading="lazy"
          onError={(e) => {
            const target = e.currentTarget
            if (!target.src.includes('hqdefault.jpg')) {
              target.src = `https://img.youtube.com/vi/${song.videoId}/hqdefault.jpg`
            }
          }}
        />
        {!isLandscape && <div className="discovery-badge">ISAI</div>}
        {song.durationFormatted && (
          <div className="duration-badge">{song.durationFormatted}</div>
        )}

        {/* Hover / Active Play Button Overlay */}
        <div className="play-hover-overlay">
          <div className="play-icon-circle">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="#000">
              <path d="M8 5v14l11-7z" />
            </svg>
          </div>
        </div>
      </div>

      <div className="song-info">
        <h3 className="song-title">{song.title}</h3>
        <p className="song-artist">
          {song.channelTitle}
          {song.viewCountFormatted ? ` • ${song.viewCountFormatted}` : ''}
        </p>

        {!isLandscape && (
          <div className="song-meta-row">
            <span className="views-tag">{song.viewCountFormatted || 'Tamil Track'}</span>

            <div className="card-actions">
              {onToggleFavorite && (
                <button
                  className={`action-btn ${isFavorite ? 'active' : ''}`}
                  onClick={(e) => {
                    e.stopPropagation()
                    onToggleFavorite(song)
                  }}
                  title={isFavorite ? 'Remove Favorite' : 'Save to Favorites'}
                >
                  {isFavorite ? '💖' : '🤍'}
                </button>
              )}

              <button
                className="action-btn play-action-btn"
                onClick={(e) => {
                  e.stopPropagation()
                  onPlay?.(song)
                }}
                title="Play song"
              >
                ▶
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
