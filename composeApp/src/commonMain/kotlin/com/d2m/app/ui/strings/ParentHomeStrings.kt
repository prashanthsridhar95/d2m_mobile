package com.d2m.app.ui.strings

/** ParentHomeScreen.kt -- mirrors d2m_web's "home_parent" namespace where concepts overlap. */
interface ParentHomeStrings {
    val errLoadDashboard: String
    val pageTitle: String
    val yourChild: String
    val profileComplete: String
    val profileIncomplete: String
    val suggestedForYouToReview: String
    fun suggestionsCount(count: Int): String
    val shortlisted: String

    // "Request Access" consent card -- mirrors d2m_web's DiscoverScreen.jsx
    // ConsentCard (see ParentHomeScreen.kt's own ConsentProspectCard).
    val consentSeriousModeHeading: String
    val consentSeriousModeBody: String
    val consentHiddenUntilAccess: String
    val consentRequestAccess: String
    val consentPendingBadge: String
    val consentWaitingOnDecision: String
    val consentSeriousWithPerson: String
    val consentFullAccessBody: String
    val consentViewFullProfile: String
    val consentMessageTheirParent: String
    val consentOpening: String
    val consentCouldntLoad: String
    val consentCouldntSendRequest: String
    val consentErrStartConversation: String
    val consentNoSeriousExplorationTitle: String
    val consentNoSeriousExplorationDesc: String

    // Parent home redesign: next-best-action card, carousel, disclosure rows.
    fun nextReviewTitle(count: Int): String
    val nextReviewBody: String
    val nextProfileTitle: String
    val nextProfileBody: String
    val nextMessageTitle: String
    val nextMessageBody: String
    val allCaughtUpTitle: String
    val allCaughtUpBody: String
    val topPicks: String
    val noTopPicks: String
    fun shortlistedCount(count: Int): String
    val shortlistEmpty: String
    val seriousModeRow: String
    val activityRow: String
    val statSuggestionsSent: String
    val statUnreadAlerts: String
    val statPendingRequests: String
    val statProfile: String
    val refreshFailed: String

    val waitingOnChildTitle: String
    val waitingOnChildBody: String
}

object ParentHomeStringsEn : ParentHomeStrings {
    override val errLoadDashboard = "Couldn't load your dashboard."
    override val pageTitle = "Your child's matches"
    override val yourChild = "Your child"
    override val profileComplete = "Profile complete"
    override val profileIncomplete = "Profile incomplete"
    override val suggestedForYouToReview = "Suggested for you to review"
    override fun suggestionsCount(count: Int) = "$count suggestions"
    override val shortlisted = "Shortlisted"

    override val consentSeriousModeHeading = "Serious mode"
    override val consentSeriousModeBody = "Your child is in a serious exploration. Request them for more details."
    override val consentHiddenUntilAccess = "Their photo, contact info, and full profile stay hidden until they grant you access."
    override val consentRequestAccess = "Request Access"
    override val consentPendingBadge = "PENDING"
    override val consentWaitingOnDecision = "Waiting on their decision"
    override val consentSeriousWithPerson = "Your child is serious with this person"
    override val consentFullAccessBody = "You now have full access to their profile -- take a look, then reach out when you're ready."
    override val consentViewFullProfile = "View full profile"
    override val consentMessageTheirParent = "Message their parent"
    override val consentOpening = "Opening…"
    override val consentCouldntLoad = "Couldn't load this."
    override val consentCouldntSendRequest = "Couldn't send the request."
    override val consentErrStartConversation = "Couldn't start that conversation."
    override val consentNoSeriousExplorationTitle = "No serious exploration yet"
    override val consentNoSeriousExplorationDesc = "Once your child goes Serious with a match, you'll be able to request that match's full details here."

    override val waitingOnChildTitle = "Waiting on your child"
    override val waitingOnChildBody = "Once they claim your invite, their matches and activity will show up here."

    override fun nextReviewTitle(count: Int) = if (count == 1) "Review 1 new suggestion" else "Review $count new suggestions"
    override val nextReviewBody = "Start with the best match for your child"
    override val nextProfileTitle = "Finish your child's profile"
    override val nextProfileBody = "A complete profile gets better matches"
    override val nextMessageTitle = "Continue the conversation"
    override val nextMessageBody = "Their family has full access to talk with you"
    override val allCaughtUpTitle = "You're all caught up"
    override val allCaughtUpBody = "New suggestions will appear here as they are found"
    override val topPicks = "Top picks"
    override val noTopPicks = "No suggestions yet"
    override fun shortlistedCount(count: Int) = "Shortlisted · $count"
    override val shortlistEmpty = "Tap the heart on any profile to shortlist it."
    override val seriousModeRow = "Serious mode"
    override val activityRow = "Activity"
    override val statSuggestionsSent = "Suggestions sent"
    override val statUnreadAlerts = "Unread alerts"
    override val statPendingRequests = "Pending requests"
    override val statProfile = "Profile"
    override val refreshFailed = "Couldn't refresh. Pull down to try again."
}

