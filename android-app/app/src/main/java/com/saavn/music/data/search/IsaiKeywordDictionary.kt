package com.saavn.music.data.search

/**
 * Centralized Keyword Master Model for ISAI Music Search
 * Supports English, Tamil script, Tanglish, synonyms, and categories.
 */
data class KeywordEntry(
    val keyword: String,
    val synonyms: List<String>,
    val language: String = "Tamil/Global",
    val category: String,
    val subCategory: String,
    val tags: List<String> = emptyList(),
    val priority: Int = 5
)

object IsaiKeywordDictionary {

    // Category Constants
    const val CAT_TRACK = "track_music"
    const val CAT_MOOD = "mood"
    const val CAT_TAMIL_MOOD = "tamil_mood"
    const val CAT_LOVE_ROMANCE = "love_romance"
    const val CAT_SAD_EMOTIONAL = "sad_emotional"
    const val CAT_GENRE = "genre"
    const val CAT_TAMIL_GENRE = "tamil_genre"
    const val CAT_ACTIVITY = "activity_energy"
    const val CAT_DEVOTIONAL = "devotional"
    const val CAT_LANGUAGE = "language"
    const val CAT_ARTIST = "artist"
    const val CAT_MOVIE = "movie"
    const val CAT_ALBUM = "album"
    const val CAT_YEAR_ERA = "year_era"
    const val CAT_RELEASE = "release_status"
    const val CAT_TRENDING = "trending_popular"
    const val CAT_CLASSIC = "old_classic"
    const val CAT_FESTIVAL = "festival_occasion"
    const val CAT_RELATIONSHIP = "relationship"
    const val CAT_SITUATION = "situation"
    const val CAT_WEATHER = "weather_nature"

