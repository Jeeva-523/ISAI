import React, { useState, useEffect } from 'react'
import type { Song } from '@shared/models/song'
import { QUICK_SEARCH_QUERIES } from '@shared/constants/categories'
import { CategoryChips } from '../components/CategoryChips'
import { SongCard } from '../components/SongCard'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { ErrorBanner } from '../components/ErrorBanner'
import { EmptyState } from '../components/EmptyState'

interface SearchPageProps {
  query: string
  results: Song[]
  isLoading: boolean
  error: string | null
  onQueryChange: (q: string) => void
  onRetry: () => void
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong?: (song: Song) => void
}

const TANGLISH_MAP: Record<string, string> = {
  anirud: 'Anirudh Ravichander',
  aniruth: 'Anirudh Ravichander',
  kadhal: 'காதல் (Love Songs)',
  kaadhal: 'காதல் (Love Songs)',
  kathal: 'காதல் (Love Songs)',
  vaathi: 'வாத்தி (Vaathi Coming)',
  vikram: '🎬 Vikram (Movie Soundtrack)',
  sad: '💔 Sad Melodies'
}

export const SearchPage: React.FC<SearchPageProps> = ({
  query,
  results,
  isLoading,
  error,
  onQueryChange,
  onRetry,
  isFavorite,
  onToggleFavorite,
  onPlaySong
}) => {
  const [history, setHistory] = useState<string[]>([])

  useEffect(() => {
    try {
      const stored = localStorage.getItem('isai_search_history')
      if (stored) setHistory(JSON.parse(stored))
    } catch {}
  }, [])

  useEffect(() => {
    if (query && query.trim().length > 1) {
      setHistory((prev) => {
        const filtered = prev.filter((item) => item.toLowerCase() !== query.toLowerCase())
        const updated = [query.trim(), ...filtered].slice(0, 10)
        try {
          localStorage.setItem('isai_search_history', JSON.stringify(updated))
        } catch {}
        return updated
      })
    }
  }, [query])

  const removeHistoryItem = (itemToRemove: string, e: React.MouseEvent) => {
    e.stopPropagation()
    setHistory((prev) => {
      const updated = prev.filter((i) => i !== itemToRemove)
      try {
        localStorage.setItem('isai_search_history', JSON.stringify(updated))
      } catch {}
      return updated
    })
  }

  const clearHistory = () => {
    setHistory([])
    try {
      localStorage.removeItem('isai_search_history')
    } catch {}
  }

  const cleanQ = query.trim().toLowerCase()
  const suggestionHint = TANGLISH_MAP[cleanQ] || null
  const topResult = results.length > 0 ? results[0] : null

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', paddingBottom: '60px' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '26px', fontWeight: 800, marginBottom: '6px' }}>
          ISAI Smart <span style={{ color: 'var(--isai-lime)' }}>Search Discovery</span>
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
          Search songs, artists, Tamil & Tanglish titles, movies, or moods
        </p>
      </div>

      {/* Recent Search History */}
      {history.length > 0 && !query && (
        <div style={{ marginBottom: '24px', background: 'var(--surface-card)', padding: '16px 20px', borderRadius: '16px', border: '1px solid var(--border-subtle)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              🕘 Recent Searches
            </span>
            <button onClick={clearHistory} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', fontSize: '12px', cursor: 'pointer' }}>
              Clear All
            </button>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {history.map((item) => (
              <div
                key={item}
                onClick={() => onQueryChange(item)}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '6px 14px',
                  background: 'var(--surface-dark)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: '20px',
                  fontSize: '13px',
                  color: 'var(--text-primary)',
                  cursor: 'pointer'
                }}
              >
                <span>{item}</span>
                <span
                  onClick={(e) => removeHistoryItem(item, e)}
                  style={{ color: 'var(--text-muted)', fontSize: '12px', padding: '0 2px' }}
                >
                  ✕
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tanglish Transliteration Suggestion */}
      {suggestionHint && (
        <div
          onClick={() => onQueryChange(suggestionHint.replace(/ \(.*\)/, ''))}
          style={{
            marginBottom: '20px',
            padding: '12px 18px',
            background: 'rgba(200, 255, 0, 0.08)',
            border: '1px solid var(--isai-lime)',
            borderRadius: '12px',
            color: 'var(--isai-lime)',
            fontSize: '14px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}
        >
          <span>💡 Did you mean:</span>
          <strong>{suggestionHint}</strong>
        </div>
      )}

      {/* Quick Search Chips */}
      <CategoryChips selectedQuery={query} onSelect={(q) => onQueryChange(q)} />

      {/* State Renderers */}
      {isLoading ? (
        <LoadingSpinner message="Discovering Tamil songs..." subMessage={query ? `Searching "${query}"` : undefined} />
      ) : error ? (
        <ErrorBanner title="Search Error" message={error} onRetry={onRetry} />
      ) : query && results.length === 0 ? (
        <EmptyState
          icon="🔍"
          title="No Matching Songs Found"
          description={`We couldn't find any songs matching "${query}". Try searching for artist names like Anirudh or A.R. Rahman, movie names like Vikram or Leo, or Tamil/Tanglish words.`}
          suggestions={QUICK_SEARCH_QUERIES.slice(0, 4)}
          onSuggestionClick={(s) => onQueryChange(s)}
        />
      ) : results.length > 0 ? (
        <div>
          {/* Top Result Card */}
          {topResult && (
            <div style={{ marginBottom: '28px' }}>
              <h3 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '12px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                🌟 Top Result
              </h3>
              <div
                onClick={() => onPlaySong && onPlaySong(topResult)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '20px',
                  padding: '18px 24px',
                  background: 'linear-gradient(135deg, var(--surface-card) 0%, #1E222C 100%)',
                  border: '1px solid var(--border-glass)',
                  borderRadius: '20px',
                  cursor: 'pointer'
                }}
              >
                <img
                  src={topResult.thumbnailUrl}
                  alt={topResult.title}
                  style={{ width: '80px', height: '80px', borderRadius: '16px', objectFit: 'cover' }}
                />
                <div style={{ flex: 1 }}>
                  <span style={{ fontSize: '11px', fontWeight: 700, background: 'var(--isai-lime)', color: '#000', padding: '2px 8px', borderRadius: '10px', textTransform: 'uppercase' }}>
                    Song
                  </span>
                  <h2 style={{ fontSize: '20px', fontWeight: 800, marginTop: '4px', color: 'var(--text-primary)' }}>
                    {topResult.title}
                  </h2>
                  <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
                    {topResult.channelTitle}
                  </p>
                </div>
                <button
                  style={{
                    width: '50px',
                    height: '50px',
                    borderRadius: '50%',
                    background: 'var(--isai-lime)',
                    border: 'none',
                    color: '#000',
                    fontSize: '20px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  ▶
                </button>
              </div>
            </div>
          )}

          {/* Songs List */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
            <div>
              <h2 style={{ fontSize: '18px', fontWeight: 700 }}>
                Discovered Songs ({results.length})
              </h2>
              <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                ISAI HD Music • Tap song to play
              </p>
            </div>
          </div>

          <div className="ytm-grid">
            {results.map((song) => (
              <SongCard
                key={song.videoId}
                song={song}
                isFavorite={isFavorite(song.videoId)}
                onToggleFavorite={onToggleFavorite}
                onPlay={() => onPlaySong && onPlaySong(song)}
              />
            ))}
          </div>
        </div>
      ) : null}
    </div>
  )
}
