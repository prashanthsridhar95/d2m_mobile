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
    /** type -> [title, body with {name}, body without a name]; the types web added copy for after the original set. */
    val extraTypes: Map<String, List<String>>
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
    override val previewCandidate = "Tap to view the profile."
    override val previewRequestFrom = "Someone sent you a request -- see Requests."
    override val previewThread = "Open your match to see the details."
    override val previewProspect = "Open to see the details."
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
    override val extraTypes = mapOf(
        "calls_unlocked" to listOf("Calling unlocked", "You and {name} have both confirmed you met -- calling is now open.", "You've both confirmed you met -- calling is now open."),
        "horoscope_signoff_complete" to listOf("Horoscope signed off", "You and {name} have both signed off on the horoscope match.", "You've both signed off on the horoscope match."),
        "family_meeting_proposed" to listOf("Meeting proposed", "{name} proposed a time to meet.", "A meeting time has been proposed."),
        "family_meeting_confirmed" to listOf("Meeting confirmed", "{name} confirmed your proposed meeting time.", "Your proposed meeting time was confirmed."),
        "family_meeting_declined" to listOf("Meeting declined", "{name} declined your proposed meeting time.", "Your proposed meeting time was declined."),
        "unmatched" to listOf("A match ended", "Your match with {name} has ended.", "One of your matches has ended."),
        "vouch_pending_approval" to listOf("Vouch awaiting approval", "A vouch is waiting for your approval.", "A vouch is waiting for your approval."),
    )
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
    override val previewCandidate = "प्रोफ़ाइल देखने के लिए टैप करें।"
    override val previewRequestFrom = "किसी ने आपको अनुरोध भेजा है -- अनुरोध देखें।"
    override val previewThread = "विवरण देखने के लिए अपना मैच खोलें।"
    override val previewProspect = "विवरण देखने के लिए खोलें।"
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
    override val extraTypes = mapOf(
        "calls_unlocked" to listOf("कॉलिंग खुल गई", "आप और {name} दोनों ने मिलने की पुष्टि की -- अब कॉलिंग खुली है।", "आप दोनों ने मिलने की पुष्टि की -- अब कॉलिंग खुली है।"),
        "horoscope_signoff_complete" to listOf("कुंडली पर सहमति", "आप और {name} दोनों ने कुंडली मिलान पर सहमति दी है।", "आप दोनों ने कुंडली मिलान पर सहमति दी है।"),
        "family_meeting_proposed" to listOf("मुलाक़ात का प्रस्ताव", "{name} ने मिलने का समय सुझाया।", "मिलने का एक समय सुझाया गया है।"),
        "family_meeting_confirmed" to listOf("मुलाक़ात की पुष्टि", "{name} ने आपके सुझाए समय की पुष्टि की।", "आपके सुझाए मिलने के समय की पुष्टि हो गई।"),
        "family_meeting_declined" to listOf("मुलाक़ात अस्वीकृत", "{name} ने आपका सुझाया समय अस्वीकार किया।", "आपका सुझाया मिलने का समय अस्वीकार किया गया।"),
        "unmatched" to listOf("मैच समाप्त", "{name} के साथ आपका मैच समाप्त हो गया है।", "आपका एक मैच समाप्त हो गया है।"),
        "vouch_pending_approval" to listOf("वाउच की स्वीकृति लंबित", "एक वाउच आपकी स्वीकृति की प्रतीक्षा में है।", "एक वाउच आपकी स्वीकृति की प्रतीक्षा में है।"),
    )
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
    override val previewCandidate = "சுயவிவரத்தைப் பார்க்கத் தட்டவும்."
    override val previewRequestFrom = "யாரோ உங்களுக்குக் கோரிக்கை அனுப்பியுள்ளனர் -- கோரிக்கைகளைப் பார்க்கவும்."
    override val previewThread = "விவரங்களைப் பார்க்க உங்கள் பொருத்தத்தைத் திறக்கவும்."
    override val previewProspect = "விவரங்களைப் பார்க்கத் திறக்கவும்."
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
    override val extraTypes = mapOf(
        "calls_unlocked" to listOf("அழைப்பு திறக்கப்பட்டது", "நீங்களும் {name}-உம் சந்தித்ததை உறுதிசெய்தீர்கள் -- அழைப்பு இப்போது திறந்துள்ளது.", "நீங்கள் இருவரும் சந்தித்ததை உறுதிசெய்தீர்கள் -- அழைப்பு இப்போது திறந்துள்ளது."),
        "horoscope_signoff_complete" to listOf("ஜாதகம் ஒப்புதல்", "நீங்களும் {name}-உம் ஜாதகப் பொருத்தத்தை ஒப்புக்கொண்டீர்கள்.", "நீங்கள் இருவரும் ஜாதகப் பொருத்தத்தை ஒப்புக்கொண்டீர்கள்."),
        "family_meeting_proposed" to listOf("சந்திப்பு முன்மொழிவு", "{name} சந்திக்க ஒரு நேரத்தை முன்மொழிந்தார்.", "சந்திக்க ஒரு நேரம் முன்மொழியப்பட்டுள்ளது."),
        "family_meeting_confirmed" to listOf("சந்திப்பு உறுதி", "{name} நீங்கள் முன்மொழிந்த நேரத்தை உறுதிசெய்தார்.", "நீங்கள் முன்மொழிந்த சந்திப்பு நேரம் உறுதிசெய்யப்பட்டது."),
        "family_meeting_declined" to listOf("சந்திப்பு நிராகரிப்பு", "{name} நீங்கள் முன்மொழிந்த நேரத்தை நிராகரித்தார்.", "நீங்கள் முன்மொழிந்த சந்திப்பு நேரம் நிராகரிக்கப்பட்டது."),
        "unmatched" to listOf("பொருத்தம் முடிந்தது", "{name} உடனான உங்கள் பொருத்தம் முடிந்துவிட்டது.", "உங்கள் பொருத்தங்களில் ஒன்று முடிந்துவிட்டது."),
        "vouch_pending_approval" to listOf("உறுதிமொழி ஒப்புதலுக்காகக் காத்திருக்கிறது", "ஒரு உறுதிமொழி உங்கள் ஒப்புதலுக்காகக் காத்திருக்கிறது.", "ஒரு உறுதிமொழி உங்கள் ஒப்புதலுக்காகக் காத்திருக்கிறது."),
    )
}
