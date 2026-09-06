import { analyticsService } from './AnalyticsService'
import type { Song } from '../../../shared/models/song'

export type TrendingTimeWindow = 'today' | 'week' | 'month'

interface ReleaseDateCache {
  [videoId: string]: number // epoch timestamp in ms
}

const releaseDateCache: ReleaseDateCache = {}

/**
 * Robust Date Parser: Handles YYYY-MM-DD, ISO, YYYY, Unix Secs/Ms, or Date objects
 */
export function parseReleaseDateToMs(input: any): number {
  const now = Date.now()
  if (!input) return now - 3 * 24 * 3600 * 1000 // Default fallback: 3 days ago

  if (typeof input === 'number') {
    // If Unix timestamp in seconds (< 10_000_000_000), convert to ms
    if (input < 10000000000) return input * 1000
    return input
  }

  if (typeof input === 'string') {
    // Try parsing ISO or YYYY-MM-DD
    const parsed = Date.parse(input)
    if (!isNaN(parsed)) return parsed

    // Match 4-digit year like "2026"
    const yearMatch = input.match(/\b(20\d{2})\b/)
    if (yearMatch) {
      return new Date(parseInt(yearMatch[1], 10), 0, 1).getTime()
    }
  }

  if (input instanceof Date && !isNaN(input.getTime())) {
    return input.getTime()
  }

  return now - 3 * 24 * 3600 * 1000
}

export class TrendingService {
  /**
   * Caches release date in ms for a song to avoid repeated lookups
   */
  registerSongReleaseDate(videoId: string, rawReleaseDate: any) {
    if (videoId) {
      const parsedMs = parseReleaseDateToMs(rawReleaseDate)
      releaseDateCache[videoId] = parsedMs
    }
  }

  /**
   * Calculates Recency-Weighted Trending Score:
   * score = (plays_last_48h * 3.0 + likes * 2.5 + saves * 3.0 + plays_7d * 1.5 + plays_30d * 1.0) / (daysSinceRelease + 1)
   * + New Release Booster Factor (+21 points for 0-day old songs decaying over 7 days)
   */
  calculateTrendingScore(song: Song): { score: number; daysOld: number; boost: number; weightedPlays: number } {
    const songId = song.videoId
    const now = Date.now()
    const DAY_MS = 24 * 3600 * 1000

    // 1. Calculate weighted play signals across time windows
    const plays48h = analyticsService.getEventCount(songId, 'play', 48 * 3600 * 1000)
    const plays7d = analyticsService.getEventCount(songId, 'play', 7 * 24 * 3600 * 1000)
    const plays30d = analyticsService.getEventCount(songId, 'play', 30 * 24 * 3600 * 1000)

    const likes48h = analyticsService.getEventCount(songId, 'like', 48 * 3600 * 1000)
    const adds48h = analyticsService.getEventCount(songId, 'playlist_add', 48 * 3600 * 1000)

    const weightedPlays = (plays48h * 3.0) + (likes48h * 2.5) + (adds48h * 3.0) + (plays7d * 1.5) + (plays30d * 1.0)

    // 2. Parse & calculate days since release
    const releaseMs = releaseDateCache[songId] || (now - 3 * DAY_MS)
    const daysSinceRelease = Math.max(0, (now - releaseMs) / DAY_MS)

    // 3. New Release Booster Factor for brand-new songs (0-7 days old)
    let newReleaseBoost = 0
    if (daysSinceRelease <= 7) {
      newReleaseBoost = (7 - daysSinceRelease) * 3.0 // Up to +21 points for fresh releases with few initial plays
    }

    // 4. Recency decay denominator score
    const recencyScore = weightedPlays / (daysSinceRelease + 1.0)
    const tieBreaker = (Math.abs(this.hashCode(songId)) % 100) / 1000.0

    const finalScore = recencyScore + newReleaseBoost + tieBreaker

    return {
      score: finalScore,
      daysOld: daysSinceRelease,
      boost: newReleaseBoost,
      weightedPlays
    }
  }

  /**
   * 1. 🔥 "Trending Now" Section: Strictly songs released within last 30 days
   */
  getTrendingRecentReleases(songs: Song[], maxReleaseDays: number = 30): Song[] {
    if (!songs || songs.length === 0) return []
    const now = Date.now()
    const DAY_MS = 24 * 3600 * 1000

    console.log(`🔍 [ISAI_TRENDING_DEBUG] Stage 1: Evaluating ${songs.length} raw candidate songs`)

    // Stage 2: Filter by release date <= 30 days
    const recentEligibleSongs = songs.filter((song) => {
      const releaseMs = releaseDateCache[song.videoId] || (now - 3 * DAY_MS)
      const daysOld = (now - releaseMs) / DAY_MS
      const isEligible = daysOld <= maxReleaseDays

      if (!isEligible) {
        console.log(`🚫 [ISAI_TRENDING_DEBUG] Filtered out "${song.title}" - Released ${daysOld.toFixed(1)} days ago (> ${maxReleaseDays}d)`)
      }
      return isEligible
    })

    console.log(`✅ [ISAI_TRENDING_DEBUG] Stage 2: ${recentEligibleSongs.length} / ${songs.length} songs passed 30-day release filter`)

    // Stage 3: Score & Sort
    const scoredList = recentEligibleSongs.map((song) => {
      const meta = this.calculateTrendingScore(song)
      return { song, meta }
    })

    scoredList.sort((a, b) => b.meta.score - a.meta.score)

    // Stage 4: Log Top Rankings
    console.log('🏆 [ISAI_TRENDING_DEBUG] Stage 3: Top Trending Rankings:')
    scoredList.slice(0, 5).forEach((item, index) => {
      console.log(
        `   #${index + 1} "${item.song.title}" | Score: ${item.meta.score.toFixed(2)} | DaysOld: ${item.meta.daysOld.toFixed(1)}d | Boost: +${item.meta.boost.toFixed(1)} | Plays: ${item.meta.weightedPlays}`
      )
    })

    return scoredList.map((item) => item.song)
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
