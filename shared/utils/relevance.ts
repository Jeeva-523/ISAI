import type { Song } from '../models/song'

export type SongMood =
  | 'GANA_FOLK'
  | 'INTRO_MASS'
  | 'MELODY_ROMANCE'
  | 'PARTY_KUTHU'
  | 'SAD_HEARTBREAK'
  | 'DEVOTIONAL'
  | 'GENERAL'

// Keyword patterns for detecting Tamil Gaana / Folk / Marana Gaana songs
const GANA_KEYWORDS = [
  'gana',
  'gaana',
  'marana gana',
  'chennai gana',
  'local gana',
  'pullingo',
  'danga maari',
  'kallamanna',
  'tasakku',
  'tasaku',
  'aaluma doluma',
  'jithu jilladi',
  'varuthu da gaana',
  'athana pathana',
  'ootaanda',
  'enna pulla',
  'adangatha asuran',
  'dappan',
  'dappankuthu',
  'thara local',
  'oru kuchi',
  'madura',
  'karuppu nerathazhagi',
  'chinna machan',
  'sandakozhi',
  'paruthiveeran',
  'sooravali',
  'komban',
  'asuran',
  'nattu koothu',
  'folk',
  'therukural',
  'oppari',
  'villupaattu',
  'gumbalaga suthuvom',
  'morattu single',
  'vaadi pulla vaadi',
  'chill bro',
  'kattikida',
  'kattu kattu',
  'machan meesa'
]

const GANA_ARTISTS = [
  'gana bala',
  'marana gana viji',
  'gana stephen',
  'gana prabha',
  'gana ulaganathan',
  'gana vinoth',
  'gana muthu',
  'anthony daasan',
  'velmurugan',
  'deva',
  'thenisai thendral',
  'kidakkuzhi sabaritha',
  'chinna ponnu',
  'rokesh',
  'kabilan vairamuthu'
]

// Keyword patterns for detecting Tamil intro / mass / hero entry songs
const INTRO_MASS_KEYWORDS = [
  'intro',
  'mass',
  'hero',
  'entry',
  'hukum',
  'alappara',
  'naa ready',
  'badass',
  'hunter',
  'vantaar',
  'matta',
  'whistle',
  'neruppu',
  'marana',
  'vaathi',
  'dharala',
  'jalabulanjangu',
  'arabic kuthu',
  'verithanam',
  'surviva',
  'aalaporaan',
  'theemai dhaan',
  'beast mode',
  'leo',
  'jailer',
  'master',
  'petta',
  'kabali',
  'mersal',
  'vikram',
  'vettaiyan',
  'kanguva',
  'bloody',
  'power',
  'roar',
  'tiger',
  'singam',
  'santhosh',
  'attitude',
  'anthem',
  'theme',
  'bgm'
]

// Keyword patterns for detecting Tamil melody / love / romance songs
const MELODY_KEYWORDS = [
  'melody',
  'love',
  'kadhal',
  'kaadhal',
  'kanave',
  'unakkul',
  'nenjukkul',
  'vaseegara',
  'munbe vaa',
  'romantic',
  'soul',
  'feel good',
  'marakkuma',
  'malare',
  'mudhal nee',
  'pirai',
  'enodu',
  'thalli pogathey',
  'kadhalaada',
  'megham karukatha',
  'anbil avan',
  'kannazhaga',
  'poove',
  'oru manam',
  'kurumugil',
  'vennilave',
  'roja',
  'minnale',
  'vinnaithaandi',
  'jeans',
  'alaipayuthey',
  'vizhiyil',
  'uyirin',
  'aaruyire',
  'poove sempoove',
  'ennodu nee irundhal',
  'unakkenna venum sollu',
  'kadhale kadhale',
  'kannamma',
  'anbe',
  'uyire',
  'thaen thaen',
  'mayakkama',
  'un perai solla',
  'suttum vizhi',
  'oru dheyvam thantha',
  'pookkal pookkum'
]

// Keyword patterns for party / kuthu dance songs
const KUTHU_KEYWORDS = [
  'kuthu',
  'party',
  'dance',
  'dappankuthu',
  'rowdy baby',
  'kaavaalaa',
  'local',
  'chilla',
  'goli soda',
  'dandanakka',
  'machan',
  'taana',
  'donu donu',
  'sodakku',
  'sarattu vandiyila',
  'sarakku',
  'vaathi coming',
  'dippam dappam',
  'thee thalapathy'
]

// Keyword patterns for sad / heartbreak songs
const SAD_KEYWORDS = [
  'sad',
  'breakup',
  'pain',
  'kanneer',
  'pirivu',
  'sogam',
  'valigal',
  'thanimai',
  'pogadha pogadha',
  'en kanmani',
  'nenje nenje',
  'kannukulla'
]

