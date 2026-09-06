import type { Song } from '../models/song'

/**
 * Clean raw HTML entities from titles (e.g. &amp;, &#39;, &quot;)
 */
export function cleanHtmlTitle(raw: string): string {
  if (!raw) return ''
  let cleaned = raw
  let prev = ''
  let pass = 0
  while (cleaned !== prev && pass < 5) {
    prev = cleaned
    pass++
    cleaned = cleaned
      .replace(/&quot;/g, '"')
      .replace(/&#0?39;/g, "'")
      .replace(/&apos;/g, "'")
      .replace(/&amp;/g, '&')
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&nbsp;/g, ' ')
      .replace(/&#(\d+);/g, (_, code) => String.fromCharCode(Number(code)))
      .replace(/&#x([0-9a-fA-F]+);/g, (_, code) => String.fromCharCode(parseInt(code, 16)))
  }
  return cleaned.trim()
}

/**
 * Format ISO 8601 duration (e.g., PT4M35S) to human-readable (e.g., 04:35)
 */
export function parseIsoDuration(iso: string): { formatted: string; durationMs: number } {
  if (!iso) return { formatted: '3:45', durationMs: 225000 }
  const match = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/)
  if (!match) return { formatted: '3:45', durationMs: 225000 }

  const hours = parseInt(match[1] || '0', 10)
  const minutes = parseInt(match[2] || '0', 10)
  const seconds = parseInt(match[3] || '0', 10)
  const totalSec = hours * 3600 + minutes * 60 + seconds
  const totalMs = totalSec * 1000

  let formatted: string
  if (hours > 0) {
    formatted = `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  } else {
    formatted = `${minutes}:${seconds.toString().padStart(2, '0')}`
  }

  return { formatted, durationMs: totalMs }
}

/**
 * Format view count to compact string (e.g., 12.5M views, 450K views)
 */
export function formatViewCount(rawCount?: string | number): string {
  if (!rawCount) return ''
  const num = typeof rawCount === 'number' ? rawCount : parseInt(rawCount, 10)
  if (isNaN(num)) return ''

  if (num >= 10_000_000) {
    return `${(num / 10_000_000).toFixed(1)}Cr views`
  }
  if (num >= 1_000_000) {
    return `${(num / 1_000_000).toFixed(1)}M views`
  }
  if (num >= 1_000) {
    return `${(num / 1_000).toFixed(1)}K views`
  }
  return `${num} views`
}

/**
 * Deduplicate songs by videoId and normalized title + artist
 */
export function deduplicateSongs(songs: Song[]): Song[] {
  if (!Array.isArray(songs) || songs.length === 0) return []
  const seenIds = new Set<string>()
  const seenKeys = new Set<string>()
  const result: Song[] = []

  for (const song of songs) {
    if (!song || !song.title) continue
    if (song.videoId && seenIds.has(song.videoId)) continue

    const normTitle = song.title
      .toLowerCase()
      .replace(/\(.*?\)|\[.*?\]/g, '')
      .replace(/official video|lyric video|full video|trending version|hd song|audio|lyric|video/gi, '')
      .replace(/[^a-z0-9]/g, '')
      .trim()

    const artistKey = song.channelTitle
      ? song.channelTitle.toLowerCase().replace(/[^a-z0-9]/g, '').slice(0, 12)
      : ''
    const normKey = `${normTitle}_${artistKey}`

    if (normTitle.length > 2 && seenKeys.has(normKey)) {
      continue
    }

    if (song.videoId) seenIds.add(song.videoId)
    if (normTitle.length > 2) seenKeys.add(normKey)
    result.push(song)
  }

  return result
}
