package com.d2m.app.ui.strings

/** ChildProfileDialogScreen.kt -- mirrors d2m_web's "profile_dialog" namespace. */
interface ChildProfileDialogStrings {
    val myProfile: String
    val tabAbout: String
    val tabProfile: String
    val tabBio: String
    val tabPreferences: String
    val basicDataNote: String

    // Bio data section headers
    val sectionBasicPersonal: String
    val sectionReligiousAstrological: String
    val sectionEducationCareer: String
    val sectionFamilyBackground: String
    val sectionLocationContact: String

    // Bio data fields
    val heightCm: String
    val complexion: String
    val motherTongue: String
    val otherLanguagesKnown: String
    val bodyType: String
    val religion: String
    val casteCommunity: String
    val sect: String
    val gothram: String
    val horoscopeMatchPreference: String
    val highestEducation: String
    val institution: String
    val employedIn: String
    val occupationDesignation: String
    val employer: String
    val monthlyIncome: String
    val currency: String
    val fathersName: String
    val fathersOccupation: String
    val mothersName: String
    val mothersOccupation: String
    val elderBrothers: String
    val youngerBrothers: String
    val elderSisters: String
    val youngerSisters: String
    val familyType: String
    val familyValues: String
    val nativePlace: String
    val financialStatus: String
    val citizenshipStatus: String

    val saved: String
    val saving: String
    val saveBioData: String
    val errSaveBioData: String

    // About Me tab
    fun aboutMeFieldsProgress(filled: Int, total: Int): String
    val aboutPromptsTitle: String
    fun aboutPromptsHint(max: Int): String
    val removePrompt: String
    fun addAPrompt(count: Int, max: Int): String
    val cancel: String
    val showFewer: String
    fun showAll(count: Int): String
    val lifestyleTitle: String
    val fitnessRoutine: String
    val sleepSchedule: String
    val pets: String
    val socialEnergy: String
    val lookingForTitle: String
    val whatIValueInPartner: String
    val whatMattersMostToMe: String
    val lifeAndFuturePlansTitle: String
    val careerAfterMarriage: String
    val livingArrangement: String
    val openToRelocation: String
    val quickFactsTitle: String
    val favoriteCuisine: String
    val dreamDestination: String
    val searchAnyCity: String
    val loveLanguage: String
    val saveAboutMe: String
    val errSaveAboutMe: String

    // Preferences tab
    val locationPreference: String
    val locationsAcceptMatch: String
    val savePreferences: String
    val errSavePreferences: String
}

object ChildProfileDialogStringsEn : ChildProfileDialogStrings {
    override val myProfile = "My profile"
    override val tabAbout = "About Me"
    override val tabProfile = "Profile"
    override val tabBio = "Bio data"
    override val tabPreferences = "Preferences"
    override val basicDataNote = "Basic data and photos live in Settings today -- see plan §4 for the photo-picker follow-up."

    override val sectionBasicPersonal = "Basic & personal details"
    override val sectionReligiousAstrological = "Religious & astrological information"
    override val sectionEducationCareer = "Education & career"
    override val sectionFamilyBackground = "Family background"
    override val sectionLocationContact = "Location & contact"

    override val heightCm = "Height (cm)"
    override val complexion = "Complexion"
    override val motherTongue = "Mother tongue"
    override val otherLanguagesKnown = "Other languages known"
    override val bodyType = "Body type"
    override val religion = "Religion"
    override val casteCommunity = "Caste / community"
    override val sect = "Sect"
    override val gothram = "Gothram"
    override val horoscopeMatchPreference = "Horoscope match preference"
    override val highestEducation = "Highest education"
    override val institution = "Institution / university"
    override val employedIn = "Employed in"
    override val occupationDesignation = "Occupation / designation"
    override val employer = "Employer"
    override val monthlyIncome = "Monthly income"
    override val currency = "Currency"
    override val fathersName = "Father's name"
    override val fathersOccupation = "Father's occupation"
    override val mothersName = "Mother's name"
    override val mothersOccupation = "Mother's occupation"
    override val elderBrothers = "Elder brothers"
    override val youngerBrothers = "Younger brothers"
    override val elderSisters = "Elder sisters"
    override val youngerSisters = "Younger sisters"
    override val familyType = "Family type"
    override val familyValues = "Family values"
    override val nativePlace = "Native place / ancestral origin"
    override val financialStatus = "Financial status"
    override val citizenshipStatus = "Citizenship / residing status"

