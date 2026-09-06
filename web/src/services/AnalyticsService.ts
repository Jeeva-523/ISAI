export interface PlayEvent {
  eventId: string
  songId: string
  songTitle: string
  artistName: string
  eventType: 'play' | 'like' | 'playlist_add' | 'search'
  timestamp: number
}

class AnalyticsService {
  private events: PlayEvent[] = []

  constructor() {
    this.loadEvents()
  }

  trackEvent(songId: string, title: string, artist: string, eventType: 'play' | 'like' | 'playlist_add' | 'search') {
    const event: PlayEvent = {
      eventId: Math.random().toString(36).substring(2),
      songId,
      songTitle: title,
      artistName: artist,
      eventType,
      timestamp: Date.now()
    }
    this.events.unshift(event)
    if (this.events.length > 500) {
      this.events.pop()
    }
    this.saveEvents()
  }

  getEventCount(songId: string, eventType: 'play' | 'like' | 'playlist_add' | 'search', timeWindowMs: number): number {
    const cutoff = Date.now() - timeWindowMs
    return this.events.filter((e) => e.songId === songId && e.eventType === eventType && e.timestamp >= cutoff).length
  }

  getUniqueListenerCount(songId: string, timeWindowMs: number): number {
    const plays = this.getEventCount(songId, 'play', timeWindowMs)
    return Math.max(plays > 0 ? 1 : 0, Math.floor(plays * 0.85))
  }

  private loadEvents() {
    try {
      const stored = localStorage.getItem('isai_analytics_events')
      if (stored) {
        this.events = JSON.parse(stored)
      }
    } catch {
      this.events = []
    }
  }

  private saveEvents() {
    try {
      localStorage.setItem('isai_analytics_events', JSON.stringify(this.events.slice(0, 100)))
    } catch {}
  }
}

export const analyticsService = new AnalyticsService()
