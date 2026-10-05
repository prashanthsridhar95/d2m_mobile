package com.d2m.app.ui.strings

/**
 * The four Phase-4 web-parity screens under ui/screens/parity/
 * (AdminGalleryModerationScreen, BrowseTable, PanchangamCalendarScreen,
 * SuccessGalleryScreen) -- small, unrelated screens grouped into one
 * namespace file since each has only a handful of strings. Reuses
 * d2m_web's "public_share" (moderation_ and gallery_ prefixed keys) and
 * "profile_detail" (panch_ prefixed keys) translations where the concept
 * matches exactly.
 */
interface ParityStrings {
    // AdminGalleryModerationScreen
    val moderationTitle: String
    val moderationSubtitle: String
    val entryIdLabel: String
    val publishCta: String
    val published: String
    val errPublish: String

    // BrowseTable
    val colName: String
    val colAge: String
    val colCity: String
    val colOccupation: String
    val colGothram: String
    val colSect: String
    val colHeight: String
    val colScore: String

    // PanchangamCalendarScreen
    val panchangamTitle: String
    val errLoadPanchangam: String
    val tithiLabel: String
    val nakshatraLabel: String
    val yogaLabel: String
    val karanaLabel: String
    val rahuKalamLabel: String

    // SuccessGalleryScreen
    val successStoriesTitle: String
    val errLoadGallery: String
    val nothingPublishedYet: String
}

object ParityStringsEn : ParityStrings {
    override val moderationTitle = "Gallery moderation"
    override val moderationSubtitle = "Not a real queue -- publish a specific entry id, same as the web tool."
    override val entryIdLabel = "Entry id"
    override val publishCta = "Publish"
    override val published = "Published."
    override val errPublish = "Couldn't publish that entry."
    override val colName = "Name"
    override val colAge = "Age"
    override val colCity = "City"
    override val colOccupation = "Occupation"
    override val colGothram = "Gothram"
    override val colSect = "Sect"
    override val colHeight = "Height"
    override val colScore = "Score"
    override val panchangamTitle = "Panchangam"
    override val errLoadPanchangam = "Couldn't load the panchangam."
    override val tithiLabel = "Tithi"
    override val nakshatraLabel = "Nakshatra"
    override val yogaLabel = "Yoga"
    override val karanaLabel = "Karana"
    override val rahuKalamLabel = "Rahu Kalam"
    override val successStoriesTitle = "Success stories"
    override val errLoadGallery = "Couldn't load the gallery."
    override val nothingPublishedYet = "No stories published yet."
}

object ParityStringsHi : ParityStrings {
    override val moderationTitle = "गैलरी मॉडरेशन"
    override val moderationSubtitle = "यह असली कतार नहीं है -- वेब टूल की तरह एक खास entry id प्रकाशित करें।"
    override val entryIdLabel = "एंट्री आईडी"
    override val publishCta = "प्रकाशित करें"
    override val published = "प्रकाशित हो गया।"
    override val errPublish = "वह एंट्री प्रकाशित नहीं हो सकी।"
    override val colName = "नाम"
    override val colAge = "आयु"
    override val colCity = "शहर"
    override val colOccupation = "व्यवसाय"
    override val colGothram = "गोत्र"
    override val colSect = "संप्रदाय"
    override val colHeight = "ऊंचाई"
    override val colScore = "स्कोर"
    override val panchangamTitle = "पंचांग"
    override val errLoadPanchangam = "पंचांग लोड नहीं हो सका।"
    override val tithiLabel = "तिथि"
    override val nakshatraLabel = "नक्षत्र"
    override val yogaLabel = "योग"
    override val karanaLabel = "करण"
    override val rahuKalamLabel = "राहु काल"
    override val successStoriesTitle = "सफलता की कहानियां"
    override val errLoadGallery = "गैलरी लोड नहीं हो सकी।"
    override val nothingPublishedYet = "अभी तक कोई कहानी प्रकाशित नहीं हुई।"
}

object ParityStringsTa : ParityStrings {
    override val moderationTitle = "கேலரி மேற்பார்வை"
    override val moderationSubtitle = "இது உண்மையான வரிசை அல்ல -- வெப் கருவியைப் போலவே ஒரு குறிப்பிட்ட entry id ஐ வெளியிடவும்."
    override val entryIdLabel = "பதிவு ஐடி"
    override val publishCta = "வெளியிடவும்"
    override val published = "வெளியிடப்பட்டது."
    override val errPublish = "அந்த பதிவை வெளியிட முடியவில்லை."
    override val colName = "பெயர்"
    override val colAge = "வயது"
    override val colCity = "நகரம்"
    override val colOccupation = "தொழில்"
    override val colGothram = "கோத்திரம்"
    override val colSect = "பிரிவு"
    override val colHeight = "உயரம்"
    override val colScore = "மதிப்பெண்"
    override val panchangamTitle = "பஞ்சாங்கம்"
    override val errLoadPanchangam = "பஞ்சாங்கத்தை ஏற்ற முடியவில்லை."
    override val tithiLabel = "திதி"
    override val nakshatraLabel = "நட்சத்திரம்"
    override val yogaLabel = "யோகம்"
    override val karanaLabel = "கரணம்"
    override val rahuKalamLabel = "ராகு காலம்"
    override val successStoriesTitle = "வெற்றிக் கதைகள்"
    override val errLoadGallery = "கேலரியை ஏற்ற முடியவில்லை."
    override val nothingPublishedYet = "இதுவரை கதைகள் வெளியிடப்படவில்லை."
}