    override val saved = "Saved."
    override val saving = "Saving…"
    override val saveBioData = "Save bio data"
    override val errSaveBioData = "Couldn't save bio data."

    override fun aboutMeFieldsProgress(filled: Int, total: Int) = "$filled of $total filled in"
    override val aboutPromptsTitle = "About me prompts"
    override fun aboutPromptsHint(max: Int) = "Candidates see these first -- pick up to $max and answer honestly."
    override val removePrompt = "Remove"
    override fun addAPrompt(count: Int, max: Int) = "+ Add a prompt ($count/$max)"
    override val cancel = "Cancel"
    override val showFewer = "Show fewer"
    override fun showAll(count: Int) = "Show all ($count)"
    override val lifestyleTitle = "Lifestyle"
    override val fitnessRoutine = "Fitness routine"
    override val sleepSchedule = "Sleep schedule"
    override val pets = "Pets"
    override val socialEnergy = "Social energy"
    override val lookingForTitle = "What I'm looking for"
    override val whatIValueInPartner = "What I value in a partner"
    override val whatMattersMostToMe = "What matters most to me"
    override val lifeAndFuturePlansTitle = "Life & future plans"
    override val careerAfterMarriage = "Career after marriage"
    override val livingArrangement = "Living arrangement"
    override val openToRelocation = "Open to relocation"
    override val quickFactsTitle = "Quick facts"
    override val favoriteCuisine = "Favorite cuisine"
    override val dreamDestination = "Dream destination"
    override val searchAnyCity = "Search any city or country…"
    override val loveLanguage = "Love language"
    override val saveAboutMe = "Save About Me"
    override val errSaveAboutMe = "Couldn't save About Me."

    override val locationPreference = "Location preference"
    override val locationsAcceptMatch = "Locations you'd accept a match from"
    override val savePreferences = "Save preferences"
    override val errSavePreferences = "Couldn't save preferences."
}

object ChildProfileDialogStringsHi : ChildProfileDialogStrings {
    override val myProfile = "मेरी प्रोफ़ाइल"
    override val tabAbout = "मेरे बारे में"
    override val tabProfile = "प्रोफ़ाइल"
    override val tabBio = "बायो डेटा"
    override val tabPreferences = "प्राथमिकताएं"
    override val basicDataNote = "बुनियादी जानकारी और फ़ोटो अभी सेटिंग्स में हैं।"

    override val sectionBasicPersonal = "बुनियादी और व्यक्तिगत"
    override val sectionReligiousAstrological = "धार्मिक और ज्योतिषीय"
    override val sectionEducationCareer = "शिक्षा और करियर"
    override val sectionFamilyBackground = "पारिवारिक पृष्ठभूमि"
    override val sectionLocationContact = "स्थान और संपर्क"

    override val heightCm = "ऊंचाई (सेमी)"
    override val complexion = "रंग"
    override val motherTongue = "मातृभाषा"
    override val otherLanguagesKnown = "अन्य ज्ञात भाषाएं"
    override val bodyType = "शारीरिक बनावट"
    override val religion = "धर्म"
    override val casteCommunity = "जाति / समुदाय"
    override val sect = "संप्रदाय"
    override val gothram = "गोत्र"
    override val horoscopeMatchPreference = "कुंडली मिलान प्राथमिकता"
    override val highestEducation = "उच्चतम शिक्षा"
    override val institution = "संस्थान"
    override val employedIn = "कार्यरत क्षेत्र"
    override val occupationDesignation = "व्यवसाय / पदनाम"
    override val employer = "नियोक्ता"
    override val monthlyIncome = "मासिक आय"
    override val currency = "मुद्रा"
    override val fathersName = "पिता का नाम"
    override val fathersOccupation = "पिता का व्यवसाय"
    override val mothersName = "माता का नाम"
    override val mothersOccupation = "माता का व्यवसाय"
    override val elderBrothers = "बड़े भाई"
    override val youngerBrothers = "छोटे भाई"
    override val elderSisters = "बड़ी बहनें"
    override val youngerSisters = "छोटी बहनें"
    override val familyType = "परिवार प्रकार"
    override val familyValues = "पारिवारिक मूल्य"
    override val nativePlace = "मूल निवास स्थान"
    override val financialStatus = "आर्थिक स्थिति"
    override val citizenshipStatus = "नागरिकता / निवास स्थिति"

