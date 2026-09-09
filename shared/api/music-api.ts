import { API_CONFIG } from '../constants/api'
import type { Song } from '../models/song'
import type { MusicProvider } from './MusicProvider'
import { cleanHtmlTitle, deduplicateSongs } from '../utils/formatters'
import { decryptMediaUrl } from '../utils/crypto'

export interface MusicApiOptions {
  baseUrl?: string
}

function parseReleaseTimestamp(dateStr?: string, yearStr?: string): number {
  if (dateStr) {
    const parsed = Date.parse(dateStr)
    if (!isNaN(parsed) && parsed > 0) return parsed
  }
  if (yearStr) {
    const yr = parseInt(yearStr, 10)
    if (!isNaN(yr) && yr >= 1970 && yr <= 2030) {
      return new Date(yr, 0, 1).getTime()
    }
  }
  return Date.now() - 45 * 86400000 // Default to ~45 days ago
}

export function detectSongLanguage(song: Partial<Song>): string {
  if (song.language && song.language.trim()) {
    return song.language.trim().toLowerCase()
  }

  const combined = `${song.title || ''} ${song.channelTitle || ''} ${song.album || ''}`.toLowerCase()

  // 1. Unicode script detection
  if (/[\u0B80-\u0BFF]/.test(combined)) return 'tamil'
  if (/[\u0C00-\u0C7F]/.test(combined)) return 'telugu'
  if (/[\u0D00-\u0D7F]/.test(combined)) return 'malayalam'
  if (/[\u0C80-\u0CFF]/.test(combined)) return 'kannada'
  if (/[\u0900-\u097F]/.test(combined)) return 'hindi'
  if (/[\u0A00-\u0A7F]/.test(combined)) return 'punjabi'

  // 2. Language keyword hints
  if (combined.includes('tamil')) return 'tamil'
  if (combined.includes('telugu')) return 'telugu'
  if (combined.includes('malayalam')) return 'malayalam'
  if (combined.includes('kannada')) return 'kannada'
  if (combined.includes('hindi') || combined.includes('bollywood')) return 'hindi'
  if (combined.includes('punjabi')) return 'punjabi'
  if (combined.includes('english')) return 'english'

  // 3. Prominent Artist heuristic
  if (/(anirudh|a\.?r\.?\s?rahman|yuvan|harris jayaraj|vidyasagar|deva|ilayaraja|santhosh narayanan|g\.?v\.?\s?prakash|dhibu|sai abhyankkar|pradeep kumar|dhanush|vijay|sivaangi|jonita|sid sriram)/i.test(combined)) {
    return 'tamil'
  }
  if (/(thaman|devi sri prasad|dsp|keeravani|mahesh babu|allu arjun|ram charan|chiranjeevi)/i.test(combined)) return 'telugu'
  if (/(sushin shyam|dabzee|jassie gift|shaan rahman|gopi sundar|heshem|mohanlal|mammootty)/i.test(combined)) return 'malayalam'
  if (/(arijit singh|pritam|badshah|shreya ghoshal|amitabh bhattacharya|armaan malik|neha kakkar|kumar sanu|kishore|atif aslam)/i.test(combined)) return 'hindi'
  if (/(diljit dosanjh|sidhu moose|ap dhillon|karan aujla|honey singh)/i.test(combined)) return 'punjabi'
  if (/(ed sheeran|taylor swift|billie eilish|the weeknd|dua lipa|coldplay|eminem|drake|post malone|bruno mars|justin bieber)/i.test(combined)) return 'english'

  return 'tamil'
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

    const primaryArtists = item.more_info.artistMap?.primary_artists?.map((a: any) => a.name) || []
    const artistsList = primaryArtists.length > 0
      ? primaryArtists
      : (item.subtitle ? item.subtitle.split(',').map((s: string) => s.trim()) : [])

    const releaseDate = item.more_info.release_date || item.year || ''
    const releaseTimestamp = parseReleaseTimestamp(releaseDate, item.year)
    const playCountNum = Number(item.play_count) || 0

    const rawLang = item.language || item.more_info?.language || ''

    const mapped: Song = {
      videoId: item.id || `saavn_${Date.now()}`,
      title: cleanHtmlTitle(item.title || item.name || ''),
      channelTitle: cleanHtmlTitle(item.subtitle || artistsList.join(', ') || 'Tamil Artist'),
      thumbnailUrl: thumbnailUrl || `https://img.youtube.com/vi/${item.id}/hqdefault.jpg`,
      durationFormatted,
      durationMs: durSec > 0 ? durSec * 1000 : 210000,
      viewCountFormatted: playCountNum > 0 ? `${playCountNum.toLocaleString()} plays` : '',
      album: cleanHtmlTitle(item.more_info.album || ''),
      audioUrl: decryptedAudio,
      language: rawLang.toLowerCase() || undefined,
      releaseDate,
      releaseTimestamp,
      genre: item.more_info.genre || undefined,
      artists: artistsList,
      playCountNumber: playCountNum
    }
    mapped.language = detectSongLanguage(mapped)
    return mapped
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

    const artistsList = item.artists?.primary?.map((a: any) => a.name) ||
      item.artists?.all?.map((a: any) => a.name) ||
      (item.subtitle ? item.subtitle.split(',').map((s: string) => s.trim()) : [])

    const artistNames = artistsList.join(', ') || item.subtitle || 'Tamil Artist'

    const durSec = Number(item.duration) || 0
    const mins = Math.floor(durSec / 60)
    const secs = durSec % 60
    const durationFormatted = durSec > 0 ? `${mins}:${secs < 10 ? '0' : ''}${secs}` : '3:30'

    const releaseDate = item.releaseDate || item.year || ''
    const releaseTimestamp = parseReleaseTimestamp(releaseDate, item.year)
    const playCountNum = Number(item.playCount) || 0
    const rawLang = item.language || ''

    const mapped: Song = {
      videoId: item.id || `saavn_${Date.now()}`,
      title: cleanHtmlTitle(item.name || item.title || ''),
      channelTitle: cleanHtmlTitle(artistNames),
      thumbnailUrl,
      durationFormatted,
      durationMs: durSec > 0 ? durSec * 1000 : 210000,
      viewCountFormatted: playCountNum > 0 ? `${playCountNum.toLocaleString()} plays` : '',
      album: cleanHtmlTitle(item.album?.name || ''),
      audioUrl,
      language: rawLang.toLowerCase() || undefined,
      releaseDate,
      releaseTimestamp,
      genre: item.genre || undefined,
      artists: artistsList,
      playCountNumber: playCountNum
    }
    mapped.language = detectSongLanguage(mapped)
    return mapped
  }

  // 3. Fallback format:
  const videoId = item.videoId || item.id || ''
  const ytThumb = videoId && videoId.length === 11 ? `https://img.youtube.com/vi/${videoId}/hqdefault.jpg` : ''
  const rawThumb = item.thumbnailUrl || (typeof item.image === 'string' ? item.image : '') || ''

  const fallbackSong: Song = {
    videoId,
    title: cleanHtmlTitle(item.title || item.name || ''),
    channelTitle: cleanHtmlTitle(item.channelTitle || item.artists || ''),
    thumbnailUrl: rawThumb || ytThumb,
    durationFormatted: item.durationFormatted || '3:30',
    durationMs: item.durationMs || 210000,
    viewCountFormatted: item.viewCountFormatted || '',
    album: cleanHtmlTitle(item.album || ''),
    audioUrl: item.audioUrl || '',
    language: item.language || undefined,
    releaseDate: item.releaseDate || undefined,
    releaseTimestamp: parseReleaseTimestamp(item.releaseDate, item.year),
    artists: item.artists ? (Array.isArray(item.artists) ? item.artists : [item.artists]) : undefined,
    playCountNumber: item.playCountNumber || 0
  }
  fallbackSong.language = detectSongLanguage(fallbackSong)
  return fallbackSong
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

