package com.d2m.app.ui.strings

/**
 * The handful of components under ui/components/ that render real
 * literal copy (not just layout/icons) -- Badge, CityAutocomplete/
 * CityChipPicker, Compatibility, EmptyStateAndError, FilterSheet,
 * FormFields, MatchCard, ProfileTabsPanel, StepUpConfirmDialog.
 * Canonical taxonomy option labels (Taxonomy.kt's toLabel(), cuisine/
 * hobby/region option values) are deliberately NOT here -- that is its
 * own, not-yet-built locale-aware pass (mirroring d2m_web's separate
 * taxonomy.js/taxonomyLabels.js split from its i18n namespaces), tracked
 * separately from this per-screen string table.
 */
interface SharedComponentsStrings {
    val featured: String
    val foundingMember: String
    val startTypingACity: String
    fun removeCity(city: String): String
    val compatLow: String
    val compatMedium: String
    val compatHigh: String
    val compatExceptional: String
    val overallCompatibility: String
    val notScoredYet: String
    val retry: String
    val nothingHereYet: String
    val clear: String
    val include: String
    val exclude: String
    val showFewer: String
    fun showAll(count: Int): String
    val filtersTitle: String
    val clearAll: String
    val ageLabel: String
    fun showResultCount(count: Int): String
    fun resultsMatch(result: Int, total: Int): String
    fun ageYearsRange(start: Int, end: Int): String
    val notSet: String
    fun photographOf(name: String): String
    val noPhotographOnFile: String
    val matchRowGothram: String
    val matchRowStar: String
    val matchRowWork: String
    val matchRowOrganisation: String
    val viewProfile: String
    val addToShortlist: String
    val removeFromShortlist: String
    val noLongerAvailable: String
    fun doshaFlagsNoted(count: Int): String
    fun doshaFlags(count: Int): String
    val confirmYourPassword: String
    val reenterPasswordBody: String
    val passwordLabel: String
    val cancel: String
    val confirm: String
    val confirmingEllipsis: String
    val errConfirmPassword: String
    val tabAboutMe: String
    val tabBioData: String
    val tabChart: String
    val loading: String
    val noBioDataYet: String
    val noAboutMeYet: String
    val sectionBasicPersonal: String
    val sectionReligiousAstrological: String
    val sectionEducationCareer: String
    val sectionFamilyBackground: String
    val sectionLocationContact: String
    val rowHeight: String
    val rowBodyType: String
    val rowComplexion: String
    val rowMotherTongue: String
    val rowOtherLanguages: String
    val rowReligion: String
    val rowCasteCommunity: String
    val rowSect: String
    val rowGothram: String
    val rowHoroscopeMatchPreference: String
    val rowHighestEducation: String
    val rowInstitution: String
    val rowOccupation: String
    val rowEmployer: String
    val rowEmployedIn: String
    val rowMonthlyIncome: String
    val rowFather: String
    val rowMother: String
    val rowSiblings: String
    val rowNativePlace: String
    val rowFamilyType: String
    val rowFamilyValues: String
    val rowFinancialStatus: String
    val rowCitizenshipResiding: String
    val lifestyleTitle: String
    val whatIValueInPartner: String
    val whatMattersMostToMe: String
    val lifeAndFuturePlansTitle: String
    val quickFactsTitle: String
    val chartNotComputedYet: String
    fun elderBrothersCount(count: Int): String
    fun youngerBrothersCount(count: Int): String
    fun elderSistersCount(count: Int): String
    fun youngerSistersCount(count: Int): String
    val screenRecordingDetected: String
    val profileContentHiddenWhileRecording: String
}

