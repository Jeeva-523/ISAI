import { cleanHtmlTitle, parseIsoDuration, formatViewCount } from '../../../shared/utils/formatters'
import type { Song } from '../../../shared/models/song'

export class YouTubeService {
  private apiKey: string
  private cache: Map<string, Song[]> = new Map()

  constructor() {
    this.apiKey = process.env.YOUTUBE_API_KEY || 'AIzaSyD07Kne6rOBJFBBZSJFq1XlsukB7e6g8K0'
  }

  /**
   * Search Tamil songs using YouTube Music Innertube API (IcySnex/YouTubeMusicAPI architecture)
   * With seamless fallback to YouTube Data API v3
   */
  async searchTamilSongs(query: string, maxResults = 20): Promise<Song[]> {
    const cacheKey = query.trim().toLowerCase()
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey)!
    }

    // 1. Primary: YouTube Music Innertube WEB_REMIX API (No quota limit, official music tracks)
    try {
      const ytmSongs = await this.searchYouTubeMusicInnertube(query, maxResults)
      if (ytmSongs && ytmSongs.length > 0) {
        this.cache.set(cacheKey, ytmSongs)
        return ytmSongs
      }
    } catch (err) {
      console.warn('[YouTubeService] YouTube Music Innertube API failed, falling back to YouTube Data API v3:', err)
    }

    // 2. Fallback: YouTube Data API v3
    return this.searchYouTubeDataApi(query, maxResults)
  }

  /**
   * Official YouTube Music Innertube WEB_REMIX API implementation
   * (Adapted from https://github.com/IcySnex/YouTubeMusicAPI)
   */
  private async searchYouTubeMusicInnertube(query: string, maxResults = 20): Promise<Song[]> {
    const effectiveQuery = query.toLowerCase().includes('tamil') ? query.trim() : `${query.trim()} Tamil song`

    const response = await fetch('https://music.youtube.com/youtubei/v1/search', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36',
        'Origin': 'https://music.youtube.com',
        'Referer': 'https://music.youtube.com/'
      },
      body: JSON.stringify({
        context: {
          client: {
            clientName: 'WEB_REMIX',
            clientVersion: '1.20260715.04.00',
            browserName: 'Chrome',
            osName: 'Windows',
            hl: 'en',
            gl: 'IN'
          }
        },
        query: effectiveQuery
      })
    })

    if (!response.ok) {
      throw new Error(`YouTube Music API HTTP ${response.status}: ${response.statusText}`)
    }

    const data = await response.json()
    const sectionList = data.contents?.tabbedSearchResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || []
    const songs: Song[] = []

    for (const section of sectionList) {
      const contents = section.itemSectionRenderer?.contents || section.musicShelfRenderer?.contents || []
      for (const item of contents) {
        const m = item.musicResponsiveListItemRenderer
        if (!m) continue

        const title = m.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text
        const flex1Runs = m.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || []

        let artist = ''
        let durationStr = '3:45'
        const textParts = flex1Runs.map((r: any) => r.text).filter((t: string) => t && t !== ' • ')
        if (textParts.length > 1) {
          artist = textParts[1]
          const lastPart = textParts[textParts.length - 1]
          if (/^\d+:\d+$/.test(lastPart) || /^\d+:\d+:\d+$/.test(lastPart)) {
            durationStr = lastPart
          }
        } else if (textParts.length === 1) {
          artist = textParts[0]
        }

        const videoId = m.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
          || m.navigationEndpoint?.watchEndpoint?.videoId
          || m.onTap?.watchEndpoint?.videoId

        const thumb = (videoId ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : '')
          || m.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.slice(-1)[0]?.url
          || ''

        if (title && videoId) {
          songs.push({
            videoId,
            title: cleanHtmlTitle(title),
            channelTitle: cleanHtmlTitle(artist || 'Tamil Music'),
            thumbnailUrl: thumb,
            durationFormatted: durationStr,
            durationMs: 225000,
            viewCountFormatted: 'YouTube Music'
          })
        }

        if (songs.length >= maxResults) break
      }
      if (songs.length >= maxResults) break
    }

    return songs
  }

  /**
   * Fallback using YouTube Data API v3
   */
  private async searchYouTubeDataApi(query: string, maxResults = 20): Promise<Song[]> {
    const effectiveQuery = query.toLowerCase().includes('tamil') ? query.trim() : `${query.trim()} Tamil song`
    const searchUrl = `https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoCategoryId=10&maxResults=${maxResults}&q=${encodeURIComponent(effectiveQuery)}&key=${this.apiKey}`

    const res = await fetch(searchUrl)
    if (!res.ok) {
      throw new Error(`YouTube API returned ${res.status}: ${res.statusText}`)
    }
    const data = await res.json()
    const items = (data.items || []) as any[]

    const videoIds = items.map(item => item.id?.videoId).filter(Boolean)

    let detailsMap: Record<string, any> = {}
    if (videoIds.length > 0) {
      try {
        const detailsUrl = `https://www.googleapis.com/youtube/v3/videos?part=contentDetails,statistics&id=${videoIds.join(',')}&key=${this.apiKey}`
        const detailsRes = await fetch(detailsUrl)
        if (detailsRes.ok) {
          const detailsData = await detailsRes.json()
          for (const item of (detailsData.items || [])) {
            detailsMap[item.id] = item
          }
        }
      } catch {
        // ignore details fetch errors
      }
    }

    return items.map(item => {
      const videoId = item.id?.videoId || ''
      const snippet = item.snippet || {}
      const details = detailsMap[videoId]

      const title = cleanHtmlTitle(snippet.title || '')
      const channelTitle = cleanHtmlTitle(snippet.channelTitle || '')
      const thumbnailUrl = snippet.thumbnails?.high?.url
        || snippet.thumbnails?.medium?.url
        || snippet.thumbnails?.default?.url
        || `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`

      const { formatted, durationMs } = parseIsoDuration(details?.contentDetails?.duration || '')
      const viewCountFormatted = formatViewCount(details?.statistics?.viewCount)

      return {
        videoId,
        title,
        channelTitle,
        thumbnailUrl,
        durationFormatted: formatted,
        durationMs,
        viewCountFormatted
      }
    }).filter(s => !!s.videoId)
  }

  async getTrending(maxResults = 20): Promise<Song[]> {
    return this.searchTamilSongs('Trending Tamil songs 2024', maxResults)
  }
}

export const youtubeService = new YouTubeService()
