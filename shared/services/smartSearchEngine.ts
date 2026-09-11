import { MASTER_KEYWORD_DICTIONARY, CAT_LANGUAGE, CAT_ARTIST, CAT_MOOD, CAT_LOVE_ROMANCE, CAT_SAD_EMOTIONAL, CAT_GENRE, CAT_TAMIL_GENRE, CAT_ACTIVITY, CAT_SITUATION, CAT_WEATHER, CAT_YEAR_ERA, CAT_RELEASE, CAT_CLASSIC, CAT_DEVOTIONAL } from '../constants/keywordDictionary'

export interface ParsedSearchIntent {
  rawQuery: string
  canonicalQuery: string
  detectedLanguage?: string
  detectedArtist?: string
  detectedMood?: string
  detectedGenre?: string
  detectedActivity?: string
  detectedYearOrEra?: string
  detectedSituation?: string
  isLatest: boolean
  isClassicOld: boolean
  isDevotional: boolean
  unmatchedTerms: string[]
  optimizedSearchQuery: string
}

const SPELLING_MAP: Record<string, string> = {
  // Romance & Mood
  kaadhal: 'kadhal',
  kathal: 'kadhal',
  காதல்: 'kadhal',
  'காதல் பாடல்கள்': 'kadhal songs',
  oruthalai: 'one sided love',
  'ஒருதலை காதல்': 'one sided love',
  // Songs
  paatu: 'songs',
  paattu: 'songs',
  பாட்டு: 'songs',
  பாடல்: 'songs',
  பாடல்கள்: 'songs',
  isai: 'music',
  இசை: 'music',
  // Artists
  aniruth: 'anirudh',
  aniruthu: 'anirudh',
  அனிருத்: 'anirudh',
  rahman: 'ar rahman',
  'a r rahman': 'ar rahman',
  'a.r. rahman': 'ar rahman',
  arr: 'ar rahman',
  ரஹ்மான்: 'ar rahman',
  ilayaraja: 'ilaiyaraaja',
  ilaiyaraja: 'ilaiyaraaja',
  இளையராஜா: 'ilaiyaraaja',
  u1: 'yuvan',
  'yuvan shankar raja': 'yuvan',
  யுவன்: 'yuvan',
  harris: 'harris jayaraj',
  hj: 'harris jayaraj',
  gvp: 'gv prakash',
  'g v prakash': 'gv prakash',
  sana: 'santhosh narayanan',
  sidsriram: 'sid sriram',
  // Sad / Emotional
  sogam: 'sad',
  sogamana: 'sad',
  சோகம்: 'sad',
  'சோக பாடல்கள்': 'sad songs',
  pirivu: 'sad separation',
  பிரிவு: 'sad separation',
  yaekkam: 'longing',
  yekkam: 'longing',
  ஏக்கம்: 'longing',
  'kaadhal tholvi': 'love failure',
  'kadhal tholvi': 'love failure',
  'காதல் தோல்வி': 'love failure',
  // Genres
  gana: 'gaana',
  கானா: 'gaana',
  dappankuthu: 'kuthu',
  'dappan kuthu': 'kuthu',
  குத்து: 'kuthu',
  மெலடி: 'melody',
  nattupura: 'folk',
  gramathu: 'folk',
  // Activity / Nature
  mazhai: 'rain',
  மழை: 'rain',
  gym: 'workout',
  'lo-fi': 'lofi',
  // Devotional
  முருகன்: 'murugan devotional',
  அம்மன்: 'amman devotional',
  விநாயகர்: 'vinayagar devotional',
  பெருமாள்: 'perumal devotional',
  // Relationships
  amma: 'mother',
  அம்மா: 'mother',
  appa: 'father',
  அப்பா: 'father',
  nanban: 'friendship',
  நண்பன்: 'friendship',
  natpu: 'friendship',
  நட்பு: 'friendship',
  puthiya: 'latest new',
  'புதிய பாடல்கள்': 'latest new songs',
  pazhaya: 'old classic',
  'பழைய பாடல்கள்': 'old classic songs'
}

export class SmartSearchEngine {
  public static normalizeText(input: string): string {
    let text = input.trim().toLowerCase()
    for (const [variant, canonical] of Object.entries(SPELLING_MAP)) {
      const escaped = variant.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      text = text.replace(new RegExp(`\\b${escaped}\\b`, 'gi'), canonical)
    }
    return text.replace(/\s+/g, ' ').trim()
  }

