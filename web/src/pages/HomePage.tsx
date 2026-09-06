import React, { useRef } from 'react'
import type { Song } from '@shared/models/song'
import { deduplicateSongs } from '@shared/utils/formatters'
import { SongCard } from '../components/SongCard'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { ErrorBanner } from '../components/ErrorBanner'

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
  { id: 'podcasts', label: 'Podcasts', query: 'Tamil podcasts' },
  { id: 'romance', label: 'Romance', query: 'Tamil love romantic songs' },
  { id: 'feelgood', label: 'Feel good', query: 'Tamil feel good melody hits' },
  { id: 'party', label: 'Party', query: 'Tamil party kuthu hits' },
  { id: 'relax', label: 'Relax', query: 'Tamil relaxing acoustic melody' },
  { id: 'commute', label: 'Commute', query: 'Tamil travel melody hits' },
  { id: 'sad', label: 'Sad', query: 'Tamil sad emotional songs' },
  { id: 'sleep', label: 'Sleep', query: 'Tamil sleep instrumental piano' },
  { id: 'energize', label: 'Energize', query: 'Tamil energetic mass bgm' },
  { id: 'workout', label: 'Workout', query: 'Tamil gym workout beats' },
  { id: 'focus', label: 'Focus', query: 'Tamil lo-fi study beats' }
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
  const videosRowRef = useRef<HTMLDivElement | null>(null)
  const listenAgainRef = useRef<HTMLDivElement | null>(null)
  const trendingRowRef = useRef<HTMLDivElement | null>(null)

  const scrollRow = (ref: React.RefObject<HTMLDivElement>, direction: 'left' | 'right') => {
    if (ref.current) {
      const scrollAmount = direction === 'left' ? -600 : 600
      ref.current.scrollBy({ left: scrollAmount, behavior: 'smooth' })
    }
  }

  const deduplicated = deduplicateSongs(trendingSongs)
  const musicVideos = deduplicated.slice(0, 10)
  const listenAgain = deduplicated.slice(10, 20)
  const trendingGrid = deduplicated.slice(20).length > 0 ? deduplicated.slice(20) : deduplicated.slice(0, 20)

  const getDynamicGreeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return 'Good Morning 👋'
    if (hour < 17) return 'Good Afternoon 👋'
    return 'Good Evening 👋'
  }

  return (
    <div className="ytm-home-container">
      {/* Dynamic Greeting & Tagline Header */}
      <div style={{ padding: '8px 4px 16px 4px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: 800, color: '#FFFFFF', margin: 0 }}>
            {getDynamicGreeting()}
          </h1>
          <p style={{ fontSize: '13px', color: '#C8FF00', fontWeight: 600, margin: '4px 0 0 0' }}>
            Un Isai. Un Feel. 🎧
          </p>
        </div>
      </div>

      {/* 1. YouTube Music Mood Pills Horizontal Scroll Bar */}
      <div className="ytm-mood-pills-row">
        {YTM_MOOD_PILLS.map((pill) => (
          <button
            key={pill.id}
            className="ytm-mood-pill"
            onClick={() => onSelectCategory(pill.query)}
          >
            {pill.label}
          </button>
        ))}
      </div>

      {isLoading ? (
        <LoadingSpinner message="Discovering Tamil music..." />
      ) : error ? (
        <ErrorBanner message={error} onRetry={onRetry} />
      ) : (
        <>
          {/* 2. Section 1: "Music videos for you" (Matching User Screenshot 16:9 Cards) */}
          {musicVideos.length > 0 && (
            <section className="ytm-section">
              <div className="ytm-section-header">
                <h2 className="ytm-section-title">Music videos for you</h2>

                <div className="ytm-section-actions">
                  <button
                    className="ytm-btn-pill-action"
                    onClick={() => onPlaySong?.(musicVideos[0])}
                  >
                    Play all
                  </button>
                  <button
                    className="ytm-btn-arrow-nav"
                    onClick={() => scrollRow(videosRowRef, 'left')}
                    title="Previous"
                  >
                    ‹
                  </button>
                  <button
                    className="ytm-btn-arrow-nav"
                    onClick={() => scrollRow(videosRowRef, 'right')}
                    title="Next"
                  >
                    ›
                  </button>
                </div>
              </div>

              <div className="ytm-landscape-scroll-row" ref={videosRowRef}>
                {musicVideos.map((song) => (
                  <div key={song.videoId} className="ytm-landscape-item">
                    <SongCard
                      song={song}
                      variant="landscape"
                      isFavorite={isFavorite(song.videoId)}
                      onToggleFavorite={onToggleFavorite}
                      onPlay={onPlaySong}
                    />
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* 3. Section 2: "JEEVA ⚡ Listen again" (User Avatar Header from Screenshot) */}
          {listenAgain.length > 0 && (
            <section className="ytm-section">
              <div className="ytm-user-section-tag">
                <div className="ytm-user-avatar-small">J</div>
                <span className="ytm-user-name">JEEVA ⚡</span>
              </div>

              <div className="ytm-section-header">
                <h2 className="ytm-section-title">Listen again</h2>

                <div className="ytm-section-actions">
                  <button
                    className="ytm-btn-pill-action"
                    onClick={() => onSelectCategory('Tamil top trending hits')}
                  >
                    More
                  </button>
                  <button
                    className="ytm-btn-arrow-nav"
                    onClick={() => scrollRow(listenAgainRef, 'left')}
                    title="Previous"
                  >
                    ‹
                  </button>
                  <button
                    className="ytm-btn-arrow-nav"
                    onClick={() => scrollRow(listenAgainRef, 'right')}
                    title="Next"
                  >
                    ›
                  </button>
                </div>
              </div>

              <div className="ytm-landscape-scroll-row" ref={listenAgainRef}>
                {listenAgain.map((song) => (
                  <div key={`la_${song.videoId}`} className="ytm-landscape-item">
                    <SongCard
                      song={song}
                      variant="landscape"
                      isFavorite={isFavorite(song.videoId)}
                      onToggleFavorite={onToggleFavorite}
                      onPlay={onPlaySong}
                    />
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* 4. Section 3: "🔥 Trending Tamil Hits" */}
          <section className="ytm-section">
            <div className="ytm-section-header">
              <div>
                <h2 className="ytm-section-title">🔥 Trending Tamil Hits</h2>
                <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Top chartbusters in 320 KBPS HD audio</p>
              </div>

              <div className="ytm-section-actions">
                <button
                  className="ytm-btn-pill-action"
                  onClick={() => onPlaySong?.(trendingGrid[0])}
                >
                  Play all
                </button>
                <button
                  className="ytm-btn-arrow-nav"
                  onClick={() => scrollRow(trendingRowRef, 'left')}
                  title="Previous"
                >
                  ‹
                </button>
                <button
                  className="ytm-btn-arrow-nav"
                  onClick={() => scrollRow(trendingRowRef, 'right')}
                  title="Next"
                >
                  ›
                </button>
              </div>
            </div>

            <div className="song-grid-portrait" ref={trendingRowRef}>
              {trendingGrid.map((song) => (
                <SongCard
                  key={`tr_${song.videoId}`}
                  song={song}
                  variant="square"
                  isFavorite={isFavorite(song.videoId)}
                  onToggleFavorite={onToggleFavorite}
                  onPlay={onPlaySong}
                />
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  )
}
