import { storageService } from '@shared/services/storageService'
import { deduplicateSongs } from '@shared/utils/formatters'
import { isSongInLanguage } from '@shared/utils/relevance'
import { ChevronLeft, ChevronRight, Globe, Heart, Play, Search } from 'lucide-react'
import React, { useRef, useState } from 'react'
import { ErrorBanner } from '../components/ErrorBanner'
import { SkeletonSongCard } from '../components/SkeletonLoader'
import type { Artist } from '../components/ArtistCard'
import type { UserProfile } from '../components/LoginModal'
import type { Song } from '@shared/models/song'

interface HomePageProps {
  trendingSongs: Song[]
  isLoading: boolean
  error: string | null
  onSelectCategory: (query: string) => void
  onRetry: () => void
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong?: (song: Song, queue?: Song[]) => void
  onAddToPlaylist?: (song: Song) => void
  onAddToQueue?: (song: Song) => void
  onPlayNext?: (song: Song) => void
  onSelectArtist?: (artist: Artist) => void
  currentSong?: Song | null
  isPlaying?: boolean
  userProfile?: UserProfile
  selectedLanguage?: string
  onSelectLanguage?: (lang: string) => void
  onOpenSearch?: () => void
  onOpenProfile?: () => void
  listeningHistory?: Song[]
  onSeeAllNewReleases?: () => void
  dailyMixes?: any[]
  onSelectPlaylistDetail?: any
  picksSongs?: Song[]
  newReleases?: Song[]
  mostPlayedSongs?: Song[]
}

const SUPPORTED_LANGUAGES = ['Tamil', 'Telugu', 'Hindi', 'Kannada', 'Malayalam', 'English']

const POPULAR_ARTISTS_BY_LANG: Record<string, Artist[]> = {
  Tamil: [
    {
      name: 'Anirudh Ravichander',
      role: 'Composer & Singer',
      image: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
      query: 'Anirudh Ravichander Tamil hits',
      followers: '24.5M Listeners'
    },
    {
      name: 'A.R. Rahman',
      role: 'Composer & Maestro',
      image: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
      query: 'A R Rahman Tamil hits',
      followers: '32.1M Listeners'
    },
    {
      name: 'Yuvan Shankar Raja',
      role: 'Composer & Singer',
      image: 'https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_002_20180802174245_500x500.jpg',
      query: 'Yuvan Shankar Raja Tamil hits',
      followers: '19.8M Listeners'
    },
    {
      name: 'Harris Jayaraj',
      role: 'Composer',
      image: 'https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg',
      query: 'Harris Jayaraj Tamil hits',
      followers: '15.4M Listeners'
    },
    {
      name: 'Sid Sriram',
      role: 'Playback Singer',
      image: 'https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg',
      query: 'Sid Sriram Tamil hits',
      followers: '14.2M Listeners'
    }
  ],
  Telugu: [
    {
      name: 'Devi Sri Prasad',
      role: 'Composer & Singer',
      image: 'https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg',
      query: 'Devi Sri Prasad Telugu hits',
      followers: '18.2M Listeners'
    },
    {
      name: 'Thaman S',
      role: 'Music Director',
      image: 'https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg',
      query: 'Thaman S Telugu hits',
      followers: '16.5M Listeners'
    },
    {
      name: 'M.M. Keeravani',
      role: 'Academy Maestro',
      image: 'https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg',
      query: 'MM Keeravani Telugu hits',
      followers: '12.8M Listeners'
    },
    {
      name: 'Sid Sriram',
      role: 'Singer',
      image: 'https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg',
      query: 'Sid Sriram Telugu hits',
      followers: '14.2M Listeners'
    }
  ],
  Hindi: [
    {
      name: 'Arijit Singh',
      role: 'Playback Singer',
      image: 'https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg',
      query: 'Arijit Singh Hindi hits',
      followers: '45.1M Listeners'
    },
    {
      name: 'Pritam',
      role: 'Composer',
      image: 'https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg',
      query: 'Pritam Hindi hits',
      followers: '28.4M Listeners'
    },
    {
      name: 'Shreya Ghoshal',
      role: 'Singer',
      image: 'https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg',
      query: 'Shreya Ghoshal Hindi hits',
      followers: '25.6M Listeners'
    }
  ],
  Malayalam: [
    {
      name: 'Sushin Shyam',
      role: 'Composer & Singer',
      image: 'https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg',
      query: 'Sushin Shyam Malayalam hits',
      followers: '8.4M Listeners'
    },
    {
      name: 'Shaan Rahman',
      role: 'Composer',
      image: 'https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg',
      query: 'Shaan Rahman Malayalam hits',
      followers: '6.2M Listeners'
    }
  ],
  Kannada: [
    {
      name: 'Ravi Basrur',
      role: 'Music Director',
      image: 'https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg',
      query: 'Ravi Basrur Kannada hits',
      followers: '7.8M Listeners'
    },
    {
      name: 'Vijay Prakash',
      role: 'Playback Singer',
      image: 'https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg',
      query: 'Vijay Prakash Kannada hits',
      followers: '5.9M Listeners'
    }
  ],
  English: [
    {
      name: 'Ed Sheeran',
      role: 'Singer-Songwriter',
      image: 'https://c.saavncdn.com/artists/Ed_Sheeran_002_20250625073038_500x500.jpg',
      query: 'Ed Sheeran top popular songs',
      followers: '85.2M Listeners'
    },
    {
      name: 'Taylor Swift',
      role: 'Global Pop Icon',
      image: 'https://c.saavncdn.com/artists/Taylor_Swift_003_20200226074119_500x500.jpg',
      query: 'Taylor Swift top hit songs',
      followers: '92.4M Listeners'
    }
  ]
}

