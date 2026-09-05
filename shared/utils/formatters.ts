/**
 * Clean raw HTML entities from titles (e.g. &amp;, &#39;, &quot;)
 */
export function cleanHtmlTitle(raw: string): string {
  if (!raw) return ''
  return raw
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .trim()
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
