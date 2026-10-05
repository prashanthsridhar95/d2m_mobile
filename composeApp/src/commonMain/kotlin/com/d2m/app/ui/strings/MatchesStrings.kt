package com.d2m.app.ui.strings

/** MatchesScreen.kt -- mirrors d2m_web's "messages" i18n namespace where concepts overlap. */
interface MatchesStrings {
    val yesterday: String
    val pageTitle: String
    val tabMatches: String
    val tabReceived: String
    val tabSent: String
    val noMatchesTitle: String
    val noMatchesBody: String
    val noRequestsWaitingTitle: String
    val noRequestsWaitingBody: String
    val wantsToMatch: String
    val accept: String
    val decline: String
    val errAcceptRequest: String
    val errDeclineRequest: String
    val noRequestsSentTitle: String
    val noRequestsSentBody: String
    val waitingForReply: String
    val errConfirmUnion: String
    val errUpdateGalleryOptin: String
    val errSendSeriousModeRequest: String
    val errRevokeSeriousMode: String
    val errUnmatch: String
    val errAcceptSeriousModeRequest: String
    val errDeclineSeriousModeRequest: String
    val errRefreshMatches: String
    val errLoadMatches: String
    val errLoadReceivedRequests: String
    val errLoadSentRequests: String
    fun waitingOnThemToConfirm(name: String): String
    fun themWantsToConfirm(name: String): String
    val confirmUnion: String
    val unionConfirmedTitle: String
    val unionConfirmedBody: String
    val optInSuccessGallery: String
    val viewSuccessGallery: String
    val messageDeleted: String
    val typing: String
    val online: String
    fun seriousModePendingSuffix(base: String): String
    val backToMatches: String
    val endToEndEncrypted: String
    val audioCall: String
    val videoCall: String
    val moreOptions: String
    val acceptSeriousMode: String
    val declineSeriousMode: String
    val goSerious: String
    val revokeSeriousMode: String
    val unmatch: String
    val statusMatched: String
    val statusSeriousExploration: String
    val statusSunsetting: String
    val statusClosed: String
}

object MatchesStringsEn : MatchesStrings {
    override val yesterday = "Yesterday"
    override val pageTitle = "Matches"
    override val tabMatches = "Matches"
    override val tabReceived = "Received"
    override val tabSent = "Sent"
    override val noMatchesTitle = "No matches yet"
    override val noMatchesBody = "Once you and someone else both accept, they'll show up here."
    override val noRequestsWaitingTitle = "No requests waiting"
    override val noRequestsWaitingBody = "When someone sends you a request, it shows up here."
    override val wantsToMatch = "Wants to match with you"
    override val accept = "Accept"
    override val decline = "Decline"
    override val errAcceptRequest = "Couldn't accept that request."
    override val errDeclineRequest = "Couldn't decline that request."
    override val noRequestsSentTitle = "No requests sent"
    override val noRequestsSentBody = "Accept a suggestion from Discover to send a request."
    override val waitingForReply = "Waiting for a reply"
    override val errConfirmUnion = "Couldn't confirm the union."
    override val errUpdateGalleryOptin = "Couldn't update gallery opt-in."
    override val errSendSeriousModeRequest = "Couldn't send that Serious Mode request."
    override val errRevokeSeriousMode = "Couldn't revoke Serious Mode."
    override val errUnmatch = "Couldn't unmatch."
    override val errAcceptSeriousModeRequest = "Couldn't accept that Serious Mode request."
    override val errDeclineSeriousModeRequest = "Couldn't decline that Serious Mode request."
    override val errRefreshMatches = "Couldn't refresh your matches."
    override val errLoadMatches = "Couldn't load your matches."
    override val errLoadReceivedRequests = "Couldn't load received requests."
    override val errLoadSentRequests = "Couldn't load sent requests."
    override fun waitingOnThemToConfirm(name: String) = "Waiting on $name to confirm your union."
    override fun themWantsToConfirm(name: String) = "$name wants to confirm your union."
    override val confirmUnion = "Confirm Union"
    override val unionConfirmedTitle = "Union confirmed 🎉"
    override val unionConfirmedBody = "Share your story in the Success Gallery? Anonymized (5-year age buckets only) -- requires both sides to opt in, and you can revoke any time."
    override val optInSuccessGallery = "Opt in to the Success Gallery"
    override val viewSuccessGallery = "View the Success Gallery"
    override val messageDeleted = "This message was deleted"
    override val typing = "typing…"
    override val online = "Online"
    override fun seriousModePendingSuffix(base: String) = "$base · Serious Mode pending"
    override val backToMatches = "Back to matches"
    override val endToEndEncrypted = "End-to-end encrypted"
    override val audioCall = "Audio call"
    override val videoCall = "Video call"
    override val moreOptions = "More options"
    override val acceptSeriousMode = "Accept Serious Mode"
    override val declineSeriousMode = "Decline Serious Mode"
    override val goSerious = "Go Serious"
    override val revokeSeriousMode = "Revoke Serious Mode"
    override val unmatch = "Unmatch"
    override val statusMatched = "Matched"
    override val statusSeriousExploration = "Serious exploration"
    override val statusSunsetting = "Sunsetting"
    override val statusClosed = "Closed"
}

