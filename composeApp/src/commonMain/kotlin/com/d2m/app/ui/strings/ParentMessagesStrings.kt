package com.d2m.app.ui.strings

/** ParentMessagesScreen.kt -- parent-to-parent chat, mirrors d2m_web's "messages" namespace where concepts overlap. */
interface ParentMessagesStrings {
    val notSignedInTitle: String
    val notSignedInBody: String
    val pageTitle: String
    val noConversationsTitle: String
    val noConversationsBody: String
    val noMessagesYet: String
    val messageDeleted: String
    val sentAnAttachment: String
    val typing: String
    val online: String
    val offline: String
    val backToMessages: String
    val audioCall: String
    val videoCall: String
}

object ParentMessagesStringsEn : ParentMessagesStrings {
    override val notSignedInTitle = "Not signed in"
    override val notSignedInBody = "Log in as a parent to see your messages."
    override val pageTitle = "Messages"
    override val noConversationsTitle = "No conversations yet"
    override val noConversationsBody = "Open a profile and use \"Message their parent\" to start one."
    override val noMessagesYet = "No messages yet"
    override val messageDeleted = "This message was deleted"
    override val sentAnAttachment = "Sent an attachment"
    override val typing = "typing…"
    override val online = "Online"
    override val offline = "Offline"
    override val backToMessages = "Back to messages"
    override val audioCall = "Audio call"
    override val videoCall = "Video call"
}

object ParentMessagesStringsHi : ParentMessagesStrings {
    override val notSignedInTitle = "साइन इन नहीं है"
    override val notSignedInBody = "अपने संदेश देखने के लिए पैरेंट के रूप में लॉगिन करें।"
    override val pageTitle = "संदेश"
    override val noConversationsTitle = "अभी कोई बातचीत नहीं"
    override val noConversationsBody = "किसी प्रोफ़ाइल को खोलें और \"उनके माता-पिता को मैसेज करें\" का उपयोग करें।"
    override val noMessagesYet = "अभी कोई संदेश नहीं"
    override val messageDeleted = "यह संदेश हटा दिया गया था"
    override val sentAnAttachment = "एक अटैचमेंट भेजा"
    override val typing = "लिख रहे हैं…"
    override val online = "ऑनलाइन"
    override val offline = "ऑफ़लाइन"
    override val backToMessages = "संदेशों पर वापस जाएं"
    override val audioCall = "ऑडियो कॉल"
    override val videoCall = "वीडियो कॉल"
}

object ParentMessagesStringsTa : ParentMessagesStrings {
    override val notSignedInTitle = "உள்நுழையவில்லை"
    override val notSignedInBody = "உங்கள் செய்திகளைப் பார்க்க பெற்றோராக உள்நுழையவும்."
    override val pageTitle = "செய்திகள்"
    override val noConversationsTitle = "இன்னும் உரையாடல்கள் இல்லை"
    override val noConversationsBody = "ஒரு சுயவிவரத்தைத் திறந்து \"அவர்களின் பெற்றோருக்கு செய்தி அனுப்பு\" என்பதைப் பயன்படுத்தவும்."
    override val noMessagesYet = "இன்னும் செய்திகள் இல்லை"
    override val messageDeleted = "இந்தச் செய்தி நீக்கப்பட்டது"
    override val sentAnAttachment = "ஒரு இணைப்பை அனுப்பியது"
    override val typing = "தட்டச்சு செய்கிறார்…"
    override val online = "ஆன்லைனில்"
    override val offline = "ஆஃப்லைனில்"
    override val backToMessages = "செய்திகளுக்குத் திரும்பு"
    override val audioCall = "ஆடியோ அழைப்பு"
    override val videoCall = "வீடியோ அழைப்பு"
}
