import { analyticsService } from './AnalyticsService'
import type { Song } from '../../../shared/models/song'

export type TrendingTimeWindow = 'today' | 'week' | 'month'

export interface TopArtistData {
  name: string
  playCount: number
}

export class TrendingService {
  /**
   * 1. 🏆 "Most Played Songs": pure cumulative play count sorting (No release date penalty)
   */
  getMostPlayedSongs(songs: Song[]): Song[] {
    if (!songs || songs.length === 0) return []
    return [...songs].sort((a, b) => {
      const playsA = analyticsService.getEventCount(a.videoId, 'play', 30 * 24 * 3600 * 1000)
      const playsB = analyticsService.getEventCount(b.videoId, 'play', 30 * 24 * 3600 * 1000)
      return playsB - playsA
    })
  }

  /**
   * 2. 🎤 "Popular Singers / Artists": Aggregates play counts grouped by artist name
   */
  getPopularArtists(songs: Song[], limit: number = 10): TopArtistData[] {
    if (!songs || songs.length === 0) return []
    const artistPlayMap: Record<string, number> = {}

    songs.forEach((song) => {
      const artist = (song.channelTitle || '').replace(/ - Topic| Official/gi, '').trim()
      if (artist && artist !== 'Tamil Artist') {
        const plays = analyticsService.getEventCount(song.videoId, 'play', 30 * 24 * 3600 * 1000)
        artistPlayMap[artist] = (artistPlayMap[artist] || 0) + (plays > 0 ? plays : 1)
      }
    })

    return Object.keys(artistPlayMap)
      .sort((a, b) => artistPlayMap[b] - artistPlayMap[a])
      .slice(0, limit)
      .map((name) => ({ name, playCount: artistPlayMap[name] }))
  }

  /**
   * Alias for backward compatibility with existing components
   */
  rankTrendingSongs(songs: Song[], _timeWindow?: string): Song[] {
    return this.getMostPlayedSongs(songs)
  }

  getTrendingRecentReleases(songs: Song[], _maxReleaseDays: number = 30): Song[] {
    return this.getMostPlayedSongs(songs)
  }

  getTopChartbusters(songs: Song[]): Song[] {
    return this.getMostPlayedSongs(songs)
  }
}

export const trendingService = new TrendingService()
