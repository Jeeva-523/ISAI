import type { Song } from '../models/song'

/** Keep this file in the original helper location so the Song import resolves.
 * Map real provider metadata to these optional fields, or pass getMetadata.
 * Unknown languages are excluded from automatic recommendations by default.
 * The explicitly chosen target remains first, regardless of its metadata.
 * Classification is a conservative text heuristic, not audio analysis.
 */
export type SongMood =
  | 'GANA_FOLK' | 'INTRO_MASS' | 'MELODY_ROMANCE' | 'PARTY_KUTHU'
  | 'SAD_HEARTBREAK' | 'MOTIVATION_INSPIRING' | 'DEVOTIONAL' | 'GENERAL'

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
  return (text ?? '').normalize('NFKC').toLowerCase()
    .replaceAll(/[^\p{L}\p{M}\p{N}\s]/gu, ' ').replaceAll(/\s+/g, ' ').trim()
}
function strings(value: unknown): string[] {
  return (Array.isArray(value) ? value : [value])
    .filter((item): item is string => typeof item === 'string' && !!item.trim())
}
function metadata(song: Song, options: RecommendationOptions): RecommendationMetadata {
  if (options.getMetadata) return options.getMetadata(song)
  // Deliberately read only simple fields. Provider-specific nested artists etc.
  // must be mapped by getMetadata; do not guess their structure.
  const data = song as unknown as Record<string, unknown>
  return {
    language: strings(data.language), artist: strings(data.artist),
    id: typeof data.id === 'string' ? data.id : undefined,
    provider: typeof data.provider === 'string' ? data.provider : undefined,
    albumId: typeof data.albumId === 'string' ? data.albumId : undefined
  }
}
const LANGUAGE_ALIASES: Record<string, string> = {
  ta: 'tamil', tam: 'tamil', tamil: 'tamil', 'தமிழ்': 'tamil',
  te: 'telugu', tel: 'telugu', telugu: 'telugu', 'తెలుగు': 'telugu',
  hi: 'hindi', hin: 'hindi', hindi: 'hindi', 'हिन्दी': 'hindi', 'हिंदी': 'hindi',
  ml: 'malayalam', mal: 'malayalam', malayalam: 'malayalam',
  kn: 'kannada', kan: 'kannada', kannada: 'kannada',
  en: 'english', eng: 'english', english: 'english',
  pa: 'punjabi', pan: 'punjabi', punjabi: 'punjabi',
  bn: 'bengali', ben: 'bengali', bengali: 'bengali',
  mr: 'marathi', mar: 'marathi', marathi: 'marathi',
  gu: 'gujarati', guj: 'gujarati', gujarati: 'gujarati',
  bho: 'bhojpuri', bhojpuri: 'bhojpuri'
}
function languageKey(value: string): string {
  const key = value.trim().toLowerCase().replaceAll(/_/g, '-')
  if (key === 'tamil' || NON_TAMIL_LANGUAGES.includes(key)) return key
  return LANGUAGE_ALIASES[key] ?? LANGUAGE_ALIASES[key.split('-')[0]] ?? key
}
function preferences(values: string[]): string[] {
  const result = values.map(languageKey).filter(Boolean)
  return result.length ? [...new Set(result)] : ['tamil']
}
export function isSongInLanguage(
  song: Song, preferredLanguages: string[] = ['tamil'], options: RecommendationOptions = {}
): boolean {
  const known = strings(metadata(song, options).language).map(languageKey)
    .filter(lang => !['unknown', 'und', 'undefined', 'null'].includes(lang))
  if (!known.length) return options.allowUnknownLanguage === true
  const selected = preferences(preferredLanguages)
  // A multilingual track is accepted only when all its declared languages are selected.
  return known.every(lang => selected.includes(lang))
}

