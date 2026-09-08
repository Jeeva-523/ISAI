import { useEffect, useRef, useState } from 'react'

import { musicApi } from '@shared/api/music-api'
import { storageService } from '@shared/services/storageService'
import type { Song, UserPlaylist } from '@shared/models/song'

import { WebPlayer } from './components/WebPlayer'
import { type UserProfile } from './components/LoginModal'
import { MainLayout, type PageTab } from './layouts/MainLayout'
import { HomePage } from './pages/HomePage'
import { LibraryPage } from './pages/LibraryPage'
import { SearchPage } from './pages/SearchPage'
import { ProfilePage } from './pages/ProfilePage'
import { LoginPage } from './pages/LoginPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'
import { ArtistDetailPage } from './pages/ArtistDetailPage'
import { PlaylistDetailPage } from './pages/PlaylistDetailPage'
import { PlaylistModal } from './components/PlaylistModal'
import { Toast, type ToastMessage } from './components/Toast'
import { type Artist } from './components/ArtistCard'
import { IsaiConnectService, PlaybackStateSync, DeviceInfo } from './services/IsaiConnectService'

// Curated fallback songs with direct 320kbps audio streams
const INITIAL_CURATED_SONGS: Song[] = [
  {
    videoId: 'KUN5Uf9mObQ',
    title: 'Arabic Kuthu - Halamithi Habibo | Beast | Vijay | Anirudh',
    channelTitle: 'Sun TV • Anirudh Ravichander',
    thumbnailUrl: 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
    durationFormatted: '4:39',
    durationMs: 279000,
    viewCountFormatted: '480M views',
    audioUrl: 'https://aac.saavncdn.com/452/59ccb79c00c58a64ba74495045901025_320.mp4'
  },
  {
    videoId: '1F3hm6MfR1k',
    title: 'Hukum - Thalaivar Alappara | Jailer | Rajinikanth | Anirudh',
    channelTitle: 'Sun TV • Anirudh Ravichander',
    thumbnailUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
    durationFormatted: '3:26',
    durationMs: 206000,
    viewCountFormatted: '185M views',
    audioUrl: 'https://aac.saavncdn.com/187/0c4d0aee91a3ac81d4b645ec448a2960_320.mp4'
  },
  {
    videoId: 'szvt1vD0Uug',
    title: 'Naa Ready | Leo | Thalapathy Vijay | Anirudh Ravichander',
    channelTitle: 'Sony Music South • Anirudh',
    thumbnailUrl: 'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
    durationFormatted: '4:08',
    durationMs: 248000,
    viewCountFormatted: '240M views',
    audioUrl: 'https://aac.saavncdn.com/415/3789bee89b94522160f1e50b2266d2c4_320.mp4'
  },
  {
    videoId: '3tmd-ClpJxA',
    title: 'Marakkuma Nenjam | Vendhu Thanindhathu Kaadu | A.R. Rahman',
    channelTitle: 'Think Music India • A.R. Rahman',
    thumbnailUrl: 'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
    durationFormatted: '4:16',
    durationMs: 256000,
    viewCountFormatted: '65M views',
    audioUrl: 'https://aac.saavncdn.com/420/14cb0983229d366c09447dbfb731f351_320.mp4'
  },
  {
    videoId: 'mqqft2x_Aa4',
    title: 'Kaavaalaa - Jailer | Rajinikanth | Tamannaah | Anirudh',
    channelTitle: 'Sun TV • Anirudh Ravichander',
    thumbnailUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
    durationFormatted: '3:10',
    durationMs: 190000,
    viewCountFormatted: '290M views',
    audioUrl: 'https://aac.saavncdn.com/187/49797372d021638077d8a6b749068bc8_320.mp4'
  },
  {
    videoId: 'eN6AnYGYdVE',
    title: 'Vaseegara - Minnale | Bombay Jayashri | Harris Jayaraj',
    channelTitle: 'Harris Jayaraj Melodies',
    thumbnailUrl: 'https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg',
    durationFormatted: '5:00',
    durationMs: 300000,
    viewCountFormatted: '85M views',
    audioUrl: 'https://aac.saavncdn.com/450/4f7b9da8e887586e60b11afb602befac_320.mp4'
  },
  {
    videoId: 'jHNNMj5bNQw',
    title: 'Rowdy Baby - Maari 2 | Dhanush | Sai Pallavi | Yuvan',
    channelTitle: 'Wunderbar Films • Yuvan Shankar Raja',
    thumbnailUrl: 'https://c.saavncdn.com/276/Maari-2-Tamil-2018-20260203193952-500x500.jpg',
    durationFormatted: '4:44',
    durationMs: 284000,
    viewCountFormatted: '1.5B views',
    audioUrl: 'https://aac.saavncdn.com/276/64b835b4e1829992f8d35f54d6dad5f3_320.mp4'
  },
  {
    videoId: 'x6Q7c9Ry3tk',
    title: 'Why This Kolaveri Di - 3 | Dhanush | Anirudh',
    channelTitle: 'Sony Music South • Anirudh',
    thumbnailUrl: 'https://c.saavncdn.com/932/3-Hindi-2012-500x500.jpg',
    durationFormatted: '4:05',
    durationMs: 245000,
    viewCountFormatted: '400M views',
    audioUrl: 'https://aac.saavncdn.com/932/7cf7f8a9d9c3faa2633d1605e97ba4a5_320.mp4'
  }
]

