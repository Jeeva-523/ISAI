package com.saavn.music.util

import com.saavn.music.data.model.YouTubeSong

enum class SongMood {
    GANA_FOLK,
    INTRO_MASS,
    MELODY_ROMANCE,
    PARTY_KUTHU,
    SAD_HEARTBREAK,
    MOTIVATION_INSPIRING,
    DEVOTIONAL,
    GENERAL
}

object RelevanceEngine {

    // Keyword patterns for detecting Tamil Gaana / Folk / Marana Gaana songs
    private val GANA_KEYWORDS = listOf(
        "gana", "gaana", "marana gana", "chennai gana", "local gana", "pullingo",
        "danga maari", "kallamanna", "tasakku", "tasaku", "aaluma doluma", "jithu jilladi",
        "varuthu da gaana", "athana pathana", "ootaanda", "enna pulla", "adangatha asuran",
        "dappan", "dappankuthu", "thara local", "oru kuchi", "madura", "karuppu nerathazhagi",
        "chinna machan", "sandakozhi", "paruthiveeran", "sooravali", "komban", "asuran",
        "nattu koothu", "folk", "therukural", "oppari", "villupaattu", "gumbalaga suthuvom",
        "morattu single", "vaadi pulla vaadi", "chill bro", "kattikida", "kattu kattu", "machan meesa"
    )

    private val GANA_ARTISTS = listOf(
        "gana bala", "marana gana viji", "gana stephen", "gana prabha",
        "gana ulaganathan", "gana vinoth", "gana muthu", "anthony daasan",
        "velmurugan", "deva", "thenisai thendral", "kidakkuzhi sabaritha",
        "chinna ponnu", "rokesh", "kabilan vairamuthu"
    )

    private val INTRO_MASS_KEYWORDS = listOf(
        "intro", "mass", "hero", "entry", "hukum", "alappara", "naa ready",
        "badass", "hunter", "vantaar", "matta", "whistle", "neruppu", "marana",
        "vaathi", "dharala", "jalabulanjangu", "arabic kuthu", "verithanam",
        "aalaporaan", "theemai dhaan", "beast mode", "leo", "jailer",
        "master", "petta", "kabali", "mersal", "vikram", "vettaiyan", "kanguva",
        "bloody", "power", "roar", "tiger", "singam", "attitude", "anthem", "theme", "bgm"
    )

    private val MELODY_KEYWORDS = listOf(
        "melody", "love", "kadhal", "kaadhal", "kanave", "unakkul", "nenjukkul",
        "vaseegara", "munbe vaa", "romantic", "romance", "soul", "feel good", "marakkuma",
        "malare", "mudhal nee", "pirai", "enodu", "thalli pogathey", "kadhalaada",
        "megham karukatha", "anbil avan", "kannazhaga", "poove", "oru manam",
        "kurumugil", "vennilave", "roja", "minnale", "vinnaithaandi", "jeans",
        "alaipayuthey", "vizhiyil", "uyirin", "aaruyire", "poove sempoove",
        "ennodu nee irundhal", "unakkenna venum sollu", "kadhale kadhale",
        "kannamma", "anbe", "uyire", "thaen thaen", "mayakkama", "un perai solla",
        "suttum vizhi", "oru dheyvam thantha", "pookkal pookkum", "hosanna", "en jeevan",
        "new york nagaram", "anbil", "innum konjam neram", "maruvaarthai", "un vizhigalil",
        "kannaana kanney", "kanave kanave", "neeyum naanum", "avalum naanum"
    )

    private val KUTHU_KEYWORDS = listOf(
        "kuthu", "party", "dance", "dappankuthu", "rowdy baby", "kaavaalaa",
        "local", "chilla", "goli soda", "dandanakka", "machan", "donu donu",
        "sodakku", "sarattu vandiyila", "sarakku", "vaathi coming", "dippam dappam",
        "thee thalapathy", "nakku mukka", "tasakku", "appa takkaru"
    )

    private val SAD_KEYWORDS = listOf(
        "sad", "breakup", "pain", "kanneer", "pirivu", "sogam", "valigal",
        "thanimai", "pogadha pogadha", "en kanmani", "nenje nenje", "kannukulla",
        "valikidhu", "po nee po", "yen ennai pirindhai", "idhu varai", "yaaro ivan"
    )

