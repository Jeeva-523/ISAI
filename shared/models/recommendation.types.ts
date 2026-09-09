import type { Song } from './song'

export type PlayEventSource = 'home' | 'search' | 'playlist' | 'recommendation' | 'queue'

export interface ListeningEvent {
  eventId: string
  userId: string
  trackId: string
  title: string
  artistId?: string
  artistName: string
  albumId?: string
  albumName?: string
  language?: string
  genre?: string
  startedAt: number
  durationPlayedSeconds: number
  completionPercentage: number
  source: PlayEventSource
  isMeaningful: boolean
  isCompleted: boolean
  isSkippedEarly: boolean
}

export interface UserPreferenceProfile {
  userId: string
  totalMeaningfulPlays: number
  artistAffinityMap: Record<string, number> // artistName -> score
  genreAffinityMap: Record<string, number>  // genre -> score
  languageAffinityMap: Record<string, number> // language -> score
  skippedArtistsMap: Record<string, number>  // artistName -> skipCount
  trackPlayCounts: Record<string, number>    // trackId -> count
  recentTrackIds: string[]                   // chronological
  favoriteTrackIds: string[]
  topArtistNames: string[]
  lastUpdated: number
}

export interface ScoredSong {
  song: Song
  score: number
  breakdown: {
    userPreferenceScore: number
    recentListeningScore: number
    artistAffinityScore: number
    genreAffinityScore: number
    popularityScore: number
    freshnessScore: number
    languageMatchScore: number
  }
  category: 'familiar' | 'related' | 'discovery'
}

export interface PersonalizedHomeFeed {
  isColdStart: boolean
  totalMeaningfulPlays: number
  preferredLanguages: string[]
  topArtistAnchor?: string // e.g. "Anirudh Ravichander"
  sections: {
    madeForYou?: Song[]
    becauseYouListenTo?: {
      artistName: string
      songs: Song[]
    }
    newReleases30Days: Song[]
    myMostPlayed?: Song[]
    trendingInLanguage: Song[]
    popularArtists: { id: string; name: string; imageUrl?: string }[]
    discoverSomethingNew?: Song[]
    popularAlbums?: { id: string; title: string; imageUrl?: string; artist?: string }[]
  }
}
