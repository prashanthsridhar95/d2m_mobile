package com.d2m.app.ui.strings

/**
 * messaging/ui/ChatPane.kt -- shared by both the child (MatchesScreen) and
 * parent (ParentMessagesScreen) chat surfaces, since both render the same
 * ChatPane. Covers the two pieces ported from d2m_web's
 * lib/messaging/profanityFilter.js + components/ReportMessageModal.jsx:
 * the client-side flagged-language pre-send warning, and reporting a
 * specific message/media item. (Most of ChatPane's other copy -- "No
 * messages yet", "Replying to…", etc. -- predates this pass and isn't
 * localized either; not touched here, out of scope.)
 */
interface MessagingStrings {
    val flaggedLanguageWarning: String
    val sendAnyway: String
    val reportMenuItem: String
    val reportDialogTitle: String
    fun reportTextIntro(name: String): String
    fun reportMediaIntro(name: String): String
    val reportReasonLabel: String
    val reportReasonPlaceholder: String
    val reportDetailsLabel: String
    val reportSubmit: String
    val reportSubmitting: String
    val reportSubmitted: String
    val reportReasonTooShort: String
    val reportError: String

    val noMessagesYet: String
    val editingMessage: String
    fun replyingToYourself(preview: String): String
    fun replyingToName(name: String, preview: String): String
    val attachFile: String
    val messagePlaceholder: String
    val emojiStickersGifs: String
    val playVideo: String
    val messageDeleted: String
    val videoCall: String
    val voiceCall: String
    val callFailed: String
    val callDeclined: String
    val missedCall: String
    val noAnswer: String
    val uploading: String
    val attachmentFailed: String
    val failedTapToRetry: String
    val edited: String
    val reply: String
    val deleteForMe: String
    val deleteForEveryone: String
    val dismiss: String
    val minimize: String
    val flip: String
    val endCall: String
    val mute: String
    val unmute: String
    val stopVideo: String
    val startVideo: String
    val inCall: String
    val ringing: String
    val calling: String
    val connecting: String
    val incomingVideoCall: String
    val incomingAudioCall: String
    fun inCallWith(name: String): String
    val hangUp: String
    val backUpMessages: String
    val restoreMessages: String
    val archiveSetupDesc: String
    val archiveRestoreDesc: String
    val choosePin: String
    val recoveryPin: String
    val confirmPin: String
    val pinsDontMatch: String
    val pinNeverSent: String
    val notNow: String
    val iDontRememberIt: String
    val working: String
    val setUpBackup: String
    val restore: String
    val searchGifs: String
    val loadingGifs: String
    val noGifsFound: String
    val poweredByGiphy: String
}

object MessagingStringsEn : MessagingStrings {
    override val flaggedLanguageWarning = "This message may contain language you might want to reconsider."
    override val sendAnyway = "Send anyway"
    override val reportMenuItem = "Report"
    override val reportDialogTitle = "Report"
    override fun reportTextIntro(name: String) = "Tell us what's wrong with this message from $name."
    override fun reportMediaIntro(name: String) = "Tell us what's wrong with this photo/video from $name."
    override val reportReasonLabel = "Reason"
    override val reportReasonPlaceholder = "e.g. Harassment, inappropriate content…"
    override val reportDetailsLabel = "Details"
    override val reportSubmit = "Submit report"
    override val reportSubmitting = "Submitting…"
    override val reportSubmitted = "Report submitted. Our team will take a look."
    override val reportReasonTooShort = "Please say a little more about why you're reporting this."
    override val reportError = "Couldn't submit that report."
    override val noMessagesYet = "No messages yet – say hi 👋"
    override val editingMessage = "Editing message"
    override fun replyingToYourself(preview: String) = "Replying to yourself: $preview"
    override fun replyingToName(name: String, preview: String) = "Replying to $name: $preview"
    override val attachFile = "Attach a file"
    override val messagePlaceholder = "Message…"
    override val emojiStickersGifs = "Emoji, stickers & GIFs"
    override val playVideo = "Play video"
    override val messageDeleted = "This message was deleted"
    override val videoCall = "Video call"
    override val voiceCall = "Voice call"
    override val callFailed = "Call failed"
    override val callDeclined = "Call declined"
    override val missedCall = "Missed call"
    override val noAnswer = "No answer"
    override val uploading = "Uploading…"
    override val attachmentFailed = "Attachment failed"
    override val failedTapToRetry = "Failed – tap to retry"
    override val edited = "edited"
    override val reply = "Reply"
    override val deleteForMe = "Delete for me"
    override val deleteForEveryone = "Delete for everyone"
    override val dismiss = "Dismiss"
    override val minimize = "Minimize"
    override val flip = "Flip"
    override val endCall = "End"
    override val mute = "Mute"
    override val unmute = "Unmute"
    override val stopVideo = "Stop video"
    override val startVideo = "Start video"
    override val inCall = "In call"
    override val ringing = "Ringing…"
    override val calling = "Calling…"
    override val connecting = "Connecting…"
    override val incomingVideoCall = "Incoming video call"
    override val incomingAudioCall = "Incoming audio call"
    override fun inCallWith(name: String) = "In call with $name"
    override val hangUp = "Hang up"
    override val backUpMessages = "Back up your messages"
    override val restoreMessages = "Restore your messages"
    override val archiveSetupDesc = "Choose a recovery PIN. It encrypts a backup of your conversations that only you can unlock – not even Soul'd can read it. You'll need this PIN to see your message history if you ever log in from a new device."
    override val archiveRestoreDesc = "This account already has a backup from another device. Enter your recovery PIN to restore your full message history here."
    override val choosePin = "Choose a PIN (min. 4 digits)"
    override val recoveryPin = "Recovery PIN"
    override val confirmPin = "Confirm PIN"
    override val pinsDontMatch = "PINs don't match."
    override val pinNeverSent = "This PIN is never sent anywhere – if you don't remember it, your history on this device can't be recovered, but you can still send and receive new messages normally."
    override val notNow = "Not now"
    override val iDontRememberIt = "I don't remember it"
    override val working = "Working…"
    override val setUpBackup = "Set up backup"
    override val restore = "Restore"
    override val searchGifs = "Search GIFs"
    override val loadingGifs = "Loading GIFs…"
    override val noGifsFound = "No GIFs found."
    override val poweredByGiphy = "Powered by GIPHY"
}

