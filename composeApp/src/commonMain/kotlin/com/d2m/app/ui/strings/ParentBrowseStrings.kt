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
    val colMatch: String
    val sortedAscending: String
    val sortedDescending: String
    val viewAs: String
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
    val moreOptions: String
    val searchById: String
    val closeSearch: String
    val clearAllFilters: String
    val loadMore: String

    val waitingOnChildTitle: String
    val waitingOnChildBody: String

    // ---- Browse sync with web (sort, views, chips, undo)
    val sortLabel: String
    val sortSheetTitle: String
    val sortBestMatch: String
    val sortCompatibilityDesc: String
    val sortAgeAsc: String
    val sortAgeDesc: String
    val sortNameAsc: String
    fun sortChip(current: String): String
    val viewSheetTitle: String
    val viewList: String
    val viewCards: String
    val viewCompare: String
    val viewListHint: String
    val viewCardsHint: String
    val viewCompareHint: String
    fun viewButtonDesc(view: String): String
    fun profilesCount(count: Int): String
    fun ofTotal(total: Int): String
    fun chipAge(min: Int, max: Int): String
    fun chipNot(value: String): String
    fun removeFilter(label: String): String
    val resetAll: String
    val appliedFilters: String
    val searchShortPlaceholder: String
    val clearSearch: String
    val undo: String
    fun shortlistAddedToast(name: String): String
    fun shortlistRemovedToast(name: String): String
    val couldntUpdateShortlist: String
    val profilesListLabel: String
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
    override val colMatch = "Match"
    override val sortedAscending = "Sorted ascending"
    override val sortedDescending = "Sorted descending"
    override val viewAs = "View as"
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
    override val waitingOnChildTitle = "Waiting on your child"
    override val waitingOnChildBody = "Once they claim your invite, you'll be able to browse matches for them here."
    override val moreOptions = "More options"
    override val searchById = "Search by profile ID"
    override val closeSearch = "Close search"
    override val clearAllFilters = "Clear all"
    override val loadMore = "Loading more…"

    override val sortLabel = "Sort"
    override val sortSheetTitle = "Sort profiles by"
    override val sortBestMatch = "Best match"
    override val sortCompatibilityDesc = "Compatibility (highest)"
    override val sortAgeAsc = "Age (youngest first)"
    override val sortAgeDesc = "Age (oldest first)"
    override val sortNameAsc = "Name (A-Z)"
    override fun sortChip(current: String) = "Sort: $current"
    override val viewSheetTitle = "How to show profiles"
    override val viewList = "List"
    override val viewCards = "Cards"
    override val viewCompare = "Compare columns"
    override val viewListHint = "Compact and easy to scan"
    override val viewCardsHint = "Big photos, one profile at a time"
    override val viewCompareHint = "Advanced: a wide table you scroll sideways"
    override fun viewButtonDesc(view: String) = "View: $view. Change view"
    override fun profilesCount(count: Int) = "$count profile${if (count == 1) "" else "s"}"
    override fun ofTotal(total: Int) = "of $total"
    override fun chipAge(min: Int, max: Int) = "Age $min–$max"
    override fun chipNot(value: String) = "Not $value"
    override fun removeFilter(label: String) = "Remove filter $label"
    override val resetAll = "Reset all"
    override val appliedFilters = "Sort and applied filters"
    override val searchShortPlaceholder = "Name or reg. no."
    override val clearSearch = "Clear search"
    override val undo = "Undo"
    override fun shortlistAddedToast(name: String) = "$name added to shortlist"
    override fun shortlistRemovedToast(name: String) = "$name removed from shortlist"
    override val couldntUpdateShortlist = "Couldn't update your shortlist."
    override val profilesListLabel = "Profiles"
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
    override val colMatch = "मिलान"
    override val sortedAscending = "आरोही क्रम"
    override val sortedDescending = "अवरोही क्रम"
    override val viewAs = "दृश्य"
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
    override val waitingOnChildTitle = "आपके बच्चे की प्रतीक्षा है"
    override val waitingOnChildBody = "एक बार वे आपका इनवाइट स्वीकार कर लें, तो आप यहां से उनके लिए मैच ब्राउज़ कर सकेंगे।"
    override val moreOptions = "और विकल्प"
    override val searchById = "प्रोफ़ाइल आईडी से खोजें"
    override val closeSearch = "खोज बंद करें"
    override val clearAllFilters = "सब हटाएं"
    override val loadMore = "और लोड हो रहा है…"

    override val sortLabel = "क्रमबद्ध करें"
    override val sortSheetTitle = "प्रोफ़ाइलों को इस आधार पर क्रमबद्ध करें"
    override val sortBestMatch = "सर्वश्रेष्ठ मैच"
    override val sortCompatibilityDesc = "अनुकूलता (सबसे अधिक)"
    override val sortAgeAsc = "उम्र (सबसे कम उम्र पहले)"
    override val sortAgeDesc = "उम्र (सबसे अधिक उम्र पहले)"
    override val sortNameAsc = "नाम (A-Z)"
    override fun sortChip(current: String) = "क्रमबद्ध करें: $current"
    override val viewSheetTitle = "प्रोफ़ाइलें कैसे दिखाएं"
    override val viewList = "सूची"
    override val viewCards = "कार्ड"
    override val viewCompare = "कॉलम की तुलना"
    override val viewListHint = "संक्षिप्त और देखने में आसान"
    override val viewCardsHint = "बड़ी तस्वीरें, एक बार में एक प्रोफ़ाइल"
    override val viewCompareHint = "उन्नत: एक चौड़ी तालिका जिसे आप बगल में स्क्रोल करते हैं"
    override fun viewButtonDesc(view: String) = "दृश्य: $view। दृश्य बदलें"
    override fun profilesCount(count: Int) = "$count प्रोफ़ाइलें"
    override fun ofTotal(total: Int) = "$total में से"
    override fun chipAge(min: Int, max: Int) = "उम्र $min–$max"
    override fun chipNot(value: String) = "$value नहीं"
    override fun removeFilter(label: String) = "फ़िल्टर हटाएं $label"
    override val resetAll = "सब रीसेट करें"
    override val appliedFilters = "क्रम और लगाए गए फ़िल्टर"
    override val searchShortPlaceholder = "नाम या पंजीकरण सं."
    override val clearSearch = "खोज साफ़ करें"
    override val undo = "पूर्ववत करें"
    override fun shortlistAddedToast(name: String) = "$name को शॉर्टलिस्ट में जोड़ा गया"
    override fun shortlistRemovedToast(name: String) = "$name को शॉर्टलिस्ट से हटाया गया"
    override val couldntUpdateShortlist = "आपकी शॉर्टलिस्ट अपडेट नहीं हो सकी।"
    override val profilesListLabel = "प्रोफ़ाइलें"
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
    override val colMatch = "பொருத்தம்"
    override val sortedAscending = "ஏறுவரிசை"
    override val sortedDescending = "இறங்குவரிசை"
    override val viewAs = "காட்சி"
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
    override val waitingOnChildTitle = "உங்கள் குழந்தைக்காக காத்திருக்கிறோம்"
    override val waitingOnChildBody = "அவர்கள் உங்கள் அழைப்பை ஏற்றவுடன், இங்கிருந்து அவர்களுக்கான பொருத்தங்களை உலாவலாம்."
    override val moreOptions = "மேலும் விருப்பங்கள்"
    override val searchById = "சுயவிவர ஐடி மூலம் தேடுங்கள்"
    override val closeSearch = "தேடலை மூடு"
    override val clearAllFilters = "அனைத்தையும் அழி"
    override val loadMore = "மேலும் ஏற்றப்படுகிறது…"

    override val sortLabel = "வரிசைப்படுத்து"
    override val sortSheetTitle = "சுயவிவரங்களை இதன்படி வரிசைப்படுத்தவும்"
    override val sortBestMatch = "சிறந்த பொருத்தம்"
    override val sortCompatibilityDesc = "இணக்கம் (அதிகம்)"
    override val sortAgeAsc = "வயது (இளையவர் முதலில்)"
    override val sortAgeDesc = "வயது (மூத்தவர் முதலில்)"
    override val sortNameAsc = "பெயர் (A-Z)"
    override fun sortChip(current: String) = "வரிசைப்படுத்து: $current"
    override val viewSheetTitle = "சுயவிவரங்களை எப்படி காட்டுவது"
    override val viewList = "பட்டியல்"
    override val viewCards = "அட்டைகள்"
    override val viewCompare = "நெடுவரிசைகளை ஒப்பிடு"
    override val viewListHint = "சுருக்கமானது, பார்க்க எளிதானது"
    override val viewCardsHint = "பெரிய புகைப்படங்கள், ஒரு நேரத்தில் ஒரு சுயவிவரம்"
    override val viewCompareHint = "மேம்பட்டது: பக்கவாட்டில் நகர்த்தும் அகலமான அட்டவணை"
    override fun viewButtonDesc(view: String) = "காட்சி: $view. காட்சியை மாற்று"
    override fun profilesCount(count: Int) = "$count சுயவிவரங்கள்"
    override fun ofTotal(total: Int) = "$total இல்"
    override fun chipAge(min: Int, max: Int) = "வயது $min–$max"
    override fun chipNot(value: String) = "$value அல்ல"
    override fun removeFilter(label: String) = "வடிகட்டியை நீக்கு $label"
    override val resetAll = "அனைத்தையும் மீட்டமை"
    override val appliedFilters = "வரிசை மற்றும் பயன்படுத்திய வடிகட்டிகள்"
    override val searchShortPlaceholder = "பெயர் அல்லது பதிவு எண்"
    override val clearSearch = "தேடலை அழி"
    override val undo = "செயல்தவிர்"
    override fun shortlistAddedToast(name: String) = "$name ஷார்ட்லிஸ்ட்டில் சேர்க்கப்பட்டது"
    override fun shortlistRemovedToast(name: String) = "$name ஷார்ட்லிஸ்ட்டிலிருந்து நீக்கப்பட்டது"
    override val couldntUpdateShortlist = "உங்கள் ஷார்ட்லிஸ்ட்டை புதுப்பிக்க முடியவில்லை."
    override val profilesListLabel = "சுயவிவரங்கள்"
}
