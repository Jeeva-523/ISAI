import { detectSongLanguage, musicApi } from '@shared/api/music-api'

import { SmartSearchEngine } from '@shared/services/smartSearchEngine'
import { storageService } from '@shared/services/storageService'
import { cleanHtmlTitle, deduplicateSongs, isSameSongOrDuplicate } from '@shared/utils/formatters'
import {
  detectSongEra,
  detectSongMood,
  getRelevantSearchQuery,
  isPlaylistOrCompilation,
  scoreSongRelevance
} from '@shared/utils/relevance'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { LanguageSelectionModal } from './components/LanguageSelectionModal'

import { PlanSelectionModal } from './components/PlanSelectionModal'
import { PlaylistModal } from './components/PlaylistModal'

import { Toast, type ToastMessage } from './components/Toast'
import { WebPlayer } from './components/WebPlayer'
import { logoutFirebaseUser, subscribeToSubscription } from './firebase'
import { MainLayout, type PageTab } from './layouts/MainLayout'
import { ArtistDetailPage } from './pages/ArtistDetailPage'
import { HomePage } from './pages/HomePage'
import { LibraryPage } from './pages/LibraryPage'
import { LoginPage } from './pages/LoginPage'
import { PlaylistDetailPage } from './pages/PlaylistDetailPage'
import { ProfilePage } from './pages/ProfilePage'
import { SearchPage } from './pages/SearchPage'
import { UpdatePage } from './pages/UpdatePage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'
import { IsaiConnectService, type DeviceInfo, type PlaybackStateSync } from './services/IsaiConnectService'
import { ListenTogetherService } from './services/ListenTogetherService'
import type { Artist } from './components/ArtistCard'
import type { UserProfile } from './components/LoginModal'
import type { Song, UserPlaylist } from '@shared/models/song'

