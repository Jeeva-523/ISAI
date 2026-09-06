import React from 'react'
import { Play } from 'lucide-react'

export interface PlaylistDisplay {
  id: string
  title: string
  subtitle: string
  coverUrl?: string
  gradient?: string
  trackCount?: number
}

interface PlaylistCardProps {
  playlist: PlaylistDisplay
  onSelect: (playlist: PlaylistDisplay) => void
}

export const PlaylistCard: React.FC<PlaylistCardProps> = ({ playlist, onSelect }) => {
  return (
    <div className="song-card" onClick={() => onSelect(playlist)}>
      <div
        className="song-thumbnail-wrap"
        style={{
          background: playlist.gradient || 'linear-gradient(135deg, #8B5CF6, #3B82F6)'
        }}
      >
        {playlist.coverUrl ? (
          <img src={playlist.coverUrl} alt={playlist.title} className="song-thumbnail" loading="lazy" />
        ) : (
          <div
            style={{
              width: '100%',
              height: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '36px'
            }}
          >
            🎧
          </div>
        )}
        <div className="play-hover-overlay">
          <div className="play-icon-circle">
            <Play size={22} fill="#ffffff" style={{ marginLeft: '3px' }} />
          </div>
        </div>
      </div>
      <h3 className="song-title">{playlist.title}</h3>
      <p className="song-artist">{playlist.subtitle}</p>
    </div>
  )
}