object MessagingStringsHi : MessagingStrings {
    override val flaggedLanguageWarning = "इस संदेश में ऐसी भाषा हो सकती है जिस पर आप फिर से विचार करना चाहें।"
    override val sendAnyway = "फिर भी भेजें"
    override val reportMenuItem = "रिपोर्ट करें"
    override val reportDialogTitle = "रिपोर्ट करें"
    override fun reportTextIntro(name: String) = "हमें बताएं कि $name के इस संदेश में क्या गलत है।"
    override fun reportMediaIntro(name: String) = "हमें बताएं कि $name की इस फ़ोटो/वीडियो में क्या गलत है।"
    override val reportReasonLabel = "कारण"
    override val reportReasonPlaceholder = "जैसे, उत्पीड़न, अनुचित सामग्री…"
    override val reportDetailsLabel = "विवरण"
    override val reportSubmit = "रिपोर्ट सबमिट करें"
    override val reportSubmitting = "सबमिट हो रहा है…"
    override val reportSubmitted = "रिपोर्ट सबमिट हो गई। हमारी टीम इसे देखेगी।"
    override val reportReasonTooShort = "कृपया थोड़ा और बताएं कि आप इसे क्यों रिपोर्ट कर रहे हैं।"
    override val reportError = "वह रिपोर्ट सबमिट नहीं हो सकी।"
    override val noMessagesYet = "अभी कोई संदेश नहीं – नमस्ते कहें 👋"
    override val editingMessage = "संदेश संपादित हो रहा है"
    override fun replyingToYourself(preview: String) = "खुद को जवाब: $preview"
    override fun replyingToName(name: String, preview: String) = "$name को जवाब: $preview"
    override val attachFile = "फ़ाइल जोड़ें"
    override val messagePlaceholder = "संदेश…"
    override val emojiStickersGifs = "इमोजी, स्टिकर और GIF"
    override val playVideo = "वीडियो चलाएं"
    override val messageDeleted = "यह संदेश हटा दिया गया था"
    override val videoCall = "वीडियो कॉल"
    override val voiceCall = "वॉइस कॉल"
    override val callFailed = "कॉल विफल रहा"
    override val callDeclined = "कॉल अस्वीकार किया गया"
    override val missedCall = "छूटा हुआ कॉल"
    override val noAnswer = "कोई जवाब नहीं"
    override val uploading = "अपलोड हो रहा है…"
    override val attachmentFailed = "अटैचमेंट विफल रहा"
    override val failedTapToRetry = "विफल – दोबारा कोशिश के लिए टैप करें"
    override val edited = "संपादित"
    override val reply = "जवाब दें"
    override val deleteForMe = "मेरे लिए हटाएं"
    override val deleteForEveryone = "सभी के लिए हटाएं"
    override val dismiss = "खारिज करें"
    override val minimize = "छोटा करें"
    override val flip = "पलटें"
    override val endCall = "समाप्त"
    override val mute = "म्यूट करें"
    override val unmute = "अनम्यूट करें"
    override val stopVideo = "वीडियो बंद करें"
    override val startVideo = "वीडियो चालू करें"
    override val inCall = "कॉल में"
    override val ringing = "घंटी बज रही है…"
    override val calling = "कॉल कर रहे हैं…"
    override val connecting = "कनेक्ट हो रहा है…"
    override val incomingVideoCall = "आने वाला वीडियो कॉल"
    override val incomingAudioCall = "आने वाला ऑडियो कॉल"
    override fun inCallWith(name: String) = "$name के साथ कॉल में"
    override val hangUp = "कॉल काटें"
    override val backUpMessages = "अपने संदेशों का बैकअप लें"
    override val restoreMessages = "अपने संदेश पुनर्स्थापित करें"
    override val archiveSetupDesc = "एक रिकवरी PIN चुनें। यह आपकी बातचीत के बैकअप को एन्क्रिप्ट करता है जिसे केवल आप खोल सकते हैं – Soul'd भी इसे नहीं पढ़ सकता। किसी नए डिवाइस से लॉग इन करने पर अपना संदेश इतिहास देखने के लिए आपको इस PIN की ज़रूरत होगी।"
    override val archiveRestoreDesc = "इस खाते का दूसरे डिवाइस से बैकअप पहले से है। अपना पूरा संदेश इतिहास यहां पुनर्स्थापित करने के लिए अपना रिकवरी PIN दर्ज करें।"
    override val choosePin = "PIN चुनें (कम से कम 4 अंक)"
    override val recoveryPin = "रिकवरी PIN"
    override val confirmPin = "PIN की पुष्टि करें"
    override val pinsDontMatch = "PIN मेल नहीं खाते।"
    override val pinNeverSent = "यह PIN कहीं नहीं भेजा जाता – यदि आपको यह याद नहीं है, तो इस डिवाइस पर आपका इतिहास वापस नहीं मिल सकता, लेकिन आप नए संदेश सामान्य रूप से भेज और पा सकते हैं।"
    override val notNow = "अभी नहीं"
    override val iDontRememberIt = "मुझे याद नहीं है"
    override val working = "काम हो रहा है…"
    override val setUpBackup = "बैकअप सेट करें"
    override val restore = "पुनर्स्थापित करें"
    override val searchGifs = "GIF खोजें"
    override val loadingGifs = "GIF लोड हो रहे हैं…"
    override val noGifsFound = "कोई GIF नहीं मिला।"
    override val poweredByGiphy = "GIPHY द्वारा संचालित"
}

