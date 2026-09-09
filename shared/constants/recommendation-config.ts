/**
 * Centralized Recommendation & Personalization Engine Configuration
 * All mathematical weights, thresholds, and diversity ratios are defined here.
 */

export const RECOMMENDATION_CONFIG = {
  // New Release Window (Dynamic: today - 30 days)
  NEW_RELEASE_DAYS: 30,

  // Cold Start & Personalization Tier Thresholds
  MIN_PLAYS_FOR_PERSONALIZATION: 10,
  STRONG_PERSONALIZATION_PLAYS: 50,

  // Meaningful Listen Thresholds (whichever reached first)
  MEANINGFUL_PLAY_SECONDS: 30,
  MEANINGFUL_PLAY_PERCENT: 0.50,

  // Skip Penalty Threshold (skipping before this duration is a negative signal)
  SKIP_PENALTY_THRESHOLD_SECONDS: 10,

  // Diversity & Content Distribution Ratios
  DIVERSITY_RATIOS: {
    FAMILIAR_CONTENT_PERCENT: 0.70, // 70% familiar / favorite artists
    RELATED_CONTENT_PERCENT: 0.20,   // 20% similar / collaborator artists
    DISCOVERY_CONTENT_PERCENT: 0.10  // 10% fresh discovery tracks
  },

  // Per-Artist Repetition Cap in Recommendation Mix
  MAX_TRACKS_PER_ARTIST_IN_FEED: 4,

  // Recommendation Scoring Signal Weights (Must sum to 1.0)
  SCORING_WEIGHTS: {
    userPreferenceScore: 0.30,
    recentListeningScore: 0.20,
    artistAffinityScore: 0.15,
    genreAffinityScore: 0.10,
    popularityScore: 0.10,
    freshnessScore: 0.10,
    languageMatchScore: 0.05
  },

  // Signal Boosts & Penalties
  SIGNAL_VALUES: {
    MEANINGFUL_PLAY_BOOST: 1.0,
    REPEAT_PLAY_BOOST: 1.5,
    FAVORITE_BOOST: 3.0,
    PLAYLIST_ADD_BOOST: 3.0,
    SKIP_PENALTY: -1.5
  },

  // Cache Time-to-Live
  RECOMMENDATION_CACHE_TTL_HOURS: 12,
  INVALIDATE_AFTER_MEANINGFUL_PLAYS: 5
} as const

export type RecommendationConfig = typeof RECOMMENDATION_CONFIG
