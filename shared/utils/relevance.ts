import { detectSongLanguage } from '../api/music-api'
import type { Song } from '../models/song'

/** Keep this file in the original helper location so the Song import resolves.
 * Map real provider metadata to these optional fields, or pass getMetadata.
 * Unknown languages are excluded from automatic recommendations by default.
 * The explicitly chosen target remains first, regardless of its metadata.
 * Classification is a conservative text heuristic, not audio analysis.
 */
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
  'kadhalan',
  'kadhale',
  'kadhalum',
  'kadhalaada',
  'kadhalae',
  'romance',
  'romantic',
  'soul',
  'feel good',
  'feel',
  'heart',
  'sweet',
  'kanave',
  'kanavugal',
  'nenjukkul',
  'nenjam',
  'nenjame',
  'nenjinile',
  'nenjukulla',
  'vaseegara',
  'munbe vaa',
  'thalli pogathey',
  'megham karukatha',
  'kannazhaga',
  'poove',
  'poovukku',
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
  'uyire',
  'aaruyire',
  'kannana',
  'kannaana',
  'katchi sera',
  'unakkul',
  'unakkul naane',
  'pirai',
  'enodu',
  'ennodu',
  'poove sempoove',
  'ennodu nee irundhal',
  'unakkenna venum sollu',
  'kadhale kadhale',
  'kannamma',
  'anbe',
  'anbae',
  'thaen thaen',
  'mayakkama',
  'un perai solla',
  'suttum vizhi',
  'oru dheyvam thantha',
  'pookkal pookkum',
  'pookal pookum',
  'hosanna',
  'en jeevan',
  'new york nagaram',
  'anbil',
  'anbil avan',
  'innum konjam neram',
  'maruvaarthai',
  'un vizhigalil',
  'kannaana kanney',
  'kanave kanave',
  'neeyum naanum',
  'avalum naanum',
  'usure',
  'usuru',
  'usure poguthey',
  'nira',
  'vizhi moodi',
  'partha mudhal',
  'paartha mudhal',
  'venmathi',
  'sirimathii',
  'yelo pullelo',
  'prema',
  'premam',
  'priya',
  'priyathama',
  'sakhi',
  'ninnila',
  'samajavaragamana',
  'geetha govindam',
  'chuttamalle',
  'valayapatti',
  'thangamey',
  'sirikkadhey',
  'po indru neeyaga',
  'ey inge paaru',
  'oh penne',
  'bae',
  'spark',
  'mella mella',
  'aagayam',
  'mudhal',
  'mudhal mazhai',
  'anbae peranbae',
  'nenaithu nenaithu',
  'en navel',
  'adiye',
  'hasili fisili',
  'pennie',
  'penne',
  'yaakai',
  'vizhi',
  'kannil',
  'kaatru',
  'kaatrukkenna',
  'malare',
  'mallipoo',
  'thooriga',
  'gundu malli',
  'dada',
  'sita ramam',
  'lover',
  'joe',
  'vtv',
  'vaaranam aayiram',
  'raja rani',
  'neethaane',
  'nanban',
  'chellamma',
  'siragugal',
  'pudhu vellai mazhai',
  'chinna chinna asai',
  'malare ninne',
  'darshana',
  'hridayam',
  'enathaney',
  'omahana',
  'azhage',
  'azhagiye',
  'orasaadha',
  'high on love',
  'kadhaippoma',
  'bodhaikaname',
  'marandaye',
  'parayuvaan',
  'aathangara marame',
  'senthoora',
  'yeno yeno',
  'kaatrae en kaatrae',
  'yaaro',
  'yaro',
  'thentral',
  'thendral',
  'pesum',
  'mounam',
  'kavidhai',
  'kavithai',
  'rasathi',
  'pesadha',
  'kannukulle',
  'pala palakurakkum',
  'kandaangi',
  'ishq',
  'mohabbat',
  'tum hi ho',
  'pehle bhi main',
  'satranga',
  'tera',
  'meri',
  'deewana',
  'pyaar',
  'dil'
]

