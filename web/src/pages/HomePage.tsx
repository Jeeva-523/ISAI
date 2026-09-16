import React, { useRef, useState } from 'react'
import type { Song } from '@shared/models/song'
import { deduplicateSongs } from '@shared/utils/formatters'
import { SkeletonSongCard } from '../components/SkeletonLoader'
import { ErrorBanner } from '../components/ErrorBanner'
import type { Artist } from '../components/ArtistCard'
import type { UserProfile } from '../components/LoginModal'
import { storageService } from '@shared/services/storageService'
import { Play, Heart, Search, ChevronRight, ChevronLeft, Globe, User } from 'lucide-react'

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
  onOpenProfile?: () => void
  onOpenSearch?: () => void
  listeningHistory?: Song[]
  onSeeAllNewReleases?: () => void
  dailyMixes?: any[]
  onSelectPlaylistDetail?: any
}

const SUPPORTED_LANGUAGES = ['Tamil', 'Telugu', 'Hindi', 'Kannada', 'Malayalam', 'English']

const POPULAR_ARTISTS_BY_LANG: Record<string, Artist[]> = {
  Tamil: [
    { name: 'Anirudh Ravichander', role: 'Composer & Singer', image: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_004_20230222091040_500x500.jpg', query: 'Anirudh Ravichander Tamil hits', followers: '24.5M Listeners' },
    { name: 'A.R. Rahman', role: 'Composer & Maestro', image: 'https://c.saavncdn.com/artists/A_R_Rahman_004_20230718070940_500x500.jpg', query: 'A R Rahman Tamil hits', followers: '32.1M Listeners' },
    { name: 'Yuvan Shankar Raja', role: 'Composer & Singer', image: 'https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_004_20220908070940_500x500.jpg', query: 'Yuvan Shankar Raja Tamil hits', followers: '19.8M Listeners' },
    { name: 'Harris Jayaraj', role: 'Composer', image: 'https://c.saavncdn.com/artists/Harris_Jayaraj_002_20200812070940_500x500.jpg', query: 'Harris Jayaraj Tamil hits', followers: '15.4M Listeners' },
    { name: 'Sid Sriram', role: 'Playback Singer', image: 'https://c.saavncdn.com/artists/Sid_Sriram_003_20230516070940_500x500.jpg', query: 'Sid Sriram Tamil hits', followers: '14.2M Listeners' }
  ],
  Telugu: [
    { name: 'Devi Sri Prasad', role: 'Composer & Singer', image: 'https://c.saavncdn.com/artists/Devi_Sri_Prasad_002_20210608070940_500x500.jpg', query: 'Devi Sri Prasad Telugu hits', followers: '18.2M Listeners' },
    { name: 'Thaman S', role: 'Music Director', image: 'https://c.saavncdn.com/artists/Thaman_S_003_20230116070940_500x500.jpg', query: 'Thaman S Telugu hits', followers: '16.5M Listeners' },
    { name: 'M.M. Keeravani', role: 'Academy Maestro', image: 'https://c.saavncdn.com/artists/M_M_Keeravani_002_20230314070940_500x500.jpg', query: 'MM Keeravani Telugu hits', followers: '12.8M Listeners' },
    { name: 'Sid Sriram', role: 'Singer', image: 'https://c.saavncdn.com/artists/Sid_Sriram_003_20230516070940_500x500.jpg', query: 'Sid Sriram Telugu hits', followers: '14.2M Listeners' }
  ],
  Hindi: [
    { name: 'Arijit Singh', role: 'Playback Singer', image: 'https://c.saavncdn.com/artists/Arijit_Singh_002_20230323070940_500x500.jpg', query: 'Arijit Singh Hindi hits', followers: '45.1M Listeners' },
    { name: 'Pritam', role: 'Composer', image: 'https://c.saavncdn.com/artists/Pritam_003_20220608070940_500x500.jpg', query: 'Pritam Hindi hits', followers: '28.4M Listeners' },
    { name: 'Shreya Ghoshal', role: 'Singer', image: 'https://c.saavncdn.com/artists/Shreya_Ghoshal_003_20230412070940_500x500.jpg', query: 'Shreya Ghoshal Hindi hits', followers: '25.6M Listeners' }
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
  onOpenProfile,
  onOpenSearch,
  listeningHistory = [],
  onSeeAllNewReleases
}) => {
  const [showLangDropdown, setShowLangDropdown] = useState(false)

  // Language Resolution
  const activeLanguage = propLanguage || userProfile?.preferredLanguages?.[0] || 'Tamil'
  const userName = userProfile?.name || 'JEEVA ⚡'

  // Ref for horizontal scrolling
  const historyRowRef = useRef<HTMLDivElement>(null)
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

  // Section 4: Unakkaaga Picks (6 songs: 2 cols x 3 rows)
  const picksSongs = deduplicated.slice(0, 6)

  // Section 5: New Releases (rolling last 30 days filter verification)
  const nowMs = Date.now()
  const thirtyDaysMs = 30 * 24 * 60 * 60 * 1000
  const verifiedNewReleases = deduplicated.filter((song) => {
    const pub = (song as any).publishedAt
    if (!pub) return true
    const pubDate = new Date(pub).getTime()
    return !isNaN(pubDate) && (nowMs - pubDate) <= thirtyDaysMs
  }).slice(0, 12)
  const displayNewReleases = verifiedNewReleases.length >= 4 ? verifiedNewReleases : deduplicated.slice(6, 18)

  // Section 6: Mood Cards
  const moodCards = [
    { title: 'Love', emoji: '💖', query: `${activeLanguage} love romantic hit songs`, gradient: 'linear-gradient(135deg, rgba(236,72,153,0.3), rgba(139,92,246,0.3))' },
    { title: 'Chill', emoji: '☕', query: `${activeLanguage} lo-fi chill rain songs`, gradient: 'linear-gradient(135deg, rgba(99,102,241,0.3), rgba(168,85,247,0.3))' },
    { title: 'Gym', emoji: '⚡', query: `${activeLanguage} energetic gym workout bgm beats`, gradient: 'linear-gradient(135deg, rgba(16,185,129,0.3), rgba(6,182,212,0.3))' },
    { title: 'Travel', emoji: '🚗', query: `${activeLanguage} road trip travel songs`, gradient: 'linear-gradient(135deg, rgba(245,158,11,0.3), rgba(239,68,68,0.3))' }
  ]

  // Section 7: Artists
  const displayArtists = POPULAR_ARTISTS_BY_LANG[activeLanguage] || POPULAR_ARTISTS_BY_LANG['Tamil']

  if (isLoading && deduplicated.length === 0) {
    return (
      <div style={{ paddingBottom: '120px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
        <div style={{ height: '60px', background: 'var(--surface-card)', borderRadius: '16px' }} />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat( auto-fit, minmax(200px, 1fr) )', gap: '16px' }}>
          {[1, 2, 3, 4, 5, 6].map((n) => <SkeletonSongCard key={n} />)}
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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ fontSize: '26px', fontWeight: 900, color: 'var(--text-primary)', margin: 0 }}>
            Vanakkam, <span style={{ color: 'var(--isai-lime)' }}>{userName}</span> 👋
          </h1>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600 }}>
            Enjoying {activeLanguage} Music on ISAI
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', position: 'relative' }}>
          {/* Language Selector Dropdown */}
          <div style={{ position: 'relative' }}>
            <button
              onClick={() => setShowLangDropdown(!showLangDropdown)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                padding: '8px 14px',
                borderRadius: '20px',
                border: '1px solid rgba(200, 255, 0, 0.4)',
                background: 'rgba(200, 255, 0, 0.1)',
                color: 'var(--isai-lime)',
                fontWeight: 800,
                fontSize: '12px',
                cursor: 'pointer'
              }}
            >
              <Globe size={14} />
              <span>{activeLanguage}</span>
              <span style={{ fontSize: '10px' }}>▾</span>
            </button>

            {showLangDropdown && (
              <div
                onClick={(e) => e.stopPropagation()}
                style={{
                  position: 'absolute',
                  top: '42px',
                  right: 0,
                  backgroundColor: '#181824',
                  border: '1px solid rgba(255,255,255,0.15)',
                  borderRadius: '14px',
                  padding: '8px',
                  zIndex: 999,
                  minWidth: '150px',
                  boxShadow: '0 12px 30px rgba(0,0,0,0.8)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '4px'
                }}
              >
                {SUPPORTED_LANGUAGES.map((lang) => {
                  const isSelected = activeLanguage.toLowerCase() === lang.toLowerCase()
                  return (
                    <button
                      key={lang}
                      onClick={() => {
                        onSelectLanguage?.(lang)
                        setShowLangDropdown(false)
                      }}
                      style={{
                        padding: '8px 12px',
                        borderRadius: '10px',
                        border: 'none',
                        background: isSelected ? 'rgba(200, 255, 0, 0.2)' : 'transparent',
                        color: isSelected ? 'var(--isai-lime)' : '#fff',
                        fontWeight: isSelected ? 800 : 500,
                        fontSize: '13px',
                        textAlign: 'left',
                        cursor: 'pointer'
                      }}
                    >
                      {lang} {isSelected && '✓'}
                    </button>
                  )
                })}
              </div>
            )}
          </div>

          {/* Profile Button */}
          <button
            onClick={onOpenProfile}
            style={{
              width: '38px',
              height: '38px',
              borderRadius: '50%',
              background: 'var(--surface-glass)',
              border: '1px solid var(--border-subtle)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer'
            }}
            title="Profile & Settings"
          >
            <User size={18} />
          </button>
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
          Paadal, artist, album thedu…
        </span>
      </div>

      {/* 3. Continue Listening (Hidden if no history) */}
      {actualHistory.length > 0 && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              Continue Listening 🎧
            </h2>
            <div style={{ display: 'flex', gap: '6px' }}>
              <button className="control-btn" onClick={() => scrollRow(historyRowRef, 'left')}><ChevronLeft size={18} /></button>
              <button className="control-btn" onClick={() => scrollRow(historyRowRef, 'right')}><ChevronRight size={18} /></button>
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
                  <div style={{ position: 'relative', width: '120px', height: '120px', borderRadius: '14px', overflow: 'hidden' }}>
                    <img src={song.thumbnailUrl} alt={song.title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
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
                  <span style={{ fontSize: '12px', fontWeight: 700, color: '#fff', overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                    {song.title}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* 4. Unakkaaga Picks ✨ (2 Columns x 3 Rows = 6 items) */}
      <div>
        <div style={{ marginBottom: '14px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            Unakkaaga Picks ✨
          </h2>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
            {actualHistory.length > 0 ? `Personalized for your ${activeLanguage} taste` : `${activeLanguage}-la Popular`}
          </span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '12px' }}>
          {picksSongs.map((song) => {
            const isThisPlaying = currentSong?.videoId === song.videoId && isPlaying
            const isFav = isFavorite(song.videoId)
            return (
              <div
                key={song.videoId}
                onClick={() => onPlaySong?.(song, deduplicated)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '10px',
                  borderRadius: '14px',
                  background: isThisPlaying ? 'rgba(200, 255, 0, 0.12)' : 'var(--surface-card)',
                  border: isThisPlaying ? '1px solid var(--isai-lime)' : '1px solid var(--border-subtle)',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease'
                }}
              >
                <img
                  src={song.thumbnailUrl}
                  alt={song.title}
                  style={{ width: '54px', height: '54px', borderRadius: '10px', objectFit: 'cover' }}
                />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: isThisPlaying ? 'var(--isai-lime)' : '#fff', overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                    {song.title}
                  </div>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
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

      {/* 5. New Releases (Pudhu {Language} Paadalgal) */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)' }}>
              Pudhu {activeLanguage} Paadalgal 🎵
            </h2>
            <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Latest releases (Last 30 Days)</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <button
              onClick={onSeeAllNewReleases || (() => onSelectCategory(`${activeLanguage} new releases 2026`))}
              style={{ background: 'none', border: 'none', color: 'var(--isai-lime)', fontWeight: 800, fontSize: '12px', cursor: 'pointer' }}
            >
              See all
            </button>
            <button className="control-btn" onClick={() => scrollRow(newReleasesRowRef, 'left')}><ChevronLeft size={18} /></button>
            <button className="control-btn" onClick={() => scrollRow(newReleasesRowRef, 'right')}><ChevronRight size={18} /></button>
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
                style={{ minWidth: '150px', maxWidth: '150px', cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: '8px' }}
              >
                <div style={{ position: 'relative', width: '150px', height: '150px', borderRadius: '16px', overflow: 'hidden', border: isThisPlaying ? '2px solid var(--isai-lime)' : 'none' }}>
                  <img src={song.thumbnailUrl} alt={song.title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  <div style={{ position: 'absolute', top: '8px', right: '8px', background: 'rgba(0,0,0,0.7)', color: 'var(--isai-lime)', fontSize: '10px', fontWeight: 800, padding: '2px 6px', borderRadius: '6px' }}>
                    NEW
                  </div>
                </div>
                <span style={{ fontSize: '13px', fontWeight: 700, color: '#fff', overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                  {song.title}
                </span>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {song.channelTitle}
                </span>
              </div>
            )
          })}
        </div>
      </div>

      {/* 6. Innaiku Enna Mood? */}
      <div>
        <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '14px' }}>
          Innaiku Enna Mood? 💫
        </h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '12px' }}>
          {moodCards.map((m) => (
            <div
              key={m.title}
              onClick={() => onSelectCategory(m.query)}
              style={{
                padding: '16px',
                borderRadius: '16px',
                background: m.gradient,
                border: '1px solid rgba(255,255,255,0.1)',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px',
                transition: 'transform 0.2s ease'
              }}
            >
              <span style={{ fontSize: '24px' }}>{m.emoji}</span>
              <span style={{ fontSize: '15px', fontWeight: 800, color: '#fff' }}>{m.title}</span>
            </div>
          ))}
        </div>
      </div>

      {/* 7. Un Favourite Artists */}
      <div>
        <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '14px' }}>
          {actualHistory.length > 0 ? 'Un Favourite Artists 🎤' : `Explore ${activeLanguage} Artists 🎤`}
        </h2>
        <div style={{ display: 'flex', gap: '20px', overflowX: 'auto', paddingBottom: '8px' }}>
          {displayArtists.map((artist) => (
            <div
              key={artist.name}
              onClick={() => onSelectArtist?.(artist)}
              style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px', cursor: 'pointer', minWidth: '90px' }}
            >
              <img
                src={artist.image}
                alt={artist.name}
                style={{ width: '84px', height: '84px', borderRadius: '50%', objectFit: 'cover', border: '2px solid var(--border-subtle)' }}
              />
              <span style={{ fontSize: '12px', fontWeight: 700, color: '#fff', textAlign: 'center', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100px' }}>
                {artist.name}
              </span>
            </div>
          ))}
        </div>
      </div>

    </div>
  )
}
