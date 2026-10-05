package com.d2m.app.ui.strings

/**
 * TrustScreen.kt -- WedLock's full trust_bridge surface (vouches,
 * connections, endorsements, recommendations, external references).
 * d2m_web's own "trust" namespace backs a differently-scoped page (My
 * Profile's settings nav, not this richer flow), so only a handful of
 * concepts actually overlap 1:1 (reused below where the English wording
 * matches exactly); everything else is translated fresh for this screen.
 * Status words and claim-scope/connection-type labels (TrustScreen.kt's
 * own humanize()) are deliberately NOT part of this table -- d2m_web's
 * TrustScreen.jsx's own statusLabel()/humanize() do the same raw
 * underscore-replace with no translation, so mobile matches that, not a
 * fully-localized enum label set.
 */
interface TrustStrings {
    val pageTitle: String
    val introBody: String
    val tabVouches: String
    val tabConnections: String
    val tabEndorsements: String
    val tabRecommendations: String
    val tabReferences: String

    // Shared across sections.
    val approve: String
    val decline: String
    val withdraw: String
    val cancel: String
    val copy: String
    val hide: String
    val revoke: String
    val submit: String
    val send: String
    val accept: String
    val acceptingEllipsis: String
    val creatingEllipsis: String
    val sendingEllipsis: String
    val requestingEllipsis: String
    val loading: String
    val waitingOnChildTitle: String
    val errGeneric: String
    val visibilityLabel: String
    val trustedConnectionLabel: String
    val chooseOnePlaceholder: String
    val noActiveConnections: String
    fun created(date: String): String
    fun expires(date: String): String
    val inviteReadyLabel: String

    // Vouches
    val errLoadVouches: String
    val errLoadGivenVouches: String
    val waitingOnChildBodyVouches: String
    val vouchCreateTitle: String
    val vouchCreateBody: String
    val createVouch: String
    val errCreateVouch: String
    val tabReceived: String
    val tabGiven: String
    val vouchesEmptyTitle: String
    val vouchesEmptyBody: String
    val givenVouchesEmptyTitle: String
    val givenVouchesEmptyBody: String
    val subjectProfileUnavailable: String

    // Connections
    val errLoadConnections: String
    val inviteConnectionTitle: String
    val inviteConnectionBody: String
    val relationshipLabel: String
    val createInvite: String
    val errCreateInvite: String
    val acceptInviteTitle: String
    val inviteCodeLabel: String
    val inviteCodePlaceholder: String
    val errAcceptInvite: String
    val connectionsEmptyTitle: String
    val connectionsEmptyBody: String
    val errRevoke: String

    // Endorsements
    val errLoadEndorsements: String
    val waitingOnChildBodyEndorsements: String
    val endorsementRequestTitle: String
    val endorsementRequestBody: String
    val whatConfirmingLabel: String
    val requestEndorsement: String
    val errRequestEndorsement: String
    val endorsementsEmptyTitle: String
    val endorsementsEmptyBody: String

    // Recommendations
    val errLoadRecommendations: String
    val waitingOnChildBodyRecommendations: String
    val recommendationRequestTitle: String
    val recommendationRequestBody: String
    val requestRecommendation: String
    val errRequestRecommendation: String
    val recommendationsEmptyTitle: String
    val recommendationsEmptyBody: String
    val whatConfirmingQuestion: String
    val yourRecommendationLabel: String
    val recommendationPlaceholder: String
    val knownSinceYearLabel: String
    val knownSinceYearPlaceholder: String
    val errSubmitRecommendation: String

    // External references
    val errLoadReferences: String
    val waitingOnChildBodyReferences: String
    val inviteReferenceTitle: String
    fun inviteReferenceBody(hours: Int): String
    val contactLabel: String
    val contactPlaceholder: String
    val relationshipHowLabel: String
    val relationshipHowPlaceholder: String
    val sendInvite: String
    val errSendInvite: String
    val referencesEmptyTitle: String
    val referencesEmptyBody: String
}

