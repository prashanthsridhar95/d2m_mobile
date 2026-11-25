package com.d2m.app.ui.strings

/** MatchCheckScreen.kt (+ the entry button on ParentHomeScreen) -- mirrors d2m_web's home_parent "mc_*" keys. */
interface MatchCheckStrings {
    val entry: String
    val title: String
    val intro: String
    val name: String
    val dob: String
    val tob: String
    val tobApprox: String
    val place: String
    val pickCity: String
    val marital: String
    val religion: String
    val community: String
    val education: String
    val income: String
    val profession: String
    val neutral: String
    val checking: String
    val submit: String
    val candidate: String
    val yourPrefs: String
    fun prefsUnmet(n: Int): String
    val communityBar: String
    val childPrefs: String
    val parentPrefs: String
    val notProvided: String
    val astrology: String
    val dealbreakers: String
    val cancelled: String
    val verifyTime: String
    val notSaved: String
    val edit: String
    val again: String
    val selectDate: String
    val selectTime: String
    val ok: String
    val cancel: String
    fun verdict(key: String): String
    fun tier(key: String): String
    fun part(key: String): String
}

object MatchCheckStringsEn : MatchCheckStrings {
    override val entry = "Check a match"
    override val title = "Check a match"
    override val intro = "Have someone's birth details? See how they fit with your child, using the same astrology and your saved preferences. We don't save these details."
    override val name = "Name"
    override val dob = "Date of birth"
    override val tob = "Time of birth"
    override val tobApprox = "I'm not sure of the exact time"
    override val place = "Place of birth"
    override val pickCity = "Pick a suggestion so coordinates and timezone fill in."
    override val marital = "Marital status"
    override val religion = "Religion"
    override val community = "Community"
    override val education = "Education"
    override val income = "Income band"
    override val profession = "Profession"
    override val neutral = "Anything you skip is treated as neutral, not as a mismatch."
    override val checking = "Checking…"
    override val submit = "Check compatibility"
    override val candidate = "Prospective match"
    override val yourPrefs = "Your preferences"
    override fun prefsUnmet(n: Int) = "This profile doesn't meet $n of your requirements."
    override val communityBar = "Community"
    override val childPrefs = "Child's preferences"
    override val parentPrefs = "Your soft preferences"
    override val notProvided = "Add religion / community to sharpen this result."
    override val astrology = "Astrology"
    override val dealbreakers = "Dealbreakers"
    override val cancelled = "cancelled"
    override val verifyTime = "Confirm the exact birth time for a firmer result."
    override val notSaved = "Not saved. These details are discarded when you leave this page."
    override val edit = "Edit details"
    override val again = "Check another"
    override val selectDate = "Select date"
    override val selectTime = "Select time"
    override val ok = "OK"
    override val cancel = "Cancel"
    override fun verdict(key: String) = when (key) {
        "strong_fit" -> "Strong fit"; "worth_exploring" -> "Worth exploring"
        "needs_discussion" -> "Needs discussion"; "not_recommended" -> "Not recommended"; else -> key
    }
    override fun tier(key: String) = when (key) {
        "TIER_1_PRIME_ALIGN" -> "Prime alignment"; "TIER_2_STRONG_POTENTIAL" -> "Strong potential"
        "TIER_3_TIMING_OR_RECIPROCITY_GATED" -> "Promising, timing-dependent"
        "TIER_4_LOW_ALIGNMENT" -> "Low alignment"; "DORMANT_BLOCKED" -> "Blocked"; else -> key
    }
    override fun part(key: String) = when (key) {
        "kp_core" -> "KP core"; "structural" -> "Structural"; "synastry" -> "Synastry"; "timing" -> "Timing"; else -> key
    }
}

