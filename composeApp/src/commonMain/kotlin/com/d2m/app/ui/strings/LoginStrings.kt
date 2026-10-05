package com.d2m.app.ui.strings

/** LoginScreen.kt + RolePickerScreen.kt -- mirrors d2m_web's "login" i18n namespace (src/locales/ login.json per language), reusing its existing Hindi/Tamil wording wherever the same English concept appears there, so the two clients read as one product in either language. */
interface LoginStrings {
    val tagline: String
    val tabLogIn: String
    val tabRegister: String
    val resolving: String
    val logInWithWedlock: String
    val emailLabel: String
    val passwordLabel: String
    val logIn: String
    val logInBusy: String
    val or: String
    val continueWithGoogle: String
    val signingIn: String
    val whichProfile: String
    val imAParent: String
    val imTheChild: String
    val sponsorIdLabel: String
    val primaryIdLabel: String
    val continueAction: String
    val checking: String
    fun loggingInAsConfirm(name: String): String
    val confirm: String
    val notMe: String
    val setUpAsParent: String
    val sendCode: String
    val sendCodeBusy: String
    val signUpWithGoogle: String
    fun otpSentTo(email: String): String
    val verificationCodeLabel: String
    val verify: String
    val verifyBusy: String
    val back: String
    val passwordHint: String
    val createAccount: String
    val createAccountBusy: String
    val errLoginFailed: String
    val errIdNotFound: String
    val errSendCodeFailed: String
    val errOtpInvalid: String
    val errCreateAccountFailed: String
    val errGoogleSignIn: String

    // RolePickerScreen.kt
    val whoIsThisFor: String
    val roleParentTitle: String
    val roleParentSubtitle: String
    val roleLinkTitle: String
    val roleLinkSubtitle: String
}

object LoginStringsEn : LoginStrings {
    override val tagline = "Every union begins with the stars."
    override val tabLogIn = "Log in"
    override val tabRegister = "Register"
    override val resolving = "Signed in. Finding your account…"
    override val logInWithWedlock = "Log in with your WedLock account."
    override val emailLabel = "Email"
    override val passwordLabel = "Password"
    override val logIn = "Log in"
    override val logInBusy = "Logging in…"
    override val or = "or"
    override val continueWithGoogle = "Continue with Google"
    override val signingIn = "Signing in…"
    override val whichProfile = "Which profile do you want to open?"
    override val imAParent = "I'm a parent"
    override val imTheChild = "I'm the child"
    override val sponsorIdLabel = "Sponsor id"
    override val primaryIdLabel = "Primary id"
    override val continueAction = "Continue"
    override val checking = "Checking…"
    override fun loggingInAsConfirm(name: String) = "Logging in as $name — confirm?"
    override val confirm = "Confirm"
    override val notMe = "Not me"
    override val setUpAsParent = "Set up your account as a parent."
    override val sendCode = "Send code"
    override val sendCodeBusy = "Sending code…"
    override val signUpWithGoogle = "Sign up with Google"
    override fun otpSentTo(email: String) = "We sent a 6-digit code to $email."
    override val verificationCodeLabel = "Verification code"
    override val verify = "Verify"
    override val verifyBusy = "Verifying…"
    override val back = "Back"
    override val passwordHint = "At least 8 characters."
    override val createAccount = "Create account"
    override val createAccountBusy = "Creating account…"
    override val errLoginFailed = "Couldn't log in. Check your email and password."
    override val errIdNotFound = "Couldn't find that id. Double check and try again."
    override val errSendCodeFailed = "Couldn't send a code to that email."
    override val errOtpInvalid = "That code didn't check out."
    override val errCreateAccountFailed = "Couldn't create that account."
    override val errGoogleSignIn = "Couldn't sign in with Google."