object TrustStringsEn : TrustStrings {
    override val pageTitle = "Trust & vouches"
    override val introBody = "Vouches, trusted connections, and endorsements from WedLock's identity network -- people who know this family can back up details on this profile."
    override val tabVouches = "Vouches"
    override val tabConnections = "Connections"
    override val tabEndorsements = "Endorsements"
    override val tabRecommendations = "Recommendations"
    override val tabReferences = "References"
    override val approve = "Approve"
    override val decline = "Decline"
    override val withdraw = "Withdraw"
    override val cancel = "Cancel"
    override val copy = "Copy"
    override val hide = "Hide"
    override val revoke = "Revoke"
    override val submit = "Submit"
    override val send = "Send"
    override val accept = "Accept"
    override val acceptingEllipsis = "Accepting…"
    override val creatingEllipsis = "Creating…"
    override val sendingEllipsis = "Sending…"
    override val requestingEllipsis = "Requesting…"
    override val loading = "Loading…"
    override val waitingOnChildTitle = "Waiting on your child"
    override val errGeneric = "That didn't go through."
    override val visibilityLabel = "Visibility"
    override val trustedConnectionLabel = "Trusted connection"
    override val chooseOnePlaceholder = "Choose one"
    override val noActiveConnections = "No active trusted connections yet."
    override fun created(date: String) = "Created $date"
    override fun expires(date: String) = "Expires $date"
    override val inviteReadyLabel = "Invite ready -- share it once"
    override val errLoadVouches = "Couldn't load vouches."
    override val errLoadGivenVouches = "Couldn't load vouches you've given."
    override val waitingOnChildBodyVouches = "Once they claim your invite, you'll be able to manage vouches from here."
    override val vouchCreateTitle = "Ask someone to vouch for this profile"
    override val vouchCreateBody = "Choose what they're confirming, then send them the request outside the app -- they'll approve it from their own WedLock account."
    override val createVouch = "Create vouch"
    override val errCreateVouch = "Couldn't create a vouch."
    override val tabReceived = "Received"
    override val tabGiven = "Given"
    override val vouchesEmptyTitle = "No vouches yet"
    override val vouchesEmptyBody = "Vouches you request or receive will appear here."
    override val givenVouchesEmptyTitle = "You haven't vouched for anyone yet"
    override val givenVouchesEmptyBody = "Vouches you give, on any profile, will appear here."
    override val subjectProfileUnavailable = "Profile no longer available"
    override val errLoadConnections = "Couldn't load trusted connections."
    override val inviteConnectionTitle = "Invite a trusted connection"
    override val inviteConnectionBody = "Creates a one-time invite link to share outside the app. Whoever accepts it can later endorse specific details on this profile."
    override val relationshipLabel = "Relationship"
    override val createInvite = "Create invite"
    override val errCreateInvite = "Couldn't create an invite."
    override val acceptInviteTitle = "Accept an invite"
    override val inviteCodeLabel = "Invite code"
    override val inviteCodePlaceholder = "Paste the invite someone shared with you"
    override val errAcceptInvite = "Couldn't accept that invite."
    override val connectionsEmptyTitle = "No trusted connections yet"
    override val connectionsEmptyBody = "Connections you invite or accept will appear here."
    override val errRevoke = "Couldn't revoke that connection."
    override val errLoadEndorsements = "Couldn't load endorsements."
    override val waitingOnChildBodyEndorsements = "Once they claim your invite, you'll be able to manage endorsements from here."
    override val endorsementRequestTitle = "Request an endorsement"
    override val endorsementRequestBody = "Built on an active trusted connection -- add one from the Connections tab first if the list below is empty."
    override val whatConfirmingLabel = "What are they confirming"
    override val requestEndorsement = "Request endorsement"
    override val errRequestEndorsement = "Couldn't request an endorsement."
    override val endorsementsEmptyTitle = "No endorsements yet"
    override val endorsementsEmptyBody = "Endorsements you request or receive will appear here."
    override val errLoadRecommendations = "Couldn't load recommendations."
    override val waitingOnChildBodyRecommendations = "Once they claim your invite, you'll be able to manage recommendations from here."
    override val recommendationRequestTitle = "Request a recommendation"
    override val recommendationRequestBody = "Built on an active trusted connection -- add one from the Connections tab first if the list below is empty. They'll write it themselves once you send the request."
    override val requestRecommendation = "Request recommendation"
    override val errRequestRecommendation = "Couldn't request a recommendation."
    override val recommendationsEmptyTitle = "No recommendations yet"
    override val recommendationsEmptyBody = "Recommendations you request or receive will appear here."
    override val whatConfirmingQuestion = "What are you confirming?"
    override val yourRecommendationLabel = "Your recommendation"
    override val recommendationPlaceholder = "20-500 characters"
    override val knownSinceYearLabel = "Known since year"
    override val knownSinceYearPlaceholder = "Optional, e.g. 2015"
    override val errSubmitRecommendation = "Couldn't submit that recommendation."
    override val errLoadReferences = "Couldn't load external references."
    override val waitingOnChildBodyReferences = "Once they claim your invite, you'll be able to manage external references from here."
    override val inviteReferenceTitle = "Invite an external reference"
    override fun inviteReferenceBody(hours: Int) = "Someone outside your trusted connections -- a colleague, family friend, anyone who knows you. They'll get a link to confirm a few details about you, valid for $hours hours."
    override val contactLabel = "Their email or phone"
    override val contactPlaceholder = "email@example.com or +91..."
    override val relationshipHowLabel = "How do you know them"
    override val relationshipHowPlaceholder = "e.g. colleague, family friend"
    override val sendInvite = "Send invite"
    override val errSendInvite = "Couldn't send that invite."
    override val referencesEmptyTitle = "No external references yet"
    override val referencesEmptyBody = "References you invite will appear here."
}

