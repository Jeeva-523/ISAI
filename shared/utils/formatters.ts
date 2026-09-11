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
 * Phonetic normalization for Tamil transliterated song titles
 */
function phoneticNormalize(text: string): string {
  if (!text) return ''
  return text
    .toLowerCase()
    .replace(/th/g, 't')
    .replace(/zh/g, 'l')
    .replace(/dh/g, 'd')
    .replace(/sh/g, 's')
    .replace(/ck/g, 'k')
    .replace(/ch/g, 'c')
    .replace(/aa/g, 'a')
    .replace(/ee/g, 'i')
    .replace(/oo/g, 'u')
    .replace(/ii/g, 'i')
    .replace(/uu/g, 'u')
    .replace(/[^a-z0-9]/g, '')
    .trim()
}

const NOISE_REGEX = /\b(official|video|lyric|lyrics|full|audio|hd|4k|8k|uhd|song|songs|trending|version|remix|bgm|theme|track|singles|single|teaser|trailer|promo|lyrical|visualizer|jukebox|compilation|all time hits|tamil|telugu|hindi|kannada|malayalam|dj|mix|prod|feat|ft|instrumental|reprise|extended|motion|poster|dialogue|scene|scenes|special|exclusive|original|soundtrack|ost|mashup)\b/gi
const STOPWORDS = new Set(['the', 'a', 'an', 'in', 'of', 'to', 'and', 'from', 'with', 'by', 'on', 'for', 'at', 'is', 'it'])

function cleanRawTitle(raw: string): string {
  if (!raw) return ''
  const unescaped = cleanHtmlTitle(raw)
  return unescaped
    .replace(/\([^)]*\)/g, ' ')
    .replace(/\[[^\]]*\]/g, ' ')
    .replace(/\bfrom\s+["'].*?["']/gi, ' ')
    .replace(/\bfrom\s+[A-Za-z0-9\s:]+/gi, ' ')
    .replace(NOISE_REGEX, ' ')
    .replace(/[^a-zA-Z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function extractTitleTokens(rawTitle: string): Set<string> {
  const cleaned = cleanRawTitle(rawTitle)
  const tokens = cleaned.split(/\s+/)
    .map(t => phoneticNormalize(t))
    .filter(t => t.length >= 2 && !STOPWORDS.has(t))
  return new Set(tokens)
}

export function normalizeTitleKey(rawTitle: string): string {
  const tokens = Array.from(extractTitleTokens(rawTitle))
  return tokens.sort().join('')
}

/**
 * Checks if two songs are the same song or repetitive title variations (covers, remixes, lyrics)
 */
export function isSameSongOrDuplicate(
  songA: { title?: string; videoId?: string },
  songB: { title?: string; videoId?: string }
): boolean {
  if (!songA || !songB) return false
  if (songA.videoId && songB.videoId && songA.videoId === songB.videoId) return true

  const keyA = normalizeTitleKey(songA.title || '')
  const keyB = normalizeTitleKey(songB.title || '')

  if (keyA && keyB && keyA === keyB) return true

  const tokensA = extractTitleTokens(songA.title || '')
  const tokensB = extractTitleTokens(songB.title || '')

  if (tokensA.size === 0 || tokensB.size === 0) return false

  const common: string[] = []
  for (const t of tokensA) {
    if (tokensB.has(t)) common.push(t)
  }

  const commonLen = common.reduce((acc, t) => acc + t.length, 0)
  const maxLen = Math.max(
    Array.from(tokensA).reduce((acc, t) => acc + t.length, 0),
    Array.from(tokensB).reduce((acc, t) => acc + t.length, 0)
  )

  // High similarity ratio (>= 80% character overlap): genuine duplicates (audio vs video / lyrics)
  if (maxLen > 0 && (commonLen / maxLen) >= 0.80) {
    return true
  }

  return false
}

/**
 * Deduplicate songs so each unique song appears only once
 */
export function deduplicateSongs(songs: Song[]): Song[] {
  if (!Array.isArray(songs) || songs.length === 0) return []
  const result: Song[] = []

  for (const song of songs) {
    if (!song || !song.title) continue
    const isDup = result.some(existing => isSameSongOrDuplicate(existing, song))
    if (!isDup) {
      result.push(song)
    }
  }

  return result
}