export class MusicApiClient implements MusicProvider {
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
   * Calculate search relevance score based on token overlap in title, artist, and album
   */
  private calculateSearchRelevance(song: Song, query: string): number {
    const stopWords = new Set(['song', 'songs', 'all', 'the', 'a', 'an', 'hits', 'track', 'music', 'mp3', 'new', 'latest', 'best', 'padal', 'paadalgal'])
    const tokens = query.toLowerCase().split(/[^a-zA-Z0-9]+/).filter(t => t.length > 1 && !stopWords.has(t))
    if (tokens.length === 0) return 1

    const title = (song.title || '').toLowerCase()
    const artist = (song.channelTitle || '').toLowerCase()
    const album = (song.album || '').toLowerCase()
    const fullText = `${title} ${artist} ${album}`

    let score = 0
    for (const token of tokens) {
      if (title.includes(token)) score += 3
      else if (artist.includes(token)) score += 2
      else if (album.includes(token)) score += 1

      // Gaana / Gana synonym matching
      if (token === 'gana' && fullText.includes('gaana')) score += 3
      if (token === 'gaana' && fullText.includes('gana')) score += 3
    }
    return score
  }

  /**
   * Search songs with intelligent intent routing, genre decomposition, and relevance ranking
   */
  async searchSongs(query: string, maxResults = 25): Promise<Song[]> {
    if (!query || !query.trim()) return []

    const cleanQuery = query.trim()
    const lower = cleanQuery.toLowerCase()

    // 1. Check for Tamil Gaana / Folk intent
    if (lower.includes('gana') || lower.includes('gaana')) {
      const ganaQueries = [
        'Gana Bala',
        'Marana Gana Viji',
        'Gana Balachandar',
        'Vaathi Coming',
        'Anthony Daasan',
        'Aathangara Orathil',
        'Danga Maari Oodhari',
        'Aaluma Doluma',
        'Open the Tasmac',
        'Ora Kannala'
      ]

      const fetchSubQuery = async (q: string): Promise<Song[]> => {
        try {
          const res = await fetch(`https://saavn-api-seven.vercel.app/api/search/songs?query=${encodeURIComponent(q)}&limit=6`, { signal: AbortSignal.timeout(4000) })
          if (res.ok) {
            const data = await res.json()
            const raw = data.data?.results || data.results || []
            if (Array.isArray(raw)) {
              return raw.map(mapToSong).filter(s => Boolean(s.audioUrl))
            }
          }
        } catch {}
        return []
      }

      const results = await Promise.all(ganaQueries.map(fetchSubQuery))
      const combined = deduplicateSongs(results.flat())
      if (combined.length > 0) {
        combined.sort((a, b) => this.calculateSearchRelevance(b, query) - this.calculateSearchRelevance(a, query))
        return ensureDistinctThumbnails(combined).slice(0, maxResults)
      }
    }

    // 2. Check for Tamil Melody intent
    if (lower.includes('melody') || lower.includes('melodies')) {
      const melodyQueries = [
        'Tamil melody hit songs',
        'Harris Jayaraj melody hits',
        'A R Rahman Tamil melodies',
        'Sid Sriram Tamil melody',
        'Yuvan Shankar Raja melody hits'
      ]
      const fetchSubQuery = async (q: string): Promise<Song[]> => {
        try {
          const res = await fetch(`https://saavn-api-seven.vercel.app/api/search/songs?query=${encodeURIComponent(q)}&limit=8`, { signal: AbortSignal.timeout(4000) })
          if (res.ok) {
            const data = await res.json()
            const raw = data.data?.results || data.results || []
            if (Array.isArray(raw)) {
              return raw.map(mapToSong).filter(s => Boolean(s.audioUrl))
            }
          }
        } catch {}
        return []
      }
      const results = await Promise.all(melodyQueries.map(fetchSubQuery))
      const combined = deduplicateSongs(results.flat())
      if (combined.length > 0) {
        return ensureDistinctThumbnails(combined).slice(0, maxResults)
      }
    }

    // 3. Check for Kuthu / Mass intent
    if (lower.includes('kuthu') || lower.includes('mass dance')) {
      const kuthuQueries = [
        'Arabic Kuthu - Halamithi Habibo',
        'Aaluma Doluma',
        'Vaathi Coming',
        'Naa Ready',
        'Danga Maari Oodhari',
        'Jithu Jilladi',
        'Kuthu Vilakku'
      ]
      const fetchSubQuery = async (q: string): Promise<Song[]> => {
        try {
          const res = await fetch(`https://saavn-api-seven.vercel.app/api/search/songs?query=${encodeURIComponent(q)}&limit=6`, { signal: AbortSignal.timeout(4000) })
          if (res.ok) {
            const data = await res.json()
            const raw = data.data?.results || data.results || []
            if (Array.isArray(raw)) {
              return raw.map(mapToSong).filter(s => Boolean(s.audioUrl))
            }
          }
        } catch {}
        return []
      }
      const results = await Promise.all(kuthuQueries.map(fetchSubQuery))
      const combined = deduplicateSongs(results.flat())
      if (combined.length > 0) {
        return ensureDistinctThumbnails(combined).slice(0, maxResults)
      }
    }

    // 4. Primary: High-speed CORS-enabled JioSaavn API
    try {
      const mirrorUrl = `https://saavn-api-seven.vercel.app/api/search/songs?query=${encodeURIComponent(cleanQuery)}&limit=${maxResults}`
      const response = await fetch(mirrorUrl, { signal: AbortSignal.timeout(4500) })
      if (response.ok) {
        const data = await response.json()
        const raw = data.data?.results || data.results || []
        if (Array.isArray(raw) && raw.length > 0) {
          const mapped = raw.map(mapToSong).filter(s => Boolean(s.audioUrl))
          if (mapped.length > 0) {
            const scored = mapped.map(s => ({ song: s, score: this.calculateSearchRelevance(s, cleanQuery) }))
            const hasMatches = scored.some(item => item.score > 0)
            if (hasMatches) {
              scored.sort((a, b) => b.score - a.score)
              return ensureDistinctThumbnails(deduplicateSongs(scored.map(i => i.song)))
            }
            return ensureDistinctThumbnails(deduplicateSongs(mapped))
          }
        }
      }
    } catch (error) {
      console.warn('[MusicApiClient] Primary CORS mirror search failed, trying official/fallback...', error)
    }

    // 5. Fallback: Official JioSaavn API
    try {
      const jioSaavnOfficialUrl = `https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&api_version=4&ctx=web6dot0&q=${encodeURIComponent(cleanQuery)}&n=${maxResults}`
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
      console.warn('[MusicApiClient] JioSaavn official API search failed:', error)
    }

    return []
  }

