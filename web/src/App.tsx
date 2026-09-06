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
    videoId: 'KUN5Uf9mObQ',
    title: 'Arabic Kuthu - Halamithi Habibo | Beast | Vijay | Anirudh',
    channelTitle: 'Sun TV • Anirudh Ravichander',
    thumbnailUrl: 'https://img.youtube.com/vi/KUN5Uf9mObQ/hqdefault.jpg',
    durationFormatted: '4:39',
    durationMs: 279000,
    viewCountFormatted: '480M views'
  },
  {
    videoId: '1F3hm6MfR1k',
    title: 'Hukum - Thalaivar Alappara | Jailer | Rajinikanth | Anirudh',
    channelTitle: 'Sun TV • Anirudh Ravichander',
    thumbnailUrl: 'https://img.youtube.com/vi/1F3hm6MfR1k/hqdefault.jpg',
    durationFormatted: '3:26',
    durationMs: 206000,
    viewCountFormatted: '185M views'
  },
  {
    videoId: 'szvt1vD0Uug',
    title: 'Naa Ready | Leo | Thalapathy Vijay | Anirudh Ravichander',
    channelTitle: 'Sony Music South • Anirudh',
    thumbnailUrl: 'https://img.youtube.com/vi/szvt1vD0Uug/hqdefault.jpg',
    durationFormatted: '4:08',
    durationMs: 248000,
    viewCountFormatted: '240M views'
  },
  {
    videoId: '3tmd-ClpJxA',
    title: 'Marakkuma Nenjam | Vendhu Thanindhathu Kaadu | A.R. Rahman',
    channelTitle: 'Think Music India • A.R. Rahman',
    thumbnailUrl: 'https://img.youtube.com/vi/3tmd-ClpJxA/hqdefault.jpg',
    durationFormatted: '4:16',
    durationMs: 256000,
    viewCountFormatted: '65M views'
  },
  {
    videoId: 'mqqft2x_Aa4',
    title: 'Kaavaalaa - Jailer | Rajinikanth | Tamannaah | Anirudh',
    channelTitle: 'Sun TV • Anirudh Ravichander',
    thumbnailUrl: 'https://img.youtube.com/vi/mqqft2x_Aa4/hqdefault.jpg',
    durationFormatted: '3:10',
    durationMs: 190000,
    viewCountFormatted: '290M views'
  },
  {
    videoId: 'eN6AnYGYdVE',
    title: 'Vaseegara - Minnale | Bombay Jayashri | Harris Jayaraj',
    channelTitle: 'Harris Jayaraj Melodies',
    thumbnailUrl: 'https://img.youtube.com/vi/eN6AnYGYdVE/hqdefault.jpg',
    durationFormatted: '5:00',
    durationMs: 300000,
    viewCountFormatted: '85M views'
  },
  {
    videoId: 'jHNNMj5bNQw',
    title: 'Rowdy Baby - Maari 2 | Dhanush | Sai Pallavi | Yuvan',
    channelTitle: 'Wunderbar Films • Yuvan Shankar Raja',
    thumbnailUrl: 'https://img.youtube.com/vi/jHNNMj5bNQw/hqdefault.jpg',
    durationFormatted: '4:44',
    durationMs: 284000,
    viewCountFormatted: '1.5B views'
  },
  {
    videoId: 'x6Q7c9Ry3tk',
    title: 'Why This Kolaveri Di - 3 | Dhanush | Anirudh',
    channelTitle: 'Sony Music South • Anirudh',
    thumbnailUrl: 'https://img.youtube.com/vi/x6Q7c9Ry3tk/hqdefault.jpg',
    durationFormatted: '4:05',
    durationMs: 245000,
    viewCountFormatted: '400M views'
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
    setTrendingError(null)
    try {
      const fetchPromise = musicApi.getTrending()
      const timeoutPromise = new Promise<Song[]>((_, reject) =>
        setTimeout(() => reject(new Error('Timeout')), 3000)
      )
      const songs = await Promise.race([fetchPromise, timeoutPromise])
      if (songs && songs.length > 0) {
        setTrendingSongs(songs)
      }
    } catch {
      // Retain instant curated songs without error
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

  const SPECIAL_KEYWORDS: Record<string, { queryOverride?: string; useTrending?: boolean }> = {
    'trending': { useTrending: true },
    'trend': { useTrending: true },
    'trendin': { useTrending: true },
    'top trending': { useTrending: true },
    'charts': { useTrending: true },
    'top charts': { useTrending: true },
    'melody': { queryOverride: 'Tamil feel good melody hit songs' },
    'melodies': { queryOverride: 'Tamil feel good melody hit songs' },
    'romantic': { queryOverride: 'Tamil love romantic hit songs' },
    'romance': { queryOverride: 'Tamil love romantic hit songs' },
    'kadhal': { queryOverride: 'Tamil love romantic hit songs' },
    'sad': { queryOverride: 'Tamil sad emotional songs' },
    'kuthu': { queryOverride: 'Tamil party kuthu mass songs' },
    'party': { queryOverride: 'Tamil party kuthu mass songs' },
    'workout': { queryOverride: 'Tamil energetic gym workout bgm beats' },
    'energize': { queryOverride: 'Tamil energetic gym workout bgm beats' },
    'relax': { queryOverride: 'Tamil relaxing acoustic melody songs' },
    'commute': { queryOverride: 'Tamil travel songs' },
    'folk': { queryOverride: 'Tamil folk village songs' },
    'devotional': { queryOverride: 'Tamil god devotional songs' },
    'gaana': { queryOverride: 'Tamil gaana hit songs' }
  }

  const getSpecialKeywordMatch = (q: string) => {
    const clean = q.trim().toLowerCase()
    if (!clean) return null

    for (const [key, config] of Object.entries(SPECIAL_KEYWORDS)) {
      if (clean === key || key.startsWith(clean) || clean.startsWith(key)) {
        return config
      }
    }
    return null
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

    // Debounce 350ms to preserve quota & responsiveness
    searchTimerRef.current = window.setTimeout(async () => {
      try {
        const specialMatch = getSpecialKeywordMatch(query)

        if (specialMatch?.useTrending) {
          setSearchResults(trendingSongs)
        } else {
          const apiQuery = specialMatch?.queryOverride || query
          const results = await musicApi.searchSongs(apiQuery)
          setSearchResults(results)
        }
        setSearchError(null)
      } catch (error: any) {
        console.error('Search failed', error)
        setSearchResults([])
        setSearchError(error?.message || 'Failed to search songs. Please verify network connection.')
      } finally {
        setIsSearching(false)
      }
    }, 350)
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

