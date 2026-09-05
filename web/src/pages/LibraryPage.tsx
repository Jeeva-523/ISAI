import React from 'react'
import type { Song } from '@shared/models/song'
import { SongCard } from '../components/SongCard'
import { EmptyState } from '../components/EmptyState'

interface LibraryPageProps {
  favorites: Song[]
  onToggleFavorite: (song: Song) => void
  onExplore: () => void
  onPlaySong?: (song: Song) => void
}

export const LibraryPage: React.FC<LibraryPageProps> = ({
  favorites,
  onToggleFavorite,
  onExplore,
  onPlaySong
}) => {
  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 800, marginBottom: '6px' }}>
          Your <span style={{ color: 'var(--neon-cyan)' }}>Favorites</span>
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
          Discovered Tamil songs saved to your local collection ({favorites.length} tracks)
        </p>
      </div>

      {favorites.length === 0 ? (
        <EmptyState
          icon="💖"
          title="No Favorite Songs Saved Yet"
          description="Click the heart icon on any song card while exploring or searching to save your favorite Tamil songs here!"
          suggestions={['Explore Trending Tamil Songs']}
          onSuggestionClick={onExplore}
        />
      ) : (
        <div className="song-grid">
          {favorites.map((song) => (
            <SongCard
              key={song.videoId}
              song={song}
              isFavorite={true}
              onToggleFavorite={onToggleFavorite}
              onPlay={onPlaySong}
            />
          ))}
        </div>
      )}
    </div>
  )
}
