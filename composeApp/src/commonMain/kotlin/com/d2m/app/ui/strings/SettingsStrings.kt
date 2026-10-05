package com.d2m.app.ui.strings

/** SettingsScreen.kt -- mirrors d2m_web's "settings" namespace where concepts overlap. */
interface SettingsStrings {
    val pageTitle: String
    val muteNotificationsTitle: String
    val muteNotificationsSubtitle: String
    val shareMyProfile: String
    val trustAndVouches: String
    val logOut: String
    val familyLinkTitle: String
    val familyLinkUnlinkedNotice: String
    val familyLinkUnlinkDescParent: String
    val familyLinkUnlinkDescChild: String
    val unlinkFamilyAccount: String
    val confirmWithPassword: String
    val unlinkModalSubtitle: String
    val passwordLabel: String
    val cancel: String
    val unlinking: String
    val unlink: String
    val errUnlink: String
    val copied: String
    val languageTitle: String
    val languageHint: String
}

object SettingsStringsEn : SettingsStrings {
    override val pageTitle = "Settings"
    override val muteNotificationsTitle = "Mute notifications"
    override val muteNotificationsSubtitle = "Turn off all push and in-app alerts"
    override val shareMyProfile = "Share my profile"
    override val trustAndVouches = "Trust & vouches"
    override val logOut = "Log out"
    override val familyLinkTitle = "Family link"
    override val familyLinkUnlinkedNotice = "This account is no longer linked to a family member. Log out and back in to refresh the app."
    override val familyLinkUnlinkDescParent = "Removes the family link between you and your child's account. Your child's profile and data stay intact, just no longer paired with you."
    override val familyLinkUnlinkDescChild = "Removes the family link between you and your parent's account. Your own profile and data stay intact, just no longer paired with them."
    override val unlinkFamilyAccount = "Unlink family account"
    override val confirmWithPassword = "Confirm with your password"
    override val unlinkModalSubtitle = "This permanently severs the family link -- re-enter your password to continue."
    override val passwordLabel = "Password"
    override val cancel = "Cancel"
    override val unlinking = "Unlinking…"
    override val unlink = "Unlink"
    override val errUnlink = "Couldn't unlink right now."
    override val copied = "Copied"
    override val languageTitle = "Language"
    override val languageHint = "Changes the app's language everywhere -- menus, buttons, and messages."
}

object SettingsStringsHi : SettingsStrings {
    override val pageTitle = "सेटिंग्स"
    override val muteNotificationsTitle = "सूचनाएं म्यूट करें"
    override val muteNotificationsSubtitle = "सभी पुश और इन-ऐप अलर्ट बंद करें"
    override val shareMyProfile = "मेरी प्रोफ़ाइल शेयर करें"
    override val trustAndVouches = "विश्वास और वाउच"
    override val logOut = "लॉग आउट करें"
    override val familyLinkTitle = "पारिवारिक लिंक"
    override val familyLinkUnlinkedNotice = "यह खाता अब किसी परिवार के सदस्य से लिंक नहीं है। ऐप रिफ्रेश करने के लिए लॉग आउट करें और फिर से लॉगिन करें।"
    override val familyLinkUnlinkDescParent = "आपके और आपके बच्चे के खाते के बीच पारिवारिक लिंक हटाता है। आपके बच्चे की प्रोफ़ाइल और डेटा सुरक्षित रहते हैं, बस अब आपसे जुड़े नहीं रहेंगे।"
    override val familyLinkUnlinkDescChild = "आपके और आपके माता-पिता के खाते के बीच पारिवारिक लिंक हटाता है। आपकी अपनी प्रोफ़ाइल और डेटा सुरक्षित रहते हैं, बस अब उनसे जुड़े नहीं रहेंगे।"
    override val unlinkFamilyAccount = "पारिवारिक खाता अनलिंक करें"
    override val confirmWithPassword = "अपने पासवर्ड से पुष्टि करें"
    override val unlinkModalSubtitle = "यह पारिवारिक लिंक को स्थायी रूप से समाप्त करता है -- जारी रखने के लिए अपना पासवर्ड फिर से डालें।"
    override val passwordLabel = "पासवर्ड"
    override val cancel = "रद्द करें"
    override val unlinking = "अनलिंक हो रहा है…"
    override val unlink = "अनलिंक करें"
    override val errUnlink = "अभी अनलिंक नहीं किया जा सका।"
    override val copied = "कॉपी हो गया"
    override val languageTitle = "भाषा"
    override val languageHint = "यह ऐप की भाषा हर जगह बदल देता है -- मेनू, बटन और संदेश।"
}

object SettingsStringsTa : SettingsStrings {
    override val pageTitle = "அமைப்புகள்"
    override val muteNotificationsTitle = "அறிவிப்புகளை முடக்கு"
    override val muteNotificationsSubtitle = "அனைத்து புஷ் மற்றும் இன்-ஆப் அலர்ட்களையும் அணைக்கவும்"
    override val shareMyProfile = "என் சுயவிவரத்தைப் பகிரவும்"
    override val trustAndVouches = "நம்பிக்கை & வவுச்கள்"
    override val logOut = "வெளியேறு"
    override val familyLinkTitle = "குடும்ப இணைப்பு"
    override val familyLinkUnlinkedNotice = "இந்தக் கணக்கு இனி ஒரு குடும்ப உறுப்பினருடன் இணைக்கப்படவில்லை. ஆப்பைப் புதுப்பிக்க வெளியேறி மீண்டும் உள்நுழையவும்."
    override val familyLinkUnlinkDescParent = "உங்களுக்கும் உங்கள் குழந்தையின் கணக்கிற்கும் இடையிலான குடும்ப இணைப்பை அகற்றுகிறது. உங்கள் குழந்தையின் சுயவிவரமும் தரவும் அப்படியே இருக்கும், இனி உங்களுடன் இணைக்கப்படாது."
    override val familyLinkUnlinkDescChild = "உங்களுக்கும் உங்கள் பெற்றோரின் கணக்கிற்கும் இடையிலான குடும்ப இணைப்பை அகற்றுகிறது. உங்கள் சொந்த சுயவிவரமும் தரவும் அப்படியே இருக்கும், இனி அவர்களுடன் இணைக்கப்படாது."
    override val unlinkFamilyAccount = "குடும்பக் கணக்கை இணைப்பு நீக்கு"
    override val confirmWithPassword = "உங்கள் கடவுச்சொல்லுடன் உறுதிப்படுத்தவும்"
    override val unlinkModalSubtitle = "இது குடும்ப இணைப்பை நிரந்தரமாக துண்டிக்கும் -- தொடர உங்கள் கடவுச்சொல்லை மீண்டும் உள்ளிடவும்."
    override val passwordLabel = "கடவுச்சொல்"
    override val cancel = "ரத்துசெய்"
    override val unlinking = "இணைப்பு நீக்கப்படுகிறது…"
    override val unlink = "இணைப்பை நீக்கு"
    override val errUnlink = "இப்போது இணைப்பை நீக்க முடியவில்லை."
    override val copied = "நகலெடுக்கப்பட்டது"
    override val languageTitle = "மொழி"
    override val languageHint = "இது ஆப்பின் மொழியை எல்லா இடங்களிலும் மாற்றும் -- மெனுக்கள், பொத்தான்கள் மற்றும் செய்திகள்."
}
