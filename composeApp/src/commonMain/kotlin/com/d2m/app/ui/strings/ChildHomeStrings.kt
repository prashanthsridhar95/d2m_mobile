package com.d2m.app.ui.strings

/** ChildHomeScreen.kt -- mirrors d2m_web's child_screens.json "home_*" keys. */
interface ChildHomeStrings {
    val errLoadHomeFeed: String
    val matchBanner: String
    val errRecordDecision: String
    fun greeting(name: String): String
    val greetingFallbackName: String
    val subtitleCaughtUp: String
    val subtitlePendingDecision: String
    fun subtitleNewRequest(count: Int): String
    val subtitleConsentPending: String
    val subtitleNewPicks: String
    val settingsContentDescription: String
    val noOneNewYetTitle: String
    val emptyListSubtitle: String
    val wantsToMatch: String
    val accept: String
    val decline: String
    fun consentRowTitle(name: String): String
    val consentFallbackName: String
    val grant: String
    val deny: String
    val waitingForResponse: String
    val todaysPicksTitle: String
    fun todaysPicksSubtitle(count: Int): String
    val takeALook: String
    val yourProfileTitle: String
    val yourProfileSubtitle: String
    val viewAndEdit: String
    val completeProfile: String
    val openChat: String
    val heroGettingSerious: String
    val heroMatched: String
    val emptyHeroTitle: String
    val emptyHeroSubtitle: String
    val emptyHeroCta: String
}

object ChildHomeStringsEn : ChildHomeStrings {
    override val errLoadHomeFeed = "Couldn't load your home feed."
    override val matchBanner = "It's a match! Head to Matches to say hi."
    override val errRecordDecision = "Couldn't record that decision."
    override fun greeting(name: String) = "Hey $name 👋"
    override val greetingFallbackName = "there"
    override val subtitleCaughtUp = "You're all caught up -- nothing waiting on you right now."
    override val subtitlePendingDecision = "Someone's waiting on your decision in Matches 💛"
    override fun subtitleNewRequest(count: Int) = if (count == 1) "1 new person sent you a request 👀" else "$count new people sent you a request 👀"
    override val subtitleConsentPending = "Your Sponsor wants to see more about someone you're talking to."
    override val subtitleNewPicks = "A few new profiles showed up for you today ✨"
    override val settingsContentDescription = "Settings"
    override val noOneNewYetTitle = "No one new yet"
    override val emptyListSubtitle = "But they will -- your profile's out there working for you."
    override val wantsToMatch = "Wants to match with you"
    override val accept = "Accept"
    override val decline = "Decline"
    override fun consentRowTitle(name: String) = "Your Sponsor wants to see more about $name"
    override val consentFallbackName = "your match"
    override val grant = "Grant"
    override val deny = "Deny"
    override val waitingForResponse = "Waiting for a response"
    override val todaysPicksTitle = "Today's picks"
    override fun todaysPicksSubtitle(count: Int) = "$count new ${if (count == 1) "profile" else "profiles"} worth a look"
    override val takeALook = "Take a look"
    override val yourProfileTitle = "How you're showing up"
    override val yourProfileSubtitle = "A peek at what people see when they check you out."
    override val viewAndEdit = "View & edit"
    override val completeProfile = "Complete profile"
    override val openChat = "Open chat"
    override val heroGettingSerious = "You two are getting serious 💛"
    override val heroMatched = "You matched -- say hi 💬"
    override val emptyHeroTitle = "Your person's out there"
    override val emptyHeroSubtitle = "No matches yet -- let's find someone worth a hello."
    override val emptyHeroCta = "Let's go"
}

object ChildHomeStringsHi : ChildHomeStrings {
    override val errLoadHomeFeed = "आपका होम फ़ीड लोड नहीं हो सका।"
    override val matchBanner = "यह एक मैच है! Matches में जाकर हाय कहें।"
    override val errRecordDecision = "वह फैसला दर्ज नहीं हो सका।"
    override fun greeting(name: String) = "हाय $name 👋"
    override val greetingFallbackName = "वहां"
    override val subtitleCaughtUp = "फ़िलहाल आपके लिए कुछ भी पेंडिंग नहीं है -- सब अपडेट है।"
    override val subtitlePendingDecision = "Matches में कोई आपके फैसले का इंतज़ार कर रहा है 💛"
    override fun subtitleNewRequest(count: Int) = if (count == 1) "एक नए व्यक्ति ने आपको रिक्वेस्ट भेजी है 👀" else "$count नए लोगों ने आपको रिक्वेस्ट भेजी है 👀"
    override val subtitleConsentPending = "आपका स्पॉन्सर उस व्यक्ति के बारे में और जानकारी देखना चाहता है जिससे आप बात कर रहे हैं।"
    override val subtitleNewPicks = "आज आपके लिए कुछ नई प्रोफ़ाइलें आई हैं ✨"
    override val settingsContentDescription = "सेटिंग्स"
    override val noOneNewYetTitle = "अभी कोई नया नहीं"
    override val emptyListSubtitle = "पर जल्द आएंगे -- आपकी प्रोफ़ाइल आपके लिए काम कर रही है।"
    override val wantsToMatch = "आपसे मैच करना चाहते हैं"
    override val accept = "स्वीकार करें"
    override val decline = "अस्वीकार करें"
    override fun consentRowTitle(name: String) = "आपका स्पॉन्सर $name के बारे में और जानना चाहता है"
    override val consentFallbackName = "आपका मैच"
    override val grant = "अनुमति दें"
    override val deny = "मना करें"
    override val waitingForResponse = "जवाब का इंतज़ार है"
    override val todaysPicksTitle = "आज की चुनिंदा प्रोफ़ाइलें"
    override fun todaysPicksSubtitle(count: Int) = "$count नई ${if (count == 1) "प्रोफ़ाइल" else "प्रोफ़ाइलें"} देखने लायक"
    override val takeALook = "देखें"
    override val yourProfileTitle = "आप कैसे दिख रहे हैं"
    override val yourProfileSubtitle = "जब लोग आपको देखते हैं तो उन्हें क्या दिखता है, एक झलक।"
    override val viewAndEdit = "देखें और बदलें"
    override val completeProfile = "प्रोफ़ाइल पूरी करें"
    override val openChat = "चैट खोलें"
    override val heroGettingSerious = "आप दोनों की बात अब गंभीर हो रही है 💛"
    override val heroMatched = "आपका मैच हो गया -- हाय कहें 💬"
    override val emptyHeroTitle = "आपका हमसफ़र कहीं है"
    override val emptyHeroSubtitle = "अभी कोई मैच नहीं -- चलिए किसी खास से मुलाक़ात ढूंढते हैं।"
    override val emptyHeroCta = "चलिए शुरू करें"
}