object SharedComponentsStringsEn : SharedComponentsStrings {
    override val featured = "Featured"
    override val foundingMember = "Founding member"
    override val startTypingACity = "Start typing a city…"
    override fun removeCity(city: String) = "Remove $city"
    override val compatLow = "Low"
    override val compatMedium = "Medium"
    override val compatHigh = "High"
    override val compatExceptional = "Exceptional"
    override val overallCompatibility = "Overall compatibility"
    override val notScoredYet = "This pairing hasn't been scored yet."
    override val retry = "Retry"
    override val nothingHereYet = "Nothing here yet"
    override val clear = "Clear"
    override val include = "Include"
    override val exclude = "Exclude"
    override val showFewer = "Show fewer"
    override fun showAll(count: Int) = "Show all $count"
    override val filtersTitle = "Filters"
    override val clearAll = "Clear all"
    override val ageLabel = "Age"
    override fun showResultCount(count: Int) = "Show $count profiles"
    override fun resultsMatch(result: Int, total: Int) = "$result of $total profiles match"
    override fun ageYearsRange(start: Int, end: Int) = "$start – $end years"
    override val notSet = "Not set"
    override fun photographOf(name: String) = "Photograph of $name"
    override val noPhotographOnFile = "No photograph on file"
    override val matchRowGothram = "Gothram"
    override val matchRowStar = "Star"
    override val matchRowWork = "Work"
    override val matchRowOrganisation = "Organisation"
    override val viewProfile = "View profile"
    override val addToShortlist = "Add to shortlist"
    override val removeFromShortlist = "Remove from shortlist"
    override val noLongerAvailable = "No longer available — they've gone Serious with someone else"
    override fun doshaFlagsNoted(count: Int) = "$count dosha flag${if (count > 1) "s" else ""} noted"
    override fun doshaFlags(count: Int) = "$count dosha flag${if (count > 1) "s" else ""}"
    override val confirmYourPassword = "Confirm your password"
    override val reenterPasswordBody = "For your security, re-enter your password to continue with this action."
    override val passwordLabel = "Password"
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val confirmingEllipsis = "Confirming…"
    override val errConfirmPassword = "Couldn't confirm your password."
    override val tabAboutMe = "About Me"
    override val tabBioData = "Bio data"
    override val tabChart = "Chart"
    override val loading = "Loading…"
    override val noBioDataYet = "No bio data on file for this profile yet."
    override val noAboutMeYet = "This profile hasn't filled in About Me yet."
    override val sectionBasicPersonal = "Basic & personal"
    override val sectionReligiousAstrological = "Religious & astrological"
    override val sectionEducationCareer = "Education & career"
    override val sectionFamilyBackground = "Family background"
    override val sectionLocationContact = "Location & contact"
    override val rowHeight = "Height"
    override val rowBodyType = "Body type"
    override val rowComplexion = "Complexion"
    override val rowMotherTongue = "Mother tongue"
    override val rowOtherLanguages = "Other languages"
    override val rowReligion = "Religion"
    override val rowCasteCommunity = "Caste / community"
    override val rowSect = "Sect"
    override val rowGothram = "Gothram"
    override val rowHoroscopeMatchPreference = "Horoscope match preference"
    override val rowHighestEducation = "Highest education"
    override val rowInstitution = "Institution"
    override val rowOccupation = "Occupation"
    override val rowEmployer = "Employer"
    override val rowEmployedIn = "Employed in"
    override val rowMonthlyIncome = "Monthly income"
    override val rowFather = "Father"
    override val rowMother = "Mother"
    override val rowSiblings = "Siblings"
    override val rowNativePlace = "Native place"
    override val rowFamilyType = "Family type"
    override val rowFamilyValues = "Family values"
    override val rowFinancialStatus = "Financial status"
    override val rowCitizenshipResiding = "Citizenship / residing status"
    override val lifestyleTitle = "Lifestyle"
    override val whatIValueInPartner = "What I value in a partner"
    override val whatMattersMostToMe = "What matters most to me"
    override val lifeAndFuturePlansTitle = "Life & future plans"
    override val quickFactsTitle = "Quick facts"
    override val chartNotComputedYet = "This profile's chart hasn't been computed yet."
    override fun elderBrothersCount(count: Int) = "$count elder brother${if (count > 1) "s" else ""}"
    override fun youngerBrothersCount(count: Int) = "$count younger brother${if (count > 1) "s" else ""}"
    override fun elderSistersCount(count: Int) = "$count elder sister${if (count > 1) "s" else ""}"
    override fun youngerSistersCount(count: Int) = "$count younger sister${if (count > 1) "s" else ""}"
    override val screenRecordingDetected = "Screen recording detected"
    override val profileContentHiddenWhileRecording = "Profile content is hidden while your screen is being recorded or mirrored."
}

