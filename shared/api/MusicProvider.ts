import type { Song } from '../models/song'

/**
 * Common Music Provider Interface
 * Decouples the recommendation engine from specific underlying vendor APIs (JioSaavn, YouTube Music, Spotify, etc.)
 */
export interface MusicProvider {
  /**
   * Search for tracks with query string
   */
  searchTracks(query: string, limit?: number): Promise<Song[]>

  /**
   * Fetch songs released strictly within the last N days (default: 30 days)
   * Calculated dynamically: today - N days
   */
  getNewReleases(languages: string[], days?: number, limit?: number): Promise<Song[]>

  /**
   * Fetch trending tracks filtered to user's preferred languages
   */
  getTrendingTracks(languages: string[], limit?: number): Promise<Song[]>

  /**
   * Fetch details for a specific track by its ID
   */
  getTrack(id: string): Promise<Song | null>

  /**
   * Fetch top tracks by a specific artist, optionally filtered by language
   */
  getArtistTracks(artistName: string, language?: string, limit?: number): Promise<Song[]>

  /**
   * Get related/similar artists based on genre, style, and collaborations
   */
  getRelatedArtists(artistName: string, language?: string): Promise<string[]>
}
