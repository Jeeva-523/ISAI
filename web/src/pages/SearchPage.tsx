import React from 'react'
import type { Song } from '@shared/models/song'
import { QUICK_SEARCH_QUERIES } from '@shared/constants/categories'
import { CategoryChips } from '../components/CategoryChips'
import { SongCard } from '../components/SongCard'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'

interface SearchPageProps {
  query: string
  results: Song[]
  isLoading: boolean
  error: string | null
  onQueryChange: (q: string) => void
  onRetry: () => void
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong?: (song: Song) => void
}

export const SearchPage: React.FC<SearchPageProps> = ({
  query,
  results,
  isLoading,
  error,
  onQueryChange,
  onRetry,
  isFavorite,
  onToggleFavorite,
  onPlaySong
}) => {
  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 800, marginBottom: '6px' }}>
          Tamil Music <span style={{ color: 'var(--neon-cyan)' }}>Song Discovery</span>
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
          Search songs, singers, composers, or movies
        </p>
      </div>

      {/* Quick Search Queries */}
      <CategoryChips
        selectedQuery={query}
        onSelect={(q) => onQueryChange(q)}
      />

      {/* State Renderers */}
      {isLoading ? (
        <LoadingSpinner
          message="Discovering Tamil songs..."
          subMessage={query ? `Searching "${query}"` : undefined}
        />
      ) : error ? (
        <ErrorBanner
          title="Search Error"
          message={error}
          onRetry={onRetry}
        />
      ) : query && results.length === 0 ? (
        <EmptyState
          icon="🔍"
          title="No Tamil Songs Found"
          description={`We couldn't find any songs matching "${query}". Please check the spelling or search another Tamil singer, movie, or song title.`}
          suggestions={QUICK_SEARCH_QUERIES.slice(0, 4)}
          onSuggestionClick={(s) => onQueryChange(s)}
        />
      ) : results.length > 0 ? (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <div>
              <h2 style={{ fontSize: '18px', fontWeight: 700 }}>
                Discovered Songs ({results.length})
              </h2>
              <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                ISAI HD Music • Song details & info
              </p>
            </div>
          </div>

          <div className="song-grid">
            {results.map((song) => (
              <SongCard
                key={song.videoId}
                song={song}
                isFavorite={isFavorite(song.videoId)}
                onToggleFavorite={onToggleFavorite}
                onPlay={onPlaySong}
              />
            ))}
          </div>
        </div>
      ) : (
        <EmptyState
          icon="🎵"
          title="Search Your Favorite Tamil Songs"
          description="Type a Tamil song title, singer name, or click any chip above to discover songs instantly."
          suggestions={QUICK_SEARCH_QUERIES.slice(0, 4)}
          onSuggestionClick={(s) => onQueryChange(s)}
        />
      )}
    </div>
  )
}
