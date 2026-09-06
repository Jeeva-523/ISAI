import React, { useState } from 'react'
import type { Song } from '@shared/models/song'
import { SearchBar } from '../components/SearchBar'
import { SongListItem } from '../components/SongListItem'
import { GenreTile, type Genre } from '../components/GenreTile'
import { type Artist } from '../components/ArtistCard'
import { SkeletonSongRow } from '../components/SkeletonLoader'
import { Compass, Music } from 'lucide-react'

interface SearchPageProps {
  searchQuery: string
  onSearchChange: (q: string) => void
  onSearchClear: () => void
  searchResults: Song[]
  isSearching: boolean
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong: (song: Song) => void
  onAddToPlaylist?: (song: Song) => void
  onSelectCategory: (q: string) => void
  onSelectArtist?: (artist: Artist) => void
  currentSong?: Song | null
  isPlaying?: boolean
}

const SEARCH_GENRES: Genre[] = [
  { id: 'g1', name: 'Melody & Romance', query: 'Tamil feel good melody hit songs', gradient: 'linear-gradient(135deg, #EC4899, #8B5CF6)', icon: '💖' },
  { id: 'g2', name: 'Mass & Kuthu', query: 'Tamil party kuthu mass songs', gradient: 'linear-gradient(135deg, #F59E0B, #EF4444)', icon: '🔥' },
  { id: 'g3', name: 'Love Hits', query: 'Tamil love romantic hit songs', gradient: 'linear-gradient(135deg, #8B5CF6, #3B82F6)', icon: '🌹' },
  { id: 'g4', name: 'Workout Beats', query: 'Tamil energetic gym workout bgm beats', gradient: 'linear-gradient(135deg, #10B981, #06B6D4)', icon: '⚡' },
  { id: 'g5', name: 'Classical & Devotional', query: 'Tamil god devotional songs', gradient: 'linear-gradient(135deg, #F97316, #EAB308)', icon: '🪔' },
  { id: 'g6', name: 'Gaana & Folk', query: 'Tamil gaana hit songs', gradient: 'linear-gradient(135deg, #84CC16, #10B981)', icon: '🥁' },
  { id: 'g7', name: 'Sad & Emotional', query: 'Tamil sad heartbreak songs', gradient: 'linear-gradient(135deg, #3B82F6, #1E40AF)', icon: '🌧️' },
  { id: 'g8', name: 'Retro & 90s Hits', query: '90s Tamil hit songs Ilaiyaraaja AR Rahman', gradient: 'linear-gradient(135deg, #A855F7, #EC4899)', icon: '📻' }
]

type FilterType = 'all' | 'songs' | 'artists' | 'playlists'

export const SearchPage: React.FC<SearchPageProps> = ({
  searchQuery,
  onSearchChange,
  onSearchClear,
  searchResults,
  isSearching,
  isFavorite,
  onToggleFavorite,
  onPlaySong,
  onAddToPlaylist,
  onSelectCategory,
  currentSong,
  isPlaying = false
}) => {
  const [activeFilter, setActiveFilter] = useState<FilterType>('all')

  return (
    <div style={{ maxWidth: '1300px', margin: '0 auto', paddingBottom: '8px' }}>
      {/* Prominent Search Bar Header */}
      <div style={{ marginBottom: '24px' }}>
        <SearchBar
          value={searchQuery}
          onChange={onSearchChange}
          onClear={onSearchClear}
          placeholder="Search songs, artists, playlists, albums..."
        />
      </div>

      {/* Filter Chips */}
      {searchQuery && (
        <div style={{ display: 'flex', gap: '10px', marginBottom: '24px' }}>
          {(['all', 'songs', 'artists', 'playlists'] as FilterType[]).map((filter) => (
            <button
              key={filter}
              className={`pill-button ${activeFilter === filter ? 'active' : ''}`}
              onClick={() => setActiveFilter(filter)}
            >
              {filter.charAt(0).toUpperCase() + filter.slice(1)}
            </button>
          ))}
        </div>
      )}

      {/* When Empty Query: Render Genre/Mood Grid Tiles */}
      {!searchQuery ? (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px' }}>
            <Compass color="var(--isai-purple-light)" size={24} />
            <h2 style={{ fontSize: '24px', fontWeight: 900 }}>Explore Genres & Moods</h2>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '20px' }}>
            {SEARCH_GENRES.map((genre) => (
              <GenreTile
                key={genre.id}
                genre={genre}
                onSelect={(q) => {
                  onSearchChange(q)
                  onSelectCategory(q)
                }}
              />
            ))}
          </div>
        </div>
      ) : isSearching ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '16px' }}>
          {[...Array(6)].map((_, i) => (
            <SkeletonSongRow key={i} />
          ))}
        </div>
      ) : searchResults.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
          <Music size={48} style={{ margin: '0 auto 16px', opacity: 0.4 }} />
          <h3 style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>No results found for "{searchQuery}"</h3>
          <p style={{ fontSize: '14px', marginTop: '4px' }}>Try searching for song titles, artist names, or movie names.</p>
        </div>
      ) : (
        <div>
          <h2 style={{ fontSize: '20px', fontWeight: 900, marginBottom: '16px' }}>
            Search Results ({searchResults.length})
          </h2>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            {searchResults.map((song, idx) => (
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
      )}
    </div>
  )
}
