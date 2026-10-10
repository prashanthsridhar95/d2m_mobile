package com.d2m.app.ui.strings

/** ProfileLinkScreen.kt -- mirrors d2m_web's "public_share" namespace. */
interface ProfileLinkStrings {
    val notFoundTitle: String
    val notFoundBody: String
    val goneTitle: String
    val goneBody: String
    val genericErrorTitle: String
    val genericErrorBody: String

    val leadTitle: String
    val leadSubtitle: String
    val leadNameLabel: String
    val leadPhoneLabel: String
    val leadViewProfile: String
    val leadUnlocking: String
    val leadError: String

    val basicDetailsTitle: String
    val familyTitle: String
    val horoscopeTitle: String

    val fieldEducation: String
    val fieldInstitution: String
    val fieldOccupation: String
    val fieldEmployer: String
    val fieldEmploymentSector: String
    val fieldHeight: String
    val fieldBodyType: String
    val fieldComplexion: String
    val fieldMotherTongue: String
    val fieldOtherLanguages: String
    val fieldGothram: String
    val fieldReligion: String
    val fieldCommunity: String
    val fieldSect: String
    val fieldCitizenship: String
    val fieldFinancialStatus: String
    val fieldSiblings: String
    val fieldNativePlace: String
    val fieldFamilyType: String
    val fieldFamilyValues: String
    val fieldStarRaasi: String
    val fieldPada: String

    fun siblingElderBrother(count: Int): String
    fun siblingYoungerBrother(count: Int): String
    fun siblingElderSister(count: Int): String
    fun siblingYoungerSister(count: Int): String

    val reachOutTitle: String
    val reachOutBody: String
    val reachOutCta: String

    val vouchesTitle: String
    val vouchesEmpty: String
    val voucherParent: String
    val voucherChild: String
    val voucherGuest: String

    val guestVouchIntro: String
    val guestVouchEmailLabel: String
    val guestVouchSendCode: String
    val guestVouchSending: String
    val guestVouchOtpError: String
    fun guestVouchSentTo(email: String): String
    val guestVouchCodeLabel: String
    val guestVouchNameLabel: String
    val guestVouchPhoneLabel: String
    val guestVouchNoteLabel: String
    val guestVouchNotePlaceholder: String
    val guestVouchCta: String
    val guestVouchSubmitting: String
    val guestVouchCreateError: String
    val guestVouchDone: String

    val footerNote: String
}

object ProfileLinkStringsEn : ProfileLinkStrings {
    override val notFoundTitle = "This link isn't valid."
    override val notFoundBody = "Double-check the link you were sent -- it may have been copied incorrectly."
    override val goneTitle = "This link is no longer available."
    override val goneBody = "Whoever shared this profile has turned the link off, or it's expired."
    override val genericErrorTitle = "Something went wrong."
    override val genericErrorBody = "Couldn't load this profile right now -- try again in a moment."

    override val leadTitle = "See this profile"
    override val leadSubtitle = "Tell us who's asking -- this profile's photo and details stay locked until then."
    override val leadNameLabel = "Your name"
    override val leadPhoneLabel = "Phone number"
    override val leadViewProfile = "View profile"
    override val leadUnlocking = "Unlocking…"
    override val leadError = "Couldn't unlock this profile -- try again."

    override val basicDetailsTitle = "Basic details"
    override val familyTitle = "Family"
    override val horoscopeTitle = "Horoscope"

    override val fieldEducation = "Education"
    override val fieldInstitution = "Institution"
    override val fieldOccupation = "Occupation"
    override val fieldEmployer = "Employer"
    override val fieldEmploymentSector = "Employment sector"
    override val fieldHeight = "Height"
    override val fieldBodyType = "Body type"
    override val fieldComplexion = "Complexion"
    override val fieldMotherTongue = "Mother tongue"
    override val fieldOtherLanguages = "Other languages"
    override val fieldGothram = "Gothram"
    override val fieldReligion = "Religion"
    override val fieldCommunity = "Community"
    override val fieldSect = "Sect"
    override val fieldCitizenship = "Citizenship"
    override val fieldFinancialStatus = "Financial status"
    override val fieldSiblings = "Siblings"
    override val fieldNativePlace = "Native place"
    override val fieldFamilyType = "Family type"
    override val fieldFamilyValues = "Family values"
    override val fieldStarRaasi = "Star / raasi"
    override val fieldPada = "Pada"