object SharedComponentsStringsHi : SharedComponentsStrings {
    override val featured = "फीचर्ड"
    override val foundingMember = "संस्थापक सदस्य"
    override val startTypingACity = "शहर टाइप करना शुरू करें…"
    override fun removeCity(city: String) = "$city हटाएं"
    override val compatLow = "कम"
    override val compatMedium = "मध्यम"
    override val compatHigh = "उच्च"
    override val compatExceptional = "असाधारण"
    override val overallCompatibility = "समग्र संगतता"
    override val notScoredYet = "इस जोड़ी का अभी तक स्कोर नहीं किया गया है।"
    override val retry = "फिर से प्रयास करें"
    override val nothingHereYet = "अभी यहां कुछ नहीं"
    override val clear = "साफ़ करें"
    override val include = "शामिल करें"
    override val exclude = "बाहर रखें"
    override val showFewer = "कम दिखाएं"
    override fun showAll(count: Int) = "सभी $count दिखाएं"
    override val filtersTitle = "फ़िल्टर"
    override val clearAll = "सभी साफ़ करें"
    override val ageLabel = "आयु"
    override fun showResultCount(count: Int) = "$count प्रोफ़ाइलें दिखाएं"
    override fun resultsMatch(result: Int, total: Int) = "$total में से $result प्रोफ़ाइलें मेल खाती हैं"
    override fun ageYearsRange(start: Int, end: Int) = "$start – $end साल"
    override val notSet = "सेट नहीं है"
    override fun photographOf(name: String) = "$name की तस्वीर"
    override val noPhotographOnFile = "कोई तस्वीर उपलब्ध नहीं"
    override val matchRowGothram = "गोत्र"
    override val matchRowStar = "नक्षत्र"
    override val matchRowWork = "काम"
    override val matchRowOrganisation = "संगठन"
    override val viewProfile = "प्रोफ़ाइल देखें"
    override val addToShortlist = "शॉर्टलिस्ट में जोड़ें"
    override val removeFromShortlist = "शॉर्टलिस्ट से हटाएं"
    override val noLongerAvailable = "अब उपलब्ध नहीं है — वे किसी और के साथ Serious Mode में चले गए हैं"
    override fun doshaFlagsNoted(count: Int) = "$count दोष चिह्नित"
    override fun doshaFlags(count: Int) = "$count दोष"
    override val confirmYourPassword = "अपने पासवर्ड की पुष्टि करें"
    override val reenterPasswordBody = "अपनी सुरक्षा के लिए, इस कार्रवाई को जारी रखने के लिए अपना पासवर्ड फिर से डालें।"
    override val passwordLabel = "पासवर्ड"
    override val cancel = "रद्द करें"
    override val confirm = "पुष्टि करें"
    override val confirmingEllipsis = "पुष्टि की जा रही है…"
    override val errConfirmPassword = "आपके पासवर्ड की पुष्टि नहीं हो सकी।"
    override val tabAboutMe = "मेरे बारे में"
    override val tabBioData = "बायो डेटा"
    override val tabChart = "कुंडली"
    override val loading = "लोड हो रहा है…"
    override val noBioDataYet = "इस प्रोफ़ाइल के लिए अभी तक कोई बायो डेटा नहीं है।"
    override val noAboutMeYet = "इस प्रोफ़ाइल ने अभी तक मेरे बारे में नहीं भरा है।"
    override val sectionBasicPersonal = "बुनियादी और व्यक्तिगत"
    override val sectionReligiousAstrological = "धार्मिक और ज्योतिषीय"
    override val sectionEducationCareer = "शिक्षा और करियर"
    override val sectionFamilyBackground = "पारिवारिक पृष्ठभूमि"
    override val sectionLocationContact = "स्थान और संपर्क"
    override val rowHeight = "ऊंचाई"
    override val rowBodyType = "शारीरिक बनावट"
    override val rowComplexion = "रंग"
    override val rowMotherTongue = "मातृभाषा"
    override val rowOtherLanguages = "अन्य भाषाएं"
    override val rowReligion = "धर्म"
    override val rowCasteCommunity = "जाति / समुदाय"
    override val rowSect = "संप्रदाय"
    override val rowGothram = "गोत्र"
    override val rowHoroscopeMatchPreference = "कुंडली मिलान प्राथमिकता"
    override val rowHighestEducation = "उच्चतम शिक्षा"
    override val rowInstitution = "संस्थान"
    override val rowOccupation = "व्यवसाय"
    override val rowEmployer = "नियोक्ता"
    override val rowEmployedIn = "कार्यरत क्षेत्र"
    override val rowMonthlyIncome = "मासिक आय"
    override val rowFather = "पिता"
    override val rowMother = "माता"
    override val rowSiblings = "भाई-बहन"
    override val rowNativePlace = "मूल निवास स्थान"
    override val rowFamilyType = "परिवार प्रकार"
    override val rowFamilyValues = "पारिवारिक मूल्य"
    override val rowFinancialStatus = "आर्थिक स्थिति"
    override val rowCitizenshipResiding = "नागरिकता / निवास स्थिति"
    override val lifestyleTitle = "जीवनशैली"
    override val whatIValueInPartner = "मैं पार्टनर में क्या महत्व देता/देती हूं"
    override val whatMattersMostToMe = "मेरे लिए सबसे ज़रूरी क्या है"
    override val lifeAndFuturePlansTitle = "जीवन और भविष्य की योजनाएं"
    override val quickFactsTitle = "त्वरित जानकारी"
    override val chartNotComputedYet = "इस प्रोफ़ाइल की कुंडली अभी नहीं बनाई गई है।"
    override fun elderBrothersCount(count: Int) = "$count बड़े भाई"
    override fun youngerBrothersCount(count: Int) = "$count छोटे भाई"
    override fun elderSistersCount(count: Int) = "$count बड़ी बहनें"
    override fun youngerSistersCount(count: Int) = "$count छोटी बहनें"
    override val screenRecordingDetected = "स्क्रीन रिकॉर्डिंग का पता चला"
    override val profileContentHiddenWhileRecording = "जब तक आपकी स्क्रीन रिकॉर्ड या मिरर की जा रही है, प्रोफ़ाइल सामग्री छुपी रहती है।"
}

