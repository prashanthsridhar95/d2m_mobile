package com.d2m.app.ui.strings

/**
 * Truly ubiquitous words/phrases reused across many unrelated screens --
 * generic actions, loading/error copy, and AppScaffold.kt's bottom-tab
 * labels. Mirrors d2m_web's "common" namespace. Anything specific to one
 * screen or one component belongs in that namespace's own strings file
 * instead, even if it happens to repeat a word used here.
 */
interface CommonStrings {
    val save: String
    val saveChanges: String
    val saving: String
    val saved: String
    val cancel: String
    val back: String
    val next: String
    val continueLabel: String
    val close: String
    val done: String
    val edit: String
    val remove: String
    val delete: String
    val confirm: String
    val approve: String
    val decline: String
    val accept: String
    val reject: String
    val send: String
    val share: String
    val copy: String
    val copied: String
    val loading: String
    val retry: String
    val yes: String
    val no: String
    val somethingWentWrong: String

    // AppScaffold.kt's D2MTab labels.
    val tabDashboard: String
    val tabSearch: String
    val tabSharing: String
    val tabSettings: String
    val tabHome: String
    val tabDiscover: String
    val tabMatches: String
    val tabNotifications: String
}

object CommonStringsEn : CommonStrings {
    override val save = "Save"
    override val saveChanges = "Save changes"
    override val saving = "Saving…"
    override val saved = "Saved"
    override val cancel = "Cancel"
    override val back = "Back"
    override val next = "Next"
    override val continueLabel = "Continue"
    override val close = "Close"
    override val done = "Done"
    override val edit = "Edit"
    override val remove = "Remove"
    override val delete = "Delete"
    override val confirm = "Confirm"
    override val approve = "Approve"
    override val decline = "Decline"
    override val accept = "Accept"
    override val reject = "Reject"
    override val send = "Send"
    override val share = "Share"
    override val copy = "Copy"
    override val copied = "Copied ✓"
    override val loading = "Loading…"
    override val retry = "Retry"
    override val yes = "Yes"
    override val no = "No"
    override val somethingWentWrong = "Something went wrong -- try again."
    override val tabDashboard = "Dashboard"
    override val tabSearch = "Search"
    override val tabSharing = "Sharing"
    override val tabSettings = "Settings"
    override val tabHome = "Home"
    override val tabDiscover = "Discover"
    override val tabMatches = "Matches"
    override val tabNotifications = "Notifications"
}

object CommonStringsHi : CommonStrings {
    override val save = "सेव करें"
    override val saveChanges = "बदलाव सेव करें"
    override val saving = "सेव हो रहा है…"
    override val saved = "सेव हो गया"
    override val cancel = "रद्द करें"
    override val back = "वापस"
    override val next = "अगला"
    override val continueLabel = "जारी रखें"
    override val close = "बंद करें"
    override val done = "हो गया"
    override val edit = "संपादित करें"
    override val remove = "हटाएं"
    override val delete = "मिटाएं"
    override val confirm = "पुष्टि करें"
    override val approve = "स्वीकृत करें"
    override val decline = "अस्वीकार करें"
    override val accept = "स्वीकार करें"
    override val reject = "अस्वीकार करें"
    override val send = "भेजें"
    override val share = "शेयर करें"
    override val copy = "कॉपी करें"
    override val copied = "कॉपी हो गया ✓"
    override val loading = "लोड हो रहा है…"
    override val retry = "फिर से प्रयास करें"
    override val yes = "हाँ"
    override val no = "नहीं"
    override val somethingWentWrong = "कुछ गलत हो गया -- फिर से प्रयास करें।"
    override val tabDashboard = "डैशबोर्ड"
    override val tabSearch = "खोजें"
    override val tabSharing = "शेयरिंग"
    override val tabSettings = "सेटिंग्स"
    override val tabHome = "होम"
    override val tabDiscover = "डिस्कवर"
    override val tabMatches = "मैचेस"
    override val tabNotifications = "सूचनाएं"
}

object CommonStringsTa : CommonStrings {
    override val save = "சேமிக்கவும்"
    override val saveChanges = "மாற்றங்களைச் சேமிக்கவும்"
    override val saving = "சேமிக்கப்படுகிறது…"
    override val saved = "சேமிக்கப்பட்டது"
    override val cancel = "ரத்துசெய்"
    override val back = "பின்செல்"
    override val next = "அடுத்து"
    override val continueLabel = "தொடரவும்"
    override val close = "மூடு"
    override val done = "முடிந்தது"
    override val edit = "திருத்து"
    override val remove = "அகற்று"
    override val delete = "நீக்கு"
    override val confirm = "உறுதிப்படுத்து"
    override val approve = "அங்கீகரி"
    override val decline = "நிராகரி"
    override val accept = "ஏற்றுக்கொள்"
    override val reject = "நிராகரி"
    override val send = "அனுப்பு"
    override val share = "பகிர்"
    override val copy = "நகலெடு"
    override val copied = "நகலெடுக்கப்பட்டது ✓"
    override val loading = "ஏற்றப்படுகிறது…"
    override val retry = "மீண்டும் முயற்சிக்கவும்"
    override val yes = "ஆம்"
    override val no = "இல்லை"
    override val somethingWentWrong = "ஏதோ தவறு நடந்தது -- மீண்டும் முயற்சிக்கவும்."
    override val tabDashboard = "டாஷ்போர்டு"
    override val tabSearch = "தேடு"
    override val tabSharing = "பகிர்வு"
    override val tabSettings = "அமைப்புகள்"
    override val tabHome = "முகப்பு"
    override val tabDiscover = "டிஸ்கவர்"
    override val tabMatches = "மேட்சஸ்"
    override val tabNotifications = "அறிவிப்புகள்"
}
