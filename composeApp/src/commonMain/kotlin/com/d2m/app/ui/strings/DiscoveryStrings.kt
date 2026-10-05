package com.d2m.app.ui.strings

/** DiscoveryScreen.kt -- mirrors d2m_web's child_screens.json "discovery_*" keys where the UX overlaps (mobile uses drag-to-decide + Accept/Snooze/Pass, not web's send-request-with-message modal, so not every web key has a mobile equivalent). */
interface DiscoveryStrings {
    val errLoadCandidates: String
    val loading: String
    val noMoreCandidatesTitle: String
    val noMoreCandidatesBody: String
    fun seenOfTotal(seen: Int, total: Int): String
    fun moreAbout(firstName: String): String
    val pass: String
    val snooze: String
    val accept: String
    val astrology: String
    val preferences: String
}

object DiscoveryStringsEn : DiscoveryStrings {
    override val errLoadCandidates = "Couldn't load candidates."
    override val loading = "Loading…"
    override val noMoreCandidatesTitle = "No more candidates right now"
    override val noMoreCandidatesBody = "Check back later for new suggestions."
    override fun seenOfTotal(seen: Int, total: Int) = "$seen of $total"
    override fun moreAbout(firstName: String) = "More about $firstName"
    override val pass = "Pass"
    override val snooze = "Snooze"
    override val accept = "Accept"
    override val astrology = "Astrology"
    override val preferences = "Preferences"
}

object DiscoveryStringsHi : DiscoveryStrings {
    override val errLoadCandidates = "प्रोफ़ाइलें लोड नहीं हो सकीं।"
    override val loading = "लोड हो रहा है…"
    override val noMoreCandidatesTitle = "अभी और कोई प्रोफ़ाइल नहीं"
    override val noMoreCandidatesBody = "नए सुझावों के लिए बाद में देखें।"
    override fun seenOfTotal(seen: Int, total: Int) = "$total में से $seen"
    override fun moreAbout(firstName: String) = "$firstName के बारे में और जानें"
    override val pass = "पास करें"
    override val snooze = "स्नूज़ करें"
    override val accept = "स्वीकार करें"
    override val astrology = "ज्योतिष"
    override val preferences = "पसंद"
}

object DiscoveryStringsTa : DiscoveryStrings {
    override val errLoadCandidates = "சுயவிவரங்களை ஏற்ற முடியவில்லை."
    override val loading = "ஏற்றப்படுகிறது…"
    override val noMoreCandidatesTitle = "இப்போது வேறு சுயவிவரங்கள் இல்லை"
    override val noMoreCandidatesBody = "புதிய பரிந்துரைகளுக்கு பிறகு பாருங்கள்."
    override fun seenOfTotal(seen: Int, total: Int) = "$total இல் $seen"
    override fun moreAbout(firstName: String) = "$firstName பற்றி மேலும்"
    override val pass = "தவிர்"
    override val snooze = "ஸ்னூஸ் செய்"
    override val accept = "ஏற்றுக்கொள்"
    override val astrology = "ஜாதகம்"
    override val preferences = "விருப்பங்கள்"
}