  // MusicProvider interface alias
  async searchTracks(query: string, limit = 20): Promise<Song[]> {
    return this.searchSongs(query, limit)
  }

  /**
   * Get rich feed of songs tailored to the user's preferred languages
   */
  async getTrending(preferredLanguages?: string[], maxResults = 80): Promise<Song[]> {
    const languageQueryMap: Record<string, string[]> = {
      tamil: [
        'Latest Tamil Hits',
        'Tamil Top Hits',
        'Sai Abhyankkar Hits',
        'Amaran Tamil Songs',
        'The Greatest Of All Time Tamil Songs',
        'Anirudh Ravichander Hits'
      ],
      telugu: [
        'Latest Telugu Hits 2025',
        'Telugu Super Hits',
        'Thaman S Telugu Hits',
        'Devi Sri Prasad Telugu Hits'
      ],
      hindi: [
        'Latest Bollywood Hindi Hits 2025',
        'Arijit Singh Super Hits',
        'Pritam Bollywood Hits',
        'Top Hindi Songs 2025'
      ],
      malayalam: [
        'Latest Malayalam Hits 2025',
        'Sushin Shyam Malayalam Hits',
        'Malayalam Melody Hits'
      ],
      english: [
        'Top Global Pop Hits 2025',
        'Billboard Hot 100 Hits',
        'Ed Sheeran Popular Songs'
      ],
      kannada: [
        'Latest Kannada Hits 2025',
        'Kannada Super Hits',
        'Ravi Basrur Hits'
      ],
      punjabi: [
        'Latest Punjabi Hits 2025',
        'Top Punjabi Songs',
        'Diljit Dosanjh Hits'
      ]
    }

    let trendingQueries: string[] = []

    if (preferredLanguages && preferredLanguages.length > 0) {
      for (const lang of preferredLanguages) {
        const cleanLang = lang.trim().toLowerCase()
        const queries = languageQueryMap[cleanLang]
        if (queries) {
          trendingQueries.push(...queries)
        } else {
          trendingQueries.push(`Latest ${lang} Hits`)
        }
      }
    }

    if (trendingQueries.length === 0) {
      trendingQueries = [
        'Latest Tamil Hits',
        'Tamil Top Hits',
        'Sai Abhyankkar Hits',
        'Amaran Tamil Songs',
        'Anirudh Ravichander Hits'
      ]
    }

    try {
      const resultsArray = await Promise.all(
        trendingQueries.map((q) => this.searchSongs(q, 15).catch(() => []))
      )
      const combined = resultsArray.flat()
      const deduplicated = deduplicateSongs(combined)
      const distinct = ensureDistinctThumbnails(deduplicated)
      const filtered = distinct.filter((song) => {
        const title = song.title.toLowerCase()
        if (title.includes('trending') || title.includes('jukebox') || title.includes('full album') || title.includes('non stop') || title.includes('compilation') || title.includes('remaster')) {
          return false
        }
        // Discard obscure self-published tracks with low play counts
        if (song.playCountNumber !== undefined && song.playCountNumber < 10000) {
          return false
        }
        return true
      })

      // Sort by plays so top chartbusters appear first
      filtered.sort((a, b) => (b.playCountNumber || 0) - (a.playCountNumber || 0))

      if (filtered.length > 0) {
        return filtered.slice(0, maxResults)
      }
    } catch (err) {
      console.warn('[MusicApiClient] Fetching combined trending songs failed:', err)
    }

    const fallback = await this.searchSongs('Latest Tamil Hits', maxResults)
    return fallback.filter((song) => {
      const title = song.title.toLowerCase()
      return !title.includes('trending') && !title.includes('jukebox') && !title.includes('remaster')
    })
  }

