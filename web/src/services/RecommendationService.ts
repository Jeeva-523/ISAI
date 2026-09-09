import { musicApi, detectSongLanguage } from '@shared/api/music-api'
import { storageService } from '@shared/services/storageService'
import type { Song } from '@shared/models/song'
import type {
  ListeningEvent,
  UserPreferenceProfile,
  ScoredSong,
  PersonalizedHomeFeed,
  PlayEventSource
} from '@shared/models/recommendation.types'
import { RECOMMENDATION_CONFIG } from '@shared/constants/recommendation-config'
import { firestore } from '../firebase'
import { doc, setDoc } from 'firebase/firestore'

const STORAGE_PREFIX = 'isai_rec_profile_'

export class RecommendationService {
  private static instance: RecommendationService | null = null
  private profileCache: Map<string, UserPreferenceProfile> = new Map()

  private constructor() {}

  static getInstance(): RecommendationService {
    if (!RecommendationService.instance) {
      RecommendationService.instance = new RecommendationService()
    }
    return RecommendationService.instance
  }

  /**
   * Load or initialize a user's recommendation preference profile
   */
  async getProfile(userId: string, preferredLanguages: string[] = ['tamil']): Promise<UserPreferenceProfile> {
    if (this.profileCache.has(userId)) {
      return this.profileCache.get(userId)!
    }

    // 1. Try Local Storage
    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        const raw = localStorage.getItem(`${STORAGE_PREFIX}${userId}`)
        if (raw) {
          const parsed = JSON.parse(raw) as UserPreferenceProfile
          this.profileCache.set(userId, parsed)
          return parsed
        }
      } catch (e) {
        console.warn('[RecommendationService] Failed reading local rec profile:', e)
      }
    }

    // 2. Default initial profile
    const langMap: Record<string, number> = {}
    for (const l of preferredLanguages) {
      langMap[l.toLowerCase()] = 1.0
    }

    const defaultProfile: UserPreferenceProfile = {
      userId,
      totalMeaningfulPlays: 0,
      artistAffinityMap: {},
      genreAffinityMap: {},
      languageAffinityMap: langMap,
      skippedArtistsMap: {},
      trackPlayCounts: {},
      recentTrackIds: [],
      favoriteTrackIds: [],
      topArtistNames: [],
      lastUpdated: Date.now()
    }

    this.profileCache.set(userId, defaultProfile)
    this.saveProfile(defaultProfile)
    return defaultProfile
  }

  /**
   * Persist user profile locally and asynchronously to Firestore
   */
  async saveProfile(profile: UserPreferenceProfile): Promise<void> {
    profile.lastUpdated = Date.now()
    this.profileCache.set(profile.userId, profile)

    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        localStorage.setItem(`${STORAGE_PREFIX}${profile.userId}`, JSON.stringify(profile))
      } catch (e) {
        console.warn('[RecommendationService] Failed saving local profile:', e)
      }
    }

    // Firestore async sync
    if (profile.userId && !profile.userId.startsWith('guest_')) {
      try {
        setDoc(doc(firestore, 'users', profile.userId), {
          totalMeaningfulPlays: profile.totalMeaningfulPlays,
          artistAffinityMap: profile.artistAffinityMap,
          genreAffinityMap: profile.genreAffinityMap,
          languageAffinityMap: profile.languageAffinityMap,
          skippedArtistsMap: profile.skippedArtistsMap,
          topArtistNames: profile.topArtistNames,
          lastUpdated: profile.lastUpdated
        }, { merge: true }).catch(() => {})
      } catch {}
    }
  }

  /**
   * Record a listening event according to strict completion & skip thresholds
   */
  async recordListen(
    userId: string,
    song: Song,
    playedSeconds: number,
    totalDurationSeconds: number,
    source: PlayEventSource = 'home'
  ): Promise<void> {
    if (!song || !song.videoId || playedSeconds <= 0) return

    const profile = await this.getProfile(userId)
    const dur = totalDurationSeconds > 0 ? totalDurationSeconds : 210
    const completionPercentage = Math.min(1.0, playedSeconds / dur)

    const isMeaningful =
      playedSeconds >= RECOMMENDATION_CONFIG.MEANINGFUL_PLAY_SECONDS ||
      completionPercentage >= RECOMMENDATION_CONFIG.MEANINGFUL_PLAY_PERCENT

    const isSkippedEarly = playedSeconds < RECOMMENDATION_CONFIG.SKIP_PENALTY_THRESHOLD_SECONDS

    const event: ListeningEvent = {
      eventId: `${song.videoId}_${Date.now()}`,
      userId,
      trackId: song.videoId,
      title: song.title,
      artistName: song.channelTitle,
      language: song.language || detectSongLanguage(song),
      genre: song.genre,
      startedAt: Date.now() - Math.round(playedSeconds * 1000),
      durationPlayedSeconds: Math.round(playedSeconds),
      completionPercentage,
      source,
      isMeaningful,
      isCompleted: completionPercentage >= 0.95,
      isSkippedEarly
    }

    const artistKey = (song.channelTitle || '').trim()
    const langKey = (event.language || 'tamil').toLowerCase().trim()
    const genreKey = (song.genre || '').toLowerCase().trim()

    // Update play counts & recents
    profile.recentTrackIds = [song.videoId, ...profile.recentTrackIds.filter(id => id !== song.videoId)].slice(0, 50)
    profile.trackPlayCounts[song.videoId] = (profile.trackPlayCounts[song.videoId] || 0) + 1

    if (isMeaningful) {
      profile.totalMeaningfulPlays = (profile.totalMeaningfulPlays || 0) + 1

      if (artistKey) {
        profile.artistAffinityMap[artistKey] = (profile.artistAffinityMap[artistKey] || 0) + RECOMMENDATION_CONFIG.SIGNAL_VALUES.MEANINGFUL_PLAY_BOOST
      }
      if (langKey) {
        profile.languageAffinityMap[langKey] = (profile.languageAffinityMap[langKey] || 0) + 1.0
      }
      if (genreKey) {
        profile.genreAffinityMap[genreKey] = (profile.genreAffinityMap[genreKey] || 0) + 1.0
      }

      // Recompute top artists
      profile.topArtistNames = Object.entries(profile.artistAffinityMap)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5)
        .map(entry => entry[0])
    } else if (isSkippedEarly && artistKey) {
      profile.skippedArtistsMap[artistKey] = (profile.skippedArtistsMap[artistKey] || 0) + 1
      if (profile.artistAffinityMap[artistKey]) {
        profile.artistAffinityMap[artistKey] = Math.max(0, profile.artistAffinityMap[artistKey] + RECOMMENDATION_CONFIG.SIGNAL_VALUES.SKIP_PENALTY)
      }
    }

    await this.saveProfile(profile)
  }

  /**
   * 7-Signal Scoring Engine
   */
  scoreSong(song: Song, profile: UserPreferenceProfile, preferredLanguages: string[] = ['tamil']): ScoredSong {
    const artistKey = (song.channelTitle || '').trim()
    const songLang = (song.language || detectSongLanguage(song)).toLowerCase().trim()
    const genreKey = (song.genre || '').toLowerCase().trim()

    // 1. Language Match Score
    let languageMatchScore = 0
    if (preferredLanguages.map(l => l.toLowerCase()).includes(songLang)) {
      languageMatchScore = 1.0
    } else if (profile.languageAffinityMap[songLang] && profile.languageAffinityMap[songLang] > 2) {
      languageMatchScore = 0.8
    } else {
      languageMatchScore = 0.1
    }

    // 2. Artist Affinity Score
    let artistAffinityScore = 0
    if (artistKey && profile.artistAffinityMap[artistKey]) {
      artistAffinityScore = Math.min(1.0, profile.artistAffinityMap[artistKey] / 10.0)
    }

    // 3. Genre Affinity Score
    let genreAffinityScore = 0
    if (genreKey && profile.genreAffinityMap[genreKey]) {
      genreAffinityScore = Math.min(1.0, profile.genreAffinityMap[genreKey] / 8.0)
    }

    // 4. Freshness / Recency Score (tracks within 30 days score 1.0)
    let freshnessScore = 0.3
    if (song.releaseTimestamp) {
      const ageDays = (Date.now() - song.releaseTimestamp) / 86400000
      if (ageDays <= RECOMMENDATION_CONFIG.NEW_RELEASE_DAYS) freshnessScore = 1.0
      else if (ageDays <= 90) freshnessScore = 0.75
      else if (ageDays <= 365) freshnessScore = 0.5
      else freshnessScore = 0.25
    }

    // 5. Popularity Score
    let popularityScore = 0.5
    if (song.playCountNumber && song.playCountNumber > 0) {
      popularityScore = Math.min(1.0, Math.log10(song.playCountNumber) / 8.0)
    }

    // 6. Recent Listening & Repeat Count Score
    let recentListeningScore = 0
    const repeatPlays = profile.trackPlayCounts[song.videoId] || 0
    if (repeatPlays > 0) {
      recentListeningScore = Math.min(1.0, repeatPlays / 5.0)
    }

    // 7. General User Preference composite
    const userPreferenceScore = (artistAffinityScore * 0.6) + (genreAffinityScore * 0.4)

    // Compute weighted score using weights
    const weights = RECOMMENDATION_CONFIG.SCORING_WEIGHTS
    const totalScore =
      userPreferenceScore * weights.userPreferenceScore +
      recentListeningScore * weights.recentListeningScore +
      artistAffinityScore * weights.artistAffinityScore +
      genreAffinityScore * weights.genreAffinityScore +
      popularityScore * weights.popularityScore +
      freshnessScore * weights.freshnessScore +
      languageMatchScore * weights.languageMatchScore

    let category: 'familiar' | 'related' | 'discovery' = 'discovery'
    if (artistAffinityScore > 0.4 || recentListeningScore > 0.3) {
      category = 'familiar'
    } else if (genreAffinityScore > 0.3 || freshnessScore > 0.8) {
      category = 'related'
    }

    return {
      song,
      score: totalScore,
      breakdown: {
        userPreferenceScore,
        recentListeningScore,
        artistAffinityScore,
        genreAffinityScore,
        popularityScore,
        freshnessScore,
        languageMatchScore
      },
      category
    }
  }

  /**
   * Generate personalized home feed with cold start handling and 70/20/10 diversity
   */
  async getPersonalizedHomeFeed(
    userId: string,
    preferredLanguages: string[] = ['tamil']
  ): Promise<PersonalizedHomeFeed> {
    const profile = await this.getProfile(userId, preferredLanguages)
    const isColdStart = (profile.totalMeaningfulPlays || 0) < RECOMMENDATION_CONFIG.MIN_PLAYS_FOR_PERSONALIZATION
    const activeLangs = preferredLanguages.length > 0 ? preferredLanguages : ['tamil']

    // Candidate pool
    const [trending, newReleases] = await Promise.all([
      musicApi.getTrending(activeLangs, 60),
      musicApi.getNewReleases(activeLangs, RECOMMENDATION_CONFIG.NEW_RELEASE_DAYS, 25)
    ])

    // Derive top artist from listening history (recently played + favorites)
    const recentSongs = storageService.getRecentlyPlayed()
    const favSongs = storageService.getFavorites()
    const combinedHistory = [...recentSongs, ...favSongs]

    let derivedTopArtist = profile.topArtistNames[0] || ''
    if (combinedHistory.length > 0) {
      const artistCounts: Record<string, number> = {}
      combinedHistory.forEach((s) => {
        const rawArtist = (s.channelTitle || '').replace(/\s*-\s*Topic/gi, '').replace(/\s*Official/gi, '').trim()
        const clean = rawArtist.split(',')[0].split('&')[0].trim()
        if (clean && clean !== 'Tamil Artist' && clean !== 'Tamil Music' && clean !== 'ISAI Artist') {
          artistCounts[clean] = (artistCounts[clean] || 0) + 1
        }
      })
      const sortedArtists = Object.keys(artistCounts).sort((a, b) => artistCounts[b] - artistCounts[a])
      if (sortedArtists.length > 0) {
        derivedTopArtist = sortedArtists[0]
      }
    }

    const topArtistAnchor = derivedTopArtist || (trending[0]?.channelTitle?.split(',')[0]?.trim() || 'Anirudh Ravichander')

    // Dynamically search targeted songs for top artist (matches Android app behavior)
    let targetedArtistSongs: Song[] = []
    if (derivedTopArtist) {
      try {
        const hits = await musicApi.searchSongs(`${derivedTopArtist} Tamil songs`, 15)
        if (hits && hits.length > 0) {
          targetedArtistSongs = hits.filter(h => !combinedHistory.some(c => c.videoId === h.videoId))
          if (targetedArtistSongs.length === 0) targetedArtistSongs = hits
        }
      } catch (err) {
        console.warn('[RecommendationService] Artist songs search warning:', err)
      }
    }

    const becauseYouListenSection = (derivedTopArtist && targetedArtistSongs.length > 0) ? {
      artistName: derivedTopArtist,
      songs: targetedArtistSongs.slice(0, 12)
    } : (derivedTopArtist ? {
      artistName: derivedTopArtist,
      songs: trending.filter(s => (s.channelTitle || '').toLowerCase().includes(derivedTopArtist.toLowerCase())).slice(0, 10)
    } : undefined)

    // Cold Start (< 10 meaningful plays)
    if (isColdStart) {
      const sortedByPlays = [...trending].sort((a, b) => (b.playCountNumber || 0) - (a.playCountNumber || 0))
      const topFresh = newReleases.length > 0 ? newReleases : trending
      return {
        isColdStart: true,
        totalMeaningfulPlays: profile.totalMeaningfulPlays,
        preferredLanguages: activeLangs,
        topArtistAnchor,
        sections: {
          madeForYou: topFresh.slice(0, 15),
          becauseYouListenTo: becauseYouListenSection,
          newReleases30Days: newReleases.slice(0, 15),
          trendingInLanguage: sortedByPlays.slice(0, 20),
          popularArtists: [
            { id: '1', name: 'Anirudh Ravichander', imageUrl: 'https://upload.wikimedia.org/wikipedia/commons/thumb/d/d1/Anirudh_Ravichander_at_Audi_Ritz_Style_Awards_2015.jpg/440px-Anirudh_Ravichander_at_Audi_Ritz_Style_Awards_2015.jpg' },
            { id: '2', name: 'A.R. Rahman', imageUrl: 'https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/A._R._Rahman_at_the_Global_Indian_Music_Awards_2012.jpg/440px-A._R._Rahman_at_the_Global_Indian_Music_Awards_2012.jpg' },
            { id: '3', name: 'Yuvan Shankar Raja', imageUrl: 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Yuvan_Shankar_Raja_at_Pyaar_Prema_Kaadhal_Press_Meet.jpg/440px-Yuvan_Shankar_Raja_at_Pyaar_Prema_Kaadhal_Press_Meet.jpg' }
          ],
          discoverSomethingNew: trending.slice(15, 30)
        }
      }
    }

    // Returning User (>= 10 plays):
    const candidatePool = [...newReleases, ...trending]
    const uniquePool = Array.from(new Map(candidatePool.map(s => [s.videoId, s])).values())
    const scoredList = uniquePool.map(s => this.scoreSong(s, profile, activeLangs))
    scoredList.sort((a, b) => b.score - a.score)

    // 70% familiar, 20% related, 10% discovery
    const targetCount = 20
    const countFamiliar = Math.round(targetCount * RECOMMENDATION_CONFIG.DIVERSITY_RATIOS.FAMILIAR_CONTENT_PERCENT)
    const countRelated = Math.round(targetCount * RECOMMENDATION_CONFIG.DIVERSITY_RATIOS.RELATED_CONTENT_PERCENT)
    const countDiscovery = targetCount - countFamiliar - countRelated

    const familiar = scoredList.filter(s => s.category === 'familiar').slice(0, countFamiliar).map(s => s.song)
    const related = scoredList.filter(s => s.category === 'related').slice(0, countRelated).map(s => s.song)
    const discovery = scoredList.filter(s => s.category === 'discovery').slice(0, countDiscovery).map(s => s.song)

    const mixedMadeForYou = [...familiar, ...related, ...discovery]
    const finalMadeForYou = mixedMadeForYou.length >= 8 ? mixedMadeForYou : scoredList.slice(0, 15).map(s => s.song)

    return {
      isColdStart: false,
      totalMeaningfulPlays: profile.totalMeaningfulPlays,
      preferredLanguages: activeLangs,
      topArtistAnchor,
      sections: {
        madeForYou: finalMadeForYou,
        becauseYouListenTo: becauseYouListenSection,
        newReleases30Days: newReleases.slice(0, 15),
        trendingInLanguage: trending.slice(0, 20),
        popularArtists: [
          { id: '1', name: 'Anirudh Ravichander', imageUrl: 'https://upload.wikimedia.org/wikipedia/commons/thumb/d/d1/Anirudh_Ravichander_at_Audi_Ritz_Style_Awards_2015.jpg/440px-Anirudh_Ravichander_at_Audi_Ritz_Style_Awards_2015.jpg' },
          { id: '2', name: 'A.R. Rahman', imageUrl: 'https://upload.wikimedia.org/wikipedia/commons/thumb/a/ad/A._R._Rahman_at_the_Global_Indian_Music_Awards_2012.jpg/440px-A._R._Rahman_at_the_Global_Indian_Music_Awards_2012.jpg' },
          { id: '3', name: 'Yuvan Shankar Raja', imageUrl: 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Yuvan_Shankar_Raja_at_Pyaar_Prema_Kaadhal_Press_Meet.jpg/440px-Yuvan_Shankar_Raja_at_Pyaar_Prema_Kaadhal_Press_Meet.jpg' }
        ],
        discoverSomethingNew: discovery.length > 0 ? discovery : trending.slice(15, 30)
      }
    }
  }
}

export const recommendationService = RecommendationService.getInstance()