    override fun siblingElderBrother(count: Int) = if (count == 1) "$count elder brother" else "$count elder brothers"
    override fun siblingYoungerBrother(count: Int) = if (count == 1) "$count younger brother" else "$count younger brothers"
    override fun siblingElderSister(count: Int) = if (count == 1) "$count elder sister" else "$count elder sisters"
    override fun siblingYoungerSister(count: Int) = if (count == 1) "$count younger sister" else "$count younger sisters"

    override val reachOutTitle = "Reach out"
    override val reachOutBody = "Sign up & log in to reach out and connect with this profile."
    override val reachOutCta = "Sign up / Log in"

    override val vouchesTitle = "Vouches"
    override val vouchesEmpty = "No one's vouched for this family yet."
    override val voucherParent = "Parent"
    override val voucherChild = "Child"
    override val voucherGuest = "Guest"

    override val guestVouchIntro = "Know this family? Vouch for them -- we'll email you a code first."
    override val guestVouchEmailLabel = "Your email"
    override val guestVouchSendCode = "Send code"
    override val guestVouchSending = "Sending code…"
    override val guestVouchOtpError = "Couldn't send that code -- try again."
    override fun guestVouchSentTo(email: String) = "We sent a code to $email."
    override val guestVouchCodeLabel = "Verification code"
    override val guestVouchNameLabel = "Your name"
    override val guestVouchPhoneLabel = "Your phone number"
    override val guestVouchNoteLabel = "Note (optional)"
    override val guestVouchNotePlaceholder = "How do you know this family?"
    override val guestVouchCta = "Vouch for this profile"
    override val guestVouchSubmitting = "Submitting…"
    override val guestVouchCreateError = "That didn't go through -- try again."
    override val guestVouchDone = "Thanks -- sent for approval. It'll show here once they confirm it."

    override val footerNote = "Shared via D2M -- this is a read-only preview, not the full app."
}

object ProfileLinkStringsHi : ProfileLinkStrings {
    override val notFoundTitle = "यह लिंक मान्य नहीं है।"
    override val notFoundBody = "आपको भेजा गया लिंक जांचें -- हो सकता है वह गलत तरीके से कॉपी हुआ हो।"
    override val goneTitle = "यह लिंक अब उपलब्ध नहीं है।"
    override val goneBody = "जिसने भी यह प्रोफ़ाइल शेयर की थी, उसने लिंक बंद कर दिया है, या यह समाप्त हो गया है।"
    override val genericErrorTitle = "कुछ गलत हो गया।"
    override val genericErrorBody = "अभी यह प्रोफ़ाइल लोड नहीं हो सकी -- कुछ देर बाद फिर प्रयास करें।"

    override val leadTitle = "यह प्रोफ़ाइल देखें"
    override val leadSubtitle = "हमें बताएं कौन पूछ रहा है -- जब तक नहीं बताएंगे, इस प्रोफ़ाइल की फ़ोटो और विवरण छिपे रहेंगे।"
    override val leadNameLabel = "आपका नाम"
    override val leadPhoneLabel = "फ़ोन नंबर"
    override val leadViewProfile = "प्रोफ़ाइल देखें"
    override val leadUnlocking = "अनलॉक हो रहा है…"
    override val leadError = "यह प्रोफ़ाइल अनलॉक नहीं हो सकी -- फिर प्रयास करें।"

    override val basicDetailsTitle = "बुनियादी विवरण"
    override val familyTitle = "परिवार"
    override val horoscopeTitle = "कुंडली"

    override val fieldEducation = "शिक्षा"
    override val fieldInstitution = "संस्थान"
    override val fieldOccupation = "व्यवसाय"
    override val fieldEmployer = "नियोक्ता"
    override val fieldEmploymentSector = "रोजगार क्षेत्र"
    override val fieldHeight = "ऊंचाई"
    override val fieldBodyType = "शरीर का प्रकार"
    override val fieldComplexion = "रंग"
    override val fieldMotherTongue = "मातृभाषा"
    override val fieldOtherLanguages = "अन्य भाषाएं"
    override val fieldGothram = "गोत्र"
    override val fieldReligion = "धर्म"
    override val fieldCommunity = "समुदाय"
    override val fieldSect = "संप्रदाय"
    override val fieldCitizenship = "नागरिकता"
    override val fieldFinancialStatus = "आर्थिक स्थिति"
    override val fieldSiblings = "भाई-बहन"
    override val fieldNativePlace = "मूल स्थान"
    override val fieldFamilyType = "परिवार का प्रकार"
    override val fieldFamilyValues = "पारिवारिक मूल्य"
    override val fieldStarRaasi = "नक्षत्र / राशि"
    override val fieldPada = "पद"