export function App() {
  const [currentTab, setCurrentTab] = useState<PageTab>('home')
  const [user, setUser] = useState<UserProfile>(() => storageService.getUserProfile())

  // Music state
  const [trendingSongs, setTrendingSongs] = useState<Song[]>(INITIAL_CURATED_SONGS)
  const [isTrendingLoading, setIsTrendingLoading] = useState(false)
  const [trendingError, setTrendingError] = useState<string | null>(null)

  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<Song[]>([])
  const [isSearching, setIsSearching] = useState(false)

  const [favorites, setFavorites] = useState<Song[]>(() => storageService.getFavorites())
  const [userPlaylists, setUserPlaylists] = useState<UserPlaylist[]>(() => storageService.getPlaylists())

  const [currentPlayingSong, setCurrentPlayingSong] = useState<Song | null>(null)
  const [playbackQueue, setPlaybackQueue] = useState<Song[]>([])
  const [currentQueueIndex, setCurrentQueueIndex] = useState(0)

  // Selected Artist / Playlist Detail State
  const [selectedArtist, setSelectedArtist] = useState<Artist | null>(null)
  const [selectedPlaylistDetail, setSelectedPlaylistDetail] = useState<{
    title: string
    subtitle: string
    songs: Song[]
    coverUrl?: string
  } | null>(null)

  // Modals & Toast State
  const [isPlaylistModalOpen, setIsPlaylistModalOpen] = useState(false)
  const [songToAddToPlaylist, setSongToAddToPlaylist] = useState<Song | null>(null)
  const [toast, setToast] = useState<ToastMessage | null>(null)

  // Isai Connect Sync state
  const [remotePlaybackState, setRemotePlaybackState] = useState<PlaybackStateSync | null>(null)
  const [connectedDevices, setConnectedDevices] = useState<DeviceInfo[]>([])
  const myDeviceId = IsaiConnectService.getMyDeviceId()

  const searchTimerRef = useRef<number | null>(null)

  const showToast = (text: string, type: 'success' | 'error' | 'info' = 'success') => {
    setToast({ id: Date.now().toString(), text, type })
  }

  // Handle URL router for Email Verification callback
  const [isVerifyView, setIsVerifyView] = useState(false)
  useEffect(() => {
    const path = window.location.pathname
    if (path === '/verify-email' || window.location.search.includes('mode=verifyEmail')) {
      setIsVerifyView(true)
    }
  }, [])

  // Initialize ISAI Connect service
  useEffect(() => {
    const activeUserId = user.email || 'user_jeeva_default'
    IsaiConnectService.initialize(activeUserId)

    const unsubDevices = IsaiConnectService.subscribeDevices((devices) => {
      setConnectedDevices(devices)
    })

    const unsubState = IsaiConnectService.subscribePlaybackState((state) => {
      setRemotePlaybackState(state)
    })

    return () => {
      unsubDevices()
      unsubState()
    }
  }, [user.email])

  // Pre-fetch audio URLs for upcoming songs in the queue to eliminate loading delay
  useEffect(() => {
    if (playbackQueue.length === 0) return

    const lookahead = playbackQueue.slice(currentQueueIndex + 1, currentQueueIndex + 4)
    
    lookahead.forEach(async (song) => {
      if (!song.audioUrl) {
        try {
          const results = await musicApi.searchSongs(song.title)
          if (results && results.length > 0 && results[0].audioUrl) {
            const resolvedUrl = results[0].audioUrl
            setPlaybackQueue(prev => prev.map(s => 
              s.videoId === song.videoId ? { ...s, audioUrl: resolvedUrl } : s
            ))
          }
        } catch (e) {
          console.warn('[App] Failed to pre-resolve audioUrl:', e)
        }
      }
    })
  }, [playbackQueue, currentQueueIndex])

  // Load trending music
  const loadTrending = async () => {
    setIsTrendingLoading(true)
    setTrendingError(null)

    try {
      const data = await musicApi.getTrending()
      if (data && data.length > 0) {
        setTrendingSongs(data)
      } else {
        setTrendingSongs(INITIAL_CURATED_SONGS)
      }
    } catch (e: any) {
      console.warn('[App] Failed to fetch trending music online, using curated offline list:', e)
      setTrendingSongs(INITIAL_CURATED_SONGS)
    } finally {
      setIsTrendingLoading(false)
    }
  }

  useEffect(() => {
    loadTrending()
  }, [])

  // User Authentication handlers
  const handleUpdateProfile = (userData: Partial<UserProfile>) => {
    const updatedUser: UserProfile = {
      ...user,
      ...userData,
      name: userData.name || user.name,
      avatar: userData.avatar || (userData.name ? userData.name.charAt(0).toUpperCase() : user.avatar)
    }
    setUser(updatedUser)
    storageService.setUserProfile(updatedUser)
    showToast('Profile updated successfully!')
  }

  const handleLogin = (newUser: Partial<UserProfile>) => {
    const updatedUser: UserProfile = {
      isLoggedIn: true,
      name: newUser.name || user.name || 'JEEVA ⚡',
      email: newUser.email || user.email || 'kongujeeva523@gmail.com',
      avatar: newUser.avatar || (newUser.name ? newUser.name.charAt(0).toUpperCase() : 'J'),
      isPremium: true
    }
    setUser(updatedUser)
    storageService.setUserProfile(updatedUser)
    showToast(`Welcome back, ${updatedUser.name}!`)
  }

  const handleLogout = () => {
    const defaultUser: UserProfile = {
      isLoggedIn: false,
      name: 'JEEVA ⚡',
      email: 'kongujeeva523@gmail.com',
      avatar: 'J',
      isPremium: true
    }
    setUser(defaultUser)
    storageService.setUserProfile(defaultUser)
    showToast('Signed out successfully', 'info')
  }

  // Playback control handlers
  const handlePlaySong = async (song: Song, queue: Song[] = [], forceLocal: boolean = false) => {
    const isRemoteActive = Boolean(
      !forceLocal &&
      remotePlaybackState &&
      remotePlaybackState.currentDeviceId &&
      remotePlaybackState.currentDeviceId !== myDeviceId &&
      remotePlaybackState.currentTitle
    )

    if (isRemoteActive) {
      IsaiConnectService.sendCommand('PLAY_SONG', { song })
      const playingDevice = connectedDevices.find(d => d.deviceId === remotePlaybackState?.currentDeviceId)
      const devName = playingDevice?.deviceName || (remotePlaybackState?.currentDeviceId.includes('android') ? "Jeeva's Phone" : "Mobile Device")
      showToast(`Playing on ${devName} 📱`)
      return
    }

    setCurrentPlayingSong(song)
    if (queue.length > 0) {
      setPlaybackQueue(queue)
      const idx = queue.findIndex((s) => s.videoId === song.videoId)
      setCurrentQueueIndex(idx >= 0 ? idx : 0)
    } else {
      setPlaybackQueue([song])
      setCurrentQueueIndex(0)
    }

    if (!song.audioUrl) {
      try {
        const results = await musicApi.searchSongs(song.title)
        if (results && results.length > 0 && results[0].audioUrl) {
          const resolvedUrl = results[0].audioUrl
          const updated = { ...song, audioUrl: resolvedUrl }
          setCurrentPlayingSong(updated)
        }
      } catch (e) {
        console.warn('[App] AudioUrl resolution error:', e)
      }
    }
  }

  const handleNextSong = async () => {
    const isRemoteActive = Boolean(
      remotePlaybackState &&
      remotePlaybackState.currentDeviceId &&
      remotePlaybackState.currentDeviceId !== myDeviceId &&
      remotePlaybackState.currentTitle
    )
    if (isRemoteActive) {
      IsaiConnectService.sendCommand('NEXT')
      return
    }

    if (!currentPlayingSong) return
    if (playbackQueue.length > 0 && currentQueueIndex >= 0 && currentQueueIndex < playbackQueue.length - 1) {
      const nextIdx = currentQueueIndex + 1
      setCurrentQueueIndex(nextIdx)
      setCurrentPlayingSong(playbackQueue[nextIdx])
      return
    }

    if (trendingSongs.length > 0) {
      const curIdx = trendingSongs.findIndex((s) => s.videoId === currentPlayingSong.videoId)
      const nextIdx = (curIdx + 1) % trendingSongs.length
      setCurrentPlayingSong(trendingSongs[nextIdx])
    }
  }

  const handlePrevSong = () => {
    const isRemoteActive = Boolean(
      remotePlaybackState &&
      remotePlaybackState.currentDeviceId &&
      remotePlaybackState.currentDeviceId !== myDeviceId &&
      remotePlaybackState.currentTitle
    )
    if (isRemoteActive) {
      IsaiConnectService.sendCommand('PREV')
      return
    }

    if (playbackQueue.length > 0 && currentQueueIndex > 0) {
      const prevIdx = currentQueueIndex - 1
      setCurrentQueueIndex(prevIdx)
      setCurrentPlayingSong(playbackQueue[prevIdx])
    }
  }

  // Search handler
  const handleSearchChange = (query: string) => {
    setSearchQuery(query)
    if (searchTimerRef.current) {
      window.clearTimeout(searchTimerRef.current)
    }

    if (!query.trim()) {
      setSearchResults([])
      setIsSearching(false)
      return
    }

    setIsSearching(true)

    searchTimerRef.current = window.setTimeout(async () => {
      try {
        const results = await musicApi.searchSongs(query)
        setSearchResults(results)
      } catch (error: any) {
        console.error('Search failed', error)
        setSearchResults([])
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

  // Favorites & Playlist handlers
  const handleToggleFavorite = (song: Song) => {
    storageService.toggleFavorite(song)
    const updated = storageService.getFavorites()
    setFavorites(updated)
    const isFav = updated.some(s => s.videoId === song.videoId)
    showToast(isFav ? 'Added to Liked Songs 💖' : 'Removed from Liked Songs', 'info')
  }

  const isFavorite = (videoId: string) => {
    return favorites.some((s) => s.videoId === videoId)
  }

  const handleCreatePlaylist = (name: string) => {
    storageService.createPlaylist(name)
    setUserPlaylists(storageService.getPlaylists())
    setIsPlaylistModalOpen(false)
    showToast(`Playlist "${name}" created! 🎉`)
  }

  const handleAddSongToPlaylist = (playlistId: string, song: Song) => {
    storageService.addSongToPlaylist(playlistId, song)
    setUserPlaylists(storageService.getPlaylists())
    showToast(`Added to playlist! 🎵`)
  }

  const handleOpenAddToPlaylistModal = (song: Song) => {
    setSongToAddToPlaylist(song)
    setIsPlaylistModalOpen(true)
  }

  const handleSelectArtist = (artist: Artist) => {
    setSelectedArtist(artist)
    setCurrentTab('artist-detail')
  }

  const handleSelectPlaylistDetail = (title: string, subtitle: string, songs: Song[], coverUrl?: string) => {
    setSelectedPlaylistDetail({ title, subtitle, songs, coverUrl })
    setCurrentTab('playlist-detail')
  }

  if (isVerifyView) {
    return <VerifyEmailPage onNavigateHome={() => (window.location.href = '/')} />
  }

  return (
    <MainLayout
      currentTab={currentTab}
      onSelectTab={(tab) => {
        setCurrentTab(tab)
        if (tab !== 'artist-detail') setSelectedArtist(null)
        if (tab !== 'playlist-detail') setSelectedPlaylistDetail(null)
      }}
      searchQuery={searchQuery}
      onSearchChange={handleSearchChange}
      onSearchClear={() => handleSearchChange('')}
      onOpenLogin={() => setCurrentTab('login')}
      userName={user.name}
      userAvatar={user.avatar}
      userPlaylists={userPlaylists}
      onCreatePlaylist={() => {
        setSongToAddToPlaylist(null)
        setIsPlaylistModalOpen(true)
      }}
      onSelectPlaylist={(pl) => handleSelectPlaylistDetail(pl.name, 'Custom Playlist', pl.songs || [])}
      hasPlayer={Boolean(currentPlayingSong)}
    >
      {/* 1. Home View */}
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
          onAddToPlaylist={handleOpenAddToPlaylistModal}
          onSelectArtist={handleSelectArtist}
          currentSong={currentPlayingSong}
          isPlaying={!!currentPlayingSong}
        />
      )}

      {/* 2. Explore / Search View */}
      {currentTab === 'search' && (
        <SearchPage
          searchQuery={searchQuery}
          onSearchChange={handleSearchChange}
          onSearchClear={() => handleSearchChange('')}
          searchResults={searchResults}
          isSearching={isSearching}
          isFavorite={isFavorite}
          onToggleFavorite={handleToggleFavorite}
          onPlaySong={(song) => handlePlaySong(song, searchResults)}
          onAddToPlaylist={handleOpenAddToPlaylistModal}
          onSelectCategory={handleCategorySelect}
          onSelectArtist={handleSelectArtist}
          currentSong={currentPlayingSong}
          isPlaying={!!currentPlayingSong}
        />
      )}

      {/* 3. Library View */}
      {currentTab === 'library' && (
        <LibraryPage
          likedSongs={favorites}
          userPlaylists={userPlaylists}
          currentSong={currentPlayingSong}
          isPlaying={!!currentPlayingSong}
          isFavorite={isFavorite}
          onToggleFavorite={handleToggleFavorite}
          onPlaySong={(song) => handlePlaySong(song, favorites)}
          onAddToPlaylist={handleOpenAddToPlaylistModal}
          onCreatePlaylist={() => {
            setSongToAddToPlaylist(null)
            setIsPlaylistModalOpen(true)
          }}
          onSelectPlaylistDetail={handleSelectPlaylistDetail}
          onNavigateToSearch={() => setCurrentTab('search')}
        />
      )}

      {/* 4. Full Profile View */}
      {currentTab === 'profile' && (
        <ProfilePage
          user={user}
          favoritesCount={favorites.length}
          playlistsCount={userPlaylists.length}
          onUpdateProfile={handleUpdateProfile}
          onNavigateToLogin={() => setCurrentTab('login')}
          onLogout={handleLogout}
          onNavigateHome={() => setCurrentTab('home')}
        />
      )}

      {/* 5. Full Sign In & Register Page */}
      {currentTab === 'login' && (
        <LoginPage
          onLogin={(res) => {
            handleLogin(res)
            setCurrentTab('profile')
          }}
          onNavigateBack={() => setCurrentTab('profile')}
        />
      )}

      {/* 5. Artist Details View */}
      {currentTab === 'artist-detail' && selectedArtist && (
        <ArtistDetailPage
          artist={selectedArtist}
          artistSongs={trendingSongs}
          currentSong={currentPlayingSong}
          isPlaying={!!currentPlayingSong}
          isFavorite={isFavorite}
          onToggleFavorite={handleToggleFavorite}
          onPlaySong={(song) => handlePlaySong(song, trendingSongs)}
          onAddToPlaylist={handleOpenAddToPlaylistModal}
          onBack={() => setCurrentTab('home')}
        />
      )}

      {/* 6. Playlist / Album Details View */}
      {currentTab === 'playlist-detail' && selectedPlaylistDetail && (
        <PlaylistDetailPage
          title={selectedPlaylistDetail.title}
          subtitle={selectedPlaylistDetail.subtitle}
          songs={selectedPlaylistDetail.songs}
          coverUrl={selectedPlaylistDetail.coverUrl}
          currentSong={currentPlayingSong}
          isPlaying={!!currentPlayingSong}
          isFavorite={isFavorite}
          onToggleFavorite={handleToggleFavorite}
          onPlaySong={(song) => handlePlaySong(song, selectedPlaylistDetail.songs)}
          onAddToPlaylist={handleOpenAddToPlaylistModal}
          onBack={() => setCurrentTab('library')}
        />
      )}


      {/* Playlist Creation / Add Song Modal */}
      <PlaylistModal
        isOpen={isPlaylistModalOpen}
        onClose={() => setIsPlaylistModalOpen(false)}
        songToAdd={songToAddToPlaylist}
        userPlaylists={userPlaylists}
        onCreatePlaylist={handleCreatePlaylist}
        onAddSongToPlaylist={handleAddSongToPlaylist}
      />

      {/* Toast Notification */}
      <Toast toast={toast} onClose={() => setToast(null)} />

      {/* Persistent Web Audio Player with Spotify Connect */}
      <WebPlayer
        song={currentPlayingSong}
        remoteState={remotePlaybackState}
        connectedDevices={connectedDevices}
        onClose={() => setCurrentPlayingSong(null)}
        isFavorite={currentPlayingSong ? isFavorite(currentPlayingSong.videoId) : false}
        onToggleFavorite={handleToggleFavorite}
        onNextSong={handleNextSong}
        onPrevSong={handlePrevSong}
        queue={playbackQueue.length > 0 ? playbackQueue : trendingSongs}
        onSelectQueueItem={(s) => handlePlaySong(s, playbackQueue.length > 0 ? playbackQueue : trendingSongs)}
        userId={user.email || 'user_jeeva_default'}
        onTransferPlayback={(s) => handlePlaySong(s, [], true)}
      />
    </MainLayout>
  )
}