  // MusicProvider interface alias
  async getTrendingTracks(languages: string[], limit = 80): Promise<Song[]> {
    return this.getTrending(languages, limit)
  }

  /**
   * Fetch a single track by its ID
   */
  async getTrack(id: string): Promise<Song | null> {
    const results = await this.searchSongs(id, 5)
    return results.find(s => s.videoId === id) || results[0] || null
  }

  /**
   * Dynamic New Releases strictly within the last N days (default 30 days)
   * Formula: releaseTimestamp >= (Date.now() - days * 86,400,000)
   */
  async getNewReleases(languages: string[] = ['tamil'], days = 30, limit = 30): Promise<Song[]> {
    const cutoffTimestamp = Date.now() - days * 86400000
    const newReleaseQueryMap: Record<string, string[]> = {
      tamil: [
        'Latest Tamil Hits',
        'Think Indie Tamil',
        'Sai Abhyankkar Hits',
        'Amaran Tamil Songs',
        'The Greatest Of All Time Tamil Songs',
        'Tamil Movie Songs'
      ],
      telugu: ['Latest Telugu Hits 2025', 'Telugu Super Hits'],
      hindi: ['Latest Bollywood Hindi Hits 2025', 'Arijit Singh Super Hits'],
      malayalam: ['Latest Malayalam Hits 2025', 'Sushin Shyam Malayalam Hits'],
      english: ['Top Global Pop Hits 2025', 'Billboard Hot 100 Hits'],
      kannada: ['Latest Kannada Hits 2025'],
      punjabi: ['Latest Punjabi Hits 2025']
    }
    const queries = languages.flatMap(lang => newReleaseQueryMap[lang.toLowerCase()] || [`Latest ${lang} Hits`])

    try {
      const results = await Promise.all(queries.map(q => this.searchSongs(q, 20).catch(() => [])))
      const all = deduplicateSongs(results.flat())

      // Filter by language, quality, and recency
      const recent = all.filter(song => {
        const detectedLang = detectSongLanguage(song)
        const matchesLang = languages.some(l => l.toLowerCase() === detectedLang)
        if (!matchesLang) return false

        const title = song.title.toLowerCase()
        if (title.includes('remaster') || title.includes('jukebox') || title.includes('full album') || title.includes('non stop') || title.includes('tribute')) {
          return false
        }
        // Exclude obscure songs with low plays (< 10000)
        if (song.playCountNumber !== undefined && song.playCountNumber < 10000) {
          return false
        }

        const isRecentYear = song.releaseDate?.includes('2026') || song.releaseDate?.includes('2025') ||
          (song.releaseTimestamp && song.releaseTimestamp >= cutoffTimestamp)
        return Boolean(isRecentYear)
      })

      // Sort by play count descending
      recent.sort((a, b) => (b.playCountNumber || 0) - (a.playCountNumber || 0))

      if (recent.length >= 5) {
        return recent.slice(0, limit)
      }

      // Fallback: return top authentic tracks
      const fallback = all.filter(song => {
        const title = song.title.toLowerCase()
        return !title.includes('remaster') && !title.includes('jukebox') && (song.playCountNumber || 0) >= 10000
      }).sort((a, b) => (b.playCountNumber || 0) - (a.playCountNumber || 0))

      return fallback.slice(0, limit)
    } catch (e) {
      console.warn('[MusicApiClient] getNewReleases failed:', e)
      return []
    }
  }

