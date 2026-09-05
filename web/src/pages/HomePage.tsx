import React from 'react'
import { TAMIL_CATEGORIES } from '@shared/constants/categories'
import type { Song } from '@shared/models/song'
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
  return (
    <div>
      {/* Hero Welcome Banner */}
      <div style={{
        padding: '36px 32px',
        borderRadius: 'var(--radius-xl)',
        background: 'linear-gradient(135deg, rgba(0, 240, 255, 0.15) 0%, rgba(139, 92, 246, 0.2) 50%, rgba(255, 46, 147, 0.15) 100%)',
        border: '1px solid var(--border-glass)',
        marginBottom: '36px',
        boxShadow: '0 20px 40px rgba(0, 0, 0, 0.4)'
      }}>
        <div style={{ display: 'inline-block', padding: '4px 12px', borderRadius: '20px', background: 'rgba(0, 240, 255, 0.15)', color: 'var(--neon-cyan)', fontSize: '12px', fontWeight: 700, marginBottom: '12px', border: '1px solid rgba(0, 240, 255, 0.3)' }}>
          ✨ OFFICIAL TAMIL MUSIC DISCOVERY
        </div>
        <h1 style={{ fontSize: '32px', fontWeight: 800, marginBottom: '8px', lineHeight: 1.2 }}>
          Discover the Soul of <span style={{ color: 'var(--neon-cyan)' }}>Tamil Music</span>
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '15px', maxWidth: '600px', lineHeight: 1.6 }}>
          Explore trending Tamil hits, timeless melodies, folk gems, and classical masterpieces in 320 KBPS Studio HD audio.
        </p>
      </div>

      {/* Tamil Music Categories */}
      <div style={{ marginBottom: '36px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
          <h2 style={{ fontSize: '22px', fontWeight: 700 }}>Tamil Music Categories</h2>
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Curated Collections</span>
        </div>

        <div className="category-grid">
          {TAMIL_CATEGORIES.map((cat) => (
            <div
              key={cat.id}
              className="category-card"
              style={{
                background: `linear-gradient(135deg, ${cat.gradient[0]} 0%, ${cat.gradient[1]} 100%)`
              }}
              onClick={() => onSelectCategory(cat.query)}
            >
              <div>
                <div className="category-title">{cat.title}</div>
                <div className="category-subtitle">{cat.subtitle}</div>
              </div>
              <div className="category-icon">{cat.icon}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Trending Tamil Songs */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
          <div>
            <h2 style={{ fontSize: '22px', fontWeight: 700 }}>🔥 Trending Tamil Songs</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Top discovered Tamil music tracks</p>
          </div>
        </div>

        {isLoading ? (
          <LoadingSpinner message="Fetching Trending Tamil songs..." />
        ) : error ? (
          <ErrorBanner message={error} onRetry={onRetry} />
        ) : (
          <div className="song-grid">
            {trendingSongs.map((song) => (
              <SongCard
                key={song.videoId}
                song={song}
                isFavorite={isFavorite(song.videoId)}
                onToggleFavorite={onToggleFavorite}
                onPlay={onPlaySong}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
