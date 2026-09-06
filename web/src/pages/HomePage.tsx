import React, { useRef, useState } from 'react'
import type { Song } from '@shared/models/song'
import { deduplicateSongs } from '@shared/utils/formatters'
import { SongCard } from '../components/SongCard'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { ErrorBanner } from '../components/ErrorBanner'
import { trendingService, TrendingTimeWindow } from '../services/TrendingService'
import { analyticsService } from '../services/AnalyticsService'

interface HomePageProps {
  trendingSongs: Song[]
  isLoading: boolean
  error: string | null
  onSelectCategory: (query: string) => void
  onRetry: () => void
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong?: (song: Song) => void
}

const YTM_MOOD_PILLS = [
  { id: 'romance', label: 'Romance', query: 'Tamil love romantic songs' },
  { id: 'feelgood', label: 'Feel good', query: 'Tamil feel good melody hits' },
  { id: 'party', label: 'Party', query: 'Tamil party kuthu hits' },
  { id: 'relax', label: 'Relax', query: 'Tamil relaxing acoustic melody' },
  { id: 'commute', label: 'Commute', query: 'Tamil travel melody hits' },
  { id: 'sad', label: 'Sad', query: 'Tamil sad emotional songs' },
  { id: 'energize', label: 'Energize', query: 'Tamil energetic mass bgm' },
  { id: 'workout', label: 'Workout', query: 'Tamil gym workout beats' }
]

