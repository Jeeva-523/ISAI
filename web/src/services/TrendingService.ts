import { analyticsService } from './AnalyticsService'
import type { Song } from '../../../shared/models/song'

export type TrendingTimeWindow = 'today' | 'week' | 'month'

interface ReleaseDateCache {
  [videoId: string]: number // epoch timestamp in ms
}

const releaseDateCache: ReleaseDateCache = {}

export class TrendingService {
  /**
   * Caches release date in ms for a song to avoid repeated lookups
   */
  registerSongReleaseDate(videoId: string, releaseDateMs: number) {
    if (videoId && releaseDateMs) {
      releaseDateCache[videoId] = releaseDateMs
    }
  }

  /**
   * Calculates Recency-Weighted Trending Score:
   * score = (plays_last_48h * 3.0 + plays_last_7d * 1.5 + plays_last_30d * 1.0) / (daysSinceRelease + 1)
   */
  calculateTrendingScore(song: Song): number {
    const songId = song.videoId
    const now = Date.now()

    // 1. Calculate weighted play signals across time windows
    const plays48h = analyticsService.getEventCount(songId, 'play', 48 * 3600 * 1000)
    const plays7d = analyticsService.getEventCount(songId, 'play', 7 * 24 * 3600 * 1000)
    const plays30d = analyticsService.getEventCount(songId, 'play', 30 * 24 * 3600 * 1000)

    const likes48h = analyticsService.getEventCount(songId, 'like', 48 * 3600 * 1000)
    const adds48h = analyticsService.getEventCount(songId, 'playlist_add', 48 * 3600 * 1000)

    const weightedPlays = (plays48h * 3.0) + (likes48h * 2.5) + (adds48h * 3.0) + (plays7d * 1.5) + (plays30d * 1.0)

    // 2. Calculate days since release
    const releaseMs = releaseDateCache[songId] || (now - 7 * 24 * 3600 * 1000) // Default to 7 days if unknown
    const daysSinceRelease = Math.max(0, (now - releaseMs) / (24 * 3600 * 1000))

    // 3. Score with recency decay denominator
    const recencyScore = weightedPlays / (daysSinceRelease + 1.0)
    const tieBreaker = (Math.abs(this.hashCode(songId)) % 100) / 1000.0

    return recencyScore + tieBreaker
  }

  /**
   * 1. 🔥 "Trending Now" Section: Strictly songs released within last 30 days
   */
  getTrendingRecentReleases(songs: Song[], maxReleaseDays: number = 30): Song[] {
    if (!songs || songs.length === 0) return []
    const now = Date.now()

    const recentEligibleSongs = songs.filter((song) => {
      const releaseMs = releaseDateCache[song.videoId]
      if (!releaseMs) return true // Include if metadata pending
      const daysOld = (now - releaseMs) / (24 * 3600 * 1000)
      return daysOld <= maxReleaseDays
    })

    return [...recentEligibleSongs].sort((a, b) => this.calculateTrendingScore(b) - this.calculateTrendingScore(a))
  }

  /**
   * Alias method for backward compatibility
   */
  rankTrendingSongs(songs: Song[], _timeWindow?: string): Song[] {
    return this.getTrendingRecentReleases(songs, 30)
  }

  /**
   * 2. 🏆 "Top Charts / Popular" Section: All-time popular songs without release date restriction
   */
  getTopChartbusters(songs: Song[]): Song[] {
    if (!songs || songs.length === 0) return []
    return [...songs].sort((a, b) => {
      const playsA = analyticsService.getEventCount(a.videoId, 'play', 30 * 24 * 3600 * 1000)
      const playsB = analyticsService.getEventCount(b.videoId, 'play', 30 * 24 * 3600 * 1000)
      return playsB - playsA
    })
  }

  private hashCode(str: string): number {
    let hash = 0
    for (let i = 0; i < str.length; i++) {
      hash = (hash << 5) - hash + str.charCodeAt(i)
      hash |= 0
    }
    return hash
  }
}

export const trendingService = new TrendingService()
