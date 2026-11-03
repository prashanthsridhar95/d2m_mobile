package com.d2m.app.ui.components

import com.d2m.app.ui.strings.AppLocale

/**
 * Chart display names in the app language (web: lib/astroNames.js). The backend and the chart grid use English/Tamil
 * transliterated names ("Aries", "Mesham", "Ashlesha", "Venus"); this maps them to Hindi/Tamil and falls back to the
 * original text when a value is unknown, so nothing disappears.
 */
internal object AstroNames {
    private fun norm(s: String) = s.lowercase().filter { it in 'a'..'z' }

    // index 0 = Mesham .. 11 = Meenam
    private val signTa = listOf("மேஷம்", "ரிஷபம்", "மிதுனம்", "கடகம்", "சிம்மம்", "கன்னி", "துலாம்", "விருச்சிகம்", "தனுசு", "மகரம்", "கும்பம்", "மீனம்")
    private val signHi = listOf("मेष", "वृषभ", "मिथुन", "कर्क", "सिंह", "कन्या", "तुला", "वृश्चिक", "धनु", "मकर", "कुम्भ", "मीन")

    fun sign(index: Int, english: String, l: AppLocale): String = when (l) {
        AppLocale.TA -> signTa.getOrNull(index) ?: english
        AppLocale.HI -> signHi.getOrNull(index) ?: english
        AppLocale.EN -> english
    }

    // abbreviation -> (ta, hi)
    private val abbr = mapOf(
        "Su" to ("சூ" to "सू"), "Mo" to ("சந்" to "चं"), "Ma" to ("செ" to "मं"), "Me" to ("பு" to "बु"),
        "Ju" to ("குரு" to "गु"), "Ve" to ("சுக்" to "शु"), "Sa" to ("சனி" to "श"), "Ra" to ("ரா" to "रा"),
        "Ke" to ("கே" to "के"), "Asc" to ("லக்" to "ल"),
    )

    fun abbr(value: String, l: AppLocale): String {
        val e = abbr[value] ?: return value
        return when (l) { AppLocale.TA -> e.first; AppLocale.HI -> e.second; AppLocale.EN -> value }
    }

    private class P(val al: List<String>, val ta: String, val hi: String)
    private val planets = listOf(
        P(listOf("sun", "surya"), "சூரியன்", "सूर्य"), P(listOf("moon", "chandra"), "சந்திரன்", "चंद्र"),
        P(listOf("mars", "mangal", "kuja"), "செவ்வாய்", "मंगल"), P(listOf("mercury", "budha"), "புதன்", "बुध"),
        P(listOf("jupiter", "guru", "brihaspati"), "குரு", "गुरु"), P(listOf("venus", "shukra"), "சுக்கிரன்", "शुक्र"),
        P(listOf("saturn", "shani"), "சனி", "शनि"), P(listOf("rahu"), "ராகு", "राहु"), P(listOf("ketu"), "கேது", "केतु"),
    )

    fun planet(value: String, l: AppLocale): String {
        if (l == AppLocale.EN) return value
        val p = planets.firstOrNull { norm(value) in it.al } ?: return value
        return if (l == AppLocale.TA) p.ta else p.hi
    }

