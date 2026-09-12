import { useEffect, useRef, useState, useMemo } from 'react'

import { musicApi, detectSongLanguage } from '@shared/api/music-api'
import { storageService } from '@shared/services/storageService'
import type { Song, UserPlaylist } from '@shared/models/song'
import { cleanHtmlTitle, isSameSongOrDuplicate, deduplicateSongs } from '@shared/utils/formatters'
import { buildRelevantQueue, getRelevantSearchQuery, scoreSongRelevance } from '@shared/utils/relevance'
import { SmartSearchEngine } from '@shared/services/smartSearchEngine'

import { LanguageSelectionModal } from './components/LanguageSelectionModal'

import { WebPlayer } from './components/WebPlayer'
import { logoutFirebaseUser } from './firebase'
import { type UserProfile } from './components/LoginModal'
import { MainLayout, type PageTab } from './layouts/MainLayout'
import { HomePage } from './pages/HomePage'
import { LibraryPage } from './pages/LibraryPage'
import { SearchPage } from './pages/SearchPage'
import { ProfilePage } from './pages/ProfilePage'
import { LoginPage } from './pages/LoginPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'
import { UpdatePage } from './pages/UpdatePage'
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
    gradient?: string
  } | null>(null)

  // Modals & Toast State
  const [isPlaylistModalOpen, setIsPlaylistModalOpen] = useState(false)
  const [songToAddToPlaylist, setSongToAddToPlaylist] = useState<Song | null>(null)
  const [toast, setToast] = useState<ToastMessage | null>(null)
  const [showLanguageModal, setShowLanguageModal] = useState(false)

  // Isai Connect Sync state
  const [remotePlaybackState, setRemotePlaybackState] = useState<PlaybackStateSync | null>(null)
  const [connectedDevices, setConnectedDevices] = useState<DeviceInfo[]>([])
  const myDeviceId = IsaiConnectService.getMyDeviceId()

  const searchTimerRef = useRef<number | null>(null)

  const showToast = (text: string, type: 'success' | 'error' | 'info' = 'success') => {
    setToast({ id: Date.now().toString(), text, type })
  }

  // Handle URL router for Email Verification callback and App Update Page
  const [isVerifyView, setIsVerifyView] = useState(false)
  const [isUpdateView, setIsUpdateView] = useState(false)
  useEffect(() => {
    const path = window.location.pathname
    if (path === '/verify-email' || window.location.search.includes('mode=verifyEmail')) {
      setIsVerifyView(true)
    }
    if (path === '/update' || path.startsWith('/update') || window.location.search.includes('action=update')) {
      setIsUpdateView(true)
    }
  }, [])

  // Ref to handlePlaySong to avoid circular useEffect dependencies
  const handlePlaySongRef = useRef<(song: Song, queue?: Song[], forceLocal?: boolean) => Promise<void>>()
  const playbackQueueRef = useRef<Song[]>([])
  playbackQueueRef.current = playbackQueue

  // Initialize ISAI Connect service
  useEffect(() => {
    const activeUserId = user.email || 'user_guest'
    IsaiConnectService.initialize(activeUserId, user.name)

    const unsubDevices = IsaiConnectService.subscribeDevices((devices) => {
      setConnectedDevices(devices)
    })

    const unsubState = IsaiConnectService.subscribePlaybackState((state) => {
      setRemotePlaybackState(state)
      // When another device transfers playback to this Web instance
      if (state && state.currentDeviceId === myDeviceId && state.updatedByDeviceId && state.updatedByDeviceId !== myDeviceId) {
        if (state.currentTitle) {
          const targetSong: Song = {
            videoId: state.currentSongId || `transfer_${Date.now()}`,
            title: state.currentTitle,
            channelTitle: state.currentArtist || 'Artist',
            thumbnailUrl: state.currentArtwork || '',
            audioUrl: state.currentAudioUrl || '',
            durationFormatted: '3:30',
            durationMs: state.durationMs || 210000,
            viewCountFormatted: ''
          }
          const currentQ = playbackQueueRef.current || []
          const queueToUse = currentQ.length > 0 ? currentQ : [targetSong]
          handlePlaySongRef.current?.(targetSong, queueToUse, true)
        }
      }
    })

    const unsubPrefs = IsaiConnectService.subscribePreferences((prefs) => {
      if (prefs.preferredLanguages && prefs.preferredLanguages.length > 0) {
        setUser((prev) => {
          const updated = { ...prev, preferredLanguages: prefs.preferredLanguages }
          storageService.setUserProfile(updated)
          return updated
        })
        loadTrending(prefs.preferredLanguages)
      }
    })

    const unsubCmd = IsaiConnectService.subscribeCommands((cmd) => {
      if (cmd.issuedByDeviceId === myDeviceId) return
      const isTargetedToMe = !cmd.targetDeviceId ||
        cmd.targetDeviceId === myDeviceId ||
        cmd.targetDeviceId.includes('web') ||
        (!remotePlaybackState?.currentDeviceId || remotePlaybackState?.currentDeviceId === myDeviceId)
      if (!isTargetedToMe) return

      if (cmd.action === 'PLAY_SONG') {
        const s = (cmd.song || {}) as any
        const songId = s.videoId || s.id || cmd.songId
        const songTitle = s.title || cmd.songTitle
        if (songId || songTitle) {
          const currentQ = playbackQueueRef.current || []
          const existingSong = currentQ.find(
            item => (songId && item.videoId === songId) || (songTitle && isSameSongOrDuplicate(item, { title: songTitle, videoId: songId }))
          )

          const targetSong: Song = {
            videoId: songId || existingSong?.videoId || `remote_${Date.now()}`,
            title: songTitle || existingSong?.title || 'ISAI Track',
            channelTitle: s.channelTitle || s.artist || cmd.songArtist || existingSong?.channelTitle || 'ISAI Artist',
            thumbnailUrl: s.thumbnailUrl || s.artwork || cmd.songArtwork || existingSong?.thumbnailUrl || '',
            audioUrl: s.audioUrl || cmd.songAudioUrl || existingSong?.audioUrl || '',
            durationFormatted: s.durationFormatted || existingSong?.durationFormatted || '3:30',
            durationMs: s.durationMs || existingSong?.durationMs || 210000,
            viewCountFormatted: ''
          }
          const queueToUse = currentQ.length > 0 ? currentQ : [targetSong]
          handlePlaySongRef.current?.(targetSong, queueToUse, true)
        }
      } else if (cmd.action === 'ADD_TO_QUEUE') {
        const targetSong: Song | null = cmd.song ? cmd.song : (cmd.songId ? {
          videoId: cmd.songId,
          title: cmd.songTitle || 'ISAI Track',
          channelTitle: cmd.songArtist || 'ISAI Artist',
          thumbnailUrl: cmd.songArtwork || '',
          audioUrl: cmd.songAudioUrl || '',
          durationFormatted: '3:30',
          durationMs: 210000,
          viewCountFormatted: ''
        } : null)

        if (targetSong) {
          setPlaybackQueue(prev => {
            if (prev.some(s => s.videoId === targetSong.videoId)) return prev
            const updated = [...prev, targetSong]
            IsaiConnectService.updatePlaybackState({
              queue: updated.map(item => ({
                id: item.videoId,
                title: item.title,
                artist: item.channelTitle,
                artwork: item.thumbnailUrl
              }))
            })
            return updated
          })
          showToast(`Added "${targetSong.title.slice(0, 20)}..." to Queue 🎵`)
        }
      } else if (cmd.action === 'PLAY_NEXT_IN_QUEUE') {
        const s = cmd.song as any
        const targetSong: Song | null = s ? {
          videoId: s.videoId || s.id || cmd.songId || `remote_${Date.now()}`,
          title: s.title || cmd.songTitle || 'ISAI Track',
          channelTitle: s.channelTitle || s.artist || cmd.songArtist || 'ISAI Artist',
          thumbnailUrl: s.thumbnailUrl || s.artwork || cmd.songArtwork || '',
          audioUrl: s.audioUrl || cmd.songAudioUrl || '',
          durationFormatted: s.durationFormatted || '3:30',
          durationMs: s.durationMs || 210000,
          viewCountFormatted: ''
        } : (cmd.songId ? {
          videoId: cmd.songId,
          title: cmd.songTitle || 'ISAI Track',
          channelTitle: cmd.songArtist || 'ISAI Artist',
          thumbnailUrl: cmd.songArtwork || '',
          audioUrl: cmd.songAudioUrl || '',
          durationFormatted: '3:30',
          durationMs: 210000,
          viewCountFormatted: ''
        } : null)

        if (targetSong) {
          setPlaybackQueue(prev => {
            const filtered = prev.filter(item => item.videoId !== targetSong.videoId)
            const curIdx = currentPlayingSong ? filtered.findIndex(item => item.videoId === currentPlayingSong.videoId) : -1
            const insertIdx = curIdx >= 0 ? curIdx + 1 : (currentQueueIndex >= 0 ? currentQueueIndex + 1 : 0)
            const updated = [...filtered]
            updated.splice(insertIdx, 0, targetSong)

            IsaiConnectService.updatePlaybackState({
              queue: updated.map(item => ({
                id: item.videoId,
                title: item.title,
                artist: item.channelTitle,
                artwork: item.thumbnailUrl
              }))
            })
            return updated
          })
          showToast(`Mobile added "${targetSong.title.slice(0, 20)}..." to Play Next ⏭️`)
        }
      }
    })

    const unsubHome = IsaiConnectService.subscribeHomeSongs((syncedSongs) => {
      if (syncedSongs && syncedSongs.length > 0) {
        console.log('[App] Received synced home songs from mobile account:', syncedSongs.length)
        setTrendingSongs(syncedSongs)
      }
    })

    const unsubFavs = IsaiConnectService.subscribeFavorites((syncedFavs) => {
      if (syncedFavs && syncedFavs.length > 0) {
        setFavorites(prev => {
          const remoteIds = new Set(syncedFavs.map(s => s.videoId))
          const localOnly = prev.filter(s => !remoteIds.has(s.videoId))
          const merged = [...syncedFavs, ...localOnly]
          storageService.setFavorites(merged)
          return merged
        })
      }
    })

    return () => {
      unsubDevices()
      unsubState()
      unsubPrefs()
      unsubCmd()
      unsubHome()
      unsubFavs()
    }
  }, [user.email])

  // Load trending music
  const loadTrending = async (languages?: string[]) => {
    setIsTrendingLoading(true)
    setTrendingError(null)

    try {
      const activeLangs = (languages && languages.length > 0)
        ? languages
        : (user.preferredLanguages && user.preferredLanguages.length > 0 ? user.preferredLanguages : ['tamil'])
      const data = await musicApi.getTrending(activeLangs)
      if (data && data.length > 0) {
        setTrendingSongs(data)
        IsaiConnectService.syncHomeSongs(data)
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
    loadTrending(user.preferredLanguages)
  }, [user.email])

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
    if (updatedUser.preferredLanguages) {
      IsaiConnectService.syncPreferences({ preferredLanguages: updatedUser.preferredLanguages })
      loadTrending(updatedUser.preferredLanguages)
    }
    showToast('Profile updated successfully!')
  }

  const handleLogin = (newUser: Partial<UserProfile>) => {
    const updatedUser: UserProfile = {
      isLoggedIn: true,
      name: newUser.name || user.name || 'JEEVA ⚡',
      email: newUser.email || user.email || 'kongujeeva523@gmail.com',
      avatar: newUser.avatar || (newUser.name ? newUser.name.charAt(0).toUpperCase() : 'J'),
      isPremium: true,
      preferredLanguages: newUser.preferredLanguages || user.preferredLanguages || ['tamil']
    }
    setUser(updatedUser)
    storageService.setUserProfile(updatedUser)
    if (updatedUser.preferredLanguages) {
      IsaiConnectService.syncPreferences({ preferredLanguages: updatedUser.preferredLanguages })
    }
    loadTrending(updatedUser.preferredLanguages)
    showToast(`Welcome back, ${updatedUser.name}!`)
  }

  const handleLogout = async () => {
    try {
      await logoutFirebaseUser()
    } catch (err) {
      console.warn('Firebase logout notice:', err)
    }
    const defaultUser: UserProfile = {
      isLoggedIn: false,
      name: 'Guest Listener',
      email: '',
      avatar: 'G',
      isPremium: false
    }
    setUser(defaultUser)
    storageService.setUserProfile(defaultUser)
    showToast('Signed out successfully', 'info')
    setCurrentTab('login')
  }

  // Extract clean primary artist name for search queries
  const extractCleanArtist = (channelOrArtist?: string): string => {
    if (!channelOrArtist) return ''
    const cleaned = channelOrArtist
      .replace(/ - Topic|VEVO|Official|Channel|Sun TV|Sony Music South|Think Music India|Wunderbar Films|Saregama/gi, '')
      .trim()
    const lower = cleaned.toLowerCase()
    if (lower.includes('anirudh')) return 'Anirudh Ravichander'
    if (lower.includes('rahman') || lower.includes('arr')) return 'A.R. Rahman'
    if (lower.includes('yuvan') || lower.includes('u1')) return 'Yuvan Shankar Raja'
    if (lower.includes('harris')) return 'Harris Jayaraj'
    if (lower.includes('santhosh') || lower.includes('sana')) return 'Santhosh Narayanan'
    if (lower.includes('g v') || lower.includes('gv prakash')) return 'G.V. Prakash'
    if (lower.includes('ilayaraja') || lower.includes('ilaiyaraaja')) return 'Ilaiyaraaja'
    if (lower.includes('sid sriram')) return 'Sid Sriram'
    if (lower.includes('deva')) return 'Deva'
    if (lower.includes('hiphop tamizha')) return 'Hiphop Tamizha'
    return cleaned.split(/[•,&|-]/)[0]?.trim() || ''
  }

  // Auto-replenish queue based on currently playing song and login language preferences
  const isFetchingQueueRef = useRef(false)
  const ensureEndlessQueue = async (seedSong: Song, currentQueue: Song[], currentIndex: number) => {
    if (isFetchingQueueRef.current) return
    const remaining = currentQueue.length - 1 - currentIndex
    if (remaining > 4) return

    isFetchingQueueRef.current = true
    try {
      const primaryLang = (user.preferredLanguages && user.preferredLanguages[0])
        ? user.preferredLanguages[0].toLowerCase()
        : 'tamil'
      const artist = extractCleanArtist(seedSong.channelTitle)
      const moodQuery = getRelevantSearchQuery(seedSong, primaryLang)

      const queries = [
        moodQuery,
        artist ? `${artist} ${moodQuery}` : moodQuery
      ]

      const fetchedResults = await Promise.all(
        queries.map(q => musicApi.searchSongs(q).catch(() => []))
      )
      const combined = fetchedResults.flat()

      const existingIds = new Set(currentQueue.map(s => s.videoId))

      const activeLangs = (user.preferredLanguages && user.preferredLanguages.length > 0)
        ? user.preferredLanguages.map(l => l.toLowerCase())
        : ['tamil']

      const newSongs: Song[] = []
      for (const s of combined) {
        if (!s || !s.videoId || existingIds.has(s.videoId)) continue
        if (isSameSongOrDuplicate(seedSong, s)) continue
        if (currentQueue.some(item => isSameSongOrDuplicate(item, s))) continue

        const songLang = (s.language || detectSongLanguage(s)).toLowerCase()
        if (!activeLangs.includes(songLang)) continue
        if (scoreSongRelevance(seedSong, s, activeLangs) <= 0) continue

        existingIds.add(s.videoId)
        newSongs.push(s)
        if (newSongs.length >= 15) break
      }

      if (newSongs.length > 0) {
        setPlaybackQueue(prevQueue => {
          const prevIds = new Set(prevQueue.map(item => item.videoId))
          const toAdd = newSongs.filter(item => !prevIds.has(item.videoId) && !isSameSongOrDuplicate(seedSong, item))
          if (toAdd.length === 0) return prevQueue
          const updated = deduplicateSongs([...prevQueue, ...toAdd])
          IsaiConnectService.updatePlaybackState({
            queue: updated.map(item => ({
              id: item.videoId,
              title: item.title,
              artist: item.channelTitle,
              artwork: item.thumbnailUrl,
              audioUrl: item.audioUrl || ''
            }))
          })
          return updated
        })
      }
    } catch (err) {
      console.warn('[App] ensureEndlessQueue error:', err)
    } finally {
      isFetchingQueueRef.current = false
    }
  }

  // Spotify-style Daily Mixes computed from trending songs and user's preferred languages
  const spotifyDailyMixes = useMemo(() => {
    const primaryLang = (user.preferredLanguages && user.preferredLanguages[0])
      ? user.preferredLanguages[0].toUpperCase()
      : 'TAMIL'

    const anirudhSongs = trendingSongs.filter(s =>
      s.title?.toLowerCase().includes('anirudh') || s.channelTitle?.toLowerCase().includes('anirudh')
    )
    const arrSongs = trendingSongs.filter(s =>
      s.title?.toLowerCase().includes('rahman') || s.channelTitle?.toLowerCase().includes('rahman') || s.channelTitle?.toLowerCase().includes('arr')
    )
    const yuvanSongs = trendingSongs.filter(s =>
      s.title?.toLowerCase().includes('yuvan') || s.channelTitle?.toLowerCase().includes('yuvan') || s.channelTitle?.toLowerCase().includes('u1')
    )

    return [
      {
        id: 'daily_mix_1',
        title: 'Daily Mix 1 • Anirudh Hits',
        subtitle: 'Anirudh, Dhanush, Vijay & club chartbusters',
        gradient: 'linear-gradient(135deg, #1DB954 0%, #121212 100%)',
        coverUrl: anirudhSongs[0]?.thumbnailUrl || 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
        songs: anirudhSongs.length > 0 ? anirudhSongs : trendingSongs.slice(0, 8)
      },
      {
        id: 'daily_mix_2',
        title: 'Daily Mix 2 • A.R. Rahman Soul',
        subtitle: 'A.R. Rahman, Bombay Jayashri & timeless melodies',
        gradient: 'linear-gradient(135deg, #7C3AED 0%, #0F0C20 100%)',
        coverUrl: arrSongs[0]?.thumbnailUrl || 'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
        songs: arrSongs.length > 0 ? arrSongs : trendingSongs.slice(1, 9)
      },
      {
        id: 'daily_mix_3',
        title: 'Daily Mix 3 • Yuvan Drug Melodies',
        subtitle: 'Yuvan Shankar Raja, Harris & night drives',
        gradient: 'linear-gradient(135deg, #2563EB 0%, #080D1A 100%)',
        coverUrl: yuvanSongs[0]?.thumbnailUrl || 'https://c.saavncdn.com/276/Maari-2-Tamil-2018-20260203193952-500x500.jpg',
        songs: yuvanSongs.length > 0 ? yuvanSongs : trendingSongs.slice(2, 10)
      },
      {
        id: 'daily_mix_4',
        title: `Top 50 • ${primaryLang}`,
        subtitle: `The most played and trending hits in ${primaryLang}`,
        gradient: 'linear-gradient(135deg, #E11D48 0%, #190A12 100%)',
        coverUrl: trendingSongs[0]?.thumbnailUrl || 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
        songs: trendingSongs
      }
    ]
  }, [trendingSongs, user.preferredLanguages])

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
      const devName = playingDevice?.deviceName || (remotePlaybackState?.currentDeviceId.includes('android') ? "Mobile Phone" : "Remote Device")
      showToast(`Playing on ${devName} 📱`)
      return
    }

    setCurrentPlayingSong(song)
    const activeLangs = (user.preferredLanguages && user.preferredLanguages.length > 0)
      ? user.preferredLanguages.map(l => l.toLowerCase())
      : ['tamil']
    const isAdvancingInCurrentQueue = queue.length > 0 && playbackQueue === queue && queue.some(s => s.videoId === song.videoId)

    let nextQueue: Song[] = []
    let activeIdx = 0

    if (isAdvancingInCurrentQueue) {
      nextQueue = queue
      const idx = queue.findIndex((s) => s.videoId === song.videoId)
      activeIdx = idx >= 0 ? idx : 0
      setPlaybackQueue(queue)
      setCurrentQueueIndex(activeIdx)
    } else {
      const candidatePool = queue.length > 0 ? queue : trendingSongs
      const moodQueue = buildRelevantQueue(song, candidatePool, activeLangs, 50)
      nextQueue = moodQueue
      setPlaybackQueue(moodQueue)
      setCurrentQueueIndex(0)
    }

    // Auto replenish queue endlessly with mood matched songs
    ensureEndlessQueue(song, nextQueue, activeIdx)

    if (!song.audioUrl) {
      try {
        const cleanTitle = cleanHtmlTitle(song.title)
          .replace(/\s*[\|\-\–\—].*$/, '')
          .replace(/\s*\(.*?(official|video|audio|lyrics|hd|4k|song).*?\)/gi, '')
          .replace(/\s*\[.*?(official|video|audio|lyrics|hd|4k|song).*?\]/gi, '')
          .replace(/\.{2,}$/, '')
          .trim()

        const results = await musicApi.searchSongs(cleanTitle || song.title)
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
  handlePlaySongRef.current = handlePlaySong

  // Handle playing song selected from search results:
  // Starts playback of this song and builds a context-aware mood-matched radio queue
  const handlePlaySongFromSearch = (song: Song) => {
    const pool = trendingSongs.length > 0 ? trendingSongs : INITIAL_CURATED_SONGS
    const activeLangs = (user.preferredLanguages && user.preferredLanguages.length > 0)
      ? user.preferredLanguages.map(l => l.toLowerCase())
      : ['tamil']
    const moodQueue = buildRelevantQueue(song, pool, activeLangs, 50)
    handlePlaySong(song, moodQueue)
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
    const activeQueue = playbackQueue.length > 0 ? playbackQueue : (trendingSongs.length > 0 ? trendingSongs : INITIAL_CURATED_SONGS)
    let curIdx = activeQueue.findIndex((s) => s.videoId === currentPlayingSong.videoId)
    if (curIdx === -1) curIdx = currentQueueIndex

    let nextIdx = curIdx + 1
    if (nextIdx >= activeQueue.length) {
      nextIdx = 0
    }

    const nextSong = activeQueue[nextIdx]
    if (nextSong) {
      handlePlaySong(nextSong, activeQueue, true)
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

    if (!currentPlayingSong) return
    const activeQueue = playbackQueue.length > 0 ? playbackQueue : (trendingSongs.length > 0 ? trendingSongs : INITIAL_CURATED_SONGS)
    let curIdx = activeQueue.findIndex((s) => s.videoId === currentPlayingSong.videoId)
    if (curIdx === -1) curIdx = currentQueueIndex

    const prevIdx = curIdx > 0 ? curIdx - 1 : activeQueue.length - 1
    const prevSong = activeQueue[prevIdx]
    if (prevSong) {
      handlePlaySong(prevSong, activeQueue, true)
    }
  }

  // Add to Queue Handler
  const handleAddToQueue = (song: Song) => {
    const isRemoteActive = Boolean(
      remotePlaybackState &&
      remotePlaybackState.currentDeviceId &&
      remotePlaybackState.currentDeviceId !== myDeviceId &&
      remotePlaybackState.currentTitle
    )

    if (isRemoteActive) {
      IsaiConnectService.sendCommand('ADD_TO_QUEUE', {
        songId: song.videoId,
        songTitle: song.title,
        songArtist: song.channelTitle,
        songArtwork: song.thumbnailUrl,
        songAudioUrl: song.audioUrl,
        song
      })
      showToast(`Added to Mobile Queue 📱`)
      return
    }

    if (!currentPlayingSong) {
      handlePlaySong(song, [song])
      showToast(`Playing "${song.title.slice(0, 25)}..." 🎵`)
      return
    }

    setPlaybackQueue((prev) => {
      if (prev.some((s) => s.videoId === song.videoId)) {
        showToast('Song already in queue ℹ️', 'info')
        return prev
      }
      const updated = [...prev, song]
      IsaiConnectService.updatePlaybackState({
        queue: updated.map(item => ({
          id: item.videoId,
          title: item.title,
          artist: item.channelTitle,
          artwork: item.thumbnailUrl,
          audioUrl: item.audioUrl || ''
        }))
      })
      showToast(`Added to Queue 🎵`)
      return updated
    })
  }

  // Play Next in Queue Handler
  const handlePlayNext = (song: Song) => {
    const isRemoteActive = Boolean(
      remotePlaybackState &&
      remotePlaybackState.currentDeviceId &&
      remotePlaybackState.currentDeviceId !== myDeviceId &&
      remotePlaybackState.currentTitle
    )

    if (isRemoteActive) {
      IsaiConnectService.sendCommand('PLAY_NEXT_IN_QUEUE', {
        songId: song.videoId,
        songTitle: song.title,
        songArtist: song.channelTitle,
        songArtwork: song.thumbnailUrl,
        songAudioUrl: song.audioUrl,
        song
      })
      showToast(`Will play next on Mobile 📱`)
      return
    }

    if (!currentPlayingSong) {
      handlePlaySong(song, [song])
      showToast(`Playing "${song.title.slice(0, 25)}..." 🎵`)
      return
    }

    setPlaybackQueue((prev) => {
      const filtered = prev.filter((s) => s.videoId !== song.videoId)
      const curIdx = filtered.findIndex((s) => s.videoId === currentPlayingSong.videoId)
      const insertIdx = curIdx >= 0 ? curIdx + 1 : (currentQueueIndex >= 0 ? currentQueueIndex + 1 : 0)
      const updated = [...filtered]
      updated.splice(insertIdx, 0, song)

      IsaiConnectService.updatePlaybackState({
        queue: updated.map(item => ({
          id: item.videoId,
          title: item.title,
          artist: item.channelTitle,
          artwork: item.thumbnailUrl,
          audioUrl: item.audioUrl || ''
        }))
      })
      showToast(`Will play next: "${song.title.slice(0, 25)}..." ⏭️`)
      return updated
    })
  }

  // Search handler powered by SmartSearchEngine & Keyword Master List
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
        const userLangs = user.preferredLanguages || ['tamil']
        const favArtists = favorites.map(f => f.channelTitle).filter(Boolean)
        const intent = SmartSearchEngine.parseQuery(query, userLangs, favArtists)

        let results = await musicApi.searchSongs(intent.optimizedSearchQuery)

        // For specific song/movie searches (e.g. "Ghilli", "Leo", "Master", "Arabic Kuthu", "Kannazhaga"),
        // also query "${query} songs" so full movie soundtrack is retrieved from the music API
        if (intent.unmatchedTerms.length > 0) {
          const cleanQ = query.trim()
          const hasSongsWord = cleanQ.toLowerCase().includes('song') || cleanQ.toLowerCase().includes('paatu')
          const additionalQueries: string[] = []
          if (cleanQ.toLowerCase() !== intent.optimizedSearchQuery.toLowerCase()) {
            additionalQueries.push(cleanQ)
          }
          if (!hasSongsWord) {
            additionalQueries.push(`${cleanQ} songs`)
          }
          if (additionalQueries.length > 0) {
            const extra = await Promise.all(
              additionalQueries.map(q => musicApi.searchSongs(q).catch(() => []))
            )
            results = [...(results || []), ...extra.flat()]
          }
        } else if (!results || results.length === 0) {
          results = await musicApi.searchSongs(query)
        }

        const deduped = deduplicateSongs(results || [])
        const ranked = [...deduped].sort((a, b) => {
          return SmartSearchEngine.rankSong(b, intent, userLangs, favArtists) -
                 SmartSearchEngine.rankSong(a, intent, userLangs, favArtists)
        })

        setSearchResults(ranked)
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
    IsaiConnectService.syncFavorites(updated)
    const isFav = updated.some((s) => s.videoId === song.videoId)
    showToast(isFav ? `Added "${song.title.slice(0, 20)}..." to Favorites ❤️` : `Removed from Favorites 💔`, 'info')
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

  const handleSelectPlaylistDetail = (title: string, subtitle: string, songs: Song[], coverUrl?: string, gradient?: string) => {
    setSelectedPlaylistDetail({ title, subtitle, songs, coverUrl, gradient })
    setCurrentTab('playlist-detail')
  }

  if (isUpdateView) {
    return <UpdatePage />
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
      isLoggedIn={user.isLoggedIn}
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
          onPlaySong={(song, queue) => handlePlaySong(song, queue || trendingSongs)}
          onAddToPlaylist={handleOpenAddToPlaylistModal}
          onAddToQueue={handleAddToQueue}
          onPlayNext={handlePlayNext}
          onSelectArtist={handleSelectArtist}
          currentSong={currentPlayingSong}
          isPlaying={!!currentPlayingSong}
          dailyMixes={spotifyDailyMixes}
          onSelectPlaylistDetail={handleSelectPlaylistDetail}
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
          onPlaySong={(song) => handlePlaySongFromSearch(song)}
          onAddToPlaylist={handleOpenAddToPlaylistModal}
          onAddToQueue={handleAddToQueue}
          onPlayNext={handlePlayNext}
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
          onAddToQueue={handleAddToQueue}
          onPlayNext={handlePlayNext}
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
          onOpenLanguageModal={() => setShowLanguageModal(true)}
          onNavigateToLogin={() => setCurrentTab('login')}
          onLogout={handleLogout}
          onNavigateHome={() => setCurrentTab('home')}
        />
      )}

      {/* 5. Full Sign In & Register Page */}
      {currentTab === 'login' && (
        <LoginPage
          allowBack={true}
          onLogin={(res) => {
            handleLogin(res)
            setCurrentTab('home')
          }}
          onNavigateBack={() => setCurrentTab(user.isLoggedIn ? 'profile' : 'home')}
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
          onAddToQueue={handleAddToQueue}
          onPlayNext={handlePlayNext}
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
          gradient={selectedPlaylistDetail.gradient}
          currentSong={currentPlayingSong}
          isPlaying={!!currentPlayingSong}
          isFavorite={isFavorite}
          onToggleFavorite={handleToggleFavorite}
          onPlaySong={(song, queue) => handlePlaySong(song, queue || selectedPlaylistDetail.songs)}
          onAddToPlaylist={handleOpenAddToPlaylistModal}
          onAddToQueue={handleAddToQueue}
          onPlayNext={handlePlayNext}
          onBack={() => setCurrentTab('library')}
          userPreferredLanguages={user.preferredLanguages}
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
        onReorderQueue={(newQueue) => {
          setPlaybackQueue(newQueue)
          IsaiConnectService.updatePlaybackState({
            queue: newQueue.map(item => ({
              id: item.videoId,
              title: item.title,
              artist: item.channelTitle,
              artwork: item.thumbnailUrl,
              audioUrl: item.audioUrl || ''
            }))
          })
        }}
        onRemoveQueueItem={(indexToRemove) => {
          setPlaybackQueue((prev) => {
            const updated = prev.filter((_, i) => i !== indexToRemove)
            IsaiConnectService.updatePlaybackState({
              queue: updated.map(item => ({
                id: item.videoId,
                title: item.title,
                artist: item.channelTitle,
                artwork: item.thumbnailUrl,
                audioUrl: item.audioUrl || ''
              }))
            })
            return updated
          })
        }}
        onClearQueue={() => {
          if (currentPlayingSong) {
            setPlaybackQueue([currentPlayingSong])
            IsaiConnectService.updatePlaybackState({
              queue: [{
                id: currentPlayingSong.videoId,
                title: currentPlayingSong.title,
                artist: currentPlayingSong.channelTitle,
                artwork: currentPlayingSong.thumbnailUrl
              }]
            })
          } else {
            setPlaybackQueue([])
            IsaiConnectService.updatePlaybackState({ queue: [] })
          }
          showToast('Queue cleared 🗑️')
        }}
        userId={user.email || 'user_guest'}
        onTransferPlayback={(s) => handlePlaySong(s, [], true)}
      />

      {showLanguageModal && (
        <LanguageSelectionModal
          isOpen={showLanguageModal}
          initialSelected={user.preferredLanguages || ['tamil']}
          onSave={(selected) => {
            handleUpdateProfile({ preferredLanguages: selected })
            setShowLanguageModal(false)
            showToast(`Music language updated to ${selected.join(', ')}!`, 'success')
          }}
          onClose={() => setShowLanguageModal(false)}
        />
      )}
    </MainLayout>
  )
}