    override val whoIsThisFor = "Who's this for?"
    override val roleParentTitle = "I'm a parent, signing up for my child"
    override val roleParentSubtitle = "Set up a profile, vet matches, and guide the process."
    override val roleLinkTitle = "I have a shared link"
    override val roleLinkSubtitle = "Claim the profile your parent started for you."
}

object LoginStringsHi : LoginStrings {
    override val tagline = "हर मिलन सितारों से शुरू होता है।"
    override val tabLogIn = "लॉगिन"
    override val tabRegister = "रजिस्टर करें"
    override val resolving = "साइन इन हो गया। आपका खाता खोजा जा रहा है…"
    override val logInWithWedlock = "अपने WedLock खाते से लॉगिन करें।"
    override val emailLabel = "ईमेल"
    override val passwordLabel = "पासवर्ड"
    override val logIn = "लॉगिन करें"
    override val logInBusy = "लॉगिन हो रहा है…"
    override val or = "या"
    override val continueWithGoogle = "Google के साथ जारी रखें"
    override val signingIn = "साइन इन हो रहा है…"
    override val whichProfile = "आप कौन सी प्रोफ़ाइल खोलना चाहते हैं?"
    override val imAParent = "मैं पैरेंट हूं"
    override val imTheChild = "मैं चाइल्ड हूं"
    override val sponsorIdLabel = "स्पॉन्सर आईडी"
    override val primaryIdLabel = "प्राइमरी आईडी"
    override val continueAction = "जारी रखें"
    override val checking = "जांच रहे हैं…"
    override fun loggingInAsConfirm(name: String) = "$name के रूप में लॉगिन कर रहे हैं — पुष्टि करें?"
    override val confirm = "पुष्टि करें"
    override val notMe = "यह मैं नहीं हूं"
    override val setUpAsParent = "पैरेंट के रूप में अपना खाता सेट करें।"
    override val sendCode = "कोड भेजें"
    override val sendCodeBusy = "कोड भेजा जा रहा है…"
    override val signUpWithGoogle = "Google से साइन अप करें"
    override fun otpSentTo(email: String) = "हमने $email पर 6-अंकों का कोड भेजा है।"
    override val verificationCodeLabel = "सत्यापन कोड"
    override val verify = "सत्यापित करें"
    override val verifyBusy = "सत्यापित हो रहा है…"
    override val back = "वापस"
    override val passwordHint = "कम से कम 8 अक्षर।"
    override val createAccount = "खाता बनाएं"
    override val createAccountBusy = "खाता बनाया जा रहा है…"
    override val errLoginFailed = "लॉगिन नहीं हो सका। अपना ईमेल और पासवर्ड जांचें।"
    override val errIdNotFound = "वह आईडी नहीं मिली। फिर से जांचें और प्रयास करें।"
    override val errSendCodeFailed = "उस ईमेल पर कोड नहीं भेजा जा सका।"
    override val errOtpInvalid = "वह कोड सही नहीं निकला।"
    override val errCreateAccountFailed = "वह खाता नहीं बनाया जा सका।"
    override val errGoogleSignIn = "Google से साइन इन नहीं हो सका।"

    override val whoIsThisFor = "यह किसके लिए है?"
    override val roleParentTitle = "मैं एक पैरेंट हूं, अपने बच्चे के लिए साइन अप कर रहा/रही हूं"
    override val roleParentSubtitle = "प्रोफ़ाइल सेट करें, मैच जांचें, और प्रक्रिया को आगे बढ़ाएं।"
    override val roleLinkTitle = "मेरे पास एक शेयर किया हुआ लिंक है"
    override val roleLinkSubtitle = "अपने पैरेंट द्वारा शुरू की गई प्रोफ़ाइल को क्लेम करें।"
}

