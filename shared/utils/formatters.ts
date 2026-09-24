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
      .replaceAll('&quot;', '"')
      .replaceAll(/&#0?39;/g, "'")
      .replaceAll('&apos;', "'")
      .replaceAll('&amp;', '&')
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&nbsp;', ' ')
      .replaceAll(/&#(\d+);/g, (_, code) => String.fromCharCode(Number(code)))
      .replaceAll(/&#x([0-9a-fA-F]+);/g, (_, code) => String.fromCharCode(Number.parseInt(code, 16)))
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

  const hours = Number.parseInt(match[1] || '0', 10)
  const minutes = Number.parseInt(match[2] || '0', 10)
  const seconds = Number.parseInt(match[3] || '0', 10)
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
  const num = typeof rawCount === 'number' ? rawCount : Number.parseInt(rawCount, 10)
  if (Number.isNaN(num)) return ''

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
    .replaceAll('th', 't')
    .replaceAll('zh', 'l')
    .replaceAll('dh', 'd')
    .replaceAll('sh', 's')
    .replaceAll('ck', 'k')
    .replaceAll('ch', 'c')
    .replaceAll('aa', 'a')
    .replaceAll('ee', 'i')
    .replaceAll('oo', 'u')
    .replaceAll('ii', 'i')
    .replaceAll('uu', 'u')
    .replaceAll(/[^a-z0-9]/g, '')
    .trim()
}

const NOISE_REGEX =
  /\b(?:official|video|lyric|lyrics|full|audio|hd|4k|8k|uhd|song|songs|trending|version|remix|bgm|theme|track|singles|single|teaser|trailer|promo|lyrical|visualizer|jukebox|compilation|all time hits|tamil|telugu|hindi|kannada|malayalam|dj|mix|prod|feat|ft|instrumental|reprise|extended|motion|poster|dialogue|scene|scenes|special|exclusive|original|soundtrack|ost|mashup)\b/gi

const STOPWORDS = new Set([
  'the',
  'a',
  'an',
  'in',
  'of',
  'to',
  'and',
  'from',
  'with',
  'by',
  'on',
  'for',
  'at',
  'is',
  'it'
])

function cleanRawTitle(raw: string): string {
  if (!raw) return ''
  const unescaped = cleanHtmlTitle(raw)
  return unescaped
    .replaceAll(/\([^)]*\)/g, ' ')
    .replaceAll(/\[[^\]]*\]/g, ' ')
    .replaceAll(/\bfrom\s+["'].*?["']/gi, ' ')
    .replaceAll(/\bfrom\s[a-z0-9\s:]+/gi, ' ')
    .replaceAll(NOISE_REGEX, ' ')
    .replaceAll(/[^a-z0-9\s]/gi, ' ')
    .replaceAll(/\s+/g, ' ')
    .trim()
}

export function extractTitleTokens(rawTitle: string): Set<string> {
  const cleaned = cleanRawTitle(rawTitle)
  const tokens = cleaned
    .split(/\s+/)
    .map((t) => phoneticNormalize(t))
    .filter((t) => t.length >= 2 && !STOPWORDS.has(t))
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
  songA: { title?: string; videoId?: string; language?: string },
  songB: { title?: string; videoId?: string; language?: string }
): boolean {
  if (!songA || !songB) return false
  if (songA.videoId && songB.videoId && songA.videoId === songB.videoId) return true

  // Language mismatch check
  const langA = (songA.language || '').toLowerCase().trim()
  const langB = (songB.language || '').toLowerCase().trim()
  if (langA && langB && langA !== langB) return false

  const tA = (songA.title || '').toLowerCase()
  const tB = (songB.title || '').toLowerCase()
  const hasTamilA = /\btamil\b/i.test(tA)
  const hasTamilB = /\btamil\b/i.test(tB)
  const hasTeluguA = /\btelugu\b/i.test(tA)
  const hasTeluguB = /\btelugu\b/i.test(tB)
  const hasHindiA = /\bhindi\b/i.test(tA)
  const hasHindiB = /\bhindi\b/i.test(tB)
  const hasMalayalamA = /\bmalayalam\b/i.test(tA)
  const hasMalayalamB = /\bmalayalam\b/i.test(tB)
  const hasKannadaA = /\bkannada\b/i.test(tA)
  const hasKannadaB = /\bkannada\b/i.test(tB)

  if (
    (hasTamilA && !hasTamilB && (hasTeluguB || hasHindiB || hasMalayalamB || hasKannadaB)) ||
    (hasTamilB && !hasTamilA && (hasTeluguA || hasHindiA || hasMalayalamA || hasKannadaA)) ||
    (hasTeluguA && !hasTeluguB && (hasTamilB || hasHindiB || hasMalayalamB || hasKannadaB)) ||
    (hasHindiA && !hasHindiB && (hasTamilB || hasTeluguB || hasMalayalamB || hasKannadaB))
  ) {
    return false
  }

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
  if (maxLen > 0 && commonLen / maxLen >= 0.8) {
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
    const isDup = result.some((existing) => isSameSongOrDuplicate(existing, song))
    if (!isDup) {
      result.push(song)
    }
  }

  return result
}
