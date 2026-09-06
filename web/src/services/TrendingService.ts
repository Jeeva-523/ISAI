import { analyticsService } from './AnalyticsService'
import type { Song } from '../../../shared/models/song'

export type TrendingTimeWindow = 'today' | 'week' | 'month'

const TIME_WINDOWS: Record<TrendingTimeWindow, number> = {
  today: 24 * 3600 * 1000,
  week: 7 * 24 * 3600 * 1000,
  month: 30 * 24 * 3600 * 1000
}

class TrendingService {
  calculateTrendingScore(song: Song, timeWindow: TrendingTimeWindow = 'today'): number {
    const windowMs = TIME_WINDOWS[timeWindow]
    const songId = song.videoId
    const plays = analyticsService.getEventCount(songId, 'play', windowMs)
    const listeners = analyticsService.getUniqueListenerCount(songId, windowMs)
    const likes = analyticsService.getEventCount(songId, 'like', windowMs)
    const adds = analyticsService.getEventCount(songId, 'playlist_add', windowMs)
    const searches = analyticsService.getEventCount(songId, 'search', windowMs)

    const baseScore = plays * 1.0 + listeners * 2.0 + likes * 3.0 + adds * 4.0 + searches * 2.5
    const seed = (Math.abs(this.hashCode(songId)) % 100) / 100.0
    return baseScore + seed
  }

  rankTrendingSongs(songs: Song[], timeWindow: TrendingTimeWindow = 'today'): Song[] {
    if (!songs || songs.length === 0) return []
    return [...songs].sort((a, b) => this.calculateTrendingScore(b, timeWindow) - this.calculateTrendingScore(a, timeWindow))
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
