package com.d2m.app.ui.strings

/** OnboardingWizardScreen.kt + HandoffScreen.kt + ClaimFlowScreen.kt -- mirrors d2m_web's "onboarding" namespace. */
interface OnboardingStrings {
    // OnboardingWizardScreen
    val setUpProfile: String
    val stepAboutYou: String
    val stepAboutThem: String
    val stepBirthDetails: String
    val stepPreferences: String
    val stepReview: String
    val aboutYouHeading: String
    val yourName: String
    val yourRelationship: String
    val relationshipMother: String
    val relationshipFather: String
    val relationshipGuardian: String
    val relationshipOther: String
    val aboutChildHeading: String
    val name: String
    val gender: String
    val seeking: String
    val whenWasSheBorn: String
    val dateLabel: String
    val timeLabel: String
    val birthPlace: String
    val noCoordinatesWarning: String
    val moreDetailsHeading: String
    val religion: String
    val yourCommunity: String
    val acceptableReligions: String
    val minAge: String
    val maxAge: String
    val acceptableLocations: String
    val acceptableMaritalStatus: String
    val beforeWeSave: String
    val reviewBorn: String
    val reviewMatchAgeRange: String
    val back: String
    val continueAction: String
    val saving: String
    val confirmProfile: String
    val errCouldntSaveProfile: String

    // HandoffScreen
    val noInviteTitle: String
    val noInviteBody: String
    fun sentTo(childName: String): String
    fun claimLinkReady(expiresAt: String): String
    val shareInvite: String
    val browseProfiles: String

    // ClaimFlowScreen
    val claimLandingTitle: String
    val claimLandingSubtitle: String
    val claimYourProfile: String
    val createAccountHeading: String
    val yourEmail: String
    val sendCode: String
    val sendCodeBusy: String
    fun otpSentTo(email: String): String
    val verificationCodeLabel: String
    val verify: String
    val verifyBusy: String
    val passwordLabel: String
    val passwordHint: String
    val createAccount: String
    val createAccountBusy: String
    val confirmIdentityHeading: String
    val contactInfoLabel: String
    val maritalStatusLabel: String
    val locationLabel: String
    val whoAreYouHopingToMeet: String
    val relationshipGoalLabel: String
    val looksGoodLetsGo: String
    val errSendCodeFailed: String
    val errOtpInvalid: String
    val errCreateAccountFailed: String
    val errBadClaimLink: String
    val errSavePreferencesFailed: String
}

object OnboardingStringsEn : OnboardingStrings {
    override val setUpProfile = "Set up the profile"
    override val stepAboutYou = "About you"
    override val stepAboutThem = "About them"
    override val stepBirthDetails = "Birth details"
    override val stepPreferences = "Preferences"
    override val stepReview = "Review"
    override val aboutYouHeading = "A little about you first."
    override val yourName = "Your name"
    override val yourRelationship = "Your relationship to them"
    override val relationshipMother = "Mother"
    override val relationshipFather = "Father"
    override val relationshipGuardian = "Guardian"
    override val relationshipOther = "Other relative"
    override val aboutChildHeading = "Tell us about your child."
    override val name = "Name"
    override val gender = "Gender"
    override val seeking = "Seeking"
    override val whenWasSheBorn = "When was she born?"
    override val dateLabel = "Date (YYYY-MM-DD)"
    override val timeLabel = "Time (HH:MM, 24h)"
    override val birthPlace = "Birth place"
    override val noCoordinatesWarning = "No coordinates resolved yet for this place -- pick a suggestion from the list, or the chart can't be computed (\"Null Island\" guard)."
    override val moreDetailsHeading = "A few more details."
    override val religion = "Religion"
    override val yourCommunity = "Your community"
    override val acceptableReligions = "Acceptable religions"
    override val minAge = "Min age"
    override val maxAge = "Max age"
    override val acceptableLocations = "Acceptable locations"
    override val acceptableMaritalStatus = "Acceptable marital status"
    override val beforeWeSave = "Before we save this."
    override val reviewBorn = "Born"
    override val reviewMatchAgeRange = "Match age range"
    override val back = "Back"
    override val continueAction = "Continue"
    override val saving = "Saving…"
    override val confirmProfile = "Confirm profile"
    override val errCouldntSaveProfile = "Couldn't save this profile."

