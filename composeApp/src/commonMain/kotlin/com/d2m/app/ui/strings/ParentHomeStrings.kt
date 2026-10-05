package com.d2m.app.ui.strings

/** ParentHomeScreen.kt -- mirrors d2m_web's "home_parent" namespace where concepts overlap. */
interface ParentHomeStrings {
    val errLoadDashboard: String
    val pageTitle: String
    val yourChild: String
    val profileComplete: String
    val profileIncomplete: String
    fun statusLabel(status: String): String
    fun unreadCount(count: Int): String
    val suggestedForYouToReview: String
    fun suggestionsCount(count: Int): String
    val shortlisted: String
}

object ParentHomeStringsEn : ParentHomeStrings {
    override val errLoadDashboard = "Couldn't load your dashboard."
    override val pageTitle = "Your child's matches"
    override val yourChild = "Your child"
    override val profileComplete = "Profile complete"
    override val profileIncomplete = "Profile incomplete"
    override fun statusLabel(status: String) = "Status: $status"
    override fun unreadCount(count: Int) = "$count unread"
    override val suggestedForYouToReview = "Suggested for you to review"
    override fun suggestionsCount(count: Int) = "$count suggestions"
    override val shortlisted = "Shortlisted"
}

object ParentHomeStringsHi : ParentHomeStrings {
    override val errLoadDashboard = "आपका डैशबोर्ड लोड नहीं हो सका।"
    override val pageTitle = "आपके बच्चे के मैच"
    override val yourChild = "आपका बच्चा"
    override val profileComplete = "प्रोफ़ाइल पूरी है"
    override val profileIncomplete = "प्रोफ़ाइल अधूरी है"
    override fun statusLabel(status: String) = "स्थिति: $status"
    override fun unreadCount(count: Int) = "$count अपठित"
    override val suggestedForYouToReview = "आपके देखने के लिए सुझाव"
    override fun suggestionsCount(count: Int) = "$count सुझाव"
    override val shortlisted = "शॉर्टलिस्ट की गई"
}

object ParentHomeStringsTa : ParentHomeStrings {
    override val errLoadDashboard = "உங்கள் டாஷ்போர்டை ஏற்ற முடியவில்லை."
    override val pageTitle = "உங்கள் குழந்தையின் மேட்சஸ்"
    override val yourChild = "உங்கள் குழந்தை"
    override val profileComplete = "சுயவிவரம் முழுமையானது"
    override val profileIncomplete = "சுயவிவரம் முழுமையடையவில்லை"
    override fun statusLabel(status: String) = "நிலை: $status"
    override fun unreadCount(count: Int) = "$count படிக்காதவை"
    override val suggestedForYouToReview = "நீங்கள் பார்வையிட பரிந்துரைக்கப்பட்டவை"
    override fun suggestionsCount(count: Int) = "$count பரிந்துரைகள்"
    override val shortlisted = "ஷார்ட்லிஸ்ட் செய்யப்பட்டவை"
}