    private val nakshatras = listOf(
        P(listOf("ashwini", "aswini"), "அஸ்வினி", "अश्विनी"), P(listOf("bharani"), "பரணி", "भरणी"),
        P(listOf("krittika", "karthigai"), "கார்த்திகை", "कृत्तिका"), P(listOf("rohini"), "ரோகிணி", "रोहिणी"),
        P(listOf("mrigashira", "mrigasira", "mrigasirisham", "mirugasirisham"), "மிருகசீரிடம்", "मृगशिरा"),
        P(listOf("ardra", "thiruvadirai", "arudra"), "திருவாதிரை", "आर्द्रा"), P(listOf("punarvasu", "punarpoosam"), "புனர்பூசம்", "पुनर्वसु"),
        P(listOf("pushya", "poosam", "pushyami"), "பூசம்", "पुष्य"), P(listOf("ashlesha", "aslesha", "ayilyam"), "ஆயில்யம்", "आश्लेषा"),
        P(listOf("magha", "magam"), "மகம்", "मघा"), P(listOf("purvaphalguni", "pooram"), "பூரம்", "पूर्वा फाल्गुनी"),
        P(listOf("uttaraphalguni", "uthiram"), "உத்திரம்", "उत्तर फाल्गुनी"), P(listOf("hasta", "hastham"), "ஹஸ்தம்", "हस्त"),
        P(listOf("chitra", "chithirai"), "சித்திரை", "चित्रा"), P(listOf("swati", "swathi", "svati"), "சுவாதி", "स्वाति"),
        P(listOf("vishakha", "visakam", "vishaka"), "விசாகம்", "विशाखा"), P(listOf("anuradha", "anusham"), "அனுஷம்", "अनुराधा"),
        P(listOf("jyeshtha", "jyestha", "kettai"), "கேட்டை", "ज्येष्ठा"), P(listOf("mula", "moola", "moolam"), "மூலம்", "मूल"),
        P(listOf("purvaashadha", "pooradam", "purvashadha"), "பூராடம்", "पूर्वाषाढ़ा"), P(listOf("uttaraashadha", "uthiradam", "uttarashadha"), "உத்திராடம்", "उत्तराषाढ़ा"),
        P(listOf("shravana", "sravana", "thiruvonam", "shravan"), "திருவோணம்", "श्रवण"), P(listOf("dhanishta", "dhanishtha", "avittam"), "அவிட்டம்", "धनिष्ठा"),
        P(listOf("shatabhisha", "satabhisha", "sathayam"), "சதயம்", "शतभिषा"), P(listOf("purvabhadrapada", "poorattathi", "purvabhadra"), "பூரட்டாதி", "पूर्वभाद्रपदा"),
        P(listOf("uttarabhadrapada", "uthirattathi", "uttarabhadra"), "உத்திரட்டாதி", "उत्तरभाद्रपदा"), P(listOf("revati", "revathi"), "ரேவதி", "रेवती"),
    )

    fun nakshatra(value: String, l: AppLocale): String {
        if (l == AppLocale.EN) return value
        val p = nakshatras.firstOrNull { norm(value) in it.al } ?: return value
        return if (l == AppLocale.TA) p.ta else p.hi
    }

    /** "5y 3m 12d" -> "5 ஆண்டு 3 மாதம் 12 நாள்". */
    fun duration(value: String, l: AppLocale): String {
        val words = when (l) {
            AppLocale.TA -> mapOf('y' to "ஆண்டு", 'm' to "மாதம்", 'd' to "நாள்")
            AppLocale.HI -> mapOf('y' to "वर्ष", 'm' to "माह", 'd' to "दिन")
            AppLocale.EN -> return value
        }
        return Regex("(\\d+)\\s*([ymd])\\b").replace(value) { m -> "${m.groupValues[1]} ${words[m.groupValues[2][0]]}" }
    }

    enum class L { RASI_D1, NAVAMSA_D9, LAGNA, RASI, NAKSHATRA, PADA, DASHA }

    fun label(k: L, l: AppLocale): String = when (l) {
        AppLocale.EN -> when (k) {
            L.RASI_D1 -> "RASI (D1)"; L.NAVAMSA_D9 -> "NAVAMSA (D9)"; L.LAGNA -> "Lagna"; L.RASI -> "Rasi"
            L.NAKSHATRA -> "Nakshatra"; L.PADA -> "Pada"; L.DASHA -> "Dasha"
        }
        AppLocale.HI -> when (k) {
            L.RASI_D1 -> "राशि (D1)"; L.NAVAMSA_D9 -> "नवांश (D9)"; L.LAGNA -> "लग्न"; L.RASI -> "राशि"
            L.NAKSHATRA -> "नक्षत्र"; L.PADA -> "पद"; L.DASHA -> "दशा"
        }
        AppLocale.TA -> when (k) {
            L.RASI_D1 -> "ராசி (D1)"; L.NAVAMSA_D9 -> "நவாம்சம் (D9)"; L.LAGNA -> "லக்னம்"; L.RASI -> "ராசி"
            L.NAKSHATRA -> "நட்சத்திரம்"; L.PADA -> "பாதம்"; L.DASHA -> "தசை"
        }
    }
}