    val MASTER_DICTIONARY: List<KeywordEntry> = listOf(
        // ==========================================
        // 1. SONG / TRACK SEARCH
        // ==========================================
        KeywordEntry(
            keyword = "song",
            synonyms = listOf("songs", "track", "tracks", "music", "paatu", "paattu", "பாட்டு", "பாடல்", "isai", "இசை", "paadal", "padal"),
            category = CAT_TRACK,
            subCategory = "track",
            tags = listOf("music", "song", "audio", "paatu"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "hit songs",
            synonyms = listOf("hits", "super hit", "super hits", "super hit songs", "top songs", "best songs", "favorite songs", "popular songs"),
            category = CAT_TRACK,
            subCategory = "hits",
            tags = listOf("top", "hits", "best"),
            priority = 9
        ),

        // ==========================================
        // 2 & 3 & 4. MOOD & LOVE / ROMANCE
        // ==========================================
        KeywordEntry(
            keyword = "love",
            synonyms = listOf("romantic", "romance", "romantic songs", "love songs", "love melody", "romantic melody", "kadhal", "kaadhal", "kathal", "காதல்", "kadhal paatu", "kaadhal paatu", "காதல் பாடல்கள்", "kadhal songs", "love paatu", "kaadhal melody"),
            category = CAT_LOVE_ROMANCE,
            subCategory = "romantic",
            tags = listOf("love", "romantic", "romance", "kadhal", "couple", "feel good"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "first love",
            synonyms = listOf("true love", "mudhal kadhal", "first crush", "mudhal paarvai"),
            category = CAT_LOVE_ROMANCE,
            subCategory = "first_love",
            tags = listOf("love", "nostalgia", "crush"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "one sided love",
            synonyms = listOf("one side love", "oruthalai kadhal", "oru thalai kadhal", "ஒருதலை காதல்"),
            category = CAT_LOVE_ROMANCE,
            subCategory = "one_sided",
            tags = listOf("love", "sad", "longing"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "proposal",
            synonyms = listOf("proposal songs", "propose songs", "love propose", "kadhal sollum paatu"),
            category = CAT_LOVE_ROMANCE,
            subCategory = "proposal",
            tags = listOf("love", "proposal", "romance"),
            priority = 7
        ),
        KeywordEntry(
            keyword = "couple",
            synonyms = listOf("couple songs", "husband wife songs", "husband songs", "wife songs", "marriage songs", "wedding songs", "engagement songs", "kalyana paatu", "thirumanam"),
            category = CAT_LOVE_ROMANCE,
            subCategory = "wedding_couple",
            tags = listOf("couple", "wedding", "marriage", "family"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "happy",
            synonyms = listOf("joyful", "fun", "celebration", "feel good", "positive", "magizhchi", "மகிழ்ச்சி", "santhosham", "sandhosham", "சந்தோஷம்", "kushi"),
            category = CAT_MOOD,
            subCategory = "happy",
            tags = listOf("joy", "upbeat", "celebration", "happy"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "peaceful",
            synonyms = listOf("calm", "relaxing", "chill", "stress relief", "mind relax", "meditation", "nimmathi", "amaidhi", "அமைதி"),
            category = CAT_MOOD,
            subCategory = "peaceful",
            tags = listOf("calm", "chill", "peaceful", "relax"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "energetic",
            synonyms = listOf("motivational", "inspirational", "energy", "pump up", "hope", "nambikkai", "eppothum ungaludan", "verithanam", "mass"),
            category = CAT_MOOD,
            subCategory = "energetic",
            tags = listOf("energy", "motivation", "inspire", "workout"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "nostalgic",
            synonyms = listOf("nostalgia", "memories", "ninaivu", "நினைவு", "sweet memories", "golden days", "childhood"),
            category = CAT_MOOD,
            subCategory = "nostalgic",
            tags = listOf("nostalgia", "memories", "retro"),
            priority = 8
        ),

        // ==========================================
        // 5. SAD / EMOTIONAL & TAMIL SAD MOODS
        // ==========================================
        KeywordEntry(
            keyword = "sad",
            synonyms = listOf("sad songs", "sad melody", "emotional", "emotional songs", "pain", "pain songs", "heartbreak", "heartbreak songs", "crying songs", "deep emotional", "sogam", "சோகம்", "sogam songs", "sogamana paatu", "சோக பாடல்கள்", "thunbam", "துன்பம்", "valigal"),
            category = CAT_SAD_EMOTIONAL,
            subCategory = "sad",
            tags = listOf("sad", "pain", "emotional", "sogam", "heartbreak"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "breakup",
            synonyms = listOf("breakup songs", "love failure", "love failure songs", "heartbreak", "kaadhal tholvi", "kadhal tholvi", "காதல் தோல்வி"),
            category = CAT_SAD_EMOTIONAL,
            subCategory = "breakup",
            tags = listOf("breakup", "love failure", "sad", "pain"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "lonely",
            synonyms = listOf("lonely songs", "missing songs", "missing someone", "thanimai", "தனிமை", "alone", "alone songs"),
            category = CAT_SAD_EMOTIONAL,
            subCategory = "lonely",
            tags = listOf("lonely", "alone", "thanimai", "night"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "pirivu",
            synonyms = listOf("separation", "separation songs", "pirivu paatu", "பிரிவு", "பிரிவு பாடல்கள்", "yaekkam", "yekkam", "ஏக்கம்"),
            category = CAT_SAD_EMOTIONAL,
            subCategory = "separation",
            tags = listOf("separation", "longing", "pirivu", "distance"),
            priority = 8
        ),

        // ==========================================
        // 6 & 7. MUSIC GENRES & TAMIL MUSIC TYPES
        // ==========================================
        KeywordEntry(
            keyword = "melody",
            synonyms = listOf("melodies", "melody songs", "soft melody", "tamil melody", "மெலடி", "மெலடி பாடல்கள்", "soulful"),
            category = CAT_GENRE,
            subCategory = "melody",
            tags = listOf("melody", "soft", "pleasant", "relax"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "kuthu",
            synonyms = listOf("tamil kuthu", "dappankuthu", "kuthu songs", "fast beat", "dance beat", "குத்து பாடல்கள்", "dappan kuthu", "thara local"),
            category = CAT_TAMIL_GENRE,
            subCategory = "kuthu",
            tags = listOf("kuthu", "dance", "fast", "party", "local"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "gaana",
            synonyms = listOf("gana", "gana songs", "tamil gaana", "marana gana", "local gana", "chennai gana", "கானா பாடல்கள்", "gana paatu"),
            category = CAT_TAMIL_GENRE,
            subCategory = "gaana",
            tags = listOf("gaana", "gana", "chennai", "local", "folk"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "folk",
            synonyms = listOf("tamil folk", "village songs", "tamil village songs", "nattupura paadal", "nattuppura paatu", "நாட்டுப்புற பாடல்கள்", "gramathu paadal"),
            category = CAT_TAMIL_GENRE,
            subCategory = "folk",
            tags = listOf("folk", "village", "traditional", "gramathu"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "classical",
            synonyms = listOf("tamil classical", "carnatic", "carnatic classical", "semi classical", "traditional music"),
            category = CAT_GENRE,
            subCategory = "classical",
            tags = listOf("classical", "carnatic", "traditional"),
            priority = 7
        ),
        KeywordEntry(
            keyword = "rap",
            synonyms = listOf("hip hop", "tamil rap", "hiphop", "tamil hip hop", "desi rap", "street rap"),
            category = CAT_GENRE,
            subCategory = "hip_hop",
            tags = listOf("rap", "hip hop", "beats", "rhymes"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "lofi",
            synonyms = listOf("lo-fi", "lofi remix", "lofi beats", "lo-fi chill", "slowed and reverb", "chill lofi"),
            category = CAT_GENRE,
            subCategory = "lofi",
            tags = listOf("lofi", "chill", "slowed", "reverb", "night"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "acoustic",
            synonyms = listOf("unplugged", "guitar", "piano", "acoustic cover", "stripped version"),
            category = CAT_GENRE,
            subCategory = "acoustic",
            tags = listOf("acoustic", "guitar", "unplugged"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "instrumental",
            synonyms = listOf("orchestral", "ambient", "bgm", "theme music", "movie bgm", "flute", "violin", "soundtrack instrumental"),
            category = CAT_GENRE,
            subCategory = "instrumental",
            tags = listOf("instrumental", "bgm", "theme", "ambient"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "remix",
            synonyms = listOf("mashup", "dj", "dj remix", "club mix", "dance mix", "party remix", "bass boosted"),
            category = CAT_GENRE,
            subCategory = "remix",
            tags = listOf("remix", "dj", "mashup", "club", "dance"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "indie",
            synonyms = listOf("independent", "tamil indie", "indie pop", "indie rock", "non film songs", "private songs"),
            category = CAT_GENRE,
            subCategory = "indie",
            tags = listOf("indie", "independent", "non film"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "pop",
            synonyms = listOf("rock", "electronic", "edm", "r&b", "jazz", "blues"),
            category = CAT_GENRE,
            subCategory = "western_pop",
            tags = listOf("pop", "rock", "edm", "western"),
            priority = 7
        ),

        // ==========================================
        // 8 & 20. ENERGY / ACTIVITY / SITUATION
        // ==========================================
        KeywordEntry(
            keyword = "workout",
            synonyms = listOf("gym", "gym songs", "workout songs", "fitness", "running", "walking", "exercise", "cardio", "pump up"),
            category = CAT_ACTIVITY,
            subCategory = "workout",
            tags = listOf("gym", "workout", "fitness", "energy", "motivation"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "travel",
            synonyms = listOf("driving", "driving songs", "travel songs", "road trip", "road trip songs", "bike ride", "bike ride songs", "car drive", "car drive songs", "highway"),
            category = CAT_ACTIVITY,
            subCategory = "travel",
            tags = listOf("travel", "driving", "trip", "journey", "highway"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "study",
            synonyms = listOf("focus", "study songs", "work", "work songs", "deep focus", "coding", "concentration"),
            category = CAT_ACTIVITY,
            subCategory = "focus",
            tags = listOf("study", "focus", "work", "peaceful"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "sleep",
            synonyms = listOf("sleep songs", "sleeping music", "bedtime", "insomnia", "deep sleep", "lullaby", "thaalattu", "தாலாட்டு"),
            category = CAT_ACTIVITY,
            subCategory = "sleep",
            tags = listOf("sleep", "calm", "relax", "lullaby"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "party",
            synonyms = listOf("party songs", "dance", "dance songs", "celebration", "club", "weekend party"),
            category = CAT_ACTIVITY,
            subCategory = "party",
            tags = listOf("party", "dance", "celebration", "kuthu"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "morning",
            synonyms = listOf("morning songs", "sunrise", "positive morning", "morning vibes", "kaalai"),
            category = CAT_SITUATION,
            subCategory = "morning",
            tags = listOf("morning", "peaceful", "fresh", "sunrise"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "night",
            synonyms = listOf("night songs", "late night", "midnight", "midnight drive", "iravu paadal"),
            category = CAT_SITUATION,
            subCategory = "night",
            tags = listOf("night", "chill", "relax", "drive"),
            priority = 8
        ),

        // ==========================================
        // 9. DEVOTIONAL / SPIRITUAL
        // ==========================================
        KeywordEntry(
            keyword = "devotional",
            synonyms = listOf("devotional songs", "bhakti", "bhakti songs", "god songs", "spiritual", "temple songs", "prayer songs", "tamil devotional", "bakthi paadal"),
            category = CAT_DEVOTIONAL,
            subCategory = "general_devotional",
            tags = listOf("devotional", "bhakti", "spiritual", "god", "prayer"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "murugan",
            synonyms = listOf("murugan songs", "murugan devotional", "kandan", "vel", "thaipusam", "kanda sashti kavasam", "kavadi", "முருகன் பாடல்கள்", "முருகன்"),
            category = CAT_DEVOTIONAL,
            subCategory = "murugan",
            tags = listOf("murugan", "kandhan", "lord murugan", "devotional"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "shiva",
            synonyms = listOf("shivan", "shiva songs", "shivan songs", "om namah shivaya", "mahadev", "sivan paatu", "ருத்ரம்"),
            category = CAT_DEVOTIONAL,
            subCategory = "shiva",
            tags = listOf("shiva", "sivan", "mahadev", "devotional"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "vinayagar",
            synonyms = listOf("vinayagar songs", "ganesha", "ganapathi", "pillaiyar", "pillayar paatu", "விநாயகர் பாடல்கள்"),
            category = CAT_DEVOTIONAL,
            subCategory = "vinayagar",
            tags = listOf("vinayagar", "ganesha", "pillayar", "devotional"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "amman",
            synonyms = listOf("amman songs", "mariamman", "aadi", "aadi thiruvizha", "amman paatu", "அம்மன் பாடல்கள்"),
            category = CAT_DEVOTIONAL,
            subCategory = "amman",
            tags = listOf("amman", "mariamman", "aadi", "devotional"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "ayyappa",
            synonyms = listOf("ayyappan", "ayyappa songs", "ayyappan songs", "sabarimala", "harivarasanam", "swamy saranam", "ஐயப்பன்"),
            category = CAT_DEVOTIONAL,
            subCategory = "ayyappa",
            tags = listOf("ayyappa", "sabarimala", "devotional"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "perumal",
            synonyms = listOf("perumal songs", "krishna", "krishna songs", "vishnu", "venkateshwara", "govinda", "பெருமாள் பாடல்கள்"),
            category = CAT_DEVOTIONAL,
            subCategory = "vishnu",
            tags = listOf("perumal", "krishna", "vishnu", "devotional"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "hanuman",
            synonyms = listOf("hanuman songs", "hanuman chalisa", "anjaneya", "ஆஞ்சநேயர்"),
            category = CAT_DEVOTIONAL,
            subCategory = "hanuman",
            tags = listOf("hanuman", "anjaneya", "devotional"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "christian",
            synonyms = listOf("christian songs", "christian devotional", "jesus songs", "gospel", "kiristhuva paadal"),
            category = CAT_DEVOTIONAL,
            subCategory = "christian",
            tags = listOf("christian", "jesus", "gospel", "devotional"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "islamic",
            synonyms = listOf("islamic songs", "islamic devotional", "allah songs", "qawwali", "sufi", "naat"),
            category = CAT_DEVOTIONAL,
            subCategory = "islamic",
            tags = listOf("islamic", "sufi", "qawwali", "devotional"),
            priority = 8
        ),

        // ==========================================
        // 10. LANGUAGE KEYWORDS
        // ==========================================
        KeywordEntry(
            keyword = "tamil",
            synonyms = listOf("tamil songs", "tamil music", "தமிழ்", "தமிழ் பாடல்கள்", "tamil paatu", "kollywood"),
            language = "Tamil",
            category = CAT_LANGUAGE,
            subCategory = "tamil",
            tags = listOf("tamil", "kollywood", "south"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "telugu",
            synonyms = listOf("telugu songs", "telugu music", "tollywood"),
            language = "Telugu",
            category = CAT_LANGUAGE,
            subCategory = "telugu",
            tags = listOf("telugu", "tollywood", "south"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "malayalam",
            synonyms = listOf("malayalam songs", "malayalam music", "mollywood"),
            language = "Malayalam",
            category = CAT_LANGUAGE,
            subCategory = "malayalam",
            tags = listOf("malayalam", "mollywood", "south"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "kannada",
            synonyms = listOf("kannada songs", "kannada music", "sandalwood"),
            language = "Kannada",
            category = CAT_LANGUAGE,
            subCategory = "kannada",
            tags = listOf("kannada", "sandalwood", "south"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "hindi",
            synonyms = listOf("hindi songs", "hindi music", "bollywood", "bollywood songs"),
            language = "Hindi",
            category = CAT_LANGUAGE,
            subCategory = "hindi",
            tags = listOf("hindi", "bollywood", "north"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "english",
            synonyms = listOf("english songs", "english music", "hollywood songs", "western songs", "global hits"),
            language = "English",
            category = CAT_LANGUAGE,
            subCategory = "english",
            tags = listOf("english", "global", "pop"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "punjabi",
            synonyms = listOf("punjabi songs", "punjabi music", "bhangra"),
            language = "Punjabi",
            category = CAT_LANGUAGE,
            subCategory = "punjabi",
            tags = listOf("punjabi", "bhangra"),
            priority = 7
        ),

        // ==========================================
        // 11. TOP ARTIST / SINGER / COMPOSER PROFILES
        // ==========================================
        KeywordEntry(
            keyword = "anirudh",
            synonyms = listOf("anirudh ravichander", "aniruth", "ani", "rockstar anirudh", "aniruthu"),
            category = CAT_ARTIST,
            subCategory = "composer",
            tags = listOf("anirudh", "composer", "youth", "mass", "chartbuster"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "ar rahman",
            synonyms = listOf("a r rahman", "a.r. rahman", "rahman", "arr", "isaipuyal", "isai puyal", "rahman sir", "allarakha rahman"),
            category = CAT_ARTIST,
            subCategory = "composer",
            tags = listOf("ar rahman", "composer", "legend", "melody", "oscar"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "ilaiyaraaja",
            synonyms = listOf("ilayaraja", "ilaiyaraja", "isaignani", "isai gnani", "raaja sir", "raaja melody"),
            category = CAT_ARTIST,
            subCategory = "composer",
            tags = listOf("ilayaraja", "composer", "maestro", "80s", "90s", "classic"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "yuvan",
            synonyms = listOf("yuvan shankar raja", "u1", "yuvan hits", "yuvan drugs", "yuvan melody"),
            category = CAT_ARTIST,
            subCategory = "composer",
            tags = listOf("yuvan", "composer", "drugs", "sad", "melody", "bgm"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "harris jayaraj",
            synonyms = listOf("harris", "harris jayaraj hits", "hj", "harris melody"),
            category = CAT_ARTIST,
            subCategory = "composer",
            tags = listOf("harris", "composer", "melody", "love", "minnale"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "gv prakash",
            synonyms = listOf("g v prakash", "g.v. prakash", "gvp", "gv prakash kumar"),
            category = CAT_ARTIST,
            subCategory = "composer_singer",
            tags = listOf("gv prakash", "composer", "melody", "asuran"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "santhosh narayanan",
            synonyms = listOf("sana", "sa na", "santhosh narayanan hits"),
            category = CAT_ARTIST,
            subCategory = "composer",
            tags = listOf("santhosh narayanan", "composer", "folk", "kabali", "indie"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "sid sriram",
            synonyms = listOf("sid", "sidsriram", "sid sriram hits"),
            category = CAT_ARTIST,
            subCategory = "singer",
            tags = listOf("sid sriram", "singer", "melody", "carnatic", "love"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "deva",
            synonyms = listOf("thenisai thendral deva", "deva gana", "deva hits"),
            category = CAT_ARTIST,
            subCategory = "composer",
            tags = listOf("deva", "gaana", "composer", "90s"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "spb",
            synonyms = listOf("s. p. balasubrahmanyam", "sp balasubrahmanyam", "balasubrahmanyam", "spb hits"),
            category = CAT_ARTIST,
            subCategory = "singer",
            tags = listOf("spb", "singer", "legend", "classic"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "kj yesudas",
            synonyms = listOf("k j yesudas", "yesudas", "ganagandharvan", "yesudas hits"),
            category = CAT_ARTIST,
            subCategory = "singer",
            tags = listOf("yesudas", "singer", "classical", "devotional"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "shreya ghoshal",
            synonyms = listOf("shreya", "shreya ghoshal hits", "shreya melody"),
            category = CAT_ARTIST,
            subCategory = "singer",
            tags = listOf("shreya ghoshal", "singer", "melody", "female"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "chinmayi",
            synonyms = listOf("chinmayi sripaada", "chinmayi hits"),
            category = CAT_ARTIST,
            subCategory = "singer",
            tags = listOf("chinmayi", "singer", "melody", "96"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "karthik",
            synonyms = listOf("singer karthik", "karthik hits", "karthik melody"),
            category = CAT_ARTIST,
            subCategory = "singer",
            tags = listOf("karthik", "singer", "melody", "love"),
            priority = 8
        ),

        // ==========================================
        // 12 & 13. MOVIE & ALBUM KEYWORDS
        // ==========================================
        KeywordEntry(
            keyword = "movie",
            synonyms = listOf("movie songs", "film songs", "cinema paatu", "thiraippadam", "soundtrack", "ost", "original soundtrack", "movie album", "album"),
            category = CAT_MOVIE,
            subCategory = "movie_soundtrack",
            tags = listOf("movie", "cinema", "soundtrack", "album"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "bgm",
            synonyms = listOf("movie bgm", "theme", "theme music", "title song", "intro song", "hero song", "heroine song", "climax song"),
            category = CAT_MOVIE,
            subCategory = "score",
            tags = listOf("bgm", "theme", "intro", "climax"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "mass songs",
            synonyms = listOf("vijay mass songs", "rajini mass songs", "ajith mass songs", "surya mass songs", "intro mass", "hero intro"),
            category = CAT_MOVIE,
            subCategory = "hero_mass",
            tags = listOf("mass", "intro", "hero", "energy"),
            priority = 9
        ),

        // ==========================================
        // 14. YEAR & DECADE SEARCH
        // ==========================================
        KeywordEntry(
            keyword = "2026",
            synonyms = listOf("2026 songs", "tamil songs 2026", "2026 hits", "latest 2026"),
            category = CAT_YEAR_ERA,
            subCategory = "current_year",
            tags = listOf("2026", "new", "fresh"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "2025",
            synonyms = listOf("2025 songs", "tamil songs 2025", "2025 hits"),
            category = CAT_YEAR_ERA,
            subCategory = "recent_year",
            tags = listOf("2025", "recent"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "2024",
            synonyms = listOf("2024 songs", "tamil songs 2024", "2024 hits"),
            category = CAT_YEAR_ERA,
            subCategory = "recent_year",
            tags = listOf("2024", "recent"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "2020s",
            synonyms = listOf("2020s tamil songs", "2020s hits"),
            category = CAT_YEAR_ERA,
            subCategory = "decade",
            tags = listOf("2020s", "modern"),
            priority = 7
        ),
        KeywordEntry(
            keyword = "2000s",
            synonyms = listOf("2000s tamil songs", "2000s hits", "2k hits", "2000s melody", "2000 songs"),
            category = CAT_YEAR_ERA,
            subCategory = "decade",
            tags = listOf("2000s", "harris", "yuvan", "nostalgia"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "90s",
            synonyms = listOf("90s songs", "90s tamil songs", "1990s", "1990s tamil songs", "90s kids", "90s hits", "90s melody", "90s love songs"),
            category = CAT_YEAR_ERA,
            subCategory = "decade",
            tags = listOf("90s", "golden era", "rahman", "ilayaraja", "evergreen"),
            priority = 10
        ),
        KeywordEntry(
            keyword = "80s",
            synonyms = listOf("80s songs", "80s tamil songs", "1980s", "1980s tamil songs", "80s hits", "80s melody", "80s ilayaraja"),
            category = CAT_YEAR_ERA,
            subCategory = "decade",
            tags = listOf("80s", "ilayaraja", "spb", "classic"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "70s",
            synonyms = listOf("70s songs", "70s tamil songs", "1970s", "70s hits", "msv songs"),
            category = CAT_YEAR_ERA,
            subCategory = "decade",
            tags = listOf("70s", "retro", "classic"),
            priority = 8
        ),

        // ==========================================
        // 15. RELEASE / NEW SONG KEYWORDS
        // ==========================================
        KeywordEntry(
            keyword = "latest",
            synonyms = listOf("new", "newest", "recent", "new release", "new releases", "latest release", "recent release", "just released", "new songs", "latest songs", "trending new songs", "latest tamil songs", "new tamil songs", "puthiya paadal", "புதிய பாடல்கள்"),
            category = CAT_RELEASE,
            subCategory = "new_releases",
            tags = listOf("latest", "new", "fresh", "trending"),
            priority = 10
        ),

        // ==========================================
        // 16. TRENDING / POPULAR
        // ==========================================
        KeywordEntry(
            keyword = "trending",
            synonyms = listOf("trending songs", "viral", "viral songs", "popular", "popular songs", "top songs", "top hits", "chartbusters", "most played", "most listened", "top 10", "best songs"),
            category = CAT_TRENDING,
            subCategory = "viral_trends",
            tags = listOf("trending", "viral", "top", "chartbuster"),
            priority = 10
        ),

        // ==========================================
        // 17. OLD / CLASSIC / EVERGREEN
        // ==========================================
        KeywordEntry(
            keyword = "old songs",
            synonyms = listOf("old tamil songs", "classic songs", "classical hits", "evergreen", "evergreen songs", "retro", "retro songs", "golden hits", "old melodies", "pazhaya paadal", "பழைய பாடல்கள்"),
            category = CAT_CLASSIC,
            subCategory = "evergreen",
            tags = listOf("old", "classic", "evergreen", "retro", "golden hits"),
            priority = 10
        ),

        // ==========================================
        // 18. FESTIVAL / OCCASION
        // ==========================================
        KeywordEntry(
            keyword = "festival",
            synonyms = listOf("festival songs", "thiruvizha", "thiruvizha paatu", "celebration songs"),
            category = CAT_FESTIVAL,
            subCategory = "festival",
            tags = listOf("festival", "celebration", "tradition"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "pongal",
            synonyms = listOf("pongal songs", "tamil new year songs", "sankranti", "thai pongal"),
            category = CAT_FESTIVAL,
            subCategory = "pongal",
            tags = listOf("pongal", "tamil new year", "folk"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "diwali",
            synonyms = listOf("diwali songs", "deepavali", "deepavali songs"),
            category = CAT_FESTIVAL,
            subCategory = "diwali",
            tags = listOf("diwali", "deepavali", "celebration"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "new year",
            synonyms = listOf("new year songs", "happy new year", "christmas songs", "xmas songs"),
            category = CAT_FESTIVAL,
            subCategory = "new_year_xmas",
            tags = listOf("new year", "christmas", "party"),
            priority = 8
        ),
        KeywordEntry(
            keyword = "birthday",
            synonyms = listOf("birthday songs", "happy birthday", "piranthanal paatu"),
            category = CAT_FESTIVAL,
            subCategory = "birthday",
            tags = listOf("birthday", "wishes", "celebration"),
            priority = 8
        ),

        // ==========================================
        // 19. RELATIONSHIP KEYWORDS
        // ==========================================
        KeywordEntry(
            keyword = "friendship",
            synonyms = listOf("friends", "best friend", "friendship songs", "nanban", "nanbargal", "நண்பன்", "thozhi", "தோழி", "natpu", "நட்பு", "natpu paadal", "dost"),
            category = CAT_RELATIONSHIP,
            subCategory = "friends",
            tags = listOf("friendship", "natpu", "friends", "bonding"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "mother",
            synonyms = listOf("mother songs", "amma", "amma songs", "அம்மா", "அம்மா பாடல்கள்", "thaai", "thai paasam"),
            category = CAT_RELATIONSHIP,
            subCategory = "mother",
            tags = listOf("mother", "amma", "family", "emotional", "paasam"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "father",
            synonyms = listOf("father songs", "appa", "appa songs", "அப்பா", "அப்பா பாடல்கள்", "thantai"),
            category = CAT_RELATIONSHIP,
            subCategory = "father",
            tags = listOf("father", "appa", "family", "emotional"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "family",
            synonyms = listOf("parents", "brother", "sister", "siblings", "udanpirappe", "anna", "thambi", "akka", "thangachi", "paasam", "பாசம்", "uravu", "உறவு"),
            category = CAT_RELATIONSHIP,
            subCategory = "family",
            tags = listOf("family", "brother", "sister", "paasam"),
            priority = 8
        ),

        // ==========================================
        // 21. WEATHER / NATURE
        // ==========================================
        KeywordEntry(
            keyword = "rain",
            synonyms = listOf("rain songs", "rainy songs", "mazhai", "மழை", "mazhai songs", "rain melody", "rain romantic", "monsoon"),
            category = CAT_WEATHER,
            subCategory = "rain",
            tags = listOf("rain", "mazhai", "weather", "monsoon", "melody"),
            priority = 9
        ),
        KeywordEntry(
            keyword = "nature",
            synonyms = listOf("nature songs", "sea", "beach", "beach songs", "mountain", "sunset", "sunset songs"),
            category = CAT_WEATHER,
            subCategory = "nature",
            tags = listOf("nature", "sea", "beach", "sunset", "peaceful"),
            priority = 8
        )
    )

    // Lookup index for synonyms -> Canonical Keyword Entry
    private val synonymLookupIndex: Map<String, KeywordEntry> by lazy {
        val map = mutableMapOf<String, KeywordEntry>()
        for (entry in MASTER_DICTIONARY) {
            map[entry.keyword.lowercase().trim()] = entry
            for (syn in entry.synonyms) {
                map[syn.lowercase().trim()] = entry
            }
        }
        map
    }

    fun findEntry(term: String): KeywordEntry? {
        val clean = term.lowercase().trim()
        return synonymLookupIndex[clean]
    }

    fun getEntriesByCategory(category: String): List<KeywordEntry> {
        return MASTER_DICTIONARY.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun getAllCategories(): List<String> {
        return MASTER_DICTIONARY.map { it.category }.distinct()
    }
}
