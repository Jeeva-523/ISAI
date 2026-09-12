package com.saavn.music.data.search

import com.saavn.music.data.model.YouTubeSong
import java.util.Locale

/**
 * Parsed intent from a user search query.
 */
data class ParsedSearchIntent(
    val rawQuery: String,
    val canonicalQuery: String,
    val detectedLanguage: String? = null,
    val detectedArtist: String? = null,
    val detectedMood: String? = null,
    val detectedGenre: String? = null,
    val detectedActivity: String? = null,
    val detectedYearOrEra: String? = null,
    val detectedSituation: String? = null,
    val isLatest: Boolean = false,
    val isClassicOld: Boolean = false,
    val isDevotional: Boolean = false,
    val isInstrumentalOrBgm: Boolean = false,
    val unmatchedTerms: List<String> = emptyList(),
    val directAudioQuery: String,
    val youtubeMusicQuery: String,
    val relatedQueries: List<String> = emptyList()
)

object SmartSearchEngine {

    // Common Spelling & Transliteration Normalization Map
    private val SPELLING_NORMALIZATION = mapOf(
        // Love / Romance
        "kaadhal" to "kadhal",
        "kathal" to "kadhal",
        "காதல்" to "kadhal",
        "காதல் பாடல்கள்" to "kadhal songs",
        "oruthalai" to "one sided love",
        "ஒருதலை காதல்" to "one sided love",
        // Songs / Tracks
        "paatu" to "songs",
        "paattu" to "songs",
        "paadal" to "songs",
        "paadalgal" to "songs",
        "பாட்டு" to "songs",
        "பாடல்" to "songs",
        "பாடல்கள்" to "songs",
        "isai" to "music",
        "இசை" to "music",
        // Artists & Music Directors
        "aniruth" to "anirudh",
        "aniruthu" to "anirudh",
        "அனிருத்" to "anirudh",
        "ani" to "anirudh",
        "rahman" to "ar rahman",
        "a r rahman" to "ar rahman",
        "a.r. rahman" to "ar rahman",
        "arr" to "ar rahman",
        "ரஹ்மான்" to "ar rahman",
        "ilayaraja" to "ilaiyaraaja",
        "ilaiyaraja" to "ilaiyaraaja",
        "இளையராஜா" to "ilaiyaraaja",
        "raaja" to "ilaiyaraaja",
        "raja" to "ilaiyaraaja",
        "u1" to "yuvan",
        "yuvan shankar raja" to "yuvan",
        "யுவன்" to "yuvan",
        "hj" to "harris jayaraj",
        "harris" to "harris jayaraj",
        "ஹாரிஸ்" to "harris jayaraj",
        "gvp" to "gv prakash",
        "g v prakash" to "gv prakash",
        "ஜி வி பிரகாஷ்" to "gv prakash",
        "sana" to "santhosh narayanan",
        "sa na" to "santhosh narayanan",
        "சந்தோஷ் நாராயணன்" to "santhosh narayanan",
        "sidsriram" to "sid sriram",
        "சித் ஸ்ரீராம்" to "sid sriram",
        "spb" to "sp balasubrahmanyam",
        "எஸ் பி பி" to "sp balasubrahmanyam",
        "yesudas" to "kj yesudas",
        "யேசுதாஸ்" to "kj yesudas",
        "deva" to "deva",
        "தேவா" to "deva",
        // Moods & Sad
        "sogam" to "sad",
        "sogamana" to "sad",
        "சோகம்" to "sad",
        "சோக பாடல்கள்" to "sad songs",
        "pirivu" to "sad separation",
        "பிரிவு" to "sad separation",
        "பிரிவு பாடல்கள்" to "sad separation songs",
        "yaekkam" to "longing",
        "yekkam" to "longing",
        "ஏக்கம்" to "longing",
        "valigal" to "pain",
        "thanimai" to "lonely",
        "தனிமை" to "lonely",
        "kaadhal tholvi" to "love failure",
        "kadhal tholvi" to "love failure",
        "காதல் தோல்வி" to "love failure",
        // Tamil Genres
        "gana" to "gaana",
        "கானா" to "gaana",
        "கானா பாடல்கள்" to "gaana songs",
        "marana gana" to "gaana",
        "marana gaana" to "gaana",
        "dappankuthu" to "kuthu",
        "dappan kuthu" to "kuthu",
        "குத்து" to "kuthu",
        "குத்து பாடல்கள்" to "kuthu songs",
        "மெலடி" to "melody",
        "மெலடி பாடல்கள்" to "melody songs",
        "nattupura" to "folk",
        "நாட்டுப்புற பாடல்கள்" to "folk songs",
        "gramathu" to "folk",
        // Activity & Weather
        "mazhai" to "rain",
        "மழை" to "rain",
        "gym" to "workout",
        "lo-fi" to "lofi",
        // Devotional
        "முருகன்" to "murugan devotional",
        "முருகன் பாடல்கள்" to "murugan devotional songs",
        "அம்மன்" to "amman devotional",
        "அம்மன் பாடல்கள்" to "amman devotional songs",
        "விநாயகர்" to "vinayagar devotional",
        "விநாயகர் பாடல்கள்" to "vinayagar devotional songs",
        "பெருமாள்" to "perumal devotional",
        "பெருமாள் பாடல்கள்" to "perumal devotional songs",
        "ஐயப்பன்" to "ayyappa devotional",
        "சிவன்" to "shiva devotional",
        "சிவன் பாடல்கள்" to "shiva devotional songs",
        // Relationships
        "amma" to "mother",
        "அம்மா" to "mother",
        "அம்மா பாடல்கள்" to "mother songs",
        "appa" to "father",
        "அப்பா" to "father",
        "அப்பா பாடல்கள்" to "father songs",
        "nanban" to "friendship",
        "நண்பன்" to "friendship",
        "natpu" to "friendship",
        "நட்பு" to "friendship",
        "natpu paadal" to "friendship songs",
        // New & Old
        "puthiya" to "latest new",
        "புதிய பாடல்கள்" to "latest new songs",
        "pazhaya" to "old classic",
        "பழைய பாடல்கள்" to "old classic songs"
    )

