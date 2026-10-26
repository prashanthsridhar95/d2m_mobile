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
    val undo: String
    fun passedSnack(firstName: String): String
    fun acceptedSnack(firstName: String): String
    fun snoozedSnack(firstName: String): String
    val actionFailed: String
    val stampAccept: String
    val stampPass: String
    val stampSnooze: String
    val caughtUpTitle: String
    val caughtUpBody: String
    val checkAgain: String
    val morePhotos: String
    val cdProfileCard: String
    val passFab: String
    val likeProfileAction: String
    val likeThisPhoto: String
    val likeThisAnswer: String
    val likeProfile: String
    fun likePhotoTitle(name: String): String
    fun likePromptTitle(name: String): String
    fun likeProfileTitle(name: String): String
    val likePhotoHint: String
    fun likeProfileHint(name: String): String
    val commentPlaceholder: String
    val cancel: String
    val sendLike: String
    val sending: String
    fun passedForNow(name: String): String
    val likeFailed: String
    fun matchBanner(name: String): String
    val sayHi: String
    val passLabel: String
    fun passCd(name: String): String
    val coachTitle: String
    val coachBody: String
    val coachOk: String
    fun passedWrap(name: String): String
    fun passedOnly(name: String): String
    fun passFailed(name: String): String
    val retry: String
    fun likeSent(name: String): String
    val commentLabel: String
    fun likeKept(error: String): String
    val tryAgain: String
    val fromFamily: String
    fun photosCount(n: Int): String
    val factWork: String
    val factHeight: String
    val factEducation: String
    val factReligion: String
    val factLanguage: String
    val factGothram: String
    val yourPreferences: String
    val parentsPreferences: String
    val whatMattersMost: String
    val seriousTitle: String
    val seriousBody: String
    val goToMatches: String
    val errOpenProfile: String
    val backToMessages: String
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
    override val undo = "Undo"
    override fun passedSnack(firstName: String) = "Passed on $firstName"
    override fun acceptedSnack(firstName: String) = "Accepted $firstName"
    override fun snoozedSnack(firstName: String) = "Snoozed $firstName"
    override val actionFailed = "Couldn't save that. The profile is back in your queue."
    override val stampAccept = "LIKE"
    override val stampPass = "NOT NOW"
    override val stampSnooze = "SNOOZE"
    override val caughtUpTitle = "You're all caught up"
    override val caughtUpBody = "You've seen every suggestion for now. New matches show up here as they arrive."
    override val checkAgain = "Check again"
    override val morePhotos = "More photos"
    override val cdProfileCard = "Profile card"
    override val passFab = "Not now"
    override val likeProfileAction = "Like profile"
    override val likeThisPhoto = "Like this photo"
    override val likeThisAnswer = "Like this answer"
    override val likeProfile = "Like their profile"
    override fun likePhotoTitle(name: String) = "Like $name's photo"
    override fun likePromptTitle(name: String) = "Like $name's answer"
    override fun likeProfileTitle(name: String) = "Like $name's profile"
    override val likePhotoHint = "They'll see which photo you liked."
    override fun likeProfileHint(name: String) = "Send a request to $name."
    override val commentPlaceholder = "A few kind words go a long way…"
    override val cancel = "Cancel"
    override val sendLike = "Send like"
    override val sending = "Sending…"
    override fun passedForNow(name: String) = "Not now for $name. We'll bring them back later."
    override val likeFailed = "Couldn't send that. Please try again."
    override fun matchBanner(name: String) = "It's a match with $name!"
    override val sayHi = "Say hi"
    override val passLabel = "Not now"
    override fun passCd(name: String) = "Not now. $name will come back later."
    override val coachTitle = "Not now -- we'll bring them back later"
    override val coachBody = "Pass is never a permanent no. You can undo it right away."
    override val coachOk = "Got it"
    override fun passedWrap(name: String) = "That's everyone. Starting over -- $name will be back later."
    override fun passedOnly(name: String) = "That's everyone for now. $name will be back later."
    override fun passFailed(name: String) = "Couldn't save that pass for $name."
    override val retry = "Retry"
    override fun likeSent(name: String) = "Like sent to $name. We'll tell you if they like you back."
    override val commentLabel = "Say why (optional)"
    override fun likeKept(error: String) = "$error Your comment is saved."
    override val tryAgain = "Try again"
    override val fromFamily = "Suggested by their family"
    override fun photosCount(n: Int) = "$n photos"
    override val factWork = "Work"
    override val factHeight = "Height"
    override val factEducation = "Education"
    override val factReligion = "Religion"
    override val factLanguage = "Mother tongue"
    override val factGothram = "Gothram"
    override val yourPreferences = "Your preferences"
    override val parentsPreferences = "Parent's preferences"
    override val whatMattersMost = "What matters most to me"
    override val seriousTitle = "You're already Serious with someone"
    override val seriousBody = "Discover is paused while you're in an exclusive Serious Mode relationship. Head to Matches to keep talking with them."
    override val goToMatches = "Go to Matches"
    override val errOpenProfile = "Couldn't open that profile right now."
    override val backToMessages = "Back to messages"
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
    override val undo = "पूर्ववत करें"
    override fun passedSnack(firstName: String) = "$firstName को पास किया"
    override fun acceptedSnack(firstName: String) = "$firstName को स्वीकार किया"
    override fun snoozedSnack(firstName: String) = "$firstName को स्नूज़ किया"
    override val actionFailed = "सहेजा नहीं जा सका। प्रोफ़ाइल आपकी कतार में वापस आ गई है।"
    override val stampAccept = "स्वीकार"
    override val stampPass = "अभी नहीं"
    override val stampSnooze = "स्नूज़"
    override val caughtUpTitle = "आप सब देख चुके हैं"
    override val caughtUpBody = "अभी के सभी सुझाव आप देख चुके हैं। नए मैच आते ही यहाँ दिखेंगे।"
    override val checkAgain = "फिर से जाँचें"
    override val morePhotos = "और फ़ोटो"
    override val cdProfileCard = "प्रोफ़ाइल कार्ड"
    override val passFab = "अभी नहीं"
    override val likeProfileAction = "प्रोफ़ाइल पसंद करें"
    override val likeThisPhoto = "यह फ़ोटो पसंद करें"
    override val likeThisAnswer = "यह जवाब पसंद करें"
    override val likeProfile = "प्रोफ़ाइल पसंद करें"
    override fun likePhotoTitle(name: String) = "$name की फ़ोटो पसंद करें"
    override fun likePromptTitle(name: String) = "$name का जवाब पसंद करें"
    override fun likeProfileTitle(name: String) = "$name की प्रोफ़ाइल पसंद करें"
    override val likePhotoHint = "उन्हें दिखेगा कि आपने कौन सी फ़ोटो पसंद की।"
    override fun likeProfileHint(name: String) = "$name को अनुरोध भेजें।"
    override val commentPlaceholder = "कुछ अच्छे शब्द बहुत मायने रखते हैं…"
    override val cancel = "रद्द करें"
    override val sendLike = "पसंद भेजें"
    override val sending = "भेज रहे हैं…"
    override fun passedForNow(name: String) = "$name के लिए अभी नहीं। हम उन्हें बाद में वापस लाएंगे।"
    override val likeFailed = "भेजा नहीं जा सका। कृपया फिर कोशिश करें।"
    override fun matchBanner(name: String) = "$name के साथ मैच हो गया!"
    override val sayHi = "नमस्ते कहें"
    override val passLabel = "अभी नहीं"
    override fun passCd(name: String) = "अभी नहीं। $name बाद में फिर दिखेंगे।"
    override val coachTitle = "अभी नहीं -- हम उन्हें बाद में वापस लाएंगे"
    override val coachBody = "छोड़ना कभी स्थायी 'ना' नहीं है। आप इसे तुरंत पूर्ववत कर सकते हैं।"
    override val coachOk = "समझ गया"
    override fun passedWrap(name: String) = "सब देख लिए। फिर से शुरू -- $name बाद में फिर दिखेंगे।"
    override fun passedOnly(name: String) = "अभी के लिए बस इतने ही। $name बाद में फिर दिखेंगे।"
    override fun passFailed(name: String) = "$name को छोड़ना सहेजा नहीं जा सका।"
    override val retry = "फिर कोशिश करें"
    override fun likeSent(name: String) = "$name को पसंद भेज दी गई। अगर वे भी आपको पसंद करेंगे तो हम बताएंगे।"
    override val commentLabel = "वजह बताएं (वैकल्पिक)"
    override fun likeKept(error: String) = "$error आपकी टिप्पणी सहेजी हुई है।"
    override val tryAgain = "फिर कोशिश करें"
    override val fromFamily = "उनके परिवार द्वारा सुझाया गया"
    override fun photosCount(n: Int) = "$n फ़ोटो"
    override val factWork = "काम"
    override val factHeight = "कद"
    override val factEducation = "शिक्षा"
    override val factReligion = "धर्म"
    override val factLanguage = "मातृभाषा"
    override val factGothram = "गोत्र"
    override val yourPreferences = "आपकी पसंद"
    override val parentsPreferences = "पैरेंट की पसंद"
    override val whatMattersMost = "मेरे लिए सबसे ज़्यादा क्या मायने रखता है"
    override val seriousTitle = "आप पहले से ही किसी के साथ गंभीर रिश्ते में हैं"
    override val seriousBody = "जब तक आप एक्सक्लूसिव Serious Mode रिश्ते में हैं, Discover रुका हुआ है। उनसे बात जारी रखने के लिए Matches पर जाएं।"
    override val goToMatches = "Matches पर जाएं"
    override val errOpenProfile = "अभी वह प्रोफ़ाइल नहीं खोली जा सकी।"
    override val backToMessages = "संदेशों पर वापस"
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
    override val undo = "செயல்தவிர்"
    override fun passedSnack(firstName: String) = "$firstName ஐ தவிர்த்தீர்கள்"
    override fun acceptedSnack(firstName: String) = "$firstName ஐ ஏற்றுக்கொண்டீர்கள்"
    override fun snoozedSnack(firstName: String) = "$firstName ஐ ஸ்னூஸ் செய்தீர்கள்"
    override val actionFailed = "சேமிக்க முடியவில்லை. சுயவிவரம் மீண்டும் வரிசையில் உள்ளது."
    override val stampAccept = "ஏற்பு"
    override val stampPass = "இப்போது வேண்டாம்"
    override val stampSnooze = "ஸ்னூஸ்"
    override val caughtUpTitle = "அனைத்தையும் பார்த்துவிட்டீர்கள்"
    override val caughtUpBody = "தற்போதைய எல்லா பரிந்துரைகளையும் பார்த்துவிட்டீர்கள். புதிய பொருத்தங்கள் வந்ததும் இங்கே தெரியும்."
    override val checkAgain = "மீண்டும் பார்"
    override val morePhotos = "மேலும் புகைப்படங்கள்"
    override val cdProfileCard = "சுயவிவர அட்டை"
    override val passFab = "இப்போது வேண்டாம்"
    override val likeProfileAction = "சுயவிவரத்தை விரும்பு"
    override val likeThisPhoto = "இந்தப் புகைப்படத்தை விரும்பு"
    override val likeThisAnswer = "இந்த பதிலை விரும்பு"
    override val likeProfile = "சுயவிவரத்தை விரும்பு"
    override fun likePhotoTitle(name: String) = "$name-ன் புகைப்படத்தை விரும்பு"
    override fun likePromptTitle(name: String) = "$name-ன் பதிலை விரும்பு"
    override fun likeProfileTitle(name: String) = "$name-ன் சுயவிவரத்தை விரும்பு"
    override val likePhotoHint = "எந்தப் புகைப்படத்தை விரும்பினீர்கள் என்பதை அவர்கள் பார்ப்பார்கள்."
    override fun likeProfileHint(name: String) = "$name-க்கு கோரிக்கை அனுப்பு."
    override val commentPlaceholder = "சில அன்பான வார்த்தைகள் பெரிய மாற்றத்தைத் தரும்…"
    override val cancel = "ரத்து செய்"
    override val sendLike = "விருப்பத்தை அனுப்பு"
    override val sending = "அனுப்புகிறது…"
    override fun passedForNow(name: String) = "$name-க்கு இப்போது வேண்டாம். அவர்களை பின்னர் மீண்டும் கொண்டு வருவோம்."
    override val likeFailed = "அனுப்ப முடியவில்லை. மீண்டும் முயலவும்."
    override fun matchBanner(name: String) = "$name உடன் பொருத்தம்!"
    override val sayHi = "வணக்கம் சொல்"
    override val passLabel = "இப்போது வேண்டாம்"
    override fun passCd(name: String) = "இப்போது வேண்டாம். $name பின்னர் மீண்டும் வருவார்."
    override val coachTitle = "இப்போது வேண்டாம் -- பின்னர் மீண்டும் கொண்டு வருவோம்"
    override val coachBody = "தவிர்ப்பது ஒருபோதும் நிரந்தர மறுப்பு அல்ல. உடனே செயல்தவிர்க்கலாம்."
    override val coachOk = "சரி"
    override fun passedWrap(name: String) = "எல்லோரையும் பார்த்துவிட்டீர்கள். மீண்டும் தொடங்குகிறது -- $name பின்னர் வருவார்."
    override fun passedOnly(name: String) = "இப்போதைக்கு இவ்வளவுதான். $name பின்னர் மீண்டும் வருவார்."
    override fun passFailed(name: String) = "$name-ஐ தவிர்த்ததைச் சேமிக்க முடியவில்லை."
    override val retry = "மீண்டும் முயல்"
    override fun likeSent(name: String) = "$name-க்கு விருப்பம் அனுப்பப்பட்டது. அவர்களும் விரும்பினால் தெரிவிப்போம்."
    override val commentLabel = "ஏன் என்று சொல்லுங்கள் (விருப்பம்)"
    override fun likeKept(error: String) = "$error உங்கள் கருத்து சேமிக்கப்பட்டுள்ளது."
    override val tryAgain = "மீண்டும் முயல்"
    override val fromFamily = "அவர்கள் குடும்பம் பரிந்துரைத்தது"
    override fun photosCount(n: Int) = "$n புகைப்படங்கள்"
    override val factWork = "வேலை"
    override val factHeight = "உயரம்"
    override val factEducation = "கல்வி"
    override val factReligion = "மதம்"
    override val factLanguage = "தாய்மொழி"
    override val factGothram = "கோத்திரம்"
    override val yourPreferences = "உங்கள் விருப்பங்கள்"
    override val parentsPreferences = "பெற்றோரின் விருப்பங்கள்"
    override val whatMattersMost = "எனக்கு மிக முக்கியமானது"
    override val seriousTitle = "நீங்கள் ஏற்கெனவே ஒருவருடன் தீவிரமாக உள்ளீர்கள்"
    override val seriousBody = "நீங்கள் பிரத்யேக Serious Mode உறவில் இருக்கும் வரை Discover இடைநிறுத்தப்பட்டுள்ளது. அவர்களுடன் தொடர்ந்து பேச Matches-க்குச் செல்லுங்கள்."
    override val goToMatches = "Matches-க்குச் செல்லுங்கள்"
    override val errOpenProfile = "இப்போது அந்த சுயவிவரத்தைத் திறக்க முடியவில்லை."
    override val backToMessages = "செய்திகளுக்குத் திரும்பு"
}