    private val MOTIVATION_KEYWORDS = listOf(
        "motivation", "motivational", "inspiring", "inspiration", "confidence", "hard work",
        "struggle", "success", "vetri", "vettri", "poradu", "saadhithu", "saadhikkalaam",
        "nambikkai", "kanavugal", "uyarvu", "valarum", "singapenney", "ethir neechal",
        "oruvan oruvan", "vetri kodi kattu", "vaazhkai", "velaiyilla pattathari", "vip",
        "surviva", "neruppu da", "believer", "unstoppable", "hall of fame",
        "aalaporaan thamizhan", "aarambam", "thunivom", "vidumurai", "padayappa", "baba",
        "anbe sivam", "jeithu", "jeippom", "vijayam", "dhillu", "veera", "porattam",
        "valigalai thaandi", "kanavu", "ezhunthu vaa", "thuninthu nil", "nimirndhu nil",
        "unnal mudiyum", "vada chennai", "soorarai pottru", "rise", "champion", "warrior", "anthem"
    )

    private val DEVOTIONAL_KEYWORDS = listOf(
        "devotional", "bakthi", "bhajan", "god", "murugan", "shivan", "siva", "ayyappa",
        "ayyappan", "vinayagar", "ganesha", "amman", "krishna", "perumal", "vishnu",
        "venkateshwara", "tirupati", "hanuman", "jesus", "allah", "kavasam", "namavali",
        "suprabhatam", "sairam", "sai baba", "temple", "pooja", "aarathi", "slokam",
        "stotram", "mahaan", "gayatri mantra", "om namah shivaya", "harivarasanam"
    )

    private val DEVOTIONAL_ARTISTS = listOf(
        "tms", "t.m. soundararajan", "seerkazhi govindarajan", "k.j. yesudas", "veeramani",
        "bombay saradha", "mahanadhi shobana", "l.r. eswari"
    )

    private val NON_TAMIL_LANGUAGES = listOf(
        "telugu", "hindi", "malayalam", "kannada", "punjabi", "bhojpuri",
        "english", "bengali", "marathi", "gujarati"
    )