    override val noInviteTitle = "No invite to show"
    override val noInviteBody = "Start setup again to generate a new one."
    override fun sentTo(childName: String) = "Sent to $childName."
    override fun claimLinkReady(expiresAt: String) = "A private claim link is ready to share -- theirs to open whenever they're ready. Expires $expiresAt."
    override val shareInvite = "Share invite"
    override val browseProfiles = "Browse profiles"

    override val claimLandingTitle = "Someone set this up for you 💌"
    override val claimLandingSubtitle = "Claim it to make it yours."
    override val claimYourProfile = "Claim your profile"
    override val createAccountHeading = "First, let's create your account."
    override val yourEmail = "Your email"
    override val sendCode = "Send code"
    override val sendCodeBusy = "Sending code…"
    override fun otpSentTo(email: String) = "We sent a 6-digit code to $email."
    override val verificationCodeLabel = "Verification code"
    override val verify = "Verify"
    override val verifyBusy = "Verifying…"
    override val passwordLabel = "Password"
    override val passwordHint = "At least 8 characters."
    override val createAccount = "Create account"
    override val createAccountBusy = "Creating account…"
    override val confirmIdentityHeading = "Let's make sure it's really you."
    override val contactInfoLabel = "Contact info (phone or email)"
    override val maritalStatusLabel = "Marital status"
    override val locationLabel = "Location"
    override val whoAreYouHopingToMeet = "Who are you hoping to meet?"
    override val relationshipGoalLabel = "Relationship goal"
    override val looksGoodLetsGo = "Looks good, let's go"
    override val errSendCodeFailed = "Couldn't send a code to that email."
    override val errOtpInvalid = "That code didn't check out."
    override val errCreateAccountFailed = "Couldn't create that account."
    override val errBadClaimLink = "That link doesn't look right -- ask for a fresh one."
    override val errSavePreferencesFailed = "Couldn't save your preferences."
}

object OnboardingStringsHi : OnboardingStrings {
    override val setUpProfile = "प्रोफ़ाइल सेट करें"
    override val stepAboutYou = "आपके बारे में"
    override val stepAboutThem = "उनके बारे में"
    override val stepBirthDetails = "जन्म विवरण"
    override val stepPreferences = "प्राथमिकताएं"
    override val stepReview = "समीक्षा"
    override val aboutYouHeading = "पहले अपने बारे में थोड़ा बताएं।"
    override val yourName = "आपका नाम"
    override val yourRelationship = "उनसे आपका रिश्ता"
    override val relationshipMother = "मां"
    override val relationshipFather = "पिता"
    override val relationshipGuardian = "अभिभावक"
    override val relationshipOther = "अन्य रिश्तेदार"
    override val aboutChildHeading = "हमें अपने बच्चे के बारे में बताएं।"
    override val name = "नाम"
    override val gender = "लिंग"
    override val seeking = "तलाश"
    override val whenWasSheBorn = "उनका जन्म कब हुआ था?"
    override val dateLabel = "तारीख़ (YYYY-MM-DD)"
    override val timeLabel = "समय (HH:MM, 24 घंटे)"
    override val birthPlace = "जन्म स्थान"
    override val noCoordinatesWarning = "इस स्थान के लिए अभी कोई निर्देशांक नहीं मिले -- सूची से एक सुझाव चुनें, वरना कुंडली नहीं बनाई जा सकती (\"Null Island\" सुरक्षा)।"
    override val moreDetailsHeading = "कुछ और विवरण।"
    override val religion = "धर्म"
    override val yourCommunity = "आपका समुदाय"
    override val acceptableReligions = "स्वीकार्य धर्म"
    override val minAge = "न्यूनतम आयु"
    override val maxAge = "अधिकतम आयु"
    override val acceptableLocations = "स्वीकार्य स्थान"
    override val acceptableMaritalStatus = "स्वीकार्य वैवाहिक स्थिति"
    override val beforeWeSave = "इसे सेव करने से पहले।"
    override val reviewBorn = "जन्म"
    override val reviewMatchAgeRange = "मैच आयु सीमा"
    override val back = "वापस"
    override val continueAction = "जारी रखें"
    override val saving = "सेव हो रहा है…"
    override val confirmProfile = "प्रोफ़ाइल की पुष्टि करें"
    override val errCouldntSaveProfile = "यह प्रोफ़ाइल सेव नहीं हो सकी।"