  /**
   * Fetch top tracks by an artist
   */
  async getArtistTracks(artistName: string, language?: string, limit = 20): Promise<Song[]> {
    const query = language ? `${artistName} ${language} hits` : `${artistName} hit songs`
    return this.searchSongs(query, limit)
  }

  /**
   * Get related artists
   */
  async getRelatedArtists(artistName: string, _language?: string): Promise<string[]> {
    const knownCollaborators: Record<string, string[]> = {
      'anirudh ravichander': ['A.R. Rahman', 'Yuvan Shankar Raja', 'Santhosh Narayanan', 'Sai Abhyankkar'],
      'a.r. rahman': ['Harris Jayaraj', 'Anirudh Ravichander', 'Yuvan Shankar Raja', 'Vidyasagar'],
      'yuvan shankar raja': ['Anirudh Ravichander', 'Harris Jayaraj', 'Santhosh Narayanan', 'Ilaiyaraaja'],
      'harris jayaraj': ['A.R. Rahman', 'Yuvan Shankar Raja', 'Anirudh Ravichander', 'G.V. Prakash Kumar'],
      'thaman s': ['Devi Sri Prasad', 'Anirudh Ravichander', 'M.M. Keeravani'],
      'devi sri prasad': ['Thaman S', 'M.M. Keeravani', 'Anirudh Ravichander'],
      'arijit singh': ['Pritam', 'Atif Aslam', 'Armaan Malik', 'Shreya Ghoshal'],
      'pritam': ['Arijit Singh', 'Amitabh Bhattacharya', 'Sachin-Jigar', 'Vishal-Shekhar'],
      'sushin shyam': ['Dabzee', 'Shaan Rahman', 'Gopi Sundar', 'Jassie Gift']
    }

    const key = artistName.trim().toLowerCase()
    return knownCollaborators[key] || ['Anirudh Ravichander', 'A.R. Rahman', 'Yuvan Shankar Raja']
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