// Curated fallback songs with direct 320kbps audio streams
const INITIAL_CURATED_SONGS: Song[] = [
  {
    videoId: 'KUN5Uf9mObQ',
    title: 'Arabic Kuthu - Halamithi Habibo',
    channelTitle: 'Anirudh Ravichander • Beast',
    thumbnailUrl: 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
    durationFormatted: '4:39',
    durationMs: 279000,
    viewCountFormatted: '53M views',
    audioUrl: 'https://aac.saavncdn.com/510/9d96fc7ddd4ffadb745f25aed86f7a4e_320.mp4'
  },
  {
    videoId: '1F3hm6MfR1k',
    title: 'Hukum - Thalaivar Alappara',
    channelTitle: 'Anirudh Ravichander • Jailer',
    thumbnailUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
    durationFormatted: '3:27',
    durationMs: 207000,
    viewCountFormatted: '43M views',
    audioUrl: 'https://aac.saavncdn.com/187/0c4d0aee91a3ac81d4b645ec448a2960_320.mp4'
  },
  {
    videoId: 'szvt1vD0Uug',
    title: 'Naa Ready',
    channelTitle: 'Vijay • Leo',
    thumbnailUrl:
      'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
    durationFormatted: '4:08',
    durationMs: 248000,
    viewCountFormatted: '34M views',
    audioUrl: 'https://aac.saavncdn.com/415/3789bee89b94522160f1e50b2266d2c4_320.mp4'
  },
  {
    videoId: '3tmd-ClpJxA',
    title: 'Marakkuma Nenjam',
    channelTitle: 'A.R. Rahman • Vendhu Thanindhathu Kaadu (Original Motion Picture Soundtrack)',
    thumbnailUrl:
      'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
    durationFormatted: '4:18',
    durationMs: 258000,
    viewCountFormatted: '2M views',
    audioUrl: 'https://aac.saavncdn.com/494/ca8637be3d98d854e620dfc6e240bf7e_320.mp4'
  },
  {
    videoId: 'mqqft2x_Aa4',
    title: 'Kaavaalaa',
    channelTitle: 'Shilpa Rao • Jailer',
    thumbnailUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
    durationFormatted: '3:10',
    durationMs: 190000,
    viewCountFormatted: '21M views',
    audioUrl: 'https://aac.saavncdn.com/187/49797372d021638077d8a6b749068bc8_320.mp4'
  },
  {
    videoId: 'eN6AnYGYdVE',
    title: 'Vaseegara (From "Minnale")',
    channelTitle: 'Bombay Jayashri • 2 - In - 1 Hits Of Maddy',
    thumbnailUrl: 'https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg',
    durationFormatted: '4:59',
    durationMs: 299000,
    viewCountFormatted: '10M views',
    audioUrl: 'https://aac.saavncdn.com/450/4f7b9da8e887586e60b11afb602befac_320.mp4'
  },
  {
    videoId: 'jHNNMj5bNQw',
    title: 'Rowdy Baby',
    channelTitle: 'Dhanush • Maari 2',
    thumbnailUrl: 'https://c.saavncdn.com/276/Maari-2-Tamil-2018-20260203193952-500x500.jpg',
    durationFormatted: '4:41',
    durationMs: 281000,
    viewCountFormatted: '48M views',
    audioUrl: 'https://aac.saavncdn.com/276/64b835b4e1829992f8d35f54d6dad5f3_320.mp4'
  },
  {
    videoId: 'x6Q7c9Ry3tk',
    title: 'Why This Kolaveri Di? (The Soup of Love)',
    channelTitle: 'Dhanush • 3',
    thumbnailUrl: 'https://c.saavncdn.com/195/3-Tamil-2011-20210522203119-500x500.jpg',
    durationFormatted: '4:19',
    durationMs: 259000,
    viewCountFormatted: '37M views',
    audioUrl: 'https://aac.saavncdn.com/195/83f3042097cdca8e1c2ce1e44f370dda_320.mp4'
  },
  {
    videoId: 'Y6wB5b89E1E',
    title: 'Chilla Chilla',
    channelTitle: 'Ghibran • Thunivu',
    thumbnailUrl: 'https://c.saavncdn.com/blob/842/Thunivu-Tamil-2022-20230217151809-500x500.jpg',
    durationFormatted: '3:42',
    durationMs: 222000,
    viewCountFormatted: '10M views',
    audioUrl: 'https://aac.saavncdn.com/071/342ecd79c19839b794216476801ab364_320.mp4'
  },
  {
    videoId: 'vx2u5uUu3DE',
    title: 'Vaathi Coming',
    channelTitle: 'Anirudh Ravichander • Master',
    thumbnailUrl: 'https://c.saavncdn.com/347/Master-Tamil-2020-20200316084627-500x500.jpg',
    durationFormatted: '3:48',
    durationMs: 228000,
    viewCountFormatted: '50M views',
    audioUrl: 'https://aac.saavncdn.com/347/c536b256fca6b96aa432322c11c21fbb_320.mp4'
  },
  {
    videoId: 'vB8csw_kkyU',
    title: 'Chellamma',
    channelTitle: 'Sivakarthikeyan • Doctor',
    thumbnailUrl: 'https://c.saavncdn.com/312/Doctor-Tamil-2021-20211005133149-500x500.jpg',
    durationFormatted: '3:56',
    durationMs: 236000,
    viewCountFormatted: '25M views',
    audioUrl: 'https://aac.saavncdn.com/312/9faaf9ef4a0f3596b15adfbf3b9cc273_320.mp4'
  },
  {
    videoId: 'mSgN49e_Jyo',
    title: 'Dippam Dappam',
    channelTitle: 'Anirudh Ravichander • Kaathuvaakula Rendu Kaadhal',
    thumbnailUrl:
      'https://c.saavncdn.com/403/Kaathuvaakula-Rendu-Kaadhal-Original-Motion-Picture-Soundtrack-Tamil-2022-20220428131043-500x500.jpg',
    durationFormatted: '3:29',
    durationMs: 209000,
    viewCountFormatted: '20M views',
    audioUrl: 'https://aac.saavncdn.com/403/1a833ab353132dfe3d5f48ecd1c80334_320.mp4'
  },
  {
    videoId: 'I4_mN6-rC0E',
    title: 'Badass',
    channelTitle: 'Anirudh Ravichander • Leo',
    thumbnailUrl:
      'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
    durationFormatted: '3:49',
    durationMs: 229000,
    viewCountFormatted: '27M views',
    audioUrl: 'https://aac.saavncdn.com/415/46a7b21d2a3f4b9e019a7cdff7442c55_320.mp4'
  },
  {
    videoId: 'sUzG3h5V6bE',
    title: 'Ranjithame',
    channelTitle: 'Vijay • Varisu',
    thumbnailUrl: 'https://c.saavncdn.com/145/Varisu-Tamil-2022-20221226190213-500x500.jpg',
    durationFormatted: '4:47',
    durationMs: 287000,
    viewCountFormatted: '23M views',
    audioUrl: 'https://aac.saavncdn.com/145/a2e4e1d3758ea68c1a216032e2b23491_320.mp4'
  },
  {
    videoId: 'uM5d992fL9g',
    title: 'Thee Thalapathy',
    channelTitle: 'Silambarasan Tr • Varisu',
    thumbnailUrl: 'https://c.saavncdn.com/145/Varisu-Tamil-2022-20221226190213-500x500.jpg',
    durationFormatted: '4:18',
    durationMs: 258000,
    viewCountFormatted: '17M views',
    audioUrl: 'https://aac.saavncdn.com/145/143277c609348c0f2d5a396fef2cd8f3_320.mp4'
  },
  {
    videoId: '8uN1kR4-31w',
    title: 'Jimikki Ponnu',
    channelTitle: 'Anirudh Ravichander • Varisu',
    thumbnailUrl: 'https://c.saavncdn.com/145/Varisu-Tamil-2022-20221226190213-500x500.jpg',
    durationFormatted: '3:44',
    durationMs: 224000,
    viewCountFormatted: '14M views',
    audioUrl: 'https://aac.saavncdn.com/145/b58fb16bcca4b777a5acb7a0a960a5d9_320.mp4'
  },
  {
    videoId: '3XmrZaWVUpE',
    title: 'Aalaporaan Thamizhan',
    channelTitle: 'A.R. Rahman • Mersal',
    thumbnailUrl: 'https://c.saavncdn.com/492/Mersal-Tamil-2017-20170820120559-500x500.jpg',
    durationFormatted: '5:48',
    durationMs: 348000,
    viewCountFormatted: '15M views',
    audioUrl: 'https://aac.saavncdn.com/492/eb45d961a1639b3c3a6df4c8777fa71c_320.mp4'
  },
  {
    videoId: 'dJvj2c8d20A',
    title: 'Singappenney',
    channelTitle: 'A.R. Rahman • Bigil',
    thumbnailUrl: 'https://c.saavncdn.com/162/Bigil-Tamil-2019-20191017202521-500x500.jpg',
    durationFormatted: '6:04',
    durationMs: 364000,
    viewCountFormatted: '11M views',
    audioUrl: 'https://aac.saavncdn.com/162/a888165f1ebeda4b0ac6ac7f4be1a0f6_320.mp4'
  },
  {
    videoId: '8kL3yJ_aH4s',
    title: 'Verithanam',
    channelTitle: 'Vijay • Bigil',
    thumbnailUrl: 'https://c.saavncdn.com/162/Bigil-Tamil-2019-20191017202521-500x500.jpg',
    durationFormatted: '4:06',
    durationMs: 246000,
    viewCountFormatted: '16M views',
    audioUrl: 'https://aac.saavncdn.com/162/c005e1ef77af574df7ba1d24bd8ee9ea_320.mp4'
  },
  {
    videoId: 'cR77gU2g16s',
    title: 'Kadhaippoma',
    channelTitle: 'Leon James • Oh My Kadavule',
    thumbnailUrl: 'https://c.saavncdn.com/435/Oh-My-Kadavule-Tamil-2020-20200207054852-500x500.jpg',
    durationFormatted: '4:42',
    durationMs: 282000,
    viewCountFormatted: '14M views',
    audioUrl: 'https://aac.saavncdn.com/435/c8c5e3d0895c11377439673577049ddd_320.mp4'
  },
  {
    videoId: 'Gg4x336nN2E',
    title: 'En Rojaa Neeye',
    channelTitle: 'Hesham Abdul Wahab • Kushi (Tamil)',
    thumbnailUrl: 'https://c.saavncdn.com/683/Kushi-Tamil-Tamil-2023-20250130073118-500x500.jpg',
    durationFormatted: '4:03',
    durationMs: 243000,
    viewCountFormatted: '10M views',
    audioUrl: 'https://aac.saavncdn.com/683/b9c5368d21c6cab0efe4cf6a05d02f38_320.mp4'
  },
  {
    videoId: 'w3v1Q3-2qgY',
    title: 'Pookkal Pookkum',
    channelTitle: 'G.V. Prakash Kumar • Madharasapattinam',
    thumbnailUrl: 'https://c.saavncdn.com/960/Madharasapattinam-Tamil-2010-20200627073521-500x500.jpg',
    durationFormatted: '6:36',
    durationMs: 396000,
    viewCountFormatted: '17M views',
    audioUrl: 'https://aac.saavncdn.com/960/c8427f9eff13f8e0e9cce7ad2f2aa841_320.mp4'
  },
  {
    videoId: 'xVj_kL3o9yE',
    title: 'Adiye',
    channelTitle: 'Dhibu Ninan Thomas • Bachelor (Original Motion Picture Soundtrack)',
    thumbnailUrl:
      'https://c.saavncdn.com/357/Bachelor-Original-Motion-Picture-Soundtrack-Tamil-2021-20251024161148-500x500.jpg',
    durationFormatted: '4:32',
    durationMs: 272000,
    viewCountFormatted: '47M views',
    audioUrl: 'https://aac.saavncdn.com/357/89d4fdbeb263f4b44c7eb6294a269831_320.mp4'
  },
  {
    videoId: 'oRdxUFDoQe0',
    title: 'Kannaana Kanney',
    channelTitle: 'Sid Sriram • Viswasam',
    thumbnailUrl: 'https://c.saavncdn.com/006/Viswasam-Tamil-2020-20240321064638-500x500.jpg',
    durationFormatted: '4:30',
    durationMs: 270000,
    viewCountFormatted: '8M views',
    audioUrl: 'https://aac.saavncdn.com/006/f9a196d028821f91c62b5d331ca456fc_320.mp4'
  },
  {
    videoId: 'P36eX-189fU',
    title: 'Munbe Vaa',
    channelTitle: 'Naresh Iyer • Sillunu Oru Kadhal',
    thumbnailUrl: 'https://c.saavncdn.com/106/Jillunu-Oru-Kadhal-2006-500x500.jpg',
    durationFormatted: '5:58',
    durationMs: 358000,
    viewCountFormatted: '38M views',
    audioUrl: 'https://aac.saavncdn.com/595/86c6a67ff7120d287cb4484ea020f488_320.mp4'
  },
  {
    videoId: '3r88BwZ_g60',
    title: 'Vinnaithaandi Varuvaayaa',
    channelTitle: 'A.R. Rahman • Vinnaithaandi Varuvaayaa',
    thumbnailUrl: 'https://c.saavncdn.com/880/Vinnaithaandi-Varuvaayaa-Tamil-2010-20260120201226-500x500.jpg',
    durationFormatted: '3:12',
    durationMs: 192000,
    viewCountFormatted: '2M views',
    audioUrl: 'https://aac.saavncdn.com/880/9d7a6106cef53e28c144752d41e68db7_320.mp4'
  },
  {
    videoId: 'kLp65mG9w1Y',
    title: 'Anbil Avan',
    channelTitle: 'A.R. Rahman • This is Kaadhal',
    thumbnailUrl: 'https://c.saavncdn.com/868/This-is-Kaadhal-3-Tamil-2026-20260206191032-500x500.jpg',
    durationFormatted: '4:11',
    durationMs: 251000,
    viewCountFormatted: '8M views',
    audioUrl: 'https://aac.saavncdn.com/868/10daa8b59e5e48c519fbc8141b9b5af5_320.mp4'
  },
  {
    videoId: '4Gq8o0y123E',
    title: 'Kanave Kanave',
    channelTitle: 'Anirudh Ravichander • David',
    thumbnailUrl: 'https://c.saavncdn.com/470/David-2012-500x500.jpg',
    durationFormatted: '4:46',
    durationMs: 286000,
    viewCountFormatted: '37M views',
    audioUrl: 'https://aac.saavncdn.com/470/763cdd1fafbf1c17a5996ec8ff1e5c0a_320.mp4'
  },
  {
    videoId: '9L0sW81a33w',
    title: 'Sirikkadhey',
    channelTitle: 'Vignesh Shivan • Remo',
    thumbnailUrl: 'https://c.saavncdn.com/110/Remo-Tamil-2016-500x500.jpg',
    durationFormatted: '4:05',
    durationMs: 245000,
    viewCountFormatted: '8M views',
    audioUrl: 'https://aac.saavncdn.com/110/2ec3baf8b62e4d69335de93d3685325e_320.mp4'
  },
  {
    videoId: 'e3X9827fghY',
    title: 'Neeyum Naanum Anbe',
    channelTitle: 'Hiphop Tamizha • Imaikkaa Nodigal (Original Motion Picture Soundtrack)',
    thumbnailUrl:
      'https://c.saavncdn.com/015/Imaikkaa-Nodigal-Original-Motion-Picture-Soundtrack-Tamil-2018-20251026054442-500x500.jpg',
    durationFormatted: '4:45',
    durationMs: 285000,
    viewCountFormatted: '15M views',
    audioUrl: 'https://aac.saavncdn.com/015/1071c160f1c11471287b4015f5dedf62_320.mp4'
  },
  {
    videoId: 'P6q9oK3Lw77',
    title: 'Annul Maelae',
    channelTitle: 'Harris Jayaraj • Vaaranam Aayiram',
    thumbnailUrl: 'https://c.saavncdn.com/635/Vaaranam-Aayiram-Tamil-2008-20190629141128-500x500.jpg',
    durationFormatted: '5:22',
    durationMs: 322000,
    viewCountFormatted: '9M views',
    audioUrl: 'https://aac.saavncdn.com/635/8a800cc2cb33a59266193c0d1c028601_320.mp4'
  },
  {
    videoId: '5w21q9Y68wY',
    title: 'Mental Manadhil',
    channelTitle: 'A.R. Rahman • O Kadhal Kanmani',
    thumbnailUrl: 'https://c.saavncdn.com/336/O-Kadhal-Kanmani-Tamil-2015-20200805153450-500x500.jpg',
    durationFormatted: '3:30',
    durationMs: 210000,
    viewCountFormatted: '14M views',
    audioUrl: 'https://aac.saavncdn.com/336/811c2d826d85ef779a7151da9b9b9273_320.mp4'
  },
  {
    videoId: 'gvyUuxdRdR4',
    title: 'Kutti Story',
    channelTitle: 'Vijay • Master',
    thumbnailUrl: 'https://c.saavncdn.com/347/Master-Tamil-2020-20200316084627-500x500.jpg',
    durationFormatted: '5:02',
    durationMs: 302000,
    viewCountFormatted: '24M views',
    audioUrl: 'https://aac.saavncdn.com/347/a9a507593fa9b9d0f1b448d0fb8c4f07_320.mp4'
  },
  {
    videoId: 'bo_efYhYU2A',
    title: 'Matta',
    channelTitle: 'Yuvan Shankar Raja • The Greatest Of All Time - (Tamil)',
    thumbnailUrl: 'https://c.saavncdn.com/829/The-Greatest-Of-All-Time-Tamil-Tamil-2024-20240903191033-500x500.jpg',
    durationFormatted: '3:32',
    durationMs: 212000,
    viewCountFormatted: '7M views',
    audioUrl: 'https://aac.saavncdn.com/829/8e65980abebbac11a936160f2b5b9a3e_320.mp4'
  }
].map((s) => ({ ...s, language: 'tamil' }))

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
  const [showPlanModal, setShowPlanModal] = useState(false)

  // Isai Connect Sync state
  const [remotePlaybackState, setRemotePlaybackState] = useState<PlaybackStateSync | null>(null)
  const [connectedDevices, setConnectedDevices] = useState<DeviceInfo[]>([])
  const [isSeparatePlayback, setIsSeparatePlayback] = useState<boolean>(() =>
    storageService.isMultiDevicePlaybackSeparate()
  )
  const myDeviceId = IsaiConnectService.getMyDeviceId()

  const searchTimerRef = useRef<number | null>(null)
  const toastTimeoutRef = useRef<any>(null)

  const handleCloseToast = useCallback(() => {
    if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current)
    setToast(null)
  }, [])

  const showToast = useCallback((text: string, type: 'success' | 'error' | 'info' = 'success') => {
    if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current)
    setToast({ id: Date.now().toString(), text, type })
    toastTimeoutRef.current = setTimeout(() => {
      setToast(null)
    }, 2500)
  }, [])

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

    const searchParams = new URLSearchParams(window.location.search)
    const roomParam = searchParams.get('room')
    let roomCodeToJoin = roomParam
    if (!roomCodeToJoin && path.startsWith('/room/')) {
      roomCodeToJoin = path.replace('/room/', '').trim()
    }
    if (roomCodeToJoin) {
      ListenTogetherService.joinRoom(roomCodeToJoin)
        .then((joined) => {
          showToast(`Joined Room: ${joined.roomCode} 🎧`, 'success')
        })
        .catch((error) => {
          showToast(error.message || 'Unable to join room.', 'error')
        })
    }
  }, [])

  // Ref to handlePlaySong to avoid circular useEffect dependencies
  const handlePlaySongRef =
    useRef<(song: Song, queue?: Song[], isAdvancing?: boolean, forceLocal?: boolean) => Promise<void>>()
  const playbackQueueRef = useRef<Song[]>([])
  playbackQueueRef.current = playbackQueue
  const currentQueueIndexRef = useRef<number>(0)
  currentQueueIndexRef.current = currentQueueIndex
  const currentPlayingSongRef = useRef<Song | null>(null)
  currentPlayingSongRef.current = currentPlayingSong
  const recommendationGenerationRef = useRef<number>(0)
  const sessionSeedRef = useRef<Song | null>(null)

  // Initialize ISAI Connect service
  useEffect(() => {
    if (!user) return
    const activeUserId = user.email || 'user_guest'
    IsaiConnectService.initialize(activeUserId, user.name)

    const unsubDevices = IsaiConnectService.subscribeDevices((devices) => {
      setConnectedDevices(devices)
    })

    const unsubState = IsaiConnectService.subscribePlaybackState((state) => {
      setRemotePlaybackState(state)
      // In separate multi-device mode, never hijack or auto-play transferred state
      if (storageService.isMultiDevicePlaybackSeparate()) {
        return
      }
      // When another device transfers playback to this Web instance
      if (
        state &&
        state.currentDeviceId === myDeviceId &&
        state.updatedByDeviceId &&
        state.updatedByDeviceId !== myDeviceId &&
        state.currentTitle
      ) {
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
      if (typeof prefs.isMultiDevicePlaybackSeparate === 'boolean') {
        storageService.setMultiDevicePlaybackSeparate(prefs.isMultiDevicePlaybackSeparate)
        setIsSeparatePlayback(prefs.isMultiDevicePlaybackSeparate)
      }
    })

    const unsubCmd = IsaiConnectService.subscribeCommands((cmd) => {
      if (cmd.issuedByDeviceId === myDeviceId) return
      // In separate multi-device mode, only accept commands explicitly targeted to this device
      if (
        storageService.isMultiDevicePlaybackSeparate() &&
        (!cmd.targetDeviceId || cmd.targetDeviceId !== myDeviceId)
      ) {
        return
      }
      const isTargetedToMe =
        !cmd.targetDeviceId ||
        cmd.targetDeviceId === myDeviceId ||
        cmd.targetDeviceId.includes('web') ||
        !remotePlaybackState?.currentDeviceId ||
        remotePlaybackState?.currentDeviceId === myDeviceId
      if (!isTargetedToMe) return

      if (cmd.action === 'PLAY_SONG') {
        const s = (cmd.song || {}) as any
        const songId = s.videoId || s.id || cmd.songId
        const songTitle = s.title || cmd.songTitle
        if (songId || songTitle) {
          const currentQ = playbackQueueRef.current || []
          const existingSong = currentQ.find(
            (item) =>
              (songId && item.videoId === songId) ||
              (songTitle && isSameSongOrDuplicate(item, { title: songTitle, videoId: songId }))
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
        const targetSong: Song | null = cmd.song
          ? cmd.song
          : cmd.songId
            ? {
                videoId: cmd.songId,
                title: cmd.songTitle || 'ISAI Track',
                channelTitle: cmd.songArtist || 'ISAI Artist',
                thumbnailUrl: cmd.songArtwork || '',
                audioUrl: cmd.songAudioUrl || '',
                durationFormatted: '3:30',
                durationMs: 210000,
                viewCountFormatted: ''
              }
            : null

        if (targetSong) {
          setPlaybackQueue((prev) => {
            const filtered = prev.filter((s) => s.videoId !== targetSong.videoId)
            const curIdx = currentPlayingSong ? filtered.findIndex((s) => s.videoId === currentPlayingSong.videoId) : -1
            const insertIdx = curIdx >= 0 ? curIdx + 1 : currentQueueIndex >= 0 ? currentQueueIndex + 1 : 0
            const updated = [...filtered]
            updated.splice(insertIdx, 0, targetSong)
            IsaiConnectService.updatePlaybackState({
              queue: updated.map((item) => ({
                id: item.videoId,
                title: item.title,
                artist: item.channelTitle,
                artwork: item.thumbnailUrl
              }))
            })
            return updated
          })
          showToast(`Added "${targetSong.title.slice(0, 20)}..." next in Queue 🎵`)
        }
      } else if (cmd.action === 'PLAY_NEXT_IN_QUEUE') {
        const s = cmd.song as any
        const targetSong: Song | null = s
          ? {
              videoId: s.videoId || s.id || cmd.songId || `remote_${Date.now()}`,
              title: s.title || cmd.songTitle || 'ISAI Track',
              channelTitle: s.channelTitle || s.artist || cmd.songArtist || 'ISAI Artist',
              thumbnailUrl: s.thumbnailUrl || s.artwork || cmd.songArtwork || '',
              audioUrl: s.audioUrl || cmd.songAudioUrl || '',
              durationFormatted: s.durationFormatted || '3:30',
              durationMs: s.durationMs || 210000,
              viewCountFormatted: ''
            }
          : cmd.songId
            ? {
                videoId: cmd.songId,
                title: cmd.songTitle || 'ISAI Track',
                channelTitle: cmd.songArtist || 'ISAI Artist',
                thumbnailUrl: cmd.songArtwork || '',
                audioUrl: cmd.songAudioUrl || '',
                durationFormatted: '3:30',
                durationMs: 210000,
                viewCountFormatted: ''
              }
            : null

        if (targetSong) {
          setPlaybackQueue((prev) => {
            const filtered = prev.filter((item) => item.videoId !== targetSong.videoId)
            const curIdx = currentPlayingSong
              ? filtered.findIndex((item) => item.videoId === currentPlayingSong.videoId)
              : -1
            const insertIdx = curIdx >= 0 ? curIdx + 1 : currentQueueIndex >= 0 ? currentQueueIndex + 1 : 0
            const updated = [...filtered]
            updated.splice(insertIdx, 0, targetSong)

            IsaiConnectService.updatePlaybackState({
              queue: updated.map((item) => ({
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
        console.info('[App] Received synced home songs from mobile account:', syncedSongs.length)
        setTrendingSongs(syncedSongs)
      }
    })

    const unsubFavs = IsaiConnectService.subscribeFavorites((syncedFavs) => {
      if (syncedFavs && syncedFavs.length > 0) {
        setFavorites((prev) => {
          const remoteIds = new Set(syncedFavs.map((s) => s.videoId))
          const localOnly = prev.filter((s) => !remoteIds.has(s.videoId))
          const merged = [...syncedFavs, ...localOnly]
          storageService.setFavorites(merged)
          return merged
        })
      }
    })

    // Real-time synchronization of Razorpay verified Premium subscription from RTDB
    const unsubSub = subscribeToSubscription(activeUserId, (subData) => {
      if (subData) {
        const isPrem =
          subData.subscription_status === 'PREMIUM' &&
          (!subData.subscription_expiry || Date.now() <= subData.subscription_expiry)
        setUser((prev) => {
          if (prev.isPremium !== isPrem || (isPrem && prev.selectedPlan !== subData.plan_type)) {
            const updated = {
              ...prev,
              isPremium: isPrem,
              selectedPlan: isPrem ? subData.plan_type || 'PREMIUM' : 'FREE'
            }
            storageService.setUserProfile(updated)
            return updated
          }
          return prev
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
      unsubSub()
    }
  }, [user.email])

  // Load trending music
  const loadTrending = async (languages?: string[]) => {
    setIsTrendingLoading(true)
    setTrendingError(null)

    try {
      const activeLangs =
        languages && languages.length > 0
          ? languages
          : user.preferredLanguages && user.preferredLanguages.length > 0
            ? user.preferredLanguages
            : ['tamil']
      const data = await musicApi.getTrending(activeLangs)
      if (data && data.length > 0) {
        setTrendingSongs(data)
        IsaiConnectService.syncHomeSongs(data)
      } else {
        setTrendingSongs(INITIAL_CURATED_SONGS)
      }
    } catch (error: any) {
      console.warn('[App] Failed to fetch trending music online, using curated offline list:', error)
      setTrendingSongs(INITIAL_CURATED_SONGS)
    } finally {
      setIsTrendingLoading(false)
    }
  }

  const userLanguagesKey = (user.preferredLanguages || []).join(',')
  useEffect(() => {
    loadTrending(user.preferredLanguages)
  }, [user.email, user.isLoggedIn, userLanguagesKey])

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
      name: newUser.name || user.name || 'Listener',
      email: newUser.email || user.email || '',
      avatar:
        newUser.avatar ||
        (newUser.name ? newUser.name.charAt(0).toUpperCase() : user.name ? user.name.charAt(0).toUpperCase() : 'U'),
      isPremium: newUser.isPremium ?? user.isPremium ?? false,
      selectedPlan: newUser.selectedPlan || user.selectedPlan || 'FREE',
      preferredLanguages: newUser.preferredLanguages || user.preferredLanguages || ['tamil']
    }
    setUser(updatedUser)
    storageService.setUserProfile(updatedUser)
    if (updatedUser.preferredLanguages) {
      IsaiConnectService.syncPreferences({ preferredLanguages: updatedUser.preferredLanguages })
    }
    loadTrending(updatedUser.preferredLanguages)
    showToast(`Welcome back, ${updatedUser.name}!`)
    // Ask user which plan they choose right after login
    setShowPlanModal(true)
  }

  const handlePlanSelect = (plan: 'FREE' | 'PREMIUM') => {
    const isPrem = plan === 'PREMIUM'
    const updatedUser: UserProfile = {
      ...user,
      isPremium: isPrem,
      selectedPlan: plan
    }
    setUser(updatedUser)
    storageService.setUserProfile(updatedUser)
    setShowPlanModal(false)
    showToast(
      isPrem
        ? '💎 ISAI Premium activated! Enjoy High Quality Audio, Multi-Sync & more!'
        : '🆓 ISAI Free selected! Enjoy unlimited music streaming!'
    )
  }

  const handleLogout = async () => {
    try {
      await logoutFirebaseUser()
    } catch (error) {
      console.warn('Firebase logout notice:', error)
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
      .replaceAll(
        / - Topic|VEVO|Official|Channel|Sun TV|Sony Music South|Think Music India|Wunderbar Films|Saregama/gi,
        ''
      )
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

  // Auto-replenish queue and seed-based recommendation system
  const isFetchingRecommendationsRef = useRef(false)
  const isFetchingQueueRef = useRef(false)

  const fetchRelatedRecommendations = async (seedSong: Song, generation: number, manualUpcoming: Song[] = []) => {
    isFetchingRecommendationsRef.current = true
    try {
      const primaryLang =
        detectSongLanguage(seedSong) ||
        (user.preferredLanguages && user.preferredLanguages[0] ? user.preferredLanguages[0].toLowerCase() : 'tamil')
      const artist = extractCleanArtist(seedSong.channelTitle)
      const moodQuery = getRelevantSearchQuery(seedSong, primaryLang)
      const seedMood = detectSongMood(seedSong)
      const era = detectSongEra(seedSong)
      const eraPrefix = era ? `${era} ` : ''

      const queries = [moodQuery]
      if (artist) {
        queries.push(
          `${artist} ${primaryLang} ${eraPrefix}${seedMood === 'MELODY_ROMANCE' ? 'love melody' : 'hit'} songs`
        )
      }
      if (seedMood === 'MELODY_ROMANCE') {
        queries.push(
          `${primaryLang} ${eraPrefix}romantic love melody hit songs`,
          `${primaryLang} ${eraPrefix}feel good love hits`
        )
      } else {
        queries.push(`${primaryLang} ${eraPrefix}evergreen hit songs`)
      }

      // Fetch candidates with bounded retry
      const candidatePromises = queries.map(async (q) => {
        let attempts = 0
        while (attempts < 2) {
          try {
            return await musicApi.searchSongs(q)
          } catch {
            attempts++
          }
        }
        return []
      })

      const fetchedArrays = await Promise.all(candidatePromises)
      const rawCandidates = fetchedArrays.flat()

      // Pool also from existing curated lists if matching language
      const allPool = deduplicateSongs([...rawCandidates, ...trendingSongs, ...INITIAL_CURATED_SONGS, ...favorites])

      const activeLangs =
        user.preferredLanguages && user.preferredLanguages.length > 0
          ? user.preferredLanguages.map((l) => l.toLowerCase())
          : [primaryLang]

      // Filter and score candidates
      const scoredCandidates = allPool
        .filter((c) => {
          if (!c || !c.videoId || c.videoId === seedSong.videoId) return false
          if (isPlaylistOrCompilation(c)) return false
          if (isSameSongOrDuplicate(seedSong, c)) return false
          const cLang = (c.language || detectSongLanguage(c)).toLowerCase()
          if (!activeLangs.includes(cLang)) return false
          return true
        })
        .map((candidate) => ({
          candidate,
          score: scoreSongRelevance(seedSong, candidate, activeLangs, {}, sessionSeedRef.current || seedSong)
        }))
        .filter((item) => item.score > 0)
        .sort((a, b) => b.score - a.score)
        .map((item) => item.candidate)

      // Check if a new recommendation context was started while we were fetching (Cancellation Check)
      if (recommendationGenerationRef.current !== generation) {
        console.info('[App] Recommendation response discarded: superseded by newer seed generation')
        return
      }

      // Update queue asynchronously without interrupting playback
      setPlaybackQueue((prevQueue) => {
        const curSeedId = seedSong.videoId
        const curIdx = prevQueue.findIndex((s) => s.videoId === curSeedId)
        const playedSoFar = prevQueue.slice(0, (curIdx >= 0 ? curIdx : 0) + 1)
        const manualItems =
          manualUpcoming.length > 0
            ? manualUpcoming
            : prevQueue.slice((curIdx >= 0 ? curIdx : 0) + 1).filter((s) => s.isManual)
        const seenIds = new Set([...playedSoFar, ...manualItems].map((s) => s.videoId))

        const newAuto: Song[] = []
        for (const cand of scoredCandidates) {
          if (seenIds.has(cand.videoId) || isSameSongOrDuplicate(seedSong, cand)) continue
          if (manualItems.some((m) => isSameSongOrDuplicate(m, cand))) continue
          seenIds.add(cand.videoId)
          newAuto.push(cand)
          if (newAuto.length >= 35) break
        }

        const combined = [...playedSoFar, ...manualItems, ...newAuto]
        if (!storageService.isMultiDevicePlaybackSeparate()) {
          IsaiConnectService.updatePlaybackState({
            queue: combined.map((item) => ({
              id: item.videoId,
              title: item.title,
              artist: item.channelTitle,
              artwork: item.thumbnailUrl,
              audioUrl: item.audioUrl || ''
            }))
          })
        }
        return combined
      })
    } catch (error) {
      console.warn('[App] fetchRelatedRecommendations error:', error)
    } finally {
      isFetchingRecommendationsRef.current = false
    }
  }

  const ensureEndlessQueue = async (currentSong: Song, currentQueue: Song[], currentIndex: number, force = false) => {
    if (isFetchingQueueRef.current) return
    const remaining = currentQueue.length - 1 - currentIndex
    if (!force && remaining > 3) return

    isFetchingQueueRef.current = true
    try {
      const activeGen = recommendationGenerationRef.current
      const primaryLang =
        detectSongLanguage(currentSong) ||
        (user.preferredLanguages && user.preferredLanguages[0] ? user.preferredLanguages[0].toLowerCase() : 'tamil')
      const moodQuery = getRelevantSearchQuery(currentSong, primaryLang)
      const fetchedResults = await musicApi.searchSongs(moodQuery).catch(() => [])

      if (recommendationGenerationRef.current !== activeGen) return

      const activeLangs =
        user.preferredLanguages && user.preferredLanguages.length > 0
          ? user.preferredLanguages.map((l) => l.toLowerCase())
          : ['tamil']

      const existingIds = new Set(currentQueue.map((s) => s.videoId))
      const newSongs: Song[] = []
      for (const s of fetchedResults) {
        if (!s || !s.videoId || existingIds.has(s.videoId)) continue
        if (isSameSongOrDuplicate(currentSong, s)) continue
        if (sessionSeedRef.current && isSameSongOrDuplicate(sessionSeedRef.current, s)) continue
        if (currentQueue.some((item) => isSameSongOrDuplicate(item, s))) continue

        const songLang = (s.language || detectSongLanguage(s)).toLowerCase()
        if (!activeLangs.includes(songLang)) continue
        if (scoreSongRelevance(currentSong, s, activeLangs, {}, sessionSeedRef.current || currentSong) <= 0) continue

        existingIds.add(s.videoId)
        newSongs.push(s)
        if (newSongs.length >= 15) break
      }

      if (newSongs.length > 0) {
        setPlaybackQueue((prevQueue) => {
          if (recommendationGenerationRef.current !== activeGen) return prevQueue
          const prevIds = new Set(prevQueue.map((item) => item.videoId))
          const toAdd = newSongs.filter(
            (item) => !prevIds.has(item.videoId) && !isSameSongOrDuplicate(currentSong, item)
          )
          if (toAdd.length === 0) return prevQueue
          const updated = deduplicateSongs([...prevQueue, ...toAdd])
          if (!storageService.isMultiDevicePlaybackSeparate()) {
            IsaiConnectService.updatePlaybackState({
              queue: updated.map((item) => ({
                id: item.videoId,
                title: item.title,
                artist: item.channelTitle,
                artwork: item.thumbnailUrl,
                audioUrl: item.audioUrl || ''
              }))
            })
          }
          return updated
        })
      }
    } catch (error) {
      console.warn('[App] ensureEndlessQueue error:', error)
    } finally {
      isFetchingQueueRef.current = false
    }
  }

  // Spotify-style Daily Mixes computed from trending songs and user's preferred languages
  const spotifyDailyMixes = useMemo(() => {
    const primaryLang =
      user.preferredLanguages && user.preferredLanguages[0] ? user.preferredLanguages[0].toUpperCase() : 'TAMIL'

    const anirudhSongs = trendingSongs.filter(
      (s) => s.title?.toLowerCase().includes('anirudh') || s.channelTitle?.toLowerCase().includes('anirudh')
    )
    const arrSongs = trendingSongs.filter(
      (s) =>
        s.title?.toLowerCase().includes('rahman') ||
        s.channelTitle?.toLowerCase().includes('rahman') ||
        s.channelTitle?.toLowerCase().includes('arr')
    )
    const yuvanSongs = trendingSongs.filter(
      (s) =>
        s.title?.toLowerCase().includes('yuvan') ||
        s.channelTitle?.toLowerCase().includes('yuvan') ||
        s.channelTitle?.toLowerCase().includes('u1')
    )

    return [
      {
        id: 'daily_mix_1',
        title: 'Daily Mix 1 • Anirudh Hits',
        subtitle: 'Anirudh, Dhanush, Vijay & club chartbusters',
        gradient: 'linear-gradient(135deg, #1DB954 0%, #121212 100%)',
        coverUrl:
          anirudhSongs[0]?.thumbnailUrl || 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
        songs: anirudhSongs.length > 0 ? anirudhSongs : trendingSongs.slice(0, 8)
      },
      {
        id: 'daily_mix_2',
        title: 'Daily Mix 2 • A.R. Rahman Soul',
        subtitle: 'A.R. Rahman, Bombay Jayashri & timeless melodies',
        gradient: 'linear-gradient(135deg, #7C3AED 0%, #0F0C20 100%)',
        coverUrl:
          arrSongs[0]?.thumbnailUrl ||
          'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
        songs: arrSongs.length > 0 ? arrSongs : trendingSongs.slice(1, 9)
      },
      {
        id: 'daily_mix_3',
        title: 'Daily Mix 3 • Yuvan Drug Melodies',
        subtitle: 'Yuvan Shankar Raja, Harris & night drives',
        gradient: 'linear-gradient(135deg, #2563EB 0%, #080D1A 100%)',
        coverUrl:
          yuvanSongs[0]?.thumbnailUrl || 'https://c.saavncdn.com/276/Maari-2-Tamil-2018-20260203193952-500x500.jpg',
        songs: yuvanSongs.length > 0 ? yuvanSongs : trendingSongs.slice(2, 10)
      },
      {
        id: 'daily_mix_4',
        title: `Top 50 • ${primaryLang}`,
        subtitle: `The most played and trending hits in ${primaryLang}`,
        gradient: 'linear-gradient(135deg, #E11D48 0%, #190A12 100%)',
        coverUrl:
          trendingSongs[0]?.thumbnailUrl || 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
        songs: trendingSongs
      }
    ]
  }, [trendingSongs, user.preferredLanguages])

  // Playback control handlers
  const handlePlaySong = async (
    song: Song,
    queue: Song[] | null = null,
    isAdvancing: boolean = false,
    forceLocal: boolean = false
  ) => {
    const isRemoteActive = Boolean(
      !forceLocal &&
      remotePlaybackState &&
      remotePlaybackState.currentDeviceId &&
      remotePlaybackState.currentDeviceId !== myDeviceId &&
      remotePlaybackState.currentTitle
    )

    if (isRemoteActive) {
      IsaiConnectService.sendCommand('PLAY_SONG', { song })
      const playingDevice = connectedDevices.find((d) => d.deviceId === remotePlaybackState?.currentDeviceId)
      const devName =
        playingDevice?.deviceName ||
        (remotePlaybackState?.currentDeviceId.includes('android') ? 'Mobile Phone' : 'Remote Device')
      showToast(`Playing on ${devName} 📱`)
      return
    }

    setCurrentPlayingSong(song)

    // Case 1: Next / Prev / Autoplay advancing within current playback queue
    if (isAdvancing) {
      const curQ = playbackQueueRef.current || []
      const idx = curQ.findIndex((s) => s.videoId === song.videoId)
      const activeIdx = idx >= 0 ? idx : currentQueueIndexRef.current
      setCurrentQueueIndex(activeIdx)
      ensureEndlessQueue(song, curQ, activeIdx)
    }
    // Case 2: Explicit playlist / album / favorites list played
    else if (queue && queue.length > 1 && queue !== playbackQueue) {
      recommendationGenerationRef.current++
      sessionSeedRef.current = song
      const songIdx = queue.findIndex((s) => s.videoId === song.videoId)
      const activeIdx = songIdx >= 0 ? songIdx : 0
      setPlaybackQueue(queue)
      setCurrentQueueIndex(activeIdx)
      ensureEndlessQueue(song, queue, activeIdx)
    }
    // Case 3: Search result clicked or explicit single song selected -> Starts new recommendation seed!
    else {
      recommendationGenerationRef.current++
      const currentGen = recommendationGenerationRef.current
      sessionSeedRef.current = song

      // Preserve any upcoming items that were explicitly added by the user
      const currentQ = playbackQueueRef.current || []
      const curIdx = currentQueueIndexRef.current
      const manualUpcoming = currentQ.slice(curIdx + 1).filter((s) => s.isManual)

      // Fast initial queue: seed + preserved manual items
      const initialQueue = [song, ...manualUpcoming]
      setPlaybackQueue(initialQueue)
      setCurrentQueueIndex(0)

      // Asynchronously fetch related candidates without delaying playback
      fetchRelatedRecommendations(song, currentGen, manualUpcoming)
    }

    if (!song.audioUrl) {
      try {
        const cleanTitle = cleanHtmlTitle(song.title)
          .replaceAll(/\s*[-|\u2013\u2014].*$/g, '')
          .replaceAll(/\s*\(.*?(official|video|audio|lyrics|hd|4k|song).*?\)/gi, '')
          .replaceAll(/\s*\[.*?(official|video|audio|lyrics|hd|4k|song).*?\]/gi, '')
          .replaceAll(/\.{2,}$/g, '')
          .trim()

        const results = await musicApi.searchSongs(cleanTitle || song.title)
        if (results && results.length > 0 && results[0].audioUrl) {
          const resolvedUrl = results[0].audioUrl
          const updated = { ...song, audioUrl: resolvedUrl }
          setCurrentPlayingSong(updated)
        }
      } catch (error) {
        console.warn('[App] AudioUrl resolution error:', error)
      }
    }
  }
  handlePlaySongRef.current = handlePlaySong

  // Handle playing song selected from search results:
  // Starts playback of this song as the seed and generates related recommendations without enqueuing other search results
  const handlePlaySongFromSearch = (song: Song) => {
    handlePlaySong(song, null, false)
  }

  const handleNextSong = () => {
    const isRemoteActive =
      !isSeparatePlayback &&
      Boolean(
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
    const activeQueue =
      playbackQueue.length > 0 ? playbackQueue : trendingSongs.length > 0 ? trendingSongs : INITIAL_CURATED_SONGS
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
    const isRemoteActive =
      !isSeparatePlayback &&
      Boolean(
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
    const activeQueue =
      playbackQueue.length > 0 ? playbackQueue : trendingSongs.length > 0 ? trendingSongs : INITIAL_CURATED_SONGS
    let curIdx = activeQueue.findIndex((s) => s.videoId === currentPlayingSong.videoId)
    if (curIdx === -1) curIdx = currentQueueIndex

    const prevIdx = curIdx > 0 ? curIdx - 1 : activeQueue.length - 1
    const prevSong = activeQueue[prevIdx]
    if (prevSong) {
      handlePlaySong(prevSong, activeQueue, true)
    }
  }

  // Add to Queue Handler: Inserts after existing manually queued tracks and before automatic recommendations
  const handleAddToQueue = (song: Song) => {
    const isRemoteActive = Boolean(
      remotePlaybackState &&
      remotePlaybackState.currentDeviceId &&
      remotePlaybackState.currentDeviceId !== myDeviceId &&
      remotePlaybackState.currentTitle
    )

    const manualSong: Song = { ...song, isManual: true }

    if (isRemoteActive) {
      IsaiConnectService.sendCommand('ADD_TO_QUEUE', {
        songId: manualSong.videoId,
        songTitle: manualSong.title,
        songArtist: manualSong.channelTitle,
        songArtwork: manualSong.thumbnailUrl,
        songAudioUrl: manualSong.audioUrl,
        song: manualSong
      })
      showToast(`Added to Mobile Queue 📱`)
      return
    }

    if (!currentPlayingSong) {
      handlePlaySong(manualSong, [manualSong])
      showToast(`Playing "${manualSong.title.slice(0, 25)}..." 🎵`)
      return
    }

    setPlaybackQueue((prev) => {
      const filtered = prev.filter((s) => s.videoId !== manualSong.videoId)
      const curIdx = currentPlayingSong
        ? filtered.findIndex((item) => item.videoId === currentPlayingSong.videoId)
        : currentQueueIndexRef.current

      // Find the last index of manual items in upcoming queue
      let lastManualIdx = -1
      for (let i = curIdx >= 0 ? curIdx + 1 : 0; i < filtered.length; i++) {
        if (filtered[i].isManual) {
          lastManualIdx = i
        }
      }

      // Insert after last manual item, or right after current song (before auto items)
      const insertIdx = lastManualIdx >= 0 ? lastManualIdx + 1 : curIdx >= 0 ? curIdx + 1 : 0
      const updated = [...filtered]
      updated.splice(insertIdx, 0, manualSong)

      IsaiConnectService.updatePlaybackState({
        queue: updated.map((item) => ({
          id: item.videoId,
          title: item.title,
          artist: item.channelTitle,
          artwork: item.thumbnailUrl,
          audioUrl: item.audioUrl || ''
        }))
      })
      showToast(`Added "${manualSong.title.slice(0, 20)}..." to Queue 🎵`)
      return updated
    })
  }

  // Play Next in Queue Handler: Inserts chosen song immediately after the current track
  const handlePlayNext = (song: Song) => {
    const isRemoteActive = Boolean(
      remotePlaybackState &&
      remotePlaybackState.currentDeviceId &&
      remotePlaybackState.currentDeviceId !== myDeviceId &&
      remotePlaybackState.currentTitle
    )

    const manualSong: Song = { ...song, isManual: true }

    if (isRemoteActive) {
      IsaiConnectService.sendCommand('PLAY_NEXT_IN_QUEUE', {
        songId: manualSong.videoId,
        songTitle: manualSong.title,
        songArtist: manualSong.channelTitle,
        songArtwork: manualSong.thumbnailUrl,
        songAudioUrl: manualSong.audioUrl,
        song: manualSong
      })
      showToast(`Will play next on Mobile 📱`)
      return
    }

    if (!currentPlayingSong) {
      handlePlaySong(manualSong, [manualSong])
      showToast(`Playing "${manualSong.title.slice(0, 25)}..." 🎵`)
      return
    }

    setPlaybackQueue((prev) => {
      const filtered = prev.filter((s) => s.videoId !== manualSong.videoId)
      const curIdx = currentPlayingSong
        ? filtered.findIndex((s) => s.videoId === currentPlayingSong.videoId)
        : currentQueueIndexRef.current
      const insertIdx = curIdx >= 0 ? curIdx + 1 : 0
      const updated = [...filtered]
      updated.splice(insertIdx, 0, manualSong)

      IsaiConnectService.updatePlaybackState({
        queue: updated.map((item) => ({
          id: item.videoId,
          title: item.title,
          artist: item.channelTitle,
          artwork: item.thumbnailUrl,
          audioUrl: item.audioUrl || ''
        }))
      })
      showToast(`Will play next: "${manualSong.title.slice(0, 25)}..." ⏭️`)
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
        const userLangs =
          user.preferredLanguages && user.preferredLanguages.length > 0 ? user.preferredLanguages : ['tamil']
        const primaryLang = userLangs[0].toLowerCase()
        const favArtists = favorites.map((f) => f.channelTitle).filter(Boolean)
        const intent = SmartSearchEngine.parseQuery(query, userLangs, favArtists)

        const cleanQ = query.trim()
        const qLower = cleanQ.toLowerCase()
        const hasLangNameInQuery = userLangs.some((l) => qLower.includes(l.toLowerCase()))

        const queriesToFetch = [intent.optimizedSearchQuery]
        if (cleanQ.toLowerCase() !== intent.optimizedSearchQuery.toLowerCase()) {
          queriesToFetch.push(cleanQ)
        }
        if (!hasLangNameInQuery && !qLower.startsWith(primaryLang)) {
          queriesToFetch.push(`${primaryLang} ${cleanQ}`)
        }
        if (intent.unmatchedTerms.length > 0 && !qLower.includes('song') && !qLower.includes('paatu')) {
          queriesToFetch.push(`${cleanQ} songs`)
        }

        const fetchedArrays = await Promise.all(queriesToFetch.map((q) => musicApi.searchSongs(q).catch(() => [])))
        const results = fetchedArrays.flat()

        const deduped = deduplicateSongs(results || [])
        const ranked = [...deduped].sort((a, b) => {
          return (
            SmartSearchEngine.rankSong(b, intent, userLangs, favArtists) -
            SmartSearchEngine.rankSong(a, intent, userLangs, favArtists)
          )
        })

        setSearchResults(ranked)
      } catch (error: unknown) {
        console.warn('Search failed:', error)
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

  const handleSelectPlaylistDetail = (
    title: string,
    subtitle: string,
    songs: Song[],
    coverUrl?: string,
    gradient?: string
  ) => {
    setSelectedPlaylistDetail({ title, subtitle, songs, coverUrl, gradient })
    setCurrentTab('playlist-detail')
  }

  if (isUpdateView) {
    return <UpdatePage />
  }

  if (isVerifyView) {
    return <VerifyEmailPage onNavigateHome={() => (window.location.href = '/')} />
  }

  const handleSelectLanguage = (newLang: string) => {
    const updatedLangs = [newLang.toLowerCase()]
    const updatedUser: UserProfile = {
      ...user,
      preferredLanguages: updatedLangs
    }
    setUser(updatedUser)
    storageService.setUserProfile(updatedUser)
    IsaiConnectService.syncPreferences({ preferredLanguages: updatedLangs })
    loadTrending(updatedLangs)
    showToast(`Switched music language to ${newLang}! 🌐`)
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
          picksSongs={trendingSongs}
          newReleases={trendingSongs.slice(5)}
          mostPlayedSongs={trendingSongs}
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
          userProfile={user}
          selectedLanguage={
            user.preferredLanguages?.[0]
              ? user.preferredLanguages[0].charAt(0).toUpperCase() + user.preferredLanguages[0].slice(1)
              : 'Tamil'
          }
          onSelectLanguage={handleSelectLanguage}
          onOpenProfile={() => setCurrentTab('profile')}
          onOpenSearch={() => setCurrentTab('search')}
          listeningHistory={storageService.getRecentlyPlayed()}
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
          onOpenPlanModal={() => setShowPlanModal(true)}
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
      <Toast toast={toast} onClose={handleCloseToast} />

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
          if (!storageService.isMultiDevicePlaybackSeparate()) {
            IsaiConnectService.updatePlaybackState({
              queue: newQueue.map((item) => ({
                id: item.videoId,
                title: item.title,
                artist: item.channelTitle,
                artwork: item.thumbnailUrl,
                audioUrl: item.audioUrl || ''
              }))
            })
          }
        }}
        onRemoveQueueItem={(indexToRemove) => {
          setPlaybackQueue((prev) => {
            const updated = prev.filter((_, i) => i !== indexToRemove)
            if (!storageService.isMultiDevicePlaybackSeparate()) {
              IsaiConnectService.updatePlaybackState({
                queue: updated.map((item) => ({
                  id: item.videoId,
                  title: item.title,
                  artist: item.channelTitle,
                  artwork: item.thumbnailUrl,
                  audioUrl: item.audioUrl || ''
                }))
              })
            }
            return updated
          })
        }}
        onClearQueue={() => {
          if (currentPlayingSong) {
            setPlaybackQueue([currentPlayingSong])
            if (!storageService.isMultiDevicePlaybackSeparate()) {
              IsaiConnectService.updatePlaybackState({
                queue: [
                  {
                    id: currentPlayingSong.videoId,
                    title: currentPlayingSong.title,
                    artist: currentPlayingSong.channelTitle,
                    artwork: currentPlayingSong.thumbnailUrl
                  }
                ]
              })
            }
          } else {
            setPlaybackQueue([])
            if (!storageService.isMultiDevicePlaybackSeparate()) {
              IsaiConnectService.updatePlaybackState({ queue: [] })
            }
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

      {showPlanModal && (
        <PlanSelectionModal
          isOpen={showPlanModal}
          currentPlan={(user.selectedPlan as 'FREE' | 'PREMIUM') || (user.isPremium ? 'PREMIUM' : 'FREE')}
          onSelectPlan={handlePlanSelect}
          onClose={() => setShowPlanModal(false)}
        />
      )}
    </MainLayout>
  )
}
