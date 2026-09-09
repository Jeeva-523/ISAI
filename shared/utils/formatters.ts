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

/**
 * Deduplicate songs by videoId, primary title key, and cleaned full title key
 */
export function deduplicateSongs(songs: Song[]): Song[] {
  if (!Array.isArray(songs) || songs.length === 0) return []
  const seenIds = new Set<string>()
  const seenPrimaryKeys = new Set<string>()
  const seenFullKeys = new Set<string>()
  const result: Song[] = []

  const noiseRegex = /\b(official|video|lyric|lyrics|full|audio|hd|4k|song|songs|trending|version|remix|bgm|theme|track|singles|single|teaser|trailer|lyrical|visualizer|jukebox|compilation|all time hits|tamil|telugu|hindi|dj|mix|prod|feat|ft|instrumental|reprise)\b/gi

  for (const song of songs) {
    if (!song || !song.title) continue

    const vid = song.videoId ? song.videoId.trim() : ''
    if (vid && seenIds.has(vid)) continue

    const rawTitle = cleanHtmlTitle(song.title)

    // 1. Strip complete bracketed content first, e.g. (From "Movie - Name") or [Tamil]
    let titleWithoutBrackets = rawTitle.replace(/\([^)]*\)/g, ' ').replace(/\[[^\]]*\]/g, ' ')

    // 2. Strip "From ..." clauses even if without brackets
    titleWithoutBrackets = titleWithoutBrackets.replace(/\bfrom\s+["'].*?["']/gi, ' ')
    titleWithoutBrackets = titleWithoutBrackets.replace(/\bfrom\s+[A-Za-z0-9\s:]+/gi, ' ')

    // 3. Strip version/reprise/mix clauses
    titleWithoutBrackets = titleWithoutBrackets.replace(/\bversion[.\s0-9]+/gi, ' ')

    // 4. Primary Title Key (first segment before separators like |, -, :, ~, /)
    const firstSegment = titleWithoutBrackets.split(/[|\-:~–—]/)[0] || titleWithoutBrackets
    const primaryCleaned = firstSegment.replace(noiseRegex, ' ')
    const primaryTitle = phoneticNormalize(primaryCleaned)

    // 5. Full Title Key
    const fullCleaned = titleWithoutBrackets.replace(noiseRegex, ' ')
    const fullTitle = phoneticNormalize(fullCleaned)

    if (primaryTitle.length >= 3 && seenPrimaryKeys.has(primaryTitle)) {
      continue
    }

    if (fullTitle.length >= 3 && seenFullKeys.has(fullTitle)) {
      continue
    }

    if (vid) seenIds.add(vid)
    if (primaryTitle.length >= 3) seenPrimaryKeys.add(primaryTitle)
    if (fullTitle.length >= 3) seenFullKeys.add(fullTitle)

    result.push(song)
  }

  return result
}

/**
 * Clean base title for cross-song comparison and deduplication
 */
export function cleanBaseTitle(rawTitle: string): string {
  if (!rawTitle) return ''
  const cleaned = cleanHtmlTitle(rawTitle)
  return cleaned
    .toLowerCase()
    .replace(/^(official\s*(video|audio|lyric(al)?\s*video)?|lyric(al)?\s*video|video\s*song|full\s*song|audio\s*song)\s*[:|-]\s*/i, '')
    .replace(/\(.*?\)/g, '')
    .replace(/\[.*?\]/g, '')
    .replace(/\s*[\|\-\–\—\:].*$/, '')
    .replace(/[^a-z0-9]/g, '')
    .trim()
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

  const baseA = cleanBaseTitle(songA.title || '')
  const baseB = cleanBaseTitle(songB.title || '')

  if (!baseA || !baseB) return false
  if (baseA === baseB) return true

  if (baseA.length >= 5 && baseB.includes(baseA)) return true
  if (baseB.length >= 5 && baseA.includes(baseB)) return true

  return false
}
