package com.d2m.app.ui.strings

/** ShareLinksScreen.kt -- mirrors d2m_web's "public_share" namespace where concepts overlap (create/manage only; the public landing page itself is a web-only route). */
interface ShareLinksStrings {
    val pageTitle: String
    val createIntro: String
    val waitingOnChildTitle: String
    val waitingOnChildBody: String
    val loading: String
    val linksShowLabel: String
    val detailLevelFull: String
    val detailLevelFullShort: String
    val detailLevelMinimal: String
    val detailLevelCustom: String
    fun fieldsSelectedLabel(count: Int): String
    val chooseFieldsCta: String
    val linksCreateCta: String
    val linksCreating: String
    val linksCreateError: String
    val linksLoadError: String
    val linksEmptyTitle: String
    val linksEmptyBody: String
    val linkActions: String
    val copyLink: String
    val turnOff: String
    val turnOn: String
    val extendCta: String
    val linkStatusActive: String
    val linkStatusOff: String
    fun detailLevelCustomCount(count: Int): String
    fun linkExpires(date: String): String
    fun linkViewCount(count: Int): String
    fun linkLastViewedSuffix(date: String): String
    val pickerTitle: String
    val pickerSubtitle: String
    val done: String
    val groupBasicPersonal: String
    val groupEducationCareer: String
    val groupFamily: String
    val fieldHeight: String
    val fieldComplexion: String
    val fieldBodyType: String
    val fieldMotherTongue: String
    val fieldOtherLanguages: String
    val fieldReligion: String
    val fieldCommunity: String
    val fieldSect: String
    val fieldGothram: String
    val fieldCitizenship: String
    val fieldEducation: String
    val fieldInstitution: String
    val fieldOccupation: String
    val fieldEmployer: String
    val fieldEmploymentSector: String
    val fieldFinancialStatus: String
    val fieldElderBrothers: String
    val fieldYoungerBrothers: String
    val fieldElderSisters: String
    val fieldYoungerSisters: String
    val fieldNativePlace: String
    val fieldFamilyType: String
    val fieldFamilyValues: String
    val fieldHoroscope: String
    val fieldHoroscopeDetail: String
    val fieldPhotoCount: String
}

object ShareLinksStringsEn : ShareLinksStrings {
    override val pageTitle = "Share my profile"
    override val createIntro = "Anyone with the link can view this profile at the level of detail you choose -- they never see the app itself. Turn a link off (and back on) or let it expire at any time; the link itself never changes."
    override val waitingOnChildTitle = "Waiting on your child"
    override val waitingOnChildBody = "Once they claim your invite, you'll be able to share their profile from here."
    override val loading = "Loading…"
    override val linksShowLabel = "What should the link show"
    override val detailLevelFull = "Entire profile (all photos, full bio data, horoscope)"
    override val detailLevelFullShort = "Entire profile"
    override val detailLevelMinimal = "One photo, name, age, basic bio data & horoscope"
    override val detailLevelCustom = "Custom -- pick exactly which fields"
    override fun fieldsSelectedLabel(count: Int) = "$count field${if (count == 1) "" else "s"} selected"
    override val chooseFieldsCta = "Choose fields…"
    override val linksCreateCta = "Create link"
    override val linksCreating = "Creating…"
    override val linksCreateError = "Couldn't create a share link."
    override val linksLoadError = "Couldn't load share links."
    override val linksEmptyTitle = "No share links yet"
    override val linksEmptyBody = "Create one above to share this profile with your network."
    override val linkActions = "Link actions"
    override val copyLink = "Copy link"
    override val turnOff = "Turn off"
    override val turnOn = "Turn on"
    override val extendCta = "Extend +7 days"
    override val linkStatusActive = "Active"
    override val linkStatusOff = "Turned off"
    override fun detailLevelCustomCount(count: Int) = "Custom ($count field${if (count == 1) "" else "s"})"
    override fun linkExpires(date: String) = "Expires $date"
    override fun linkViewCount(count: Int) = "$count view${if (count == 1) "" else "s"}"
    override fun linkLastViewedSuffix(date: String) = " · last viewed $date"
    override val pickerTitle = "Choose what this link shows"
    override val pickerSubtitle = "Only the fields you turn on here will be visible to anyone with the link."
    override val done = "Done"
    override val groupBasicPersonal = "Basic & personal"
    override val groupEducationCareer = "Education & career"
    override val groupFamily = "Family"
    override val fieldHeight = "Height"
    override val fieldComplexion = "Complexion"
    override val fieldBodyType = "Body type"
    override val fieldMotherTongue = "Mother tongue"
    override val fieldOtherLanguages = "Other languages"
    override val fieldReligion = "Religion"
    override val fieldCommunity = "Community"
    override val fieldSect = "Sect"
    override val fieldGothram = "Gothram"
    override val fieldCitizenship = "Citizenship"
    override val fieldEducation = "Education"
    override val fieldInstitution = "Institution"
    override val fieldOccupation = "Occupation"
    override val fieldEmployer = "Employer"
    override val fieldEmploymentSector = "Employment sector"
    override val fieldFinancialStatus = "Financial status"
    override val fieldElderBrothers = "Elder brothers"
    override val fieldYoungerBrothers = "Younger brothers"
    override val fieldElderSisters = "Elder sisters"
    override val fieldYoungerSisters = "Younger sisters"
    override val fieldNativePlace = "Native place"
    override val fieldFamilyType = "Family type"
    override val fieldFamilyValues = "Family values"
    override val fieldHoroscope = "Horoscope"
    override val fieldHoroscopeDetail = "Nakshatra, pada, D1/D9 chart & dasha"
    override val fieldPhotoCount = "How many photos"
}