object MatchesStringsHi : MatchesStrings {
    override val yesterday = "कल"
    override val pageTitle = "Matches"
    override val tabMatches = "मैच"
    override val tabReceived = "प्राप्त"
    override val tabSent = "भेजे गए"
    override val noMatchesTitle = "अभी कोई मैच नहीं"
    override val noMatchesBody = "जब आप और कोई दूसरा व्यक्ति दोनों स्वीकार करेंगे, तो वे यहां दिखेंगे।"
    override val noRequestsWaitingTitle = "कोई रिक्वेस्ट प्रतीक्षा में नहीं"
    override val noRequestsWaitingBody = "जब कोई आपको रिक्वेस्ट भेजेगा, तो वह यहां दिखेगी।"
    override val wantsToMatch = "आपसे मैच करना चाहते हैं"
    override val accept = "स्वीकार करें"
    override val decline = "अस्वीकार करें"
    override val errAcceptRequest = "वह रिक्वेस्ट स्वीकार नहीं हो सकी।"
    override val errDeclineRequest = "वह रिक्वेस्ट अस्वीकार नहीं हो सकी।"
    override val noRequestsSentTitle = "कोई रिक्वेस्ट नहीं भेजी गई"
    override val noRequestsSentBody = "रिक्वेस्ट भेजने के लिए Discover से कोई सुझाव स्वीकार करें।"
    override val waitingForReply = "जवाब का इंतज़ार है"
    override val errConfirmUnion = "विवाह की पुष्टि नहीं हो सकी।"
    override val errUpdateGalleryOptin = "गैलरी ऑप्ट-इन अपडेट नहीं हो सका।"
    override val errSendSeriousModeRequest = "वह सीरियस मोड रिक्वेस्ट नहीं भेजी जा सकी।"
    override val errRevokeSeriousMode = "सीरियस मोड रद्द नहीं किया जा सका।"
    override val errUnmatch = "अनमैच नहीं किया जा सका।"
    override val errAcceptSeriousModeRequest = "वह सीरियस मोड रिक्वेस्ट स्वीकार नहीं हो सकी।"
    override val errDeclineSeriousModeRequest = "वह सीरियस मोड रिक्वेस्ट अस्वीकार नहीं हो सकी।"
    override val errRefreshMatches = "आपके मैच रीफ्रेश नहीं हो सके।"
    override val errLoadMatches = "आपके मैच लोड नहीं हो सके।"
    override val errLoadReceivedRequests = "प्राप्त रिक्वेस्ट लोड नहीं हो सकीं।"
    override val errLoadSentRequests = "भेजी गई रिक्वेस्ट लोड नहीं हो सकीं।"
    override fun waitingOnThemToConfirm(name: String) = "$name के विवाह की पुष्टि करने का इंतज़ार है।"
    override fun themWantsToConfirm(name: String) = "$name विवाह की पुष्टि करना चाहते हैं।"
    override val confirmUnion = "विवाह की पुष्टि करें"
    override val unionConfirmedTitle = "विवाह की पुष्टि हो गई 🎉"
    override val unionConfirmedBody = "Success Gallery में अपनी कहानी साझा करें? गुमनाम (केवल 5-वर्ष की आयु सीमा) -- दोनों पक्षों की सहमति ज़रूरी है, और आप कभी भी इसे वापस ले सकते हैं।"
    override val optInSuccessGallery = "Success Gallery में शामिल हों"
    override val viewSuccessGallery = "Success Gallery देखें"
    override val messageDeleted = "यह संदेश हटा दिया गया था"
    override val typing = "लिख रहे हैं…"
    override val online = "ऑनलाइन"
    override fun seriousModePendingSuffix(base: String) = "$base · सीरियस मोड लंबित"
    override val backToMatches = "Matches पर वापस जाएं"
    override val endToEndEncrypted = "एंड-टू-एंड एन्क्रिप्टेड"
    override val audioCall = "ऑडियो कॉल"
    override val videoCall = "वीडियो कॉल"
    override val moreOptions = "और विकल्प"
    override val acceptSeriousMode = "सीरियस मोड स्वीकार करें"
    override val declineSeriousMode = "सीरियस मोड अस्वीकार करें"
    override val goSerious = "सीरियस मोड शुरू करें"
    override val revokeSeriousMode = "सीरियस मोड रद्द करें"
    override val unmatch = "अनमैच करें"
    override val statusMatched = "मैच हुआ"
    override val statusSeriousExploration = "सीरियस एक्सप्लोरेशन"
    override val statusSunsetting = "समाप्त हो रहा है"
    override val statusClosed = "बंद"
}