    override fun siblingElderBrother(count: Int) = "$count बड़ा भाई"
    override fun siblingYoungerBrother(count: Int) = "$count छोटा भाई"
    override fun siblingElderSister(count: Int) = "$count बड़ी बहन"
    override fun siblingYoungerSister(count: Int) = "$count छोटी बहन"

    override val reachOutTitle = "संपर्क करें"
    override val reachOutBody = "इस प्रोफ़ाइल से संपर्क करने के लिए साइन अप करें और लॉगिन करें।"
    override val reachOutCta = "साइन अप / लॉगिन करें"

    override val vouchesTitle = "वाउच"
    override val vouchesEmpty = "अभी तक किसी ने इस परिवार के लिए वाउच नहीं किया है।"
    override val voucherParent = "माता-पिता"
    override val voucherChild = "बच्चा"
    override val voucherGuest = "अतिथि"

    override val guestVouchIntro = "इस परिवार को जानते हैं? उनके लिए वाउच करें -- पहले हम आपको ईमेल पर एक कोड भेजेंगे।"
    override val guestVouchEmailLabel = "आपका ईमेल"
    override val guestVouchSendCode = "कोड भेजें"
    override val guestVouchSending = "कोड भेजा जा रहा है…"
    override val guestVouchOtpError = "वह कोड नहीं भेजा जा सका -- फिर प्रयास करें।"
    override fun guestVouchSentTo(email: String) = "हमने $email पर एक कोड भेजा है।"
    override val guestVouchCodeLabel = "सत्यापन कोड"
    override val guestVouchNameLabel = "आपका नाम"
    override val guestVouchPhoneLabel = "आपका फ़ोन नंबर"
    override val guestVouchNoteLabel = "नोट (वैकल्पिक)"
    override val guestVouchNotePlaceholder = "आप इस परिवार को कैसे जानते हैं?"
    override val guestVouchCta = "इस प्रोफ़ाइल के लिए वाउच करें"
    override val guestVouchSubmitting = "सबमिट हो रहा है…"
    override val guestVouchCreateError = "वह सबमिट नहीं हो सका -- फिर प्रयास करें।"
    override val guestVouchDone = "धन्यवाद -- अनुमोदन के लिए भेज दिया गया। उनकी पुष्टि होते ही यह यहां दिखेगा।"

    override val footerNote = "D2M के माध्यम से शेयर किया गया -- यह केवल पढ़ने के लिए एक पूर्वावलोकन है, पूरा ऐप नहीं।"
}

object ProfileLinkStringsTa : ProfileLinkStrings {
    override val notFoundTitle = "இந்த இணைப்பு செல்லுபடியாகாது."
    override val notFoundBody = "உங்களுக்கு அனுப்பப்பட்ட இணைப்பை சரிபார்க்கவும் -- அது தவறாக நகலெடுக்கப்பட்டிருக்கலாம்."
    override val goneTitle = "இந்த இணைப்பு இனி கிடைக்கவில்லை."
    override val goneBody = "இந்த சுயவிவரத்தைப் பகிர்ந்தவர் இணைப்பை முடக்கியுள்ளார், அல்லது அது காலாவதியாகிவிட்டது."
    override val genericErrorTitle = "ஏதோ தவறு நடந்தது."
    override val genericErrorBody = "இப்போது இந்த சுயவிவரத்தை ஏற்ற முடியவில்லை -- சிறிது நேரம் கழித்து முயற்சிக்கவும்."

    override val leadTitle = "இந்த சுயவிவரத்தைப் பாருங்கள்"
    override val leadSubtitle = "யார் கேட்கிறீர்கள் என்று எங்களிடம் சொல்லுங்கள் -- அதுவரை இந்த சுயவிவரத்தின் புகைப்படமும் விவரங்களும் பூட்டப்பட்டிருக்கும்."
    override val leadNameLabel = "உங்கள் பெயர்"
    override val leadPhoneLabel = "தொலைபேசி எண்"
    override val leadViewProfile = "சுயவிவரத்தைக் காண்க"
    override val leadUnlocking = "திறக்கிறது…"
    override val leadError = "இந்த சுயவிவரத்தைத் திறக்க முடியவில்லை -- மீண்டும் முயற்சிக்கவும்."