const ARTIST_ALIASES: Record<string, string> = {
  'anirudh': 'anirudh ravichander', 'a r rahman': 'a r rahman',
  'ar rahman': 'a r rahman', 'yuvan': 'yuvan shankar raja', 'u1': 'yuvan shankar raja',
  'harris': 'harris jayaraj', 'gv prakash': 'g v prakash',
  'ilayaraja': 'ilaiyaraaja', 's p balasubrahmanyam': 's p balasubrahmanyam',
  'spb': 's p balasubrahmanyam', 'sana': 'santhosh narayanan',
  'thenisai thendral': 'deva', 'tms': 't m soundararajan'
}
export function extractPrimaryArtist(channelOrArtist?: string): string {
  const name = normalizeText(channelOrArtist)
    .replaceAll(/(?:\s+(?:official|topic|vevo|channel))+$/gu, '').trim()
  if (!name || /\b(?:records|music|films|tv|productions)\b/u.test(name)) return ''
  return ARTIST_ALIASES[name] ?? name
}
function artists(song: Song, options: RecommendationOptions): string[] {
  const supplied = strings(metadata(song, options).artist)
  if (supplied.length) return supplied.map(extractPrimaryArtist).filter(Boolean)
  // Accept exact known artist names or Topic channels, never arbitrary uploaders.
  const channel = extractPrimaryArtist(song.channelTitle)
  const known = [...GANA_ARTISTS, ...MELODY_ARTISTS, ...DEVOTIONAL_ARTISTS,
  ...Object.keys(ARTIST_ALIASES), ...Object.values(ARTIST_ALIASES)].map(extractPrimaryArtist)
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
const WEAK_KEYWORDS = new Set([
  'hero', 'entry', 'hunter', 'whistle', 'neruppu', 'marana', 'vaathi',
  'leo', 'jailer', 'master', 'petta', 'kabali', 'mersal', 'vikram', 'vettaiyan',
  'kanguva', 'bloody', 'power', 'roar', 'tiger', 'singam', 'attitude', 'anthem', 'theme',
  'madura', 'sandakozhi', 'paruthiveeran', 'komban', 'asuran',
  'soul', 'feel', 'heart', 'sweet', 'kanave', 'kanavugal', 'nenjam', 'roja',
  'minnale', 'vinnaithaandi', 'jeans', 'alaipayuthey', 'pirai', 'priya', 'bae',
  'spark', 'mudhal', 'vizhi', 'kannil', 'kaatru', 'dada', 'sita ramam', 'lover',
  'joe', 'vtv', 'vaaranam aayiram', 'raja rani', 'nanban', 'hridayam',
  'geetha govindam', 'premam', 'tera', 'meri', 'dil',
  'local', 'chilla', 'machan', 'taana', 'pain', 'confidence', 'hard work',
  'struggle', 'success', 'vetri', 'vettri', 'vaazhkai', 'vip', 'aarambam',
  'vidumurai', 'padayappa', 'baba', 'anbe sivam', 'veera', 'kanavu',
  'vada chennai', 'soorarai pottru', 'rise', 'champion', 'warrior',
  'god', 'murugan', 'shivan', 'siva', 'ayyappa', 'ayyappan', 'vinayagar',
  'ganesha', 'amman', 'krishna', 'perumal', 'vishnu', 'venkateshwara',
  'tirupati', 'hanuman', 'jesus', 'allah', 'sairam', 'sai baba', 'temple', 'mahaan'
].map(normalizeText))
// Resolve original cross-category collisions explicitly, rather than by loop order.
const PHRASE_OVERRIDES: Record<string, SongMood> = {
  'kanave kanave': 'SAD_HEARTBREAK',
  'arabic kuthu': 'PARTY_KUTHU',
  'dappankuthu': 'PARTY_KUTHU',
  'tasakku': 'PARTY_KUTHU',
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
  const candidates = NORMALIZED_RULES.map(rule => {
    const matches = rule.phrases.filter(phrase => containsPhrase(title, phrase))
    let strong = 0
    let weak = 0
    for (const phrase of matches) {
      const override = PHRASE_OVERRIDES[phrase]
      if (override && override !== rule.mood) continue
      if (WEAK_KEYWORDS.has(phrase)) { weak = 1; continue }
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
    const artistBonus = strong > 0 && names.some(name => hints.includes(name)) ? 1 : 0
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
  MELODY_ROMANCE: { SAD_HEARTBREAK: 20, GENERAL: 15 },
  SAD_HEARTBREAK: { MELODY_ROMANCE: 25 },
  PARTY_KUTHU: { GANA_FOLK: 50, INTRO_MASS: 40 },
  GANA_FOLK: { PARTY_KUTHU: 50 },
  INTRO_MASS: { PARTY_KUTHU: 45, MOTIVATION_INSPIRING: 40 }
}
export function scoreSongRelevance(
  target: Song, candidate: Song, preferredLanguages: string[] = ['tamil'],
  options: RecommendationOptions = {}
): number {
  if (!isSongInLanguage(candidate, preferredLanguages, options)) return -999
  const targetId = identity(target, options)
  if (target === candidate || (targetId && targetId === identity(candidate, options))) return -999
  const a = detectSongMood(target, options)
  const b = detectSongMood(candidate, options)
  let score: number
  if (a === b && a !== 'GENERAL') score = a === 'DEVOTIONAL' ? 120 : 100
  else if (a === 'DEVOTIONAL' || b === 'DEVOTIONAL') return -500
  else if (a === 'GENERAL') score = 10 // Verified-language fallback for unclassified tracks.
  else score = COMPATIBILITY[a]?.[b] ?? -100
  if (score < 0) return score
  const targetArtists = artists(target, options)
  if (artists(candidate, options).some(artist => targetArtists.includes(artist))) score += 35
  const tm = metadata(target, options)
  const cm = metadata(candidate, options)
  const sameProvider = !!tm.provider?.trim() &&
    tm.provider.trim().toLowerCase() === cm.provider?.trim().toLowerCase()
  const albumA = normalizeText(target.album)
  const albumB = normalizeText(candidate.album)
  const sameAlbum = sameProvider && tm.albumId && cm.albumId
    ? tm.albumId === cm.albumId : !!albumA && albumA === albumB
  if (sameAlbum) score += 25
  return score
}
export function getRelevantSearchQuery(
  song: Song, preferredLang = 'tamil', options: RecommendationOptions = {}
): string {
  const lang = languageKey(preferredLang) || 'tamil'
  const artist = artists(song, options)[0] ?? ''
  const terms: Record<SongMood, string> = {
    MOTIVATION_INSPIRING: 'motivational inspiring songs', MELODY_ROMANCE: 'romantic melody songs',
    DEVOTIONAL: 'devotional songs', GANA_FOLK: lang === 'tamil' ? 'gaana folk songs' : 'folk songs',
    PARTY_KUTHU: lang === 'tamil' ? 'party kuthu dance songs' : 'party dance songs',
    INTRO_MASS: 'hero intro energetic songs', SAD_HEARTBREAK: 'sad heartbreak songs', GENERAL: 'songs'
  }
  return [artist, lang, terms[detectSongMood(song, options)]].filter(Boolean).join(' ')
}
export function buildRelevantQueue(
  target: Song, candidatePool: Song[], preferredLanguages: string[] = ['tamil'],
  maxItems = 25, options: RecommendationOptions = {}
): Song[] {
  const limit = Number.isFinite(maxItems) ? Math.max(0, Math.floor(maxItems)) : 25
  if (limit === 0) return []
  if (!target) return []
  const targetId = identity(target, options)
  const ranked = candidatePool
    .filter(candidate => {
      if (!candidate || candidate === target) return false
      const id = identity(candidate, options)
      return !!id && id !== targetId
    })
    .map((song, index) => ({
      song, index,
      score: scoreSongRelevance(target, song, preferredLanguages, options)
    }))
    .filter(item => item.score > 0)
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