    private val MULTI_SPACE_REGEX = Regex("\\s+")
    private val LEADING_SYMBOL_REGEX = Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Punct}\\s]+")

    private val PRECOMPILED_SPELLING_MAP: List<Pair<Regex, String>> = SPELLING_NORMALIZATION.map { (variant, canonical) ->
        val pattern = if (variant.any { it.code > 127 }) {
            Regex(Regex.escape(variant), RegexOption.IGNORE_CASE)
        } else {
            Regex("(?i)\\b" + Regex.escape(variant) + "\\b")
        }
        pattern to canonical
    }

    fun normalizeText(input: String): String {
        var text = input.trim().lowercase(Locale.ROOT)
        // Strip leading emojis from quick chips (e.g. "⚡ Anirudh Hits" -> "Anirudh Hits")
        text = LEADING_SYMBOL_REGEX.replace(text, "")
        for ((regex, canonical) in PRECOMPILED_SPELLING_MAP) {
            text = text.replace(regex, canonical)
        }
        return MULTI_SPACE_REGEX.replace(text, " ").trim()
    }

    /**
     * Parse raw search query into multi-keyword structured facets and generate optimized queries.
     * Supports user language preferences and favorite artists for non-invasive personalization.
     */
    fun parseQuery(
        rawQuery: String,
        userPreferredLanguages: List<String> = listOf("tamil"),
        userFavoriteArtists: List<String> = emptyList()
    ): ParsedSearchIntent {
        val cleanRaw = rawQuery.trim()
        if (cleanRaw.isBlank()) {
            return ParsedSearchIntent(
                rawQuery = "",
                canonicalQuery = "",
                directAudioQuery = "",
                youtubeMusicQuery = ""
            )
        }

        val normalized = normalizeText(cleanRaw)
        val tokens = normalized.split(" ").filter { it.isNotBlank() }

        var detectedLang: String? = null
        var detectedArtist: String? = null
        var detectedMood: String? = null
        var detectedGenre: String? = null
        var detectedActivity: String? = null
        var detectedYearOrEra: String? = null
        var detectedSituation: String? = null
        var isLatest = false
        var isClassicOld = false
        var isDevotional = false
        var isInstrumental = false

        val matchedTokens = mutableSetOf<String>()

        // 1. Multi-word phrase matching against Dictionary
        for (entry in IsaiKeywordDictionary.MASTER_DICTIONARY) {
            val phrases = (listOf(entry.keyword) + entry.synonyms).sortedByDescending { it.length }
            for (p in phrases) {
                val pNorm = p.lowercase(Locale.ROOT).trim()
                if (pNorm.isNotBlank() && normalized.contains(pNorm)) {
                    when (entry.category) {
                        IsaiKeywordDictionary.CAT_LANGUAGE -> if (detectedLang == null) detectedLang = entry.keyword
                        IsaiKeywordDictionary.CAT_ARTIST -> if (detectedArtist == null) detectedArtist = entry.keyword
                        IsaiKeywordDictionary.CAT_MOOD, IsaiKeywordDictionary.CAT_TAMIL_MOOD, IsaiKeywordDictionary.CAT_LOVE_ROMANCE -> {
                            if (detectedMood == null) detectedMood = entry.keyword
                        }
                        IsaiKeywordDictionary.CAT_SAD_EMOTIONAL -> {
                            if (detectedMood == null) detectedMood = entry.keyword
                        }
                        IsaiKeywordDictionary.CAT_GENRE, IsaiKeywordDictionary.CAT_TAMIL_GENRE -> {
                            if (detectedGenre == null) detectedGenre = entry.keyword
                        }
                        IsaiKeywordDictionary.CAT_ACTIVITY -> if (detectedActivity == null) detectedActivity = entry.keyword
                        IsaiKeywordDictionary.CAT_SITUATION -> if (detectedSituation == null) detectedSituation = entry.keyword
                        IsaiKeywordDictionary.CAT_WEATHER -> if (detectedSituation == null) detectedSituation = entry.keyword
                        IsaiKeywordDictionary.CAT_YEAR_ERA -> if (detectedYearOrEra == null) detectedYearOrEra = entry.keyword
                        IsaiKeywordDictionary.CAT_RELEASE -> isLatest = true
                        IsaiKeywordDictionary.CAT_CLASSIC -> isClassicOld = true
                        IsaiKeywordDictionary.CAT_DEVOTIONAL -> isDevotional = true
                        IsaiKeywordDictionary.CAT_ALBUM, IsaiKeywordDictionary.CAT_MOVIE -> {
                            if (entry.keyword == "bgm") isInstrumental = true
                        }
                    }
                    pNorm.split(" ").forEach { matchedTokens.add(it) }
                }
            }
        }

        // 2. Fallback token inspection for specific known terms
        for (token in tokens) {
            when (token) {
                "tamil" -> if (detectedLang == null) detectedLang = "tamil"
                "telugu" -> if (detectedLang == null) detectedLang = "telugu"
                "hindi" -> if (detectedLang == null) detectedLang = "hindi"
                "malayalam" -> if (detectedLang == null) detectedLang = "malayalam"
                "kannada" -> if (detectedLang == null) detectedLang = "kannada"
                "english" -> if (detectedLang == null) detectedLang = "english"
                "punjabi" -> if (detectedLang == null) detectedLang = "punjabi"
                "anirudh", "ani" -> if (detectedArtist == null) detectedArtist = "anirudh"
                "rahman", "arr" -> if (detectedArtist == null) detectedArtist = "ar rahman"
                "yuvan", "u1" -> if (detectedArtist == null) detectedArtist = "yuvan"
                "ilayaraja", "ilaiyaraaja", "raaja", "raja" -> if (detectedArtist == null) detectedArtist = "ilaiyaraaja"
                "harris" -> if (detectedArtist == null) detectedArtist = "harris jayaraj"
                "sidsriram", "sid" -> if (detectedArtist == null) detectedArtist = "sid sriram"
                "spb" -> if (detectedArtist == null) detectedArtist = "sp balasubrahmanyam"
                "deva" -> if (detectedArtist == null) detectedArtist = "deva"
                "kadhal", "love", "romantic" -> if (detectedMood == null) detectedMood = "love"
                "sad", "breakup", "pain", "sogam" -> if (detectedMood == null) detectedMood = "sad"
                "kuthu", "gaana", "gana", "melody", "folk", "rap", "lofi" -> if (detectedGenre == null) detectedGenre = token
                "gym", "workout" -> if (detectedActivity == null) detectedActivity = "workout"
                "rain", "mazhai" -> if (detectedSituation == null) detectedSituation = "rain"
                "travel", "driving" -> if (detectedActivity == null) detectedActivity = "travel"
                "90s", "1990s" -> if (detectedYearOrEra == null) detectedYearOrEra = "90s"
                "80s", "1980s" -> if (detectedYearOrEra == null) detectedYearOrEra = "80s"
                "2000s", "2k" -> if (detectedYearOrEra == null) detectedYearOrEra = "2000s"
                "2026", "2025", "2024" -> if (detectedYearOrEra == null) detectedYearOrEra = token
                "new", "latest", "recent" -> isLatest = true
                "old", "retro", "classic" -> isClassicOld = true
                "murugan", "shiva", "amman", "krishna", "bhakti" -> isDevotional = true
            }
        }

        // Generic search stop words
        val genericStopWords = setOf(
            "song", "songs", "track", "tracks", "music", "for", "in", "the", "a", "an",
            "paatu", "paattu", "paadal", "paadalgal", "hits", "hit", "best", "top", "all",
            "tamil", "telugu", "hindi", "malayalam", "kannada", "english"
        )

        // Unmatched tokens are words that might be specific song or movie titles (e.g. "hukum", "munbe", "vaa", "leo")
        val unmatched = tokens.filterNot { matchedTokens.contains(it) || it in genericStopWords }

        // Primary language resolution
        val primaryLang = userPreferredLanguages.firstOrNull()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "tamil"
        val effectiveLang = detectedLang ?: primaryLang
        val capLang = effectiveLang.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        // Resolve artist canonical display name
        val artistCanonicalName = detectedArtist?.let { art ->
            when (art.lowercase(Locale.ROOT)) {
                "anirudh", "ani" -> "Anirudh Ravichander"
                "ar rahman", "rahman", "arr" -> "A.R. Rahman"
                "ilaiyaraaja", "ilayaraja", "raaja", "raja" -> "Ilaiyaraaja"
                "yuvan", "u1" -> "Yuvan Shankar Raja"
                "harris jayaraj", "harris", "hj" -> "Harris Jayaraj"
                "sid sriram", "sidsriram" -> "Sid Sriram"
                "gv prakash", "gvp" -> "G.V. Prakash"
                "santhosh narayanan", "sana" -> "Santhosh Narayanan"
                "sp balasubrahmanyam", "spb" -> "S.P. Balasubrahmanyam"
                "kj yesudas", "yesudas" -> "K.J. Yesudas"
                "deva" -> "Deva"
                else -> art.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }

        val isSpecificSongOrMovie = unmatched.isNotEmpty()

        // Construct high-yield YouTube Music and Audio queries
        val ytMusic: String
        val directAudio: String

        if (isSpecificSongOrMovie) {
            // Case 1: Specific song title / movie title / track search
            // (e.g. "Hukum", "Arabic Kuthu", "Munbe Vaa", "Leo Naa Ready")
            // Send the normalized query cleanly to YouTube Music so official results rank #1!
            // If artist was also mentioned (e.g. "Anirudh Hukum"), keep both.
            val titleTerms = unmatched.joinToString(" ")
            ytMusic = if (artistCanonicalName != null) {
                "$artistCanonicalName $titleTerms"
            } else {
                normalized
            }
            directAudio = if (artistCanonicalName != null) {
                "$titleTerms $artistCanonicalName"
            } else {
                normalized
            }
        } else {
            // Case 2: Categorical / Keyword-driven search
            // (e.g. "Anirudh hits", "90s Tamil songs", "Yuvan sad songs", "Tamil kuthu", "Murugan songs")
            val ytBuilder = mutableListOf<String>()
            val audioBuilder = mutableListOf<String>()

            // 1. Language prefix (only when relevant)
            if (detectedLang != null || detectedArtist == null) {
                ytBuilder.add(capLang)
                audioBuilder.add(capLang)
            }

            // 2. Artist
            artistCanonicalName?.let {
                ytBuilder.add(it)
                audioBuilder.add(it)
            }

            // 3. Era / Year
            detectedYearOrEra?.let { era ->
                ytBuilder.add(era)
                audioBuilder.add(era)
            }

            // 4. Mood
            detectedMood?.let { m ->
                when (m) {
                    "love" -> {
                        ytBuilder.add("love melody")
                        audioBuilder.add("love songs")
                    }
                    "sad" -> {
                        ytBuilder.add("sad songs")
                        audioBuilder.add("sad songs")
                    }
                    "happy" -> {
                        ytBuilder.add("feel good songs")
                        audioBuilder.add("happy songs")
                    }
                    "peaceful" -> {
                        ytBuilder.add("peaceful relaxing melody")
                        audioBuilder.add("melody songs")
                    }
                    else -> {
                        ytBuilder.add("$m songs")
                        audioBuilder.add("$m songs")
                    }
                }
            }

            // 5. Genre
            detectedGenre?.let { g ->
                when (g) {
                    "kuthu" -> {
                        ytBuilder.add("mass kuthu songs")
                        audioBuilder.add("kuthu songs")
                    }
                    "gaana" -> {
                        ytBuilder.add("marana gaana songs")
                        audioBuilder.add("gaana songs")
                    }
                    "melody" -> {
                        ytBuilder.add("evergreen melody songs")
                        audioBuilder.add("melody hits")
                    }
                    "folk" -> {
                        ytBuilder.add("village folk songs")
                        audioBuilder.add("folk songs")
                    }
                    "lofi" -> {
                        ytBuilder.add("lofi chill songs")
                        audioBuilder.add("lofi songs")
                    }
                    else -> {
                        ytBuilder.add("$g songs")
                        audioBuilder.add("$g songs")
                    }
                }
            }

            // 6. Situation / Activity
            detectedSituation?.let { s ->
                when (s) {
                    "rain" -> {
                        ytBuilder.add("rain mazhai melody songs")
                        audioBuilder.add("rain songs")
                    }
                    else -> {
                        ytBuilder.add("$s songs")
                        audioBuilder.add("$s songs")
                    }
                }
            }
            detectedActivity?.let { a ->
                when (a) {
                    "workout" -> {
                        ytBuilder.add("gym workout beats")
                        audioBuilder.add("workout songs")
                    }
                    "travel" -> {
                        ytBuilder.add("travel road trip songs")
                        audioBuilder.add("travel songs")
                    }
                    else -> {
                        ytBuilder.add("$a songs")
                        audioBuilder.add("$a songs")
                    }
                }
            }

            // 7. Devotional
            if (isDevotional) {
                ytBuilder.add("devotional bhakti songs")
                audioBuilder.add("devotional songs")
            }

            // Suffix
            if (isLatest) {
                ytBuilder.add("latest new songs")
                audioBuilder.add("latest songs")
            } else if (isClassicOld) {
                ytBuilder.add("classic evergreen hits")
                audioBuilder.add("old classic songs")
            } else if (detectedGenre == null && detectedMood == null && detectedActivity == null && detectedSituation == null) {
                ytBuilder.add("hit songs")
                audioBuilder.add("hit songs")
            }

            ytMusic = ytBuilder.distinct().joinToString(" ").replace(Regex("\\s+"), " ").trim()
            directAudio = audioBuilder.distinct().joinToString(" ").replace(Regex("\\s+"), " ").trim()
        }

        // Build fallback related queries only for empty result fallbacks
        val relatedList = mutableListOf<String>()
        artistCanonicalName?.let { art ->
            relatedList.add("$art $capLang hit songs")
            relatedList.add("$art $capLang melody")
        }
        detectedMood?.let { m ->
            relatedList.add("$capLang $m songs")
        }
        detectedGenre?.let { g ->
            relatedList.add("$capLang $g songs")
        }

        return ParsedSearchIntent(
            rawQuery = rawQuery,
            canonicalQuery = normalized,
            detectedLanguage = detectedLang,
            detectedArtist = detectedArtist,
            detectedMood = detectedMood,
            detectedGenre = detectedGenre,
            detectedActivity = detectedActivity,
            detectedYearOrEra = detectedYearOrEra,
            detectedSituation = detectedSituation,
            isLatest = isLatest,
            isClassicOld = isClassicOld,
            isDevotional = isDevotional,
            isInstrumentalOrBgm = isInstrumental,
            unmatchedTerms = unmatched,
            directAudioQuery = directAudio,
            youtubeMusicQuery = ytMusic,
            relatedQueries = relatedList.distinct()
        )
    }

    /**
     * Highly accurate relevance ranker.
     * Rewarding exact title matches, full token coverage, Innertube search order,
     * and penalizing irrelevant low-quality clutter (karaoke, status videos, tutorials).
     */
    fun scoreAndRankSong(
        song: YouTubeSong,
        intent: ParsedSearchIntent,
        userPreferredLanguages: List<String> = listOf("tamil"),
        userFavoriteArtists: List<String> = emptyList(),
        originalRank: Int = 0
    ): Int {
        var score = 1000

        // 1. YouTube Innertube Position Baseline (Preserve proven ML ranking from YouTube)
        // First result gets +400, second gets +390, down to +0
        val positionBonus = (400 - (originalRank * 10)).coerceAtLeast(0)
        score += positionBonus

        val rawQueryLower = intent.rawQuery.trim().lowercase(Locale.ROOT)
        val canonicalLower = intent.canonicalQuery.trim().lowercase(Locale.ROOT)
        val titleLower = song.title.lowercase(Locale.ROOT).trim()
        val artistLower = song.channelTitle.lowercase(Locale.ROOT).trim()
        val fullText = "$titleLower $artistLower"

        // Stop words to exclude from token checks
        val stopWords = setOf("song", "songs", "track", "music", "the", "a", "an", "in", "of", "and", "for", "paatu", "paadal", "paattu", "paadalgal", "hits", "hit", "movie", "film", "cinema", "soundtrack", "ost")
        val queryTokens = canonicalLower.split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.length > 1 && it !in stopWords }

        // 2. Exact Title Match Bonus (Massive priority for specific song searches)
        val cleanTitle = titleLower.replace(Regex("\\(.*\\)|\\[.*\\]"), "").trim()
        if (cleanTitle == rawQueryLower || cleanTitle == canonicalLower) {
            score += 2500
        } else if (cleanTitle.startsWith(rawQueryLower) || cleanTitle.startsWith(canonicalLower)) {
            score += 1800
        } else if (titleLower.contains(rawQueryLower) || titleLower.contains(canonicalLower)) {
            score += 1200
        }

        // 2b. Movie / Album Match Bonus (Massive priority when searching for movie names like Ghilli, Leo, Master, etc.)
        val albumPart = if (artistLower.contains(" • ")) {
            artistLower.substringAfter(" • ").trim()
        } else ""
        if (albumPart.isNotBlank()) {
            if (albumPart == rawQueryLower || albumPart == canonicalLower) {
                score += 2400
            } else if (albumPart.startsWith(rawQueryLower) || albumPart.startsWith(canonicalLower)) {
                score += 1900
            } else if (albumPart.contains(rawQueryLower) || albumPart.contains(canonicalLower)) {
                score += 1500
            } else if (queryTokens.isNotEmpty() && queryTokens.all { albumPart.contains(it) }) {
                score += 1400
            }
        } else if (queryTokens.isNotEmpty() && queryTokens.all { artistLower.contains(it) }) {
            score += 1200
        }

        // 3. Query Token Coverage in Title / Artist / Album
        if (queryTokens.isNotEmpty()) {
            var matchedCount = 0
            for (token in queryTokens) {
                val inTitle = titleLower.contains(token)
                val inArtist = artistLower.contains(token)
                if (inTitle) {
                    score += 250
                    matchedCount++
                } else if (inArtist) {
                    score += 200
                    matchedCount++
                }
            }

            // All tokens matched bonus
            if (matchedCount == queryTokens.size) {
                score += 600
            } else if (matchedCount == 0 && queryTokens.size >= 2) {
                // Irrelevant song that doesn't contain any query words
                score -= 500
            }
        }

        // 4. Keyword: Artist Match
        intent.detectedArtist?.let { art ->
            val artLower = art.lowercase(Locale.ROOT)
            if (fullText.contains(artLower)) {
                score += 700
            } else {
                // If query is predominantly an artist search (e.g. "Anirudh", "SPB hits") and song does not have this artist
                if (queryTokens.all { it in artLower || it in stopWords }) {
                    score -= 1000
                }
            }
        }

        // 5. Keyword: Genre Match (kuthu, gaana, melody, folk, lofi, rap)
        intent.detectedGenre?.let { genre ->
            val genreLower = genre.lowercase(Locale.ROOT)
            if (fullText.contains(genreLower)) {
                score += 400
            }
        }

        // 6. Keyword: Mood Match (kadhal/love, sad/sogam, happy, devotional)
        intent.detectedMood?.let { mood ->
            val moodLower = mood.lowercase(Locale.ROOT)
            val isLove = moodLower in listOf("love", "kadhal", "romantic", "melody")
            val isSad = moodLower in listOf("sad", "sogam", "breakup", "pain")
            if (isLove && (fullText.contains("love") || fullText.contains("kadhal") || fullText.contains("romantic") || fullText.contains("melody"))) {
                score += 400
            } else if (isSad && (fullText.contains("sad") || fullText.contains("sogam") || fullText.contains("breakup") || fullText.contains("pain") || fullText.contains("tears") || fullText.contains("alone"))) {
                score += 500
            } else if (fullText.contains(moodLower)) {
                score += 400
            }
        }

        // 7. Keyword: Era Match (90s, 80s, 2000s, classic)
        intent.detectedYearOrEra?.let { era ->
            val cleanEra = era.lowercase(Locale.ROOT).replace("s", "")
            if (fullText.contains(cleanEra)) {
                score += 400
            }
        }

        // 8. Keyword: Devotional Match
        if (intent.isDevotional) {
            val devotionalTags = listOf("bhakti", "devotional", "murugan", "shiva", "amman", "vinayagar", "krishna", "ayyappan", "perumal", "god")
            if (devotionalTags.any { fullText.contains(it) }) {
                score += 500
            } else {
                score -= 500
            }
        }

        // 9. Junk / Low Quality Penalties (Filter out WhatsApp status, 30s clips, lessons)
        val junkKeywords = listOf(
            "whatsapp status", "status video", "short video", "shorts", "30 sec", "30s status",
            "karaoke", "instrumental cover", "guitar tutorial", "how to play", "guitar lesson",
            "reaction", "reaction video", "movie review", "1 hour loop", "10 hours loop"
        )
        for (junk in junkKeywords) {
            if (titleLower.contains(junk)) {
                score -= 1500
                break
            }
        }

        // 10. Official Music Video / Audio Label Boost
        val officialLabels = listOf("official", "lyric video", "video song", "audio song", "sony music", "think music", "saregama", "t-series", "tips", "sun nxt", "zeemusic")
        if (officialLabels.any { fullText.contains(it) }) {
            score += 100
        }

        // 11. User Preferred Language Match (Gentle)
        val selectedLang = intent.detectedLanguage ?: userPreferredLanguages.firstOrNull()?.lowercase(Locale.ROOT)
        if (selectedLang != null && selectedLang.isNotBlank()) {
            if (fullText.contains(selectedLang)) {
                score += 50
            }
        }

        return score
    }
}