    override val noInviteTitle = "दिखाने के लिए कोई इनवाइट नहीं"
    override val noInviteBody = "नया बनाने के लिए फिर से सेटअप शुरू करें।"
    override fun sentTo(childName: String) = "$childName को भेजा गया।"
    override fun claimLinkReady(expiresAt: String) = "एक निजी क्लेम लिंक शेयर करने के लिए तैयार है -- जब वे तैयार हों तब खोल सकते हैं। समाप्ति: $expiresAt।"
    override val shareInvite = "इनवाइट शेयर करें"
    override val browseProfiles = "प्रोफ़ाइलें ब्राउज़ करें"

    override val claimLandingTitle = "किसी ने आपके लिए यह सेट किया है 💌"
    override val claimLandingSubtitle = "इसे अपना बनाने के लिए क्लेम करें।"
    override val claimYourProfile = "अपनी प्रोफ़ाइल क्लेम करें"
    override val createAccountHeading = "पहले, आइए आपका खाता बनाते हैं।"
    override val yourEmail = "आपका ईमेल"
    override val sendCode = "कोड भेजें"
    override val sendCodeBusy = "कोड भेजा जा रहा है…"
    override fun otpSentTo(email: String) = "हमने $email पर 6-अंकों का कोड भेजा है।"
    override val verificationCodeLabel = "सत्यापन कोड"
    override val verify = "सत्यापित करें"
    override val verifyBusy = "सत्यापित हो रहा है…"
    override val passwordLabel = "पासवर्ड"
    override val passwordHint = "कम से कम 8 अक्षर।"
    override val createAccount = "खाता बनाएं"
    override val createAccountBusy = "खाता बनाया जा रहा है…"
    override val confirmIdentityHeading = "यह सुनिश्चित करते हैं कि यह सच में आप हैं।"
    override val contactInfoLabel = "संपर्क जानकारी (फ़ोन या ईमेल)"
    override val maritalStatusLabel = "वैवाहिक स्थिति"
    override val locationLabel = "स्थान"
    override val whoAreYouHopingToMeet = "आप किससे मिलना चाहते हैं?"
    override val relationshipGoalLabel = "रिश्ते का लक्ष्य"
    override val looksGoodLetsGo = "ठीक लग रहा है, चलें"
    override val errSendCodeFailed = "उस ईमेल पर कोड नहीं भेजा जा सका।"
    override val errOtpInvalid = "वह कोड सही नहीं निकला।"
    override val errCreateAccountFailed = "वह खाता नहीं बनाया जा सका।"
    override val errBadClaimLink = "यह लिंक सही नहीं लगता -- एक नया लिंक मांगें।"
    override val errSavePreferencesFailed = "आपकी प्राथमिकताएं सेव नहीं हो सकीं।"
}

object OnboardingStringsTa : OnboardingStrings {
    override val setUpProfile = "சுயவிவரத்தை அமைக்கவும்"
    override val stepAboutYou = "உங்களைப் பற்றி"
    override val stepAboutThem = "அவர்களைப் பற்றி"
    override val stepBirthDetails = "பிறப்பு விவரங்கள்"
    override val stepPreferences = "விருப்பத்தேர்வுகள்"
    override val stepReview = "மதிப்பாய்வு"
    override val aboutYouHeading = "முதலில் உங்களைப் பற்றி கொஞ்சம் சொல்லுங்கள்."
    override val yourName = "உங்கள் பெயர்"
    override val yourRelationship = "அவர்களுடனான உங்கள் உறவு"
    override val relationshipMother = "தாய்"
    override val relationshipFather = "தந்தை"
    override val relationshipGuardian = "பாதுகாவலர்"
    override val relationshipOther = "மற்ற உறவினர்"
    override val aboutChildHeading = "உங்கள் குழந்தையைப் பற்றி சொல்லுங்கள்."
    override val name = "பெயர்"
    override val gender = "பாலினம்"
    override val seeking = "தேடுவது"
    override val whenWasSheBorn = "அவர் எப்போது பிறந்தார்?"
    override val dateLabel = "தேதி (YYYY-MM-DD)"
    override val timeLabel = "நேரம் (HH:MM, 24 மணி)"
    override val birthPlace = "பிறந்த இடம்"
    override val noCoordinatesWarning = "இந்த இடத்திற்கு இன்னும் ஆயத்தொலைவுகள் கிடைக்கவில்லை -- பட்டியலில் இருந்து ஒரு பரிந்துரையைத் தேர்வு செய்யவும், இல்லையெனில் ஜாதகத்தைக் கணக்கிட முடியாது (\"Null Island\" பாதுகாப்பு)."
    override val moreDetailsHeading = "இன்னும் சில விவரங்கள்."
    override val religion = "மதம்"
    override val yourCommunity = "உங்கள் சமூகம்"
    override val acceptableReligions = "ஏற்கத்தக்க மதங்கள்"
    override val minAge = "குறைந்தபட்ச வயது"
    override val maxAge = "அதிகபட்ச வயது"
    override val acceptableLocations = "ஏற்கத்தக்க இடங்கள்"
    override val acceptableMaritalStatus = "ஏற்கத்தக்க திருமண நிலை"
    override val beforeWeSave = "இதைச் சேமிக்கும் முன்."
    override val reviewBorn = "பிறப்பு"
    override val reviewMatchAgeRange = "பொருத்த வயது வரம்பு"
    override val back = "பின்செல்"
    override val continueAction = "தொடரவும்"
    override val saving = "சேமிக்கப்படுகிறது…"
    override val confirmProfile = "சுயவிவரத்தை உறுதிப்படுத்தவும்"
    override val errCouldntSaveProfile = "இந்த சுயவிவரத்தைச் சேமிக்க முடியவில்லை."