object SharedComponentsStringsTa : SharedComponentsStrings {
    override val featured = "சிறப்பு"
    override val foundingMember = "நிறுவன உறுப்பினர்"
    override val startTypingACity = "ஒரு நகரத்தைத் தட்டச்சு செய்யத் தொடங்கவும்…"
    override fun removeCity(city: String) = "$city ஐ அகற்று"
    override val compatLow = "குறைவு"
    override val compatMedium = "நடுத்தரம்"
    override val compatHigh = "அதிகம்"
    override val compatExceptional = "விதிவிலக்கானது"
    override val overallCompatibility = "ஒட்டுமொத்த பொருத்தம்"
    override val notScoredYet = "இந்த ஜோடி இன்னும் மதிப்பிடப்படவில்லை."
    override val retry = "மீண்டும் முயற்சிக்கவும்"
    override val nothingHereYet = "இன்னும் இங்கே எதுவும் இல்லை"
    override val clear = "அழி"
    override val include = "சேர்"
    override val exclude = "விலக்கு"
    override val showFewer = "குறைவாகக் காட்டு"
    override fun showAll(count: Int) = "அனைத்து $count ஐயும் காட்டு"
    override val filtersTitle = "வடிகட்டிகள்"
    override val clearAll = "அனைத்தையும் அழி"
    override val ageLabel = "வயது"
    override fun showResultCount(count: Int) = "$count சுயவிவரங்களைக் காட்டு"
    override fun resultsMatch(result: Int, total: Int) = "$total இல் $result சுயவிவரங்கள் பொருந்துகின்றன"
    override fun ageYearsRange(start: Int, end: Int) = "$start – $end வயது"
    override val notSet = "அமைக்கப்படவில்லை"
    override fun photographOf(name: String) = "$name இன் புகைப்படம்"
    override val noPhotographOnFile = "புகைப்படம் இல்லை"
    override val matchRowGothram = "கோத்திரம்"
    override val matchRowStar = "நட்சத்திரம்"
    override val matchRowWork = "தொழில்"
    override val matchRowOrganisation = "நிறுவனம்"
    override val viewProfile = "சுயவிவரத்தைப் பார்க்கவும்"
    override val addToShortlist = "ஷார்ட்லிஸ்ட்டில் சேர்"
    override val removeFromShortlist = "ஷார்ட்லிஸ்ட்டில் இருந்து அகற்று"
    override val noLongerAvailable = "இனி கிடைக்கவில்லை — அவர்கள் வேறு ஒருவருடன் Serious Mode-க்குச் சென்றுவிட்டனர்"
    override fun doshaFlagsNoted(count: Int) = "$count தோஷக் குறிகள் குறிக்கப்பட்டன"
    override fun doshaFlags(count: Int) = "$count தோஷக் குறிகள்"
    override val confirmYourPassword = "உங்கள் கடவுச்சொல்லை உறுதிப்படுத்தவும்"
    override val reenterPasswordBody = "உங்கள் பாதுகாப்பிற்காக, இந்தச் செயலைத் தொடர உங்கள் கடவுச்சொல்லை மீண்டும் உள்ளிடவும்."
    override val passwordLabel = "கடவுச்சொல்"
    override val cancel = "ரத்துசெய்"
    override val confirm = "உறுதிப்படுத்து"
    override val confirmingEllipsis = "உறுதிப்படுத்தப்படுகிறது…"
    override val errConfirmPassword = "உங்கள் கடவுச்சொல்லை உறுதிப்படுத்த முடியவில்லை."
    override val tabAboutMe = "என்னைப் பற்றி"
    override val tabBioData = "தனிப்பட்ட தகவல்"
    override val tabChart = "ஜாதகம்"
    override val loading = "ஏற்றப்படுகிறது…"
    override val noBioDataYet = "இந்த சுயவிவரத்திற்கு இன்னும் தனிப்பட்ட தகவல் இல்லை."
    override val noAboutMeYet = "இந்த சுயவிவரம் இன்னும் என்னைப் பற்றி நிரப்பவில்லை."
    override val sectionBasicPersonal = "அடிப்படை & தனிப்பட்ட"
    override val sectionReligiousAstrological = "மத & ஜோதிட"
    override val sectionEducationCareer = "கல்வி & தொழில்"
    override val sectionFamilyBackground = "குடும்பப் பின்னணி"
    override val sectionLocationContact = "இடம் & தொடர்பு"
    override val rowHeight = "உயரம்"
    override val rowBodyType = "உடல்வாகு"
    override val rowComplexion = "நிறம்"
    override val rowMotherTongue = "தாய்மொழி"
    override val rowOtherLanguages = "மற்ற மொழிகள்"
    override val rowReligion = "மதம்"
    override val rowCasteCommunity = "சாதி / சமூகம்"
    override val rowSect = "பிரிவு"
    override val rowGothram = "கோத்திரம்"
    override val rowHoroscopeMatchPreference = "ஜாதக பொருத்த விருப்பத்தேர்வு"
    override val rowHighestEducation = "உயர் கல்வி"
    override val rowInstitution = "நிறுவனம்"
    override val rowOccupation = "தொழில்"
    override val rowEmployer = "பணியாளர்"
    override val rowEmployedIn = "பணிபுரியும் துறை"
    override val rowMonthlyIncome = "மாத வருமானம்"
    override val rowFather = "தந்தை"
    override val rowMother = "தாய்"
    override val rowSiblings = "உடன்பிறப்புகள்"
    override val rowNativePlace = "சொந்த ஊர்"
    override val rowFamilyType = "குடும்ப வகை"
    override val rowFamilyValues = "குடும்ப விழுமியங்கள்"
    override val rowFinancialStatus = "நிதி நிலை"
    override val rowCitizenshipResiding = "குடியுரிமை / வதிவிட நிலை"
    override val lifestyleTitle = "வாழ்க்கை முறை"
    override val whatIValueInPartner = "ஒரு பங்காளியில் நான் மதிப்பது"
    override val whatMattersMostToMe = "எனக்கு மிக முக்கியமானது"
    override val lifeAndFuturePlansTitle = "வாழ்க்கை & எதிர்கால திட்டங்கள்"
    override val quickFactsTitle = "விரைவு தகவல்கள்"
    override val chartNotComputedYet = "இந்த சுயவிவரத்தின் ஜாதகம் இன்னும் கணக்கிடப்படவில்லை."
    override fun elderBrothersCount(count: Int) = "$count அண்ணன்மார்"
    override fun youngerBrothersCount(count: Int) = "$count தம்பிமார்"
    override fun elderSistersCount(count: Int) = "$count அக்காக்கள்"
    override fun youngerSistersCount(count: Int) = "$count தங்கைகள்"
    override val screenRecordingDetected = "ஸ்க்ரீன் ரெக்கார்டிங் கண்டறியப்பட்டது"
    override val profileContentHiddenWhileRecording = "உங்கள் திரை பதிவு செய்யப்படும் அல்லது பிரதிபலிக்கப்படும் போது சுயவிவரத் தகவல் மறைக்கப்பட்டிருக்கும்."
}