object TrustStringsHi : TrustStrings {
    override val pageTitle = "विश्वास और वाउच"
    override val introBody = "WedLock के पहचान नेटवर्क से वाउच, भरोसेमंद कनेक्शन, और एंडोर्समेंट -- जो लोग इस परिवार को जानते हैं वे इस प्रोफ़ाइल पर विवरणों की पुष्टि कर सकते हैं।"
    override val tabVouches = "वाउच"
    override val tabConnections = "कनेक्शन"
    override val tabEndorsements = "एंडोर्समेंट"
    override val tabRecommendations = "सिफारिशें"
    override val tabReferences = "संदर्भ"
    override val approve = "स्वीकृत करें"
    override val decline = "अस्वीकार करें"
    override val withdraw = "वापस लें"
    override val cancel = "रद्द करें"
    override val copy = "कॉपी करें"
    override val hide = "छुपाएं"
    override val revoke = "रद्द करें"
    override val submit = "सबमिट करें"
    override val send = "भेजें"
    override val accept = "स्वीकार करें"
    override val acceptingEllipsis = "स्वीकार किया जा रहा है…"
    override val creatingEllipsis = "बनाया जा रहा है…"
    override val sendingEllipsis = "भेजा जा रहा है…"
    override val requestingEllipsis = "रिक्वेस्ट भेजी जा रही है…"
    override val loading = "लोड हो रहा है…"
    override val waitingOnChildTitle = "आपके बच्चे की प्रतीक्षा है"
    override val errGeneric = "वह सफल नहीं हुआ।"
    override val visibilityLabel = "विज़िबिलिटी"
    override val trustedConnectionLabel = "भरोसेमंद कनेक्शन"
    override val chooseOnePlaceholder = "एक चुनें"
    override val noActiveConnections = "अभी तक कोई सक्रिय भरोसेमंद कनेक्शन नहीं।"
    override fun created(date: String) = "$date को बनाया गया"
    override fun expires(date: String) = "$date को समाप्त"
    override val inviteReadyLabel = "इनवाइट तैयार है -- इसे एक बार शेयर करें"
    override val errLoadVouches = "वाउच लोड नहीं हो सके।"
    override val errLoadGivenVouches = "आपके द्वारा दिए गए वाउच लोड नहीं हो सके।"
    override val waitingOnChildBodyVouches = "एक बार वे आपका इनवाइट स्वीकार कर लें, तो आप यहां से वाउच प्रबंधित कर सकेंगे।"
    override val vouchCreateTitle = "किसी से इस प्रोफ़ाइल के लिए वाउच करने को कहें"
    override val vouchCreateBody = "चुनें कि वे क्या पुष्टि करेंगे, फिर ऐप के बाहर उन्हें रिक्वेस्ट भेजें -- वे इसे अपने WedLock खाते से स्वीकृत करेंगे।"
    override val createVouch = "वाउच बनाएं"
    override val errCreateVouch = "वाउच नहीं बनाया जा सका।"
    override val tabReceived = "प्राप्त"
    override val tabGiven = "दिए गए"
    override val vouchesEmptyTitle = "अभी तक कोई वाउच नहीं"
    override val vouchesEmptyBody = "आपके द्वारा मांगे या प्राप्त किए गए वाउच यहां दिखेंगे।"
    override val givenVouchesEmptyTitle = "आपने अभी तक किसी के लिए वाउच नहीं किया"
    override val givenVouchesEmptyBody = "आपके द्वारा दिए गए वाउच, किसी भी प्रोफ़ाइल पर, यहां दिखेंगे।"
    override val subjectProfileUnavailable = "प्रोफ़ाइल अब उपलब्ध नहीं है"
    override val errLoadConnections = "भरोसेमंद कनेक्शन लोड नहीं हो सके।"
    override val inviteConnectionTitle = "एक भरोसेमंद कनेक्शन इनवाइट करें"
    override val inviteConnectionBody = "ऐप के बाहर शेयर करने के लिए एक बार का इनवाइट लिंक बनाता है। जो भी इसे स्वीकार करे वह बाद में इस प्रोफ़ाइल के विशेष विवरणों का एंडोर्समेंट कर सकता है।"
    override val relationshipLabel = "रिश्ता"
    override val createInvite = "इनवाइट बनाएं"
    override val errCreateInvite = "इनवाइट नहीं बनाया जा सका।"
    override val acceptInviteTitle = "एक इनवाइट स्वीकार करें"
    override val inviteCodeLabel = "इनवाइट कोड"
    override val inviteCodePlaceholder = "किसी ने आपसे शेयर किया इनवाइट पेस्ट करें"
    override val errAcceptInvite = "वह इनवाइट स्वीकार नहीं हो सका।"
    override val connectionsEmptyTitle = "अभी तक कोई भरोसेमंद कनेक्शन नहीं"
    override val connectionsEmptyBody = "आपके द्वारा इनवाइट किए या स्वीकार किए गए कनेक्शन यहां दिखेंगे।"
    override val errRevoke = "वह कनेक्शन रद्द नहीं हो सका।"
    override val errLoadEndorsements = "एंडोर्समेंट लोड नहीं हो सके।"
    override val waitingOnChildBodyEndorsements = "एक बार वे आपका इनवाइट स्वीकार कर लें, तो आप यहां से एंडोर्समेंट प्रबंधित कर सकेंगे।"
    override val endorsementRequestTitle = "एक एंडोर्समेंट के लिए रिक्वेस्ट करें"
    override val endorsementRequestBody = "एक सक्रिय भरोसेमंद कनेक्शन पर आधारित -- यदि नीचे की सूची खाली है तो पहले कनेक्शन टैब से एक जोड़ें।"
    override val whatConfirmingLabel = "वे क्या पुष्टि कर रहे हैं"
    override val requestEndorsement = "एंडोर्समेंट के लिए रिक्वेस्ट करें"
    override val errRequestEndorsement = "एंडोर्समेंट के लिए रिक्वेस्ट नहीं हो सकी।"
    override val endorsementsEmptyTitle = "अभी तक कोई एंडोर्समेंट नहीं"
    override val endorsementsEmptyBody = "आपके द्वारा मांगे या प्राप्त किए गए एंडोर्समेंट यहां दिखेंगे।"
    override val errLoadRecommendations = "सिफारिशें लोड नहीं हो सकीं।"
    override val waitingOnChildBodyRecommendations = "एक बार वे आपका इनवाइट स्वीकार कर लें, तो आप यहां से सिफारिशें प्रबंधित कर सकेंगे।"
    override val recommendationRequestTitle = "एक सिफारिश के लिए रिक्वेस्ट करें"
    override val recommendationRequestBody = "एक सक्रिय भरोसेमंद कनेक्शन पर आधारित -- यदि नीचे की सूची खाली है तो पहले कनेक्शन टैब से एक जोड़ें। आपकी रिक्वेस्ट भेजने पर वे इसे स्वयं लिखेंगे।"
    override val requestRecommendation = "सिफारिश के लिए रिक्वेस्ट करें"
    override val errRequestRecommendation = "सिफारिश के लिए रिक्वेस्ट नहीं हो सकी।"
    override val recommendationsEmptyTitle = "अभी तक कोई सिफारिश नहीं"
    override val recommendationsEmptyBody = "आपके द्वारा मांगी या प्राप्त की गई सिफारिशें यहां दिखेंगी।"
    override val whatConfirmingQuestion = "आप क्या पुष्टि कर रहे हैं?"
    override val yourRecommendationLabel = "आपकी सिफारिश"
    override val recommendationPlaceholder = "20-500 अक्षर"
    override val knownSinceYearLabel = "किस वर्ष से जानते हैं"
    override val knownSinceYearPlaceholder = "वैकल्पिक, जैसे 2015"
    override val errSubmitRecommendation = "वह सिफारिश सबमिट नहीं हो सकी।"
    override val errLoadReferences = "बाहरी संदर्भ लोड नहीं हो सके।"
    override val waitingOnChildBodyReferences = "एक बार वे आपका इनवाइट स्वीकार कर लें, तो आप यहां से बाहरी संदर्भ प्रबंधित कर सकेंगे।"
    override val inviteReferenceTitle = "एक बाहरी संदर्भ इनवाइट करें"
    override fun inviteReferenceBody(hours: Int) = "आपके भरोसेमंद कनेक्शनों से बाहर कोई व्यक्ति -- एक सहकर्मी, परिवार का मित्र, जो भी आपको जानता हो। उन्हें आपके बारे में कुछ विवरण पुष्टि करने के लिए एक लिंक मिलेगा, जो $hours घंटे तक मान्य रहेगा।"
    override val contactLabel = "उनका ईमेल या फ़ोन"
    override val contactPlaceholder = "email@example.com या +91..."
    override val relationshipHowLabel = "आप उन्हें कैसे जानते हैं"
    override val relationshipHowPlaceholder = "जैसे सहकर्मी, परिवार का मित्र"
    override val sendInvite = "इनवाइट भेजें"
    override val errSendInvite = "वह इनवाइट नहीं भेजा जा सका।"
    override val referencesEmptyTitle = "अभी तक कोई बाहरी संदर्भ नहीं"
    override val referencesEmptyBody = "आपके द्वारा इनवाइट किए गए संदर्भ यहां दिखेंगे।"
}