const NON_TAMIL_LANGUAGES = [
  'telugu',
  'hindi',
  'malayalam',
  'kannada',
  'punjabi',
  'bhojpuri',
  'english',
  'bengali',
  'marathi',
  'gujarati'
]

/**
 * Clean and normalize text for comparison
 */
function normalizeText(text?: string): string {
  return (text || '')
    .toLowerCase()
    .replace(/[^\w\s]/gi, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * Check if a candidate song belongs to the user's preferred language
 */
export function isSongInLanguage(song: Song, preferredLanguages: string[] = ['tamil']): boolean {
  const normLangs = preferredLanguages.map(l => l.toLowerCase().trim()).filter(Boolean)
  if (normLangs.length === 0) return true

  const text = `${song.title || ''} ${song.album || ''} ${song.channelTitle || ''}`.toLowerCase()

  // If song explicitly contains another language that user DID NOT select:
  for (const otherLang of NON_TAMIL_LANGUAGES) {
    if (!normLangs.includes(otherLang)) {
      const regex = new RegExp(`\\b${otherLang}\\b`, 'i')
      if (regex.test(text)) {
        return false
      }
    }
  }

  return true
}

/**
 * Extract clean primary composer or artist name
 */
export function extractPrimaryArtist(channelOrArtist?: string): string {
  const norm = normalizeText(channelOrArtist)
    .replace(/topic|official|vevo|channel|sun tv|sony music south|think music india|wunderbar films/gi, '')
    .trim()

  if (norm.includes('anirudh')) return 'anirudh ravichander'
  if (norm.includes('rahman') || norm.includes('a r rahman') || norm.includes('a.r.')) return 'a r rahman'
  if (norm.includes('yuvan') || norm.includes('u1')) return 'yuvan shankar raja'
  if (norm.includes('harris')) return 'harris jayaraj'
  if (norm.includes('santhosh narayanan') || norm.includes('sana')) return 'santhosh narayanan'
  if (norm.includes('g v prakash') || norm.includes('gv prakash')) return 'g v prakash'
  if (norm.includes('ilaiyaraaja') || norm.includes('ilayaraja')) return 'ilaiyaraaja'
  if (norm.includes('sid sriram')) return 'sid sriram'
  if (norm.includes('gana bala')) return 'gana bala'
  if (norm.includes('marana gana viji')) return 'marana gana viji'
  if (norm.includes('anthony daasan')) return 'anthony daasan'
  if (norm.includes('deva')) return 'deva'

  return norm.split(' ')[0] || ''
}

/**
 * Detect the dominant mood/genre of a given song
 */
export function detectSongMood(song: Song): SongMood {
  const text = normalizeText(`${song.title} ${song.album || ''} ${song.channelTitle || ''}`)

  // 1. Check Gaana / Folk (Highest priority for local beat songs)
  for (const artist of GANA_ARTISTS) {
    if (text.includes(artist)) return 'GANA_FOLK'
  }
  for (const kw of GANA_KEYWORDS) {
    if (text.includes(kw)) return 'GANA_FOLK'
  }

  // 2. Check Sad / Heartbreak
  for (const kw of SAD_KEYWORDS) {
    if (text.includes(kw)) return 'SAD_HEARTBREAK'
  }

  // 3. Check Intro / Mass
  for (const kw of INTRO_MASS_KEYWORDS) {
    if (text.includes(kw)) return 'INTRO_MASS'
  }

  // 4. Check Party / Kuthu
  for (const kw of KUTHU_KEYWORDS) {
    if (text.includes(kw)) return 'PARTY_KUTHU'
  }

  // 5. Check Melodies / Romantic
  for (const kw of MELODY_KEYWORDS) {
    if (text.includes(kw)) return 'MELODY_ROMANCE'
  }

  return 'GENERAL'
}

/**
 * Score relevance between a target song and a candidate song (0 to 100+)
 */
export function scoreSongRelevance(
  target: Song,
  candidate: Song,
  preferredLanguages: string[] = ['tamil']
): number {
  if (target.videoId === candidate.videoId) return 9999

  // Reject songs that clearly belong to unselected foreign languages
  if (!isSongInLanguage(candidate, preferredLanguages)) {
    return -999
  }

  let score = 0
  const targetMood = detectSongMood(target)
  const candidateMood = detectSongMood(candidate)

  // 1. Exact Mood / Style Match (Gaana stays with Gaana, Melody stays with Melody)
  if (targetMood === candidateMood && targetMood !== 'GENERAL') {
    score += 90
  } else if (
    (targetMood === 'GANA_FOLK' && candidateMood === 'PARTY_KUTHU') ||
    (targetMood === 'PARTY_KUTHU' && candidateMood === 'GANA_FOLK')
  ) {
    score += 50 // Folk + Fast Kuthu synergy
  } else if (
    (targetMood === 'INTRO_MASS' && candidateMood === 'PARTY_KUTHU') ||
    (targetMood === 'PARTY_KUTHU' && candidateMood === 'INTRO_MASS')
  ) {
    score += 45 // High-energy mass synergy
  } else if (
    (targetMood === 'GANA_FOLK' && (candidateMood === 'MELODY_ROMANCE' || candidateMood === 'SAD_HEARTBREAK')) ||
    ((targetMood === 'MELODY_ROMANCE' || targetMood === 'SAD_HEARTBREAK') && candidateMood === 'GANA_FOLK')
  ) {
    score -= 100 // Severe penalty: NEVER mix Gaana with slow soft melodies or sad songs
  } else if (
    (targetMood === 'INTRO_MASS' && candidateMood === 'MELODY_ROMANCE') ||
    (targetMood === 'MELODY_ROMANCE' && candidateMood === 'INTRO_MASS')
  ) {
    score -= 60 // Heavy penalty for mass vs soft romance clash
  } else if (
    (targetMood === 'PARTY_KUTHU' && candidateMood === 'MELODY_ROMANCE') ||
    (targetMood === 'MELODY_ROMANCE' && candidateMood === 'PARTY_KUTHU')
  ) {
    score -= 60
  }

  // 2. Composer / Artist Match
  const targetArtist = extractPrimaryArtist(target.channelTitle)
  const candidateArtist = extractPrimaryArtist(candidate.channelTitle)
  if (targetArtist && candidateArtist && targetArtist === candidateArtist) {
    score += 35
  }

  // 3. Same Movie / Album Match
  const normTargetAlbum = normalizeText(target.album || target.title)
  const normCandAlbum = normalizeText(candidate.album || candidate.title)
  if (normTargetAlbum && normCandAlbum) {
    const movieWords = normTargetAlbum.split(' ').filter(w => w.length > 3)
    for (const mw of movieWords) {
      if (normCandAlbum.includes(mw)) {
        score += 25
        break
      }
    }
  }

  // 4. Preferred Language Boost
  const candText = `${candidate.title} ${candidate.channelTitle}`.toLowerCase()
  for (const lang of preferredLanguages) {
    if (candText.includes(lang.toLowerCase())) {
      score += 20
      break
    }
  }

  return score
}

/**
 * Generate a smart search query to find additional relevant songs online
 */
export function getRelevantSearchQuery(song: Song, preferredLang = 'tamil'): string {
  const lang = preferredLang ? preferredLang.toLowerCase().trim() : 'tamil'
  const mood = detectSongMood(song)
  const artist = extractPrimaryArtist(song.channelTitle)
  const artistPrefix = artist ? `${artist} ` : ''

  switch (mood) {
    case 'GANA_FOLK':
      return `${lang} gana songs ${artistPrefix}super hit marana gana kuthu`
    case 'PARTY_KUTHU':
      return `${lang} party kuthu dance celebration songs ${artistPrefix}`
    case 'INTRO_MASS':
      return `${lang} mass hero intro entry hit songs ${artistPrefix}`
    case 'MELODY_ROMANCE':
      return `${lang} feel good romantic love melody hit songs ${artistPrefix}`
    case 'SAD_HEARTBREAK':
      return `${lang} sad breakup emotional hit songs ${artistPrefix}`
    default:
      if (artist) {
        return `${artist} ${lang} super hit songs`
      }
      return `${song.title.split('-')[0].trim()} ${lang} songs`
  }
}

/**
 * Build an intelligently ordered relevant playback queue.
 * Target song is placed at index 0, followed by the most relevant matching songs.
 */
export function buildRelevantQueue(
  target: Song,
  candidatePool: Song[],
  preferredLanguages: string[] = ['tamil'],
  maxItems = 25
): Song[] {
  if (!target) return candidatePool

  const normLangs = preferredLanguages.length > 0 ? preferredLanguages : ['tamil']

  // Filter out candidates that violate language or are the exact target
  const otherCandidates = candidatePool.filter(
    c => c.videoId !== target.videoId && isSongInLanguage(c, normLangs)
  )

  // Sort candidates by descending relevance score and only keep positive synergies
  const scored = otherCandidates
    .map(c => ({
      song: c,
      score: scoreSongRelevance(target, c, normLangs)
    }))
    .filter(item => item.score > 0)
    .sort((a, b) => b.score - a.score)

  const relevantSongs = scored.map(s => s.song)
  return [target, ...relevantSongs].slice(0, maxItems)
}