object MatchCheckStringsHi : MatchCheckStrings {
    override val entry = "मैच जाँचें"
    override val title = "मैच जाँचें"
    override val intro = "किसी की जन्म जानकारी है? उसी ज्योतिष और आपकी सहेजी गई पसंद के आधार पर देखें कि वे आपके बच्चे के साथ कितने उपयुक्त हैं। हम ये जानकारी सहेजते नहीं हैं।"
    override val name = "नाम"
    override val dob = "जन्म तिथि"
    override val tob = "जन्म समय"
    override val tobApprox = "मुझे सही समय पता नहीं है"
    override val place = "जन्म स्थान"
    override val pickCity = "सूची से एक सुझाव चुनें ताकि निर्देशांक और समय क्षेत्र अपने आप भर जाएँ।"
    override val marital = "वैवाहिक स्थिति"
    override val religion = "धर्म"
    override val community = "समुदाय"
    override val education = "शिक्षा"
    override val income = "आय वर्ग"
    override val profession = "पेशा"
    override val neutral = "जो जानकारी आप छोड़ देंगे उसे तटस्थ माना जाएगा, बेमेल नहीं।"
    override val checking = "जाँच हो रही है…"
    override val submit = "अनुकूलता जाँचें"
    override val candidate = "संभावित रिश्ता"
    override val yourPrefs = "आपकी पसंद"
    override fun prefsUnmet(n: Int) = "यह प्रोफ़ाइल आपकी $n आवश्यकताओं को पूरा नहीं करती।"
    override val communityBar = "समुदाय"
    override val childPrefs = "बच्चे की पसंद"
    override val parentPrefs = "आपकी अन्य पसंद"
    override val notProvided = "इस परिणाम को बेहतर बनाने के लिए धर्म / समुदाय जोड़ें।"
    override val astrology = "ज्योतिष"
    override val dealbreakers = "निर्णायक बाधाएँ"
    override val cancelled = "निरस्त"
    override val verifyTime = "अधिक पक्के परिणाम के लिए सही जन्म समय की पुष्टि करें।"
    override val notSaved = "सहेजा नहीं गया। इस पेज से बाहर जाने पर ये जानकारी हटा दी जाती है।"
    override val edit = "जानकारी बदलें"
    override val again = "दूसरा जाँचें"
    override val selectDate = "तिथि चुनें"
    override val selectTime = "समय चुनें"
    override val ok = "ठीक है"
    override val cancel = "रद्द करें"
    override fun verdict(key: String) = when (key) {
        "strong_fit" -> "बहुत उपयुक्त"; "worth_exploring" -> "आगे बढ़ने योग्य"
        "needs_discussion" -> "चर्चा आवश्यक"; "not_recommended" -> "अनुशंसित नहीं"; else -> key
    }
    override fun tier(key: String) = when (key) {
        "TIER_1_PRIME_ALIGN" -> "उत्कृष्ट तालमेल"; "TIER_2_STRONG_POTENTIAL" -> "प्रबल संभावना"
        "TIER_3_TIMING_OR_RECIPROCITY_GATED" -> "आशाजनक, समय पर निर्भर"
        "TIER_4_LOW_ALIGNMENT" -> "कम तालमेल"; "DORMANT_BLOCKED" -> "अवरुद्ध"; else -> key
    }
    override fun part(key: String) = when (key) {
        "kp_core" -> "केपी मूल"; "structural" -> "संरचनात्मक"; "synastry" -> "सिनेस्ट्री"; "timing" -> "समय"; else -> key
    }
}