object MessagingStringsTa : MessagingStrings {
    override val flaggedLanguageWarning = "இந்தச் செய்தியில் நீங்கள் மீண்டும் சிந்திக்க வேண்டிய வார்த்தைகள் இருக்கலாம்."
    override val sendAnyway = "எப்படியும் அனுப்பு"
    override val reportMenuItem = "புகார் செய்"
    override val reportDialogTitle = "புகார் செய்"
    override fun reportTextIntro(name: String) = "$name அனுப்பிய இந்தச் செய்தியில் என்ன தவறு என்று எங்களிடம் கூறுங்கள்."
    override fun reportMediaIntro(name: String) = "$name அனுப்பிய இந்தப் படம்/வீடியோவில் என்ன தவறு என்று எங்களிடம் கூறுங்கள்."
    override val reportReasonLabel = "காரணம்"
    override val reportReasonPlaceholder = "எ.கா. துன்புறுத்தல், பொருத்தமற்ற உள்ளடக்கம்…"
    override val reportDetailsLabel = "விவரங்கள்"
    override val reportSubmit = "புகாரை சமர்ப்பிக்கவும்"
    override val reportSubmitting = "சமர்ப்பிக்கப்படுகிறது…"
    override val reportSubmitted = "புகார் சமர்ப்பிக்கப்பட்டது. எங்கள் குழு இதைப் பார்க்கும்."
    override val reportReasonTooShort = "இதை ஏன் புகார் செய்கிறீர்கள் என்பதை இன்னும் சிறிது கூறுங்கள்."
    override val reportError = "அந்தப் புகாரை சமர்ப்பிக்க முடியவில்லை."
    override val noMessagesYet = "இன்னும் செய்திகள் இல்லை – வணக்கம் சொல்லுங்கள் 👋"
    override val editingMessage = "செய்தி திருத்தப்படுகிறது"
    override fun replyingToYourself(preview: String) = "உங்களுக்கே பதில்: $preview"
    override fun replyingToName(name: String, preview: String) = "$name-க்கு பதில்: $preview"
    override val attachFile = "கோப்பை இணை"
    override val messagePlaceholder = "செய்தி…"
    override val emojiStickersGifs = "எமோஜி, ஸ்டிக்கர், GIF"
    override val playVideo = "வீடியோவை இயக்கு"
    override val messageDeleted = "இந்தச் செய்தி நீக்கப்பட்டது"
    override val videoCall = "வீடியோ அழைப்பு"
    override val voiceCall = "குரல் அழைப்பு"
    override val callFailed = "அழைப்பு தோல்வியடைந்தது"
    override val callDeclined = "அழைப்பு நிராகரிக்கப்பட்டது"
    override val missedCall = "தவறிய அழைப்பு"
    override val noAnswer = "பதில் இல்லை"
    override val uploading = "பதிவேற்றுகிறது…"
    override val attachmentFailed = "இணைப்பு தோல்வியடைந்தது"
    override val failedTapToRetry = "தோல்வி – மீண்டும் முயல தட்டவும்"
    override val edited = "திருத்தப்பட்டது"
    override val reply = "பதிலளி"
    override val deleteForMe = "எனக்கு மட்டும் நீக்கு"
    override val deleteForEveryone = "அனைவருக்கும் நீக்கு"
    override val dismiss = "மூடு"
    override val minimize = "சிறிதாக்கு"
    override val flip = "மாற்று"
    override val endCall = "முடி"
    override val mute = "ஒலியடக்கு"
    override val unmute = "ஒலியை இயக்கு"
    override val stopVideo = "வீடியோவை நிறுத்து"
    override val startVideo = "வீடியோவைத் தொடங்கு"
    override val inCall = "அழைப்பில்"
    override val ringing = "அழைக்கிறது…"
    override val calling = "அழைக்கிறது…"
    override val connecting = "இணைக்கிறது…"
    override val incomingVideoCall = "உள்வரும் வீடியோ அழைப்பு"
    override val incomingAudioCall = "உள்வரும் ஆடியோ அழைப்பு"
    override fun inCallWith(name: String) = "$name உடன் அழைப்பில்"
    override val hangUp = "அழைப்பைத் துண்டி"
    override val backUpMessages = "உங்கள் செய்திகளைக் காப்புப்பிரதி எடுங்கள்"
    override val restoreMessages = "உங்கள் செய்திகளை மீட்டெடுங்கள்"
    override val archiveSetupDesc = "மீட்பு PIN ஒன்றைத் தேர்ந்தெடுங்கள். இது உங்கள் உரையாடல்களின் காப்புப்பிரதியை மறையாக்கம் செய்கிறது; அதை நீங்கள் மட்டுமே திறக்க முடியும் – Soul'd-ஆலும் படிக்க முடியாது. புதிய சாதனத்தில் உள்நுழையும்போது உங்கள் செய்தி வரலாற்றைப் பார்க்க இந்த PIN தேவைப்படும்."
    override val archiveRestoreDesc = "இந்தக் கணக்கில் மற்றொரு சாதனத்தின் காப்புப்பிரதி ஏற்கனவே உள்ளது. உங்கள் முழு செய்தி வரலாற்றை இங்கே மீட்டெடுக்க மீட்பு PIN-ஐ உள்ளிடுங்கள்."
    override val choosePin = "PIN-ஐத் தேர்ந்தெடுங்கள் (குறைந்தது 4 இலக்கங்கள்)"
    override val recoveryPin = "மீட்பு PIN"
    override val confirmPin = "PIN-ஐ உறுதிப்படுத்துங்கள்"
    override val pinsDontMatch = "PIN-கள் பொருந்தவில்லை."
    override val pinNeverSent = "இந்த PIN எங்கும் அனுப்பப்படுவதில்லை – இது நினைவில் இல்லையெனில் இந்தச் சாதனத்தில் உங்கள் வரலாற்றை மீட்க முடியாது; ஆனால் புதிய செய்திகளை வழக்கம்போல அனுப்பவும் பெறவும் முடியும்."
    override val notNow = "இப்போது வேண்டாம்"
    override val iDontRememberIt = "எனக்கு நினைவில்லை"
    override val working = "செயல்படுகிறது…"
    override val setUpBackup = "காப்புப்பிரதியை அமை"
    override val restore = "மீட்டெடு"
    override val searchGifs = "GIF-களைத் தேடு"
    override val loadingGifs = "GIF-கள் ஏற்றப்படுகின்றன…"
    override val noGifsFound = "GIF-கள் எதுவும் கிடைக்கவில்லை."
    override val poweredByGiphy = "GIPHY வழங்குகிறது"
}