object TrustStringsTa : TrustStrings {
    override val pageTitle = "நம்பிக்கை & வவுச்கள்"
    override val introBody = "WedLock-இன் அடையாள நெட்வொர்க்கிலிருந்து வவுச்கள், நம்பகமான இணைப்புகள், மற்றும் ஒப்புதல்கள் -- இந்த குடும்பத்தை அறிந்தவர்கள் இந்த சுயவிவரத்தில் விவரங்களை உறுதிப்படுத்தலாம்."
    override val tabVouches = "வவுச்கள்"
    override val tabConnections = "இணைப்புகள்"
    override val tabEndorsements = "ஒப்புதல்கள்"
    override val tabRecommendations = "பரிந்துரைகள்"
    override val tabReferences = "சான்றுகள்"
    override val approve = "அங்கீகரிக்கவும்"
    override val decline = "நிராகரிக்கவும்"
    override val withdraw = "திரும்பப்பெறு"
    override val cancel = "ரத்துசெய்"
    override val copy = "நகலெடு"
    override val hide = "மறை"
    override val revoke = "நீக்கு"
    override val submit = "சமர்ப்பிக்கவும்"
    override val send = "அனுப்பு"
    override val accept = "ஏற்றுக்கொள்"
    override val acceptingEllipsis = "ஏற்கப்படுகிறது…"
    override val creatingEllipsis = "உருவாக்கப்படுகிறது…"
    override val sendingEllipsis = "அனுப்பப்படுகிறது…"
    override val requestingEllipsis = "கோரப்படுகிறது…"
    override val loading = "ஏற்றப்படுகிறது…"
    override val waitingOnChildTitle = "உங்கள் குழந்தைக்காக காத்திருக்கிறோம்"
    override val errGeneric = "அது வெற்றிபெறவில்லை."
    override val visibilityLabel = "தெரிவுநிலை"
    override val trustedConnectionLabel = "நம்பகமான இணைப்பு"
    override val chooseOnePlaceholder = "ஒன்றைத் தேர்ந்தெடுக்கவும்"
    override val noActiveConnections = "இதுவரை செயலில் உள்ள நம்பகமான இணைப்புகள் இல்லை."
    override fun created(date: String) = "$date அன்று உருவாக்கப்பட்டது"
    override fun expires(date: String) = "$date அன்று காலாவதியாகும்"
    override val inviteReadyLabel = "அழைப்பு தயார் -- அதை ஒரு முறை பகிரவும்"
    override val errLoadVouches = "வவுச்களை ஏற்ற முடியவில்லை."
    override val errLoadGivenVouches = "நீங்கள் அளித்த வவுச்களை ஏற்ற முடியவில்லை."
    override val waitingOnChildBodyVouches = "அவர்கள் உங்கள் அழைப்பை ஏற்றவுடன், இங்கிருந்து வவுச்களை நிர்வகிக்கலாம்."
    override val vouchCreateTitle = "இந்த சுயவிவரத்திற்கு வவுச் செய்யும்படி ஒருவரைக் கேளுங்கள்"
    override val vouchCreateBody = "அவர்கள் எதை உறுதிப்படுத்துகிறார்கள் என்பதைத் தேர்ந்தெடுக்கவும், பின் ஆப்பிற்கு வெளியே அவர்களுக்கு கோரிக்கையை அனுப்பவும் -- அவர்கள் தங்கள் சொந்த WedLock கணக்கிலிருந்து அதை அங்கீகரிப்பார்கள்."
    override val createVouch = "வவுச் உருவாக்கவும்"
    override val errCreateVouch = "ஒரு வவுச்சை உருவாக்க முடியவில்லை."
    override val tabReceived = "பெறப்பட்டவை"
    override val tabGiven = "அளிக்கப்பட்டவை"
    override val vouchesEmptyTitle = "இதுவரை வவுச்கள் இல்லை"
    override val vouchesEmptyBody = "நீங்கள் கோரிய அல்லது பெற்ற வவுச்கள் இங்கே தோன்றும்."
    override val givenVouchesEmptyTitle = "நீங்கள் இதுவரை யாருக்கும் வவுச் செய்யவில்லை"
    override val givenVouchesEmptyBody = "நீங்கள் அளிக்கும் வவுச்கள், எந்த சுயவிவரத்திலும், இங்கே தோன்றும்."
    override val subjectProfileUnavailable = "சுயவிவரம் இனி கிடைக்கவில்லை"
    override val errLoadConnections = "நம்பகமான இணைப்புகளை ஏற்ற முடியவில்லை."
    override val inviteConnectionTitle = "ஒரு நம்பகமான இணைப்பை அழைக்கவும்"
    override val inviteConnectionBody = "ஆப்பிற்கு வெளியே பகிர ஒரு முறை அழைப்பு இணைப்பை உருவாக்குகிறது. அதை ஏற்கும் எவரும் பின்னர் இந்த சுயவிவரத்தின் குறிப்பிட்ட விவரங்களை ஒப்புதல் செய்யலாம்."
    override val relationshipLabel = "உறவுமுறை"
    override val createInvite = "அழைப்பை உருவாக்கவும்"
    override val errCreateInvite = "ஒரு அழைப்பை உருவாக்க முடியவில்லை."
    override val acceptInviteTitle = "ஒரு அழைப்பை ஏற்கவும்"
    override val inviteCodeLabel = "அழைப்பு குறியீடு"
    override val inviteCodePlaceholder = "ஒருவர் உங்களுடன் பகிர்ந்த அழைப்பை ஒட்டவும்"
    override val errAcceptInvite = "அந்த அழைப்பை ஏற்க முடியவில்லை."
    override val connectionsEmptyTitle = "இதுவரை நம்பகமான இணைப்புகள் இல்லை"
    override val connectionsEmptyBody = "நீங்கள் அழைக்கும் அல்லது ஏற்கும் இணைப்புகள் இங்கே தோன்றும்."
    override val errRevoke = "அந்த இணைப்பை நீக்க முடியவில்லை."
    override val errLoadEndorsements = "ஒப்புதல்களை ஏற்ற முடியவில்லை."
    override val waitingOnChildBodyEndorsements = "அவர்கள் உங்கள் அழைப்பை ஏற்றவுடன், இங்கிருந்து ஒப்புதல்களை நிர்வகிக்கலாம்."
    override val endorsementRequestTitle = "ஒரு ஒப்புதலைக் கோரவும்"
    override val endorsementRequestBody = "ஒரு செயலில் உள்ள நம்பகமான இணைப்பின் அடிப்படையில் -- கீழே உள்ள பட்டியல் காலியாக இருந்தால் முதலில் இணைப்புகள் தாவலில் இருந்து ஒன்றைச் சேர்க்கவும்."
    override val whatConfirmingLabel = "அவர்கள் எதை உறுதிப்படுத்துகிறார்கள்"
    override val requestEndorsement = "ஒப்புதலைக் கோரவும்"
    override val errRequestEndorsement = "ஒப்புதலைக் கோர முடியவில்லை."
    override val endorsementsEmptyTitle = "இதுவரை ஒப்புதல்கள் இல்லை"
    override val endorsementsEmptyBody = "நீங்கள் கோரிய அல்லது பெற்ற ஒப்புதல்கள் இங்கே தோன்றும்."
    override val errLoadRecommendations = "பரிந்துரைகளை ஏற்ற முடியவில்லை."
    override val waitingOnChildBodyRecommendations = "அவர்கள் உங்கள் அழைப்பை ஏற்றவுடன், இங்கிருந்து பரிந்துரைகளை நிர்வகிக்கலாம்."
    override val recommendationRequestTitle = "ஒரு பரிந்துரையைக் கோரவும்"
    override val recommendationRequestBody = "ஒரு செயலில் உள்ள நம்பகமான இணைப்பின் அடிப்படையில் -- கீழே உள்ள பட்டியல் காலியாக இருந்தால் முதலில் இணைப்புகள் தாவலில் இருந்து ஒன்றைச் சேர்க்கவும். நீங்கள் கோரிக்கையை அனுப்பியவுடன் அவர்களே அதை எழுதுவார்கள்."
    override val requestRecommendation = "பரிந்துரையைக் கோரவும்"
    override val errRequestRecommendation = "பரிந்துரையைக் கோர முடியவில்லை."
    override val recommendationsEmptyTitle = "இதுவரை பரிந்துரைகள் இல்லை"
    override val recommendationsEmptyBody = "நீங்கள் கோரிய அல்லது பெற்ற பரிந்துரைகள் இங்கே தோன்றும்."
    override val whatConfirmingQuestion = "நீங்கள் எதை உறுதிப்படுத்துகிறீர்கள்?"
    override val yourRecommendationLabel = "உங்கள் பரிந்துரை"
    override val recommendationPlaceholder = "20-500 எழுத்துகள்"
    override val knownSinceYearLabel = "எந்த ஆண்டு முதல் அறிவீர்கள்"
    override val knownSinceYearPlaceholder = "விருப்பத்தேர்வு, எ.கா. 2015"
    override val errSubmitRecommendation = "அந்த பரிந்துரையை சமர்ப்பிக்க முடியவில்லை."
    override val errLoadReferences = "வெளி சான்றுகளை ஏற்ற முடியவில்லை."
    override val waitingOnChildBodyReferences = "அவர்கள் உங்கள் அழைப்பை ஏற்றவுடன், இங்கிருந்து வெளி சான்றுகளை நிர்வகிக்கலாம்."
    override val inviteReferenceTitle = "ஒரு வெளி சான்றை அழைக்கவும்"
    override fun inviteReferenceBody(hours: Int) = "உங்கள் நம்பகமான இணைப்புகளுக்கு வெளியே ஒருவர் -- ஒரு சகாவேலையாளர், குடும்ப நண்பர், உங்களை அறிந்த எவரும். உங்களைப் பற்றிய சில விவரங்களை உறுதிப்படுத்த அவர்களுக்கு ஒரு இணைப்பு கிடைக்கும், $hours மணி நேரம் வரை செல்லுபடியாகும்."
    override val contactLabel = "அவர்களின் மின்னஞ்சல் அல்லது தொலைபேசி"
    override val contactPlaceholder = "email@example.com அல்லது +91..."
    override val relationshipHowLabel = "நீங்கள் அவர்களை எப்படி அறிவீர்கள்"
    override val relationshipHowPlaceholder = "எ.கா. சகாவேலையாளர், குடும்ப நண்பர்"
    override val sendInvite = "அழைப்பை அனுப்பவும்"
    override val errSendInvite = "அந்த அழைப்பை அனுப்ப முடியவில்லை."
    override val referencesEmptyTitle = "இதுவரை வெளி சான்றுகள் இல்லை"
    override val referencesEmptyBody = "நீங்கள் அழைக்கும் சான்றுகள் இங்கே தோன்றும்."
}