object MatchesStringsTa : MatchesStrings {
    override val yesterday = "நேற்று"
    override val pageTitle = "Matches"
    override val tabMatches = "மேட்சஸ்"
    override val tabReceived = "பெறப்பட்டவை"
    override val tabSent = "அனுப்பப்பட்டவை"
    override val noMatchesTitle = "இன்னும் மேட்ச் இல்லை"
    override val noMatchesBody = "நீங்களும் மற்றவரும் ஏற்றுக்கொண்டதும், அவை இங்கே தெரியும்."
    override val noRequestsWaitingTitle = "காத்திருக்கும் கோரிக்கைகள் இல்லை"
    override val noRequestsWaitingBody = "யாராவது உங்களுக்கு கோரிக்கை அனுப்பும்போது, அது இங்கே தெரியும்."
    override val wantsToMatch = "உங்களுடன் மேட்ச் செய்ய விரும்புகிறார்"
    override val accept = "ஏற்றுக்கொள்"
    override val decline = "நிராகரி"
    override val errAcceptRequest = "அந்த கோரிக்கையை ஏற்க முடியவில்லை."
    override val errDeclineRequest = "அந்த கோரிக்கையை நிராகரிக்க முடியவில்லை."
    override val noRequestsSentTitle = "கோரிக்கைகள் அனுப்பப்படவில்லை"
    override val noRequestsSentBody = "கோரிக்கை அனுப்ப Discover-இலிருந்து ஒரு பரிந்துரையை ஏற்கவும்."
    override val waitingForReply = "பதிலுக்காக காத்திருக்கிறது"
    override val errConfirmUnion = "திருமணத்தை உறுதிப்படுத்த முடியவில்லை."
    override val errUpdateGalleryOptin = "கேலரி ஒப்புதலைப் புதுப்பிக்க முடியவில்லை."
    override val errSendSeriousModeRequest = "அந்த சீரியஸ் மோட் கோரிக்கையை அனுப்ப முடியவில்லை."
    override val errRevokeSeriousMode = "சீரியஸ் மோடை ரத்து செய்ய முடியவில்லை."
    override val errUnmatch = "பொருத்தத்தை நீக்க முடியவில்லை."
    override val errAcceptSeriousModeRequest = "அந்த சீரியஸ் மோட் கோரிக்கையை ஏற்க முடியவில்லை."
    override val errDeclineSeriousModeRequest = "அந்த சீரியஸ் மோட் கோரிக்கையை நிராகரிக்க முடியவில்லை."
    override val errRefreshMatches = "உங்கள் மேட்சஸை புதுப்பிக்க முடியவில்லை."
    override val errLoadMatches = "உங்கள் மேட்சஸை ஏற்ற முடியவில்லை."
    override val errLoadReceivedRequests = "பெறப்பட்ட கோரிக்கைகளை ஏற்ற முடியவில்லை."
    override val errLoadSentRequests = "அனுப்பப்பட்ட கோரிக்கைகளை ஏற்ற முடியவில்லை."
    override fun waitingOnThemToConfirm(name: String) = "$name உங்கள் திருமணத்தை உறுதிப்படுத்த காத்திருக்கிறது."
    override fun themWantsToConfirm(name: String) = "$name திருமணத்தை உறுதிப்படுத்த விரும்புகிறார்."
    override val confirmUnion = "திருமணத்தை உறுதிப்படுத்து"
    override val unionConfirmedTitle = "திருமணம் உறுதிசெய்யப்பட்டது 🎉"
    override val unionConfirmedBody = "Success Gallery-இல் உங்கள் கதையைப் பகிரவா? அநாமதேயமானது (5-வருட வயது குழுக்கள் மட்டும்) -- இரு தரப்பினரும் ஒப்புக்கொள்ள வேண்டும், எப்போது வேண்டுமானாலும் திரும்பப் பெறலாம்."
    override val optInSuccessGallery = "Success Gallery-இல் சேரவும்"
    override val viewSuccessGallery = "Success Gallery-ஐப் பார்க்கவும்"
    override val messageDeleted = "இந்தச் செய்தி நீக்கப்பட்டது"
    override val typing = "தட்டச்சு செய்கிறார்…"
    override val online = "ஆன்லைனில்"
    override fun seriousModePendingSuffix(base: String) = "$base · சீரியஸ் மோட் நிலுவையில்"
    override val backToMatches = "Matches-க்குத் திரும்பு"
    override val endToEndEncrypted = "எண்டு-டு-எண்டு என்க்ரிப்ட் செய்யப்பட்டது"
    override val audioCall = "ஆடியோ அழைப்பு"
    override val videoCall = "வீடியோ அழைப்பு"
    override val moreOptions = "மேலும் விருப்பங்கள்"
    override val acceptSeriousMode = "சீரியஸ் மோடை ஏற்கவும்"
    override val declineSeriousMode = "சீரியஸ் மோடை நிராகரிக்கவும்"
    override val goSerious = "சீரியஸ் மோட் தொடங்கு"
    override val revokeSeriousMode = "சீரியஸ் மோடை ரத்து செய்"
    override val unmatch = "பொருத்தத்தை நீக்கு"
    override val statusMatched = "பொருந்தியது"
    override val statusSeriousExploration = "சீரியஸ் ஆய்வு"
    override val statusSunsetting = "முடிவடைகிறது"
    override val statusClosed = "மூடப்பட்டது"
}
