/**
 * ISAI Centralized Keyword Master Dictionary (TypeScript / Shared)
 * Supports English, Tamil script, Tanglish, synonyms, and categories.
 */

export interface KeywordEntry {
  keyword: string
  synonyms: string[]
  language: string
  category: string
  subCategory: string
  tags: string[]
  priority: number
}

export const CAT_TRACK = 'track_music'
export const CAT_MOOD = 'mood'
export const CAT_TAMIL_MOOD = 'tamil_mood'
export const CAT_LOVE_ROMANCE = 'love_romance'
export const CAT_SAD_EMOTIONAL = 'sad_emotional'
export const CAT_GENRE = 'genre'
export const CAT_TAMIL_GENRE = 'tamil_genre'
export const CAT_ACTIVITY = 'activity_energy'
export const CAT_DEVOTIONAL = 'devotional'
export const CAT_LANGUAGE = 'language'
export const CAT_ARTIST = 'artist'
export const CAT_MOVIE = 'movie'
export const CAT_ALBUM = 'album'
export const CAT_YEAR_ERA = 'year_era'
export const CAT_RELEASE = 'release_status'
export const CAT_TRENDING = 'trending_popular'
export const CAT_CLASSIC = 'old_classic'
export const CAT_FESTIVAL = 'festival_occasion'
export const CAT_RELATIONSHIP = 'relationship'
export const CAT_SITUATION = 'situation'
export const CAT_WEATHER = 'weather_nature'