object ShareLinksStringsHi : ShareLinksStrings {
    override val pageTitle = "मेरी प्रोफ़ाइल शेयर करें"
    override val createIntro = "लिंक वाला कोई भी व्यक्ति इस प्रोफ़ाइल को आपके चुने विवरण स्तर पर देख सकता है -- वे कभी ऐप नहीं देखते। आप किसी भी समय लिंक बंद (और फिर चालू) कर सकते हैं या इसे समाप्त होने दे सकते हैं; लिंक खुद कभी नहीं बदलता।"
    override val waitingOnChildTitle = "आपके बच्चे की प्रतीक्षा है"
    override val waitingOnChildBody = "एक बार वे आपका इनवाइट स्वीकार कर लें, तो आप यहां से उनकी प्रोफ़ाइल शेयर कर सकेंगे।"
    override val loading = "लोड हो रहा है…"
    override val linksShowLabel = "लिंक क्या दिखाए"
    override val detailLevelFull = "पूरी प्रोफ़ाइल (सभी तस्वीरें, पूरा बायो डेटा, कुंडली)"
    override val detailLevelFullShort = "पूरी प्रोफ़ाइल"
    override val detailLevelMinimal = "एक तस्वीर, नाम, आयु, बुनियादी बायो डेटा और कुंडली"
    override val detailLevelCustom = "कस्टम -- ठीक-ठीक चुनें कि कौन से फ़ील्ड दिखें"
    override fun fieldsSelectedLabel(count: Int) = "$count फ़ील्ड चयनित"
    override val chooseFieldsCta = "फ़ील्ड चुनें…"
    override val linksCreateCta = "लिंक बनाएं"
    override val linksCreating = "बनाया जा रहा है…"
    override val linksCreateError = "शेयर लिंक नहीं बन सका।"
    override val linksLoadError = "शेयर लिंक लोड नहीं हो सके।"
    override val linksEmptyTitle = "अभी तक कोई शेयर लिंक नहीं"
    override val linksEmptyBody = "अपने नेटवर्क के साथ यह प्रोफ़ाइल शेयर करने के लिए ऊपर एक बनाएं।"
    override val linkActions = "लिंक क्रियाएं"
    override val copyLink = "लिंक कॉपी करें"
    override val turnOff = "बंद करें"
    override val turnOn = "चालू करें"
    override val extendCta = "+7 दिन बढ़ाएं"
    override val linkStatusActive = "सक्रिय"
    override val linkStatusOff = "बंद किया गया"
    override fun detailLevelCustomCount(count: Int) = "कस्टम ($count फ़ील्ड)"
    override fun linkExpires(date: String) = "$date को समाप्त"
    override fun linkViewCount(count: Int) = "$count बार देखा गया"
    override fun linkLastViewedSuffix(date: String) = " · आखिरी बार $date को देखा गया"
    override val pickerTitle = "चुनें कि यह लिंक क्या दिखाए"
    override val pickerSubtitle = "यहां आप जो फ़ील्ड चालू करेंगे वे ही लिंक वाले किसी भी व्यक्ति को दिखेंगे।"
    override val done = "हो गया"
    override val groupBasicPersonal = "बुनियादी व व्यक्तिगत"
    override val groupEducationCareer = "शिक्षा व करियर"
    override val groupFamily = "परिवार"
    override val fieldHeight = "ऊंचाई"
    override val fieldComplexion = "रंग"
    override val fieldBodyType = "शारीरिक बनावट"
    override val fieldMotherTongue = "मातृभाषा"
    override val fieldOtherLanguages = "अन्य भाषाएं"
    override val fieldReligion = "धर्म"
    override val fieldCommunity = "समुदाय"
    override val fieldSect = "संप्रदाय"
    override val fieldGothram = "गोत्र"
    override val fieldCitizenship = "नागरिकता"
    override val fieldEducation = "शिक्षा"
    override val fieldInstitution = "संस्थान"
    override val fieldOccupation = "व्यवसाय"
    override val fieldEmployer = "नियोक्ता"
    override val fieldEmploymentSector = "कार्यक्षेत्र"
    override val fieldFinancialStatus = "आर्थिक स्थिति"
    override val fieldElderBrothers = "बड़े भाई"
    override val fieldYoungerBrothers = "छोटे भाई"
    override val fieldElderSisters = "बड़ी बहनें"
    override val fieldYoungerSisters = "छोटी बहनें"
    override val fieldNativePlace = "मूल निवास स्थान"
    override val fieldFamilyType = "परिवार प्रकार"
    override val fieldFamilyValues = "पारिवारिक मूल्य"
    override val fieldHoroscope = "कुंडली"
    override val fieldHoroscopeDetail = "नक्षत्र, पद, D1/D9 चार्ट और दशा"
    override val fieldPhotoCount = "कितनी तस्वीरें"
}

