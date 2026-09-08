import React, { useRef, useState } from 'react'
import type { Song } from '@shared/models/song'
import { deduplicateSongs } from '@shared/utils/formatters'
import { SongCard } from '../components/SongCard'
import { SongListItem } from '../components/SongListItem'
import { ArtistCard, type Artist } from '../components/ArtistCard'
import { GenreTile, type Genre } from '../components/GenreTile'
import { SkeletonSongCard } from '../components/SkeletonLoader'
import { ErrorBanner } from '../components/ErrorBanner'
import { trendingService } from '../services/TrendingService'
import { ChevronLeft, ChevronRight, Play, Sparkles, Flame, Radio, Trophy, RefreshCw } from 'lucide-react'

interface HomePageProps {
  trendingSongs: Song[]
  isLoading: boolean
  error: string | null
  onSelectCategory: (query: string) => void
  onRetry: () => void
  isFavorite: (videoId: string) => boolean
  onToggleFavorite: (song: Song) => void
  onPlaySong?: (song: Song) => void
  onAddToPlaylist?: (song: Song) => void
  onSelectArtist?: (artist: Artist) => void
  currentSong?: Song | null
  isPlaying?: boolean
}

const CATEGORY_PILLS = [
  { label: 'Most Played', query: 'Latest Tamil hits' },
  { label: 'Melody', query: 'Tamil feel good melody hit songs' },
  { label: 'Love Songs', query: 'Tamil love romantic hit songs' },
  { label: 'Party & Kuthu', query: 'Tamil party kuthu mass songs' },
  { label: 'Folk', query: 'Tamil folk village songs' },
  { label: 'Workout & Beats', query: 'Tamil energetic gym workout bgm beats' },
  { label: 'New Releases', query: 'Latest Tamil movie songs 2024' }
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
  onSelectArtist,
  currentSong,
  isPlaying = false
}) => {
  const [selectedCategory, setSelectedCategory] = useState('Most Played')
  const [mostPlayedPage, setMostPlayedPage] = useState(0)

  const trendingRef = useRef<HTMLDivElement | null>(null)
  const recsRef = useRef<HTMLDivElement | null>(null)
  const artistsRef = useRef<HTMLDivElement | null>(null)

  const scrollRow = (ref: React.RefObject<HTMLDivElement>, direction: 'left' | 'right') => {
    if (ref.current) {
      const scrollAmount = direction === 'left' ? -480 : 480
      ref.current.scrollBy({ left: scrollAmount, behavior: 'smooth' })
    }
  }

  const deduplicated = deduplicateSongs(trendingSongs)
  const rankedTrending = trendingService.rankTrendingSongs(deduplicated, 'today')

  const quickAccessTiles = rankedTrending.slice(0, 6)
  const carouselTrending = deduplicated.length > 0 ? deduplicated : rankedTrending
  const recommendedSongs = deduplicated.length > 3 ? [...deduplicated.slice(3), ...deduplicated.slice(0, 3)] : deduplicated

  const getDynamicGreeting = () => {
    const hour = new Date().getHours()
    if (hour >= 5 && hour < 12) return 'Good Morning'
    if (hour >= 12 && hour < 17) return 'Good Afternoon'
    if (hour >= 17 && hour < 22) return 'Good Evening'
    return 'Night Vibes'
  }

  const handleCategoryClick = (cat: typeof CATEGORY_PILLS[0]) => {
    setSelectedCategory(cat.label)
    if (cat.label !== 'Most Played') {
      onSelectCategory(cat.query)
    }
  }

  return (
    <div style={{ paddingBottom: '8px' }}>
      {/* Category Pills Header */}
      <div style={{ marginBottom: '20px', overflowX: 'auto', paddingBottom: '4px' }}>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          {CATEGORY_PILLS.map((pill) => {
            const isSelected = selectedCategory === pill.label
            return (
              <button
                key={pill.label}
                className={`pill-button ${isSelected ? 'active' : ''}`}
                onClick={() => handleCategoryClick(pill)}
              >
                {pill.label}
              </button>
            )
          })}
        </div>
      </div>

      {/* Hero Greeting Header */}
      <div style={{ marginBottom: '22px' }}>
        <span style={{ fontSize: '11px', fontWeight: 900, color: 'var(--isai-purple-light)', textTransform: 'uppercase', letterSpacing: '0.12em', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Sparkles size={14} /> LISTEN • FEEL • LIVE 🎧
        </span>
        <h1 style={{ fontSize: '28px', fontWeight: 900, marginTop: '2px', color: 'var(--text-primary)' }}>
          {getDynamicGreeting()}, <span style={{ color: 'var(--isai-purple-light)' }}>JEEVA ⚡</span>
        </h1>
      </div>

      {/* Quick Access 2x3 Grid Tiles */}
      {quickAccessTiles.length > 0 && (
        <div className="quick-access-grid">
          {quickAccessTiles.map((song) => (
            <div
              key={song.videoId}
              className="quick-tile"
              onClick={() => onPlaySong?.(song)}
            >
              <img src={song.thumbnailUrl} alt={song.title} className="quick-tile-art" />
              <div className="quick-tile-title">{song.title}</div>
              <div className="quick-tile-play-btn">
                <Play size={18} fill="#ffffff" style={{ marginLeft: '2px' }} />
              </div>
            </div>
          ))}
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
          {/* Section 1: Trending Now Carousel */}
          <div style={{ marginBottom: '36px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Flame color="var(--isai-pink)" size={20} />
                <h2 style={{ fontSize: '20px', fontWeight: 900 }}>Trending Now</h2>
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button className="control-btn" onClick={() => scrollRow(trendingRef, 'left')}>
                  <ChevronLeft size={22} />
                </button>
                <button className="control-btn" onClick={() => scrollRow(trendingRef, 'right')}>
                  <ChevronRight size={22} />
                </button>
              </div>
            </div>

            <div ref={trendingRef} className="carousel-row">
              {carouselTrending.map((song) => (
                <SongCard
                  key={song.videoId}
                  song={song}
                  isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                  isFavorite={isFavorite(song.videoId)}
                  onToggleFavorite={onToggleFavorite}
                  onPlay={onPlaySong}
                  onAddToPlaylist={onAddToPlaylist}
                />
              ))}
            </div>
          </div>

          {/* Section 2: Popular Artists Carousel */}
          <div style={{ marginBottom: '36px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Radio color="var(--isai-purple-light)" size={20} />
                <h2 style={{ fontSize: '20px', fontWeight: 900 }}>Popular Artists</h2>
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button className="control-btn" onClick={() => scrollRow(artistsRef, 'left')}>
                  <ChevronLeft size={22} />
                </button>
                <button className="control-btn" onClick={() => scrollRow(artistsRef, 'right')}>
                  <ChevronRight size={22} />
                </button>
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

          {/* Section 3: Recommended For You Carousel */}
          <div style={{ marginBottom: '36px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Sparkles color="var(--isai-cyan)" size={20} />
                <h2 style={{ fontSize: '20px', fontWeight: 900 }}>Made For You</h2>
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button className="control-btn" onClick={() => scrollRow(recsRef, 'left')}>
                  <ChevronLeft size={22} />
                </button>
                <button className="control-btn" onClick={() => scrollRow(recsRef, 'right')}>
                  <ChevronRight size={22} />
                </button>
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
                  onPlay={onPlaySong}
                  onAddToPlaylist={onAddToPlaylist}
                />
              ))}
            </div>
          </div>

          {/* Section 4: Genre / Mood Grid */}
          <div style={{ marginBottom: '40px' }}>
            <h2 style={{ fontSize: '22px', fontWeight: 900, marginBottom: '16px' }}>
              Browse by Mood
            </h2>
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

          {/* Section 5: Most Played List */}
          <div style={{ marginBottom: '40px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <Trophy color="#F59E0B" size={22} />
              <h2 style={{ fontSize: '22px', fontWeight: 900 }}>Most Played</h2>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {rankedTrending.slice(mostPlayedPage * 25, (mostPlayedPage + 1) * 25).map((song, index) => (
                <SongListItem
                  key={song.videoId}
                  song={song}
                  index={mostPlayedPage * 25 + index}
                  isPlaying={currentSong?.videoId === song.videoId && isPlaying}
                  isFavorite={isFavorite(song.videoId)}
                  onToggleFavorite={onToggleFavorite}
                  onPlay={onPlaySong}
                  onAddToPlaylist={onAddToPlaylist}
                />
              ))}
            </div>
            
            {rankedTrending.length > 25 && (
              <div style={{ display: 'flex', justifyContent: 'center', marginTop: '24px' }}>
                <button 
                  className="pill-button active" 
                  onClick={() => setMostPlayedPage(p => (p + 1) % Math.ceil(rankedTrending.length / 25))}
                  style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 24px', fontWeight: 700 }}
                >
                  <RefreshCw size={18} />
                  Refresh List
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
