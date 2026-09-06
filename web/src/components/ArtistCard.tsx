import React, { useState } from 'react'
import { Play } from 'lucide-react'

export interface Artist {
  name: string
  role?: string
  image: string
  query: string
  followers?: string
}

interface ArtistCardProps {
  artist: Artist
  onSelectArtist: (artist: Artist) => void
}

export const ArtistCard: React.FC<ArtistCardProps> = ({ artist, onSelectArtist }) => {
  const [imgError, setImgError] = useState(false)

  const initial = artist.name ? artist.name.charAt(0).toUpperCase() : 'A'

  return (
    <div className="artist-card" onClick={() => onSelectArtist(artist)}>
      <div className="artist-avatar-wrap">
        {!imgError ? (
          <img
            src={artist.image}
            alt={artist.name}
            className="artist-avatar"
            loading="lazy"
            referrerPolicy="no-referrer"
            onError={() => setImgError(true)}
          />
        ) : (
          <div
            style={{
              width: '100%',
              height: '100%',
              background: 'var(--isai-gradient)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '28px',
              fontWeight: 900,
              color: '#fff',
              boxShadow: '0 4px 14px rgba(139, 92, 246, 0.4)'
            }}
          >
            {initial}
          </div>
        )}
        <div className="play-hover-overlay">
          <div className="play-icon-circle">
            <Play size={20} fill="#ffffff" style={{ marginLeft: '2px' }} />
          </div>
        </div>
      </div>
      <h4 className="artist-name">{artist.name}</h4>
      <p className="artist-role">{artist.role || 'Music Artist'}</p>
    </div>
  )
}
