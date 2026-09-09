import React from 'react'
import type { Song } from '@shared/models/song'
import { Heart, Plus, Share2, ListPlus, ListStart } from 'lucide-react'

interface SongListItemProps {
  song: Song
  index: number
  isPlaying?: boolean
  isFavorite?: boolean
  onPlay?: (song: Song) => void
  onToggleFavorite?: (song: Song) => void
  onAddToPlaylist?: (song: Song) => void
  onAddToQueue?: (song: Song) => void
  onPlayNext?: (song: Song) => void
  onShare?: (song: Song) => void
}

export const SongListItem: React.FC<SongListItemProps> = ({
  song,
  index,
  isPlaying = false,
  isFavorite = false,
  onPlay,
  onToggleFavorite,
  onAddToPlaylist,
  onAddToQueue,
  onPlayNext,
  onShare
}) => {
  return (
    <div
      className={`song-list-row ${isPlaying ? 'active-playing' : ''}`}
      onClick={() => onPlay?.(song)}
    >
      {/* Index number or Equalizer animation */}
      <div className="song-row-num">
        {isPlaying ? (
          <div className="equalizer-wave">
            <div className="equalizer-bar" />
            <div className="equalizer-bar" />
            <div className="equalizer-bar" />
            <div className="equalizer-bar" />
          </div>
        ) : (
          index + 1
        )}
      </div>

      {/* Cover artwork */}
      <img
        src={song.thumbnailUrl || 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg'}
        alt={song.title}
        className="song-row-art"
        loading="lazy"
        onError={(e) => {
          const target = e.currentTarget
          if (!target.src.includes('Jailer-Tamil-2023')) {
            target.src = 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg'
          }
        }}
      />

      {/* Title & Artist */}
      <div style={{ overflow: 'hidden' }}>
        <div className="song-row-title">{song.title}</div>
        <div className="song-row-artist">{song.channelTitle}</div>
      </div>

      {/* Views / Category Tag */}
      <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
        {song.viewCountFormatted || 'Tamil Hit'}
      </div>

      {/* Duration */}
      <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-muted)' }}>
        {song.durationFormatted || '3:45'}
      </div>

      {/* Action buttons */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'flex-end' }}>
        {onToggleFavorite && (
          <button
            className={`control-btn like-btn ${isFavorite ? 'liked active' : ''}`}
            onClick={(e) => {
              e.stopPropagation()
              onToggleFavorite(song)
            }}
            style={{ color: isFavorite ? '#EC4899' : 'var(--text-muted)' }}
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
            style={{ color: 'var(--text-muted)' }}
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
            style={{ color: 'var(--text-muted)' }}
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
            style={{ color: 'var(--text-muted)' }}
            title="Add to Playlist"
          >
            <Plus size={16} />
          </button>
        )}

        {onShare && (
          <button
            className="control-btn"
            onClick={(e) => {
              e.stopPropagation()
              onShare(song)
            }}
            style={{ color: 'var(--text-muted)' }}
            title="Share Song"
          >
            <Share2 size={16} />
          </button>
        )}
      </div>
    </div>
  )
}