export const HomePage: React.FC<HomePageProps> = ({
  trendingSongs,
  isLoading,
  error,
  onSelectCategory,
  onRetry,
  isFavorite,
  onToggleFavorite,
  onPlaySong,
  onSelectArtist,
  currentSong,
  isPlaying = false,
  userProfile,
  selectedLanguage: propLanguage,
  onSelectLanguage,
  onOpenSearch,
  listeningHistory = [],
  onSeeAllNewReleases,
  picksSongs: propPicks,
  newReleases: propNewReleases,
  mostPlayedSongs: propMostPlayed
}) => {
  const [showLangDropdown, setShowLangDropdown] = useState(false)

  // Language Resolution
  const rawLanguage = propLanguage || userProfile?.preferredLanguages?.[0] || 'Tamil'
  const activeLanguage =
    rawLanguage.toLowerCase().trim() === 'ta' || rawLanguage.toLowerCase().trim() === 'tam'
      ? 'Tamil'
      : rawLanguage.charAt(0).toUpperCase() + rawLanguage.slice(1)
  const rawName = userProfile?.name?.trim()
  const userName = (rawName && !rawName.toLowerCase().startsWith('jeeva ⚡') && rawName.toLowerCase() !== 'jeeva ⚡')
    ? rawName
    : 'Listener'

  // Ref for horizontal scrolling
  const historyRowRef = useRef<HTMLDivElement>(null)
  const picksRowRef = useRef<HTMLDivElement>(null)
  const newReleasesRowRef = useRef<HTMLDivElement>(null)

  const scrollRow = (ref: React.RefObject<HTMLDivElement>, dir: 'left' | 'right') => {
    if (ref.current) {
      const scrollAmount = dir === 'left' ? -360 : 360
      ref.current.scrollBy({ left: scrollAmount, behavior: 'smooth' })
    }
  }

  const deduplicated = deduplicateSongs(trendingSongs)

  // Section 3: Continue Listening (filtered by history, hidden if empty)
  const actualHistory = listeningHistory.length > 0 ? listeningHistory : storageService.getRecentlyPlayed()

  const activeLangs = [
    activeLanguage.toLowerCase().trim() === 'ta' || activeLanguage.toLowerCase().trim() === 'tam'
      ? 'tamil'
      : activeLanguage.toLowerCase().trim()
  ]

  // Section 4: Picks For You (Authoritative / Personalized pool - Strictly in active language, min 30-35 songs)
  const validPicks = (propPicks || []).filter((s) => isSongInLanguage(s, activeLangs))
  const validTrending = deduplicated.filter((s) => isSongInLanguage(s, activeLangs))
  const rawPicks = deduplicateSongs([...validPicks, ...validTrending])
  const picksSongs = (rawPicks.length >= 5 ? rawPicks : deduplicated).slice(0, 35)

  // Section 5: New Releases (Strictly in active language, min 30-35 songs)
  const validPropNew = (propNewReleases || []).filter((s) => isSongInLanguage(s, activeLangs))
  const nowMs = Date.now()
  const thirtyDaysMs = 30 * 24 * 60 * 60 * 1000
  const verifiedNewReleases = validTrending.filter((song) => {
    const pub = (song as any).publishedAt
    if (!pub) return true
    const pubDate = new Date(pub).getTime()
    return !Number.isNaN(pubDate) && nowMs - pubDate <= thirtyDaysMs
  })
  const rawNewReleases = deduplicateSongs([
    ...validPropNew,
    ...verifiedNewReleases,
    ...validTrending.slice(5)
  ])
  const displayNewReleases = (rawNewReleases.length >= 5 ? rawNewReleases : deduplicated.slice(5)).slice(0, 35)

  // Section 6: Mood Cards
  const moodCards = [
    {
      title: 'Love',
      subtitle: 'Romantic Melodies',
      emoji: '💖',
      image: 'https://images.unsplash.com/photo-1518199266791-5375a83190b7?auto=format&fit=crop&w=600&q=80',
      query: `${activeLanguage} love romantic hit songs`,
      gradient: 'linear-gradient(135deg, rgba(236,72,153,0.45), rgba(139,92,246,0.45))'
    },
    {
      title: 'Chill',
      subtitle: 'Lo-Fi & Relax',
      emoji: '☕',
      image: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80',
      query: `${activeLanguage} lo-fi chill rain songs`,
      gradient: 'linear-gradient(135deg, rgba(99,102,241,0.45), rgba(168,85,247,0.45))'
    },
    {
      title: 'Gym',
      subtitle: 'Workout Energy',
      emoji: '⚡',
      image: 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?auto=format&fit=crop&w=600&q=80',
      query: `${activeLanguage} energetic gym workout bgm beats`,
      gradient: 'linear-gradient(135deg, rgba(16,185,129,0.45), rgba(6,182,212,0.45))'
    },
    {
      title: 'Party',
      subtitle: 'Dance & Beats',
      emoji: '🎉',
      image: 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=600&q=80',
      query: `${activeLanguage} party dance kuthu fast beat songs`,
      gradient: 'linear-gradient(135deg, rgba(244,63,94,0.45), rgba(249,115,22,0.45))'
    },
    {
      title: 'Travel',
      subtitle: 'Road Trip Vibes',
      emoji: '🚗',
      image: 'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=600&q=80',
      query: `${activeLanguage} road trip travel songs`,
      gradient: 'linear-gradient(135deg, rgba(245,158,11,0.45), rgba(239,68,68,0.45))'
    },
    {
      title: 'Sad',
      subtitle: 'Heartbreak Soul',
      emoji: '🌧️',
      image: 'https://images.unsplash.com/photo-1518495973542-4542c06a5843?auto=format&fit=crop&w=600&q=80',
      query: `${activeLanguage} sad emotional heartbreak breakup songs`,
      gradient: 'linear-gradient(135deg, rgba(59,130,246,0.45), rgba(99,102,241,0.45))'
    }
  ]

  // Section 7: Artists
  const displayArtists = POPULAR_ARTISTS_BY_LANG[activeLanguage] || POPULAR_ARTISTS_BY_LANG.Tamil

  // Section 8: Most Played Songs (min 30-35 songs)
  const validPropMost = (propMostPlayed || []).filter((s) => isSongInLanguage(s, activeLangs))
  const rawMost = deduplicateSongs([...validPropMost, ...validTrending])
  const mostPlayedList = (rawMost.length >= 5 ? rawMost : deduplicated).slice(0, 35)

  if (isLoading && deduplicated.length === 0) {
    return (
      <div style={{ paddingBottom: '120px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
        <div style={{ height: '60px', background: 'var(--surface-card)', borderRadius: '16px' }} />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat( auto-fit, minmax(200px, 1fr) )', gap: '16px' }}>
          {[1, 2, 3, 4, 5, 6].map((n) => (
            <SkeletonSongCard key={n} />
          ))}
        </div>
      </div>
    )
  }

  if (error && deduplicated.length === 0) {
    return (
      <div style={{ paddingBottom: '120px', paddingTop: '40px' }}>
        <ErrorBanner message={error} onRetry={onRetry} />
      </div>
    )
  }

  return (
    <div style={{ paddingBottom: '120px', display: 'flex', flexDirection: 'column', gap: '32px' }}>
      {/* 1. Header Section */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px'
        }}
      >
        <div>
          <h1
            style={{
              fontSize: '24px',
              fontWeight: 900,
              color: 'var(--text-primary)',
              margin: 0,
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}
          >
            Welcome, <span style={{ color: 'var(--isai-lime)' }}>{userName}</span> 👋
          </h1>
          <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: '4px 0 0 0' }}>
            Enjoying {activeLanguage} Music on ISAI
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {/* Language Selector Dropdown */}
          <div style={{ position: 'relative' }}>
            <button
              onClick={() => setShowLangDropdown(!showLangDropdown)}
              style={{
                padding: '6px 14px',
                borderRadius: '20px',
                background: 'rgba(200, 255, 0, 0.15)',
                color: 'var(--isai-lime)',
                border: '1px solid rgba(200, 255, 0, 0.4)',
                fontWeight: 800,
                fontSize: '12px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}
            >
              <Globe size={14} />
              <span>{activeLanguage}</span>
            </button>

            {showLangDropdown && (
              <div
                onClick={(e) => e.stopPropagation()}
                style={{
                  position: 'absolute',
                  top: '40px',
                  right: 0,
                  backgroundColor: 'rgba(20, 20, 28, 0.95)',
                  backdropFilter: 'blur(20px)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: '14px',
                  padding: '8px',
                  zIndex: 999,
                  minWidth: '140px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '4px',
                  boxShadow: '0 10px 30px rgba(0,0,0,0.8)'
                }}
              >
                {SUPPORTED_LANGUAGES.map((lang) => {
                  const isSel = (lang as any).equalsIgnoreCase
                    ? (lang as any).equalsIgnoreCase(activeLanguage)
                    : lang.toLowerCase() === activeLanguage.toLowerCase()
                  return (
                    <button
                      key={lang}
                      onClick={() => {
                        onSelectLanguage?.(lang)
                        setShowLangDropdown(false)
                      }}
                      style={{
                        padding: '8px 12px',
                        borderRadius: '8px',
                        background: isSel ? 'rgba(200, 255, 0, 0.15)' : 'transparent',
                        color: isSel ? 'var(--isai-lime)' : 'var(--text-primary)',
                        fontWeight: isSel ? 800 : 500,
                        fontSize: '13px',
                        border: 'none',
                        cursor: 'pointer',
                        textAlign: 'left'
                      }}
                    >
                      {lang} {isSel ? '✓' : ''}
                    </button>
                  )
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 2. Full Width Search Bar */}
      <div
        onClick={onOpenSearch}
        style={{
          width: '100%',
          padding: '14px 18px',
          borderRadius: '16px',
          background: 'var(--surface-card)',
          border: '1.5px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          cursor: 'pointer',
          transition: 'all 0.2s ease'
        }}
      >
        <Search size={20} style={{ color: 'var(--isai-lime)' }} />
        <span style={{ fontSize: '14px', color: 'var(--text-muted)', fontWeight: 500 }}>
          Search songs, artists, albums…
        </span>
      </div>

      {/* 3. Continue Listening (Hidden if no history) */}
      {actualHistory.length > 0 && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2
              style={{
                fontSize: '18px',
                fontWeight: 800,
                color: 'var(--text-primary)',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}
            >
              Continue Listening 🎧
            </h2>
            <div style={{ display: 'flex', gap: '6px' }}>
              <button className="control-btn" onClick={() => scrollRow(historyRowRef, 'left')}>
                <ChevronLeft size={18} />
              </button>
              <button className="control-btn" onClick={() => scrollRow(historyRowRef, 'right')}>
                <ChevronRight size={18} />
              </button>
            </div>
          </div>

          <div
            ref={historyRowRef}
            style={{
              display: 'flex',
              gap: '14px',
              overflowX: 'auto',
              scrollBehavior: 'smooth',
              paddingBottom: '8px'
            }}
          >
            {actualHistory.map((song) => {
              const isThisPlaying = currentSong?.videoId === song.videoId && isPlaying
              return (
                <div
                  key={song.videoId}
                  onClick={() => onPlaySong?.(song, actualHistory)}
                  style={{
                    minWidth: '120px',
                    maxWidth: '120px',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '6px'
                  }}
                >
                  <div
                    style={{
                      position: 'relative',
                      width: '120px',
                      height: '120px',
                      borderRadius: '14px',
                      overflow: 'hidden'
                    }}
                  >
                    <img
                      src={song.thumbnailUrl}
                      alt={song.title}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                    <div
                      style={{
                        position: 'absolute',
                        inset: 0,
                        background: 'rgba(0,0,0,0.3)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        opacity: isThisPlaying ? 1 : 0.8
                      }}
                    >
                      {isThisPlaying ? (
                        <div className="equalizer-wave compact">
                          <div className="equalizer-bar" />
                          <div className="equalizer-bar" />
                          <div className="equalizer-bar" />
                        </div>
                      ) : (
                        <Play size={22} fill="#fff" color="#fff" />
                      )}
                    </div>
                  </div>
                  <span
                    style={{
                      fontSize: '12px',
                      fontWeight: 700,
                      color: isThisPlaying ? 'var(--isai-lime)' : 'var(--text-primary)',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical'
                    }}
                  >
                    {song.title}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* 4. Picks For You ✨ (YouTube Music Style Cards) */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h2
              style={{
                fontSize: '18px',
                fontWeight: 800,
                color: 'var(--text-primary)',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}
            >
              Picks For You ✨
            </h2>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              {actualHistory.length > 0
                ? `Personalized for your ${activeLanguage} taste`
                : `Popular in ${activeLanguage}`}
            </span>
          </div>
          <div style={{ display: 'flex', gap: '6px' }}>
            <button className="control-btn" onClick={() => scrollRow(picksRowRef, 'left')}>
              <ChevronLeft size={18} />
            </button>
            <button className="control-btn" onClick={() => scrollRow(picksRowRef, 'right')}>
              <ChevronRight size={18} />
            </button>
          </div>
        </div>

        <div
          ref={picksRowRef}
          style={{ display: 'flex', gap: '16px', overflowX: 'auto', scrollBehavior: 'smooth', paddingBottom: '8px' }}
        >
          {picksSongs.map((song) => {
            const isThisPlaying = currentSong?.videoId === song.videoId && isPlaying
            const isFav = isFavorite(song.videoId)
            return (
              <div
                key={song.videoId}
                onClick={() => onPlaySong?.(song, deduplicated)}
                style={{
                  minWidth: '150px',
                  maxWidth: '150px',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px'
                }}
              >
                <div
                  style={{
                    position: 'relative',
                    width: '150px',
                    height: '150px',
                    borderRadius: '16px',
                    overflow: 'hidden',
                    border: isThisPlaying ? '2px solid var(--isai-lime)' : 'none'
                  }}
                >
                  <img
                    src={song.thumbnailUrl}
                    alt={song.title}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      onToggleFavorite(song)
                    }}
                    style={{
                      position: 'absolute',
                      top: '8px',
                      right: '8px',
                      background: 'rgba(0,0,0,0.65)',
                      border: 'none',
                      borderRadius: '50%',
                      width: '28px',
                      height: '28px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer'
                    }}
                  >
                    <Heart size={14} fill={isFav ? '#FF007A' : 'none'} color={isFav ? '#FF007A' : '#fff'} />
                  </button>
                  {isThisPlaying && (
                    <div
                      style={{
                        position: 'absolute',
                        inset: 0,
                        background: 'rgba(0,0,0,0.4)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                    >
                      <Play size={28} fill="var(--isai-lime)" color="var(--isai-lime)" />
                    </div>
                  )}
                </div>
                <span
                  style={{
                    fontSize: '13px',
                    fontWeight: 700,
                    color: isThisPlaying ? 'var(--isai-lime)' : 'var(--text-primary)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical'
                  }}
                >
                  {song.title}
                </span>
                <span
                  style={{
                    fontSize: '11px',
                    color: 'var(--text-muted)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  }}
                >
                  {song.channelTitle}
                </span>
              </div>
            )
          })}
        </div>
      </div>

      {/* 5. New Releases (New {Language} Songs) */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)' }}>
              New {activeLanguage} Songs 🎵
            </h2>
            <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Latest releases (Last 30 Days)</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              onClick={onSeeAllNewReleases || (() => onSelectCategory(`${activeLanguage} new releases 2026`))}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--isai-lime)',
                fontWeight: 800,
                fontSize: '12px',
                cursor: 'pointer'
              }}
            >
              See all
            </button>
            <button className="control-btn" onClick={() => scrollRow(newReleasesRowRef, 'left')}>
              <ChevronLeft size={18} />
            </button>
            <button className="control-btn" onClick={() => scrollRow(newReleasesRowRef, 'right')}>
              <ChevronRight size={18} />
            </button>
          </div>
        </div>

        <div
          ref={newReleasesRowRef}
          style={{ display: 'flex', gap: '16px', overflowX: 'auto', scrollBehavior: 'smooth', paddingBottom: '8px' }}
        >
          {displayNewReleases.map((song) => {
            const isThisPlaying = currentSong?.videoId === song.videoId && isPlaying
            return (
              <div
                key={song.videoId}
                onClick={() => onPlaySong?.(song, displayNewReleases)}
                style={{
                  minWidth: '150px',
                  maxWidth: '150px',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px'
                }}
              >
                <div
                  style={{
                    position: 'relative',
                    width: '150px',
                    height: '150px',
                    borderRadius: '16px',
                    overflow: 'hidden',
                    border: isThisPlaying ? '2px solid var(--isai-lime)' : 'none'
                  }}
                >
                  <img
                    src={song.thumbnailUrl}
                    alt={song.title}
                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                  />
                  <div
                    style={{
                      position: 'absolute',
                      top: '8px',
                      right: '8px',
                      background: 'rgba(0,0,0,0.7)',
                      color: 'var(--isai-lime)',
                      fontSize: '10px',
                      fontWeight: 800,
                      padding: '2px 6px',
                      borderRadius: '6px'
                    }}
                  >
                    NEW
                  </div>
                </div>
                <span
                  style={{
                    fontSize: '13px',
                    fontWeight: 700,
                    color: isThisPlaying ? 'var(--isai-lime)' : 'var(--text-primary)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    display: '-webkit-box',
                    WebkitLineClamp: 2,
                    WebkitBoxOrient: 'vertical'
                  }}
                >
                  {song.title}
                </span>
                <span
                  style={{
                    fontSize: '11px',
                    color: 'var(--text-muted)',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  }}
                >
                  {song.channelTitle}
                </span>
              </div>
            )
          })}
        </div>
      </div>

      {/* 6. What's Your Mood Today? */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)' }}>
              What's Your Mood Today? 💫
            </h2>
            <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
              Curated playlists matched to your current vibe
            </span>
          </div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '14px' }}>
          {moodCards.map((m) => (
            <div key={m.title} onClick={() => onSelectCategory(m.query)} className="mood-card">
              {/* Background Cover Image */}
              <img
                src={m.image}
                alt={m.title}
                loading="lazy"
                referrerPolicy="no-referrer"
                className="mood-card-bg"
                style={{
                  position: 'absolute',
                  inset: 0,
                  width: '100%',
                  height: '100%',
                  objectFit: 'cover',
                  transition: 'transform 0.4s ease'
                }}
              />
              {/* Dark Gradient Overlay for readability */}
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  background:
                    'linear-gradient(to top, rgba(10,10,16,0.92) 0%, rgba(10,10,16,0.42) 55%, rgba(0,0,0,0.2) 100%)'
                }}
              />
              {/* Tint overlay */}
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  background: m.gradient,
                  mixBlendMode: 'overlay',
                  opacity: 0.8
                }}
              />
              {/* Foreground content */}
              <div
                style={{
                  position: 'relative',
                  zIndex: 2,
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  padding: '12px 14px'
                }}
              >
                <div
                  style={{
                    alignSelf: 'flex-start',
                    background: 'rgba(0,0,0,0.45)',
                    backdropFilter: 'blur(8px)',
                    WebkitBackdropFilter: 'blur(8px)',
                    border: '1px solid rgba(255,255,255,0.18)',
                    borderRadius: '20px',
                    padding: '3px 8px',
                    fontSize: '14px',
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  <span>{m.emoji}</span>
                </div>
                <div>
                  <div style={{ fontSize: '15px', fontWeight: 800, color: '#fff', letterSpacing: '-0.2px' }}>
                    {m.title}
                  </div>
                  <div style={{ fontSize: '11px', fontWeight: 500, color: 'rgba(255,255,255,0.75)', marginTop: '2px' }}>
                    {m.subtitle}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 7. Your Favorite Artists */}
      <div>
        <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '14px' }}>
          {actualHistory.length > 0 ? 'Your Favorite Artists 🎤' : `Explore ${activeLanguage} Artists 🎤`}
        </h2>
        <div style={{ display: 'flex', gap: '20px', overflowX: 'auto', paddingBottom: '8px' }}>
          {displayArtists.map((artist) => (
            <div key={artist.name} onClick={() => onSelectArtist?.(artist)} className="artist-avatar-card">
              <div
                className="artist-avatar-img"
                style={{
                  position: 'relative',
                  width: '84px',
                  height: '84px',
                  borderRadius: '50%',
                  background: 'linear-gradient(135deg, var(--isai-purple), var(--isai-pink))',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '2px solid rgba(255,255,255,0.15)',
                  boxShadow: '0 6px 18px rgba(0,0,0,0.35)',
                  overflow: 'hidden',
                  transition: 'all 0.25s ease'
                }}
              >
                <span style={{ fontSize: '24px', fontWeight: 900, color: '#fff' }}>{artist.name.charAt(0)}</span>
                <img
                  src={artist.image}
                  alt={artist.name}
                  loading="lazy"
                  referrerPolicy="no-referrer"
                  style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }}
                  onError={(e) => {
                    ;(e.target as HTMLElement).style.display = 'none'
                  }}
                />
              </div>
              <span
                style={{
                  fontSize: '12px',
                  fontWeight: 700,
                  color: 'var(--text-primary)',
                  textAlign: 'center',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  maxWidth: '100px'
                }}
              >
                {artist.name}
              </span>
              <span style={{ fontSize: '10px', color: 'var(--text-muted)', textAlign: 'center', marginTop: '-4px' }}>
                {artist.role}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 8. Most Played Songs Section 🔥 */}
      <div>
        <div style={{ marginBottom: '14px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)' }}>Most Played Songs 🔥</h2>
          <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Top played hits by ISAI listeners</span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {mostPlayedList.map((song, idx) => {
            const isThisPlaying = currentSong?.videoId === song.videoId && isPlaying
            const isFav = isFavorite(song.videoId)
            return (
              <div
                key={`${song.videoId}_mp_${idx}`}
                onClick={() => onPlaySong?.(song, mostPlayedList)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '10px 14px',
                  borderRadius: '14px',
                  background: isThisPlaying ? 'rgba(200, 255, 0, 0.12)' : 'var(--surface-card)',
                  border: isThisPlaying ? '1px solid var(--isai-lime)' : '1px solid var(--border-subtle)',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
              >
                <div
                  style={{
                    minWidth: '32px',
                    height: '32px',
                    borderRadius: '8px',
                    background: 'var(--isai-lime)',
                    color: '#000',
                    fontSize: '12px',
                    fontWeight: 900,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  #{idx + 1}
                </div>

                <img
                  src={song.thumbnailUrl}
                  alt={song.title}
                  style={{ width: '52px', height: '52px', borderRadius: '10px', objectFit: 'cover' }}
                />

                <div style={{ flex: 1, minWidth: 0 }}>
                  <div
                    style={{
                      fontSize: '13px',
                      fontWeight: 700,
                      color: isThisPlaying ? 'var(--isai-lime)' : 'var(--text-primary)',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis'
                    }}
                  >
                    {song.title}
                  </div>
                  <div
                    style={{
                      fontSize: '11px',
                      color: 'var(--text-muted)',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis'
                    }}
                  >
                    {song.channelTitle}
                  </div>
                </div>

                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    onToggleFavorite(song)
                  }}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '6px' }}
                >
                  <Heart size={18} fill={isFav ? '#FF007A' : 'none'} color={isFav ? '#FF007A' : 'var(--text-muted)'} />
                </button>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
