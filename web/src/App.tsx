import { useEffect, useRef, useState } from 'react'

import { musicApi } from '@shared/api/music-api'
import { storageService } from '@shared/services/storageService'
import type { Song } from '@shared/models/song'

import { WebPlayer } from './components/WebPlayer'
import { MainLayout, type PageTab } from './layouts/MainLayout'
import { HomePage } from './pages/HomePage'
import { LibraryPage } from './pages/LibraryPage'
import { SearchPage } from './pages/SearchPage'

// Curated fallback songs for instant offline-first display
const INITIAL_CURATED_SONGS: Song[] = [
  {
    videoId: 'kJQP7kiw5Fk',
    title: 'Luis Fonsi - Despacito ft. Daddy Yankee',
    channelTitle: 'Luis Fonsi',
    thumbnailUrl: 'https://img.youtube.com/vi/kJQP7kiw5Fk/hqdefault.jpg',
    durationFormatted: '4:42',
    durationMs: 282000,
    viewCountFormatted: '8.2B views'
  },
  {
    videoId: '2Vv-BfVoq4g',
    title: 'Ed Sheeran - Perfect',
    channelTitle: 'Ed Sheeran',
    thumbnailUrl: 'https://img.youtube.com/vi/2Vv-BfVoq4g/hqdefault.jpg',
    durationFormatted: '4:39',
    durationMs: 279000,
    viewCountFormatted: '3.6B views'
  },
  {
    videoId: 'OPf0YbXqDm0',
    title: 'Mark Ronson - Uptown Funk ft. Bruno Mars',
    channelTitle: 'Mark Ronson',
    thumbnailUrl: 'https://img.youtube.com/vi/OPf0YbXqDm0/hqdefault.jpg',
    durationFormatted: '4:30',
    durationMs: 270000,
    viewCountFormatted: '4.8B views'
  },
  {
    videoId: 'JGwWNGJdvx8',
    title: 'Ed Sheeran - Shape of You',
    channelTitle: 'Ed Sheeran',
    thumbnailUrl: 'https://img.youtube.com/vi/JGwWNGJdvx8/hqdefault.jpg',
    durationFormatted: '4:23',
    durationMs: 263000,
    viewCountFormatted: '6.1B views'
  }
]

export function App() {
  const [currentTab, setCurrentTab] = useState<PageTab>('home')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<Song[]>([])
  const [isSearching, setIsSearching] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)

  const [trendingSongs, setTrendingSongs] = useState<Song[]>(INITIAL_CURATED_SONGS)
  const [isTrendingLoading, setIsTrendingLoading] = useState(false)
  const [trendingError, setTrendingError] = useState<string | null>(null)

  const [favorites, setFavorites] = useState<Song[]>([])
  const [currentPlayingSong, setCurrentPlayingSong] = useState<Song | null>(null)

  const searchTimerRef = useRef<number | null>(null)

  // Load favorites & trending on mount
  useEffect(() => {
    setFavorites(storageService.getFavorites())
    loadTrending()
  }, [])

  const loadTrending = async () => {
    setIsTrendingLoading(true)
    setTrendingError(null)
    try {
      const songs = await musicApi.getTrending()
      if (songs.length > 0) {
        setTrendingSongs(songs)
      }
    } catch {
      // Keep curated fallback if backend is starting
      setTrendingSongs(INITIAL_CURATED_SONGS)
    } finally {
      setIsTrendingLoading(false)
    }
  }

  const handleSearchChange = (query: string) => {
    setSearchQuery(query)
    if (searchTimerRef.current) {
      window.clearTimeout(searchTimerRef.current)
    }

    if (!query.trim()) {
      setSearchResults([])
      setIsSearching(false)
      setSearchError(null)
      return
    }

    setIsSearching(true)
    setSearchError(null)

    // Debounce 400ms to preserve quota
    searchTimerRef.current = window.setTimeout(async () => {
      try {
        const results = await musicApi.searchSongs(query)
        setSearchResults(results)
        setSearchError(null)
      } catch (error: any) {
        console.error('Search failed', error)
        setSearchResults([])
        setSearchError(error?.message || 'Failed to search songs. Please verify network connection.')
      } finally {
        setIsSearching(false)
      }
    }, 400)
  }

  const handleCategorySelect = (query: string) => {
    setSearchQuery(query)
    setCurrentTab('search')
    handleSearchChange(query)
  }

  const handleToggleFavorite = (song: Song) => {
    storageService.toggleFavorite(song)
    setFavorites(storageService.getFavorites())
  }

  const isFavorite = (videoId: string) => {
    return favorites.some((s) => s.videoId === videoId)
  }

  return (
    <MainLayout
      currentTab={currentTab}
      onSelectTab={setCurrentTab}
      searchQuery={searchQuery}
      onSearchChange={handleSearchChange}
      onSearchClear={() => handleSearchChange('')}
    >
      {currentTab === 'home' && (
        <HomePage
          trendingSongs={trendingSongs}
          isLoading={isTrendingLoading}
          error={trendingError}
          onSelectCategory={handleCategorySelect}
          onRetry={loadTrending}
          isFavorite={isFavorite}
          onToggleFavorite={handleToggleFavorite}
          onPlaySong={setCurrentPlayingSong}
        />
      )}

      {currentTab === 'search' && (
        <SearchPage
          query={searchQuery}
          results={searchResults}
          isLoading={isSearching}
          error={searchError}
          onQueryChange={handleSearchChange}
          onRetry={() => handleSearchChange(searchQuery)}
          isFavorite={isFavorite}
          onToggleFavorite={handleToggleFavorite}
          onPlaySong={setCurrentPlayingSong}
        />
      )}

      {currentTab === 'library' && (
        <LibraryPage
          favorites={favorites}
          onToggleFavorite={handleToggleFavorite}
          onExplore={() => setCurrentTab('home')}
          onPlaySong={setCurrentPlayingSong}
        />
      )}

      {/* Floating Modern YouTube Player */}
      <WebPlayer
        song={currentPlayingSong}
        onClose={() => setCurrentPlayingSong(null)}
        isFavorite={currentPlayingSong ? isFavorite(currentPlayingSong.videoId) : false}
        onToggleFavorite={handleToggleFavorite}
      />
    </MainLayout>
  )
}