object LoginStringsTa : LoginStrings {
    override val tagline = "ஒவ்வொரு இணைப்பும் நட்சத்திரங்களுடன் தொடங்குகிறது."
    override val tabLogIn = "உள்நுழைவு"
    override val tabRegister = "பதிவு செய்யவும்"
    override val resolving = "உள்நுழைந்துவிட்டீர்கள். உங்கள் கணக்கைக் கண்டறிகிறோம்…"
    override val logInWithWedlock = "உங்கள் WedLock கணக்கில் உள்நுழையவும்."
    override val emailLabel = "மின்னஞ்சல்"
    override val passwordLabel = "கடவுச்சொல்"
    override val logIn = "உள்நுழையவும்"
    override val logInBusy = "உள்நுழைகிறோம்…"
    override val or = "அல்லது"
    override val continueWithGoogle = "Google மூலம் தொடரவும்"
    override val signingIn = "உள்நுழைகிறோம்…"
    override val whichProfile = "எந்த சுயவிவரத்தைத் திறக்க விரும்புகிறீர்கள்?"
    override val imAParent = "நான் பெற்றோர்"
    override val imTheChild = "நான் குழந்தை"
    override val sponsorIdLabel = "ஸ்பான்சர் ஐடி"
    override val primaryIdLabel = "பிரைமரி ஐடி"
    override val continueAction = "தொடரவும்"
    override val checking = "சரிபார்க்கிறோம்…"
    override fun loggingInAsConfirm(name: String) = "$name ஆக உள்நுழைகிறீர்கள் — உறுதிசெய்யவா?"
    override val confirm = "உறுதிசெய்"
    override val notMe = "இது நான் இல்லை"
    override val setUpAsParent = "பெற்றோராக உங்கள் கணக்கை அமைக்கவும்."
    override val sendCode = "குறியீட்டை அனுப்பவும்"
    override val sendCodeBusy = "குறியீடு அனுப்பப்படுகிறது…"
    override val signUpWithGoogle = "Google மூலம் பதிவு செய்யவும்"
    override fun otpSentTo(email: String) = "$email க்கு 6-இலக்க குறியீட்டை அனுப்பியுள்ளோம்."
    override val verificationCodeLabel = "சரிபார்ப்புக் குறியீடு"
    override val verify = "சரிபார்க்கவும்"
    override val verifyBusy = "சரிபார்க்கிறோம்…"
    override val back = "பின்செல்"
    override val passwordHint = "குறைந்தது 8 எழுத்துகள்."
    override val createAccount = "கணக்கை உருவாக்கவும்"
    override val createAccountBusy = "கணக்கு உருவாக்கப்படுகிறது…"
    override val errLoginFailed = "உள்நுழைய முடியவில்லை. உங்கள் மின்னஞ்சல் மற்றும் கடவுச்சொல்லைச் சரிபார்க்கவும்."
    override val errIdNotFound = "அந்த ஐடி கிடைக்கவில்லை. மீண்டும் சரிபார்த்து முயற்சிக்கவும்."
    override val errSendCodeFailed = "அந்த மின்னஞ்சலுக்கு குறியீட்டை அனுப்ப முடியவில்லை."
    override val errOtpInvalid = "அந்தக் குறியீடு சரியில்லை."
    override val errCreateAccountFailed = "அந்தக் கணக்கை உருவாக்க முடியவில்லை."
    override val errGoogleSignIn = "Google மூலம் உள்நுழைய முடியவில்லை."

    override val whoIsThisFor = "இது யாருக்காக?"
    override val roleParentTitle = "நான் ஒரு பெற்றோர், என் குழந்தைக்காக பதிவு செய்கிறேன்"
    override val roleParentSubtitle = "சுயவிவரத்தை அமைக்கவும், பொருத்தங்களைச் சரிபார்க்கவும், செயல்முறையை வழிநடத்தவும்."
    override val roleLinkTitle = "எனக்கு ஒரு பகிரப்பட்ட இணைப்பு உள்ளது"
    override val roleLinkSubtitle = "உங்கள் பெற்றோர் தொடங்கிய சுயவிவரத்தைப் பெறுங்கள்."
}