object ChildHomeStringsTa : ChildHomeStrings {
    override val errLoadHomeFeed = "உங்கள் முகப்புப் பக்கத்தை ஏற்ற முடியவில்லை."
    override val matchBanner = "இது ஒரு மேட்ச்! ஹாய் சொல்ல Matches-க்குச் செல்லுங்கள்."
    override val errRecordDecision = "அந்த முடிவைப் பதிவு செய்ய முடியவில்லை."
    override fun greeting(name: String) = "ஹாய் $name 👋"
    override val greetingFallbackName = "நண்பரே"
    override val subtitleCaughtUp = "இப்போதைக்கு உங்களுக்காக காத்திருப்பது எதுவும் இல்லை -- எல்லாம் தயார்."
    override val subtitlePendingDecision = "Matches-இல் உங்கள் முடிவுக்காக ஒருவர் காத்திருக்கிறார் 💛"
    override fun subtitleNewRequest(count: Int) = if (count == 1) "ஒருவர் உங்களுக்கு புதிய கோரிக்கை அனுப்பியுள்ளார் 👀" else "$count பேர் உங்களுக்கு புதிய கோரிக்கை அனுப்பியுள்ளனர் 👀"
    override val subtitleConsentPending = "நீங்கள் பேசிக்கொண்டிருக்கும் ஒருவரைப் பற்றி உங்கள் ஸ்பான்சர் மேலும் தெரிந்துகொள்ள விரும்புகிறார்."
    override val subtitleNewPicks = "இன்று உங்களுக்காக சில புதிய சுயவிவரங்கள் வந்துள்ளன ✨"
    override val settingsContentDescription = "அமைப்புகள்"
    override val noOneNewYetTitle = "இன்னும் யாரும் புதிதாக இல்லை"
    override val emptyListSubtitle = "ஆனால் வருவார்கள் -- உங்கள் சுயவிவரம் உங்களுக்காக வேலை செய்துகொண்டிருக்கிறது."
    override val wantsToMatch = "உங்களுடன் மேட்ச் செய்ய விரும்புகிறார்"
    override val accept = "ஏற்றுக்கொள்"
    override val decline = "நிராகரி"
    override fun consentRowTitle(name: String) = "$name பற்றி உங்கள் ஸ்பான்சர் மேலும் தெரிந்துகொள்ள விரும்புகிறார்"
    override val consentFallbackName = "உங்கள் மேட்ச்"
    override val grant = "அனுமதி கொடு"
    override val deny = "மறு"
    override val waitingForResponse = "பதிலுக்காக காத்திருக்கிறது"
    override val todaysPicksTitle = "இன்றைய தேர்வுகள்"
    override fun todaysPicksSubtitle(count: Int) = "$count புதிய ${if (count == 1) "சுயவிவரம்" else "சுயவிவரங்கள்"} பார்க்கத் தகுந்தவை"
    override val takeALook = "பார்க்கவும்"
    override val yourProfileTitle = "நீங்கள் எப்படித் தெரிகிறீர்கள்"
    override val yourProfileSubtitle = "மற்றவர்கள் உங்களைப் பார்க்கும்போது என்ன தெரியும் என்பதன் ஒரு பார்வை."
    override val viewAndEdit = "பார்த்து மாற்றவும்"
    override val completeProfile = "சுயவிவரத்தை முடிக்கவும்"
    override val openChat = "சாட் திற"
    override val heroGettingSerious = "நீங்கள் இருவரும் தீவிரமாகிறீர்கள் 💛"
    override val heroMatched = "உங்களுக்கு மேட்ச் கிடைத்தது -- ஹாய் சொல்லுங்கள் 💬"
    override val emptyHeroTitle = "உங்கள் துணை எங்கோ இருக்கிறார்"
    override val emptyHeroSubtitle = "இன்னும் மேட்ச் இல்லை -- ஒரு ஹலோவுக்கு தகுதியான ஒருவரைத் தேடுவோம்."
    override val emptyHeroCta = "செல்லலாம்"
}
