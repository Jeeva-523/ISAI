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
        "melody", "love", "kadhal", "kaadhal", "kadhalan", "kadhale", "kadhalum", "kadhalaada", "kadhalae",
        "kaadhalin", "kaadhale", "kaadhalan", "kadhali", "romance", "romantic", "soul", "soulful", "feel good",
        "heart", "sweet", "sweetheart", "duet", "kanave", "kanavugal", "nenjukkul", "nenjam", "nenjame", "nenjinile",
        "nenjukulla", "nenje", "nenjil", "marakkuma nenjam", "vaseegara", "munbe vaa", "thalli pogathey", "thalli pogadhae",
        "megham karukatha", "megam karukatha", "kannazhaga", "poove", "poovukku", "oru manam", "kurumugil", "vennilave",
        "vennilavae", "roja", "minnale", "vinnaithaandi", "jeans", "alaipayuthey", "vizhiyil", "vizhigal", "uyirin", "uyire",
        "uyirae", "aaruyire", "aaruyirae", "kannana", "kannaana", "kannaana kanney", "katchi sera", "unakkul", "unakkul naane",
        "pirai", "enodu", "ennodu", "poove sempoove", "ennodu nee irundhal", "unakkenna venum sollu", "kadhale kadhale",
        "kannamma", "kannammaa", "anbe", "anbae", "anbendra", "anbulla", "anbil", "anbil avan", "thaen thaen", "mayakkama",
        "un perai solla", "suttum vizhi", "oru dheyvam thantha", "pookkal pookkum", "pookal pookum", "hosanna", "en jeevan",
        "new york nagaram", "innum konjam neram", "maruvaarthai", "maruvarthai", "un vizhigalil", "kanave kanave", "neeyum naanum",
        "avalum naanum", "usure", "usuru", "usure poguthey", "usure pogudhey", "nira", "vizhi moodi", "partha mudhal",
        "paartha mudhal", "venmathi", "sirimathii", "yelo pullelo", "prema", "premam", "priya", "priyathama", "sakhi",
        "ninnila", "samajavaragamana", "geetha govindam", "chuttamalle", "valayapatti", "thangamey", "thangame", "sirikkadhey",
        "po indru neeyaga", "ey inge paaru", "oh penne", "bae", "spark", "mella mella", "aagayam", "mudhal mazhai", "anbae peranbae",
        "nenaithu nenaithu", "en navel", "adiye", "hasili fisili", "pennie", "penne", "yaakai", "vizhi", "kannil", "kannukulle",
        "kannula", "kannala", "kaatru", "kaatrukkenna", "malare", "mallipoo", "thooriga", "gundu malli", "dada", "sita ramam",
        "lover", "joe", "vtv", "vaaranam aayiram", "raja rani", "neethaane", "nanban", "chellamma", "siragugal", "pudhu vellai mazhai",
        "chinna chinna asai", "malare ninne", "darshana", "hridayam", "enathaney", "omahana", "azhage", "azhagiye", "orasaadha",
        "high on love", "kadhaippoma", "bodhaikaname", "marandaye", "parayuvaan", "aathangara marame", "senthoora", "yeno yeno",
        "kaatrae en kaatrae", "yaaro", "yaro", "thentral", "thendral", "pesum", "mounam", "kavidhai", "kavithai", "rasathi",
        "pesadha", "kannukulle", "pala palakurakkum", "kandaangi", "visiri", "adada mazhaida", "mazhaiye", "mazhaikuruvi",
        "pachai nirame", "snehidhane", "snegidhane", "evano oruvan", "annul maele", "mundhinam", "ava enna", "yennai arindhaal",
        "swasamae", "swasame", "thillana", "kandukondain", "aarariraro", "thaalattu", "kannukkul pothivaippen", "poovellam",
        "ullam ketkume", "mazhai thuli", "nenjil nenjil", "kanmoodi", "kaatru veliyidai", "vaan", "azhagiyae", "onakkaaga",
        "bodhai kodhai", "asuran ellu vaya", "adiga adiga", "inkem inkem", "butta bomma", "choosi choodangane", "chaleya",
        "heeriye", "kesariya", "tum hi ho", "channa mereya", "apna bana le", "o maahi", "ve kamleya", "samjhawan",
        "agar tum saath ho", "tere vaaste", "shayad", "hawayein", "phir aur kya chahiye", "sajni", "naina", "ishq",
        "mohabbat", "deewana", "pyaar", "dil", "unplugged melody", "acoustic love", "love duet", "melody hits", "love hits",
        "kadhal hits", "melody songs", "love songs", "romantic hits"
    )

    private val MELODY_ARTISTS = listOf(
        "sid sriram", "pradeep kumar", "shreya ghoshal", "chinmayi", "bombay jayashri",
        "haricharan", "karthik", "vijay prakash", "swarnalatha", "s.p. balasubrahmanyam", "spb",
        "s. janaki", "janaki", "chithra", "k.s. chithra", "ks chithra", "saindhavi",
        "kapil kapilan", "hesham abdul wahab", "stephen zechariah", "dhibu ninan thomas",
        "jonita gandhi", "shakthisree gopalan", "unni menon", "sujatha", "sujatha mohan", "sadhana sargam",
        "tippu", "andrea jeremiah", "sithara", "naresh iyer", "shashaa tirupati", "harini",
        "anuradha sriram", "chinmayee", "govind vasantha", "sean roldan", "alisha thomas", "siddhu kumar"
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
            val normalized = normalizeLanguage(song.language)
            if (normalized != "unknown") return normalized
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
            "tamil" to listOf("tamil", "kollywood", "tamizh"),
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

        // 3. Prominent Artist / Label / Title keyword heuristics (High-priority non-Tamil checks first)
        if (Regex("thaman|s thaman|devi sri prasad|dsp\\b|keeravani|m\\.?m\\.?\\s*keeravani|ram miriyala|anurag kulkarni|mangli|aditya music|lahari music|sri balaji|mango music|madhura audio|geetha arts|mythri|dvv entertainment|sithara entertainments|guntur kaaram|devara|salaar|kalki|game changer|pushpa 2|pushpa|saripodhaa|chuttamalle|daavudi|ayudha pooja|fear song|kurchi madathapetti|dum masala|samajavaragamana|butta bomma|ramuloo ramulaa|oo antava|srivalli|saami saami|dheera dheera|jaragandi|ta takkara|nee chuttu|maname|cheliya|prema|pilla|ninnu|nuvvu|nuvve|gundellona|priyathama|chusthu|kalagane|naa kosam|telusa", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "telugu"
        }
        if (Regex("arijit singh|pritam|badshah|amitabh bhattacharya|armaan malik|neha kakkar|kumar sanu|kishore kumar|udit narayan|sonu nigam|atif aslam|alka yagnik|lata mangeshkar|mohammed rafi|jubin nautiyal|vishal mishra|darshan raval|sachin-jigar|vishal-shekhar|mithoon|shankar-ehsaan-loy|yo yo honey singh|sunidhi chauhan|palak muchhal|t-series|tseries|zee music|tips official|yrf|yash raj|stree 2|munjya|bad newz|animal|jawan hindi|dunki|fighter|aayi nai|aaj ki raat|taras|tauba tauba|chaleya|zinda banda|arjan vailly|pehle bhi main|satranga|heeriye|kesariya|tum hi ho|channa mereya|apna bana le|o maahi|ve kamleya|agar tum saath|tere vaaste|lut gaye|raataan lambiyan|dilbar|ghungroo|soni soni|shayad|hawayein|phir aur kya|sajni|naina|pyaar|ishq|mohabbat|deewana|dhadkan|saathiya|tere bina|meri jaan|tujhe|khuda|zindagi|yaara|sanam|bewafa|humsafar|duniya", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "hindi"
        }
        if (Regex("sushin shyam|dabzee|jassie gift|shaan rahman|gopi sundar|hesham abdul wahab|muzik247|manorama music|satyam videos|satyam audios|sithara krishnakumar|ks harisankar|vineeth sreenivasan|vidhu prathap|sujatha mohan|premam|hridayam|malare ninne|darshana|onakkaaga|aavesham|manjummel boys|premalu|bramayugam|goatlife|aadujeevitham|arm malayalam|illuminati|kuthanthram|jaada|mathaappu|kathaalolam|periyone|oru chiri|neela nilave|alare|pavizha mazha", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "malayalam"
        }
        if (Regex("arjun janya|b ajaneesh loknath|charan raj|ravi basrur|v harikrishna|anand audio|jhankar music|a2 music|d beats|kgf|kantara|salaar kannada|martin kannada|bagheera|ui kannada|vijay prakash kannada|sanjith hegde kannada", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "kannada"
        }
        if (Regex("diljit dosanjh|sidhu moose|ap dhillon|karan aujla|honey singh|guru randhawa|ammy virk|b praak|speed records", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "punjabi"
        }
        if (Regex("ed sheeran|taylor swift|billie eilish|the weeknd|dua lipa|coldplay|eminem|drake|post malone|bruno mars|justin bieber|alan walker|maroon 5|imagine dragons|shawn mendes|charlie puth|selena gomez|ariana grande|olivia rodrigo|adele|chainsmokers|marshmello|david guetta|calvin harris", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "english"
        }
        if (Regex("anirudh|a\\.?r\\.?\\s?rahman|arr\\b|yuvan|u1\\b|harris jayaraj|vidyasagar|deva\\b|ilaiyaraaja|ilayaraja|santhosh narayanan|sana\\b|g\\.?v\\.?\\s?prakash|gv prakash|dhibu|sai abhyankkar|pradeep kumar|dhanush|sivaangi|jonita gandhi|gana bala|marana gana|anthony daasan|hiphop tamizha|sean roldan|sam cs|sam c\\.?s\\.?|d imman|vijay antony|govind vasantha|justin prabhakaran|sony music south|think music india|think indie|saregama tamil|lahari tamil|sun pictures|red giant|lyca|wunderbar|thalapathy|vijay\\b|ajith|rajini|kamal\\b|suriya|karthi|sivakarthikeyan|sk\\b|vikram\\b|silambarasan|simbu|vijay sethupathi|coolie|the greatest of all time|goat\\b|amaran|leo\\b|jailer|thug life|viduthalai|vettaiyan|retro", RegexOption.IGNORE_CASE).containsMatchIn(combined)) {
            return "tamil"
        }

        return ""
    }

    fun normalizeLanguage(lang: String?): String {
        if (lang.isNullOrBlank()) return "tamil"
        val clean = lang.trim().lowercase()
        return when (clean) {
            "ta", "tamil" -> "tamil"
            "te", "telugu" -> "telugu"
            "hi", "hindi" -> "hindi"
            "ml", "malayalam" -> "malayalam"
            "kn", "kannada" -> "kannada"
            "pa", "punjabi" -> "punjabi"
            "en", "english" -> "english"
            else -> clean
        }
    }

    fun formatLanguageDisplayName(lang: String?): String {
        val norm = normalizeLanguage(lang)
        return norm.replaceFirstChar { it.uppercase() }
    }

    fun isSongInLanguage(song: YouTubeSong, preferredLanguages: List<String>): Boolean {
        val normLangs = preferredLanguages.map { normalizeLanguage(it) }.filter { it.isNotEmpty() }
        if (normLangs.isEmpty()) return true

        // 1. Explicit song.language check
        if (song.language.isNotBlank()) {
            val songNorm = normalizeLanguage(song.language)
            if (songNorm != "unknown") {
                return normLangs.contains(songNorm)
            }
        }

        // 2. High-precision detected language check
        val detected = detectSongLanguage(song)
        if (detected.isNotBlank()) {
            return normLangs.contains(detected)
        }

        // 3. Strict rejection of non-preferred language indicators
        val allKnownLangs = listOf("tamil", "telugu", "hindi", "malayalam", "kannada", "punjabi", "english", "bhojpuri", "bengali", "marathi", "gujarati")
        val nonPreferred = allKnownLangs.filter { !normLangs.contains(it) }
        val text = "${song.title} ${song.channelTitle}".lowercase()
        for (np in nonPreferred) {
            val regex = Regex("\\b$np\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(text)) {
                return false
            }
        }

        // 4. If preferred language includes Tamil, verify it doesn't have non-Tamil artist/label traces
        if (normLangs.contains("tamil")) {
            val hasNonTamilTrace = Regex("t-series|tseries|zee music|tips official|yrf|aditya music|lahari music|muzik247|manorama music|anand audio|jhankar music|arijit|pritam|badshah|thaman|devi sri prasad|dsp\\b|sushin shyam|dabzee|diljit|sidhu moose|b praak", RegexOption.IGNORE_CASE).containsMatchIn(text)
            if (hasNonTamilTrace) {
                return false
            }
            return true
        }

        return false
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
        for (artist in MELODY_ARTISTS) {
            if (text.contains(artist)) return SongMood.MELODY_ROMANCE
        }
        for (kw in MELODY_KEYWORDS) {
            if (text.contains(kw)) return SongMood.MELODY_ROMANCE
        }

        return SongMood.GENERAL
    }

    val KNOWN_FILM_YEARS: Map<String, Int> = mapOf(
        // 1990s
        "roja" to 1992, "gentleman" to 1993, "kadhalan" to 1994, "bombay" to 1995,
        "baasha" to 1995, "muthu" to 1995, "indian" to 1996, "kadhal desam" to 1996, "kadhaldesam" to 1996,
        "ullathai allitha" to 1996, "suryavamsam" to 1997, "jeans" to 1998, "mudhalvan" to 1999,
        "padayappa" to 1999, "thulladha manamum thullum" to 1999, "vaali" to 1999, "sangamam" to 1999,
        "kandukondain" to 2000, "alaipayuthey" to 2000, "kushi" to 2000, "rhythm" to 2000, "priyamanavale" to 2000,

        // 2000 - 2005 (The requested 2002 era window)
        "minnale" to 2001, "dhill" to 2001, "dheena" to 2001, "nandha" to 2001, "poovellam un vaasam" to 2001,
        "aanandham" to 2001, "shahjahan" to 2001, "run" to 2002, "gemini" to 2002, "mounam pesiyadhe" to 2002,
        "samurai" to 2002, "baba" to 2002, "panchathanthiram" to 2002, "red" to 2002, "villain" to 2002,
        "roja kootam" to 2002, "five star" to 2002, "bagavathi" to 2002, "thamizhan" to 2002, "youth" to 2002,
        "dhool" to 2003, "saamy" to 2003, "kaakha kaakha" to 2003, "boys" to 2003, "thirumalai" to 2003,
        "pithamagan" to 2003, "ghilli" to 2004, "aayutha ezhuthu" to 2004, "vasoolraja" to 2004, "autograph" to 2004,
        "manmadhan" to 2004, "7g rainbow colony" to 2004, "7g" to 2004, "new" to 2004, "madhurey" to 2004,
        "anniyan" to 2005, "chandramukhi" to 2005, "ghajini" to 2005, "sachein" to 2005, "thirupaachi" to 2005,
        "kanda naal mudhal" to 2005,

        // 2006 - 2010
        "sillunu oru kadhal" to 2006, "pudhupettai" to 2006, "vettaiyaadu vilaiyaadu" to 2006,
        "pokkiri" to 2007, "sivaji" to 2007, "billa" to 2007, "paruthiveeran" to 2007, "polladhavan" to 2007,
        "dasavathaaram" to 2008, "vaaranam aayiram" to 2008, "subramaniapuram" to 2008, "ayan" to 2009, "aadhavan" to 2009,
        "padikathavan" to 2009, "paiyaa" to 2010, "vinnaithaandi varuvaayaa" to 2010, "madharasapattinam" to 2010,
        "enthiran" to 2010, "singam" to 2010,

        // 2011 - 2015
        "mankatha" to 2011, "mayakkam enna" to 2011, "7aum arivu" to 2011, "3" to 2012, "thuppakki" to 2012,
        "nanban" to 2012, "raja rani" to 2013, "maryan" to 2013, "vanakkam chennai" to 2013, "kaththi" to 2014,
        "vip" to 2014, "jigarthanda" to 2014, "i" to 2015, "vedalam" to 2015, "naanum rowdy dhaan" to 2015,
        "ok kanmani" to 2015,

        // 2016 - 2025
        "theri" to 2016, "kabali" to 2016, "24" to 2016, "mersal" to 2017, "vikram vedha" to 2017,
        "kaatru veliyidai" to 2017, "96" to 2018, "sarkar" to 2018, "vada chennai" to 2018, "pyaar prema kaadhal" to 2018,
        "bigil" to 2019, "petta" to 2019, "viswasam" to 2019, "kaithi" to 2019, "asuran" to 2019,
        "soorarai pottru" to 2020, "master" to 2021, "doctor" to 2021, "karnan" to 2021, "vikram" to 2022,
        "ponniyin selvan" to 2022, "thiruchitrambalam" to 2022, "beast" to 2022, "love today" to 2022,
        "jailer" to 2023, "leo" to 2023, "varisu" to 2023, "chithha" to 2023, "joe" to 2023,
        "lover" to 2024, "goat" to 2024, "vettaiyan" to 2024, "amaran" to 2024
    )

    fun extractReleaseYear(song: YouTubeSong): Int? {
        if (!song.year.isNullOrBlank()) {
            val y = song.year.trim().toIntOrNull()
            if (y != null && y in 1950..2030) return y
        }

        val cleanTitle = normalize(song.title)
        val cleanChannel = normalize(song.channelTitle)
        val combined = " $cleanTitle $cleanChannel "

        for ((film, year) in KNOWN_FILM_YEARS) {
            val normFilm = normalize(film)
            if (normFilm.length >= 2 && combined.contains(" $normFilm ")) {
                return year
            }
        }

        val textToScan = "${song.title} ${song.channelTitle}"
        val regex = Regex("\\b(19[6-9]\\d|20[0-2]\\d)\\b")
        for (match in regex.findAll(textToScan)) {
            val y = match.value.toIntOrNull() ?: continue
            val idx = match.range.first
            val after = if (idx + 4 < textToScan.length) textToScan[idx + 4] else ' '
            val before = if (idx > 0) textToScan[idx - 1] else ' '
            if (after.lowercaseChar() in listOf('p', 'k', 'x') || before.lowercaseChar() in listOf('p', 'k', 'x')) continue
            if (y in 1960..2026) return y
        }

        return null
    }

    fun detectSongEra(song: YouTubeSong): String? {
        val y = extractReleaseYear(song) ?: return null
        return when {
            y < 1980 -> "70s"
            y < 1990 -> "80s"
            y < 2000 -> "90s"
            y < 2010 -> "2000s"
            y < 2020 -> "2010s"
            else -> "2020s"
        }
    }

    fun scoreSongRelevance(
        target: YouTubeSong,
        candidate: YouTubeSong,
        preferredLanguages: List<String> = listOf("tamil"),
        sessionSeed: YouTubeSong? = null
    ): Int {
        if (target.videoId == candidate.videoId) return -999

        if (isPlaylistOrCompilation(candidate)) {
            return -9999
        }

        val targetLang = target.language.ifBlank { detectSongLanguage(target) }.lowercase().trim()
        val candidateLang = candidate.language.ifBlank { detectSongLanguage(candidate) }.lowercase().trim()

        // Strict cross-language barrier: If both songs have identified languages and they differ, strictly reject!
        if (targetLang.isNotBlank() && candidateLang.isNotBlank() && targetLang != candidateLang) {
            return -999
        }

        val requiredLangs = if (targetLang.isNotBlank()) listOf(targetLang) else preferredLanguages
        if (!isSongInLanguage(candidate, requiredLangs)) {
            return -999
        }

        var score = 0
        val targetMood = detectSongMood(target)
        val candidateMood = detectSongMood(candidate)

        // 1. Strict Mood Matching & Filtering
        if (targetMood == SongMood.DEVOTIONAL || candidateMood == SongMood.DEVOTIONAL) {
            if (targetMood == SongMood.DEVOTIONAL && candidateMood == SongMood.DEVOTIONAL) {
                score += 120
            } else {
                return -500 // Never mix devotional with love/mass/party songs
            }
        } else if (targetMood == candidateMood && targetMood != SongMood.GENERAL) {
            score += 100 // Exact mood match (Love stays with Love, Motivation with Motivation)
        } else if (targetMood == SongMood.MOTIVATION_INSPIRING) {
            if (candidateMood == SongMood.INTRO_MASS) {
                score += 35 // Mass / Inspiring have high energetic synergy
            } else {
                return -100 // NEVER put soft love songs, sad breakup, or local gaana into motivation queue
            }
        } else if (targetMood == SongMood.MELODY_ROMANCE) {
            if (candidateMood == SongMood.SAD_HEARTBREAK) {
                val candText = normalize("${candidate.title} ${candidate.channelTitle}")
                val hasLoveElement = MELODY_KEYWORDS.any { candText.contains(it) } || MELODY_ARTISTS.any { candText.contains(it) }
                if (hasLoveElement) {
                    score += 25
                } else {
                    return -100
                }
            } else {
                // Strict: Never allow GENERAL, KUTHU, INTRO_MASS, GANA, or DEVOTIONAL in Love Song queue!
                return -100
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

        // If mood is incompatible, candidate is already rejected. Artist similarity must NOT override mood!
        if (score <= 0 && targetMood != SongMood.GENERAL) {
            return -100
        }

        // 2. Composer / Artist Match (Only supports ranking if mood is already compatible)
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

        // 4. Era / Release Year Match
        val yearA = extractReleaseYear(target)
        val yearB = extractReleaseYear(candidate)
        if (yearA != null && yearB != null) {
            val diff = kotlin.math.abs(yearA - yearB)
            when {
                diff <= 3 -> score += 45 // Very close era match (e.g. 2002 and 2001/2003)
                diff <= 6 -> score += 30 // Within ±5 years window
                diff <= 10 -> score += 15 // Nearby era
                diff <= 15 -> score += 0
                diff <= 20 -> score -= 30 // Penalize distant era drift
                else -> score -= 60 // Heavily de-prioritize distant era (e.g. 2024 track when target is 2002)
            }
        }

        // 5. Session Seed Drift Prevention during continuous autoplay
        if (sessionSeed != null && sessionSeed.videoId != target.videoId) {
            val seedMood = detectSongMood(sessionSeed)
            if (candidateMood != seedMood) {
                score -= 80 // Strongly prevent drift away from initial seed mood
            }
            val seedYear = extractReleaseYear(sessionSeed)
            if (seedYear != null && yearB != null) {
                val seedDiff = kotlin.math.abs(seedYear - yearB)
                if (seedDiff > 12) score -= 25
                if (seedDiff > 20) score -= 50
            }
        }

        // 6. Preferred Language Boost
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
        val era = detectSongEra(song)?.let { "$it " } ?: ""

        return when (mood) {
            SongMood.MOTIVATION_INSPIRING -> "$lang ${era}motivational inspiring confidence success hit songs $artistPrefix"
            SongMood.MELODY_ROMANCE -> "$lang ${era}feel good romantic love melody hit songs $artistPrefix"
            SongMood.DEVOTIONAL -> "$lang devotional bakthi songs $artistPrefix temple prayers"
            SongMood.GANA_FOLK -> "$lang ${era}gana songs ${artistPrefix}super hit marana gana kuthu"
            SongMood.PARTY_KUTHU -> "$lang ${era}party kuthu dance fast beat songs $artistPrefix"
            SongMood.INTRO_MASS -> "$lang ${era}mass hero intro entry hit songs $artistPrefix"
            SongMood.SAD_HEARTBREAK -> "$lang ${era}sad breakup emotional hit songs $artistPrefix"
            else -> {
                if (artist.isNotBlank()) "$artist $lang ${era}super hit songs"
                else "$lang ${era}top trending hit songs"
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

        val langA = songA.language.ifBlank { detectSongLanguage(songA) }.lowercase().trim()
        val langB = songB.language.ifBlank { detectSongLanguage(songB) }.lowercase().trim()
        if (langA.isNotBlank() && langB.isNotBlank() && langA != langB) return false

        val tA = songA.title.lowercase()
        val tB = songB.title.lowercase()
        val hasTamilA = Regex("(?i)\\b(tamil)\\b").containsMatchIn(tA)
        val hasTamilB = Regex("(?i)\\b(tamil)\\b").containsMatchIn(tB)
        val hasTeluguA = Regex("(?i)\\b(telugu)\\b").containsMatchIn(tA)
        val hasTeluguB = Regex("(?i)\\b(telugu)\\b").containsMatchIn(tB)
        val hasHindiA = Regex("(?i)\\b(hindi)\\b").containsMatchIn(tA)
        val hasHindiB = Regex("(?i)\\b(hindi)\\b").containsMatchIn(tB)
        val hasMalayalamA = Regex("(?i)\\b(malayalam)\\b").containsMatchIn(tA)
        val hasMalayalamB = Regex("(?i)\\b(malayalam)\\b").containsMatchIn(tB)
        val hasKannadaA = Regex("(?i)\\b(kannada)\\b").containsMatchIn(tA)
        val hasKannadaB = Regex("(?i)\\b(kannada)\\b").containsMatchIn(tB)

        if ((hasTamilA && !hasTamilB && (hasTeluguB || hasHindiB || hasMalayalamB || hasKannadaB)) ||
            (hasTamilB && !hasTamilA && (hasTeluguA || hasHindiA || hasMalayalamA || hasKannadaA)) ||
            (hasTeluguA && !hasTeluguB && (hasTamilB || hasHindiB || hasMalayalamB || hasKannadaB)) ||
            (hasHindiA && !hasHindiB && (hasTamilB || hasTeluguB || hasMalayalamB || hasKannadaB))) {
            return false
        }

        val baseA = cleanBaseTitle(songA.title)
        val baseB = cleanBaseTitle(songB.title)
        if (baseA.isEmpty() || baseB.isEmpty()) return false
        if (baseA == baseB) return true
        if (baseA.length >= 5 && baseB.contains(baseA)) return true
        if (baseB.length >= 5 && baseA.contains(baseB)) return true
        return false
    }

    val PLAYLIST_EXCLUSION_REGEX = Regex(
        "(?i)\\b(p{1,2}laylist|jukebox|audio jukebox|video jukebox|full album|all songs|songs collection|best of|non stop|nonstop|compilation|mashup|mega mix|megamix|discography|top \\d+|hit songs collection|audio songs jukebox|video songs jukebox|evergreen hits jukebox|greatest hits jukebox|all hit songs)\\b"
    )

    fun isPlaylistOrCompilation(song: YouTubeSong): Boolean {
        return isPlaylistOrCompilationText(song.title) || isPlaylistOrCompilationText(song.channelTitle)
    }

    fun isPlaylistOrCompilationText(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        return PLAYLIST_EXCLUSION_REGEX.containsMatchIn(title)
    }

    fun buildRelevantQueue(
        target: YouTubeSong,
        candidatePool: List<YouTubeSong>,
        preferredLanguages: List<String> = listOf("tamil"),
        maxItems: Int = 50,
        minItems: Int = 25,
        sessionSeed: YouTubeSong? = null
    ): List<YouTubeSong> {
        val normLangs = preferredLanguages.ifEmpty { listOf("tamil") }

        val others = candidatePool.filter {
            it.videoId != target.videoId &&
            !isSameSongOrDuplicate(target, it) &&
            !isPlaylistOrCompilation(it) &&
            isSongInLanguage(it, normLangs)
        }
        val sortedOthers = others
            .map { it to scoreSongRelevance(target, it, normLangs, sessionSeed) }
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
            if (distinctOthers.size >= maxItems - 1) break
        }

        return (listOf(target) + distinctOthers).take(maxItems)
    }

    fun deduplicateSongs(songs: List<YouTubeSong>): List<YouTubeSong> {
        if (songs.isEmpty()) return emptyList()
        val result = mutableListOf<YouTubeSong>()
        for (song in songs) {
            if (song.title.isBlank() || isPlaylistOrCompilation(song)) continue
            if (result.none { isSameSongOrDuplicate(it, song) }) {
                result.add(song)
            }
        }
        return result
    }

    fun getSafeThumbnailUrl(thumbnailUrl: String?, videoId: String = ""): String {
        val raw = thumbnailUrl?.trim() ?: ""
        if (raw.isNotBlank() && !raw.contains("null") && !raw.endsWith("/default.jpg")) {
            return raw
        }
        if (videoId.isNotBlank() && videoId.length == 11) {
            return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        }
        return if (raw.isNotBlank() && !raw.contains("null")) raw else ""
    }
}
