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
    val groupToday: String
    val groupYesterday: String
    val markAllError: String
    val unreadLabel: String
    val markRead: String
    val browseSuggestions: String
    val browseProfiles: String
    val justNow: String
    /** "{n}" is replaced with the number. */
    val minutesAgo: String
    val hoursAgo: String
    val previewCandidate: String
    val previewRequestFrom: String
    val previewThread: String
    val previewProspect: String
    val previewViewerNamed: String
    val previewViewerAnonymous: String
    val previewMutualSponsorInterest: String
    val previewShortlistedProfileUpdated: String
    val ctaViewMatch: String
    val ctaViewRequest: String
    val ctaRespond: String
    val ctaViewConversation: String
    val ctaViewDetails: String
    val ctaViewSuggestion: String
    val ctaViewTheirProfile: String
    val ctaViewShareLinks: String
    val ctaViewProfile: String
}

object NotificationsStringsEn : NotificationsStrings {
    override val pageTitle = "Notifications"
    override val markAllRead = "Mark all read"
    override val marking = "Marking…"
    override val loading = "Loading…"
    override val emptyTitle = "Nothing to catch up on"
    override val emptyDescription = "Suggestions, match requests and consent decisions land here. We'll also send a push if you've turned those on in Settings."
    override val loadError = "Couldn't load notifications."
    override val typeNewSuggestion = "New suggestion"
    override val typeMutualMatch = "Mutual match"
    override val typeSeriousModeRequest = "Serious Mode request"
    override val typeSeriousModeAccepted = "Serious Mode accepted"
    override val typeSeriousModeRevoked = "Serious Mode ended"
    override val typeConsentRequest = "Consent request"
    override val typeConsentGranted = "Consent granted"
    override val typeRequestReceived = "New request"
    override val typeShareLinkViewed = "Your shared profile was viewed"
    override val typeMutualSponsorInterest = "Mutual interest"
    override val typeShortlistedProfileUpdated = "Shortlist update"
    override val groupToday = "Today"
    override val groupYesterday = "Yesterday"
    override val markAllError = "Couldn't mark everything as read."
    override val unreadLabel = "Unread"
    override val markRead = "Mark read"
    override val browseSuggestions = "Browse suggestions"
    override val browseProfiles = "Browse profiles"
    override val justNow = "Just now"
    override val minutesAgo = "{n}m ago"
    override val hoursAgo = "{n}h ago"
    override val previewCandidate = "Candidate {id}…"
    override val previewRequestFrom = "From {id}… -- see Requests"
    override val previewThread = "Thread {id}…"
    override val previewProspect = "Prospect {id}…"
    override val previewViewerNamed = "{name} opened one of your profile share links."
    override val previewViewerAnonymous = "Someone opened one of your profile share links for the first time."
    override val previewMutualSponsorInterest = "You've now both been suggested to each other."
    override val previewShortlistedProfileUpdated = "Someone on your shortlist updated their profile."
    override val ctaViewMatch = "View match"
    override val ctaViewRequest = "View request"
    override val ctaRespond = "Respond"
    override val ctaViewConversation = "View conversation"
    override val ctaViewDetails = "View details"
    override val ctaViewSuggestion = "View suggestion"
    override val ctaViewTheirProfile = "View their profile"
    override val ctaViewShareLinks = "View share links"
    override val ctaViewProfile = "View profile"
}

object NotificationsStringsHi : NotificationsStrings {
    override val pageTitle = "सूचनाएं"
    override val markAllRead = "सभी को पढ़ा हुआ चिह्नित करें"
    override val marking = "चिह्नित किया जा रहा है…"
    override val loading = "लोड हो रहा है…"
    override val emptyTitle = "देखने के लिए कुछ नया नहीं"
    override val emptyDescription = "सुझाव, मैच अनुरोध और सहमति निर्णय यहां दिखेंगे। यदि आपने सेटिंग्स में चालू किया है, तो हम पुश सूचना भी भेजेंगे।"
    override val loadError = "सूचनाएं लोड नहीं हो सकीं।"
    override val typeNewSuggestion = "नया सुझाव"
    override val typeMutualMatch = "आपसी मैच"
    override val typeSeriousModeRequest = "सीरियस मोड अनुरोध"
    override val typeSeriousModeAccepted = "सीरियस मोड स्वीकृत"
    override val typeSeriousModeRevoked = "सीरियस मोड समाप्त"
    override val typeConsentRequest = "सहमति अनुरोध"
    override val typeConsentGranted = "सहमति दी गई"
    override val typeRequestReceived = "नया अनुरोध"
    override val typeShareLinkViewed = "आपकी शेयर की गई प्रोफ़ाइल देखी गई"
    override val typeMutualSponsorInterest = "आपसी रुचि"
    override val typeShortlistedProfileUpdated = "शॉर्टलिस्ट अपडेट"
    override val groupToday = "आज"
    override val groupYesterday = "कल"
    override val markAllError = "सभी को पढ़ा हुआ चिह्नित नहीं किया जा सका।"
    override val unreadLabel = "अपठित"
    override val markRead = "पढ़ा हुआ चिह्नित करें"
    override val browseSuggestions = "सुझाव ब्राउज़ करें"
    override val browseProfiles = "प्रोफ़ाइलें ब्राउज़ करें"
    override val justNow = "अभी"
    override val minutesAgo = "{n} मिनट पहले"
    override val hoursAgo = "{n} घंटे पहले"
    override val previewCandidate = "उम्मीदवार {id}…"
    override val previewRequestFrom = "{id}… से -- अनुरोध देखें"
    override val previewThread = "थ्रेड {id}…"
    override val previewProspect = "संभावित उम्मीदवार {id}…"
    override val previewViewerNamed = "{name} ने आपकी प्रोफ़ाइल का एक शेयर लिंक खोला।"
    override val previewViewerAnonymous = "किसी ने पहली बार आपकी प्रोफ़ाइल का एक शेयर लिंक खोला।"
    override val previewMutualSponsorInterest = "अब आप दोनों एक-दूसरे को सुझाए गए हैं।"
    override val previewShortlistedProfileUpdated = "आपकी शॉर्टलिस्ट में किसी ने अपनी प्रोफ़ाइल अपडेट की।"
    override val ctaViewMatch = "मैच देखें"
    override val ctaViewRequest = "अनुरोध देखें"
    override val ctaRespond = "जवाब दें"
    override val ctaViewConversation = "बातचीत देखें"
    override val ctaViewDetails = "विवरण देखें"
    override val ctaViewSuggestion = "सुझाव देखें"
    override val ctaViewTheirProfile = "उनकी प्रोफ़ाइल देखें"
    override val ctaViewShareLinks = "शेयर लिंक देखें"
    override val ctaViewProfile = "प्रोफ़ाइल देखें"
}

