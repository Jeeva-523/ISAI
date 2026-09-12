import type { Song } from '../models/song'

export type SongMood =
  | 'GANA_FOLK'
  | 'INTRO_MASS'
  | 'MELODY_ROMANCE'
  | 'PARTY_KUTHU'
  | 'SAD_HEARTBREAK'
  | 'MOTIVATION_INSPIRING'
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
  'romance',
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
  'pookkal pookkum',
  'hosanna',
  'en jeevan',
  'new york nagaram',
  'anbil',
  'innum konjam neram',
  'maruvaarthai',
  'un vizhigalil',
  'kannaana kanney',
  'kanave kanave',
  'neeyum naanum',
  'avalum naanum'
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
  'thee thalapathy',
  'nakku mukka',
  'tasakku',
  'appa takkaru'
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
  'kannukulla',
  'valikidhu',
  'po nee po',
  'yen ennai pirindhai',
  'idhu varai',
  'yaaro ivan'
]

// Keyword patterns for motivational / inspiring songs
const MOTIVATION_KEYWORDS = [
  'motivation',
  'motivational',
  'inspiring',
  'inspiration',
  'confidence',
  'hard work',
  'struggle',
  'success',
  'vetri',
  'vettri',
  'poradu',
  'saadhithu',
  'saadhikkalaam',
  'nambikkai',
  'kanavugal',
  'uyarvu',
  'valarum',
  'singapenney',
  'ethir neechal',
  'oruvan oruvan',
  'vetri kodi kattu',
  'vaazhkai',
  'velaiyilla pattathari',
  'vip',
  'surviva',
  'neruppu da',
  'believer',
  'unstoppable',
  'hall of fame',
  'aalaporaan thamizhan',
  'aarambam',
  'thunivom',
  'vidumurai',
  'padayappa',
  'baba',
  'anbe sivam',
  'jeithu',
  'jeippom',
  'vijayam',
  'dhillu',
  'veera',
  'porattam',
  'valigalai thaandi',
  'kanavu',
  'ezhunthu vaa',
  'thuninthu nil',
  'nimirndhu nil',
  'unnal mudiyum',
  'vada chennai',
  'soorarai pottru',
  'rise',
  'champion',
  'warrior',
  'anthem'
]

// Keyword patterns for devotional / spiritual bhakti songs
const DEVOTIONAL_KEYWORDS = [
  'devotional',
  'bakthi',
  'bhajan',
  'god',
  'murugan',
  'shivan',
  'siva',
  'ayyappa',
  'ayyappan',
  'vinayagar',
  'ganesha',
  'amman',
  'krishna',
  'perumal',
  'vishnu',
  'venkateshwara',
  'tirupati',
  'hanuman',
  'jesus',
  'allah',
  'kavasam',
  'namavali',
  'suprabhatam',
  'sairam',
  'sai baba',
  'temple',
  'pooja',
  'aarathi',
  'slokam',
  'stotram',
  'mahaan',
  'gayatri mantra',
  'om namah shivaya',
  'harivarasanam'
]

const DEVOTIONAL_ARTISTS = [
  'tms',
  't.m. soundararajan',
  'seerkazhi govindarajan',
  'k.j. yesudas',
  'veeramani',
  'bombay saradha',
  'mahanadhi shobana',
  'l.r. eswari'
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

  // 1. Check Devotional / Spiritual first (must never mix with cinema love/kuthu)
  for (const artist of DEVOTIONAL_ARTISTS) {
    if (text.includes(artist)) return 'DEVOTIONAL'
  }
  for (const kw of DEVOTIONAL_KEYWORDS) {
    if (text.includes(kw)) return 'DEVOTIONAL'
  }

  // 2. Check Motivational / Inspiring (e.g. Vetri Kodi Kattu, Singapenney, Ethir Neechal, VIP, Believer)
  for (const kw of MOTIVATION_KEYWORDS) {
    if (text.includes(kw)) return 'MOTIVATION_INSPIRING'
  }

  // 3. Check Gaana / Folk (Highest priority for local beat songs)
  for (const artist of GANA_ARTISTS) {
    if (text.includes(artist)) return 'GANA_FOLK'
  }
  for (const kw of GANA_KEYWORDS) {
    if (text.includes(kw)) return 'GANA_FOLK'
  }

  // 4. Check Sad / Heartbreak
  for (const kw of SAD_KEYWORDS) {
    if (text.includes(kw)) return 'SAD_HEARTBREAK'
  }

  // 5. Check Intro / Mass
  for (const kw of INTRO_MASS_KEYWORDS) {
    if (text.includes(kw)) return 'INTRO_MASS'
  }

  // 6. Check Party / Kuthu
  for (const kw of KUTHU_KEYWORDS) {
    if (text.includes(kw)) return 'PARTY_KUTHU'
  }

  // 7. Check Melodies / Romantic
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

  // 1. Strict Mood Matching & Filtering
  if (targetMood === candidateMood && targetMood !== 'GENERAL') {
    score += 100 // Exact mood match (Love stays with Love, Motivation with Motivation)
  } else if (targetMood === 'MOTIVATION_INSPIRING') {
    if (candidateMood === 'INTRO_MASS') {
      score += 35 // Mass / Inspiring have energetic synergy
    } else {
      return -100 // NEVER put soft love songs, sad breakup, or gaana into motivation queue
    }
  } else if (targetMood === 'MELODY_ROMANCE') {
    if (candidateMood === 'SAD_HEARTBREAK') {
      score += 20 // Soft romantic melodies and emotional soul tracks share acoustic vibe
    } else {
      return -100 // NEVER put loud party kuthu, mass intro, or gaana into love melody queue
    }
  } else if (targetMood === 'DEVOTIONAL') {
    if (candidateMood === 'DEVOTIONAL') {
      score += 120
    } else {
      return -500 // Strict: Devotional sessions only allow devotional tracks
    }
  } else if (targetMood === 'SAD_HEARTBREAK') {
    if (candidateMood === 'MELODY_ROMANCE') {
      score += 25
    } else {
      return -100
    }
  } else if (targetMood === 'PARTY_KUTHU') {
    if (candidateMood === 'GANA_FOLK') {
      score += 50
    } else if (candidateMood === 'INTRO_MASS') {
      score += 40
    } else {
      return -100
    }
  } else if (targetMood === 'GANA_FOLK') {
    if (candidateMood === 'PARTY_KUTHU') {
      score += 50
    } else {
      return -100
    }
  } else if (targetMood === 'INTRO_MASS') {
    if (candidateMood === 'PARTY_KUTHU') {
      score += 45
    } else if (candidateMood === 'MOTIVATION_INSPIRING') {
      score += 40
    } else {
      return -100
    }
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
    case 'MOTIVATION_INSPIRING':
      return `${lang} motivational inspiring confidence success hit songs ${artistPrefix}`
    case 'MELODY_ROMANCE':
      return `${lang} feel good romantic love melody hit songs ${artistPrefix}`
    case 'DEVOTIONAL':
      return `${lang} devotional bakthi songs ${artistPrefix}temple prayers`
    case 'GANA_FOLK':
      return `${lang} gana songs ${artistPrefix}super hit marana gana kuthu`
    case 'PARTY_KUTHU':
      return `${lang} party kuthu dance celebration songs ${artistPrefix}`
    case 'INTRO_MASS':
      return `${lang} mass hero intro entry hit songs ${artistPrefix}`
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
