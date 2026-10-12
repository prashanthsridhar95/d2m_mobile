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
}
