package com.saavn.music.util

import com.saavn.music.data.model.YouTubeSong

enum class SongMood {
    GANA_FOLK,
    INTRO_MASS,
    MELODY_ROMANCE,
    PARTY_KUTHU,
    SAD_HEARTBREAK,
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
        "surviva", "aalaporaan", "theemai dhaan", "beast mode", "leo", "jailer",
        "master", "petta", "kabali", "mersal", "vikram", "vettaiyan", "kanguva",
        "bloody", "power", "roar", "tiger", "singam", "attitude", "anthem", "theme", "bgm"
    )

    private val MELODY_KEYWORDS = listOf(
        "melody", "love", "kadhal", "kaadhal", "kanave", "unakkul", "nenjukkul",
        "vaseegara", "munbe vaa", "romantic", "soul", "feel good", "marakkuma",
        "malare", "mudhal nee", "pirai", "enodu", "thalli pogathey", "kadhalaada",
        "megham karukatha", "anbil avan", "kannazhaga", "poove", "oru manam",
        "kurumugil", "vennilave", "roja", "minnale", "vinnaithaandi", "jeans",
        "alaipayuthey", "vizhiyil", "uyirin", "aaruyire", "poove sempoove",
        "ennodu nee irundhal", "unakkenna venum sollu", "kadhale kadhale",
        "kannamma", "anbe", "uyire", "thaen thaen", "mayakkama", "un perai solla",
        "suttum vizhi", "oru dheyvam thantha", "pookkal pookkum"
    )

    private val KUTHU_KEYWORDS = listOf(
        "kuthu", "party", "dance", "dappankuthu", "rowdy baby", "kaavaalaa",
        "local", "chilla", "goli soda", "dandanakka", "machan", "donu donu",
        "sodakku", "sarattu vandiyila", "sarakku", "vaathi coming", "dippam dappam",
        "thee thalapathy"
    )

    private val SAD_KEYWORDS = listOf(
        "sad", "breakup", "pain", "kanneer", "pirivu", "sogam", "valigal",
        "thanimai", "pogadha pogadha", "en kanmani", "nenje nenje", "kannukulla"
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

    fun isSongInLanguage(song: YouTubeSong, preferredLanguages: List<String>): Boolean {
        val normLangs = preferredLanguages.map { it.lowercase().trim() }.filter { it.isNotEmpty() }
        if (normLangs.isEmpty()) return true

        val text = "${song.title} ${song.channelTitle}".lowercase()

        for (otherLang in NON_TAMIL_LANGUAGES) {
            if (!normLangs.contains(otherLang)) {
                val regex = Regex("\\b$otherLang\\b", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(text)) {
                    return false
                }
            }
        }
        return true
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

        for (artist in GANA_ARTISTS) {
            if (text.contains(artist)) return SongMood.GANA_FOLK
        }
        for (kw in GANA_KEYWORDS) {
            if (text.contains(kw)) return SongMood.GANA_FOLK
        }
        for (kw in SAD_KEYWORDS) {
            if (text.contains(kw)) return SongMood.SAD_HEARTBREAK
        }
        for (kw in INTRO_MASS_KEYWORDS) {
            if (text.contains(kw)) return SongMood.INTRO_MASS
        }
        for (kw in KUTHU_KEYWORDS) {
            if (text.contains(kw)) return SongMood.PARTY_KUTHU
        }
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

        // 1. Mood Matching & Synergies
        if (targetMood == candidateMood && targetMood != SongMood.GENERAL) {
            score += 90
        } else if ((targetMood == SongMood.GANA_FOLK && candidateMood == SongMood.PARTY_KUTHU) ||
            (targetMood == SongMood.PARTY_KUTHU && candidateMood == SongMood.GANA_FOLK)) {
            score += 50
        } else if ((targetMood == SongMood.INTRO_MASS && candidateMood == SongMood.PARTY_KUTHU) ||
            (targetMood == SongMood.PARTY_KUTHU && candidateMood == SongMood.INTRO_MASS)) {
            score += 45
        } else if ((targetMood == SongMood.GANA_FOLK && (candidateMood == SongMood.MELODY_ROMANCE || candidateMood == SongMood.SAD_HEARTBREAK)) ||
            ((targetMood == SongMood.MELODY_ROMANCE || targetMood == SongMood.SAD_HEARTBREAK) && candidateMood == SongMood.GANA_FOLK)) {
            score -= 100 // Never mix Gaana with slow melody
        } else if ((targetMood == SongMood.INTRO_MASS && candidateMood == SongMood.MELODY_ROMANCE) ||
            (targetMood == SongMood.MELODY_ROMANCE && candidateMood == SongMood.INTRO_MASS)) {
            score -= 60
        } else if ((targetMood == SongMood.PARTY_KUTHU && candidateMood == SongMood.MELODY_ROMANCE) ||
            (targetMood == SongMood.MELODY_ROMANCE && candidateMood == SongMood.PARTY_KUTHU)) {
            score -= 60
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
            SongMood.GANA_FOLK -> "$lang gana songs ${artistPrefix}super hit marana gana kuthu"
            SongMood.PARTY_KUTHU -> "$lang party kuthu dance fast beat songs $artistPrefix"
            SongMood.INTRO_MASS -> "$lang mass hero intro entry hit songs $artistPrefix"
            SongMood.MELODY_ROMANCE -> "$lang feel good romantic love melody hit songs $artistPrefix"
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
