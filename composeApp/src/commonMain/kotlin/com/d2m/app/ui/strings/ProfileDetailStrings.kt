package com.d2m.app.ui.strings

/** ProfileDetailScreen.kt -- mirrors d2m_web's "profile_detail" namespace. */
interface ProfileDetailStrings {
    val back: String
    val errLoadProfile: String
    fun photographOf(name: String): String
    val noPhotographOnFile: String
    val astrology: String
    val preferences: String
    val seeWhoVouched: String
    fun whoVouchedFor(name: String): String
    val thisProfile: String
    val noMatchVisibleVouches: String
    val aFamilyMember: String
    val close: String
    val errLoadVouchers: String
    val errStartConversation: String
    val errCreateShareLink: String
    val suggestToChild: String
    val suggesting: String
    val messageTheirParent: String
    val opening: String
    val shareThisProfile: String
    val creatingLink: String
    val linkCopied: String
    val sendRequest: String
    val snooze: String
    val pass: String
}

object ProfileDetailStringsEn : ProfileDetailStrings {
    override val back = "← Back"
    override val errLoadProfile = "Couldn't load this profile."
    override fun photographOf(name: String) = "Photograph of $name"
    override val noPhotographOnFile = "No photograph on file"
    override val astrology = "Astrology"
    override val preferences = "Preferences"
    override val seeWhoVouched = "See who vouched"
    override fun whoVouchedFor(name: String) = "Who vouched for $name"
    override val thisProfile = "this profile"
    override val noMatchVisibleVouches = "No match-visible vouches to show."
    override val aFamilyMember = "A family member"
    override val close = "Close"
    override val errLoadVouchers = "Couldn't load who vouched for this profile."
    override val errStartConversation = "Couldn't start that conversation."
    override val errCreateShareLink = "Couldn't create a share link."
    override val suggestToChild = "Suggest to child"
    override val suggesting = "Suggesting…"
    override val messageTheirParent = "Message their parent"
    override val opening = "Opening…"
    override val shareThisProfile = "Share this profile"
    override val creatingLink = "Creating link…"
    override val linkCopied = "Link copied ✓"
    override val sendRequest = "Send request"
    override val snooze = "Snooze"
    override val pass = "Pass"
}

object ProfileDetailStringsHi : ProfileDetailStrings {
    override val back = "← वापस"
    override val errLoadProfile = "यह प्रोफ़ाइल लोड नहीं हो सकी।"
    override fun photographOf(name: String) = "$name की तस्वीर"
    override val noPhotographOnFile = "कोई तस्वीर उपलब्ध नहीं"
    override val astrology = "ज्योतिष"
    override val preferences = "पसंद"
    override val seeWhoVouched = "देखें किसने वाउच किया"
    override fun whoVouchedFor(name: String) = "$name के लिए किसने वाउच किया"
    override val thisProfile = "इस प्रोफ़ाइल"
    override val noMatchVisibleVouches = "दिखाने के लिए कोई मैच-विज़िबल वाउच नहीं हैं।"
    override val aFamilyMember = "एक परिवार सदस्य"
    override val close = "बंद करें"
    override val errLoadVouchers = "इस प्रोफ़ाइल के लिए किसने वाउच किया, लोड नहीं हो सका।"
    override val errStartConversation = "वह बातचीत शुरू नहीं हो सकी।"
    override val errCreateShareLink = "शेयर लिंक नहीं बनाया जा सका।"
    override val suggestToChild = "बच्चे को सुझाएं"
    override val suggesting = "सुझाया जा रहा है…"
    override val messageTheirParent = "उनके माता-पिता को मैसेज करें"
    override val opening = "खोला जा रहा है…"
    override val shareThisProfile = "यह प्रोफ़ाइल शेयर करें"
    override val creatingLink = "लिंक बनाया जा रहा है…"
    override val linkCopied = "लिंक कॉपी हो गया ✓"
    override val sendRequest = "रिक्वेस्ट भेजें"
    override val snooze = "स्नूज़ करें"
    override val pass = "छोड़ें"
}

object ProfileDetailStringsTa : ProfileDetailStrings {
    override val back = "← பின்செல்"
    override val errLoadProfile = "இந்த சுயவிவரத்தை ஏற்ற முடியவில்லை."
    override fun photographOf(name: String) = "$name இன் புகைப்படம்"
    override val noPhotographOnFile = "புகைப்படம் இல்லை"
    override val astrology = "ஜாதகம்"
    override val preferences = "விருப்பங்கள்"
    override val seeWhoVouched = "யார் வவுச் செய்தார்கள் என்று பார்"
    override fun whoVouchedFor(name: String) = "$name க்கு யார் வவுச் செய்தார்கள்"
    override val thisProfile = "இந்த சுயவிவரம்"
    override val noMatchVisibleVouches = "காட்ட பொருத்தம்-தெரியும் வவுச்கள் இல்லை."
    override val aFamilyMember = "ஒரு குடும்ப உறுப்பினர்"
    override val close = "மூடு"
    override val errLoadVouchers = "இந்த சுயவிவரத்திற்கு யார் வவுச் செய்தார்கள் என்பதை ஏற்ற முடியவில்லை."
    override val errStartConversation = "அந்த உரையாடலைத் தொடங்க முடியவில்லை."
    override val errCreateShareLink = "பகிர்வு இணைப்பை உருவாக்க முடியவில்லை."
    override val suggestToChild = "குழந்தைக்கு பரிந்துரை"
    override val suggesting = "பரிந்துரைக்கப்படுகிறது…"
    override val messageTheirParent = "அவர்களின் பெற்றோருக்கு செய்தி அனுப்பு"
    override val opening = "திறக்கப்படுகிறது…"
    override val shareThisProfile = "இந்த சுயவிவரத்தைப் பகிரவும்"
    override val creatingLink = "இணைப்பு உருவாக்கப்படுகிறது…"
    override val linkCopied = "இணைப்பு நகலெடுக்கப்பட்டது ✓"
    override val sendRequest = "கோரிக்கை அனுப்பு"
    override val snooze = "ஸ்னூஸ் செய்"
    override val pass = "தவிர்"
}