    override val noInviteTitle = "காட்ட அழைப்பு இல்லை"
    override val noInviteBody = "புதியதை உருவாக்க மீண்டும் அமைப்பைத் தொடங்கவும்."
    override fun sentTo(childName: String) = "$childName க்கு அனுப்பப்பட்டது."
    override fun claimLinkReady(expiresAt: String) = "ஒரு தனிப்பட்ட கோரிக்கை இணைப்பு பகிர தயாராக உள்ளது -- அவர்கள் தயாரானதும் திறக்கலாம். காலாவதி: $expiresAt."
    override val shareInvite = "அழைப்பைப் பகிரவும்"
    override val browseProfiles = "சுயவிவரங்களை உலாவவும்"

    override val claimLandingTitle = "யாரோ உங்களுக்காக இதை அமைத்துள்ளனர் 💌"
    override val claimLandingSubtitle = "அதை உங்களுடையதாக்க கோருங்கள்."
    override val claimYourProfile = "உங்கள் சுயவிவரத்தை கோருங்கள்"
    override val createAccountHeading = "முதலில், உங்கள் கணக்கை உருவாக்குவோம்."
    override val yourEmail = "உங்கள் மின்னஞ்சல்"
    override val sendCode = "குறியீட்டை அனுப்பவும்"
    override val sendCodeBusy = "குறியீடு அனுப்பப்படுகிறது…"
    override fun otpSentTo(email: String) = "$email க்கு 6-இலக்க குறியீட்டை அனுப்பியுள்ளோம்."
    override val verificationCodeLabel = "சரிபார்ப்புக் குறியீடு"
    override val verify = "சரிபார்க்கவும்"
    override val verifyBusy = "சரிபார்க்கிறோம்…"
    override val passwordLabel = "கடவுச்சொல்"
    override val passwordHint = "குறைந்தது 8 எழுத்துகள்."
    override val createAccount = "கணக்கை உருவாக்கவும்"
    override val createAccountBusy = "கணக்கு உருவாக்கப்படுகிறது…"
    override val confirmIdentityHeading = "இது உண்மையில் நீங்கள்தான் என்பதை உறுதிசெய்வோம்."
    override val contactInfoLabel = "தொடர்பு தகவல் (தொலைபேசி அல்லது மின்னஞ்சல்)"
    override val maritalStatusLabel = "திருமண நிலை"
    override val locationLabel = "இடம்"
    override val whoAreYouHopingToMeet = "நீங்கள் யாரைச் சந்திக்க விரும்புகிறீர்கள்?"
    override val relationshipGoalLabel = "உறவு இலக்கு"
    override val looksGoodLetsGo = "நன்றாக இருக்கிறது, செல்லலாம்"
    override val errSendCodeFailed = "அந்த மின்னஞ்சலுக்கு குறியீட்டை அனுப்ப முடியவில்லை."
    override val errOtpInvalid = "அந்தக் குறியீடு சரியில்லை."
    override val errCreateAccountFailed = "அந்தக் கணக்கை உருவாக்க முடியவில்லை."
    override val errBadClaimLink = "அந்த இணைப்பு சரியாகத் தெரியவில்லை -- புதிய இணைப்பைக் கேளுங்கள்."
    override val errSavePreferencesFailed = "உங்கள் விருப்பங்களைச் சேமிக்க முடியவில்லை."
}
