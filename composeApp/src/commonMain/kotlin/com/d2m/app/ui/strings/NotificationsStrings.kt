package com.d2m.app.ui.strings

/** NotificationsScreen.kt -- mirrors d2m_web's "notifications" namespace. */
interface NotificationsStrings {
    val pageTitle: String
    val markAllRead: String
    val marking: String
    val loading: String
    val emptyTitle: String
    val emptyDescription: String
    val loadError: String
    val typeNewSuggestion: String
    val typeMutualMatch: String
    val typeSeriousModeRequest: String
    val typeSeriousModeAccepted: String
    val typeSeriousModeRevoked: String
    val typeConsentRequest: String
    val typeConsentGranted: String
    val typeRequestReceived: String
    val typeShareLinkViewed: String
    val typeMutualSponsorInterest: String
    val typeShortlistedProfileUpdated: String
}

object NotificationsStringsEn : NotificationsStrings {
    override val pageTitle = "Notifications"
    override val markAllRead = "Mark all read"
    override val marking = "Marking…"
    override val loading = "Loading…"
    override val emptyTitle = "Nothing to catch up on"
    override val emptyDescription = "New suggestions, requests, and match updates will show up here."
    override val loadError = "Couldn't load notifications."
    override val typeNewSuggestion = "New suggestion in your feed"
    override val typeMutualMatch = "You have a new match"
    override val typeSeriousModeRequest = "Someone wants to go Serious"
    override val typeSeriousModeAccepted = "Serious Mode confirmed"
    override val typeSeriousModeRevoked = "Serious Mode was revoked"
    override val typeConsentRequest = "A parent requested access to your details"
    override val typeConsentGranted = "Access request granted"
    override val typeRequestReceived = "You received a new request"
    override val typeShareLinkViewed = "Your shared profile was viewed"
    override val typeMutualSponsorInterest = "You've now both been suggested to each other"
    override val typeShortlistedProfileUpdated = "Someone on your shortlist updated their profile"
}

object NotificationsStringsHi : NotificationsStrings {
    override val pageTitle = "सूचनाएं"
    override val markAllRead = "सभी को पढ़ा हुआ चिह्नित करें"
    override val marking = "चिह्नित किया जा रहा है…"
    override val loading = "लोड हो रहा है…"
    override val emptyTitle = "देखने के लिए कुछ नया नहीं"
    override val emptyDescription = "नए सुझाव, अनुरोध और मैच अपडेट यहां दिखेंगे।"
    override val loadError = "सूचनाएं लोड नहीं हो सकीं।"
    override val typeNewSuggestion = "आपके फ़ीड में नया सुझाव"
    override val typeMutualMatch = "आपका एक नया मैच है"
    override val typeSeriousModeRequest = "कोई सीरियस मोड में जाना चाहता है"
    override val typeSeriousModeAccepted = "सीरियस मोड की पुष्टि हुई"
    override val typeSeriousModeRevoked = "सीरियस मोड समाप्त कर दिया गया"
    override val typeConsentRequest = "एक पैरेंट ने आपके विवरण तक पहुंच का अनुरोध किया"
    override val typeConsentGranted = "एक्सेस अनुरोध स्वीकृत"
    override val typeRequestReceived = "आपको एक नया अनुरोध मिला"
    override val typeShareLinkViewed = "आपकी शेयर की गई प्रोफ़ाइल देखी गई"
    override val typeMutualSponsorInterest = "अब आप दोनों एक-दूसरे को सुझाए गए हैं"
    override val typeShortlistedProfileUpdated = "आपकी शॉर्टलिस्ट में किसी ने अपनी प्रोफ़ाइल अपडेट की"
}

object NotificationsStringsTa : NotificationsStrings {
    override val pageTitle = "அறிவிப்புகள்"
    override val markAllRead = "அனைத்தையும் படித்ததாகக் குறிக்கவும்"
    override val marking = "குறிக்கப்படுகிறது…"
    override val loading = "ஏற்றப்படுகிறது…"
    override val emptyTitle = "பார்க்க புதிதாக எதுவும் இல்லை"
    override val emptyDescription = "புதிய பரிந்துரைகள், கோரிக்கைகள் மற்றும் பொருத்த புதுப்பிப்புகள் இங்கே தோன்றும்."
    override val loadError = "அறிவிப்புகளை ஏற்ற முடியவில்லை."
    override val typeNewSuggestion = "உங்கள் ஃபீட்டில் புதிய பரிந்துரை"
    override val typeMutualMatch = "உங்களுக்கு ஒரு புதிய பொருத்தம் உள்ளது"
    override val typeSeriousModeRequest = "ஒருவர் சீரியஸ் மோடுக்குச் செல்ல விரும்புகிறார்"
    override val typeSeriousModeAccepted = "சீரியஸ் மோட் உறுதிசெய்யப்பட்டது"
    override val typeSeriousModeRevoked = "சீரியஸ் மோட் முடிவுக்கு வந்தது"
    override val typeConsentRequest = "ஒரு பெற்றோர் உங்கள் விவரங்களுக்கான அணுகலைக் கோரினார்"
    override val typeConsentGranted = "அணுகல் கோரிக்கை அங்கீகரிக்கப்பட்டது"
    override val typeRequestReceived = "உங்களுக்கு ஒரு புதிய கோரிக்கை வந்துள்ளது"
    override val typeShareLinkViewed = "உங்கள் பகிரப்பட்ட சுயவிவரம் பார்க்கப்பட்டது"
    override val typeMutualSponsorInterest = "இப்போது நீங்கள் இருவரும் ஒருவருக்கொருவர் பரிந்துரைக்கப்பட்டுள்ளீர்கள்"
    override val typeShortlistedProfileUpdated = "உங்கள் ஷார்ட்லிஸ்ட்டில் உள்ள ஒருவர் தங்கள் சுயவிவரத்தைப் புதுப்பித்தார்"
}