object NotificationsStringsTa : NotificationsStrings {
    override val pageTitle = "அறிவிப்புகள்"
    override val markAllRead = "அனைத்தையும் படித்ததாகக் குறிக்கவும்"
    override val marking = "குறிக்கப்படுகிறது…"
    override val loading = "ஏற்றப்படுகிறது…"
    override val emptyTitle = "பார்க்க புதிதாக எதுவும் இல்லை"
    override val emptyDescription = "பரிந்துரைகள், பொருத்த கோரிக்கைகள் மற்றும் ஒப்புதல் முடிவுகள் இங்கே வரும். அமைப்புகளில் இயக்கியிருந்தால் நாங்கள் புஷ் அறிவிப்பும் அனுப்புவோம்."
    override val loadError = "அறிவிப்புகளை ஏற்ற முடியவில்லை."
    override val typeNewSuggestion = "புதிய பரிந்துரை"
    override val typeMutualMatch = "பரஸ்பர பொருத்தம்"
    override val typeSeriousModeRequest = "சீரியஸ் மோட் கோரிக்கை"
    override val typeSeriousModeAccepted = "சீரியஸ் மோட் ஏற்கப்பட்டது"
    override val typeSeriousModeRevoked = "சீரியஸ் மோட் முடிந்தது"
    override val typeConsentRequest = "ஒப்புதல் கோரிக்கை"
    override val typeConsentGranted = "ஒப்புதல் வழங்கப்பட்டது"
    override val typeRequestReceived = "புதிய கோரிக்கை"
    override val typeShareLinkViewed = "உங்கள் பகிரப்பட்ட சுயவிவரம் பார்க்கப்பட்டது"
    override val typeMutualSponsorInterest = "பரஸ்பர ஆர்வம்"
    override val typeShortlistedProfileUpdated = "ஷார்ட்லிஸ்ட் புதுப்பிப்பு"
    override val groupToday = "இன்று"
    override val groupYesterday = "நேற்று"
    override val markAllError = "அனைத்தையும் படித்ததாகக் குறிக்க முடியவில்லை."
    override val unreadLabel = "படிக்காதது"
    override val markRead = "படித்ததாகக் குறிக்கவும்"
    override val browseSuggestions = "பரிந்துரைகளை உலாவவும்"
    override val browseProfiles = "சுயவிவரங்களை உலாவவும்"
    override val justNow = "இப்போது"
    override val minutesAgo = "{n} நிமிடங்களுக்கு முன்"
    override val hoursAgo = "{n} மணிநேரத்துக்கு முன்"
    override val previewCandidate = "வேட்பாளர் {id}…"
    override val previewRequestFrom = "{id}… இலிருந்து -- கோரிக்கைகளைப் பார்க்கவும்"
    override val previewThread = "தொடர் {id}…"
    override val previewProspect = "வாய்ப்புள்ளவர் {id}…"
    override val previewViewerNamed = "{name} உங்கள் சுயவிவரத்தின் ஒரு பகிர்வு இணைப்பைத் திறந்தார்."
    override val previewViewerAnonymous = "யாரோ முதல் முறையாக உங்கள் சுயவிவரத்தின் ஒரு பகிர்வு இணைப்பைத் திறந்தனர்."
    override val previewMutualSponsorInterest = "இப்போது நீங்கள் இருவரும் ஒருவருக்கொருவர் பரிந்துரைக்கப்பட்டுள்ளீர்கள்."
    override val previewShortlistedProfileUpdated = "உங்கள் ஷார்ட்லிஸ்ட்டில் உள்ள ஒருவர் தங்கள் சுயவிவரத்தைப் புதுப்பித்தார்."
    override val ctaViewMatch = "பொருத்தத்தைப் பார்க்கவும்"
    override val ctaViewRequest = "கோரிக்கையைப் பார்க்கவும்"
    override val ctaRespond = "பதிலளிக்கவும்"
    override val ctaViewConversation = "உரையாடலைப் பார்க்கவும்"
    override val ctaViewDetails = "விவரங்களைப் பார்க்கவும்"
    override val ctaViewSuggestion = "பரிந்துரையைப் பார்க்கவும்"
    override val ctaViewTheirProfile = "அவர்களின் சுயவிவரத்தைப் பார்க்கவும்"
    override val ctaViewShareLinks = "பகிர்வு இணைப்புகளைப் பார்க்கவும்"
    override val ctaViewProfile = "சுயவிவரத்தைப் பார்க்கவும்"
}
