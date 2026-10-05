package com.d2m.app.ui.strings

/** ParentBrowseScreen.kt -- mirrors d2m_web's "browse" namespace. */
interface ParentBrowseStrings {
    val browseTitle: String
    val errLoadProfiles: String
    fun filteredOfPool(filtered: Int, pool: Int): String
    val findProfileByIdPlaceholder: String
    val find: String
    val finding: String
    val errSearchById: String
    val matching: String
    val allProfiles: String
    val grid: String
    val table: String
    fun filtersWithCount(count: Int): String
    val filters: String
    val facetCity: String
    val facetGothram: String
    val facetSect: String
    val facetNakshatra: String
    val nothingMatchesFiltersTitle: String
    val nothingMatchesFiltersBody: String
    val clearFilters: String
    val noProfilesYetTitle: String
    val noProfilesMatchingBody: String
    val noProfilesAllBody: String
}

object ParentBrowseStringsEn : ParentBrowseStrings {
    override val browseTitle = "Browse"
    override val errLoadProfiles = "Couldn't load profiles."
    override fun filteredOfPool(filtered: Int, pool: Int) = "$filtered of $pool profile${if (pool == 1) "" else "s"}"
    override val findProfileByIdPlaceholder = "Find profile by ID…"
    override val find = "Find"
    override val finding = "Finding…"
    override val errSearchById = "Couldn't search for that ID."
    override val matching = "Matching"
    override val allProfiles = "All profiles"
    override val grid = "Grid"
    override val table = "Table"
    override fun filtersWithCount(count: Int) = "Filters ($count)"
    override val filters = "Filters"
    override val facetCity = "City"
    override val facetGothram = "Gothram"
    override val facetSect = "Sect"
    override val facetNakshatra = "Nakshatra"
    override val nothingMatchesFiltersTitle = "Nothing matches those filters"
    override val nothingMatchesFiltersBody = "Clear a filter or two and the list will fill back in."
    override val clearFilters = "Clear filters"
    override val noProfilesYetTitle = "No profiles yet"
    override val noProfilesMatchingBody = "Once the matching engine has scored some candidates for your child, they show up here."
    override val noProfilesAllBody = "There are no active profiles in the pool right now."
}

object ParentBrowseStringsHi : ParentBrowseStrings {
    override val browseTitle = "ब्राउज़ करें"
    override val errLoadProfiles = "प्रोफ़ाइलें लोड नहीं हो सकीं।"
    override fun filteredOfPool(filtered: Int, pool: Int) = "$pool में से $filtered प्रोफ़ाइलें"
    override val findProfileByIdPlaceholder = "ID से प्रोफ़ाइल खोजें…"
    override val find = "खोजें"
    override val finding = "खोजा जा रहा है…"
    override val errSearchById = "उस ID के लिए खोज नहीं हो सकी।"
    override val matching = "मेल खाती"
    override val allProfiles = "सभी प्रोफ़ाइलें"
    override val grid = "ग्रिड"
    override val table = "तालिका"
    override fun filtersWithCount(count: Int) = "फ़िल्टर ($count)"
    override val filters = "फ़िल्टर"
    override val facetCity = "शहर"
    override val facetGothram = "गोत्र"
    override val facetSect = "संप्रदाय"
    override val facetNakshatra = "नक्षत्र"
    override val nothingMatchesFiltersTitle = "उन फ़िल्टर से कुछ मेल नहीं खाता"
    override val nothingMatchesFiltersBody = "एक-दो फ़िल्टर हटाएं और सूची फिर भर जाएगी।"
    override val clearFilters = "फ़िल्टर साफ़ करें"
    override val noProfilesYetTitle = "अभी कोई प्रोफ़ाइल नहीं"
    override val noProfilesMatchingBody = "जब मैचिंग इंजन आपके बच्चे के लिए कुछ उम्मीदवारों का मूल्यांकन कर लेगा, तो वे यहां दिखेंगे।"
    override val noProfilesAllBody = "अभी पूल में कोई सक्रिय प्रोफ़ाइल नहीं है।"
}

object ParentBrowseStringsTa : ParentBrowseStrings {
    override val browseTitle = "உலாவு"
    override val errLoadProfiles = "சுயவிவரங்களை ஏற்ற முடியவில்லை."
    override fun filteredOfPool(filtered: Int, pool: Int) = "$pool இல் $filtered சுயவிவரங்கள்"
    override val findProfileByIdPlaceholder = "ID மூலம் சுயவிவரத்தைத் தேடுங்கள்…"
    override val find = "தேடு"
    override val finding = "தேடுகிறது…"
    override val errSearchById = "அந்த ID-க்கு தேட முடியவில்லை."
    override val matching = "பொருந்தும்"
    override val allProfiles = "அனைத்து சுயவிவரங்கள்"
    override val grid = "கட்டம்"
    override val table = "அட்டவணை"
    override fun filtersWithCount(count: Int) = "வடிகட்டிகள் ($count)"
    override val filters = "வடிகட்டிகள்"
    override val facetCity = "நகரம்"
    override val facetGothram = "கோத்திரம்"
    override val facetSect = "பிரிவு"
    override val facetNakshatra = "நட்சத்திரம்"
    override val nothingMatchesFiltersTitle = "அந்த வடிகட்டிகளுக்கு எதுவும் பொருந்தவில்லை"
    override val nothingMatchesFiltersBody = "ஒரு சில வடிகட்டிகளை அகற்றினால் பட்டியல் மீண்டும் நிரம்பும்."
    override val clearFilters = "வடிகட்டிகளை அழி"
    override val noProfilesYetTitle = "இன்னும் சுயவிவரங்கள் இல்லை"
    override val noProfilesMatchingBody = "பொருத்த இன்ஜின் உங்கள் குழந்தைக்காக சில வேட்பாளர்களை மதிப்பிட்டதும், அவை இங்கே தெரியும்."
    override val noProfilesAllBody = "இப்போது குளத்தில் செயலில் உள்ள சுயவிவரங்கள் இல்லை."
}
