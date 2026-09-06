import { API_CONFIG } from '../constants/api'
import type { Song } from '../models/song'
import { cleanHtmlTitle, deduplicateSongs } from '../utils/formatters'
import { decryptMediaUrl } from '../utils/crypto'

export interface MusicApiOptions {
  baseUrl?: string
}

function mapToSong(item: any): Song {
  // 1. Official JioSaavn API format (with encrypted_media_url)
  if (item.more_info && (item.more_info.encrypted_media_url || item.more_info.media_url)) {
    const encUrl = item.more_info.encrypted_media_url
    const decryptedAudio = decryptMediaUrl(encUrl) || item.more_info.media_url || ''
    const rawImg = item.image || ''
    const thumbnailUrl = typeof rawImg === 'string'
      ? rawImg.replace('-150x150', '-500x500').replace('-50x50', '-500x500').replace('images.saavncdn.com', 'c.saavncdn.com')
      : ''
    const durSec = Number(item.more_info.duration) || 0
    const mins = Math.floor(durSec / 60)
    const secs = durSec % 60
    const durationFormatted = durSec > 0 ? `${mins}:${secs < 10 ? '0' : ''}${secs}` : '3:30'

    return {
      videoId: item.id || `saavn_${Date.now()}`,
      title: cleanHtmlTitle(item.title || item.name || ''),
      channelTitle: cleanHtmlTitle(item.subtitle || item.more_info.artistMap?.primary_artists?.map((a: any) => a.name).join(', ') || 'Tamil Artist'),
      thumbnailUrl: thumbnailUrl || `https://img.youtube.com/vi/${item.id}/hqdefault.jpg`,
      durationFormatted,
      durationMs: durSec > 0 ? durSec * 1000 : 210000,
      viewCountFormatted: item.play_count ? `${Number(item.play_count).toLocaleString()} plays` : '',
      album: cleanHtmlTitle(item.more_info.album || ''),
      audioUrl: decryptedAudio
    }
  }

  // 2. JioSaavn / NepoTune API format (with downloadUrl array):
  if (item.name && (item.downloadUrl || item.audioUrl)) {
    const downloadUrls = Array.isArray(item.downloadUrl) ? item.downloadUrl : []
    const audio320 = downloadUrls.find((d: any) => d.quality === '320kbps')?.url
    const audio160 = downloadUrls.find((d: any) => d.quality === '160kbps')?.url
    const audioUrl = audio320 || audio160 || downloadUrls.at(-1)?.url || item.audioUrl || ''

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
      videoId: item.id || `saavn_${Date.now()}`,
      title: cleanHtmlTitle(item.name || item.title || ''),
      channelTitle: cleanHtmlTitle(artistNames),
      thumbnailUrl,
      durationFormatted,
      durationMs: durSec > 0 ? durSec * 1000 : 210000,
      viewCountFormatted: item.playCount ? `${Number(item.playCount).toLocaleString()} plays` : '',
      album: cleanHtmlTitle(item.album?.name || ''),
      audioUrl
    }
  }

  // 3. Fallback format:
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

  getBaseUrl(): string {
    return this.baseUrl
  }

  /**
   * Search Tamil songs via official JioSaavn & NepoTune 320kbps audio API
   */
  async searchSongs(query: string, maxResults = 20): Promise<Song[]> {
    if (!query || !query.trim()) return []

    const cleanQuery = query.trim()
    const queryWithTamil = !cleanQuery.toLowerCase().includes('tamil')
      ? `${cleanQuery} Tamil`
      : cleanQuery

    const jioSaavnOfficialUrl = `https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&api_version=4&ctx=web6dot0&q=${encodeURIComponent(queryWithTamil)}&n=${maxResults}`

    try {
      const response = await fetch(jioSaavnOfficialUrl, { signal: AbortSignal.timeout(3500) })
      if (response.ok) {
        const data = await response.json()
        const rawResults = data.results || data.data?.results || []
        if (Array.isArray(rawResults) && rawResults.length > 0) {
          const mapped = rawResults.map(mapToSong).filter(s => Boolean(s.audioUrl))
          if (mapped.length > 0) {
            return ensureDistinctThumbnails(deduplicateSongs(mapped))
          }
        }
      }
    } catch (error) {
      console.warn('[MusicApiClient] JioSaavn official API search failed, trying mirror...', error)
    }

    // Mirror API fallback
    try {
      const mirrorUrl = `https://saavn.dev/api/search/songs?query=${encodeURIComponent(queryWithTamil)}&limit=${maxResults}`
      const response = await fetch(mirrorUrl, { signal: AbortSignal.timeout(3500) })
      if (response.ok) {
        const data = await response.json()
        const raw = data.data?.results || data.results || []
        const mapped = raw.map(mapToSong)
        return ensureDistinctThumbnails(deduplicateSongs(mapped))
      }
    } catch (error) {
      console.error('[MusicApiClient] Mirror search also failed:', error)
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
      const filtered = distinct.filter((song) => {
        const title = song.title.toLowerCase()
        return !title.includes('trending') && !title.includes('jukebox') && !title.includes('full album') && !title.includes('non stop')
      })

      if (filtered.length > 0) {
        return filtered.slice(0, maxResults)
      }
    } catch (err) {
      console.warn('[MusicApiClient] Fetching combined trending songs failed:', err)
    }

    const fallback = await this.searchSongs('Latest Tamil Movie Songs 2025 2026', maxResults)
    return fallback.filter((song) => {
      const title = song.title.toLowerCase()
      return !title.includes('trending') && !title.includes('jukebox')
    })
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