    override val basicDetailsTitle = "அடிப்படை விவரங்கள்"
    override val familyTitle = "குடும்பம்"
    override val horoscopeTitle = "ஜாதகம்"

    override val fieldEducation = "கல்வி"
    override val fieldInstitution = "நிறுவனம்"
    override val fieldOccupation = "தொழில்"
    override val fieldEmployer = "முதலாளி"
    override val fieldEmploymentSector = "வேலை துறை"
    override val fieldHeight = "உயரம்"
    override val fieldBodyType = "உடல் வகை"
    override val fieldComplexion = "நிறம்"
    override val fieldMotherTongue = "தாய்மொழி"
    override val fieldOtherLanguages = "மற்ற மொழிகள்"
    override val fieldGothram = "கோத்திரம்"
    override val fieldReligion = "மதம்"
    override val fieldCommunity = "சமூகம்"
    override val fieldSect = "பிரிவு"
    override val fieldCitizenship = "குடியுரிமை"
    override val fieldFinancialStatus = "நிதி நிலை"
    override val fieldSiblings = "உடன்பிறப்புகள்"
    override val fieldNativePlace = "சொந்த ஊர்"
    override val fieldFamilyType = "குடும்ப வகை"
    override val fieldFamilyValues = "குடும்ப மதிப்புகள்"
    override val fieldStarRaasi = "நட்சத்திரம் / ராசி"
    override val fieldPada = "பாதம்"

    override fun siblingElderBrother(count: Int) = "$count மூத்த சகோதரர்"
    override fun siblingYoungerBrother(count: Int) = "$count இளைய சகோதரர்"
    override fun siblingElderSister(count: Int) = "$count மூத்த சகோதரி"
    override fun siblingYoungerSister(count: Int) = "$count இளைய சகோதரி"

    override val reachOutTitle = "தொடர்பு கொள்ளுங்கள்"
    override val reachOutBody = "இந்த சுயவிவரத்துடன் தொடர்பு கொள்ள பதிவு செய்து உள்நுழையவும்."
    override val reachOutCta = "பதிவு / உள்நுழைவு"

    override val vouchesTitle = "வவுச்கள்"
    override val vouchesEmpty = "இந்த குடும்பத்திற்கு இதுவரை யாரும் வவுச் செய்யவில்லை."
    override val voucherParent = "பெற்றோர்"
    override val voucherChild = "குழந்தை"
    override val voucherGuest = "விருந்தினர்"

    override val guestVouchIntro = "இந்த குடும்பத்தை தெரியுமா? அவர்களுக்காக வவுச் செய்யுங்கள் -- முதலில் உங்கள் மின்னஞ்சலுக்கு ஒரு குறியீடு அனுப்புவோம்."
    override val guestVouchEmailLabel = "உங்கள் மின்னஞ்சல்"
    override val guestVouchSendCode = "குறியீட்டை அனுப்பவும்"
    override val guestVouchSending = "குறியீடு அனுப்பப்படுகிறது…"
    override val guestVouchOtpError = "அந்த குறியீட்டை அனுப்ப முடியவில்லை -- மீண்டும் முயற்சிக்கவும்."
    override fun guestVouchSentTo(email: String) = "$email க்கு ஒரு குறியீட்டை அனுப்பியுள்ளோம்."
    override val guestVouchCodeLabel = "சரிபார்ப்புக் குறியீடு"
    override val guestVouchNameLabel = "உங்கள் பெயர்"
    override val guestVouchPhoneLabel = "உங்கள் தொலைபேசி எண்"
    override val guestVouchNoteLabel = "குறிப்பு (விருப்பத்தேர்வு)"
    override val guestVouchNotePlaceholder = "இந்த குடும்பத்தை நீங்கள் எப்படி அறிவீர்கள்?"
    override val guestVouchCta = "இந்த சுயவிவரத்திற்கு வவுச் செய்யுங்கள்"
    override val guestVouchSubmitting = "சமர்ப்பிக்கிறது…"
    override val guestVouchCreateError = "அது சமர்ப்பிக்கப்படவில்லை -- மீண்டும் முயற்சிக்கவும்."
    override val guestVouchDone = "நன்றி -- ஒப்புதலுக்கு அனுப்பப்பட்டது. அவர்கள் உறுதிசெய்தவுடன் இங்கே தெரியும்."

    override val footerNote = "D2M மூலம் பகிரப்பட்டது -- இது படிக்க மட்டுமான முன்னோட்டம், முழு ஆப் அல்ல."
}
