import React, { useRef, useState } from 'react'
import type { Song } from '@shared/models/song'
import { deduplicateSongs } from '@shared/utils/formatters'
import { SongCard } from '../components/SongCard'
import { ArtistCard, type Artist } from '../components/ArtistCard'
import { GenreTile, type Genre } from '../components/GenreTile'
import { SkeletonSongCard } from '../components/SkeletonLoader'
import { ErrorBanner } from '../components/ErrorBanner'
import { trendingService } from '../services/TrendingService'
import { ChevronLeft, ChevronRight, Play, Sparkles, ListPlus, ListStart, Heart, Plus, ChevronDown } from 'lucide-react'

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
  dailyMixes?: {
    id: string
    title: string
    subtitle: string
    coverUrl?: string
    gradient: string
    songs: Song[]
  }[]
  onSelectPlaylistDetail?: (title: string, subtitle: string, songs: Song[], coverUrl?: string, gradient?: string) => void
}

const YTM_ACTIVITY_CHIPS = [
  { id: 'energize', label: 'Energize', query: 'Tamil energetic gym workout bgm beats' },
  { id: 'workout', label: 'Workout', query: 'Tamil gym workout motivational hit songs' },
  { id: 'relax', label: 'Relax', query: 'Tamil lo-fi chill rain songs' },
  { id: 'focus', label: 'Focus', query: 'Tamil instrumental violin flute melody' },
  { id: 'commute', label: 'Commute', query: 'Tamil road trip travel songs' },
  { id: 'party', label: 'Party', query: 'Tamil party kuthu mass songs' },
  { id: 'romance', label: 'Romance', query: 'Tamil love romantic hit songs' },
  { id: 'feelgood', label: 'Feel Good', query: 'Tamil feel good melody hit songs' },
  { id: 'trending', label: 'Trending Hits', query: 'Tamil hits 2025 2026' }
]

const POPULAR_ARTISTS: Artist[] = [
  {
    name: 'Anirudh Ravichander',
    role: 'Composer & Singer',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/d/d1/Anirudh_Ravichander_at_Audi_Ritz_Style_Awards_2015.jpg/440px-Anirudh_Ravichander_at_Audi_Ritz_Style_Awards_2015.jpg',
    query: 'Anirudh Ravichander Tamil hits',
    followers: '24.5M Listeners'
  },
  {
    name: 'A.R. Rahman',
    role: 'Composer & Maestro',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/A._R._Rahman_at_the_Global_Indian_Music_Awards_2012.jpg/440px-A._R._Rahman_at_the_Global_Indian_Music_Awards_2012.jpg',
    query: 'A R Rahman Tamil hits',
    followers: '32.1M Listeners'
  },
  {
    name: 'Yuvan Shankar Raja',
    role: 'Composer & Singer',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Yuvan_Shankar_Raja_at_Pyaar_Prema_Kaadhal_Press_Meet.jpg/440px-Yuvan_Shankar_Raja_at_Pyaar_Prema_Kaadhal_Press_Meet.jpg',
    query: 'Yuvan Shankar Raja Tamil hits',
    followers: '19.8M Listeners'
  },
  {
    name: 'Harris Jayaraj',
    role: 'Composer',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Harris_Jayaraj.jpg/440px-Harris_Jayaraj.jpg',
    query: 'Harris Jayaraj Tamil hits',
    followers: '15.4M Listeners'
  },
  {
    name: 'Sid Sriram',
    role: 'Playback Singer',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Sid_Sriram_at_Adithya_Varma_Audio_Launch.jpg/440px-Sid_Sriram_at_Adithya_Varma_Audio_Launch.jpg',
    query: 'Sid Sriram Tamil hits',
    followers: '14.2M Listeners'
  },
  {
    name: 'G.V. Prakash',
    role: 'Composer & Actor',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/G._V._Prakash_Kumar_at_Kadavul_Irukaan_Kumaru_Press_Meet.jpg/440px-G._V._Prakash_Kumar_at_Kadavul_Irukaan_Kumaru_Press_Meet.jpg',
    query: 'G V Prakash Tamil hits',
    followers: '11.6M Listeners'
  },
  {
    name: 'Santhosh Narayanan',
    role: 'Music Director',
    image: 'https://upload.wikimedia.org/wikipedia/commons/thumb/2/20/Santhosh_Narayanan.jpg/440px-Santhosh_Narayanan.jpg',
    query: 'Santhosh Narayanan Tamil hits',
    followers: '9.8M Listeners'
  }
]