object ShareLinksStringsTa : ShareLinksStrings {
    override val pageTitle = "எனது சுயவிவரத்தைப் பகிரவும்"
    override val createIntro = "இணைப்பைப் பெறும் எவரும் நீங்கள் தேர்ந்தெடுக்கும் விவர நிலையில் இந்த சுயவிவரத்தைப் பார்க்கலாம் -- அவர்கள் ஆப்பைப் பார்க்க மாட்டார்கள். எந்த நேரத்திலும் இணைப்பை அணைக்கலாம் (மீண்டும் இயக்கலாம்) அல்லது காலாவதியாக விடலாம்; இணைப்பே ஒருபோதும் மாறாது."
    override val waitingOnChildTitle = "உங்கள் குழந்தைக்காக காத்திருக்கிறோம்"
    override val waitingOnChildBody = "அவர்கள் உங்கள் அழைப்பை ஏற்றவுடன், இங்கிருந்து அவர்களின் சுயவிவரத்தைப் பகிரலாம்."
    override val loading = "ஏற்றப்படுகிறது…"
    override val linksShowLabel = "இணைப்பு என்ன காட்ட வேண்டும்"
    override val detailLevelFull = "முழு சுயவிவரம் (அனைத்து புகைப்படங்கள், முழு தனிப்பட்ட தகவல், ஜாதகம்)"
    override val detailLevelFullShort = "முழு சுயவிவரம்"
    override val detailLevelMinimal = "ஒரு படம், பெயர், வயது, அடிப்படை தனிப்பட்ட தகவல் & ஜாதகம்"
    override val detailLevelCustom = "தனிப்பயன் -- எந்தெந்த புலங்கள் என்று சரியாகத் தேர்ந்தெடுக்கவும்"
    override fun fieldsSelectedLabel(count: Int) = "$count புலங்கள் தேர்ந்தெடுக்கப்பட்டன"
    override val chooseFieldsCta = "புலங்களைத் தேர்ந்தெடுக்கவும்…"
    override val linksCreateCta = "இணைப்பை உருவாக்கவும்"
    override val linksCreating = "உருவாக்கப்படுகிறது…"
    override val linksCreateError = "பகிர்வு இணைப்பை உருவாக்க முடியவில்லை."
    override val linksLoadError = "பகிர்வு இணைப்புகளை ஏற்ற முடியவில்லை."
    override val linksEmptyTitle = "இதுவரை பகிர்வு இணைப்புகள் இல்லை"
    override val linksEmptyBody = "உங்கள் நெட்வொர்க்குடன் இந்த சுயவிவரத்தைப் பகிர மேலே ஒன்றை உருவாக்கவும்."
    override val linkActions = "இணைப்பு செயல்கள்"
    override val copyLink = "இணைப்பை நகலெடுக்கவும்"
    override val turnOff = "அணைக்கவும்"
    override val turnOn = "இயக்கவும்"
    override val extendCta = "+7 நாட்கள் நீட்டிக்கவும்"
    override val linkStatusActive = "செயலில்"
    override val linkStatusOff = "அணைக்கப்பட்டது"
    override fun detailLevelCustomCount(count: Int) = "தனிப்பயன் ($count புலங்கள்)"
    override fun linkExpires(date: String) = "$date அன்று காலாவதியாகும்"
    override fun linkViewCount(count: Int) = "$count முறை பார்க்கப்பட்டது"
    override fun linkLastViewedSuffix(date: String) = " · கடைசியாக $date அன்று பார்க்கப்பட்டது"
    override val pickerTitle = "இந்த இணைப்பு என்ன காட்ட வேண்டும் என்று தேர்ந்தெடுக்கவும்"
    override val pickerSubtitle = "இங்கே நீங்கள் இயக்கும் புலங்கள் மட்டுமே இணைப்பைப் பெறும் எவருக்கும் தெரியும்."
    override val done = "முடிந்தது"
    override val groupBasicPersonal = "அடிப்படை & தனிப்பட்டவை"
    override val groupEducationCareer = "கல்வி & தொழில்"
    override val groupFamily = "குடும்பம்"
    override val fieldHeight = "உயரம்"
    override val fieldComplexion = "நிறம்"
    override val fieldBodyType = "உடல்வாகு"
    override val fieldMotherTongue = "தாய்மொழி"
    override val fieldOtherLanguages = "மற்ற மொழிகள்"
    override val fieldReligion = "மதம்"
    override val fieldCommunity = "சமூகம்"
    override val fieldSect = "பிரிவு"
    override val fieldGothram = "கோத்திரம்"
    override val fieldCitizenship = "குடியுரிமை"
    override val fieldEducation = "கல்வி"
    override val fieldInstitution = "நிறுவனம்"
    override val fieldOccupation = "தொழில்"
    override val fieldEmployer = "பணியாளர்"
    override val fieldEmploymentSector = "பணித்துறை"
    override val fieldFinancialStatus = "நிதி நிலை"
    override val fieldElderBrothers = "அண்ணன்மார்"
    override val fieldYoungerBrothers = "தம்பிமார்"
    override val fieldElderSisters = "அக்காக்கள்"
    override val fieldYoungerSisters = "தங்கைகள்"
    override val fieldNativePlace = "சொந்த ஊர்"
    override val fieldFamilyType = "குடும்ப வகை"
    override val fieldFamilyValues = "குடும்ப விழுமியங்கள்"
    override val fieldHoroscope = "ஜாதகம்"
    override val fieldHoroscopeDetail = "நட்சத்திரம், பாதம், D1/D9 சக்கரம் & தசை"
    override val fieldPhotoCount = "எத்தனை புகைப்படங்கள்"
}