object MatchCheckStringsTa : MatchCheckStrings {
    override val entry = "பொருத்தம் பார்க்க"
    override val title = "பொருத்தம் பார்க்க"
    override val intro = "யாரோ ஒருவரின் பிறப்பு விவரங்கள் உள்ளனவா? அதே ஜோதிடம் மற்றும் நீங்கள் சேமித்த விருப்பங்களின்படி அவர்கள் உங்கள் குழந்தைக்கு எவ்வளவு பொருத்தம் என்று பாருங்கள். இந்த விவரங்களை நாங்கள் சேமிப்பதில்லை."
    override val name = "பெயர்"
    override val dob = "பிறந்த தேதி"
    override val tob = "பிறந்த நேரம்"
    override val tobApprox = "சரியான நேரம் எனக்குத் தெரியாது"
    override val place = "பிறந்த இடம்"
    override val pickCity = "பட்டியலில் இருந்து ஒரு பரிந்துரையைத் தேர்ந்தெடுங்கள்; ஆயத்தொலைவுகளும் நேர மண்டலமும் தானாக நிரப்பப்படும்."
    override val marital = "திருமண நிலை"
    override val religion = "மதம்"
    override val community = "சமூகம்"
    override val education = "கல்வி"
    override val income = "வருமான வரம்பு"
    override val profession = "தொழில்"
    override val neutral = "நீங்கள் தவிர்ப்பவை நடுநிலையாகக் கருதப்படும்; பொருத்தமின்மையாக அல்ல."
    override val checking = "சரிபார்க்கிறது…"
    override val submit = "பொருத்தத்தைச் சரிபார்க்கவும்"
    override val candidate = "வரன் விவரம்"
    override val yourPrefs = "உங்கள் விருப்பங்கள்"
    override fun prefsUnmet(n: Int) = "இந்த சுயவிவரம் உங்கள் தேவைகளில் $n ஐப் பூர்த்தி செய்யவில்லை."
    override val communityBar = "சமூகம்"
    override val childPrefs = "குழந்தையின் விருப்பங்கள்"
    override val parentPrefs = "உங்கள் மற்ற விருப்பங்கள்"
    override val notProvided = "இந்த முடிவை மேம்படுத்த மதம் / சமூகத்தைச் சேர்க்கவும்."
    override val astrology = "ஜோதிடம்"
    override val dealbreakers = "தடையாகும் காரணிகள்"
    override val cancelled = "நிவர்த்தி"
    override val verifyTime = "உறுதியான முடிவுக்கு சரியான பிறப்பு நேரத்தை உறுதிசெய்யுங்கள்."
    override val notSaved = "சேமிக்கப்படவில்லை. இந்தப் பக்கத்தை விட்டு வெளியேறும்போது இந்த விவரங்கள் நீக்கப்படும்."
    override val edit = "விவரங்களைத் திருத்து"
    override val again = "மற்றொன்றைப் பார்க்க"
    override val selectDate = "தேதியைத் தேர்ந்தெடுக்கவும்"
    override val selectTime = "நேரத்தைத் தேர்ந்தெடுக்கவும்"
    override val ok = "சரி"
    override val cancel = "ரத்து செய்"
    override fun verdict(key: String) = when (key) {
        "strong_fit" -> "மிகவும் பொருத்தம்"; "worth_exploring" -> "மேலும் ஆராயலாம்"
        "needs_discussion" -> "பேசி முடிவெடுக்க வேண்டும்"; "not_recommended" -> "பரிந்துரைக்கப்படவில்லை"; else -> key
    }
    override fun tier(key: String) = when (key) {
        "TIER_1_PRIME_ALIGN" -> "மிகச் சிறந்த இணக்கம்"; "TIER_2_STRONG_POTENTIAL" -> "வலுவான வாய்ப்பு"
        "TIER_3_TIMING_OR_RECIPROCITY_GATED" -> "நம்பிக்கைக்குரியது, நேரத்தைப் பொறுத்தது"
        "TIER_4_LOW_ALIGNMENT" -> "குறைந்த இணக்கம்"; "DORMANT_BLOCKED" -> "தடைபட்டது"; else -> key
    }
    override fun part(key: String) = when (key) {
        "kp_core" -> "கேபி மையம்"; "structural" -> "கட்டமைப்பு"; "synastry" -> "சினாஸ்ட்ரி"; "timing" -> "காலம்"; else -> key
    }
}