object ParentHomeStringsHi : ParentHomeStrings {
    override val errLoadDashboard = "आपका डैशबोर्ड लोड नहीं हो सका।"
    override val pageTitle = "आपके बच्चे के मैच"
    override val yourChild = "आपका बच्चा"
    override val profileComplete = "प्रोफ़ाइल पूरी है"
    override val profileIncomplete = "प्रोफ़ाइल अधूरी है"
    override val suggestedForYouToReview = "आपके देखने के लिए सुझाव"
    override fun suggestionsCount(count: Int) = "$count सुझाव"
    override val shortlisted = "शॉर्टलिस्ट की गई"

    override val consentSeriousModeHeading = "सीरियस मोड"
    override val consentSeriousModeBody = "आपका बच्चा एक सीरियस एक्सप्लोरेशन में है। अधिक जानकारी के लिए उनसे अनुरोध करें।"
    override val consentHiddenUntilAccess = "उनकी फ़ोटो, संपर्क जानकारी, और पूरी प्रोफ़ाइल तब तक छिपी रहेगी जब तक वे आपको एक्सेस नहीं देते।"
    override val consentRequestAccess = "एक्सेस का अनुरोध करें"
    override val consentPendingBadge = "लंबित"
    override val consentWaitingOnDecision = "उनके निर्णय की प्रतीक्षा में"
    override val consentSeriousWithPerson = "आपका बच्चा इस व्यक्ति के साथ सीरियस है"
    override val consentFullAccessBody = "अब आपको उनकी प्रोफ़ाइल तक पूरी पहुंच है -- देखें, फिर तैयार होने पर संपर्क करें।"
    override val consentViewFullProfile = "पूरी प्रोफ़ाइल देखें"
    override val consentMessageTheirParent = "उनके माता-पिता को मैसेज करें"
    override val consentOpening = "खोला जा रहा है…"
    override val consentCouldntLoad = "यह लोड नहीं हो सका।"
    override val consentCouldntSendRequest = "अनुरोध नहीं भेजा जा सका।"
    override val consentErrStartConversation = "वह बातचीत शुरू नहीं हो सकी।"
    override val consentNoSeriousExplorationTitle = "अभी तक कोई सीरियस एक्सप्लोरेशन नहीं"
    override val consentNoSeriousExplorationDesc = "जैसे ही आपका बच्चा किसी मैच के साथ सीरियस होगा, आप यहां उस मैच का पूरा विवरण मांग सकेंगे।"

    override val waitingOnChildTitle = "आपके बच्चे की प्रतीक्षा है"
    override val waitingOnChildBody = "एक बार वे आपका इनवाइट स्वीकार कर लें, तो उनके मैच और गतिविधि यहां दिखाई देंगे।"

    override fun nextReviewTitle(count: Int) = "$count नए सुझाव देखें"
    override val nextReviewBody = "अपने बच्चे के लिए सबसे अच्छे मैच से शुरू करें"
    override val nextProfileTitle = "अपने बच्चे की प्रोफ़ाइल पूरी करें"
    override val nextProfileBody = "पूरी प्रोफ़ाइल से बेहतर मैच मिलते हैं"
    override val nextMessageTitle = "बातचीत जारी रखें"
    override val nextMessageBody = "उनके परिवार को आपसे बात करने की पूरी पहुंच है"
    override val allCaughtUpTitle = "सब कुछ देख लिया गया है"
    override val allCaughtUpBody = "नए सुझाव मिलते ही यहां दिखेंगे"
    override val topPicks = "शीर्ष चयन"
    override val noTopPicks = "अभी कोई सुझाव नहीं"
    override fun shortlistedCount(count: Int) = "शॉर्टलिस्ट · $count"
    override val shortlistEmpty = "किसी भी प्रोफ़ाइल पर दिल दबाकर उसे शॉर्टलिस्ट करें।"
    override val seriousModeRow = "सीरियस मोड"
    override val activityRow = "गतिविधि"
    override val statSuggestionsSent = "भेजे गए सुझाव"
    override val statUnreadAlerts = "अपठित सूचनाएं"
    override val statPendingRequests = "लंबित अनुरोध"
    override val statProfile = "प्रोफ़ाइल"
    override val refreshFailed = "रिफ्रेश नहीं हो सका। दोबारा नीचे खींचें।"
}

object ParentHomeStringsTa : ParentHomeStrings {
    override val errLoadDashboard = "உங்கள் டாஷ்போர்டை ஏற்ற முடியவில்லை."
    override val pageTitle = "உங்கள் குழந்தையின் மேட்சஸ்"
    override val yourChild = "உங்கள் குழந்தை"
    override val profileComplete = "சுயவிவரம் முழுமையானது"
    override val profileIncomplete = "சுயவிவரம் முழுமையடையவில்லை"
    override val suggestedForYouToReview = "நீங்கள் பார்வையிட பரிந்துரைக்கப்பட்டவை"
    override fun suggestionsCount(count: Int) = "$count பரிந்துரைகள்"
    override val shortlisted = "ஷார்ட்லிஸ்ட் செய்யப்பட்டவை"

