import { useEffect, useRef, useState } from 'react'

import { musicApi } from '@shared/api/music-api'
import { storageService } from '@shared/services/storageService'
import type { Song, UserPlaylist } from '@shared/models/song'

import { WebPlayer } from './components/WebPlayer'
import { LoginModal, type UserProfile } from './components/LoginModal'
import { MainLayout, type PageTab } from './layouts/MainLayout'
import { HomePage } from './pages/HomePage'
import { LibraryPage } from './pages/LibraryPage'
import { SearchPage } from './pages/SearchPage'
import { LoginPage } from './pages/LoginPage'

// Curated fallback songs for instant offline-first display
const INITIAL_CURATED_SONGS: Song[] = [
  {
    videoId: 'W6L3wM0WnQ8',
    title: 'Hukum - Thalaivar Alappara | Jailer | Rajinikanth | Anirudh',
    channelTitle: 'Anirudh Ravichander',
    thumbnailUrl: 'https://img.youtube.com/vi/W6L3wM0WnQ8/hqdefault.jpg',
    durationFormatted: '3:27',
    durationMs: 207000,
    viewCountFormatted: '120M views'
  },
  {
    videoId: 'Y4u0F6VpY6k',
    title: 'Naa Ready | Leo | Thalapathy Vijay | Anirudh Ravichander',
    channelTitle: 'Anirudh Ravichander',
    thumbnailUrl: 'https://img.youtube.com/vi/Y4u0F6VpY6k/hqdefault.jpg',
    durationFormatted: '4:08',
    durationMs: 248000,
    viewCountFormatted: '180M views'
  },
  {
    videoId: 'mNP7V2sSg-4',
    title: 'Arabic Kuthu - Halamithi Habibo | Beast | Vijay | Anirudh',
    channelTitle: 'Anirudh Ravichander',
    thumbnailUrl: 'https://img.youtube.com/vi/mNP7V2sSg-4/hqdefault.jpg',
    durationFormatted: '4:39',
    durationMs: 279000,
    viewCountFormatted: '340M views'
  },
  {
    videoId: '3tmd-ClpJxA',
    title: 'Marakkuma Nenjam | Vendhu Thanindhathu Kaadu | A.R. Rahman',
    channelTitle: 'A.R. Rahman',
    thumbnailUrl: 'https://img.youtube.com/vi/3tmd-ClpJxA/hqdefault.jpg',
    durationFormatted: '4:16',
    durationMs: 256000,
    viewCountFormatted: '65M views'
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

  const [playbackQueue, setPlaybackQueue] = useState<Song[]>([])
  const [currentQueueIndex, setCurrentQueueIndex] = useState<number>(-1)

  const [user, setUser] = useState<UserProfile>(() => {
    const saved = localStorage.getItem('isai_user_session')
    if (saved) {
      try { return JSON.parse(saved) } catch {}
    }
    return {
      name: 'JEEVA ⚡',
      email: 'jeeva@isaimusic.com',
      avatar: 'J',
      isLoggedIn: true,
      isPremium: true
    }
  })
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false)

  const [userPlaylists, setUserPlaylists] = useState<UserPlaylist[]>(() => storageService.getPlaylists())

  const handleCreatePlaylist = () => {
    const name = window.prompt('Enter playlist name:')
    if (name && name.trim()) {
      storageService.createPlaylist(name.trim())
      setUserPlaylists(storageService.getPlaylists())
    }
  }

  const searchTimerRef = useRef<number | null>(null)

  // Load favorites & trending on mount
  useEffect(() => {
    setFavorites(storageService.getFavorites())
    loadTrending()
  }, [])

  const handleLogin = (userData: Partial<UserProfile>) => {
    const updated: UserProfile = {
      name: userData.name || 'User',
      email: userData.email || 'user@isaimusic.com',
      avatar: userData.avatar || 'U',
      isLoggedIn: true,
      isPremium: true
    }
    setUser(updated)
    localStorage.setItem('isai_user_session', JSON.stringify(updated))
  }

  const handleLogout = () => {
    const loggedOut: UserProfile = {
      name: 'Guest User',
      email: 'guest@isaimusic.com',
      avatar: 'G',
      isLoggedIn: false,
      isPremium: false
    }
    setUser(loggedOut)
    localStorage.setItem('isai_user_session', JSON.stringify(loggedOut))
  }

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

  const handlePlaySong = (song: Song, queue?: Song[]) => {
    setCurrentPlayingSong(song)
    storageService.addRecentlyPlayed(song)

    if (queue && queue.length > 0) {
      setPlaybackQueue(queue)
      const idx = queue.findIndex((s) => s.videoId === song.videoId)
      setCurrentQueueIndex(idx >= 0 ? idx : 0)
    } else {
      setPlaybackQueue([song])
      setCurrentQueueIndex(0)
    }
  }

  const handleNextSong = async () => {
    if (!currentPlayingSong) return

    // 1. Next in queue
    if (playbackQueue.length > 0 && currentQueueIndex >= 0 && currentQueueIndex < playbackQueue.length - 1) {
      const nextIdx = currentQueueIndex + 1
      setCurrentQueueIndex(nextIdx)
      setCurrentPlayingSong(playbackQueue[nextIdx])
      return
    }

    // 2. Auto-fetch related songs by channelTitle/artist or title
    try {
      const artist = currentPlayingSong.channelTitle.replace(/ - Topic| Official/g, '').trim()
      const query = artist && artist !== 'Tamil Artist' ? `${artist} Tamil hit songs` : `${currentPlayingSong.title} Tamil song`
      const related = await musicApi.searchSongs(query)
      const fresh = related.filter((s) => s.videoId !== currentPlayingSong.videoId)
      if (fresh.length > 0) {
        const nextSong = fresh[0]
        setPlaybackQueue(fresh)
        setCurrentQueueIndex(0)
        setCurrentPlayingSong(nextSong)
        return
      }
    } catch (e) {
      console.warn('Failed to fetch next recommendation:', e)
    }

    // 3. Fallback to next song in trending list
    if (trendingSongs.length > 0) {
      const curIdx = trendingSongs.findIndex((s) => s.videoId === currentPlayingSong.videoId)
      const nextIdx = (curIdx + 1) % trendingSongs.length
      setCurrentPlayingSong(trendingSongs[nextIdx])
    }
  }

  const handlePrevSong = () => {
    if (playbackQueue.length > 0 && currentQueueIndex > 0) {
      const prevIdx = currentQueueIndex - 1
      setCurrentQueueIndex(prevIdx)
      setCurrentPlayingSong(playbackQueue[prevIdx])
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
      onOpenLogin={() => setIsLoginModalOpen(true)}
      userName={user.name}
      userAvatar={user.avatar}
      userPlaylists={userPlaylists}
      onCreatePlaylist={handleCreatePlaylist}
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
          onPlaySong={(song) => handlePlaySong(song, trendingSongs)}
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
          onPlaySong={(song) => handlePlaySong(song, searchResults)}
        />
      )}

      {currentTab === 'library' && (
        <LibraryPage
          favorites={favorites}
          onToggleFavorite={handleToggleFavorite}
          onExplore={() => setCurrentTab('home')}
          onPlaySong={(song) => handlePlaySong(song, favorites)}
        />
      )}

      {currentTab === 'login' && (
        <LoginPage
          user={user}
          onLogin={handleLogin}
          onLogout={handleLogout}
          onNavigateHome={() => setCurrentTab('home')}
        />
      )}

      {/* Login Modal Overlay */}
      <LoginModal
        isOpen={isLoginModalOpen}
        onClose={() => setIsLoginModalOpen(false)}
        user={user}
        onLogin={handleLogin}
        onLogout={handleLogout}
      />

      {/* Floating Modern YouTube Player */}
      <WebPlayer
        song={currentPlayingSong}
        onClose={() => setCurrentPlayingSong(null)}
        isFavorite={currentPlayingSong ? isFavorite(currentPlayingSong.videoId) : false}
        onToggleFavorite={handleToggleFavorite}
        onNextSong={handleNextSong}
        onPrevSong={handlePrevSong}
        queue={playbackQueue.length > 0 ? playbackQueue : trendingSongs}
        onSelectQueueItem={(s) => handlePlaySong(s, playbackQueue.length > 0 ? playbackQueue : trendingSongs)}
        userId={user.email || 'user_jeeva_default'}
      />
    </MainLayout>
  )
}