  public static parseQuery(
    rawQuery: string,
    userPreferredLanguages: string[] = ['tamil'],
    _userFavoriteArtists: string[] = []
  ): ParsedSearchIntent {
    const cleanRaw = rawQuery.trim()
    if (!cleanRaw) {
      return {
        rawQuery: '',
        canonicalQuery: '',
        isLatest: false,
        isClassicOld: false,
        isDevotional: false,
        unmatchedTerms: [],
        optimizedSearchQuery: ''
      }
    }

    const normalized = this.normalizeText(cleanRaw)
    const tokens = normalized.split(' ').filter(Boolean)

    let detectedLang: string | undefined
    let detectedArtist: string | undefined
    let detectedMood: string | undefined
    let detectedGenre: string | undefined
    let detectedActivity: string | undefined
    let detectedYearOrEra: string | undefined
    let detectedSituation: string | undefined
    let isLatest = false
    let isClassicOld = false
    let isDevotional = false

    const matchedTokens = new Set<string>()

    // Check against Master Dictionary phrases
    for (const entry of MASTER_KEYWORD_DICTIONARY) {
      const phrases = [entry.keyword, ...entry.synonyms].sort((a, b) => b.length - a.length)
      for (const phrase of phrases) {
        const pNorm = phrase.toLowerCase().trim()
        if (normalized.includes(pNorm)) {
          if (entry.category === CAT_LANGUAGE && !detectedLang) detectedLang = entry.keyword
          if (entry.category === CAT_ARTIST && !detectedArtist) detectedArtist = entry.keyword
          if ((entry.category === CAT_MOOD || entry.category === CAT_LOVE_ROMANCE || entry.category === CAT_SAD_EMOTIONAL) && !detectedMood) {
            detectedMood = entry.keyword
          }
          if ((entry.category === CAT_GENRE || entry.category === CAT_TAMIL_GENRE) && !detectedGenre) {
            detectedGenre = entry.keyword
          }
          if (entry.category === CAT_ACTIVITY && !detectedActivity) detectedActivity = entry.keyword
          if ((entry.category === CAT_SITUATION || entry.category === CAT_WEATHER) && !detectedSituation) {
            detectedSituation = entry.keyword
          }
          if (entry.category === CAT_YEAR_ERA && !detectedYearOrEra) detectedYearOrEra = entry.keyword
          if (entry.category === CAT_RELEASE) isLatest = true
          if (entry.category === CAT_CLASSIC) isClassicOld = true
          if (entry.category === CAT_DEVOTIONAL) isDevotional = true

          pNorm.split(' ').forEach((w) => matchedTokens.add(w))
        }
      }
    }

    const stopWords = new Set(['song', 'songs', 'track', 'music', 'for', 'in', 'the', 'a', 'an', 'paatu', 'paadal'])
    const unmatched = tokens.filter((t) => !matchedTokens.has(t) && !stopWords.has(t))

    const primaryLang = (userPreferredLanguages[0] || 'tamil').toLowerCase()
    const effectiveLang = detectedLang || primaryLang

    const queryParts: string[] = []

    if (effectiveLang) {
      queryParts.push(effectiveLang.charAt(0).toUpperCase() + effectiveLang.slice(1))
    }

    if (detectedArtist) {
      const formattedArtist =
        detectedArtist === 'anirudh' ? 'Anirudh Ravichander' :
        detectedArtist === 'ar rahman' ? 'A.R. Rahman' :
        detectedArtist === 'ilaiyaraaja' ? 'Ilaiyaraaja' :
        detectedArtist === 'yuvan' ? 'Yuvan Shankar Raja' :
        detectedArtist.charAt(0).toUpperCase() + detectedArtist.slice(1)
      queryParts.push(formattedArtist)
    }

    if (detectedYearOrEra) queryParts.push(detectedYearOrEra)
    if (detectedMood) queryParts.push(detectedMood === 'love' ? 'romantic love' : detectedMood)
    if (detectedGenre) queryParts.push(detectedGenre === 'gaana' ? 'gana' : detectedGenre)
    if (detectedActivity === 'workout') queryParts.push('gym workout beats')
    if (detectedActivity === 'travel') queryParts.push('road trip driving')
    if (detectedSituation === 'rain') queryParts.push('rain melody')
    if (isDevotional) queryParts.push('devotional bhakti')

    if (unmatched.length > 0) {
      queryParts.push(unmatched.join(' '))
    }

    if (unmatched.length === 0) {
      if (isLatest) queryParts.push('latest new songs')
      else if (isClassicOld) queryParts.push('old classic evergreen hits')
      else queryParts.push('hit songs')
    }

    const optimized = Array.from(new Set(queryParts)).join(' ').replace(/\s+/g, ' ').trim()

    return {
      rawQuery,
      canonicalQuery: normalized,
      detectedLanguage: detectedLang,
      detectedArtist,
      detectedMood,
      detectedGenre,
      detectedActivity,
      detectedYearOrEra,
      detectedSituation,
      isLatest,
      isClassicOld,
      isDevotional,
      unmatchedTerms: unmatched,
      optimizedSearchQuery: optimized || rawQuery.trim()
    }
  }

  public static rankSong(
    song: { title: string; channelTitle: string },
    intent: ParsedSearchIntent,
    userPreferredLanguages: string[] = ['tamil'],
    userFavoriteArtists: string[] = []
  ): number {
    let score = 100
    const text = `${song.title} ${song.channelTitle}`.toLowerCase()

    const targetLang = intent.detectedLanguage || userPreferredLanguages[0]?.toLowerCase()
    if (targetLang && text.includes(targetLang)) score += 30

    if (intent.detectedArtist && text.includes(intent.detectedArtist.toLowerCase())) {
      score += 50
    } else {
      for (const fav of userFavoriteArtists) {
        if (fav && text.includes(fav.toLowerCase())) {
          score += 25
          break
        }
      }
    }

    if (intent.detectedMood && (text.includes(intent.detectedMood) || text.includes('love') || text.includes('romantic'))) {
      score += 20
    }
    if (intent.detectedGenre && text.includes(intent.detectedGenre)) score += 25
    if (intent.detectedYearOrEra && text.includes(intent.detectedYearOrEra.replace('s', ''))) score += 25

    for (const term of intent.unmatchedTerms) {
      if (text.includes(term.toLowerCase())) score += 40
    }

    return score
  }
}