    override val consentSeriousModeHeading = "சீரியஸ் மோட்"
    override val consentSeriousModeBody = "உங்கள் குழந்தை ஒரு சீரியஸ் ஆராய்ச்சியில் உள்ளது. மேலும் விவரங்களுக்கு அவர்களிடம் கோரவும்."
    override val consentHiddenUntilAccess = "அவர்கள் உங்களுக்கு அனுமதி அளிக்கும் வரை அவர்களின் புகைப்படம், தொடர்பு தகவல் மற்றும் முழு சுயவிவரமும் மறைந்திருக்கும்."
    override val consentRequestAccess = "அனுமதி கோரவும்"
    override val consentPendingBadge = "நிலுவையில்"
    override val consentWaitingOnDecision = "அவர்களின் முடிவுக்காக காத்திருக்கிறோம்"
    override val consentSeriousWithPerson = "உங்கள் குழந்தை இந்த நபருடன் சீரியஸாக உள்ளது"
    override val consentFullAccessBody = "இப்போது உங்களுக்கு அவர்களின் சுயவிவரத்திற்கு முழு அணுகல் உள்ளது -- பாருங்கள், பின்னர் தயாரானதும் தொடர்பு கொள்ளுங்கள்."
    override val consentViewFullProfile = "முழு சுயவிவரத்தைக் காண்க"
    override val consentMessageTheirParent = "அவர்களின் பெற்றோருக்கு மெசேஜ் அனுப்பவும்"
    override val consentOpening = "திறக்கப்படுகிறது…"
    override val consentCouldntLoad = "இதை ஏற்ற முடியவில்லை."
    override val consentCouldntSendRequest = "கோரிக்கையை அனுப்ப முடியவில்லை."
    override val consentErrStartConversation = "அந்த உரையாடலைத் தொடங்க முடியவில்லை."
    override val consentNoSeriousExplorationTitle = "இதுவரை சீரியஸ் ஆராய்ச்சி இல்லை"
    override val consentNoSeriousExplorationDesc = "உங்கள் குழந்தை ஒரு பொருத்தத்துடன் சீரியஸாகும்போது, அந்த பொருத்தத்தின் முழு விவரங்களையும் இங்கே கோரலாம்."

    override val waitingOnChildTitle = "உங்கள் குழந்தைக்காக காத்திருக்கிறோம்"
    override val waitingOnChildBody = "அவர்கள் உங்கள் அழைப்பை ஏற்றவுடன், அவர்களின் பொருத்தங்களும் செயல்பாடும் இங்கே தெரியும்."

    override fun nextReviewTitle(count: Int) = "$count புதிய பரிந்துரைகளைப் பார்க்கவும்"
    override val nextReviewBody = "உங்கள் குழந்தைக்கான சிறந்த பொருத்தத்திலிருந்து தொடங்குங்கள்"
    override val nextProfileTitle = "உங்கள் குழந்தையின் சுயவிவரத்தை முடிக்கவும்"
    override val nextProfileBody = "முழுமையான சுயவிவரம் சிறந்த பொருத்தங்களைத் தரும்"
    override val nextMessageTitle = "உரையாடலைத் தொடருங்கள்"
    override val nextMessageBody = "அவர்களின் குடும்பத்திற்கு உங்களுடன் பேச முழு அணுகல் உள்ளது"
    override val allCaughtUpTitle = "அனைத்தும் பார்க்கப்பட்டன"
    override val allCaughtUpBody = "புதிய பரிந்துரைகள் கிடைத்ததும் இங்கே தோன்றும்"
    override val topPicks = "சிறந்த தேர்வுகள்"
    override val noTopPicks = "இன்னும் பரிந்துரைகள் இல்லை"
    override fun shortlistedCount(count: Int) = "ஷார்ட்லிஸ்ட் · $count"
    override val shortlistEmpty = "எந்த சுயவிவரத்திலும் இதயத்தைத் தட்டி ஷார்ட்லிஸ்ட் செய்யுங்கள்."
    override val seriousModeRow = "சீரியஸ் மோட்"
    override val activityRow = "செயல்பாடு"
    override val statSuggestionsSent = "அனுப்பிய பரிந்துரைகள்"
    override val statUnreadAlerts = "படிக்காத அறிவிப்புகள்"
    override val statPendingRequests = "நிலுவையில் உள்ள கோரிக்கைகள்"
    override val statProfile = "சுயவிவரம்"
    override val refreshFailed = "புதுப்பிக்க முடியவில்லை. மீண்டும் கீழே இழுக்கவும்."
}