export const HomePage: React.FC<HomePageProps> = ({
  trendingSongs,
  isLoading,
  error,
  onSelectCategory,
  onRetry,
  isFavorite,
  onToggleFavorite,
  onPlaySong
}) => {
  const [timeWindow, setTimeWindow] = useState<TrendingTimeWindow>('today')
  const listenAgainRef = useRef<HTMLDivElement | null>(null)
  const trendingRowRef = useRef<HTMLDivElement | null>(null)

  const scrollRow = (ref: React.RefObject<HTMLDivElement>, direction: 'left' | 'right') => {
    if (ref.current) {
      const scrollAmount = direction === 'left' ? -600 : 600
      ref.current.scrollBy({ left: scrollAmount, behavior: 'smooth' })
    }
  }

  const handlePlay = (song: Song) => {
    analyticsService.trackEvent(song.videoId, song.title, song.channelTitle, 'play')
    if (onPlaySong) onPlaySong(song)
  }

  const deduplicated = deduplicateSongs(trendingSongs)
  const rankedTrending = trendingService.rankTrendingSongs(deduplicated, timeWindow)

  const heroTrending = rankedTrending.slice(0, 8)
  const listenAgain = rankedTrending.slice(8, 16)
  const trendingGrid = rankedTrending.slice(16).length > 0 ? rankedTrending.slice(16) : rankedTrending.slice(0, 20)

  const getDynamicGreeting = () => {
    const hour = new Date().getHours()
    if (hour >= 5 && hour < 12) return 'Good Morning'
    if (hour >= 12 && hour < 17) return 'Good Afternoon'
    if (hour >= 17 && hour < 22) return 'Good Evening'
    return 'Night Vibes'
  }

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', paddingBottom: '60px' }}>
      {/* 1. YTM Top Mood Filter Pills */}
      <div className="ytm-mood-bar-container" style={{ marginBottom: '24px' }}>
        <div className="ytm-mood-bar">
          {YTM_MOOD_PILLS.map((pill) => (
            <button
              key={pill.id}
              className="ytm-mood-chip"
              onClick={() => onSelectCategory(pill.query)}
            >
              {pill.label}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <LoadingSpinner message="Fetching ISAI Trending & For You Recommendations..." />
      ) : error ? (
        <ErrorBanner title="Failed to Load Home Content" message={error} onRetry={onRetry} />
      ) : (
        <div>
          {/* Greeting Banner */}
          <div style={{ marginBottom: '28px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <span style={{ fontSize: '12px', fontWeight: 800, color: 'var(--isai-lime)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                Un Isai. Un Feel.
              </span>
              <h1 style={{ fontSize: '32px', fontWeight: 900, marginTop: '2px', color: 'var(--text-primary)' }}>
                {getDynamicGreeting()}, <span style={{ color: 'var(--isai-lime)' }}>JEEVA ⚡</span>
              </h1>
            </div>

            {/* Time Window Selector Pills */}
            <div style={{ display: 'flex', gap: '8px', background: 'var(--surface-card)', padding: '6px', borderRadius: '20px', border: '1px solid var(--border-subtle)' }}>
              {(['today', 'week', 'month'] as TrendingTimeWindow[]).map((w) => (
                <button
                  key={w}
                  onClick={() => setTimeWindow(w)}
                  style={{
                    padding: '6px 14px',
                    borderRadius: '14px',
                    border: 'none',
                    background: timeWindow === w ? 'var(--isai-lime)' : 'transparent',
                    color: timeWindow === w ? '#000' : 'var(--text-secondary)',
                    fontWeight: 700,
                    fontSize: '12px',
                    cursor: 'pointer',
                    textTransform: 'capitalize'
                  }}
                >
                  {w === 'today' ? '🔥 Today' : w === 'week' ? '📅 This Week' : '🏆 This Month'}
                </button>
              ))}
            </div>
          </div>

          {/* 2. 🔥 Dynamic Trending Now Section */}
          {heroTrending.length > 0 && (
            <section className="ytm-section" style={{ marginBottom: '36px' }}>
              <div className="ytm-section-header" style={{ marginBottom: '16px' }}>
                <div>
                  <h2 className="ytm-section-title" style={{ fontSize: '22px', fontWeight: 800 }}>
                    🔥 Trending Now ({timeWindow === 'today' ? 'Today' : timeWindow === 'week' ? 'This Week' : 'This Month'})
                  </h2>
                  <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
                    Dynamic rankings based on plays, listener growth & likes
                  </p>
                </div>

                <div className="ytm-section-actions">
                  <button className="ytm-btn-arrow-nav" onClick={() => scrollRow(trendingRowRef, 'left')}>‹</button>
                  <button className="ytm-btn-arrow-nav" onClick={() => scrollRow(trendingRowRef, 'right')}>›</button>
                </div>
              </div>

              <div className="ytm-horizontal-row" ref={trendingRowRef}>
                {heroTrending.map((song, index) => (
                  <div key={song.videoId} style={{ position: 'relative', flexShrink: 0 }}>
                    <div
                      style={{
                        position: 'absolute',
                        top: '10px',
                        left: '10px',
                        zIndex: 2,
                        background: 'rgba(0,0,0,0.85)',
                        border: '1px solid var(--isai-lime)',
                        color: 'var(--isai-lime)',
                        fontWeight: 900,
                        fontSize: '13px',
                        width: '28px',
                        height: '28px',
                        borderRadius: '50%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                    >
                      #{index + 1}
                    </div>
                    <SongCard
                      song={song}
                      isFavorite={isFavorite(song.videoId)}
                      onToggleFavorite={onToggleFavorite}
                      onPlay={() => handlePlay(song)}
                    />
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* 3. ✨ Recommended For You */}
          {listenAgain.length > 0 && (
            <section className="ytm-section" style={{ marginBottom: '36px' }}>
              <div className="ytm-section-header" style={{ marginBottom: '16px' }}>
                <div>
                  <h2 className="ytm-section-title" style={{ fontSize: '22px', fontWeight: 800 }}>
                    ✨ Trending For You
                  </h2>
                  <p style={{ fontSize: '13px', color: 'var(--isai-lime)' }}>
                    Personalized based on your Tamil listening history
                  </p>
                </div>
                <div className="ytm-section-actions">
                  <button className="ytm-btn-arrow-nav" onClick={() => scrollRow(listenAgainRef, 'left')}>‹</button>
                  <button className="ytm-btn-arrow-nav" onClick={() => scrollRow(listenAgainRef, 'right')}>›</button>
                </div>
              </div>

              <div className="ytm-horizontal-row" ref={listenAgainRef}>
                {listenAgain.map((song) => (
                  <SongCard
                    key={song.videoId}
                    song={song}
                    isFavorite={isFavorite(song.videoId)}
                    onToggleFavorite={onToggleFavorite}
                    onPlay={() => handlePlay(song)}
                  />
                ))}
              </div>
            </section>
          )}

          {/* 4. Full Catalog Grid */}
          <section className="ytm-section">
            <div className="ytm-section-header" style={{ marginBottom: '16px' }}>
              <h2 className="ytm-section-title" style={{ fontSize: '22px', fontWeight: 800 }}>
                🎵 All Chartbuster Hits
              </h2>
            </div>
            <div className="ytm-grid">
              {trendingGrid.map((song) => (
                <SongCard
                  key={song.videoId}
                  song={song}
                  isFavorite={isFavorite(song.videoId)}
                  onToggleFavorite={onToggleFavorite}
                  onPlay={() => handlePlay(song)}
                />
              ))}
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