    override val saved = "सेव हो गया।"
    override val saving = "सेव हो रहा है…"
    override val saveBioData = "बायो डेटा सेव करें"
    override val errSaveBioData = "बायो डेटा सेव नहीं हो सका।"

    override fun aboutMeFieldsProgress(filled: Int, total: Int) = "$total में से $filled भरे गए"
    override val aboutPromptsTitle = "मेरे बारे में प्रश्न"
    override fun aboutPromptsHint(max: Int) = "उम्मीदवार इन्हें सबसे पहले देखते हैं -- $max तक चुनें और ईमानदारी से जवाब दें।"
    override val removePrompt = "हटाएं"
    override fun addAPrompt(count: Int, max: Int) = "+ एक प्रश्न जोड़ें ($count/$max)"
    override val cancel = "रद्द करें"
    override val showFewer = "कम दिखाएं"
    override fun showAll(count: Int) = "सभी दिखाएं ($count)"
    override val lifestyleTitle = "जीवनशैली"
    override val fitnessRoutine = "फिटनेस दिनचर्या"
    override val sleepSchedule = "नींद का समय"
    override val pets = "पालतू जानवर"
    override val socialEnergy = "सामाजिक स्वभाव"
    override val lookingForTitle = "मैं क्या तलाश रहा/रही हूं"
    override val whatIValueInPartner = "मैं साथी में क्या महत्व देता/देती हूं"
    override val whatMattersMostToMe = "मेरे लिए सबसे ज़्यादा क्या मायने रखता है"
    override val lifeAndFuturePlansTitle = "जीवन और भविष्य की योजनाएं"
    override val careerAfterMarriage = "शादी के बाद करियर"
    override val livingArrangement = "रहने की व्यवस्था"
    override val openToRelocation = "स्थानांतरण के लिए तैयार"
    override val quickFactsTitle = "त्वरित तथ्य"
    override val favoriteCuisine = "पसंदीदा व्यंजन"
    override val dreamDestination = "सपनों की जगह"
    override val searchAnyCity = "कोई भी शहर या देश खोजें…"
    override val loveLanguage = "प्रेम की भाषा"
    override val saveAboutMe = "About Me सेव करें"
    override val errSaveAboutMe = "About Me सेव नहीं हो सका।"

    override val locationPreference = "स्थान प्राथमिकता"
    override val locationsAcceptMatch = "जिन स्थानों से आप मैच स्वीकार करेंगे"
    override val savePreferences = "प्राथमिकताएं सेव करें"
    override val errSavePreferences = "प्राथमिकताएं सेव नहीं हो सकीं।"
}

object ChildProfileDialogStringsTa : ChildProfileDialogStrings {
    override val myProfile = "எனது சுயவிவரம்"
    override val tabAbout = "என்னைப் பற்றி"
    override val tabProfile = "சுயவிவரம்"
    override val tabBio = "தனிப்பட்ட தகவல்"
    override val tabPreferences = "விருப்பத்தேர்வுகள்"
    override val basicDataNote = "அடிப்படை தகவலும் புகைப்படங்களும் இப்போது அமைப்புகளில் உள்ளன."

    override val sectionBasicPersonal = "அடிப்படை & தனிப்பட்ட"
    override val sectionReligiousAstrological = "மத & ஜோதிட"
    override val sectionEducationCareer = "கல்வி & தொழில்"
    override val sectionFamilyBackground = "குடும்பப் பின்னணி"
    override val sectionLocationContact = "இடம் & தொடர்பு"