    private fun normalize(text: String?): String {
        return (text ?: "")
            .lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun detectSongLanguage(song: YouTubeSong): String {
        if (song.language.isNotBlank()) {
            return song.language.lowercase().trim()
        }

        val combined = "${song.title} ${song.channelTitle}".lowercase()

        // 1. Unicode script detection
        if (Regex("[\\u0B80-\\u0BFF]").containsMatchIn(combined)) return "tamil"
        if (Regex("[\\u0C00-\\u0C7F]").containsMatchIn(combined)) return "telugu"
        if (Regex("[\\u0D00-\\u0D7F]").containsMatchIn(combined)) return "malayalam"
        if (Regex("[\\u0C80-\\u0CFF]").containsMatchIn(combined)) return "kannada"
        if (Regex("[\\u0900-\\u097F]").containsMatchIn(combined)) return "hindi"
        if (Regex("[\\u0A00-\\u0A7F]").containsMatchIn(combined)) return "punjabi"

        // 2. Language keyword tokens or brackets: e.g. [Tamil], (Telugu), "Tamil Song", etc.
        val langPatterns = listOf(
            "tamil" to listOf("tamil", "kollywood"),
            "telugu" to listOf("telugu", "tollywood"),
            "malayalam" to listOf("malayalam", "mollywood"),
            "kannada" to listOf("kannada", "sandalwood"),
            "hindi" to listOf("hindi", "bollywood"),
            "punjabi" to listOf("punjabi"),
            "english" to listOf("english", "hollywood")
        )

        for ((lang, keywords) in langPatterns) {
            for (kw in keywords) {
                if (Regex("\\b$kw\\b", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
                    return lang
                }
            }
        }

        // 3. Prominent Artist / Label heuristic
        if (Regex("anirudh|a\\.?r\\.?\\s?rahman|arr\\b|yuvan|u1\\b|harris jayaraj|vidyasagar|deva\\b|ilaiyaraaja|ilayaraja|santhosh narayanan|sana\\b|g\\.?v\\.?\\s?prakash|dhibu|sai abhyankkar|pradeep kumar|dhanush|sivaangi|jonita|gana bala|marana gana|anthony daasan|hiphop tamizha|sean roldan|sam cs|d imman|vijay antony", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "tamil"
        }
        if (Regex("thaman|devi sri prasad|dsp\\b|keeravani|ram miriyala|anurag kulkarni|mangli|aditya music|lahari music", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "telugu"
        }
        if (Regex("sushin shyam|dabzee|jassie gift|shaan rahman|gopi sundar|hesham|muzin|manorama music|satyam videos", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "malayalam"
        }
        if (Regex("arijit singh|pritam|badshah|shreya ghoshal|amitabh bhattacharya|armaan malik|neha kakkar|kumar sanu|kishore kumar|udit narayan|sonu nigam|atif aslam|t-series|zee music", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "hindi"
        }
        if (Regex("diljit dosanjh|sidhu moose|ap dhillon|karan aujla|honey singh|guru randhawa|ammy virk|b praak|speed records", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "punjabi"
        }
        if (Regex("ed sheeran|taylor swift|billie eilish|the weeknd|dua lipa|coldplay|eminem|drake|post malone|bruno mars|justin bieber|alan walker|maroon 5|imagine dragons", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "english"
        }

        return ""
    }

    fun isSongInLanguage(song: YouTubeSong, preferredLanguages: List<String>): Boolean {
        val normLangs = preferredLanguages.map { it.lowercase().trim() }.filter { it.isNotEmpty() }
        if (normLangs.isEmpty()) return true

        val detected = detectSongLanguage(song)
        if (detected.isNotBlank()) {
            return normLangs.contains(detected)
        }

        // If not clearly detected yet, reject if it explicitly contains any non-preferred language keyword
        val allKnownLangs = listOf("tamil", "telugu", "hindi", "malayalam", "kannada", "punjabi", "english", "bhojpuri", "bengali", "marathi", "gujarati")
        val nonPreferred = allKnownLangs.filter { !normLangs.contains(it) }
        val text = "${song.title} ${song.channelTitle}".lowercase()
        for (np in nonPreferred) {
            val regex = Regex("\\b$np\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(text)) {
                return false
            }
        }

        return normLangs.contains("tamil")
    }

    fun extractPrimaryArtist(channelOrArtist: String?): String {
        val norm = normalize(channelOrArtist)
            .replace(Regex("topic|official|vevo|channel|sun tv|sony music south|think music india|wunderbar films"), "")
            .trim()

        return when {
            norm.contains("anirudh") -> "anirudh ravichander"
            norm.contains("rahman") || norm.contains("a r rahman") || norm.contains("a.r.") -> "a r rahman"
            norm.contains("yuvan") || norm.contains("u1") -> "yuvan shankar raja"
            norm.contains("harris") -> "harris jayaraj"
            norm.contains("santhosh narayanan") || norm.contains("sana") -> "santhosh narayanan"
            norm.contains("g v prakash") || norm.contains("gv prakash") -> "g v prakash"
            norm.contains("ilaiyaraaja") || norm.contains("ilayaraja") -> "ilaiyaraaja"
            norm.contains("sid sriram") -> "sid sriram"
            norm.contains("gana bala") -> "gana bala"
            norm.contains("marana gana viji") -> "marana gana viji"
            norm.contains("anthony daasan") -> "anthony daasan"
            norm.contains("deva") -> "deva"
            else -> norm.split(" ").firstOrNull() ?: ""
        }
    }

    fun detectSongMood(song: YouTubeSong): SongMood {
        val text = normalize("${song.title} ${song.channelTitle}")

        // 1. Devotional check first (spiritual songs must never be mixed with cinema love/kuthu)
        for (artist in DEVOTIONAL_ARTISTS) {
            if (text.contains(artist)) return SongMood.DEVOTIONAL
        }
        for (kw in DEVOTIONAL_KEYWORDS) {
            if (text.contains(kw)) return SongMood.DEVOTIONAL
        }

        // 2. Motivational / Inspiring (e.g. Vetri Kodi Kattu, Singapenney, Ethir Neechal, VIP, Believer)
        for (kw in MOTIVATION_KEYWORDS) {
            if (text.contains(kw)) return SongMood.MOTIVATION_INSPIRING
        }

        // 3. Gaana / Folk
        for (artist in GANA_ARTISTS) {
            if (text.contains(artist)) return SongMood.GANA_FOLK
        }
        for (kw in GANA_KEYWORDS) {
            if (text.contains(kw)) return SongMood.GANA_FOLK
        }

        // 4. Sad / Heartbreak
        for (kw in SAD_KEYWORDS) {
            if (text.contains(kw)) return SongMood.SAD_HEARTBREAK
        }

        // 5. Intro / Mass hero anthems
        for (kw in INTRO_MASS_KEYWORDS) {
            if (text.contains(kw)) return SongMood.INTRO_MASS
        }

        // 6. Party / Kuthu fast beats
        for (kw in KUTHU_KEYWORDS) {
            if (text.contains(kw)) return SongMood.PARTY_KUTHU
        }

        // 7. Melodies / Romantic love songs
        for (kw in MELODY_KEYWORDS) {
            if (text.contains(kw)) return SongMood.MELODY_ROMANCE
        }

        return SongMood.GENERAL
    }

    fun scoreSongRelevance(
        target: YouTubeSong,
        candidate: YouTubeSong,
        preferredLanguages: List<String> = listOf("tamil")
    ): Int {
        if (target.videoId == candidate.videoId) return 9999

        if (!isSongInLanguage(candidate, preferredLanguages)) {
            return -999
        }

        var score = 0
        val targetMood = detectSongMood(target)
        val candidateMood = detectSongMood(candidate)

        // 1. Strict Mood Matching & Filtering
        if (targetMood == candidateMood && targetMood != SongMood.GENERAL) {
            score += 100 // Exact mood match (Love stays with Love, Motivation with Motivation)
        } else if (targetMood == SongMood.MOTIVATION_INSPIRING) {
            if (candidateMood == SongMood.INTRO_MASS) {
                score += 35 // Mass / Inspiring have high energetic synergy
            } else {
                return -100 // NEVER put soft love songs, sad breakup, or local gaana into motivation queue
            }
        } else if (targetMood == SongMood.MELODY_ROMANCE) {
            if (candidateMood == SongMood.SAD_HEARTBREAK) {
                score += 20 // Soft romantic melodies and emotional soul tracks share acoustic vibe
            } else {
                return -100 // NEVER put loud party kuthu, mass intro, or gaana into love melody queue
            }
        } else if (targetMood == SongMood.DEVOTIONAL) {
            if (candidateMood == SongMood.DEVOTIONAL) {
                score += 120
            } else {
                return -500 // Strict: Devotional sessions only allow devotional tracks
            }
        } else if (targetMood == SongMood.SAD_HEARTBREAK) {
            if (candidateMood == SongMood.MELODY_ROMANCE) {
                score += 25
            } else {
                return -100 // Never put celebration kuthu into sad breakup queue
            }
        } else if (targetMood == SongMood.PARTY_KUTHU) {
            if (candidateMood == SongMood.GANA_FOLK) {
                score += 50
            } else if (candidateMood == SongMood.INTRO_MASS) {
                score += 40
            } else {
                return -100 // Never put slow romantic melodies into party kuthu queue
            }
        } else if (targetMood == SongMood.GANA_FOLK) {
            if (candidateMood == SongMood.PARTY_KUTHU) {
                score += 50
            } else {
                return -100
            }
        } else if (targetMood == SongMood.INTRO_MASS) {
            if (candidateMood == SongMood.PARTY_KUTHU) {
                score += 45
            } else if (candidateMood == SongMood.MOTIVATION_INSPIRING) {
                score += 40
            } else {
                return -100
            }
        }

        // 2. Composer / Artist Match
        val targetArtist = extractPrimaryArtist(target.channelTitle)
        val candidateArtist = extractPrimaryArtist(candidate.channelTitle)
        if (targetArtist.isNotBlank() && candidateArtist.isNotBlank() && targetArtist == candidateArtist) {
            score += 35
        }

        // 3. Title / Movie Match
        val targetTokens = normalize(target.title).split(" ").filter { it.length > 3 }
        val candText = normalize(candidate.title)
        for (token in targetTokens) {
            if (candText.contains(token)) {
                score += 20
                break
            }
        }

        // 4. Preferred Language Boost
        for (lang in preferredLanguages) {
            if (candText.contains(lang.lowercase())) {
                score += 20
                break
            }
        }

        return score
    }

    fun getRelevantSearchQuery(song: YouTubeSong, preferredLang: String = "tamil"): String {
        val lang = if (preferredLang.isNotBlank()) preferredLang.lowercase().trim() else "tamil"
        val mood = detectSongMood(song)
        val artist = extractPrimaryArtist(song.channelTitle)
        val artistPrefix = if (artist.isNotBlank()) "$artist " else ""

        return when (mood) {
            SongMood.MOTIVATION_INSPIRING -> "$lang motivational inspiring confidence success hit songs $artistPrefix"
            SongMood.MELODY_ROMANCE -> "$lang feel good romantic love melody hit songs $artistPrefix"
            SongMood.DEVOTIONAL -> "$lang devotional bakthi songs $artistPrefix temple prayers"
            SongMood.GANA_FOLK -> "$lang gana songs ${artistPrefix}super hit marana gana kuthu"
            SongMood.PARTY_KUTHU -> "$lang party kuthu dance fast beat songs $artistPrefix"
            SongMood.INTRO_MASS -> "$lang mass hero intro entry hit songs $artistPrefix"
            SongMood.SAD_HEARTBREAK -> "$lang sad breakup emotional hit songs $artistPrefix"
            else -> {
                if (artist.isNotBlank()) "$artist $lang super hit songs"
                else "$lang top trending hit songs"
            }
        }
    }

    fun cleanBaseTitle(rawTitle: String?): String {
        if (rawTitle.isNullOrBlank()) return ""
        return rawTitle
            .lowercase()
            .replace(Regex("^(official\\s*(video|audio|lyric(al)?\\s*video)?|lyric(al)?\\s*video|video\\s*song|full\\s*song|audio\\s*song)\\s*[:|-]\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\s*[|–—:-].*"), "")
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .trim()
    }

    fun isSameSongOrDuplicate(songA: YouTubeSong, songB: YouTubeSong): Boolean {
        if (songA.videoId == songB.videoId) return true
        val baseA = cleanBaseTitle(songA.title)
        val baseB = cleanBaseTitle(songB.title)
        if (baseA.isEmpty() || baseB.isEmpty()) return false
        if (baseA == baseB) return true
        if (baseA.length >= 5 && baseB.contains(baseA)) return true
        if (baseB.length >= 5 && baseA.contains(baseB)) return true
        return false
    }

    fun buildRelevantQueue(
        target: YouTubeSong,
        candidatePool: List<YouTubeSong>,
        preferredLanguages: List<String> = listOf("tamil"),
        maxItems: Int = 50
    ): List<YouTubeSong> {
        val normLangs = preferredLanguages.ifEmpty { listOf("tamil") }

        val others = candidatePool.filter {
            it.videoId != target.videoId &&
            !isSameSongOrDuplicate(target, it) &&
            isSongInLanguage(it, normLangs)
        }
        val sortedOthers = others
            .map { it to scoreSongRelevance(target, it, normLangs) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        val seenBaseTitles = mutableSetOf<String>()
        val targetBase = cleanBaseTitle(target.title)
        if (targetBase.isNotEmpty()) seenBaseTitles.add(targetBase)

        val distinctOthers = mutableListOf<YouTubeSong>()
        for (cand in sortedOthers) {
            val base = cleanBaseTitle(cand.title)
            if (base.isNotEmpty() && !seenBaseTitles.contains(base) && !isSameSongOrDuplicate(target, cand)) {
                seenBaseTitles.add(base)
                distinctOthers.add(cand)
            }
        }

        return (listOf(target) + distinctOthers).take(maxItems)
    }
}
