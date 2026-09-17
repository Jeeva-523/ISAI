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
    { name: 'Anirudh Ravichander', role: 'Composer & Singer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/Anirudh_Ravichander_at_Audi_R8_LMX_launch.jpg/480px-Anirudh_Ravichander_at_Audi_R8_LMX_launch.jpg', query: 'Anirudh Ravichander Tamil hits', followers: '24.5M Listeners' },
    { name: 'A.R. Rahman', role: 'Composer & Maestro', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/9c/A._R._Rahman_WM2016.jpg/480px-A._R._Rahman_WM2016.jpg', query: 'A R Rahman Tamil hits', followers: '32.1M Listeners' },
    { name: 'Yuvan Shankar Raja', role: 'Composer & Singer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/1/14/Yuvan_Shankar_Raja.jpg/480px-Yuvan_Shankar_Raja.jpg', query: 'Yuvan Shankar Raja Tamil hits', followers: '19.8M Listeners' },
    { name: 'Harris Jayaraj', role: 'Composer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Harris_Jayaraj_at_Irandaam_Ulagam_Audio_Launch.jpg/480px-Harris_Jayaraj_at_Irandaam_Ulagam_Audio_Launch.jpg', query: 'Harris Jayaraj Tamil hits', followers: '15.4M Listeners' },
    { name: 'Sid Sriram', role: 'Playback Singer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Sid_Sriram_at_Enai_Noki_Paayum_Thota_Audio_Launch.jpg/480px-Sid_Sriram_at_Enai_Noki_Paayum_Thota_Audio_Launch.jpg', query: 'Sid Sriram Tamil hits', followers: '14.2M Listeners' }
  ],
  Telugu: [
    { name: 'Devi Sri Prasad', role: 'Composer & Singer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/Devi_Sri_Prasad.jpg/480px-Devi_Sri_Prasad.jpg', query: 'Devi Sri Prasad Telugu hits', followers: '18.2M Listeners' },
    { name: 'Thaman S', role: 'Music Director', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/S_Thaman.jpg/480px-S_Thaman.jpg', query: 'Thaman S Telugu hits', followers: '16.5M Listeners' },
    { name: 'M.M. Keeravani', role: 'Academy Maestro', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/MM_Keeravani_2023.jpg/480px-MM_Keeravani_2023.jpg', query: 'MM Keeravani Telugu hits', followers: '12.8M Listeners' },
    { name: 'Sid Sriram', role: 'Singer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Sid_Sriram_at_Enai_Noki_Paayum_Thota_Audio_Launch.jpg/480px-Sid_Sriram_at_Enai_Noki_Paayum_Thota_Audio_Launch.jpg', query: 'Sid Sriram Telugu hits', followers: '14.2M Listeners' }
  ],
  Hindi: [
    { name: 'Arijit Singh', role: 'Playback Singer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/9b/Arijit_Singh_5th_GiMA_Awards.jpg/480px-Arijit_Singh_5th_GiMA_Awards.jpg', query: 'Arijit Singh Hindi hits', followers: '45.1M Listeners' },
    { name: 'Pritam', role: 'Composer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Pritam_Chakraborty.jpg/480px-Pritam_Chakraborty.jpg', query: 'Pritam Hindi hits', followers: '28.4M Listeners' },
    { name: 'Shreya Ghoshal', role: 'Singer', image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Shreya_Ghoshal_at_FCAT.jpg/480px-Shreya_Ghoshal_at_FCAT.jpg', query: 'Shreya Ghoshal Hindi hits', followers: '25.6M Listeners' }
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

  // Section 4: Picks For You (10 recommended songs)
  const picksSongs = deduplicated.slice(0, 10)

  // Section 5: New Releases (rolling last 30 days filter verification)
  const nowMs = Date.now()
  const thirtyDaysMs = 30 * 24 * 60 * 60 * 1000
  const verifiedNewReleases = deduplicated.filter((song) => {
    const pub = (song as any).publishedAt
    if (!pub) return true
    const pubDate = new Date(pub).getTime()
    return !Number.isNaN(pubDate) && (nowMs - pubDate) <= thirtyDaysMs
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

  // Section 8: Most Played Songs
  const mostPlayedList = deduplicated.slice(0, 25)

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
          <h1 style={{ fontSize: '24px', fontWeight: 900, color: '#fff', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
            Welcome, {userName} 👋
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
                  const isSel = (lang as any).equalsIgnoreCase ? (lang as any).equalsIgnoreCase(activeLanguage) : lang.toLowerCase() === activeLanguage.toLowerCase()
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
                        color: isSel ? 'var(--isai-lime)' : '#fff',
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

          <button
            onClick={onOpenProfile}
            style={{
              width: '38px',
              height: '38px',
              borderRadius: '50%',
              background: 'var(--surface-card)',
              border: '1px solid var(--border-subtle)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer'
            }}
            title="Profile"
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
          Search songs, artists, albums…
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

      {/* 4. Picks For You ✨ (YouTube Music Style Cards) */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              Picks For You ✨
            </h2>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              {actualHistory.length > 0 ? `Personalized for your ${activeLanguage} taste` : `Popular in ${activeLanguage}`}
            </span>
          </div>
          <div style={{ display: 'flex', gap: '6px' }}>
            <button className="control-btn" onClick={() => scrollRow(picksRowRef, 'left')}><ChevronLeft size={18} /></button>
            <button className="control-btn" onClick={() => scrollRow(picksRowRef, 'right')}><ChevronRight size={18} /></button>
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
                style={{ minWidth: '150px', maxWidth: '150px', cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: '8px' }}
              >
                <div style={{ position: 'relative', width: '150px', height: '150px', borderRadius: '16px', overflow: 'hidden', border: isThisPlaying ? '2px solid var(--isai-lime)' : 'none' }}>
                  <img src={song.thumbnailUrl} alt={song.title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
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
                    <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Play size={28} fill="var(--isai-lime)" color="var(--isai-lime)" />
                    </div>
                  )}
                </div>
                <span style={{ fontSize: '13px', fontWeight: 700, color: isThisPlaying ? 'var(--isai-lime)' : '#fff', overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
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

      {/* 6. What's Your Mood Today? */}
      <div>
        <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '14px' }}>
          What's Your Mood Today? 💫
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

      {/* 7. Your Favorite Artists */}
      <div>
        <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '14px' }}>
          {actualHistory.length > 0 ? 'Your Favorite Artists 🎤' : `Explore ${activeLanguage} Artists 🎤`}
        </h2>
        <div style={{ display: 'flex', gap: '20px', overflowX: 'auto', paddingBottom: '8px' }}>
          {displayArtists.map((artist) => (
            <div
              key={artist.name}
              onClick={() => onSelectArtist?.(artist)}
              style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px', cursor: 'pointer', minWidth: '90px' }}
            >
              <div style={{ position: 'relative', width: '84px', height: '84px', borderRadius: '50%', background: 'linear-gradient(135deg, var(--isai-purple), var(--isai-pink))', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '2px solid var(--border-subtle)', overflow: 'hidden' }}>
                <span style={{ fontSize: '24px', fontWeight: 900, color: '#fff' }}>{artist.name.charAt(0)}</span>
                <img
                  src={artist.image}
                  alt={artist.name}
                  style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }}
                  onError={(e) => { (e.target as HTMLElement).style.display = 'none' }}
                />
              </div>
              <span style={{ fontSize: '12px', fontWeight: 700, color: '#fff', textAlign: 'center', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '100px' }}>
                {artist.name}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* 8. Most Played Songs Section 🔥 */}
      <div>
        <div style={{ marginBottom: '14px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--text-primary)' }}>
            Most Played Songs 🔥
          </h2>
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
                  <div style={{ fontSize: '13px', fontWeight: 700, color: isThisPlaying ? 'var(--isai-lime)' : '#fff', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
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

    </div>
  )
}