    override val heightCm = "உயரம் (செ.மீ)"
    override val complexion = "நிறம்"
    override val motherTongue = "தாய்மொழி"
    override val otherLanguagesKnown = "தெரிந்த மற்ற மொழிகள்"
    override val bodyType = "உடல்வாகு"
    override val religion = "மதம்"
    override val casteCommunity = "சாதி / சமூகம்"
    override val sect = "பிரிவு"
    override val gothram = "கோத்திரம்"
    override val horoscopeMatchPreference = "ஜாதக பொருத்த விருப்பத்தேர்வு"
    override val highestEducation = "உயர் கல்வி"
    override val institution = "நிறுவனம்"
    override val employedIn = "பணிபுரியும் துறை"
    override val occupationDesignation = "தொழில் / பதவி"
    override val employer = "பணியாளர்"
    override val monthlyIncome = "மாத வருமானம்"
    override val currency = "நாணயம்"
    override val fathersName = "தந்தையின் பெயர்"
    override val fathersOccupation = "தந்தையின் தொழில்"
    override val mothersName = "தாயின் பெயர்"
    override val mothersOccupation = "தாயின் தொழில்"
    override val elderBrothers = "அண்ணன்மார்"
    override val youngerBrothers = "தம்பிமார்"
    override val elderSisters = "அக்காக்கள்"
    override val youngerSisters = "தங்கைகள்"
    override val familyType = "குடும்ப வகை"
    override val familyValues = "குடும்ப விழுமியங்கள்"
    override val nativePlace = "சொந்த ஊர்"
    override val financialStatus = "நிதி நிலை"
    override val citizenshipStatus = "குடியுரிமை / வதிவிட நிலை"

    override val saved = "சேமிக்கப்பட்டது."
    override val saving = "சேமிக்கப்படுகிறது…"
    override val saveBioData = "தனிப்பட்ட தகவலைச் சேமி"
    override val errSaveBioData = "தனிப்பட்ட தகவலைச் சேமிக்க முடியவில்லை."

    override fun aboutMeFieldsProgress(filled: Int, total: Int) = "$total இல் $filled நிரப்பப்பட்டது"
    override val aboutPromptsTitle = "என்னைப் பற்றிய கேள்விகள்"
    override fun aboutPromptsHint(max: Int) = "வேட்பாளர்கள் இவற்றை முதலில் பார்ப்பார்கள் -- $max வரை தேர்ந்தெடுத்து நேர்மையாக பதிலளிக்கவும்."
    override val removePrompt = "அகற்று"
    override fun addAPrompt(count: Int, max: Int) = "+ ஒரு கேள்வியைச் சேர்க்கவும் ($count/$max)"
    override val cancel = "ரத்துசெய்"
    override val showFewer = "குறைவாகக் காட்டு"
    override fun showAll(count: Int) = "அனைத்தையும் காட்டு ($count)"
    override val lifestyleTitle = "வாழ்க்கை முறை"
    override val fitnessRoutine = "உடற்பயிற்சி முறை"
    override val sleepSchedule = "தூக்க நேரம்"
    override val pets = "செல்லப்பிராணிகள்"
    override val socialEnergy = "சமூக மனப்பான்மை"
    override val lookingForTitle = "நான் தேடுவது"
    override val whatIValueInPartner = "துணையில் நான் மதிப்பது"
    override val whatMattersMostToMe = "எனக்கு மிக முக்கியமானது"
    override val lifeAndFuturePlansTitle = "வாழ்க்கை & எதிர்கால திட்டங்கள்"
    override val careerAfterMarriage = "திருமணத்திற்குப் பின் தொழில்"
    override val livingArrangement = "வசிப்பு ஏற்பாடு"
    override val openToRelocation = "இடமாற்றத்திற்கு தயார்"
    override val quickFactsTitle = "விரைவு தகவல்கள்"
    override val favoriteCuisine = "பிடித்த உணவு வகை"
    override val dreamDestination = "கனவு இடம்"
    override val searchAnyCity = "எந்த நகரம் அல்லது நாட்டையும் தேடவும்…"
    override val loveLanguage = "அன்பு மொழி"
    override val saveAboutMe = "About Me-ஐ சேமி"
    override val errSaveAboutMe = "About Me-ஐ சேமிக்க முடியவில்லை."

    override val locationPreference = "இட விருப்பம்"
    override val locationsAcceptMatch = "நீங்கள் ஏற்றுக்கொள்ளும் இடங்கள்"
    override val savePreferences = "விருப்பங்களைச் சேமி"
    override val errSavePreferences = "விருப்பங்களைச் சேமிக்க முடியவில்லை."
}