export const MASTER_KEYWORD_DICTIONARY: KeywordEntry[] = [
  // 1. Song / Track Search
  {
    keyword: 'song',
    synonyms: ['songs', 'track', 'tracks', 'music', 'paatu', 'paattu', 'பாட்டு', 'பாடல்', 'isai', 'இசை', 'paadal', 'padal'],
    language: 'Tamil/Global',
    category: CAT_TRACK,
    subCategory: 'track',
    tags: ['music', 'song', 'audio', 'paatu'],
    priority: 10
  },
  {
    keyword: 'hit songs',
    synonyms: ['hits', 'super hit', 'super hits', 'super hit songs', 'top songs', 'best songs', 'favorite songs', 'popular songs'],
    language: 'Global',
    category: CAT_TRACK,
    subCategory: 'hits',
    tags: ['top', 'hits', 'best'],
    priority: 9
  },

  // 2, 3, 4. Mood & Love / Romance
  {
    keyword: 'love',
    synonyms: [
      'romantic', 'romance', 'romantic songs', 'love songs', 'love melody', 'romantic melody',
      'kadhal', 'kaadhal', 'kathal', 'காதல்', 'kadhal paatu', 'kaadhal paatu',
      'காதல் பாடல்கள்', 'kadhal songs', 'love paatu', 'kaadhal melody'
    ],
    language: 'Tamil/Global',
    category: CAT_LOVE_ROMANCE,
    subCategory: 'romantic',
    tags: ['love', 'romantic', 'romance', 'kadhal', 'couple', 'feel good'],
    priority: 10
  },
  {
    keyword: 'first love',
    synonyms: ['true love', 'mudhal kadhal', 'first crush', 'mudhal paarvai'],
    language: 'Tamil/Global',
    category: CAT_LOVE_ROMANCE,
    subCategory: 'first_love',
    tags: ['love', 'nostalgia', 'crush'],
    priority: 8
  },
  {
    keyword: 'one sided love',
    synonyms: ['one side love', 'oruthalai kadhal', 'oru thalai kadhal', 'ஒருதலை காதல்'],
    language: 'Tamil',
    category: CAT_LOVE_ROMANCE,
    subCategory: 'one_sided',
    tags: ['love', 'sad', 'longing'],
    priority: 8
  },
  {
    keyword: 'proposal',
    synonyms: ['proposal songs', 'propose songs', 'love propose', 'kadhal sollum paatu'],
    language: 'Tamil/Global',
    category: CAT_LOVE_ROMANCE,
    subCategory: 'proposal',
    tags: ['love', 'proposal', 'romance'],
    priority: 7
  },
  {
    keyword: 'couple',
    synonyms: ['couple songs', 'husband wife songs', 'husband songs', 'wife songs', 'marriage songs', 'wedding songs', 'engagement songs', 'kalyana paatu', 'thirumanam'],
    language: 'Tamil/Global',
    category: CAT_LOVE_ROMANCE,
    subCategory: 'wedding_couple',
    tags: ['couple', 'wedding', 'marriage', 'family'],
    priority: 8
  },
  {
    keyword: 'happy',
    synonyms: ['joyful', 'fun', 'celebration', 'feel good', 'positive', 'magizhchi', 'மகிழ்ச்சி', 'santhosham', 'sandhosham', 'சந்தோஷம்', 'kushi'],
    language: 'Tamil/Global',
    category: CAT_MOOD,
    subCategory: 'happy',
    tags: ['joy', 'upbeat', 'celebration', 'happy'],
    priority: 9
  },
  {
    keyword: 'peaceful',
    synonyms: ['calm', 'relaxing', 'chill', 'stress relief', 'mind relax', 'meditation', 'nimmathi', 'amaidhi', 'அமைதி'],
    language: 'Tamil/Global',
    category: CAT_MOOD,
    subCategory: 'peaceful',
    tags: ['calm', 'chill', 'peaceful', 'relax'],
    priority: 8
  },
  {
    keyword: 'energetic',
    synonyms: ['motivational', 'inspirational', 'energy', 'pump up', 'hope', 'nambikkai', 'verithanam', 'mass'],
    language: 'Tamil/Global',
    category: CAT_MOOD,
    subCategory: 'energetic',
    tags: ['energy', 'motivation', 'inspire', 'workout'],
    priority: 8
  },
  {
    keyword: 'nostalgic',
    synonyms: ['nostalgia', 'memories', 'ninaivu', 'நினைவு', 'sweet memories', 'golden days', 'childhood'],
    language: 'Tamil/Global',
    category: CAT_MOOD,
    subCategory: 'nostalgic',
    tags: ['nostalgia', 'memories', 'retro'],
    priority: 8
  },

  // 5. Sad / Emotional
  {
    keyword: 'sad',
    synonyms: [
      'sad songs', 'sad melody', 'emotional', 'emotional songs', 'pain', 'pain songs',
      'heartbreak', 'heartbreak songs', 'crying songs', 'deep emotional', 'sogam', 'சோகம்',
      'sogam songs', 'sogamana paatu', 'சோக பாடல்கள்', 'thunbam', 'துன்பம்', 'valigal'
    ],
    language: 'Tamil/Global',
    category: CAT_SAD_EMOTIONAL,
    subCategory: 'sad',
    tags: ['sad', 'pain', 'emotional', 'sogam', 'heartbreak'],
    priority: 10
  },
  {
    keyword: 'breakup',
    synonyms: ['breakup songs', 'love failure', 'love failure songs', 'heartbreak', 'kaadhal tholvi', 'kadhal tholvi', 'காதல் தோல்வி'],
    language: 'Tamil/Global',
    category: CAT_SAD_EMOTIONAL,
    subCategory: 'breakup',
    tags: ['breakup', 'love failure', 'sad', 'pain'],
    priority: 9
  },
  {
    keyword: 'lonely',
    synonyms: ['lonely songs', 'missing songs', 'missing someone', 'thanimai', 'தனிமை', 'alone', 'alone songs'],
    language: 'Tamil/Global',
    category: CAT_SAD_EMOTIONAL,
    subCategory: 'lonely',
    tags: ['lonely', 'alone', 'thanimai', 'night'],
    priority: 8
  },
  {
    keyword: 'pirivu',
    synonyms: ['separation', 'separation songs', 'pirivu paatu', 'பிரிவு', 'பிரிவு பாடல்கள்', 'yaekkam', 'yekkam', 'ஏக்கம்'],
    language: 'Tamil',
    category: CAT_SAD_EMOTIONAL,
    subCategory: 'separation',
    tags: ['separation', 'longing', 'pirivu', 'distance'],
    priority: 8
  },

  // 6 & 7. Music Genres & Tamil Music Types
  {
    keyword: 'melody',
    synonyms: ['melodies', 'melody songs', 'soft melody', 'tamil melody', 'மெலடி', 'மெலடி பாடல்கள்', 'soulful'],
    language: 'Tamil/Global',
    category: CAT_GENRE,
    subCategory: 'melody',
    tags: ['melody', 'soft', 'pleasant', 'relax'],
    priority: 10
  },
  {
    keyword: 'kuthu',
    synonyms: ['tamil kuthu', 'dappankuthu', 'kuthu songs', 'fast beat', 'dance beat', 'குத்து பாடல்கள்', 'dappan kuthu', 'thara local'],
    language: 'Tamil',
    category: CAT_TAMIL_GENRE,
    subCategory: 'kuthu',
    tags: ['kuthu', 'dance', 'fast', 'party', 'local'],
    priority: 10
  },
  {
    keyword: 'gaana',
    synonyms: ['gana', 'gana songs', 'tamil gaana', 'marana gana', 'local gana', 'chennai gana', 'கானா பாடல்கள்', 'gana paatu'],
    language: 'Tamil',
    category: CAT_TAMIL_GENRE,
    subCategory: 'gaana',
    tags: ['gaana', 'gana', 'chennai', 'local', 'folk'],
    priority: 10
  },
  {
    keyword: 'folk',
    synonyms: ['tamil folk', 'village songs', 'tamil village songs', 'nattupura paadal', 'nattuppura paatu', 'நாட்டுப்புற பாடல்கள்', 'gramathu paadal'],
    language: 'Tamil',
    category: CAT_TAMIL_GENRE,
    subCategory: 'folk',
    tags: ['folk', 'village', 'traditional', 'gramathu'],
    priority: 9
  },
  {
    keyword: 'lofi',
    synonyms: ['lo-fi', 'lofi remix', 'lofi beats', 'lo-fi chill', 'slowed and reverb', 'chill lofi'],
    language: 'Global',
    category: CAT_GENRE,
    subCategory: 'lofi',
    tags: ['lofi', 'chill', 'slowed', 'reverb', 'night'],
    priority: 9
  },
  {
    keyword: 'rap',
    synonyms: ['hip hop', 'tamil rap', 'hiphop', 'tamil hip hop', 'desi rap', 'street rap'],
    language: 'Tamil/Global',
    category: CAT_GENRE,
    subCategory: 'hip_hop',
    tags: ['rap', 'hip hop', 'beats', 'rhymes'],
    priority: 8
  },
  {
    keyword: 'acoustic',
    synonyms: ['unplugged', 'guitar', 'piano', 'acoustic cover', 'stripped version'],
    language: 'Global',
    category: CAT_GENRE,
    subCategory: 'acoustic',
    tags: ['acoustic', 'guitar', 'unplugged'],
    priority: 8
  },
  {
    keyword: 'instrumental',
    synonyms: ['orchestral', 'ambient', 'bgm', 'theme music', 'movie bgm', 'flute', 'violin'],
    language: 'Global',
    category: CAT_GENRE,
    subCategory: 'instrumental',
    tags: ['instrumental', 'bgm', 'theme', 'ambient'],
    priority: 8
  },
  {
    keyword: 'remix',
    synonyms: ['mashup', 'dj', 'dj remix', 'club mix', 'dance mix', 'party remix', 'bass boosted'],
    language: 'Global',
    category: CAT_GENRE,
    subCategory: 'remix',
    tags: ['remix', 'dj', 'mashup', 'club', 'dance'],
    priority: 8
  },

  // 8 & 20. Energy / Activity / Situation
  {
    keyword: 'workout',
    synonyms: ['gym', 'gym songs', 'workout songs', 'fitness', 'running', 'walking', 'exercise', 'cardio', 'pump up'],
    language: 'Global',
    category: CAT_ACTIVITY,
    subCategory: 'workout',
    tags: ['gym', 'workout', 'fitness', 'energy', 'motivation'],
    priority: 9
  },
  {
    keyword: 'travel',
    synonyms: ['driving', 'driving songs', 'travel songs', 'road trip', 'road trip songs', 'bike ride', 'bike ride songs', 'car drive', 'highway'],
    language: 'Global',
    category: CAT_ACTIVITY,
    subCategory: 'travel',
    tags: ['travel', 'driving', 'trip', 'journey', 'highway'],
    priority: 9
  },
  {
    keyword: 'sleep',
    synonyms: ['sleep songs', 'sleeping music', 'bedtime', 'deep sleep', 'lullaby', 'thaalattu', 'தாலாட்டு'],
    language: 'Global',
    category: CAT_ACTIVITY,
    subCategory: 'sleep',
    tags: ['sleep', 'calm', 'relax', 'lullaby'],
    priority: 8
  },
  {
    keyword: 'party',
    synonyms: ['party songs', 'dance', 'dance songs', 'celebration', 'club', 'weekend party'],
    language: 'Global',
    category: CAT_ACTIVITY,
    subCategory: 'party',
    tags: ['party', 'dance', 'celebration', 'kuthu'],
    priority: 9
  },

  // 9. Devotional
  {
    keyword: 'devotional',
    synonyms: ['devotional songs', 'bhakti', 'bhakti songs', 'god songs', 'spiritual', 'temple songs', 'prayer songs', 'tamil devotional'],
    language: 'Tamil/Global',
    category: CAT_DEVOTIONAL,
    subCategory: 'general_devotional',
    tags: ['devotional', 'bhakti', 'spiritual', 'god', 'prayer'],
    priority: 10
  },
  {
    keyword: 'murugan',
    synonyms: ['murugan songs', 'murugan devotional', 'kandan', 'vel', 'thaipusam', 'kanda sashti kavasam', 'முருகன் பாடல்கள்', 'முருகன்'],
    language: 'Tamil',
    category: CAT_DEVOTIONAL,
    subCategory: 'murugan',
    tags: ['murugan', 'kandhan', 'lord murugan', 'devotional'],
    priority: 9
  },
  {
    keyword: 'shiva',
    synonyms: ['shivan', 'shiva songs', 'shivan songs', 'om namah shivaya', 'mahadev', 'sivan paatu'],
    language: 'Tamil/Global',
    category: CAT_DEVOTIONAL,
    subCategory: 'shiva',
    tags: ['shiva', 'sivan', 'mahadev', 'devotional'],
    priority: 9
  },

  // 10. Language Keywords
  {
    keyword: 'tamil',
    synonyms: ['tamil songs', 'tamil music', 'தமிழ்', 'தமிழ் பாடல்கள்', 'tamil paatu', 'kollywood'],
    language: 'Tamil',
    category: CAT_LANGUAGE,
    subCategory: 'tamil',
    tags: ['tamil', 'kollywood', 'south'],
    priority: 10
  },
  {
    keyword: 'telugu',
    synonyms: ['telugu songs', 'telugu music', 'tollywood'],
    language: 'Telugu',
    category: CAT_LANGUAGE,
    subCategory: 'telugu',
    tags: ['telugu', 'tollywood', 'south'],
    priority: 9
  },
  {
    keyword: 'hindi',
    synonyms: ['hindi songs', 'hindi music', 'bollywood', 'bollywood songs'],
    language: 'Hindi',
    category: CAT_LANGUAGE,
    subCategory: 'hindi',
    tags: ['hindi', 'bollywood', 'north'],
    priority: 9
  },
  {
    keyword: 'malayalam',
    synonyms: ['malayalam songs', 'malayalam music', 'mollywood'],
    language: 'Malayalam',
    category: CAT_LANGUAGE,
    subCategory: 'malayalam',
    tags: ['malayalam', 'mollywood', 'south'],
    priority: 9
  },
  {
    keyword: 'english',
    synonyms: ['english songs', 'english music', 'hollywood songs', 'western songs', 'global hits'],
    language: 'English',
    category: CAT_LANGUAGE,
    subCategory: 'english',
    tags: ['english', 'global', 'pop'],
    priority: 8
  },

  // 11. Top Artists
  {
    keyword: 'anirudh',
    synonyms: ['anirudh ravichander', 'aniruth', 'ani', 'rockstar anirudh'],
    language: 'Tamil',
    category: CAT_ARTIST,
    subCategory: 'composer',
    tags: ['anirudh', 'composer', 'youth', 'mass'],
    priority: 10
  },
  {
    keyword: 'ar rahman',
    synonyms: ['a r rahman', 'a.r. rahman', 'rahman', 'arr', 'isaipuyal'],
    language: 'Tamil/Global',
    category: CAT_ARTIST,
    subCategory: 'composer',
    tags: ['ar rahman', 'composer', 'legend', 'melody'],
    priority: 10
  },
  {
    keyword: 'ilaiyaraaja',
    synonyms: ['ilayaraja', 'ilaiyaraja', 'isaignani', 'raaja sir'],
    language: 'Tamil',
    category: CAT_ARTIST,
    subCategory: 'composer',
    tags: ['ilayaraja', 'composer', 'maestro', 'classic'],
    priority: 10
  },
  {
    keyword: 'yuvan',
    synonyms: ['yuvan shankar raja', 'u1', 'yuvan hits', 'yuvan drugs'],
    language: 'Tamil',
    category: CAT_ARTIST,
    subCategory: 'composer',
    tags: ['yuvan', 'composer', 'sad', 'melody'],
    priority: 10
  },
  {
    keyword: 'harris jayaraj',
    synonyms: ['harris', 'harris jayaraj hits', 'hj'],
    language: 'Tamil',
    category: CAT_ARTIST,
    subCategory: 'composer',
    tags: ['harris', 'composer', 'melody', 'love'],
    priority: 9
  },
  {
    keyword: 'sid sriram',
    synonyms: ['sid', 'sidsriram', 'sid sriram hits'],
    language: 'Tamil/Telugu',
    category: CAT_ARTIST,
    subCategory: 'singer',
    tags: ['sid sriram', 'singer', 'melody'],
    priority: 9
  },

  // 14. Year & Decade
  {
    keyword: '2026',
    synonyms: ['2026 songs', 'tamil songs 2026', '2026 hits', 'latest 2026'],
    language: 'Global',
    category: CAT_YEAR_ERA,
    subCategory: 'current_year',
    tags: ['2026', 'new', 'fresh'],
    priority: 10
  },
  {
    keyword: '2025',
    synonyms: ['2025 songs', 'tamil songs 2025', '2025 hits'],
    language: 'Global',
    category: CAT_YEAR_ERA,
    subCategory: 'recent_year',
    tags: ['2025', 'recent'],
    priority: 9
  },
  {
    keyword: '90s',
    synonyms: ['90s songs', '90s tamil songs', '1990s', '1990s tamil songs', '90s hits', '90s melody'],
    language: 'Tamil/Global',
    category: CAT_YEAR_ERA,
    subCategory: 'decade',
    tags: ['90s', 'golden era', 'rahman', 'ilayaraja'],
    priority: 10
  },
  {
    keyword: '80s',
    synonyms: ['80s songs', '80s tamil songs', '1980s', '80s hits', '80s melody'],
    language: 'Tamil/Global',
    category: CAT_YEAR_ERA,
    subCategory: 'decade',
    tags: ['80s', 'ilayaraja', 'spb'],
    priority: 9
  },
  {
    keyword: '2000s',
    synonyms: ['2000s tamil songs', '2000s hits', '2k hits', '2000s melody'],
    language: 'Tamil/Global',
    category: CAT_YEAR_ERA,
    subCategory: 'decade',
    tags: ['2000s', 'harris', 'yuvan'],
    priority: 9
  },

  // 15, 16, 17. Release Status, Trending, Old
  {
    keyword: 'latest',
    synonyms: ['new', 'newest', 'recent', 'new release', 'new releases', 'latest release', 'just released', 'latest tamil songs', 'puthiya paadal', 'புதிய பாடல்கள்'],
    language: 'Global',
    category: CAT_RELEASE,
    subCategory: 'new_releases',
    tags: ['latest', 'new', 'trending'],
    priority: 10
  },
  {
    keyword: 'trending',
    synonyms: ['trending songs', 'viral', 'viral songs', 'popular', 'popular songs', 'top songs', 'top hits', 'chartbusters'],
    language: 'Global',
    category: CAT_TRENDING,
    subCategory: 'viral_trends',
    tags: ['trending', 'viral', 'top'],
    priority: 10
  },
  {
    keyword: 'old songs',
    synonyms: ['old tamil songs', 'classic songs', 'classical hits', 'evergreen', 'evergreen songs', 'retro', 'golden hits', 'pazhaya paadal', 'பழைய பாடல்கள்'],
    language: 'Tamil/Global',
    category: CAT_CLASSIC,
    subCategory: 'evergreen',
    tags: ['old', 'classic', 'evergreen', 'retro'],
    priority: 10
  },

  // 19. Relationships
  {
    keyword: 'friendship',
    synonyms: ['friends', 'best friend', 'friendship songs', 'nanban', 'நண்பன்', 'thozhi', 'தோழி', 'natpu', 'நட்பு', 'natpu paadal'],
    language: 'Tamil/Global',
    category: CAT_RELATIONSHIP,
    subCategory: 'friends',
    tags: ['friendship', 'natpu', 'friends'],
    priority: 9
  },
  {
    keyword: 'mother',
    synonyms: ['mother songs', 'amma', 'amma songs', 'அம்மா', 'அம்மா பாடல்கள்', 'thaai'],
    language: 'Tamil/Global',
    category: CAT_RELATIONSHIP,
    subCategory: 'mother',
    tags: ['mother', 'amma', 'family', 'paasam'],
    priority: 9
  },
  {
    keyword: 'father',
    synonyms: ['father songs', 'appa', 'appa songs', 'அப்பா', 'அப்பா பாடல்கள்'],
    language: 'Tamil/Global',
    category: CAT_RELATIONSHIP,
    subCategory: 'father',
    tags: ['father', 'appa', 'family'],
    priority: 9
  },

  // 21. Weather / Nature
  {
    keyword: 'rain',
    synonyms: ['rain songs', 'rainy songs', 'mazhai', 'மழை', 'mazhai songs', 'rain melody'],
    language: 'Tamil/Global',
    category: CAT_WEATHER,
    subCategory: 'rain',
    tags: ['rain', 'mazhai', 'weather', 'monsoon'],
    priority: 9
  }
]

const synonymMap = new Map<string, KeywordEntry>()
for (const entry of MASTER_KEYWORD_DICTIONARY) {
  synonymMap.set(entry.keyword.toLowerCase().trim(), entry)
  for (const syn of entry.synonyms) {
    synonymMap.set(syn.toLowerCase().trim(), entry)
  }
}

export function findKeywordEntry(term: string): KeywordEntry | undefined {
  return synonymMap.get(term.toLowerCase().trim())
}