const GENRE_TILES: Genre[] = [
  { id: '1', name: 'Melody & Soul', query: 'Tamil feel good melody hit songs', gradient: 'linear-gradient(135deg, #EC4899, #8B5CF6)', icon: '💖' },
  { id: '2', name: 'Mass Kuthu & Party', query: 'Tamil party kuthu mass songs', gradient: 'linear-gradient(135deg, #F59E0B, #EF4444)', icon: '🔥' },
  { id: '3', name: 'Romantic Love', query: 'Tamil love romantic hit songs', gradient: 'linear-gradient(135deg, #8B5CF6, #3B82F6)', icon: '🌹' },
  { id: '4', name: 'Gym & Workout', query: 'Tamil energetic gym workout bgm beats', gradient: 'linear-gradient(135deg, #10B981, #06B6D4)', icon: '⚡' },
  { id: '5', name: 'Folk & Village', query: 'Tamil folk village songs', gradient: 'linear-gradient(135deg, #84CC16, #10B981)', icon: '🪘' },
  { id: '6', name: 'Chill & Relax', query: 'Tamil lo-fi chill rain songs', gradient: 'linear-gradient(135deg, #6366F1, #A855F7)', icon: '☕' }
]

export const HomePage: React.FC<HomePageProps> = ({
  trendingSongs,
  isLoading,
  error,
  onSelectCategory,
  onRetry,
  isFavorite,
  onToggleFavorite,
  onPlaySong,
  onAddToPlaylist,
  onAddToQueue,
  onPlayNext,
  onSelectArtist,
  currentSong,
  isPlaying = false,
  dailyMixes = [],
  onSelectPlaylistDetail
}) => {
  const [activeChip, setActiveChip] = useState<string | null>(null)

  const [gridLimit, setGridLimit] = useState(24)

  const quickPicksRef = useRef<HTMLDivElement | null>(null)
  const mixesRef = useRef<HTMLDivElement | null>(null)
  const trendingRef = useRef<HTMLDivElement | null>(null)
  const recsRef = useRef<HTMLDivElement | null>(null)
  const artistsRef = useRef<HTMLDivElement | null>(null)
  const melodiesRef = useRef<HTMLDivElement | null>(null)
  const partyRef = useRef<HTMLDivElement | null>(null)
  const retroRef = useRef<HTMLDivElement | null>(null)

  const scrollRow = (ref: React.RefObject<HTMLDivElement>, direction: 'left' | 'right') => {
    if (ref.current) {
      const scrollAmount = direction === 'left' ? -480 : 480
      ref.current.scrollBy({ left: scrollAmount, behavior: 'smooth' })
    }
  }

  const deduplicated = deduplicateSongs(trendingSongs)
  const rankedTrending = trendingService.rankTrendingSongs(deduplicated, 'today')

  // 1. YouTube Music Quick picks: 24 songs (6 columns x 4 rows)
  const quickPickSongs = rankedTrending.slice(0, 24)
  const quickPickColumns: Song[][] = []
  for (let i = 0; i < quickPickSongs.length; i += 4) {
    quickPickColumns.push(quickPickSongs.slice(i, i + 4))
  }

  // 2. Trending Viral Hits (20 songs)
  const displayTrending = rankedTrending.slice(24, 44).length >= 6
    ? rankedTrending.slice(24, 44)
    : rankedTrending.slice(0, 20)

  // 3. Soulful Melodies & Romance (20 songs)
  const melodyFiltered = rankedTrending.filter(s => {
    const t = (s.title + ' ' + s.channelTitle).toLowerCase()
    return t.includes('melody') || t.includes('love') || t.includes('kadhal') || t.includes('rahman') ||
           t.includes('harris') || t.includes('sid sriram') || t.includes('feel good') || t.includes('soul') ||
           t.includes('romance') || t.includes('nenj') || t.includes('kanave')
  })
  const melodySongs = melodyFiltered.length >= 6 ? melodyFiltered.slice(0, 20) : rankedTrending.slice(10, 30)

  // 4. Party & Mass Kuthu Beats (20 songs)
  const partyFiltered = rankedTrending.filter(s => {
    const t = (s.title + ' ' + s.channelTitle).toLowerCase()
    return t.includes('kuthu') || t.includes('party') || t.includes('dance') || t.includes('mass') ||
           t.includes('anirudh') || t.includes('sana') || t.includes('beat') || t.includes('energy') ||
           t.includes('fast') || t.includes('thara') || t.includes('local')
  })
  const partySongs = partyFiltered.length >= 6 ? partyFiltered.slice(0, 20) : rankedTrending.slice(20, 40)

  // 5. Retro & 90s Evergreens (20 songs)
  const retroFiltered = rankedTrending.filter(s => {
    const t = (s.title + ' ' + s.channelTitle).toLowerCase()
    return t.includes('ilayaraja') || t.includes('ilaiyaraaja') || t.includes('spb') || t.includes('90s') ||
           t.includes('golden') || t.includes('classic') || t.includes('evergreen') || t.includes('deva') ||
           t.includes('chitra') || t.includes('hariharan') || t.includes('swarnalatha')
  })
  const retroSongs = retroFiltered.length >= 6 ? retroFiltered.slice(0, 20) : rankedTrending.slice(30, 50)

  // 6. Recommended For You: 20 songs
  const recommendedSongs = rankedTrending.slice(12, 32).length > 0 ? rankedTrending.slice(12, 32) : displayTrending

  // 7. Discover All Songs Grid: 24 to 100+ songs
  const gridSongs = rankedTrending.slice(0, gridLimit)

  const getDynamicGreeting = () => {
    const hour = new Date().getHours()
    if (hour >= 5 && hour < 12) return 'Good Morning'
    if (hour >= 12 && hour < 17) return 'Good Afternoon'
    if (hour >= 17 && hour < 22) return 'Good Evening'
    return 'Night Vibes'
  }

  const handleActivityChipClick = (chip: typeof YTM_ACTIVITY_CHIPS[0]) => {
    if (activeChip === chip.id) {
      setActiveChip(null)
      onSelectCategory('')
    } else {
      setActiveChip(chip.id)
      onSelectCategory(chip.query)
    }
  }

  return (
    <div style={{ paddingBottom: '8px' }}>
      {/* 1. YouTube Music Activity Mood Chips Bar */}
      <div className="ytm-activity-chips-bar">
        {YTM_ACTIVITY_CHIPS.map((chip) => {
          const isSelected = activeChip === chip.id
          return (
            <button
              key={chip.id}
              className={`ytm-activity-chip ${isSelected ? 'active' : ''}`}
              onClick={() => handleActivityChipClick(chip)}
            >
              {chip.label}
            </button>
          )
        })}
      </div>

      {/* Hero Greeting Header */}
      <div style={{ marginBottom: '24px' }}>
        <span style={{ fontSize: '11px', fontWeight: 900, color: 'var(--isai-purple-light)', textTransform: 'uppercase', letterSpacing: '0.12em', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Sparkles size={14} /> LISTEN • FEEL • LIVE 🎧
        </span>
        <h1 style={{ fontSize: '28px', fontWeight: 900, marginTop: '2px', color: 'var(--text-primary)' }}>
          {getDynamicGreeting()}, <span style={{ color: 'var(--isai-purple-light)' }}>JEEVA ⚡</span>
        </h1>
      </div>

      {/* 2. YouTube Music Signature: Quick picks (4-Row Vertical Stack, Horizontal Scrolling) */}
      {quickPickSongs.length > 0 && (
        <div style={{ marginBottom: '40px' }}>
          <div className="ytm-section-header">
            <div className="ytm-section-subtitle">START RADIO FROM A SONG</div>
            <div className="ytm-section-title-row">
              <h2 className="ytm-section-title">Quick picks</h2>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button className="control-btn" onClick={() => scrollRow(quickPicksRef, 'left')} title="Previous">
                  <ChevronLeft size={22} />
                </button>
                <button className="control-btn" onClick={() => scrollRow(quickPicksRef, 'right')} title="Next">
                  <ChevronRight size={22} />
                </button>
              </div>
            </div>
          </div>

          <div ref={quickPicksRef} className="ytm-quick-picks-container">
            {quickPickColumns.map((col, colIdx) => (
              <div key={colIdx} className="ytm-quick-picks-column">
                {col.map((song) => {
                  const isThisPlaying = currentSong?.videoId === song.videoId && isPlaying
                  const isFav = isFavorite(song.videoId)
                  return (
                    <div
                      key={song.videoId}
                      className={`ytm-quick-pick-item ${isThisPlaying ? 'playing' : ''}`}
                      onClick={() => onPlaySong?.(song, rankedTrending)}
                      title={`Play ${song.title}`}
                    >
                      <div className="ytm-qp-thumb-wrap">
                        <img
                          src={song.thumbnailUrl || 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg'}
                          alt={song.title}
                          className="ytm-qp-thumb"
                          loading="lazy"
                          onError={(e) => {
                            const target = e.currentTarget
                            if (!target.src.includes('Jailer-Tamil-2023')) {
                              target.src = 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg'
                            }
                          }}
                        />
                        <div className={`ytm-qp-play-overlay ${isThisPlaying ? 'active' : ''}`}>
                          {isThisPlaying ? (
                            <div className="equalizer-wave compact">
                              <div className="equalizer-bar" />
                              <div className="equalizer-bar" />
                              <div className="equalizer-bar" />
                            </div>
                          ) : (
                            <Play size={16} fill="#ffffff" color="#ffffff" style={{ marginLeft: '2px' }} />
                          )}
                        </div>
                      </div>

                      <div className="ytm-qp-info">
                        <div className="ytm-qp-title">{song.title}</div>
                        <div className="ytm-qp-artist">
                          {song.channelTitle} • {song.durationFormatted || 'Audio'}
                        </div>
                      </div>

                      <div className="ytm-qp-actions" onClick={(e) => e.stopPropagation()}>
                        <button
                          className={`ytm-qp-btn ${isFav ? 'liked' : ''}`}
                          onClick={() => onToggleFavorite(song)}
                          title={isFav ? 'Remove Favorite' : 'Save to Favorites'}
                        >
                          <Heart
                            size={16}
                            color={isFav ? '#EC4899' : 'currentColor'}
                            fill={isFav ? '#EC4899' : 'none'}
                          />
                        </button>
                        {onPlayNext && (
                          <button
                            className="ytm-qp-btn"
                            onClick={() => onPlayNext(song)}
                            title="Play next"
                          >
                            <ListStart size={16} />
                          </button>
                        )}
                        {onAddToQueue && (
                          <button
                            className="ytm-qp-btn"
                            onClick={() => onAddToQueue(song)}
                            title="Add to queue"
                          >
                            <ListPlus size={16} />
                          </button>
                        )}
                        {onAddToPlaylist && (
                          <button
                            className="ytm-qp-btn"
                            onClick={() => onAddToPlaylist(song)}
                            title="Add to playlist"
                          >
                            <Plus size={16} />
                          </button>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            ))}
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="carousel-row">
          {[...Array(6)].map((_, i) => (
            <SkeletonSongCard key={i} />
          ))}
        </div>
      ) : error ? (
        <ErrorBanner title="Failed to Load Music Feed" message={error} onRetry={onRetry} />
      ) : (
        <div>
          {/* Section: Mixed For You */}
          {dailyMixes.length > 0 && (
            <div style={{ marginBottom: '38px' }}>
              <div className="ytm-section-header">
                <div className="ytm-section-subtitle">COMMUNITY PLAYLISTS & MIXES</div>
                <div className="ytm-section-title-row">
                  <h2 className="ytm-section-title">Mixed for you</h2>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="control-btn" onClick={() => scrollRow(mixesRef, 'left')} title="Previous">
                      <ChevronLeft size={22} />
                    </button>
                    <button className="control-btn" onClick={() => scrollRow(mixesRef, 'right')} title="Next">
                      <ChevronRight size={22} />
                    </button>
                  </div>
                </div>
              </div>

              <div className="carousel-row" ref={mixesRef}>
                {dailyMixes.map((mix) => (
                  <div
                    key={mix.id}
                    className="song-card"
                    style={{ flex: '0 0 185px', cursor: 'pointer' }}
                    onClick={() => onSelectPlaylistDetail?.(mix.title, mix.subtitle, mix.songs, mix.coverUrl, mix.gradient)}
                  >
                    <div
                      className="song-thumbnail-wrap"
                      style={{
                        background: mix.gradient,
                        aspectRatio: '1',
                        borderRadius: 'var(--radius-lg)',
                        boxShadow: '0 8px 24px rgba(0, 0, 0, 0.45)',
                        position: 'relative',
                        overflow: 'hidden'
                      }}
                    >
                      {mix.coverUrl ? (
                        <img src={mix.coverUrl} alt={mix.title} className="song-thumbnail" loading="lazy" />
                      ) : (
                        <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '36px' }}>
                          🎧
                        </div>
                      )}
                      <div className="play-hover-overlay">
                        <div
                          className="play-icon-circle"
                          style={{ background: 'var(--isai-purple)', width: '46px', height: '46px' }}
                          onClick={(e) => {
                            e.stopPropagation()
                            if (mix.songs.length > 0) onPlaySong?.(mix.songs[0], mix.songs)
                          }}
                        >
                          <Play size={20} fill="#ffffff" color="#ffffff" style={{ marginLeft: '2px' }} />
                        </div>
                      </div>
                    </div>
                    <h3 className="song-title" style={{ marginTop: '10px', fontSize: '14.5px', fontWeight: 700 }}>{mix.title}</h3>
                    <p className="song-artist" style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{mix.subtitle}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Section 1: Trending Songs Carousel (Pure Audio) */}
          <div style={{ marginBottom: '38px' }}>
            <div className="ytm-section-header">
              <div className="ytm-section-subtitle">LISTEN AGAIN & VIRAL HITS</div>
              <div className="ytm-section-title-row">
                <h2 className="ytm-section-title">Trending songs</h2>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="control-btn" onClick={() => scrollRow(trendingRef, 'left')} title="Previous">
                    <ChevronLeft size={22} />
                  </button>
                  <button className="control-btn" onClick={() => scrollRow(trendingRef, 'right')} title="Next">
                    <ChevronRight size={22} />
                  </button>
                </div>
              </div>
            </div>

            <div ref={trendingRef} className="carousel-row">
              {displayTrending.map((song) => (
                <SongCard
                  key={song.videoId}
                  song={song}
                  isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                  isFavorite={isFavorite(song.videoId)}
                  onToggleFavorite={onToggleFavorite}
                  onPlay={(s) => onPlaySong?.(s, rankedTrending)}
                  onAddToPlaylist={onAddToPlaylist}
                  onAddToQueue={onAddToQueue}
                  onPlayNext={onPlayNext}
                />
              ))}
            </div>
          </div>

          {/* Section 2: Popular Artists Carousel */}
          <div style={{ marginBottom: '38px' }}>
            <div className="ytm-section-header">
              <div className="ytm-section-subtitle">SIMILAR TO YOUR FAVORITES</div>
              <div className="ytm-section-title-row">
                <h2 className="ytm-section-title">Popular artists</h2>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="control-btn" onClick={() => scrollRow(artistsRef, 'left')} title="Previous">
                    <ChevronLeft size={22} />
                  </button>
                  <button className="control-btn" onClick={() => scrollRow(artistsRef, 'right')} title="Next">
                    <ChevronRight size={22} />
                  </button>
                </div>
              </div>
            </div>

            <div ref={artistsRef} className="carousel-row">
              {POPULAR_ARTISTS.map((artist) => (
                <ArtistCard
                  key={artist.name}
                  artist={artist}
                  onSelectArtist={(art) => onSelectArtist?.(art)}
                />
              ))}
            </div>
          </div>

          {/* Section 3: Recommended Music Carousel */}
          <div style={{ marginBottom: '38px' }}>
            <div className="ytm-section-header">
              <div className="ytm-section-subtitle">RECOMMENDED FOR YOU</div>
              <div className="ytm-section-title-row">
                <h2 className="ytm-section-title">Recommended music</h2>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button className="control-btn" onClick={() => scrollRow(recsRef, 'left')} title="Previous">
                    <ChevronLeft size={22} />
                  </button>
                  <button className="control-btn" onClick={() => scrollRow(recsRef, 'right')} title="Next">
                    <ChevronRight size={22} />
                  </button>
                </div>
              </div>
            </div>

            <div ref={recsRef} className="carousel-row">
              {recommendedSongs.map((song) => (
                <SongCard
                  key={song.videoId}
                  song={song}
                  isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                  isFavorite={isFavorite(song.videoId)}
                  onToggleFavorite={onToggleFavorite}
                  onPlay={(s) => onPlaySong?.(s, rankedTrending)}
                  onAddToPlaylist={onAddToPlaylist}
                  onAddToQueue={onAddToQueue}
                  onPlayNext={onPlayNext}
                />
              ))}
            </div>
          </div>

          {/* Section: Soulful Melodies & Romance */}
          {melodySongs.length > 0 && (
            <div style={{ marginBottom: '38px' }}>
              <div className="ytm-section-header">
                <div className="ytm-section-subtitle">FEEL-GOOD ROMANTIC VIBES</div>
                <div className="ytm-section-title-row">
                  <h2 className="ytm-section-title">Soulful Melodies & Romance 💖</h2>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="control-btn" onClick={() => scrollRow(melodiesRef, 'left')} title="Previous">
                      <ChevronLeft size={22} />
                    </button>
                    <button className="control-btn" onClick={() => scrollRow(melodiesRef, 'right')} title="Next">
                      <ChevronRight size={22} />
                    </button>
                  </div>
                </div>
              </div>

              <div ref={melodiesRef} className="carousel-row">
                {melodySongs.map((song) => (
                  <SongCard
                    key={song.videoId}
                    song={song}
                    isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                    isFavorite={isFavorite(song.videoId)}
                    onToggleFavorite={onToggleFavorite}
                    onPlay={(s) => onPlaySong?.(s, rankedTrending)}
                    onAddToPlaylist={onAddToPlaylist}
                    onAddToQueue={onAddToQueue}
                    onPlayNext={onPlayNext}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Section: Party & Mass Kuthu Beats */}
          {partySongs.length > 0 && (
            <div style={{ marginBottom: '38px' }}>
              <div className="ytm-section-header">
                <div className="ytm-section-subtitle">HIGH ENERGY CLUB & FAST BEATS</div>
                <div className="ytm-section-title-row">
                  <h2 className="ytm-section-title">Party & Mass Kuthu Beats 🔥</h2>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="control-btn" onClick={() => scrollRow(partyRef, 'left')} title="Previous">
                      <ChevronLeft size={22} />
                    </button>
                    <button className="control-btn" onClick={() => scrollRow(partyRef, 'right')} title="Next">
                      <ChevronRight size={22} />
                    </button>
                  </div>
                </div>
              </div>

              <div ref={partyRef} className="carousel-row">
                {partySongs.map((song) => (
                  <SongCard
                    key={song.videoId}
                    song={song}
                    isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                    isFavorite={isFavorite(song.videoId)}
                    onToggleFavorite={onToggleFavorite}
                    onPlay={(s) => onPlaySong?.(s, rankedTrending)}
                    onAddToPlaylist={onAddToPlaylist}
                    onAddToQueue={onAddToQueue}
                    onPlayNext={onPlayNext}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Section: Retro & 90s Golden Era */}
          {retroSongs.length > 0 && (
            <div style={{ marginBottom: '38px' }}>
              <div className="ytm-section-header">
                <div className="ytm-section-subtitle">TIMELESS EVERGREEN HITS</div>
                <div className="ytm-section-title-row">
                  <h2 className="ytm-section-title">Retro & 90s Evergreens 📻</h2>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="control-btn" onClick={() => scrollRow(retroRef, 'left')} title="Previous">
                      <ChevronLeft size={22} />
                    </button>
                    <button className="control-btn" onClick={() => scrollRow(retroRef, 'right')} title="Next">
                      <ChevronRight size={22} />
                    </button>
                  </div>
                </div>
              </div>

              <div ref={retroRef} className="carousel-row">
                {retroSongs.map((song) => (
                  <SongCard
                    key={song.videoId}
                    song={song}
                    isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                    isFavorite={isFavorite(song.videoId)}
                    onToggleFavorite={onToggleFavorite}
                    onPlay={(s) => onPlaySong?.(s, rankedTrending)}
                    onAddToPlaylist={onAddToPlaylist}
                    onAddToQueue={onAddToQueue}
                    onPlayNext={onPlayNext}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Section: Discover All Chartbusters Grid (Continuous Browser Feed) */}
          {gridSongs.length > 0 && (
            <div style={{ marginBottom: '45px' }}>
              <div className="ytm-section-header">
                <div className="ytm-section-subtitle">UNLIMITED PLAYLIST EXPLORER</div>
                <div className="ytm-section-title-row">
                  <h2 className="ytm-section-title">All Trending Chartbusters ({rankedTrending.length} Songs)</h2>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: '20px' }}>
                {gridSongs.map((song) => (
                  <SongCard
                    key={song.videoId}
                    song={song}
                    isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                    isFavorite={isFavorite(song.videoId)}
                    onToggleFavorite={onToggleFavorite}
                    onPlay={(s) => onPlaySong?.(s, rankedTrending)}
                    onAddToPlaylist={onAddToPlaylist}
                    onAddToQueue={onAddToQueue}
                    onPlayNext={onPlayNext}
                  />
                ))}
              </div>

              {gridLimit < rankedTrending.length && (
                <div style={{ display: 'flex', justifyContent: 'center', marginTop: '28px' }}>
                  <button
                    className="pill-button active"
                    style={{
                      padding: '12px 32px',
                      fontSize: '15px',
                      fontWeight: 700,
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      borderRadius: '50px',
                      background: 'linear-gradient(135deg, var(--isai-purple), var(--isai-purple-dark))',
                      boxShadow: '0 6px 20px rgba(124, 58, 237, 0.4)'
                    }}
                    onClick={() => setGridLimit((prev) => Math.min(rankedTrending.length, prev + 24))}
                  >
                    <ChevronDown size={18} />
                    Load More Hits ({rankedTrending.length - gridLimit} remaining)
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Section: Genre / Mood Grid */}
          <div style={{ marginBottom: '40px' }}>
            <div className="ytm-section-header">
              <div className="ytm-section-subtitle">EXPLORE GENRES & MOODS</div>
              <h2 className="ytm-section-title" style={{ marginBottom: '16px' }}>
                Browse by mood
              </h2>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '16px' }}>
              {GENRE_TILES.map((genre) => (
                <GenreTile
                  key={genre.id}
                  genre={genre}
                  onSelect={(q) => onSelectCategory(q)}
                />
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
