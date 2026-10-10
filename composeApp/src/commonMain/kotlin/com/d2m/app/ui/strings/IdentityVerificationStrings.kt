package com.d2m.app.ui.strings

/** IdentityVerificationScreen.kt -- mirrors d2m_web's "trust" namespace's Verification tab. */
interface IdentityVerificationStrings {
    val pageTitle: String
    val loadError: String

    val statusPending: String
    val pendingNote: String
    val statusApproved: String
    val approvedNote: String
    fun rejectedWithReason(reason: String): String
    val rejectedNoReason: String

    val documentTypeLabel: String
    val documentTypePassport: String
    val documentTypeDriversLicense: String
    val documentTypeNationalId: String
    val documentTypeOther: String
    val documentFileLabel: String
    val selfieFileLabel: String
    val choosePhoto: String
    val changePhoto: String
    val submit: String
    val submitting: String
    val submitError: String
    val back: String
}

object IdentityVerificationStringsEn : IdentityVerificationStrings {
    override val pageTitle = "Verification"
    override val loadError = "Couldn't load your verification status."

    override val statusPending = "PENDING REVIEW"
    override val pendingNote = "Submitted -- an admin will review this shortly."
    override val statusApproved = "VERIFIED"
    override val approvedNote = "This profile now shows a Verified badge."
    override fun rejectedWithReason(reason: String) = "Not approved: $reason"
    override val rejectedNoReason = "Not approved. Submit again with a clearer document."

    override val documentTypeLabel = "Document type"
    override val documentTypePassport = "Passport"
    override val documentTypeDriversLicense = "Driver's license"
    override val documentTypeNationalId = "National ID"
    override val documentTypeOther = "Other government photo ID"
    override val documentFileLabel = "Photo of your ID"
    override val selfieFileLabel = "Selfie"
    override val choosePhoto = "Choose photo"
    override val changePhoto = "Change photo"
    override val submit = "Submit for review"
    override val submitting = "Submitting…"
    override val submitError = "Couldn't submit that -- try again."
    override val back = "Back"
}

object IdentityVerificationStringsHi : IdentityVerificationStrings {
    override val pageTitle = "सत्यापन"
    override val loadError = "आपकी सत्यापन स्थिति लोड नहीं हो सकी।"

    override val statusPending = "समीक्षा लंबित"
    override val pendingNote = "भेज दिया गया -- एक एडमिन जल्द ही इसकी समीक्षा करेगा।"
    override val statusApproved = "सत्यापित"
    override val approvedNote = "यह प्रोफ़ाइल अब सत्यापित बैज दिखाती है।"
    override fun rejectedWithReason(reason: String) = "स्वीकृत नहीं: $reason"
    override val rejectedNoReason = "स्वीकृत नहीं हुआ। एक स्पष्ट दस्तावेज़ के साथ फिर से भेजें।"

    override val documentTypeLabel = "दस्तावेज़ का प्रकार"
    override val documentTypePassport = "पासपोर्ट"
    override val documentTypeDriversLicense = "ड्राइविंग लाइसेंस"
    override val documentTypeNationalId = "राष्ट्रीय पहचान पत्र"
    override val documentTypeOther = "अन्य सरकारी फोटो पहचान पत्र"
    override val documentFileLabel = "आपके ID की फ़ोटो"
    override val selfieFileLabel = "सेल्फी"
    override val choosePhoto = "फ़ोटो चुनें"
    override val changePhoto = "फ़ोटो बदलें"
    override val submit = "समीक्षा के लिए भेजें"
    override val submitting = "भेजा जा रहा है…"
    override val submitError = "वह भेजा नहीं जा सका -- फिर प्रयास करें।"
    override val back = "वापस"
}

object IdentityVerificationStringsTa : IdentityVerificationStrings {
    override val pageTitle = "சரிபார்ப்பு"
    override val loadError = "உங்கள் சரிபார்ப்பு நிலையை ஏற்ற முடியவில்லை."

    override val statusPending = "மதிப்பாய்வு நிலுவையில்"
    override val pendingNote = "சமர்ப்பிக்கப்பட்டது -- ஒரு நிர்வாகி விரைவில் இதை மதிப்பாய்வு செய்வார்."
    override val statusApproved = "சரிபார்க்கப்பட்டது"
    override val approvedNote = "இந்த சுயவிவரம் இப்போது சரிபார்க்கப்பட்ட பேட்ஜைக் காட்டுகிறது."
    override fun rejectedWithReason(reason: String) = "ஒப்புதல் இல்லை: $reason"
    override val rejectedNoReason = "ஒப்புதல் இல்லை. தெளிவான ஆவணத்துடன் மீண்டும் சமர்ப்பிக்கவும்."

    override val documentTypeLabel = "ஆவண வகை"
    override val documentTypePassport = "பாஸ்போர்ட்"
    override val documentTypeDriversLicense = "ஓட்டுநர் உரிமம்"
    override val documentTypeNationalId = "தேசிய அடையாள அட்டை"
    override val documentTypeOther = "மற்ற அரசு புகைப்பட அடையாளம்"
    override val documentFileLabel = "உங்கள் ஐடியின் புகைப்படம்"
    override val selfieFileLabel = "செல்ஃபி"
    override val choosePhoto = "புகைப்படத்தைத் தேர்ந்தெடுக்கவும்"
    override val changePhoto = "புகைப்படத்தை மாற்று"
    override val submit = "மதிப்பாய்வுக்கு சமர்ப்பிக்கவும்"
    override val submitting = "சமர்ப்பிக்கிறது…"
    override val submitError = "அது சமர்ப்பிக்கப்படவில்லை -- மீண்டும் முயற்சிக்கவும்."
    override val back = "பின்செல்"
}
