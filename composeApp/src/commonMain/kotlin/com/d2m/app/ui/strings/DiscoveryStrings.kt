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
    override val stampPass = "PASS"
    override val stampSnooze = "SNOOZE"
    override val caughtUpTitle = "You're all caught up"
    override val caughtUpBody = "You've seen every suggestion for now. New matches show up here as they arrive."
    override val checkAgain = "Check again"
    override val morePhotos = "More photos"
    override val cdProfileCard = "Profile card"
    override val passFab = "Pass for now"
    override val likeProfileAction = "Like profile"
    override val likeThisPhoto = "Like this photo"
    override val likeThisAnswer = "Like this answer"
    override val likeProfile = "Like their profile"
    override fun likePhotoTitle(name: String) = "Like $name's photo"
    override fun likePromptTitle(name: String) = "Like $name's answer"
    override fun likeProfileTitle(name: String) = "Like $name's profile"
    override val likePhotoHint = "They'll see which photo you liked."
    override fun likeProfileHint(name: String) = "Send a request to $name."
    override val commentPlaceholder = "Add a comment (optional)"
    override val cancel = "Cancel"
    override val sendLike = "Send like"
    override val sending = "Sending…"
    override fun passedForNow(name: String) = "Skipped $name for now. They'll come back later."
    override val likeFailed = "Couldn't send that. Please try again."
    override fun matchBanner(name: String) = "It's a match with $name!"
    override val sayHi = "Say hi"
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
    override val stampPass = "पास"
    override val stampSnooze = "स्नूज़"
    override val caughtUpTitle = "आप सब देख चुके हैं"
    override val caughtUpBody = "अभी के सभी सुझाव आप देख चुके हैं। नए मैच आते ही यहाँ दिखेंगे।"
    override val checkAgain = "फिर से जाँचें"
    override val morePhotos = "और फ़ोटो"
    override val cdProfileCard = "प्रोफ़ाइल कार्ड"
    override val passFab = "अभी छोड़ें"
    override val likeProfileAction = "प्रोफ़ाइल पसंद करें"
    override val likeThisPhoto = "यह फ़ोटो पसंद करें"
    override val likeThisAnswer = "यह जवाब पसंद करें"
    override val likeProfile = "प्रोफ़ाइल पसंद करें"
    override fun likePhotoTitle(name: String) = "$name की फ़ोटो पसंद करें"
    override fun likePromptTitle(name: String) = "$name का जवाब पसंद करें"
    override fun likeProfileTitle(name: String) = "$name की प्रोफ़ाइल पसंद करें"
    override val likePhotoHint = "उन्हें दिखेगा कि आपने कौन सी फ़ोटो पसंद की।"
    override fun likeProfileHint(name: String) = "$name को अनुरोध भेजें।"
    override val commentPlaceholder = "टिप्पणी जोड़ें (वैकल्पिक)"
    override val cancel = "रद्द करें"
    override val sendLike = "पसंद भेजें"
    override val sending = "भेज रहे हैं…"
    override fun passedForNow(name: String) = "$name को अभी छोड़ दिया। वे बाद में फिर दिखेंगे।"
    override val likeFailed = "भेजा नहीं जा सका। कृपया फिर कोशिश करें।"
    override fun matchBanner(name: String) = "$name के साथ मैच हो गया!"
    override val sayHi = "नमस्ते कहें"
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
    override val stampPass = "தவிர்"
    override val stampSnooze = "ஸ்னூஸ்"
    override val caughtUpTitle = "அனைத்தையும் பார்த்துவிட்டீர்கள்"
    override val caughtUpBody = "தற்போதைய எல்லா பரிந்துரைகளையும் பார்த்துவிட்டீர்கள். புதிய பொருத்தங்கள் வந்ததும் இங்கே தெரியும்."
    override val checkAgain = "மீண்டும் பார்"
    override val morePhotos = "மேலும் புகைப்படங்கள்"
    override val cdProfileCard = "சுயவிவர அட்டை"
    override val passFab = "இப்போதைக்கு தவிர்"
    override val likeProfileAction = "சுயவிவரத்தை விரும்பு"
    override val likeThisPhoto = "இந்தப் புகைப்படத்தை விரும்பு"
    override val likeThisAnswer = "இந்த பதிலை விரும்பு"
    override val likeProfile = "சுயவிவரத்தை விரும்பு"
    override fun likePhotoTitle(name: String) = "$name-ன் புகைப்படத்தை விரும்பு"
    override fun likePromptTitle(name: String) = "$name-ன் பதிலை விரும்பு"
    override fun likeProfileTitle(name: String) = "$name-ன் சுயவிவரத்தை விரும்பு"
    override val likePhotoHint = "எந்தப் புகைப்படத்தை விரும்பினீர்கள் என்பதை அவர்கள் பார்ப்பார்கள்."
    override fun likeProfileHint(name: String) = "$name-க்கு கோரிக்கை அனுப்பு."
    override val commentPlaceholder = "கருத்து சேர்க்கவும் (விருப்பம்)"
    override val cancel = "ரத்து செய்"
    override val sendLike = "விருப்பத்தை அனுப்பு"
    override val sending = "அனுப்புகிறது…"
    override fun passedForNow(name: String) = "$name-ஐ இப்போதைக்கு தவிர்த்தீர்கள். அவர்கள் பின்னர் மீண்டும் வருவார்கள்."
    override val likeFailed = "அனுப்ப முடியவில்லை. மீண்டும் முயலவும்."
    override fun matchBanner(name: String) = "$name உடன் பொருத்தம்!"
    override val sayHi = "வணக்கம் சொல்"
}
