import { API_CONFIG } from '../constants/api'
import type { Song } from '../models/song'
import { cleanHtmlTitle, deduplicateSongs } from '../utils/formatters'

export interface MusicApiOptions {
  baseUrl?: string
}

function mapToSong(item: any): Song {
  // If it's JioSaavn / NepoTuneAPI format:
  if (item.name && item.downloadUrl) {
    const downloadUrls = Array.isArray(item.downloadUrl) ? item.downloadUrl : []
    const audio320 = downloadUrls.find((d: any) => d.quality === '320kbps')?.url
    const audio160 = downloadUrls.find((d: any) => d.quality === '160kbps')?.url
    const audioUrl = audio320 || audio160 || downloadUrls.at(-1)?.url || ''

    const images = Array.isArray(item.image) ? item.image : []
    const img500 = images.find((i: any) => i.quality === '500x500')?.url
    const img150 = images.find((i: any) => i.quality === '150x150')?.url
    const thumbnailUrl = img500 || img150 || images.at(-1)?.url || (typeof item.image === 'string' ? item.image : '')

    const artistNames =
      item.artists?.primary?.map((a: any) => a.name).join(', ') ||
      item.artists?.all?.map((a: any) => a.name).join(', ') ||
      item.subtitle ||
      'Tamil Artist'

    const durSec = Number(item.duration) || 0
    const mins = Math.floor(durSec / 60)
    const secs = durSec % 60
    const durationFormatted = durSec > 0 ? `${mins}:${secs < 10 ? '0' : ''}${secs}` : '3:30'

    return {
      videoId: item.id,
      title: cleanHtmlTitle(item.name || ''),
      channelTitle: cleanHtmlTitle(artistNames),
      thumbnailUrl,
      durationFormatted,
      durationMs: durSec > 0 ? durSec * 1000 : 210000,
      viewCountFormatted: item.playCount ? `${Number(item.playCount).toLocaleString()} plays` : '',
      album: cleanHtmlTitle(item.album?.name || ''),
      audioUrl
    }
  }

  // Fallback for YouTube or already-formed Song items:
  const videoId = item.videoId || item.id || ''
  const ytThumb = videoId && videoId.length === 11 ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : ''
  const rawThumb = item.thumbnailUrl || (typeof item.image === 'string' ? item.image : '') || ''

  return {
    videoId,
    title: cleanHtmlTitle(item.title || item.name || ''),
    channelTitle: cleanHtmlTitle(item.channelTitle || item.artists || ''),
    thumbnailUrl: rawThumb || ytThumb,
    durationFormatted: item.durationFormatted || '3:30',
    durationMs: item.durationMs || 210000,
    viewCountFormatted: item.viewCountFormatted || '',
    album: cleanHtmlTitle(item.album || ''),
    audioUrl: item.audioUrl || ''
  }
}

/**
 * Filter out repeated compilation album artwork so every card has a distinct picture
 */
function ensureDistinctThumbnails(songs: Song[]): Song[] {
  const imageCounts = new Map<string, number>()
  
  for (const song of songs) {
    if (song.thumbnailUrl) {
      imageCounts.set(song.thumbnailUrl, (imageCounts.get(song.thumbnailUrl) || 0) + 1)
    }
  }

  return songs.map((song) => {
    // If an image URL repeats 3 or more times (compilation album art), fallback to YouTube thumbnail if videoId available
    if (song.thumbnailUrl && (imageCounts.get(song.thumbnailUrl) || 0) >= 3) {
      if (song.videoId && song.videoId.length === 11) {
        return {
          ...song,
          thumbnailUrl: `https://img.youtube.com/vi/${song.videoId}/hqdefault.jpg`
        }
      }
    }
    return song
  })
}

export class MusicApiClient {
  private baseUrl: string

  constructor(options?: MusicApiOptions) {
    this.baseUrl = options?.baseUrl || API_CONFIG.DEFAULT_BACKEND_URL
  }

  setBaseUrl(url: string) {
    this.baseUrl = url
  }

  /**
   * Search Tamil songs via NepoTune / JioSaavn direct audio API with fallback
   */
  async searchSongs(query: string, maxResults = 20): Promise<Song[]> {
    if (!query || !query.trim()) return []

    const cleanQuery = query.trim()
    const jioSaavnUrl = `${this.baseUrl}/api/search/songs?query=${encodeURIComponent(cleanQuery)}&limit=${maxResults}`

    try {
      const response = await fetch(jioSaavnUrl)
      if (response.ok) {
        const data = await response.json()
        const rawResults = data.data?.results || data.results || []
        if (Array.isArray(rawResults) && rawResults.length > 0) {
          return ensureDistinctThumbnails(deduplicateSongs(rawResults.map(mapToSong)))
        }
      }
    } catch (error) {
      console.warn('[MusicApiClient] Direct audio search failed, trying fallback...', error)
    }

    // Fallback to unified search endpoint if direct audio search is empty
    try {
      const fallbackUrl = `${this.baseUrl}${API_CONFIG.SEARCH_ENDPOINT}?q=${encodeURIComponent(cleanQuery)}&maxResults=${maxResults}`
      const response = await fetch(fallbackUrl)
      if (response.ok) {
        const data = await response.json()
        const raw = data.data?.results || data.items || data.results || []
        return ensureDistinctThumbnails(deduplicateSongs(raw.map(mapToSong)))
      }
    } catch (error) {
      console.error('[MusicApiClient] Fallback search also failed:', error)
    }

    return []
  }

  /**
   * Get rich feed of 50+ trending Tamil songs with direct 320kbps audio streams across multiple artists and genres
   */
  async getTrending(maxResults = 60): Promise<Song[]> {
    const trendingQueries = [
      'Tamil Latest Hits 2024 2025',
      'Anirudh Ravichander Tamil Hits',
      'A R Rahman Tamil Super Hits',
      'Yuvan Shankar Raja Tamil Hits',
      'Harris Jayaraj Tamil Hits',
      'G V Prakash Tamil Hits',
      'Santhosh Narayanan Tamil Hits'
    ]

    try {
      const resultsArray = await Promise.all(
        trendingQueries.map((q) => this.searchSongs(q, 15).catch(() => []))
      )
      const combined = resultsArray.flat()
      const deduplicated = deduplicateSongs(combined)
      const distinct = ensureDistinctThumbnails(deduplicated)

      if (distinct.length > 0) {
        return distinct.slice(0, maxResults)
      }
    } catch (err) {
      console.warn('[MusicApiClient] Fetching combined trending songs failed:', err)
    }

    return await this.searchSongs('Tamil Top Hits', maxResults)
  }

  /**
   * Fetch songs for a specific category
   */
  async getCategorySongs(query: string, maxResults = 30): Promise<Song[]> {
    return await this.searchSongs(query, maxResults)
  }
}

// Default singleton instance
export const musicApi = new MusicApiClient()