const MELODY_ARTISTS = [
  'sid sriram',
  'pradeep kumar',
  'shreya ghoshal',
  'chinmayi',
  'bombay jayashri',
  'haricharan',
  'karthik',
  'vijay prakash',
  'swarnalatha',
  's.p. balasubrahmanyam',
  'spb',
  's. janaki',
  'janaki',
  'chithra',
  'k.s. chithra',
  'ks chithra',
  'saindhavi',
  'kapil kapilan',
  'hesham abdul wahab',
  'stephen zechariah',
  'dhibu ninan thomas',
  'jonita gandhi',
  'shakthisree gopalan',
  'unni menon',
  'sujatha',
  'sadhana sargam',
  'tippu',
  'andrea jeremiah',
  'sithara'
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
  'soup Songs',
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

export interface RecommendationMetadata {
  language?: string | string[]
  artist?: string | string[]
  albumId?: string
  provider?: string
  id?: string
  mood?: SongMood // Only supply a trusted tag, not a guessed artist genre.
}
export interface RecommendationOptions {
  getMetadata?: (song: Song) => RecommendationMetadata
  allowUnknownLanguage?: boolean // Defaults to false; enabling weakens language filtering.
}

export function normalizeText(text?: string): string {
  return (text ?? '')
    .normalize('NFKC')
    .toLowerCase()
    .replaceAll(/[^\p{L}\p{M}\p{N}\s]/gu, ' ')
    .replaceAll(/\s+/g, ' ')
    .trim()
}
function strings(value: unknown): string[] {
  return (Array.isArray(value) ? value : [value]).filter(
    (item): item is string => typeof item === 'string' && !!item.trim()
  )
}
function metadata(song: Song, options: RecommendationOptions): RecommendationMetadata {
  if (options.getMetadata) return options.getMetadata(song)
  // Deliberately read only simple fields. Provider-specific nested artists etc.
  // must be mapped by getMetadata; do not guess their structure.
  const data = song as unknown as Record<string, unknown>
  return {
    language: strings(data.language),
    artist: strings(data.artist),
    id: typeof data.id === 'string' ? data.id : undefined,
    provider: typeof data.provider === 'string' ? data.provider : undefined,
    albumId: typeof data.albumId === 'string' ? data.albumId : undefined
  }
}
const LANGUAGE_ALIASES: Record<string, string> = {
  ta: 'tamil',
  tam: 'tamil',
  tamil: 'tamil',
  தமிழ்: 'tamil',
  te: 'telugu',
  tel: 'telugu',
  telugu: 'telugu',
  తెలుగు: 'telugu',
  hi: 'hindi',
  hin: 'hindi',
  hindi: 'hindi',
  हिन्दी: 'hindi',
  हिंदी: 'hindi',
  ml: 'malayalam',
  mal: 'malayalam',
  malayalam: 'malayalam',
  kn: 'kannada',
  kan: 'kannada',
  kannada: 'kannada',
  en: 'english',
  eng: 'english',
  english: 'english',
  pa: 'punjabi',
  pan: 'punjabi',
  punjabi: 'punjabi',
  bn: 'bengali',
  ben: 'bengali',
  bengali: 'bengali',
  mr: 'marathi',
  mar: 'marathi',
  marathi: 'marathi',
  gu: 'gujarati',
  guj: 'gujarati',
  gujarati: 'gujarati',
  bho: 'bhojpuri',
  bhojpuri: 'bhojpuri'
}
function languageKey(value: string): string {
  const key = value.trim().toLowerCase().replaceAll('_', '-')
  if (key === 'tamil' || NON_TAMIL_LANGUAGES.includes(key)) return key
  return LANGUAGE_ALIASES[key] ?? LANGUAGE_ALIASES[key.split('-')[0]] ?? key
}
function preferences(values: string[]): string[] {
  const result = values.map(languageKey).filter(Boolean)
  return result.length ? [...new Set(result)] : ['tamil']
}
export function isSongInLanguage(
  song: Song,
  preferredLanguages: string[] = ['tamil'],
  options: RecommendationOptions = {}
): boolean {
  if (!song) return false
  const selected = preferences(preferredLanguages)
  if (selected.length === 0) return true

  // 1. Explicit language on song or metadata
  const known = strings([song.language, ...strings(metadata(song, options).language)])
    .map(languageKey)
    .filter((lang) => !['unknown', 'und', 'undefined', 'null'].includes(lang))

  if (known.length > 0) {
    return known.some((lang) => selected.includes(lang))
  }

  // 2. Dynamic language detection via title & artist heuristics
  const detected = detectSongLanguage(song)
  if (detected && !['unknown', 'und', 'undefined', 'null'].includes(detected)) {
    const detectedKey = languageKey(detected)
    return selected.includes(detectedKey)
  }

  // 3. Fallback: check if title or channel explicitly mentions another non-preferred language
  const titleText = `${song.title || ''} ${song.channelTitle || ''}`.toLowerCase()
  const nonPreferred = NON_TAMIL_LANGUAGES.filter((l) => !selected.includes(l))
  for (const np of nonPreferred) {
    if (new RegExp(`\\b${np}\\b`, 'i').test(titleText)) {
      return false
    }
  }

  if (selected.includes('tamil')) {
    const hasNonTamilTrace =
      /t-series|tseries|zee music|tips official|yrf|aditya music|lahari music|muzik247|manorama music|anand audio|jhankar music|arijit|pritam|badshah|thaman|devi sri prasad|\bdsp\b|sushin shyam|dabzee|diljit|sidhu moose|b praak/i.test(
        titleText
      )
    if (hasNonTamilTrace) {
      return false
    }
    return true
  }

  return options.allowUnknownLanguage === true
}

const ARTIST_ALIASES: Record<string, string> = {
  anirudh: 'anirudh ravichander',
  'a r rahman': 'a r rahman',
  'ar rahman': 'a r rahman',
  yuvan: 'yuvan shankar raja',
  u1: 'yuvan shankar raja',
  harris: 'harris jayaraj',
  'gv prakash': 'g v prakash',
  ilayaraja: 'ilaiyaraaja',
  's p balasubrahmanyam': 's p balasubrahmanyam',
  spb: 's p balasubrahmanyam',
  sana: 'santhosh narayanan',
  'thenisai thendral': 'deva',
  tms: 't m soundararajan'
}
export function extractPrimaryArtist(channelOrArtist?: string): string {
  const name = normalizeText(channelOrArtist)
    .replaceAll(/(?:\s+(?:official|topic|vevo|channel))+$/gu, '')
    .trim()
  if (!name || /\b(?:records|music|films|tv|productions)\b/u.test(name)) return ''
  return ARTIST_ALIASES[name] ?? name
}
function artists(song: Song, options: RecommendationOptions): string[] {
  const supplied = strings(metadata(song, options).artist)
  if (supplied.length) return supplied.map(extractPrimaryArtist).filter(Boolean)
  // Accept exact known artist names or Topic channels, never arbitrary uploaders.
  const channel = extractPrimaryArtist(song.channelTitle)
  const known = [
    ...GANA_ARTISTS,
    ...MELODY_ARTISTS,
    ...DEVOTIONAL_ARTISTS,
    ...Object.keys(ARTIST_ALIASES),
    ...Object.values(ARTIST_ALIASES)
  ].map(extractPrimaryArtist)
  if (channel && (known.includes(channel) || /(?:\s|-)+topic\s*$/i.test(song.channelTitle ?? ''))) {
    return [extractPrimaryArtist(song.channelTitle)].filter(Boolean)
  }
  return []
}

/** All original keyword/artist entries are retained above. Only their weights
 * and matching rules change. Album-only words and artist-only hints cannot
 * establish a mood. This remains a heuristic; trusted mood metadata wins.
 */
const RULES: Record<Exclude<SongMood, 'GENERAL'>, readonly string[]> = {
  DEVOTIONAL: [...DEVOTIONAL_KEYWORDS, 'bhakti', 'பக்தி', 'பஜனை', 'கந்த சஷ்டி கவசம்'],
  MOTIVATION_INSPIRING: [...MOTIVATION_KEYWORDS, 'தன்னம்பிக்கை'],
  GANA_FOLK: [...GANA_KEYWORDS, 'கானா', 'நாட்டுப்புற', 'ஒப்பாரி'],
  SAD_HEARTBREAK: [...SAD_KEYWORDS, 'heartbreak', 'சோகம்', 'பிரிவு'],
  INTRO_MASS: INTRO_MASS_KEYWORDS,
  PARTY_KUTHU: [...KUTHU_KEYWORDS, 'குத்து'],
  MELODY_ROMANCE: [...MELODY_KEYWORDS, 'காதல்', 'மெலடி']
}
const ARTIST_HINTS: Partial<Record<SongMood, readonly string[]>> = {
  DEVOTIONAL: DEVOTIONAL_ARTISTS,
  GANA_FOLK: GANA_ARTISTS,
  MELODY_ROMANCE: MELODY_ARTISTS
}
// Preserve these original entries but do not let a broad word or movie title
// independently classify an unrelated track. Album names never drive mood.
const WEAK_KEYWORDS = new Set(
  [
    'hero',
    'entry',
    'hunter',
    'whistle',
    'neruppu',
    'marana',
    'vaathi',
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
    'madura',
    'sandakozhi',
    'paruthiveeran',
    'komban',
    'asuran',
    'soul',
    'feel',
    'heart',
    'sweet',
    'kanave',
    'kanavugal',
    'nenjam',
    'roja',
    'minnale',
    'vinnaithaandi',
    'jeans',
    'alaipayuthey',
    'pirai',
    'priya',
    'bae',
    'spark',
    'mudhal',
    'vizhi',
    'kannil',
    'kaatru',
    'dada',
    'sita ramam',
    'lover',
    'joe',
    'vtv',
    'vaaranam aayiram',
    'raja rani',
    'nanban',
    'hridayam',
    'geetha govindam',
    'premam',
    'tera',
    'meri',
    'dil',
    'local',
    'chilla',
    'machan',
    'taana',
    'pain',
    'confidence',
    'hard work',
    'struggle',
    'success',
    'vetri',
    'vettri',
    'vaazhkai',
    'vip',
    'aarambam',
    'vidumurai',
    'padayappa',
    'baba',
    'anbe sivam',
    'veera',
    'kanavu',
    'vada chennai',
    'soorarai pottru',
    'rise',
    'champion',
    'warrior',
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
    'sairam',
    'sai baba',
    'temple',
    'mahaan'
  ].map(normalizeText)
)
// Resolve original cross-category collisions explicitly, rather than by loop order.
const PHRASE_OVERRIDES: Record<string, SongMood> = {
  'kanave kanave': 'SAD_HEARTBREAK',
  'arabic kuthu': 'PARTY_KUTHU',
  dappankuthu: 'PARTY_KUTHU',
  tasakku: 'PARTY_KUTHU',
  'vaathi coming': 'PARTY_KUTHU'
}
const NORMALIZED_RULES = Object.entries(RULES).map(([mood, phrases]) => ({
  mood: mood as Exclude<SongMood, 'GENERAL'>,
  phrases: [...new Set(phrases.map(normalizeText))]
}))
function containsPhrase(text: string, phrase: string): boolean {
  return !!phrase && ` ${text} `.includes(` ${phrase} `)
}
export function detectSongMood(song: Song, options: RecommendationOptions = {}): SongMood {
  const tagged = metadata(song, options).mood
  if (tagged && (tagged === 'GENERAL' || Object.prototype.hasOwnProperty.call(RULES, tagged))) return tagged
  // Remove an explicitly supplied album from the title as providers often append it.
  // Token boundaries prevent deleting parts of song words.
  const album = normalizeText(song.album)
  let title = normalizeText(song.title)
  if (album) title = ` ${title} `.split(` ${album} `).join(' ').trim()
  const names = artists(song, options)
  const candidates = NORMALIZED_RULES.map((rule) => {
    const matches = rule.phrases.filter((phrase) => containsPhrase(title, phrase))
    let strong = 0
    let weak = 0
    for (const phrase of matches) {
      const override = PHRASE_OVERRIDES[phrase]
      if (override && override !== rule.mood) continue
      if (WEAK_KEYWORDS.has(phrase)) {
        weak = 1
        continue
      }
      const words = phrase.split(' ').length
      strong = Math.max(strong, 10 + words * 5 + (override ? 5 : 0))
    }
    // An override can add a title to its corrected category while retaining originals.
    for (const [phrase, mood] of Object.entries(PHRASE_OVERRIDES)) {
      if (mood === rule.mood && containsPhrase(title, phrase)) {
        strong = Math.max(strong, 15 + phrase.split(' ').length * 5)
      }
    }
    const hints = (ARTIST_HINTS[rule.mood] ?? []).map(extractPrimaryArtist)
    const artistBonus = strong > 0 && names.some((name) => hints.includes(name)) ? 1 : 0
    return { mood: rule.mood, score: strong > 0 ? strong + weak + artistBonus : 0 }
  }).sort((a, b) => b.score - a.score)
  if (candidates[0].score === 0 || candidates[0].score === candidates[1].score) return 'GENERAL'
  return candidates[0].mood
}
function identity(song: Song, options: RecommendationOptions): string | undefined {
  const meta = metadata(song, options)
  if (meta.id?.trim() && meta.provider?.trim()) {
    return JSON.stringify(['provider', meta.provider.trim().toLowerCase(), meta.id.trim()])
  }
  if (typeof song.videoId === 'string' && song.videoId.trim()) {
    return JSON.stringify(['videoId', song.videoId.trim()])
  }
  if (meta.id?.trim()) return JSON.stringify(['id', meta.id.trim()])
  return undefined
}
const COMPATIBILITY: Partial<Record<SongMood, Partial<Record<SongMood, number>>>> = {
  MOTIVATION_INSPIRING: { INTRO_MASS: 35 },
  MELODY_ROMANCE: { SAD_HEARTBREAK: 15 },
  SAD_HEARTBREAK: { MELODY_ROMANCE: 25 },
  PARTY_KUTHU: { GANA_FOLK: 50, INTRO_MASS: 40 },
  GANA_FOLK: { PARTY_KUTHU: 50 },
  INTRO_MASS: { PARTY_KUTHU: 45, MOTIVATION_INSPIRING: 40 }
}

export const KNOWN_FILM_YEARS: Record<string, number> = {
  // 1990s
  roja: 1992,
  gentleman: 1993,
  kadhalan: 1994,
  bombay: 1995,
  baasha: 1995,
  muthu: 1995,
  indian: 1996,
  kadhaldesam: 1996,
  'kadhal desam': 1996,
  ullathaiallitha: 1996,
  suryavamsam: 1997,
  jeans: 1998,
  mudhalvan: 1999,
  padayappa: 1999,
  thulladhamanavamthullum: 1999,
  vaali: 1999,
  sangamam: 1999,
  kandukondain: 2000,
  'kandukondain kandukondain': 2000,
  alaipayuthey: 2000,
  kushi: 2000,
  rhythm: 2000,
  priyamanavale: 2000,

  // 2000 - 2005 (The requested 2002 era window)
  minnale: 2001,
  dhill: 2001,
  dheena: 2001,
  nandha: 2001,
  poovellamunvaasam: 2001,
  'poovellam un vaasam': 2001,
  aanandham: 2001,
  shahjahan: 2001,
  run: 2002,
  gemini: 2002,
  mounampesiyadhe: 2002,
  'mounam pesiyadhe': 2002,
  samurai: 2002,
  baba: 2002,
  panchathanthiram: 2002,
  red: 2002,
  villain: 2002,
  rojakootam: 2002,
  'roja kootam': 2002,
  fivestar: 2002,
  'five star': 2002,
  bagavathi: 2002,
  thamizhan: 2002,
  youth: 2002,
  dhool: 2003,
  saamy: 2003,
  kaakhakaakha: 2003,
  'kaakha kaakha': 2003,
  boys: 2003,
  thirumalai: 2003,
  pithamagan: 2003,
  ghilli: 2004,
  ayuthaezhuthu: 2004,
  'aayutha ezhuthu': 2004,
  vasoolraja: 2004,
  autograph: 2004,
  manmadhan: 2004,
  '7g rainbow colony': 2004,
  '7g': 2004,
  new: 2004,
  madhurey: 2004,
  anniyan: 2005,
  chandramukhi: 2005,
  ghajini: 2005,
  sachein: 2005,
  thirupaachi: 2005,
  'kanda naal mudhal': 2005,

  // 2006 - 2010
  'sillunu oru kadhal': 2006,
  pudhupettai: 2006,
  vettaiyaaduvilaiyaadu: 2006,
  'vettaiyaadu vilaiyaadu': 2006,
  pokkiri: 2007,
  sivaji: 2007,
  billa: 2007,
  paruthiveeran: 2007,
  polladhavan: 2007,
  dasavathaaram: 2008,
  vaaranamaayiram: 2008,
  'vaaranam aayiram': 2008,
  subramaniapuram: 2008,
  ayan: 2009,
  aadhavan: 2009,
  padikathavan: 2009,
  paiyaa: 2010,
  vinnaithaandivaruvaayaa: 2010,
  'vinnaithaandi varuvaayaa': 2010,
  madharasapattinam: 2010,
  enthiran: 2010,
  singam: 2010,

  // 2011 - 2015
  mankatha: 2011,
  'mayakkam enna': 2011,
  '7aum arivu': 2011,
  '3': 2012,
  thuppakki: 2012,
  nanban: 2012,
  rajarani: 2013,
  'raja rani': 2013,
  maryan: 2013,
  'vanakkam chennai': 2013,
  kaththi: 2014,
  vip: 2014,
  jigarthanda: 2014,
  i: 2015,
  vedalam: 2015,
  'naanum rowdy dhaan': 2015,
  'ok kanmani': 2015,

  // 2016 - 2025
  theri: 2016,
  kabali: 2016,
  '24': 2016,
  mersal: 2017,
  'vikram vedha': 2017,
  'kaatru veliyidai': 2017,
  '96': 2018,
  sarkar: 2018,
  'vada chennai': 2018,
  'pyaar prema kaadhal': 2018,
  bigil: 2019,
  petta: 2019,
  viswasam: 2019,
  kaithi: 2019,
  asuran: 2019,
  'soorarai pottru': 2020,
  master: 2021,
  doctor: 2021,
  karnan: 2021,
  vikram: 2022,
  'ponniyin selvan': 2022,
  thiruchitrambalam: 2022,
  beast: 2022,
  'love today': 2022,
  jailer: 2023,
  leo: 2023,
  varisu: 2023,
  chithha: 2023,
  joe: 2023,
  lover: 2024,
  goat: 2024,
  vettaiyan: 2024,
  amaran: 2024
}

export function extractReleaseYear(song: Song): number | null {
  if (!song) return null

  // 1. Explicit year field
  if (song.year !== undefined && song.year !== null && song.year !== '') {
    const parsed = typeof song.year === 'number' ? song.year : Number.parseInt(String(song.year).trim(), 10)
    if (parsed >= 1950 && parsed <= 2030) return parsed
  }

  // 2. Explicit releaseDate
  if (song.releaseDate) {
    const match = String(song.releaseDate).match(/\b(19\d{2}|20[0-2]\d)\b/)
    if (match) {
      const y = Number.parseInt(match[1], 10)
      if (y >= 1950 && y <= 2030) return y
    }
  }

  // 3. Known film landmark lookup
  const cleanTitle = normalizeText(song.title)
  const cleanAlbum = normalizeText(song.album)
  const combined = ` ${cleanAlbum} ${cleanTitle} `

  for (const [film, year] of Object.entries(KNOWN_FILM_YEARS)) {
    const normFilm = normalizeText(film)
    if (normFilm.length >= 2 && combined.includes(` ${normFilm} `)) {
      return year
    }
  }

  // 4. Regex in title or album (e.g. "Run (2002)" or "2002 Tamil Hits")
  const textToScan = `${song.title || ''} ${song.album || ''}`
  const regexMatches = textToScan.matchAll(/\b(19[6-9]\d|20[0-2]\d)\b/g)
  for (const m of regexMatches) {
    const y = Number.parseInt(m[1], 10)
    const idx = m.index ?? -1
    const before = idx > 0 ? textToScan.charAt(idx - 1) : ' '
    const after = idx + 4 < textToScan.length ? textToScan.charAt(idx + 4) : ' '
    if (/[pxk]/i.test(after) || /[pxk]/i.test(before)) continue
    if (y >= 1960 && y <= 2026) return y
  }

  return null
}

export function detectSongEra(song: Song): string | null {
  const y = extractReleaseYear(song)
  if (!y) return null
  if (y < 1980) return '70s'
  if (y < 1990) return '80s'
  if (y < 2000) return '90s'
  if (y < 2010) return '2000s'
  if (y < 2020) return '2010s'
  return '2020s'
}

export const PLAYLIST_EXCLUSION_REGEX =
  /(?:\b|_)(p{1,2}laylist|jukebox|audio jukebox|video jukebox|full album|all songs|songs collection|best of|non stop|nonstop|compilation|mashup|mega mix|megamix|discography|top \d+|hit songs collection|audio songs jukebox|video songs jukebox|evergreen hits jukebox|greatest hits jukebox|all hit songs)(?:\b|_)/i

export function isPlaylistOrCompilation(song: Song): boolean {
  return isPlaylistOrCompilationText(song.title) || isPlaylistOrCompilationText(song.channelTitle)
}

export function isPlaylistOrCompilationText(title?: string): boolean {
  if (!title) return false
  return PLAYLIST_EXCLUSION_REGEX.test(title)
}

export function scoreSongRelevance(
  target: Song,
  candidate: Song,
  preferredLanguages: string[] = ['tamil'],
  options: RecommendationOptions = {},
  sessionSeed?: Song
): number {
  if (isPlaylistOrCompilation(candidate)) return -9999
  const langA = (target.language || detectSongLanguage(target)).toLowerCase().trim()
  const langB = (candidate.language || detectSongLanguage(candidate)).toLowerCase().trim()
  if (langA && langB && langA !== langB) return -999
  const requiredLangs = langA ? [langA] : preferredLanguages
  if (!isSongInLanguage(candidate, requiredLangs, options)) return -999
  const targetId = identity(target, options)
  if (target === candidate || (targetId && targetId === identity(candidate, options))) return -999

  const a = detectSongMood(target, options)
  const b = detectSongMood(candidate, options)

  let score: number
  if (a === 'DEVOTIONAL' || b === 'DEVOTIONAL') {
    if (a === 'DEVOTIONAL' && b === 'DEVOTIONAL') score = 120
    else return -500 // Never mix devotional with romantic/party/other tracks
  } else if (a === b && a !== 'GENERAL') {
    score = 100
  } else if (a === 'GENERAL') {
    score = 10 // Verified-language fallback for unclassified tracks.
  } else {
    score = COMPATIBILITY[a]?.[b] ?? -100
  }

  // If mood is incompatible, reject candidate immediately. Artist similarity must NOT override mood!
  if (score < 0) return score

  // Artist similarity (supports ranking only if mood is already compatible)
  const targetArtists = artists(target, options)
  if (artists(candidate, options).some((artist) => targetArtists.includes(artist))) {
    score += 35
  }

  // Album similarity
  const tm = metadata(target, options)
  const cm = metadata(candidate, options)
  const sameProvider = !!tm.provider?.trim() && tm.provider.trim().toLowerCase() === cm.provider?.trim().toLowerCase()
  const albumA = normalizeText(target.album)
  const albumB = normalizeText(candidate.album)
  const sameAlbum = sameProvider && tm.albumId && cm.albumId ? tm.albumId === cm.albumId : !!albumA && albumA === albumB
  if (sameAlbum) score += 25

  // Era / Year matching
  const yearA = extractReleaseYear(target)
  const yearB = extractReleaseYear(candidate)
  if (yearA && yearB) {
    const diff = Math.abs(yearA - yearB)
    if (diff <= 3) {
      score += 45 // Very close era match (e.g. 2002 and 2001/2003)
    } else if (diff <= 6) {
      score += 30 // Within ±5 years window
    } else if (diff <= 10) {
      score += 15 // Nearby era
    } else if (diff <= 15) {
      score += 0
    } else if (diff <= 20) {
      score -= 30 // Penalize distant era drift
    } else {
      score -= 60 // Heavily de-prioritize distant era (e.g. 2024 track when target is 2002)
    }
  }

  // Session Seed drift prevention during continuous playback
  if (sessionSeed && sessionSeed !== target) {
    const seedMood = detectSongMood(sessionSeed, options)
    if (b !== seedMood && COMPATIBILITY[seedMood]?.[b] === undefined) {
      score -= 80 // Strongly prevent gradual drift away from initial seed mood
    }
    const seedYear = extractReleaseYear(sessionSeed)
    if (seedYear && yearB) {
      const seedDiff = Math.abs(seedYear - yearB)
      if (seedDiff > 12) score -= 25
      if (seedDiff > 20) score -= 50
    }
  }

  return score
}

export function getRelevantSearchQuery(
  song: Song,
  preferredLang = 'tamil',
  options: RecommendationOptions = {}
): string {
  const lang = languageKey(preferredLang) || 'tamil'
  const artist = artists(song, options)[0] ?? ''
  const mood = detectSongMood(song, options)
  const era = detectSongEra(song)
  const eraKeyword = era ? `${era} ` : ''

  const terms: Record<SongMood, string> = {
    MOTIVATION_INSPIRING: `${eraKeyword}motivational inspiring songs`,
    MELODY_ROMANCE: `${eraKeyword}romantic melody love songs`,
    DEVOTIONAL: 'devotional bakthi songs',
    GANA_FOLK: lang === 'tamil' ? `${eraKeyword}gaana folk songs` : `${eraKeyword}folk songs`,
    PARTY_KUTHU: lang === 'tamil' ? `${eraKeyword}party kuthu dance songs` : `${eraKeyword}party dance songs`,
    INTRO_MASS: `${eraKeyword}hero mass entry energetic songs`,
    SAD_HEARTBREAK: `${eraKeyword}sad heartbreak songs`,
    GENERAL: `${eraKeyword}songs`
  }
  return [artist, lang, terms[mood]].filter(Boolean).join(' ')
}

export function buildRelevantQueue(
  target: Song,
  candidatePool: Song[],
  preferredLanguages: string[] = ['tamil'],
  maxItems = 40,
  minItems = 20,
  options: RecommendationOptions = {},
  sessionSeed?: Song
): Song[] {
  if (minItems < 0) return []
  const limit = Number.isFinite(maxItems) ? Math.max(0, Math.floor(maxItems)) : 40
  if (limit === 0 || !target) return []
  const targetId = identity(target, options)
  const normLangs = (preferredLanguages && preferredLanguages.length > 0 ? preferredLanguages : ['tamil']).map((l) =>
    l.toLowerCase().trim()
  )

  const ranked = candidatePool
    .filter((candidate) => {
      if (!candidate || candidate === target) return false
      const id = identity(candidate, options)
      return !!id && id !== targetId
    })
    .map((song, index) => ({
      song,
      index,
      score: scoreSongRelevance(target, song, normLangs, options, sessionSeed)
    }))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score || a.index - b.index)

  const result: Song[] = [target]
  const seen = new Set<string>()
  if (targetId) seen.add(targetId)
  for (const item of ranked) {
    if (result.length >= limit) break
    const id = identity(item.song, options)
    if (!id || seen.has(id)) continue
    seen.add(id)
    result.push(item.song)
  }

  return result
}
